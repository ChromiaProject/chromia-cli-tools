package com.chromia.build.tools.lib.installers

import com.chromia.build.tools.lib.DirectoryHashCalculator
import com.chromia.build.tools.lib.LibraryChainNetworkUtils.createLibraryChainClient
import com.chromia.build.tools.lib.LibraryInstallProgress
import com.chromia.build.tools.util.safeDelete
import com.chromia.cli.model.RellLibraryModel
import com.chromia.library.chain.versioning.TypesSLibraryVersionFilesInBytes
import com.chromia.library.chain.versioning.external.getLibrary
import com.chromia.library.chain.versioning.external.getLibraryRid
import com.chromia.library.chain.versioning.external.getLibraryVersionFilesInBytes
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.coroutineScope
import net.postchain.client.core.PostchainClient
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.div
import kotlin.io.path.writeBytes

class ChromiaLibChainInstaller(private val progress: LibraryInstallProgress) : LibrarySourceInstaller {

    @OptIn(ExperimentalPathApi::class)
    override suspend fun install(
            libraryId: String,
            libModel: RellLibraryModel,
            libRoot: Path,
            forceInstall: Boolean,
    ) {
        progress.onProgress(libraryId, 5, 100, "Connecting to library chain")
        val client = createLibraryChainClient(libModel.registry, libModel.brid)
        val version = requireNotNull(libModel.version) { "version is required for library $libraryId" }

        progress.onProgress(libraryId, 10, 100, "Fetching library metadata")
        val name = requireNotNull(client.getLibrary(libraryId)?.displayName) {
            "Library '$libraryId' not found."
        }
        val expectedRid = requireNotNull(client.getLibraryRid(libraryId, version)) {
            "Version '$version' doesn't exist for library '$libraryId'"
        }

        val tempLibraryDir = createTempDirectory(name)
        val targetDir = libRoot / name

        try {
            progress.onProgress(libraryId, 30, 100, "Downloading library files")
            val allFiles = fetchLibraryFiles(client, libraryId, version) { filesCount ->
                // FIXME: need to update library-chain Rell code, so that we can have
                //  metadata of files count beforehand to report real stats to the user
                val message = "Downloaded $filesCount ${if (filesCount == 1L) "file" else "files"}"
                progress.onProgress(libraryId, filesCount, filesCount + 1, message)
            }

            progress.onProgress(libraryId, 60, 100, "Processing downloaded files")
            val installableFiles = allFiles
                    .flatMap { it.files.entries }
                    .filter { (filePath, _) -> shouldInstallFile(filePath, libModel) }

            installableFiles.forEach { (filePath, content) ->
                val tempPath = tempLibraryDir / filePath
                installFile(tempPath, content.data)
            }

            progress.onProgress(libraryId, 85, 100, "Verifying installation")
            val calculatedRid = calculateRid(tempLibraryDir)

            if (calculatedRid.contentEquals(expectedRid) || forceInstall) {
                progress.onProgress(libraryId, 90, 100, "Finalizing installation")
                targetDir.safeDelete()
                targetDir.parent.createDirectories()
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
            version: String,
            onFilesFetched: ((filesCount: Long) -> Unit)? = null
    ) = coroutineScope {
        val allFiles = mutableListOf<TypesSLibraryVersionFilesInBytes>()
        var offset = 0L

        while (true) {
            val batch = getLibraryFilesBatch(client, libraryId, offset, version)

            if (batch.files.isEmpty()) {
                break
            }

            allFiles += batch
            onFilesFetched?.invoke(allFiles.sumOf { it.files.size.toLong() })

            offset = batch.nextOffset ?: break
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
    ) = client.getLibraryVersionFilesInBytes(libraryId, version, LIBRARY_PAGE_SIZE, offset)

    fun calculateRid(libDir: Path): ByteArray {
        val calculator = DirectoryHashCalculator(libDir)
        val rid = calculator.compute(libDir, DirectoryHashCalculator.RidStrategy.LIST)
        return rid.data
    }

    companion object {
        private const val LIBRARY_PAGE_SIZE = 20L
    }
}
