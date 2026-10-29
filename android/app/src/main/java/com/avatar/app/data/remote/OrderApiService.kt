package com.avatar.app.data.remote

import com.avatar.app.data.model.CreateOrderRequest
import com.avatar.app.data.model.Order
import com.avatar.app.data.model.OrderDetailResponse
import com.avatar.app.data.model.UpdateOrderStatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface OrderApiService {
    @GET("orders")
    suspend fun getOrders(): Response<List<Order>>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: Int): Response<OrderDetailResponse>

    @POST("orders")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<Map<String, Any>>

    @PATCH("orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") id: Int,
        @Body request: UpdateOrderStatusRequest
    ): Response<Map<String, Any>>
}
