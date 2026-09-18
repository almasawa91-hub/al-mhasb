package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountingRobolectricTest {

    private lateinit var database: AccountingDatabase
    private lateinit var repository: AccountingRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AccountingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AccountingRepository(database, database.accountingDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("المحاسب الذكي", appName)
    }

    @Test
    fun testZeroFakeDataInitialState() = runBlocking {
        // Initial state must have zero records (Zero Fake Data compliance)
        val customers = repository.allCustomers.first()
        val suppliers = repository.allSuppliers.first()
        val items = repository.allItems.first()
        val invoices = repository.allInvoices.first()
        val vouchers = repository.allVouchers.first()

        assertTrue(customers.isEmpty())
        assertTrue(suppliers.isEmpty())
        assertTrue(items.isEmpty())
        assertTrue(invoices.isEmpty())
        assertTrue(vouchers.isEmpty())
    }

    @Test
    fun testCustomerCreationAndLedger() = runBlocking {
        // Add customer with opening balance
        val custId = repository.addCustomer(
            Customer(
                name = "شركة الأمل للتجارة",
                phone = "0501234567",
                openingBalance = 1500.0
            )
        )
        assertTrue(custId > 0)

        val balance = repository.getCustomerBalanceFlow(custId).first()
        assertEquals(1500.0, balance ?: 0.0, 0.01)

        val transactions = repository.getCustomerTransactions(custId).first()
        assertEquals(1, transactions.size)
        assertEquals("OPENING", transactions[0].transactionType)
        assertEquals(1500.0, transactions[0].debit, 0.01)
    }

    @Test
    fun testSalesInvoiceTransactionalLifecycle() = runBlocking {
        // 1. Setup Customer & Item
        val custId = repository.addCustomer(Customer(name = "عميل تجريبي", openingBalance = 0.0))
        val itemId = repository.addItem(
            Item(
                name = "طابعة باركود حرارية",
                salePrice = 500.0,
                purchasePrice = 300.0,
                currentStock = 10.0
            )
        )

        // 2. Issue Sale Invoice of 2 units for 1000 SAR, paid 400 SAR cash
        val invoice = Invoice(
            invoiceNumber = "INV-S-001",
            invoiceType = "SALE",
            partyType = "CUSTOMER",
            partyId = custId,
            partyName = "عميل تجريبي",
            warehouseId = 1,
            warehouseName = "المستودع الرئيسي",
            cashboxId = 1,
            cashboxName = "الصندوق",
            subtotal = 1000.0,
            discount = 0.0,
            taxRate = 0.0,
            taxAmount = 0.0,
            totalAmount = 1000.0,
            paidAmount = 400.0,
            remainingAmount = 600.0,
            paymentStatus = "مدفوعة جزئياً",
            paymentMethod = "نقداً"
        )

        val itemsList = listOf(
            InvoiceItem(
                invoiceId = 0,
                itemId = itemId,
                itemName = "طابعة باركود حرارية",
                quantity = 2.0,
                unitPrice = 500.0,
                totalPrice = 1000.0,
                purchaseCost = 300.0
            )
        )

        val invId = repository.createInvoiceWithTransaction(invoice, itemsList)
        assertTrue(invId > 0)

        // 3. Verify item stock decreased by 2 (10 -> 8)
        val updatedItem = repository.getItemById(itemId)
        assertEquals(8.0, updatedItem?.currentStock ?: 0.0, 0.01)

        // 4. Verify customer balance increased by 600 remaining
        val custBalance = repository.getCustomerBalanceFlow(custId).first()
        assertEquals(600.0, custBalance ?: 0.0, 0.01)

        // 5. Test Invoice Reversal / Cancellation
        repository.reverseInvoice(invId)

        // Stock restored back to 10
        val restoredItem = repository.getItemById(itemId)
        assertEquals(10.0, restoredItem?.currentStock ?: 0.0, 0.01)

        // Customer debt cleared back to 0
        val restoredCustBalance = repository.getCustomerBalanceFlow(custId).first()
        assertEquals(0.0, restoredCustBalance ?: 0.0, 0.01)
    }

    @Test
    fun testVoucherTransactionalLifecycle() = runBlocking {
        // Customer with 500 debit balance
        val custId = repository.addCustomer(Customer(name = "عميل سداد", openingBalance = 500.0))

        // Issue receipt voucher of 500
        val voucher = Voucher(
            voucherNumber = "REC-001",
            type = "RECEIPT",
            partyType = "CUSTOMER",
            partyId = custId,
            partyName = "عميل سداد",
            amount = 500.0,
            cashboxId = 1
        )

        val voucherId = repository.createVoucherWithTransaction(voucher)
        assertTrue(voucherId > 0)

        // Balance must be 0 after payment
        val balAfterReceipt = repository.getCustomerBalanceFlow(custId).first()
        assertEquals(0.0, balAfterReceipt ?: 0.0, 0.01)
    }
}
