package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.ui.components.SponsoredAdDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SavingsRecurringScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.TripScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.BudgetMateTheme
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.BudgetMateViewModel

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {

    private val viewModel: BudgetMateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetMateTheme {
                MainAppContent(viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: BudgetMateViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val showAdDialog by viewModel.showAdDialog.collectAsState()
    val pendingAdPurpose by viewModel.pendingAdPurpose.collectAsState()

    // Handle back button when not on dashboard
    if (currentScreen != "dashboard" && currentUser != null) {
        BackHandler {
            viewModel.setScreen("dashboard")
        }
    }

    if (currentUser == null) {
        AuthScreen(viewModel = viewModel)
    } else {
        val navItems = listOf(
            NavItem("dashboard", "Home", Icons.Default.AccountBalanceWallet),
            NavItem("transactions", "History", Icons.Default.ReceiptLong),
            NavItem("trips", "Trips", Icons.Default.FlightTakeoff),
            NavItem("voice", "Voice", Icons.Default.Mic),
            NavItem("budget", "Budget", Icons.Default.PieChart),
            NavItem("savings", "Goals", Icons.Default.Savings),
            NavItem("reports", "Reports", Icons.Default.Assessment),
            NavItem("profile", "Profile", Icons.Default.Person)
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    navItems.forEach { item ->
                        val selected = currentScreen == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (item.route == "reports") {
                                    // User requested reports/summaries -> Check ad requirement
                                    viewModel.requestSummaryOrInsightsWithAd("Reports & Summaries") {
                                        viewModel.setScreen(item.route)
                                    }
                                } else {
                                    viewModel.setScreen(item.route)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_${item.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    "dashboard" -> DashboardScreen(viewModel = viewModel)
                    "transactions" -> TransactionsScreen(viewModel = viewModel)
                    "trips" -> TripScreen(viewModel = viewModel)
                    "voice" -> VoiceAssistantScreen(viewModel = viewModel)
                    "budget" -> BudgetScreen(viewModel = viewModel)
                    "savings" -> SavingsRecurringScreen(viewModel = viewModel)
                    "reports" -> ReportsScreen(viewModel = viewModel)
                    "profile" -> ProfileScreen(viewModel = viewModel)
                    else -> DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Interactive Sponsored Ad Dialog triggered when user requests weekly, monthly, yearly summaries or spending insights
    if (showAdDialog) {
        SponsoredAdDialog(
            purposeTitle = pendingAdPurpose.ifBlank { "Insights & Summaries" },
            onDismiss = { viewModel.dismissAdDialog() },
            onRewardUnlocked = { viewModel.unlockAdReward() }
        )
    }
}
