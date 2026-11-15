package com.homelab.household.data.repository

import com.homelab.household.data.datasource.local.LocalNetworkPreferencesLocalDataSource
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What the phone remembers about local network access outlives the app. */
class LocalNetworkRepositoryTest {
    private val store = MapSettings()

    private fun repository() = LocalNetworkRepositoryImpl(LocalNetworkPreferencesLocalDataSource(store))

    @Test
    fun `GIVEN a new install WHEN the app reads whether it explained local network access THEN it has not`() {
        assertFalse(repository().explained())
    }

    @Test
    fun `GIVEN a new install WHEN the app reads whether the member stopped it asking THEN they have not`() {
        assertFalse(repository().stoppedAsking())
    }

    @Test
    fun `GIVEN the explanation was shown WHEN the app starts again THEN it remembers`() {
        repository().markExplained()

        assertTrue(repository().explained())
    }

    @Test
    fun `GIVEN the member chose Don't ask again WHEN the app starts again THEN it remembers`() {
        repository().stopAsking()

        assertTrue(repository().stoppedAsking())
    }
}
