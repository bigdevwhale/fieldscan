package app.fieldscan.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.core.content.res.ResourcesCompat
import app.fieldscan.R
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Catch
import app.fieldscan.domain.Hash
import app.fieldscan.domain.Rules
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Frames of caught creatures: grown once from the catch and kept as PNGs (the archive). A frame still in
 * memory is re-grown as it wears, a step a day.
 */
object Frames {
    private val cache = HashMap<String, Frame>()

    fun init(ctx: Context) {
        if (TrailCam.typeface == null) TrailCam.typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.m6x11) }.getOrNull()
    }

    /** Where the real photo of a catch is kept. */
    fun photoFile(dir: File, c: Catch) = File(dir, "photo_" + c.id.replace('@', '_').replace(':', '_') + ".jpg")

    fun shot(c: Catch, worn: Int, photo: Bitmap? = null): Shot {
        val cr = Bestiary[c.creature]
        return Shot(
            body = cr.body, habitat = cr.habitat, seed = c.seed, part = c.part, fog = c.fog, rain = c.rain, snow = c.snow,
            temp = c.temp, moon = c.moon, worn = worn, cam = 1 + (Hash.mix(Hash.of(c.place), 5) ushr 1).rem(24).toInt(),
            stamp = SimpleDateFormat("dd/MM  HH:mm:ss", Locale.US).format(Date(c.time)),
            photo = photo,
        )
    }

    /** The frame of [c] as of [now]; rendered on first use (call off the main thread). */
    fun of(ctx: Context, dir: File, c: Catch, now: Long = System.currentTimeMillis()): Frame {
        init(ctx)
        val worn = Rules.worn(c, now)
        val key = c.id.replace('@', '_').replace(':', '_') + "_w$worn"
        cache[key]?.let { return it }
        val f = File(dir, "$key.png"); val meta = File(dir, "$key.txt")
        val fr = if (f.exists() && meta.exists()) runCatching {
            val r = meta.readText().trim().split(',').map { it.toInt() }
            Frame(BitmapFactory.decodeFile(f.path), Rect(r[0], r[1], r[2], r[3]))
        }.getOrNull() else null
        val out = fr ?: TrailCam.render(shot(c, worn, photoFile(dir, c).takeIf { it.exists() }?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() })).also { fresh ->
            runCatching {
                f.outputStream().use { fresh.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                meta.writeText("${fresh.crop.left},${fresh.crop.top},${fresh.crop.right},${fresh.crop.bottom}")
            }
        }
        if (cache.size > 40) cache.clear()
        cache[key] = out
        return out
    }

    /** The square thumbnail around the creature. */
    fun thumb(fr: Frame): Bitmap = Bitmap.createBitmap(fr.bitmap, fr.crop.left, fr.crop.top, fr.crop.width(), fr.crop.height())

    private val unknown = HashMap<String, Bitmap>()
    fun unknown(id: String): Bitmap = unknown.getOrPut(id) { TrailCam.unknown(Hash.of(id)) }
}
