package com.example.friedrichremote.ir

import android.util.Log

enum class Mode(val code: Int, val displayName: String) {
    COOL(0, "Cool"),
    MONEY_SAVER(6, "Money Saver"),
    FAN_ONLY(2, "Fan Only"),
    DRY(1, "Dry")
}

enum class FanSpeed(val code: Int, val displayName: String) {
    F1(0, "F1"),
    F2(2, "F2"),
    F3(4, "F3")
}

data class AcState(
    val power: Boolean = false,
    val mode: Mode = Mode.COOL,
    val tempFahrenheit: Int = 75,
    val fanSpeed: FanSpeed = FanSpeed.F1,
    val swing: Boolean = false
)

object LgAcProtocol {
    const val FREQ_HZ = 38000
    const val TAG = "LgAcProtocol"

    private const val SIGNATURE = 0x88
    private const val HDR_MARK = 8500
    private const val HDR_SPACE = 4250
    private const val BIT_MARK = 550
    private const val ONE_SPACE = 1600
    private const val ZERO_SPACE = 550
    private const val GAP = 40000

    const val MIN_TEMP_F = 60
    const val MAX_TEMP_F = 86

    const val OFF_COMMAND = 0x88C0051
    const val SWING_TOGGLE = 0x8813004

    fun stateToCode(state: AcState, powerOnCommand: Boolean = false): Int {
        if (!state.power) return OFF_COMMAND
        return rawStateToCode(
            state.mode.code,
            state.tempFahrenheit,
            state.fanSpeed.code,
            powerOnCommand
        )
    }

    fun rawStateToCode(
        modeCode: Int,
        tempFahrenheit: Int,
        fanCode: Int,
        powerOnCommand: Boolean = false
    ): Int {
        val tempCode = tempFahrenheit.coerceIn(MIN_TEMP_F, MAX_TEMP_F) - 59
        var code = 0
        code = code or (SIGNATURE shl 20)
        code = code or (1 shl 17)                                // on-flag
        code = code or ((if (powerOnCommand) 0 else 1) shl 15)   // change-flag
        code = code or ((modeCode and 0x7) shl 12)               // mode (3 bits)
        code = code or ((tempCode and 0x10) shl 3)               // temp high bit -> bit 7
        code = code or ((tempCode and 0x0F) shl 8)               // temp low nibble -> bits 8-11
        code = code or ((fanCode and 0x7) shl 4)                 // fan (bits 4-6)
        val checksum = irutils.sumNibbles(code shr 4, 4)
        return code or checksum
    }

    fun codeToTimingPattern(code: Int): IntArray {
        val parts = mutableListOf<Int>()

        addDataFrame(parts, code)

        val arr = parts.toIntArray()
        Log.d(TAG, "Pattern: ${formatHex(code)} ${arr.asList()}")
        return arr
    }

    private fun addDataFrame(parts: MutableList<Int>, code: Int) {
        parts.add(HDR_MARK)
        parts.add(HDR_SPACE)

        for (i in 27 downTo 0) {
            parts.add(BIT_MARK)
            if ((code shr i) and 1 == 1) {
                parts.add(ONE_SPACE)
            } else {
                parts.add(ZERO_SPACE)
            }
        }

        parts.add(BIT_MARK)
        parts.add(GAP)
    }

    fun buildPattern(state: AcState): IntArray =
        codeToTimingPattern(stateToCode(state, powerOnCommand = false))

    fun buildPowerOnPattern(state: AcState): IntArray =
        codeToTimingPattern(stateToCode(state, powerOnCommand = true))

    private fun formatHex(code: Int): String {
        return "0x${java.lang.Integer.toHexString(code).uppercase()}"
    }
}

object irutils {
    fun sumNibbles(value: Int, nibbles: Int): Int {
        var sum = 0
        for (i in 0 until nibbles) {
            sum += (value shr (i * 4)) and 0xF
        }
        return sum and 0xF
    }
}
