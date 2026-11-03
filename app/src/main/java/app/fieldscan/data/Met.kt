package app.fieldscan.data

import app.fieldscan.domain.Weather
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** The weather right now from MET Norway. Only coordinates rounded to ~1 km are sent. */
object Met {
    private const val AGENT = "Fieldscan/0.1 github.com/bigdevwhale/fieldscan"

    fun fetch(lat: Double, lon: Double, now: Long = System.currentTimeMillis()): Weather {
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.2f&lon=%.2f".format(Locale.US, lat, lon)
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 7000; c.readTimeout = 10_000
        c.setRequestProperty("User-Agent", AGENT)
        val body = try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
        return parse(body, now)
    }

    /** The forecast hour closest to now. */
    fun parse(json: String, now: Long): Weather {
        val ts = JSONObject(json).getJSONObject("properties").getJSONArray("timeseries")
        var best: JSONObject? = null; var bestD = Long.MAX_VALUE
        for (i in 0 until ts.length()) {
            val e = ts.getJSONObject(i)
            val t = java.time.Instant.parse(e.getString("time")).toEpochMilli()
            val d = kotlin.math.abs(t - now)
            if (d < bestD) { bestD = d; best = e }
        }
        val d = best?.getJSONObject("data") ?: return Weather(at = now)
        val inst = d.getJSONObject("instant").getJSONObject("details")
        val sym = d.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: d.optJSONObject("next_6_hours")?.optJSONObject("summary")?.optString("symbol_code") ?: ""
        val rh = inst.optDouble("relative_humidity", 70.0)
        val temp = inst.optDouble("air_temperature", 8.0)
        val fogFrac = inst.optDouble("fog_area_fraction", 0.0)
        return Weather(
            temp = temp,
            fog = sym.startsWith("fog") || fogFrac > 40 || rh >= 98.5,
            rain = "rain" in sym || "sleet" in sym,
            snow = "snow" in sym || ("sleet" in sym && temp < 1),
            wind = inst.optDouble("wind_speed", 2.0),
            windFrom = inst.optDouble("wind_from_direction", 270.0),
            at = now,
        )
    }
}
