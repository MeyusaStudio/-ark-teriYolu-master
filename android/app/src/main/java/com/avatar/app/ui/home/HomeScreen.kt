package com.avatar.app.ui.home

import androidx.compose.foundation.clickable
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
import com.avatar.app.ui.admin.AdminUsersScreen
import com.avatar.app.ui.admin.AdminViewModel
import com.avatar.app.ui.auth.AuthViewModel
import com.avatar.app.ui.butchery.ButcheryScreen
import com.avatar.app.ui.butchery.ButcheryViewModel
import com.avatar.app.ui.components.ServerConfigDialog
import com.avatar.app.ui.invoices.InvoicesScreen
import com.avatar.app.ui.invoices.InvoicesViewModel
import com.avatar.app.ui.listings.FarmerListingsScreen
import com.avatar.app.ui.listings.FarmerListingsViewModel
import com.avatar.app.ui.listings.ProducerListingsManageScreen
import com.avatar.app.ui.listings.ProducerListingsScreen
import com.avatar.app.ui.listings.ProducerListingsViewModel
import com.avatar.app.ui.lots.LotsScreen
import com.avatar.app.ui.lots.LotsViewModel
import com.avatar.app.ui.market.MarketListingsScreen
import com.avatar.app.ui.market.MarketPurchasesScreen
import com.avatar.app.ui.market.MarketViewModel
import com.avatar.app.ui.notifications.NotificationsDialog
import com.avatar.app.ui.notifications.NotificationsViewModel
import com.avatar.app.ui.orders.OrdersScreen
import com.avatar.app.ui.orders.OrdersViewModel
import com.avatar.app.ui.production.ProductionScreen
import com.avatar.app.ui.production.ProductionViewModel
import com.avatar.app.ui.rawmaterials.RawMaterialsScreen
import com.avatar.app.ui.rawmaterials.RawMaterialsViewModel
import com.avatar.app.ui.recalls.RecallsScreen
import com.avatar.app.ui.recalls.RecallsViewModel
import com.avatar.app.ui.sales.FarmerSalesScreen
import com.avatar.app.ui.sales.FarmerSalesViewModel
import com.avatar.app.ui.theme.*

