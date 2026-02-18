package com.chromia.build.tools.model.writer

import com.chromia.build.tools.lib.OnYamlUpdateCallback
import net.postchain.common.BlockchainRid
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.parser.ParserImpl
import org.yaml.snakeyaml.reader.StreamReader
import org.yaml.snakeyaml.resolver.Resolver
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

object ChromiaYmlWriter {

    fun updateDeploymentNode(
        yamlFile: File,
        networkName: String,
        chainName: String,
        brid: BlockchainRid,
        onYamlUpdateCallback: OnYamlUpdateCallback = {}
    ) {
        // TODO: Used to print yaml differences
        val originalContent = yamlFile.readText()

        var rootNodeModified = false
        val yamlDir = yamlFile.parentFile
        val updatedRootNode = yamlFile.bufferedReader().use { reader ->
            val rootNode = reader.parseYaml()
            rootNodeModified = updateChromiaDeploymentNode(rootNode, networkName, chainName, brid, yamlDir, onYamlUpdateCallback)
            rootNode
        }

        if (rootNodeModified) {
            yamlFile.bufferedWriter().use { writer ->
                dumpYaml(updatedRootNode, writer)
            }
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
    val options = DumperOptions().apply {
        isProcessComments = true
        indentWithIndicator = true
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
    }

    val yaml = Yaml(options)
    yaml.serialize(yamlNode, writer)
}

