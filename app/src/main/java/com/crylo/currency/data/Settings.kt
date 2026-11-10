package com.crylo.currency.data

import android.content.Context
import androidx.core.content.edit

/** The user's list, the row being edited and its expression. Nothing else is stored. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var currencies: List<String>?
        get() = prefs.getString(KEY_LIST, null)?.split(',')?.filter { it.isNotBlank() }
        set(value) = prefs.edit { putString(KEY_LIST, value?.joinToString(",")) }

    var active: String?
        get() = prefs.getString(KEY_ACTIVE, null)
        set(value) = prefs.edit { putString(KEY_ACTIVE, value) }

    var expression: String
        get() = prefs.getString(KEY_EXPR, null) ?: "1"
        set(value) = prefs.edit { putString(KEY_EXPR, value) }

    private companion object {
        const val KEY_LIST = "currencies"
        const val KEY_ACTIVE = "active"
        const val KEY_EXPR = "expression"
    }
}
