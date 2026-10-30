# Google Play — what to paste

Everything Play Console asks for before Speedyt's first release, in the order it
asks. Written for **0.1.0 (1)**. The answers come from the code: the manifest asks
for `INTERNET`, `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE` only, and the app
has no SDK besides AndroidX, Compose for TV and OkHttp.

## 1. Account, package, signing

- Account: the **Fabrique Futur** organisation account (the one Loco and Iron U ship
  from). It is exempt from the 12-testers-for-14-days rule of personal accounts, so
  the order is internal testing → production.
- Package `com.fabriquefutur.speedyt` (permanent).
- **Upload key** `~/.speedyt/android-upload.jks`, password in
  `~/.speedyt/android-signing.properties`. Both live outside the repo. Play App
  Signing re-signs with Google's key; if the upload key is lost, Play Console →
  App integrity → "Request upload key reset".
  Upload certificate SHA-256:
  `7F:C8:20:6E:E5:B2:47:D7:45:97:BC:18:A3:86:3A:FB:FB:76:E3:15:4C:9D:77:34:F8:5C:E7:D1:9E:89:77:DB`
- Build: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
  `versionCode` rises with every upload.
- Upload: `~/ops/bin/play upload --package com.fabriquefutur.speedyt --aab <aab> --track internal`.
  The service account `play-publisher@fabrique-accounts.iam.gserviceaccount.com`
  needs "Release to testing tracks" (and "Manage store presence" for listing
  images) on this app: Play Console → Users and permissions.

## 2. Create app

| field | value |
|---|---|
| App name | `Speedyt: TV Speed Test` |
| Default language | English (United States) — `en-US` |
| App or game | App |
| Free or paid | Free (permanent once published) |

## 3. Store listing

Languages: `en-US` (default), `es-ES`, `ru-RU` — the app speaks the same three.
Category **Tools**. Contact email: the Fabrique Futur support address. Website:
`https://zaitsew.github.io/speedyt/`.

### en-US

- **Name (≤ 30):** `Speedyt: TV Speed Test`
- **Short description (≤ 80):** `Ad-free speed test for your TV: can your line handle 4K, calls and gaming?`
- **Full description:**

```
Speedyt measures the internet connection of your TV itself, not your phone's.

Press Start with the remote. In about 30 seconds you see:
• Download and upload speed
• Ping, jitter and latency under load (bufferbloat grade A–D)
• What your line handles: how many 4K and 1080p streams at once, HD video calls, online gaming
• Your TV's connection: Ethernet or Wi-Fi, the Wi-Fi band (2.4, 5 or 6 GHz), signal strength and link speed

Speedyt also tells you when the TV is the bottleneck. Examples: a weak Wi-Fi signal, the 2.4 GHz band, or an Ethernet port limited to 100 Mbps, which many TVs have.

Your last 50 results stay on the TV, so you can compare Wi-Fi and cable, or morning and evening.

No ads. No account. No tracking. The test runs against Cloudflare's global network, and nothing else leaves your TV. Speedyt is open source: github.com/zaitsew/speedyt
```

### es-ES

- **Nombre:** `Speedyt: test de velocidad TV`
- **Descripción breve:** `Test de velocidad sin anuncios para tu TV: ¿aguanta 4K, videollamadas y juegos?`
- **Descripción completa:**

```
Speedyt mide la conexión a internet de tu propio televisor, no la de tu móvil.

Pulsa Empezar con el mando. En unos 30 segundos verás:
• Velocidad de descarga y de subida
• Ping, jitter y latencia bajo carga (nota de bufferbloat de la A a la D)
• Lo que aguanta tu línea: cuántas emisiones 4K y 1080p a la vez, videollamadas HD, juegos online
• La conexión de tu TV: Ethernet o Wi-Fi, la banda Wi-Fi (2,4, 5 o 6 GHz), la intensidad de la señal y la velocidad del enlace

Speedyt también te avisa cuando el cuello de botella es el televisor: una señal Wi-Fi débil, la banda de 2,4 GHz o un puerto Ethernet de 100 Mbps, algo habitual en muchos televisores.

Tus últimos 50 resultados se guardan en la TV, para comparar Wi-Fi y cable, o la mañana y la noche.

Sin anuncios. Sin cuenta. Sin rastreo. La prueba se hace contra la red global de Cloudflare y nada más sale de tu televisor. Speedyt es de código abierto: github.com/zaitsew/speedyt
```

