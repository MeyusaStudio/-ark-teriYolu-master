package com.avatar.app.ui.recalls

import androidx.compose.foundation.background
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
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecallsScreen(
    viewModel: RecallsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var showNewRecallDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showNewRecallDialog) {
        NewRecallDialog(
            availableLots = state.availableLots,
            isSubmitting = state.isSubmitting,
            onDismiss = { showNewRecallDialog = false },
            onSubmit = { lotId, reason ->
                viewModel.initiateRecall(lotId, reason) {
                    showNewRecallDialog = false
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("⛔ Parti Geri Çağırma (Recall)", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFFDC2626))
                        Text("Mikrobiyolojik ve kalite blokajları yönetimi", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewRecallDialog = true },
                containerColor = Color(0xFFDC2626),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                text = { Text("Geri Çağırma Başlat", fontWeight = FontWeight.Bold) }
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
            // İstatistik Kartları (Aktif Geri Çağırma, Tamamlanan, Etkilenen Marketler)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.activeRecallsCount > 0) Color(0xFFFEF2F2) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Aktif Blokaj", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "${state.activeRecallsCount}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.activeRecallsCount > 0) Color(0xFFDC2626) else Color.DarkGray
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Tamamlanan", fontSize = 11.sp, color = Color.Gray)
                        Text("${state.completedRecallsCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                    }
                }

                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Etkilenen Market", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "${state.totalAffectedMarkets} Market",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            // Yenileme ve Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Parti Geri Çağırma (Recall) Geçmişi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(onClick = { viewModel.loadData() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                }
            }

            // Geri Çağırma Listesi
            if (state.isLoading && state.recalls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFDC2626))
                }
            } else if (state.recalls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛡️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Aktif geri çağırma kaydı bulunmuyor", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Kalite standart dışı ürünler için buradan blokaj ve acil bildirim başlatabilirsiniz.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.recalls) { recall ->
                        val isAktif = recall.status == "aktif"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAktif) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isAktif) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                if (isAktif) "● AKTİF GERİ ÇAĞIRMA" else "✓ TAMAMLANDI",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAktif) Color(0xFFDC2626) else ForestGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFF3E8FF)
                                        ) {
                                            Text(
                                                "Lot: ${recall.lotNumber ?: "LOT-${recall.lotId}"}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7E22CE),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        recall.createdAt?.take(10) ?: "",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Gerekçe: ${recall.reason}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (isAktif) Color(0xFF991B1B) else Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Bildirilen: ${recall.affectedCount ?: 0} Market • Başlatan: ${recall.initiatedByName ?: "Kullanıcı"}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )

                                    if (isAktif) {
                                        Button(
                                            onClick = { viewModel.completeRecall(recall.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tamamla", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
