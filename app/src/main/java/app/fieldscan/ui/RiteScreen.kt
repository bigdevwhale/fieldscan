package app.fieldscan.ui

import android.Manifest
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fieldscan.BuildConfig
import app.fieldscan.Fix
import app.fieldscan.container
import app.fieldscan.domain.Catch
import app.fieldscan.domain.CircleTrack
import app.fieldscan.domain.ClimbTrack
import app.fieldscan.domain.Pt
import app.fieldscan.domain.WalkTrack
import app.fieldscan.domain.Cond
import app.fieldscan.domain.Facing
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Habitat
import app.fieldscan.domain.Moment
import app.fieldscan.domain.Notes
import app.fieldscan.domain.Place
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Sky
import app.fieldscan.domain.Step
import app.fieldscan.domain.Trace
import app.fieldscan.sense.PitchListener
import app.fieldscan.sense.Sfx
import app.fieldscan.sense.Torch
import app.fieldscan.sense.rememberHeading
import app.fieldscan.sense.rememberLux
import app.fieldscan.sense.rememberMotion
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Steps already done at a place, kept for two hours so a rite can be left and resumed. */
object RiteMemory {
    private val done = HashMap<String, Pair<Int, Long>>()
    fun get(key: String): Int = done[key]?.takeIf { System.currentTimeMillis() - it.second < 2 * 3_600_000 }?.first ?: 0
    fun put(key: String, i: Int) { done[key] = i to System.currentTimeMillis() }
}

