# 사건 사진: PageImages와 Commons 파일 (M3a3c)

> 브랜치: `feat/event-photos` (`main` ce3760c에서 갈라짐) · 작성: planner · 상태: **PM 승인 (2026-10-15, 개정)** — 결정 1 **(a)** 미디어 호출 재시도 없음, 2 **(b)** 위키백과 문서 하나가 `UNAVAILABLE` 이면 그날 사진을 찾지 않음, 3 **(a)** 한 PR + 확인점 두 개, 4 **(b)** T0은 backend-dev. 처음 승인 2026-10-14(M3a3b에서 분할). 이번 개정은 머지된 M3a3b 코드(PR #17)에 맞추고, PR #17 리뷰 권장 5개를 넣고, 크기를 M3a3b 실측으로 다시 셌다.
> 기준: PM 미디어 결정 2026-10-11, 안전장치 1~6, `CLAUDE.md` 7절 1·2·3·5·6항, `docs/product-brief.md` 6.1절·6.2절·6.3절·8절·12절, `docs/research/day-events.md` 3·4절, `docs/plans/m3a3b-commons-photos.md`(Commons 파일 매퍼·사진 규칙·시간 상한), `docs/plans/m3a3-event-media.md`(형식·R1~R9·`date_relation`), M3-P7, M3-P16 변경(2026-10-12), #22 (c), #15 초안(±5년)
> 선행: **M3a3b 머지됨 (PR #17, `main` ce3760c)**. 웹 M3b3도 머지됨(PR #16) — 웹은 이미 사건 `media` 를 그린다(M3b2).
> 뒤따르는 기능: **M3b2 T6**(PM 실제 사건 사진 확인, 이 PR 머지 뒤 `main` 에서). M3a4는 같은 연결 자리를 고친다(리스크 5).

## PM 결정 (2026-10-15)

| # | 질문 | 결정 | 반영 |
|---|---|---|---|
| 1 | 이월 1: 미디어 경로의 재시도 | **(a)** 미디어 호출(날짜 범주·PageImages·파일 제목 묶음)은 재시도 없이 1회, 읽기 타임아웃은 고정 `media.lookup-deadline`. planner 추천과 같음. 고르지 않은 것: (b) 남은 시간을 읽기 타임아웃으로, (c) 그대로 | "실패와 로그", T0 (3), T1 (3), 리스크 12 |
| 2 | 이월 4: 위키백과 문서 하나가 `UNAVAILABLE` 인 날의 그날 사진 | **(b)** 찾지 않는다 — `tragic` 날처럼 취급(약 5줄). 사건 사진은 그대로 찾는다. planner 추천과 같음. 고르지 않은 것: (a) 리스크로만 | "고르는 법", 서비스 계약, T2 (14), 리스크 9 |
| 3 | 크기 약 445(370~530) | **(a)** 한 PR + 확인점 두 개: T0·T1 뒤 누적 S1 320 초과면 알림, T2 뒤 500 초과면 T3 전에 멈추고 물음. planner 추천과 같음. 고르지 않은 것: (b) T0을 `fix/media-deadline` PR로, (c) 공용 도우미 정리를 M6b로 | "크기", 작업 목록 순서, T1 (8), T2 (16) |
| 4 | ★ T0(공용 시간 상한 도우미)을 PM이 직접 구현할까 | **(b)** backend-dev가 구현. planner 추천과 같음 | T0 담당 |

## 이번 개정에서 바뀐 것 (2026-10-15)

| 항목 | 내용 |
|---|---|
| 머지된 코드에 맞춤 | "이어받는 계약"을 실제 이름으로 다시 썼다: `CommonsClient.dayCategory(LocalDate, Duration)` → `Pages(List<JsonNode>, boolean failed)`, `ActionApiCommonsClient.pages(label, UriComponentsBuilder, Duration)`(패키지 전용), `CommonsFileMapper.map(JsonNode, Predicate<String>, String, LocalDate)` → `Result(MediaItem item, Drop drop)`, `MediaLookup.dayMedia(LocalDate, boolean)` → `DayMedia`, `CommonsMediaLookup`(시간 상한 코드를 직접 가짐), `MediaLookupProperties(dayMediaLimit, lookupDeadline)`, `WorldStoryService(source, judge, media)` |
| 질의 언어 | Commons 질의에 `iiextmetadatalanguage` 를 **보내지 않는다**(기본 언어, PM 결정 2026-10-14 — `ko` 면 `DateTimeOriginal` 이 "촬영 날짜: …"로 바뀌어 읽히지 않음). 이전 판의 `iiextmetadatalanguage=ko` 를 지웠다. M3a3b 계획서의 같은 문구도 고쳤다(이월 3) |
| 이월 리뷰 권장 | PR #17 리뷰 권장 4개 + 시간 메모 1개 → 새 **T0**와 리스크(아래 표) |
| 기존 시험을 깨지 않는 계약 | `MediaLookup` 은 지금 `dayMedia` 하나뿐인 함수형 인터페이스이고, 테스트 6개 파일이 `(date, tragicDay) -> DayMedia.NONE` 람다로 만든다. 그래서 추상 메서드를 바꾸지 않고 **기본 메서드**를 더한다("API / 데이터 변경") |
| 위키백과 스텁 충돌 | 실제로 확인했다: `WorldStoryDemoTest` 등은 `/w/api.php` + `action=query` 만으로 pageprops를 스텁하고, `action=query` 요청 수를 센다. PageImages도 `action=query` 라 그 스텁이 답하고 수가 늘어난다 → T1 (5) 기본 스텁(우선순위 1), T2 (12) 요청 수 대상 한정 |
| 크기 | M3a3b 실측(585, 다시 셈 485의 ×1.21)으로 다시 셌다: 약 230 → **약 445 (370~530)**. 결정 3 (a)로 한 PR + 확인점 |
| 작업 | T0 추가(이월 1·2). ★는 T0(공용 시간 상한 도우미) — 결정 4 (b)로 backend-dev가 구현 |

### 이월된 리뷰 권장 (PR #17)

| # | 내용 | 처리 |
|---|---|---|
| 1 | `CommonsMediaLookup` 이 `media.lookup-deadline` 전체(3s)를 호출별 읽기 타임아웃으로 넘긴다. 첫 시도가 타임아웃이면 이미 상한이라 `WikimediaHttpClient` 의 재시도(타임아웃·502/503/504에 1회)는 끝날 수 없고, 취소 직전에 쓸모없는 두 번째 요청을 보낼 수 있다. 시간 상한 코드가 `SparqlWikidataSignalSource.within` 과 겹친다 | **T0** (1)~(3). 결정 1 (a): 미디어 경로는 재시도 없이 1회 |
| 2 | `ActionApiCommonsClient`: `error` 가 있는데 `code` 가 없는 응답이 성공(빈 목록, `partial: false`)으로 처리된다 | **T0** (4). 새 PageImages 어댑터도 처음부터 같은 규칙(T1 (1)) |
| 3 | `docs/plans/m3a3b-commons-photos.md` 61·128·248행에 `iiextmetadatalanguage=ko`·"ko로 받은 값"이 남아 있다 | **이 개정에서 planner가 고쳤다**(그 계획서 머리말에 날짜 메모). 작업 아님 |
| 4 | 위키백과 두 문서 중 하나가 실패한 날(`UNAVAILABLE`)에는 빠진 문서의 비극 사건을 볼 수 없어 그날 사진을 막지 못한다 | 결정 2 (b): 그날 사진을 찾지 않는다(T2 (14)). 사건 사진은 같은 구멍이 없다(리스크 9) |
| 5 | 세계 섹션 최악 시간: 스텁 약 11.7초, 실제 설정 추정 13~15초 — 제품 정의서 8절 10초 목표 초과(M6b) | 리스크 6, "세계 섹션 시간 예산". 사건 사진은 **같은 미디어 상한 안에서 그날 사진과 동시에** 돌아 최악 시간을 늘리지 않는다 — T2 (9), T3 (6) |

## 목표

1. `GET /api/story/world` 의 `events[].media` 를 Commons **사진**으로 채운다(사건마다 최대 1장). 라이선스가 확인되지 않아도 출처·라이선스 상태·촬영 시점과 함께 직접 보인다. 촬영 시점을 모르는 사진은 "촬영 시점 미상"으로 붙는다.
2. `tragic` 사건에는 사진을 붙이지 않고 찾지도 않는다. 큐레이션 `hide` 로 내린 사건도 찾지 않는다.
3. 사건과 촬영 연도가 ±5년(`media.max-years-apart`)을 넘는 사진은 붙이지 않는다.
4. 사건 사진과 같은 파일은 `day_media` 에서 뺀다. 같은 파일은 앞 사건에만 붙는다.
5. 사건 사진 조회가 실패하거나 늦어도 사건과 `day_media` 는 그대로 200으로 나가고 `partial: true` 만 켜진다. 세계 섹션 최악 시간은 M3a3b보다 늘지 않는다.
6. PR #17 리뷰 권장 1·2를 닫는다(시간 상한 공용화와 미디어 경로 재시도, `error` 판정). 위키백과 문서 하나가 실패한 날에는 그날 사진을 찾지 않는다(결정 2 (b)).

## 범위 밖

- 그날 사진 규칙, Commons 파일 매퍼 규칙 → M3a3b (이 기능은 쓰기만 한다. 예외: T0 고침, 결정 2 (b)의 그날 사진 차단 조건)
- 영상 → M3a4~M3a5b
- 사진 메타데이터 캐시 → 후속(리스크 4)
- Wikidata 대표 이미지(P18) — 쓰지 않는다
- 미디어 파일 복사·프록시 (안전장치 1)
- 세계 섹션 전체 시간 상한(10초 맞추기) → M6b (로드맵 후속 후보). 이 기능은 늘리지만 않는다
- 위키백과 날짜·연도 문서, pageprops, Wikidata 호출의 재시도 규칙 → 바꾸지 않는다(T0은 미디어 경로만)
- `ActionApiKoWikiClient` 의 `error` 판정 → 바꾸지 않는다(링크 해석은 `error` 가 있고 `query` 가 없으면 이미 실패로 본다)
- 프론트엔드 → 변경 없음(M3b2가 이미 사건 `media` 를 그린다)

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| 한국어 위키백과 PageImages | **확인됨** — [day-events.md 3절 Commons 카드·4절](../research/day-events.md) [48][64][65]. 실측은 영어 위키백과(같은 MediaWiki 확장, 기본값 `pilicense=free`). 한국어 위키백과 응답은 T1 녹화로 확인한다(리스크 10) | 사건 대표 링크 문서의 대표 이미지 파일 이름(`prop=pageimages&piprop=name&pilicense=free`, 제목 묶음 1회) | 비자유 이미지를 1차로 거른다(M3-P7). `wikimedia.kowiki-api-url` 재사용 |
| Wikimedia Commons (Action API) | **확인됨** — [day-events.md 3절 Commons 카드](../research/day-events.md#3-출처-카드) | 파일 제목 묶음(`titles=File:A\|File:B&prop=imageinfo&iiprop=url\|mime\|size\|extmetadata&iiurlwidth=640`, **언어 지정 없음**) | M3a3b와 같음: `WikimediaHttpClient` 로만, User-Agent·429·동시 3, 작가·라이선스·원문 페이지 표시, 썸네일은 `thumburl` 만, 핫링크만 |

키 없음. 새 HTTP 클라이언트를 만들지 않는다. User-Agent는 연락처로 **저장소 주소만** 쓴다 — 녹화 `curl` 은 정확히 `The-World-That-Day (https://github.com/ericmschoi/The-World-That-Day)`, 테스트 상수는 `WikimediaWireMockTest.TEST_USER_AGENT`. 이메일을 넣지 않는다.

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| 미디어 표시·촬영 시점 미상·안전장치 1~6 | M3a3 계획서와 같음 | 결정 2026-10-11 |
| #13-C | (a) Commons 주소 그대로(핫링크, `no-referrer`) | 결정 |
| #22 | (c) + `tragic` 사건엔 미디어 없음, `tragic` 이 있는 날은 `day_media` 없음 | 결정 |
| M3-P7 | 비자유 위키 로컬 파일 제외 | 결정 |
| M3-P16 | **변경 2026-10-12**: 원문을 거르지 않음 → 응답에 남은 사건은 모두 사진 후보. `hide` 는 바로 내리는 수단 | 결정 |
| 근접 연도 | 사건 사진은 ±5년 안만 (`media.max-years-apart: 5`) | #15 확정 전 초안 |
| M3a3b 결정 1·2 | 1 (a) 두 PR — 이 기능이 사건 사진, 2 (b) M3a3b T2는 backend-dev | 결정 2026-10-14 |
| Commons 질의 언어 | `iiextmetadatalanguage` 를 보내지 않음(기본 언어) | 결정 2026-10-14 (M3a3b T1 막힘 (a)) |
| M3a3c 결정 1~4 | 1 (a), 2 (b), 3 (a), 4 (b) — 위 "PM 결정 (2026-10-15)" | **결정 2026-10-15** |

## 이어받는 계약 (머지된 코드, `main` ce3760c)

### 미디어 조회 (M3a3b)

| 자리 | 지금 코드 | 이 기능에서 |
|---|---|---|
| Commons 클라이언트 | `media.commons.CommonsClient`: `Pages dayCategory(LocalDate, Duration readTimeout)`, `record Pages(List<JsonNode> pages, boolean failed)`. 구현 `ActionApiCommonsClient`: 패키지 전용 `pages(String label, UriComponentsBuilder query, Duration readTimeout)` 가 `prop=imageinfo&iiprop=url\|mime\|size\|extmetadata&iiurlwidth=640&format=json&formatversion=2` 를 붙이고 `.build().encode()` 로 주소를 만든 뒤 `http.get(SOURCE, label, uri, readTimeout)`. `error.code` 가 있으면 실패(`ratelimited` 면 `blockHost`), `query.pages` 가 배열이거나 없으면 성공. 상수 `SOURCE = "commons"`, `DAY_CATEGORY = "commons day category"` | 파일 제목 묶음 메서드를 더한다(T1). `pages()` 는 주소 템플릿 변수를 받게 바꾼다(제목의 `&`·`+` 를 엄격히 인코딩 — 리스크 13). T0에서 `error` 판정과 재시도 끔 |
| Commons 파일 매퍼 | `CommonsFileMapper.map(JsonNode page, Predicate<String> mime, String placeRef, LocalDate date)` → `Result(MediaItem item, Drop drop)`, `Drop` = `MISSING`·`NON_FREE`·`MIME`·`FILTERED`·`RULE`. `id` = `commons-<pageid>`. 로그를 쓰지 않음 | 그대로 쓴다. MIME `image/jpeg`, `placeRef` = 사건 `scope`: `domestic` → `country:KR`, `world` → `world`, `unknown` → `unknown`(스키마 `place_ref` 허용 값) |
| 미디어 조회 | `media.MediaLookup`(함수형 인터페이스): `DayMedia dayMedia(LocalDate date, boolean tragicDay)`, `record DayMedia(List<MediaItem> items, boolean failed)` + `NONE`·`FAILED`. 구현 `media.commons.CommonsMediaLookup(CommonsClient, CommonsFileMapper, MediaLookupProperties)`: 가상 스레드 + `FutureTask` 하나, `lookupDeadline` 만큼 기다리고 넘으면 `cancel(true)` + `CANCEL_GRACE`(1s), 실패 WARN이 없었을 때만 `cause=deadline` WARN. `collect` 안에서 제목 순 → 순위(`same_day`·그 밖·`unknown`) → `dayMediaLimit` 개로 자름 | 추상 메서드는 그대로 두고 기본 메서드를 더한다(아래). `CommonsMediaLookup` 이 그 메서드를 재정의해 그날 사진 경로와 사건 사진 경로를 **같은 끝 시각** 안에서 동시에 돌린다. 자르기(6개)는 사건 사진 `id` 를 뺀 **뒤**로 옮긴다 |
| 설정 | `MediaLookupProperties(@DefaultValue("6") int dayMediaLimit, @DefaultValue("3s") Duration lookupDeadline)`, 잘못된 값은 시작 실패. 테스트가 2인자 생성자를 부른다(`CommonsMediaLookupTest`) | `maxYearsApart`(기본 5, 음수면 시작 실패)를 더하고 **2인자 생성자를 남긴다**(기존 테스트 그대로) |
| 세계 서비스 | `WorldStoryService.find`: 판정 → `hidden` 빼고 `normal`·`tragic` 목록 → `media.dayMedia(date, !tragic.isEmpty())` → `normal.addAll(tragic)` → `partial = day.degraded() \|\| judgement.partial() \|\| dayMedia.failed()` | 사건 사진 대상 목록을 만들어 새 기본 메서드를 부른다. 그날 사진 차단 조건에 "위키백과 문서 하나라도 `UNAVAILABLE`"을 더한다(결정 2 (b)). `Event.of` 에 미디어를 넘긴다 |
| 응답 | `WorldStoryResponse.Event.of(KoWikiEvent, Verdict)` 가 `media` 에 `List.of()`. 테스트는 `Event.of` 를 부르지 않는다 | `Event.of(KoWikiEvent, Verdict, List<MediaItem>)` |
| 테스트 기반 | `WikimediaWireMockTest`: `KOWIKI_PATH = /w/api.php`, `SPARQL_PATH`, `COMMONS_PATH = /commons/w/api.php`, 매 테스트 전 리셋 + Commons 빈 범주 기본 스텁, `read-timeout` 2s, `TEST_USER_AGENT` | PageImages 기본 스텁(우선순위 1)을 더한다(T1 (5)) |
| 녹화 fixture | `fixtures/wikimedia/commons/day-category-{1988-09-17,2002-06-22,1995-03-15}.json` + `README.md`(언어 지정 없음, 2026-10-14 녹화, 모든 `url` 은 `upload.wikimedia.org`, `thumburl` 은 `thumb.wikimedia.org` 960px, `utm_*` 쿼리 포함). 위키백과 `fixtures/wikimedia/kowiki/pageprops-*.json` | PageImages·파일 제목 묶음 녹화를 더한다(T1 (6)) |

### Wikimedia 호출과 시간 상한 (M3a1~M3a2)

| 자리 | 지금 코드 | 이 기능에서 |
|---|---|---|
| HTTP | `WikimediaHttpClient.get(source, title, uri[, readTimeout])`: 시도마다 읽기 타임아웃, `wikimedia.max-retries`(1)만큼 타임아웃·502/503/504를 다시 시도. 읽기 타임아웃 값마다 `RestClient` 하나를 만들어 `restClients` 맵에 **영구 보관**한다(`computeIfAbsent(readTimeout, …)`). 빈자리 대기는 `wikimedia.read-timeout`(4s) | T0: 재시도 없는 호출(오버로드)을 더한다(결정 1 (a)). 읽기 타임아웃은 고정값이라 `RestClient` 는 하나만 생긴다(요청마다 남은 시간을 넘기면 맵이 끝없이 커진다) |
| 시간 상한 | `SparqlWikidataSignalSource.within(label, call)`·`ended(thread)`(private, 약 45줄), `CommonsMediaLookup.dayMedia` 안 같은 모양 코드(약 40줄) | T0: `wikimedia` 패키지 공용 도우미 하나로 합친다. 두 곳 모두 그것을 쓰고, **기존 Wikidata·Commons 테스트를 고치지 않고 통과** |
| 링크 해석 모양 | `ActionApiKoWikiLinkResolver`: `UriComponentsBuilder` + `titles={titles}` 템플릿 + `.encode().buildAndExpand(String.join("\|", batch))`, 묶음 50, `formatversion=2`, `normalized`·`redirects` 를 따라감, `ratelimited` 면 `blockHost`, `error` 또는 `query` 없음 → 실패, 이름표 `pageprops of N titles` | PageImages 어댑터가 같은 모양을 따른다 |

### M3a2·M3a3

- `EventToneJudge.Verdict(Tone tone, Basis basis, Scope scope, boolean hidden)`. 사진 대상: `hidden == false` 이고 `tone == NORMAL` 이고 `title`(= `KoWikiEvent.linkTitles().getFirst()`)이 있음.
- `KoWikiDayResult.degraded()` = 링크 해석 실패 또는 문서 하나라도 `UNAVAILABLE`·`STALE`. `allUnavailable()` 이면 503. `docs` 는 날짜 문서·연도 문서 순서의 `KoWikiDocStatus`(`state()`).
- `DateRelation(Kind kind, int years)` 는 **요청 날짜** 기준이다(사건 날짜 = 요청 날짜). ±5년은 `years` 의 절댓값으로 본다. `UNKNOWN` 은 연도를 모르므로 붙인다. `SAME_DAY`·`SAME_MONTH`·`SAME_YEAR` 는 `years` 0.

## 고르는 법

| 자리 | 후보 | 개수·규칙 |
|---|---|---|
| `events[].media` | 응답에 남은 사건 중 `tone == NORMAL` 이고 `title`(대표 링크)이 있는 사건 → 한국어 위키백과 PageImages(`pilicense=free`) 파일 이름 → Commons 파일 제목 묶음 → `CommonsFileMapper.map(page, "image/jpeg"::equals, scope 위치, date)` | 사건마다 사진 최대 1(M3a4부터 영상이 더해진다). `tragic` 사건(`default` 포함) 0, PageImages 요청에도 넣지 않음. `hidden` 사건은 요청에 넣지 않음. 대상이 50개를 넘으면 앞 50개만(요청 1회). `years` 의 절댓값 > `media.max-years-apart`(5)면 붙이지 않음(`unknown` 은 붙이고 "촬영 시점 미상"). 같은 파일(`id`)이 두 사건에 나오면 응답 순서에서 앞 사건에만. Commons에 없는 파일(위키 로컬 파일, `missing`)은 매퍼가 `MISSING` 으로 버린다(M3-P7) |
| `day_media` 와의 관계 | — | 그날 사진 후보(정렬 뒤)에서 사건 사진과 같은 `id` 를 뺀 **뒤** `media.day-media-limit`(6)개를 자른다. 사건 사진 경로가 실패하면 빼지 않는다 |
| 그날 사진 차단 (결정 2 (b)) | — | 응답에 `tragic` 사건이 있는 날(M3a3b)에 더해, 위키백과 날짜·연도 문서 중 하나라도 `UNAVAILABLE` 인 날도 `day_media` = `[]` 이고 날짜 범주 요청 0. `STALE`(옛 사본)은 해당 없음. 사건 사진은 그대로 찾는다 |

요청 수(API 호출 1회): 위키백과 PageImages 최대 1(대상 사건이 없으면 0), Commons 최대 2(날짜 범주 0~1 — M3a3b, 파일 제목 묶음 0~1 — PageImages가 파일을 하나도 주지 않으면 0). 날짜 범주 요청과 PageImages → 파일 제목 묶음은 **동시에** 간다(공용 `Semaphore(3)` 중 최대 2자리).

### 실패와 로그

| 규칙 | 내용 |
|---|---|
| 일부 실패 | PageImages 실패 또는 파일 제목 묶음 실패(요청 실패·`error`(코드 유무와 상관없이)·읽을 수 없는 본문·`ratelimited`) → `events[].media` 만 모두 `[]`, `day_media` 는 그대로, `partial: true`, 200. `tragic` 으로 비운 미디어, 규칙으로 버린 미디어, 대표 이미지가 없는 문서는 `partial` 이 아니다 |
| 시간 상한 | 그날 사진 경로와 사건 사진 경로가 **끝 시각 하나**(`media.lookup-deadline`, 3s)를 나눠 쓴다. 끝 시각에 안 끝난 경로만 취소(`cancel(true)`, 1초 유예)하고 비운다. 끝난 경로의 결과는 남는다 |
| 재시도 (결정 1 (a)) | 미디어 경로의 모든 호출(날짜 범주, PageImages, 파일 제목 묶음)은 재시도 없이 1회, 읽기 타임아웃은 고정값 `media.lookup-deadline` |
| 503 | 바뀌지 않음 |
| 로그 | `title` 이름표는 `commons day category`(그대로), `kowiki pageimages of <개수>`, `commons files of <개수>` 만. 날짜·파일 이름·문서 제목·API `error.info` 를 넣지 않는다. 실패 하나 = WARN 한 줄(경로마다). 버린 이유는 DEBUG에 이유별 개수만(연도 초과는 새 이유 이름, 예: `YEARS`). 사건은 ID 해시 8자리만 |

### 세계 섹션 시간 예산 (이월 5)

| 구간 | M3a3b (지금) | 이 기능 뒤 |
|---|---|---|
| 위키백과 (`kowiki-day-deadline`) | 5초 | 5초 (그대로) |
| Wikidata (`wikidata-read-timeout` + 유예) | 5초 (+1초) | 그대로 |
| 미디어 (`media.lookup-deadline` + 유예) | 3초 (+1초) — 날짜 범주 1개 | **3초 (+1초)** — 날짜 범주와 사건 사진 경로가 같은 끝 시각을 나눠 씀 |
| 최악 합계 | 약 13초 (최대 15초), 스텁 측정 약 11.7초 | **같음**: 약 13초 (최대 15초). 스텁 측정은 M3a3b 값 + 0.5초 이하(T3 (6)) |

- 늘지 않게 하는 장치: 사건 사진 경로를 미디어 상한 **뒤에 이어 붙이지 않고** 같은 끝 시각 안에서 동시에 돌린다(T2 (9)). 재시도를 꺼서(결정 1 (a)) 상한 직전의 쓸모없는 두 번째 요청도 없어진다.
- 대가: 사건 사진 경로는 요청 2개가 줄지어 3초 안에 끝나야 한다. 늦으면 사건 사진만 비고 `partial: true`.
- 10초 목표와의 차이는 그대로 남는다 → M6b(로드맵 후속 후보 "섹션 전체 시간 상한").

## API / 데이터 변경

- `GET /api/story/world`: `events[].media` 에 M3a3 형식의 사진 레코드(0~1장)가 들어간다. `day_media` 에서 사건 사진과 같은 파일이 빠진다. 위키백과 문서 하나가 `UNAVAILABLE` 인 날은 `day_media` 가 `[]` 다(결정 2 (b), 그런 날은 이미 `partial: true`). `partial` 에 사건 사진 실패가 더해진다. 새 에러 코드 없음. 응답 키·형은 그대로다(웹 계약 변화 없음, `MediaItem` 키 27개).
- 설정 추가: `media.max-years-apart: 5`(0 이상, 음수면 시작 실패, `MediaLookupProperties`). `media.lookup-deadline` 주석을 "호출별 읽기 타임아웃, 재시도 없음"으로 고친다.
- DB 변경 없음. 데이터 파일 없음(fixture는 테스트 리소스).

`events[]` 항목 하나(값은 T1 녹화로 정해진다. 바뀌는 것은 `media` 뿐):

```
{"id": "…", "title": "1988년 하계 올림픽", "scope": "domestic", "tone": "normal", "tone_basis": "curated", …,
 "media": [{"id": "commons-<pageid>", "type": "photo", "display_mode": "image",
            "url": "https://upload.wikimedia.org/…", "thumbnail_url": "https://thumb.wikimedia.org/…/960px-….jpg",
            "source_name": "Wikimedia Commons", "license": "…", "license_status": "free|other|unknown",
            "captured_at": "1988-09-17" 또는 null, "date_precision": "day" 또는 null, "place_ref": "country:KR",
            …(키 27개, 값이 없는 키도 null로 남음),
            "date_relation": {"kind": "same_day", "years": 0}}]}
```

서버 안 계약 (이름은 예시, 머지 코드 이름은 보고서에 적는다):

| 부품 | 모양 |
|---|---|
| 공용 시간 상한 (T0, `wikimedia` 패키지, 공개) | 끝 시각을 한 번 정하고(`budget` 으로부터), 그 끝 시각을 여러 작업이 나눠 쓴다. 작업마다 가상 스레드. 기다리기는 **남은 시간까지만**, 넘으면 `cancel(true)` 뒤 최대 1초 유예. 결과는 "끝남(값)" / "시간 초과(유예 안에 멈췄는지 포함)" / "예외" 중 하나라서 부르는 쪽이 "실패 하나 = WARN 한 줄"을 지킬 수 있다 |
| 재시도 없는 HTTP 호출 (T0, 결정 1 (a)) | `WikimediaHttpClient` 오버로드(예: `get(source, title, uri, readTimeout, retry=false)`): 시도 1회. 429·Retry-After 있는 503의 차단 처리는 그대로. 기존 오버로드는 그대로 `max-retries` |
| PageImages 어댑터 (T1, 새 인터페이스 + Action API 구현, 예: `media.pageimages.KoWikiPageImages`) | `Optional<Map<String, String>> imageNames(List<String> titles, Duration readTimeout)`: 요청 제목 → 파일 이름(`File:` 없이), 대표 이미지가 없는 제목은 맵에 없음, 실패면 빈 `Optional`. 예외 없음 |
| Commons 파일 제목 묶음 (T1, `CommonsClient` 에 더함) | 예: `Files files(List<String> fileNames, Duration readTimeout)` → `record Files(Map<String, JsonNode> byName, boolean failed)`: 요청한 파일 이름 → 응답 페이지(`normalized` 를 따라감, `missing` 페이지도 넣어 매퍼가 `MISSING` 으로 버리게 함). `Pages` 레코드는 바꾸지 않는다 |
| 미디어 조회 (T2, `MediaLookup` 에 기본 메서드) | `record EventTarget(String eventId, String title, String placeRef)`, `record WorldMedia(Map<String, MediaItem> eventPhotos, List<MediaItem> dayMedia, boolean failed)`(`eventPhotos` 키는 사건 ID). `default WorldMedia lookup(LocalDate date, List<EventTarget> targets, boolean tragicDay)` 의 기본 구현은 `dayMedia(date, tragicDay)` 만 부르고 사건 사진은 비운다 → 기존 람다·`Recording` 가짜가 고치지 않고 동작한다. `CommonsMediaLookup` 이 재정의한다. `dayMedia` 는 `lookup(date, List.of(), tragicDay)` 와 같은 결과 |
| 세계 서비스 (T2) | 대상 = `normal` 사건 중 `title` 이 있는 것(응답 순서), `placeRef` 는 `scope` 로. `tragicDay` = 응답에 `tragic` 사건이 있음 **또는** 위키백과 문서 하나라도 `UNAVAILABLE`(결정 2 (b)). `media.lookup(date, targets, tragicDay)` → `Event.of(event, verdict, 사건 사진 0~1장)`, `partial` 에 `\|\| failed` |

## 테스트 방침

- M3a3b "테스트 방침"을 따른다. **외부 네트워크를 부르지 않는다** — WireMock + 녹화 fixture. 컨텍스트 테스트의 모든 `wikimedia.*-url` 은 WireMock을 가리킨다.
- 녹화 fixture: `fixtures/wikimedia/kowiki/pageimages-1988-09-17.json`(제목 3개)·`pageimages-2002-06-22.json`(제목 1개), `fixtures/wikimedia/commons/files-<날짜>.json`(돌려받은 파일의 `imageinfo`, 받은 파일이 없으면 만들지 않음). 각 `README.md` 에 원 URL·녹화 일자·User-Agent(저장소 주소)·라이선스 행. 바이트를 고치지 않는다. 이미지 파일은 받지 않는다.
- 합성 fixture로 규칙 표의 행마다 시험. 실제 비극 사건 이름을 쓰지 않는다. 표본 날짜는 데모 3날짜와 공개 역사 사건 날짜·경계값(예: 2000-02-29). 실제 사람의 생년월일을 쓰지 않는다. 합성 파일 이름·문서 제목은 지어낸 값.
- 응답 미디어 전부를 `date_relation` 을 뺀 뒤 M3a3 스키마로 검증(`DataSchemas`).
- 시간 시험은 짧은 상한(예: 300ms~1s)과 응답하지 않는 스텁으로, 여유 1초를 둔다(기존 방식).

## 데모 입력 3개로 확인할 것

사건·`tone` 은 M3a2 결과, `day_media` 는 M3a3b 결과(사건 사진과 겹치는 파일만 빠짐)다. 사건 사진 값은 **T1 녹화** 결과로 정하고 T2가 그 값으로 고정 단언한다(리서치 기준 영어 위키백과 올림픽·월드컵 문서는 자유 대표 이미지가 없었고, 개막식 문서는 `Seoul_Olympic_torch.jpg` — 1988-09-17 날짜 범주에도 있는 파일이다. 한국어 문서가 같은 파일을 주면 `day_media` 에서 빠지는 것이 정상).

| 데모 입력 | 사건 (M3a2) | 확인 (T2) |
|---|---|---|
| 1988-09-17 + "서울" | 3개, 모두 `normal`/`curated`/`domestic`: ① `1988년 하계 올림픽`(`Q8470`) ② `KBS 1TV`(`Q777278`) ③ `KBS 2TV`(`Q624509`) | PageImages 요청 **1회**에 세 제목이 모두 들어간다. 각 사건 `media` 0~1장, 모두 `display_mode: image`, `place_ref: country:KR`(GPS 없을 때), 스키마 통과, `years` 절댓값 5 이하 또는 `unknown`. 로고(SVG·PNG)·비자유 로컬 파일은 붙지 않는다(KBS 두 사건은 대표 이미지가 로고뿐이면 `[]` — 정상). 사건 사진과 같은 `id` 는 `day_media` 에 없고 `day_media` 는 나머지 그날 사진(최대 6). Commons 요청 ≤ 2. `partial: false` |
| 2002-06-22 + "광주 서구 풍암동" | 1개: `2002년 FIFA 월드컵`(`Q47735`), `normal`/`curated` | PageImages 요청 1회(제목 1개). 사건 `media` 0~1장(T1 녹화로 확정). `day_media` 는 M3a3b 결과(1장) 그대로(같은 파일이 아니면). `partial: false` |
| 1995-03-15 + "서울대학교병원" | 0개, `notice.code` = `NO_EVENTS_FOUND` | PageImages 요청 0, Commons 요청 1(날짜 범주만). `partial: false` |
| (합성) `tragic` 섞인 날 | `tragic`(`default` 포함) 사건 하나 + `normal` 사건 하나 | `tragic` 사건 `media` = `[]`, PageImages 요청에 그 제목 없음, `normal` 사건은 사진 후보(`day_media` 는 M3a3b 규칙대로 `[]`, 날짜 범주 요청 0) |

## 크기 (400줄 규칙)

제품 코드·설정(S1, `added`) 기준.

### 실측 비율 (M3a3b, `docs/progress.md`)

| 작업 | 다시 셈 (2026-10-14) | 실제 S1 | 비율 |
|---|---|---|---|
| T0 (이월 권장 1~3 — 작은 고침) | 약 25 | **52** | ×2.08 |
| T1 (클라이언트·매퍼·설정) | 약 280 | **333** (누적 385) | ×1.19 |
| T2 (조회·시간 상한·연결) | 약 180 | **200** (누적 585) | ×1.11 |
| **합계** | 약 485 (420~585) | **585** (T3은 테스트만) | **×1.21** |

교훈: 새 클래스·연결은 파일별 셈의 ×1.1~1.2, 여러 곳의 작은 고침은 ×2 안팎(주석·상수·검증이 붙는다). 이전 판의 이 기능 셈(T1+T2 약 230)에 ×1.21을 곱하면 약 280 — 아래 파일별 셈(T1+T2 약 325)은 이전 판에 없던 계약(기본 메서드, `Files` 레코드, 템플릿 인코딩, 두 경로 동시 실행, 결정 2 (b))을 더한 값이다.

### 다시 셈

| 작업 | 내용 (머지된 파일 크기 기준 셈) | 셈 | 보정 | 다시 셈 | 범위 |
|---|---|---|---|---|---|
| T0 | 공용 시간 상한 도우미(약 65), Wikidata·Commons에서 바꿔 쓰기(약 15) / 재시도 없는 호출(약 10), `error` 판정(약 4), 설정·Javadoc 주석(약 4) | 80 / 18 | ×1.15 / ×2.1 | 약 130 → **약 120** | 100~150 |
| T1 | PageImages 인터페이스(약 20)·Action API 구현(약 75 — 링크 해석 120줄에서 여러 묶음 빼고), 파일 제목 묶음 메서드·`Files`·인터페이스(약 35), `pages()` 템플릿 변수(약 5) | 135 | ×1.19 | **약 160** | 135~190 |
| T2 | `MediaLookup` 기본 메서드·레코드 2개(약 30), `CommonsMediaLookup` 두 경로·사건 사진 고르기·자르기 위치(약 75), `maxYearsApart`·2인자 생성자(약 12), 서비스·`Event.of`(약 25), 결정 2 (b)(약 5), 설정 약 5 | 150 | ×1.11 | **약 165** | 140~200 |
| **합계** | | 약 365 | | **약 445** | **370~530** |

- 400을 넘는다(M3a3a 456, M3a3 505, M3a3b 585와 비슷한 크기). **결정 3 (a)**: 한 PR, 확인점 두 개.
- **확인점 1 (알림): T0·T1 뒤 누적 S1이 320을 넘으면** 오케스트레이터가 PM에게 **알리고 계속한다**(예상 약 280. PM이 멈추라고 하면 멈춘다).
- **확인점 2 (멈춤): T2 뒤 누적 S1이 500을 넘으면** 오케스트레이터가 **T3 전에 멈추고** PM에게 어떻게 할지 묻는다(예상 약 445).
- 테스트(S2)·fixture는 따로 보고한다.

## 작업 목록

순서: T0 → T1 → **확인점 1 (320, 알림)** → T2 → **확인점 2 (500, 멈춤)** → T3 → T4(PR 검수 때) → 머지 → M3b2 T6(`main` 에서).

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | ★ PR #17 이월 1·2: 공용 시간 상한 도우미, 미디어 경로 재시도 끔, Commons `error` 판정 | backend-dev (결정 4 (b)) | - | (1) `wikimedia` 패키지에 공개 도우미 하나(이름 자유): 끝 시각을 한 번 정하고 여러 작업이 나눠 쓰며, 작업마다 가상 스레드, 남은 시간까지만 기다리고 넘으면 `cancel(true)` 뒤 최대 1초 유예, 결과가 끝남/시간 초과(유예 안에 멈췄는지)/예외로 구분된다. 단위 시험: 끝 시각 1초를 나눠 쓰는 두 작업 — 첫째는 100ms에 끝나고 둘째는 응답하지 않음 → 둘째 기다림이 시작부터 1초 + 유예 1초 + 여유 0.5초 안에 끝나고(작업마다 1초씩 더하지 않음) 첫째 값은 남는다. 인터럽트를 무시하는 작업 → 유예 뒤 "멈추지 않음"으로 돌아온다. (2) `SparqlWikidataSignalSource` 와 `CommonsMediaLookup` 이 그 도우미를 쓰고 두 클래스의 private `within`·`ended`·`CANCEL_GRACE` 가 없어진다. `SparqlWikidataSignalSourceTest`·`CommonsMediaLookupTest`·`WorldStoryDayMedia*Test` 를 **고치지 않고** 통과한다(WARN 줄 수·`cause=deadline` 규칙 그대로). (3) **결정 1 (a)**: `WikimediaHttpClient` 에 재시도 없는 호출(오버로드)이 있다 — 읽기 타임아웃보다 늦게 답하는 스텁 → WireMock 요청 정확히 1, `Retry-After` 없는 503 → 요청 1, 둘 다 WARN 1줄. 기존 오버로드는 그대로 재시도한다(`WikimediaHttpClient*Test` 고치지 않고 통과). 날짜 범주 호출이 그것을 쓰고 읽기 타임아웃은 고정값 `media.lookup-deadline` 이다: 컨텍스트 시험에서 Commons 스텁이 상한보다 늦으면 Commons 요청이 정확히 1(지금은 최대 2), 결과는 그대로 200 + `partial: true`. (4) `ActionApiCommonsClient`: `error` 키가 있으면 `code` 유무·형과 상관없이 실패 — 합성 `{"error":{"info":"x"}}`, `{"error":"x"}` → `Pages.failed() == true`, WARN `cause` 가 `api error`(코드가 있으면 `api error <code>`)이고 `info` 글자(`x`)가 로그에 없다. `ratelimited` → `blockHost` 그대로. `ActionApiCommonsClientTest` 고치지 않고 통과. (5) `application.yml` `media.lookup-deadline` 주석과 `MediaLookupProperties` Javadoc이 실제 동작(호출별 읽기 타임아웃, 재시도 없음)을 말한다. (6) `cd server && ./gradlew check` 성공. (7) 보고서에 S1(예상 약 120, 150 넘으면 이유) |
| T1 | PageImages 어댑터, Commons 파일 제목 묶음, 녹화 fixture | backend-dev | T0 | (1) PageImages: `wikimedia.kowiki-api-url` 에 `action=query&prop=pageimages&piprop=name&pilicense=free&pilimit=50&redirects=1&titles=…&format=json&formatversion=2` 가 WireMock 요청에 보인다. 제목은 주소 템플릿 변수로 엄격히 인코딩 — 합성 제목 `A&B`·`C+D` 가 WireMock에서 그대로 읽힌다. 제목 51개 → 요청 1회에 앞 50개. `normalized`·`redirects` 를 따라 요청 제목 → 파일 이름(`File:` 없음). 대표 이미지가 없는 문서는 맵에 없음(실패 아님). `error` 키가 있으면(코드 유무 무관) 실패, `ratelimited` → `blockHost` + 실패, JSON 아님·`query` 없음 → 실패. 예외 없음. (2) Commons 파일 제목 묶음: `titles=File:A\|File:B&prop=imageinfo&iiprop=url\|mime\|size\|extmetadata&iiurlwidth=640&format=json&formatversion=2`(**`iiextmetadatalanguage` 없음** — 요청에 그 키가 없음을 단언), 최대 50개, 템플릿 변수 인코딩(합성 파일 이름 `A&B.jpg`·`C+D.jpg`). 결과는 요청 이름 → 페이지이고 `normalized`(밑줄 → 공백, 첫 글자 대문자)를 따른다. `missing` 페이지는 `CommonsFileMapper` 에서 `MISSING`. `error` 판정은 T0 (4)와 같다. `pages()` 를 바꾼 뒤 기존 날짜 범주 주소 시험이 **고치지 않고** 통과한다. (3) 두 요청 모두 `WikimediaHttpClient` 의 재시도 없는 호출(결정 1 (a)), `User-Agent` = `TEST_USER_AGENT`, 읽기 타임아웃은 부르는 쪽이 준 값(`media.lookup-deadline`). (4) WireMock 503 → WARN `title` 이 `kowiki pageimages of N`·`commons files of N` 이고, 로그 어디에도 `YYYY-MM-DD` 모양·`File:`·`Category:`·합성 문서 제목이 없다. (5) `WikimediaWireMockTest` 리셋에 PageImages 기본 스텁(`/w/api.php` + `prop=pageimages`, **우선순위 1**, 대표 이미지 없는 응답)을 더한다. 시험: `action=query` 만으로 맞추는 pageprops 스텁이 있어도 PageImages 요청은 기본 스텁이 받는다. 기존 컨텍스트 테스트가 고치지 않고 통과한다. (6) 녹화 fixture: `kowiki/pageimages-1988-09-17.json`(제목 3개 한 요청)·`pageimages-2002-06-22.json`, 돌려받은 파일의 `commons/files-<날짜>.json`, 두 `README.md` 에 행 추가(원 URL, 녹화 일자, User-Agent `The-World-That-Day (https://github.com/ericmschoi/The-World-That-Day)`, 라이선스, 언어 지정 없음). 이미지 파일은 받지 않는다. 네트워크를 쓸 수 없으면 "막힘" + PM `curl` 명령(`-H "User-Agent: $WIKIMEDIA_USER_AGENT"`). (7) 보고서에 데모 사건 4개 결과 표(제목, 파일 이름 또는 없음, Commons에 있는지(`missing`), MIME, `LicenseShortName`, `DateTimeOriginal` 원문, 붙을지 여부와 이유)와 `url`·`thumburl` 호스트(M3a3b 기록 `upload.wikimedia.org`·`thumb.wikimedia.org` 와 다르면 따로). 한국어 위키백과가 `pilicense=free` 에 대표 이미지를 주는지도 적는다(리스크 10). (8) `./gradlew check` 성공. 보고서에 T0·T1 누적 S1 — **확인점 1 (320, 알림)** |
| T2 | 세계 API 연결: 사건 사진, ±5년, 중복, 같은 끝 시각, 문서 실패 날, `partial` | backend-dev | T1 | (1) "데모" 표 4행 통과(값은 T1 녹화로 고정). (2) 데모 3날짜 응답의 모든 미디어가 스키마 통과(`date_relation` 뺀 뒤). (3) 합성: 촬영 연도 −6·+6년 → 안 붙음, ±5년 → 붙음, 촬영 시점 미상 → 붙고 `unknown`. `media.max-years-apart: 0` → 같은 해(`years` 0)와 `unknown` 만. (4) `tragic`: `TRAGIC` 사건(`default` 포함) → `media` = `[]` 이고 PageImages 요청 제목에 없음. 다른 `normal` 사건은 후보. (5) `hidden`: `hide` 로 내린 사건의 제목은 PageImages 요청에 없다. (6) 같은 파일이 두 사건에 나오면 앞 사건에만. 사건 사진과 같은 `id` 는 `day_media` 에 없다. 합성 날짜 범주 7장(모두 통과, 그중 하나가 사건 사진) → `day_media` 6장이고 사건 사진 `id` 없음(빼고 나서 자름). 사건 사진 경로가 실패하면 `day_media` 는 M3a3b와 같다. (7) PageImages 503 → `events[].media` 모두 `[]`, `day_media` 는 채워짐, `partial: true`. 파일 제목 묶음 503 → 같은 결과. PageImages `{"error":{"info":"x"}}` → 같은 결과. (8) 요청 수(API 호출 1회당): PageImages ≤ 1, Commons ≤ 2. 1995-03-15는 PageImages 0·Commons 1. 대상 사건이 없으면 PageImages 0. PageImages가 파일을 주지 않으면 파일 제목 묶음 0. (9) **같은 끝 시각**: 응답하지 않는 PageImages 스텁 + 정상 날짜 범주 → `lookup-deadline` + 1초 + 여유 1초 안에 200, `partial: true`, `day_media` 는 남는다. 날짜 범주와 PageImages가 **둘 다** 응답하지 않음 → 여전히 `lookup-deadline` + 1초 + 여유 1초 안(창이 두 개가 아님), 경로마다 WARN 1줄. (10) `media.max-years-apart`: `MediaLookupProperties` 에 있고 음수면 시작 실패(오류 메시지에 설정 이름), 2인자 생성자가 남아 `MediaLookupPropertiesTest`·`CommonsMediaLookupTest` 가 고치지 않고 통과. (11) `MediaLookup` 의 추상 메서드는 `dayMedia` 하나로 남고, 테스트의 `(date, tragicDay) -> DayMedia.NONE` 람다와 `WorldStoryServiceMediaTest.Recording` 이 고치지 않고 컴파일·통과한다(기본 `lookup` 이 `dayMedia` 를 부름). (12) 기존 `WorldStory*Test`·M3a3b 테스트의 **단언은 고치지 않는다**. 바꿀 수 있는 것: 준비 코드(생성자 인자, PageImages 스텁 추가)와 요청 수 단언의 대상 한정(예: `WorldStoryDemoTest` 의 `action=query` 세기를 `prop=pageprops` 로) — 바꾼 테스트 줄을 보고서에 모두 적는다. (13) `Event.of` 가 미디어 목록을 받고, 응답 키·형은 그대로. MockMvc `GET /api/story/world?date=1988-09-17` 의 사건 `media` 항목이 `MediaRecords.STORED_KEYS` 26개 + `date_relation` 이고 값 없는 키가 `null` 로 있다(M3a3b T2 (9)와 같은 시험을 사건 미디어에). (14) **결정 2 (b)**: 위키백과 문서 하나가 `UNAVAILABLE`(날짜 문서·연도 문서 각각, 다른 하나는 내용 있음) → `day_media` = `[]`, 날짜 범주 요청 0, 사건 사진은 그대로 찾음, `partial: true`(지금과 같음). 문서가 `STALE` 이면 그날 사진을 찾는다. (15) 응답 어디에도 `display_mode: link_only` 가 없다. 로그에 날짜·파일 이름·문서 제목·전체 사건 ID가 없다(ID 해시 8자리만). (16) `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 2 (500 넘으면 T3 전에 멈춤)** |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | T2 (확인점 2 통과) | (1) T0·T1·T2 기준 표(기준마다 테스트 이름). 빠진 기준은 테스트를 더한다(제품 코드는 고치지 않음). (2) `cd server && ./gradlew check` 통과, 3회 반복 안정. (3) S1·S2·S3(0이 정상) 보고. (4) 데모 3날짜 미디어 표(자리 `events[n]`/`day_media`, 사건, 순서, `id`, `display_mode`, `license`, `license_status`, `captured_at`, `date_relation`, `place_ref`). (5) 최종 서버 미디어 DTO 필드 목록이 M3a3b T3 (5)와 같은지(웹 `web/src/world/types.ts` 와 대조). (6) 스텁으로 잰 세계 섹션 최악 시간(위키백과·Wikidata·Commons 날짜 범주·PageImages 모두 상한까지 늦을 때)이 M3a3b T3 측정값(약 11.7초) + 0.5초 이하이고, 미디어 구간만 따로 잰 값이 `lookup-deadline` + 1초 + 0.5초 이하. (7) 컨텍스트 테스트에서 WireMock이 받지 않은 바깥 요청이 0(모든 `*-url` 이 WireMock 주소) |
| T4 | 실제 호출 확인 (PR 검수 때) | PM | T3 | (1) `docker compose up -d db`, `cd server && ./gradlew bootRun` 이 뜬다(루트 `.env` 의 `WIKIMEDIA_USER_AGENT` 는 저장소 주소만, ASCII). (2) P1~P3 결과가 "데모" 표와 맞는다. (3) 사건 사진의 `thumbnail_url` 하나를 브라우저로 열어 보이고 `source_url` 페이지의 라이선스가 `license` 와 같다. (4) 사건 사진이 사건과 너무 엉뚱하면 PR 코멘트로 알린다(리스크 1). (5) 머지 뒤 **M3b2 T6**(사건 사진 화면 확인)을 `main` 에서 한다 |

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
  | jq '{partial, events: [.events[] | {title, tone, media: [.media[] | {id, display_mode, license, license_status, captured_at, date_relation, thumbnail_url}]}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' \
  | jq '{partial, events: [.events[] | {title, media: [.media[] | {id, license, date_relation}]}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=1995-03-15' | jq '{events, day_media, notice, partial}'
```

담당별: backend-dev 3 (T0, T1, T2), test-engineer 1 (T3), PM 1 (T4). 모두 5개.

## 로드맵 반영 (2026-10-15, planner)

`docs/roadmap.md` 에 반영했다: 머리말 상태(M3a3b·M3b3 완료, M3a3c 개정 승인과 결정 1~4), "현재 위치", M3 절 머리 수(완료 13·진행 2·남은 5 = 20), 기능 표 M3a3b·M3a3c 행, 비율 문단의 M3a3b 실측, 진행 순서 그림(M3a3b ✔), 후속 후보 "섹션 전체 시간 상한" 행. `feat/privacy-consent` 가 고치는 자리(M3b3b 행, 132행 근처 메모, M3 PM 결정 표, PM 작업 표)는 건드리지 않았다 — M3a3c 결정 1~4 행을 M3 PM 결정 표·"M0 이후 확정 항목"·"결정 시점"에 더하는 일과 PM 작업 표의 M3b2b T6 시점 갱신은 두 브랜치를 합친 뒤에 한다.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 시점 |
|---|---|---|
| **M3b2 T6** (PM, 웹 사건 사진 실제 확인) | 실제 `events[].media` | 이 PR 머지 뒤 `main` 에서. M3b2b T6(그날 사진)을 아직 안 했으면 한자리에서 함께 해도 된다 |
| M3a4 영상 검색 틀 | 공용 시간 상한 도우미, 재시도 없는 호출, `MediaLookup.lookup` 의 사건별 미디어 자리(영상이 더해짐) | 병렬이면 worktree를 나누고 오케스트레이터가 합친다(리스크 5). 기본은 이 PR 머지 뒤 |
| M3a4b Commons 동영상 | 파일 제목 묶음(`CommonsClient`), 매퍼(MIME 조건만 바꿈) | M3a4 뒤 |

## 리스크와 확인할 것

1. **사건 대표 이미지가 사건과 어긋날 수 있다** — 특히 1988의 KBS 두 줄은 원문 그대로 나가므로(M3-P16) 그 문서의 대표 이미지(로고가 아니면 방송국 건물 등)가 붙을 수 있다. `date_relation` 으로 정직하게 보이고 ±5년을 넘으면 붙이지 않는다. 촬영 시점 미상 이미지는 연도 거르기가 안 되므로 "촬영 시점 미상"으로만 보인다. 너무 엉뚱하면 사건 미디어에서 `unknown` 을 빼는 규칙을 후속으로 정할 수 있다.
2. **보수적 `tragic` 이 사진을 지운다** — Wikidata가 실패하거나 QID가 없는 날(큐레이션 밖 사건이 `default` → `tragic`)에는 사건 사진이 없어진다. 의도된 방향(6.3절)이다.
3. **JPEG만** — PNG 사진도 빠진다(M3a3b 리스크 5와 같음).
4. **요청마다 PageImages·Commons를 부른다** — 캐시가 없다. 로컬 데모라 분당 200 제한에 닿지 않는다. 필요하면 M3a4b 결과 캐시에 사진 출처를 더한다(후속).
5. **같은 연결 자리** — M3a4가 병렬로 진행되면 둘 다 `CommonsMediaLookup`·`MediaLookup`·`WorldStoryService`·`Event.of` 를 고친다. worktree를 나누고 오케스트레이터가 합친다. 기본은 M3a3c → M3a4.
6. **세계 섹션 시간 (이월 5)** — 최악 약 13초(최대 15초), 스텁 약 11.7초로 제품 정의서 8절 10초와 웹 시간 초과(10초)를 넘을 수 있다. 이 기능은 사건 사진 경로를 같은 미디어 끝 시각 안에서 동시에 돌려 **늘리지 않는다**("세계 섹션 시간 예산", T2 (9), T3 (6)). 10초 맞추기는 M6b(섹션 전체 상한 또는 Wikidata·미디어 앞당겨 겹치기). 공용 `Semaphore(3)` 중 미디어는 최대 2자리.
7. **위키백과 스텁과의 충돌 (확인됨)** — PageImages는 위키백과 날짜·연도 문서·pageprops와 같은 `/w/api.php` + `action=query` 다. `WorldStoryDemoTest` 등은 `action=query` 만으로 pageprops를 스텁하고 그 요청 수를 센다. 대응: 우선순위 1 기본 스텁(T1 (5)), 요청 수 단언의 대상을 `prop=pageprops` 로 한정(T2 (12) — 단언 값은 그대로).
8. **알려진 한계** — 제외 목록의 `EPA`·`Getty` 안전 쪽 오탐은 M3a3b 리스크 9와 같다. 사건 사진에서 보이면 T3 표에 적는다.
9. **위키백과 문서 하나가 실패한 날 (이월 4)** — 날짜 문서나 연도 문서 하나가 `UNAVAILABLE`(캐시도 없음)이면 그 문서의 비극 사건이 응답에 없어 `tragic` 날 판단에 들어가지 않는다. `STALE`(옛 사본)은 사건이 있으니 해당 없고, 링크 해석 실패는 QID가 없어 사건이 `default` → `tragic` 이 되므로 이미 막힌다. 대응: **결정 2 (b)** — 그런 날은 그날 사진을 찾지 않는다(T2 (14)). 7일 캐시와 옛 사본 덕에 드물어 잃는 사진이 적다. **사건 사진은 같은 구멍이 없다** — 사진은 화면에 나온 `normal` 사건 자기에게만 붙고, 빠진 문서의 사건은 화면에 없으니 그 사건 옆에 사진이 놓일 일이 없다.
10. **PageImages 근거는 영어 위키백과 실측** — 한국어 위키백과도 같은 PageImages 확장이지만 `pilicense=free` 응답은 T1 녹화로 처음 본다. 대표 이미지를 전혀 주지 않거나 모양이 다르면 T1이 "막힘"으로 보고한다(그때 선택지: 사건 사진 없이 머지 / data-researcher 확인). 한국어 위키백과에 **로컬로 올린 자유 파일**은 Commons에 없어 `MISSING` 으로 빠진다(작은 손실, M3-P7과 같은 방향).
11. **크기** — 약 445(370~530)로 400을 넘는다. 결정 3 (a): T0·T1 뒤 320에서 알리고, T2 뒤 500을 넘으면 T3 전에 멈춘다.
12. **재시도를 껐으므로**(결정 1 (a)) 미디어 호출이 순간적인 502/503/504에 한 번 더 시도하지 않고 바로 `partial: true` 가 된다. 미디어는 덤이라 섹션만 빈다. 실제로 자주 보이면 "남은 시간(100ms 단위 올림)을 읽기 타임아웃으로" 방식을 후속으로 다시 본다.
13. **제목 인코딩** — 문서 제목·파일 이름의 `&`·`+` 는 쿼리에서 깨질 수 있다(`+` 는 MediaWiki가 공백으로 읽음). 템플릿 변수로 엄격히 인코딩하고 합성 제목으로 시험한다(T1 (1)(2)).
14. **로그로 날짜를 짐작할 수 있음** — 이 기능은 이름표만 넘기지만, M3a1 위키백과 호출의 WARN 줄은 지금도 문서 제목(`9월 17일`, `1988년`)을 찍는다. 장소가 요청에 들어오는 M2에서 다시 본다(로드맵 후속 후보).

## 이전 결정 (그대로)

| # | 결정 | 상태 |
|---|---|---|
| M3a3b 결정 1 | (a) 두 PR — 이 기능이 사건 사진 | 결정 2026-10-14 |
| M3a3b 결정 2 | (b) M3a3b T2는 backend-dev | 결정 2026-10-14 |
| Commons 질의 언어 | 기본 언어(`iiextmetadatalanguage` 없음) | 결정 2026-10-14 |

새로 묻는 것 없음(확인점 2에서만 다시 묻는다).
