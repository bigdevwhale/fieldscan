package app.fieldscan.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Daypart
import app.fieldscan.render.Frames
import app.fieldscan.render.Shot
import app.fieldscan.render.TrailCam
import app.fieldscan.sense.Sfx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** One beat of the cold open: a dateline, the lines typed out, maybe a frame from the trail camera. */
private class Beat(val stamp: String, val lines: List<String>, val frame: Boolean = false, val title: Boolean = false)

/**
 * The cold open before the first switch-on, like the start of an episode: night, static, a dateline, a few
 * typed lines about the observer who had the device before, the last frame in its memory, the title card,
 * and the three rules. A tap goes on; «skip» goes straight to setting up.
 */
@Composable
fun Intro(onDone: () -> Unit) {
    val ru = LocalRu.current
    val beats = remember(ru) {
        if (ru) listOf(
            Beat("СЕДЬМОЙ УЧАСТОК · 03:14", listOf("Город спит.", "Фонари горят через один.", "Под мостом что-то шевелится.")),
            Beat("ДАВНО", listOf("Бюро полевых наблюдений раздало двенадцать приборов.", "Двенадцать человек каждую ночь обходили свой участок.", "Они слушали эфир. Они знали, кто живёт рядом.")),
            Beat("ПОСЛЕДНИЙ КАДР В ПАМЯТИ", listOf("Наблюдатель седьмого участка — Н. Ветрова.", "Это последнее, что сняла её фотоловушка."), frame = true),
            Beat("СЕГОДНЯ", listOf("Прибор нашёлся на скамейке у моста.", "Заряд полный. Хозяйка не выходит на связь.", "Он всегда находит того, кто будет ходить.", "Теперь он у тебя.")),
            Beat("", listOf("Они живут рядом.", "Прибор их слышит."), title = true),
            Beat("ПРАВИЛА ОБХОДА", listOf("1. Ходи. Шаги заряжают ловушку.", "2. Слушай эфир. Следы ведут к местам.", "3. Выполни обряд — и сними.", "", "И сдавай кадры домой. Город должен помнить.")),
        ) else listOf(
            Beat("DISTRICT SEVEN · 03:14", listOf("The city is asleep.", "Every other lamp is lit.", "Something is moving under the bridge.")),
            Beat("LONG AGO", listOf("The Bureau of Field Observation handed out twelve devices.", "Twelve people walked their district every night.", "They listened to the air. They knew who lived nearby.")),
            Beat("THE LAST FRAME IN MEMORY", listOf("The observer of district seven: N. Vetrova.", "This is the last thing her trail camera caught."), frame = true),
            Beat("TODAY", listOf("The device turned up on a bench by the bridge.", "Fully charged. Its keeper doesn't answer.", "It always finds someone who will walk.", "Now it has found you.")),
            Beat("", listOf("They live nearby.", "The device can hear them."), title = true),
            Beat("RULES OF THE ROUND", listOf("1. Walk. Steps charge the trap.", "2. Listen to the air. Traces lead to places.", "3. Do the rite, and shoot.", "", "And take the frames home. The city must remember.")),
        )
    }
    var i by rememberSaveable { mutableIntStateOf(0) }
    val beat = beats[i]
    // the whole beat as one text, typed out; a tap while typing shows it all at once
    val full = beat.lines.joinToString("\n")
    var shown by remember(i) { mutableIntStateOf(0) }
    val typing = shown < full.length
    LaunchedEffect(i) {
        delay(if (beat.title) 900 else 500)
        while (shown < full.length) {
            val ch = full[shown]
            shown++
            if (ch != ' ' && ch != '\n' && shown % 2 == 0) Sfx.key()
            delay(if (ch == '\n') 380 else if (ch == '.' || ch == '—') 160 else 34)
        }
    }
    // the air hisses all through the opening
    DisposableEffect(Unit) { Sfx.level = 2.2f; onDispose { Sfx.level = 0f } }
    LaunchedEffect(i) { if (beat.frame || beat.title) Sfx.page() }

    val ink = Color(0xFFE6E6DE)
    Box(
        Modifier.fillMaxSize().background(Color(0xFF0B0C0A)).tap {
            if (typing) shown = full.length else if (i < beats.lastIndex) i++ else onDone()
        },
    ) {
        Grain()
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(C.red))
                Spacer(Modifier.width(8.dp))
                T(if (ru) "ЗАПИСЬ" else "REC", Ty.small.copy(color = C.red))
                Spacer(Modifier.weight(1f))
                if (i < beats.lastIndex) T(tr("пропустить »", "skip »"), Ty.small.copy(color = ink.copy(alpha = .6f)), Modifier.tap { onDone() }.padding(6.dp))
            }
            Spacer(Modifier.weight(if (beat.title) 1f else .5f))
            if (beat.title) TitleCard(ink)
            else {
                if (beat.stamp.isNotEmpty()) {
                    T(beat.stamp, Ty.small.copy(color = C.red, letterSpacing = 2.sp))
                    Gap(14.dp)
                }
                if (beat.frame) { LastFrame(); Gap(14.dp) }
            }
            T(full.take(shown) + if (typing) "▌" else "", Ty.body.copy(color = ink, fontSize = if (beat.title) 20.sp else 18.sp, lineHeight = 26.sp,
                textAlign = if (beat.title) TextAlign.Center else TextAlign.Start), Modifier.fillMaxWidth())
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                beats.indices.forEach { k -> Box(Modifier.size(width = if (k == i) 18.dp else 6.dp, height = 4.dp).background(if (k <= i) ink else ink.copy(alpha = .25f))) }
                Spacer(Modifier.weight(1f))
                if (!typing) T(if (i < beats.lastIndex) tr("нажми ›", "tap ›") else tr("включить прибор ›", "switch on ›"), Ty.small.copy(color = if (i < beats.lastIndex) ink.copy(alpha = .7f) else C.red))
            }
        }
    }
}

