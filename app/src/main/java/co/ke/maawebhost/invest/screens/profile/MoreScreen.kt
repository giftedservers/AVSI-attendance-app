package co.ke.maawebhost.invest.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.data.Session
import co.ke.maawebhost.invest.ui.theme.*
import kotlinx.coroutines.launch

private data class MoreRow(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val sub: String)

private val ROWS = listOf(
    MoreRow(Icons.Filled.MilitaryTech, "Become a Treasurer", "For your chama"),
    MoreRow(Icons.Filled.PeopleAlt, "Refer & Earn", "Invite friends and earn"),
    MoreRow(Icons.Filled.SupportAgent, "Help Center", "Get support"),
    MoreRow(Icons.Filled.Star, "Rate Our App", "We value your feedback"),
    MoreRow(Icons.Filled.Info, "About MAA INVEST", "Learn more about us"),
)

@Composable
fun MoreScreen(onProfile: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(20.dp)) {
        Text("More", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp).clickable(onClick = onProfile)
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = TextMuted)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("My Profile", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Account details & settings", fontSize = 11.5.sp, color = TextMuted)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextFaint)
            }
            ROWS.forEach { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp).clickable {
                        Toast.makeText(context, "Coming soon.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(row.icon, contentDescription = null, tint = TextMuted)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(row.sub, fontSize = 11.5.sp, color = TextMuted)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextFaint)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .background(PinkSoft, RoundedCornerShape(18.dp))
                .clickable { confirmLogout = true }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Danger)
            Spacer(Modifier.width(8.dp))
            Text("Log Out", color = Danger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    scope.launch { Session.logout() }
                }) { Text("Log Out", color = Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel") }
            }
        )
    }
}
