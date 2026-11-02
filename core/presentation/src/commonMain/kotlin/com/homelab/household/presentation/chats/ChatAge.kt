package com.homelab.household.presentation.chats

import com.homelab.household.presentation.memories.civil
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** When a chat last moved, as its row says it: "now", "10m", "3h", "Yesterday", "Tue", "12 Oct". */
sealed interface ChatAge {
    data object Now : ChatAge

    data class Minutes(
        val minutes: Int,
    ) : ChatAge

    /** Earlier today on the phone. */
    data class Hours(
        val hours: Int,
    ) : ChatAge

    data object Yesterday : ChatAge

    /** Two to six days ago. [isoDay] is 1 for Monday through 7 for Sunday. */
    data class Weekday(
        val isoDay: Int,
    ) : ChatAge

    /** A week ago or more. [year] only when it isn't this one. */
    data class Date(
        val day: Int,
        val month: Int,
        val year: Int?,
    ) : ChatAge
}

/** [at] measured against [now] on a phone [offsetSeconds] from UTC. A clock running behind reads as now. */
@OptIn(ExperimentalTime::class)
fun chatAge(
    at: Instant,
    now: Instant,
    offsetSeconds: Int,
): ChatAge {
    val minutes = (now - at).inWholeMinutes
    val then = civil(at, offsetSeconds)
    val today = civil(now, offsetSeconds)
    val days = today.epochDay - then.epochDay
    return when {
        minutes < 1 -> ChatAge.Now
        minutes < MINUTES_PER_HOUR -> ChatAge.Minutes(minutes.toInt())
        days <= 0 -> ChatAge.Hours((minutes / MINUTES_PER_HOUR).toInt())
        days == 1L -> ChatAge.Yesterday
        days < WEEK -> ChatAge.Weekday(then.isoDay)
        else -> ChatAge.Date(then.day, then.month, then.year.takeIf { it != today.year })
    }
}

private const val MINUTES_PER_HOUR = 60L
private const val WEEK = 7L
