package com.avatar.app.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.Order
import com.avatar.app.data.model.OrderRawMaterial
import com.avatar.app.data.model.UpdateOrderStatusRequest
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedOrderDetail: Order? = null,
    val selectedOrderRawMaterials: List<OrderRawMaterial> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val isUpdatingStatus: Boolean = false
)

class OrdersViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = apiClient.orderService.getOrders()
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(isLoading = false, orders = resp.body()!!) }
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
                        errorMessage = "Siparişler alınamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadOrderDetail(order: Order) {
        _uiState.update {
            it.copy(
                selectedOrderDetail = order,
                selectedOrderRawMaterials = order.rawMaterials ?: emptyList(),
                isLoadingDetail = true
            )
        }
        viewModelScope.launch {
            try {
                val resp = apiClient.orderService.getOrderById(order.id)
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    _uiState.update {
                        it.copy(
                            selectedOrderDetail = body.order,
                            selectedOrderRawMaterials = body.rawMaterials,
                            isLoadingDetail = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingDetail = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingDetail = false) }
            }
        }
    }

    fun clearDetail() {
        _uiState.update { it.copy(selectedOrderDetail = null, selectedOrderRawMaterials = emptyList()) }
    }

    // Sucukçunun siparişi onaylaması
    fun approveOrder(orderId: Int, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true, errorMessage = null) }
            try {
                val resp = apiClient.orderService.updateOrderStatus(
                    orderId,
                    UpdateOrderStatusRequest(status = "onaylandi")
                )
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isUpdatingStatus = false,
                            selectedOrderDetail = null,
                            successMessage = "Sipariş onaylandı! Hammaddeler hayvancının stoğundan düşüldü ve doğrudan üretime aktarıldı."
                        )
                    }
                    loadOrders()
                    onSuccess?.invoke()
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdatingStatus = false,
                            errorMessage = apiClient.parseError(resp.errorBody()?.string())
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingStatus = false,
                        errorMessage = "Durum güncellenemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
