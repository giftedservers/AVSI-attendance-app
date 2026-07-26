package co.ke.maawebhost.invest.screens.groups

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
import androidx.compose.material.icons.filled.Key
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
import co.ke.maawebhost.invest.data.api.apiCall
import co.ke.maawebhost.invest.data.api.apiService
import co.ke.maawebhost.invest.data.api.dto.Group
import co.ke.maawebhost.invest.ui.theme.*
import co.ke.maawebhost.invest.util.formatMoney
import co.ke.maawebhost.invest.util.initials
import kotlinx.coroutines.launch

@Composable
fun GroupsScreen(onOpenGroup: (Int) -> Unit, onJoinByCode: () -> Unit) {
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf(listOf<Group>()) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                groups = apiCall { apiService.listGroups() }.groups
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
        Text("Join a Group", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("Discover & join savings groups", fontSize = 13.sp, color = TextMuted)

        Row(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .clickable(onClick = onJoinByCode),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Key, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Have an invite code? Join directly", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        }

        if (groups.isEmpty()) {
            MaaCard { Text("No groups yet. Join one with an invite code.", color = TextMuted, fontSize = 13.sp) }
        } else {
            groups.forEach { g ->
                MaaCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenGroup(g.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(46.dp).background(PrimaryLight, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials(g.name), color = Primary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(g.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text("${g.member_count} members · ${g.my_role ?: "member"}", fontSize = 12.sp, color = TextMuted)
                            Text("Saved ${formatMoney(g.saved_amount)}", fontSize = 12.5.sp, color = Primary, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier.background(PrimaryLight, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Open", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
