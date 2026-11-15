package com.homelab.household

import com.homelab.household.localnetwork.HubProbe
import com.homelab.household.localnetwork.ProbeOutcome
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A test connection to the hub, written in Swift with the Network framework
 * (`HouseholdHub/NetworkHubProbe.swift`): its `NWPath.unsatisfiedReason` is the only place iOS says
 * local network access was denied (Apple TN3179). Swift hands one in through [provideHubProbe].
 */
interface HubConnectionProbe {
    /** Connects to [host]:[port], reports the first outcome to [report] once, then cancels. */
    fun probe(
        host: String,
        port: Int,
        timeoutSeconds: Double,
        report: ProbeReport,
    )
}

/** How the test connection went; the first call wins. */
interface ProbeReport {
    fun ready()

    fun waiting(localNetworkDenied: Boolean)

    fun failed()

    fun timedOut()
}

/** The Swift probe, as the [HubProbe] the shared code reads. */
internal class SwiftHubProbe(
    private val swift: HubConnectionProbe,
) : HubProbe {
    override suspend fun probe(
        host: String,
        port: Int,
    ): ProbeOutcome =
        suspendCancellableCoroutine { continuation ->
            fun answer(outcome: ProbeOutcome) {
                if (continuation.isActive) continuation.resume(outcome)
            }
            swift.probe(
                host = host,
                port = port,
                timeoutSeconds = TIMEOUT_SECONDS,
                report =
                    object : ProbeReport {
                        override fun ready() = answer(ProbeOutcome.Ready)

                        override fun waiting(localNetworkDenied: Boolean) =
                            answer(
                                if (localNetworkDenied) ProbeOutcome.LocalNetworkDenied else ProbeOutcome.WaitingOther,
                            )

                        override fun failed() = answer(ProbeOutcome.Failed)

                        override fun timedOut() = answer(ProbeOutcome.TimedOut)
                    },
            )
        }

    private companion object {
        /** The same 3 s as `IosLocalNetworkAccess.PROBE_TIMEOUT`, so Swift cancels its connection too. */
        const val TIMEOUT_SECONDS = 3.0
    }
}
