package app.fieldscan.ui

import app.fieldscan.domain.Bestiary
import app.fieldscan.domain.Cond
import app.fieldscan.domain.Creature
import app.fieldscan.domain.Daypart
import app.fieldscan.domain.Facing
import app.fieldscan.domain.Habitat
import app.fieldscan.domain.Notes
import app.fieldscan.domain.Step
import app.fieldscan.domain.Tier
import java.util.Locale
import kotlin.math.roundToInt

/** All the small phrases of the device, in both languages. */
object Words {
    fun dist(m: Double, ru: Boolean): String = when {
        m < 1000 -> "${(m / 10).roundToInt() * 10} ${if (ru) "м" else "m"}"
        else -> String.format(if (ru) Locale("ru") else Locale.US, "%.1f %s", m / 1000, if (ru) "км" else "km")
    }

    fun part(p: Daypart, ru: Boolean) = when (p) {
        Daypart.NIGHT -> if (ru) "ночь" else "night"
        Daypart.DAWN -> if (ru) "рассвет" else "dawn"
        Daypart.DAY -> if (ru) "день" else "day"
        Daypart.DUSK -> if (ru) "сумерки" else "dusk"
    }

    fun parts(ps: Set<Daypart>, ru: Boolean): String = when {
        ps == setOf(Daypart.DUSK, Daypart.NIGHT) -> if (ru) "после заката" else "after sunset"
        ps == setOf(Daypart.NIGHT) -> if (ru) "ночью" else "at night"
        ps == setOf(Daypart.DUSK) -> if (ru) "в сумерках" else "at dusk"
        ps == setOf(Daypart.DAWN) -> if (ru) "на рассвете" else "at dawn"
        ps == setOf(Daypart.DAWN, Daypart.DAY) -> if (ru) "утром и днём" else "morning and day"
        ps == setOf(Daypart.DAY, Daypart.DUSK) -> if (ru) "днём и в сумерках" else "by day and at dusk"
        else -> ps.joinToString(", ") { part(it, ru) }
    }

    fun cond(c: Cond, ru: Boolean): String = when (c) {
        is Cond.Window -> parts(c.parts, ru).replaceFirstChar { it.uppercase() }
        Cond.Fog -> if (ru) "Туман" else "Fog"
        Cond.Snow -> if (ru) "Снегопад" else "Snowfall"
        Cond.Frost -> if (ru) "Мороз" else "Frost"
        Cond.FullMoon -> if (ru) "Полная луна над горизонтом" else "A full moon above the horizon"
        is Cond.With -> (if (ru) "В памяти: " else "In memory: ") + Bestiary[c.creature].name.of(ru)
        is Cond.Without -> (if (ru) "Не брать с собой: " else "Don't carry: ") + Bestiary[c.creature].name.of(ru)
        is Cond.Carry -> (if (ru) "С собой: " else "Carrying: ") + c.item.t.of(ru)
    }

    fun facing(f: Facing, ru: Boolean) = when (f) {
        Facing.UPSTREAM -> if (ru) "против течения" else "upstream"
        Facing.NORTH -> if (ru) "на север" else "north"
        Facing.SOUTH -> if (ru) "на юг" else "south"
        Facing.WEST -> if (ru) "на запад, к закату" else "west, to the sunset"
        Facing.EAST -> if (ru) "на восток" else "east"
        Facing.WIND -> if (ru) "лицом к ветру" else "into the wind"
        Facing.TOWARD -> if (ru) "лицом к месту" else "towards the place"
    }

