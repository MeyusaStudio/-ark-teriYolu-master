package com.avatar.app.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.ComplaintItem
import com.avatar.app.data.model.CreateRecallRequest
import com.avatar.app.data.model.MarketPurchaseItem
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FarmerSalesUiState(
    val isLoading: Boolean = false,
    val sales: List<MarketPurchaseItem> = emptyList(),
    val complaints: List<ComplaintItem> = emptyList(),
    val isInitiatingRecall: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val totalRevenue: Double
        get() = sales.sumOf { it.totalPrice }

    val totalSoldQuantity: Double
        get() = sales.sumOf { it.quantity }

    val distinctMarketCount: Int
        get() = sales.map { it.marketId }.distinct().size

    val activeComplaintCount: Int
        get() = complaints.size
}

class FarmerSalesViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(FarmerSalesUiState())
    val uiState: StateFlow<FarmerSalesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val salesResp = apiClient.purchaseService.getPurchases()
                val complaintsResp = try {
                    apiClient.purchaseService.getComplaints()
                } catch (_: Exception) { null }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sales = salesResp.body() ?: emptyList(),
                        complaints = complaintsResp?.body() ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Satış verileri yüklenemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun initiateRecall(lotId: Int, reason: String, onSuccess: () -> Unit) {
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Geri çağırma gerekçesi yazınız.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isInitiatingRecall = true, errorMessage = null) }
            try {
                val req = CreateRecallRequest(lotId = lotId, reason = reason.trim())
                val resp = apiClient.recallService.initiateRecall(req)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isInitiatingRecall = false,
                            successMessage = "⛔ Geri çağırma başlatıldı! İlgili tüm partiler ve ilanlar bloke edildi, marketlere bildirim gönderildi."
                        )
                    }
                    loadData()
                    onSuccess()
                } else {
                    val err = apiClient.parseError(resp.errorBody()?.string())
                    _uiState.update { it.copy(isInitiatingRecall = false, errorMessage = err) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInitiatingRecall = false,
                        errorMessage = "Geri çağırma başlatılamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
