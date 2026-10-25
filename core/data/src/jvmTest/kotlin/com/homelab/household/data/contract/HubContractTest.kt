package com.homelab.household.data.contract

import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The app against the hub's contract, now that the two live in separate repositories.
 *
 * The contract is the hub's OpenAPI schema and its tool names, published to the private repo
 * household-hub-contract on every hub merge. CI downloads it and passes its folder as
 * `-Phub.contract.dir=…`; without it (a fork PR, a local run) this test is skipped.
 *
 * Three promises are checked:
 * - every request the data sources make is a route and method the hub serves;
 * - every field a DTO reads or sends is in the hub's schema for it;
 * - every tool the hub offers has words of its own in the app (ToolLabel.kt), or it shows as the
 *   generic "Used a tool" (read_page and lookup_sources did, in #36).
 *
 * The calls are read from the source, like the architecture guards read theirs, so a new call is
 * checked without anyone listing it.
 */
class HubContractTest {
    private val contractDir = System.getProperty("hub.contract.dir").orEmpty()

    private val moduleDir =
        File(System.getProperty("user.dir")).let { dir ->
            if (File(dir, "src/commonMain").exists()) dir else File(dir, "core/data")
        }
    private val rootDir = moduleDir.parentFile.parentFile
    private val dataSources = File(moduleDir, "src/commonMain/kotlin/com/homelab/household/data")

    private fun read(file: File): String = file.readText().replace("\r\n", "\n")

    private val contract by lazy {
        assumeTrue(contractDir.isNotBlank(), "no hub.contract.dir: the contract is checked in CI")
        HubContract(read(File(contractDir, "openapi.json")), read(File(contractDir, "tools.json")))
    }

    private val sources by lazy {
        listOf("datasource/remote", "network")
            .flatMap { File(dataSources, it).walkTopDown().filter { f -> f.extension == "kt" }.toList() }
            .associateWith { read(it) }
    }

    /** `const val SOURCES_PATH = "/api/v1/…"` and its kin, so `"$baseUrl$SOURCES_PATH"` resolves. */
    private val pathConstants by lazy {
        sources.values
            .flatMap { Regex("""const val ([A-Z_]+) = "(/api/[^"]*)"""").findAll(it).toList() }
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    /** Every `.get("$baseUrl/api/v1/…")`, `.preparePost(…)` and so on in the data sources. */
    private val calls by lazy {
        val dollar = '$'
        val call =
            Regex(
                """\.(get|post|put|patch|delete|prepareGet|preparePost|preparePut|preparePatch|prepareDelete)\(\s*"\${dollar}baseUrl([^"]*)"""",
            )
        sources.values
            .flatMap { call.findAll(it).toList() }
            .map { match ->
                val method = match.groupValues[1].removePrefix("prepare").lowercase()
                val raw = match.groupValues[2]
                val path = pathConstants.entries.fold(raw) { acc, (name, value) -> acc.replace("$$name", value) }
                ApiCall(method, normalisePath(path))
            }.toSet()
    }

    /**
     * The hub's name for each DTO's schema, where it is not the DTO's name without "Dto" or with
     * "Read" instead.
     */
    private val schemaFor =
        mapOf(
            "ChangeAnswerDto" to "ProposalAnswer",
            "GoogleSignInStartDto" to "GoogleSignInStartResponse",
            "InviteCreateRequestDto" to "InviteCreate",
            "InviteRedeemRequestDto" to "InviteRedeem",
            "PinResetApproveRequestDto" to "PinResetApprove",
            "PinResetRedeemRequestDto" to "PinResetRedeem",
            "SpaceUpdateDto" to "SpaceSettingsUpdate",
            "TokenResponseDto" to "Token",
            "UserLoginRequestDto" to "LoginRequest",
            "UserOnboardRequestDto" to "FirstRunRegister",
        )

    /** DTOs the hub's schema says nothing about, and why. Each new DTO must be mapped or listed here. */
    private val unchecked =
        mapOf(
            "HealthCheckDto" to "the hub's /health answers an untyped object",
            "PinRefusalDto" to "an error body, which FastAPI does not describe",
            "ToolApprovalResponseDto" to "the decision endpoint answers an untyped object",
        )

    private val dtoNames by lazy {
        File(dataSources, "dto")
            .walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap {
                Regex("""@Serializable\s+(?:data |enum )?class (\w+)""").findAll(read(it)).map { m ->
                    m.groupValues[1]
                }
            }
            .toSortedSet()
    }

    @Test
    fun `GIVEN the data sources WHEN their requests are checked against the hub THEN it serves every route and method`() {
        assertTrue(calls.size >= 40, "the scan should find the data sources' calls, found ${calls.size}")
        assertEquals(emptyList<ApiCall>(), contract.missingCalls(calls.sortedBy { it.path }))
    }

    @Test
    fun `GIVEN the DTOs WHEN their fields are checked against the hub schemas THEN none is missing`() {
        val fieldsBySchema =
            (dtoNames - unchecked.keys).associate { name ->
                val descriptor = serializer(Class.forName("com.homelab.household.data.dto.$name")).descriptor
                val base = name.removeSuffix("Dto")
                val schema = schemaFor[name] ?: contract.schemaNamed(base, "${base}Read") ?: base
                schema to (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }
            }
        assertEquals(emptyList<String>(), contract.fieldGaps(fieldsBySchema))
    }

    @Test
    fun `GIVEN the hub tools WHEN the app labels are read THEN every tool has words of its own`() {
        val labels =
            read(
                File(
                    rootDir,
                    "composeApp/src/commonMain/kotlin/com/homelab/household/app/screens/conversation/ToolLabel.kt",
                ),
            )
        val hubTool =
            read(File(rootDir, "core/domain/src/commonMain/kotlin/com/homelab/household/domain/model/HubTool.kt"))
        // The labels name each tool through HubTool, the one place the app spells the hub's names.
        val named = Regex("""const val ([A-Z_]+) = "([a-z_]+)"""").findAll(hubTool).associate {
            it.groupValues[1] to
                it.groupValues[2]
        }
        val labelled =
            Regex("""^\s*"([a-z_]+)" to$""", RegexOption.MULTILINE).findAll(labels).map { it.groupValues[1] }.toSet() +
                Regex("""^\s*HubTool\.([A-Z_]+) to$""", RegexOption.MULTILINE).findAll(labels).mapNotNull {
                    named[it.groupValues[1]]
                }
        assertEquals(emptyList<String>(), contract.unlabelledTools(labelled))
    }
}
