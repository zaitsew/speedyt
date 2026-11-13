# Speedyt

An ad-free, open-source internet speed test for Android TV and Google TV.
It measures download, upload, ping, jitter and latency under load, then says
in plain words what the connection can handle: how many 4K streams, HD video
calls, and whether it is good for gaming. No ads, no accounts, no tracking.

Built for the living room: dark theme, large type, full D-pad control.
English, Spanish and Russian. It runs on Android 8.0 (API 26) and newer.

Google Play: version 0.1.1 (2) went to production review on 2026-11-01
([`docs/PLAY-STORE.md`](docs/PLAY-STORE.md)). Until you find it in the TV's Play Store, build it as shown below.

## Quick start

```sh
git clone https://github.com/zaitsew/speedyt && cd speedyt
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # your Android SDK path
./gradlew :app:assembleDebug                                   # needs JDK 17 or newer
adb connect <tv-ip>:5555                                       # TV: Settings > Developer options > Network debugging
adb install -r app/build/outputs/apk/debug/app-debug.apk       # then open Speedyt from the TV launcher
```

## How it measures

The test talks to Cloudflare's public speed-test endpoints,
`GET https://speed.cloudflare.com/__down?bytes=N` and `POST .../__up`.

| Stage | What happens |
|---|---|
| Idle latency | 1 warm-up request (opens the connection, not counted), then 20 requests of `__down?bytes=0`. Time from sending the request to the first response byte. Reported: median (ping) and jitter, the mean absolute difference of consecutive samples. |
| Download | 6 parallel HTTP/1.1 connections stream 50 MB requests for 10 s. Bytes are counted as they arrive and sampled 4 times a second. The first 2 s are dropped as warm-up; the result is the rate over the remaining 8 s. |
| Upload | The same, with 6 parallel POSTs of 25 MB each (one random block, repeated) for 10 s. |
| Loaded latency | While download runs, and again while upload runs, a separate connection keeps probing latency about 5 times a second. Probes in the warm-up window are dropped. The median is reported for each phase. |
| Bufferbloat | Grade from the worse of the two loaded medians minus the idle ping: under 30 ms A, under 60 ms B, under 200 ms C, otherwise D. |

The server is named by its data-centre code, the `colo` (or `cf-meta-colo`)
response header, and shown as "Server: Madrid (MAD)" through a table of common
codes in [`ColoNames.kt`](app/src/main/kotlin/com/fabriquefutur/speedyt/ui/ColoNames.kt).
Cloudflare's `city` and `country` headers locate the viewer, not the server,
so they are never shown.
The client IP address is never read, shown or stored.

"What this connection handles" uses named thresholds in
[`Thresholds.kt`](app/src/main/kotlin/com/fabriquefutur/speedyt/analysis/Thresholds.kt):
25 Mbps per 4K stream (YouTube recommends 20, Netflix 15), 5 Mbps per 1080p
stream, 4 Mbps both ways per HD video call, and for gaming a ping under 50 ms
with jitter under 20 ms.

The Connection card shows the network type, the Wi-Fi band and signal, and the
Wi-Fi link rates: receive and send (`↓ 433 · ↑ 866 Mbps`; Android 10 or newer
reports both, older versions show only the send rate). Tips appear only when
they apply:

- switch to 5 GHz, when the TV is on 2.4 GHz;
- weak signal (below -70 dBm);
- a download of 85 to 97 Mbps on Ethernet, which points at a 100 Mbps port;
- "Wi-Fi link is the bottleneck", when the link rate is under 2 times the
  download speed. It uses the receive rate, or the send rate on older Android.

The History screen lists the last 50 results.

Numbers from a TV are real but not lab-grade. Wi-Fi, the TV's own network
chip and the distance to the nearest Cloudflare data centre all show up in
the result. That is the point: it measures what the TV gets.

## Privacy

Speedyt has no analytics, no ads, no crash-reporting SDK and no accounts.
The only network traffic is the speed test itself, to Cloudflare. The last 50
results are stored on the TV and nowhere else. It asks for no location
permission, and it never reads the Wi-Fi network name.
See [docs/privacy.md](docs/privacy.md).

## Develop

```sh
./check.sh                              # unit tests + lint + debug build (the gate)
git config core.hooksPath .githooks     # run ./check.sh before every push
./check.sh usage                        # prints telemetry=none
```

- `engine/` is pure Kotlin: the transport sits behind an interface and the
  clock is injected, so the whole test runs in virtual time in unit tests.
- `analysis/` holds the judgement: handles, bufferbloat grade, Wi-Fi band and signal, tips.
- Strings are in `res/values` (English), `values-es` and `values-ru`.
- `tools/make-banner.swift` redraws the TV banner (macOS).
- `docs/play/` holds the Play listing texts, graphics and screenshots in three languages.

### Release build

`./gradlew :app:bundleRelease` builds a shrunk (R8) Android App Bundle in
`app/build/outputs/bundle/release/`. It is signed when
`~/.speedyt/android-signing.properties` exists (`storeFile`, `storePassword`,
`keyAlias`, `keyPassword`), and unsigned otherwise. Keys are never committed.

## License

MIT. See [LICENSE](LICENSE).

<!-- teyla:reviewed 2026-11-13 -->
