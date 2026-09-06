package com.lookouter.friedrichacremote.ir

import org.junit.Assert.*
import org.junit.Test

class LgAcProtocolTest {

    @Test
    fun `power off produces special off command`() {
        val state = AcState(power = false)
        assertEquals(LgAcProtocol.OFF_COMMAND, LgAcProtocol.stateToCode(state))
    }

    @Test
    fun `power on cool 60F F1 produces known code`() {
        val state = AcState(power = true, mode = Mode.COOL, tempFahrenheit = 60, fanSpeed = FanSpeed.F1)
        assertEquals(0x8820103, LgAcProtocol.stateToCode(state, powerOnCommand = true))
    }

    @Test
    fun `state change cool 60F F1 produces known code`() {
        val state = AcState(power = true, mode = Mode.COOL, tempFahrenheit = 60, fanSpeed = FanSpeed.F1)
        assertEquals(0x882810B, LgAcProtocol.stateToCode(state))
    }

    @Test
    fun `power on command has change flag clear`() {
        val state = AcState(power = true, mode = Mode.COOL, tempFahrenheit = 60, fanSpeed = FanSpeed.F1)
        val powerOn = LgAcProtocol.stateToCode(state, powerOnCommand = true)
        val change = LgAcProtocol.stateToCode(state, powerOnCommand = false)
        assertEquals(0, (powerOn shr 15) and 1)
        assertEquals(1, (change shr 15) and 1)
    }

    @Test
    fun `fan speeds produce known codes`() {
        fun code(fan: FanSpeed) = LgAcProtocol.rawStateToCode(Mode.COOL.code, 60, fan.code)
        assertEquals(0x882810B, code(FanSpeed.F1))
        assertEquals(0x882812D, code(FanSpeed.F2))
        assertEquals(0x882814F, code(FanSpeed.F3))
    }

    @Test
    fun `modes produce known codes`() {
        fun code(mode: Mode) = LgAcProtocol.rawStateToCode(mode.code, 60, FanSpeed.F1.code)
        assertEquals(0x882810B, code(Mode.COOL))
        assertEquals(0x882910C, code(Mode.DRY))
        assertEquals(0x882A10D, code(Mode.FAN_ONLY))
        assertEquals(0x882E101, code(Mode.MONEY_SAVER))
    }

    @Test
    fun `temperature maps to known codes`() {
        fun code(f: Int) = LgAcProtocol.rawStateToCode(Mode.COOL.code, f, FanSpeed.F1.code)
        assertEquals(0x882810B, code(60))
        assertEquals(0x8828082, code(75))
        assertEquals(0x8828B8D, code(86))
    }

    @Test
    fun `temperature below min clamps to 60F`() {
        assertEquals(
            LgAcProtocol.rawStateToCode(Mode.COOL.code, 60, FanSpeed.F1.code),
            LgAcProtocol.rawStateToCode(Mode.COOL.code, 50, FanSpeed.F1.code)
        )
    }

    @Test
    fun `temperature above max clamps to 86F`() {
        assertEquals(
            LgAcProtocol.rawStateToCode(Mode.COOL.code, 86, FanSpeed.F1.code),
            LgAcProtocol.rawStateToCode(Mode.COOL.code, 99, FanSpeed.F1.code)
        )
    }

    @Test
    fun `all modes and fan speeds produce valid signature`() {
        Mode.entries.forEach { mode ->
            FanSpeed.entries.forEach { fan ->
                val code = LgAcProtocol.rawStateToCode(mode.code, 72, fan.code)
                assertEquals(0x88, (code shr 20) and 0xFF)
            }
        }
    }

    @Test
    fun `checksum is correct for known value`() {
        val code = 0x8828B8D
        val checksum = code and 0xF
        val data = code shr 4
        assertEquals(irutils.sumNibbles(data, 4), checksum)
    }

    @Test
    fun `timing pattern has correct structure`() {
        val pattern = LgAcProtocol.codeToTimingPattern(0x882810B)
        assertTrue("Pattern must not be empty", pattern.isNotEmpty())
        assertEquals("Pattern should be even length", 0, pattern.size % 2)
        assertEquals("First element should be header mark", 8500, pattern[0])
        assertEquals("Second element should be header space", 4250, pattern[1])
        assertEquals("Last element should be gap", 40000, pattern[pattern.size - 1])
    }

    @Test
    fun `timing pattern length is correct for 28 bit code`() {
        assertEquals(60, LgAcProtocol.codeToTimingPattern(0x882810B).size)
    }

    @Test
    fun `all timing values are positive`() {
        LgAcProtocol.buildPattern(AcState(power = true)).forEach { value ->
            assertTrue("Value $value must be positive", value > 0)
        }
    }

    @Test
    fun `swing toggle is a known command`() {
        assertEquals(0x8813004, LgAcProtocol.SWING_TOGGLE)
    }
}
