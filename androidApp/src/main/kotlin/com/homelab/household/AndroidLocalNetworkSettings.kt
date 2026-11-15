package com.homelab.household

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.homelab.household.app.platform.LocalNetworkSettings
import com.homelab.household.app.platform.SettingsPlatform
import com.homelab.household.localnetwork.AndroidLocalNetworkAccess
import kotlinx.coroutines.CompletableDeferred

/**
 * Android's side of local network access (#5): `ACCESS_LOCAL_NETWORK`, a runtime permission in the
 * Nearby devices group from Android 17 (API 37). Built in [MainActivity.onCreate], because a result
 * launcher has to be registered before the activity is started.
 */
class AndroidLocalNetworkSettings(
    private val activity: ComponentActivity,
) : LocalNetworkSettings {
    override val platform: SettingsPlatform = SettingsPlatform.Android

    private var pending: CompletableDeferred<Boolean>? = null

    private val launcher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pending?.complete(granted)
            pending = null
        }

    override suspend fun ask(): Boolean {
        if (Build.VERSION.SDK_INT < ANDROID_17) return true
        // One question at a time; an older one still waiting is over.
        pending?.complete(false)
        val answer = CompletableDeferred<Boolean>()
        pending = answer
        launcher.launch(AndroidLocalNetworkAccess.PERMISSION)
        return answer.await()
    }

    /** True after a single "Don't allow"; after two, Android stops asking and only Settings is left. */
    override fun canAskAgain(): Boolean =
        Build.VERSION.SDK_INT >= ANDROID_17 &&
            activity.shouldShowRequestPermissionRationale(AndroidLocalNetworkAccess.PERMISSION)

    /** App info, where the switch is under Permissions › Nearby devices. */
    override fun openSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null))
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No settings app to open: nothing to hand over to.
        }
    }

    private companion object {
        const val ANDROID_17 = 37
    }
}
