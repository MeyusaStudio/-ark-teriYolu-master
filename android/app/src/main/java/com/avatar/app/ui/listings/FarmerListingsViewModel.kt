package com.avatar.app.ui.listings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.CreateFarmerListingRequest
import com.avatar.app.data.model.FarmerListing
import com.avatar.app.data.model.LotItem
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FarmerListingsUiState(
    val isLoading: Boolean = false,
    val listings: List<FarmerListing> = emptyList(),
    val availableLots: List<LotItem> = emptyList(),
    val searchQuery: String = "",
    val isCreatingListing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredListings: List<FarmerListing>
        get() {
            if (searchQuery.isBlank()) return listings
            val q = searchQuery.lowercase()
            return listings.filter {
                it.productName.lowercase().contains(q) ||
                it.lotNumber.lowercase().contains(q) ||
                (it.description?.lowercase()?.contains(q) == true)
            }
        }

    val activeCount: Int
        get() = listings.count { it.isActive == true || it.isActive == 1 || it.isActive == "1" }

    val passiveCount: Int
        get() = listings.size - activeCount

    val totalValue: Double
        get() = listings.filter { it.isActive == true || it.isActive == 1 || it.isActive == "1" }
            .sumOf { it.quantity * it.unitPrice }
}

class FarmerListingsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(FarmerListingsUiState())
    val uiState: StateFlow<FarmerListingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val listingsResp = apiClient.listingService.getMyFarmerListings()
                val lotsResp = apiClient.lotService.getLots()

                val listings = listingsResp.body() ?: emptyList()
                val allLots = lotsResp.body() ?: emptyList()

                // Satış sadece Sucuk, Salam, Sosis, Pastırma gibi fason mamullere yapılmalı.
                // Çiğ et parçaları ("parca") KESİNLİKLE ilana konulamaz.
                val available = allLots.filter {
                    !it.isBlocked &&
                    it.quantity > 0 &&
                    it.lot_type == "mamul"
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        listings = listings,
                        availableLots = available
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Veriler yüklenemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun createListing(
        lotId: Int,
        productName: String,
        quantity: Double,
        unit: String = "paket",
        unitPrice: Double,
        currency: String = "EUR",
        description: String?,
        onSuccess: () -> Unit
    ) {
        if (productName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ürün adı boş olamaz.") }
            return
        }
        if (quantity <= 0) {
            _uiState.update { it.copy(errorMessage = "Miktar 0'dan büyük olmalıdır.") }
            return
        }
        if (unitPrice <= 0) {
            _uiState.update { it.copy(errorMessage = "Birim satış fiyatı giriniz.") }
            return
        }

        val lot = _uiState.value.availableLots.find { it.id == lotId }
        if (lot == null || lot.lot_type != "mamul") {
            _uiState.update { it.copy(errorMessage = "Satış sadece Sucuk, Salam, Sosis, Pastırma gibi mamul ürünler için yapılabilir. Çiğ et parçaları ilana koyulamaz.") }
            return
        }

        if (quantity > lot.quantity) {
            _uiState.update { it.copy(errorMessage = "İlan miktarı depodaki mevcut lot miktarından (${lot.quantity} ${lot.unit}) fazla olamaz.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingListing = true, errorMessage = null) }
            try {
                val req = CreateFarmerListingRequest(
                    lotId = lotId,
                    productName = productName.trim(),
                    quantity = quantity,
                    unit = unit,
                    unitPrice = unitPrice,
                    currency = currency,
                    description = description?.trim().takeIf { !it.isNullOrBlank() }
                )
                val resp = apiClient.listingService.createFarmerListing(req)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isCreatingListing = false,
                            successMessage = "Mamül ilanı başarıyla oluşturuldu! Market ekranlarında yayınlandı."
                        )
                    }
                    loadData()
                    onSuccess()
                } else {
                    val err = apiClient.parseError(resp.errorBody()?.string())
                    _uiState.update { it.copy(isCreatingListing = false, errorMessage = err) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingListing = false,
                        errorMessage = "İlan oluşturulamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun toggleListing(id: Int) {
        viewModelScope.launch {
            try {
                val resp = apiClient.listingService.toggleFarmerListing(id)
                if (resp.isSuccessful) {
                    loadData()
                } else {
                    val err = apiClient.parseError(resp.errorBody()?.string())
                    _uiState.update { it.copy(errorMessage = err) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Durum güncellenemedi: ${e.localizedMessage}") }
            }
        }
    }

    fun updateListing(
        id: Int,
        unitPrice: Double,
        quantity: Double,
        description: String?,
        onSuccess: () -> Unit
    ) {
        if (unitPrice <= 0 || quantity <= 0) {
            _uiState.update { it.copy(errorMessage = "Geçerli fiyat ve miktar giriniz.") }
            return
        }
        viewModelScope.launch {
            try {
                val body = mapOf(
                    "unit_price" to unitPrice,
                    "quantity" to quantity,
                    "description" to description
                )
                val resp = apiClient.listingService.updateFarmerListing(id, body)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(successMessage = "İlan güncellendi") }
                    loadData()
                    onSuccess()
                } else {
                    val err = apiClient.parseError(resp.errorBody()?.string())
                    _uiState.update { it.copy(errorMessage = err) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "İlan güncellenemedi: ${e.localizedMessage}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
