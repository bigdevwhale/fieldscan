package app.fieldscan

import app.fieldscan.domain.Story
import app.fieldscan.sense.Radio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioTest {
    @Test fun everyInterceptIsOnAPageAndSendable() {
        for ((title, t) in Story.radio) {
            assertTrue(title, Story.pages.any { it.title.en == title })
            for (msg in listOf(t.ru, t.en)) for (ch in msg) if (ch != ' ') assertNotNull("$msg: '$ch'", Radio.code(ch))
        }
    }

    @Test fun marksFollowTheMessage() {
        val c = Radio.clip("ЭТО Я")
        assertEquals(5, c.marks.size)
        assertTrue(c.marks.zipWithNext().all { (a, b) -> a < b })
        assertTrue(c.marks.last() < c.ms)
    }
}
