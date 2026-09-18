package com.saku.speak

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognizerHelper(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onStateChange: (isListening: Boolean) -> Unit,
    private val onError: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var lastPartialText = ""
    private var isCancelled = false

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } catch (e: Exception) {
                onError("Failed to initialize speech recognizer: ${e.localizedMessage}")
                return null
            }
        }
        return speechRecognizer
    }

    fun startListening(languageCode: String = "ja-JP") {
        mainHandler.post {
            isCancelled = false
            lastPartialText = ""
            val recognizer = ensureRecognizer() ?: return@post

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            try {
                recognizer.startListening(intent)
                isListening = true
                onStateChange(true)
            } catch (e: Exception) {
                isListening = false
                onStateChange(false)
                onError("Failed to start listening: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (ignored: Exception) {
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            isCancelled = true
            lastPartialText = ""
            try {
                speechRecognizer?.cancel()
            } catch (ignored: Exception) {
            }
            isListening = false
            onStateChange(false)
            onPartialResult("")
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                isListening = false
            } catch (ignored: Exception) {
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
                isListening = false
                onStateChange(false)
            }

            override fun onError(error: Int) {
                isListening = false
                onStateChange(false)
                onRmsChanged(0f)

                if (isCancelled) {
                    lastPartialText = ""
                    return
                }

                // Reset recognizer instance on unrecoverable client/busy state
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                    try {
                        speechRecognizer?.destroy()
                    } catch (ignored: Exception) {}
                    speechRecognizer = null
                }

                // If we already have partial text captured, deliver it before erroring out
                if (lastPartialText.isNotBlank()) {
                    val finalCandidate = lastPartialText.trim()
                    lastPartialText = ""
                    onFinalResult(finalCandidate)
                    return
                }

                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
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
                isListening = false
                onStateChange(false)
                onRmsChanged(0f)

                if (isCancelled) {
                    lastPartialText = ""
                    return
                }

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
