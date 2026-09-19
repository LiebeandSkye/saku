package com.saku.speak

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerHelper(
    context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onStateChange: (isListening: Boolean) -> Unit,
    private val onError: (String) -> Unit
) {

    private val appContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var lastPartialText = ""
    private var isCancelled = false
    private var pendingResultsRunnable: Runnable? = null
    private var hasDeliveredFinal = false

    fun isAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(appContext)
        } catch (_: Throwable) {
            false
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

        val validServices = resolveInfos.mapNotNull { it.serviceInfo }

        // 1. Google App (QuickSearchBox) Recognition Service
        val googleAppRecognition = validServices.firstOrNull {
            it.packageName == "com.google.android.googlequicksearchbox" &&
            it.name.contains("RecognitionService", ignoreCase = true)
        }
        if (googleAppRecognition != null) {
            return ComponentName(googleAppRecognition.packageName, googleAppRecognition.name)
        }

        // 2. OEM or other third-party recognition services (exclude tts and android system intelligence)
        val oemService = validServices.firstOrNull {
            !it.packageName.startsWith("com.google.android.as") &&
            !it.packageName.startsWith("com.google.android.tts")
        }
        if (oemService != null) {
            return ComponentName(oemService.packageName, oemService.name)
        }

        return null
    }

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer

        if (!isAvailable()) {
            onError("Speech recognition service is not available on this device.")
            return null
        }

        // Strategy 1: Default SpeechRecognizer (uses Android system configured recognition service)
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
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

        // Strategy 2: Fallback to verified dedicated service (Google Search / OEM) if default failed
        val bestComponent = findBestRecognitionService(appContext)
        if (bestComponent != null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext, bestComponent).apply {
                    setRecognitionListener(createListener())
                }
                Log.d("SpeechHelper", "SpeechRecognizer initialized with component: ${bestComponent.flattenToShortString()}")
                return speechRecognizer
            } catch (t: Throwable) {
                Log.e("SpeechHelper", "Failed to create recognizer with component $bestComponent: ${t.message}")
                try {
                    speechRecognizer?.destroy()
                } catch (_: Throwable) {}
                speechRecognizer = null
            }
        }

        onError("Speech recognition engine could not be initialized.")
        return null
    }

    fun startListening(languageCode: String = "ja-JP") {
        mainHandler.post {
            pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingResultsRunnable = null
            isCancelled = false
            hasDeliveredFinal = false
            lastPartialText = ""

            if (!isAvailable()) {
                isListening = false
                onStateChange(false)
                onError("Speech recognition is unavailable on this device. Please use typing mode.")
                return@post
            }

            val recognizer = ensureRecognizer() ?: return@post

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
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
                Log.e("SpeechHelper", "Exception during startListening", t)
                isListening = false
                onStateChange(false)
                try {
                    speechRecognizer?.destroy()
                } catch (_: Throwable) {}
                speechRecognizer = null
                onError("Failed to start listening: ${t.localizedMessage ?: "Service error"}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Throwable) {
            }
            // Fallback watchdog: only fire if engine stalls for >2.5s and partial speech was captured
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
                mainHandler.postDelayed(runnable, 2500)
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
            try {
                speechRecognizer?.cancel()
            } catch (_: Throwable) {
            }
            isListening = false
            onStateChange(false)
            onPartialResult("")
        }
    }

    fun destroy() {
        mainHandler.post {
            pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingResultsRunnable = null
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
                onRmsChanged(rmsdB)
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
                    mainHandler.postDelayed(runnable, 3500)
                }
            }

            override fun onError(error: Int) {
                pendingResultsRunnable?.let { mainHandler.removeCallbacks(it) }
                pendingResultsRunnable = null
                isListening = false
                onStateChange(false)
                onRmsChanged(0f)

                if (isCancelled || hasDeliveredFinal) {
                    lastPartialText = ""
                    return
                }

                // Asynchronously reset recognizer instance on unrecoverable client/busy state
                // Never call destroy() synchronously within the onError callback to prevent Binder re-entrancy crashes
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                    mainHandler.postDelayed({
                        try {
                            speechRecognizer?.destroy()
                        } catch (_: Throwable) {}
                        speechRecognizer = null
                    }, 350)
                }

                // If we already have partial text captured, deliver it before erroring out
                if (lastPartialText.isNotBlank()) {
                    val finalCandidate = lastPartialText.trim()
                    lastPartialText = ""
                    hasDeliveredFinal = true
                    onFinalResult(finalCandidate)
                    return
                }

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
