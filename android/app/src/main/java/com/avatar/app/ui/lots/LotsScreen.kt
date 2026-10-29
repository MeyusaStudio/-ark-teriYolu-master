package com.avatar.app.ui.lots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avatar.app.data.model.LotItem
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotsScreen(
    viewModel: LotsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadLots()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "🏷️ Lot Yönetimi & Stok",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Et parçaları, üretilen mamuller ve hammadde lotları",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadLots() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Arama Çubuğu
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Lot no veya ürün adı ile ara...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            // Filtre Çipleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filterType == "all",
                    onClick = { viewModel.setFilterType("all") },
                    label = { Text("Tümü (${state.lots.size})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = state.filterType == "et",
                    onClick = { viewModel.setFilterType("et") },
                    label = { Text("🥩 Et Parçaları (${state.etCount})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = state.filterType == "mamul",
                    onClick = { viewModel.setFilterType("mamul") },
                    label = { Text("🌭 Üretilen Mamuller (${state.mamulCount})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = state.filterType == "hammadde",
                    onClick = { viewModel.setFilterType("hammadde") },
                    label = { Text("🧂 Hammaddeler (${state.hammaddeCount})", fontSize = 12.sp) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryBlue
                    )
                } else if (state.filteredLots.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🏷️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (state.searchQuery.isNotBlank()) "Arama kriterine uygun lot bulunamadı" else "Henüz kayıtlı lot bulunmuyor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Parçalama yaptığınızda et lotları, sucukçu fason üretimi tamamladığında mamul lotları (SCK) burada listelenecektir.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.filteredLots, key = { it.id }) { lot ->
                            LotCard(lot = lot)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LotCard(lot: LotItem) {
    val isMeat = LotsUiState.isMeatLot(lot)
    val isMamul = LotsUiState.isMamulLot(lot)

    val (typeBg, typeText, typeLabel) = when {
        isMamul -> Triple(Color(0xFFFAF5FF), Color(0xFF7E22CE), "🌭 Üretilen Mamul")
        isMeat -> Triple(Color(0xFFF0FDF4), ForestGreen, "🥩 Mezbaha Eti")
        else -> Triple(Color(0xFFEFF6FF), PrimaryBlue, "🧂 Hammadde")
    }

    val (statusBg, statusText, statusLabel) = when (lot.status.lowercase()) {
        "active" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Aktif")
        "consumed" -> Triple(Color(0xFFF1F5F9), Color.DarkGray, "Tüketildi")
        "recalled" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Geri Çağrıldı")
        "blocked" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Bloke")
        else -> Triple(Color(0xFFF1F5F9), Color.DarkGray, lot.status)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Başlık: Lot No ve Rozetler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        lot.lot_number,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isMamul) Color(0xFF7E22CE) else if (isMeat) ForestGreen else PrimaryBlue
                    )
                    Text(
                        lot.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(typeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = typeText)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Metrikler: Miktar, Birim Maliyet (EKP), Toplam Maliyet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Mevcut Miktar", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        "${String.format(Locale.US, "%.2f", lot.quantity)} ${lot.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (lot.quantity > 0) ForestGreen else Color.Gray
                    )
                }

                if (lot.unit_cost != null && lot.unit_cost > 0) {
                    Column {
                        Text(if (isMamul) "Birim EKP" else "Birim Maliyet", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            String.format(Locale.US, "%.4f € / %s", lot.unit_cost, lot.unit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }

                if (lot.total_cost != null && lot.total_cost > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Toplam Değer", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            String.format(Locale.US, "%.2f €", lot.total_cost),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            // Bilgi Açıklaması
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when {
                            isMamul -> "🏭 Sucukçuda fason üretimle üretildi ve hayvancı mamul stoğuna kaydedildi."
                            isMeat -> "🔪 Mezbaha parçalama işleminden elde edilen aktif et parçası."
                            else -> "🧂 Depoda kayıtlı yardımcı hammadde stoğu."
                        },
                        fontSize = 10.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
    }
}