/** «FIELDSCAN-7» flickering on like an old tube. */
@Composable
private fun TitleCard(ink: Color) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        for (v in listOf(.8f, .1f, .9f, .3f, 1f, .6f, 1f)) { a.snapTo(v); delay(70) }
    }
    Column(Modifier.fillMaxWidth().alpha(a.value), horizontalAlignment = Alignment.CenterHorizontally) {
        T(tr("СКАНЕР-7", "FIELDSCAN-7"), Ty.big.copy(color = ink, fontSize = 46.sp, letterSpacing = 4.sp))
        Box(Modifier.width(140.dp).height(3.dp).background(C.red))
        Gap(6.dp)
        T(tr("полевой прибор для наблюдения аномалий", "a field device for observing anomalies"), Ty.small.copy(color = ink.copy(alpha = .6f)))
        Gap(28.dp)
    }
}

/** A real trail-camera frame of someone half seen, grown like any catch, developing top-down. */
@Composable
private fun LastFrame() {
    var img by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        img = withContext(Dispatchers.Default) {
            runCatching {
                val cr = Bestiary["most"]
                TrailCam.render(Shot(cr.body, cr.habitat, seed = 7_314L, part = Daypart.NIGHT, fog = true, worn = 2, cam = 7, stamp = "14/10  03:14:07")).bitmap.asImageBitmap()
            }.getOrNull() ?: Frames.unknown("most").asImageBitmap()
        }
    }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(img) { if (img != null) reveal.animateTo(1f, tween(1600, easing = LinearEasing)) }
    Box(Modifier.fillMaxWidth().aspectRatio(TrailCam.W / TrailCam.H.toFloat()).border(1.dp, Color(0xFF3A3C36)).background(Color.Black)) {
        img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * reveal.value
            drawRect(Color.Black, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, size.height - y))
            if (reveal.value < 1f) drawRect(C.red, Offset(0f, y - 2f), androidx.compose.ui.geometry.Size(size.width, 3f))
        }
    }
}

/** Film grain and scan lines over the dark. */
@Composable
private fun Grain() {
    var seed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { if (!Still.on) while (true) { delay(90); seed++ } }
    Canvas(Modifier.fillMaxSize()) {
        val r = Random(seed)
        repeat(260) {
            drawRect(Color.White.copy(alpha = r.nextFloat() * .07f), Offset(r.nextFloat() * size.width, r.nextFloat() * size.height), androidx.compose.ui.geometry.Size(2f, 2f))
        }
        var y = 0f
        while (y < size.height) { drawRect(Color.Black.copy(alpha = .18f), Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, 1.5f)); y += 4f }
    }
}
