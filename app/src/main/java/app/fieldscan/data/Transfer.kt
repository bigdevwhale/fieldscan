package app.fieldscan.data

import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.GameState
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Moving the game to another phone by hand: one zip with the save and the real photos of the catches.
 * The rendered frames are not carried, they grow again from the catch. Nothing goes through a server:
 * the observer takes the file wherever they like.
 */
object Transfer {
    private const val STATE = "state.json"
    private val PHOTO = Regex("photo_[A-Za-z0-9_-]{1,160}\\.jpg")
    private const val MAX_STATE = 16L shl 20
    private const val MAX_PHOTO = 32L shl 20
    private const val MAX_TOTAL = 2L shl 30

    /** Writes [s] and the photos found in [frames] to [out]. */
    fun write(out: OutputStream, s: GameState, frames: File) {
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry(STATE))
            z.write(Store.toJson(s).put("format", "fieldscan").toString().toByteArray())
            z.closeEntry()
            frames.listFiles { f -> f.isFile && PHOTO.matches(f.name) }.orEmpty().sortedBy { it.name }.forEach { f ->
                z.putNextEntry(ZipEntry(f.name))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
    }

    /**
     * Reads a file made by [write]: the photos go into [into] (emptied first), the save is returned, made fit
     * for this phone. Throws if the file is not a save.
     */
    fun read(input: InputStream, into: File): GameState {
        into.deleteRecursively(); into.mkdirs()
        var json: String? = null
        var total = 0L
        ZipInputStream(input).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                // only known flat names: nothing in the file can write outside [into]
                when {
                    e.isDirectory -> Unit
                    e.name == STATE -> json = String(z.readCapped(MAX_STATE))
                    PHOTO.matches(e.name) -> {
                        val bytes = z.readCapped(MAX_PHOTO)
                        total += bytes.size
                        require(total <= MAX_TOTAL) { "too big" }
                        File(into, e.name).writeBytes(bytes)
                    }
                }
                z.closeEntry()
            }
        }
        val o = JSONObject(requireNotNull(json) { "no save in the file" })
        require(o.optString("format") == "fieldscan" && o.has("catches")) { "not a fieldscan save" }
        return arrive(Store.fromJson(o))
    }

    /**
     * The save as it starts on a new phone: the step counter and Health Connect are this phone's own, so their
     * marks start afresh (the steps already banked stay); creatures this version doesn't know are left out.
     */
    fun arrive(s: GameState): GameState = s.copy(
        stepMark = -1, stepBoot = 0, healthFrom = -1, healthSeen = emptyMap(),
        catches = s.catches.filter { it.creature in Bestiary.byId },
    )

    private fun InputStream.readCapped(max: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 shl 10)
        while (true) {
            val n = read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            require(out.size() <= max) { "entry too big" }
        }
        return out.toByteArray()
    }
}
