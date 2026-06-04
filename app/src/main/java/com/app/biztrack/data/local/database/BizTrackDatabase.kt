package com.app.biztrack.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.biztrack.data.local.dao.FinanceDao
import com.app.biztrack.data.local.entity.BusinessContactEntity
import com.app.biztrack.data.local.entity.CashAccountEntity
import com.app.biztrack.data.local.entity.CategoryEntity
import com.app.biztrack.data.local.entity.DebtReceivableEntity
import com.app.biztrack.data.local.entity.InvoiceEntity
import com.app.biztrack.data.local.entity.InvoiceItemEntity
import com.app.biztrack.data.local.entity.InventoryItemEntity
import com.app.biztrack.data.local.entity.InventoryMovementEntity
import com.app.biztrack.data.local.entity.PaymentMethodEntity
import com.app.biztrack.data.local.entity.RecurringTemplateEntity
import com.app.biztrack.data.local.entity.ReminderEntity
import com.app.biztrack.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CashAccountEntity::class,
        PaymentMethodEntity::class,
        DebtReceivableEntity::class,
        ReminderEntity::class,
        RecurringTemplateEntity::class,
        InventoryItemEntity::class,
        InventoryMovementEntity::class,
        BusinessContactEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class BizTrackDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile private var instance: BizTrackDatabase? = null

        fun getInstance(context: Context): BizTrackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BizTrackDatabase::class.java,
                    "biztrack.db",
                )
                    .addMigrations(Migration1To2)
                    .addMigrations(Migration2To3)
                    .addMigrations(Migration3To4)
                    .addMigrations(Migration4To5)
                    .build()
                    .also { instance = it }
            }

        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `categoryId` INTEGER,
                        `cashAccountId` INTEGER NOT NULL,
                        `paymentMethodId` INTEGER,
                        `note` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inventory_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `sku` TEXT NOT NULL,
                        `stock` INTEGER NOT NULL,
                        `costPrice` INTEGER NOT NULL,
                        `salePrice` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inventory_movements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_movements_itemId` ON `inventory_movements` (`itemId`)")
            }
        }

        val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `business_contacts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_business_contacts_type` ON `business_contacts` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_business_contacts_name` ON `business_contacts` (`name`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `invoices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoiceNumber` TEXT NOT NULL,
                        `contactId` INTEGER,
                        `issueDate` INTEGER NOT NULL,
                        `dueDate` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `totalAmount` INTEGER NOT NULL,
                        `paidAmount` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `transactionId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_invoiceNumber` ON `invoices` (`invoiceNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_status` ON `invoices` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_contactId` ON `invoices` (`contactId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_dueDate` ON `invoices` (`dueDate`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `invoice_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoiceId` INTEGER NOT NULL,
                        `inventoryItemId` INTEGER,
                        `description` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitPrice` INTEGER NOT NULL,
                        `totalAmount` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_invoiceId` ON `invoice_items` (`invoiceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_inventoryItemId` ON `invoice_items` (`inventoryItemId`)")
            }
        }

        val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `inventory_items` ADD COLUMN `minStock` INTEGER NOT NULL DEFAULT 5")
            }
        }
    }
}
