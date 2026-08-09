package com.example.friedrichremote

import com.example.friedrichremote.ir.AcState
import com.example.friedrichremote.ir.FanSpeed
import com.example.friedrichremote.ir.LgAcProtocol
import com.example.friedrichremote.ir.Mode
import com.example.friedrichremote.ui.RemoteViewModel
import org.junit.Assert.*
import org.junit.Test

class RemoteViewModelTest {

    @Test
    fun `initial state is correct`() {
        val vm = RemoteViewModel()
        val state = vm.state.value
        assertFalse(state.power)
        assertEquals(Mode.COOL, state.mode)
        assertEquals(24, state.tempCelsius)
        assertEquals(FanSpeed.AUTO, state.fanSpeed)
    }

    @Test
    fun `power toggle changes state`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        assertTrue(vm.state.value.power)
        vm.onPowerToggle()
        assertFalse(vm.state.value.power)
    }

    @Test
    fun `mode cycles correctly`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        assertEquals(Mode.COOL, vm.state.value.mode)

        vm.onModePress()
        assertEquals(Mode.DRY, vm.state.value.mode)

        vm.onModePress()
        assertEquals(Mode.FAN, vm.state.value.mode)

        vm.onModePress()
        assertEquals(Mode.HEAT, vm.state.value.mode)

        vm.onModePress()
        assertEquals(Mode.AUTO, vm.state.value.mode)

        vm.onModePress()
        assertEquals(Mode.COOL, vm.state.value.mode)
    }

    @Test
    fun `temp up increases temperature`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        assertEquals(24, vm.state.value.tempCelsius)

        vm.onTempUp()
        assertEquals(25, vm.state.value.tempCelsius)
    }

    @Test
    fun `temp up stops at max`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        repeat(20) { vm.onTempUp() }
        assertEquals(LgAcProtocol.MAX_TEMP_C, vm.state.value.tempCelsius)
    }

    @Test
    fun `temp down stops at min`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        repeat(20) { vm.onTempDown() }
        assertEquals(LgAcProtocol.MIN_TEMP_C, vm.state.value.tempCelsius)
    }

    @Test
    fun `fan speed cycles correctly`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        assertEquals(FanSpeed.AUTO, vm.state.value.fanSpeed)

        vm.onFanSpeedPress()
        assertEquals(FanSpeed.LOW, vm.state.value.fanSpeed)

        vm.onFanSpeedPress()
        assertEquals(FanSpeed.MEDIUM, vm.state.value.fanSpeed)

        vm.onFanSpeedPress()
        assertEquals(FanSpeed.HIGH, vm.state.value.fanSpeed)

        vm.onFanSpeedPress()
        assertEquals(FanSpeed.LOWEST, vm.state.value.fanSpeed)

        vm.onFanSpeedPress()
        assertEquals(FanSpeed.AUTO, vm.state.value.fanSpeed)
    }

    @Test
    fun `each action produces a timing pattern`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        assertNotNull(vm.lastPattern.value)

        vm.onModePress()
        assertNotNull(vm.lastPattern.value)

        vm.onTempUp()
        assertNotNull(vm.lastPattern.value)

        vm.onTempDown()
        assertNotNull(vm.lastPattern.value)

        vm.onFanSpeedPress()
        assertNotNull(vm.lastPattern.value)
    }

    @Test
    fun `celsius to fahrenheit conversion is correct`() {
        val vm = RemoteViewModel()
        assertEquals(32, vm.celsiusToFahrenheit(0))
        assertEquals(75, vm.celsiusToFahrenheit(24))
        assertEquals(86, vm.celsiusToFahrenheit(30))
        assertEquals(60, vm.celsiusToFahrenheit(16))
    }

    @Test
    fun `mode change while power is off does not change mode`() {
        val vm = RemoteViewModel()
        assertEquals(Mode.COOL, vm.state.value.mode)
        vm.onModePress()
        assertEquals(Mode.COOL, vm.state.value.mode)  // unchanged when off
    }

    @Test
    fun `temp change while power is off does not change temp`() {
        val vm = RemoteViewModel()
        assertEquals(24, vm.state.value.tempCelsius)
        vm.onTempUp()
        assertEquals(24, vm.state.value.tempCelsius)  // unchanged when off
    }
}
