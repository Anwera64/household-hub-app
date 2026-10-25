package com.homelab.household.data.contract

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One request the app makes: a lowercase HTTP method and a path with every parameter as `{}`. */
data class ApiCall(
    val method: String,
    val path: String,
)

/** Turns `/sessions/{session_id}` and `/sessions/$sessionId` alike into `/sessions/{}`. */
fun normalisePath(path: String): String =
    path
        .substringBefore('?')
        .replace(Regex("""\{[^}]*}"""), "{}")
        .replace(Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_.]*"""), "{}")

/**
 * The hub's contract with the app, as household-hub-contract publishes it: the OpenAPI schema
 * (openapi.json) and the names of the tools agents can call (tools.json).
 */
class HubContract(
    openApiJson: String,
    toolsJson: String,
) {
    private val openApi: JsonObject = Json.parseToJsonElement(openApiJson).jsonObject
    private val tools: List<String> = Json.parseToJsonElement(toolsJson).jsonArray.map { it.jsonPrimitive.content }

    private val served: Set<ApiCall> =
        openApi["paths"]
            ?.jsonObject
            .orEmpty()
            .flatMap { (path, operations) ->
                operations.jsonObject.keys.map { ApiCall(it.lowercase(), normalisePath(path)) }
            }
            .toSet()

    private val schemas: JsonObject =
        openApi["components"]?.jsonObject?.get("schemas")?.jsonObject ?: JsonObject(emptyMap())

    /** The calls the hub does not serve, in the order given. */
    fun missingCalls(calls: Collection<ApiCall>): List<ApiCall> =
        calls.filter {
            it.copy(path = normalisePath(it.path)) !in
                served
        }

    /** "Schema.field" for every field the app reads or sends that the hub's schema lacks. */
    fun fieldGaps(fieldsBySchema: Map<String, List<String>>): List<String> =
        fieldsBySchema.flatMap { (schema, fields) ->
            val properties = schemas[schema]?.jsonObject?.get("properties")?.jsonObject
            if (properties == null) {
                listOf("$schema (no such schema)")
            } else {
                fields.filter { it !in properties }.map { "$schema.$it" }
            }
        }

    /** The first of [names] the hub has a schema for, or null. */
    fun schemaNamed(vararg names: String): String? = names.firstOrNull { it in schemas }

    /** The hub's tools that have no label in the app. */
    fun unlabelledTools(labelled: Set<String>): List<String> = tools.filter { it !in labelled }
}
