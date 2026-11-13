package app.fieldscan.sense

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.fieldscan.domain.Geo
import kotlin.math.abs
import kotlin.math.sqrt

/** Compass heading in degrees from true north (null without a magnetometer). */
@Composable
fun rememberHeading(at: Geo?): State<Float?> {
    val ctx = LocalContext.current
    val s = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(at?.lat?.toInt(), at?.lon?.toInt()) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val decl = at?.let { GeomagneticField(it.lat.toFloat(), it.lon.toFloat(), 0f, System.currentTimeMillis()).declination } ?: 0f
        val rot = FloatArray(9)
        var smooth: Float? = null
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                // where the phone points, at any tilt: its top edge plus its back (world x east, y north, z up).
                // Flat, the back looks at the ground and the top gives the way; upright, the top looks at the sky
                // and the back does; in between both agree. getOrientation's azimuth wanders once the phone is raised.
                val fx = rot[1] - rot[2]
                val fy = rot[4] - rot[5]
                if (fx * fx + fy * fy < 1e-4f) return
                val deg = ((Math.toDegrees(kotlin.math.atan2(fx.toDouble(), fy.toDouble())).toFloat() + decl) + 360) % 360
                val prev = smooth
                // smooth on the circle
                smooth = if (prev == null) deg else (prev + (((deg - prev + 540) % 360) - 180) * .15f + 360) % 360
                s.value = smooth
                if (app.fieldscan.BuildConfig.DEBUG && e.timestamp - logged > 500_000_000L) {
                    logged = e.timestamp
                    android.util.Log.d("FieldHeading", "deg=${deg.toInt()} smooth=${smooth?.toInt()} top=(%.2f,%.2f,%.2f) back=(%.2f,%.2f,%.2f)".format(rot[1], rot[4], rot[7], -rot[2], -rot[5], -rot[8]))
                }
            }
            private var logged = 0L
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }
    return s
}

/** How the phone is held: [moving] is a smoothed shake level (0 = perfectly still), [faceDown] when lying screen down. */
class Motion { var moving by mutableFloatStateOf(0f); var faceDown by mutableStateOf(false) }

@Composable
fun rememberMotion(): Motion {
    val ctx = LocalContext.current
    val m = remember { Motion() }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last: FloatArray? = null
        var level = 0f
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val v = e.values
                val p = last
                if (p != null) {
                    val d = abs(v[0] - p[0]) + abs(v[1] - p[1]) + abs(v[2] - p[2])
                    level = level * .9f + d * .1f
                    m.moving = level
                }
                last = v.copyOf()
                val g = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
                m.faceDown = v[2] < -7.5f && g in 8f..11.5f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    return m
}

/** Ambient light in lux, or null if the phone has no light sensor. */
@Composable
fun rememberLux(): State<Float?> {
    val ctx = LocalContext.current
    val s = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { s.value = e.values[0] }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    return s
}

/** The torch, switched without the camera permission (Android allows that). */
class Torch(ctx: Context) {
    private val cm = ctx.getSystemService(CameraManager::class.java)
    private val id: String? = runCatching {
        cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
    }.getOrNull()
    val present get() = id != null
    fun set(on: Boolean) { id?.let { runCatching { cm.setTorchMode(it, on) } } }
}

/**
 * Listens for a sung note: the pitch only, by the YIN method, over short windows. The audio is never
 * stored or sent; each window is measured and dropped.
 */
class PitchListener(private val ctx: Context, private val onPitch: (Double?) -> Unit) {
    private var thread: Thread? = null
    @Volatile private var running = false

    val allowed get() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !allowed) return
        running = true
        thread = Thread {
            val rate = 22050
            val n = 2048
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = runCatching { AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, n * 2)) }.getOrNull()
            if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) { running = false; return@Thread }
            val buf = ShortArray(n)
            rec.startRecording()
            try {
                while (running) {
                    var read = 0
                    while (read < n && running) { val r = rec.read(buf, read, n - read); if (r <= 0) break; read += r }
                    if (read < n) continue
                    val hz = Yin.pitch(buf, rate)
                    if (app.fieldscan.BuildConfig.DEBUG) android.util.Log.d("FieldPitch", "rms=${Yin.rms(buf).toInt()} hz=${hz?.toInt()}")
                    onPitch(hz)
                }
            } finally { runCatching { rec.stop() }; rec.release() }
        }.apply { isDaemon = true; start() }
    }

    fun stop() { running = false; thread = null }
}

object Yin {
    fun rms(buf: ShortArray): Double { var e = 0.0; for (s in buf) e += s * s.toDouble(); return sqrt(e / buf.size) }

    /** Fundamental frequency in Hz, or null if the window is quiet or unvoiced. */
    fun pitch(buf: ShortArray, rate: Int): Double? {
        val n = buf.size
        var energy = 0.0
        for (s in buf) energy += s * s.toDouble()
        if (sqrt(energy / n) < 60) return null
        val half = n / 2
        val d = DoubleArray(half)
        for (tau in 1 until half) {
            var sum = 0.0
            for (i in 0 until half) { val x = (buf[i] - buf[i + tau]).toDouble(); sum += x * x }
            d[tau] = sum
        }
        // cumulative mean normalised difference
        d[0] = 1.0
        var run = 0.0
        for (tau in 1 until half) { run += d[tau]; d[tau] = if (run == 0.0) 1.0 else d[tau] * tau / run }
        val minTau = rate / 1000 // 1000 Hz top
        val maxTau = minOf(half - 1, rate / 70) // 70 Hz bottom
        var tau = minTau
        while (tau < maxTau) {
            if (d[tau] < 0.15) {
                while (tau + 1 < maxTau && d[tau + 1] < d[tau]) tau++
                // parabolic interpolation
                val a = d[tau - 1]; val b = d[tau]; val c = d[tau + 1]
                val shift = (a - c) / (2 * (a - 2 * b + c)).let { if (it == 0.0) 1.0 else it }
                return rate / (tau + shift.coerceIn(-1.0, 1.0))
            }
            tau++
        }
        return null
    }
}
