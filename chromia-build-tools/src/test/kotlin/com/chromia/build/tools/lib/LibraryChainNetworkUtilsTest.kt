package com.chromia.build.tools.lib

import com.chromia.build.tools.config.ChromiaPredefinedNetworks
import com.chromia.build.tools.config.DirectoryChainNetwork
import com.chromia.build.tools.lib.LibraryChainNetworkUtils.createLibraryChainClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.ChromiaPostchainClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests for lib-chain client creation.
 *
 * [createLibraryChainClient] has three modes:
 * 1. No registry (default) - uses hardcoded mainnet lib-chain RID via [ChromiaPredefinedNetworks]
 * 2. Predefined network registry - requires explicit RID, delegates to [ChromiaPredefinedNetworks]
 * 3. Custom URL registry - requires explicit RID, creates a direct [PostchainClientImpl]
 */
internal class LibraryChainNetworkUtilsTest {

    private val mainnetLibChainRID =
        BlockchainRid.buildFromHex("C9051571CD822507DDD1F3B43F2DC066B54CC5A25ECD758A1B5A42913483CF20")
    private val customBrid =
        BlockchainRid.buildFromHex("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB")

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // --- predefinedLibChainRids ---

    @Test
    fun `mainnet lib-chain RID is the well-known hardcoded value`() {
        assertEquals(mainnetLibChainRID, LibraryChainNetworkUtils.predefinedLibChainRids["mainnet"])
    }

    // --- createLibraryChainClient: null registry (mainnet default) ---

    @Test
    fun `null registry uses hardcoded mainnet lib-chain RID`() {
        val mockClient = mockk<ChromiaPostchainClient>()
        val mockNetwork = mockk<DirectoryChainNetwork>()
        mockkObject(ChromiaPredefinedNetworks)
        every { ChromiaPredefinedNetworks.connect("mainnet") } returns mockNetwork
        every { mockNetwork.getChainClient(mainnetLibChainRID) } returns mockClient

        assertEquals(mockClient, createLibraryChainClient())
    }

    @Test
    fun `null registry throws when network cannot be resolved`() {
        mockkObject(ChromiaPredefinedNetworks)
        every { ChromiaPredefinedNetworks.connect("mainnet") } throws IllegalStateException("Node URL is not defined for network mainnet")

        assertThrows<IllegalStateException> { createLibraryChainClient() }
    }

    // --- createLibraryChainClient: predefined network registry ---

    @Test
    fun `predefined network registry with explicit brid delegates to ChromiaPredefinedNetworks`() {
        val mockClient = mockk<ChromiaPostchainClient>()
        val mockNetwork = mockk<DirectoryChainNetwork>()
        mockkObject(ChromiaPredefinedNetworks)
        every { ChromiaPredefinedNetworks.isPredefined("testnet") } returns true
        every { ChromiaPredefinedNetworks.connect("testnet") } returns mockNetwork
        every { mockNetwork.getChainClient(customBrid) } returns mockClient

        assertEquals(mockClient, createLibraryChainClient(registry = "testnet", brid = customBrid))
    }

    @Test
    fun `predefined network registry without brid throws`() {
        mockkObject(ChromiaPredefinedNetworks)
        every { ChromiaPredefinedNetworks.isPredefined("testnet") } returns true

        assertThrows<IllegalArgumentException> {
            createLibraryChainClient(registry = "testnet", brid = null)
        }
    }

    @Test
    fun `predefined network registry throws when network cannot be resolved`() {
        mockkObject(ChromiaPredefinedNetworks)
        every { ChromiaPredefinedNetworks.isPredefined("testnet") } returns true
        every { ChromiaPredefinedNetworks.connect("testnet") } throws IllegalStateException("Node URL is not defined for network testnet")

        assertThrows<IllegalStateException> {
            createLibraryChainClient(registry = "testnet", brid = customBrid)
        }
    }

    // --- createLibraryChainClient: custom URL registry ---

    @Test
    fun `custom URL registry with brid creates a direct PostchainClientImpl`() {
        mockkObject(ChromiaPredefinedNetworks)
        every { ChromiaPredefinedNetworks.isPredefined("https://my-node.example.com") } returns false

        assertInstanceOf(
            PostchainClientImpl::class.java,
            createLibraryChainClient(registry = "https://my-node.example.com", brid = customBrid)
        )
    }

    @Test
    fun `custom URL registry without brid throws`() {
        mockkObject(ChromiaPredefinedNetworks)
        every { ChromiaPredefinedNetworks.isPredefined("https://my-node.example.com") } returns false

        assertThrows<IllegalArgumentException> {
            createLibraryChainClient(registry = "https://my-node.example.com", brid = null)
        }
    }
}
