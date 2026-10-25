package ai.desert.studio.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0C0E12)
val SurfaceDark = Color(0xFF15181E)
val SurfaceElevated = Color(0xFF1E222A)
val BorderDark = Color(0xFF2B313B)

val AccentPrimary = Color(0xFF10B981) // Emerald Mint
val AccentSecondary = Color(0xFF06B6D4) // Cyan
val WarningRedaction = Color(0xFFF43F5E) // Redaction Rose
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.Black,
    secondary = AccentSecondary,
    onSecondary = Color.Black,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark
)

@Composable
fun DesertStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
