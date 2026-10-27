# Sber SDX-50U4139 (Салют ТВ / StarOS, Android 13)

Телевизор 50", плата CVTE `TP.T962D4.PC756` (Amlogic T962D4, платформа `t5w`, codename `at30a6`), 2 ГБ памяти,
8 ГБ хранилища (раздел `/data` — 3 ГБ), прошивка от 17 июня 2026 года. Та же платформа, что у
[`sber-salute-tv-at30a6`](../sber-salute-tv-at30a6/README.md); этот список сделан из него.

Все команды ниже выполняются из корня репозитория `cleanyourtv` на Mac. Один раз нужно окружение Python
для скриптов: `python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt`. Вместо `<tv ip>` подставьте адрес
телевизора в домашней сети (Настройки → Сеть → ваша Wi‑Fi сеть → IP‑адрес; адрес `26.26.26.x` — это VPN Happ,
он не подходит).

## Как снова подключиться к телевизору

ADB после работы закрыт. Чтобы что‑то поменять:

1. На ТВ: **Настройки → Для разработчиков → Беспроводная отладка** — включить.
2. Там же **«Подключить устройство с помощью кода»**, и сразу на Mac:
   ```bash
   adb pair <tv ip>:<порт из окна> <код из окна>
   adb connect <tv ip>:<порт с экрана «Беспроводная отладка»>
   adb tcpip 5555
   ```
3. Дальше работают все команды ниже (`TV_HOST=<tv ip>`). На ТВ может спросить «Разрешить отладку?» — «Всегда».

В конце снова закройте: выключите «Беспроводную отладку» и «Отладку по USB», перезагрузите ТВ.

## Что отключено

Отключено (`pm disable-user`), ничего не удалено — всё возвращается одной командой. Полный список с
комментариями — [`debloat.txt`](debloat.txt), 25 пакетов:

| Группа | Пакеты |
|---|---|
| Контент Сбера | Звук, StarLiveTV, «Видео», SberPlay, SberPrime, детская комната, родительский контроль, профили, умный дом (2 пакета), «Настройка провайдеров» |
| Обёртки | сберовский «YouTube» (`ru.sberdevices.startube`), Салют‑браузер |
| Фоновые службы | опросы, бета, багрепортер, «Очистка памяти», обновление прошивки пульта, геймпад |
| Трансляция | Huawei Cast, DLNA. **AirPlay (MHshare) оставлен** |
| Обновления прошивки | `ru.sberdevices.updater`, `ru.sberdevices.services.core` |
| Прочее | заставка Сбера, стоковый лаунчер Сбера (заменён Clean Your TV) |

**Оставлено:** YouTube (`com.google.android.youtube.tv`), Кинопоиск, Okko (`ru.more.play`), YummyTV, Happ,
Яндекс Браузер, RUTUBE, VLC, RuStore, каталог Сбера (спрятан в лаунчере — через него обновляются приложения),
голосовой помощник Салют (его нельзя отключить), HDMI‑входы, клавиатура и все системные пакеты.

Свободное место на `/data`: было **722 МБ**, стало **956 МБ** (удалены обновления отключённых системных
приложений и кэш заставки Сбера). Внимание: `pm uninstall-system-updates <пакет>` на этой прошивке **снова
включает** пакет — после него отключите его ещё раз.

Порог автоудаления приложений снижен до 1 % (`sys_storage_threshold_percentage`): StarOS удаляет ваши
приложения, когда свободно меньше ~231 МБ вместо ~355 МБ. Вернуть: `tools/venv/bin/python tools/tv.py 'settings delete global
sys_storage_threshold_percentage'`.

## Как вернуть одно приложение

```bash
TV_HOST=<tv ip> tools/venv/bin/python tools/tv.py 'pm enable ru.sberdevices.music'
```

Имя пакета — из таблицы выше или из [`debloat.txt`](debloat.txt). Чтобы оно не отключилось снова при
`check.py --fix`, закомментируйте его строку в `debloat.txt` (`# ` в начале).

## Как откатить всё

```bash
TV_HOST=<tv ip> tools/restore.sh examples/sber-sdx-50u4139/debloat.txt ru.sberdevices.starlauncher
TV_HOST=<tv ip> tools/venv/bin/python tools/tv.py 'settings delete global sys_storage_threshold_percentage'
TV_HOST=<tv ip> tools/venv/bin/python tools/tv.py 'settings delete secure screensaver_components'
```

