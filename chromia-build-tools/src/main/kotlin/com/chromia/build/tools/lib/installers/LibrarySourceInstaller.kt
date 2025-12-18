package com.chromia.build.tools.lib.installers

import com.chromia.cli.model.RellLibraryModel
import java.nio.file.Path

interface LibrarySourceInstaller {
    suspend fun install(
            libraryId: String,
            libModel: RellLibraryModel,
            libRoot: Path,
            forceInstall: Boolean,
    )
}

