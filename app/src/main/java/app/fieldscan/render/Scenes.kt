package app.fieldscan.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import app.fieldscan.domain.Habitat
import app.fieldscan.domain.Scene
import app.fieldscan.render.TrailCam.H
import app.fieldscan.render.TrailCam.W
import app.fieldscan.render.TrailCam.argb
import app.fieldscan.render.TrailCam.gray
import app.fieldscan.render.TrailCam.radial

/** The places a trail camera watches: what is behind the creature and what is in front of it. */
object Scenes {
    class Def(val x: Float, val ground: Float, val scale: Float, val bg: (Canvas, Rng) -> Unit, val fg: (Canvas, Rng) -> Unit)

    private fun fill(c: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c }

    /** An irregular trunk or post: never a clean rectangle. */
    private fun trunk(c: Canvas, cx: Float, w: Float, seed: Long, color: Int, top: Float = 0f, bottom: Float = H.toFloat()) {
        val n = Noise(seed); val p = Path()
        var y = top
        p.moveTo(cx - w / 2 + (n(top * .03f, 1f) - .5f) * w * .5f, top)
        while (y <= bottom) { p.lineTo(cx - w / 2 + (n(y * .03f, 1f) - .5f) * w * .5f, y); y += 6 }
        y = bottom
        while (y >= top) { p.lineTo(cx + w / 2 + (n(y * .03f, 5f) - .5f) * w * .5f, y); y -= 6 }
        p.close(); c.drawPath(p, fill(color))
    }

