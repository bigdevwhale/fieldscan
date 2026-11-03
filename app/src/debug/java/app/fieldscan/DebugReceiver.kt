package app.fieldscan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import app.fieldscan.domain.Geo
import kotlin.math.PI
import kotlin.math.sin

/**
 * Debug builds only. Drives the device from adb so rites can be checked at a desk:
 *
 *   adb shell am broadcast -n app.fieldscan.dev/app.fieldscan.DebugReceiver --es cmd fix --ef east 10 --ef north -5 [--ef acc 5]
 *       a fix relative to the place of the open rite (metres east/north of it)
 *   ... --es cmd along --ef t 0.5        a fix along the place's line (a bridge, stairs): 0 = a, 1 = b
 *   ... --es cmd at --ed lat 55.7 --ed lon 37.6
 *   ... --es cmd weather --ez fog true --ez snow false
 *   ... --es cmd heading --ef deg 180    (or --ef deg -1 to give the compass back)
 *   ... --es cmd tone --ef hz 440 --ei ms 5000   plays a sine through the speaker
 *   ... --es cmd real                     real GPS again
 */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val co = context.container
        val now = System.currentTimeMillis()
        val acc = intent.getFloatExtra("acc", 5f)
        fun put(g: Geo) { co.mockUntil = now + 10 * 60_000; co.onFix(Fix(g, acc, now), test = true) }
        when (intent.getStringExtra("cmd")) {
            "fix" -> co.riteSpot?.let { p ->
                val e = intent.getFloatExtra("east", 0f).toDouble(); val n = intent.getFloatExtra("north", 0f).toDouble()
                put(p.at.offset(0.0, n).offset(90.0, e))
            }
            "along" -> co.riteSpot?.let { p ->
                val a = p.a ?: p.at; val b = p.b ?: p.at; val t = intent.getFloatExtra("t", 0f).toDouble()
                put(Geo(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t))
            }
            "at" -> put(Geo(intent.getDoubleExtra("lat", 0.0), intent.getDoubleExtra("lon", 0.0)))
            "weather" -> co.weather.value = co.weather.value.copy(fog = intent.getBooleanExtra("fog", false), snow = intent.getBooleanExtra("snow", false), at = now)
            "heading" -> intent.getFloatExtra("deg", -1f).let { co.headingOverride.value = if (it < 0) null else it }
            "tone" -> tone(intent.getFloatExtra("hz", 440f).toDouble(), intent.getIntExtra("ms", 5000))
            "real" -> { co.mockUntil = 0 }
        }
        setResultData("ok fix=${co.fix.value?.at} spot=${co.riteSpot?.id}")
    }

    private fun tone(hz: Double, ms: Int) {
        val rate = 44100
        val n = rate * ms / 1000
        val pcm = ShortArray(n) { i -> (sin(2 * PI * hz * i / rate) * 12000 * minOf(1.0, i / 2000.0, (n - i) / 2000.0)).toInt().toShort() }
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(n * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
        t.write(pcm, 0, n); t.play()
        Thread { Thread.sleep(ms + 300L); t.release() }.start()
    }
}
