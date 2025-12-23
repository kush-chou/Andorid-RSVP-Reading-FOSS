package com.example.fossrsvp

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Try to set language to default, fallback to US
                val result = tts?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to US if default is not supported
                     val usResult = tts?.setLanguage(Locale.US)
                     if (usResult != TextToSpeech.LANG_MISSING_DATA && usResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                         isInitialized = true
                     }
                } else {
                    isInitialized = true
                }
            }
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            // QUEUE_FLUSH drops all pending entries in the playback queue.
            // This ensures immediate playback of the current word.
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
