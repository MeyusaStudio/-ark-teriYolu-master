package com.avatar.app.ui.rawmaterials

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.local.AppDatabase
import com.avatar.app.data.local.entity.RawMaterialEntity
import com.avatar.app.data.model.CreateRawMaterialRequest
import com.avatar.app.data.model.RawMaterial
import com.avatar.app.data.model.RawMaterialPreset
import com.avatar.app.data.model.RawMaterialPresets
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RawMaterialsUiState(
    val isLoading: Boolean = false,
    val materials: List<RawMaterial> = emptyList(),
    val filteredCategory: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Dialog state
    val isSubmitting: Boolean = false,
    val selectedPreset: RawMaterialPreset = RawMaterialPresets.ITEMS[0],
    val customMaterialType: String = RawMaterialPresets.ITEMS[0].label,
    val quantity: Double = 10.0,
    val unit: String = "kg",
    val unitPrice: Double = RawMaterialPresets.ITEMS[0].defaultPrice,
    val supplierName: String = "Tedarikçi A.Ş."
) {
    val totalCost: Double
        get() = quantity * unitPrice

    val displayedMaterials: List<RawMaterial>
        get() {
            if (filteredCategory.isNullOrBlank()) return materials
            return materials.filter { m ->
                val preset = RawMaterialPresets.ITEMS.find { it.label.equals(m.materialType, ignoreCase = true) }
                preset?.category == filteredCategory
            }
        }
}

class RawMaterialsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)
    private val database = AppDatabase.getDatabase(application)
    private val rawMaterialDao = database.rawMaterialDao()

    private val _uiState = MutableStateFlow(RawMaterialsUiState())
    val uiState: StateFlow<RawMaterialsUiState> = _uiState.asStateFlow()

    init {
        loadMaterials()
    }

    fun loadMaterials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = apiClient.rawMaterialService.getMaterials()
                if (resp.isSuccessful && resp.body() != null) {
                    val list = resp.body()!!
                    _uiState.update { it.copy(isLoading = false, materials = list) }

                    // Cache in Room SQLite
                    val entities = list.map { m ->
                        RawMaterialEntity(
                            id = m.id,
                            materialType = m.materialType,
                            quantity = m.quantity,
                            unit = m.unit,
                            unitPrice = m.unitPrice,
                            totalCost = m.totalCost,
                            currency = m.currency ?: "EUR",
                            lotNumber = m.lotNumber,
                            lotStatus = m.lotStatus ?: "active",
                            createdAt = m.createdAt
                        )
                    }
                    rawMaterialDao.insertMaterials(entities)
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
                        errorMessage = "Hammaddeler yüklenemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setFilterCategory(category: String?) {
        _uiState.update { it.copy(filteredCategory = category) }
    }

    fun selectPreset(preset: RawMaterialPreset) {
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                customMaterialType = preset.label,
                unit = preset.unit,
                unitPrice = preset.defaultPrice
            )
        }
    }

    fun updateForm(
        materialType: String? = null,
        quantity: Double? = null,
        unit: String? = null,
        unitPrice: Double? = null,
        supplierName: String? = null
    ) {
        _uiState.update {
            it.copy(
                customMaterialType = materialType ?: it.customMaterialType,
                quantity = quantity ?: it.quantity,
                unit = unit ?: it.unit,
                unitPrice = unitPrice ?: it.unitPrice,
                supplierName = supplierName ?: it.supplierName
            )
        }
    }

    fun createMaterial(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.customMaterialType.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Malzeme adı zorunludur.") }
            return
        }
        if (state.quantity <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Miktar 0'dan büyük olmalıdır.") }
            return
        }
        if (state.unitPrice <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Birim fiyat 0'dan büyük olmalıdır.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val req = CreateRawMaterialRequest(
                    materialType = state.customMaterialType,
                    quantity = state.quantity,
                    unit = state.unit,
                    unitPrice = state.unitPrice,
                    currency = "EUR",
                    supplierName = state.supplierName.ifBlank { null }
                )

                val resp = apiClient.rawMaterialService.createMaterial(req)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            successMessage = "Hammadde stoğa eklendi! Lot No: ${body?.lotNumber ?: "-"}"
                        )
                    }
                    loadMaterials()
                    onSuccess()
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
                        errorMessage = "Hata oluştu: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun deleteMaterial(id: Int) {
        viewModelScope.launch {
            try {
                val resp = apiClient.rawMaterialService.deleteMaterial(id)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(successMessage = "Hammadde silindi (tüketildi).") }
                    loadMaterials()
                } else {
                    _uiState.update { it.copy(errorMessage = apiClient.parseError(resp.errorBody()?.string())) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Silinemedi: ${e.localizedMessage}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
