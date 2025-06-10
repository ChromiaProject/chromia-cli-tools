package com.chromia.cli.tools.ft

import com.chromia.ft4.fetchEvmAuthMessage
import com.chromia.lib.ft4.core.auth.Signature
import com.chromia.lib.ft4.external.auth.evmAuthOperation
import com.chromia.lib.ft4.external.auth.evmSignaturesOperation
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.mordant.rendering.TextStyles.Companion.hyperlink
import com.google.gson.Gson
import com.google.gson.JsonArray
import net.postchain.client.core.PostchainClient
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.common.wrap
import net.postchain.gtv.Gtv
import org.apache.commons.text.StringEscapeUtils
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.webJars
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

fun CoreCliktCommand.addEvmAuthOperation(
        client: PostchainClient,
        transactionBuilder: TransactionBuilder,
        opName: String,
        opArgs: List<Gtv>,
        evmAddress: ByteArray,
        accountId: ByteArray,
        authDescriptorId: ByteArray,
        launchWebBrowser: Boolean = true,
        urlNotifier: (String) -> Unit = {},
) {
    val encodedSignatures = fetchEvmSignatures(
            client,
            listOf(OperationDescriptor(opName, opArgs, forEvmSignatures = false)),
            evmAddress,
            accountId,
            authDescriptorId,
            launchWebBrowser,
            urlNotifier
    )
    transactionBuilder.evmAuthOperation(accountId, authDescriptorId, encodedSignatures)
}

fun CoreCliktCommand.addEvmSignaturesOperation(
        client: PostchainClient,
        transactionBuilder: TransactionBuilder,
        opName: String,
        opArgs: List<Gtv>,
        evmAddress: ByteArray,
        accountId: ByteArray,
        authDescriptorId: ByteArray,
        launchWebBrowser: Boolean = true,
        urlNotifier: (String) -> Unit = {},
) {
    val encodedSignatures = fetchEvmSignatures(
            client,
            listOf(OperationDescriptor(opName, opArgs, forEvmSignatures = true)),
            evmAddress,
            accountId,
            authDescriptorId,
            launchWebBrowser,
            urlNotifier
    )
    transactionBuilder.evmSignaturesOperation(listOf(evmAddress), encodedSignatures)
}

data class OperationDescriptor(
        val opName: String,
        val opArgs: List<Gtv>,
        val forEvmSignatures: Boolean
)

fun CoreCliktCommand.fetchEvmSignatures(
        client: PostchainClient,
        operations: List<OperationDescriptor>,
        evmAddress: ByteArray,
        accountId: ByteArray,
        authDescriptorId: ByteArray,
        launchWebBrowser: Boolean = true,
        urlNotifier: (String) -> Unit = {}
): List<Signature> {
    val authMessages = operations.map {
        fetchEvmAuthMessage(client, accountId, authDescriptorId, it.opName, it.opArgs, it.forEvmSignatures)
    }

    val html = this::class.java.getResource("/com/chromia/cli/tools/evm_auth/index.html")!!.readText()
            .replace("{{address}}", "0x${evmAddress.toHex()}")
            .replace("{{messages}}", authMessages.joinToString(separator = "") {
                "\"${StringEscapeUtils.escapeEcmaScript(it)}\",\n"
            })
    val signaturesFuture = CompletableFuture<String>()
    val server = routes(
            "/" bind GET to { Response(OK).header("Content-Type", "text/html").body(html) },
            "/signatures" bind POST to { request ->
                signaturesFuture.complete(request.bodyString())
                Response(OK)
            },
            "/error" bind POST to { request ->
                signaturesFuture.completeExceptionally(CliktError(request.bodyString()))
                Response(OK)
            },
            webJars()
    ).asServer(Netty(port = 0)).start()
    val url = "http://localhost:${server.port()}"
    if (launchWebBrowser) {
        openWebLink(url)
    }
    urlNotifier(url)
    val rawSignatures = try {
        signaturesFuture.get()
    } catch (e: ExecutionException) {
        throw (e.cause ?: e)
    } finally {
        server.stop()
    }
    val signatures = Gson().fromJson(rawSignatures, JsonArray::class.java)
    return signatures.asList().map {
        Signature(
                r = it.asJsonObject.get("r").asString.drop(2).hexStringToByteArray().wrap(),
                s = it.asJsonObject.get("s").asString.drop(2).hexStringToByteArray().wrap(),
                v = it.asJsonObject.get("v").asLong)
    }
}

fun CoreCliktCommand.openWebLink(url: String) {
    val os = System.getProperty("os.name")
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else if (os.contains("mac")) {
            Runtime.getRuntime().exec(arrayOf("open", url))
        } else if (os.contains("nix") || os.contains("nux")) {
            Runtime.getRuntime().exec(arrayOf("xdg-open", url))
        } else {
            terminalWebLink(url)
        }
    } catch (_: IOException) {
        terminalWebLink(url)
    }
}

fun CoreCliktCommand.terminalWebLink(url: String) {
    echo("Open ${hyperlink(url)(url)} in your web browser to continue")
}
