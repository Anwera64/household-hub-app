package com.homelab.household.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.homelab.household.app.navigation.AppNavHost
import com.homelab.household.app.platform.ExternalApps
import com.homelab.household.app.platform.LocalDragHidesKeyboard
import com.homelab.household.app.platform.LocalExternalApps
import com.homelab.household.app.platform.LocalLocalNetworkSettings
import com.homelab.household.app.platform.LocalNetworkSettings
import com.homelab.household.app.screens.localnetwork.LocalNetworkGateHost
import com.homelab.household.app.theme.HearthTheme
import com.homelab.household.app.util.WatchForMember
import com.homelab.household.domain.secret.AppSwitcherPrivacy
import com.homelab.household.domain.secret.SecretLockController
import org.koin.compose.koinInject

/**
 * The platform entry point: the theme, the other apps a screen can hand off to, whether scrolling
 * hides the keyboard (iOS, which has no back button for it), the watch that keeps secret chats
 * open only while the member is here, what the app switcher may see of them, local network access
 * (the explanation, and the cover when it is off), and the navigation host.
 */
@Composable
fun App(
    externalApps: ExternalApps,
    localNetworkSettings: LocalNetworkSettings,
    dragHidesKeyboard: Boolean = false,
) {
    CompositionLocalProvider(
        LocalExternalApps provides externalApps,
        LocalLocalNetworkSettings provides localNetworkSettings,
        LocalDragHidesKeyboard provides dragHidesKeyboard,
    ) {
        val secretLock = koinInject<SecretLockController>()
        // Paused is the moment the app switcher may take its picture; resumed is the member back.
        val appSwitcher = koinInject<AppSwitcherPrivacy>()
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { appSwitcher.onForeground() }
        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { appSwitcher.onBackground() }
        WatchForMember(onInput = secretLock::onUserInput, onStop = secretLock::onAppStopped) {
            HearthTheme {
                LocalNetworkGateHost { AppNavHost() }
            }
        }
    }
}
