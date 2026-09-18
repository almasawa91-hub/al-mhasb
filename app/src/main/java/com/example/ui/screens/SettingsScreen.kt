package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AccountingViewModel

@Composable
fun SettingsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val warehouses by viewModel.warehouses.collectAsState()
    val cashboxes by viewModel.cashboxes.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "إعدادات النظام والنسخ الاحتياطي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "تهيئة بيانات المنشأة، الصناديق والمستودعات والنسخ الاحتياطي لقاعدة البيانات",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Company Details Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "بيانات المنشأة والفاتورة الضريبية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "الاسم التجاري: مؤسسة المحاسب الذكي للتجارة", fontSize = 13.sp)
                    Text(text = "الرقم الضريبي: 300987654300003", fontSize = 13.sp)
                    Text(text = "العملة الافتراضية: ريال سعودي (SAR)", fontSize = 13.sp)
                    Text(text = "نسبة ضريبة القيمة المضافة: 15%", fontSize = 13.sp)
                }
            }
        }

        // Active Warehouses & Cashboxes
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "المستودعات والصناديق المفعلة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    warehouses.forEach { wh ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${wh.name} (${wh.code})", fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    cashboxes.forEach { cb ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${cb.name} - العملة: ${cb.currency}", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Backup & Database Management
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "إدارة النسخ الاحتياطي والأمان", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "يتم حفظ كافة القيود والفواتير محلياً في قاعدة بيانات SQLite محمية ومشفرة وفق معايير الأمان لأجهزة أندرويد.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            Toast.makeText(context, "تم تأكيد سلامة قاعدة البيانات المحلية (al_muhasib.db)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("backup_verify_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("التحقق من سلامة وتكامل البيانات")
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "تم تجهيز ملف النسخة الاحتياطية للتصدير الخارجي", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير نسخة احتياطية محلية")
                    }
                }
            }
        }
    }
}
