package com.homelab.household.presentation.pinentry

import com.homelab.household.domain.exception.PinLockedException
import com.homelab.household.domain.exception.ServerOfflineException
import com.homelab.household.domain.exception.WrongPinException
import com.homelab.household.domain.model.Pin
import com.homelab.household.domain.util.runCatchingSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What a PIN pad does with its keys, whatever the PIN is for: signing in, or unlocking secret chats.
 *
 * The sixth digit sends the PIN to [check]; there is no submit button. A miss clears the dots and
 * keeps the tries left, a lockout counts down and ignores the pad until it ends, and an unreachable
 * hub says so. [onAccepted] runs when [check] returns. Every change of what the pad shows goes to
 * [show]: how many digits are in, never which.
 */
class PinPadInput(
    private val scope: CoroutineScope,
    private val check: suspend (String) -> Unit,
    private val onAccepted: suspend () -> Unit,
    private val show: (entered: Int, status: PinStatus) -> Unit,
) {
    private var pin = ""
    private var status: PinStatus = PinStatus.Idle
    private var countdown: Job? = null

    fun onDigit(digit: Char) {
        if (digit !in '0'..'9' || !acceptsInput() || pin.length == Pin.LENGTH) return
        pin += digit
        show(pin.length, status)
        if (pin.length == Pin.LENGTH) submit()
    }

    fun onDelete() {
        if (!acceptsInput() || pin.isEmpty()) return
        pin = pin.dropLast(1)
        show(pin.length, status)
    }

    private fun acceptsInput(): Boolean =
        when (status) {
            PinStatus.Checking, is PinStatus.Locked -> false
            else -> true
        }

    private fun set(newStatus: PinStatus) {
        status = newStatus
        show(pin.length, status)
    }

    private fun submit() {
        val attempt = pin
        set(PinStatus.Checking)

        scope.launch {
            val result = runCatchingSafe { check(attempt) }
            if (result.isSuccess) {
                onAccepted()
                return@launch
            }

            pin = ""
            when (val error = result.exceptionOrNull()) {
                is PinLockedException -> countDown(error.retryAfterSeconds)
                is WrongPinException -> set(PinStatus.WrongPin(error.attemptsLeft))
                is ServerOfflineException -> set(PinStatus.Unreachable)
                else -> set(PinStatus.Failed)
            }
        }
    }

    private fun countDown(seconds: Int) {
        countdown?.cancel()
        countdown =
            scope.launch {
                for (left in seconds downTo 1) {
                    set(PinStatus.Locked(secondsLeft = left))
                    delay(1_000)
                }
                set(PinStatus.Idle)
            }
    }
}
