package app.fieldscan.domain

import kotlin.math.abs

/** One GPS fix for the trackers: where, how sure (metres), when (ms). */
data class Pt(val at: Geo, val acc: Double, val time: Long)

/** How close counts as "at" a point, given the GPS accuracy. */
fun reach(acc: Double) = maxOf(6.0, minOf(acc, 12.0))

/** Walking round [centre]: the signed angle swept (clockwise positive), ignoring fixes too near, too far or too vague. */
class CircleTrack(private val centre: Geo, private val maxR: Double = 70.0) {
    var total = 0.0; private set
    private var prev: Double? = null
    private var prevAt: Geo? = null
    fun add(p: Pt) {
        val d = p.at.dist(centre)
        // too close to the centre the bearing is all noise; standing still isn't walking round
        if (p.acc > 25 || d < maxOf(4.0, p.acc * .8) || d > maxR) return
        if (prevAt?.let { it.dist(p.at) < 2.0 } == true) return
        val a = centre.bearing(p.at)
        prev?.let { val dt = Geo.turn(it, a); if (abs(dt) < 90) total += dt }
        prev = a; prevAt = p.at
    }
    fun rounds(ccw: Boolean) = (if (ccw) -total else total) / 360.0
}

/**
 * Steps without a stop, from the phone's step counter (a running total since reboot): counted from the first
 * reading; no new step for [stallMs] once walking has begun, and the count starts over (#2: the barometer and
 * GPS climbs failed on many phones).
 */
class StepsTrack(private val target: Int, private val stallMs: Long = 25_000) {
    private var base: Long? = null
    private var last = 0L
    private var lastAt = 0L
    var count = 0; private set
    var stalled = false; private set
    var done = false; private set

    fun add(total: Long, now: Long) {
        if (done) return
        val b = base
        if (b == null) { base = total; last = total; lastAt = now; return }
        if (total > last) {
            last = total; lastAt = now; stalled = false
            count = (total - b).toInt()
            if (count >= target) done = true
        }
    }

    /** Called on a timer, so standing still is noticed without new readings. */
    fun tick(now: Long) {
        if (done || count == 0) return
        if (now - lastAt > stallMs) { base = last; count = 0; stalled = true }
    }
}

/** Distance walked, counting only hops a walker could make (≤ 3 m/s) and, if asked, only near water. */
class WalkTrack(private val water: List<Geo>? = null) {
    var metres = 0.0; private set
    var off = false; private set
    private var last: Pt? = null
    fun add(p: Pt) {
        if (p.acc > 30) return
        if (water != null) { off = water.none { it.dist(p.at) < 250 }; if (off) { last = p; return } }
        val l = last
        if (l != null) {
            val d = l.at.dist(p.at)
            val most = maxOf(8.0, (p.time - l.time) / 1000.0 * 3.0)
            if (d in 2.0..most) metres += d
        }
        if (l == null || l.at.dist(p.at) >= 2) last = p
    }
}
