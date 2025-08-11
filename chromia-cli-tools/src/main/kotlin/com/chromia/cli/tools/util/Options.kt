package com.chromia.cli.tools.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
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
