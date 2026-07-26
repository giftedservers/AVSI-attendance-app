package co.ke.maawebhost.invest.data

import android.content.Context
import android.content.SharedPreferences

object TokenStore {
    private const val PREFS = "maa_invest_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_PIN = "device_pin"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun setToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun getPin(): String? = prefs.getString(KEY_PIN, null)

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }
}
