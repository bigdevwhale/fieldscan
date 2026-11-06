package app.fieldscan

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.fieldscan.data.DemoSeed
import app.fieldscan.data.Osm
import app.fieldscan.domain.Moment
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Scanner
import app.fieldscan.render.Frames
import app.fieldscan.ui.FieldTheme
import app.fieldscan.ui.LocalRu
import app.fieldscan.ui.Over
import app.fieldscan.ui.Root
import app.fieldscan.ui.Still
import app.fieldscan.ui.Tab
import app.fieldscan.ui.Tip
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar

/** Every main screen over the demo district, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    /** Today at nine in the evening. */
    private val evening: Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 14) }.timeInMillis

    private val app get() = ApplicationProvider.getApplicationContext<FieldApp>()
    private val places by lazy { Osm.parse(File("demo-assets/district.json").readText()) }

    @Before fun still() { Still.on = true; Frames.init(app) }

    private fun seed(fresh: Boolean = true) {
        if (fresh) app.container.reset()
        DemoSeed.apply(app.container, places, evening)
        // the manual's pages are read, except where a test wants one
        // and the journal's pages too, so no popup covers the screen
        app.container.update { it.copy(tips = Tip.entries.map { t -> t.name }.toSet(), pageSeen = it.story) }
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { FieldTheme { CompositionLocalProvider(LocalRu provides (lang == "ru")) { content() } } }
        repeat(40) { rule.mainClock.advanceTimeBy(100); Thread.sleep(40) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(2_000)
        Thread.sleep(400)
        rule.mainClock.advanceTimeBy(2_000)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun traces() = Scanner.traces(Moment(evening, app.container.fix.value?.at, app.container.weather.value, app.container.state.value), places, app.container.hosts.value)

    @Test fun welcome() { app.container.reset(); app.container.fix.value = Fix(DemoSeed.HOME, 8f, evening); shot("0-welcome") { Root({}, clock = evening) } }
    /** A new observer just home: the first round's banner and the homeling at the door. */
    @Test fun guide() {
        app.container.reset()
        app.container.setHome(DemoSeed.HOME)
        app.container.places.value = places
        app.container.hosts.value = Rules.hosts(places)
        app.container.fix.value = Fix(DemoSeed.HOME, 6f, evening)
        app.container.update { it.copy(tips = Tip.entries.map { t -> t.name }.toSet(), pageSeen = 99) }
        shot("g-guide") { Root({}, Tab.SCAN, clock = evening) }
    }
    /** Far into the story: finds on the air. */
    @Test fun finds() {
        seed()
        app.container.update { it.copy(story = 17, pageSeen = 17) }
        app.container.places.value = places
        app.container.pinFinds()
        shot("s-finds") { Root({}, Tab.SCAN, clock = evening) }
    }
    /** The journal with an episode's task waiting. */
    @Test fun episode() {
        seed()
        app.container.update { it.copy(story = 8, pageSeen = 8, pageAt = evening - 30 * 86_400_000L) }
        shot("e-episode") { Root({}, Tab.RUM, clock = evening) }
    }
    @Test fun scan() { seed(); shot("1-scan") { Root({}, Tab.SCAN, clock = evening) } }
    @Test fun rite() {
        seed()
        val t = traces().first { it.creature.id == "kolod" || it.creature.id == "peril" || it.creature.id == "perek" }
        app.container.fix.value = Fix(t.place.at.offset(200.0, 60.0), 6f, evening)
        shot("2-rite") { Root({}, Tab.SCAN, Over.Rite(t), clock = evening) }
    }
    @Test fun capture() {
        seed()
        val c = app.container.state.value.catches.first { it.creature == "most" }
        shot("3-capture") { Root({}, Tab.SCAN, Over.Shot(c), clock = evening) }
    }
    @Test fun bestiary() { seed(); shot("4-bestiary") { Root({}, Tab.BEST, clock = evening) } }
    @Test fun card() { seed(); shot("5-card") { Root({}, Tab.BEST, Over.Card("tuman"), clock = evening) } }
    @Test fun riddle() { seed(); shot("6-riddle") { Root({}, Tab.BEST, Over.Riddle("kolod"), clock = evening) } }
    @Test fun rumours() { seed(); shot("7-rumours") { Root({}, Tab.RUM, clock = evening) } }
    @Test fun darkScan() { seed(); app.container.update { it.copy(theme = 2) }; shot("d1-scan") { Root({}, Tab.SCAN, clock = evening) } }
    @Test fun darkBestiary() { seed(); app.container.update { it.copy(theme = 2) }; shot("d2-bestiary") { Root({}, Tab.BEST, clock = evening) } }
    @Test fun darkRite() {
        seed(); app.container.update { it.copy(theme = 2) }
        val t = traces().first { it.creature.id == "kolod" || it.creature.id == "peril" || it.creature.id == "perek" }
        app.container.fix.value = Fix(t.place.at.offset(200.0, 60.0), 6f, evening)
        shot("d3-rite") { Root({}, Tab.SCAN, Over.Rite(t), clock = evening) }
    }
    @Test fun darkDevice() { seed(); app.container.update { it.copy(theme = 2) }; shot("d4-device") { Root({}, Tab.DEV, clock = evening) } }
    @Test fun manual() { seed(); app.container.update { it.copy(tips = it.tips - "SCAN") }; shot("9-manual") { Root({}, Tab.SCAN, clock = evening) } }
    @Test fun device() { seed(); shot("8-device") { Root({}, Tab.DEV, clock = evening) } }
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }
