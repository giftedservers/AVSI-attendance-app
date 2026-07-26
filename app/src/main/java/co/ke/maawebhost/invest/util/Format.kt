package co.ke.maawebhost.invest.util

import java.text.NumberFormat
import java.util.Locale

fun formatMoney(value: String?, currency: String = "KES"): String {
    val n = value?.toDoubleOrNull() ?: 0.0
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$currency ${nf.format(n)}"
}

fun formatMoney(value: Double, currency: String = "KES"): String = formatMoney(value.toString(), currency)

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

fun formatDate(raw: String): String = raw.substringBefore(" ").ifBlank { raw }
