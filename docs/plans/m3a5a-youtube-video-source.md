# YouTube 영상 출처 연결과 방침 문장 (M3a5a)

> 브랜치: `feat/youtube-video-source` (M3a5 `feat/official-channel-video` 가 머지된 `main` 에서 갈라짐) · 작성: planner (2026-10-26) · 상태: **PM 승인 대기** (크기 결정 1과 ★ 결정 2가 열려 있음 — M3a5a를 시작할 때 정해도 된다)
> 이력: M3a5 크기 확인점 1(T3 뒤 누적 S1 525 > 500)에서 PM이 **결정 1 (b) 두 PR로 나눔**(2026-10-26). M3a5 계획의 T4·T5·T6·T8을 이 계획으로 옮기고 완료 기준을 머지될 M3a5의 실제 이름에 맞췄다. 순서는 **M3a5 → M3a5a → M3a5b**. M3 PR은 23개다(크기 결정 1이 (b)면 24개).
> 설계 기준: [`docs/plans/m3a5-official-channel-video.md`](m3a5-official-channel-video.md) 의 "규칙"·"시간과 요청 제한"·"쿼터"·"캐시와 30일"·"테스트 방침"·"데모·표본 날짜" 절을 **이 계획의 일부로 읽는다**(옮기지 않고 가리킨다. 값이 다르면 아래 "M3a5 계획과 달라지는 것"이 이긴다). 그 밖의 기준: M3-P9 (b), M3-P14, 안전장치 1~6, `CLAUDE.md` 7절 1~7항, `docs/plans/m3a4-event-video.md`, `m3a4b-commons-video.md`, `m3a4c-media-search-cache.md`, `m3b3c-youtube-embed.md`(웹 계약), `docs/research/video-sources.md` 3.2절·3.5절·7절 R-Y1~R-Y7·R-N5·8절 위험 9~17·10.1절
> 선행: **M3a5 머지** — 설정·클라이언트·쿼터 부품이 `main` 에 있다. 출처 쪽 조건(YouTube 경로 "확인됨")은 6bf0267, 약관·쿼터 문서 다시 확인(위험 17)은 M3a5 T0c(b3f6be6)로 끝났다.
> **화면 노출**: M3b3b(동의·방침)와 M3b3c(YouTube 플레이어)가 이미 머지되어 있어서, 이 기능이 머지되면 API와 화면에 YouTube 영상이 함께 나온다. **M3a5 머지 직후에는 API·화면에 아무것도 달라지지 않는다**(출처를 흐름에 연결하지 않았다 — 의도된 것). 눈에 보이는 변화는 이 기능부터다.

## 목표

1. 머지된 검색 틀(`VideoSearch`)에 **YouTube 공식 채널 출처**(`EventVideoSource`, key `youtube`)를 붙인다. 사건마다 KTV 아카이브 한정 검색(M3a5의 `YoutubeClient`)을 부르고, 날짜의 후보 ID를 모아 `videos.list`(50개씩)로 확인한 뒤, 맞는 것만 `video_embed`(`provider: youtube`)로 낸다.
2. 쿼터(M3a5의 `YoutubeQuota`)를 실제 호출 앞에 건다. 넘으면 그날은 YouTube만 건너뛴다(`QUOTA_SKIPPED` — 사진·Commons 영상은 그대로, 오류 아님, 캐시 안 됨).
3. 결과 캐시에 `youtube` 줄(TTL 7일, M3a5에서 설정)을 쓰고, 29일 지난 줄을 지워 YouTube API 데이터를 30일 안에 지운다(III.E.4.d).
4. M3b3c에서 넘어온 마무리: 계약 예시의 빈 필드 채우기(T5 (3)), 방침 문장 현재형과 시행일(T6), 머지 뒤 PM의 M3b3c T6.
5. 세계 섹션 최악 시간을 늘리지 않는다(M3a5 계획 "시간과 요청 제한" 절 그대로).

## 범위 밖

