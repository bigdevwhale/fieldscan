package app.fieldscan.domain

import kotlin.math.round

/**
 * The story out in the street: when certain journal pages open, a find appears on the air, pinned to a real
 * place near home ([habitat]; or a bare point when [habitat] is null). Walking there opens a scene and a page.
 * [night] finds only answer after sunset.
 */
data class Find(
    val id: String,
    /** The journal page (its English title) that brings it. */
    val after: String,
    val habitat: Habitat?,
    val night: Boolean,
    val title: T,
    val hint: T,
    val text: T,
)

object Finds {
    const val PARTS = 2
    /** How near counts as "found", in metres (more with a poor fix). */
    const val REACH = 30.0

    val all: List<Find> = listOf(
        Find("pustoe", "They tore down the stairs", Habitat.STEPS, false,
            T("Пустое место", "An empty place"),
            T("Ветрова пишет о лестнице, которую снесли. Проверь ближайшую к дому лестницу: жива ли она.",
                "Vetrova writes of stairs that were torn down. Check the stairs nearest your home: are they still alive?"),
            T("Лестница на месте. Прибор слушает долго — и наконец: «…шарк… шарк…». Здесь кто-то живёт. Значит, это место ещё помнят.",
                "The stairs are still there. The device listens a long time, and at last: \"…scuff… scuff…\". Someone lives here. So this place is still remembered.")),
        Find("peredacha", "Night transmissions", null, true,
            T("Ночная передача", "A night transmission"),
            T("Ветрова засекла сигнал со стороны Слияния. Прибор отметил точку приёма — приходи ночью.",
                "Vetrova took a bearing on a signal from the Confluence. The device has marked a listening point: come at night."),
            T("Прибор оживает сам: поток цифр, щелчки, потом голос — не разобрать ни слова. Стрелка упрямо смотрит в одну сторону, на Слияние. Значит, она не ошиблась.",
                "The device wakes by itself: a stream of figures, clicks, then a voice, not a word to make out. The needle points stubbornly one way, at the Confluence. So she was right.")),
        Find("metka6", "A number on the railing", Habitat.BRIDGE, false,
            T("Метка «6»", "The mark «6»"),
            T("Ветрова нашла цифру на перилах моста. Проверь ближайший к дому мост — перила, где посередине.",
                "Vetrova found a number on a bridge railing. Check the bridge nearest your home: the railing, in the middle."),
            T("Под слоем краски — процарапанная «6». Рядом стрелка: вниз по течению. Шестой наблюдатель был здесь. Куда он шёл?",
                "Under the paint, a scratched «6». Beside it an arrow: downstream. The sixth observer was here. Where was he going?")),
        Find("skameyka", "The sixth observer", Habitat.BENCH, false,
            T("Скамейка шестого", "The sixth's bench"),
            T("Прибор шестого нашли на скамейке в сквере. Посиди на ближайшей к дому скамейке.",
                "The sixth's device was found on a bench in a square. Sit on the bench nearest your home."),
            T("Скамейка как скамейка. Но прибор вдруг ловит чужой позывной — слабый, старый: «шестой… на обходе… шестой…». Запись обрывается. Он сидел здесь последним.",
                "A bench like any bench. But the device suddenly picks up someone else's call sign, faint and old: \"sixth… on the round… sixth…\". The recording breaks off. He sat here last.")),
        Find("bezmesta", "A trace with no place", null, true,
            T("След без места", "A trace with no place"),
            T("Ночью прибор поймал след там, где на карте ничего нет. Сходи туда после заката.",
                "At night the device caught a trace where the map shows nothing. Go there after sunset."),
            T("Здесь пусто. Ни фонаря, ни скамейки. Только прибор шепчет: «…шаги… шаги… шаги…». Ты стоишь в её следах.",
                "Nothing here. No lamp, no bench. Only the device whispering: \"…steps… steps… steps…\". You're standing in her footsteps.")),
        Find("tuk", "Knock", Habitat.HOME, true,
            T("Тук", "Knock"),
            T("У архива новый след. Возвращайся домой, когда стемнеет.",
                "A new trace by the archive. Come home after dark."),
            T("Тихо. Потом — три лёгких стука изнутри стены. Прибор пишет: «хозяйка участка · сила 5 · дома». Она здесь.",
                "Quiet. Then three light knocks from inside the wall. The device writes: \"keeper of the district · strength 5 · home\". She's here.")),
    )

    private fun pageOf(f: Find) = Story.pages.indexOfFirst { it.title.en == f.after }

    /** Finds whose page is open and that aren't found yet. */
    fun open(s: GameState): List<Find> = all.filter { f -> pageOf(f).let { it >= 0 && s.story > it } && f.id !in s.finds }

    /** Finds already made, in order, for the journal. */
    fun made(s: GameState): List<Find> = all.filter { it.id in s.finds }

    /**
     * Where a find is: the nearest place of its kind to home, or a bare point near home (towards the
     * confluence for the night transmission). Chosen once and kept in the state, so it never wanders.
     */
    fun pick(f: Find, home: Geo, places: List<Place>): Geo {
        if (f.habitat == Habitat.HOME) return home
        f.habitat?.let { h -> places.filter { it.habitat == h }.minByOrNull { it.at.dist(home) }?.takeIf { it.at.dist(home) < 3000 }?.let { return it.mid } }
        val conf = Geo(round(home.lat), round(home.lon))
        val bearing = if (f.id == "peredacha") home.bearing(conf) else (Hash.unit(Hash.of(f.id + home.lat.toString()), 3) * 360)
        return home.offset(bearing, if (f.id == "peredacha") 500.0 else 380.0)
    }
}
