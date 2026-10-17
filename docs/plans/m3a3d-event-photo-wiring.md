# 사건 사진 연결: 세계 API (M3a3d)

> 브랜치: `feat/event-photo-wiring` (M3a3c 머지 뒤 `main` 에서 갈라짐) · 작성: planner (2026-10-16) · 상태: **작성 — PM 확인 필요**. M3a3c 결정 5(2026-10-16, 분할)로 M3a3c의 T2(PM 승인 2026-10-15)와 T3·T4의 사건 사진 부분을 옮겨 왔다. 완료 기준의 규칙은 승인된 그대로이고, 바뀐 것은 머지된 부품 이름, M3a3c T1 녹화로 정해진 데모 값(결정 6), 크기 다시 셈, M3a3c에서 넘어온 메모 3개다. 아래 "PM 결정 필요" 1건(★).
> 기준: PM 미디어 결정 2026-10-11, 안전장치 1~6, `CLAUDE.md` 7절 1·2·3·5·6항, `docs/product-brief.md` 6.1절·6.2절·6.3절·8절·12절, `docs/plans/m3a3c-event-photos.md`(결정 1~6, 부품), `docs/plans/m3a3b-commons-photos.md`(Commons 파일 매퍼·사진 규칙·시간 상한), `docs/plans/m3a3-event-media.md`(형식·R1~R9·`date_relation`), M3-P7, M3-P16 변경(2026-10-12), #22 (c), #15 초안(±5년)
> 선행: **M3a3c 머지**(`Deadline`, `WikimediaHttpClient.getOnce`, `KoWikiPageImages`, `CommonsClient.files`, PageImages 기본 스텁, 녹화 fixture).
> 뒤따르는 기능: **M3b2 T6**(PM 웹 확인, 이 PR 머지 뒤 `main` 에서). M3a4는 같은 미디어 연결 자리를 고친다(리스크 5) — 기본은 이 PR 뒤.

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| 미디어 표시·촬영 시점 미상·안전장치 1~6 | M3a3 계획서와 같음 | 결정 2026-10-11 |
| #13-C | (a) Commons 주소 그대로(핫링크, `no-referrer`) | 결정 |
| #22 | (c) + `tragic` 사건엔 미디어 없음, `tragic` 이 있는 날은 `day_media` 없음 | 결정 |
| M3-P7 | 비자유 위키 로컬 파일 제외 | 결정 |
| M3-P16 | **변경 2026-10-12**: 원문을 거르지 않음 → 응답에 남은 사건은 모두 사진 후보. `hide` 는 바로 내리는 수단 | 결정 |
| 근접 연도 | 사건 사진은 ±5년 안만 (`media.max-years-apart: 5`) | #15 확정 전 초안 |
| Commons 질의 언어 | `iiextmetadatalanguage` 를 보내지 않음(기본 언어) | 결정 2026-10-14 |
| M3a3c 결정 1 | (a) 미디어 호출은 재시도 없이 1회(`getOnce`), 읽기 타임아웃은 고정 `media.lookup-deadline` | 결정 2026-10-15 |
| M3a3c 결정 2 | (b) 위키백과 날짜·연도 문서 중 하나라도 `UNAVAILABLE` 이면 그날 사진을 찾지 않음(`tragic` 날처럼). 사건 사진은 그대로 — **이 기능 T1 (14)** | 결정 2026-10-15 |
| M3a3c 결정 5 | 분할: 사건 사진 연결은 이 기능(M3a3d). M3 PR 21개 | 결정 2026-10-16 |
| M3a3c 결정 6 | 그날에 맞는 사건 사진이 없으면 사진 없이 낸다. **다른 날짜를 찾지 않는다.** PM 실제 확인은 데모 날짜에 사건 사진이 나오지 않고 이상한 것이 보이지 않음을 확인 | 결정 2026-10-16 |

## 목표

1. `GET /api/story/world` 의 `events[].media` 를 Commons **사진**으로 채운다(사건마다 최대 1장). 라이선스가 확인되지 않아도 출처·라이선스 상태·촬영 시점과 함께 직접 보인다. 촬영 시점을 모르는 사진은 "촬영 시점 미상"으로 붙는다. 맞는 사진이 없으면 비운다(결정 6).
2. `tragic` 사건에는 사진을 붙이지 않고 찾지도 않는다. 큐레이션 `hide` 로 내린 사건도 찾지 않는다.
3. 사건과 촬영 연도가 ±5년(`media.max-years-apart`)을 넘는 사진은 붙이지 않는다.
4. 사건 사진과 같은 파일은 `day_media` 에서 뺀다. 같은 파일은 앞 사건에만 붙는다.
5. 사건 사진 조회가 실패하거나 늦어도 사건과 `day_media` 는 그대로 200으로 나가고 `partial: true` 만 켜진다. 세계 섹션 최악 시간은 M3a3b·M3a3c보다 늘지 않는다.
6. 위키백과 문서 하나가 `UNAVAILABLE` 인 날에는 그날 사진을 찾지 않는다(M3a3c 결정 2 (b)).

