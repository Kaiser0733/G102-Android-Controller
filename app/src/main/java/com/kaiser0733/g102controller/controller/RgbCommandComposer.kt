package com.kaiser0733.g102controller.controller

import com.kaiser0733.g102controller.protocol.ColorUtils
import com.kaiser0733.g102controller.protocol.LightSyncEffects
import com.kaiser0733.g102controller.protocol.ProtocolPackets
import com.kaiser0733.g102controller.settings.LightingConfig

/**
 * Translates a LightingConfig into the exact HID++ packets to send —
 * pure logic, no Android imports, JVM-testable end to end.
 *
 * RGB OFF  = mode switch + solid(0,0,0)              [v1 regression-pinned]
 * RGB ON   = mode switch + last-config effect packet [restores saved lighting]
 */
object RgbCommandComposer {

    /** RGB-off command sequence; byte-for-byte covered by regression tests. */
    fun composeRgbOff(): List<ByteArray> = listOf(
        ProtocolPackets.buildDisableOnboardMemoryPacket(),
        ProtocolPackets.buildRgbOffPacket(),
    )

    /**
     * ON = mode switch + the config's effect (any effect re-enables lighting).
     * Solid configs with brightness 0 would send black (an accidental re-OFF),
     * so ON forces a visible floor: brightness >= 1 — and if the scaled color
     * lands on pure black, falls back to white so ON always illuminates.
     */
    fun composeRgbOn(config: LightingConfig): List<ByteArray> {
        val safeConfig = if (config.effect == LightingConfig.EFFECT_SOLID) {
            val floored = config.copy(brightnessPercent = config.brightnessPercent.coerceAtLeast(1))
            val scaled = ColorUtils.scaleForBrightness(floored.color, floored.brightnessPercent)
            if (scaled == 0x000000) floored.copy(color = 0xFFFFFF, brightnessPercent = 100)
            else floored
        } else {
            config.copy(brightnessPercent = config.brightnessPercent.coerceAtLeast(1))
        }
        return composeModeSwitch() + composeEffect(safeConfig)
    }

    /** Config -> effect packets. Solid brightness is software-scaled (no native byte). */
    fun composeEffect(config: LightingConfig): List<ByteArray> = when (config.effect) {
        LightingConfig.EFFECT_SOLID -> listOf(
            ProtocolPackets.buildSolidColorPacket(
                ColorUtils.red(ColorUtils.scaleForBrightness(config.color, config.brightnessPercent)),
                ColorUtils.green(ColorUtils.scaleForBrightness(config.color, config.brightnessPercent)),
                ColorUtils.blue(ColorUtils.scaleForBrightness(config.color, config.brightnessPercent)),
            ),
        )
        LightingConfig.EFFECT_CYCLE -> listOf(
            LightSyncEffects.buildCyclePacket(config.rateMs, config.brightnessPercent.coerceIn(1, 100)),
        )
        LightingConfig.EFFECT_WAVE -> listOf(
            LightSyncEffects.buildWavePacket(config.rateMs, config.waveDirection, config.brightnessPercent.coerceIn(1, 100)),
        )
        LightingConfig.EFFECT_BREATHE -> listOf(
            LightSyncEffects.buildBreathePacket(
                ColorUtils.red(config.color), ColorUtils.green(config.color), ColorUtils.blue(config.color),
                config.rateMs, config.brightnessPercent.coerceIn(1, 100),
            ),
        )
        LightingConfig.EFFECT_BLEND -> listOf(
            LightSyncEffects.buildBlendPacket(config.rateMs, config.brightnessPercent.coerceIn(1, 100)),
        )
        LightingConfig.EFFECT_ZONES -> LightSyncEffects.buildTripleZonePackets(
            config.zoneColors.getOrElse(0) { 0 },
            config.zoneColors.getOrElse(1) { 0 },
            config.zoneColors.getOrElse(2) { 0 },
        )
        else -> listOf(ProtocolPackets.buildSolidColorPacket(0xFF, 0xFF, 0xFF)) // unknown -> safe white
    }

    fun composeModeSwitch(): List<ByteArray> = listOf(
        ProtocolPackets.buildDisableOnboardMemoryPacket(),
    )
}
