package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalSales: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalReceipts: Double = 0.0,
    val totalPayments: Double = 0.0,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val itemCount: Int = 0,
    val netCashFlow: Double = 0.0
)

class AccountingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AccountingRepository

    init {
        val db = AccountingDatabase.getDatabase(application)
        repository = AccountingRepository(db.accountingDao())
        ensureDefaultsInitialized()
    }

    private fun ensureDefaultsInitialized() {
        viewModelScope.launch {
            // Check and initialize default warehouse, cashbox, currency if empty (Standard schema requirement)
            val warehouses = repository.allWarehouses.firstOrNull() ?: emptyList()
            if (warehouses.isEmpty()) {
                repository.insertWarehouse(Warehouse(name = "المستودع الرئيسي", code = "WH-01", location = "المقر الرئيسي", isDefault = true))
            }
            val cashboxes = repository.allCashboxes.firstOrNull() ?: emptyList()
            if (cashboxes.isEmpty()) {
                repository.insertCashbox(Cashbox(name = "الصندوق الرئيسي", type = "نقدي", currency = "SAR", isDefault = true))
            }
            val currencies = repository.allCurrencies.firstOrNull() ?: emptyList()
            if (currencies.isEmpty()) {
                repository.insertCurrency(Currency(code = "SAR", name = "ريال سعودي", symbol = "ر.س", exchangeRate = 1.0, isDefault = true))
                repository.insertCurrency(Currency(code = "USD", name = "دولار أمريكي", symbol = "$", exchangeRate = 3.75))
            }
        }
    }

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

    // Dashboard Statistics Flow
    private val financialTotalsFlow = combine(
        repository.totalSales,
        repository.totalPurchases,
        repository.totalReceipts,
        repository.totalPayments
    ) { sales, purchases, receipts, payments ->
        val s = sales ?: 0.0
        val p = purchases ?: 0.0
        val r = receipts ?: 0.0
        val py = payments ?: 0.0
        val net = r - py
        listOf(s, p, r, py, net)
    }

    private val countsFlow = combine(
        repository.customerCount,
        repository.supplierCount,
        repository.itemCount
    ) { custCount, supCount, itCount ->
        Triple(custCount, supCount, itCount)
    }

    val dashboardStats: StateFlow<DashboardStats> = combine(
        financialTotalsFlow,
        countsFlow
    ) { fin, counts ->
        DashboardStats(
            totalSales = fin[0],
            totalPurchases = fin[1],
            totalReceipts = fin[2],
            totalPayments = fin[3],
            netCashFlow = fin[4],
            customerCount = counts.first,
            supplierCount = counts.second,
            itemCount = counts.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Actions
    fun addCustomer(name: String, phone: String, email: String, address: String, creditLimit: Double, openingBalance: Double) {
        viewModelScope.launch {
            repository.insertCustomer(
                Customer(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    address = address.trim(),
                    creditLimit = creditLimit,
                    openingBalance = openingBalance
                )
            )
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch { repository.deleteCustomer(customer) }
    }

    fun addSupplier(name: String, phone: String, email: String, address: String, companyName: String, openingBalance: Double) {
        viewModelScope.launch {
            repository.insertSupplier(
                Supplier(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    address = address.trim(),
                    companyName = companyName.trim(),
                    openingBalance = openingBalance
                )
            )
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch { repository.deleteSupplier(supplier) }
    }

    fun addItem(name: String, barcode: String, category: String, unit: String, purchasePrice: Double, salePrice: Double, initialStock: Double, minAlert: Double) {
        viewModelScope.launch {
            repository.insertItem(
                Item(
                    name = name.trim(),
                    barcode = barcode.trim(),
                    category = category.trim().ifEmpty { "عام" },
                    unit = unit.trim().ifEmpty { "قطعة" },
                    purchasePrice = purchasePrice,
                    salePrice = salePrice,
                    currentStock = initialStock,
                    minStockAlert = minAlert
                )
            )
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun createInvoice(
        invoiceNumber: String,
        type: String, // "SALE" or "PURCHASE"
        partyId: Long,
        partyName: String,
        itemsList: List<InvoiceItem>,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        notes: String
    ) {
        viewModelScope.launch {
            val subtotal = itemsList.sumOf { it.totalPrice }
            val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
            val taxAmount = afterDiscount * 0.15 // 15% VAT
            val totalAmount = afterDiscount + taxAmount
            val remaining = (totalAmount - paidAmount).coerceAtLeast(0.0)

            val invoice = Invoice(
                invoiceNumber = invoiceNumber.ifEmpty { "INV-${System.currentTimeMillis() % 100000}" },
                invoiceType = type,
                partyType = if (type == "SALE") "CUSTOMER" else "SUPPLIER",
                partyId = partyId,
                partyName = partyName,
                subtotal = subtotal,
                discount = discount,
                taxRate = 0.15,
                taxAmount = taxAmount,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                remainingAmount = remaining,
                paymentStatus = if (remaining <= 0) "مدفوعة" else if (paidAmount > 0) "مدفوعة جزئياً" else "آجلة",
                paymentMethod = paymentMethod,
                notes = notes
            )
            repository.createInvoiceWithItems(invoice, itemsList)
        }
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch { repository.deleteInvoice(invoice) }
    }

    fun createVoucher(
        voucherNumber: String,
        type: String, // "RECEIPT" (قبض), "PAYMENT" (صرف)
        partyName: String,
        partyType: String,
        amount: Double,
        paymentMethod: String,
        notes: String
    ) {
        viewModelScope.launch {
            val vNumber = voucherNumber.ifEmpty { (if (type == "RECEIPT") "REC-" else "PAY-") + (System.currentTimeMillis() % 100000) }
            val voucher = Voucher(
                voucherNumber = vNumber,
                type = type,
                partyName = partyName,
                partyType = partyType,
                amount = amount,
                paymentMethod = paymentMethod,
                notes = notes
            )
            repository.insertVoucher(voucher)
        }
    }

    fun deleteVoucher(voucher: Voucher) {
        viewModelScope.launch { repository.deleteVoucher(voucher) }
    }
}
