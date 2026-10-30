package com.avatar.app.data.remote

import com.avatar.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ListingApiService {
    @GET("listings/producer")
    suspend fun getProducerListings(): Response<List<ProducerListing>>

    @GET("listings/producer/mine")
    suspend fun getMyProducerListings(): Response<List<ProducerListing>>

    @POST("listings/producer")
    suspend fun createProducerListing(
        @Body request: CreateProducerListingRequest
    ): Response<Map<String, Any>>

    @PUT("listings/producer/{id}")
    suspend fun updateProducerListing(
        @Path("id") id: Int,
        @Body request: Map<String, Any?>
    ): Response<Map<String, Any>>

    @POST("listings/producer/{id}/toggle")
    suspend fun toggleProducerListing(
        @Path("id") id: Int,
        @Body body: Map<String, String> = emptyMap()
    ): Response<Map<String, Any>>

    @GET("listings/farmer")
    suspend fun getFarmerListings(): Response<List<FarmerListing>>

    @GET("listings/farmer/mine")
    suspend fun getMyFarmerListings(): Response<List<FarmerListing>>

    @POST("listings/farmer")
    suspend fun createFarmerListing(
        @Body request: CreateFarmerListingRequest
    ): Response<Map<String, Any>>

    @PUT("listings/farmer/{id}")
    suspend fun updateFarmerListing(
        @Path("id") id: Int,
        @Body request: Map<String, Any?>
    ): Response<Map<String, Any>>

    @POST("listings/farmer/{id}/toggle")
    suspend fun toggleFarmerListing(
        @Path("id") id: Int,
        @Body body: Map<String, String> = emptyMap()
    ): Response<Map<String, Any>>
}
