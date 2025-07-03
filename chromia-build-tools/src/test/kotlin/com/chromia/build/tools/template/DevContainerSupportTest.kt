package com.chromia.build.tools.template

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.exists
import assertk.assertions.isDirectory
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class DevContainerSupportTest {
    private val devContainerSupport = DevContainerSupport()

    @Test
    fun `should throw exception when project directory does not exist`() {
        val nonExistentDir = File("/non/existent/path")

        val exception = assertThrows<IllegalArgumentException> {
            devContainerSupport.addToProject(nonExistentDir)
        }

        assertThat(
            exception.message
        ).isEqualTo("Provided path is not a valid directory: ${nonExistentDir.absolutePath}")
    }

    @Test
    fun `should throw exception when project path is a file not a directory`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("testfile.txt").toFile()
        file.createNewFile()

        val exception = assertThrows<IllegalArgumentException> {
            devContainerSupport.addToProject(file)
        }

        assertThat(exception.message).isEqualTo("Provided path is not a valid directory: ${file.absolutePath}")
    }

    @Test
    fun `should successfully create devcontainer files when valid directory provided`(@TempDir tempDir: Path) {
        val projectDir = tempDir.toFile()
        val devContainerFolder = tempDir.resolve(DevContainerSupport.DEV_CONTAINER_FOLDER_NAME)

        assertThat(devContainerFolder.exists()).isFalse()

        devContainerSupport.addToProject(projectDir)

        assertThat(devContainerFolder).exists()
        assertThat(devContainerFolder).isDirectory()

        val createdFiles = devContainerFolder.listDirectoryEntries().map { it.name }
        assertThat(createdFiles).containsExactlyInAnyOrder(
            "devcontainer.json",
            "Dockerfile",
            "warn_once.sh",
        )
    }
}
