package com.homelab.household.app.util

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.homelab.household.app.testing.StillTheme
import com.homelab.household.app.testing.runScreenTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The secret lock hears the member's own touches and the app leaving the screen, and nothing else.
 * Watching must never take a tap away from what was tapped.
 */
@OptIn(ExperimentalTestApi::class)
class WatchForMemberTest {
    private class ScreenLifecycle : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle = registry
    }

    @Test
    fun `GIVEN a watched screen WHEN the member taps a button THEN the touch is reported and the button still gets it`() =
        runScreenTest {
            var touches = 0
            var clicks = 0

            setContent {
                StillTheme {
                    WatchForMember(onInput = { touches++ }, onStop = {}) {
                        Button(onClick = { clicks++ }) { Text("Send") }
                    }
                }
            }
            onWatchedScreen { taps("Send") }

            assertEquals(1, touches)
            assertEquals(1, clicks)
        }

    @Test
    fun `GIVEN a watched screen WHEN the member swipes THEN the touch is reported`() =
        runScreenTest {
            var touches = 0

            setContent {
                StillTheme {
                    WatchForMember(onInput = { touches++ }, onStop = {}) {
                        Column { Text("Messages") }
                    }
                }
            }
            onWatchedScreen { swipesUpOn("Messages") }

            assertEquals(1, touches)
        }

    @Test
    fun `GIVEN a watched screen WHEN the app leaves the screen THEN that is reported once`() =
        runScreenTest {
            val screen = ScreenLifecycle()
            var stops = 0

            setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides screen) {
                    StillTheme {
                        WatchForMember(onInput = {}, onStop = { stops++ }) { Text("Chats") }
                    }
                }
            }
            runOnIdle { screen.registry.currentState = Lifecycle.State.CREATED }
            waitForIdle()

            assertEquals(1, stops)
        }
}
