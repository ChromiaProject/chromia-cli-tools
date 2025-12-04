package com.chromia.build.tools.config

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div

val chromiaHome: Path
    get() = System.getenv("CHROMIA_HOME")?.let { Path(it) } ?: (Path(System.getProperty("user.home")) / ".chromia")
