package app.fieldscan.domain

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Part of the day by the sun's height. */
enum class Daypart { NIGHT, DAWN, DAY, DUSK }

/**
 * The sun and the moon, computed on the phone (NOAA's simplified solar position, a mean-elements moon):
 * what rites need — is it after sunset, dawn now, which way does a shadow fall, how full is the moon.
 */
object Sky {
    private fun jd(ms: Long) = ms / 86_400_000.0 + 2_440_587.5

    /** Sun altitude and azimuth in degrees (azimuth 0 = north, clockwise). */
    fun sun(at: Long, g: Geo): DoubleArray {
        val d = jd(at) - 2_451_545.0
        val l = Math.toRadians((280.460 + 0.9856474 * d) % 360)
        val m = Math.toRadians((357.528 + 0.9856003 * d) % 360)
        val lambda = l + Math.toRadians(1.915) * sin(m) + Math.toRadians(0.020) * sin(2 * m)
        val eps = Math.toRadians(23.439 - 0.0000004 * d)
        val ra = atan2(cos(eps) * sin(lambda), cos(lambda))
        val dec = asin(sin(eps) * sin(lambda))
        return altAz(d, ra, dec, g)
    }

    private fun altAz(d: Double, ra: Double, dec: Double, g: Geo): DoubleArray {
        val gmst = Math.toRadians((280.46061837 + 360.98564736629 * d) % 360)
        val ha = gmst + Math.toRadians(g.lon) - ra
        val lat = Math.toRadians(g.lat)
        val alt = asin(sin(lat) * sin(dec) + cos(lat) * cos(dec) * cos(ha))
        val az = atan2(sin(ha), cos(ha) * sin(lat) - tan(dec) * cos(lat))
        return doubleArrayOf(Math.toDegrees(alt), (Math.toDegrees(az) + 180 + 360) % 360)
    }

    fun daypart(at: Long, g: Geo): Daypart {
        val alt = sun(at, g)[0]
        val later = sun(at + 20 * 60_000, g)[0]
        return when {
            alt < -6 -> Daypart.NIGHT
            alt < 4 -> if (later > alt) Daypart.DAWN else Daypart.DUSK
            else -> Daypart.DAY
        }
    }

    /** True after sunset and before sunrise (sun below the horizon). */
    fun dark(at: Long, g: Geo) = sun(at, g)[0] < -0.8

    /** Moon age in days (0 new, ~14.8 full) and lit fraction 0..1. */
    fun moon(at: Long): DoubleArray {
        val age = ((jd(at) - 2_451_550.1) % 29.530588853 + 29.530588853) % 29.530588853
        val frac = (1 - cos(2 * Math.PI * age / 29.530588853)) / 2
        return doubleArrayOf(age, frac)
    }

    /** Moon altitude and azimuth (accurate to a degree or two: enough for "is it up", "where's the shadow"). */
    fun moonPos(at: Long, g: Geo): DoubleArray {
        val d = jd(at) - 2_451_545.0
        val l = Math.toRadians((218.316 + 13.176396 * d) % 360)
        val m = Math.toRadians((134.963 + 13.064993 * d) % 360)
        val f = Math.toRadians((93.272 + 13.229350 * d) % 360)
        val lon = l + Math.toRadians(6.289) * sin(m)
        val lat = Math.toRadians(5.128) * sin(f)
        val eps = Math.toRadians(23.4397)
        val ra = atan2(sin(lon) * cos(eps) - tan(lat) * sin(eps), cos(lon))
        val dec = asin(sin(lat) * cos(eps) + cos(lat) * sin(eps) * sin(lon))
        return altAz(d, ra, dec, g)
    }

    fun fullMoon(at: Long) = moon(at)[1] > 0.96

    /** A moon glyph for the status line. */
    fun moonGlyph(at: Long): String {
        val age = moon(at)[0]
        return when {
            age < 1.8 || age > 27.7 -> "○"
            age < 12.9 -> "◐"
            age < 16.6 -> "●"
            else -> "◑"
        }
    }

    /** Minutes from now to the next sunrise or sunset crossing, for "sleeps until ..." lines. */
    fun nextCross(at: Long, g: Geo, up: Boolean): Long? {
        var t = at
        var prev = sun(t, g)[0]
        repeat(36 * 6) {
            t += 10 * 60_000
            val a = sun(t, g)[0]
            if (up && prev < -0.8 && a >= -0.8) return (t - at) / 60_000
            if (!up && prev >= -0.8 && a < -0.8) return (t - at) / 60_000
            prev = a
        }
        return null
    }

    @Suppress("unused") private fun wrap(x: Double) = x - 360 * floor(x / 360)
    @Suppress("unused") private fun acosd(x: Double) = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
}
