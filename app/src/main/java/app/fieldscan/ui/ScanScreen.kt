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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import app.fieldscan.sense.Sfx
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fieldscan.container
import app.fieldscan.sense.rememberHeading
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

/** How near a trace counts as "you're here": the popup and the red hint. */
const val NEAR = 120.0

@Composable
fun ScanScreen(traces: List<Trace>, fix: Fix?, tuning: Tuning, state: GameState, now: Long, finds: List<Pair<app.fieldscan.domain.Find, app.fieldscan.domain.Geo>> = emptyList(), dark: Boolean = true, onDevice: () -> Unit = {}, onJournal: () -> Unit = {}, onGuideOff: () -> Unit = {}, onTrace: (Trace) -> Unit) {
    val ru = LocalRu.current
    val still = Still.on
    val t = rememberInfiniteTransition(label = "sweep")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "s")
    val angle = if (still) 70f else sweep
    // the dial turns with the phone, so a dot ahead on the screen is ahead in the street; a tap holds north up
    val sensed by rememberHeading(fix?.at)
    val forced by LocalContext.current.container.headingOverride.collectAsStateWithLifecycle()
    var northUp by remember { mutableStateOf(false) }
    val heading = (forced ?: sensed)?.takeIf { !still }
    val turn = if (northUp) 0f else heading ?: 0f
    // the air hisses louder the nearer an awake trace is; the sweep pings as it passes one
    val loud = traces.filter { it.open && !it.caught }.maxOfOrNull { it.strength } ?: 0
    DisposableEffect(Unit) { onDispose { Sfx.level = 0f } }
    LaunchedEffect(loud) { Sfx.level = 1f + loud * .8f }
    val blips by rememberUpdatedState(traces.map { (it.bearing - turn).toFloat() to it })
    var lastSweep by remember { mutableFloatStateOf(-1f) }
    LaunchedEffect(sweep.toInt() / 6) {
        if (still) return@LaunchedEffect
        val from = lastSweep; lastSweep = sweep
        if (from < 0f) return@LaunchedEffect
        val span = ((sweep - from) % 360 + 360) % 360
        if (span > 90f) return@LaunchedEffect
        for ((a, tr) in blips) {
            val d = ((a - from) % 360 + 360) % 360
            if (d <= span) Sfx.ping(tr.strength, tr.strength >= 4 && tr.open && !tr.caught)
        }
    }
    // the dial zooms to the traces: 500 m, 1, 2 or the full range
    val far = traces.maxOfOrNull { it.dist } ?: state.radius
    val zoom = listOf(500.0, 1000.0, 2000.0, state.radius).first { it >= far * 1.15 || it == state.radius }
    val guided = Guide.stage(state) != Guide.Stage.DONE
    val open = { x: Trace -> Sfx.tune(); onTrace(x) }
    Column(Modifier.fillMaxSize()) {
        // the title stays put while the list scrolls, so it's always clear which screen this is
        Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Title(tr("Эфир", "The air"), Tip.SCAN, Modifier.weight(1f))
            Bars(angle)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp).padding(bottom = 8.dp)) {
            // first what to do now: the guide's step on the first round, then one card with the next thing
            if (guided) GuideBanner(state, traces, onDevice, onOpen = open, onOff = onGuideOff)
            else NowCard(traces, finds, fix, heading, dark, ru, open)
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                Radar(traces, angle, zoom, turn, fix?.at?.let { me -> finds.map { (_, g) -> me.bearing(g) to me.dist(g) } } ?: emptyList(),
                    fix?.at?.let { me -> state.stops.filter { !it.done }.map { me.bearing(it.at) to me.dist(it.at) } } ?: emptyList(), Modifier.size(200.dp).tap { northUp = !northUp })
            }
            Legend(finds.isNotEmpty(), state.stops.any { !it.done } && !guided, "⌀ " + Words.dist(zoom, ru) + if (heading == null || northUp) tr(" · С↑", " · N↑") else "")
            val line = when {
                fix == null -> tr("Ищу спутники…", "Looking for satellites…")
                tuning.busy -> tr("Настройка на эфир: карта ${tuning.done}/${tuning.total}", "Tuning in: map ${tuning.done}/${tuning.total}")
                tuning.failed -> tr("Нет связи с картой — слышу только ближние следы", "No map connection — only nearby traces")
                else -> null
            }
            line?.let { T(it, Ty.small, Modifier.fillMaxWidth(), center = true) }

            val lead = Guide.lead(state, traces)
            val top = if (guided) null else nowTrace(traces)
            Section(tr("Следы рядом", "Traces nearby"), traces.count { it.open && !it.caught }.takeIf { traces.isNotEmpty() })
            for (x in traces) if (x.key != top?.key) TraceRow(x, ru, heading, lead = x.key == lead?.key) { open(x) }
            if (traces.isEmpty() && fix != null && !tuning.busy) Frame(dashed = true) {
                T(tr("Рядом тихо. Попробуй дойти до воды, к мостам или дождаться заката.", "It's quiet nearby. Try walking to water or bridges, or wait for sunset."), Ty.small)
            }

            // the story: the journal's task (it lives in the journal, here only a way to it) and the finds out in the street
            val task = app.fieldscan.domain.Episodes.current(state)?.takeIf { !guided && !app.fieldscan.domain.Episodes.done(state) && it.task !is app.fieldscan.domain.Task.Find }
            if (task != null || finds.isNotEmpty()) {
                Section(tr("Сюжет", "Story"))
                task?.let { e ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).border(2.dp, C.red).tap(onClick = onJournal).padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(tr("Задание журнала", "The journal's task"), Ty.small.copy(color = C.red))
                            T(e.say.of(ru), Ty.body)
                        }
                        Chevron()
                    }
                }
                for ((f, at) in finds) FindRow(f, at, fix, heading, dark, ru)
            }
            if (!guided && state.stops.isNotEmpty()) {
                Section(tr("Обход недели", "Round of the week"))
                WeekCard(state, fix, now, ru)
            }
        }
    }
}

