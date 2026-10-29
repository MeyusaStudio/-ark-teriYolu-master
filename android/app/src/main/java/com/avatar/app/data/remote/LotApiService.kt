package com.avatar.app.data.remote

import com.avatar.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LotApiService {
    @GET("lots")
    suspend fun getLots(): Response<List<LotItem>>

    @GET("lots/{id}")
    suspend fun getLotById(@Path("id") id: Int): Response<LotItem>

    @POST("lots/{id}/sell")
    suspend fun sellLot(
        @Path("id") id: Int,
        @Body request: SellLotRequest
    ): Response<SellLotResponse>

    @GET("lots/{id}/trace/forward")
    suspend fun traceForward(
        @Path("id") id: Int
    ): Response<TraceForwardResponse>

    @GET("lots/{id}/trace/backward")
    suspend fun traceBackward(
        @Path("id") id: Int
    ): Response<TraceBackwardResponse>
}
