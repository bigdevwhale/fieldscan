package app.fieldscan.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import app.fieldscan.domain.Body
import app.fieldscan.domain.Daypart
import app.fieldscan.domain.Habitat
import app.fieldscan.domain.Scene
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Everything a frame depends on. */
data class Shot(
    val body: Body,
    val habitat: Habitat,
    val seed: Long,
    val part: Daypart = Daypart.NIGHT,
    val fog: Boolean = false,
    val rain: Boolean = false,
    val snow: Boolean = false,
    val temp: Double = 6.0,
    val moon: Double = .5,
    val worn: Int = 0,
    val cam: Int = 7,
    val stamp: String = "03/11  03:14:22",
    /** A real photo of the place (W × H) to grow the creature into, instead of a drawn scene. */
    val photo: Bitmap? = null,
)

/** A rendered frame and the square around the creature (for the bestiary thumbnail). */
class Frame(val bitmap: Bitmap, val crop: Rect)

/**
 * The trail camera: a night IR frame of a creature in its place. The creature is grown from bones,
 * its outline torn by domain-warped noise, given fur and texture; the scene is drawn around it and
 * partly in front; then the camera's grain, vignette and data strip.
 */
object TrailCam {
    const val W = 480
    const val H = 360

    var typeface: Typeface? = null

    fun render(s: Shot): Frame {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val drawn = Scenes.of(s.habitat.scene, s.habitat)
        val rng = Rng(s.seed)
        val photo = s.photo
        // in a real photo the creature stands low in the middle, where the viewfinder showed its eyes; nothing is drawn around it
        val scene = if (photo == null) drawn else Scenes.Def(W * .5f, H - 34f, drawn.scale, { _, _ -> }, { _, _ -> })
        if (photo != null) c.drawBitmap(infrared(photo), 0f, 0f, null)
        else {
            // sky
            paintGradient(c, 0f, 0f, 0f, H.toFloat(), intArrayOf(gray(12), gray(29), gray(44)), floatArrayOf(0f, .7f, 1f))
            scene.bg(c, rng)
        }
        // creature
        val sk = Skeletons.of(s.body, s.seed)
        val sc = scene.scale * s.body.size * fitScale(sk, scene, s.body.size)
        val ax = scene.x; val ay = scene.ground
        val layer = creature(s.body, sk, s.seed, sc, ax, ay)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        val soft = blur(layer.pixels, if (s.body.shape == app.fieldscan.domain.Shape.FOG) 4 else 1)
        val img = Bitmap.createBitmap(soft, W, H, Bitmap.Config.ARGB_8888)
        p.alpha = (255 * s.body.alpha).toInt()
        c.drawBitmap(img, 0f, 0f, p)
        p.alpha = (255 * s.body.alpha * .3f).toInt()
        c.drawBitmap(img, 4f, 0f, p)
        scene.fg(c, rng)
        // eyes / glowing head
        for (e in sk.eyes) {
            val ex = ax + e.x * sc; val ey = ay + e.y * sc; val r = max(1.4f, e.r * sc) * 3.2f
            val bright = if (s.part == Daypart.DAY) .55f else 1f
            radial(c, ex, ey, r, intArrayOf(argb(bright, 255, 255, 255), argb(bright * .8f, 232, 244, 226), argb(0f, 232, 244, 226)), floatArrayOf(0f, .35f, 1f))
        }
        sk.glow?.let { g ->
            val gx = ax + g.x * sc; val gy = ay + g.y * sc; val r = g.r * sc * 2.6f
            radial(c, gx, gy, r, intArrayOf(Color.WHITE, Color.rgb(238, 244, 232), argb(.35f, 200, 214, 194), argb(0f, 200, 214, 194)), floatArrayOf(0f, .18f, .45f, 1f))
        }
        weather(c, s, rng)
        // IR flash, grain, vignette
        radial(c, W / 2f, H * .6f, W * .7f, intArrayOf(argb(.10f, 200, 214, 194), argb(0f, 0, 0, 0)), floatArrayOf(0f, 1f))
        post(bmp, s, rng)
        radial(c, W / 2f, H / 2f, W * .62f, intArrayOf(argb(0f, 0, 0, 0), argb(0f, 0, 0, 0), argb(.72f, 0, 0, 0)), floatArrayOf(0f, .48f, 1f))
        hud(c, s)
        // thumbnail square around the creature
        val found = layer.maxX > layer.minX
        val cx = if (found) (layer.minX + layer.maxX) / 2f else ax
        val cy = if (found) (layer.minY + layer.maxY) / 2f else ay - 80f
        val side = if (found) (max(layer.maxX - layer.minX, layer.maxY - layer.minY) * 1.15f).coerceIn(110f, H - 30f) else 200f
        val l = (cx - side / 2).coerceIn(0f, W - side); val t = (cy - side / 2).coerceIn(0f, H - 22 - side)
        return Frame(bmp, Rect(l.toInt(), t.toInt(), (l + side).toInt(), (t + side).toInt()))
    }

