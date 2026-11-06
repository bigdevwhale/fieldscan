package app.fieldscan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import app.fieldscan.data.DemoSeed
import app.fieldscan.data.Met
import app.fieldscan.data.Osm
import app.fieldscan.data.Store
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Catch
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Geo
import app.fieldscan.domain.Hash
import app.fieldscan.domain.Most
import app.fieldscan.domain.Place
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Sky
import app.fieldscan.domain.Story
import app.fieldscan.domain.Upgrade
import app.fieldscan.domain.Weather
import app.fieldscan.domain.Pt
import app.fieldscan.domain.Find
import app.fieldscan.domain.Episodes
import app.fieldscan.domain.Weekly
import app.fieldscan.sense.Sfx
import app.fieldscan.domain.Finds
import app.fieldscan.render.Frames
import app.fieldscan.domain.WalkTrack
import app.fieldscan.sense.HealthSteps
import app.fieldscan.sense.StepSensor
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A GPS fix with its accuracy in metres. */
data class Fix(val at: Geo, val acc: Float, val time: Long)

/** How the district download is going. */
data class Tuning(val done: Int = 0, val total: Int = 0, val failed: Boolean = false) {
    val busy get() = done < total && !failed
}

class Container(context: Context) {
    val app: Context = context.applicationContext
    val store = Store(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(store.load() ?: GameState())
    val state: StateFlow<GameState> = _state

    val fix = MutableStateFlow<Fix?>(null)

    // ——— test hooks, driven only by the debug-build receiver (DebugReceiver) ———
    /** The place of the rite on screen, so test fixes can be given relative to it. */
    @Volatile var riteSpot: Place? = null
    /** Until when real GPS fixes are ignored because test fixes are coming. */
    @Volatile var mockUntil = 0L
    /** A compass heading forced by a test (the phone can't be turned from a desk). */
    val headingOverride = MutableStateFlow<Float?>(null)
    val weather = MutableStateFlow(Weather())
    val places = MutableStateFlow<List<Place>>(emptyList())
    val hosts = MutableStateFlow<Map<Most, Place>>(emptyMap())
    val tuning = MutableStateFlow(Tuning())

    private val loaded = HashMap<String, List<Place>>()
    private val tileLock = Mutex()
    private var weatherAt = 0L
    private var weatherWhere: Geo? = null

    /** Changes the state and saves it; a journal page opens on the way if one is due. */
    fun update(f: (GameState) -> GameState) {
        val s = f(_state.value).let { n ->
            // an episode task the state itself shows done (a catch, the archive, steps, a find): mark it and give its parts
            if (Episodes.current(n) != null && n.story - 1 !in n.tasks && Episodes.done(n)) Episodes.mark(n) else n
        }.let { Story.advance(it, System.currentTimeMillis()) }
        _state.value = s
        scope.launch(Dispatchers.IO) { store.save(s) }
    }

    /** A new fix: maybe load tiles around, maybe refresh the weather. */
    fun onFix(f: Fix, test: Boolean = false) {
        if (!test && System.currentTimeMillis() < mockUntil) return
        fix.value = f
        if (BuildConfig.DEMO) return
        if (!test) gpsSteps(f)
        checkEpisode(f)
        checkWeek(f)
        scope.launch { ensureTiles(f.at) }
        val stale = System.currentTimeMillis() - weatherAt > 50 * 60_000 || (weatherWhere?.dist(f.at) ?: 1e9) > 5000
        if (stale) refreshWeather(f.at)
    }

    fun refreshWeather(at: Geo) {
        weatherAt = System.currentTimeMillis(); weatherWhere = at
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { Met.fetch(at.lat, at.lon) } }.onSuccess { weather.value = it }
                .onFailure { weatherAt = 0 }
        }
    }

    /** Loads the 3 × 3 tiles around [g] from the cache or Overpass, nearest first. */
    suspend fun ensureTiles(g: Geo): Unit = tileLock.withLock {
        val need = Osm.around(g)
        val missing = need.filter { it.key !in loaded }
        if (missing.isEmpty()) { publish(need.map { it.key }, g); return@withLock }
        tuning.value = Tuning(need.size - missing.size, need.size)
        var done = need.size - missing.size
        for (t in missing) {
            val f = store.tile(t.key)
            val fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < 30 * Rules.DAY
            val ps: List<Place>? = withContext(Dispatchers.IO) {
                if (fresh) runCatching { Osm.fromJson(f.readText()) }.getOrNull()
                else fetch(t) ?: if (f.exists()) runCatching { Osm.fromJson(f.readText()) }.getOrNull() else null
            }
            if (ps == null) {
                tuning.value = Tuning(done, need.size, failed = true); publish(need.map { it.key }, g)
                // the public servers are often busy for a few minutes: try again later on our own
                scope.launch { delay(90_000); fix.value?.let { ensureTiles(it.at) } }
                return@withLock
            }
            loaded[t.key] = ps
            done++
            tuning.value = Tuning(done, need.size)
            publish(need.map { it.key }, g)
            if (!fresh) delay(1200) // be kind to the public Overpass servers
        }
    }

    /** A tile from Overpass, three rounds over all servers with growing pauses. */
    private suspend fun fetch(t: Osm.Tile): List<Place>? {
        for (round in 0 until 3) {
            val r = runCatching { Osm.parse(Osm.download(t.box), t.box).also { Store.write(store.tile(t.key), Osm.toJson(it)) } }
            if (r.isSuccess) return r.getOrNull()
            delay(listOf(8_000L, 25_000L, 0L)[round])
        }
        return null
    }

    private fun publish(keys: List<String>, g: Geo) {
        val ps = keys.flatMap { loaded[it].orEmpty() }
        places.value = ps
        hosts.value = Rules.hosts(ps)
        // keep memory bounded when travelling: forget tiles far away
        if (loaded.size > 30) loaded.keys.filter { it !in keys }.forEach { loaded.remove(it) }
    }

    fun setHome(g: Geo) = update { it.copy(home = g, started = if (it.started == 0L) System.currentTimeMillis() else it.started) }

    fun know(ids: Collection<String>) {
        val s = _state.value
        if (s.known.containsAll(ids)) return
        update { it.copy(known = it.known + ids) }
    }

    /** Seals a creature into memory. Returns the catch. */
    fun capture(creature: String, place: Place, at: Geo, photo: android.graphics.Bitmap? = null): Catch {
        val now = System.currentTimeMillis()
        val w = weather.value
        val cr = Bestiary[creature]
        val c = Catch(
            creature, place.id, place.name, at, now, Sky.daypart(now, at), w.temp, w.fog, w.rain, w.snow, Sky.moon(now)[1],
            Hash.mix(Hash.of(place.id), now / Rules.DAY),
        )
        // the real photo goes down first, so the frame is grown into it from the start
        photo?.let { p -> runCatching { Frames.photoFile(store.frames, c).outputStream().use { p.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) } } }
        val first = creature !in _state.value.caughtIds
        update { s ->
            s.copy(
                charge = (s.charge - Rules.pace(cr)).coerceAtLeast(0),
                catches = s.catches + c,
                known = s.known + creature + cr.tells,
                parts = s.parts + if (first) Rules.parts(cr.tier) else 0,
                items = (s.items + listOfNotNull(cr.gives)) - cr.conds.filterIsInstance<app.fieldscan.domain.Cond.Carry>().map { it.item }.toSet(),
            )
        }
        return c
    }

    /** Hands everything in memory to the home archive (the caller checks the distance). */
    fun archive() {
        val now = System.currentTimeMillis()
        update { s -> s.copy(catches = s.catches.map { if (it.archived == null) it.copy(archived = now, wornAtArchive = Rules.worn(it, now)) else it }) }
    }

    fun buy(u: Upgrade) = update { s -> if (s.parts >= u.cost && u !in s.upgrades) s.copy(parts = s.parts - u.cost, upgrades = s.upgrades + u) else s }

    fun reset() { update { GameState() } }

    // ——— steps: they charge the trap ———
    val stepSensor = StepSensor(app)
    val healthSteps = HealthSteps(app)
    private var lastCommit = 0L
    private var healthJob: Job? = null
    /** Without a step counter or Health Connect, steps are reckoned from the GPS walk. */
    private var gpsWalk = WalkTrack()
    private var gpsBanked = 0.0

    /** [s] with [add] steps banked: into the charge (up to its cap) and the walked total. */
    private fun bank(s: GameState, add: Long): GameState {
        val n = add.coerceIn(0, 50_000)
        return s.copy(charge = (s.charge + n).coerceAtMost(Rules.CHARGE_MAX.toLong()).toInt(), walked = s.walked + n)
    }

    /** An episode task that needs the observer somewhere: reached with this fix? */
    fun checkEpisode(f: Fix) {
        val s = _state.value
        if (Episodes.current(s) == null || Episodes.done(s)) return
        if (Episodes.reached(s, f.at, f.acc.toDouble(), Sky.daypart(f.time.takeIf { it > 0 } ?: System.currentTimeMillis(), f.at), places.value)) {
            update { Episodes.mark(it) }
            Sfx.step()
        }
    }

    /** A stop of the week's round reached with this fix? Three give the reward. */
    fun checkWeek(f: Fix) {
        val s = _state.value
        val n = Weekly.visit(s, f.at, f.acc.toDouble())
        if (n === s) return
        update { Weekly.visit(it, f.at, f.acc.toDouble()) }
        if (n.weekPaid && !s.weekPaid) Sfx.found() else Sfx.step()
    }

    /** Lets the episode task go (after a while), without its parts. */
    fun skipTask() = update { Episodes.mark(it, reward = false) }

    /** Pins open story finds to places near home (once each), and the week's round when a new week begins. */
    fun pinFinds() {
        val s = _state.value
        val home = s.home ?: return
        val ps = places.value
        if (ps.isEmpty()) return
        val wk = Weekly.week(System.currentTimeMillis())
        if (s.week != wk) Weekly.pick(home, wk, ps).takeIf { it.isNotEmpty() }?.let { st -> update { it.copy(week = wk, stops = st, weekPaid = false) } }
        val need = Finds.open(s).filter { it.id !in s.findAt }
        if (need.isEmpty()) return
        update { st -> st.copy(findAt = st.findAt + need.associate { it.id to Finds.pick(it, home, ps) }) }
    }

    /** A story find reached: it's made, its parts given. */
    fun found(f: Find) = update { it.copy(finds = it.finds + f.id, parts = it.parts + Finds.PARTS) }

    /** A tab to open, asked for from outside (a notification): "rum" for the journal. */
    val openTab = MutableStateFlow<String?>(null)

    /** Opens a journal page whose days have run out (called on the clock). */
    fun tick() { if (Story.waiting(_state.value, System.currentTimeMillis())?.ready == true) update { it } }

    /** A reading of the phone's counter: banks what came since the last one. */
    fun onCounter(v: Long, force: Boolean = false) {
        val s = _state.value
        if (s.health) return
        val now = System.currentTimeMillis()
        val boot = now - android.os.SystemClock.elapsedRealtime()
        val rebooted = abs(boot - s.stepBoot) > 120_000
        val add = when {
            s.stepMark < 0 -> 0L
            rebooted || v < s.stepMark -> v
            else -> v - s.stepMark
        }
        // commit in small batches so a walk doesn't write the save on every step
        if (!force && s.stepMark >= 0 && !rebooted && add < 10 && now - lastCommit < 4000) return
        lastCommit = now
        update { bank(it, add).copy(stepMark = v, stepBoot = boot) }
    }

    /** Banks the Health Connect steps not banked yet (the last three days, so late watch syncs still count). */
    suspend fun syncHealth() {
        val s = _state.value
        if (!s.health) return
        val today = LocalDate.now()
        val from = maxOf(LocalDate.ofEpochDay(s.healthFrom), today.minusDays(2))
        val totals = healthSteps.days(from, today) ?: return
        update { st ->
            val add = totals.entries.sumOf { (d, n) -> maxOf(0L, n - (st.healthSeen[d] ?: 0L)) }
            bank(st, add).copy(healthSeen = (st.healthSeen + totals).filterKeys { it >= today.toEpochDay() - 3 })
        }
    }

    /** Health Connect (true) or the phone's own counter (false). */
    fun useHealth(on: Boolean) {
        update { if (on) it.copy(healthFrom = if (it.health) it.healthFrom else LocalDate.now().toEpochDay()) else it.copy(healthFrom = -1, stepMark = -1) }
        if (on) scope.launch { syncHealth() }
    }

    /** While the app is in front: the counter live, Health Connect every minute. */
    fun startSteps() {
        stepSensor.start { v -> scope.launch { onCounter(v) } }
        healthJob?.cancel()
        healthJob = scope.launch { while (true) { syncHealth(); delay(60_000) } }
    }

    fun stopSteps() { stepSensor.stop(); healthJob?.cancel(); healthJob = null }

    private fun gpsSteps(f: Fix) {
        val s = _state.value
        if (s.health || stepSensor.works) return
        gpsWalk.add(Pt(f.at, f.acc.toDouble(), f.time))
        val n = ((gpsWalk.metres - gpsBanked) / Rules.STRIDE).toInt()
        if (n >= 10) { gpsBanked += n * Rules.STRIDE; update { bank(it, n.toLong()) } }
    }
}

