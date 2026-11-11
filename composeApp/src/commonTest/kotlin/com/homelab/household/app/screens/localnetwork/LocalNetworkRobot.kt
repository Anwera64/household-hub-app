package com.homelab.household.app.screens.localnetwork

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.homelab.household.app.navigation.StubScreens
import com.homelab.household.app.platform.SettingsPlatform
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.app_name
import com.homelab.household.app.resources.local_network_back_on_wifi
import com.homelab.household.app.resources.local_network_checking
import com.homelab.household.app.resources.local_network_chip_still_off
import com.homelab.household.app.resources.local_network_continue
import com.homelab.household.app.resources.local_network_first_body
import com.homelab.household.app.resources.local_network_first_title
import com.homelab.household.app.resources.local_network_off_title
import com.homelab.household.app.resources.local_network_open_settings
import com.homelab.household.app.resources.local_network_still_off_body
import com.homelab.household.app.resources.local_network_turn_on
import com.homelab.household.app.resources.local_network_turn_on_later
import com.homelab.household.app.resources.local_network_where
import com.homelab.household.app.resources.local_network_where_android
import com.homelab.household.app.resources.local_network_where_android_spoken
import com.homelab.household.app.resources.local_network_where_ios
import com.homelab.household.app.resources.secret_off_dont_ask
import com.homelab.household.app.resources.secret_offer_not_now
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** The local network screens as a person sees them: the explanation, and the cover saying access is off. */
@OptIn(ExperimentalTestApi::class)
class LocalNetworkRobot(
    private val test: ComposeUiTest,
) {
    suspend fun seesTheExplanation() {
        seesHeading(getString(Res.string.local_network_first_title))
        seesText(named(Res.string.local_network_first_body))
        seesText(getString(Res.string.local_network_continue))
    }

    suspend fun tapsContinue() = taps(getString(Res.string.local_network_continue))

    suspend fun seesAccessIsOff() = seesHeading(getString(Res.string.local_network_off_title))

    /** The one line that differs per phone; Android's path is spoken step by step. */
    suspend fun seesWhereToFindItOn(platform: SettingsPlatform) {
        seesText(getString(Res.string.local_network_where))
        when (platform) {
            SettingsPlatform.Ios -> {
                seesText(named(Res.string.local_network_where_ios))
            }

            SettingsPlatform.Android -> {
                val spoken = named(Res.string.local_network_where_android_spoken)
                test.waitUntilExactlyOneExists(
                    hasText(named(Res.string.local_network_where_android)),
                    timeoutMillis = WAIT_MILLIS,
                )
                test
                    .onNodeWithText(named(Res.string.local_network_where_android))
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(spoken)))
            }
        }
    }

    suspend fun doesNotSeeWhereToFindIt() {
        test.onNodeWithText(getString(Res.string.local_network_where)).assertDoesNotExist()
    }

    suspend fun tapsOpenSettings() = taps(getString(Res.string.local_network_open_settings))

    suspend fun seesTurnItOn() = seesText(getString(Res.string.local_network_turn_on))

    suspend fun seesItChecking() = seesText(getString(Res.string.local_network_checking))

    suspend fun seesItStillOff() {
        seesText(named(Res.string.local_network_still_off_body))
        seesText(getString(Res.string.local_network_chip_still_off))
    }

    suspend fun tapsNotNow() = taps(getString(Res.string.secret_offer_not_now))

    suspend fun tapsDontAskAgain() = taps(getString(Res.string.secret_off_dont_ask))

    /** The quietest button is the last one: with large text it is scrolled to. */
    suspend fun reachesDontAskAgain() {
        val label = getString(Res.string.secret_off_dont_ask)
        test.waitUntilExactlyOneExists(hasText(label), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(label).performScrollTo().assertIsDisplayed()
    }

    suspend fun seesNoCover() {
        test.waitUntilDoesNotExist(hasText(getString(Res.string.local_network_off_title)), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(getString(Res.string.local_network_first_title)).assertDoesNotExist()
    }

    suspend fun seesBackOnHomeWifi() = seesText(getString(Res.string.local_network_back_on_wifi))

    suspend fun seesItCanBeTurnedOnLaterInProfile() = seesText(getString(Res.string.local_network_turn_on_later))

    /** The screen that was open underneath is still there. */
    fun seesHome() = seesText(StubScreens.HOME)

    private suspend fun named(resource: StringResource) = getString(resource, getString(Res.string.app_name))

    private fun seesHeading(text: String) {
        test.waitUntilExactlyOneExists(hasText(text) and isHeading(), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(text).assertIsDisplayed()
    }

    private fun seesText(text: String) {
        test.waitUntilExactlyOneExists(hasText(text), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(text).assertIsDisplayed()
    }

    private fun taps(text: String) {
        test.waitUntilExactlyOneExists(hasText(text), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(text).performScrollTo().performClick()
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
    }
}

@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.onLocalNetwork(block: suspend LocalNetworkRobot.() -> Unit) {
    LocalNetworkRobot(this).block()
}
