package com.chromia.build.tools.keystore

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import net.postchain.common.exception.UserMistake
import net.postchain.crypto.KeyPair
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

class ChromiaKeyStoreTest {

    private val keyPair = KeyPair.of("03CE4656584DBBAE56CE27C4D97F91A68DE0A789FA9844D589FEB87C5F2A17DADC",
            "EA5148DD724F33FBDD911CDFE58B14CCC0DFD96D7F30E966975FA2B2C8BDD0C8")
    private val mnemonic =
            "section easy total social evoke title opera street firm master aim spare chair bronze venture edge increase problem sentence panda science draft soup vicious"

    @TempDir
    private lateinit var testDir: Path
    private lateinit var chromiaKeyStore: ChromiaKeyStore

    @BeforeEach
    fun setupKeyStory() {
        EnvironmentVariables("CHROMIA_HOME", testDir.absolutePathString()).execute {
            chromiaKeyStore = ChromiaKeyStore()
        }
    }

    @Test
    fun `Save and load key pair flow`() {
        chromiaKeyStore.saveKeyPair(keyPair, mnemonic)
        val loadedKeyPair = chromiaKeyStore.loadKeyPair()
        assertThat(chromiaKeyStore.publicKeyFile.readText()).contains(keyPair.pubKey.hex())
        assertThat(chromiaKeyStore.privateKeyFile.readText()).contains(keyPair.privKey.hex())
        assertThat(chromiaKeyStore.mnemonicFile.readText()).contains(mnemonic)

        assertThat(loadedKeyPair.pubKey.hex()).isEqualTo(keyPair.pubKey.hex())
        assertThat(loadedKeyPair.privKey.hex()).isEqualTo(keyPair.privKey.hex())
    }

    @Test
    fun `Save and find key pair flow`() {
        assertThat(chromiaKeyStore.findKeyPair()).isNull()
        chromiaKeyStore.saveKeyPair(keyPair)
        val foundKeyPair = chromiaKeyStore.findKeyPair()
        assertThat(foundKeyPair).isNotNull()
        assertThat(foundKeyPair!!.pubKey.hex()).isEqualTo(keyPair.pubKey.hex())
        assertThat(foundKeyPair.privKey.hex()).isEqualTo(keyPair.privKey.hex())
    }

    @Test
    fun `loadKeyPair throws when load key pair do not find expected keys`() {
        val throwable = assertThrows<UserMistake> { chromiaKeyStore.loadKeyPair() }
        assertThat(throwable.message).isEqualTo("Could not find key pair with key id: chromia_key in $testDir")
    }

    @Test
    fun `saveKeyPair throws when key pair with same id already exists`() {
        chromiaKeyStore.saveKeyPair(keyPair)
        val throwable = assertThrows<UserMistake> { chromiaKeyStore.saveKeyPair(keyPair) }
        assertThat(throwable.message).isEqualTo("Key pair with keyId: chromia_key already exists in $testDir")
    }
}
