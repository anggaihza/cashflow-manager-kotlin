package com.app.biztrack.data.repository

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.app.biztrack.data.local.database.BizTrackDatabase
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
import com.app.biztrack.data.preferences.AppPreferences
import com.app.biztrack.data.preferences.AppPreferencesDataSource
import com.app.biztrack.domain.model.BalanceEffect
import com.app.biztrack.domain.model.BusinessRules
import com.app.biztrack.domain.model.CategoryTypes
import com.app.biztrack.domain.model.DebtStatus
import com.app.biztrack.domain.model.InvoiceStatus
import com.app.biztrack.domain.model.TransactionTypes
import com.app.biztrack.utils.formatMoney
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class FinanceRepository(
    private val database: BizTrackDatabase,
    private val preferencesDataSource: AppPreferencesDataSource,
) {
    private val dao = database.financeDao()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    val preferences: Flow<AppPreferences> = preferencesDataSource.preferences
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val categories: Flow<List<CategoryEntity>> = dao.observeActiveCategories()
    val cashAccounts: Flow<List<CashAccountEntity>> = dao.observeActiveCashAccounts()
    val paymentMethods: Flow<List<PaymentMethodEntity>> = dao.observeActivePaymentMethods()
    val debtReceivables: Flow<List<DebtReceivableEntity>> = dao.observeDebtReceivables()
    val reminders: Flow<List<ReminderEntity>> = dao.observeReminders()
    val recurringTemplates: Flow<List<RecurringTemplateEntity>> = dao.observeActiveRecurringTemplates()
    val inventoryItems: Flow<List<InventoryItemEntity>> = dao.observeActiveInventoryItems()
    val businessContacts: Flow<List<BusinessContactEntity>> = dao.observeActiveBusinessContacts()
    val invoices: Flow<List<InvoiceEntity>> = dao.observeInvoices()
    val invoiceItems: Flow<List<InvoiceItemEntity>> = dao.observeInvoiceItems()

    data class InvoiceLineInput(
        val inventoryItemId: Long?,
        val description: String,
        val quantity: Long,
        val unitPrice: Long,
    )

    fun observeTransaction(id: Long): Flow<TransactionEntity?> = dao.observeTransaction(id)

    suspend fun completeOnboarding(
        businessName: String,
        currency: String,
        accountName: String,
        initialBalance: Long,
        useDarkMode: Boolean,
    ) {
        database.withTransaction {
            if (dao.countCategories() == 0) {
                dao.upsertCategories(defaultIncomeCategories() + defaultExpenseCategories())
            }
            if (dao.countPaymentMethods() == 0) {
                dao.upsertPaymentMethods(defaultPaymentMethods())
            }
            if (dao.countCashAccounts() == 0) {
                val now = System.currentTimeMillis()
                dao.insertCashAccount(
                    CashAccountEntity(
                        name = accountName.ifBlank { "Cash" },
                        initialBalance = initialBalance,
                        currentBalance = initialBalance,
                        note = "Akun kas pertama",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }
        preferencesDataSource.completeOnboarding(
            businessName = businessName.ifBlank { "Bisnis Saya" },
            currency = currency.ifBlank { "IDR" },
            themeMode = if (useDarkMode) "dark" else "light",
        )
    }

    suspend fun addTransaction(transaction: TransactionEntity): Long {
        require(transaction.amount > 0) { "Nominal harus lebih dari 0." }
        return database.withTransaction {
            validateTransactionEffect(transaction)
            val id = dao.insertTransaction(transaction)
            applyEffect(transaction.copy(id = id))
            id
        }
    }

    suspend fun updateTransaction(updated: TransactionEntity) {
        require(updated.amount > 0) { "Nominal harus lebih dari 0." }
        database.withTransaction {
            val existing = dao.getTransaction(updated.id) ?: return@withTransaction
            revertEffect(existing)
            validateTransactionEffect(updated)
            dao.updateTransaction(updated.copy(updatedAt = System.currentTimeMillis()))
            applyEffect(updated)
        }
    }

    suspend fun deleteTransaction(id: Long) {
        database.withTransaction {
            val existing = dao.getTransaction(id) ?: return@withTransaction
            revertEffect(existing)
            dao.deleteTransaction(existing)
        }
    }

    suspend fun createCategory(name: String, type: String, color: String) {
        if (name.isBlank()) return
        dao.insertCategory(
            CategoryEntity(
                name = name.trim(),
                type = type,
                color = color,
                isDefault = false,
            ),
        )
    }

    suspend fun createBusinessContact(
        name: String,
        type: String,
        phone: String,
        email: String,
        address: String,
        note: String,
    ) {
        if (name.isBlank()) return
        dao.insertBusinessContact(
            BusinessContactEntity(
                name = name.trim(),
                type = type,
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                note = note.trim(),
            ),
        )
    }

    suspend fun deactivateBusinessContact(id: Long) {
        dao.deactivateBusinessContact(id, System.currentTimeMillis())
    }

    suspend fun createInvoice(
        invoiceNumber: String,
        contactId: Long?,
        issueDate: Long,
        dueDate: Long,
        status: String,
        lines: List<InvoiceLineInput>,
        note: String,
        createDueReminder: Boolean,
    ): Long {
        val validLines = lines
            .filter { it.description.isNotBlank() && it.quantity > 0 && it.unitPrice > 0 }
        require(validLines.isNotEmpty()) { "Minimal satu item invoice wajib diisi." }
        val now = System.currentTimeMillis()
        val total = validLines.sumOf { it.quantity * it.unitPrice }
        val finalInvoiceNumber = invoiceNumber.ifBlank { "INV-${now.toString().takeLast(6)}" }
        return database.withTransaction {
            val invoiceId = dao.insertInvoice(
                InvoiceEntity(
                    invoiceNumber = finalInvoiceNumber,
                    contactId = contactId,
                    issueDate = issueDate,
                    dueDate = dueDate,
                    status = status,
                    totalAmount = total,
                    note = note.trim(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            validLines.forEach { line ->
                dao.insertInvoiceItem(
                    InvoiceItemEntity(
                        invoiceId = invoiceId,
                        inventoryItemId = line.inventoryItemId,
                        description = line.description.trim(),
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        totalAmount = line.quantity * line.unitPrice,
                    ),
                )
            }
            if (createDueReminder && dueDate > now) {
                dao.insertReminder(
                    ReminderEntity(
                        title = "Invoice jatuh tempo",
                        description = "Tagih $finalInvoiceNumber",
                        reminderDate = dueDate,
                        relatedType = "invoice",
                        relatedId = invoiceId,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            invoiceId
        }
    }

    suspend fun recordInvoicePayment(
        invoice: InvoiceEntity,
        amount: Long,
        cashAccountId: Long,
        paymentMethodId: Long?,
    ) {
        require(amount > 0) { "Nominal pembayaran harus lebih dari 0." }
        require(invoice.status != InvoiceStatus.PAID) { "Invoice sudah lunas." }
        val now = System.currentTimeMillis()
        database.withTransaction {
            val payment = amount.coerceAtMost(invoice.totalAmount - invoice.paidAmount)
            require(payment > 0) { "Nominal pembayaran melebihi sisa invoice." }
            val invoiceItems = dao.getInvoiceItems(invoice.id)
            val willBePaid = invoice.paidAmount + payment >= invoice.totalAmount
            if (invoice.paidAmount == 0L && willBePaid) {
                invoiceItems
                    .filter { it.inventoryItemId != null }
                    .forEach { line ->
                        val itemId = line.inventoryItemId ?: return@forEach
                        validateInventoryMovement(itemId, "out", line.quantity)
                    }
            } else if (invoiceItems.any { it.inventoryItemId != null } && !willBePaid) {
                error("Invoice dengan produk inventory harus dilunasi penuh agar stok akurat.")
            }
            val transaction = TransactionEntity(
                type = TransactionTypes.INCOME,
                amount = payment,
                date = now,
                categoryId = null,
                cashAccountId = cashAccountId,
                paymentMethodId = paymentMethodId,
                note = "Pembayaran ${invoice.invoiceNumber}",
                createdAt = now,
                updatedAt = now,
            )
            val transactionId = dao.insertTransaction(transaction)
            applyEffect(transaction.copy(id = transactionId))
            if (invoice.paidAmount == 0L && willBePaid) {
                invoiceItems
                    .filter { it.inventoryItemId != null }
                    .forEach { line ->
                        val itemId = line.inventoryItemId ?: return@forEach
                        dao.insertInventoryMovement(
                            InventoryMovementEntity(
                                itemId = itemId,
                                type = "out",
                                quantity = line.quantity,
                                note = "Invoice ${invoice.invoiceNumber}: ${line.description}",
                                createdAt = now,
                            ),
                        )
                        dao.adjustInventoryStock(itemId, -line.quantity, now)
                    }
            }
            val paidAmount = invoice.paidAmount + payment
            dao.updateInvoice(
                invoice.copy(
                    status = if (paidAmount >= invoice.totalAmount) InvoiceStatus.PAID else InvoiceStatus.PARTIAL,
                    paidAmount = paidAmount.coerceAtMost(invoice.totalAmount),
                    transactionId = transactionId,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun markInvoicePaid(invoice: InvoiceEntity, cashAccountId: Long, paymentMethodId: Long?) {
        recordInvoicePayment(
            invoice = invoice,
            amount = invoice.totalAmount - invoice.paidAmount,
            cashAccountId = cashAccountId,
            paymentMethodId = paymentMethodId,
        )
    }

    suspend fun markInvoiceSent(invoice: InvoiceEntity) {
        require(invoice.status != InvoiceStatus.PAID) { "Invoice sudah lunas." }
        dao.updateInvoice(
            invoice.copy(
                status = InvoiceStatus.SENT,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deactivateCategory(id: Long) {
        dao.deactivateCategory(id, System.currentTimeMillis())
    }

    suspend fun createCashAccount(name: String, initialBalance: Long, note: String) {
        if (name.isBlank()) return
        val now = System.currentTimeMillis()
        dao.insertCashAccount(
            CashAccountEntity(
                name = name.trim(),
                initialBalance = initialBalance,
                currentBalance = initialBalance,
                note = note.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateCashAccount(account: CashAccountEntity) {
        dao.updateCashAccount(account.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deactivateCashAccount(id: Long) {
        dao.deactivateCashAccount(id, System.currentTimeMillis())
    }

    suspend fun seedDemoData(): Boolean = database.withTransaction {
        if (dao.getAllTransactions().isNotEmpty()) {
            return@withTransaction false
        }

        if (dao.countCategories() == 0) {
            dao.upsertCategories(defaultIncomeCategories() + defaultExpenseCategories())
        }
        if (dao.countPaymentMethods() == 0) {
            dao.upsertPaymentMethods(defaultPaymentMethods())
        }

        val now = System.currentTimeMillis()
        val accounts = dao.getAllCashAccounts().filter { it.isActive }
        val cashId = accounts.firstOrNull()?.id ?: dao.insertCashAccount(
            CashAccountEntity(
                name = "Cash",
                initialBalance = 1_250_000,
                currentBalance = 1_250_000,
                note = "Kas operasional harian",
                createdAt = now,
                updatedAt = now,
            ),
        )
        val bankId = accounts.firstOrNull { it.name.contains("bank", ignoreCase = true) }?.id
            ?: dao.insertCashAccount(
                CashAccountEntity(
                    name = "Bank BCA",
                    initialBalance = 12_500_000,
                    currentBalance = 12_500_000,
                    note = "Rekening utama bisnis",
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        val qrisId = accounts.firstOrNull { it.name.contains("qris", ignoreCase = true) }?.id
            ?: dao.insertCashAccount(
                CashAccountEntity(
                    name = "QRIS",
                    initialBalance = 2_750_000,
                    currentBalance = 2_750_000,
                    note = "Settlement pembayaran digital",
                    createdAt = now,
                    updatedAt = now,
                ),
            )

        val categories = dao.getAllCategories()
        val methods = dao.getAllPaymentMethods()
        fun categoryId(type: String, name: String): Long? =
            categories.firstOrNull { it.type == type && it.name.equals(name, ignoreCase = true) }?.id
                ?: categories.firstOrNull { it.type == type }?.id

        fun methodId(name: String): Long? =
            methods.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
                ?: methods.firstOrNull()?.id

        val startOfToday = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        fun daysAgo(days: Long): Long = startOfToday - (days * 86_400_000L)

        val demoTransactions = listOf(
            TransactionEntity(
                type = TransactionTypes.INCOME,
                amount = 4_850_000,
                date = daysAgo(0),
                categoryId = categoryId(CategoryTypes.INCOME, "Penjualan"),
                cashAccountId = qrisId,
                paymentMethodId = methodId("QRIS"),
                note = "Penjualan paket bundling akhir pekan",
                createdAt = now - 1_000,
                updatedAt = now - 1_000,
            ),
            TransactionEntity(
                type = TransactionTypes.EXPENSE,
                amount = 1_175_000,
                date = daysAgo(0),
                categoryId = categoryId(CategoryTypes.EXPENSE, "Bahan baku"),
                cashAccountId = bankId,
                paymentMethodId = methodId("Transfer Bank"),
                note = "Restock bahan utama supplier",
                createdAt = now - 2_000,
                updatedAt = now - 2_000,
            ),
            TransactionEntity(
                type = TransactionTypes.INCOME,
                amount = 2_250_000,
                date = daysAgo(1),
                categoryId = categoryId(CategoryTypes.INCOME, "Jasa"),
                cashAccountId = bankId,
                paymentMethodId = methodId("Transfer Bank"),
                note = "Invoice jasa konsultasi client A",
                createdAt = now - 3_000,
                updatedAt = now - 3_000,
            ),
            TransactionEntity(
                type = TransactionTypes.EXPENSE,
                amount = 425_000,
                date = daysAgo(2),
                categoryId = categoryId(CategoryTypes.EXPENSE, "Iklan"),
                cashAccountId = bankId,
                paymentMethodId = methodId("Debit"),
                note = "Campaign marketplace dan sosial media",
                createdAt = now - 4_000,
                updatedAt = now - 4_000,
            ),
            TransactionEntity(
                type = TransactionTypes.INCOME,
                amount = 1_780_000,
                date = daysAgo(3),
                categoryId = categoryId(CategoryTypes.INCOME, "Pelunasan"),
                cashAccountId = cashId,
                paymentMethodId = methodId("Cash"),
                note = "Pelunasan order grosir toko sekitar",
                createdAt = now - 5_000,
                updatedAt = now - 5_000,
            ),
            TransactionEntity(
                type = TransactionTypes.EXPENSE,
                amount = 780_000,
                date = daysAgo(4),
                categoryId = categoryId(CategoryTypes.EXPENSE, "Operasional"),
                cashAccountId = cashId,
                paymentMethodId = methodId("Cash"),
                note = "Kebutuhan operasional outlet",
                createdAt = now - 6_000,
                updatedAt = now - 6_000,
            ),
            TransactionEntity(
                type = TransactionTypes.INCOME,
                amount = 3_350_000,
                date = daysAgo(6),
                categoryId = categoryId(CategoryTypes.INCOME, "DP"),
                cashAccountId = bankId,
                paymentMethodId = methodId("Transfer Bank"),
                note = "DP project custom bulan ini",
                createdAt = now - 7_000,
                updatedAt = now - 7_000,
            ),
            TransactionEntity(
                type = TransactionTypes.EXPENSE,
                amount = 1_450_000,
                date = daysAgo(8),
                categoryId = categoryId(CategoryTypes.EXPENSE, "Sewa"),
                cashAccountId = bankId,
                paymentMethodId = methodId("Transfer Bank"),
                note = "Sewa kios periode berjalan",
                createdAt = now - 8_000,
                updatedAt = now - 8_000,
            ),
        )

        demoTransactions.forEach { transaction ->
            val id = dao.insertTransaction(transaction)
            applyEffect(transaction.copy(id = id))
        }
        true
    }

    suspend fun updateBusinessInfo(businessName: String, currency: String) {
        preferencesDataSource.updateBusinessInfo(businessName, currency)
    }

    suspend fun updateThemeMode(themeMode: String) {
        preferencesDataSource.updateThemeMode(themeMode)
    }

    suspend fun updateMonthlyTargets(incomeTarget: Long, expenseLimit: Long) {
        preferencesDataSource.updateMonthlyTargets(incomeTarget, expenseLimit)
    }

    suspend fun updatePin(pinHash: String) {
        preferencesDataSource.updatePin(pinHash)
    }

    suspend fun transferCash(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        date: Long,
        note: String,
    ): Long {
        require(fromAccountId != toAccountId) { "Akun asal dan tujuan harus berbeda." }
        require(amount > 0) { "Nominal transfer harus lebih dari 0." }
        return addTransaction(
            TransactionEntity(
                type = TransactionTypes.TRANSFER,
                amount = amount,
                date = date,
                cashAccountId = fromAccountId,
                targetCashAccountId = toAccountId,
                note = note.trim(),
            ),
        )
    }

    suspend fun createDebtReceivable(
        type: String,
        partyName: String,
        totalAmount: Long,
        dueDate: Long?,
        note: String,
    ) {
        if (partyName.isBlank()) return
        require(totalAmount > 0) { "Nominal harus lebih dari 0." }
        val now = System.currentTimeMillis()
        dao.insertDebtReceivable(
            DebtReceivableEntity(
                type = type,
                partyName = partyName.trim(),
                totalAmount = totalAmount,
                paidAmount = 0,
                remainingAmount = totalAmount,
                dueDate = dueDate,
                status = DebtStatus.UNPAID,
                note = note.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun recordDebtPayment(item: DebtReceivableEntity, amount: Long, cashAccountId: Long) {
        require(amount > 0) { "Nominal pembayaran harus lebih dari 0." }
        database.withTransaction {
            val payment = amount.coerceAtMost(item.remainingAmount)
            val paid = (item.paidAmount + payment).coerceAtMost(item.totalAmount)
            val remaining = (item.totalAmount - paid).coerceAtLeast(0)
            val status = when {
                remaining == 0L -> DebtStatus.PAID
                paid > 0 -> DebtStatus.PARTIAL
                else -> DebtStatus.UNPAID
            }
            val transactionType = if (item.type == com.app.biztrack.domain.model.DebtTypes.RECEIVABLE) {
                TransactionTypes.INCOME
            } else {
                TransactionTypes.EXPENSE
            }
            val transaction = TransactionEntity(
                type = transactionType,
                amount = payment,
                date = System.currentTimeMillis(),
                cashAccountId = cashAccountId,
                note = "Pembayaran ${item.partyName}: ${item.note}".trim(),
            )
            validateTransactionEffect(transaction)
            val id = dao.insertTransaction(transaction)
            applyEffect(transaction.copy(id = id))
            dao.updateDebtReceivable(
                item.copy(
                    paidAmount = paid,
                    remainingAmount = remaining,
                    status = status,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun createRecurringTemplate(
        title: String,
        type: String,
        amount: Long,
        categoryId: Long?,
        cashAccountId: Long,
        paymentMethodId: Long?,
        note: String,
    ) {
        if (title.isBlank()) return
        require(amount > 0) { "Nominal template harus lebih dari 0." }
        dao.insertRecurringTemplate(
            RecurringTemplateEntity(
                title = title.trim(),
                type = type,
                amount = amount,
                categoryId = categoryId,
                cashAccountId = cashAccountId,
                paymentMethodId = paymentMethodId,
                note = note.trim(),
            ),
        )
    }

    suspend fun createTransactionFromTemplate(template: RecurringTemplateEntity, date: Long) {
        addTransaction(
            TransactionEntity(
                type = template.type,
                amount = template.amount,
                date = date,
                categoryId = template.categoryId,
                cashAccountId = template.cashAccountId,
                paymentMethodId = template.paymentMethodId,
                note = template.note.ifBlank { template.title },
            ),
        )
    }

    suspend fun deactivateRecurringTemplate(id: Long) {
        dao.deactivateRecurringTemplate(id, System.currentTimeMillis())
    }

    suspend fun createInventoryItem(
        name: String,
        sku: String,
        stock: Long,
        minStock: Long,
        costPrice: Long,
        salePrice: Long,
    ) {
        if (name.isBlank()) return
        dao.insertInventoryItem(
            InventoryItemEntity(
                name = name.trim(),
                sku = sku.trim(),
                stock = stock.coerceAtLeast(0),
                minStock = minStock.coerceAtLeast(0),
                costPrice = costPrice.coerceAtLeast(0),
                salePrice = salePrice.coerceAtLeast(0),
            ),
        )
    }

    suspend fun updateInventoryMinStock(item: InventoryItemEntity, minStock: Long) {
        dao.updateInventoryItem(
            item.copy(
                minStock = minStock.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun recordInventoryMovement(itemId: Long, type: String, quantity: Long, note: String) {
        require(quantity > 0) { "Jumlah stok harus lebih dari 0." }
        val delta = if (type == "in") quantity else -quantity
        database.withTransaction {
            validateInventoryMovement(itemId, type, quantity)
            dao.insertInventoryMovement(
                InventoryMovementEntity(
                    itemId = itemId,
                    type = type,
                    quantity = quantity,
                    note = note.trim(),
                ),
            )
            dao.adjustInventoryStock(itemId, delta, System.currentTimeMillis())
        }
    }

    suspend fun recordInventoryMovementWithTransaction(
        itemId: Long,
        type: String,
        quantity: Long,
        note: String,
        cashAccountId: Long,
        paymentMethodId: Long?,
    ) {
        require(quantity > 0) { "Jumlah stok harus lebih dari 0." }
        val item = dao.getAllInventoryItems().firstOrNull { it.id == itemId }
            ?: error("Produk tidak ditemukan.")
        val amount = if (type == "in") item.costPrice * quantity else item.salePrice * quantity
        require(amount > 0) { "Harga produk belum diisi, transaksi kas tidak bisa dibuat otomatis." }
        val transactionType = if (type == "in") TransactionTypes.EXPENSE else TransactionTypes.INCOME
        database.withTransaction {
            validateInventoryMovement(itemId, type, quantity)
            validateTransactionEffect(
                TransactionEntity(
                    type = transactionType,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    cashAccountId = cashAccountId,
                    paymentMethodId = paymentMethodId,
                ),
            )
            dao.insertInventoryMovement(
                InventoryMovementEntity(
                    itemId = itemId,
                    type = type,
                    quantity = quantity,
                    note = note.trim(),
                ),
            )
            dao.adjustInventoryStock(itemId, if (type == "in") quantity else -quantity, System.currentTimeMillis())
            val transaction = TransactionEntity(
                type = transactionType,
                amount = amount,
                date = System.currentTimeMillis(),
                cashAccountId = cashAccountId,
                paymentMethodId = paymentMethodId,
                note = note.ifBlank {
                    if (type == "in") "Pembelian stok ${item.name}" else "Penjualan stok ${item.name}"
                },
            )
            val id = dao.insertTransaction(transaction)
            applyEffect(transaction.copy(id = id))
        }
    }

    suspend fun deactivateInventoryItem(id: Long) {
        dao.deactivateInventoryItem(id, System.currentTimeMillis())
    }

    suspend fun createReminder(title: String, description: String, reminderDate: Long) {
        if (title.isBlank()) return
        dao.insertReminder(
            ReminderEntity(
                title = title.trim(),
                description = description.trim(),
                reminderDate = reminderDate,
            ),
        )
    }

    suspend fun deactivateReminder(item: ReminderEntity) {
        dao.updateReminder(item.copy(isActive = false, updatedAt = System.currentTimeMillis()))
    }

    suspend fun exportCsv(context: Context, startDate: Long, endDate: Long): File {
        val transactions = dao.getTransactionsBetween(startDate, endDate)
        val categories = dao.getAllCategories().associateBy { it.id }
        val accounts = dao.getAllCashAccounts().associateBy { it.id }
        val methods = dao.getAllPaymentMethods().associateBy { it.id }
        val settings = preferences.first()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withLocale(Locale.forLanguageTag("id-ID"))
            .withZone(ZoneId.systemDefault())

        val csv = buildString {
            appendLine("Tanggal,Tipe,Kategori,Akun Kas,Metode Pembayaran,Nominal,Catatan")
            transactions.forEach { tx ->
                appendLine(
                    listOf(
                        formatter.format(Instant.ofEpochMilli(tx.date)),
                        tx.type,
                        categories[tx.categoryId]?.name ?: "-",
                        accounts[tx.cashAccountId]?.name ?: "-",
                        methods[tx.paymentMethodId]?.name ?: "-",
                        tx.amount.toString(),
                        tx.note,
                    ).joinToString(",") { it.csvEscape() },
                )
            }
        }

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val fileName = "BizTrack_${settings.businessName.ifBlank { "Laporan" }}_${System.currentTimeMillis()}.csv"
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return File(directory, fileName).also { it.writeText(csv) }
    }

    suspend fun exportPdf(context: Context, startDate: Long, endDate: Long): File {
        val transactions = dao.getTransactionsBetween(startDate, endDate)
        val settings = preferences.first()
        val income = transactions.sumOf { if (it.type == TransactionTypes.INCOME) it.amount else 0 }
        val expense = transactions.sumOf { if (it.type == TransactionTypes.EXPENSE) it.amount else 0 }
        val profit = income - expense
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withLocale(Locale.forLanguageTag("id-ID"))
            .withZone(ZoneId.systemDefault())

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
            color = android.graphics.Color.WHITE
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.rgb(8, 30, 28)
        }
        val mutedPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.rgb(101, 112, 107)
        }
        val emeraldPaint = Paint().apply {
            color = android.graphics.Color.rgb(0, 76, 67)
        }
        val goldPaint = Paint().apply {
            color = android.graphics.Color.rgb(217, 162, 74)
        }
        val cardPaint = Paint().apply {
            color = android.graphics.Color.rgb(255, 248, 239)
        }

        canvas.drawRoundRect(32f, 32f, 563f, 128f, 16f, 16f, emeraldPaint)
        canvas.drawCircle(522f, 42f, 32f, goldPaint)
        var y = 66f
        canvas.drawText("Laporan BizTrack", 52f, y, titlePaint)
        y += 24f
        canvas.drawText(settings.businessName.ifBlank { "Bisnis Saya" }, 52f, y, Paint(bodyPaint).apply { color = android.graphics.Color.WHITE })

        fun summaryCard(left: Float, top: Float, label: String, value: String) {
            canvas.drawRoundRect(left, top, left + 166f, top + 70f, 12f, 12f, cardPaint)
            canvas.drawText(label, left + 14f, top + 24f, mutedPaint)
            canvas.drawText(value, left + 14f, top + 49f, Paint(bodyPaint).apply { isFakeBoldText = true; textSize = 13f })
        }
        summaryCard(42f, 150f, "Pemasukan", formatMoney(income, settings.currency))
        summaryCard(214f, 150f, "Pengeluaran", formatMoney(expense, settings.currency))
        summaryCard(386f, 150f, "Profit", formatMoney(profit, settings.currency))

        y = 255f
        canvas.drawText("Transaksi", 42f, y, Paint(bodyPaint).apply { textSize = 16f; isFakeBoldText = true })
        y += 22f
        transactions.take(24).forEach { tx ->
            val sign = when (tx.type) {
                TransactionTypes.INCOME -> "+"
                TransactionTypes.EXPENSE -> "-"
                else -> ""
            }
            canvas.drawText(
                "${formatter.format(Instant.ofEpochMilli(tx.date))}  ${tx.type}  $sign${formatMoney(tx.amount, settings.currency)}",
                42f,
                y,
                bodyPaint,
            )
            y += 16f
            if (tx.note.isNotBlank()) {
                canvas.drawText(tx.note.take(76), 54f, y, mutedPaint)
                y += 15f
            }
        }
        document.finishPage(page)

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val fileName = "BizTrack_report_${System.currentTimeMillis()}.pdf"
        val file = File(directory, fileName)
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    suspend fun exportInvoicePdf(context: Context, invoice: InvoiceEntity): File {
        val settings = preferences.first()
        val contact = dao.getAllBusinessContacts().firstOrNull { it.id == invoice.contactId }
        val items = dao.getInvoiceItems(invoice.id)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withLocale(Locale.forLanguageTag("id-ID"))
            .withZone(ZoneId.systemDefault())

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
            color = android.graphics.Color.WHITE
        }
        val headingPaint = Paint().apply {
            textSize = 13f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(40, 46, 43)
        }
        val textPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.rgb(75, 82, 78)
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.rgb(221, 211, 190)
            strokeWidth = 1.3f
        }
        val emeraldPaint = Paint().apply {
            color = android.graphics.Color.rgb(8, 63, 58)
        }
        val goldPaint = Paint().apply {
            color = android.graphics.Color.rgb(215, 177, 106)
        }
        val cardPaint = Paint().apply {
            color = android.graphics.Color.rgb(255, 252, 246)
        }
        val tablePaint = Paint().apply {
            color = android.graphics.Color.rgb(246, 241, 232)
        }

        canvas.drawColor(android.graphics.Color.rgb(253, 250, 245))
        canvas.drawRoundRect(32f, 32f, 563f, 132f, 16f, 16f, emeraldPaint)
        canvas.drawCircle(530f, 36f, 34f, goldPaint)
        canvas.drawText(settings.businessName.ifBlank { "BizTrack" }, 48f, 62f, titlePaint)
        canvas.drawText("Invoice ${invoice.invoiceNumber}", 48f, 90f, Paint(titlePaint).apply { textSize = 14f })
        canvas.drawRoundRect(430f, 82f, 545f, 114f, 10f, 10f, Paint().apply {
            color = if (invoice.status == InvoiceStatus.PAID) android.graphics.Color.rgb(218, 239, 230) else android.graphics.Color.rgb(246, 232, 205)
        })
        canvas.drawText(invoice.status.uppercase(), 450f, 103f, Paint(headingPaint).apply {
            color = if (invoice.status == InvoiceStatus.PAID) android.graphics.Color.rgb(13, 98, 88) else android.graphics.Color.rgb(140, 106, 50)
        })

        canvas.drawRoundRect(42f, 154f, 547f, 230f, 12f, 12f, cardPaint)
        canvas.drawText("Customer", 58f, 184f, headingPaint)
        canvas.drawText(contact?.name ?: "-", 58f, 207f, textPaint)
        canvas.drawText("Tanggal", 318f, 184f, headingPaint)
        canvas.drawText(formatter.format(Instant.ofEpochMilli(invoice.issueDate)), 318f, 207f, textPaint)
        canvas.drawText("Jatuh tempo", 430f, 184f, headingPaint)
        canvas.drawText(formatter.format(Instant.ofEpochMilli(invoice.dueDate)), 430f, 207f, textPaint)

        var y = 270f
        canvas.drawRoundRect(42f, y - 22f, 547f, y + 10f, 9f, 9f, tablePaint)
        canvas.drawText("Item", 58f, y, headingPaint)
        canvas.drawText("Qty", 330f, y, headingPaint)
        canvas.drawText("Harga", 390f, y, headingPaint)
        canvas.drawText("Total", 485f, y, headingPaint)
        y += 30f
        items.forEach { item ->
            canvas.drawLine(48f, y - 14f, 547f, y - 14f, linePaint)
            canvas.drawText(item.description.take(36), 58f, y, textPaint)
            canvas.drawText(item.quantity.toString(), 330f, y, textPaint)
            canvas.drawText(formatMoney(item.unitPrice, settings.currency), 390f, y, textPaint)
            canvas.drawText(formatMoney(item.totalAmount, settings.currency), 485f, y, textPaint)
            y += 26f
        }
        y += 8f
        canvas.drawRoundRect(340f, y, 547f, y + 62f, 12f, 12f, emeraldPaint)
        canvas.drawText("Total", 360f, y + 24f, Paint(titlePaint).apply { textSize = 12f; color = android.graphics.Color.rgb(235, 229, 213) })
        canvas.drawText(formatMoney(invoice.totalAmount, settings.currency), 360f, y + 48f, Paint(titlePaint).apply { textSize = 17f })
        if (invoice.note.isNotBlank()) {
            y += 92f
            canvas.drawText("Catatan", 48f, y, headingPaint)
            y += 20f
            canvas.drawText(invoice.note.take(86), 48f, y, textPaint)
        }
        document.finishPage(page)

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(directory, "${invoice.invoiceNumber}.pdf".replace(Regex("[^A-Za-z0-9_.-]"), "_"))
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    suspend fun exportCustomerStatementPdf(context: Context, contactId: Long): File {
        val settings = preferences.first()
        val contact = dao.getAllBusinessContacts().firstOrNull { it.id == contactId }
            ?: error("Kontak tidak ditemukan.")
        val invoices = dao.getAllInvoices()
            .filter { it.contactId == contactId }
            .sortedByDescending { it.issueDate }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withLocale(Locale.forLanguageTag("id-ID"))
            .withZone(ZoneId.systemDefault())
        val paid = invoices.sumOf { it.paidAmount }
        val billed = invoices.sumOf { it.totalAmount }
        val balance = invoices.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0) }

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 21f
            isFakeBoldText = true
            color = android.graphics.Color.WHITE
        }
        val headingPaint = Paint().apply {
            textSize = 13f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(35, 44, 41)
        }
        val textPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.rgb(73, 82, 78)
        }
        val mutedPaint = Paint(textPaint).apply {
            color = android.graphics.Color.rgb(117, 124, 119)
        }
        val emeraldPaint = Paint().apply {
            color = android.graphics.Color.rgb(8, 63, 58)
        }
        val softPaint = Paint().apply {
            color = android.graphics.Color.rgb(255, 252, 246)
        }
        val goldPaint = Paint().apply {
            color = android.graphics.Color.rgb(215, 177, 106)
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.rgb(226, 215, 193)
            strokeWidth = 1.2f
        }

        canvas.drawColor(android.graphics.Color.rgb(253, 250, 245))
        canvas.drawRoundRect(32f, 32f, 563f, 128f, 16f, 16f, emeraldPaint)
        canvas.drawCircle(532f, 36f, 32f, goldPaint)
        canvas.drawText("Customer Statement", 48f, 64f, titlePaint)
        canvas.drawText(settings.businessName.ifBlank { "BizTrack" }, 48f, 91f, Paint(titlePaint).apply { textSize = 13f })
        canvas.drawText(contact.name, 48f, 116f, Paint(titlePaint).apply { textSize = 13f })

        fun summaryCard(left: Float, top: Float, label: String, value: String) {
            canvas.drawRoundRect(left, top, left + 166f, top + 70f, 12f, 12f, softPaint)
            canvas.drawText(label, left + 14f, top + 24f, mutedPaint)
            canvas.drawText(value, left + 14f, top + 49f, Paint(headingPaint).apply { textSize = 12f })
        }

        summaryCard(42f, 152f, "Ditagihkan", formatMoney(billed, settings.currency))
        summaryCard(214f, 152f, "Terbayar", formatMoney(paid, settings.currency))
        summaryCard(386f, 152f, "Sisa", formatMoney(balance, settings.currency))

        var y = 260f
        canvas.drawText("Riwayat Invoice", 42f, y, Paint(headingPaint).apply { textSize = 16f })
        y += 26f
        if (invoices.isEmpty()) {
            canvas.drawText("Belum ada invoice untuk kontak ini.", 42f, y, mutedPaint)
        } else {
            invoices.take(24).forEach { invoice ->
                canvas.drawLine(42f, y - 14f, 547f, y - 14f, linePaint)
                canvas.drawText(invoice.invoiceNumber, 42f, y, headingPaint)
                canvas.drawText(formatter.format(Instant.ofEpochMilli(invoice.issueDate)), 168f, y, textPaint)
                canvas.drawText(invoice.status.uppercase(), 274f, y, textPaint)
                canvas.drawText(formatMoney(invoice.totalAmount, settings.currency), 388f, y, textPaint)
                y += 18f
                val remaining = (invoice.totalAmount - invoice.paidAmount).coerceAtLeast(0)
                canvas.drawText("Sisa ${formatMoney(remaining, settings.currency)}", 54f, y, mutedPaint)
                y += 24f
            }
        }
        document.finishPage(page)

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val fileName = "statement_${contact.name}_${System.currentTimeMillis()}.pdf"
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
        val file = File(directory, fileName)
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    suspend fun createBackup(context: Context): File {
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            preferences = preferences.first(),
            transactions = dao.getAllTransactions(),
            categories = dao.getAllCategories(),
            cashAccounts = dao.getAllCashAccounts(),
            paymentMethods = dao.getAllPaymentMethods(),
            debtReceivables = dao.getAllDebtReceivables(),
            reminders = dao.getAllReminders(),
            recurringTemplates = dao.getAllRecurringTemplates(),
            inventoryItems = dao.getAllInventoryItems(),
            inventoryMovements = dao.getAllInventoryMovements(),
            businessContacts = dao.getAllBusinessContacts(),
            invoices = dao.getAllInvoices(),
            invoiceItems = dao.getAllInvoiceItems(),
        )
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(directory, "biztrack_backup_${payload.exportedAt}.json")
        file.writeText(gson.toJson(payload))
        preferencesDataSource.markBackupCreated(payload.exportedAt)
        return file
    }

    suspend fun previewBackup(context: Context, uri: Uri): BackupPreview {
        val payload = readBackupPayload(context, uri)
        return BackupPreview(
            exportedAt = payload.exportedAt,
            businessName = payload.preferences.businessName,
            transactionCount = payload.transactions.size,
            categoryCount = payload.categories.size,
            cashAccountCount = payload.cashAccounts.size,
            debtReceivableCount = payload.debtReceivables.size,
            reminderCount = payload.reminders.size,
            recurringTemplateCount = payload.recurringTemplates.size,
            inventoryItemCount = payload.inventoryItems.size,
            businessContactCount = payload.businessContacts.size,
            invoiceCount = payload.invoices.size,
        )
    }

    suspend fun restoreBackup(context: Context, uri: Uri) {
        val payload = readBackupPayload(context, uri)
        require(payload.schemaVersion == 1) { "Versi backup tidak didukung." }

        database.withTransaction {
            dao.clearTransactions()
            dao.clearDebtReceivables()
            dao.clearReminders()
            dao.clearRecurringTemplates()
            dao.clearInventoryMovements()
            dao.clearInventoryItems()
            dao.clearInvoiceItems()
            dao.clearInvoices()
            dao.clearBusinessContacts()
            dao.clearCategories()
            dao.clearCashAccounts()
            dao.clearPaymentMethods()
            dao.upsertBusinessContacts(payload.businessContacts)
            dao.upsertCategories(payload.categories)
            dao.upsertCashAccounts(payload.cashAccounts)
            dao.upsertPaymentMethods(payload.paymentMethods)
            dao.upsertTransactions(payload.transactions)
            dao.upsertDebtReceivables(payload.debtReceivables)
            dao.upsertReminders(payload.reminders)
            dao.upsertRecurringTemplates(payload.recurringTemplates)
            dao.upsertInventoryItems(payload.inventoryItems)
            dao.upsertInventoryMovements(payload.inventoryMovements)
            dao.upsertInvoices(payload.invoices)
            dao.upsertInvoiceItems(payload.invoiceItems)
        }
        preferencesDataSource.replaceFromBackup(payload.preferences.copy(onboardingCompleted = true))
    }

    private fun readBackupPayload(context: Context, uri: Uri): BackupPayload {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("File backup tidak valid atau rusak.")
        return runCatching { gson.fromJson(json, BackupPayload::class.java) }.getOrNull()
            ?: error("File backup tidak valid atau rusak.")
    }

    suspend fun deleteAllData() {
        database.withTransaction {
            dao.clearTransactions()
            dao.clearDebtReceivables()
            dao.clearReminders()
            dao.clearRecurringTemplates()
            dao.clearInventoryMovements()
            dao.clearInventoryItems()
            dao.clearInvoiceItems()
            dao.clearInvoices()
            dao.clearBusinessContacts()
            dao.clearCategories()
            dao.clearCashAccounts()
            dao.clearPaymentMethods()
        }
        preferencesDataSource.reset()
    }

    fun shareUriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private suspend fun applyEffect(transaction: TransactionEntity) {
        val delta = BalanceEffect.apply(transaction.type, transaction.amount)
        dao.adjustCashAccountBalance(
            transaction.cashAccountId,
            delta.sourceDelta,
            System.currentTimeMillis(),
        )
        if (transaction.type == TransactionTypes.TRANSFER) {
            transaction.targetCashAccountId?.let { targetId ->
                dao.adjustCashAccountBalance(targetId, delta.targetDelta, System.currentTimeMillis())
            }
        }
    }

    private suspend fun validateTransactionEffect(transaction: TransactionEntity) {
        when (transaction.type) {
            TransactionTypes.EXPENSE, TransactionTypes.TRANSFER -> {
                val source = dao.getAllCashAccounts().firstOrNull { it.id == transaction.cashAccountId }
                    ?: error("Akun kas tidak ditemukan.")
                BusinessRules.requireEnoughBalance(source.name, source.currentBalance, transaction.amount)
            }
        }
    }

    private suspend fun validateInventoryMovement(itemId: Long, type: String, quantity: Long) {
        if (type != "out") return
        val item = dao.getAllInventoryItems().firstOrNull { it.id == itemId }
            ?: error("Produk tidak ditemukan.")
        BusinessRules.requireEnoughStock(item.name, item.stock, quantity)
    }

    private suspend fun revertEffect(transaction: TransactionEntity) {
        val delta = BalanceEffect.revert(transaction.type, transaction.amount)
        dao.adjustCashAccountBalance(
            transaction.cashAccountId,
            delta.sourceDelta,
            System.currentTimeMillis(),
        )
        if (transaction.type == TransactionTypes.TRANSFER) {
            transaction.targetCashAccountId?.let { targetId ->
                dao.adjustCashAccountBalance(targetId, delta.targetDelta, System.currentTimeMillis())
            }
        }
    }

    private fun defaultIncomeCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(name = "Penjualan", type = CategoryTypes.INCOME, color = "#12B886", isDefault = true),
        CategoryEntity(name = "Jasa", type = CategoryTypes.INCOME, color = "#0CA678", isDefault = true),
        CategoryEntity(name = "DP", type = CategoryTypes.INCOME, color = "#2F9E44", isDefault = true),
        CategoryEntity(name = "Pelunasan", type = CategoryTypes.INCOME, color = "#37B24D", isDefault = true),
        CategoryEntity(name = "Lain-lain", type = CategoryTypes.INCOME, color = "#51CF66", isDefault = true),
    )

    private fun defaultExpenseCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(name = "Operasional", type = CategoryTypes.EXPENSE, color = "#F76707", isDefault = true),
        CategoryEntity(name = "Bahan baku", type = CategoryTypes.EXPENSE, color = "#E8590C", isDefault = true),
        CategoryEntity(name = "Gaji", type = CategoryTypes.EXPENSE, color = "#FA5252", isDefault = true),
        CategoryEntity(name = "Transportasi", type = CategoryTypes.EXPENSE, color = "#FD7E14", isDefault = true),
        CategoryEntity(name = "Iklan", type = CategoryTypes.EXPENSE, color = "#FF922B", isDefault = true),
        CategoryEntity(name = "Sewa", type = CategoryTypes.EXPENSE, color = "#F03E3E", isDefault = true),
        CategoryEntity(name = "Lain-lain", type = CategoryTypes.EXPENSE, color = "#FF6B6B", isDefault = true),
    )

    private fun defaultPaymentMethods(): List<PaymentMethodEntity> = listOf(
        PaymentMethodEntity(name = "Cash", isDefault = true),
        PaymentMethodEntity(name = "Transfer Bank", isDefault = true),
        PaymentMethodEntity(name = "QRIS", isDefault = true),
        PaymentMethodEntity(name = "E-Wallet", isDefault = true),
        PaymentMethodEntity(name = "Debit", isDefault = true),
        PaymentMethodEntity(name = "Marketplace", isDefault = true),
    )

    private fun String.csvEscape(): String {
        val escaped = replace("\"", "\"\"")
        return if (contains(",") || contains("\"") || contains("\n")) "\"$escaped\"" else escaped
    }
}