@Composable
fun RiteScreen(trace: Trace, m: Moment, places: List<Place>, onBack: () -> Unit, onCaught: (Catch) -> Unit) {
    val ctx = LocalContext.current
    val co = ctx.container
    val ru = LocalRu.current
    val fix by co.fix.collectAsStateWithLifecycle()
    val state by co.state.collectAsStateWithLifecycle()
    val cr = trace.creature
    val place = trace.place
    DisposableEffect(place) { co.riteSpot = place; onDispose { co.riteSpot = null } }
    var index by remember(trace.key) { mutableIntStateOf(RiteMemory.get(trace.key)) }
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }

    val moment = m.copy(state = state, me = fix?.at ?: m.me)
    val ready = moment.ready(cr)
    val steps = cr.steps.map { if (it is Step.Note && state.shy) Step.Still(20) else it }
    val full = state.memoryKb + cr.kb > state.capacity
    val pace = Rules.pace(cr)
    val weak = state.charge < pace && !BuildConfig.DEMO
    val vib = remember { ctx.getSystemService(Vibrator::class.java) }
    val advance = rememberUpdatedState {
        runCatching { vib?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)) }
        Sfx.step()
        index++
        RiteMemory.put(trace.key, index)
    }
    val shoot = index >= steps.size && ready && !full && !weak
    // the air thickens as the place comes near
    val near = fix?.at?.dist(place.mid)
    LaunchedEffect(near?.let { (it / 25).toInt() }) { Sfx.level = if (near == null) 1f else 1.5f + 3.5f * (1 - (near / 400.0).coerceIn(0.0, 1.0)).toFloat() }
    DisposableEffect(Unit) { onDispose { Sfx.level = 0f } }
    var fired by remember(trace.key) { mutableStateOf(false) }
    fun caught(photo: android.graphics.Bitmap?) {
        // one press, one frame: a second tap mustn't catch (and spend) twice
        if (fired) return
        fired = true
        Sfx.shutter()
        RiteMemory.put(trace.key, 0)
        onCaught(co.capture(cr.id, place, fix?.at ?: place.at, photo))
    }
    // the demo and the screenshots have no camera to point: the frame is drawn at once
    LaunchedEffect(shoot) { if (shoot && (BuildConfig.DEMO || Still.on)) caught(null) }
    // the trail camera is ready: say so in a way felt through a pocket
    LaunchedEffect(shoot) {
        if (!shoot || BuildConfig.DEMO || Still.on) return@LaunchedEffect
        delay(400)
        Sfx.found()
        runCatching { vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 150, 400), -1)) }
    }

    Column(
        Modifier.fillMaxSize().background(C.paper)
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false); lastTouch = System.currentTimeMillis() } }
            .verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        T(tr("← к эфиру", "← back to the air"), Ty.small, Modifier.tap(onClick = onBack).padding(bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Title(tr("Обряд", "The rite"), Tip.RITE, Modifier.weight(1f))
            Tag(trace.place.habitat.name(ru))
        }
        Gap(4.dp)
        val known = cr.id in state.caughtIds
        if (known) T(cr.name.of(ru), Ty.body.copy(color = C.red))
        QuoteBox(cr.riddle.of(ru))
        Gap(6.dp)
        val sensed by rememberHeading(fix?.at)
        val forced by co.headingOverride.collectAsStateWithLifecycle()
        val heading = forced ?: sensed
        fix?.let { f ->
            val d = f.at.dist(place.mid)
            val b = f.at.bearing(place.mid)
            T(tr("до места ", "to the place ") + Words.dist(d, ru) + " · " + Geo.side(b, ru) +
                (heading?.let { h -> " · " + Words.rel(b, h.toDouble(), ru) } ?: "") +
                (if (place.name.isNotBlank()) " · ${place.name}" else ""), Ty.small)
        } ?: T(tr("Ищу спутники…", "Looking for satellites…"), Ty.small)
        Gap(8.dp)
        if (cr.conds.isNotEmpty()) {
            for (c in cr.conds) {
                val ok = moment.ok(c)
                Row(Modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(18.dp).border(2.dp, if (ok) C.ink else C.red).background(if (ok) C.ink else C.paper), contentAlignment = Alignment.Center) {
                        T(if (ok) "✓" else "✕", Ty.small.copy(color = if (ok) C.paper else C.red))
                    }
                    Spacer(Modifier.width(8.dp))
                    T(Words.cond(c, ru) + if (!ok && c is Cond.Window) tr(" · сейчас ${Words.part(moment.part, true)}", " · now ${Words.part(moment.part, false)}") else "", Ty.body)
                }
                if (!ok) Words.how(c, ru)?.let { T(it, Ty.small, Modifier.padding(start = 26.dp)) }
            }
            Gap(8.dp)
        }
        Charge(state.charge, pace, weak)
        Gap(8.dp)
        if (!ready) Frame(dashed = true, red = true) {
            T(tr("Пока не выйдет: не все условия. Шаги откроются, когда всё сойдётся.", "It won't come yet: not all conditions are met. The steps open when they are."), Ty.small.copy(color = C.red))
        } else if (full && index >= steps.size) Frame(dashed = true, red = true) {
            T(tr("Память полна. Сдай снимки в архив дома — существо подождёт.", "Memory is full. Hand frames in at home — it will wait."), Ty.small.copy(color = C.red))
        } else if (weak && index >= steps.size) Frame(dashed = true, red = true) {
            T(tr("Обряд пройден, но ловушка не удержит: пройди ещё ", "The rite is done, but the trap won't hold it: walk another ") + Words.steps(pace - state.charge, ru) +
                tr(". Шаги обряда сохранены: когда заряда хватит, откроется фотоловушка.", ". The rite's steps are kept: once the charge is enough, the trail camera opens."), Ty.small.copy(color = C.red))
        } else if (shoot && !BuildConfig.DEMO && !Still.on) Viewfinder(
            // the test build skips steps from home, so it doesn't hold you to the place
            toPlace = near.takeIf { !BuildConfig.DEBUG },
            // a rite may end a walk away from the place (walk 200 m, round it): that's still "there"
            reach = 60.0 + minOf(fix?.acc?.toDouble() ?: 0.0, 40.0) + steps.maxOf { s -> when (s) { is Step.Walk -> s.m.toDouble(); is Step.Circle -> 70.0; else -> 0.0 } },
        ) { caught(it) }
        Gap(6.dp)
        steps.forEachIndexed { i, s ->
            val st = when { i < index -> 2; i == index && ready -> 1; else -> 0 }
            StepCard(i, s, st, place, fix, moment, places, lastTouch, onSkip = { if ((BuildConfig.DEMO || BuildConfig.DEBUG) && i == index) advance.value() }) { if (i == index) advance.value() }
        }
    }
}

