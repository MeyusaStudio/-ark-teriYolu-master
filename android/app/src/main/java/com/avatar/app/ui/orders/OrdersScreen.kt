package com.avatar.app.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avatar.app.data.model.Order
import com.avatar.app.data.model.OrderRawMaterial
import com.avatar.app.data.model.User
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    user: User,
    viewModel: OrdersViewModel = viewModel(),
    onNavigateToProduction: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

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
                            if (user.role == "sucukcu") "📋 Gelen Fason Siparişler" else "🚚 Fason Siparişlerim",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            if (user.role == "sucukcu") "Hayvancılardan gelen üretim talepleri" else "Sucukçulara iletilen et işleme siparişleri",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadOrders() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryBlue)
            } else if (state.orders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Henüz sipariş bulunmuyor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (user.role == "sucukcu") "Hayvancılar ilanınız üzerinden fason sipariş verdiğinde burada listelenecektir."
                        else "Fason İlanları sekmesinden sucukçu ilanına sipariş verebilirsiniz.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.orders) { order ->
                        OrderCard(
                            order = order,
                            isSucukcu = user.role == "sucukcu",
                            onClick = { viewModel.loadOrderDetail(order) }
                        )
                    }
                }
            }
        }
    }

    state.selectedOrderDetail?.let { order ->
        OrderDetailDialog(
            order = order,
            rawMaterials = state.selectedOrderRawMaterials,
            isLoading = state.isLoadingDetail,
            isUpdating = state.isUpdatingStatus,
            isSucukcu = user.role == "sucukcu",
            onApprove = {
                viewModel.approveOrder(order.id) {
                    onNavigateToProduction?.invoke()
                }
            },
            onDismiss = { viewModel.clearDetail() }
        )
    }
}

@Composable
fun OrderCard(
    order: Order,
    isSucukcu: Boolean,
    onClick: () -> Unit
) {
    val (statusBg, statusText, statusLabel) = when (order.status) {
        "beklemede" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Beklemede")
        "onaylandi" -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), "Onaylandı")
        "uretimde" -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), "Üretimde")
        "tamamlandi" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Tamamlandı")
        "iptal" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "İptal")
        else -> Triple(Color(0xFFF1F5F9), Color.DarkGray, order.status)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sipariş #${order.id}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(order.orderDate, fontSize = 11.sp, color = Color.Gray)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusText)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(if (isSucukcu) "Siparişi Veren (Hayvancı)" else "Üretici (Sucukçu)", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        (if (isSucukcu) order.buyerName else order.sellerName) ?: "İşletme",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Ürün ve Miktar", fontSize = 10.sp, color = Color.Gray)
                    val displayQty = if (order.status == "tamamlandi" && order.actualQty != null) {
                        if (Math.round(order.actualQty) != Math.round(order.expectedQty)) {
                            "${order.productName} (${Math.round(order.actualQty)} paket / Sipariş: ${Math.round(order.expectedQty)})"
                        } else {
                            "${order.productName} (${Math.round(order.actualQty)} paket)"
                        }
                    } else {
                        "${order.productName} (${Math.round(order.expectedQty)} paket)"
                    }
                    Text(
                        displayQty,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (order.status == "tamamlandi") ForestGreen else PrimaryBlue
                    )
                }
            }

            if (!order.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Not: ${order.notes}", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailDialog(
    order: Order,
    rawMaterials: List<OrderRawMaterial>,
    isLoading: Boolean,
    isUpdating: Boolean,
    isSucukcu: Boolean,
    onApprove: () -> Unit,
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
                                "Sipariş Detayı #${order.id}",
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
                },
                bottomBar = {
                    if (isSucukcu && order.status == "beklemede") {
                        Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                    Text("Kapat")
                                }
                                Button(
                                    onClick = onApprove,
                                    enabled = !isUpdating,
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    modifier = Modifier.weight(2f)
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Onaylanıyor...")
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Siparişi Onayla & Üretime Al", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Ürün: ${order.productName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (order.status == "tamamlandi" && order.actualQty != null) {
                                    Text(
                                        "Teslim Edilen / Üretilen: ${Math.round(order.actualQty)} paket",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreen
                                    )
                                    if (Math.round(order.actualQty) != Math.round(order.expectedQty)) {
                                        Text(
                                            "Siparişte İstenen: ${Math.round(order.expectedQty)} paket",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                } else {
                                    Text("Beklenen Paket: ${Math.round(order.expectedQty)} paket", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("Durum: ${order.status}", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    item {
                        Text(
                            "Hayvancının Gönderdiği Et ve Hammadde Lotları (${rawMaterials.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (rawMaterials.isEmpty() && !isLoading) {
                        item {
                            Text("Bu siparişe bağlı hammadde kaydı bulunamadı.", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        items(rawMaterials) { rm ->
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
                                        Text(rm.materialType, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Lot No: ${rm.lotNumber ?: "Belirtilmedi"}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text(
                                        "${rm.quantity} ${rm.unit}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ForestGreen
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