### ru-RU

- **Название:** `Speedyt: тест скорости для ТВ`
- **Краткое описание:** `Тест скорости для телевизора без рекламы: потянет ли линия 4K, звонки и игры?`
- **Полное описание:**

```
Speedyt измеряет интернет самого телевизора, а не телефона.

Нажмите «Старт» на пульте. Примерно через 30 секунд вы увидите:
• Скорость загрузки и отдачи
• Пинг, джиттер и задержку под нагрузкой (оценка bufferbloat от A до D)
• Что потянет ваша линия: сколько потоков 4K и 1080p одновременно, видеозвонки в HD, онлайн-игры
• Подключение телевизора: Ethernet или Wi-Fi, диапазон Wi-Fi (2,4, 5 или 6 ГГц), уровень сигнала и скорость соединения

Speedyt подскажет, если узкое место — сам телевизор: слабый сигнал Wi-Fi, диапазон 2,4 ГГц или порт Ethernet на 100 Мбит/с, который стоит во многих телевизорах.

Последние 50 результатов хранятся на телевизоре: можно сравнить Wi-Fi и кабель, утро и вечер.

Без рекламы. Без аккаунта. Без слежки. Тест идёт через глобальную сеть Cloudflare, больше ничего с телевизора не уходит. Исходный код Speedyt открыт: github.com/zaitsew/speedyt
```

### Graphics

All in `docs/play/`.

| asset | size | file |
|---|---|---|
| App icon | 512 × 512 PNG | `icon-512.png` |
| Feature graphic | 1024 × 500 | `feature-graphic.png` |
| Android TV banner | 1280 × 720 | `tv-banner.png` |
| TV screenshots | 1920 × 1080, 1–8 per language | `screenshots/<locale>/` |

Phone screenshots are not needed: the app is TV-only (`leanback` required).

## 4. Form factors

Advanced settings → Form factors → **Add Android TV**, then opt in. The TV listing
uses the TV banner and TV screenshots. Remove "Phone" if Play added it: the
manifest requires leanback, so phones cannot install it anyway. Play reviews TV
apps against the TV app quality guidelines: D-pad navigation everywhere, no
touch-only controls, a leanback banner, and readable text at TV distance.

## 5. App content

| section | answer |
|---|---|
| Privacy policy | `https://zaitsew.github.io/speedyt/privacy` |
| App access | All functionality is available without special access |
| Ads | No, the app contains no ads |
| Content rating | Questionnaire category **"All other app types"**; answer No to every content question (no violence, sexuality, language, controlled substances, gambling, user interaction, sharing location, or digital purchases). Expected: PEGI 3 / Everyone |
| Target audience | 18 and over (keeps the app out of the Families program; nothing in it targets children) |
| News app | No |
| Data safety | See below |
| Government app | No |
| Financial features | None |
| Health | None |
| Advertising ID | No (the app does not use it) |

### Data safety

- Does your app collect or share any of the required user data types? **No.**
- Is all user data encrypted in transit? **Yes** (all traffic is HTTPS).
- Account deletion: not applicable (no accounts).

Why "No": the history of results stays on the TV (DataStore) and is never sent.
The speed-test requests go to Cloudflare, which sees the TV's IP address as any
web server does; Speedyt sends no identifier, no history and no device data with
them, and reads back only the server location headers.

## 6. Release order

1. Internal testing track, tester list with Ivan's Google account (the one signed in on the TV).
2. Ivan opens the opt-in link once, then installs from the TV's Play Store.
3. Production, after a day of use without a blocking problem. Play reviews the
   first production release and the TV form factor (allow up to 7 days).
