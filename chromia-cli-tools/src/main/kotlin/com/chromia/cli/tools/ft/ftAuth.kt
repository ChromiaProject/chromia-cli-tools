package com.chromia.cli.tools.ft

import com.chromia.ft4.flags
import com.chromia.ft4.getMultiSigners
import com.chromia.ft4.getSingleSigner
import com.chromia.ft4.numberOfSigners
import com.chromia.lib.ft4.core.accounts.AuthType
import com.chromia.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.lib.ft4.external.accounts.getAccountAuthDescriptorsBySigner
import com.chromia.lib.ft4.external.accounts.getAccountsBySigner
import com.chromia.lib.ft4.external.accounts.strategies.registerAccountOperation
import com.chromia.lib.ft4.external.auth.ftAuthOperation
import com.chromia.lib.ft4.external.auth.getAuthFlags
import com.chromia.lib.ft4.utils.PagedResult
import com.chromia.lib.ft4.version.getVersion
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.input.interactiveSelectList
import net.postchain.client.core.PostchainQuery
import net.postchain.client.exception.ClientError
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap

fun CoreCliktCommand.initFtAuth(client: PostchainQuery) {
    val version = try {
        client.getVersion()
    } catch (e: ClientError) {
        throw CliktError("Dapp is not FT4 compatible: ${e.errorMessage}")
    }
    // 0.0.* -> 0.3.*
    if (version.matches(Regex("^0\\.[0-3]\\.(0|[1-9]\\d*).*"))) {
        throw CliktError("Versions before release 0.4.0 are not supported, current FT4 version $version is to old")
    }
}

fun CoreCliktCommand.addFtAuthenticationOperation(
        client: PostchainQuery,
        transactionBuilder: TransactionBuilder,
        opName: String,
        signer: ByteArray,
        optionalAccountId: String? = null,
        optionalAuthDescriptorId: String? = null
) {
    addFtAuthenticationOp(
            client, transactionBuilder, opName, signer, optionalAccountId?.hexStringToByteArray(),
            optionalAuthDescriptorId?.hexStringToByteArray()
    )
}

fun CoreCliktCommand.addFtAuthenticationOp(
        client: PostchainQuery,
        transactionBuilder: TransactionBuilder,
        opName: String,
        signer: ByteArray,
        optionalAccountId: ByteArray? = null,
        optionalAuthDescriptorId: ByteArray? = null
) {
    val (accountId, authDescriptorId) = findFtAccountIdWithAuthDescriptorId(client, optionalAccountId, signer, opName,
            optionalAuthDescriptorId)
    addFtAuthOperation(transactionBuilder, accountId, authDescriptorId)
}

fun CoreCliktCommand.findFtAccountIdAndAuthDescriptorId(
        client: PostchainQuery,
        optionalAccountId: String?,
        signer: ByteArray,
        opName: String,
        optionalAuthDescriptorId: String?
): Pair<ByteArray, ByteArray> = findFtAccountIdWithAuthDescriptorId(
        client, optionalAccountId?.hexStringToByteArray(), signer, opName,
        optionalAuthDescriptorId?.hexStringToByteArray()
)

fun CoreCliktCommand.findFtAccountIdWithAuthDescriptorId(
        client: PostchainQuery,
        optionalAccountId: ByteArray?,
        signer: ByteArray,
        opName: String,
        optionalAuthDescriptorId: ByteArray?
): Pair<ByteArray, ByteArray> {
    val accountId = optionalAccountId ?: findAccountId(client, signer)
    return accountId to findValidAuthDescriptorIdForOperation(
            client, opName, accountId, signer, optionalAuthDescriptorId
    ).data
}

