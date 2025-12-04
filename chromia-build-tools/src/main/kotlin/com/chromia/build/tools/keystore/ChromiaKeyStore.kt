package com.chromia.build.tools.keystore

import com.chromia.build.tools.config.chromiaHome
import mu.KLogging
import net.postchain.common.exception.UserMistake
import net.postchain.crypto.KeyPair
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ChromiaKeyStore(val keyId: String = "chromia_key") {
    companion object : KLogging()

    val publicKeyFile = chromiaHome / "$keyId.pubkey"
    val privateKeyFile = chromiaHome / keyId
    val mnemonicFile = chromiaHome / "${keyId}_mnemonic"

    fun saveKeyPair(keyPair: KeyPair, mnemonic: String? = null): String {
        if (findKeyPair() != null) {
            throw UserMistake("Key pair with keyId: $keyId already exists in $chromiaHome")
        }

        chromiaHome.createDirectories()
        publicKeyFile.writeText(keyPair.pubKey.hex())
        privateKeyFile.writeText(keyPair.privKey.hex())
        try {
            Files.setPosixFilePermissions(privateKeyFile,
                    setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
        } catch (e: UnsupportedOperationException) {
            // we don't expect this to work on Windows
            logger.debug {
                "Could not set permissions on private key file ${privateKeyFile.absolutePathString()}: ${e.message}"
            }
        }
        if (mnemonic != null) {
            mnemonicFile.writeText(
                    """
                                This is a generated file that contains your mnemonic phrase to recover your keypair with public key ${keyPair.pubKey.hex()}.
                                It is highly recommended that you delete this file from your system once the phrase has been placed in a secure place or moved this file to a secure place. 
                                Mnemonic phrase generated:
                                $mnemonic
                            """.trimIndent()
            )
            try {
                Files.setPosixFilePermissions(mnemonicFile,
                        setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
            } catch (e: UnsupportedOperationException) {
                // we don't expect this to work on Windows
                logger.debug {
                    "Could not set permissions on mnemonic file ${mnemonicFile.absolutePathString()}: ${e.message}"
                }
            }
        }

        println(
                """
            |Keypair is written to $chromiaHome. To use this key pair, set key.id = $keyId in your configuration file
        """.trimMargin()
        )
        return chromiaHome.toString()
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
