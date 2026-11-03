package app.fieldscan.render

import app.fieldscan.domain.Body
import app.fieldscan.domain.Shape

/** One joint of a bone: position (ground at y = 0, up is negative) and thickness. */
class J(val x: Float, val y: Float, val r: Float)

/** A creature's skeleton in local units: bones, eyes, an optional glowing head; [h] is its height. */
class Skeleton(val bones: List<List<J>>, val eyes: List<J>, val glow: J?, val h: Float, val w: Float)

/**
 * Grows a skeleton from a [Body]: each shape has a hand-tuned base pose (taken from the design frames),
 * bent and stretched by the body's proportions and a little by the seed, so no two look the same.
 */
object Skeletons {
    fun of(b: Body, seed: Long): Skeleton {
        val r = Rng(seed + 17)
        val j = { x: Float, y: Float, rr: Float -> J(x + r.range(-2f, 2f), y + r.range(-2f, 2f), rr * r.range(.9f, 1.1f)) }
        val bones = ArrayList<List<J>>()
        val eyes = ArrayList<J>()
        var glow: J? = null
        fun bone(vararg p: Triple<Float, Float, Float>) { bones += p.map { j(it.first, it.second, it.third) } }
        fun t(x: Number, y: Number, rr: Number) = Triple(x.toFloat(), y.toFloat(), rr.toFloat())
        val a = b.arms
        val lean = b.lean
        // horns / ears on a head at (hx, hy) with radius hr
        fun adorn(hx: Float, hy: Float, hr: Float) {
            if (b.horns >= 1) bone(t(hx - hr * .4f, hy - hr * .8f, 5), t(hx - hr * .9f, hy - hr * 1.9f, 3.2), t(hx - hr * 1.3f, hy - hr * 2.6f, 1.4))
            if (b.horns >= 2) bone(t(hx + hr * .4f, hy - hr * .8f, 5), t(hx + hr * .9f, hy - hr * 1.8f, 3), t(hx + hr * 1.4f, hy - hr * 2.3f, 1.2))
            if (b.ears >= 1) bone(t(hx - hr * .6f, hy - hr * .7f, 5.5), t(hx - hr * 1.1f, hy - hr * 1.8f, 4.5), t(hx - hr * 1.4f, hy - hr * 2.4f, 2))
            if (b.ears >= 2) bone(t(hx + hr * .6f, hy - hr * .7f, 5.5), t(hx + hr * 1.1f, hy - hr * 1.8f, 4.5), t(hx + hr * 1.3f, hy - hr * 2.3f, 2))
        }
        fun eyesAt(hx: Float, hy: Float, gap: Float) {
            when (b.eyes) {
                0 -> Unit
                1 -> eyes += J(hx, hy, 2.4f)
                2 -> { eyes += J(hx - gap / 2, hy, 2.1f); eyes += J(hx + gap / 2, hy + 3, 1.7f) }
                3 -> { eyes += J(hx - gap / 2, hy, 1.9f); eyes += J(hx + gap / 2, hy + 3, 1.4f); eyes += J(hx - gap * .1f, hy + 6, 1.2f) }
                else -> { eyes += J(hx - gap / 2, hy, 1.9f); eyes += J(hx + gap / 2, hy + 2, 1.6f); eyes += J(hx - gap, hy + 7, 1.2f); eyes += J(hx + gap, hy + 8, 1.1f) }
            }
        }
        var h: Float; var w: Float
        when (b.shape) {
            Shape.HUNCH -> {
                bone(t(-24, -68, 30), t(-12, -102, 36), t(8, -128, 36), t(28, -138, 28))
                val hx = 42f + lean * 8; val hy = -132f
                bone(t(30, -136, 20), t(hx, hy, 19), t(hx + 8, hy + 6, 15))
                adorn(hx - 4, hy - 4, 12f)
                bone(t(18, -116, 12), t(18 + 18 * a, -116 + 34 * a, 10), t(18 + 27 * a, -116 + 76 * a, 8), t(18 + 33 * a, -116 + 108 * a, 6), t(18 + 40 * a, -116 + 116 * a, 4))
                val fx = 18 + 33 * a; val fy = -116 + 110 * a
                bone(t(fx, fy, 3), t(fx - 5, fy + 14, 1.6)); bone(t(fx + 4, fy + 2, 3), t(fx + 10, fy + 16, 1.4)); bone(t(fx + 7, fy + 4, 2.6), t(fx + 19, fy + 12, 1.2))
                bone(t(-2, -112, 10), t(-2 - 8 * a, -112 + 40 * a, 8), t(-2 - 12 * a, -112 + 80 * a, 6.5), t(-2 - 8 * a, -112 + 108 * a, 4.5))
                bone(t(-26, -64, 20), t(-41, -34, 15), t(-36, -8, 11), t(-20, 0, 7))
                bone(t(-8, -58, 17), t(-2, -28, 13), t(12, -6, 8))
                eyesAt(hx + 6, hy + 1, 9f)
                h = 160f; w = 140f
            }
            Shape.TALL -> {
                // legs apart, a thin trunk, a shoulder bar, long arms held a little away from the body
                bone(t(-15, 0, 5), t(-12, -58, 6.5), t(-7, -112, 8)); bone(t(15, 0, 5), t(12, -58, 6.5), t(7, -112, 8))
                bone(t(0, -108, 12), t(-1, -165, 13), t(2, -214, 11))
                bone(t(-22, -218, 6.5), t(0, -224, 8), t(24, -217, 6.5))
                val nx = 8f * (1 + lean * 1.5f); val hx = 14f * (1 + lean * 1.4f); val hy = -246f + lean * 10
                bone(t(2, -222, 5), t(nx, -238, 4.5))
                if (b.glow) { bone(t(hx - 6, hy + 4, 9), t(hx, hy - 10, 10.5), t(hx + 2, hy - 18, 5)); glow = J(hx, hy - 8, 26f) }
                else bone(t(hx, hy, 10.5), t(hx + 6, hy + 8, 9))
                adorn(hx + 2, hy, 10f)
                val la = 1 + (a - 1) * 1.4f
                bone(t(-24, -216, 5), t(-36, -216 + 60 * la, 4), t(-38, -216 + 118 * la, 3.2), t(-34, -216 + 142 * la, 2))
                bone(t(-34, -216 + 142 * la, 1.8), t(-40, -216 + 152 * la, 1)); bone(t(-34, -216 + 142 * la, 1.8), t(-30, -216 + 154 * la, 1))
                bone(t(26, -215, 5), t(38, -215 + 62 * la, 4), t(40, -215 + 116 * la, 3.2), t(37, -215 + 140 * la, 2))
                bone(t(37, -215 + 140 * la, 1.8), t(43, -215 + 150 * la, 1)); bone(t(37, -215 + 140 * la, 1.8), t(33, -215 + 152 * la, 1))
                if (!b.glow) eyesAt(hx + 5, hy + 1, 7f)
                h = 265f; w = 90f
            }
            Shape.HAIR -> {
                bone(t(0, 0, 20), t(-2, -36, 19), t(-4, -64, 15))
                bone(t(-5, -80, 16), t(-10, -96, 15))
                bone(t(-18, -48, 6.5), t(-44, -24, 5.5), t(-60, -12, 4.5), t(-70, -6, 2.6))
                bone(t(12, -48, 6.5), t(38, -26, 5.5), t(54, -12, 4.5), t(66, -7, 2.6))
                for (i in 0 until 16) {
                    val x0 = -26 + i * 2.6f + r.f() * 2; val len = 70 + r.f() * 55
                    val s = ArrayList<J>()
                    var k = 0f
                    while (k <= 1f) { s += J(x0 + (r.f() - .5f) * 4 + kotlin.math.sin(k * 5 + i) * 3 + (x0 + 7) * k * .5f, -102 + k * len, 2.4f * (1 - k) + .7f); k += .1f }
                    bones += s
                }
                eyesAt(-1f, -84f, 9f)
                h = 120f; w = 140f
            }
            Shape.CRAWLER -> {
                bone(t(-90, -34, 13), t(-45, -70, 16), t(-2, -106, 14))
                val hx = 20f; val hy = -122f
                bone(t(8, -114, 11), t(hx, hy, 9), t(hx + 6, hy - 2, 6))
                adorn(hx, hy, 9f)
                listOf(-80f to -40f, -60f to -54f, -38f to -72f, -18f to -88f, -4f to -100f).forEachIndexed { i, (x, y) ->
                    val up = if (i % 2 == 0) -1 else 1
                    bone(t(x, y, 3.4), t(x - 10 + i * 3, y - 20 * up - 6, 2.6), t(x - 4 + i * 4, y + 22, 1.8), t(x + 6 + i * 2, y + 30, 1))
                }
                eyesAt(hx, hy - 2, 7f)
                h = 130f; w = 130f
            }
            Shape.SMALL -> {
                bone(t(2, -8, 17), t(-2, -30, 16), t(-6, -48, 13))
                val hx = -10f; val hy = -64f
                bone(t(-8, -60, 11), t(hx - 4, hy - 6, 8))
                adorn(hx - 4, hy - 4, 8f)
                if (b.tail) bone(t(12, -2, 3), t(36, 4, 2.2), t(58, -2, 1.4), t(70, -12, .8))
                bone(t(-14, -32, 3), t(-26 * a, -22, 2.2), t(-30 * a, -16, 1.2)); bone(t(4, -32, 3), t(14 * a, -20, 2.2), t(18 * a, -14, 1.2))
                eyesAt(hx - 2, hy - 2, 8f)
                h = 95f; w = 80f
            }
            Shape.FOG -> {
                bone(t(-60, -70, 50), t(-10, -105, 60), t(50, -98, 52), t(100, -75, 40))
                bone(t(-10, -120, 40), t(10, -150, 34))
                bone(t(-90, -50, 30), t(-120, -34, 20)); bone(t(110, -70, 26), t(156, -58, 16))
                eyesAt(14f, -142f, 16f)
                h = 190f; w = 320f
            }
            Shape.QUAD -> {
                bone(t(-40, -60, 18), t(0, -64, 20), t(36, -62, 17))
                val hx = 70f; val hy = -88f
                bone(t(50, -72, 12), t(64, -86, 13), t(hx + 6, hy, 9))
                bone(t(hx + 12, hy + 4, 6), t(hx + 22, hy + 6, 3))
                adorn(hx - 2, hy - 2, 10f)
                listOf(-34f, -20f, 26f, 40f).forEach { x -> bone(t(x, -50, 7), t(x + r.range(-4f, 4f), -24, 5), t(x, 0, 4)) }
                if (b.tail) bone(t(-56, -64, 5), t(-74, -76, 3), t(-88, -80, 1.5))
                eyesAt(hx + 10, hy - 1, 8f)
                h = 120f; w = 190f
            }
            Shape.GNARL -> {
                bone(t(0, 0, 16), t(-4, -50, 14), t(4, -100, 12), t(0, -140, 10))
                bone(t(2, -150, 14))
                bone(t(-6, -110, 6), t(-30, -130, 4), t(-44, -160, 2.5), t(-50, -176, 1)); bone(t(-40, -152, 2), t(-60, -150, 1))
                bone(t(8, -104, 6), t(34, -118, 4), t(50, -146, 2.5), t(54, -160, 1))
                bone(t(-6, -6, 6), t(-26, 4, 4), t(-40, 8, 2)); bone(t(6, -6, 6), t(28, 4, 4), t(42, 10, 2))
                adorn(2f, -156f, 10f)
                eyesAt(2f, -152f, 12f)
                h = 190f; w = 110f
            }
            Shape.PERCH -> {
                bone(t(0, -30, 22), t(-4, -60, 24), t(0, -86, 18))
                val hx = 10f; val hy = -104f
                bone(t(6, -100, 13), t(hx + 4, hy - 2, 10))
                bone(t(hx + 10, hy, 5), t(hx + 22, hy + 4, 2))
                adorn(hx, hy - 2, 9f)
                bone(t(-20, -70, 10), t(-34, -40, 7), t(-40, -10, 3)); bone(t(20, -70, 9), t(28, -40, 6), t(30, -14, 3))
                bone(t(-6, -10, 3), t(-8, 0, 2)); bone(t(6, -10, 3), t(8, 0, 2))
                eyesAt(hx + 5, hy - 2, 7f)
                h = 125f; w = 90f
            }
            Shape.SQUAT -> {
                bone(t(0, -28, 28), t(0, -50, 30), t(2, -64, 24))
                val hx = 2f; val hy = -76f
                bone(t(hx, hy, 20))
                adorn(hx, hy - 6, 16f)
                bone(t(-26, -54, 7), t(-26 - 14 * a, -54 + 24 * a, 6), t(-26 - 20 * a, -54 + 46 * a, 4))
                bone(t(26, -54, 7), t(26 + 14 * a, -54 + 24 * a, 6), t(26 + 20 * a, -54 + 46 * a, 4))
                bone(t(-14, -10, 9), t(-16, 0, 6)); bone(t(14, -10, 9), t(16, 0, 6))
                eyesAt(hx + 1, hy - 2, 14f)
                h = 105f; w = 110f
            }
        }
        return Skeleton(bones, eyes, glow, h, w)
    }
}
