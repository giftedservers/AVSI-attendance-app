package co.ke.maawebhost.invest.data.api

import co.ke.maawebhost.invest.data.TokenStore
import co.ke.maawebhost.invest.data.api.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor

/** Point this at your deployed MAA INVEST backend. */
const val API_BASE_URL = "https://invest.maawebhost.co.ke/api/"

class ApiException(message: String, val status: Int, val upgradeRequired: Boolean = false) : Exception(message)

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
private val jsonMedia = "application/json; charset=utf-8".toMediaType()

private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
    .build()

/** Thin wrapper around OkHttp that reads/writes JSON via kotlinx.serialization directly. */
private object Api {

    suspend fun <T> get(path: String, serializer: KSerializer<T>, auth: Boolean = true): T =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(API_BASE_URL + path).get()
            if (auth) TokenStore.getToken()?.let { builder.addHeader("Authorization", "Bearer $it") }
            execute(builder.build(), serializer)
        }

    suspend fun <T> post(path: String, body: Map<String, String>, serializer: KSerializer<T>, auth: Boolean = true): T =
        withContext(Dispatchers.IO) {
            val bodyJson = json.encodeToString(MapSerializer(String.serializer(), String.serializer()), body)
            val builder = Request.Builder()
                .url(API_BASE_URL + path)
                .post(bodyJson.toRequestBody(jsonMedia))
            if (auth) TokenStore.getToken()?.let { builder.addHeader("Authorization", "Bearer $it") }
            execute(builder.build(), serializer)
        }

    private fun <T> execute(request: Request, serializer: KSerializer<T>): T {
        okHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val parsed = try {
                    json.decodeFromString(ApiErrorBody.serializer(), bodyStr)
                } catch (_: Exception) {
                    null
                }
                throw ApiException(
                    parsed?.message ?: "Something went wrong. Please try again.",
                    response.code,
                    parsed?.upgrade_required ?: false
                )
            }
            return try {
                json.decodeFromString(serializer, bodyStr)
            } catch (e: Exception) {
                throw ApiException("Unexpected response from server.", response.code)
            }
        }
    }
}

/** Wraps a call so network failures surface as ApiException too, matching the HTTP-error path. */
suspend fun <T> apiCall(block: suspend () -> T): T {
    return try {
        block()
    } catch (e: ApiException) {
        throw e
    } catch (e: java.io.IOException) {
        throw ApiException("Could not reach the server. Check your connection.", 0)
    }
}

/**
 * Same method names/signatures the screens already call — only the
 * implementation underneath changed from Retrofit to plain OkHttp.
 */
object apiService {
    suspend fun register(body: Map<String, String>) = Api.post("auth/register.php", body, AuthResponse.serializer(), auth = false)
    suspend fun login(body: Map<String, String>) = Api.post("auth/login.php", body, AuthResponse.serializer(), auth = false)
    suspend fun me() = Api.get("auth/me.php", MeResponse.serializer())
    suspend fun logout() = Api.post("auth/logout.php", emptyMap(), SimpleOk.serializer())

    suspend fun dashboard() = Api.get("dashboard.php", DashboardResponse.serializer())

    suspend fun listGoals() = Api.get("goals/list.php", GoalListResponse.serializer())
    suspend fun createGoal(body: Map<String, String>) = Api.post("goals/create.php", body, GoalResponse.serializer())
    suspend fun fundGoal(body: Map<String, String>) = Api.post("goals/fund.php", body, GoalResponse.serializer())

    suspend fun walletBalance() = Api.get("wallet/balance.php", WalletBalanceResponse.serializer())
    suspend fun walletTransactions(page: Int) = Api.get("wallet/transactions.php?page=$page", TransactionListResponse.serializer())
    suspend fun deposit(body: Map<String, String>) = Api.post("wallet/deposit.php", body, WalletActionResponse.serializer())
    suspend fun withdraw(body: Map<String, String>) = Api.post("wallet/withdraw.php", body, WalletActionResponse.serializer())

    suspend fun listProducts() = Api.get("investments/products.php", ProductsResponse.serializer())
    suspend fun listHoldings() = Api.get("investments/holdings.php", HoldingsResponse.serializer())
    suspend fun buyInvestment(body: Map<String, String>) = Api.post("investments/buy.php", body, WalletActionResponse.serializer())

    suspend fun listGroups() = Api.get("groups/list.php", GroupListResponse.serializer())
    suspend fun joinGroup(body: Map<String, String>) = Api.post("groups/join.php", body, GroupResponse.serializer())
    suspend fun contributeToGroup(body: Map<String, String>) = Api.post("groups/contribute.php", body, ContributeResponse.serializer())
}
