package com.avatar.app.data.remote

import com.avatar.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AdminApiService {
    @GET("/api/admin/users")
    suspend fun getUsers(
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
        @Query("q") query: String? = null
    ): Response<AdminUsersResponse>

    @PATCH("/api/admin/users/{id}/ban")
    suspend fun banUser(
        @Path("id") id: Int,
        @Body request: BanUserRequest
    ): Response<BanUserResponse>

    @POST("/api/admin/notifications")
    suspend fun sendNotification(
        @Body request: SendAdminNotificationRequest
    ): Response<SendAdminNotificationResponse>

    @GET("/api/admin/stats")
    suspend fun getSystemStats(): Response<SystemStatsResponse>
}
