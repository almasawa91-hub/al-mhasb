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
import com.example.data.local.Cashbox
import com.example.data.local.Customer
import com.example.data.local.Supplier
import com.example.data.local.Voucher
import com.example.ui.AccountingViewModel
import com.example.ui.components.EmptyStateView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchersScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val vouchers by viewModel.vouchers.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val cashboxes by viewModel.cashboxes.collectAsState()
    val currencySymbol by viewModel.activeCurrencySymbol.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Receipt, 2: Payment
    var showCreateDialog by remember { mutableStateOf(false) }
    var dialogVoucherType by remember { mutableStateOf("RECEIPT") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val filteredVouchers = remember(vouchers, selectedTab) {
        when (selectedTab) {
            1 -> vouchers.filter { it.type == "RECEIPT" }
            2 -> vouchers.filter { it.type == "PAYMENT" }
            else -> vouchers
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FloatingActionButton(
                    onClick = {
                        dialogVoucherType = "PAYMENT"
                        showCreateDialog = true
                    },
                    containerColor = Color(0xFFC62828),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_payment_voucher_fab")
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سند صرف", fontWeight = FontWeight.Bold)
                    }
                }

                FloatingActionButton(
                    onClick = {
                        dialogVoucherType = "RECEIPT"
                        showCreateDialog = true
                    },
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_receipt_voucher_fab")
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سند قبض", fontWeight = FontWeight.Bold)
                    }
                }
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
            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("الكل") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("سندات القبض") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("سندات الصرف") }
                )
            }

            if (filteredVouchers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = "لا توجد سندات مالية مسجلة",
                    description = "استخدم سندات القبض والصرف لتسجيل المدفوعات والمقبوضات النقدية وتحديث كشوفات الحسابات فوراً",
                    actionText = "إصدار سند جديد",
                    onActionClick = {
                        dialogVoucherType = "RECEIPT"
                        showCreateDialog = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredVouchers, key = { it.id }) { voucher ->
                        VoucherCard(
                            voucher = voucher,
                            currencySymbol = currencySymbol,
                            onCancel = {
                                viewModel.reverseVoucher(voucher.id) {
                                    snackbarMessage = "تم إلغاء السند وعكس القيود بنجاح"
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateVoucherDialog(
            voucherType = dialogVoucherType,
            customers = customers,
            suppliers = suppliers,
            cashboxes = cashboxes,
            currencySymbol = currencySymbol,
            onDismiss = { showCreateDialog = false },
            onConfirm = { num, type, pId, pName, pType, amount, cbId, cbName, method, notes ->
                viewModel.createVoucher(
                    voucherNumber = num,
                    type = type,
                    partyId = pId,
                    partyName = pName,
                    partyType = pType,
                    amount = amount,
                    cashboxId = cbId,
                    cashboxName = cbName,
                    paymentMethod = method,
                    currencyCode = currencySymbol,
                    notes = notes,
                    onSuccess = {
                        showCreateDialog = false
                        snackbarMessage = "تم حفظ السند وتحديث الرصيد بنجاح"
                    },
                    onError = { err -> snackbarMessage = err }
                )
            }
        )
    }

    snackbarMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { snackbarMessage = null },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { snackbarMessage = null }) { Text("حسناً") }
            }
        )
    }
}

