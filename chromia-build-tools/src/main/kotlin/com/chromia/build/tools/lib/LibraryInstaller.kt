package com.chromia.build.tools.lib

import com.chromia.build.tools.util.safeDelete
import com.chromia.cli.model.RellLibraryModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        sourceDir: Path,
        private val tempDir: Path,
        private val forceInstall: Boolean
) {

    private val libRoot: Path = sourceDir.resolve("lib")
    private val tmpLibRoot: Path = tempDir.resolve(".tmp/lib")
    private val libraryVerifyer = LibraryVerifyer(env, libRoot)
    private val chromiaLibInstaller = ChromiaLibInstaller()

    fun installLibs(libs: Map<String, RellLibraryModel>) = runBlocking {
        coroutineScope {
            libs.forEach { (id, libModel) ->
                launch { libModel.install(id) }
            }
        }
    }

    private fun RellLibraryModel.install(id: String) =
        version?.let {
            chromiaLibInstaller.installChromiaLibrary(id, this, libRoot, forceInstall)
        } ?: installGitLibrary(id, this)

    private fun installGitLibrary(name: String, model: RellLibraryModel) {
        cleanupTempDir()
        val installDir = libRoot.resolve(name)
        if (installDir.exists() && installDir.isNotEmptyDir()) {
            if (libraryVerifyer.verifyLib(name, model, true)) return
            env.print("Library $name not up to date, reinstalling")
            installDir.safeDelete()
        }
        cloneRepository(name, model, installDir)
        if (!libraryVerifyer.verifyLib(name, model)) {
            installDir.toFile().deleteRecursively()
            throw LibraryInstallException("Failed to install lib $name")
        }
    }

    private fun cloneRepository(name: String, model: RellLibraryModel, installDir: Path) {
        model.registry ?: throw LibraryInstallException("Registry not set for library $name")
        val tmpInstallDir = tmpLibRoot.resolve(name)
        try {
            repositoryCloner.clone(model.registry, tmpInstallDir, model.tagOrBranch)

            val sourcePath = tmpInstallDir.resolve(model.path)
            validateLibPath(sourcePath, model, name)
            copyRellFilesInFolder(sourcePath, installDir)
        } finally {
            cleanupTempDir()
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

    private fun cleanupTempDir() {
        tempDir.resolve(".tmp").let {
            if (it.exists()) it.toFile().deleteRecursively()
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
