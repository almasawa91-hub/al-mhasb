package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Cashbox
import com.example.data.local.OrganizationSettings
import com.example.data.local.Warehouse
import com.example.ui.AccountingViewModel
import com.example.ui.BackupRestoreResult
import com.example.ui.theme.PrimaryBlue

@Composable
fun SettingsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orgSettings by viewModel.organizationSettings.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val cashboxes by viewModel.cashboxes.collectAsState()

    var orgName by remember(orgSettings) { mutableStateOf(orgSettings?.name ?: "") }
    var taxNumber by remember(orgSettings) { mutableStateOf(orgSettings?.taxNumber ?: "") }
    var commercialRegister by remember(orgSettings) { mutableStateOf(orgSettings?.commercialRegister ?: "") }
    var phone by remember(orgSettings) { mutableStateOf(orgSettings?.phone ?: "") }
    var address by remember(orgSettings) { mutableStateOf(orgSettings?.address ?: "") }
    var currencySymbol by remember(orgSettings) { mutableStateOf(orgSettings?.currencySymbol ?: "ر.س") }
    var currencyName by remember(orgSettings) { mutableStateOf(orgSettings?.currencyName ?: "ريال سعودي") }
    var isTaxEnabled by remember(orgSettings) { mutableStateOf(orgSettings?.isTaxEnabled ?: true) }
    var defaultTaxRateStr by remember(orgSettings) {
        val ratePercent = ((orgSettings?.defaultTaxRate ?: 0.15) * 100).toInt()
        mutableStateOf(ratePercent.toString())
    }

    var showAddWarehouseDialog by remember { mutableStateOf(false) }
    var showAddCashboxDialog by remember { mutableStateOf(false) }
    var alertNotice by remember { mutableStateOf<String?>(null) }

    // Real File Export / Import launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportDatabaseBackup(context, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreDatabaseBackup(context, uri)
        }
    }

    // Collect backup/restore feedback
    LaunchedEffect(Unit) {
        viewModel.backupStatus.collect { result ->
            when (result) {
                is BackupRestoreResult.Success -> alertNotice = result.message
                is BackupRestoreResult.Error -> alertNotice = result.message
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Business Profile Configuration
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "بيانات المنشأة الضريبية والتجارية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "تظهر هذه البيانات على الفواتير الرسمية وكشوفات الحسابات المطبوعة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = orgName,
                        onValueChange = { orgName = it },
                        label = { Text("اسم المؤسسة / المتجر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_org_name")
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taxNumber,
                            onValueChange = { taxNumber = it },
                            label = { Text("الرقم الضريبي (VAT)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = commercialRegister,
                            onValueChange = { commercialRegister = it },
                            label = { Text("السجل التجاري") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("هاتف التواصل") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان الرئيسي") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = "إعدادات العملة والضريبة الافتراضية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text("رمز العملة (مثلاً: ر.س، $)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = currencyName,
                            onValueChange = { currencyName = it },
                            label = { Text("اسم العملة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isTaxEnabled, onCheckedChange = { isTaxEnabled = it })
                            Text(text = "تفعيل ضريبة القيمة المضافة", fontSize = 13.sp)
                        }
                        if (isTaxEnabled) {
                            OutlinedTextField(
                                value = defaultTaxRateStr,
                                onValueChange = { defaultTaxRateStr = it },
                                label = { Text("نسبة الضريبة %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val rate = (defaultTaxRateStr.toDoubleOrNull() ?: 15.0) / 100.0
                            viewModel.updateOrganizationSettings(
                                OrganizationSettings(
                                    id = 1,
                                    name = orgName.trim(),
                                    taxNumber = taxNumber.trim(),
                                    commercialRegister = commercialRegister.trim(),
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    currencySymbol = currencySymbol.trim().ifEmpty { "ر.س" },
                                    currencyName = currencyName.trim().ifEmpty { "ريال" },
                                    isTaxEnabled = isTaxEnabled,
                                    defaultTaxRate = rate,
                                    isConfigured = true
                                )
                            )
                            alertNotice = "تم حفظ إعدادات المنشأة والعملة بنجاح"
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                    ) {
                        Text("حفظ الإعدادات العامة")
                    }
                }
            }
        }

        // Warehouses Management
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "إدارة الفروع والمستودعات (${warehouses.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "لتوزيع وحركة الأصناف وفصل الجرد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showAddWarehouseDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مستودع جديد")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    warehouses.forEach { wh ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = wh.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { viewModel.deleteWarehouse(wh) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Cashboxes Management
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "الصناديق والحسابات البنكية (${cashboxes.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "لإدارة السيولة النقدية ومطابقة الأرصدة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showAddCashboxDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("صندوق جديد")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    cashboxes.forEach { cb ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "${cb.name} (${cb.type})", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(text = "الرصيد: ${cb.currentBalance} ${cb.currency}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteCashbox(cb) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Real Backup & Restore Section (النسخ الاحتياطي الحقيقي لقاعدة البيانات SQLite)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "النسخ الاحتياطي والاستعادة الحقيقية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text(
                        text = "يتم حفظ واستعادة ملف قاعدة البيانات المحلي الحقيقي SQLite بالكامل (العملاء، الموردين، الفواتير، المخزون، السندات، القيود). لا يتم استخدام أي بيانات وهمية.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val timestamp = System.currentTimeMillis()
                                exportLauncher.launch("al_muhasib_backup_$timestamp.db")
                            },
                            modifier = Modifier.weight(1f).testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير نسخة احتياطية", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.weight(1f).testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة نسخة", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddWarehouseDialog) {
        var whName by remember { mutableStateOf("") }
        var whCode by remember { mutableStateOf("") }
        var whLocation by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddWarehouseDialog = false },
            title = { Text("إضافة مستودع / فرع جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = whName, onValueChange = { whName = it }, label = { Text("اسم المستودع *") }, singleLine = true)
                    OutlinedTextField(value = whCode, onValueChange = { whCode = it }, label = { Text("رمز المستودع") }, singleLine = true)
                    OutlinedTextField(value = whLocation, onValueChange = { whLocation = it }, label = { Text("الموقع / المدينة") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (whName.isNotBlank()) {
                            viewModel.addWarehouse(whName, whCode, whLocation, "")
                            showAddWarehouseDialog = false
                        }
                    },
                    enabled = whName.isNotBlank()
                ) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showAddWarehouseDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showAddCashboxDialog) {
        var cbName by remember { mutableStateOf("") }
        var cbType by remember { mutableStateOf("صندوق نقدي") }
        var cbOpenBal by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { showAddCashboxDialog = false },
            title = { Text("إضافة صندوق أو حساب بنكي", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cbName, onValueChange = { cbName = it }, label = { Text("اسم الصندوق / الحساب البنكي *") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("صندوق نقدي", "حساب بنكي", "نقاط بيع").forEach { t ->
                            FilterChip(selected = cbType == t, onClick = { cbType = t }, label = { Text(t, fontSize = 11.sp) })
                        }
                    }
                    OutlinedTextField(
                        value = cbOpenBal,
                        onValueChange = { cbOpenBal = it },
                        label = { Text("الرصيد الافتتاحي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cbName.isNotBlank()) {
                            viewModel.addCashbox(cbName, cbType, "", cbOpenBal.toDoubleOrNull() ?: 0.0)
                            showAddCashboxDialog = false
                        }
                    },
                    enabled = cbName.isNotBlank()
                ) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCashboxDialog = false }) { Text("إلغاء") }
            }
        )
    }

    alertNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { alertNotice = null },
            text = { Text(notice) },
            confirmButton = {
                TextButton(onClick = { alertNotice = null }) { Text("حسناً") }
            }
        )
    }
}
