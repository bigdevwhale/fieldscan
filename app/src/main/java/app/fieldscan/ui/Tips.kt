package app.fieldscan.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.fieldscan.domain.T as Tx

/** A page of the device's manual, shown the first time a screen opens and again from its «?». */
enum class Tip(val title: Tx, val lines: List<Tx>) {
    SCAN(Tx("Эфир", "The air"), listOf(
        Tx("Круг — это радар. Точки на нём — следы существ вокруг тебя: чем ближе к центру, тем ближе место.",
            "The circle is the radar. Its dots are traces of creatures around you: the nearer the centre, the nearer the place."),
        Tx("Радар поворачивается вместе с телефоном: вверху — то, что перед тобой. Красная галочка — куда ты смотришь, «N» — север. Нажми на круг, чтобы держать север вверху.",
            "The radar turns with the phone: the top is what's in front of you. The red notch is where you face, «N» is north. Tap the dial to keep north up."),
        Tx("Под радаром — тип места, расстояние и направление. Столбики справа — сила сигнала.",
            "Below it: the kind of place, the distance and the direction. The bars on the right are signal strength."),
        Tx("Самого существа прибор не видит. Нажми на след — откроются загадка и обряд. Под каждым следом написано, можно ли его ловить сейчас.",
            "The device can't see the creature itself. Tap a trace to open its riddle and rite. Under each trace it says whether it can be caught now."),
        Tx("Как поймать: нажми на след → прочитай обряд → дойди до места → выполни шаги → сними фотоловушкой. Когда подойдёшь к живому следу ближе 120 м, прибор сам позовёт.",
            "How to catch: tap a trace → read the rite → walk to the place → do the steps → shoot with the trail camera. Within 120 m of an awake trace the device calls you itself."),
        Tx("«Спит» значит, что сейчас не его время. Вернись в сумерках, ночью или на рассвете.",
            "«Asleep» means it isn't its time. Come back at dusk, at night or at dawn."),
    )),
    RITE(Tx("Обряд", "The rite"), listOf(
        Tx("Сверху загадка: где ждать и что сделать.", "At the top is the riddle: where to wait and what to do."),
        Tx("Галочки — условия: время суток, погода, кого взять с собой. Пока они не сошлись, шаги закрыты.",
            "The ticks are conditions: time of day, weather, who to bring. Until they're met, the steps stay shut."),
        Tx("Шаги идут по порядку. Каждый проверяет телефон: координаты, компас, микрофон, фонарик, неподвижность.",
            "The steps go in order. Each is checked by the phone: position, compass, microphone, torch, stillness."),
        Tx("Под каждым шагом написано, как его проверяет телефон и что засчитывается.",
            "Under each step it says how the phone checks it and what counts."),
        Tx("Ловушка заряжается шагами. Чем сильнее существо, тем больше заряда нужно, чтобы его удержать: обитателю — 500–700 шагов, хозяину — 3000, легенде — 6000.",
            "The trap charges with steps. The stronger the creature, the more charge it takes to hold: a dweller 500–700 steps, a keeper 3000, a legend 6000."),
        Tx("Сбился — шаг начнётся заново. Пройденные шаги прибор помнит два часа, можно уйти и вернуться.",
            "Slip up and the step starts over. Steps already done are kept for two hours, so you can leave and come back."),
    )),
    BEST(Tx("Бестиарий", "The bestiary"), listOf(
        Tx("Обитатели живут в типах мест, хозяева — в «самых-самых» местах района, странники приходят с погодой, легенды стоят на точках планеты, домовые — у твоего дома.",
            "Dwellers live in kinds of places, keepers in the district's most-of-all places, wanderers come with the weather, legends stand on points of the planet, homelings live at your home."),
        Tx("Кадр — снимок фотоловушки в момент поимки. У каждого свой: место, время и погода видны на нём.",
            "A frame is the trail camera's shot at the catch. Each is its own: the place, the hour and the weather show in it."),
        Tx("Тёмный кадр с огоньками глаз — загадка: кто-то там есть, но ты его ещё не поймал.",
            "A dark frame with a pair of eyes is a riddle: someone is there, you just haven't caught it yet."),
    )),
    RUM(Tx("Слухи", "Rumours"), listOf(
        Tx("Сверху — журнал прежнего хозяина прибора. Страницы открываются со временем, примерно раз в месяц, если ты ходишь и ловишь. Всего их на три года.",
            "At the top is the journal of the device's last owner. Pages open with time, about once a month, as long as you walk and catch. There are three years of them."),
        Tx("Каждый пойманный рассказывает о ком-то ещё. Так открываются новые загадки.",
            "Everyone caught tells about someone else. That's how new riddles open."),
        Tx("Ниже — хозяева района и то, чего они ждут: спутника в памяти или вещь с собой.",
            "Below are the district's keepers and what they wait for: a companion in memory or a thing to carry."),
    )),
    DEV(Tx("Прибор", "The device"), listOf(
        Tx("Память маленькая: каждое существо занимает от 1 до 6 КБ.", "Memory is small: each creature takes 1 to 6 KB."),
        Tx("Снимки в памяти портятся каждый день. Вернись домой и сдай их в архив — там они хранятся навсегда.",
            "Frames in memory wear every day. Go home and hand them in to the archive, where they keep for good."),
        Tx("Заряд ловушки копится от шагов: из шагомера телефона или из Health Connect — тогда засчитываются и шаги, пройденные с закрытым прибором.",
            "The trap's charge builds from steps: from the phone's step counter, or from Health Connect, which also counts steps walked with the device closed."),
        Tx("За новых существ дают детали. На них ставятся насадки: дальше антенна, больше память.",
            "New creatures bring parts. Parts buy attachments: a longer antenna, more memory."),
    )),
}

/** Lets a screen's «?» open its page of the manual. */
val LocalHelp = staticCompositionLocalOf<(Tip) -> Unit> { {} }

/** A screen title with the small «?» beside it. */
@Composable
fun Title(text: String, tip: Tip, modifier: Modifier = Modifier) {
    val help = LocalHelp.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        T(text, Ty.head)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(22.dp).border(2.dp, C.ink).tap { help(tip) }, contentAlignment = Alignment.Center) { T("?", Ty.small.copy(color = C.ink)) }
    }
}

/** The manual page over the screen. */
@Composable
fun TipOverlay(tip: Tip, onClose: () -> Unit) {
    val ru = LocalRu.current
    Box(Modifier.fillMaxSize().background(C.scrim).tap(onClick = onClose), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(14.dp).fillMaxWidth().background(C.paper).border(2.dp, C.ink).tap { }
                .verticalScroll(rememberScrollState()).padding(14.dp),
        ) {
            T(tr("ИНСТРУКЦИЯ ПРИБОРА", "DEVICE MANUAL"), Ty.small.copy(color = C.red))
            T(tip.title.of(ru), Ty.head)
            Gap(8.dp)
            tip.lines.forEachIndexed { i, l ->
                Row(Modifier.padding(bottom = 7.dp)) {
                    Box(Modifier.size(20.dp).background(C.ink), contentAlignment = Alignment.Center) { T("${i + 1}", Ty.small.copy(color = C.paper)) }
                    Spacer(Modifier.width(9.dp))
                    T(l.of(ru), Ty.body, Modifier.weight(1f))
                }
            }
            Gap(6.dp)
            Btn(tr("Понятно", "Got it"), onClick = onClose)
        }
    }
}
