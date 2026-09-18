package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val creditLimit: Double = 0.0,
    val openingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val companyName: String = "",
    val openingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "warehouses")
data class Warehouse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val location: String = "",
    val keeperName: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "items")
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
    val warehouseId: Long = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashboxes")
data class Cashbox(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "صندوق نقدي", // صندوق نقدي أو حساب بنكي
    val accountNumber: String = "",
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val currency: String = "SAR",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "currencies")
data class Currency(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // SAR, USD, YER, EGP, AED
    val name: String,
    val symbol: String,
    val exchangeRate: Double = 1.0,
    val isDefault: Boolean = false
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val invoiceType: String, // "SALE" (بيع), "PURCHASE" (شراء), "RETURN_SALE", "RETURN_PURCHASE"
    val partyType: String, // "CUSTOMER", "SUPPLIER"
    val partyId: Long,
    val partyName: String,
    val date: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discount: Double = 0.0,
    val taxRate: Double = 0.15, // 15% VAT
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val paymentStatus: String = "مدفوعة", // مدفوعة، آجلة، مدفوعة جزئياً
    val paymentMethod: String = "نقداً", // نقداً، شبكة، تحويل بنكي، آجل
    val notes: String = "",
    val warehouseId: Long = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val itemId: Long,
    val itemName: String,
    val unit: String = "قطعة",
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double
)

@Entity(tableName = "vouchers")
data class Voucher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherNumber: String,
    val type: String, // "RECEIPT" (سند قبض), "PAYMENT" (سند صرف)
    val partyType: String, // "CUSTOMER", "SUPPLIER", "GENERAL_EXPENSE", "CAPITAL"
    val partyId: Long? = null,
    val partyName: String,
    val amount: Double,
    val cashboxId: Long = 1,
    val paymentMethod: String = "نقداً",
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val type: String, // "أصول", "خصوم", "حقوق ملكية", "إيرادات", "مصروفات"
    val parentId: Long? = null,
    val balance: Double = 0.0
)
