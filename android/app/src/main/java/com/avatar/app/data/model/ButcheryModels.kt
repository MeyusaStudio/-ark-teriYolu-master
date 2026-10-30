package com.avatar.app.data.model

import com.google.gson.annotations.SerializedName

data class ButcheryRecord(
    val id: Int,
    @SerializedName("animal_type") val animalType: String? = "koyun",
    @SerializedName("animal_ear_tag") val animalEarTag: String? = null,
    @SerializedName("operation_date") val operationDate: String,
    @SerializedName("input_weight_kg") val inputWeightKg: Double = 0.0,
    @SerializedName("total_output_kg") val totalOutputKg: Double = 0.0,
    @SerializedName("waste_kg") val wasteKg: Double = 0.0,
    @SerializedName("total_cost") val totalCost: Double = 0.0,
    @SerializedName("unit_cost_input") val unitCostInput: Double? = null,
    @SerializedName("distribution_method") val distributionMethod: String? = "agirlik",
    val currency: String? = "EUR",
    val notes: String? = null,
    val parts: List<ButcheryPartDetail>? = null,
    val isOfflinePending: Boolean = false
)

data class ButcheryDetailResponse(
    val operation: ButcheryRecord,
    val parts: List<ButcheryPartDetail>? = null
)

data class ButcheryPartDetail(
    @SerializedName("part_name") val partName: String,
    @SerializedName("actual_weight_kg") val actualWeightKg: Double,
    @SerializedName("remaining_kg") val remainingKg: Double? = null,
    @SerializedName("allocated_cost") val allocatedCost: Double,
    @SerializedName("unit_cost") val unitCost: Double,
    @SerializedName("lot_number") val lotNumber: String? = null,
    @SerializedName("is_byproduct") val isByproduct: Any? = false
)

data class CreatePartInput(
    @SerializedName("part_name") val partName: String,
    @SerializedName("actual_weight_kg") val actualWeightKg: Double,
    @SerializedName("is_byproduct") val isByproduct: Boolean = false
)

data class CreateButcheryRequest(
    @SerializedName("animal_type") val animalType: String,
    @SerializedName("animal_ear_tag") val animalEarTag: String?,
    @SerializedName("operation_date") val operationDate: String,
    @SerializedName("input_weight_kg") val inputWeightKg: Double,
    @SerializedName("total_cost") val totalCost: Double,
    @SerializedName("unit_cost_input") val unitCostInput: Double,
    @SerializedName("distribution_method") val distributionMethod: String,
    val currency: String = "EUR",
    val notes: String? = null,
    val parts: List<CreatePartInput>
)

data class CreateButcheryResponse(
    val message: String,
    val operationId: Int?,
    val lots: List<Any>? = null
)

// UI Helper Types
data class AnimalTypeDefinition(
    val key: String,
    val emoji: String,
    val label: String,
    val defaultWeight: Double,
    val defaultCost: Double,
    val defaultTag: String,
    val parts: List<String>
)

object ButcheryConstants {
    val ANIMAL_TYPES = listOf(
        AnimalTypeDefinition(
            key = "koyun",
            emoji = "🐑",
            label = "Koyun / Kuzu",
            defaultWeight = 25.0,
            defaultCost = 287.50,
            defaultTag = "TR-06-KUZU-001",
            parts = listOf("Pirzola", "But", "Sırt / Kontrfile", "Kol / Omuz", "Kaburga", "Gerdan", "Kıyma / Kuşbaşı", "İncik", "Bağırsak")
        ),
        AnimalTypeDefinition(
            key = "inek",
            emoji = "🐄",
            label = "İnek / Dana",
            defaultWeight = 350.0,
            defaultCost = 4200.00,
            defaultTag = "TR-34-DANA-101",
            parts = listOf("Antrikot", "Bonfile", "Kontrfile", "Sokum", "But", "Kol", "Kaburga", "Gerdan", "Bağırsak")
        ),
        AnimalTypeDefinition(
            key = "tavuk",
            emoji = "🐔",
            label = "Tavuk",
            defaultWeight = 2.5,
            defaultCost = 15.00,
            defaultTag = "TR-16-TVK-01",
            parts = listOf("Bütün Tavuk")
        )
    )

    val PART_VALUE_MULTIPLIERS = mapOf(
        "pirzola" to 2.2,
        "sırt / kontrfile" to 1.8,
        "sırt" to 1.8,
        "but" to 1.3,
        "kol / omuz" to 1.1,
        "kol" to 1.1,
        "kaburga" to 0.9,
        "incik" to 0.9,
        "gerdan" to 0.8,
        "kıyma / kuşbaşı" to 1.0,
        "kıyma" to 1.0,
        "kuşbaşı" to 1.1,
        "bağırsak" to 0.3,
        "antrikot" to 2.5,
        "bonfile" to 2.8,
        "kontrfile" to 2.2,
        "sokum" to 1.5,
        "tavuk" to 1.0
    )

    fun getMultiplier(partName: String): Double {
        val norm = partName.lowercase().trim()
        for ((key, mult) in PART_VALUE_MULTIPLIERS) {
            if (norm.contains(key) || key.contains(norm)) {
                return mult
            }
        }
        return 1.0
    }
}
