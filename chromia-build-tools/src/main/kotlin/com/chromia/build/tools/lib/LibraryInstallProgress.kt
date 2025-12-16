package com.chromia.build.tools.lib

interface LibraryInstallProgress {
    fun onStart(libraryId: String)
    fun onProgress(libraryId: String, current: Long, total: Long, message: String)
    fun onSuccess(libraryId: String)
    fun onError(libraryId: String)
    fun onPostInstall(libraryId: String, libraryVersion: String)
    fun onSummary(errors: Map<String, String>)
}

