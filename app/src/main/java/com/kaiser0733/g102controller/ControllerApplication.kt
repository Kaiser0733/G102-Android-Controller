package com.kaiser0733.g102controller

import android.app.Application
import android.os.Process
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kaiser0733.g102controller.controller.CommandGate
import com.kaiser0733.g102controller.diagnostics.CrashLog
import com.kaiser0733.g102controller.diagnostics.DiagnosticBuffer
import java.io.File
import java.util.concurrent.Executors
import kotlin.system.exitProcess

class ControllerApplication : Application() {
    val commands = CommandGate(Executors.newSingleThreadExecutor { task ->
        Thread(task, "g102-command").apply { isDaemon = true }
    })
    val diagnostics = DiagnosticBuffer()
    @Volatile var lastOutcome: String? = null
    // Main-thread listener belongs only to the visible Activity, cleared on stop.
    var commandListener: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun notifyCommandFinished() {
        mainHandler.post { commandListener?.invoke() }
    }

    lateinit var crashLog: CrashLog
        private set

    override fun onCreate() {
        super.onCreate()
        crashLog = CrashLog(File(getExternalFilesDir(null) ?: filesDir, "crash.txt"))
        try { crashLog.trim() } catch (_: Exception) { /* Reporting must not prevent launch. */ }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val log = crashLog // No Activity or mutable UI config retained by the global handler.
        Thread.setDefaultUncaughtExceptionHandler { thread, failure ->
            try {
                log.append("\n==== CRASH ${System.currentTimeMillis()} thread=${thread.name} ====\n" +
                    Log.getStackTraceString(failure).takeLast(60000) + "\n")
            } catch (_: Throwable) { /* Delegate even when storage is unavailable. */ }
            try {
                previous?.uncaughtException(thread, failure)
            } finally {
                // Android's handler normally terminates. Never silently continue a fatal process.
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }
}
