package com.avatar.app.data.model

import com.google.gson.annotations.SerializedName

data class ProductionOrder(
    val id: Int,
    @SerializedName("order_id") val orderId: Int,
    @SerializedName("buyer_id") val buyerId: Int? = null,
    @SerializedName("buyer_name") val buyerName: String? = null,
    @SerializedName("production_number") val productionNumber: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("expected_qty") val expectedQty: Double,
    @SerializedName("actual_qty") val actualQty: Double? = null,
    @SerializedName("package_weight") val packageWeight: Double? = 1.0,
    @SerializedName("total_input_kg") val totalInputKg: Double? = null,
    @SerializedName("fire_qty") val fireQty: Double? = null,
    @SerializedName("fire_rate") val fireRate: Double? = null,
    @SerializedName("service_cost") val serviceCost: Double? = null,
    @SerializedName("total_raw_cost") val totalRawCost: Double? = null,
    val ekp: Double? = null,
    val status: String, // "beklemede", "uretimde", "tamamlandi", "sevk_edildi"
    @SerializedName("production_date") val productionDate: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CompleteProductionRequest(
    @SerializedName("actual_qty") val actualQty: Double,
    @SerializedName("service_cost") val serviceCost: Double,
    @SerializedName("unit_service_cost") val unitServiceCost: Double? = null,
    @SerializedName("production_date") val productionDate: String
)

data class ProductionRawMaterial(
    val id: Int? = null,
    @SerializedName("material_type") val materialType: String,
    @SerializedName("quantity_used") val quantityUsed: Double = 0.0,
    val quantity: Double = 0.0,
    val unit: String? = "kg",
    @SerializedName("unit_price") val unitPrice: Double? = 0.0,
    @SerializedName("total_cost") val totalCost: Double? = 0.0,
    @SerializedName("lot_id") val lotId: Int? = null,
    @SerializedName("lot_number") val lotNumber: String? = null
) {
    val effectiveQuantity: Double
        get() = if (quantityUsed > 0) quantityUsed else quantity
}

data class ProductionFinishedLot(
    val id: Int? = null,
    @SerializedName("lot_number") val lotNumber: String,
    val quantity: Double,
    val unit: String? = "paket"
)

data class ProductionCompletionResult(
    val message: String? = null,
    val lotNumber: String? = null,
    val actualQty: Double? = null,
    val packageWeight: Double? = 1.0,
    val totalInputKg: Double? = null,
    val fireQty: Double? = null,
    val fireRate: Double? = null,
    val totalRawCost: Double? = null,
    val serviceCost: Double? = null,
    val totalCost: Double? = null,
    val ekp: Double? = null,
    val vkp: Double? = null,
    val brutKar: Double? = null,
    val marj: Double? = null
)

data class ProductionDetailResponse(
    val productionOrder: ProductionOrder,
    val rawMaterials: List<ProductionRawMaterial>? = emptyList(),
    val finishedLots: List<ProductionFinishedLot>? = emptyList()
)
