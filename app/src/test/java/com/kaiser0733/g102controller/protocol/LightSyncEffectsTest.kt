package com.kaiser0733.g102controller.protocol

import com.kaiser0733.g102controller.controller.RgbCommandComposer
import com.kaiser0733.g102controller.settings.LightingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reference vectors below were produced by the reference implementation's own
 * templates (g203-led.py) and verified by script before this file was written.
 * They are ground truth from real working hardware behavior — if these tests
 * fail after a change, the change broke the protocol.
 */
class LightSyncEffectsTest {

    private fun hex(p: ByteArray) = ProtocolPackets.toHex(p)

    // ===== RGB OFF regression vector (verified on hardware) =====

    @Test fun rgbOffSequence_matchesKnownGoodVector() {
        // v1 sequence: mode switch 10ff0e5b010305 + solid black
        // 11ff0e1b00010000000000000000000001000000 (apply flag at byte 16).
        val packets = RgbCommandComposer.composeRgbOff()
        assertEquals(2, packets.size)
        assertEquals("10ff0e5b010305", hex(packets[0]))
        assertEquals("11ff0e1b00010000000000000000000001000000", hex(packets[1]))
        // Additional structural pins: lengths and report IDs
        assertEquals(7, packets[0].size)
        assertEquals(20, packets[1].size)
        assertEquals(0x11, ProtocolPackets.u8(packets[1][0]))
        assertEquals(0x10, ProtocolPackets.u8(packets[0][0]))
    }

    // ===== SOLID =====

    @Test fun solidPacket_matchesReferenceVector() {
        // set_ls_solid('b76e79'): 11ff0e1b0001b76e790000000000000001000000
        assertEquals(
            "11ff0e1b0001b76e790000000000000001000000",
            hex(LightSyncEffects.buildSolidPacket(0xB7, 0x6E, 0x79)),
        )
    }

    @Test fun solidPacket_boundaries() {
        // #000000 (hardware OFF) and #FFFFFF (white)
        assertEquals(
            "11ff0e1b00010000000000000000000001000000",
            hex(LightSyncEffects.buildSolidPacket(0, 0, 0)),
        )
        assertEquals(
            "11ff0e1b0001ffffff0000000000000001000000",
            hex(LightSyncEffects.buildSolidPacket(0xFF, 0xFF, 0xFF)),
        )
    }

    // ===== CYCLE =====

    @Test fun cyclePacket_matchesReferenceVector() {
        // set_ls_cycle(10000, 100): 11ff0e1b00020000000000271064000001000000
        assertEquals(
            "11ff0e1b00020000000000271064000001000000",
            hex(LightSyncEffects.buildCyclePacket(10000, 100)),
        )
    }

    @Test fun cyclePacket_rateBoundaries() {
        // min rate 1000ms = 0x03E8, max 65535 = 0xFFFF
        assertEquals(
            "11ff0e1b0002000000000003e864000001000000",
            hex(LightSyncEffects.buildCyclePacket(1000, 100)),
        )
        assertEquals(
            "11ff0e1b00020000000000ffff0a000001000000",
            hex(LightSyncEffects.buildCyclePacket(65535, 10)),
        )
    }

    @Test fun cyclePacket_rejectsOutOfRange() {
        assertThrows { LightSyncEffects.buildCyclePacket(999, 100) }
        assertThrows { LightSyncEffects.buildCyclePacket(65536, 100) }
        assertThrows { LightSyncEffects.buildCyclePacket(1000, 0) }
        assertThrows { LightSyncEffects.buildCyclePacket(1000, 101) }
    }

    // ===== WAVE =====

    @Test fun wavePacket_matchesReferenceVector_right() {
        // set_ls_wave(10000, 100, right): 11ff0e1b00030000000000001001642701000000
        // rate 10000 = 0x2710 -> lo@12=10 hi@15=27; dir@13=01; bri@14=64
        assertEquals(
            "11ff0e1b00030000000000001001642701000000",
            hex(LightSyncEffects.buildWavePacket(10000, LightSyncEffects.WAVE_RIGHT, 100)),
        )
    }

