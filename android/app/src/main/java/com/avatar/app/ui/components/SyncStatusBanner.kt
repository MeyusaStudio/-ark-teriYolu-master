package com.avatar.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avatar.app.data.sync.SyncUiState
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue

@Composable
fun SyncStatusBanner(
    syncState: SyncUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Durum Rozeti (Çevrimiçi / Çevrimdışı)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bağlantı Durumu
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (syncState.isOnline) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (syncState.isOnline) ForestGreen.copy(alpha = 0.4f) else Color(0xFFD97706).copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (syncState.isOnline) ForestGreen else Color(0xFFD97706),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (syncState.isOnline) "🟢 Çevrimiçi (MySQL Bağlı)" else "🟠 Çevrimdışı (SQLite Aktif)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (syncState.isOnline) Color(0xFF14532D) else Color(0xFF92400E)
                    )
                }
            }

            // Sağ Taraf: Eşitleme Durumu veya Bekleyen Sayısı
            if (syncState.isSyncing) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Otomatik Eşitleniyor...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
            } else if (syncState.pendingCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "💾 ${syncState.pendingCount} Bekleyen Kayıt",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            } else if (syncState.lastSyncResult != null && syncState.lastSyncResult.contains("✅")) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4)
                ) {
                    Text(
                        text = "✅ Senkronize Edildi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Bekleyen işlem bilgilendirme kartı (Tamamen otomatik, kullanıcı müdahalesi gerektirmez)
        AnimatedVisibility(
            visible = syncState.pendingCount > 0 || syncState.isSyncing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (syncState.isSyncing) Color(0xFFEFF6FF) else Color(0xFFFFFBEB),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (syncState.isSyncing) PrimaryBlue.copy(alpha = 0.3f) else Color(0xFFFDE68A)
                ),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (syncState.isSyncing) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (!syncState.isOnline) "📴" else "⏳",
                            fontSize = 18.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                syncState.isSyncing -> "Otomatik senkronizasyon yapılıyor..."
                                !syncState.isOnline -> "Çevrimdışı Mod (SQLite Aktif)"
                                else -> "İnternet bağlantısı algılandı"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (syncState.isSyncing) PrimaryBlue else Color(0xFF92400E)
                        )
                        Text(
                            text = when {
                                syncState.isSyncing -> "${syncState.pendingCount} işlem MySQL veritabanına aktarılıyor, lütfen bekleyin."
                                !syncState.isOnline -> "${syncState.pendingCount} işlem SQLite yerel veritabanında saklanıyor. İnternete bağlanınca otomatik eşitlenecektir."
                                else -> "Bekleyen ${syncState.pendingCount} işlem birazdan MySQL sunucusuna otomatik aktarılacak."
                            },
                            fontSize = 11.sp,
                            color = if (syncState.isSyncing) PrimaryBlue.copy(alpha = 0.8f) else Color(0xFFB45309),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
