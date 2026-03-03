package com.chromia.build.tools.test.sql

import com.google.gson.GsonBuilder

private data class SqlReport(
    val idx: Int,
    val type: String,
    val durationMs: Long,
    val testCase: String,
    val sql: String,
    val params: String,
    val rowCount: Int?,
    val error: String?
)

fun List<SqlStatisticsEntry>.htmlSqlLogReport(name: String): String {
    val entries = this
    val totalCount = entries.size
    val totalDurationMs = entries.sumOf { it.event.durationMs }

    val sqlReport = entries.mapIndexed { idx, entry ->
        SqlReport(
            idx = idx + 1,
            type = if (entry.event.isSystem) "SYSTEM" else "USER",
            durationMs = entry.event.durationMs,
            testCase = entry.testCaseName ?: "(no test case)",
            sql = entry.event.sql,
            params = formatParams(entry.event.parameters),
            rowCount = entry.event.rowCount,
            error = entry.event.error?.let { it.message ?: "FAILED" }
        )
    }
    val gson = GsonBuilder().serializeNulls().create()
    val jsonData = gson.toJson(sqlReport)

    val template = checkNotNull(
        SqlStatisticsEntry::class.java.getResource(
            "/com/chromia/build/tools/test/sql/sqlQueriesReportTemplate.html"
        )
    ) { "sqlQueriesReport.html template resource not found" }
        .readText()

    return template
        .replace("{{NAME}}", name)
        .replace("{{TOTAL_COUNT}}", totalCount.toString())
        .replace("{{TOTAL_DURATION}}", formatDuration(totalDurationMs))
        .replace("{{JSON_DATA}}", jsonData)
}

private fun formatParams(parameters: List<Any?>): String =
    parameters.joinToString(", ") { it?.toString() ?: "null" }

private fun formatDuration(ms: Long): String = "${ms}ms"
