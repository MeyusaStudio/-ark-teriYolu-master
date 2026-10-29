package com.avatar.app.ui.listings

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
import androidx.compose.material.icons.filled.Send
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
import com.avatar.app.data.model.ProducerListing
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFasonOrderDialog(
    listing: ProducerListing,
    viewModel: ProducerListingsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val recipeIngredients = remember(listing) {
        listing.requiredMaterialsParsed.filterKeys { !it.contains("et", ignoreCase = true) }
    }

    val totalStockKg = state.stockSelections.values.sum()
    val totalInputKg = state.selectedPartKg + totalStockKg
    val expectedPackages = if (totalInputKg > 0) kotlin.math.max(1, kotlin.math.floor(totalInputKg / 1.0).toInt()) else 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "Fason Üretim Siparişi Ver",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${listing.productName} · Üretici: ${listing.producerName ?: "Sucukçu"}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat")
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                Text("Vazgeç")
                            }
                            Button(
                                onClick = {
                                    viewModel.submitOrder(onSuccess = onDismiss)
                                },
                                enabled = !state.isOrdering && state.selectedPart != null && state.selectedPartKg > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.weight(2f)
                            ) {
                                if (state.isOrdering) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("İletiliyor...")
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Siparişi Gönder", fontWeight = FontWeight.Bold)
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // İlan Bilgi Kartı
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Fason İşçilik Ücreti", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        String.format(Locale.US, "%.2f € / kg", listing.servicePrice),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PrimaryBlue
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Min. Sipariş", fontSize = 11.sp, color = Color.Gray)
                                    Text("${listing.minOrderQty ?: 1.0} kg", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // 1. Kendi Depondan Et Parçası Seç
                    item {
                        Text(
                            "1. Kendi Deponuzdaki Et Parçasını Seçiniz",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Parçalama işleminizden oluşan veya depoda bulunan aktif et lotları:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    if (state.allAvailableMeats.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "⚠️ Deponuzda hazır et bulunamadı!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        "Lütfen önce 'Parçalama' sekmesinden bir kuzu/dana parçalama işlemi gerçekleştirin veya depoya hammadde ekleyin.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.allAvailableMeats) { part ->
                            val isSelected = state.selectedPart?.lotId == part.lotId
                            val suitability = viewModel.checkPartSuitability(listing.productName, part.partName)
                            val suitBg = when (suitability.status) {
                                "recommended" -> Color(0xFFDCFCE7)
                                "warning" -> Color(0xFFFEF3C7)
                                "unsuitable" -> Color(0xFFFEE2E2)
                                else -> Color(0xFFF1F5F9)
                            }
                            val suitText = when (suitability.status) {
                                "recommended" -> Color(0xFF15803D)
                                "warning" -> Color(0xFFB45309)
                                "unsuitable" -> Color(0xFFB91C1C)
                                else -> Color.DarkGray
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ForestGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) ForestGreen else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.selectPartForOrder(part, part.availableKg)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(part.partName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(suitBg)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(suitability.badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = suitText)
                                                }
                                            }
                                            Text("Lot No: ${part.lotNumber} · Mevcut: ${String.format(Locale.US, "%.2f", part.availableKg)} kg", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectPartForOrder(part, part.availableKg) }
                                        )
                                    }

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = if (state.selectedPartKg > 0) state.selectedPartKg.toString() else "",
                                                onValueChange = {
                                                    val v = it.toDoubleOrNull() ?: 0.0
                                                    viewModel.selectPartForOrder(part, v)
                                                },
                                                label = { Text("Üretime Verilecek Miktar (kg)", fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                suffix = { Text("kg", fontSize = 11.sp) }
                                            )
                                            OutlinedButton(
                                                onClick = { viewModel.selectPartForOrder(part, part.availableKg) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Tümü", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Diğer Reçete Hammaddeleri (Reçeteye Göre Otomatik Doldurma)
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "2. Reçete Hammaddeleri",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Üreticinin reçetesine göre otomatik hesaplanan girdi miktarları:",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            if (state.selectedPartKg > 0 && recipeIngredients.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.autoFillMaterialsFromRecipe() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reçeteyle Doldur", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (recipeIngredients.isNotEmpty()) {
                        items(recipeIngredients.keys.toList()) { matName ->
                            val spec = recipeIngredients[matName]
                            val currentQty = state.stockSelections[matName] ?: 0.0
                            val stockItem = state.rawMaterialsStock.find {
                                it.materialType.contains(matName, ignoreCase = true) && it.lotStatus == "active"
                            }
                            val availStockKg = stockItem?.quantity ?: 0.0
                            val hasEnough = availStockKg >= currentQty

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (currentQty > 0 && !hasEnough) Color(0xFFFDBA74) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(matName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            "Reçete: ${spec?.amount ?: 1.0} ${spec?.unit ?: "kg"}/birim",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                        if (stockItem != null) {
                                            Text(
                                                "Deponuzda: ${String.format(Locale.US, "%.2f", availStockKg)} ${stockItem.unit} (${stockItem.lotNumber})",
                                                fontSize = 10.sp,
                                                color = if (hasEnough) ForestGreen else Color(0xFFC2410C),
                                                fontWeight = FontWeight.Medium
                                            )
                                        } else {
                                            Text(
                                                "Depoda lot bulunamadı (Üreticiye iletilecek)",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = if (currentQty > 0) currentQty.toString() else "",
                                        onValueChange = {
                                            val v = it.toDoubleOrNull() ?: 0.0
                                            viewModel.updateStockSelection(matName, v)
                                        },
                                        placeholder = { Text("0.0", fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(110.dp),
                                        singleLine = true,
                                        suffix = { Text(spec?.unit ?: "kg", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Reçete girilmemişse genel aktif depo stoklarını göster
                        items(state.rawMaterialsStock.filter { it.lotStatus == "active" && it.lotId != state.selectedPart?.lotId }) { mat ->
                            val currentQty = state.stockSelections[mat.materialType] ?: 0.0
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(mat.materialType, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Depo: ${mat.quantity} ${mat.unit} (${mat.lotNumber})", fontSize = 10.sp, color = Color.Gray)
                                    }

                                    OutlinedTextField(
                                        value = if (currentQty > 0) currentQty.toString() else "",
                                        onValueChange = {
                                            val v = it.toDoubleOrNull() ?: 0.0
                                            viewModel.updateStockSelection(mat.materialType, v)
                                        },
                                        placeholder = { Text("0.0", fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(110.dp),
                                        singleLine = true,
                                        suffix = { Text(mat.unit, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Sipariş Özeti Kartı
                    if (state.selectedPartKg > 0) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📋 Sipariş Girdi & Çıktı Özeti", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Seçilen Et:", fontSize = 11.sp, color = Color.Gray)
                                        Text("${state.selectedPart?.partName}: ${String.format(Locale.US, "%.2f kg", state.selectedPartKg)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Diğer Hammaddeler:", fontSize = 11.sp, color = Color.Gray)
                                        Text(String.format(Locale.US, "%.2f kg", totalStockKg), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Toplam Hammadde Girdisi:", fontSize = 11.sp, color = Color.Gray)
                                        Text(String.format(Locale.US, "%.2f kg", totalInputKg), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Beklenen Mamul Çıktısı:", fontSize = 11.sp, color = Color.Gray)
                                        Text("$expectedPackages paket (1 kg/paket)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreen)
                                    }
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
