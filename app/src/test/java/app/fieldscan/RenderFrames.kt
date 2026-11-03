package app.fieldscan

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Daypart
import app.fieldscan.render.Frames
import app.fieldscan.render.Shot
import app.fieldscan.render.TrailCam
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every creature's trail-camera frame, and one creature in all weathers (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderFrames {
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "frames").apply { mkdirs() } }
    private fun save(b: Bitmap, name: String) = File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    @Test fun all() {
        Frames.init(ApplicationProvider.getApplicationContext())
        Bestiary.all.forEachIndexed { i, c ->
            val t0 = System.currentTimeMillis()
            val f = TrailCam.render(Shot(c.body, c.habitat, 1000L + i * 7919, cam = 1 + i % 24))
            println("${c.id}: ${System.currentTimeMillis() - t0} ms crop=${f.crop}")
            save(f.bitmap, "%02d-%s".format(i, c.id))
            save(Frames.thumb(f), "%02d-%s-thumb".format(i, c.id))
        }
    }

    @Test fun weathers() {
        Frames.init(ApplicationProvider.getApplicationContext())
        val c = Bestiary["most"]
        val base = Shot(c.body, c.habitat, 4242)
        save(TrailCam.render(base.copy(part = Daypart.DAY)).bitmap, "w-day")
        save(TrailCam.render(base.copy(part = Daypart.DUSK)).bitmap, "w-dusk")
        save(TrailCam.render(base.copy(fog = true)).bitmap, "w-fog")
        save(TrailCam.render(base.copy(rain = true)).bitmap, "w-rain")
        save(TrailCam.render(base.copy(snow = true, temp = -3.0)).bitmap, "w-snow")
        save(TrailCam.render(base.copy(worn = 3)).bitmap, "w-worn3")
        save(TrailCam.unknown(77), "unknown")
    }
}
