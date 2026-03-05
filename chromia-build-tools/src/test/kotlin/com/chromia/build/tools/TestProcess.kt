package com.chromia.build.tools

import assertk.Assert
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.support.expected
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Wraps a running CLI process for use in integration tests.
 *
 * Handles process lifecycle, output reading, and assertions. Instances are created via [Builder].
 *
 * Example usage:
 * ```kotlin
 * TestProcess.Builder("node", "start")
 *     .awaitCompletion(false)
 *     .startCondition("Node is running")
 *     .start { process ->
 *         // do work while process is running
 *     }
 * ```
 *
 * The process is automatically destroyed when the block passed to [Builder.start] completes,
 * since [TestProcess] implements [AutoCloseable].
 */
class TestProcess private constructor(processBuilder: ProcessBuilder, startCondition: String?, wholeOutput: String?,
                                      shouldFinish: Boolean, expectedExitCode: Int, timeout: Duration, val verbose: Boolean,
                                      val input: String?, val binaryInput: ByteArray?, val partialOutput: String? = null) : AutoCloseable {

    val process = processBuilder.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))

    init {
        if (verbose) println("Starting command " + processBuilder.command().subList(1, processBuilder.command().size))
        if (input != null) process.outputStream.use { it.writer().use { w -> w.write(input) } }
        if (binaryInput != null) process.outputStream.use { it.write(binaryInput) }
        if (!startCondition.isNullOrBlank()) {
            waitUntil(startCondition, timeout)
        }
        if (shouldFinish) {
            process.waitFor(timeout.seconds, TimeUnit.SECONDS)
            assertThat(this).finished(expectedExitCode, wholeOutput, partialOutput)
        }
    }

    override fun close() {
        process.destroy()
        process.waitFor(2, TimeUnit.SECONDS)
    }

    private fun readLine(): String? = reader.readLine()
    fun readLines() = reader.readLines()
    fun waitUntil(msg: String, timeout: Duration) {
        var found = false
        val output = mutableListOf<String>()
        val startTime = System.currentTimeMillis()
        while (!found && System.currentTimeMillis() - startTime < timeout.toMillis()) {
            readLine()?.let {
                output.add(it)
                if (verbose) println(it)
                if (it.contains(msg)) {
                    found = true
                }
            }
        }
        if (!found) {
            println("Timed out ($timeout) waiting for message: $msg")
            println("Process output:")
            println(output.joinToString("\n"))
        }
        assertThat(found).isTrue()
    }

    private fun Assert<TestProcess>.finished(
        exitCode: Int,
        wholeOutput: String?,
        partialOutput: String? = null
    ) = given { actual ->
        if (actual.process.exitValue() == exitCode) {
            if (wholeOutput != null) {
                val processOutput = actual.readLines().joinToString("\n")
                assertThat(processOutput).isEqualTo(wholeOutput)
                if (verbose) println(processOutput)
            } else if (partialOutput != null) {
                assertThat(actual.readLines().contains(partialOutput))
            } else if (verbose) {
                actual.readLines().forEach { println(it) }
            }
        } else {
            expected("process to complete successfully, but exit code was ${actual.process.exitValue()} with logs: \n${actual.readLines().joinToString("\n")}")
        }
    }

    /**
     * Builder for configuring and launching a [TestProcess].
     *
     * The [args] are the CLI sub-command and its arguments (e.g. `"node", "query", "--network", "testnet"`).
     * The executable itself is resolved from the `DIST_EXECUTABLE` environment variable.
     *
     * Example — assert a command finishes successfully with specific output:
     * ```kotlin
     * TestProcess.Builder("node", "query")
     *     .timeout(Duration.ofSeconds(60))
     *     .exitCode(0)
     *     .wholeOutput("expected output")
     *     .start()
     * ```
     *
     * Example — start a long-running process and interact with it:
     * ```kotlin
     * TestProcess.Builder("node", "start")
     *     .awaitCompletion(false)
     *     .startCondition("Node is running")
     *     .start { process ->
     *         // do work while process is running
     *     }
     * ```
     */
    class Builder(vararg val args: String) {
        private var config: File? = null
        private var shouldFinish = true
        private var exitCode = 0
        private var timeout = Duration.ofSeconds(30)
        private var startCondition: String? = null
        private var wholeOutput: String? = null
        private var verbose = false
        private val env = mutableMapOf<String, String>()
        private var workingDir: File? = null
        private var input: String? = null
        private var binaryInput: ByteArray? = null
        private var partialOutput: String? = null

        /** Sets the config file path, passed as `-s <path>` to the executable. */
        fun setConfig(file: File) = apply { config = file }

        /** Sets the working directory for the process. */
        fun setWorkingDir(file: File) = apply { workingDir = file }

        /**
         * Whether to wait for the process to finish before returning from [start].
         * Defaults to `true`. Set to `false` for long-running processes where you only
         * need to wait for a [startCondition].
         */
        fun awaitCompletion(value: Boolean) = apply { shouldFinish = value }

        /** Expected exit code when [awaitCompletion] is `true`. Defaults to `0`. */
        fun exitCode(value: Int) = apply { exitCode = value }

        /** Maximum time to wait for the process to finish or for [startCondition] to appear. Defaults to 30 seconds. */
        fun timeout(value: Duration) = apply { timeout = value }

        /**
         * A string to scan for in the process output before returning from [start].
         */
        fun startCondition(condition: String) = apply { startCondition = condition }

        /** Asserts that the full process output exactly equals [output] after completion. */
        fun wholeOutput(output: String) = apply { wholeOutput = output }

        /** Prints each output line to stdout as it is produced. Also prints the command being run. */
        fun verbose() = apply { verbose = true }

        /** Adds environment variables to the process environment. */
        fun env(vararg envvars: Pair<String, String>) = apply { env.putAll(envvars) }

        /** Writes [s] to the process stdin and closes the stream before reading output. */
        fun input(s: String) = apply { input = s }

        /** Writes raw bytes to the process stdin and closes the stream before reading output. */
        fun binaryInput(b: ByteArray) = apply { binaryInput = b }

        /** Asserts that the process output contains [output] as one of its lines after completion. */
        fun partialOutput(output: String) = apply { partialOutput = output }

        /** Starts the process with default completion handling. Shorthand for `start {}`. */
        fun start() = start {}

        /**
         * Starts the process, applies the configured assertions, and invokes [onCompleted] with the
         * running [TestProcess]. The process is destroyed automatically when [onCompleted] returns.
         */
        fun <R> start(onCompleted: (TestProcess) -> R): R {
            val executable = System.getenv("DIST_EXECUTABLE")
            require(executable.isNotBlank()) { "DIST_EXECUTABLE not set" }
            require(File(executable).exists()) { "Executable $executable not found" }
            require(shouldFinish || wholeOutput == null) { "Cannot use wholeOutput if shouldFinish is false" }
            val processArgs = buildList<String> {
                add(executable)
                addAll(args)
                config?.let { addAll(listOf("-s", it.absolutePath)) }
            }
            val pb = ProcessBuilder(*processArgs.toTypedArray())
                    .apply {
                        redirectErrorStream(true)
                        workingDir?.let { directory(it) }
                        environment()["COLUMNS"] = "150"
                        environment().putIfAbsent("CHROMIA_HOME", System.getProperty("user.dir"))

                        env.forEach { (k, v) -> environment()[k] = v }
                    }
            return TestProcess(
                    pb, startCondition, wholeOutput, shouldFinish, exitCode, timeout, verbose, input, binaryInput, partialOutput
            ).use {
                if (verbose) {
                    thread(isDaemon = true) {
                        try {
                            while (true) {
                                println(it.reader.readLine() ?: break)
                            }
                        } catch (e: java.io.IOException) {
                            if (e.message != "Stream closed") throw e
                        }
                    }
                }
                onCompleted(it)
            }
        }
    }
}
