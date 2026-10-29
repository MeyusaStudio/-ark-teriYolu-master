package com.avatar.app.ui.market

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.CreatePurchaseRequest
import com.avatar.app.data.model.FarmerListing
import com.avatar.app.data.model.MarketPurchaseItem
import com.avatar.app.data.model.SubmitFeedbackRequest
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketUiState(
    val isLoading: Boolean = false,
    val listings: List<FarmerListing> = emptyList(),
    val purchases: List<MarketPurchaseItem> = emptyList(),
    val searchQuery: String = "",
    val isPurchasing: Boolean = false,
    val isSubmittingFeedback: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredListings: List<FarmerListing>
        get() {
            if (searchQuery.isBlank()) return listings
            val q = searchQuery.lowercase()
            return listings.filter {
                it.productName.lowercase().contains(q) ||
                (it.farmerName?.lowercase()?.contains(q) == true) ||
                it.lotNumber.lowercase().contains(q)
            }
        }

    val totalSpent: Double
        get() = purchases.sumOf { it.totalPrice }

    val totalBoughtQty: Double
        get() = purchases.sumOf { it.quantity }
}

class MarketViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        loadListings()
        loadPurchases()
    }

    fun loadListings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = apiClient.listingService.getFarmerListings()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        listings = resp.body() ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Market ilanları yüklenemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadPurchases() {
        viewModelScope.launch {
            try {
                val resp = apiClient.purchaseService.getPurchases()
                _uiState.update {
                    it.copy(purchases = resp.body() ?: emptyList())
                }
            } catch (_: Exception) {}
        }
    }

    fun setSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun buyProduct(
        listing: FarmerListing,
        quantity: Double,
        onSuccess: () -> Unit
    ) {
        if (quantity <= 0) {
            _uiState.update { it.copy(errorMessage = "Satın alma miktarı 0'dan büyük olmalıdır.") }
            return
        }
        if (quantity > listing.quantity) {
            _uiState.update { it.copy(errorMessage = "İlanda kalan mevcut miktardan (${listing.quantity} ${listing.unit}) fazla alım yapılamaz.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, errorMessage = null) }
            try {
                val req = CreatePurchaseRequest(
                    farmerId = listing.farmerId,
                    listingId = listing.id,
                    lotId = listing.lotId,
                    quantity = quantity,
                    unit = listing.unit,
                    unitPrice = listing.unitPrice
                )
                val resp = apiClient.purchaseService.createPurchase(req)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            successMessage = "Satın alma başarılı! Mamul ürün faturası oluşturuldu ve stoğunuza eklendi."
                        )
                    }
                    loadAll()
                    onSuccess()
                } else {
                    val err = apiClient.parseError(resp.errorBody()?.string())
                    _uiState.update { it.copy(isPurchasing = false, errorMessage = err) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = "Satın alma işlemi tamamlanamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun submitFeedback(
        lotNumber: String,
        issueDescription: String,
        farmerId: Int?,
        lotId: Int?,
        onSuccess: () -> Unit
    ) {
        if (issueDescription.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Lütfen sorun açıklamasını yazınız.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingFeedback = true, errorMessage = null) }
            try {
                val req = SubmitFeedbackRequest(
                    lotNumber = lotNumber,
                    issueDescription = issueDescription.trim(),
                    farmerId = farmerId,
                    lotId = lotId
                )
                val resp = apiClient.purchaseService.submitFeedback(req)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSubmittingFeedback = false,
                            successMessage = "Kalite sorunu bildirimi hayvancıya iletildi!"
                        )
                    }
                    loadPurchases()
                    onSuccess()
                } else {
                    val err = apiClient.parseError(resp.errorBody()?.string())
                    _uiState.update { it.copy(isSubmittingFeedback = false, errorMessage = err) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmittingFeedback = false,
                        errorMessage = "Bildirim iletilemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
