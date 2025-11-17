package com.chromia.cli.tools.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.common.hexStringToByteArray
import net.postchain.crypto.PubKey
import java.time.Clock

fun CliktCommand.thresholdOption() = option(
    "-t",
    "--threshold",
    help = """
        0: supermajority of voters, specifically  `n - (n - 1) / 3` (which is usually around 67%)
        -1: simple majority
        positive number: that many voters
    """.trimIndent()
).long()

fun CliktCommand.timebOptions(clock: Clock) = mutuallyExclusiveOptions(
        option("--timeb-at", help = "Add timeb operation to make transaction fail if applied after the given time (UTC). $SUPPORTED_TIME_AT_FORMATS")
                .convert { timeAtConverter(it) },
        option("--timeb-after", help = "Add timeb operation to make transaction fail if applied after the given number of seconds from now.")
                .convert { clock.millis() + (it.toLong() * 1000L) } // convert seconds to milliseconds
).single()

fun CliktCommand.slowDBStatementLogMsOption() = option("-sdbl", "--slow-db-statement-log-ms",
    help = "Threshold for slow DB statement log in milliseconds")
    .long()

fun ParameterHolder.signersOption(name: String = "Signers, leave out this option to send transaction directly") = mutuallyExclusiveOptions(
        option("--signer", help = "Public keys of signer (can be repeated)")
                .convert { PubKey(it.hexStringToByteArray()) }
                .multiple().unique(),
        option("--signers", help = "Comma separated list of keys of signers")
                .convert { s -> s.split(",").map { PubKey(it.hexStringToByteArray()) }.toSet() },
        option("--signers-file", help = "Path to file containing public keys of signers, one per line")
                .file(canBeDir = false, mustExist = true, mustBeReadable = true)
                .convert { file -> file.readLines().map { PubKey(it.hexStringToByteArray()) }.toSet() },
        name = name,
).single()
