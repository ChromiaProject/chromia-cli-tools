package com.chromia.build.tools.model.writer

import net.postchain.common.BlockchainRid
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import java.io.File

data class DeploymentUpdate(
    val networkName: String,
    val chainName: String,
    val brid: BlockchainRid
)

internal fun deploymentsUpdater(
    deploymentsUpdates: List<DeploymentUpdate>,
    onYamlUpdateCallback: OnYamlUpdateCallback
): YamlNodeUpdater = { rootNode, yamlDir ->
    updateChromiaDeploymentNodes(rootNode, deploymentsUpdates, yamlDir, onYamlUpdateCallback)
}

private fun updateChromiaDeploymentNodes(rootNode: Node, deploymentsUpdates: List<DeploymentUpdate>, yamlDir: File, onYamlUpdateCallback: OnYamlUpdateCallback): Boolean {
    if (rootNode !is MappingNode) return false
    if (deploymentsUpdates.isEmpty()) return false

    val deploymentsNode = rootNode.value.find { it.keyNode.isScalarWithValue("deployments") }
    if (deploymentsNode != null && deploymentsNode.valueNode.isIncludeNode()) {
        val includePath = (deploymentsNode.valueNode as ScalarNode).value
        updateIncludedDeploymentsFile(File(yamlDir, includePath), deploymentsUpdates, onYamlUpdateCallback)
        return false
    } else {
        val deploymentsNode = rootNode.getOrCreateMappingNode("deployments")
        for (update in deploymentsUpdates) {
            val chainsNode = deploymentsNode
                .getOrCreateMappingNode(update.networkName)
                .getOrCreateMappingNode("chains")

            chainsNode.value.add(
                NodeTuple(createScalarNode(update.chainName), createScalarNode("x\"${update.brid.toHex()}\""))
            )
        }
        return true
    }
}

private fun updateIncludedDeploymentsFile(includeFile: File, deploymentsUpdates: List<DeploymentUpdate>, onYamlUpdateCallback: OnYamlUpdateCallback) {
    if (!includeFile.exists()) throw InvalidChromiaModel("Included deployments file not found: ${includeFile.path}")

    val originalContent = includeFile.readText()
    val rootNode = includeFile.bufferedReader().use { it.parseYaml() }
    val deploymentsNode = rootNode as? MappingNode ?: throw InvalidChromiaModel("Expected root of ${includeFile.name} to be a mapping node but found ${rootNode::class.simpleName}")

    for (update in deploymentsUpdates) {
        deploymentsNode
            .getOrCreateMappingNode(update.networkName)
            .getOrCreateMappingNode("chains")
            .value.add(NodeTuple(createScalarNode(update.chainName), createScalarNode("x\"${update.brid.toHex()}\"")))
    }

    includeFile.bufferedWriter().use { dumpYaml(deploymentsNode, it) }
    printYamlDiff(includeFile.name, originalContent, includeFile.readText(), onYamlUpdateCallback)
}

