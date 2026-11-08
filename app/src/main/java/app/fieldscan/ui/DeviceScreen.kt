package app.fieldscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.fieldscan.sense.HealthSteps
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.fieldscan.Fix
import app.fieldscan.container
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Rules
import app.fieldscan.domain.Upgrade

@Composable
fun DeviceScreen(state: GameState, fix: Fix?, now: Long) {
    val ru = LocalRu.current
    val co = LocalContext.current.container
    val home = state.home
    val toHome = if (fix != null && home != null) fix.at.dist(home) else null
    val atHome = toHome != null && toHome <= Rules.HOME_RADIUS + minOf(fix!!.acc.toDouble(), 30.0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Title(tr("Прибор", "Device"), Tip.DEV)
        Gap(6.dp)
        Frame {
            Column {
                Row { T(tr("Память", "Memory"), Ty.body, Modifier.weight(1f)); T("${state.memoryKb} / ${state.capacity} " + tr("КБ", "KB")) }
                Gap(6.dp)
                // one cell per KB
                val cells = ArrayList<Int>() // 0 empty, 1 fresh, 2 worn
                state.inMemory.forEach { c -> repeat(Bestiary[c.creature].kb) { cells += if (Rules.worn(c, now) >= 2) 2 else 1 } }
                while (cells.size < state.capacity) cells += 0
                cells.chunked(8).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        row.forEach { k -> Box(Modifier.weight(1f).aspectRatio(1.6f).border(2.dp, if (k == 2) C.red else C.ink).background(when (k) { 1 -> C.ink; 2 -> C.red; else -> C.paper })) }
                    }
                }
                Gap(4.dp)
                // names once; how many are wearing, in one line rather than after every name
                if (state.inMemory.isNotEmpty()) T(tr("В памяти: ", "In memory: ") + state.inMemory.joinToString(", ") { Bestiary[it.creature].name.of(ru) }, Ty.small.copy(color = C.ink))
                val worn = state.inMemory.count { Rules.worn(it, now) > 0 }
                if (worn > 0) T(tr("Портятся: $worn из ${state.inMemory.size}", "Wearing: $worn of ${state.inMemory.size}"), Ty.small.copy(color = C.red))
                Gap(8.dp)
                if (Guide.stage(state) == Guide.Stage.ARCHIVE) {
                    T(tr("↓ Первый обход: сдай кадр сюда — он сохранится навсегда.", "↓ First round: hand the frame in here, it keeps for good."), Ty.small.copy(color = C.red))
                    Gap(4.dp)
                }
                // the archive is at home: say so plainly instead of a greyed-out button
                when {
                    state.inMemory.isEmpty() -> T(tr("Память пуста — сдавать нечего.", "Memory is empty: nothing to hand in."), Ty.small)
                    atHome -> Btn(tr("Сдать в архив", "Hand in to the archive"), red = true) { co.archive() }
                    else -> Frame(dashed = true) {
                        T(tr("Сдать в архив можно дома", "Frames are handed in at home") + (toHome?.let { tr(" · до дома ", " · home is ") + Words.dist(it, ru) } ?: ""), Ty.small.copy(color = C.ink))
                    }
                }
            }
        }
        Gap(10.dp)
        StepSource(state)
        Gap(10.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(tr("Насадки", "Attachments"), Ty.head, Modifier.weight(1f))
            T(tr("детали: ", "parts: ") + state.parts, Ty.body.copy(color = C.red))
        }
        Gap(6.dp)
        for (u in Upgrade.entries) {
            val has = u in state.upgrades
            val can = !has && state.parts >= u.cost && (u != Upgrade.MEMORY2 || Upgrade.MEMORY in state.upgrades)
            Frame(Modifier.padding(bottom = 6.dp), dashed = !has) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { T(u.t.of(ru)); T(u.about.of(ru), Ty.small) }
                    Spacer(Modifier.width(8.dp))
                    if (has) T(tr("стоит", "fitted"), Ty.small)
                    else Box(Modifier.width(110.dp)) { Btn(tr("${u.cost} дет.", "${u.cost} parts"), outline = !can, enabled = can) { co.buy(u) } }
                }
            }
        }
        Gap(10.dp)
        T(tr("Настройки", "Settings"), Ty.head)
        Gap(4.dp)
        Toggle(tr("Ночные существа", "Night creatures"), tr("Выключи — и ночные следы пропадут из эфира.", "Off: night-only traces disappear from the air."), !state.nightOff) { co.update { it.copy(nightOff = !it.nightOff) } }
        Toggle(tr("Звук эфира", "Sound of the air"), tr("Шум эфира, отклик радара и сигналы обряда.", "The air's hiss, the radar's pings and the rite's signals."), state.sound) { co.update { it.copy(sound = !it.sound) } }
        Toggle(tr("Тихий вариант", "Quiet way"), tr("Вместо пения — замереть на 20 секунд.", "Instead of singing, keep still for 20 seconds."), state.shy) { co.update { it.copy(shy = !it.shy) } }
        Gap(6.dp)
        T(tr("Тема", "Theme"), Ty.body)
        Gap(4.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((v, label) in listOf(0 to tr("Системная", "System"), 1 to tr("Светлая", "Light"), 2 to tr("Тёмная", "Dark"))) {
                Btn(label, Modifier.weight(1f), outline = state.theme != v) { co.update { it.copy(theme = v) } }
            }
        }
        Gap(10.dp)
        T(tr("Язык", "Language"), Ty.body)
        Gap(4.dp)
        val cur = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags().take(2)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((tag, label) in listOf("" to tr("Системный", "System"), "ru" to "Русский", "en" to "English")) {
                Btn(label, Modifier.weight(1f), outline = cur != tag) {
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        if (tag.isEmpty()) androidx.core.os.LocaleListCompat.getEmptyLocaleList() else androidx.core.os.LocaleListCompat.forLanguageTags(tag))
                }
            }
        }
        Gap(10.dp)
        if (fix != null) Btn(tr("Перенести архив сюда", "Move the archive here"), outline = true) { co.setHome(fix.at) }
        Gap(6.dp)
        Btn(tr("Пролог", "Prologue"), outline = true) { co.openTab.value = "intro" }
        Gap(12.dp)
        T(tr("Сообщество", "Community"), Ty.head)
        Gap(4.dp)
        val ctx = LocalContext.current
        for ((label, url) in listOf(
            tr("Чат StillCraft в Telegram", "StillCraft chat on Telegram") to "https://t.me/+Q4gFEDk8O842OGVi",
            tr("Сообщить об ошибке", "Report a bug") to "https://t.me/ArcaneWhisperBot?start=bug_fieldscan",
            tr("Предложить идею", "Suggest an idea") to "https://t.me/ArcaneWhisperBot?start=wish_fieldscan",
            tr("Следить за обновлениями", "Follow updates") to "https://t.me/ArcaneWhisperBot?start=f_fieldscan",
            tr("Исходный код", "Source code") to "https://github.com/bigdevwhale/fieldscan",
        )) T("→ $label", Ty.body, Modifier.fillMaxWidth().tap {
            runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }.padding(vertical = 7.dp))
        Gap(12.dp)
        T(tr("Карта © участники OpenStreetMap (ODbL). Погода — MET Norway. Шрифт m6x11 — Daniel Linssen.", "Map © OpenStreetMap contributors (ODbL). Weather by MET Norway. Font m6x11 by Daniel Linssen."), Ty.small)
    }
}

