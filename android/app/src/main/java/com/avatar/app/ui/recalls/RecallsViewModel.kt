package com.avatar.app.ui.recalls

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.CreateRecallRequest
import com.avatar.app.data.model.LotItem
import com.avatar.app.data.model.RecallItem
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecallsUiState(
    val isLoading: Boolean = false,
    val recalls: List<RecallItem> = emptyList(),
    val availableLots: List<LotItem> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val activeRecallsCount: Int
        get() = recalls.count { it.status == "aktif" }

    val completedRecallsCount: Int
        get() = recalls.count { it.status == "tamamlandi" }

    val totalAffectedMarkets: Int
        get() = recalls.sumOf { it.affectedCount ?: 0 }
}

class RecallsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(RecallsUiState())
    val uiState: StateFlow<RecallsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val recallResp = apiClient.recallService.getRecalls()
                val lotsResp = apiClient.recallService.getRecallableLots()

                val recalls = recallResp.body() ?: emptyList()
                val lots = lotsResp.body() ?: emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recalls = recalls,
                        availableLots = lots
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Geri çağırma kayıtları yüklenemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun initiateRecall(lotId: Int, reason: String, onSuccess: () -> Unit) {
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Geri çağırma gerekçesini yazınız.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val req = CreateRecallRequest(lotId = lotId, reason = reason.trim())
                val resp = apiClient.recallService.initiateRecall(req)
                if (resp.isSuccessful) {
                    val count = resp.body()?.affectedMarketCount ?: 0
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            successMessage = "⛔ Geri çağırma başlatıldı! $count market etkilendi ve parti bloke edildi."
                        )
                    }
                    loadData()
                    onSuccess()
                } else {
                    val err = apiClient.parseError(resp.errorBody()?.string())
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = err) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "İşlem başarısız: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun completeRecall(recallId: Int) {
        viewModelScope.launch {
            try {
                val resp = apiClient.recallService.completeRecall(recallId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(successMessage = "Geri çağırma tamamlandı olarak işaretlendi.") }
                    loadData()
                } else {
                    val err = apiClient.parseError(resp.errorBody()?.string())
                    _uiState.update { it.copy(errorMessage = err) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Tamamlama işlemi başarısız: ${e.localizedMessage}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
