package com.kaiser0733.g102controller.diagnostics

import java.util.ArrayDeque

class DiagnosticBuffer(private val capacity: Int = 500) {
    init { require(capacity > 0) }
    private val lines = ArrayDeque<String>()

    @Synchronized fun add(message: String) {
        // Bound individual entries too; a single stack/device dump cannot defeat the cap.
        // 512 keeps the full 500-line worst case ~256KB — safe for clipboard + TextView.
        message.lineSequence().forEach { line ->
            if (lines.size == capacity) lines.removeFirst()
            lines.addLast(line.take(512))
        }
    }
    @Synchronized fun snapshot(): List<String> = lines.toList()
    @Synchronized fun isEmpty(): Boolean = lines.isEmpty()
}
