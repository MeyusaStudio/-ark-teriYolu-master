package com.avatar.app.data.remote

import com.avatar.app.data.model.ButcheryDetailResponse
import com.avatar.app.data.model.ButcheryRecord
import com.avatar.app.data.model.CreateButcheryRequest
import com.avatar.app.data.model.CreateButcheryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ButcheryApiService {
    @GET("butchery")
    suspend fun getOperations(): Response<List<ButcheryRecord>>

    @GET("butchery/parts/available")
    suspend fun getAvailableParts(): Response<List<com.avatar.app.data.model.AvailablePart>>

    @GET("butchery/{id}")
    suspend fun getOperationById(@Path("id") id: Int): Response<ButcheryDetailResponse>

    @POST("butchery")
    suspend fun createOperation(
        @Body request: CreateButcheryRequest
    ): Response<CreateButcheryResponse>
}
