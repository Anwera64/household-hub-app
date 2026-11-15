package com.homelab.household

import com.homelab.household.app.platform.LocalNetworkSettings
import com.homelab.household.app.platform.SettingsPlatform
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * iOS's side of local network access (#5), and the twin of `AndroidLocalNetworkSettings`. iOS has no
 * API to show its question or read the answer (Apple TN3179): it asks by itself the first time the
 * app reaches the home network, with `NSLocalNetworkUsageDescription`. After a "Don't Allow", the
 * switch is on the app's own page in Settings, which is where this opens.
 */
class IosLocalNetworkSettings : LocalNetworkSettings {
    override val platform: SettingsPlatform = SettingsPlatform.Ios

    /** Nothing to show: iOS asks by itself at the first local connection. */
    override suspend fun ask(): Boolean = true

    override fun canAskAgain(): Boolean = false

    override fun openSettings() {
        val settings = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(settings, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}
