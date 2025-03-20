package com.chromia.build.tools.lib

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.lib.DirectoryHashCalculator.RidStrategy
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.wrap
import net.postchain.crypto.sha256Digest
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculatorV2
import net.postchain.gtv.merkleHash
import net.postchain.rell.base.utils.RellGtxConfigConstants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

class DirectoryHashCalculatorTest {

    @TempDir
    lateinit var sourceDir: Path

    val hashCalculator = GtvMerkleHashCalculatorV2(::sha256Digest)

    @Test
    fun `Given set of files, rid is computed as the hash of the map of files to their content`() {
        sourceDir.resolve("path/to/file/a.rell").createParentDirectories().writeText("content a")
        sourceDir.resolve("path/to/other/file/b.rell").createParentDirectories().writeText("content b")

        val expectedRid = gtv(RellGtxConfigConstants.SOURCES_KEY to gtv("path/to/file/a.rell" to gtv("content a"), "path/to/other/file/b.rell" to gtv("content b"))).merkleHash(hashCalculator)
        val computedRid = DirectoryHashCalculator(sourceDir).compute(sourceDir.resolve("path/to"), RidStrategy.MAP)
        assertThat(computedRid).isEqualTo(expectedRid.wrap())
        assertThat(computedRid).isEqualTo(("C7F369AFD42DF7E8F64F55F65946841B0D5339E989FAE8EDF8655C2665D5DB8D").hexStringToWrappedByteArray())
    }

    @Test
    fun `Given one file, rid is computed as the hash of the map of files to their content`() {
        sourceDir.resolve("path/to/file/a.rell").createParentDirectories().writeText("content a")

        val expectedRid = gtv(RellGtxConfigConstants.SOURCES_KEY to gtv("path/to/file/a.rell" to gtv("content a"))).merkleHash(hashCalculator)
        val computedRid = DirectoryHashCalculator(sourceDir).compute(sourceDir.resolve("path/to"), RidStrategy.MAP)
        assertThat(computedRid).isEqualTo(expectedRid.wrap())
        assertThat(computedRid).isEqualTo(("A8C568372587E5562C6DF5E3CA9C1E134A53781D20CF7378435C4A8DB76E8E70").hexStringToWrappedByteArray())
    }

    @Test
    fun `Given zero files, rid is computed as the hash of the map of files to their content`() {
        sourceDir.resolve("path/to/file").createParentDirectories()

        val expectedRid = gtv(RellGtxConfigConstants.SOURCES_KEY to gtv(mapOf())).merkleHash(hashCalculator)
        val computedRid = DirectoryHashCalculator(sourceDir).compute(sourceDir.resolve("path/to"), RidStrategy.MAP)
        assertThat(computedRid).isEqualTo(expectedRid.wrap())
        assertThat(computedRid).isEqualTo(("E0F9E7BAE3A4CAB77C404D843F307600E5C660E409BD6197E24C56111A03774F").hexStringToWrappedByteArray())
    }

    @Test
    fun `Given set of files, legacy rid is computed as the has of the ordered set of file contents`() {
        sourceDir.resolve("path/to/file/a.rell").createParentDirectories().writeText("content a")
        sourceDir.resolve("path/to/other/file/b.rell").createParentDirectories().writeText("content b")
        sourceDir.resolve("path/to/a/file/c.rell").createParentDirectories().writeText("content c")

        val expectedRid = gtv(RellGtxConfigConstants.SOURCES_KEY to gtv(gtv("content c"), gtv("content a"), gtv("content b"))).merkleHash(hashCalculator)
        val computedRid = DirectoryHashCalculator(sourceDir).compute(sourceDir.resolve("path/to"), RidStrategy.LIST)
        assertThat(computedRid).isEqualTo(expectedRid.wrap())
        assertThat(computedRid).isEqualTo("C1401B4ED25FC1C96A3FC8CD7957F445EA47E408019AC58E640EC56FB9615168".hexStringToWrappedByteArray())
    }

    @Test
    fun `Given one file, legacy rid is computed as the has of the ordered set of file contents`() {
        sourceDir.resolve("path/to/file/a.rell").createParentDirectories().writeText("content a")

        val expectedRid = gtv(RellGtxConfigConstants.SOURCES_KEY to gtv(gtv("content a"))).merkleHash(GtvMerkleHashCalculatorV2(::sha256Digest))
        val computedRid = DirectoryHashCalculator(sourceDir).compute(sourceDir.resolve("path/to"), RidStrategy.LIST)
        assertThat(computedRid).isEqualTo(expectedRid.wrap())
        assertThat(computedRid).isEqualTo("312A11E369D2526FC02C1CAFC828D8FB48ED1ECD15688D17CC3868F76DF1DC3A".hexStringToWrappedByteArray())
    }

    @Test
    fun `Given zero files, legacy rid is computed as the has of the ordered set of file contents`() {
        sourceDir.resolve("path/to/file").createParentDirectories()

        val expectedRid = gtv(RellGtxConfigConstants.SOURCES_KEY to gtv(listOf())).merkleHash(GtvMerkleHashCalculatorV2(::sha256Digest))
        val computedRid = DirectoryHashCalculator(sourceDir).compute(sourceDir.resolve("path/to"), RidStrategy.LIST)
        assertThat(computedRid).isEqualTo(expectedRid.wrap())
        assertThat(computedRid).isEqualTo("9A24CB2C26F8C5700C527698DBBF28737CE1A49169C08AA99C9CE2D2B6BBFF31".hexStringToWrappedByteArray())
    }
}
