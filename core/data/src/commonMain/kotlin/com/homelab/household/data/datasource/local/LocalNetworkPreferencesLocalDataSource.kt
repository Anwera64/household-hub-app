package com.homelab.household.data.datasource.local

import com.russhwolf.settings.Settings

/** What this phone remembers about local network access, in the platform's own small store. */
class LocalNetworkPreferencesLocalDataSource(
    private val settings: Settings,
) {
    fun explained(): Boolean = settings.getBoolean(EXPLAINED, defaultValue = false)

    fun markExplained() = settings.putBoolean(EXPLAINED, true)

    fun stoppedAsking(): Boolean = settings.getBoolean(STOPPED_ASKING, defaultValue = false)

    fun stopAsking() = settings.putBoolean(STOPPED_ASKING, true)

    private companion object {
        const val EXPLAINED = "local_network.explained"
        const val STOPPED_ASKING = "local_network.stopped_asking"
    }
}
