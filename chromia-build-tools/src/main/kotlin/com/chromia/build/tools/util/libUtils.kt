package com.chromia.build.tools.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun File.safeDelete(): Boolean = if (isDirectory) deleteRecursively() else delete()

fun Path.safeDelete(): Boolean = runCatching {
    Files.walk(this).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
}.isSuccess

inline fun <T, R> Collection<T>.ifNotEmpty(block: (Collection<T>) -> R): R? =
    if (isNotEmpty()) block(this) else null

inline fun <K, V> Map<K, V>.ifNotEmpty(block: (Map<K, V>) -> Unit) =
    if (!isEmpty()) block(this) else null