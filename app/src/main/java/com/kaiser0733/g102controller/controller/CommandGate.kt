package com.kaiser0733.g102controller.controller

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/** Shared across Activities. Busy requests are rejected, never queued. */
class CommandGate(private val executor: Executor) {
    private val active = AtomicBoolean(false)
    val busy: Boolean get() = active.get()

    fun submit(work: () -> Unit, finished: () -> Unit): Boolean {
        if (!active.compareAndSet(false, true)) return false
        try {
            executor.execute {
                try {
                    work()
                } finally {
                    active.set(false)
                    finished()
                }
            }
        } catch (failure: RuntimeException) {
            active.set(false)
            throw failure
        }
        return true
    }
}
