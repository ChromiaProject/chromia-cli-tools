package com.chromia.build.tools.test.sql

import net.postchain.rell.api.gtx.SqlExecutionEvent

data class SqlStatisticsEntry(
    val event: SqlExecutionEvent,
    val testCaseName: String?,
)
