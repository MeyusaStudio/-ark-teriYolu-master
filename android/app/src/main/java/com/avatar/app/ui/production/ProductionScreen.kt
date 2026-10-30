package com.avatar.app.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avatar.app.data.model.ProductionOrder
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    viewModel: ProductionViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadProductions()
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
                            "🏭 Üretim & EKP Hesabı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Fason Üretim Onayı ve Gerçek Maliyet Takibi",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadProductions() }) {
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
            } else if (state.productions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🏭", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Üretime alınmış sipariş bulunmuyor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Hayvancılardan gelen fason siparişleri 'Siparişler' sekmesinden onayladığınızda burada üretime aktarılır.",
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
                    items(state.productions) { prod ->
                        ProductionCard(
                            order = prod,
                            onCompleteClick = { viewModel.openCompleteDialog(prod) }
                        )
                    }
                }
            }
        }
    }

    state.selectedForComplete?.let { prod ->
        CompleteProductionDialog(
            order = prod,
            viewModel = viewModel,
            onDismiss = { viewModel.closeCompleteDialog() }
        )
    }
}

@Composable
fun ProductionCard(
    order: ProductionOrder,
    onCompleteClick: () -> Unit
) {
    val isDone = order.status == "tamamlandi" || order.status == "sevk_edildi"

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
                Column {
                    Text(order.productionNumber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Müşteri: ${order.buyerName ?: "İşletme"}", fontSize = 11.sp, color = Color.Gray)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDone) Color(0xFFDCFCE7) else Color(0xFFEFF6FF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        if (isDone) "✓ Tamamlandı" else "⚙️ Üretimde",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) Color(0xFF15803D) else Color(0xFF1D4ED8)
                    )
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
                    Text("Ürün", fontSize = 10.sp, color = Color.Gray)
                    Text(order.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text("Girdi / Çıktı", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        "${if (order.totalInputKg != null) String.format(Locale.US, "%.2f kg", order.totalInputKg) else "-"} ➔ ${Math.round(order.actualQty ?: order.expectedQty)} paket",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Fire Oranı", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        if (order.fireRate != null) "%${String.format(Locale.US, "%.1f", order.fireRate)}" else "Hesaplanmadı",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if ((order.fireRate ?: 0.0) > 15) Color(0xFFDC2626) else ForestGreen
                    )
                }
            }

            if (isDone && order.ekp != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gerçek Maliyet (EKP):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    Text(
                        String.format(Locale.US, "%.4f € / paket", order.ekp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFB45309)
                    )
                }
            }

            if (!isDone) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onCompleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Üretimi Onayla & Hizmet Bedeli Gir", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProductionDialog(
    order: ProductionOrder,
    viewModel: ProductionViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    if (state.completionResult != null) "🎉 Üretim Tamamlandı" else "Üretim Onayı & Hizmet Bedeli",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${order.productionNumber} · ${order.productName}",
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
                },
                bottomBar = {
                    Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.completionResult != null) {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Tamam / Kapat", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                    Text("Vazgeç")
                                }
                                Button(
                                    onClick = {
                                        viewModel.submitCompleteProduction()
                                    },
                                    enabled = !state.isSubmitting,
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    modifier = Modifier.weight(2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (state.isSubmitting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Onaylanıyor...")
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Üretimi Onayla & Bitir", fontWeight = FontWeight.Bold)
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Tamamlama Sonuç Özeti
                    state.completionResult?.let { res ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF86EFAC)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Mamul Lotu Başarıyla Üretildi!", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCFCE7)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("📦 Mamul Lot No:", fontSize = 12.sp, color = Color.Gray)
                                                Text(res.lotNumber ?: "-", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Gerçekleşen Çıktı:", fontSize = 12.sp, color = Color.Gray)
                                                Text("${Math.round(res.actualQty ?: 0.0)} Paket", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Oluşan Fire:", fontSize = 12.sp, color = Color.Gray)
                                                Text("${String.format(Locale.US, "%.2f", res.fireQty ?: 0.0)} kg (%%%${String.format(Locale.US, "%.1f", res.fireRate ?: 0.0)})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFDC2626))
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Gerçek Maliyet (EKP):", fontSize = 12.sp, color = Color.Gray)
                                                Text(String.format(Locale.US, "%.4f € / paket", res.ekp ?: 0.0), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB45309))
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Tavsiye Satış (VKP):", fontSize = 12.sp, color = Color.Gray)
                                                Text(String.format(Locale.US, "%.4f € / paket", res.vkp ?: 0.0), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreen)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Fason Hizmet Bedeli Faturası:", fontSize = 12.sp, color = Color.Gray)
                                                Text(String.format(Locale.US, "%.2f €", res.serviceCost ?: 0.0), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF7E22CE))
                                            }
                                        }
                                    }

                                    Text(
                                        "Bu üretim tamamlanarak mamul lotu sisteme işlenmiş ve işletmeye fason hizmet bedeli faturası kesilmiştir.",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    // Form Alanları (Eğer henüz tamamlanmadıysa)
                    if (state.completionResult == null) {
                        // Sipariş ve Alıcı Bilgisi
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Müşteri (Hayvancı): ${order.buyerName ?: "İşletme"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Hedef Ürün: ${order.productName} · Talep: ${Math.round(order.expectedQty)} paket (1 kg/paket)", fontSize = 12.sp, color = Color(0xFF1E3A8A))
                                }
                            }
                        }

                        // Kullanılan Hammaddeler Detayı
                        if (state.detailRawMaterials.isNotEmpty()) {
                            item {
                                Text(
                                    "Kullanılan Hammaddeler & Et Lotları (${state.detailRawMaterials.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(state.detailRawMaterials) { rm ->
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
                                            Text(rm.materialType, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Text("Lot: ${rm.lotNumber ?: "Lot #${rm.lotId ?: "-"}"}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Text(
                                            "${String.format(Locale.US, "%.2f", rm.effectiveQuantity)} ${rm.unit ?: "kg"}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = ForestGreen
                                        )
                                    }
                                }
                            }
                        }

                        // Hızlı Görev 1 Senaryo Butonu
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Hızlı Yarışma Senaryosu (Görev 1)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestGreen)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.applyScenario430to400() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
                                    ) {
                                        Text("4.30 kg Girdi ➔ 4.00 kg Net Mamul (Fire %6.98)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Girdi ve Çıktı Miktarları
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = String.format(Locale.US, "%.2f kg", state.orderInputKg),
                                    onValueChange = {},
                                    label = { Text("Toplam Girdi (kg)", fontSize = 12.sp) },
                                    readOnly = true,
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = if (state.actualQty > 0) state.actualQty.toString() else "",
                                    onValueChange = {
                                        val v = it.toDoubleOrNull() ?: 0.0
                                        viewModel.updateCompleteForm(actualQty = v)
                                    },
                                    label = { Text("Gerçekleşen Paket", fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        // Fason Hizmet Bedeli
                        item {
                            OutlinedTextField(
                                value = if (state.unitServiceCost > 0) state.unitServiceCost.toString() else "",
                                onValueChange = {
                                    val v = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateCompleteForm(unitServiceCost = v)
                                },
                                label = { Text("Paket Başı Fason Hizmet Bedeli (€ / paket)", fontSize = 12.sp) },
                                supportingText = {
                                    Text(
                                        "Toplam Fason Hizmet Bedeli: ${String.format(Locale.US, "%.2f €", state.totalServiceCost)} (${state.actualQty.toInt()} paket × ${state.unitServiceCost} €)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreen
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // Canlı Fire ve EKP Özeti
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Canlı Kütle Dengesi & Maliyet Özeti", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Üretilen Paket:", fontSize = 12.sp, color = Color.Gray)
                                        Text("${state.actualQty.toInt()} Paket", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Hesaplanan Fire:", fontSize = 12.sp, color = Color.Gray)
                                        Text(
                                            "${String.format(Locale.US, "%.3f", state.fireKg)} kg (%${String.format(Locale.US, "%.2f", state.fireRate)})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (state.fireRate > 20) Color(0xFFDC2626) else ForestGreen
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Tahmini EKP (Birim Maliyet):", fontSize = 12.sp, color = Color.Gray)
                                        Text(
                                            String.format(Locale.US, "%.4f € / paket", state.estimatedEkp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PrimaryBlue
                                        )
                                    }

                                    if (state.fireRate > 20) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF2F2),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("⚠️ Fire oranı %20 eşiğini aştı! Yangı/fire anomalisi incelenmelidir.", fontSize = 10.sp, color = Color(0xFF991B1B))
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
