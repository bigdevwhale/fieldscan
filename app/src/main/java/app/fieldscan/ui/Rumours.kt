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
import app.fieldscan.domain.Tier

/** What the caught have told, newest first; and what the keepers are waiting for. */
@Composable
fun RumoursScreen(state: GameState, now: Long) {
    val ru = LocalRu.current
    val firsts = state.catches.groupBy { it.creature }.map { it.value.first() }.sortedByDescending { it.time }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Title(tr("Слухи", "Rumours"), Tip.RUM)
        T(tr("Пойманные болтают. Так открываются следующие загадки.", "The caught talk. That's how the next riddles open."), Ty.small)
        Gap(8.dp)
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
        Rule()
        T(tr("Хозяева ждут", "The keepers wait"), Ty.head)
        Gap(4.dp)
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

