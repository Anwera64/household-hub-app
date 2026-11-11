package com.homelab.household.app.testing

import com.homelab.household.app.platform.LocalNetworkSettings
import com.homelab.household.app.platform.SettingsPlatform
import com.homelab.household.data.datasource.local.LocalNetworkPreferencesLocalDataSource
import com.homelab.household.data.repository.LocalNetworkRepositoryImpl
import com.homelab.household.domain.localnetwork.LocalNetworkAccess
import com.homelab.household.domain.localnetwork.LocalNetworkStatus
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CompletableDeferred

/** The phone's local network permission, as the test says. Allowed unless told otherwise. */
class FakeLocalNetworkAccess(
    var status: LocalNetworkStatus = LocalNetworkStatus.Allowed,
    override val readsWithoutAsking: Boolean = true,
) : LocalNetworkAccess {
    /** Set to keep the next read waiting, so a check can be seen while it runs. */
    var holdBack: CompletableDeferred<Unit>? = null

    override suspend fun status(): LocalNetworkStatus {
        holdBack?.await()
        return status
    }
}

/** Counts the phone's question and its Settings page instead of showing them. */
class FakeLocalNetworkSettings(
    override val platform: SettingsPlatform = SettingsPlatform.Android,
    var canAsk: Boolean = false,
    /** What the member answers to the phone's question; also what [access] becomes on a yes. */
    var answer: Boolean = false,
    private val access: FakeLocalNetworkAccess? = null,
) : LocalNetworkSettings {
    var asked = 0
        private set
    var settingsOpened = 0
        private set

    override suspend fun ask(): Boolean {
        asked++
        if (answer) access?.status = LocalNetworkStatus.Allowed
        return answer
    }

    override fun canAskAgain(): Boolean = canAsk

    override fun openSettings() {
        settingsOpened++
    }
}

/** A phone the app already explained local network access on, so it starts past the explanation. */
fun explainedBefore(): Settings =
    MapSettings().also { LocalNetworkRepositoryImpl(LocalNetworkPreferencesLocalDataSource(it)).markExplained() }
