package app.fieldscan.domain

/** One place of the week's round. */
data class Stop(val id: String, val habitat: Habitat, val at: Geo, val name: String, val done: Boolean = false)

/**
 * The round of the week: every Monday three places of the district, 200 m to 1.5 km from home, each of a
 * different kind, chosen by the week and the home (the same all week, pinned in the state). All three walked
 * to give parts and a charge for the trap.
 */
object Weekly {
    const val COUNT = 3
    const val PARTS = 2
    const val CHARGE = 1000
    const val REACH = 40.0
    private const val NEAREST = 200.0
    private const val FARTHEST = 1500.0

    /** The week number, Mondays first (epoch day 0 was a Thursday). */
    fun week(now: Long): Long = (now / Rules.DAY + 3) / 7

    /** Days left in the week, today counted. */
    fun daysLeft(now: Long): Int = (7 - ((now / Rules.DAY + 3) % 7)).toInt()

    fun pick(home: Geo, week: Long, places: List<Place>): List<Stop> {
        val seed = Hash.mix(week, Hash.of("${round(home.lat)}:${round(home.lon)}"))
        val fit = places.filter { it.habitat != Habitat.HOME && it.at.dist(home) in NEAREST..FARTHEST }
            .sortedBy { Hash.mix(seed, Hash.of(it.id)) }
        val out = ArrayList<Stop>()
        for (p in fit) {
            if (out.size == COUNT) break
            if (out.any { it.habitat == p.habitat }) continue
            out += Stop(p.id, p.habitat, p.mid, p.name)
        }
        return out
    }

    private fun round(x: Double) = kotlin.math.round(x * 100) / 100

    /** The state with any stop within reach of [at] marked, and the reward if that made three. */
    fun visit(s: GameState, at: Geo, acc: Double): GameState {
        val reach = REACH + minOf(acc, 25.0)
        if (s.stops.none { !it.done && it.at.dist(at) <= reach }) return s
        val stops = s.stops.map { if (!it.done && it.at.dist(at) <= reach) it.copy(done = true) else it }
        val all = stops.size == COUNT && stops.all { it.done }
        return if (all && !s.weekPaid) s.copy(stops = stops, weekPaid = true, parts = s.parts + PARTS, charge = (s.charge + CHARGE).coerceAtMost(Rules.CHARGE_MAX))
        else s.copy(stops = stops)
    }
}
