package co.ke.maawebhost.invest.screens.save

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
fun CreateGoalScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        ScreenHeader("Create Goal", subtitle = "Let's achieve your target", onBack = onDone)
        MaaTextField("Goal Name", name, { name = it }, placeholder = "e.g. Emergency Fund")
        MaaTextField(
            "Target Amount", target, { target = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = "KES 0.00", keyboardType = KeyboardType.Decimal
        )
        MaaTextField("Target Date", deadline, { deadline = it }, placeholder = "YYYY-MM-DD")

        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            "Create Goal",
            loading = loading,
            onClick = {
                val amt = target.toDoubleOrNull()
                if (name.isBlank() || amt == null || amt <= 0) {
                    Toast.makeText(context, "Give your goal a name and a target amount.", Toast.LENGTH_SHORT).show()
                    return@PrimaryButton
                }
                loading = true
                scope.launch {
                    try {
                        val body = mutableMapOf("name" to name, "target_amount" to amt.toString())
                        if (deadline.isNotBlank()) body["deadline"] = deadline
                        apiCall { apiService.createGoal(body) }
                        Toast.makeText(context, "Goal created — fund it any time from Save.", Toast.LENGTH_LONG).show()
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