/** The one thing worth doing next, on top of the air: a trace you're standing at, the nearest awake one, or a find. */
@Composable
private fun NowCard(traces: List<Trace>, finds: List<Pair<app.fieldscan.domain.Find, Geo>>, fix: Fix?, heading: Float?, dark: Boolean, ru: Boolean, onOpen: (Trace) -> Unit) {
    val next = nowTrace(traces)
    val here = next?.takeIf { it.dist <= NEAR }
    val find = finds.firstOrNull { !it.first.night || dark }
    @Composable
    fun card(label: String, title: String, dist: Double?, bearing: Double?, note: String?, button: String?, onClick: () -> Unit) {
        Column(Modifier.fillMaxWidth().padding(bottom = 6.dp).border(3.dp, C.red).background(C.paper).padding(10.dp)) {
            T(label, Ty.small.copy(color = C.red))
            T(title, Ty.head, lines = 2)
            if (dist != null && bearing != null) Where(dist, bearing, heading, ru, big = true)
            note?.let { T(it, Ty.small) }
            button?.let { Gap(8.dp); Btn(it, red = true, onClick = onClick) }
        }
    }
    when {
        next != null -> card(
            if (here != null) tr("ТЫ НА МЕСТЕ", "YOU'RE THERE") else tr("БЛИЖАЙШИЙ ЖИВОЙ СЛЕД", "NEAREST AWAKE TRACE"),
            placeTitle(next, ru), next.dist, next.bearing,
            "«" + next.creature.hint.of(ru) + "»",
            if (here != null) tr("Начать обряд", "Start the rite") else tr("Открыть обряд", "Open the rite"),
        ) { onOpen(next) }
        find != null && fix != null -> card(tr("СЮЖЕТ", "STORY"), find.first.title.of(ru), fix.at.dist(find.second), fix.at.bearing(find.second), find.first.hint.of(ru), null) {}
        traces.isNotEmpty() -> card(tr("СЕЙЧАС", "NOW"), tr("Все рядом спят", "Everyone nearby is asleep"), null, null,
            tr("Загляни в сумерках или ночью.", "Look in at dusk or at night."), null) {}
        else -> Unit
    }
}

