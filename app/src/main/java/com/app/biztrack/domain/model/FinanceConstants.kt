package com.app.biztrack.domain.model

object TransactionTypes {
    const val INCOME = "income"
    const val EXPENSE = "expense"
    const val TRANSFER = "transfer"
}

object CategoryTypes {
    const val INCOME = "income"
    const val EXPENSE = "expense"
}

object DebtTypes {
    const val DEBT = "debt"
    const val RECEIVABLE = "receivable"
}

object DebtStatus {
    const val UNPAID = "unpaid"
    const val PARTIAL = "partial"
    const val PAID = "paid"
    const val OVERDUE = "overdue"
}

object ContactTypes {
    const val CUSTOMER = "customer"
    const val SUPPLIER = "supplier"
}

object InvoiceStatus {
    const val DRAFT = "draft"
    const val SENT = "sent"
    const val UNPAID = "unpaid"
    const val PARTIAL = "partial"
    const val PAID = "paid"
    const val OVERDUE = "overdue"
}
