package com.chromia.build.tools.lib

import org.eclipse.jgit.api.CloneCommand
import java.io.File
import java.nio.file.Path
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.internal.transport.sshd.agent.connector.Factory
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import org.eclipse.jgit.util.FS

class GitRepositoryCloner(val sshDir: File? = null, val quiet: Boolean = false) : RepositoryCloner {

    override fun clone(registry: String, target: Path, tagOrBranch: String?) {
        try {
            val sshdSessionFactory = createSshdSessionFactory()
            SshSessionFactory.setInstance(sshdSessionFactory)
            Git.cloneRepository()
                    .apply { if (tagOrBranch != null) setBranch(tagOrBranch) }
                    .setDepth(1)
                    .setCloneAllBranches(false) // Speed up when [tagOrBranch] is not set
                    .setDirectory(target.toFile())
                    .setURI(registry)
                    .setTimeout(60)
                    .apply { if (!quiet) setProgressMonitor(TextProgressMonitor()) }
                    .call()
                    .close()
        } catch (e: Exception) {
            val humanFriendlyError = when (e) {
                is InvalidRemoteException -> "Invalid repository URL '$registry'."
                is TransportException -> formatTransportExceptionMsg(e, registry, tagOrBranch)
                is GitAPIException -> "Git operation failed: ${e.message}"
                else -> e.message ?: ""
            }
            target.toFile().deleteRecursively()
            throw LibraryInstallException(humanFriendlyError)
        }
    }

    private fun createSshdSessionFactory() = SshdSessionFactoryBuilder()
            .setConnectorFactory(Factory())
            .apply {
                if (sshDir != null) {
                    setSshDirectory(sshDir)
                } else {
                    setSshDirectory(File(FS.DETECTED.userHome(), ".ssh"))
                }
            }
            .setHomeDirectory(FS.DETECTED.userHome())
            .build(null)

    private fun formatTransportExceptionMsg(e: TransportException, url: String, tagOrBranch: String?): String {
        val branchNotFoundRegex = Regex("Remote branch '.*' not found")

        return e.message?.let {
            when {
                it.contains("not authorized") -> "Authentication failed for $url"
                it.contains("timeout") -> "Connection timed out while accessing $url"
                branchNotFoundRegex.containsMatchIn(it) -> "Remote repository '$url' doesn't have branch '$tagOrBranch'"
                else -> e.message
            }
        } ?: ""
    }
}
