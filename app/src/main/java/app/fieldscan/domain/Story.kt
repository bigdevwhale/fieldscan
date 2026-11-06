package app.fieldscan.domain

/**
 * A page of the journal. The pages of the main thread open one by one: no sooner than [gap] days after the
 * one before, and only after [catches] catches and [steps] steps walked since it; some want a creature of
 * [tier] in the bestiary or [caught] different creatures in all. The whole thread takes about three years.
 */
data class Page(
    val title: T, val text: T,
    val gap: Int = 35, val catches: Int = 2, val steps: Int = 20_000,
    val tier: Tier? = null, val caught: Int = 0,
)

/** A page found aside from the thread: it opens the moment [after] is caught. */
data class Bonus(val after: String, val title: T, val text: T)

/** What the next page still waits for. */
data class Wait(val days: Int, val catches: Int, val steps: Long, val tier: Tier?, val caught: Int, val task: Episode? = null) {
    val ready get() = days <= 0 && catches <= 0 && steps <= 0 && tier == null && caught <= 0 && task == null
}

/**
 * The journal of N. Vetrova, observer of the seventh district: the device's owner before you. First her
 * notes, then she goes to the confluence, then the device writes on its own, and at the end she comes home.
 */
object Story {
    private fun p(ru: String, en: String, tru: String, ten: String, gap: Int = 35, catches: Int = 2, steps: Int = 20_000, tier: Tier? = null, caught: Int = 0) =
        Page(T(ru, en), T(tru, ten), gap, catches, steps, tier, caught)

