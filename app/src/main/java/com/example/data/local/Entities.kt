package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * بيانات المنشأة / الشركة (إعدادات عامة غير وهمية)
 */
@Entity(tableName = "organization_settings")
data class OrganizationSettings(
    @PrimaryKey val id: Long = 1,
    val name: String = "",
    val activityType: String = "",
    val taxNumber: String = "",
    val commercialRegister: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val defaultTaxRate: Double = 0.0, // نسبة الضريبة المعتمدة للمنشأة قابلة للتعديل
    val isTaxEnabled: Boolean = true,
    val currencyCode: String = "SAR",
    val currencySymbol: String = "ر.س",
    val currencyName: String = "ريال سعودي",
    val logoUri: String = "",
    val invoiceNotes: String = "",
    val isConfigured: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * جهة التعامل: العميل
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"])
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val creditLimit: Double = 0.0,
    val openingBalance: Double = 0.0, // موجب: مديونية عليه (مدين)، سالب: رصيد له (دائن)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * جهة التعامل: المورد
 */
@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"])
    ]
)
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val companyName: String = "",
    val openingBalance: Double = 0.0, // موجب: مستحقات له (دائن للمنشأة)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * المستودعات
 */
@Entity(
    tableName = "warehouses",
    indices = [Index(value = ["name"])]
)
data class Warehouse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val location: String = "",
    val keeperName: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * الأصناف والمنتجات
 */
@Entity(
    tableName = "items",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val sku: String = "",
    val category: String = "عام",
    val unit: String = "قطعة",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val currentStock: Double = 0.0,
    val defaultWarehouseId: Long? = null,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * الصناديق النقدية والحسابات البنكية
 */
@Entity(tableName = "cashboxes")
data class Cashbox(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "صندوق نقدي", // "صندوق نقدي", "حساب بنكي", "محفظة إلكترونية"
    val accountNumber: String = "",
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val currency: String = "SAR",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * جدول العملات
 */
@Entity(
    tableName = "currencies",
    indices = [Index(value = ["code"], unique = true)]
)
data class Currency(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val symbol: String,
    val exchangeRate: Double = 1.0,
    val isDefault: Boolean = false
)

/**
 * الفواتير (مبيعات / مشتريات / مرتجعات)
 */
@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["partyId"]),
        Index(value = ["invoiceType"]),
        Index(value = ["date"])
    ]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val invoiceType: String, // "SALE" (بيع), "PURCHASE" (شراء), "RETURN_SALE" (مرتجع بيع), "RETURN_PURCHASE" (مرتجع شراء)
    val partyType: String, // "CUSTOMER", "SUPPLIER"
    val partyId: Long,
    val partyName: String,
    val date: Long = System.currentTimeMillis(),
    val warehouseId: Long,
    val warehouseName: String = "",
    val cashboxId: Long? = null,
    val cashboxName: String = "",
    val subtotal: Double,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0, // معدل الضريبة المطبق في وقت الفاتورة
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val paymentStatus: String = "مدفوعة", // "مدفوعة", "آجلة", "مدفوعة جزئياً"
    val paymentMethod: String = "نقداً", // "نقداً", "شبكة/بنك", "آجل"
    val currencyCode: String = "SAR",
    val notes: String = "",
    val isCancelled: Boolean = false, // لإلغاء أو عكس الفاتورة
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * بنود الفاتورة
 */
@Entity(
    tableName = "invoice_items",
    indices = [
        Index(value = ["invoiceId"]),
        Index(value = ["itemId"])
    ]
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val itemId: Long,
    val itemName: String,
    val unit: String = "قطعة",
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val purchaseCost: Double = 0.0 // لحساب تكلفة البضاعة المباعة بدقة
)

/**
 * سندات القبض والصرف
 */
@Entity(
    tableName = "vouchers",
    indices = [
        Index(value = ["voucherNumber"], unique = true),
        Index(value = ["partyId"]),
        Index(value = ["type"]),
        Index(value = ["cashboxId"]),
        Index(value = ["date"])
    ]
)
data class Voucher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherNumber: String,
    val type: String, // "RECEIPT" (قبض), "PAYMENT" (صرف)
    val partyType: String, // "CUSTOMER", "SUPPLIER", "GENERAL_EXPENSE", "CAPITAL"
    val partyId: Long? = null,
    val partyName: String,
    val amount: Double,
    val cashboxId: Long,
    val cashboxName: String = "",
    val paymentMethod: String = "نقداً",
    val currencyCode: String = "SAR",
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isCancelled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * حركة المخزون (Stock Movement) لتتبع كل وارد وصادر
 */
@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["warehouseId"]),
        Index(value = ["date"]),
        Index(value = ["referenceType", "referenceId"])
    ]
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val warehouseId: Long,
    val movementType: String, // "PURCHASE_IN", "SALE_OUT", "TRANSFER_IN", "TRANSFER_OUT", "ADJUSTMENT", "CANCEL_REVERSE"
    val quantity: Double, // كمية الحركة (+ أو -)
    val unitCost: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val referenceType: String = "", // "INVOICE", "TRANSFER", "MANUAL"
    val referenceId: Long? = null,
    val referenceNumber: String = "",
    val notes: String = "",
    val date: Long = System.currentTimeMillis()
)

/**
 * كشف حساب العميل (Customer Ledger Transaction)
 */
@Entity(
    tableName = "customer_transactions",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["date"]),
        Index(value = ["referenceId"])
    ]
)
data class CustomerTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val date: Long = System.currentTimeMillis(),
    val transactionType: String, // "OPENING", "INVOICE_SALE", "RECEIPT_VOUCHER", "INVOICE_CANCEL", "RETURN"
    val referenceNumber: String = "",
    val referenceId: Long? = null,
    val description: String = "",
    val debit: Double = 0.0,  // مدين (عليه)
    val credit: Double = 0.0, // دائن (له)
    val balanceAfter: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * كشف حساب المورد (Supplier Ledger Transaction)
 */
@Entity(
    tableName = "supplier_transactions",
    indices = [
        Index(value = ["supplierId"]),
        Index(value = ["date"]),
        Index(value = ["referenceId"])
    ]
)
data class SupplierTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val date: Long = System.currentTimeMillis(),
    val transactionType: String, // "OPENING", "INVOICE_PURCHASE", "PAYMENT_VOUCHER", "INVOICE_CANCEL", "RETURN"
    val referenceNumber: String = "",
    val referenceId: Long? = null,
    val description: String = "",
    val debit: Double = 0.0,  // مدين (سددنا له)
    val credit: Double = 0.0, // دائن (مستحق له)
    val balanceAfter: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * حركة الصناديق والبنوك (Cash Transaction)
 */
@Entity(
    tableName = "cash_transactions",
    indices = [
        Index(value = ["cashboxId"]),
        Index(value = ["date"])
    ]
)
data class CashTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashboxId: Long,
    val date: Long = System.currentTimeMillis(),
    val movementType: String, // "IN" (إيداع/قبض), "OUT" (صرف/مدفوعات)
    val amount: Double,
    val balanceAfter: Double = 0.0,
    val source: String = "", // "INVOICE_SALE", "INVOICE_PURCHASE", "VOUCHER", "OPENING", "TRANSFER"
    val referenceId: Long? = null,
    val referenceNumber: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * دليل الحسابات (Chart of Accounts)
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val type: String, // "أصول", "خصوم", "حقوق ملكية", "إيرادات", "مصروفات"
    val parentId: Long? = null,
    val balance: Double = 0.0
)
