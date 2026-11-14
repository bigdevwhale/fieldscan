# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow теперь кладёт в GitHub Release ещё и `Fieldscan-v<версия>.aab`.
  В v0.14.0 его ещё нет: первая версия для Play — **следующий релиз** (v0.14.1 или новее). Секреты `SIGNING_*`
  в репозитории заданы; без них сборка подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в «Приборе» перестаёт работать. Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.fieldscan`.
  2. **Select key** → ключ с SHA-256 `d872393a73e9aaa6cdfba0c604a210d45de1e908213ce3eb68b72f16b9873fa5`
     (сертификат CN=bigdevwhale, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Так `app.fieldscan` остаётся за одним
  зарегистрированным ключом, а APK из GitHub и версия из Play обновляют друг друга.
  Если оставить «Google создаст ключ подписи», версии из Play и GitHub друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория (на двух языках, с разделами про
  местоположение, камеру, микрофон и Health Connect). После push ссылка:
  `https://github.com/bigdevwhale/fieldscan/blob/main/PRIVACY.md`
  (её же открывает Health Connect с экрана разрешений — `HealthPrivacy.kt`).
- **Автозагрузка из CI** (`.github/workflows/play.yml`) — после каждого успешного Release берёт AAB из
  релиза и отправляет в Play, release notes — из `listing/*/release_notes.txt` на теге. До первой ручной
  загрузки она не нужна, поэтому:
  1. В репозитории Settings → Environments → создать **`google-play`** с Required reviewer (вы) —
     иначе job запустится без подтверждения сразу после релиза.
  2. Play Console → Users and permissions → пригласить `play-publisher@intense-context-514611-h3.iam.gserviceaccount.com`
     с правом выпускать релизы для Fieldscan.
  3. В Google Cloud у пула `github` (Workload Identity Federation) разрешить репозиторию
     `bigdevwhale/fieldscan` действовать от имени этого сервисного аккаунта — так же, как для остальных
     приложений.
  Пока это не сделано, отклоняйте запуск «Google Play» после релиза (или он упадёт на авторизации — без вреда).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Fieldscan: Creatures Nearby` |
| Default language | English (United States) – en-US |
| App or game | **Game** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны `tools/play.py` из скриншотов README (`docs/screenshots/<язык>/*.jpg`,
1080×2341 — экраны демо, нарисованные Robolectric): Play не берёт картинки длиннее двух ширин, поэтому
экран уходит за нижний край под подписью. На скриншотах: снимок фотоловушки, эфир, обряд, бестиарий,
карточка существа, слухи, дневник и бестиарий в ночном режиме. Нужен `pip install pillow`. Как пересобрать:

```bash
./gradlew testDebugUnitTest -Prender --tests '*RenderScreens*'   # экраны демо без телефона, en и ru
python tools/shots.py   # docs/screenshots
python tools/play.py    # графика для Play
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Game → **Adventure** |
| Tags (до 5) | Adventure, Puzzle-adventure, Interactive story, Logic puzzle, Casual |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/fieldscan/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Игры, где ловят существ в реальных местах (Pokémon GO и похожие), живут в **Adventure** —
туда же Fieldscan: бестиарий, загадки, обряды и история. Шаги здесь — заряд ловушки, а не цель, поэтому
не Health & Fitness. Категорию можно сменить позже.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, одинаковое для всех приложений:
тогда Play сам покажет под карточкой «Другие приложения этого разработчика».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*
(Существа живут у настоящих мест из OpenStreetMap, так что ревьюеру нужно лишь разрешить местоположение;
аккаунтов и логинов нет.)

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **Game**.
- Насилие, кровь, сексуальный контент, ненормативная лексика, наркотики/алкоголь/табак, азартные игры — **No**.
  Ловушка «ловит» существо снимком, сражений нет; заряд — шаги, не деньги.
- **Fear** — существа показаны зернистыми ночными ИК-кадрами, немного жутковато, но без крови, нападений
  и скримеров: **Yes** → **Scary elements** (Horrifying — нет) → частота **Often** (кадр после каждой поимки,
  весь бестиарий из них). Ожидаемый результат — около PEGI 7.
- **Users interact / share content** — **No**: общения между пользователями нет; ссылки на чат в Telegram
  открываются во внешнем приложении.
- **Shares user's location with other users** — **No**.
- **Digital purchases** — **No**. **Web browser / unrestricted internet** — **No**.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over**. Младше 13 не отмечать: иначе действует политика Families,
  а игра зовёт ходить одному по городу, в том числе вечером, с GPS.
- *Could your store listing unintentionally appeal to children?* → **No**.

**News apps** — No.

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**

Нам ничего не приходит, но Play считает «сбором» любую передачу с телефона, в том числе в чужие
сервисы. Честный минимум:

- *Does your app collect or share any of the required user data types?* → **Yes**.
- *Is all of the user data collected by your app encrypted in transit?* → **Yes** (всё по HTTPS).
- *Do you provide a way for users to request that their data is deleted?* → **No** (аккаунтов нет,
  на серверах ничего не хранится; данные удаляются вместе с приложением).
- Типы данных:
  - **Location → Approximate location** — границы квадратов карты (~2 км) уходят в Overpass (OpenStreetMap),
    координаты, округлённые до ~1 км, — в MET Norway за погодой. Collected: **Yes**. Shared: **No**
    (передача сервису, который выполняет функцию приложения, — не «sharing»). **Processed ephemerally: Yes**.
    Required: **Yes** (без карты нет следов). Purpose: **App functionality**.
- Больше ничего не отмечать: точные координаты, шаги («Fitness info»), снимки камеры, голос, дом и бестиарий
  с телефона не уходят. Аналитики, рекламы, идентификаторов устройства нет. Системное резервное копирование
  Android делает система, а не приложение, его не декларируем.
- Итог в карточке: «Collects: Location», «No data shared with third parties», «Data is encrypted in transit».

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — отметьте **Activity and fitness** (заряд ловушки шагами) и **Health Connect**; медицинские
функции и прочее — не отмечать.

**Health Connect permissions** (App content → Health Connect, появится после загрузки AAB с `health.READ_STEPS`)
- `READ_STEPS` — *Activity and fitness*. Обоснование:
  > Fieldscan is a city exploration game in which walking charges the trap used to catch creatures. If the
  > user chooses Health Connect in the Device screen instead of the phone's own step counter, the app reads
  > only the daily step totals (Samsung Health, Mi Fitness, a watch or band put them there), so the steps
  > walked count towards the trap's charge. Steps stay on the device; nothing is written to Health Connect
  > or shared.
- Фоновое чтение (`READ_HEALTH_DATA_IN_BACKGROUND`) не запрашивается и не декларируется.
- Ссылка на политику: PRIVACY.md (раздел о Health Connect и Limited Use в нём есть); в приложении она
  открывается с экрана разрешений Health Connect.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Location permissions** — отдельной декларации не нужно: `ACCESS_BACKGROUND_LOCATION` не запрашивается,
местоположение берётся только при открытом приложении. Foreground-сервисов нет.

**Прочие разрешения** — особых деклараций не нужно:
- `CAMERA` — вид фотоловушки на настоящее место при поимке; кадр не сохраняется и не уходит с телефона.
- `RECORD_AUDIO` — только шаг «спой ноту»: измеряется высота голоса, ничего не записывается; разрешение
  спрашивается на этом шаге, есть тихий вариант без микрофона.
- `ACTIVITY_RECOGNITION` (шагомер), `POST_NOTIFICATIONS` (новая страница дневника, только днём),
  `VIBRATE`, `INTERNET` не относятся к чувствительным. Точных будильников нет — ежечасная проверка идёт
  через WorkManager.

## 5. Pricing & distribution

- Countries / regions: все доступные. (Места берутся из OpenStreetMap, так что игра работает везде, где
  карта подробна; в бета-версии лучше всего — в крупных городах.)
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Fieldscan-v<версия>.aab` из GitHub Release (первый релиз с AAB).
3. Release name: `<версия>`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели;
   с Health Connect — дольше: декларацию Health Connect проверяют отдельно.
5. После установки из Play проверьте: разрешение на местоположение → эфир показывает следы вокруг;
   обряд с шагом «спой ноту» спрашивает микрофон; Прибор → шаги из Health Connect открывают его экран
   разрешений; Прибор → Язык → Русский/English — экран переключается сразу.

## Следующие версии

- `release_notes.txt` переписать под новую версию (≤500 символов — `play.yml` это проверяет;
  для первой там «First release on Google Play» и новинка 0.14.0). Дальше релиз уходит в Play сам через
  `play.yml` после подтверждения в окружении `google-play`.
- Скриншоты пересобирать, только если поменялись показанные экраны.

## Риск: Overpass и MET Norway

Оба бесплатны и без ключей; запросы идут с собственным User-Agent (`Osm.AGENT`, `Met.AGENT`). Публичные
серверы Overpass ограничивают частоту и бывают перегружены — приложение перебирает четыре зеркала и
держит ответ в кеше 30 дней. MET Norway требует User-Agent с контактом и не чаще одного запроса в несколько
минут на точку — у одного пользователя это не упрётся. Если игра разойдётся, Overpass может начать
отказывать: тогда понадобится свой сервер или выгрузка районов.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.fieldscan`.
