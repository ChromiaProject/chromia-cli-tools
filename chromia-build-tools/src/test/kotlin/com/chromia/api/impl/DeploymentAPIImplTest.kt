package com.chromia.api.impl

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.isSuccess
import com.chromia.build.tools.TestClient
import com.chromia.build.tools.config.ChromiaConfigLoader
import com.chromia.build.tools.testData
import com.chromia.cli.model.DeploymentModel
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

fun printer(isError: Boolean, message: String) {
    assertThat(message.length).isGreaterThan(0)
}

fun logger(message: String) {
    assertThat(message.length).isGreaterThan(0)
}

class DeploymentAPIImplTest {
    @TempDir
    private lateinit var dir: Path

    @Test
    fun invalidConfigurations() {
        testData(dir)
        val testModel = DeploymentModel(BlockchainRid.ZERO_RID, "my_container", gtv("http://host"), mapOf("my_chain" to BlockchainRid.ZERO_RID))
        testModel.failsToCreateNewDeployment("Deployment for chain [my_chain] already configured")
        testModel.copy(container = null).failsToCreateNewDeployment("No container id is configured")
        testModel.copy(chains = mapOf()).failsToCreateNewDeployment("No signers configured")
    }

    @Test
    fun successfulDeployment() {
        testData(dir) {
            secret()
        }
        val testModel = DeploymentModel(BlockchainRid.ZERO_RID, "my_container", gtv("http://host"))

        val res = createNew(::printer, testModel, ChromiaConfigLoader(::logger).loadClientConfigFile(dir.resolve(".chromia/config").toFile()), listOf(BlockchainConfiguration("my_chain", GtvNull)), false) { TestClient(it, { 0L }) }
        assertThat(res.isSuccess()).isTrue()
        assertThat(res.first().blockchainRid).isNotNull()
    }

    @Test
    fun failingUpdateConfigs() {
        testData(dir)
        val testModel = DeploymentModel(BlockchainRid.ZERO_RID, "my_container", gtv("http://host"), mapOf("my_chain" to BlockchainRid.ZERO_RID))
        testModel.failsToUpdateDeployment("No signers configured")
        testModel.copy(chains = mapOf()).failsToUpdateDeployment("Deployment for chain [my_chain] not found")

        val res = assertThrows<IllegalArgumentException> {
            val modelWithMultiple = testModel.copy(chains = mapOf("my_chain" to BlockchainRid.buildRepeat(2), "other_chain" to BlockchainRid.buildRepeat(3)))
            updateExisting(::printer, modelWithMultiple, ChromiaConfigLoader(::logger).loadClientConfigFile(), listOf(BlockchainConfiguration("my_chain", GtvNull), BlockchainConfiguration("other_chain", GtvNull)), 32, false)
        }
        assertThat(res.message!!).contains("Cannot update multiple blockchains when height is set")
    }

    @Test
    fun successfulUpdate() {
        testData(dir) {
            secret()
        }
        val testModel = DeploymentModel(BlockchainRid.ZERO_RID, "my_container", gtv("http://host"), chains = mapOf("my_chain" to BlockchainRid.buildRepeat(2)))

        val res = updateExisting(::printer, testModel, ChromiaConfigLoader(::logger).loadClientConfigFile(dir.resolve(".chromia/config").toFile()), listOf(BlockchainConfiguration("my_chain", GtvNull)), null, false, { DeploymentClient(it) })
        assertThat(res.isSuccess()).isTrue()
        assertThat(res.first().blockchainRid).isEqualTo(BlockchainRid.buildRepeat(2))
    }

    private fun DeploymentModel.failsToCreateNewDeployment(containsMessage: String) {
        val res1 = assertThrows<IllegalArgumentException> {
            createNew(::printer, this, ChromiaConfigLoader(::logger).loadClientConfigFile(), listOf(BlockchainConfiguration("my_chain", GtvNull)), false)
        }
        assertThat(res1.message!!).contains(containsMessage)
    }

    private fun DeploymentModel.failsToUpdateDeployment(containsMessage: String) {
        val res1 = assertThrows<IllegalArgumentException> {
            updateExisting(::printer, this, ChromiaConfigLoader(::logger).loadClientConfigFile(), listOf(BlockchainConfiguration("my_chain", GtvNull)), null, false)
        }
        assertThat(res1.message!!).contains(containsMessage)
    }

    inner class DeploymentClient(config: PostchainClientConfig) : TestClient(config, { 100 }) {
        override fun query(name: String, args: Gtv): Gtv {
            return when (name) {
                "cm_get_blockchain_cluster" -> gtv("my_cluster")
                else -> super.query(name, args)
            }
        }

        override fun getFeatures(): Map<String, Gtv> {
            TODO("Not yet implemented")
        }

        override fun getWaitingTransactions(): List<TxRid> {
            TODO("Not yet implemented")
        }

        override fun genericGetGtv(path: String): Gtv {
            TODO("Not yet implemented")
        }

        override fun genericGetJson(path: String): String {
            TODO("Not yet implemented")
        }
    }
}
