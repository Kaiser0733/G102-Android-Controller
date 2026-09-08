package com.kaiser0733.g102controller.diagnostics

import java.io.File
import java.io.RandomAccessFile

/** Reads only the tail, even when upgrading an already oversized crash.txt. */
class CrashLog(private val file: File, private val maxBytes: Int = 65536) {
    init { require(maxBytes > 0) }

    private fun tail(limit: Int): ByteArray {
        if (!file.exists() || limit == 0) return ByteArray(0)
        return RandomAccessFile(file, "r").use { input ->
            val size = minOf(input.length(), limit.toLong()).toInt()
            input.seek(input.length() - size)
            ByteArray(size).also { input.readFully(it) }
        }
    }

    @Synchronized fun append(message: String) {
        val encoded = message.toByteArray(Charsets.UTF_8)
        val newest = encoded.copyOfRange((encoded.size - maxBytes).coerceAtLeast(0), encoded.size)
        val previous = tail(maxBytes - newest.size)
        file.outputStream().use { output ->
            output.write(previous)
            output.write(newest)
        }
    }

    @Synchronized fun trim() {
        if (file.exists() && file.length() > maxBytes) {
            val retained = tail(maxBytes)
            file.writeBytes(retained)
        }
    }

    @Synchronized fun read(): String = tail(maxBytes).toString(Charsets.UTF_8)
}
