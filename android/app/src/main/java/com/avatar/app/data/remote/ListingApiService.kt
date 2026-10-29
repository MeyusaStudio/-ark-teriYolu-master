package com.avatar.app.data.remote

import com.avatar.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ListingApiService {
    @GET("listings/producer")
    suspend fun getProducerListings(): Response<List<ProducerListing>>

    @GET("listings/producer/mine")
    suspend fun getMyProducerListings(): Response<List<ProducerListing>>

    @POST("listings/producer")
    suspend fun createProducerListing(
        @Body request: CreateProducerListingRequest
    ): Response<Map<String, Any>>

    @GET("listings/farmer")
    suspend fun getFarmerListings(): Response<List<FarmerListing>>

    @GET("listings/farmer/mine")
    suspend fun getMyFarmerListings(): Response<List<FarmerListing>>

    @POST("listings/farmer")
    suspend fun createFarmerListing(
        @Body request: CreateFarmerListingRequest
    ): Response<Map<String, Any>>
}
