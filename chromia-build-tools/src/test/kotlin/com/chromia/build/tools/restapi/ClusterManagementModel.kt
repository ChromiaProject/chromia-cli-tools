package com.chromia.build.tools.restapi

import com.chromia.directory1.cm_api.cmGetBlockchainCluster
import com.chromia.directory1.cm_api.cmGetClusterAnchoringChains
import com.chromia.directory1.cm_api.cmGetClusterInfo
import com.chromia.directory1.cm_api.cmGetClusterNames
import net.postchain.client.core.PostchainQuery
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtx.GtxQuery

class ClusterManagementModel(private val model: CachedModel, val directoryChain: PostchainQuery) : CachedModel by model {
    constructor(directoryChain: PostchainQuery) : this(TestModel(BlockchainRid.ZERO_RID), directoryChain)

    override fun queryWithHeight(query: GtxQuery): Pair<Gtv, Long> = query(query) to 0

    override fun query(query: GtxQuery): Gtv {
        val (name, args) = query
        return when (name) {
            // TODO: Not exhaustive cm api and possibly parameterize
            "cm_get_cluster_names" -> directoryChain.cmGetClusterNames().map { gtv(it) }.let { gtv(it) }
            "cm_get_cluster_info" -> GtvObjectMapper.toGtvDictionary(directoryChain.cmGetClusterInfo(args["cluster_name"]!!.asString()))
            "cm_get_cluster_of_blockchain" -> directoryChain.cmGetBlockchainCluster(args["blockchain_rid"]!!.asByteArray()).let { gtv(it) }
            "cm_cluster_anchoring_chains" -> directoryChain.cmGetClusterAnchoringChains().map { gtv(it) }.let { gtv(it) }
            "cm_get_blockchain_api_urls" -> gtv(gtv(RestApiInstance.apiUrl))
            "cm_get_blockchain_cluster" -> gtv("test_cluster")
            else -> model.query(query)
        }
    }
}