## 범위 밖

- 다른 날짜의 사건 사진 찾기 — 하지 않는다(결정 6)
- 그날 사진·Commons 파일 매퍼 규칙(M3a3b), 어댑터·시간 상한 도우미(M3a3c) — 쓰기만 한다
- Commons 파일 묶음의 `redirects=1` — 더하지 않는다(리스크 12, 후속 후보)
- 영상 → M3a4~M3a5b, 사진 메타데이터 캐시 → 후속(리스크 4), 미디어 파일 복사·프록시(안전장치 1)
- Wikidata 대표 이미지(P18) — 쓰지 않는다
- 세계 섹션 전체 시간 상한(10초 맞추기) → M6b
- 프론트엔드 → 변경 없음(M3b2가 이미 사건 `media` 를 그린다)

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| 한국어 위키백과 PageImages | **확인됨** — [day-events.md](../research/day-events.md) [48][64][65], M3a3c T1 녹화로 한국어 위키백과 응답 확인(2026-10-16) | `KoWikiPageImages.imageNames` (M3a3c) | `pilicense=free`, 제목 묶음 1회 |
| Wikimedia Commons (Action API) | **확인됨** — [day-events.md 3절 Commons 카드](../research/day-events.md#3-출처-카드) | `CommonsClient.files`, `dayCategory` (M3a3b·M3a3c) | `WikimediaHttpClient` 로만, User-Agent·429·동시 3, 작가·라이선스·원문 페이지 표시, 썸네일은 `thumburl` 만, 핫링크만 |

키 없음. 새 HTTP 호출 코드를 만들지 않는다(M3a3c 어댑터만 부른다). 새 녹화는 없다. 꼭 필요하면 User-Agent는 정확히 `The-World-That-Day (https://github.com/ericmschoi/The-World-That-Day)` — 이메일을 넣지 않는다.

## 이어받는 계약

### M3a3c (선행 — backend-dev 보고 이름)

| 부품 | 실제 모양 | 이 기능에서 |
|---|---|---|
| 공용 시간 상한 | `wikimedia.Deadline`: `static Deadline after(Duration budget)`, `<T> Task<T> start(String name, Callable<T> call)`, `Task.await()` → `Outcome<T>` = `Done(T value)` / `TimedOut(boolean stopped, T late)` / `Failed(Throwable cause)`(`InterruptedException` 이면 기다리던 스레드가 인터럽트된 것). `CANCEL_GRACE` 1s. 끝 시각이 지나 한 작업의 기다림이 시간 초과되면 그 마감의 **끝나지 않은 작업을 모두** 인터럽트한다 | 그날 사진 경로와 사건 사진 경로를 **같은 `Deadline`** 에서 `start` 하고 둘 다 `await`. 늦게 끝나는 경로만 비운다(리스크 14) |
| 재시도 없는 호출 | `WikimediaHttpClient.getOnce(source, title, uri, readTimeout)` | 어댑터 안에서 이미 쓴다. 새로 부르지 않는다 |
| PageImages | `media.pageimages.KoWikiPageImages.imageNames(List<String> titles, Duration readTimeout)` → `Optional<Map<String, String>>`(요청 제목 → 파일 이름, `File:` 없음. 이미지 없는 제목은 맵에 없음. 빈 값 = 실패), `MAX_TITLES = 50` | 대상 사건의 `title` 묶음 |
| Commons 파일 묶음 | `CommonsClient.files(List<String> fileNames, Duration readTimeout)` → `Files(Map<String, JsonNode> byName, boolean failed)`, `MAX_FILES = 50`. `normalized` 를 따르고, Commons에 없는 파일은 `missing` 페이지 | `byName` 의 페이지를 `CommonsFileMapper.map` 으로 |
| Commons 파일 매퍼 | `CommonsFileMapper.map(JsonNode page, Predicate<String> mime, String placeRef, LocalDate date)` → `Result(MediaItem item, Drop drop)`, `Drop` = `MISSING`·`NON_FREE`·`MIME`·`FILTERED`·`RULE` | MIME `image/jpeg`, `placeRef` = 사건 `scope`: `domestic` → `country:KR`, `world` → `world`, `unknown` → `unknown` |
| 미디어 조회 | `MediaLookup.dayMedia(LocalDate, boolean)` → `DayMedia(items, failed)`(함수형 인터페이스, 테스트 6개 파일이 람다로 만듦). `CommonsMediaLookup(CommonsClient, CommonsFileMapper, MediaLookupProperties)` 가 `Deadline` 하나로 날짜 범주를 조회하고 `collect` 안에서 6개로 자른다 | 기본 메서드 `lookup` 을 더하고 `CommonsMediaLookup` 이 재정의. 생성자에 `KoWikiPageImages` 를 더한다. 자르기는 사건 사진 `id` 를 뺀 뒤로 |
| 설정 | `MediaLookupProperties(dayMediaLimit, lookupDeadline)` + 테스트가 부르는 2인자 생성자 | `maxYearsApart`(기본 5) 추가, 2인자 생성자 유지 |
| 테스트 기반 | `WikimediaWireMockTest` 리셋에 Commons 빈 범주 기본 스텁 + PageImages 기본 스텁(`prop=pageimages`, `atPriority(1)`, 대표 이미지 없음) | 이 기능의 PageImages 스텁도 `atPriority(1)` |
| 녹화 fixture | `kowiki/pageimages-1988-09-17.json`(`1988년 하계 올림픽`·`KBS 1TV`·`KBS 2TV` → `KBS 2TV` 만 `KBS_2_logo.svg`), `kowiki/pageimages-2002-06-22.json`(`2002년 FIFA 월드컵` → 없음), `commons/files-1988-09-17.json`(`KBS_2_logo.svg`, SVG) | 데모 값 고정에 쓴다 |

### M3a3b·M3a2 (머지된 코드)

- `WorldStoryService.find`: 판정 → `hidden` 빼고 `normal`·`tragic` 목록 → `media.dayMedia(date, !tragic.isEmpty())` → `normal.addAll(tragic)` → `partial = day.degraded() || judgement.partial() || dayMedia.failed()`.
- `WorldStoryResponse.Event.of(KoWikiEvent, Verdict)` 가 `media` 에 `List.of()`. 테스트는 `Event.of` 를 부르지 않는다.
- `EventToneJudge.Verdict(Tone tone, Basis basis, Scope scope, boolean hidden)`. 사진 대상: `hidden == false` 이고 `tone == NORMAL` 이고 `title`(= `KoWikiEvent.linkTitles().getFirst()`)이 있음.
- `KoWikiDayResult.docs()` = 날짜 문서·연도 문서의 `KoWikiDocStatus`(`state()` = `FRESH`·`STALE`·`ABSENT`·`UNAVAILABLE` 등). `allUnavailable()` 이면 503, `degraded()` 는 `UNAVAILABLE`·`STALE`·링크 해석 실패.
- `DateRelation(Kind kind, int years)` 는 **요청 날짜** 기준(사건 날짜 = 요청 날짜). ±5년은 `years` 의 절댓값. `UNKNOWN` 은 붙인다. `SAME_DAY`·`SAME_MONTH`·`SAME_YEAR` 는 `years` 0.

## M3a3c에서 넘어온 메모

| # | 내용 | 처리 |
|---|---|---|
| 1 | `KoWikiFixtureIntegrityTest` 가 M3a3c T1에서 계획 허용 밖으로 바뀌었다(`pageimages-` fixture를 `pageprops-` 처럼 제외, README 검사 추가). M3a3c 리뷰가 받아들일지 정한다 | 이 기능은 그 테스트를 더 고치지 않는다(T1 (12)). M3a3c 리뷰에서 되돌리라고 하면 그 결과를 따른다 |
| 2 | `ActionApiCommonsClientTest` 에 낡은 주석 "two 200 ms attempts"(재시도가 없어진 뒤로 틀림) | **T2 (8)**: test-engineer가 주석만 고친다(단언·동작 그대로) |
| 3 | Commons 파일 묶음에 `redirects=1` 이 없다 → 이름이 바뀐(넘겨주기된) Commons 파일은 `missing` 으로 돌아와 `MISSING` 으로 빠진다 | 리스크 12(알려진 한계). 이 기능에서 고치지 않는다 |

## 고르는 법

| 자리 | 후보 | 개수·규칙 |
|---|---|---|
| `events[].media` | 응답에 남은 사건 중 `tone == NORMAL` 이고 `title` 이 있는 사건(응답 순서) → `KoWikiPageImages.imageNames` → `CommonsClient.files` → `CommonsFileMapper.map(page, "image/jpeg"::equals, scope 위치, date)` | 사건마다 사진 최대 1(M3a4부터 영상이 더해진다). `tragic` 사건(`default` 포함) 0, PageImages 요청에도 넣지 않음. `hidden` 사건은 요청에 넣지 않음. 대상이 50개를 넘으면 앞 50개만. `years` 의 절댓값 > `media.max-years-apart`(5)면 붙이지 않음(`unknown` 은 붙이고 "촬영 시점 미상"). 같은 파일(`id`)이 두 사건에 나오면 앞 사건에만. 맞는 사진이 없으면 `[]` — 다른 날짜를 찾지 않는다(결정 6) |
| `day_media` 와의 관계 | — | 그날 사진 후보(정렬 뒤)에서 사건 사진과 같은 `id` 를 뺀 **뒤** `media.day-media-limit`(6)개를 자른다. 사건 사진 경로가 실패하면 빼지 않는다 |
| 그날 사진 차단 (결정 2 (b)) | — | 응답에 `tragic` 사건이 있는 날(M3a3b)에 더해, 위키백과 날짜·연도 문서 중 하나라도 `UNAVAILABLE` 인 날도 `day_media` = `[]` 이고 날짜 범주 요청 0. `STALE`(옛 사본)은 해당 없음. 사건 사진은 그대로 찾는다 |

요청 수(API 호출 1회): 위키백과 PageImages 최대 1(대상 사건이 없으면 0), Commons 최대 2(날짜 범주 0~1, 파일 제목 묶음 0~1 — PageImages가 파일을 하나도 주지 않으면 0). 날짜 범주 요청과 PageImages → 파일 제목 묶음은 **동시에** 간다(공용 `Semaphore(3)` 중 최대 2자리).

### 실패와 로그

| 규칙 | 내용 |
|---|---|
| 일부 실패 | PageImages 실패(빈 `Optional`) 또는 파일 제목 묶음 실패(`Files.failed()`) → `events[].media` 만 모두 `[]`, `day_media` 는 그대로, `partial: true`, 200. `tragic` 으로 비운 미디어, 규칙으로 버린 미디어, 대표 이미지가 없는 문서는 `partial` 이 아니다 |
| 시간 상한 | 두 경로가 `Deadline` 하나(`media.lookup-deadline`, 3s)를 나눠 쓴다. 끝 시각에 안 끝난 경로만 비운다. 끝난 경로의 결과는 남는다 |
| 503 | 바뀌지 않음 |
| 로그 | 어댑터의 이름표(`kowiki pageimages of <개수>`, `commons files of <개수>`, `commons day category`)를 그대로 쓴다. 시간 초과 WARN도 그 경로의 이름표로, 실패 하나 = WARN 한 줄(경로마다). 날짜·파일 이름·문서 제목을 넣지 않는다. 버린 이유는 DEBUG에 이유별 개수만(연도 초과는 새 이유 이름, 예: `YEARS`). 사건은 ID 해시 8자리만 |

### 세계 섹션 시간 예산

| 구간 | M3a3c 뒤 (지금) | 이 기능 뒤 |
|---|---|---|
| 위키백과 (`kowiki-day-deadline`) | 5초 | 5초 (그대로) |
| Wikidata (`wikidata-read-timeout` + 유예) | 5초 (+1초) | 그대로 |
| 미디어 (`media.lookup-deadline` + 유예) | 3초 (+1초) — 날짜 범주 1개 | **3초 (+1초)** — 날짜 범주와 사건 사진 경로가 같은 끝 시각을 나눠 씀 |
| 최악 합계 | 약 13초 (최대 15초), 스텁 약 11.7초(M3a3b T3) | **같음**. 스텁 측정은 M3a3b 값 + 0.5초 이하(T2 (6)) |

사건 사진 경로는 요청 2개가 줄지어 3초 안에 끝나야 한다. 늦으면 사건 사진만 비고 `partial: true`. 10초 목표와의 차이는 M6b.

## API / 데이터 변경

- `GET /api/story/world`: `events[].media` 에 M3a3 형식의 사진 레코드(0~1장)가 들어갈 수 있다(데모 3날짜는 모두 `[]` — 결정 6). `day_media` 에서 사건 사진과 같은 파일이 빠진다. 위키백과 문서 하나가 `UNAVAILABLE` 인 날은 `day_media` 가 `[]`(그런 날은 이미 `partial: true`). `partial` 에 사건 사진 실패가 더해진다. 새 에러 코드 없음. 응답 키·형은 그대로(웹 계약 변화 없음, `MediaItem` 키 27개).
- 설정 추가: `media.max-years-apart: 5`(0 이상, 음수면 시작 실패, `MediaLookupProperties`).
- DB 변경 없음. 데이터 파일 없음.

서버 안 계약 (이름은 예시, 머지 코드 이름은 보고서에 적는다):

| 부품 | 모양 |
|---|---|
| 미디어 조회 (`MediaLookup` 에 기본 메서드) | `record EventTarget(String eventId, String title, String placeRef)`, `record WorldMedia(Map<String, MediaItem> eventPhotos, List<MediaItem> dayMedia, boolean failed)`(키는 사건 ID). `default WorldMedia lookup(LocalDate date, List<EventTarget> targets, boolean tragicDay)` 의 기본 구현은 `dayMedia(date, tragicDay)` 만 부르고 사건 사진은 비운다 → 기존 람다·`Recording` 가짜가 고치지 않고 동작. `CommonsMediaLookup` 이 재정의. `dayMedia` 는 `lookup(date, List.of(), tragicDay)` 와 같은 결과 |
| 세계 서비스 | 대상 = `normal` 사건 중 `title` 이 있는 것(응답 순서), `placeRef` 는 `scope` 로. `tragicDay` = 응답에 `tragic` 사건이 있음 **또는** 위키백과 문서 하나라도 `UNAVAILABLE`(결정 2 (b)). `media.lookup(date, targets, tragicDay)` → `Event.of(event, verdict, 사건 사진 0~1장)`, `partial` 에 `\|\| failed` |
| 응답 | `Event.of(KoWikiEvent, Verdict, List<MediaItem>)` |

## 테스트 방침

- **외부 네트워크를 부르지 않는다** — WireMock + M3a3c 녹화 fixture. 컨텍스트 테스트의 모든 `wikimedia.*-url` 은 WireMock을 가리킨다.
- 데모 날짜에는 붙는 사건 사진이 없으므로(결정 6), **붙는 경로는 합성 fixture로 시험한다**(지어낸 문서 제목·파일 이름, JPEG, 촬영 시점을 바꿔 가며). 합성 날짜는 공개 역사 사건 날짜·경계값(예: 2000-02-29). 실제 비극 사건 이름, 실제 사람의 생년월일을 쓰지 않는다.
- 응답 미디어 전부를 `date_relation` 을 뺀 뒤 M3a3 스키마로 검증(`DataSchemas`).
- 시간 시험은 짧은 상한과 응답하지 않는 스텁, 여유 1초(기존 방식).

## 데모 입력 3개로 확인할 것

사건·`tone` 은 M3a2 결과, `day_media` 는 M3a3b 결과. 사건 사진 값은 M3a3c T1 녹화로 정해졌다: **데모 3날짜 모두 사건 사진 없음**(결정 6 — 다른 날짜를 찾지 않는다).

| 데모 입력 | 사건 (M3a2) | 확인 (T1) |
|---|---|---|
| 1988-09-17 + "서울" | 3개, 모두 `normal`/`curated`/`domestic`: ① `1988년 하계 올림픽`(`Q8470`) ② `KBS 1TV`(`Q777278`) ③ `KBS 2TV`(`Q624509`) | PageImages 요청 **1회**에 세 제목. 파일 제목 묶음 요청 1회(`KBS_2_logo.svg` 하나) → 매퍼가 버림(SVG — `MIME`). 세 사건 모두 `media` = `[]`. `day_media` 는 M3a3b 결과 그대로(`id` 목록·순서 같음). Commons 요청 2(날짜 범주 + 파일 묶음). `partial: false`. 응답 어디에도 `.svg` 주소가 없다 |
| 2002-06-22 + "광주 서구 풍암동" | 1개: `2002년 FIFA 월드컵`(`Q47735`), `normal`/`curated` | PageImages 요청 1회(제목 1개) → 대표 이미지 없음 → 파일 묶음 요청 0. 사건 `media` = `[]`. `day_media` 는 M3a3b 결과(1장) 그대로. Commons 요청 1. `partial: false` |
| 1995-03-15 + "서울대학교병원" | 0개, `notice.code` = `NO_EVENTS_FOUND` | PageImages 요청 0, Commons 요청 1(날짜 범주만). `partial: false` |
| (합성) `tragic` 섞인 날 | `tragic`(`default` 포함) 사건 하나 + `normal` 사건 하나 | `tragic` 사건 `media` = `[]`, PageImages 요청에 그 제목 없음, `normal` 사건은 사진 후보. `day_media` = `[]`, 날짜 범주 요청 0 |
| (합성) 사진이 붙는 날 | `normal` 사건 하나, 합성 PageImages가 JPEG 파일 하나를 줌 | 그 사건 `media` 1장, `display_mode: image`, 스키마 통과, `place_ref` 가 사건 `scope` 위치(GPS 없을 때), 같은 파일이 날짜 범주에도 있으면 `day_media` 에서 빠짐 |

## 크기 (400줄 규칙)

제품 코드·설정(S1, `added`) 기준.

### 실측 비율 (M3a3c, 2026-10-16 확인점 1)

| 작업 | 다시 셈 | 실제 S1 | 비율 |
|---|---|---|---|
| M3a3c T0 | 약 120 | 197 | ×1.64 |
| M3a3c T1 | 약 160 | 236 | ×1.48 |
| 합계 | 약 280 | 433 | ×1.55 |

### 다시 셈

| 작업 | 내용 (파일별 셈 — M3a3c 계획서의 T2 셈 그대로) | 셈 | 2026-10-15 보정 (×1.11) | 다시 셈 (×1.5, M3a3c 실측) | 범위 (×1.3~×1.8) |
|---|---|---|---|---|---|
| T1 | `MediaLookup` 기본 메서드·레코드 2개(약 30), `CommonsMediaLookup` 두 경로·사건 사진 고르기·자르기 위치·생성자(약 75), `maxYearsApart`·2인자 생성자(약 12), 서비스·`Event.of`(약 25), 결정 2 (b)(약 5), 설정 약 5 | 150 | 약 165 | **약 250** | 215~300 |
| T2 | 테스트만 (S1 0) | 0 | 0 | 0 | — |
| **합계** | | | | **약 250** | **215~300** |

- 400 아래다. **확인점 (알림): T1 뒤 누적 S1이 320을 넘으면** 오케스트레이터가 PM에게 알리고 계속한다. **400을 넘으면** T2 전에 멈추고 묻는다.
- 테스트(S2)는 따로 보고한다. M3a3c 실측으로 보면 테스트가 제품 코드보다 크다.

## 작업 목록

순서: T1 → **확인점 (320 알림, 400 멈춤)** → T2 → T3(PR 검수 때) → 머지 → M3b2 T6(`main` 에서).

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | ★ 세계 API 연결: 사건 사진, ±5년, 중복, 같은 끝 시각, 문서 실패 날, `partial` (이전 M3a3c T2) | backend-dev (PM 결정 필요 1) | **M3a3c 머지** | (1) "데모" 표 5행 통과(데모 값은 M3a3c 녹화로 고정 — 세 날짜 모두 사건 사진 없음). (2) 데모 3날짜와 합성 "사진이 붙는 날" 응답의 모든 미디어가 스키마 통과(`date_relation` 뺀 뒤). (3) 합성: 촬영 연도 −6·+6년 → 안 붙음, ±5년 → 붙음, 촬영 시점 미상 → 붙고 `unknown`. `media.max-years-apart: 0` → 같은 해(`years` 0)와 `unknown` 만. (4) `tragic`: `TRAGIC` 사건(`default` 포함) → `media` = `[]` 이고 PageImages 요청 제목에 없음. 다른 `normal` 사건은 후보. (5) `hidden`: `hide` 로 내린 사건의 제목은 PageImages 요청에 없다. (6) 같은 파일이 두 사건에 나오면 앞 사건에만. 사건 사진과 같은 `id` 는 `day_media` 에 없다. 합성 날짜 범주 7장(모두 통과, 그중 하나가 사건 사진) → `day_media` 6장이고 사건 사진 `id` 없음(빼고 나서 자름). 사건 사진 경로가 실패하면 `day_media` 는 M3a3b와 같다. (7) PageImages 503 → `events[].media` 모두 `[]`, `day_media` 는 채워짐, `partial: true`. 파일 제목 묶음 503 → 같은 결과. PageImages `{"error":{"info":"x"}}` → 같은 결과. (8) 요청 수(API 호출 1회당): PageImages ≤ 1, Commons ≤ 2. 1988-09-17은 PageImages 1·Commons 2, 2002-06-22는 PageImages 1·Commons 1, 1995-03-15는 PageImages 0·Commons 1. 대상 사건이 없으면 PageImages 0. PageImages가 파일을 주지 않으면 파일 제목 묶음 0. (9) **같은 끝 시각**: 응답하지 않는 PageImages 스텁 + 정상 날짜 범주 → `lookup-deadline` + 1초 + 여유 1초 안에 200, `partial: true`, `day_media` 는 남는다. 날짜 범주와 PageImages가 **둘 다** 응답하지 않음 → 여전히 `lookup-deadline` + 1초 + 여유 1초 안(창이 두 개가 아님), 경로마다 WARN 1줄. 두 경로가 같은 `Deadline` 인스턴스에서 시작한다. (10) `media.max-years-apart`: `MediaLookupProperties` 에 있고 음수면 시작 실패(오류 메시지에 설정 이름), 2인자 생성자가 남아 `MediaLookupPropertiesTest`·`CommonsMediaLookupTest` 의 단언이 그대로 통과(생성자 인자만 바뀔 수 있음 — (12)). (11) `MediaLookup` 의 추상 메서드는 `dayMedia` 하나로 남고, 테스트의 `(date, tragicDay) -> DayMedia.NONE` 람다와 `WorldStoryServiceMediaTest.Recording` 이 고치지 않고 컴파일·통과한다(기본 `lookup` 이 `dayMedia` 를 부름). (12) 기존 `WorldStory*Test`·M3a3b·M3a3c 테스트의 **단언은 고치지 않는다**. 바꿀 수 있는 것: 준비 코드(생성자 인자 — 예: `CommonsMediaLookup` 에 `KoWikiPageImages`, PageImages 스텁 추가)와 요청 수 단언의 대상 한정(예: `WorldStoryDemoTest` 의 `action=query` 세기를 `prop=pageprops` 로). `KoWikiFixtureIntegrityTest` 는 더 고치지 않는다(메모 1). 바꾼 테스트 줄을 보고서에 모두 적는다. (13) `Event.of` 가 미디어 목록을 받고, 응답 키·형은 그대로. MockMvc로 합성 "사진이 붙는 날"을 부르면 사건 `media` 항목이 `MediaRecords.STORED_KEYS` 26개 + `date_relation` 이고 값 없는 키가 `null` 로 있다(M3a3b T2 (9)와 같은 시험을 사건 미디어에). (14) **결정 2 (b)**: 위키백과 날짜 문서 또는 연도 문서 하나가 `UNAVAILABLE`(다른 하나는 내용 있음, 경우마다 시험) → `day_media` = `[]`, 날짜 범주 요청 0, 사건 사진은 그대로 찾음, `partial: true`. 문서가 `STALE` 이면 그날 사진을 찾는다. (15) 응답 어디에도 `display_mode: link_only` 가 없다. 로그에 날짜·파일 이름·문서 제목·전체 사건 ID가 없다(ID 해시 8자리만). (16) `cd server && ./gradlew check` 성공. 보고서에 누적 S1 — **확인점 (320 알림, 400 멈춤)** |
| T2 | 완료 기준 검증과 전체 테스트 (이전 M3a3c T3의 사건 사진 부분) | test-engineer | T1 (확인점 통과) | (1) T1 기준 표(기준마다 테스트 이름). 빠진 기준은 테스트를 더한다(제품 코드는 고치지 않음). (2) `cd server && ./gradlew check` 통과, 3회 반복 안정. (3) S1·S2·S3(0이 정상) 보고. (4) 데모 3날짜 미디어 표(자리 `events[n]`/`day_media`, 사건, 순서, `id`, `display_mode`, `license`, `license_status`, `captured_at`, `date_relation`, `place_ref`) — 사건 자리는 모두 비어 있음을 표에 적는다. (5) 최종 서버 미디어 DTO 필드 목록이 M3a3b T3 (5)와 같은지(웹 `web/src/world/types.ts` 와 대조). (6) 스텁으로 잰 세계 섹션 최악 시간(위키백과·Wikidata·Commons 날짜 범주·PageImages 모두 상한까지 늦을 때)이 M3a3b T3 측정값(약 11.7초) + 0.5초 이하이고, 미디어 구간만 따로 잰 값이 `lookup-deadline` + 1초 + 0.5초 이하. (7) 컨텍스트 테스트에서 WireMock이 받지 않은 바깥 요청이 0. (8) 메모 2: `ActionApiCommonsClientTest` 의 낡은 주석 "two 200 ms attempts" 를 실제 동작(시도 1회)에 맞게 고친다 — 주석만, 단언 그대로 |
| T3 | 실제 호출 확인 (PR 검수 때) | PM | T2 | (1) `docker compose up -d db`, `cd server && ./gradlew bootRun` 이 뜬다(루트 `.env` 의 `WIKIMEDIA_USER_AGENT` 는 저장소 주소만, ASCII). (2) P1·P2: 1988-09-17과 2002-06-22의 모든 사건 `media` 가 `[]` 이다(결정 6 — 정상). 응답 어디에도 로고·`.svg` 주소·`link_only` 가 없고, `day_media` 는 M3a3b 때와 같은 사진, `partial: false`. (3) P3: 1995-03-15는 M3a3b 때와 같다. (4) 다른 날짜로 사건 사진을 찾아보지 않는다(결정 6). (5) 머지 뒤 **M3b2 T6**을 `main` 에서 한다 — 데모 날짜의 사건 카드에 사진 자리가 비어 있거나 없고 깨진 이미지가 없음을 확인 |

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
  | jq '{partial, events: [.events[] | {title, tone, media}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' \
  | jq '{partial, events: [.events[] | {title, media}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=1995-03-15' | jq '{events, day_media, notice, partial}'
```

담당별: backend-dev 1 (T1), test-engineer 1 (T2), PM 1 (T3). 모두 3개.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 시점 |
|---|---|---|
| **M3b2 T6** (PM, 웹 사건 사진 확인) | 실제 `events[].media`(데모 날짜는 빈 목록) | 이 PR 머지 뒤 `main` 에서. M3b2b T6을 아직 안 했으면 함께 |
| M3a4 영상 검색 틀 | `MediaLookup.lookup` 의 사건별 미디어 자리(영상이 더해짐), 같은 `Deadline` | 기본은 이 PR 머지 뒤. 병렬이면 worktree를 나누고 오케스트레이터가 합친다(리스크 5) |

## 리스크와 확인할 것

1. **사건 대표 이미지가 사건과 어긋날 수 있다** — 원문 그대로 나가는 줄(M3-P16)의 대표 이미지가 붙을 수 있다. `date_relation` 으로 정직하게 보이고 ±5년을 넘으면 붙이지 않는다. 촬영 시점 미상 이미지는 연도 거르기가 안 되므로 "촬영 시점 미상"으로만 보인다. 너무 엉뚱하면 사건 미디어에서 `unknown` 을 빼는 규칙을 후속으로 정할 수 있다.
2. **보수적 `tragic` 이 사진을 지운다** — Wikidata가 실패하거나 QID가 없는 날에는 사건 사진이 없어진다. 의도된 방향(6.3절).
3. **JPEG만** — PNG 사진도 빠진다.
4. **요청마다 PageImages·Commons를 부른다** — 캐시가 없다. 로컬 데모라 제한에 닿지 않는다. 필요하면 M3a4b 결과 캐시에 사진 출처를 더한다(후속).
5. **같은 연결 자리** — M3a4가 병렬로 진행되면 둘 다 `CommonsMediaLookup`·`MediaLookup`·`WorldStoryService`·`Event.of` 를 고친다. 기본은 M3a3d → M3a4.
6. **세계 섹션 시간** — 최악 약 13초(최대 15초), 스텁 약 11.7초로 10초 목표를 넘을 수 있다. 이 기능은 사건 사진 경로를 같은 미디어 끝 시각 안에서 동시에 돌려 늘리지 않는다(T1 (9), T2 (6)). 10초 맞추기는 M6b. 공용 `Semaphore(3)` 중 미디어는 최대 2자리.
7. **위키백과 스텁과의 충돌** — `WorldStoryDemoTest` 등은 `action=query` 만으로 pageprops를 스텁하고 그 요청 수를 센다. PageImages 요청은 M3a3c의 우선순위 1 기본 스텁이 받지만, 연결이 생기면 `action=query` 요청 수가 늘어난다 → 세는 대상을 `prop=pageprops` 로 한정(T1 (12), 단언 값은 그대로).
8. **알려진 한계** — 제외 목록의 `EPA`·`Getty` 안전 쪽 오탐(M3a3b 리스크 9). 사건 사진에서 보이면 T2 표에 적는다.
9. **위키백과 문서 하나가 실패한 날** — 빠진 문서의 비극 사건이 `tragic` 날 판단에 들어가지 않는 문제는 결정 2 (b)로 막는다(T1 (14)). 사건 사진은 같은 구멍이 없다 — 사진은 화면에 나온 `normal` 사건 자기에게만 붙는다.
10. **위키 로컬 자유 파일** — 한국어 위키백과에 로컬로 올린 자유 파일은 Commons에 없어 `MISSING` 으로 빠진다(작은 손실, M3-P7과 같은 방향).
11. **재시도 없음** (M3a3c 결정 1 (a)) — 순간적인 502/503/504에 바로 `partial: true`. 자주 보이면 후속으로 다시 본다.
12. **Commons 파일 묶음에 `redirects=1` 이 없다** (메모 3) — 이름이 바뀐 Commons 파일은 `MISSING` 으로 빠진다. 사건 사진이 이 때문에 빠지는 일이 실제로 보이면 후속으로 더한다(로드맵 후속 후보로 올릴 수 있음).
13. **데모 날짜에는 붙는 사건 사진이 없다** (결정 6) — PM 실제 확인에서 사진이 붙는 경로가 눈에 보이지 않는다. 대응: 붙는 경로·연도·중복·`day_media` 빼기를 합성 fixture로 시험(T1 (2)(3)(6)(13), "데모" 표 합성 행). 다른 날짜로 찾아 보이게 하지 않는다.
14. **`Deadline` 의 취소 범위** — 끝 시각이 지나 한 작업의 기다림이 시간 초과되면 같은 마감의 끝나지 않은 작업이 모두 인터럽트된다. 두 경로는 같은 끝 시각을 쓰므로 맞는 동작이지만, 이미 끝난 경로의 결과를 먼저 받아 두지 않으면 잃을 수 있다 → T1 (9)의 "하나만 늦을 때 다른 쪽 결과가 남는다" 시험으로 지킨다.
15. **로그로 날짜를 짐작할 수 있음** — M3a1 위키백과 호출의 WARN 줄은 문서 제목을 찍는다. M2에서 다시 본다(로드맵 후속 후보).
16. **크기** — 약 250(215~300). M3a3c처럼 ×1.5를 넘어도 400 아래일 가능성이 크다. T1 뒤 320 알림, 400 멈춤.

## PM 결정 필요

| # | 질문 | 선택지 | 추천 |
|---|---|---|---|
| 1 | ★ T1(세계 API 연결 — 두 경로를 한 `Deadline` 에서 동시에 돌리기, 사건 사진 고르기, 기본 메서드로 기존 테스트 지키기)을 PM이 직접 구현할까 | **(a)** PM 직접 / **(b)** backend-dev | **(b)** — M3a3c T0·T1을 만든 backend-dev가 부품을 그대로 이어 쓰면 빠르다. 배우고 싶으면 PR 리뷰 때 `CommonsMediaLookup` 의 두 경로 부분을 함께 읽기를 권한다 |
