package co.ke.maawebhost.invest.screens.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun TransactionHistoryScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var txs by remember { mutableStateOf(listOf<Transaction>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                txs = apiCall { apiService.walletTransactions(1) }.transactions
            } catch (_: Exception) {
            } finally {
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(20.dp)) {
        ScreenHeader("Transaction History", onBack = onBack)

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (txs.isEmpty()) {
            MaaCard { Text("No transactions yet.", color = TextMuted, fontSize = 13.sp) }
        } else {
            LazyColumn {
                items(txs) { tx ->
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
}
