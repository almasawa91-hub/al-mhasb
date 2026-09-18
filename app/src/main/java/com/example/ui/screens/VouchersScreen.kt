package com.example.ui.screens

import androidx.compose.foundation.background
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Receipts (قبض), 2: Payments (صرف)
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredVouchers = remember(vouchers, selectedTab) {
        when (selectedTab) {
            1 -> vouchers.filter { it.type == "RECEIPT" }
            2 -> vouchers.filter { it.type == "PAYMENT" }
            else -> vouchers
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_voucher_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "سند جديد")
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("جميع السندات") }
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
                    title = "لا توجد سندات مسجلة",
                    description = "سجل سندات القبض للمبالغ المستلمة وسندات الصرف للنفقات والمصروفات",
                    actionText = "إصدار سند جديد",
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredVouchers, key = { it.id }) { voucher ->
                        VoucherCard(voucher = voucher, onDelete = { viewModel.deleteVoucher(voucher) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVoucherDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { num, type, partyName, partyType, amount, payMethod, notes ->
                viewModel.createVoucher(num, type, partyName, partyType, amount, payMethod, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun VoucherCard(voucher: Voucher, onDelete: () -> Unit) {
    val isReceipt = voucher.type == "RECEIPT"
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val dateStr = remember(voucher.date) { dateFormat.format(Date(voucher.date)) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("voucher_card_${voucher.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isReceipt) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isReceipt) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isReceipt) "سند قبض: ${voucher.voucherNumber}" else "سند صرف: ${voucher.voucherNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = "المستفيد/الدافع: ${voucher.partyName}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (voucher.notes.isNotEmpty()) {
                    Text(text = "البيان: ${voucher.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.US, "%,.2f", voucher.amount)} ر.س",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isReceipt) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddVoucherDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        num: String,
        type: String,
        partyName: String,
        partyType: String,
        amount: Double,
        payMethod: String,
        notes: String
    ) -> Unit
) {
    var type by remember { mutableStateOf("RECEIPT") } // RECEIPT or PAYMENT
    var voucherNumber by remember { mutableStateOf("") }
    var partyName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "RECEIPT") "إصدار سند قبض جديد" else "إصدار سند صرف جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "RECEIPT",
                        onClick = { type = "RECEIPT" },
                        label = { Text("سند قبض (استلام)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "PAYMENT",
                        onClick = { type = "PAYMENT" },
                        label = { Text("سند صرف (دفع)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text(if (type == "RECEIPT") "استلمنا من السيد / الجهة *" else "صرفنا للسيد / الحساب *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("voucher_party_name_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ بالأرقام (ر.س) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("voucher_amount_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("وذلك عن / البيان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (partyName.isNotBlank() && amt > 0) {
                        onConfirm(
                            voucherNumber,
                            type,
                            partyName,
                            "GENERAL",
                            amt,
                            paymentMethod,
                            notes
                        )
                    }
                },
                enabled = partyName.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("confirm_add_voucher_button")
            ) {
                Text("حفظ وترحيل السند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
