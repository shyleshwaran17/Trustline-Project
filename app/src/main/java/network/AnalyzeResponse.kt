package com.trustline.ai.network

data class AnalyzeResponse(
    val transcript: String,
    val trust_score: Int,
    val prediction: String,
    val confidence: Double,

    val risk_override: Boolean = false,

    val detected: List<String>,

    val rule_score: Int,
    val ai_prediction: String,
    val ai_score: Double,

    val voice_prediction: String,
    val voice_label: String,
    val voice_confidence: Double,
    val voice_score: Double,

    val risk_reasons: List<String>? = emptyList(),
    val voice_analysis: String? = null
)