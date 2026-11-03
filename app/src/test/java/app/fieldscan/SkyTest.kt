package app.fieldscan

import app.fieldscan.domain.Daypart
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Notes
import app.fieldscan.domain.Sky
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime
import kotlin.math.abs

class SkyTest {
    private val moscow = Geo(55.75, 37.62)
    private fun t(s: String) = ZonedDateTime.parse(s).toInstant().toEpochMilli()

    @Test fun noonSunIsSouthAndUp() {
        val s = Sky.sun(t("2026-06-21T12:30:00+03:00"), moscow)
        assertTrue("alt ${s[0]}", s[0] in 55.0..59.0)
        assertTrue("az ${s[1]}", abs(s[1] - 180) < 12)
    }

    @Test fun dayparts() {
        assertEquals(Daypart.NIGHT, Sky.daypart(t("2026-11-03T23:00:00+03:00"), moscow))
        assertEquals(Daypart.DAY, Sky.daypart(t("2026-11-03T12:00:00+03:00"), moscow))
        val dusk = Sky.daypart(t("2026-11-03T16:50:00+03:00"), moscow)
        assertTrue("$dusk", dusk == Daypart.DUSK)
        val dawn = Sky.daypart(t("2026-11-03T07:30:00+03:00"), moscow)
        assertTrue("$dawn", dawn == Daypart.DAWN)
    }

    @Test fun moonPhase() {
        // full moon on 2026-11-24 (approx), new moon on 2026-11-09
        assertTrue(Sky.moon(t("2026-11-24T12:00:00Z"))[1] > .95)
        assertTrue(Sky.moon(t("2026-11-09T12:00:00Z"))[1] < .05)
    }

    @Test fun notes() {
        assertEquals(9, Notes.of(440.0).first)
        assertEquals(9, Notes.of(220.0).first)
        assertEquals(0, Notes.of(261.63).first)
        assertTrue(abs(Notes.of(446.0).second) < 30)
    }
}
