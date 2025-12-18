package com.chromia.build.tools.lib

import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.lib.DirectoryHashCalculator.RidStrategy
import com.chromia.build.tools.util.isChromiaLib
import com.chromia.cli.model.RellLibraryModel
import com.chromia.library.chain.versioning.external.getLibraryRid
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import java.nio.file.Path
import kotlin.io.path.notExists
import net.postchain.rell.api.base.RellCliEnv

class LibraryVerifyer(
    private val env: RellCliEnv,
    private val libRoot: Path,
    private val libraryProgress: LibraryInstallProgress = CliLibraryInstallProgress(env)
) {

    fun verifyLibs(libs: Map<String, RellLibraryModel>) {
        libs.forEach { (name, rellLibrary) ->
            val actualName = rellLibrary.getSimpleName(name)
            val libraryLocation = libRoot.resolve(actualName)
            if (libraryLocation.notExists()) throw ValidationException("Library $name is not installed, install before building")
            if (!verifyLib(name, rellLibrary)) throw ValidationException("Failed validation of library $name")
        }
    }

    fun verifyLib(name: String, model: RellLibraryModel): Boolean {
        when (val libraryRidResult = compareLibraryRid(name, model)) {
            is LibraryRidCompared.Insecure -> return true
            is LibraryRidCompared.Valid -> return true
            is LibraryRidCompared.Invalid -> {
                val message = """
                The rid for library $name does not match the configured value.
                Should be: ${libraryRidResult.expected}
                Was: ${libraryRidResult.actual}
                Do not blindly copy the calculated rid as the integrity of the library cannot be verified.
                """.trimIndent()
                libraryProgress.onError(libraryId = name, errMessage = message)
                return false
            }
        }
    }

    fun hasMatchingRid(name: String, model: RellLibraryModel): Boolean {
        return when (compareLibraryRid(name, model)) {
            is LibraryRidCompared.Insecure -> true
            is LibraryRidCompared.Valid -> true
            is LibraryRidCompared.Invalid -> false
        }
    }

    private fun compareLibraryRid(name: String, model: RellLibraryModel): LibraryRidCompared {
        if (model.insecure) return LibraryRidCompared.Insecure

        val (finalModel, finalName) = if (model.isChromiaLib) {
            val client = createLibraryChainClient(model.registry, model.brid)
            val expectedRid = model.rid
                    ?: client.getLibraryRid(name, model.version!!)?.wrap()
                    ?: error("Library '$name' with Version '${model.version}' doesn't exist")
            model.copy(rid = expectedRid) to name.substringAfterLast(".")
        } else {
            model to name
        }

        val libDir = libRoot.resolve(finalName)
        val hashCalculator = DirectoryHashCalculator(libRoot.parent)
        val libraryRid = hashCalculator.compute(libDir, RidStrategy.LIST)
        if (finalModel.rid == libraryRid) return LibraryRidCompared.Valid
        return LibraryRidCompared.Invalid(finalModel.rid!!, libraryRid)
    }

    private fun RellLibraryModel.getSimpleName(originalName: String): String =
        if (isChromiaLib) originalName.substringAfterLast(".") else originalName
}

sealed class LibraryRidCompared {
    data object Insecure : LibraryRidCompared()
    data object Valid : LibraryRidCompared()
    data class Invalid(val expected: WrappedByteArray, val actual: WrappedByteArray) : LibraryRidCompared()
}
