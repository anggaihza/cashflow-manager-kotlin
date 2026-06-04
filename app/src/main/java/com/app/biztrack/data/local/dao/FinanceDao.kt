package com.app.biztrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeTransaction(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransaction(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, createdAt DESC")
    suspend fun getTransactionsBetween(startDate: Long, endDate: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY type, name")
    fun observeActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type, name")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: Long): Int

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivateCategory(id: Long, updatedAt: Long)

    @Query("SELECT * FROM cash_accounts WHERE isActive = 1 ORDER BY name")
    fun observeActiveCashAccounts(): Flow<List<CashAccountEntity>>

    @Query("SELECT * FROM cash_accounts ORDER BY name")
    suspend fun getAllCashAccounts(): List<CashAccountEntity>

    @Query("SELECT COUNT(*) FROM cash_accounts")
    suspend fun countCashAccounts(): Int

    @Insert
    suspend fun insertCashAccount(account: CashAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCashAccounts(accounts: List<CashAccountEntity>)

    @Update
    suspend fun updateCashAccount(account: CashAccountEntity)

    @Query("UPDATE cash_accounts SET currentBalance = currentBalance + :delta, updatedAt = :updatedAt WHERE id = :accountId")
    suspend fun adjustCashAccountBalance(accountId: Long, delta: Long, updatedAt: Long)

    @Query("UPDATE cash_accounts SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivateCashAccount(id: Long, updatedAt: Long)

    @Query("SELECT * FROM payment_methods WHERE isActive = 1 ORDER BY name")
    fun observeActivePaymentMethods(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods ORDER BY name")
    suspend fun getAllPaymentMethods(): List<PaymentMethodEntity>

    @Query("SELECT COUNT(*) FROM payment_methods")
    suspend fun countPaymentMethods(): Int

    @Insert
    suspend fun insertPaymentMethod(method: PaymentMethodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaymentMethods(methods: List<PaymentMethodEntity>)

    @Update
    suspend fun updatePaymentMethod(method: PaymentMethodEntity)

    @Query("UPDATE payment_methods SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivatePaymentMethod(id: Long, updatedAt: Long)

    @Query("SELECT * FROM debt_receivables ORDER BY dueDate ASC, createdAt DESC")
    fun observeDebtReceivables(): Flow<List<DebtReceivableEntity>>

    @Insert
    suspend fun insertDebtReceivable(item: DebtReceivableEntity): Long

    @Update
    suspend fun updateDebtReceivable(item: DebtReceivableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDebtReceivables(items: List<DebtReceivableEntity>)

    @Query("SELECT * FROM debt_receivables ORDER BY dueDate ASC, createdAt DESC")
    suspend fun getAllDebtReceivables(): List<DebtReceivableEntity>

    @Query("SELECT * FROM reminders ORDER BY reminderDate ASC")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Insert
    suspend fun insertReminder(item: ReminderEntity): Long

    @Update
    suspend fun updateReminder(item: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminders(items: List<ReminderEntity>)

    @Query("SELECT * FROM reminders ORDER BY reminderDate ASC")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Query("SELECT * FROM recurring_templates WHERE isActive = 1 ORDER BY title")
    fun observeActiveRecurringTemplates(): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates ORDER BY title")
    suspend fun getAllRecurringTemplates(): List<RecurringTemplateEntity>

    @Insert
    suspend fun insertRecurringTemplate(item: RecurringTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringTemplates(items: List<RecurringTemplateEntity>)

    @Query("UPDATE recurring_templates SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivateRecurringTemplate(id: Long, updatedAt: Long)

    @Query("SELECT * FROM inventory_items WHERE isActive = 1 ORDER BY name")
    fun observeActiveInventoryItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM business_contacts WHERE isActive = 1 ORDER BY name")
    fun observeActiveBusinessContacts(): Flow<List<BusinessContactEntity>>

    @Query("SELECT * FROM business_contacts ORDER BY name")
    suspend fun getAllBusinessContacts(): List<BusinessContactEntity>

    @Insert
    suspend fun insertBusinessContact(item: BusinessContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBusinessContacts(items: List<BusinessContactEntity>)

    @Query("UPDATE business_contacts SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivateBusinessContact(id: Long, updatedAt: Long)

    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, createdAt DESC")
    fun observeInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, createdAt DESC")
    suspend fun getAllInvoices(): List<InvoiceEntity>

    @Query("SELECT * FROM invoice_items ORDER BY invoiceId, id")
    suspend fun getAllInvoiceItems(): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items ORDER BY invoiceId, id")
    fun observeInvoiceItems(): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY id")
    suspend fun getInvoiceItems(invoiceId: Long): List<InvoiceItemEntity>

    @Insert
    suspend fun insertInvoice(item: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(item: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoices(items: List<InvoiceEntity>)

    @Insert
    suspend fun insertInvoiceItem(item: InvoiceItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM inventory_items ORDER BY name")
    suspend fun getAllInventoryItems(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_movements ORDER BY createdAt DESC")
    suspend fun getAllInventoryMovements(): List<InventoryMovementEntity>

    @Insert
    suspend fun insertInventoryItem(item: InventoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventoryItems(items: List<InventoryItemEntity>)

    @Update
    suspend fun updateInventoryItem(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventoryMovements(items: List<InventoryMovementEntity>)

    @Insert
    suspend fun insertInventoryMovement(item: InventoryMovementEntity): Long

    @Query("UPDATE inventory_items SET stock = stock + :delta, updatedAt = :updatedAt WHERE id = :id")
    suspend fun adjustInventoryStock(id: Long, delta: Long, updatedAt: Long)

    @Query("UPDATE inventory_items SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivateInventoryItem(id: Long, updatedAt: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM cash_accounts")
    suspend fun clearCashAccounts()

    @Query("DELETE FROM payment_methods")
    suspend fun clearPaymentMethods()

    @Query("DELETE FROM debt_receivables")
    suspend fun clearDebtReceivables()

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM recurring_templates")
    suspend fun clearRecurringTemplates()

    @Query("DELETE FROM inventory_items")
    suspend fun clearInventoryItems()

    @Query("DELETE FROM inventory_movements")
    suspend fun clearInventoryMovements()

    @Query("DELETE FROM business_contacts")
    suspend fun clearBusinessContacts()

    @Query("DELETE FROM invoices")
    suspend fun clearInvoices()

    @Query("DELETE FROM invoice_items")
    suspend fun clearInvoiceItems()
}