/** The trace the top card shows: one you're standing at, else the nearest awake one. */
private fun nowTrace(traces: List<Trace>): Trace? {
    val awake = traces.filter { it.open && !it.caught }
    return awake.firstOrNull { it.dist <= NEAR } ?: awake.minByOrNull { it.dist }
}

/** A place's name as a row shows it. */
@Composable
private fun placeTitle(t: Trace, ru: Boolean) =
    if (t.creature.tier == Tier.LEGEND) tr("Точка слияния", "Confluence") else t.place.habitat.name(ru).let { if (t.place.name.isNotBlank()) "$it · ${t.place.name}" else it }

/** Distance and the way there: an arrow (turned with the phone when there's a compass), metres, side of the world. */
@Composable
fun Where(dist: Double, bearing: Double, heading: Float?, ru: Boolean, big: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Arrow((bearing - (heading ?: 0f)).toFloat(), Modifier.size(if (big) 18.dp else 14.dp))
        Spacer(Modifier.width(6.dp))
        T(Words.dist(dist, ru), if (big) Ty.head else Ty.body)
        Spacer(Modifier.width(6.dp))
        T(Geo.side(bearing, ru) + (heading?.let { " · " + Words.rel(bearing, it.toDouble(), ru) } ?: ""), Ty.small)
    }
}

/** What the marks on the dial mean. */
@Composable
private fun Legend(story: Boolean, round: Boolean, scale: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        @Composable
        fun item(label: String, draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
            Canvas(Modifier.size(10.dp), onDraw = draw)
            Spacer(Modifier.width(4.dp))
            T(label, Ty.small)
            Spacer(Modifier.width(12.dp))
        }
        val ink = C.ink; val red = C.red
        item(tr("след", "trace")) { drawRect(ink, Offset(size.width * .2f, size.height * .2f), Size(size.width * .6f, size.height * .6f)) }
        item(tr("рядом", "near")) { drawRect(red, Offset(size.width * .1f, size.height * .1f), Size(size.width * .8f, size.height * .8f)) }
        if (story) item(tr("сюжет", "story")) {
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(size.width / 2, 0f); lineTo(size.width, size.height / 2); lineTo(size.width / 2, size.height); lineTo(0f, size.height / 2); close() }
            drawPath(p, red)
        }
        if (round) item(tr("обход", "round")) { drawRect(ink, Offset.Zero, size, style = Stroke(2.dp.toPx())) }
        T(scale, Ty.small)
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
private fun Radar(traces: List<Trace>, sweep: Float, radius: Double, turn: Float, story: List<Pair<Double, Double>>, round: List<Pair<Double, Double>>, modifier: Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 - 4.dp.toPx()
        val ink = C.ink
        drawCircle(ink, r, c, style = Stroke(2.dp.toPx()))
        val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 5f))
        drawCircle(C.muted, r * .66f, c, style = Stroke(1.5f, pathEffect = dash))
        drawCircle(C.muted, r * .33f, c, style = Stroke(1.5f, pathEffect = dash))
        for (a in 0 until 360 step 30) {
            val rad = Math.toRadians(a.toDouble() - turn)
            val inner = if (a % 90 == 0) r - 12 else r - 6
            drawLine(ink, Offset(c.x + sin(rad).toFloat() * inner, c.y - cos(rad).toFloat() * inner), Offset(c.x + sin(rad).toFloat() * r, c.y - cos(rad).toFloat() * r), 2f)
        }
        // N, where north is now
        val nr = Math.toRadians(-turn.toDouble())
        val n = 7.dp.toPx(); val nd = r - 18.dp.toPx(); val nx = c.x + sin(nr).toFloat() * nd; val ny = c.y - cos(nr).toFloat() * nd
        drawLine(ink, Offset(nx - n / 2, ny + n / 2), Offset(nx - n / 2, ny - n / 2), 3f)
        drawLine(ink, Offset(nx - n / 2, ny - n / 2), Offset(nx + n / 2, ny + n / 2), 3f)
        drawLine(ink, Offset(nx + n / 2, ny + n / 2), Offset(nx + n / 2, ny - n / 2), 3f)
        // sweep with a fading trail (e-paper ghosting)
        for (k in 0 until 26 step 2) {
            val rad = Math.toRadians((sweep - k).toDouble())
            drawLine(if (k < 2) ink else C.paperD, c, Offset(c.x + sin(rad).toFloat() * r, c.y - cos(rad).toFloat() * r), if (k < 2) 2.5f else 2f)
        }
        // where you face: a notch at the top when the dial follows the compass
        if (turn != 0f) {
            val w = 6.dp.toPx()
            drawLine(C.red, Offset(c.x - w, c.y - r - 2), Offset(c.x, c.y - r + w * 1.4f), 3f)
            drawLine(C.red, Offset(c.x + w, c.y - r - 2), Offset(c.x, c.y - r + w * 1.4f), 3f)
        }
        // blips
        for (t in traces) {
            val shown = t.bearing - turn
            val d = ((sweep - shown.toFloat()) % 360 + 360) % 360
            val rad = Math.toRadians(shown)
            val k = (t.dist / radius).coerceIn(.08, 1.0).toFloat()
            val p = Offset(c.x + sin(rad).toFloat() * r * k, c.y - cos(rad).toFloat() * r * k)
            val near = t.strength >= 4 && t.open && !t.caught
            val col = when { d < 60 -> if (near) C.red else ink; d < 160 -> C.paperD; else -> C.paperD.copy(alpha = .5f) }
            val s = if (d < 40) 5.dp.toPx() else 3.5.dp.toPx()
            drawRect(col, Offset(p.x - s / 2, p.y - s / 2), Size(s, s))
        }
        // story finds: a red diamond, always lit
        for ((b, d) in story) {
            val rad = Math.toRadians(b - turn)
            val k = (d / radius).coerceIn(.08, 1.0).toFloat()
            val p = Offset(c.x + sin(rad).toFloat() * r * k, c.y - cos(rad).toFloat() * r * k)
            val s = 6.dp.toPx()
            val path = androidx.compose.ui.graphics.Path().apply { moveTo(p.x, p.y - s); lineTo(p.x + s, p.y); lineTo(p.x, p.y + s); lineTo(p.x - s, p.y); close() }
            drawPath(path, C.red)
        }
        // the week's round: hollow squares
        for ((b, d) in round) {
            val rad = Math.toRadians(b - turn)
            val k = (d / radius).coerceIn(.08, 1.0).toFloat()
            val p = Offset(c.x + sin(rad).toFloat() * r * k, c.y - cos(rad).toFloat() * r * k)
            val s = 9.dp.toPx()
            drawRect(ink, Offset(p.x - s / 2, p.y - s / 2), Size(s, s), style = Stroke(2.dp.toPx()))
        }
        drawRect(ink, Offset(c.x - 3, c.y - 3), Size(6f, 6f))
    }
}

