package app.fieldscan

import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Catch
import app.fieldscan.domain.Daypart
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Story
import app.fieldscan.domain.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryTest {
    private fun catch(id: String, t: Long) = Catch(id, "p", "", Geo(55.0, 37.0), t, Daypart.DAY, 5.0, false, false, false, .5, 1)

    /** An eager observer: catches and walks every day. The journal still takes about three years. */
    @Test
    fun takesAboutThreeYears() {
        val ids = Bestiary.all.map { it.id }
        var s = GameState()
        var day = 0
        val start = 1_700_000_000_000L
        while (s.story < Story.pages.size && day < 3000) {
            val now = start + day * Rules.DAY
            s = s.copy(catches = s.catches + catch(ids[day % ids.size], now), walked = s.walked + 10_000)
            // the player does each episode's task the day it's set
            if (!app.fieldscan.domain.Episodes.done(s)) s = app.fieldscan.domain.Episodes.mark(s)
            repeat(3) { s = Story.advance(s, now) }
            day++
        }
        assertEquals(Story.pages.size, s.story)
        assertTrue("finished on day $day", day in 1000..1200)
    }

    @Test
    fun pagesWaitForTheirTier() {
        val p = Story.pages.indexOfFirst { it.tier == Tier.HOST }
        val s = GameState(story = p, pageAt = 0, catches = List(5) { catch("most", it.toLong()) }, walked = 1_000_000)
        assertEquals(Tier.HOST, Story.waiting(s, 400 * Rules.DAY)!!.tier)
        assertEquals(p, Story.advance(s, 400 * Rules.DAY).story)
    }

    @Test
    fun bonusCreaturesExist() {
        Story.bonus.forEach { Bestiary[it.after] }
    }

    @Test
    fun strongerNeedsMoreSteps() {
        fun of(t: Tier) = Bestiary.all.filter { it.tier == t }.maxOf { Rules.pace(it) }
        assertTrue(of(Tier.DWELLER) < of(Tier.WANDERER))
        assertTrue(of(Tier.WANDERER) < of(Tier.HOST))
        assertTrue(of(Tier.HOST) < of(Tier.LEGEND))
        assertTrue(of(Tier.LEGEND) <= Rules.CHARGE_MAX)
    }
}
