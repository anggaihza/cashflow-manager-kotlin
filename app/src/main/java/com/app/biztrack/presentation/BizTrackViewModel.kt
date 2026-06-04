package com.app.biztrack.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.biztrack.data.local.entity.BusinessContactEntity
import com.app.biztrack.data.local.entity.CashAccountEntity
import com.app.biztrack.data.local.entity.CategoryEntity
import com.app.biztrack.data.local.entity.DebtReceivableEntity
import com.app.biztrack.data.local.entity.InvoiceEntity
import com.app.biztrack.data.local.entity.InvoiceItemEntity
import com.app.biztrack.data.local.entity.InventoryItemEntity
import com.app.biztrack.data.local.entity.PaymentMethodEntity
import com.app.biztrack.data.local.entity.RecurringTemplateEntity
import com.app.biztrack.data.local.entity.ReminderEntity
import com.app.biztrack.data.local.entity.TransactionEntity
import com.app.biztrack.data.preferences.AppPreferences
import com.app.biztrack.data.repository.BackupPreview
import com.app.biztrack.data.repository.FinanceRepository
import com.app.biztrack.domain.model.DebtTypes
import com.app.biztrack.domain.model.InvoiceStatus
import com.app.biztrack.domain.model.TransactionTypes
import com.app.biztrack.utils.currentMonth
import com.app.biztrack.utils.monthEndMillis
import com.app.biztrack.utils.monthStartMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.time.YearMonth

data class TransactionFilters(
    val search: String = "",
    val type: String = "all",
)

data class EnrichedTransaction(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val cashAccount: CashAccountEntity?,
    val targetCashAccount: CashAccountEntity? = null,
    val paymentMethod: PaymentMethodEntity?,
)

data class CategoryTotal(
    val category: CategoryEntity?,
    val amount: Long,
)

data class EnrichedInvoice(
    val invoice: InvoiceEntity,
    val contact: BusinessContactEntity?,
    val items: List<InvoiceItemEntity> = emptyList(),
)

data class FinanceData(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val cashAccounts: List<CashAccountEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val debtReceivables: List<DebtReceivableEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val recurringTemplates: List<RecurringTemplateEntity> = emptyList(),
    val inventoryItems: List<InventoryItemEntity> = emptyList(),
    val businessContacts: List<BusinessContactEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val invoiceItems: List<InvoiceItemEntity> = emptyList(),
)

data class ReportSummary(
    val month: YearMonth = currentMonth(),
    val income: Long = 0,
    val expense: Long = 0,
    val profit: Long = 0,
    val transactionCount: Int = 0,
    val topIncomeCategory: CategoryTotal? = null,
    val topExpenseCategory: CategoryTotal? = null,
    val transactions: List<EnrichedTransaction> = emptyList(),
)

data class CashflowChartPoint(
    val month: YearMonth,
    val income: Long,
    val expense: Long,
    val profit: Long,
)

data class GlobalSearchResult(
    val title: String,
    val subtitle: String,
    val type: String,
    val amount: Long? = null,
)

private data class UiInputs(
    val preferences: AppPreferences,
    val data: FinanceData,
    val filters: TransactionFilters,
    val month: YearMonth,
    val globalSearch: String,
)

private data class CommercialData(
    val businessContacts: List<BusinessContactEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val invoiceItems: List<InvoiceItemEntity> = emptyList(),
)

private data class FinanceExtraData(
    val debtReceivables: List<DebtReceivableEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val recurringTemplates: List<RecurringTemplateEntity> = emptyList(),
    val inventoryItems: List<InventoryItemEntity> = emptyList(),
    val commercialData: CommercialData = CommercialData(),
)

