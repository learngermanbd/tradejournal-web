package com.tradejournal.app.data

import android.content.Context

data class UserSettings(
    val displayName: String = "Alex",
    val currency: String = "USD",
    val defaultMarket: String = "Stocks",
    val riskLimitPercent: String = "5",
    val reviewReminder: Boolean = true,
    val cloudAiEnabled: Boolean = false,
)

class UserSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("tradejournal_preferences", Context.MODE_PRIVATE)

    fun load(): UserSettings = UserSettings(
        displayName = preferences.getString("displayName", "Alex") ?: "Alex",
        currency = preferences.getString("currency", "USD") ?: "USD",
        defaultMarket = preferences.getString("defaultMarket", "Stocks") ?: "Stocks",
        riskLimitPercent = preferences.getString("riskLimitPercent", "5") ?: "5",
        reviewReminder = preferences.getBoolean("reviewReminder", true),
        cloudAiEnabled = preferences.getBoolean("cloudAiEnabled", false),
    )

    fun save(settings: UserSettings) {
        preferences.edit()
            .putString("displayName", settings.displayName)
            .putString("currency", settings.currency)
            .putString("defaultMarket", settings.defaultMarket)
            .putString("riskLimitPercent", settings.riskLimitPercent)
            .putBoolean("reviewReminder", settings.reviewReminder)
            .putBoolean("cloudAiEnabled", settings.cloudAiEnabled)
            .apply()
    }
}
