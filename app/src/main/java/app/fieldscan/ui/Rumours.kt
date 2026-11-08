package app.fieldscan.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Cond
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Story
import app.fieldscan.domain.Episodes
import app.fieldscan.container
import app.fieldscan.domain.Tier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** The last owner's journal: the newest page in full, the earlier ones a tap away, and what the next waits for. */
@Composable
private fun Journal(state: GameState, now: Long) {
    val ru = LocalRu.current
    val co = androidx.compose.ui.platform.LocalContext.current.container
    // newest page first, then its task and what's due next; then finds and found pages; then the older pages
    val pages = Story.pages.take(state.story).map { it.title to it.text }.asReversed()
    val aside = Story.bonusOpen(state).map { it.title to it.text } +
        app.fieldscan.domain.Finds.made(state).map { app.fieldscan.domain.T("Находка: " + it.title.ru, "Find: " + it.title.en) to it.text }
    var shown by remember { mutableStateOf(Story.pages.getOrNull(state.story - 1)?.title) }
    T(tr("Записи Н. Ветровой · ${state.story} из ${Story.pages.size}", "N. Vetrova's notes · ${state.story} of ${Story.pages.size}"), Ty.small)
    Section(tr("Новая страница", "Newest page"))
    @Composable
    fun entry(title: app.fieldscan.domain.T, text: app.fieldscan.domain.T) {
        val radio = Story.radio[title.en]
        val on = title == shown
        Frame(Modifier.padding(bottom = 6.dp).tap { shown = if (on) null else title }, dashed = !on) {
            Column {
                T((if (on) "▾ " else "▸ ") + title.of(ru), Ty.body)
                if (on) { Gap(4.dp); T(text.of(ru), Ty.small.copy(color = C.ink)) }
                if (on && radio != null) { Gap(6.dp); Intercept(radio.of(ru)) }
                if (!on && radio != null) T(tr("есть перехват", "has an intercept"), Ty.small.copy(color = C.red))
            }
        }
    }
    pages.firstOrNull()?.let { (t, x) -> entry(t, x) }
    // the episode: what the newest page asks to do out in the city
    Episodes.current(state)?.let { e ->
        val done = Episodes.done(state)
        Frame(Modifier.padding(bottom = 6.dp), red = !done) {
            Column {
                T(if (done) tr("ЗАДАНИЕ ВЫПОЛНЕНО ✓", "TASK DONE ✓") else tr("ЗАДАНИЕ ЭПИЗОДА", "THE EPISODE'S TASK"), Ty.small.copy(color = if (done) C.ink else C.red))
                T(e.say.of(ru), Ty.body)
                if (!done) {
                    T(tr("Награда: ${Episodes.PARTS} деталь", "Reward: ${Episodes.PARTS} part"), Ty.small)
                    if (Episodes.skippable(state, now)) {
                        Gap(6.dp)
                        Btn(tr("Не выходит — пропустить", "Can't do it: skip"), outline = true) { co.skipTask() }
                    }
                }
            }
        }
    }
    val w = Story.waiting(state, now)
    if (w == null) T(tr("Журнал дописан.", "The journal is complete."), Ty.small)
    else {
        val left = listOfNotNull(
            w.days.takeIf { it > 0 }?.let { tr("через $it ${Words.plural(it, "день", "дня", "дней")}", "in $it ${if (it == 1) "day" else "days"}") },
            w.catches.takeIf { it > 0 }?.let { tr("ещё поимок: $it", "$it more ${if (it == 1) "catch" else "catches"}") },
            w.steps.takeIf { it > 0 }?.let { tr("ещё ", "") + Words.steps(it.toInt(), ru) + tr("", " more") },
            w.tier?.let { tr("поймай: ", "catch a ") + Words.tier(it, ru) },
            w.caught.takeIf { it > 0 }?.let { tr("в бестиарии ещё $it", "$it more in the bestiary") },
            w.task?.let { tr("задание эпизода", "the episode's task") },
        )
        T(tr("Следующая страница: ", "Next page: ") + left.joinToString(" · ").ifEmpty { tr("вот-вот", "any moment") }, Ty.small)
    }
    if (aside.isNotEmpty() || pages.size > 1) Section(tr("Прежние страницы", "Earlier pages"), aside.size + pages.size - 1)
    for ((t, x) in aside.asReversed()) entry(t, x)
    for ((t, x) in pages.drop(1)) entry(t, x)
}

/** What the caught have told, newest first; and what the keepers are waiting for. */
@Composable
fun RumoursScreen(state: GameState, now: Long) {
    val ru = LocalRu.current
    val firsts = state.catches.groupBy { it.creature }.map { it.value.first() }.sortedByDescending { it.time }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Title(tr("Журнал", "Journal"), Tip.RUM)
        Journal(state, now)
        Section(tr("Слухи", "Rumours"), firsts.size.takeIf { it > 0 })
        Gap(6.dp)
        if (firsts.isEmpty()) Frame(dashed = true) { T(tr("Пока никто ничего не рассказал. Поймай первого.", "Nobody has told anything yet. Catch the first one."), Ty.small) }
        for (c in firsts) {
            val cr = Bestiary[c.creature]
            Frame(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(52.dp).border(1.dp, C.ink)) { FrameImage(rememberFrame(c, now, thumb = true), square = true) }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        T(cr.name.of(ru) + tr(" говорит:", " says:"), Ty.small)
                        QuoteBox(cr.rumour.of(ru))
                    }
                }
            }
        }
        Section(tr("Хозяева ждут", "The keepers wait"))
        for (cr in Bestiary.all.filter { it.tier == Tier.HOST }) {
            val caught = cr.id in state.caughtIds
            val needs = cr.conds.filter { it is Cond.With || it is Cond.Carry }
            Frame(Modifier.padding(bottom = 6.dp), dashed = !caught) {
                Column {
                    T(if (caught || cr.id in state.known) cr.name.of(ru) else tr("Хозяин — ", "Keeper — ") + Words.habitatOf(cr, ru), Ty.body)
                    T(if (caught) tr("пойман", "caught") else Words.habitatOf(cr, ru) + (if (needs.isNotEmpty()) " · " + needs.joinToString(", ") { Words.cond(it, ru).lowercase() } else ""), Ty.small)
                }
            }
        }
        if (state.items.isNotEmpty()) {
            Gap(6.dp)
            T(tr("С собой: ", "Carrying: ") + state.items.joinToString(", ") { it.t.of(ru) }, Ty.body)
        }
    }
}

