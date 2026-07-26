package co.ke.maawebhost.invest.screens.groups

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.foundation.text.KeyboardOptions
import co.ke.maawebhost.invest.components.MaaCard
import co.ke.maawebhost.invest.components.PrimaryButton
import co.ke.maawebhost.invest.data.api.ApiException
import co.ke.maawebhost.invest.data.api.apiCall
import co.ke.maawebhost.invest.data.api.apiService
import co.ke.maawebhost.invest.data.api.dto.Group
import co.ke.maawebhost.invest.ui.theme.*
import co.ke.maawebhost.invest.util.formatMoney
import co.ke.maawebhost.invest.util.initials
import kotlinx.coroutines.launch

@Composable
fun GroupDetailScreen(groupId: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var group by remember { mutableStateOf<Group?>(null) }
    var showContribute by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            try {
                group = apiCall { apiService.listGroups() }.groups.find { it.id == groupId }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    val g = group
    if (g == null) {
        Column(modifier = Modifier.fillMaxSize().background(Bg).padding(20.dp)) {
            Text("Loading group…", color = TextMuted)
        }
        return
    }

    val saved = g.saved_amount.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(Primary, RoundedCornerShape(20.dp)).padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(initials(g.name), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(g.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text("${g.member_count} Members", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
                Icon(
                    Icons.Filled.Close, contentDescription = "Close", tint = Color.White,
                    modifier = Modifier.clickable(onClick = onClose)
                )
            }

            Spacer(Modifier.height(22.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Savings", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(formatMoney(saved), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text("My Share", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        formatMoney(if (g.member_count > 0) saved / g.member_count else 0.0),
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GroupAction(Icons.Filled.AddCircle, "Contribute") { showContribute = true }
                GroupAction(Icons.Filled.ArrowUpward, "Withdraw") {
                    Toast.makeText(context, "Withdrawals are coming soon.", Toast.LENGTH_SHORT).show()
                }
                GroupAction(Icons.Filled.AttachMoney, "Loan") {
                    Toast.makeText(context, "Group loans are coming soon.", Toast.LENGTH_SHORT).show()
                }
                GroupAction(Icons.Filled.People, "Members") {
                    Toast.makeText(context, "Member management is coming soon.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Recent Activity", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        MaaCard {
            Text(
                "Contribution history for this group isn't exposed by the API yet.",
                fontSize = 12.5.sp,
                color = TextMuted
            )
        }
    }

    if (showContribute) {
        AlertDialog(
            onDismissRequest = { showContribute = false },
            title = { Text("Contribute to ${g.name}") },
            text = {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text("KES 0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = amount.toDoubleOrNull()
                    if (value == null || value <= 0) {
                        Toast.makeText(context, "Enter a valid amount.", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    loading = true
                    scope.launch {
                        try {
                            apiCall { apiService.contributeToGroup(mapOf("group_id" to groupId.toString(), "amount" to value.toString())) }
                            Toast.makeText(context, "Contribution successful.", Toast.LENGTH_SHORT).show()
                            showContribute = false
                            amount = ""
                            load()
                        } catch (e: ApiException) {
                            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                        } finally {
                            loading = false
                        }
                    }
                }) { Text("Contribute", color = Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showContribute = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GroupAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = label, tint = Color.White) }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
