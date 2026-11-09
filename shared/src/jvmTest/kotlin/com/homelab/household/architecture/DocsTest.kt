package com.homelab.household.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The docs live in a private wiki; the README stays short and doesn't mention it, and nothing links
 * the GitHub wiki, which is off. The repo is public, so the README names no home network address,
 * and no source points at a docs file the repo doesn't have.
 */
class DocsTest {
    private val clientRootDir =
        File(System.getProperty("user.dir")).let { dir ->
            if (dir.name == "shared") dir.parentFile else dir
        }

    private val readme by lazy { File(clientRootDir, "README.md").readText().replace("\r\n", "\n") }

    private val sources by lazy {
        clientRootDir
            .walkTopDown()
            .onEnter { it.name !in setOf("build", ".gradle", ".git", ".idea", ".kotlin", "node_modules") }
            .filter { it.isFile && it.extension in setOf("kt", "kts", "md", "yml", "swift") }
            .toList()
    }

    @Test
    fun `GIVEN the README WHEN it is read THEN it stays under 60 lines and leaves the wiki out`() {
        val lines = readme.lines().size
        assertTrue(lines <= 60, "the README has $lines lines; the detail belongs in the wiki")
        assertFalse(readme.contains("wiki", ignoreCase = true), "the wiki is private; the README must not mention it")
    }

    @Test
    fun `GIVEN the repo's sources WHEN they are scanned THEN none links the GitHub wiki`() {
        val linking =
            sources
                .filterNot { it.name == "DocsTest.kt" }
                .filter { it.readText().contains(GITHUB_WIKI) }
                .map { it.relativeTo(clientRootDir).path }
        assertEquals(emptyList<String>(), linking)
    }

    @Test
    fun `GIVEN the repo's sources WHEN they are scanned THEN none names a docs file that is not in the repo`() {
        val docsFile = Regex("""docs/[A-Za-z0-9_-]+\.md""")
        val missing =
            sources.flatMap { file ->
                docsFile
                    .findAll(file.readText())
                    .map { it.value }
                    .filterNot { File(clientRootDir, it).exists() }
                    .map { "${file.relativeTo(clientRootDir)}: $it" }
                    .toList()
            }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun `GIVEN the README WHEN it is read THEN it names no private network address`() {
        val private = Regex("""\b192\.168\.\d+\.\d+|\b[a-z0-9-]+\.lan\b""")
        assertEquals(emptyList<String>(), private.findAll(readme).map { it.value }.toList())
    }

    @Test
    fun `GIVEN the agent skills WHEN they are read THEN each names and describes itself`() {
        for (skill in SKILLS) {
            val header = Regex("^---\nname: $skill\ndescription: .+\n---\n")
            assertTrue(header.containsMatchIn(skillText(skill)), "$skill needs its header")
        }
    }

    @Test
    fun `GIVEN the agent skills WHEN they are read THEN none names a home address or domain`() {
        val home = Regex("""\b192\.168\.|\.lan\b|duckdns|spicy-llama|ssh """)
        val found = SKILLS.flatMap { skill -> home.findAll(skillText(skill)).map { "$skill: ${it.value}" }.toList() }
        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun `GIVEN AGENTS md WHEN it is read THEN it names the skills`() {
        val agents = File(clientRootDir, "AGENTS.md").readText()
        for (skill in SKILLS) assertTrue(agents.contains("`$skill`"), "AGENTS.md must name `$skill`")
    }

    private fun skillText(skill: String): String =
        File(clientRootDir, ".claude/skills/$skill/SKILL.md").readText().replace("\r\n", "\n")

    private companion object {
        val SKILLS = listOf("patch-release", "test-pr")

        const val GITHUB_WIKI = "github.com/Anwera64/household-hub-app/wiki"
    }
}
