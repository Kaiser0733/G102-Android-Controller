package com.kaiser0733.g102controller

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.kaiser0733.g102controller.controller.RgbCommandComposer
import com.kaiser0733.g102controller.protocol.ColorUtils
import com.kaiser0733.g102controller.protocol.LightSyncEffects
import com.kaiser0733.g102controller.protocol.ProtocolPackets
import com.kaiser0733.g102controller.settings.LightingConfig
import com.kaiser0733.g102controller.settings.LightingStore
import com.kaiser0733.g102controller.usb.LogitechUsbManager
import com.kaiser0733.g102controller.usb.UsbDiagnostics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v2: full LIGHTSYNC controller — ON/OFF, solid color + hex + presets,
 * brightness (native where available), 5 effects + zones, speed/direction,
 * debounced live preview, app-side persistence, optional auto-apply.
 * The v1 transport path is untouched; every command rides the same
 * proven SET_REPORT lane.
 */
class MainActivity : Activity() {

    private lateinit var usb: LogitechUsbManager
    private lateinit var store: LightingStore

    private lateinit var textDevice: TextView
    private lateinit var textResult: TextView
    private lateinit var textDiagnostics: TextView
    private lateinit var textBrightnessLabel: TextView
    private lateinit var textSpeedLabel: TextView
    private lateinit var textZoneLabel: TextView
    private lateinit var textDirectionLabel: TextView
    private lateinit var btnGrantUsb: Button
    private lateinit var btnRgbOn: Button
    private lateinit var btnRgbOff: Button
    private lateinit var btnApplyColor: Button
    private lateinit var btnApplyEffect: Button
    private lateinit var btnToggleDiagnostics: Button
    private lateinit var btnCopyDiagnostics: Button
    private lateinit var btnShareDiagnostics: Button
    private lateinit var colorPreview: View
    private lateinit var editHex: EditText
    private lateinit var seekBrightness: SeekBar
    private lateinit var seekSpeed: SeekBar
    private lateinit var spinnerEffect: Spinner
    private lateinit var radioDirection: RadioGroup
    private lateinit var zoneRow: LinearLayout
    private lateinit var checkAutoApply: CheckBox

    private val diagLines = mutableListOf<String>()
    private var diagnosticsVisible = false
    private var busy = false
    private var suppressWatchers = false

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Current app-side config (never written to mouse memory)
    private var config = LightingConfig()

    // Debounced live preview: re-arm 120ms after the last change
    private var previewRunnable: Runnable? = null
    private val debounceMs = 120L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usb = LogitechUsbManager(this) { line -> onLog(line) }
        store = LightingStore(this)
        config = store.load() ?: LightingConfig()

