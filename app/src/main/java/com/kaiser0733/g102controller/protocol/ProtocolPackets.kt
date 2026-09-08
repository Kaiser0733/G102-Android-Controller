package com.kaiser0733.g102controller.protocol

/**
 * Pure LIGHTSYNC packet construction — no Android imports, fully unit-testable on the JVM.
 *
 * Every constant here was derived from the MIT-licensed reference implementation
 * github.com/smasty/g203-led (TheAquaSheep's LightSync support) and independently
 * byte-verified against its formatted output:
 *   mode switch (short report): 10 ff 0e 5b 01 03 05
 *   solid color  (long report):  11 ff 0e 1b 00 01 RR GG BB 00*9 01 00 00
 * See PROTOCOL.md for the full byte map and provenance.
 *
 * Transport semantics (mirrors the reference, which uses libusb control transfers):
 *   bmRequestType 0x21 — host-to-device, class, interface recipient
 *   bRequest      0x09 — HID SET_REPORT
 *   wValue        0x0210/0x0211 — (report type OUTPUT << 8) | report ID
 *   wIndex        0x01 — the HID++ vendor interface (interface 1 on G102/G203)
 * A 7-byte short report goes out as report ID 0x10 (wValue 0x0210);
 * a 20-byte long report as report ID 0x11 (wValue 0x0211).
 */
object ProtocolPackets {

    // --- HID++ report IDs -------------------------------------------------------
    const val HIDPP_SHORT_REPORT_ID = 0x10
    const val HIDPP_LONG_REPORT_ID = 0x11

    // --- HID++ addressing --------------------------------------------------------
    /** 0xFF = the wired mouse itself (HID++ "all devices" index for directly-attached). */
    const val DEVICE_INDEX_WIRED = 0xFF
    /** Fixed HID++ 1.0 feature index 0x0E the LIGHTSYNC firmware watches on this hardware
     *  family — NOT a HID++ 2.0 feature ID; no ROOT_GET_FEATURE discovery applies here. */
    const val FEATURE_INDEX_RGB_CONTROL = 0x0E

    // --- LIGHTSYNC functions on feature 0x0E -----------------------------------
    /** 0x1B = "set color effect" (solid color variant). */
    const val FUNCTION_SET_COLOR_EFFECT = 0x1B
    /** 0x5B = device-mode command (used for both the LightSync memory switch and intro effect). */
    const val FUNCTION_DEVICE_MODE = 0x5B

    // --- Fixed payload constants (verified against reference output) ------------
    /** Effect variant for a solid static color. */
    const val EFFECT_SOLID_COLOR = 0x01
    /** Byte the reference sends as the trailing "apply" flag on color-effect packets. */
    const val APPLY_FLAG = 0x01
    /** Payload of the LightSync onboard-memory disabling command (short report). */
    const val DEVICE_MODE_DISABLE_ONBOARD_MEMORY = 0x01

    const val LONG_REPORT_SIZE = 20
    const val SHORT_REPORT_SIZE = 7

    /** HID SET_REPORT wValue for the short report: OUTPUT (2) << 8 | report ID 0x10. */
    const val WVALUE_SHORT_REPORT = 0x0210
    /** HID SET_REPORT wValue for the long report: OUTPUT (2) << 8 | report ID 0x11. */
    const val WVALUE_LONG_REPORT = 0x0211

    // --- Packet builders (pure) --------------------------------------------------

    /**
     * The mode-switch short report the reference always sends before a color-effect
     * command: disables LightSync onboard-memory application so the runtime color
     * sticks for this power cycle only. Reversible; touches no persistent memory.
     *
     * Verified vector: 10 FF 0E 5B 01 03 05
     */
    fun buildDisableOnboardMemoryPacket(): ByteArray = byteArrayOf(
        HIDPP_SHORT_REPORT_ID.toByte(),
        DEVICE_INDEX_WIRED.toByte(),
        FEATURE_INDEX_RGB_CONTROL.toByte(),
        FUNCTION_DEVICE_MODE.toByte(),
        DEVICE_MODE_DISABLE_ONBOARD_MEMORY.toByte(),
        0x03, 0x05,
    )

    /**
     * Solid static color on the single logical LIGHTSYNC zone. Setting 00 00 00 is
     * the accepted "off" representation in the reference implementation — the LEDs
     * are commanded to emit nothing. Labeled BLACK_FALLBACK in the UI, not TRUE_OFF,
     * because no distinct LED-disable effect is known for this hardware family.
     *
     * Verified vector (black): 11 FF 0E 1B 00 01 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00
     */
    fun buildSolidColorPacket(red: Int, green: Int, blue: Int): ByteArray {
        require(red in 0..255 && green in 0..255 && blue in 0..255) {
            "RGB channels must be 0..255, got r=$red g=$green b=$blue"
        }
        return ByteArray(LONG_REPORT_SIZE).also { p ->
            p[0] = HIDPP_LONG_REPORT_ID.toByte()
            p[1] = DEVICE_INDEX_WIRED.toByte()
            p[2] = FEATURE_INDEX_RGB_CONTROL.toByte()
            p[3] = FUNCTION_SET_COLOR_EFFECT.toByte()
            p[4] = 0x00
            p[5] = EFFECT_SOLID_COLOR.toByte()
            p[6] = red.toByte()
            p[7] = green.toByte()
            p[8] = blue.toByte()
            // bytes 9..16 stay zero
            p[17] = APPLY_FLAG.toByte()
            // bytes 18..19 stay zero
        }
    }

    /** Convenience: the v1 headline feature. Solid black = LEDs emit nothing. */
    fun buildRgbOffPacket(): ByteArray = buildSolidColorPacket(0, 0, 0)

    /**
     * True if `bytes` looks like an HID++ response to one of our short-report
     * commands: report ID 0x10, matching device index, feature 0x0E.
     */
    fun isShortReportResponse(bytes: ByteArray): Boolean =
        bytes.size >= SHORT_REPORT_SIZE &&
            u8(bytes[0]) == HIDPP_SHORT_REPORT_ID &&
            u8(bytes[1]) == DEVICE_INDEX_WIRED &&
            u8(bytes[2]) == FEATURE_INDEX_RGB_CONTROL

    /**
     * True if `bytes` looks like an HID++ response to our long-report color
     * command: report ID 0x11, matching device index, feature 0x0E.
     */
    fun isLongReportResponse(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            u8(bytes[0]) == HIDPP_LONG_REPORT_ID &&
            u8(bytes[1]) == DEVICE_INDEX_WIRED &&
            u8(bytes[2]) == FEATURE_INDEX_RGB_CONTROL

    /** Kotlin Byte is signed; HID++ payloads are unsigned. Normalize for checks/logging. */
    fun u8(b: Byte): Int = b.toInt() and 0xFF

    /** Lowercase hex with guaranteed two digits per byte — the diagnostics format. */
    fun toHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(u8(it)) }

    /**
     * HID SET_REPORT wValue for a report of the given size, or null if the size is
     * not a valid HID++ report length (7 = short report ID 0x10, 20 = long 0x11).
     */
    fun wValueForReportSize(size: Int): Int? = when (size) {
        SHORT_REPORT_SIZE -> WVALUE_SHORT_REPORT
        LONG_REPORT_SIZE -> WVALUE_LONG_REPORT
        else -> null
    }
}
