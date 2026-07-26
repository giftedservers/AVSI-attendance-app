package co.ke.maawebhost.invest.data

import androidx.compose.runtime.mutableStateOf
import co.ke.maawebhost.invest.data.api.ApiException
import co.ke.maawebhost.invest.data.api.apiCall
import co.ke.maawebhost.invest.data.api.apiService
import co.ke.maawebhost.invest.data.api.dto.ApiUser

object Session {
    val user = mutableStateOf<ApiUser?>(null)
    val isLoading = mutableStateOf(true)
    val pinUnlocked = mutableStateOf(false)

    suspend fun bootstrap() {
        val token = TokenStore.getToken()
        if (token != null) {
            try {
                user.value = apiCall { apiService.me() }.user
            } catch (_: ApiException) {
                TokenStore.clearToken()
            }
        }
        isLoading.value = false
    }

    suspend fun login(identity: String, password: String) {
        val res = apiCall { apiService.login(mapOf("identity" to identity, "password" to password, "device_label" to "MAA Invest Android")) }
        TokenStore.setToken(res.token)
        user.value = res.user
    }

    suspend fun register(fullName: String, email: String, phone: String, password: String) {
        val res = apiCall {
            apiService.register(
                mapOf(
                    "full_name" to fullName,
                    "email" to email,
                    "phone" to phone,
                    "password" to password,
                    "device_label" to "MAA Invest Android"
                )
            )
        }
        TokenStore.setToken(res.token)
        user.value = res.user
    }

    suspend fun refreshUser() {
        try {
            user.value = apiCall { apiService.me() }.user
        } catch (_: Exception) { /* keep stale user on transient failure */
        }
    }

    suspend fun logout() {
        try {
            apiCall { apiService.logout() }
        } catch (_: Exception) {
        }
        TokenStore.clearToken()
        user.value = null
        pinUnlocked.value = false
    }
}
