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
import com.example.data.local.Customer
import com.example.data.local.CustomerTransaction
import com.example.ui.AccountingViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    val currencySymbol by viewModel.activeCurrencySymbol.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var viewingStatementCustomer by remember { mutableStateOf<Customer?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة عميل")
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("search_customer_input"),
                placeholder = { Text("بحث باسم العميل أو رقم الهاتف...") },
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

            if (filteredCustomers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PeopleOutline,
                    title = if (searchQuery.isEmpty()) "لا يوجد عملاء مسجلون" else "لا توجد نتائج مطابقة",
                    description = if (searchQuery.isEmpty()) "سجل بيانات العملاء لإدارة الفواتير، الأرصدة وكشوفات الحساب" else "تحقق من صحة نص البحث",
                    actionText = if (searchQuery.isEmpty()) "إضافة عميل جديد" else null,
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        val balance by viewModel.getCustomerBalanceFlow(customer.id).collectAsState(initial = customer.openingBalance)
                        CustomerCard(
                            customer = customer,
                            currentBalance = balance ?: 0.0,
                            currencySymbol = currencySymbol,
                            onEdit = { editingCustomer = customer },
                            onDelete = { viewModel.deleteCustomer(customer) },
                            onStatementClick = { viewingStatementCustomer = customer }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CustomerDialog(
            title = "إضافة عميل جديد",
            customer = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, email, address, limit, balance, notes ->
                viewModel.addCustomer(name, phone, email, address, limit, balance, notes)
                showAddDialog = false
            }
        )
    }

    editingCustomer?.let { cust ->
        CustomerDialog(
            title = "تعديل بيانات العميل",
            customer = cust,
            onDismiss = { editingCustomer = null },
            onConfirm = { name, phone, email, address, limit, _, notes ->
                viewModel.updateCustomer(
                    cust.copy(
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        creditLimit = limit,
                        notes = notes
                    )
                )
                editingCustomer = null
            }
        )
    }

    viewingStatementCustomer?.let { cust ->
        val transactions by viewModel.getCustomerTransactions(cust.id).collectAsState(initial = emptyList())
        CustomerStatementDialog(
            customer = cust,
            transactions = transactions,
            currencySymbol = currencySymbol,
            onDismiss = { viewingStatementCustomer = null }
        )
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
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
            .testTag("customer_card_${customer.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (customer.phone.isNotEmpty()) {
                        Text(text = customer.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(text = "الرصيد الحالي: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun CustomerDialog(
    title: String,
    customer: Customer?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, address: String, limit: Double, balance: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var creditLimit by remember { mutableStateOf(customer?.creditLimit?.takeIf { it > 0 }?.toString() ?: "") }
    var openingBalance by remember { mutableStateOf(customer?.openingBalance?.toString() ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }

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
                    label = { Text("اسم العميل *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = creditLimit,
                        onValueChange = { creditLimit = it },
                        label = { Text("سقف الائتمان") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (customer == null) {
                        OutlinedTextField(
                            value = openingBalance,
                            onValueChange = { openingBalance = it },
                            label = { Text("الرصيد الافتتاحي") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                            creditLimit.toDoubleOrNull() ?: 0.0,
                            openingBalance.toDoubleOrNull() ?: 0.0,
                            notes.trim()
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_customer_button")
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
fun CustomerStatementDialog(
    customer: Customer,
    transactions: List<CustomerTransaction>,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val totalDebit = transactions.sumOf { it.debit }
    val totalCredit = transactions.sumOf { it.credit }
    val netBalance = totalDebit - totalCredit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "كشف حساب: ${customer.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "الرصيد النهائي: ${String.format(Locale.US, "%,.2f", netBalance)} $currencySymbol",
                    fontSize = 13.sp,
                    color = if (netBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            if (transactions.isEmpty()) {
                Text(text = "لا توجد أي حركات مسجلة لهذا العميل حتى الآن.", modifier = Modifier.padding(16.dp))
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
                                    Text(text = "مدين: ${String.format(Locale.US, "%,.2f", tx.debit)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    Text(text = "دائن: ${String.format(Locale.US, "%,.2f", tx.credit)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
