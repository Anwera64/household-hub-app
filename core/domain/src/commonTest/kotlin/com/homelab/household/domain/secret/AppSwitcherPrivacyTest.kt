package com.homelab.household.domain.secret

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The picture the app switcher keeps of the app never shows an unlocked secret chat. Leaving the app
 * locks them, so the rule has to outlast that lock until the member is back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppSwitcherPrivacyTest {
    private val unlocked = MutableStateFlow(false)

    private fun TestScope.privacy(): AppSwitcherPrivacy {
        val privacy = AppSwitcherPrivacy(unlocked, backgroundScope)
        runCurrent()
        return privacy
    }

    private fun TestScope.hiddenInTheBackground(): AppSwitcherPrivacy {
        val privacy = privacy()
        unlocked.value = true
        runCurrent()
        privacy.onBackground()
        return privacy
    }

    @Test
    fun `GIVEN secret chats locked WHEN they unlock THEN the app hides from the app switcher`() =
        runTest {
            val privacy = privacy()

            unlocked.value = true
            runCurrent()

            assertTrue(privacy.hides.value)
        }

    @Test
    fun `GIVEN secret chats locked WHEN the app goes to the background THEN it does not hide`() =
        runTest {
            val privacy = privacy()

            privacy.onBackground()

            assertFalse(privacy.hides.value)
        }

    @Test
    fun `GIVEN hidden and in the background WHEN secret chats lock THEN it stays hidden`() =
        runTest {
            val privacy = hiddenInTheBackground()

            unlocked.value = false
            runCurrent()

            assertTrue(privacy.hides.value)
        }

    @Test
    fun `GIVEN hidden in the background with secret chats locked WHEN the app comes back THEN it shows again`() =
        runTest {
            val privacy = hiddenInTheBackground()
            unlocked.value = false
            runCurrent()

            privacy.onForeground()

            assertFalse(privacy.hides.value)
        }

    @Test
    fun `GIVEN hidden in the foreground WHEN secret chats lock THEN it shows again`() =
        runTest {
            val privacy = privacy()
            unlocked.value = true
            runCurrent()

            unlocked.value = false
            runCurrent()

            assertFalse(privacy.hides.value)
        }
}