data class BizTrackUiState(
    val preferences: AppPreferences = AppPreferences(),
    val transactions: List<TransactionEntity> = emptyList(),
    val enrichedTransactions: List<EnrichedTransaction> = emptyList(),
    val filteredTransactions: List<EnrichedTransaction> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val cashAccounts: List<CashAccountEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val debtReceivables: List<DebtReceivableEntity> = emptyList(),
    val activeReminders: List<ReminderEntity> = emptyList(),
    val recurringTemplates: List<RecurringTemplateEntity> = emptyList(),
    val inventoryItems: List<InventoryItemEntity> = emptyList(),
    val businessContacts: List<BusinessContactEntity> = emptyList(),
    val enrichedInvoices: List<EnrichedInvoice> = emptyList(),
    val unpaidInvoiceCount: Int = 0,
    val overdueInvoiceCount: Int = 0,
    val lowStockCount: Int = 0,
    val cashflowChart: List<CashflowChartPoint> = emptyList(),
    val globalSearchQuery: String = "",
    val globalSearchResults: List<GlobalSearchResult> = emptyList(),
    val filters: TransactionFilters = TransactionFilters(),
    val selectedReportMonth: YearMonth = currentMonth(),
    val monthlyIncome: Long = 0,
    val monthlyExpense: Long = 0,
    val monthlyProfit: Long = 0,
    val totalCashBalance: Long = 0,
    val todayTransactionCount: Int = 0,
    val topExpenseCategories: List<CategoryTotal> = emptyList(),
    val receivableTotal: Long = 0,
    val debtTotal: Long = 0,
    val upcomingReminderCount: Int = 0,
    val backupPreview: BackupPreview? = null,
    val recentTransactions: List<EnrichedTransaction> = emptyList(),
    val report: ReportSummary = ReportSummary(),
    val errorMessage: String? = null,
)

