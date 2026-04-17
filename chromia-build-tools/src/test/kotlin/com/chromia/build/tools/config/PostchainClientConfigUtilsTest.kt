package com.chromia.build.tools.config

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.Duration

internal class PostchainClientConfigUtilsTest {

    private val brid = BlockchainRid.buildFromHex("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
    private val url = "https://node.example.com"

    private val base = PostchainClientConfig(
        blockchainRid = brid,
        endpointPool = EndpointPool.singleUrl(url),
        connectTimeout = Duration.ofMillis(5000),
        responseTimeout = Duration.ofMillis(10000)
    )

    @Test
    fun `empty overrides returns the same instance`() {
        assertSame(base, base.withOverrides(emptyMap()))
    }

    @Test
    fun `override takes precedence over base value`() {
        val result = base.withOverrides(mapOf("connect.timeout" to "1000"))
        assertEquals(Duration.ofMillis(1000), result.connectTimeout)
    }

    @Test
    fun `non-overridden fields are preserved from base`() {
        val result = base.withOverrides(mapOf("connect.timeout" to "1000"))
        assertEquals(Duration.ofMillis(10000), result.responseTimeout)
    }

    @Test
    fun `multiple overrides are all applied`() {
        val result = base.withOverrides(mapOf(
            "connect.timeout" to "2000",
            "failover.attempts" to "5"
        ))
        assertEquals(Duration.ofMillis(2000), result.connectTimeout)
        assertEquals(5, result.failOverConfig.attemptsPerEndpoint)
    }

    @Test
    fun `brid is preserved from base when not in overrides`() {
        val result = base.withOverrides(mapOf("connect.timeout" to "1000"))
        assertEquals(brid, result.blockchainRid)
    }

    @Test
    fun `api url is preserved from base when not in overrides`() {
        val result = base.withOverrides(mapOf("connect.timeout" to "1000"))
        assertEquals(url, result.endpointPool.first().url)
    }

    @Test
    fun `brid in overrides takes precedence over base`() {
        val newBrid = BlockchainRid.buildFromHex("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB")
        val result = base.withOverrides(mapOf("brid" to newBrid.toHex()))
        assertEquals(newBrid, result.blockchainRid)
    }

    @Test
    fun `api url in overrides takes precedence over base`() {
        val result = base.withOverrides(mapOf("api.url" to "https://other-node.example.com"))
        assertEquals("https://other-node.example.com", result.endpointPool.first().url)
    }
}
