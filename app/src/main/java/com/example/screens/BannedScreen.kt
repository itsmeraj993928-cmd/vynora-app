package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shown instead of the app when an admin has banned the account. The user can send one appeal. */
@Composable
fun BannedScreen(isDarkMode: Boolean, onSubmitAppeal: (String, (Boolean) -> Unit) -> Unit, onLogout: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    Column(
        modifier = Modifier.fillMaxSize().background(if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your account is suspended", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = fg)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Your account broke the VYNORA Community Guidelines. If you think this is a mistake, you can send an appeal and an admin will review it.",
            fontSize = 14.sp, color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (sent) {
            Text("Appeal sent. We will review it. Please check back later.", color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
        } else {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("Why should we review your account?") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
            )
            if (failed) Text("Could not send. Check your internet and try again.", color = Color(0xFFEF4444), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = { failed = false; onSubmitAppeal(text) { ok -> if (ok) sent = true else failed = true } }, modifier = Modifier.fillMaxWidth()) {
                Text("Send appeal")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onLogout) { Text("Log out") }
    }
}
