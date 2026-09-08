package com.kaiser0733.g102controller.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * All vectors in this file were extracted mechanically from the MIT-licensed
 * reference implementation github.com/smasty/g203-led (see PROTOCOL.md). They
 * are what real G203 LIGHTSYNC hardware accepts — not values restated from
 * the implementation under test.
 */
class ProtocolPacketsTest {

    // --- Known-good vectors (reference-formatted) ----------------------------

    private val SOLID_BLACK_HEX =
        "11ff0e1b00010000000000000000000001000000"
    private val SOLID_RED_HEX =
        "11ff0e1b0001ff00000000000000000001000000"
    private val MODE_SWITCH_HEX =
        "10ff0e5b010305"

    @Test fun rgbOffPacket_matchesReferenceVector() {
        assertEquals(SOLID_BLACK_HEX, ProtocolPackets.toHex(ProtocolPackets.buildRgbOffPacket()))
    }

    @Test fun solidPacket_matchesReferenceVector() {
        assertEquals(SOLID_RED_HEX, ProtocolPackets.toHex(ProtocolPackets.buildSolidColorPacket(0xFF, 0x00, 0x00)))
    }

    @Test fun modeSwitchPacket_matchesReferenceVector() {
        assertEquals(MODE_SWITCH_HEX, ProtocolPackets.toHex(ProtocolPackets.buildDisableOnboardMemoryPacket()))
    }

    @Test fun reportSizes_matchReferenceWValueLogic() {
        assertEquals(7, ProtocolPackets.buildDisableOnboardMemoryPacket().size)
        assertEquals(20, ProtocolPackets.buildRgbOffPacket().size)
    }

    // --- Byte-level structure ----------------------------------------------------

    @Test fun rgbOffPacket_bytePositions() {
        val p = ProtocolPackets.buildRgbOffPacket()
        assertEquals(0x11, ProtocolPackets.u8(p[0]))   // long report ID
        assertEquals(0xFF, ProtocolPackets.u8(p[1]))   // wired device index
        assertEquals(0x0E, ProtocolPackets.u8(p[2]))   // feature index
        assertEquals(0x1B, ProtocolPackets.u8(p[3]))  // set-color-effect function
        assertEquals(0x01, ProtocolPackets.u8(p[5]))  // solid-color effect variant
        assertEquals(0x01, ProtocolPackets.u8(p[17])) // apply flag
    }

    @Test fun solidPacket_rgbPayloadAtOffsets6to8() {
        // Verified placement: the color lives at bytes 6-8 of the long report.
        val p = ProtocolPackets.buildSolidColorPacket(0x12, 0x34, 0x56)
        assertEquals(0x12, ProtocolPackets.u8(p[6]))
        assertEquals(0x34, ProtocolPackets.u8(p[7]))
        assertEquals(0x56, ProtocolPackets.u8(p[8]))
    }

    @Test fun solidPacket_rejectsOutOfRangeColors() {
        assertThrows { ProtocolPackets.buildSolidColorPacket(256, 0, 0) }
        assertThrows { ProtocolPackets.buildSolidColorPacket(-1, 0, 0) }
        assertThrows { ProtocolPackets.buildSolidColorPacket(0, 999, 0) }
        assertThrows { ProtocolPackets.buildSolidColorPacket(0, 0, 256) }
    }

    @Test fun shortReportStructure() {
        val p = ProtocolPackets.buildDisableOnboardMemoryPacket()
        assertEquals(0x10, ProtocolPackets.u8(p[0]))   // short report ID
        assertEquals(0xFF, ProtocolPackets.u8(p[1]))
        assertEquals(0x0E, ProtocolPackets.u8(p[2]))
        assertEquals(0x5B, ProtocolPackets.u8(p[3]))   // device-mode function
        assertEquals(0x01, ProtocolPackets.u8(p[4]))   // disable onboard-memory mode
        assertEquals(0x03, ProtocolPackets.u8(p[5]))
        assertEquals(0x05, ProtocolPackets.u8(p[6]))
    }

    // --- Signed Byte handling -------------------------------------------------------

    @Test fun u8_handlesSignedByteAbove127() {
        // 0xFF as a signed Kotlin Byte is -1; u8 must normalize it back to 255.
        assertEquals(255, ProtocolPackets.u8((-1).toByte()))
        assertEquals(0xFF, ProtocolPackets.u8((-1).toByte()))
        assertEquals(0x11, ProtocolPackets.u8(0x11.toByte()))
        // 0x80 = -128 signed, must come back as 128 unsigned.
        assertEquals(128, ProtocolPackets.u8(0x80.toByte()))
    }

    @Test fun hexFormatting_isLowercaseTwoDigits() {
        assertEquals("00", ProtocolPackets.toHex(byteArrayOf(0x00)))
        assertEquals("0f", ProtocolPackets.toHex(byteArrayOf(0x0F)))
        assertEquals("ff", ProtocolPackets.toHex(byteArrayOf((-1).toByte())))
        assertEquals("ff0080", ProtocolPackets.toHex(byteArrayOf((-1).toByte(), 0x00, 0x80.toByte())))
    }

    @Test fun modeSwitch_isShortReport() {
        // The mode switch must map to wValue 0x0210 (report ID 0x10) — never the long-report value.
        assertEquals(0x0210, ProtocolPackets.wValueForReportSize(7))
        assertEquals(0x0211, ProtocolPackets.wValueForReportSize(20))
    }

    @Test fun wValue_rejectsMalformedSizes() {
        assertNull(ProtocolPackets.wValueForReportSize(6))
        assertNull(ProtocolPackets.wValueForReportSize(8))
        assertNull(ProtocolPackets.wValueForReportSize(19))
        assertNull(ProtocolPackets.wValueForReportSize(21))
        assertNull(ProtocolPackets.wValueForReportSize(0))
    }

    // --- Response parsing -----------------------------------------------------------

    @Test fun shortReportResponse_isRecognized() {
        val resp = byteArrayOf(0x10, 0xFF, 0x0E, 0x5B, 0x01, 0x03, 0x05)
        assertTrue(ProtocolPackets.isShortReportResponse(resp))
    }

    @Test fun shortReportResponse_rejectsWrongFeature() {
        val resp = byteArrayOf(0x10, 0xFF, 0x1F, 0x5B, 0x01, 0x03, 0x05)
        assertFalse(ProtocolPackets.isShortReportResponse(resp))
    }

    @Test fun longReportResponse_isRecognized() {
        val resp = ByteArray(20)
        resp[0] = 0x11.toByte(); resp[1] = 0xFF.toByte(); resp[2] = 0x0E.toByte()
        assertTrue(ProtocolPackets.isLongReportResponse(resp))
    }

    @Test fun longReportResponse_rejectsWrongFeature() {
        val resp = ByteArray(20)
        resp[0] = 0x11.toByte(); resp[1] = 0xFF.toByte(); resp[2] = 0x1F.toByte()
        assertFalse(ProtocolPackets.isLongReportResponse(resp))
    }

    @Test fun responses_rejectShortInput() {
        assertFalse(ProtocolPackets.isShortReportResponse(ByteArray(3)))
        assertFalse(ProtocolPackets.isLongReportResponse(ByteArray(2)))
    }

    // --- helpers ------------------------------------------------------------------

    private fun assertThrows(block: () -> Unit) {
        try {
            block()
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
