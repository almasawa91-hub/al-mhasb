package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AccountingViewModel
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: AccountingViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToVouchers: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()
    val orgSettings by viewModel.organizationSettings.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Business Header Bar
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_org_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = PrimaryBlue)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = orgSettings?.name?.ifEmpty { "المحاسب الذكي" } ?: "المحاسب الذكي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (orgSettings?.taxNumber?.isNotEmpty() == true) "الرقم الضريبي: ${orgSettings?.taxNumber}" else "نظام المحاسبة والمخزون المالي المتكامل",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Tune, contentDescription = "الإعدادات", tint = PrimaryBlue)
                    }
                }
            }
        }

        // Operational Financial Metrics Table (Grid of clean accounting tiles)
        item {
            Text(
                text = "المؤشرات المالية (محسوبة لحظياً من القيود)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinancialStatBox(
                    title = "إجمالي المبيعات",
                    amount = stats.totalSales,
                    symbol = stats.currencySymbol,
                    icon = Icons.Default.TrendingUp,
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                FinancialStatBox(
                    title = "إجمالي المشتريات",
                    amount = stats.totalPurchases,
                    symbol = stats.currencySymbol,
                    icon = Icons.Default.TrendingDown,
                    accentColor = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinancialStatBox(
                    title = "إجمالي المقبوضات",
                    amount = stats.totalReceipts,
                    symbol = stats.currencySymbol,
                    icon = Icons.Default.ArrowDownward,
                    accentColor = SecondaryTeal,
                    modifier = Modifier.weight(1f)
                )
                FinancialStatBox(
                    title = "إجمالي المدفوعات",
                    amount = stats.totalPayments,
                    symbol = stats.currencySymbol,
                    icon = Icons.Default.ArrowUpward,
                    accentColor = Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinancialStatBox(
                    title = "أرصدة العملاء (مدين)",
                    amount = stats.totalCustomerReceivables,
                    symbol = stats.currencySymbol,
                    icon = Icons.Default.PeopleOutline,
                    accentColor = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                FinancialStatBox(
                    title = "مستحقات الموردين (دائن)",
                    amount = stats.totalSupplierPayables,
                    symbol = stats.currencySymbol,
                    icon = Icons.Default.LocalShipping,
                    accentColor = Color(0xFF6A1B9A),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Low stock alerts
        if (lowStockItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToItems() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنبيه نقص المخزون",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "يوجد ${lowStockItems.size} أصناف وصلت لحد إعادة الطلب أو نفدت.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Accounting Actions Grid (مطابق لأسلوب محاسب سوفت)
        item {
            Text(
                text = "الوصول السريع للعمليات",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionTile(
                        title = "فاتورة بيع",
                        icon = Icons.Default.PointOfSale,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSales
                    )
                    QuickActionTile(
                        title = "فاتورة شراء",
                        icon = Icons.Default.ShoppingCart,
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPurchases
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionTile(
                        title = "سندات مالية",
                        icon = Icons.Default.ReceiptLong,
                        color = SecondaryTeal,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToVouchers
                    )
                    QuickActionTile(
                        title = "دليل الأصناف",
                        icon = Icons.Default.Inventory2,
                        color = Color(0xFF00838F),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToItems
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionTile(
                        title = "سجل العملاء",
                        icon = Icons.Default.People,
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCustomers
                    )
                    QuickActionTile(
                        title = "سجل الموردين",
                        icon = Icons.Default.LocalShipping,
                        color = Color(0xFF5E35B1),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSuppliers
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionTile(
                        title = "التقارير المالية",
                        icon = Icons.Default.Assessment,
                        color = Color(0xFFD81B60),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReports
                    )
                    QuickActionTile(
                        title = "الإعدادات والنسخ",
                        icon = Icons.Default.Settings,
                        color = Color(0xFF546E7A),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSettings
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialStatBox(
    title: String,
    amount: Double,
    symbol: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${String.format(Locale.US, "%,.2f", amount)} $symbol",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
