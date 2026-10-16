# 사건 사진 부품: 시간 상한 공용화, PageImages, Commons 파일 묶음 (M3a3c)

> 브랜치: `feat/event-photos` (`main` ce3760c에서 갈라짐) · 작성: planner · 상태: **PM 승인 (2026-10-15, 개정) · 분할 (2026-10-16, 결정 5·6)** — 결정 1 **(a)** 미디어 호출 재시도 없음, 2 **(b)** 위키백과 문서 하나가 `UNAVAILABLE` 이면 그날 사진을 찾지 않음, 3 **(a)** 한 PR + 확인점 두 개, 4 **(b)** T0은 backend-dev, **5 분할**: 이 PR은 T0·T1 + 검증(T3) + PM 확인(T4)만 내고 T2(사건 사진 연결)는 **M3a3d `feat/event-photo-wiring`** (`docs/plans/m3a3d-event-photo-wiring.md`)로, **6** 데모 날짜에 맞는 사건 사진이 없으면 사진 없이 낸다(다른 날짜를 찾지 않음). 처음 승인 2026-10-14(M3a3b에서 분할).
> 기준: PM 미디어 결정 2026-10-11, 안전장치 1~6, `CLAUDE.md` 7절 1·2·3·5·6항, `docs/product-brief.md` 6.1절·6.2절·6.3절·8절·12절, `docs/research/day-events.md` 3·4절, `docs/plans/m3a3b-commons-photos.md`(Commons 파일 매퍼·사진 규칙·시간 상한), `docs/plans/m3a3-event-media.md`(형식·R1~R9·`date_relation`), M3-P7, #22 (c)
> 선행: **M3a3b 머지됨 (PR #17, `main` ce3760c)**.
> 진행: T0·T1 **완료** (backend-dev, 249f201·57cc5c2·6c220bd·4058edb, S1 433 = T0 197 + T1 236, `./gradlew check` 1359/0). 다음 T3(test-engineer 검증) → 리뷰 → PR → T4(PM).
> 뒤따르는 기능: **M3a3d** `feat/event-photo-wiring`(사건 사진을 세계 API에 연결, 이 PR 머지 뒤 `main` 에서), 그 뒤 M3b2 T6(PM 웹 확인). M3a4는 같은 미디어 연결 자리를 고친다.

## PM 결정

| # | 질문 | 결정 | 반영 |
|---|---|---|---|
| 1 | 이월 1: 미디어 경로의 재시도 | **(a)** 미디어 호출(날짜 범주·PageImages·파일 제목 묶음)은 재시도 없이 1회, 읽기 타임아웃은 고정 `media.lookup-deadline`. 고르지 않은 것: (b) 남은 시간을 읽기 타임아웃으로, (c) 그대로 (2026-10-15) | T0 (3), T1 (3) — 구현됨(`WikimediaHttpClient.getOnce`) |
| 2 | 이월 4: 위키백과 문서 하나가 `UNAVAILABLE` 인 날의 그날 사진 | **(b)** 찾지 않는다 — `tragic` 날처럼 취급. 사건 사진은 그대로 찾는다. 고르지 않은 것: (a) 리스크로만 (2026-10-15) | 세계 서비스 연결 작업이라 **M3a3d T1 (14)**로 옮김(결정 5) |
| 3 | 크기 약 445(370~530) | **(a)** 한 PR + 확인점 두 개(T0·T1 뒤 320 알림, T2 뒤 500 멈춤) (2026-10-15) | 확인점 1에서 433 > 320, T2 뒤 예상 약 600 → 결정 5 |
| 4 | ★ T0을 PM이 직접 구현할까 | **(b)** backend-dev (2026-10-15) | T0 |
| **5** | 확인점 1: T0·T1 뒤 누적 S1 433, T2 뒤 예상 약 600(> 500) | **지금 나눈다**: 이 PR(`feat/event-photos`)은 T0·T1만, test-engineer 검증(T3)과 리뷰 뒤 PR. T2(사건 사진 연결)와 그 시험·실제 확인은 새 기능 **M3a3d** `feat/event-photo-wiring` 으로. M3 PR 20 → **21** (2026-10-16) | 머리말, 목표, 범위 밖, 작업 목록, 크기, `docs/plans/m3a3d-event-photo-wiring.md`, 로드맵 |
| **6** | T1 녹화로 데모 날짜에 붙을 사건 사진이 없음(대표 이미지는 `KBS 2TV` 의 `KBS_2_logo.svg` 하나 — SVG이고 촬영 연도 차이 34년이라 거절) | **그날에 맞는 사진이 없으면 사진 없이 낸다. 다른 날짜를 찾지 않는다.** PM의 실제 확인은 데모 날짜에 사건 사진이 나오지 않고 이상한 것이 보이지 않음을 확인하는 것으로 충분하다 (2026-10-16) | M3a3d "데모" 표·T3, 로드맵 데모 표 |

## 이번 개정에서 바뀐 것

| 날짜 | 항목 | 내용 |
|---|---|---|
| 2026-10-15 | 머지된 코드에 맞춤 | "이어받는 계약"을 실제 이름으로 다시 썼다(M3a3b 머지 코드). |
| 2026-10-15 | 질의 언어 | Commons 질의에 `iiextmetadatalanguage` 를 **보내지 않는다**(기본 언어, PM 결정 2026-10-14). M3a3b 계획서의 같은 문구도 고쳤다(이월 3) |
| 2026-10-15 | 이월 리뷰 권장 | PR #17 리뷰 권장 4개 + 시간 메모 1개 → T0와 리스크 |
| 2026-10-15 | 크기 | M3a3b 실측(×1.21)으로 약 445 |
| **2026-10-16** | **분할 (결정 5)** | T2(세계 API 연결 — 사건 사진 고르기, ±5년, 중복, 두 경로 같은 끝 시각, 결정 2 (b), `partial`)와 T3·T4의 사건 사진 부분을 M3a3d로 옮겼다. 이 계획서의 "고르는 법"·"세계 섹션 시간 예산"·연결 계약·"데모" 표도 M3a3d로 옮겼다. 남은 T3·T4는 T0·T1 검증과 회귀 확인으로 다시 썼다 |
| **2026-10-16** | **녹화 결과 (결정 6)** | 한국어 위키백과도 `pilicense=free` 에 대표 이미지를 준다(리스크 10 해소). 데모 사건 4개 중 대표 이미지가 있는 것은 `KBS 2TV`(`KBS_2_logo.svg`)뿐이고, 붙지 않는다. 다른 날짜로 찾지 않는다 |

### 이월된 리뷰 권장 (PR #17)

| # | 내용 | 처리 |
|---|---|---|
| 1 | `CommonsMediaLookup` 이 `media.lookup-deadline` 전체를 호출별 읽기 타임아웃으로 넘겨, 타임아웃 뒤 재시도가 끝날 수 없고 취소 직전 쓸모없는 두 번째 요청을 보낼 수 있다. 시간 상한 코드가 `SparqlWikidataSignalSource.within` 과 겹친다 | **T0 완료**: 공용 `wikimedia.Deadline`, `WikimediaHttpClient.getOnce`(결정 1 (a)) |
| 2 | `ActionApiCommonsClient`: `error` 가 있는데 `code` 가 없는 응답이 성공으로 처리된다 | **T0 완료** (4). PageImages 어댑터도 같은 규칙(T1 (1)) |
| 3 | `docs/plans/m3a3b-commons-photos.md` 의 `iiextmetadatalanguage=ko` 문구 | planner가 고쳤다(2026-10-15) |
| 4 | 위키백과 문서 하나가 실패한 날의 그날 사진 | 결정 2 (b) → **M3a3d T1 (14)** |
| 5 | 세계 섹션 최악 시간(스텁 약 11.7초, 목표 10초 초과) | 이 PR은 시간을 바꾸지 않는다(연결이 없음, 재시도만 줄어듦 — T3 (6)). 사건 사진 경로를 같은 끝 시각 안에 두는 일은 **M3a3d** |

## 목표 (분할 뒤 이 PR)

1. PR #17 리뷰 권장 1·2를 닫는다: 공용 시간 상한 도우미(`Deadline`)로 Wikidata·Commons의 중복을 없애고, 미디어 호출은 재시도 없이 1회 보내며, Commons `error` 를 코드 유무와 상관없이 실패로 본다.
2. M3a3d가 쓸 부품을 만든다: 한국어 위키백과 PageImages 어댑터, Commons 파일 제목 묶음 질의, 녹화 fixture, WireMock 기본 스텁.
3. `GET /api/story/world` 응답은 M3a3b와 **같다**(사건 `media` 는 모두 `[]`, `day_media` 그대로). 바뀌는 것은 Commons 날짜 범주 요청이 실패 때 다시 시도하지 않는다는 것뿐이다.

## 범위 밖

- **사건 사진 연결 → M3a3d**: `events[].media` 채우기, `tragic`·`hidden` 사건 제외, ±5년(`media.max-years-apart`), 같은 파일 중복, `day_media` 에서 사건 사진 빼기, 두 경로를 같은 끝 시각에, 결정 2 (b) 그날 사진 차단, `MediaLookup` 기본 메서드, `Event.of` 미디어
- 다른 날짜의 사건 사진 찾기 — 하지 않는다(결정 6)
- 그날 사진 규칙, Commons 파일 매퍼 규칙 → M3a3b (예외: T0 고침)
- 영상 → M3a4~M3a5b, 사진 메타데이터 캐시 → 후속, 미디어 파일 복사·프록시(안전장치 1)
- 위키백과 날짜·연도 문서, pageprops, Wikidata 호출의 재시도 규칙 → 바꾸지 않는다
- 프론트엔드 → 변경 없음

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| 한국어 위키백과 PageImages | **확인됨** — [day-events.md 3절 Commons 카드·4절](../research/day-events.md) [48][64][65]. T1 녹화로 한국어 위키백과도 `pilicense=free` 에 대표 이미지를 주는 것을 확인(2026-10-16) | 사건 대표 링크 문서의 대표 이미지 파일 이름(`prop=pageimages&piprop=name&pilicense=free`, 제목 묶음 1회) | 비자유 이미지를 1차로 거른다(M3-P7). `wikimedia.kowiki-api-url` 재사용 |
| Wikimedia Commons (Action API) | **확인됨** — [day-events.md 3절 Commons 카드](../research/day-events.md#3-출처-카드) | 파일 제목 묶음(`titles=File:A\|File:B&prop=imageinfo&iiprop=url\|mime\|size\|extmetadata&iiurlwidth=640`, **언어 지정 없음**) | M3a3b와 같음: `WikimediaHttpClient` 로만, User-Agent·429·동시 3, 작가·라이선스·원문 페이지 표시, 썸네일은 `thumburl` 만, 핫링크만 |

키 없음. 새 HTTP 클라이언트를 만들지 않는다. User-Agent는 연락처로 **저장소 주소만** 쓴다 — 녹화 `curl` 은 정확히 `The-World-That-Day (https://github.com/ericmschoi/The-World-That-Day)`, 테스트 상수는 `WikimediaWireMockTest.TEST_USER_AGENT`. 이메일을 넣지 않는다.

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| 미디어 표시·촬영 시점 미상·안전장치 1~6 | M3a3 계획서와 같음 | 결정 2026-10-11 |
| #13-C | (a) Commons 주소 그대로(핫링크, `no-referrer`) | 결정 |
| M3-P7 | 비자유 위키 로컬 파일 제외 | 결정 |
| M3a3b 결정 1·2 | 1 (a) 두 PR, 2 (b) M3a3b T2는 backend-dev | 결정 2026-10-14 |
| Commons 질의 언어 | `iiextmetadatalanguage` 를 보내지 않음(기본 언어) | 결정 2026-10-14 |
| M3a3c 결정 1~6 | 위 "PM 결정" | 결정 2026-10-15 (1~4), 2026-10-16 (5·6) |

## 이어받는 계약 (머지된 코드, `main` ce3760c)

| 자리 | 지금 코드 (M3a3b) | 이 PR에서 |
|---|---|---|
| Commons 클라이언트 | `CommonsClient.dayCategory(LocalDate, Duration)` → `Pages(List<JsonNode>, boolean failed)`. `ActionApiCommonsClient.pages(label, UriComponentsBuilder, Duration)`(패키지 전용) — `.build().encode()`, `error.code` 가 있으면 실패 | 파일 제목 묶음 메서드, 템플릿 변수 인코딩, `error` 판정, 재시도 없음 |
| Commons 파일 매퍼 | `CommonsFileMapper.map(JsonNode, Predicate<String>, String, LocalDate)` → `Result(MediaItem item, Drop drop)` | 그대로 |
| 미디어 조회 | `MediaLookup.dayMedia(LocalDate, boolean)` → `DayMedia`, `CommonsMediaLookup` 이 시간 상한 코드를 직접 가짐 | 시간 상한을 공용 `Deadline` 으로 |
| HTTP | `WikimediaHttpClient.get(source, title, uri[, readTimeout])` — 타임아웃·502/503/504 1회 재시도, 읽기 타임아웃 값마다 `RestClient` 를 맵에 영구 보관 | 재시도 없는 `getOnce` 추가 |
| 시간 상한 | `SparqlWikidataSignalSource.within`·`ended`(private) | 공용 `Deadline` 으로 |
| 링크 해석 모양 | `ActionApiKoWikiLinkResolver`: `titles={titles}` 템플릿 + `.encode().buildAndExpand(…)`, 묶음 50, `normalized`·`redirects` | PageImages 어댑터가 같은 모양 |
| 테스트 기반 | `WikimediaWireMockTest`(`KOWIKI_PATH`, `SPARQL_PATH`, `COMMONS_PATH`, Commons 빈 범주 기본 스텁) | PageImages 기본 스텁(우선순위 1) |

## T0·T1이 만든 부품 (backend-dev 보고, M3a3d가 쓴다)

| 부품 | 실제 모양 |
|---|---|
| 공용 시간 상한 | `wikimedia.Deadline`: `static Deadline after(Duration budget)`, `<T> Task<T> start(String name, Callable<T> call)`(가상 스레드), `Task.await()` → `Outcome<T>` = `Done(T value)` / `TimedOut(boolean stopped, T late)` / `Failed(Throwable cause)`. `CANCEL_GRACE` = 1s. 끝 시각이 지나 어느 작업을 기다리다 시간 초과가 나면 **그 마감의 끝나지 않은 작업을 모두** 인터럽트한다. `SparqlWikidataSignalSource`·`CommonsMediaLookup` 이 쓴다 |
| 재시도 없는 호출 | `WikimediaHttpClient.getOnce(String source, String title, URI uri, Duration readTimeout)` — 시도 1회, 429·Retry-After 503 차단은 그대로 |
| PageImages 어댑터 | `media.pageimages.KoWikiPageImages.imageNames(List<String> titles, Duration readTimeout)` → `Optional<Map<String, String>>`(요청 제목 → 파일 이름, `File:` 없음, 이미지 없는 제목은 맵에 없음, 실패면 빈 값), `MAX_TITLES = 50`. 구현 `ActionApiKoWikiPageImages`(`getOnce`) |
| Commons 파일 묶음 | `CommonsClient.files(List<String> fileNames, Duration readTimeout)` → `Files(Map<String, JsonNode> byName, boolean failed)`, `MAX_FILES = 50`. `normalized` 를 따르고 Commons에 없는 파일은 `missing` 페이지로 돌려준다. `redirects=1` 은 없다(리스크 17) |
| 테스트 기반 | `WikimediaWireMockTest` 리셋에 PageImages 기본 스텁(`prop=pageimages`, `atPriority(1)`) |
| 녹화 fixture | `kowiki/pageimages-1988-09-17.json`(제목 3개)·`pageimages-2002-06-22.json`(제목 1개), `commons/files-1988-09-17.json`(`KBS_2_logo.svg`). 2002는 대표 이미지가 없어 파일 묶음 녹화 없음. 두 `README.md` 에 행 |

## API / 데이터 변경

- `GET /api/story/world`: **바뀌지 않음**(응답 키·값 모두 M3a3b와 같다). 내부적으로 Commons 날짜 범주 요청이 실패해도 다시 시도하지 않는다.
- 설정: `media.lookup-deadline` 주석이 "호출별 읽기 타임아웃, 재시도 없음"을 말한다. 새 설정 없음(`media.max-years-apart` 는 M3a3d).
- DB 변경 없음. 데이터 파일 없음(fixture는 테스트 리소스).

## 테스트 방침

- **외부 네트워크를 부르지 않는다** — WireMock + 녹화 fixture. 녹화 fixture는 바이트를 고치지 않고, README에 원 URL·녹화 일자·User-Agent(저장소 주소)·라이선스. 이미지 파일은 받지 않는다.
- 합성 fixture는 지어낸 파일 이름·문서 제목. 실제 비극 사건 이름, 실제 사람의 생년월일을 쓰지 않는다. 표본 날짜는 데모 3날짜와 공개 역사 사건 날짜·경계값.

## 크기 (400줄 규칙)

제품 코드·설정(S1, `added`) 기준.

| 작업 | 다시 셈 (2026-10-15) | 실제 S1 | 비율 |
|---|---|---|---|
| T0 | 약 120 | **197** | ×1.64 |
| T1 | 약 160 | **236** | ×1.48 |
| **이 PR 합계** | 약 280 | **433** | ×1.55 |
| (T2 → M3a3d) | 약 165 | — | M3a3d 계획서에서 이 비율로 다시 셈(약 250) |

- 433으로 400을 넘지만 T0·T1은 이미 끝났고 T3은 테스트만이라 S1이 더 늘지 않는다(결정 5로 이 PR 확정). T3 뒤 S1이 433에서 바뀌면 오케스트레이터가 이유와 함께 알린다.
- 테스트(S2)·fixture는 따로 보고한다.

## 작업 목록

순서: T0 ✔ → T1 ✔ → **확인점 1 (433 > 320, 결정 5로 분할)** → T3 → 리뷰 → PR → T4(PR 검수 때) → 머지 → M3a3d(`main` 에서). **이전 T2는 M3a3d T1로 옮겼다.**

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | ★ PR #17 이월 1·2: 공용 시간 상한 도우미, 미디어 경로 재시도 끔, Commons `error` 판정 — **완료** (S1 197) | backend-dev (결정 4 (b)) | - | (1) `wikimedia` 패키지에 공개 도우미 하나(이름 자유): 끝 시각을 한 번 정하고 여러 작업이 나눠 쓰며, 작업마다 가상 스레드, 남은 시간까지만 기다리고 넘으면 `cancel(true)` 뒤 최대 1초 유예, 결과가 끝남/시간 초과(유예 안에 멈췄는지)/예외로 구분된다. 단위 시험: 끝 시각 1초를 나눠 쓰는 두 작업 — 첫째는 100ms에 끝나고 둘째는 응답하지 않음 → 둘째 기다림이 시작부터 1초 + 유예 1초 + 여유 0.5초 안에 끝나고(작업마다 1초씩 더하지 않음) 첫째 값은 남는다. 인터럽트를 무시하는 작업 → 유예 뒤 "멈추지 않음"으로 돌아온다. (2) `SparqlWikidataSignalSource` 와 `CommonsMediaLookup` 이 그 도우미를 쓰고 두 클래스의 private `within`·`ended`·`CANCEL_GRACE` 가 없어진다. `SparqlWikidataSignalSourceTest`·`CommonsMediaLookupTest`·`WorldStoryDayMedia*Test` 를 **고치지 않고** 통과한다(WARN 줄 수·`cause=deadline` 규칙 그대로). (3) **결정 1 (a)**: `WikimediaHttpClient` 에 재시도 없는 호출(오버로드)이 있다 — 읽기 타임아웃보다 늦게 답하는 스텁 → WireMock 요청 정확히 1, `Retry-After` 없는 503 → 요청 1, 둘 다 WARN 1줄. 기존 오버로드는 그대로 재시도한다(`WikimediaHttpClient*Test` 고치지 않고 통과). 날짜 범주 호출이 그것을 쓰고 읽기 타임아웃은 고정값 `media.lookup-deadline` 이다: 컨텍스트 시험에서 Commons 스텁이 상한보다 늦으면 Commons 요청이 정확히 1(지금은 최대 2), 결과는 그대로 200 + `partial: true`. (4) `ActionApiCommonsClient`: `error` 키가 있으면 `code` 유무·형과 상관없이 실패 — 합성 `{"error":{"info":"x"}}`, `{"error":"x"}` → `Pages.failed() == true`, WARN `cause` 가 `api error`(코드가 있으면 `api error <code>`)이고 `info` 글자(`x`)가 로그에 없다. `ratelimited` → `blockHost` 그대로. `ActionApiCommonsClientTest` 고치지 않고 통과. (5) `application.yml` `media.lookup-deadline` 주석과 `MediaLookupProperties` Javadoc이 실제 동작(호출별 읽기 타임아웃, 재시도 없음)을 말한다. (6) `cd server && ./gradlew check` 성공. (7) 보고서에 S1 |
| T1 | PageImages 어댑터, Commons 파일 제목 묶음, 녹화 fixture — **완료** (S1 236, 누적 433) | backend-dev | T0 | (1) PageImages: `wikimedia.kowiki-api-url` 에 `action=query&prop=pageimages&piprop=name&pilicense=free&pilimit=50&redirects=1&titles=…&format=json&formatversion=2` 가 WireMock 요청에 보인다. 제목은 주소 템플릿 변수로 엄격히 인코딩 — 합성 제목 `A&B`·`C+D` 가 WireMock에서 그대로 읽힌다. 제목 51개 → 요청 1회에 앞 50개. `normalized`·`redirects` 를 따라 요청 제목 → 파일 이름(`File:` 없음). 대표 이미지가 없는 문서는 맵에 없음(실패 아님). `error` 키가 있으면(코드 유무 무관) 실패, `ratelimited` → `blockHost` + 실패, JSON 아님·`query` 없음 → 실패. 예외 없음. (2) Commons 파일 제목 묶음: `titles=File:A\|File:B&prop=imageinfo&iiprop=url\|mime\|size\|extmetadata&iiurlwidth=640&format=json&formatversion=2`(**`iiextmetadatalanguage` 없음** — 요청에 그 키가 없음을 단언), 최대 50개, 템플릿 변수 인코딩(합성 파일 이름 `A&B.jpg`·`C+D.jpg`). 결과는 요청 이름 → 페이지이고 `normalized`(밑줄 → 공백, 첫 글자 대문자)를 따른다. `missing` 페이지는 `CommonsFileMapper` 에서 `MISSING`. `error` 판정은 T0 (4)와 같다. `pages()` 를 바꾼 뒤 기존 날짜 범주 주소 시험이 **고치지 않고** 통과한다. (3) 두 요청 모두 `WikimediaHttpClient` 의 재시도 없는 호출(결정 1 (a)), `User-Agent` = `TEST_USER_AGENT`, 읽기 타임아웃은 부르는 쪽이 준 값(`media.lookup-deadline`). (4) WireMock 503 → WARN `title` 이 `kowiki pageimages of N`·`commons files of N` 이고, 로그 어디에도 `YYYY-MM-DD` 모양·`File:`·`Category:`·합성 문서 제목이 없다. (5) `WikimediaWireMockTest` 리셋에 PageImages 기본 스텁(`/w/api.php` + `prop=pageimages`, **우선순위 1**, 대표 이미지 없는 응답)을 더한다. 시험: `action=query` 만으로 맞추는 pageprops 스텁이 있어도 PageImages 요청은 기본 스텁이 받는다. 기존 컨텍스트 테스트가 고치지 않고 통과한다. (6) 녹화 fixture: `kowiki/pageimages-1988-09-17.json`(제목 3개 한 요청)·`pageimages-2002-06-22.json`, 돌려받은 파일의 `commons/files-<날짜>.json`, 두 `README.md` 에 행 추가(원 URL, 녹화 일자, User-Agent `The-World-That-Day (https://github.com/ericmschoi/The-World-That-Day)`, 라이선스, 언어 지정 없음). 이미지 파일은 받지 않는다. (7) 보고서에 데모 사건 4개 결과 표와 `url`·`thumburl` 호스트, 한국어 위키백과가 `pilicense=free` 에 대표 이미지를 주는지. (8) `./gradlew check` 성공. 보고서에 T0·T1 누적 S1 — **확인점 1 (320, 알림)** |
| T3 | T0·T1 완료 기준 검증, 회귀 확인, 전체 테스트 (분할 뒤 다시 씀) | test-engineer | T1 | (1) T0·T1 기준 표(기준마다 테스트 이름). 빠진 기준은 테스트를 더한다(제품 코드는 고치지 않음). (2) `cd server && ./gradlew check` 통과, 3회 반복 안정. (3) S1·S2·S3 보고 — S1은 433 그대로, S3은 0. (4) **회귀 없음**: 데모 3날짜 `GET /api/story/world` 응답에서 사건 `media` 가 모두 `[]`, `day_media` 의 `id` 목록과 순서가 M3a3b T3 표와 같고, `partial: false`. 세 날짜 모두 PageImages 요청 0(아직 연결 없음), Commons 요청은 날짜 범주 1(1995 포함). (5) 미디어 경로 재시도 없음: 컨텍스트 시험에서 Commons 날짜 범주 스텁이 `lookup-deadline` 보다 늦으면 Commons 요청이 정확히 1, 응답 200 + `partial: true`. (6) 스텁으로 잰 세계 섹션 최악 시간(위키백과·Wikidata·Commons 날짜 범주 모두 상한까지 늦을 때)이 M3a3b T3 측정값(약 11.7초) + 0.5초 이하. (7) 컨텍스트 테스트에서 WireMock이 받지 않은 바깥 요청이 0(모든 `*-url` 이 WireMock 주소). (8) 계획 허용 밖 테스트 변경을 보고서에 따로 적는다: `KoWikiFixtureIntegrityTest`(`pageimages-` fixture를 `pageprops-` 처럼 제외, README 검사 추가)의 바뀐 줄과 이유 — 리뷰가 판단한다 |
| T4 | 실제 호출 확인 — 회귀 없음 (PR 검수 때) | PM | T3 | (1) `docker compose up -d db`, `cd server && ./gradlew bootRun` 이 뜬다(루트 `.env` 의 `WIKIMEDIA_USER_AGENT` 는 저장소 주소만, ASCII). (2) P1~P3 결과가 M3a3b 때와 같다: 사건 `media` 는 모두 `[]`, `day_media` 는 M3a3b T4와 같은 사진, `partial: false`. (3) 이 PR에는 사건 사진 확인이 없다 — M3a3d T3에서 한다(데모 날짜는 사건 사진이 없는 것이 정상, 결정 6) |

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

담당별: backend-dev 2 (T0, T1 — 완료), test-engineer 1 (T3), PM 1 (T4). 모두 4개. 이전 T2는 M3a3d T1.

## 로드맵 반영

- 2026-10-15: 머리말 상태(M3a3b·M3b3 완료, M3a3c 개정 승인과 결정 1~4), "현재 위치", M3 절 머리 수, 기능 표 M3a3b·M3a3c 행, 비율 문단, 진행 순서 그림, 후속 후보 "섹션 전체 시간 상한" 행.
- 2026-10-16 (결정 5·6): M3a3c 행(범위 T0·T1, 실제 S1 433), 새 M3a3d 행, M3 PR 20 → 21, 현재 위치, 진행 순서 그림, M3b2 T6 시점(M3a3d 뒤), 데모 표 1988 행(사건 사진 없음), 비율 문단(M3a3c 실측).
- 아직 안 한 것(`feat/privacy-consent` 와 합친 뒤): M3a3c 결정 1~6 행을 M3 PM 결정 표·"M0 이후 확정 항목"·"결정 시점"에, 낡은 "M3a3b T2 뒤 확인점" 행 정리, 진행 순서 메모(132행 근처)의 "M3a3b → M3a3c → M3a4" 를 "→ M3a3d →" 로.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 시점 |
|---|---|---|
| **M3a3d** 사건 사진 연결 `feat/event-photo-wiring` | `Deadline`, `getOnce`, `KoWikiPageImages`, `CommonsClient.files`, PageImages 기본 스텁, 녹화 fixture | 이 PR 머지 뒤 `main` 에서 |
| M3b2 T6 (PM, 웹 사건 사진 확인) | — | M3a3d 머지 뒤 |
| M3a4 영상 검색 틀 | `Deadline`, `getOnce` | 기본은 M3a3d 뒤(같은 미디어 연결 자리) |
| M3a4b Commons 동영상 | `CommonsClient.files`(MIME 조건만 바꿔 매퍼) | M3a4 뒤 |

## 리스크와 확인할 것

이 PR에 남은 것만 적는다. 사건 사진 연결의 리스크(대표 이미지 어긋남, 보수적 `tragic`, JPEG만, 캐시 없음, 같은 연결 자리, 세계 섹션 시간, 문서 하나가 실패한 날, 크기)는 M3a3d 계획서로 옮겼다.

7. **위키백과 스텁과의 충돌 (확인됨)** — PageImages는 위키백과와 같은 `/w/api.php` + `action=query` 다. 대응: 우선순위 1 기본 스텁(T1 (5), 완료). 요청 수 단언 대상 한정은 연결이 생기는 M3a3d T1 (12).
8. **알려진 한계** — 제외 목록의 `EPA`·`Getty` 안전 쪽 오탐은 M3a3b 리스크 9와 같다.
10. **PageImages 근거 — 해소** — T1 녹화로 한국어 위키백과도 `pilicense=free` 대표 이미지를 준다. 한국어 위키백과에 로컬로 올린 자유 파일은 Commons에 없어 `MISSING` 으로 빠진다(작은 손실).
12. **재시도를 껐으므로**(결정 1 (a)) 미디어 호출이 순간적인 502/503/504에 다시 시도하지 않고 바로 `partial: true` 가 된다. 자주 보이면 "남은 시간(100ms 단위 올림)" 방식을 후속으로 다시 본다.
13. **제목 인코딩** — `&`·`+` 는 템플릿 변수로 엄격히 인코딩했다(T1 (1)(2)).
14. **로그로 날짜를 짐작할 수 있음** — M3a1 위키백과 호출의 WARN 줄은 지금도 문서 제목을 찍는다. M2에서 다시 본다(로드맵 후속 후보).
15. **계획 허용 밖 테스트 변경** — T1에서 `KoWikiFixtureIntegrityTest` 가 바뀌었다(`pageimages-` fixture를 `pageprops-` 처럼 제외, README 검사 추가). 계획은 기존 테스트를 고치지 않는다고 했다. T3 (8)이 바뀐 줄을 보고하고 리뷰가 받아들일지 정한다.
16. **낡은 시험 주석** — `ActionApiCommonsClientTest` 에 "two 200 ms attempts" 주석이 남아 있다(재시도가 없어진 뒤로 틀림). 동작 문제는 아니다. M3a3d T2에서 고친다(테스트만).
17. **파일 묶음에 `redirects=1` 이 없다** — Commons에서 이름이 바뀐(넘겨주기된) 파일은 새 이름을 따라가지 않고 `missing` 으로 돌아와 `MISSING` 으로 빠진다. PageImages 쪽은 위키백과 문서의 넘겨주기를 따르므로 문서 쪽은 문제없다. 알려진 한계로 두고, 사건 사진이 이 때문에 빠지는 일이 실제로 보이면 후속으로 `redirects=1` 을 더한다(M3a3d 리스크 12).

## 이전 결정 (그대로)

| # | 결정 | 상태 |
|---|---|---|
| M3a3b 결정 1 | (a) 두 PR — 이 기능이 사건 사진 | 결정 2026-10-14 |
| M3a3b 결정 2 | (b) M3a3b T2는 backend-dev | 결정 2026-10-14 |
| Commons 질의 언어 | 기본 언어(`iiextmetadatalanguage` 없음) | 결정 2026-10-14 |

새로 묻는 것 없음.
