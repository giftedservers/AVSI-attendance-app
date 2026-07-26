package co.ke.maawebhost.invest.screens.groups

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.ke.maawebhost.invest.components.MaaTextField
import co.ke.maawebhost.invest.components.PrimaryButton
import co.ke.maawebhost.invest.components.ScreenHeader
import co.ke.maawebhost.invest.data.api.ApiException
import co.ke.maawebhost.invest.data.api.apiCall
import co.ke.maawebhost.invest.data.api.apiService
import co.ke.maawebhost.invest.ui.theme.Bg
import kotlinx.coroutines.launch

@Composable
fun JoinGroupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(20.dp)) {
        ScreenHeader("Join a Group", subtitle = "Enter the invite code shared with you", onBack = onDone)
        MaaTextField("Invite Code", code, { code = it }, placeholder = "e.g. CHAMA-7F2K")
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            "Join Group",
            loading = loading,
            onClick = {
                if (code.isBlank()) {
                    Toast.makeText(context, "Enter the group invite code.", Toast.LENGTH_SHORT).show()
                    return@PrimaryButton
                }
                loading = true
                scope.launch {
                    try {
                        val res = apiCall { apiService.joinGroup(mapOf("invite_code" to code.trim())) }
                        Toast.makeText(context, "You're now a member of \"${res.group.name}\".", Toast.LENGTH_LONG).show()
                        onDone()
                    } catch (e: ApiException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    } finally {
                        loading = false
                    }
                }
            }
        )
    }
}
