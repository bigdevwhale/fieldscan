package app.fieldscan.sense

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.Period

/**
 * The phone's own step counter: a hardware sensor counting steps since the last reboot. While the app is
 * open it is listened to live; the step detector asks for held-back batches at once.
 */
class StepSensor(private val context: Context) {
    private val sm = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var listener: SensorEventListener? = null

    val present: Boolean get() = sensor != null

    val allowed: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    val works: Boolean get() = present && allowed

    /** Starts listening; [onCount] gets the counter since reboot. */
    fun start(onCount: (Long) -> Unit) {
        val s = sensor ?: return
        if (!allowed || listener != null) return
        val detector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        var asked = 0L
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (e.sensor.type == Sensor.TYPE_STEP_COUNTER) onCount(e.values[0].toLong())
                else if (e.timestamp - asked > 2_000_000_000L) { asked = e.timestamp; sm.flush(this) }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        sm.registerListener(l, s, SensorManager.SENSOR_DELAY_UI, 0)
        if (detector != null) sm.registerListener(l, detector, SensorManager.SENSOR_DELAY_UI, 0)
    }

    fun stop() { listener?.let { sm.unregisterListener(it) }; listener = null }

    /** One reading of the counter, or null if there's none, it isn't allowed, or it stays silent. */
    suspend fun read(timeoutMs: Long = 8_000): Long? {
        val s = sensor ?: return null
        if (!allowed) return null
        return kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            kotlinx.coroutines.suspendCancellableCoroutine { k ->
                val l = object : SensorEventListener {
                    override fun onSensorChanged(e: SensorEvent) {
                        sm.unregisterListener(this)
                        if (k.isActive) k.resumeWith(Result.success(e.values[0].toLong()))
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sm.registerListener(l, s, SensorManager.SENSOR_DELAY_NORMAL)
                k.invokeOnCancellation { sm.unregisterListener(l) }
            }
        }
    }
}

/**
 * Steps from Health Connect: the phone's shared health store, where Samsung Health, Mi Fitness, Zepp Life,
 * Google Fit, watches and bands put the steps they count. Read as totals per day, so steps a watch hands
 * over late still arrive.
 */
class HealthSteps(private val context: Context) {
    private val status: Int get() = HealthConnectClient.getSdkStatus(context)

    val available: Boolean get() = status == HealthConnectClient.SDK_AVAILABLE
    val installable: Boolean get() = status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    private var made: HealthConnectClient? = null
    private val client: HealthConnectClient?
        get() = made ?: if (!available) null else runCatching { HealthConnectClient.getOrCreate(context) }
            .onFailure { Log.w(TAG, "client", it) }.getOrNull()?.also { made = it }

    suspend fun allowed(): Boolean {
        val c = client ?: return false
        return runCatching { READ in c.permissionController.getGrantedPermissions() }.getOrDefault(false)
    }

    /** Steps per day from [from] to [to], keyed by epoch day; null if Health Connect can't be read now. */
    suspend fun days(from: LocalDate, to: LocalDate): Map<Long, Long>? {
        val c = client ?: return null
        return runCatching {
            c.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            ).associate { g -> g.startTime.toLocalDate().toEpochDay() to (g.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
        }.onFailure { Log.w(TAG, "read", it) }.getOrNull()
    }

    /** The store page where Health Connect can be installed or updated. */
    fun storeIntent(): Intent = Intent(Intent.ACTION_VIEW, "market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding".toUri())
        .setPackage("com.android.vending")
        .putExtra("overlay", true)
        .putExtra("callerId", context.packageName)

    companion object {
        private const val TAG = "HealthSteps"
        const val PROVIDER = "com.google.android.apps.healthdata"
        val READ = HealthPermission.getReadPermission(StepsRecord::class)
        fun requestContract() = PermissionController.createRequestPermissionResultContract()
    }
}
