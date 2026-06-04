package com.app.biztrack.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.bizTrackDataStore by preferencesDataStore(name = "biztrack_preferences")

class AppPreferencesDataSource(private val context: Context) {
    private object Keys {
        val BusinessName = stringPreferencesKey("business_name")
        val Currency = stringPreferencesKey("currency")
        val DateFormat = stringPreferencesKey("date_format")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val PinEnabled = booleanPreferencesKey("pin_enabled")
        val PinHash = stringPreferencesKey("pin_hash")
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val LastBackupAt = longPreferencesKey("last_backup_at")
        val MonthlyIncomeTarget = longPreferencesKey("monthly_income_target")
        val MonthlyExpenseLimit = longPreferencesKey("monthly_expense_limit")
    }

    val preferences: Flow<AppPreferences> = context.bizTrackDataStore.data.map { values ->
        AppPreferences(
            businessName = values[Keys.BusinessName] ?: "",
            currency = values[Keys.Currency] ?: "IDR",
            dateFormat = values[Keys.DateFormat] ?: "yyyy-MM-dd",
            themeMode = values[Keys.ThemeMode] ?: "light",
            pinEnabled = values[Keys.PinEnabled] ?: false,
            pinHash = values[Keys.PinHash] ?: "",
            onboardingCompleted = values[Keys.OnboardingCompleted] ?: false,
            lastBackupAt = values[Keys.LastBackupAt],
            monthlyIncomeTarget = values[Keys.MonthlyIncomeTarget] ?: 0,
            monthlyExpenseLimit = values[Keys.MonthlyExpenseLimit] ?: 0,
        )
    }

    suspend fun completeOnboarding(
        businessName: String,
        currency: String,
        themeMode: String,
    ) {
        context.bizTrackDataStore.edit { values ->
            values[Keys.BusinessName] = businessName
            values[Keys.Currency] = currency
            values[Keys.ThemeMode] = themeMode
            values[Keys.OnboardingCompleted] = true
        }
    }

    suspend fun updateBusinessInfo(businessName: String, currency: String) {
        context.bizTrackDataStore.edit { values ->
            values[Keys.BusinessName] = businessName
            values[Keys.Currency] = currency
        }
    }

    suspend fun updateThemeMode(themeMode: String) {
        context.bizTrackDataStore.edit { values ->
            values[Keys.ThemeMode] = themeMode
        }
    }

    suspend fun updateMonthlyTargets(incomeTarget: Long, expenseLimit: Long) {
        context.bizTrackDataStore.edit { values ->
            values[Keys.MonthlyIncomeTarget] = incomeTarget.coerceAtLeast(0)
            values[Keys.MonthlyExpenseLimit] = expenseLimit.coerceAtLeast(0)
        }
    }

    suspend fun updateDateFormat(dateFormat: String) {
        context.bizTrackDataStore.edit { values ->
            values[Keys.DateFormat] = dateFormat
        }
    }

    suspend fun updatePin(pinHash: String) {
        context.bizTrackDataStore.edit { values ->
            if (pinHash.isBlank()) {
                values[Keys.PinEnabled] = false
                values.remove(Keys.PinHash)
            } else {
                values[Keys.PinEnabled] = true
                values[Keys.PinHash] = pinHash
            }
        }
    }

    suspend fun markBackupCreated(timestamp: Long) {
        context.bizTrackDataStore.edit { values ->
            values[Keys.LastBackupAt] = timestamp
        }
    }

    suspend fun replaceFromBackup(preferences: AppPreferences) {
        context.bizTrackDataStore.edit { values ->
            values[Keys.BusinessName] = preferences.businessName
            values[Keys.Currency] = preferences.currency
            values[Keys.DateFormat] = preferences.dateFormat
            values[Keys.ThemeMode] = preferences.themeMode
            values[Keys.PinEnabled] = preferences.pinEnabled
            values[Keys.PinHash] = preferences.pinHash
            values[Keys.OnboardingCompleted] = preferences.onboardingCompleted
            values[Keys.MonthlyIncomeTarget] = preferences.monthlyIncomeTarget
            values[Keys.MonthlyExpenseLimit] = preferences.monthlyExpenseLimit
            preferences.lastBackupAt?.let { values[Keys.LastBackupAt] = it }
        }
    }

    suspend fun reset() {
        context.bizTrackDataStore.edit { it.clear() }
    }
}
