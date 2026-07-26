package co.ke.maawebhost.invest.screens.save

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.components.MaaCard
import co.ke.maawebhost.invest.components.ProgressBar
import co.ke.maawebhost.invest.data.api.apiCall
import co.ke.maawebhost.invest.data.api.apiService
import co.ke.maawebhost.invest.data.api.dto.Goal
import co.ke.maawebhost.invest.ui.theme.*
import co.ke.maawebhost.invest.util.formatMoney
import kotlinx.coroutines.launch

@Composable
fun SaveScreen(onCreateGoal: () -> Unit) {
    val scope = rememberCoroutineScope()
    var goals by remember { mutableStateOf(listOf<Goal>()) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                goals = apiCall { apiService.listGoals() }.goals
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
                Text("Save", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("My Savings Goals", fontSize = 13.sp, color = TextMuted)
            }
            Row(
                modifier = Modifier
                    .background(PrimaryLight, RoundedCornerShape(50))
                    .clickable(onClick = onCreateGoal)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                Text(" New Goal", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        if (goals.isEmpty()) {
            MaaCard { Text("You haven't created a savings goal yet.", color = TextMuted, fontSize = 13.sp) }
        } else {
            goals.forEach { g ->
                val saved = g.saved_amount?.toDoubleOrNull() ?: 0.0
                val target = g.target_amount?.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
                MaaCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(g.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Text(
                            "${formatMoney(saved).removePrefix("KES ")} / ${formatMoney(target)}",
                            fontSize = 12.5.sp,
                            color = TextMuted
                        )
                    }
                    ProgressBar(progress = (saved / target).toFloat())
                    if (!g.deadline.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Target: ${g.deadline}", fontSize = 11.5.sp, color = TextFaint)
                    }
                }
            }
        }
    }
}