    /** A photo as the trail camera's IR sensor sees it: grey with a green cast, flat and a little dark. */
    fun infrared(src: Bitmap): Bitmap {
        val px = IntArray(W * H)
        val b = if (src.width == W && src.height == H) src else Bitmap.createScaledBitmap(src, W, H, true)
        b.getPixels(px, 0, W, 0, 0, W, H)
        for (i in px.indices) {
            val cl = px[i]
            val l = (cl shr 16 and 255) * .3f + (cl shr 8 and 255) * .59f + (cl and 255) * .11f
            // squeeze into the camera's range: blacks lifted, whites dimmed
            val v = (14 + l * .68f).toInt()
            px[i] = gray(v)
        }
        return Bitmap.createBitmap(px, W, H, Bitmap.Config.ARGB_8888)
    }

    /** The middle of [src] at the frame's 4:3, scaled to W × H. */
    fun crop(src: Bitmap): Bitmap {
        val k = minOf(src.width / W.toFloat(), src.height / H.toFloat())
        val w = (W * k).toInt(); val h = (H * k).toInt()
        val part = Bitmap.createBitmap(src, (src.width - w) / 2, (src.height - h) / 2, w, h)
        return Bitmap.createScaledBitmap(part, W, H, true)
    }

    /** Keeps tall creatures inside the frame. */
    private fun fitScale(sk: Skeleton, sc: Scenes.Def, size: Float): Float {
        val room = sc.ground - 30f
        return min(1f, room / (sk.h * sc.scale * size * 1.1f))
    }

