package com.avatar.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.avatar.app.data.model.AdminUserItem
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.OrangeMarket
import com.avatar.app.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: AdminViewModel,
    onOpenNotifications: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedUserForBan by remember { mutableStateOf<AdminUserItem?>(null) }
    var selectedUserForUnban by remember { mutableStateOf<AdminUserItem?>(null) }
    var selectedUserForMessage by remember { mutableStateOf<AdminUserItem?>(null) }
    var showBroadcastDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🛡️", fontSize = 20.sp)
                        Text(
                            "Kullanıcı Yönetimi (Admin)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Bildirimler",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { viewModel.loadUsers() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = Color.White
                        )
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
                .background(Color(0xFFF8FAFC))
        ) {
            // KPI Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard("Toplam", state.stats.total.toString(), Color(0xFF64748B), "👥")
                AdminStatCard("İşletmeler", state.stats.hayvanci.toString(), ForestGreen, "🐑")
                AdminStatCard("Üreticiler", state.stats.sucukcu.toString(), PrimaryBlue, "🥩")
                AdminStatCard("Marketler", state.stats.market.toString(), OrangeMarket, "🛒")
                AdminStatCard("Banlılar", state.stats.banned.toString(), Color(0xFFDC2626), "⛔")
            }

            // Arama ve Toplu Mesaj Çubuğu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("İsim, e-posta veya tel no ara...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Temizle")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Button(
                        onClick = { showBroadcastDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duyuru", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Rol Filtreleme Çipleri
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = state.selectedRole == "all",
                        onClick = { viewModel.setRoleFilter("all") },
                        label = { Text("Tümü", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = state.selectedRole == "hayvanci",
                        onClick = { viewModel.setRoleFilter("hayvanci") },
                        label = { Text("🐑 İşletmeler", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = state.selectedRole == "sucukcu",
                        onClick = { viewModel.setRoleFilter("sucukcu") },
                        label = { Text("🥩 Üreticiler", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = state.selectedRole == "market",
                        onClick = { viewModel.setRoleFilter("market") },
                        label = { Text("🛒 Marketler", fontSize = 12.sp) }
                    )
                }

                // Durum Filtresi (Tümü / Aktif / Banlı)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = state.selectedStatus == "all",
                        onClick = { viewModel.setStatusFilter("all") },
                        label = { Text("Tüm Durumlar", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = state.selectedStatus == "active",
                        onClick = { viewModel.setStatusFilter("active") },
                        label = { Text("✓ Aktifler", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = state.selectedStatus == "banned",
                        onClick = { viewModel.setStatusFilter("banned") },
                        label = { Text("⛔ Banlılar", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Kullanıcı Listesi
            if (state.isLoading && state.users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (state.users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🔍", fontSize = 36.sp)
                        Text(
                            "Arama kriterine uygun kullanıcı bulunamadı.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.users, key = { it.id }) { userItem ->
                        AdminUserCard(
                            user = userItem,
                            onBanClick = { selectedUserForBan = userItem },
                            onUnbanClick = { selectedUserForUnban = userItem },
                            onMessageClick = { selectedUserForMessage = userItem }
                        )
                    }
                }
            }
        }
    }

    // Ban Dialog
    selectedUserForBan?.let { user ->
        BanUserReasonDialog(
            user = user,
            isLoading = state.isActionInProgress,
            onDismiss = { selectedUserForBan = null },
            onConfirm = { reason ->
                viewModel.banUser(user.id, true, reason) {
                    selectedUserForBan = null
                }
            }
        )
    }

    // Unban Dialog
    selectedUserForUnban?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUserForUnban = null },
            title = { Text("Banı Kaldır", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "${user.name} (${user.email}) adlı kullanıcının erişim engelini kaldırmak istiyor musunuz? Kullanıcı tekrar sisteme giriş yapabilecektir."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.banUser(user.id, false) {
                            selectedUserForUnban = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Evet, Banı Kaldır")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedUserForUnban = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Tekil Kullanıcıya Mesaj Dialog
    selectedUserForMessage?.let { user ->
        SendAdminMessageDialog(
            targetTitle = "${user.name} (${user.roleDisplayTitle})",
            isLoading = state.isActionInProgress,
            onDismiss = { selectedUserForMessage = null },
            onSend = { title, message ->
                viewModel.sendNotification(userId = user.id, title = title, message = message) {
                    selectedUserForMessage = null
                }
            }
        )
    }

    // Toplu Bildirim / Duyuru Dialog
    if (showBroadcastDialog) {
        SendBroadcastNotificationDialog(
            isLoading = state.isActionInProgress,
            onDismiss = { showBroadcastDialog = false },
            onSend = { targetRole, title, message ->
                viewModel.sendNotification(
                    role = if (targetRole == "all") null else targetRole,
                    title = title,
                    message = message
                ) {
                    showBroadcastDialog = false
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(title: String, count: String, color: Color, emoji: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.width(105.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 16.sp)
                Text(count, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun AdminUserCard(
    user: AdminUserItem,
    onBanClick: () -> Unit,
    onUnbanClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    val isBanned = user.isBanned
    val isAdmin = user.role == "admin"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBanned) Color(0xFFFFF1F2) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Başlık & Avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when (user.role) {
                        "hayvanci" -> ForestGreen.copy(alpha = 0.15f)
                        "sucukcu" -> PrimaryBlue.copy(alpha = 0.15f)
                        "market" -> OrangeMarket.copy(alpha = 0.15f)
                        else -> Color(0xFF6B21A8).copy(alpha = 0.15f)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(user.roleEmoji, fontSize = 20.sp)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (isAdmin) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF6B21A8)
                            ) {
                                Text(
                                    "Yönetici",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(user.email, fontSize = 12.sp, color = Color.Gray)
                    user.phone?.let {
                        Text("📞 $it", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }

                // Durum Rozeti
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isBanned) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = if (isBanned) "⛔ BANLI" else "✓ Aktif",
                        color = if (isBanned) Color(0xFFB91C1C) else Color(0xFF15803D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Rol ve Faaliyet Rozetleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        user.roleDisplayTitle,
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (user.lot_count > 0) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEFF6FF)) {
                        Text(
                            "📦 ${user.lot_count} Lot",
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (user.order_count > 0) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0FDF4)) {
                        Text(
                            "📝 ${user.order_count} Sipariş",
                            fontSize = 11.sp,
                            color = ForestGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (user.purchase_count > 0) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFF7ED)) {
                        Text(
                            "🛒 ${user.purchase_count} Satın Alım",
                            fontSize = 11.sp,
                            color = OrangeMarket,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Ban Gerekçesi (Varsa)
            if (isBanned) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⚠️", fontSize = 13.sp)
                        Column {
                            Text("Ban Gerekçesi:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF991B1B))
                            Text(
                                user.ban_reason ?: "Yönetici tarafından askıya alındı",
                                fontSize = 12.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Eylem Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mesaj Gönder Butonu
                OutlinedButton(
                    onClick = onMessageClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mesaj At", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Ban / Banı Kaldır Butonları
                if (!isAdmin) {
                    if (isBanned) {
                        Button(
                            onClick = onUnbanClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Banı Kaldır", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onBanClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Banla", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BanUserReasonDialog(
    user: AdminUserItem,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val quickReasons = listOf(
        "Kural ihlali",
        "Geri çağırma kurallarına uymama",
        "Şüpheli ürün ve parti bildirimi",
        "E-Fatura usulsüzlüğü",
        "Yetkisiz işlem / hesap güvenliği"
    )

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("⛔")
                Text("Kullanıcıyı Banla", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${user.name} kullanıcısının sisteme erişimi tamamen engellenecektir. Aktif oturumları sonlandırılır.",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Text("Hızlı Gerekçe Seçin:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickReasons.forEach { r ->
                        SuggestionChip(
                            onClick = { reason = r },
                            label = { Text(r, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Ban Gerekçesi (Zorunlu)") },
                    placeholder = { Text("Kullanıcıya gösterilecek sebebi yazın...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = !isLoading && reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Kullanıcıyı Askıya Al / Banla")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isLoading) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun SendAdminMessageDialog(
    targetTitle: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSend: (title: String, message: String) -> Unit
) {
    var title by remember { mutableStateOf("Yönetici Bildirimi") }
    var message by remember { mutableStateOf("") }

    val quickTemplates = listOf(
        "Geri Çağırma Denetimi" to "Lütfen son sevkiyat ve depo lotlarınızı acilen denetleyin.",
        "Fatura Hatırlatması" to "Sistemde bekleyen faturalandırılmamış işlemleriniz mevcuttur. Lütfen tamamlayınız.",
        "Kural Hatırlatması" to "Platform ticaret ve hijyen standartlarına uygun hareket etmeniz rica olunur."
    )

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("✉️")
                Text("Mesaj / Bildirim Gönder", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Alıcı: $targetTitle",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Text("Hızlı Şablonlar:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickTemplates.forEach { (tmplTitle, tmplMsg) ->
                        SuggestionChip(
                            onClick = {
                                title = tmplTitle
                                message = tmplMsg
                            },
                            label = { Text(tmplTitle, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bildirim Başlığı") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Mesaj İçeriği") },
                    placeholder = { Text("Kullanıcı bildirim kutusuna düşecek mesaj...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(title, message) },
                enabled = !isLoading && title.isNotBlank() && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Gönder")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Vazgeç")
            }
        }
    )
}

@Composable
fun SendBroadcastNotificationDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSend: (targetRole: String, title: String, message: String) -> Unit
) {
    var selectedTargetRole by remember { mutableStateOf("all") }
    var title by remember { mutableStateOf("📢 AVATAR Sistem Duyurusu") }
    var message by remember { mutableStateOf("") }

    val quickTemplates = listOf(
        "Sistem Bakımı" to "Platformumuzda bu gece 02:00 - 04:00 saatleri arasında sunucu bakım çalışması gerçekleştirilecektir.",
        "Mevzuat Güncellemesi" to "Yeni gıda güvenliği ve geri çağırma regülasyonlarına uygun olarak parti kodları zorunlu hale getirilmiştir.",
        "Önemli Hatırlatma" to "Tüm tarafların e-arşiv ve e-fatura bilgilerini eksiksiz düzenlemesi rica olunur."
    )

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📢")
                Text("Toplu Duyuru & Bildirim", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Hedef Kitle:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTargetRole == "all",
                        onClick = { selectedTargetRole = "all" },
                        label = { Text("Herkes (Tüm Kullanıcılar)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedTargetRole == "hayvanci",
                        onClick = { selectedTargetRole = "hayvanci" },
                        label = { Text("🐑 İşletmeler", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedTargetRole == "sucukcu",
                        onClick = { selectedTargetRole = "sucukcu" },
                        label = { Text("🥩 Üreticiler", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedTargetRole == "market",
                        onClick = { selectedTargetRole = "market" },
                        label = { Text("🛒 Marketler", fontSize = 11.sp) }
                    )
                }

                Text("Hızlı Şablonlar:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickTemplates.forEach { (tmplTitle, tmplMsg) ->
                        SuggestionChip(
                            onClick = {
                                title = "📢 $tmplTitle"
                                message = tmplMsg
                            },
                            label = { Text(tmplTitle, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Duyuru Başlığı") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Duyuru Mesajı") },
                    placeholder = { Text("Seçili hedef kitledeki tüm kullanıcıların bildirim kutusuna gidecektir...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(selectedTargetRole, title, message) },
                enabled = !isLoading && title.isNotBlank() && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Toplu İlet")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Vazgeç")
            }
        }
    )
}
