package com.chromia.build.tools.model.writer

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag

internal fun Node.isIncludeNode(): Boolean =
    this is ScalarNode && this.tag == Tag("!include")

internal fun Node.isScalarWithValue(value: String): Boolean =
    this is ScalarNode && this.value == value

internal fun createScalarNode(value: String): ScalarNode =
    ScalarNode(Tag.STR, value, null, null, DumperOptions.ScalarStyle.PLAIN)

internal fun MappingNode.getOrCreateMappingNode(key: String): MappingNode {
    val existing = value.find { it.keyNode.isScalarWithValue(key) }?.valueNode

    return when (existing) {
        null -> MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK).also {
            value.add(NodeTuple(createScalarNode(key), it))
        }
        is MappingNode -> existing
        else -> throw InvalidChromiaModel(
            "Expected '$key' to be a mapping node but found ${existing::class.simpleName}"
        )
    }
}
