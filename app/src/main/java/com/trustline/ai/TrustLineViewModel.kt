package com.trustline.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class TrustLineViewModel : ViewModel() {

    var trustScore by mutableStateOf(85)
        private set

    var prediction by mutableStateOf("Waiting...")
        private set

    var confidence by mutableStateOf(0.0)
        private set

    var transcript by mutableStateOf("Waiting for call audio...")
        private set

    var isRecording by mutableStateOf(false)
        private set

    fun startRecording() {
        isRecording = true
    }

    fun stopRecording() {
        isRecording = false
    }

    fun updateResult(
        score: Int,
        pred: String,
        conf: Double,
        text: String
    ) {
        trustScore = score
        prediction = pred
        confidence = conf

        if (text.isNotBlank()) {

            if (transcript == "Waiting for call audio...") {
                transcript = text.trim()
            } else {
                transcript = "$transcript\n\n${text.trim()}"
            }
        }
    }
}