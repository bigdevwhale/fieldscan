package app.fieldscan.data

import android.content.Context
import app.fieldscan.domain.Catch
import app.fieldscan.domain.Daypart
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Item
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Upgrade
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Saves the game as JSON in the app's files, atomically (write aside, then rename). */
class Store(context: Context) {
    private val dir = context.filesDir
    private val stateFile = File(dir, "state.json")
    val tiles = File(dir, "tiles").apply { mkdirs() }
    val frames = File(dir, "frames").apply { mkdirs() }

    fun load(): GameState? = runCatching { fromJson(JSONObject(stateFile.readText())) }.getOrNull()

    @Synchronized
    fun save(s: GameState) = write(stateFile, toJson(s).toString())

    /** Bump the version when parsing changes, so old tiles are fetched again. */
    fun tile(key: String): File = File(tiles, "v2_$key.json")

    companion object {
        fun write(f: File, text: String) {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        }

        fun toJson(s: GameState): JSONObject = JSONObject().apply {
            s.home?.let { put("home", JSONArray().put(it.lat).put(it.lon)) }
            put("catches", JSONArray().apply {
                s.catches.forEach { c ->
                    put(JSONObject().put("c", c.creature).put("p", c.place).put("pn", c.placeName).put("y", c.at.lat).put("x", c.at.lon)
                        .put("t", c.time).put("d", c.part.name).put("tp", c.temp).put("fg", c.fog).put("rn", c.rain).put("sn", c.snow)
                        .put("m", c.moon).put("s", c.seed).apply { c.archived?.let { put("a", it) } }.put("w", c.wornAtArchive))
                }
            })
            put("known", JSONArray(s.known.toList()))
            put("parts", s.parts)
            put("up", JSONArray(s.upgrades.map { it.name }))
            put("items", JSONArray(s.items.map { it.name }))
            put("nightOff", s.nightOff); put("shy", s.shy); put("sound", s.sound); put("started", s.started)
            put("tips", JSONArray(s.tips.toList()))
            put("theme", s.theme)
            put("charge", s.charge); put("stepMark", s.stepMark); put("stepBoot", s.stepBoot); put("healthFrom", s.healthFrom)
            put("walked", s.walked); put("story", s.story); put("pageAt", s.pageAt); put("pageCatches", s.pageCatches); put("pageWalked", s.pageWalked); put("pageSeen", s.pageSeen); put("guide", s.guide)
            put("finds", JSONArray(s.finds.toList()))
            put("tasks", JSONArray(s.tasks.toList())); put("pageKinds", s.pageKinds)
            put("week", s.week); put("weekPaid", s.weekPaid)
            put("stops", JSONArray().apply { s.stops.forEach { p -> put(JSONObject().put("id", p.id).put("h", p.habitat.name).put("y", p.at.lat).put("x", p.at.lon).put("n", p.name).put("d", p.done)) } })
            put("findAt", JSONObject().apply { s.findAt.forEach { (k, g) -> put(k, JSONArray().put(g.lat).put(g.lon)) } })
            put("healthSeen", JSONObject().apply { s.healthSeen.forEach { (d, n) -> put(d.toString(), n) } })
        }

        fun fromJson(o: JSONObject): GameState {
            fun strs(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            val cs = o.optJSONArray("catches")
            return GameState(
                home = o.optJSONArray("home")?.let { Geo(it.getDouble(0), it.getDouble(1)) },
                catches = (0 until (cs?.length() ?: 0)).map { i ->
                    val c = cs!!.getJSONObject(i)
                    Catch(
                        c.getString("c"), c.getString("p"), c.optString("pn"), Geo(c.getDouble("y"), c.getDouble("x")), c.getLong("t"),
                        runCatching { Daypart.valueOf(c.getString("d")) }.getOrDefault(Daypart.NIGHT), c.optDouble("tp", 8.0),
                        c.optBoolean("fg"), c.optBoolean("rn"), c.optBoolean("sn"), c.optDouble("m", .5), c.getLong("s"),
                        if (c.has("a")) c.getLong("a") else null, c.optInt("w"),
                    )
                },
                known = strs("known").toSet(),
                parts = o.optInt("parts"),
                upgrades = strs("up").mapNotNull { runCatching { Upgrade.valueOf(it) }.getOrNull() }.toSet(),
                items = strs("items").mapNotNull { runCatching { Item.valueOf(it) }.getOrNull() }.toSet(),
                nightOff = o.optBoolean("nightOff"), shy = o.optBoolean("shy"), sound = o.optBoolean("sound", true),
                started = o.optLong("started"),
                tips = strs("tips").toSet(),
                theme = o.optInt("theme"),
                charge = o.optInt("charge", Rules.START_CHARGE),
                stepMark = o.optLong("stepMark", -1),
                stepBoot = o.optLong("stepBoot"),
                healthFrom = o.optLong("healthFrom", -1),
                walked = o.optLong("walked"), story = o.optInt("story"), pageAt = o.optLong("pageAt"),
                pageCatches = o.optInt("pageCatches"), pageWalked = o.optLong("pageWalked"), pageSeen = o.optInt("pageSeen", o.optInt("story")), guide = o.optBoolean("guide", true),
                finds = strs("finds").toSet(),
                tasks = o.optJSONArray("tasks")?.let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() } ?: emptySet(),
                pageKinds = o.optInt("pageKinds"),
                week = o.optLong("week", -1), weekPaid = o.optBoolean("weekPaid"),
                stops = o.optJSONArray("stops")?.let { a -> (0 until a.length()).mapNotNull { i ->
                    val p = a.getJSONObject(i)
                    runCatching { app.fieldscan.domain.Stop(p.getString("id"), app.fieldscan.domain.Habitat.valueOf(p.getString("h")), Geo(p.getDouble("y"), p.getDouble("x")), p.optString("n"), p.optBoolean("d")) }.getOrNull()
                } } ?: emptyList(),
                findAt = o.optJSONObject("findAt")?.let { f -> f.keys().asSequence().associateWith { k -> f.getJSONArray(k).let { Geo(it.getDouble(0), it.getDouble(1)) } } } ?: emptyMap(),
                healthSeen = o.optJSONObject("healthSeen")?.let { h -> h.keys().asSequence().associate { it.toLong() to h.getLong(it) } } ?: emptyMap(),
            )
        }
    }
}
