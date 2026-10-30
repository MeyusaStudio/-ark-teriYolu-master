package com.avatar.app.ui.listings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avatar.app.data.model.LotItem
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFarmerListingDialog(
    availableLots: List<LotItem>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (lotId: Int, productName: String, quantity: Double, unit: String, unitPrice: Double, currency: String, description: String?) -> Unit
) {
    var selectedLot by remember { mutableStateOf(availableLots.firstOrNull()) }
    var productName by remember { mutableStateOf(selectedLot?.displayName ?: "Fermente Kangal Sucuk") }
    var quantityText by remember { mutableStateOf(selectedLot?.quantity?.toInt()?.toString() ?: "10") }
    val unit = "paket"
    var unitPriceText by remember { mutableStateOf(if ((selectedLot?.unit_cost ?: 0.0) > 0) String.format(Locale.US, "%.2f", (selectedLot?.unit_cost ?: 0.0) * 1.3) else "12.50") }
    var currency by remember { mutableStateOf(selectedLot?.currency ?: "TL") }
    var description by remember { mutableStateOf("Geleneksel yöntemlerle fermente edilmiş, katkısız kaliteli mamul sucuk.") }
    val currencyOptions = listOf("TL", "EUR", "USD", "GBP")

    LaunchedEffect(selectedLot) {
        selectedLot?.let { lot ->
            productName = lot.displayName
            quantityText = lot.quantity.toInt().toString()
            if (!lot.currency.isNullOrBlank()) {
                currency = lot.currency
            }
            val cost = lot.unit_cost ?: 0.0
            if (cost > 0) {
                unitPriceText = String.format(Locale.US, "%.2f", cost * 1.3)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("🌭 Mamul İlanı Oluştur", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Sucuk, Salam, Sosis, Pastırma (Çiğ et ilana koyulamaz)", fontSize = 11.sp, color = Color.Gray)
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
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Vazgeç")
                            }
                            Button(
                                onClick = {
                                    val lotId = selectedLot?.id ?: return@Button
                                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                                    val price = unitPriceText.toDoubleOrNull() ?: 0.0
                                    onSubmit(lotId, productName, qty, unit, price, currency, description)
                                },
                                enabled = !isSubmitting && selectedLot != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0 && (unitPriceText.toDoubleOrNull() ?: 0.0) > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.weight(2f)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Yayınlanıyor...")
                                } else {
                                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("İlanı Yayınla", fontWeight = FontWeight.Bold)
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (availableLots.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "⚠️ Satış sadece Sucuk, Salam, Sosis, Pastırma gibi fason mamul ürünler için geçerlidir. Çiğ et parçaları ilana koyulamaz.\n\nDeponuzda henüz fason üretimi tamamlanmış mamul partisi bulunmuyor. Sucukçudan gelen üretimler tamamlandığında mamul lotlarınız burada listelenecektir.",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    } else {
                        item {
                            Text("1. İlan Verilecek Mamul Lotunu Seçin (Sucuk, Salam, Sosis, Pastırma)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        items(availableLots) { lot ->
                            val isSelected = selectedLot?.id == lot.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLot = lot },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) PrimaryBlue else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedLot = lot }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(lot.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "Lot: ${lot.lot_number} • Stok: ${lot.quantity.toInt()} paket",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        val cost = lot.unit_cost ?: 0.0
                                        if (cost > 0) {
                                            Text(
                                                "Maliyet (EKP): ${String.format(Locale.US, "%.2f", cost)} ${lot.currency ?: "TL"}/paket",
                                                fontSize = 10.sp,
                                                color = PrimaryBlue,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("2. İlan Detayları", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        item {
                            OutlinedTextField(
                                value = productName,
                                onValueChange = { productName = it },
                                label = { Text("Ürün Adı") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                label = { Text("Satış Miktarı (paket)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        item {
                            Text("Para Birimi Seçin:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currencyOptions.forEach { curr ->
                                    FilterChip(
                                        selected = currency == curr,
                                        onClick = { currency = curr },
                                        label = { Text(curr, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = unitPriceText,
                                onValueChange = { unitPriceText = it },
                                label = { Text("Birim Satış Fiyatı ($currency / paket)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        item {
                            val qty = quantityText.toDoubleOrNull() ?: 0.0
                            val price = unitPriceText.toDoubleOrNull() ?: 0.0
                            val total = qty * price
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Toplam İlan Değeri:", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "${String.format(Locale.US, "%.2f", total)} $currency",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreen
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Açıklama & Market Notu") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                    }
                }
            }
        }
    }
}
