package com.chromia.build.tools.lib

import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import net.postchain.crypto.sha256Digest
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculatorV2
import net.postchain.gtv.merkleHash
import net.postchain.rell.base.utils.RellGtxConfigConstants
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors.toList
import java.util.stream.Collectors.toMap
import java.util.stream.Stream
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.relativeTo

/**
 * Computes the hash of all rell files in a folder relative to it parent source folder
 */
class DirectoryHashCalculator(private val sourceDir: Path) {
    private val hashCalculator = GtvMerkleHashCalculatorV2(::sha256Digest)

    companion object {
        const val EOL_WINDOWS: String = "\r\n"
        const val EOL_UNIX: String = "\n"
    }

    /**
     * Computes the rid (hash) of the contents of a folder.
     **/
    fun compute(dir: Path, strategy: RidStrategy): WrappedByteArray {
        val filesStream = Files.walk(dir).filter { it.extension == "rell" }.map { it.relativeTo(sourceDir) to it.readText().replace(EOL_WINDOWS, EOL_UNIX) }
        return gtv(RellGtxConfigConstants.SOURCES_KEY to strategy.reduce(filesStream))
                .merkleHash(hashCalculator)
                .wrap()
    }

    fun interface RidStrategy {
        fun reduce(s: Stream<Pair<Path, String>>): Gtv

        companion object {
            /**
             * Given a files structure like `src/lib/foo/a.rell`, `src/lib/foo/b.rell`, it computes the hash of the files as an ordered list
             * `[<a.rell-content>, <b.rell-content>]`
             */
            val LIST = RidStrategy { s -> gtv(s.sorted(Comparator.comparing { it.first }).map { gtv(it.second) }.collect(toList())) }

            /**
             * Given a files structure like `src/lib/foo/module.rell`, it computes the hash of the files as a map
             * `lib/foo/module.rell -> <content>`
             */
            val MAP = RidStrategy { s -> gtv(s.collect(toMap({ it.first.pathString.replace("\\", "/") }, { gtv(it.second) }))) }
        }
    }
}
