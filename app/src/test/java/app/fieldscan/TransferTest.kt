package app.fieldscan

import app.fieldscan.data.Transfer
import app.fieldscan.domain.Catch
import app.fieldscan.domain.Daypart
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class TransferTest {
    private fun catch(id: String, t: Long) = Catch(id, "p", "Двор", Geo(55.0, 37.0), t, Daypart.DAY, 5.0, false, false, false, .5, 1)
    private fun tmp(): File = Files.createTempDirectory("fs").toFile()

    @Test
    fun theGameGoesOverWithItsPhotos() {
        val frames = tmp()
        File(frames, "photo_porog_1700000000000.jpg").writeBytes(byteArrayOf(1, 2, 3))
        File(frames, "porog_1700000000000_w0.png").writeBytes(byteArrayOf(9))   // a rendered frame: not carried
        val s = GameState(
            home = Geo(55.7, 37.6), catches = listOf(catch("porog", 1_700_000_000_000), catch("nobody_yet", 1_700_000_100_000)),
            parts = 7, known = setOf("porog", "mysh"), walked = 12_345, charge = 40,
            stepMark = 999, stepBoot = 5, healthFrom = 19_000, healthSeen = mapOf(19_000L to 10L),
        )
        val bytes = ByteArrayOutputStream().also { Transfer.write(it, s, frames) }.toByteArray()

        val into = tmp()
        val got = Transfer.read(ByteArrayInputStream(bytes), into)
        assertEquals(listOf("porog"), got.catches.map { it.creature })   // a creature this version doesn't know is left out
        assertEquals(s.home, got.home); assertEquals(7, got.parts); assertEquals(s.known, got.known)
        assertEquals(12_345L, got.walked); assertEquals(40, got.charge)
        // the step marks belong to the old phone
        assertEquals(-1L, got.stepMark); assertEquals(-1L, got.healthFrom); assertTrue(got.healthSeen.isEmpty())
        assertEquals(listOf("photo_porog_1700000000000.jpg"), into.list()!!.toList())
        assertEquals(listOf<Byte>(1, 2, 3), File(into, "photo_porog_1700000000000.jpg").readBytes().toList())
    }

    @Test
    fun aStrangeFileIsRefusedAndWritesNothingOutside() {
        val zip = ByteArrayOutputStream().also { o ->
            ZipOutputStream(o).use { z ->
                z.putNextEntry(ZipEntry("../photo_evil.jpg")); z.write(1); z.closeEntry()
                z.putNextEntry(ZipEntry("state.json")); z.write("{\"hello\":1}".toByteArray()); z.closeEntry()
            }
        }.toByteArray()
        val root = tmp(); val into = File(root, "in")
        assertTrue(runCatching { Transfer.read(ByteArrayInputStream(zip), into) }.isFailure)
        assertFalse(File(root, "photo_evil.jpg").exists())
        assertTrue(runCatching { Transfer.read(ByteArrayInputStream("not a zip".toByteArray()), into) }.isFailure)
    }
}
