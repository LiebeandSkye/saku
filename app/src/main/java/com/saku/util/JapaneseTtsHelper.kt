package com.saku.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class JapaneseTtsHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.JAPANESE)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setSpeechRate(1.0f)
                        isInitialized = true
                    }
                }
            }
        } catch (ignored: Exception) {
            isInitialized = false
        }
    }

    fun speak(
        text: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || text.isBlank()) {
            mainHandler.post { onDone?.invoke() }
            return
        }
        try {
            val utteranceId = "SakuTTS_${System.currentTimeMillis()}"
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    if (id == utteranceId) {
                        mainHandler.post { onStart?.invoke() }
                    }
                }
                override fun onDone(id: String?) {
                    if (id == utteranceId) {
                        mainHandler.post { onDone?.invoke() }
                    }
                }
                override fun onError(id: String?) {
                    if (id == utteranceId) {
                        mainHandler.post { onDone?.invoke() }
                    }
                }
            })
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (ignored: Exception) {
            mainHandler.post { onDone?.invoke() }
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (ignored: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (ignored: Exception) {}
    }
}
