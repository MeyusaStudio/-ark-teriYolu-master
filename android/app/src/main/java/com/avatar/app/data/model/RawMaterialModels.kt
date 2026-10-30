package com.avatar.app.data.model

import com.google.gson.annotations.SerializedName

data class RawMaterial(
    val id: Int,
    @SerializedName("owner_id") val ownerId: Int? = null,
    @SerializedName("lot_id") val lotId: Int? = null,
    @SerializedName("material_type") val materialType: String,
    val quantity: Double,
    val unit: String,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_cost") val totalCost: Double,
    val currency: String? = "EUR",
    @SerializedName("lot_number") val lotNumber: String? = null,
    @SerializedName("lot_status") val lotStatus: String? = "active",
    @SerializedName("expiry_date") val expiryDate: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val isOfflinePending: Boolean = false
) {
    fun isMeat(): Boolean {
        val norm = materialType.lowercase().trim()
        return norm == "et" || norm.contains("et ") || norm.contains(" et") ||
                norm.contains("karkas") || norm.contains("kıyma") || norm.contains("kiyma") ||
                norm.contains("dana") || norm.contains("kuzu") || norm.contains("koyun") ||
                norm.contains("tavuk") || norm.contains("but") || norm.contains("kol") ||
                norm.contains("sırt") || norm.contains("antrikot") || norm.contains("bonfile") ||
                norm.contains("kontrfile") || norm.contains("kuşbaşı") || norm.contains("kusbasi")
    }

    fun toAvailablePart(): AvailablePart {
        val animal = if (materialType.contains("dana", ignoreCase = true) || materialType.contains("inek", ignoreCase = true)) "inek"
        else if (materialType.contains("kuzu", ignoreCase = true) || materialType.contains("koyun", ignoreCase = true)) "koyun"
        else if (materialType.contains("tavuk", ignoreCase = true)) "tavuk"
        else "Depo"
        return AvailablePart(
            partId = this.id,
            partName = this.materialType,
            animalType = animal,
            operationDate = this.createdAt?.take(10) ?: "",
            lotId = this.lotId ?: this.id,
            lotNumber = this.lotNumber ?: "LOT-${this.id}",
            availableKg = this.quantity
        )
    }
}

data class CreateRawMaterialRequest(
    @SerializedName("material_type") val materialType: String,
    val quantity: Double,
    val unit: String,
    @SerializedName("unit_price") val unitPrice: Double,
    val currency: String = "EUR",
    @SerializedName("supplier_name") val supplierName: String? = null,
    @SerializedName("expiry_date") val expiryDate: String? = null
)

data class CreateRawMaterialResponse(
    val message: String,
    val rmId: Int?,
    val lotNumber: String?,
    val totalCost: Double?
)

data class RawMaterialPreset(
    val label: String,
    val unit: String,
    val category: String,
    val defaultPrice: Double
)

object RawMaterialPresets {
    val ITEMS = listOf(
        RawMaterialPreset("Tuz", "kg", "Kuru/Toz", 0.50),
        RawMaterialPreset("Baharat", "kg", "Kuru/Toz", 12.00),
        RawMaterialPreset("Çemen", "kg", "Kuru/Toz", 6.50),
        RawMaterialPreset("Sarımsak", "kg", "Kuru/Toz", 4.00),
        RawMaterialPreset("Jelatin", "kg", "Kuru/Toz", 15.00),
        RawMaterialPreset("Yağ", "kg", "Sıvı", 3.50),
        RawMaterialPreset("İlikli Kemik Suyu", "kg", "Sıvı", 5.00),
        RawMaterialPreset("Kırmızı Pancar Suyu", "kg", "Sıvı", 4.50),
        RawMaterialPreset("Salça", "kg", "Sebze/Meyve", 2.80),
        RawMaterialPreset("Yoğurt", "kg", "Süt/Yumurta", 1.80),
        RawMaterialPreset("Yumurta Akı", "kg", "Süt/Yumurta", 3.00),
        RawMaterialPreset("Bağırsak", "kg", "Bağırsak", 8.50),
        RawMaterialPreset("İnek Bağırsağı", "kg", "Bağırsak", 8.00),
        RawMaterialPreset("Koyun Bağırsağı", "kg", "Bağırsak", 9.50)
    )
}
