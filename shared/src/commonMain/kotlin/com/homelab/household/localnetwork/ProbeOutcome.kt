package com.homelab.household.localnetwork

import com.homelab.household.domain.localnetwork.LocalNetworkStatus

/**
 * How a test connection to the hub went, on iOS. iOS has no API for its local network permission
 * (Apple TN3179): a denial only shows as a connection waiting with `localNetworkDenied`.
 */
enum class ProbeOutcome {
    Ready,
    LocalNetworkDenied,
    WaitingOther,
    Failed,
    TimedOut,
}

/** Only a denial says access is off; anything but ready or denied makes no claim. */
fun localNetworkStatusOf(outcome: ProbeOutcome): LocalNetworkStatus =
    when (outcome) {
        ProbeOutcome.Ready -> LocalNetworkStatus.Allowed
        ProbeOutcome.LocalNetworkDenied -> LocalNetworkStatus.Off
        ProbeOutcome.WaitingOther, ProbeOutcome.Failed, ProbeOutcome.TimedOut -> LocalNetworkStatus.Unknown
    }
