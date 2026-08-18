package com.example.friedrichremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.friedrichremote.ir.AcState
import com.example.friedrichremote.ir.IrTransmitter
import com.example.friedrichremote.ir.LgAcProtocol

private val WallBackground = Color(0xFFC6CDD4)
private val RemoteTop = Color(0xFFF6F6F6)
private val RemoteBottom = Color(0xFFD9D9D9)
private val RemoteBorder = Color(0xFFB8B8B8)

private val LcdBackground = Color(0xFF16241E)
private val LcdBezel = Color(0xFF0A0A0A)
private val LcdOn = Color(0xFFA9E6C1)
private val LcdDim = Color(0xFF4B6558)

private val ButtonGrey = Color(0xFFEDEDED)
private val ButtonBorder = Color(0xFFA9A9A9)
private val ButtonText = Color(0xFF1A1A1A)
private val PowerRed = Color(0xFFE5533C)

@Composable
fun RemoteScreen() {
    val viewModel = remember { RemoteViewModel() }
    val state by viewModel.state.collectAsState()
    val lastAction by viewModel.lastAction.collectAsState()
    val context = LocalContext.current
    val irTransmitter = remember { IrTransmitter(context) }
    val hasIr = remember { irTransmitter.hasIrBlaster() }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lastAction) {
        if (lastAction.isNotEmpty()) {
            snackbarHostState.showSnackbar(lastAction, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = WallBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RemoteBody(state, viewModel, irTransmitter)
            Spacer(modifier = Modifier.height(20.dp))
            SettingsStrip(hasIr = hasIr)
        }
    }
}

@Composable
private fun RemoteBody(
    state: AcState,
    viewModel: RemoteViewModel,
    irTransmitter: IrTransmitter
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(RemoteTop, RemoteBottom)))
            .border(1.dp, RemoteBorder, RoundedCornerShape(28.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LcdPanel(state, viewModel)
        Spacer(modifier = Modifier.height(24.dp))

        PowerButton(
            isOn = state.power,
            onClick = {
                viewModel.onPowerToggle()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        ModeFanSwingRow(state, viewModel, irTransmitter)
        Spacer(modifier = Modifier.height(16.dp))

        TempControl(state, viewModel, irTransmitter)
    }
}

@Composable
private fun LcdPanel(state: AcState, viewModel: RemoteViewModel) {
    val on = state.power
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LcdBackground)
            .border(2.dp, LcdBezel, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MODE ${state.mode.displayName.uppercase()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (on) LcdOn else LcdDim
                )
                Text(
                    text = "FAN ${state.fanSpeed.displayName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (on) LcdOn else LcdDim
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (on) {
                    Text(
                        text = "${state.tempFahrenheit}°F",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        color = LcdOn
                    )
                } else {
                    Text(
                        text = "OFF",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        color = LcdDim
                    )
                }
                Text(
                    text = "°F",
                    fontSize = 14.sp,
                    color = if (on) LcdOn else LcdDim
                )
            }
        }
    }
}

@Composable
private fun PowerButton(isOn: Boolean, onClick: () -> Unit) {
    RubberButton(
        onClick = onClick,
        containerColor = PowerRed,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier.size(84.dp)
    ) {
        Text(
            text = if (isOn) "ON" else "OFF",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ModeFanSwingRow(
    state: AcState,
    viewModel: RemoteViewModel,
    irTransmitter: IrTransmitter
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        RubberButton(
            onClick = {
                viewModel.onModePress()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            },
            enabled = state.power,
            modifier = Modifier.weight(1f).height(72.dp)
        ) {
            LabeledValue("MODE", state.mode.displayName.uppercase())
        }
        RubberButton(
            onClick = {
                viewModel.onFanSpeedPress()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            },
            enabled = state.power,
            modifier = Modifier.weight(1f).height(72.dp)
        ) {
            LabeledValue("FAN", state.fanSpeed.displayName)
        }
        RubberButton(
            onClick = {
                viewModel.onSwingToggle()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            },
            enabled = state.power,
            modifier = Modifier.weight(1f).height(72.dp)
        ) {
            LabeledValue("SWING", if (state.swing) "ON" else "OFF")
        }
    }
}

@Composable
private fun TempControl(
    state: AcState,
    viewModel: RemoteViewModel,
    irTransmitter: IrTransmitter
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RubberButton(
            onClick = {
                viewModel.onTempDown()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            },
            enabled = state.power && state.tempFahrenheit > LgAcProtocol.MIN_TEMP_F,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Text("\u25BC", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = if (state.power) "${state.tempFahrenheit}°F" else "---",
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = if (state.power) ButtonText else ButtonText.copy(alpha = 0.35f),
            modifier = Modifier.width(96.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        RubberButton(
            onClick = {
                viewModel.onTempUp()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            },
            enabled = state.power && state.tempFahrenheit < LgAcProtocol.MAX_TEMP_F,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Text("\u25B2", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = ButtonText.copy(alpha = 0.55f))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RubberButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = ButtonGrey,
    contentColor: Color = ButtonText,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .shadow(3.dp, shape)
            .border(1.dp, ButtonBorder, shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.45f),
            disabledContentColor = contentColor.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(8.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsStrip(hasIr: Boolean) {
    Surface(
        color = Color(0xFFEAEAEA),
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasIr) "IR ✓" else "No IR",
                fontSize = 12.sp,
                color = if (hasIr) Color(0xFF2E7D5B) else Color(0xFFE5533C),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
