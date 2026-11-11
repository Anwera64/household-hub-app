package com.homelab.household.app.screens.localnetwork

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.homelab.household.app.navigation.AppNavHost
import com.homelab.household.app.navigation.StubScreens
import com.homelab.household.app.platform.SettingsPlatform
import com.homelab.household.app.testing.FakeLocalNetworkAccess
import com.homelab.household.app.testing.FakeLocalNetworkSettings
import com.homelab.household.app.testing.TestApp
import com.homelab.household.app.testing.TestLifecycle
import com.homelab.household.app.testing.runScreenTest
import com.homelab.household.data.datasource.local.InMemorySessionStorage
import com.homelab.household.domain.localnetwork.LocalNetworkStatus
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The local network screens over the real app root: the gate, the navigation host with stub screens,
 * and the real graph. The hub at the far end finds no route, the way Android 17 blocks a phone at
 * home without local network access; the phone's permission and Settings page are faked.
 */
@OptIn(ExperimentalTestApi::class)
class LocalNetworkScreenTest {
    /** Every call times out connecting, as a blocked one does. */
    private val blockedHub = MockEngine { throw ConnectTimeoutException("connect timed out") }

    private val lifecycle = TestLifecycle()

    /** A phone signed in at home: the session renews on start, and that call finds no route. */
    private fun ComposeUiTest.signedInAtHome(
        access: FakeLocalNetworkAccess,
        settings: FakeLocalNetworkSettings,
        fontScale: Float? = null,
    ) {
        setContent {
            TestApp(
                blockedHub,
                sessionStorage = InMemorySessionStorage().apply { saveTokens("token-from-last-time") },
                localNetworkAccess = access,
                localNetworkSettings = settings,
                lifecycle = lifecycle,
            ) {
                val app = @androidx.compose.runtime.Composable {
                    LocalNetworkGateHost { AppNavHost(screens = StubScreens()) }
                }
                if (fontScale == null) {
                    app()
                } else {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                        Box(Modifier.requiredSize(SMALL_PHONE_WIDTH, SMALL_PHONE_HEIGHT)) { app() }
                    }
                }
            }
        }
    }

    /** iOS-style reads (no first-time check) so the cover tests start past the explanation. */
    private fun accessOff() = FakeLocalNetworkAccess(LocalNetworkStatus.Off, readsWithoutAsking = false)

    // 1 · First time

    @Test
    fun `GIVEN an Android phone never asked WHEN the app opens THEN it explains first`() {
        runScreenTest {
            setContent {
                TestApp(
                    blockedHub,
                    localNetworkAccess = FakeLocalNetworkAccess(LocalNetworkStatus.Off),
                    localNetworkSettings = FakeLocalNetworkSettings(),
                ) { LocalNetworkGateHost { AppNavHost(screens = StubScreens()) } }
            }

            onLocalNetwork { seesTheExplanation() }
        }
    }

    @Test
    fun `GIVEN the explanation WHEN the member taps Continue THEN the phone asks once`() {
        val settings = FakeLocalNetworkSettings()

        runScreenTest {
            setContent {
                TestApp(
                    blockedHub,
                    localNetworkAccess = FakeLocalNetworkAccess(LocalNetworkStatus.Off),
                    localNetworkSettings = settings,
                ) { LocalNetworkGateHost { AppNavHost(screens = StubScreens()) } }
            }

            onLocalNetwork {
                seesTheExplanation()
                tapsContinue()
            }
            waitUntil(timeoutMillis = WAIT_MILLIS) { settings.asked == 1 }
        }

        assertEquals(1, settings.asked)
    }

    // 2 · Access is off

    @Test
    fun `GIVEN access off on an iPhone WHEN a hub call finds no route THEN the cover says where to find it on an iPhone`() {
        runScreenTest {
            signedInAtHome(accessOff(), FakeLocalNetworkSettings(SettingsPlatform.Ios))

            onLocalNetwork {
                seesAccessIsOff()
                seesWhereToFindItOn(SettingsPlatform.Ios)
            }
        }
    }

    @Test
    fun `GIVEN access off on an Android phone WHEN a hub call finds no route THEN the cover says where to find it on Android`() {
        runScreenTest {
            signedInAtHome(accessOff(), FakeLocalNetworkSettings(SettingsPlatform.Android))

            onLocalNetwork {
                seesAccessIsOff()
                seesWhereToFindItOn(SettingsPlatform.Android)
            }
        }
    }

    @Test
    fun `GIVEN access is off on screen WHEN the member taps Open settings THEN Settings opens once`() {
        val settings = FakeLocalNetworkSettings()

        runScreenTest {
            signedInAtHome(accessOff(), settings)

            onLocalNetwork {
                seesAccessIsOff()
                tapsOpenSettings()
            }
        }

        assertEquals(1, settings.settingsOpened)
    }

    @Test
    fun `GIVEN Android can still ask WHEN a hub call finds no route THEN the cover offers Turn it on without saying where`() {
        runScreenTest {
            signedInAtHome(accessOff(), FakeLocalNetworkSettings(canAsk = true))

            onLocalNetwork {
                seesAccessIsOff()
                seesTurnItOn()
                doesNotSeeWhereToFindIt()
            }
        }
    }

    // 3 · Back from Settings

    @Test
    fun `GIVEN the member opened Settings WHEN they come back THEN the cover is checking`() {
        val access = accessOff()

        runScreenTest {
            signedInAtHome(access, FakeLocalNetworkSettings())
            onLocalNetwork {
                seesAccessIsOff()
                tapsOpenSettings()
            }
            access.holdBack = CompletableDeferred()

            lifecycle.leavesAndComesBack()

            onLocalNetwork { seesItChecking() }
        }
    }

    @Test
    fun `GIVEN the member opened Settings WHEN they come back with access still off THEN the cover says still off`() {
        runScreenTest {
            signedInAtHome(accessOff(), FakeLocalNetworkSettings())
            onLocalNetwork {
                seesAccessIsOff()
                tapsOpenSettings()
            }

            lifecycle.leavesAndComesBack()

            onLocalNetwork { seesItStillOff() }
        }
    }

    @Test
    fun `GIVEN the member opened Settings WHEN they come back with access on THEN the cover goes and they are back on home Wi-Fi`() {
        val access = accessOff()

        runScreenTest {
            signedInAtHome(access, FakeLocalNetworkSettings())
            onLocalNetwork {
                seesAccessIsOff()
                tapsOpenSettings()
            }
            access.status = LocalNetworkStatus.Allowed

            lifecycle.leavesAndComesBack()

            onLocalNetwork {
                seesNoCover()
                seesBackOnHomeWifi()
            }
        }
    }

    // 4 · Don't ask again, and Not now

    @Test
    fun `GIVEN access is off on screen WHEN the member taps Don't ask again THEN the cover goes and says it is in Profile`() {
        runScreenTest {
            signedInAtHome(accessOff(), FakeLocalNetworkSettings())

            onLocalNetwork {
                seesAccessIsOff()
                tapsDontAskAgain()
                seesNoCover()
                seesItCanBeTurnedOnLaterInProfile()
            }
        }
    }

    @Test
    fun `GIVEN a member signed in at home WHEN a hub call finds no route with access off THEN the cover comes up over Home and Home is still there after Not now`() {
        runScreenTest {
            signedInAtHome(accessOff(), FakeLocalNetworkSettings())

            onLocalNetwork {
                seesAccessIsOff()
                tapsNotNow()
                seesNoCover()
                seesHome()
            }
        }
    }

    @Test
    fun `GIVEN large text on a small phone WHEN access is off on screen THEN the last button scrolls into reach`() {
        runScreenTest {
            signedInAtHome(accessOff(), FakeLocalNetworkSettings(SettingsPlatform.Ios), fontScale = LARGE_TEXT)

            onLocalNetwork {
                seesAccessIsOff()
                reachesDontAskAgain()
            }
        }
    }

    private companion object {
        /** The cover only comes up once the renewal has failed and access was read. */
        const val WAIT_MILLIS = 15_000L
        const val LARGE_TEXT = 2f
        val SMALL_PHONE_WIDTH = 360.dp
        val SMALL_PHONE_HEIGHT = 640.dp
    }
}
