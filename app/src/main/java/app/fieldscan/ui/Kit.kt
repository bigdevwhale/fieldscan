package app.fieldscan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A plain click without the ripple: the e-paper doesn't ripple. */
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(enabled = enabled, interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)
)

@Composable
fun T(text: String, style: TextStyle = Ty.body, modifier: Modifier = Modifier, color: Color = Color.Unspecified, center: Boolean = false, lines: Int = Int.MAX_VALUE) =
    Text(text, modifier, style = style, color = if (color == Color.Unspecified) style.color else color, textAlign = if (center) TextAlign.Center else null,
        maxLines = lines, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)

/** A framed box: solid, dashed or inverted. */
@Composable
fun Frame(modifier: Modifier = Modifier, dashed: Boolean = false, inverted: Boolean = false, red: Boolean = false, pad: Dp = 9.dp, content: @Composable () -> Unit) {
    val line = if (red) C.red else C.ink
    Box(
        modifier
            .fillMaxWidth()
            .then(if (inverted) Modifier.background(C.ink) else Modifier)
            .drawBehind {
                val w = 2.dp.toPx()
                drawRect(line, topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                    style = Stroke(w, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(7f, 6f)) else null))
            }
            .padding(pad)
    ) { content() }
}

/** The device's flat button: ink with a paper shadow; a disabled one is just a dashed outline. */
@Composable
fun Btn(text: String, modifier: Modifier = Modifier, outline: Boolean = false, red: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    if (!enabled) {
        Box(modifier.fillMaxWidth().drawBehind {
            val w = 2.dp.toPx()
            drawRect(C.paperD, topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f))))
        }.padding(horizontal = 10.dp, vertical = 9.dp), contentAlignment = Alignment.Center) { T(text, Ty.body.copy(color = C.muted), center = true) }
        return
    }
    val bg = when { outline -> C.paper; red -> C.red; else -> C.ink }
    // red always carries light text: in the dark theme the paper is dark and would sink into it
    val fg = when { outline -> C.ink; red -> Color(0xFFF3EFE2); else -> C.paper }
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind { drawRect(C.paperD, topLeft = Offset(3.dp.toPx(), 3.dp.toPx())) }
            .background(bg)
            .border(BorderStroke(2.dp, if (red) C.red else C.ink))
            .tap(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, Ty.body.copy(color = fg), center = true) }
}

@Composable
fun Tag(text: String, red: Boolean = false) {
    val c = if (red) C.red else C.ink
    Box(Modifier.border(1.dp, c).padding(horizontal = 4.dp, vertical = 0.dp)) { T(text, Ty.small.copy(color = c)) }
}

/** Five little cells of signal strength. */
@Composable
fun Signal(n: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) Box(Modifier.size(5.dp, 11.dp).border(1.dp, C.ink).then(if (i <= n) Modifier.background(C.ink) else Modifier))
    }
}

/** Memory as a row of cells: filled, worn (red), empty. */
@Composable
fun MemCells(used: Int, worn: Int, cap: Int, cell: Dp = 5.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        for (i in 0 until cap) {
            val m = Modifier.size(cell, cell * 1.8f)
            when {
                i < used - worn -> Box(m.background(C.ink))
                i < used -> Box(m.background(C.red))
                else -> Box(m.border(1.dp, C.ink))
            }
        }
    }
}

@Composable
fun Rule() = Canvas(Modifier.fillMaxWidth().height(14.dp)) {
    drawLine(C.ink.copy(alpha = .5f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
}

/** A riddle or rumour: text with a red bar. */
@Composable
fun QuoteBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().drawBehind { drawRect(C.red, size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)) }.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) { T(text) }
}

@Composable
fun Gap(h: Dp = 8.dp) = Spacer(Modifier.height(h))

/** A dashed 2 dp outline: something not there yet. */
fun Modifier.dashedBorder(color: Color): Modifier = this.drawBehind {
    val w = 2.dp.toPx()
    drawRect(color, topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f))))
}

/** A full-width way back at the top of a screen that sits over a tab: big enough to hit with a thumb. */
@Composable
fun BackBar(label: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().tap(onClick = onBack).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).border(2.dp, C.ink), contentAlignment = Alignment.Center) { T("‹", Ty.body.copy(color = C.ink)) }
        Spacer(Modifier.width(8.dp))
        T(label, Ty.body)
    }
}

/** A section's label in a long screen: red caps, a count, and a rule to the edge. */
@Composable
fun Section(label: String, count: Int? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        T(label.uppercase() + (count?.let { " · $it" } ?: ""), Ty.small.copy(color = C.red))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(2.dp).background(C.ink.copy(alpha = .35f)))
    }
}

/** The mark of a row that opens something. */
@Composable
fun Chevron() = T("›", Ty.head.copy(color = C.ink))

/** An arrow pointing the way: [deg] clockwise from the top of the screen. */
@Composable
fun Arrow(deg: Float, modifier: Modifier = Modifier, color: Color = C.ink) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2
        val rad = Math.toRadians(deg.toDouble())
        fun at(a: Double, k: Float) = Offset(c.x + kotlin.math.sin(rad + a).toFloat() * r * k, c.y - kotlin.math.cos(rad + a).toFloat() * r * k)
        val path = androidx.compose.ui.graphics.Path().apply {
            val tip = at(0.0, .95f); moveTo(tip.x, tip.y)
            val l = at(2.5, .8f); lineTo(l.x, l.y)
            val m = at(Math.PI, .35f); lineTo(m.x, m.y)
            val rr = at(-2.5, .8f); lineTo(rr.x, rr.y); close()
        }
        drawPath(path, color)
    }
}

@Composable
fun Labeled(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        T(label, Ty.small, Modifier.width(96.dp))
        T(value, Ty.body, Modifier.weight(1f))
    }
}
