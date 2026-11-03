package com.homelab.household.app.screens.chats

import androidx.compose.runtime.Composable
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.chats_age_date
import com.homelab.household.app.resources.chats_age_date_year
import com.homelab.household.app.resources.chats_age_hours
import com.homelab.household.app.resources.chats_age_minutes
import com.homelab.household.app.resources.chats_age_now
import com.homelab.household.app.resources.chats_age_yesterday
import com.homelab.household.app.screens.conversation.MONTHS
import com.homelab.household.app.screens.conversation.WEEKDAYS
import com.homelab.household.presentation.chats.ChatAge
import org.jetbrains.compose.resources.stringResource

/** When a chat last moved, in the few letters its row has room for. Nothing when the hub sent no time. */
@Composable
fun chatAgeText(age: ChatAge?): String =
    when (age) {
        null -> {
            ""
        }

        ChatAge.Now -> {
            stringResource(Res.string.chats_age_now)
        }

        is ChatAge.Minutes -> {
            stringResource(Res.string.chats_age_minutes, age.minutes)
        }

        is ChatAge.Hours -> {
            stringResource(Res.string.chats_age_hours, age.hours)
        }

        ChatAge.Yesterday -> {
            stringResource(Res.string.chats_age_yesterday)
        }

        is ChatAge.Weekday -> {
            stringResource(WEEKDAYS[age.isoDay - 1])
        }

        is ChatAge.Date -> {
            val month = stringResource(MONTHS[age.month - 1])
            age.year?.let { stringResource(Res.string.chats_age_date_year, age.day, month, it) }
                ?: stringResource(Res.string.chats_age_date, age.day, month)
        }
    }
