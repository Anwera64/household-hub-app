package com.homelab.household.domain.localnetwork

/**
 * The phone's own local network permission, read per platform. Android reads it from the package
 * manager. iOS has no API for it (Apple TN3179), so it tries a connection to the hub and reads why
 * it waits.
 */
interface LocalNetworkAccess {
    /** Asks the phone. On iOS this touches the network, so it takes up to a few seconds. */
    suspend fun status(): LocalNetworkStatus

    /**
     * Whether [status] can be read without the phone asking the member. True on Android. False on
     * iOS, where the test connection itself would bring up iOS's question before the app explains.
     */
    val readsWithoutAsking: Boolean
}
