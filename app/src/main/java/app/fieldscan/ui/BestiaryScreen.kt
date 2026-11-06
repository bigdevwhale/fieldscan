package app.fieldscan.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Creature
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Tier
import app.fieldscan.domain.Trace
import app.fieldscan.render.Frames
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BestiaryScreen(state: GameState, now: Long, onCard: (String) -> Unit, onRiddle: (String) -> Unit) {
    val ru = LocalRu.current
    var tier by rememberSaveable { mutableStateOf(Tier.DWELLER) }
    val caught = state.caughtIds
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Title(tr("Бестиарий", "Bestiary"), Tip.BEST, Modifier.weight(1f))
            T(tr("поймано ", "caught ") + "${caught.size} " + tr("из", "of") + " ${Bestiary.all.size}", Ty.small)
        }
        Gap(6.dp)
        // one line of kinds that slides sideways, instead of a ragged two-line wrap
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (t in Tier.entries) {
                val n = Bestiary.all.count { it.tier == t && it.id in caught }
                val on = t == tier
                Box(Modifier.border(2.dp, C.ink).background(if (on) C.ink else C.paper).tap { tier = t }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    T("${t.t.of(ru)} $n", Ty.small.copy(color = if (on) C.paper else C.ink))
                }
            }
        }
        Gap(6.dp)
        val list = Bestiary.all.filter { it.tier == tier }
        T("${tier.t.of(ru)}: ${list.count { it.id in caught }} ${tr("из", "of")} ${list.size}. ${tier.about.of(ru)}", Ty.small)
        Gap(8.dp)
        val cols = 4
        list.chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { cr -> Cell(cr, state, now, Modifier.weight(1f).fillMaxHeight(), onCard, onRiddle) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Cell(cr: Creature, state: GameState, now: Long, modifier: Modifier, onCard: (String) -> Unit, onRiddle: (String) -> Unit) {
    val ru = LocalRu.current
    val c = state.firstOf(cr.id)
    val known = cr.id in state.known
    val num = Bestiary.all.filter { it.tier == cr.tier }.indexOf(cr) + 1
    Column(
        modifier.then(if (c != null || known) Modifier.border(2.dp, if (c != null) C.ink else C.muted) else Modifier.dashedBorder(C.paperD)).tap(enabled = c != null || known) { if (c != null) onCard(cr.id) else onRiddle(cr.id) }.padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            c != null -> FrameImage(rememberFrame(c, now, thumb = true), square = true)
            known -> { val bmp = remember(cr.id) { Frames.unknown(cr.id).asImageBitmap() }; Image(bmp, null, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop) }
            else -> Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) { T("?", Ty.head.copy(color = C.paperD)) }
        }
        // always two lines high, hyphenated, so every cell in a row is the same size
        val caption = when { c != null -> cr.name.of(ru); known -> tr("загадка ›", "riddle ›"); else -> "%03d".format(num) }
        // words never break: a long word makes the caption a little smaller instead
        val longest = caption.split(' ').maxOf { it.length }
        val size = when { longest <= 10 -> 14; longest <= 12 -> 12; else -> 11 }
        androidx.compose.material3.Text(
            caption,
            Modifier.fillMaxWidth(),
            style = Ty.small.copy(
                color = when { c != null -> C.ink; known -> C.red; else -> C.paperD }, fontSize = size.sp, lineHeight = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            ),
            minLines = 2, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/** A caught creature's card: the full frame and what's known. */
@Composable
fun CardScreen(id: String, state: GameState, now: Long, onBack: () -> Unit) {
    val ru = LocalRu.current
    val cr = Bestiary[id]
    val c = state.firstOf(id) ?: return
    val img = rememberFrame(c, now, thumb = false)
    val all = state.catches.filter { it.creature == id }
    Column(Modifier.fillMaxSize().background(C.paper).verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp)) {
        BackBar(tr("Бестиарий", "Bestiary"), onBack)
        T(cr.name.of(ru), Ty.head)
        Row { Tag(Words.tier(cr.tier, ru)); Spacer(Modifier.width(4.dp)); Tag("${cr.kb} " + tr("КБ", "KB")); if (c.archived == null) { Spacer(Modifier.width(4.dp)); Tag(tr("в памяти", "in memory"), red = true) } }
        Gap(8.dp)
        Box(Modifier.border(2.dp, C.ink)) { FrameImage(img) }
        Gap(8.dp)
        val fmt = SimpleDateFormat(if (ru) "d MMM · HH:mm" else "MMM d · HH:mm", if (ru) Locale("ru") else Locale.US)
        Labeled(tr("Ареал", "Habitat"), Words.habitatOf(cr, ru))
        Labeled(tr("Пойман", "Caught"), c.placeName.ifBlank { tr("безымянное место", "a nameless place") })
        Labeled(tr("Когда", "When"), fmt.format(Date(c.time)) + " · " + Words.part(c.part, ru) + " · " + Words.temp(c.temp) +
            (if (c.fog) tr(" · туман", " · fog") else "") + (if (c.snow) tr(" · снег", " · snow") else "") + (if (c.rain) tr(" · дождь", " · rain") else ""))
        Labeled(tr("Обряд", "Rite"), cr.steps.joinToString("; ") { Words.step(it, cr.habitat, ru, false).replaceFirstChar { ch -> ch.lowercase() } })
        if (all.size > 1) Labeled(tr("Снимков", "Frames"), all.size.toString())
        if (c.archived == null) {
            val w = Rules.worn(c, now)
            Labeled(tr("Кадр", "Frame"), if (w == 0) tr("свежий — сдай в архив, пока не испортился", "fresh — hand it in before it wears") else tr("портится: $w из 4", "wearing: $w of 4"))
        }
        Gap(6.dp)
        T(tr("Слух", "Rumour"), Ty.small)
        QuoteBox(cr.rumour.of(ru))
    }
}

/** A creature not yet caught: a dark frame with eyes and its riddle. */
@Composable
fun RiddleScreen(id: String, state: GameState, traces: List<Trace>, onBack: () -> Unit) {
    val ru = LocalRu.current
    val cr = Bestiary[id]
    val bmp = remember(id) { Frames.unknown(id).asImageBitmap() }
    val near = traces.firstOrNull { it.creature.id == id }
    Column(Modifier.fillMaxSize().background(C.paper).verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp)) {
        BackBar(tr("Бестиарий", "Bestiary"), onBack)
        T(tr("Неизвестный", "Unknown"), Ty.head)
        Row { Tag(Words.tier(cr.tier, ru)); Spacer(Modifier.width(4.dp)); Tag(tr("не пойман", "not caught")) }
        Gap(8.dp)
        Image(bmp, null, Modifier.fillMaxWidth().aspectRatio(1.4f).border(2.dp, C.ink), contentScale = ContentScale.Crop)
        Gap(8.dp)
        QuoteBox(cr.riddle.of(ru))
        Gap(6.dp)
        Labeled(tr("Где искать", "Where"), Words.habitatOf(cr, ru))
        if (near != null) Labeled(tr("След", "Trace"), Words.dist(near.dist, ru) + " · " + app.fieldscan.domain.Geo.side(near.bearing, ru))
        else T(tr("Сканер подсветит след, когда окажешься рядом с подходящим местом.", "The scanner will show a trace when you're near a fitting place."), Ty.small)
        if (state.items.isNotEmpty()) { Gap(6.dp); T(tr("С собой: ", "Carrying: ") + state.items.joinToString(", ") { it.t.of(ru) }, Ty.small) }
    }
}
