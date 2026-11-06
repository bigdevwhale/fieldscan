package app.fieldscan.domain

/** What an episode asks of the observer between one journal page and the next. */
sealed interface Task {
    /** Come to any place of [habitat] (within [Episodes.REACH] metres), at [parts] of the day if given. */
    data class Visit(val habitat: Habitat, val parts: Set<Daypart>? = null) : Task
    /** Go further than [m] metres from home. */
    data class Far(val m: Int) : Task
    /** Catch anyone ([new]: someone not yet in the bestiary). */
    data class Catch(val new: Boolean = false) : Task
    /** Hand frames in to the archive. */
    data object Archive : Task
    /** Come back to where the first catch was made. */
    data object First : Task
    /** Walk [n] steps. */
    data class Steps(val n: Int) : Task
    /** Make the story find [id]. */
    data class Find(val id: String) : Task
}

/** An episode: the page it belongs to (its English title), the task, and the words that set it. */
data class Episode(val page: String, val task: Task, val say: T)

/**
 * The journal as a series: most pages come with one thing to do out in the city, and the next page waits
 * for it. Done tasks are kept by page index in [GameState.tasks]; those the state alone can show (catches,
 * the archive, steps, finds) are read straight from it. After [SKIP_DAYS] a task may be let go.
 */
object Episodes {
    const val REACH = 40.0
    const val SKIP_DAYS = 21
    const val PARTS = 1

    private val NIGHT = setOf(Daypart.DUSK, Daypart.NIGHT)

    val all: List<Episode> = listOf(
        Episode("They tore down the stairs", Task.Find("pustoe"), T("Проверь ближайшую лестницу: жива ли она.", "Check the nearest stairs: are they still alive?")),
        Episode("The Bureau", Task.Visit(Habitat.POSTBOX), T("Отправь весточку Бюро: подойди к любому почтовому ящику.", "Send the Bureau word: go to any postbox.")),
        Episode("What a frame is", Task.Catch(), T("Сделай кадр — любой. Посмотри, насколько он чёткий.", "Take a frame, any. See how sharp it comes out.")),
        Episode("Empty lamps", Task.Visit(Habitat.LAMP, NIGHT), T("Подойди к фонарю после заката. Посмотри, горит ли.", "Go to a street lamp after sunset. See whether it's lit.")),
        Episode("The first keeper", Task.Archive, T("Сдай кадры в архив: хозяин должен знать, что ты помнишь.", "Hand frames in to the archive: the keeper must know you remember.")),
        Episode("The keeper's answer", Task.Far(1500), T("Уйди от дома дальше чем на полтора километра — туда, где давно никто не ходил.", "Go further than a kilometre and a half from home, where nobody has walked for a long time.")),
        Episode("The other districts", Task.Visit(Habitat.WORSHIP, setOf(Daypart.DUSK)), T("В сумерках подойди к храму: колокол — старый способ звать своих.", "At dusk go to a church: a bell is the old way to call your own.")),
        Episode("The Bureau's address", Task.Visit(Habitat.STOP, NIGHT), T("Бюро возило наблюдателей ночными автобусами. Постой на остановке после заката.", "The Bureau drove its observers on night buses. Stand at a stop after sunset.")),
        Episode("Night transmissions", Task.Find("peredacha"), T("Найди точку приёма и послушай ночью.", "Find the listening point and listen at night.")),
        Episode("Wanderers", Task.Catch(), T("Поймай кого-нибудь — погода сама подскажет кого.", "Catch someone: the weather will tell you who.")),
        Episode("A year", Task.Archive, T("Год обхода. Сдай кадры — пусть этот год останется в архиве.", "A year of rounds. Hand the frames in: let the year stay in the archive.")),
        Episode("A number on the railing", Task.Find("metka6"), T("Найди метку на ближайшем мосту.", "Find the mark on the nearest bridge.")),
        Episode("The sixth observer", Task.Find("skameyka"), T("Посиди на скамейке шестого.", "Sit on the sixth's bench.")),
        Episode("Frames going dark", Task.Archive, T("Сдай кадры, пока они не потемнели.", "Hand the frames in before they darken.")),
        Episode("What erases", Task.First, T("Вернись к месту своей первой поимки. Обход лечит.", "Go back to where you made your first catch. Walking the round heals.")),
        Episode("Homelings", Task.Archive, T("Береги дом: сдай всё, что носишь, в архив.", "Look after home: hand everything you carry in to the archive.")),
        Episode("The five-road crossing", Task.Far(2000), T("Уйди от дома дальше чем на два километра: туда сходятся дороги.", "Go further than two kilometres from home: that's where roads meet.")),
        Episode("Points that aren't there", Task.Far(2500), T("Дальше, чем на два с половиной километра. К краю своей карты.", "Further than two and a half kilometres. To the edge of your map.")),
        Episode("Getting ready", Task.Steps(30_000), T("Копи, как она: пройди тридцать тысяч шагов.", "Save up as she did: walk thirty thousand steps.")),
        Episode("Gone", Task.Visit(Habitat.BENCH), T("Посиди на скамейке. Просто посиди.", "Sit on a bench. Just sit.")),
        Episode("Another hand", Task.Catch(), T("Участок теперь твой. Сделай первый кадр как его наблюдатель.", "The district is yours now. Take your first frame as its observer.")),
        Episode("A trace with no place", Task.Find("bezmesta"), T("Сходи туда, где на карте ничего нет. Ночью.", "Go where the map shows nothing. At night.")),
        Episode("Two years", Task.Archive, T("Два года. Сдай кадры.", "Two years. Hand the frames in.")),
        Episode("Where she is", Task.Visit(Habitat.LAMP, NIGHT), T("Постой у фонаря ночью. Её фонари — твои шаги.", "Stand by a lamp at night. Her lamps are your steps.")),
        Episode("The riddles are hers", Task.Catch(new = true), T("Поймай того, кого ещё нет в бестиарии: она вывела его для тебя.", "Catch someone not yet in the bestiary: she led it out for you.")),
        Episode("A map of looking", Task.Far(3000), T("Уйди на три километра. Пусть горит дальше.", "Go three kilometres out. Let it shine further.")),
        Episode("The sixth wakes", Task.Visit(Habitat.STOP), T("Встреть шестого: он ждёт на остановке.", "Meet the sixth: he's waiting at a stop.")),
        Episode("Twelve", Task.Steps(40_000), T("Пройди сорок тысяч шагов. Двенадцать участков слышат каждый.", "Walk forty thousand steps. Twelve districts hear every one.")),
        Episode("The last crossing", Task.Catch(new = true), T("Ещё один камень на её дорогу: новый в бестиарии.", "One more stone on her road: someone new in the bestiary.")),
        Episode("Knock", Task.Find("tuk"), T("Иди домой, когда стемнеет.", "Go home when it's dark.")),
        Episode("A call sign", Task.Visit(Habitat.TREE, setOf(Daypart.DAWN)), T("Встреть рассвет у дерева. Скажи в эфир: «Седьмой на обходе».", "Meet the dawn by a tree. Say into the air: \"Seventh on the round\".")),
    )

