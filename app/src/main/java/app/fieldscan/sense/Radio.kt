package app.fieldscan.sense

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Radio intercepts for the journal: a stretch of the air with a voice drowned in it, then a message in
 * Morse code (Russian or Latin letters, as written). [marks] is when each character of the text has been
 * sent, in milliseconds from the start, so the screen can decode it along with the sound.
 */
class Clip(val pcm: ShortArray, val text: String, val marks: List<Long>) {
    val ms: Long get() = pcm.size * 1000L / Radio.RATE
}

object Radio {
    const val RATE = 22050
    private const val UNIT = 75 // ms in a Morse dot

    private val latin = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..",
        'J' to ".---", 'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--", 'Z' to "--..",
    )
    private val cyrillic = mapOf(
        'А' to ".-", 'Б' to "-...", 'В' to ".--", 'Г' to "--.", 'Д' to "-..", 'Е' to ".", 'Ё' to ".", 'Ж' to "...-", 'З' to "--..",
        'И' to "..", 'Й' to ".---", 'К' to "-.-", 'Л' to ".-..", 'М' to "--", 'Н' to "-.", 'О' to "---", 'П' to ".--.", 'Р' to ".-.",
        'С' to "...", 'Т' to "-", 'У' to "..-", 'Ф' to "..-.", 'Х' to "....", 'Ц' to "-.-.", 'Ч' to "---.", 'Ш' to "----", 'Щ' to "--.-",
        'Ъ' to "--.--", 'Ы' to "-.--", 'Ь' to "-..-", 'Э' to "..-..", 'Ю' to "..--", 'Я' to ".-.-",
    )
    private val digits = mapOf('0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.")

    fun code(c: Char): String? = c.uppercaseChar().let { latin[it] ?: cyrillic[it] ?: digits[it] }

    /** The whole intercept for [text]: air, a voice under it, the message, air again. */
    fun clip(text: String, seed: Long = text.hashCode().toLong()): Clip {
        val r = Random(seed)
        val out = ArrayList<Short>(RATE * 20)
        var lp = 0f; var lp2 = 0f
        var t = 0L
        fun air(): Float { val w = r.nextFloat() * 2 - 1; lp += (w - lp) * .2f; lp2 += (lp - lp2) * .4f; return lp2 * 2f }
        fun put(v: Float) { out += (v.coerceIn(-1f, 1f) * 32767).toInt().toShort(); t++ }
        fun ms() = out.size * 1000L / RATE

        // the air comes up
        repeat(n(700)) { i -> put(air() * .14f * (i / n(700).toFloat())) }
        // a voice, too far to make out: noise shaped by two drifting formants, in syllables
        val voice = n(1600)
        var f1 = 0.0; var f2 = 0.0
        repeat(voice) { i ->
            val k = i.toDouble() / RATE
            val syll = (.5 + .5 * sin(2 * PI * 4.3 * k + sin(2 * PI * .7 * k) * 2)).let { if (it < .25) 0.0 else it }
            f1 += 2 * PI * (520 + 180 * sin(2 * PI * 1.3 * k)) / RATE
            f2 += 2 * PI * (1450 + 400 * sin(2 * PI * .9 * k + 1)) / RATE
            val pitch = if (sin(2 * PI * 128 * k) > 0) 1.0 else -.6
            val v = (sin(f1) * .6 + sin(f2) * .3) * pitch * syll * .11
            put(air() * .12f + v.toFloat() + (if (r.nextFloat() < .0006f) r.nextFloat() * .4f else 0f))
        }
        repeat(n(450)) { put(air() * .12f) }
        // the message
        val marks = ArrayList<Long>()
        var phase = 0.0
        val hz = 690.0
        fun tone(units: Int) {
            val len = n(units * UNIT)
            repeat(len) { i ->
                phase += 2 * PI * hz / RATE
                val fade = (.75 + .25 * sin(2 * PI * .35 * t / RATE)).toFloat()
                val edge = minOf(1f, i / 90f, (len - i) / 90f)
                put(air() * .10f + (sin(phase) * .28).toFloat() * fade * edge)
            }
        }
        fun gap(units: Int) = repeat(n(units * UNIT)) { put(air() * .10f) }
        for (ch in text) {
            val c = code(ch)
            if (ch == ' ') gap(4)
            else if (c != null) {
                c.forEachIndexed { k, s -> tone(if (s == '.') 1 else 3); if (k < c.lastIndex) gap(1) }
                gap(3)
            }
            marks += ms()
        }
        // the air goes down
        repeat(n(900)) { i -> put(air() * .12f * (1 - i / n(900).toFloat())) }
        return Clip(out.toShortArray(), text, marks)
    }

    private fun n(ms: Int) = RATE * ms / 1000

    @Volatile private var track: AudioTrack? = null

    /** Plays a clip (stopping one already playing); returns false when the sound is off. */
    fun play(c: Clip): Boolean {
        stop()
        if (!Sfx.on) return false
        return runCatching {
            val t = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(c.pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
            t.write(c.pcm, 0, c.pcm.size); t.play()
            track = t
            true
        }.getOrDefault(false)
    }

    fun stop() { track?.let { runCatching { it.stop(); it.release() } }; track = null }

}
