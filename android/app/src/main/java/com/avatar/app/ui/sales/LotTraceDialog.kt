package com.avatar.app.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avatar.app.data.model.LotItem
import com.avatar.app.data.model.TraceBackwardResponse
import com.avatar.app.data.model.TraceForwardResponse
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotTraceDialog(
    lot: LotItem,
    forwardData: TraceForwardResponse?,
    backwardData: TraceBackwardResponse?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "🔍 Lot Soyağacı & İzlenebilirlik",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${lot.lot_number} · ${lot.displayName}",
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
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("⏩ İleri İzleme", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("⏪ Geri İzleme", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selectedTab == 0) {
                                // ── İLERİ İZLEME ──
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                "Bu lottan sonra nereye gidildi?",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = PrimaryBlue
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Bu et / hammadde lotunun kullanıldığı mamul (sucuk) partileri ve satıldığı marketler:",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        "1. Üretilen Mamul / Sucuk Partileri (${forwardData?.finishedLots?.size ?: 0})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (forwardData?.finishedLots.isNullOrEmpty()) {
                                    item {
                                        Text("Bu lottan henüz türetilen mamul bulunmuyor.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                } else {
                                    items(forwardData!!.finishedLots!!) { finishedLot ->
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
                                                    Text(finishedLot.lot_number, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(finishedLot.displayName, fontSize = 11.sp, color = Color.DarkGray)
                                                }
                                                Text(
                                                    "${finishedLot.quantity} ${finishedLot.unit}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = ForestGreen
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "2. Satış Yapılan Marketler / Noktalar (${forwardData?.marketSales?.size ?: 0})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (forwardData?.marketSales.isNullOrEmpty()) {
                                    item {
                                        Text("Bu lot henüz herhangi bir markete satılmamış.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                } else {
                                    items(forwardData!!.marketSales!!) { sale ->
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
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Store, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(sale.marketName ?: "Market", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(sale.marketAddress ?: "Adres belirtilmedi", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("${sale.quantity} ${sale.unit}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreen)
                                                    Text(sale.status ?: "satıldı", fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ── GERİ İZLEME ──
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                "Bu ürün nereden geldi?",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = ForestGreen
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Bu ürünün üretiminde kullanılan et, baharat, tuz, bağırsak lotları ve kaynak tedarikçiler:",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        "Kullanılan Hammadde & Et Lotları (${backwardData?.rawMaterialLots?.size ?: 0})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (backwardData?.rawMaterialLots.isNullOrEmpty()) {
                                    item {
                                        Text("Bu lot için alt hammadde kaydı bulunamadı (Kendisi birincil hammadde/parça olabilir).", fontSize = 12.sp, color = Color.Gray)
                                    }
                                } else {
                                    items(backwardData!!.rawMaterialLots!!) { rm ->
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
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFE0F2FE))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(rm.lotNumber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0369A1))
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(rm.materialType ?: "Hammadde", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("Tedarikçi: ${rm.supplierName ?: "Kendi Kesimimiz"}", fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("${rm.quantityUsed ?: 0.0} ${rm.unit ?: "kg"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    if (rm.unitPrice != null) {
                                                        Text("${rm.unitPrice} €/br", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
