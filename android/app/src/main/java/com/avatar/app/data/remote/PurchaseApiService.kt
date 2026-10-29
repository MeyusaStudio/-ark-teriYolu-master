package com.avatar.app.data.remote

import com.avatar.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PurchaseApiService {
    @GET("purchases")
    suspend fun getPurchases(): Response<List<MarketPurchaseItem>>

    @POST("purchases")
    suspend fun createPurchase(
        @Body request: CreatePurchaseRequest
    ): Response<Map<String, Any>>

    @GET("purchases/complaints")
    suspend fun getComplaints(): Response<List<ComplaintItem>>

    @POST("purchases/feedback")
    suspend fun submitFeedback(
        @Body request: SubmitFeedbackRequest
    ): Response<Map<String, Any>>
}
