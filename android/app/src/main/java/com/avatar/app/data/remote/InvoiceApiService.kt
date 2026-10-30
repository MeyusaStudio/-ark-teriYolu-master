package com.avatar.app.data.remote

import com.avatar.app.data.model.Invoice
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface InvoiceApiService {
    @GET("invoices")
    suspend fun getInvoices(
        @Query("type") type: String? = null,
        @Query("reference_type") referenceType: String? = null,
        @Query("reference_id") referenceId: Int? = null,
        @Query("order_id") orderId: Int? = null
    ): Response<List<Invoice>>

    @GET("invoices/{id}")
    suspend fun getInvoiceById(
        @Path("id") id: Int
    ): Response<Invoice>
}
