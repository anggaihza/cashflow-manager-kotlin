package com.app.biztrack.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BizTrackMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BizTrackDatabase::class.java,
    )

    @Test
    fun migrate1To5PreservesCoreTablesAndAddsOperationalTables() {
        helper.createDatabase(TEST_DB, 1).apply {
            createVersionOneTables(this)
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            BizTrackDatabase.Migration1To2,
            BizTrackDatabase.Migration2To3,
            BizTrackDatabase.Migration3To4,
            BizTrackDatabase.Migration4To5,
        )
    }

    private fun createVersionOneTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `date` INTEGER NOT NULL, `categoryId` INTEGER, `cashAccountId` INTEGER NOT NULL, `targetCashAccountId` INTEGER, `paymentMethodId` INTEGER, `note` TEXT NOT NULL, `attachmentPath` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `color` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `cash_accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `initialBalance` INTEGER NOT NULL, `currentBalance` INTEGER NOT NULL, `note` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `payment_methods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `debt_receivables` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `partyName` TEXT NOT NULL, `totalAmount` INTEGER NOT NULL, `paidAmount` INTEGER NOT NULL, `remainingAmount` INTEGER NOT NULL, `dueDate` INTEGER, `status` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `reminderDate` INTEGER NOT NULL, `relatedType` TEXT, `relatedId` INTEGER, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_cashAccountId` ON `transactions` (`cashAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_paymentMethodId` ON `transactions` (`paymentMethodId`)")
    }

    private companion object {
        const val TEST_DB = "biztrack-migration-test"
    }
}
