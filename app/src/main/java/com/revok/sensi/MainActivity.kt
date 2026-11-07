package com.revok.sensi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.concurrent.thread
import kotlin.math.roundToInt

private val Red = Color(0xFFE10600)

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Color(0xFF0B0B0B), surface = Color(0xFF161616))) {
                Screen()
            }
        }
    }
}

@Composable
fun Screen() {
    var mode by remember { mutableStateOf(Shell.mode()) }
    var pointer by remember { mutableFloatStateOf(0f) }
    var refresh by remember { mutableFloatStateOf(60f) }
    var sat by remember { mutableFloatStateOf(100f) }
    var log by remember { mutableStateOf("") }

    fun go(cmd: String) = thread { val r = Shell.run(cmd); log = if (r.isBlank()) "OK: $cmd" else r }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0B0B)).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("REVOK SENSI", style = MaterialTheme.typography.headlineMedium, color = Red)
        Text("Acesso: ${mode.name}", color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { Shell.requestShizuku(); mode = Shell.mode() }) { Text("Shizuku") }
            OutlinedButton(onClick = { thread { mode = Shell.mode() } }) { Text("Verificar root") }
        }

        Text("Velocidade do ponteiro: ${pointer.roundToInt()}")
        Slider(pointer, { pointer = it }, valueRange = -7f..7f, steps = 13,
            onValueChangeFinished = { go("settings put system pointer_speed ${pointer.roundToInt()}") })

        Text("Taxa de atualização: ${refresh.roundToInt()} Hz")
        Slider(refresh, { refresh = it }, valueRange = 60f..144f,
            onValueChangeFinished = {
                val hz = refresh.roundToInt()
                go("settings put system min_refresh_rate $hz; settings put system peak_refresh_rate $hz")
            })

        Text("Saturação: ${sat.roundToInt()}%")
        Slider(sat, { sat = it }, valueRange = 50f..150f,
            onValueChangeFinished = { go("cmd color_display set-saturation ${(sat / 100f)}") })

        OutlinedButton(onClick = {
            pointer = 0f; refresh = 60f; sat = 100f
            go("settings put system pointer_speed 0; settings delete system min_refresh_rate; " +
               "settings delete system peak_refresh_rate; cmd color_display set-saturation 1.0")
        }, modifier = Modifier.fillMaxWidth()) { Text("Restaurar padrão") }

        if (log.isNotBlank()) Text(log, color = Color.Gray)
    }
}
