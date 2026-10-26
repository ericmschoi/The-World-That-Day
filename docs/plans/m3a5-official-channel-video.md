# 공식 채널 영상 자동 검색: YouTube (M3a5)

> 브랜치: `feat/official-channel-video` (`main` 28ffb01 — PR #25 M3a4c 머지 뒤 — 에서 갈라짐) · 작성: planner · 상태: **개정 작성 2026-10-25, PM 검토 대기** (처음 승인 2026-10-11, 다시 승인 2026-10-12). 2026-10-25 개정: 승인 뒤 머지된 M3a4(PR #23)·M3a4b(PR #24)·M3a4c(PR #25) 코드와 이름·확장 지점·시험 방식을 맞췄다. M3b3c에서 넘어온 3가지와 M3a4c 리뷰 권장 1·3의 위치를 정했고, 크기를 다시 셌다.
> 이력: 2026-10-11 영상 리서치 T5와 PM 결정(`docs/research/video-sources.md` 10.1절 5~9·22~24번) 반영. 대한뉴스(이전 T2)는 M3a5b `feat/daehan-news-video` 로 나눔(로드맵 D1 (a), 2026-10-12). 2026-10-12 PM이 키를 발급하고 Y1·Y2·N3·Y4를 실행했고(리서치 3.5절), data-researcher가 YouTube 경로(Y-a·Y-d)를 "확인됨"으로 갱신함(6bf0267) → T0b 완료, T0은 Y5만 남음.
> 기준: M3-P9 (b), M3-P14(허용 공식 채널만), 안전장치 1~6, `CLAUDE.md` 7절 1~7항, `docs/plans/m3a4-event-video.md`(검색 틀·정확도 규칙), `docs/plans/m3a4b-commons-video.md`(어댑터·두 단계 대기), `docs/plans/m3a4c-media-search-cache.md`(결과 캐시, "뒤따르는 기능", 리스크 3·9), `docs/plans/m3a3-event-media.md`(형식, R5·R9), `docs/plans/m3b3c-youtube-embed.md`(웹 계약), `docs/research/video-sources.md` 3.2절(V2·V3·V4 카드), 3.5절(PM 실행 결과, [177]), 3절 종합 경로 표·하루 처리량, 4절 YouTube 열, 7절 R-Y1~R-Y7·R-N5·R-N7, 8절 위험 9~17, 10.1절
> 선행: **M3a4c 머지됨 (PR #25, `main` 28ffb01)** — 결과 캐시·`QUOTA_SKIPPED`·`Candidate.origin` 이 있다. 출처 쪽 조건(YouTube 경로 "확인됨")은 6bf0267로 채워졌다. Y5(콘솔 사용량 확인)는 구현을 막지 않고 T8 전까지 하면 된다.
> **화면 노출**: M3b3b(동의·방침, PR #18·#19)와 M3b3c(YouTube 플레이어, PR #21)가 이미 머지되었다. 그래서 이 기능이 머지되면 API와 화면에 YouTube 영상이 함께 나온다(10.1절 9·23번 의무는 갖춰져 있음). 실제 화면 확인은 머지 뒤 PM이 하는 M3b3c T6이다.

## 이번 개정(2026-10-25)에서 바뀐 것 — 머지 코드와 대조

| 항목 | 2026-10-12 계획 | 머지 코드(`main` 28ffb01) 확인 결과 → 이 계획 |
|---|---|---|
| 붙는 자리 | "M3a4 검색 틀에 어댑터" | `media.video.EventVideoSource`(`key()`, `find(LocalDate, List<EventContext>, boolean dayPathsOff)` → `SourceResult(byEvent, englishLabels, status)`)를 구현하는 `@Component` 하나(key `youtube`). `VideoSearch` 가 `ObjectProvider` 로 출처를 모두 모으고, 출처마다 `deadline.start("video-" + key, …)` 로 같은 미디어 창에서 돌린다 → **`VideoSearch` 흐름은 고치지 않는다**. 고치는 곳은 키가 없을 때 끄는 한 곳(T5 (2))과 쓰기 실패 처리 한 줄(T5 (6))뿐 |
| 쿼터 건너뜀 | "캐시하지 않는다, `partial` 그대로" | 이미 머지됨: `Status.QUOTA_SKIPPED` 는 `VideoSearch.Running.result` 에서 실패로 보지 않고(`partial` 그대로), `await` 는 `Status.OK` 만 쓴다. 어댑터는 그 상태만 돌려주면 된다 |
| 부분 쿼터 | (없음) | `VideoSearch.await` 는 `OK` 결과를 **물은 사건 전부**에 한 줄씩 쓴다(결과가 없는 사건은 0건으로). 사건 일부만 검색하고 `OK` 를 내면, 검색하지 않은 사건이 0건으로 캐시된다 → **필요한 검색 횟수를 한꺼번에 예약**한다(전부 아니면 없음, "쿼터" 절) |
| 캐시 | "`media.search-cache.ttl.youtube` 기본 7일, 30일 넘으면 시작 실패" | 머지된 `SearchCacheProperties`(`media.search-cache.ttl.<출처>`)에 `youtube: 7d` 한 줄을 더한다. 30일 상한은 따로 검사한다(T1 (2)). **새로**: 머지된 캐시는 만료 줄을 다음 쓰기에서 덮기만 한다(M3a4c "범위 밖"). 그러면 아무도 다시 묻지 않는 날짜의 줄이 30일을 넘어 남는다 → 29일이 지난 `youtube` 줄을 지운다(T5 (5), III.E.4.d) |
| 저장 조건 | (없음) | `SearchCacheFormat.write` 는 `Candidate.origin` 이 없는 후보가 하나라도 있으면 그 사건을 쓰지 않는다(M3a4c 리스크 9) → YouTube 후보는 `MediaOrigin("youtube", 영상 ID, null, null, 채널 ID)` 를 반드시 넘긴다(T4 (4)) |
| 정확도 | "M3a4 규칙(공식 출처 +20 …)" | `VideoAccuracy`: `official` +20. 날짜 점수는 `originalCapture`·`explicitDates`·(`dateTextExcluded` 가 아니면) 제목·설명 속 날짜(`TextDates`)에서 받는다. 길이가 `media.video.max-duration-seconds` 600을 넘거나 모르면 `DURATION` 으로 거른다. **어댑터의 "601초 버림"은 이 규칙에 맡긴다**(결과 같음, 줄 수 절약) |
| 차단·제외 | (차단 목록만) | `MediaFilter` 는 차단 목록의 `source: youtube` `id`(영상 ID)·`channel`(채널 ID)을 이미 읽는다. 통신사 제외는 `image`·`video_file` 에만 걸고 공식 임베드는 건드리지 않는다 → 새 코드 없음 |
| HTTP | (없음) | `WikimediaHttpClient`(공정 `Semaphore(3)`, `Lane`, `openPhotoPath()`, Wikimedia User-Agent)는 Wikimedia 전용 → YouTube는 **자기 클라이언트**(T2). 마감은 같은 `Deadline` 을 쓴다("시간과 요청 제한" 절) |
| 같은 꼭지 여러 ID (R-N7) | M3a5에서 | **M3a5b로 옮김**: 리서치 사례(736·750·535·601호)는 모두 대한뉴스 꼭지다. 대한뉴스 제목은 이 기능에서 날짜 재료가 없어 기준선(60)을 넘지 못한다(50점) → 이 기능에서는 효과가 없다. M3a5b 계획이 (호수, 꼭지 제목)으로 같은 규칙을 이미 쓴다. 같은 영상 ID는 `VideoSearch` 가 한 번만 센다 |
| M3a5b용 "조회 지점" (옛 목표 3, T1 (7)) | 시험으로 확인하는 조회 지점 | **뺌**: `VideoSearch` 는 출처를 병렬로 돌린다. 그래서 같은 조회에서 한 출처의 결과를 다른 출처가 읽을 자리가 없다. M3a5b의 제목 대조(N-e, 추가 요청 0)는 YouTube 어댑터를 넓히는 쪽이 자연스럽다. 이 기능은 대한뉴스 제목 후보를 `dateTextExcluded` 로 그대로 남기는 데까지만 하고(T4), 넓히는 모양은 M3a5b 계획이 정한다 |
| `videos.list` 횟수 | 날짜당 1회(최대 50개) | 사건 3개 × `maxResults` 25 = 최대 75개라 **50개씩 나눠 날짜당 최대 2회**(일반 버킷 8,000단위에 비하면 작다). 검색 버킷 사용량은 그대로 |
| 계약 예시 | `lat`·`lng`·`place_ref`·`retrieved_at`·`date_relation.years` 없음 | 채움(M3b3c에서 넘어온 (1)). 웹은 `place_ref`(또는 좌표)·`retrieved_at` 이 없으면 숨긴다 |
| 방침 문장 | (없음) | 웹 작업 T6(M3b3c에서 넘어온 (2)) |

## 넘어온 것과 처리 위치

| 넘어온 것 | 출처 | 처리 |
|---|---|---|
| 계약 예시에 `lat`·`lng`·`place_ref`·`retrieved_at`·`date_relation.years` | 로드맵 M3b3c 메모 (1) | "API / 데이터 변경"의 계약 예시. T5 (3)의 끝까지 시험이 같은 키를 단언 |
| 방침 "YouTube Data API가 더해질 예정입니다" → 현재형 + 시행일 | 로드맵 M3b3c 메모 (2) | **T6** (frontend-dev, 머지하는 날 한 커밋) |
| 머지 뒤 PM이 M3b3c T6 | 로드맵 M3b3c 메모 (3) | 작업 목록 아래 "머지 뒤" |
| M3a4c 리뷰 권장 1 (붙어 있는 javadoc 두 개) | PR #25 리뷰 | **T7** — 시험 파일 주석만, S1 0 |
| M3a4c 리뷰 권장 3 (DB 실패 "WARN 정확히 한 줄"은 가짜 저장소로만 시험됨) | PR #25 리뷰 | **후속 후보**: 이 기능은 캐시 실패 처리를 바꾸지 않는다(T5 (6)의 한 줄은 쓰기 건너뛰기만 넓힌다). 실제 Hikari·Hibernate가 더하는 줄은 자리 표시 값뿐이고, 캐시 줄에는 장소가 없다. 장애 격리를 재는 M6b에서 실제 DB를 끊는 시험으로 보는 것이 맞다 |
| 30일이 지난 YouTube 줄 지우기 | M3a4c "뒤따르는 기능" | **T5 (5)** |
| 새 출처는 `Candidate.origin` 을 넘길 것 | M3a4c 리스크 9 | **T4 (4)** |
| 타임아웃·마감·요청 제한기 | 오케스트레이터 지시 | "시간과 요청 제한" 절 |
| 쿼터 두 버킷, 다시 켤 때, 경계 시험 | 오케스트레이터 지시 | "쿼터" 절, **T3** |
| T0 Y5(필수)·Y3(선택) | 2026-10-12 계획 | **T0** (PM), T8 전 |
| 미룬 작은 정리 7건 | 로드맵·M3a4c "다음 단계 제안" | "후속 작업 후보" 표 (이 PR 범위 아님) |

## 목표

1. 머지된 검색 틀(`VideoSearch`)에 **YouTube 공식 채널 출처**(`EventVideoSource`, key `youtube`)를 붙인다. 사건마다 KTV 아카이브 한정 `search.list` 를 부르고, 날짜의 후보 ID를 모아 `videos.list`(50개씩)로 확인한 뒤, 맞는 것만 `video_embed`(`provider: youtube`)로 낸다.
2. 쿼터 두 버킷(검색 하루 80회, 일반 하루 8,000단위, 태평양 시간 자정 초기화)을 지킨다. 넘으면 그날은 YouTube만 건너뛴다(`QUOTA_SKIPPED` — 사진·Commons 영상은 그대로, 오류 아님, 캐시 안 됨).
3. 결과 캐시에 `youtube` TTL(7일)을 더하고, YouTube API 데이터를 30일 안에 지운다(III.E.4.d).
4. M3b3c에서 넘어온 3가지를 닫는다: 계약 예시 빈 필드 채우기, 방침 문장 현재형과 시행일, 머지 뒤 PM의 M3b3c T6.
5. 세계 섹션 최악 시간을 늘리지 않는다("시간과 요청 제한" 절).

## 범위 밖

- 허용 채널 밖 YouTube(M3-P14), 방송사 채널, KTV 국민방송(Y2로 보아 지금은 넣지 않음 — 넣을 때는 설정값만 더함)
- 채널 한정 없는 검색(Y-b, 10.1절 6번 — Y3 결과가 좋아도 지금 결정은 (a)), 업로드 목록 색인(Y-c, 10.1절 7번), 서버 oEmbed 자동 판정
- 대한뉴스 회차 대조·색인과 같은 꼭지 여러 ID(R-N7) → M3a5b. 컬러 복원 재업로드를 대한뉴스 회차로 쓰기(보조 정규식 `\[대한뉴스\s*(?:제\s*)?(\d+)\s*호\]` 와 "컬러 복원" 캡션) → 후속 작업 후보
- 쿼터 증설 신청(공개 배포 #5, 10.1절 6번 (d)), 쿼터 계수기를 DB에 보존("쿼터" 절)
- YouTube 전용 요청 제한기("시간과 요청 제한" 절)
- 동의 단계·방침 내용(M3b3b 완료)과 플레이어(M3b3c 완료). 방침은 문장 하나와 시행일만 고친다(T6)
- 영상 내려받기·서버 경유 재생. 다른 날짜로 영상을 찾아 보이게 하는 것(PM 피드백 2026-10-16 — 영상이 없는 날은 사진만 나오는 것이 정상)
- 영상 슬롯 정책·서버 전체 시간 상한(M6b)

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| YouTube Data API v3 `search.list` (Y-a) ([video-sources.md](../research/video-sources.md) 3.2절 V2 카드, 종합 경로 표) | **확인됨** (3.5절, 6bf0267) | `part=snippet`, `channelId`, `q`, `type=video`, `videoEmbeddable=true`, `maxResults`, `regionCode=KR`, `relevanceLanguage=ko` | 키 `YOUTUBE_API_KEY` 는 서버 전용이고 **`x-goog-api-key` 헤더로만** 보낸다(URL에 넣지 않음, [96]). 검색은 별도 버킷 하루 100회, 잘못된 요청도 1회 [83]. 서비스 하나에 프로젝트 하나(III.D.1.c), 키를 공개 저장소에 넣지 않음(III.D.1.d) |
| YouTube Data API v3 `videos.list` (Y-d) | **확인됨** (3.5절: `embeddable`·`privacyStatus`·`regionRestriction`·`ytRating`·`duration` 확인, `recordingDate` 는 표본 모두 비어 있음) | `part=snippet,status,contentDetails,recordingDetails`, `id` 쉼표 목록 | 일반 버킷 하루 10,000단위, 1회 1단위 [83][86]. **메타데이터는 "제한된 양"을 최대 30일, 그 뒤 지우거나 새로 받음(III.E.4.d), 최신으로 유지 노력(III.E.4.e·f), 영상 저장 금지(III.E.1.a)** — 리서치 3.2절 V2 카드 "캐싱·저장"과 V2-9(확인됨, 문서). "제한된 양" 해석은 10.1절 7번 (a)로 정해짐(출처를 구분해 보여 주는 지금 설계, 업로드 목록 전체 색인 안 함) |
| YouTube 임베드 플레이어 (V4) | **확인됨** | 임베드 주소 형식 | `autoplay`·`mute` 없이, `origin` 을 넣는다 [102][103]. 200×200 이상·겹치기 금지·절반 넘게 보일 때만 자동 재생·하나만 — M3b3c(완료) |

- 출처 표시(III.F.2.a)·약관 링크(III.A.1)·방침(III.A.2)은 웹 M3b3b·M3b3c가 했다. 이 기능은 `attribution` "<채널 이름> · YouTube"와 `license_url` YouTube 이용약관을 레코드에 넣는다.
- 약관·정책은 2026-09-14에 갱신됐고 변경은 게시 30일 뒤 효력이 생긴다(위험 17). 그래서 구현 직전에 **T0c**(data-researcher)가 근거 문서 날짜와 조항을 다시 확인한다.

## 이어받는 계약 (`main` 28ffb01에서 확인한 이름)

| 부품 | 실제 이름·모양 | 이 기능에서 |
|---|---|---|
| 출처 | `media.video.EventVideoSource`: `key()`, `SourceResult find(LocalDate, List<EventContext>, boolean dayPathsOff)`. `record SourceResult(byEvent, englishLabels, status)`, `enum Status { OK, FAILED, QUOTA_SKIPPED }` — `FAILED` 는 어댑터가 WARN을 이미 남긴 것 | `YoutubeVideoSource`(이름은 backend-dev가 정함), key `youtube`. `englishLabels` 는 비움. `dayPathsOff` 는 쓰지 않음(날짜 단위 경로가 없음) |
| 검색 | `media.video.VideoSearch`: `start(Deadline, LocalDate, List<EventTarget>, boolean)` 가 앞 `media.video.max-events`(3)개 사건을 `EventContext.of` 로 만들고, 출처마다 캐시를 읽은 뒤 없는 사건만 `deadline.start("video-" + key, …)`. `Running.await()` → `Videos(byEvent, failed)`. `BASIS_ORDER` 에 `YOUTUBE_OFFICIAL` 있음. 같은 영상 ID는 한 번, 순서는 자유 라이선스 → 공식(`Candidate.official`) → 나머지 → 점수 | 흐름은 그대로. 키가 없을 때 그 출처를 건너뛰는 자리만 더한다(T5 (2)) |
| 후보 | `media.video.Candidate(item, title, description, originalCapture, explicitDates, eventLink, official, dateTextExcluded, basis, origin)` — 10개 요소. 9개 인자 생성자는 origin `null`(캐시에 저장되지 않음) | **10개 인자로** 만든다(T4 (4)) |
| 사건 | `media.video.EventContext(eventId, date, title, englishLabel, summary, wikidataQid, placeRef)` | `title`(대표 링크 한국어 제목)이 검색어 `q`, `placeRef` 가 레코드 `place_ref` |
| 정확도 | `media.video.VideoAccuracy`(`official` +20, `TextDates.find`, `notAfter(CaptureTime, LocalDate)`, `Reject.DURATION`·`LOW_SCORE`), 설정 `media.video.*`(`min-score` 60, `max-duration-seconds` 600) | 그대로 |
| 규칙 | `media.MediaFilter.check(MediaRecord, MediaOrigin)`, `media.MediaNormalizer.normalize(MediaRecord, LocalDate)` — R5: `video_embed` 는 `embed_url`·`provider` 가 있고 `provider` 가 `media.allowed-video-embed-providers` 안에 있으며 `url` 이 없음. R9: 주소는 http(s) | 그대로. 허용 제공자에 `youtube` 를 더한다(T1) |
| 선택 | `media.Selection.Basis.YOUTUBE_OFFICIAL` → `"youtube_official"` | 그대로 |
| 캐시 | `media.video.SearchCache`(`Session.read` 는 `start` 에서, `write` 는 `await` 에서, 읽기 실패 뒤 `down`), `SearchCacheFormat` v1(origin 없는 후보 거부, 읽을 때 다시 거름·정규화), `SearchCacheProperties`(`ttl(source)`, 0·없음 = 캐시 안 함, 음수 시작 실패), `EventMediaSearchCacheRepository`(`find`, `upsert`, 패키지 전용), 테이블 `event_media_search_cache` | TTL 한 줄, 30일 상한, 지우기 메서드, 쓰기 실패 뒤 `down`(T5) |
| 마감 | `wikimedia.Deadline`(`after`, `start`, `Task.await()` → `Done`/`TimedOut`/`Failed`/`Interrupted`, `close()` 가 남은 작업을 인터럽트, `CANCEL_GRACE` 1초). `CommonsVideoSource` 는 안쪽 `Deadline.after(readTimeout)` 으로 두 단계를 기다린다 | 같은 방식(T5 (1)) |
| 설정 | `MediaLookupProperties.lookupDeadline()`(3s, 미디어 요청 읽기 타임아웃을 겸함), `MediaProperties`(`allowedVideoEmbedProviders`, `blocklist`), `config.AppProperties`(`app.*`, 지금은 `data-dir` 만), `Clock` 빈(`config.ClockConfig`) | `app.web-origin` 을 `AppProperties` 에 더한다(T1) |
| 시험 기반 | `WikimediaWireMockTest`, 시험 `config/application.yml`(외부 주소를 `localhost:9` 로, Wikimedia 꺼 둠), `MutableClock`, `FakeVideoSource`, `VideoSearchCacheTest`·`VideoSearchCacheWindowTest`, `OutputCaptureExtension`, fixture `README.md` 방식(`fixtures/wikimedia/commons/video/README.md`) | YouTube도 시험 설정에서 꺼 둔다(T1 (4)) |

## 규칙

| 규칙 | 내용 |
|---|---|
| 허용 채널 | 설정값 `youtube.allowed-channels` = [`UC8_LPVE4Yuc6KF0opF6uS_w` (KTV 아카이브)]. **채널 ID로** 대조한다(R-Y4). 이름은 보지 않는다 |
| 검색 | 사건마다 채널마다 `search.list` 1회: `part=snippet`, `type=video`, `videoEmbeddable=true`, `channelId`, `q`=대표 링크 제목(`EventContext.title`, 한국어), `maxResults`=`youtube.max-results`(25), `regionCode=KR`, `relevanceLanguage=ko`. 제목이 빈 사건은 묻지 않는다(그 사건은 0건) |
| 확인 | 날짜의 후보 ID를 처음 나온 순서로 모아 중복을 빼고, 50개씩 `videos.list`(`part=snippet,status,contentDetails,recordingDetails`) — 날짜당 최대 2회, 동시에 |
| 버림 (레코드를 만들지 않음, `link_only` 도 아님) | `status.embeddable` false, `status.privacyStatus` ≠ `public`, `contentDetails.regionRestriction.blocked` 에 KR이 있거나 `allowed` 가 있는데 KR이 없음, `contentDetails.contentRating.ytRating` = `ytAgeRestricted`, `snippet.channelId` 가 허용 목록에 없음, ID가 `^[A-Za-z0-9_-]{11}$` 가 아님, `videos.list` 응답에 없는 ID, **컬러 복원 재업로드**(제목에 "컬러 복원"이 있거나 `\[대한뉴스\s*(?:제\s*)?\d+\s*호\]` 가 있음, R-N5), 차단 목록(`MediaFilter`), R1~R9 위반(`MediaNormalizer`). 버린 수는 DEBUG에 이유별 개수만 |
| 길이 | 10분(600초) 넘거나 모르면(`duration` 이 없거나 `P0D`) 붙지 않는다 — 어댑터가 아니라 `VideoAccuracy` 의 `DURATION` 규칙이 거른다 |
| 정확도 | M3a4 규칙 그대로. 후보는 `official` = true(허용 채널 ID를 확인한 뒤), `eventLink` = `NONE`, `explicitDates` = 빈 목록, `basis` = `YOUTUBE_OFFICIAL`, `title` = `snippet.title`, `description` = `snippet.description` 전체(사건어 판정용) |
| 촬영 시점 (`captured_at`) | 차례대로 처음 나온 것: `recordingDetails.recordingDate` → 제목의 첫 날짜(`TextDates`) → 설명의 첫 날짜. 어느 것이든 사건 날짜보다 늦으면(`VideoAccuracy.notAfter`) 버리고 다음 것을 본다. `publishedAt` 은 쓰지 않는다(R-Y1). **회고 편집물** — 제목이 `youtube.retrospective-title-patterns`(처음 `\d{4}년\s*\d{1,2}월\s*\d{1,2}일\s*오늘`)에 맞으면 날짜 점수는 받되 `captured_at` 은 `null`(R-Y3). **대한뉴스 제목**(`^\s*대한뉴스\s*제\s*\d+\s*호`) — 제목·설명의 날짜("제작일: YYYY-MM-DD" 등)는 회차 날짜라 쓰지 않는다: `captured_at` `null`, `dateTextExcluded` true. 회차 날짜는 M3a5b가 붙인다. 아무것도 없으면 `null` → "촬영 시점 미상". `originalCapture` 는 `recordingDate` 를 읽은 값(없으면 `UNKNOWN`) |
| 임베드 주소 | `https://www.youtube-nocookie.com/embed/<ID>?playsinline=1&rel=0&enablejsapi=1&origin=<app.web-origin URL 인코딩>`. `autoplay`·`mute` 를 넣지 않는다 |
| 레코드 값 (리서치 4절) | `id` `youtube-<ID>`, `type` `video`, `display_mode` `video_embed`, `url` null, `thumbnail_url`·`thumbnail_width`·`thumbnail_height` = `snippet.thumbnails.medium`, `video_sources` null, `duration_seconds` = ISO 8601 `contentDetails.duration` → 초(`PT1M57S` → 117.0, 읽을 수 없거나 0이면 null), `embed_url` 위 형식, `provider` `youtube`, `render`·`link_only_reason` null, `source_name` `snippet.channelTitle`, `source_url` `https://www.youtube.com/watch?v=<ID>`, `license` `provider-terms`, `license_status` `other`, `license_url` `https://www.youtube.com/t/terms`, `attribution` "<채널 이름> · YouTube", `description` `snippet.description` 첫 줄(최대 200자, 비면 null), `captured_at`·`date_precision` 위 규칙, `lat`·`lng` null, `place_ref` = 사건의 `placeRef`, `retrieved_at` = 지금(UTC, 초) |
| origin | `MediaOrigin("youtube", <ID>, null, null, <snippet.channelId>)` — `MediaFilter` 와 캐시가 쓴다 |
| 허용 제공자 | `media.allowed-video-embed-providers` 에 `youtube` |
| 키 없음 | `YOUTUBE_API_KEY` 가 비면 출처가 꺼진다: 요청 0, 그 출처의 캐시 읽기·쓰기 0, 시작 때 WARN 1줄(값은 출력 안 함) |
| 차단 목록 | M3a3a `media.blocklist` 의 `source: youtube` — `id`(영상 ID) 또는 `channel`(채널 ID). 캐시에서 되살릴 때도 다시 걸린다(M3a4c) |
| `tragic` | 요청 0 — `VideoSearch` 가 `tragic` 사건을 대상으로 넘기지 않는다(머지 코드) |
| 로그 | 실패마다 WARN 1줄 `YouTube call failed: call=<search|videos> cause=<status N|reason|예외 클래스>`. 주소·검색어(사건 제목)·영상 ID·채널 ID·날짜·키·응답 본문은 넣지 않는다 |

## 시간과 요청 제한

| 구간 | 지금 (`main` 28ffb01) | M3a5 뒤 | 근거 |
|---|---|---|---|
| 미디어 창 | `CommonsMediaLookup.lookup` 의 `Deadline.after(media.lookup-deadline = 3s)` 하나: 사진 두 경로와 `video-commons` 작업. 끝 + 취소 유예 1초 안(M3a4b 측정 약 3.03초) | 같은 창에 `video-youtube` 작업이 하나 더. 끝 시각이 같다 → **늘지 않음** | `VideoSearch.start` |
| YouTube 안쪽 | - | 안쪽 `Deadline.after(lookup-deadline)` 에서 1단계 검색(사건마다, 동시에 최대 3개) → 2단계 `videos.list`(50개씩, 동시에 최대 2개). 요청마다 읽기 타임아웃 = `media.lookup-deadline`, 연결 타임아웃 `youtube.connect-timeout` 2초, 재시도 없음(M3a3c 결정 1 (a) — 미디어 요청은 1회). 바깥 작업이 끝나거나 인터럽트되면 안쪽을 닫아 요청을 끊는다 | `CommonsVideoSource` 와 같은 방식 |
| 캐시 읽기 (창 안, 요청 스레드) | 출처 1개 | 출처 2개라 읽기 2번(보통 수 ms). DB가 죽으면 첫 읽기가 실패한 뒤(최대 `connection-timeout` 1초) `down` 이 되어 둘째는 건너뛴다 → 최악은 같다 | `SearchCache.Session.read` |
| 캐시 쓰기 (창 밖, 요청 스레드) | 출처 1개. 쓰기가 실패하면 그 출처에 WARN 1줄 | 출처 2개. **쓰기가 한 번 실패하면 같은 조회의 다른 출처 쓰기도 건너뛴다**(T5 (6)) → 창 밖 최악 +1초 그대로(M3a4c 리스크 3). 이 줄을 고치지 않으면 +2초가 된다 | `SearchCache.Session.write` |
| 쿼터 | - | 메모리 계수기, I/O 없음 | "쿼터" 절 |
| 30일 지우기 | - | 스케줄 스레드에서 돈다 — 요청 경로 밖 | T5 (5) |
| 세계 섹션 최악 | 스텁 측정 약 11.7초(이론 13~15초). 웹 시간 초과는 10초 — 지켜보는 위험, M6b에서 다시 | **같음** | 로드맵 후속 후보 "섹션 전체 시간 상한" |

**요청 제한기는 두지 않는다.** (1) Wikimedia `Semaphore(3)` 는 Wikimedia API 예절(동시 3개)과 사진 우선(`Lane`)을 위한 것이다. YouTube 문서에는 동시 요청 수 규칙이 없고 하루 단위 쿼터만 있다(리서치 3.2절 "요금·쿼터"). (2) 하루 총량은 쿼터 예약이 막는다. (3) 조회 하나의 YouTube 요청은 동시에 최대 3개(검색), 2개(`videos.list`)다. (4) YouTube 요청은 Wikimedia 자리를 쓰지 않으므로 사진 경로를 늦추지 않는다. 같은 새 날짜를 동시에 두 번 부르면 검색이 두 번 나갈 수 있다(리스크 2).

## 쿼터

| 항목 | 내용 |
|---|---|
| 버킷 | **검색**: `search.list` 1회 = 1, 상한 `youtube.daily-search-budget` 80(하루 100회의 80%). **일반**: `videos.list` 1회 = 1단위, 상한 `youtube.daily-unit-budget` 8,000(10,000단위의 80%) [83] |
| 예약 | 호출 **전에** 조회 하나에 필요한 수를 한꺼번에 예약한다. 검색은 (검색어가 있는 사건 수 × 허용 채널 수), 일반은 `ceil(ID 수 / 50)`. 남은 수가 모자라면 아무것도 세지 않고 요청 0 → `QUOTA_SKIPPED`. 예약한 수는 오류·시간 초과에도 돌려주지 않는다(문서: 잘못된 요청도 최소 1) |
| 403 `quotaExceeded` | 그 버킷을 다음 초기화까지 다 쓴 것으로 표시하고, 그 조회는 `QUOTA_SKIPPED`(검색에서 받았으면 `videos.list` 를 부르지 않는다). 그 밖의 403(`forbidden`, `rateLimitExceeded` 등)·4xx·5xx는 `FAILED` [91] |
| 초기화 | `America/Los_Angeles` 날짜가 바뀌면(자정, 서머타임 반영 — 한국 시간 16:00 또는 17:00) 두 버킷을 0으로 하고 소진 표시를 지운다. 시각은 `Clock` 빈에서 |
| 다시 켤 때 | **보존하지 않는다**(메모리 — 2026-10-12 승인 그대로). 같은 태평양 날에 서버를 다시 켜면 0부터 세므로 그날 최대 100회(구글 한도)까지 나갈 수 있고, 넘으면 403 `quotaExceeded` 처리가 막는다. 잃는 것은 20% 여유(PM이 확인 명령을 돌릴 몫)뿐이다. DB 보존은 쓰기가 마감 작업 안에서 일어나 M3a4b 리스크 9(마감 인터럽트가 JDBC를 끊음)를 다시 부르고 줄 수도 늘어서 하지 않는다 — 공개 배포 때 다시 본다(후속 후보) |
| 알림 | 그 태평양 날에 상한에 처음 닿거나 403을 처음 받으면, 버킷마다 한 번 WARN `YouTube quota spent: bucket=<search|units>` (날짜 없음) |
| 시험 (네트워크 없음) | `MutableClock` 으로 경계 시각을 맞춘다: 2026-03-08T07:59:59Z(태평양 3월 7일 23:59:59 PST)와 08:00:00Z(3월 8일 0시), 서머타임 시작 뒤 2026-03-09T06:59:59Z와 07:00:00Z, 서머타임이 끝나는 날 2026-11-01T06:59:59Z와 07:00:00Z, 그다음 2026-11-02T07:59:59Z와 08:00:00Z. 이 값은 쿼터 시계의 경계값이고 이야기 날짜가 아니다. 동시 예약은 스레드 여러 개로 |

## 캐시와 30일

- **TTL**: `media.search-cache.ttl.youtube: 7d`. 30일을 넘게(예 `31d`) 설정하면 시작 실패, 오류 글에 `media.search-cache.ttl.youtube`. `30d` 는 허용. 근거: 리서치 3.2절 V2 카드 "캐싱·저장"과 2절 V2-9(확인됨, 문서) — 키로 받은 공개 데이터는 "제한된 양"을 최대 30일 임시 저장하고, 30일이 지나면 지우거나 새로 받는다(III.E.4.d). 최신으로 유지하려고 노력해야 한다(III.E.4.e·f) → 7일마다 새로 받는다.
- **지우기**: 머지된 캐시는 만료 줄을 다음 쓰기에서 덮기만 한다. `source = 'youtube'` 이고 `fetched_at + 29일 ≤ 지금` 인 줄을 서버 시작 직후 한 번, 그 뒤 1시간마다 지운다(29일 + 1시간 < 30일). 키가 비어 출처가 꺼져 있어도 돈다. `commons` 줄은 건드리지 않는다. 서버가 꺼져 있는 동안은 지우지 못하고, 켜자마자 지운다(리스크 11).
- **저장하는 것**: M3a4c 형식 v1 그대로(정규화된 레코드·origin·제목·설명·촬영 시점 재료). 파일·포스터 없음. `Candidate.description` 이 전체 설명(최대 5,000바이트)이라 줄 하나가 수십 KB일 수 있다 — 메타데이터뿐이고 30일 안이다.
- 0건도 7일 캐시한다(2002-06-22 같은 날). `QUOTA_SKIPPED`·`FAILED`·시간 초과·예외·`Interrupted` 는 캐시하지 않는다(머지 코드).
- 요청한 사람 정보는 저장하지 않는다(M3a4c T1 열 단언 그대로). 쿼터 계수기는 API 데이터가 아니다.

## API / 데이터 변경

- `GET /api/story/world`: 요청·응답 모양은 M3a3 그대로이고 `video_embed` 레코드가 새로 나온다. 웹(M3b3c)과의 계약 예시(합성 값, 1986-09-20 표본 — M3b3c 웹 fixture `exampleYoutubeEmbed` 와 키가 같다):

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
- 설정 (T1). 표의 검사를 어기면 시작 실패, 오류 글에 키 이름:

| 키 | 기본값 | 검사 |
|---|---|---|
| `youtube.api-key` | `${YOUTUBE_API_KEY:}` | 비면 출처 꺼짐(WARN 1줄). 값은 로그·`toString`·오류 글에 나오지 않음 |
| `youtube.api-url` | `https://www.googleapis.com/youtube/v3` | http(s) 주소 |
| `youtube.allowed-channels` | [`UC8_LPVE4Yuc6KF0opF6uS_w`] | 비지 않음, 각 `^UC[A-Za-z0-9_-]{22}$` |
| `youtube.daily-search-budget` | 80 | 0~100 |
| `youtube.daily-unit-budget` | 8000 | 0~10000 |
| `youtube.max-results` | 25 | 1~50 |
| `youtube.connect-timeout` | 2s | 양수 |
| `youtube.retrospective-title-patterns` | [`\d{4}년\s*\d{1,2}월\s*\d{1,2}일\s*오늘`] | 정규식으로 컴파일됨 |
| `app.web-origin` | `http://localhost:5173` | 경로·쿼리·조각 없는 `http(s)://호스트[:포트]` |
| `media.allowed-video-embed-providers` | [`youtube`] (지금 빈 목록) | 머지된 검사 그대로 |
| `media.search-cache.ttl.youtube` | 7d | 0 이상(머지된 검사), **30일 이하**(새로) |

- 환경변수: `YOUTUBE_API_KEY` — 루트 `.env.example` 에 **주석 처리한 이름만**(`# YOUTUBE_API_KEY=`, 선택 항목 규칙 — 비면 Spring이 기본값을 건너뛰지 않도록), 값 없음, `VITE_` 금지. PM의 루트 `.env` 에는 이미 값이 있다(에이전트는 읽지 않는다).
- DB 변경 없음(V3 `event_media_search_cache` 그대로). 지우기는 그 테이블의 DELETE.
- `data/` 변경 없음. 웹: `web/privacy.html` 의 문장 하나와 시행일, 그 시험(T6).

## 작업 목록

순서: T0c → T1 → T2 → T3 ★ → (확인점 1) → T4 → T5 → (확인점 2) → T7 → 리뷰 → PR → T8(PM, Y5 먼저) → T6(머지하는 날) → 머지 → PM M3b3c T6. T3과 T4는 서로 의존이 없다 — PM이 ★ T3을 맡으면 backend-dev는 T4를 먼저 한다(T5는 둘 다 기다린다).

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | YouTube 키 발급과 확인 명령 | PM | 리서치 머지 | **대부분 완료(2026-10-11)**: 키 발급, 6.0절 1~3번(Y1·Y2), 8번(N3), 9번(Y4) — 리서치 3.5절. **남음**: (1) 10번 Y5 — 콘솔 할당량 페이지에서 검색 버킷 100회와 일반 10,000단위가 보이고, 사용량이 검색 11회·일반 5단위 근처인지(**T8 전, 필수**). (2) 4번 Y3 — 고친 6.2절 함수로 다시 실행(선택. Y-b는 쓰지 않는 대안이라 이 기능을 막지 않는다). 결과는 data-researcher에게(리서치 한 줄, 이 브랜치 커밋) |
| T0b | 리서치 확신도 갱신 | data-researcher | T0 | **완료 (6bf0267)**: Y-a·Y-d "확인됨", R-N5 컬러 복원·R-N7·R-N8·R-T1 낱말 추가 |
| T0c | 약관·쿼터 문서 다시 확인 (위험 17) (`docs(research)` 커밋) | data-researcher | - | (1) 근거 [83][85][86][87][88][91][96][97][98][102]의 "최종 업데이트" 날짜를 2026-10-11 확인 값과 나란히 적은 표를 보고서에 낸다. (2) 날짜가 바뀐 문서는 이 계획이 기대는 조항을 다시 읽고 "같음/다름"을 적는다: III.E.4.d(30일)·III.E.4.e·f, III.E.1.a, III.A.1·A.2, III.F.2.a, III.D.1.c·d, 쿼터 표(검색 하루 100회 별도 버킷, `videos.list` 1단위, 태평양 자정, 잘못된 요청도 1), 오류 `quotaExceeded` 403, `search.list` 매개변수(`channelId`·`type`·`videoEmbeddable`·`maxResults` 0~50·`regionCode`·`relevanceLanguage`), `videos.list` `id`·`part`, 키 헤더 `x-goog-api-key`, 임베드 매개변수(`playsinline`·`rel`·`enablejsapi`·`origin`). (3) 뜻이 바뀐 조항이 하나라도 있으면 "막힘"으로 보고하고 T2를 시작하지 않는다. 없으면 `docs/research/video-sources.md` 에 다시 확인한 날짜 한 줄. (4) HTTP 요청의 User-Agent는 저장소 주소(https://github.com/ericmschoi/The-World-That-Day)만, 이메일 없음. 키·로그인·약관 동의 없음 |
| T1 | YouTube 설정·키·웹 origin·TTL 상한 (`feat(config)` 커밋) | backend-dev | T0c | (1) 위 설정 표의 `youtube.*` 설정 클래스와 `app.web-origin`: 기본값이 표와 같다. 표의 검사를 어기는 값마다 시작 실패·오류 글에 키 이름(설정 시험: 채널 ID `UC123`, 빈 채널 목록, 예산 −1·101·10001, `max-results` 0·51, 정규식 `(`, origin `http://localhost:5173/`·`http://localhost:5173/app`·`ftp://x`·빈 값). (2) `application.yml` 에 `youtube.*` 블록과 주석, `media.allowed-video-embed-providers: [youtube]`, `media.search-cache.ttl.youtube: 7d`. `ttl.youtube` 가 `31d` 면 시작 실패(오류 글에 `media.search-cache.ttl.youtube`), `30d` 면 뜬다. 머지된 `SearchCacheProperties`·`media.*` 설정 시험이 단언을 바꾸지 않고 통과. (3) 키가 비면 시작 때 WARN 정확히 한 줄 `YOUTUBE_API_KEY is empty: YouTube videos are disabled`. 가짜 키 `test-key-not-real` 로 띄운 컨텍스트의 로그(DEBUG 포함)·설정 객체 `toString`·바인딩 오류 글에 그 문자열이 없다. (4) 루트 `.env.example` 에 `# YOUTUBE_API_KEY=` 와 설명 주석(값 없음, `VITE_` 없음) — `LocalDatabaseComposeTest`·`CopiedEnvExampleDefaultsTest` 통과. 시험 `config/application.yml` 에 `youtube.api-key: ""`, `youtube.api-url: http://localhost:9/youtube/v3` — `YOUTUBE_API_KEY=test-key-not-real` 속성을 준 컨텍스트에서도 `youtube.api-key` 가 빈 값임을 시험한다(루트 `.env` 에 진짜 키가 있어도 시험이 Google에 닿지 않는다). (5) 아직 흐름에 연결하지 않는다 → 기존 시험 단언·응답·요청 수 그대로. (6) `cd server && ./gradlew check` 성공. 보고서에 누적 S1 |
| T2 | YouTube API 클라이언트 (`feat(media)` 커밋) | backend-dev | T1 | (1) `search(channelId, q)`: `GET <api-url>/search` 에 규칙 표의 매개변수(값까지 WireMock으로 단언) → 영상 ID 목록(응답 순서). `videos(ids)`: `GET <api-url>/videos?part=snippet,status,contentDetails,recordingDetails&id=<쉼표 목록>` → 항목 목록. ID가 50개를 넘으면 부르지 않고 예외(나누기는 T5). (2) 키는 `x-goog-api-key` 헤더로만 — 모든 요청에 그 헤더가 있고 쿼리에 `key=` 가 없다(WireMock 단언). User-Agent에 이메일이 없다. (3) 연결 타임아웃 `youtube.connect-timeout`, 읽기 타임아웃 `media.lookup-deadline`, 1회만(재시도 없음): WireMock 지연이 `lookup-deadline` 보다 길면 `lookup-deadline` + 0.5초 안에 실패 결과, 요청은 1번. (4) 결과는 성공 / 쿼터(403이고 `error.errors[].reason` 이 `quotaExceeded`) / 실패(그 밖의 403 `forbidden`·`rateLimitExceeded`, 400, 404, 500, 503, 깨진 JSON, 연결 실패, 시간 초과) 셋으로 가른다 — 경우마다 시험. (5) 실패·쿼터마다 WARN 정확히 한 줄 `YouTube call failed: call=<search|videos> cause=<…>`. 로그 캡처(DEBUG 포함)에 가짜 키·검색어(합성 사건 제목)·영상 ID·채널 ID·요청 주소가 없다. (6) 요청 중 스레드가 인터럽트되면(WireMock 지연 5초) 0.5초 안에 끝난다. (7) 합성 fixture `server/src/test/resources/fixtures/youtube/` + `README.md`(근거 문서 URL [85][86][87][88][91], "공식 문서 필드 구조로 만든 합성 값, 실제 응답·실제 영상 ID 없음"). (8) 아직 연결하지 않음. `./gradlew check` 성공. 보고서에 누적 S1 |
| T3 | ★ 쿼터 두 버킷 (`feat(media)` 커밋) | backend-dev 또는 PM (PM 결정 2) | T1 | (1) 버킷 `SEARCH`(상한 `youtube.daily-search-budget`)·`UNITS`(상한 `youtube.daily-unit-budget`). 예약 `reserve(bucket, n)` 은 전부 아니면 없음: 쓴 수 + n ≤ 상한이면 참이고 n을 더한다. 아니면 거짓이고 아무것도 더하지 않는다. (2) 소진 표시 `exhausted(bucket)` 뒤에는 그 버킷 예약이 다음 초기화까지 거짓. (3) 태평양 날짜(`America/Los_Angeles`, 주입한 `Clock`)가 바뀌면 두 버킷의 쓴 수와 소진 표시가 지워진다 — "쿼터" 절의 경계 시각 8개 모두 시험(바뀌기 1초 전은 같은 날, 그 시각은 새 날). (4) 동시성: 상한 80에서 200개 스레드가 1씩 예약하면 참이 정확히 80, 쓴 수 80. (5) 상한 0이면 늘 거짓. 79를 쓴 뒤 `reserve(SEARCH, 1)` 은 참, `reserve(SEARCH, 2)` 는 거짓이고 쓴 수 그대로. (6) 그 태평양 날에 버킷마다 처음 상한에 닿거나 소진 표시가 될 때만 WARN 한 줄 `YouTube quota spent: bucket=<search|units>`(같은 날 두 번째는 없음, 날짜 없음). (7) 저장은 메모리뿐 — DB 테이블·파일 없음. 새 인스턴스는 0부터(클래스 설명에 "다시 켜면 0부터, 403 처리가 뒤를 받친다"). (8) 아직 연결하지 않음. `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 1 (T1~T3 누적 500 넘으면 T4 전에 멈추고 PM 결정 1 (b)로 나눌지 묻는다)** |
| T4 | 확인·버림 규칙과 레코드 만들기 (`feat(media)` 커밋) | backend-dev | T2 | (1) `videos.list` 항목 하나 + `EventContext` + 날짜 → 후보 하나 또는 버림 이유. 규칙 표의 "버림" 줄마다 합성 항목으로 시험(임베드 불가, `private`·`unlisted`, `blocked: [KR]`, `allowed: [JP]`, `ytAgeRestricted`, 다른 채널 ID, ID 10자, 컬러 복원 두 형태, 차단 목록 `id`·`channel`, R1~R9 위반). 버린 수는 DEBUG에 이유별 개수만. (2) 레코드가 규칙 표 "레코드 값"과 필드마다 같다 — 위 계약 예시를 만드는 합성 항목에서 `MediaItem` JSON이 예시와 키·값이 모두 같다(`retrieved_at` 은 `MutableClock`, `place_ref` 는 사건 값). 응답 어디에도 `link_only` 없음. `duration`: `PT1M57S` → 117.0, `PT10M1S` → 601.0, `P0D`·없음·깨짐 → null. 설명 첫 줄 200자 자름, 빈 설명 → null. (3) `embed_url` 이 형식과 정확히 같고 `autoplay`·`mute` 가 없다. `app.web-origin` 을 바꾸면 `origin` 값이 따라 바뀐다(URL 인코딩). (4) 후보: `official` true, `eventLink` `NONE`, `explicitDates` 빈 목록, `basis` `YOUTUBE_OFFICIAL`, `origin` = `MediaOrigin("youtube", ID, null, null, 채널 ID)` (null 아님 — `SearchCacheFormat.write` 가 거부하지 않는다). (5) 촬영 시점: `recordingDate` 가 사건 날짜 이하면 그 값, 늦으면 다음 재료. 제목 「1986 서울아시안게임 개막식」 → `1986` `year`. 제목에 날짜가 없고 설명에 「1986년 9월 20일」 → `1986-09-20` `day`. 설명에만 「2023년 복원」(늦음) → null. 회고 제목 「1986년 9월 20일 오늘 #제10회 #아시안게임 #개막」 → `captured_at` null, `dateTextExcluded` false. 대한뉴스 제목 「대한뉴스 제 1715호-서울 올림픽 개막」(설명 "제작일: 1988-09-17") → `captured_at` null, `dateTextExcluded` true. `publishedAt` 은 어디에도 쓰이지 않는다(값을 바꿔도 결과 같음). (6) "데모·표본 날짜" 표의 자동 테스트 행 중 점수·붙음 행이 `VideoAccuracy` 와 함께 통과(1986 클립 60 붙음, 1715호 50 안 붙음, 예행연습·카운트다운·발사·50주년·601초 안 붙음). (7) 아직 연결하지 않음. `./gradlew check` 성공. 보고서에 누적 S1 |
| T5 | 출처 연결·캐시 30일·키 없을 때 끄기 (`feat(media)` 커밋) | backend-dev | T2, T3, T4 | (1) **출처** (`EventVideoSource`, key `youtube`): 검색어가 있는 사건 × 허용 채널만큼 `SEARCH` 를 예약하고, 모자라면 요청 0으로 `QUOTA_SKIPPED`. 1단계 검색을 안쪽 `Deadline` 에서 동시에 → 하나라도 실패면 `FAILED`, 쿼터면 `SEARCH` 소진 표시 + `QUOTA_SKIPPED`. ID가 없으면 `videos.list` 0회로 `OK`(사건마다 빈 목록). 있으면 `ceil(ID/50)` 단위를 예약(모자라면 `QUOTA_SKIPPED`) → 2단계 동시에 → 실패면 `FAILED`, 쿼터면 `UNITS` 소진 표시 + `QUOTA_SKIPPED`. 사건마다 그 사건 검색 순서대로 T4 후보. 안쪽 작업은 출처가 끝나거나 인터럽트되면 닫힌다(인터럽트 시험). (2) **키 없음**: `youtube.api-key` 가 비면 `VideoSearch` 가 그 출처의 작업도 캐시 조회도 하지 않는다(가짜 저장소 호출 0, WireMock 요청 0). 응답이 `main` 과 같다(기존 세계 API 시험이 단언을 바꾸지 않고 통과). 방법(예: `EventVideoSource` 에 기본 참인 `enabled()`)은 보고서에. (3) **세계 API 끝까지**(캐시 켠 전용 시험 클래스, Wikimedia·YouTube 모두 WireMock): 합성 1986-09-20에서 `video_embed` 하나 — 위 계약 예시의 키가 모두 있고 `selection {youtube_official, 60}`, `provider` `youtube`. 같은 날짜 두 번째 호출은 YouTube 요청 0이고 응답 JSON이 같다(`retrieved_at` 포함). 합성 2002-06-22(검색 0건): 검색 1회·`videos.list` 0회, 두 번째 호출 요청 0(0건 캐시). (4) **쿼터 행**(아래 표): 검색 79회 쓴 상태에서 사건 1개 날짜 → 검색 1회, 80회 → 요청 0·응답 200·`partial` false·캐시 줄 없음. 사건 3개 날짜에 78회 → 요청 0(부분 검색 없음). 403 `quotaExceeded` → `partial` false·줄 없음·그 뒤 태평양 자정까지 검색 0, 자정 뒤(`MutableClock`) 다시 검색. (5) **30일 지우기**: `source = 'youtube'` 이고 `fetched_at + 29일 ≤ 지금` 인 줄을 지우고, 29일 1초 전 줄과 `commons` 줄은 남긴다(Testcontainers + `MutableClock`). 서버 시작 직후 한 번 돈다(컨텍스트 시험), 그 뒤 1시간 간격(설정 또는 상수, 보고서에). 키가 비어도 돈다. (6) **캐시 쓰기 실패**: `SearchCache.Session.write` 가 한 번 실패하면 같은 조회의 나머지 쓰기(다른 출처 포함)를 건너뛴다 — 두 출처 가짜 저장소 시험에서 쓰기 시도 1번, WARN 한 줄. M3a4c 캐시 시험(`VideoSearchCacheTest` 등)이 단언을 바꾸지 않고 통과. (7) **실패·시간**: YouTube 500 → `partial` true·줄 없음·다음 호출이 다시 부름. YouTube 지연이 `lookup-deadline` 보다 길면 미디어 구간이 `lookup-deadline` + `CANCEL_GRACE` + 0.5초 이하, 사진 두 경로가 늦어지지 않고(`WorldStoryEventPhotoPartsRegressionTest` 방식), `partial` true, 줄 없음. (8) 차단 목록 `source: youtube` 의 `id`·`channel` 이 새 결과와 캐시에서 되살린 결과 모두에서 영상을 뺀다. (9) `tragic` 사건(합성)과 1950-06-25·2014-04-16 → YouTube 요청 0. 1995-03-15(사건 없음) → 요청 0. (10) 응답 어디에도 `link_only` 없음, YouTube 레코드가 R5·R9 통과. (11) `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 2 (1,000 넘으면 알림)** |
| T6 | 개인정보처리방침 문장 현재형과 시행일 (`docs(privacy)` 커밋, 머지하는 날) | frontend-dev | T8 (PM이 PR을 승인하고 머지 날짜를 정함) | (1) `web/privacy.html` 의 "지금은 한국어 위키백과, Wikidata, Wikimedia Commons(날짜별 사진 분류)이고, YouTube Data API가 더해질 예정입니다." 를 "지금은 한국어 위키백과, Wikidata, Wikimedia Commons(사진·영상), YouTube Data API(KTV 아카이브 채널 영상 검색)입니다." 로 바꾼다(Commons 괄호는 M3a4b 뒤 영상도 찾으므로 함께 고친다. PM이 T8에서 다른 문구를 주면 그 문구로). (2) `시행일: 2026년 10월 16일` 을 머지하는 날로. (3) `web/src/privacy/privacy.test.ts` 의 문장·시행일 두 단언만 새 값으로 바꾼다. 다른 단언(바깥 URL 목록 C7, 연도 패턴이 `2026년` 하나)은 그대로 통과. (4) `cd web && npm run lint && npm run format:check && npm run typecheck && npm test` 성공. (5) 커밋 하나. 그날 머지하지 못하면 머지하는 날 시행일만 한 줄 더 커밋 |
| T7 | 완료 기준 검증과 전체 테스트 | test-engineer | T5 | (1) T0c~T5 기준 표(기준마다 시험 이름). 빠진 기준은 시험을 더한다(제품 코드는 고치지 않음). (2) `cd server && ./gradlew check` **2회** 반복 안정. 쿼터 동시성·시간(창·타임아웃·인터럽트)·캐시 DB 실패 시험이 든 클래스는 **3회**. (3) C2 출력 없음. (4) S1·S2를 파일별로 보고. (5) 새 시험 모두 `findAllUnmatchedRequests` 빈 목록이고, 어떤 시험도 `googleapis.com`·`youtube.com` 에 연결하지 않는다(시험 설정 주소). (6) 로그 캡처(DEBUG 포함)에 가짜 키·검색어(사건 제목)·영상 ID·채널 ID·날짜가 없다. (7) **M3a4c 리뷰 권장 1**: `server/src/test/java/io/github/ericmschoi/theworldthatday/story/world/WorldStoryEventPhotoPartsRegressionTest.java` 153~161줄의 붙어 있는 javadoc 두 개 중 "four fixed labels" 블록을 `VIDEO_WARNING`(163줄) 바로 위로 옮긴다 — 주석만, 단언 그대로(`test(story)` 커밋). (8) 기존 시험 단언 변화 목록(`git diff main...HEAD -- server/src/test`) — T1 시험 설정과 (7)만 허용. (9) 결과 표: 데모·표본 8날짜(합성 fixture)마다 첫·두 번째 호출의 YouTube 검색·`videos.list` 수, 붙은 영상, `captured_at` |
| T8 | PM 품질 확인 (PR 검수 때) | PM | T7, T0 Y5 | (1) 루트 `.env` 에 `YOUTUBE_API_KEY`(있음)와 저장소 주소 `WIKIMEDIA_USER_AGENT`. `docker compose up -d db`, `cd server && ./gradlew bootRun` — 시작 로그에 `YOUTUBE_API_KEY is empty` 가 없다. (2) 아래 P1로 데모 3 + 표본 5 날짜를 뽑아 "데모·표본 날짜" 표의 PM 열과 대조: 허용 채널 밖 영상 없음, 엉뚱한 영상 없음(예행연습·카운트다운·발사·다른 노선·컬러 복원), 날짜가 적히지 않은 영상은 `captured_at` null, `embed_url` 에 `autoplay`·`mute` 없음. (3) P1을 한 번 더 돌려 결과가 같다(캐시). (4) P2로 `youtube` 줄이 있고 요청자 정보 열이 없다. (5) 콘솔에서 그날 검색 사용량이 예상(새 날짜마다 사건 수, 이 확인으로 12회 이하) 안인지. (6) 다른 날짜로 찾아보지 않는다. (7) T6 문구를 확인하고 머지 날짜를 정한다 |

**머지 뒤 (`main`)**: PM이 M3b3c T6 — 실제 YouTube(KTV 아카이브) 영상으로 화면 확인(nocookie + IFrame API 조합 포함). 1986-09-20 표본 버튼이 대상이다. 1988-09-17 대한뉴스는 M3a5b 뒤. 나머지 날짜에 YouTube 영상이 없으면 그것이 정상이다.

```
# C2 — 키 값이 코드·설정 파일에 없는지. 출력 없음 (값은 .env 에만)
grep -rnE 'AIza[0-9A-Za-z_-]{20,}' server/src web/src .env.example

# S1 — 제품 코드·설정 줄 수 (M3a4c와 같은 명령)
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

담당별: backend-dev 5 (T1~T5, ★ T3은 PM 결정 2), data-researcher 2 (T0b 완료, T0c), frontend-dev 1 (T6), test-engineer 1 (T7), PM 2 (T0, T8). 모두 11개. 머지 뒤 PM의 M3b3c T6은 그 계획의 작업이다.

## 테스트 방침

- **외부 네트워크를 부르지 않는다.** YouTube 응답은 **공식 문서 필드 구조로 만든 합성 fixture**다(#12 (a) — Google 응답은 녹화하지 않는다. 3.5절 값은 fixture 모양의 참고로만 쓰고 원문·실제 영상 ID를 넣지 않는다). fixture 옆 `README.md` 에 근거 문서 URL. 합성 영상 ID는 `Abc123xyz_0` 처럼 11자, 합성 채널 ID는 시험이 `youtube.allowed-channels` 로 준다(설정 시험 하나만 `application.yml` 의 KTV 아카이브 ID를 본다).
- 테스트는 키를 쓰지 않는다(가짜 키 문자열 `test-key-not-real` + WireMock 주소). 시험 `config/application.yml` 이 YouTube를 꺼 두어, 루트 `.env` 에 진짜 키가 있어도 기존 시험이 Google에 닿지 않는다(T1 (4)).
- 쿼터 초기화는 `MutableClock`(태평양 자정, 서머타임 시작·끝 날 포함). 시간·창 시험의 상한은 설정값과 `Deadline.CANCEL_GRACE` 에서 계산한다.
- 저장소·지우기는 Testcontainers(Docker 필요).
- 반복: T7은 `./gradlew check` **2회**, 쿼터 동시성·시간·DB 실패 시험이 든 범위는 **3회**.
- 날짜는 데모 입력 3개(1988-09-17, 2002-06-22, 1995-03-15)와 표본(1950-06-25, 1969-07-20, 1986-09-20, 2000-02-29, 2014-04-16 — M3-P13의 공개 역사 사건 날짜·경계값)뿐이다. 실제 사람의 생년월일이 아니다. 쿼터 시계의 2026년 경계 시각은 이야기 날짜가 아니다. 시험용 `tragic` 사건은 지어낸 문장이다.
- 실제 결과 품질은 PM 품질 확인(T8).

## 데모·표본 날짜로 확인할 것

| 날짜·경우 | 자동 테스트 (합성 fixture) | PM 품질 확인 (T8) |
|---|---|---|
| 1986-09-20 | 허용 채널·임베드 가능·제목에 "1986"·붙여 쓴 "서울아시안게임" → `video_embed`, 점수 60, `captured_at` "1986" `year`, 계약 예시의 키 모두. 같은 검색의 「대한뉴스 제 1610호-아시안 게임 준비」·「… 카운트 다운」 → 안 붙음 | 3.5절 Y2 1위 개막식 클립 같은 영상이 붙는지. 화면은 머지 뒤 M3b3c T6 |
| 1988-09-17 | 「대한뉴스 제 1715호-서울 올림픽 개막」(설명 "제작일: 1988-09-17") → 날짜 재료가 없어 50점, **안 붙음**(M3a5b에서 붙음). 「…(종합)예행연습」 → 안 붙음. 허용 채널 밖(같은 이름 다른 채널 ID) → 버림. `embeddable` false / `private` / KR 차단 / 연령 제한 → 버림, 601초 → 안 붙음 | YouTube 영상이 없는 것이 정상(1988 영상은 M3a5b 뒤) |
| 1969-07-20 | 「아폴로 11호 발사 [1969]」 → 다른 단계 −40으로 안 붙음. 제목 "50주년" → 안 붙음 | 결과 표 |
| 컬러 복원 | 「[📺컬러 복원] 해외 소식 \| … [대한뉴스 698호] 📽️1968년 11월 2일 방영」 → 후보에서 빠짐(점수 계산 전) | - |
| 회고 편집물 | (설정에 시험용 채널을 더해) 「1986년 9월 20일 오늘 #제10회 #아시안게임 #개막」 → 붙고 `captured_at` null | - |
| 2000-02-29 | 「서울 지하철 2호선 개통」 등 → 사건어 불일치(숫자 핵심어)로 0건 → 사진만 | 기준표와 대조 (3.5절: 0건) |
| 2002-06-22 | 허용 채널 결과 0건 → `videos.list` 0회, 0건 캐시, 사진만 | 기준표와 대조 (3.5절: 0건, 설계대로) |
| 1950-06-25, 2014-04-16 | `tragic` → YouTube 요청 0 | 같음 |
| 1995-03-15 | 사건 없음 → 요청 0 | 같음 |
| 쿼터 | 검색 79회 쓴 상태 → 사건 1개 날짜 1회 호출. 80회 → 호출 0·응답 200·`partial` false·캐시 줄 없음. 사건 3개 날짜에 78회 → 호출 0. 403 `quotaExceeded` → 그 뒤 초기화까지 호출 0. 태평양 시간 자정이 지나면 다시 호출 | 콘솔 사용량이 예상 안 |
| 키 | 비면 요청 0·캐시 조회 0·WARN 1줄. 요청에 `x-goog-api-key` 헤더가 있고 쿼리에 `key=` 가 없다. 로그·`toString` 에 키 값이 없다 | 시작 로그에 키 경고 없음 |
| 임베드 주소 | `autoplay`·`mute` 없음, `origin` 이 설정값, 잘못된 ID는 버림 | `embed_url` 확인 |
| 캐시 | 두 번째 호출 요청 0·응답 같음, 29일 지난 `youtube` 줄 지워짐, `commons` 줄 그대로 | P1 두 번, P2 |

같은 꼭지 여러 ID(R-N7) 행은 M3a5b로 옮겼다(대한뉴스 꼭지라 이 기능에서는 붙지 않음).

## 크기 (400줄 규칙)

S1 = 제품 코드·설정(`server`, 테스트 제외, M3a4c와 같은 명령). 루트 `.env.example`(약 3줄)과 웹 `privacy.html`(약 2줄)은 S1에 들어가지 않는다.

### 실측 비율

| 기능 | 파일별 셈 | 실제 S1 | 비율 |
|---|---|---|---|
| M3a3d (PR #22) | 약 234 | 312 / −72 | ×1.33 |
| M3a4 (PR #23) | 약 450 | 953 | ×2.1 |
| M3a4b (PR #24) T0~T3 | 약 405 | 954 / −222 | ×2.36 |
| M3a4b (PR #24) 최종 | 약 405 | 1,155 / −236 (계획에 없던 영상 슬롯 수정 포함) | ×2.85 |
| M3a4c (PR #25) | 약 323 | **583 / −23** (확인점 T1 뒤 181, T3 뒤 580) | **×1.8** (기대값 ×2.0 약 650보다 작음) |

### 다시 셈 (머지 코드 대조)

2026-10-12 계획의 셈(약 220, 보정 약 350)은 머지 코드 이전 것이다. 머지 코드를 보고 파일별로 다시 셌다. 새로 생긴 것: 키 없을 때 끄기, 30일 상한과 지우기, 쓰기 실패 `down`, `videos.list` 나누기, 전부 아니면 없음 예약, 설정 검사. 빠진 것: 같은 꼭지 여러 ID(→ M3a5b), M3a5b 조회 지점, 어댑터의 길이 버림.

| 작업 | 파일별 셈 | 셈 | ×1.8 | ×2.0 (기대) | ×2.36 |
|---|---|---|---|---|---|
| T1 | YouTube 설정 클래스(8개·검사, 약 40) · `app.web-origin` 과 검사(약 12) · `application.yml` YouTube 블록·주석·제공자·TTL(약 29) · TTL 30일 상한(약 6) · 키 없음 WARN(약 5) | 약 92 | 약 166 | 약 184 | 약 217 |
| T2 | 요청 두 가지·매개변수·헤더(약 30) · 응답 읽기(약 15) · 결과 가르기(`quotaExceeded`)·WARN(약 20) · 클라이언트 만들기(타임아웃, 약 8) | 약 73 | 약 131 | 약 146 | 약 172 |
| T3 ★ | 두 버킷·전부 아니면 없음 예약·소진 표시·태평양 날짜·하루 한 번 알림 | 약 45 | 약 81 | 약 90 | 약 106 |
| T4 | 버림 규칙 6개(약 20) · 컬러 복원·대한뉴스·회고 정규식(약 15) · 촬영 시점 차례(약 12) · 임베드 주소·ID 검사(약 8) · 레코드(약 25) · 길이·설명 첫 줄(약 10) · 거르기·정규화·origin(약 8) | 약 98 | 약 176 | 약 196 | 약 231 |
| T5 | `find` 두 단계·예약·상태(약 60) · 사건별 후보(약 15) · 키 없을 때 끄기(약 4) · 쓰기 실패 `down`(약 2) · 30일 지우기(저장소 삭제·주기 실행, 약 25) | 약 106 | 약 191 | 약 212 | 약 250 |
| T6·T7 | 웹·시험만 — S1 0 | 0 | 0 | 0 | 0 |
| **합계** | | **약 414** | **약 745** | **약 830** | **약 975** |

- 비율은 M3a4c ×1.8(새 외부 호출 없음)과 M3a4b ×2.36(새 외부 어댑터) 사이 가운데인 **×2.0, 기대값 약 830 (745~975)** 으로 잡았다. 이 기능은 새 외부 어댑터라 M3a4c보다 크게 늘 수 있고, 검색 틀·캐시가 이미 있어 M3a4b보다는 덜 늘 것으로 본다. ×2.85(M3a4b 최종)는 쓰지 않았다 — 계획에 없던 슬롯 수정이 들어 있던 값이다.
- T1~T3 누적 기대값 약 420 (380~495), T4·T5 약 410 (365~480). 두 덩어리가 비슷해서 확인점 1이 나누는 자리를 겸한다.

### PM 결정 1 (크기가 400을 넘는다)

| 안 | 내용 | 크기 (기대, 범위) | M3 PR 수 |
|---|---|---|---|
| **(a) 한 PR + 확인점 (추천)** | T0c~T8 모두 `feat/official-channel-video`. **확인점 1**: T3 뒤 누적 S1이 500을 넘으면 T4 전에 멈추고 (b)로 나눌지 묻는다(T1~T3이 (b)의 앞 PR과 같아 고칠 것 없이 나눌 수 있다). **확인점 2**: T5 뒤 1,000을 넘으면 알림(멈추지 않음) | 약 830 (745~975) | 22 |
| (b) 두 PR로 나눔 | M3a5 `feat/official-channel-video` = T0c·T1·T2·T3 + 검증(동작 변화 없음 — 설정·클라이언트·쿼터, WireMock 시험) → 새 **M3a5a** `feat/youtube-video-source` = T4·T5·T6 + 검증·T8(영상이 응답과 화면에 나옴). 순서 M3a5 → M3a5a → M3a5b(M3a3 → M3a3a → M3a3b와 같은 이름 방식) | 약 420 (380~495) / 약 410 (365~480) | 23 |
| (c) YouTube 결과를 캐시하지 않음 | `ttl.youtube` 를 두지 않아 30일 상한·지우기·쓰기 실패 `down` 이 빠진다(약 −35, 기대 −70). 대신 같은 날짜를 다시 볼 때마다 검색을 다시 써서 하루에 볼 수 있는 날짜 수가 줄고(새 날짜 26개 → 조회 26번), 10.1절 6번 (a)("채널 한정 검색 + 캐시")와 어긋난다 | 약 760 (680~890) | 22 |

추천 이유 (a): YouTube 영상은 T5에서야 응답에 나온다. (b)의 앞 PR은 눈에 보이는 동작이 없어서 리뷰가 뒤 PR과 같이 봐야 의미가 있다(M3a4c 결정 1 (a)와 같은 이유). PR 한 번이 줄어 검증·리뷰·PM 확인이 한 번씩 덜 든다. 그래도 크기가 규칙의 두 배쯤이므로, T3 뒤 확인점에서 실제 숫자를 보고 (b)로 바꿀 수 있게 두었다. (c)는 줄이 적게 줄고 쿼터 설계의 전제를 깬다.

각 backend-dev 보고서(T1~T5)는 누적 S1을 적는다. 테스트(S2)는 따로 보고한다.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 메모 |
|---|---|---|
| M3a5b 대한뉴스 | YouTube 클라이언트·쿼터(검색 버킷 공유, 80회 상한), 확인·버림 규칙, 대한뉴스 제목 후보(`dateTextExcluded` true, `captured_at` null, 제목 그대로), 캐시(`youtube` 줄 — 대한뉴스를 YouTube 어댑터를 넓혀 붙이면 같은 줄, 따로 출처 `daehan_news` 면 새 TTL과 30일 상한) | 제목 대조(N-e)를 어디에 붙일지(어댑터 확장 / 새 출처)와 같은 꼭지 여러 ID(R-N7)는 M3a5b 계획이 정한다 |
| M3b3c T6 (웹, PM) | 실제 YouTube 영상 | 머지 뒤 `main` 에서 |
| M6b | 쿼터 소진 동작, 세계 섹션 시간 측정 | 로드맵 M6b 그대로 |

## 리스크와 확인할 것

1. **크기** — 기대값 약 830이 400을 넘는다. PM 결정 1과 확인점 두 개로 다룬다.
2. **검색 하루 100회**(위험 B·9) — KTV 아카이브 한 채널, 날짜당 사건 3개, 80% 상한이면 캐시에 없는 새 날짜를 **하루 약 26개** 처리한다. 넘으면 그날 YouTube만 빠진다(정상). PM이 확인 명령을 실행한 날은 검색을 먼저 쓴다. 시간 초과·실패한 검색은 예약을 돌려주지 않고 캐시되지도 않으므로, 느린 날에는 같은 날짜가 검색을 다시 쓴다. 같은 새 날짜를 동시에 두 번 부르면 검색이 두 번 나간다(로컬 데모에서는 드묾). 공개 배포(#5) 때 증설(준수 감사)을 다시 본다.
3. **국내 데모 영상은 M3a5b에 달려 있다** — 1988-09-17의 KTV 아카이브 영상은 대한뉴스 꼭지(제1715호)라서, 회차 날짜를 붙이는 M3a5b가 있어야 기준선을 넘는다. 이 기능만으로 붙는 데모·표본 영상은 1986-09-20 개막식 클립 정도다. M3a5b는 PM 작업(공공데이터포털 활용신청·N1·N2·CSV)이 끝나야 시작한다.
4. **임베드 허용이어도 재생이 막힐 수 있다**(위험 12) — M3b3c가 재생 오류 때 그 항목을 숨긴다(10.1절 22번).
5. **nocookie + IFrame API 조합**(위험 15) — 문서에 없다. 머지 뒤 M3b3c T6 브라우저 확인에서 안 되면 임베드 도메인을 `www.youtube.com` 으로 바꿔야 할 수 있다. 그때 PM에게 묻는다(서버 도메인·웹 `YOUTUBE_EMBED_ORIGIN`·방침 문구·시행일이 함께 바뀜 — 로드맵 후속 후보 "nocookie 대체").
6. **기관 이용 조건**(위험 16, 10.1절 8번) — KTV 아카이브 채널 설명의 "부분발췌, 상업적 이용 등 무단사용을 금합니다"가 확인됐다(3.5절). 결정 (a)(비상업 공식 임베드 + 차단 목록) 그대로이고, 공개 배포 전 KTV 서면 확인이 필요하다(로드맵 후속).
7. **약관 개정**(위험 17) — 2026-09-14 갱신. T0c가 구현 직전에 다시 확인하고, 뜻이 바뀌었으면 T2 전에 멈춘다.
8. **엉뚱한 영상** — 같은 주제 다른 날 회차(1711·1704호 예행연습, 1609호 카운트다운, 1717호 폐회식), 발사일 클립, 다른 노선 개통(3.5절 Y2). M3a4 규칙(다른 단계·숫자 핵심어)과 PM 품질 확인으로 막는다.
9. **데이터 합치기 해석**(10.1절 7번 (a)) — 출처를 구분해 보여 준다. 업로드 목록 전체 색인은 하지 않는다.
10. **쿼터 계수기는 다시 켜면 0** — 같은 태평양 날에 여러 번 다시 켜면 20% 여유를 쓸 수 있다. 구글 한도(100회)는 403 처리가 지킨다.
11. **30일 지우기와 꺼진 서버** — 서버가 꺼져 있는 동안은 줄을 지우지 못하고, 켜자마자 지운다. 로컬 데모라 허용한다. 공개 배포 때 DB 쪽 정리(예: 예약 작업)를 다시 본다.
12. **시험이 진짜 키를 쓸 위험** — `application.yml` 이 루트 `.env` 를 읽으므로, 시험 설정이 YouTube를 꺼 두지 않으면 PM 컴퓨터의 시험이 Google을 부를 수 있다. T1 (4)가 시험 설정과 그 시험으로 막는다.
13. **키 노출** — 헤더로만 보내고 로그·`toString`·오류 글에 값을 넣지 않는다(T1 (3), T2 (2)·(5)). C2 명령으로 코드·설정에 키 모양 문자열이 없음을 본다.
14. **세계 섹션 시간** — 이 기능은 같은 미디어 창 안에서 돌아 최악 시간을 늘리지 않는다("시간과 요청 제한" 절, T5 (6)·(7)). 스텁 약 11.7초 > 웹 10초는 M6b에서 다룬다(지켜보는 위험).
15. **사생활** — YouTube에는 사건 이름(대표 링크 제목)과 채널 ID만 보낸다. 생년월일·장소는 보내지 않는다(방침 "날짜와 사건 이름으로 외부 출처에 자료를 요청"과 같음). 캐시 줄에 요청자 정보가 없고, 로그에 검색어·날짜가 없다.

## 후속 작업 후보 (이 계획 밖, PM이 일정을 정함)

| 후보 | 내용 | 종류·시점 |
|---|---|---|
| 웹 mock `selection.score` | `web/src/mocks/fixtures/world/youtube.ts` 의 `0.9`(소수)를 서버처럼 정수(예 90)로 | 작은 웹 `fix/`. M3a5 머지 전이면 M3b3c T6과 함께 보기 좋다 |
| M3b3c 리뷰 권장 1 | 플레이어 `onReady` 전에 사용자가 누른 재생을 조정자가 모른다 → 그 영상이 화면 밖에서도 계속 재생되고 "한 번에 하나만"에서 빠질 수 있다 | 작은 웹 `fix/`. M3b3c T6(이 기능 머지 뒤) 전에 하면 실제 확인에서 함께 본다 |
| 시험 공용 `noUnmatchedRequest` | 매치되지 않은 WireMock 요청 0을 시험마다 따로 단언하는 것을 공용 `@AfterEach` 하나로 | 작은 `chore/` (시험만) |
| 위키백과 사건 어댑터의 `ActionApiErrors` | `ActionApiKoWikiClient`·`ActionApiKoWikiLinkResolver` 도 M3a3d T0의 공용 `error` 검사로(로그 코드 제한 포함) | 작은 `chore/`, 또는 M2 계획 때 Wikimedia 로그 제목 정리와 함께 |
| `WorldSection.test` describe 이름 | 지금 내용과 맞지 않는 describe 이름 고치기 | 작은 웹 `chore/` |
| 흔들리는 `TwoPageKoWikiEventSourceTest` | 가끔 실패하는 시험의 원인 찾기·고치기 | 작은 `fix/` (시험) |
| `server-ci` 경로 | `web/src/media/types.ts`(서버 DTO와의 대조)와 `data/` 변경에도 `server-ci` 가 돌게 | 작은 `chore/` (CI) |
| M3a4c 리뷰 권장 3 | 캐시 DB 실패 때 "WARN 정확히 한 줄"은 가짜 저장소로만 시험됐다. 실제 Hikari·Hibernate가 더 남기는 줄(자리 표시 값만)을 실제 DB를 끊는 시험으로 확인 | M6b(장애 격리), 또는 작은 `test/` chore |
| 쿼터 계수기 보존 | 다시 켜도 그날 쓴 수가 남게(DB, 마감 작업 밖에서) | 공개 배포(#5) 때 |
| 타임아웃·실패 검색의 쿼터 | 실패한 날짜가 검색을 다시 쓰는 비용 — 측정 뒤 짧은 "실패 기억"이 필요한지 | M6b 측정 뒤 |
| 같은 꼭지 여러 ID (R-N7) | (호수, 꼭지 제목)이 같으면 하나만 | M3a5b (이 개정에서 옮김) |
| 컬러 복원 재업로드를 회차로 쓰기 | 보조 정규식 + "컬러 복원" 캡션 | R-N5, M3a5b 뒤 |

## PM 결정 필요

이미 정해져 다시 묻지 않는 것: M3-P14, 10.1절 5번 (a) KTV 아카이브만(Y2 뒤에도 유지), 6번 (a) 채널 한정 검색 + 캐시·넘으면 건너뜀, 7번 (a), 8번 (a) + 차단 목록, 9번 (a), 22번 (a), 23번 (b), 24번 (a), 대한뉴스 분리(로드맵 D1 (a)), 쿼터 계수기는 메모리(2026-10-12), M3b3c에서 넘어온 3가지(로드맵 메모 — 방침 시행일은 머지하는 날).

| # | 물을 것 | 선택지 | 추천 |
|---|---|---|---|
| 1 | 크기 기대값 약 830(745~975)이 400을 넘는다 — 나눌지 | (a) 한 PR + 확인점 T3 뒤 500(멈추고 (b)를 물음)·T5 뒤 1,000(알림) / (b) 두 PR: M3a5(설정·클라이언트·쿼터) + M3a5a `feat/youtube-video-source`(규칙·연결·방침), M3 PR 23개 / (c) YouTube 결과를 캐시하지 않아 줄임 | **(a)** — 위 "PM 결정 1" 설명 |
| 2 | ★ T3 쿼터 두 버킷(전부 아니면 없음 예약, 태평양 자정·서머타임, 동시성, 주입한 시계)을 PM이 직접 구현할지 | (a) PM이 구현(backend-dev는 그동안 T4, T5는 T3을 기다림) / (b) backend-dev | **(b)** — M3에서 PM이 ★ 작업을 매번 backend-dev로 정했고(M3a3b T2 ~ M3a4c T1), T3은 T5의 선행이라 PM 일정에 묶인다. 직접 해 보려면 T3은 외부 호출·DB가 없는 순수 로직 + 시계 주입이라 따로 떼어 맡기 좋다 |
