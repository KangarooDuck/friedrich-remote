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
                _lastPattern.value = LgAcProtocol.buildPattern(current.copy(power = true))
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
                Mode.COOL -> Mode.DRY
                Mode.DRY -> Mode.FAN
                Mode.FAN -> Mode.COOL
                else -> Mode.COOL
            }
            _lastAction.value = "Mode: ${newMode.name}"
            _lastPattern.value = LgAcProtocol.buildPattern(current.copy(mode = newMode))
            current.copy(mode = newMode)
        }
    }

    fun onTempUp() {
        _state.update { current ->
            if (!current.power) return@update current
            val newTemp = (current.tempCelsius + 1).coerceAtMost(LgAcProtocol.MAX_TEMP_C)
            _lastAction.value = "Temp: ${newTemp}°C"
            _lastPattern.value = LgAcProtocol.buildPattern(current.copy(tempCelsius = newTemp))
            current.copy(tempCelsius = newTemp)
        }
    }

    fun onTempDown() {
        _state.update { current ->
            if (!current.power) return@update current
            val newTemp = (current.tempCelsius - 1).coerceAtLeast(LgAcProtocol.MIN_TEMP_C)
            _lastAction.value = "Temp: ${newTemp}°C"
            _lastPattern.value = LgAcProtocol.buildPattern(current.copy(tempCelsius = newTemp))
            current.copy(tempCelsius = newTemp)
        }
    }

    fun onFanSpeedPress() {
        _state.update { current ->
            if (!current.power) return@update current
            val newSpeed = when (current.fanSpeed) {
                FanSpeed.LOW -> FanSpeed.MEDIUM
                FanSpeed.MEDIUM -> FanSpeed.HIGH
                FanSpeed.HIGH -> FanSpeed.LOW
                else -> FanSpeed.LOW
            }
            _lastAction.value = "Fan: ${newSpeed.name}"
            _lastPattern.value = LgAcProtocol.buildPattern(current.copy(fanSpeed = newSpeed))
            current.copy(fanSpeed = newSpeed)
        }
    }

    fun celsiusToFahrenheit(celsius: Int): Int {
        return (celsius * 9 / 5) + 32
    }
}
