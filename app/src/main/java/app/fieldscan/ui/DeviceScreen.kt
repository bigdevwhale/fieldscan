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
import androidx.compose.runtime.Composable
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
                        row.forEach { k -> Box(Modifier.weight(1f).aspectRatio(1f).border(2.dp, if (k == 2) C.red else C.ink).background(when (k) { 1 -> C.ink; 2 -> C.red; else -> C.paper })) }
                    }
                }
                Gap(4.dp)
                if (state.inMemory.isNotEmpty()) T(state.inMemory.joinToString(", ") { Bestiary[it.creature].name.of(ru) + (Rules.worn(it, now).takeIf { w -> w > 0 }?.let { w -> " ($w/4)" } ?: "") }, Ty.small)
                T(tr("Снимки в памяти портятся: каждый день на ступень. В архиве — навсегда.", "Frames in memory wear a step a day. In the archive, they're kept for good."), Ty.small)
                Gap(8.dp)
                Btn(
                    if (atHome) tr("Сдать в архив", "Hand in to the archive") else tr("Сдать в архив · дом ", "Hand in · home ") + (toHome?.let { Words.dist(it, ru) } ?: "?"),
                    enabled = atHome && state.inMemory.isNotEmpty(),
                ) { co.archive() }
            }
        }
        Gap(10.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(tr("Насадки", "Attachments"), Ty.head, Modifier.weight(1f))
            T(tr("детали: ", "parts: ") + state.parts, Ty.body.copy(color = C.red))
        }
        T(tr("Детали дают за новых: обитатель — 1, странник и домовой — 2, хозяин — 3, легенда — 5.", "Parts come with new catches: dweller 1, wanderer and homeling 2, keeper 3, legend 5."), Ty.small)
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
        Toggle(tr("Тихий вариант", "Quiet way"), tr("Вместо пения — замереть на 20 секунд.", "Instead of singing, keep still for 20 seconds."), state.shy) { co.update { it.copy(shy = !it.shy) } }
        Gap(6.dp)
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
