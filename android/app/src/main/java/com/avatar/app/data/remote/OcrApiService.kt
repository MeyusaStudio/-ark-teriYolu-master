package com.avatar.app.data.remote

import com.avatar.app.data.model.OcrAnalyzeRequest
import com.avatar.app.data.model.OcrAnalyzeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OcrApiService {
    @POST("ocr/analyze")
    suspend fun analyze(
        @Body request: OcrAnalyzeRequest
    ): Response<OcrAnalyzeResponse>
}
