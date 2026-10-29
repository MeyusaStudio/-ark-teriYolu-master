package com.avatar.app.data.remote

import com.avatar.app.data.model.ButcheryPredictRequest
import com.avatar.app.data.model.MLButcheryResult
import com.avatar.app.data.model.MLProductionResult
import com.avatar.app.data.model.ProductionPredictRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MlApiService {
    @POST("ml/predict/butchery")
    suspend fun predictButchery(
        @Body request: ButcheryPredictRequest
    ): Response<MLButcheryResult>

    @POST("ml/predict/production")
    suspend fun predictProduction(
        @Body request: ProductionPredictRequest
    ): Response<MLProductionResult>
}
