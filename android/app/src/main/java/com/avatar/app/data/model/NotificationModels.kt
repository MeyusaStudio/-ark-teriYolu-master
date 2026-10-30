package com.avatar.app.data.model

data class NotificationItem(
    val id: Int,
    val user_id: Int,
    val type: String,
    val title: String,
    val message: String,
    val reference_type: String? = null,
    val reference_id: Int? = null,
    val is_read: Int = 0,
    val created_at: String? = null
) {
    val isRead: Boolean get() = is_read == 1

    val iconEmoji: String
        get() = when (type) {
            "admin_mesaj" -> "📢"
            "admin_uyari" -> "⛔"
            "recall" -> "🚨"
            "order" -> "📦"
            "invoice" -> "📄"
            else -> "🔔"
        }
}

data class NotificationsResponse(
    val notifications: List<NotificationItem>,
    val unreadCount: Int = 0
)

data class ReadNotificationResponse(
    val message: String
)
