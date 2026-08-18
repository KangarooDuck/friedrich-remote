package com.example.friedrichremote

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
        assertEquals(75, state.tempFahrenheit)
        assertEquals(FanSpeed.F1, state.fanSpeed)
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
        assertEquals(Mode.MONEY_SAVER, vm.state.value.mode)

        vm.onModePress()
        assertEquals(Mode.FAN_ONLY, vm.state.value.mode)

        vm.onModePress()
        assertEquals(Mode.DRY, vm.state.value.mode)

        vm.onModePress()
        assertEquals(Mode.COOL, vm.state.value.mode)
    }

    @Test
    fun `fan cycles correctly`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        assertEquals(FanSpeed.F1, vm.state.value.fanSpeed)

        vm.onFanSpeedPress()
        assertEquals(FanSpeed.F2, vm.state.value.fanSpeed)

        vm.onFanSpeedPress()
        assertEquals(FanSpeed.F3, vm.state.value.fanSpeed)

        vm.onFanSpeedPress()
        assertEquals(FanSpeed.F1, vm.state.value.fanSpeed)
    }

    @Test
    fun `temp up increases temperature`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        assertEquals(75, vm.state.value.tempFahrenheit)

        vm.onTempUp()
        assertEquals(76, vm.state.value.tempFahrenheit)
    }

    @Test
    fun `temp up stops at max`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        repeat(100) { vm.onTempUp() }
        assertEquals(LgAcProtocol.MAX_TEMP_F, vm.state.value.tempFahrenheit)
    }

    @Test
    fun `temp down stops at min`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        repeat(100) { vm.onTempDown() }
        assertEquals(LgAcProtocol.MIN_TEMP_F, vm.state.value.tempFahrenheit)
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

        vm.onSwingToggle()
        assertNotNull(vm.lastPattern.value)
    }

    @Test
    fun `swing toggle changes state`() {
        val vm = RemoteViewModel()
        vm.onPowerToggle()
        assertFalse(vm.state.value.swing)

        vm.onSwingToggle()
        assertTrue(vm.state.value.swing)

        vm.onSwingToggle()
        assertFalse(vm.state.value.swing)
    }

    @Test
    fun `swing toggle while power is off does not change swing`() {
        val vm = RemoteViewModel()
        vm.onSwingToggle()
        assertFalse(vm.state.value.swing)
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
        assertEquals(75, vm.state.value.tempFahrenheit)
        vm.onTempUp()
        assertEquals(75, vm.state.value.tempFahrenheit)  // unchanged when off
    }
}
