package com.homelab.household.domain.localnetwork

/** Whether this phone lets the app reach devices on the home network, the hub among them. */
enum class LocalNetworkStatus {
    /** The phone has no such permission: Android before 17, or an app that doesn't target it. */
    NotNeeded,

    Allowed,

    /** The member hasn't allowed it, or said no. Calls to a LAN address are blocked. */
    Off,

    /** The phone couldn't say in time, or answered something else. Makes no claim either way. */
    Unknown,
}