    private class Layer(val pixels: IntArray, val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

    private fun creature(b: Body, sk: Skeleton, seed: Long, sc: Float, ax: Float, ay: Float): Layer {
        // 1. bones into a mask
        val mask = FloatArray(W * H)
        val n0 = Noise(seed)
        sk.bones.forEachIndexed { bi, bone ->
            for (i in 0 until bone.size - 1) {
                val p = bone[i]; val q = bone[i + 1]
                val x0 = ax + p.x * sc; val y0 = ay + p.y * sc; val x1 = ax + q.x * sc; val y1 = ay + q.y * sc
                val len = hypot(x1 - x0, y1 - y0).coerceAtLeast(.1f)
                var t = 0f
                while (t <= len) {
                    val k = t / len
                    val r = (p.r + (q.r - p.r) * k) * sc * (.86f + .28f * n0(bi * 7.3f + i + k * 3, bi.toFloat()))
                    disc(mask, x0 + (x1 - x0) * k, y0 + (y1 - y0) * k, max(.6f, r))
                    t += .7f
                }
            }
        }
        // 2. warp: ragged, organic outline
        val n1 = Noise(seed + 1); val n2 = Noise(seed + 2); val n3 = Noise(seed + 3)
        val amp = b.amp; val fine = b.amp * .4f
        val w = FloatArray(W * H)
        var minX = W; var minY = H; var maxX = 0; var maxY = 0
        for (y in 0 until H) for (x in 0 until W) {
            val dx = (n1.fbm(x * .035f, y * .035f) - .5f) * 2 * amp + (n3(x * .3f, y * .3f) - .5f) * 2 * fine
            val dy = (n2.fbm(x * .035f, y * .035f) - .5f) * 2 * amp + (n3(y * .3f + 50, x * .3f) - .5f) * 2 * fine
            val sx = (x + dx).roundToInt(); val sy = (y + dy).roundToInt()
            val v = if (sx < 0 || sy < 0 || sx >= W || sy >= H) 0f else mask[sy * W + sx]
            w[y * W + x] = v
            if (v > .5f) { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y }
        }
        // 3. texture and the flash's light
        val px = IntArray(W * H)
        val n4 = Noise(seed + 9); val r = Rng(seed + 5)
        val litX = ax; val litY = ay - sk.h * sc * .55f
        for (y in 0 until H) for (x in 0 until W) {
            val a = w[y * W + x]; if (a <= 0f) continue
            val tex = (n4.fbm(x * .18f, y * .18f, 2) - .5f) * 28 + (r.f() - .5f) * 10
            val light = max(0f, 1 - hypot(x - litX, y - litY) / 320f) * 30
            val v = (b.tone + tex + light).coerceIn(0f, 255f)
            px[y * W + x] = Color.argb((min(1f, a * 1.3f) * 255).toInt(), v.toInt(), (v * 1.04f).coerceAtMost(255f).toInt(), (v * .96f).toInt())
        }
        // 4. fur: strands from the outline outwards (or hanging down)
        if (b.fur > 0) {
            val bm = Bitmap.createBitmap(px, W, H, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true)
            val c = Canvas(bm)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
            val path = Path()
            for (y in 2 until H - 2) for (x in 2 until W - 2) {
                val a = w[y * W + x]; if (a < .5f) continue
                val gx = w[y * W + x + 1] - w[y * W + x - 1]; val gy = w[(y + 1) * W + x] - w[(y - 1) * W + x]
                val g = hypot(gx, gy); if (g < .3f || r.f() > b.fur) continue
                val nx = -gx / g; val ny = -gy / g
                val len = (2 + r.f() * b.furLen) * sc.coerceAtLeast(.6f)
                val bend = (r.f() - .5f) * .9f
                val v = (b.tone - 6 + r.f() * 20).coerceIn(0f, 255f)
                paint.color = Color.argb(((.5f + r.f() * .5f) * 255).toInt(), v.toInt(), (v * 1.04f).coerceAtMost(255f).toInt(), (v * .96f).toInt())
                paint.strokeWidth = .6f + r.f() * .9f
                path.reset(); path.moveTo(x.toFloat(), y.toFloat())
                if (b.droop) path.quadTo(x + nx * len * .25f + bend * 3, y + len * .5f, x + nx * len * .3f + bend * 5, y + len)
                else path.quadTo(x + nx * len * .5f + bend * 3, y + ny * len * .5f, x + nx * len + bend * 4, y + ny * len)
                c.drawPath(path, paint)
            }
            bm.getPixels(px, 0, W, 0, 0, W, H)
            bm.recycle()
        }
        return Layer(px, minX, minY, maxX, maxY)
    }

    private fun disc(m: FloatArray, cx: Float, cy: Float, r: Float) {
        val x0 = max(0, (cx - r).toInt()); val x1 = min(W - 1, (cx + r).toInt() + 1)
        val y0 = max(0, (cy - r).toInt()); val y1 = min(H - 1, (cy + r).toInt() + 1)
        val rr = r * r
        for (y in y0..y1) { val dy = y - cy; for (x in x0..x1) { val dx = x - cx; if (dx * dx + dy * dy <= rr) m[y * W + x] = 1f } }
    }

    /** Box blur of an ARGB image, [r] passes of radius 1. */
    private fun blur(src: IntArray, r: Int): IntArray {
        var a = src
        repeat(r) {
            val b = IntArray(a.size)
            for (y in 0 until H) for (x in 0 until W) {
                var sa = 0; var sr = 0; var sg = 0; var sb = 0; var n = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val xx = x + dx; val yy = y + dy
                    if (xx < 0 || yy < 0 || xx >= W || yy >= H) continue
                    val c = a[yy * W + xx]; val al = c ushr 24
                    sa += al; sr += (c shr 16 and 255) * al; sg += (c shr 8 and 255) * al; sb += (c and 255) * al; n++
                }
                b[y * W + x] = if (sa == 0) 0 else Color.argb(sa / n, sr / sa, sg / sa, sb / sa)
            }
            a = b
        }
        return a
    }

