package co.ke.maawebhost.invest.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.data.TokenStore
import co.ke.maawebhost.invest.ui.theme.Border
import co.ke.maawebhost.invest.ui.theme.Primary
import co.ke.maawebhost.invest.ui.theme.TextMuted
import co.ke.maawebhost.invest.ui.theme.TextPrimary

private val KEYS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")

@Composable
fun PinScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val existingPin = remember { TokenStore.getPin() }
    val isSetup = existingPin == null
    var pin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp, vertical = 20.dp)) {
        Text(
            if (isSetup) "Set up your PIN" else "Enter your PIN",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            if (isSetup) "Secure your account" else "Welcome back",
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 56.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp)
                        .border(1.5.dp, if (i < pin.length) Primary else Border, CircleShape)
                        .background(if (i < pin.length) Primary else Color.Transparent, CircleShape)
                )
            }
        }

        Spacer(Modifier.height(56.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            KEYS.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.4f)
                                .clickable(enabled = key.isNotEmpty()) {
                                    when (key) {
                                        "del" -> pin = pin.dropLast(1)
                                        "" -> {}
                                        else -> {
                                            if (pin.length < 4) pin += key
                                            if (pin.length == 4) {
                                                if (isSetup) {
                                                    TokenStore.setPin(pin)
                                                    onUnlocked()
                                                } else if (pin == existingPin) {
                                                    onUnlocked()
                                                } else {
                                                    Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                                                    pin = ""
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "del") {
                                Icon(Icons.Filled.Backspace, contentDescription = "Delete", tint = TextMuted)
                            } else if (key.isNotEmpty()) {
                                Text(key, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
