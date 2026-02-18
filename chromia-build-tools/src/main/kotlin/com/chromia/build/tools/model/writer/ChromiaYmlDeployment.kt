package com.chromia.build.tools.model.writer

import com.chromia.build.tools.lib.OnYamlUpdateCallback
import net.postchain.common.BlockchainRid
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import java.io.File

// TODO: Handle include node

internal fun updateChromiaDeploymentNode(rootNode: Node, networkName: String, chainName: String, brid: BlockchainRid, yamlDir: File, onYamlUpdateCallback: OnYamlUpdateCallback) {
    if (rootNode !is MappingNode) return

    val chainsNode = rootNode
        .getOrCreateMappingNode("deployments")
        .getOrCreateMappingNode(networkName)
        .getOrCreateMappingNode("chains")

    chainsNode.value.add(
        NodeTuple(createScalarNode(chainName), createScalarNode("x\"${brid.toHex()}\""))
    )
}