private fun findAuthDescriptors(
        descriptors: List<Ft4GetAccountAuthDescriptorsBySignerResult>,
        optionalAuthDescriptorId: ByteArray?,
        signer: ByteArray
): List<Ft4GetAccountAuthDescriptorsBySignerResult> {
    return descriptors.filter { descriptor ->
        when {
            optionalAuthDescriptorId != null && optionalAuthDescriptorId.isNotEmpty() -> {
                descriptor.id == optionalAuthDescriptorId.wrap()
            }

            descriptor.authType == AuthType.S -> {
                descriptor.getSingleSigner().wrap() == signer.wrap()
            }

            descriptor.authType == AuthType.M -> {
                descriptor.getMultiSigners().any { oneSigner ->
                    oneSigner.wrap() == signer.wrap()
                }
            }

            else -> {
                throw CliktError("Authtype: ${descriptor.authType} is not supported in FTAuthenticator")
            }
        }
    }
}

private fun CoreCliktCommand.findValidAuthDescriptorIdForOperation(
        client: PostchainQuery,
        opName: String,
        accountId: ByteArray,
        signer: ByteArray,
        optionalAuthDescriptorId: ByteArray?
): WrappedByteArray {
    val flags = client.getAuthFlags(opName)
    val authDescriptors = client.getAccountAuthDescriptorsBySigner(accountId, signer = signer)

    val authDescriptorsCandidates = findAuthDescriptors(authDescriptors, optionalAuthDescriptorId, signer)
    val authDescriptor = if (authDescriptorsCandidates.isEmpty()) {
        throw CliktError("No valid account descriptor found. User not authorized for operation $opName")
    } else if (authDescriptorsCandidates.size == 1) {
        authDescriptorsCandidates.first()
    } else if (terminal.terminalInfo.inputInteractive) {
        val candidateMap = authDescriptorsCandidates.associateBy { it.id.toHex() }
        (terminal.interactiveSelectList(
                entries = authDescriptorsCandidates.map {
                    """id: ${it.id}
                                |flags: ${it.flags()}
                                |signatures needed: ${it.numberOfSigners()} 
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
        throw CliktError(
                """No valid account descriptor found. 
                    |Operation $opName requires the flag(s): $flags, 
                    |while the flag(s) of the auth descriptor is: ${authDescriptor.flags()}""".trimMargin()
        )
    }
    return authDescriptor.id
}

private fun isValid(requiredFlags: List<String>, authDescriptor: Ft4GetAccountAuthDescriptorsBySignerResult): Boolean {
    val flags = authDescriptor.flags()
    return flags.containsAll(requiredFlags)
}

fun CoreCliktCommand.findAccountId(client: PostchainQuery, signer: ByteArray): ByteArray {
    val accounts = client.getAccountsBySigner(signer, 100, null).getAccountIds()
    return accountPicker(accounts, signer)
}

private fun PagedResult.getAccountIds() = this.data.map { it.asDict()["id"]!!.asByteArray() }

private fun CoreCliktCommand.accountPicker(accounts: List<ByteArray>, signer: ByteArray): ByteArray {
    return accounts.let {
        if (it.isEmpty()) throw CliktError("No FT4 Account found for signer: ${signer.toHex()}")
        if (it.size == 1) it.first()
        else if (terminal.terminalInfo.inputInteractive) {
            terminal.interactiveSelectList(
                    entries = it.map { ac -> ac.toHex() }.toSet(),
                    title = "More than one account found, which one should we use?"
            )
                    ?.hexStringToByteArray()
                    ?: throw Abort()
        } else {
            throw CliktError("More than one account found, please specify which one to use with --ft-account-id option")
        }
    }
}

fun CoreCliktCommand.addFtAuthOperation(
        transactionBuilder: TransactionBuilder,
        accountId: ByteArray,
        authDescriptorId: ByteArray
) {
    transactionBuilder.ftAuthOperation(accountId, authDescriptorId)
}

fun CoreCliktCommand.addFtRegisterAccountOperation(
        transactionBuilder: TransactionBuilder,
) {
    transactionBuilder.registerAccountOperation()
}

fun Ft4GetAccountAuthDescriptorsBySignerResult.getKeysAsFormattedString(): String = if (this.authType == AuthType.S) {
    this.args.asArray()[1].toString()
} else {
    this.args.asArray()[2].asArray().joinToString("\n")
}
