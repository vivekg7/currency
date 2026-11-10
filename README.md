# Currency

An Android currency converter with no ads, no tracking and no account. Free and open
source (MIT).

Converter apps on the Play Store are mostly ad-funded, and many ship analytics SDKs.
This one does the same job with one permission and nothing running in the background.

## Features

- **Multi-currency list.** Type into any row and every other row updates. The defaults
  are INR, USD, EUR, GBP and AED, plus your device locale's currency if it is another.
- **Inline calculator.** The keypad supports `+ − × ÷`, parentheses and `%`, so
  `1200×3+50` converts 3650. Half-typed expressions such as `1200×` still convert
  (as 1200), so the other rows don't flicker while you type.
- **Offline first.** The last rates are cached on the device, and the list always shows
  "Rates as of <date> (source)". After the first sync the app works with no network.
- **Refresh.** When the app opens it refetches only if newer rates should exist. Pull
  down to refresh manually.
- **160+ currencies.** Search by code, name or country, and drag the handle to reorder.
  Add or remove currencies from the same picker.
- **History chart** for any pair (1M, 6M, 1Y, 5Y), drawn with Compose Canvas. Touch the
  chart to read a day's rate.
- Material You dynamic colour, dark theme, edge-to-edge.

## Permissions

| Permission | Why                                                              |
| ---------- | ---------------------------------------------------------------- |
| `INTERNET` | Download exchange rates and chart history. Nothing else is sent. |

See [PRIVACY.md](PRIVACY.md) for exactly which hosts are contacted.

## Rate sources: which one and why

Neither source needs an API key. The app sends `User-Agent: Currency/1.0`.

|                | Frankfurter v2 (`api.frankfurter.dev/v2`)                               | fawazahmed0 exchange-api                                                 |
| -------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| Coverage       | 161 fiat currencies (plus crypto and metals, which the app filters out) | ~160 fiat currencies, plus many crypto tokens                            |
| Data           | Blends ~40 central-bank feeds (ECB, RBI, Bank Al-Maghrib, …)            | Aggregated, published daily as static files                              |
| History        | Decades, as one time-series request, optionally grouped weekly          | One file per day since March 2024, so a chart needs hundreds of requests |
| Infrastructure | A single API server                                                     | jsDelivr CDN, with a documented Cloudflare Pages mirror                  |

- **Primary: Frankfurter v2.** The original Frankfurter (`/v1`) only has the ~30 ECB
  currencies, with no AED or SAR. Its v2 API covers INR, AED, SAR and the rest, and
  serves a whole chart in one request, which neither the alternative nor v1 can do for
  these currencies.
- **Fallback for latest rates: exchange-api.** It runs on separate infrastructure (a
  static file on a CDN, tried on jsDelivr first and then on `currency-api.pages.dev`),
  so one outage doesn't leave a fresh install without rates. It is not used for charts.
  Fetching a series one day at a time would be slow and unkind to a free service.
- Both sources quote against EUR, and every other pair is a cross rate through EUR.
  One ~12 KB download therefore covers every combination.
- **Bundled currency list.** The 161 supported fiat codes are bundled in the app
  ([`Currencies.kt`](app/src/main/java/com/crylo/currency/core/Currencies.kt)). Bundling
  lets the picker work before the first sync and filters crypto out of both feeds. A
  currency introduced later needs an app update. Names and countries come from the
  platform's locale data, so they are translated.

## Design decisions

- **Staleness.** Both sources publish daily. A snapshot dated D counts as superseded
  once day D+1 begins (UTC). After that, opening the app retries at most hourly until
  the source has the new day, and always retries after 24 h. This keeps rates current
  without a request on every open, and avoids hammering the source on days when nothing
  new is published.
- **Formatting.** Amounts use ISO 4217 minor units (JPY 0 decimals, KWD 3), rounded half
  up, with the locale's separators. An amount below 1 keeps 3 significant digits, so
  1 IDR shows as 0.0000597 USD and not 0.00.
- **The calculator is our own small recursive-descent parser**
  ([`Expression.kt`](app/src/main/java/com/crylo/currency/core/Expression.kt)). A
  scripting engine would be a large dependency for five grammar rules. The locale's
  decimal separator is honoured, and the other separator is read as grouping.
- **On-screen keypad, not the system keyboard.** Number keyboards lack `×`, `÷` and
  parentheses, and a fixed keypad keeps the list from jumping around.
- **No flags.** Rows show a code badge instead, which avoids licensing a flag image set
  and politically contested flags.
- **Logic lives in plain Kotlin** (`core/`) with JVM unit tests. These cover cross
  rates, the parser, per-currency rounding, parsing of both sources and the cache, and
  staleness. The UI is a thin Compose layer over one ViewModel. With only three
  screens, no navigation library is used.

## Building

This machine has no system JDK; Android Studio's bundled JBR works:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest
./gradlew assembleRelease   # unsigned unless keystore.properties exists
```

To sign release builds, create an untracked `keystore.properties` next to
`settings.gradle.kts`:

```properties
storeFile=/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Requires Android 12 (API 31) or newer.

## License

MIT. See [LICENSE](LICENSE).
