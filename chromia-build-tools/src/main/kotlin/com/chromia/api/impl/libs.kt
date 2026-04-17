package com.chromia.api.impl

import com.chromia.build.tools.lib.LibraryInstallProgress
import com.chromia.build.tools.lib.LibraryInstaller
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.model.ChromiaModel
import net.postchain.client.config.PostchainClientConfig
import net.postchain.rell.api.base.RellCliEnv

fun install(
        cliEnv: RellCliEnv,
        repositoryCloner: RepositoryCloner,
        model: ChromiaModel,
        forceInstall: Boolean,
        libraryProgress: LibraryInstallProgress?,
        isExplicitInstall: Boolean,
        postchainClientConfig: PostchainClientConfig? = null,
        postchainClientConfigOverrides: Map<String, String> = emptyMap()
) {
    LibraryInstaller(
            repositoryCloner,
            cliEnv,
            model,
            forceInstall,
            libraryProgress,
            isExplicitInstall,
            postchainClientConfig,
            postchainClientConfigOverrides
    ).installLibs(model.libs)
}
