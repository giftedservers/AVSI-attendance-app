package co.ke.maawebhost.invest.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.components.ScreenHeader
import co.ke.maawebhost.invest.data.Session
import co.ke.maawebhost.invest.ui.theme.*
import co.ke.maawebhost.invest.util.initials

private data class ProfileRow(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val kyc: Boolean = false)

private val ROWS = listOf(
    ProfileRow(Icons.Filled.Person, "Personal Information"),
    ProfileRow(Icons.Filled.VerifiedUser, "KYC Verification", kyc = true),
    ProfileRow(Icons.Filled.Lock, "Security"),
    ProfileRow(Icons.Filled.AccountBalance, "Bank Accounts"),
    ProfileRow(Icons.Filled.Notifications, "Notification Settings"),
    ProfileRow(Icons.Filled.Help, "Help & Support"),
)

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val user by Session.user

    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(20.dp)) {
        ScreenHeader("Profile", onBack = onBack)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
            Box(
                modifier = Modifier.size(56.dp).background(Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(initials(user?.full_name ?: "U"), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(user?.full_name ?: "", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("${user?.phone ?: ""} · ${user?.email ?: ""}", fontSize = 12.5.sp, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp)
        ) {
            ROWS.forEach { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Icon(row.icon, contentDescription = null, tint = TextMuted)
                    Spacer(Modifier.width(14.dp))
                    Text(row.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (row.kyc) {
                        Text(
                            user?.kyc_status ?: "pending",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (user?.kyc_status == "verified") Primary else Gold
                        )
                    } else {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextFaint)
                    }
                }
            }
        }
    }
}
