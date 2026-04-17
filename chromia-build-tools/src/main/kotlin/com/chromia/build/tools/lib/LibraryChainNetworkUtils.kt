package com.chromia.build.tools.lib

import com.chromia.build.tools.config.ChromiaPredefinedNetworks
import com.chromia.build.tools.config.MAINNET
import com.chromia.build.tools.config.withOverrides
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid

object LibraryChainNetworkUtils {

    val predefinedLibChainRids: Map<String, BlockchainRid> = mapOf(
            MAINNET to BlockchainRid.buildFromHex("C9051571CD822507DDD1F3B43F2DC066B54CC5A25ECD758A1B5A42913483CF20")
    )

    fun createLibraryChainClient(
            registry: String? = null,
            brid: BlockchainRid? = null,
            config: PostchainClientConfig? = null,
            configOverrides: Map<String, String> = emptyMap()
    ): PostchainClient = if (registry == null) {

        val network = MAINNET
        val lcRid = predefinedLibChainRids[network]
                ?: error("Library chain RID is not defined for network $network.")
        ChromiaPredefinedNetworks.connect(network, config, configOverrides).getChainClient(lcRid)

    } else if (ChromiaPredefinedNetworks.isPredefined(registry)) {

        requireNotNull(brid) { "Library chain RID is required for predefined network $registry" }
        ChromiaPredefinedNetworks.connect(registry, config, configOverrides).getChainClient(brid)

    } else { // Single node registry
        requireNotNull(brid) { "Brid of library chain is required for registry $registry" }

        // Create default config if none is provided
        val config0 = config
                ?.copy(blockchainRid = brid, endpointPool = EndpointPool.singleUrl(registry))
                ?: PostchainClientConfig(blockchainRid = brid, endpointPool = EndpointPool.singleUrl(registry))

        // Apply overrides
        val resultingConfig = config0.withOverrides(configOverrides)

        PostchainClientImpl(resultingConfig)
    }
}
