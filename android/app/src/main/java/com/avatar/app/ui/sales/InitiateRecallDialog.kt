package com.avatar.app.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avatar.app.data.model.LotItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitiateRecallDialog(
    lot: LotItem,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("Mikrobiyolojik kontaminasyon riski ve kalite standart dışılığı") }

    val presetReasons = listOf(
        "Mikrobiyolojik uygunsuzluk",
        "Soğuk zincir kırılması",
        "Yabancı madde şüphesi",
        "Etiketleme / Parti hatası"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "⛔ Geri Çağırma (Blokaj)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFDC2626)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat")
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                Text("Vazgeç")
                            }
                            Button(
                                onClick = { onConfirm(reason) },
                                enabled = !isSubmitting && reason.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier.weight(2f)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bloke Ediliyor...")
                                } else {
                                    Text("Geri Çağır & Bloke Et", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Şartname 3. Görev Uyarısı
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Yarışma Görevi 3 Şartname Kuralı:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Bu işlem başlatıldığında '${lot.lot_number}' ve bu lottan üretilen tüm alt mamuller anında 'recalled' durumuna geçirilecek ve satışları SİSTEM TARAFINDAN KİLİTLENECEKTİR.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    Text(
                        "Lot Bilgisi:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        "${lot.displayName} (${lot.quantity} ${lot.unit}) · Lot No: ${lot.lot_number}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Text(
                        "Geri Çağırma Gerekçesi:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetReasons.forEach { r ->
                            FilterChip(
                                selected = reason == r,
                                onClick = { reason = r },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Açıklama / Gerekçe", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                }
            }
        }
    }
}
