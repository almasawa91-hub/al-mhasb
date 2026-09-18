package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        OrganizationSettings::class,
        Customer::class,
        Supplier::class,
        Warehouse::class,
        Item::class,
        Cashbox::class,
        Currency::class,
        Invoice::class,
        InvoiceItem::class,
        Voucher::class,
        StockMovement::class,
        CustomerTransaction::class,
        SupplierTransaction::class,
        CashTransaction::class,
        Account::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AccountingDatabase : RoomDatabase() {
    abstract fun accountingDao(): AccountingDao

    companion object {
        @Volatile
        private var INSTANCE: AccountingDatabase? = null

        // Real production migration from Version 1 to Version 2 preserving user data
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create organization_settings table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `organization_settings` (
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `activityType` TEXT NOT NULL,
                        `taxNumber` TEXT NOT NULL,
                        `commercialRegister` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `defaultTaxRate` REAL NOT NULL,
                        `isTaxEnabled` INTEGER NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `currencySymbol` TEXT NOT NULL,
                        `currencyName` TEXT NOT NULL,
                        `logoUri` TEXT NOT NULL,
                        `invoiceNotes` TEXT NOT NULL,
                        `isConfigured` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                // 2. Create stock_movements table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_movements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `warehouseId` INTEGER NOT NULL,
                        `movementType` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `unitCost` REAL NOT NULL,
                        `balanceAfter` REAL NOT NULL,
                        `referenceType` TEXT NOT NULL,
                        `referenceId` INTEGER,
                        `referenceNumber` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `date` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_itemId` ON `stock_movements` (`itemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_warehouseId` ON `stock_movements` (`warehouseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_date` ON `stock_movements` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_referenceType_referenceId` ON `stock_movements` (`referenceType`, `referenceId`)")

                // 3. Create customer_transactions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `customer_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `customerId` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `referenceNumber` TEXT NOT NULL,
                        `referenceId` INTEGER,
                        `description` TEXT NOT NULL,
                        `debit` REAL NOT NULL,
                        `credit` REAL NOT NULL,
                        `balanceAfter` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_transactions_customerId` ON `customer_transactions` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_transactions_date` ON `customer_transactions` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_transactions_referenceId` ON `customer_transactions` (`referenceId`)")

                // 4. Create supplier_transactions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `supplier_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `supplierId` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `referenceNumber` TEXT NOT NULL,
                        `referenceId` INTEGER,
                        `description` TEXT NOT NULL,
                        `debit` REAL NOT NULL,
                        `credit` REAL NOT NULL,
                        `balanceAfter` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_transactions_supplierId` ON `supplier_transactions` (`supplierId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_transactions_date` ON `supplier_transactions` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_transactions_referenceId` ON `supplier_transactions` (`referenceId`)")

                // 5. Create cash_transactions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cash_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cashboxId` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `movementType` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `balanceAfter` REAL NOT NULL,
                        `source` TEXT NOT NULL,
                        `referenceId` INTEGER,
                        `referenceNumber` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_transactions_cashboxId` ON `cash_transactions` (`cashboxId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_transactions_date` ON `cash_transactions` (`date`)")

                // 6. Alter existing tables if missing new columns
                try {
                    db.execSQL("ALTER TABLE `customers` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}

                try {
                    db.execSQL("ALTER TABLE `suppliers` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}

                try {
                    db.execSQL("ALTER TABLE `items` ADD COLUMN `defaultWarehouseId` INTEGER")
                    db.execSQL("ALTER TABLE `items` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}

                try {
                    db.execSQL("ALTER TABLE `invoices` ADD COLUMN `warehouseName` TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE `invoices` ADD COLUMN `cashboxId` INTEGER")
                    db.execSQL("ALTER TABLE `invoices` ADD COLUMN `cashboxName` TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE `invoices` ADD COLUMN `currencyCode` TEXT NOT NULL DEFAULT 'SAR'")
                    db.execSQL("ALTER TABLE `invoices` ADD COLUMN `isCancelled` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}

                try {
                    db.execSQL("ALTER TABLE `invoice_items` ADD COLUMN `purchaseCost` REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}

                try {
                    db.execSQL("ALTER TABLE `vouchers` ADD COLUMN `cashboxName` TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE `vouchers` ADD COLUMN `currencyCode` TEXT NOT NULL DEFAULT 'SAR'")
                    db.execSQL("ALTER TABLE `vouchers` ADD COLUMN `isCancelled` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): AccountingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AccountingDatabase::class.java,
                    "al_muhasib.db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