@Composable
fun HomeScreen(
    user: User,
    authViewModel: AuthViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val butcheryViewModel: ButcheryViewModel = viewModel()
    val rawMaterialsViewModel: RawMaterialsViewModel = viewModel()
    val listingsViewModel: ProducerListingsViewModel = viewModel()
    val farmerListingsViewModel: FarmerListingsViewModel = viewModel()
    val ordersViewModel: OrdersViewModel = viewModel()
    val farmerSalesViewModel: FarmerSalesViewModel = viewModel()
    val productionViewModel: ProductionViewModel = viewModel()
    val lotsViewModel: LotsViewModel = viewModel()
    val recallsViewModel: RecallsViewModel = viewModel()
    val marketViewModel: MarketViewModel = viewModel()
    val invoicesViewModel: InvoicesViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel()
    val notificationsViewModel: NotificationsViewModel = viewModel()

    val notifState by notificationsViewModel.uiState.collectAsState()
    val role = user.role.lowercase()

    if (showNotificationsDialog) {
        NotificationsDialog(
            viewModel = notificationsViewModel,
            onDismiss = { showNotificationsDialog = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (notifState.unreadCount > 0 && role != "admin") {
                ExtendedFloatingActionButton(
                    onClick = { showNotificationsDialog = true },
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${notifState.unreadCount} Yeni Mesaj", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                if (role == "admin") {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.ManageAccounts, contentDescription = "Kullanıcılar") },
                        label = { Text("Kullanıcılar", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                        label = { Text("Profil", fontSize = 11.sp) }
                    )
                } else if (role == "hayvanci") {
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
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Sipariş ve Satış") },
                        label = { Text("Sipariş/Satış", fontSize = 8.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.QrCode, contentDescription = "Lotlar ve Recall") },
                        label = { Text("Lot/Recall", fontSize = 8.sp) }
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
            if (role == "admin") {
                when (selectedTab) {
                    0 -> AdminUsersScreen(viewModel = adminViewModel, onOpenNotifications = { showNotificationsDialog = true })
                    1 -> ProfileScreen(
                        user = user,
                        authViewModel = authViewModel,
                        invoicesViewModel = invoicesViewModel,
                        notificationsViewModel = notificationsViewModel,
                        onOpenNotifications = { showNotificationsDialog = true }
                    )
                }
            } else if (role == "hayvanci") {
                when (selectedTab) {
                    0 -> ButcheryScreen(viewModel = butcheryViewModel)
                    1 -> RawMaterialsScreen(viewModel = rawMaterialsViewModel)
                    2 -> HayvanciListingsContainer(producerViewModel = listingsViewModel, farmerViewModel = farmerListingsViewModel)
                    3 -> HayvanciOrdersAndSalesContainer(user = user, ordersViewModel = ordersViewModel, salesViewModel = farmerSalesViewModel)
                    4 -> HayvanciLotsAndRecallsContainer(lotsViewModel = lotsViewModel, recallsViewModel = recallsViewModel)
                    5 -> ProfileScreen(
                        user = user,
                        authViewModel = authViewModel,
                        invoicesViewModel = invoicesViewModel,
                        notificationsViewModel = notificationsViewModel,
                        onOpenNotifications = { showNotificationsDialog = true }
                    )
                }
            } else if (role == "sucukcu") {
                when (selectedTab) {
                    0 -> ProducerListingsManageScreen(viewModel = listingsViewModel)
                    1 -> OrdersScreen(user = user, viewModel = ordersViewModel, onNavigateToProduction = { selectedTab = 2 })
                    2 -> ProductionScreen(viewModel = productionViewModel)
                    3 -> ProfileScreen(
                        user = user,
                        authViewModel = authViewModel,
                        invoicesViewModel = invoicesViewModel,
                        notificationsViewModel = notificationsViewModel,
                        onOpenNotifications = { showNotificationsDialog = true }
                    )
                }
            } else {
                when (selectedTab) {
                    0 -> MarketListingsScreen(viewModel = marketViewModel)
                    1 -> MarketPurchasesScreen(viewModel = marketViewModel)
                    2 -> ProfileScreen(
                        user = user,
                        authViewModel = authViewModel,
                        invoicesViewModel = invoicesViewModel,
                        notificationsViewModel = notificationsViewModel,
                        onOpenNotifications = { showNotificationsDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
fun HayvanciListingsContainer(
    producerViewModel: ProducerListingsViewModel,
    farmerViewModel: FarmerListingsViewModel
) {
    var subTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = Color.White,
            contentColor = PrimaryBlue
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("Fason İlanlar", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("Mamul İlanlarım", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (subTab == 0) {
                ProducerListingsScreen(viewModel = producerViewModel)
            } else {
                FarmerListingsScreen(viewModel = farmerViewModel)
            }
        }
    }
}

@Composable
fun HayvanciOrdersAndSalesContainer(
    user: User,
    ordersViewModel: OrdersViewModel,
    salesViewModel: FarmerSalesViewModel
) {
    var subTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = Color.White,
            contentColor = ForestGreen
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("Fason Siparişler", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("Market Satışlarım", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (subTab == 0) {
                OrdersScreen(user = user, viewModel = ordersViewModel)
            } else {
                FarmerSalesScreen(viewModel = salesViewModel)
            }
        }
    }
}

@Composable
fun HayvanciLotsAndRecallsContainer(
    lotsViewModel: LotsViewModel,
    recallsViewModel: RecallsViewModel
) {
    var subTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = Color.White,
            contentColor = Color(0xFFDC2626)
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("Depo Lotları", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("Geri Çağırma (Recall)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (subTab == 0) {
                LotsScreen(viewModel = lotsViewModel)
            } else {
                RecallsScreen(viewModel = recallsViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    authViewModel: AuthViewModel,
    invoicesViewModel: InvoicesViewModel,
    notificationsViewModel: NotificationsViewModel? = null,
    onOpenNotifications: () -> Unit = {}
) {
    val authState by authViewModel.uiState.collectAsState()
    val notifState = notificationsViewModel?.uiState?.collectAsState()?.value
    var showServerDialog by remember { mutableStateOf(false) }
    var showInvoicesScreen by remember { mutableStateOf(false) }

    if (showInvoicesScreen) {
        InvoicesScreen(
            user = user,
            viewModel = invoicesViewModel,
            onBack = { showInvoicesScreen = false }
        )
        return
    }

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
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(
                            badge = {
                                if ((notifState?.unreadCount ?: 0) > 0) {
                                    Badge { Text(notifState!!.unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Bildirimler", tint = Color.White)
                        }
                    }
                },
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
                            color = when (user.role.lowercase()) {
                                "hayvanci" -> ForestGreen.copy(alpha = 0.2f)
                                "sucukcu" -> PrimaryBlue.copy(alpha = 0.2f)
                                "market" -> OrangeMarket.copy(alpha = 0.2f)
                                else -> Color(0xFF6B21A8).copy(alpha = 0.2f)
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    when (user.role.lowercase()) {
                                        "hayvanci" -> "🐑"
                                        "sucukcu" -> "🥩"
                                        "market" -> "🛒"
                                        else -> "🛡️"
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
                                color = when (user.role.lowercase()) {
                                    "hayvanci" -> ForestGreen
                                    "sucukcu" -> PrimaryBlue
                                    "market" -> OrangeMarket
                                    else -> Color(0xFF6B21A8)
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    when (user.role.lowercase()) {
                                        "hayvanci" -> "İşletme (Hayvancı)"
                                        "sucukcu" -> "Üretici (Sucukçu)"
                                        "market" -> "Market / Tüketici"
                                        else -> "Sistem Yöneticisi (Admin)"
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

            // Bildirimler & Mesajlar Kartı
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenNotifications() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFD97706))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🔔 Bildirimler & Yönetici Mesajları", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if ((notifState?.unreadCount ?: 0) > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFDC2626)
                                    ) {
                                        Text(
                                            "${notifState!!.unreadCount} yeni",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text("Yönetici mesajları, geri çağırmalar ve sistem duyuruları", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            // E-Faturalar & E-Arşiv Belgesi Kartı
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showInvoicesScreen = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryBlue)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("📄 E-Faturalar & E-Arşiv", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Fason ve mamul satış faturalarını inceleyin / indirin", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
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
