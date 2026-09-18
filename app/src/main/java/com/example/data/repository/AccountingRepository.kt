package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AccountingRepository(
    private val database: AccountingDatabase,
    private val dao: AccountingDao
) {

    // ---------------- Organization Settings ----------------
    val organizationSettings: Flow<OrganizationSettings?> = dao.getOrganizationSettings()
    suspend fun getOrganizationSettingsOnce(): OrganizationSettings? = dao.getOrganizationSettingsOnce()
    suspend fun saveOrganizationSettings(settings: OrganizationSettings) {
        dao.insertOrUpdateOrganizationSettings(settings)
    }

    // ---------------- Customers ----------------
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val customerCount: Flow<Int> = dao.getCustomerCount()
    val totalCustomerReceivables: Flow<Double?> = dao.getTotalCustomerReceivables()

    suspend fun getCustomerById(id: Long): Customer? = dao.getCustomerById(id)

    suspend fun addCustomer(customer: Customer): Long = database.withTransaction {
        val customerId = dao.insertCustomer(customer)
        if (customer.openingBalance != 0.0) {
            // Opening balance transaction in customer ledger
            dao.insertCustomerTransaction(
                CustomerTransaction(
                    customerId = customerId,
                    transactionType = "OPENING",
                    referenceNumber = "OP-$customerId",
                    description = "رصيد افتتاحي",
                    debit = if (customer.openingBalance > 0) customer.openingBalance else 0.0,
                    credit = if (customer.openingBalance < 0) -customer.openingBalance else 0.0,
                    balanceAfter = customer.openingBalance
                )
            )
        }
        customerId
    }

    suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = database.withTransaction {
        dao.deleteCustomer(customer)
    }

    fun getCustomerTransactions(customerId: Long): Flow<List<CustomerTransaction>> =
        dao.getCustomerTransactions(customerId)

    fun getCustomerBalanceFlow(customerId: Long): Flow<Double?> =
        dao.getCustomerBalanceFlow(customerId)

    // ---------------- Suppliers ----------------
    val allSuppliers: Flow<List<Supplier>> = dao.getAllSuppliers()
    val supplierCount: Flow<Int> = dao.getSupplierCount()
    val totalSupplierPayables: Flow<Double?> = dao.getTotalSupplierPayables()

    suspend fun getSupplierById(id: Long): Supplier? = dao.getSupplierById(id)

    suspend fun addSupplier(supplier: Supplier): Long = database.withTransaction {
        val supplierId = dao.insertSupplier(supplier)
        if (supplier.openingBalance != 0.0) {
            // Opening balance in supplier ledger (credit = payable to supplier)
            dao.insertSupplierTransaction(
                SupplierTransaction(
                    supplierId = supplierId,
                    transactionType = "OPENING",
                    referenceNumber = "OP-$supplierId",
                    description = "رصيد افتتاحي",
                    debit = if (supplier.openingBalance < 0) -supplier.openingBalance else 0.0,
                    credit = if (supplier.openingBalance > 0) supplier.openingBalance else 0.0,
                    balanceAfter = supplier.openingBalance
                )
            )
        }
        supplierId
    }

    suspend fun updateSupplier(supplier: Supplier) = dao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = database.withTransaction {
        dao.deleteSupplier(supplier)
    }

    fun getSupplierTransactions(supplierId: Long): Flow<List<SupplierTransaction>> =
        dao.getSupplierTransactions(supplierId)

    fun getSupplierBalanceFlow(supplierId: Long): Flow<Double?> =
        dao.getSupplierBalanceFlow(supplierId)

    // ---------------- Items & Inventory ----------------
    val allItems: Flow<List<Item>> = dao.getAllItems()
    val itemCount: Flow<Int> = dao.getItemCount()
    val lowStockItems: Flow<List<Item>> = dao.getLowStockItems()
    val allStockMovements: Flow<List<StockMovement>> = dao.getAllStockMovements()

    suspend fun getItemById(id: Long): Item? = dao.getItemById(id)
    suspend fun getItemByBarcode(barcode: String): Item? = dao.getItemByBarcode(barcode)

    suspend fun addItem(item: Item): Long = database.withTransaction {
        val itemId = dao.insertItem(item)
        if (item.currentStock > 0) {
            val whId = item.defaultWarehouseId ?: 1L
            dao.insertStockMovement(
                StockMovement(
                    itemId = itemId,
                    warehouseId = whId,
                    movementType = "OPENING",
                    quantity = item.currentStock,
                    unitCost = item.purchasePrice,
                    balanceAfter = item.currentStock,
                    referenceType = "MANUAL",
                    referenceNumber = "OP-STOCK-$itemId",
                    notes = "رصيد افتتاحي للمخزون"
                )
            )
        }
        itemId
    }

    suspend fun updateItem(item: Item) = dao.updateItem(item)
    suspend fun deleteItem(item: Item) = database.withTransaction {
        dao.deleteItem(item)
    }

    suspend fun adjustItemStock(itemId: Long, warehouseId: Long, quantityDelta: Double, notes: String) = database.withTransaction {
        val currentItem = dao.getItemById(itemId) ?: return@withTransaction
        val newStock = currentItem.currentStock + quantityDelta
        dao.updateStock(itemId, quantityDelta)
        dao.insertStockMovement(
            StockMovement(
                itemId = itemId,
                warehouseId = warehouseId,
                movementType = "ADJUSTMENT",
                quantity = quantityDelta,
                unitCost = currentItem.purchasePrice,
                balanceAfter = newStock,
                referenceType = "MANUAL",
                notes = notes
            )
        )
    }

    fun getStockMovementsForItem(itemId: Long): Flow<List<StockMovement>> =
        dao.getStockMovementsForItem(itemId)

    // ---------------- Warehouses ----------------
    val allWarehouses: Flow<List<Warehouse>> = dao.getAllWarehouses()
    suspend fun getWarehouseById(id: Long): Warehouse? = dao.getWarehouseById(id)
    suspend fun insertWarehouse(warehouse: Warehouse): Long = dao.insertWarehouse(warehouse)
    suspend fun updateWarehouse(warehouse: Warehouse) = dao.updateWarehouse(warehouse)
    suspend fun deleteWarehouse(warehouse: Warehouse) = dao.deleteWarehouse(warehouse)

    // ---------------- Cashboxes ----------------
    val allCashboxes: Flow<List<Cashbox>> = dao.getAllCashboxes()
    val totalCashboxBalance: Flow<Double?> = dao.getTotalCashboxBalance()

    suspend fun getCashboxById(id: Long): Cashbox? = dao.getCashboxById(id)
    suspend fun insertCashbox(cashbox: Cashbox): Long = dao.insertCashbox(cashbox)
    suspend fun updateCashbox(cashbox: Cashbox) = dao.updateCashbox(cashbox)
    suspend fun deleteCashbox(cashbox: Cashbox) = dao.deleteCashbox(cashbox)
    fun getTransactionsForCashbox(cashboxId: Long): Flow<List<CashTransaction>> =
        dao.getTransactionsForCashbox(cashboxId)

    // ---------------- Invoices (Sales & Purchases) Transactional Lifecycle ----------------
    val allInvoices: Flow<List<Invoice>> = dao.getAllActiveInvoices()
    fun getInvoicesByType(type: String): Flow<List<Invoice>> = dao.getInvoicesByType(type)
    val totalSales: Flow<Double?> = dao.getTotalSales()
    val totalPurchases: Flow<Double?> = dao.getTotalPurchases()

    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItem>> = dao.getItemsForInvoice(invoiceId)

    /**
     * Create Invoice with Full Transactional Integrity:
     * 1. Insert Invoice
     * 2. Insert InvoiceItems
     * 3. Update Item stock & Record Stock Movement
     * 4. Update Party Ledger (Customer or Supplier)
     * 5. Record Cash Transaction & Update Cashbox balance if payment was made
     */
    suspend fun createInvoiceWithTransaction(invoice: Invoice, items: List<InvoiceItem>): Long = database.withTransaction {
        val invoiceId = dao.insertInvoice(invoice)
        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        dao.insertInvoiceItems(itemsWithId)

        val isSale = invoice.invoiceType == "SALE" || invoice.invoiceType == "RETURN_PURCHASE"

        // 1. Stock Updates and Stock Movement Records
        for (item in items) {
            val stockDelta = if (isSale) -item.quantity else item.quantity
            dao.updateStock(item.itemId, stockDelta)

            val updatedItem = dao.getItemById(item.itemId)
            val balanceAfter = updatedItem?.currentStock ?: 0.0

            dao.insertStockMovement(
                StockMovement(
                    itemId = item.itemId,
                    warehouseId = invoice.warehouseId,
                    movementType = if (isSale) "SALE_OUT" else "PURCHASE_IN",
                    quantity = stockDelta,
                    unitCost = item.purchaseCost.takeIf { it > 0.0 } ?: item.unitPrice,
                    balanceAfter = balanceAfter,
                    referenceType = "INVOICE",
                    referenceId = invoiceId,
                    referenceNumber = invoice.invoiceNumber,
                    notes = if (isSale) "فاتورة مبيعات رقم ${invoice.invoiceNumber}" else "فاتورة مشتريات رقم ${invoice.invoiceNumber}"
                )
            )
        }

        // 2. Party Ledger Updates
        if (invoice.partyType == "CUSTOMER") {
            // Customer is Debited for the total invoice amount
            val currentBal = dao.getCustomerBalanceOnce(invoice.partyId) ?: 0.0
            val newBal = currentBal + invoice.totalAmount
            dao.insertCustomerTransaction(
                CustomerTransaction(
                    customerId = invoice.partyId,
                    transactionType = "INVOICE_SALE",
                    referenceNumber = invoice.invoiceNumber,
                    referenceId = invoiceId,
                    description = "فاتورة مبيعات رقم ${invoice.invoiceNumber}",
                    debit = invoice.totalAmount,
                    credit = 0.0,
                    balanceAfter = newBal
                )
            )

            // If immediate payment made on invoice: Customer credited for paid amount
            if (invoice.paidAmount > 0) {
                val balAfterPayment = newBal - invoice.paidAmount
                dao.insertCustomerTransaction(
                    CustomerTransaction(
                        customerId = invoice.partyId,
                        transactionType = "PAYMENT_ON_INVOICE",
                        referenceNumber = invoice.invoiceNumber,
                        referenceId = invoiceId,
                        description = "سداد نقدي/بنكي لفاتورة مبيعات ${invoice.invoiceNumber}",
                        debit = 0.0,
                        credit = invoice.paidAmount,
                        balanceAfter = balAfterPayment
                    )
                )
            }
        } else if (invoice.partyType == "SUPPLIER") {
            // Supplier is Credited for the total purchase invoice amount
            val currentBal = dao.getSupplierBalanceOnce(invoice.partyId) ?: 0.0
            val newBal = currentBal + invoice.totalAmount
            dao.insertSupplierTransaction(
                SupplierTransaction(
                    supplierId = invoice.partyId,
                    transactionType = "INVOICE_PURCHASE",
                    referenceNumber = invoice.invoiceNumber,
                    referenceId = invoiceId,
                    description = "فاتورة مشتريات رقم ${invoice.invoiceNumber}",
                    debit = 0.0,
                    credit = invoice.totalAmount,
                    balanceAfter = newBal
                )
            )

            // If immediate payment made on purchase: Supplier debited for paid amount
            if (invoice.paidAmount > 0) {
                val balAfterPayment = newBal - invoice.paidAmount
                dao.insertSupplierTransaction(
                    SupplierTransaction(
                        supplierId = invoice.partyId,
                        transactionType = "PAYMENT_ON_INVOICE",
                        referenceNumber = invoice.invoiceNumber,
                        referenceId = invoiceId,
                        description = "سداد لفاتورة مشتريات ${invoice.invoiceNumber}",
                        debit = invoice.paidAmount,
                        credit = 0.0,
                        balanceAfter = balAfterPayment
                    )
                )
            }
        }

        // 3. Cashbox Update & Cash Transaction
        if (invoice.paidAmount > 0 && invoice.cashboxId != null) {
            val cashDelta = if (isSale) invoice.paidAmount else -invoice.paidAmount
            dao.updateCashboxBalance(invoice.cashboxId, cashDelta)

            val updatedCashbox = dao.getCashboxById(invoice.cashboxId)
            val cashboxBalanceAfter = updatedCashbox?.currentBalance ?: 0.0

            dao.insertCashTransaction(
                CashTransaction(
                    cashboxId = invoice.cashboxId,
                    movementType = if (isSale) "IN" else "OUT",
                    amount = invoice.paidAmount,
                    balanceAfter = cashboxBalanceAfter,
                    source = if (isSale) "INVOICE_SALE" else "INVOICE_PURCHASE",
                    referenceId = invoiceId,
                    referenceNumber = invoice.invoiceNumber,
                    description = if (isSale) "متحصلات نقدية/بنكية لفاتورة مبيعات ${invoice.invoiceNumber}" else "مدفوعات نقدية/بنكية لفاتورة مشتريات ${invoice.invoiceNumber}"
                )
            )
        }

        invoiceId
    }

    /**
     * Cancel / Reverse Invoice with Full Reversal Integrity:
     * - Restores Item quantities and records reverse stock movements
     * - Reverses party ledger entries
     * - Reverses cashbox movement if payment was made
     */
    suspend fun reverseInvoice(invoiceId: Long) = database.withTransaction {
        val invoice = dao.getInvoiceById(invoiceId) ?: return@withTransaction
        if (invoice.isCancelled) return@withTransaction

        val items = dao.getItemsForInvoiceOnce(invoiceId)
        val isSale = invoice.invoiceType == "SALE" || invoice.invoiceType == "RETURN_PURCHASE"

        // 1. Reverse stock
        for (item in items) {
            val reverseStockDelta = if (isSale) item.quantity else -item.quantity
            dao.updateStock(item.itemId, reverseStockDelta)

            val updatedItem = dao.getItemById(item.itemId)
            val balanceAfter = updatedItem?.currentStock ?: 0.0

            dao.insertStockMovement(
                StockMovement(
                    itemId = item.itemId,
                    warehouseId = invoice.warehouseId,
                    movementType = "CANCEL_REVERSE",
                    quantity = reverseStockDelta,
                    unitCost = item.unitPrice,
                    balanceAfter = balanceAfter,
                    referenceType = "INVOICE_CANCEL",
                    referenceId = invoiceId,
                    referenceNumber = invoice.invoiceNumber,
                    notes = "إلغاء وعكس فاتورة رقم ${invoice.invoiceNumber}"
                )
            )
        }

        // 2. Reverse customer or supplier ledger
        if (invoice.partyType == "CUSTOMER") {
            val currentBal = dao.getCustomerBalanceOnce(invoice.partyId) ?: 0.0
            val netCustomerDebt = invoice.remainingAmount
            if (netCustomerDebt > 0) {
                dao.insertCustomerTransaction(
                    CustomerTransaction(
                        customerId = invoice.partyId,
                        transactionType = "INVOICE_CANCEL",
                        referenceNumber = invoice.invoiceNumber,
                        referenceId = invoiceId,
                        description = "عكس إلغاء فاتورة مبيعات رقم ${invoice.invoiceNumber}",
                        debit = 0.0,
                        credit = netCustomerDebt,
                        balanceAfter = currentBal - netCustomerDebt
                    )
                )
            }
        } else if (invoice.partyType == "SUPPLIER") {
            val currentBal = dao.getSupplierBalanceOnce(invoice.partyId) ?: 0.0
            val netSupplierPayable = invoice.remainingAmount
            if (netSupplierPayable > 0) {
                dao.insertSupplierTransaction(
                    SupplierTransaction(
                        supplierId = invoice.partyId,
                        transactionType = "INVOICE_CANCEL",
                        referenceNumber = invoice.invoiceNumber,
                        referenceId = invoiceId,
                        description = "عكس إلغاء فاتورة مشتريات رقم ${invoice.invoiceNumber}",
                        debit = netSupplierPayable,
                        credit = 0.0,
                        balanceAfter = currentBal - netSupplierPayable
                    )
                )
            }
        }

        // 3. Reverse cashbox
        if (invoice.paidAmount > 0 && invoice.cashboxId != null) {
            val reverseCashDelta = if (isSale) -invoice.paidAmount else invoice.paidAmount
            dao.updateCashboxBalance(invoice.cashboxId, reverseCashDelta)

            val cashbox = dao.getCashboxById(invoice.cashboxId)
            val balAfter = cashbox?.currentBalance ?: 0.0

            dao.insertCashTransaction(
                CashTransaction(
                    cashboxId = invoice.cashboxId,
                    movementType = if (isSale) "OUT" else "IN",
                    amount = invoice.paidAmount,
                    balanceAfter = balAfter,
                    source = "INVOICE_CANCEL",
                    referenceId = invoiceId,
                    referenceNumber = invoice.invoiceNumber,
                    description = "استرداد/عكس حركة نقدية لإلغاء الفاتورة ${invoice.invoiceNumber}"
                )
            )
        }

        dao.markInvoiceCancelled(invoiceId)
    }

    // ---------------- Vouchers (Receipt & Payment) Transactional Lifecycle ----------------
    val allVouchers: Flow<List<Voucher>> = dao.getAllVouchers()
    fun getVouchersByType(type: String): Flow<List<Voucher>> = dao.getVouchersByType(type)
    val totalReceipts: Flow<Double?> = dao.getTotalReceipts()
    val totalPayments: Flow<Double?> = dao.getTotalPayments()

    suspend fun createVoucherWithTransaction(voucher: Voucher): Long = database.withTransaction {
        val voucherId = dao.insertVoucher(voucher)
        val isReceipt = voucher.type == "RECEIPT"

        // 1. Update Cashbox
        val cashDelta = if (isReceipt) voucher.amount else -voucher.amount
        dao.updateCashboxBalance(voucher.cashboxId, cashDelta)

        val updatedCashbox = dao.getCashboxById(voucher.cashboxId)
        val balanceAfter = updatedCashbox?.currentBalance ?: 0.0

        dao.insertCashTransaction(
            CashTransaction(
                cashboxId = voucher.cashboxId,
                movementType = if (isReceipt) "IN" else "OUT",
                amount = voucher.amount,
                balanceAfter = balanceAfter,
                source = "VOUCHER",
                referenceId = voucherId,
                referenceNumber = voucher.voucherNumber,
                description = if (isReceipt) "سند قبض ${voucher.voucherNumber} - ${voucher.partyName}" else "سند صرف ${voucher.voucherNumber} - ${voucher.partyName}"
            )
        )

        // 2. Update Customer / Supplier ledger
        if (voucher.partyId != null && voucher.partyId > 0) {
            if (voucher.partyType == "CUSTOMER") {
                // Customer receipt decreases customer debt (credit)
                val currentBal = dao.getCustomerBalanceOnce(voucher.partyId) ?: 0.0
                val newBal = currentBal - voucher.amount
                dao.insertCustomerTransaction(
                    CustomerTransaction(
                        customerId = voucher.partyId,
                        transactionType = "RECEIPT_VOUCHER",
                        referenceNumber = voucher.voucherNumber,
                        referenceId = voucherId,
                        description = "سند قبض رقم ${voucher.voucherNumber}: ${voucher.notes}",
                        debit = 0.0,
                        credit = voucher.amount,
                        balanceAfter = newBal
                    )
                )
            } else if (voucher.partyType == "SUPPLIER") {
                // Supplier payment decreases company debt to supplier (debit)
                val currentBal = dao.getSupplierBalanceOnce(voucher.partyId) ?: 0.0
                val newBal = currentBal - voucher.amount
                dao.insertSupplierTransaction(
                    SupplierTransaction(
                        supplierId = voucher.partyId,
                        transactionType = "PAYMENT_VOUCHER",
                        referenceNumber = voucher.voucherNumber,
                        referenceId = voucherId,
                        description = "سند صرف رقم ${voucher.voucherNumber}: ${voucher.notes}",
                        debit = voucher.amount,
                        credit = 0.0,
                        balanceAfter = newBal
                    )
                )
            }
        }

        voucherId
    }

    suspend fun reverseVoucher(voucherId: Long) = database.withTransaction {
        val voucher = dao.getVoucherById(voucherId) ?: return@withTransaction
        if (voucher.isCancelled) return@withTransaction

        val isReceipt = voucher.type == "RECEIPT"
        val reverseCashDelta = if (isReceipt) -voucher.amount else voucher.amount
        dao.updateCashboxBalance(voucher.cashboxId, reverseCashDelta)

        val updatedCashbox = dao.getCashboxById(voucher.cashboxId)
        val balanceAfter = updatedCashbox?.currentBalance ?: 0.0

        dao.insertCashTransaction(
            CashTransaction(
                cashboxId = voucher.cashboxId,
                movementType = if (isReceipt) "OUT" else "IN",
                amount = voucher.amount,
                balanceAfter = balanceAfter,
                source = "VOUCHER_CANCEL",
                referenceId = voucherId,
                referenceNumber = voucher.voucherNumber,
                description = "عكس/إلغاء سند ${voucher.voucherNumber}"
            )
        )

        // Reverse party ledger
        if (voucher.partyId != null && voucher.partyId > 0) {
            if (voucher.partyType == "CUSTOMER") {
                val currentBal = dao.getCustomerBalanceOnce(voucher.partyId) ?: 0.0
                dao.insertCustomerTransaction(
                    CustomerTransaction(
                        customerId = voucher.partyId,
                        transactionType = "VOUCHER_CANCEL",
                        referenceNumber = voucher.voucherNumber,
                        referenceId = voucherId,
                        description = "عكس سند قبض رقم ${voucher.voucherNumber}",
                        debit = voucher.amount,
                        credit = 0.0,
                        balanceAfter = currentBal + voucher.amount
                    )
                )
            } else if (voucher.partyType == "SUPPLIER") {
                val currentBal = dao.getSupplierBalanceOnce(voucher.partyId) ?: 0.0
                dao.insertSupplierTransaction(
                    SupplierTransaction(
                        supplierId = voucher.partyId,
                        transactionType = "VOUCHER_CANCEL",
                        referenceNumber = voucher.voucherNumber,
                        referenceId = voucherId,
                        description = "عكس سند صرف رقم ${voucher.voucherNumber}",
                        debit = 0.0,
                        credit = voucher.amount,
                        balanceAfter = currentBal + voucher.amount
                    )
                )
            }
        }

        dao.markVoucherCancelled(voucherId)
    }

    // ---------------- Currencies ----------------
    val allCurrencies: Flow<List<Currency>> = dao.getAllCurrencies()
    suspend fun insertCurrency(currency: Currency): Long = dao.insertCurrency(currency)
    suspend fun updateCurrency(currency: Currency) = dao.updateCurrency(currency)
    suspend fun deleteCurrency(currency: Currency) = dao.deleteCurrency(currency)
}
