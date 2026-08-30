package com.trustline.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TrustLineTts(context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isReady = false

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            textToSpeech?.language = Locale.US
            isReady = true
        }
    }

    fun speak(message: String) {

        if (isReady) {

            textToSpeech?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "TRUSTLINE_ALERT"
            )
        }
    }

    fun stop() {

        textToSpeech?.stop()
    }

    fun shutdown() {

        textToSpeech?.stop()
        textToSpeech?.shutdown()

        isReady = false
    }
}