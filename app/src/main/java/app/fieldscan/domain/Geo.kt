package app.fieldscan.domain

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on the ground. */
data class Geo(val lat: Double, val lon: Double) {
    /** Metres to [o] (haversine). */
    fun dist(o: Geo): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(o.lat - lat)
        val dLon = Math.toRadians(o.lon - lon)
        val a = sin(dLat / 2).let { it * it } + cos(Math.toRadians(lat)) * cos(Math.toRadians(o.lat)) * sin(dLon / 2).let { it * it }
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Compass bearing to [o], 0..360, 0 = north. */
    fun bearing(o: Geo): Double {
        val p1 = Math.toRadians(lat); val p2 = Math.toRadians(o.lat)
        val dl = Math.toRadians(o.lon - lon)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /** The point [m] metres away along [bearing]. */
    fun offset(bearing: Double, m: Double): Geo {
        val b = Math.toRadians(bearing)
        val dLat = m * cos(b) / 110_574.0
        val dLon = m * sin(b) / (111_320.0 * cos(Math.toRadians(lat)))
        return Geo(lat + dLat, lon + dLon)
    }

    /** Local flat metres (east, north) from [origin]: good enough inside a district. */
    fun xy(origin: Geo): Pair<Double, Double> =
        (lon - origin.lon) * 111_320.0 * cos(Math.toRadians(origin.lat)) to (lat - origin.lat) * 110_574.0

    companion object {
        /** Signed difference of two bearings, -180..180. */
        fun turn(from: Double, to: Double): Double = ((to - from + 540) % 360) - 180
        fun near(a: Double, b: Double, tol: Double) = abs(turn(a, b)) <= tol

        fun side(b: Double, ru: Boolean): String {
            val i = (((b % 360) + 360) % 360 / 45.0 + 0.5).toInt() % 8
            return (if (ru) listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ") else listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"))[i]
        }
    }
}

/** Small deterministic hashing: a place always holds the same creature, a frame always has the same grain. */
object Hash {
    fun of(s: String): Long {
        var h = -0x340d631b7bdddcdbL
        for (c in s) { h = h xor c.code.toLong(); h *= 0x100000001b3L }
        return h
    }
    fun mix(a: Long, b: Long): Long {
        var x = a * -0x61c8864680b583ebL + b
        x = (x xor (x ushr 33)) * -0xae502812aa7333L
        x = (x xor (x ushr 33)) * -0x3b314601e57a13adL
        return x xor (x ushr 33)
    }
    /** 0..1 */
    fun unit(a: Long, b: Long = 0): Double = (mix(a, b) ushr 11).toDouble() / (1L shl 53).toDouble()
}
