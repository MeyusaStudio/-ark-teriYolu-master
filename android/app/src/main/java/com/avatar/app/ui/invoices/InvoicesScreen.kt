package com.avatar.app.ui.invoices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avatar.app.data.model.Invoice
import com.avatar.app.data.model.User
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    user: User,
    viewModel: InvoicesViewModel,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user.id) {
        viewModel.setCurrentUser(user)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    state.selectedInvoice?.let { invoice ->
        InvoiceDetailDialog(
            invoice = invoice,
            onDismiss = { viewModel.closeInvoiceDetail() }
        )
    }

    val userRole = user.role.lowercase()
    val headerTitle = when (userRole) {
        "sucukcu" -> "🛠️ Fason Hizmet Faturaları"
        "market" -> "🛒 Toptan Alış Faturaları"
        else -> "📄 E-Fatura & E-Arşiv"
    }
    val headerSubtitle = when (userRole) {
        "sucukcu" -> "İşletmelere verdiğiniz fason üretim işçilik faturaları"
        "market" -> "Hayvancılardan temin ettiğiniz toptan mamul faturaları"
        else -> "Fason gider ve toptan mamul satış faturalarınız"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(headerTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(headerSubtitle, fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadInvoices() }) {
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
            // Özet İstatistikler (Belge Adedi, Toplam Tutar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("İlgili Belgeler", fontSize = 11.sp, color = Color.Gray)
                        Text("${state.roleInvoices.size} adet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                }

                Card(
                    modifier = Modifier.weight(1.3f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(if (userRole == "sucukcu") "Hizmet Bedeli Tutarı" else if (userRole == "market") "Toptan Alış Tutarı" else "Toplam Tutar", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "${String.format(Locale.US, "%.1f", state.totalAmount)} EUR",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                    }
                }
            }

            // Sekmeler: Hayvancı için Tümü/Hizmet/Satış; Sucukçu veya Market için bilgilendirici rol etiketi
            if (userRole == "hayvanci") {
                TabRow(
                    selectedTabIndex = when (state.filterType) {
                        "hizmet" -> 1
                        "satis" -> 2
                        else -> 0
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PrimaryBlue
                ) {
                    Tab(
                        selected = state.filterType == "all",
                        onClick = { viewModel.setFilterType("all") },
                        text = { Text("Tümü (${state.roleInvoices.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = state.filterType == "hizmet",
                        onClick = { viewModel.setFilterType("hizmet") },
                        text = { Text("🛠️ Fason Gider (${state.serviceInvoicesCount})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = state.filterType == "satis",
                        onClick = { viewModel.setFilterType("satis") },
                        text = { Text("🛒 Mamul Ciro (${state.salesInvoicesCount})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            } else {
                Surface(
                    color = if (userRole == "sucukcu") Color(0xFFF3E8FF) else Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (userRole == "sucukcu")
                            "ℹ️ Yalnızca üretici olarak düzenlediğiniz fason işçilik faturaları görüntülenmektedir."
                        else
                            "ℹ️ Yalnızca toptan mamul alımlarınıza ait yasal e-arşiv faturaları görüntülenmektedir.",
                        fontSize = 11.sp,
                        color = if (userRole == "sucukcu") Color(0xFF6B21A8) else Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Arama Çubuğu
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Fatura no, gönderen, alıcı veya lot ara...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // Fatura Listesi
            if (state.isLoading && state.invoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (state.filteredInvoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📄", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kayıtlı fatura bulunamadı", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Üretim onaylandığında veya satış yapıldığında faturalar otomatik oluşur.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.filteredInvoices) { inv ->
                        InvoiceListItemCard(
                            invoice = inv,
                            currentUser = state.currentUser,
                            onClick = { viewModel.openInvoiceDetail(inv) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceListItemCard(
    invoice: Invoice,
    currentUser: User?,
    onClick: () -> Unit
) {
    val isHizmet = invoice.invoiceType == "hizmet"
    val isSenderMe = currentUser?.id == invoice.senderId || (currentUser?.email?.equals(invoice.senderEmail, ignoreCase = true) == true)
    val isReceiverMe = currentUser?.id == invoice.receiverId || (currentUser?.email?.equals(invoice.receiverEmail, ignoreCase = true) == true)

    val senderDisplay = if (isSenderMe) "${invoice.senderName ?: "İşletmeniz"} (Siz)" else (invoice.senderName ?: "Satıcı")
    val receiverDisplay = if (isReceiverMe) "${invoice.receiverName ?: "İşletmeniz"} (Siz)" else (invoice.receiverName ?: "Alıcı")

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isHizmet) Color(0xFFF3E8FF) else Color(0xFFDCFCE7)
                    ) {
                        Text(
                            if (isHizmet) "🛠️ FASON HİZMET" else "🛒 MAMUL SATIŞ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHizmet) Color(0xFF7E22CE) else ForestGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(invoice.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Text(invoice.invoiceDate.take(10), fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "$senderDisplay  ➔  $receiverDisplay",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${invoice.productName ?: "Ürün / Hizmet"}${if (!invoice.lotNumber.isNullOrBlank()) " (${invoice.lotNumber})" else ""}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${String.format(Locale.US, "%.2f", invoice.totalAmount)} ${invoice.currency}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ForestGreen
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            "✓ ${invoice.status.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Faturayı İncele & İndir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
