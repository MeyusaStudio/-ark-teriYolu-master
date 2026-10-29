package com.avatar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val phone: String? = null,
    val address: String? = null,
    val token: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
