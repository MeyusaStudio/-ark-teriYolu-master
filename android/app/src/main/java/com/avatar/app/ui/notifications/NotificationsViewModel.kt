package com.avatar.app.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.NotificationItem
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = apiClient.notificationService.getNotifications()
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = body.notifications,
                            unreadCount = body.unreadCount
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = apiClient.parseError(resp.errorBody()?.string())
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Bildirimler alınamadı: ${e.localizedMessage ?: "Bağlantı hatası"}"
                    )
                }
            }
        }
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            try {
                val resp = apiClient.notificationService.markAsRead(notificationId)
                if (resp.isSuccessful) {
                    _uiState.update { state ->
                        val updated = state.notifications.map {
                            if (it.id == notificationId) it.copy(is_read = 1) else it
                        }
                        val newUnread = updated.count { it.is_read == 0 }
                        state.copy(notifications = updated, unreadCount = newUnread)
                    }
                }
            } catch (_: Exception) {
                // Silently ignore or retry
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val resp = apiClient.notificationService.markAllAsRead()
                if (resp.isSuccessful) {
                    _uiState.update { state ->
                        val updated = state.notifications.map { it.copy(is_read = 1) }
                        state.copy(notifications = updated, unreadCount = 0)
                    }
                }
            } catch (_: Exception) {
                // Silently ignore
            }
        }
    }
}
