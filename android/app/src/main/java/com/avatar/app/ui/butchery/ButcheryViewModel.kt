package com.avatar.app.ui.butchery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.app.data.local.AppDatabase
import com.avatar.app.data.local.entity.ButcheryOperationEntity
import com.avatar.app.data.model.*
import com.avatar.app.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CalculatedPart(
    val partName: String,
    val weightKg: Double,
    val allocatedCost: Double,
    val unitCost: Double,
    val ratio: Double,
    val multiplier: Double
)

data class ButcheryUiState(
    val isLoading: Boolean = false,
    val operations: List<ButcheryRecord> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Detail Dialog State
    val selectedDetailRecord: ButcheryRecord? = null,
    val selectedDetailParts: List<ButcheryPartDetail> = emptyList(),
    val isLoadingDetail: Boolean = false,

    // New Butchery Form State
    val selectedAnimalType: AnimalTypeDefinition = ButcheryConstants.ANIMAL_TYPES[0],
    val earTag: String = "TR-06-KUZU-001",
    val operationDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val inputWeightKg: Double = 25.0,
    val purchaseCost: Double = 287.50,
    val distributionMethod: String = "agirlik", // "agirlik" or "deger"
    val notes: String = "",
    val partWeights: Map<String, Double> = emptyMap(),
    val isSubmitting: Boolean = false,

    // ML AI Prediction State
    val mlResult: MLButcheryResult? = null,
    val isMlLoading: Boolean = false
) {
    val totalOutputKg: Double
        get() = partWeights.values.sum()

    val wasteKg: Double
        get() = kotlin.math.max(0.0, (inputWeightKg - totalOutputKg).let { Math.round(it * 1000.0) / 1000.0 })

    val fireRate: Double
        get() = if (inputWeightKg > 0) (wasteKg / inputWeightKg) * 100.0 else 0.0

    val unitCostInput: Double
        get() = if (inputWeightKg > 0) purchaseCost / inputWeightKg else 0.0

    val calculatedParts: List<CalculatedPart>
        get() {
            val list = mutableListOf<CalculatedPart>()
            val cost = purchaseCost
            val outKg = totalOutputKg

            var totalPoints = 0.0
            if (distributionMethod == "deger") {
                selectedAnimalType.parts.forEach { partName ->
                    val w = partWeights[partName] ?: 0.0
                    val mult = ButcheryConstants.getMultiplier(partName)
                    totalPoints += (w * mult)
                }
            }

            selectedAnimalType.parts.forEach { partName ->
                val w = partWeights[partName] ?: 0.0
                val mult = ButcheryConstants.getMultiplier(partName)
                val ratio = if (distributionMethod == "deger" && totalPoints > 0) {
                    (w * mult) / totalPoints
                } else if (outKg > 0) {
                    w / outKg
                } else {
                    0.0
                }

                val allocated = cost * ratio
                val unitCost = if (w > 0) allocated / w else 0.0

                list.add(
                    CalculatedPart(
                        partName = partName,
                        weightKg = w,
                        allocatedCost = allocated,
                        unitCost = unitCost,
                        ratio = ratio,
                        multiplier = mult
                    )
                )
            }
            return list
        }
}

class ButcheryViewModel(application: Application) : AndroidViewModel(application) {
    private val apiClient = ApiClient(application)
    private val database = AppDatabase.getDatabase(application)
    private val butcheryDao = database.butcheryDao()

    private val _uiState = MutableStateFlow(ButcheryUiState())
    val uiState: StateFlow<ButcheryUiState> = _uiState.asStateFlow()

    init {
        loadOperations()
        initDefaultLambForm()
    }

