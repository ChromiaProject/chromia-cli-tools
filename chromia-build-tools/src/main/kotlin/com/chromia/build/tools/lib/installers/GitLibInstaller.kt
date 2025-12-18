package com.chromia.build.tools.lib.installers
import com.chromia.build.tools.lib.LibraryInstallException
import com.chromia.build.tools.lib.LibraryInstallProgress
import com.chromia.build.tools.lib.LibraryVerifyer
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.build.tools.util.safeDelete
import com.chromia.cli.model.RellLibraryModel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

class GitLibInstaller(
        private val repositoryCloner: RepositoryCloner,
        private val tmpLibRoot: Path,
        private val libraryVerifier: LibraryVerifyer,
        private val progress: LibraryInstallProgress
) : LibrarySourceInstaller {
    override suspend fun install(
        libraryId: String,
        libModel: RellLibraryModel,
        libRoot: Path,
        forceInstall: Boolean,
    ) {
        val installDir = libRoot.resolve(libraryId)
        if (installDir.exists() && installDir.isNotEmptyDir()) {
            if (libraryVerifier.hasMatchingRid(libraryId, libModel)) return
            progress.onProgress(libraryId, 5, 100, "Library $libraryId not up to date, reinstalling")
            installDir.safeDelete()
        }
        cloneRepository(libraryId, libModel, installDir)
        progress.onProgress(libraryId, 10, 100, "Verifying installation")
        if (!libraryVerifier.verifyLib(libraryId, libModel)) {
            installDir.toFile().deleteRecursively()
            throw LibraryInstallException("Failed to install lib $libraryId")
        }
    }

    private fun cloneRepository(name: String, model: RellLibraryModel, installDir: Path) {
        model.registry ?: throw LibraryInstallException("Registry not set for library $name")
        val tmpInstallDir = tmpLibRoot.resolve(name)
        try {
            progress.onProgress(name, 20, 100, "Cloning repository")
            repositoryCloner.clone(model.registry, tmpInstallDir, model.tagOrBranch)
            val sourcePath = tmpInstallDir.resolve(model.path)
            progress.onProgress(name, 40, 100, "Validating library path")
            validateLibPath(sourcePath, model, name)
            progress.onProgress(name, 60, 100, "Copying files to lib directory")
            copyRellFilesInFolder(sourcePath, installDir)
        } finally {
            progress.onProgress(name, 80, 100, "Cleaning up temporary files")
            tmpInstallDir.safeDelete()
        }
    }

    private fun validateLibPath(sourcePath: Path, model: RellLibraryModel, name: String) {
        if (!sourcePath.exists()) {
            val pathNotFoundMsg = buildString {
                appendLine(
                        """
                    |Path '${model.path}' not found in repository '${model.registry}'.
                    |Library: $name
                    |-> Version/Branch: ${model.tagOrBranch ?: ""}
                    |-> Repository: ${model.registry}
                    |-> Requested Path: ${model.path}
                    """.trimMargin()
                )
                append("Please update the 'path' accordingly in chromia.yml 'libs->$name->path'")
            }
            throw LibraryInstallException(pathNotFoundMsg)
        }
    }

    private fun copyRellFilesInFolder(src: Path, dest: Path) {
        if (!dest.exists()) dest.toFile().mkdirs() else dest.toFile().deleteRecursively()
        Files.walk(src).use { stream ->
            stream.filter { it.isDirectory() || it.extension == "rell" }.forEach {
                Files.copy(it, dest.resolve(src.relativize(it)), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun Path.isNotEmptyDir(): Boolean = isValidDirectory && hasEntries

    private val Path.isValidDirectory: Boolean
        get() = Files.exists(this) && Files.isDirectory(this)

    private val Path.hasEntries: Boolean
        get() = Files.list(this).use { it.findAny().isPresent }

}
