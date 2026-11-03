package app.fieldscan.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.fieldscan.Fix
import app.fieldscan.Tuning
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Tier
import app.fieldscan.domain.Trace
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScanScreen(traces: List<Trace>, fix: Fix?, tuning: Tuning, state: GameState, now: Long, onTrace: (Trace) -> Unit) {
    val ru = LocalRu.current
    val still = Still.on
    val t = rememberInfiniteTransition(label = "sweep")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "s")
    val angle = if (still) 70f else sweep
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp)) {
        // the dial zooms to the traces: 500 m, 1, 2 or the full range
        val far = traces.maxOfOrNull { it.dist } ?: state.radius
        val zoom = listOf(500.0, 1000.0, 2000.0, state.radius).first { it >= far * 1.15 || it == state.radius }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Title(tr("Эфир", "The air"), Tip.SCAN, Modifier.weight(1f))
            Bars(angle)
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Radar(traces, angle, zoom, Modifier.size(232.dp))
        }
        val line = when {
            fix == null -> tr("Ищу спутники…", "Looking for satellites…")
            tuning.busy -> tr("Настройка на эфир: карта ${tuning.done}/${tuning.total}", "Tuning in: map ${tuning.done}/${tuning.total}")
            tuning.failed -> tr("Нет связи с картой — слышу только ближние следы", "No map connection — only nearby traces")
            else -> tr("шкала ", "scale ") + Words.dist(zoom, ru) + tr(" · точность ${fix.acc.toInt()} м", " · accuracy ${fix.acc.toInt()} m")
        }
        T(line, Ty.small, Modifier.fillMaxWidth(), center = true)
        Gap(8.dp)
        for (x in traces) TraceRow(x, ru, now) { onTrace(x) }
        if (traces.isEmpty() && fix != null && !tuning.busy) Frame(dashed = true) {
            T(tr("Рядом тихо. Попробуй дойти до воды, к мостам или дождаться заката.", "It's quiet nearby. Try walking to water or bridges, or wait for sunset."), Ty.small)
        }
        Rule()
        T(tr(
            "Сканер показывает только след и тип места. Кто там и как его вызвать — подскажут загадка и слухи.",
            "The scanner shows only a trace and a kind of place. Who's there and how to call it — the riddle and rumours will tell.",
        ), Ty.small)
    }
}

@Composable
private fun Bars(angle: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(18.dp)) {
        for (i in 0 until 8) {
            val v = ((sin(Math.toRadians(angle * 3.0 + i * 47)) + 1) / 2 * (if (i < 5) 1.0 else .5)).toFloat()
            Box(Modifier.width(5.dp).height((3 + v * 14).dp).background(C.ink))
        }
    }
}

@Composable
private fun Radar(traces: List<Trace>, sweep: Float, radius: Double, modifier: Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 - 4.dp.toPx()
        val ink = C.ink
        drawCircle(ink, r, c, style = Stroke(2.dp.toPx()))
        val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 5f))
        drawCircle(C.muted, r * .66f, c, style = Stroke(1.5f, pathEffect = dash))
        drawCircle(C.muted, r * .33f, c, style = Stroke(1.5f, pathEffect = dash))
        for (a in 0 until 360 step 30) {
            val rad = Math.toRadians(a.toDouble())
            val inner = if (a % 90 == 0) r - 12 else r - 6
            drawLine(ink, Offset(c.x + sin(rad).toFloat() * inner, c.y - cos(rad).toFloat() * inner), Offset(c.x + sin(rad).toFloat() * r, c.y - cos(rad).toFloat() * r), 2f)
        }
        // N
        val n = 7.dp.toPx(); val nx = c.x; val ny = c.y - r + 18
        drawLine(ink, Offset(nx - n / 2, ny + n / 2), Offset(nx - n / 2, ny - n / 2), 3f)
        drawLine(ink, Offset(nx - n / 2, ny - n / 2), Offset(nx + n / 2, ny + n / 2), 3f)
        drawLine(ink, Offset(nx + n / 2, ny + n / 2), Offset(nx + n / 2, ny - n / 2), 3f)
        // sweep with a fading trail (e-paper ghosting)
        for (k in 0 until 26 step 2) {
            val rad = Math.toRadians((sweep - k).toDouble())
            drawLine(if (k < 2) ink else C.paperD, c, Offset(c.x + sin(rad).toFloat() * r, c.y - cos(rad).toFloat() * r), if (k < 2) 2.5f else 2f)
        }
        // blips
        for (t in traces) {
            val d = ((sweep - t.bearing.toFloat()) % 360 + 360) % 360
            val rad = Math.toRadians(t.bearing)
            val k = (t.dist / radius).coerceIn(.08, 1.0).toFloat()
            val p = Offset(c.x + sin(rad).toFloat() * r * k, c.y - cos(rad).toFloat() * r * k)
            val near = t.strength >= 4 && t.open && !t.caught
            val col = when { d < 60 -> if (near) C.red else ink; d < 160 -> C.paperD; else -> C.paperD.copy(alpha = .5f) }
            val s = if (d < 40) 5.dp.toPx() else 3.5.dp.toPx()
            drawRect(col, Offset(p.x - s / 2, p.y - s / 2), Size(s, s))
        }
        drawRect(ink, Offset(c.x - 3, c.y - 3), Size(6f, 6f))
    }
}

@Composable
private fun TraceRow(t: Trace, ru: Boolean, now: Long, onClick: () -> Unit) {
    val hot = t.strength >= 4 && t.open && !t.caught
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).border(2.dp, C.ink).tap(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).background(if (hot) C.red else C.paper).border(2.dp, if (hot) C.red else C.ink), contentAlignment = Alignment.Center) {
            T(t.creature.habitat.glyph.takeIf { t.creature.tier != Tier.HOMELY } ?: "⌂", Ty.head.copy(color = if (hot) C.paper else C.ink))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val title = if (t.creature.tier == Tier.LEGEND) tr("Точка слияния", "Confluence") else t.place.habitat.name(ru).let { if (t.place.name.isNotBlank() && it.length < 12) "$it · ${t.place.name}" else it }
                T(title, Ty.body, Modifier.weight(1f, fill = false))
                if (t.creature.tier != Tier.DWELLER) { Spacer(Modifier.width(4.dp)); Tag(Words.tier(t.creature.tier, ru), red = t.creature.tier != Tier.HOMELY) }
                if (t.caught) { Spacer(Modifier.width(4.dp)); Tag(tr("уже в бестиарии", "caught")) }
            }
            val where = Words.dist(t.dist, ru) + " · " + Geo.side(t.bearing, ru)
            val tail = if (!t.open) tr("спит", "asleep") else tr("след: ", "trace: ") + t.creature.hint.of(ru)
            T("$where · $tail", Ty.small)
        }
        Spacer(Modifier.width(6.dp))
        Signal(t.strength)
    }
}

