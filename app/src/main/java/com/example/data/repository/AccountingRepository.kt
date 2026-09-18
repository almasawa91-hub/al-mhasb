package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.Flow

class AccountingRepository(private val dao: AccountingDao) {

    // Customers
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val customerCount: Flow<Int> = dao.getCustomerCount()
    suspend fun insertCustomer(customer: Customer): Long = dao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = dao.deleteCustomer(customer)

    // Suppliers
    val allSuppliers: Flow<List<Supplier>> = dao.getAllSuppliers()
    val supplierCount: Flow<Int> = dao.getSupplierCount()
    suspend fun insertSupplier(supplier: Supplier): Long = dao.insertSupplier(supplier)
    suspend fun updateSupplier(supplier: Supplier) = dao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = dao.deleteSupplier(supplier)

    // Items & Inventory
    val allItems: Flow<List<Item>> = dao.getAllItems()
    val itemCount: Flow<Int> = dao.getItemCount()
    val lowStockItems: Flow<List<Item>> = dao.getLowStockItems()
    suspend fun getItemById(id: Long): Item? = dao.getItemById(id)
    suspend fun insertItem(item: Item): Long = dao.insertItem(item)
    suspend fun updateItem(item: Item) = dao.updateItem(item)
    suspend fun deleteItem(item: Item) = dao.deleteItem(item)
    suspend fun updateStock(itemId: Long, delta: Double) = dao.updateStock(itemId, delta)

    // Warehouses
    val allWarehouses: Flow<List<Warehouse>> = dao.getAllWarehouses()
    suspend fun insertWarehouse(warehouse: Warehouse): Long = dao.insertWarehouse(warehouse)

    // Cashboxes
    val allCashboxes: Flow<List<Cashbox>> = dao.getAllCashboxes()
    suspend fun insertCashbox(cashbox: Cashbox): Long = dao.insertCashbox(cashbox)
    suspend fun updateCashboxBalance(id: Long, amount: Double) = dao.updateCashboxBalance(id, amount)

    // Invoices
    val allInvoices: Flow<List<Invoice>> = dao.getAllInvoices()
    fun getInvoicesByType(type: String): Flow<List<Invoice>> = dao.getInvoicesByType(type)
    val totalSales: Flow<Double?> = dao.getTotalSales()
    val totalPurchases: Flow<Double?> = dao.getTotalPurchases()

    suspend fun createInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = dao.insertInvoice(invoice)
        val itemsWithInvoiceId = items.map { it.copy(invoiceId = invoiceId) }
        dao.insertInvoiceItems(itemsWithInvoiceId)

        // Real Inventory impact (no fake logic):
        // If sale: decrease item stock. If purchase: increase item stock.
        for (item in items) {
            val delta = if (invoice.invoiceType == "SALE") -item.quantity else item.quantity
            dao.updateStock(item.itemId, delta)
        }

        // Real Cashbox impact if paid cash/bank:
        if (invoice.paidAmount > 0) {
            val cashDelta = if (invoice.invoiceType == "SALE") invoice.paidAmount else -invoice.paidAmount
            dao.updateCashboxBalance(1, cashDelta)
        }

        return invoiceId
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        dao.deleteItemsForInvoice(invoice.id)
        dao.deleteInvoice(invoice)
    }

    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItem>> = dao.getItemsForInvoice(invoiceId)

    // Vouchers
    val allVouchers: Flow<List<Voucher>> = dao.getAllVouchers()
    fun getVouchersByType(type: String): Flow<List<Voucher>> = dao.getVouchersByType(type)
    val totalReceipts: Flow<Double?> = dao.getTotalReceipts()
    val totalPayments: Flow<Double?> = dao.getTotalPayments()

    suspend fun insertVoucher(voucher: Voucher): Long {
        val id = dao.insertVoucher(voucher)
        // Adjust cashbox balance
        val delta = if (voucher.type == "RECEIPT") voucher.amount else -voucher.amount
        dao.updateCashboxBalance(voucher.cashboxId, delta)
        return id
    }

    suspend fun deleteVoucher(voucher: Voucher) {
        dao.deleteVoucher(voucher)
        val reverseDelta = if (voucher.type == "RECEIPT") -voucher.amount else voucher.amount
        dao.updateCashboxBalance(voucher.cashboxId, reverseDelta)
    }

    // Currencies
    val allCurrencies: Flow<List<Currency>> = dao.getAllCurrencies()
    suspend fun insertCurrency(currency: Currency): Long = dao.insertCurrency(currency)
}
