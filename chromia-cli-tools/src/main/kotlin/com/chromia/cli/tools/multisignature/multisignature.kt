package com.chromia.cli.tools.multisignature

import com.chromia.build.tools.multisignature.MultiSignatureTxData
import com.chromia.cli.tools.util.getFormattedUtcDateTime
import com.github.ajalt.clikt.core.PrintMessage
import java.io.File

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
