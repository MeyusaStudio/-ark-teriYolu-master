package com.avatar.app.ui.invoices

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.Invoice
import com.avatar.app.data.model.User
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoicesUiState(
    val isLoading: Boolean = false,
    val invoices: List<Invoice> = emptyList(),
    val currentUser: User? = null,
    val filterType: String = "all", // "all", "hizmet", "satis"
    val searchQuery: String = "",
    val selectedInvoice: Invoice? = null,
    val errorMessage: String? = null
) {
    // Sadece mevcut kullanıcı/işletme ile doğrudan ilgili olan faturalar
    val roleInvoices: List<Invoice>
        get() {
            val user = currentUser ?: return invoices
            val uid = user.id
            val uEmail = user.email.lowercase().trim()
            val uRole = user.role.lowercase().trim()

            return invoices.filter { inv ->
                val isSender = inv.senderId == uid || (inv.senderEmail?.lowercase()?.trim() == uEmail)
                val isReceiver = inv.receiverId == uid || (inv.receiverEmail?.lowercase()?.trim() == uEmail)

                when (uRole) {
                    "sucukcu" -> {
                        // Sucukçu sadece fason üretim işçilik hizmeti faturasında düzenleyendir (sender)
                        // Mamul satış faturalarını kesinlikle görmez
                        inv.invoiceType == "hizmet" && isSender
                    }
                    "market" -> {
                        // Market sadece toptan mamul alımında alıcıdır (receiver)
                        // Fason hizmet veya başka işletmelerin faturalarını kesinlikle görmez
                        inv.invoiceType == "satis" && isReceiver
                    }
                    "hayvanci" -> {
                        // Hayvancı fason üretim hizmetinde alıcıdır (gider),
                        // toptan mamul satışında ise satıcıdır (ciro).
                        (inv.invoiceType == "hizmet" && isReceiver) || (inv.invoiceType == "satis" && isSender)
                    }
                    else -> isSender || isReceiver
                }
            }
        }

    val filteredInvoices: List<Invoice>
        get() {
            var list = when (filterType) {
                "hizmet" -> roleInvoices.filter { it.invoiceType == "hizmet" }
                "satis" -> roleInvoices.filter { it.invoiceType == "satis" }
                else -> roleInvoices
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase().trim()
                list = list.filter {
                    it.invoiceNumber.lowercase().contains(q) ||
                    (it.senderName?.lowercase()?.contains(q) == true) ||
                    (it.receiverName?.lowercase()?.contains(q) == true) ||
                    (it.productName?.lowercase()?.contains(q) == true) ||
                    (it.lotNumber?.lowercase()?.contains(q) == true)
                }
            }
            return list
        }

    val totalAmount: Double
        get() = roleInvoices.sumOf { it.totalAmount }

    val serviceInvoicesCount: Int
        get() = roleInvoices.count { it.invoiceType == "hizmet" }

    val salesInvoicesCount: Int
        get() = roleInvoices.count { it.invoiceType == "satis" }
}

class InvoicesViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(InvoicesUiState())
    val uiState: StateFlow<InvoicesUiState> = _uiState.asStateFlow()

    init {
        loadInvoices()
    }

    fun setCurrentUser(user: User) {
        val prev = _uiState.value.currentUser
        _uiState.update { it.copy(currentUser = user) }
        if (prev?.id != user.id || _uiState.value.invoices.isEmpty()) {
            loadInvoices()
        }
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = apiClient.invoiceService.getInvoices()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        invoices = resp.body() ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Faturalar yüklenemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setFilterType(type: String) {
        _uiState.update { it.copy(filterType = type) }
    }

    fun setSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun openInvoiceDetail(invoice: Invoice) {
        _uiState.update { it.copy(selectedInvoice = invoice) }
    }

    fun closeInvoiceDetail() {
        _uiState.update { it.copy(selectedInvoice = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
