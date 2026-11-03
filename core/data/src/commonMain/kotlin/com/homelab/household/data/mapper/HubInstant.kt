package com.homelab.household.data.mapper

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** The hub's SQLite hands times back without a zone; every one it writes is UTC. */
@OptIn(ExperimentalTime::class)
internal fun hubInstant(iso: String): Instant {
    val zoned = ZONE.containsMatchIn(iso)
    return Instant.parse(if (zoned) iso else "${iso}Z")
}

private val ZONE = Regex("""(Z|[+-]\d{2}:?\d{2})$""")
