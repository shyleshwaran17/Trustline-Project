package com.trustline.ai.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object UploadRepository {

    suspend fun uploadAudio(audioFile: File): AnalyzeResponse? {

        val requestFile = audioFile.asRequestBody(
            "audio/*".toMediaTypeOrNull()
        )

        val body = MultipartBody.Part.createFormData(
            "audio",
            audioFile.name,
            requestFile
        )

        val response = RetrofitClient.api.analyzeAudio(body)

        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }
}