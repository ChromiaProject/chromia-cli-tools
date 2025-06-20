package com.chromia.cli.model

import com.chromia.build.tools.model.ensureBrid
import com.chromia.build.tools.model.ensureType
import net.postchain.common.BlockchainRid
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap

data class RellLibraryModel(
        val registry: String? = null,
        val tagOrBranch: String? = null,
        val path: String? = null,
        val insecure: Boolean = false,
        val rid: WrappedByteArray? = null,
        val brid: BlockchainRid? = null,
        val version: String? = null,
) {

    fun format(name: String) = buildString {
        append("\n  $name:")
        registry?.let { append("\n    registry: $it") }
        tagOrBranch?.let { append("\n    tagOrBranch: $it") }
        version?.let { append("\n    version: $it") }
        path?.let { append("\n    path: $it") }
        if (insecure) append("\n    insecure: true")
        rid?.let { append("\n    rid: x\"$it\"") }
        brid?.let { append("\n    brid: x\"${it.toHex()}\"") }
    }

    companion object {
        fun load(data: Map<String, Any>, additionalProperty: String) = RellLibraryModel(
                registry = ensureType<String?>(data["registry"], "libs", additionalProperty, "registry"),
                tagOrBranch = ensureType<String?>(data["tagOrBranch"], "libs", additionalProperty, "tagOrBranch"),
                path = ensureType<String?>(data["path"], "libs", additionalProperty, "path"),
                insecure = ensureType<Boolean?>(data["insecure"], "libs", additionalProperty, "insecure")
                        ?: false,
                rid = ensureType<ByteArray?>(data["rid"], "libs", additionalProperty, "rid")?.wrap(),
                brid = data["brid"]?.let {
                    ensureBrid(it, "libs", additionalProperty, "brid")
                },
                version = ensureType<String?>(data["version"], "libs", additionalProperty, "version"),
        )
    }
}
