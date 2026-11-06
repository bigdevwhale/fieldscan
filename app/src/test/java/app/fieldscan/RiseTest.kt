package app.fieldscan

import app.fieldscan.domain.RiseTrack
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiseTest {
    @Test
    fun steadyClimbCounts() {
        val t = RiseTrack(6.0)
        for (i in 0..30) t.add(100.0 + i * .3, i * 1000L)
        assertTrue(t.done)
    }

    @Test
    fun aStopStartsOver() {
        val t = RiseTrack(6.0)
        for (i in 0..10) t.add(100.0 + i * .4, i * 1000L) // 4 m up
        for (i in 11..40) t.add(104.0, i * 1000L)          // stands 30 s
        assertTrue(t.stalled)
        assertFalse(t.done)
        for (i in 41..50) t.add(104.0 + (i - 40) * .4, i * 1000L) // another 4 m: from the new start, not enough
        assertFalse(t.done)
    }

    @Test
    fun standingStillAtTheBottomIsNotAStop() {
        val t = RiseTrack(6.0)
        for (i in 0..60) t.add(100.0 + (i % 2) * .1, i * 1000L)
        assertFalse(t.stalled)
    }
}
