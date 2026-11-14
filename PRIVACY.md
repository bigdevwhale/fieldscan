# Fieldscan — Privacy Policy

*Last updated: 14 November 2026*

Fieldscan is an Android game: an anomaly scanner for your own city. Creatures wait at real places around
you (bridges, springs, stairs, crossroads), and you call them out with small rites checked by the phone's
sensors. It is published as open source at <https://github.com/bigdevwhale/fieldscan>.

[Русская версия ниже](#политика-конфиденциальности)

## What we collect

**Nothing.** Fieldscan has no accounts, no analytics, no advertising and no crash reporting.
We run no server, so nothing you do in the app ever reaches us.

## What stays on your device

The app keeps the following only in its private storage on your phone:

- your home point (set by you, to hand frames in to the archive) and the places of your district found on the map;
- your bestiary: the creatures caught, their frames, the time, weather and place of each catch, rumours and riddles;
- your steps: the trap's charge and the readings of the phone's step counter (or the daily totals from Health Connect);
- the journal pages opened and your settings (language, theme, step source, notifications).

Your coordinates are used on the phone to find the traces around you and to check rite steps
(«stand in the middle of the bridge», «walk round three times»). They are not stored as a track and
are not sent anywhere except as described in «The map and the weather».

## The map and the weather

To learn which places are around you, the app downloads map data from OpenStreetMap through public
Overpass servers (overpass-api.de, overpass.kumi.systems, overpass.private.coffee, maps.mail.ru). Only the
bounds of squares of a fixed map grid (about 2 km each) around you are sent; the answer is cached on the phone for 30 days.
For the weather (fog, rain, wind, frost) the app asks MET Norway (api.met.no), sending coordinates rounded
to about 1 km. Both are requested over HTTPS with no identifiers of you or your device. These services see
the request like any website sees a visit (including your IP address); see their own privacy policies.

## Camera

When a rite is done, the trail camera can show the real place behind the frame through the phone's camera.
The picture is processed on the phone only; it is not uploaded anywhere.

## Microphone

Only the «sing a note» step uses the microphone, and only while that step is open: the app measures the
pitch of your voice. Nothing is recorded, kept or sent. You can choose the quiet way in Device instead.

## Steps and Health Connect

The trap charges with your steps. By default they are read from the phone's built-in step counter sensor.
Only if you choose Health Connect in Device and allow it there, the app instead reads your step count
(the number of steps per day, nothing else) from Health Connect, where your health and fitness apps
(Samsung Health, Mi Fitness, Google Fit, watches and bands) keep it. The app only reads steps: it writes
nothing to Health Connect, reads no other health data, and never sends your steps anywhere. You can
withdraw the permission at any time in Health Connect.

The use of information received from Health Connect adheres to the
[Health Connect Permissions policy](https://support.google.com/googleplay/android-developer/answer/12991134),
including the Limited Use requirements.

## Links

The «Telegram» items in Device open links to the StillCraft chat and the feedback bot in your browser or the
Telegram app. Nothing is sent to them unless you write there yourself.

## Permissions

- **Location (precise and approximate)** — to show the traces around you and to check where you stand in a rite.
  Only while the app is open; no background location.
- **Physical activity** — to read the phone's step counter, which charges the trap.
- **Health Connect: steps** (only if chosen in Device) — to read the steps per day.
- **Camera** — the trail camera's view of the real place at a catch.
- **Microphone** — only for the «sing a note» step; the pitch is measured, nothing is recorded.
- **Notifications** — «a new journal page», by day only.
- **Vibration**, **Internet** — the scanner's buzz; the map and the weather.

## Children

Fieldscan is not directed at children under 13.

## Changes

Changes to this policy are published on this page with a new date.

## Contact

Questions — through an issue: <https://github.com/bigdevwhale/fieldscan/issues>.

---

# Политика конфиденциальности

*Обновлено: 14 ноября 2026*

«Сканер» (Fieldscan) — игра для Android: сканер аномалий для вашего города. Существа ждут у настоящих мест
вокруг (мосты, родники, лестницы, перекрёстки), и вы вызываете их маленькими обрядами, которые проверяют
датчики телефона. Исходный код открыт: <https://github.com/bigdevwhale/fieldscan>.

## Что мы собираем

**Ничего.** В приложении нет аккаунтов, аналитики, рекламы и отчётов о сбоях. Своего сервера у нас нет,
поэтому ничего из того, что вы делаете в приложении, до нас не доходит.

## Что хранится на устройстве

Только в личном хранилище приложения на вашем телефоне:

- точка дома (её задаёте вы, чтобы сдавать снимки в архив) и места вашего района, найденные на карте;
- бестиарий: пойманные существа, их снимки, время, погода и место поимки, слухи и загадки;
- шаги: заряд ловушки и показания шагомера телефона (или итоги за день из Health Connect);
- открытые страницы дневника и настройки (язык, тема, источник шагов, уведомления).

Координаты используются на телефоне, чтобы найти следы вокруг и проверить шаги обряда («встань на середине
моста», «обойди три раза»). Маршрут не записывается, а координаты никуда не уходят, кроме описанного в
разделе «Карта и погода».

## Карта и погода

Чтобы узнать, какие места вокруг, приложение загружает данные карты OpenStreetMap через открытые серверы
Overpass (overpass-api.de, overpass.kumi.systems, overpass.private.coffee, maps.mail.ru). Отправляются только
границы квадратов постоянной сетки карты вокруг вас (около 2 км каждый); ответ хранится на телефоне 30 дней. Для погоды (туман,
дождь, ветер, мороз) приложение обращается к MET Norway (api.met.no) с координатами, округлёнными примерно
до 1 км. Оба запроса идут по HTTPS без идентификаторов вас или устройства. Эти сервисы видят запрос, как
любой сайт видит посещение (включая IP-адрес); см. их собственные политики.

## Камера

Когда обряд пройден, фотоловушка может показать настоящее место за снимком через камеру телефона.
Изображение обрабатывается только на телефоне и никуда не загружается.

## Микрофон

Микрофон нужен только для шага «спой ноту» и только пока этот шаг открыт: приложение измеряет высоту голоса.
Ничего не записывается, не хранится и не отправляется. Вместо этого можно включить тихий вариант в Приборе.

## Шаги и Health Connect

Ловушка заряжается шагами. По умолчанию они берутся со встроенного шагомера телефона. Только если вы
выберете Health Connect в Приборе и разрешите это там, приложение вместо этого читает число шагов (шаги
за день, больше ничего) из Health Connect, куда их записывают ваши приложения здоровья и фитнеса (Samsung
Health, Mi Fitness, Google Fit, часы и браслеты). Приложение только читает шаги: ничего не пишет в Health
Connect, не читает другие данные о здоровье и никуда не отправляет ваши шаги. Разрешение можно отозвать
в любой момент в Health Connect.

Использование информации, полученной из Health Connect, соответствует
[политике разрешений Health Connect](https://support.google.com/googleplay/android-developer/answer/12991134),
включая требования Limited Use.

## Ссылки

Пункты «Telegram» в Приборе открывают ссылки на чат StillCraft и бота обратной связи в браузере или
приложении Telegram. Им ничего не передаётся, пока вы сами там не напишете.

## Разрешения

- **Местоположение (точное и приблизительное)** — чтобы показать следы вокруг и проверить, где вы стоите
  в обряде. Только при открытом приложении, без фонового доступа.
- **Физическая активность** — чтобы читать шагомер телефона, который заряжает ловушку.
- **Health Connect: шаги** (только если выбрано в Приборе) — чтобы читать шаги за день.
- **Камера** — вид фотоловушки на настоящее место при поимке.
- **Микрофон** — только для шага «спой ноту»; измеряется высота звука, ничего не записывается.
- **Уведомления** — «новая страница дневника», только днём.
- **Вибрация**, **интернет** — отклик сканера; карта и погода.

## Дети

Приложение не предназначено для детей младше 13 лет.

## Изменения

Изменения этой политики публикуются на этой странице с новой датой.

## Контакты

Вопросы — через issue: <https://github.com/bigdevwhale/fieldscan/issues>.
