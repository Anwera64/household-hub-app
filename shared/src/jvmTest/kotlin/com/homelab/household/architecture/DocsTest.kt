package com.homelab.household.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The docs live in the repo's GitHub wiki; the README stays short and points there. The repo and its
 * wiki are public, so the README names no home network address, and no source points at a docs
 * file the repo doesn't have.
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
    fun `GIVEN the README WHEN it is read THEN it stays under 60 lines and links the wiki`() {
        val lines = readme.lines().size
        assertTrue(lines <= 60, "the README has $lines lines; the detail belongs in the wiki")
        assertTrue(readme.contains(WIKI), "the README must link the wiki")
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

        const val WIKI = "https://github.com/Anwera64/household-hub-app/wiki"
    }
}
