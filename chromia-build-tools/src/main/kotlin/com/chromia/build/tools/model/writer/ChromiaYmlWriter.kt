package com.chromia.build.tools.model.writer

import com.chromia.build.tools.lib.OnYamlUpdateCallback
import net.postchain.common.BlockchainRid
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.nodes.Node
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

        val yamlDir = yamlFile.parentFile
        val updatedRootNode = yamlFile.bufferedReader().use { reader ->
            val rootNode = reader.parseYaml()
            updateChromiaDeploymentNode(rootNode, networkName, chainName, brid, yamlDir, onYamlUpdateCallback)
            rootNode
        }

        yamlFile.bufferedWriter().use { writer ->
            dumpYaml(updatedRootNode, writer)
        }
    }

    private fun BufferedReader.parseYaml(): Node {
        val reader = StreamReader(this)
        val loaderOptions = LoaderOptions().apply {
            isProcessComments = true
        }
        val parser = ParserImpl(reader, loaderOptions)
        val composer = Composer(parser, Resolver(), loaderOptions)
        return composer.singleNode
    }

    private fun dumpYaml(yamlNode: Node, writer: BufferedWriter) {
        val options = DumperOptions().apply {
            isProcessComments = true
            indentWithIndicator = true
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
        }

        val yaml = Yaml(options)
        yaml.serialize(yamlNode, writer)
    }
}

internal fun Node.isScalarWithValue(value: String): Boolean =
        this is ScalarNode && this.value == value

internal fun createScalarNode(value: String): ScalarNode =
        ScalarNode(Tag.STR, value, null, null, DumperOptions.ScalarStyle.PLAIN)