/** The week's round: three places, ticked as they're walked to. */
@Composable
private fun WeekCard(state: GameState, fix: Fix?, now: Long, ru: Boolean) {
    val stops = state.stops
    val done = stops.count { it.done }
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp).border(2.dp, C.ink).padding(horizontal = 8.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(tr("Пройдено $done из ${stops.size}", "$done of ${stops.size} walked"), Ty.body, Modifier.weight(1f))
            val left = app.fieldscan.domain.Weekly.daysLeft(now)
            T(tr("ещё $left ${Words.plural(left, "день", "дня", "дней")}", "$left ${if (left == 1) "day" else "days"} left"), Ty.small)
        }
        if (state.weekPaid) T(tr("Пройден ✓ · +${app.fieldscan.domain.Weekly.PARTS} детали · +${app.fieldscan.domain.Weekly.CHARGE} к заряду. Новый — в понедельник.",
            "Done ✓ · +${app.fieldscan.domain.Weekly.PARTS} parts · +${app.fieldscan.domain.Weekly.CHARGE} charge. A new one on Monday."), Ty.small.copy(color = C.ink))
        else T(tr("Награда: ${app.fieldscan.domain.Weekly.PARTS} детали · ${app.fieldscan.domain.Weekly.CHARGE} шагов заряда",
            "Reward: ${app.fieldscan.domain.Weekly.PARTS} parts · ${app.fieldscan.domain.Weekly.CHARGE} steps of charge"), Ty.small)
        Gap(4.dp)
        for (s in stops) {
            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).border(2.dp, C.ink).background(if (s.done) C.ink else C.paper), contentAlignment = Alignment.Center) {
                    if (s.done) T("✓", Ty.small.copy(color = C.paper))
                }
                Spacer(Modifier.width(8.dp))
                val where = fix?.at?.let { " · " + Words.dist(it.dist(s.at), ru) + " · " + Geo.side(it.bearing(s.at), ru) } ?: ""
                T(s.habitat.name(ru) + (if (s.name.isNotBlank()) " · ${s.name}" else "") + where, Ty.small.copy(color = if (s.done) C.muted else C.ink), Modifier.weight(1f))
            }
        }
    }
}

