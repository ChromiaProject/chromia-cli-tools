package com.chromia.build.tools.test.sql

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isTrue
import net.postchain.rell.api.gtx.SqlExecutionEvent
import org.junit.jupiter.api.Test

class SqlQueriesReportTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun createEntry(
        sql: String,
        parameters: List<Any?> = emptyList(),
        startTimeMs: Long = 1000L,
        durationMs: Long = 10L,
        rowCount: Int? = null,
        isSystem: Boolean = false,
        error: Exception? = null,
        testCaseName: String? = null,
    ) = SqlStatisticsEntry(
        event = SqlExecutionEvent(
            sql = sql,
            parameters = parameters,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            rowCount = rowCount,
            isSystem = isSystem,
            error = error,
        ),
        testCaseName = testCaseName,
    )

    // -------------------------------------------------------------------------
    // HTML structure / header
    // -------------------------------------------------------------------------

    @Test
    fun `report title contains provided name`() {
        val html = emptyList<SqlStatisticsEntry>().htmlSqlLogReport("my-chain", SqlLoggingType.BOTH)

        assertThat(html).contains("<title>SQL Log Report: my-chain</title>")
        assertThat(html).contains("<h1>SQL Log Report: my-chain</h1>")
    }

    @Test
    fun `summary shows total count total duration and filter type`() {
        val entries = listOf(
            createEntry(sql = "SELECT 1", durationMs = 30L),
            createEntry(sql = "SELECT 2", durationMs = 70L),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("Total Queries:</strong> 2")
        assertThat(html).contains("Total Time:</strong> 100ms")
        assertThat(html).contains("Filter:</strong> BOTH")
    }

    // -------------------------------------------------------------------------
    // Filtering by SqlLoggingType
    // -------------------------------------------------------------------------

    @Test
    fun `USER filter excludes system queries`() {
        val entries = listOf(
            createEntry(sql = "SELECT user", isSystem = false),
            createEntry(sql = "SELECT system", isSystem = true),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.USER)

        assertThat(html).contains("SELECT user")
        assertThat(html).doesNotContain("SELECT system")
    }

    @Test
    fun `SYSTEM filter excludes user queries`() {
        val entries = listOf(
            createEntry(sql = "SELECT user", isSystem = false),
            createEntry(sql = "SELECT system", isSystem = true),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.SYSTEM)

        assertThat(html).doesNotContain("SELECT user")
        assertThat(html).contains("SELECT system")
    }

    @Test
    fun `BOTH filter includes all queries`() {
        val entries = listOf(
            createEntry(sql = "SELECT user", isSystem = false),
            createEntry(sql = "SELECT system", isSystem = true),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("SELECT user")
        assertThat(html).contains("SELECT system")
    }

    @Test
    fun `NONE filter produces empty tables`() {
        val entries = listOf(
            createEntry(sql = "SELECT user", isSystem = false),
            createEntry(sql = "SELECT system", isSystem = true),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.NONE)

        assertThat(html).doesNotContain("SELECT user")
        assertThat(html).doesNotContain("SELECT system")
        assertThat(html).contains("Total Queries:</strong> 0")
    }

    // -------------------------------------------------------------------------
    // Grouping by test case
    // -------------------------------------------------------------------------

    @Test
    fun `each test case gets its own section heading`() {
        val entries = listOf(
            createEntry(sql = "SELECT 1", testCaseName = "alpha"),
            createEntry(sql = "SELECT 2", testCaseName = "beta"),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("<h2>alpha</h2>")
        assertThat(html).contains("<h2>beta</h2>")
    }

    @Test
    fun `null test case name is shown as no test case`() {
        val entries = listOf(createEntry(sql = "SELECT 1", testCaseName = null))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("<h2>(no test case)</h2>")
    }

    @Test
    fun `group summary shows correct query count total time and rows`() {
        val entries = listOf(
            createEntry(sql = "SELECT 1", durationMs = 40L, rowCount = 2, testCaseName = "tc"),
            createEntry(sql = "SELECT 2", durationMs = 60L, rowCount = 3, testCaseName = "tc"),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("Queries: 2")
        assertThat(html).contains("Total Time: 100ms")
        assertThat(html).contains("Total Rows: 5")
    }

    // -------------------------------------------------------------------------
    // Sorting within groups (slowest first)
    // -------------------------------------------------------------------------

    @Test
    fun `queries within a group are sorted slowest first`() {
        val entries = listOf(
            createEntry(sql = "fast query", durationMs = 10L, startTimeMs = 1000L, testCaseName = "tc"),
            createEntry(sql = "slow query", durationMs = 500L, startTimeMs = 2000L, testCaseName = "tc"),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        // "slow query" should appear before "fast query" in the group table
        assertThat(html.indexOf("slow query") < html.indexOf("fast query")).isTrue()
    }

    // -------------------------------------------------------------------------
    // Top 10 slowest queries
    // -------------------------------------------------------------------------

    @Test
    fun `top 10 section contains the slowest queries across all test cases`() {
        val entries = listOf(
            createEntry(sql = "slowest", durationMs = 1000L, testCaseName = "tc1"),
            createEntry(sql = "second slowest", durationMs = 900L, testCaseName = "tc2"),
            createEntry(sql = "fast", durationMs = 1L, testCaseName = "tc1"),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        // Top 10 heading
        assertThat(html).contains("<h2>Top 10 Slowest Queries</h2>")
        // Slowest entries appear
        assertThat(html).contains("slowest")
        assertThat(html).contains("second slowest")
    }

    @Test
    fun `top 10 section includes test case column`() {
        val entries = listOf(
            createEntry(sql = "SELECT 1", durationMs = 200L, testCaseName = "myTestCase"),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        // The top-10 table has a "Test Case" header
        assertThat(html).contains("<th>Test Case</th>")
        // And the test case name appears in the top-10 table rows
        assertThat(html).contains("myTestCase")
    }

    @Test
    fun `top 10 is limited to ten entries`() {
        val entries = (1..15).map { i ->
            createEntry(sql = "query_$i", durationMs = i.toLong() * 10, testCaseName = "tc")
        }

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        // query_1 is the slowest when reversed: query_15 > ... > query_6 are top 10; query_5 and below are excluded
        assertThat(html.indexOf("Top 10 Slowest Queries") < html.indexOf("query_15")).isTrue()
        // query_1 (durationMs=10, rank 15th) should not appear in the top-10 table
        // It still appears in the per-group section, but we verify the section ordering
        val top10End = html.indexOf("<h2>tc</h2>")
        val top10Section = html.substring(html.indexOf("Top 10 Slowest Queries"), top10End)
        assertThat(top10Section).doesNotContain("query_1\"")
    }

    // -------------------------------------------------------------------------
    // Row class styling
    // -------------------------------------------------------------------------

    @Test
    fun `slow query rows have slow class`() {
        val entries = listOf(createEntry(sql = "SELECT slow", durationMs = 101L))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("""class="slow"""")
    }

    @Test
    fun `fast query rows have no row class`() {
        val entries = listOf(createEntry(sql = "SELECT fast", durationMs = 50L))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).doesNotContain("""class="slow"""")
        assertThat(html).doesNotContain("""class="error"""")
    }

    @Test
    fun `error query rows have error class`() {
        val entries = listOf(createEntry(sql = "SELECT bad", error = Exception("oops")))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("""class="error"""")
    }

    @Test
    fun `slow and error query rows have combined class`() {
        val entries = listOf(createEntry(sql = "SELECT bad slow", durationMs = 200L, error = Exception("fail")))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("""class="slow error"""")
    }

    // -------------------------------------------------------------------------
    // Type badges
    // -------------------------------------------------------------------------

    @Test
    fun `user queries show badge-user badge`() {
        val entries = listOf(createEntry(sql = "SELECT 1", isSystem = false))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("""class="badge-user">USER</span>""")
    }

    @Test
    fun `system queries show badge-system badge`() {
        val entries = listOf(createEntry(sql = "SELECT 1", isSystem = true))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("""class="badge-system">SYSTEM</span>""")
    }

    // -------------------------------------------------------------------------
    // HTML escaping
    // -------------------------------------------------------------------------

    @Test
    fun `special characters in SQL are escaped`() {
        val entries = listOf(createEntry(sql = "SELECT <b> & \"quotes\""))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("SELECT &lt;b&gt; &amp; &quot;quotes&quot;")
        assertThat(html).doesNotContain("SELECT <b>")
    }

    @Test
    fun `special characters in error message are escaped`() {
        val entries = listOf(createEntry(sql = "SELECT 1", error = Exception("<script>alert(1)</script>")))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;")
        assertThat(html).doesNotContain("<script>")
    }

    @Test
    fun `special characters in test case name are escaped`() {
        val entries = listOf(createEntry(sql = "SELECT 1", testCaseName = "test<case>&name"))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("<h2>test&lt;case&gt;&amp;name</h2>")
    }

    // -------------------------------------------------------------------------
    // Parameters formatting
    // -------------------------------------------------------------------------

    @Test
    fun `parameters are joined with comma separator`() {
        val entries = listOf(createEntry(sql = "SELECT ?", parameters = listOf("alice", 42)))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("alice, 42")
    }

    @Test
    fun `null parameters are shown as null`() {
        val entries = listOf(createEntry(sql = "SELECT ?", parameters = listOf(null, "val")))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("null, val")
    }

    // -------------------------------------------------------------------------
    // Status cell
    // -------------------------------------------------------------------------

    @Test
    fun `successful queries show OK status`() {
        val entries = listOf(createEntry(sql = "SELECT 1", error = null))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("""class="status-ok">OK</span>""")
    }

    @Test
    fun `failed queries show error message in status`() {
        val entries = listOf(createEntry(sql = "SELECT 1", error = Exception("constraint violation")))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("""class="status-error">constraint violation</span>""")
    }

    // -------------------------------------------------------------------------
    // Row count
    // -------------------------------------------------------------------------

    @Test
    fun `null row count is shown as N slash A`() {
        val entries = listOf(createEntry(sql = "SELECT 1", rowCount = null))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("<td>N/A</td>")
    }

    @Test
    fun `non-null row count is shown as number`() {
        val entries = listOf(createEntry(sql = "SELECT 1", rowCount = 7))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("<td>7</td>")
    }

    // -------------------------------------------------------------------------
    // Test case ordering by first query start time
    // -------------------------------------------------------------------------

    @Test
    fun `test cases are ordered by the start time of their first query`() {
        val entries = listOf(
            createEntry(sql = "from-beta", startTimeMs = 2000L, testCaseName = "beta"),
            createEntry(sql = "from-alpha", startTimeMs = 1000L, testCaseName = "alpha"),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html.indexOf("<h2>alpha</h2>") < html.indexOf("<h2>beta</h2>")).isTrue()
    }
}