@Composable
private fun StepCard(i: Int, s: Step, st: Int, place: Place, fix: Fix?, m: Moment, places: List<Place>, lastTouch: Long, onSkip: () -> Unit, onDone: () -> Unit) {
    val ru = LocalRu.current
    val shy = m.state.shy
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .then(if (st == 0) Modifier.background(C.paper) else Modifier)
            .border(2.dp, if (st == 0) C.paperD else C.ink)
            .pointerInput(st) { detectTapGestures(onLongPress = { onSkip() }) }
            .padding(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).border(2.dp, if (st == 0) C.paperD else C.ink).background(if (st == 2) C.ink else C.paper), contentAlignment = Alignment.Center) {
                T(if (st == 2) "✓" else "${i + 1}", Ty.small.copy(color = if (st == 2) C.paper else if (st == 0) C.paperD else C.ink))
            }
            Spacer(Modifier.width(8.dp))
            T(Words.step(s, place.habitat, ru, shy), Ty.body.copy(color = if (st == 0) C.muted else C.ink), Modifier.weight(1f))
        }
        if (st == 1) {
            Gap(4.dp)
            T(Words.how(if (s is Step.Note && shy) Step.Still(20) else s, ru), Ty.small)
            Gap(6.dp)
            when (s) {
                is Step.Near -> NearStep(s.m, place.at, fix, onDone)
                Step.Middle -> MiddleStep(place, fix, onDone)
                is Step.Face -> FaceStep(s, place, fix, m, onDone)
                is Step.Still -> StillStep(s.sec, onDone)
                is Step.FaceDown -> FaceDownStep(s.sec, onDone)
                is Step.Note -> NoteStep(s.note, onDone)
                is Step.Morse -> MorseStep(s.code, onDone)
                is Step.Dark -> DarkStep(s.sec, m, onDone)
                is Step.Circle -> CircleStep(s, place, fix, onDone)
                is Step.Climb -> ClimbStep(s.m, place, fix, onDone)
                is Step.Wait -> WaitStep(s.min, place, fix, onDone)
                is Step.Walk -> WalkStep(s, fix, places, onDone)
                is Step.Hush -> HushStep(s.sec, lastTouch, onDone)
            }
        }
    }
}

/** The trap's charge against what this creature needs. */
@Composable
private fun Charge(have: Int, need: Int, weak: Boolean) {
    val ru = LocalRu.current
    Column(Modifier.fillMaxWidth().border(2.dp, if (weak) C.red else C.ink).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(tr("Заряд ловушки", "Trap charge"), Ty.body, Modifier.weight(1f))
            T("${minOf(have, need)} / $need", Ty.body.copy(color = if (weak) C.red else C.ink))
        }
        Gap(4.dp)
        Progress(have.toFloat() / need, red = weak)
        Gap(3.dp)
        T(if (weak) tr("Ловушка заряжается шагами: пройди ещё ", "The trap charges with steps: walk another ") + Words.steps(need - have, ru) + "."
            else tr("Хватит. Поимка потратит ", "Enough. The catch will spend ") + Words.steps(need, ru) + tr(" из ${have}.", " of $have."), Ty.small)
    }
}

@Composable
private fun Progress(f: Float, red: Boolean = true) {
    Box(Modifier.fillMaxWidth().height(12.dp).border(2.dp, C.ink)) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).height(12.dp).background(if (red) C.red else C.ink))
    }
}

