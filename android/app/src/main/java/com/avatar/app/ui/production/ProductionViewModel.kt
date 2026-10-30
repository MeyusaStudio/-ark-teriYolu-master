package com.avatar.app.ui.production

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.CompleteProductionRequest
import com.avatar.app.data.model.ProductionCompletionResult
import com.avatar.app.data.model.ProductionOrder
import com.avatar.app.data.model.ProductionRawMaterial
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProductionUiState(
    val isLoading: Boolean = false,
    val productions: List<ProductionOrder> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Complete / Onay Dialog
    val selectedForComplete: ProductionOrder? = null,
    val actualQty: Double = 4.0,
    val unitServiceCost: Double = 3.50,
    val productionDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val orderInputKg: Double = 4.30,
    val totalRawCost: Double = 0.0,
    val detailRawMaterials: List<ProductionRawMaterial> = emptyList(),
    val isSubmitting: Boolean = false,
    val completionResult: ProductionCompletionResult? = null,

    // Detail Dialog
    val selectedDetail: ProductionOrder? = null,
    val isLoadingDetail: Boolean = false
) {
    val totalServiceCost: Double
        get() = actualQty * unitServiceCost

    val netPackagedKg: Double
        get() = actualQty * 1.0 // 1.00 kg/paket

    val fireKg: Double
        get() = kotlin.math.max(0.0, orderInputKg - netPackagedKg)

    val fireRate: Double
        get() = if (orderInputKg > 0) (fireKg / orderInputKg) * 100.0 else 0.0

    val effectiveRawCost: Double
        get() = if (totalRawCost > 0) totalRawCost else (selectedForComplete?.totalRawCost ?: 0.0)

    val estimatedEkp: Double
        get() = if (actualQty > 0) (effectiveRawCost + totalServiceCost) / actualQty else 0.0
}

class ProductionViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(ProductionUiState())
    val uiState: StateFlow<ProductionUiState> = _uiState.asStateFlow()

    init {
        loadProductions()
    }

    fun loadProductions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = apiClient.productionService.getProductionOrders()
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(isLoading = false, productions = resp.body()!!) }
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
                        errorMessage = "Üretim emirleri alınamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun openCompleteDialog(order: ProductionOrder) {
        val defaultQty = if (order.expectedQty > 0) order.expectedQty else 4.0
        val defaultUnitCost = 3.50
        _uiState.update {
            it.copy(
                selectedForComplete = order,
                actualQty = defaultQty,
                unitServiceCost = defaultUnitCost,
                orderInputKg = defaultQty * 1.075, // geçici varsayılan, API detayı güncelleyecek
                totalRawCost = 0.0,
                detailRawMaterials = emptyList(),
                completionResult = null
            )
        }
        viewModelScope.launch {
            try {
                val resp = apiClient.productionService.getProductionById(order.id)
                if (resp.isSuccessful && resp.body() != null) {
                    val raw = resp.body()!!.rawMaterials ?: emptyList()
                    val totalKg = raw.sumOf { it.effectiveQuantity }
                    val rawCost = raw.sumOf { it.totalCost ?: 0.0 }
                    _uiState.update { current ->
                        current.copy(
                            detailRawMaterials = raw,
                            orderInputKg = if (totalKg > 0) totalKg else current.orderInputKg,
                            totalRawCost = if (rawCost > 0) rawCost else current.totalRawCost
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun closeCompleteDialog() {
        _uiState.update {
            it.copy(
                selectedForComplete = null,
                completionResult = null,
                detailRawMaterials = emptyList()
            )
        }
    }

    fun updateCompleteForm(actualQty: Double? = null, unitServiceCost: Double? = null) {
        _uiState.update {
            it.copy(
                actualQty = actualQty ?: it.actualQty,
                unitServiceCost = unitServiceCost ?: it.unitServiceCost
            )
        }
    }

    // Görev 1: 4.30 kg -> 4.00 kg Fire Senaryosu Doldurma
    fun applyScenario430to400() {
        _uiState.update {
            it.copy(
                actualQty = 4.0,
                unitServiceCost = 3.75,
                orderInputKg = 4.30,
                successMessage = "Görev 1 Standart Senaryosu: 4.30 kg Girdi ➔ 4 Paket Mamul (0.30 kg / %6.98 Fire)!"
            )
        }
    }

    fun submitCompleteProduction() {
        val state = _uiState.value
        val order = state.selectedForComplete ?: return

        if (state.actualQty <= 0) {
            _uiState.update { it.copy(errorMessage = "Üretilen paket miktarı 0'dan büyük olmalıdır.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val req = CompleteProductionRequest(
                    actualQty = state.actualQty,
                    serviceCost = state.totalServiceCost,
                    unitServiceCost = state.unitServiceCost,
                    productionDate = state.productionDate
                )
                val resp = apiClient.productionService.completeProduction(order.id, req)
                if (resp.isSuccessful && resp.body() != null) {
                    val res = resp.body()!!
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            completionResult = res,
                            successMessage = "Üretim başarıyla onaylandı! Mamul sucuk lotu (${res.lotNumber ?: ""}) oluşturuldu."
                        )
                    }
                    loadProductions()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = apiClient.parseError(resp.errorBody()?.string())
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Üretim tamamlanamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
