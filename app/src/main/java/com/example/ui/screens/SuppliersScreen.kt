package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Supplier
import com.example.data.local.SupplierTransaction
import com.example.ui.AccountingViewModel
import com.example.ui.components.EmptyStateView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val currencySymbol by viewModel.activeCurrencySymbol.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var viewingStatementSupplier by remember { mutableStateOf<Supplier?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else suppliers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.companyName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_supplier_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مورد")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("search_supplier_input"),
                placeholder = { Text("بحث باسم المورد أو اسم الشركة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            if (filteredSuppliers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.LocalShipping,
                    title = if (searchQuery.isEmpty()) "لا يوجد موردون مسجلون" else "لا توجد نتائج مطابقة",
                    description = if (searchQuery.isEmpty()) "سجل بيانات الموردين لإدارة فواتير المشتريات ومستحقات الدفع" else "تحقق من نص البحث",
                    actionText = if (searchQuery.isEmpty()) "إضافة مورد جديد" else null,
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSuppliers, key = { it.id }) { supplier ->
                        val balance by viewModel.getSupplierBalanceFlow(supplier.id).collectAsState(initial = supplier.openingBalance)
                        SupplierCard(
                            supplier = supplier,
                            currentBalance = balance ?: 0.0,
                            currencySymbol = currencySymbol,
                            onEdit = { editingSupplier = supplier },
                            onDelete = { viewModel.deleteSupplier(supplier) },
                            onStatementClick = { viewingStatementSupplier = supplier }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        SupplierDialog(
            title = "إضافة مورد جديد",
            supplier = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, email, address, company, balance, notes ->
                viewModel.addSupplier(name, phone, email, address, company, balance, notes)
                showAddDialog = false
            }
        )
    }

    editingSupplier?.let { supp ->
        SupplierDialog(
            title = "تعديل بيانات المورد",
            supplier = supp,
            onDismiss = { editingSupplier = null },
            onConfirm = { name, phone, email, address, company, _, notes ->
                viewModel.updateSupplier(
                    supp.copy(
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        companyName = company,
                        notes = notes
                    )
                )
                editingSupplier = null
            }
        )
    }

    viewingStatementSupplier?.let { supp ->
        val transactions by viewModel.getSupplierTransactions(supp.id).collectAsState(initial = emptyList())
        SupplierStatementDialog(
            supplier = supp,
            transactions = transactions,
            currencySymbol = currencySymbol,
            onDismiss = { viewingStatementSupplier = null }
        )
    }
}

@Composable
fun SupplierCard(
    supplier: Supplier,
    currentBalance: Double,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatementClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStatementClick() }
            .testTag("supplier_card_${supplier.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = supplier.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (supplier.companyName.isNotEmpty()) {
                        Text(text = "الشركة: ${supplier.companyName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "كشف الحساب",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { onStatementClick() }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "المستحق له: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%,.2f", currentBalance)} $currencySymbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (currentBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierDialog(
    title: String,
    supplier: Supplier?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, address: String, company: String, balance: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var companyName by remember { mutableStateOf(supplier?.companyName ?: "") }
    var openingBalance by remember { mutableStateOf(supplier?.openingBalance?.toString() ?: "") }
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المورد *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                )
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("اسم الشركة / المؤسسة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / المدينة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (supplier == null) {
                    OutlinedTextField(
                        value = openingBalance,
                        onValueChange = { openingBalance = it },
                        label = { Text("الرصيد الافتتاحي (مستحق له)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            phone.trim(),
                            email.trim(),
                            address.trim(),
                            companyName.trim(),
                            openingBalance.toDoubleOrNull() ?: 0.0,
                            notes.trim()
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_supplier_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun SupplierStatementDialog(
    supplier: Supplier,
    transactions: List<SupplierTransaction>,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val totalDebit = transactions.sumOf { it.debit }
    val totalCredit = transactions.sumOf { it.credit }
    val netBalance = totalCredit - totalDebit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "كشف حساب المورد: ${supplier.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "صافي المستحق له: ${String.format(Locale.US, "%,.2f", netBalance)} $currencySymbol",
                    fontSize = 13.sp,
                    color = if (netBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            if (transactions.isEmpty()) {
                Text(text = "لا توجد أي حركات مسجلة لهذا المورد حتى الآن.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { tx ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = dateFormat.format(Date(tx.date)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = tx.referenceNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(text = tx.description, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "مدين (سددنا): ${String.format(Locale.US, "%,.2f", tx.debit)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(text = "دائن (فاتورة): ${String.format(Locale.US, "%,.2f", tx.credit)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    Text(text = "الرصيد: ${String.format(Locale.US, "%,.2f", tx.balanceAfter)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}