/** Calls [onDone] once when [ok] has held for [holdMs]. */
@Composable
private fun DoneWhen(ok: Boolean, holdMs: Long = 0, onDone: () -> Unit) {
    val cb by rememberUpdatedState(onDone)
    LaunchedEffect(ok) { if (ok) { delay(holdMs); cb() } }
}

@Composable
private fun NearStep(m: Int, at: Geo, fix: Fix?, onDone: () -> Unit) {
    val ru = LocalRu.current
    val d = fix?.at?.dist(at)
    val need = maxOf(m.toDouble(), minOf(fix?.acc?.toDouble() ?: 0.0, 30.0))
    if (d == null) T(tr("Нет координат", "No position"), Ty.small) else {
        T(if (d <= need) tr("На месте.", "You're there.") else tr("Ещё ", "Another ") + Words.dist(d - need, ru), Ty.small)
        Gap(4.dp)
        Progress((1 - ((d - need) / 300.0)).toFloat(), red = false)
    }
    DoneWhen(d != null && d <= need, 600, onDone)
}

@Composable
private fun MiddleStep(place: Place, fix: Fix?, onDone: () -> Unit) {
    val a = place.a; val b = place.b
    if (a == null || b == null) { NearStep(15, place.at, fix, onDone); return }
    val ru = LocalRu.current
    val mid = place.mid
    val d = fix?.at?.dist(mid)
    val need = maxOf(6.0, minOf((fix?.acc ?: 10f).toDouble() * .8, 14.0))
    Canvas(Modifier.fillMaxWidth().height(90.dp).clipToBounds().border(2.dp, C.ink)) {
        val (ax, ay) = a.xy(mid); val (bx, by) = b.xy(mid)
        val me = fix?.at?.xy(mid)
        val span = maxOf(abs(ax - bx), abs(ay - by), 20.0, (me?.let { maxOf(abs(it.first), abs(it.second)) * 2 } ?: 0.0)) * 1.25
        val k = (minOf(size.width, size.height * 2.2f) / span).toFloat()
        fun p(x: Double, y: Double) = Offset(size.width / 2 + x.toFloat() * k, size.height / 2 - y.toFloat() * k)
        // water under the bridge, across it
        place.flow?.let { fl ->
            val r = Math.toRadians(fl)
            val dx = sin(r) * span; val dy = cos(r) * span
            drawLine(C.paperD, p(-dx, -dy), p(dx, dy), 26.dp.toPx())
            // arrow downstream
            val tip = p(dx * .3, dy * .3); val tail = p(dx * .1, dy * .1)
            drawLine(C.muted, tail, tip, 3f)
        }
        drawLine(C.ink, p(ax, ay), p(bx, by), 6.dp.toPx())
        drawCircle(C.red, 5.dp.toPx(), p(0.0, 0.0))
        fix?.at?.xy(mid)?.let { (x, y) -> drawRect(C.ink, p(x, y) - Offset(5f, 5f) * 1.5f, Size(15f, 15f)) }
    }
    Gap(4.dp)
    if (d != null) T(if (d <= need) tr("Середина пролёта.", "The middle of the span.") else tr("До середины ", "To the middle ") + Words.dist(d, ru), Ty.small)
    DoneWhen(d != null && d <= need, 800, onDone)
}

