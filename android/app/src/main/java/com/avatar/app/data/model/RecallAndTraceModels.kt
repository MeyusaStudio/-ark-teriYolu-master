package com.avatar.app.data.model

import com.google.gson.annotations.SerializedName

data class RecallItem(
    val id: Int,
    @SerializedName("initiated_by") val initiatedBy: Int,
    @SerializedName("lot_id") val lotId: Int,
    val reason: String,
    val status: String, // "aktif", "tamamlandi"
    @SerializedName("affected_count") val affectedCount: Int? = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("lot_number") val lotNumber: String? = null,
    @SerializedName("initiated_by_name") val initiatedByName: String? = null
)

data class CreateRecallRequest(
    @SerializedName("lot_id") val lotId: Int,
    val reason: String
)

data class CreateRecallResponse(
    val message: String,
    val recallId: Int?,
    val affectedLotCount: Int?,
    val affectedMarketCount: Int?
)

data class TraceSummary(
    val productionOrderCount: Int? = 0,
    val finishedLotCount: Int? = 0,
    val affectedMarketCount: Int? = 0,
    val rawMaterialLotCount: Int? = 0,
    val suppliers: List<String>? = emptyList()
)

data class MarketSaleTrace(
    val id: Int,
    @SerializedName("market_id") val marketId: Int,
    @SerializedName("market_name") val marketName: String?,
    @SerializedName("market_address") val marketAddress: String?,
    @SerializedName("market_phone") val marketPhone: String?,
    val quantity: Double,
    val unit: String,
    @SerializedName("lot_number") val lotNumber: String?,
    val status: String? = "satildi"
)

data class TraceForwardResponse(
    val sourceLot: LotItem?,
    val productionOrders: List<Any>? = emptyList(),
    val finishedLots: List<LotItem>? = emptyList(),
    val marketSales: List<MarketSaleTrace>? = emptyList(),
    val summary: TraceSummary? = null
)

data class RawMaterialTraceItem(
    val id: Int,
    @SerializedName("lot_number") val lotNumber: String,
    @SerializedName("material_type") val materialType: String?,
    @SerializedName("quantity_used") val quantityUsed: Double?,
    val unit: String?,
    @SerializedName("supplier_name") val supplierName: String?,
    @SerializedName("unit_price") val unitPrice: Double?
)

data class TraceBackwardResponse(
    val finishedLot: LotItem?,
    val productionOrders: List<Any>? = emptyList(),
    val rawMaterialLots: List<RawMaterialTraceItem>? = emptyList(),
    val summary: TraceSummary? = null
)
