package com.chromia.cli.tools.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.time.Instant

class FormattedTimeTest {
    @Test
    fun `get formatted UTC date and time`() {
        val fixedInstant = Instant.parse("2024-10-03T12:34:56.123456789Z")
        val expectedFormattedDateTime = "2024-10-03T12.34.56.123456789Z"

        val formattedDateTime = getFormattedUtcDateTime(fixedInstant)

        assertThat(formattedDateTime).isEqualTo(expectedFormattedDateTime)
    }
}