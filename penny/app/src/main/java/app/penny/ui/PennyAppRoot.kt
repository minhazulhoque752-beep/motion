package app.penny.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.penny.data.UserPrefs
import app.penny.domain.EntryType
import app.penny.ui.common.Hairline
import app.penny.ui.common.LocalAppContainer
import app.penny.ui.entry.EntryScreen
import app.penny.ui.goals.GoalDetailScreen
import app.penny.ui.goals.GoalEditorScreen
import app.penny.ui.goals.GoalsScreen
import app.penny.ui.history.HistoryScreen
import app.penny.ui.home.HomeScreen
import app.penny.ui.icons.PennyIcons
import app.penny.ui.insights.InsightsScreen
import app.penny.ui.onboarding.OnboardingScreen
import app.penny.ui.settings.PrivacyScreen
import app.penny.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val GOALS = "goals"
    const val INSIGHTS = "insights"
    const val ENTRY = "entry?id={id}&type={type}&goalId={goalId}"
    const val GOAL_DETAIL = "goal/{id}"
    const val GOAL_EDITOR = "goalEditor?id={id}"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"

    fun entry(id: Long? = null, type: EntryType = EntryType.DEPOSIT, goalId: Long? = null) =
        "entry?id=${id ?: -1}&type=${type.name}&goalId=${goalId ?: -1}"

    fun goalDetail(id: Long) = "goal/$id"
    fun goalEditor(id: Long? = null) = "goalEditor?id=${id ?: -1}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", PennyIcons.Home),
    Tab(Routes.HISTORY, "History", PennyIcons.History),
    Tab(Routes.GOALS, "Goals", PennyIcons.Goals),
    Tab(Routes.INSIGHTS, "Insights", PennyIcons.Insights),
)

@Composable
fun PennyAppRoot(
    prefs: UserPrefs,
    openAddRequested: Boolean,
    onOpenAddHandled: () -> Unit,
) {
    if (!prefs.onboarded) {
        OnboardingScreen()
    } else {
        MainScaffold(openAddRequested, onOpenAddHandled)
    }
}

@Composable
private fun MainScaffold(openAddRequested: Boolean, onOpenAddHandled: () -> Unit) {
    val container = LocalAppContainer.current
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute == null || tabs.any { it.route == currentRoute }

    LaunchedEffect(snackbar) {
        container.messenger.messages.collectLatest { m ->
            val result = snackbar.showSnackbar(
                message = m.text,
                actionLabel = m.actionLabel,
                duration = if (m.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) m.onAction?.invoke()
        }
    }

    // Opened from the daily reminder notification: go straight to "Add savings".
    LaunchedEffect(openAddRequested) {
        if (openAddRequested) {
            navController.navigate(Routes.entry()) { launchSingleTop = true }
            onOpenAddHandled()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = MaterialTheme.shapes.small,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        },
        bottomBar = { if (showBottomBar) BottomBar(navController, currentRoute) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAdd = { type -> navController.navigate(Routes.entry(type = type)) },
                    onOpenEntry = { id -> navController.navigate(Routes.entry(id = id)) },
                    onSeeAll = { navController.navigateToTab(Routes.HISTORY) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenGoals = { navController.navigateToTab(Routes.GOALS) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenEntry = { id -> navController.navigate(Routes.entry(id = id)) },
                    onAdd = { navController.navigate(Routes.entry()) },
                )
            }
            composable(Routes.GOALS) {
                GoalsScreen(
                    onOpenGoal = { id -> navController.navigate(Routes.goalDetail(id)) },
                    onCreateGoal = { navController.navigate(Routes.goalEditor()) },
                )
            }
            composable(Routes.INSIGHTS) {
                InsightsScreen(onAdd = { navController.navigate(Routes.entry()) })
            }
            composable(
                Routes.ENTRY,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("type") { type = NavType.StringType; defaultValue = EntryType.DEPOSIT.name },
                    navArgument("goalId") { type = NavType.LongType; defaultValue = -1L },
                ),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
            ) { entry ->
                val args = entry.arguments
                val id = args?.getLong("id")?.takeIf { it > 0 }
                val type = if (args?.getString("type") == EntryType.WITHDRAWAL.name) EntryType.WITHDRAWAL else EntryType.DEPOSIT
                val goalId = args?.getLong("goalId")?.takeIf { it > 0 }
                EntryScreen(
                    editingId = id,
                    initialType = type,
                    initialGoalId = goalId,
                    onClose = { navController.popBackStack() },
                )
            }
            composable(
                Routes.GOAL_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                GoalDetailScreen(
                    goalId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.goalEditor(id)) },
                    onAddToGoal = { type -> navController.navigate(Routes.entry(type = type, goalId = id)) },
                    onOpenEntry = { entryId -> navController.navigate(Routes.entry(id = entryId)) },
                )
            }
            composable(
                Routes.GOAL_EDITOR,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                GoalEditorScreen(
                    editingId = id,
                    onClose = { navController.popBackStack() },
                    onDeleted = {
                        // The goal's detail screen is now stale; return to the goals list.
                        navController.popBackStack(Routes.GOALS, inclusive = false)
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                )
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    Column {
        Hairline()
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) navController.navigateToTab(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
