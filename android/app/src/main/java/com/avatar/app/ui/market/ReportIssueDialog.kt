package com.avatar.app.ui.market

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avatar.app.data.model.MarketPurchaseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueDialog(
    purchase: MarketPurchaseItem,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (description: String) -> Unit
) {
    var issueText by remember { mutableStateOf("Ambalaj vakumu bozuk ve lezzet standart dışı") }

    val presetIssues = listOf(
        "Vakum kaçağı / Ambalaj hasarı",
        "Koku veya renk anomalisi",
        "Son kullanma tarihi okunaksız",
        "Miktar eksikliği"
    )

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
                    Text("⚠️ Kalite Sorunu Bildir", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFDC2626))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(purchase.productName ?: "Mamul Ürün", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Lot No: ${purchase.lotNumber} • Üretici: ${purchase.farmerName ?: "Hayvancı"}", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Text("Hızlı Sorun Şablonları:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presetIssues.take(2).forEach { p ->
                        OutlinedButton(
                            onClick = { issueText = p },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(p, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }

                OutlinedTextField(
                    value = issueText,
                    onValueChange = { issueText = it },
                    label = { Text("Sorun Açıklaması") },
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
                        onClick = { onSubmit(issueText) },
                        enabled = !isSubmitting && issueText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("İletiliyor...")
                        } else {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sorunu Bildir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
