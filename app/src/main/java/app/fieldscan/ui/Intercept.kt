package app.fieldscan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import app.fieldscan.sense.Clip
import app.fieldscan.sense.Radio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * A radio intercept on a journal page: ▶ plays the air, the voice and the Morse; the message is decoded on
 * screen letter by letter as it's sent, over a strip of the signal's level. [auto] starts it by itself.
 */
@Composable
fun Intercept(text: String, auto: Boolean = false) {
    var clip by remember(text) { mutableStateOf<Clip?>(null) }
    LaunchedEffect(text) { clip = withContext(Dispatchers.Default) { Radio.clip(text) } }
    var playing by remember(text) { mutableStateOf(false) }
    var started by remember(text) { mutableLongStateOf(0L) }
    var elapsed by remember(text) { mutableLongStateOf(0L) }
    var heard by remember(text) { mutableIntStateOf(0) }
    fun start() { val c = clip ?: return; Radio.play(c); started = System.currentTimeMillis(); elapsed = 0; playing = true }
    LaunchedEffect(clip, auto) { if (auto && clip != null && !playing && heard == 0) start() }
    LaunchedEffect(playing) {
        val c = clip ?: return@LaunchedEffect
        while (playing) {
            elapsed = System.currentTimeMillis() - started
            heard = maxOf(heard, c.marks.count { it <= elapsed })
            if (elapsed >= c.ms) playing = false
            delay(40)
        }
    }
    DisposableEffect(text) { onDispose { Radio.stop() } }

    Column(Modifier.fillMaxWidth().border(1.dp, C.ink).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(tr("ПЕРЕХВАТ", "INTERCEPT"), Ty.small.copy(color = C.red), Modifier.weight(1f))
            val c = clip
            T(if (c == null) "…" else if (playing) "■ " + tr("стоп", "stop") else "▶ " + tr("слушать", "listen") + " · 0:%02d".format(c.ms / 1000),
                Ty.small.copy(color = C.ink), Modifier.tap { if (playing) { Radio.stop(); playing = false } else start() }.padding(4.dp))
        }
        // the signal's level, as it plays
        val c = clip
        Canvas(Modifier.fillMaxWidth().height(26.dp).padding(vertical = 3.dp)) {
            if (c == null) return@Canvas
            val bars = 64
            val per = c.pcm.size / bars
            val upTo = if (playing || heard > 0) (elapsed.toFloat() / c.ms).coerceIn(0f, 1f) else 0f
            val w = size.width / bars
            for (b in 0 until bars) {
                var m = 0
                var i = b * per
                val end = minOf(c.pcm.size, i + per)
                while (i < end) { m = maxOf(m, abs(c.pcm[i].toInt())); i += 37 }
                val h = (m / 32767f * 2.4f).coerceIn(.06f, 1f) * size.height
                val lit = b.toFloat() / bars < upTo
                drawRect(if (lit) C.red else C.paperD, Offset(b * w, (size.height - h) / 2), Size(w * .7f, h))
            }
        }
        // the message, decoded as it comes
        val shown = text.take(heard)
        T(if (heard == 0) tr("— ждёт расшифровки —", "— waiting to be decoded —") else shown + if (heard < text.length && playing) "▌" else "",
            Ty.body.copy(color = if (heard == 0) C.muted else C.ink, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified))
        if (heard > 0 && heard <= text.length) {
            Spacer(Modifier.width(1.dp))
            val code = text.take(heard).takeLast(1).firstOrNull()?.let { Radio.code(it) } ?: ""
            T(code.replace('.', '·').replace('-', '–'), Ty.small.copy(color = C.muted))
        }
    }
}