    fun sec(s: Int, ru: Boolean) = when {
        s % 60 == 0 && s >= 60 -> "${s / 60} ${if (ru) plural(s / 60, "минуту", "минуты", "минут") else if (s == 60) "minute" else "minutes"}"
        else -> "$s ${if (ru) plural(s, "секунду", "секунды", "секунд") else "seconds"}"
    }

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = n % 100; val b = n % 10
        return when { a in 11..14 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    fun step(s: Step, h: Habitat, ru: Boolean, shy: Boolean): String = when (s) {
        is Step.Near -> if (ru) "Подойди: ближе ${s.m} м" else "Come within ${s.m} m"
        Step.Middle -> if (ru) "Встань ровно посередине" else "Stand right in the middle"
        is Step.Face -> if (ru) "Повернись ${facing(s.to, true)}" else "Turn ${facing(s.to, false)}"
        is Step.Still -> if (ru) "Замри на ${sec(s.sec, true)}" else "Keep still for ${sec(s.sec, false)}"
        is Step.FaceDown -> if (ru) "Положи телефон экраном вниз на ${sec(s.sec, true)}" else "Lay the phone face down for ${sec(s.sec, false)}"
        is Step.Note -> if (shy) (if (ru) "Тихий вариант: замри на 20 секунд" else "Quiet way: keep still for 20 seconds")
            else if (ru) "Спой ноту «${Notes.name(s.note, true)}» и держи три секунды" else "Sing ${Notes.name(s.note, false)} and hold it three seconds"
        is Step.Morse -> if (ru) "Мигни фонариком «${s.letter.ru}»: ${s.code.replace('.', '·').replace('-', '–')}" else "Blink the torch \"${s.letter.en}\": ${s.code.replace('.', '·').replace('-', '–')}"
        is Step.Dark -> if (ru) "Подожди в темноте ${sec(s.sec, true)}" else "Wait in the dark for ${sec(s.sec, false)}"
        is Step.Circle -> if (ru) "Обойди ${if (s.turns == 1) "один раз" else "${s.turns} раза"} ${if (s.ccw) "против часовой" else "по солнцу (по часовой)"}"
            else "Walk round ${if (s.turns == 1) "once" else "${s.turns} times"} ${if (s.ccw) "anticlockwise" else "sunwise (clockwise)"}"
        Step.Climb -> if (ru) "Поднимись снизу доверху без остановок" else "Climb from bottom to top without stopping"
        is Step.Wait -> if (ru) "Побудь здесь ${s.min} ${plural(s.min, "минуту", "минуты", "минут")}" else "Stay here for ${s.min} minutes"
        is Step.Walk -> if (ru) "Пройди ${s.m} м${if (s.water) " вдоль воды" else ""}, не сворачивая" else "Walk ${s.m} m${if (s.water) " along the water" else ""} without turning off"
        is Step.Hush -> if (ru) "Не трогай экран ${sec(s.sec, true)}" else "Don't touch the screen for ${sec(s.sec, false)}"
    }

    fun tier(t: Tier, ru: Boolean) = when (t) {
        Tier.DWELLER -> if (ru) "обитатель" else "dweller"
        Tier.HOST -> if (ru) "хозяин" else "keeper"
        Tier.WANDERER -> if (ru) "странник" else "wanderer"
        Tier.LEGEND -> if (ru) "легенда" else "legend"
        Tier.HOMELY -> if (ru) "домовой" else "homeling"
    }

    fun habitatOf(c: Creature, ru: Boolean): String = when (c.tier) {
        Tier.HOST -> when (c.most) {
            app.fieldscan.domain.Most.OLDEST_TREE -> if (ru) "самое старое дерево района" else "the oldest tree in the district"
            app.fieldscan.domain.Most.LONGEST_STAIRS -> if (ru) "самая длинная лестница района" else "the longest stairs in the district"
            app.fieldscan.domain.Most.LONGEST_BRIDGE -> if (ru) "самый длинный мост района" else "the longest bridge in the district"
            app.fieldscan.domain.Most.BUSIEST_CROSSING -> if (ru) "самый большой перекрёсток района" else "the biggest crossroads in the district"
            null -> c.habitat.name(ru)
        }
        Tier.LEGEND -> if (ru) "пересечение целых градусов" else "a whole-degree confluence"
        Tier.HOMELY -> if (ru) "у твоего дома" else "at your home"
        Tier.WANDERER -> c.conds.filter { it !is Cond.Window }.joinToString(", ") { cond(it, ru).lowercase() } + (if (ru) " · " else " · ") + c.habitat.name(ru).lowercase()
        Tier.DWELLER -> when (c.habitat) {
            Habitat.BRIDGE -> if (ru) "мосты над текучей водой" else "bridges over running water"
            Habitat.SPRING -> if (ru) "родники, фонтаны, колодцы" else "springs, fountains, wells"
            Habitat.STEPS -> if (ru) "длинные лестницы" else "long stairs"
            Habitat.STOP -> if (ru) "остановки" else "bus stops"
            Habitat.PHARMACY -> if (ru) "аптеки" else "pharmacies"
            Habitat.LAMP -> if (ru) "фонари" else "street lamps"
            Habitat.TREE -> if (ru) "деревья" else "trees"
            Habitat.PLAYGROUND -> if (ru) "детские площадки" else "playgrounds"
            Habitat.CROSSING -> if (ru) "перекрёстки пяти дорог" else "crossroads of five roads"
            Habitat.WORSHIP -> if (ru) "храмы и часовни" else "churches and chapels"
            Habitat.MEMORIAL -> if (ru) "памятники" else "monuments"
            Habitat.TOWER -> if (ru) "башни" else "towers"
            Habitat.PIER -> if (ru) "причалы" else "piers"
            Habitat.UNDERPASS -> if (ru) "подземные переходы" else "underpasses"
            Habitat.POSTBOX -> if (ru) "почтовые ящики" else "postboxes"
            Habitat.BENCH -> if (ru) "скамейки" else "benches"
            Habitat.VIEWPOINT -> if (ru) "смотровые площадки" else "viewpoints"
            Habitat.WATER -> if (ru) "берега рек" else "riverbanks"
            Habitat.HOME -> if (ru) "дом" else "home"
        }
    }

    fun temp(t: Double) = (if (t >= 0) "+" else "−") + kotlin.math.abs(t).roundToInt() + "°"
}
