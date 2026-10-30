package com.avatar.app.ui.lots

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.AnimalLotGroup
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
                    (it.displayName.lowercase().contains(q)) ||
                    (it.animal_ear_tag?.lowercase()?.contains(q) == true) ||
                    (it.supplier_name?.lowercase()?.contains(q) == true)
                }
            }
            return list
        }

    val animalGroups: List<AnimalLotGroup>
        get() {
            val groupsMap = linkedMapOf<String, MutableList<LotItem>>()
            val animalInfoMap = mutableMapOf<String, Pair<String?, String?>>()

            filteredLots.forEach { lot ->
                val tag = lot.animal_ear_tag ?: if (lot.lot_type.equals("parca", ignoreCase = true) || isMeatLot(lot)) {
                    if (lot.butchery_operation_id != null) "Kesim #${lot.butchery_operation_id}" else "Parçalama Etleri"
                } else null

                if (tag != null) {
                    groupsMap.getOrPut(tag) { mutableListOf() }.add(lot)
                    if (!animalInfoMap.containsKey(tag)) {
                        val aType = lot.animal_type ?: if (tag.contains("KUZU", ignoreCase = true)) "Koyun / Kuzu" else if (tag.contains("DANA", ignoreCase = true)) "Dana / Sığır" else "Büyükbaş / Küçükbaş"
                        val bDate = lot.butchery_date ?: lot.production_date
                        animalInfoMap[tag] = Pair(aType, bDate)
                    }
                }
            }

            return groupsMap.map { (tag, parts) ->
                val info = animalInfoMap[tag]
                AnimalLotGroup(
                    earTag = tag,
                    animalType = info?.first,
                    butcheryDate = info?.second,
                    totalKg = parts.sumOf { it.quantity },
                    parts = parts
                )
            }
        }

    val otherLots: List<LotItem>
        get() {
            return filteredLots.filter { lot ->
                lot.animal_ear_tag == null && !lot.lot_type.equals("parca", ignoreCase = true) && !isMeatLot(lot)
            }
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
            return t == "parca" || t == "et" || num.startsWith("ET-") || num.startsWith("KZM-") || num.startsWith("DN-") || num.startsWith("PRC-") || lot.animal_ear_tag != null
        }

        fun isMamulLot(lot: LotItem): Boolean {
            val t = lot.lot_type.lowercase()
            val num = lot.lot_number.uppercase()
            val name = (lot.product_name ?: "").lowercase()
            return t == "mamul" || num.startsWith("SCK-") || num.startsWith("SLM-") || num.startsWith("PST-") ||
                   name.contains("sucuk") || name.contains("salam") || name.contains("sosis") || name.contains("pastırma") || name.contains("pastirma")
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
