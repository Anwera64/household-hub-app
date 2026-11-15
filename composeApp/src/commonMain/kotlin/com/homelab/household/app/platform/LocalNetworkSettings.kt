package com.homelab.household.app.platform

import androidx.compose.runtime.staticCompositionLocalOf

/** Which phone the app runs on, for the one line that differs: where the switch is in Settings. */
enum class SettingsPlatform { Android, Ios }

/**
 * The phone's own side of local network access (#5): its question, and its Settings page. Each
 * platform supplies its own through [com.homelab.household.app.App]; tests supply a fake that counts.
 */
interface LocalNetworkSettings {
    val platform: SettingsPlatform

    /**
     * Shows the phone's own question and waits for the answer. True unless the member said no.
     * iOS has no API for its question (it asks by itself at the first local connection), so there
     * this returns true at once.
     */
    suspend fun ask(): Boolean

    /** Whether the phone can still show its question: on Android after a single "Don't allow". */
    fun canAskAgain(): Boolean

    /** The app's own page in the phone's Settings. */
    fun openSettings()
}

val LocalLocalNetworkSettings =
    staticCompositionLocalOf<LocalNetworkSettings> {
        error("No LocalNetworkSettings provided: App() and TestApp provide one")
    }
