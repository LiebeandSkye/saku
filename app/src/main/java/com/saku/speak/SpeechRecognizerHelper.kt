package com.saku.speak

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.io.File
import kotlin.math.log10
import kotlin.math.max

class SpeechRecognizerHelper(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onStateChange: (isListening: Boolean) -> Unit,
    private val onError: (String) -> Unit,
    private val onAudioRecorded: ((File) -> Unit)? = null
) {

    private val appContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var lastPartialText = ""
    private var isCancelled = false
    private var pendingResultsRunnable: Runnable? = null
    private var hasDeliveredFinal = false

    // Hardware MediaRecorder fallback when Android's SpeechRecognizer service is missing,
    // blocked by OEM permissions, or throws client/service errors.
    private var useHardwareRecorderFallback = false
    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var amplitudePollRunnable: Runnable? = null
    private var hardwareStartTimeMs = 0L
    private var hasDetectedVoiceInHardware = false
    private var silenceStartMs = 0L

    fun isAvailable(): Boolean {
        val hasMicHardware = try {
            appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        } catch (_: Throwable) {
            true
        }
        val hasSpeechService = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Throwable) {
            false
        }
        return hasMicHardware || hasSpeechService
    }

    /**
     * Installs a reflection-based Handler.Callback guard on Android's internal SpeechRecognizer mHandler.
     * Android's SpeechRecognizer.startListening() posts MSG_START to its internal mHandler on Looper.getMainLooper().
     * Without this guard, a SecurityException or IllegalArgumentException thrown during asynchronous bindService()
     * inside mHandler would bypass startListening()'s try-catch block and crash the app process.
     */
    private fun installSafeHandlerGuard(recognizer: SpeechRecognizer) {
        try {
            var clazz: Class<*>? = recognizer.javaClass
            var handlerField: java.lang.reflect.Field? = null
            while (clazz != null && handlerField == null) {
                handlerField = try {
                    clazz.getDeclaredField("mHandler")
                } catch (_: NoSuchFieldException) {
                    null
                }
                clazz = clazz.superclass
            }
            if (handlerField != null) {
                handlerField.isAccessible = true
                val internalHandler = handlerField.get(recognizer) as? Handler
                if (internalHandler != null) {
                    val callbackField = Handler::class.java.getDeclaredField("mCallback")
                    callbackField.isAccessible = true
                    val existingCallback = callbackField.get(internalHandler) as? Handler.Callback
                    callbackField.set(internalHandler, Handler.Callback { msg ->
                        try {
                            if (existingCallback != null && existingCallback.handleMessage(msg)) {
                                return@Callback true
                            }
                            internalHandler.handleMessage(msg)
                            true
                        } catch (t: Throwable) {
                            Log.e("SpeechHelper", "Intercepted async SpeechRecognizer mHandler crash: ${t.message}", t)
                            fallbackToHardwareRecorderOnFailure()
                            true
                        }
                    })
                }
            }
        } catch (t: Throwable) {
            Log.w("SpeechHelper", "Could not install mHandler guard: ${t.message}")
        }
    }

    private fun fallbackToHardwareRecorderOnFailure() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Throwable) {}
            speechRecognizer = null
            useHardwareRecorderFallback = true
            if (!isCancelled && !hasDeliveredFinal && onAudioRecorded != null) {
                startHardwareRecording()
            } else {
                isListening = false
                onStateChange(false)
                onRmsChanged(0f)
                onError("Voice service unavailable; please try again.")
            }
        }
    }

    private fun findBestRecognitionService(ctx: Context): ComponentName? {
        val pm = ctx.packageManager
        val serviceIntent = Intent(RecognitionService.SERVICE_INTERFACE)
        val resolveInfos = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(serviceIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentServices(serviceIntent, 0)
            }
        } catch (t: Throwable) {
            Log.e("SpeechHelper", "Error querying recognition services", t)
            emptyList()
        }

        if (resolveInfos.isEmpty()) {
            return null
        }

        val validServices = resolveInfos.mapNotNull { it.serviceInfo }.filter { it.exported }

        // 1. Speech Recognition & Synthesis from Google (com.google.android.tts)
        val googleTtsRecognition = validServices.firstOrNull {
            it.packageName == "com.google.android.tts" &&
                it.name.contains("RecognitionService", ignoreCase = true)
        }
        if (googleTtsRecognition != null) {
            return ComponentName(googleTtsRecognition.packageName, googleTtsRecognition.name)
        }

        // 2. Google App (QuickSearchBox) Recognition Service
        val googleAppRecognition = validServices.firstOrNull {
            it.packageName == "com.google.android.googlequicksearchbox" &&
                it.name.contains("RecognitionService", ignoreCase = true)
        }
        if (googleAppRecognition != null) {
            return ComponentName(googleAppRecognition.packageName, googleAppRecognition.name)
        }

        // 3. Other exported recognition service (excluding Android System Intelligence which rejects 3rd-party binds)
        val oemService = validServices.firstOrNull {
            !it.packageName.startsWith("com.google.android.as") &&
                (it.permission.isNullOrBlank() || it.permission == "android.permission.BIND_RECOGNITION_SERVICE")
        }
        if (oemService != null) {
            return ComponentName(oemService.packageName, oemService.name)
        }

        return null
    }

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer

        val speechServiceAvailable = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Throwable) {
            false
        }
        if (!speechServiceAvailable) {
            return null
        }

        // Strategy 1: Verified dedicated service (Google Speech Services / Google QuickSearchBox)
        // Using the Activity context ensures Android 12-15 AttributionSource & window token checks succeed.
        val bestComponent = findBestRecognitionService(context)
        if (bestComponent != null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context, bestComponent).apply {
                    installSafeHandlerGuard(this)
                    setRecognitionListener(createListener())
                }
                Log.d("SpeechHelper", "SpeechRecognizer initialized with component: ${bestComponent.flattenToShortString()}")
                return speechRecognizer
            } catch (t: Throwable) {
                Log.w("SpeechHelper", "Failed to create recognizer with component $bestComponent: ${t.message}")
                try {
                    speechRecognizer?.destroy()
                } catch (_: Throwable) {}
                speechRecognizer = null
            }
        }

        // Strategy 2: Default SpeechRecognizer (with Activity context and mHandler crash guard)
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                installSafeHandlerGuard(this)
                setRecognitionListener(createListener())
            }
            Log.d("SpeechHelper", "SpeechRecognizer initialized with system default")
            return speechRecognizer
        } catch (t: Throwable) {
            Log.w("SpeechHelper", "Failed to create default SpeechRecognizer: ${t.message}")
            try {
                speechRecognizer?.destroy()
            } catch (_: Throwable) {}
            speechRecognizer = null
        }

        // Strategy 3: On-device SpeechRecognizer on Android 13+ if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context).apply {
                        installSafeHandlerGuard(this)
                        setRecognitionListener(createListener())
                    }
                    Log.d("SpeechHelper", "SpeechRecognizer initialized with on-device recognizer")
                    return speechRecognizer
                }
            } catch (t: Throwable) {
                Log.w("SpeechHelper", "Failed to create on-device SpeechRecognizer: ${t.message}")
                try {
                    speechRecognizer?.destroy()
                } catch (_: Throwable) {}
                speechRecognizer = null
            }
        }

        return null
    }

    fun startListening(languageCode: String = "ja-JP") {
        mainHandler.post {
            pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingResultsRunnable = null
            isCancelled = false
            hasDeliveredFinal = false
            lastPartialText = ""

            if (useHardwareRecorderFallback && onAudioRecorded != null) {
                startHardwareRecording()
                return@post
            }

            val recognizer = ensureRecognizer()
            if (recognizer == null) {
                if (onAudioRecorded != null) {
                    useHardwareRecorderFallback = true
                    startHardwareRecording()
                } else {
                    isListening = false
                    onStateChange(false)
                    onError("Speech recognition is unavailable on this device.")
                }
                return@post
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                // Japanese conversational pause tolerance: 2000ms
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            }

            try {
                recognizer.startListening(intent)
                isListening = true
                onStateChange(true)
            } catch (t: Throwable) {
                Log.e("SpeechHelper", "Exception during startListening, falling back to hardware recorder", t)
                try {
                    speechRecognizer?.destroy()
                } catch (_: Throwable) {}
                speechRecognizer = null
                if (onAudioRecorded != null) {
                    useHardwareRecorderFallback = true
                    startHardwareRecording()
                } else {
                    isListening = false
                    onStateChange(false)
                    onError("Failed to start listening: ${t.localizedMessage ?: "Service error"}")
                }
            }
        }
    }

    private fun startHardwareRecording() {
        stopHardwareRecording(deliverResult = false)
        try {
            val outFile = File(appContext.cacheDir, "saku_voice_input.m4a")
            if (outFile.exists()) {
                outFile.delete()
            }
            currentAudioFile = outFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(16000)
            recorder.setAudioEncodingBitRate(64000)
            recorder.setOutputFile(outFile.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            hardwareStartTimeMs = System.currentTimeMillis()
            hasDetectedVoiceInHardware = false
            silenceStartMs = 0L
            isListening = true
            onStateChange(true)
            onPartialResult("聞いています...")

            val pollRunnable = object : Runnable {
                override fun run() {
                    val activeRecorder = mediaRecorder ?: return
                    if (!isListening || isCancelled) return
                    try {
                        val maxAmp = activeRecorder.maxAmplitude
                        // Convert 0..32767 amplitude into 0..10 dB scale for waveform ring animation
                        val rms = if (maxAmp > 80) {
                            (20f * log10(maxAmp.toFloat() / 80f)).coerceIn(0f, 10f)
                        } else {
                            0f
                        }
                        onRmsChanged(rms)

                        val now = System.currentTimeMillis()
                        val elapsed = now - hardwareStartTimeMs

                        if (maxAmp > 950) {
                            hasDetectedVoiceInHardware = true
                            silenceStartMs = 0L
                        } else if (hasDetectedVoiceInHardware) {
                            if (silenceStartMs == 0L) {
                                silenceStartMs = now
                            } else if (now - silenceStartMs >= 1850L && elapsed >= 1200L) {
                                // Hands-free auto-stop when user finishes speaking and pauses for ~1.85s
                                stopHardwareRecording(deliverResult = true)
                                return
                            }
                        }

                        // Safety cap at 25 seconds
                        if (elapsed >= 25000L) {
                            stopHardwareRecording(deliverResult = true)
                            return
                        }

                        mainHandler.postDelayed(this, 60L)
                    } catch (_: Throwable) {
                    }
                }
            }
            amplitudePollRunnable = pollRunnable
            mainHandler.postDelayed(pollRunnable, 60L)
        } catch (t: Throwable) {
            Log.e("SpeechHelper", "Hardware MediaRecorder failed to start", t)
            stopHardwareRecording(deliverResult = false)
            isListening = false
            onStateChange(false)
            onRmsChanged(0f)
            onError("Microphone error: ${t.localizedMessage ?: "Check microphone permissions"}")
        }
    }

    private fun stopHardwareRecording(deliverResult: Boolean) {
        amplitudePollRunnable?.let { mainHandler.removeCallbacks(it) }
        amplitudePollRunnable = null

        val recorder = mediaRecorder
        mediaRecorder = null
        val durationMs = System.currentTimeMillis() - hardwareStartTimeMs

        var stoppedCleanly = false
        if (recorder != null) {
            try {
                recorder.stop()
                stoppedCleanly = true
            } catch (_: Throwable) {
            } finally {
                try {
                    recorder.release()
                } catch (_: Throwable) {}
            }
        }

        if (deliverResult && !isCancelled && !hasDeliveredFinal) {
            hasDeliveredFinal = true
            isListening = false
            onStateChange(false)
            onRmsChanged(0f)
            onPartialResult("")

            val recordedFile = currentAudioFile
            if (stoppedCleanly && recordedFile != null && recordedFile.exists() && recordedFile.length() > 300L && durationMs >= 300L) {
                onAudioRecorded?.invoke(recordedFile)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            if (mediaRecorder != null) {
                stopHardwareRecording(deliverResult = true)
                return@post
            }

            try {
                speechRecognizer?.stopListening()
            } catch (_: Throwable) {
            }
            // Fallback watchdog: only fire if engine stalls for >2.0s and partial speech was captured
            if (lastPartialText.isNotBlank() && !hasDeliveredFinal) {
                pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
                val runnable = Runnable {
                    if (lastPartialText.isNotBlank() && !hasDeliveredFinal) {
                        hasDeliveredFinal = true
                        val text = lastPartialText.trim()
                        lastPartialText = ""
                        isListening = false
                        onStateChange(false)
                        onFinalResult(text)
                    }
                }
                pendingResultsRunnable = runnable
                mainHandler.postDelayed(runnable, 2000)
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingResultsRunnable = null
            isCancelled = true
            hasDeliveredFinal = true
            lastPartialText = ""
            stopHardwareRecording(deliverResult = false)
            try {
                speechRecognizer?.cancel()
            } catch (_: Throwable) {
            }
            isListening = false
            onStateChange(false)
            onRmsChanged(0f)
            onPartialResult("")
        }
    }

    fun destroy() {
        mainHandler.post {
            pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingResultsRunnable = null
            stopHardwareRecording(deliverResult = false)
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                isListening = false
            } catch (_: Throwable) {
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onStateChange(true)
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB typically ranges from -2 to 10
                onRmsChanged(max(0f, rmsdB))
            }

            override fun onBufferReceived(buffer: ByteArray?) {
            }

            override fun onEndOfSpeech() {
                // Voice activity ceased: engine is now computing results.
                // Keep isListening = true so UI doesn't drop back to idle and user doesn't collide by pressing again.
                if (lastPartialText.isNotBlank() && !hasDeliveredFinal) {
                    pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
                    val runnable = Runnable {
                        if (lastPartialText.isNotBlank() && !hasDeliveredFinal) {
                            hasDeliveredFinal = true
                            val text = lastPartialText.trim()
                            lastPartialText = ""
                            isListening = false
                            onStateChange(false)
                            onFinalResult(text)
                        }
                    }
                    pendingResultsRunnable = runnable
                    mainHandler.postDelayed(runnable, 3000)
                }
            }

            override fun onError(error: Int) {
                pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
                pendingResultsRunnable = null

                if (isCancelled || hasDeliveredFinal) {
                    isListening = false
                    onStateChange(false)
                    onRmsChanged(0f)
                    lastPartialText = ""
                    return
                }

                // If we already have partial text captured, deliver it before erroring out
                if (lastPartialText.isNotBlank()) {
                    isListening = false
                    onStateChange(false)
                    onRmsChanged(0f)
                    val finalCandidate = lastPartialText.trim()
                    lastPartialText = ""
                    hasDeliveredFinal = true
                    onFinalResult(finalCandidate)
                    return
                }

                // If Android's SpeechRecognizer service failed due to client/permission/service issues,
                // seamlessly switch to hardware MediaRecorder fallback so the mic ALWAYS works!
                val isServiceFailure = error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                    error == SpeechRecognizer.ERROR_AUDIO ||
                    error == SpeechRecognizer.ERROR_SERVER ||
                    error == 11 || // ERROR_SERVER_DISCONNECTED
                    error == 12 || // ERROR_LANGUAGE_NOT_SUPPORTED
                    error == 13    // ERROR_LANGUAGE_UNAVAILABLE

                if (isServiceFailure && onAudioRecorded != null) {
                    Log.w("SpeechHelper", "SpeechRecognizer error ($error) -> switching to Hardware MediaRecorder fallback")
                    useHardwareRecorderFallback = true
                    mainHandler.postDelayed({
                        try {
                            speechRecognizer?.destroy()
                        } catch (_: Throwable) {}
                        speechRecognizer = null
                    }, 250)
                    startHardwareRecording()
                    return
                }

                isListening = false
                onStateChange(false)
                onRmsChanged(0f)

                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio recording permission missing"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    else -> "Recognition error ($error)"
                }

                // Ignore quiet timeouts or no matches smoothly without spamming toast
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    onError(message)
                }
            }

            override fun onResults(results: Bundle?) {
                pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
                pendingResultsRunnable = null
                isListening = false
                onStateChange(false)
                onRmsChanged(0f)

                if (isCancelled || hasDeliveredFinal) {
                    lastPartialText = ""
                    return
                }
                hasDeliveredFinal = true

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim() ?: lastPartialText.trim()
                lastPartialText = ""
                if (recognizedText.isNotBlank()) {
                    onFinalResult(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    lastPartialText = text
                    onPartialResult(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
            }
        }
    }
}
