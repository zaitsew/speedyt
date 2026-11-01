# Google Play — what to paste

Everything Play Console asks for before Speedyt's first release, in the order it
asks. Written for **0.1.0 (1)**; the release order in section 6 was used for 0.1.1 (2). The answers come from the code: the manifest asks
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
  The service account `play-publisher@<gcp-project>.iam.gserviceaccount.com`
  needs "Release to testing tracks" (and "Manage store presence" for listing
  images) on this app: Play Console → Users and permissions. While the app is
  still a draft, a listing commit also needs "Edit and delete draft apps";
  without it Play answers 403.

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

### Texts

One folder per language in [`docs/play/listing/`](play/listing/), in the
fastlane-supply layout: `title.txt` (≤ 30), `short_description.txt` (≤ 80),
`full_description.txt` (≤ 4000). Those files are the source of truth; edit
them, not a copy.

### Graphics

All in `docs/play/`, drawn by `swift tools/make-banner.swift`.

| asset | size | file |
|---|---|---|
| App icon | 512 × 512 PNG | `icon-512.png` |
| Feature graphic | 1024 × 500 | `feature-graphic-1024x500.png` |
| Android TV banner | 1280 × 720 | `tv-banner-1280x720.png` |
| TV screenshots | 1920 × 1080, 1–8 per language | `screenshots/<locale>/` |

Play still requires 2–8 phone screenshots on the default listing, even for a
TV-only app: the dashboard's "Set up your store listing" step stays open without
them. The phone slot accepts 16:9, so `tools/play-listing.sh` uploads the same
TV screenshots there.

Upload texts and graphics for every language with `tools/play-listing.sh`
(add `--dry-run` first). It assembles the fastlane layout and calls
`~/ops/bin/play listing`; nothing goes through the Console by hand.

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

1. Internal testing track, tester list with the owner's Google account (the one signed in on the TV).
2. The owner opens the opt-in link once, then installs from the TV's Play Store.
3. Production, in the Console. The service account has no production permission
   (403), and a draft app accepts only `--status draft` over the API, so it stays
   on the internal track:
   - Production → Create new release → Add from library → pick the bundle.
   - Set the release name and notes, then Next.
   - A first release fails with "No countries or regions": Countries / regions →
     select all → Save (178 today). Then return to the release and Save.
   - Publishing overview → Submit changes for review → confirm.
4. Quick checks run for up to 14 minutes, then Play reviews the first production
   release (allow up to 7 days). Managed publishing is off, so an approved release
   goes live on its own.

The Android TV opt-in applies when the review is sent: it leaves the list and shows
Active under Advanced settings → Form factors.

## 7. Submission history

| date | version | state |
|---|---|---|
| 2026-11-01 | 0.1.1 (2) | first production release, sent for review, full rollout |
