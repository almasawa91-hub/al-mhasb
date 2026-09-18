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
import com.example.data.local.Invoice
import com.example.data.local.InvoiceItem
import com.example.data.local.Item
import com.example.ui.AccountingViewModel
import com.example.ui.components.EmptyStateView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    type: String, // "SALE" or "PURCHASE"
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val isSale = type == "SALE"
    val title = if (isSale) "فواتير المبيعات" else "فواتير المشتريات"
    val allInvoices by viewModel.invoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val availableItems by viewModel.items.collectAsState()

    val filteredInvoices = remember(allInvoices, type) {
        allInvoices.filter { it.invoiceType == type }
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                modifier = Modifier.testTag(if (isSale) "new_sale_invoice_fab" else "new_purchase_invoice_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "فاتورة جديدة")
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
            if (filteredInvoices.isEmpty()) {
                EmptyStateView(
                    icon = if (isSale) Icons.Default.PointOfSale else Icons.Default.ShoppingCart,
                    title = if (isSale) "لا توجد فواتير مبيعات مسجلة" else "لا توجد فواتير مشتريات مسجلة",
                    description = if (isSale) "أنشئ فاتورة بيع جديدة لخصم المنتجات من المخزن وإثبات الإيرادات" else "سجل فاتورة شراء لإضافة البضائع إلى المخزون",
                    actionText = if (isSale) "إنشاء فاتورة بيع" else "إنشاء فاتورة شراء",
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        InvoiceCard(invoice = invoice, onDelete = { viewModel.deleteInvoice(invoice) })
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateInvoiceDialog(
            isSale = isSale,
            parties = if (isSale) customers.map { it.id to it.name } else suppliers.map { it.id to it.name },
            availableItems = availableItems,
            onDismiss = { showCreateDialog = false },
            onConfirm = { invNumber, partyId, partyName, itemsList, discount, paid, payMethod, notes ->
                viewModel.createInvoice(
                    invoiceNumber = invNumber,
                    type = type,
                    partyId = partyId,
                    partyName = partyName,
                    itemsList = itemsList,
                    discount = discount,
                    paidAmount = paid,
                    paymentMethod = payMethod,
                    notes = notes
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun InvoiceCard(invoice: Invoice, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val dateStr = remember(invoice.date) { dateFormat.format(Date(invoice.date)) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("invoice_card_${invoice.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = invoice.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = invoice.paymentStatus,
                        fontSize = 11.sp,
                        color = when (invoice.paymentStatus) {
                            "مدفوعة" -> Color(0xFF2E7D32)
                            "مدفوعة جزئياً" -> Color(0xFFEF6C00)
                            else -> Color(0xFFC62828)
                        },
                        modifier = Modifier
                            .background(
                                color = when (invoice.paymentStatus) {
                                    "مدفوعة" -> Color(0xFFE8F5E9)
                                    "مدفوعة جزئياً" -> Color(0xFFFFF3E0)
                                    else -> Color(0xFFFFEBEE)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${if (invoice.partyType == "CUSTOMER") "العميل" else "المورد"}: ${invoice.partyName}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(text = "الإجمالي شامل الضريبة 15%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%,.2f", invoice.totalAmount)} ر.س",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (invoice.remainingAmount > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "المتبقي / آجل", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        Text(
                            text = "${String.format(Locale.US, "%,.2f", invoice.remainingAmount)} ر.س",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceDialog(
    isSale: Boolean,
    parties: List<Pair<Long, String>>,
    availableItems: List<Item>,
    onDismiss: () -> Unit,
    onConfirm: (
        invNumber: String,
        partyId: Long,
        partyName: String,
        itemsList: List<InvoiceItem>,
        discount: Double,
        paid: Double,
        payMethod: String,
        notes: String
    ) -> Unit
) {
    var invNumber by remember { mutableStateOf("INV-${System.currentTimeMillis() % 100000}") }
    var selectedPartyId by remember { mutableStateOf(parties.firstOrNull()?.first ?: 0L) }
    var selectedPartyName by remember { mutableStateOf(parties.firstOrNull()?.second ?: "") }
    var manualPartyName by remember { mutableStateOf("") }

    val invoiceItems = remember { mutableStateListOf<InvoiceItem>() }
    var selectedItemId by remember { mutableStateOf(availableItems.firstOrNull()?.id ?: 0L) }
    var itemQty by remember { mutableStateOf("1") }
    var discountText by remember { mutableStateOf("0") }
    var paidText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    val subtotal = invoiceItems.sumOf { it.totalPrice }
    val discountVal = discountText.toDoubleOrNull() ?: 0.0
    val afterDiscount = (subtotal - discountVal).coerceAtLeast(0.0)
    val vatAmount = afterDiscount * 0.15
    val totalWithVat = afterDiscount + vatAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSale) "إنشاء فاتورة بيع جديدة" else "إنشاء فاتورة شراء جديدة", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = invNumber,
                        onValueChange = { invNumber = it },
                        label = { Text("رقم الفاتورة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Party selection
                item {
                    if (parties.isNotEmpty()) {
                        Text(text = if (isSale) "اختر العميل:" else "اختر المورد:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            parties.take(3).forEach { (id, name) ->
                                FilterChip(
                                    selected = selectedPartyId == id,
                                    onClick = {
                                        selectedPartyId = id
                                        selectedPartyName = name
                                    },
                                    label = { Text(name, fontSize = 12.sp) }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = manualPartyName,
                            onValueChange = { manualPartyName = it },
                            label = { Text(if (isSale) "اسم العميل (نقدي أو جديد)" else "اسم المورد") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Items adder section
                item {
                    Divider()
                    Text(text = "إضافة أصناف للفاتورة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (availableItems.isNotEmpty()) {
                        val currentItem = availableItems.find { it.id == selectedItemId } ?: availableItems.first()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currentItem.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = itemQty,
                                onValueChange = { itemQty = it },
                                label = { Text("الكمية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.width(80.dp)
                            )
                            Button(
                                onClick = {
                                    val qty = itemQty.toDoubleOrNull() ?: 1.0
                                    val price = if (isSale) currentItem.salePrice else currentItem.purchasePrice
                                    invoiceItems.add(
                                        InvoiceItem(
                                            invoiceId = 0,
                                            itemId = currentItem.id,
                                            itemName = currentItem.name,
                                            unit = currentItem.unit,
                                            quantity = qty,
                                            unitPrice = price,
                                            totalPrice = qty * price
                                        )
                                    )
                                    itemQty = "1"
                                }
                            ) {
                                Text("إضافة")
                            }
                        }
                    } else {
                        Text("يرجى إضافة أصناف أولاً من شاشة دليل الأصناف", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }

                // Added items list
                items(invoiceItems) { itm ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(itm.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${itm.quantity} × ${itm.unitPrice} = ${itm.totalPrice} ر.س", fontSize = 12.sp)
                            }
                            IconButton(onClick = { invoiceItems.remove(itm) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إزالة", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Financial calculations preview
                item {
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("الخصم") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paidText,
                            onValueChange = { paidText = it },
                            label = { Text("المدفوع") },
                            placeholder = { Text("${String.format(Locale.US, "%.2f", totalWithVat)}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "المجموع: ${String.format(Locale.US, "%.2f", subtotal)} | الضريبة (15%): ${String.format(Locale.US, "%.2f", vatAmount)} | الإجمالي: ${String.format(Locale.US, "%.2f", totalWithVat)} ر.س",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات الفاتورة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPartyName = if (parties.isNotEmpty()) selectedPartyName else manualPartyName.ifEmpty { "عميل نقدي" }
                    val paidAmount = paidText.toDoubleOrNull() ?: totalWithVat
                    onConfirm(
                        invNumber,
                        selectedPartyId,
                        finalPartyName,
                        invoiceItems.toList(),
                        discountVal,
                        paidAmount,
                        paymentMethod,
                        notes
                    )
                },
                enabled = invoiceItems.isNotEmpty(),
                modifier = Modifier.testTag("confirm_create_invoice_button")
            ) {
                Text("حفظ الفاتورة وترحيلها")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
