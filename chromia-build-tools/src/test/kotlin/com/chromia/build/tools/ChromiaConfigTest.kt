package com.chromia.build.tools

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.chromia.build.tools.config.ChromiaConfigLoader
import com.chromia.build.tools.config.ChromiaConfigWriter
import com.chromia.build.tools.config.SUPPRESS_KEY_STORAGE_DEPRECATION_WARNING_SYSTEM_PROPERTY
import com.chromia.build.tools.keystore.ChromiaKeyStore
import net.postchain.crypto.KeyPair
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.properties.SystemProperties
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class ChromiaConfigTest {
    val pubKey = "02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765"
    val privKey = "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429"
    val keyPair = KeyPair.of(pubKey, privKey)
    val keyIdName = "keyIdUsedForTesting"

    @Test
    fun `Keystore key gets populated in config`(@TempDir dir: Path) {
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            ChromiaKeyStore(keyIdName).saveKeyPair(keyPair)
            ChromiaConfigWriter.global.setKeyId(keyIdName)
            val clientConfig = ChromiaConfigLoader { _ -> }.loadClientConfigFile()
            assertThat(clientConfig.signers.size).isEqualTo(1)
            assertThat(clientConfig.signers.first().pubKey.hex()).isEqualTo(pubKey)
            assertThat(clientConfig.signers.first().privKey.hex()).isEqualTo(privKey)
        }
    }

    @Test
    fun `Global Keystore key gets overridden by manual reference having pub and priv key`(@TempDir dir: Path) {
        val test = dir.resolve("sub")
        testData(dir) {
            secret {
                createFile(test)
            }
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            ChromiaKeyStore(keyIdName).saveKeyPair(keyPair)
            ChromiaConfigWriter.global.setKeyId(keyIdName)
            val clientConfig = ChromiaConfigLoader { _ -> }.loadClientConfigFile(
                test.resolve(".chromia/config").toFile()
            )
            assertThat(clientConfig.signers.size).isEqualTo(1)
            assertThat(clientConfig.signers.first().pubKey.hex()).isEqualTo(TestDataBuilder.keyPair.pubKey.hex())
            assertThat(clientConfig.signers.first().privKey.hex()).isEqualTo(TestDataBuilder.keyPair.privKey.hex())
        }
    }

    @Test
    fun `Global Keystore key gets overridden by manual reference having key id`(@TempDir dir: Path) {
        val test = dir.resolve("sub")
        val explicitKeyId = "explicitKeyId"
        File(test.toFile(), "/config").also {
            it.parentFile.mkdirs()
        }.writeText("key.id = $explicitKeyId")

        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            ChromiaKeyStore(explicitKeyId).saveKeyPair(TestDataBuilder.keyPair)
            ChromiaKeyStore(keyIdName).saveKeyPair(keyPair)
            ChromiaConfigWriter.global.setKeyId(keyIdName)
            val clientConfig = ChromiaConfigLoader { _ -> }.loadClientConfigFile(test.resolve("config").toFile())
            assertThat(clientConfig.signers.size).isEqualTo(1)
            assertThat(clientConfig.signers.first().pubKey.hex()).isEqualTo(TestDataBuilder.keyPair.pubKey.hex())
            assertThat(clientConfig.signers.first().privKey.hex()).isEqualTo(TestDataBuilder.keyPair.privKey.hex())
        }
    }

    @Test
    fun `Warning is logged when config file contains pub and priv key explicitly`(@TempDir dir: Path) {
        val test = dir.resolve("sub")
        testData(dir) {
            secret {
                createFile(test)
            }
        }
        val output = mutableListOf<String>()
        val logger = { message: String ->
            output.add(message)
            Unit
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            ChromiaConfigLoader(logger).loadClientConfigFile(test.resolve(".chromia/config").toFile())
            assertThat(output).contains(
                """
                    WARNING: The properties 'pubkey', 'privkey' are currently marked as deprecated.
                    We're standardizing our key management approach. This method of storing keys will be removed in future versions.
                    Please migrate to using a key ID or store this data in a secret file instead.
                """.trimIndent()
            )
        }
    }

    @Test
    fun `Logging warning skipped when suppression system property is set to true`(@TempDir dir: Path) {
        val test = dir.resolve("sub")
        testData(dir) {
            secret {
                createFile(test)
            }
        }
        val output = mutableListOf<String>()
        val logger = { message: String ->
            output.add(message)
            Unit
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            SystemProperties(SUPPRESS_KEY_STORAGE_DEPRECATION_WARNING_SYSTEM_PROPERTY, "true").execute {
                ChromiaConfigLoader(logger).loadClientConfigFile(test.resolve(".chromia/config").toFile())
            }
            assertThat(output).doesNotContain(
                """
                    WARNING: The properties 'pubkey', 'privkey' are currently marked as deprecated.
                    We're standardizing our key management approach. This method of storing keys will be removed in future versions.
                    Please migrate to using a key ID or store this data in a secret file instead.
                """.trimIndent()
            )
        }
    }
}
