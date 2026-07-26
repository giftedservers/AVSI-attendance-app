package co.ke.maawebhost.invest.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.components.MaaTextField
import co.ke.maawebhost.invest.components.PrimaryButton
import co.ke.maawebhost.invest.components.ScreenHeader
import co.ke.maawebhost.invest.data.Session
import co.ke.maawebhost.invest.data.api.ApiException
import co.ke.maawebhost.invest.ui.theme.Primary
import co.ke.maawebhost.invest.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onGoRegister: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var identity by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        ScreenHeader("Welcome Back", subtitle = "Log in to your account")
        MaaTextField("Email or Phone Number", identity, { identity = it }, placeholder = "Enter email or phone number")
        MaaTextField("Password", password, { password = it }, placeholder = "Enter password", isPassword = true)

        PrimaryButton(
            "Log In",
            loading = loading,
            onClick = {
                if (identity.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Enter your email/phone and password.", Toast.LENGTH_SHORT).show()
                    return@PrimaryButton
                }
                loading = true
                scope.launch {
                    try {
                        Session.login(identity.trim(), password)
                        onLoggedIn()
                    } catch (e: ApiException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    } finally {
                        loading = false
                    }
                }
            }
        )

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Don't have an account? ", fontSize = 13.sp, color = TextMuted)
            Text(
                "Sign Up",
                fontSize = 13.sp,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onGoRegister)
            )
        }
    }
}
