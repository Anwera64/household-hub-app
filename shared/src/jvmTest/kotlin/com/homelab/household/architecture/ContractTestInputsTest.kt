package com.homelab.household.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The contract test reads the hub's contract from the folder in `-Phub.contract.dir`. Gradle knows
 * only what a task declares: with just the path as a system property, a changed contract in the same
 * folder left the test up to date, or brought its old pass back from the build cache, and a broken
 * contract passed (the backend's app-contract job, 2026-10-25).
 */
class ContractTestInputsTest {
    private val clientRootDir =
        File(System.getProperty("user.dir")).let { dir ->
            if (dir.name == "shared") dir.parentFile else dir
        }

    private val dataBuild by lazy { File(clientRootDir, "core/data/build.gradle.kts").readText().replace("\r\n", "\n") }

    @Test
    fun `GIVEN a contract folder WHEN the tests are configured THEN its files are an input of the test task`() {
        assertTrue(
            dataBuild.contains(
                """inputs.dir(dir).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("hubContract")""",
            ),
            "core/data/build.gradle.kts must declare the hub.contract.dir folder with inputs.dir",
        )
    }
}
