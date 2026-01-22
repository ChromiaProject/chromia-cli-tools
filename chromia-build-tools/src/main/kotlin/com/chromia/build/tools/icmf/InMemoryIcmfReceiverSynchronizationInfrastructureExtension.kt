package com.chromia.build.tools.icmf

import net.postchain.PostchainContext
import net.postchain.common.exception.UserMistake
import net.postchain.core.BlockchainProcess
import net.postchain.core.SynchronizationInfrastructureExtension
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.mapper.Nullable
import net.postchain.gtv.mapper.toList
import net.postchain.gtx.GTXModule
import net.postchain.gtx.GTXModuleAware

class InMemoryIcmfReceiverSynchronizationInfrastructureExtension(private val postchainContext: PostchainContext) : SynchronizationInfrastructureExtension {

    override fun connectProcess(process: BlockchainProcess) {
        val engine = process.blockchainEngine
        val configuration = engine.getConfiguration()
        if (configuration is GTXModuleAware) {
            getIcmfReceiverSpecialTxExtension(configuration.module)?.let { txExt ->
                val icmConfigs = configuration.rawConfig["icmf"]?.get("receiver") ?: throw UserMistake("Missing configuration key icmf/receiver")
                txExt.topics = buildList {
                    icmConfigs["local"]?.toList<IcmfReceiverSpecificBlockChainConfig>()?.map { it.topic }?.let { addAll(it) }
                    icmConfigs["global"]?.get("topics")?.asArray()?.map { it.asString() }?.let { addAll(it) }
                    icmConfigs["anchoring"]?.get("topics")?.asArray()?.map { it.asString() }?.let { addAll(it) }
                    icmConfigs["directory-chain"]?.get("topics")?.asArray()?.map { it.asString() }?.let { addAll(it) }
                }
            }

        }
    }

    override fun disconnectProcess(process: BlockchainProcess) {}

    override fun shutdown() {}

    private fun getIcmfReceiverSpecialTxExtension(module: GTXModule): InMemoryIcmfReceiverSpecialTxExtension? {
        return module.getSpecialTxExtensions().firstOrNull { ext ->
            (ext is InMemoryIcmfReceiverSpecialTxExtension)
        } as InMemoryIcmfReceiverSpecialTxExtension?
    }

    data class IcmfReceiverSpecificBlockChainConfig(
            @param:Nullable // Nullable on local node since we do not read it. Must be non-null in production
            @param:Name("bc-rid")
            val blockchainRid: ByteArray?,

            @param:Name("topic")
            val topic: String
    )
}
