package com.avatar.app.data.model

import com.google.gson.annotations.SerializedName

data class Order(
    val id: Int,
    @SerializedName("buyer_id") val buyerId: Int,
    @SerializedName("buyer_name") val buyerName: String? = null,
    @SerializedName("seller_id") val sellerId: Int,
    @SerializedName("seller_name") val sellerName: String? = null,
    @SerializedName("seller_phone") val sellerPhone: String? = null,
    @SerializedName("seller_address") val sellerAddress: String? = null,
    @SerializedName("listing_id") val listingId: Int? = null,
    @SerializedName("product_name") val productName: String,
    @SerializedName("expected_qty") val expectedQty: Double,
    val status: String, // "beklemede", "onaylandi", "hammadde_gonderildi", "uretimde", "tamamlandi", "iptal"
    @SerializedName("order_date") val orderDate: String,
    val notes: String? = null,
    val rawMaterials: List<OrderRawMaterial>? = emptyList()
)

data class OrderRawMaterial(
    val id: Int? = null,
    @SerializedName("material_type") val materialType: String,
    val quantity: Double,
    val unit: String,
    @SerializedName("unit_price") val unitPrice: Double? = 0.0,
    @SerializedName("total_cost") val totalCost: Double? = 0.0,
    @SerializedName("lot_id") val lotId: Int? = null,
    @SerializedName("lot_number") val lotNumber: String? = null
)

data class OrderRawMaterialInput(
    @SerializedName("material_type") val materialType: String,
    val quantity: Double,
    val unit: String,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("lot_id") val lotId: Int? = null
)

data class CreateOrderRequest(
    @SerializedName("seller_id") val sellerId: Int,
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("expected_qty") val expectedQty: Double,
    val notes: String? = null,
    val rawMaterials: List<OrderRawMaterialInput>
)

data class UpdateOrderStatusRequest(
    val status: String
)

data class AvailablePart(
    @SerializedName("part_id") val partId: Int,
    @SerializedName("part_name") val partName: String,
    @SerializedName("animal_type") val animalType: String,
    @SerializedName("operation_date") val operationDate: String,
    @SerializedName("lot_id") val lotId: Int,
    @SerializedName("lot_number") val lotNumber: String,
    @SerializedName("available_kg") val availableKg: Double
)

data class OrderDetailResponse(
    val order: Order,
    val rawMaterials: List<OrderRawMaterial> = emptyList()
)
