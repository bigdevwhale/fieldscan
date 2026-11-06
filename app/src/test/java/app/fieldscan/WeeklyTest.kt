package app.fieldscan

import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Habitat
import app.fieldscan.domain.Place
import app.fieldscan.domain.Weekly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyTest {
    private val home = Geo(55.74, 37.62)
    private val kinds = listOf(Habitat.LAMP, Habitat.BENCH, Habitat.STOP, Habitat.TREE, Habitat.PHARMACY, Habitat.POSTBOX)
    private val places = (0 until 60).map { i -> Place("n$i", kinds[i % kinds.size], home.offset(i * 37.0 % 360, 150.0 + i * 30), "") }

    @Test fun threeDifferentKindsInRange() {
        val st = Weekly.pick(home, 2900, places)
        assertEquals(3, st.size)
        assertEquals(3, st.map { it.habitat }.toSet().size)
        assertTrue(st.all { it.at.dist(home) in 200.0..1500.0 })
        assertEquals(st, Weekly.pick(home, 2900, places.shuffled()))
        assertNotEquals(st, Weekly.pick(home, 2901, places))
    }

    @Test fun threeVisitsPayOnce() {
        val st = Weekly.pick(home, 2900, places)
        var s = GameState(home = home, stops = st, week = 2900, charge = 0)
        for (p in st) s = Weekly.visit(s, p.at, 5.0)
        assertTrue(s.weekPaid)
        assertEquals(Weekly.PARTS, s.parts)
        assertEquals(Weekly.CHARGE, s.charge)
        assertTrue(Weekly.visit(s, st[0].at, 5.0) === s)
        assertFalse(Weekly.visit(GameState(stops = st), home, 5.0).stops.any { it.done })
    }

    @Test fun mondayStartsTheWeek() {
        // 2026-11-09 was a Monday, 2026-11-08 a Sunday
        val mon = java.time.LocalDate.of(2026, 11, 9).toEpochDay() * 86_400_000L
        assertEquals(Weekly.week(mon), Weekly.week(mon + 6 * 86_400_000L))
        assertNotEquals(Weekly.week(mon), Weekly.week(mon - 1))
        assertEquals(7, Weekly.daysLeft(mon))
    }
}
