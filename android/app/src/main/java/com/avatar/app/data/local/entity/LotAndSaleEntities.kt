package com.avatar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lots")
data class LotEntity(
    @PrimaryKey val id: Int,
    val lotNumber: String,
    val lotType: String,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val unitCost: Double?,
    val totalCost: Double?,
    val currency: String,
    val status: String, // "active", "recalled", "blocked", "consumed"
    val ownerId: Int,
    val ownerName: String?,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lotId: Int,
    val lotNumber: String,
    val productName: String,
    val customerName: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val totalPrice: Double,
    val currency: String,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis()
)
