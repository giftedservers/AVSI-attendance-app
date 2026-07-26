package co.ke.maawebhost.invest.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.ke.maawebhost.invest.data.Session
import co.ke.maawebhost.invest.data.TokenStore
import co.ke.maawebhost.invest.screens.auth.LoginScreen
import co.ke.maawebhost.invest.screens.auth.PinScreen
import co.ke.maawebhost.invest.screens.auth.RegisterScreen
import co.ke.maawebhost.invest.screens.auth.WelcomeScreen
import co.ke.maawebhost.invest.screens.groups.GroupDetailScreen
import co.ke.maawebhost.invest.screens.groups.GroupsScreen
import co.ke.maawebhost.invest.screens.groups.JoinGroupScreen
import co.ke.maawebhost.invest.screens.history.TransactionHistoryScreen
import co.ke.maawebhost.invest.screens.home.HomeScreen
import co.ke.maawebhost.invest.screens.invest.InvestScreen
import co.ke.maawebhost.invest.screens.profile.MoreScreen
import co.ke.maawebhost.invest.screens.profile.ProfileScreen
import co.ke.maawebhost.invest.screens.save.CreateGoalScreen
import co.ke.maawebhost.invest.screens.save.SaveScreen
import co.ke.maawebhost.invest.screens.wallet.AddMoneyScreen
import co.ke.maawebhost.invest.screens.wallet.WithdrawScreen
import co.ke.maawebhost.invest.ui.theme.Primary
import co.ke.maawebhost.invest.ui.theme.TextFaint
import kotlinx.coroutines.launch

private const val WELCOME = "welcome"
private const val LOGIN = "login"
private const val REGISTER = "register"
private const val HOME = "home"
private const val INVEST = "invest"
private const val SAVE = "save"
private const val GROUPS = "groups"
private const val MORE = "more"
private const val ADD_MONEY = "add_money"
private const val WITHDRAW = "withdraw"
private const val CREATE_GOAL = "create_goal"
private const val JOIN_GROUP = "join_group"
private const val HISTORY = "history"
private const val PROFILE = "profile"
private const val GROUP_DETAIL = "group_detail/{groupId}"

@Composable
fun MaaNavGraph() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        TokenStore.init(context)
        scope.launch { Session.bootstrap() }
    }

    val isLoading by Session.isLoading
    val user by Session.user
    val pinUnlocked by Session.pinUnlocked

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    when {
        user == null -> AuthNavHost()
        !pinUnlocked -> PinScreen(onUnlocked = { Session.pinUnlocked.value = true })
        else -> MainScaffold()
    }
}

@Composable
private fun AuthNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = WELCOME) {
        composable(WELCOME) {
            WelcomeScreen(
                onCreateAccount = { navController.navigate(REGISTER) },
                onLogin = { navController.navigate(LOGIN) }
            )
        }
        composable(LOGIN) {
            LoginScreen(onLoggedIn = {}, onGoRegister = { navController.navigate(REGISTER) })
        }
        composable(REGISTER) {
            RegisterScreen(onRegistered = {}, onGoLogin = { navController.navigate(LOGIN) })
        }
    }
}

private data class Tab(
    val route: String,
    val label: String,
    val filled: androidx.compose.ui.graphics.vector.ImageVector,
    val outline: androidx.compose.ui.graphics.vector.ImageVector,
)

private val TABS = listOf(
    Tab(HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Tab(INVEST, "Invest", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    Tab(SAVE, "Save", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    Tab(GROUPS, "Groups", Icons.Filled.People, Icons.Outlined.People),
    Tab(MORE, "More", Icons.Filled.GridView, Icons.Outlined.GridView),
)

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TABS.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                    TABS.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(HOME) { inclusive = false; saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.filled else tab.outline, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                unselectedIconColor = TextFaint,
                                unselectedTextColor = TextFaint
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = HOME) {
                composable(HOME) {
                    HomeScreen(
                        onAddMoney = { navController.navigate(ADD_MONEY) },
                        onSave = { navController.navigate(CREATE_GOAL) },
                        onWithdraw = { navController.navigate(WITHDRAW) },
                        onProfile = { navController.navigate(PROFILE) },
                    )
                }
                composable(INVEST) { InvestScreen() }
                composable(SAVE) { SaveScreen(onCreateGoal = { navController.navigate(CREATE_GOAL) }) }
                composable(GROUPS) {
                    GroupsScreen(
                        onOpenGroup = { id -> navController.navigate("group_detail/$id") },
                        onJoinByCode = { navController.navigate(JOIN_GROUP) }
                    )
                }
                composable(MORE) { MoreScreen(onProfile = { navController.navigate(PROFILE) }) }
                composable(ADD_MONEY) { AddMoneyScreen(onDone = { navController.popBackStack() }) }
                composable(WITHDRAW) { WithdrawScreen(onDone = { navController.popBackStack() }) }
                composable(CREATE_GOAL) { CreateGoalScreen(onDone = { navController.popBackStack() }) }
                composable(JOIN_GROUP) { JoinGroupScreen(onDone = { navController.popBackStack() }) }
                composable(HISTORY) { TransactionHistoryScreen(onBack = { navController.popBackStack() }) }
                composable(PROFILE) { ProfileScreen(onBack = { navController.popBackStack() }) }
                composable(
                    GROUP_DETAIL,
                    arguments = listOf(navArgument("groupId") { type = NavType.IntType })
                ) { backStackEntry2 ->
                    val groupId = backStackEntry2.arguments?.getInt("groupId") ?: 0
                    GroupDetailScreen(groupId = groupId, onClose = { navController.popBackStack() })
                }
            }
        }
    }
}
