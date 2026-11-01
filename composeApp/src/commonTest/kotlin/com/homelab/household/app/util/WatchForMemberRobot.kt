package com.homelab.household.app.util

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp

/** The member's touches on a watched screen: a tap on a button, a swipe through what is drawn. */
@OptIn(ExperimentalTestApi::class)
class WatchForMemberRobot(
    private val test: ComposeUiTest,
) {
    fun taps(label: String) {
        test.onNodeWithText(label).performClick()
        test.waitForIdle()
    }

    fun swipesUpOn(text: String) {
        test.onNodeWithText(text).performTouchInput { swipeUp() }
        test.waitForIdle()
    }
}

@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.onWatchedScreen(block: suspend WatchForMemberRobot.() -> Unit) {
    WatchForMemberRobot(this).block()
}