    @Test fun wavePacket_matchesReferenceVector_left() {
        // set_ls_wave(5000, 50, left): rate=0x1388 lo=88 hi=13; dir=06; bri=32
        assertEquals(
            "11ff0e1b00030000000000008806321301000000",
            hex(LightSyncEffects.buildWavePacket(5000, LightSyncEffects.WAVE_LEFT, 50)),
        )
    }

    @Test fun wavePacket_rejectsBadDirection() {
        assertThrows { LightSyncEffects.buildWavePacket(1000, 0x02, 100) }
        assertThrows { LightSyncEffects.buildWavePacket(1000, 0x99, 100) }
    }

    // ===== BREATHE =====

    @Test fun breathePacket_matchesReferenceVector() {
        // set_ls_breathe('b76e79', 10000, 100):
        // 11ff0e1b0004b76e792710006400000001000000
        assertEquals(
            "11ff0e1b0004b76e792710006400000001000000",
            hex(LightSyncEffects.buildBreathePacket(0xB7, 0x6E, 0x79, 10000, 100)),
        )
    }

    @Test fun breathePacket_rejectsBadColor() {
        assertThrows { LightSyncEffects.buildBreathePacket(256, 0, 0, 1000, 50) }
        assertThrows { LightSyncEffects.buildBreathePacket(0, -1, 0, 1000, 50) }
    }

    // ===== BLEND =====

    @Test fun blendPacket_matchesReferenceVector() {
        // set_ls_blend(10000, 100): 11ff0e1b00060000000000001027640001000000
        assertEquals(
            "11ff0e1b00060000000000001027640001000000",
            hex(LightSyncEffects.buildBlendPacket(10000, 100)),
        )
    }

    // ===== ZONES =====

    @Test fun tripleZonePackets_matchReferenceVectors() {
        // set_ls_triple('ff0000','00ff00','0000ff'):
        //   set:   11ff121b01ff00000200ff00030000ff00000000
        //   apply: 11ff127b00000000000000000000000000000000
        val packets = LightSyncEffects.buildTripleZonePackets(0xFF0000, 0x00FF00, 0x0000FF)
        assertEquals(2, packets.size)
        assertEquals("11ff121b01ff00000200ff00030000ff00000000", hex(packets[0]))
        assertEquals("11ff127b00000000000000000000000000000000", hex(packets[1]))
    }

    @Test fun tripleZonePackets_rejectOutOfRange() {
        assertThrows { LightSyncEffects.buildTripleZonePackets(0x1000000, 0, 0) }
        assertThrows { LightSyncEffects.buildTripleZonePackets(0, -1, 0) }
    }

    // ===== native brightness =====

    @Test fun solid_hasNoNativeBrightness_othersDo() {
        assertTrue(!LightSyncEffects.hasNativeBrightness(LightSyncEffects.EFFECT_SOLID))
        assertTrue(LightSyncEffects.hasNativeBrightness(LightSyncEffects.EFFECT_CYCLE))
        assertTrue(LightSyncEffects.hasNativeBrightness(LightSyncEffects.EFFECT_WAVE))
        assertTrue(LightSyncEffects.hasNativeBrightness(LightSyncEffects.EFFECT_BREATHE))
        assertTrue(LightSyncEffects.hasNativeBrightness(LightSyncEffects.EFFECT_BLEND))
    }

    // ===== COMPOSER: RGB ON restores saved config =====

    @Test fun rgbOn_restoresLastSavedSolid() {
        val config = LightingConfig(effect = LightingConfig.EFFECT_SOLID, color = 0xB76E79)
        val packets = RgbCommandComposer.composeRgbOn(config)
        assertEquals(2, packets.size)
        assertEquals("10ff0e5b010305", hex(packets[0])) // mode switch first, as always
        assertEquals("11ff0e1b0001b76e790000000000000001000000", hex(packets[1]))
    }

    @Test fun rgbOn_unknownEffect_fallsBackToSafeWhite() {
        val config = LightingConfig(effect = "GARBAGE")
        val packets = RgbCommandComposer.composeRgbOn(config)
        assertEquals("11ff0e1b0001ffffff0000000000000001000000", hex(packets[1]))
    }

