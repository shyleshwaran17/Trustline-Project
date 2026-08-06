package com.trustline.ai.network

data class AnalyzeResponse(
    val transcript: String,
    val trust_score: Int,
    val prediction: String,
    val confidence: Double,
    val detected: List<String>
)