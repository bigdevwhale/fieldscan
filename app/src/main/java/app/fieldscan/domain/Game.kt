package app.fieldscan.domain

import kotlin.math.round

/** A creature caught: the frame is grown from [seed]; [archived] is when it was handed in at home. */
data class Catch(
    val creature: String,
    val place: String,
    val placeName: String,
    val at: Geo,
    val time: Long,
    val part: Daypart,
    val temp: Double,
    val fog: Boolean,
    val rain: Boolean,
    val snow: Boolean,
    val moon: Double,
    val seed: Long,
    val archived: Long? = null,
    /** How worn the frame was when it went into the archive (0..4). */
    val wornAtArchive: Int = 0,
) {
    val id get() = "$creature@$time"
}

/** Upgrades of the device bought with parts. */
enum class Upgrade(val t: T, val about: T, val cost: Int) {
    ANTENNA(T("Антенна II", "Antenna II"), T("Радиус сканера 3 → 6 км.", "Scanner range 3 → 6 km."), 5),
    MEMORY(T("Модуль памяти", "Memory module"), T("Память 16 → 24 КБ.", "Memory 16 → 24 KB."), 4),
    MEMORY2(T("Второй модуль", "Second module"), T("Память 24 → 32 КБ.", "Memory 24 → 32 KB."), 6),
}

/** Everything that's saved. */
data class GameState(
    val home: Geo? = null,
    val catches: List<Catch> = emptyList(),
    /** Riddles the observer has seen (from a trace or a rumour). */
    val known: Set<String> = emptySet(),
    val parts: Int = 0,
    val upgrades: Set<Upgrade> = emptySet(),
    val items: Set<Item> = emptySet(),
    val nightOff: Boolean = false,
    val shy: Boolean = false,
    val sound: Boolean = true,
    val started: Long = 0,
    /** Manual pages already read (Tip names). */
    val tips: Set<String> = emptySet(),
    /** 0 follows the system, 1 light, 2 dark. */
    val theme: Int = 0,
) {
    val caughtIds: Set<String> get() = catches.map { it.creature }.toSet()
    val inMemory: List<Catch> get() = catches.filter { it.archived == null }
    val memoryKb: Int get() = inMemory.sumOf { Bestiary[it.creature].kb }
    val capacity: Int get() = 16 + (if (Upgrade.MEMORY in upgrades) 8 else 0) + (if (Upgrade.MEMORY2 in upgrades) 8 else 0)
    val radius: Double get() = if (Upgrade.ANTENNA in upgrades) 6000.0 else 3000.0
    fun firstOf(id: String) = catches.firstOrNull { it.creature == id }
}

object Rules {
    const val DAY = 86_400_000L
    /** Share of fitting places that stay empty: not every lamp is haunted. */
    const val EMPTY = 0.4
    const val HOME_RADIUS = 50.0

    /** 0 fresh … 4 badly worn: a frame wears a step a day while it sits in memory. */
    fun worn(c: Catch, now: Long): Int = if (c.archived != null) c.wornAtArchive else ((now - c.time) / DAY).toInt().coerceIn(0, 4)

    fun parts(t: Tier) = when (t) { Tier.DWELLER -> 1; Tier.WANDERER, Tier.HOMELY -> 2; Tier.HOST -> 3; Tier.LEGEND -> 5 }

    /** The dweller of a place, or null if it's one of the empty ones. */
    fun dweller(p: Place): Creature? {
        val list = Bestiary.dwellersOf(p.habitat)
        if (list.isEmpty()) return null
        val h = Hash.of(p.id)
        if (Hash.unit(h, 1) < EMPTY) return null
        return list[(Hash.mix(h, 2) ushr 1).rem(list.size).toInt()]
    }

    /** The keepers' places in the district: the oldest tree, the longest stairs and bridge, the busiest crossroads. */
    fun hosts(places: List<Place>): Map<Most, Place> {
        val m = HashMap<Most, Place>()
        val trees = places.filter { it.habitat == Habitat.TREE }
        // the oldest by date; without dates the biggest; without sizes the device just "knows" one
        (trees.filter { it.year != null }.minByOrNull { it.year!! } ?: trees.filter { it.size > 0 }.maxByOrNull { it.size }
            ?: trees.minByOrNull { Hash.of(it.id) })?.let { m[Most.OLDEST_TREE] = it }
        places.filter { it.habitat == Habitat.STEPS }.maxByOrNull { it.size }?.let { m[Most.LONGEST_STAIRS] = it }
        places.filter { it.habitat == Habitat.BRIDGE }.maxByOrNull { it.size }?.let { m[Most.LONGEST_BRIDGE] = it }
        places.filter { it.habitat == Habitat.CROSSING }.maxByOrNull { it.ways }?.let { m[Most.BUSIEST_CROSSING] = it }
        return m
    }

