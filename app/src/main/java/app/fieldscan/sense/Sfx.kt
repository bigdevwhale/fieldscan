package app.fieldscan.sense

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The device's sounds, all synthesised (no recordings): the hiss of the air with its crackles, the sweep's
 * ping on a trace, and short signals for what happens in a rite. Off when [on] is false.
 */
object Sfx {
    private const val RATE = 22050
    @Volatile var on = true

    // ——— the air: a stream of static that thickens near a trace ———
    /** 0 = silent, 1 = quiet hiss far from anything … 5 = right by an awake trace. */
    @Volatile var level = 0f
    @Volatile private var running = false
    private var thread: Thread? = null

    fun startAir() {
        if (running) return
        running = true
        thread = Thread {
            val n = 1024
            val track = runCatching {
                AudioTrack.Builder().setAudioAttributes(attrs()).setAudioFormat(format())
                    .setBufferSizeInBytes(maxOf(n * 4, AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)))
                    .setTransferMode(AudioTrack.MODE_STREAM).build()
            }.getOrNull() ?: run { running = false; return@Thread }
            track.play()
            val buf = ShortArray(n)
            val r = Random(System.nanoTime())
            var lp = 0f; var lp2 = 0f; var crack = 0f; var t = 0L; var gain = 0f
            var whistle = 0f; var wPhase = 0.0; var wFreq = 900.0
            try {
                while (running) {
                    val lv = if (on) level else 0f
                    val target = if (lv <= 0f) 0f else .035f + lv * .018f
                    for (i in 0 until n) {
                        gain += (target - gain) * .0005f
                        // pink-ish hiss: white noise through two soft low-passes
                        val w = r.nextFloat() * 2 - 1
                        lp += (w - lp) * .18f; lp2 += (lp - lp2) * .35f
                        // slow breathing of the air
                        val wob = .75f + .25f * sin(2 * PI * .23 * t / RATE).toFloat()
                        // crackles: more of them near a trace
                        if (r.nextFloat() < (.0002f + lv * .00025f)) crack = .5f + r.nextFloat() * .5f
                        val c = crack * (r.nextFloat() * 2 - 1); crack *= .93f
                        // close by, the receiver tunes in: a thin whistle sliding about
                        if (lv >= 4 && r.nextFloat() < .00003f) { whistle = .25f; wFreq = 700.0 + r.nextDouble() * 900 }
                        wFreq += (r.nextDouble() - .5) * 2; wPhase += 2 * PI * wFreq / RATE
                        val wh = whistle * sin(wPhase).toFloat(); whistle *= .99985f
                        val v = (lp2 * 2.2f * wob + c * .9f + wh) * gain
                        buf[i] = (v.coerceIn(-1f, 1f) * 32767).toInt().toShort()
                        t++
                    }
                    track.write(buf, 0, n)
                }
            } finally { runCatching { track.stop() }; track.release() }
        }.apply { isDaemon = true; name = "air"; start() }
    }

    fun stopAir() { running = false; thread = null }

    // ——— short signals ———
    /** The sweep passing a trace: higher and brighter for a strong one. */
    fun ping(strength: Int, hot: Boolean) = play(tone(if (hot) 1560.0 else 900.0 + strength * 90, 70, .10f + strength * .025f, decay = 40.0, noise = .15f))

    /** A trace opened: the receiver tunes in. */
    fun tune() = play(sweep(500.0, 1700.0, 220, .14f))

    /** A trace close by and awake: three rising pings. */
    fun found() = play(tone(990.0, 90, .2f, 20.0) + silence(60) + tone(1240.0, 90, .2f, 20.0) + silence(60) + tone(1560.0, 180, .22f, 12.0))

    /** A typewriter key, for the opening's typed lines. */
    fun key() = play(burst(6, .10f) + tone(2900.0, 12, .03f, 120.0))

    /** A rite step done. */
    fun step() = play(tone(880.0, 70, .22f, 30.0) + tone(1320.0, 110, .22f, 25.0))

    /** A step broken: you moved, it ducked back. */
    fun fail() = play(square(140.0, 260, .16f))

    /** The trail camera's shutter. */
    fun shutter() = play(burst(25, .5f) + silence(45) + burst(35, .35f) + silence(60) + tone(2400.0, 140, .12f, 18.0))

    /** A new journal page: a call sign coming through the noise. */
    fun page() = play(listOf(90, 90, 230).flatMap { listOf(tone(1040.0, it, .16f, 2.0), silence(70)) }.reduce { a, b -> a + b })

    // ——— making the sounds ———
    private fun play(pcm: ShortArray) {
        if (!on || pcm.isEmpty()) return
        Thread {
            runCatching {
                val t = AudioTrack.Builder().setAudioAttributes(attrs()).setAudioFormat(format())
                    .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                t.write(pcm, 0, pcm.size); t.play()
                Thread.sleep(pcm.size * 1000L / RATE + 100)
                t.release()
            }
        }.apply { isDaemon = true; start() }
    }

    private fun n(ms: Int) = RATE * ms / 1000

    private fun tone(hz: Double, ms: Int, vol: Float, decay: Double, noise: Float = 0f): ShortArray {
        val r = Random(hz.toLong())
        return ShortArray(n(ms)) { i ->
            val t = i.toDouble() / RATE
            val env = exp(-t * decay) * minOf(1.0, i / 60.0)
            ((sin(2 * PI * hz * t) * (1 - noise) + (r.nextDouble() * 2 - 1) * noise) * env * vol * 32767).toInt().toShort()
        }
    }

    private fun sweep(from: Double, to: Double, ms: Int, vol: Float): ShortArray {
        val len = n(ms); var ph = 0.0; val r = Random(7)
        return ShortArray(len) { i ->
            val k = i.toDouble() / len
            ph += 2 * PI * (from + (to - from) * k * k) / RATE
            val env = sin(PI * k)
            ((sin(ph) * .6 + (r.nextDouble() * 2 - 1) * .4) * env * vol * 32767).toInt().toShort()
        }
    }

    private fun square(hz: Double, ms: Int, vol: Float): ShortArray {
        val len = n(ms)
        return ShortArray(len) { i ->
            val t = i.toDouble() / RATE
            val s = if (sin(2 * PI * hz * t) >= 0) 1.0 else -1.0
            (s * exp(-t * 9) * vol * 32767).toInt().toShort()
        }
    }

    private fun burst(ms: Int, vol: Float): ShortArray {
        val len = n(ms); val r = Random(ms)
        return ShortArray(len) { i -> ((r.nextDouble() * 2 - 1) * exp(-i * 6.0 / len) * vol * 32767).toInt().toShort() }
    }

    private fun silence(ms: Int) = ShortArray(n(ms))

    private fun attrs() = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private fun format() = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
}
