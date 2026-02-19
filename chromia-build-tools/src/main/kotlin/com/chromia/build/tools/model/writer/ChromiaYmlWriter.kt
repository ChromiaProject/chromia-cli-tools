package com.chromia.build.tools.model.writer

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import net.postchain.common.BlockchainRid
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.parser.ParserImpl
import org.yaml.snakeyaml.reader.StreamReader
import org.yaml.snakeyaml.resolver.Resolver
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

typealias YamlNodeUpdater = (rootNode: Node, yamlDir: File) -> Boolean

object ChromiaYmlWriter {

    fun updateDeploymentNode(
        yamlFile: File,
        networkName: String,
        chainName: String,
        brid: BlockchainRid,
        onYamlUpdateCallback: OnYamlUpdateCallback = {}
    ) = update(yamlFile, onYamlUpdateCallback, deploymentUpdater(networkName, chainName, brid, onYamlUpdateCallback))

    fun updateLibraryNode(
        yamlFile: File,
        libraryName: String,
        libraryVersion: String,
        onYamlUpdateCallback: OnYamlUpdateCallback = {}
    ) = update(yamlFile, onYamlUpdateCallback, libraryUpdater(libraryName, libraryVersion, onYamlUpdateCallback))

    private fun update(
        yamlFile: File,
        onYamlUpdateCallback: OnYamlUpdateCallback,
        updater: YamlNodeUpdater
    ) {
        val originalContent = yamlFile.readText()
        val yamlDir = yamlFile.parentFile
        val rootNode = yamlFile.bufferedReader().use { it.parseYaml() }
        val rootModified = updater(rootNode, yamlDir)

        if (rootModified) {
            yamlFile.bufferedWriter().use { dumpYaml(rootNode, it) }
            printYamlDiff(yamlFile.name, originalContent, yamlFile.readText(), onYamlUpdateCallback)
        }
    }
}

internal fun BufferedReader.parseYaml(): Node {
    val reader = StreamReader(this)
    val loaderOptions = LoaderOptions().apply {
        isProcessComments = true
    }
    val parser = ParserImpl(reader, loaderOptions)
    val composer = Composer(parser, Resolver(), loaderOptions)
    return composer.singleNode
}

internal fun dumpYaml(yamlNode: Node, writer: BufferedWriter) {
    /*
        Previous indentations in file not respected with snake YAML serialize. It does a complete re-writing
     */
    val options = DumperOptions().apply {
        isProcessComments = true
        indentWithIndicator = true
        indicatorIndent = 2
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
    }

    val yaml = Yaml(options)
    yaml.serialize(yamlNode, writer)
}

internal fun printYamlDiff(fileName: String, originalContent: String, updatedContent: String, onYamlUpdateCallback: OnYamlUpdateCallback) {
    val originalLines = originalContent.lines()
    val updatedLines = updatedContent.lines()

    if (originalLines == updatedLines) {
        return
    }

    val patch = DiffUtils.diff(originalLines, updatedLines)
    val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
        fileName,
        fileName,
        originalLines,
        patch,
        3
    )
    onYamlUpdateCallback(unifiedDiff.joinToString("\n"))
}
