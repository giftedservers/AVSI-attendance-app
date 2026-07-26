package co.ke.maawebhost.invest.screens.invest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.components.MaaCard
import co.ke.maawebhost.invest.data.api.ApiException
import co.ke.maawebhost.invest.data.api.apiCall
import co.ke.maawebhost.invest.data.api.apiService
import co.ke.maawebhost.invest.data.api.dto.InvestmentProduct
import co.ke.maawebhost.invest.ui.theme.*
import co.ke.maawebhost.invest.util.formatMoney
import kotlinx.coroutines.launch

@Composable
fun InvestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf(listOf<InvestmentProduct>()) }
    var selected by remember { mutableStateOf<InvestmentProduct?>(null) }
    var amount by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                products = apiCall { apiService.listProducts() }.products
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Invest", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("Explore investment products", fontSize = 13.sp, color = TextMuted)
        Spacer(Modifier.height(16.dp))

        if (products.isEmpty()) {
            MaaCard { Text("No products available right now.", color = TextMuted, fontSize = 13.sp) }
        } else {
            products.forEach { p ->
                val riskColor = when (p.risk_level) {
                    "high" -> Pink
                    "medium" -> Gold
                    else -> Primary
                }
                val riskBg = when (p.risk_level) {
                    "high" -> PinkSoft
                    "medium" -> GoldSoft
                    else -> PrimaryLight
                }
                MaaCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (p.locked) {
                                    Toast.makeText(context, "Upgrade your plan to unlock this.", Toast.LENGTH_SHORT).show()
                                } else {
                                    selected = p
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(riskBg, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = riskColor) }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text(
                                "${p.risk_level.replaceFirstChar { it.uppercase() }} Risk · From ${formatMoney(p.min_investment ?: "0")}",
                                fontSize = 12.sp, color = TextMuted
                            )
                        }
                        if (p.locked) Icon(Icons.Filled.Lock, contentDescription = null, tint = TextFaint)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("${p.annual_return_rate ?: "0"}% p.a.", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = riskColor)
                }
            }
        }
    }

    val product = selected
    if (product != null) {
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Invest in ${product.name}") },
            text = {
                Column {
                    Text(
                        "Minimum ${formatMoney(product.min_investment ?: "0")} · ${product.annual_return_rate}% p.a.",
                        fontSize = 12.5.sp, color = TextMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder = { Text("KES 0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = amount.toDoubleOrNull()
                    val min = product.min_investment?.toDoubleOrNull() ?: 0.0
                    if (value == null || value < min) {
                        Toast.makeText(context, "Minimum investment is ${formatMoney(min)}.", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    loading = true
                    scope.launch {
                        try {
                            apiCall { apiService.buyInvestment(mapOf("product_id" to product.id.toString(), "amount" to value.toString())) }
                            Toast.makeText(context, "Invested ${formatMoney(value)} in ${product.name}.", Toast.LENGTH_LONG).show()
                            selected = null
                            amount = ""
                        } catch (e: ApiException) {
                            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                        } finally {
                            loading = false
                        }
                    }
                }) { Text("Invest Now", color = Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("Cancel") }
            }
        )
    }
}