        bindViews()
        setupControls()
        registerUsbEvents()
        renderConfig()
    }

    override fun onResume() {
        super.onResume()
        refreshDeviceState()
        maybeAutoApply()
    }

    override fun onDestroy() {
        usb.unregister()
        super.onDestroy()
    }

    // --- view binding -----------------------------------------------------

    private fun bindViews() {
        textDevice = findViewById(R.id.textDevice)
        textResult = findViewById(R.id.textResult)
        textDiagnostics = findViewById(R.id.textDiagnostics)
        textBrightnessLabel = findViewById(R.id.textBrightnessLabel)
        textBrightnessLabel.text = getString(R.string.brightness_label, config.brightnessPercent)
        textSpeedLabel = findViewById(R.id.textSpeedLabel)
        textSpeedLabel.text = getString(R.string.speed_label)
        textZoneLabel = findViewById(R.id.textZoneLabel)
        textZoneLabel.text = getString(R.string.zone_label)
        textDirectionLabel = findViewById(R.id.textDirectionLabel)
        textDirectionLabel.text = getString(R.string.direction_label)
        btnGrantUsb = findViewById(R.id.btnGrantUsb)
        btnRgbOn = findViewById(R.id.btnRgbOn)
        btnRgbOff = findViewById(R.id.btnRgbOff)
        btnApplyColor = findViewById(R.id.btnApplyColor)
        btnApplyEffect = findViewById(R.id.btnApplyEffect)
        btnToggleDiagnostics = findViewById(R.id.btnToggleDiagnostics)
        btnCopyDiagnostics = findViewById(R.id.btnCopyDiagnostics)
        btnShareDiagnostics = findViewById(R.id.btnShareDiagnostics)
        colorPreview = findViewById(R.id.colorPreview)
        editHex = findViewById(R.id.editHex)
        seekBrightness = findViewById(R.id.seekBrightness)
        seekSpeed = findViewById(R.id.seekSpeed)
        spinnerEffect = findViewById(R.id.spinnerEffect)
        radioDirection = findViewById(R.id.radioDirection)
        zoneRow = findViewById(R.id.zoneRow)
        checkAutoApply = findViewById(R.id.checkAutoApply)
    }

    // --- control setup ------------------------------------------------------

    private fun setupControls() {
        textResult.text = getString(R.string.status_unknown)
        btnGrantUsb.setOnClickListener {
            currentDevice()?.let { usb.requestPermission(it) }
        }
        btnRgbOff.setOnClickListener { sendCommandList(RgbCommandComposer.composeRgbOff(), "RGB OFF") }
        btnRgbOn.setOnClickListener {
            sendCommandList(RgbCommandComposer.composeRgbOn(config), "RGB ON")
        }
        btnApplyColor.setOnClickListener { applyCurrentConfig() }
        btnApplyEffect.setOnClickListener { applyCurrentConfig() }
        btnToggleDiagnostics.setOnClickListener { toggleDiagnostics() }
        btnCopyDiagnostics.setOnClickListener { copyDiagnostics() }
        btnShareDiagnostics.setOnClickListener { shareDiagnostics() }

        // Presets
        val presets = intArrayOf(0xFF0000, 0x00FF00, 0x0000FF, 0x800080, 0x00FFFF, 0xFFA500, 0xFFC0CB, 0xFFFFFF)
        presets.forEach { color ->
            val swatch = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 72).apply { weight = 1f }
                setBackgroundColor(color)
                contentDescription = ColorUtils.toHexDisplay(color)
                setOnClickListener { selectColor(color) }
            }
            findViewById<LinearLayout>(R.id.presetRow).addView(swatch)
        }

        // Hex input
        editHex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (suppressWatchers) return
                ColorUtils.parseHexColor(s?.toString() ?: "")?.let { parsed ->
                    config = config.copy(color = parsed)
                    renderColorControls()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        // Brightness slider
        seekBrightness.max = 100
        seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !suppressWatchers) {
                    config = config.copy(brightnessPercent = progress)
                    textBrightnessLabel.text = getString(R.string.brightness_label, progress)
                    schedulePreview()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // Effect spinner
        val names = resources.getStringArray(R.array.effect_names)
        spinnerEffect.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinnerEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (suppressWatchers) return
                config = config.copy(effect = effectNameToConstant(names[pos]))
                renderEffectControls()
                schedulePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Speed slider
        seekSpeed.max = 100
        seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !suppressWatchers) {
                    config = config.copy(rateMs = ColorUtils.sliderToRate(progress))
                    schedulePreview()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // Wave direction
        radioDirection.setOnCheckedChangeListener { _, checkedId ->
            if (!suppressWatchers) {
                config = config.copy(
                    waveDirection = if (checkedId == R.id.radioRight)
                        LightSyncEffects.WAVE_RIGHT else LightSyncEffects.WAVE_LEFT,
                )
                schedulePreview()
            }
        }

        // Zones: three mini pickers cycling a curated palette (simple, no dialogs)
        val zonePalette = intArrayOf(0xFF0000, 0x00FF00, 0x0000FF, 0xFF00FF, 0x00FFFF, 0xFFFF00, 0xFFFFFF, 0x000000)
        config.zoneColors.forEachIndexed { idx, color ->
            val btn = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 72).apply { weight = 1f }
                setBackgroundColor(color)
                tag = idx
                contentDescription = "zone $idx"
                setOnClickListener { self ->
                    val zoneIndex = self.tag as Int
                    val current = config.zoneColors[zoneIndex]
                    val next = zonePalette[(zonePalette.indexOfFirst { it == current } + 1) % zonePalette.size]
                    val updated = config.zoneColors.toMutableList().also { it[zoneIndex] = next }
                    config = config.copy(zoneColors = updated)
                    self.setBackgroundColor(next)
                    schedulePreview()
                }
            }
            zoneRow.addView(btn)
        }

        // Auto-apply
        checkAutoApply.isChecked = store.loadAutoApply()
        checkAutoApply.setOnCheckedChangeListener { _, checked ->
            store.saveAutoApply(checked)
        }

        // Version footnote
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "2.0.0"
        } catch (_: Exception) {
            "2.0.0"
        }
        findViewById<TextView>(R.id.textFootnote).text = getString(R.string.footnote, versionName)
    }

    private fun effectNameToConstant(name: String): String = when (name) {
        "Solid" -> LightingConfig.EFFECT_SOLID
        "Cycle" -> LightingConfig.EFFECT_CYCLE
        "Wave" -> LightingConfig.EFFECT_WAVE
        "Breathe" -> LightingConfig.EFFECT_BREATHE
        "Blend" -> LightingConfig.EFFECT_BLEND
        "Zones" -> LightingConfig.EFFECT_ZONES
        else -> LightingConfig.EFFECT_SOLID
    }

    // --- rendering ----------------------------------------------------------

    private fun renderConfig() {
        suppressWatchers = true
        renderColorControls()
        renderEffectControls()
        textBrightnessLabel.text = getString(R.string.brightness_label, config.brightnessPercent)
        seekBrightness.progress = config.brightnessPercent
        seekSpeed.progress = ColorUtils.rateToSlider(config.rateMs)
        radioDirection.check(if (config.waveDirection == LightSyncEffects.WAVE_RIGHT) R.id.radioRight else R.id.radioLeft)
        val effectPosition = when (config.effect) {
            LightingConfig.EFFECT_SOLID -> 0
            LightingConfig.EFFECT_CYCLE -> 1
            LightingConfig.EFFECT_WAVE -> 2
            LightingConfig.EFFECT_BREATHE -> 3
            LightingConfig.EFFECT_BLEND -> 4
            LightingConfig.EFFECT_ZONES -> 5
            else -> 0
        }
        spinnerEffect.setSelection(effectPosition)
        checkAutoApply.isChecked = store.loadAutoApply()
        suppressWatchers = false
    }

    private fun renderColorControls() {
        colorPreview.setBackgroundColor(config.color)
        val hexText = ColorUtils.toHexDisplay(config.color)
        if (editHex.text.toString() != hexText) {
            suppressWatchers = true
            editHex.setText(hexText)
            editHex.setSelection(hexText.length)
            suppressWatchers = false
        }
    }

    private fun renderEffectControls() {
        val showSpeed = config.effect in setOf(
            LightingConfig.EFFECT_CYCLE, LightingConfig.EFFECT_WAVE,
            LightingConfig.EFFECT_BREATHE, LightingConfig.EFFECT_BLEND,
        )
        val showDirection = config.effect == LightingConfig.EFFECT_WAVE
        val showZones = config.effect == LightingConfig.EFFECT_ZONES
        val showColor = config.effect in setOf(LightingConfig.EFFECT_SOLID, LightingConfig.EFFECT_BREATHE)

        textSpeedLabel.visibility = if (showSpeed) View.VISIBLE else View.GONE
        seekSpeed.visibility = if (showSpeed) View.VISIBLE else View.GONE
        textDirectionLabel.visibility = if (showDirection) View.VISIBLE else View.GONE
        radioDirection.visibility = if (showDirection) View.VISIBLE else View.GONE
        zoneRow.visibility = if (showZones) View.VISIBLE else View.GONE
        textZoneLabel.visibility = if (showZones) View.VISIBLE else View.GONE
        colorPreview.visibility = if (showColor) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.presetRow).visibility = if (showColor) View.VISIBLE else View.GONE
        btnApplyColor.visibility = if (showColor) View.VISIBLE else View.GONE
    }

    private fun selectColor(color: Int) {
        config = config.copy(color = color)
        renderColorControls()
        schedulePreview()
    }

    /** Applies the current on-screen config: persists it, then sends it. */
    private fun applyCurrentConfig() {
        store.save(config)
        sendCommandList(RgbCommandComposer.composeModeSwitch() + RgbCommandComposer.composeEffect(config), "APPLY")
    }

    // --- live preview -------------------------------------------------------

    /** Debounced: fires 120ms after the latest UI change — no USB flooding. */
    private fun schedulePreview() {
        previewRunnable?.let { mainHandler.removeCallbacks(it) }
        previewRunnable = Runnable {
            if (!busy) sendCommandList(composePreviewPackets(), "PREVIEW")
        }.also { mainHandler.postDelayed(it, debounceMs) }
    }

    /** Solid/breathe preview only — effects with cycles would flood on every tweak. */
    private fun composePreviewPackets(): List<ByteArray> = when (config.effect) {
        LightingConfig.EFFECT_SOLID -> listOf(
            ProtocolPackets.buildDisableOnboardMemoryPacket(),
            ProtocolPackets.buildSolidColorPacket(
                ColorUtils.red(ColorUtils.scaleForBrightness(config.color, config.brightnessPercent)),
                ColorUtils.green(ColorUtils.scaleForBrightness(config.color, config.brightnessPercent)),
                ColorUtils.blue(ColorUtils.scaleForBrightness(config.color, config.brightnessPercent)),
            ),
        )
        else -> RgbCommandComposer.composeEffect(config) // cycle/wave/breathe/blend/zones: apply only
    }

    // --- USB command sending ---------------------------------------------------

    private fun sendCommandList(packets: List<ByteArray>, label: String) {
        if (busy) return
        val deviceSnapshot = currentDevice() ?: run {
            textResult.text = getString(R.string.no_device)
            return
        }
        busy = true
        setControlsEnabled(false)
        textResult.text = getString(R.string.status_busy)
        Thread {
            val outcome = runCommandSequence(deviceSnapshot, packets, label)
            runOnUiThread {
                busy = false
                setControlsEnabled(true)
                textResult.text = outcome
                refreshDeviceState()
            }
        }.apply { name = "g102-cmd" }.start()
    }

    private fun runCommandSequence(snapshot: UsbDevice, packets: List<ByteArray>, label: String): String {
        return try {
        val device = usb.findLogitechDevices().firstOrNull {
            it.deviceName == snapshot.deviceName
        } ?: return "$label failed: device disappeared."

        if (!usb.hasPermission(device)) return "$label failed: USB permission not granted."

        val connection = usb.openDevice(device)
            ?: return "$label failed: could not open USB device."

        var claimedInterface: UsbInterface? = null
        try {
            val iface = usb.selectHidppInterface(device)
                ?: return "$label failed: no HID++ interface — copy diagnostics."

            if (!usb.claimInterface(connection, iface)) {
                return "$label failed: could not claim interface — copy diagnostics."
            }
            claimedInterface = iface
            usb.drainStaleResponses(connection, iface)

            var lastWriteCount = -1
            var anyError: String? = null
            for (packet in packets) {
                val result = usb.sendReportAndRead(connection, iface, packet)
                lastWriteCount = result.bytesWritten
                if (result.error != null) {
                    anyError = result.error
                    break
                }
            }
            when {
                anyError != null -> "$label failed: $anyError — copy diagnostics."
                lastWriteCount < 0 -> "$label failed: negative transfer result."
                else -> successTextFor(label)
            }
        } finally {
            usb.close(connection, claimedInterface)
        }
    } catch (t: Throwable) {
        onLog("Sequence error: ${t.javaClass.simpleName}: ${t.message}")
        "$label failed: ${t.javaClass.simpleName}: ${t.message}"
    }
    }

    // honest status text per label — "sent" never claims visual confirmation
    private fun successTextFor(label: String): String = when (label) {
        "RGB OFF" -> "RGB OFF command sent successfully (solid 0,0,0 — BLACK_FALLBACK)."
        "RGB ON" -> "RGB ON command sent successfully (${config.effect.lowercase()} restored)."
        "PREVIEW" -> "Preview sent (${config.effect.lowercase()})."
        else -> "$label command sent successfully (${config.effect.lowercase()})."
    }

    // --- auto-apply ------------------------------------------------------------

    /** One-shot per connection: (re)applying only when the mouse freshly attaches. */
    private var autoAppliedForConnection = false

    private fun maybeAutoApply() {
        if (!store.loadAutoApply()) return
        if (autoAppliedForConnection) return
        val device = currentDevice() ?: return
        if (!usb.hasPermission(device)) return
        if (busy) return
        autoAppliedForConnection = true
        onLog("Auto-apply: applying saved config on (re)connect")
        sendCommandList(RgbCommandComposer.composeRgbOn(config), "RGB ON")
    }

    // --- device state ------------------------------------------------------------

    private fun currentDevice(): UsbDevice? = usb.findLogitechDevices().firstOrNull()

    private fun refreshDeviceState() {
        val device = currentDevice()
        when {
            device == null -> {
                textDevice.text = getString(R.string.no_device)
                btnGrantUsb.visibility = View.GONE
                setControlsEnabled(false)
            }
            !usb.hasPermission(device) -> {
                renderDevice(device)
                btnGrantUsb.visibility = View.VISIBLE
                setControlsEnabled(false)
            }
            else -> {
                renderDevice(device)
                btnGrantUsb.visibility = View.GONE
                setControlsEnabled(!busy)
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

    private fun setControlsEnabled(enabled: Boolean) {
        btnRgbOn.isEnabled = enabled
        btnRgbOff.isEnabled = enabled
        btnApplyColor.isEnabled = enabled
        btnApplyEffect.isEnabled = enabled
    }

    // --- USB events ------------------------------------------------------------

    private fun registerUsbEvents() {
        usb.register { intent ->
            runOnUiThread {
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    UsbManager.ACTION_USB_DEVICE_DETACHED,
                    usb.permissionAction,
                    -> {
                        if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                            autoAppliedForConnection = false // replug re-arms auto-apply
                        }
                        refreshDeviceState()
                        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                            maybeAutoApply()
                        }
                    }
                }
            }
        }
    }

    // --- diagnostics ----------------------------------------------------------

    private fun onLog(line: String) {
        runOnUiThread {
            diagLines += "[%s] %s".format(timeFormat.format(Date()), line)
            renderDiagnostics()
        }
    }

    private fun renderDiagnostics() {
        if (diagnosticsVisible) {
            textDiagnostics.visibility = View.VISIBLE
            textDiagnostics.text = diagLines.joinToString("\n").ifEmpty { getString(R.string.diag_empty) }
        }
    }

    private fun toggleDiagnostics() {
        diagnosticsVisible = !diagnosticsVisible
        btnToggleDiagnostics.text =
            if (diagnosticsVisible) getString(R.string.btn_diagnostics_toggle).replace("▾", "▴")
            else getString(R.string.btn_diagnostics_toggle)
        if (diagnosticsVisible) {
            textDiagnostics.visibility = View.VISIBLE
            textRowEmptyCheck()
        } else {
            textDiagnostics.visibility = View.GONE
        }
    }

    private fun textRowEmptyCheck() {
        textDiagnostics.text = diagLines.joinToString("\n").ifEmpty { getString(R.string.diag_empty) }
    }

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

    private fun diagnosticsReport(): String = diagLines.joinToString("\n")
}
