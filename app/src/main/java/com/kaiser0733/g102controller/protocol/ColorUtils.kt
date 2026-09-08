package com.kaiser0733.g102controller.protocol

/**
 * Color parsing + software brightness scaling — pure, JVM-testable.
 *
 * Hex format: #RRGGBB or RRGGBB (case-insensitive). Never throws for
 * user input paths; [parseHexColor] returns null on malformed input.
 */
object ColorUtils {

    /** Parses "#B76E79", "b76e79", "#b76e79" — null when malformed. */
    fun parseHexColor(input: String): Int? {
        val s = input.trim().removePrefix("#")
        if (s.length != 6) return null
        for (ch in s) {
            if (!ch.isDigit() && ch.lowercaseChar() !in 'a'..'f') return null
        }
        return s.toInt(16).let { if (it in 0..0xFFFFFF) it else null }
    }

    /** "#b76e79" lowercase — the display format. */
    fun toHexDisplay(color: Int): String = "#%06x".format(color)

    fun red(color: Int): Int = (color shr 16) and 0xFF
    fun green(color: Int): Int = (color shr 8) and 0xFF
    fun blue(color: Int): Int = color and 0xFF

    fun fromRgb(r: Int, g: Int, b: Int): Int =
        ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

    /**
     * Software brightness: scales RGB channels by brightness percent.
     * 100% -> identity; 0% -> black (which the solid command renders as off).
     * This is documented RGB scaling — solid has no native hardware brightness.
     */
    fun scaleForBrightness(color: Int, brightnessPercent: Int): Int {
        val b = brightnessPercent.coerceIn(0, 100)
        return fromRgb(
            red(color) * b / 100,
            green(color) * b / 100,
            blue(color) * b / 100,
        )
    }

    /** Milliseconds per wave/breathe/blend cycle -> 0..100 slider position (log-ish). */
    fun rateToSlider(rateMs: Int): Int {
        val clamped = rateMs.coerceIn(LightSyncEffects.RATE_MIN_MS, LightSyncEffects.RATE_MAX_MS)
        // Map 1000..65535 onto 0..100 inverted (faster = higher).
        return 100 - ((clamped - 1000) * 100) / (65535 - 1000)
    }

    /** 0..100 slider position -> milliseconds (inverse of [rateToSlider]). */
    fun sliderToRate(slider: Int): Int {
        val s = slider.coerceIn(0, 100)
        return LightSyncEffects.RATE_MIN_MS +
            ((100 - s) * (LightSyncEffects.RATE_MAX_MS - LightSyncEffects.RATE_MIN_MS)) / 100
    }
}
