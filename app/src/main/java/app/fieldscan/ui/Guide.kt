package app.fieldscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.fieldscan.domain.GameState
import app.fieldscan.domain.Trace

/**
 * The first round, held by the hand: the homeling at the door, the archive, then one trace in the street.
 * It reads everything from the state, so it can't get stuck: each stage is over when its thing is done.
 */
object Guide {
    enum class Stage { DOOR, ARCHIVE, STREET, DONE }

    fun stage(s: GameState): Stage = when {
        !s.guide || s.home == null -> Stage.DONE
        s.caughtIds.size >= 2 -> Stage.DONE
        s.catches.isEmpty() -> Stage.DOOR
        s.catches.none { it.archived != null } -> Stage.ARCHIVE
        else -> Stage.STREET
    }

    /** The trace the guide points at on the air, if any. */
    fun lead(s: GameState, traces: List<Trace>): Trace? = when (stage(s)) {
        Stage.DOOR -> traces.firstOrNull { it.creature.id == "porog" }
        Stage.STREET -> traces.filter { it.open && !it.caught }.minByOrNull { it.dist }
        else -> null
    }
}

/** The banner at the top of the air: one task, a step number, and a way out. */
@Composable
fun GuideBanner(s: GameState, traces: List<Trace>, onDevice: () -> Unit, onOpen: (Trace) -> Unit, onOff: () -> Unit) {
    val ru = LocalRu.current
    val stage = Guide.stage(s)
    if (stage == Guide.Stage.DONE) return
    val lead = Guide.lead(s, traces)
    val n = stage.ordinal + 1
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).border(2.dp, C.red).background(C.paper).padding(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(tr("ПЕРВЫЙ ОБХОД · $n/3", "FIRST ROUND · $n/3"), Ty.small.copy(color = C.red), Modifier.weight(1f))
            T("×", Ty.body.copy(color = C.muted), Modifier.tap(onClick = onOff).padding(horizontal = 6.dp))
        }
        when (stage) {
            Guide.Stage.DOOR -> {
                T(tr("У твоего порога кто-то есть", "Someone is at your door"), Ty.head)
                T(tr("Домовые живут у дома и выходят без особых условий. Нажми на след «Дом» — откроется обряд: постой рядом и полминуты не двигай телефон. Потом сними его фотоловушкой.",
                    "Homelings live by the house and come out with no special conditions. Tap the «Home» trace: the rite opens. Stay close and keep the phone still for half a minute. Then shoot it with the trail camera."), Ty.small.copy(color = C.ink))
                lead?.let { Gap(6.dp); Btn(tr("Открыть обряд у порога", "Open the rite at the door"), red = true) { onOpen(it) } }
            }
            Guide.Stage.ARCHIVE -> {
                T(tr("Сдай кадр в архив", "Hand the frame in"), Ty.head)
                T(tr("Кадры в памяти портятся — по ступени в день. Дома их сдают в архив, и там они хранятся навсегда. Ты как раз дома.",
                    "Frames in memory wear, a step a day. At home they go into the archive and keep for good. You're home right now."), Ty.small.copy(color = C.ink))
                Gap(6.dp); Btn(tr("В Прибор → Сдать в архив", "To Device → hand in"), red = true, onClick = onDevice)
            }
            Guide.Stage.STREET -> {
                T(tr("Теперь — улица", "Now the street"), Ty.head)
                if (lead != null) {
                    T(tr("Ближайший живой след: ", "The nearest awake trace: ") + lead.place.habitat.name(ru) + " · " + Words.dist(lead.dist, ru) + " · " + app.fieldscan.domain.Geo.side(lead.bearing, ru) +
                        tr(". Открой его обряд, прочитай загадку и иди. Подойдёшь ближе 120 м — прибор позовёт.", ". Open its rite, read the riddle and go. Within 120 m the device will call you."), Ty.small.copy(color = C.ink))
                    Gap(6.dp); Btn(tr("Открыть обряд", "Open the rite"), red = true) { onOpen(lead) }
                } else T(tr("Сейчас все рядом спят. Загляни на закате или ночью — тогда эфир оживает.", "Everyone nearby is asleep. Look in at sunset or at night: that's when the air wakes."), Ty.small.copy(color = C.ink))
            }
            Guide.Stage.DONE -> Unit
        }
    }
}

/** A small red pointer on the trace the guide means. */
@Composable
fun LeadMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(4.dp))
        Tag(tr("сюда", "here"), red = true)
    }
}
