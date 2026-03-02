package com.chromia.build.tools.test.sql

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
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
    // Interactive controls
    // -------------------------------------------------------------------------

    @Test
    fun `filter buttons for user system and both are present`() {
        val html = emptyList<SqlStatisticsEntry>().htmlSqlLogReport("test", SqlLoggingType.BOTH)

        assertThat(html).contains("""data-filter="BOTH"""")
        assertThat(html).contains("""data-filter="USER"""")
        assertThat(html).contains("""data-filter="SYSTEM"""")
    }

    @Test
    fun `table has sortable duration and test case column headers`() {
        val html = emptyList<SqlStatisticsEntry>().htmlSqlLogReport("test", SqlLoggingType.BOTH)

        assertThat(html).contains("""data-col="durationMs"""")
        assertThat(html).contains("""data-col="testCase"""")
    }

    @Test
    fun `search input is present`() {
        val html = emptyList<SqlStatisticsEntry>().htmlSqlLogReport("test", SqlLoggingType.BOTH)

        assertThat(html).contains("""id="search"""")
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
    fun `NONE filter produces no query data`() {
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
    // JSON data embedding
    // -------------------------------------------------------------------------

    @Test
    fun `entry type is embedded correctly in json data`() {
        val entries = listOf(
            createEntry(sql = "SELECT 1", isSystem = false),
            createEntry(sql = "SELECT 2", isSystem = true),
        )

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains(""""type":"USER"""")
        assertThat(html).contains(""""type":"SYSTEM"""")
    }

    @Test
    fun `test case name is embedded in json data`() {
        val entries = listOf(createEntry(sql = "SELECT 1", testCaseName = "my-test"))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains(""""testCase":"my-test"""")
    }

    @Test
    fun `null test case name defaults to no test case in json`() {
        val entries = listOf(createEntry(sql = "SELECT 1", testCaseName = null))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains(""""testCase":"(no test case)"""")
    }

    @Test
    fun `duration is embedded in json data`() {
        val entries = listOf(createEntry(sql = "SELECT 1", durationMs = 250L))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains(""""durationMs":250""")
    }

    @Test
    fun `non-null row count is embedded in json data`() {
        val entries = listOf(createEntry(sql = "SELECT 1", rowCount = 7))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains(""""rowCount":7""")
    }

    @Test
    fun `null row count is embedded as null in json data`() {
        val entries = listOf(createEntry(sql = "SELECT 1", rowCount = null))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains(""""rowCount":null""")
    }

    @Test
    fun `error message is embedded in json data`() {
        val entries = listOf(createEntry(sql = "SELECT 1", error = Exception("constraint violation")))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains(""""error":"constraint violation"""")
    }

    @Test
    fun `null error is embedded as null in json data`() {
        val entries = listOf(createEntry(sql = "SELECT 1", error = null))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains(""""error":null""")
    }

    // -------------------------------------------------------------------------
    // JSON safety — angle brackets and ampersands must be Unicode-escaped
    // so that content cannot break out of the <script> tag
    // -------------------------------------------------------------------------

    @Test
    fun `angle brackets in sql are unicode-escaped in json`() {
        val entries = listOf(createEntry(sql = "SELECT <b> & 'x'"))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("\\u003cb\\u003e")
        assertThat(html).doesNotContain("<b>")
    }

    @Test
    fun `ampersand in sql is unicode-escaped in json`() {
        val entries = listOf(createEntry(sql = "a & b"))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        assertThat(html).contains("\\u0026")
        assertThat(html).doesNotContain(""""sql":"a & b"""")
    }

    @Test
    fun `script tag in error message cannot escape the script block`() {
        val entries = listOf(createEntry(sql = "SELECT 1", error = Exception("<script>alert(1)</script>")))

        val html = entries.htmlSqlLogReport("rell", SqlLoggingType.BOTH)

        // Angle brackets must be Unicode-escaped; a raw </script> would break the page
        assertThat(html).contains("\\u003cscript\\u003e")
        assertThat(html).doesNotContain("<script>")
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
}
