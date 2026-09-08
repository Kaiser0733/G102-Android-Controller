package com.kaiser0733.g102controller

import com.kaiser0733.g102controller.controller.CommandGate
import com.kaiser0733.g102controller.controller.CommandSession
import com.kaiser0733.g102controller.controller.LightingActions
import com.kaiser0733.g102controller.diagnostics.CrashLog
import com.kaiser0733.g102controller.diagnostics.DiagnosticBuffer
import com.kaiser0733.g102controller.protocol.ColorUtils
import com.kaiser0733.g102controller.settings.LightingConfig
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

class RuntimeHotfixTest {
    @Test fun stopCancelsAndResumeNeverRestartsOldWork() {
        val session = CommandSession()
        assertFalse(session.begin())
        session.resume()
        assertFalse(session.canContinue)
        assertTrue(session.begin())
        assertTrue(session.canContinue)
        session.stop()
        assertFalse(session.canContinue)
        session.resume()
        assertFalse(session.canContinue)
        assertTrue(session.begin())
    }

    @Test fun detachStopsRemainingPacketsAndReconnectDoesNotStartWork() {
        val session = CommandSession()
        session.resume()
        session.begin()
        var packets = 0
        repeat(3) {
            if (session.canContinue) { packets++; session.detach() }
        }
        assertEquals(1, packets)
        session.resume()
        assertFalse(session.canContinue)
        assertTrue(session.begin())
    }

    @Test fun stoppedQueuedCommandNeverTouchesHardware() {
        val queue = mutableListOf<Runnable>()
        val gate = CommandGate(Executor { queue.add(it) })
        val session = CommandSession()
        session.resume()
        session.begin()
        var packets = 0
        gate.submit({ if (session.canContinue) packets++ }, {})
        session.stop()
        queue.single().run()
        assertEquals(0, packets)
        assertFalse(gate.busy)
    }

    @Test fun configurationEventsNeverSend() {
        var sends = 0
        val controls = LightingActions { _, _ -> sends++ }
        // Initial render/config load, including repeated programmatic Spinner selection.
        controls.config = LightingConfig.deserialize("WAVE|b76e79|50|1000|6|ff0000,00ff00,0000ff")!!
        repeat(1000) {
            controls.config = controls.config.copy(effect = LightingConfig.EFFECT_WAVE)
            controls.config = controls.config.copy(brightnessPercent = 10)
            controls.config = controls.config.copy(color = 0xB76E79)
            controls.config = controls.config.copy(rateMs = 65535)
            controls.config = controls.config.copy(waveDirection = 1)
            controls.config = controls.config.copy(zoneColors = listOf(1, 2, 3))
            ColorUtils.rgbToAndroidColor(controls.config.color)
            ColorUtils.rateToSlider(controls.config.rateMs)
            controls.config.serialize()
        }
        assertEquals(0, sends)
        // No executor/scheduler is available to this configuration model.
    }

    @Test fun onlyExplicitButtonsSendAndOffPreservesV1() {
        val sent = mutableListOf<Pair<String, List<String>>>()
        val controls = LightingActions { packets, label ->
            sent.add(label to packets.map { packet -> packet.joinToString("") { "%02x".format(it.toInt() and 255) } })
        }
        controls.execute(LightingActions.Action.OFF)
        assertEquals(listOf("10ff0e5b010305", "11ff0e1b00010000000000000000000001000000"), sent.single().second)
        controls.execute(LightingActions.Action.ON)
        controls.execute(LightingActions.Action.APPLY)
        assertEquals(listOf("RGB OFF", "RGB ON", "APPLY"), sent.map { it.first })
    }

    @Test fun swatchesAlwaysUseOpaqueAlpha() {
        val vectors = listOf(0xFF0000 to 0xFFFF0000L, 0x00FF00 to 0xFF00FF00L,
            0x0000FF to 0xFF0000FFL, 0xFFFFFF to 0xFFFFFFFFL,
            0 to 0xFF000000L, 0xB76E79 to 0xFFB76E79L)
        vectors.forEach { (rgb, argb) -> assertEquals(argb.toInt(), ColorUtils.rgbToAndroidColor(rgb)) }
        assertEquals(0xFF123456.toInt(), ColorUtils.rgbToAndroidColor(0x12123456))
    }

    @Test fun diagnosticsKeepNewest500Lines() {
        val buffer = DiagnosticBuffer()
        repeat(1000) { buffer.add("line $it") }
        assertEquals(500, buffer.snapshot().size)
        assertEquals("line 500", buffer.snapshot().first())
        assertEquals("line 999", buffer.snapshot().last())
    }

    @Test fun multilineAndOversizedEntriesStayBounded() {
        val buffer = DiagnosticBuffer(2)
        buffer.add("old\nnext\n" + "x".repeat(2000))
        assertEquals(2, buffer.snapshot().size)
        assertEquals("next", buffer.snapshot().first())
        assertEquals(1024, buffer.snapshot().last().length)
    }

    @Test fun crashLogCapsNewestBytesAndMigratesOversizedFile() {
        val file = Files.createTempFile("g102-crash-test", ".txt").toFile()
        try {
            file.writeText("x".repeat(200000))
            val log = CrashLog(file)
            log.trim()
            assertEquals(65536L, file.length())
            repeat(100) { log.append("y".repeat(2000)) }
            log.append("NEWEST")
            assertEquals(65536L, file.length())
            assertTrue(log.read().endsWith("NEWEST"))
            log.append("z".repeat(100000))
            assertEquals(65536L, file.length())
            assertEquals("z".repeat(65536), log.read())
        } finally { file.delete() }
    }

    @Test fun rapid20TapsQueueOnlyOneAndReleaseOnCompletion() {
        val queue = mutableListOf<Runnable>()
        val gate = CommandGate(Executor { queue.add(it) })
        var completed = 0
        var writes = 0
        assertTrue(gate.submit({ writes++ }, { completed++ }))
        repeat(20) { assertFalse(gate.submit({ writes++ }, { completed++ })) }
        assertEquals(1, queue.size)
        assertTrue(gate.busy)
        queue.removeAt(0).run()
        assertFalse(gate.busy)
        assertEquals(1, writes)
        assertEquals(1, completed)
        assertTrue(gate.submit({}, {}))
        queue.removeAt(0).run()
    }

    @Test fun simultaneousCallersAdmitOnlyOneSequence() {
        val queued = java.util.concurrent.ConcurrentLinkedQueue<Runnable>()
        val gate = CommandGate(Executor { queued.add(it) })
        val callers = java.util.concurrent.Executors.newFixedThreadPool(8)
        try {
            val attempts = (1..100).map { callers.submit<Boolean> { gate.submit({}, {}) } }
            assertEquals(1, attempts.count { it.get(5, java.util.concurrent.TimeUnit.SECONDS) })
            assertEquals(1, queued.size)
            queued.remove().run()
            assertFalse(gate.busy)
        } finally { callers.shutdownNow() }
    }

    @Test fun throwingWorkerAlwaysReleasesGate() {
        val queue = mutableListOf<Runnable>()
        val gate = CommandGate(Executor { queue.add(it) })
        var completed = false
        gate.submit({ throw IllegalStateException("unplugged") }, { completed = true })
        assertThrows(IllegalStateException::class.java) { queue.single().run() }
        assertFalse(gate.busy)
        assertTrue(completed)
    }

    @Test fun rejectedExecutorAlwaysReleasesGate() {
        val gate = CommandGate(Executor { throw RejectedExecutionException("stopped") })
        assertThrows(RejectedExecutionException::class.java) { gate.submit({}, {}) }
        assertFalse(gate.busy)
    }
}
