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
        val config: PostchainClientConfig? = null,
        val configOverrides: Map<String, String> = emptyMap()
) {

    val clientFactory: StandardChromiaClient
    val directoryChainClient: ChromiaPostchainClient
    val directoryChainApiUrls: List<String>

    init {
        // Create default config if none is provided
        val config0 = config?.copy(
                endpointPool = EndpointPool.singleUrl(nodeUrl),
                requestStrategy = TryNextOnErrorRequestStrategyFactory()
        ) ?: PostchainClientConfig(
                blockchainRid = BlockchainRid.ZERO_RID, // Automatically look up directory chain if not provided
                endpointPool = EndpointPool.singleUrl(nodeUrl),
                requestStrategy = TryNextOnErrorRequestStrategyFactory()
        )

        // Apply overrides
        val resultingConfig = config0.withOverrides(configOverrides)

        clientFactory = StandardChromiaClient(resultingConfig)
        directoryChainClient = clientFactory.getDirectoryChainClient()
        directoryChainApiUrls = directoryChainClient.config.endpointPool.map { it.url }
    }

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
     * Connects to a predefined Chromia network and returns a cached [DirectoryChainNetwork] instance.
     *
     * This method creates a new connection to the specified predefined network on the first call
     * and caches it for subsequent calls. The cache is keyed only by the [network] parameter.
     *
     * **Important:** Once a network connection is cached, subsequent calls with the same [network]
     * will return the cached instance and ignore any newly provided [config] or [configOverrides].
     * In particular, if multiple `rell-install` executions run within the same JVM (e.g. two Maven
     * plugin executions in a single build), only the first execution's parameters take effect —
     * subsequent executions silently use the cached network regardless of their [configOverrides].
     *
     * @param network The name of the predefined network to connect to. Must be one of:
     *                [MAINNET], [TESTNET], [DEVNET1], [DEVNET2], or [CHROMIA_PREDEFINED_TESTING_NETWORK].
     * @param config Optional [PostchainClientConfig] to use for the connection. Only applied on the
     *               first call for a given network; ignored on subsequent calls.
     * @param configOverrides Optional map of configuration overrides. Only applied on the first call
     *                        for a given network; ignored on subsequent calls.
     * @return A [DirectoryChainNetwork] instance for the specified network.
     * @throws IllegalStateException if the network is not predefined (node URL not found).
     */
    fun connect(network: String, config: PostchainClientConfig? = null, configOverrides: Map<String, String> = emptyMap()): DirectoryChainNetwork {
        val url = nodeUrls[network] ?: error("Node URL is not defined for network $network")
        return networks.computeIfAbsent(network) {
            DirectoryChainNetwork(url, config, configOverrides)
        }
    }
}
