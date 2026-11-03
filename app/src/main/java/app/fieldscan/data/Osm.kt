package app.fieldscan.data

import app.fieldscan.domain.Geo
import app.fieldscan.domain.Habitat
import app.fieldscan.domain.Place
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * The district from OpenStreetMap: only the things creatures live in. Downloaded in tiles of about 2 km
 * through Overpass and boiled down to [Place]s.
 */
object Osm {
    const val TILE_LAT = 0.02
    const val TILE_LON = 0.035

    data class Tile(val y: Int, val x: Int) {
        val key get() = "${y}_$x"
        val box get() = doubleArrayOf(y * TILE_LAT, x * TILE_LON, (y + 1) * TILE_LAT, (x + 1) * TILE_LON)
        companion object {
            fun of(g: Geo) = Tile(floor(g.lat / TILE_LAT).toInt(), floor(g.lon / TILE_LON).toInt())
        }
    }

    /** The tile under [g] and its eight neighbours: about 6 × 6 km. */
    fun around(g: Geo): List<Tile> {
        val t = Tile.of(g)
        return (-1..1).flatMap { dy -> (-1..1).map { dx -> Tile(t.y + dy, t.x + dx) } }.sortedBy { abs(it.y - t.y) + abs(it.x - t.x) }
    }

    fun query(b: DoubleArray): String {
        val bb = "%.5f,%.5f,%.5f,%.5f".format(Locale.US, b[0], b[1], b[2], b[3])
        return """
            [out:json][timeout:90][bbox:$bb];
            (
             way["bridge"]["bridge"!="no"]["highway"];
             way["waterway"~"^(river|stream|canal)$"];
             way["highway"="steps"];
             nwr["amenity"~"^(fountain|drinking_water|pharmacy|place_of_worship|post_box|bench)$"];
             nwr["natural"="spring"]; nwr["man_made"~"^(water_well|water_tower|tower|lighthouse|pier)$"];
             node["highway"~"^(bus_stop|street_lamp)$"];
             node["natural"="tree"];
             nwr["leisure"="playground"];
             nwr["historic"~"^(memorial|monument)$"];
             nwr["tourism"="viewpoint"];
             way["highway"~"^(footway|path|pedestrian)$"]["tunnel"="yes"];
             way["highway"~"^(primary|secondary|tertiary|residential|unclassified|living_street|pedestrian)$"];
            );
            out body geom qt;
        """.trimIndent()
    }

