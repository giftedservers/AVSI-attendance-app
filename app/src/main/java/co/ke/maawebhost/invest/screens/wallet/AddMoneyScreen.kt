package co.ke.maawebhost.invest.screens.wallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.components.PrimaryButton
import co.ke.maawebhost.invest.components.ScreenHeader
import co.ke.maawebhost.invest.data.api.ApiException
import co.ke.maawebhost.invest.data.api.apiCall
import co.ke.maawebhost.invest.data.api.apiService
import co.ke.maawebhost.invest.ui.theme.*
import kotlinx.coroutines.launch

private data class Method(val key: String, val label: String, val tag: String?, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val METHODS = listOf(
    Method("mpesa", "M-Pesa", "Recommended", Icons.Filled.PhoneAndroid),
    Method("bank", "Bank Transfer", null, Icons.Filled.AccountBalance),
    Method("card", "Debit / Credit Card", null, Icons.Filled.CreditCard),
)

@Composable
fun AddMoneyScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var method by remember { mutableStateOf("mpesa") }
    var amount by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        ScreenHeader("Add Money", subtitle = "Choose deposit method", onBack = onDone)

        METHODS.forEach { m ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(CardColor, RoundedCornerShape(18.dp))
                    .border(1.dp, Border, RoundedCornerShape(18.dp))
                    .clickable { method = m.key }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(m.let { PrimaryLight }, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(m.icon, contentDescription = null, tint = Primary) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(m.label, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    if (m.tag != null) Text(m.tag, fontSize = 11.sp, color = Primary, fontWeight = FontWeight.Bold)
                }
                RadioButton(selected = method == m.key, onClick = { method = m.key }, colors = RadioButtonDefaults.colors(selectedColor = Primary))
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Enter Amount", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = { Text("KES", fontWeight = FontWeight.Bold, color = TextFaint) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            "Continue",
            loading = loading,
            onClick = {
                val value = amount.toDoubleOrNull()
                if (value == null || value <= 0) {
                    Toast.makeText(context, "Enter a valid amount.", Toast.LENGTH_SHORT).show()
                    return@PrimaryButton
                }
                loading = true
                scope.launch {
                    try {
                        apiCall { apiService.deposit(mapOf("amount" to value.toString(), "channel" to method)) }
                        Toast.makeText(
                            context,
                            if (method == "mpesa") "Check your phone to complete the M-Pesa payment." else "Payment is being processed.",
                            Toast.LENGTH_LONG
                        ).show()
                        onDone()
                    } catch (e: ApiException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    } finally {
                        loading = false
                    }
                }
            }
        )
    }
}
