package com.chromia.build.tools.model.writer

import com.chromia.build.tools.lib.OnYamlUpdateCallback
import net.postchain.common.BlockchainRid
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.Tag
import java.io.File

internal fun updateChromiaDeploymentNode(rootNode: Node, networkName: String, chainName: String, brid: BlockchainRid, yamlDir: File, onYamlUpdateCallback: OnYamlUpdateCallback) {
    if (rootNode !is MappingNode) return

    val deploymentNode = rootNode.value.find { it.keyNode.isScalarWithValue("deployments") }
    if (deploymentNode != null) {
        updateDeploymentsNode(deploymentNode, networkName, chainName, brid)
    } else {
        createDeploymentsNodeInRoot(rootNode, networkName, chainName, brid)
    }
}

private fun createDeploymentsNodeInRoot(rootNode: MappingNode, networkName: String, chainName: String, brid: BlockchainRid) {
    val deploymentKeyNode = createScalarNode("deployments")
    val deploymentsNode = MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK)

    val networkKeyNode = createScalarNode(networkName)
    val networksNode = MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK)

    val chainsKeyNode = createScalarNode("chains")
    val chainsNode = MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK)

    val chainKeyNode = createScalarNode(chainName)
    val bridKeyNode = createScalarNode("x\"${brid.toHex()}\"")
    val chainNode = NodeTuple(chainKeyNode, bridKeyNode)

    chainsNode.value.add(chainNode)
    networksNode.value.add(NodeTuple(chainsKeyNode, chainsNode))
    deploymentsNode.value.add(NodeTuple(networkKeyNode, networksNode))
    rootNode.value.add(NodeTuple(deploymentKeyNode, deploymentsNode))
}

private fun updateDeploymentsNode(deploymentsNode: NodeTuple, networkName: String, chainName: String, brid: BlockchainRid) {

    when {
        deploymentsNode.valueNode is MappingNode -> {
            val deploymentsNodeAsMapping = deploymentsNode.valueNode as MappingNode
            val networksNode = deploymentsNodeAsMapping.findMappingNode(networkName) ?: throw Exception("handle this")
            val chainsNode = networksNode.findMappingNode("chains") ?: throw Exception("handle this")

            val chainKeyNode = createScalarNode(chainName)
            val bridKeyNode = createScalarNode("x\"${brid.toHex()}\"")
            val chainNode = NodeTuple(chainKeyNode, bridKeyNode)

            chainsNode.value.add(chainNode)
        }
    }
}
