package com.homelab.household.data.network

import com.homelab.household.data.datasource.local.HubEventsLocalDataSource
import com.homelab.household.domain.util.runCatchingSafe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A call that finds no route to the hub is announced, so the app can tell whether the phone's local
 * network access is what blocked it. A hub that answers, however badly, is not unreachable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportUnreachableTest {
    private val hub = "https://hub.test.local"
    private val events = HubEventsLocalDataSource()

    private fun client(handler: MockRequestHandler) =
        HttpClient(MockEngine(handler)) {
            reportUnreachable(events::raiseUnreachable)
        }

    @Test
    fun `GIVEN a hub nothing answers WHEN a call times out connecting THEN it is announced once`() =
        runTest {
            val seen = mutableListOf<Unit>()
            backgroundScope.launch { events.observeUnreachable().collect { seen += it } }
            runCurrent()

            runCatchingSafe {
                client { throw ConnectTimeoutException("connect timed out") }.get("$hub/api/v1/auth/status")
            }
            runCurrent()

            assertEquals(1, seen.size)
        }

    @Test
    fun `GIVEN a hub that answers 500 WHEN a call is made THEN nothing is announced`() =
        runTest {
            val seen = mutableListOf<Unit>()
            backgroundScope.launch { events.observeUnreachable().collect { seen += it } }
            runCurrent()

            client { respond("down", HttpStatusCode.InternalServerError) }.get("$hub/api/v1/spaces/shared")
            runCurrent()

            assertTrue(seen.isEmpty())
        }

    @Test
    fun `GIVEN a hub that answers 401 WHEN a call is made THEN nothing is announced`() =
        runTest {
            val seen = mutableListOf<Unit>()
            backgroundScope.launch { events.observeUnreachable().collect { seen += it } }
            runCurrent()

            client { respond("no", HttpStatusCode.Unauthorized) }.get("$hub/api/v1/spaces/shared")
            runCurrent()

            assertTrue(seen.isEmpty())
        }

    @Test
    fun `GIVEN a call already found no route WHEN someone starts listening later THEN it is not replayed`() =
        runTest {
            runCatchingSafe {
                client { throw ConnectTimeoutException("connect timed out") }.get("$hub/api/v1/auth/status")
            }
            val seen = mutableListOf<Unit>()

            backgroundScope.launch { events.observeUnreachable().collect { seen += it } }
            runCurrent()

            assertTrue(seen.isEmpty())
        }
}
