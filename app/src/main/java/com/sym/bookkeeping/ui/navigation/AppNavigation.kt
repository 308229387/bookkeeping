package com.sym.bookkeeping.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sym.bookkeeping.ui.TradeViewModel
import com.sym.bookkeeping.ui.UiMessage
import com.sym.bookkeeping.ui.analysis.AnalysisScreen
import com.sym.bookkeeping.ui.journal.JournalScreen
import com.sym.bookkeeping.ui.journal.ReviewDetailScreen
import com.sym.bookkeeping.ui.journal.TrackDetailScreen
import com.sym.bookkeeping.ui.journal.TrackEditScreen
import com.sym.bookkeeping.ui.limitup.LimitUpScreen
import com.sym.bookkeeping.ui.strategy.DiagramDetailScreen
import com.sym.bookkeeping.ui.strategy.StrategyDetailScreen
import com.sym.bookkeeping.ui.strategy.StrategyListScreen
import com.sym.bookkeeping.ui.trades.TradeDetailScreen
import com.sym.bookkeeping.ui.trades.TradeEditScreen
import com.sym.bookkeeping.ui.trades.TradeListScreen

private sealed class TopDest(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Trades : TopDest("trades", "记录", Icons.AutoMirrored.Filled.ReceiptLong)
    data object Strategies : TopDest("strategies", "战法", Icons.Default.School)
    data object LimitUp : TopDest("limit_up", "行情", Icons.Default.TrendingUp)
    data object Journal : TopDest("journal", "日志", Icons.AutoMirrored.Filled.MenuBook)
    data object Analysis : TopDest("analysis", "分析", Icons.Default.Analytics)
}

private val topDestinations = listOf(
    TopDest.Trades,
    TopDest.Strategies,
    TopDest.LimitUp,
    TopDest.Journal,
    TopDest.Analysis
)

@Composable
fun BookkeepingAppNav(viewModel: TradeViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val route = currentDestination?.route
    val showBottomBar = topDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    } && route?.startsWith("strategy/") != true &&
        route?.startsWith("diagram/") != true &&
        route?.startsWith("review/") != true &&
        route?.startsWith("track/") != true &&
        route != "track_edit" &&
        route?.startsWith("track_edit/") != true &&
        route?.startsWith("trade_detail/") != true &&
        route != "trade_edit" &&
        route?.startsWith("trade_edit/") != true

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            when (message) {
                is UiMessage.Text -> snackbarHostState.showSnackbar(message.value)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == dest.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.Trades.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopDest.Trades.route) {
                TradeListScreen(
                    viewModel = viewModel,
                    onAddClick = { navController.navigate("trade_edit") },
                    onItemClick = { id -> navController.navigate("trade_detail/$id") }
                )
            }
            composable(TopDest.Strategies.route) {
                StrategyListScreen(
                    onOpenDetail = { id -> navController.navigate("strategy/$id") },
                    onOpenDiagram = { id -> navController.navigate("diagram/$id") }
                )
            }
            composable(
                route = "diagram/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                DiagramDetailScreen(
                    diagramId = id,
                    onBack = { navController.popBackStack() },
                    onOpenStrategy = { sid -> navController.navigate("strategy/$sid") }
                )
            }
            composable(TopDest.LimitUp.route) {
                LimitUpScreen(viewModel = viewModel)
            }
            composable(TopDest.Journal.route) {
                JournalScreen(
                    viewModel = viewModel,
                    onTrackClick = { id -> navController.navigate("track/$id") },
                    onTrackAdd = {
                        viewModel.startCreateTrack()
                        navController.navigate("track_edit")
                    },
                    onReviewClick = { id -> navController.navigate("review/$id") }
                )
            }
            composable(
                route = "track/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                TrackDetailScreen(
                    trackId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { editId -> navController.navigate("track_edit/$editId") }
                )
            }
            composable("track_edit") {
                TrackEditScreen(
                    trackId = null,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "track_edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id")
                TrackEditScreen(
                    trackId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "trade_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                TradeDetailScreen(
                    tradeId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { editId -> navController.navigate("trade_edit/$editId") }
                )
            }
            composable(
                route = "review/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ReviewDetailScreen(
                    reviewId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(TopDest.Analysis.route) {
                AnalysisScreen(viewModel = viewModel)
            }
            composable("trade_edit") {
                TradeEditScreen(
                    tradeId = null,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "trade_edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id")
                TradeEditScreen(
                    tradeId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "strategy/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                StrategyDetailScreen(
                    strategyId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
