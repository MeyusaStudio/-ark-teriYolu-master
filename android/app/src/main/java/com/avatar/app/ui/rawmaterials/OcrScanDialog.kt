package com.avatar.app.ui.rawmaterials

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avatar.app.data.model.OcrAnalyzeRequest
import com.avatar.app.data.model.OcrExtractionResult
import com.avatar.app.data.remote.ApiClient
import com.avatar.app.ui.theme.ForestGreen
import com.avatar.app.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScanDialog(
    apiClient: ApiClient,
    onDismiss: () -> Unit,
    onApplyToForm: (result: OcrExtractionResult) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedSample by remember { mutableStateOf("sample_meat") }
    var customText by remember { mutableStateOf("") }
    var isCustomTextMode by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var ocrResult by remember { mutableStateOf<OcrExtractionResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun runOcr(sampleId: String?, rawText: String?) {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val req = OcrAnalyzeRequest(sampleId = sampleId, rawText = rawText)
                val resp = apiClient.ocrService.analyze(req)
                if (resp.isSuccessful && resp.body()?.data != null) {
                    ocrResult = resp.body()!!.data
                } else {
                    errorMessage = "Belge OCR motoru ile okunamadı."
                }
            } catch (e: Exception) {
                errorMessage = "Bağlantı hatası: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    // İlk açılışta et irsaliyesi örneğini analiz et
    LaunchedEffect(Unit) {
        runOcr("sample_meat", null)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(6.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fatura & İrsaliye OCR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Text("Yapay zeka ile belge ayrıştırma & aktarma", fontSize = 11.sp, color = Color.Gray)
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
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                Text("Vazgeç")
                            }
                            Button(
                                onClick = {
                                    ocrResult?.let {
                                        onApplyToForm(it)
                                        onDismiss()
                                    }
                                },
                                enabled = ocrResult != null,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.weight(1.8f)
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hammaddeye Aktar", fontWeight = FontWeight.Bold)
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
                    // Bilgilendirme Notu
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "✨ Akıllı OCR Motoru",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B21A8)
                                )
                                Text(
                                    "Tedarikçi firma, belge no, parti lotu, miktar ve birim fiyat fatura metninden otomatik ayrıştırılır.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF7E22CE)
                                )
                            }
                        }
                    }

                    // Şablon Seçici
                    item {
                        Text("Örnek Belge Seçin veya Metin Girin:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedSample = "sample_meat"
                                    isCustomTextMode = false
                                    runOcr("sample_meat", null)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!isCustomTextMode && selectedSample == "sample_meat") Color(0xFFEFF6FF) else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text("🥩 Et İrsaliyesi", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedSample = "sample_spice"
                                    isCustomTextMode = false
                                    runOcr("sample_spice", null)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!isCustomTextMode && selectedSample == "sample_spice") Color(0xFFEFF6FF) else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text("🧂 Baharat Ftr.", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedSample = "sample_casing"
                                    isCustomTextMode = false
                                    runOcr("sample_casing", null)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!isCustomTextMode && selectedSample == "sample_casing") Color(0xFFEFF6FF) else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text("🌭 Bağırsak", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Özel Metin Girişi Toggle
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Özel Metin / Fatura Metni Yaz", fontSize = 12.sp, color = Color.Gray)
                            Switch(
                                checked = isCustomTextMode,
                                onCheckedChange = { isCustomTextMode = it }
                            )
                        }
                    }

                    if (isCustomTextMode) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = customText,
                                    onValueChange = { customText = it },
                                    placeholder = { Text("Fatura metnini buraya yapıştırın veya yazın...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6
                                )
                                Button(
                                    onClick = { runOcr(null, customText) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Metni OCR ile Ayrıştır", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Yükleniyor veya Sonuç
                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF7E22CE))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Belge Yapay Zeka Tarafından Ayrıştırılıyor...", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    } else if (errorMessage != null) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))) {
                                Text(errorMessage ?: "", color = Color(0xFFDC2626), modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                            }
                        }
                    } else if (ocrResult != null) {
                        val res = ocrResult!!
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("AYRIŞTIRILAN VERİLER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                "✓ %${String.format(Locale.US, "%.0f", res.confidence * 100)} Doğruluk",
                                                color = ForestGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Firma / Tedarikçi:", fontSize = 11.sp, color = Color.Gray)
                                        Text(res.supplierName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Belge / Fatura No:", fontSize = 11.sp, color = Color.Gray)
                                        Text(res.invoiceNumber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Malzeme Tanımı:", fontSize = 11.sp, color = Color.Gray)
                                        Text(res.materialLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Parti / Lot No:", fontSize = 11.sp, color = Color.Gray)
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF3E8FF)) {
                                            Text(res.lotNumber, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Miktar:", fontSize = 11.sp, color = Color.Gray)
                                        Text("${res.quantity} ${res.unit}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Birim Fiyat:", fontSize = 11.sp, color = Color.Gray)
                                        Text("${String.format(Locale.US, "%.2f", res.unitPrice)} ${res.currency ?: "EUR"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Toplam Tutar:", fontSize = 11.sp, color = Color.Gray)
                                        Text("${String.format(Locale.US, "%.2f", res.totalAmount)} ${res.currency ?: "EUR"}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                    }
                                }
                            }
                        }

                        // Ham Algılanan Metin Kartı
                        if (!res.detectedText.isNullOrBlank()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Belgeden Okunan Ham Metin:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            res.detectedText,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = Color.DarkGray
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
}
