package com.avatar.app.ui.butchery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.avatar.app.data.model.ButcheryConstants
import com.avatar.app.ui.theme.ForestGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewButcheryDialog(
    viewModel: ButcheryViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.selectedAnimalType.key, state.inputWeightKg, state.purchaseCost) {
        viewModel.fetchMlPrediction()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Yeni Parçalama Başlat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("İptal")
                            }
                            Button(
                                onClick = {
                                    viewModel.submitButchery(onSuccess = onDismiss)
                                },
                                enabled = !state.isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.weight(2f)
                            ) {
                                if (state.isSubmitting) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Kaydediliyor...")
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Parçalamayı Kaydet", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Hayvan Türü Seçimi
                    item {
                        Text(
                            "1. Hayvan Türü Seçiniz",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ButcheryConstants.ANIMAL_TYPES.forEach { type ->
                                val isSelected = state.selectedAnimalType.key == type.key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) ForestGreen.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) ForestGreen else Color.LightGray,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.selectAnimalType(type) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(type.emoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            type.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ForestGreen else Color.Unspecified
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hızlı Test Senaryoları (Yarışma Görevi 2)
                    if (state.selectedAnimalType.key == "koyun") {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF0FDF4)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.ElectricBolt,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Hızlı Yarışma Senaryoları (Görev 2)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.applyScenario287() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF15803D)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("287.50 € Standart", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.applyScenario351() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF15803D)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("351.00 € Ek Maliyet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Temel Bilgiler
                    item {
                        Text(
                            "2. Giriş ve Maliyet Bilgileri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.earTag,
                                onValueChange = { viewModel.updateFormFields(earTag = it) },
                                label = { Text("Küpe / Seri No", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = if (state.inputWeightKg > 0) state.inputWeightKg.toString() else "",
                                onValueChange = {
                                    val v = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateFormFields(inputWeightKg = v)
                                },
                                label = { Text("Giriş Ağırlığı (kg)", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (state.purchaseCost > 0) state.purchaseCost.toString() else "",
                                onValueChange = {
                                    val v = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateFormFields(purchaseCost = v)
                                },
                                label = { Text("Toplam Alış Maliyeti (€)", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = String.format(Locale.US, "%.2f €/kg", state.unitCostInput),
                                onValueChange = {},
                                label = { Text("Birim Maliyet", fontSize = 12.sp) },
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    // Maliyet Dağıtım Anahtarı
                    item {
                        Text(
                            "3. Maliyet Dağıtım Anahtarı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.distributionMethod == "agirlik",
                                onClick = { viewModel.updateFormFields(distributionMethod = "agirlik") },
                                label = { Text("⚖️ Ağırlık Bazlı", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = state.distributionMethod == "deger",
                                onClick = { viewModel.updateFormFields(distributionMethod = "deger") },
                                label = { Text("💎 Değer Çarpanı Bazlı", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Kütle Dengesi ve Fire Özeti
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.wasteKg >= 0) Color(0xFFF8FAFC) else Color(0xFFFEF2F2)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Çıkan Toplam", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        String.format(Locale.US, "%.2f kg", state.totalOutputKg),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Fire Ağırlığı", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        String.format(Locale.US, "%.2f kg", state.wasteKg),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (state.wasteKg > 0) Color(0xFFDC2626) else ForestGreen
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Fire Oranı", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        String.format(Locale.US, "%%%.1f", state.fireRate),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (state.fireRate > 15) Color(0xFFDC2626) else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    // Parça Ağırlıkları ve Dağıtılan Canlı Maliyetler
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "4. Parça Ağırlıkları ve Maliyet Dağıtımı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (state.isMlLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else if (state.mlResult != null) {
                                val badgeText = when (state.mlResult?.method) {
                                    "random_forest" -> "🤖 RandomForest (${state.mlResult?.sampleCount} kayıt)"
                                    "linear" -> "🤖 Lineer Regresyon (${state.mlResult?.sampleCount} kayıt)"
                                    else -> "📐 Reçete Referans Oranları"
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF3E8FF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8B4FE))
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7E22CE),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // YZ Tahminlerini Uygula Butonu
                    item {
                        Button(
                            onClick = { viewModel.applyMlPredictions() },
                            enabled = state.mlResult != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🤖 YZ Tahminlerini Uygula (Tüm Parçaları Doldur)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(state.calculatedParts) { part ->
                        val mlPart = state.mlResult?.parts?.find {
                            it.partName.lowercase().contains(part.partName.lowercase()) ||
                            part.partName.lowercase().contains(it.partName.lowercase())
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(part.partName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    if (mlPart != null) {
                                        Text(
                                            "🤖 YZ: ~${String.format(Locale.US, "%.2f", mlPart.predictedWeightKg)} kg (%${String.format(Locale.US, "%.0f", mlPart.expectedRatio * 100)})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF7E22CE)
                                        )
                                    }
                                    if (state.distributionMethod == "deger") {
                                        Text(
                                            "Çarpan: ${part.multiplier}x · Pay: %${String.format(Locale.US, "%.1f", part.ratio * 100)}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    } else {
                                        Text(
                                            "Pay: %${String.format(Locale.US, "%.1f", part.ratio * 100)}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = if (part.weightKg > 0) part.weightKg.toString() else "",
                                    onValueChange = {
                                        val w = it.toDoubleOrNull() ?: 0.0
                                        viewModel.updatePartWeight(part.partName, w)
                                    },
                                    placeholder = { Text("0.0", fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    suffix = { Text("kg", fontSize = 11.sp) }
                                )

                                Column(
                                    modifier = Modifier.weight(1.2f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        String.format(Locale.US, "%.2f €", part.allocatedCost),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = ForestGreen
                                    )
                                    Text(
                                        String.format(Locale.US, "%.2f €/kg", part.unitCost),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
