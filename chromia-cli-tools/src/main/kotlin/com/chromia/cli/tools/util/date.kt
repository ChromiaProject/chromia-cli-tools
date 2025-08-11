package com.chromia.cli.tools.util

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

val DATE_TIME_FORMATS = listOf(
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm",
).associateWith { DateTimeFormatter.ofPattern(it) }

val SUPPORTED_TIME_AT_FORMATS = "Supported formats: ${DATE_TIME_FORMATS.keys.joinToString(", ")} and milliseconds since 1970 (unix/epoch time)"

fun timeAtConverter(input: String): Long =
        if (input.all { it.isDigit() } && input.length == 13) {
            input.toLong()
        } else {
            parseDateTimeAsEpochMillis(input, DATE_TIME_FORMATS.values.toList())
                    ?: throw IllegalArgumentException("Invalid time format: $input supported formats: ${DATE_TIME_FORMATS.keys.joinToString(", ")} and milliseconds since 1970 (unix/epoch time)")
        }

fun parseDateTime(time: String, formats: List<DateTimeFormatter> = DATE_TIME_FORMATS.values.toList()): LocalDateTime? {
    for (formatter in formats) {
        try {
            return LocalDateTime.parse(time, formatter)
        } catch (_: DateTimeParseException) {
        }
    }
    return null
}

fun parseDateTimeAsEpochMillis(time: String, formats: List<DateTimeFormatter> = DATE_TIME_FORMATS.values.toList()): Long? {
    return parseDateTime(time, formats)?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
}
