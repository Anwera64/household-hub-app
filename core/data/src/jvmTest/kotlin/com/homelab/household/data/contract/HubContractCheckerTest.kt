package com.homelab.household.data.contract

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The checker behind [HubContractTest], on small contracts written here: it must name exactly
 * what the app uses and the hub does not have.
 */
class HubContractCheckerTest {
    private val openApi =
        """
        {
          "openapi": "3.1.0",
          "paths": {
            "/api/v1/spaces/personal": { "get": {} },
            "/api/v1/sessions/{session_id}": { "get": {}, "delete": {} },
            "/api/v1/sessions/{session_id}/tools/{tool_call_id}/decision": { "post": {} }
          },
          "components": {
            "schemas": {
              "SpaceRead": { "properties": { "id": {}, "name": {}, "owner_id": {} } },
              "Token": { "properties": { "access_token": {}, "token_type": {} } }
            }
          }
        }
        """.trimIndent()
    private val contract = HubContract(openApi, """["calendar_read", "read_page"]""")

    @Test
    fun `GIVEN calls the hub serves WHEN checked THEN nothing is missing whatever the parameters are called`() {
        val calls =
            setOf(
                ApiCall("get", "/api/v1/spaces/personal"),
                ApiCall("delete", "/api/v1/sessions/{}"),
                ApiCall("post", "/api/v1/sessions/{}/tools/{}/decision"),
            )

        assertEquals(emptyList<ApiCall>(), contract.missingCalls(calls))
    }

    @Test
    fun `GIVEN a route or a method the hub lacks WHEN checked THEN each is named`() {
        val calls =
            setOf(
                ApiCall("get", "/api/v1/spaces/personal"),
                ApiCall("get", "/api/v1/spaces/shared"),
                ApiCall("patch", "/api/v1/sessions/{}"),
            )

        assertEquals(
            listOf(ApiCall("get", "/api/v1/spaces/shared"), ApiCall("patch", "/api/v1/sessions/{}")),
            contract.missingCalls(calls),
        )
    }

    @Test
    fun `GIVEN a DTO field missing from the hub schema WHEN checked THEN the field is named with its schema`() {
        val gaps =
            contract.fieldGaps(
                mapOf(
                    "SpaceRead" to listOf("id", "name", "created_at"),
                    "Token" to listOf("access_token"),
                ),
            )

        assertEquals(listOf("SpaceRead.created_at"), gaps)
    }

    @Test
    fun `GIVEN a schema name the hub does not have WHEN checked THEN the schema is named`() {
        assertEquals(listOf("Gone (no such schema)"), contract.fieldGaps(mapOf("Gone" to listOf("id"))))
    }

    @Test
    fun `GIVEN a hub tool without a label in the app WHEN checked THEN the tool is named`() {
        assertEquals(listOf("read_page"), contract.unlabelledTools(setOf("calendar_read", "remember")))
    }

    @Test
    fun `GIVEN every hub tool labelled WHEN checked THEN nothing is missing`() {
        assertEquals(emptyList<String>(), contract.unlabelledTools(setOf("calendar_read", "read_page")))
    }
}
