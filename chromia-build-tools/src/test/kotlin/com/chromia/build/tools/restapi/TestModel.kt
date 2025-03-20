package com.chromia.build.tools.restapi

import com.chromia.cli.model.DefaultChromiaModelRellVersion
import net.postchain.api.rest.BlockHeight
import net.postchain.api.rest.BlockSignature
import net.postchain.api.rest.BlockchainNodeState
import net.postchain.api.rest.InfraVersion
import net.postchain.api.rest.TransactionsCount
import net.postchain.api.rest.Version
import net.postchain.api.rest.model.ApiRejectedTransaction
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.base.ConfirmationProof
import net.postchain.base.configuration.KEY_FEATURES
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.NotFound
import net.postchain.common.tx.TransactionStatus
import net.postchain.core.BlockRid
import net.postchain.core.TransactionInfoExt
import net.postchain.core.TransactionInfoExtsTruncated
import net.postchain.core.block.BlockDetail
import net.postchain.core.block.BlockDetailsTruncated
import net.postchain.core.block.BlockQueryHeightFilter
import net.postchain.core.block.BlockQueryTimeFilter
import net.postchain.crypto.PubKey
import net.postchain.crypto.sha256Digest
import net.postchain.ebft.rest.contract.StateNodeStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculatorV1
import net.postchain.gtx.Gtx
import net.postchain.gtx.GtxQuery
import java.time.Instant

class TestModel(
        override val blockchainRid: BlockchainRid = BlockchainRid.buildRepeat(1),
        override val chainIID: Long = 0,
        val merkleHashVersion: Long = 1,
) : CachedModel {
    override val queryCacheTtlSeconds: Long = 0
    override var live: Boolean = true
    override val txQueue = mutableListOf<Gtx>()
    override val txMap = mutableMapOf<TxRid, Gtx>()
    override fun confirmBlock(blockRID: BlockRid): BlockSignature? {
        TODO("Not yet implemented")
    }

    override fun getBlock(height: Long, txHashesOnly: Boolean): BlockDetail? {
        TODO("Not yet implemented")
    }

    override fun getBlock(blockRID: BlockRid, txHashesOnly: Boolean): BlockDetail? {
        TODO("Not yet implemented")
    }

    override fun getBlockchainConfiguration(height: Long): ByteArray? =
            GtvEncoder.encodeGtv(gtv(mapOf(KEY_FEATURES to
                    gtv(mapOf("merkle_hash_version" to gtv(merkleHashVersion))))))

    override fun getBlockchainNodeState(): BlockchainNodeState {
        TODO("Not yet implemented")
    }

    override fun getBlocksBetweenHeights(heightFilter: BlockQueryHeightFilter, limit: Int, txHashesOnly: Boolean, maxDataSize: Int, excludeEmpty: Boolean): BlockDetailsTruncated {
        TODO("Not yet implemented")
    }

    override fun getBlocksBetweenTimes(timeFilter: BlockQueryTimeFilter, limit: Int, txHashesOnly: Boolean, maxDataSize: Int, excludeEmpty: Boolean): BlockDetailsTruncated {
        TODO("Not yet implemented")
    }
    
    override fun getConfirmationProof(txRID: TxRid): ConfirmationProof? {
        TODO("Not yet implemented")
    }

    override fun getCurrentBlockHeight(): BlockHeight {
        TODO("Not yet implemented")
    }

    override fun getLastTransactionNumber(): TransactionsCount {
        TODO("Not yet implemented")
    }

    override fun getNextBlockchainConfigurationHeight(height: Long): BlockHeight? {
        TODO("Not yet implemented")
    }

    override fun getVersion(): Version {
        TODO("Not yet implemented")
    }

    override fun getInfrastructureVersion(): InfraVersion {
        TODO("Not yet implemented")
    }

    override fun getStatus(txRID: TxRid): ApiStatus {
        val tx = txMap[txRID]
        return ApiStatus(tx?.let { TransactionStatus.CONFIRMED } ?: TransactionStatus.UNKNOWN)
    }

    override fun getWaitingTransactions(): List<TxRid> {
        TODO("Not yet implemented")
    }

    override fun getWaitingTransaction(txRID: TxRid): Pair<ByteArray, Instant>? {
        TODO("Not yet implemented")
    }

    override fun getRejectedTransactions(): List<ApiRejectedTransaction> {
        TODO("Not yet implemented")
    }

    override fun getTransaction(txRID: TxRid): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun getTransactionInfo(txRID: TxRid, includeTxData: Boolean): TransactionInfoExt? {
        TODO("Not yet implemented")
    }

    override fun getTransactionsInfo(timeFilter: BlockQueryTimeFilter, limit: Int, maxDataSize: Int): TransactionInfoExtsTruncated {
        TODO("Not yet implemented")
    }

    override fun getTransactionsInfoBySigner(timeFilter: BlockQueryTimeFilter, limit: Int, signer: PubKey, maxDataSize: Int): TransactionInfoExtsTruncated {
        TODO("Not yet implemented")
    }

    override fun nodePeersStatusQuery(): List<StateNodeStatus> {
        TODO("Not yet implemented")
    }

    override fun nodeStatusQuery(): StateNodeStatus {
        TODO("Not yet implemented")
    }

    override fun postTransaction(tx: ByteArray) {
        val decoded = Gtx.decode(tx)
        // TODO [use-new-algo] use new hash version here
        val rid = decoded.gtxBody.calculateTxRid(GtvMerkleHashCalculatorV1(::sha256Digest)).let { TxRid(it) }
        txQueue.add(decoded)
        txMap[rid] = decoded
    }

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "rell.get_rell_version" -> gtv(DefaultChromiaModelRellVersion)
            else -> throw NotFound("Query ${query.name} no implemented")
        }
    }

    override fun validateBlockchainConfiguration(configuration: Gtv) {
        TODO("Not yet implemented")
    }
}
