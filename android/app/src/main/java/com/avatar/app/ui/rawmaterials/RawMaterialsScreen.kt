package com.avatar.app.ui.rawmaterials

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
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
import com.avatar.app.data.model.RawMaterial
import com.avatar.app.data.model.RawMaterialPresets
import com.avatar.app.ui.theme.ForestGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawMaterialsScreen(
    viewModel: RawMaterialsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<RawMaterial?>(null) }
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
                            "📦 Hammadde Stoğu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Et, Baharat, Bağırsak ve Malzeme Lotları",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMaterials() }) {
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
                onClick = { showAddDialog = true },
                containerColor = ForestGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Hammadde Ekle", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Kategori Filtre Çipleri
            val categories = listOf("Tümü", "Et", "Kuru/Toz", "Sıvı", "Bağırsak", "Süt/Yumurta", "Sebze/Meyve")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = if (cat == "Tümü") state.filteredCategory == null else state.filteredCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setFilterCategory(if (cat == "Tümü") null else cat)
                        },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ForestGreen
                    )
                } else if (state.displayedMaterials.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📦", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Hammadde bulunamadı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Baharat, tuz, bağırsak veya karkas stoğu eklemek için 'Hammadde Ekle' butonuna basınız.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.displayedMaterials) { item ->
                            RawMaterialCard(
                                material = item,
                                onDelete = { itemToDelete = item }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRawMaterialDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hammaddeyi Sil") },
            text = { Text("${item.materialType} (${item.lotNumber}) kaydını silmek (tüketildi olarak işaretlemek) istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMaterial(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
fun RawMaterialCard(
    material: RawMaterial,
    onDelete: () -> Unit
) {
    val preset = RawMaterialPresets.ITEMS.find { it.label.equals(material.materialType, ignoreCase = true) }
    val category = preset?.category ?: "Genel"
    val (catBg, catText) = when (category) {
        "Et" -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
        "Kuru/Toz" -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        "Sıvı" -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        "Bağırsak" -> Pair(Color(0xFFCFFAFE), Color(0xFF0E7490))
        "Süt/Yumurta" -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
        "Sebze/Meyve" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }

    val isConsumed = material.lotStatus == "consumed"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConsumed) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isConsumed) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(catBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = catText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        material.materialType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                if (!isConsumed) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Miktar", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        "${material.quantity} ${material.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text("Birim Fiyat", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        String.format(Locale.US, "%.2f €", material.unitPrice),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Toplam Tutar", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        String.format(Locale.US, "%.2f €", material.totalCost),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ForestGreen
                    )
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE0F2FE))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        material.lotNumber ?: "Lot atanmadı",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0369A1)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isConsumed) Color(0xFFF1F5F9) else Color(0xFFDCFCE7)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (isConsumed) "Tüketildi" else "Aktif Stok",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConsumed) Color.Gray else Color(0xFF15803D)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRawMaterialDialog(
    viewModel: RawMaterialsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

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
                                "Hammadde Stoğu Ekle",
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
                    Surface(
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                Text("İptal")
                            }
                            Button(
                                onClick = {
                                    viewModel.createMaterial(onSuccess = onDismiss)
                                },
                                enabled = !state.isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.weight(2f)
                            ) {
                                if (state.isSubmitting) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ekleniyor...")
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stoğa Ekle", fontWeight = FontWeight.Bold)
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
                        Text(
                            "Önceden Tanımlı Malzeme Seçiniz",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RawMaterialPresets.ITEMS.take(7).forEach { p ->
                                val isSelected = state.selectedPreset.label == p.label
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectPreset(p) },
                                    label = { Text(p.label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = state.customMaterialType,
                            onValueChange = { viewModel.updateForm(materialType = it) },
                            label = { Text("Malzeme Adı", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (state.quantity > 0) state.quantity.toString() else "",
                                onValueChange = {
                                    val q = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateForm(quantity = q)
                                },
                                label = { Text("Miktar", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.unit,
                                onValueChange = { viewModel.updateForm(unit = it) },
                                label = { Text("Birim", fontSize = 12.sp) },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (state.unitPrice > 0) state.unitPrice.toString() else "",
                                onValueChange = {
                                    val p = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateForm(unitPrice = p)
                                },
                                label = { Text("Birim Fiyat (€)", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.supplierName,
                                onValueChange = { viewModel.updateForm(supplierName = it) },
                                label = { Text("Tedarikçi", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hesaplanan Toplam Maliyet:", fontSize = 13.sp, color = Color.DarkGray)
                                Text(
                                    String.format(Locale.US, "%.2f €", state.totalCost),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
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
