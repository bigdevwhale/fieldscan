# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [0.13.0] - 2026-11-13

### Changed
- The stairs creatures no longer need a barometer: "climb N metres" became "up the stairs: N steps without stopping", counted by the phone's step counter (#2). The barometer climb didn't work on phones without one, and its GPS fallback along the stairs on the map often never counted. A stop of more than 25 seconds still starts the count over.
- In the opening, «skip» moved to the top left, away from «next ›», which is now bigger and brighter (#1).

<!-- tg
Изменено: задания на лестницах больше не требуют барометра – теперь нужно подняться на N шагов без остановки, шаги считает шагомер.
В заставке «пропустить» переехало влево, а «дальше» стало крупнее, чтобы их не путать.
-->

## [0.12.1]

- Less text on the screens: the explanations that sat under every block now live in the «?» manual, and each screen keeps only what changes.
- Air: the top card shows the creature's whisper and a button, without instructions; the nearest trace isn't listed twice; rows are two lines; the radar's scale sits on the legend's line («⌀ 500 m · N↑»).
- Rite: how a step is checked folds under «how it counts»; the trap's charge says only how many steps are left.
- Device: memory lists the names once and one line «wearing: N of M»; the step source is one short line. Journal: a shorter heading, rewards without explanations.
- The «signal close by» popup: distance, side and the whisper, no paragraph.

<!-- tg
Экраны стали чище: пояснения переехали в справку «?», на экране осталось главное.
Ближайший след больше не дублируется в списке, строки следов короче.
В обряде объяснение шага раскрывается по нажатию «как засчитывается».
В Приборе память читается одной строкой: кто в памяти и сколько портится.
-->

## [0.12.0]

- The air opens with what to do now: the nearest awake trace (or "you're there") and a button for its rite. Below it, sections: traces nearby, the story, the round of the week. The title stays put while the list scrolls.
- Traces show an arrow pointing the way and the distance in large type; the clue fits one line; a status line appears only when a trace sleeps, is caught or you're there. Asleep and caught traces are paler, and rows that open something have a «›».
- A legend under the radar for its marks: trace, near, story, round. One short line for the scale and the compass mode.
- The buttons at the bottom have icons and a clearly lit current one. Scanner is now Air, Rumours is now Journal. A red mark shows where something waits: a task in the journal; in Device, parts for an attachment, full memory or frames going bad.
- The status line shows charge and memory as numbers (memory turns red when frames wear or it's full); the clock is gone, the phone already has one.
- The episode's task lives in the journal; the air only links to it. Story finds are dashed, since they're walked to, not tapped.
- Journal in sections: newest page, earlier pages, rumours, keepers. Bestiary: kinds in one sliding row, "caught 5 of 50", riddles in red, empty slots dashed. Rite: the way to the place with an arrow. Device: "frames are handed in at home · N m" instead of a greyed-out button; shorter texts.
- A big way back on the rite, card and riddle screens; light text on red buttons in the dark theme.

<!-- tg
Интерфейс стал понятнее: сверху эфира — что делать сейчас и кнопка обряда, ниже разделы.
У следов стрелка направления и крупное расстояние, спящие — бледнее.
Нижние кнопки с иконками и красными метками; «Сканер» теперь «Эфир», «Слухи» — «Журнал».
Архив, заряд и память читаются с первого взгляда.
-->

## [0.11.0]

- The trap eats steps: walking charges it, a catch spends the charge, and stronger creatures take more (500–700 steps for a dweller, 1500 for a wanderer, 3000 for a keeper, 6000 for the legend). The charge shows in the rite, on the air and in Device.
- Steps from the phone's step counter or from Health Connect (Samsung Health, Mi Fitness, Zepp Life, Google Fit, watches and bands), chosen in Device; without either, steps are reckoned from the GPS walk.
- A story: the journal of N. Vetrova, the device's last owner. 36 pages that open about once a month as you walk and catch, three years in all, and found pages for catching certain creatures. In Rumours.
- The radar turns with the compass: the top is where you face, the dots stay put on the street. Tap the dial for north up. Traces and the rite also say ahead, right, behind or left.
- Frames on real photos: when the rite is done, the trail camera's viewfinder opens over the phone's camera, grey-green like the IR sensor, with a pair of eyes in it; the shutter grows the creature into the real place. The photo stays on the phone; without a camera the place is drawn as before.
- The sound of the air, all synthesised: a hiss with crackles that thickens near an awake trace, the sweep's ping on each dot, a tune-in when a trace opens, signals for a step done or broken, the shutter, a call sign for a new journal page. A switch in Device.
- The compass works at any tilt, held flat or upright.
- A cold open before the first switch-on, like the start of an episode: night, static, typed lines, the last frame in the old observer's memory, the title card, the three rules. Again from Device → Prologue.
- The first round, held by the hand: the homeling at the door, the archive, then the nearest awake trace in the street; the trace meant is marked on the air. Ends by itself after two catches, or with its ×.
- The story out in the street: six journal pages leave a find on the air, a red diamond pinned near home (the nearest stairs, bridge, bench, a bare point at night, home itself). Reaching it opens a scene, a journal entry and 2 parts.
- Episodes: 31 journal pages come with one thing to do out in the city (a lamp after sunset, a stop at night, two kilometres from home, back to your first catch, thirty thousand steps, a story find), and the next page waits for it. The device notices by itself; +1 part; after three weeks a task can be let go.
- Radio intercepts on nine key pages: the air, a voice drowned in it, then a message in Morse (Russian or Latin letters), decoded on screen letter by letter as it plays. A page with one plays it as it arrives.
- The round of the week: every Monday three places of the district, 200 m to 1.5 km from home, each of a different kind, on the air as hollow squares. All three walked to give 2 parts and 1000 steps of charge.
- Journal pages come on their own: checked every hour with the app closed, a due page opens with a notification (by day only); with the app open, a page pops up.
- "A signal close by": within 120 m of an awake trace a popup with a buzz and a sound; every trace says whether it can be caught now; a long buzz when the trail camera is ready.
- The climb is now height gained without stopping, by the barometer (stairs, a hill, floors); without one, up the stairs on the map by GPS.
- Every rite step now says how the phone checks it; the stairs show a sketch with the bottom, the top and you; the companion and item conditions are explained.

<!-- tg
Ловушка теперь заряжается шагами: чем сильнее существо, тем больше надо пройти. Шаги можно брать из Health Connect.
Появился сюжет: журнал прежней хозяйки прибора, страницы открываются примерно раз в месяц, на три года.
Существа ловятся на настоящих фото: после обряда открывается видоискатель фотоловушки.
В сканере появился звук эфира: шум, треск, отклик радара и сигналы обряда.
Каждую неделю — обход из трёх мест района: дойди до всех, получи детали и заряд ловушки.
На ключевых страницах журнала появились радиоперехваты: шум, голос и морзянка, которая расшифровывается на экране.
Журнал стал сериалом: у страниц появились задания в городе, следующая страница ждёт их выполнения.
Сюжет вышел на улицу: страницы журнала оставляют на радаре находки у реальных мест рядом с домом.
Первая поимка теперь ведётся за руку: домовой у порога, архив, первый след на улице.
Новый пролог в духе сериала и уведомление, когда открывается страница журнала.
Радар поворачивается по компасу, а под каждым шагом обряда написано, как он работает.
-->

## [0.10.0]

- A dark theme: the screen's night mode, dark paper and light ink. System, light or dark, in Device.
- 21 more creatures, 50 in all: a new dweller for every kind of place (the Grey mouse at the all-night pharmacy, the Lamp-walker, the Candling, the Clockling, the Strider…), wanderers that come with rain, a strong wind and frost, and two homelings for dawn and midnight at your door.

<!-- tg
В «Сканере» появилась тёмная тема: экран прибора переходит в ночной режим. Выбрать её можно в «Приборе».
Существ стало 50: у каждого типа мест появился новый жилец, а ещё странники, которые приходят с дождём, сильным ветром и морозом.
-->

## [0.9.0]

The first version, a beta.

- The scanner: traces around you from OpenStreetMap places, by kind, distance, direction and strength; the dial zooms to them.
- 29 creatures in five tiers: dwellers, keepers of the district's most-of-all places, wanderers that come with fog, snow and the full moon, a legend on a whole-degree confluence, a homeling at home.
- Rites of one to four steps checked by GPS, compass, accelerometer, microphone pitch, torch and touch; steps done are kept for two hours.
- Trail-camera frames grown in code: bones, noise-torn outlines, fur, scenes per place, the hour and the weather in the frame.
- Rumours that open riddles, items and companions, memory that wears and the archive at home, parts and attachments.
- A page of the device's manual on every screen; back always leads to the air, which asks before exiting; language choice in Device.

<!-- tg
Новое приложение «Сканер» (бета): прибор, который ищет аномалии в твоём городе.
Сканер ловит только след, а кто там и как его вызвать, подскажут загадки. Встань посередине моста, спой ноту у фонтана, встреть рассвет на остановке.
Каждого пойманного снимает фотоловушка: ночной кадр со временем и погодой. В бета-версии 29 существ.
-->
