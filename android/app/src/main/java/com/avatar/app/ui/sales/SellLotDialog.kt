package com.avatar.app.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avatar.app.data.model.LotItem
import com.avatar.app.ui.theme.*

@Composable
fun SellLotDialog(
    lot: LotItem,
    onDismiss: () -> Unit,
    onConfirmSell: (customerName: String, quantity: Double, unitPrice: Double) -> Unit
) {
    var customerName by remember { mutableStateOf("Süpermarket A") }
    var quantityText by remember { mutableStateOf("1.0") }
    var unitPriceText by remember { mutableStateOf(lot.unit_cost?.let { "%.2f".format(it * 1.3) } ?: "25.0") }

    val quantity = quantityText.toDoubleOrNull() ?: 0.0
    val unitPrice = unitPriceText.toDoubleOrNull() ?: 0.0
    val totalAmount = quantity * unitPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (lot.isBlocked) Icons.Default.Block else Icons.Default.PointOfSale,
                    contentDescription = null,
                    tint = if (lot.isBlocked) ErrorRed else PrimaryBlue
                )
                Text(
                    text = if (lot.isBlocked) "Satış Engeli: Bloke Lot" else "Lot Satışı Yap",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Ürün ve Lot Bilgisi
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lot.isBlocked) Color(0xFFFEE2E2) else Color(0xFFF1F5F9)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = lot.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (lot.isBlocked) Color(0xFF991B1B) else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lot: ${lot.lot_number}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Kalan: ${"%.2f".format(lot.quantity)} ${lot.unit}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lot.isBlocked) ErrorRed else PrimaryBlue
                            )
                        }
                    }
                }

                // ── 3. GÖREV ŞARTNAME KURALI: BLOKE LOT UYARISI ──
                if (lot.isBlocked) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                            Text(
                                text = "⚠️ ŞARTNAME KURALI: Bu lot kalite problemi veya geri çağırma nedeniyle bloke edilmiştir. Kotlin satış uygulamasından satışı KESİNLİKLE ENGELLENMİŞTİR.",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    // Müşteri Adı
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Müşteri / Alıcı Market Adı *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Satış Miktarı
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Satılacak Miktar (${lot.unit}) *") },
                        trailingIcon = { Text(lot.unit, modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Birim Fiyat
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = { Text("Birim Fiyat (${lot.currency} / ${lot.unit}) *") },
                        trailingIcon = { Text(lot.currency, modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Toplam Tutar Özeti
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Toplam Satış Tutarı:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "${"%.2f".format(totalAmount)} ${lot.currency}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!lot.isBlocked) {
                Button(
                    onClick = {
                        if (quantity > 0 && unitPrice > 0 && customerName.isNotBlank()) {
                            onConfirmSell(customerName, quantity, unitPrice)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Satışı Onayla")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lot.isBlocked) "Tamam, Kapat" else "Vazgeç")
            }
        }
    )
}
