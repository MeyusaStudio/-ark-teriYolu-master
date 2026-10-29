package com.avatar.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avatar.app.data.model.User
import com.avatar.app.ui.auth.AuthViewModel
import com.avatar.app.ui.butchery.ButcheryScreen
import com.avatar.app.ui.butchery.ButcheryViewModel
import com.avatar.app.ui.components.ServerConfigDialog
import com.avatar.app.ui.listings.ProducerListingsManageScreen
import com.avatar.app.ui.listings.ProducerListingsScreen
import com.avatar.app.ui.listings.ProducerListingsViewModel
import com.avatar.app.ui.lots.LotsScreen
import com.avatar.app.ui.lots.LotsViewModel
import com.avatar.app.ui.orders.OrdersScreen
import com.avatar.app.ui.orders.OrdersViewModel
import com.avatar.app.ui.production.ProductionScreen
import com.avatar.app.ui.production.ProductionViewModel
import com.avatar.app.ui.rawmaterials.RawMaterialsScreen
import com.avatar.app.ui.rawmaterials.RawMaterialsViewModel
import com.avatar.app.ui.theme.*

@Composable
fun HomeScreen(
    user: User,
    authViewModel: AuthViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val butcheryViewModel: ButcheryViewModel = viewModel()
    val rawMaterialsViewModel: RawMaterialsViewModel = viewModel()
    val listingsViewModel: ProducerListingsViewModel = viewModel()
    val ordersViewModel: OrdersViewModel = viewModel()
    val productionViewModel: ProductionViewModel = viewModel()
    val lotsViewModel: LotsViewModel = viewModel()

    val role = user.role.lowercase()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                if (role == "hayvanci") {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.ContentCut, contentDescription = "Parçalama") },
                        label = { Text("Parçalama", fontSize = 9.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = "Hammadde") },
                        label = { Text("Hammadde", fontSize = 9.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Storefront, contentDescription = "Fason İlanlar") },
                        label = { Text("İlanlar", fontSize = 9.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Siparişler") },
                        label = { Text("Siparişler", fontSize = 9.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.QrCode, contentDescription = "Lotlar") },
                        label = { Text("Lotlar", fontSize = 9.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                        label = { Text("Profil", fontSize = 9.sp) }
                    )
                } else if (role == "sucukcu") {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Campaign, contentDescription = "İlanlarım") },
                        label = { Text("İlanlarım", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Assignment, contentDescription = "Siparişler") },
                        label = { Text("Siparişler", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.PrecisionManufacturing, contentDescription = "Üretim & EKP") },
                        label = { Text("Üretim (EKP)", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                        label = { Text("Profil", fontSize = 10.sp) }
                    )
                } else {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Storefront, contentDescription = "Pazar") },
                        label = { Text("Market İlanları", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Alımlarım") },
                        label = { Text("Satın Aldıklarım", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                        label = { Text("Profil", fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (role == "hayvanci") {
                when (selectedTab) {
                    0 -> ButcheryScreen(viewModel = butcheryViewModel)
                    1 -> RawMaterialsScreen(viewModel = rawMaterialsViewModel)
                    2 -> ProducerListingsScreen(viewModel = listingsViewModel)
                    3 -> OrdersScreen(user = user, viewModel = ordersViewModel)
                    4 -> LotsScreen(viewModel = lotsViewModel)
                    5 -> ProfileScreen(user = user, authViewModel = authViewModel)
                }
            } else if (role == "sucukcu") {
                when (selectedTab) {
                    0 -> ProducerListingsManageScreen(viewModel = listingsViewModel)
                    1 -> OrdersScreen(user = user, viewModel = ordersViewModel, onNavigateToProduction = { selectedTab = 2 })
                    2 -> ProductionScreen(viewModel = productionViewModel)
                    3 -> ProfileScreen(user = user, authViewModel = authViewModel)
                }
            } else {
                when (selectedTab) {
                    0 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Market İlanları (Sıradaki Adım)") }
                    1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Satın Aldıklarım (Sıradaki Adım)") }
                    2 -> ProfileScreen(user = user, authViewModel = authViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.uiState.collectAsState()
    var showServerDialog by remember { mutableStateOf(false) }

    if (showServerDialog) {
        ServerConfigDialog(
            currentUrl = authState.serverUrl,
            onDismiss = { showServerDialog = false },
            onSave = { newUrl ->
                authViewModel.updateServerUrl(newUrl)
                showServerDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil & Sistem Durumu", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kullanıcı Bilgisi Kartı
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when (user.role) {
                                "hayvanci" -> ForestGreen.copy(alpha = 0.2f)
                                "sucukcu" -> PrimaryBlue.copy(alpha = 0.2f)
                                else -> OrangeMarket.copy(alpha = 0.2f)
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    when (user.role) {
                                        "hayvanci" -> "🐑"
                                        "sucukcu" -> "🥩"
                                        else -> "🛒"
                                    },
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Column {
                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(user.email, fontSize = 13.sp, color = Color.Gray)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (user.role) {
                                    "hayvanci" -> ForestGreen
                                    "sucukcu" -> PrimaryBlue
                                    else -> OrangeMarket
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    when (user.role) {
                                        "hayvanci" -> "İşletme (Hayvancı)"
                                        "sucukcu" -> "Üretici (Sucukçu)"
                                        else -> "Market / Tüketici"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Sunucu URL Ayarı Kartı
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sunucu Bağlantısı", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                authState.serverUrl,
                                fontSize = 12.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { showServerDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Ayar", tint = PrimaryBlue)
                        }
                    }
                }
            }

            // Çıkış Yap Butonu
            OutlinedButton(
                onClick = { authViewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Oturumu Kapat", fontWeight = FontWeight.Bold)
            }
        }
    }
}
