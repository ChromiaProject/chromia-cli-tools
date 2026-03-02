package com.chromia.build.tools.test.sql

fun List<SqlStatisticsEntry>.htmlSqlLogReport(name: String, sqlLoggingType: SqlLoggingType): String {
    val filtered = filter { shouldLogEntry(it, sqlLoggingType) }
    val groupedByTestCase = filtered.groupBy { it.testCaseName ?: "(no test case)" }
    val orderedGroups = groupedByTestCase.entries
        .sortedBy { (_, entries) -> entries.minOf { it.event.startTimeMs } }

    val totalCount = filtered.size
    val totalDurationMs = filtered.sumOf { it.event.durationMs }

    val top10 = filtered.sortedByDescending { it.event.durationMs }.take(10)

    return buildString {
        appendLine(
            """<!DOCTYPE html>
<html lang="en">§
<head>
<meta charset="UTF-8">
<title>SQL Log Report: $name</title>
<style>
  body { font-family: sans-serif; margin: 2rem; background: #f5f5f5; color: #333; }
  h1 { color: #222; }
  h2 { margin-top: 2rem; color: #444; border-bottom: 2px solid #ddd; padding-bottom: 0.3rem; }
  .summary { background: #fff; padding: 1rem; border-radius: 6px; margin-bottom: 1rem; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
  table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 6px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); margin-bottom: 1.5rem; }
  th { background: #444; color: #fff; padding: 0.5rem 0.75rem; text-align: left; }
  td { padding: 0.4rem 0.75rem; border-bottom: 1px solid #eee; vertical-align: top; font-size: 0.9rem; }
  tr.slow { background: #fffbe6; }
  tr.error { background: #fff0f0; }
  tr.slow.error { background: #fff0e6; }
  .badge-user { background: #2e7d32; color: #fff; border-radius: 3px; padding: 1px 6px; font-size: 0.8rem; }
  .badge-system { background: #888; color: #fff; border-radius: 3px; padding: 1px 6px; font-size: 0.8rem; }
  .status-ok { color: #2e7d32; }
  .status-error { color: #c62828; }
  .group-summary { font-size: 0.85rem; color: #555; margin: 0.4rem 0 0.6rem; }
  pre { margin: 0; white-space: pre-wrap; word-break: break-word; }
</style>
</head>
<body>
<h1>SQL Log Report: $name</h1>
<div class="summary">
  <strong>Total Queries:</strong> $totalCount &nbsp;|&nbsp;
  <strong>Total Time:</strong> ${formatDuration(totalDurationMs)} &nbsp;|&nbsp;
  <strong>Filter:</strong> $sqlLoggingType
</div>"""
        )

        appendLine("<h2>Top 10 Slowest Queries</h2>")
        appendLine(buildQueryTable(top10, includeTestCase = true, startIndex = 1))

        for ((testCaseName, entries) in orderedGroups) {
            val sorted = entries.sortedByDescending { it.event.durationMs }
            val groupTotal = entries.sumOf { it.event.durationMs }
            val groupRows = entries.sumOf { it.event.rowCount ?: 0 }

            appendLine("<h2>${escapeHtml(testCaseName)}</h2>")
            appendLine(
                """<p class="group-summary">Queries: ${entries.size} &nbsp;|&nbsp; Total Time: ${formatDuration(
                    groupTotal
                )} &nbsp;|&nbsp; Total Rows: $groupRows</p>"""
            )
            appendLine(buildQueryTable(sorted, includeTestCase = false, startIndex = 1))
        }

        appendLine("</body>\n</html>")
    }
}

private fun buildQueryTable(entries: List<SqlStatisticsEntry>, includeTestCase: Boolean, startIndex: Int): String = buildString {
    appendLine("<table>")
    appendLine("<thead><tr>")
    appendLine("  <th>#</th><th>Type</th><th>Duration</th>")
    if (includeTestCase) appendLine("  <th>Test Case</th>")
    appendLine("  <th>SQL</th><th>Parameters</th><th>Rows</th><th>Status</th>")
    appendLine("</tr></thead>")
    appendLine("<tbody>")

    entries.forEachIndexed { idx, entry ->
        val slow = entry.event.durationMs > 100
        val hasError = entry.event.error != null
        val rowClass = when {
            slow && hasError -> "slow error"
            slow -> "slow"
            hasError -> "error"
            else -> ""
        }
        val classAttr = if (rowClass.isNotEmpty()) """ class="$rowClass"""" else ""
        val typeBadge = if (entry.queryType == SqlQueryType.USER) {
            """<span class="badge-user">USER</span>"""
        } else {
            """<span class="badge-system">SYSTEM</span>"""
        }

        appendLine("<tr$classAttr>")
        appendLine("  <td>${startIndex + idx}</td>")
        appendLine("  <td>$typeBadge</td>")
        appendLine("  <td>${formatDuration(entry.event.durationMs)}</td>")
        if (includeTestCase) {
            appendLine("  <td>${escapeHtml(entry.testCaseName ?: "(no test case)")}</td>")
        }
        appendLine("  <td><pre>${escapeHtml(entry.event.sql)}</pre></td>")
        appendLine("  <td><pre>${escapeHtml(formatParams(entry.event.parameters))}</pre></td>")
        appendLine("  <td>${entry.event.rowCount ?: "N/A"}</td>")
        appendLine("  <td>${formatStatus(entry.event.error)}</td>")
        appendLine("</tr>")
    }

    appendLine("</tbody></table>")
}

private fun shouldLogEntry(entry: SqlStatisticsEntry, sqlLoggingType: SqlLoggingType) = when (sqlLoggingType) {
    SqlLoggingType.USER -> !entry.event.isSystem
    SqlLoggingType.SYSTEM -> entry.event.isSystem
    SqlLoggingType.BOTH -> true
    SqlLoggingType.NONE -> false
}

private fun formatParams(parameters: List<Any?>): String =
    parameters.joinToString(", ") { it?.toString() ?: "null" }

private fun formatStatus(error: Exception?): String =
    if (error == null) {
        """<span class="status-ok">OK</span>"""
    } else {
        """<span class="status-error">${escapeHtml(error.message ?: "FAILED")}</span>"""
    }

private fun formatDuration(ms: Long): String = "${ms}ms"

private fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
