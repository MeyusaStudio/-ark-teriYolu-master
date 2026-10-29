package com.avatar.app.ui.market

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
import com.avatar.app.data.model.MarketPurchaseItem
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketPurchasesScreen(
    viewModel: MarketViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var selectedPurchaseForFeedback by remember { mutableStateOf<MarketPurchaseItem?>(null) }
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

    selectedPurchaseForFeedback?.let { purchase ->
        ReportIssueDialog(
            purchase = purchase,
            isSubmitting = state.isSubmittingFeedback,
            onDismiss = { selectedPurchaseForFeedback = null },
            onSubmit = { reason ->
                viewModel.submitFeedback(
                    lotNumber = purchase.lotNumber,
                    issueDescription = reason,
                    farmerId = purchase.farmerId,
                    lotId = purchase.lotId
                ) {
                    selectedPurchaseForFeedback = null
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
                        Text("🛍️ Satın Aldıklarım", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Marketinizin mamul alımları ve faturaları", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPurchases() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                }
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
            // Özet Kartları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Toplam Alım Tutarı", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "${String.format(Locale.US, "%.2f", state.totalSpent)} EUR",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Satın Alınan Miktar", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "${String.format(Locale.US, "%.0f", state.totalBoughtQty)} adet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            // Alımlar Listesi
            if (state.purchases.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Henüz bir mamul alımı gerçekleştirmediniz", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Market İlanları sekmesinden üreticilerin ürünlerini inceleyebilirsiniz.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.purchases) { purchase ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            purchase.productName ?: "Mamul Ürün",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF3E8FF)) {
                                                Text(
                                                    "Lot: ${purchase.lotNumber}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF7E22CE),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                                                Text(
                                                    "✓ Fatura Kesildi / Teslim Alındı",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForestGreen,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${String.format(Locale.US, "%.2f", purchase.totalPrice)} ${purchase.currency ?: "EUR"}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = ForestGreen
                                        )
                                        Text(
                                            "${purchase.quantity} ${purchase.unit} × ${String.format(Locale.US, "%.2f", purchase.unitPrice)}",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Üretici: ${purchase.farmerName ?: "Hayvancı"}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        if (!purchase.farmerPhone.isNullOrBlank()) {
                                            Text("Tel: ${purchase.farmerPhone}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    Text(
                                        purchase.purchaseDate.take(10),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { selectedPurchaseForFeedback = purchase },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFDC2626))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sorun / Şikayet Bildir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
