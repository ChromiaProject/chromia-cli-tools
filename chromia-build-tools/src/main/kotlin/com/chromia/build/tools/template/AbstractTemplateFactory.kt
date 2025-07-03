package com.chromia.build.tools.template

import java.io.File

abstract class AbstractTemplateFactory : TemplateFactory {

    override fun createProjectFromTemplate(targetDir: File, projectName: String, options: TemplateOptions?) {
        options?.let { options ->
            if (options.includeDevContainer) {
                DevContainerSupport().addToProject(targetDir)
            }
        }
        createProjectFiles(targetDir, projectName, options)
    }

    abstract fun createProjectFiles(targetDir: File, projectName: String, options: TemplateOptions?)
}