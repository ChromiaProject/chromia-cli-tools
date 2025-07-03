package com.chromia.build.tools.template

import java.io.File

/**
 * Minimal working example dapp (hello world)
 */
class MinimalTemplateFactory : AbstractTemplateFactory() {
    override fun createProjectFiles(targetDir: File, projectName: String, options: TemplateOptions?) {
        with(FileBuilder(targetDir, "minimal")) {
            createChromiaConfig(projectName)
            createGitIgnore()
            createFormatterConfig()
            createLinterConfig()
            createFile("src/main.rell")
            createFile("src/test/arithmetic_test.rell")
            createFile("src/test/data_test.rell")
        }
    }
}