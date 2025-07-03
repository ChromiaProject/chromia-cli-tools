package com.chromia.build.tools.template

import java.io.File

interface TemplateFactory {
    fun createProjectFromTemplate(targetDir: File, projectName: String, options: TemplateOptions? = null)
}

data class TemplateOptions(
    val includeDevContainer: Boolean = false
)
