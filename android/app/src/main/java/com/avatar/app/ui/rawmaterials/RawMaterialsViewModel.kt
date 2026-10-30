package com.avatar.app.ui.rawmaterials

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.local.AppDatabase
import com.avatar.app.data.local.entity.OfflineRawMaterialEntity
import com.avatar.app.data.local.entity.RawMaterialEntity
import com.avatar.app.data.model.CreateRawMaterialRequest
import com.avatar.app.data.model.RawMaterial
import com.avatar.app.data.model.RawMaterialPreset
import com.avatar.app.data.model.RawMaterialPresets
import com.avatar.app.data.remote.ApiClient
import com.avatar.app.data.sync.SyncManager
import com.avatar.app.data.sync.SyncUiState
import com.avatar.app.utils.NetworkConnectivityObserver
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
    val syncState: SyncUiState = SyncUiState(),

    // Dialog state
    val isSubmitting: Boolean = false,
    val selectedPreset: RawMaterialPreset = RawMaterialPresets.ITEMS[0],
    val customMaterialType: String = RawMaterialPresets.ITEMS[0].label,
    val quantity: Double = 10.0,
    val unit: String = "kg",
    val unitPrice: Double = RawMaterialPresets.ITEMS[0].defaultPrice,
    val currency: String = "EUR",
    val supplierName: String = "Tedarikçi A.Ş.",
    val expiryDate: String? = null
) {
    val totalCost: Double
        get() = quantity * unitPrice

    val displayedMaterials: List<RawMaterial>
        get() {
            if (filteredCategory.isNullOrBlank()) return materials
            return materials.filter { m ->
                val cat = when {
                    m.materialType.contains("bağırsak", ignoreCase = true) || m.materialType.contains("bagirsak", ignoreCase = true) -> "Bağırsak"
                    m.isMeat() -> "Et"
                    else -> RawMaterialPresets.ITEMS.find {
                        it.label.contains(m.materialType, ignoreCase = true) || m.materialType.contains(it.label, ignoreCase = true)
                    }?.category ?: "Diğer"
                }
                cat.equals(filteredCategory, ignoreCase = true)
            }
        }
}

class RawMaterialsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)
    private val database = AppDatabase.getDatabase(application)
    private val rawMaterialDao = database.rawMaterialDao()
    private val offlineRawMaterialDao = database.offlineRawMaterialDao()
    private val syncManager = SyncManager.getInstance(application)
    private val connectivityObserver = NetworkConnectivityObserver(application)

    private val _uiState = MutableStateFlow(RawMaterialsUiState())
    val uiState: StateFlow<RawMaterialsUiState> = _uiState.asStateFlow()

    init {
        // Senkronizasyon durumunu takip et ve otomatik senkronizasyon tamamlandığında listeyi güncelle
        viewModelScope.launch {
            var lastRecordedSync: Long? = null
            syncManager.syncState.collect { sState ->
                _uiState.update { it.copy(syncState = sState) }
                if (sState.lastSyncTime != null && sState.lastSyncTime != lastRecordedSync && !sState.isSyncing) {
                    lastRecordedSync = sState.lastSyncTime
                    loadMaterials()
                }
            }
        }
        loadMaterials()
    }

    fun loadMaterials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Önce SQLite yerel veritabanındaki hammadde kayıtlarını anında göster
            try {
                val cachedEntities = rawMaterialDao.getAllMaterialsList()
                val offlinePending = offlineRawMaterialDao.getAllOfflineMaterialsList()

                val cachedRecords = cachedEntities.map { entity ->
                    RawMaterial(
                        id = entity.id,
                        lotId = entity.lotId,
                        materialType = entity.materialType,
                        quantity = entity.quantity,
                        unit = entity.unit,
                        unitPrice = entity.unitPrice,
                        totalCost = entity.totalCost,
                        currency = entity.currency,
                        lotNumber = entity.lotNumber,
                        lotStatus = entity.lotStatus,
                        expiryDate = entity.expiryDate,
                        createdAt = entity.createdAt,
                        isOfflinePending = false
                    )
                }

                val pendingRecords = offlinePending.map { off ->
                    RawMaterial(
                        id = -off.localId.toInt(),
                        materialType = off.materialType,
                        quantity = off.quantity,
                        unit = off.unit,
                        unitPrice = off.unitPrice,
                        totalCost = off.totalCost,
                        currency = off.currency,
                        lotNumber = off.lotNumber ?: "OFFLINE-RM-${off.localId}",
                        lotStatus = "pending",
                        expiryDate = off.expiryDate,
                        createdAt = null,
                        isOfflinePending = true
                    )
                }

                val combined = pendingRecords + cachedRecords
                if (combined.isNotEmpty()) {
                    _uiState.update { it.copy(materials = combined, isLoading = false) }
                }
            } catch (_: Exception) {}

            // 2. İnternet/Sunucu erişimi varsa MySQL'den en güncel listeyi al
            if (connectivityObserver.isOnlineNow()) {
                try {
                    val resp = apiClient.rawMaterialService.getMaterials()
                    if (resp.isSuccessful && resp.body() != null) {
                        val serverList = resp.body()!!

                        // SQLite önbelleğine kaydet
                        val entities = serverList.map { m ->
                            RawMaterialEntity(
                                id = m.id,
                                lotId = m.lotId,
                                materialType = m.materialType,
                                quantity = m.quantity,
                                unit = m.unit,
                                unitPrice = m.unitPrice,
                                totalCost = m.totalCost,
                                currency = m.currency ?: "EUR",
                                lotNumber = m.lotNumber,
                                lotStatus = m.lotStatus ?: "active",
                                expiryDate = m.expiryDate,
                                createdAt = m.createdAt
                            )
                        }
                        rawMaterialDao.insertMaterials(entities)

                        // Bekleyen çevrimdışı kayıtlarla birleştir
                        val offlinePending = offlineRawMaterialDao.getAllOfflineMaterialsList()
                        val pendingRecords = offlinePending.map { off ->
                            RawMaterial(
                                id = -off.localId.toInt(),
                                materialType = off.materialType,
                                quantity = off.quantity,
                                unit = off.unit,
                                unitPrice = off.unitPrice,
                                totalCost = off.totalCost,
                                currency = off.currency,
                                lotNumber = off.lotNumber ?: "OFFLINE-RM-${off.localId}",
                                lotStatus = "pending",
                                expiryDate = off.expiryDate,
                                createdAt = null,
                                isOfflinePending = true
                            )
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                materials = pendingRecords + serverList
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } catch (_: Exception) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
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
                unit = "kg",
                unitPrice = preset.defaultPrice
            )
        }
    }

    fun updateForm(
        materialType: String? = null,
        quantity: Double? = null,
        unitPrice: Double? = null,
        currency: String? = null,
        supplierName: String? = null,
        expiryDate: String? = null
    ) {
        _uiState.update {
            it.copy(
                customMaterialType = materialType ?: it.customMaterialType,
                quantity = quantity ?: it.quantity,
                unit = "kg",
                unitPrice = unitPrice ?: it.unitPrice,
                currency = currency ?: it.currency,
                supplierName = supplierName ?: it.supplierName,
                expiryDate = expiryDate ?: it.expiryDate
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

            val req = CreateRawMaterialRequest(
                materialType = state.customMaterialType,
                quantity = state.quantity,
                unit = "kg",
                unitPrice = state.unitPrice,
                currency = state.currency,
                supplierName = state.supplierName.ifBlank { null },
                expiryDate = state.expiryDate?.ifBlank { null }
            )

            // 1. Çevrimiçiyse sunucuya (MySQL) kaydetmeyi dene
            if (connectivityObserver.isOnlineNow()) {
                try {
                    val resp = apiClient.rawMaterialService.createMaterial(req)
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                successMessage = "Hammadde stoğa eklendi (MySQL)! Lot No: ${body?.lotNumber ?: "-"}"
                            )
                        }
                        loadMaterials()
                        onSuccess()
                        return@launch
                    } else {
                        val err = apiClient.parseError(resp.errorBody()?.string())
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = err
                            )
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    // Bağlantı koptuysa veya sunucuya ulaşılamıyorsa çevrimdışı SQLite kaydına geç
                }
            }

            // 2. Çevrimdışı Mod (SQLite Room):
            try {
                val tempLotNumber = "OFFLINE-RM-${System.currentTimeMillis() % 100000}"
                val offlineEntity = OfflineRawMaterialEntity(
                    materialType = req.materialType,
                    quantity = req.quantity,
                    unit = "kg",
                    unitPrice = req.unitPrice,
                    totalCost = state.totalCost,
                    currency = state.currency,
                    supplierName = req.supplierName,
                    expiryDate = req.expiryDate,
                    lotNumber = tempLotNumber,
                    syncStatus = "pending"
                )
                offlineRawMaterialDao.insertMaterial(offlineEntity)

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = "📴 Çevrimdışı hammadde SQLite veritabanına kaydedildi! İnternet geldiğinde otomatik olarak MySQL sunucusuna aktarılacak."
                    )
                }
                loadMaterials()
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "SQLite kaydı sırasında hata oluştu: ${e.localizedMessage}"
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
