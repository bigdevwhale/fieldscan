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
 * A climb from [a] (bottom) to [b] (top): starts at the bottom, counts the best progress, and is reset when
 * there's no progress for [stallMs]. Done when most of the way is covered and the top is reached.
 */
class ClimbTrack(private val a: Geo, private val b: Geo, private val stallMs: Long = 25_000) {
    private val len = a.dist(b).coerceAtLeast(10.0)
    var started = false; private set
    var stalled = false; private set
    var best = 0.0; private set
    var done = false; private set
    private var lastGain = 0L

    fun add(p: Pt) {
        if (done) return
        if (started && p.time - lastGain > stallMs && best < .85) { started = false; stalled = true }
        if (!started) {
            if (p.at.dist(a) <= reach(p.acc) && p.at.dist(a) < p.at.dist(b)) { started = true; stalled = false; best = 0.0; lastGain = p.time }
            return
        }
        val prog = (1 - p.at.dist(b) / len).coerceIn(0.0, 1.0)
        if (prog > best + .03) { best = prog; lastGain = p.time }
        if (best >= .85 && p.at.dist(b) <= reach(p.acc)) done = true
    }

    /** Called on a timer too, so standing still is noticed without new fixes. */
    fun tick(now: Long) { if (started && !done && now - lastGain > stallMs && best < .85) { started = false; stalled = true } }
}

/**
 * Height gained without stopping, from altitudes in metres (the barometer's): counted from the lowest point
 * since the last stop. No gain of half a metre for [stallMs] once the climb has begun, and it starts over
 * from where you stand. Going down moves the start down with you.
 */
class RiseTrack(private val target: Double, private val stallMs: Long = 25_000) {
    private var base: Double? = null
    private var top = 0.0
    private var lastGain = 0L
    var gained = 0.0; private set
    var stalled = false; private set
    var done = false; private set

    fun add(alt: Double, time: Long) {
        if (done) return
        val b = base
        if (b == null || alt < b) { base = alt; top = 0.0; gained = 0.0; lastGain = time; return }
        val g = alt - b
        if (g > top + .5) { top = g; lastGain = time; stalled = false }
        gained = maxOf(0.0, g)
        if (top >= target) { done = true; return }
        if (top >= 1.0 && time - lastGain > stallMs) { base = alt; top = 0.0; gained = 0.0; lastGain = time; stalled = true }
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
