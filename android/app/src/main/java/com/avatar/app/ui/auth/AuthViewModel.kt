package com.avatar.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.local.AppDatabase
import com.avatar.app.data.local.SessionManager
import com.avatar.app.data.local.entity.UserEntity
import com.avatar.app.data.model.LoginRequest
import com.avatar.app.data.model.RegisterRequest
import com.avatar.app.data.model.User
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val serverUrl: String = ""
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val database = AppDatabase.getDatabase(application)
    private var apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = sessionManager.isLoggedIn(),
            user = sessionManager.getUser(),
            serverUrl = sessionManager.getServerUrl()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Lütfen e-posta ve şifrenizi giriniz.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = apiClient.authService.login(LoginRequest(email.trim(), pass))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionManager.saveSession(body.token, body.user)

                    // SQLite Yerel Veritabanına da kaydet (Yarışma Şartnamesi)
                    database.userDao().clearUsers()
                    database.userDao().insertUser(
                        UserEntity(
                            id = body.user.id,
                            name = body.user.name,
                            email = body.user.email,
                            role = body.user.role,
                            phone = body.user.phone,
                            address = body.user.address,
                            token = body.token
                        )
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = body.user,
                        successMessage = "Giriş başarılı! Hoş geldiniz."
                    )
                } else {
                    val err = apiClient.parseError(response.errorBody()?.string())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Bağlantı hatası: ${e.localizedMessage ?: "Sunucuya erişilemedi. Lütfen bağlantı ayarlarını ve Wi-Fi'ı kontrol edin."}"
                )
            }
        }
    }

    fun register(name: String, email: String, pass: String, role: String, phone: String?, address: String?) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ad soyad, e-posta ve şifre zorunludur.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val req = RegisterRequest(
                    name = name.trim(),
                    email = email.trim(),
                    password = pass,
                    role = role,
                    phone = phone?.takeIf { it.isNotBlank() },
                    address = address?.takeIf { it.isNotBlank() }
                )
                val response = apiClient.authService.register(req)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionManager.saveSession(body.token, body.user)

                    // SQLite Yerel Veritabanına kaydet
                    database.userDao().clearUsers()
                    database.userDao().insertUser(
                        UserEntity(
                            id = body.user.id,
                            name = body.user.name,
                            email = body.user.email,
                            role = body.user.role,
                            phone = body.user.phone,
                            address = body.user.address,
                            token = body.token
                        )
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = body.user,
                        successMessage = "Kayıt başarılı! Hesabınız oluşturuldu."
                    )
                } else {
                    val err = apiClient.parseError(response.errorBody()?.string())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Bağlantı hatası: ${e.localizedMessage ?: "Sunucuya erişilemedi."}"
                )
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        viewModelScope.launch {
            database.userDao().clearUsers()
        }
        _uiState.value = _uiState.value.copy(
            isLoggedIn = false,
            user = null,
            successMessage = "Çıkış yapıldı."
        )
    }

    fun updateServerUrl(newUrl: String) {
        sessionManager.setServerUrl(newUrl)
        apiClient = ApiClient(getApplication())
        _uiState.value = _uiState.value.copy(
            serverUrl = sessionManager.getServerUrl(),
            successMessage = "API adresi güncellendi"
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
