package com.chromia.build.tools.model.writer

import assertk.Assert
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.support.expected
import assertk.assertions.support.show
import net.postchain.common.BlockchainRid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Path

class ChromiaYmlWriterDeploymentTest {

    @TempDir
    lateinit var tempDir: Path

    private val yaml = Yaml()

    private fun <K, V> Assert<Map<K, V>>.containsKey(key: K): Assert<Map<K, V>> =
        transform("contains key ${show(key)}") { actual ->
            if (!actual.containsKey(key)) {
                expected("to contain key ${show(key)} but was ${show(actual)}")
            }
            actual
        }


    @Test
    fun `should add deployment section with deployment when it doesn't exist`() {
        val yamlContent = """
            blockchains:
              chain_zero:
                module: main
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia.yml").apply {
            writeText(yamlContent)
        }

        var capturedDiff: String? = null
        ChromiaYmlWriter.updateDeploymentNode(yamlFile, "testnet", "chain_zero", BlockchainRid.ZERO_RID) { capturedDiff = it }

        val expectedYamlContent = """
            blockchains:
              chain_zero:
                module: main
            deployments:
              testnet:
                chains:
                  chain_zero: x"${BlockchainRid.ZERO_RID}"

        """.trimIndent()
        val updatedContent = yamlFile.readText()

        assertThat(updatedContent).isEqualTo(expectedYamlContent)
        assertThat(capturedDiff).isNotNull()
        assertThat(capturedDiff!!).contains("+deployments:")
        assertThat(capturedDiff!!).contains("+  testnet:")
        assertThat(capturedDiff!!).contains("+    chains:")
        assertThat(capturedDiff!!).contains("+      chain_zero: x\"${BlockchainRid.ZERO_RID}\"")
    }

    @Test
    fun `should extend chains with new deployed chain`() {
        val yamlContent = """
            blockchains:
              chain_zero:
                module: main
              chain_one:
                module: main
            deployments:
              testnet:
                chains:
                  chain_zero: x"${BlockchainRid.ZERO_RID}"
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia.yml").apply {
            writeText(yamlContent)
        }

        var capturedDiff: String? = null
        ChromiaYmlWriter.updateDeploymentNode(yamlFile, "testnet", "chain_one", BlockchainRid.buildRepeat(1)) { capturedDiff = it }

        val expectedYamlContent = """
            blockchains:
              chain_zero:
                module: main
              chain_one:
                module: main
            deployments:
              testnet:
                chains:
                  chain_zero: x"${BlockchainRid.ZERO_RID}"
                  chain_one: x"${BlockchainRid.buildRepeat(1)}"

        """.trimIndent()
        val updatedContent = yamlFile.readText()

