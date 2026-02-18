package com.chromia.build.tools.model.writer

import com.chromia.build.tools.lib.OnYamlUpdateCallback
import net.postchain.common.BlockchainRid
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import java.io.File

internal fun updateChromiaDeploymentNode(rootNode: Node, networkName: String, chainName: String, brid: BlockchainRid, yamlDir: File, onYamlUpdateCallback: OnYamlUpdateCallback): Boolean {
    if (rootNode !is MappingNode) return false

    val deploymentsNode = rootNode.value.find { it.keyNode.isScalarWithValue("deployments") }
    if (deploymentsNode != null && deploymentsNode.valueNode.isIncludeNode()) {
        val includePath = (deploymentsNode.valueNode as ScalarNode).value
        updateIncludedDeploymentsFile(File(yamlDir, includePath), networkName, chainName, brid)
        return false
    } else {
        val chainsNode = rootNode
            .getOrCreateMappingNode("deployments")
            .getOrCreateMappingNode(networkName)
            .getOrCreateMappingNode("chains")

        chainsNode.value.add(
            NodeTuple(createScalarNode(chainName), createScalarNode("x\"${brid.toHex()}\""))
        )
        return true
    }
}

private fun updateIncludedDeploymentsFile(includeFile: File, networkName: String, chainName: String, brid: BlockchainRid) {
    if (!includeFile.exists()) throw InvalidChromiaModel("Included deployments file not found: ${includeFile.path}")

    val rootNode = includeFile.bufferedReader().use { it.parseYaml() }
    val deploymentsNode = rootNode as? MappingNode ?: throw InvalidChromiaModel("Expected root of ${includeFile.name} to be a mapping node but found ${rootNode::class.simpleName}")

    deploymentsNode
        .getOrCreateMappingNode(networkName)
        .getOrCreateMappingNode("chains")
        .value.add(NodeTuple(createScalarNode(chainName), createScalarNode("x\"${brid.toHex()}\"")))

    includeFile.bufferedWriter().use { dumpYaml(deploymentsNode, it) }
}

