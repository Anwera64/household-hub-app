package com.homelab.household.data.repository

import com.homelab.household.data.datasource.local.SecretPreferencesLocalDataSource
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** "Don't ask again" is kept per member, in a store that outlives the app and the sign-in. */
class SecretPreferencesRepositoryTest {
    private val store = MapSettings()

    private fun repository() = SecretPreferencesRepositoryImpl(SecretPreferencesLocalDataSource(store))

    @Test
    fun `GIVEN a phone nobody chose anything on WHEN Emma turns Secret Mode off THEN she is asked first`() {
        assertTrue(repository().asksBeforeTurningOff("emma"))
    }

    @Test
    fun `GIVEN Emma ticked Don't ask again WHEN the app starts again THEN she is not asked`() {
        repository().stopAskingBeforeTurningOff("emma")

        assertFalse(repository().asksBeforeTurningOff("emma"))
    }

    @Test
    fun `GIVEN Emma ticked Don't ask again WHEN Liam turns Secret Mode off on the same phone THEN he is still asked`() {
        repository().stopAskingBeforeTurningOff("emma")

        assertTrue(repository().asksBeforeTurningOff("liam"))
    }
}
