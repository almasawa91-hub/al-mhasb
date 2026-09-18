package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.AccountingViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("الرئيسية", Icons.Default.Dashboard),
    SALES("المبيعات", Icons.Default.PointOfSale),
    PURCHASES("المشتريات", Icons.Default.ShoppingCart),
    VOUCHERS("السندات", Icons.Default.ReceiptLong),
    CUSTOMERS("العملاء", Icons.Default.People),
    SUPPLIERS("الموردون", Icons.Default.LocalShipping),
    ITEMS("الأصناف", Icons.Default.Inventory2),
    REPORTS("التقارير", Icons.Default.Assessment),
    SETTINGS("الإعدادات", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: AccountingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Force Right-to-Left (RTL) for Arabic Accounting UX
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme {
                    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(currentScreen.title) },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                navigationIcon = {
                                    if (currentScreen != AppScreen.DASHBOARD) {
                                        IconButton(onClick = { currentScreen = AppScreen.DASHBOARD }) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                                        }
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.DASHBOARD,
                                    onClick = { currentScreen = AppScreen.DASHBOARD },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                                    label = { Text("الرئيسية") },
                                    modifier = Modifier.testTag("nav_dashboard")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.SALES,
                                    onClick = { currentScreen = AppScreen.SALES },
                                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "المبيعات") },
                                    label = { Text("المبيعات") },
                                    modifier = Modifier.testTag("nav_sales")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.PURCHASES,
                                    onClick = { currentScreen = AppScreen.PURCHASES },
                                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "المشتريات") },
                                    label = { Text("المشتريات") },
                                    modifier = Modifier.testTag("nav_purchases")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.ITEMS,
                                    onClick = { currentScreen = AppScreen.ITEMS },
                                    icon = { Icon(Icons.Default.Inventory2, contentDescription = "الأصناف") },
                                    label = { Text("الأصناف") },
                                    modifier = Modifier.testTag("nav_items")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.REPORTS,
                                    onClick = { currentScreen = AppScreen.REPORTS },
                                    icon = { Icon(Icons.Default.Assessment, contentDescription = "التقارير") },
                                    label = { Text("التقارير") },
                                    modifier = Modifier.testTag("nav_reports")
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentScreen) {
                                AppScreen.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToSales = { currentScreen = AppScreen.SALES },
                                    onNavigateToPurchases = { currentScreen = AppScreen.PURCHASES },
                                    onNavigateToCustomers = { currentScreen = AppScreen.CUSTOMERS },
                                    onNavigateToItems = { currentScreen = AppScreen.ITEMS },
                                    onNavigateToVouchers = { currentScreen = AppScreen.VOUCHERS }
                                )
                                AppScreen.SALES -> InvoicesScreen(
                                    type = "SALE",
                                    viewModel = viewModel
                                )
                                AppScreen.PURCHASES -> InvoicesScreen(
                                    type = "PURCHASE",
                                    viewModel = viewModel
                                )
                                AppScreen.VOUCHERS -> VouchersScreen(
                                    viewModel = viewModel
                                )
                                AppScreen.CUSTOMERS -> CustomersScreen(
                                    viewModel = viewModel
                                )
                                AppScreen.SUPPLIERS -> SuppliersScreen(
                                    viewModel = viewModel
                                )
                                AppScreen.ITEMS -> ItemsScreen(
                                    viewModel = viewModel
                                )
                                AppScreen.REPORTS -> ReportsScreen(
                                    viewModel = viewModel
                                )
                                AppScreen.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
