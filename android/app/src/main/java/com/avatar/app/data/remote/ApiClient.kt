package com.avatar.app.data.remote

import android.content.Context
import com.avatar.app.data.local.SessionManager
import com.avatar.app.data.model.ApiError
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {
    private val sessionManager = SessionManager(context)
    private val gson = Gson()

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        val token = sessionManager.getToken()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Accept", "application/json")
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

    val retrofit: Retrofit
        get() = Retrofit.Builder()
            .baseUrl(sessionManager.getServerUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val authService: AuthApiService
        get() = retrofit.create(AuthApiService::class.java)

    val lotService: LotApiService
        get() = retrofit.create(LotApiService::class.java)

    val butcheryService: ButcheryApiService
        get() = retrofit.create(ButcheryApiService::class.java)

    val rawMaterialService: RawMaterialApiService
        get() = retrofit.create(RawMaterialApiService::class.java)

    val recallService: RecallApiService
        get() = retrofit.create(RecallApiService::class.java)

    val listingService: ListingApiService
        get() = retrofit.create(ListingApiService::class.java)

    val orderService: OrderApiService
        get() = retrofit.create(OrderApiService::class.java)

    val productionService: ProductionApiService
        get() = retrofit.create(ProductionApiService::class.java)

    val mlService: MlApiService
        get() = retrofit.create(MlApiService::class.java)

    val purchaseService: PurchaseApiService
        get() = retrofit.create(PurchaseApiService::class.java)

    val invoiceService: InvoiceApiService
        get() = retrofit.create(InvoiceApiService::class.java)

    val ocrService: OcrApiService
        get() = retrofit.create(OcrApiService::class.java)

    val adminService: AdminApiService
        get() = retrofit.create(AdminApiService::class.java)

    val notificationService: NotificationApiService
        get() = retrofit.create(NotificationApiService::class.java)

    fun parseError(responseBodyString: String?): String {
        if (responseBodyString.isNullOrBlank()) return "Bilinmeyen bir hata oluştu"
        return try {
            val err = gson.fromJson(responseBodyString, ApiError::class.java)
            err.detail ?: err.error ?: responseBodyString
        } catch (_: Exception) {
            responseBodyString
        }
    }
}
