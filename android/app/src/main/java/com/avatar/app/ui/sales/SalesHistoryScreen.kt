package com.avatar.app.ui.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avatar.app.data.local.entity.SaleEntity
import com.avatar.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: SalesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Satış Geçmişi & Faturalar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Yerel SQLite (Room) Kayıtları",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = PrimaryBlue)
                    Column {
                        Text(
                            text = "Toplam Yapılan Satış: ${uiState.salesHistory.size} Adet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tüm satışlar yerel SQLite veritabanına işlenir ve fatura kesilir.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (uiState.salesHistory.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                        Text("Henüz mobil satış kaydı yok", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Satış & Lotlar sekmesinden bir et/ürün seçip satış yaptığınızda burada listelenecektir.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.salesHistory, key = { it.id }) { sale ->
                        SaleHistoryItem(sale = sale, dateFormat = dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
fun SaleHistoryItem(
    sale: SaleEntity,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sale.productName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = sale.invoiceNumber,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Alıcı / Müşteri:", fontSize = 12.sp, color = TextSecondary)
                Text(sale.customerName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Parti / Lot No:", fontSize = 12.sp, color = TextSecondary)
                Text(sale.lotNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Satış Miktarı:", fontSize = 12.sp, color = TextSecondary)
                Text("${"%.2f".format(sale.quantity)} ${sale.unit}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Toplam Tutar:", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = "${"%.2f".format(sale.totalPrice)} ${sale.currency}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SecondaryGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormat.format(Date(sale.timestamp)),
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
