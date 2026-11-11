package com.chromia.build.tools.template

import java.io.File

/**
 * Adds instructions for AI coding agents to a new project:
 * - AGENTS.md, read by Codex, Cursor and other agents
 * - CLAUDE.md, read by Claude Code; it imports AGENTS.md
 * - .cursor/rules/rell.mdc, a Cursor project rule with the same content as AGENTS.md
 */
class AgentInstructionsSupport {

    companion object {
        const val AGENTS_FILE_NAME = "AGENTS.md"
        const val CLAUDE_FILE_NAME = "CLAUDE.md"
        const val CURSOR_RULE_PATH = ".cursor/rules/rell.mdc"
        private const val RESOURCE_FOLDER = "agents"
    }

    fun addToProject(projectDir: File) {
        require(projectDir.exists() && projectDir.isDirectory) {
            "Provided path is not a valid directory: ${projectDir.absolutePath}"
        }

        val agents = readResource(AGENTS_FILE_NAME)
        projectDir.resolve(AGENTS_FILE_NAME).writeText(agents)
        projectDir.resolve(CLAUDE_FILE_NAME).writeText(readResource(CLAUDE_FILE_NAME))
        with(projectDir.resolve(CURSOR_RULE_PATH)) {
            parentFile.mkdirs()
            writeText(readResource("cursor-rule-header.mdc") + agents)
        }
    }

    private fun readResource(name: String) =
        AgentInstructionsSupport::class.java.getResource("$RESOURCE_FOLDER/$name")!!.readText()
}