    /** The nearest whole-degree confluence. */
    fun confluence(g: Geo) = Place("conf_${round(g.lat).toInt()}_${round(g.lon).toInt()}", Habitat.VIEWPOINT, Geo(round(g.lat), round(g.lon)), "")

    fun homePlace(home: Geo) = Place("home", Habitat.HOME, home)
}

/** What the device knows about the moment: for conditions. */
data class Moment(val now: Long, val me: Geo?, val weather: Weather, val state: GameState) {
    val part: Daypart get() = Sky.daypart(now, me ?: state.home ?: Geo(55.75, 37.62))
    val memoryIds: Set<String> get() = state.inMemory.map { it.creature }.toSet()

    fun ok(c: Cond): Boolean = when (c) {
        is Cond.Window -> part in c.parts
        Cond.Fog -> weather.fog
        Cond.Rain -> weather.rain
        is Cond.Wind -> weather.wind >= c.ms
        Cond.Snow -> weather.snow
        Cond.Frost -> weather.frost
        Cond.FullMoon -> Sky.fullMoon(now) && Sky.moonPos(now, me ?: Geo(55.75, 37.62))[0] > 2
        is Cond.With -> c.creature in memoryIds
        is Cond.Without -> c.creature !in memoryIds
        is Cond.Carry -> c.item in state.items
    }

    /** The time window and weather are open (companions and items aside). */
    fun open(cr: Creature) = cr.conds.all { it is Cond.With || it is Cond.Without || it is Cond.Carry || ok(it) }
    fun ready(cr: Creature) = cr.conds.all { ok(it) }
}

/** One trace on the scanner. */
data class Trace(val place: Place, val creature: Creature, val dist: Double, val bearing: Double, val strength: Int, val open: Boolean, val caught: Boolean) {
    val key get() = creature.id + "@" + place.id
}

object Scanner {
    fun strength(d: Double, open: Boolean): Int {
        val s = when { d < 100 -> 5; d < 300 -> 4; d < 800 -> 3; d < 1600 -> 2; else -> 1 }
        return if (open) s else minOf(s, 1)
    }

    /**
     * Traces around the observer, best first: new ones before caught, open before sleeping, near before far.
     * Wanderers come only when their weather does, to the nearest fitting place.
     */
    fun traces(m: Moment, places: List<Place>, hosts: Map<Most, Place>, limit: Int = 5): List<Trace> {
        val me = m.me ?: return emptyList()
        val st = m.state
        val r = st.radius
        val out = ArrayList<Trace>()
        fun add(p: Place, c: Creature) {
            val d = me.dist(p.at)
            if (d > r) return
            if (st.nightOff && c.conds.any { it is Cond.Window && it.parts == setOf(Daypart.NIGHT) }) return
            val open = m.open(c)
            out += Trace(p, c, d, me.bearing(p.at), strength(d, open), open, c.id in st.caughtIds)
        }
        for (p in places) Rules.dweller(p)?.let { add(p, it) }
        for ((most, p) in hosts) Bestiary.all.firstOrNull { it.most == most }?.let { add(p, it) }
        st.home?.let { add(Rules.homePlace(it), Bestiary["porog"]) }
        // wanderers: only in their weather
        for (c in Bestiary.all.filter { it.tier == Tier.WANDERER }) {
            if (!c.conds.filter { it !is Cond.Window }.all { m.ok(it) }) continue
            val fit = places.filter { it.habitat == c.habitat || (c.habitat == Habitat.WATER && it.habitat in setOf(Habitat.PIER, Habitat.BRIDGE)) }
            val p = (fit.ifEmpty { places }).minByOrNull { me.dist(it.at) } ?: continue
            add(p.copy(id = "wander_${c.id}_" + p.id), c)
        }
        add(Rules.confluence(me), Bestiary["strazh"])
        return out.sortedWith(compareBy<Trace>({ it.caught }, { !it.open }, { it.dist })).distinctBy { it.creature.id + if (it.caught) it.place.id else "" }.take(limit)
    }
}