    private const val AGENT = "Fieldscan/0.1 (github.com/bigdevwhale/fieldscan)"
    private val SERVERS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
    )

    /** Raw Overpass JSON for a box. Only the box is sent. */
    fun download(box: DoubleArray): String {
        val body = "data=" + URLEncoder.encode(query(box), "UTF-8")
        var last: Throwable? = null
        for (srv in SERVERS) {
            val r = runCatching {
                val c = URL(srv).openConnection() as HttpURLConnection
                c.connectTimeout = 15_000; c.readTimeout = 75_000
                c.setRequestProperty("User-Agent", AGENT)
                c.requestMethod = "POST"; c.doOutput = true
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                c.outputStream.use { it.write(body.toByteArray()) }
                try {
                    if (c.responseCode !in 200..299) error("HTTP ${c.responseCode} at ${URL(srv).host}")
                    c.inputStream.bufferedReader().use { it.readText() }.also { if (!it.trimStart().startsWith("{")) error("not JSON") }
                } finally { c.disconnect() }
            }
            if (r.isSuccess) return r.getOrThrow()
            last = r.exceptionOrNull()
        }
        throw last ?: IllegalStateException("no server")
    }

    private class Way(val id: Long, val tags: Map<String, String>, val nodes: LongArray, val pts: List<Geo>)

    /** Turns an Overpass dump into places. Only places whose point lies inside [box] are kept (tiles don't overlap). */
    fun parse(json: String, box: DoubleArray? = null): List<Place> {
        val els = JSONObject(json).getJSONArray("elements")
        val out = ArrayList<Place>()
        val ways = ArrayList<Way>()
        fun tags(e: JSONObject): Map<String, String> {
            val t = e.optJSONObject("tags") ?: return emptyMap()
            return t.keys().asSequence().associateWith { t.optString(it) }
        }
        fun geom(e: JSONObject): List<Geo> {
            val g = e.optJSONArray("geometry") ?: return emptyList()
            return (0 until g.length()).mapNotNull { i -> g.optJSONObject(i)?.let { Geo(it.getDouble("lat"), it.getDouble("lon")) } }
        }
        fun centre(e: JSONObject): Geo? {
            if (e.has("lat")) return Geo(e.getDouble("lat"), e.getDouble("lon"))
            e.optJSONObject("center")?.let { return Geo(it.getDouble("lat"), it.getDouble("lon")) }
            val p = geom(e)
            if (p.isNotEmpty()) return Geo(p.sumOf { it.lat } / p.size, p.sumOf { it.lon } / p.size)
            e.optJSONObject("bounds")?.let { return Geo((it.getDouble("minlat") + it.getDouble("maxlat")) / 2, (it.getDouble("minlon") + it.getDouble("maxlon")) / 2) }
            return null
        }
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            val t = tags(e)
            if (t.isEmpty()) continue
            val type = e.getString("type")
            val id = type.first() + e.getLong("id").toString()
            if (type == "way") {
                val ns = e.optJSONArray("nodes")
                ways += Way(e.getLong("id"), t, LongArray(ns?.length() ?: 0) { ns!!.getLong(it) }, geom(e))
            }
            val name = t["name"].orEmpty()
            val h: Habitat? = when {
                t["amenity"] == "fountain" || t["amenity"] == "drinking_water" || t["natural"] == "spring" || t["man_made"] == "water_well" -> Habitat.SPRING
                t["amenity"] == "pharmacy" -> Habitat.PHARMACY
                t["amenity"] == "place_of_worship" -> Habitat.WORSHIP
                t["amenity"] == "post_box" -> Habitat.POSTBOX
                t["amenity"] == "bench" -> Habitat.BENCH
                t["highway"] == "bus_stop" -> Habitat.STOP
                t["highway"] == "street_lamp" -> Habitat.LAMP
                t["natural"] == "tree" -> Habitat.TREE
                t["leisure"] == "playground" -> Habitat.PLAYGROUND
                t["historic"] == "memorial" || t["historic"] == "monument" -> Habitat.MEMORIAL
                t["tourism"] == "viewpoint" -> Habitat.VIEWPOINT
                t["man_made"] in setOf("water_tower", "tower", "lighthouse") -> Habitat.TOWER
                t["man_made"] == "pier" -> Habitat.PIER
                type == "way" && t["tunnel"] == "yes" && t["highway"] in setOf("footway", "path", "pedestrian") -> Habitat.UNDERPASS
                else -> null
            }
            if (h != null) centre(e)?.let { g ->
                val year = t["start_date"]?.let { Regex("\\d{4}").find(it)?.value?.toIntOrNull() }
                // a tree that's a listed natural monument counts as very old even without a date
                val y = year ?: if (h == Habitat.TREE && (t["denotation"] == "natural_monument" || t["protected"] == "yes")) 1700 else null
                // for trees, size is how big it is: girth in cm, else height in m — the oldest one is the keeper's
                val big = if (h == Habitat.TREE) (t["circumference"]?.let { num(it) }?.let { if (it < 20) it * 100 else it }
                    ?: t["diameter_crown"]?.let { num(it) }?.times(10) ?: t["height"]?.let { num(it) } ?: 0.0) else 0.0
                out += Place(id, h, g, name, year = y, size = big)
            }
        }
        out += bridges(ways)
        out += stairs(ways)
        out += crossings(ways)
        out += waterside(ways)
        return if (box == null) out else out.filter { it.at.lat >= box[0] && it.at.lat < box[2] && it.at.lon >= box[1] && it.at.lon < box[3] }
    }

    private fun num(s: String) = Regex("[0-9]+([.,][0-9]+)?").find(s)?.value?.replace(',', '.')?.toDoubleOrNull()

    private fun len(p: List<Geo>) = p.zipWithNext().sumOf { (a, b) -> a.dist(b) }

    /** Bridges that cross running water, with the bearing the water flows (OSM draws waterways downstream). */
    private fun bridges(ways: List<Way>): List<Place> {
        val water = ways.filter { it.tags["waterway"] in setOf("river", "stream", "canal") && it.pts.size >= 2 }
        val out = ArrayList<Place>()
        for (w in ways) {
            val b = w.tags["bridge"] ?: continue
            if (b == "no" || "highway" !in w.tags || w.pts.size < 2) continue
            val origin = w.pts[0]
            var flow: Double? = null
            loop@ for (r in water) for ((p, q) in r.pts.zipWithNext()) for ((u, v) in w.pts.zipWithNext()) {
                if (cross(u.xy(origin), v.xy(origin), p.xy(origin), q.xy(origin))) { flow = p.bearing(q); break@loop }
            }
            if (flow == null) continue
            out += Place("w${w.id}", Habitat.BRIDGE, Geo(w.pts.sumOf { it.lat } / w.pts.size, w.pts.sumOf { it.lon } / w.pts.size),
                w.tags["name"].orEmpty(), a = w.pts.first(), b = w.pts.last(), flow = flow, size = len(w.pts))
        }
        return out
    }

    /** Staircases long enough to climb: bottom first (OSM incline=up means drawn upwards). */
    private fun stairs(ways: List<Way>): List<Place> = ways.filter { it.tags["highway"] == "steps" && it.pts.size >= 2 }.mapNotNull { w ->
        val count = w.tags["step_count"]?.toIntOrNull()
        val l = len(w.pts)
        if ((count ?: 0) < 30 && l < 25) return@mapNotNull null
        val down = w.tags["incline"]?.let { it == "down" || it.startsWith("-") } ?: false
        val a = if (down) w.pts.last() else w.pts.first()
        val b = if (down) w.pts.first() else w.pts.last()
        Place("w${w.id}", Habitat.STEPS, Geo((a.lat + b.lat) / 2, (a.lon + b.lon) / 2), w.tags["name"].orEmpty(), a = a, b = b,
            size = (count ?: (l / 0.3).toInt()).toDouble())
    }

    /** Nodes where five or more road arms meet. */
    private fun crossings(ways: List<Way>): List<Place> {
        val roads = ways.filter { it.tags["highway"] in setOf("primary", "secondary", "tertiary", "residential", "unclassified", "living_street", "pedestrian") && it.tags["tunnel"] == null }
        val arms = HashMap<Long, Int>()
        val at = HashMap<Long, Geo>()
        for (w in roads) {
            if (w.nodes.size != w.pts.size) continue
            for (i in w.nodes.indices) {
                val n = w.nodes[i]
                arms[n] = (arms[n] ?: 0) + if (i == 0 || i == w.nodes.lastIndex) 1 else 2
                at[n] = w.pts[i]
            }
        }
        return arms.filter { it.value >= 5 }.map { (n, k) -> Place("n$n", Habitat.CROSSING, at.getValue(n), ways = k) }
    }

    /** Points every 400 m along rivers and canals: where waterside creatures and the fog come. */
    private fun waterside(ways: List<Way>): List<Place> {
        val out = ArrayList<Place>()
        for (w in ways) {
            if (w.tags["waterway"] !in setOf("river", "canal") || w.pts.size < 2) continue
            var acc = 0.0; var k = 0
            for ((p, q) in w.pts.zipWithNext()) {
                val d = p.dist(q)
                while (acc + d >= (k + 1) * 400.0 - 200) {
                    val f = (((k + 1) * 400.0 - 200) - acc) / d
                    if (f in 0.0..1.0) out += Place("w${w.id}#$k", Habitat.WATER, Geo(p.lat + (q.lat - p.lat) * f, p.lon + (q.lon - p.lon) * f), w.tags["name"].orEmpty(), flow = p.bearing(q))
                    k++
                }
                acc += d
            }
        }
        return out
    }

    private fun cross(a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>, d: Pair<Double, Double>): Boolean {
        fun o(p: Pair<Double, Double>, q: Pair<Double, Double>, r: Pair<Double, Double>) = (q.first - p.first) * (r.second - p.second) - (q.second - p.second) * (r.first - p.first)
        val d1 = o(c, d, a); val d2 = o(c, d, b); val d3 = o(a, b, c); val d4 = o(a, b, d)
        return d1 * d2 < 0 && d3 * d4 < 0
    }

    // ——— compact cache form ———
    fun toJson(ps: List<Place>): String {
        val a = JSONArray()
        for (p in ps) {
            val o = JSONObject().put("i", p.id).put("h", p.habitat.name).put("y", p.at.lat).put("x", p.at.lon)
            if (p.name.isNotEmpty()) o.put("n", p.name)
            p.a?.let { o.put("ay", it.lat).put("ax", it.lon) }
            p.b?.let { o.put("by", it.lat).put("bx", it.lon) }
            p.flow?.let { o.put("f", it) }
            if (p.size != 0.0) o.put("s", p.size)
            p.year?.let { o.put("yr", it) }
            if (p.ways != 0) o.put("w", p.ways)
            a.put(o)
        }
        return a.toString()
    }

    fun fromJson(s: String): List<Place> {
        val a = JSONArray(s)
        return (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            val h = runCatching { Habitat.valueOf(o.getString("h")) }.getOrNull() ?: return@mapNotNull null
            Place(
                o.getString("i"), h, Geo(o.getDouble("y"), o.getDouble("x")), o.optString("n"),
                a = if (o.has("ay")) Geo(o.getDouble("ay"), o.getDouble("ax")) else null,
                b = if (o.has("by")) Geo(o.getDouble("by"), o.getDouble("bx")) else null,
                flow = if (o.has("f")) o.getDouble("f") else null,
                size = o.optDouble("s", 0.0), year = if (o.has("yr")) o.getInt("yr") else null, ways = o.optInt("w", 0),
            )
        }
    }
}
