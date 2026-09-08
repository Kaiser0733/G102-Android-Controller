package com.kaiser0733.g102controller

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.kaiser0733.g102controller.protocol.ProtocolPackets
import com.kaiser0733.g102controller.usb.LogitechUsbManager
import com.kaiser0733.g102controller.usb.UsbDiagnostics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v1 does exactly one thing: TURN RGB OFF on a G102/G203 LIGHTSYNC over USB.
 * Every claim the UI makes is backed by a real USB-layer result; "sent successfully"
 * never means "visually confirmed" — that is the user's eyes' job.
 */
class MainActivity : Activity() {

    private lateinit var usb: LogitechUsbManager

    private lateinit var textDevice: TextView
    private lateinit var textResult: TextView
    private lateinit var textDiagnostics: TextView
    private lateinit var btnGrantUsb: Button
    private lateinit var btnRgbOff: Button
    private lateinit var btnToggleDiagnostics: Button
    private lateinit var btnCopyDiagnostics: Button
    private lateinit var btnShareDiagnostics: Button

    private val diagLines = mutableListOf<String>()
    private var diagnosticsVisible = false
    private var busy = false

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usb = LogitechUsbManager(this) { line -> onLog(line) }

        textDevice = findViewById(R.id.textDevice)
        textResult = findViewById(R.id.textResult)
        textDiagnostics = findViewById(R.id.textDiagnostics)
        btnGrantUsb = findViewById(R.id.btnGrantUsb)
        btnRgbOff = findViewById(R.id.btnRgbOff)
        btnToggleDiagnostics = findViewById(R.id.btnToggleDiagnostics)
        btnCopyDiagnostics = findViewById(R.id.btnCopyDiagnostics)
        btnShareDiagnostics = findViewById(R.id.btnShareDiagnostics)
        textResult.text = getString(R.string.status_unknown)