@Composable
private fun FaceStep(s: Step.Face, place: Place, fix: Fix?, m: Moment, onDone: () -> Unit) {
    val ru = LocalRu.current
    val sensed by rememberHeading(fix?.at)
    val forced by LocalContext.current.container.headingOverride.collectAsStateWithLifecycle()
    val heading = forced ?: sensed
    val target = when (s.to) {
        Facing.UPSTREAM -> place.flow?.let { (it + 180) % 360 } ?: 0.0
        Facing.NORTH -> 0.0
        Facing.SOUTH -> 180.0
        Facing.WEST -> fix?.at?.let { Sky.sun(m.now, it)[1] }?.takeIf { it in 200.0..340.0 } ?: 270.0
        Facing.EAST -> 90.0
        Facing.WIND -> m.weather.windFrom
        Facing.TOWARD -> fix?.at?.bearing(place.at) ?: 0.0
    }
    val h = heading
    if (h == null) {
        T(tr("У телефона нет компаса. Повернись ${Words.facing(s.to, true)} сам и нажми.", "No compass on this phone. Turn ${Words.facing(s.to, false)} yourself and press."), Ty.small)
        Gap(6.dp); Btn(tr("Повернулся", "Done"), outline = true, onClick = onDone); return
    }
    val ok = Geo.near(h.toDouble(), target, s.tol.toDouble())
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(96.dp)) {
            val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2 - 4
            drawCircle(C.ink, r, c, style = Stroke(3f))
            for (a in 0 until 360 step 45) { val t = Math.toRadians(a - h.toDouble()); drawLine(C.ink, c + Offset((sin(t) * (r - 8)).toFloat(), (-cos(t) * (r - 8)).toFloat()), c + Offset((sin(t) * r).toFloat(), (-cos(t) * r).toFloat()), 3f) }
            // north mark
            val tn = Math.toRadians(-h.toDouble()); drawCircle(C.ink, 4f, c + Offset((sin(tn) * (r - 14)).toFloat(), (-cos(tn) * (r - 14)).toFloat()))
            // target
            val tt = Math.toRadians(target - h); drawCircle(C.red, 7f, c + Offset((sin(tt) * (r - 4)).toFloat(), (-cos(tt) * (r - 4)).toFloat()))
            // where you look: always up
            drawLine(if (ok) C.red else C.ink, c, c + Offset(0f, -r + 10), 5f)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            T(tr("Цель: ", "Target: ") + Words.facing(s.to, ru) + " (${target.toInt()}°)", Ty.small)
            T(tr("Ты смотришь: ", "You face: ") + "${h.toInt()}° ${Geo.side(h.toDouble(), ru)}", Ty.body.copy(color = if (ok) C.red else C.ink))
            if (s.to == Facing.UPSTREAM && place.flow == null) T(tr("Течения не видно — смотри на север.", "No current here — face north."), Ty.small)
        }
    }
    DoneWhen(ok, 1500, onDone)
}

@Composable
private fun StillStep(sec: Int, onDone: () -> Unit) {
    val motion = rememberMotion()
    var t by remember { mutableFloatStateOf(0f) }
    var broke by remember { mutableStateOf(false) }
    val cb by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        while (t < sec) {
            delay(100)
            if (motion.moving > .45f) { if (t > 1f) { broke = true; Sfx.fail() }; t = 0f } else t += .1f
        }
        cb()
    }
    T(if (broke) tr("Шевельнулся — оно нырнуло обратно. Ещё раз.", "You moved: it ducked back. Again.") else tr("Не двигайся.", "Don't move."), Ty.small.copy(color = if (broke) C.red else C.muted))
    Gap(4.dp); Progress(t / sec)
}

@Composable
private fun FaceDownStep(sec: Int, onDone: () -> Unit) {
    val motion = rememberMotion()
    var t by remember { mutableFloatStateOf(0f) }
    val cb by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) { while (t < sec) { delay(100); if (motion.faceDown) t += .1f }; cb() }
    T(if (motion.faceDown) tr("Лежит. Жди вибрации.", "Lying down. Wait for the buzz.") else tr("Переверни телефон экраном вниз.", "Turn the phone face down."), Ty.small)
    Gap(4.dp); Progress(t / sec)
}

