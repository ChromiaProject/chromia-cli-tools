package com.chromia.build.tools.test.sql

fun List<SqlStatisticsEntry>.htmlSqlLogReport(name: String): String {
    val entries = this
    val totalCount = entries.size
    val totalDurationMs = entries.sumOf { it.event.durationMs }

    val jsonData = buildString {
        append("[")
        entries.forEachIndexed { idx, entry ->
            if (idx > 0) append(",")
            val errMsg = entry.event.error?.let { it.message ?: "FAILED" }
            append("{")
            append("\"idx\":${idx + 1},")
            append("\"type\":\"${if (entry.event.isSystem) "SYSTEM" else "USER"}\",")
            append("\"durationMs\":${entry.event.durationMs},")
            append("\"testCase\":${jsonString(entry.testCaseName ?: "(no test case)")},")
            append("\"sql\":${jsonString(entry.event.sql)},")
            append("\"params\":${jsonString(formatParams(entry.event.parameters))},")
            append("\"rowCount\":${entry.event.rowCount ?: "null"},")
            append("\"error\":${if (errMsg != null) jsonString(errMsg) else "null"}")
            append("}")
        }
        append("]")
    }

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

private fun jsonString(s: String): String = buildString {
    append('"')
    for (c in s) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '<' -> append("\\u003c")
            '>' -> append("\\u003e")
            '&' -> append("\\u0026")
            else -> if (c.code < 0x20) append("\\u${c.code.toString(16).padStart(4, '0')}") else append(c)
        }
    }
    append('"')
}
