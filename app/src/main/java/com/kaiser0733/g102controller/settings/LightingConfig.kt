package com.kaiser0733.g102controller.settings

/**
 * One saved lighting configuration — app-side persistence only
 * (SharedPreferences), NEVER written to mouse EEPROM/onboard memory.
 *
 * Serialized as a compact string: effect|color|brightness|rate|direction|zones
 * Example: "SOLID|#B76E79|100|10000|1|B76E79,00FF00,0000FF"
 */
data class LightingConfig(
    val effect: String = EFFECT_SOLID,
    val color: Int = 0xFFFFFF,
    val brightnessPercent: Int = 100,
    val rateMs: Int = 10000,
    val waveDirection: Int = 1, // 1=right, 6=left (protocol states)
    val zoneColors: List<Int> = listOf(0xFF0000, 0x00FF00, 0x0000FF),
) {
    companion object {
        const val EFFECT_SOLID = "SOLID"
        const val EFFECT_CYCLE = "CYCLE"
        const val EFFECT_WAVE = "WAVE"
        const val EFFECT_BREATHE = "BREATHE"
        const val EFFECT_BLEND = "BLEND"
        const val EFFECT_ZONES = "ZONES"

        val ALL_EFFECTS = listOf(EFFECT_SOLID, EFFECT_CYCLE, EFFECT_WAVE, EFFECT_BREATHE, EFFECT_BLEND, EFFECT_ZONES)

        private const val SEPARATOR = "|"

        fun deserialize(raw: String?): LightingConfig? {
            if (raw == null) return null
            val parts = raw.split(SEPARATOR)
            if (parts.size != 6) return null
            return try {
                val effect = parts[0]
                val color = parts[1].removePrefix("#").toInt(16)
                val brightness = parts[2].toInt()
                val rate = parts[3].toInt()
                val direction = parts[4].toInt()
                val zones = parts[5].split(",").map { it.toInt(16) }
                val wellFormed = effect in ALL_EFFECTS &&
                    color in 0..0xFFFFFF &&
                    brightness in 0..100 &&
                    rate in com.kaiser0733.g102controller.protocol.LightSyncEffects.RATE_MIN_MS..
                        com.kaiser0733.g102controller.protocol.LightSyncEffects.RATE_MAX_MS &&
                    (direction == 1 || direction == 6) &&
                    zones.size == 3 && zones.all { it in 0..0xFFFFFF }
                if (!wellFormed) return null
                LightingConfig(
                    effect = effect,
                    color = color,
                    brightnessPercent = brightness,
                    rateMs = rate,
                    waveDirection = direction,
                    zoneColors = zones,
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    fun serialize(): String = buildString {
        append(effect)
        append(SEPARATOR)
        append("%06x".format(color))
        append(SEPARATOR)
        append(brightnessPercent)
        append(SEPARATOR)
        append(rateMs)
        append(SEPARATOR)
        append(waveDirection)
        append(SEPARATOR)
        append(zoneColors.joinToString(",") { "%06x".format(it) })
    }
}
