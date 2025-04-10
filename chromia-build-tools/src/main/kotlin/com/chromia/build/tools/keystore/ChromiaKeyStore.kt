package com.chromia.build.tools.keystore

import net.postchain.common.exception.UserMistake
import net.postchain.crypto.KeyPair
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ChromiaKeyStore(val keyId: String = "chromia_key") {

    val chromiaHome = System.getenv("CHROMIA_HOME") ?: (System.getProperty("user.home") + "/.chromia")
    val publicKeyFile = Path("$chromiaHome/$keyId.pubkey")
    val privateKeyFile = Path("$chromiaHome/$keyId")
    val mnemonicFile = Path("$chromiaHome/${keyId}_mnemonic")

    fun saveKeyPair(keyPair: KeyPair, mnemonic: String? = null): String {
        if (findKeyPair() != null) {
            throw UserMistake("Key pair with keyId: $keyId already exists in $chromiaHome")
        }

        Path(chromiaHome).createDirectories()
        publicKeyFile.writeText(keyPair.pubKey.hex())
        privateKeyFile.writeText(keyPair.privKey.hex())
        if (mnemonic != null) {
            mnemonicFile.writeText(
                    """
                                This is a generated file that contains your mnemonic phrase to recover your keypair with public key ${keyPair.pubKey.hex()}.
                                It is highly recommended that you delete this file from your system once the phrase has been placed in a secure place or moved this file to a secure place. 
                                Mnemonic phrase generated:
                                $mnemonic
                            """.trimIndent()
            )
        }

        println(
                """
            |Keypair is written to $chromiaHome. To use this key pair, set key.id = $keyId in your configuration file
        """.trimMargin()
        )
        return chromiaHome
    }

    fun loadKeyPair(): KeyPair {
        return findKeyPair()
                ?: throw UserMistake("Could not find key pair with key id: $keyId in $chromiaHome")
    }

    fun findKeyPair(): KeyPair? {
        if (publicKeyFile.exists() && privateKeyFile.exists()) {
            return KeyPair.of(publicKeyFile.readText().trim(), privateKeyFile.readText().trim())
        }
        return null
    }
}
