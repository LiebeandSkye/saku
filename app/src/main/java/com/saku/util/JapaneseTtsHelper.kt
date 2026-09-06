package com.saku.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class JapaneseTtsHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.JAPANESE)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isInitialized = true
                    }
                }
            }
        } catch (ignored: Exception) {
            isInitialized = false
        }
    }

    fun speak(text: String) {
        if (!isInitialized || text.isBlank()) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SakuTTS_${System.currentTimeMillis()}")
        } catch (ignored: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (ignored: Exception) {}
    }
}
