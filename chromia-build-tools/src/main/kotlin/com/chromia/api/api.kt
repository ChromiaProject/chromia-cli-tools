package com.chromia.api

import com.chromia.api.impl.compileGtv
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.build.tools.lib.LibraryInstallProgress
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.DeploymentModel
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.rell.api.base.RellCliEnv

object ChromiaCompileApi {

    /**
     * Builds blockchain configurations
     */
    fun build(cliEnv: RellCliEnv, model: ChromiaModel, verifyLibraries: Boolean = true)
            : List<BlockchainConfiguration> = compileGtv(cliEnv, model, verifyLibraries)

    /**
     * Verifies rell source code and computes the RID
     */
    @ExperimentalApi
    fun verify(cliEnv: RellCliEnv, model: ChromiaModel)
            : Boolean = com.chromia.api.impl.verify(cliEnv, model)
}

object ChromiaLibrariesApi {
    /**
     * Installs libraries to src/lib folder.
     */
    @ExperimentalApi("May want to remove RepositoryCloner parameter from this api")
    fun install(
        cliEnv: RellCliEnv,
        model: ChromiaModel,
        repositoryCloner: RepositoryCloner,
        forceInstall: Boolean = false,
        progress: LibraryInstallProgress? = null,
        isExplicitInstall: Boolean = false,
        postchainClientConfig: Map<String, String> = emptyMap()
    ) = com.chromia.api.impl.install(cliEnv, repositoryCloner, model, forceInstall, progress, isExplicitInstall, postchainClientConfig)
}

object ChromiaDeploymentApi {
    fun create(printer: (isError: Boolean, message: String) -> Unit, model: DeploymentModel, chromiaConfig: ChromiaClientConfig, configurations: List<BlockchainConfiguration>, compressConfigirations: Boolean): List<BlockchainDeploymentResult> {
            return com.chromia.api.impl.createNew(printer, model, chromiaConfig, configurations, compressConfigirations, PostchainClientProviderImpl())
    }

    fun update(printer: (isError: Boolean, message: String) -> Unit, model: DeploymentModel, chromiaConfig: ChromiaClientConfig, configurations: List<BlockchainConfiguration>, compressConfigurations: Boolean, height: Long? = null): List<BlockchainDeploymentResult> {
        return com.chromia.api.impl.updateExisting(printer, model, chromiaConfig, configurations, height, compressConfigurations, PostchainClientProviderImpl())
    }

    fun action(model: DeploymentModel, chromiaConfig: ChromiaClientConfig, action: BlockchainAction, reason: String): Pair<Boolean, String?> {
        return com.chromia.api.impl.action(model, chromiaConfig, action, reason)
    }
}
