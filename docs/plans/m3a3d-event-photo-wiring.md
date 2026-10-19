# 사건 사진 연결: 세계 API (M3a3d)

> 브랜치: `feat/event-photo-wiring` (`main` 25d5227 — PR #20 M3a3c 머지 뒤 — 에서 갈라짐) · 작성: planner (2026-10-16, 같은 날 머지 코드에 맞춰 개정) · 상태: **PM 승인 (2026-10-17)** — 결정 1 **(a)** 한 PR, 확인점 1(T0 뒤 누적 S1 130 넘으면 멈춤)·확인점 2(T1 뒤 420 넘으면 알림). M3a3c 결정 5(분할)로 M3a3c의 T2(PM 승인 2026-10-15)와 T3·T4의 사건 사진 부분을 옮겨 왔다. 사건 사진 규칙은 승인된 그대로다. 이번 개정에서 바뀐 것은 아래 "이번 개정에서 바뀐 것" 표.
> 기준: PM 미디어 결정 2026-10-11, 안전장치 1~6, `CLAUDE.md` 7절 1·2·3·5·6항, `docs/product-brief.md` 6.1절·6.2절·6.3절·8절·12절, `docs/plans/m3a3c-event-photos.md`(결정 1~6, 부품), `docs/plans/m3a3b-commons-photos.md`(Commons 파일 매퍼·사진 규칙·시간 상한), `docs/plans/m3a3-event-media.md`(형식·R1~R9·`date_relation`), M3-P7, M3-P16 변경(2026-10-12), #22 (c), #15 초안(±5년), PM 피드백 2026-10-16(다른 날짜를 찾지 않음)
> 선행: **M3a3c 머지됨 (PR #20, `main` 25d5227)** — `Deadline`, `WikimediaHttpClient.getOnce`, `KoWikiPageImages`, `CommonsClient.files`, PageImages 기본 스텁, 녹화 fixture.
> 뒤따르는 기능: **M3b2 T6**(PM 웹 확인, 이 PR 머지 뒤 `main` 에서). M3a4는 같은 미디어 연결 자리를 고친다(리스크 5) — 기본은 이 PR 뒤.

## 이번 개정에서 바뀐 것 (2026-10-16, PR #20 머지 코드 대조)

| 항목 | 내용 |
|---|---|
| 이름 대조 | "이어받는 계약"의 모든 클래스·메서드 이름을 `main` 25d5227 코드로 다시 확인했다. 틀린 이름은 없었다. 새로 적은 것: `Deadline` 은 `AutoCloseable` 이 아니고 `cancelAll()` 이 `private`, `Failed(InterruptedException)` 은 "기다리던 스레드가 인터럽트됨"과 "작업이 스스로 던짐"을 가르지 못함, `CommonsClient.files` 는 `query` 없는 답을 빈 맵(성공)으로 봄, 두 어댑터에 같은 `error` 검사 블록이 있음 |
| PR #20 리뷰 권장 5개 | 모두 이 기능에서 처리한다 — 2·3·4·5는 **새 T0**(연결 전 정리), 1은 **T2 (6)**. 아래 "PR #20 리뷰 권장" 표 |
| 깨지는 기존 단언 | 연결이 생기면 M3a3c의 "아직 연결 안 됨" 회귀 시험(`WorldStoryEventPhotoPartsRegressionTest`)의 "PageImages 요청 0" 단언과 `WorldStoryDemoTest` 의 위키백과 요청 수가 바뀐다 → 바꿔도 되는 줄을 T1 (12)에 정확히 적었다 |
| 메모 1 | `KoWikiFixtureIntegrityTest` 변경은 M3a3c 리뷰가 받아들였다(필수 0) → 닫음 |
| 크기 | 파일별 셈 대비 M3a3c 실측 비율(약 ×1.7)로 다시 셌다. T0 약 110 + T1 약 290 = **약 400 (350~445)** → 확인점 2개와 분할 선택지(PM 결정 1 → **(a)**, 2026-10-17) |
| 사건별 미디어 자리 | `WorldMedia` 의 사건 미디어를 `Map<String, List<MediaItem>>`(사건 ID → 0~1장)으로 — M3a4가 영상을 같은 목록에 더한다 |

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
| M3a3c 결정 2 | (b) 위키백과 날짜·연도 문서 중 하나라도 `UNAVAILABLE` 이면 그날 사진을 찾지 않음(`tragic` 날처럼). 사건 사진은 그대로 — **T1 (14)** | 결정 2026-10-15 |
| M3a3c 결정 5 | 분할: 사건 사진 연결은 이 기능(M3a3d). M3 PR 21개 | 결정 2026-10-16 |
| M3a3c 결정 6 · PM 피드백 2026-10-16 | 그날에 맞는 사건 사진이 없으면 사진 없이 낸다. **기능을 보이거나 확인하려고 다른 날짜를 찾지 않고, 제안하지도 않는다.** PM 실제 확인은 데모 날짜에 사건 사진이 나오지 않고 이상한 것이 보이지 않음을 확인 | 결정 2026-10-16 |
| M3a3d T1 담당 | ★ backend-dev | **결정 2026-10-16** |
| M3a3d 결정 1 | **(a)** 한 PR. 확인점 1: T0 뒤 누적 S1 130 넘으면 T1 전에 멈추고 T0만 낼지 물음. 확인점 2: T1 뒤 420 넘으면 알림 | **결정 2026-10-17** |

## 목표

1. `GET /api/story/world` 의 `events[].media` 를 Commons **사진**으로 채운다(사건마다 최대 1장). 라이선스가 확인되지 않아도 출처·라이선스 상태·촬영 시점과 함께 직접 보인다. 촬영 시점을 모르는 사진은 "촬영 시점 미상"으로 붙는다. 맞는 사진이 없으면 비운다(결정 6).
2. `tragic` 사건에는 사진을 붙이지 않고 찾지도 않는다. 큐레이션 `hide` 로 내린 사건도 찾지 않는다.
3. 사건과 촬영 연도가 ±5년(`media.max-years-apart`)을 넘는 사진은 붙이지 않는다.
4. 사건 사진과 같은 파일은 `day_media` 에서 뺀다. 같은 파일은 앞 사건에만 붙는다.
5. 사건 사진 조회가 실패하거나 늦어도 사건과 `day_media` 는 그대로 200으로 나가고 `partial: true` 만 켜진다. 미디어 구간은 두 경로가 끝 시각 하나를 나눠 써서 M3a3b·M3a3c보다 늘지 않는다.
6. 위키백과 문서 하나가 `UNAVAILABLE` 인 날에는 그날 사진을 찾지 않는다(M3a3c 결정 2 (b)).
7. 연결 전에 PR #20 리뷰 권장 2~5를 닫는다(T0): 마감에 `close()`, 인터럽트 구분, 파일 묶음 실패 판정 맞춤, `error` 검사 공용화와 로그 코드 제한.

## 범위 밖

- 다른 날짜의 사건 사진 찾기 — 하지 않는다(결정 6, PM 피드백 2026-10-16)
- 그날 사진·Commons 파일 매퍼 규칙(M3a3b) — 쓰기만 한다. `MediaFilter`·`MediaNormalizer` 는 직접 부르지 않는다(`CommonsFileMapper.map` 안에서 기록 → `MediaFilter.check` → `MediaNormalizer.normalize(record, date)` 순서로 이미 돈다)
- Commons 파일 묶음의 `redirects=1` — 더하지 않는다(리스크 12, 후속 후보)
- 위키백과 사건 어댑터(`ActionApiKoWikiClient`·`ActionApiKoWikiLinkResolver`)의 `error` 처리 — T0의 공용 검사는 미디어 어댑터 둘에만 쓴다(다음 단계 제안)
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

## 이어받는 계약 (`main` 25d5227에서 확인)

| 부품 | 실제 모양 | 이 기능에서 |
|---|---|---|
| 공용 시간 상한 | `wikimedia.Deadline` (final class): `static Deadline after(Duration budget)`, `<T> Task<T> start(String name, Callable<T> call)`(가상 스레드), `Task.await()` → sealed `Outcome<T>` = `Done(T value)` / `TimedOut(boolean stopped, T late)` / `Failed(Throwable cause)`. `CANCEL_GRACE` 1s. 한 작업의 기다림이 끝 시각을 넘으면 `private cancelAll()` 로 **끝나지 않은 작업을 모두** 인터럽트하고 유예까지 기다린다. 이미 끝난 작업은 취소 표시를 받지 않아 결과가 남는다. `Failed(InterruptedException)` 은 기다리던 스레드가 인터럽트된 경우에도, 작업이 스스로 던진 경우에도 나온다. **기다리지 않은 작업은 끝 시각에 취소되지 않는다** | T0이 `AutoCloseable`·`Interrupted` 를 더한다(리뷰 권장 2·3). T1은 두 경로를 **같은 `Deadline`** 에서 `start` 하고 둘 다 `await`, try-with-resources로 닫는다 |
| 재시도 없는 호출 | `WikimediaHttpClient.getOnce(String source, String title, URI uri, Duration readTimeout)` → `WikimediaResponse`. 실패 때 WARN 한 줄(`source`·`title`·원인 — 주소·본문 없음). 429·`Retry-After` 503은 호스트를 막음 | 어댑터 안에서 이미 쓴다. 새로 부르지 않는다 |
| PageImages | `media.pageimages.KoWikiPageImages.imageNames(List<String> titles, Duration readTimeout)` → `Optional<Map<String, String>>`(요청 제목 → 파일 이름, `File:` 없음, `normalized`·`redirects` 따름, 대표 이미지 없는 제목은 맵에 없음, 빈 `Optional` = 실패), `MAX_TITLES = 50`. 구현 `ActionApiKoWikiPageImages`(`source=kowiki`, 이름표 `kowiki pageimages of <개수>`). `query` 가 객체가 아니면 실패(`no query`) | 대상 사건의 `title` 묶음. 함수형 인터페이스라 테스트는 람다로 만든다 |
| Commons 파일 묶음 | `CommonsClient.files(List<String> fileNames, Duration readTimeout)` → `Files(Map<String, JsonNode> byName, boolean failed)`, `MAX_FILES = 50`, 빈 이름·중복 제거, `normalized` 따름, 없는 파일은 `missing` 페이지. 구현 `ActionApiCommonsClient`(`source=commons`, 이름표 `commons files of <개수>`). **`query` 없는 답은 지금 빈 맵(성공)** | T0이 실패로 맞춘다(리뷰 권장 4). `byName` 의 페이지를 `CommonsFileMapper.map` 으로 |
| 날짜 범주 | `CommonsClient.dayCategory(LocalDate, Duration)` → `Pages(List<JsonNode> pages, boolean failed)`. `query` 없는 답(`{"batchcomplete":true}`)은 **빈 범주이고 실패가 아니다**(범주가 없는 날의 실제 답, 기본 스텁) | 그대로 둔다 |
| Commons 파일 매퍼 | `CommonsFileMapper.map(JsonNode page, Predicate<String> mime, String placeRef, LocalDate date)` → `Result(MediaItem item, Drop drop)`, `Drop` = `MISSING`·`NON_FREE`·`MIME`·`FILTERED`·`RULE` | MIME `"image/jpeg"::equals`, `placeRef` = 사건 `scope`: `domestic` → `country:KR`, `world` → `world`, `unknown` → `unknown`(스키마 `^(world\|unknown\|[a-z]+:\S+)$` 통과). GPS가 있으면 좌표가 이긴다 |
| 미디어 조회 | `media.MediaLookup`: 추상 메서드 `DayMedia dayMedia(LocalDate date, boolean tragicDay)` 하나, `record DayMedia(List<MediaItem> items, boolean failed)`(`NONE`·`FAILED`). 테스트 6곳이 람다 `(date, tragicDay) -> DayMedia.NONE` 로 만들고(`WorldStoryServiceTest`·`WorldStoryServiceHideTest`·`WorldStoryCuratedHideTest`·`WorldStoryControllerTest`·`CuratedEventToneJudgeTest` 등), `WorldStoryServiceMediaTest.Recording` 이 `dayMedia` 만 구현 | 기본 메서드 `lookup` 을 더한다(추상 메서드는 그대로 하나) |
| Commons 조회 | `media.commons.CommonsMediaLookup(CommonsClient, CommonsFileMapper, MediaLookupProperties)`: `Deadline.after(lookupDeadline).start("commons", …)` 하나, `collect` 안에서 제목순 → `rank`(same_day 0, 기타 1, unknown 2) 정렬 → `dayMediaLimit` 개로 자름. 시간 초과 WARN은 `source=commons title=commons day category cause=deadline`(어댑터가 이미 WARN했으면 생략) | 생성자에 `KoWikiPageImages` 를 더하고 `lookup` 을 재정의. 자르기는 사건 사진 `id` 를 뺀 뒤로 |
| 설정 | `MediaLookupProperties(int dayMediaLimit = 6, Duration lookupDeadline = 3s)`(음수·0 검사) | `maxYearsApart`(기본 5, 음수면 시작 실패) 추가, 테스트가 부르는 2인자 생성자 유지 |
| 세계 서비스 | `WorldStoryService.find`: 503 검사(`day.allUnavailable()`) → 판정 → `hidden` 빼고 `normal`·`tragic` 목록(이때 `Event.of(event, verdict)`) → `media.dayMedia(date, !tragic.isEmpty())` → `normal.addAll(tragic)` → `partial = day.degraded() \|\| judgement.partial() \|\| dayMedia.failed()`. 숨긴 사건은 ID 해시만 DEBUG | 대상 고르기·`tragicDay` 확장·`lookup` 호출·`Event.of` 에 미디어 |
| 응답 | `WorldStoryResponse.Event.of(KoWikiEvent, Verdict)` 가 `media` 에 `List.of()`. 테스트는 `Event.of` 를 부르지 않는다 | `Event.of(KoWikiEvent, Verdict, List<MediaItem>)` |
| 판정 | `EventToneJudge.Verdict(Tone tone, Basis basis, Scope scope, boolean hidden)`, `Scope` = `DOMESTIC`·`WORLD`·`UNKNOWN`. 사진 대상: `hidden == false` 이고 `tone == NORMAL` 이고 `title`(= `KoWikiEvent.linkTitles().getFirst()`)이 있음 | 그대로 |
| 문서 상태 | `KoWikiDayResult.docs()` = 날짜 문서·연도 문서의 `KoWikiDocStatus`, `state()` 는 `KoWikiPage.State`(`FRESH`·`CACHED`·`STALE`·`ABSENT`·`UNAVAILABLE`). `degraded()` = `UNAVAILABLE`·`STALE`·링크 해석 실패 | 결정 2 (b)는 `UNAVAILABLE` 만 본다 |
| 날짜 관계 | `DateRelation(Kind kind, int years)` 는 **요청 날짜** 기준. `YEARS_APART`·`APPROXIMATE` 만 `years` ≠ 0. `CaptureTimeParser` 는 `CURRENT` 를 만들지 않으므로 Commons 사진에는 나오지 않는다 | ±5년은 `Math.abs(years)` |
| 테스트 기반 | `WikimediaWireMockTest` 리셋: Commons 빈 범주 기본 스텁 + PageImages 기본 스텁(`prop=pageimages`, `atPriority(1)`, `NO_PAGE_IMAGES` = 대표 이미지 없음). 컨텍스트 읽기 타임아웃 2s | 이 기능의 PageImages 스텁도 `atPriority(1)` |
| 녹화 fixture | `kowiki/pageimages-1988-09-17.json`(`1988년 하계 올림픽`·`KBS 1TV`·`KBS 2TV` → `KBS 2TV` 만 `KBS_2_logo.svg`), `kowiki/pageimages-2002-06-22.json`(`2002년 FIFA 월드컵` → 없음), `commons/files-1988-09-17.json`(`KBS_2_logo.svg`, SVG) | 데모 값 고정에 쓴다 |

## PR #20 리뷰 권장 (M3a3c 코드 리뷰, 승인 · 필수 0 · 권장 5)

| # | 내용 | 결정 | 이유 |
|---|---|---|---|
| 1 | 최악 시간 시험 `worstTimeStaysWithinTheM3a3bMeasurementAndCommonsIsAskedOnce` 의 상한 12.2s(측정 11.7 + 0.5)가 여유 약 0.5초. server-ci는 통과 | **이 기능 T2 (6)** | 이 기능이 그 시험의 PageImages 단언을 어차피 바꾸고(T1 (12)), 최악 시간에 PageImages 지연을 더한다. 상한을 `WorldStoryDayMediaAcceptanceTest` 처럼 설정값에서 계산하고, "미디어 창이 두 개가 아님"은 미디어 구간만 재는 설정값 상한으로 따로 지킨다 |
| 2 | `Deadline`: 아무도 기다리지 않는 작업은 끝 시각에 취소되지 않는다 | **T0 (1)** — `Deadline implements AutoCloseable`, `close()` 가 끝나지 않은 작업을 인터럽트 | 이 기능이 처음으로 한 마감에 작업 두 개를 올린다. 첫 `await` 와 둘째 `await` 사이에서 예외가 나면 둘째 작업이 공용 `Semaphore(3)` 한 자리를 읽기 타임아웃(3초)까지 쥔다. M3a4가 세 번째 경로(영상)를 더한다 |
| 3 | 작업 자신의 `InterruptedException` 과 기다리던 스레드의 인터럽트를 가르지 않아 WARN 없이 실패한다 | **T0 (2)** — `Outcome` 에 `Interrupted` 를 더하고, `Failed` 는 늘 "작업이 던짐"(WARN) | 실패 하나 = WARN 한 줄 규칙. 두 경로에서 원인 모를 `partial` 이 생기면 로그로 찾을 수 없다. 고치는 곳은 `switch` 두 개(`CommonsMediaLookup`·`SparqlWikidataSignalSource`)뿐 |
| 4 | `CommonsClient.files`: `query` 없는 답이 빈 맵, PageImages는 실패 | **T0 (3)** — 파일 묶음은 `query.pages` 배열이 없으면 실패. 날짜 범주는 그대로(빈 범주의 실제 답) | 연결 전에 맞춰야 `partial` 이 맞다(제목을 물었는데 `query` 가 없으면 이상한 답이지 "사진 없음"이 아님) |
| 5 | `error` 검사 블록이 두 어댑터에 중복이고 원격 `code` 를 그대로 로그에 찍는다 | **T0 (4)** — `wikimedia` 패키지의 공용 검사 하나, 로그 코드는 `[a-z0-9-]{1,64}` 만 | 원격 값이 로그 줄을 꾸밀 수 없게(줄바꿈·임의 문자) 한다. 미디어 어댑터 둘에만 쓴다(위키백과 사건 어댑터는 다음 단계 제안) |

## 고르는 법

| 자리 | 후보 | 개수·규칙 |
|---|---|---|
| `events[].media` | 응답에 남은 사건 중 `tone == NORMAL` 이고 `title` 이 있는 사건(응답 순서) → `KoWikiPageImages.imageNames` → `CommonsClient.files` → `CommonsFileMapper.map(page, "image/jpeg"::equals, scope 위치, date)` | 사건마다 사진 최대 1(M3a4부터 영상이 더해진다). `tragic` 사건(`default` 포함) 0, PageImages 요청에도 넣지 않음. `hidden` 사건은 요청에 넣지 않음. 대상이 50개를 넘으면 앞 50개만. `Math.abs(years)` > `media.max-years-apart`(5)면 붙이지 않음(`unknown` 은 붙이고 "촬영 시점 미상"). 같은 파일(`id`)이 두 사건에 나오면 앞 사건에만. 맞는 사진이 없으면 `[]` — 다른 날짜를 찾지 않는다(결정 6) |
| `day_media` 와의 관계 | — | 그날 사진 후보(정렬 뒤)에서 사건 사진과 같은 `id` 를 뺀 **뒤** `media.day-media-limit`(6)개를 자른다. 사건 사진 경로가 실패하거나 늦으면 빼지 않는다. `day-media-limit: 0` 은 그날 사진만 끈다(사건 사진은 찾음) |
| 그날 사진 차단 (결정 2 (b)) | — | 응답에 `tragic` 사건이 있는 날(M3a3b)에 더해, 위키백과 날짜·연도 문서 중 하나라도 `UNAVAILABLE` 인 날도 `day_media` = `[]` 이고 날짜 범주 요청 0. `STALE`(옛 사본)·`ABSENT`(문서 없음)는 해당 없음. 사건 사진은 그대로 찾는다 |

요청 수(API 호출 1회): 위키백과 PageImages 최대 1(대상 사건이 없으면 0), Commons 최대 2(날짜 범주 0~1, 파일 제목 묶음 0~1 — PageImages가 파일을 하나도 주지 않으면 0). 날짜 범주 요청과 PageImages → 파일 제목 묶음은 **동시에** 간다(공용 `Semaphore(3)` 중 최대 2자리).

### 실패와 로그

| 규칙 | 내용 |
|---|---|
| 일부 실패 | PageImages 실패(빈 `Optional`) 또는 파일 제목 묶음 실패(`Files.failed()` — T0 뒤 `query` 없는 답 포함) → `events[].media` 만 모두 `[]`, `day_media` 는 그대로, `partial: true`, 200. `tragic` 으로 비운 미디어, 규칙으로 버린 미디어, 대표 이미지가 없는 문서는 `partial` 이 아니다 |
| 시간 상한 | 두 경로가 `Deadline` 하나(`media.lookup-deadline`, 3s)를 나눠 쓴다. 끝 시각에 안 끝난 경로만 비운다. 끝난 경로의 결과는 남는다. 조회가 끝나면(정상·예외 모두) try-with-resources가 마감을 닫는다 |
| 503 | 바뀌지 않음 |
| 로그 | 어댑터의 이름표(`kowiki pageimages of <개수>`, `commons files of <개수>`, `commons day category`)를 그대로 쓴다. 사건 사진 경로의 시간 초과·예외 WARN은 고정 이름표 `source=media title=event photos of <대상 개수>` 로, 어댑터가 이미 WARN했으면 생략(실패 하나 = WARN 한 줄, 경로마다). 날짜·파일 이름·문서 제목을 넣지 않는다. 버린 이유는 DEBUG에 이유별 개수만(`Drop` 이름 + 새 이유 `YEARS`·`DUPLICATE`). 사건은 ID 해시 8자리만 |

### 세계 섹션 시간 예산

| 구간 | M3a3c 뒤 (지금) | 이 기능 뒤 |
|---|---|---|
| 위키백과 (`kowiki-day-deadline`) | 5초 | 5초 (그대로) |
| Wikidata (`wikidata-read-timeout` + 유예) | 5초 (+1초) | 그대로 |
| 미디어 (`media.lookup-deadline` + 유예) | 3초 (+1초) — 날짜 범주 1개 | **3초 (+1초)** — 날짜 범주와 사건 사진 경로가 같은 끝 시각을 나눠 씀 |
| 최악 합계 | 약 13초 (최대 15초), 스텁 측정 약 11.7초(M3a3b T3·M3a3c T3) | **같음**. 시험 상한은 설정값 합 + 여유 1초, 미디어 구간은 `lookup-deadline` + 1초 + 0.5초(T2 (6)) |

사건 사진 경로는 요청 2개가 줄지어 3초 안에 끝나야 한다. 늦으면 사건 사진만 비고 `partial: true`. 10초 목표와의 차이는 M6b.

## API / 데이터 변경

- `GET /api/story/world`: `events[].media` 에 M3a3 형식의 사진 레코드(0~1장)가 들어갈 수 있다(데모 3날짜는 모두 `[]` — 결정 6). `day_media` 에서 사건 사진과 같은 파일이 빠진다. 위키백과 문서 하나가 `UNAVAILABLE` 인 날은 `day_media` 가 `[]`(그런 날은 이미 `partial: true`). `partial` 에 사건 사진 실패가 더해진다. 새 에러 코드 없음. 응답 키·형은 그대로(웹 계약 변화 없음, `MediaItem` = `MediaRecords.STORED_KEYS` 26개 + `date_relation`).
- 설정 추가: `media.max-years-apart: 5`(0 이상, 음수면 시작 실패, `MediaLookupProperties`, `application.yml` 에 주석과 함께).
- DB 변경 없음. 데이터 파일 없음.

응답 예시 (합성 — 지어낸 문서·파일, 경계값 날짜 2000-02-29. 데모 날짜에는 이런 항목이 없다):

```json
{
  "date": "2000-02-29",
  "events": [
    {
      "id": "kowiki-2000-02-29-1a2b3c4d",
      "title": "시험 문서",
      "scope": "domestic",
      "tone": "normal",
      "media": [
        {
          "id": "commons-900001",
          "type": "photo",
          "display_mode": "image",
          "url": "https://upload.wikimedia.org/wikipedia/commons/0/00/Example_test_photo.jpg",
          "thumbnail_url": "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/Example_test_photo.jpg/640px-Example_test_photo.jpg",
          "source_name": "Wikimedia Commons",
          "license": "CC BY-SA 4.0",
          "license_status": "free",
          "captured_at": "1998",
          "date_precision": "year",
          "place_ref": "country:KR",
          "date_relation": { "kind": "years_apart", "years": -2 }
        }
      ]
    }
  ],
  "day_media": [],
  "notice": null,
  "partial": false
}
```

(사건·미디어의 나머지 키는 M3a2·M3a3 형식 그대로 — 값 없는 키는 `null` 로 있다.)

서버 안 계약 (이름은 예시, 머지 코드 이름은 보고서에 적는다):

| 부품 | 모양 |
|---|---|
| 마감 (T0) | `Deadline implements AutoCloseable`: `close()` = 끝나지 않은 작업을 모두 인터럽트, 기다리지 않음, 여러 번 불러도 됨. `Outcome` 에 `record Interrupted<T>()` — 기다리던 스레드가 인터럽트됨(플래그 다시 세움, 작업 취소). `Failed(cause)` 는 늘 작업이 던진 것(작업 자신의 `InterruptedException` 포함) |
| 공용 `error` 검사 (T0) | `wikimedia` 패키지의 작은 도우미(예: `ActionApiErrors.cause(JsonNode tree)` → `Optional<String>` = `"api error"` 또는 `"api error <code>"`, `code` 가 `[a-z0-9-]{1,64}` 일 때만 붙임. `isRateLimited(JsonNode tree)` 는 원래 `code` 로 판단) |
| 미디어 조회 (`MediaLookup` 에 기본 메서드, T1) | `record EventTarget(String eventId, String title, String placeRef)`, `record WorldMedia(Map<String, List<MediaItem>> eventMedia, List<MediaItem> dayMedia, boolean failed)`(키는 사건 ID, 값 0~1장). `default WorldMedia lookup(LocalDate date, List<EventTarget> targets, boolean tragicDay)` 의 기본 구현은 `dayMedia(date, tragicDay)` 만 부르고 사건 미디어는 비운다 → 기존 람다·`Recording` 이 고치지 않고 동작. `CommonsMediaLookup` 이 재정의. `dayMedia(d, t)` 는 `lookup(d, List.of(), t)` 의 `dayMedia`·`failed` 와 같다 |
| 세계 서비스 (T1) | 대상 = `normal` 사건 중 `title` 이 있는 것(응답 순서), `placeRef` 는 `scope` 로. `tragicDay` = 응답에 `tragic` 사건이 있음 **또는** 위키백과 문서 하나라도 `UNAVAILABLE`(결정 2 (b)). `media.lookup(date, targets, tragicDay)` → `Event.of(event, verdict, eventMedia.getOrDefault(id, List.of()))`, `partial` 에 `\|\| failed` |
| 응답 (T1) | `Event.of(KoWikiEvent, Verdict, List<MediaItem>)` |

## 테스트 방침

- **외부 네트워크를 부르지 않는다** — WireMock + M3a3c 녹화 fixture. 컨텍스트 테스트의 모든 `wikimedia.*-url` 은 WireMock을 가리킨다.
- 데모 날짜에는 붙는 사건 사진이 없으므로(결정 6), **붙는 경로는 합성 fixture로 시험한다**(지어낸 문서 제목·파일 이름, JPEG, 촬영 시점을 바꿔 가며). 합성 날짜는 공개 역사 사건 날짜·경계값(예: 2000-02-29). 실제 비극 사건 이름, 실제 사람의 생년월일을 쓰지 않는다. 데모 날짜에 사진을 붙이려고 다른 날짜를 녹화하지 않는다.
- 응답 미디어 전부를 `date_relation` 을 뺀 뒤 M3a3 스키마로 검증(`DataSchemas`).
- 시간 시험은 짧은 상한과 응답하지 않는 스텁. 상한은 상수 측정값이 아니라 **설정값과 `Deadline.CANCEL_GRACE` 에서 계산**한다(리뷰 권장 1).
- 로그 시험은 `OutputCaptureExtension` 으로 날짜·파일 이름·문서 제목·전체 사건 ID가 없음을 본다.

## 데모 입력 3개로 확인할 것

사건·`tone` 은 M3a2 결과, `day_media` 는 M3a3b 결과. 사건 사진 값은 M3a3c T1 녹화로 정해졌다: **데모 3날짜 모두 사건 사진 없음**(결정 6 — 다른 날짜를 찾지 않는다).

| 데모 입력 | 사건 (M3a2) | 확인 (T1) |
|---|---|---|
| 1988-09-17 + "서울" | 3개, 모두 `normal`/`curated`/`domestic`: ① `1988년 하계 올림픽`(`Q8470`) ② `KBS 1TV`(`Q777278`) ③ `KBS 2TV`(`Q624509`) | 녹화 `pageimages-1988-09-17.json`·`files-1988-09-17.json` 으로: PageImages 요청 **1회**에 세 제목. 파일 제목 묶음 요청 1회(`KBS_2_logo.svg` 하나) → 매퍼가 버림(SVG — `MIME`). 세 사건 모두 `media` = `[]`. `day_media` 는 M3a3b 결과 그대로(`commons-4930021`, `commons-4196553`, `commons-112453227`, `commons-4929997`, `commons-26546944` — 순서 같음). Commons 요청 2(날짜 범주 + 파일 묶음). `partial: false`. 응답 어디에도 `.svg` 주소가 없다 |
| 2002-06-22 + "광주 서구 풍암동" | 1개: `2002년 FIFA 월드컵`(`Q47735`), `normal`/`curated` | 녹화 `pageimages-2002-06-22.json` 으로: PageImages 요청 1회(제목 1개) → 대표 이미지 없음 → 파일 묶음 요청 0. 사건 `media` = `[]`. `day_media` 는 M3a3b 결과(`commons-129150318`) 그대로. Commons 요청 1. `partial: false` |
| 1995-03-15 + "서울대학교병원" | 0개, `notice.code` = `NO_EVENTS_FOUND` | PageImages 요청 0, Commons 요청 1(날짜 범주만). `partial: false` |
| (합성) `tragic` 섞인 날 | `tragic`(`default` 포함) 사건 하나 + `normal` 사건 하나 | `tragic` 사건 `media` = `[]`, PageImages 요청에 그 제목 없음, `normal` 사건은 사진 후보. `day_media` = `[]`, 날짜 범주 요청 0 |
| (합성) 사진이 붙는 날 | `normal` 사건 하나, 합성 PageImages가 JPEG 파일 하나를 줌 | 그 사건 `media` 1장, `display_mode: image`, 스키마 통과, `place_ref` 가 사건 `scope` 위치(GPS 없을 때), 같은 파일이 날짜 범주에도 있으면 `day_media` 에서 빠짐 |

## 크기 (400줄 규칙)

제품 코드·설정(S1, `added`) 기준.

### 실측 비율 (M3a3c, PR #20)

| 작업 | M3a3c 다시 셈 (파일별 셈 ×1.11) | 실제 S1 | 다시 셈 대비 | 파일별 셈 대비 (÷1.11) |
|---|---|---|---|---|
| M3a3c T0 | 약 120 | 197 | ×1.64 | 약 ×1.8 |
| M3a3c T1 | 약 160 | 236 | ×1.48 | 약 ×1.6 |
| 합계 | 약 280 | **433 / −109** | ×1.55 | **약 ×1.7** |

이전판은 파일별 셈 150에 ×1.11과 ×1.5를 함께 곱해 250으로 잡았다. 이번에는 파일을 다시 세고 **파일별 셈 대비 실측 ×1.7**(범위 ×1.5~×1.9)을 바로 곱한다.

### 다시 셈

| 작업 | 파일별 셈 | 셈 | ×1.7 | 범위 (×1.5~×1.9) |
|---|---|---|---|---|
| T0 | `Deadline`: `AutoCloseable`·`close()`(약 8), `Interrupted` 와 `await` 고침(약 6) · `SparqlWikidataSignalSource`·`CommonsMediaLookup` 의 `switch` (약 6) · 새 공용 `error` 검사 클래스(약 30) · `ActionApiCommonsClient`: 공용 검사로 바꿈 + 파일 묶음 `query` 검사(약 8) · `ActionApiKoWikiPageImages`: 공용 검사로 바꿈(약 6) | 약 64 | **약 110** | 95~120 |
| T1 | `MediaLookup` 기본 메서드·레코드 2개(약 30) · `CommonsMediaLookup` 두 경로·try-with-resources·결과 처리(약 35), 사건 사진 고르기(PageImages → 파일 묶음 → 매퍼 → 연도 → 중복, 약 35), 자르기 위치·빼기·경고(약 15), 생성자(약 5) · `MediaLookupProperties` `maxYearsApart`·2인자 생성자·검사(약 12) · `WorldStoryService` 대상·결정 2 (b)·`lookup`·`partial`(약 25) · `Event.of` (약 4) · `application.yml` (약 4) | 약 170 | **약 290** | 255~325 |
| T2 | 테스트만 (S1 0) | 0 | 0 | — |
| **합계** | | 약 234 | **약 400** | **350~445** |

- 가운데가 400이다. T0은 따로 내도 되는 단위(연결 없이 고침 + 시험)라 **T0 뒤에서 실제 비율을 보고 나눌 수 있다** — PM 결정 1 **(a)**(2026-10-17).
- **확인점 1 (T0 뒤, 멈춤)**: 누적 S1이 **130을 넘으면** T1 전에 멈추고 묻는다(그 비율이면 합계 약 470). 선택지: T0만 이 PR로 내고 T1·T2를 새 브랜치로 / 그대로 계속. 130 이하면 알리기만 하고 계속.
- **확인점 2 (T1 뒤, 알림)**: 누적 S1이 **420을 넘으면** 오케스트레이터가 PM에게 알리고 계속한다(T2는 테스트만이라 S1이 늘지 않는다).
- 테스트(S2)는 따로 보고한다. M3a3c 실측으로 보면 테스트가 제품 코드보다 크다.

## 작업 목록

순서: T0 → **확인점 1 (130 멈춤)** → T1 → **확인점 2 (420 알림)** → T2 → 리뷰 → PR → T3(PR 검수 때) → 머지 → M3b2 T6(`main` 에서).

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | 연결 전 정리: PR #20 리뷰 권장 2·3·4·5 (`fix(wikimedia)`·`fix(media)` 커밋) | backend-dev | M3a3c 머지 (완료) | (1) **권장 2**: `Deadline` 이 `AutoCloseable` 이다. `DeadlineTest` 에: 시작하고 기다리지 않은 작업이 `close()` 뒤 200ms 안에 인터럽트되어 끝난다. 모든 작업이 끝난 마감의 `close()` 는 아무것도 바꾸지 않고(끝난 작업의 `await()` 는 그대로 `Done`), `close()` 는 막히지 않으며(50ms 안에 돌아옴) 두 번 불러도 예외가 없다. 기존 `DeadlineTest` 단언은 그대로 통과. (2) **권장 3**: `Outcome` 에 `Interrupted` 가 있다. 기다리던 스레드가 인터럽트되면 `Interrupted` 이고 그 스레드의 플래그가 다시 서 있고 작업이 인터럽트된다(기존 시험의 기대값을 `Failed(InterruptedException)` 에서 `Interrupted` 로 바꾸는 것만 허용 — 바꾼 줄을 보고). 작업이 **스스로** `InterruptedException` 을 던지면(취소 없이) `Failed` 이고 기다린 스레드의 플래그는 서 있지 않다. `CommonsMediaLookupTest`: 날짜 범주 작업이 스스로 `InterruptedException` 을 던지면 `DayMedia.FAILED` 이고 WARN 한 줄 `source=commons title=commons day category cause=InterruptedException`. `SparqlWikidataSignalSource` 도 같은 규칙(`Interrupted` 는 WARN 없음, `Failed` 는 WARN 한 줄). (3) **권장 4**: `ActionApiCommonsClient.files` 가 `{"batchcomplete":true}` 와 `{"batchcomplete":true,"query":{}}` 에 `Files.failed() == true`, WARN 한 줄 `source=commons title=commons files of 1 cause=no query`. `{"batchcomplete":true,"query":{"pages":[]}}` 는 실패가 아니다(기존 시험 그대로). `dayCategory` 의 `{"batchcomplete":true}` 는 여전히 빈 범주이고 실패가 아니다(`ActionApiCommonsClientErrorTest.answerWithoutAnErrorKeyIsStillASuccess`·`CommonsMediaLookupTest` 그대로 통과). PageImages도 `query.pages` 가 배열이 아니면 실패(`{"query":{}}` → 빈 `Optional`, WARN `cause=no query`). (4) **권장 5**: `error` 검사가 `wikimedia` 패키지의 한 곳에만 있고 두 미디어 어댑터가 그것을 쓴다(`ActionApiCommonsClient`·`ActionApiKoWikiPageImages` 안에 `tree.has("error")` 블록이 없다). `code` 가 `badvalue` → `cause=api error badvalue`(기존 시험 그대로), `code` 가 없거나 `"Bad Code"`·`"x\ny"`·`"internal_api_error_DBQueryError"`·65자 → `cause=api error` 뿐이고 로그에 그 값이 없다. `ratelimited` 는 두 어댑터 모두 여전히 호스트를 막는다(다음 호출이 요청 0). (5) 동작 변화는 (1)~(4)뿐: 기존 테스트 단언은 (2)에 적은 것 말고 고치지 않는다. (6) `cd server && ./gradlew check` 성공. 보고서에 누적 S1 — **확인점 1 (130 멈춤)** |
| T1 | ★ 세계 API 연결: 사건 사진, ±5년, 중복, 같은 끝 시각, 문서 실패 날, `partial` (이전 M3a3c T2) | backend-dev (★ PM 결정 2026-10-16) | T0 (확인점 1 통과) | (1) "데모" 표 5행 통과(데모 값은 M3a3c 녹화로 고정 — 세 날짜 모두 사건 사진 없음). (2) 데모 3날짜와 합성 "사진이 붙는 날" 응답의 모든 미디어가 스키마 통과(`date_relation` 뺀 뒤). (3) 합성(요청 2000-02-29): 촬영 연도 1994(−6)·2006(+6) → 안 붙음, 1995(−5)·2005(+5) → 붙음, 촬영 시점 미상 → 붙고 `date_relation.kind` = `unknown`. `media.max-years-apart: 0` → 같은 해(`years` 0)와 `unknown` 만. (4) `tragic`: `TRAGIC` 사건(`default` 포함) → `media` = `[]` 이고 PageImages 요청 제목에 없음. 다른 `normal` 사건은 후보. (5) `hidden`: `hide` 로 내린 사건의 제목은 PageImages 요청에 없다. (6) 같은 파일이 두 사건에 나오면 앞 사건에만. 사건 사진과 같은 `id` 는 `day_media` 에 없다. 합성 날짜 범주 7장(모두 통과, 그중 하나가 사건 사진) → `day_media` 6장이고 사건 사진 `id` 없음(빼고 나서 자름). 사건 사진 경로가 실패하면 `day_media` 는 M3a3b와 같다. `day-media-limit: 0` 이어도 사건 사진은 붙는다. (7) PageImages 503 → `events[].media` 모두 `[]`, `day_media` 는 채워짐, `partial: true`. 파일 제목 묶음 503 → 같은 결과. PageImages `{"error":{"info":"x"}}` → 같은 결과. 파일 묶음 `{"batchcomplete":true}` → 같은 결과(T0 (3)). (8) 요청 수(API 호출 1회당): PageImages ≤ 1, Commons ≤ 2. 녹화 fixture로 1988-09-17은 PageImages 1·Commons 2, 2002-06-22는 PageImages 1·Commons 1, 1995-03-15는 PageImages 0·Commons 1. 대상 사건이 없으면 PageImages 0. PageImages가 파일을 주지 않으면 파일 제목 묶음 0. (9) **같은 끝 시각**: 응답하지 않는 PageImages 스텁 + 정상 날짜 범주 → `lookup-deadline` + `CANCEL_GRACE` + 여유 1초 안에 200, `partial: true`, `day_media` 는 남는다. 응답하지 않는 날짜 범주 + 정상 사건 사진 경로(합성 JPEG) → 사건 사진은 남고 `partial: true`. 날짜 범주와 PageImages가 **둘 다** 응답하지 않음 → 여전히 `lookup-deadline` + `CANCEL_GRACE` + 여유 1초 안(창이 두 개가 아님), 경로마다 WARN 정확히 1줄(`commons day category`, `event photos of <n>` 또는 어댑터 이름표). 두 경로는 한 `Deadline` 에서 시작하고 try-with-resources로 닫힌다(리뷰 확인 항목). (10) `media.max-years-apart`: `MediaLookupProperties` 에 있고 음수면 시작 실패(오류 메시지에 `media.max-years-apart`), 2인자 생성자가 남아 `MediaLookupPropertiesTest`·`CommonsMediaLookupTest` 의 단언이 그대로 통과. (11) `MediaLookup` 의 추상 메서드는 `dayMedia` 하나로 남고, 테스트의 `(date, tragicDay) -> DayMedia.NONE` 람다들과 `WorldStoryServiceMediaTest.Recording` 이 고치지 않고 컴파일·통과한다(기본 `lookup` 이 `dayMedia` 를 부름). (12) 기존 `WorldStory*Test`·M3a3b·M3a3c 테스트의 **단언은 고치지 않는다**. 예외는 이것뿐이고 바꾼 줄을 보고서에 모두 적는다: (a) 준비 코드 — `CommonsMediaLookup` 생성자에 `KoWikiPageImages`(람다), PageImages 스텁 추가. (b) `WorldStoryDemoTest` 의 `wikipediaRequests()`·`pagepropsRequests()` 가 `prop=pageimages` 요청을 세지 않게 대상을 한정(단언 값 7·0 등은 그대로). (c) `WorldStoryEventPhotoPartsRegressionTest` 는 "아직 연결 안 됨"(M3a3c T3)을 지키던 시험이라 전제가 끝난다: 클래스 설명, `pageImagesRequests()).isZero()` 를 1988·2002는 `isEqualTo(1)`, 1995는 그대로 0, 최악 시간 시험은 `isEqualTo(1)` 로. Commons 요청 1·`day_media` 목록·`partial`·로그 단언은 그대로(기본 스텁은 대표 이미지가 없어 파일 묶음 0). 최악 시간 상한(`WORST_BOUND`)은 T1이 고치지 않는다(T2 (6)). (13) `Event.of` 가 미디어 목록을 받고, 응답 키·형은 그대로. MockMvc로 합성 "사진이 붙는 날"을 부르면 사건 `media` 항목이 `MediaRecords.STORED_KEYS` 26개 + `date_relation` 이고 값 없는 키가 `null` 로 있다(M3a3b T2 (9)와 같은 시험을 사건 미디어에). (14) **결정 2 (b)**: 위키백과 날짜 문서 또는 연도 문서 하나가 `UNAVAILABLE`(다른 하나는 내용 있음, 경우마다 시험) → `day_media` = `[]`, 날짜 범주 요청 0, 사건 사진은 그대로 찾음(PageImages 요청 1), `partial: true`. 문서가 `STALE` 또는 `ABSENT` 이면 그날 사진을 찾는다. (15) 응답 어디에도 `display_mode: link_only` 가 없다. 로그(DEBUG 포함)에 날짜·파일 이름·문서 제목·전체 사건 ID가 없다(ID 해시 8자리만). (16) `cd server && ./gradlew check` 성공. 보고서에 누적 S1 — **확인점 2 (420 알림)** |
| T2 | 완료 기준 검증과 전체 테스트 + PR #20 리뷰 권장 1 + 메모 2 | test-engineer | T1 | (1) T0·T1 기준 표(기준마다 테스트 이름). 빠진 기준은 테스트를 더한다(제품 코드는 고치지 않음). (2) `cd server && ./gradlew check` 통과, 3회 반복 안정. (3) S1·S2·S3(0이 정상) 보고. (4) 데모 3날짜 미디어 표(자리 `events[n]`/`day_media`, 사건, 순서, `id`, `display_mode`, `license`, `license_status`, `captured_at`, `date_relation`, `place_ref`) — 사건 자리는 모두 비어 있음을 표에 적는다. (5) 최종 서버 미디어 DTO 필드 목록이 M3a3b T3 (5)와 같은지(웹 `web/src/world/types.ts` 와 대조). (6) **권장 1**: `WorldStoryEventPhotoPartsRegressionTest` 의 상수 `WORST_BOUND`(11.7 + 0.5초)를 설정값 상한으로 바꾼다 — `kowiki-day-deadline` + `wikidata-read-timeout` + `CANCEL_GRACE` + `lookup-deadline` + `CANCEL_GRACE` + 여유 1초(`WorldStoryDayMediaAcceptanceTest` 와 같은 식), 아래 한계 `wikidata-read-timeout` + `lookup-deadline` 이상. 같은 시험에 PageImages도 상한까지 늦는 스텁을 더한다(위키백과·Wikidata·Commons 날짜 범주·PageImages 모두 늦음). 미디어 구간만 따로 재는 시험(두 경로 모두 응답하지 않음)이 `lookup-deadline` + `CANCEL_GRACE` + 0.5초 이하 — "창이 두 개가 아님"을 이것으로 지킨다. 잰 값을 출력하고 보고서에 M3a3b·M3a3c 측정(약 11.7초)과 나란히 적는다(단언은 설정값 상한). (7) 컨텍스트 테스트에서 WireMock이 받지 않은 바깥 요청이 0(`findAllUnmatchedRequests` 빈 목록). (8) 메모 2: `ActionApiCommonsClientTest` 170행 근처의 낡은 주석 "two 200 ms attempts" 를 실제 동작(시도 1회)에 맞게 고친다 — 주석만, 단언 그대로 |
| T3 | 실제 호출 확인 (PR 검수 때) | PM | T2 | (1) `docker compose up -d db`, `cd server && ./gradlew bootRun` 이 뜬다(루트 `.env` 의 `WIKIMEDIA_USER_AGENT` 는 저장소 주소만, ASCII). (2) P1·P2: 1988-09-17과 2002-06-22의 모든 사건 `media` 가 `[]` 이다(결정 6 — 정상). 응답 어디에도 로고·`.svg` 주소·`link_only` 가 없고, `day_media` 는 M3a3b 때와 같은 사진, `partial: false`. (3) P3: 1995-03-15는 M3a3b 때와 같다. (4) 다른 날짜로 사건 사진을 찾아보지 않는다(결정 6). (5) 서버 로그에 날짜·문서 제목·파일 이름이 든 미디어 WARN이 없다. (6) 머지 뒤 **M3b2 T6**을 `main` 에서 한다 — 데모 날짜의 사건 카드에 사진 자리가 비어 있거나 없고 깨진 이미지가 없음을 확인 |

완료 기준에 쓰는 명령 (저장소 루트):

```
# S1 — 제품 코드·설정 줄 수
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
# S3 — 데이터 파일 줄 수 (0이 정상)
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P1~P3 — PM 실제 호출 (서버 실행 중, 키 없음 — WIKIMEDIA_USER_AGENT 는 루트 .env)
curl -s 'http://localhost:8080/api/story/world?date=1988-09-17' \
  | jq '{partial, events: [.events[] | {title, tone, media}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' \
  | jq '{partial, events: [.events[] | {title, media}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=1995-03-15' | jq '{events, day_media, notice, partial}'
```

담당별: backend-dev 2 (T0, T1), test-engineer 1 (T2), PM 1 (T3). 모두 4개.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 시점 |
|---|---|---|
| **M3b2 T6** (PM, 웹 사건 사진 확인) | 실제 `events[].media`(데모 날짜는 빈 목록) | 이 PR 머지 뒤 `main` 에서 |
| M3a4 영상 검색 틀 | `MediaLookup.lookup` 의 사건별 미디어 목록(영상이 더해짐), 같은 `Deadline`(try-with-resources, 세 번째 경로), `Interrupted`/`Failed` 구분 | 기본은 이 PR 머지 뒤. 병렬이면 worktree를 나누고 오케스트레이터가 합친다(리스크 5) |

## 리스크와 확인할 것

1. **사건 대표 이미지가 사건과 어긋날 수 있다** — 원문 그대로 나가는 줄(M3-P16)의 대표 이미지가 붙을 수 있다. `date_relation` 으로 정직하게 보이고 ±5년을 넘으면 붙이지 않는다. 촬영 시점 미상 이미지는 연도 거르기가 안 되므로 "촬영 시점 미상"으로만 보인다. 너무 엉뚱하면 사건 미디어에서 `unknown` 을 빼는 규칙을 후속으로 정할 수 있다.
2. **보수적 `tragic` 이 사진을 지운다** — Wikidata가 실패하거나 QID가 없는 날에는 사건 사진이 없어진다. 의도된 방향(6.3절).
3. **JPEG만** — PNG 사진도 빠진다.
4. **요청마다 PageImages·Commons를 부른다** — 캐시가 없다. 로컬 데모라 제한에 닿지 않는다. 필요하면 M3a4b 결과 캐시에 사진 출처를 더한다(후속).
5. **같은 연결 자리** — M3a4가 병렬로 진행되면 둘 다 `CommonsMediaLookup`·`MediaLookup`·`WorldStoryService`·`Event.of` 를 고친다. 기본은 M3a3d → M3a4.
6. **세계 섹션 시간** — 최악 약 13초(최대 15초), 스텁 약 11.7초로 10초 목표를 넘을 수 있다. 이 기능은 사건 사진 경로를 같은 미디어 끝 시각 안에서 동시에 돌려 늘리지 않는다(T1 (9), T2 (6)). 10초 맞추기는 M6b. 공용 `Semaphore(3)` 중 미디어는 최대 2자리.
7. **위키백과 스텁과의 충돌** — `WorldStoryDemoTest` 등은 `action=query` 만으로 pageprops를 스텁하고 위키백과 요청 수를 센다. PageImages 요청은 M3a3c의 우선순위 1 기본 스텁이 받지만, 연결이 생기면 위키백과 요청 수가 1 늘어난다 → 세는 대상에서 `prop=pageimages` 를 뺀다(T1 (12)(b), 단언 값은 그대로).
8. **최악 시간 상한이 느슨해진다** (리뷰 권장 1) — 설정값 상한(지금 값으로 16초)은 측정값 상한(12.2초)보다 넓어 "M3a3b보다 늘지 않음"을 직접 단언하지 않는다. 대신 미디어 구간 상한(`lookup-deadline` + 1초 + 0.5초)과 아래 한계로 지키고, 잰 값을 보고서에 늘 적는다(T2 (6)). CI 기계가 느려도 깨지지 않는 쪽을 골랐다.
9. **알려진 한계** — 제외 목록의 `EPA`·`Getty` 안전 쪽 오탐(M3a3b 리스크 9). 사건 사진에서 보이면 T2 표에 적는다.
10. **위키백과 문서 하나가 실패한 날** — 빠진 문서의 비극 사건이 `tragic` 날 판단에 들어가지 않는 문제는 결정 2 (b)로 막는다(T1 (14)). 사건 사진은 같은 구멍이 없다 — 사진은 화면에 나온 `normal` 사건 자기에게만 붙는다.
11. **위키 로컬 자유 파일** — 한국어 위키백과에 로컬로 올린 자유 파일은 Commons에 없어 `MISSING` 으로 빠진다(작은 손실, M3-P7과 같은 방향).
12. **재시도 없음** (M3a3c 결정 1 (a)) — 순간적인 502/503/504에 바로 `partial: true`. 자주 보이면 후속으로 다시 본다.
13. **Commons 파일 묶음에 `redirects=1` 이 없다** (M3a3c 메모 3) — 이름이 바뀐 Commons 파일은 `MISSING` 으로 빠진다. 사건 사진이 이 때문에 빠지는 일이 실제로 보이면 후속으로 더한다(로드맵 후속 후보).
14. **데모 날짜에는 붙는 사건 사진이 없다** (결정 6) — PM 실제 확인에서 사진이 붙는 경로가 눈에 보이지 않는다. 대응: 붙는 경로·연도·중복·`day_media` 빼기를 합성 fixture로 시험(T1 (2)(3)(6)(13), "데모" 표 합성 행). 다른 날짜로 찾아 보이게 하지 않는다.
15. **`Deadline` 의 취소 범위** — 끝 시각이 지나 한 작업의 기다림이 시간 초과되면 같은 마감의 끝나지 않은 작업이 모두 인터럽트된다. 이미 끝난 작업은 취소 표시를 받지 않아 결과가 남는다(머지 코드 확인) → T1 (9)의 "하나만 늦을 때 다른 쪽 결과가 남는다" 시험 두 방향으로 지킨다. `close()`(T0)는 기다리지 않은 작업만 정리하고 끝난 결과를 바꾸지 않는다.
16. **로그 코드 제한의 부작용** (리뷰 권장 5) — 밑줄·대문자가 든 MediaWiki 오류 코드(예: `internal_api_error_…`)는 `api error` 로만 찍힌다. 원인 찾기가 조금 어려워지지만 원격 값이 로그를 꾸미지 못한다. 필요하면 허용 문자에 `_` 를 더하는 것은 작은 후속.
17. **로그로 날짜를 짐작할 수 있음** — M3a1 위키백과 호출의 WARN 줄은 문서 제목을 찍는다. M2에서 다시 본다(로드맵 후속 후보). 이 기능의 새 로그는 날짜·제목이 없다.
18. **크기** — 약 400(350~445). T0 뒤 130에서 멈추고 나눌지 묻는다(PM 결정 1 (a)).

## PM 결정 (2026-10-17, 기록)

| # | 물은 것 | 결정 | 이 계획서에 반영 |
|---|---|---|---|
| 1 | 크기 약 400(350~445) — 400 규칙 경계. 리뷰 권장 2~5(T0, 약 110)를 더해 이전판(약 250)보다 커졌다 | **(a)** 한 PR. 확인점 1: T0 뒤 누적 S1 130 넘으면 T1 전에 멈추고 T0만 내고 나눌지 물음. 확인점 2: T1 뒤 420 넘으면 알림. planner 추천과 같음. 고르지 않은 것: (b) T0을 별도 PR로 지금 나눔(M3 PR 22개), (c) 리뷰 권장 2·3·5를 M3a4로 미룸 | 머리말 상태, 전제 표, 크기 절, 작업 순서, 리스크 18 |

★ T1(PM 직접 구현 후보)은 PM이 2026-10-16에 backend-dev로 정했다 — 다시 묻지 않는다. 배우고 싶으면 PR 리뷰 때 `CommonsMediaLookup.lookup` 의 두 경로·try-with-resources 부분을 함께 읽기를 권한다.

## 다음 단계 제안 (이 PR 밖)

- 위키백과 사건 어댑터(`ActionApiKoWikiClient`·`ActionApiKoWikiLinkResolver`)도 T0의 공용 `error` 검사로 옮기기 — 작은 `chore/`, 또는 M2 계획 때 Wikimedia 로그 제목 정리(후속 후보)와 함께.
- `WikimediaHttpClient.acquirePermit` 는 요청별 읽기 타임아웃이 아니라 전역 `read-timeout`(4s)만큼 자리를 기다린다. 미디어 경로는 마감 취소로 끊겨 문제가 없지만, M6b 시간 측정 때 함께 본다.
