package com.avatar.app.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.local.AppDatabase
import com.avatar.app.data.local.entity.LotEntity
import com.avatar.app.data.local.entity.SaleEntity
import com.avatar.app.data.model.*
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SalesUiState(
    val isLoading: Boolean = false,
    val lots: List<LotItem> = emptyList(),
    val salesHistory: List<SaleEntity> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val filterType: String = "ALL", // "ALL", "ACTIVE", "BLOCKED"
    val isBlockedAlert: Boolean = false,

    // Trace & Recall States
    val selectedTraceLot: LotItem? = null,
    val traceForwardData: TraceForwardResponse? = null,
    val traceBackwardData: TraceBackwardResponse? = null,
    val isLoadingTrace: Boolean = false,
    val recallsList: List<RecallItem> = emptyList(),
    val isInitiatingRecall: Boolean = false
)

class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(SalesUiState())
    val uiState: StateFlow<SalesUiState> = _uiState.asStateFlow()

    init {
        loadLots()
        observeLocalData()
        loadRecalls()
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            database.saleDao().getAllSalesFlow().collect { sales ->
                _uiState.update { it.copy(salesHistory = sales) }
            }
        }
    }

    fun loadLots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = apiClient.lotService.getLots()
                if (response.isSuccessful && response.body() != null) {
                    val lotList = response.body()!!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lots = lotList
                        )
                    }

                    // Yerel SQLite'a kaydet (Senkronizasyon)
                    val entities = lotList.map {
                        LotEntity(
                            id = it.id,
                            lotNumber = it.lot_number,
                            lotType = it.lot_type,
                            productName = it.displayName,
                            quantity = it.quantity,
                            unit = it.unit,
                            unitCost = it.unit_cost,
                            totalCost = it.total_cost,
                            currency = it.currency,
                            status = it.status,
                            ownerId = it.owner_id,
                            ownerName = it.owner_name
                        )
                    }
                    database.lotDao().clearLots()
                    database.lotDao().insertLots(entities)
                } else {
                    val err = apiClient.parseError(response.errorBody()?.string())
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Bağlantı hatası: ${e.localizedMessage ?: "Sunucuya erişilemedi"}"
                    )
                }
            }
        }
    }

    fun sellLot(
        lot: LotItem,
        customerName: String,
        quantity: Double,
        unitPrice: Double,
        currency: String = "EUR"
    ) {
        // ── 3. GÖREV ŞARTNAME KURALI: GERİ ÇAĞRILAN / BLOKE LOTLARIN SATIŞI ENGELLENİR ──
        if (lot.isBlocked) {
            _uiState.update {
                it.copy(
                    errorMessage = "⛔ ŞARTNAME KURALI: ${lot.lot_number} numaralı lot geri çağrılmıştır veya bloke edilmiştir! Bu lotun satışı KESİNLİKLE ENGELLENMİŞTİR.",
                    isBlockedAlert = true
                )
            }
            return
        }

        if (customerName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Lütfen müşteri / market adını giriniz.") }
            return
        }

        if (quantity <= 0) {
            _uiState.update { it.copy(errorMessage = "Satış miktarı 0'dan büyük olmalıdır.") }
            return
        }

        if (quantity > lot.quantity) {
            _uiState.update {
                it.copy(
                    errorMessage = "Yetersiz stok! Mevcut miktar: ${lot.quantity} ${lot.unit}"
                )
            }
            return
        }

        if (unitPrice <= 0) {
            _uiState.update { it.copy(errorMessage = "Birim fiyat 0'dan büyük olmalıdır.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val req = SellLotRequest(
                    customer_name = customerName.trim(),
                    quantity = quantity,
                    unit_price = unitPrice,
                    currency = currency,
                    notes = "Kotlin Mobil Satış Uygulaması"
                )
                val response = apiClient.lotService.sellLot(lot.id, req)
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!

                    // Yerel SQLite Satış tablosuna kaydet
                    database.saleDao().insertSale(
                        SaleEntity(
                            lotId = res.lot_id,
                            lotNumber = res.lot_number,
                            productName = res.product_name,
                            customerName = res.customer_name,
                            quantity = res.sold_quantity,
                            unit = res.unit,
                            unitPrice = res.unit_price,
                            totalPrice = res.total_price,
                            currency = res.currency,
                            invoiceNumber = res.invoice_number
                        )
                    )

                    // Yerel lot miktarını da güncelle
                    val newStatus = if (res.remaining_quantity <= 0) "consumed" else "active"
                    database.lotDao().updateLotQuantity(res.lot_id, res.remaining_quantity, newStatus)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Satış yapıldı! Fatura No: ${res.invoice_number} (Stoktan düşüldü: ${res.sold_quantity} ${res.unit})"
                        )
                    }

                    // Sunucudan taze listeyi çek
                    loadLots()
                } else {
                    val err = apiClient.parseError(response.errorBody()?.string())
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Satış hatası: ${e.localizedMessage ?: "İşlem tamamlanamadı"}"
                    )
                }
            }
        }
    }

    // ── GÖREV 3: İLERİ VE GERİ İZLENEBİLİRLİK (TRACE) ──
    fun loadTrace(lot: LotItem) {
        _uiState.update {
            it.copy(
                selectedTraceLot = lot,
                isLoadingTrace = true,
                traceForwardData = null,
                traceBackwardData = null
            )
        }
        viewModelScope.launch {
            try {
                val fwdResp = apiClient.lotService.traceForward(lot.id)
                val bwdResp = apiClient.lotService.traceBackward(lot.id)

                _uiState.update {
                    it.copy(
                        isLoadingTrace = false,
                        traceForwardData = fwdResp.body(),
                        traceBackwardData = bwdResp.body()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingTrace = false,
                        errorMessage = "İzleme verisi alınamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearTrace() {
        _uiState.update {
            it.copy(
                selectedTraceLot = null,
                traceForwardData = null,
                traceBackwardData = null
            )
        }
    }

    // ── GÖREV 3: GERİ ÇAĞIRMA (RECALL & BLOKAJ) ──
    fun loadRecalls() {
        viewModelScope.launch {
            try {
                val resp = apiClient.recallService.getRecalls()
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(recallsList = resp.body()!!) }
                }
            } catch (_: Exception) {}
        }
    }

    fun initiateRecall(lotId: Int, reason: String, onSuccess: () -> Unit) {
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Lütfen geri çağırma gerekçesini yazınız.") }
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
                            successMessage = "⛔ Geri çağırma başlatıldı! İlgili tüm partiler bloke edildi ve satışları durduruldu."
                        )
                    }
                    loadLots()
                    loadRecalls()
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

    fun setFilter(filter: String) {
        _uiState.update { it.copy(filterType = filter) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null, isBlockedAlert = false) }
    }
}
