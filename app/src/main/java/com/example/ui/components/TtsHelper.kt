package com.example.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsHelper", "Language not supported")
            } else {
                isReady = true
            }
        } else {
            Log.e("TtsHelper", "TTS Initialization failed")
        }
    }

    fun speak(text: String, languageCode: String = "en") {
        if (!isReady || tts == null) return

        val locale = when (languageCode.lowercase()) {
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "es" -> Locale("es", "ES")
            "tr" -> Locale("tr", "TR")
            else -> Locale.US
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AdibiIELTSTtsUtterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
