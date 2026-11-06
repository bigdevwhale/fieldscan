package app.fieldscan

import app.fieldscan.domain.Catch
import app.fieldscan.domain.Daypart
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.ui.Guide
import org.junit.Assert.assertEquals
import org.junit.Test

class GuideTest {
    private val home = Geo(55.0, 37.0)
    private fun c(id: String, archived: Long? = null) = Catch(id, "p", "", home, 1, Daypart.DAY, 5.0, false, false, false, .5, 1, archived)

    @Test fun stages() {
        assertEquals(Guide.Stage.DONE, Guide.stage(GameState()))
        assertEquals(Guide.Stage.DOOR, Guide.stage(GameState(home = home)))
        assertEquals(Guide.Stage.ARCHIVE, Guide.stage(GameState(home = home, catches = listOf(c("porog")))))
        assertEquals(Guide.Stage.STREET, Guide.stage(GameState(home = home, catches = listOf(c("porog", archived = 2)))))
        assertEquals(Guide.Stage.DONE, Guide.stage(GameState(home = home, catches = listOf(c("porog", 2), c("most")))))
        assertEquals(Guide.Stage.DONE, Guide.stage(GameState(home = home, guide = false)))
    }
}
