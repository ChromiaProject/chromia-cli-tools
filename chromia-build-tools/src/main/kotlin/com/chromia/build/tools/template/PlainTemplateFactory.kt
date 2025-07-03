package com.chromia.build.tools.template

import com.chromia.build.tools.util.snakeCaseName
import java.io.File

/**
 * Creates an empty single-module project with a main and a test file.
 */
class PlainTemplateFactory : AbstractTemplateFactory() {
    override fun createProjectFiles(targetDir: File, projectName: String, options: TemplateOptions?) {
        val projectFileName = snakeCaseName(projectName)
        with(FileBuilder(targetDir, "plain")) {
            createChromiaConfig(projectName)
            createGitIgnore()
            createFormatterConfig()
            createLinterConfig()
            createFile("src/main.rell")
            createFile("src/test/plain_test.rell", "src/test/${projectFileName}_test.rell")
        }
    }
}