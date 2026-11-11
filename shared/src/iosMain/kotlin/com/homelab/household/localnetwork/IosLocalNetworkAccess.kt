package com.homelab.household.localnetwork

import com.homelab.household.domain.localnetwork.LocalNetworkAccess
import com.homelab.household.domain.localnetwork.LocalNetworkStatus
import io.ktor.http.Url
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** A test connection to the hub. The app supplies one built on the Network framework. */
fun interface HubProbe {
    suspend fun probe(
        host: String,
        port: Int,
    ): ProbeOutcome
}

/**
 * iOS has no API to read its local network permission (Apple TN3179). A denial only shows as a
 * connection that waits with `localNetworkDenied`, so this tries one to the hub and reads that.
 * Touching the network is also what makes iOS ask, so this is never read before the app explains
 * ([readsWithoutAsking] is false). Over a VPN the traffic isn't local, so the probe is ready.
 */
class IosLocalNetworkAccess(
    private val hubBaseUrl: String,
    private val probe: HubProbe,
) : LocalNetworkAccess {
    override val readsWithoutAsking: Boolean = false

    override suspend fun status(): LocalNetworkStatus {
        val hub = Url(hubBaseUrl)
        val outcome = withTimeoutOrNull(PROBE_TIMEOUT) { probe.probe(hub.host, hub.port) } ?: ProbeOutcome.TimedOut
        return localNetworkStatusOf(outcome)
    }

    companion object {
        /** How long the test connection may take before it gives up and says nothing (#5). */
        val PROBE_TIMEOUT = 3.seconds
    }
}
