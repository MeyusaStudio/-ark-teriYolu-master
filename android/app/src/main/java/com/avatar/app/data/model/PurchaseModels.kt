package com.avatar.app.data.model

import com.google.gson.annotations.SerializedName

data class MarketPurchaseItem(
    val id: Int,
    @SerializedName("market_id") val marketId: Int,
    @SerializedName("market_name") val marketName: String? = null,
    @SerializedName("market_phone") val marketPhone: String? = null,
    @SerializedName("market_address") val marketAddress: String? = null,
    @SerializedName("farmer_id") val farmerId: Int,
    @SerializedName("farmer_name") val farmerName: String? = null,
    @SerializedName("farmer_phone") val farmerPhone: String? = null,
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("lot_id") val lotId: Int,
    @SerializedName("lot_number") val lotNumber: String,
    @SerializedName("lot_status") val lotStatus: String? = "active",
    val quantity: Double,
    val unit: String,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_price") val totalPrice: Double,
    val currency: String? = "EUR",
    val status: String,
    @SerializedName("purchase_date") val purchaseDate: String,
    @SerializedName("complaint_count") val complaintCount: Int? = 0,
    @SerializedName("latest_complaint") val latestComplaint: String? = null
)

data class CreatePurchaseRequest(
    @SerializedName("farmer_id") val farmerId: Int,
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("lot_id") val lotId: Int,
    val quantity: Double,
    val unit: String,
    @SerializedName("unit_price") val unitPrice: Double
)

data class ComplaintItem(
    val id: Int,
    val title: String,
    val message: String,
    @SerializedName("lot_id") val lotId: Int,
    @SerializedName("lot_number") val lotNumber: String,
    @SerializedName("lot_status") val lotStatus: String? = null,
    @SerializedName("lot_quantity") val lotQuantity: Double? = null,
    @SerializedName("lot_unit") val lotUnit: String? = null,
    @SerializedName("is_read") val isRead: Any? = false,
    @SerializedName("created_at") val createdAt: String
)

data class SubmitFeedbackRequest(
    @SerializedName("lot_number") val lotNumber: String,
    @SerializedName("issue_description") val issueDescription: String,
    @SerializedName("farmer_id") val farmerId: Int? = null,
    @SerializedName("lot_id") val lotId: Int? = null
)