@Composable
private fun NoteStep(note: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val ru = LocalRu.current
    var hz by remember { mutableStateOf<Double?>(null) }
    var hold by remember { mutableFloatStateOf(0f) }
    var allowed by remember { mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    if (!allowed) {
        T(tr("Нужен микрофон: прибор слушает только высоту голоса, ничего не записывает.", "The microphone is needed: the device hears only the pitch and records nothing."), Ty.small)
        Gap(6.dp); Btn(tr("Разрешить микрофон", "Allow the microphone"), outline = true) { ask.launch(Manifest.permission.RECORD_AUDIO) }
        T(tr("Или включи тихий вариант в Приборе.", "Or switch on the quiet way in Device."), Ty.small)
        return
    }
    DisposableEffect(Unit) {
        val l = PitchListener(ctx) { hz = it }
        l.start(); onDispose { l.stop() }
    }
    val cb by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        while (hold < 3f) {
            delay(100)
            val h = hz
            val (n, cents) = if (h != null) Notes.of(h) else (-1 to 0.0)
            if (n == note && abs(cents) < 40) hold += .1f else hold = maxOf(0f, hold - .25f)
        }
        cb()
    }
    val h = hz
    Row(verticalAlignment = Alignment.CenterVertically) {
        T(h?.let { Notes.name(Notes.of(it).first, ru) } ?: "—", Ty.big, Modifier.width(110.dp))
        T(h?.let { "${it.toInt()} " + tr("Гц", "Hz") } ?: tr("пой", "sing"), Ty.small)
    }
    Progress(hold / 3f)
}

@Composable
private fun MorseStep(code: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val torch = remember { Torch(ctx) }
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var down by remember { mutableLongStateOf(0L) }
    var lastUp by remember { mutableLongStateOf(0L) }
    DisposableEffect(Unit) { onDispose { torch.set(false) } }
    val cb by rememberUpdatedState(onDone)
    LaunchedEffect(lastUp) {
        if (lastUp == 0L) return@LaunchedEffect
        delay(1500)
        if (entered == code) cb() else if (entered.isNotEmpty()) { wrong = true; entered = ""; Sfx.fail() }
    }
    T(tr("Короткое нажатие — точка, долгое — тире. Пауза — конец буквы.", "Short press is a dot, long is a dash. A pause ends the letter."), Ty.small)
    Gap(4.dp)
    T((entered.ifEmpty { " " }).replace('.', '·').replace('-', '–'), Ty.big)
    if (wrong) T(tr("Не та буква. Ещё раз.", "Wrong letter. Again."), Ty.small.copy(color = C.red))
    Gap(4.dp)
    Box(
        Modifier.fillMaxWidth().height(56.dp).background(C.ink).pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(); down = System.currentTimeMillis(); torch.set(true); wrong = false
                do { val e = awaitPointerEvent() } while (e.changes.any { it.pressed })
                torch.set(false)
                val len = System.currentTimeMillis() - down
                entered += if (len < 350) "." else "-"
                lastUp = System.currentTimeMillis()
            }
        },
        contentAlignment = Alignment.Center,
    ) { T(if (torch.present) tr("держи — свет", "hold — light") else tr("держи (фонарика нет)", "hold (no torch)"), Ty.body.copy(color = C.paper)) }
}

@Composable
private fun DarkStep(sec: Int, m: Moment, onDone: () -> Unit) {
    val lux by rememberLux()
    var t by remember { mutableFloatStateOf(0f) }
    val cb by rememberUpdatedState(onDone)
    val dark = lux?.let { it < 8f } ?: Sky.dark(m.now, m.me ?: Geo(55.75, 37.62))
    val darkNow by rememberUpdatedState(dark)
    LaunchedEffect(Unit) { while (t < sec) { delay(100); if (darkNow) t += .1f }; cb() }
    T(if (dark) tr("Темно. Жди.", "Dark. Wait.") else tr("Слишком светло: отойди от фонарей.", "Too bright: step away from the lamps."), Ty.small)
    Gap(4.dp); Progress(t / sec)
}