        btnGrantUsb.setOnClickListener {
            currentDevice()?.let { usb.requestPermission(it) }
        }
        btnRgbOff.setOnClickListener { sendRgbOff() }
        btnToggleDiagnostics.setOnClickListener { toggleDiagnostics() }
        btnCopyDiagnostics.setOnClickListener { copyDiagnostics() }
        btnShareDiagnostics.setOnClickListener { shareDiagnostics() }

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.1.0"
        } catch (_: Exception) {
            "0.1.0"
        }
        findViewById<TextView>(R.id.textFootnote).text = getString(R.string.footnote, versionName)

        usb.register { intent ->
            runOnUiThread {
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    UsbManager.ACTION_USB_DEVICE_DETACHED,
                    usb.permissionAction,
                    -> refreshDeviceState()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDeviceState()
    }

    override fun onDestroy() {
        usb.unregister()
        super.onDestroy()
    }

    // --- Device state ------------------------------------------------------------

    /** Preferred attached Logitech device, re-enumerated fresh every call. */
    private fun currentDevice(): UsbDevice? = usb.findLogitechDevices().firstOrNull()

    private fun refreshDeviceState() {
        val device = currentDevice()
        when {
            device == null -> {
                textDevice.text = getString(R.string.no_device)
                btnGrantUsb.visibility = View.GONE
                btnRgbOff.isEnabled = false
            }
            !usb.hasPermission(device) -> {
                renderDevice(device)
                btnGrantUsb.visibility = View.VISIBLE
                btnRgbOff.isEnabled = false
            }
            else -> {
                renderDevice(device)
                btnGrantUsb.visibility = View.GONE
                btnRgbOff.isEnabled = !busy
            }
        }
    }

    private fun renderDevice(device: UsbDevice) {
        val known = if (device.productId in LogitechUsbManager.KNOWN_LIGHTSYNC_PIDS) {
            "G102/G203 LIGHTSYNC family"
        } else {
            "unknown Logitech model — proceed with care"
        }
        textDevice.text =
            "Device: Logitech VID=0x%04x PID=0x%04x (%s)\nUSB: %s".format(
                device.vendorId, device.productId, known, device.deviceName,
            )
        if (diagLines.isEmpty()) {
            diagLines += UsbDiagnostics.describeDevice(device)
            renderDiagnostics()
        }
    }

    // --- The one v1 command ---------------------------------------------------------

    private fun sendRgbOff() {
        if (busy) return
        val deviceSnapshot = currentDevice() ?: run {
            textResult.text = getString(R.string.no_device)
            return
        }
        busy = true
        btnRgbOff.isEnabled = false
        textResult.text = getString(R.string.status_busy)
        diagLines += UsbDiagnostics.describeDevice(deviceSnapshot)
        renderDiagnostics()

        Thread {
            val outcome = runOffSequence(deviceSnapshot)
            runOnUiThread {
                busy = false
                textResult.text = outcome
                refreshDeviceState()
            }
        }.apply { name = "g102-rgb-off" }.start()
    }

    /**
     * Runs on a worker thread. Re-resolves the device by name (Android re-creates
     * UsbDevice objects on reconnect), opens, selects the HID++ interface, sends
     * the mode switch + solid black, and closes everything again.
     */
    private fun runOffSequence(snapshot: UsbDevice): String = try {
        val device = usb.findLogitechDevices().firstOrNull {
            it.deviceName == snapshot.deviceName
        } ?: return "RGB OFF failed: device disappeared before the command could run."

        if (!usb.hasPermission(device)) {
            return "RGB OFF failed: USB permission was not granted."
        }
        val connection = usb.openDevice(device)
            ?: return "RGB OFF failed: could not open USB device (permission missing?)."

        var claimedInterface: android.hardware.usb.UsbInterface? = null
        try {
            val iface = usb.selectHidppInterface(device)
                ?: return "RGB OFF failed: no HID++ capable interface found — copy diagnostics."

            if (!usb.claimInterface(connection, iface)) {
                return "RGB OFF failed: could not claim the HID++ interface — copy diagnostics."
            }
            claimedInterface = iface

            val switchPacket = ProtocolPackets.buildDisableOnboardMemoryPacket()
            val switchResult = usb.sendReportAndRead(connection, iface, switchPacket)

            val offPacket = ProtocolPackets.buildRgbOffPacket()
            val offResult = usb.sendReportAndRead(connection, iface, offPacket)

            buildResultMessage(switchResult, offResult)
        } finally {
            usb.close(connection, claimedInterface)
        }
    } catch (t: Throwable) {
        onLog("Sequence error: ${t.javaClass.simpleName}: ${t.message}")
        "RGB OFF failed: ${t.javaClass.simpleName}: ${t.message}"
    }

    /**
     * Honest semantics, deliberately verbose: black-fallback is labeled as such,
     * a missing response read is never hidden, and USB write success is never
     * inflated into visual confirmation.
     */
    private fun buildResultMessage(
        switchResult: com.kaiser0733.g102controller.usb.SendResult,
        offResult: com.kaiser0733.g102controller.usb.SendResult,
    ): String {
        if (switchResult.error != null) {
            return "RGB OFF failed at the mode-switch step: ${switchResult.error}. Copy diagnostics."
        }
        if (offResult.error != null) {
            return "RGB OFF failed at the color step: ${offResult.error}. Copy diagnostics."
        }
        if (offResult.bytesWritten < offPacketExpected) {
            return "RGB OFF failed: short transfer — ${offResult.bytesWritten} of $offPacketExpected bytes written."
        }

        val mode = "BLACK_FALLBACK (LEDs commanded to 0,0,0 — no true LED-off effect is known for this hardware)"
        val switchResp = switchResult.response
        val offResp = offResult.response
        val response = when {
            switchResp != null && offResp != null ->
                "Device responses: ${ProtocolPackets.toHex(switchResp)} / ${ProtocolPackets.toHex(offResp)}"
            offResp != null ->
                "Device response: ${ProtocolPackets.toHex(offResp)}"
            else ->
                "No response read (write still confirmed by the USB layer)"
        }
        return "RGB OFF command sent successfully ($mode). $response."
    }

    private val offPacketExpected = ProtocolPackets.LONG_REPORT_SIZE

    // --- Diagnostics ----------------------------------------------------------------

    private fun onLog(line: String) {
        // Called from both the UI thread and the command worker thread.
        runOnUiThread {
            diagLines += "[%s] %s".format(timeFormat.format(Date()), line)
            renderDiagnostics()
        }
    }

    private fun renderDiagnostics() {
        if (diagnosticsVisible) {
            textDiagnostics.visibility = View.VISIBLE
            textDiagnostics.text = diagLines.joinToString("\n")
        }
    }

    private fun toggleDiagnostics() {
        diagnosticsVisible = !diagnosticsVisible
        btnToggleDiagnostics.text =
            if (diagnosticsVisible) getString(R.string.btn_diagnostics_toggle).replace("▾", "▴")
            else getString(R.string.btn_diagnostics_toggle)
        if (diagnosticsVisible) {
            textDiagnostics.visibility = View.VISIBLE
            textDiagnostics.text = diagLines.joinToString("\n").ifEmpty { getString(R.string.diag_empty) }
        } else {
            textDiagnostics.visibility = View.GONE
        }
    }

    private fun diagnosticsReport(): String = diagLines.joinToString("\n")

    private fun copyDiagnostics() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("G102 diagnostics", diagnosticsReport()))
        Toast.makeText(this, R.string.diag_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareDiagnostics() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, diagnosticsReport())
        }
        startActivity(Intent.createChooser(send, null))
    }
}
