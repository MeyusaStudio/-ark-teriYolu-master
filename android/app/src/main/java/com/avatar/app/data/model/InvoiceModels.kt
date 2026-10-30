package com.avatar.app.data.model

import com.google.gson.annotations.SerializedName

data class Invoice(
    val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("invoice_type") val invoiceType: String, // "hizmet" (fason), "satis" (mamul satisi)
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("reference_type") val referenceType: String? = null,
    @SerializedName("reference_id") val referenceId: Int? = null,
    @SerializedName("lot_number") val lotNumber: String? = null,
    @SerializedName("product_name") val productName: String? = null,
    val description: String? = null,
    val quantity: Double = 0.0,
    val unit: String = "adet",
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0,
    @SerializedName("tax_rate") val taxRate: Double = 0.0,
    @SerializedName("tax_amount") val taxAmount: Double = 0.0,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    val currency: String = "EUR",
    val status: String = "odendi", // "odendi", "bekliyor", "iptal"
    @SerializedName("invoice_date") val invoiceDate: String,
    @SerializedName("sender_name") val senderName: String? = null,
    @SerializedName("sender_email") val senderEmail: String? = null,
    @SerializedName("sender_role") val senderRole: String? = null,
    @SerializedName("receiver_name") val receiverName: String? = null,
    @SerializedName("receiver_email") val receiverEmail: String? = null,
    @SerializedName("receiver_role") val receiverRole: String? = null
)

data class OcrAnalyzeRequest(
    val sampleId: String? = null,
    val rawText: String? = null,
    val fileName: String? = null
)

data class OcrExtractionResult(
    @SerializedName("supplier_name") val supplierName: String,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("invoice_date") val invoiceDate: String,
    @SerializedName("material_type") val materialType: String,
    @SerializedName("material_label") val materialLabel: String,
    val quantity: Double,
    val unit: String,
    @SerializedName("unit_price") val unitPrice: Double,
    val currency: String? = "EUR",
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("lot_number") val lotNumber: String,
    @SerializedName("expiry_date") val expiryDate: String? = null,
    val confidence: Double = 0.95,
    @SerializedName("detected_text") val detectedText: String? = null
)

data class OcrAnalyzeResponse(
    val success: Boolean,
    val source: String? = null,
    val data: OcrExtractionResult
)
