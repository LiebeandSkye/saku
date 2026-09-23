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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Pure hardware microphone recorder using Android's low-level [AudioRecord] (16kHz, 16-bit mono PCM WAV).
 *
 * Completely avoids Android's `android.speech.SpeechRecognizer` / `RecognitionService` Binder IPC,
 * which crashes the Main Thread Looper or hangs indefinitely on many OEM Android 12-15 devices.
 */
class SpeechRecognizerHelper(
    context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onStateChange: (isListening: Boolean) -> Unit,
    private val onError: (String) -> Unit,
    private val onAudioRecorded: ((File) -> Unit)? = null
) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isListening = false

    @Volatile
    private var isCancelled = false

    @Volatile
    private var shouldStopAndDeliver = false

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
        // Cancel any previous session cleanly before starting a new one
        isCancelled = true
        shouldStopAndDeliver = false
        try {
            recordingThread?.join(150)
        } catch (_: Throwable) {}

        isCancelled = false
        shouldStopAndDeliver = false
        isListening = true

        mainHandler.post {
            onStateChange(true)
            onPartialResult("聞いています... (話してください)")
        }

        val thread = Thread({
            var audioRecord: AudioRecord? = null
            val pcmStream = ByteArrayOutputStream()

            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSizeInBytes = max(minBufSize * 2, 4096)

                // Try VOICE_RECOGNITION first (tuned for clean speech), fallback to MIC
                val recordCandidate = try {
                    AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        sampleRate,
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
                        sampleRate,
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
                        onPartialResult("")
                        onError("Microphone could not be initialized. Please check microphone permissions.")
                    }
                    return@Thread
                }

                audioRecord.startRecording()

                val shortBuffer = ShortArray(1024) // 64ms per chunk at 16kHz
                val byteBuffer = ByteBuffer.allocate(shortBuffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)

                val startTimeMs = System.currentTimeMillis()
                var hasDetectedSpeech = false
                var lastSpeechTimeMs = startTimeMs
                var noiseFloorRms = 180f
                var hasShownSpeechBadge = false

                while (!isCancelled && !shouldStopAndDeliver) {
                    val readCount = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                    if (readCount > 0) {
                        byteBuffer.clear()
                        var sumSquares = 0.0
                        for (i in 0 until readCount) {
                            val sample = shortBuffer[i]
                            byteBuffer.putShort(sample)
                            sumSquares += sample.toDouble() * sample.toDouble()
                        }
                        pcmStream.write(byteBuffer.array(), 0, readCount * 2)

                        val rms = sqrt(sumSquares / readCount).toFloat()
                        val now = System.currentTimeMillis()
                        val elapsedMs = now - startTimeMs

                        // Calibrate ambient room noise floor during the first 180ms if quiet
                        if (elapsedMs < 180L && rms < 600f) {
                            noiseFloorRms = (noiseFloorRms * 0.7f) + (rms * 0.3f)
                        }

                        val speechThreshold = max(320f, noiseFloorRms * 2.1f).coerceAtMost(950f)

                        // Convert RMS into 0..10 scale for the pulsing circle UI
                        val rmsDb = if (rms > 60f) {
                            (20f * log10(rms / 60f)).coerceIn(0f, 10f)
                        } else {
                            0f
                        }

                        if (rms >= speechThreshold) {
                            hasDetectedSpeech = true
                            lastSpeechTimeMs = now
                            if (!hasShownSpeechBadge) {
                                hasShownSpeechBadge = true
                                mainHandler.post {
                                    if (isListening && !isCancelled) {
                                        onPartialResult("🎙️ 聞き取り中... (話し終えると自動送信)")
                                    }
                                }
                            }
                        }

                        mainHandler.post {
                            if (isListening && !isCancelled) {
                                onRmsChanged(rmsDb)
                            }
                        }

                        // Hands-free auto-stop: if user has spoken and then pauses for 1.65s
                        if (hasDetectedSpeech && (now - lastSpeechTimeMs >= 1650L) && elapsedMs >= 1000L) {
                            shouldStopAndDeliver = true
                            break
                        }

                        // Safety maximum duration: 25 seconds
                        if (elapsedMs >= 25000L) {
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

                val totalDurationMs = System.currentTimeMillis() - startTimeMs
                val pcmBytes = pcmStream.toByteArray()

                isListening = false

                if (isCancelled) {
                    mainHandler.post {
                        onStateChange(false)
                        onRmsChanged(0f)
                        onPartialResult("")
                    }
                    return@Thread
                }

                // Deliver recorded WAV file if at least ~300ms of audio was captured
                if (pcmBytes.size >= 9600 && totalDurationMs >= 280L) {
                    val wavFile = File(appContext.cacheDir, "saku_voice_input.wav")
                    writeWavFile(wavFile, pcmBytes, sampleRate, 1, 16)

                    mainHandler.post {
                        onStateChange(false)
                        onRmsChanged(0f)
                        onAudioRecorded?.invoke(wavFile)
                    }
                } else {
                    mainHandler.post {
                        onStateChange(false)
                        onRmsChanged(0f)
                        onPartialResult("")
                    }
                }
            } catch (t: Throwable) {
                Log.e("SpeechHelper", "AudioRecord thread error", t)
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
                    onPartialResult("")
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
            onPartialResult("")
        }
    }

    fun destroy() {
        cancel()
    }

    private fun writeWavFile(
        outFile: File,
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        // "RIFF"
        header.put('R'.code.toByte())
        header.put('I'.code.toByte())
        header.put('F'.code.toByte())
        header.put('F'.code.toByte())
        header.putInt(chunkSize)
        // "WAVE"
        header.put('W'.code.toByte())
        header.put('A'.code.toByte())
        header.put('V'.code.toByte())
        header.put('E'.code.toByte())
        // "fmt "
        header.put('f'.code.toByte())
        header.put('m'.code.toByte())
        header.put('t'.code.toByte())
        header.put(' '.code.toByte())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        // "data"
        header.put('d'.code.toByte())
        header.put('a'.code.toByte())
        header.put('t'.code.toByte())
        header.put('a'.code.toByte())
        header.putInt(dataSize)

        FileOutputStream(outFile).use { fos ->
            fos.write(header.array())
            fos.write(pcmData)
            fos.flush()
        }
    }
}
