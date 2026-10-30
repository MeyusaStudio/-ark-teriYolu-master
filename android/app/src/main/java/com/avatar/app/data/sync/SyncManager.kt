package com.avatar.app.data.sync

import android.content.Context
import com.avatar.app.data.local.AppDatabase
import com.avatar.app.data.local.entity.OfflineButcheryEntity
import com.avatar.app.data.local.entity.OfflineRawMaterialEntity
import com.avatar.app.data.model.CreateButcheryRequest
import com.avatar.app.data.model.CreatePartInput
import com.avatar.app.data.model.CreateRawMaterialRequest
import com.avatar.app.data.remote.ApiClient
import com.avatar.app.utils.NetworkConnectivityObserver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SyncUiState(
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val pendingButcheryCount: Int = 0,
    val pendingRawMaterialCount: Int = 0,
    val lastSyncTime: Long? = null,
    val lastSyncResult: String? = null,
    val isOnline: Boolean = true
)

class SyncManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val offlineButcheryDao = database.offlineButcheryDao()
    private val offlineRawMaterialDao = database.offlineRawMaterialDao()
    private val apiClient = ApiClient(appContext)
    private val connectivityObserver = NetworkConnectivityObserver(appContext)
    private val gson = Gson()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncState = MutableStateFlow(SyncUiState(isOnline = connectivityObserver.isOnlineNow()))
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    init {
        // 1. Bekleyen kayıt sayılarını ve ağ bağlantısını sürekli izle
        scope.launch {
            combine(
                offlineButcheryDao.getPendingCountFlow(),
                offlineRawMaterialDao.getPendingCountFlow(),
                connectivityObserver.isOnline
            ) { butcheryPending, rmPending, online ->
                Triple(butcheryPending, rmPending, online)
            }.collect { (bCount, rmCount, online) ->
                val totalPending = bCount + rmCount

                _syncState.update {
                    it.copy(
                        pendingCount = totalPending,
                        pendingButcheryCount = bCount,
                        pendingRawMaterialCount = rmCount,
                        isOnline = online
                    )
                }

                // Cihaz internete bağlandığında ve bekleyen işlem varsa OTOMATİK EŞİTLE
                if (online && totalPending > 0 && !_syncState.value.isSyncing) {
                    delay(1200) // Ağın oturması için kısa bekleme
                    if (connectivityObserver.isOnlineNow() && !_syncState.value.isSyncing) {
                        syncAll()
                    }
                }
            }
        }

        // 2. Periyodik kontrol: İnternet aktif ve bekleyen veri varsa arka planda otomatik eşitle
        scope.launch {
            while (isActive) {
                delay(30000)
                if (connectivityObserver.isOnlineNow() && _syncState.value.pendingCount > 0 && !_syncState.value.isSyncing) {
                    syncAll()
                }
            }
        }
    }

    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        // 1. Kesin Çevrimdışı Kontrolü: Cihaz çevrimdışıysa hiçbir ağ isteği yapma ve eşitlemeyi durdur!
        if (!connectivityObserver.isOnlineNow()) {
            return@withContext SyncResult(0, 0, "Cihaz çevrimdışı. İnternet bağlantısı sağlandığında otomatik senkronize edilecektir.")
        }

        if (_syncState.value.isSyncing) {
            return@withContext SyncResult(0, 0, "Senkronizasyon zaten devam ediyor.")
        }

        _syncState.update { it.copy(isSyncing = true, lastSyncResult = null) }

        var successCount = 0
        var failCount = 0

        try {
            // Sunucu soketine erişim testi
            val isReachable = connectivityObserver.isServerReachable(timeoutMs = 2000)
            if (!isReachable) {
                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncResult = "Sunucuya ulaşılamadı. Bağlantı kurulduğunda otomatik olarak tekrar denenecektir."
                    )
                }
                return@withContext SyncResult(0, 0, "Sunucuya ulaşılamadı.")
            }

            // 1. Parçalama Senkronizasyonu
            val pendingButcheries = offlineButcheryDao.getPendingOperations()
            for (op in pendingButcheries) {
                try {
                    val partsType = object : TypeToken<List<CreatePartInput>>() {}.type
                    val partsList: List<CreatePartInput> = gson.fromJson(op.partsJson, partsType) ?: emptyList()

                    val req = CreateButcheryRequest(
                        animalType = op.animalType,
                        animalEarTag = op.animalEarTag,
                        operationDate = op.operationDate,
                        inputWeightKg = op.inputWeightKg,
                        totalCost = op.totalCost,
                        unitCostInput = op.unitCostInput,
                        distributionMethod = op.distributionMethod,
                        currency = op.currency,
                        notes = op.notes,
                        parts = partsList
                    )

                    val resp = apiClient.butcheryService.createOperation(req)
                    if (resp.isSuccessful && resp.body() != null) {
                        val body = resp.body()!!
                        offlineButcheryDao.updateSyncStatus(
                            localId = op.localId,
                            status = "synced",
                            serverId = body.operationId,
                            errorMsg = null
                        )
                        successCount++
                    } else {
                        val err = apiClient.parseError(resp.errorBody()?.string())
                        offlineButcheryDao.updateSyncStatus(
                            localId = op.localId,
                            status = "pending",
                            serverId = null,
                            errorMsg = err
                        )
                        failCount++
                    }
                } catch (e: Exception) {
                    offlineButcheryDao.updateSyncStatus(
                        localId = op.localId,
                        status = "pending",
                        serverId = null,
                        errorMsg = e.localizedMessage
                    )
                    failCount++
                }
            }

            // 2. Hammadde Girişi Senkronizasyonu
            val pendingMaterials = offlineRawMaterialDao.getPendingMaterials()
            for (mat in pendingMaterials) {
                try {
                    val req = CreateRawMaterialRequest(
                        materialType = mat.materialType,
                        quantity = mat.quantity,
                        unit = mat.unit,
                        unitPrice = mat.unitPrice,
                        currency = mat.currency,
                        supplierName = mat.supplierName,
                        expiryDate = mat.expiryDate
                    )

                    val resp = apiClient.rawMaterialService.createMaterial(req)
                    if (resp.isSuccessful && resp.body() != null) {
                        val body = resp.body()!!
                        offlineRawMaterialDao.updateSyncStatus(
                            localId = mat.localId,
                            status = "synced",
                            serverId = body.rmId,
                            errorMsg = null
                        )
                        successCount++
                    } else {
                        val err = apiClient.parseError(resp.errorBody()?.string())
                        offlineRawMaterialDao.updateSyncStatus(
                            localId = mat.localId,
                            status = "pending",
                            serverId = null,
                            errorMsg = err
                        )
                        failCount++
                    }
                } catch (e: Exception) {
                    offlineRawMaterialDao.updateSyncStatus(
                        localId = mat.localId,
                        status = "pending",
                        serverId = null,
                        errorMsg = e.localizedMessage
                    )
                    failCount++
                }
            }

            // Başarıyla senkronize olanları SQLite geçici tablosundan temizle
            offlineButcheryDao.deleteSyncedOperations()
            offlineRawMaterialDao.deleteSyncedMaterials()

            val msg = when {
                successCount > 0 && failCount == 0 -> "✅ $successCount işlem başarıyla MySQL sunucusuna aktarıldı!"
                successCount > 0 && failCount > 0 -> "⚠️ $successCount işlem aktarıldı, $failCount işlem bağlantı bekleniyor."
                failCount > 0 -> "❌ Sunucuya aktarılamadı ($failCount işlem bağlantı gelince otomatik aktarılacak)."
                else -> "Tüm veriler güncel."
            }

            _syncState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis(),
                    lastSyncResult = msg
                )
            }

            SyncResult(successCount, failCount, msg)
        } catch (e: Exception) {
            val msg = "Senkronizasyon hatası: ${e.localizedMessage}"
            _syncState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncResult = msg
                )
            }
            SyncResult(successCount, failCount, msg)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SyncManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
}

data class SyncResult(
    val successCount: Int,
    val failCount: Int,
    val message: String
)
