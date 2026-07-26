package co.ke.maawebhost.invest.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.components.MaaCard
import co.ke.maawebhost.invest.data.Session
import co.ke.maawebhost.invest.data.api.apiCall
import co.ke.maawebhost.invest.data.api.apiService
import co.ke.maawebhost.invest.data.api.dto.DashboardResponse
import co.ke.maawebhost.invest.ui.theme.*
import co.ke.maawebhost.invest.util.formatMoney
import co.ke.maawebhost.invest.util.initials
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onAddMoney: () -> Unit,
    onSave: () -> Unit,
    onWithdraw: () -> Unit,
    onProfile: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dashboard by remember { mutableStateOf<DashboardResponse?>(null) }
    val user by Session.user

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                dashboard = apiCall { apiService.dashboard() }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Good morning,", fontSize = 13.sp, color = TextMuted)
                Text(
                    "${user?.full_name?.split(" ")?.firstOrNull() ?: "there"} 👋",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Primary, CircleShape)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center
            ) {
                Text(initials(user?.full_name ?: "U"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(18.dp))

        val portfolioTotal = (dashboard?.total_saved ?: 0.0) + (dashboard?.investment_balance ?: 0.0)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Total Portfolio Value", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(formatMoney(portfolioTotal), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Leaf, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Wallet: ${formatMoney(dashboard?.wallet_balance ?: 0.0)}", color = Leaf, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Active Goals", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))

        val goals = dashboard?.goals.orEmpty()
        if (goals.isEmpty()) {
            MaaCard { Text("No active goals yet. Head to Save to create one.", color = TextMuted, fontSize = 13.sp) }
        } else {
            goals.take(3).forEach { g ->
                MaaCard(modifier = Modifier.padding(bottom = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(g.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "${formatMoney(g.saved_amount)} / ${formatMoney(g.target_amount)}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickAction(Icons.Filled.AddCircle, "Add Money", onAddMoney)
            QuickAction(Icons.Filled.Bookmark, "Save", onSave)
            QuickAction(Icons.Filled.SwapVert, "Withdraw", onWithdraw)
            QuickAction(Icons.Filled.GridView, "More", onProfile)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(PrimaryLight, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Primary)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}
