package app.fieldscan.data

import android.content.Context
import app.fieldscan.Container
import app.fieldscan.Fix
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Catch
import app.fieldscan.domain.Daypart
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Hash
import app.fieldscan.domain.Item
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Upgrade
import app.fieldscan.domain.Weather

/**
 * The demo build: a real district bundled as an Overpass dump (Zamoskvorechye and Bolotny island, Moscow),
 * the observer standing on the island, half a bestiary caught over two weeks, a damp autumn evening.
 */
object DemoSeed {
    val HERE = Geo(55.74195, 37.61290)
    val HOME = Geo(55.73862, 37.62312)

    fun install(ctx: Context, co: Container) {
        val places = runCatching { ctx.assets.open("district.json").bufferedReader().use { Osm.parse(it.readText()) } }.getOrDefault(emptyList())
        apply(co, places)
    }

    /** Puts the demo district, position, weather and (once) the half-full bestiary into [co]. */
    fun apply(co: Container, places: List<app.fieldscan.domain.Place>, now: Long = System.currentTimeMillis()) {
        co.places.value = places
        co.hosts.value = Rules.hosts(places)
        co.weather.value = Weather(temp = 6.0, fog = false, wind = 3.0, windFrom = 240.0, at = now)
        co.fix.value = Fix(HERE, 6f, now)
        if (co.state.value.home == null) co.update { seed(places, now) }
    }

    private fun seed(places: List<app.fieldscan.domain.Place>, now: Long): GameState {
        val day = Rules.DAY
        fun at(creature: String, daysAgo: Double, part: Daypart, temp: Double, fog: Boolean = false, rain: Boolean = false, archived: Boolean = true): Catch {
            val cr = Bestiary[creature]
            val p = places.filter { Rules.dweller(it)?.id == creature }.minByOrNull { it.at.dist(HERE) }
                ?: places.filter { it.habitat == cr.habitat }.minByOrNull { it.at.dist(HERE) }
            // the hour fits the part of day: dawn at 6:40, day at 13:10, dusk at 17:30, night at 23:20
            val hour = when (part) { Daypart.DAWN -> 6.67; Daypart.DAY -> 13.17; Daypart.DUSK -> 17.5; Daypart.NIGHT -> 23.33 }
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = now - (daysAgo.toLong()) * day; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) }
            val t = minOf(now - 60_000, cal.timeInMillis + (hour * 3_600_000).toLong() + (Hash.unit(Hash.of(creature)) * 40 * 60_000).toLong())
            return Catch(creature, p?.id ?: "home", p?.name.orEmpty(), p?.at ?: HOME, t, part, temp, fog, rain, false, .62,
                Hash.mix(Hash.of(p?.id ?: creature), t / day), archived = if (archived) t + day / 3 else null)
        }
        val catches = listOf(
            at("porog", 15.0, Daypart.DUSK, 9.0),
            at("kornevik", 14.6, Daypart.DAY, 11.0),
            at("mysh", 12.2, Daypart.DAY, 10.0, rain = true),
            at("fonar", 11.1, Daypart.NIGHT, 7.0),
            at("ost", 9.0, Daypart.DAWN, 4.0),
            at("kamysh", 8.9, Daypart.DAWN, 3.0, fog = true),
            at("tuman", 8.8, Daypart.DAWN, 3.0, fog = true),
            at("lest", 6.3, Daypart.DUSK, 7.0),
            at("most", 3.2, Daypart.NIGHT, 6.0),
            at("sidel", 1.4, Daypart.DAY, 8.0, archived = false),
            at("kachel", .3, Daypart.DUSK, 6.0, archived = false),
        )
        val caught = catches.map { it.creature }.toSet()
        val known = caught + caught.flatMap { Bestiary[it].tells }
        return GameState(
            home = HOME, catches = catches, known = known, parts = 7, upgrades = setOf(Upgrade.MEMORY),
            items = setOf(Item.ACORN), started = now - 16 * day,
        )
    }
}
