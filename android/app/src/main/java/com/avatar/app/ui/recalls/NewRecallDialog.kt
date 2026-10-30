package com.avatar.app.ui.recalls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.avatar.app.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRecallDialog(
    availableLots: List<LotItem>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (lotId: Int, reason: String) -> Unit
) {
    var selectedLot by remember { mutableStateOf(availableLots.firstOrNull()) }
    var reason by remember { mutableStateOf("Mikrobiyolojik kontaminasyon riski ve kalite standart dışılığı şüphesi") }

    val presetReasons = listOf(
        "Mikrobiyolojik uygunsuzluk tespiti",
        "Soğuk zincir kırılması / bozulma",
        "Yabancı madde şüphesi",
        "Etiketleme / Parti karışıklığı"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("⛔ Yeni Geri Çağırma Başlat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFDC2626))
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
                                    val lotId = selectedLot?.id ?: return@Button
                                    onSubmit(lotId, reason)
                                },
                                enabled = !isSubmitting && selectedLot != null && reason.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier.weight(2f)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("İşleniyor...")
                                } else {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Geri Çağır & Bloke Et", fontWeight = FontWeight.Bold)
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
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "⚠️ DİKKAT: Geri çağırma başlatıldığında seçilen lot, bundan üretilen mamuller ve ilgili satış ilanları sistem tarafından DERHAL BLOKE EDİLİR. Bu lotu satın alan marketlere sistemden acil geri çağırma bildirimi iletilir.",
                                color = Color(0xFF991B1B),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    item {
                        Text("1. Geri Çağrılacak Lotu Seçin", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (availableLots.isEmpty()) {
                        item {
                            Text("Geri çağrılabilecek aktif lot bulunmuyor.", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        items(availableLots) { lot ->
                            val isSelected = selectedLot?.id == lot.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLot = lot },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Color(0xFFDC2626) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedLot = lot },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFDC2626))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(lot.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "Lot: ${lot.lot_number} • ${lot.quantity} paket • Tür: Mamül",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("2. Gerekçe Seçin veya Yazın", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            presetReasons.forEach { preset ->
                                OutlinedButton(
                                    onClick = { reason = preset },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (reason == preset) Color(0xFFFEF2F2) else Color.Transparent
                                    )
                                ) {
                                    Text(preset, fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Detaylı Gerekçe") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
            }
        }
    }
}
