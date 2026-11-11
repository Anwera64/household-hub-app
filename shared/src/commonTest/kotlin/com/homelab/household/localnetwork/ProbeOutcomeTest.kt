package com.homelab.household.localnetwork

import com.homelab.household.domain.localnetwork.LocalNetworkStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * iOS can't be asked about local network access (Apple TN3179), so the app tries a connection to the
 * hub and reads how it went. Only a connection waiting because local network access was denied says
 * access is off.
 */
class ProbeOutcomeTest {
    @Test
    fun `GIVEN a test connection to the hub WHEN it is ready THEN access is allowed`() {
        assertEquals(LocalNetworkStatus.Allowed, localNetworkStatusOf(ProbeOutcome.Ready))
    }

    @Test
    fun `GIVEN a test connection to the hub WHEN it waits because local network access was denied THEN access is off`() {
        assertEquals(LocalNetworkStatus.Off, localNetworkStatusOf(ProbeOutcome.LocalNetworkDenied))
    }

    @Test
    fun `GIVEN a test connection to the hub WHEN it waits for another reason THEN nothing is known`() {
        assertEquals(LocalNetworkStatus.Unknown, localNetworkStatusOf(ProbeOutcome.WaitingOther))
    }

    @Test
    fun `GIVEN a test connection to the hub WHEN it fails THEN nothing is known`() {
        assertEquals(LocalNetworkStatus.Unknown, localNetworkStatusOf(ProbeOutcome.Failed))
    }

    @Test
    fun `GIVEN a test connection to the hub WHEN nothing comes back in time THEN nothing is known`() {
        assertEquals(LocalNetworkStatus.Unknown, localNetworkStatusOf(ProbeOutcome.TimedOut))
    }
}
