package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {

    // ---------------- Organization Settings ----------------
    @Query("SELECT * FROM organization_settings WHERE id = 1 LIMIT 1")
    fun getOrganizationSettings(): Flow<OrganizationSettings?>

    @Query("SELECT * FROM organization_settings WHERE id = 1 LIMIT 1")
    suspend fun getOrganizationSettingsOnce(): OrganizationSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOrganizationSettings(settings: OrganizationSettings)

    // ---------------- Customers ----------------
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT COUNT(*) FROM customers")
    fun getCustomerCount(): Flow<Int>

    // ---------------- Customer Transactions (Ledger) ----------------
    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY date ASC, id ASC")
    fun getCustomerTransactions(customerId: Long): Flow<List<CustomerTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerTransaction(tx: CustomerTransaction): Long

    @Query("DELETE FROM customer_transactions WHERE referenceId = :refId AND transactionType = :type")
    suspend fun deleteCustomerTransactionByRef(refId: Long, type: String)

    @Query("SELECT (COALESCE(SUM(debit), 0.0) - COALESCE(SUM(credit), 0.0)) FROM customer_transactions WHERE customerId = :customerId")
    fun getCustomerBalanceFlow(customerId: Long): Flow<Double?>

    @Query("SELECT (COALESCE(SUM(debit), 0.0) - COALESCE(SUM(credit), 0.0)) FROM customer_transactions WHERE customerId = :customerId")
    suspend fun getCustomerBalanceOnce(customerId: Long): Double?

    @Query("SELECT (COALESCE(SUM(debit), 0.0) - COALESCE(SUM(credit), 0.0)) FROM customer_transactions")
    fun getTotalCustomerReceivables(): Flow<Double?>

    // ---------------- Suppliers ----------------
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    @Query("SELECT COUNT(*) FROM suppliers")
    fun getSupplierCount(): Flow<Int>

    // ---------------- Supplier Transactions (Ledger) ----------------
    @Query("SELECT * FROM supplier_transactions WHERE supplierId = :supplierId ORDER BY date ASC, id ASC")
    fun getSupplierTransactions(supplierId: Long): Flow<List<SupplierTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierTransaction(tx: SupplierTransaction): Long

    @Query("DELETE FROM supplier_transactions WHERE referenceId = :refId AND transactionType = :type")
    suspend fun deleteSupplierTransactionByRef(refId: Long, type: String)

    @Query("SELECT (COALESCE(SUM(credit), 0.0) - COALESCE(SUM(debit), 0.0)) FROM supplier_transactions WHERE supplierId = :supplierId")
    fun getSupplierBalanceFlow(supplierId: Long): Flow<Double?>

    @Query("SELECT (COALESCE(SUM(credit), 0.0) - COALESCE(SUM(debit), 0.0)) FROM supplier_transactions WHERE supplierId = :supplierId")
    suspend fun getSupplierBalanceOnce(supplierId: Long): Double?

    @Query("SELECT (COALESCE(SUM(credit), 0.0) - COALESCE(SUM(debit), 0.0)) FROM supplier_transactions")
    fun getTotalSupplierPayables(): Flow<Double?>

    // ---------------- Items ----------------
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): Item?

    @Query("SELECT * FROM items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("UPDATE items SET currentStock = currentStock + :delta WHERE id = :itemId")
    suspend fun updateStock(itemId: Long, delta: Double)

    @Query("SELECT COUNT(*) FROM items")
    fun getItemCount(): Flow<Int>

    @Query("SELECT * FROM items WHERE currentStock <= minStockAlert")
    fun getLowStockItems(): Flow<List<Item>>

    // ---------------- Stock Movements ----------------
    @Query("SELECT * FROM stock_movements WHERE itemId = :itemId ORDER BY date DESC, id DESC")
    fun getStockMovementsForItem(itemId: Long): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY date DESC, id DESC")
    fun getAllStockMovements(): Flow<List<StockMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovement): Long

    @Query("DELETE FROM stock_movements WHERE referenceId = :refId AND referenceType = :refType")
    suspend fun deleteStockMovementsByRef(refId: Long, refType: String)

    // ---------------- Warehouses ----------------
    @Query("SELECT * FROM warehouses ORDER BY id ASC")
    fun getAllWarehouses(): Flow<List<Warehouse>>

    @Query("SELECT * FROM warehouses WHERE id = :id")
    suspend fun getWarehouseById(id: Long): Warehouse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: Warehouse): Long

    @Update
    suspend fun updateWarehouse(warehouse: Warehouse)

    @Delete
    suspend fun deleteWarehouse(warehouse: Warehouse)

    // ---------------- Cashboxes ----------------
    @Query("SELECT * FROM cashboxes ORDER BY id ASC")
    fun getAllCashboxes(): Flow<List<Cashbox>>

    @Query("SELECT * FROM cashboxes WHERE id = :id")
    suspend fun getCashboxById(id: Long): Cashbox?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashbox(cashbox: Cashbox): Long

    @Update
    suspend fun updateCashbox(cashbox: Cashbox)

    @Delete
    suspend fun deleteCashbox(cashbox: Cashbox)

    @Query("UPDATE cashboxes SET currentBalance = currentBalance + :amount WHERE id = :id")
    suspend fun updateCashboxBalance(id: Long, amount: Double)

    @Query("SELECT COALESCE(SUM(currentBalance), 0.0) FROM cashboxes")
    fun getTotalCashboxBalance(): Flow<Double?>

    // ---------------- Cash Transactions ----------------
    @Query("SELECT * FROM cash_transactions WHERE cashboxId = :cashboxId ORDER BY date DESC, id DESC")
    fun getTransactionsForCashbox(cashboxId: Long): Flow<List<CashTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashTransaction(tx: CashTransaction): Long

    @Query("DELETE FROM cash_transactions WHERE referenceId = :refId AND source = :source")
    suspend fun deleteCashTransactionByRef(refId: Long, source: String)

    // ---------------- Invoices ----------------
    @Query("SELECT * FROM invoices WHERE isCancelled = 0 ORDER BY date DESC, id DESC")
    fun getAllActiveInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices ORDER BY date DESC, id DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE invoiceType = :type AND isCancelled = 0 ORDER BY date DESC, id DESC")
    fun getInvoicesByType(type: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("UPDATE invoices SET isCancelled = 1 WHERE id = :id")
    suspend fun markInvoiceCancelled(id: Long)

    @Query("SELECT SUM(totalAmount) FROM invoices WHERE invoiceType = 'SALE' AND isCancelled = 0")
    fun getTotalSales(): Flow<Double?>

    @Query("SELECT SUM(totalAmount) FROM invoices WHERE invoiceType = 'PURCHASE' AND isCancelled = 0")
    fun getTotalPurchases(): Flow<Double?>

    // ---------------- Invoice Items ----------------
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getItemsForInvoiceOnce(invoiceId: Long): List<InvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Long)

    // ---------------- Vouchers ----------------
    @Query("SELECT * FROM vouchers WHERE isCancelled = 0 ORDER BY date DESC, id DESC")
    fun getAllVouchers(): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE type = :type AND isCancelled = 0 ORDER BY date DESC, id DESC")
    fun getVouchersByType(type: String): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE id = :id")
    suspend fun getVoucherById(id: Long): Voucher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: Voucher): Long

    @Update
    suspend fun updateVoucher(voucher: Voucher)

    @Delete
    suspend fun deleteVoucher(voucher: Voucher)

    @Query("UPDATE vouchers SET isCancelled = 1 WHERE id = :id")
    suspend fun markVoucherCancelled(id: Long)

    @Query("SELECT SUM(amount) FROM vouchers WHERE type = 'RECEIPT' AND isCancelled = 0")
    fun getTotalReceipts(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM vouchers WHERE type = 'PAYMENT' AND isCancelled = 0")
    fun getTotalPayments(): Flow<Double?>

    // ---------------- Currencies ----------------
    @Query("SELECT * FROM currencies ORDER BY isDefault DESC, id ASC")
    fun getAllCurrencies(): Flow<List<Currency>>

    @Query("SELECT * FROM currencies WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCurrency(): Currency?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrency(currency: Currency): Long

    @Update
    suspend fun updateCurrency(currency: Currency)

    @Delete
    suspend fun deleteCurrency(currency: Currency)
}