    @Test fun composeEffect_solidAppliesSoftwareBrightness() {
        // 50% of #FFFFFF = #808080 (255*50/100 = 127 = 0x7F — integer division!)
        val config = LightingConfig(effect = LightingConfig.EFFECT_SOLID, color = 0xFFFFFF, brightnessPercent = 50)
        val packets = RgbCommandComposer.composeEffect(config)
        assertEquals(1, packets.size)
        // 255*50/100 = 127 (0x7F) per channel — integer math, documented behavior
        assertEquals("11ff0e1b00017f7f7f0000000000000001000000", hex(packets[0]))
    }

    // ===== hex parser =====

    @Test fun hexParser_validInputs() {
        assertEquals(0xB76E79, ColorUtils.parseHexColor("#B76E79")!!)
        assertEquals(0xB76E79, ColorUtils.parseHexColor("b76e79")!!)
        assertEquals(0xB76E79, ColorUtils.parseHexColor("#b76e79")!!)
        assertEquals(0x000000, ColorUtils.parseHexColor("#000000")!!)
        assertEquals(0xFFFFFF, ColorUtils.parseHexColor("#FFFFFF")!!)
        assertEquals(0xFF0000, ColorUtils.parseHexColor("#FF0000")!!)
    }

    @Test fun hexParser_invalidInputs() {
        assertNull(ColorUtils.parseHexColor("#12345"))     // too short
        assertNull(ColorUtils.parseHexColor("#1234567"))  // too long
        assertNull(ColorUtils.parseHexColor("#12345G"))  // non-hex char
        assertNull(ColorUtils.parseHexColor(""))          // empty
        assertNull(ColorUtils.parseHexColor("#GGGGGG"))  // garbage
    }

    @Test fun brightnessScaling_law() {
        // 100% identity, 0% black, 50% of 255 = 127 (integer floor)
        assertEquals(0xFFFFFF, ColorUtils.scaleForBrightness(0xFFFFFF, 100))
        assertEquals(0x000000, ColorUtils.scaleForBrightness(0xFFFFFF, 0))
        assertEquals(0x7F7F7F, ColorUtils.scaleForBrightness(0xFFFFFF, 50))
        // 0..100 clamping outside values
        assertEquals(0x000000, ColorUtils.scaleForBrightness(0xFFFFFF, -5))
        assertEquals(0xFFFFFF, ColorUtils.scaleForBrightness(0xFFFFFF, 200))
    }

    @Test fun rateSlider_roundTrip() {
        // slider 0..100 <-> rate 65535..1000 (inverted: high slider = fast = low ms)
        assertEquals(100, ColorUtils.rateToSlider(1000))
        assertEquals(0, ColorUtils.rateToSlider(65535))
        assertEquals(1000, ColorUtils.sliderToRate(100))
        assertEquals(65535, ColorUtils.sliderToRate(0))
        // round trip keeps value within quantization error
        (0..100 step 10).forEach { s ->
            val r = ColorUtils.sliderToRate(s)
            assertTrue("rate out of range for slider $s: $r", r in 1000..65535)
        }
    }

    // ===== settings round trip =====

    @Test fun lightingConfig_serializationRoundTrip() {
        val original = LightingConfig(
            effect = LightingConfig.EFFECT_BREATHE,
            color = 0xB76E79,
            brightnessPercent = 50,
            rateMs = 5000,
            waveDirection = 6,
            zoneColors = listOf(0xFF0000, 0x00FF00, 0x0000FF),
        )
        val restored = LightingConfig.deserialize(original.serialize())
        assertEquals(original, restored)
    }

    @Test fun lightingConfig_rejectsGarbage() {
        assertNull(LightingConfig.deserialize(null))
        assertNull(LightingConfig.deserialize(""))
        assertNull(LightingConfig.deserialize("garbage"))
        assertNull(LightingConfig.deserialize("SOLID|b76e79")) // missing fields
    }

    @Test fun lightingConfig_defaultIsSafe() {
        val def = LightingConfig()
        assertEquals(LightingConfig.EFFECT_SOLID, def.effect)
        assertEquals(0xFFFFFF, def.color)
        assertEquals(100, def.brightnessPercent)
    }

    // helpers ---------------------------------------------------------------

    private fun assertThrows(block: () -> Unit) {
        try {
            block()
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
