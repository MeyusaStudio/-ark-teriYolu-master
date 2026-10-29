package com.avatar.app.data.remote

import com.avatar.app.data.model.CreateRecallRequest
import com.avatar.app.data.model.CreateRecallResponse
import com.avatar.app.data.model.RecallItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface RecallApiService {
    @GET("recalls")
    suspend fun getRecalls(): Response<List<RecallItem>>

    @POST("recalls")
    suspend fun initiateRecall(
        @Body request: CreateRecallRequest
    ): Response<CreateRecallResponse>

    @PATCH("recalls/{id}/complete")
    suspend fun completeRecall(
        @Path("id") id: Int
    ): Response<Map<String, String>>
}
