package app.fieldscan

import app.fieldscan.data.Osm
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Habitat
import app.fieldscan.domain.Moment
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Scanner
import app.fieldscan.domain.Weather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** The demo district read the way the app reads any district. */
@RunWith(RobolectricTestRunner::class)
class DistrictTest {
    private val places by lazy { Osm.parse(File("demo-assets/district.json").readText()) }
    private val here = Geo(55.74195, 37.61290)

    @Test fun habitats() {
        val by = places.groupBy { it.habitat }
        for (h in Habitat.entries) println("%-11s %d".format(h, by[h]?.size ?: 0))
        assertTrue("bridges over water", (by[Habitat.BRIDGE]?.size ?: 0) >= 3)
        assertTrue("every bridge knows its flow", by[Habitat.BRIDGE].orEmpty().all { it.flow != null })
        assertTrue("stairs", (by[Habitat.STEPS]?.size ?: 0) >= 1)
        assertTrue("waterside points", (by[Habitat.WATER]?.size ?: 0) >= 3)
    }

    @Test fun hosts() {
        val h = Rules.hosts(places)
        h.forEach { (k, p) -> println("$k -> ${p.id} ${p.name} size=${p.size} year=${p.year} ways=${p.ways}") }
        assertTrue(h.isNotEmpty())
    }

    @Test fun cacheRoundTrip() {
        val back = Osm.fromJson(Osm.toJson(places))
        assertEquals(places, back)
    }

    @Test fun traces() {
        val st = GameState(home = Geo(55.73862, 37.62312))
        val night = 1762200000000L // 3 Nov 2025 ~20:00 UTC: night in Moscow
        val m = Moment(night, here, Weather(fog = true), st)
        val ts = Scanner.traces(m, places, Rules.hosts(places), limit = 12)
        ts.forEach { println("${it.creature.id.padEnd(12)} ${it.place.habitat} ${it.dist.toInt()} m str=${it.strength} open=${it.open}") }
        assertTrue(ts.isNotEmpty())
        // every dweller kind with a habitat in the district has at least one home
        val homes = places.mapNotNull { Rules.dweller(it)?.id }.toSet()
        println("dwellers with a home here: ${homes.size} of ${Bestiary.all.count { it.tier == app.fieldscan.domain.Tier.DWELLER }}: $homes")
    }

    @Test fun deterministic() {
        val a = places.map { Rules.dweller(it)?.id }
        val b = places.map { Rules.dweller(it)?.id }
        assertEquals(a, b)
        val empty = places.count { Rules.dweller(it) == null && Bestiary.dwellersOf(it.habitat).isNotEmpty() }.toDouble() /
            places.count { Bestiary.dwellersOf(it.habitat).isNotEmpty() }
        println("empty share %.2f".format(empty))
        assertTrue(empty in .3..0.5)
    }
}
