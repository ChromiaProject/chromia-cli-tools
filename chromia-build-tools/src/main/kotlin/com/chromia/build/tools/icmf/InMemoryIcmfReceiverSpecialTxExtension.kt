package com.chromia.build.tools.icmf

import net.postchain.base.SpecialTransactionPosition
import net.postchain.common.BlockchainRid
import net.postchain.core.BlockEContext
import net.postchain.crypto.CryptoSystem
import net.postchain.gtx.GTXModule
import net.postchain.gtx.data.OpData
import net.postchain.gtx.special.GTXSpecialTxExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryIcmfReceiverSpecialTxExtension : GTXSpecialTxExtension {
    lateinit var topics: List<String>
    private val lastReadMessagePerTopic = ConcurrentHashMap<String, Int>()
    private val blockedPipes = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    override fun init(module: GTXModule, chainID: Long, blockchainRID: BlockchainRid, cs: CryptoSystem) = Unit

    override fun getRelevantOps() = setOf(IcmfMessageOp.OP_NAME)

    override fun needsSpecialTransaction(position: SpecialTransactionPosition): Boolean = when (position) {
        SpecialTransactionPosition.Begin -> true
        SpecialTransactionPosition.End -> false
    }

    override fun createSpecialOperations(position: SpecialTransactionPosition, bctx: BlockEContext): List<OpData> {
        val allOps = topics.filterNot { it in blockedPipes }.flatMap { topic ->
            val lastSeenMessageIndex = lastReadMessagePerTopic.getOrPut(topic) { 0 }
            val messages = InMemoryIcmfMessageQueue.getMessages(lastSeenMessageIndex, topic)
            val size = messages.size // To not get ConcurrentModificationException in after commit hook
            bctx.addAfterCommitHook {
                lastReadMessagePerTopic[topic] = lastSeenMessageIndex + size
            }
            messages.map { msg -> msg.toOpData() }
        }
        bctx.addAfterCommitHook {
            blockedPipes.clear()
        }
        return allOps
    }

    override fun validateSpecialOperations(position: SpecialTransactionPosition, bctx: BlockEContext, ops: List<OpData>): Boolean {
        ops.filter { it.opName == IcmfMessageOp.OP_NAME }.forEach {
            IcmfMessageOp.fromOpData(it) ?: return false
        }
        return true
    }

    fun blockPipe(topic: String) {
        blockedPipes += topic
    }
}
