package com.app.biztrack.data.preferences

data class AppPreferences(
    val businessName: String = "",
    val currency: String = "IDR",
    val dateFormat: String = "yyyy-MM-dd",
    val themeMode: String = "light",
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val onboardingCompleted: Boolean = false,
    val lastBackupAt: Long? = null,
    val monthlyIncomeTarget: Long = 0,
    val monthlyExpenseLimit: Long = 0,
)
