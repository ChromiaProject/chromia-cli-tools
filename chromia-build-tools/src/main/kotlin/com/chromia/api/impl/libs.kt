package com.chromia.api.impl

import com.chromia.build.tools.lib.CliLibraryInstallProgress
import com.chromia.build.tools.lib.LibraryInstallProgress
import com.chromia.build.tools.lib.LibraryInstaller
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.model.ChromiaModel
import net.postchain.rell.api.base.RellCliEnv

fun install(
    cliEnv: RellCliEnv,
    repositoryCloner: RepositoryCloner,
    model: ChromiaModel,
    forceInstall: Boolean,
    libraryProgress: LibraryInstallProgress?,
    isExplicitInstall: Boolean
) {
    LibraryInstaller(
        repositoryCloner,
        cliEnv,
        model,
        forceInstall,
        libraryProgress,
        isExplicitInstall
    ).installLibs(model.libs)
}
