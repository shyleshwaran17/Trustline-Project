package com.trustline.ai
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.os.SystemClock
class TrustLineViewModel(application: Application) : AndroidViewModel(application) {
    private val trustLineTts = TrustLineTts(application.applicationContext)
    // ---------------------------------
// TTS alert cooldown
// ---------------------------------

    private var lastAlertTime = 0L

    private val alertCooldownMillis = 15_000L
    // ---------------------------------
    // Main TrustLine result
    // ---------------------------------
    var monitoringState by mutableStateOf(MonitoringState.IDLE)
        private set
    fun startMonitoring() {
        monitoringState = MonitoringState.MONITORING
    }

    fun pauseMonitoring() {
        monitoringState = MonitoringState.PAUSED
    }

    fun resumeMonitoring() {
        monitoringState = MonitoringState.MONITORING
    }

    fun stopMonitoring() {
        monitoringState = MonitoringState.IDLE
    }

    fun setMonitoringError() {
        monitoringState = MonitoringState.ERROR
    }

    var trustScore by mutableStateOf(0)
        private set

    var prediction by mutableStateOf("Waiting for analysis...")
        private set
    var sessionRiskScore by mutableStateOf(85)
        private set

    var sessionRiskReasons by mutableStateOf<List<String>>(emptyList())
        private set
    var hasResult by mutableStateOf(false)
        private set
    var scamAlertPlayed by mutableStateOf(false)
        private set
    var confidence by mutableStateOf(0.0)
        private set
    var riskOverride by mutableStateOf(false)
        private set
    // ---------------------------------
// Elderly Accessibility Mode
// ---------------------------------

    var accessibilityMode by mutableStateOf(false)
        private set

    fun toggleAccessibilityMode() {
        accessibilityMode = !accessibilityMode
    }
    // ---------------------------------
    // Explainable scoring
    // ---------------------------------

    var ruleScore by mutableStateOf(100)
        private set

    var aiScore by mutableStateOf(100.0)
        private set

    var voiceScore by mutableStateOf(100.0)
        private set

    // ---------------------------------
    // Voice / deepfake analysis
    // ---------------------------------

    var voicePrediction by mutableStateOf("Waiting...")
        private set

    var voiceLabel by mutableStateOf("unknown")
        private set

    var voiceConfidence by mutableStateOf(0.0)
        private set

    var voiceAnalysis by mutableStateOf(
        "Waiting for voice analysis..."
    )
        private set

    // ---------------------------------
    // Explainable risk reasons
    // ---------------------------------

    var riskReasons by mutableStateOf<List<String>>(emptyList())
        private set

    // ---------------------------------
    // Transcript
    // ---------------------------------

    var transcript by mutableStateOf(
        "Waiting for call audio..."
    )
        private set

    // ---------------------------------
    // Recording state
    // ---------------------------------

    var isRecording by mutableStateOf(false)
        private set
// ---------------------------------
// Alert mute state
// ---------------------------------

    var areAlertsMuted by mutableStateOf(false)
        private set

    fun toggleAlertsMute() {
        areAlertsMuted = !areAlertsMuted
    }

    fun unmuteAlerts() {
        areAlertsMuted = false
    }
    fun startRecording() {
        isRecording = true
    }

    fun stopRecording() {
        isRecording = false
    }
    fun shouldPlayScamAlert(): Boolean {

        return hasResult &&
                trustScore <= 40 &&
                !scamAlertPlayed
    }
    fun markScamAlertPlayed() {
        scamAlertPlayed = true
    }
    // ---------------------------------
    // Update complete analysis result
    // ---------------------------------

    fun updateResult(
        score: Int,
        pred: String,
        conf: Double,
        text: String,
        newRiskOverride: Boolean = false,
        newRuleScore: Int = 100,
        newAiScore: Double = 100.0,
        newVoiceScore: Double = 100.0,
        newRiskReasons: List<String> = emptyList(),
        newVoicePrediction: String = "unknown",
        newVoiceLabel: String = "unknown",
        newVoiceConfidence: Double = 0.0,
        newVoiceAnalysis: String =
            "Voice analysis unavailable."
    ) {
        trustScore = score
        prediction = pred
        confidence = conf
        // ---------------------------------
// TrustLine voice safety alert
// ---------------------------------

        val currentTime = SystemClock.elapsedRealtime()

        if (
            !areAlertsMuted &&
            currentTime - lastAlertTime >= alertCooldownMillis
        ) {

            when {
                score < 40 -> {

                    val warningMessage = if (accessibilityMode) {
                        "Warning. This call may be a scam. Do not send money. Do not share your bank account, password, PIN, or OTP."
                    } else {
                        "Warning. Possible scam call detected. Do not share passwords, OTPs, or bank details."
                    }

                    trustLineTts.speak(warningMessage)

                    lastAlertTime = currentTime
                }

                score < 60 -> {

                    val warningMessage = if (accessibilityMode) {
                        "Warning. This call may be dangerous. Please stop and do not share personal or financial information."
                    } else {
                        "Warning. This conversation appears to be high risk. Please be careful."
                    }

                    trustLineTts.speak(warningMessage)

                    lastAlertTime = currentTime
                }

                score < 80 -> {

                    val warningMessage = if (accessibilityMode) {
                        "Caution. This call may be suspicious. Do not share personal information."
                    } else {
                        "Caution. This conversation may be suspicious."
                    }

                    trustLineTts.speak(warningMessage)

                    lastAlertTime = currentTime
                }
            }
        }
        riskOverride = newRiskOverride
        sessionRiskScore = minOf(
            sessionRiskScore,
            score
        )

        sessionRiskReasons = (
                sessionRiskReasons + newRiskReasons
                ).distinct()
        hasResult = true
        ruleScore = newRuleScore
        aiScore = newAiScore
        voiceScore = newVoiceScore

        riskReasons = newRiskReasons

        voicePrediction = newVoicePrediction
        voiceLabel = newVoiceLabel
        voiceConfidence = newVoiceConfidence
        voiceAnalysis = newVoiceAnalysis

        if (text.isNotBlank()) {

            if (transcript == "Waiting for call audio...") {
                transcript = text.trim()
            } else {
                transcript =
                    "$transcript\n\n${text.trim()}"
            }
        }
    }
    fun resetSession() {
        areAlertsMuted = false
        sessionRiskScore = 85

        sessionRiskReasons = emptyList()

        trustScore = 0

        prediction = "Waiting for analysis..."

        confidence = 0.0
        riskOverride = false
        hasResult = false

        ruleScore = 100
        aiScore = 100.0
        voiceScore = 100.0
        scamAlertPlayed = false
        riskReasons = emptyList()

        voicePrediction = "unknown"
        voiceLabel = "unknown"
        voiceConfidence = 0.0
        voiceAnalysis = "Voice analysis unavailable."

        transcript = "Waiting for call audio..."
    }
}