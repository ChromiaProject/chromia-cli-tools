package com.chromia.api.impl.deployment

import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.build.tools.config.BlockchainConfigurationCompressor
import com.chromia.directory1.proposal_blockchain.findBlockchainRid
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.client.core.PostchainClient
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.mapper.toObject

fun interface DeploymentOperation {
    operator fun invoke(transactionBuilder: TransactionBuilder)
}

fun postTransaction(printer: (Boolean, String) -> Unit, client: PostchainClient, configuration: BlockchainConfiguration, operation: DeploymentOperation): BlockchainDeploymentResult {
    val result = client
            .transactionBuilder()
            .addNop()
            .apply {
                operation(this)
            }
            .post()

    if (result.status == TransactionStatus.REJECTED) {
        printer(true, "Deployment of blockchain ${configuration.name} failed: ${result.rejectReason ?: ""}")
    }

    return BlockchainDeploymentResult(
            configuration,
            result.txRid,
            success = result.status != TransactionStatus.REJECTED,
            transactionResult = result
    )
}

fun awaitConfirmation(printer: (Boolean, String) -> Unit, client: PostchainClient, partialResult: BlockchainDeploymentResult): BlockchainDeploymentResult {
    if (!partialResult.success) return partialResult
    val result = client.awaitConfirmation(partialResult.txRid, client.config.statusPollCount, client.config.statusPollInterval)
    when (result.status) {
        TransactionStatus.CONFIRMED -> {
            return partialResult
        }

        TransactionStatus.REJECTED -> {
            printer(true, "Deployment of blockchain ${partialResult.blockchain.name} failed: ${result.rejectReason ?: ""}")
        }

        TransactionStatus.WAITING -> {
            printer(false, "Deployment of blockchain ${partialResult.blockchain.name} still pending, on tx-rid: ${partialResult.txRid.rid},  please check the tx-rid to see if the request got rejected/accepted")
        }
        else -> throw RuntimeException("Cannot find status for this transaction")
    }
    return partialResult.copy(success = false, transactionResult = result)
}

fun findBlockchainRid(printer: (Boolean, String) -> Unit, client: PostchainClient, apiVersion: Long, partialResult: BlockchainDeploymentResult): BlockchainDeploymentResult {
    if (!partialResult.success) return partialResult
    val maybeBcRid = if (apiVersion >= 8) {
        client.findBlockchainRid(partialResult.txRid.rid.hexStringToByteArray())?.let { BlockchainRid(it) }
    } else {
        GtvToBlockchainRidFactory.calculateBlockchainRid(partialResult.blockchain.config.toObject())
    }
    if (maybeBcRid == null) {
        printer(true, "Deployment of blockchain ${partialResult.blockchain.name} was proposed, tx-rid: ${partialResult.txRid.rid}")
        return partialResult.copy(success = false)
    }
    printer(false, "Deployment of blockchain ${partialResult.blockchain.name} was successful")
    return partialResult.copy(blockchainRid = maybeBcRid)
}

fun compressConfiguration(client: PostchainClient, apiVersion: Long, partialResult: BlockchainConfiguration): BlockchainConfiguration {
    return partialResult.copy(
            config = BlockchainConfigurationCompressor.compress(client, partialResult.config, apiVersion)
    )
}

