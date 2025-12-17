package com.chromia.build.tools.lib

import com.chromia.build.tools.util.isChromiaLib
import com.chromia.build.tools.util.safeDelete
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.RellLibraryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import net.postchain.rell.api.base.RellCliEnv

class LibraryInstaller(
    private val repositoryCloner: RepositoryCloner,
    private val env: RellCliEnv,
    private val model: ChromiaModel,
    private val forceInstall: Boolean,
    private val progress: LibraryInstallProgress? = CliLibraryInstallProgress(env),
    private val isExplicitInstall: Boolean = false
) {

    private val libRoot: Path = model.compile.source.resolve("lib")
    private val tmpLibRoot: Path = model.compile.target.resolve(".tmp/lib")
    private val libraryVerifier = LibraryVerifyer(env, libRoot, progress)
    private val chromiaLibInstaller = ChromiaLibInstaller()

    fun installLibs(libs: Map<String, RellLibraryModel>)  = runBlocking {
        if (libs.isEmpty()) return@runBlocking
        installLibrariesAsync(libs)
    }

    private suspend fun installLibrariesAsync(chromiaLibs: Map<String, RellLibraryModel>) = supervisorScope {
        val jobs = chromiaLibs.map { (id, libModel) ->
            launch(Dispatchers.IO) {
                installLibraryWithProgress(id, libModel)
            }
        }
        jobs.joinAll()

        if (progress?.hasError == true) {
            progress.onSummary()
            val size = progress.errors.size
            throw LibraryInstallException("Failed to install $size ${if (size == 1) "library" else "libraries"}")
        }
    }

    private suspend fun installLibraryWithProgress(
        libraryId: String,
        libModel: RellLibraryModel
    ) = runCatching {
        progress?.onStart(libraryId)
        if (libModel.isChromiaLib) {
            chromiaLibInstaller.installChromiaLibrary(
                libraryId = libraryId,
                libModel = libModel,
                libRoot = libRoot,
                forceInstall = forceInstall,
                progress
            )
        } else {
            installGitLibrary(libraryId, libModel)
        }
    }.fold(
        onSuccess = {
            progress?.onSuccess(libraryId)
            if (isExplicitInstall && libModel.version != null) {
                progress?.onPostInstall(libraryId, libModel.version)
            }
        },
        onFailure = { e ->
            val errorMessage = e.message ?: "Unknown error"
            progress?.onError(libraryId, errorMessage)
        }
    )

    private fun installGitLibrary(name: String, model: RellLibraryModel) {
        val installDir = libRoot.resolve(name)
        if (installDir.exists() && installDir.isNotEmptyDir()) {
            if (libraryVerifier.verifyLib(name, model)) return
            progress?.onProgress(name, 5, 100, "Library $name not up to date, reinstalling")
            installDir.safeDelete()
        }
        cloneRepository(name, model, installDir)
        progress?.onProgress(name, 10, 100, "Verifying installation")
        if (!libraryVerifier.verifyLib(name, model)) {
            installDir.toFile().deleteRecursively()
            throw LibraryInstallException("Failed to install lib $name")
        }
    }

    private fun cloneRepository(name: String, model: RellLibraryModel, installDir: Path) {
        model.registry ?: throw LibraryInstallException("Registry not set for library $name")
        val tmpInstallDir = tmpLibRoot.resolve(name)
        try {
            progress?.onProgress(name, 20, 100, "Cloning repository")
            repositoryCloner.clone(model.registry, tmpInstallDir, model.tagOrBranch)
            val sourcePath = tmpInstallDir.resolve(model.path)
            progress?.onProgress(name, 40, 100, "Validating library path")
            validateLibPath(sourcePath, model, name)
            progress?.onProgress(name, 60, 100, "Copying files to lib directory")
            copyRellFilesInFolder(sourcePath, installDir)
        } finally {
            progress?.onProgress(name, 80, 100, "Cleaning up temporary files")
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
