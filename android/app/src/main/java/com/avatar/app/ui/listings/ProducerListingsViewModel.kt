package com.avatar.app.ui.listings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.model.*
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class PartSuitabilityResult(
    val status: String, // "recommended", "warning", "unsuitable", "neutral"
    val badge: String,
    val description: String
)

data class ProducerListingsUiState(
    val isLoading: Boolean = false,
    val listings: List<ProducerListing> = emptyList(),
    val myListings: List<ProducerListing> = emptyList(),
    val availableParts: List<AvailablePart> = emptyList(),
    val rawMaterialsStock: List<RawMaterial> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Filter states for Hayvancı
    val selectedProductFilter: String? = null, // "Sucuk", "Salam", "Sosis", "Pastırma"
    val selectedAnimalFilter: String? = null,   // "inek", "koyun", "tavuk"
    val searchQuery: String = "",

    // Create Listing State (Sucukçu)
    val isCreatingListing: Boolean = false,
    val selectedTemplate: ProductTemplate = ProductTemplates.ITEMS[0],
    val servicePrice: Double = 3.50,
    val minOrderQty: Double = 5.0,
    val listingDescription: String = "Geleneksel fermente Türk sucuğu fason üretimi",
    val ingredientAmounts: Map<String, Double> = mapOf(
        "Et" to 3.20,
        "Yağ" to 0.50,
        "Sarımsak" to 0.15,
        "Bağırsak" to 0.10,
        "Tuz" to 0.10,
        "Baharat" to 0.25
    ),

    // Order Creation State (Hayvancı)
    val isOrdering: Boolean = false,
    val selectedListingForOrder: ProducerListing? = null,
    val orderAnimalFilter: String? = null, // "koyun", "inek", "tavuk"
    val selectedPart: AvailablePart? = null,
    val selectedPartKg: Double = 0.0,
    val stockSelections: Map<String, Double> = emptyMap(), // materialName -> kg
    val orderNote: String = "",

    // ML Prediction State (Hayvancı)
    val selectedListingForMl: ProducerListing? = null,
    val mlMaterials: Map<String, Double> = emptyMap(),
    val mlResult: MLProductionResult? = null,
    val isMlPredicting: Boolean = false
) {
    val allAvailableMeats: List<AvailablePart>
        get() {
            val list = mutableListOf<AvailablePart>()
            list.addAll(availableParts.filter { it.availableKg > 0 })

            val existingLotIds = list.map { it.lotId }.toSet()
            rawMaterialsStock.forEach { rm ->
                if (rm.quantity > 0 && rm.isMeat()) {
                    val lotId = rm.lotId ?: rm.id
                    if (!existingLotIds.contains(lotId)) {
                        list.add(rm.toAvailablePart())
                    }
                }
            }
            return list
        }

    val totalAvailMeatKg: Double
        get() = allAvailableMeats.sumOf { it.availableKg }

    fun getAnimalMeatKg(animal: String): Double {
        return allAvailableMeats
            .filter { it.animalType.contains(animal, ignoreCase = true) }
            .sumOf { it.availableKg }
    }

    val hasActiveFilters: Boolean
        get() = selectedProductFilter != null || selectedAnimalFilter != null || searchQuery.isNotBlank()

    val orderFilteredMeats: List<AvailablePart>
        get() {
            if (orderAnimalFilter.isNullOrBlank()) return allAvailableMeats
            val target = orderAnimalFilter.lowercase()
            return allAvailableMeats.filter {
                val a = it.animalType.lowercase()
                when {
                    target.contains("koyun") || target.contains("kuzu") -> a.contains("koyun") || a.contains("kuzu")
                    target.contains("inek") || target.contains("dana") -> a.contains("inek") || a.contains("dana")
                    target.contains("tavuk") || target.contains("pilic") -> a.contains("tavuk")
                    else -> a.contains(target)
                }
            }
        }

    fun getMissingStockMaterials(listing: ProducerListing?): List<String> {
        if (listing == null || selectedPartKg <= 0) return emptyList()
        val mats = listing.requiredMaterialsParsed.filterKeys { !it.contains("et", ignoreCase = true) }
        if (mats.isEmpty()) return emptyList()
        val etEntry = listing.requiredMaterialsParsed.entries.firstOrNull { it.key.contains("et", ignoreCase = true) }
        val etBaseline = etEntry?.value?.amount?.takeIf { it > 0 } ?: 1.0
        val factor = selectedPartKg / etBaseline

        val missing = mutableListOf<String>()
        mats.forEach { (matName, spec) ->
            val needed = stockSelections[matName] ?: (Math.round(spec.amount * factor * 100.0) / 100.0)
            if (needed > 0) {
                val stockItem = rawMaterialsStock.find {
                    it.materialType.contains(matName, ignoreCase = true) && it.lotStatus == "active"
                }
                val avail = stockItem?.quantity ?: 0.0
                if (avail < needed || avail <= 0.0) {
                    missing.add("$matName (İhtiyaç: ${String.format(Locale.US, "%.2f", needed)} ${spec.unit}, Depo: ${String.format(Locale.US, "%.2f", avail)} ${spec.unit})")
                }
            }
        }
        return missing
    }

    val filteredListings: List<ProducerListing>
        get() {
            return listings.filter { l ->
                val matchesProduct = selectedProductFilter == null || l.productName.contains(selectedProductFilter, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                        l.productName.contains(searchQuery, ignoreCase = true) ||
                        (l.producerName ?: "").contains(searchQuery, ignoreCase = true) ||
                        (l.description ?: "").contains(searchQuery, ignoreCase = true)
                val matchesAnimal = selectedAnimalFilter == null || when (selectedAnimalFilter.lowercase()) {
                    "inek" -> listOf("sucuk", "salam", "sosis", "pastırma", "pastirma").any { l.productName.contains(it, ignoreCase = true) }
                    "koyun" -> listOf("sucuk", "salam", "sosis").any { l.productName.contains(it, ignoreCase = true) }
                    "tavuk" -> listOf("sucuk", "salam", "sosis").any { l.productName.contains(it, ignoreCase = true) }
                    else -> true
                }
                matchesProduct && matchesSearch && matchesAnimal
            }
        }
}

class ProducerListingsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)

    private val _uiState = MutableStateFlow(ProducerListingsUiState())
    val uiState: StateFlow<ProducerListingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        loadAllProducerListings()
        loadMyProducerListings()
        loadAvailableParts()
        loadStock()
    }

    fun loadAllProducerListings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = apiClient.listingService.getProducerListings()
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(isLoading = false, listings = resp.body()!!) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "İlanlar alınamadı: ${e.localizedMessage}") }
            }
        }
    }

    fun loadMyProducerListings() {
        viewModelScope.launch {
            try {
                val resp = apiClient.listingService.getMyProducerListings()
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(myListings = resp.body()!!) }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadAvailableParts() {
        viewModelScope.launch {
            try {
                val resp = apiClient.butcheryService.getAvailableParts()
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(availableParts = resp.body()!!) }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadStock() {
        viewModelScope.launch {
            try {
                val resp = apiClient.rawMaterialService.getMaterials()
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(rawMaterialsStock = resp.body()!!) }
                }
            } catch (_: Exception) {}
        }
    }

    // ── AKILLI PARÇA UYUM MOTORU (Web ile birebir) ──
    fun checkPartSuitability(productName: String, partName: String): PartSuitabilityResult {
        val norm = partName.lowercase().trim()
        val prodNorm = productName.lowercase().trim()

        if (prodNorm.contains("pastırma") || prodNorm.contains("pastirma")) {
            val ideal = listOf("antrikot", "kontrfile", "sokum", "bonfile", "nuat", "tranç")
            val unsuitable = listOf("kaburga", "gerdan", "kol", "but", "incik", "bağırsak", "tavuk")
            if (unsuitable.any { norm.contains(it) }) {
                return PartSuitabilityResult("unsuitable", "❌ Uyumsuz", "Pastırma için uygun değildir. Pastırma sadece blok bifteklik etlerle (Antrikot, Kontrfile, Sokum) üretilebilir.")
            }
            if (ideal.any { norm.contains(it) }) {
                return PartSuitabilityResult("recommended", "✅ İdeal Parça", "Pastırma üretimi için ideal ve tavsiye edilen et parçasıdır.")
            }
        }

        if (prodNorm.contains("sucuk")) {
            if (norm.contains("bağırsak") || norm.contains("tavuk")) {
                return PartSuitabilityResult("unsuitable", "❌ Uyumsuz", "Kırmızı et sucuğu için ana et olarak uygun değildir.")
            }
            val luxury = listOf("antrikot", "kontrfile", "bonfile")
            if (luxury.any { norm.contains(it) }) {
                return PartSuitabilityResult("warning", "⚠️ Lüks Parça", "Bifteklik değerli et. Kıyma/harç yerine pastırma veya taze tüketim için önerilir.")
            }
            val ideal = listOf("kaburga", "gerdan", "kol", "but", "incik", "kuşbaşı", "kıyma")
            if (ideal.any { norm.contains(it) }) {
                return PartSuitabilityResult("recommended", "✅ İdeal Parça", "Sucuk üretimi için ideal kıyma ve harç etidir.")
            }
        }

        return PartSuitabilityResult("neutral", "Kullanılabilir", "Standart et parçası olarak fason üretimde kullanılabilir.")
    }

    // Sucukçu İlan Açma
    fun selectProductTemplate(template: ProductTemplate) {
        val amounts = template.ingredients.associate { it.name to it.defaultAmount }
        _uiState.update {
            it.copy(
                selectedTemplate = template,
                listingDescription = "${template.label} fason üretimi (${template.description})",
                ingredientAmounts = amounts
            )
        }
    }

    fun updateServicePrice(price: Double) {
        _uiState.update { it.copy(servicePrice = price) }
    }

    fun updateMinOrderQty(qty: Double) {
        _uiState.update { it.copy(minOrderQty = qty) }
    }

    fun updateIngredientAmount(name: String, amount: Double) {
        _uiState.update {
            val map = it.ingredientAmounts.toMutableMap()
            map[name] = amount
            it.copy(ingredientAmounts = map)
        }
    }

    fun createProducerListing(onSuccess: () -> Unit) {
        val state = _uiState.value
        val required = state.ingredientAmounts.filter { it.value > 0 }.mapValues { (name, amount) ->
            val tmpl = state.selectedTemplate.ingredients.find { it.name == name }
            IngredientSpec(amount = amount, unit = tmpl?.unit ?: "kg")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingListing = true, errorMessage = null) }
            try {
                val req = CreateProducerListingRequest(
                    productName = state.selectedTemplate.label,
                    description = state.listingDescription,
                    servicePrice = state.servicePrice,
                    serviceUnit = "kg",
                    minOrderQty = state.minOrderQty,
                    requiredMaterials = required
                )
                val resp = apiClient.listingService.createProducerListing(req)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isCreatingListing = false,
                            successMessage = "Fason üretim ilanınız başarıyla yayınlandı!"
                        )
                    }
                    loadAllProducerListings()
                    loadMyProducerListings()
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isCreatingListing = false,
                            errorMessage = apiClient.parseError(resp.errorBody()?.string())
                        )
                    }
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

    // Hayvancı Fason Sipariş Başlatma
    fun openOrderDialog(listing: ProducerListing) {
        loadAvailableParts()
        loadStock()
        _uiState.update {
            it.copy(
                selectedListingForOrder = listing,
                orderAnimalFilter = null,
                selectedPart = null,
                selectedPartKg = 0.0,
                stockSelections = emptyMap(),
                orderNote = ""
            )
        }
    }

    fun setOrderAnimalFilter(animal: String?) {
        _uiState.update {
            val isCurrentPartValid = animal == null || it.selectedPart == null || it.selectedPart.animalType.contains(animal, ignoreCase = true)
            it.copy(
                orderAnimalFilter = animal,
                selectedPart = if (isCurrentPartValid) it.selectedPart else null,
                selectedPartKg = if (isCurrentPartValid) it.selectedPartKg else 0.0
            )
        }
    }

    fun closeOrderDialog() {
        _uiState.update { it.copy(selectedListingForOrder = null) }
    }

    fun selectPartForOrder(part: AvailablePart, kg: Double) {
        val listing = _uiState.value.selectedListingForOrder
        val autoInputs = mutableMapOf<String, Double>()
        if (listing != null && kg > 0) {
            val mats = listing.requiredMaterialsParsed
            val etEntry = mats.entries.firstOrNull { it.key.contains("et", ignoreCase = true) }
            val etBaseline = etEntry?.value?.amount?.takeIf { it > 0 } ?: 1.0
            val factor = kg / etBaseline
            mats.forEach { (key, spec) ->
                if (!key.contains("et", ignoreCase = true)) {
                    val calculated = (Math.round(spec.amount * factor * 100.0) / 100.0)
                    autoInputs[key] = calculated
                }
            }
        }

        _uiState.update {
            it.copy(
                selectedPart = part,
                selectedPartKg = kg,
                stockSelections = if (autoInputs.isNotEmpty()) autoInputs else it.stockSelections
            )
        }
    }

    // Reçeteye göre diğer hammaddeleri otomatik doldurma (Web ProducerListingsPage autoFillStockInputs)
    fun autoFillMaterialsFromRecipe() {
        val state = _uiState.value
        val listing = state.selectedListingForOrder ?: return
        val kg = state.selectedPartKg
        if (kg <= 0) {
            _uiState.update { it.copy(errorMessage = "Lütfen önce bir et parçasından miktar giriniz.") }
            return
        }
        val mats = listing.requiredMaterialsParsed
        val etEntry = mats.entries.firstOrNull { it.key.contains("et", ignoreCase = true) }
        val etBaseline = etEntry?.value?.amount?.takeIf { it > 0 } ?: 1.0
        val factor = kg / etBaseline
        val autoInputs = mutableMapOf<String, Double>()
        mats.forEach { (key, spec) ->
            if (!key.contains("et", ignoreCase = true)) {
                val calculated = (Math.round(spec.amount * factor * 100.0) / 100.0)
                autoInputs[key] = calculated
            }
        }
        _uiState.update {
            it.copy(
                stockSelections = autoInputs,
                successMessage = "Reçete oranlarına göre diğer hammaddeler otomatik dolduruldu!"
            )
        }
    }

    fun updateStockSelection(materialName: String, kg: Double) {
        _uiState.update {
            val map = it.stockSelections.toMutableMap()
            map[materialName] = kg
            it.copy(stockSelections = map)
        }
    }

    // Hayvancı Yapay Zeka (AI / ML) Tahmin
    fun openMlPrediction(listing: ProducerListing) {
        val initialMats = mutableMapOf<String, Double>()
        val parsed = listing.requiredMaterialsParsed
        parsed.forEach { (name, spec) ->
            initialMats[name] = if (spec.amount > 0) spec.amount else 1.0
        }
        if (initialMats.isEmpty()) {
            initialMats["Et"] = 3.5
            initialMats["Baharat"] = 0.5
        }
        _uiState.update {
            it.copy(
                selectedListingForMl = listing,
                mlMaterials = initialMats,
                mlResult = null,
                isMlPredicting = false
            )
        }
        runMlPrediction()
    }

    fun closeMlPrediction() {
        _uiState.update {
            it.copy(selectedListingForMl = null, mlResult = null)
        }
    }

    fun updateMlMaterial(name: String, amount: Double) {
        _uiState.update {
            val map = it.mlMaterials.toMutableMap()
            map[name] = amount
            it.copy(mlMaterials = map)
        }
    }

    fun runMlPrediction() {
        val state = _uiState.value
        val listing = state.selectedListingForMl ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isMlPredicting = true) }
            try {
                val req = ProductionPredictRequest(
                    productName = listing.productName,
                    producerId = listing.producerId,
                    servicePricePerUnit = listing.servicePrice,
                    materials = state.mlMaterials
                )
                val resp = apiClient.mlService.predictProduction(req)
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(isMlPredicting = false, mlResult = resp.body()) }
                } else {
                    _uiState.update { it.copy(isMlPredicting = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isMlPredicting = false) }
            }
        }
    }

    fun submitOrder(onSuccess: () -> Unit) {
        val state = _uiState.value
        val listing = state.selectedListingForOrder ?: return

        if (state.selectedPart == null || state.selectedPartKg <= 0) {
            _uiState.update { it.copy(errorMessage = "Lütfen üretilecek ürün için kendi deponuzdan bir et parçası seçiniz.") }
            return
        }

        if (state.selectedPartKg > state.selectedPart.availableKg) {
            _uiState.update {
                it.copy(errorMessage = "Girdiğiniz et miktarı (${state.selectedPartKg} kg), depodaki mevcut stoktan (${state.selectedPart.availableKg} kg) fazladır.")
            }
            return
        }

        val missing = state.getMissingStockMaterials(listing)
        if (missing.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Deponuzda şu ürünler eksik veya yetersiz: ${missing.joinToString(", ")}. Depoda malzeme bulunmadan sipariş tamamlanamaz."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isOrdering = true, errorMessage = null) }
            try {
                val rawMaterials = mutableListOf<OrderRawMaterialInput>()

                // 1. Et parçası girdisi (Hayvancının parça lotu)
                rawMaterials.add(
                    OrderRawMaterialInput(
                        materialType = state.selectedPart.partName,
                        quantity = state.selectedPartKg,
                        unit = "kg",
                        unitPrice = 0.0, // Backend parça lotundan çeker
                        lotId = state.selectedPart.lotId
                    )
                )

                // 2. Depo hammaddeleri (Tuz, Baharat, Bağırsak vb.)
                state.stockSelections.forEach { (matName, qty) ->
                    if (qty > 0) {
                        val stockItem = state.rawMaterialsStock.find { it.materialType.contains(matName, ignoreCase = true) }
                        rawMaterials.add(
                            OrderRawMaterialInput(
                                materialType = matName,
                                quantity = qty,
                                unit = stockItem?.unit ?: "kg",
                                unitPrice = stockItem?.unitPrice ?: 0.0,
                                lotId = stockItem?.lotId
                            )
                        )
                    }
                }

                val totalKg = rawMaterials.sumOf { it.quantity }
                val expectedPackages = kotlin.math.floor(totalKg / 1.0) // 1.0 kg/paket

                val req = CreateOrderRequest(
                    sellerId = listing.producerId,
                    listingId = listing.id,
                    expectedQty = kotlin.math.max(1.0, expectedPackages),
                    notes = state.orderNote.ifBlank { "Mobil Fason Sipariş Talebi" },
                    rawMaterials = rawMaterials
                )

                val resp = apiClient.orderService.createOrder(req)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isOrdering = false,
                            selectedListingForOrder = null,
                            successMessage = "Fason üretim siparişiniz sucukçuya iletildi! Sucukçu onaylayınca et ve malzemeleriniz üretime alınacaktır."
                        )
                    }
                    loadAvailableParts()
                    loadStock()
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isOrdering = false,
                            errorMessage = apiClient.parseError(resp.errorBody()?.string())
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isOrdering = false,
                        errorMessage = "Sipariş verilemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setProductFilter(product: String?) {
        _uiState.update { it.copy(selectedProductFilter = product) }
    }

    fun setAnimalFilter(animal: String?) {
        _uiState.update { it.copy(selectedAnimalFilter = animal) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(selectedProductFilter = null, selectedAnimalFilter = null, searchQuery = "") }
    }

    fun setFilter(product: String?, search: String = _uiState.value.searchQuery) {
        _uiState.update { it.copy(selectedProductFilter = product, searchQuery = search) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
