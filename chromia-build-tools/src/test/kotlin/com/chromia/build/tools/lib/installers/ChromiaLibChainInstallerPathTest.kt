package com.chromia.build.tools.lib.installers

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ChromiaLibChainInstallerPathTest {

    @TempDir
    private lateinit var baseDir: Path

    @Test
    fun `normal library file paths resolve inside the installation directory`() {
        val base = baseDir.toAbsolutePath().normalize()
        assertThat(ChromiaLibChainInstaller.resolveInside(baseDir, "module.rell"))
                .isEqualTo(base.resolve("module.rell"))
        assertThat(ChromiaLibChainInstaller.resolveInside(baseDir, "src/lib/ft4/module.rell"))
                .isEqualTo(base.resolve("src/lib/ft4/module.rell"))
        assertThat(ChromiaLibChainInstaller.resolveInside(baseDir, "src/./a/../b.rell"))
                .isEqualTo(base.resolve("src/b.rell"))
    }

    @Test
    fun `paths that climb out of the installation directory are rejected`() {
        listOf("../target.rell", "../../target.rell", "src/../../target.rell", "..", ".").forEach { path ->
            assertThrows<IllegalArgumentException>(path) {
                ChromiaLibChainInstaller.resolveInside(baseDir, path)
            }
        }
    }

    @Test
    fun `absolute and empty paths are rejected`() {
        val absolute = baseDir.toAbsolutePath().resolve("x.rell").toString()
        listOf(absolute, "/etc/target.rell", "", " ").forEach { path ->
            assertThrows<IllegalArgumentException>(path) {
                ChromiaLibChainInstaller.resolveInside(baseDir, path)
            }
        }
    }
}
