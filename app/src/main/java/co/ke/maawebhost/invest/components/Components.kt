package co.ke.maawebhost.invest.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.ke.maawebhost.invest.ui.theme.*

@Composable
fun MaaCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(18.dp))
            .border(1.dp, Border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    outline: Boolean = false,
    enabled: Boolean = true,
) {
    if (outline) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary)
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
            else Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun MaaTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
) {
    var visible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(!isPassword) }
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextFaint) },
            singleLine = true,
            visualTransformation = if (isPassword && !visible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (isPassword) androidx.compose.ui.text.input.KeyboardType.Password else keyboardType),
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Border,
                focusedBorderColor = Primary,
                unfocusedContainerColor = CardColor,
                focusedContainerColor = CardColor,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(CardColor, RoundedCornerShape(10.dp))
                    .border(1.dp, Border, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = TextMuted)
        }
    }
}

@Composable
fun ProgressBar(progress: Float, color: Color = Primary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Border, RoundedCornerShape(50))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, RoundedCornerShape(50))
        )
    }
}

@Composable
fun IconChip(bg: Color, size: androidx.compose.ui.unit.Dp = 40.dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(size).background(bg, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) { content() }
}
