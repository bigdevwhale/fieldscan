package app.fieldscan.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fieldscan.BuildConfig
import app.fieldscan.Fix
import app.fieldscan.container
import app.fieldscan.domain.Catch
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Moment
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Scanner
import app.fieldscan.domain.Sky
import app.fieldscan.domain.Trace
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Tab { SCAN, BEST, RUM, DEV }

/** What sits on top of the tab. */
sealed interface Over {
    data class Rite(val trace: Trace) : Over
    data class Shot(val catch: Catch) : Over
    data class Card(val creature: String) : Over
    data class Riddle(val creature: String) : Over
}

/** The whole device: brand strip, e-paper screen, four hardware buttons. */
@Composable
fun Root(onAskLocation: () -> Unit, startTab: Tab = Tab.SCAN, startOver: Over? = null, clock: Long? = null) {
    val ctx = LocalContext.current
    val co = ctx.container
    val state by co.state.collectAsStateWithLifecycle()
    val fix by co.fix.collectAsStateWithLifecycle()
    val weather by co.weather.collectAsStateWithLifecycle()
    val places by co.places.collectAsStateWithLifecycle()
    val hosts by co.hosts.collectAsStateWithLifecycle()
    val tuning by co.tuning.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(startTab) }
    var over by remember { mutableStateOf(startOver) }
    var help by remember { mutableStateOf<Tip?>(null) }
    var askExit by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(clock ?: System.currentTimeMillis()) }
    LaunchedEffect(clock) { if (clock == null) while (true) { delay(15_000); now = System.currentTimeMillis() } }

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (state.theme) { 1 -> false; 2 -> true; else -> systemDark }
    if (C.dark != dark) C.dark = dark
    val moment = Moment(now, fix?.at, weather, state)
    val traces = remember(now / 15_000, fix?.at, places, hosts, state, weather) { Scanner.traces(moment, places, hosts) }
    LaunchedEffect(traces) { co.know(traces.filter { !it.caught }.map { it.creature.id }) }

    Column(Modifier.fillMaxSize().background(C.case).statusBarsPadding().navigationBarsPadding().padding(horizontal = 12.dp)) {
        Brand()
        Column(Modifier.weight(1f).fillMaxWidth().background(C.caseD).padding(5.dp).background(C.paper)) {
            if (state.home == null) {
                StatusBar(now, fix?.at, weather, state)
                Onboarding(fix, onAskLocation) { co.setHome(it) }
            } else {
                StatusBar(now, fix?.at, weather, state)
                // the manual: its page opens by itself the first time a screen is seen, and from the screen's «?»
                val auto = when (over) { is Over.Rite -> Tip.RITE; null -> when (tab) { Tab.SCAN -> Tip.SCAN; Tab.BEST -> Tip.BEST; Tab.RUM -> Tip.RUM; Tab.DEV -> Tip.DEV }; else -> null }
                val tip = help ?: auto?.takeIf { it.name !in state.tips }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                  androidx.compose.runtime.CompositionLocalProvider(LocalHelp provides { t: Tip -> help = t }) {
                    when (tab) {
                        Tab.SCAN -> ScanScreen(traces, fix, tuning, state, now) {
                            // the demo has no real walk: it steps to the place
                            if (BuildConfig.DEMO) co.fix.value = Fix(it.place.at.offset(200.0, 45.0), 5f, now)
                            over = Over.Rite(it)
                        }
                        Tab.BEST -> BestiaryScreen(state, now, { over = Over.Card(it) }, { over = Over.Riddle(it) })
                        Tab.RUM -> RumoursScreen(state, now)
                        Tab.DEV -> DeviceScreen(state, fix, now)
                    }
                    when (val o = over) {
                        is Over.Rite -> RiteScreen(o.trace, moment, places, onBack = { over = null }) { c -> over = Over.Shot(c) }
                        is Over.Shot -> CaptureScreen(o.catch, now) { tab = Tab.BEST; over = null }
                        is Over.Card -> CardScreen(o.creature, state, now) { over = null }
                        is Over.Riddle -> RiddleScreen(o.creature, state, traces) { over = null }
                        null -> Unit
                    }
                  }
                    val closeTip = { t: Tip -> help = null; if (t.name !in state.tips) co.update { it.copy(tips = it.tips + t.name) } }
                    tip?.let { t -> TipOverlay(t) { closeTip(t) } }
                    // Back: a manual page closes first; any other screen goes to the air; on the air it asks before switching off
                    val activity = androidx.activity.compose.LocalActivity.current
                    androidx.activity.compose.BackHandler {
                        when {
                            askExit -> askExit = false
                            tip != null -> closeTip(tip)
                            over != null || tab != Tab.SCAN -> { over = null; tab = Tab.SCAN }
                            else -> askExit = true
                        }
                    }
                    if (askExit) ExitConfirm(onStay = { askExit = false }) { activity?.finish() }
                }
            }
        }
        Nav(tab, enabled = state.home != null) { tab = it; over = null }
    }
}

@Composable
private fun Brand() {
    val t = rememberInfiniteTransition(label = "led")
    val still = Still.on
    val a by t.animateFloat(1f, .2f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "a")
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).alpha(if (still) 1f else if (a > .3f) 1f else .25f).background(C.red))
        Spacer(Modifier.width(8.dp))
        T(tr("СКАНЕР-7", "FIELDSCAN-7"), Ty.small.copy(color = C.caseText, letterSpacing = 1.5.sp))
        Spacer(Modifier.weight(1f))
        repeat(2) { Box(Modifier.padding(start = 4.dp).size(6.dp).clip(CircleShape).background(C.caseD)) }
    }
}

