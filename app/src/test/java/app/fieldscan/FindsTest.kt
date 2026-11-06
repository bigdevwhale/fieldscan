package app.fieldscan

import app.fieldscan.domain.Finds
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Habitat
import app.fieldscan.domain.Place
import app.fieldscan.domain.Story
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindsTest {
    private val home = Geo(55.74, 37.62)

    @Test fun everyFindHasItsPage() {
        for (f in Finds.all) assertTrue(f.after, Story.pages.any { it.title.en == f.after })
    }

    @Test fun openWithTheirPagesAndCloseWhenFound() {
        assertTrue(Finds.open(GameState(story = 0)).isEmpty())
        val all = GameState(story = Story.pages.size)
        assertEquals(Finds.all.size, Finds.open(all).size)
        assertEquals(Finds.all.size - 1, Finds.open(all.copy(finds = setOf("metka6"))).size)
    }

    @Test fun pinnedNearHome() {
        val bridge = Place("w1", Habitat.BRIDGE, home.offset(90.0, 600.0), "", a = home.offset(90.0, 580.0), b = home.offset(90.0, 620.0))
        val far = Place("w2", Habitat.BRIDGE, home.offset(0.0, 2500.0), "")
        val metka = Finds.all.first { it.id == "metka6" }
        assertTrue(Finds.pick(metka, home, listOf(far, bridge)).dist(bridge.mid) < 5)
        // no bench on the map: a bare point near home, not nothing
        val bench = Finds.all.first { it.id == "skameyka" }
        assertTrue(Finds.pick(bench, home, listOf(bridge)).dist(home) in 300.0..450.0)
        assertEquals(home, Finds.pick(Finds.all.first { it.id == "tuk" }, home, emptyList()))
    }
}
