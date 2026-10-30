package com.avatar.app.data.remote

import com.avatar.app.data.model.NotificationsResponse
import com.avatar.app.data.model.ReadNotificationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationApiService {
    @GET("/api/notifications")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @PATCH("/api/notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: Int): Response<ReadNotificationResponse>

    @POST("/api/notifications/read-all")
    suspend fun markAllAsRead(): Response<ReadNotificationResponse>
}