@Composable
private fun CircleStep(s: Step.Circle, place: Place, fix: Fix?, onDone: () -> Unit) {
    val track = remember { CircleTrack(place.at) }
    var got by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fix) { fix?.let { track.add(Pt(it.at, it.acc.toDouble(), it.time)); got = track.rounds(s.ccw).toFloat() } }
    T(tr("Пройдено кругов: ", "Rounds: ") + "%.1f".format(maxOf(0f, got)) + " / ${s.turns}", Ty.small)
    if (got < -.12f) T(tr("Не в ту сторону.", "The wrong way."), Ty.small.copy(color = C.red))
    Gap(4.dp); Progress(got / s.turns)
    DoneWhen(got >= s.turns, 0, onDone)
}

@Composable
private fun ClimbStep(m: Int, place: Place, fix: Fix?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    if (remember { app.fieldscan.sense.hasBarometer(ctx) }) { RiseStep(m, onDone); return }
    // no barometer: the old way, along the stairs on the map by GPS
    val a = place.a; val b = place.b
    if (a == null || b == null) { NearStep(15, place.at, fix, onDone); return }
    val ru = LocalRu.current
    val track = remember { ClimbTrack(a, b) }
    // mirrors of the tracker for the screen
    var started by remember { mutableStateOf(false) }
    var stalled by remember { mutableStateOf(false) }
    var best by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    fun sync() { started = track.started; stalled = track.stalled; best = track.best.toFloat(); done = track.done }
    LaunchedEffect(fix) { fix?.let { track.add(Pt(it.at, it.acc.toDouble(), System.currentTimeMillis())); sync() } }
    LaunchedEffect(Unit) { while (true) { delay(1000); track.tick(System.currentTimeMillis()); sync() } }
    LaunchedEffect(stalled) { if (stalled) Sfx.fail() }
    LineSketch(a, b, fix, endRed = true)
    Gap(4.dp)
    val toBottom = fix?.at?.dist(a)
    T(when {
        stalled && !started -> tr("Остановился. Спустись и начни снизу.", "You stopped. Go down and start again.")
        !started -> tr("Встань у нижнего конца лестницы (чёрная точка)", "Stand at the bottom end of the stairs (the black dot)") +
            (toBottom?.let { " · " + Words.dist(it, ru) } ?: "") + "."
        else -> tr("Вверх, не останавливаясь.", "Up, without stopping.")
    }, Ty.small.copy(color = if (stalled && !started) C.red else C.muted))
    Gap(4.dp); Progress(best)
    DoneWhen(done, 300, onDone)
}

/** The climb by the barometer: metres gained without a stop. */
@Composable
private fun RiseStep(m: Int, onDone: () -> Unit) {
    val ru = LocalRu.current
    val alt by app.fieldscan.sense.rememberAltitude()
    val track = remember { app.fieldscan.domain.RiseTrack(m.toDouble()) }
    var gained by remember { mutableFloatStateOf(0f) }
    var stalled by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    fun feed(a: Double?) { a?.let { track.add(it, System.currentTimeMillis()) }; gained = track.gained.toFloat(); stalled = track.stalled; done = track.done }
    LaunchedEffect(alt) { feed(alt) }
    // the clock runs on even when the height doesn't change, so a stop is noticed
    LaunchedEffect(Unit) { while (true) { delay(1000); feed(alt) } }
    LaunchedEffect(stalled) { if (stalled) Sfx.fail() }
    T(when {
        alt == null -> tr("Барометр просыпается…", "The barometer is waking…")
        stalled -> tr("Остановился — счёт начат заново отсюда. Вверх!", "You stopped: the count starts over from here. Up!")
        gained < .5f -> tr("Начинай подниматься: лестница, горка, этажи — что угодно вверх.", "Start climbing: stairs, a hill, floors, anything upwards.")
        else -> tr("Вверх, не останавливаясь: ", "Up, without stopping: ") + "%.1f".format(gained) + tr(" из $m м", " of $m m")
    }, Ty.small.copy(color = if (stalled) C.red else C.muted))
    Gap(4.dp); Progress(gained / m)
    DoneWhen(done, 300, onDone)
}