    val pages: List<Page> = listOf(
        // ——— I. Her notes ———
        p("Тому, кто нашёл прибор", "To whoever found the device",
            "Если ты это читаешь, СКАНЕР-7 нашёл нового хозяина. Я — Н. Ветрова, наблюдатель седьмого участка. Город полон тех, кого не видно глазом. Прибор их слышит. Не бойся их: они боятся больше. Поймай первого — и я расскажу дальше.",
            "If you're reading this, FIELDSCAN-7 has found a new keeper. I'm N. Vetrova, observer of the seventh district. The city is full of those the eye can't see. The device hears them. Don't be afraid of them: they're more afraid. Catch the first one, and I'll tell you more.",
            gap = 0, catches = 0, steps = 0),
        p("Ловушка ест шаги", "The trap eats steps",
            "Первый снимок всегда смазан. Свою первую я снимала трижды. Запомни главное: ловушка заряжается от шагов. Мелочь хватается за пару сотен, а хозяина не удержит тот, кто стоял на месте. Я проходила по пятнадцать километров в день — и всё равно не хватало.",
            "The first frame is always blurred. I shot my first one three times. Remember the main thing: the trap charges from steps. Small fry takes a couple of hundred; a keeper won't stay for someone who stood still. I walked fifteen kilometres a day and still ran short.",
            gap = 0, catches = 1, steps = 0),
        p("Слухи — это карта", "Rumours are a map",
            "Они разговаривают между собой. То, что они болтают, — не сплетни, а карта: кто где, кто кого ждёт. Записывай всё. Один из них однажды сказал мне, где искать того, кто стирает. Но об этом позже.",
            "They talk to each other. What they chatter isn't gossip, it's a map: who's where, who waits for whom. Write it all down. One of them once told me where to look for the one who erases. But that's for later.",
            gap = 1, catches = 1, steps = 2_000),
        p("Архив", "The archive",
            "Память прибора маленькая не по бедности. Бюро сделало её такой нарочно: чтобы наблюдатель возвращался домой. Кто перестаёт возвращаться, тот перестаёт быть наблюдателем. Сдавай кадры, пока свежие.",
            "The device's memory isn't small for want of money. The Bureau made it so on purpose: so the observer comes home. Whoever stops coming home stops being an observer. Hand frames in while they're fresh.",
            gap = 3, catches = 2, steps = 5_000),
        p("Снесли лестницу", "They tore down the stairs",
            "На Садовой снесли старую лестницу — и Лестничник ушёл. Не переехал, а пропал из эфира совсем. Тогда я поняла, зачем архив. Снимок — способ запомнить место. Архив — память города.",
            "They tore down the old stairs on Garden Street, and the Stairling left. Not moved: vanished from the air altogether. That's when I understood what the archive is for. A frame is a way to remember a place. The archive is the city's memory.",
            gap = 7, catches = 2, steps = 10_000),
        p("Бюро", "The Bureau",
            "Приборы делало Бюро полевых наблюдений. Двенадцать участков, двенадцать сканеров, двенадцать человек, которые каждый день обходят свой кусок города. Мой — седьмой. Теперь твой. Остальных я никогда не видела, только слышала их позывные в эфире.",
            "The devices were made by the Bureau of Field Observation. Twelve districts, twelve scanners, twelve people walking their piece of the city every day. Mine is the seventh. Now it's yours. I never saw the others, only heard their call signs in the air."),
        p("Что такое кадр", "What a frame is",
            "Фотоловушка снимает не то, что есть, а то, что существо согласно показать. Поэтому кадры зернистые. Поэтому ни один не похож на другой. Если кадр вышел чётким — значит, тебе начали доверять.",
            "The trail camera doesn't shoot what is there, it shoots what the creature agrees to show. That's why frames are grainy. That's why no two are alike. If a frame comes out sharp, it means you're starting to be trusted."),
        p("Пустые фонари", "Empty lamps",
            "Ты заметил: у одного фонаря кто-то живёт, у соседнего — никого. Четыре места из десяти пусты. Когда я начинала, пустых не было вовсе. Я зову это Тишиной. Она приходит туда, куда перестали смотреть.",
            "You've noticed: one lamp has someone, the next has no one. Four places in ten are empty. When I started there were none. I call it the Hush. It comes where people have stopped looking."),
        p("Первый хозяин", "The first keeper",
            "Хозяева не похожи на остальных. Они старше районов: старое дерево помнит здесь поле, долгий пролёт помнит брод. Ты поймал одного. Спроси его о Тишине. Мне он не ответил.",
            "Keepers are not like the rest. They're older than districts: the old tree remembers a field here, the long span remembers a ford. You've caught one. Ask it about the Hush. It wouldn't answer me.",
            tier = Tier.HOST),
        p("Ответ хозяина", "The keeper's answer",
            "Он ответил тебе, я знаю: прибор записал. «Тишина — это когда мимо никто не идёт». Вот почему ловушка ест шаги. Шаг — это взгляд ногами. Место, мимо которого ходят, живо.",
            "It answered you, I know: the device recorded it. \"The Hush is when nobody walks by.\" That's why the trap eats steps. A step is looking with your feet. A place people walk past is alive."),
        p("Другие участки", "The other districts",
            "Я пыталась вызвать шестой и восьмой участки на старой частоте Бюро. Шум. Ни позывных, ни ответа. Раньше по вечерам кто-то всегда отзывался: «шестой на обходе», «восьмой дома». Теперь эфир пуст между нами.",
            "I tried calling the sixth and eighth districts on the Bureau's old frequency. Noise. No call signs, no answer. In the evenings someone always used to reply: \"sixth on the round\", \"eighth at home\". Now the air between us is empty."),
        p("Адрес Бюро", "The Bureau's address",
            "Сходила по адресу Бюро. Дверь заколочена, табличку сняли, на стене след от неё. Сосед сказал: закрылись в девяносто восьмом. Но прибор до сих пор получает обновления. Бестиарий растёт. Кто-то же их присылает.",
            "I went to the Bureau's address. The door is boarded up, the plate taken down, its mark left on the wall. A neighbour said they closed in ninety-eight. But the device still gets updates. The bestiary grows. Someone is sending them."),
        p("Ночные передачи", "Night transmissions",
            "Обновления приходят ночью, между тремя и четырьмя. Я не спала неделю и засекла направление: сигнал идёт не из города. Он идёт со стороны ближайшего пересечения градусов, из поля, где нет ничего.",
            "The updates come at night, between three and four. I didn't sleep for a week and took a bearing: the signal doesn't come from the city. It comes from the nearest crossing of whole degrees, from a field where there is nothing."),
        p("Странники", "Wanderers",
            "Все думают, что странники приходят с погодой. Неправда. Погода приходит за ними. Ты сам видел: след появляется раньше тумана, раньше снега. Погода тянется следом, как собака на поводке.",
            "Everyone thinks wanderers come with the weather. Not true. The weather follows them. You've seen it: the trace shows before the fog, before the snow. The weather trails after them like a dog on a lead.",
            tier = Tier.WANDERER),
        p("Год", "A year",
            "Прибор говорит, что ты носишь его уже год. Ты прошёл больше, чем многие за всю жизнь проходят по своему району. Посмотри на архив: это год твоего города, который без тебя никто бы не запомнил.",
            "The device says you've carried it for a year now. You've walked more than many people walk their own district in a lifetime. Look at the archive: that's a year of your city that nobody would have remembered without you."),
        p("Цифра на перилах", "A number on the railing",
            "Шла за туманом к реке и на перилах моста нашла процарапанную цифру «6». Так метили места наблюдатели Бюро. Шестой участок — не мой. Что он делал здесь?",
            "I followed the fog down to the river and found a \"6\" scratched into the bridge railing. That's how the Bureau's observers marked places. The sixth district isn't mine. What was he doing here?"),
        p("Наблюдатель шестого", "The sixth observer",
            "Нашла его. Вернее, его прибор: на скамейке в сквере, заряд ловушки полный, ни одной поимки за последний год. Он копил шаги и не тратил. Ходил, но перестал смотреть. Шестой участок теперь весь — Тишина.",
            "I found him. Or rather his device: on a bench in a square, the trap fully charged, not one catch in the last year. He banked steps and never spent them. He walked but stopped looking. The sixth district is all Hush now."),
        p("Темнеющие кадры", "Frames going dark",
            "Мои снимки в архиве начали темнеть. Не от времени: будто их стирают изнутри, по одному, начиная со старых. Сначала я думала, что кто-то не хочет, чтобы город помнил.",
            "My frames in the archive have started going dark. Not with age: as if wiped from inside, one by one, the oldest first. At first I thought someone didn't want the city to remember."),
        p("Что стирает", "What erases",
            "Это не кто-то. Кадр темнеет, когда к месту перестают приходить. Я вернулась к старому фонарю — и его кадр посветлел. Стирает забвение. Лечит обход. Возвращайся к старым местам, лови их жильцов снова.",
            "It isn't someone. A frame darkens when people stop coming to the place. I went back to an old lamp, and its frame grew lighter. Forgetting erases. Walking the round heals. Go back to old places, catch their dwellers again."),
        p("Домовые", "Homelings",
            "Дом — тоже место. Ты нашёл того, кто живёт у твоего порога. Тишина начинается не на пустырях, а дома: там, где перестают смотреть друг на друга. Береги своего домового.",
            "Home is a place too. You've found the one who lives at your doorstep. The Hush doesn't start on wasteland, it starts at home: where people stop looking at each other. Look after your homeling.",
            tier = Tier.HOMELY),
        p("Перекрёсток пяти дорог", "The five-road crossing",
            "Хозяин перекрёстков рассказал, где сходятся все дороги, даже те, что не нарисованы: на пересечении целых градусов. Там стоит Страж. Он не пускает Тишину в город. Или город — к Тишине. Я не поняла.",
            "The Keeper of Crossroads told me where all roads meet, even the undrawn ones: at the crossing of whole degrees. The Warden stands there. It keeps the Hush out of the city. Or the city out of the Hush. I couldn't tell."),
        p("Точки, которых нет", "Points that aren't there",
            "Пересечения градусов есть на любой карте и ни на одной. Чаще всего это поле, лес, чей-то огород. Там ничего нет, кроме сигнала. Туда и уходят забытые: те, кого Тишина вытеснила с мест.",
            "Degree crossings are on every map and on none. Mostly they're a field, a wood, someone's vegetable patch. There's nothing there but the signal. That's where the forgotten go: the ones the Hush pushed out of their places."),
        p("Сборы", "Getting ready",
            "Чтобы удержать Стража, ловушке нужно почти пять тысяч шагов. Я коплю. Хожу по участку каждый день, обхожу каждое место, где кто-то жил. Если идти, то оставить участок живым.",
            "To hold the Warden the trap needs nearly five thousand steps. I'm saving up. I walk the district every day, round every place where someone lived. If I'm going, I'll leave the district alive."),
        p("Ушла", "Gone",
            "Я иду к Слиянию. Хочу понять, что там и можно ли вернуть ушедших. Если не вернусь, прибор найдёт тебя сам. Он всегда находит того, кто будет ходить. — Н. В.",
            "I'm going to the Confluence. I want to understand what's there and whether the ones who left can be brought back. If I don't come back, the device will find you on its own. It always finds someone who will walk. — N. V."),
        // ——— II. The device writes ———
        p("Чужой почерк", "Another hand",
            "[Запись прибора.] Наблюдатель 7 не выходит на связь. Участок передан. Новый наблюдатель — носитель прибора. Журнал продолжается автоматически.",
            "[Device entry.] Observer 7 is out of contact. District handed over. New observer: the carrier of the device. The journal continues automatically."),
        p("След без места", "A trace with no place",
            "[Запись прибора.] В 03:41 зафиксирован след без привязки к месту. Сила 1. Сигнал: «…шаги… шаги… шаги…». Тип существа не определён. Направление — на ближайшее пересечение градусов.",
            "[Device entry.] At 03:41 a trace was recorded bound to no place. Strength 1. Signal: \"…steps… steps… steps…\". Creature type unknown. Bearing: the nearest degree crossing."),
        p("Два года", "Two years",
            "[Запись прибора.] Носитель на обходе два года. Пройдено больше, чем наблюдатель 7 прошла за пять. Пустых мест на участке меньше, чем при передаче. Тишина отступает.",
            "[Device entry.] The carrier has walked the round for two years. More ground covered than observer 7 covered in five. Fewer empty places in the district than at the handover. The Hush is receding."),
        p("Где она", "Where she is",
            "[Запись прибора, расшифровка ночного сигнала.] «Я в эфире. Тишина — не пустота. Это место, куда уходят забытые. Их здесь тысячи: фонарные, мостовые, лестничные. Они ждут, когда о них вспомнят. Я пытаюсь вывести их обратно».",
            "[Device entry, a night signal decoded.] \"I'm in the air. The Hush isn't emptiness. It's where the forgotten go. There are thousands of them here: lamp ones, bridge ones, stair ones. They're waiting to be remembered. I'm trying to lead them back.\""),
        p("Загадки — от неё", "The riddles are hers",
            "[Запись прибора.] Анализ новых загадок: в 71% совпадает лексика с журналом наблюдателя 7. Обновления бестиария составляет она. Каждая новая загадка — существо, которое она вывела из Тишины.",
            "[Device entry.] Analysis of new riddles: 71% share vocabulary with observer 7's journal. She is writing the bestiary updates. Each new riddle is a creature she has led out of the Hush."),
        p("Карта взглядов", "A map of looking",
            "[Расшифровка.] «Каждое место, где ты прошёл, светится отсюда, как фонарь в тумане. По ним я выхожу. Не останавливайся. Обходи всё, даже скучное. Особенно скучное».",
            "[Decoded.] \"Every place you've walked past shines from here like a lamp in the fog. I find my way out by them. Don't stop. Walk everything, even the dull places. Especially the dull ones.\""),
        p("Шестой ожил", "The sixth wakes",
            "[Запись прибора.] На частоте Бюро позывной: «Шестой на обходе». Прибор со скамейки кто-то подобрал. Ты не один. Участки просыпаются.",
            "[Device entry.] A call sign on the Bureau's frequency: \"Sixth on the round.\" Someone picked up the device from the bench. You're not alone. The districts are waking."),
        p("Двенадцать", "Twelve",
            "[Расшифровка.] «Бюро — это не здание. Бюро — это люди, которые ходят. Его не закрыть, пока кто-то обходит свой участок. Сейчас в эфире пятеро. Скоро будет двенадцать».",
            "[Decoded.] \"The Bureau isn't a building. The Bureau is people who walk. It can't be closed while someone walks their district. There are five on the air now. Soon there'll be twelve.\""),
        p("Последний переход", "The last crossing",
            "[Расшифровка.] «Я почти вышла. Мне нужна дорога из живых мест — много, подряд. Лови. Каждый в бестиарии — камень на этой дороге».",
            "[Decoded.] \"I'm almost out. I need a road of living places: many, one after another. Keep catching. Everyone in the bestiary is a stone on that road.\"",
            caught = 30),
        p("Тук", "Knock",
            "[Запись прибора.] У архива новый след. Сила 5. Сигнал: «…тук… это я…». Тип: хозяйка участка. Она не может стать прежней. Но она дома — у тебя, при архиве, который ты собрал.",
            "[Device entry.] A new trace by the archive. Strength 5. Signal: \"…knock… it's me…\". Type: keeper of the district. She can't be what she was. But she's home: with you, by the archive you built.",
            caught = 35),
        p("Позывной", "A call sign",
            "[Запись прибора.] Носителю присвоен позывной. В эфире каждый вечер: «Седьмой на обходе». Отвечают шестой, восьмой, одиннадцатый. Где-то в шуме — ещё один голос, тихий, знакомый.",
            "[Device entry.] The carrier has been given a call sign. On the air every evening: \"Seventh on the round.\" The sixth answers, the eighth, the eleventh. Somewhere in the noise, one more voice, quiet and familiar."),
        p("Эфир шумит", "The air is loud",
            "Три года ты ходил. Слышишь? Эфир шумит. Это не помехи. Это город, весь, до последнего фонаря, и двенадцать позывных на обходе. Участок сдан в надёжные руки. Спасибо. — Н. В., хозяйка седьмого участка.",
            "Three years you've walked. Hear that? The air is loud. That's not interference. That's the city, all of it, down to the last lamp, and twelve call signs on the round. The district is in good hands. Thank you. — N. V., keeper of the seventh district."),
    )