- 설정·클라이언트·쿼터 부품 자체(M3a5에서 끝남), 허용 채널 밖 YouTube(M3-P14), 방송사 채널, KTV 국민방송, 채널 한정 없는 검색(Y-b), 업로드 목록 색인(Y-c)
- 대한뉴스 회차 대조·색인과 같은 꼭지 여러 ID(R-N7) → M3a5b. 컬러 복원 재업로드를 회차로 쓰기 → 후속 후보
- 쿼터 증설 신청(공개 배포 #5), 쿼터 계수기를 DB에 보존, YouTube 전용 요청 제한기
- 동의 단계·방침 내용(M3b3b)과 플레이어(M3b3c). 방침은 문장 하나와 시행일만 고친다(T6)
- 영상 내려받기·서버 경유 재생. **영상이 없는 날짜에 다른 날짜로 영상을 찾아 보이게 하는 것**(PM 피드백 2026-10-16 — 영상이 없는 날은 사진만 나오는 것이 정상)
- 영상 슬롯 정책·서버 전체 시간 상한(M6b)

## 사용할 외부 출처

M3a5 계획의 같은 이름 절과 같다. 이 기능에서 쓰는 부분만 적는다.

| 출처 | 확신도 | 이 기능에서 하는 일 | 지킬 조건 |
|---|---|---|---|
| YouTube Data API v3 `search.list`·`videos.list` ([video-sources.md](../research/video-sources.md) 3.2절 V2 카드, 3.5절) | **확인됨** (6bf0267, 약관·쿼터 문서 다시 확인 b3f6be6) | 호출은 M3a5의 `YoutubeClient` 가 한다. 이 기능은 그 결과를 규칙대로 거르고 레코드로 만든다 | 키는 헤더로만(M3a5), 메타데이터는 30일 안에 지우거나 새로 받음(III.E.4.d) — 이 기능의 T5 (5), 영상 저장 금지(III.E.1.a) |
| YouTube 임베드 플레이어 (V4) | **확인됨** | `embed_url` 형식 만들기(T4) | `autoplay`·`mute` 없이 `origin` 을 넣는다 [102][103]. 재생 규칙은 M3b3c(완료) |

- 출처 표시(III.F.2.a)·약관 링크(III.A.1)·방침(III.A.2) 화면은 M3b3b·M3b3c가 했다. 이 기능은 레코드에 `attribution` "<채널 이름> · YouTube"와 `license_url` YouTube 이용약관을 넣고, 방침 문장을 현재형으로 고친다(T6).

## 이어받는 부품 (M3a5 머지 이름)

| 부품 | 이름·모양 | 이 기능에서 |
|---|---|---|
| 설정 | `YoutubeProperties`(`youtube.*`: `api-key`·`allowed-channels`·`max-results`·`retrospective-title-patterns` 등), `config.AppProperties` 의 `app.web-origin`, `media.allowed-video-embed-providers` 의 `youtube`, `media.search-cache.ttl.youtube`(7일, 30일 상한) | 읽기만 한다 |
| 키 점검 | `YoutubeKeyCheck` — 키가 비면 시작 때 WARN 한 줄. 시험 설정이 그 로그 수준을 낮춰 둔다(M3a5 T1b) | 건드리지 않는다. 키 없음 판정은 `YoutubeProperties` 의 키 값으로 한다(T5 (2)) |
| 클라이언트 | `YoutubeClient`(구현 `YoutubeDataApiClient`) — 검색(채널·검색어)과 `videos`(ID 최대 50개) 호출. 결과는 `Ok` / `QuotaExceeded` / `Failed` 셋. 실패·쿼터는 클라이언트가 WARN을 한 줄 남긴다 | 부르고 결과를 상태로 옮긴다(T5) |
| 쿼터 | `YoutubeQuota` — 버킷 `SEARCH`·`UNITS`, `reserve(bucket, n)`(전부 아니면 없음, **n < 1 이면 예외**), `exhausted(bucket)`(소진 표시), 태평양 날짜 초기화 | 호출 전에 예약한다(T5 (1)) |
| 시험 기반 | 시험 `config/application.yml` 이 YouTube를 꺼 두고 주소를 `localhost:9` 로(M3a5 T1), `YoutubeClient` 합성 fixture `server/src/test/resources/fixtures/youtube/` + `README.md`(M3a5 T2) | fixture를 재사용·확장한다 |
| 검색 틀·후보·정확도·캐시 | M3a5 계획 "이어받는 계약" 표 그대로(`EventVideoSource`, `VideoSearch`, `Candidate`(10개 요소, `origin` 필수), `EventContext`, `VideoAccuracy`, `MediaFilter`, `MediaNormalizer`, `SearchCache`, `SearchCacheFormat`, `Deadline`) | 같다 |

> 위 이름 중 M3a5 계획서에 없던 것(`YoutubeProperties`·`YoutubeKeyCheck`·`YoutubeClient`·`YoutubeDataApiClient`·`Ok`·`QuotaExceeded`·`Failed`·`reserve`·`exhausted`)은 M3a5 머지 코드에서 확인한 값이다. backend-dev는 T4·T5 보고서 첫 줄에 "확인한 이름"을 적고, 다르면 이 계획서의 이름 쪽을 고치게 알린다.

## M3a5 계획과 달라지는 것

1. **`YoutubeQuota.reserve(bucket, n)` 은 n < 1 이면 예외를 던진다**(PM 결정, 2026-10-26 — 그대로 둔다). 그래서 T5는 **예약할 것이 없을 때 `reserve` 를 부르지 않는다**: 검색어가 있는 사건이 하나도 없을 때(검색 예약 0), 검색 결과에 영상 ID가 하나도 없을 때(`videos.list` 예약 0). 예약 0이 `QUOTA_SKIPPED` 로 새면 안 되고(예약이 "모자람"이 아니라 "없음"이다), 예외가 사건 하나의 문제를 날짜 전체의 실패로 번지게 해서도 안 된다.
2. **403 오류 본문의 `error.errors[].reason` 모양은 공식 문서에 적혀 있지 않다**(M3a5 T2 메모). 클라이언트는 본문이 읽히고 `reason` 이 `quotaExceeded` 일 때만 `QuotaExceeded` 로 가르고, **읽을 수 없는 403은 `Failed`** 다. 그래서 실제 응답 모양이 다르면 쿼터 소진이 `FAILED`(`partial` true, 캐시 안 됨, 소진 표시 없음)로 보일 수 있다 — 이때는 우리 쪽 상한(80회)이 호출 수를 막아 주고, PM이 T8 로그로 `cause=status 403` 이 반복되는지 본다(리스크 3).
3. 이름은 위 표의 머지된 이름을 쓴다(M3a5 계획서의 "T3 `reserve`·`exhausted`", "T2 `search`·`videos`" 와 같은 부품).

## API / 데이터 변경

- `GET /api/story/world`: 요청·응답 모양은 M3a3 그대로이고 `video_embed` 레코드가 새로 나온다. 설정·DB·`data/` 변경은 없다(M3a5에서 끝남, V3 `event_media_search_cache` 그대로. 지우기는 그 테이블의 DELETE). 웹은 `web/privacy.html` 문장 하나와 시행일, 그 시험(T6).
- 웹(M3b3c)과의 계약 예시(합성 값, 1986-09-20 표본 — M3b3c 웹 fixture `exampleYoutubeEmbed` 와 키가 같다). T5 (3)의 끝까지 시험이 같은 키를 단언한다:

```json
{
  "id": "youtube-Abc123xyz_0",
  "type": "video",
  "display_mode": "video_embed",
  "url": null,
  "thumbnail_url": "https://i.ytimg.com/vi/Abc123xyz_0/mqdefault.jpg",
  "thumbnail_width": 320,
  "thumbnail_height": 180,
  "video_sources": null,
  "duration_seconds": 117.0,
  "embed_url": "https://www.youtube-nocookie.com/embed/Abc123xyz_0?playsinline=1&rel=0&enablejsapi=1&origin=http%3A%2F%2Flocalhost%3A5173",
  "provider": "youtube",
  "render": null,
  "link_only_reason": null,
  "source_name": "KTV 아카이브",
  "source_url": "https://www.youtube.com/watch?v=Abc123xyz_0",
  "license": "provider-terms",
  "license_status": "other",
  "license_url": "https://www.youtube.com/t/terms",
  "attribution": "KTV 아카이브 · YouTube",
  "description": "시험용 개막식 영상 설명",
  "captured_at": "1986",
  "date_precision": "year",
  "lat": null,
  "lng": null,
  "place_ref": "country:KR",
  "retrieved_at": "2026-10-25T03:00:00Z",
  "date_relation": { "kind": "same_year", "years": 0 },
  "selection": { "basis": "youtube_official", "score": 60 }
}
```

  - `lat`·`lng` 는 늘 null(좌표를 쓰지 않는다). `place_ref` 는 사건의 `EventContext.placeRef()`(이 예는 국내 사건). `retrieved_at` 은 처음 조회 시각(UTC, 초)이고 캐시에서 되살려도 그대로다. 웹은 `place_ref`(또는 좌표)·`retrieved_at` 이 없으면 숨기므로 둘은 늘 있다.
  - `duration_seconds` 는 서버 `Double` 이라 `117.0` 으로 나간다(웹 타입 `number`). `selection` 은 고른 영상에만 있고 정수다(60 = 날짜 같은 해 10 + 사건어 30 + 공식 20).
  - 레코드 값·`origin`·임베드 주소·버림 규칙·촬영 시점 차례는 M3a5 계획 "규칙" 표가 기준이다.

## 작업 목록

순서: T0(PM, Y5) → T4 ★ → (확인점 1) → T5 → (확인점 2) → T7 → 리뷰 → PR → T8(PM) → T6(머지하는 날) → 머지 → PM M3b3c T6. T0은 T8 전에만 끝나면 된다.

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | 콘솔 사용량 확인 (M3a5 T0에서 남은 것) | PM | M3a5 머지 | (1) **Y5(필수, T8 전)**: 콘솔 할당량 페이지에서 검색 버킷 100회와 일반 10,000단위가 보이고, 사용량이 검색 11회·일반 5단위 근처인지. (2) 4번 Y3(선택, 쓰지 않는 대안 확인용): 고친 6.2절 함수로 다시 실행. 결과는 data-researcher에게(리서치 한 줄, 이 브랜치 커밋) |
| T4 | ★ 확인·버림 규칙과 레코드 만들기 (`feat(media)` 커밋) | backend-dev (PM 결정 2) | M3a5 머지 | (1) `videos.list` 항목 하나 + `EventContext` + 날짜 → 후보 하나 또는 버림 이유. M3a5 계획 "규칙" 표의 "버림" 줄마다 합성 항목으로 시험(임베드 불가, `private`·`unlisted`, `blocked: [KR]`, `allowed: [JP]`, `ytAgeRestricted`, 다른 채널 ID, ID 10자, 컬러 복원 두 형태, 차단 목록 `id`·`channel`, R1~R9 위반). 버린 수는 DEBUG에 이유별 개수만. (2) 레코드가 규칙 표 "레코드 값"과 필드마다 같다 — 위 계약 예시를 만드는 합성 항목에서 `MediaItem` JSON이 예시와 키·값이 모두 같다(`retrieved_at` 은 `MutableClock`, `place_ref` 는 사건 값). 응답 어디에도 `link_only` 없음. `duration`: `PT1M57S` → 117.0, `PT10M1S` → 601.0, `P0D`·없음·깨짐 → null. 설명 첫 줄 200자 자름, 빈 설명 → null. (3) `embed_url` 이 형식과 정확히 같고 `autoplay`·`mute` 가 없다. `app.web-origin` 을 바꾸면 `origin` 값이 따라 바뀐다(URL 인코딩). (4) 후보: `official` true, `eventLink` `NONE`, `explicitDates` 빈 목록, `basis` `YOUTUBE_OFFICIAL`, `origin` = `MediaOrigin("youtube", ID, null, null, 채널 ID)` (null 아님 — `SearchCacheFormat.write` 가 거부하지 않는다). (5) 촬영 시점: `recordingDate` 가 사건 날짜 이하면 그 값, 늦으면 다음 재료. 제목 「1986 서울아시안게임 개막식」 → `1986` `year`. 제목에 날짜가 없고 설명에 「1986년 9월 20일」 → `1986-09-20` `day`. 설명에만 「2023년 복원」(늦음) → null. 회고 제목 「1986년 9월 20일 오늘 #제10회 #아시안게임 #개막」 → `captured_at` null, `dateTextExcluded` false. 대한뉴스 제목 「대한뉴스 제 1715호-서울 올림픽 개막」(설명 "제작일: 1988-09-17") → `captured_at` null, `dateTextExcluded` true. `publishedAt` 은 어디에도 쓰이지 않는다(값을 바꿔도 결과 같음). (6) "데모·표본 날짜" 표(M3a5 계획)의 자동 테스트 행 중 점수·붙음 행이 `VideoAccuracy` 와 함께 통과(1986 클립 60 붙음, 1715호 50 안 붙음, 예행연습·카운트다운·발사·50주년·601초 안 붙음). (7) **미디어 원칙** (제품 정의서 6절, `CLAUDE.md` 7.1절): 모든 레코드에 `source_name`(채널 이름)·`source_url`·`license`(`provider-terms`)·`attribution` 이 있고, `source_name` 이 빈 영상은 만들지 않는다(`MediaNormalizer`). `captured_at` 을 모르면 null이고 `date_precision` 이 "촬영 시점 미상" 표시가 되는 값이다(그날 것처럼 보이지 않음). 영상은 `video_embed`(공식 임베드, 원 출처 주소) 하나뿐이고 파일을 복사·내려받는 코드가 없으며, 통신사 사진 경로가 없다. (8) 아직 연결하지 않음 → 기존 시험 단언·응답·요청 수 그대로. `./gradlew check` 성공. 보고서에 M3a5a 누적 S1 — **확인점 1 (T4 뒤 누적 300 넘으면 T5 전에 멈추고 크기 결정 1 (b)로 나눌지 묻는다)** |
| T5 | 출처 연결·캐시 30일·키 없을 때 끄기 (`feat(media)` 커밋) | backend-dev | T4 | (1) **출처** (`EventVideoSource`, key `youtube`): 검색어가 있는 사건 × 허용 채널 = n. **n ≥ 1 일 때만** `YoutubeQuota.reserve(SEARCH, n)`; 거짓이면 요청 0으로 `QUOTA_SKIPPED`. **n = 0(모든 사건의 제목이 빔)이면 `reserve` 를 부르지 않고 요청 0으로 `OK`**(사건마다 빈 목록, 예외 없음 — M3a5 결정 3). 1단계 검색을 안쪽 `Deadline` 에서 동시에 → `Failed` 가 하나라도 있으면 `FAILED`, `QuotaExceeded` 면 `SEARCH` 소진 표시(`exhausted`) + `QUOTA_SKIPPED`(둘이 섞이면 어느 쪽인지 보고서에 적고 시험으로 고정). 영상 ID가 하나도 없으면 **`reserve(UNITS, …)` 를 부르지 않고** `videos` 호출 0회로 `OK`(사건마다 빈 목록). 있으면 `ceil(ID/50)` 단위를 `reserve(UNITS, …)`(모자라면 `QUOTA_SKIPPED`) → 2단계 동시에 → `Failed` 면 `FAILED`, `QuotaExceeded` 면 `UNITS` 소진 표시 + `QUOTA_SKIPPED`. 사건마다 그 사건 검색 순서대로 T4 후보. 안쪽 작업은 출처가 끝나거나 인터럽트되면 닫힌다(인터럽트 시험). (2) **키 없음**: `YoutubeProperties` 의 키가 비면 `VideoSearch` 가 그 출처의 작업도 캐시 조회도 하지 않는다(가짜 저장소 호출 0, WireMock 요청 0). 응답이 `main` 과 같다(기존 세계 API 시험이 단언을 바꾸지 않고 통과). 방법(예: `EventVideoSource` 에 기본 참인 `enabled()`)은 보고서에. (3) **세계 API 끝까지**(캐시 켠 전용 시험 클래스, Wikimedia·YouTube 모두 WireMock): 합성 1986-09-20에서 `video_embed` 하나 — 위 계약 예시의 키가 모두 있고 `selection {youtube_official, 60}`, `provider` `youtube`. 같은 날짜 두 번째 호출은 YouTube 요청 0이고 응답 JSON이 같다(`retrieved_at` 포함). 합성 2002-06-22(검색 0건): 검색 1회·`videos` 호출 0회·`reserve(UNITS, …)` 호출 0, 두 번째 호출 요청 0(0건 캐시). (4) **쿼터 행**(M3a5 계획 "데모·표본 날짜" 표): 검색 79회 쓴 상태에서 사건 1개 날짜 → 검색 1회, 80회 → 요청 0·응답 200·`partial` false·캐시 줄 없음. 사건 3개 날짜에 78회 → 요청 0(부분 검색 없음). 403 `quotaExceeded` → `partial` false·줄 없음·그 뒤 태평양 자정까지 검색 0, 자정 뒤(`MutableClock`) 다시 검색. **읽을 수 없는 403 본문 → `FAILED`(`partial` true·줄 없음·소진 표시 없음 — 다음 호출이 다시 부름)**. (5) **30일 지우기**: `source = 'youtube'` 이고 `fetched_at + 29일 ≤ 지금` 인 줄을 지우고, 29일 1초 전 줄과 `commons` 줄은 남긴다(Testcontainers + `MutableClock`). 서버 시작 직후 한 번 돈다(컨텍스트 시험), 그 뒤 1시간 간격(설정 또는 상수, 보고서에). 키가 비어도 돈다. (6) **캐시 쓰기 실패**: `SearchCache.Session.write` 가 한 번 실패하면 같은 조회의 나머지 쓰기(다른 출처 포함)를 건너뛴다 — 두 출처 가짜 저장소 시험에서 쓰기 시도 1번, WARN 한 줄. M3a4c 캐시 시험(`VideoSearchCacheTest` 등)이 단언을 바꾸지 않고 통과. (7) **실패·시간**: YouTube 500 → `partial` true·줄 없음·다음 호출이 다시 부름. YouTube 지연이 `lookup-deadline` 보다 길면 미디어 구간이 `lookup-deadline` + `CANCEL_GRACE` + 0.5초 이하, 사진 두 경로가 늦어지지 않고(`WorldStoryEventPhotoPartsRegressionTest` 방식), `partial` true, 줄 없음. (8) 차단 목록 `source: youtube` 의 `id`·`channel` 이 새 결과와 캐시에서 되살린 결과 모두에서 영상을 뺀다. (9) `tragic` 사건(합성)과 1950-06-25·2014-04-16 → YouTube 요청 0. 1995-03-15(사건 없음) → 요청 0. (10) 응답 어디에도 `link_only` 없음, YouTube 레코드가 R5·R9 통과. (11) `./gradlew check` 성공. 보고서에 M3a5a 누적 S1 — **확인점 2 (650 넘으면 알림)** |
| T6 | 개인정보처리방침 문장 현재형과 시행일 (`docs(privacy)` 커밋, 머지하는 날) | frontend-dev | T8 (PM이 PR을 승인하고 머지 날짜를 정함) | (1) `web/privacy.html` 의 "지금은 한국어 위키백과, Wikidata, Wikimedia Commons(날짜별 사진 분류)이고, YouTube Data API가 더해질 예정입니다." 를 "지금은 한국어 위키백과, Wikidata, Wikimedia Commons(사진·영상), YouTube Data API(KTV 아카이브 채널 영상 검색)입니다." 로 바꾼다(Commons 괄호는 M3a4b 뒤 영상도 찾으므로 함께 고친다. PM이 T8에서 다른 문구를 주면 그 문구로). (2) `시행일: 2026년 10월 16일` 을 머지하는 날로. (3) `web/src/privacy/privacy.test.ts` 의 문장·시행일 두 단언만 새 값으로 바꾼다. 다른 단언(바깥 URL 목록 C7, 연도 패턴이 `2026년` 하나)은 그대로 통과. (4) `cd web && npm run lint && npm run format:check && npm run typecheck && npm test` 성공. (5) 커밋 하나. 그날 머지하지 못하면 머지하는 날 시행일만 한 줄 더 커밋 |
| T7 | 완료 기준 검증과 전체 테스트 | test-engineer | T5 | (1) T4·T5·T6 기준 표(기준마다 시험 이름). 빠진 기준은 시험을 더한다(제품 코드는 고치지 않음). 특히 **n = 0 이면 `reserve` 를 부르지 않는 두 경우**(전 사건 제목 빔, ID 0개)와 읽을 수 없는 403이 각각 시험에 있다. (2) `cd server && ./gradlew check` **2회** 반복 안정. 쿼터·시간(창·타임아웃·인터럽트)·캐시 DB 실패 시험이 든 클래스는 **3회**. (3) C2 출력 없음. (4) S1·S2를 파일별로 보고. (5) 새 시험 모두 `findAllUnmatchedRequests` 빈 목록이고, 어떤 시험도 `googleapis.com`·`youtube.com` 에 연결하지 않는다(시험 설정 주소). (6) 로그 캡처(DEBUG 포함)에 가짜 키·검색어(사건 제목)·영상 ID·채널 ID·날짜가 없다. (7) 기존 시험 단언 변화 목록(`git diff main...HEAD -- server/src/test` 중 기존 파일) — 이 기능은 **단언을 바꾸지 않는다**(M3a5에서 이미 반영된 새 yml 값 4건 제외, 새 시험 파일 추가만). 바뀐 단언이 있으면 이유와 함께 보고. (8) 결과 표: 데모·표본 8날짜(합성 fixture)마다 첫·두 번째 호출의 YouTube 검색·`videos.list` 수, 붙은 영상, `captured_at` |
| T8 | PM 품질 확인 (PR 검수 때) | PM | T7, T0 Y5 | (1) 루트 `.env` 에 `YOUTUBE_API_KEY`(있음)와 저장소 주소 `WIKIMEDIA_USER_AGENT`. `docker compose up -d db`, `cd server && ./gradlew bootRun` — 시작 로그에 `YOUTUBE_API_KEY is empty` 가 없다. (2) 아래 P1로 데모 3 + 표본 5 날짜를 뽑아 "데모·표본 날짜" 표의 PM 열과 대조: 허용 채널 밖 영상 없음, 엉뚱한 영상 없음(예행연습·카운트다운·발사·다른 노선·컬러 복원), 날짜가 적히지 않은 영상은 `captured_at` null, `embed_url` 에 `autoplay`·`mute` 없음. (3) P1을 한 번 더 돌려 결과가 같다(캐시). (4) P2로 `youtube` 줄이 있고 요청자 정보 열이 없다. (5) 콘솔에서 그날 검색 사용량이 예상(새 날짜마다 사건 수, 이 확인으로 12회 이하) 안인지. (6) 다른 날짜로 찾아보지 않는다. (7) 서버 로그에 `YouTube call failed: … cause=status 403` 이 반복되지 않는다(반복되면 403 `quotaExceeded` 본문 모양이 다르다는 뜻 — 이 계획 "M3a5 계획과 달라지는 것" 2, 리스크 3). (8) T6 문구를 확인하고 머지 날짜를 정한다 |

**머지 뒤 (`main`)**: PM이 M3b3c T6 — 실제 YouTube(KTV 아카이브) 영상으로 화면 확인(nocookie + IFrame API 조합 포함). 1986-09-20 표본 버튼이 대상이다. 1988-09-17 대한뉴스는 M3a5b 뒤. 나머지 날짜에 YouTube 영상이 없으면 그것이 정상이다.

```
# C2 — 키 값이 코드·설정 파일에 없는지. 출력 없음 (값은 .env 에만)
grep -rnE 'AIza[0-9A-Za-z_-]{20,}' server/src web/src .env.example

# S1 — 제품 코드·설정 줄 수 (M3a5와 같은 명령, M3a5 머지 뒤 main 기준)
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'

# P1 — (T8) 서버 실행 중. 날짜마다 YouTube 영상만
for d in 1988-09-17 2002-06-22 1995-03-15 1950-06-25 1969-07-20 1986-09-20 2000-02-29 2014-04-16; do
  echo "== $d"
  curl -s "http://localhost:8080/api/story/world?date=$d" \
    | jq -c '{partial, events: [.events[] | {title, youtube: [.media[] | select(.provider=="youtube") | {id, source_name, captured_at, date_precision, selection, embed_url}]}]}'
done

# P2 — (T8) 캐시 줄. .env 의 POSTGRES_USER·POSTGRES_DB를 바꿨으면 그 값으로
docker compose exec db psql -U postgres -d the_world_that_day \
  -c "SELECT source, count(*), min(fetched_at), max(fetched_at) FROM event_media_search_cache GROUP BY source"
```

담당별: backend-dev 2 (T4 ★ — PM 결정 2, T5), frontend-dev 1 (T6), test-engineer 1 (T7), PM 2 (T0, T8). 모두 6개.

## 테스트 방침

- M3a5 계획 "테스트 방침" 절 그대로다. **외부 네트워크를 부르지 않는다.** YouTube 응답은 공식 문서 필드 구조로 만든 합성 fixture(M3a5 T2의 `fixtures/youtube/` 에 더한다, README에 근거 문서 URL). 합성 영상 ID는 `Abc123xyz_0` 처럼 11자, 합성 채널 ID는 시험이 `youtube.allowed-channels` 로 준다. 가짜 키 `test-key-not-real` + WireMock 주소.
- 쿼터 초기화는 `MutableClock`. 시간·창 시험의 상한은 설정값과 `Deadline.CANCEL_GRACE` 에서 계산한다. 저장소·지우기는 Testcontainers(Docker 필요).
- 반복: T7은 `./gradlew check` **2회**, 쿼터·시간·DB 실패 시험이 든 범위는 **3회**.
- **날짜는 제품 정의서 12절 데모 입력 3개(1988-09-17, 2002-06-22, 1995-03-15)와 표본(1950-06-25, 1969-07-20, 1986-09-20, 2000-02-29, 2014-04-16 — M3-P13의 공개 역사 사건 날짜·경계값)뿐이다.** 실제 사람의 생년월일이 아니다. 쿼터 시계의 2026년 경계 시각은 이야기 날짜가 아니다. 시험용 `tragic` 사건은 지어낸 문장이다.
- **어떤 날짜에 영상이 없으면 그대로 두고, 기능을 보이려고 다른 날짜를 찾거나 제안하지 않는다**(M3a3c 결정 6, PM 피드백 2026-10-16). 실제 결과 품질은 PM 품질 확인(T8).

## 데모·표본 날짜로 확인할 것

자동 테스트 열과 PM 열의 내용은 M3a5 계획 "데모·표본 날짜로 확인할 것" 표가 기준이다(1986-09-20, 1988-09-17, 1969-07-20, 컬러 복원, 회고 편집물, 2000-02-29, 2002-06-22, 1950-06-25·2014-04-16, 1995-03-15, 쿼터, 키, 임베드 주소, 캐시 행). 이 기능이 맡는 행은 **T4 (6)에서 점수·붙음, T5 (3)·(4)·(9)에서 날짜별 응답·쿼터·요청 0, T8에서 PM 열**이다. 키 행의 "헤더·`toString` 에 키 없음"은 M3a5 T1·T2에서 끝났고, 이 기능은 "비면 요청 0·캐시 조회 0"(T5 (2))만 맡는다.

## 크기 (400줄 규칙)

S1 = 제품 코드·설정(`server`, 테스트 제외, M3a5와 같은 명령). 루트 `.env.example` 과 웹 `privacy.html`(약 2줄)은 S1에 들어가지 않는다.

### 실측 비율

| 기능 | 파일별 셈 | 실제 S1 | 비율 |
|---|---|---|---|
| M3a4c (PR #25) | 약 323 | 583 / −23 | ×1.8 |
| M3a4b (PR #24) T0~T3 | 약 405 | 954 / −222 | ×2.36 |
| **M3a5 T1~T3** (확인점 1, 2026-10-26) | 약 210 (T1 92·T2 73·T3 45) | **525 / −6** (T1 뒤 200, T2 뒤 413, T3 뒤 525) | **×2.5** (T1 ×2.2, T2 ×2.9, T3 ×2.5). 기대 ×2.0(약 420)보다 컸고 확인점 500을 넘어 이 기능이 나뉘었다 |

### 다시 셈

M3a5 계획 크기 표의 T4·T5 파일별 셈을 가져왔다(분할로 새 일은 없다. T5의 `reserve` 건너뛰기 두 줄은 T5 셈 약 106에 이미 포함된 범위).

| 작업 | 파일별 셈 | ×2.0 | ×2.5 (M3a5 실측) | ×2.9 (T2 실측) |
|---|---|---|---|---|
| T4 | 버림 규칙 6개(약 20) · 컬러 복원·대한뉴스·회고 정규식(약 15) · 촬영 시점 차례(약 12) · 임베드 주소·ID 검사(약 8) · 레코드(약 25) · 길이·설명 첫 줄(약 10) · 거르기·정규화·origin(약 8) = 약 98 | 약 196 | 약 245 | 약 284 |
| T5 | `find` 두 단계·예약·상태(약 60) · 사건별 후보(약 15) · 키 없을 때 끄기(약 4) · 쓰기 실패 `down`(약 2) · 30일 지우기(저장소 삭제·주기 실행, 약 25) = 약 106 | 약 212 | 약 265 | 약 307 |
| T6·T7 | 웹·시험만 — S1 0 | 0 | 0 | 0 |
| **합계** | **약 204** | **약 408** | **약 510** | **약 592** |

기대값은 M3a5 실측 ×2.5를 쓴 **약 510 (410~590)** 이다. 새 외부 호출이 없는 연결 작업(M3a4c ×1.8 쪽)이면 줄 수 있고, 규칙·정규식이 많은 T4는 T2 쪽(×2.9)으로 갈 수 있다. 400을 넘으므로 크기 결정 1을 둔다.

### 크기 결정 1 (400을 넘는다 — M3a5a를 시작할 때 정해도 된다)

| 안 | 내용 | 크기 (기대, 범위) | M3 PR 수 |
|---|---|---|---|
| **(a) 한 PR + 확인점 (추천)** | T0~T8 모두 `feat/youtube-video-source`. **확인점 1**: T4 뒤 누적 S1이 300을 넘으면 T5 전에 멈추고 (b)로 나눌지 묻는다. **확인점 2**: T5 뒤 650을 넘으면 알림(멈추지 않음) | 약 510 (410~590) | 23 |
| (b) 두 PR로 또 나눔 | 앞 PR = T4 + 검증(규칙·레코드, 동작 변화 없음, 약 245), 뒤 PR = T5·T6·T8 + 검증(연결, 약 265, 영상이 나옴). 이름은 M3a5a(규칙)와 새 기능(연결, 예: M3a5a2 `feat/youtube-video-wiring` — PM이 정함). 순서 M3a5 → M3a5a → (새 기능) → M3a5b | 약 245 / 약 265 | 24 |
| (c) 30일 지우기(T5 (5), 약 25~60줄)를 뒤로 미룸 | 29일이 되기 전에 다음 PR에서 넣는다. 처음 `youtube` 줄이 쌓인 날부터 30일 의무가 시작되므로 늦어도 그 안에 들어가야 한다. 시험·리뷰가 한 번 더 | 약 450 / 약 60 | 24 |

추천 이유 (a): 이 PR에서야 영상이 응답과 화면에 나온다. 앞 PR만 내면(b) 눈에 보이는 동작이 없는 규칙 코드를 따로 리뷰하게 되고, M3a5를 이미 한 번 나눈 뒤라 PR이 하나 더 늘어난다(검증·리뷰·PM 확인이 한 번씩 더 든다). 대신 총량이 M3a5(실제 525)와 비슷하므로 **T4 뒤 확인점(멈춤)** 에서 실제 숫자를 보고 (b)로 바꿀 수 있게 둔다 — T4가 순수 규칙·레코드 코드라 나누기 좋은 자리이고, T5는 T4 없이 시작할 수 없어서 (b)로 바꿔도 고칠 것이 없다. (c)는 줄이 적게 줄고 30일 의무의 시한을 만든다.

각 backend-dev 보고서(T4·T5)는 누적 S1을 적는다. 테스트(S2)는 따로 보고한다.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 메모 |
|---|---|---|
| M3a5b 대한뉴스 | YouTube 출처(`youtube` key), 확인·버림 규칙, 대한뉴스 제목 후보(`dateTextExcluded` true, `captured_at` null, 제목 그대로), 캐시(`youtube` 줄), 쿼터(검색 버킷 공유, 80회 상한) | 제목 대조(N-e)를 어디에 붙일지(어댑터 확장 / 새 출처)와 같은 꼭지 여러 ID(R-N7)는 M3a5b 계획이 정한다 |
| M3b3c T6 (웹, PM) | 실제 YouTube 영상 | 이 기능 머지 뒤 `main` 에서 |
| M6b | 쿼터 소진 동작, 세계 섹션 시간 측정, 403 본문 모양 확인 | 로드맵 M6b 그대로 |

## 리스크와 확인할 것

- **M3a5 리뷰에서 넘어옴 (2026-10-26, 권장 1·7)**: (1) 태평양 자정 직전에 보낸 요청의 403 `quotaExceeded` 가 자정 뒤에 도착하면 `YoutubeQuota.exhausted` 가 새 날을 소진으로 표시해 하루를 잃는다 — T5에서 "예약한 날과 다르면 소진 표시를 하지 않음" 같은 처리를 정하고 시험한다. (2) 연결 타임아웃(`youtube.connect-timeout` 2초)이 실제로 쓰이는지는 시험이 없다 — `YoutubeDataApiClient` 가 `HttpClient.Builder` 를 주입받게 하면 시험할 수 있다(T5에서 하거나 M6b 후속). (3) M3a5 T7의 `WorldStoryYoutubeNotWiredTest` 는 T5가 출처를 연결하면 의도대로 깨진다 — "연결됨" 시험으로 바꾼다.

M3a5 계획의 리스크 가운데 연결·화면에 걸린 것만 가져왔다. 나머지(키 노출·약관 개정·시험이 진짜 키를 쓸 위험)는 M3a5에서 닫혔다.

1. **크기** — 기대값 약 510이 400을 넘는다. 크기 결정 1과 확인점 두 개로 다룬다.
2. **검색 하루 100회**(위험 B·9) — 80% 상한이면 캐시에 없는 새 날짜를 하루 약 26개 처리한다. 넘으면 그날 YouTube만 빠진다(정상). PM이 확인 명령을 실행한 날은 검색을 먼저 쓴다. 시간 초과·실패한 검색은 예약을 돌려주지 않고 캐시되지도 않으므로, 느린 날에는 같은 날짜가 검색을 다시 쓴다. 같은 새 날짜를 동시에 두 번 부르면 검색이 두 번 나간다(로컬 데모에서는 드묾).
3. **403 `quotaExceeded` 본문 모양이 문서에 없다** — 실제 응답의 `error.errors[].reason` 모양이 다르면 쿼터 소진이 `FAILED` 로 읽힌다(소진 표시 없음, 캐시 안 됨). 우리 상한 80회가 먼저 호출 수를 막으므로 구글 한도에 닿기 전에 `QUOTA_SKIPPED` 가 되는 것이 보통이다. T8 (7)에서 PM이 로그를 본다. 다르게 나오면 클라이언트의 가르기만 고치는 작은 `fix/` 다.
4. **국내 데모 영상은 M3a5b에 달려 있다** — 1988-09-17의 KTV 아카이브 영상은 대한뉴스 꼭지(제1715호)라서, 회차 날짜를 붙이는 M3a5b가 있어야 기준선을 넘는다. 이 기능만으로 붙는 데모·표본 영상은 1986-09-20 개막식 클립 정도다. 영상이 없는 날은 사진만 나오는 것이 정상이다.
5. **임베드 허용이어도 재생이 막힐 수 있다**(위험 12) — M3b3c가 재생 오류 때 그 항목을 숨긴다(10.1절 22번).
6. **nocookie + IFrame API 조합**(위험 15) — 문서에 없다. 머지 뒤 M3b3c T6에서 안 되면 임베드 도메인을 `www.youtube.com` 으로 바꿔야 할 수 있다. 그때 PM에게 묻는다(서버 도메인·웹 `YOUTUBE_EMBED_ORIGIN`·방침 문구·시행일이 함께 바뀜).
7. **기관 이용 조건**(위험 16, 10.1절 8번) — KTV 아카이브 채널 설명의 "부분발췌, 상업적 이용 등 무단사용을 금합니다". 결정 (a)(비상업 공식 임베드 + 차단 목록) 그대로, 공개 배포 전 KTV 서면 확인(로드맵 후속).
8. **엉뚱한 영상** — 같은 주제 다른 날 회차, 발사일 클립, 다른 노선 개통(3.5절 Y2). M3a4 규칙과 T4 (6), PM 품질 확인(T8)으로 막는다.
9. **쿼터 계수기는 다시 켜면 0**, **30일 지우기는 꺼진 서버에서 못 돈다**(켜자마자 지움) — 로컬 데모라 허용, 공개 배포 때 다시 본다.
10. **세계 섹션 시간** — 같은 미디어 창 안에서 돌아 최악 시간을 늘리지 않는다(T5 (6)·(7)). 스텁 약 11.7초 > 웹 10초는 M6b에서 다룬다(지켜보는 위험).
11. **사생활** — YouTube에는 사건 이름(대표 링크 제목)과 채널 ID만 보낸다. 생년월일·장소는 보내지 않는다. 캐시 줄에 요청자 정보가 없고, 로그에 검색어·날짜가 없다(T7 (6)).

## PM 결정 필요

이미 정해져 다시 묻지 않는 것: M3-P14, 10.1절 5번 (a) KTV 아카이브만, 6번 (a) 채널 한정 검색 + 캐시·넘으면 건너뜀, 7번 (a), 8번 (a) + 차단 목록, 9번 (a), 22번 (a), 23번 (b), 24번 (a), 대한뉴스 분리(로드맵 D1 (a)), 쿼터 계수기는 메모리, M3b3c에서 넘어온 3가지, M3a5 분할 (b)·시험 설정 WARN 고침 (a)·`reserve` n < 1 예외 유지 (a)(2026-10-26).

| # | 물을 것 | 선택지 | 추천 |
|---|---|---|---|
| 1 | 크기 기대값 약 510(410~590)이 400을 넘는다 — 나눌지 | (a) 한 PR + 확인점 T4 뒤 300(멈추고 (b)를 물음)·T5 뒤 650(알림) / (b) 두 PR로 또 나눔(T4 / T5·T6·T8), M3 PR 24개 / (c) 30일 지우기를 다음 PR로 미룸 | **(a)** — 위 "크기 결정 1" 설명. M3a5a를 시작할 때 정해도 된다 |
| 2 | ★ T4 확인·버림 규칙과 레코드 만들기(순수 함수 — 응답 항목 → 후보, 외부 호출·DB 없음, 정규식·날짜 정밀도·ISO 8601 길이 읽기)를 PM이 직접 구현할지 | (a) PM이 구현(backend-dev는 그동안 T5의 키 없음·30일 지우기 같은 T4와 겹치지 않는 부분을 먼저, T5 연결은 T4를 기다림) / (b) backend-dev | **(b)** — M3에서 PM이 ★ 작업을 매번 backend-dev로 정했고(M3a3b T2 ~ M3a5 T3), T4는 T5의 선행이라 PM 일정에 묶인다. 직접 해 보고 싶다면 T4가 가장 떼어 맡기 좋다(외부 호출·DB가 없고 시험이 쉽다) |
