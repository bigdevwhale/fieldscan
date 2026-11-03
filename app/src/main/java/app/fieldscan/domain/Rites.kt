package app.fieldscan.domain

/** Text in both languages. */
data class T(val ru: String, val en: String) {
    fun of(ru: Boolean) = if (ru) this.ru else en
}

/** Which way a step wants the observer to look. */
enum class Facing { UPSTREAM, NORTH, SOUTH, WEST, EAST, WIND, TOWARD }

/** Things an observer can carry for a rite (given by one creature, asked for by another). */
enum class Item(val t: T) {
    ACORN(T("жёлудь", "an acorn")),
    DROP(T("капля из родника", "a drop from a spring")),
    FEATHER(T("перо флюгера", "a weathervane feather")),
}

/** What must be true around: checked by the device on its own, nothing to do but come at the right time. */
sealed interface Cond {
    data class Window(val parts: Set<Daypart>) : Cond
    data object Fog : Cond
    data object Rain : Cond
    /** Wind of at least [ms] metres a second. */
    data class Wind(val ms: Double) : Cond
    data object Snow : Cond
    data object Frost : Cond
    data object FullMoon : Cond
    /** Comes only to someone carrying this creature in memory. */
    data class With(val creature: String) : Cond
    /** Won't come to someone carrying this creature. */
    data class Without(val creature: String) : Cond
    data class Carry(val item: Item) : Cond
}

/** One thing to do on the spot; each is checked by a sensor. */
sealed interface Step {
    /** Come within [m] metres. */
    data class Near(val m: Int) : Step
    /** Stand in the middle of the line place (a bridge's span). */
    data object Middle : Step
    data class Face(val to: Facing, val tol: Int = 15) : Step
    /** Don't move for [sec] seconds. */
    data class Still(val sec: Int) : Step
    /** Lay the phone face down for [sec] seconds. */
    data class FaceDown(val sec: Int) : Step
    /** Sing [note] (0 = C … 9 = A, any octave) and hold it three seconds. */
    data class Note(val note: Int) : Step
    /** Blink the torch in Morse: [code] of dots and dashes. */
    data class Morse(val code: String, val letter: T) : Step
    /** Wait in the dark (light sensor) for [sec] seconds. */
    data class Dark(val sec: Int) : Step
    /** Walk round the place [turns] times, counter-clockwise if [ccw]. */
    data class Circle(val turns: Int, val ccw: Boolean) : Step
    /** Climb a staircase bottom to top without stopping. */
    data object Climb : Step
    /** Stay within the place for [min] minutes (you may move about). */
    data class Wait(val min: Int) : Step
    /** Walk [m] metres; along water if [water]. */
    data class Walk(val m: Int, val water: Boolean) : Step
    /** Don't touch the screen for [sec] seconds (the app stays open). */
    data class Hush(val sec: Int) : Step
}

/** The 12 notes, Russian and English names. */
object Notes {
    val ru = listOf("до", "до-диез", "ре", "ре-диез", "ми", "фа", "фа-диез", "соль", "соль-диез", "ля", "ля-диез", "си")
    val en = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    fun name(n: Int, ru: Boolean) = (if (ru) this.ru else en)[((n % 12) + 12) % 12]
    /** Nearest note to [hz] and how far off it is in cents. */
    fun of(hz: Double): Pair<Int, Double> {
        val semis = 12 * kotlin.math.ln(hz / 440.0) / kotlin.math.ln(2.0)
        val r = kotlin.math.round(semis)
        return (((r.toInt() + 9) % 12) + 12) % 12 to (semis - r) * 100
    }
}
