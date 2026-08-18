package com.example.friedrichremote.ui

import com.example.friedrichremote.ir.AcState
import com.example.friedrichremote.ir.FanSpeed
import com.example.friedrichremote.ir.LgAcProtocol
import com.example.friedrichremote.ir.Mode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RemoteViewModel {

    private val _state = MutableStateFlow(AcState())
    val state: StateFlow<AcState> = _state.asStateFlow()

    private val _lastPattern = MutableStateFlow<IntArray?>(null)
    val lastPattern: StateFlow<IntArray?> = _lastPattern.asStateFlow()

    private val _lastAction = MutableStateFlow("")
    val lastAction: StateFlow<String> = _lastAction.asStateFlow()

    fun onPowerToggle() {
        _state.update { current ->
            val newPower = !current.power
            if (newPower) {
                _lastAction.value = "Power ON"
                _lastPattern.value = LgAcProtocol.buildPowerOnPattern(current.copy(power = true))
            } else {
                _lastAction.value = "Power OFF"
                _lastPattern.value = LgAcProtocol.codeToTimingPattern(LgAcProtocol.OFF_COMMAND)
            }
            current.copy(power = newPower)
        }
    }

    fun onModePress() {
        _state.update { current ->
            if (!current.power) return@update current
            val newMode = when (current.mode) {
                Mode.COOL -> Mode.MONEY_SAVER
                Mode.MONEY_SAVER -> Mode.FAN_ONLY
                Mode.FAN_ONLY -> Mode.DRY
                Mode.DRY -> Mode.COOL
            }
            _lastAction.value = "Mode: ${newMode.displayName}"
            _lastPattern.value = LgAcProtocol.buildPattern(current.copy(mode = newMode))
            current.copy(mode = newMode)
        }
    }

    fun onTempUp() {
        _state.update { current ->
            if (!current.power) return@update current
            val newTemp = (current.tempFahrenheit + 1).coerceAtMost(LgAcProtocol.MAX_TEMP_F)
            _lastAction.value = "Temp: ${newTemp}°F"
            _lastPattern.value = LgAcProtocol.buildPattern(current.copy(tempFahrenheit = newTemp))
            current.copy(tempFahrenheit = newTemp)
        }
    }

    fun onTempDown() {
        _state.update { current ->
            if (!current.power) return@update current
            val newTemp = (current.tempFahrenheit - 1).coerceAtLeast(LgAcProtocol.MIN_TEMP_F)
            _lastAction.value = "Temp: ${newTemp}°F"
            _lastPattern.value = LgAcProtocol.buildPattern(current.copy(tempFahrenheit = newTemp))
            current.copy(tempFahrenheit = newTemp)
        }
    }

    fun onFanSpeedPress() {
        _state.update { current ->
            if (!current.power) return@update current
            val newSpeed = when (current.fanSpeed) {
                FanSpeed.F1 -> FanSpeed.F2
                FanSpeed.F2 -> FanSpeed.F3
                FanSpeed.F3 -> FanSpeed.F1
            }
            _lastAction.value = "Fan: ${newSpeed.displayName}"
            _lastPattern.value = LgAcProtocol.buildPattern(current.copy(fanSpeed = newSpeed))
            current.copy(fanSpeed = newSpeed)
        }
    }

    fun onSwingToggle() {
        _state.update { current ->
            if (!current.power) return@update current
            val newSwing = !current.swing
            _lastAction.value = "Swing ${if (newSwing) "ON" else "OFF"}"
            _lastPattern.value = LgAcProtocol.codeToTimingPattern(LgAcProtocol.SWING_TOGGLE)
            current.copy(swing = newSwing)
        }
    }
}
