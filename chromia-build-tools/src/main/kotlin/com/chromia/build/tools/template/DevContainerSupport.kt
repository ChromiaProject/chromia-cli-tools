package com.chromia.build.tools.template

import java.io.File

class DevContainerSupport {

    companion object {
        const val DEV_CONTAINER_FOLDER_NAME = ".devcontainer"
    }

    fun addToProject(projectDir: File) {
        require(projectDir.exists() && projectDir.isDirectory) {
            "Provided path is not a valid directory: ${projectDir.absolutePath}"
        }

        with(FileBuilder(projectDir.resolve(DEV_CONTAINER_FOLDER_NAME), DEV_CONTAINER_FOLDER_NAME)) {
            moveFolder("")
        }
    }
}
