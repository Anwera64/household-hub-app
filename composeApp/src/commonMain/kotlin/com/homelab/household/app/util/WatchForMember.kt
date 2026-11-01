package com.homelab.household.app.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Tells the secret lock when the member is here and when the app has left the screen.
 *
 * Every touch starts with a press, and a wheel scroll is its own event, so those two are what count
 * as the member being here. They are watched on the way down ([PointerEventPass.Initial]) and never
 * consumed, so whatever was touched still gets the touch. Typing has no pointer events; the composer
 * reports it itself.
 *
 * Leaving the screen is `ON_STOP`: the app going to the background, or the device locking, on
 * Android and on iOS alike.
 */
@Composable
fun WatchForMember(
    onInput: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val input by rememberUpdatedState(onInput)
    val stop by rememberUpdatedState(onStop)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stop() }
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press || event.type == PointerEventType.Scroll) input()
                    }
                }
            },
    ) {
        content()
    }
}
