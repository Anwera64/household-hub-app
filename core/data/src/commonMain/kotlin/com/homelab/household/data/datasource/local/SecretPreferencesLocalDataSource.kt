package com.homelab.household.data.datasource.local

import com.russhwolf.settings.Settings

/** A member's Secret Mode choices on this phone, in the platform's own small store. */
class SecretPreferencesLocalDataSource(
    private val settings: Settings,
) {
    fun asksBeforeTurningOff(memberId: String): Boolean = settings.getBoolean(asksKey(memberId), defaultValue = true)

    fun stopAskingBeforeTurningOff(memberId: String) = settings.putBoolean(asksKey(memberId), false)

    private fun asksKey(memberId: String) = "secret.asks_before_off.$memberId"
}
