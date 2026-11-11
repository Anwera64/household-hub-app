package com.homelab.household.domain.localnetwork

/** What the app draws about local network access, over or instead of the screen underneath. */
enum class LocalNetworkScreen {
    /** The app hasn't decided yet at start. Nothing is drawn, and nothing reaches the hub. */
    Starting,

    Hidden,

    /** The explanation before the phone's own question. It replaces the app until Continue. */
    FirstTime,

    /** Access is off, and a hub call just found no route. Drawn over the open screen. */
    Off,

    /** Back in the app with the cover up: reading access again. */
    Checking,

    /** Back in the app, and access is still off. */
    StillOff,
}
