package com.saku.speak

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Streaming Rolling-Window Hardware Microphone Engine using Android's [AudioRecord].
 *
 * Architecture highlights:
 * 1. Captures 16kHz PCM audio in memory on a dedicated background thread (zero Android SpeechRecognizer IPC crashes).
 * 2. Downsamples 2:1 to 8kHz mono WAV in memory and trims leading/trailing silence (only 16 KB/sec, zero disk I/O).
 * 3. Fires non-blocking rolling snapshots while you speak (and 180ms into any pause) so live Japanese text
 *    appears in the placeholder right above the microphone button as you talk.
 * 4. Pre-transcribes during the 180ms..850ms silence window so final text is ready with near-zero latency when speech ends.
 */
class SpeechRecognizerHelper(
    context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onStateChange: (isListening: Boolean) -> Unit,
    private val onError: (String) -> Unit,
    private val transcribeAudioChunk: (suspend (ByteArray) -> Result<String>)? = null
) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isListening = false

    @Volatile
    private var isCancelled = false

    @Volatile
    private var shouldStopAndDeliver = false

    @Volatile
    private var latestRecognizedText = ""

    @Volatile
    private var activeSnapshotJob: Job? = null

    @Volatile
    private var completedSnapshotEndSample = 0

    private var recordingThread: Thread? = null

    fun isAvailable(): Boolean {
        return try {
            appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        } catch (_: Throwable) {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening(languageCode: String = "ja-JP") {
        isCancelled = true
        shouldStopAndDeliver = false
        try {
            recordingThread?.join(120)
        } catch (_: Throwable) {}

        isCancelled = false
        shouldStopAndDeliver = false
        isListening = true
        latestRecognizedText = ""
        activeSnapshotJob = null
        completedSnapshotEndSample = 0

        mainHandler.post {
            onStateChange(true)
            onPartialResult("聞いています...")
        }

        val thread = Thread({
            var audioRecord: AudioRecord? = null
            // Store 16kHz samples synchronized for fast non-blocking rolling snapshots
            val samplesLock = Any()
            val pcmSamples16k = ShortArrayList(initialCapacity = 16000 * 8)

            try {
                val captureSampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufSize = AudioRecord.getMinBufferSize(captureSampleRate, channelConfig, audioFormat)
                val bufferSizeInBytes = max(minBufSize * 2, 4096)

                val recordCandidate = try {
                    AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        captureSampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSizeInBytes
                    )
                } catch (_: Throwable) {
                    null
                }

                audioRecord = if (recordCandidate != null && recordCandidate.state == AudioRecord.STATE_INITIALIZED) {
                    recordCandidate
                } else {
                    try {
                        recordCandidate?.release()
                    } catch (_: Throwable) {}
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        captureSampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSizeInBytes
                    )
                }

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    try {
                        audioRecord.release()
                    } catch (_: Throwable) {}
                    isListening = false
                    mainHandler.post {
                        onStateChange(false)
                        onRmsChanged(0f)
                        onError("Microphone could not be initialized.")
                    }
                    return@Thread
                }

                audioRecord.startRecording()

                val shortBuffer = ShortArray(800) // 50ms chunks at 16kHz
                val startTimeMs = System.currentTimeMillis()
                var hasDetectedSpeech = false
                var firstSpeechSampleIndex = -1
                var lastSpeechSampleIndex = 0
                var lastSpeechTimeMs = startTimeMs
                var noiseFloorRms = 160f

                // Rolling live transcription state
                var lastSnapshotTimeMs = startTimeMs
                var lastSnapshotEndSample = 0

                val triggerRollingSnapshot: (Int) -> Unit = { targetEndSample ->
                    if (transcribeAudioChunk != null && firstSpeechSampleIndex >= 0 && targetEndSample - firstSpeechSampleIndex >= 3200) {
                        lastSnapshotEndSample = targetEndSample
                        lastSnapshotTimeMs = System.currentTimeMillis()
                        val startIdx = max(0, firstSpeechSampleIndex - 2400) // 150ms pre-roll
                        val wavBytes = synchronized(samplesLock) {
                            buildDownsampled8kHzWavBytes(pcmSamples16k, startIdx, min(targetEndSample + 1600, pcmSamples16k.size))
                        }
                        activeSnapshotJob = scope.launch {
                            val res = transcribeAudioChunk.invoke(wavBytes)
                            res.onSuccess { text ->
                                if (text.isNotBlank() && !isCancelled) {
                                    latestRecognizedText = text
                                    completedSnapshotEndSample = max(completedSnapshotEndSample, targetEndSample)
                                    mainHandler.post {
                                        onPartialResult(text)
                                    }
                                }
                            }
                        }
                    }
                }

                while (!isCancelled && !shouldStopAndDeliver) {
                    val readCount = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                    if (readCount > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until readCount) {
                            val s = shortBuffer[i]
                            sumSquares += s.toDouble() * s.toDouble()
                        }

                        val currentSampleCount = synchronized(samplesLock) {
                            pcmSamples16k.addAll(shortBuffer, readCount)
                            pcmSamples16k.size
                        }

                        val rms = sqrt(sumSquares / readCount).toFloat()
                        val now = System.currentTimeMillis()
                        val elapsedMs = now - startTimeMs

                        if (elapsedMs < 150L && rms < 550f) {
                            noiseFloorRms = (noiseFloorRms * 0.7f) + (rms * 0.3f)
                        }

                        val speechThreshold = max(290f, noiseFloorRms * 2.0f).coerceAtMost(900f)
                        val rmsDb = if (rms > 55f) {
                            (20f * log10(rms / 55f)).coerceIn(0f, 10f)
                        } else {
                            0f
                        }

                        if (rms >= speechThreshold) {
                            if (!hasDetectedSpeech) {
                                hasDetectedSpeech = true
                                firstSpeechSampleIndex = max(0, currentSampleCount - readCount)
                            }
                            lastSpeechSampleIndex = currentSampleCount
                            lastSpeechTimeMs = now

                            // Live streaming update every ~950ms while user is actively speaking
                            if (now - lastSnapshotTimeMs >= 950L && (activeSnapshotJob == null || activeSnapshotJob?.isCompleted == true)) {
                                triggerRollingSnapshot(lastSpeechSampleIndex)
                            }
                        } else if (hasDetectedSpeech) {
                            val silenceMs = now - lastSpeechTimeMs

                            // Pre-transcribe immediately after 180ms of pause so transcript is ALREADY ready before silence timeout finishes!
                            if (silenceMs >= 180L && lastSpeechSampleIndex > lastSnapshotEndSample &&
                                (activeSnapshotJob == null || activeSnapshotJob?.isCompleted == true)
                            ) {
                                triggerRollingSnapshot(lastSpeechSampleIndex)
                            }

                            // Snappy hands-free auto-stop after 850ms of pause
                            if (silenceMs >= 850L && elapsedMs >= 750L) {
                                shouldStopAndDeliver = true
                                break
                            }
                        }

                        mainHandler.post {
                            if (isListening && !isCancelled) {
                                onRmsChanged(rmsDb)
                            }
                        }

                        if (elapsedMs >= 22000L) {
                            shouldStopAndDeliver = true
                            break
                        }
                    } else if (readCount < 0) {
                        break
                    }
                }

                try {
                    audioRecord.stop()
                } catch (_: Throwable) {}
                try {
                    audioRecord.release()
                } catch (_: Throwable) {}
                audioRecord = null

                isListening = false

                if (isCancelled) {
                    mainHandler.post {
                        onStateChange(false)
                        onRmsChanged(0f)
                    }
                    return@Thread
                }

                val totalSamples = synchronized(samplesLock) { pcmSamples16k.size }
                val totalDurationMs = System.currentTimeMillis() - startTimeMs

                if (totalSamples < 3200 || totalDurationMs < 220L || transcribeAudioChunk == null) {
                    mainHandler.post {
                        onStateChange(false)
                        onRmsChanged(0f)
                    }
                    return@Thread
                }

                // Finalize transcription on coroutine scope
                scope.launch {
                    try {
                        // Wait briefly if a pre-transcription job from the 180ms pause detector is already in flight
                        activeSnapshotJob?.join()

                        // If the pre-transcription job already covered up to the end of speech (within 150ms / 2400 samples),
                        // use it immediately with ZERO additional network wait!
                        val effectiveSpeechEnd = if (hasDetectedSpeech) lastSpeechSampleIndex else totalSamples
                        val needsFinalCall = latestRecognizedText.isBlank() || (effectiveSpeechEnd - completedSnapshotEndSample > 2400)

                        var finalTranscript = latestRecognizedText
                        if (needsFinalCall) {
                            val startIdx = if (firstSpeechSampleIndex >= 0) max(0, firstSpeechSampleIndex - 2400) else 0
                            val endIdx = min(totalSamples, effectiveSpeechEnd + 1600)
                            val finalWavBytes = synchronized(samplesLock) {
                                buildDownsampled8kHzWavBytes(pcmSamples16k, startIdx, endIdx)
                            }
                            val res = transcribeAudioChunk.invoke(finalWavBytes)
                            res.fold(
                                onSuccess = { text ->
                                    if (text.isNotBlank()) {
                                        finalTranscript = text
                                    }
                                },
                                onFailure = { err ->
                                    if (finalTranscript.isBlank()) {
                                        val msg = err.localizedMessage ?: ""
                                        mainHandler.post {
                                            onStateChange(false)
                                            onRmsChanged(0f)
                                            if (!msg.contains("No speech detected", ignoreCase = true)) {
                                                onError(msg.ifBlank { "Speech recognition error" })
                                            } else {
                                                onPartialResult("")
                                            }
                                        }
                                        return@launch
                                    }
                                }
                            )
                        }

                        if (!isCancelled && finalTranscript.isNotBlank()) {
                            val cleanFinal = finalTranscript.trim()
                            mainHandler.post {
                                onStateChange(false)
                                onRmsChanged(0f)
                                // Keep the transcribed text displayed right above the microphone as placeholder!
                                onPartialResult(cleanFinal)
                                onFinalResult(cleanFinal)
                            }
                        } else {
                            mainHandler.post {
                                onStateChange(false)
                                onRmsChanged(0f)
                                onPartialResult("")
                            }
                        }
                    } catch (t: Throwable) {
                        mainHandler.post {
                            onStateChange(false)
                            onRmsChanged(0f)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e("SpeechHelper", "AudioRecord error", t)
                isListening = false
                try {
                    audioRecord?.stop()
                } catch (_: Throwable) {}
                try {
                    audioRecord?.release()
                } catch (_: Throwable) {}

                mainHandler.post {
                    onStateChange(false)
                    onRmsChanged(0f)
                    onError("Microphone error: ${t.localizedMessage ?: "Please check mic permissions"}")
                }
            }
        }, "SakuAudioRecorderThread")

        recordingThread = thread
        thread.start()
    }

    fun stopListening() {
        shouldStopAndDeliver = true
    }

    fun cancel() {
        isCancelled = true
        shouldStopAndDeliver = false
        isListening = false
        mainHandler.post {
            onStateChange(false)
            onRmsChanged(0f)
        }
    }

    fun destroy() {
        cancel()
    }

    /**
     * Builds an in-memory 8kHz 16-bit mono WAV byte array from the 16kHz PCM buffer slice [startIdx, endIdx).
     * 2:1 averaging filter prevents aliasing while cutting payload size in half (only 16 KB/sec) for fast upload.
     */
    private fun buildDownsampled8kHzWavBytes(
        samples16k: ShortArrayList,
        startIdx: Int,
        endIdx: Int
    ): ByteArray {
        val safeStart = startIdx.coerceIn(0, samples16k.size)
        val safeEnd = endIdx.coerceIn(safeStart, samples16k.size)
        val outSampleCount = (safeEnd - safeStart) / 2
        val sampleRate = 8000
        val channels = 1
        val bitsPerSample = 16
        val dataSize = outSampleCount * 2
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val chunkSize = 36 + dataSize

        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(chunkSize)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(dataSize)

        var idx = safeStart
        for (i in 0 until outSampleCount) {
            val s1 = samples16k.get(idx).toInt()
            val s2 = samples16k.get(idx + 1).toInt()
            val avg = ((s1 + s2) shr 1).toShort()
            buffer.putShort(avg)
            idx += 2
        }

        return buffer.array()
    }

    private class ShortArrayList(initialCapacity: Int) {
        private var data = ShortArray(initialCapacity)
        var size: Int = 0
            private set

        fun addAll(source: ShortArray, count: Int) {
            if (size + count > data.size) {
                val newCapacity = max(data.size * 2, size + count + 8192)
                data = data.copyOf(newCapacity)
            }
            System.arraycopy(source, 0, data, size, count)
            size += count
        }

        fun get(index: Int): Short = data[index]
    }
}
