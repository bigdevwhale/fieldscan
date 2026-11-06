package app.fieldscan

import app.fieldscan.domain.Catch
import app.fieldscan.domain.Daypart
import app.fieldscan.domain.Episodes
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Habitat
import app.fieldscan.domain.Place
import app.fieldscan.domain.Story
import app.fieldscan.domain.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodesTest {
    private val home = Geo(55.74, 37.62)
    private fun page(en: String) = Story.pages.indexOfFirst { it.title.en == en } + 1
    private fun c(id: String, at: Geo = home, archived: Long? = null) = Catch(id, "p", "", at, 5, Daypart.DAY, 5.0, false, false, false, .5, 1, archived)

    @Test fun everyEpisodeHasItsPage() {
        for (e in Episodes.all) assertTrue(e.page, Story.pages.any { it.title.en == e.page })
        assertEquals(Episodes.all.size, Episodes.all.map { it.page }.toSet().size)
    }

    @Test fun nextPageWaitsForTheTask() {
        val s = GameState(home = home, story = page("The Bureau's address"), pageAt = 1, walked = 1_000_000, catches = List(9) { c("most") })
        val w = Story.waiting(s, 100 * 86_400_000L)!!
        assertTrue(w.task?.task is Task.Visit)
        assertFalse(w.ready)
        val lamp = Place("n1", Habitat.STOP, home.offset(10.0, 20.0), "")
        assertFalse("not by day", Episodes.reached(s, home, 5.0, Daypart.DAY, listOf(lamp)))
        assertTrue(Episodes.reached(s, home, 5.0, Daypart.NIGHT, listOf(lamp)))
        assertTrue(Story.waiting(Episodes.mark(s), 100 * 86_400_000L)!!.ready)
    }

    @Test fun theStateShowsSomeTasksDone() {
        val s = GameState(home = home, story = page("The first keeper"), pageAt = 1000, catches = listOf(c("most")))
        assertFalse(Episodes.done(s))
        assertTrue(Episodes.done(s.copy(catches = listOf(c("most", archived = 2000)))))
        val first = GameState(home = home, story = page("What erases"), pageAt = 1, catches = listOf(c("most", at = home.offset(0.0, 900.0))))
        assertFalse(Episodes.reached(first, home, 5.0, Daypart.DAY, emptyList()))
        assertTrue(Episodes.reached(first, home.offset(0.0, 890.0), 5.0, Daypart.DAY, emptyList()))
    }
}
