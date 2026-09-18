package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Customer::class,
        Supplier::class,
        Warehouse::class,
        Item::class,
        Cashbox::class,
        Currency::class,
        Invoice::class,
        InvoiceItem::class,
        Voucher::class,
        Account::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AccountingDatabase : RoomDatabase() {
    abstract fun accountingDao(): AccountingDao

    companion object {
        @Volatile
        private var INSTANCE: AccountingDatabase? = null

        fun getDatabase(context: Context): AccountingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AccountingDatabase::class.java,
                    "al_muhasib.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
