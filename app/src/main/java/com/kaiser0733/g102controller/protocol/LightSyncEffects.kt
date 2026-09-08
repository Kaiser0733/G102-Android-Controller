package com.kaiser0733.g102controller.protocol

/**
 * LIGHTSYNC effect packets for G102/G203 LIGHTSYNC — v2 layer.
 *
 * Every builder byte-verified against the reference implementation
 * github.com/smasty/g203-led via scripted mirror comparison BEFORE being
 * written (the mirror is ground truth; vectors live in LightSyncEffectsTest).
 *
 * Byte-position law (verified from the reference's formatted output):
 *   All 0x0E-effect long reports: apply flag = byte 16.
 *     solid  : color @ 6..8                              (no native brightness)
 *     cycle  : rate @ 11..12 (BE), brightness @ 13
 *     wave   : rateLo @ 12, direction @ 13, brightness @ 14, rateHi @ 15
 *     breathe: color @ 6..8, rate @ 9..10 (BE), 0x00 @ 11, brightness @ 12
 *     blend  : rateLo @ 12, rateHi @ 13, brightness @ 14
 *   Triple-zone (feature 0x12 — a different feature from lighting 0x0E):
 *     tags @ 4, 8, 12; colors @ 5..7, 9..11, 13..15; must be followed by
 *     the 0x7B apply packet.
 *
 * The v1 RGB OFF path (mode switch + solid black) is UNTOUCHED — pinned by
 * a dedicated regression test that must fail if it ever changes.
 */
object LightSyncEffects {

    // --- Effect IDs (byte 5, from reference set_ls_* headers) ---
    const val EFFECT_SOLID = 0x01
    const val EFFECT_CYCLE = 0x02
    const val EFFECT_WAVE = 0x03
    const val EFFECT_BREATHE = 0x04
    const val EFFECT_BLEND = 0x06

    // --- WAVE direction states (set_ls_wave: right=01, left=06) ---
    const val WAVE_RIGHT = 0x01
    const val WAVE_LEFT = 0x06

    // --- Parameter ranges (process_rate / process_brightness) ---
    const val RATE_MIN_MS = 1000
    const val RATE_MAX_MS = 65535
    const val BRIGHTNESS_MIN = 1   // reference clamps 0 -> 1; 0 not expressible
    const val BRIGHTNESS_MAX = 100

    // --- Zone commands (feature 0x12) ---
    const val ZONE_FEATURE_INDEX = 0x12
    const val ZONE_FUNCTION_SET = 0x1B
    const val ZONE_FUNCTION_APPLY = 0x7B
    const val ZONE_COUNT = 3

    /** Solid color — delegates to the v1 builder (byte-identical, regression-pinned). */
    fun buildSolidPacket(red: Int, green: Int, blue: Int): ByteArray =
        ProtocolPackets.buildSolidColorPacket(red, green, blue)

    /** Color cycle (rainbow). Native rate + brightness. */
    fun buildCyclePacket(rateMs: Int, brightness: Int): ByteArray {
        requireRate(rateMs); requireBrightness(brightness)
        return effectLongPacket(EFFECT_CYCLE) {
            it[11] = (rateMs shr 8).toByte()
            it[12] = rateMs.toByte()
            it[13] = brightness.toByte()
        }
    }

    /** Wave. Direction: WAVE_RIGHT or WAVE_LEFT. Reference order: lo@12 dir@13 bri@14 hi@15. */
    fun buildWavePacket(rateMs: Int, direction: Int, brightness: Int): ByteArray {
        requireRate(rateMs); requireBrightness(brightness)
        require(direction == WAVE_RIGHT || direction == WAVE_LEFT) {
            "direction must be WAVE_RIGHT($WAVE_RIGHT) or WAVE_LEFT($WAVE_LEFT)"
        }
        return effectLongPacket(EFFECT_WAVE) {
            it[12] = (rateMs and 0xFF).toByte()
            it[13] = direction.toByte()
            it[14] = brightness.toByte()
            it[15] = (rateMs shr 8).toByte()
        }
    }

    /** Single-color breathing. */
    fun buildBreathePacket(red: Int, green: Int, blue: Int, rateMs: Int, brightness: Int): ByteArray {
        requireColor(red, green, blue); requireRate(rateMs); requireBrightness(brightness)
        return effectLongPacket(EFFECT_BREATHE) {
            it[6] = red.toByte(); it[7] = green.toByte(); it[8] = blue.toByte()
            it[9] = (rateMs shr 8).toByte(); it[10] = rateMs.toByte()
            it[11] = 0x00
            it[12] = brightness.toByte()
        }
    }

