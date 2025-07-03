package com.chromia.build.tools.template

import java.io.File

/**
 * Creates an empty template with a rell backend and simple frontend to connect a wallet
 */
class AssetManagementTemplateFactory : AbstractTemplateFactory() {
    override fun createProjectFiles(targetDir: File, projectName: String, options: TemplateOptions?) {
        with(FileBuilder(targetDir, "asset-management")) {
            moveFolder("")
            createFormatterConfig()
            createLinterConfig()
        }
    }
}