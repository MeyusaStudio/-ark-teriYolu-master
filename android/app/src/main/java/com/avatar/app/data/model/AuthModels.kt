package com.avatar.app.data.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String, // "hayvanci", "sucukcu", "market"
    val phone: String? = null,
    val address: String? = null,
    val created_at: String? = null
) {
    val roleDisplayTitle: String
        get() = when (role.lowercase()) {
            "hayvanci" -> "İşletme / Çiftlik"
            "sucukcu" -> "Üretici / Fason"
            "market" -> "Süpermarket / Alıcı"
            else -> role
        }

    val roleEmoji: String
        get() = when (role.lowercase()) {
            "hayvanci" -> "🐑"
            "sucukcu" -> "🥩"
            "market" -> "🛒"
            else -> "👤"
        }
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: User
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    val phone: String? = null,
    val address: String? = null
)

data class RegisterResponse(
    val message: String,
    val token: String,
    val user: User
)

data class ApiError(
    val error: String? = null,
    val detail: String? = null
)
