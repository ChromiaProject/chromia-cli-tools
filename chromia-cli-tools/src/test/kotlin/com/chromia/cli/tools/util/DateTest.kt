package com.chromia.cli.tools.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class DateTest {

    @Test
    fun test() {
        listOf(
                "2025-07-20 19:04",
                "2025-07-20T19:04",
        ).forEach {
            assertThat(parseDateTimeAsEpochMillis(it)).isEqualTo(1753038240000)
        }

        assertThat(parseDateTimeAsEpochMillis("2025-07-20 19:04")).isEqualTo(1753038240000)
    }
}

