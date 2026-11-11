package com.chromia.build.tools.template

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import java.nio.file.Path

class AgentInstructionsSupportTest {

    @Test
    fun `should throw exception when project directory does not exist`() {
        val nonExistentDir = File("/non/existent/path")
        val exception = assertThrows<IllegalArgumentException> {
            AgentInstructionsSupport().addToProject(nonExistentDir)
        }
        assertThat(exception.message).isEqualTo("Provided path is not a valid directory: ${nonExistentDir.absolutePath}")
    }

    @Test
    fun `should write AGENTS md, CLAUDE md and the cursor rule`(@TempDir tempDir: Path) {
        val projectDir = tempDir.toFile()
        AgentInstructionsSupport().addToProject(projectDir)

        val agents = projectDir.resolve("AGENTS.md").readText()
        assertThat(agents).contains("chr build")
        assertThat(agents).contains("chr test")
        assertThat(agents).contains("@omit @sort_desc")
        assertThat(agents).contains("struct module_args")

        assertThat(projectDir.resolve("CLAUDE.md").readText()).contains("@AGENTS.md")

        val rule = projectDir.resolve(".cursor/rules/rell.mdc").readText()
        assertThat(rule).startsWith("---\n")
        assertThat(rule).contains("alwaysApply: true")
        assertThat(rule).endsWith(agents)
    }

    @ParameterizedTest
    @EnumSource(TemplateProject::class)
    fun `every template gets agent instructions`(template: TemplateProject, @TempDir tempDir: Path) {
        val projectDir = tempDir.resolve("my-dapp").toFile().apply { mkdirs() }
        TemplateFactoryProvider.getFactory(template).createProjectFromTemplate(projectDir, "my-dapp", TemplateOptions(false))

        assertThat(projectDir.resolve("AGENTS.md").isFile).isTrue()
        assertThat(projectDir.resolve("CLAUDE.md").isFile).isTrue()
        assertThat(projectDir.resolve(".cursor/rules/rell.mdc").isFile).isTrue()
    }
}
