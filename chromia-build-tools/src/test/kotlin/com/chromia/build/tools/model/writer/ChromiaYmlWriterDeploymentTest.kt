package com.chromia.build.tools.model.writer

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show
import com.chromia.build.tools.lib.updateChromiaYamlForLibrary
import net.postchain.common.BlockchainRid
import org.junit.jupiter.api.Test
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

        ChromiaYmlWriter.updateDeploymentNode(yamlFile, "testnet", "chain_zero", BlockchainRid.ZERO_RID)

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
    }
}
