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
    val owner_name: String? = null,
    val animal_ear_tag: String? = null,
    val animal_type: String? = null,
    val butchery_date: String? = null,
    val butchery_operation_id: Int? = null
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

data class AnimalLotGroup(
    val earTag: String,
    val animalType: String? = null,
    val butcheryDate: String? = null,
    val totalKg: Double = 0.0,
    val parts: List<LotItem> = emptyList()
)

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
