package com.chromia.build.tools.lib

import com.chromia.build.tools.blockchain.BridFetcher
import com.chromia.build.tools.config.ChromiaClientConfig.Companion.DEFAULT_API_URL
import com.chromia.build.tools.config.predefinedNetworks
import com.chromia.build.tools.lib.LibraryChainNetworkUtils.CHROMIA_MAINNET
import com.chromia.build.tools.lib.LibraryChainNetworkUtils.libraryPredefinedNetworks
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.defaultHttpHandler
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.StandardChromiaClient
import org.http4k.core.HttpHandler

object LibraryChainNetworkUtils {
    const val CHROMIA_MAINNET = "mainnet"
    const val TESTNET = "testnet"
    const val LOCALHOST = "localhost"

    val libraryPredefinedNetworks: Map<String, () -> BlockchainRid> by lazy {
        mapOf(
            CHROMIA_MAINNET to {
                BlockchainRid.buildFromHex(
                    "C9051571CD822507DDD1F3B43F2DC066B54CC5A25ECD758A1B5A42913483CF20"
                )
            },
            TESTNET to {
                BlockchainRid.buildFromHex(
                    "76693857DEDCCA049BA3546ACADB2F73648B1A83FF8A8210F3F89EBD59DBC7C7"
                )
            },
            LOCALHOST to {
                fetchBridFromLocalNode()
            }
        )
    }

    private fun fetchBridFromLocalNode() = runCatching {
        val config = PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.singleUrl(DEFAULT_API_URL))
        val httpHandler: HttpHandler = defaultHttpHandler(config)
        BridFetcher(httpHandler, DEFAULT_API_URL).fetchBlockchainRid(0)
    }.onFailure { _ ->
        error("Unable to fetch brid from local node")
    }.getOrThrow()
}

fun createLibraryChainClient(explicitUrl: String? = null, explicitBrid: BlockchainRid? = null): PostchainClient {
    val networkOrUrl = explicitUrl ?: CHROMIA_MAINNET
    val targetUrls = resolveLibraryChainNetworkUrls(networkOrUrl)

    val libraryChainBrid = resolveLibraryChainBrid(explicitBrid, networkOrUrl)

    return StandardChromiaClient(EndpointPool.default(targetUrls))
        .getClient(libraryChainBrid)
}

private fun resolveLibraryChainNetworkUrls(networkOrUrl: String) = when {
    predefinedNetworks[networkOrUrl] != null -> predefinedNetworks[networkOrUrl]!!
    else -> listOf(networkOrUrl)
}

private fun resolveLibraryChainBrid(explicitBrid: BlockchainRid?, networkOrUrl: String) =
    explicitBrid
        ?: libraryPredefinedNetworks[networkOrUrl]?.invoke()
        ?: error("Brid of library_chain is required")