    private fun weather(c: Canvas, s: Shot, r: Rng) {
        if (s.fog) {
            paintGradient(c, 0f, 0f, 0f, H.toFloat(), intArrayOf(argb(.45f, 190, 200, 186), argb(.22f, 170, 180, 166), argb(.12f, 150, 160, 146)), floatArrayOf(0f, .6f, 1f))
        }
        if (s.rain) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f }
            repeat(260) {
                val x = r.f() * W; val y = r.f() * H; val l = 8 + r.f() * 16
                p.color = argb(.15f + r.f() * .25f, 220, 228, 214)
                c.drawLine(x, y, x - l * .25f, y + l, p)
            }
            repeat(6) { radial(c, r.f() * W, r.f() * H, 6 + r.f() * 14, intArrayOf(argb(.18f, 230, 236, 226), argb(0f, 230, 236, 226)), floatArrayOf(0f, 1f)) }
        }
        if (s.snow) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            repeat(220) {
                val big = r.f() < .15f
                p.color = argb(.5f + r.f() * .5f, 236, 240, 232)
                c.drawCircle(r.f() * W, r.f() * H, if (big) 2.5f + r.f() * 3 else .8f + r.f() * 1.4f, p)
            }
        }
        if (s.temp < -2 && !s.snow) {
            // frost creeping in from the corners of the lens
            radial(c, 0f, 0f, 120f, intArrayOf(argb(.35f, 220, 228, 224), argb(0f, 220, 228, 224)), floatArrayOf(0f, 1f))
            radial(c, W.toFloat(), 0f, 100f, intArrayOf(argb(.28f, 220, 228, 224), argb(0f, 220, 228, 224)), floatArrayOf(0f, 1f))
        }
    }

    /** Grain, the daytime colour, and wear for frames left too long in memory. */
    private fun post(bmp: Bitmap, s: Shot, r: Rng) {
        val px = IntArray(W * H)
        bmp.getPixels(px, 0, W, 0, 0, W, H)
        val grain = 40f + s.worn * 14
        val day = s.part == Daypart.DAY
        val twilight = s.part == Daypart.DAWN || s.part == Daypart.DUSK
        val band = Rng(s.seed + 31)
        val bands = (0 until s.worn * 2).map { (band.f() * H).toInt() to (2 + band.f() * 6).toInt() }
        for (y in 0 until H) {
            val inBand = bands.any { (by, bh) -> y in by until by + bh }
            for (x in 0 until W) {
                val i = y * W + x
                val cl = px[i]
                var rr = (cl shr 16 and 255).toFloat(); var gg = (cl shr 8 and 255).toFloat(); var bb = (cl and 255).toFloat()
                if (day) {
                    // daytime mode: colour comes back, washed out
                    val l = (rr + gg + bb) / 3 * 1.35f + 30
                    val k = y.toFloat() / H
                    rr = l * (1.0f - .08f * (1 - k)) * (.92f + .12f * k); gg = l * (.98f + .02f * k); bb = l * (1.08f - .22f * k)
                } else if (twilight) {
                    rr *= .9f; bb *= 1.08f
                }
                if (s.worn > 0) { val d = 1 - s.worn * .09f; rr *= d; gg *= d; bb *= d }
                val n = (r.f() - .5f) * grain
                var o = 0f
                if (inBand) o = 26f + r.f() * 30
                px[i] = Color.rgb((rr + n + o).toInt().coerceIn(0, 255), (gg + n * 1.03f + o).toInt().coerceIn(0, 255), (bb + n * .97f + o).toInt().coerceIn(0, 255))
            }
        }
        bmp.setPixels(px, 0, W, 0, 0, W, H)
    }

    private fun hud(c: Canvas, s: Shot) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        c.drawRect(0f, H - 22f, W.toFloat(), H.toFloat(), p)
        p.color = Color.rgb(230, 230, 222); p.textSize = 16f; typeface?.let { p.typeface = it }
        val moon = when { s.moon < .08 -> "○"; s.moon > .92 -> "●"; else -> "◐" }
        val t = (if (s.temp >= 0) "+" else "−") + abs(s.temp).roundToInt() + "°C"
        c.drawText("CAM-%02d".format(s.cam), 8f, H - 7f, p)
        c.drawText("$moon  $t", 120f, H - 7f, p)
        c.drawText(s.stamp, W - 8f - p.measureText(s.stamp), H - 7f, p)
        c.drawText(if (s.part == Daypart.DAY) "DAY" else "IR", W - 8f - p.measureText(if (s.part == Daypart.DAY) "DAY" else "IR"), 18f, p)
    }

    // ——— small drawing helpers shared with the scenes ———
    fun gray(v: Int) = Color.rgb(v, (v * 1.05f).toInt().coerceAtMost(255), (v * .95f).toInt())
    fun argb(a: Float, r: Int, g: Int, b: Int) = Color.argb((a.coerceIn(0f, 1f) * 255).toInt(), r, g, b)

    fun radial(c: Canvas, x: Float, y: Float, r: Float, colors: IntArray, stops: FloatArray) {
        if (r <= 0f) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(x, y, r, colors, stops, Shader.TileMode.CLAMP) }
        c.drawRect(x - r, y - r, x + r, y + r, p)
    }

    fun paintGradient(c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, colors: IntArray, stops: FloatArray, l: Float = 0f, t: Float = 0f, r: Float = W.toFloat(), b: Float = H.toFloat()) {
        val p = Paint().apply { shader = LinearGradient(x0, y0, x1, y1, colors, stops, Shader.TileMode.CLAMP) }
        c.drawRect(l, t, r, b, p)
    }

    /** A dark frame with only a pair of eyes in it: a creature not yet caught. */
    fun unknown(seed: Long, size: Int = 200): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); val r = Rng(seed)
        val s = size.toFloat()
        val p = Paint().apply { shader = LinearGradient(0f, 0f, 0f, s, gray(11), gray(35), Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, s, s, p)
        val q = Paint()
        repeat(4) { q.color = argb(.4f + r.f() * .5f, 8, 10, 8); val x = r.f() * s; c.drawRect(x, 0f, x + (6 + r.f() * 18) * s / 200, s, q) }
        val ex = s * (.25f + r.f() * .5f); val ey = s * (.3f + r.f() * .35f); val gap = s * (.03f + r.f() * .04f)
        radial(c, ex, ey, s * .025f, intArrayOf(argb(.95f, 255, 255, 255), argb(0f, 255, 255, 255)), floatArrayOf(0f, 1f))
        radial(c, ex + gap, ey + r.f() * 3, s * .022f, intArrayOf(argb(.6f, 255, 255, 255), argb(0f, 255, 255, 255)), floatArrayOf(0f, 1f))
        val px = IntArray(size * size); bmp.getPixels(px, 0, size, 0, 0, size, size)
        for (i in px.indices) {
            val cl = px[i]; val n = ((r.f() - .5f) * 44).toInt()
            px[i] = Color.rgb(((cl shr 16 and 255) + n).coerceIn(0, 255), ((cl shr 8 and 255) + n).coerceIn(0, 255), ((cl and 255) + n).coerceIn(0, 255))
        }
        bmp.setPixels(px, 0, size, 0, 0, size, size)
        return bmp
    }
}
