package com.chromia.api.result

import com.chromia.build.tools.compile.BlockchainConfigurationWriter.storeConfig
import com.chromia.build.tools.compile.BlockchainConfigurationWriter.storeConfigAsBinaryGtv
import com.chromia.build.tools.compile.BlockchainConfigurationWriter.storeConfigLenient
import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.compile.withSigner
import net.postchain.base.configuration.BlockchainConfigurationData
import net.postchain.common.exception.UserMistake
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.mapper.toObject
import java.nio.file.Path

data class BlockchainConfiguration(override val name: String, val config: Gtv) : Named<Gtv>(name, config) {
    val configByteArray get() = GtvEncoder.encodeGtv(config)

    fun withSigners(vararg signer: ByteArray) = BlockchainConfiguration(name, withSigner(config, *signer))

    /**
     * Save configuration as GtvML (XML).
     *
     * @throws IllegalArgumentException if any string in [config] contains
     * [character not allowed in XML](https://www.w3.org/TR/xml/#NT-Char)
     */
    fun save(target: Path, fileName: String = name) = storeConfig(config, fileName, target)

    /**
     * Save configuration as GtvML (XML).
     *
     * Will output ill-formed XML if [config] contains [character not allowed in XML](https://www.w3.org/TR/xml/#NT-Char)
     */
    fun saveLenient(target: Path, fileName: String = name) = storeConfigLenient(config, fileName, target)

    /**
     * Save configuration as binary GTV.
     */
    fun saveAsBinaryGtv(target: Path, fileName: String = name) = storeConfigAsBinaryGtv(config, fileName, target)

    fun validate() {
        try {
            withSigner(config, "000000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray())
                    .toObject<BlockchainConfigurationData>()
        } catch (e: UserMistake) {
            throw ValidationException(e.message!!)
        }
    }
}

open class Named<T>(
        open val name: String,
        val value: T
)
//data class Named<T>(val name: String, val value: T)
