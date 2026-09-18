package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun ReportsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val items by viewModel.items.collectAsState()
    val currencySymbol by viewModel.activeCurrencySymbol.collectAsState()

    var selectedReportTab by remember { mutableStateOf(0) } // 0: Income Statement, 1: Balances & Receivables, 2: Inventory Valuation

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(selectedTabIndex = selectedReportTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = selectedReportTab == 0, onClick = { selectedReportTab = 0 }, text = { Text("قائمة الدخل والأرباح", fontSize = 12.sp) })
            Tab(selected = selectedReportTab == 1, onClick = { selectedReportTab = 1 }, text = { Text("كشف الذمم والأرصدة", fontSize = 12.sp) })
            Tab(selected = selectedReportTab == 2, onClick = { selectedReportTab = 2 }, text = { Text("تقييم المخزون", fontSize = 12.sp) })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedReportTab) {
                0 -> {
                    // Income Statement / Profit & Loss
                    val grossProfit = stats.totalSales - stats.totalPurchases
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "قائمة الأرباح والخسائر التقديرية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = "مستخرجة آلياً من فواتير المبيعات وفواتير المشتريات المعتمدة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(14.dp))

                                ReportRow(title = "إجمالي إيرادات المبيعات", amount = stats.totalSales, symbol = currencySymbol, color = SuccessGreen)
                                ReportRow(title = "تكلفة المشتريات", amount = stats.totalPurchases, symbol = currencySymbol, color = ErrorRed)
                                Divider(modifier = Modifier.padding(vertical = 10.dp))
                                ReportRow(
                                    title = "مجمل الربح / الخسارة التشغيلية",
                                    amount = grossProfit,
                                    symbol = currencySymbol,
                                    color = if (grossProfit >= 0) SuccessGreen else ErrorRed,
                                    isBold = true
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                ReportRow(title = "المتحصلات النقدية الفعلية (سندات القبض)", amount = stats.totalReceipts, symbol = currencySymbol, color = SecondaryTeal)
                                ReportRow(title = "المدفوعات والمصروفات الفعلية (سندات الصرف)", amount = stats.totalPayments, symbol = currencySymbol, color = ErrorRed)
                                Divider(modifier = Modifier.padding(vertical = 10.dp))
                                ReportRow(
                                    title = "صافي التدفق النقدي",
                                    amount = stats.netCashFlow,
                                    symbol = currencySymbol,
                                    color = if (stats.netCashFlow >= 0) PrimaryBlue else ErrorRed,
                                    isBold = true
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Receivables & Payables Ledger
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "ملخص الذمم المدينة والدائنة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                ReportRow(title = "إجمالي ذمم العملاء (مستحقات للمنشأة)", amount = stats.totalCustomerReceivables, symbol = currencySymbol, color = PrimaryBlue, isBold = true)
                                ReportRow(title = "إجمالي التزامات الموردين (مستحقات على المنشأة)", amount = stats.totalSupplierPayables, symbol = currencySymbol, color = ErrorRed, isBold = true)
                                Divider(modifier = Modifier.padding(vertical = 10.dp))
                                ReportRow(
                                    title = "صافي موقف الذمم",
                                    amount = stats.totalCustomerReceivables - stats.totalSupplierPayables,
                                    symbol = currencySymbol,
                                    color = if (stats.totalCustomerReceivables >= stats.totalSupplierPayables) SuccessGreen else ErrorRed,
                                    isBold = true
                                )
                            }
                        }
                    }

                    item {
                        Text(text = "كشف بأعلى العملاء مديونية (${customers.size} عميل)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    if (customers.isEmpty()) {
                        item { Text(text = "لا يوجد عملاء مسجلون", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(customers) { cust ->
                            val bal by viewModel.getCustomerBalanceFlow(cust.id).collectAsState(initial = cust.openingBalance)
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (cust.phone.isNotEmpty()) Text(text = cust.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%,.2f", bal ?: 0.0)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if ((bal ?: 0.0) > 0) ErrorRed else SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Inventory Valuation
                    val totalInventoryCost = items.sumOf { it.currentStock * it.purchasePrice }
                    val totalInventorySaleVal = items.sumOf { it.currentStock * it.salePrice }
                    val potentialProfit = totalInventorySaleVal - totalInventoryCost

                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "تقرير تقييم بضاعة آخر المدة بالمستودعات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                ReportRow(title = "عدد الأصناف المعرفة بالمخزن", amount = items.size.toDouble(), symbol = "صنف", color = PrimaryBlue)
                                ReportRow(title = "قيمة المخزون بسعر التكلفة (الشراء)", amount = totalInventoryCost, symbol = currencySymbol, color = SecondaryTeal, isBold = true)
                                ReportRow(title = "قيمة المخزون المتوقعة بسعر البيع", amount = totalInventorySaleVal, symbol = currencySymbol, color = SuccessGreen, isBold = true)
                                Divider(modifier = Modifier.padding(vertical = 10.dp))
                                ReportRow(title = "الأرباح المتوقعة عند بيع كامل المخزون", amount = potentialProfit, symbol = currencySymbol, color = SuccessGreen, isBold = true)
                            }
                        }
                    }

                    item {
                        Text(text = "تفاصيل جرد الأصناف بالمخازن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    if (items.isEmpty()) {
                        item { Text(text = "لا توجد أصناف في المستودع", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(items) { itm ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = itm.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "الرصيد: ${itm.currentStock} ${itm.unit} × ${itm.purchasePrice} تكلفة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%,.2f", itm.currentStock * itm.purchasePrice)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportRow(
    title: String,
    amount: Double,
    symbol: String,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = if (isBold) 14.sp else 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${String.format(Locale.US, "%,.2f", amount)} $symbol",
            fontSize = if (isBold) 14.sp else 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