    private fun grass(c: Canvas, y0: Float, y1: Float, count: Int, r: Rng, front: Boolean, base: Int = 60) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        val path = Path()
        repeat(count) {
            val gx = r.f() * W; val gy = y0 + r.f() * (y1 - y0)
            val h = (if (front) 18f else 6f) + r.f() * (if (front) 36f else 16f); val lean = (r.f() - .5f) * h * .7f
            val v = (base + r.f() * 50).toInt()
            p.color = argb(if (front) .8f else .5f, v, (v * 1.07f).toInt().coerceAtMost(255), (v * .96f).toInt())
            p.strokeWidth = if (front) 1f + r.f() * 1.6f else .6f + r.f() * .8f
            path.reset(); path.moveTo(gx, gy); path.quadTo(gx + lean * .3f, gy - h * .6f, gx + lean, gy - h)
            c.drawPath(path, p)
        }
    }

    /** A wobbly polygon. */
    private fun blob(c: Canvas, pts: List<Pair<Float, Float>>, seed: Long, amp: Float, color: Int) {
        val n = Noise(seed); val p = Path()
        pts.forEachIndexed { i, (x, y) ->
            val jx = x + (n(i * .7f, 2f) - .5f) * amp; val jy = y + (n(i * .7f, 9f) - .5f) * amp
            if (i == 0) p.moveTo(jx, jy) else p.lineTo(jx, jy)
        }
        p.close(); c.drawPath(p, fill(color))
    }

    private fun ground(c: Canvas, y: Float, seed: Long, col: Int = gray(48)) = blob(c, listOf(0f to y, W * .5f to y - 4, W.toFloat() to y + 2, W.toFloat() to H.toFloat(), 0f to H.toFloat()), seed, 6f, col)

    private fun trees(c: Canvas, r: Rng, n: Int, seed: Long) {
        repeat(n) { trunk(c, r.f() * W, 10 + r.f() * 26, seed + it, argb(.4f + r.f() * .5f, 12, 14, 12)) }
    }

    fun of(s: Scene, h: Habitat): Def = when (s) {
        Scene.BRIDGE -> Def(270f, 320f, 1f, { c, r ->
            blob(c, listOf(0f to 0f, W.toFloat() to 0f, W.toFloat() to 70f, 380f to 82f, 300f to 92f, 200f to 90f, 120f to 78f, 0f to 64f), 3, 10f, gray(14))
            blob(c, listOf(70f to 60f, 132f to 64f, 138f to 330f, 64f to 330f), 4, 8f, gray(31))
            TrailCam.paintGradient(c, 0f, 300f, 0f, H.toFloat(), intArrayOf(gray(43), gray(21)), floatArrayOf(0f, 1f), t = 300f)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f }
            repeat(70) { p.color = argb(.15f + r.f() * .3f, 106, 112, 102); val y = 305 + r.f() * 55; val x = r.f() * W; c.drawLine(x, y, x + 8 + r.f() * 30, y, p) }
        }, { c, r -> grass(c, 300f, 322f, 60, r, true, 70) })

        Scene.WELL -> Def(250f, 262f, 1f, { c, r ->
            trees(c, r, 5, 20); grass(c, 250f, H.toFloat(), 240, r, false)
        }, { c, r ->
            blob(c, listOf(168f to 252f, 200f to 244f, 250f to 240f, 300f to 244f, 334f to 252f, 340f to 340f, 160f to 340f), 6, 6f, gray(44))
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(.6f, 20, 23, 19); strokeWidth = 1.4f }
            for (i in 0 until 9) { val y = 262f + i * 9; c.drawLine(166f, y, 336f, y, p); var x = 170f + (i % 2) * 9; while (x < 334) { c.drawLine(x, y, x, y + 9, p); x += 18 + r.f() * 10 } }
            blob(c, listOf(164f to 250f, 200f to 240f, 250f to 236f, 300f to 240f, 338f to 250f, 300f to 258f, 250f to 260f, 200f to 258f), 7, 4f, gray(64))
            grass(c, 330f, H.toFloat(), 70, r, true, 80)
        })

        Scene.LAMP -> Def(300f, 345f, 1f, { c, r ->
            trees(c, r, 6, 40)
            ground(c, 296f, 5, gray(50))
            trunk(c, 120f, 9f, 90, gray(13), top = 54f)
            radial(c, 124f, 60f, 80f, intArrayOf(Color.rgb(223, 232, 216), argb(.66f, 154, 165, 150), argb(0f, 0, 0, 0)), floatArrayOf(0f, .15f, 1f))
            grass(c, 290f, H.toFloat(), 200, r, false)
        }, { c, r -> grass(c, 330f, H.toFloat(), 60, r, true, 80) })

        Scene.STOP -> Def(204f, 350f, 1f, { c, _ ->
            ground(c, 280f, 1, gray(58))
            c.drawRect(120f, 62f, 450f, 76f, fill(gray(23)))
            trunk(c, 131f, 6f, 70, gray(23), top = 74f, bottom = 306f); trunk(c, 439f, 6f, 71, gray(23), top = 74f, bottom = 306f)
            c.drawRect(134f, 78f, 436f, 278f, fill(argb(.32f, 150, 162, 146)))
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(.18f, 160, 170, 156); strokeWidth = 2f }
            for (i in 0 until 4) c.drawLine(150f + i * 80, 90f, 190f + i * 80, 260f, p)
            c.drawRect(300f, 220f, 420f, 230f, fill(gray(32))); c.drawRect(306f, 230f, 312f, 270f, fill(gray(32))); c.drawRect(408f, 230f, 414f, 270f, fill(gray(32)))
            c.drawRect(360f, 96f, 406f, 126f, fill(Color.rgb(207, 216, 200)))
            val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gray(32); textSize = 16f; TrailCam.typeface?.let { typeface = it } }
            c.drawText("14", 372f, 116f, t)
        }, { c, r -> grass(c, 320f, H.toFloat(), 40, r, true, 80) })

        Scene.STAIRS -> Def(240f, 300f, .9f, { c, _ ->
            val p = Path().apply { moveTo(0f, H.toFloat()); lineTo(0f, 330f); for (i in 0 until 14) { lineTo(i * 36f, 330f - i * 22); lineTo(i * 36f + 36, 330f - i * 22) }; lineTo(W.toFloat(), H.toFloat()); close() }
            c.drawPath(p, fill(gray(38)))
            val l = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(.35f, 150, 160, 146); strokeWidth = 1.5f }
            for (i in 0 until 14) c.drawLine(i * 36f, 330f - i * 22, i * 36f + 36, 330f - i * 22, l)
            val rail = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gray(13); strokeWidth = 3f }
            c.drawLine(0f, 290f, W.toFloat(), 0f, rail)
            rail.strokeWidth = 2f
            for (i in 0 until 9) c.drawLine(i * 58f, 290f - i * 36.2f, i * 58f, 330f - i * 22, rail)
        }, { c, r -> grass(c, 330f, H.toFloat(), 30, r, true, 80) })

        Scene.DOOR -> Def(300f, 322f, .9f, { c, _ ->
            ground(c, 316f, 2, gray(48))
            c.drawRect(60f, 40f, 280f, 324f, fill(gray(24)))
            c.drawRect(84f, 120f, 164f, 320f, fill(argb(.22f, 150, 162, 146))); c.drawRect(176f, 120f, 256f, 320f, fill(argb(.22f, 150, 162, 146)))
            if (h == Habitat.PHARMACY) {
                radial(c, 170f, 78f, 70f, intArrayOf(argb(.55f, 230, 240, 224), argb(0f, 0, 0, 0)), floatArrayOf(0f, 1f))
                c.drawRect(160f, 58f, 180f, 98f, fill(Color.rgb(238, 244, 232))); c.drawRect(150f, 68f, 190f, 88f, fill(Color.rgb(238, 244, 232)))
            } else {
                // a postbox on the wall
                blob(c, listOf(330f to 150f, 372f to 150f, 374f to 214f, 328f to 214f), 9, 3f, gray(30))
                c.drawRect(338f, 166f, 364f, 170f, fill(gray(70)))
            }
        }, { _, _ -> })

        Scene.FOREST -> Def(240f, 318f, 1f, { c, r ->
            trees(c, r, 7, 60)
            ground(c, 300f, 8, gray(44))
            trunk(c, 130f, 46f, 61, gray(17), top = 0f, bottom = 320f)
            grass(c, 290f, H.toFloat(), 240, r, false)
        }, { c, r ->
            grass(c, 318f, H.toFloat(), 80, r, true, 70)
            // a low branch across the top
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gray(10); strokeWidth = 7f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
            val path = Path().apply { moveTo(-10f, 30f); quadTo(120f, 50f, 260f, 22f + r.f() * 10) }
            c.drawPath(path, p)
        })

        Scene.WATER -> Def(240f, 300f, 1f, { c, r ->
            trees(c, r, 6, 80)
            TrailCam.paintGradient(c, 0f, 270f, 0f, H.toFloat(), intArrayOf(gray(40), gray(22)), floatArrayOf(0f, 1f), t = 270f)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f }
            repeat(90) { p.color = argb(.12f + r.f() * .3f, 110, 118, 106); val y = 278 + r.f() * 80; val x = r.f() * W; c.drawLine(x, y, x + 6 + r.f() * 34, y, p) }
            if (h == Habitat.PIER) { c.drawRect(0f, 286f, 210f, 296f, fill(gray(26))); for (i in 0 until 5) c.drawRect(20f + i * 44, 296f, 26f + i * 44, 360f, fill(gray(20))) }
        }, { c, r -> grass(c, 320f, H.toFloat(), 50, r, true, 70) })

        Scene.STREET -> Def(250f, 330f, 1f, { c, r ->
            // houses far away, a road and its kerb
            var x = -10f
            while (x < W) { val w = 60 + r.f() * 80; val top = 60 + r.f() * 80; blob(c, listOf(x to top, x + w to top + r.f() * 8, x + w to 260f, x to 260f), (x * 7).toLong(), 3f, gray(18 + (r.f() * 8).toInt())); x += w + 6 }
            val win = fill(argb(.22f, 190, 200, 184))
            repeat(10) { val wx = r.f() * W; val wy = 90 + r.f() * 150; c.drawRect(wx, wy, wx + 7, wy + 10, win) }
            c.drawRect(0f, 260f, W.toFloat(), H.toFloat(), fill(gray(40)))
            val m = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb(.35f, 200, 208, 196); strokeWidth = 3f }
            for (i in 0 until 6) c.drawLine(i * 90f, 300f, i * 90f + 50, 300f, m)
            radial(c, 60f, 120f, 70f, intArrayOf(argb(.6f, 223, 232, 216), argb(0f, 0, 0, 0)), floatArrayOf(0f, 1f))
        }, { _, _ -> })

        Scene.YARD -> Def(250f, 318f, .95f, { c, r ->
            c.drawRect(0f, 30f, W.toFloat(), 290f, fill(gray(22)))
            val win = fill(argb(.16f, 205, 216, 198)); val dark = fill(gray(16))
            for (row in 0 until 3) for (col in 0 until 6) { val x = 24f + col * 76; val y = 54f + row * 74; c.drawRect(x, y, x + 34, y + 44, if (r.f() < .3f) win else dark) }
            ground(c, 288f, 3, gray(46))
            if (h == Habitat.BENCH || h == Habitat.PLAYGROUND) {
                c.drawRect(330f, 270f, 450f, 278f, fill(gray(28))); c.drawRect(336f, 278f, 342f, 300f, fill(gray(28))); c.drawRect(438f, 278f, 444f, 300f, fill(gray(28)))
            }
            if (h == Habitat.PLAYGROUND) {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gray(16); strokeWidth = 4f }
                c.drawLine(40f, 300f, 80f, 160f, p); c.drawLine(160f, 300f, 120f, 160f, p); c.drawLine(70f, 160f, 130f, 160f, p)
                p.strokeWidth = 1.5f; c.drawLine(92f, 162f, 96f, 250f, p); c.drawLine(110f, 162f, 106f, 250f, p); c.drawRect(90f, 250f, 112f, 255f, fill(gray(16)))
            }
            if (h == Habitat.HOME) {
                c.drawRect(176f, 150f, 304f, 300f, fill(gray(14))); c.drawRect(192f, 166f, 288f, 296f, fill(argb(.5f, 205, 216, 198)))
                c.drawRect(166f, 296f, 314f, 308f, fill(gray(54)))
            }
            grass(c, 288f, H.toFloat(), 120, r, false)
        }, { c, r -> grass(c, 330f, H.toFloat(), 40, r, true, 80) })

        Scene.OPEN -> Def(250f, 312f, 1f, { c, r ->
            // open sky, a far horizon and a plinth
            radial(c, 380f, 70f, 40f, intArrayOf(argb(.5f, 220, 228, 214), argb(0f, 0, 0, 0)), floatArrayOf(0f, 1f))
            blob(c, listOf(0f to 250f, 120f to 240f, 260f to 248f, 400f to 236f, W.toFloat() to 244f, W.toFloat() to H.toFloat(), 0f to H.toFloat()), 11, 8f, gray(30))
            ground(c, 296f, 12, gray(48))
            if (h == Habitat.MEMORIAL) { blob(c, listOf(60f to 300f, 150f to 300f, 146f to 200f, 64f to 200f), 13, 3f, gray(26)); blob(c, listOf(84f to 200f, 126f to 200f, 120f to 90f, 104f to 70f, 90f to 90f), 14, 5f, gray(18)) }
            grass(c, 296f, H.toFloat(), 160, r, false)
        }, { c, r -> grass(c, 320f, H.toFloat(), 70, r, true, 80) })

        Scene.TOWER -> Def(300f, 318f, .95f, { c, r ->
            trees(c, r, 3, 100)
            blob(c, listOf(70f to 330f, 170f to 330f, 160f to 90f, 120f to 40f, 80f to 90f), 15, 4f, gray(20))
            // weathervane
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gray(16); strokeWidth = 2f }
            c.drawLine(120f, 40f, 120f, 10f, p); c.drawLine(104f, 16f, 140f, 18f, p)
            if (h == Habitat.WORSHIP) radial(c, 120f, 60f, 26f, intArrayOf(argb(.3f, 220, 228, 214), argb(0f, 0, 0, 0)), floatArrayOf(0f, 1f))
            ground(c, 300f, 16, gray(46))
            grass(c, 300f, H.toFloat(), 160, r, false)
        }, { c, r -> grass(c, 324f, H.toFloat(), 40, r, true, 80) })
    }
}
