# Privacy

Currency has no account, no analytics, no ads, no crash reporting and no server of
its own.

## What stays on your device

- Your list of currencies, their order, and the amount you last typed.
- The most recently downloaded exchange rates (one small file in app-private storage).

None of this leaves the device. Uninstalling the app deletes it.

## Network requests

The app's only permission is `INTERNET`, used solely to download public exchange
rates. Requests carry the generic User-Agent `Currency/1.0` and no identifiers, cookies
or API keys. They go to:

| When                                                                 | Host                                                     | Operator                                                                                                         |
| -------------------------------------------------------------------- | -------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| Latest rates (at most about once a day, or when you pull to refresh) | `api.frankfurter.dev`                                    | [Frankfurter](https://frankfurter.dev), an open-source rates API                                                 |
| Latest rates, only if Frankfurter fails                              | `cdn.jsdelivr.net`, then `latest.currency-api.pages.dev` | [fawazahmed0/exchange-api](https://github.com/fawazahmed0/exchange-api), served by jsDelivr and Cloudflare Pages |
| History chart, when you open it                                      | `api.frankfurter.dev`                                    | Frankfurter                                                                                                      |

The history request names the currency pair you opened (for example `USD` and `INR`);
the latest-rates request does not name any currency. Like any web server, these hosts
can see your IP address; their own privacy policies apply to that.

## Contact

vivek@ivy.homes
