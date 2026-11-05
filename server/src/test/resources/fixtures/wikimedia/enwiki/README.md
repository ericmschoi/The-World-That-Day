# English Wikipedia fixtures (C1 S3a T1)

Recorded answers for the five demo dates of plan `docs/plans/c2-english-events.md` (decision 1 (b): 1988-02-13, 1976-07-17, 2010-02-12, 1980-04-12, and the video sample 1969-07-20). These are public event dates, not anyone's birthday.

## How they were recorded

- Recorded: 2026-10-30 between 04:05 and 04:10 UTC, by backend-dev with `curl`, one request at a time with a pause of 1.5 to 2 seconds between requests (43 requests in all; five date-page section lists were asked twice, see below). The exact time of each file is in the tables.
- User-Agent: `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)` (repository address only). No key, no login, no email or other personal data was sent.
- Every file holds the response body exactly as received (no byte changed). Each answered HTTP 200 with no `error` and no `warnings` member.
- Only JSON metadata was recorded. No media file was downloaded: the image file names below were looked up on Commons, but the images themselves were never requested.
- The URLs are the ones the server sends, with the same parameter order and the strict percent-encoding of `UriComponentsBuilder.encode()` (spaces as `%20`, `|` as `%7C`, upper-case escapes).
- The date pages' section lists were first asked with `page=February_13` and so on, then again with `page=February%2013` (the server's form). Both answers were the same bytes; the second is stored.

## Licences

