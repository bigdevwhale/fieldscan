package app.fieldscan.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.fieldscan.container
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Catch
import app.fieldscan.render.Frames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The full frame or the creature's square of a catch, grown in the background. */
@Composable
fun rememberFrame(c: Catch?, now: Long, thumb: Boolean): ImageBitmap? {
    val ctx = LocalContext.current
    var img by remember(c?.id, thumb) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(c?.id, thumb, now / 3_600_000) {
        if (c == null) return@LaunchedEffect
        img = withContext(Dispatchers.Default) {
            val fr = Frames.of(ctx, ctx.container.store.frames, c, now)
            (if (thumb) Frames.thumb(fr) else fr.bitmap).asImageBitmap()
        }
    }
    return img
}

@Composable
fun FrameImage(img: ImageBitmap?, modifier: Modifier = Modifier, square: Boolean = false) {
    Box(modifier.fillMaxWidth().aspectRatio(if (square) 1f else 4f / 3f).background(C.film)) {
        img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

/** The moment of capture: the screen flashes like an e-paper refresh and the frame develops top-down. */
@Composable
fun CaptureScreen(c: Catch, now: Long, onDone: () -> Unit) {
    val ru = LocalRu.current
    val cr = Bestiary[c.creature]
    val img = rememberFrame(c, now, thumb = false)
    var flash by remember { mutableIntStateOf(0) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!Still.on) { for (i in 1..5) { flash = i; delay(150) } }
        flash = 0
    }
    LaunchedEffect(img) { if (img != null) reveal.animateTo(1f, tween(if (Still.on) 1 else 1400, easing = LinearEasing)) }
    val bg = when (flash) { 1, 3 -> C.ink; 5 -> C.red; else -> C.paper }
    Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (flash != 0) return@Column
        Gap(16.dp)
        T(tr("СНИМОК · ЗАПИСЬ В ПАМЯТЬ…", "FRAME · WRITING TO MEMORY…"), Ty.small, center = true)
        Gap(6.dp)
        Box(Modifier.fillMaxWidth().border(2.dp, C.ink)) {
            FrameImage(img, Modifier.drawWithContent {
                val y = size.height * reveal.value
                drawContent()
                drawRect(C.film.copy(alpha = .82f), Offset(0f, y), Size(size.width, size.height - y))
                if (reveal.value < 1f) drawRect(C.red, Offset(0f, y - 2f), Size(size.width, 4f))
            })
        }
        if (reveal.value >= 1f) {
            Gap(10.dp)
            T(cr.name.of(ru), Ty.head, center = true)
            T(tr("запечатан · ${cr.kb} КБ · ", "sealed · ${cr.kb} KB · ") + Words.tier(cr.tier, ru), Ty.small, center = true)
            cr.gives?.let { T(tr("Отдал тебе: ", "Gave you: ") + it.t.of(ru), Ty.small.copy(color = C.red), center = true) }
            Gap(10.dp)
            Frame { Column { T(tr("Слух", "Rumour"), Ty.small); QuoteBox(cr.rumour.of(ru)) } }
            Gap(10.dp)
            Btn(tr("В бестиарий", "To the bestiary"), onClick = onDone)
        }
    }
}
