package com.example.friedrichremote.ir

import org.junit.Assert.*
import org.junit.Test

class LgAcProtocolTest {

    @Test
    fun `cool 24C fan auto produces known code`() {
        val state = AcState(power = true, mode = Mode.COOL, tempCelsius = 24, fanSpeed = FanSpeed.AUTO)
        val code = LgAcProtocol.stateToCode(state)
        assertEquals(0x880095E, code)
    }

    @Test
    fun `cool 24C fan low produces known code`() {
        val state = AcState(power = true, mode = Mode.COOL, tempCelsius = 24, fanSpeed = FanSpeed.LOW)
        val code = LgAcProtocol.stateToCode(state)
        assertEquals(0x880091A, code)
    }

    @Test
    fun `cool 16C fan auto produces correct code`() {
        val state = AcState(power = true, mode = Mode.COOL, tempCelsius = 16, fanSpeed = FanSpeed.AUTO)
        val code = LgAcProtocol.stateToCode(state)
        assertEquals(0x8800156, code)
    }

    @Test
    fun `heat 30C fan high produces known code`() {
        val state = AcState(power = true, mode = Mode.HEAT, tempCelsius = 30, fanSpeed = FanSpeed.HIGH)
        val code = LgAcProtocol.stateToCode(state)
        assertEquals(0x8804FAD, code)
    }

    @Test
    fun `power off produces special off command`() {
        val state = AcState(power = false)
        val code = LgAcProtocol.stateToCode(state)
        assertEquals(LgAcProtocol.OFF_COMMAND, code)
    }

    @Test
    fun `power on with cool auto high fan produces valid signature`() {
        val state = AcState(power = true)
        val code = LgAcProtocol.stateToCode(state)
        val signature = (code shr 20) and 0xFF
        assertEquals(0x88, signature)
    }

    @Test
    fun `temp below min is clamped to 16`() {
        val state = AcState(power = true, tempCelsius = 10)
        val code = LgAcProtocol.stateToCode(state)
        val tempBits = (code shr 8) and 0xF
        assertEquals(1, tempBits)  // 16 - 15 = 1
    }

    @Test
    fun `temp above max is clamped to 30`() {
        val state = AcState(power = true, tempCelsius = 35)
        val code = LgAcProtocol.stateToCode(state)
        val tempBits = (code shr 8) and 0xF
        assertEquals(15, tempBits)  // 30 - 15 = 15
    }

    @Test
    fun `all mode values produce valid codes`() {
        Mode.entries.forEach { mode ->
            val state = AcState(power = true, mode = mode, tempCelsius = 24, fanSpeed = FanSpeed.AUTO)
            val code = LgAcProtocol.stateToCode(state)
            val signature = (code shr 20) and 0xFF
            assertEquals("Mode $mode should have valid signature", 0x88, signature)
        }
    }

    @Test
    fun `all fan speeds produce valid codes`() {
        FanSpeed.entries.forEach { fanSpeed ->
            val state = AcState(power = true, mode = Mode.COOL, tempCelsius = 24, fanSpeed = fanSpeed)
            val code = LgAcProtocol.stateToCode(state)
            val signature = (code shr 20) and 0xFF
            assertEquals("FanSpeed $fanSpeed should have valid signature", 0x88, signature)
        }
    }

    @Test
    fun `timing pattern has correct structure`() {
        val code = 0x880095E
        val pattern = LgAcProtocol.codeToTimingPattern(code)

        assertTrue("Pattern must not be empty", pattern.isNotEmpty())

        assertEquals("Pattern starts with mark, ends with space (should be even length)", 0, pattern.size % 2)

        assertEquals("First element should be header mark", 8500, pattern[0])
        assertEquals("Second element should be header space", 4250, pattern[1])

        assertEquals("Last element should be gap", 40000, pattern[pattern.size - 1])
    }

    @Test
    fun `timing pattern length is correct for 28 bit code`() {
        val code = 0x880095E
        val pattern = LgAcProtocol.codeToTimingPattern(code)
        assertEquals(64, pattern.size)
    }

    @Test
    fun `all timing values are positive`() {
        val state = AcState(power = true)
        val pattern = LgAcProtocol.buildPattern(state)
        pattern.forEach { value ->
            assertTrue("Value $value must be positive", value > 0)
        }
    }

    @Test
    fun `checksum is correct for known values`() {
        val code = 0x880095E
        val checksum = code and 0xF
        val data = code shr 4
        val expected = irutils.sumNibbles(data, 4)
        assertEquals(expected, checksum)
    }

    @Test
    fun `bad checksum does not match`() {
        val code = 0x880095E
        val badChecksum = ((code and 0xF) + 1) and 0xF
        val badCode = (code and 0xF.inv()) or badChecksum
        val checksum = badCode and 0xF
        val data = badCode shr 4
        val expected = irutils.sumNibbles(data, 4)
        assertNotEquals(expected, checksum)
    }

    @Test
    fun `dry mode produces mode bits 001`() {
        val state = AcState(power = true, mode = Mode.DRY, tempCelsius = 24, fanSpeed = FanSpeed.AUTO)
        val code = LgAcProtocol.stateToCode(state)
        val modeBits = (code shr 12) and 0x7
        assertEquals(1, modeBits)
    }

    @Test
    fun `fan mode produces mode bits 010`() {
        val state = AcState(power = true, mode = Mode.FAN, tempCelsius = 24, fanSpeed = FanSpeed.AUTO)
        val code = LgAcProtocol.stateToCode(state)
        val modeBits = (code shr 12) and 0x7
        assertEquals(2, modeBits)
    }

    @Test
    fun `auto mode produces mode bits 011`() {
        val state = AcState(power = true, mode = Mode.AUTO, tempCelsius = 24, fanSpeed = FanSpeed.AUTO)
        val code = LgAcProtocol.stateToCode(state)
        val modeBits = (code shr 12) and 0x7
        assertEquals(3, modeBits)
    }

    @Test
    fun `heat mode produces mode bits 100`() {
        val state = AcState(power = true, mode = Mode.HEAT, tempCelsius = 24, fanSpeed = FanSpeed.AUTO)
        val code = LgAcProtocol.stateToCode(state)
        val modeBits = (code shr 12) and 0x7
        assertEquals(4, modeBits)
    }
}
