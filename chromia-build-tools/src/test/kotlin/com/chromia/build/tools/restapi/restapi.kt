package com.chromia.build.tools.restapi

import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.client.core.PostchainQuery
import net.postchain.common.exception.UserMistake
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GtxQuery
import net.postchain.rell.base.model.R_LangVersion


fun CachedModel.withClusterManagement(directoryChain: PostchainQuery) = ClusterManagementModel(this, directoryChain)
fun CachedModel.withCompression() = CompressableModel(this)

fun CachedModel.withValidConfiguration() = object : CachedModel by this {
    override fun validateBlockchainConfiguration(configuration: Gtv) {}
}

fun CachedModel.withInvalidConfiguration() = object : CachedModel by this {
    override fun validateBlockchainConfiguration(configuration: Gtv) {
        throw UserMistake("Invalid configuration")
    }
}

fun CachedModel.withRellVersion(version: String) = withRellVersion(R_LangVersion.of(version))
fun CachedModel.withRellVersion(version: R_LangVersion) = withQuery("rell.get_rell_version", gtv(version.str()))

fun CachedModel.withRellVersionWithHeight(version: String) = withRellVersionWithHeight(R_LangVersion.of(version))
fun CachedModel.withRellVersionWithHeight(version: R_LangVersion) = withQueryWithHeight("rell.get_rell_version", gtv(version.str()))


fun CachedModel.withQuery(name: String, response: Gtv) = withQuery(name) { response }

fun CachedModel.withStatus(status: TransactionStatus, reason: String) = object : CachedModel by this {
    override fun getStatus(txRID: TxRid) = ApiStatus(status, reason, System.currentTimeMillis())
}

fun CachedModel.withQuery(name: String, response: (GtxQuery) -> Gtv) = object : CachedModel by this {
    override fun query(query: GtxQuery) = when (query.name) {
        name -> response(query)
        else -> this@withQuery.query(query)
    }
}
//queryWithHeight(query: GtxQuery): Pair<Gtv, Long>

fun CachedModel.withQueryWithHeight(name: String, response: Gtv, height: Long = 0) = withQueryWithHeight(name, { response }, height)

fun CachedModel.withQueryWithHeight(name: String, response: (GtxQuery) -> Gtv, height: Long = 0) = object : CachedModel by this {
    override fun queryWithHeight(query: GtxQuery): Pair<Gtv, Long> {
        return when (query.name) {
            name -> response(query) to height
            else -> this@withQueryWithHeight.queryWithHeight(query)
        }
    }
}