class BizTrackViewModel(
    private val repository: FinanceRepository,
) : ViewModel() {
    private val filters = MutableStateFlow(TransactionFilters())
    private val selectedReportMonth = MutableStateFlow(currentMonth())
    private val globalSearch = MutableStateFlow("")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val backupPreview = MutableStateFlow<BackupPreview?>(null)

    private val financeCoreData = combine(
        repository.transactions,
        repository.categories,
        repository.cashAccounts,
        repository.paymentMethods,
    ) { transactions, categories, accounts, methods ->
        FinanceData(
            transactions = transactions,
            categories = categories,
            cashAccounts = accounts,
            paymentMethods = methods,
        )
    }

    private val commercialData = combine(
        repository.businessContacts,
        repository.invoices,
        repository.invoiceItems,
    ) { contacts, invoices, invoiceItems ->
        CommercialData(contacts, invoices, invoiceItems)
    }

    private val financeExtraData = combine(
        repository.debtReceivables,
        repository.reminders,
        repository.recurringTemplates,
        repository.inventoryItems,
        commercialData,
    ) { debtReceivables, reminders, recurringTemplates, inventoryItems, commercial ->
        FinanceExtraData(
            debtReceivables = debtReceivables,
            reminders = reminders,
            recurringTemplates = recurringTemplates,
            inventoryItems = inventoryItems,
            commercialData = commercial,
        )
    }

    private val financeData = combine(
        financeCoreData,
        financeExtraData,
    ) { data, extra ->
        data.copy(
            debtReceivables = extra.debtReceivables,
            reminders = extra.reminders,
            recurringTemplates = extra.recurringTemplates,
            inventoryItems = extra.inventoryItems,
            businessContacts = extra.commercialData.businessContacts,
            invoices = extra.commercialData.invoices,
            invoiceItems = extra.commercialData.invoiceItems,
        )
    }

    private val uiInputs = combine(
        repository.preferences,
        financeData,
        filters,
        selectedReportMonth,
        globalSearch,
    ) { preferences, data, activeFilters, month, search ->
        UiInputs(preferences, data, activeFilters, month, search)
    }

    val uiState = combine(
        uiInputs,
        errorMessage,
        backupPreview,
    ) { inputs, error, activeBackupPreview ->
        val preferences = inputs.preferences
        val data = inputs.data
        val activeFilters = inputs.filters
        val month = inputs.month
        val globalSearchQuery = inputs.globalSearch
        val categoryMap = data.categories.associateBy { it.id }
        val accountMap = data.cashAccounts.associateBy { it.id }
        val methodMap = data.paymentMethods.associateBy { it.id }
        val contactMap = data.businessContacts.associateBy { it.id }
        val enriched = data.transactions.map {
            EnrichedTransaction(
                transaction = it,
                category = categoryMap[it.categoryId],
                cashAccount = accountMap[it.cashAccountId],
                targetCashAccount = accountMap[it.targetCashAccountId],
                paymentMethod = methodMap[it.paymentMethodId],
            )
        }
        val invoiceItemsByInvoice = data.invoiceItems.groupBy { it.invoiceId }
        val enrichedInvoices = data.invoices.map {
            EnrichedInvoice(
                invoice = it,
                contact = contactMap[it.contactId],
                items = invoiceItemsByInvoice[it.id].orEmpty(),
            )
        }

        val nowMonth = currentMonth()
        val monthStart = monthStartMillis(nowMonth)
        val monthEnd = monthEndMillis(nowMonth)
        val monthTransactions = data.transactions.filter { it.date in monthStart..monthEnd }
        val income = monthTransactions.sumOf { if (it.type == TransactionTypes.INCOME) it.amount else 0 }
        val expense = monthTransactions.sumOf { if (it.type == TransactionTypes.EXPENSE) it.amount else 0 }
        val todayStart = com.app.biztrack.utils.todayMillis()
        val todayEnd = todayStart + 86_399_999L

        val filtered = enriched.filter { item ->
            val query = activeFilters.search.trim().lowercase()
            val matchesType = activeFilters.type == "all" || item.transaction.type == activeFilters.type
            val searchableText = listOfNotNull(
                item.transaction.note,
                item.category?.name,
                item.cashAccount?.name,
                item.paymentMethod?.name,
            ).joinToString(" ").lowercase()
            matchesType && (query.isBlank() || searchableText.contains(query))
        }

        val topExpense = monthTransactions
            .filter { it.type == TransactionTypes.EXPENSE }
            .groupBy { it.categoryId }
            .map { (categoryId, items) -> CategoryTotal(categoryMap[categoryId], items.sumOf { it.amount }) }
            .sortedByDescending { it.amount }
            .take(4)

        val report = buildReport(month, data, enriched)
        val activeReminders = data.reminders.filter { it.isActive }.sortedBy { it.reminderDate }
        val receivableTotal = data.debtReceivables
            .filter { it.type == DebtTypes.RECEIVABLE }
            .sumOf { it.remainingAmount }
        val debtTotal = data.debtReceivables
            .filter { it.type == DebtTypes.DEBT }
            .sumOf { it.remainingAmount }
        val unpaidInvoiceCount = data.invoices.count {
            it.status == InvoiceStatus.UNPAID || it.status == InvoiceStatus.SENT || it.status == InvoiceStatus.PARTIAL
        }
        val overdueInvoiceCount = data.invoices.count {
            it.status != InvoiceStatus.PAID && it.status != InvoiceStatus.DRAFT && it.dueDate < todayStart
        }
        val lowStockCount = data.inventoryItems.count { it.stock <= it.minStock }
        val cashflowChart = buildCashflowChart(data.transactions, nowMonth)
        val globalSearchResults = buildGlobalSearchResults(
            query = globalSearchQuery,
            data = data,
            enriched = enriched,
            enrichedInvoices = enrichedInvoices,
        )

        BizTrackUiState(
            preferences = preferences,
            transactions = data.transactions,
            enrichedTransactions = enriched,
            filteredTransactions = filtered,
            categories = data.categories,
            cashAccounts = data.cashAccounts,
            paymentMethods = data.paymentMethods,
            debtReceivables = data.debtReceivables,
            activeReminders = activeReminders,
            recurringTemplates = data.recurringTemplates,
            inventoryItems = data.inventoryItems,
            businessContacts = data.businessContacts,
            enrichedInvoices = enrichedInvoices,
            unpaidInvoiceCount = unpaidInvoiceCount,
            overdueInvoiceCount = overdueInvoiceCount,
            lowStockCount = lowStockCount,
            cashflowChart = cashflowChart,
            globalSearchQuery = globalSearchQuery,
            globalSearchResults = globalSearchResults,
            filters = activeFilters,
            selectedReportMonth = month,
            monthlyIncome = income,
            monthlyExpense = expense,
            monthlyProfit = income - expense,
            totalCashBalance = data.cashAccounts.sumOf { it.currentBalance },
            todayTransactionCount = data.transactions.count { it.date in todayStart..todayEnd },
            topExpenseCategories = topExpense,
            receivableTotal = receivableTotal,
            debtTotal = debtTotal,
            upcomingReminderCount = activeReminders.count { it.reminderDate >= todayStart },
            backupPreview = activeBackupPreview,
            recentTransactions = enriched.take(5),
            report = report,
            errorMessage = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BizTrackUiState())

    fun clearError() {
        errorMessage.value = null
    }

    fun updateSearch(query: String) {
        filters.update { it.copy(search = query) }
    }

    fun updateTypeFilter(type: String) {
        filters.update { it.copy(type = type) }
    }

    fun updateGlobalSearch(query: String) {
        globalSearch.value = query
    }

    fun selectPreviousReportMonth() {
        selectedReportMonth.update { it.minusMonths(1) }
    }

    fun selectNextReportMonth() {
        selectedReportMonth.update { it.plusMonths(1) }
    }

    fun completeOnboarding(
        businessName: String,
        currency: String,
        accountName: String,
        initialBalance: Long,
        useDarkMode: Boolean,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.completeOnboarding(
                    businessName = businessName,
                    currency = currency,
                    accountName = accountName,
                    initialBalance = initialBalance,
                    useDarkMode = useDarkMode,
                )
            }.onFailure { errorMessage.value = it.message }
        }
    }

    fun addTransaction(
        type: String,
        amount: Long,
        date: Long,
        categoryId: Long?,
        cashAccountId: Long,
        paymentMethodId: Long?,
        note: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addTransaction(
                    TransactionEntity(
                        type = type,
                        amount = amount,
                        date = date,
                        categoryId = categoryId,
                        cashAccountId = cashAccountId,
                        paymentMethodId = paymentMethodId,
                        note = note.trim(),
                    ),
                )
            }.onSuccess { onSaved() }
                .onFailure { errorMessage.value = it.message ?: "Data gagal disimpan. Silakan coba lagi." }
        }
    }

    fun updateTransaction(
        existing: TransactionEntity,
        type: String,
        amount: Long,
        date: Long,
        categoryId: Long?,
        cashAccountId: Long,
        paymentMethodId: Long?,
        note: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updateTransaction(
                    existing.copy(
                        type = type,
                        amount = amount,
                        date = date,
                        categoryId = categoryId,
                        cashAccountId = cashAccountId,
                        paymentMethodId = paymentMethodId,
                        note = note.trim(),
                    ),
                )
            }.onSuccess { onSaved() }
                .onFailure { errorMessage.value = it.message ?: "Data gagal disimpan. Silakan coba lagi." }
        }
    }

    fun deleteTransaction(id: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.deleteTransaction(id) }
                .onSuccess { onDeleted() }
                .onFailure { errorMessage.value = it.message ?: "Data gagal dihapus." }
        }
    }

    fun createCategory(name: String, type: String, color: String) {
        viewModelScope.launch {
            runCatching { repository.createCategory(name, type, color) }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun deactivateCategory(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deactivateCategory(id) }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun createCashAccount(name: String, initialBalance: Long, note: String) {
        viewModelScope.launch {
            runCatching { repository.createCashAccount(name, initialBalance, note) }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun updateCashAccount(account: CashAccountEntity) {
        viewModelScope.launch {
            runCatching { repository.updateCashAccount(account) }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun deactivateCashAccount(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deactivateCashAccount(id) }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun createBusinessContact(
        name: String,
        type: String,
        phone: String,
        email: String,
        address: String,
        note: String,
    ) {
        viewModelScope.launch {
            runCatching { repository.createBusinessContact(name, type, phone, email, address, note) }
                .onFailure { errorMessage.value = it.message ?: "Kontak gagal dibuat." }
        }
    }

    fun deactivateBusinessContact(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deactivateBusinessContact(id) }
                .onFailure { errorMessage.value = it.message ?: "Kontak gagal diarsip." }
        }
    }

    fun createInvoice(
        invoiceNumber: String,
        contactId: Long?,
        issueDate: Long,
        dueDate: Long,
        status: String,
        lines: List<FinanceRepository.InvoiceLineInput>,
        note: String,
        createDueReminder: Boolean,
    ) {
        viewModelScope.launch {
            runCatching { repository.createInvoice(invoiceNumber, contactId, issueDate, dueDate, status, lines, note, createDueReminder) }
                .onFailure { errorMessage.value = it.message ?: "Invoice gagal dibuat." }
        }
    }

    fun recordInvoicePayment(invoice: InvoiceEntity, amount: Long, cashAccountId: Long, paymentMethodId: Long?) {
        viewModelScope.launch {
            runCatching { repository.recordInvoicePayment(invoice, amount, cashAccountId, paymentMethodId) }
                .onFailure { errorMessage.value = it.message ?: "Pembayaran invoice gagal dicatat." }
        }
    }

    fun markInvoicePaid(invoice: InvoiceEntity, cashAccountId: Long, paymentMethodId: Long?) {
        viewModelScope.launch {
            runCatching { repository.markInvoicePaid(invoice, cashAccountId, paymentMethodId) }
                .onFailure { errorMessage.value = it.message ?: "Pembayaran invoice gagal dicatat." }
        }
    }

    fun markInvoiceSent(invoice: InvoiceEntity) {
        viewModelScope.launch {
            runCatching { repository.markInvoiceSent(invoice) }
                .onFailure { errorMessage.value = it.message ?: "Status invoice gagal diperbarui." }
        }
    }

    fun updateBusinessInfo(businessName: String, currency: String) {
        viewModelScope.launch {
            runCatching { repository.updateBusinessInfo(businessName, currency) }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            runCatching { repository.updateThemeMode(themeMode) }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun updateMonthlyTargets(incomeTarget: Long, expenseLimit: Long) {
        viewModelScope.launch {
            runCatching { repository.updateMonthlyTargets(incomeTarget, expenseLimit) }
                .onFailure { errorMessage.value = it.message }
        }
    }

    fun updatePin(pin: String) {
        viewModelScope.launch {
            runCatching {
                require(pin.isBlank() || pin.length >= 4) { "PIN minimal 4 digit." }
                repository.updatePin(if (pin.isBlank()) "" else hashPin(pin))
            }.onFailure { errorMessage.value = it.message ?: "PIN gagal diperbarui." }
        }
    }

    fun verifyPin(pin: String, expectedHash: String): Boolean =
        expectedHash.isNotBlank() && hashPin(pin) == expectedHash

    private fun hashPin(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun transferCash(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        date: Long,
        note: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.transferCash(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amount = amount,
                    date = date,
                    note = note,
                )
            }.onSuccess { onSaved() }
                .onFailure { errorMessage.value = it.message ?: "Transfer gagal disimpan." }
        }
    }

    fun createDebtReceivable(
        type: String,
        partyName: String,
        totalAmount: Long,
        dueDate: Long?,
        note: String,
    ) {
        viewModelScope.launch {
            runCatching { repository.createDebtReceivable(type, partyName, totalAmount, dueDate, note) }
                .onFailure { errorMessage.value = it.message ?: "Data piutang/hutang gagal disimpan." }
        }
    }

    fun recordDebtPayment(item: DebtReceivableEntity, amount: Long, cashAccountId: Long) {
        viewModelScope.launch {
            runCatching { repository.recordDebtPayment(item, amount, cashAccountId) }
                .onFailure { errorMessage.value = it.message ?: "Pembayaran gagal dicatat." }
        }
    }

    fun createRecurringTemplate(
        title: String,
        type: String,
        amount: Long,
        categoryId: Long?,
        cashAccountId: Long,
        paymentMethodId: Long?,
        note: String,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.createRecurringTemplate(title, type, amount, categoryId, cashAccountId, paymentMethodId, note)
            }.onFailure { errorMessage.value = it.message ?: "Template gagal dibuat." }
        }
    }

    fun createTransactionFromTemplate(template: RecurringTemplateEntity, date: Long) {
        viewModelScope.launch {
            runCatching { repository.createTransactionFromTemplate(template, date) }
                .onFailure { errorMessage.value = it.message ?: "Transaksi dari template gagal dibuat." }
        }
    }

    fun deactivateRecurringTemplate(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deactivateRecurringTemplate(id) }
                .onFailure { errorMessage.value = it.message ?: "Template gagal dinonaktifkan." }
        }
    }

    fun createInventoryItem(name: String, sku: String, stock: Long, minStock: Long, costPrice: Long, salePrice: Long) {
        viewModelScope.launch {
            runCatching { repository.createInventoryItem(name, sku, stock, minStock, costPrice, salePrice) }
                .onFailure { errorMessage.value = it.message ?: "Produk gagal dibuat." }
        }
    }

    fun updateInventoryMinStock(item: InventoryItemEntity, minStock: Long) {
        viewModelScope.launch {
            runCatching { repository.updateInventoryMinStock(item, minStock) }
                .onFailure { errorMessage.value = it.message ?: "Minimum stok gagal diperbarui." }
        }
    }

    fun recordInventoryMovement(itemId: Long, type: String, quantity: Long, note: String) {
        viewModelScope.launch {
            runCatching { repository.recordInventoryMovement(itemId, type, quantity, note) }
                .onFailure { errorMessage.value = it.message ?: "Stok gagal diperbarui." }
        }
    }

    fun recordInventoryMovementWithTransaction(
        itemId: Long,
        type: String,
        quantity: Long,
        note: String,
        cashAccountId: Long,
        paymentMethodId: Long?,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.recordInventoryMovementWithTransaction(
                    itemId = itemId,
                    type = type,
                    quantity = quantity,
                    note = note,
                    cashAccountId = cashAccountId,
                    paymentMethodId = paymentMethodId,
                )
            }.onFailure { errorMessage.value = it.message ?: "Stok dan transaksi gagal diperbarui." }
        }
    }

    fun deactivateInventoryItem(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deactivateInventoryItem(id) }
                .onFailure { errorMessage.value = it.message ?: "Produk gagal dinonaktifkan." }
        }
    }

    fun createReminder(title: String, description: String, reminderDate: Long) {
        viewModelScope.launch {
            runCatching { repository.createReminder(title, description, reminderDate) }
                .onFailure { errorMessage.value = it.message ?: "Reminder gagal dibuat." }
        }
    }

    fun deactivateReminder(item: ReminderEntity) {
        viewModelScope.launch {
            runCatching { repository.deactivateReminder(item) }
                .onFailure { errorMessage.value = it.message ?: "Reminder gagal dinonaktifkan." }
        }
    }

    fun seedDemoData(onFinished: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.seedDemoData() }
                .onSuccess { onFinished(it) }
                .onFailure { errorMessage.value = it.message ?: "Data contoh gagal dibuat." }
        }
    }

    suspend fun exportCurrentReportCsv(context: Context): File {
        val month = selectedReportMonth.value
        return repository.exportCsv(context, monthStartMillis(month), monthEndMillis(month))
    }

    suspend fun exportCurrentReportPdf(context: Context): File {
        val month = selectedReportMonth.value
        return repository.exportPdf(context, monthStartMillis(month), monthEndMillis(month))
    }

    suspend fun exportInvoicePdf(context: Context, invoice: InvoiceEntity): File =
        repository.exportInvoicePdf(context, invoice)

    suspend fun exportCustomerStatementPdf(context: Context, contactId: Long): File =
        repository.exportCustomerStatementPdf(context, contactId)

    suspend fun createBackup(context: Context): File = repository.createBackup(context)

    fun previewBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching { repository.previewBackup(context, uri) }
                .onSuccess { backupPreview.value = it }
                .onFailure { errorMessage.value = it.message ?: "Preview backup gagal." }
        }
    }

    fun clearBackupPreview() {
        backupPreview.value = null
    }

    suspend fun restoreBackup(context: Context, uri: Uri) = repository.restoreBackup(context, uri)

    suspend fun deleteAllData() = repository.deleteAllData()

    fun shareUriForFile(context: Context, file: File): Uri = repository.shareUriForFile(context, file)

    private fun buildReport(
        month: YearMonth,
        data: FinanceData,
        enriched: List<EnrichedTransaction>,
    ): ReportSummary {
        val start = monthStartMillis(month)
        val end = monthEndMillis(month)
        val reportTransactions = data.transactions.filter { it.date in start..end }
        val reportEnriched = enriched.filter { it.transaction.date in start..end }
        val income = reportTransactions.sumOf { if (it.type == TransactionTypes.INCOME) it.amount else 0 }
        val expense = reportTransactions.sumOf { if (it.type == TransactionTypes.EXPENSE) it.amount else 0 }
        val categoryMap = data.categories.associateBy { it.id }
        val topIncome = reportTransactions
            .filter { it.type == TransactionTypes.INCOME }
            .groupBy { it.categoryId }
            .map { (categoryId, items) -> CategoryTotal(categoryMap[categoryId], items.sumOf { it.amount }) }
            .maxByOrNull { it.amount }
        val topExpense = reportTransactions
            .filter { it.type == TransactionTypes.EXPENSE }
            .groupBy { it.categoryId }
            .map { (categoryId, items) -> CategoryTotal(categoryMap[categoryId], items.sumOf { it.amount }) }
            .maxByOrNull { it.amount }
        return ReportSummary(
            month = month,
            income = income,
            expense = expense,
            profit = income - expense,
            transactionCount = reportTransactions.size,
            topIncomeCategory = topIncome,
            topExpenseCategory = topExpense,
            transactions = reportEnriched,
        )
    }

    private fun buildCashflowChart(
        transactions: List<TransactionEntity>,
        current: YearMonth,
    ): List<CashflowChartPoint> =
        (5 downTo 0).map { offset ->
            val month = current.minusMonths(offset.toLong())
            val start = monthStartMillis(month)
            val end = monthEndMillis(month)
            val monthTransactions = transactions.filter { it.date in start..end }
            val income = monthTransactions.sumOf { if (it.type == TransactionTypes.INCOME) it.amount else 0 }
            val expense = monthTransactions.sumOf { if (it.type == TransactionTypes.EXPENSE) it.amount else 0 }
            CashflowChartPoint(
                month = month,
                income = income,
                expense = expense,
                profit = income - expense,
            )
        }

    private fun buildGlobalSearchResults(
        query: String,
        data: FinanceData,
        enriched: List<EnrichedTransaction>,
        enrichedInvoices: List<EnrichedInvoice>,
    ): List<GlobalSearchResult> {
        val normalized = query.trim().lowercase()
        if (normalized.length < 2) return emptyList()

        fun String.matchesQuery(): Boolean = lowercase().contains(normalized)

        val transactions = enriched
            .filter {
                listOfNotNull(
                    it.transaction.note,
                    it.category?.name,
                    it.cashAccount?.name,
                    it.paymentMethod?.name,
                ).joinToString(" ").matchesQuery()
            }
            .take(3)
            .map {
                GlobalSearchResult(
                    title = it.transaction.note.ifBlank { it.category?.name ?: "Transaksi" },
                    subtitle = it.cashAccount?.name ?: "Transaksi",
                    type = if (it.transaction.type == TransactionTypes.INCOME) "Pemasukan" else if (it.transaction.type == TransactionTypes.EXPENSE) "Pengeluaran" else "Transfer",
                    amount = it.transaction.amount,
                )
            }

        val invoices = enrichedInvoices
            .filter {
                listOfNotNull(
                    it.invoice.invoiceNumber,
                    it.contact?.name,
                    it.invoice.note,
                    it.invoice.status,
                ).joinToString(" ").matchesQuery()
            }
            .take(3)
            .map {
                GlobalSearchResult(
                    title = it.invoice.invoiceNumber,
                    subtitle = "${it.contact?.name ?: "Tanpa kontak"} · ${it.invoice.status}",
                    type = "Invoice",
                    amount = (it.invoice.totalAmount - it.invoice.paidAmount).coerceAtLeast(0),
                )
            }

        val contacts = data.businessContacts
            .filter {
                listOf(it.name, it.phone, it.email, it.note).joinToString(" ").matchesQuery()
            }
            .take(2)
            .map {
                GlobalSearchResult(
                    title = it.name,
                    subtitle = it.type.replaceFirstChar { char -> char.uppercase() },
                    type = "Kontak",
                )
            }

        val inventory = data.inventoryItems
            .filter {
                listOf(it.name, it.sku).joinToString(" ").matchesQuery()
            }
            .take(2)
            .map {
                GlobalSearchResult(
                    title = it.name,
                    subtitle = "Stok ${it.stock} · Min ${it.minStock}",
                    type = "Produk",
                    amount = it.salePrice.takeIf { price -> price > 0 },
                )
            }

        return (transactions + invoices + contacts + inventory).take(8)
    }
}

class BizTrackViewModelFactory(
    private val repository: FinanceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BizTrackViewModel::class.java)) {
            return BizTrackViewModel(repository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
