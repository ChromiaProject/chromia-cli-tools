package com.chromia.build.tools.lib

import net.postchain.rell.api.base.RellCliEnv
import java.util.concurrent.ConcurrentHashMap

interface LibraryInstallProgress {
    val errors: ConcurrentHashMap<String, String>
    fun onStart(libraryId: String)
    fun onProgress(libraryId: String, current: Long, total: Long, message: String)
    fun onSuccess(libraryId: String)
    fun onError(libraryId: String, errMessage: String)
    fun onPostInstall(libraryId: String, libraryVersion: String)
    val hasError: Boolean
        get() = errors.isNotEmpty()
}

// TODO: Refactor to make cleaner. Unlike animated progress bars, this output updates in place
//  we don't want to spam the CLI with multiple lines, (only print the relevant statements)
class CliLibraryInstallProgress(
    private val env: RellCliEnv,
    override val errors: ConcurrentHashMap<String, String> = ConcurrentHashMap()
) : LibraryInstallProgress {

    override fun onStart(libraryId: String) {}

    override fun onProgress(libraryId: String, current: Long, total: Long, message: String) {
        if (message.contains("reinstalling", ignoreCase = true)) {
            env.print(message)
        }
    }

    override fun onSuccess(libraryId: String) {
        env.print("[$libraryId] installed successfully.")
    }

    override fun onError(libraryId: String, errMessage: String) {
        errors.putIfAbsent(libraryId, errMessage)
        env.error("- Failed to install library $libraryId: $errMessage")
    }

    override fun onPostInstall(libraryId: String, libraryVersion: String) {}
}