/** The trap's charge and where the steps come from: the phone's own counter, or Health Connect. */
@Composable
private fun StepSource(state: GameState) {
    val ru = LocalRu.current
    val ctx = LocalContext.current
    val co = ctx.container
    val health = state.health
    // null while asking Health Connect whether reading is still allowed
    var allowed by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(health) { allowed = co.healthSteps.allowed() }
    var note by remember { mutableStateOf<String?>(null) }
    val denied = tr("Health Connect не разрешил читать шаги.", "Health Connect didn't allow reading steps.")
    val install = tr("Сначала нужно установить или обновить Health Connect.", "Health Connect needs to be installed or updated first.")
    val none = tr("На этом телефоне нет Health Connect.", "This phone has no Health Connect.")
    val ask = rememberLauncherForActivityResult(HealthSteps.requestContract()) { got ->
        allowed = HealthSteps.READ in got
        if (allowed == true) { note = null; co.useHealth(true) } else note = denied
    }
    var sensorOk by remember { mutableStateOf(co.stepSensor.works) }
    val askSensor = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { sensorOk = co.stepSensor.works; if (it) co.startSteps() }
    fun pickHealth() {
        when {
            co.healthSteps.available -> ask.launch(setOf(HealthSteps.READ))
            co.healthSteps.installable -> { note = install; runCatching { ctx.startActivity(co.healthSteps.storeIntent()) } }
            else -> note = none
        }
    }
    Frame {
        Column {
            Row { T(tr("Заряд ловушки", "Trap charge"), Ty.body, Modifier.weight(1f)); T("${state.charge} / ${Rules.CHARGE_MAX}") }
            Gap(4.dp)
            Box(Modifier.fillMaxWidth().height(12.dp).border(2.dp, C.ink)) {
                Box(Modifier.fillMaxWidth(state.charge.toFloat() / Rules.CHARGE_MAX).height(12.dp).background(C.ink))
            }
            Gap(4.dp)
            T(tr("всего пройдено: ", "walked in all: ") + Words.steps(state.walked.toInt(), ru), Ty.small)
            Gap(8.dp)
            T(tr("Откуда шаги", "Where steps come from"), Ty.body)
            Gap(4.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Btn(tr("Телефон", "Phone"), Modifier.weight(1f), outline = health) { note = null; co.useHealth(false) }
                Btn("Health Connect", Modifier.weight(1f), outline = !health) { pickHealth() }
            }
            Gap(4.dp)
            val status = when {
                !health -> note ?: when {
                    !co.stepSensor.present -> tr("Шагомера нет — считаю по GPS.", "No step counter: reckoning by GPS.")
                    !sensorOk -> tr("Шагомеру нужно разрешение, пока — по GPS.", "The step counter needs permission; GPS until then.")
                    else -> tr("Считает, пока прибор открыт.", "Counts while the device is open.")
                }
                allowed == null -> "…"
                allowed == true -> tr("Считает и с закрытым прибором.", "Counts with the device closed too.")
                co.healthSteps.available -> denied
                co.healthSteps.installable -> install
                else -> none
            }
            T(status, Ty.small.copy(color = if (health && allowed == false) C.red else C.muted))
            if (!health && co.stepSensor.present && !sensorOk && android.os.Build.VERSION.SDK_INT >= 29) {
                Gap(4.dp)
                Btn(tr("Разрешить шагомер", "Allow the step counter"), outline = true) { askSensor.launch(android.Manifest.permission.ACTIVITY_RECOGNITION) }
            }
            if (health && allowed == false && (co.healthSteps.available || co.healthSteps.installable)) {
                Gap(4.dp)
                Btn(if (co.healthSteps.available) tr("Разрешить в Health Connect", "Allow in Health Connect") else tr("Установить Health Connect", "Install Health Connect"), outline = true) { pickHealth() }
            }
        }
    }
}

@Composable
private fun Toggle(title: String, about: String, on: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().tap(onClick = onClick).padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { T(title); T(about, Ty.small) }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(42.dp, 22.dp).border(2.dp, C.ink).background(if (on) C.ink else C.paper), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(Modifier.padding(3.dp).size(14.dp).background(if (on) C.red else C.ink))
        }
    }
}