Включит все пакеты из списка и вернёт домашним экран Сбера. Удалённые обновления системных приложений
вернутся через каталог Сбера. Лаунчер Clean Your TV останется установленным, но перестанет быть домашним
экраном.

## Если Сбер вернул мусор

Сервис Сбера умеет ставить свои приложения обратно после обновлений. Проверить и снова отключить то, что
вернулось:

```bash
TV_HOST=<tv ip> tools/venv/bin/python tools/check.py examples/sber-sdx-50u4139/debloat.txt
TV_HOST=<tv ip> tools/venv/bin/python tools/check.py --fix examples/sber-sdx-50u4139/debloat.txt com.gathin.tvhome
```

Первая команда только показывает, вторая исправляет. Если вернулась заставка Сбера — снова поставьте свою:
`tools/venv/bin/python tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'`.

## Лаунчер

Лаунчер собран из ветки `sber-neobrutal` этого репозитория (стиль из `design/`: песочный фон, кремовые
карточки с чёрной обводкой и жёсткой тенью, шрифты Unbounded и Golos Text). Все его настройки — в
[`config.json`](config.json); после правки отправьте на ТВ:

```bash
TV_HOST=<tv ip> tools/theme.sh examples/sber-sdx-50u4139
```

Переустановка лаунчера (после изменений в коде):

```bash
cd launcher && TV_HOST=<tv ip> STOCK_LAUNCHER=none INPUT_KEY_SERVICE=1 ./build-and-install.sh
```

`INPUT_KEY_SERVICE=1` обязателен — без него перестанут работать кнопки «ТВ» и «RUTUBE».

### Как поменять док

В `config.json`, ключ `dock_apps` — пакеты по порядку:

```json
"dock_apps": ["com.google.android.youtube.tv", "ru.kinopoisk.tv", "ru.more.play", "su.afk.yummy.tv", "com.yandex.browser.tv"]
```

Или прямо на ТВ: большая карточка над доком → «Убрать из дока»; добавить — меню шестерёнки →
«Изменить порядок приложений». Категория над названием в карточке — ключ `neo_categories`
(`"пакет=Категория"`).

### Как поменять кнопки пульта

Ключ `app_keys` в `config.json`:

```json
"app_keys": ["KEYCODE_F6:su.happ.proxyutility", "KEYCODE_F5:com.google.android.youtube.tv"]
```

| Кнопка | Код | Сейчас открывает |
|---|---|---|
| «ТВ» | `KEYCODE_F6` (скан‑код 64) | Happ |
| «RUTUBE» | `KEYCODE_F5` (скан‑код 63) | YouTube |

Замените пакет справа от двоеточия и отправьте тему (`tools/theme.sh`). Код другой кнопки можно узнать так:
`tools/venv/bin/python tools/tv.py 'timeout 15 getevent -lt'` и нажать её. Автоповтор громкости с этим сервисом сохраняется.

### Обои

Меню шестерёнки → **«Обои»**. Песок, Графит и Камень рисуются цветом (ключ `neo_wallpapers`:
`"Имя|#RRGGBB|Описание"`). Картинки лежат в [`wallpapers/`](wallpapers), их названия — в
`wallpapers/captions.txt` (`файл<TAB>Название`). Добавить свою: положите JPG 1920×1080 (больше не нужно —
интерфейс ТВ рисуется в 1080p), допишите строку в `captions.txt` и отправьте тему.

«Менять каждый день» листает всю коллекцию по одной в день; «Показывать как заставку» — обои за часами.

### Заставка

Системная заставка и часы на Home после 10 минут простоя (`home_idle_clock_minutes`) — `ClockDream`
лаунчера. Погоды нет: лаунчер не ходит в интернет.

## Замеры (`tools/bench.py --keys 36 --series 3`)

| | до редизайна | после |
|---|---|---|
| Отклик кнопки p50 / p95 | 127 / 142 мс | 125 / 146 мс |
| Память лаунчера (PSS) | 39 МБ | 34 МБ |
| CPU лаунчера в простое | 0,1 % | 0,1 % |
| Кадры с рывками | 4,7 % | 11,4 % |

Если на главном экране тормозит — добавьте `"reduced_motion": true` в `config.json`.