/** A line place from above: [a] a black dot, [b] a red one (when [endRed]), you a square; north up. */
@Composable
private fun LineSketch(a: Geo, b: Geo, fix: Fix?, endRed: Boolean) {
    val mid = Geo((a.lat + b.lat) / 2, (a.lon + b.lon) / 2)
    Canvas(Modifier.fillMaxWidth().height(90.dp).clipToBounds().border(2.dp, C.ink)) {
        val (ax, ay) = a.xy(mid); val (bx, by) = b.xy(mid)
        val me = fix?.at?.xy(mid)
        val span = maxOf(abs(ax - bx), abs(ay - by), 20.0, (me?.let { minOf(maxOf(abs(it.first), abs(it.second)) * 2, 400.0) } ?: 0.0)) * 1.3
        val k = (minOf(size.width, size.height * 2.2f) / span).toFloat()
        fun p(x: Double, y: Double) = Offset(size.width / 2 + x.toFloat() * k, size.height / 2 - y.toFloat() * k)
        drawLine(C.paperD, p(ax, ay), p(bx, by), 8.dp.toPx())
        drawCircle(C.ink, 5.dp.toPx(), p(ax, ay))
        drawCircle(if (endRed) C.red else C.ink, 5.dp.toPx(), p(bx, by))
        me?.let { (x, y) -> drawRect(C.ink, p(x, y) - Offset(7.5f, 7.5f), Size(15f, 15f)) }
    }
}

@Composable
private fun WaitStep(min: Int, place: Place, fix: Fix?, onDone: () -> Unit) {
    var t by remember { mutableFloatStateOf(0f) }
    val here = fix?.at?.dist(place.at)?.let { it <= 60 } ?: false
    val hereNow by rememberUpdatedState(here)
    val cb by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) { while (t < min * 60) { delay(1000); if (hereNow) t += 1 }; cb() }
    val left = (min * 60 - t).toInt().coerceAtLeast(0)
    T(if (here) tr("Осталось ", "Left ") + "%d:%02d".format(left / 60, left % 60) else tr("Вернись к месту — время стоит.", "Come back: the clock has stopped."), Ty.small)
    Gap(4.dp); Progress(t / (min * 60))
}

@Composable
private fun WalkStep(s: Step.Walk, fix: Fix?, places: List<Place>, onDone: () -> Unit) {
    val ru = LocalRu.current
    val track = remember(places.isEmpty()) {
        WalkTrack(if (s.water) places.filter { it.habitat == Habitat.WATER || it.habitat == Habitat.PIER || it.habitat == Habitat.BRIDGE }.map { it.at } else null)
    }
    var walked by remember { mutableFloatStateOf(0f) }
    var off by remember { mutableStateOf(false) }
    LaunchedEffect(fix) { fix?.let { track.add(Pt(it.at, it.acc.toDouble(), it.time)); walked = track.metres.toFloat(); off = track.off } }
    T(if (off) tr("Отошёл от воды — шаги не считаются.", "Away from the water: steps don't count.") else Words.dist(walked.toDouble(), ru) + " / " + Words.dist(s.m.toDouble(), ru), Ty.small.copy(color = if (off) C.red else C.muted))
    Gap(4.dp); Progress(walked / s.m)
    DoneWhen(walked >= s.m, 0, onDone)
}

@Composable
private fun HushStep(sec: Int, lastTouch: Long, onDone: () -> Unit) {
    val start = remember { System.currentTimeMillis() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(200); now = System.currentTimeMillis() } }
    val from = maxOf(start, lastTouch)
    val t = (now - from) / 1000f
    T(tr("Не трогай экран. Можно убрать телефон в карман.", "Don't touch the screen. You can pocket the phone."), Ty.small)
    Gap(4.dp); Progress(t / sec)
    DoneWhen(t >= sec, 0, onDone)
}
