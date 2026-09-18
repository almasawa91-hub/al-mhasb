package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AccountingViewModel
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

    val totalInventoryCost = items.sumOf { it.currentStock * it.purchasePrice }
    val totalInventoryValue = items.sumOf { it.currentStock * it.salePrice }
    val grossProfit = (stats.totalSales - stats.totalPurchases).coerceAtLeast(0.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "التقارير المالية والختامية (محسوبة لحظياً)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "كافة الأرقام مستخرجة مباشرة من قاعدة البيانات المحلية دون أي افتراضات",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Income Statement Card (قائمة الدخل المبسطة)
        item {
            ReportCard(
                title = "ملخص قائمة الدخل والأرباح",
                icon = Icons.Default.Assessment,
                color = MaterialTheme.colorScheme.primary
            ) {
                ReportRow(label = "إجمالي المبيعات المحققة", value = "${String.format(Locale.US, "%,.2f", stats.totalSales)} ر.س", color = Color(0xFF2E7D32))
                ReportRow(label = "تكلفة المشتريات المسجلة", value = "${String.format(Locale.US, "%,.2f", stats.totalPurchases)} ر.س", color = Color(0xFFC62828))
                Divider(modifier = Modifier.padding(vertical = 6.dp))
                ReportRow(
                    label = "مجمل الربح التقديري",
                    value = "${String.format(Locale.US, "%,.2f", grossProfit)} ر.س",
                    isBold = true,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Cash Flow Card (حركة السيولة والصناديق)
        item {
            ReportCard(
                title = "تقرير حركة السيولة النقدية (Cash Flow)",
                icon = Icons.Default.AccountBalanceWallet,
                color = MaterialTheme.colorScheme.secondary
            ) {
                ReportRow(label = "مقبوضات نقدية وبنكية (سندات القبض)", value = "${String.format(Locale.US, "%,.2f", stats.totalReceipts)} ر.س", color = Color(0xFF2E7D32))
                ReportRow(label = "مدفوعات ومصروفات (سندات الصرف)", value = "${String.format(Locale.US, "%,.2f", stats.totalPayments)} ر.س", color = Color(0xFFE65100))
                Divider(modifier = Modifier.padding(vertical = 6.dp))
                ReportRow(
                    label = "صافي الحركة النقدية",
                    value = "${String.format(Locale.US, "%,.2f", stats.netCashFlow)} ر.س",
                    isBold = true,
                    color = if (stats.netCashFlow >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }

        // Inventory Valuation Card (تقييم بضاعة المخزون)
        item {
            ReportCard(
                title = "تقرير تقييم المخزون وبضاعة آخر المدة",
                icon = Icons.Default.Inventory,
                color = Color(0xFF00838F)
            ) {
                ReportRow(label = "عدد الأصناف المسجلة", value = "${items.size} صنف")
                ReportRow(label = "إجمالي قيمة المخزون بسعر التكلفة", value = "${String.format(Locale.US, "%,.2f", totalInventoryCost)} ر.س")
                ReportRow(label = "القيمة البيعية التقديرية للمخزون", value = "${String.format(Locale.US, "%,.2f", totalInventoryValue)} ر.س")
            }
        }

        // Parties Overview (العملاء والموردون)
        item {
            ReportCard(
                title = "إحصائيات دفتر الأستاذ العام",
                icon = Icons.Default.Contacts,
                color = Color(0xFF4527A0)
            ) {
                ReportRow(label = "إجمالي عدد العملاء المسجلين", value = "${customers.size} عميل")
                ReportRow(label = "إجمالي عدد الموردين المسجلين", value = "${suppliers.size} مورد")
            }
        }
    }
}

@Composable
fun ReportCard(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("report_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ReportRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}
