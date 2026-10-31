# Speedyt privacy policy

Last updated: 31 October 2026

Speedyt is an internet speed test for Android TV. It is open source:
<https://github.com/zaitsew/speedyt>.

## What data Speedyt collects

None. Speedyt has no accounts, no analytics, no advertising, and no
crash-reporting service. The developer receives no data from the app.

## What leaves your TV

Only the speed test itself. When you run a test, the app downloads and uploads
test data to and from Cloudflare's public speed-test servers
(`speed.cloudflare.com`). Like any web server, Cloudflare sees the IP address
the request comes from, and Cloudflare's own privacy policy applies to that
traffic: <https://www.cloudflare.com/privacypolicy/>. Speedyt does not read,
show or store your IP address.

The test data is random bytes. It contains nothing about you.

## What stays on your TV

- **Test history.** The last 50 results (date and time, download, upload, ping,
  jitter, and whether the TV was on Ethernet or Wi-Fi and which band) are stored
  on the TV only. Clearing the app's storage in the Android TV settings, or
  uninstalling Speedyt, deletes them.
- **Connection details.** To explain a result, the app reads how the TV is
  connected: Ethernet or Wi-Fi, the Wi-Fi band, signal strength and link speed.
  It does not read the Wi-Fi network name or any address, and it needs no
  location permission. These details are shown on screen; only the connection
  type and band are saved to the history.

## Permissions

- `INTERNET`: to run the test.
- `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE`: to show whether the TV is on Ethernet or Wi-Fi and how good the link is.

## Children

Speedyt does not collect personal information from anyone, including children.

## Changes

If this policy changes, the new version is published at this address with a new date.

## Contact

Open an issue at <https://github.com/zaitsew/speedyt/issues>.
