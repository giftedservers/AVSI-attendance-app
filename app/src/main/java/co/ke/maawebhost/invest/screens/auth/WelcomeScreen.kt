package co.ke.maawebhost.invest.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.R
import co.ke.maawebhost.invest.components.PrimaryButton
import co.ke.maawebhost.invest.ui.theme.Primary
import co.ke.maawebhost.invest.ui.theme.TextMuted

@Composable
fun WelcomeScreen(onCreateAccount: () -> Unit, onLogin: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_full),
                contentDescription = "MAA INVEST",
                modifier = Modifier.width(240.dp)
            )
            Spacer(Modifier.height(40.dp))
            Text(
                "Smart Investing.\nSecure Future.",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Save, invest and grow your wealth with MAA INVEST.",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }

        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            PrimaryButton("Create Account", onClick = onCreateAccount)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Log In", onClick = onLogin, outline = true)
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Licensed & Regulated by CMA", fontSize = 12.sp, color = TextMuted)
            }
        }
    }
}
