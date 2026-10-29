package com.avatar.app.ui.butchery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
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
import com.avatar.app.data.model.ButcheryRecord
import com.avatar.app.ui.theme.ForestGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButcheryScreen(
    viewModel: ButcheryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showNewDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "🥩 Parçalama & Kütle Dengesi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Görev 2: Gerçek Maliyet Dağıtımı",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadOperations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewDialog = true },
                containerColor = ForestGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Yeni Parçalama", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ForestGreen
                )
            } else if (state.operations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🐑", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Henüz parçalama kaydı bulunmuyor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sağ alttaki 'Yeni Parçalama' butonuna tıklayarak ilk hayvan parçalama ve lot oluşturma işlemini başlatabilirsiniz.",
                        fontSize = 13.sp,
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
                    items(state.operations) { op ->
                        ButcheryOperationCard(
                            record = op,
                            onViewDetail = { viewModel.loadOperationDetail(op) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        NewButcheryDialog(
            viewModel = viewModel,
            onDismiss = { showNewDialog = false }
        )
    }

    state.selectedDetailRecord?.let { op ->
        ButcheryDetailDialog(
            record = op,
            parts = state.selectedDetailParts,
            isLoading = state.isLoadingDetail,
            onDismiss = { viewModel.clearDetail() }
        )
    }
}

@Composable
fun ButcheryOperationCard(
    record: ButcheryRecord,
    onViewDetail: () -> Unit
) {
    val animalEmoji = when (record.animalType) {
        "koyun" -> "🐑"
        "inek" -> "🐄"
        "tavuk" -> "🐔"
        else -> "🥩"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(animalEmoji, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "${record.animalEarTag ?: "Küpe Belirtilmedi"} (#${record.id})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(record.operationDate, fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (record.distributionMethod == "deger") Color(0xFFF3E8FF) else Color(0xFFE0F2FE)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (record.distributionMethod == "deger") "💎 Değer Dağıtımı" else "⚖️ Ağırlık Dağıtımı",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (record.distributionMethod == "deger") Color(0xFF7E22CE) else Color(0xFF0369A1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Giriş", fontSize = 10.sp, color = Color.Gray)
                    Text("${record.inputWeightKg} kg", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Çıkan Toplam", fontSize = 10.sp, color = Color.Gray)
                    Text("${record.totalOutputKg} kg", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Fire", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        "${record.wasteKg} kg",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (record.wasteKg > 0) Color(0xFFDC2626) else ForestGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Toplam Maliyet", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        String.format(Locale.US, "%.2f €", record.totalCost),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ForestGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (record.parts != null && record.parts.isNotEmpty()) "${record.parts.size} parça oluşturuldu"
                    else "Oluşan parçalar ve lotlar",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                TextButton(
                    onClick = onViewDetail,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Detay & Lotlar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
