package app.fieldscan

import app.fieldscan.domain.CircleTrack
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Pt
import app.fieldscan.domain.StepsTrack
import app.fieldscan.domain.WalkTrack
import app.fieldscan.sense.Yin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** The rites' GPS and pitch logic on synthetic tracks. */
class TracksTest {
    private val o = Geo(55.74, 37.61)
    private fun at(east: Double, north: Double) = o.offset(0.0, north).offset(90.0, east)

    @Test fun circleCountsAnticlockwise() {
        val t = CircleTrack(o)
        var time = 0L
        for (k in 0..(3 * 360 / 20)) { val b = Math.toRadians(-k * 20.0); t.add(Pt(at(20 * sin(b), 20 * kotlin.math.cos(b)), 5.0, time)); time += 3000 }
        assertTrue("${t.rounds(true)}", t.rounds(true) >= 3.0)
        assertTrue(t.rounds(false) < 0)
    }

    @Test fun circleIgnoresJitterAtCentre() {
        val t = CircleTrack(o)
        // standing on the spot with GPS noise: no rounds
        repeat(200) { t.add(Pt(at(sin(it * 1.3) * 2.5, sin(it * 0.7) * 2.5), 5.0, it * 1000L)) }
        assertTrue(abs(t.rounds(true)) < .1)
    }

    @Test fun stepsCountFromTheFirstReading() {
        val t = StepsTrack(40)
        t.add(10_000, 0)
        t.add(10_020, 10_000)
        assertEquals(20, t.count); assertFalse(t.done)
        t.add(10_040, 20_000)
        assertTrue(t.done)
    }

    @Test fun stepsStartOverAfterAStop() {
        val t = StepsTrack(40)
        t.add(500, 0); t.add(530, 10_000)
        t.tick(40_000)
        assertTrue(t.stalled); assertEquals(0, t.count)
        t.add(545, 41_000)
        assertEquals(15, t.count); assertFalse(t.stalled); assertFalse(t.done)
    }

    @Test fun walkCountsStepsNotJumps() {
        val w = WalkTrack()
        var time = 0L
        w.add(Pt(at(0.0, 0.0), 5.0, time))
        time += 1000; w.add(Pt(at(0.0, 45.0), 5.0, time)) // GPS jump: 45 m in a second
        assertEquals(0.0, w.metres, .01)
        for (k in 1..20) { time += 2000; w.add(Pt(at(0.0, 45.0 + k * 5.0), 5.0, time)) }
        assertEquals(100.0, w.metres, 3.0)
    }

    @Test fun walkAlongWaterOnly() {
        val w = WalkTrack(water = listOf(at(0.0, 0.0)))
        var time = 0L
        for (k in 0..20) { time += 2000; w.add(Pt(at(500.0, k * 5.0), 5.0, time)) }
        assertTrue(w.off); assertEquals(0.0, w.metres, .01)
    }

    private fun sine(hz: Double, amp: Double = 3000.0, rate: Int = 22050) = ShortArray(2048) { (sin(2 * PI * hz * it / rate) * amp).toInt().toShort() }

    @Test fun yinFindsNotes() {
        for (hz in listOf(110.0, 196.0, 293.66, 300.0, 440.0, 523.25, 880.0)) {
            val got = Yin.pitch(sine(hz), 22050)
            assertTrue("$hz -> $got", got != null && abs(got - hz) / hz < .02)
        }
    }

    @Test fun yinQuietAndNoise() {
        assertNull(Yin.pitch(sine(440.0, amp = 20.0), 22050))
        val r = java.util.Random(1)
        assertNull(Yin.pitch(ShortArray(2048) { (r.nextGaussian() * 2000).toInt().toShort() }, 22050))
    }

    @Test fun yinVoiceLike() {
        // a sung A with overtones and a little vibrato
        val buf = ShortArray(2048) { i ->
            val t = i / 22050.0; val f = 220.0 * (1 + .004 * sin(2 * PI * 5 * t))
            ((sin(2 * PI * f * t) + .5 * sin(4 * PI * f * t) + .3 * sin(6 * PI * f * t)) * 2500).toInt().toShort()
        }
        val got = Yin.pitch(buf, 22050)
        assertTrue("$got", got != null && abs(got - 220.0) < 5)
    }
}
