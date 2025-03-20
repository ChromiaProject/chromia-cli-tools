package com.chromia.build.tools.config

import com.chromia.build.tools.keystore.ChromiaKeyStore
import com.chromia.cli.model.DeploymentModel
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.config.PostchainClientConfig.Companion.fromConfiguration
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.PropertiesFileLoader
import net.postchain.common.exception.UserMistake
import net.postchain.crypto.KeyPair
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class ChromiaClientConfig private constructor(
    private var config: PostchainClientConfig
) {
    val blockchainRid get() = config.blockchainRid
    val signers get() = config.signers
    val endpointPool get() = config.endpointPool
    val apiUrls get() = config.endpointPool.map { it.url }

    fun setBrid(blockchainRid: BlockchainRid) = apply {
        config = config.copy(blockchainRid = blockchainRid)
    }

    fun setApiUrls(urls: List<String>) = apply {
        config = config.copy(endpointPool = EndpointPool.default(urls))
    }

    fun setSigner(vararg keyPair: KeyPair) = apply {
        config = config.copy(signers = keyPair.toList())
    }

    fun setSignerFromSecret(path: Path) = apply {
        val secretProps = PropertiesFileLoader.load(path.absolutePathString())
        if (secretProps.containsKey("pubkey") && secretProps.containsKey("privkey")) {
            setSigner(KeyPair.of(secretProps.getString("pubkey"), secretProps.getString("privkey")))
        } else {
            throw UserMistake("Secret file: $path does not contain 'pubkey' and/or 'privkey' properties")
        }
    }

    fun setSignerUsingKeyId(keyId: String) = apply {
        ChromiaKeyStore(keyId).findKeyPair()?.let { setSigner(it) }
            ?: throw UserMistake("Key with ID '$keyId' not found")
    }

    fun setDeployment(deploymentModel: DeploymentModel) = apply {
        require(deploymentModel.blockchainRid != null) { "blockchainRid must be set" }
        setBrid(deploymentModel.blockchainRid)
        setApiUrls(deploymentModel.urls)
    }

    fun client(provider: PostchainClientProvider) = provider.createClient(config)

    companion object {
        const val DEFAULT_API_URL = "http://localhost:7740"

        val EMPTY = from(PropertiesConfiguration())

        fun from(config: Configuration): ChromiaClientConfig = PropertiesConfiguration()
            .apply {
                copy(config)
                if (!config.containsKey("api.url")) setProperty("api.url", DEFAULT_API_URL)
                if (!config.containsKey("brid")) setProperty("brid", BlockchainRid.ZERO_RID)
            }
            .let { fromConfiguration(it) }
            .let { ChromiaClientConfig(it) }
    }
}
