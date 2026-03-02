package com.chromia.build.tools.test.sql

fun List<SqlStatisticsEntry>.htmlSqlLogReport(name: String): String {
    val totalCount = size
    val totalDurationMs = sumOf { it.event.durationMs }

    val jsonData = buildString {
        append("[")
        this@htmlSqlLogReport.forEachIndexed { idx, entry ->
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

    return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>SQL Log Report: $name</title>
<style>
  body { font-family: sans-serif; margin: 2rem; background: #f5f5f5; color: #333; }
  h1 { color: #222; }
  .summary { background: #fff; padding: 1rem; border-radius: 6px; margin-bottom: 1rem; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
  .controls { display: flex; gap: 1rem; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; }
  .filter-group { display: flex; gap: 0.3rem; }
  .filter-btn { padding: 0.3rem 0.8rem; border: 1px solid #999; border-radius: 4px; background: #fff; cursor: pointer; font-size: 0.9rem; transition: background 0.15s; }
  .filter-btn.active { background: #444; color: #fff; border-color: #444; }
  .filter-btn:hover:not(.active) { background: #f0f0f0; }
  #search { padding: 0.3rem 0.6rem; border: 1px solid #ccc; border-radius: 4px; font-size: 0.9rem; width: 250px; }
  #visible-count { font-size: 0.9rem; color: #666; }
  table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 6px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); margin-bottom: 1.5rem; }
  th { background: #444; color: #fff; padding: 0.5rem 0.75rem; text-align: left; white-space: nowrap; }
  th.sortable { cursor: pointer; user-select: none; }
  th.sortable:hover { background: #555; }
  th.sort-asc::after { content: " \25B2"; font-size: 0.75rem; }
  th.sort-desc::after { content: " \25BC"; font-size: 0.75rem; }
  td { padding: 0.4rem 0.75rem; border-bottom: 1px solid #eee; vertical-align: top; font-size: 0.9rem; }
  tr.slow { background: #fffbe6; }
  tr.error { background: #fff0f0; }
  tr.slow.error { background: #fff0e6; }
  .badge-user { background: #2e7d32; color: #fff; border-radius: 3px; padding: 1px 6px; font-size: 0.8rem; }
  .badge-system { background: #888; color: #fff; border-radius: 3px; padding: 1px 6px; font-size: 0.8rem; }
  .status-ok { color: #2e7d32; }
  .status-error { color: #c62828; font-size: 0.85rem; }
  pre { margin: 0; white-space: pre-wrap; word-break: break-word; max-width: 420px; }
  .no-results { text-align: center; padding: 2rem; color: #888; font-style: italic; }
</style>
</head>
<body>
<h1>SQL Log Report: $name</h1>
<div class="summary">
  <strong>Total Queries:</strong> $totalCount &nbsp;|&nbsp;
  <strong>Total Time:</strong> ${formatDuration(totalDurationMs)}
</div>
<div class="controls">
  <div class="filter-group">
    <button class="filter-btn active" data-filter="BOTH">Both</button>
    <button class="filter-btn" data-filter="USER">User</button>
    <button class="filter-btn" data-filter="SYSTEM">System</button>
  </div>
  <input type="text" id="search" placeholder="Search SQL or test case...">
  <span id="visible-count"></span>
</div>
<table>
  <thead>
    <tr>
      <th class="sortable" data-col="idx">#</th>
      <th>Type</th>
      <th class="sortable" data-col="durationMs">Duration</th>
      <th class="sortable" data-col="testCase">Test Case</th>
      <th>SQL</th>
      <th>Parameters</th>
      <th class="sortable" data-col="rowCount">Rows</th>
      <th>Status</th>
    </tr>
  </thead>
  <tbody id="tbody"></tbody>
</table>
<script>
var DATA = $jsonData;
var currentFilter = 'BOTH';
var sortCol = 'durationMs';
var sortDir = -1;
var searchVal = '';

function escHtml(s) {
  if (!s) return '';
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function renderTable() {
  var rows = DATA.filter(function(d) {
    if (currentFilter === 'USER' && d.type !== 'USER') return false;
    if (currentFilter === 'SYSTEM' && d.type !== 'SYSTEM') return false;
    if (searchVal) {
      var q = searchVal.toLowerCase();
      if (d.sql.toLowerCase().indexOf(q) === -1 && d.testCase.toLowerCase().indexOf(q) === -1) return false;
    }
    return true;
  });

  rows.sort(function(a, b) {
    var av = a[sortCol], bv = b[sortCol];
    if (av === null && bv === null) return 0;
    if (av === null) return sortDir;
    if (bv === null) return -sortDir;
    if (typeof av === 'string') return av.localeCompare(bv) * sortDir;
    return (av - bv) * sortDir;
  });

  var html = '';
  rows.forEach(function(d, i) {
    var slow = d.durationMs > 100;
    var hasErr = d.error !== null;
    var cls = slow && hasErr ? ' class="slow error"' : slow ? ' class="slow"' : hasErr ? ' class="error"' : '';
    var badge = d.type === 'USER'
      ? '<span class="badge-user">USER</span>'
      : '<span class="badge-system">SYSTEM</span>';
    var status = d.error === null
      ? '<span class="status-ok">OK</span>'
      : '<span class="status-error">' + escHtml(d.error) + '</span>';
    html += '<tr' + cls + '>';
    html += '<td>' + (i + 1) + '</td>';
    html += '<td>' + badge + '</td>';
    html += '<td>' + d.durationMs + 'ms</td>';
    html += '<td><pre>' + escHtml(d.testCase) + '</pre></td>';
    html += '<td><pre>' + escHtml(d.sql) + '</pre></td>';
    html += '<td><pre>' + escHtml(d.params) + '</pre></td>';
    html += '<td>' + (d.rowCount !== null ? d.rowCount : 'N/A') + '</td>';
    html += '<td>' + status + '</td>';
    html += '</tr>';
  });

  if (rows.length === 0) {
    html = '<tr><td colspan="8" class="no-results">No results found.</td></tr>';
  }

  document.getElementById('tbody').innerHTML = html;
  document.getElementById('visible-count').textContent = 'Showing ' + rows.length + ' of $totalCount';

  document.querySelectorAll('th.sortable').forEach(function(th) {
    th.classList.remove('sort-asc', 'sort-desc');
    if (th.getAttribute('data-col') === sortCol) {
      th.classList.add(sortDir === 1 ? 'sort-asc' : 'sort-desc');
    }
  });
}

document.querySelectorAll('.filter-btn').forEach(function(btn) {
  btn.addEventListener('click', function() {
    document.querySelectorAll('.filter-btn').forEach(function(b) { b.classList.remove('active'); });
    btn.classList.add('active');
    currentFilter = btn.getAttribute('data-filter');
    renderTable();
  });
});

document.querySelectorAll('th.sortable').forEach(function(th) {
  th.addEventListener('click', function() {
    var col = th.getAttribute('data-col');
    if (sortCol === col) {
      sortDir = -sortDir;
    } else {
      sortCol = col;
      sortDir = (col === 'durationMs' || col === 'rowCount') ? -1 : 1;
    }
    renderTable();
  });
});

document.getElementById('search').addEventListener('input', function() {
  searchVal = this.value;
  renderTable();
});

renderTable();
</script>
</body>
</html>"""
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
