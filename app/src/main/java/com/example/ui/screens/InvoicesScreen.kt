package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.local.*
import com.example.ui.AccountingViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    invoiceType: String, // "SALE" or "PURCHASE"
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val invoices by viewModel.invoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val items by viewModel.items.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val cashboxes by viewModel.cashboxes.collectAsState()
    val currencySymbol by viewModel.activeCurrencySymbol.collectAsState()
    val activeTaxRate by viewModel.activeTaxRate.collectAsState()

    val isSale = invoiceType == "SALE"
    val screenTitle = if (isSale) "فواتير المبيعات" else "فواتير المشتريات"
    val partyLabel = if (isSale) "العميل" else "المورد"

    val filteredInvoices = remember(invoices, invoiceType) {
        invoices.filter { it.invoiceType == invoiceType }
    }

    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    var viewingInvoiceDetails by remember { mutableStateOf<Invoice?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateInvoiceDialog = true },
                containerColor = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_invoice_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إنشاء فاتورة جديدة")
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
                    title = "لا توجد $screenTitle مسجلة",
                    description = if (isSale) "قم بإنشاء فواتير بيع لتحديث رصيد العميل والمخزون وحساب الأرباح" else "قم بتسجيل فواتير الشراء لتحديث المخزون ومستحقات الموردين",
                    actionText = "إنشاء فاتورة جديدة",
                    onActionClick = { showCreateInvoiceDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredInvoices, key = { it.id }) { inv ->
                        InvoiceCard(
                            invoice = inv,
                            currencySymbol = currencySymbol,
                            onClick = { viewingInvoiceDetails = inv },
                            onCancel = {
                                viewModel.reverseInvoice(inv.id) {
                                    snackbarMessage = "تم إلغاء الفاتورة وعكس القيود والمخزون بنجاح"
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateInvoiceDialog) {
        CreateInvoiceDialog(
            isSale = isSale,
            customers = customers,
            suppliers = suppliers,
            items = items,
            warehouses = warehouses,
            cashboxes = cashboxes,
            currencySymbol = currencySymbol,
            defaultTaxRate = activeTaxRate,
            onDismiss = { showCreateInvoiceDialog = false },
            onConfirm = { num, pId, pName, whId, whName, cbId, cbName, itemsList, disc, tax, paid, method, notes ->
                viewModel.createInvoice(
                    invoiceNumber = num,
                    type = invoiceType,
                    partyId = pId,
                    partyName = pName,
                    warehouseId = whId,
                    warehouseName = whName,
                    cashboxId = cbId,
                    cashboxName = cbName,
                    itemsList = itemsList,
                    discount = disc,
                    taxRate = tax,
                    paidAmount = paid,
                    paymentMethod = method,
                    currencyCode = currencySymbol,
                    notes = notes,
                    onSuccess = {
                        showCreateInvoiceDialog = false
                        snackbarMessage = "تم إصدار الفاتورة وحفظ القيود بنجاح"
                    },
                    onError = { err -> snackbarMessage = err }
                )
            }
        )
    }

    viewingInvoiceDetails?.let { inv ->
        val invoiceItems by viewModel.getItemsForInvoice(inv.id).collectAsState(initial = emptyList())
        InvoiceDetailsDialog(
            invoice = inv,
            items = invoiceItems,
            currencySymbol = currencySymbol,
            onDismiss = { viewingInvoiceDetails = null }
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
fun InvoiceCard(
    invoice: Invoice,
    currencySymbol: String,
    onClick: () -> Unit,
    onCancel: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("invoice_card_${invoice.id}")
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = invoice.invoiceNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(invoice.date)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (invoice.paymentStatus) {
                        "مدفوعة" -> Color(0xFFE8F5E9)
                        "مدفوعة جزئياً" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFEBEE)
                    }
                ) {
                    Text(
                        text = invoice.paymentStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (invoice.paymentStatus) {
                            "مدفوعة" -> Color(0xFF2E7D32)
                            "مدفوعة جزئياً" -> Color(0xFFEF6C00)
                            else -> Color(0xFFC62828)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (invoice.partyType == "CUSTOMER") "العميل" else "المورد"}: ${invoice.partyName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${String.format(Locale.US, "%,.2f", invoice.totalAmount)} $currencySymbol",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (invoice.remainingAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "المسدد: ${String.format(Locale.US, "%,.2f", invoice.paidAmount)} $currencySymbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "المتبقي (آجل): ${String.format(Locale.US, "%,.2f", invoice.remainingAmount)} $currencySymbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
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
                    text = "طريقة الدفع: ${invoice.paymentMethod}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    TextButton(onClick = onClick) {
                        Text("عرض البنود", fontSize = 12.sp)
                    }
                    TextButton(onClick = onCancel) {
                        Text("إلغاء وعكس", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
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
    customers: List<Customer>,
    suppliers: List<Supplier>,
    items: List<Item>,
    warehouses: List<Warehouse>,
    cashboxes: List<Cashbox>,
    currencySymbol: String,
    defaultTaxRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (
        invoiceNumber: String,
        partyId: Long,
        partyName: String,
        warehouseId: Long,
        warehouseName: String,
        cashboxId: Long?,
        cashboxName: String,
        itemsList: List<InvoiceItem>,
        discount: Double,
        taxRate: Double,
        paidAmount: Double,
        paymentMethod: String,
        notes: String
    ) -> Unit
) {
    var invoiceNumber by remember { mutableStateOf("") }
    var selectedPartyId by remember {
        mutableStateOf(if (isSale) customers.firstOrNull()?.id ?: 0L else suppliers.firstOrNull()?.id ?: 0L)
    }
    var selectedWarehouseId by remember { mutableStateOf(warehouses.firstOrNull()?.id ?: 1L) }
    var selectedCashboxId by remember { mutableStateOf(cashboxes.firstOrNull()?.id ?: 1L) }

    var invoiceItems by remember { mutableStateOf(listOf<InvoiceItem>()) }
    var discountStr by remember { mutableStateOf("0") }
    var applyTax by remember { mutableStateOf(defaultTaxRate > 0.0) }
    var paidAmountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    // Item adding state
    var selectedItemId by remember { mutableStateOf(items.firstOrNull()?.id ?: 0L) }
    var itemQtyStr by remember { mutableStateOf("1") }
    var itemPriceStr by remember {
        val initialItem = items.firstOrNull()
        mutableStateOf(if (initialItem != null) (if (isSale) initialItem.salePrice else initialItem.purchasePrice).toString() else "0")
    }

    val subtotal = invoiceItems.sumOf { it.totalPrice }
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
    val taxRate = if (applyTax) defaultTaxRate.takeIf { it > 0 } ?: 0.15 else 0.0
    val taxAmount = afterDiscount * taxRate
    val grandTotal = afterDiscount + taxAmount

    // Default paid amount to grand total if empty
    val paidAmount = paidAmountStr.toDoubleOrNull() ?: grandTotal

    val partyName = if (isSale) {
        customers.find { it.id == selectedPartyId }?.name ?: "عميل نقدي"
    } else {
        suppliers.find { it.id == selectedPartyId }?.name ?: "مورد عام"
    }
    val whName = warehouses.find { it.id == selectedWarehouseId }?.name ?: "المستودع الرئيسي"
    val cbName = cashboxes.find { it.id == selectedCashboxId }?.name ?: "الصندوق الرئيسي"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSale) "إصدار فاتورة بيع جديدة" else "تسجيل فاتورة شراء جديدة",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("رقم الفاتورة (فارغ للتوليد التلقائي)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Party Selector
                item {
                    Text(
                        text = if (isSale) "العميل: $partyName" else "المورد: $partyName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (isSale && customers.size > 1) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            customers.take(3).forEach { cust ->
                                FilterChip(
                                    selected = selectedPartyId == cust.id,
                                    onClick = { selectedPartyId = cust.id },
                                    label = { Text(cust.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    } else if (!isSale && suppliers.size > 1) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            suppliers.take(3).forEach { supp ->
                                FilterChip(
                                    selected = selectedPartyId == supp.id,
                                    onClick = { selectedPartyId = supp.id },
                                    label = { Text(supp.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Add Items Section
                item {
                    Divider()
                    Text(text = "إضافة أصناف للفاتورة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (items.isEmpty()) {
                        Text(text = "يرجى إضافة أصناف أولاً من شاشة دليل الأصناف", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    } else {
                        val currentSelectedItem = items.find { it.id == selectedItemId } ?: items.first()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = currentSelectedItem.name, fontWeight = FontWeight.Medium, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = itemQtyStr,
                                onValueChange = { itemQtyStr = it },
                                label = { Text("كمية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(80.dp)
                            )
                            OutlinedTextField(
                                value = itemPriceStr,
                                onValueChange = { itemPriceStr = it },
                                label = { Text("السعر") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(90.dp)
                            )
                            IconButton(
                                onClick = {
                                    val qty = itemQtyStr.toDoubleOrNull() ?: 1.0
                                    val price = itemPriceStr.toDoubleOrNull() ?: 0.0
                                    if (qty > 0 && price >= 0) {
                                        invoiceItems = invoiceItems + InvoiceItem(
                                            invoiceId = 0,
                                            itemId = currentSelectedItem.id,
                                            itemName = currentSelectedItem.name,
                                            unit = currentSelectedItem.unit,
                                            quantity = qty,
                                            unitPrice = price,
                                            totalPrice = qty * price,
                                            purchaseCost = currentSelectedItem.purchasePrice
                                        )
                                        itemQtyStr = "1"
                                    }
                                }
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "إضافة بند", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Quick chip picker for other items
                        if (items.size > 1) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items.take(4).forEach { itm ->
                                    FilterChip(
                                        selected = selectedItemId == itm.id,
                                        onClick = {
                                            selectedItemId = itm.id
                                            itemPriceStr = (if (isSale) itm.salePrice else itm.purchasePrice).toString()
                                        },
                                        label = { Text(itm.name, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Items Table in Invoice
                if (invoiceItems.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "بنود الفاتورة (${invoiceItems.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                invoiceItems.forEachIndexed { index, bnd ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "${index + 1}. ${bnd.itemName} (${bnd.quantity} × ${bnd.unitPrice})", fontSize = 12.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "${String.format(Locale.US, "%,.2f", bnd.totalPrice)} $currencySymbol", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            IconButton(
                                                onClick = { invoiceItems = invoiceItems.filterIndexed { i, _ -> i != index } },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "حذف البند", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Financial Calculations
                item {
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountStr,
                            onValueChange = { discountStr = it },
                            label = { Text("الخصم") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = applyTax, onCheckedChange = { applyTax = it })
                            Text(text = "تطبيق ضريبة (${(defaultTaxRate * 100).toInt()}%)", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "المجموع الفرعي:", fontSize = 12.sp)
                                Text(text = "${String.format(Locale.US, "%,.2f", subtotal)} $currencySymbol", fontSize = 12.sp)
                            }
                            if (taxAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "مبلغ الضريبة:", fontSize = 12.sp)
                                    Text(text = "${String.format(Locale.US, "%,.2f", taxAmount)} $currencySymbol", fontSize = 12.sp)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "الإجمالي النهائي:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "${String.format(Locale.US, "%,.2f", grandTotal)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Payment Details
                item {
                    OutlinedTextField(
                        value = paidAmountStr,
                        onValueChange = { paidAmountStr = it },
                        label = { Text("المبلغ المدفوع حالياً (اتركه فارغاً للسداد الكامل)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("نقداً", "شبكة / بنك", "آجل").forEach { meth ->
                            FilterChip(
                                selected = paymentMethod == meth,
                                onClick = { paymentMethod = meth },
                                label = { Text(meth, fontSize = 11.sp) }
                            )
                        }
                    }
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
                    onConfirm(
                        invoiceNumber.trim(),
                        selectedPartyId,
                        partyName,
                        selectedWarehouseId,
                        whName,
                        selectedCashboxId,
                        cbName,
                        invoiceItems,
                        discount,
                        taxRate,
                        paidAmount,
                        paymentMethod,
                        notes.trim()
                    )
                },
                enabled = invoiceItems.isNotEmpty(),
                modifier = Modifier.testTag("submit_invoice_button")
            ) {
                Text("إصدار وحفظ الفاتورة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun InvoiceDetailsDialog(
    invoice: Invoice,
    items: List<InvoiceItem>,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "فاتورة ${if (invoice.invoiceType == "SALE") "بيع" else "شراء"}: ${invoice.invoiceNumber}", fontWeight = FontWeight.Bold)
                Text(text = dateFormat.format(Date(invoice.date)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(text = "${if (invoice.partyType == "CUSTOMER") "العميل" else "المورد"}: ${invoice.partyName}", fontWeight = FontWeight.Bold)
                    Text(text = "المستودع: ${invoice.warehouseName.ifEmpty { "الرئيسي" }}", fontSize = 12.sp)
                    Text(text = "طريقة الدفع: ${invoice.paymentMethod}", fontSize = 12.sp)
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                }

                items(items) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${item.itemName} (${item.quantity} ${item.unit} × ${item.unitPrice})", fontSize = 12.sp)
                        Text(text = "${String.format(Locale.US, "%,.2f", item.totalPrice)} $currencySymbol", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "المجموع الفرعي:", fontSize = 12.sp)
                        Text(text = "${String.format(Locale.US, "%,.2f", invoice.subtotal)} $currencySymbol", fontSize = 12.sp)
                    }
                    if (invoice.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "الخصم:", fontSize = 12.sp)
                            Text(text = "-${String.format(Locale.US, "%,.2f", invoice.discount)} $currencySymbol", fontSize = 12.sp)
                        }
                    }
                    if (invoice.taxAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "ضريبة القيمة المضافة:", fontSize = 12.sp)
                            Text(text = "+${String.format(Locale.US, "%,.2f", invoice.taxAmount)} $currencySymbol", fontSize = 12.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "الإجمالي النهائي:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "${String.format(Locale.US, "%,.2f", invoice.totalAmount)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "المبلغ المسدد:", fontSize = 12.sp)
                        Text(text = "${String.format(Locale.US, "%,.2f", invoice.paidAmount)} $currencySymbol", fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "المتبقي:", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        Text(text = "${String.format(Locale.US, "%,.2f", invoice.remainingAmount)} $currencySymbol", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}
