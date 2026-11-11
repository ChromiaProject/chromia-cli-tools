package com.chromia.build.tools.keystore

import com.chromia.build.tools.config.chromiaHome
import mu.KLogging
import net.postchain.common.exception.UserMistake
import net.postchain.crypto.KeyPair
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
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
        writeSecretFile(privateKeyFile, keyPair.privKey.hex())
        if (mnemonic != null) {
            writeSecretFile(mnemonicFile,
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
        return chromiaHome.toString()
    }

    /**
     * Writes a secret so that it is never readable by other users, not even for a moment:
     * a new file is created with owner-only permissions before any content is written,
     * and an existing file is restricted first and then overwritten.
     */
    private fun writeSecretFile(file: Path, content: String) {
        val ownerOnly = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
        try {
            if (file.exists()) {
                Files.setPosixFilePermissions(file, ownerOnly)
            } else {
                Files.createFile(file, PosixFilePermissions.asFileAttribute(ownerOnly))
            }
        } catch (e: UnsupportedOperationException) {
            // we don't expect this to work on Windows
            logger.debug {
                "Could not set permissions on secret file ${file.absolutePathString()}: ${e.message}"
            }
        }
        file.writeText(content)
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
