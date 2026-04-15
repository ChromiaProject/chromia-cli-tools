package com.chromia.api.impl

import com.chromia.build.tools.lib.LibraryInstallProgress
import com.chromia.build.tools.lib.LibraryInstaller
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.model.ChromiaModel
import net.postchain.client.config.PostchainClientConfig
import net.postchain.rell.api.base.RellCliEnv
import org.apache.commons.configuration2.MapConfiguration

fun install(
        cliEnv: RellCliEnv,
        repositoryCloner: RepositoryCloner,
        model: ChromiaModel,
        forceInstall: Boolean,
        libraryProgress: LibraryInstallProgress?,
        isExplicitInstall: Boolean,
        postchainClientConfig: Map<String, String> = emptyMap()
) {
    LibraryInstaller(
            repositoryCloner,
            cliEnv,
            model,
            forceInstall,
            libraryProgress,
            isExplicitInstall,
            postchainClientConfig.takeIf { it.isNotEmpty() }?.let {
                PostchainClientConfig.fromConfiguration(MapConfiguration(it))
            }
    ).installLibs(model.libs)
}
