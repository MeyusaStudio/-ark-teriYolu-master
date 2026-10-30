package com.avatar.app.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.*
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val users: List<AdminUserItem> = emptyList(),
    val stats: AdminStats = AdminStats(),
    val selectedRole: String = "all", // "all", "hayvanci", "sucukcu", "market"
    val selectedStatus: String = "all", // "all", "active", "banned"
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        val currentRole = _uiState.value.selectedRole
        val currentStatus = _uiState.value.selectedStatus
        val query = _uiState.value.searchQuery.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = apiClient.adminService.getUsers(
                    role = if (currentRole == "all") null else currentRole,
                    status = if (currentStatus == "all") null else currentStatus,
                    query = if (query.isEmpty()) null else query
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = body.users,
                            stats = body.stats
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
                        errorMessage = "Kullanıcılar yüklenemedi: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                    )
                }
            }
        }
    }

    fun setRoleFilter(role: String) {
        _uiState.update { it.copy(selectedRole = role) }
        loadUsers()
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadUsers()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadUsers()
    }

    fun banUser(userId: Int, isBanned: Boolean, reason: String? = null, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            try {
                val resp = apiClient.adminService.banUser(
                    id = userId,
                    request = BanUserRequest(is_banned = isBanned, ban_reason = reason)
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val result = resp.body()!!
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            successMessage = result.message
                        )
                    }
                    loadUsers()
                    onSuccess?.invoke()
                } else {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            errorMessage = apiClient.parseError(resp.errorBody()?.string())
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        errorMessage = "İşlem başarısız oldu: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                    )
                }
            }
        }
    }

    fun sendNotification(
        userId: Int? = null,
        role: String? = null,
        title: String,
        message: String,
        onSuccess: (() -> Unit)? = null
    ) {
        if (title.isBlank() || message.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Başlık ve mesaj içeriği boş bırakılamaz") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            try {
                val resp = apiClient.adminService.sendNotification(
                    SendAdminNotificationRequest(
                        user_id = userId,
                        role = role,
                        title = title.trim(),
                        message = message.trim()
                    )
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val result = resp.body()!!
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            successMessage = result.message
                        )
                    }
                    onSuccess?.invoke()
                } else {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            errorMessage = apiClient.parseError(resp.errorBody()?.string())
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        errorMessage = "Bildirim iletilemedi: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
