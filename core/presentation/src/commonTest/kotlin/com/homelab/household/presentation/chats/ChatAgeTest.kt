package com.homelab.household.presentation.chats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * When a chat last moved, as its row on Chats says it (canvas Chats): "now", "10m", "3h",
 * "Yesterday", "Tue", then a date, with the year only when it isn't this one.
 */
@OptIn(ExperimentalTime::class)
class ChatAgeTest {
    // Tuesday 3 November 2026, 18:00 on a phone two hours ahead of UTC.
    private val now = Instant.parse("2026-11-03T16:00:00Z")
    private val offset = 2 * 3_600

    private fun ageOf(ago: Duration) = chatAge(now - ago, now, offset)

    @Test
    fun `GIVEN a chat that moved seconds ago WHEN its row is shown THEN it reads now`() {
        assertEquals(ChatAge.Now, ageOf(20.seconds))
    }

    @Test
    fun `GIVEN a chat that moved ten minutes ago WHEN its row is shown THEN it reads in minutes`() {
        assertEquals(ChatAge.Minutes(10), ageOf(10.minutes))
    }

    @Test
    fun `GIVEN a chat that moved earlier today WHEN its row is shown THEN it reads in hours`() {
        assertEquals(ChatAge.Hours(3), ageOf(3.hours + 20.minutes))
    }

    @Test
    fun `GIVEN a chat that moved late last night WHEN its row is shown THEN it reads yesterday`() {
        // 23:30 yesterday on the phone, though only eighteen and a half hours ago.
        assertEquals(ChatAge.Yesterday, ageOf(18.hours + 30.minutes))
    }

    @Test
    fun `GIVEN a chat that moved four days ago WHEN its row is shown THEN it reads the weekday`() {
        assertEquals(ChatAge.Weekday(isoDay = 5), ageOf(4.days))
    }

    @Test
    fun `GIVEN a chat that moved weeks ago this year WHEN its row is shown THEN it reads the day and month`() {
        assertEquals(ChatAge.Date(day = 12, month = 10, year = null), ageOf(22.days))
    }

    @Test
    fun `GIVEN a chat that moved last year WHEN its row is shown THEN the date carries the year`() {
        assertEquals(ChatAge.Date(day = 3, month = 11, year = 2025), ageOf(365.days))
    }
}
