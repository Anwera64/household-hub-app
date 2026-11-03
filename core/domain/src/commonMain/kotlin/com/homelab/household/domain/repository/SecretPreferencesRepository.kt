package com.homelab.household.domain.repository

/**
 * What a member chose about Secret Mode on this phone. Kept across restarts and sign-outs, and per
 * member: someone else signing in on the same phone is asked as before.
 */
interface SecretPreferencesRepository {
    /** Whether turning Secret Mode off asks first, for [memberId] on this phone. */
    fun asksBeforeTurningOff(memberId: String): Boolean

    /** "Don't ask again": [memberId] turns Secret Mode off on this phone without the sheet from now on. */
    fun stopAskingBeforeTurningOff(memberId: String)
}
