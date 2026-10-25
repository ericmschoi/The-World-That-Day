# Commons 동영상 자동 검색과 결과 캐시 (M3a4b)

> 브랜치: `feat/commons-video` (`main` 298cb8e — PR #23 M3a4 머지 뒤 — 에서 갈라짐) · 작성: planner · 상태: **계획 개정 PM 승인 (2026-10-24): 결정 1 (a) 두 PR — M3a4b 어댑터(T0~T5) → M3a4c `feat/media-search-cache` 결과 캐시(T6~T9), M3 PR 22개 · 2 (a) Wikidata 질의는 어댑터가 따로 1회 · 3 (b) ★ T6은 backend-dev · 4 (a) 보조 경로는 후속 후보** — 이 브랜치는 A(T0~T5)만 한다. B(T6~T9)는 M3a4b 머지 뒤 M3a4c를 시작할 때 이 계획서에서 따로 떼어 낸다 (처음 승인 2026-10-12, M3a4 분할로 생김 — 로드맵 D1 (a)). 2026-10-24 개정은 머지된 M3a4 코드 이름 대조, PR #23 리뷰 권장 2·3(M3a4 결정 5 (a)로 넘어옴), 다시 셈과 분할안이다 — 아래 "이번 개정에서 바뀐 것".
> 기준: `docs/research/video-sources.md` 1절 기능 표, 3절 Commons 카드·3.1절·종합 경로 표·하루 처리량, 4절 Commons 열, 5절 V1 표, 6.1절, 7절 최종 추천·V1 추천·R-V1~R-V7, 8절 위험 2·4·5·6, 10.1절 1·2·3·4·21번, `docs/plans/m3a4-event-video.md`(검색 틀·정확도 규칙·결정 1~5), `docs/plans/m3a3b-commons-photos.md`(Commons 파일 매퍼·녹화 방식), `docs/plans/m3a3c-event-photos.md`(재시도 없음·읽기 타임아웃), `docs/plans/m3a1b-wiki-section-cache.md`(캐시 방식), `CLAUDE.md` 7절 1·2·3·5·6항, 제품 정의서 6.1·6.3·12절
> 선행: **M3a4 머지됨 (PR #23, `main` 298cb8e)**, M3a3b(Commons 파일 매퍼, 완료). **키가 필요 없다.** 리서치 6.1절 핫링크 `curl` 2개(P0)는 선택이고 이 기능을 막지 않는다.
> 뒤따르는 기능: M3a5(YouTube)가 이 기능의 결과 캐시를 쓴다(`media.search-cache.ttl.youtube`).

## 이번 개정에서 바뀐 것 (2026-10-24, `main` 298cb8e 코드 대조)

| 항목 | 전 (2026-10-12) | 지금 | 이유 |
|---|---|---|---|
| 선행 | "M3a4 `feat/event-video` 머지" | M3a4 머지(PR #23, 298cb8e) | 머지 이름으로 다시 씀(아래 "이어받는 계약") |
| PR #23 리뷰 권장 2·3 | — | **새 T0**(첫 어댑터 연결 전): 출처별 판정 격리 + `Candidate`·`SourceResult` 입력 검사, 낱말 패턴 재사용 | M3a4 결정 5 (a). 어댑터가 잘못된 후보를 주면 지금은 판정 예외가 `CommonsMediaLookup.lookup` 밖으로 나가 세계 API가 500이 된다 |
| `notAfter` 자리 | "어댑터가 정함" | **`CommonsFileMapper` 의 영상 매핑**이 `DateTimeOriginal` 파서 값에 `VideoAccuracy.notAfter(…, 요청 날짜)` 를 적용해 `captured_at` 을 만들고, 파서 원래 값은 `Candidate.originalCapture` 로 따로 넘긴다 | 지금 매퍼는 `DateTimeOriginal` 을 안에서 읽어 바로 레코드를 만든다(사진 전용 `PHOTO`/`IMAGE`) |
| `Semaphore(3)` 자리 다툼 | 리스크로만 | 요청 수와 기다림 단계를 줄인다: **변환본 요청을 없앤다**(검색·범주·파일 묶음 요청 자체를 `prop=videoinfo` + `derivatives` 로), 어댑터 안 요청을 **두 단계 병렬**(Wikidata ∥ 날짜 범주 → P10 파일 묶음 ∥ 사건 범주들). 새 날짜 요청 최대 7 → **6**, 기다림 단계 3 → **2** | `WikimediaHttpClient` 의 동시 요청 3개(`wikimedia.max-concurrent-requests`)는 Wikidata·위키백과·Commons가 모두 나눠 쓴다. 영상 출처는 사진 두 경로와 같은 `media.lookup-deadline`(3초) 창 안에 있다 |
| 보조 경로 (b-2)·(b-3) (전 T4, 크기 조건부) | "크기가 되면" | **범위 밖 → 후속 후보**(로드맵 "Commons 보조 경로") | 다시 셈이 400을 크게 넘는다(아래 "크기") — 원래 조건대로 뺀다. PM 결정 4 |
| 결과 캐시 위치 | "Wikidata 조회 결과도 같은 줄" | 출처 공통 흐름을 `VideoSearch` 쪽에 두고, 줄은 (출처, 날짜, 사건 ID, `dayPathsOff`)마다. **판정 전 후보**를 저장하고 읽을 때 다시 판정·다시 거른다 | 낱말 목록·기준선을 바꿔도 캐시를 비울 필요가 없고(M3a1b가 위키텍스트 원문만 저장한 것과 같은 생각), 차단 목록이 캐시된 항목에도 듣는다(안전장치 5) |
| Flyway 번호 | V3 | **V3 확인**(`db/migration` 에 V1·V2만) | — |
| 크기 | 245 → 보정 390 | 파일별 약 648 → **×1.7 약 1,100 (860~1,360)** → 분할안(PM 결정 1) | M3a4 실측 ×2.1, M3a3d ×1.33 |

## 목표

1. M3a4 검색 틀(`EventVideoSource`)에 첫 어댑터 **Commons 동영상**(키 없음)을 붙인다. 경로는 리서치 7절 최종 추천대로 **(a-3) 사건 QID `P10` → (a-1) 사건 범주 직속 → (c-1) `Videos taken on YYYY-MM-DD` 직속**. 재생은 `upload.wikimedia.org` 원본·변환본 핫링크(`video_file`).
2. 사건 QID의 `P10`·`P373`·`P910` 사이트링크·영어 라벨을 Wikidata 질의 한 번으로 받아 `SourceResult.englishLabels` 로 넘긴다(정확도 규칙 R-T2 — `VideoSearch` 가 `EventContext.withEnglishLabel` 로 씀).
3. 출처 공통 **결과 캐시**(Flyway V3): 출처별 TTL, 0건도 캐시, 실패·시간 초과·쿼터 건너뜀은 캐시 안 함.
4. 확인용 기준표(`data/verification/event-media-expectations.json`)를 만든다 — M3a5·M3a5b 품질 확인에도 같은 표.
5. 연결 전에 PR #23 리뷰 권장 2·3을 닫는다(T0).

## 범위 밖

- 보조 경로 (b-2) 영어 라벨 검색어·(b-3) `insource:` 날짜 문자열 → 후속 후보(PM 결정 4)
- `deepcat:` (a-2)·(c-2) — 영상 1번 (a)
- 핫링크가 막혔을 때 `link_only` 로 두기 — 레코드를 만들지 않고 보고한다(리서치 4절 결론)
- YouTube·대한뉴스 → M3a5·M3a5b
- 파일 내려받기·서버 경유 재생(안전장치 1). 영상 파일·포스터 JPEG는 녹화하지도 않는다(메타데이터 JSON만)
- 사진 메타데이터 캐시(로드맵 후속 후보 — 이 캐시는 영상 출처만)
- 오래된 캐시 줄 정리 작업(만료 줄은 다음 쓰기에서 덮어씀). YouTube의 30일 삭제 의무는 M3a5가 다룬다
- 다른 날짜로 영상을 찾아 보이게 하는 것 — 하지 않는다(PM 피드백 2026-10-16). 데모 날짜에 Commons 영상이 없는 것이 정상이다

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| Wikimedia Commons Action API | **확인됨** (video-sources.md 3절 Commons 카드, 1절 표, 종합 경로 표 "지금 사용") | `generator=search`·`generator=categorymembers`·`titles=` + `prop=videoinfo`(`viprop=url\|size\|mime\|mediatype\|extmetadata\|derivatives`) | 키 없음. User-Agent `WIKIMEDIA_USER_AGENT` — 연락처는 **저장소 주소만**(`https://github.com/ericmschoi/The-World-That-Day`), 개인 이메일 금지(녹화 `curl` 포함). 분당 200·동시 3(`WikimediaHttpClient` 공용 자리). 한 번만 보냄(재시도 없음, M3a3c 결정 1 (a)), 읽기 타임아웃 `media.lookup-deadline`. 썸네일은 API가 준 `thumburl` 만(위험 4). **파일은 복사하지 않고 핫링크**. 메타데이터·점수만 캐시 |
| Wikidata Query Service | **확인됨** (day-events.md, M3a2 클라이언트, 리서치 3.1절 (a-0)) | 사건 QID → `P10`(동영상 파일), `P373`(범주 이름), `P910` 항목의 commonswiki 사이트링크, 영어 라벨 | 질의 60초, IP당 동시 5, 429 처리(`WikimediaHttpClient`). 날짜 하나의 대상 사건 QID(최대 3개)를 질의 1회로 묶는다. CC0 |

메모: 리서치는 `P10` 을 M3a2 SPARQL에 더하면 추가 요청이 0이라고 적었다. 이 계획은 **어댑터가 따로 질의 1회**를 하는 안을 추천한다 — PM 결정 2.

## 이어받는 계약 (`main` 298cb8e에서 확인)

| 부품 | 실제 이름·모양 | 이 기능에서 |
|---|---|---|
| 출처 인터페이스 | `media.video.EventVideoSource`: `String key()`, `SourceResult find(LocalDate date, List<EventContext> events, boolean dayPathsOff)`. `record SourceResult(Map<String, List<Candidate>> byEvent, Map<String, String> englishLabels, Status status)` — 지도는 복사하지만 **안쪽 목록은 복사하지 않음**. `enum Status { OK, FAILED, QUOTA_SKIPPED }` | Commons 어댑터가 구현(key `commons`). T0이 안쪽 목록 복사·`null` 거부 |
| 검색 | `media.video.VideoSearch`(`@Component`, 출처 빈을 `ObjectProvider<EventVideoSource>.orderedStream()` 으로 받음): `Running start(Deadline, LocalDate, List<EventTarget>, boolean dayPathsOff)` — 앞 `media.video.max-events` 개를 `EventContext.of(target, date)` 로 만들고, 출처마다 `deadline.start("video-" + key, …)`. `Running.await()` → `Videos(Map<String, MediaItem> byEvent, boolean failed)`. **판정(`accuracy.judge`)은 `await` 를 부른 스레드에서 돌고 감싸여 있지 않다** | T0 판정 격리, T7 캐시 흐름 |
| 사건 | `media.video.EventContext(eventId, date, title, englishLabel, summary, wikidataQid, placeRef)`, `of(EventTarget, LocalDate)`(라벨 `null`), `withEnglishLabel(String)` | 그대로 |
| 대상 | `media.MediaLookup.EventTarget(eventId, title, placeRef, summary, wikidataQid)` — 동등성은 앞 세 필드만(M3a4 결정 5 (a)). `WorldStoryService` 가 `normal` 이고 대표 링크가 있는 사건만 대상으로 만든다. `wikidataQid` 는 대표 링크 항목(`KoWikiEvent.wikidataQid()`) | 그대로 |
| 후보 | `media.video.Candidate(MediaItem item, String title, String description, CaptureTime originalCapture, List<LocalDate> explicitDates, EventLink eventLink, boolean official, boolean dateTextExcluded, Selection.Basis basis)`. 지금 검사: `item`·`eventLink`·`basis` 비면 예외, `originalCapture` `null` → `UNKNOWN`, `explicitDates` `null` → 빈 목록. **`originalCapture` 모양·`explicitDates` 원소는 검사 안 함**. `EventLink { P10, CATEGORY_DIRECT, NONE }` | T0 입력 검사 |
| 판정 | `media.video.VideoAccuracy`(`@Component`): `Judgement judge(EventContext, Candidate)`, `Judgement(int score, Reject reject)`, `Reject { NO_MATCH, BASIS_RULE, DURATION, LOW_SCORE, LATE_CAPTURE }`, `static CaptureTime notAfter(CaptureTime, LocalDate)`. 영어 핵심어 대조의 `wholeWord(key)` 가 **후보마다 키마다 `Pattern.compile`** | T0 패턴 재사용. 매퍼가 `notAfter` 를 씀 |
| 경로 값 | `media.Selection(Basis basis, int score)`, `Basis { COMMONS_P10, COMMONS_CATEGORY, COMMONS_DATE_CATEGORY, COMMONS_SEARCH, YOUTUBE_OFFICIAL, DAEHAN_NEWS }` | 앞 셋을 씀(`COMMONS_SEARCH` 는 보조 경로 — 이번엔 안 씀) |
| 미디어 조회 | `media.commons.CommonsMediaLookup.lookup(LocalDate, List<EventTarget>, boolean tragicDay)` — `try (Deadline deadline = Deadline.after(properties.lookupDeadline()))` 안에서 날짜 범주(`"commons"`)·사건 사진(`"media"`) 작업을 올린 **뒤** `videos.start(deadline, date, targets, tragicDay)`, 사진 둘을 `await` 한 뒤 `search.await()`. 사건 `media` = 사진 뒤에 영상 | 그대로 |
| 마감 | `wikimedia.Deadline implements AutoCloseable`: `after`, `start(name, Callable)`, `Task.await()` → `Done`/`TimedOut(stopped, late)`/`Failed(cause)`/`Interrupted()`, `CANCEL_GRACE` 1초, `close()` 가 끝나지 않은 작업을 인터럽트 | 어댑터 안 병렬에도 쓸 수 있다(T3) |
| HTTP | `wikimedia.WikimediaHttpClient.getOnce(source, title, uri, readTimeout)` — 공정 `Semaphore(wikimedia.max-concurrent-requests = 3)` 를 **모든 Wikimedia 호스트가 나눠 씀**, 자리 기다림은 `wikimedia.read-timeout`(4초)까지지만 마감 인터럽트로 먼저 끝남. 실패 WARN `Wikimedia call failed: source= title= cause=` | 새 요청은 모두 `getOnce` + 고정 이름표 |
| Commons 사진 클라이언트 | `media.commons.CommonsClient`(`dayCategory`, `files`, `MAX_FILES` 50) / `ActionApiCommonsClient` — `prop=imageinfo&iiprop=url\|mime\|size\|extmetadata&iiurlwidth=640` 이 고정 | 영상은 **따로 나눈 클라이언트**(사진 시험의 가짜 `CommonsClient` 가 바뀌지 않게). `wikimedia.ActionApiErrors` 공용 |
| 파일 매퍼 (M3a3b) | **`media.commons.CommonsFileMapper`**: `Result map(JsonNode page, Predicate<String> mime, String placeRef, LocalDate date)` — `imageinfo[0]`, `PHOTO`/`IMAGE` 고정, `DateTimeOriginal` → `CaptureTimeParser.parse`, `MediaFilter.check(…, new MediaOrigin("commons", 파일 이름, artist, credit, null))` → `MediaNormalizer.normalize`. `Drop { MISSING, NON_FREE, MIME, FILTERED, RULE }`, `SOURCE_NAME` | 영상 매핑을 더한다(T2). 사진 결과는 그대로 |
| 촬영 시점 | `media.CaptureTimeParser.parse(String)`(문자열 전체가 한 모양 — "Taken on …" 접두는 뗌), `media.TextDates.find(String)`(글 속 날짜 — 판정이 제목·설명에서 씀), `media.CaptureTime(capturedAt, datePrecision)` — **모양 검사 없음** | T0이 모양 검사 도우미 |
| 설정 | `media.video.*` = `media.video.VideoSearchProperties`(`min-score` 60, `max-events` 3, `max-duration-seconds` 600, 낱말 목록 셋). `media.lookup-deadline` 3s = `media.MediaLookupProperties`. `wikimedia.*` = `WikimediaProperties`·`CommonsProperties`(`commons-api-url`)·`WikidataProperties`(`wikidata-sparql-url`, `QID` 패턴) | 새 `media.search-cache.ttl.*`(T6). `media.video` 에 새 키 없음 |
| 캐시 선례 (M3a1b) | `V2__wiki_section_cache.sql`, `WikiSectionCache` 엔티티, `WikiSectionCacheRepository.upsert`(`ON CONFLICT … DO UPDATE`), `CachingKoWikiClient`(DB 실패는 캐시만 잃음, 신선 = `fetched_at + ttl > now`) | 같은 방식 |
| Flyway | `server/src/main/resources/db/migration`: `V1__baseline.sql`, `V2__wiki_section_cache.sql` | **V3** |
| 시험 기반 | `WikimediaWireMockTest`: 경로 `KOWIKI_PATH`·`SPARQL_PATH`·`COMMONS_PATH`, 기본 Commons 스텁 `{"batchcomplete":true}`. 세계 API 시험 여러 개가 경로만으로 SPARQL·Commons를 스텁하고 **경로별 요청 수를 센다**(`WorldStoryDemoTest` 등). `WorldStoryNoVideoSourceTest` 는 "출처 빈 없음"(`sources.stream()` 빔)을 단언 | T3 (8) |

## 경로와 요청 (Commons 어댑터)

| 단계 | 경로 | 요청 | `EventLink` / `basis` | 규칙 |
|---|---|---|---|---|
| 1 | Wikidata 연결 | SPARQL 1회(날짜당, 대상 사건 QID 묶음): `P10`, `P373`, `P910`→commonswiki 사이트링크, `rdfs:label@en` | - | QID 모양이 아닌 값은 뺌, 없으면 요청 0 |
| 1 | (c-1) 날짜 범주 직속 | `generator=categorymembers&gcmtitle=Category:Videos taken on YYYY-MM-DD&gcmtype=file&gcmlimit=20` + 영상 정보 (날짜당 1, 사건끼리 공유) | `NONE` / `COMMONS_DATE_CATEGORY` | 사건어 일치 필수(M3a4 필수 조건 2). `dayPathsOff` 면 요청 없음 |
| 2 | (a-3) 사건 QID `P10` | `titles=File:…\|File:…` + 영상 정보 (묶음 1) | `P10` / `COMMONS_P10` | 사건 연결 +50 |
| 2 | (a-1) 범주 직속 | `generator=search&gsrnamespace=6&gsrlimit=10&gsrsearch=incategory:"<범주>" filetype:video` + 영상 정보 (사건당 1). 범주는 `P373`, 없으면 `P910` 사이트링크(R-V2) | `CATEGORY_DIRECT` / `COMMONS_CATEGORY` | 직속만. 사건 연결 +50 |

- "영상 정보" = `prop=videoinfo&viprop=url|size|mime|mediatype|extmetadata|derivatives`, 썸네일 폭은 사진과 같은 640(API가 준 `thumburl` 그대로), `format=json&formatversion=2`, 언어 기본값(사진과 같음 — 한국어 `DateTimeOriginal` 을 피함). `extmetadata` 는 비싸므로 `viextmetadatafilter` 로 매퍼가 읽는 키만 받아도 된다.
- 변환본(`derivatives`)은 위 요청에 함께 온다 — **따로 요청하지 않는다**. 녹화(T2)로 확인하고, 생성기 응답에 `derivatives` 가 없으면 고른 파일 묶음 요청 1회를 더하고 보고한다(최대 7).
- 새 날짜 하나당 요청: Wikidata 1 + 날짜 범주 0~1 + P10 묶음 0~1 + 범주 0~3 = **최대 6**, 기다림 단계 2. 분당 200이라 하루 한도 제약이 아니다(리서치 하루 처리량). `tragic` 사건은 대상이 아니라 그 QID로 요청 0, `tragic` 사건이 있는 날(또는 위키백과 문서 하나가 `UNAVAILABLE`)은 날짜 범주 요청 0.
- 같은 3초 창: 사진 경로(날짜 범주 1, PageImages 1 → 파일 묶음 1)와 합쳐 최대 9요청이 자리 3개를 나눠 쓴다. 공정 세마포어라 먼저 온 순서다. 끝 시각에 못 끝난 출처만 비고 `partial: true` 다(M3a4 그대로). 실제 시간은 T5에서 본다(리스크 1).
- 요청 하나라도 실패하면 그 출처는 `FAILED`(빈 결과, 캐시 안 함) — 다음 요청에서 다시 찾는다.

## 미디어 레코드 (리서치 4절 Commons 열)

| 필드 | 값 |
|---|---|
| `id` | `commons-` + `pageid` |
| `type` / `display_mode` | `video` / `video_file` |
| `url` | `videoinfo.url`(원본, API 값 그대로) |
| `thumbnail_url`, `thumbnail_width`·`thumbnail_height` | `thumburl`, `thumbwidth`·`thumbheight` |
| `video_sources` | `derivatives[]` → `{url: src, mime: type 의 ";" 앞(코덱 매개변수 뗌), height}`. `height` 작은 것부터, 같은 높이면 `video/webm` 앞·`video/quicktime` 뒤. 원본 MIME이 `video/` 로 시작하면 원본을 끝에 하나 더한다. `application/ogg` 원본(예 `AP11 FINAL APPROACH.ogv`)은 `url` 에만 둔다(`MediaNormalizer` 의 `video/…` 규칙) |
| `duration_seconds` | `duration` (M3a4 판정이 600초 넘거나 없으면 `DURATION` 으로 버림) |
| `embed_url`·`provider`·`render`·`link_only_reason` | `null` |
| `source_name` / `source_url` | `Wikimedia Commons` / `descriptionurl` |
| `license`·`license_url`·`license_status` | `extmetadata.LicenseShortName`·`LicenseUrl`, 상태는 `LicenseClassifier`(사진과 같음) |
| `attribution` | 사진과 같음(작가 HTML 제거, 없으면 `Credit`, 둘 다 없으면 "작가 정보 없음" + 라이선스 + `Wikimedia Commons`) |
| `description` | `extmetadata.ImageDescription` (HTML 제거, 200자) |
| `captured_at`·`date_precision` | `DateTimeOriginal` → `CaptureTimeParser` → **`VideoAccuracy.notAfter(…, 요청 날짜)`** — 사건 날짜보다 늦으면 `null`(R-V3, 화면 "촬영 시점 미상"). `extmetadata.DateTime`(업로드 시각)은 읽지 않는다. 사건 날짜를 `captured_at` 으로 옮기는 경로는 없다 |
| 위치 | 좌표(`GPSLatitude`·`GPSLongitude`)가 있으면 사용, 없으면 사건의 `place_ref` |
| `retrieved_at` | 조회 시각(캐시에서 읽어도 처음 조회 시각 그대로) |

- `mediatype` 이 `VIDEO` 인 파일만 쓴다(소리만 있는 `AUDIO` ogg 등은 버림).
- 통신사 표기(`Associated Press` 등) 파일은 `MediaFilter` 가 뺀다(사진과 같은 경로). 자유 라이선스 언론사 영상(예 뉴스타파)은 허용, 권리자 요청 시 `media.blocklist`(영상 4번 (a)).
- 판정 재료(매퍼가 함께 돌려줌): 파일 제목(`File:` 뗀 것), 설명 평문(자르기 전), 파서 원래 값(`notAfter` 전 — `originalCapture`).
- 핫링크 재생이 막힌 것이 확인되면(P0이 403·429) `link_only` 를 만들지 않고 PM에게 보고한다.

## 결과 캐시 (Flyway V3)

```sql
-- 예시 (이름·타입은 T6에서 정하고 보고)
CREATE TABLE event_media_search_cache (
    id            BIGSERIAL    PRIMARY KEY,
    source        VARCHAR(32)  NOT NULL,   -- 'commons' (뒤에 'youtube', 'daehan_news')
    event_date    DATE         NOT NULL,
    event_id      VARCHAR(64)  NOT NULL,   -- 'kowiki-<날짜>-<8자리>' (M3a1c)
    day_paths_off BOOLEAN      NOT NULL,   -- 날짜 단위 경로를 껐는지 (같은 날도 결과가 다름)
    result        TEXT 또는 JSONB NOT NULL, -- 판정 전 후보 + 필터 입력 + 영어 라벨. 메타데이터만, 파일 아님
    fetched_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_event_media_search_cache UNIQUE (source, event_date, event_id, day_paths_off)
);
```

- 신선함은 읽을 때 `fetched_at + media.search-cache.ttl.<출처> > now`(M3a1b와 같음). 설정: `media.search-cache.ttl.commons: 7d`. TTL이 없는 출처는 캐시하지 않는다(M3a5가 `youtube` 를 더하고 30일 상한을 검사). 음수는 시작 실패(오류에 설정 이름), `0` 은 끔.
- **0건도 캐시**한다. **`FAILED`·`QUOTA_SKIPPED`·예외·시간 초과·`Interrupted` 는 캐시하지 않는다.**
- 일부 사건만 신선하면 없는 사건만 출처에 묻는다. 모두 신선하면 그 출처 요청 0.
- 읽을 때: 다시 판정(`VideoAccuracy`), 다시 거름(`MediaFilter` 차단 목록·통신사 제외, `MediaNormalizer` R1~R9·`date_relation`). 읽을 수 없는 줄은 없는 것으로 보고 다시 찾아 덮어쓴다.
- 쓰기는 마감 작업 밖(요청 스레드, `Running.await()` 뒤)에서 — 마감 인터럽트가 JDBC를 끊지 않는다(M6b 확인 항목과 같은 위험을 만들지 않음).
- DB 실패는 캐시만 잃는다(영상 검색은 그대로, `partial` 그대로, WARN 한 줄 — 날짜·사건 ID 없음).
- 요청한 사람 정보는 저장하지 않는다(출처·날짜·사건·결과·시각뿐, `CLAUDE.md` 7절 6항).

## 확인용 기준표 (`data/verification/event-media-expectations.json`, content-curator)

데모 3개(1988-09-17, 2002-06-22, 1995-03-15)와 표본 5개 날짜(1950-06-25, 1969-07-20, 1986-09-20, 2000-02-29, 2014-04-16 — M3-P13, 공개 역사 사건 날짜·경계값)마다 사건별·출처별(Commons / 공식 YouTube / 대한뉴스) "있어야 할 것 / 없어야 할 것"과 근거 URL. 근거는 리서치 5절 종합 표·V1·V2·V5 표와 3.5절 Y2 표. 예: "1988-09-17 올림픽: Commons 없음 / YouTube·대한뉴스 제1715호 `approx`", "1969-07-20: Commons 있음(`P10`), 발사일 영상·기념 영상은 없어야 함", "2000-02-29: 2·3·4호선 개통 회차는 없어야 함", "2014-04-16: `tragic`, 영상 없음", "1995-03-15: 사건 없음". 응답에는 쓰지 않는다. 다른 날짜를 더하지 않는다.

## API / 데이터 변경

- `GET /api/story/world`: 형식은 M3a4 그대로. 맞는 Commons 영상이 있는 사건의 `media` 에 영상 항목 하나가 사진 뒤에 붙는다(`selection` 은 영상에만). **데모 3날짜는 Commons 영상이 없어 응답이 지금과 같다**(사진만, 오류 아님).
- 영상 항목 예시 (녹화 값으로 T2가 정함 — 표본 날짜 1969-07-20, 공개 역사 사건):

```json
{
  "id": "commons-<pageid>",
  "type": "video",
  "display_mode": "video_file",
  "url": "https://upload.wikimedia.org/wikipedia/commons/1/19/AP11_FINAL_APPROACH.ogv",
  "thumbnail_url": "<API thumburl>",
  "video_sources": [{ "url": "https://upload.wikimedia.org/wikipedia/commons/transcoded/1/19/AP11_FINAL_APPROACH.ogv/AP11_FINAL_APPROACH.ogv.240p.vp9.webm", "mime": "video/webm", "height": 240 }],
  "duration_seconds": 270.0,
  "source_name": "Wikimedia Commons",
  "license": "Public domain",
  "license_status": "free",
  "captured_at": "1969-07-20",
  "date_precision": "day",
  "place_ref": "world",
  "date_relation": { "kind": "same_day", "years": 0 },
  "selection": { "basis": "commons_p10", "score": 130 }
}
```

(나머지 키는 M3a3 형식 그대로 — 값 없는 키는 `null`. 주소·`pageid` 는 녹화 값을 따른다.)

- 설정: `media.search-cache.ttl.commons: 7d`(T6). `media.video.*` 는 바뀌지 않는다.
- DB: V3 `event_media_search_cache`(T6).
- 데이터: `data/verification/event-media-expectations.json`(T1, 응답에 쓰지 않음).

## 테스트 방침

- **외부 네트워크를 부르지 않는다.** WireMock + 녹화 fixture(Wikimedia는 복사 허용 출처, `CLAUDE.md` 7절 5항): `fixtures/wikimedia/commons/video/`·`fixtures/wikimedia/wikidata/` 의 `README.md` 에 원 URL·녹화 일자·User-Agent(저장소 주소)·라이선스(Commons 응답 텍스트 CC BY-SA 4.0, 파일 메타데이터는 각 파일 표기, Wikidata CC0). 바이트를 고치지 않는다. 데모 날짜가 0건이면 0건 응답을 녹화한다. 경계 사례는 합성 fixture(`synthetic-` 접두).
- 위키백과 녹화는 데모 3날짜뿐이다 — 1969·1986은 **어댑터·`VideoSearch` 수준**에서 `EventContext` 를 직접 만들어 시험하고, 세계 API 끝까지는 기존 합성 2000-02-29 위키백과 fixture + 합성 Wikidata·Commons로 한 번 본다. 새 위키백과 녹화는 하지 않는다.
- 날짜는 데모 3개와 표본 5개(경계값 2000-02-29 포함)뿐이다 — 실제 사람의 생년월일이 아니다(M3-P13). 시험용 `tragic` 사건은 지어낸 문장.
- 자동 테스트는 규칙과 경로만 본다. 실제 품질은 PM 품질 확인(T5).
- 시간 시험은 짧은 상한·늦게 답하는 스텁, 상한은 설정값과 `Deadline.CANCEL_GRACE` 에서 계산(M3a3d T2 (6)과 같은 식).
- 반복: 검증 작업은 `./gradlew check` **2회**, 마감·병렬·시간 시험이 든 범위는 **3회**.
- 로그 시험은 `OutputCaptureExtension` 으로 날짜·범주 이름·파일 이름·QID·전체 사건 ID가 없음을 본다.

## 데모·표본 날짜로 확인할 것

| 날짜 | 자동 테스트 (fixture) | PM 품질 확인 (T5) |
|---|---|---|
| 1988-09-17 | 세계 API(녹화): 범주 `1988 Summer Olympics` 직속 동영상 0건, 날짜 범주 없음 → 영상 없음, `selection` 키 없음, `partial: false` | 기준표와 대조 (Commons 없음이 정상) |
| 2002-06-22 | 같음(녹화 0건) | 같음 |
| 1995-03-15 | 대상 없음 → Wikidata·Commons 영상 요청 0 | 같음 |
| 1969-07-20 | 어댑터·`VideoSearch`(녹화): `P10` 에 `AP11 FINAL APPROACH.ogv` → `video_file`, `selection {commons_p10, 130}`, `captured_at` 1969-07-20, `video_sources` 작은 해상도 먼저. `P10` 을 뺀 합성 Wikidata → 범주 직속 후보 중 점수 최고, 발사 장면(`Launch`) 영상은 안 붙음 | 결과 표 |
| 1986-09-20 | 녹화: `P373` 이 없으면 `P910` 사이트링크 범주로 요청(요청 경로 확인. 녹화에 그런 항목이 없으면 합성으로). 결과는 녹화대로 단언한다(리서치 기대: 직속 동영상 0건 → 영상 없음. 녹화가 다르면 보고) | 결과 표 |
| 2000-02-29 | 합성 세계 API 끝까지: 영상 하나 붙음, `[사진, 영상]` 순서, 스키마 통과 | 기준표와 대조(영상 0건이 정상) |
| 1950-06-25, 2014-04-16 | 합성(지어낸 `tragic` 사건 + `normal` 하나): `tragic` QID로 요청 0, 날짜 범주 요청 0, `tragic` 사건 `media` `[]` | 기준표와 대조 |
| 캐시 (T7) | 같은 날짜 두 번째 요청 → Wikidata·Commons 영상 요청 0, 응답 같음. TTL이 지나면 다시. 0건도 캐시. Commons 503·시간 초과 → 캐시 안 됨, 다음 요청에서 다시 부름 | 캐시 줄 확인(T9) |

## 크기 (400줄 규칙)

S1 = 제품 코드·설정(`server`, 테스트 제외 — 이전 계획과 같은 명령), S3 = `data/`(기준표 JSON, 따로 셈).

### 실측 비율

| 기능 | 파일별 셈 | 실제 S1 | 비율 |
|---|---|---|---|
| M3a3d (PR #22) | 약 234 | 312 / −72 | ×1.33 |
| M3a4 (PR #23) | 약 450 | **953** | **×2.1** |

이번에는 파일별 셈에 **×1.7**(두 실측의 가운데, 범위 ×1.33~×2.1)을 곱한다.

### 다시 셈

| 작업 | 파일별 셈 | 셈 | ×1.7 | 범위 |
|---|---|---|---|---|
| T0 | `VideoSearch` 출처별 판정 격리(약 14) · `SourceResult` 안쪽 목록 복사·`null` 거부(약 5) · `CaptureTime` 모양 검사 도우미(약 14) · `Candidate` 검사 호출(약 4) · `VideoAccuracy` 패턴 재사용·글 압축 한 번(약 18) | 약 55 | 약 95 | 75~115 |
| T2 | Wikidata 연결 조회(질의·해석·`getOnce`, 약 75) · Commons 영상 클라이언트(인터페이스·Action API 구현, 약 95) · `CommonsFileMapper` 영상 매핑(공용 부분 나누기·`videoinfo`·`mediatype`·변환본·`notAfter`·재료, 약 75) | 약 245 | 약 415 | 325~515 |
| T3 | Commons 영상 출처(`find`·두 단계 병렬·사건별 후보·상태, 약 95) · `application.yml` 주석(약 10) | 약 105 | 약 180 | 140~220 |
| **A 소계 (T0~T3)** | | **약 405** | **약 690** | **540~850** |
| T6 ★ | V3(약 15) · 엔티티(약 45) · 저장소·upsert(약 35) · `media.search-cache` 설정과 검사(약 25) · `application.yml`(약 8) | 약 128 | 약 220 | 170~270 |
| T7 | 캐시 흐름(조회·없는 사건만·쓰기·DB 실패, 약 85) · 후보 JSON 읽고 쓰기(필터 입력 포함, 약 30) | 약 115 | 약 195 | 155~240 |
| **B 소계 (T6~T7)** | | **약 243** | **약 415** | **325~510** |
| **합계** | | **약 648** | **약 1,100** | **860~1,360** |

- S3(T1 기준표 JSON): 약 150~250(코드가 아님, 400 규칙에 넣지 않음 — 이전 계획과 같음).
- 한 PR이면 약 1,100으로 M3에서 가장 크다. A·B는 선행이 갈리고(B는 출처에 기대지 않음, M3a5가 B를 씀) 리뷰 초점도 다르다 → **PM 결정 1**.

### 분할안 (PM 결정 1)

| 안 | 내용 | 크기 (×1.7, 범위) | 확인점 | M3 PR 수 |
|---|---|---|---|---|
| **(a) 두 PR — 어댑터 먼저 (추천)** | M3a4b `feat/commons-video` = T0~T5(리뷰 권장·기준표·Commons 어댑터·검증·PM 품질 확인). **M3a4c `feat/media-search-cache`** = T6~T9(결과 캐시). 순서 M3a4b → M3a4c → M3a5 | M3a4b 약 690 (540~850), M3a4c 약 415 (325~510) | M3a4b 확인점 1: T2 뒤 누적 S1 **600** 넘으면 T3 전에 **멈추고** 물음(부품만 먼저 낼지 — T0·T2는 응답을 바꾸지 않음), 확인점 2: T3 뒤 **850** 넘으면 알림. M3a4c 확인점: T6 뒤 **300** 넘으면 알림 | 22 |
| (b) 두 PR — 캐시 먼저 | M3a4b = T0 + T6·T7(가짜 출처로 시험) + 검증, M3a4c = T1~T5(기준표·Commons 어댑터) | 약 510 (400~625) / 약 595 (465~735) | M3a4b T7 뒤 600 알림, M3a4c T2 뒤 500 알림 | 22 |
| (c) 한 PR | T0~T9 모두 `feat/commons-video` | 약 1,100 (860~1,360) | 확인점 1: T3 뒤 누적 S1 **850** 넘으면 T6 전에 **멈추고** 캐시(T6~T9)를 M3a4c로 뺄지 물음, 확인점 2: T7 뒤 **1,350** 넘으면 알림 | 21 |

추천 이유 (a): 첫 실제 어댑터를 실제 데이터로 먼저 검증하고(3초 창 안에 끝나는지 — 리스크 1), 머지 뒤 PM이 M3b3 T6(실제 Commons 영상 화면 확인)을 바로 할 수 있다. 캐시는 출처에 기대지 않는 공용 부품이라 따로 리뷰하기 좋고, ★ 후보(T6)가 작은 PR 안에 깔끔하게 들어간다. (b)는 캐시를 가짜 출처로만 먼저 검증하고 실제 영상은 더 늦게 나온다. (c)는 리뷰할 양이 M3a4(953)보다 크다.

각 backend-dev 보고서(T0·T2·T3·T6·T7)는 누적 S1을 적는다. 테스트(S2)는 따로 보고한다.

## 작업 목록

순서: T0 → T2 → **확인점 1** → T3 → **확인점 2** → T4 → 리뷰 → PR(M3a4b) → T5(PR 검수 때) → 머지 → T6 → T7 → T8 → 리뷰 → PR(M3a4c) → T9 → 머지. T1(content-curator, `data/` 만)은 T0~T3과 폴더가 겹치지 않아 worktree로 동시에 해도 된다(T4 전까지). 결정 1 (c)면 PR이 하나이고 확인점이 (c) 값이다.

### A — Commons 동영상 (결정 1 (a)의 M3a4b)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | 연결 전 정리: PR #23 리뷰 권장 2·3 (`fix(media)`·`refactor(media)` 커밋) | backend-dev | M3a4 머지 (완료) | (1) **판정 격리**: `VideoSearch.Running.await` 가 출처마다 판정을 따로 감싼다. 판정이 예외를 던지는 가짜 `VideoAccuracy`(하위 클래스)와 가짜 출처 둘로: 던지는 출처만 비고 `Videos.failed` 가 참(세계 API에서는 `partial: true`), WARN 정확히 한 줄 `Video search failed: source=<key> events=<n> cause=<예외 클래스 이름>`, 다른 출처의 영상·사건 사진·`day_media` 는 남고 응답은 200. 예외가 `CommonsMediaLookup.lookup` 밖으로 나가지 않는다. (2) **입력 검사**: `Candidate` 는 `originalCapture` 의 `capturedAt` 모양이 `datePrecision` 과 맞지 않으면 생성 때 `IllegalArgumentException` — 예 `("1969-7-20", DAY)`, `("1969-07-20", YEAR)`, `(null, DAY)`, 없는 날 `("1969-02-30", DAY)`, `("1969", null)`(R8과 같은 규칙, 도우미 하나로 — 예 `CaptureTime` 의 검사 메서드). `explicitDates` 에 `null` 원소가 있으면 예외. `SourceResult` 는 `byEvent` 안쪽 목록도 복사하고 `null` 원소·`null` 목록이면 예외. 이 예외는 어댑터 작업 안에서 나므로 `Failed` 로 그 출처만 빈다 — 가짜 출처가 `null` 원소를 넣는 시험으로 확인(WARN 한 줄, 다른 출처 남음). (3) **패턴 재사용**: 영어 라벨 핵심어의 낱말 패턴을 후보마다 만들지 않는다 — 사건마다 한 번 준비해 그 사건의 후보들에 쓴다(`judge(EventContext, Candidate)` 는 그대로 남아 기존 시험이 고치지 않고 동작). 한글 낱말 목록 대조의 띄어쓰기 없앤 글도 후보마다 한 번. 리뷰 확인 항목: 후보마다 도는 경로에 `Pattern.compile` 이 없다. (4) `VideoAccuracy*Test`·`WorldStoryScoreTableTest`·`CandidateTest`·`VideoSearchTest` 의 기존 단언 그대로 통과(점수·`Reject` 불변). (5) `cd server && ./gradlew check` 성공. 보고서에 누적 S1 |
| T1 | 확인용 기준표 | content-curator | 리서치 머지 (완료) | (1) `data/verification/event-media-expectations.json`: 데모 3 + 표본 5 날짜마다 사건별·출처별(Commons / 공식 YouTube / 대한뉴스) "있어야 할 것/없어야 할 것"과 근거 URL. 이 8개 날짜 밖은 넣지 않는다. (2) `tragic` 날짜(1950-06-25, 2014-04-16)는 "영상 없음". (3) Commons 열은 리서치 5절 V1 표, YouTube 열은 3.5절 Y2 표, 대한뉴스 열은 5절 V5 표와 맞는다(행마다 근거 번호). (4) 개인정보 없음(날짜는 공개 사건 날짜). (5) C1 `ok`. 보고서에 S3 |
| T2 | Commons 영상 부품: Wikidata 연결 조회·영상 클라이언트·파일 매퍼 영상 매핑·녹화 (`feat(media)` 커밋) | backend-dev | T0 | (1) **녹화** (`curl`, User-Agent는 저장소 주소만, 영상·포스터 파일은 받지 않음): Wikidata 연결 질의 — 1969-07-20 아폴로 11호(Q43653), 1988-09-17·2002-06-22 데모 사건 QID(기존 `pageprops-*` 녹화의 값), 1986-09-20 아시안 게임 / Commons — `P10` 파일 묶음(`AP11 FINAL APPROACH.ogv`), 범주 직속 `Apollo 11`·`1988 Summer Olympics`·데모 2002 범주·1986 범주, `Videos taken on 1969-07-20`·`…1988-09-17`·`…2002-06-22`. `README.md` 에 원 URL·일자·User-Agent·라이선스, 바이트 그대로. (2) **Wikidata 연결 조회**: QID 묶음 질의 1회(QID 모양이 아니면 뺌, 남는 것이 없으면 요청 0) → QID마다 `P10` 파일 이름(`Special:FilePath/` 주소를 풀어서, 여럿이면 모두), 범주(`P373`, 없으면 `P910` 항목의 commonswiki 사이트링크 — `Category:` 접두 정리), 영어 라벨. `WikimediaHttpClient.getOnce`, 읽기 타임아웃 `media.lookup-deadline`. 녹화 단언: Q43653 → `AP11 FINAL APPROACH.ogv`, 범주 `Apollo 11`, 라벨 `Apollo 11`. `P373` 없는 항목 → `P910` 사이트링크(녹화에 없으면 합성). 오류·읽을 수 없는 응답 → 실패(빈 값과 구별). (3) **영상 클라이언트**(사진 `CommonsClient` 와 나눈 인터페이스 — 기존 사진 시험 파일은 고치지 않음): "경로와 요청" 표의 세 요청 모양(매개변수를 WireMock `withQueryParam` 으로 단언), 한 번만 보냄, 읽기 타임아웃 `media.lookup-deadline`, `ActionApiErrors` 의 `error` → 실패, `ratelimited` → 호스트 막기, 없는 범주는 0건(실패 아님). 녹화로 생성기 응답에 `derivatives` 가 옴을 확인 — 안 오면 묶음 요청을 더하고 보고. (4) **매퍼**(`CommonsFileMapper` 재사용): 영상 매핑이 "미디어 레코드" 표대로 만든다 — `videoinfo[0]`, `mediatype` `VIDEO` 만, `video_sources` 순서·MIME 매개변수 떼기·`application/ogg` 원본은 `url` 에만, `captured_at` = `notAfter(파서 값, 요청 날짜)`, 업로드 시각 안 읽음, 차단·통신사 제외·R1~R9는 사진과 같은 경로. 판정 재료(제목·설명 평문·`notAfter` 전 파서 값)를 함께 돌려준다. **사진 매핑 결과는 그대로**: `CommonsFileMapperTest`·`CommonsRecordedDaysTest`·`CommonsMediaLookup*Test` 단언 그대로 통과. (5) 녹화 단언: `AP11 FINAL APPROACH.ogv` → `id` `commons-<pageid>`, `type` `video`, `display_mode` `video_file`, `captured_at` 1969-07-20 `day`, `video_sources[0]` 이 가장 작은 높이의 `video/webm`, 어떤 `mime` 에도 `;` 없음, `duration_seconds` 녹화 값(약 270), `license_status` `free`, 위치는 넘긴 `place_ref`. 합성: `DateTimeOriginal` 2019-07-21 + 요청 1969-07-20 → `captured_at`·`date_precision` `null`, 재료의 파서 값은 2019-07-21 / 작가 `Associated Press` → 버림 / `mediatype` `AUDIO` → 버림 / `video_sources` 의 `mime` 이 `video/webm; codecs="vp9, opus"` → `video/webm`. (6) 로그: 실패 WARN의 `title` 은 고정 이름표(예 `wikidata video links of 3 items`, `commons video category search`, `commons video files of 2`) — 날짜·범주 이름·파일 이름·QID 없음(로그 캡처 시험). (7) 아직 `EventVideoSource` 빈이 없다 → 세계 API 응답 그대로(기존 `WorldStory*Test` 그대로). (8) `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 1** |
| T3 | Commons 영상 출처 연결 (`feat(media)` 커밋) | backend-dev | T2 | (1) `EventVideoSource` 구현 빈(key `commons`)이 `VideoSearch` 에 잡힌다. `find(date, events, dayPathsOff)` 는 기다림 두 단계: 1단계 Wikidata 질의 ∥ 날짜 범주(`dayPathsOff` 면 없음), 2단계 `P10` 파일 묶음 ∥ 사건마다 범주 직속 검색. 안쪽 작업은 바깥 작업이 끊기면 함께 끊긴다(시험: 바깥 마감에 걸리면 안쪽 요청 스레드가 남지 않음). (2) 후보: `P10` → `EventLink.P10`·`COMMONS_P10`, 범주 직속 → `CATEGORY_DIRECT`·`COMMONS_CATEGORY`, 날짜 범주 → `NONE`·`COMMONS_DATE_CATEGORY`(그날의 모든 대상 사건에 후보로 — 사건 사이 중복은 `VideoSearch` 가 막음). `official` 거짓, `explicitDates` 빈, `dateTextExcluded` 거짓. 영어 라벨은 `SourceResult.englishLabels`. QID 없는 사건은 날짜 범주 후보만. (3) 상태: 요청 하나라도 실패하면 `FAILED`(WARN은 HTTP·클라이언트 줄만 — M3a4 T6 (8) 규칙 그대로 한 실패에 한 줄), 아니면 `OK`(0건 포함). (4) 요청 수(WireMock 서브 이벤트로): 대상 3개 새 날짜 최대 6, `dayPathsOff` 면 날짜 범주 0, 대상이 없으면 Wikidata·Commons 영상 요청 0, `tragic` 사건 QID는 질의에 없음. (5) **병렬**: 응답마다 300ms 늦는 스텁에서 사건 3개 날짜의 `find` 가 1.5초 안에 끝난다(순서대로면 6 × 300 = 1.8초 이상). (6) "데모·표본 날짜" 표의 자동 테스트 행(캐시 행 제외)이 통과한다 — 1969-07-20 `selection {commons_p10, 130}`·`captured_at` 1969-07-20·`date_relation.kind` `same_day`, `P10` 을 뺀 합성에서 범주 직속 최고점·`Launch` 영상 안 붙음, 데모 2날짜 영상 0·`partial: false`·`Video search failed` 없음, 1995-03-15 요청 0, 합성 2000-02-29 세계 API 끝까지 `[사진, 영상]`·영상 항목이 `date_relation`·`selection` 을 뺀 뒤 `media.schema.json` 통과, 합성 `tragic` 날. (7) **영어 라벨**: 영어 제목만 있는 후보가 녹화 라벨로 사건어 +30을 받아 붙고, 라벨이 없는 합성 응답에서는 같은 후보가 버려진다. (8) **기존 시험**: 기대값(단언)을 바꾸지 않는다. 요청 수를 세는 기존 시험은 세는 대상을 그 시험의 경로로 좁히거나(예: SPARQL 질의 내용으로 나눔) 그 시험만 `media.video.max-events: 0` 으로 돈다 — 준비 코드. **예외 하나**: `WorldStoryNoVideoSourceTest` 의 `sources.stream()` 이 비었다는 단언은 M3a4 전제("이 PR에는 출처 빈이 없다")가 이 작업으로 끝나므로 "출처 빈은 key `commons` 하나"로 바꾸고, 같은 시험의 나머지 단언(`selection`·`video`·`link_only` 없음, `partial` 거짓, WARN 없음, 매치 안 된 요청 0)은 녹화 0건 스텁을 더해 그대로 통과. 바꾼 준비 줄·단언 줄을 보고서에 모두 적는다. (9) **창 하나**: 사진 두 경로와 Commons 영상이 모두 응답하지 않아도 미디어 구간이 `lookup-deadline` + `CANCEL_GRACE` + 0.5초 이하, `WorldStoryEventPhotoPartsRegressionTest` 의 상한 시험이 단언을 바꾸지 않고 통과. 실패 격리: Commons 503 / Wikidata 시간 초과 → 그 출처만 빔, `partial: true`, 사진 남음. (10) 응답 어디에도 `link_only` 없음. 로그(DEBUG 포함)에 날짜·범주 이름·파일 이름·QID·전체 사건 ID 없음. (11) `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 2** |
| T4 | A 완료 기준 검증과 전체 테스트 | test-engineer | T1, T3 | (1) T0~T3 기준 표(기준마다 테스트 이름). 빠진 기준은 테스트를 더한다(제품 코드는 고치지 않음). (2) `cd server && ./gradlew check` **3회** 반복 안정(병렬·마감 시험 포함). (3) S1·S2·S3 보고. (4) fixture 기준 데모·표본 날짜 결과 표(날짜, 사건, 영상 유무, `selection`, `captured_at`, `date_relation.kind`, 요청 수) — 시험할 수 있는 수준(세계 API / 어댑터)을 열로 밝힌다. (5) 잰 미디어 구간 시간과 세계 섹션 최악 시간을 M3a4 T7 측정과 나란히. (6) 새 시험·바뀐 시험 모두 WireMock 밖으로 요청하지 않음(`findAllUnmatchedRequests` 빈 목록). (7) 녹화 fixture `README.md` 에 이메일이 없고 User-Agent가 저장소 주소인지. (8) T1 기준표 C1 `ok` 와 행 근거 번호가 있는지 |
| T5 | PM 품질 확인 (M3a4b PR 검수 때) | PM | T4 | (1) `docker compose up -d db`, `cd server && ./gradlew bootRun`(루트 `.env` 의 `WIKIMEDIA_USER_AGENT` 는 저장소 주소만, ASCII. 키 없음). (2) P1으로 데모 3 + 표본 5 날짜를 뽑아 기준표와 대조: 엉뚱한 영상, 있어야 하는데 없는 영상, `tragic` 날짜에 영상 없음, `unknown` 영상에 사건 날짜가 촬영 시점으로 들어가지 않음. 국내 날짜에 Commons 영상이 없는 것은 정상 — **다른 날짜로 찾아보지 않는다**. (3) 1969-07-20에서 서버 로그에 `Video search failed … cause=deadline` 이 있는지(3초 창 — 리스크 1). (4) (선택) P0 핫링크 재생 확인 — 403·429면 보고. (5) 머지 뒤 M3b3 T6(웹 화면 실제 Commons 영상 확인, 1969-07-20 표본 버튼)을 할 수 있다 |

### B — 결과 캐시 (결정 1 (a)의 M3a4c `feat/media-search-cache`)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T6 | ★ 캐시 저장소: Flyway V3·엔티티·저장소·TTL 설정 (`feat(db)` 커밋) | backend-dev (PM 결정 3 (b)) | A 머지 (결정 1 (c)면 T4) | (1) `V3__event_media_search_cache.sql` 이 적용되고 `ddl-auto=validate` 로 컨텍스트가 뜬다(Testcontainers). (2) 열은 "결과 캐시" 절의 것뿐 — `information_schema.columns` 로 열 이름 목록을 단언(요청자 정보·IP·장소 없음). 열 타입(`TEXT`·`JSONB`)과 이름은 보고서에 이유와 함께. (3) 유일 키 (출처, 날짜, 사건 ID, `day_paths_off`). upsert는 같은 키를 덮어쓰고, 두 쓰기가 겹쳐도 예외 없이 하나만 남는다(V2 `upsert` 와 같은 방식). (4) `media.search-cache.ttl` 지도: `commons: 7d`(`application.yml`, 주석). 음수는 시작 실패(오류에 설정 이름), `0` 과 없는 출처는 "캐시 안 함". 기존 `media.*` 설정 시험 그대로. (5) 아직 흐름에 연결하지 않는다 → 응답·요청 수 그대로. (6) `./gradlew check` 성공. 보고서에 누적 S1 — 결정 1 (a)의 M3a4c 확인점(300 알림) |
| T7 | 캐시 흐름 (`feat(media)` 커밋) | backend-dev | T6 | (1) 출처 공통: `VideoSearch` 가 출처마다 (출처, 날짜, 사건 ID, `dayPathsOff`)로 캐시를 본다. 신선한 사건은 출처에 묻지 않고, 일부만 있으면 없는 사건만 `find` 에 넘긴다, 모두 있으면 그 출처 요청 0. (2) 쓰기: `Status.OK` 결과만 사건마다 한 줄(0건 포함). `FAILED`·`QUOTA_SKIPPED`·예외·시간 초과·`Interrupted` 는 쓰지 않는다(가짜 출처로 각각). 쓰기는 마감 작업 밖(`Running.await()` 뒤, 요청 스레드). (3) 저장 내용: 판정 전 후보(레코드·판정 재료·`basis`), 필터 입력(`MediaOrigin`), 영어 라벨 — 메타데이터만. 읽을 때 다시 판정하고 `MediaFilter`·`MediaNormalizer` 를 다시 거친다: 캐시에 있는 파일을 `media.blocklist` 에 넣고 컨텍스트를 다시 띄우면 나오지 않는다(시험). `retrieved_at` 은 처음 조회 시각. 설정 `min-score` 를 바꾸면 캐시 줄 그대로 결과가 따라 바뀐다. (4) 읽을 수 없는 줄 → 없는 것으로 보고 다시 찾아 덮어쓴다. (5) DB 실패(던지는 가짜 저장소): 읽기 실패 → 출처에 묻는다, 쓰기 실패 → 결과는 그대로 쓴다. 각각 WARN 한 줄 `Media search cache <read\|write> failed: source=<key> cause=<클래스>` — 날짜·사건 ID 없음. `partial` 은 캐시 때문에 바뀌지 않는다. (6) "데모·표본 날짜" 표의 캐시 행: 같은 날짜 두 번째 요청 → Wikidata·Commons 영상 요청 0, 응답 같음(`retrieved_at` 포함) / 시계를 옮겨 정확히 `fetched_at + ttl` 이면 다시 부름, 1초 전이면 안 부름 / 0건도 캐시 / Commons 503·시간 초과 → 캐시 없음, 다음 요청에서 다시 부름 / `dayPathsOff` 가 다른 요청은 다른 줄. (7) 창 하나·최악 시간 시험(`WorldStoryEventPhotoPartsRegressionTest`)과 A의 시험이 단언을 바꾸지 않고 통과. 시험 사이에 캐시가 새지 않는다(캐시 비우기 준비 코드 — 바꾼 줄 보고). (8) `./gradlew check` 성공. 보고서에 누적 S1 |
| T8 | B 완료 기준 검증과 전체 테스트 | test-engineer | T7 | (1) T6·T7 기준 표(기준마다 테스트 이름), 빠진 기준은 테스트를 더한다. (2) `./gradlew check` **2회** 반복 안정, 마감·창 하나 시험이 든 클래스는 **3회**. (3) S1·S2·S3 보고. (4) 캐시 행 결과 표(첫 요청·두 번째 요청의 출처별 요청 수, TTL 경계, 실패 뒤 다시 부름). (5) 바깥 요청 0 |
| T9 | PM 캐시 확인 (M3a4c PR 검수 때) | PM | T8 | (1) 서버 실행 중 1969-07-20을 두 번 부른다(P1). 두 응답의 영상 항목이 같다. (2) P2로 캐시 줄이 출처 `commons` 로 있고, 열에 요청자 정보가 없다. (3) 다른 날짜로 찾아보지 않는다 |

```
# C1 — 기준표 JSON (content-curator). ok
python3 -c 'import json,sys; json.load(open(sys.argv[1])); print("ok")' data/verification/event-media-expectations.json

# S1 — 제품 코드·설정 줄 수 (M3a4와 같은 명령)
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
# S3 — 데이터 파일 줄 수
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P0 — (선택, PM) Commons 핫링크 재생 확인. 리서치 6.1절 명령 2개를 그대로 실행한다
#      (206 또는 200 + video/webm, 200 + image/jpeg 이면 정상. User-Agent는 $WIKIMEDIA_USER_AGENT)

# P1 — PM 품질 확인 (서버 실행 중, 키 없음)
for d in 1988-09-17 2002-06-22 1995-03-15 1950-06-25 1969-07-20 1986-09-20 2000-02-29 2014-04-16; do
  echo "== $d"
  curl -s "http://localhost:8080/api/story/world?date=$d" \
    | jq -c '{partial, events: [.events[] | {title, tone, videos: [.media[] | select(.type=="video") | {source_url, attribution, selection, captured_at, date_relation}]}]}'
done

# P2 — (T9) 캐시 줄 확인. .env 의 POSTGRES_USER·POSTGRES_DB를 바꿨으면 그 값으로
docker compose exec db psql -U postgres -d the_world_that_day \
  -c "SELECT source, event_date, day_paths_off, fetched_at FROM event_media_search_cache ORDER BY fetched_at"
```

담당별: backend-dev 5 (T0, T2, T3, T6, T7 — T6은 PM 결정 3 (b)), content-curator 1 (T1), test-engineer 2 (T4, T8), PM 2 (T5, T9). 모두 10개.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 메모 |
|---|---|---|
| M3a5 YouTube | 결과 캐시(`media.search-cache.ttl.youtube` 를 더하고 30일 상한 검사), `QUOTA_SKIPPED` 는 캐시 안 됨(T7 (2)), 출처 공통 캐시 흐름, T0의 입력 검사·판정 격리 | 30일이 지난 YouTube 줄을 지우는 일(III.E.4.d)은 M3a5가 정한다 — 이 기능은 만료 줄을 덮어쓰기만 한다 |
| M3a5b 대한뉴스 | 같음 | — |
| M3b3 T6 (웹) | 실제 Commons 영상(1969-07-20 표본) | A 머지 뒤 `main` 에서 |

## 리스크와 확인할 것

1. **3초 창과 요청 자리 3개** — 새 날짜에 Commons 영상 요청(최대 6, 2단계)이 사진 요청(최대 3)과 같은 3초·같은 자리 3개를 나눠 쓴다. Commons `extmetadata` 응답이 느리면 영상만 끝 시각에 잘려 `partial: true` 가 되고, 실패는 캐시하지 않으므로 같은 날짜를 다시 불러도 계속 잘릴 수 있다. 대응: 변환본 요청 없앰, 두 단계 병렬, `gsrlimit` 10·`gcmlimit` 20, `extmetadata` 키 줄이기, T4 측정, **T5에서 1969-07-20 실제 로그 확인**. 계속 잘리면 그때 PM이 정한다(선택지 예: Wikidata 질의를 M3a2 질의에 합침(결정 2 (b)), 영상 출처만 끝난 단계를 캐시, `media.lookup-deadline` 늘림 — 세계 섹션 최악 시간이 늘어남).
2. **국내 날짜에 Commons 영상이 거의 없다**(리서치 T1, 위험 A) — 데모 날짜(모두 국내 사건)에는 이 기능으로 영상이 붙지 않는다. 정상(사진만)이다. 국내 영상은 M3a5·M3a5b가 주 경로. 다른 날짜로 보이려 하지 않는다.
3. **촬영일 필드 오류**(위험 2) — 업로드·복원일이 들어 있다. R-V3(`notAfter`)으로 비우고 "촬영 시점 미상"으로 붙는다. `CaptureTimeParser` 가 못 읽는 모양("Taken on" 이외의 문장)도 "촬영 시점 미상"이다(안전한 쪽). 점수는 설명의 글 속 날짜(`TextDates`)로 받을 수 있다.
4. **글 속 날짜가 사건보다 늦어도 같은 달·같은 해 점수**(PR #23 리뷰 메모 3, `VideoAccuracy.dateScore`) — Commons 설명에 "July 24, 1969"(귀환일) 같은 날짜가 있으면 1969-07-20 사건에 +20을 받는다. 이 기능은 규칙을 바꾸지 않는다. T5 결과 표에서 이런 사례가 보이면 M3a4 T4(낱말 목록 검토)와 함께 PM이 다시 본다.
5. **핫링크** — 정책상 "가능하지만 권장하지 않음". 로컬 데모 규모에서는 문제를 찾지 못했다. 공개 배포(#5) 때 다시 본다. `thumburl` 만 쓴다(위험 4·5).
6. **모바일 용량** — 원본이 수백 MB일 수 있다. 작은 변환본부터, 10분 상한(위험 6). `application/ogg` 원본은 `video_sources` 에 넣지 않는다.
7. **기존 시험의 요청 수** — 경로만으로 스텁하고 요청 수를 세는 세계 API 시험이 여럿이다. 어댑터가 붙으면 SPARQL·Commons 요청이 늘어난다. T3 (8)은 기대값을 바꾸지 않고 세는 대상이나 준비 설정만 바꾸게 하고, `WorldStoryNoVideoSourceTest` 의 단언 하나만 전제가 끝나 바뀐다(보고서에 모두).
8. **캐시와 차단 목록** — 캐시된 항목이 차단 목록을 우회하지 않게 읽을 때 다시 거른다(T7 (3)). 차단 목록은 지금처럼 다시 띄워야 적용된다(M3a3a 리스크 5).
9. **캐시와 마감 인터럽트** — 쓰기를 마감 작업 밖에서 해 JDBC가 인터럽트되지 않게 한다(M6b의 M3a1c 리뷰 권장 2와 같은 위험을 새로 만들지 않음).
10. **크기** — 한 PR이면 약 1,100. 결정 1과 확인점으로 다룬다. M3a4는 파일별 셈의 ×2.1이었다.

## PM 결정 필요

**PM 결정 (2026-10-24)**: 1 **(a)**, 2 **(a)**, 3 **(b)**, 4 **(a)** — 모두 planner 추천과 같다. 확인점: M3a4b T2 뒤 누적 S1 600 넘으면 T3 전에 멈춤, T3 뒤 850 넘으면 알림, M3a4c T6 뒤 300 넘으면 알림.

**확인점 1 결과 (2026-10-24)**: T2 뒤 누적 S1 added 763 / deleted 222 (순증 541, 그중 약 147줄은 `ActionApiCommonsClient` → `CommonsActionApi` 로 옮긴 코드) > 600 → PM **(a)** 이 PR에서 T3까지 진행. 고르지 않은 것: (b) T0·T2 부품만 먼저 PR, (c) 옮김을 되돌려 줄 수 줄이기. 확인점 2(T3 뒤 850)는 알림만.

**확인점 2 결과 (2026-10-24)**: T3 뒤 누적 S1 added 954 / deleted 222 > 850 → 알림만, 계속.

**T4 결정 (2026-10-24)**: 1 **(a)** 기준표 1988-09-17 `kbs-1tv`·`kbs-2tv` 두 행은 근거 번호 없이 `not_researched` + `evidence_gap` 그대로 둔다(리서치가 보지 않은 칸을 정직하게 적은 것 — T1 (3)의 "행마다 근거 번호"에 대한 예외). 2 **(a)** 녹화 `commons/video/date-category-1969-07-20.json` 의 Commons 파일 설명에 든 제3자 공개 연락처(미국 NARA)는 바이트 그대로 둔다(녹화를 고치지 않음, `CLAUDE.md` 7절 5항). 둘 다 test-engineer 추천과 같다.

| # | 물을 것 | 선택지 | 추천 |
|---|---|---|---|
| 1 | 크기 약 1,100 (860~1,360) — 나눌지 | (a) 두 PR: M3a4b 어댑터(T0~T5, 약 690) → M3a4c `feat/media-search-cache` 결과 캐시(T6~T9, 약 415). 확인점: M3a4b T2 뒤 600(멈춤)·T3 뒤 850(알림), M3a4c T6 뒤 300(알림). M3 PR 22개 / (b) 두 PR, 캐시 먼저 / (c) 한 PR, 확인점 T3 뒤 850(멈춤)·T7 뒤 1,350(알림) | **(a)** — 실제 어댑터를 먼저 검증하고 M3b3 T6을 앞당긴다. 캐시는 공용 부품이라 따로 리뷰하기 좋다 |
| 2 | 영상용 Wikidata 질의를 어디서 하나 | (a) Commons 어댑터가 따로 1회(미디어 창 안) / (b) M3a2 톤 질의(`SparqlWikidataSignalSource.query`)에 `P10`·`P373`·`P910`·영어 라벨을 더해 추가 요청 0 | **(a)** — 외부 연동을 어댑터마다 나누는 규칙(`CLAUDE.md` 7절 3항)을 지키고, 질의가 무거워져 톤 신호가 실패하면 그날 사건이 "판정 불가 → `tragic`"으로 바뀌는 위험을 만들지 않는다. T5에서 창이 모자라면(리스크 1) 그때 (b)를 다시 본다 |
| 3 | ★ T6(캐시 저장소: Flyway V3·JPA 엔티티·upsert·TTL 설정)을 PM이 직접 구현할지 | (a) PM 직접 / (b) backend-dev | **(b)** backend-dev — 지금까지의 속도를 유지한다. 다만 M3에서 JPA·Flyway를 직접 해 보기에 가장 작고 떨어진 작업(약 130줄, 출처 코드와 무관)이라, 배우고 싶으면 (a)도 좋다 |
| 4 | 보조 경로 (b-2)·(b-3)(전 T4, 크기 조건부) | (a) 이번 범위에서 빼고 후속 후보로 / (b) 남김(약 +45~70) | **(a)** — 원래 "크기가 되면" 조건이었고 크기가 되지 않는다. 품질 확인(T5·M3a5 T4) 뒤 필요하면 따로 |

결정됨(그대로): 영상 1~4·21번(2026-10-11), M3a4에서 나누는 것(D1 (a), 2026-10-12), M3a4 결정 5 (a) — 리뷰 권장 2·3을 이 기능으로(2026-10-24).

## 다음 단계 제안 (이 계획 밖, PM이 일정을 정함)

- 웹 mock `selection.score` 0.9 → 정수(예 90) — 작은 웹 `fix/`
- M3b3c 리뷰 권장 1 — 플레이어 `onReady` 전에 사용자가 누른 재생을 조정자가 모름 — 작은 웹 `fix/`
- 시험 공용 `noUnmatchedRequest` `@AfterEach`(매치 안 된 WireMock 요청 0을 시험마다 따로 단언하는 것을 하나로)
- 위키백과 사건 어댑터(`ActionApiKoWikiClient`·`ActionApiKoWikiLinkResolver`)의 `ActionApiErrors` 공용 검사
- `WorldSection.test` 의 `describe` 이름 정리
- 가끔 실패하는 `TwoPageKoWikiEventSourceTest` 안정화
- `server-ci` 경로에 `web/src/media/types.ts`(서버 계약 시험이 읽음)와 `data/` 더하기
