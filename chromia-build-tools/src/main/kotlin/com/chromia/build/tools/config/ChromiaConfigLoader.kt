package com.chromia.build.tools.config

import com.chromia.build.tools.keystore.ChromiaKeyStore
import net.postchain.common.PropertiesFileLoader
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import java.io.File

class ChromiaConfigLoader(private val logger: (String) -> Unit) {

    companion object {
        private const val DEFAULT_CONFIG_FILENAME = ".chromia/config"
        private const val DEFAULT_PMC_CONFIG_FILENAME = ".pmc/config"
        private const val DEFAULT_CHROMIA_MODEL_FILENAME = "chromia.yml"
        private const val DEFAULT_CONFIG_MODEL_FILENAME = "config.yml"
        private val chromiaHome
            get() = System.getenv("CHROMIA_HOME") ?: (System.getProperty("user.home") + "/.chromia")

        private val SENSITIVE_PROPERTY_FILE_KEYS = setOf("pubkey", "privkey")

        fun globalConfigurationFile() = File("$chromiaHome/config")
        fun localConfigurationFile() = File(DEFAULT_CONFIG_FILENAME)
    }

    /**
     * Loads configuration from system
     * @param file file containing additional overriding properties
     */
    fun loadClientConfigFile(file: File? = null): ChromiaClientConfig {
        val config = loadProperties(file)
        return ChromiaClientConfig.from(config)
    }

    /**
     * Loads properties from the system
     * @param file file containing additional overriding properties
     */
    fun loadProperties(file: File? = null): Configuration {
        val config = PropertiesConfiguration()
        config.setProperty("status.poll-interval", 2000)
        loadFromFileIfExists(globalConfigurationFile(), config)
        if (!localConfigurationFile().exists() && File(DEFAULT_PMC_CONFIG_FILENAME).exists()) {
            logger("Loading .pmc/config file. Rename to .chromia/config to silence this message")
            loadFromFileIfExists(File(DEFAULT_PMC_CONFIG_FILENAME), config) // Backwards compatibility
        }

        loadFromFileIfExists(localConfigurationFile(), config)

        val explicitConfig = PropertiesConfiguration().apply {
            loadFromFileIfExists(file, this)
        }
        val keysExplicitlyProvided = allSensitiveKeysExist(explicitConfig)
        config.copy(explicitConfig)

        if (config.containsKey("key.id") && !keysExplicitlyProvided) {
            ChromiaKeyStore(config.getString("key.id")).findKeyPair()?.let {
                config.setProperty("pubkey", it.pubKey.hex())
                config.setProperty("privkey", it.privKey.hex())
            }
        }
        return config
    }

    private fun loadFromFileIfExists(file: File?, config: Configuration) {
        if (file != null && file.exists()) {
            val c = PropertiesFileLoader.load(file.absolutePath)
            if (allSensitiveKeysExist(c)) {
                logger(
                    """
                    WARNING: The properties ${SENSITIVE_PROPERTY_FILE_KEYS.joinToString { "'$it'" }} are currently marked as deprecated.
                    We're standardizing our key management approach. This method of storing keys will be removed in future versions.
                    Please migrate to using a key ID or store this data in a secret file instead.
                    """.trimIndent()
                )
            }

            c.keys.forEach { key ->
                config.setProperty(key, c.getProperty(key))
            }
        }
    }

    private fun allSensitiveKeysExist(c: Configuration): Boolean {
        return SENSITIVE_PROPERTY_FILE_KEYS.all { c.containsKey(it) }
    }

    fun findModelFile(explicitFile: File?): File? {
        if (explicitFile != null) {
            require(explicitFile.isFile) { "File $explicitFile is not a regular file" }
            return explicitFile.absoluteFile
        }
        val chromiaModelFile = File(DEFAULT_CHROMIA_MODEL_FILENAME)
        if (chromiaModelFile.exists() && chromiaModelFile.isFile) {
            return chromiaModelFile.absoluteFile
        }

        val configModelFile = File(DEFAULT_CONFIG_MODEL_FILENAME)
        if (configModelFile.exists() && configModelFile.isFile) {
            logger("Found config.yml settings file. Rename to chromia.yml to silence this message")
            return configModelFile.absoluteFile
        }
        return null
    }
}
