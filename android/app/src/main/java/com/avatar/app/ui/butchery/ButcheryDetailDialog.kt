package com.avatar.app.ui.butchery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avatar.app.data.model.ButcheryPartDetail
import com.avatar.app.data.model.ButcheryRecord
import com.avatar.app.ui.theme.ForestGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButcheryDetailDialog(
    record: ButcheryRecord,
    parts: List<ButcheryPartDetail>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Parçalama Detayı #${record.id}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat")
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Tarih: ${record.operationDate}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        "Küpe: ${record.animalEarTag ?: "-"}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Giriş", fontSize = 11.sp, color = Color.Gray)
                                        Text("${record.inputWeightKg} kg", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Column {
                                        Text("Çıkan Toplam", fontSize = 11.sp, color = Color.Gray)
                                        Text("${record.totalOutputKg} kg", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Column {
                                        Text("Fire", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            "${record.wasteKg} kg",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                    Column {
                                        Text("Toplam Maliyet", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            String.format(Locale.US, "%.2f €", record.totalCost),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ForestGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Oluşan Parçalar ve Parti/Lot Numaraları",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                        }
                    }

                    if (parts.isEmpty() && !isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Bu işleme ait parça kaydı bulunamadı.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }

                    items(parts) { part ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(part.partName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE0F2FE))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                part.lotNumber ?: "Lot atanmadı",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0369A1)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val rem = part.remainingKg ?: part.actualWeightKg
                                        Text(
                                            if (part.remainingKg != null && part.remainingKg != part.actualWeightKg) {
                                                "${String.format(Locale.US, "%.2f", rem)} kg Kalan (${part.actualWeightKg} kg)"
                                            } else {
                                                "${part.actualWeightKg} kg"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = if (part.remainingKg != null) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (rem > 0) ForestGreen else Color.Gray
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        String.format(Locale.US, "%.2f €", part.allocatedCost),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ForestGreen
                                    )
                                    Text(
                                        String.format(Locale.US, "%.2f €/kg", part.unitCost),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
