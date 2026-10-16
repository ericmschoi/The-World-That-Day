# 그날 사진: Commons 연결과 파일 매퍼 (M3a3b)

> 브랜치: `feat/commons-photos` (`main` 9642d08에서 갈라짐) · 작성: planner · 상태: **PM 승인 (2026-10-14, 개정)** — 결정 1 **(a)** 두 PR로 나눔, 결정 2 **(b)** T2는 backend-dev. 처음 승인 2026-10-13(M3a3 분할 결정 A (a), 선행을 M3a3a로 바꿈).
> **분할 (2026-10-14, 결정 1 (a))**: 개정판의 2부(사건 사진 — PageImages, Commons 파일 제목 묶음, 사건 사진 고르기, 이전 T5~T8)는 **M3a3c `feat/event-photos`** (`docs/plans/m3a3c-event-photos.md`, T1~T4)로 옮겼다. 이 계획서는 1부(T0~T4)만 남는다.
> 기준: PM 미디어 결정 2026-10-11, 안전장치 1~6, `CLAUDE.md` 7절 1·2·3·5·6항, `docs/product-brief.md` 6.1절·6.2절·6.3절·8절·12절, `docs/research/day-events.md` 3·8절, `docs/research/video-sources.md` V1-8·리스크 5(`thumb.wikimedia.org`), `docs/plans/m3a3-event-media.md`(형식·R1~R9·`date_relation`), `docs/plans/m3a3a-media-rules.md`(파서·라이선스 상태·차단과 제외), #21 (a), #22 (c), M3-P17 (a)
> 선행: **모두 머지됨** — M3a2 (PR #11), M3a3 (PR #12, 형식·R1~R9·`date_relation`), M3a3a (PR #14, `CaptureTimeParser`·`LicenseClassifier`·`MediaFilter`·`MediaOrigin`·`MediaText`·`MediaProperties` 차단 목록과 제외 목록). 웹 M3b2 (PR #13)·M3b2b (PR #15)도 머지됨 — 웹은 이미 `day_media` 를 캡션 배지로 세 묶음에 나눠 그리고, `tragic` 사건에는 미디어를 그리지 않는다.
> 뒤따르는 기능: 아래 "뒤따르는 기능" 절 — **M3a3c**(사건 사진), **M3b2b T6**(PM 실제 그날 사진 확인, 이 PR 머지 뒤 `main` 에서), M3a4b(Commons 파일 매퍼 재사용), M4c.

## PM 결정 (2026-10-14)

| # | 질문 | 결정 | 반영 |
|---|---|---|---|
| 1 | 다시 셈 약 715줄(640~860)을 어떻게 낼까 | **(a) 두 PR — 그날 사진 / 사건 사진**. 이 PR(M3a3b) = 그날 사진·Commons 파일 매퍼·이월 리뷰 권장, 약 485. 사건 사진은 M3a3c `feat/event-photos`, 약 230. M3 PR 19 → **20**. planner 추천과 같음. 고르지 않은 것: (b) 한 PR, (c) 어댑터 / 연결로 두 PR | 머리말, 범위 밖, 크기(확인점 2개), 작업 목록, 뒤따르는 기능. 2부는 `docs/plans/m3a3c-event-photos.md` |
| 2 | ★ T2 "세계 API 연결: 그날 사진"을 PM이 직접 구현할까 | **(b) backend-dev가 구현**. planner 추천과 같음 | T2 담당 |

## 이번 개정에서 바뀐 것 (2026-10-14)

| 항목 | 내용 |
|---|---|
| 머지된 코드에 맞춤 | "이어받는 계약"을 실제 클래스·메서드 이름으로 다시 썼다: `MediaFilter.check(MediaRecord, MediaOrigin)` → `Optional<MediaFilter.Reason>`, `MediaOrigin(source, itemId, author, credit, channel)`, `CaptureTimeParser.parse(String)` → `CaptureTime`, `LicenseClassifier.classify(String)` → `LicenseStatus`, `MediaNormalizer.normalize(MediaRecord, LocalDate)` → `Optional<MediaItem>`. 필터는 `MediaRecord` 를 받으므로 순서가 **레코드 만들기 → 필터 → 정규화** 다(이전 계획의 "정규화 앞에서 원문 정보로" 를 고침) |
| 설정 묶음 이름 | 미디어 조회 설정은 `MediaProperties` 에 더하지 않고 새 묶음(`MediaLookupProperties`, 이름은 예시)으로 같은 `media` 접두어에 둔다. `MediaProperties` 의 생성자를 부르는 테스트가 6개 파일 15곳이라 생성자를 바꾸지 않는다. `wikimedia` 접두어의 `WikimediaProperties` + `WikidataProperties` 와 같은 방식이다. 이 PR은 `day-media-limit`·`lookup-deadline`, M3a3c가 `max-years-apart` 를 더한다 |
| WireMock 경로 | 테스트 기반 `WikimediaWireMockTest` 는 위키백과를 `/w/api.php` 에 둔다. Commons도 실제 경로가 `/w/api.php` 라 **다른 경로**(예: `/commons/w/api.php`)로 돌린다(T1). M3a2 테스트가 미디어 요청 때문에 깨지지 않게 하는 규칙을 T2에 넣었다 |
| 날짜 범주 순서 | "범주 순서" → **파일 제목 순**. `generator=categorymembers` 응답의 `pages` 순서는 범주 순서를 보장하지 않아 결과가 흔들릴 수 있다 |
| 이월 권장 6개 | 아래 "이월된 리뷰 권장" 표. 1~3은 새 **T0**, 4는 T2, 5는 T1(+ "웹에 필요한 것"), 6은 리스크 9(알려진 한계, 작업 아님) |
| 크기 다시 셈과 분할 | 머지된 실측(M3a3, M3a3a)과 파일별 셈으로 약 560 → 전체 약 715. PM 결정 1 (a)로 이 PR은 **약 485 (420~585)**, 사건 사진 약 230은 M3a3c |
| 작업 번호 | 이 PR은 T0~T4. 이전 T1(클라이언트·매퍼·PageImages)은 T1(날짜 범주·매퍼)과 M3a3c T1(PageImages·파일 제목 묶음)로, 이전 T2(연결)는 T2(그날 사진)와 M3a3c T2(사건 사진)로 나뉘었다 |

### 이월된 리뷰 권장

| # | 출처 | 내용 | 담당 작업 |
|---|---|---|---|
| 1 | M3a3a 리뷰 권장 1 | `MediaFilter` 가 `String.strip()` 으로 다듬어 끝의 U+00A0(줄 바꿈 없는 공백)이 남는다 → 끝에 NBSP가 붙은 차단 작가가 빠져나간다. 설정 쪽 `BlockEntry` 도 `strip()` 이라 같은 구멍이 있다 | **T0** (1)(2) |
| 2 | M3a3a 리뷰 권장 2 | `LicenseClassifier` 의 구분자 `[\s_-]` 가 U+00A0·U+2007 등을 구분자로 보지 않는다 → `[\s\p{Z}_-]` | **T0** (3) |
| 3 | M3a3a 리뷰 권장 3 | Commons 파일 이름의 첫 글자는 대소문자를 가리지 않는다(MediaWiki가 제목 첫 글자를 대문자로 바꾼다). 차단 목록 `id` 비교에 반영 | **T0** (4) |
| 4 | M3a3 리뷰 권장 3 | 지금 테스트는 테스트용 `JsonMapper`(`MediaRecords.JSON`)로만 null 키를 확인한다. Spring이 실제로 쓰는 매퍼와 MockMvc 응답에서도 null 키가 남는지 단언 | **T2** (9) |
| 5 | M3a3 리뷰 권장 6 | 이미지는 `upload.wikimedia.org`(원본)와 썸네일 호스트에서 나온다. 코드와 허용 목록이 기대하는 호스트가 맞는지 확인하고 웹에 필요한 것을 적는다 | **T1** (8), 아래 "웹에 필요한 것" |
| 6 | 메모 | 제외 목록의 `EPA` 가 미국 환경보호청(US EPA)의 퍼블릭 도메인 사진도 뺀다 | 리스크 9 (알려진 한계, 작업 아님) |

## 목표

1. `GET /api/story/world` 의 `day_media` 를 Commons **사진**으로 채운다. 라이선스가 확인되지 않아도 출처·라이선스 상태·촬영 시점과 함께 직접 보인다. 촬영 시점을 모르는 사진도 "촬영 시점 미상"으로 보이고 그날 사진으로 분류되지 않는다.
2. `tragic` 사건이 있는 날에는 그날 사진을 찾지 않는다.
3. Commons 파일 메타데이터 → `MediaItem` 매핑(Commons 파일 매퍼)을 M3a3c(사건 사진)·M3a4b(Commons 동영상)·M4c가 재사용할 수 있게 만든다.
4. 미디어 조회가 실패하거나 늦어도 사건은 그대로 200으로 나가고 `partial: true` 만 켜진다.
5. M3a3a·M3a3 리뷰에서 넘어온 권장 1~5를 완료 기준으로 닫는다.

## 범위 밖

- **사건 사진(`events[].media`) → M3a3c** — PageImages, Commons 파일 제목 묶음, `tragic`·`hidden` 사건 제외, ±5년(`media.max-years-apart`), 같은 파일 중복, 사건 사진과 겹치는 `day_media` 빼기. 이 PR 뒤에도 사건 `media` 는 `[]` 다
- 형식·정규화·`date_relation`(M3a3), 파서·라이선스 상태·차단과 제외의 규칙(M3a3a) — 이 기능은 쓰기만 한다. 예외는 이월 권장 1~3의 빈틈 고침(T0)뿐이다
- 영상 → M3a4~M3a5b
- 사진 메타데이터 캐시 → 후속(필요하면 M3a4b 결과 캐시에 출처를 더한다). 요청마다 부른다(리스크 6)
- 미디어 파일 복사·프록시 (안전장치 1)
- 서버 쪽 이미지 호스트 허용 목록, 웹 CSP — 만들지 않는다(기록만, "웹에 필요한 것")
- 프론트엔드 → 변경 없음(M3b2·M3b2b가 이미 그린다)

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| Wikimedia Commons (Action API) | **확인됨** — [day-events.md 3절 Commons 카드](../research/day-events.md#3-출처-카드) | 날짜 범주 `Category:South Korea photographs taken on YYYY-MM-DD` 의 파일(`generator=categorymembers&gcmtype=file&prop=imageinfo&iiprop=url\|mime\|size\|extmetadata&iiurlwidth=640&iiextmetadatalanguage=ko`) | User-Agent·429·동시 3(`WikimediaHttpClient` 로만). 작가·라이선스·원문 페이지 표시. REST 요약·OTD의 `thumbnail` 을 쓰지 않는다. 썸네일은 API가 준 `thumburl` 만(표준 폭이 아닌 주소를 직접 만들면 429 — video-sources.md 리스크 4). 핫링크는 "가능하지만 권장하지 않음" — 안전장치 1에 따라 핫링크로만 쓰고 공개 배포 때 다시 본다. `extmetadata` 는 비싼 속성이라 한 번에 적은 파일만 묻는다 |

키 없음. 새 HTTP 클라이언트를 만들지 않는다. User-Agent는 연락처로 **저장소 주소만**(`https://github.com/ericmschoi/The-World-That-Day`) 쓴다 — 이메일을 넣지 않는다(녹화 `curl`, 테스트 상수 `WikimediaWireMockTest.TEST_USER_AGENT` 모두). 한국어 위키백과 PageImages는 M3a3c가 쓴다.

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| 미디어 표시·촬영 시점 미상·안전장치 1~6 | M3a3 계획서와 같음 | 결정 2026-10-11 |
| #13-C | (a) Commons 주소 그대로(핫링크, `no-referrer`) | 결정 |
| #21 | (a) 사건이 없는 날도 같은 날 사진(`day_media`)은 찾는다 | 결정 |
| #22 | (c) + `tragic` 사건엔 미디어 없음, `tragic` 이 있는 날은 `day_media` 없음 | 결정 |
| M3-P17 | (a) "그날 찍힌 사진"은 `same_day` 만 → `day_media` 순서 | 결정 |
| M3a3 분할 A, 확인점 | (a) 두 PR → 확인점 (a) 선행 M3a3a | 결정 2026-10-13 |
| M3a3a D1 | (b) 통신사 낱말 경계 원래 문구 — `AP통신`·`EPA연합` 은 걸리지 않음(알려진 한계) | 결정 2026-10-13 |
| M3a3b 결정 1·2 | 1 (a) 두 PR(이 PR + M3a3c), 2 (b) T2는 backend-dev | **결정 2026-10-14** |

## 이어받는 계약 (머지된 코드, `main` 9642d08)

### 세계 API (M3a2)

| 자리 | 지금 코드 | 이 기능에서 |
|---|---|---|
| 서비스 | `story.world.WorldStoryService(KoWikiEventSource source, EventToneJudge judge)`, `find(LocalDate)`: `source.find(date)` → `allUnavailable()` 이면 `ApiProblem(503, "UPSTREAM_UNAVAILABLE")` → `judge.judge(found)` → 판정마다 `hidden` 이면 빼고(ID 해시 8자리만 DEBUG), `Tone.NORMAL` 은 `normal`, 나머지는 `tragic` 목록 → `normal.addAll(tragic)` → `new WorldStoryResponse(date, events, List.of(), notice, day.degraded() \|\| judgement.partial())` | 판정 뒤에 미디어 조회를 부른다. `tragic` 목록이 비지 않았으면 그날 사진을 찾지 않는다. `dayMedia` 에 결과, `partial` 에 `\|\| 미디어 실패`. `Event.of` 는 그대로(M3a3c가 바꾼다) |
| 응답 | `WorldStoryResponse(String date, List<Event> events, List<MediaItem> dayMedia, Notice notice, boolean partial)`, `Event.of(KoWikiEvent, Verdict)` 는 `media` 에 `List.of()` | `dayMedia` 만 채움 |
| 판정 | `EventToneJudge.Verdict(Tone tone, Basis basis, Scope scope, boolean hidden)`, `Judgement(List<Verdict> verdicts, boolean partial)` | 그날 사진 차단: `hidden == false` 이고 `tone == TRAGIC` 인 사건이 하나라도 있음 |
| 에러 | `ApiProblem` → ProblemDetail + `code` | 새 에러 코드 없음. 미디어 실패는 늘 200 + `partial: true`. 503은 위키백과 두 문서가 모두 `UNAVAILABLE` 일 때만(그때는 미디어를 찾지 않음) |

### Wikimedia 호출 (M3a1~M3a2)

| 자리 | 지금 코드 | 이 기능에서 |
|---|---|---|
| HTTP | `wikimedia.WikimediaHttpClient.get(String source, String title, URI uri[, Duration readTimeout])` → `WikimediaResponse`(`Ok(String body)` / `Unavailable(Reason)`, 예외 없음). 공정 `Semaphore(wikimedia.max-concurrent-requests = 3)`, 빈자리 대기는 `wikimedia.read-timeout`(4s)까지, 인터럽트되면 `INTERRUPTED`. 실패 시 WARN 1줄(`source`·`title`·원인). 2xx 본문의 `ratelimited` 는 부르는 쪽이 `blockHost(uri)` | Commons도 이것으로만. 호출별 읽기 타임아웃 = `media.lookup-deadline`. `title` 에는 날짜·파일 이름 없는 이름표만 |
| 시간 상한 | `SparqlWikidataSignalSource.within(label, call)`(private): 가상 스레드 + `FutureTask`, `wikidata-read-timeout` 까지 기다리고 넘으면 `cancel(true)` 후 `CANCEL_GRACE`(1s) 동안 끝나기를 기다려, 끝나지 않았거나 실패 WARN이 없었을 때만 `cause=deadline` WARN 1줄 | 미디어 조회 전체에 같은 방식의 상한(`media.lookup-deadline`, 3s). 이 도우미를 `wikimedia` 패키지 공용(예: `Deadline`)으로 빼도 된다 — **기존 Wikidata 테스트를 고치지 않고 통과할 때만**. M3a3c가 같은 상한 안에 사건 사진 경로를 더한다 |
| 설정 묶음 | `WikimediaProperties`(`wikimedia`, `kowikiApiUrl` 검증), `WikidataProperties`(같은 `wikimedia` 접두어, 주소 검증) — `@ConfigurationPropertiesScan` | 새 `CommonsProperties`(`wikimedia.commons-api-url`, 기본 `https://commons.wikimedia.org/w/api.php`, 절대 `http(s)` 아니면 시작 실패) — `WikidataProperties` 와 같은 방식 |
| 테스트 기반 | `WikimediaWireMockTest`: WireMock 하나에 `wikimedia.kowiki-api-url` = `/w/api.php`, `wikidata-sparql-url` = `/sparql`, `read-timeout` 2s, `TEST_USER_AGENT`(저장소 주소), 매 테스트 전 리셋 | `wikimedia.commons-api-url` 을 **다른 경로**(예: `/commons/w/api.php`)로 더한다(T1) |

### 미디어 형식과 규칙 (M3a3·M3a3a, `media` 패키지)

| 부품 | 실제 모양 | 이 기능에서 |
|---|---|---|
| `MediaRecord` | 저장 형식 26필드 레코드(위치 인자 생성자). 열거형 `Type`·`DisplayMode`·`LinkOnlyReason`·`LicenseStatus`·`DatePrecision`, 값은 소문자 `@JsonValue` | Commons 파일 매퍼가 만든다(`type: photo`, `display_mode: image`) |
| `MediaItem` | `MediaItem(@JsonUnwrapped MediaRecord record, DateRelation dateRelation)` — 저장 필드를 평평하게, null 키 포함 | 응답 항목. 정규화만 만든다 |
| `DateRelation` | `DateRelation(Kind kind, int years)`, `Kind` = `SAME_DAY`…`UNKNOWN`, `years` 늘 있음 | `day_media` 순서 |
| `MediaNormalizer` | `@Component`, `normalize(MediaRecord, LocalDate requested)` → `Optional<MediaItem>`. R1~R9, 잘못된 `license_url` 만 `null` 로 고침, DEBUG `id`·`rule` 만 | 필터 다음에 부른다 |
| `MediaFilter` | `@Component`, `check(MediaRecord record, MediaOrigin origin)` → `Optional<Reason>`(`BLOCKED_ID`·`BLOCKED_AUTHOR`·`BLOCKED_CHANNEL`·`EXCLUDED_CREDIT`). 예외 없음, DEBUG에 이유 이름만 | 레코드를 만든 **뒤**, 정규화 **앞**. T0에서 빈틈 고침 |
| `MediaOrigin` | `MediaOrigin(String source, String itemId, String author, String credit, String channel)` | Commons: `source = "commons"`, `itemId` = 파일 이름(`File:` 없이), `author` = `Artist` 평문, `credit` = `Credit` 평문, `channel = null` |
| `CaptureTimeParser` | `static CaptureTime parse(String raw)` — 태그를 먼저 벗김, 못 읽으면 `CaptureTime.UNKNOWN`(`capturedAt`·`datePrecision` 모두 `null`), `isKnown()` | `DateTimeOriginal` 에 쓴다 |
| `LicenseClassifier` | `static LicenseStatus classify(String license)` — 빈 값 `UNKNOWN`, 허용 목록 `FREE`, 나머지 `OTHER` | `LicenseShortName` 에 쓴다. T0에서 구분자 고침 |
| `MediaText` | `final class`(패키지 전용), `static boolean isBlank(String)` — `Character.isWhitespace \|\| Character.isSpaceChar` | T0에서 공개로 바꾸고 같은 집합으로 양끝을 다듬는 메서드를 더한다(Commons 매퍼가 `media` 패키지 밖이면 필요) |
| `MediaProperties` | `@ConfigurationProperties("media")` 레코드: `allowedImageEmbedProviders`, `allowedVideoEmbedProviders`, `blocklist`(`BlockEntry(source, id, author, channel)`), `excludedCredits` + 2인자 생성자 | **고치지 않는다**(생성자를 부르는 테스트 15곳). 조회 설정은 새 묶음 |
| 테스트 도우미 | `media.MediaRecords`(엄격한 테스트 `JsonMapper`, `STORED_KEYS` 26개), `data.DataSchemas`(스키마 검증, 테스트 범위 — 결정 B (a)) | 응답 미디어를 `date_relation` 을 뺀 뒤 스키마로 검증 |

## Commons 사진 규칙

| 검사 | 결과 |
|---|---|
| Commons에 없음(응답 페이지의 `missing` — 위키 로컬 파일) / `extmetadata.NonFree` 값이 있음(빈 값·`false` 제외) | 제외 |
| `MediaFilter.check` 가 이유를 돌려줌(차단 목록·통신사·언론사 표기, M3a3a 규칙) | 제외 |
| MIME이 `image/jpeg` 가 아님 | 제외 (로고·지도·국기 거르기. PNG 사진도 빠진다 — 리스크 5) |
| `LicenseShortName` | `LicenseClassifier.classify` 로 `free`/`other`, 없거나 빈 값이면 `license: null`·`unknown` |
| `Restrictions` 있음 | 표시(거르지 않음) |
| `DateTimeOriginal` | `CaptureTimeParser.parse`. 못 읽으면 `captured_at`·`date_precision` null, `kind` `unknown` (그래도 표시, 그날 사진으로 분류하지 않음) |
| 정규화(R1~R9)에서 버려짐 | 제외 |

Commons 날짜 범주의 날짜를 촬영 시점으로 옮기지 않는다.

필드: `type: photo`, `display_mode: image`, `id` = `commons-` + `pageid`, `url` = `imageinfo.url`, `thumbnail_url` = `thumburl`(`iiurlwidth=640`), `thumbnail_width`·`thumbnail_height` = `thumbwidth`·`thumbheight`, `source_name` = `Wikimedia Commons`, `source_url` = `descriptionurl`, `license` = `LicenseShortName`(다듬은 값), `license_url` = `LicenseUrl`, `attribution` = `Artist`(평문, 없으면 `Credit`, 둘 다 없거나 빈 값이면 "작가 정보 없음") + ` · ` + (`license` 또는 "라이선스 확인 안 됨") + ` · Wikimedia Commons`, `description` = `ImageDescription` 평문(`iiextmetadatalanguage=ko` 로 받은 값, 200자(코드 포인트)에서 자름, 빈 값이면 `null`), 위치 = GPS(`GPSLatitude`·`GPSLongitude`)가 둘 다 있고 범위 안이면 좌표 + `place_ref: null`, 아니면 부르는 쪽이 준 `place_ref`(날짜 범주 사진은 `country:KR`. 사건 사진은 M3a3c가 사건 `scope` 로 준다), `retrieved_at` = 응답을 받은 시각(주입된 `Clock`, UTC 초 단위).

- **평문**: HTML 태그를 벗기고 엔티티를 풀고(예: Spring `HtmlUtils.htmlUnescape`), `MediaText` 공백 집합의 연속을 공백 하나로, 양끝을 같은 집합으로 다듬는다. 결과가 `MediaText.isBlank` 면 "없음"으로 본다.
- **주소는 그대로**: `url`·`thumburl`·`descriptionurl`·`LicenseUrl` 은 API 값을 글자 하나 바꾸지 않고 옮긴다. 호스트를 바꾸거나 주소를 조립하지 않는다(이월 권장 5).
- **순서**: 레코드 만들기 → `MediaFilter.check(record, origin)` → 비었으면 `MediaNormalizer.normalize(record, date)`.
- **Commons 파일 매퍼**는 MIME 거르기와 위치 대체값(`place_ref`)을 부르는 쪽이 정한다(이 기능은 `image/jpeg`·`country:KR`, M3a3c는 `image/jpeg`·사건 `scope`, M3a4b는 동영상 MIME). 그 밖의 검사(`missing`, `NonFree`, 필터·정규화 호출, 작가·라이선스·설명·촬영 시점·위치)는 매퍼 안에 둔다. 입력은 응답의 페이지 하나, MIME 조건, 위치 대체값, 요청 날짜다. 응답 페이지 목록을 읽는 부분은 M3a3c의 파일 제목 묶음 응답(같은 `query.pages` 모양)에도 그대로 쓸 수 있게 둔다.

### 고르는 법

| 자리 | 후보 | 개수·규칙 |
|---|---|---|
| `day_media` | `Category:South Korea photographs taken on <날짜>` 의 파일, **파일 제목 순** | `gcmlimit` 20, 한 번만(이어받기 `continue` 안 함). 최대 `media.day-media-limit`(6). 응답에 남은 사건 중 `tragic` 이 하나라도 있으면 0이고 날짜 범주 요청도 0. 사건이 0개인 날도 찾는다(#21 (a)). 범주가 없으면(`query` 없음) `[]` 이고 실패가 아니다. 순서: `same_day` 먼저(제목 순), 그다음 그 밖의 `kind`(제목 순), `unknown` 맨 뒤(M3-P17 (a)). 정렬 뒤 6개를 자른다. 사건 사진과 겹치는 파일 빼기는 M3a3c |

요청 수(API 호출 1회): Commons 최대 1(날짜 범주). M3a3c가 PageImages 최대 1, Commons 파일 제목 묶음 최대 1을 더한다.

### 실패와 시간

| 규칙 | 내용 |
|---|---|
| 시간 상한 | 미디어 조회 전체를 `media.lookup-deadline`(3초) 안에서 끝낸다. `within` 과 같은 방식: 넘으면 `cancel(true)`, 최대 1초 취소 유예 뒤에도 안 끝났을 때만 WARN 1줄 |
| 호출별 읽기 타임아웃 | `WikimediaHttpClient.get(…, readTimeout)` 에 `media.lookup-deadline` 을 넘긴다(타임아웃 뒤 재시도는 사실상 없음) |
| 일부 실패 | Commons 실패·시간 초과·`ratelimited`·읽을 수 없는 본문 → 사건은 그대로, `day_media` = `[]`, `partial: true`, 200. 차단·제외·규칙 위반으로 버린 미디어, `tragic` 으로 비운 미디어, 범주가 없는 날은 `partial` 이 아니다 |
| 503 | 바뀌지 않음 |
| 세계 섹션 최악 시간 | 위키백과 5초 + Wikidata 5초(+ 드물게 1초 유예) + 미디어 3초(+ 드물게 1초) ≈ 13초(최대 15초). 제품 정의서 8절(10초)을 넘을 수 있다 — M6b 측정, 리스크 3 |

### 로그

- `WikimediaHttpClient` 에 넘기는 `title` 은 이름표만: `commons day category`. 날짜·범주 이름·파일 이름을 넣지 않는다(파일·범주 이름에 날짜가 들어간다). M3a3c가 `commons files of <개수>`, `kowiki pageimages of <개수>` 를 더한다.
- 미디어를 버린 이유는 DEBUG에 이유별 **개수만**(`MediaFilter`·`MediaNormalizer` 의 기존 DEBUG 줄은 그대로 — `id` 는 `commons-<pageid>` 라 날짜가 없다). 사건을 가리킬 때는 M3a2처럼 ID 해시 8자리만.

## 웹에 필요한 것 (이월 권장 5 — 기록만, 이 기능은 웹을 고치지 않는다)

- 원본 `url` 은 `upload.wikimedia.org`, 썸네일 `thumburl` 은 `thumb.wikimedia.org`(`iiurlwidth=640` 요청이 960px 표준 폭 주소로 맞춰짐)로 나온다 — M3a3 예시(`commons-1988-09-17-seoul-olympics-opening-pd.json`)와 video-sources.md V1-8 기준. `thumb.wikimedia.org` 도메인의 공식 공지는 없다(video-sources.md 리스크 5) → API 값을 그대로 쓰면 영향이 없다. T1 녹화로 실제 호스트를 다시 확인한다.
- 서버에는 이미지 호스트 허용 목록이 **없다**. R9(`MediaNormalizer`)는 `http(s)` 만 본다.
- 웹도 허용 목록·CSP가 **없다**(M3b1b `isDisplayable` 은 `http(s)` 만). M3b2 fixture가 이미 `thumb.wikimedia.org` 주소를 그려 이 호스트가 걸러지지 않음을 시험한다. 사진 `img` 는 요소별 `referrerpolicy="no-referrer"`.
- 나중에 `img-src` CSP나 허용 목록을 두면(M6b·공개 배포 #5) `https://upload.wikimedia.org` 와 `https://thumb.wikimedia.org` 를 넣는다(로드맵 후속 후보 "이미지 호스트 허용 목록·CSP"). T1 녹화에 다른 호스트가 보이면 그 목록에 더한다.
- `thumbnail_width` 는 요청 폭(640)이고 실제 파일은 960px일 수 있다 — 브라우저가 줄여 보인다. 웹은 가로세로 비율로만 쓴다(지금 그대로).

## API / 데이터 변경

- `GET /api/story/world`: `day_media` 에 M3a3 형식의 사진 레코드가 들어간다. `partial` 에 미디어 실패가 더해진다. 사건 `media` 는 그대로 `[]`(M3a3c). 새 에러 코드 없음. 응답 키·형은 그대로다(M3b2·M3b2b 웹 계약 변화 없음).
- 설정 추가 (`application.yml`, 잘못된 값은 시작 실패):
  - `wikimedia.commons-api-url: https://commons.wikimedia.org/w/api.php` (`CommonsProperties`)
  - `media.day-media-limit: 6`(0 이상), `media.lookup-deadline: 3s`(0보다 큼) (`MediaLookupProperties`)
- 서버 안 부품(이름은 예시, 머지 코드 이름은 보고서에 적는다 — M3a3c가 이어서 쓴다):
  - Commons 클라이언트: `dayCategory(LocalDate)` → 페이지 목록 또는 실패(예외 없음)
  - Commons 파일 매퍼: `map(JsonNode page, Predicate<String> mime, String placeRef, LocalDate date)` → `Optional<MediaItem>` + 버린 이유
  - 미디어 조회(어댑터 인터페이스 뒤, 예외 없음): `dayMedia(LocalDate, boolean tragicDay)` → `(List<MediaItem>, boolean failed)`
- DB 변경 없음. 데이터 파일 없음(fixture는 테스트 리소스).

`day_media` 항목 하나(키 27개 = 저장 26 + `date_relation`, 값이 없는 키도 `null` 로 남음. 값은 녹화로 정해진다):

```
{"id": "commons-<pageid>", "type": "photo", "display_mode": "image",
 "url": "https://upload.wikimedia.org/…", "thumbnail_url": "https://thumb.wikimedia.org/…/960px-….jpg",
 "thumbnail_width": 640, "thumbnail_height": …, "video_sources": null, "duration_seconds": null,
 "embed_url": null, "provider": null, "render": null, "link_only_reason": null,
 "source_name": "Wikimedia Commons", "source_url": "https://commons.wikimedia.org/wiki/File:…",
 "license": "Public domain", "license_status": "free", "license_url": null,
 "attribution": "<작가> · Public domain · Wikimedia Commons", "description": "…",
 "captured_at": "1988-09-17", "date_precision": "day", "lat": null, "lng": null, "place_ref": "country:KR",
 "retrieved_at": "2026-…Z", "date_relation": {"kind": "same_day", "years": 0}}
```

## 테스트 방침

- M3a1 "테스트 방침"을 따른다. **외부 네트워크를 부르지 않는다** — WireMock + 녹화 fixture. 컨텍스트 테스트의 모든 `wikimedia.*-url` 은 WireMock을 가리킨다.
- 녹화 fixture: `fixtures/wikimedia/commons/` 에 데모 3날짜 날짜 범주 응답. `README.md` 에 원 URL·녹화 일자·User-Agent(저장소 주소)·라이선스(응답 텍스트는 Commons 설명 페이지 라이선스 CC BY-SA 4.0, 파일 메타데이터는 각 파일의 라이선스 표기 그대로). 바이트를 고치지 않는다. **이미지 파일 자체는 내려받지 않는다**(메타데이터 JSON만).
- 합성 fixture로 규칙 표의 행마다 시험. 실제 비극 사건 이름을 쓰지 않는다. 표본 날짜는 데모 3날짜(제품 정의서 12절)와 공개 역사 사건 날짜·경계값(`CLAUDE.md` 7절 6항, 예: 2000-02-29). 실제 사람의 생년월일을 쓰지 않는다.
- 공백 시험 값(U+00A0, U+2007, U+202F, U+3000 등)은 자바 문자열·JSON에서 유니코드 이스케이프로 적어 눈에 보이게 한다. 보이지 않는 원문자를 소스에 넣지 않는다(M3a3a 방침).
- 응답 미디어 전부를 `date_relation` 을 뺀 뒤 M3a3 스키마로 검증(`DataSchemas`).
- 날짜 기준 테스트는 기존 고정 시계(`MutableClock`, `Asia/Seoul`)를 쓴다.

## 데모 입력 3개로 확인할 것

사건·`tone` 은 M3a2 결과 그대로다. 그날 사진 값은 **T1 녹화** 결과로 정하고 T2가 그 값으로 고정 단언한다. 사건 `media` 는 이 PR에서 모두 `[]` 다(M3a3c).

| 데모 입력 | 사건 (M3a2) | 확인 (T2) |
|---|---|---|
| 1988-09-17 + "서울" | 3개, 모두 `normal`/`curated`/`domestic`: ① `1988년 하계 올림픽`(`Q8470`) ② `KBS 1TV`(`Q777278`) ③ `KBS 2TV`(`Q624509`, 문장 원문 그대로) | `day_media` 에 날짜 범주 사진(리서치 기준 5장)이 **모두** `display_mode: image`. 한 날짜로 읽히는 파일은 `same_day`, 값이 둘인 파일도 빠지지 않고 `unknown`·`date_precision` null·`captured_at` null이며 `same_day` 파일들 **뒤에** 온다. PD·CC BY-SA → `license_status: free`. 사건 `media` 는 모두 `[]`. Commons 요청 1. `partial: false` |
| 2002-06-22 + "광주 서구 풍암동" | 1개: `2002년 FIFA 월드컵`(`Q47735`), `normal`/`curated` | `day_media` 1장, `license` `KOGL Type 1`, `free`, `same_day`, `attribution` 이 `Artist` 평문(리서치 기준 "기증자")으로 시작하고 ` · KOGL Type 1 · Wikimedia Commons` 로 끝난다. `partial: false` |
| 1995-03-15 + "서울대학교병원" | 0개, `notice.code` = `NO_EVENTS_FOUND` | Commons 날짜 범주 요청 1 → `day_media` = `[]`(녹화 — 범주 없음). SPARQL 요청 0(M3a2 그대로). `partial: false` |
| (합성) `tragic` 날 | `tragic`(`default` 포함) 사건이 하나 있음 | `day_media` = `[]`, 날짜 범주 요청 0 |

## 크기 (400줄 규칙)

제품 코드·설정(S1) 기준.

### 실측 비율

planner 실행에 셸이 없어 `git log`·`git diff` 를 직접 돌리지 못했다. 아래는 `docs/progress.md` 와 계획서에 적힌 실측이다.

| 기능 | 이전 방식 | ×2.8 다시 추정 | 실제 S1 | 이전 방식 대비 | 다시 추정 대비 |
|---|---|---|---|---|---|
| M3a3 (PR #12) | 약 130 (T2) | 약 365 | **505**(T2 뒤 확인점, 4d8929c — 뒤의 414863c 정규화 맞춤으로 조금 늘었을 수 있음) | ×3.9 | ×1.38 |
| M3a3a (PR #14) | 약 140(130 + 리뷰 권장 10) | 약 395 | **456**(2c77589 뒤 최종, a1100fd는 테스트만) | ×3.3 | ×1.15 |

### 다시 셈

나누기 전 전체는 약 715(640~860)였다(비율로 660~780, 머지된 코드 모양으로 파일별 셈). PM 결정 1 (a)로 이 PR은 아래 1부만이다. 사건 사진(약 230)은 M3a3c.

| 작업 | 내용 | 다시 셈 | 범위 |
|---|---|---|---|
| T0 | 이월 권장 1~3, `MediaText` 공개·다듬기 메서드 | 약 25 | 20~35 |
| T1 | `CommonsProperties`(약 30), Commons 클라이언트의 날짜 범주 질의·페이지 읽기·실패 처리(약 100), Commons 파일 매퍼(약 145), 설정 5 | 약 280 | 250~330 |
| T2 | 그날 사진 조회(약 70), 시간 상한(약 45 — 공용으로 빼면 순증 약 30), 서비스 연결(약 25), `MediaLookupProperties` 2개(약 30), 설정 8 | 약 180 | 150~220 |
| **합계** | | **약 485** | **420~585** |

- 400을 넘는다(M3a3 505·M3a3a 456과 비슷). PM 결정 1 (a)로 한 PR이고, 확인점 두 개를 둔다.
- **확인점 1 (알림): T0·T1 뒤 누적 S1이 350을 넘으면** 오케스트레이터가 PM에게 **알리고 계속한다**(PM이 멈추라고 하면 멈춘다).
- **확인점 2 (멈춤): T2 뒤 누적 S1이 450을 넘으면** 오케스트레이터가 **T3 전에 멈추고** PM에게 어떻게 할지 묻는다(예: 그대로 T3로 / T2의 일부를 M3a3c로 옮김).
- 테스트(S2)·fixture는 따로 보고한다.

## 작업 목록

순서: T0 → T1 → **확인점 1 (350, 알림)** → T2 → **확인점 2 (450, 멈춤)** → T3 → T4(PR 검수 때) → 머지 → M3b2b T6(`main` 에서). 사건 사진은 이 PR 머지 뒤 M3a3c.

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | M3a3a 리뷰 권장 1~3: 차단 비교와 라이선스 구분자의 공백 빈틈, Commons 첫 글자 | backend-dev | - | (1) `MediaFilter` 의 `source`·`author`·`channel`·`itemId` 비교와 `MediaProperties.BlockEntry` 의 `id`·`author`·`channel` 다듬기가 `String.strip()` 대신 `MediaText` 공백 집합(`Character.isWhitespace \|\| Character.isSpaceChar`)으로 양끝을 다듬는다. 합성 테스트: 차단 `author` 가 `Example Author` 일 때 원문 작가 "`Example Author` + 끝 U+00A0", "앞 U+3000 + `Example Author`", "`Example Author` + 끝 U+202F" → 모두 `BLOCKED_AUTHOR`. 차단 `author` 설정값 끝에 U+00A0이 붙어 있어도 원문 `Example Author` 를 막는다. `channel`·`id` 도 끝 U+00A0 한 경우씩 같은 시험. (2) 기존 `MediaFilter*Test`·`MediaPropertiesTest`·`MediaBlankParityTest` 를 **고치지 않고** 통과. (3) `LicenseClassifier` 구분자가 `[\s\p{Z}_-]+` 이고, 구분자를 공백으로 바꾼 **뒤** 양끝을 다듬는다: "`CC` U+00A0 `BY-SA` U+00A0 `3.0`", "`CC BY` U+2007 `4.0`", "`Public domain` + 끝 U+00A0" → `free`, "`CC` U+00A0 `BY-NC 4.0`" → `other`, U+00A0·U+3000만으로 된 값 → `unknown`. 기존 `LicenseClassifierTest` 그대로 통과. (4) Commons `id` 비교에서 첫 글자(코드 포인트 하나)만 대소문자를 가리지 않는다: 차단 `id` `example photo.jpg` 가 `itemId` `Example photo.jpg`·`File:example_photo.jpg` 를 막고, `Example Photo.jpg` 는 막지 **않는다**(둘째 글자부터는 그대로). Commons 밖 출처(`youtube`)의 `id` 는 대소문자를 그대로 비교한다. (5) `MediaText` 가 공개 클래스이고 `isBlank` 와 양끝 다듬기 메서드(이름 자유)가 공개다 — T1 매퍼가 쓴다. (6) `application.yml` 차단 목록 주석에 "Commons 파일 이름의 첫 글자는 대소문자 무시"를 한 줄 더한다. (7) `cd server && ./gradlew check` 성공. (8) 보고서에 S1(목표 35 이하) |
| T1 | Commons 날짜 범주 클라이언트·파일 매퍼, 녹화 fixture | backend-dev | T0 | (1) `CommonsProperties`(`wikimedia.commons-api-url`, 기본 `https://commons.wikimedia.org/w/api.php`) — 상대 주소·`ftp:` 는 시작 실패(오류 메시지에 설정 이름). `application.yml` 에 값과 주석. (2) 날짜 범주 질의: `generator=categorymembers&gcmtitle=Category:South Korea photographs taken on <YYYY-MM-DD>&gcmtype=file&gcmlimit=20&prop=imageinfo&iiprop=url\|mime\|size\|extmetadata&iiurlwidth=640&iiextmetadatalanguage=ko&format=json&formatversion=2` 가 WireMock 요청에 그대로 보인다. `query` 가 없는 응답(범주 없음) → 빈 목록(실패 아님). `error` 가 있는 응답·JSON이 아닌 본문 → 실패, `error.code == "ratelimited"` 이면 `blockHost` 도 부름. (3) 모든 요청이 `WikimediaHttpClient` 로 나가고(새 HTTP 클라이언트 없음) `User-Agent` 헤더가 `TEST_USER_AGENT`(저장소 주소, 이메일 없음)이며, 호출별 읽기 타임아웃으로 `media.lookup-deadline` 을 넘긴다(T2 전에는 상수나 인자로 받아도 된다). (4) "Commons 사진 규칙" 표의 행마다 합성 테스트: `NonFree`·`missing`·비JPEG → 제외, 차단 목록에 넣은 파일 이름(합성) → 제외, `Credit` 이 `AP Photo/…` → 제외(`MediaFilter` 를 거침 — 순서: 레코드 → 필터 → 정규화), 허용 라이선스 → `free`, 다른 이름 → `other`, 라이선스 없음·빈 값 → 표시 + `license: null`·`unknown`, `Restrictions` 있음 → 표시, `Artist` 없음 → `Credit`, `Artist` 가 `&nbsp;` 뿐 → `Credit`, 둘 다 없음 → "작가 정보 없음", `Artist` HTML → 평문, 설명 201자 → 200자, GPS 있음 → 좌표·`place_ref` null, GPS 범위 밖 → 받은 `place_ref`, 없음 → 받은 `place_ref`, `DateTimeOriginal` 두 날짜 → `captured_at` null·`date_precision` null. (5) `MediaOrigin` 이 `("commons", "File:" 뺀 파일 이름, Artist 평문, Credit 평문, null)` 로 채워진다(합성 테스트로 확인). (6) 만든 항목은 모두 `MediaNormalizer` 를 지나고 `date_relation` 을 뺀 뒤 스키마(`DataSchemas`) 통과. `retrieved_at` 은 주입된 `Clock` 의 초 단위 UTC. (7) `WikimediaWireMockTest` 가 `wikimedia.commons-api-url` 을 WireMock의 **위키백과와 다른 경로**(예: `/commons/w/api.php`)로 돌리고, 기존 컨텍스트 테스트가 고치지 않고 통과한다. (8) **이월 권장 5 (호스트)**: 매퍼가 `url`·`thumburl`·`descriptionurl`·`LicenseUrl` 을 글자 그대로 옮긴다 — 합성 테스트: `thumburl` = `https://thumb.wikimedia.org/wikipedia/commons/thumb/…/960px-….jpg`, `url` = `https://upload.wikimedia.org/…` 인 페이지 → 같은 문자열의 `thumbnail_url`·`url` 로 정규화를 통과해 나온다. 서버 코드에 이미지 호스트 허용 목록이나 주소 조립이 없다. 보고서에 녹화된 모든 파일의 `url`·`thumburl` 호스트 목록(예상: `upload.wikimedia.org`·`thumb.wikimedia.org`)과 `thumburl` 의 폭 단계(예상: 960px) — 다른 호스트가 있으면 따로 적는다. (9) WireMock 503 → WARN 줄의 `title` 이 `commons day category` 이고, 로그 어디에도 `YYYY-MM-DD` 모양·`File:`·`Category:` 가 없다. (10) 녹화 fixture: 데모 3날짜 날짜 범주 응답(`fixtures/wikimedia/commons/`) + `README.md`(원 URL·녹화 일자·User-Agent 저장소 주소·라이선스). 이미지 파일은 받지 않는다. 네트워크를 쓸 수 없으면 "막힘"으로 보고하고 PM이 실행할 `curl` 명령(`-H "User-Agent: $WIKIMEDIA_USER_AGENT"`)을 적는다. (11) 보고서에 데모 3날짜 날짜 범주 결과 표(제목, MIME, `LicenseShortName`, `DateTimeOriginal` 원문, 읽힌 `captured_at`·정밀도, 남는지 여부)와 T0·T1 누적 S1 — **확인점 1 (350, 알림)**. (12) 보고서에 M3a3c가 이어 쓸 공개 부품 이름(클라이언트·매퍼·페이지 읽기 메서드). (13) `./gradlew check` 성공 |
| T2 | ★ 세계 API 연결: 그날 사진, `tragic` 날, 시간 상한, `partial` | backend-dev (PM 결정 2 (b)) | T1 | (1) "데모" 표 4행 통과(값은 T1 녹화로 고정). (2) 데모 3날짜 응답의 모든 미디어가 `date_relation` 을 뺀 뒤 스키마 통과. (3) 순서: 합성 범주에 `same_day`·`years_apart`·`unknown` 이 섞여 있으면 `day_media` 가 `same_day`(제목 순) → 그 밖(제목 순) → `unknown` 순서. 촬영 시점을 못 읽은 파일은 `day_media` 에 남고 `captured_at` null·`kind` `unknown`(범주의 날짜가 들어가지 않음). (4) 범주 파일 8개(모두 통과) → `day_media` 6개, 중복 `id` 없음. `media.day-media-limit: 0` → 범주 요청 0, `[]`. (5) `tragic`: `Verdict.tone() == TRAGIC` 인 사건(`tone_basis` `default` 포함)이 응답에 하나라도 있으면 `day_media` = `[]` 이고 Commons 요청 0. `hide` 로 내린 사건만 `tragic` 판정이면 그날 사진을 찾는다(`hidden` 은 판단에 넣지 않음). (6) Commons 503·`ratelimited` 본문 → 200, 사건 그대로, `day_media` = `[]`, `partial: true`. 범주 없음 → `partial: false`. 두 위키백과 문서 `UNAVAILABLE` → 여전히 503 `UPSTREAM_UNAVAILABLE`(problem+json), Commons 요청 0. (7) 시간 상한: 응답하지 않는 Commons 스텁(지연 > `lookup-deadline`) → `media.lookup-deadline` + 1초 + 여유(1초) 안에 200, `partial: true`, 그 실패의 WARN이 1줄. 시간 상한 도우미를 공용으로 뺐다면 기존 `SparqlWikidataSignalSource*Test` 를 고치지 않고 통과. (8) 설정: `MediaLookupProperties`(이름 예시)에 `media.day-media-limit`(음수면 시작 실패)·`media.lookup-deadline`(0 이하면 시작 실패, 오류 메시지에 설정 이름). `MediaProperties` 생성자는 바뀌지 않는다. (9) **이월 권장 4 (null 키)**: Spring 컨텍스트의 JSON 매퍼(MVC가 쓰는 `JsonMapper` 빈)로 `MediaItem` 하나를 쓰면 키가 `MediaRecords.STORED_KEYS` 26개 + `date_relation` 이고 `video_sources`·`duration_seconds`·`embed_url`·`provider`·`render`·`link_only_reason` 키가 `null` 값으로 **있다**. MockMvc `GET /api/story/world?date=1988-09-17` 응답의 `day_media` 모든 항목도 같은 키 집합이고, `unknown` 항목은 `captured_at`·`date_precision` 키가 `null` 값으로 있다. `spring.jackson.*` 포함 규칙 설정을 더하지 않는다. (10) 기존 M3a2 `WorldStory*Test` 의 **단언은 고치지 않는다**. 미디어 요청 때문에 바꿀 수 있는 것은 준비 코드(Commons 빈 범주 스텁 추가, 서비스 생성자 인자)와 요청 수 단언의 대상 경로 한정뿐이고, 바꾼 테스트 줄을 보고서에 모두 적는다. (11) 사건 `media` 는 모두 `[]`. 응답 어디에도 `display_mode: link_only` 가 없다. 로그에 날짜·파일 이름·전체 사건 ID가 없다(ID 해시 8자리만). (12) `./gradlew check` 성공. (13) 보고서에 누적 S1 — **확인점 2 (450 넘으면 T3 전에 멈춤)** |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | T2 (확인점 2 통과) | (1) T0·T1·T2 기준 표(기준마다 테스트 이름). 빠진 기준은 테스트를 더한다(제품 코드는 고치지 않음). (2) `cd server && ./gradlew check` 통과, 3회 반복 안정. (3) S1·S2·S3(0이 정상) 보고. (4) 데모 3날짜 `day_media` 표(순서, `id`, `display_mode`, `license`, `license_status`, `captured_at`, `date_precision`, `date_relation`, `url`·`thumbnail_url` 호스트). (5) **서버 미디어 DTO 필드 목록(이름·형·null 가능)** 이 M3a3 T4 목록과 같은지(웹 `web/src/world/types.ts`·M3b2b 응답 모양 검사와 대조), 다르면 차이. (6) 컨텍스트 테스트 전체에서 WireMock이 받지 않은 바깥 요청이 0임을 보이는 방법(예: 모든 `*-url` 이 WireMock 주소인지 단언). (7) 스텁으로 잰 세계 섹션 최악 시간(위키백과·Wikidata·Commons 모두 상한까지 늦을 때) |
| T4 | 실제 호출 확인 (PR 검수 때) | PM | T3 | (1) `docker compose up -d db`, `cd server && ./gradlew bootRun` 이 뜬다(루트 `.env` 의 `WIKIMEDIA_USER_AGENT` 는 저장소 주소만, ASCII). (2) P1~P3 결과가 "데모" 표와 맞는다. (3) `thumbnail_url` 하나를 브라우저로 열어 이미지가 보이고 `source_url` 페이지의 라이선스가 `license` 와 같다. (4) `date_relation.kind` 가 `unknown` 인 항목의 `captured_at` 이 `null` 이다. (5) 머지 뒤 **M3b2b T6**(그날 사진 묶음 화면 확인)을 `main` 에서 한다 |

완료 기준에 쓰는 명령 (저장소 루트):

```
# S1 — 제품 코드·설정 줄 수
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
# S3 — 데이터 파일 줄 수 (0이 정상)
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P1~P3 — PM 실제 호출 (서버 실행 중)
curl -s 'http://localhost:8080/api/story/world?date=1988-09-17' \
  | jq '{partial, events: [.events[] | {title, tone, media}], day_media: [.day_media[] | {display_mode, license, license_status, captured_at, date_relation, thumbnail_url}]}'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' \
  | jq '{partial, day_media: [.day_media[] | {display_mode, license, attribution, date_relation}]}'
curl -s 'http://localhost:8080/api/story/world?date=1995-03-15' | jq '{events, day_media, notice, partial}'
```

담당별: backend-dev 3 (T0, T1, T2), test-engineer 1 (T3), PM 1 (T4). 모두 5개.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 시점 |
|---|---|---|
| **M3a3c** 사건 사진 `feat/event-photos` | Commons 클라이언트(파일 제목 묶음 질의를 더함)·파일 매퍼·`MediaLookupProperties`·시간 상한·미디어 조회 연결 자리 | 이 PR 머지 뒤 |
| **M3b2b T6** (PM, 웹 그날 사진 묶음·캡션 실제 확인) | 실제 `day_media` | 이 PR 머지 뒤 `main` 에서 |
| M3b2 T6 (PM, 웹 사건 사진 실제 확인) | — | M3a3c 머지 뒤 `main` 에서 |
| M3a4 영상 검색 틀 | — (같은 `WorldStoryService` 연결 자리) | 병렬이면 worktree를 나누고 오케스트레이터가 합친다(리스크 7) |
| M3a4b Commons 동영상 | Commons 클라이언트·파일 매퍼(MIME 조건을 동영상으로) | M3a4 + 이 PR 머지 뒤 (M3a3c는 필요 없음) |
| M4c 동네 사진 | Commons 파일 매퍼 | M4 |

## 리스크와 확인할 것

1. **저작권 (PM이 알고 정함, 2026-10-11)** — 라이선스가 확인되지 않은 사진을 직접 보여 준다. 비상업·로컬 데모, 복사 없이 핫링크만, 출처·라이선스 상태 표시, 통신사·언론사 직접 표시 금지, 차단 목록으로 위험을 줄인다. 공개 배포 전에 다시 본다(로드맵 리스크 4).
2. **핫링크** — Commons는 권장하지 않는다. 로컬 데모라 요청이 적다. 브라우저는 `referrerpolicy="no-referrer"`(M3b1b). 썸네일 호스트 `thumb.wikimedia.org` 는 공식 공지가 없다 — API 값을 그대로 쓰므로 바뀌어도 따라간다("웹에 필요한 것").
3. **세계 섹션 시간** — 위키백과 5초 + Wikidata 5초(+1초 유예) + 미디어 3초(+1초 유예)로 최악 약 13~15초. 제품 정의서 8절(10초)과 웹 시간 초과(10초, M3b2)를 넘을 수 있다. 공용 `Semaphore(3)` 도 줄을 세운다. M6b에서 재고, 넘으면 섹션 전체 상한(로드맵 후속 후보)을 제안한다. T3 (7)에 스텁 측정값을 남긴다.
4. **보수적 `tragic` 이 그날 사진을 지운다** — Wikidata가 실패하거나 QID가 없는 날(큐레이션 밖 사건이 `default` → `tragic`)에는 그날 사진이 없어진다. 의도된 방향(6.3절)이다. `hide` 로 내린 사건은 `tragic` 날 판단에 넣지 않는다. 실제 비극 줄을 권리자 요청으로 내린 드문 경우에는 그날 사진이 보일 수 있다 — 그때는 PM이 날짜 범주 사진을 차단 목록에 함께 넣는다.
5. **JPEG만** — PNG 사진도 빠진다.
6. **요청마다 Commons를 부른다** — 사진 메타데이터 캐시가 없다. 로컬 데모라 분당 200 제한에 닿지 않는다. 필요하면 M3a4b 결과 캐시에 사진 출처를 더한다(후속).
7. **같은 연결 자리** — M3a3c·M3a4가 모두 `WorldStoryService` 의 미디어 연결을 고친다. 병렬이면 worktree를 나누고 오케스트레이터가 합친다. 기본은 M3a3b → M3a3c 순서.
8. **크기** — 약 485(420~585)로 400을 넘는다. T0·T1 뒤 350에서 알리고, T2 뒤 450을 넘으면 T3 전에 멈춘다.
9. **알려진 한계 — 제외 목록의 안전 쪽 오탐 (이월 메모 6)**: `media.excluded-credits` 의 `EPA` 는 낱말 단위라 `Credit`·`Artist` 에 `US EPA` 처럼 적힌 **미국 환경보호청의 퍼블릭 도메인 사진**도 뺀다. 같은 이유로 `Getty` 는 `J. Paul Getty Museum` 의 공개 소장품 사진도 뺄 수 있다. 통신사 사진을 놓치는 것보다 안전한 쪽이라 작업으로 두지 않는다. 데모 날짜에서 실제로 빠진 사진이 보이면 T3 표에 적고, 자주 보이면 M3a3a D1 후속(로드맵 "통신사 한글 붙은 표기")과 함께 다시 정한다.
10. **M3a2 테스트와의 충돌** — 미디어 조회가 붙으면 기존 세계 API 컨텍스트 테스트에 Commons 요청이 생긴다. 스텁이 없으면 404 → `partial: true` 로 기존 단언이 깨질 수 있다. 대응: Commons 경로 분리(T1 (7)), 단언은 고치지 않고 준비 코드만(T2 (10)).
11. **차단 비교 정규화의 범위** — T0은 양끝 다듬기와 Commons 첫 글자만 다룬다. 폭 없는 문자(U+200B 등 `\p{Cf}`)는 M3a3a 리스크 7 (b)대로 여전히 글자로 본다. Commons `Artist` 에서 이런 값이 보이면 T1 보고에 적는다.
12. **로그로 날짜를 짐작할 수 있음** — 이 기능은 이름표만 넘기지만, M3a1 위키백과 호출의 WARN 줄은 지금도 문서 제목(`9월 17일`, `1988년`)을 찍는다. 장소가 요청에 들어오는 M2에서 다시 본다(로드맵 후속 후보).

## PM 결정

| # | 결정 | 상태 |
|---|---|---|
| M3a3 분할 A | (a) 두 PR — 이 기능이 2부 | 결정 2026-10-13 |
| B | (a) 스키마 검증 라이브러리는 테스트 범위 유지 — 이 기능은 테스트에서만 `DataSchemas` 를 쓴다 | 결정 2026-10-13 |
| C | (c) `chore/env-guard` 하지 않음 — T4 (1)에서 PM이 `.env` 를 직접 확인한다 | 결정 2026-10-13 |
| M3a3 확인점 | (a) M3a3 T3을 M3a3a로 — 이 기능의 선행이 M3a3a로 바뀜 | 결정 2026-10-13 |
| **M3a3b 결정 1** | **(a)** 두 PR — 이 PR(그날 사진·매퍼·이월 권장, 약 485) + M3a3c(사건 사진, 약 230). M3 PR 20개 | **결정 2026-10-14** |
| **M3a3b 결정 2** | **(b)** ★ T2는 backend-dev가 구현 | **결정 2026-10-14** |

새로 묻는 것 없음(확인점 2에서만 다시 묻는다).
