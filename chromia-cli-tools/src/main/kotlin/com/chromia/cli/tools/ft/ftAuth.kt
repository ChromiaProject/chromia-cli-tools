package com.chromia.cli.tools.ft

import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.directory1.lib.ft4.external.accounts.getAccountAuthDescriptorsBySigner
import com.chromia.directory1.lib.ft4.external.accounts.getAccountsBySigner
import com.chromia.directory1.lib.ft4.external.auth.ftAuthOperation
import com.chromia.directory1.lib.ft4.external.auth.getAuthFlags
import com.chromia.directory1.lib.ft4.utils.PagedResult
import com.chromia.directory1.lib.ft4.version.getVersion
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.input.interactiveSelectList
import net.postchain.client.core.PostchainQuery
import net.postchain.client.exception.ClientError
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.toHex
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap

fun CoreCliktCommand.initFtAuth(client: PostchainQuery) {
    val version = try {
        client.getVersion()
    } catch (e: ClientError) {
        throw PrintMessage("Dapp is not FT4 compatible: ${e.errorMessage}", statusCode = 1)
    }
    // 0.0.* -> 0.3.*
    if (version.matches(Regex("^0\\.[0-3]\\.(0|[1-9]\\d*).*"))) {
        throw PrintMessage("Versions before release 0.4.0 are not supported, current FT4 version $version is to old", statusCode = 1)
    }
}

fun CoreCliktCommand.addFtAuthenticationOperation(client: PostchainQuery, transactionBuilder: TransactionBuilder, opName: String, signer: ByteArray, optionalAccountId: String? = null, optionalAuthDescriptorId: String? = null) {
    val (accountId, authDescriptorId) = findFtAccountIdAndAuthDescriptorId(client, optionalAccountId, signer, opName, optionalAuthDescriptorId)
    addFtAuthOperation(transactionBuilder, accountId, authDescriptorId)
}

fun CoreCliktCommand.findFtAccountIdAndAuthDescriptorId(client: PostchainQuery, optionalAccountId: String?, signer: ByteArray, opName: String, optionalAuthDescriptorId: String?): Pair<ByteArray, ByteArray> {
    val accountId = optionalAccountId?.hexStringToByteArray() ?: findAccountId(client, signer)
    return accountId to findValidAuthDescriptorIdForOperation(client, opName, accountId, signer, optionalAuthDescriptorId).data
}

private fun findAuthDescriptors(descriptors: List<Ft4GetAccountAuthDescriptorsBySignerResult>, optionalAuthDescriptorId: String?, signer: ByteArray): List<Ft4GetAccountAuthDescriptorsBySignerResult> {
    return descriptors.filter { descriptor ->
        when {
            !optionalAuthDescriptorId.isNullOrEmpty() -> {
                descriptor.id == optionalAuthDescriptorId.hexStringToWrappedByteArray()
            }

            descriptor.authType == AuthType.S -> {
                descriptor.getSingleKey().wrap() == signer.wrap()
            }

            descriptor.authType == AuthType.M -> {
                descriptor.getMultiKeys().any { oneSigner ->
                    oneSigner.asByteArray().wrap() == signer.wrap()
                }
            }

            else -> {
                throw PrintMessage("Authtype: ${descriptor.authType} is not supported in FTAuthenticator")
            }
        }
    }
}

private fun CoreCliktCommand.findValidAuthDescriptorIdForOperation(client: PostchainQuery, opName: String, accountId: ByteArray, signer: ByteArray, optionalAuthDescriptorId: String?): WrappedByteArray {
    val flags = client.getAuthFlags(opName)
    val authDescriptors = client.getAccountAuthDescriptorsBySigner(accountId, signer = signer)

    val authDescriptorsCandidates = findAuthDescriptors(authDescriptors, optionalAuthDescriptorId, signer)
    val authDescriptor = if (authDescriptorsCandidates.isEmpty()) {
        throw PrintMessage("No valid account descriptor found. User not authorized for operation $opName", statusCode = 1)
    } else if (authDescriptorsCandidates.size == 1) {
        authDescriptorsCandidates.first()
    } else if (terminal.terminalInfo.inputInteractive) {
        val candidateMap = authDescriptorsCandidates.associateBy { it.id.toHex() }
        (terminal.interactiveSelectList(
                entries = authDescriptorsCandidates.map {
                    """id: ${it.id}
                                |flags: ${it.getFlags()}
                                |signatures needed: ${it.getNumberOfSigners()} 
                                |keys: ${it.getKeysAsFormattedString()}
                                |""".trimMargin()
                },
                title = "Please select a valid auth descriptor"
        )
                ?: throw Abort()).let { candidateMap[it.substring(4, it.indexOf("\n"))]!! }
    } else {
        authDescriptorsCandidates.first()
    }

    if (!isValid(flags, authDescriptor)) {
        throw PrintMessage("No valid account descriptor found. Operation $opName requires the flag(s): $flags, while the flag(s) of the auth descriptor is: ${authDescriptor.getFlags()}", statusCode = 1)
    }
    return authDescriptor.id
}

private fun isValid(requiredFlags: List<String>, authDescriptor: Ft4GetAccountAuthDescriptorsBySignerResult): Boolean {
    val flags = authDescriptor.getFlags()
    return flags.containsAll(requiredFlags)
}

fun CoreCliktCommand.findAccountId(client: PostchainQuery, signer: ByteArray): ByteArray {
    val accounts = client.getAccountsBySigner(signer, 100, null).getAccountIds()
    return accountPicker(accounts, signer)
}

private fun PagedResult.getAccountIds() = this.data.map { it.asDict()["id"]!!.asByteArray() }

private fun CoreCliktCommand.accountPicker(accounts: List<ByteArray>, signer: ByteArray): ByteArray {
    return accounts.let {
        if (it.isEmpty()) throw PrintMessage("No FT4 Account found for signer: ${signer.toHex()}", statusCode = 1)
        if (it.size == 1) it.first()
        else if (terminal.terminalInfo.inputInteractive) {
            terminal.interactiveSelectList(
                    entries = it.map { ac -> ac.toHex() }.toSet(),
                    title = "More than one account found, which one should we use?"
            )
                    ?.hexStringToByteArray()
                    ?: throw Abort()
        } else {
            throw PrintMessage("More than one account found, please specify which one to use with --ft-account-id option",
                    statusCode = 1)
        }
    }
}

fun CoreCliktCommand.addFtAuthOperation(transactionBuilder: TransactionBuilder, accountId: ByteArray, authDescriptorId: ByteArray) {
    transactionBuilder.ftAuthOperation(accountId, authDescriptorId)
}
