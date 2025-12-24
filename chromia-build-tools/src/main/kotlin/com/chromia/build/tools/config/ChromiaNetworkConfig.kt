package com.chromia.build.tools.config

import com.chromia.directory1.common.queries.getBlockchainApiUrls
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.StandardChromiaClient

const val MAINNET = "mainnet"
const val TESTNET = "testnet"
const val DEVNET1 = "devnet1"
const val DEVNET2 = "devnet2"
const val CHROMIA_PREDEFINED_TESTING_NETWORK = "chromia_predefined_testing_network_chromia_cli"

fun getProviderUrlsForNetwork(network: String): List<String>? =
    predefinedNetworks[network]?.value

fun getDirectoryChainRidFor(network: String): BlockchainRid? =
    predefinedDirectoryChains[network]?.getDirectoryChainRid


private val predefinedDirectoryChains: Map<String,DirectoryChainNetwork> = mapOf(
    MAINNET to DirectoryChainNetwork("https://system.chromaway.com"),
    TESTNET to DirectoryChainNetwork("https://node0.testnet.chromia.com"),
    DEVNET1 to DirectoryChainNetwork("https://node0.devnet1.chromia.dev"),
    DEVNET2 to DirectoryChainNetwork("https://node0.devnet2.chromia.dev"),
    CHROMIA_PREDEFINED_TESTING_NETWORK to DirectoryChainNetwork("http://localhost:7745")
)

// NOTE: This will only be evaluated when accessed, preventing network calls during initialization
//  otherwise for devnet1 it will hang without openvpn connected
private val predefinedNetworks: Map<String, Lazy<List<String>>> =
    predefinedDirectoryChains.mapValues { (_, chain) ->
        lazy { chain.getProvidersFromDirectoryChain }
    }

private data class DirectoryChainNetwork(
    val nodeUrl: String,
) {
    private val client by lazy {
        StandardChromiaClient(EndpointPool.singleUrl(nodeUrl))
            .getDirectoryChainClient()
    }

    val getProvidersFromDirectoryChain  by lazy {
        val directoryChainRid = client.getBlockchainRID(0L)
        client.getBlockchainApiUrls(directoryChainRid)
    }

    val getDirectoryChainRid by lazy {
        client.getBlockchainRID(0L)
    }
}
