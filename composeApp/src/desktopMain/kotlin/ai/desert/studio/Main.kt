package ai.desert.studio

import ai.desert.studio.ui.App
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(width = 1000.dp, height = 750.dp)
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Desert AI Studio — On-Device Multimodal Engine"
    ) {
        App()
    }
}
