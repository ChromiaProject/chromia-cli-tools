package com.chromia.build.tools.lib

import com.chromia.build.tools.lib.installers.ChromiaLibChainInstaller
import com.chromia.build.tools.lib.installers.GitLibInstaller
import com.chromia.build.tools.util.isChromiaLib
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.RellLibraryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import net.postchain.rell.api.base.RellCliEnv
import java.nio.file.Path

class LibraryInstaller(
        private val repositoryCloner: RepositoryCloner,
        private val env: RellCliEnv,
        private val model: ChromiaModel,
        private val forceInstall: Boolean,
        libraryProgress: LibraryInstallProgress?,
        private val isExplicitInstall: Boolean = false
) {
    private val progress: LibraryInstallProgress = libraryProgress ?: CliLibraryInstallProgress(env)
    private val libRoot: Path = model.compile.source.resolve("lib")
    private val tmpLibRoot: Path = model.compile.target.resolve(".tmp/lib")
    private val libraryVerifier = LibraryVerifyer(env, libRoot, progress)

    fun installLibs(libs: Map<String, RellLibraryModel>) = runBlocking {
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

        if (progress.hasError) {
            val size = progress.errors.size
            throw LibraryInstallException("Failed to install $size ${if (size == 1) "library" else "libraries"}")
        }
    }

    private suspend fun installLibraryWithProgress(
            libraryId: String,
            libModel: RellLibraryModel
    ) = runCatching {
        progress.onStart(libraryId)

        val installer = if (libModel.isChromiaLib) {
            ChromiaLibChainInstaller(progress)
        } else {
            GitLibInstaller(repositoryCloner, tmpLibRoot, libraryVerifier, progress)
        }
        installer.install(libraryId, libModel, libRoot, forceInstall)
    }.fold(
            onSuccess = {
                progress.onSuccess(libraryId)
                if (isExplicitInstall && libModel.version != null) {
                    progress.onPostInstall(libraryId, libModel.version)
                }
            },
            onFailure = { e ->
                val errorMessage = e.message ?: "Unknown error"
                progress.onError(libraryId, errorMessage)
            }
    )
}
