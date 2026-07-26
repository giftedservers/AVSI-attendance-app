package co.ke.maawebhost.invest.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiUser(
    val id: Int,
    val full_name: String,
    val email: String,
    val phone: String,
    val role: String,
    val kyc_status: String,
    val status: String,
    val xp: Int = 0,
    val level: Int = 0,
    val coins: Int = 0,
    val current_streak: Int = 0,
    val longest_streak: Int = 0,
    val referral_code: String? = null,
    val created_at: String? = null,
)

@Serializable
data class AuthResponse(
    val success: Boolean = true,
    val token: String,
    val user: ApiUser,
)

@Serializable
data class MeResponse(val success: Boolean = true, val user: ApiUser)

@Serializable
data class DashboardResponse(
    val success: Boolean = true,
    val user: ApiUser,
    val wallet_balance: Double = 0.0,
    val currency: String = "KES",
    val total_saved: Double = 0.0,
    val active_goals_count: Int = 0,
    val investment_balance: Double = 0.0,
    val goals: List<Goal> = emptyList(),
    val recent_transactions: List<Transaction> = emptyList(),
)

@Serializable
data class Goal(
    val id: Int,
    val name: String,
    val target_amount: String? = null,
    val saved_amount: String? = null,
    val deadline: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val category_name: String? = null,
)

@Serializable
data class GoalListResponse(val success: Boolean = true, val goals: List<Goal> = emptyList())

@Serializable
data class GoalResponse(val success: Boolean = true, val goal: Goal)

@Serializable
data class Transaction(
    val id: Int,
    val type: String,
    val channel: String? = null,
    val amount: String? = null,
    val balance_after: String? = null,
    val description: String? = null,
    val created_at: String,
)

@Serializable
data class TransactionListResponse(
    val success: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val page: Int = 1,
    val per_page: Int = 20,
    val total: Int = 0,
    val total_pages: Int = 1,
)

@Serializable
data class WalletBalance(val balance: String, val currency: String? = null, val updated_at: String? = null)

@Serializable
data class WalletBalanceResponse(val success: Boolean = true, val wallet: WalletBalance)

@Serializable
data class WalletActionResponse(
    val success: Boolean = true,
    val status: String? = null,
    val reference: String? = null,
    val wallet_balance: Double? = null,
)

@Serializable
data class InvestmentProduct(
    val id: Int,
    val name: String,
    val type: String? = null,
    val annual_return_rate: String? = null,
    val risk_level: String = "low",
    val min_investment: String? = null,
    val is_premium: Boolean? = null,
    val locked: Boolean = false,
)

@Serializable
data class ProductsResponse(val success: Boolean = true, val products: List<InvestmentProduct> = emptyList())

@Serializable
data class Holding(
    val id: Int,
    val product_id: Int,
    val product_name: String,
    val annual_return_rate: String? = null,
    val amount_invested: String? = null,
    val current_value: String? = null,
)

@Serializable
data class HoldingsResponse(
    val success: Boolean = true,
    val holdings: List<Holding> = emptyList(),
    val portfolio_value: Double = 0.0,
    val portfolio_invested: Double = 0.0,
)

@Serializable
data class Group(
    val id: Int,
    val name: String,
    val description: String? = null,
    val target_amount: String? = null,
    val saved_amount: String = "0",
    val invite_code: String? = null,
    val my_role: String? = null,
    val member_count: Int = 0,
)

@Serializable
data class GroupListResponse(val success: Boolean = true, val groups: List<Group> = emptyList())

@Serializable
data class GroupResponse(val success: Boolean = true, val group: Group)

@Serializable
data class ContributeResponse(val success: Boolean = true, val wallet_balance: Double = 0.0)

@Serializable
data class SimpleOk(val success: Boolean = true)

@Serializable
data class ApiErrorBody(
    val success: Boolean = false,
    val message: String = "Something went wrong.",
    val upgrade_required: Boolean = false,
)
