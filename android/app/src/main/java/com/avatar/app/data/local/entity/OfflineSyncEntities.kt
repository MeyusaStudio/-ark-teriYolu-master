package com.avatar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_butchery_operations")
data class OfflineButcheryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Int? = null,
    val animalType: String,
    val animalEarTag: String?,
    val operationDate: String,
    val inputWeightKg: Double,
    val totalCost: Double,
    val unitCostInput: Double,
    val distributionMethod: String,
    val currency: String = "EUR",
    val notes: String? = null,
    val partsJson: String, // List<CreatePartInput> as JSON
    val totalOutputKg: Double,
    val wasteKg: Double,
    val syncStatus: String = "pending", // "pending", "synced", "failed"
    val syncErrorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "offline_raw_materials")
data class OfflineRawMaterialEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Int? = null,
    val materialType: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val totalCost: Double,
    val currency: String = "EUR",
    val supplierName: String? = null,
    val expiryDate: String? = null,
    val lotNumber: String? = null,
    val syncStatus: String = "pending", // "pending", "synced", "failed"
    val syncErrorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
