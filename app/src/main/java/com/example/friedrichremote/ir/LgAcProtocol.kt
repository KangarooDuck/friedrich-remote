package com.example.friedrichremote.ir

import android.util.Log

enum class ProtocolVariant { LG, LG2 }

enum class Mode { COOL, DRY, FAN, AUTO, HEAT }

enum class FanSpeed { LOWEST, LOW, MEDIUM, HIGH, AUTO }

data class AcState(
    val power: Boolean = false,
    val mode: Mode = Mode.COOL,
    val tempCelsius: Int = 16,
    val fanSpeed: FanSpeed = FanSpeed.MEDIUM
)

object LgAcProtocol {
    const val FREQ_HZ = 38000
    const val TAG = "LgAcProtocol"

    var variant: ProtocolVariant = ProtocolVariant.LG
    var onPowerCode: Int = 0  // 0 = Power ON per IRremoteESP8266

    private fun hdrMark() = if (variant == ProtocolVariant.LG2) 3200 else 8500
    private fun hdrSpace() = if (variant == ProtocolVariant.LG2) 9900 else 4250
    private fun bitMark() = if (variant == ProtocolVariant.LG2) 480 else 550

    private const val SIGNATURE = 0x88
    private const val ONE_SPACE = 1600
    private const val ZERO_SPACE = 550
    private const val RPT_SPACE = 2250
    private const val GAP = 40000

    const val MIN_TEMP_C = 16
    const val MAX_TEMP_C = 30

    const val OFF_COMMAND = 0x88C0051

    fun stateToCode(state: AcState): Int {
        if (!state.power) return OFF_COMMAND
        return rawStateToCode(state.mode.code(), tempToCode(state.tempCelsius), state.fanSpeed.code())
    }

    fun rawStateToCode(modeCode: Int, tempCode: Int, fanCode: Int): Int {
        var code = 0
        code = code or (SIGNATURE shl 20)
        code = code or (onPowerCode shl 18)
        code = code or (modeCode shl 12)
        code = code or (tempCode shl 8)
        code = code or (fanCode shl 4)
        val checksum = irutils.sumNibbles(code shr 4, 4)
        code = code or checksum
        return code
    }

    fun codeToTimingPattern(code: Int): IntArray {
        val parts = mutableListOf<Int>()

        addDataFrame(parts, code)
        addRepeatFrame(parts)

        val arr = parts.toIntArray()
        Log.d(TAG, "Pattern[${variant.name}]: ${formatHex(code)} ${arr.asList()}")
        return arr
    }

    fun codeToSimplePattern(code: Int): IntArray {
        val parts = mutableListOf<Int>()

        parts.add(hdrMark())
        parts.add(hdrSpace())

        for (i in 27 downTo 0) {
            parts.add(bitMark())
            if ((code shr i) and 1 == 1) {
                parts.add(ONE_SPACE)
            } else {
                parts.add(ZERO_SPACE)
            }
        }

        parts.add(bitMark())
        parts.add(GAP)

        val arr = parts.toIntArray()
        Log.d(TAG, "Simple[${variant.name}]: ${formatHex(code)} ${arr.asList()}")
        return arr
    }

    private fun addDataFrame(parts: MutableList<Int>, code: Int) {
        parts.add(hdrMark())
        parts.add(hdrSpace())

        for (i in 27 downTo 0) {
            parts.add(bitMark())
            if ((code shr i) and 1 == 1) {
                parts.add(ONE_SPACE)
            } else {
                parts.add(ZERO_SPACE)
            }
        }

        parts.add(bitMark())
        parts.add(GAP)
    }

    private fun addRepeatFrame(parts: MutableList<Int>) {
        parts.add(hdrMark())
        parts.add(RPT_SPACE)
        parts.add(bitMark())
        parts.add(GAP)
    }

    fun buildPattern(state: AcState): IntArray {
        return codeToTimingPattern(stateToCode(state))
    }

    fun buildTurnOnPattern(state: AcState): IntArray {
        val off = codeToTimingPattern(OFF_COMMAND)
        val on = codeToTimingPattern(stateToCode(state.copy(power = true)))
        return off + on
    }

    private fun formatHex(code: Int): String {
        return "0x${java.lang.Integer.toHexString(code).uppercase()}"
    }

    fun modeCode(mode: Mode): Int = when (mode) {
        Mode.COOL -> 0; Mode.DRY -> 1; Mode.FAN -> 2; Mode.AUTO -> 3; Mode.HEAT -> 4
    }

    private fun Mode.code(): Int = modeCode(this)

    private fun tempToCode(celsius: Int): Int {
        val clamped = celsius.coerceIn(MIN_TEMP_C, MAX_TEMP_C)
        return clamped - 15
    }

    private fun FanSpeed.code(): Int = when (this) {
        FanSpeed.LOWEST -> 0   // F1
        FanSpeed.LOW -> 0      // F1
        FanSpeed.MEDIUM -> 2   // F2
        FanSpeed.HIGH -> 4     // F3
        FanSpeed.AUTO -> 5     // Auto
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
