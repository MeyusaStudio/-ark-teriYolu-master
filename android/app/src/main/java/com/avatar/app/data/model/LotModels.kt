package com.avatar.app.data.model

data class LotItem(
    val id: Int,
    val lot_number: String,
    val lot_type: String, // "parca", "mamul", "hammadde", "hayvan"
    val owner_id: Int,
    val quantity: Double,
    val unit: String,
    val unit_cost: Double? = null,
    val total_cost: Double? = null,
    val currency: String = "EUR",
    val supplier_name: String? = null,
    val production_date: String? = null,
    val status: String, // "active", "recalled", "blocked", "consumed"
    val product_name: String? = null,
    val owner_name: String? = null
) {
    val isBlocked: Boolean
        get() = status.equals("recalled", ignoreCase = true) || status.equals("blocked", ignoreCase = true)

    val displayName: String
        get() = product_name ?: when (lot_type.lowercase()) {
            "parca" -> "Parçalama Eti"
            "mamul" -> "Mamul Ürün"
            "hammadde" -> "Hammadde"
            else -> "Lot: $lot_number"
        }
}

data class SellLotRequest(
    val customer_name: String,
    val quantity: Double,
    val unit_price: Double,
    val currency: String = "EUR",
    val notes: String? = null
)

data class SellLotResponse(
    val message: String,
    val invoice_number: String,
    val lot_id: Int,
    val lot_number: String,
    val product_name: String,
    val sold_quantity: Double,
    val unit: String,
    val unit_price: Double,
    val total_price: Double,
    val currency: String,
    val remaining_quantity: Double,
    val customer_name: String
)