/** Listens to the GPS while the app is in front. */
class Tracker(private val context: Context, private val onFix: (Fix) -> Unit) : LocationListener {
    private val lm = context.getSystemService(LocationManager::class.java)
    private var on = false

    val allowed get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (!allowed || on) return
        on = true
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            if (runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)) {
                runCatching { lm.requestLocationUpdates(p, 2000L, 1f, this, Looper.getMainLooper()) }
                runCatching { lm.getLastKnownLocation(p) }.getOrNull()?.let { onLocationChanged(it) }
            }
        }
    }

    fun stop() {
        if (!on) return
        on = false
        runCatching { lm.removeUpdates(this) }
    }

    private var last: Location? = null

    override fun onLocationChanged(l: Location) {
        // a coarse network fix shouldn't override a fresh GPS one
        val prev = last
        if (prev != null && l.provider == LocationManager.NETWORK_PROVIDER && prev.provider == LocationManager.GPS_PROVIDER &&
            l.time - prev.time < 30_000 && l.accuracy > prev.accuracy) return
        last = l
        onFix(Fix(Geo(l.latitude, l.longitude), if (l.hasAccuracy()) l.accuracy else 30f, l.time))
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
}

class FieldApp : Application() {
    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        container = Container(this)
        if (BuildConfig.DEMO) DemoSeed.install(this, container) else Pages.schedule(this)
    }
}

val Context.container: Container get() = (applicationContext as FieldApp).container
