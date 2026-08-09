package com.example.friedrichremote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.friedrichremote.ir.FanSpeed
import com.example.friedrichremote.ir.IrTransmitter
import com.example.friedrichremote.ir.LgAcProtocol
import com.example.friedrichremote.ir.Mode
import com.example.friedrichremote.ir.ProtocolVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen() {
    val viewModel = remember { RemoteViewModel() }
    val state by viewModel.state.collectAsState()
    val lastAction by viewModel.lastAction.collectAsState()
    val context = LocalContext.current
    val irTransmitter = remember { IrTransmitter(context) }
    val hasIr = remember { irTransmitter.hasIrBlaster() }

    val snackbarHostState = remember { SnackbarHostState() }
    var protocolVariant by remember { mutableStateOf(LgAcProtocol.variant) }
    var powerCode by remember { mutableStateOf(LgAcProtocol.onPowerCode) }

    LaunchedEffect(lastAction) {
        if (lastAction.isNotEmpty()) {
            snackbarHostState.showSnackbar(lastAction, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F1923),
        topBar = {
            TopAppBar(
                title = { Text("Friedrich Remote", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F1923)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            DisplayPanel(state, viewModel)

            PowerButton(
                isOn = state.power,
                onClick = {
                    viewModel.onPowerToggle()
                    viewModel.lastPattern.value?.let { pattern ->
                        repeat(3) { irTransmitter.transmit(pattern); Thread.sleep(30) }
                    }
                }
            )

            ModeAndFanRow(state, viewModel, irTransmitter)

            TempControl(state, viewModel, irTransmitter)

            StatusBar(hasIr, protocolVariant, powerCode,
                onToggleVariant = {
                    val next = if (LgAcProtocol.variant == ProtocolVariant.LG) {
                        ProtocolVariant.LG2
                    } else {
                        ProtocolVariant.LG
                    }
                    LgAcProtocol.variant = next
                    protocolVariant = next
                },
                onTogglePowerCode = {
                    val next = (powerCode + 1) % 4
                    LgAcProtocol.onPowerCode = next
                    powerCode = next
                }
            )
        }
    }
}

@Composable
private fun DisplayPanel(
    state: com.example.friedrichremote.ir.AcState,
    viewModel: RemoteViewModel
) {
    val gradientColors = if (state.power) {
        listOf(Color(0xFF1A3A5C), Color(0xFF0D2137))
    } else {
        listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A))
    }

    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush = Brush.verticalGradient(gradientColors))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (state.power) "${viewModel.celsiusToFahrenheit(state.tempCelsius)}°F" else "OFF",
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = if (state.power) Color.White else Color.White.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.mode.name,
                fontSize = 14.sp,
                color = if (state.power) Color(0xFF90CAF9) else Color.White.copy(alpha = 0.2f)
            )
            Text(
                text = "Fan: ${state.fanSpeed.displayName()}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = if (state.power) 0.5f else 0.2f)
            )
        }
    }
}

@Composable
private fun PowerButton(isOn: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        if (isOn) Color(0xFF2196F3) else Color(0xFF424242),
        label = "powerColor"
    )
    Button(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = if (isOn) "ON" else "OFF",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ModeAndFanRow(
    state: com.example.friedrichremote.ir.AcState,
    viewModel: RemoteViewModel,
    irTransmitter: IrTransmitter
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        ControlButton(
            label = "MODE",
            value = state.mode.displayName(),
            enabled = state.power,
            onClick = {
                viewModel.onModePress()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            },
            modifier = Modifier.weight(1f)
        )
        ControlButton(
            label = "FAN",
            value = state.fanSpeed.displayName(),
            enabled = state.power,
            onClick = {
                viewModel.onFanSpeedPress()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TempControl(
    state: com.example.friedrichremote.ir.AcState,
    viewModel: RemoteViewModel,
    irTransmitter: IrTransmitter
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TempButton(
            label = "\u25BC",
            enabled = state.power && state.tempCelsius > LgAcProtocol.MIN_TEMP_C,
            onClick = {
                viewModel.onTempDown()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            }
        )
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.width(100.dp).height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = Color.White.copy(alpha = 0.1f),
                disabledContentColor = Color.White
            )
        ) {
            Text(
                text = if (state.power) "${viewModel.celsiusToFahrenheit(state.tempCelsius)}°F" else "---",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
        TempButton(
            label = "\u25B2",
            enabled = state.power && state.tempCelsius < LgAcProtocol.MAX_TEMP_C,
            onClick = {
                viewModel.onTempUp()
                viewModel.lastPattern.value?.let { irTransmitter.transmit(it) }
            }
        )
    }
}

@Composable
private fun ControlButton(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A3A5C),
            disabledContainerColor = Color(0xFF1A1A2E)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
private fun TempButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A3A5C),
            disabledContainerColor = Color(0xFF1A1A2E)
        )
    ) {
        Text(label, fontSize = 20.sp, color = Color.White)
    }
}

@Composable
private fun StatusBar(hasIr: Boolean, protocolVariant: ProtocolVariant, powerCode: Int, onToggleVariant: () -> Unit, onTogglePowerCode: () -> Unit) {
    Surface(
        color = Color(0xFF1A2A3A),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\u2713" , color = if (hasIr) Color(0xFF4CAF50) else Color(0xFFFF5722), fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (hasIr) "IR" else "No IR", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Button(
                onClick = onTogglePowerCode,
                colors = ButtonDefaults.buttonColors(containerColor = when (powerCode) { 1 -> Color(0xFF4CAF50); 2 -> Color(0xFFFF9800); else -> Color(0xFF555555) }),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("P$powerCode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Button(
                onClick = onToggleVariant,
                colors = ButtonDefaults.buttonColors(containerColor = if (protocolVariant == ProtocolVariant.LG2) Color(0xFF4CAF50) else Color(0xFF555555)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(protocolVariant.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

private fun Mode.displayName(): String = when (this) { Mode.COOL -> "Cool"; Mode.DRY -> "Dry"; Mode.FAN -> "Fan"; Mode.HEAT -> "Heat"; Mode.AUTO -> "Auto" }
private fun FanSpeed.displayName(): String = when (this) { FanSpeed.LOWEST -> "F1"; FanSpeed.LOW -> "F1"; FanSpeed.MEDIUM -> "F2"; FanSpeed.HIGH -> "F3"; FanSpeed.AUTO -> "AUTO" }
