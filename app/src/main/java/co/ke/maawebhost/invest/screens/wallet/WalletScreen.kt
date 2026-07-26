package co.ke.maawebhost.invest.screens.wallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.components.MaaCard
import co.ke.maawebhost.invest.components.ScreenHeader
import co.ke.maawebhost.invest.data.api.apiCall
import co.ke.maawebhost.invest.data.api.apiService
import co.ke.maawebhost.invest.data.api.dto.Transaction
import co.ke.maawebhost.invest.ui.theme.*
import co.ke.maawebhost.invest.util.formatMoney
import kotlinx.coroutines.launch

@Composable
fun WalletScreen(onAddMoney: () -> Unit, onWithdraw: () -> Unit, onHistory: () -> Unit) {
    val scope = rememberCoroutineScope()
    var balance by remember { mutableStateOf("0") }
    var txs by remember { mutableStateOf(listOf<Transaction>()) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                balance = apiCall { apiService.walletBalance() }.wallet.balance
                txs = apiCall { apiService.walletTransactions(1) }.transactions
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
        ScreenHeader("Wallet")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Purple, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Wallet Balance", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(formatMoney(balance), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WalletAction(Icons.Filled.Add, "Add Money", onAddMoney)
                WalletAction(Icons.Filled.ArrowUpward, "Withdraw", onWithdraw)
                WalletAction(Icons.Filled.History, "History", onHistory)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Recent Transactions", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))

        if (txs.isEmpty()) {
            MaaCard { Text("No transactions yet.", color = TextMuted, fontSize = 13.sp) }
        } else {
            txs.take(8).forEach { tx ->
                MaaCard(modifier = Modifier.padding(bottom = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.description ?: tx.type, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1)
                            Text(tx.created_at, fontSize = 11.5.sp, color = TextFaint)
                        }
                        val isCredit = tx.type == "deposit"
                        Text(
                            (if (isCredit) "+" else "-") + formatMoney(tx.amount).removePrefix("KES "),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (isCredit) Primary else Danger
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
