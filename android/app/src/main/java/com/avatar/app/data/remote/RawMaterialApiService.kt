package com.avatar.app.data.remote

import com.avatar.app.data.model.CreateRawMaterialRequest
import com.avatar.app.data.model.CreateRawMaterialResponse
import com.avatar.app.data.model.RawMaterial
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RawMaterialApiService {
    @GET("raw-materials")
    suspend fun getMaterials(): Response<List<RawMaterial>>

    @POST("raw-materials")
    suspend fun createMaterial(
        @Body request: CreateRawMaterialRequest
    ): Response<CreateRawMaterialResponse>

    @DELETE("raw-materials/{id}")
    suspend fun deleteMaterial(
        @Path("id") id: Int
    ): Response<Map<String, String>>
}
