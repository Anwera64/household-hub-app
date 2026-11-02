package com.homelab.household.app.screens.conversation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEventBus
import com.homelab.household.app.testing.FakePrivacyHub
import com.homelab.household.app.testing.TestApp
import com.homelab.household.app.testing.runScreenTest
import com.homelab.household.data.datasource.local.InMemorySessionStorage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A message the hub judges private, through the app's real wiring down to the HTTP engine: the
 * line under it, the offer after the answer, and what each of its buttons does (slice 6, P4).
 */
@OptIn(ExperimentalTestApi::class)
class PrivacyOfferTest {
    @Test
    fun `GIVEN an ordinary chat WHEN a private message is answered and the member makes it secret THEN the chat is secret from here on`() =
        runScreenTest {
            privacyScreen(FakePrivacyHub())

            onConversation {
                waitsFor(FakePrivacyHub.ASKED)
                says(FakePrivacyHub.PRIVATE_WORDS)
                seesItKeptBetweenUs(question = FakePrivacyHub.PRIVATE_WORDS)
                seesTheSecretOffer(agentNoticed = false)
                makesItSecret()
                seesSecretModeOn()
                seesTheSecretMarkerBetween(before = FakePrivacyHub.SAFE, after = null)
                doesNotSeeTheSecretOffer()
            }
        }

    @Test
    fun `GIVEN an offer after a private message WHEN the member says not now THEN the card goes and the hub is told`() =
        runScreenTest {
            val hub = FakePrivacyHub()
            privacyScreen(hub)

            onConversation {
                waitsFor(FakePrivacyHub.ASKED)
                says(FakePrivacyHub.PRIVATE_WORDS)
                seesTheSecretOffer(agentNoticed = false)
                saysNotNow()
                doesNotSeeTheSecretOffer()
            }
            assertEquals(1, hub.declines)
        }
}

/** The real conversation screen and everything behind it, against [hub], opened on its ordinary chat. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.privacyScreen(hub: FakePrivacyHub) {
    setContent {
        TestApp(hub.engine, sessionStorage = InMemorySessionStorage().apply { saveTokens("signed-in-token") }) {
            // Navigation provides the bus a conversation hears "calendar connected" on.
            CompositionLocalProvider(LocalResultEventBus provides ResultEventBus()) {
                ConversationScreen(sessionId = "s-2", onBack = {})
            }
        }
    }
}
