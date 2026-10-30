package com.avatar.app.ui.sales

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
import androidx.compose.ui.window.Dialog
import com.avatar.app.data.model.ComplaintItem
import com.avatar.app.data.model.Invoice
import com.avatar.app.data.model.MarketPurchaseItem
import com.avatar.app.ui.invoices.InvoiceDetailDialog
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerSalesScreen(
    viewModel: FarmerSalesViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Satışlar, 1 = Şikayetler
    var recallLotTarget by remember { mutableStateOf<Pair<Int, String>?>(null) } // lotId to lotNumber
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val apiClient = remember { com.avatar.app.data.remote.ApiClient(context) }
    var activeInvoiceForDialog by remember { mutableStateOf<Invoice?>(null) }

    activeInvoiceForDialog?.let { invoice ->
        InvoiceDetailDialog(
            invoice = invoice,
            onDismiss = { activeInvoiceForDialog = null }
        )
    }

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

    recallLotTarget?.let { (lotId, lotNumber) ->
        QuickRecallDialog(
            lotId = lotId,
            lotNumber = lotNumber,
            isSubmitting = state.isInitiatingRecall,
            onDismiss = { recallLotTarget = null },
            onConfirm = { reason ->
                viewModel.initiateRecall(lotId, reason) {
                    recallLotTarget = null
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
                        Text("💰 Market Satışlarım & Ciro", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Marketlerin satın aldığı ürünler ve şikayet bildirimleri", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            // İstatistik Kartları (Ciro, Satış Adedi, Market Sayısı, Şikayet Sayısı)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Toplam Ciro", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "${String.format(Locale.US, "%.1f", state.totalRevenue)} EUR",
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
                        Text("Satış Miktarı", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "${String.format(Locale.US, "%.0f", state.totalSoldQuantity)} adet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.activeComplaintCount > 0) Color(0xFFFEF2F2) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Şikayet / Bildirim", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "${state.activeComplaintCount}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.activeComplaintCount > 0) Color(0xFFDC2626) else Color.DarkGray
                        )
                    }
                }
            }

            // TabRow: Satışlar vs Şikayetler
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Satışlarım (${state.sales.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Şikayetler (${state.complaints.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            if (state.complaints.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFDC2626)) {
                                    Text(
                                        "${state.complaints.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Yenile Butonu & Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedTab == 0) "Marketlere Yapılan Mamul Satışları" else "Market Kalite Bildirimleri",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                IconButton(onClick = { viewModel.loadData() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                }
            }

            // İçerik
            if (state.isLoading && state.sales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ForestGreen)
                }
            } else if (selectedTab == 0) {
                // Satışlar Sekmesi
                if (state.sales.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛒", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Henüz market satışı gerçekleşmedi", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Mamul ilanlarınıza marketler sipariş verdikçe burada listelenecektir.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.sales) { sale ->
                            SaleItemCard(
                                sale = sale,
                                onInitiateRecall = {
                                    recallLotTarget = Pair(sale.lotId, sale.lotNumber)
                                },
                                onViewInvoice = {
                                    coroutineScope.launch {
                                        try {
                                            val res = apiClient.invoiceService.getInvoices(referenceType = "market_purchase", referenceId = sale.id)
                                            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                                                activeInvoiceForDialog = res.body()!!.first()
                                            } else {
                                                snackbarHostState.showSnackbar("Bu satışa ait fatura kaydı bulunamadı.")
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Fatura yüklenemedi: ${e.localizedMessage}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Şikayetler Sekmesi
                if (state.complaints.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎉", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Herhangi bir kalite sorunu bildirimi yok!", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Marketlerden gelen ürün şikayetleri burada listelenir.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.complaints) { complaint ->
                            ComplaintItemCard(
                                complaint = complaint,
                                onInitiateRecall = {
                                    recallLotTarget = Pair(complaint.lotId, complaint.lotNumber)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaleItemCard(
    sale: MarketPurchaseItem,
    onInitiateRecall: () -> Unit,
    onViewInvoice: () -> Unit
) {
    val isRecalled = sale.status == "geri_cagrildi" || sale.lotStatus == "recalled"

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
                        sale.productName ?: "Mamul Satışı",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF3E8FF)) {
                            Text(
                                "Lot: ${sale.lotNumber}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7E22CE),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isRecalled) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                        ) {
                            Text(
                                if (isRecalled) "⛔ GERİ ÇAĞRILDI / BLOKE" else "✓ Teslim Edildi / Ödendi",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecalled) Color(0xFFDC2626) else ForestGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${String.format(Locale.US, "%.2f", sale.totalPrice)} ${sale.currency ?: "EUR"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ForestGreen
                    )
                    Text(
                        "${sale.quantity.toInt()} paket × ${String.format(Locale.US, "%.2f", sale.unitPrice)}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Alıcı Market Bilgisi & Fatura Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(sale.marketName ?: "Market", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    if (!sale.marketPhone.isNullOrBlank()) {
                        Text("Tel: ${sale.marketPhone}", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        sale.purchaseDate.take(10),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    OutlinedButton(
                        onClick = onViewInvoice,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fatura İndir / Gör", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (!isRecalled) {
                        Button(
                            onClick = onInitiateRecall,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Geri Çağır", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Kalite Şikayeti / Sorun Varsa
            if ((sale.complaintCount ?: 0) > 0 || !sale.latestComplaint.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Market Kalite Bildirimi:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFDC2626))
                        }
                        Text(
                            sale.latestComplaint ?: "Market bu parti için sorun bildirdi.",
                            fontSize = 11.sp,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (isRecalled) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Bu parti geri çağrılmıştır (tüm market satışları bloke edildi).",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = onInitiateRecall,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("⛔ Bu Partiyi Geri Çağır & Bloke Et", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplaintItemCard(
    complaint: ComplaintItem,
    onInitiateRecall: () -> Unit
) {
    val isComplaintRecalled = complaint.lotStatus == "recalled"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isComplaintRecalled) Color(0xFFF8FAFC) else Color(0xFFFEF2F2)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isComplaintRecalled) Color(0xFFE2E8F0) else Color(0xFFFCA5A5))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isComplaintRecalled) Icons.Default.CheckCircle else Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = if (isComplaintRecalled) ForestGreen else Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Lot No: ${complaint.lotNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isComplaintRecalled) Color(0xFF166534) else Color(0xFF991B1B)
                    )
                }
                Text(complaint.createdAt.take(10), fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(complaint.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.DarkGray)
            Text(complaint.message, fontSize = 12.sp, color = if (isComplaintRecalled) Color.Gray else Color(0xFF7F1D1D), modifier = Modifier.padding(vertical = 4.dp))

            Spacer(modifier = Modifier.height(8.dp))
            if (isComplaintRecalled) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "✓ Bu partiye ait ürünler tüm marketlerden geri çağrıldı ve stok bloke edildi.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF166534)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onInitiateRecall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("⛔ Geri Çağırma (Recall) Başlat ve Partiyi Bloke Et", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun QuickRecallDialog(
    lotId: Int,
    lotNumber: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("Market kalite geri bildirimi ve mikrobiyolojik standart dışılık şüphesi") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⛔ Geri Çağırma Başlat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFDC2626))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))) {
                    Text(
                        "Lot No: $lotNumber\nBu işlem, partiyi ve ilgili tüm satış/ilan kayıtlarını BLOKE edecek ve marketlere otomatik acil geri çağırma bildirimi gönderecektir.",
                        fontSize = 11.sp,
                        color = Color(0xFF991B1B),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Geri Çağırma Gerekçesi") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Vazgeç")
                    }
                    Button(
                        onClick = { onConfirm(reason) },
                        enabled = !isSubmitting && reason.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bloke Ediliyor...")
                        } else {
                            Text("Geri Çağır & Bloke Et", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
