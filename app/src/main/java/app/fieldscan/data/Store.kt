package app.fieldscan.data

import android.content.Context
import app.fieldscan.domain.Catch
import app.fieldscan.domain.Daypart
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Item
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
            )
        }
    }
}
