package com.avatar.app.data.model

data class AdminUserItem(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val phone: String? = null,
    val address: String? = null,
    val is_active: Int = 1,
    val is_banned: Int = 0,
    val ban_reason: String? = null,
    val created_at: String? = null,
    val lot_count: Int = 0,
    val order_count: Int = 0,
    val purchase_count: Int = 0,
    val unread_notifications_count: Int = 0
) {
    val isBanned: Boolean get() = is_banned == 1

    val roleDisplayTitle: String
        get() = when (role.lowercase()) {
            "hayvanci" -> "İşletme (Hayvancı)"
            "sucukcu" -> "Üretici (Sucukçu)"
            "market" -> "Market / Satıcı"
            "admin" -> "Sistem Yöneticisi"
            else -> role
        }

    val roleEmoji: String
        get() = when (role.lowercase()) {
            "hayvanci" -> "🐑"
            "sucukcu" -> "🥩"
            "market" -> "🛒"
            "admin" -> "🛡️"
            else -> "👤"
        }
}

data class AdminStats(
    val total: Int = 0,
    val hayvanci: Int = 0,
    val sucukcu: Int = 0,
    val market: Int = 0,
    val admin: Int = 0,
    val banned: Int = 0
)

data class AdminUsersResponse(
    val users: List<AdminUserItem>,
    val stats: AdminStats
)

data class BanUserRequest(
    val is_banned: Boolean,
    val ban_reason: String? = null
)

data class BanUserResponse(
    val message: String,
    val userId: Int,
    val isBanned: Boolean,
    val banReason: String? = null
)

data class SendAdminNotificationRequest(
    val user_id: Int? = null,
    val user_ids: List<Int>? = null,
    val role: String? = null,
    val title: String,
    val message: String
)

data class SendAdminNotificationResponse(
    val message: String,
    val deliveredCount: Int
)

data class SystemStatsResponse(
    val totalUsers: Int = 0,
    val activeLots: Int = 0,
    val completedOrders: Int = 0,
    val activeRecalls: Int = 0,
    val totalInvoices: Int = 0,
    val totalRevenue: Double = 0.0
)
