package com.homelab.household

import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.homelab.household.domain.localnetwork.LocalNetworkGate
import com.homelab.household.domain.localnetwork.LocalNetworkScreen
import com.homelab.household.localnetwork.AndroidLocalNetworkAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Android 17 blocks the hub on home Wi-Fi until `ACCESS_LOCAL_NETWORK` is granted (#5), so a phone
 * that never answered is told why first. Only API 37 has the permission: on older emulators (CI runs
 * API 36) these are skipped. Run them on the Pixel_10_Pro AVD (API 37) with fresh app data and the
 * permission revoked: `adb shell pm clear com.homelab.household.dev` then
 * `adb shell pm revoke com.homelab.household.dev android.permission.ACCESS_LOCAL_NETWORK`. Revoking
 * from inside the test would kill the app's process, and the test with it.
 */
@RunWith(AndroidJUnit4::class)
class LocalNetworkFirstTimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before
    fun onlyWherePermissionExists() {
        assumeTrue("ACCESS_LOCAL_NETWORK exists from API 37", Build.VERSION.SDK_INT >= ANDROID_17)
    }

    @Test
    fun GIVEN_access_not_granted_on_fresh_app_data_WHEN_the_app_opens_THEN_it_explains_first() {
        assumeFalse("needs the permission revoked first", granted())

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            awaitScreen(LocalNetworkScreen.FirstTime)
        }
    }

    @Test
    fun GIVEN_the_first_time_screen_WHEN_the_permission_is_granted_and_the_app_resumes_THEN_it_is_gone() {
        assumeFalse("needs the permission revoked first", granted())

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            awaitScreen(LocalNetworkScreen.FirstTime)
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                AndroidLocalNetworkAccess.PERMISSION,
            )

            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            awaitScreen(LocalNetworkScreen.Hidden)
        }
    }

    private fun granted() =
        context.checkSelfPermission(AndroidLocalNetworkAccess.PERMISSION) == PackageManager.PERMISSION_GRANTED

    private fun awaitScreen(screen: LocalNetworkScreen) =
        runBlocking {
            withTimeout(WAIT_MILLIS) {
                GlobalContext
                    .get()
                    .get<LocalNetworkGate>()
                    .screen
                    .first { it == screen }
            }
        }

    private companion object {
        const val ANDROID_17 = 37
        const val WAIT_MILLIS = 10_000L
    }
}
