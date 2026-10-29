package com.avatar.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avatar.app.data.local.SessionManager

@Composable
fun ServerConfigDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var inputUrl by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lan, contentDescription = "Server")
                Text("Backend Bağlantı Ayarı", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Telefonunuzun bilgisayardaki Docker servisine erişebileceği IP adresi:",
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("API Adresi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Hızlı Seçenekler:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { inputUrl = SessionManager.DEFAULT_SERVER_URL },
                        label = { Text("Wi-Fi (192.168.18.5)", fontSize = 11.sp) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { inputUrl = SessionManager.USB_REVERSE_URL },
                        label = { Text("USB Reverse (127.0.0.1)", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { inputUrl = SessionManager.EMULATOR_URL },
                        label = { Text("Emülatör (10.0.2.2)", fontSize = 11.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(inputUrl) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
