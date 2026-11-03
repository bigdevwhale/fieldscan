package app.fieldscan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import app.fieldscan.R

/**
 * The device: dark plastic case, a three-colour e-paper screen (paper, ink, red). The dark theme is the
 * screen's night mode: the paper goes dark and the ink light. Reading a colour reads [dark], so a switch redraws.
 */
object C {
    var dark by mutableStateOf(false)
    val case = Color(0xFF2B2D2A)
    val caseL = Color(0xFF3C3F3B)
    val caseD = Color(0xFF161715)
    val caseText = Color(0xFF9A9C94)
    val paper get() = if (dark) Color(0xFF1E1F1C) else Color(0xFFE3E0D4)
    val paperD get() = if (dark) Color(0xFF3B3C37) else Color(0xFFC9C5B6)
    val ink get() = if (dark) Color(0xFFDAD6C8) else Color(0xFF1D1D1B)
    val muted get() = if (dark) Color(0xFF9C988C) else Color(0xFF6D6A60)
    val red get() = if (dark) Color(0xFFD6493F) else Color(0xFFB3302A)
    val redL get() = if (dark) Color(0xFF4A2521) else Color(0xFFE9C9C2)
    /** Dims what lies under a dialog: always dark, whatever the theme. */
    val scrim = Color(0x8C000000)
    /** Behind photos while they load. */
    val film = Color(0xFF111210)
}

val Pixel = FontFamily(Font(R.font.m6x11))

object Ty {
    val body get() = TextStyle(fontFamily = Pixel, fontSize = 17.sp, lineHeight = 19.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Pixel, fontSize = 15.sp, lineHeight = 17.sp, color = C.muted)
    val head get() = TextStyle(fontFamily = Pixel, fontSize = 26.sp, lineHeight = 28.sp, color = C.ink, letterSpacing = 1.sp)
    val big get() = TextStyle(fontFamily = Pixel, fontSize = 34.sp, lineHeight = 36.sp, color = C.ink)
}

/** Russian or not: all the copy is in code, both languages side by side. */
val LocalRu = staticCompositionLocalOf { true }

/** The radar sweep, flashes and reveals stop for screenshots. */
object Still { var on = false }

@Composable
fun tr(ru: String, en: String) = if (LocalRu.current) ru else en

@Composable
fun FieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = C.red, background = C.case, surface = C.paper, onSurface = C.ink), content = content)
}
