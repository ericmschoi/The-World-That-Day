# Media schema examples

Examples for `data/schema/media.schema.json`, the shared media storage format from
`docs/plans/m3a3-event-media.md` (fields table, rules R1-R9). Plan M3a3 T1.

- `valid/` must pass the schema and, unchanged, the server normalization (T2 (6)).
- `invalid/` must fail the schema. Each file breaks exactly one rule, listed below with the field
  the validator error points at.
- The runtime does not read these files. Media files are never downloaded or copied; every
  address points to the original source.

## Schema notes

- JSON Schema 2020-12, `additionalProperties: false`. Every stored key is always present, and
  unknown values are `null`. API-only fields (`date_relation`, later `selection`) are not part of
  the storage format and must be removed before validation.
- Address fields (`source_url`, `url`, `thumbnail_url`, `embed_url`, `video_sources[].url`, and
  `license_url`) use only the pattern `^https?://\S+$` (R9). `format: uri` is not used, so
  addresses with non-ASCII characters are judged by the pattern alone. The server replaces a
  malformed `license_url` with `null` instead of dropping the record, so a stored `license_url` is
  always `http(s)` or `null`.
- Text fields (`id`, `source_name`, `attribution`, `render.provider`, and `provider`, `license`,
  `description` when not `null`) use `$defs.nonEmptyString`: blank means made
  only of characters in the whitespace set from `docs/plans/m3a3a-media-rules.md` (Java
  `Character.isWhitespace` or Unicode category `Z`, for example U+00A0 and U+2007), the same rule
  as the server `MediaNormalizer`. The pattern `[^\s\p{Z}\x1C-\x1F]` needs one character outside
  that set (in the validator's Java regex `\s` is U+0009-U+000D and U+0020), so text with inner
  Unicode spaces stays valid.
- `retrieved_at` uses `format: date-time` plus a UTC-to-the-second pattern. `place_ref` uses
  `^(world|unknown|[a-z]+:\S+)$`.
- `link_only_reason` is `null` for every mode except `link_only` (fields table: "only for
  link_only").
- The schema only checks that `provider` is a string. The allowed embed provider lists (R3, R5),
  the per-source blocklist and the excluded wire/news agency credits (R2) are server settings.
- Synthetic examples have ids starting with `example-`, use `example.com`/`example.org`/`example`
  addresses and provider names, and use only demo dates (1988-09-17, 2002-06-22), the boundary
  date 2000-02-29, or public-event dates allowed by M3-P13 (`CLAUDE.md` 7.6):
  `example-video-embed.json` keeps `captured_at: "1986"` from the M3a5 contract example (the 1986
  Asian Games opening, a public event). The embed examples use the provider `example`, which the T2
  tests add to the allowed lists.

## valid/

| File | What it shows | Source |
|---|---|---|
| `commons-1988-09-17-seoul-olympics-opening-pd.json` | Real Commons photo, `image`, `Public domain` → `free`, captured 1988-09-17 (`day`). `date_relation` for request date 1988-09-17 is `{"kind":"same_day","years":0}` | https://commons.wikimedia.org/wiki/File:1988_Summer_Olympics_opening_ceremony.JPEG — checked 2026-10-13 |
| `commons-2002-06-22-gwangju-world-cup-kogl1.json` | Real Commons photo, `image`, `KOGL Type 1` → `free`, captured 2002-06-22 (`day`). `date_relation` for request date 2002-06-22 is `{"kind":"same_day","years":0}` | https://commons.wikimedia.org/wiki/File:2002%EB%85%84_%ED%95%9C%C2%B7%EC%9D%BC_%EC%9B%94%EB%93%9C%EC%BB%B5-%EB%8C%80%ED%95%9C%EB%AF%BC%EA%B5%AD%C2%B7%EC%8A%A4%ED%8E%98%EC%9D%B8_8%EA%B0%95_%EA%B2%BD%EA%B8%B0-%EA%B2%BD%EA%B8%B0%EC%9E%A5_%EC%A0%84%EA%B2%BD.jpg — checked 2026-10-13 |
| `example-photo-license-unknown.json` | Synthetic. `license: null`, `license_status: unknown`, attribution "작가 정보 없음 · 라이선스 확인 안 됨 · …", still `display_mode: image` | synthetic |
| `example-photo-capture-time-unknown.json` | Synthetic. `captured_at: null`, `date_precision: null` (shown as "촬영 시점 미상", `date_relation.kind = unknown`, never as "same day") | synthetic |
| `example-photo-approx.json` | Synthetic. `aerial`, `approx` with `captured_at: "1988"` (R8), location by `lat`/`lng` with `place_ref: null` | synthetic |
| `example-image-embed.json` | Synthetic. `image_embed` with `embed_url`, `provider`, `url`/`thumbnail_url` null, `license: provider-terms` → `other` (R3) | synthetic |
| `example-video-file.json` | Synthetic. `video_file` with two `video_sources` (smaller first), `month` precision `1988-09` (R4) | synthetic |
| `example-video-embed.json` | Synthetic. `video_embed` in the same shape as the M3a5 contract example, without `selection` and `date_relation`, plus the stored location and `retrieved_at` keys (R5) | synthetic |
| `example-sdk.json` | Synthetic. `streetview`, `sdk` with `render`, `current` precision and `captured_at: null` (R6, R8) | synthetic |
| `example-link-only.json` | Synthetic. `link_only` with `link_only_reason: hotlink_blocked`, no `url`/`thumbnail_url`/`embed_url` (R7). M3 does not produce this mode for Commons | synthetic |

### How the two real examples were filled

Values come from the Commons file description pages and the Commons API
(`action=query&prop=imageinfo&iiprop=url|size|extmetadata&iiurlwidth=640`) for the same files,
read on 2026-10-13 with the tool's default User-Agent. Nothing was downloaded.

| Field | 1988-09-17 file | 2002-06-22 file |
|---|---|---|
| `id` | `pageid` 4930021 | `pageid` 129150318 |
| `url` | imageinfo `url` | imageinfo `url` |
| `thumbnail_url`, `thumbnail_width`, `thumbnail_height` | imageinfo `thumburl` (960px bucket on `thumb.wikimedia.org`, as returned), `thumbwidth` 640, `thumbheight` 955 | `thumburl` (same form), 640, 422 |
| `license` | `LicenseShortName` "Public domain" (file page: PD-USGov-Military-Air Force, "No Copyright - United States") | `LicenseShortName` "KOGL Type 1" (file page: "This file is licensed under the Korea Open Government License Type 1.") |
| `license_url` | none in extmetadata → `null` | `LicenseUrl` http://www.kogl.or.kr/info/licenseType1.do |
| `attribution` | `Artist` "Ken Hackman, U.S. Air Force" · license · Wikimedia Commons | `Artist` "문덕관 기증" · `Credit` "국립민속박물관 민속아카이브" · `UsageTerms` "공공누리 제1유형 출처 표시" (KOGL Type 1) · Wikimedia Commons |
| `description` | Korean rendering of `ImageDescription` "An ornately dressed Korean holds a flag in a ceremony at the XXIVth Olympiad", placed in the category "1988 Summer Olympics opening ceremony" | `ImageDescription` (Korean, quoted without the film note "35mm 칼라 포지티브 필름.") |
| `captured_at`, `date_precision` | `DateTimeOriginal` "Taken on 17 September 1988" → `1988-09-17`, `day` | `DateTimeOriginal` "Taken on 22 June 2002" → `2002-06-22`, `day` |
| `place_ref` | `country:KR` (finding aid: "Base: Seoul Country: Republic Of Korea (KOR)"); no GPS → `lat`/`lng` null | `country:KR` (Gwangju World Cup Stadium); no GPS → `lat`/`lng` null |
| `retrieved_at` | 2026-10-13T23:51:32Z (time of the check) | same |

Notes:

- The capture date comes from each file's own `DateTimeOriginal`, not from the hidden date
  categories ("South Korea photographs taken on …"), which the plan forbids as a capture-date
  source.
- 1988 file: the National Archives ingest block on the file page also shows the date
  "1 September 1988" (the page does not say what that date is based on), while the caption `Date`, the
  structured data inception and `DateTimeOriginal` all say 17 September 1988, the day of the
  opening ceremony. The example follows `DateTimeOriginal`, the parser input the plan names.
- The `utm_source`/`utm_campaign`/`utm_content` tracking query that the API appends to `url` and
  `thumburl` was removed; the rest of each address is exactly as returned.
- Neither file is a wire or news agency photo: the 1988 photographer is a U.S. Air Force service
  member (DoD Defense Imagery, DF-ST-90-05029), the 2002 photo is a donation to the National Folk
  Museum of Korea archive.

## invalid/

Each file is a copy of a valid example with one change.

| File | Breaks | Change | Error points at |
|---|---|---|---|
| `r1-empty-source-url.json` | R1 (empty source field → not shown) | `source_url: ""` | `/source_url` |
| `r1-blank-attribution.json` | R1 (empty source field → not shown) | `attribution: "   "` (whitespace only) | `/attribution` |
| `r1-nbsp-only-attribution.json` | R1 (empty source field → not shown) | `attribution: "\u00a0\u2007"` (no-break spaces only, U+00A0 and U+2007) | `/attribution` |
| `r1-null-source-name.json` | R1 (empty source field → not shown) | `source_name: null` | `/source_name` |
| `r1-no-location.json` | R1 (location is `lat`/`lng` or `place_ref`) | `lat`, `lng`, `place_ref` all `null` | `/lat`, `/lng`, `/place_ref` |
| `r1-lat-without-lng.json` | R1 (`lat` and `lng` together) | `lat` set, `lng: null` (`place_ref` still set) | `/lat`, `/lng` |
| `r2-image-without-thumbnail.json` | R2 (`image` needs `url` and `thumbnail_url`) | `thumbnail_url: null` | `/thumbnail_url` |
| `r3-image-embed-with-thumbnail-url.json` | R3 (`image_embed` has `url` and `thumbnail_url` null) | `thumbnail_url` set to a direct image address | `/thumbnail_url` |
| `r4-video-file-without-sources.json` | R4 (`video_file` needs `url` or `video_sources`) | `url: null`, `video_sources: null` | `/url`, `/video_sources` |
| `r5-video-embed-without-embed-url.json` | R5 (`video_embed` needs `embed_url` and `provider`) | `embed_url: null` | `/embed_url` |
| `r6-sdk-without-render.json` | R6 (`sdk` needs `render` and `provider`) | `render: null` | `/render` |
| `r7-link-only-without-reason.json` | R7 (`link_only` needs `link_only_reason`) | `link_only_reason: null` | `/link_only_reason` |
| `r8-day-precision-with-year.json` | R8 (`day` → `YYYY-MM-DD`) | `captured_at: "1988"` with `date_precision: day` | `/captured_at` |
| `r8-unknown-precision-with-date.json` | R8 (`null` precision → `captured_at` null; capture time unknown is never shown as a date) | `captured_at: "1988-09-17"` with `date_precision: null` | `/captured_at` |
| `r9-javascript-thumbnail-url.json` | R9 (http/https only) | `thumbnail_url: "javascript:alert(1)"` | `/thumbnail_url` |
| `r9-protocol-relative-embed-url.json` | R9 (http/https only) | `embed_url: "//player.example.com/…"` | `/embed_url` |
| `r9-ftp-video-source-url.json` | R9 (http/https only) | `video_sources[0].url` starts with `ftp://` | `/video_sources/0/url` |
| `api-only-date-relation.json` | `additionalProperties: false` (API-only field in the storage format) | adds `date_relation` | root (`date_relation` not allowed) |

Checked again on 2026-10-13 after the whitespace-set change (plan M3a3a T2b) with
`com.networknt:json-schema-validator` 3.0.6 configured like the test helper `DataSchemas` (2020-12,
remote fetching off, format assertions on): the schema passes the
2020-12 meta-schema, every `valid/` file passes, and every `invalid/` file fails with errors at
the field listed above, sometimes also at that field's parent path (for example
`r9-ftp-video-source-url.json` also reports a `oneOf` error at `/video_sources`). Only
`api-only-date-relation.json`, whose listed location is the root, has a root-level error.
