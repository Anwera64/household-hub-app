package com.homelab.household.domain.localnetwork

import com.homelab.household.domain.repository.LocalNetworkRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * When the app explains local network access, and when it says access is off. It only ever says so
 * once a call to the hub found no route while access is off: a phone away on Tailscale never sees it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalNetworkGateTest {
    private class FakeAccess(
        override val readsWithoutAsking: Boolean,
        var answer: LocalNetworkStatus,
    ) : LocalNetworkAccess {
        var asked = 0

        /** Set to hold the next answer back, so the check can be seen while it runs. */
        var holdBack: CompletableDeferred<Unit>? = null

        override suspend fun status(): LocalNetworkStatus {
            asked++
            holdBack?.await()
            return answer
        }
    }

    private class FakeChoices(
        var explained: Boolean = false,
        var stopped: Boolean = false,
    ) : LocalNetworkRepository {
        override fun explained(): Boolean = explained

        override fun markExplained() {
            explained = true
        }

        override fun stoppedAsking(): Boolean = stopped

        override fun stopAsking() {
            stopped = true
        }
    }

    private val hubUnreachable = MutableSharedFlow<Unit>()

    private fun android(answer: LocalNetworkStatus) = FakeAccess(readsWithoutAsking = true, answer = answer)

    private fun iPhone(answer: LocalNetworkStatus = LocalNetworkStatus.Off) =
        FakeAccess(readsWithoutAsking = false, answer = answer)

    private fun TestScope.gate(
        access: FakeAccess,
        choices: FakeChoices = FakeChoices(),
        signedIn: Boolean = false,
    ): LocalNetworkGate =
        LocalNetworkGate(
            access = access,
            choices = choices,
            hasStoredSession = { signedIn },
            hubUnreachable = hubUnreachable,
            scope = backgroundScope,
        )

    /** A gate already past its start, the explanation behind it. */
    private fun TestScope.started(
        access: FakeAccess,
        choices: FakeChoices = FakeChoices(explained = true),
        signedIn: Boolean = false,
    ): LocalNetworkGate =
        gate(access, choices, signedIn).also {
            it.onStart()
            runCurrent()
        }

    private suspend fun TestScope.hubFindsNoRoute() {
        hubUnreachable.emit(Unit)
        runCurrent()
    }

    private fun TestScope.eventsOf(gate: LocalNetworkGate): List<LocalNetworkEvent> {
        val seen = mutableListOf<LocalNetworkEvent>()
        backgroundScope.launch { gate.events.collect { seen += it } }
        runCurrent()
        return seen
    }

    // First time

    @Test
    fun `GIVEN an Android phone never asked with access off WHEN the app starts THEN it explains first`() =
        runTest {
            val gate = gate(android(LocalNetworkStatus.Off))

            gate.onStart()
            runCurrent()

            assertEquals(LocalNetworkScreen.FirstTime, gate.screen.value)
        }

    @Test
    fun `GIVEN an Android phone never asked with access already on WHEN the app starts THEN nothing shows`() =
        runTest {
            val gate = gate(android(LocalNetworkStatus.Allowed))

            gate.onStart()
            runCurrent()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN an Android phone too old to need access WHEN the app starts THEN nothing shows`() =
        runTest {
            val gate = gate(android(LocalNetworkStatus.NotNeeded))

            gate.onStart()
            runCurrent()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN a new iPhone install WHEN the app starts THEN it explains first without touching the network`() =
        runTest {
            val access = iPhone()
            val gate = gate(access)

            gate.onStart()
            runCurrent()

            assertEquals(LocalNetworkScreen.FirstTime, gate.screen.value)
            assertEquals(0, access.asked)
        }

    @Test
    fun `GIVEN an iPhone already signed in WHEN the app starts THEN it does not explain`() =
        runTest {
            val gate = gate(iPhone(), signedIn = true)

            gate.onStart()
            runCurrent()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN an iPhone already signed in WHEN the app starts THEN it counts as explained after a sign out too`() =
        runTest {
            val choices = FakeChoices()
            val gate = gate(iPhone(), choices, signedIn = true)

            gate.onStart()
            runCurrent()

            assertTrue(choices.explained)
        }

    @Test
    fun `GIVEN the explanation on screen WHEN the member taps Continue THEN it goes and is never shown again`() =
        runTest {
            val choices = FakeChoices()
            val gate = gate(android(LocalNetworkStatus.Off), choices)
            gate.onStart()
            runCurrent()

            gate.onContinue()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
            assertTrue(choices.explained)
        }

    @Test
    fun `GIVEN the explanation on an Android phone WHEN the member comes back with access turned on THEN it goes`() =
        runTest {
            val access = android(LocalNetworkStatus.Off)
            val gate = gate(access)
            gate.onStart()
            runCurrent()
            access.answer = LocalNetworkStatus.Allowed

            gate.onResumed()
            runCurrent()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN the explanation on an iPhone WHEN the member comes back to the app THEN access is not read`() =
        runTest {
            val access = iPhone()
            val gate = gate(access)
            gate.onStart()
            runCurrent()

            gate.onResumed()
            runCurrent()

            assertEquals(LocalNetworkScreen.FirstTime, gate.screen.value)
            assertEquals(0, access.asked)
        }

    @Test
    fun `GIVEN access off but no hub call has failed WHEN the app starts THEN nothing shows`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    // Access is off

    @Test
    fun `GIVEN access off WHEN a hub call finds no route THEN it says access is off`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))

            hubFindsNoRoute()

            assertEquals(LocalNetworkScreen.Off, gate.screen.value)
        }

    @Test
    fun `GIVEN a signed in iPhone with access off WHEN a hub call finds no route THEN it says access is off`() =
        runTest {
            val gate = started(iPhone(LocalNetworkStatus.Off), FakeChoices(), signedIn = true)

            hubFindsNoRoute()

            assertEquals(LocalNetworkScreen.Off, gate.screen.value)
        }

    @Test
    fun `GIVEN access on WHEN a hub call finds no route THEN nothing shows`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Allowed))

            hubFindsNoRoute()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN an iPhone whose check cannot tell WHEN a hub call finds no route THEN nothing shows`() =
        runTest {
            val gate = started(iPhone(LocalNetworkStatus.Unknown), signedIn = true)

            hubFindsNoRoute()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN a check already running WHEN more hub calls find no route THEN access is read once`() =
        runTest {
            val access = android(LocalNetworkStatus.Off).apply { holdBack = CompletableDeferred() }
            started(access)
            hubFindsNoRoute()

            hubFindsNoRoute()

            assertEquals(1, access.asked)
        }

    @Test
    fun `GIVEN the member said no to the phone's question WHEN a hub call finds no route THEN nothing shows until the app restarts`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))
            gate.onSystemPromptAnswered(granted = false)

            hubFindsNoRoute()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN the member said no to the phone's question WHEN the app is restarted and a hub call finds no route THEN it says access is off`() =
        runTest {
            started(android(LocalNetworkStatus.Off)).onSystemPromptAnswered(granted = false)
            val next = started(android(LocalNetworkStatus.Off))

            hubFindsNoRoute()

            assertEquals(LocalNetworkScreen.Off, next.screen.value)
        }

    @Test
    fun `GIVEN access is off on screen WHEN the member taps Not now THEN it goes`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))
            hubFindsNoRoute()

            gate.onNotNow()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN the member tapped Not now WHEN another hub call finds no route THEN nothing shows`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))
            hubFindsNoRoute()
            gate.onNotNow()

            hubFindsNoRoute()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN access is off on screen WHEN the member taps Don't ask again THEN it goes and says where the fix is once`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))
            val events = eventsOf(gate)
            hubFindsNoRoute()

            gate.onDontAskAgain()
            runCurrent()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
            assertEquals(listOf(LocalNetworkEvent.StoppedAsking), events)
        }

    @Test
    fun `GIVEN the member chose Don't ask again WHEN the app restarts and a hub call finds no route THEN nothing shows`() =
        runTest {
            val choices = FakeChoices(explained = true)
            started(android(LocalNetworkStatus.Off), choices).onDontAskAgain()
            val next = started(android(LocalNetworkStatus.Off), choices)

            hubFindsNoRoute()

            assertEquals(LocalNetworkScreen.Hidden, next.screen.value)
        }

    @Test
    fun `GIVEN an Android phone never asked that chose Don't ask again WHEN the app starts THEN it does not explain`() =
        runTest {
            val gate = gate(android(LocalNetworkStatus.Off), FakeChoices(stopped = true))

            gate.onStart()
            runCurrent()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    // Launch's own offline screen

    @Test
    fun `GIVEN launch is showing its offline screen WHEN a hub call finds no route with access off THEN nothing covers it`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))
            gate.coversAllowed(false)

            hubFindsNoRoute()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    @Test
    fun `GIVEN access is off on screen WHEN launch shows its offline screen THEN the cover goes`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))
            hubFindsNoRoute()

            gate.coversAllowed(false)

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
        }

    // Back from Settings

    @Test
    fun `GIVEN access is off on screen WHEN the member comes back to the app THEN it checks`() =
        runTest {
            val access = android(LocalNetworkStatus.Off)
            val gate = started(access)
            hubFindsNoRoute()
            access.holdBack = CompletableDeferred()

            gate.onResumed()
            runCurrent()

            assertEquals(LocalNetworkScreen.Checking, gate.screen.value)
        }

    @Test
    fun `GIVEN access is off on screen WHEN the member comes back with it still off THEN it says still off`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))
            hubFindsNoRoute()

            gate.onResumed()
            runCurrent()

            assertEquals(LocalNetworkScreen.StillOff, gate.screen.value)
        }

    @Test
    fun `GIVEN access is off on screen WHEN the member comes back with it turned on THEN it goes and says they are back on home Wi-Fi`() =
        runTest {
            val access = android(LocalNetworkStatus.Off)
            val gate = started(access)
            val events = eventsOf(gate)
            hubFindsNoRoute()
            access.answer = LocalNetworkStatus.Allowed

            gate.onResumed()
            runCurrent()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
            assertEquals(listOf(LocalNetworkEvent.BackOnWifi), events)
        }

    @Test
    fun `GIVEN nothing on screen WHEN the member comes back to the app THEN access is not read`() =
        runTest {
            val access = android(LocalNetworkStatus.Off)
            val gate = started(access)

            gate.onResumed()
            runCurrent()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
            assertEquals(0, access.asked)
        }

    @Test
    fun `GIVEN access is off on screen WHEN the member says yes to the phone's question THEN it goes and says they are back on home Wi-Fi`() =
        runTest {
            val gate = started(android(LocalNetworkStatus.Off))
            val events = eventsOf(gate)
            hubFindsNoRoute()

            gate.onSystemPromptAnswered(granted = true)
            runCurrent()

            assertEquals(LocalNetworkScreen.Hidden, gate.screen.value)
            assertEquals(listOf(LocalNetworkEvent.BackOnWifi), events)
        }
}
