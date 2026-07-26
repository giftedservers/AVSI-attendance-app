package co.ke.maawebhost.invest.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun RegisterScreen(onRegistered: () -> Unit, onGoLogin: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var identity by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        ScreenHeader("Create Account", subtitle = "Let's get started")
        MaaTextField("Full Name", fullName, { fullName = it }, placeholder = "Enter your full name")
        MaaTextField("Email or Phone Number", identity, { identity = it }, placeholder = "Enter email or phone number")
        MaaTextField("Password", password, { password = it }, placeholder = "Create a password", isPassword = true)
        MaaTextField("Confirm Password", confirm, { confirm = it }, placeholder = "Confirm your password", isPassword = true)

        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 8.dp)) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(checkedColor = Primary)
            )
            Text(
                "I agree to the Terms & Conditions and Privacy Policy",
                fontSize = 12.5.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        PrimaryButton(
            "Create Account",
            loading = loading,
            onClick = {
                if (fullName.isBlank() || identity.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                    return@PrimaryButton
                }
                if (password != confirm) {
                    Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                    return@PrimaryButton
                }
                if (!agreed) {
                    Toast.makeText(context, "Please agree to the Terms & Conditions.", Toast.LENGTH_SHORT).show()
                    return@PrimaryButton
                }
                val isEmail = identity.contains("@")
                val email = if (isEmail) identity.trim() else "${identity.filter { it.isDigit() }}@maainvest.africa"
                val phone = if (isEmail) "" else identity.trim()

                loading = true
                scope.launch {
                    try {
                        Session.register(fullName.trim(), email, phone, password)
                        onRegistered()
                    } catch (e: ApiException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", fontSize = 13.sp, color = TextMuted)
            Text(
                "Log In",
                fontSize = 13.sp,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onGoLogin)
            )
        }
    }
}
