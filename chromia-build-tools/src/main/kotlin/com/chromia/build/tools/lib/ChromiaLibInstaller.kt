package com.chromia.build.tools.lib

import com.chromia.build.tools.util.safeDelete
import com.chromia.cli.model.RellLibraryModel
import com.chromia.library.chain.versioning.TypesSLibraryVersionFilesInBytes
import com.chromia.library.chain.versioning.external.getLibrary
import com.chromia.library.chain.versioning.external.getLibraryRid
import com.chromia.library.chain.versioning.external.getLibraryVersionFilesInBytes
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import net.postchain.client.core.PostchainClient
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.div
import kotlin.io.path.writeBytes

class ChromiaLibInstaller {

    @OptIn(ExperimentalPathApi::class)
    fun installChromiaLibrary(
        libraryId: String,
        libModel: RellLibraryModel,
        libRoot: Path,
        forceInstall: Boolean = false
    ) = runBlocking {
        val client = createLibraryChainClient(libModel.registry, libModel.brid)
        val version = libModel.version ?: error("version is required for library $libraryId")
        val name = client.getLibrary(libraryId)?.displayName
            ?: error("Library '$libraryId' not found.")

        val expectedRid = client.getLibraryRid(libraryId, version)
            ?: error("Version '$version' doesn't exist for library '$libraryId'")

        val tempLibraryDir = createTempDirectory(name)
        val targetDir = libRoot / name

        try {
            val installableFiles = fetchLibraryFiles(client, libraryId, version)
                .flatMap { it.files.entries }
                .filter { (filePath, _) -> shouldInstallFile(filePath, libModel) }

            installableFiles.forEach { (filePath, content) ->
                val tempPath = tempLibraryDir / filePath
                installFile(tempPath, content.data)
            }

            val calculatedRid = calculateRid(tempLibraryDir)

            if (calculatedRid.contentEquals(expectedRid) || forceInstall) {
                targetDir.safeDelete()
                targetDir.parent?.createDirectories()
                tempLibraryDir.copyToRecursively(targetDir, overwrite = true, followLinks = false)
            } else {
                error(
                    """
                        The hash of the library has changed.
                        This could indicate that files has been corrupted or tampered with.
                        Use --force to install anyway.
                    """.trimIndent()
                )
            }
        } finally {
            tempLibraryDir.safeDelete()
        }
    }

    private suspend fun fetchLibraryFiles(
        client: PostchainClient,
        libraryId: String,
        version: String
    ) = coroutineScope {
        val allFiles = mutableListOf<TypesSLibraryVersionFilesInBytes>()
        var offset = 0L
        var hasMoreFiles = true

        while (hasMoreFiles) {
            val filesAtOffset = async {
                // note: this function call needs an offset and number of files per page
                //  we need to track both until no files were returned
                getLibraryFilesBatch(client, libraryId, offset, version)
            }.await()

            if (filesAtOffset.files.isNotEmpty()) {
                allFiles += filesAtOffset
                offset++
            } else {
                hasMoreFiles = false
            }
        }
        allFiles.toImmutableList()
    }

    private fun shouldInstallFile(filePath: String, libModel: RellLibraryModel): Boolean =
        filePath.endsWith(".rell") &&
            (libModel.path?.let { filePath.startsWith(it) } ?: true)

    private fun installFile(targetPath: Path, content: ByteArray) {
        targetPath.parent?.let { Files.createDirectories(it) }
        targetPath.writeBytes(content)
    }

    private fun getLibraryFilesBatch(
        client: PostchainClient,
        libraryId: String,
        offset: Long,
        version: String
    ) = client.getLibraryVersionFilesInBytes(libraryId, version, 10L, offset)

    fun calculateRid(libDir: Path): ByteArray {
        val calculator = DirectoryHashCalculator(libDir)
        val rid = calculator.compute(libDir, DirectoryHashCalculator.RidStrategy.LIST)
        return rid.data
    }
}