- English Wikipedia text (section lists, Events sections, link and lead image lookups): [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) (and GFDL). The contributors of each page are in its history, `https://en.wikipedia.org/w/index.php?title=<title>&action=history`. The fixtures are used only in tests and the wikitext is not edited.
- Wikidata items in the link lookups (`wikibase_item`) and the Wikidata Query Service answers: [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) ([Wikidata:Licensing](https://www.wikidata.org/wiki/Wikidata:Licensing)). No attribution is required.
- Commons file information: the file description pages' text is [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). Each file keeps its own licence, stated in the response (`extmetadata.LicenseShortName`) and listed below.

## Section lists and Events sections (`<page>-tocdata.json`, `<page>-events.json`)

- Request 1 per page: `action=parse&page=<title>&prop=tocdata|revid&redirects=1`. `prop=sections` is not used (it is deprecated, research `canada-day-events.md` section 3).
- Request 2 per page: the level-2 (`hLevel` 2) `Events` section of that revision, `action=parse&oldid=<revid>&prop=wikitext&section=<index>`. The index is 1 on every page except `2010`, where a `Pronunciation` section comes first and `Events` is index 2.
- Only the `Events` section was requested, with its sub-sections (`Pre-1600`, `1601–1900`, `1901–present` on date pages; one per month on year pages). Births, Deaths and Holidays were not requested, so they are not stored (the section lists still name them).
- `revid` is the page revision the section list returned; the Events request is pinned to it with `oldid`. File names use `MM-DD` for date pages and the year for year pages, as in `../kowiki/`.

| File | Page | `revid` | Recorded (UTC) | Original URL |
|---|---|---|---|---|
| `02-13-tocdata.json` | February 13 | 1370674264 | 2026-10-30 04:06:14 | `https://en.wikipedia.org/w/api.php?action=parse&page=February%2013&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `02-13-events.json` | February 13 (Events) | 1370674264 | 2026-10-30 04:06:33 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1370674264&prop=wikitext&section=1&format=json&formatversion=2` |
| `07-17-tocdata.json` | July 17 | 1375524665 | 2026-10-30 04:06:16 | `https://en.wikipedia.org/w/api.php?action=parse&page=July%2017&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `07-17-events.json` | July 17 (Events) | 1375524665 | 2026-10-30 04:06:35 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1375524665&prop=wikitext&section=1&format=json&formatversion=2` |
| `02-12-tocdata.json` | February 12 | 1370674257 | 2026-10-30 04:06:18 | `https://en.wikipedia.org/w/api.php?action=parse&page=February%2012&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `02-12-events.json` | February 12 (Events) | 1370674257 | 2026-10-30 04:06:37 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1370674257&prop=wikitext&section=1&format=json&formatversion=2` |
| `04-12-tocdata.json` | April 12 | 1378081960 | 2026-10-30 04:06:20 | `https://en.wikipedia.org/w/api.php?action=parse&page=April%2012&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `04-12-events.json` | April 12 (Events) | 1378081960 | 2026-10-30 04:06:40 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1378081960&prop=wikitext&section=1&format=json&formatversion=2` |
| `07-20-tocdata.json` | July 20 | 1373354079 | 2026-10-30 04:06:21 | `https://en.wikipedia.org/w/api.php?action=parse&page=July%2020&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `07-20-events.json` | July 20 (Events) | 1373354079 | 2026-10-30 04:06:42 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1373354079&prop=wikitext&section=1&format=json&formatversion=2` |
| `1988-tocdata.json` | 1988 | 1382686554 | 2026-10-30 04:05:47 | `https://en.wikipedia.org/w/api.php?action=parse&page=1988&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `1988-events.json` | 1988 (Events) | 1382686554 | 2026-10-30 04:06:45 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1382686554&prop=wikitext&section=1&format=json&formatversion=2` |
| `1976-tocdata.json` | 1976 | 1382023488 | 2026-10-30 04:05:49 | `https://en.wikipedia.org/w/api.php?action=parse&page=1976&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `1976-events.json` | 1976 (Events) | 1382023488 | 2026-10-30 04:06:47 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1382023488&prop=wikitext&section=1&format=json&formatversion=2` |
| `2010-tocdata.json` | 2010 | 1382559665 | 2026-10-30 04:05:51 | `https://en.wikipedia.org/w/api.php?action=parse&page=2010&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `2010-events.json` | 2010 (Events) | 1382559665 | 2026-10-30 04:06:51 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1382559665&prop=wikitext&section=2&format=json&formatversion=2` |
| `1980-tocdata.json` | 1980 | 1382118549 | 2026-10-30 04:05:53 | `https://en.wikipedia.org/w/api.php?action=parse&page=1980&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `1980-events.json` | 1980 (Events) | 1382118549 | 2026-10-30 04:06:54 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1382118549&prop=wikitext&section=1&format=json&formatversion=2` |
| `1969-tocdata.json` | 1969 | 1380604178 | 2026-10-30 04:05:55 | `https://en.wikipedia.org/w/api.php?action=parse&page=1969&prop=tocdata%7Crevid&redirects=1&format=json&formatversion=2` |
| `1969-events.json` | 1969 (Events) | 1380604178 | 2026-10-30 04:06:57 | `https://en.wikipedia.org/w/api.php?action=parse&oldid=1380604178&prop=wikitext&section=1&format=json&formatversion=2` |

## Link lookups (`pageprops-<date>.json`)

- Request: `action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&titles=<titles>`, the lookup `ActionApiKoWikiLinkResolver` sends. The titles are the links of that day's lines (date page Events first, then year page Events), in first-seen order, normalized as `KoWikiLinkResolver.normalize` does (first Latin letter upper case, so `apartheid` is `Apartheid`). Date and year keys, including a range end such as `[[February 28|28]]`, are not part of a line's links.
- Day lines found (research section 2): date page 0, 2, 0, 3, 2 and year page 1, 2, 1, 2, 2 for the dates in table order. Footnotes (`<ref>…</ref>`) are dropped before the links are read.
- Redirects in the answers: `IOC` to `International Olympic Committee`, `East Timor` to `Timor-Leste` (1976-07-17); `St. John's, Newfoundland` to `St. John's, Newfoundland and Labrador`, `Samuel K. Doe` to `Samuel Doe`, `Presidential succession` to `Order of succession` (fragment `Republics`), `Marathon of Hope` to `Terry Fox` (fragment `Marathon of Hope`) (1980-04-12). No page is missing.
- `query` answers carry no `revid`; the result is the state at recording time.

| File | Date | Requested titles | Recorded (UTC) | Original URL |
|---|---|---|---|---|
| `pageprops-1988-02-13.json` | 1988-02-13 | `1988 Winter Olympics`, `Calgary`, `Alberta` | 2026-10-30 04:08:21 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&titles=1988%20Winter%20Olympics%7CCalgary%7CAlberta&format=json&formatversion=2` |
| `pageprops-1976-07-17.json` | 1976-07-17 | `East Timor`, `Indonesian invasion of East Timor`, `Provinces of Indonesia`, `Indonesia`, `1976 Summer Olympics`, `Montreal`, `New Zealand`, `IOC`, `South Africa`, `Apartheid`, `Indonesian occupation of East Timor` | 2026-10-30 04:08:22 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&titles=East%20Timor%7CIndonesian%20invasion%20of%20East%20Timor%7CProvinces%20of%20Indonesia%7CIndonesia%7C1976%20Summer%20Olympics%7CMontreal%7CNew%20Zealand%7CIOC%7CSouth%20Africa%7CApartheid%7CIndonesian%20occupation%20of%20East%20Timor&format=json&formatversion=2` |
| `pageprops-2010-02-12.json` | 2010-02-12 | `2010 Winter Olympics`, `Vancouver`, `Whistler, British Columbia` | 2026-10-30 04:08:24 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&titles=2010%20Winter%20Olympics%7CVancouver%7CWhistler%2C%20British%20Columbia&format=json&formatversion=2` |
| `pageprops-1980-04-12.json` | 1980-04-12 | `1980 Liberian coup d'état`, `Transbrasil Flight 303`, `Boeing 727`, `Hercílio Luz International Airport`, `Florianópolis`, `Terry Fox`, `St. John's, Newfoundland and Labrador`, `Samuel K. Doe`, `Liberia`, `Coup d'état`, `William Tolbert`, `Democracy`, `Presidential succession`, `Marathon of Hope`, `St. John's, Newfoundland` | 2026-10-30 04:08:26 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&titles=1980%20Liberian%20coup%20d%27%C3%A9tat%7CTransbrasil%20Flight%20303%7CBoeing%20727%7CHerc%C3%ADlio%20Luz%20International%20Airport%7CFlorian%C3%B3polis%7CTerry%20Fox%7CSt.%20John%27s%2C%20Newfoundland%20and%20Labrador%7CSamuel%20K.%20Doe%7CLiberia%7CCoup%20d%27%C3%A9tat%7CWilliam%20Tolbert%7CDemocracy%7CPresidential%20succession%7CMarathon%20of%20Hope%7CSt.%20John%27s%2C%20Newfoundland&format=json&formatversion=2` |
| `pageprops-1969-07-20.json` | 1969-07-20 | `Apollo program`, `Apollo 11`, `Moon`, `Mare Tranquillitatis`, `Neil Armstrong`, `Buzz Aldrin`, `Honduras`, `El Salvador`, `Football War`, `Moon landing`, `Lunar Module Eagle`, `1969 Tour de France`, `Eddy Merckx` | 2026-10-30 04:08:28 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&titles=Apollo%20program%7CApollo%2011%7CMoon%7CMare%20Tranquillitatis%7CNeil%20Armstrong%7CBuzz%20Aldrin%7CHonduras%7CEl%20Salvador%7CFootball%20War%7CMoon%20landing%7CLunar%20Module%20Eagle%7C1969%20Tour%20de%20France%7CEddy%20Merckx&format=json&formatversion=2` |

## Lead image lookups (`pageimages-<date>.json`)

- Request: `action=query&prop=pageimages&piprop=name&pilicense=free&pilimit=50&redirects=1&titles=<titles>`, the lookup `ActionApiKoWikiPageImages` sends, now on English Wikipedia.
- The titles are the main links (first link, after redirects) of every event the day is expected to show once lines of the same event are merged, in page order. The server asks only for its `normal` events, so on a day with a `tragic` event its request is a subset of this one (for example `Terry Fox` alone on 1980-04-12); the answer is looked up by title, so the extra pages do no harm.
- Results: `1988 Winter Olympics` and `2010 Winter Olympics` have no free lead image (their logos are non-free, research `canada-day-events.md` section 2) and `1980 Liberian coup d'état` has none. The others name one file each, listed in the next table.

| File | Date | Requested titles | Recorded (UTC) | Original URL |
|---|---|---|---|---|
| `pageimages-1988-02-13.json` | 1988-02-13 | `1988 Winter Olympics` | 2026-10-30 04:09:14 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&piprop=name&pilicense=free&pilimit=50&redirects=1&titles=1988%20Winter%20Olympics&format=json&formatversion=2` |
| `pageimages-1976-07-17.json` | 1976-07-17 | `Timor-Leste`, `1976 Summer Olympics` | 2026-10-30 04:09:15 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&piprop=name&pilicense=free&pilimit=50&redirects=1&titles=Timor-Leste%7C1976%20Summer%20Olympics&format=json&formatversion=2` |
| `pageimages-2010-02-12.json` | 2010-02-12 | `2010 Winter Olympics` | 2026-10-30 04:09:17 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&piprop=name&pilicense=free&pilimit=50&redirects=1&titles=2010%20Winter%20Olympics&format=json&formatversion=2` |
| `pageimages-1980-04-12.json` | 1980-04-12 | `1980 Liberian coup d'état`, `Transbrasil Flight 303`, `Terry Fox` | 2026-10-30 04:09:19 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&piprop=name&pilicense=free&pilimit=50&redirects=1&titles=1980%20Liberian%20coup%20d%27%C3%A9tat%7CTransbrasil%20Flight%20303%7CTerry%20Fox&format=json&formatversion=2` |
| `pageimages-1969-07-20.json` | 1969-07-20 | `Apollo program`, `Honduras`, `1969 Tour de France` | 2026-10-30 04:09:21 | `https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&piprop=name&pilicense=free&pilimit=50&redirects=1&titles=Apollo%20program%7CHonduras%7C1969%20Tour%20de%20France&format=json&formatversion=2` |

## Commons file information (`commons-files-<date>.json`)

- Request: `action=query&titles=File:<name>|…&prop=imageinfo&iiprop=url|mime|size|extmetadata&iiurlwidth=640` on Commons, the file batch `ActionApiCommonsClient.files` sends for the lead image names above (same order, underscores kept, no `iiextmetadatalanguage`). Every file was found on Commons (`imagerepository` `local` there).
- 1988-02-13 and 2010-02-12 have no file: PageImages named no image, so the server sends no file request and there is no fixture.
- The Commons day categories (`Canada photographs taken on <date>`) were not recorded: day photos stay off until S3c (decision D7) and S3c T1 records them.

| File | `LicenseShortName` and `DateTimeOriginal` by file |
|---|---|
| `commons-files-1976-07-17.json` | `Flag of East Timor.svg`: Public domain, 2002-05-20. `1976 Summer Olympics logo.svg`: Public domain, 2024-03-17 |
| `commons-files-1980-04-12.json` | `Trans Brasil Boeing 727-100 PT-TYS.jpg`: GFDL 1.2, 1979-04. `TerryFoxToronto19800712.JPG`: Public domain, 1980-07-12 (not taken on 1980-04-12, research `canada-day-events.md` section 2) |
| `commons-files-1969-07-20.json` | `Apollo program.svg`: Public domain, 2017-03-05. `Flag of Honduras (1949–2022, 2026–present).svg`: Public domain, a prose date ("Adopted on March 7, 1866; …"). `Route of the 1969 Tour de France.png`: CC BY-SA 4.0, 2016-05-25 |

| File | Date | Requested files | Recorded (UTC) | Original URL |
|---|---|---|---|---|
| `commons-files-1976-07-17.json` | 1976-07-17 | `File:Flag_of_East_Timor.svg`, `File:1976_Summer_Olympics_logo.svg` | 2026-10-30 04:09:38 | `https://commons.wikimedia.org/w/api.php?action=query&titles=File%3AFlag_of_East_Timor.svg%7CFile%3A1976_Summer_Olympics_logo.svg&prop=imageinfo&iiprop=url%7Cmime%7Csize%7Cextmetadata&iiurlwidth=640&format=json&formatversion=2` |
| `commons-files-1980-04-12.json` | 1980-04-12 | `File:Trans_Brasil_Boeing_727-100_PT-TYS.jpg`, `File:TerryFoxToronto19800712.JPG` | 2026-10-30 04:09:39 | `https://commons.wikimedia.org/w/api.php?action=query&titles=File%3ATrans_Brasil_Boeing_727-100_PT-TYS.jpg%7CFile%3ATerryFoxToronto19800712.JPG&prop=imageinfo&iiprop=url%7Cmime%7Csize%7Cextmetadata&iiurlwidth=640&format=json&formatversion=2` |
| `commons-files-1969-07-20.json` | 1969-07-20 | `File:Apollo_program.svg`, `File:Flag_of_Honduras_(1949–2022,_2026–present).svg`, `File:Route_of_the_1969_Tour_de_France.png` | 2026-10-30 04:09:41 | `https://commons.wikimedia.org/w/api.php?action=query&titles=File%3AApollo_program.svg%7CFile%3AFlag_of_Honduras_%281949%E2%80%932022%2C_2026%E2%80%93present%29.svg%7CFile%3ARoute_of_the_1969_Tour_de_France.png&prop=imageinfo&iiprop=url%7Cmime%7Csize%7Cextmetadata&iiurlwidth=640&format=json&formatversion=2` |

## Wikidata signals (`wikidata-signals-<date>.json`)

- Request: the Wikidata Query Service query of `SparqlWikidataSignalSource.query` (tragic classes from `wikimedia.wikidata-tragic-classes`: Q3839081, Q171558, Q2223653, Q350604), `format=json`, for the QIDs of the same main links as the lead image lookups (the link lookups above give the QIDs).
- Results: `P17` is Canada (Q16) for Q9658 (1988 Winter Olympics), Q8444 (1976 Summer Olympics) and Q9674 (2010 Winter Olympics). Q7833816 (Transbrasil Flight 303) has the tragic class Q171558, 55 deaths and Brazil (Q155). Q295053 (Terry Fox) has no `P17`. Q27628137 (1980 Liberian coup d'état) has Liberia (Q1014) and no tragic class. Q836884 (1969 Tour de France) has five countries.
- Seconds: the `curl` total time of each request; all are well under `wikimedia.wikidata-read-timeout` (5s).

| File | Date | Queried items | Recorded (UTC) | Seconds | Original URL |
|---|---|---|---|---|---|
| `wikidata-signals-1988-02-13.json` | 1988-02-13 | Q9658 | 2026-10-30 04:08:43 | 0.47 | `https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3FtragicClass%20%3Fdeaths%20%3Fcountry%20WHERE%20%7B%20VALUES%20%3Fitem%20%7B%20wd%3AQ9658%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP31%2Fwdt%3AP279%2A%20%3FtragicClass%20.%20VALUES%20%3FtragicClass%20%7B%20wd%3AQ3839081%20wd%3AQ171558%20wd%3AQ2223653%20wd%3AQ350604%20%7D%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP1120%20%3Fdeaths%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP17%20%3Fcountry%20%7D%20%7D` |
| `wikidata-signals-1976-07-17.json` | 1976-07-17 | Q574 Q8444 | 2026-10-30 04:08:47 | 1.73 | `https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3FtragicClass%20%3Fdeaths%20%3Fcountry%20WHERE%20%7B%20VALUES%20%3Fitem%20%7B%20wd%3AQ574%20wd%3AQ8444%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP31%2Fwdt%3AP279%2A%20%3FtragicClass%20.%20VALUES%20%3FtragicClass%20%7B%20wd%3AQ3839081%20wd%3AQ171558%20wd%3AQ2223653%20wd%3AQ350604%20%7D%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP1120%20%3Fdeaths%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP17%20%3Fcountry%20%7D%20%7D` |
| `wikidata-signals-2010-02-12.json` | 2010-02-12 | Q9674 | 2026-10-30 04:08:49 | 0.56 | `https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3FtragicClass%20%3Fdeaths%20%3Fcountry%20WHERE%20%7B%20VALUES%20%3Fitem%20%7B%20wd%3AQ9674%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP31%2Fwdt%3AP279%2A%20%3FtragicClass%20.%20VALUES%20%3FtragicClass%20%7B%20wd%3AQ3839081%20wd%3AQ171558%20wd%3AQ2223653%20wd%3AQ350604%20%7D%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP1120%20%3Fdeaths%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP17%20%3Fcountry%20%7D%20%7D` |
| `wikidata-signals-1980-04-12.json` | 1980-04-12 | Q27628137 Q7833816 Q295053 | 2026-10-30 04:08:52 | 0.87 | `https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3FtragicClass%20%3Fdeaths%20%3Fcountry%20WHERE%20%7B%20VALUES%20%3Fitem%20%7B%20wd%3AQ27628137%20wd%3AQ7833816%20wd%3AQ295053%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP31%2Fwdt%3AP279%2A%20%3FtragicClass%20.%20VALUES%20%3FtragicClass%20%7B%20wd%3AQ3839081%20wd%3AQ171558%20wd%3AQ2223653%20wd%3AQ350604%20%7D%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP1120%20%3Fdeaths%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP17%20%3Fcountry%20%7D%20%7D` |
| `wikidata-signals-1969-07-20.json` | 1969-07-20 | Q46611 Q783 Q836884 | 2026-10-30 04:08:55 | 0.57 | `https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3FtragicClass%20%3Fdeaths%20%3Fcountry%20WHERE%20%7B%20VALUES%20%3Fitem%20%7B%20wd%3AQ46611%20wd%3AQ783%20wd%3AQ836884%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP31%2Fwdt%3AP279%2A%20%3FtragicClass%20.%20VALUES%20%3FtragicClass%20%7B%20wd%3AQ3839081%20wd%3AQ171558%20wd%3AQ2223653%20wd%3AQ350604%20%7D%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP1120%20%3Fdeaths%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP17%20%3Fcountry%20%7D%20%7D` |

## Synthetic fixtures (`synthetic/`, C1 S3a T2 to T4)

Written by hand by backend-dev on 2026-11-05, not recorded. They follow the field structure of the recorded answers above (`parse.tocdata.sections` with `hLevel` and `index`, `parse.wikitext`, `query.pages[].pageprops.wikibase_item`, `query.pages[].pageimage`). Every line is neutral invented text ("Test event A", "Test page A") or a short paraphrase of a public event; no real response is copied. The integrity test above reads only the top folder, so these files have no table row.

- `synthetic-<page>-tocdata.json`, `synthetic-<page>-events.json`: whole pages for the context tests. `09-17`·`1988` (an opening day with a date line and three year sub-lines), `06-22`·`2002` (one match on both pages, the year line through the redirect `2002 World Cup`), `03-15`·`1995` (a day without events), `02-29`·`2000` (the leap day: a level-3 `Events` and `Pronunciation` before the level-2 `Events`, as on the recorded 2010 page), `02-29-next` (a later revision of the leap day page, for the cache tests), `no-events-tocdata.json` (only a level-3 `Events` under `Births`, so no level-2 `Events` section).
- `synthetic-pageprops-<date>.json`, `synthetic-pageimages-<date>.json`: link and lead image lookups for those pages (`Q8470` and the other items of the opening day, `Q47735` with the redirect for 2002-06-22).
- `synthetic-missingtitle.json`, `synthetic-ratelimited.json`: the API error answers `missingtitle` (page absent) and `ratelimited`.
- `dedup/synthetic-<case>-{date,year}.wikitext`: line pairs for the merge rules D3 and D3m (`counterexample`: two lines that share only generic links; `one-link`: a line with one specific link on each page; `country-pair`: two matches between different country pairs, kept apart by the place rule).
- `parser/synthetic-<case>.wikitext`: one parser case per file (alias and bold links, references and comments, file links, sub-lines on date and year pages, unlinked dates, the four dash separators, templates, external links, keys that are not a year such as `44 BC`, HTML tags and entities, non-breaking spaces, ranges and a range whose end falls in the next year).
