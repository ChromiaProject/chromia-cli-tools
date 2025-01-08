package com.chromia.build.tools.iccf

import net.postchain.base.BaseBlockWitnessBuilder
import net.postchain.base.ConfirmationProof
import net.postchain.base.extension.getMerkleHashVersion
import net.postchain.base.gtv.BlockHeaderData
import net.postchain.common.BlockchainRid
import net.postchain.common.data.Hash
import net.postchain.common.exception.UserMistake
import net.postchain.common.toHex
import net.postchain.core.TxEContext
import net.postchain.core.block.BlockHeader
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtv.merkle.makeMerkleHashCalculator
import net.postchain.gtv.merkle.proof.merkleHash
import net.postchain.gtv.merkle.proof.toGtvVirtual
import net.postchain.gtv.merkleHash
import net.postchain.gtx.GTXOpMistake
import net.postchain.gtx.GTXOperation
import net.postchain.gtx.data.ExtOpData
import net.postchain.rell.base.utils.PostchainGtvUtils.cryptoSystem

/**
 * Verifies the witnesses and confirmationProof of an iccf-operation where the tx has been processed on the same node.
 *
 * NOT TO BE USED IN PRODUCTION!
 *
 * Production implementation found here:
 * https://gitlab.com/chromaway/postchain-chromia/-/blob/3.14.11/chromia-infrastructure/src/main/kotlin/net/postchain/d1/iccf/IccfGTXOperation.kt
 */
class SingleNodeIccfGtxOperation(context: SingleNodeIccfGtxModule.Config, opData: ExtOpData) : GTXOperation(opData) {
    private val postchainContext = context.context

    override fun apply(ctx: TxEContext) = true

    override fun checkCorrectness() {
        require(data.args.size == 3 || data.args.size == 6)
        val (_, sourceTxHash, sourceTxConfirmationProof, sourceBlockRid, sourceBlockHeaderData) = getSourceInfo(data.args)
        verifyWitnessesAndMerkleProofTree(sourceTxConfirmationProof, sourceBlockRid, sourceTxHash, sourceBlockHeaderData)
    }

    private fun verifyWitnessesAndMerkleProofTree(confirmationProof: ConfirmationProof, blockRid: Hash, txHash: ByteArray, blockHeaderData: BlockHeaderData) {
        verifyWitnessesAreCorrectAllowedWitnesses(blockHeaderData, confirmationProof, blockRid)
        verifyMerkleProofTree(confirmationProof, blockHeaderData, txHash)
    }
    
    private fun verifyWitnessesAreCorrectAllowedWitnesses(decodedBlockHeader: BlockHeaderData, confirmationProof: ConfirmationProof, blockRid: Hash) {
        val signers = listOf(postchainContext.appConfig.pubKeyByteArray)
        val blockWitnessBuilder = BaseBlockWitnessBuilder(cryptoSystem, object : BlockHeader {
            override val prevBlockRID = decodedBlockHeader.getPreviousBlockRid()
            override val rawData = confirmationProof.blockHeader
            override val blockRID = blockRid
        }, signers.toTypedArray(), 1)

        for (signature in confirmationProof.witness.getSignatures()) {
            blockWitnessBuilder.applySignature(signature)
        }
    }

    private fun verifyMerkleProofTree(confirmationProof: ConfirmationProof, decodedBlockHeader: BlockHeaderData, txHash: ByteArray) {
        val merkleHashCalculator = makeMerkleHashCalculator(decodedBlockHeader.getMerkleHashVersion())
        val proofRootHash = confirmationProof.merkleProofTree.merkleHash(merkleHashCalculator)
        if (!decodedBlockHeader.getMerkleRootHash().contentEquals(proofRootHash)) {
            throw UserMistake("Proof tree root hash mismatch, expected ${decodedBlockHeader.getMerkleRootHash().toHex()} but was ${proofRootHash.toHex()}")
        }

        val proofTxHash = confirmationProof.merkleProofTree.toGtvVirtual()[confirmationProof.txIndex.toInt()].asByteArray()
        if (!txHash.contentEquals(proofTxHash)) {
            throw UserMistake("Proof transaction hash mismatch, expected ${txHash.toHex()} but was ${proofTxHash.toHex()}")
        }
    }

    private fun getSourceInfo(args: Array<out Gtv>): SourceInfo {
        val sourceBlockchainRid = BlockchainRid(decodeSafely(args, 0) { it.asByteArray() })
        val sourceTxHash = decodeSafely(args, 1) { it.asByteArray() }
        val sourceTxProof = GtvDecoder.decodeGtv(decodeSafely(args, 2) { it.asByteArray() })
        val sourceTxConfirmationProof = GtvObjectMapper.fromGtv(sourceTxProof, ConfirmationProof::class.java)
        val sourceBlockHeaderGtv = GtvDecoder.decodeGtv(sourceTxConfirmationProof.blockHeader)
        val sourceBlockHeaderData = BlockHeaderData.fromGtv(sourceBlockHeaderGtv)
        val merkleHashCalculator = makeMerkleHashCalculator(sourceBlockHeaderData.getMerkleHashVersion())
        val sourceBlockRid = sourceBlockHeaderGtv.merkleHash(merkleHashCalculator)
        return SourceInfo(sourceBlockchainRid, sourceTxHash, sourceTxConfirmationProof, sourceBlockRid, sourceBlockHeaderData)
    }

    private fun <T> decodeSafely(args: Array<out Gtv>, argIndex: Int, decodeFn: (Gtv) -> T): T {
        return try {
            decodeFn(args[argIndex])
        } catch (e: UserMistake) {
            throw GTXOpMistake("Wrong argument type", data, argIndex, e)
        }
    }

    @Suppress("ArrayInDataClass")
    private data class SourceInfo(
            val sourceBlockchainRid: BlockchainRid,
            val sourceTxHash: ByteArray,
            val sourceTxConfirmationProof: ConfirmationProof,
            val sourceBlockRid: Hash,
            val sourceBlockHeader: BlockHeaderData
    )
}
