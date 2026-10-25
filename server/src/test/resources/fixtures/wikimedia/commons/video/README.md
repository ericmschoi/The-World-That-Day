# Wikimedia Commons video fixtures (M3a4b T2)

## Recorded fixtures

- Source: Wikimedia Commons, MediaWiki Action API `action=query` with `prop=videoinfo&viprop=url|size|mime|mediatype|extmetadata|derivatives&viurlwidth=640`, the three requests `ActionApiCommonsVideoClient` sends (event `P10` file batch, event category search, `Videos taken on` date category). Saved exactly as received (no byte changed).
- Recorded: 2026-10-24, by backend-dev with `curl`, once per file (times below, UTC).
- User-Agent: `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)` (repository address only, no personal contact).
- Language: no `viextmetadatalanguage` (the API's default, English), as for photos.
- Only metadata JSON was recorded. No video file and no poster image was downloaded.
- License of the response text: the file description pages' text on Commons, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). Each file keeps its own license, as stated in its `LicenseShortName` (table below). These fixtures are used only in tests.
- Categories and file names come from `../../wikidata/video-links-*.json` (the `P10`, `P373` and `P910` values recorded the same day).

| File | Recorded (UTC) | Request | Response | Original URL |
|---|---|---|---|---|
| `p10-files-1969-07-20.json` | 2026-10-24 21:15 | `File:AP11 FINAL APPROACH.ogv` (`P10` of Q43653) | pageid 7366097, `application/ogg`, `VIDEO`, 270 s, `Public domain`, `DateTimeOriginal` `1969-07-20`; derivatives: the original (no `transcodekey`), 240p VP9 WebM, 360p `video/quicktime` (both 240 high) | `https://commons.wikimedia.org/w/api.php?action=query&titles=File%3aAP11+FINAL+APPROACH.ogv&prop=videoinfo&viprop=url%7csize%7cmime%7cmediatype%7cextmetadata%7cderivatives&viurlwidth=640&format=json&formatversion=2` |
| `category-apollo-11.json` | 2026-10-24 21:15 | videos directly in `Category:Apollo 11` | 5 videos (Public domain, CC BY 3.0, CC BY 2.5 ar), every page with `derivatives` | `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrlimit=10&gsrsearch=incategory%3a%22Apollo+11%22+filetype%3avideo&prop=videoinfo&viprop=url%7csize%7cmime%7cmediatype%7cextmetadata%7cderivatives&viurlwidth=640&format=json&formatversion=2` |
| `category-1988-summer-olympics.json` | 2026-10-24 21:15 | videos directly in `Category:1988 Summer Olympics` | 0: the body is `{"batchcomplete":true}` | `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrlimit=10&gsrsearch=incategory%3a%221988+Summer+Olympics%22+filetype%3avideo&prop=videoinfo&viprop=url%7csize%7cmime%7cmediatype%7cextmetadata%7cderivatives&viurlwidth=640&format=json&formatversion=2` |
| `category-2002-fifa-world-cup.json` | 2026-10-24 21:15 | videos directly in `Category:2002 FIFA World Cup` | 0: `{"batchcomplete":true}` | `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrlimit=10&gsrsearch=incategory%3a%222002+FIFA+World+Cup%22+filetype%3avideo&prop=videoinfo&viprop=url%7csize%7cmime%7cmediatype%7cextmetadata%7cderivatives&viurlwidth=640&format=json&formatversion=2` |
| `category-1986-asian-games.json` | 2026-10-24 21:15 | videos directly in `Category:1986 Asian Games` (the `P910` sitelink; Q495611 has no `P373`) | 0: `{"batchcomplete":true}` (as the research expected) | `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrlimit=10&gsrsearch=incategory%3a%221986+Asian+Games%22+filetype%3avideo&prop=videoinfo&viprop=url%7csize%7cmime%7cmediatype%7cextmetadata%7cderivatives&viurlwidth=640&format=json&formatversion=2` |
| `date-category-1969-07-20.json` | 2026-10-24 21:15 | `Category:Videos taken on 1969-07-20` | 1: `President Nixon speaking with astronauts Armstrong and Aldrin on the Moon.webm` (Public domain, `Taken on 20 July 1969`) | `https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category%3aVideos+taken+on+1969-07-20&gcmtype=file&gcmlimit=20&prop=videoinfo&viprop=url%7csize%7cmime%7cmediatype%7cextmetadata%7cderivatives&viurlwidth=640&format=json&formatversion=2` |
| `date-category-1988-09-17.json` | 2026-10-24 21:15 | `Category:Videos taken on 1988-09-17` (does not exist) | 0: `{"batchcomplete":true}` | `https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category%3aVideos+taken+on+1988-09-17&gcmtype=file&gcmlimit=20&prop=videoinfo&viprop=url%7csize%7cmime%7cmediatype%7cextmetadata%7cderivatives&viurlwidth=640&format=json&formatversion=2` |
| `date-category-2002-06-22.json` | 2026-10-24 21:15 | `Category:Videos taken on 2002-06-22` (does not exist) | 0: `{"batchcomplete":true}` | `https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category%3aVideos+taken+on+2002-06-22&gcmtype=file&gcmlimit=20&prop=videoinfo&viprop=url%7csize%7cmime%7cmediatype%7cextmetadata%7cderivatives&viurlwidth=640&format=json&formatversion=2` |

Seen in the recording:

- `derivatives` comes with the generator and `titles=` answers, so no separate derivative request is needed (plan "경로와 요청").
- The first derivative is the original file itself, without `transcodekey`; for an `application/ogg` original it is typed `video/ogg`. The mapper leaves that entry out, so such an original stays in `url` only.
- Derivative `type` values carry codec parameters (`video/webm; codecs="vp9, opus"`); the mapper keeps the part before `;`.
- `viextmetadatafilter` was tried first and is ignored by `prop=videoinfo` (the answer had the same bytes), so the client does not send it.
- `url`, `thumburl` and derivative `src` values end with `utm_*` queries on some entries; the mapper copies them unchanged.

## Synthetic responses

The rule tests (`CommonsFileMapperVideoTest`, `ActionApiCommonsVideoClientTest`) build synthetic pages in code with made-up file names and authors.
