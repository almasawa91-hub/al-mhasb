package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class DashboardStats(
    val totalSales: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalReceipts: Double = 0.0,
    val totalPayments: Double = 0.0,
    val totalCustomerReceivables: Double = 0.0,
    val totalSupplierPayables: Double = 0.0,
    val totalCashboxBalance: Double = 0.0,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val itemCount: Int = 0,
    val netCashFlow: Double = 0.0,
    val currencySymbol: String = "ر.س"
)

sealed class BackupRestoreResult {
    data class Success(val message: String) : BackupRestoreResult()
    data class Error(val message: String) : BackupRestoreResult()
}

class AccountingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AccountingRepository

    val organizationSettings: StateFlow<OrganizationSettings?>

    init {
        val db = AccountingDatabase.getDatabase(application)
        repository = AccountingRepository(db, db.accountingDao())

        organizationSettings = repository.organizationSettings
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

    // Currencies
    val currencies: StateFlow<List<Currency>> = repository.allCurrencies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Currency Symbol
    val activeCurrencySymbol: StateFlow<String> = organizationSettings
        .map { it?.currencySymbol?.ifEmpty { "ر.س" } ?: "ر.س" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ر.س")

    // Active Tax Rate
    val activeTaxRate: StateFlow<Double> = organizationSettings
        .map { if (it?.isTaxEnabled == true) it.defaultTaxRate else 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Customers
    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Suppliers
    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Items & Inventory
    val items: StateFlow<List<Item>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockItems: StateFlow<List<Item>> = repository.lowStockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStockMovements: StateFlow<List<StockMovement>> = repository.allStockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Invoices
    val invoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vouchers
    val vouchers: StateFlow<List<Voucher>> = repository.allVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Warehouses & Cashboxes
    val warehouses: StateFlow<List<Warehouse>> = repository.allWarehouses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashboxes: StateFlow<List<Cashbox>> = repository.allCashboxes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Stats Stream - 100% computed from real database queries
    private val financialTotalsFlow = combine(
        repository.totalSales,
        repository.totalPurchases,
        repository.totalReceipts,
        repository.totalPayments
    ) { sales, purchases, receipts, payments ->
        listOf(sales ?: 0.0, purchases ?: 0.0, receipts ?: 0.0, payments ?: 0.0)
    }

    private val countsFlow = combine(
        repository.customerCount,
        repository.supplierCount,
        repository.itemCount
    ) { custCount, suppCount, itmCount ->
        Triple(custCount, suppCount, itmCount)
    }

    private val balancesFlow = combine(
        repository.totalCustomerReceivables,
        repository.totalSupplierPayables,
        repository.totalCashboxBalance
    ) { receivables, payables, cashBalance ->
        Triple(receivables ?: 0.0, payables ?: 0.0, cashBalance ?: 0.0)
    }

    val dashboardStats: StateFlow<DashboardStats> = combine(
        financialTotalsFlow,
        countsFlow,
        balancesFlow,
        activeCurrencySymbol
    ) { financialList, counts, balances, symbol ->
        val sales = financialList[0]
        val purchases = financialList[1]
        val receipts = financialList[2]
        val payments = financialList[3]

        val custCount = counts.first
        val suppCount = counts.second
        val itmCount = counts.third

        val receivables = balances.first
        val payables = balances.second
        val cashBalance = balances.third

        DashboardStats(
            totalSales = sales,
            totalPurchases = purchases,
            totalReceipts = receipts,
            totalPayments = payments,
            totalCustomerReceivables = receivables,
            totalSupplierPayables = payables,
            totalCashboxBalance = cashBalance,
            customerCount = custCount,
            supplierCount = suppCount,
            itemCount = itmCount,
            netCashFlow = receipts - payments,
            currencySymbol = symbol
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Backup & Restore State
    private val _backupStatus = MutableSharedFlow<BackupRestoreResult>()
    val backupStatus: SharedFlow<BackupRestoreResult> = _backupStatus.asSharedFlow()

    // ---------------- Actions & Mutators ----------------

    // Organization Settings
    fun updateOrganizationSettings(settings: OrganizationSettings) {
        viewModelScope.launch {
            repository.saveOrganizationSettings(settings.copy(isConfigured = true, updatedAt = System.currentTimeMillis()))
        }
    }

    // Customers
    fun addCustomer(name: String, phone: String, email: String, address: String, creditLimit: Double, openingBalance: Double, notes: String = "") {
        viewModelScope.launch {
            repository.addCustomer(
                Customer(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    address = address.trim(),
                    creditLimit = creditLimit,
                    openingBalance = openingBalance,
                    notes = notes.trim()
                )
            )
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch { repository.updateCustomer(customer) }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch { repository.deleteCustomer(customer) }
    }

    fun getCustomerTransactions(customerId: Long): Flow<List<CustomerTransaction>> =
        repository.getCustomerTransactions(customerId)

    fun getCustomerBalanceFlow(customerId: Long): Flow<Double?> =
        repository.getCustomerBalanceFlow(customerId)

    // Suppliers
    fun addSupplier(name: String, phone: String, email: String, address: String, companyName: String, openingBalance: Double, notes: String = "") {
        viewModelScope.launch {
            repository.addSupplier(
                Supplier(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    address = address.trim(),
                    companyName = companyName.trim(),
                    openingBalance = openingBalance,
                    notes = notes.trim()
                )
            )
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch { repository.updateSupplier(supplier) }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch { repository.deleteSupplier(supplier) }
    }

    fun getSupplierTransactions(supplierId: Long): Flow<List<SupplierTransaction>> =
        repository.getSupplierTransactions(supplierId)

    fun getSupplierBalanceFlow(supplierId: Long): Flow<Double?> =
        repository.getSupplierBalanceFlow(supplierId)

    // Items
    fun addItem(
        name: String,
        barcode: String,
        category: String,
        unit: String,
        purchasePrice: Double,
        salePrice: Double,
        initialStock: Double,
        minAlert: Double,
        warehouseId: Long?,
        description: String = ""
    ) {
        viewModelScope.launch {
            repository.addItem(
                Item(
                    name = name.trim(),
                    barcode = barcode.trim(),
                    category = category.trim().ifEmpty { "عام" },
                    unit = unit.trim().ifEmpty { "قطعة" },
                    purchasePrice = purchasePrice,
                    salePrice = salePrice,
                    currentStock = initialStock,
                    minStockAlert = minAlert,
                    defaultWarehouseId = warehouseId,
                    description = description.trim()
                )
            )
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch { repository.updateItem(item) }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun adjustItemStock(itemId: Long, warehouseId: Long, delta: Double, notes: String) {
        viewModelScope.launch {
            repository.adjustItemStock(itemId, warehouseId, delta, notes)
        }
    }

    fun getStockMovementsForItem(itemId: Long): Flow<List<StockMovement>> =
        repository.getStockMovementsForItem(itemId)

    // Warehouses
    fun addWarehouse(name: String, code: String, location: String, keeperName: String, isDefault: Boolean = false) {
        viewModelScope.launch {
            repository.insertWarehouse(
                Warehouse(
                    name = name.trim(),
                    code = code.trim(),
                    location = location.trim(),
                    keeperName = keeperName.trim(),
                    isDefault = isDefault
                )
            )
        }
    }

    fun updateWarehouse(warehouse: Warehouse) {
        viewModelScope.launch { repository.updateWarehouse(warehouse) }
    }

    fun deleteWarehouse(warehouse: Warehouse) {
        viewModelScope.launch { repository.deleteWarehouse(warehouse) }
    }

    // Cashboxes & Banks
    fun addCashbox(name: String, type: String, accountNumber: String, openingBalance: Double, currency: String = "SAR", isDefault: Boolean = false) {
        viewModelScope.launch {
            repository.insertCashbox(
                Cashbox(
                    name = name.trim(),
                    type = type.trim(),
                    accountNumber = accountNumber.trim(),
                    openingBalance = openingBalance,
                    currentBalance = openingBalance,
                    currency = currency,
                    isDefault = isDefault
                )
            )
        }
    }

    fun updateCashbox(cashbox: Cashbox) {
        viewModelScope.launch { repository.updateCashbox(cashbox) }
    }

    fun deleteCashbox(cashbox: Cashbox) {
        viewModelScope.launch { repository.deleteCashbox(cashbox) }
    }

    fun getTransactionsForCashbox(cashboxId: Long): Flow<List<CashTransaction>> =
        repository.getTransactionsForCashbox(cashboxId)

    // Invoices (Sales & Purchases)
    fun createInvoice(
        invoiceNumber: String,
        type: String, // "SALE" or "PURCHASE"
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
        currencyCode: String,
        notes: String,
        onSuccess: (Long) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (itemsList.isEmpty()) {
                onError("لا يمكن حفظ فاتورة بدون أصناف")
                return@launch
            }
            if (warehouseId <= 0) {
                onError("يرجى اختيار المستودع")
                return@launch
            }

            try {
                val subtotal = itemsList.sumOf { it.totalPrice }
                val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
                val taxAmount = afterDiscount * taxRate
                val totalAmount = afterDiscount + taxAmount
                val remaining = (totalAmount - paidAmount).coerceAtLeast(0.0)

                val generatedNum = invoiceNumber.trim().ifEmpty {
                    val prefix = if (type == "SALE") "INV-S" else "INV-P"
                    "$prefix-${System.currentTimeMillis() % 100000}"
                }

                val invoice = Invoice(
                    invoiceNumber = generatedNum,
                    invoiceType = type,
                    partyType = if (type == "SALE") "CUSTOMER" else "SUPPLIER",
                    partyId = partyId,
                    partyName = partyName,
                    warehouseId = warehouseId,
                    warehouseName = warehouseName,
                    cashboxId = if (paidAmount > 0) cashboxId else null,
                    cashboxName = if (paidAmount > 0) cashboxName else "",
                    subtotal = subtotal,
                    discount = discount,
                    taxRate = taxRate,
                    taxAmount = taxAmount,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    remainingAmount = remaining,
                    paymentStatus = if (remaining <= 0) "مدفوعة" else if (paidAmount > 0) "مدفوعة جزئياً" else "آجلة",
                    paymentMethod = paymentMethod,
                    currencyCode = currencyCode,
                    notes = notes
                )

                val id = repository.createInvoiceWithTransaction(invoice, itemsList)
                onSuccess(id)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "حدث خطأ أثناء حفظ الفاتورة")
            }
        }
    }

    fun reverseInvoice(invoiceId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reverseInvoice(invoiceId)
            onDone()
        }
    }

    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItem>> =
        repository.getItemsForInvoice(invoiceId)

    // Vouchers (Receipts & Payments)
    fun createVoucher(
        voucherNumber: String,
        type: String, // "RECEIPT" or "PAYMENT"
        partyId: Long?,
        partyName: String,
        partyType: String,
        amount: Double,
        cashboxId: Long,
        cashboxName: String,
        paymentMethod: String,
        currencyCode: String,
        notes: String,
        onSuccess: (Long) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (amount <= 0.0) {
                onError("المبلغ يجب أن يكون أكبر من الصفر")
                return@launch
            }
            if (cashboxId <= 0) {
                onError("يرجى اختيار الصندوق أو الحساب البنكي")
                return@launch
            }

            try {
                val generatedNum = voucherNumber.trim().ifEmpty {
                    val prefix = if (type == "RECEIPT") "REC" else "PAY"
                    "$prefix-${System.currentTimeMillis() % 100000}"
                }

                val voucher = Voucher(
                    voucherNumber = generatedNum,
                    type = type,
                    partyId = partyId,
                    partyName = partyName.trim(),
                    partyType = partyType,
                    amount = amount,
                    cashboxId = cashboxId,
                    cashboxName = cashboxName,
                    paymentMethod = paymentMethod,
                    currencyCode = currencyCode,
                    notes = notes.trim()
                )

                val id = repository.createVoucherWithTransaction(voucher)
                onSuccess(id)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "فشل حفظ السند")
            }
        }
    }

    fun reverseVoucher(voucherId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reverseVoucher(voucherId)
            onDone()
        }
    }

    // ---------------- Real SQLite Backup & Restore Implementation ----------------
    fun exportDatabaseBackup(context: Context, targetUri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val dbFile = context.getDatabasePath("al_muhasib.db")
                    if (!dbFile.exists()) {
                        _backupStatus.emit(BackupRestoreResult.Error("ملف قاعدة البيانات غير موجود على الجهاز"))
                        return@withContext
                    }

                    context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                        FileInputStream(dbFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    _backupStatus.emit(BackupRestoreResult.Success("تم تصدير نسخة احتياطية حقيقية بنجاح إلى الملف المحدد"))
                } catch (e: Exception) {
                    _backupStatus.emit(BackupRestoreResult.Error("فشل تصدير النسخة الاحتياطية: ${e.localizedMessage}"))
                }
            }
        }
    }

    fun restoreDatabaseBackup(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val dbFile = context.getDatabasePath("al_muhasib.db")
                    val tempRestoreFile = File(context.cacheDir, "temp_restore.db")

                    // Copy imported file to temp
                    context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                        FileOutputStream(tempRestoreFile).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    // Simple SQLite header validation (first 16 bytes: "SQLite format 3\000")
                    val header = ByteArray(16)
                    FileInputStream(tempRestoreFile).use { it.read(header) }
                    val headerString = String(header, 0, 15)
                    if (headerString != "SQLite format 3") {
                        tempRestoreFile.delete()
                        _backupStatus.emit(BackupRestoreResult.Error("الملف المحدد ليس ملف قاعدة بيانات SQLite صالح أو أنه تالف"))
                        return@withContext
                    }

                    // Valid DB -> replace target
                    dbFile.parentFile?.mkdirs()
                    FileInputStream(tempRestoreFile).use { inStream ->
                        FileOutputStream(dbFile).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    tempRestoreFile.delete()

                    _backupStatus.emit(BackupRestoreResult.Success("تمت استعادة قاعدة البيانات بنجاح! يرجى إعادة فتح التطبيق لتحديث البيانات المستعادة"))
                } catch (e: Exception) {
                    _backupStatus.emit(BackupRestoreResult.Error("فشل استعادة النسخة الاحتياطية: ${e.localizedMessage}"))
                }
            }
        }
    }
}