        assertThat(updatedContent).isEqualTo(expectedYamlContent)
        assertThat(capturedDiff).isNotNull()
        assertThat(capturedDiff!!).contains("+      chain_one: x\"${BlockchainRid.buildRepeat(1)}\"")
    }

    @Test
    fun `should extend networks section with new chain deployment on new network`() {
        val yamlContent = """
            blockchains:
              chain_zero:
                module: main
            deployments:
              testnet:
                chains:
                  chain_zero: x"${BlockchainRid.ZERO_RID}"
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia.yml").apply {
            writeText(yamlContent)
        }

        var capturedDiff: String? = null
        ChromiaYmlWriter.updateDeploymentNode(yamlFile, "mainnet", "chain_zero", BlockchainRid.ZERO_RID) { capturedDiff = it }

        val expectedYamlContent = """
            blockchains:
              chain_zero:
                module: main
            deployments:
              testnet:
                chains:
                  chain_zero: x"${BlockchainRid.ZERO_RID}"
              mainnet:
                chains:
                  chain_zero: x"${BlockchainRid.ZERO_RID}"

        """.trimIndent()
        val updatedContent = yamlFile.readText()

        assertThat(updatedContent).isEqualTo(expectedYamlContent)
        assertThat(capturedDiff).isNotNull()
        assertThat(capturedDiff!!).contains("+  mainnet:")
        assertThat(capturedDiff!!).contains("+    chains:")
        assertThat(capturedDiff!!).contains("+      chain_zero: x\"${BlockchainRid.ZERO_RID}\"")
    }

    @Test
    fun `incomplete deployment config`() {
        val yamlContent = """
            blockchains:
              chain_zero:
                module: main
            deployments:
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia.yml").apply {
            writeText(yamlContent)
        }

        val res = assertThrows<InvalidChromiaModel> {
            ChromiaYmlWriter.updateDeploymentNode(yamlFile, "testnet", "chain_zero", BlockchainRid.ZERO_RID)
        }
        assertThat(res.message).isEqualTo("Expected 'deployments' to be a mapping node but found ScalarNode")
    }

    @Test
    fun `incomplete networks config`() {
        val yamlContent = """
            blockchains:
              chain_zero:
                module: main
            deployments:
              testnet:
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia.yml").apply {
            writeText(yamlContent)
        }

        val res = assertThrows<InvalidChromiaModel> {
            ChromiaYmlWriter.updateDeploymentNode(yamlFile, "testnet", "chain_zero", BlockchainRid.ZERO_RID)
        }
        assertThat(res.message).isEqualTo("Expected 'testnet' to be a mapping node but found ScalarNode")
    }

    @Test
    fun `should write to included deployments file when deployments uses include tag`() {
        val deploymentsContent = """
            testnet:
              chains:
                chain_zero: x"${BlockchainRid.ZERO_RID}"
        """.trimIndent()

        val yamlContent = """
            blockchains:
              chain_zero:
                module: main
              chain_one:
                module: main
            deployments: !include deployments.yml
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia.yml").apply { writeText(yamlContent) }
        val deploymentFile = File(tempDir.toFile(), "deployments.yml").apply { writeText(deploymentsContent) }

        var capturedDiff: String? = null
        ChromiaYmlWriter.updateDeploymentNode(yamlFile, "testnet", "chain_one", BlockchainRid.buildRepeat(1)) { capturedDiff = it }

        val expectedDeploymentsContent = """
            testnet:
              chains:
                chain_zero: x"${BlockchainRid.ZERO_RID}"
                chain_one: x"${BlockchainRid.buildRepeat(1)}"

        """.trimIndent()

        assertThat(yamlFile.readText()).isEqualTo(yamlContent)
        assertThat(deploymentFile.readText()).isEqualTo(expectedDeploymentsContent)
        assertThat(capturedDiff).isNotNull()
        assertThat(capturedDiff!!).contains("--- deployments.yml")
        assertThat(capturedDiff!!).contains("+    chain_one: x\"${BlockchainRid.buildRepeat(1)}\"")
    }

    @Test
    fun `should throw when included deployments file does not exist`() {
        val yamlContent = """
            blockchains:
              chain_zero:
                module: main
            deployments: !include deployments.yml
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia.yml").apply { writeText(yamlContent) }

        val res = assertThrows<InvalidChromiaModel> {
            ChromiaYmlWriter.updateDeploymentNode(yamlFile, "testnet", "chain_zero", BlockchainRid.ZERO_RID)
        }
        assertThat(res.message).isEqualTo("Included deployments file not found: ${tempDir.resolve("deployments.yml")}")
    }

    @Test
    fun `should throw when included deployments file is with invalid format`() {
        val deploymentsContent = """
            testnet:
        """.trimIndent()

        val yamlContent = """
            blockchains:
              chain_zero:
                module: main
            deployments: !include deployments.yml
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia.yml").apply { writeText(yamlContent) }
        File(tempDir.toFile(), "deployments.yml").apply { writeText(deploymentsContent) }

        val res = assertThrows<InvalidChromiaModel> {
            ChromiaYmlWriter.updateDeploymentNode(yamlFile, "testnet", "chain_one", BlockchainRid.buildRepeat(1))
        }
        assertThat(res.message).isEqualTo("Expected 'testnet' to be a mapping node but found ScalarNode")
    }
}
