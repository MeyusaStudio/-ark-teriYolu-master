package com.avatar.app.data.model

import com.google.gson.annotations.SerializedName

// ── ML PARÇALAMA MODELLERİ (Butchery Predict) ──

data class ButcheryPredictRequest(
    @SerializedName("animal_type")
    val animalType: String,
    @SerializedName("entry_weight_kg")
    val entryWeightKg: Double,
    @SerializedName("purchase_cost")
    val purchaseCost: Double = 0.0
)

data class MLPartPrediction(
    @SerializedName("part_name")
    val partName: String,
    @SerializedName("predicted_weight_kg")
    val predictedWeightKg: Double,
    @SerializedName("expected_ratio")
    val expectedRatio: Double,
    @SerializedName("allocated_cost")
    val allocatedCost: Double,
    @SerializedName("unit_cost")
    val unitCost: Double
)

data class MLButcheryResult(
    @SerializedName("animal_type")
    val animalType: String,
    @SerializedName("entry_weight_kg")
    val entryWeightKg: Double,
    @SerializedName("parts")
    val parts: List<MLPartPrediction>,
    @SerializedName("predicted_waste_kg")
    val predictedWasteKg: Double,
    @SerializedName("total_predicted_kg")
    val totalPredictedKg: Double,
    @SerializedName("sample_count")
    val sampleCount: Int,
    @SerializedName("method")
    val method: String,
    @SerializedName("notes")
    val notes: String?
)

// ── ML ÜRETİM & EKP MODELLERİ (Production Predict) ──

data class ProductionPredictRequest(
    @SerializedName("product_name")
    val productName: String,
    @SerializedName("producer_id")
    val producerId: Int?,
    @SerializedName("service_price_per_unit")
    val servicePricePerUnit: Double,
    @SerializedName("materials")
    val materials: Map<String, Double>?
)

data class MLProductionResult(
    @SerializedName("predicted_packages")
    val predictedPackages: Int?,
    @SerializedName("package_weight")
    val packageWeight: Double?,
    @SerializedName("total_input_kg")
    val totalInputKg: Double?,
    @SerializedName("predicted_fire_kg")
    val predictedFireKg: Double?,
    @SerializedName("predicted_fire_rate")
    val predictedFireRate: Double?,
    @SerializedName("predicted_qty_ratio")
    val predictedQtyRatio: Double = 0.85,
    @SerializedName("predicted_ekp")
    val predictedEkp: Double = 0.0,
    @SerializedName("confidence")
    val confidence: Double = 0.0,
    @SerializedName("sample_count")
    val sampleCount: Int = 0,
    @SerializedName("method")
    val method: String = "rule_based",
    @SerializedName("notes")
    val notes: String? = null
)
