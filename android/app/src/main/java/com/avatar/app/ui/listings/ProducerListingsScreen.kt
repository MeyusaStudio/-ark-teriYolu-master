package com.avatar.app.ui.listings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avatar.app.data.model.ProducerListing
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProducerListingsScreen(
    viewModel: ProducerListingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadData()
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
                            "🏭 Sucukçu Fason İlanları",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Etlerinizi Sucuk, Salam, Pastırmaya Dönüştürün",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
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
            // Arama Kutusu
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("İlan, ürün veya üretici ara...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Web ile Birebir İlan Filtreleme Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Başlık & Temizle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("İlan Filtreleme", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "(${state.filteredListings.size} ilan · Depoda ${String.format(Locale.US, "%.1f", state.totalAvailMeatKg)} kg et)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        if (state.hasActiveFilters) {
                            TextButton(
                                onClick = { viewModel.clearFilters() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("Temizle", color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Ürün Türü Filtresi
                    Text("Ürün Türü:", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedProductFilter == null,
                            onClick = { viewModel.setProductFilter(null) },
                            label = { Text("Tümü (${state.listings.size})", fontSize = 11.sp) }
                        )
                        val productTypes = listOf(
                            "Sucuk" to "🌭",
                            "Salam" to "🥩",
                            "Sosis" to "🍖",
                            "Pastırma" to "🥓"
                        )
                        productTypes.forEach { (name, emoji) ->
                            val count = state.listings.count { it.productName.contains(name, ignoreCase = true) }
                            val isSelected = state.selectedProductFilter == name
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setProductFilter(if (isSelected) null else name) },
                                label = { Text("$emoji $name ($count)", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Hayvan Türü Uyumu Filtresi (Mezbaha Stoğu ile)
                    Text("Hayvan Türü (Depo Stoğu):", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedAnimalFilter == null,
                            onClick = { viewModel.setAnimalFilter(null) },
                            label = { Text("Fark Etmez", fontSize = 11.sp) }
                        )
                        val animals = listOf(
                            "inek" to ("🐄 İnek / Dana" to state.getAnimalMeatKg("inek")),
                            "koyun" to ("🐑 Koyun / Kuzu" to state.getAnimalMeatKg("koyun")),
                            "tavuk" to ("🐔 Tavuk" to state.getAnimalMeatKg("tavuk"))
                        )
                        animals.forEach { (key, pair) ->
                            val (label, kg) = pair
                            val isSelected = state.selectedAnimalFilter == key
                            val text = if (kg > 0) "$label (${String.format(Locale.US, "%.0f", kg)} kg)" else label
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setAnimalFilter(if (isSelected) null else key) },
                                label = { Text(text, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ForestGreen)
                } else if (state.filteredListings.isEmpty()) {
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
                            "Uygun fason ilan bulunamadı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Sucukçu hesabından henüz bu ürün için ilan açılmamış olabilir.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (state.hasActiveFilters) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { viewModel.clearFilters() }) {
                                Text("Filtreleri Temizle", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.filteredListings) { listing ->
                            HayvanciProducerListingCard(
                                listing = listing,
                                onMlClick = { viewModel.openMlPrediction(listing) },
                                onOrderClick = { viewModel.openOrderDialog(listing) }
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

    state.selectedListingForOrder?.let { listing ->
        CreateFasonOrderDialog(
            listing = listing,
            viewModel = viewModel,
            onDismiss = { viewModel.closeOrderDialog() }
        )
    }

    state.selectedListingForMl?.let { listing ->
        MLPredictionDialog(
            listing = listing,
            viewModel = viewModel,
            onDismiss = { viewModel.closeMlPrediction() }
        )
    }
}

@Composable
fun HayvanciProducerListingCard(
    listing: ProducerListing,
    onMlClick: () -> Unit,
    onOrderClick: () -> Unit
) {
    val emoji = when (listing.productName.lowercase()) {
        "sucuk" -> "🌭"
        "salam" -> "🥩"
        "sosis" -> "🍖"
        "pastırma", "pastirma" -> "🥓"
        else -> "🍽️"
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
                    Text(emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            listing.productName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Üretici: ${listing.producerName ?: "Sucukçu A.Ş."}",
                            fontSize = 12.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    String.format(Locale.US, "%.2f %s / paket", listing.servicePrice, listing.currency ?: "TL"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ForestGreen
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                listing.description ?: "Fason üretim hizmeti",
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            if (!listing.phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(listing.phone, fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Min. ${(listing.minOrderQty ?: 1.0).toInt()} paket",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onMlClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7E22CE)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🤖 AI Tahmini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onOrderClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sipariş Ver", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
