# Wikimedia Commons fixtures (M3a3b T1)

## Recorded fixtures (`day-category-*.json`)

- Source: Wikimedia Commons, MediaWiki Action API `action=query&generator=categorymembers` with `prop=imageinfo`, the day category request `ActionApiCommonsClient.dayCategory` sends. Saved exactly as received (no byte changed).
- Recorded: 2026-10-14, by backend-dev with `curl`, once per file (times below, UTC).
- User-Agent: `The-World-That-Day (https://github.com/ericmschoi/The-World-That-Day)` (repository address only, no email).
- Language: no `iiextmetadatalanguage`, so the API's default (English). With `ko` the API localizes `DateTimeOriginal` to `촬영 날짜: 1988년 9월 17일`, which `CaptureTimeParser` does not read (PM decision (a), 2026-10-14).
- Only metadata JSON was recorded. No image file was downloaded.
- License of the response text: the file description pages' text on Commons, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). Each file keeps its own license, as stated in its `LicenseShortName` (table below). These fixtures are used only in tests.

| File | Recorded (UTC) | Category | Files in the response (`LicenseShortName`) | Original URL |
|---|---|---|---|---|
| `day-category-1988-09-17.json` | 2026-10-14 21:45 | `Category:South Korea photographs taken on 1988-09-17` | 5: `Seoul Olympic torch.jpg`, `US Olympics Team 1988.JPEG`, `1988 Summer Olympics opening ceremony.JPEG` (Public domain, U.S. Air Force), `Jose 5.jpg` (CC BY-SA 3.0), `Seul 1988 - Romania.jpg` (Public domain) | `https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category:South%20Korea%20photographs%20taken%20on%201988-09-17&gcmtype=file&gcmlimit=20&prop=imageinfo&iiprop=url%7Cmime%7Csize%7Cextmetadata&iiurlwidth=640&format=json&formatversion=2` |
| `day-category-2002-06-22.json` | 2026-10-14 21:45 | `Category:South Korea photographs taken on 2002-06-22` | 1: `2002년 한·일 월드컵-대한민국·스페인 8강 경기-경기장 전경.jpg` (KOGL Type 1) | `https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category:South%20Korea%20photographs%20taken%20on%202002-06-22&gcmtype=file&gcmlimit=20&prop=imageinfo&iiprop=url%7Cmime%7Csize%7Cextmetadata&iiurlwidth=640&format=json&formatversion=2` |
| `day-category-1995-03-15.json` | 2026-10-14 21:45 | `Category:South Korea photographs taken on 1995-03-15` (does not exist) | 0: the body is `{"batchcomplete":true}` | `https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category:South%20Korea%20photographs%20taken%20on%201995-03-15&gcmtype=file&gcmlimit=20&prop=imageinfo&iiprop=url%7Cmime%7Csize%7Cextmetadata&iiurlwidth=640&format=json&formatversion=2` |

Seen in the recording: every `url` is on `upload.wikimedia.org` and every `thumburl` on `thumb.wikimedia.org` with the `960px` width step (`thumbwidth` 640); both end with a `utm_*` query that the mapper copies unchanged. No file has `NonFree` or GPS values.

## Recorded file batch (`files-*.json`, M3a3c T1)

- Source: Wikimedia Commons, MediaWiki Action API `action=query&titles=File:…` with `prop=imageinfo`, the request `ActionApiCommonsClient.files` sends for the lead image names Korean Wikipedia PageImages gave (`../kowiki/pageimages-*.json`). Saved exactly as received (no byte changed).
- Recorded: 2026-10-16, by backend-dev with `curl`, once (time below, UTC).
- User-Agent: `The-World-That-Day (https://github.com/ericmschoi/The-World-That-Day)` (repository address only, no email).
- Language: no `iiextmetadatalanguage` (the API's default, English), as above.
- Only metadata JSON was recorded. No image file was downloaded.
- License of the response text: the file description page's text on Commons, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). The file keeps its own license (`LicenseShortName` below).
- 2002-06-22 has no file: PageImages gave no lead image for its event, so no file request is sent and there is no `files-2002-06-22.json`.

| File | Recorded (UTC) | Requested | Response (`mime`, `LicenseShortName`, `DateTimeOriginal`) | Original URL |
|---|---|---|---|---|
| `files-1988-09-17.json` | 2026-10-16 06:03 | `File:KBS_2_logo.svg` (lead image of `KBS 2TV`) | normalized to `File:KBS 2 logo.svg`, pageid 58546568, on Commons (`imagerepository` `local` there); `image/svg+xml`, `Public domain`, `2022-07-10`. Also carries an `iistart` `continue` block for the file history, which the client ignores | `https://commons.wikimedia.org/w/api.php?action=query&titles=File%3aKBS_2_logo.svg&prop=imageinfo&iiprop=url%7cmime%7csize%7cextmetadata&iiurlwidth=640&format=json&formatversion=2` |

## Synthetic responses

The rule tests (`CommonsFileMapperTest`, `ActionApiCommonsClientTest`) build synthetic pages in code with made-up file names and authors.
