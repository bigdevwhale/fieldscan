package app.fieldscan

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.fieldscan.domain.Story
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Every hour, with the app closed too: bank the steps walked since, and if a journal page is due, open it
 * and say so with a notification (by day only: 8 to 21). The page's own conditions stay as they are.
 */
class PageWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        if (c.state.value.home == null) return Result.success()
        c.stepSensor.read()?.let { c.onCounter(it, force = true) }
        runCatching { c.syncHealth() }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 8..20) {
            val before = c.state.value.story
            c.tick()
            val s = c.state.value
            if (s.story > before) Pages.notify(applicationContext, s.story - 1)
        }
        return Result.success()
    }
}

object Pages {
    private const val CHANNEL = "journal"
    const val EXTRA_TAB = "tab"

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<PageWorker>(1, TimeUnit.HOURS).build()
        // without WorkManager (the unit tests' Robolectric) there's just no hourly check
        runCatching { WorkManager.getInstance(context).enqueueUniquePeriodicWork("pages", ExistingPeriodicWorkPolicy.KEEP, req) }
    }

    fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** "A new journal page": its title and its first lines; a tap opens the journal. */
    fun notify(context: Context, index: Int) {
        if (!allowed(context)) return
        val page = Story.pages.getOrNull(index) ?: return
        val ru = context.resources.configuration.locales[0].language == "ru"
        val m = context.getSystemService(NotificationManager::class.java)
        if (m.getNotificationChannel(CHANNEL) == null) {
            m.createNotificationChannel(NotificationChannel(CHANNEL, if (ru) "Журнал наблюдателя" else "The observer's journal", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).putExtra(EXTRA_TAB, "rum").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = page.text.of(ru).let { if (it.length > 140) it.take(140).substringBeforeLast(' ') + "…" else it }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle((if (ru) "Новая страница журнала: " else "A new journal page: ") + "«" + page.title.of(ru) + "»")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        runCatching { NotificationManagerCompat.from(context).notify(7, n) }
    }
}
