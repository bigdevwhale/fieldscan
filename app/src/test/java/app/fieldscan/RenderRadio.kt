package app.fieldscan

import app.fieldscan.sense.Radio
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Writes the journal's intercepts as WAV files to listen to (run with `-Prender`). */
class RenderRadio {
    @Test fun wavs() {
        val out = File(System.getProperty("render.out") ?: "build/art", "radio").apply { mkdirs() }
        for ((name, text) in listOf("night" to "НВ 7 ЗДЕСЬ", "knock" to "ЭТО Я", "steps" to "ШАГИ ШАГИ ШАГИ")) {
            val pcm = Radio.clip(text).pcm
            val b = ByteBuffer.allocate(44 + pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            b.put("RIFF".toByteArray()).putInt(36 + pcm.size * 2).put("WAVE".toByteArray())
            b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(Radio.RATE).putInt(Radio.RATE * 2).putShort(2).putShort(16)
            b.put("data".toByteArray()).putInt(pcm.size * 2)
            pcm.forEach { b.putShort(it) }
            File(out, "$name.wav").writeBytes(b.array())
        }
    }
}
