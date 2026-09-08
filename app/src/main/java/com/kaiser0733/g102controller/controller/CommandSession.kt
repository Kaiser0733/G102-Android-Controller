package com.kaiser0733.g102controller.controller

/** Resume never revives cancelled work; only a new explicit button command can. */
class CommandSession {
    @Volatile var foreground = false
        private set
    @Volatile var cancelled = true
        private set
    val canContinue: Boolean get() = foreground && !cancelled

    fun resume() { foreground = true }
    fun stop() { foreground = false; cancelled = true }
    fun detach() { cancelled = true }
    fun begin(): Boolean {
        if (!foreground) return false
        cancelled = false
        return true
    }
}