    /** Blend effect. Reference order: rateLo@12 rateHi@13 bri@14. */
    fun buildBlendPacket(rateMs: Int, brightness: Int): ByteArray {
        requireRate(rateMs); requireBrightness(brightness)
        return effectLongPacket(EFFECT_BLEND) {
            it[12] = (rateMs and 0xFF).toByte()
            it[13] = (rateMs shr 8).toByte()
            it[14] = brightness.toByte()
        }
    }

    /**
     * Triple-zone colors, e.g. left=red, middle=green, right=blue.
     * Returns [zoneSetPacket, zoneApplyPacket] — apply MUST be sent right
     * after set, exactly like the reference.
     */
    fun buildTripleZonePackets(zone1: Int, zone2: Int, zone3: Int): List<ByteArray> {
        fun unpack(color: Int): IntArray = intArrayOf(
            (color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF,
        )
        fun check(c: Int) = require(c in 0..0xFFFFFF) { "zone color must be 0x000000..0xFFFFFF" }
        check(zone1); check(zone2); check(zone3)

        val set = ByteArray(ProtocolPackets.LONG_REPORT_SIZE).also { p ->
            p[0] = ProtocolPackets.HIDPP_LONG_REPORT_ID.toByte()
            p[1] = ProtocolPackets.DEVICE_INDEX_WIRED.toByte()
            p[2] = ZONE_FEATURE_INDEX.toByte()          // 0x12, not 0x0E
            p[3] = ZONE_FUNCTION_SET.toByte()
            val (r1, g1, b1) = unpack(zone1)
            p[4] = 0x01                                   // zone tag 1
            p[5] = r1.toByte(); p[6] = g1.toByte(); p[7] = b1.toByte()
            val (r2, g2, b2) = unpack(zone2)
            p[8] = 0x02                                   // zone tag 2
            p[9] = r2.toByte(); p[10] = g2.toByte(); p[11] = b2.toByte()
            val (r3, g3, b3) = unpack(zone3)
            p[12] = 0x03                                  // zone tag 3
            p[13] = r3.toByte(); p[14] = g3.toByte(); p[15] = b3.toByte()
            // bytes 16..19 zero
        }

        val apply = ByteArray(ProtocolPackets.LONG_REPORT_SIZE).also { p ->
            p[0] = ProtocolPackets.HIDPP_LONG_REPORT_ID.toByte()
            p[1] = ProtocolPackets.DEVICE_INDEX_WIRED.toByte()
            p[2] = ZONE_FEATURE_INDEX.toByte()
            p[3] = ZONE_FUNCTION_APPLY.toByte()           // 0x7B
            // bytes 4..19 zero
        }
        return listOf(set, apply)
    }

    /** True when [effectId] carries a native brightness byte (solid does not). */
    fun hasNativeBrightness(effectId: Int): Boolean = effectId != EFFECT_SOLID

    // --- helpers -------------------------------------------------------------

    /** 20-byte long report with the fixed 0x0E effect header; apply flag at byte 16. */
    private inline fun effectLongPacket(effectId: Int, fill: (ByteArray) -> Unit): ByteArray {
        val p = ByteArray(ProtocolPackets.LONG_REPORT_SIZE)
        p[0] = ProtocolPackets.HIDPP_LONG_REPORT_ID.toByte()
        p[1] = ProtocolPackets.DEVICE_INDEX_WIRED.toByte()
        p[2] = ProtocolPackets.FEATURE_INDEX_RGB_CONTROL.toByte()
        p[3] = ProtocolPackets.FUNCTION_SET_COLOR_EFFECT.toByte()
        p[4] = 0x00
        p[5] = effectId.toByte()
        fill(p)
        p[16] = ProtocolPackets.APPLY_FLAG.toByte()
        return p
    }

    private fun requireRate(rateMs: Int) =
        require(rateMs in RATE_MIN_MS..RATE_MAX_MS) { "rate must be $RATE_MIN_MS..$RATE_MAX_MS ms" }

    private fun requireBrightness(brightness: Int) =
        require(brightness in BRIGHTNESS_MIN..BRIGHTNESS_MAX) { "brightness must be $BRIGHTNESS_MIN..$BRIGHTNESS_MAX" }

    private fun requireColor(red: Int, green: Int, blue: Int) =
        require(red in 0..255 && green in 0..255 && blue in 0..255) {
            "RGB channels must be 0..255, got r=$red g=$green b=$blue"
        }
}
