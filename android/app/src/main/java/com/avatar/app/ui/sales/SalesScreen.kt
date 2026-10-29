package com.avatar.app.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avatar.app.data.model.LotItem
import com.avatar.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: SalesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedLotForSale by remember { mutableStateOf<LotItem?>(null) }
    var selectedLotForRecall by remember { mutableStateOf<LotItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Satış Yap Modal
    selectedLotForSale?.let { lot ->
        SellLotDialog(
            lot = lot,
            onDismiss = { selectedLotForSale = null },
            onConfirmSell = { customerName, quantity, unitPrice ->
                viewModel.sellLot(lot, customerName, quantity, unitPrice, lot.currency)
                selectedLotForSale = null
            }
        )
    }

    // Soyağacı & İzleme Modal (Görev 3)
    uiState.selectedTraceLot?.let { lot ->
        LotTraceDialog(
            lot = lot,
            forwardData = uiState.traceForwardData,
            backwardData = uiState.traceBackwardData,
            isLoading = uiState.isLoadingTrace,
            onDismiss = { viewModel.clearTrace() }
        )
    }

    // Geri Çağırma Modal (Görev 3)
    selectedLotForRecall?.let { lot ->
        InitiateRecallDialog(
            lot = lot,
            isSubmitting = uiState.isInitiatingRecall,
            onDismiss = { selectedLotForRecall = null },
            onConfirm = { reason ->
                viewModel.initiateRecall(lot.id, reason) {
                    selectedLotForRecall = null
                }
            }
        )
    }

    val filteredLots = remember(uiState.lots, uiState.filterType) {
        when (uiState.filterType) {
            "ACTIVE" -> uiState.lots.filter { !it.isBlocked && it.quantity > 0 }
            "BLOCKED" -> uiState.lots.filter { it.isBlocked }
            else -> uiState.lots
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🏷️ Parti Satışı & İzlenebilirlik",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Görev 2 & 3: Satış, Soyağacı ve Geri Çağırma",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadLots() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Şartname Bilgilendirme Rozeti
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isBlockedAlert) Color(0xFFFFF1F2) else Color(0xFFEFF6FF)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.isBlockedAlert) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (uiState.isBlockedAlert) Color(0xFFE11D48) else PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "3. Görev: Parti/Lot İzlenebilirliği ve Satış Engeli",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (uiState.isBlockedAlert) Color(0xFFBE123C) else PrimaryBlue
                        )
                        Text(
                            text = "Geri çağrılan (recalled) veya bloke lotların satışı hem mobilde hem sunucuda kilitlenir.",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Filtre Çipleri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterType == "ALL",
                    onClick = { viewModel.setFilter("ALL") },
                    label = { Text("Tümü (${uiState.lots.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = uiState.filterType == "ACTIVE",
                    onClick = { viewModel.setFilter("ACTIVE") },
                    label = { Text("Satışa Hazır (${uiState.lots.count { !it.isBlocked && it.quantity > 0 }})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = uiState.filterType == "BLOCKED",
                    onClick = { viewModel.setFilter("BLOCKED") },
                    label = {
                        Text(
                            "⛔ Bloke / Geri Çağrılan (${uiState.lots.count { it.isBlocked }})",
                            fontSize = 11.sp,
                            color = if (uiState.filterType == "BLOCKED") Color.Red else Color.Unspecified
                        )
                    }
                )
            }

            // Lot Listesi
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (filteredLots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Filtreye uygun parti/lot bulunamadı.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLots) { lot ->
                        LotCard(
                            lot = lot,
                            onSellClick = { selectedLotForSale = lot },
                            onTraceClick = { viewModel.loadTrace(lot) },
                            onRecallClick = { selectedLotForRecall = lot }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LotCard(
    lot: LotItem,
    onSellClick: () -> Unit,
    onTraceClick: () -> Unit,
    onRecallClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (lot.isBlocked) Color(0xFFFFF1F2) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Başlık satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (lot.lot_type.lowercase()) {
                            "parca" -> "🥩"
                            "mamul" -> "🌭"
                            "hammadde" -> "📦"
                            else -> "🏷️"
                        },
                        fontSize = 20.sp
                    )
                    Text(
                        text = lot.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (lot.isBlocked) Color(0xFF9F1239) else TextPrimary
                    )
                }

                // Durum Rozeti
                Surface(
                    color = if (lot.isBlocked) ErrorRed else SecondaryGreen,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (lot.isBlocked) "⛔ BLOKE / GERİ ÇAĞRILDI" else "✓ Satışa Hazır",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Detay Satırları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Parti / Lot Numarası:", fontSize = 11.sp, color = TextSecondary)
                    Text(lot.lot_number, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Depo Stok Miktarı:", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = "${"%.2f".format(lot.quantity)} ${lot.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (lot.isBlocked) ErrorRed else PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Aksiyon Butonları (İzleme, Geri Çağır, Satış Yap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onTraceClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("İzleme", fontSize = 11.sp)
                    }

                    if (!lot.isBlocked) {
                        OutlinedButton(
                            onClick = onRecallClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Geri Çağır", fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = onSellClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (lot.isBlocked) Color(0xFFBE123C) else PrimaryBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (lot.isBlocked) Icons.Default.Block else Icons.Default.PointOfSale,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lot.isBlocked) "Satış Engeli" else "Satış Yap",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
