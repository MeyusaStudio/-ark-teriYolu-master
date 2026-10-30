package com.avatar.app.ui.lots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.avatar.app.data.model.AnimalLotGroup
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Hayvan Bazında Gruplandırılmış Kesim & Parça Lotları
                        if (state.animalGroups.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🥩 Hayvan Kesim ve Parçaları",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ForestGreen
                                    )
                                    Text(
                                        "${state.animalGroups.size} Hayvan",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            items(state.animalGroups, key = { it.earTag }) { group ->
                                AnimalGroupCard(group = group)
                            }
                        }

                        // 2. Diğer Lotlar (Mamul & Hammadde)
                        if (state.otherLots.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "📦 Diğer Lotlar (Mamul & Hammadde)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF334155)
                                    )
                                    Text(
                                        "${state.otherLots.size} Lot",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            items(state.otherLots, key = { it.id }) { lot ->
                                LotCard(lot = lot)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getAnimalEmoji(animalType: String?): String {
    val norm = (animalType ?: "").lowercase()
    return when {
        norm.contains("inek") || norm.contains("dana") || norm.contains("sığır") -> "🐄"
        norm.contains("koyun") || norm.contains("kuzu") || norm.contains("keçi") -> "🐑"
        norm.contains("tavuk") || norm.contains("piliç") -> "🐔"
        else -> "🥩"
    }
}

@Composable
fun AnimalGroupCard(group: AnimalLotGroup) {
    var expanded by remember { mutableStateOf(true) }
    val emoji = getAnimalEmoji(group.animalType)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Başlık: Hayvan Küpe Numarası ve Özet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }

                    Column {
                        Text(
                            group.earTag,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ForestGreen
                        )
                        Text(
                            "${group.animalType ?: "Hayvan"} · Kesim: ${group.butcheryDate?.take(10) ?: "-"}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${String.format(Locale.US, "%.2f", group.totalKg)} kg",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ForestGreen
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${group.parts.size} Parça",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                        }
                    }

                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Kapat" else "Aç",
                        tint = Color.Gray
                    )
                }
            }

            // Açılır Parça Detayları
            if (expanded) {
                HorizontalDivider(color = Color(0xFFF1F5F9))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    group.parts.forEach { part ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        part.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        part.lot_number,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PrimaryBlue
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${String.format(Locale.US, "%.2f", part.quantity)} ${part.unit}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = ForestGreen
                                    )
                                    if (part.unit_cost != null && part.unit_cost > 0) {
                                        Text(
                                            "${String.format(Locale.US, "%.2f", part.unit_cost)} ${part.currency}/kg",
                                            fontSize = 11.sp,
                                            color = Color(0xFFB45309)
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
