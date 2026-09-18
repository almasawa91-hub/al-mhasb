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

    // ---------------- Customers ----------------
    @Query("SELECT * FROM customers ORDER BY id DESC")
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

    // ---------------- Suppliers ----------------
    @Query("SELECT * FROM suppliers ORDER BY id DESC")
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

    // ---------------- Items ----------------
    @Query("SELECT * FROM items ORDER BY id DESC")
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

    // ---------------- Warehouses ----------------
    @Query("SELECT * FROM warehouses ORDER BY id ASC")
    fun getAllWarehouses(): Flow<List<Warehouse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: Warehouse): Long

    // ---------------- Cashboxes ----------------
    @Query("SELECT * FROM cashboxes ORDER BY id ASC")
    fun getAllCashboxes(): Flow<List<Cashbox>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashbox(cashbox: Cashbox): Long

    @Query("UPDATE cashboxes SET currentBalance = currentBalance + :amount WHERE id = :id")
    suspend fun updateCashboxBalance(id: Long, amount: Double)

    // ---------------- Invoices ----------------
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE invoiceType = :type ORDER BY id DESC")
    fun getInvoicesByType(type: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("SELECT SUM(totalAmount) FROM invoices WHERE invoiceType = 'SALE'")
    fun getTotalSales(): Flow<Double?>

    @Query("SELECT SUM(totalAmount) FROM invoices WHERE invoiceType = 'PURCHASE'")
    fun getTotalPurchases(): Flow<Double?>

    // ---------------- Invoice Items ----------------
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Long)

    // ---------------- Vouchers ----------------
    @Query("SELECT * FROM vouchers ORDER BY id DESC")
    fun getAllVouchers(): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE type = :type ORDER BY id DESC")
    fun getVouchersByType(type: String): Flow<List<Voucher>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: Voucher): Long

    @Delete
    suspend fun deleteVoucher(voucher: Voucher)

    @Query("SELECT SUM(amount) FROM vouchers WHERE type = 'RECEIPT'")
    fun getTotalReceipts(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM vouchers WHERE type = 'PAYMENT'")
    fun getTotalPayments(): Flow<Double?>

    // ---------------- Currencies ----------------
    @Query("SELECT * FROM currencies ORDER BY id ASC")
    fun getAllCurrencies(): Flow<List<Currency>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrency(currency: Currency): Long
}
