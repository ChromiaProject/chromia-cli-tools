package com.chromia.build.tools.test.sql

import net.postchain.rell.api.gtx.SqlExecutionEvent
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.random.Random

/**
 * Generates a production-like HTML report with 100+ SQL events.
 * Run this test and open the printed file path in a browser to preview the report.
 */
class SqlQueriesReportVisualTest {

    @Test
    @Disabled("Only used to manually check generated sql report html file")
    fun `generate production-like html report`() {
        val rng = Random(42)
        val entries = buildRealisticEntries(rng)

        val html = entries.htmlSqlLogReport("dapp-mainnet")

        val outDir = Paths.get("target", "test-reports", "sql")
        Files.createDirectories(outDir)
        val outFile = outDir.resolve("sql-report-visual.html")
        Files.writeString(outFile, html)

        println("\n>>> SQL report written to: ${outFile.toAbsolutePath()}\n")
    }

    // -------------------------------------------------------------------------
    // Data generation
    // -------------------------------------------------------------------------

    private fun buildRealisticEntries(rng: Random): List<SqlStatisticsEntry> {
        val testCases = listOf(
            "transfer_asset_between_accounts",
            "create_account_and_mint_tokens",
            "query_account_balances_paginated",
            "batch_nft_metadata_update",
            "cross_chain_message_routing",
        )

        val entries = mutableListOf<SqlStatisticsEntry>()
        var wallClock = 1_700_000_000_000L

        for ((tcIndex, testCase) in testCases.withIndex()) {
            // 15–25 queries per test case
            val queryCount = 15 + rng.nextInt(11)
            repeat(queryCount) { i ->
                val isSystem = rng.nextFloat() < 0.30f
                val durationMs = pickDuration(rng, isSystem)
                val error = if (!isSystem && rng.nextFloat() < 0.05f) {
                    Exception(pickError(rng))
                } else null

                entries += SqlStatisticsEntry(
                    event = SqlExecutionEvent(
                        sql = if (isSystem) pickSystemSql(rng) else pickUserSql(rng, tcIndex, i),
                        parameters = pickParams(rng, isSystem),
                        startTimeMs = wallClock,
                        durationMs = durationMs,
                        rowCount = if (error != null) null else pickRowCount(rng, isSystem),
                        isSystem = isSystem,
                        error = error,
                    ),
                    testCaseName = testCase,
                )
                wallClock += durationMs + rng.nextLong(5)
            }
            wallClock += 200L // gap between test cases
        }

        return entries
    }

    private fun pickDuration(rng: Random, isSystem: Boolean): Long {
        // Weighted towards fast queries, a handful of slow ones
        val roll = rng.nextFloat()
        return when {
            isSystem -> when {
                roll < 0.70f -> rng.nextLong(1, 20)
                roll < 0.95f -> rng.nextLong(20, 80)
                else -> rng.nextLong(80, 300)
            }
            else -> when {
                roll < 0.50f -> rng.nextLong(5, 50)
                roll < 0.80f -> rng.nextLong(50, 200)
                roll < 0.95f -> rng.nextLong(200, 800)
                else -> rng.nextLong(800, 2500)
            }
        }
    }

    private fun pickRowCount(rng: Random, isSystem: Boolean): Int? {
        if (rng.nextFloat() < 0.15f) return null
        return if (isSystem) rng.nextInt(1, 5) else rng.nextInt(0, 500)
    }

    private fun pickError(rng: Random): String = listOf(
        "duplicate key value violates unique constraint \"account_pkey\"",
        "foreign key constraint \"tx_account_id_fkey\" violated",
        "deadlock detected — transaction aborted",
        "column \"asset_id\" of relation \"balance\" does not exist",
        "value too long for type character varying(64)",
    ).random(rng)

    private fun pickParams(rng: Random, isSystem: Boolean): List<Any?> {
        if (isSystem) return emptyList()
        val count = rng.nextInt(0, 5)
        return List(count) {
            when (rng.nextInt(4)) {
                0 -> "0x" + List(32) { "0123456789abcdef"[rng.nextInt(16)] }.joinToString("")
                1 -> rng.nextLong(1, 9_999_999)
                2 -> null
                else -> listOf("pending", "confirmed", "failed", "active").random(rng)
            }
        }
    }

    private fun pickUserSql(rng: Random, tcIndex: Int, queryIndex: Int): String {
        val sqls = listOf(
            """SELECT a.id, a.name, b.amount, b.asset_id
FROM account a
JOIN balance b ON b.account_id = a.id
WHERE a.id = $1
  AND b.asset_id = $2""",
            """INSERT INTO transaction (id, account_id, asset_id, amount, status, created_at)
VALUES ($1, $2, $3, $4, 'pending', now())
RETURNING id""",
            """UPDATE balance
SET amount = amount - $1
WHERE account_id = $2
  AND asset_id = $3
  AND amount >= $1""",
            """SELECT t.id, t.amount, t.status, a.name AS recipient
FROM transaction t
JOIN account a ON a.id = t.to_account_id
WHERE t.from_account_id = $1
  AND t.created_at > $2
ORDER BY t.created_at DESC
LIMIT 50""",
            """DELETE FROM pending_message
WHERE id = $1
  AND processed = true""",
            """SELECT m.id, m.payload, m.source_chain, m.target_chain, m.created_at
FROM cross_chain_message m
WHERE m.target_chain = $1
  AND m.status = 'queued'
ORDER BY m.priority DESC, m.created_at ASC
LIMIT $2""",
            """UPDATE nft_metadata
SET attributes = $1::jsonb,
    updated_at  = now()
WHERE token_id = $2
  AND collection_id = $3""",
            """SELECT COUNT(*) AS total, SUM(amount) AS volume
FROM transaction
WHERE asset_id = $1
  AND created_at BETWEEN $2 AND $3
  AND status = 'confirmed'""",
        )
        return sqls[(tcIndex * 3 + queryIndex) % sqls.size]
    }

    private fun pickSystemSql(rng: Random): String = listOf(
        "SELECT nextval('block_iid_seq')",
        "INSERT INTO block (iid, block_rid, block_height, timestamp) VALUES ($1, $2, $3, $4)",
        "SELECT block_height FROM block ORDER BY block_height DESC LIMIT 1",
        "UPDATE block SET finalized = true WHERE iid = $1",
        "SELECT configuration FROM blockchain_config WHERE chain_iid = $1 ORDER BY height DESC LIMIT 1",
        "INSERT INTO transaction_info (tx_rid, block_iid, tx_number) VALUES ($1, $2, $3)",
        "SELECT tx_rid FROM transaction_info WHERE block_iid = $1 ORDER BY tx_number",
        "DELETE FROM reverted_transaction WHERE block_iid < $1",
    ).random(rng)
}
