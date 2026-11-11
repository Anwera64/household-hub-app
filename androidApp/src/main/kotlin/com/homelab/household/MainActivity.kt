package com.homelab.household

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.homelab.household.app.App
import com.homelab.household.domain.secret.AppSwitcherPrivacy
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private lateinit var externalApps: AndroidExternalApps
    private lateinit var localNetworkSettings: AndroidLocalNetworkSettings
    private val appSwitcher: AppSwitcherPrivacy by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate: swaps the splash theme for Theme.HyggeHub. The splash leaves with
        // the first frame — nothing holds it on screen.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        externalApps = AndroidExternalApps(this)
        // Registers its permission launcher, which has to happen before the activity starts.
        localNetworkSettings = AndroidLocalNetworkSettings(this)
        setContent {
            App(externalApps = externalApps, localNetworkSettings = localNetworkSettings)
        }
        hideFromRecentsWhileSecret()
    }

    /**
     * Recent apps shows a blank card, not the screen, while secret chats are unlocked (#30).
     * Screenshots inside the app still work. The switch exists from Android 13; older phones keep
     * showing the screen there.
     */
    private fun hideFromRecentsWhileSecret() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        lifecycleScope.launch {
            appSwitcher.hides.collect { hides -> setRecentsScreenshotEnabled(!hides) }
        }
    }

    /** A browser sign-in coming back (`hyggehub://calendar/…`), delivered here because the activity is `singleTask`. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { externalApps.onRedirect(it.toString()) }
    }

    // After onNewIntent, so a sign-in that came back has already been handed over.
    override fun onResume() {
        super.onResume()
        externalApps.onResumed()
    }
}
