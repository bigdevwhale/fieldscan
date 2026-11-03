package app.fieldscan

import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The catalogue holds together: unique ids, rumours that lead somewhere, every creature reachable. */
class BestiaryTest {
    @Test fun idsAreUnique() = assertEquals(Bestiary.all.size, Bestiary.all.map { it.id }.toSet().size)

    @Test fun rumoursPointToCreatures() {
        for (c in Bestiary.all) for (t in c.tells) assertTrue("${c.id} tells $t", t in Bestiary.byId)
    }

    @Test fun everyoneIsToldAboutOrSeen() {
        // a creature is found either by a trace on the scanner (dwellers, homelings, wanderers in their weather)
        // or through a rumour; keepers and the legend need a rumour or a trace too
        val told = Bestiary.all.flatMap { it.tells }.toSet()
        val lonely = Bestiary.all.filter { it.id !in told && it.tier == Tier.HOST }
        assertTrue("keepers nobody mentions: ${lonely.map { it.id }}", lonely.isEmpty())
    }

    @Test fun textsAreFilled() {
        for (c in Bestiary.all) {
            for (t in listOf(c.name, c.riddle, c.hint, c.rumour)) assertTrue(c.id, t.ru.isNotBlank() && t.en.isNotBlank())
            assertTrue(c.id, c.steps.isNotEmpty())
        }
        println("creatures: ${Bestiary.all.size} " + Tier.entries.joinToString { "${it.name}=${Bestiary.all.count { c -> c.tier == it }}" })
    }
}
