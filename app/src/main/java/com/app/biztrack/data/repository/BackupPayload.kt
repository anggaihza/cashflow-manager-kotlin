package com.app.biztrack.data.repository

import com.app.biztrack.data.local.entity.CashAccountEntity
import com.app.biztrack.data.local.entity.BusinessContactEntity
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
import com.app.biztrack.data.preferences.AppPreferences

data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val preferences: AppPreferences = AppPreferences(),
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val cashAccounts: List<CashAccountEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val debtReceivables: List<DebtReceivableEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val recurringTemplates: List<RecurringTemplateEntity> = emptyList(),
    val inventoryItems: List<InventoryItemEntity> = emptyList(),
    val inventoryMovements: List<InventoryMovementEntity> = emptyList(),
    val businessContacts: List<BusinessContactEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val invoiceItems: List<InvoiceItemEntity> = emptyList(),
)

data class BackupPreview(
    val exportedAt: Long,
    val businessName: String,
    val transactionCount: Int,
    val categoryCount: Int,
    val cashAccountCount: Int,
    val debtReceivableCount: Int,
    val reminderCount: Int,
    val recurringTemplateCount: Int,
    val inventoryItemCount: Int,
    val businessContactCount: Int,
    val invoiceCount: Int,
)
