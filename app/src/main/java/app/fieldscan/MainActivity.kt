package app.fieldscan

import android.Manifest
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import app.fieldscan.render.Frames
import app.fieldscan.ui.FieldTheme
import app.fieldscan.ui.LocalRu
import app.fieldscan.ui.Root
import app.fieldscan.ui.Still

class MainActivity : AppCompatActivity() {
    private lateinit var tracker: Tracker

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tracker.start(); container.startSteps() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(t), navigationBarStyle = SystemBarStyle.dark(t))
        Still.on = BuildConfig.SHOTS
        Frames.init(this)
        // the right paper from the first frame, before Root reads the setting
        val night = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        app.fieldscan.ui.C.dark = when (container.state.value.theme) { 1 -> false; 2 -> true; else -> night }
        tracker = Tracker(this) { container.onFix(it) }
        intent?.getStringExtra(Pages.EXTRA_TAB)?.let { container.openTab.value = it }
        val ru = resources.configuration.locales[0].language == "ru"
        setContent {
            FieldTheme {
                CompositionLocalProvider(LocalRu provides ru) {
                    Root(onAskLocation = { askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) +
                        (if (android.os.Build.VERSION.SDK_INT >= 29) arrayOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyArray()) +
                        (if (android.os.Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray())) })
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Pages.EXTRA_TAB)?.let { container.openTab.value = it }
    }

    override fun onResume() {
        super.onResume()
        if (!BuildConfig.DEMO) { tracker.start(); container.startSteps() }
        if (!BuildConfig.SHOTS) app.fieldscan.sense.Sfx.startAir()
    }

    override fun onPause() {
        super.onPause()
        tracker.stop()
        container.stopSteps()
        app.fieldscan.sense.Sfx.stopAir()
    }
}
