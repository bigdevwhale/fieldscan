package app.fieldscan.render

import kotlin.math.floor

/** Park–Miller generator, the same sequence as the design prototype. */
class Rng(seed: Long) {
    private var s: Long = ((seed % 2147483647L) + 2147483647L) % 2147483647L
    init { if (s <= 0) s += 2147483646L }
    fun next(): Double { s = s * 16807 % 2147483647L; return s.toDouble() / 2147483647.0 }
    fun f(): Float = next().toFloat()
    fun range(a: Float, b: Float) = a + (b - a) * f()
}

/** Smooth value noise on a hashed lattice, 0..1. */
class Noise(seed: Long) {
    private val p = FloatArray(512).also { val r = Rng(seed); for (i in it.indices) it[i] = r.f() }
    private fun h(x: Int, y: Int) = p[((x * 73856093) xor (y * 19349663)) and 511]
    operator fun invoke(x: Float, y: Float): Float {
        val xi = floor(x).toInt(); val yi = floor(y).toInt()
        var xf = x - xi; var yf = y - yi
        xf = xf * xf * (3 - 2 * xf); yf = yf * yf * (3 - 2 * yf)
        val a = h(xi, yi); val b = h(xi + 1, yi); val c = h(xi, yi + 1); val d = h(xi + 1, yi + 1)
        return a + (b - a) * xf + (c - a) * yf + (a - b - c + d) * xf * yf
    }
    fun fbm(x: Float, y: Float, oct: Int = 3): Float {
        var v = 0f; var a = .5f; var f = 1f; var norm = 0f
        repeat(oct) { v += a * this(x * f, y * f); norm += a; a *= .5f; f *= 2.1f }
        return v / norm
    }
}
