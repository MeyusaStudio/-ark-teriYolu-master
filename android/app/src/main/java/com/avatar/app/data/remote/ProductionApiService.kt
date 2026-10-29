package com.avatar.app.data.remote

import com.avatar.app.data.model.CompleteProductionRequest
import com.avatar.app.data.model.ProductionCompletionResult
import com.avatar.app.data.model.ProductionDetailResponse
import com.avatar.app.data.model.ProductionOrder
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ProductionApiService {
    @GET("production")
    suspend fun getProductionOrders(): Response<List<ProductionOrder>>

    @GET("production/{id}")
    suspend fun getProductionById(@Path("id") id: Int): Response<ProductionDetailResponse>

    @PATCH("production/{id}/complete")
    suspend fun completeProduction(
        @Path("id") id: Int,
        @Body request: CompleteProductionRequest
    ): Response<ProductionCompletionResult>
}
