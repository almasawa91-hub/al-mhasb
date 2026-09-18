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
import com.example.data.local.Item
import com.example.data.local.StockMovement
import com.example.data.local.Warehouse
import com.example.ui.AccountingViewModel
import com.example.ui.components.EmptyStateView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val currencySymbol by viewModel.activeCurrencySymbol.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Item?>(null) }
    var viewingMovementsItem by remember { mutableStateOf<Item?>(null) }
    var adjustingStockItem by remember { mutableStateOf<Item?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter { it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة صنف")
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
                    .testTag("search_item_input"),
                placeholder = { Text("بحث باسم الصنف، الباركود أو التصنيف...") },
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

            if (filteredItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = if (searchQuery.isEmpty()) "لا توجد أصناف في المستودع" else "لا توجد أصناف مطابقة",
                    description = if (searchQuery.isEmpty()) "أضف الأصناف وحدد أسعار الشراء والبيع والباركود وحساب المخزون" else "تحقق من نص البحث",
                    actionText = if (searchQuery.isEmpty()) "إضافة صنف جديد" else null,
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            currencySymbol = currencySymbol,
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.deleteItem(item) },
                            onMovementsClick = { viewingMovementsItem = item },
                            onAdjustStock = { adjustingStockItem = item }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ItemDialog(
            title = "إضافة صنف جديد",
            item = null,
            warehouses = warehouses,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, barcode, category, unit, pPrice, sPrice, stock, minAlert, whId, desc ->
                viewModel.addItem(name, barcode, category, unit, pPrice, sPrice, stock, minAlert, whId, desc)
                showAddDialog = false
            }
        )
    }

    editingItem?.let { itm ->
        ItemDialog(
            title = "تعديل بيانات الصنف",
            item = itm,
            warehouses = warehouses,
            onDismiss = { editingItem = null },
            onConfirm = { name, barcode, category, unit, pPrice, sPrice, _, minAlert, whId, desc ->
                viewModel.updateItem(
                    itm.copy(
                        name = name,
                        barcode = barcode,
                        category = category,
                        unit = unit,
                        purchasePrice = pPrice,
                        salePrice = sPrice,
                        minStockAlert = minAlert,
                        defaultWarehouseId = whId,
                        description = desc
                    )
                )
                editingItem = null
            }
        )
    }

    viewingMovementsItem?.let { itm ->
        val movements by viewModel.getStockMovementsForItem(itm.id).collectAsState(initial = emptyList())
        StockMovementsDialog(
            item = itm,
            movements = movements,
            onDismiss = { viewingMovementsItem = null }
        )
    }

    adjustingStockItem?.let { itm ->
        StockAdjustmentDialog(
            item = itm,
            warehouses = warehouses,
            onDismiss = { adjustingStockItem = null },
            onConfirm = { whId, delta, notes ->
                viewModel.adjustItemStock(itm.id, whId, delta, notes)
                adjustingStockItem = null
            }
        )
    }
}

@Composable
fun ItemCard(
    item: Item,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMovementsClick: () -> Unit,
    onAdjustStock: () -> Unit
) {
    val isLowStock = item.currentStock <= item.minStockAlert

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("item_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLowStock) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.barcode.isNotEmpty()) {
                            Text(text = "باركود: ${item.barcode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = "التصنيف: ${item.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // Pricing & Stock Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "سعر البيع: ${String.format(Locale.US, "%,.2f", item.salePrice)} $currencySymbol", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "سعر التكلفة: ${String.format(Locale.US, "%,.2f", item.purchasePrice)} $currencySymbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "المخزون: ", fontSize = 12.sp)
                        Text(
                            text = "${item.currentStock} ${item.unit}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isLowStock) {
                        Text(text = "حد إعادة الطلب: ${item.minStockAlert}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAdjustStock) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تعديل مخزون", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onMovementsClick) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "حركة الصنف", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDialog(
    title: String,
    item: Item?,
    warehouses: List<Warehouse>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, barcode: String, category: String, unit: String, pPrice: Double, sPrice: Double, stock: Double, minAlert: Double, whId: Long?, desc: String) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var barcode by remember { mutableStateOf(item?.barcode ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "عام") }
    var unit by remember { mutableStateOf(item?.unit ?: "قطعة") }
    var purchasePrice by remember { mutableStateOf(item?.purchasePrice?.takeIf { it > 0 }?.toString() ?: "") }
    var salePrice by remember { mutableStateOf(item?.salePrice?.takeIf { it > 0 }?.toString() ?: "") }
    var currentStock by remember { mutableStateOf(item?.currentStock?.toString() ?: "") }
    var minStockAlert by remember { mutableStateOf(item?.minStockAlert?.toString() ?: "5") }
    var selectedWarehouseId by remember { mutableStateOf(item?.defaultWarehouseId ?: warehouses.firstOrNull()?.id) }
    var description by remember { mutableStateOf(item?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الصنف *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("item_name_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("الباركود / رمز التتبع") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("التصنيف") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("الوحدة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { purchasePrice = it },
                            label = { Text("سعر الشراء") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = salePrice,
                            onValueChange = { salePrice = it },
                            label = { Text("سعر البيع") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (item == null) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = currentStock,
                                onValueChange = { currentStock = it },
                                label = { Text("الكمية الافتتاحية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = minStockAlert,
                                onValueChange = { minStockAlert = it },
                                label = { Text("حد التنبيه") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("مواصفات / تفاصيل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            barcode.trim(),
                            category.trim().ifEmpty { "عام" },
                            unit.trim().ifEmpty { "قطعة" },
                            purchasePrice.toDoubleOrNull() ?: 0.0,
                            salePrice.toDoubleOrNull() ?: 0.0,
                            currentStock.toDoubleOrNull() ?: 0.0,
                            minStockAlert.toDoubleOrNull() ?: 5.0,
                            selectedWarehouseId,
                            description.trim()
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_item_button")
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
fun StockMovementsDialog(
    item: Item,
    movements: List<StockMovement>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "حركة المخزون للصنف: ${item.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "الرصيد الفعلي الحالي: ${item.currentStock} ${item.unit}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            if (movements.isEmpty()) {
                Text(text = "لا توجد حركات مخزنية مسجلة لهذا الصنف بعد.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movements) { mov ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = dateFormat.format(Date(mov.date)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (mov.quantity > 0) "+${mov.quantity}" else "${mov.quantity}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mov.quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(text = mov.notes.ifEmpty { mov.movementType }, fontSize = 12.sp)
                                Text(text = "الرصيد بعد الحركة: ${mov.balanceAfter}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun StockAdjustmentDialog(
    item: Item,
    warehouses: List<Warehouse>,
    onDismiss: () -> Unit,
    onConfirm: (warehouseId: Long, delta: Double, notes: String) -> Unit
) {
    var deltaStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedWhId by remember { mutableStateOf(item.defaultWarehouseId ?: warehouses.firstOrNull()?.id ?: 1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "تسوية مخزون: ${item.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "الرصيد الحالي: ${item.currentStock} ${item.unit}", fontSize = 13.sp)
                OutlinedTextField(
                    value = deltaStr,
                    onValueChange = { deltaStr = it },
                    label = { Text("الكمية المضافة أو المخصومة (مثلاً +5 أو -2)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("سبب التسوية / ملاحظات الجرد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val delta = deltaStr.toDoubleOrNull() ?: 0.0
                    if (delta != 0.0) {
                        onConfirm(selectedWhId, delta, notes.trim())
                    }
                },
                enabled = deltaStr.toDoubleOrNull() != null && deltaStr.toDoubleOrNull() != 0.0
            ) {
                Text("تطبيق التسوية")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
