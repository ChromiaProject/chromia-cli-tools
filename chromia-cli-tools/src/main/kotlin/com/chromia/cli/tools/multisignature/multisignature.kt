package com.chromia.cli.tools.multisignature

import com.chromia.build.tools.multisignature.MultiSignatureTxData
import com.chromia.cli.tools.util.getFormattedUtcDateTime
import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.common.PropertiesFileLoader
import net.postchain.crypto.PubKey
import java.io.File

fun getSignersFromFile(file: File): Set<PubKey> {
    val properties = PropertiesFileLoader.load(file.path)
    val signers = mutableListOf<PubKey>()

    val keys = properties.keys
    while (keys.hasNext()) {
        val key = keys.next()
        val value = properties.getString(key)
        try {
            val pubkey = PubKey(value)
            signers.add(pubkey)
        } catch (e: IllegalArgumentException) {
            throw PrintMessage("Failed to add signer for value: $value, reason: ${e.message}. Please verify that your signers file is defined correctly", 1)
        }
    }
    return signers.toSet()
}

fun MultiSignatureTxData.Companion.parseTransactionFile(transactionFile: File): MultiSignatureTxData = try {
    decode(transactionFile.readText().trim())
} catch (e: IllegalArgumentException) {
    logger.error(e) { "Error while parsing transaction file" }
    throw PrintMessage("Transaction file is incompatible with current CLI version", 1)
}

fun MultiSignatureTxData.saveTransactionToFile(outputFolder: File, name: String): File {
    val file = outputFolder.resolve("${name}_${getFormattedUtcDateTime()}")
    file.writeText(encode())
    return file
}
