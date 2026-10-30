package com.avatar.app.data.model

import com.google.gson.annotations.SerializedName

data class ProducerListing(
    val id: Int,
    @SerializedName("producer_id") val producerId: Int,
    @SerializedName("producer_name") val producerName: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerializedName("product_name") val productName: String,
    val description: String? = null,
    @SerializedName("service_price") val servicePrice: Double,
    val currency: String? = "EUR",
    @SerializedName("service_unit") val serviceUnit: String = "kg",
    @SerializedName("min_order_qty") val minOrderQty: Double? = 0.0,
    @SerializedName("required_materials") val requiredMaterials: Any? = null,
    @SerializedName("is_active") val isActive: Any? = true,
    @SerializedName("created_at") val createdAt: String? = null
) {
    val requiredMaterialsParsed: Map<String, IngredientSpec>
        get() {
            if (requiredMaterials == null) return emptyMap()
            return try {
                val gson = com.google.gson.Gson()
                if (requiredMaterials is String) {
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, IngredientSpec>>() {}.type
                    gson.fromJson<Map<String, IngredientSpec>>(requiredMaterials, type) ?: emptyMap()
                } else {
                    val json = gson.toJson(requiredMaterials)
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, IngredientSpec>>() {}.type
                    gson.fromJson<Map<String, IngredientSpec>>(json, type) ?: emptyMap()
                }
            } catch (_: Exception) {
                emptyMap()
            }
        }

    val isCurrentlyActive: Boolean
        get() = when (val v = isActive) {
            is Boolean -> v
            is Number -> v.toInt() == 1
            is String -> v == "1" || v.equals("true", ignoreCase = true)
            else -> true
        }
}

data class IngredientSpec(
    val amount: Double,
    val unit: String
)

data class CreateProducerListingRequest(
    @SerializedName("product_name") val productName: String,
    val description: String?,
    @SerializedName("service_price") val servicePrice: Double,
    val currency: String = "TL",
    @SerializedName("service_unit") val serviceUnit: String = "paket",
    @SerializedName("min_order_qty") val minOrderQty: Double?,
    @SerializedName("required_materials") val requiredMaterials: Map<String, IngredientSpec>
)

data class FarmerListing(
    val id: Int,
    @SerializedName("farmer_id") val farmerId: Int,
    @SerializedName("farmer_name") val farmerName: String? = null,
    val phone: String? = null,
    @SerializedName("lot_id") val lotId: Int,
    @SerializedName("lot_number") val lotNumber: String,
    @SerializedName("lot_status") val lotStatus: String? = "active",
    @SerializedName("product_name") val productName: String,
    val quantity: Double,
    val unit: String,
    @SerializedName("unit_price") val unitPrice: Double,
    val currency: String? = "EUR",
    val description: String? = null,
    @SerializedName("is_active") val isActive: Any? = true,
    @SerializedName("created_at") val createdAt: String? = null
) {
    val isCurrentlyActive: Boolean
        get() = when (val v = isActive) {
            is Boolean -> v
            is Number -> v.toInt() == 1
            is String -> v == "1" || v.equals("true", ignoreCase = true)
            else -> true
        }
}

data class CreateFarmerListingRequest(
    @SerializedName("lot_id") val lotId: Int,
    @SerializedName("product_name") val productName: String,
    val quantity: Double,
    val unit: String = "paket",
    @SerializedName("unit_price") val unitPrice: Double,
    val currency: String = "EUR",
    val description: String? = null
)

data class UpdateFarmerListingRequest(
    @SerializedName("unit_price") val unitPrice: Double,
    val currency: String? = "TL",
    val quantity: Double,
    val unit: String = "paket",
    val description: String? = null
)

data class UpdateProducerListingRequest(
    @SerializedName("service_price") val servicePrice: Double,
    val currency: String? = "TL",
    @SerializedName("service_unit") val serviceUnit: String = "paket",
    @SerializedName("min_order_qty") val minOrderQty: Double? = null,
    val description: String? = null
)

// Standart Ürün ve Reçete Tanımları (Web ile birebir)
data class ProductTemplate(
    val key: String,
    val emoji: String,
    val label: String,
    val description: String,
    val ingredients: List<IngredientTemplate>
)

data class IngredientTemplate(
    val name: String,
    val unit: String,
    val defaultAmount: Double,
    val source: String // "part" (et parçası) veya "stock" (depodaki baharat/tuz)
)

object ProductTemplates {
    val ITEMS = listOf(
        ProductTemplate(
            key = "sucuk",
            emoji = "🌭",
            label = "Sucuk",
            description = "Geleneksel fermente Türk sucuğu",
            ingredients = listOf(
                IngredientTemplate("Et", "kg", 3.20, "part"),
                IngredientTemplate("Yağ", "kg", 0.50, "stock"),
                IngredientTemplate("Sarımsak", "kg", 0.15, "stock"),
                IngredientTemplate("Bağırsak", "kg", 0.10, "stock"),
                IngredientTemplate("Tuz", "kg", 0.10, "stock"),
                IngredientTemplate("Baharat", "kg", 0.25, "stock")
            )
        ),
        ProductTemplate(
            key = "salam",
            emoji = "🥩",
            label = "Salam",
            description = "Pişirilmiş ve emülsifiye et ürünü",
            ingredients = listOf(
                IngredientTemplate("Et", "kg", 3.00, "part"),
                IngredientTemplate("Sarımsak", "kg", 0.10, "stock"),
                IngredientTemplate("Tuz", "kg", 0.10, "stock"),
                IngredientTemplate("Baharat", "kg", 0.20, "stock"),
                IngredientTemplate("Jelatin", "kg", 0.10, "stock"),
                IngredientTemplate("İlikli Kemik Suyu", "lt", 0.40, "stock"),
                IngredientTemplate("Yumurta Akı", "lt", 0.20, "stock"),
                IngredientTemplate("Kırmızı Pancar Suyu", "lt", 0.10, "stock")
            )
        ),
        ProductTemplate(
            key = "sosis",
            emoji = "🍖",
            label = "Sosis",
            description = "Taze ve lezzetli kahvaltılık sosis",
            ingredients = listOf(
                IngredientTemplate("Et", "kg", 3.00, "part"),
                IngredientTemplate("Yoğurt", "kg", 0.30, "stock"),
                IngredientTemplate("Salça", "kg", 0.20, "stock"),
                IngredientTemplate("Tuz", "kg", 0.10, "stock"),
                IngredientTemplate("Baharat", "kg", 0.20, "stock"),
                IngredientTemplate("Bağırsak", "kg", 0.10, "stock")
            )
        ),
        ProductTemplate(
            key = "pastirma",
            emoji = "🥓",
            label = "Pastırma",
            description = "Geleneksel çemenli kuru kürlenmiş pastırma",
            ingredients = listOf(
                IngredientTemplate("Et", "kg", 3.50, "part"),
                IngredientTemplate("Tuz", "kg", 0.30, "stock"),
                IngredientTemplate("Çemen", "kg", 0.50, "stock")
            )
        )
    )
}