/** A story find in the list: where it is and what the journal says to look for. */
@Composable
private fun FindRow(f: app.fieldscan.domain.Find, at: app.fieldscan.domain.Geo, fix: Fix?, heading: Float?, dark: Boolean, ru: Boolean) {
    // dashed: it isn't tapped, it's walked to; the device notices by itself
    Frame(Modifier.padding(bottom = 6.dp), dashed = true, red = true) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Diamond()
                Spacer(Modifier.width(6.dp))
                T(f.title.of(ru), Ty.body, Modifier.weight(1f))
            }
            fix?.at?.let { Where(it.dist(at), it.bearing(at), heading, ru) }
            if (f.night && !dark) T(tr("откликнется только после заката", "answers only after sunset"), Ty.small.copy(color = C.red))
            T(f.hint.of(ru), Ty.small.copy(color = C.ink))
        }
    }
}

/** The story's mark, as on the dial. */
@Composable
private fun Diamond() {
    val red = C.red
    Canvas(Modifier.size(12.dp)) {
        val p = androidx.compose.ui.graphics.Path().apply { moveTo(size.width / 2, 0f); lineTo(size.width, size.height / 2); lineTo(size.width / 2, size.height); lineTo(0f, size.height / 2); close() }
        drawPath(p, red)
    }
}

@Composable
private fun TraceRow(t: Trace, ru: Boolean, heading: Float?, lead: Boolean = false, onClick: () -> Unit) {
    val hot = t.strength >= 4 && t.open && !t.caught
    val here = t.open && !t.caught && t.dist <= NEAR
    // asleep or already caught: drawn quieter, so the awake ones stand out
    val quiet = !t.open || t.caught
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).border(if (lead || here) 3.dp else 2.dp, if (lead || here) C.red else if (quiet) C.paperD else C.ink)
            .tap(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).background(if (hot) C.red else C.paper).border(2.dp, if (hot) C.red else if (quiet) C.paperD else C.ink), contentAlignment = Alignment.Center) {
            T(t.creature.habitat.glyph.takeIf { t.creature.tier != Tier.HOMELY } ?: "⌂", Ty.head.copy(color = if (hot) C.paper else if (quiet) C.muted else C.ink))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(placeTitle(t, ru), Ty.body.copy(color = if (quiet) C.muted else C.ink), Modifier.weight(1f, fill = false), lines = 1)
                if (t.creature.tier != Tier.DWELLER) { Spacer(Modifier.width(4.dp)); Tag(Words.tier(t.creature.tier, ru), red = t.creature.tier != Tier.HOMELY) }
                if (lead) LeadMark()
            }
            Where(t.dist, t.bearing, heading, ru)
            // only what differs from the usual is said: you're there, it sleeps, it's caught
            val window = t.creature.conds.filterIsInstance<app.fieldscan.domain.Cond.Window>().firstOrNull()
            when {
                t.caught -> T(tr("уже в бестиарии", "already caught"), Ty.small)
                !t.open -> T(if (window != null) tr("спит · приходи ", "asleep · come ") + Words.parts(window.parts, ru) else tr("спит · ждёт своей погоды", "asleep · waits for its weather"), Ty.small)
                here -> T(tr("ты на месте · нажми и лови", "you're there · tap and catch"), Ty.small.copy(color = C.red))
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.End) {
            Signal(t.strength)
            Chevron()
        }
    }
}

