package com.chromia.build.tools.model.writer

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.nodes.*
import java.io.BufferedWriter
import java.io.File
import java.io.StringWriter
import kotlin.collections.find
import kotlin.collections.indexOfFirst
import kotlin.io.bufferedReader
import kotlin.io.bufferedWriter
import kotlin.io.readText
import kotlin.io.use
import kotlin.run

typealias OnYamlUpdateCallback = (String) -> Unit

fun updateChromiaYamlForLibrary(
    yamlFile: File,
    libraryName: String,
    libraryVersion: String,
    onYamlUpdateCallback: OnYamlUpdateCallback = {
    }
) {
    val originalContent = yamlFile.readText()

    val yamlDir = yamlFile.parentFile
    val updatedRootNode = yamlFile.bufferedReader().use { reader ->
        val rootNode = reader.parseYaml()
        addNodeForChromiaLib(rootNode, libraryName, libraryVersion, yamlDir, onYamlUpdateCallback)
        rootNode
    }

    val stringWriter = StringWriter()
    BufferedWriter(stringWriter).use { writer ->
        dumpYaml(updatedRootNode, writer)
    }
    val updatedContent = stringWriter.toString()

    printYamlDiff(yamlFile.name, originalContent, updatedContent, onYamlUpdateCallback)

    yamlFile.bufferedWriter().use { writer ->
        dumpYaml(updatedRootNode, writer)
    }
}

private fun Node.findMappingNode(key: String): MappingNode? = when (this) {
    is MappingNode -> value.find { it.keyNode.isScalarWithValue(key) }?.valueNode as? MappingNode
    else -> null
}

private fun addNodeForChromiaLib(rootNode: Node, libraryName: String, libraryVersion: String, yamlDir: File, onYamlUpdateCallback: OnYamlUpdateCallback) {
    if (rootNode !is MappingNode) return

    val libsTuple = rootNode.value.find { it.keyNode.isScalarWithValue("libs") }
    if (libsTuple != null) {
        when {
            libsTuple.valueNode.isIncludeNode() -> {
                val includeScalar = libsTuple.valueNode as ScalarNode
                val includePath = includeScalar.value
                val includeFile = File(yamlDir, includePath)
                updateIncludedLibsFile(includeFile, libraryName, libraryVersion, onYamlUpdateCallback)
            }
            libsTuple.valueNode is MappingNode -> {
                val libsNode = libsTuple.valueNode as MappingNode
                libsNode.ensureChromiaLib(libraryName, libraryVersion)
            }
            else -> {
                createLibsNodeInRoot(rootNode, libraryName, libraryVersion)
            }
        }
    } else {
        createLibsNodeInRoot(rootNode, libraryName, libraryVersion)
    }
}

private fun updateIncludedLibsFile(includeFile: File, libraryName: String, libraryVersion: String, onYamlUpdateCallback: OnYamlUpdateCallback) {
    val originalContent = if (includeFile.exists()) includeFile.readText() else ""
    if (!includeFile.exists()) {
        includeFile.parentFile.mkdirs()
        includeFile.createNewFile()

        val emptyLibsNode = MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK)
        includeFile.bufferedWriter().use { writer ->
            dumpYaml(emptyLibsNode, writer)
        }
    }

    val updatedLibsNode = includeFile.bufferedReader().use { reader ->
        val rootNode = reader.parseYaml()
        if (rootNode is MappingNode) {
            rootNode.ensureChromiaLib(libraryName, libraryVersion)
        }
        rootNode
    }

    val stringWriter = StringWriter()
    BufferedWriter(stringWriter).use { writer ->
        dumpYaml(updatedLibsNode, writer)
    }
    val updatedContent = stringWriter.toString()

    printYamlDiff(includeFile.name, originalContent, updatedContent, onYamlUpdateCallback)

    includeFile.bufferedWriter().use { writer ->
        dumpYaml(updatedLibsNode, writer)
    }
}

private fun createLibsNodeInRoot(rootNode: MappingNode, libraryName: String, libraryVersion: String) {
    val libsKey = createScalarNode("libs")
    val libsNode = MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK)
    libsNode.ensureChromiaLib(libraryName, libraryVersion)
    rootNode.value.add(NodeTuple(libsKey, libsNode))
}

private fun MappingNode.ensureChromiaLib(libraryName: String, libraryVersion: String) {
    val chromiaLibNode = findMappingNode(libraryName) ?: run {
        val newKey = createScalarNode(libraryName)
        val newNode = MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK)
        value.add(NodeTuple(newKey, newNode))
        newNode
    }
    ensureVersion(chromiaLibNode, libraryVersion)
}

private fun ensureVersion(libraryNode: MappingNode, version: String) {
    val versionIndex = libraryNode.value.indexOfFirst { it.keyNode.isScalarWithValue("version") }

    if (versionIndex != -1) {
        val versionKey = libraryNode.value[versionIndex].keyNode
        val versionValue = createScalarNode(version)
        libraryNode.value[versionIndex] = NodeTuple(versionKey, versionValue)
    } else {
        val versionKey = createScalarNode("version")
        val versionValue = createScalarNode(version)
        libraryNode.value.add(NodeTuple(versionKey, versionValue))
    }
}