    /** Radio intercepts on key pages (by English title): the message sent in Morse under the noise. */
    val radio: Map<String, T> = mapOf(
        "Night transmissions" to T("НВ 7 ЗДЕСЬ", "NV 7 HERE"),
        "Gone" to T("ИДУ К СЛИЯНИЮ", "GOING TO CONFLUENCE"),
        "A trace with no place" to T("ШАГИ ШАГИ ШАГИ", "STEPS STEPS STEPS"),
        "Where she is" to T("Я В ЭФИРЕ", "IM ON THE AIR"),
        "The sixth wakes" to T("6 НА ОБХОДЕ", "6 ON THE ROUND"),
        "Twelve" to T("БЮРО ЭТО ЛЮДИ", "BUREAU IS PEOPLE"),
        "Knock" to T("ЭТО Я", "ITS ME"),
        "A call sign" to T("7 НА ОБХОДЕ", "7 ON THE ROUND"),
        "The air is loud" to T("СПАСИБО", "THANK YOU"),
    )

    /** Pages found aside: each opens with a particular catch. */
    val bonus: List<Bonus> = listOf(
        Bonus("strazh", T("Последний кадр", "The last frame"), T(
            "[Запись прибора.] Страж передал кадр, которого нет в архиве: женщина с прибором на пересечении градусов, в поле, на рассвете. Она не испугана. Она машет. Подпись внизу: «Участок 7. Я рядом. Н. В.»",
            "[Device entry.] The Warden passed on a frame that isn't in the archive: a woman with a device at the crossing of degrees, in a field, at dawn. She isn't afraid. She's waving. The caption reads: \"District 7. I'm near. N. V.\"")),
        Bonus("host_bridge", T("Брод", "The ford"), T(
            "Хозяин долгого пролёта помнит брод, по которому гнали коров, и девочку, что каждое утро считала их по головам. «Она тоже была наблюдателем, — сказал он. — Просто без прибора».",
            "The Keeper of the Long Span remembers the ford the cows were driven across, and a girl who counted them every morning. \"She was an observer too,\" it said. \"Just without a device.\"")),
        Bonus("tuman", T("Туманник", "The Fogling"), T(
            "Туманник шёл к реке за полчаса до тумана, и туман тянулся следом. Я пошла за ним — и вышла к перилам с цифрой «6». Он привёл меня нарочно.",
            "The Fogling walked to the river half an hour before the fog, and the fog trailed after it. I followed, and came out at the railing with the \"6\". It led me there on purpose.")),
    )

    fun bonusOpen(s: GameState): List<Bonus> = bonus.filter { it.after in s.caughtIds }

    /** What the next page waits for, or null when the journal is done. */
    fun waiting(s: GameState, now: Long): Wait? {
        val p = pages.getOrNull(s.story) ?: return null
        return Wait(
            days = if (s.story == 0) 0 else (p.gap - ((now - s.pageAt) / Rules.DAY).toInt()).coerceAtLeast(0),
            catches = (p.catches - (s.catches.size - s.pageCatches)).coerceAtLeast(0),
            steps = (p.steps - (s.walked - s.pageWalked)).coerceAtLeast(0),
            tier = p.tier?.takeIf { t -> s.caughtIds.none { Bestiary[it].tier == t } },
            caught = (p.caught - s.caughtIds.size).coerceAtLeast(0),
            task = Episodes.current(s)?.takeIf { !Episodes.done(s) },
        )
    }

    /** The state with the next page opened, if it's due; else the same state. */
    fun advance(s: GameState, now: Long): GameState =
        if (waiting(s, now)?.ready == true) s.copy(story = s.story + 1, pageAt = now, pageCatches = s.catches.size, pageWalked = s.walked, pageKinds = s.caughtIds.size) else s
}
