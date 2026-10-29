package com.avatar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "butchery_operations")
data class ButcheryOperationEntity(
    @PrimaryKey val id: Int,
    val animalType: String,
    val animalEarTag: String?,
    val operationDate: String,
    val inputWeightKg: Double,
    val totalOutputKg: Double,
    val wasteKg: Double,
    val totalCost: Double,
    val unitCostInput: Double?,
    val distributionMethod: String,
    val currency: String,
    val notes: String?
)

@Entity(tableName = "raw_materials")
data class RawMaterialEntity(
    @PrimaryKey val id: Int,
    val materialType: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val totalCost: Double,
    val currency: String,
    val lotNumber: String?,
    val lotStatus: String,
    val createdAt: String?
)
