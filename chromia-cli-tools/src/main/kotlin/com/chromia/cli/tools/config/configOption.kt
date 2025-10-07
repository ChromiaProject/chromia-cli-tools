package com.chromia.cli.tools.config

import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.build.tools.config.ChromiaConfigLoader
import com.chromia.cli.model.exceptionSuppressingParse
import com.chromia.cli.model.parseModel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.BlockchainRid
import java.io.File


// Optional Client Configuration. Reads from file system if not set
fun CliktCommand.chromiaConfigOption() = ChromiaConfigOption { msg -> echo(msg, err = true) }

// Chromia model must be found or explicitly set
fun CliktCommand.chromiaModelOption() = ChromiaModelOption { msg -> echo(msg, err = true) }

// Chromia model is optional. Will not throw if model file is not found
fun CliktCommand.optionalChromiaModelOption() = OptionalChromiaModelOption { msg -> echo(msg, err = true) }

// Chromia model is optional. Will not throw if model file is not found or has errors in it
fun CliktCommand.safeOptionalChromiaModelOption() = SafeOptionalChromiaModelOption { msg -> echo(msg, err = true) }


// Chromia model must be found or explicitly set. Client config is read from system if not set
fun CliktCommand.chromiaModelConfigOption() = ChromiaModelConfigOption { msg -> echo(msg, err = true) }

// Chromia model if optional. Client config is read from system if not set
fun CliktCommand.optionalChromiaModelConfigOption() = OptionalChromiaModelConfigOption { msg -> echo(msg, err = true) }

open class ChromiaConfigOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigFileOption()
    val config by lazy { ChromiaConfigLoader(logger).loadClientConfigFile(configFile) }
}

open class ChromiaModelOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    private val modelFile: File by requiredChromiaModelOption(logger)
    val model by lazy { parseModel(modelFile) }
    val projectFolder by lazy { modelFile.parentFile }
    val sourceDir get() = model.compile.source.toFile()
    val targetDir get() = model.compile.target.toFile()
}

open class OptionalChromiaModelOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    private val modelFile by chromiaModelFileOption()
    private val resolvedModelFile by lazy { ChromiaConfigLoader(logger).findModelFile(modelFile) }
    val projectFolder by lazy { resolvedModelFile?.parentFile }
    val model by lazy { resolvedModelFile?.let { parseModel(it) } }
    val sourceDir get() = model?.compile?.source?.toFile()
    val targetDir get() = model?.compile?.target?.toFile()
}

open class SafeOptionalChromiaModelOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    private val modelFile by chromiaModelFileOption()
    private val resolvedModelFile by lazy { ChromiaConfigLoader(logger).findModelFile(modelFile) }
    val projectFolder by lazy { resolvedModelFile?.parentFile }
    val model by lazy { resolvedModelFile?.let { exceptionSuppressingParse(it) } }
    val sourceDir get() = model?.compile?.source?.toFile()
    val targetDir get() = model?.compile?.target?.toFile()
}


open class ChromiaModelConfigOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigFileOption()
    val config by lazy { ChromiaConfigLoader(logger).loadClientConfigFile(configFile) }
    val modelFile by requiredChromiaModelOption(logger)
    val model by lazy { parseModel(modelFile) }
    val projectFolder by lazy { modelFile.parentFile }
    val sourceDir get() = model.compile.source.toFile()
    val targetDir get() = model.compile.target.toFile()
}

open class OptionalChromiaModelConfigOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigFileOption()
    val config by lazy { ChromiaConfigLoader(logger).loadClientConfigFile(configFile) }
    private val modelFile by chromiaModelFileOption()
    val model by lazy { ChromiaConfigLoader(logger).findModelFile(modelFile)?.let { parseModel(it) } }
    val projectFolder by lazy { modelFile?.parentFile }
}

open class BlockchainOptions(logger: (String) -> Unit) : OptionGroup() {
    private val clientConfigFile by chromiaConfigFileOption()
    val config by lazy { ChromiaConfigLoader(logger).loadClientConfigFile(clientConfigFile) }
    val url by targetUrlOption()
    val blockchainRid by blockchainRidOption("Target Blockchain RID").convert { BlockchainRid.buildFromHex(it) }.required()
}

internal fun ParameterHolder.requiredChromiaModelOption(logger: (String) -> Unit) = chromiaModelFileOption()
        .defaultLazy {
            ChromiaConfigLoader(logger).findModelFile(null)
                    ?: throw PrintMessage("Project settings file not found", statusCode = 1)
        }

fun ParameterHolder.chromiaModelFileOption() = option(
        "-s", "--settings",
        help = "Alternate path for project settings file",
        metavar = "SETTINGS",
        envvar = "CHROMIA_PROJECT_SETTINGS",
)
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { it.absoluteFile }

fun ParameterHolder.chromiaConfigFileOption() = option(
        "-cfg", "--config",
        help = "Alternate path for client configuration file",
        metavar = "CONFIG",
        envvar = "CHROMIA_CONFIG",
)
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { it.absoluteFile }

fun ParameterHolder.targetUrlOption() = option("--api-url", help = "Target url")

fun ParameterHolder.blockchainRidOption(help: String) = option("--blockchain-rid", "-brid", help = help)

enum class ConfigurationFormat {
    GTV, XML
}

fun ParameterHolder.configurationFormatOption() = option("-f", "--format", help = "Blockchain configuration format")
        .enum<ConfigurationFormat>().default(ConfigurationFormat.XML)

sealed class KeyPairSource {
    data class KeyId(val name: String) : KeyPairSource()
    data class SecretFile(val file: File) : KeyPairSource()
}

fun ParameterHolder.secretOption(hideHelpMessage: Boolean = false) =
        option("--secret", help = "Path to secret file (pubkey/privkey)", hidden = hideHelpMessage)
                .file(canBeDir = false, mustExist = true, mustBeReadable = true)

fun ParameterHolder.keyIdOption(hideHelpMessage: Boolean = false) =
        option("--key-id", help = "Key ID of the keypair to use", metavar = "KEY_ID", hidden = hideHelpMessage)

fun ParameterHolder.keyPairSourceOption(hideHelpMessage: Boolean = false) = mutuallyExclusiveOptions(
        name = if (hideHelpMessage) null else "Key pair source",
        option1 = secretOption(hideHelpMessage).convert {
            ChromiaConfigLoader.setSkipLoadingKeysById(true)
            KeyPairSource.SecretFile(it)
        },
        option2 = keyIdOption(hideHelpMessage).convert { KeyPairSource.KeyId(it) },
).single()

fun ChromiaClientConfig.configureSigners(keyPairSource: KeyPairSource?) {
    when (keyPairSource) {
        is KeyPairSource.SecretFile -> setSignerFromSecret(keyPairSource.file.toPath())
        is KeyPairSource.KeyId -> setSignerUsingKeyId(keyPairSource.name)
        null -> {}
    }
}