@Composable
fun VoucherCard(
    voucher: Voucher,
    currencySymbol: String,
    onCancel: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val isReceipt = voucher.type == "RECEIPT"

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("voucher_card_${voucher.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isReceipt) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isReceipt) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isReceipt) "سند قبض" else "سند صرف",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isReceipt) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                    Text(
                        text = voucher.voucherNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = dateFormat.format(Date(voucher.date)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "الطرف: ${voucher.partyName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (voucher.notes.isNotEmpty()) {
                        Text(text = "البيان: ${voucher.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = "${String.format(Locale.US, "%,.2f", voucher.amount)} $currencySymbol",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isReceipt) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الصندوق/البنك: ${voucher.cashboxName.ifEmpty { "الرئيسي" }} (${voucher.paymentMethod})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onCancel) {
                    Text("إلغاء وعكس", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVoucherDialog(
    voucherType: String,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    cashboxes: List<Cashbox>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (
        voucherNumber: String,
        type: String,
        partyId: Long?,
        partyName: String,
        partyType: String,
        amount: Double,
        cashboxId: Long,
        cashboxName: String,
        paymentMethod: String,
        notes: String
    ) -> Unit
) {
    var type by remember { mutableStateOf(voucherType) }
    var voucherNumber by remember { mutableStateOf("") }
    var partyType by remember { mutableStateOf(if (voucherType == "RECEIPT") "CUSTOMER" else "SUPPLIER") }
    var selectedPartyId by remember {
        mutableStateOf(if (partyType == "CUSTOMER") customers.firstOrNull()?.id ?: 0L else suppliers.firstOrNull()?.id ?: 0L)
    }
    var customPartyName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCashboxId by remember { mutableStateOf(cashboxes.firstOrNull()?.id ?: 1L) }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    val resolvedPartyName = when (partyType) {
        "CUSTOMER" -> customers.find { it.id == selectedPartyId }?.name ?: customPartyName
        "SUPPLIER" -> suppliers.find { it.id == selectedPartyId }?.name ?: customPartyName
        else -> customPartyName.ifEmpty { "جهة عامة" }
    }
    val cbName = cashboxes.find { it.id == selectedCashboxId }?.name ?: "الصندوق الرئيسي"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (type == "RECEIPT") "إصدار سند قبض نقدية/بنك" else "إصدار سند صرف نقدية/بنك",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == "RECEIPT",
                            onClick = { type = "RECEIPT"; partyType = "CUSTOMER" },
                            label = { Text("سند قبض (وارد)") }
                        )
                        FilterChip(
                            selected = type == "PAYMENT",
                            onClick = { type = "PAYMENT"; partyType = "SUPPLIER" },
                            label = { Text("سند صرف (صادر)") }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = voucherNumber,
                        onValueChange = { voucherNumber = it },
                        label = { Text("رقم السند (فارغ للتوليد التلقائي)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = partyType == "CUSTOMER",
                            onClick = { partyType = "CUSTOMER" },
                            label = { Text("عميل") }
                        )
                        FilterChip(
                            selected = partyType == "SUPPLIER",
                            onClick = { partyType = "SUPPLIER" },
                            label = { Text("مورد") }
                        )
                        FilterChip(
                            selected = partyType == "GENERAL_EXPENSE",
                            onClick = { partyType = "GENERAL_EXPENSE" },
                            label = { Text("مصروف عام / أخرى") }
                        )
                    }
                }

                if (partyType == "CUSTOMER" && customers.isNotEmpty()) {
                    item {
                        Text(text = "العميل: $resolvedPartyName", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            customers.take(3).forEach { c ->
                                FilterChip(
                                    selected = selectedPartyId == c.id,
                                    onClick = { selectedPartyId = c.id },
                                    label = { Text(c.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                } else if (partyType == "SUPPLIER" && suppliers.isNotEmpty()) {
                    item {
                        Text(text = "المورد: $resolvedPartyName", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            suppliers.take(3).forEach { s ->
                                FilterChip(
                                    selected = selectedPartyId == s.id,
                                    onClick = { selectedPartyId = s.id },
                                    label = { Text(s.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = customPartyName,
                            onValueChange = { customPartyName = it },
                            label = { Text("اسم الجهة / المستفيد") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("المبلغ المطلوب ($currencySymbol) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("voucher_amount_input")
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("نقداً", "شبكة / نقاط بيع", "تحويل بنكي", "شيك").forEach { m ->
                            FilterChip(
                                selected = paymentMethod == m,
                                onClick = { paymentMethod = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("البيان / سبب المعاملة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(
                            voucherNumber.trim(),
                            type,
                            if (partyType == "GENERAL_EXPENSE") null else selectedPartyId,
                            resolvedPartyName,
                            partyType,
                            amt,
                            selectedCashboxId,
                            cbName,
                            paymentMethod,
                            notes.trim()
                        )
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.testTag("save_voucher_button")
            ) {
                Text("حفظ وترحيل السند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
