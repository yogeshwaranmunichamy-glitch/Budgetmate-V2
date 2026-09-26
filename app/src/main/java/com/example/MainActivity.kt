package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
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
        var showMoreSheet by remember { mutableStateOf(false) }

        val primaryNavItems = listOf(
            NavItem("dashboard", "Home", Icons.Default.AccountBalanceWallet),
            NavItem("transactions", "History", Icons.Default.ReceiptLong),
            NavItem("trips", "Trips", Icons.Default.FlightTakeoff),
            NavItem("voice", "Voice", Icons.Default.Mic),
            NavItem("more", "More", Icons.Default.MoreHoriz)
        )

        val secondaryScreens = listOf("budget", "savings", "reports", "profile")

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    primaryNavItems.forEach { item ->
                        val isMoreItem = item.route == "more"
                        val selected = if (isMoreItem) {
                            currentScreen in secondaryScreens
                        } else {
                            currentScreen == item.route
                        }

                        val displayTitle = if (isMoreItem && currentScreen in secondaryScreens) {
                            when (currentScreen) {
                                "budget" -> "Budget"
                                "savings" -> "Goals"
                                "reports" -> "Reports"
                                "profile" -> "Profile"
                                else -> "More"
                            }
                        } else {
                            item.title
                        }

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (isMoreItem) {
                                    showMoreSheet = true
                                } else {
                                    viewModel.setScreen(item.route)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = displayTitle,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
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

        // More Options Bottom Sheet with spacious touch targets
        if (showMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "More Features",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showMoreSheet = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val moreItems = listOf(
                        Triple("budget", "Budget & Limits", Icons.Default.PieChart) to "Set category spending limits & track alerts",
                        Triple("savings", "Goals & Recurring", Icons.Default.Savings) to "Target emergency funds & recurring bills",
                        Triple("reports", "Reports & Analytics", Icons.Default.Assessment) to "Deep spending breakdown & export reports",
                        Triple("profile", "User Profile & ML", Icons.Default.Person) to "Account settings, currency & AI models"
                    )

                    moreItems.forEach { (navInfo, subtitle) ->
                        val (route, title, icon) = navInfo
                        val isCurrent = currentScreen == route

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) EmeraldPrimary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    showMoreSheet = false
                                    if (route == "reports") {
                                        viewModel.requestSummaryOrInsightsWithAd("Reports & Summaries") {
                                            viewModel.setScreen(route)
                                        }
                                    } else {
                                        viewModel.setScreen(route)
                                    }
                                }
                                .testTag("more_sheet_$route")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = subtitle,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
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
