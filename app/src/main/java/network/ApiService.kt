package com.trustline.ai.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("/analyze")
    suspend fun analyzeAudio(
        @Part audio: MultipartBody.Part
    ): Response<AnalyzeResponse>

}