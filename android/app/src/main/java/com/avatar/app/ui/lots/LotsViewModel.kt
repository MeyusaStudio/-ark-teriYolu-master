package com.avatar.app.ui.lots

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.LotItem
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LotsUiState(
    val isLoading: Boolean = false,
    val lots: List<LotItem> = emptyList(),
    val filterType: String = "all", // "all", "et", "mamul", "hammadde"
    val searchQuery: String = "",
    val errorMessage: String? = null
) {
    val filteredLots: List<LotItem>
        get() {
            var list = lots
            list = when (filterType) {
                "et" -> list.filter { isMeatLot(it) }
                "mamul" -> list.filter { isMamulLot(it) }
                "hammadde" -> list.filter { it.lot_type.equals("hammadde", ignoreCase = true) }
                else -> list
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.lot_number.lowercase().contains(q) ||
                    (it.product_name?.lowercase()?.contains(q) == true) ||
                    (it.displayName.lowercase().contains(q))
                }
            }
            return list
        }

    val etCount: Int
        get() = lots.count { isMeatLot(it) }

    val mamulCount: Int
        get() = lots.count { isMamulLot(it) }

    val hammaddeCount: Int
        get() = lots.count { it.lot_type.equals("hammadde", ignoreCase = true) }

    companion object {
        fun isMeatLot(lot: LotItem): Boolean {
            val t = lot.lot_type.lowercase()
            val num = lot.lot_number.uppercase()
            return t == "parca" || t == "et" || num.startsWith("ET-") || num.startsWith("KZM-") || num.startsWith("DN-")
        }

        fun isMamulLot(lot: LotItem): Boolean {
            val t = lot.lot_type.lowercase()
            val num = lot.lot_number.uppercase()
            return t == "mamul" || num.startsWith("SCK-") || num.startsWith("SLM-") || num.startsWith("PST-")
        }
    }
}

class LotsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(LotsUiState())
    val uiState: StateFlow<LotsUiState> = _uiState.asStateFlow()

    init {
        loadLots()
    }

    fun loadLots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = apiClient.lotService.getLots()
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(isLoading = false, lots = resp.body()!!) }
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
                        errorMessage = "Lotlar alınamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setFilterType(type: String) {
        _uiState.update { it.copy(filterType = type) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