    fun of(page: Int): Episode? = Story.pages.getOrNull(page)?.let { p -> all.firstOrNull { it.page == p.title.en } }

    /** The episode of the newest open page: what the next page waits for. */
    fun current(s: GameState): Episode? = of(s.story - 1)

    /** Whether the current episode's task is done (or there is none). */
    fun done(s: GameState): Boolean {
        val i = s.story - 1
        val e = of(i) ?: return true
        if (i in s.tasks) return true
        return when (val t = e.task) {
            is Task.Catch -> if (t.new) s.caughtIds.size > s.pageKinds else s.catches.size > s.pageCatches
            Task.Archive -> s.catches.any { (it.archived ?: 0) >= s.pageAt && s.pageAt > 0 }
            is Task.Steps -> s.walked - s.pageWalked >= t.n
            is Task.Find -> t.id in s.finds
            else -> false
        }
    }

    /** A task that needs the observer somewhere: checked against a fix and the places around. */
    fun reached(s: GameState, at: Geo, acc: Double, part: Daypart, places: List<Place>): Boolean {
        val e = current(s) ?: return false
        val reach = REACH + minOf(acc, 25.0)
        return when (val t = e.task) {
            is Task.Visit -> (t.parts == null || part in t.parts) && places.any { it.habitat == t.habitat && it.at.dist(at) <= reach }
            is Task.Far -> s.home?.let { at.dist(it) >= t.m } ?: false
            Task.First -> s.catches.firstOrNull()?.let { at.dist(it.at) <= reach } ?: false
            else -> false
        }
    }

    /** The task may be let go after a while, so the story never gets stuck on a place that isn't there. */
    fun skippable(s: GameState, now: Long) = !done(s) && now - s.pageAt > SKIP_DAYS * Rules.DAY

    /** The state with the current task marked done (by arriving, or by letting it go), and its parts. */
    fun mark(s: GameState, reward: Boolean = true) =
        if (s.story - 1 in s.tasks) s else s.copy(tasks = s.tasks + (s.story - 1), parts = s.parts + if (reward) PARTS else 0)
}