@Composable
fun StatusBar(now: Long, at: app.fieldscan.domain.Geo?, w: app.fieldscan.domain.Weather, s: GameState) {
    val ru = LocalRu.current
    val sky = when {
        w.fog -> tr("туман", "fog"); w.snow -> tr("снег", "snow"); w.rain -> tr("дождь", "rain"); else -> ""
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            T(SimpleDateFormat("HH:mm", Locale.US).format(Date(now)))
            Spacer(Modifier.width(10.dp))
            T("${Sky.moonGlyph(now)} ${(Sky.moon(now)[1] * 100).toInt()}%")
            Spacer(Modifier.width(10.dp))
            if (w.at > 0) T(Words.temp(w.temp) + if (sky.isNotEmpty()) " $sky" else "")
            Spacer(Modifier.weight(1f))
            T(if (ru) "ПАМ " else "MEM ", Ty.small.copy(color = C.ink))
            MemCells(s.memoryKb, s.inMemory.filter { Rules.worn(it, now) >= 2 }.sumOf { app.fieldscan.domain.Bestiary[it.creature].kb }, s.capacity, 4.dp)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(C.ink))
    }
}

@Composable
private fun Nav(tab: Tab, enabled: Boolean, onTab: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((t, ru, en) in listOf(Triple(Tab.SCAN, "Сканер", "Scanner"), Triple(Tab.BEST, "Бестиарий", "Bestiary"), Triple(Tab.RUM, "Слухи", "Rumours"), Triple(Tab.DEV, "Прибор", "Device"))) {
            val on = t == tab
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(if (on) Color(0xFF4C4F49) else C.caseL)
                    .tap(enabled) { onTab(t) }.padding(top = 7.dp, bottom = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(6.dp).background(if (on) C.red else C.caseD))
                Spacer(Modifier.height(4.dp))
                T(tr(ru, en), Ty.small.copy(color = if (on) Color.White else Color(0xFFB9BBB2)))
            }
        }
    }
}

/** First switch-on: what the device is, location, and where home (the archive) is. */
@Composable
private fun Onboarding(fix: Fix?, onAskLocation: () -> Unit, onHome: (app.fieldscan.domain.Geo) -> Unit) {
    val ctx = LocalContext.current
    val allowed = remember(fix) { androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        T(tr("СКАНЕР-7", "FIELDSCAN-7"), Ty.big)
        T(tr("Полевой прибор для наблюдения аномалий", "A field device for observing anomalies"), Ty.small)
        Gap(14.dp)
        T(tr(
            "В городе живут существа. У каждого своё место и свой способ показаться. Прибор слышит только след — кто там и как его вызвать, разгадываешь ты.",
            "Creatures live in the city. Each has its own place and its own way of showing itself. The device hears only a trace; who it is and how to call it out is yours to work out.",
        ))
        Gap(10.dp)
        T(tr(
            "Пойманные ложатся в память прибора. Память маленькая: сдавай снимки в архив дома.",
            "Catches go into the device's memory. It's small: hand the frames in at the archive at home.",
        ))
        Gap(18.dp)
        if (!allowed) {
            T(tr("Прибору нужно знать, где ты стоишь. Координаты не покидают телефон.", "The device needs to know where you stand. Coordinates never leave the phone."), Ty.small)
            Gap(8.dp)
            Btn(tr("Разрешить геолокацию", "Allow location")) { onAskLocation() }
        } else if (fix == null) {
            T(tr("Ищу спутники…", "Looking for satellites…"))
        } else {
            T(tr("Отметь дом: сюда ты будешь сдавать снимки.", "Mark home: this is where you'll hand in frames."))
            T(tr("Точность ${fix.acc.toInt()} м", "Accuracy ${fix.acc.toInt()} m"), Ty.small)
            Gap(8.dp)
            Btn(tr("Я дома — здесь архив", "I'm home — the archive is here"), red = true) { onHome(fix.at) }
            Gap(6.dp)
            T(tr("Можно отметить позже из любого места: Прибор → Перенести архив.", "You can move it later: Device → Move the archive."), Ty.small)
        }
    }
}

/** The thin red strip that slides down a frame while it's being written. */
@Composable
fun ScanLine(frac: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val y = size.height * frac
        drawRect(C.red, topLeft = Offset(0f, y - 1.5f), size = androidx.compose.ui.geometry.Size(size.width, 3f))
    }
}

val DEMO = BuildConfig.DEMO

/** "Switch the device off?" before leaving the app from the air. */
@Composable
private fun ExitConfirm(onStay: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(C.scrim).tap(onClick = onStay), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp).fillMaxWidth().background(C.paper).border(2.dp, C.ink).tap { }.padding(16.dp)) {
            T(tr("Выключить прибор?", "Switch the device off?"), Ty.head)
            Gap(6.dp)
            T(tr("Пройденные шаги обряда прибор помнит ещё два часа.", "Rite steps already done are kept for two more hours."), Ty.small)
            Gap(14.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(tr("Остаться", "Stay"), Modifier.weight(1f), outline = true, onClick = onStay)
                Btn(tr("Выйти", "Exit"), Modifier.weight(1f), red = true, onClick = onExit)
            }
        }
    }
}
