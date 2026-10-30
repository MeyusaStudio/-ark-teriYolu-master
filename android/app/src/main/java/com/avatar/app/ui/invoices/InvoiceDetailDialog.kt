package com.avatar.app.ui.invoices

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avatar.app.data.model.Invoice
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailDialog(
    invoice: Invoice,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isHizmet = invoice.invoiceType == "hizmet"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(6.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    if (isHizmet) "🛠️ Fason Hizmet Faturası" else "🛒 Mamul Satış Faturası",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(invoice.invoiceNumber, fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat")
                            }
                        },
                        actions = {
                            IconButton(onClick = { shareInvoice(context, invoice) }) {
                                Icon(Icons.Default.Share, contentDescription = "Faturayı Paylaş")
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                Text("Kapat")
                            }
                            Button(
                                onClick = { shareInvoice(context, invoice) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.weight(1.8f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Faturayı İndir / Paylaş", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // E-Fatura Resmi Başlık Şablonu
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text("AVATAR E-FATURA", fontWeight = FontWeight.Black, fontSize = 17.sp, color = PrimaryBlue)
                                        Text("Gıda Üretim & İzlenebilirlik Platformu", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            "✓ RESMİ E-ARŞİV",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Fatura No:", fontSize = 11.sp, color = Color.Gray)
                                        Text(invoice.invoiceNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Fatura Tarihi:", fontSize = 11.sp, color = Color.Gray)
                                        Text(invoice.invoiceDate.take(10), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Taraflar Kartı (Gönderen vs Alıcı)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Gönderen
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DÜZENLEYEN (SATICI)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(invoice.senderName ?: "Satıcı", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Rol: ${invoice.senderRole ?: "Firma"}", fontSize = 11.sp, color = Color.DarkGray)
                                    if (!invoice.senderEmail.isNullOrBlank()) {
                                        Text(invoice.senderEmail, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }

                                VerticalDivider(modifier = Modifier.height(70.dp), color = Color(0xFFE2E8F0))

                                // Alıcı
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ALICI (MÜŞTERİ)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(invoice.receiverName ?: "Alıcı", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Rol: ${invoice.receiverRole ?: "Müşteri"}", fontSize = 11.sp, color = Color.DarkGray)
                                    if (!invoice.receiverEmail.isNullOrBlank()) {
                                        Text(invoice.receiverEmail, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    // Kalem Detayı
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("FATURA KALEMLERİ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            invoice.productName ?: if (isHizmet) "Fason Üretim Hizmet Bedeli" else "Mamul Ürün Satışı",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (!invoice.lotNumber.isNullOrBlank()) {
                                            Text("Parti / Lot: ${invoice.lotNumber}", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                                        }
                                        if (!invoice.description.isNullOrBlank()) {
                                            Text(invoice.description, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${String.format(Locale.US, "%.2f", invoice.totalAmount)} ${invoice.currency}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ForestGreen
                                        )
                                        Text(
                                            "${invoice.quantity} ${invoice.unit} × ${String.format(Locale.US, "%.2f", invoice.unitPrice)}",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tutar Özeti & KDV
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ara Toplam:", fontSize = 12.sp, color = Color.DarkGray)
                                    Text("${String.format(Locale.US, "%.2f", invoice.subtotal)} ${invoice.currency}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("KDV (%${invoice.taxRate.toInt()}):", fontSize = 12.sp, color = Color.DarkGray)
                                    Text("${String.format(Locale.US, "%.2f", invoice.taxAmount)} ${invoice.currency}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                HorizontalDivider(color = Color(0xFFCBD5E1))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("GENEL TOPLAM:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${String.format(Locale.US, "%.2f", invoice.totalAmount)} ${invoice.currency}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
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
}

// Faturayı metin/HTML formatında paylaşma & PDF dışa aktarma fonksiyonu
fun shareInvoice(context: Context, invoice: Invoice) {
    val text = """
        ==================================================
                      AVATAR E-FATURA
        ==================================================
        Fatura No: ${invoice.invoiceNumber}
        Tarih: ${invoice.invoiceDate.take(10)}
        Tür: ${if (invoice.invoiceType == "hizmet") "Fason Hizmet Bedeli" else "Mamul Satış Faturası"}
        Durum: ${invoice.status.uppercase()}
        --------------------------------------------------
        DÜZENLEYEN:
        ${invoice.senderName ?: "Satıcı"} (${invoice.senderRole ?: "Firma"})
        ${invoice.senderEmail ?: ""}

        ALICI:
        ${invoice.receiverName ?: "Alıcı"} (${invoice.receiverRole ?: "Müşteri"})
        ${invoice.receiverEmail ?: ""}
        --------------------------------------------------
        ÜRÜN / HİZMET:
        ${invoice.productName ?: "Mamul / Fason Hizmet"}
        Parti / Lot: ${invoice.lotNumber ?: "-"}
        Miktar: ${invoice.quantity} ${invoice.unit}
        Birim Fiyat: ${String.format(Locale.US, "%.2f", invoice.unitPrice)} ${invoice.currency}
        --------------------------------------------------
        Ara Toplam: ${String.format(Locale.US, "%.2f", invoice.subtotal)} ${invoice.currency}
        KDV (%${invoice.taxRate.toInt()}): ${String.format(Locale.US, "%.2f", invoice.taxAmount)} ${invoice.currency}
        GENEL TOPLAM: ${String.format(Locale.US, "%.2f", invoice.totalAmount)} ${invoice.currency}
        ==================================================
        Bu belge AVATAR Gıda İzlenebilirlik Platformu
        üzerinden elektronik olarak oluşturulmuştur.
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "AVATAR E-Fatura: ${invoice.invoiceNumber}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Faturayı İndir / Paylaş"))
}
