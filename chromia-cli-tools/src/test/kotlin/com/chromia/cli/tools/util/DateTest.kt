package com.chromia.cli.tools.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class DateTest {

    @Test
    fun success() {
        listOf(
                "2025-07-20 19:04",
                "2025-07-20T19:04",
        ).forEach {
            assertThat(parseDateTimeAsEpochMillis(it)).isEqualTo(1753038240000)
        }
    }

    @Test
    fun failure() {
        listOf(
                "x2025-07-20 19:04",
                "Wednesday, July 20, 2025 19:04",
        ).forEach {
            assertThat(parseDateTimeAsEpochMillis(it)).isNull()
        }
    }
}
