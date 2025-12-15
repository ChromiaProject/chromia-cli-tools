package com.chromia.build.tools.lib

interface LibraryInstallProgress {
    fun onStart(libraryId: String)
    fun onProgress(libraryId: String, current: Long, total: Long, message: String)
    fun onSuccess(libraryId: String)
    fun onError(libraryId: String, errorMessage: String?)
    fun onPostInstall(libraryId: String, libraryVersion: String)
}

