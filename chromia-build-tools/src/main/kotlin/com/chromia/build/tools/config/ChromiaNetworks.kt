package com.chromia.build.tools.config

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.impl.TryNextOnErrorRequestStrategyFactory
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.ChromiaPostchainClient
import net.postchain.d1.client.StandardChromiaClient
import java.util.concurrent.ConcurrentHashMap

const val MAINNET = "mainnet"
const val TESTNET = "testnet"
const val DEVNET1 = "devnet1"
const val DEVNET2 = "devnet2"
const val CHROMIA_PREDEFINED_TESTING_NETWORK = "chromia_predefined_testing_network_chromia_cli"

data class DirectoryChainNetwork(
        val nodeUrl: String,
        val config: PostchainClientConfig? = null
) {

    val clientFactory: StandardChromiaClient = if (config != null)
        StandardChromiaClient(config.copy(
                requestStrategy = TryNextOnErrorRequestStrategyFactory(),
                endpointPool = EndpointPool.singleUrl(nodeUrl)
        ))
    else
        StandardChromiaClient(EndpointPool.singleUrl(nodeUrl))

    val directoryChainClient: ChromiaPostchainClient = clientFactory.getDirectoryChainClient()

    val directoryChainApiUrls = directoryChainClient.config.endpointPool.map { it.url }

    fun getChainClient(brid: BlockchainRid): ChromiaPostchainClient = clientFactory.getClient(brid)
}

object ChromiaPredefinedNetworks {

    private val nodeUrls: Map<String, String> = mapOf(
            MAINNET to "https://system.chromaway.com",
            TESTNET to "https://node0.testnet.chromia.com",
            DEVNET1 to "https://node0.devnet1.chromia.dev",
            DEVNET2 to "https://node0.devnet2.chromia.dev",
            CHROMIA_PREDEFINED_TESTING_NETWORK to "http://localhost:7745"
    )

    private val networks = ConcurrentHashMap<String, DirectoryChainNetwork>()

    fun isPredefined(network: String): Boolean = nodeUrls.containsKey(network)

    /**
     * Connects to a predefined network using [config] and caches the connection.
     *
     * The cache key is only [network]. The first call for a given network creates and caches
     * the [DirectoryChainNetwork]. Further calls for the same network return the cached
     * network and ignore the provided [config].
     */
    fun connect(network: String, config: PostchainClientConfig? = null): DirectoryChainNetwork {
        val url = nodeUrls[network] ?: error("Node URL is not defined for network $network")
        return networks.computeIfAbsent(network) {
            DirectoryChainNetwork(url, config)
        }
    }
}