    fun loadOperations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = apiClient.butcheryService.getOperations()
                if (response.isSuccessful && response.body() != null) {
                    val ops = response.body()!!
                    _uiState.update { it.copy(isLoading = false, operations = ops) }

                    // Cache in Room SQLite
                    val entities = ops.map { op ->
                        ButcheryOperationEntity(
                            id = op.id,
                            animalType = op.animalType ?: "koyun",
                            animalEarTag = op.animalEarTag,
                            operationDate = op.operationDate,
                            inputWeightKg = op.inputWeightKg,
                            totalOutputKg = op.totalOutputKg,
                            wasteKg = op.wasteKg,
                            totalCost = op.totalCost,
                            unitCostInput = op.unitCostInput,
                            distributionMethod = op.distributionMethod ?: "agirlik",
                            currency = op.currency ?: "EUR",
                            notes = op.notes
                        )
                    }
                    butcheryDao.insertOperations(entities)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = apiClient.parseError(response.errorBody()?.string())
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Parçalama verileri alınamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectAnimalType(type: AnimalTypeDefinition) {
        val initialMap = type.parts.associateWith { 0.0 }
        _uiState.update {
            it.copy(
                selectedAnimalType = type,
                earTag = type.defaultTag,
                inputWeightKg = type.defaultWeight,
                purchaseCost = type.defaultCost,
                partWeights = initialMap
            )
        }
        fetchMlPrediction()
    }

    fun updatePartWeight(partName: String, weight: Double) {
        _uiState.update {
            val updated = it.partWeights.toMutableMap()
            updated[partName] = weight
            it.copy(partWeights = updated)
        }
    }

    fun updateFormFields(
        inputWeightKg: Double? = null,
        purchaseCost: Double? = null,
        earTag: String? = null,
        distributionMethod: String? = null,
        notes: String? = null
    ) {
        _uiState.update {
            it.copy(
                inputWeightKg = inputWeightKg ?: it.inputWeightKg,
                purchaseCost = purchaseCost ?: it.purchaseCost,
                earTag = earTag ?: it.earTag,
                distributionMethod = distributionMethod ?: it.distributionMethod,
                notes = notes ?: it.notes
            )
        }
    }

    fun fetchMlPrediction() {
        val state = _uiState.value
        val animalKey = when (state.selectedAnimalType.key) {
            "dana" -> "inek"
            "kuzu" -> "koyun"
            else -> state.selectedAnimalType.key
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isMlLoading = true) }
            try {
                val req = ButcheryPredictRequest(
                    animalType = animalKey,
                    entryWeightKg = state.inputWeightKg,
                    purchaseCost = state.purchaseCost
                )
                val resp = apiClient.mlService.predictButchery(req)
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.update { it.copy(isMlLoading = false, mlResult = resp.body()) }
                } else {
                    _uiState.update { it.copy(isMlLoading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isMlLoading = false) }
            }
        }
    }

    fun applyMlPredictions() {
        val result = _uiState.value.mlResult ?: return
        val currentParts = _uiState.value.selectedAnimalType.parts
        val newWeights = _uiState.value.partWeights.toMutableMap()

        currentParts.forEach { partName ->
            val mlPart = result.parts.find {
                it.partName.lowercase().contains(partName.lowercase()) ||
                partName.lowercase().contains(it.partName.lowercase())
            }
            if (mlPart != null) {
                newWeights[partName] = mlPart.predictedWeightKg
            }
        }
        _uiState.update {
            it.copy(
                partWeights = newWeights,
                successMessage = "🤖 Yapay zeka tahminleri parça ağırlıklarına başarıyla aktarıldı!"
            )
        }
    }

    fun initDefaultLambForm() {
        val koyun = ButcheryConstants.ANIMAL_TYPES[0]
        selectAnimalType(koyun)
    }

    // Görev 2: Standart 287.50 € Kuzu Parçalama Senaryosu
    fun applyScenario287() {
        val koyun = ButcheryConstants.ANIMAL_TYPES[0]
        val weights = mapOf(
            "Pirzola" to 3.50,
            "But" to 6.00,
            "Sırt / Kontrfile" to 2.50,
            "Kol / Omuz" to 4.50,
            "Kaburga" to 2.50,
            "Gerdan" to 1.50,
            "Kıyma / Kuşbaşı" to 0.50,
            "İncik" to 0.50,
            "Bağırsak" to 0.50
        )
        _uiState.update {
            it.copy(
                selectedAnimalType = koyun,
                inputWeightKg = 25.0,
                purchaseCost = 287.50,
                earTag = "TR-06-KUZU-001",
                notes = "Standart kuzu parçalama (25 kg Kuzu, 11.50 €/kg)",
                partWeights = weights,
                successMessage = "287.50 € Standart Kuzu senaryosu dolduruldu (25 kg Giriş · 22 kg Çıkan · 3 kg Fire)!"
            )
        }
    }

    // Görev 2: 351.00 € Ek Maliyetli Kuzu Parçalama Senaryosu
    fun applyScenario351() {
        val koyun = ButcheryConstants.ANIMAL_TYPES[0]
        val weights = mapOf(
            "Pirzola" to 3.50,
            "But" to 6.00,
            "Sırt / Kontrfile" to 2.50,
            "Kol / Omuz" to 4.50,
            "Kaburga" to 2.50,
            "Gerdan" to 1.50,
            "Kıyma / Kuşbaşı" to 0.50,
            "İncik" to 0.50,
            "Bağırsak" to 0.50
        )
        _uiState.update {
            it.copy(
                selectedAnimalType = koyun,
                inputWeightKg = 25.0,
                purchaseCost = 351.00,
                earTag = "TR-06-KUZU-002",
                notes = "Ek masraflı alternatif kuzu maliyet senaryosu (351.00 €)",
                partWeights = weights,
                successMessage = "351.00 € Ek Maliyetli Kuzu senaryosu dolduruldu!"
            )
        }
    }

    fun submitButchery(onSuccess: () -> Unit) {
        val state = _uiState.value
        val validParts = state.calculatedParts
            .filter { it.weightKg > 0.0 }
            .map {
                CreatePartInput(
                    partName = it.partName,
                    actualWeightKg = it.weightKg,
                    isByproduct = it.partName.lowercase().contains("bağırsak")
                )
            }

        if (validParts.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Lütfen en az bir parça için ağırlık giriniz.") }
            return
        }

        if (state.inputWeightKg <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Giriş ağırlığı 0'dan büyük olmalıdır.") }
            return
        }

        if (state.purchaseCost <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Alış maliyeti 0'dan büyük olmalıdır.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val req = CreateButcheryRequest(
                    animalType = state.selectedAnimalType.key,
                    animalEarTag = state.earTag.ifBlank { null },
                    operationDate = state.operationDate,
                    inputWeightKg = state.inputWeightKg,
                    totalCost = state.purchaseCost,
                    unitCostInput = state.unitCostInput,
                    distributionMethod = state.distributionMethod,
                    currency = "EUR",
                    notes = state.notes.ifBlank { null },
                    parts = validParts
                )

                val resp = apiClient.butcheryService.createOperation(req)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            successMessage = "Parçalama ve lot oluşturma başarıyla kaydedildi!"
                        )
                    }
                    loadOperations()
                    onSuccess()
                } else {
                    val err = apiClient.parseError(resp.errorBody()?.string())
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = err) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Kayıt sırasında hata oluştu: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadOperationDetail(record: ButcheryRecord) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedDetailRecord = record,
                    selectedDetailParts = record.parts ?: emptyList(),
                    isLoadingDetail = true
                )
            }
            try {
                val resp = apiClient.butcheryService.getOperationById(record.id)
                if (resp.isSuccessful && resp.body() != null) {
                    val detail = resp.body()!!
                    _uiState.update {
                        it.copy(
                            selectedDetailRecord = detail.operation,
                            selectedDetailParts = detail.parts ?: emptyList(),
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
        _uiState.update { it.copy(selectedDetailRecord = null, selectedDetailParts = emptyList()) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
