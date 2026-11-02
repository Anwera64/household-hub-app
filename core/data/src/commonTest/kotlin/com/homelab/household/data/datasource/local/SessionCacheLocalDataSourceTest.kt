package com.homelab.household.data.datasource.local

import app.cash.turbine.test
import com.homelab.household.data.dto.ChatMessageReadDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The state the chat screens share between coroutines, in one place small enough to read at once.
 * It used to be two plain mutable collections inside a 266-line repository, mutated from whichever
 * coroutine happened to be streaming — which is what these tests exist to make impossible.
 */
class SessionCacheLocalDataSourceTest {
    private fun message(
        id: String,
        sessionId: String,
        content: String = "Hello",
    ) = ChatMessageReadDto(id = id, session_id = sessionId, role = "user", content = content)

    // ---- messages ----------------------------------------------------------

    @Test
    fun `GIVEN nothing cached for a session WHEN its messages are observed THEN the conversation reads as empty`() =
        runTest {
            val cache = SessionCacheLocalDataSource()

            val messages = cache.observeMessages("s-1")

            messages.test {
                assertEquals(emptyList(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a session being observed WHEN its messages are cached THEN the observer sees them`() =
        runTest {
            val cache = SessionCacheLocalDataSource()

            cache.observeMessages("s-1").test {
                assertEquals(emptyList(), awaitItem())

                cache.cacheMessages("s-1", listOf(message("m-1", "s-1")))

                assertEquals(listOf("m-1"), awaitItem().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN two sessions cached WHEN one is observed THEN the other session's messages never reach it`() =
        runTest {
            val cache = SessionCacheLocalDataSource()
            cache.cacheMessages("s-1", listOf(message("m-1", "s-1")))

            cache.cacheMessages("s-2", listOf(message("m-2", "s-2")))

            cache.observeMessages("s-1").test {
                assertEquals(listOf("m-1"), awaitItem().map { it.id })
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a message cached against a session WHEN it is looked up by id THEN the session holding it is found`() =
        runTest {
            val cache = SessionCacheLocalDataSource()
            cache.cacheMessages("s-7", listOf(message("m-9", "s-7", content = "What's for dinner?")))

            val found = cache.sessionHolding("m-9")

            assertEquals("s-7", found?.first)
            assertEquals("What's for dinner?", found?.second?.content)
        }

    @Test
    fun `GIVEN a message id nothing has cached WHEN it is looked up THEN nothing is found`() =
        runTest {
            val cache = SessionCacheLocalDataSource()
            cache.cacheMessages("s-1", listOf(message("m-1", "s-1")))

            val found = cache.sessionHolding("m-does-not-exist")

            assertNull(found)
        }

    /**
     * Twenty sessions cached at once from twenty coroutines. Against the `mutableMapOf` this
     * replaced, a concurrent write could drop an entry; the point of the state flow is that this
     * cannot happen, and the point of this test is that nobody quietly puts the map back.
     */
    @Test
    fun `GIVEN many sessions cached at the same time WHEN every write has landed THEN none of them was lost`() =
        runTest {
            val cache = SessionCacheLocalDataSource()
            val sessionIds = (1..20).map { "s-$it" }

            sessionIds.map { id -> async { cache.cacheMessages(id, listOf(message("m-$id", id))) } }.awaitAll()

            assertEquals(sessionIds.toSet(), sessionIds.filter { cache.sessionHolding("m-$it") != null }.toSet())
        }
}
