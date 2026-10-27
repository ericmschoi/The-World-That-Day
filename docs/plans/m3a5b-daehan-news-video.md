# 대한뉴스 영상 자동 연결 (M3a5b)

> 브랜치: `feat/daehan-news-video` (시작할 때의 `main` 에서 갈라짐 — M3a5a PR #27 머지 124cf31 이후) · 작성: planner · 상태: **막힘 — PM T0 대기.** 색인 경로(N-a)가 아직 "부분 확인"이라 `CLAUDE.md` 7절 2항에 따라 T0(PM)과 T0b(data-researcher, N-a "확인됨")가 끝나기 전에는 T1·T2·T2b를 시작하지 않는다. **PM 재승인 (2026-10-27): 결정 1 (a) YouTube 출처 안의 단계, 2 (a) `videos.list` 제목, 3 (a) UNITS 소진 확인을 T2b에, 4 (a) 한 PR · 확인점 T2 뒤 450(멈춤) · T2b 뒤 650(알림), 5 (b) ★ T2는 backend-dev.** 구현은 T0·T0b가 끝날 때까지 막혀 있다.
> 이력: 2026-10-12 처음 승인(M3a5의 이전 T2를 나눔, 로드맵 D1 (a)). 같은 날 N3 결과와 R-N5·R-N7·R-N8 반영(리서치 3.5절·7절, 6bf0267). **2026-10-26 개정 (PM 결정 (c))**: 계획을 지금 머지된 코드에 맞춰 고치고, T0은 PM이 따로 한다. M3a5(PR #26)와 M3a5a(PR #27)의 머지 코드와 대조해 붙는 자리, 요청 순서, 쿼터, 캐시, 시험 설정, 크기와 작업을 다시 썼다("이번 개정에서 바뀐 것" 절).
> 기준: `docs/research/video-sources.md` 3.3절(V5 카드), 3.5절(N3·Y2·Y4 결과, 제목 변형, [177]), 3절 하루 처리량 표, 4절 V5 표, 5절 V5 표, 6.3절, 7절 V5 추천·R-N1~R-N8·R-Y2, 8절 위험 18~24, 10.1절 8·10·11·12·13번, `docs/plans/m3a4-event-video.md`, `m3a4c-media-search-cache.md`, `m3a5-official-channel-video.md`("규칙"·"쿼터"·"캐시와 30일"), `m3a5a-youtube-video-source.md`(PM 결정 블록 전부), `CLAUDE.md` 7절 1·2·3·5·6·7항
> 선행: **M3a5a 머지 (완료, PR #27)** + **T0 (PM)**: 공공데이터포털 활용신청, N1·N2, CSV 내려받기 N-B1, Y4에 제1611호 ID + **T0b (data-researcher)**: 색인 경로 N-a를 "확인됨"으로 + T1 (content-curator 색인). **지금 상태**: 제목 대조(N-e·N-f의 제목 형식)는 N3로 "확인됨"이고, 색인(N-a)은 "부분 확인"이다(PM 작업 대기).

## 이번 개정(2026-10-26)에서 바뀐 것 — 머지 코드와 대조

| 항목 | 2026-10-12 계획 | 머지 코드(`main`) 확인 결과 → 이 계획 |
|---|---|---|
| 붙는 자리 | "대한뉴스 어댑터"(모양 미정) | YouTube 출처는 `media.youtube.YoutubeVideoSource`(`EventVideoSource`, key `youtube`)다. `VideoSearch` 는 출처들을 **병렬로** 돌려서 한 출처의 결과를 다른 출처가 읽을 수 없다(M3a5 계획 "조회 지점 뺌"). 그래서 N-e(검색 결과 재사용, 추가 요청 0)는 **YouTube 출처 안의 단계**로 붙여야 한다 → PM 결정 1 (추천 (a)) |
| 대한뉴스 제목의 지금 처리 | (없음) | `YoutubeVideoMapper` 는 「대한뉴스 제 N호…」 제목(`^\s*대한뉴스\s*제\s*\d+\s*호`)을 **버리지 않는다**: 후보로 남기되 `captured_at` null, `dateTextExcluded` true, `basis` `YOUTUBE_OFFICIAL` 로 만든다 → 날짜 점수 0, 보통 50점이라 붙지 않는다. 대괄호 형태 「[대한뉴스 698호]」와 "컬러 복원"은 `Drop.COLOR_RESTORED` 로 **버린다**(R-N5). 이 기능은 앞의 것을 대한뉴스 후보로 **바꾸고**, 뒤의 것은 그대로 둔다 |
| N-e가 읽는 제목 | "Y-a 결과의 제목" | 머지된 `YoutubeClient.search` 는 **영상 ID만** 돌려준다(응답의 `snippet.title` 은 버림). 제목은 2단계 `videos.list` 항목에 있다 → PM 결정 2 (추천 (a): `videos.list` 제목으로, 클라이언트 그대로) |
| N-f 결과 확인 | "같은 날짜의 묶음에 더해 `videos.list` 1회" | **대체**: 날짜의 `videos.list` 는 N-e 전에 이미 나갔다(결정 2 (a)). N-f로 새로 나온 ID만 `videos.list` 를 한 번 더 부른다(보통 1단위) |
| 확인·버림 규칙 | "M3a5의 `videos.list` 확인·버림 규칙" | `YoutubeVideoMapper`(M3a5a T4) 그대로 — 임베드·공개·KR·연령·채널 ID·ID 11자·컬러 복원·차단 목록·R1~R9. 길이 600초는 `VideoAccuracy` |
| 키 없음 | "키가 없으면 이 어댑터도 끈다" | `YoutubeVideoSource` 는 `@Conditional(KeySet)` 이라 키가 비면 **빈으로 등록되지 않는다** → 대한뉴스 단계도 함께 없다. 새 코드 없음 |
| 색인 없음 | "WARN 1줄 후 이 어댑터를 끈다" | 대조 부품만 꺼지고 **YouTube 출처는 M3a5a 그대로** 돈다. 그리고 **새로**: 시험 설정은 색인을 조용히 끈다(아래 "시험 설정") — 시험이 저장소 `data/` 를 읽기 때문 |
| 쿼터 | "M3a5 검색 버킷 공유(80회 상한)" | 그대로(`YoutubeQuota` 의 `SEARCH`·`UNITS`). 머지 이름: `reserveOn(bucket, n)` → 예약한 태평양 날짜(거부면 null), `exhausted(bucket, reservedOn)`(날이 바뀌었으면 표시 안 함). `reserve` 는 n < 1 이면 예외(M3a5 결정 3 (a)) → N-f가 없으면 부르지 않는다 |
| 캐시·30일 | (없음) | 결정 1 (a)면 대한뉴스 후보도 **`youtube` 줄**에 저장된다: TTL 7일(`media.search-cache.ttl.youtube`), `SearchCachePurge` 가 29일 지난 `youtube` 줄을 지움, `SearchCache.Session` 은 쓰기가 한 번 실패하면 `down = true`(M3a5a T5 결정 (a)) — **모두 그대로 덮인다**. 저장 형식 v1이 `basis`·`explicit_dates`·`date_text_excluded` 를 이미 담는다 |
| 같은 영상 하나 | "M3a5 결과로도 나오면 레코드 하나(`daehan_news` 우선)" | 같은 출처 안에서 매퍼 후보를 대한뉴스 후보로 **바꾸므로** 합칠 것이 없다. `VideoSearch` 의 같은 ID 한 번·`BASIS_ORDER`(`DAEHAN_NEWS` 맨 앞)는 안전장치로 남는다 |
| 정확도에 넘기는 제목 | YouTube 제목 | **새로**: 대한뉴스 후보의 `title` 은 **꼭지 제목**(호수 머리 뺌)이다. `VideoAccuracy` 는 제목 속 1900~2099 숫자를 "다른 해"로 보고 −40을 준다 — 대한뉴스 1900~2040호(1992~1994년 회차)의 호수가 연도로 읽혀 맞는 영상이 떨어지는 것을 막는다 |
| 시험 설정 | (없음) | **새로**: 시험 `config/application.yml` 이 `daehan-news.index-file` 을 빈 값으로 둔다(로그 없이 끔). 기존 시험 `WorldStoryYoutubeSampleDatesTest.theOlympicsDayAttachesNoNewsreelOrRehearsalAndCachesTheAnswer` 등이 단언을 바꾸지 않고 통과한다. M3a5 T1b(시험 컨텍스트의 시작 WARN이 다른 시험을 순서에 따라 깨뜨림)의 교훈으로, 빈 값일 때는 WARN을 남기지 않는다 |
| M3a5a 리뷰 권장 1 | (없음) | UNITS 버킷이 소진된 날에도 검색을 예약하고 보낸다(그 뒤 `videos.list` 예약이 거부되어 검색이 버려진다). N-f가 검색을 하나 더 쓰므로 여기서 같이 고칠지 → PM 결정 3 (추천 (a)) |
| 크기 | 약 150, 보정 약 240 | 파일별 다시 셈 약 206 × 실측 비율 → **약 515 (370~570)**. 400을 넘어 PM 결정 4 |
| 작업 | T0·T0b·T1·T2·T3·T4 | T2를 둘로: **T2**(★ 색인·대조 부품, 연결 안 함) + **T2b**(YouTube 출처에 연결). T3(검증)·T4(PM 품질 확인) 번호는 그대로 둔다(로드맵이 "M3a5b T4"를 가리킨다) |

## 목표

1. 사건 날짜 d가 **1953-06-23 ~ 1994-12-31** 이면, `data/` 의 대한뉴스 꼭지 색인에서 회차 날짜가 **d ~ d+7** 이고 **꼭지 제목에 사건어가 있는** 꼭지를 찾는다(10.1절 10·12번).
2. YouTube 출처(M3a5a)가 이미 받은 KTV 아카이브 결과에서 제목 맨 앞 "대한뉴스 제 N호"의 호수와 꼭지 제목이 그 꼭지와 같은 영상을 고른다(N-e, 추가 요청 0). 없으면 그 꼭지로 채널 한정 검색을 사건마다 1회 더 한다(N-f).
3. 고른 영상에 회차 날짜를 `approx` 로 붙이고(정직한 날짜: 회차 날짜는 촬영일이 아니다, R-N3), 캡션 출처에 **"대한뉴스(정부 제작 뉴스영화)"** 를 적는다(10.1절 13번).
4. 재생은 **KTV 아카이브 YouTube 임베드로만** 한다. e영상역사관 플레이어·페이지 iframe은 쓰지 않는다.
5. 세계 섹션 최악 시간을 늘리지 않는다(같은 YouTube 작업, 같은 안쪽 마감).

## 범위 밖

- e영상역사관 플레이어·페이지 iframe(공식 임베드 없음), e영상역사관 정보 조회 API(N-c), 목록 페이지 긁기
- 서버가 odcloud API로 색인을 받는 방식(N-b) — 10.1절 11번 (a)로 쓰지 않음. N1·N2는 값 형식 확인용
- 색인에서 꼭지를 찾았지만 YouTube 영상이 없을 때 링크만 보여 주기(`CLAUDE.md` 7절 1항 — 레코드를 만들지 않음)
- e영상역사관 꼭지 내용 요약 문단 옮기기(위험 24)
- **컬러 복원 재업로드**(「[📺컬러 복원] … [대한뉴스 698호] 📽️1968년 11월 2일 방영」) — `YoutubeVideoMapper` 가 `COLOR_RESTORED` 로 버린다(R-N5). 보조 정규식 `\[대한뉴스\s*(?:제\s*)?(\d+)\s*호\]` 와 "컬러 복원" 캡션은 후속 후보
- 허용 채널 밖 YouTube(M3-P14 — KTV 아카이브만), 쿼터 증설, 쿼터 계수기 DB 보존
- 웹 변경(타입 `daehan_news` 와 `approx` 표시는 이미 있다 — M3b1·M3b3c). 영상 슬롯 정책(M6b)
- 영상이 없는 날짜에 다른 날짜로 찾아 보이게 하는 것(PM 피드백 2026-10-16)

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| 대한뉴스 CSV(공공데이터포털 "한국정책방송원_영상역사관 대한뉴스관" 파일데이터, data.go.kr/data/15050744) ([video-sources.md](../research/video-sources.md) 3.3절 V5, 3절 종합 경로 표 N-a) | **부분 확인** — N1·N2·N-B1 뒤 "확인됨"(T0b). **"확인됨" 전에는 T1·T2·T2b를 시작하지 않는다** | PM이 한 번 받은 파일로 만든 꼭지 색인(`data/daehan-news/`) | 이용허락범위 "제한 없음"(해석은 10.1절 11번 (a), archive-media.md D3). 원본 파일은 저장소에 넣지 않는다. 출처·기준일 기록 |
| YouTube Data API `search.list`·`videos.list` (V2, M3a5·M3a5a) | **확인됨** (6bf0267, 약관·쿼터 다시 확인 b3f6be6). KTV 아카이브 대한뉴스 제목 형식은 N3로 "확인됨"(N-e·N-f) | N-e: YouTube 출처의 2단계 결과 재사용(추가 0). N-f: 채널 한정 검색 1회 + 새 ID `videos.list` | 키는 `x-goog-api-key` 헤더로만(M3a5 T2), 메타데이터 30일 안에 지움(III.E.4.d — `SearchCachePurge`), 영상 저장 금지(III.E.1.a). 검색 버킷 공유(80회 상한) |
| KTV 아카이브 YouTube 임베드 (V4) | **확인됨** | 재생(`youtube-nocookie.com/embed`, M3a5a 매퍼 형식 그대로) | M3b3c(완료). KTV 채널 설명의 이용 조건은 10.1절 8번 (a) + 차단 목록 |

## 이어받는 부품 (`main` 에서 확인한 이름)

| 부품 | 이름·모양 | 이 기능에서 |
|---|---|---|
| YouTube 출처 | `media.youtube.YoutubeVideoSource`(key `youtube`, `@Conditional(KeySet)`): 검색(사건 × 허용 채널) → `videos.list`(50개씩) → 사건마다 `YoutubeVideoMapper.candidates(videos, event)`. 단계마다 `quota.reserveOn` 먼저, 같은 안쪽 `Deadline.after(media.lookup-deadline)`. 상태 가르기 `collect(…)`: 실패·시간 초과 → `FAILED`, `QuotaExceeded` → `exhausted(bucket, reservedOn)` + `QUOTA_SKIPPED` | N-e·N-f 단계를 더한다(T2b) |
| 매퍼 | `YoutubeVideoMapper.map(JsonNode, EventContext)` → `Result(candidate, drop)`, `KEY = "youtube"`, `Drop` 9가지, 레코드 값(M3a5 "규칙" 표) | **고치지 않는다.** 대한뉴스 단계가 매퍼 후보를 받아 바꾼다 |
| 클라이언트 | `YoutubeClient.search(channelId, q)` → `Response<List<String>>`(ID만), `videos(ids)`(≤ `MAX_IDS` 50), `Ok`/`QuotaExceeded`/`Failed` | 결정 2 (a)면 그대로 |
| 쿼터 | `YoutubeQuota`: `Bucket.SEARCH`·`UNITS`, `reserve`(n < 1 예외), `reserveOn`, `exhausted(bucket)`, `exhausted(bucket, reservedOn)`, `used(bucket)`, 태평양 자정 초기화, 메모리 | N-f 예약. 결정 3 (a)면 읽기 전용 확인 메서드 하나 |
| 검색·후보 | `VideoSearch`(출처 병렬, 사건마다 하나, 같은 ID 한 번, `BASIS_ORDER` 첫째 `DAEHAN_NEWS`), `Candidate`(10요소 — `explicitDates`·`dateTextExcluded`·`basis`·`origin`), `Selection.Basis.DAEHAN_NEWS` → `"daehan_news"`, `EventVideoSource.Status { OK, FAILED, QUOTA_SKIPPED }` | 그대로 |
| 정확도 | `VideoAccuracy`: 날짜 점수(같은 날 40·같은 달 20·같은 해 10 — `originalCapture`·`explicitDates`·`dateTextExcluded` 가 아니면 글 속 날짜), 사건어 30, 공식 20, 다른 해 −40(`otherYear`, 제목만), 기념 −30, 다른 단계 −40, `DURATION` 600초, `min-score` 60 | 사건어 판정만 하는 **공개 메서드 하나**를 더한다(같은 `Prepared` 를 씀, 규칙 복사 없음 — T2 (2)) |
| 캐시 | `SearchCache`(`Session.read`/`write`, 실패 뒤 `down`), `SearchCacheFormat` v1, `SearchCacheProperties`(`ttl.youtube` 30일 상한), `SearchCachePurge`(`SOURCE = "youtube"`, 29일, 시작 때 + 1시간마다) | 결정 1 (a)면 **그대로** |
| 설정·데이터 | `config.AppProperties`(`app.data-dir`, 기본 `../data`), 큐레이션 읽기 방식 `event.tone.EventCuration`(시작 때 읽고 형식이 틀리면 시작 실패) | 같은 방식으로 색인을 읽는다 |
| 시험 기반 | `YoutubeWorldTest`·`YoutubeItems`(`item`, `titled`), `fixtures/youtube/`, `MutableClock`, 시험 `config/application.yml`(YouTube 끔, 주소 `localhost:9`) | 재사용·확장 |

> backend-dev는 T2·T2b 보고서 첫 줄에 "확인한 이름"을 적고, 위 이름과 다르면 이 계획서를 고치게 알린다. 새 부품 이름(아래 `DaehanNewsIndex`·`DaehanNewsMatcher`·`DaehanNewsProperties`, 패키지 `media.daehannews`)은 예시이고 backend-dev가 정한다.

## 붙는 자리 (설계 — PM 결정 1·2)

**결정 1 (추천 (a))**: YouTube 출처 안의 단계 + 따로 떼어 둔 대조 부품.

- `DaehanNewsIndex`(색인 읽기·창 조회)와 `DaehanNewsMatcher`(꼭지 고르기, 제목 대조, R-N6·R-N7·R-N8, 대한뉴스 후보 만들기)는 **외부 호출이 없는 순수 부품**이다(T2). YouTube 출처는 이 부품을 선택적으로 받는다(색인이 꺼져 있으면 없음 → M3a5a 동작 그대로).
- 외부 출처마다 어댑터 하나(`CLAUDE.md` 7절 3항)는 지켜진다: 외부 호출은 YouTube 하나이고 그 어댑터는 `YoutubeVideoSource` 하나다. 색인은 우리 `data/` 파일이다.

| 안 | 내용 | 장점 | 단점 |
|---|---|---|---|
| **(a) YouTube 출처 안 단계 (추천)** | `YoutubeVideoSource.find` 의 2단계 뒤에 N-e(요청 0)·N-f(필요할 때만)를 더한다. 결과는 같은 `youtube` 결과로 나간다 | N-e가 정말 추가 요청 0(10.1절·리서치 7절 V5 추천 그대로). 캐시 `youtube` 줄·TTL 7일·29일 지우기·차단 목록 `source: youtube`·키 없을 때 꺼짐이 **새 코드 없이** 덮인다. 같은 영상이 두 출처에서 겹치지 않는다 | 머지된 `YoutubeVideoSource` 를 고친다(시험 구성 한 곳). N-f 날은 같은 안쪽 마감 안에서 왕복이 2번에서 4번으로 는다(리스크 4) |
| (b) 따로 출처 `daehan_news` | 새 `EventVideoSource`(key `daehan_news`)가 YouTube 출처와 **병렬로** 돈다 | M3a5a 코드를 건드리지 않는다. 왕복이 늘지 않는다 | 병렬이라 YouTube 출처 결과를 읽을 수 없어 **N-e가 불가능** → 꼭지가 있는 사건마다 늘 검색 1회 더(1953~1994 날짜의 새 날짜 처리량이 하루 약 26개 → 최악 13개가 보통이 됨). 같은 ID `videos.list` 를 두 번. 새 TTL `ttl.daehan_news` + 30일 상한을 `youtube` 밖으로 넓힘 + `SearchCachePurge` 의 `SOURCE` 를 목록으로(둘 다 머지 코드 수정). 검색·예약·마감 흐름을 복사 |

**결정 2 (추천 (a))**: N-e를 어느 제목으로 대조하나.

| 안 | 내용 | 왕복 (N-f가 필요한 날) | 바뀌는 머지 코드 |
|---|---|---|---|
| **(a) `videos.list` 제목 (추천)** | 2단계 항목의 `snippet.title` 로 N-e. N-f가 필요하면 4단계 검색 → 5단계 새 ID `videos.list` | 4번 (N-f 없는 날은 2번 그대로) | 없음 (클라이언트 그대로) |
| (b) `search.list` 제목 | `YoutubeClient.search` 가 (ID, 제목)을 돌려주게 바꿔 1단계 뒤에 N-e → N-f 검색 → `videos.list` 한 번(옛 계획 모양) | 3번 | 클라이언트 계약과 기존 클라이언트·출처 시험의 결과 단언이 바뀐다. 검색 응답 제목은 HTML 문자 참조(`&quot;` 등)로 올 수 있다(호수 머리는 영향 없음) |

추천 이유: (a)는 머지된 계약을 바꾸지 않고, 늘어나는 왕복은 N-e가 실패한 1953~1994 날짜에만 생긴다. 최악 시간은 같은 안쪽 마감이 막아 늘지 않는다(늦으면 `FAILED`, 캐시 안 됨 — 리스크 4).

### 단계 (결정 1 (a)·2 (a) 기준)

| 단계 | 하는 일 | 요청 | 예약 | 실패·거부일 때 |
|---|---|---|---|---|
| 0 | 사건마다 색인 꼭지 고르기(요청 없음): d ∈ [1953-06-23, 1994-12-31], `issue_date` ∈ [d, d+7], 꼭지 제목이 사건어 규칙 통과. 차례는 `issue_date` 오름차순, 같은 날이면 색인 줄 순서. 기간 밖·색인 꺼짐·`tragic`(대상 아님)이면 빈 목록 | 0 | - | - |
| 1 | M3a5a 그대로: 검색(사건 × 허용 채널) | n | `SEARCH` n (n ≥ 1 일 때만) | M3a5a 그대로 |
| 2 | M3a5a 그대로: `videos.list` | ceil(ID/50) | `UNITS` (ID ≥ 1 일 때만) | M3a5a 그대로 |
| 3 (N-e) | 2단계 항목 중 제목이 대한뉴스 머리인 것을 0단계 꼭지와 대조(아래 규칙 "꼭지 대조") | 0 | - | - |
| 4 (N-f) | 0단계 꼭지가 있는데 3단계에서 **그 사건의 어떤 꼭지에도** 대조된 영상이 없는 사건마다, 첫 꼭지 하나로 KTV 아카이브 한정 검색 1회. `q="대한뉴스 제 {호수}호 {꼭지 제목}"` | k ≤ 3 | `SEARCH` k (**k ≥ 1 일 때만** — M3a5 결정 3 (a)) | 예약 거부 → **출처 전체 `QUOTA_SKIPPED`**(그 조회는 YouTube 영상 없음, 캐시 안 됨, `partial` 그대로). 403 `quotaExceeded` → `exhausted(SEARCH, 예약한 날)` + `QUOTA_SKIPPED`. 그 밖의 실패·시간 초과 → `FAILED`(`partial` true, 캐시 안 됨) |
| 5 | 4단계 결과 중 2단계에 없던 ID만 `videos.list` | ceil(새 ID/50), 보통 1 | `UNITS` (새 ID ≥ 1 일 때만) | 4단계와 같은 방식(`UNITS`) |
| 6 | 사건마다 후보: 매퍼 후보(1·2·4·5단계 영상, 그 사건의 검색 순서, N-f 결과는 뒤에) → 대조된 영상은 R-N6·R-N8을 통과하면 **대한뉴스 후보로 바꿈**, 통과하지 못하면 매퍼 후보 그대로 → R-N7 | 0 | - | `OK` |

- 4단계에서 출처 전체를 `QUOTA_SKIPPED`/`FAILED` 로 하는 이유: `OK` 로 내면 대한뉴스가 빠진 답이 7일 캐시된다. 지금 `SourceResult` 에는 "보여 주되 캐시하지 않음" 상태가 없고, 그것을 더하는 것은 `VideoSearch` 계약 변경이다(옛 계획 "검색 버킷이 없으면 건너뛴다(캐시하지 않음)"을 머지 코드로 옮긴 모양). 그 조회에서 KTV 클립까지 빠지는 것은 드문 경우(검색 상한 바로 앞, 또는 실패)뿐이다.
- 결정 3 (a)면 1단계 전에 `UNITS` 가 1단위도 줄 수 없는 상태(소진 표시 또는 상한)인지 먼저 보고, 그렇다면 검색을 예약하지 않고 요청 0으로 `QUOTA_SKIPPED`. 4단계 전에도 같다.

## 데이터 형식 (`data/daehan-news/`, content-curator)

`segments.csv` — UTF-8, 첫 줄 머리글:

```
issue_no,issue_date,segment_title,duration_seconds,source_url
1715,1988-09-17,서울 올림픽 개막,576,https://www.ehistory.go.kr/view/movie?...
736,1969-07-26,사람이 달에 내리다-아폴로 11호 제1신-,298,https://www.ehistory.go.kr/view/movie?...
1611,1986-09-20,아시안 게임 개막,555,https://www.ehistory.go.kr/view/movie?...
```

- `issue_no` 정수(원본 `호수` 가 "제1715호" 형식이면 숫자만), `issue_date` `YYYY-MM-DD`(원본 `제작일`), `segment_title` 원문 그대로(NFC), `duration_seconds` 정수 또는 빈 값(원본 `재생시간(초)`), `source_url` 원본 `링크주소`(`http(s)`). 위 예시 값은 형식 예시이고 실제 값은 T1이 색인에서 넣는다.
- **방영본만** 넣는다. 방영본을 가르는 열(`메뉴코드`·`대분류`·`중분류`)과 값은 T0의 N1·N-B1 결과로 정해 `README.md` 에 적는다.
- 1953-06-23 ~ 1994-12-31 밖의 행은 넣지 않는다.
- `README.md`: 데이터셋 이름·주소, 원본 파일 이름, 기준일(20251031), 이용허락범위, 받은 날짜, 방영본 거름 규칙, 원본 행 수와 색인 행 수, 바꾼 것(열 이름, 호수 숫자화, NFC).
- `source_url`(e영상역사관 주소)은 출처 기록용이다. **응답의 어떤 필드에도 나가지 않는다**(T2 (8)).

### 설정과 시험 설정

| 키 | 기본값 | 검사 |
|---|---|---|
| `daehan-news.index-file` | `${app.data-dir}/daehan-news/segments.csv` | **빈 값이면 로그 없이 끔**(시험 설정용). 값이 있는데 파일이 없으면 시작 때 WARN 한 줄 `Daehan News index not found: newsreel dating is off` 후 끔. 파일 형식이 틀리면 시작 실패(오류 글에 줄 번호, M3a2 큐레이션과 같은 방식) |
| `daehan-news.match-window-days` | 7 | 0~31 |
| `daehan-news.max-duration-ratio` | 2.0 | 1.0 초과 |

- 수록 기간(1953-06-23 ~ 1994-12-31)은 상수다(10.1절 10번 (a), 대한뉴스는 끝난 자료).
- **시험 `config/application.yml` 에 `daehan-news.index-file: ""`** 한 줄. 이 기능의 시험만 시험 리소스 색인(`server/src/test/resources/fixtures/daehan-news/segments.csv` + `README.md`)을 켜서 쓴다. 그래서 저장소 `data/` 에 색인이 들어와도 기존 시험(1988-09-17 대한뉴스가 붙지 않는다는 M3a5a 시험 포함)이 단언을 바꾸지 않는다.

## 규칙

| 규칙 | 내용 |
|---|---|
| 수록 기간 | d가 1953-06-23 ~ 1994-12-31 밖이면 대한뉴스 단계 요청 0(10.1절 10번 (a), R-N4). YouTube 출처의 1·2단계는 M3a5a 그대로 돈다 |
| 매칭 창 | `issue_date` ∈ [d, d + `match-window-days`]. 사건 앞 회차(준비·예행연습·카운트다운 보도)는 보지 않는다(10.1절 12번 (a), R-N1) |
| 꼭지 단위 | 꼭지 제목이 `VideoAccuracy` 의 사건어 규칙(한국어 핵심어 절반 이상, 띄어쓰기 무시, 숫자 핵심어 필수)을 통과해야 한다(R-N2). 규칙을 복사하지 않고 `VideoAccuracy` 의 공개 메서드로 부른다 |
| 제목의 호수 (R-N5) | NFC 제목이 `^\s*대한뉴스\s*제\s*(\d+)\s*호\s*-?\s*` 에 맞으면 잡힌 숫자가 호수, 그 뒤가 "제목 꼭지". 맨 앞만 보므로 꼭지 제목 안의 "제1신"·"제 9회"는 호수가 아니다. 공백·"-" 앞뒤 공백은 고정하지 않는다(3.5절 「대한뉴스 제 23호 - …」). 대괄호 형태는 매퍼가 이미 버렸다 |
| 꼭지 대조 (N-e) | 영상의 호수 = 꼭지의 `issue_no` **이고** 제목 꼭지와 `segment_title` 이 공백·문장부호를 빼고(NFC, 소문자) 같을 때만 그 꼭지의 영상이다. 호수만 같은 같은 회차의 다른 꼭지(예: 736호 「개헌은 국민의 뜻으로」)는 대조되지 않는다. 검색은 정확 일치가 아니므로 N-f 결과에도 같은 대조를 그대로 한다 |
| N-f 보조 검색 | 위 "단계" 표 4·5단계. 사건마다 최대 1회, 첫 꼭지 하나로 |
| 확인 | 대조된 영상은 이미 `YoutubeVideoMapper` 를 통과한 후보다(임베드·공개·KR·연령·채널 ID·ID·차단 목록·R1~R9). 매퍼가 버린 영상은 대조하지 않는다 |
| 같은 꼭지 여러 ID (R-N7) | 한 꼭지에 대조된 영상이 둘 이상이면 `snippet.publishedAt` 이 가장 이른 하나만 대한뉴스 후보로 남기고 나머지는 후보에서 뺀다(같으면 ID 순). `publishedAt` 은 **이 고르기에만** 쓰고 날짜·점수에는 쓰지 않는다(R-Y1 그대로) |
| 설명의 "제작일" (R-N8) | 설명 첫 줄이 `^\s*제작일\s*:\s*(\d{4}-\d{2}-\d{2})` 이면 색인 `issue_date` 와 같은지 본다. **다르면 회차 날짜를 붙이지 않는다**(매퍼 후보 그대로). 날짜의 원천은 색인이고 설명은 대조용이다. "제작일"이 없으면 대조를 건너뛴다 |
| 길이 대조 (R-N6) | YouTube 길이(`duration_seconds`)와 색인 `duration_seconds` 의 큰 쪽 ÷ 작은 쪽이 `max-duration-ratio`(2.0)를 넘으면 다른 편집본으로 보고 회차 날짜를 붙이지 않는다. 어느 한쪽 길이가 없으면 대조하지 않는다. 표본: 9분 33초 대 9분 36초, 4분 59초 대 4분 58초(3.5절) |
| 대한뉴스 후보 | 매퍼 후보에서 다음만 바꾼다: `basis` `DAEHAN_NEWS`, `explicitDates` = [`issue_date`], `dateTextExcluded` true(그대로), **`title` = 꼭지 제목**(호수 머리 뺌 — 호수가 연도로 읽히지 않게), `description` = 꼭지 제목, `official` true, `eventLink` `NONE`, `originalCapture` 매퍼 값(그대로), `origin` 매퍼 값 `MediaOrigin("youtube", ID, null, null, 채널 ID)`(그대로). 레코드는 아래 "레코드 값"으로 다시 만들어 `MediaFilter`·`MediaNormalizer` 를 다시 거친다 |
| 레코드 값 (4절 V5) | 매퍼 레코드와 같고 다른 것만: `attribution` "대한뉴스 제{호수}호 ({YYYY}년 {M}월 {D}일 자) · 대한뉴스(정부 제작 뉴스영화) · {채널 이름} · YouTube", `description` 꼭지 제목, `captured_at` = 회차 연도, `date_precision` `approx`(R8 — `captured_at` 은 연도 "1988"), `selection.basis` `daehan_news`. `source_url`·`embed_url`·`thumbnail_url` 은 YouTube 값 그대로 |
| 날짜·점수 | `explicitDates` 로 M3a4 날짜 점수(같은 날 +40, 같은 달 +20, 같은 해 +10). 1715호(+0일) 40 + 사건어 30 + 공식 20 = **90**, 736호(+6일, 같은 달) **70** — 머지된 `WorldStoryScoreTableTest` 6·7행과 같다. 화면은 "1988년 무렵 촬영(추정)"(R-N3, M3b1). `publishedAt`·`recordingDate` 는 날짜로 쓰지 않는다 |
| 날짜를 붙이지 못함 | R-N6·R-N8 실패면 매퍼 후보 그대로(`captured_at` null → "촬영 시점 미상", `attribution` 에 날짜 없음, 보통 50점이라 붙지 않음) |
| 사건마다 하나 | `VideoSearch` 가 사건마다 영상 하나를 고르므로, 같은 사건에 KTV 클립(60)과 대한뉴스(90)가 함께 맞으면 대한뉴스가 고른다(1986-09-20 — 리스크 6) |
| 키 | 키가 비면 `YoutubeVideoSource` 가 등록되지 않아 대한뉴스 단계도 없다(요청 0) |
| `tragic` | `VideoSearch` 가 대상으로 넘기지 않는다 → 요청 0 |
| 로그 | 대조·버림은 DEBUG에 이유별 개수만. 날짜·사건 제목·꼭지 제목·호수·영상 ID·검색어를 로그에 넣지 않는다. 예외: 기존 `MediaNormalizer` DEBUG 줄의 영상 ID(M3a5a T4 결정 2 (a), 공개 배포 전 `fix/`) |

## 캐시·30일·정직한 날짜 (결정 1 (a) 기준)

- **캐시 키**: 출처 `youtube` 한 줄(날짜·사건·`dayPathsOff`)에 그 사건의 최종 후보(대한뉴스 후보 포함)가 저장된다. 형식 v1 그대로(`VERSION` 1, 새 필드 없음). 읽을 때 `MediaFilter`·`MediaNormalizer` 를 다시 거친다 — 대한뉴스 후보의 `approx` 연도 `captured_at` 이 R8을 통과해야 한다(T2b (5) 시험).
- **TTL과 30일**: `media.search-cache.ttl.youtube` 7일, `SearchCachePurge` 가 29일 지난 `youtube` 줄을 지운다. 대한뉴스 후보도 YouTube API 데이터이므로 같은 의무 아래에 있고, **새 코드 없이** 덮인다. 결정 1 (b)를 고르면 `daehan_news` TTL·30일 상한·지우기를 넓혀야 한다.
- **캐시하지 않는 것**: `QUOTA_SKIPPED`·`FAILED`·시간 초과(머지 코드 그대로). N-f가 거부·실패한 조회도 여기에 든다.
- **이미 쌓인 줄**: M3a5a로 이미 캐시된 날짜(대한뉴스 날짜 없음)는 7일 동안 그대로다. 색인 파일을 고쳐도 같다. PM 품질 확인(T4) 전에 P3으로 `youtube` 줄을 지운다.
- **정직한 날짜**: 회차 날짜는 촬영일이 아니다(736호에 7/16~7/25 장면). 그래서 `captured_at` 은 연도, `date_precision` `approx`, `date_relation` `{"kind": "approximate", "years": 0}`, 캡션에 "… 자"를 쓴다. 그날 찍은 것처럼 보이지 않는다(`CLAUDE.md` 7절 1항).
- **쿼터**: 검색·일반 버킷은 YouTube 출처와 공유한다(검색 80회, 일반 8,000단위). N-f는 1953~1994 날짜에서 N-e가 실패한 사건에만 검색을 1회 더 쓴다(3절 하루 처리량: 최악 13개, 보통 26개에 가까움).

## API / 데이터 변경

- `GET /api/story/world`: 요청·응답 모양 그대로. `video_embed` 레코드의 `selection.basis` 에 `daehan_news` 가 나온다. 합성 예(1988-09-17, 시험 fixture 값):

```json
{
  "id": "youtube-Abc123xyz_0",
  "type": "video",
  "display_mode": "video_embed",
  "url": null,
  "embed_url": "https://www.youtube-nocookie.com/embed/Abc123xyz_0?playsinline=1&rel=0&enablejsapi=1&origin=http%3A%2F%2Flocalhost%3A5173",
  "provider": "youtube",
  "source_name": "KTV 아카이브",
  "source_url": "https://www.youtube.com/watch?v=Abc123xyz_0",
  "license": "provider-terms",
  "license_status": "other",
  "license_url": "https://www.youtube.com/t/terms",
  "attribution": "대한뉴스 제1715호 (1988년 9월 17일 자) · 대한뉴스(정부 제작 뉴스영화) · KTV 아카이브 · YouTube",
  "description": "서울 올림픽 개막",
  "captured_at": "1988",
  "date_precision": "approx",
  "lat": null,
  "lng": null,
  "place_ref": "country:KR",
  "date_relation": { "kind": "approximate", "years": 0 },
  "selection": { "basis": "daehan_news", "score": 90 }
}
```

  (나머지 키 `thumbnail_*`·`video_sources`·`duration_seconds`·`render`·`link_only_reason`·`retrieved_at` 은 M3a5a 계약 예시와 같은 규칙.)
- 새 데이터 파일 `data/daehan-news/segments.csv`, `README.md`. 설정 `daehan-news.*`(위 표). 시험 설정 한 줄.
- DB 변경 없음(V3 `event_media_search_cache` 그대로). 웹 변경 없음.

## 테스트 방침

- **외부 네트워크를 부르지 않는다.** YouTube 응답은 M3a5·M3a5a처럼 공식 문서 필드 구조로 만든 합성 fixture(`fixtures/youtube/`, 합성 ID 11자). 색인은 공공 데이터("제한 없음")라 시험 리소스에 T1 색인의 표본 행(1609·1610·1611·1714·1715·736·737호)을 그대로 쓰고, 옆 `README.md` 에 데이터셋 주소·기준일·이용허락범위를 적는다.
- 쿼터·시간은 `MutableClock` 과 설정값·`Deadline.CANCEL_GRACE` 로 계산한다. 캐시는 Testcontainers(Docker 필요).
- 반복: `./gradlew check` **2회**, 쿼터·시간·캐시 DB 실패 시험이 든 클래스는 **3회**.
- **날짜는 제품 정의서 12절 데모 입력 3개(1988-09-17, 2002-06-22, 1995-03-15)와 표본(1950-06-25, 1969-07-20, 1986-09-20, 2000-02-29, 2014-04-16 — M3-P13의 공개 역사 사건 날짜·경계값)뿐이다.** 실제 사람의 생년월일이 아니다. 색인의 회차 날짜(1969-07-26 등)는 그 사건들의 공개 보도 날짜다. 시험용 `tragic` 사건은 지어낸 문장이다.
- 어떤 날짜에 영상이 없으면 그대로 두고 다른 날짜를 찾거나 제안하지 않는다(PM 피드백 2026-10-16). 실제 품질은 PM 품질 확인(T4).

## 데모·표본 날짜로 확인할 것

| 날짜·경우 | 자동 테스트 (합성 fixture + 시험 색인) | PM 품질 확인 (T4) |
|---|---|---|
| 1988-09-17 | 색인 1715호(+0일) + 2단계 항목 「대한뉴스 제 1715호-서울 올림픽 개막」(설명 "제작일: 1988-09-17") → `video_embed`, `daehan_news`, 점수 90, `captured_at` "1988" `approx`, 위 `attribution`. **검색 1·`videos.list` 1(N-e, 추가 0)**. 두 번째 호출 YouTube 요청 0, 응답 같음. 1714호(−4일, 「한강축제」)·1711호 예행연습은 창 밖 | **데모 날짜에 대한뉴스 영상이 붙는다**(로드맵 데모 표 "M3a5b 뒤") |
| 1969-07-20 | 736호(07-26, +6일) → 붙음, 점수 70. 같은 회차 다른 꼭지(「개헌은 국민의 뜻으로」)는 꼭지 대조·사건어 불일치로 안 붙음. 737호(+13일)는 창 밖 | 결과 표 |
| 1986-09-20 | 2단계에 1611호가 없음 → **N-f 검색 1회**(`q` 와 `channelId` 를 WireMock으로 단언) → 새 ID만 `videos.list` 1회 → 「대한뉴스 제 1611호-아시안 게임 개막」 붙음(90). 같은 사건의 KTV 클립(60)은 고르지 않음(사건마다 하나). 1610호(09-12, 「아시안 게임 준비」)·1609호(카운트다운)는 창 밖 | 결과 표 (3.5절 Y2 2위). 클립 대신 대한뉴스가 나오는 것이 정상(리스크 6) |
| 제목 변형 | 「대한뉴스 제1715호-…」, 「대한뉴스  제 1715 호 - …」 → 1715. 「대한뉴스 제 736호-…아폴로 11호 제1신-」 → 736(11·1이 아님). 「[📺컬러 복원] … [대한뉴스 698호] …」 → 매퍼에서 빠짐(대조까지 오지 않음) | - |
| 호수가 연도로 읽힘 | 대한뉴스 후보의 `title` 이 꼭지 제목(호수 없음)이라 `VideoAccuracy` 의 다른 해 −40이 호수에 걸리지 않는다(1715호 후보 `title` = 「서울 올림픽 개막」 단언) | - |
| 같은 꼭지 두 ID (R-N7) | (736, 「사람이 달에 내리다…」) 두 ID → `publishedAt` 이른 하나만 대한뉴스 후보. `publishedAt` 을 바꿔도 `captured_at`·점수는 같다 | - |
| 제작일 대조 (R-N8) | 설명 "제작일: 1986-09-27" ≠ 색인 1986-09-20 → 날짜 안 붙음 → 매퍼 후보(50) → 버림. 설명에 "제작일" 없음 → 대조 건너뛰고 색인 날짜로 붙음 | - |
| 길이 대조 (R-N6) | YouTube 1200초 대 색인 555초(2.16배) → 날짜 안 붙음 → 버림. 색인 길이 빈 값 → 대조 건너뜀 | - |
| N-f 쿼터 | 1단계 뒤 검색 버킷이 0 → N-f 요청 0, 응답 200, `partial` false, 캐시 줄 없음, 그 조회의 YouTube 영상 없음. N-f 403 `quotaExceeded` → 그 뒤 태평양 자정까지 검색 0. N-f 500 → `partial` true, 줄 없음 | 콘솔 검색 사용량이 예상 안 |
| UNITS 소진 (결정 3 (a)) | `exhausted(UNITS)` 뒤 → 검색 요청 0·검색 예약 0, `QUOTA_SKIPPED` | - |
| 시간 | N-f 검색이 `lookup-deadline` 보다 늦으면 미디어 구간 ≤ `lookup-deadline` + `CANCEL_GRACE` + 0.5초, `partial` true, 줄 없음, 사진 경로 늦지 않음 | - |
| 2002-06-22, 2000-02-29, 1995-03-15 | 기간 밖(1995-03-15는 사건 없음) → 대한뉴스 단계 요청 0(YouTube 1·2단계 요청 수는 M3a5a 시험 그대로) | 같음 |
| 1950-06-25, 2014-04-16 | `tragic`(그리고 기간 밖) → 요청 0 | 같음 |
| 색인 없음·꺼짐 | 빈 설정 → 로그 없음, M3a5a 동작 그대로. 값이 있는데 파일 없음 → WARN 1줄, 나머지 응답 정상. 형식 틀림 → 시작 실패 | - |

## 크기 (400줄 규칙)

S1 = 제품 코드·설정(`server`, 테스트 제외, M3a5a와 같은 명령). `data/daehan-news/`(색인 데이터)와 시험 리소스는 S1에 들어가지 않는다.

### 실측 비율

| 기능 | 파일별 셈 | 실제 S1 | 비율 |
|---|---|---|---|
| M3a4c (PR #25) | 약 323 | 583 / −23 | ×1.8 (새 외부 호출 없음 — 연결·캐시) |
| M3a5 (PR #26) | 약 210 | 532 / −6 (확인점 1에서 525) | ×2.5 |
| **M3a5a (PR #27)** | 약 204 (T4 98·T5 106) | **563** (T4 뒤 281, T5 뒤 557 → 결정 반영 563) | **×2.76** (기대 ×2.5 약 510보다 조금 큼) |

### 다시 셈

| 작업 | 파일별 셈 | ×1.8 | ×2.5 (기대) | ×2.76 |
|---|---|---|---|---|
| T2 ★ | 색인 읽기·검증·끄기·창 조회(약 40) · 설정 `daehan-news.*` 와 `application.yml`(약 15) · 꼭지 고르기(기간·창·사건어, 약 15) · 제목 호수 정규식·꼭지 대조(약 15) · R-N6·R-N8(약 15) · R-N7(약 10) · 대한뉴스 후보·레코드 다시 만들기(약 30) · `VideoAccuracy` 사건어 공개 메서드(약 6) = **약 146** | 약 263 | 약 365 | 약 403 |
| T2b | YouTube 출처의 N-e·N-f 단계(꼭지별 대조, 4·5단계 예약·상태, 사건별 후보 합치기, 약 45) · 부품 주입·꺼짐(약 5) · UNITS 먼저 확인(결정 3 (a), `YoutubeQuota` 읽기 메서드 포함, 약 10) = **약 60** | 약 108 | 약 150 | 약 166 |
| T3·T4 | 시험·PM — S1 0 | 0 | 0 | 0 |
| **합계** | **약 206** | **약 371** | **약 515** | **약 569** |

기대값은 **약 515 (370~570)** 다. 규칙·정규식이 많은 T2는 M3a5a 쪽(×2.76)으로, 연결뿐인 T2b는 M3a4c 쪽(×1.8)으로 갈 수 있다. 결정 3을 (b)·(c)로 하면 약 −25. 결정 1 (b)면 출처 복사·TTL·지우기 확장으로 약 +40(×2.5 기준 +100). **400을 넘으므로 PM 결정 4**를 둔다.

### 크기 결정 4

| 안 | 내용 | 크기 (기대) | M3 PR 수 |
|---|---|---|---|
| **(a) 한 PR + 확인점 (추천)** | T0b~T4 모두 `feat/daehan-news-video`. **확인점 1 (멈춤)**: T2 뒤 누적 S1이 **450**을 넘으면 T2b 전에 멈추고 (b)로 나눌지 묻는다. **확인점 2 (알림)**: T2b 뒤 **650**을 넘으면 알린다(멈추지 않음) | 약 515 (370~570) | 23 |
| (b) 두 PR | 앞 PR = T0b·T1·T2 + 검증(색인 데이터와 부품, 연결 안 함 — 동작 변화 없음, 약 365), 뒤 PR = T2b + 검증 + T4(연결, 대한뉴스가 나옴, 약 150). 이름은 PM이 정함(예: M3a5b `feat/daehan-news-index` → M3a5b2 `feat/daehan-news-video`) | 약 365 / 약 150 | 24 |
| (c) N-f를 뒤로 미룸 | 4·5단계를 빼고 N-e만(약 −30, 기대 −75). 대신 Y-a 결과에 없는 꼭지는 붙지 않는다 — **1986-09-20의 제1611호가 빠진다**(3.5절: Y-a 결과에 없음). 결정 3도 덜 급해진다 | 약 440 | 23 |

추천 이유 (a): 뒤 PR이 약 150으로 작고 앞 PR은 눈에 보이는 동작이 없어(M3a5 → M3a5a 때와 같은 이유) 리뷰·검증·PM 확인이 한 번씩 더 든다. M3a5a(실제 563)를 PM이 한 PR로 받은 선례가 있다. T2는 연결이 없는 순수 부품이라 확인점 1에서 숫자를 보고 (b)로 바꿔도 고칠 것이 없다. (c)는 표본 하나를 잃고 줄은 조금만 준다.

각 backend-dev 보고서(T2·T2b)는 누적 S1을 적는다. 테스트(S2)는 따로 보고한다.

## 작업 목록

순서: **T0 (PM, 따로) → T0b → T1 → T2 ★ → (확인점 1, 멈춤) → T2b → (확인점 2, 알림) → T3 → 리뷰 → PR → T4 (PM) → 머지.** T0b가 "확인됨"을 내기 전에는 T1·T2·T2b를 시작하지 않는다.

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | 대한뉴스 확인 (리서치 6.0절) | PM | 리서치 머지 | (1) 6번: 공공데이터포털 "한국정책방송원_영상역사관 대한뉴스관" 활용신청. (2) 7번 N1·N2 실행(`$DATA_GO_KR_SERVICE_KEY`). (3) 5번 N-B1: 브라우저로 CSV를 받아 **저장소 밖**에 두고 경로를 오케스트레이터에게. (4) 6.2절 Y4를 `W0isyEBFRvI`(제1611호)로 1회 더(`embeddable`·설명 첫 줄 확인, 일반 1단위). (5) (선택) 11번 N-B9. 명령 출력은 data-researcher에게. 8번 N3은 완료(2026-10-11) |
| T0b | 리서치 확신도 갱신 (`docs(research)` 커밋) | data-researcher | T0 | (1) 색인 경로(N-a) 행이 "확인됨". (2) 방영본 구분 열·값, `제작일`·`호수`·`재생시간(초)`·`링크주소` 형식 기록. (3) 제1611호 `embeddable` 과 설명 첫 줄 "제작일" 유무 기록(R-N8 표본 3/3인지). (4) **새로**: 표본 3개(1715·736·1611호)의 CSV 꼭지 제목이 3.5절 KTV 아카이브 제목의 호수 뒤 부분과 공백·문장부호를 빼고 같은지 표로(규칙 "꼭지 대조"의 근거). 하나라도 다르면 "막힘"으로 보고 — planner가 대조 규칙을 고친다. (5) HTTP 요청의 User-Agent는 저장소 주소만, 이메일 없음. 키·로그인·약관 동의 없음 |
| T1 | 대한뉴스 꼭지 색인 (`data(daehan-news)` 커밋) | content-curator | T0b | (1) C-D1 출력이 `ok <행 수>`. (2) 1715·736·1611호 행이 리서치 5절 V5 표(회차 날짜·꼭지 제목·길이)와 같다. (3) 방영본만, 기간 밖 행 없음. (4) 원본 CSV는 커밋하지 않는다(`git status` 에 없음). (5) README에 출처·기준일·이용허락범위·받은 날짜·거름 규칙·원본 행 수와 색인 행 수 |
| T2 | ★ 색인 읽기와 대조 부품 (`feat(media)` 커밋, 연결 안 함) | backend-dev (PM 결정 5) | T1 | (1) **색인**: `daehan-news.index-file` 의 CSV를 시작 때 읽는다. 머리글이 다르거나 행이 C-D1 규칙을 어기면 시작 실패(오류 글에 설정 키·줄 번호, 행 내용 없음). 값이 있는데 파일이 없으면 WARN 정확히 한 줄 `Daehan News index not found: newsreel dating is off` 후 끔. **빈 값이면 로그 없이 끔.** 시험 `config/application.yml` 에 `daehan-news.index-file: ""` 한 줄 — 기존 시험 단언 그대로. 설정 표 검사(창 −1·32, 비율 1.0)마다 시작 실패·키 이름. (2) **꼭지 고르기**: 수록 기간 밖이면 빈 목록. 창 [d, d+7]. 사건어는 `VideoAccuracy` 에 더한 공개 메서드 하나로(같은 `Prepared`, 규칙 복사 없음) — 1988-09-17 "1988년 하계 올림픽" → 1715호만(1714호 창 밖), 1969-07-20 "아폴로 11호" → 736호의 「사람이 달에 내리다…」만(「개헌은 국민의 뜻으로」·737호 아님), 1986-09-20 → 1611호만(1610·1609호 창 밖). (3) **제목 대조**: "데모·표본 날짜" 표의 제목 변형 행 전부. 호수가 같아도 꼭지 제목이 다르면 대조 안 됨. (4) **R-N6·R-N7·R-N8**: 표의 각 행. `publishedAt` 은 R-N7 고르기에만 쓰이고 `captured_at`·점수에는 영향이 없다. (5) **대한뉴스 후보**: 규칙 표 "대한뉴스 후보"·"레코드 값"과 필드마다 같다 — 위 JSON 예시를 만드는 합성 항목에서 `MediaItem` JSON의 해당 키가 예시와 같다. `title` = 꼭지 제목. `origin` = `MediaOrigin("youtube", ID, null, null, 채널 ID)`. 차단 목록 `source: youtube` 의 `id`·`channel` 이 대한뉴스 후보도 뺀다. `SearchCacheFormat.write` → `read` 왕복 뒤에도 같은 후보(`basis`·`explicitDates`·`approx` 연도). (6) **점수**: `VideoAccuracy` 와 함께 1715호 90 붙음, 736호 70 붙음, R-N6·R-N8 실패 50 안 붙음. (7) **날짜를 붙이지 못하면** 매퍼 후보 그대로(`captured_at` null, `basis` `youtube_official`). (8) **미디어 원칙** (제품 정의서 6절, `CLAUDE.md` 7.1절): 모든 대한뉴스 레코드에 `source_name`(채널 이름)·`source_url`(YouTube)·`license`(`provider-terms`)·`attribution` 이 있다. `captured_at` 은 회차 **연도**이고 `date_precision` `approx` 라 그날 찍은 것처럼 보이지 않는다("무렵", "… 자"). 표시는 `video_embed`(KTV 아카이브 공식 YouTube 임베드)뿐이고, **e영상역사관 주소(`ehistory.go.kr`)가 레코드의 어떤 필드에도 없다**. 파일을 내려받거나 복사하는 코드가 없고 통신사 사진 경로가 없다. (9) 로그 캡처(DEBUG 포함)에 날짜·사건 제목·꼭지 제목·호수·영상 ID가 없다(기존 `MediaNormalizer` DEBUG 줄 예외만). (10) 아직 연결하지 않음 → 기존 시험 단언·응답·요청 수 그대로. `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 1: 450을 넘으면 T2b 전에 멈추고 결정 4 (b)를 묻는다** |
| T2b | YouTube 출처에 대한뉴스 단계 연결 (`feat(media)` 커밋) | backend-dev | T2 | (1) **단계**: "단계" 표대로. 1988-09-17(합성): 검색 1·`videos.list` 1, `daehan_news` 90 붙음(N-e, 추가 요청 0). 1969-07-20: 736호 70 붙음. 1986-09-20: 검색 2(두 번째는 `q="대한뉴스 제 1611호 아시안 게임 개막"`·KTV 채널 ID를 WireMock으로 단언)·`videos.list` 2(두 번째는 새 ID만), 1611호 90 붙음, 같은 사건 KTV 클립은 고르지 않음. (2) **예약**: N-f가 필요한 사건이 0이면 `reserve` 를 부르지 않는다(k = 0), 새 ID가 0이면 `UNITS` 예약 없음. (3) **N-f 쿼터·실패**: 표의 "N-f 쿼터" 행 — 거부 → 요청 0·응답 200·`partial` false·줄 없음, 403 `quotaExceeded` → `exhausted(SEARCH, 예약한 날)`·`QUOTA_SKIPPED`·자정(`MutableClock`) 뒤 다시 검색, 500·읽을 수 없는 403 → `FAILED`·`partial` true·줄 없음. (4) **결정 3 (a)면** UNITS 소진 행: 검색 요청 0·`SEARCH` 쓴 수 그대로. (5) **캐시 끝까지**(Testcontainers): 1988-09-17 두 번째 호출 YouTube 요청 0, 응답 JSON 같음(`retrieved_at` 포함), `youtube` 줄 1개. 29일 지난 그 줄을 `SearchCachePurge` 가 지운다(기존 지우기 시험이 단언 그대로 통과). (6) **시간**: N-f 검색 지연이 `lookup-deadline` 보다 길면 미디어 구간 ≤ `lookup-deadline` + `CANCEL_GRACE` + 0.5초, 사진 두 경로가 늦어지지 않음, `partial` true, 줄 없음. (7) **요청 0**: 2002-06-22·2000-02-29·1995-03-15에서 대한뉴스 단계 요청 0(YouTube 요청 수가 M3a5a 시험과 같음), 1950-06-25·2014-04-16 요청 0, 색인 꺼짐이면 M3a5a와 요청·응답이 같다. (8) **기존 시험**: `WorldStoryYoutube*`·`YoutubeVideoSourceTest`·`YoutubeVideoMapperTest`·캐시 시험이 **단언을 바꾸지 않고** 통과한다. 허용되는 기존 시험 변경은 `YoutubeVideoSource` 생성자를 부르는 곳(구성만)뿐이고 보고서에 목록. (9) 응답 어디에도 `link_only` 없음, 대한뉴스 레코드가 R5·R9 통과, `ehistory.go.kr` 이 응답 본문에 없다. (10) `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 2: 650을 넘으면 알림** |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | T2b | (1) T2·T2b 기준 표(기준마다 시험 이름). 빠진 기준은 시험을 더한다(제품 코드는 고치지 않음). 특히 k = 0 이면 `reserve` 를 부르지 않음, 호수가 연도로 읽히지 않음, 시험 설정 빈 값에서 WARN 없음이 각각 시험에 있다. (2) `cd server && ./gradlew check` **2회** 안정, 쿼터·시간·캐시 DB 실패 시험이 든 클래스는 **3회**. (3) C2 출력 없음. (4) S1·S2를 파일별로 보고. (5) 새 시험 모두 매치되지 않은 WireMock 요청 0이고, 어떤 시험도 `googleapis.com`·`youtube.com`·`ehistory.go.kr`·`data.go.kr` 에 연결하지 않는다. (6) 로그 캡처(DEBUG 포함)에 가짜 키·검색어·꼭지 제목·호수·영상 ID·날짜가 없다(기존 예외 1건만). (7) 기존 시험 단언 변화 목록(`git diff main...HEAD -- server/src/test` 중 기존 파일) — **허용: 시험 설정 한 줄, `YoutubeVideoSource` 생성 구성.** 그 밖의 바뀐 단언은 "발견"으로 이유와 함께 보고. (8) 결과 표: 데모·표본 8날짜(합성 fixture)마다 첫·두 번째 호출의 검색·`videos.list` 수, 붙은 영상과 `basis`·점수·`captured_at` |
| T4 | PM 품질 확인 (PR 검수 때) | PM | T3 | (1) 루트 `.env` 에 `YOUTUBE_API_KEY`(있음)와 저장소 주소 `WIKIMEDIA_USER_AGENT`. `docker compose up -d db`, **P3으로 이전 `youtube` 캐시 줄을 지운다**(M3a5a 때 저장된 날짜는 대한뉴스 날짜가 없다), `cd server && ./gradlew bootRun` — 시작 로그에 `Daehan News index not found` 가 없다. (2) P1로 1988-09-17·1969-07-20·1986-09-20에 대한뉴스 영상이 붙고 `attribution` 에 "대한뉴스(정부 제작 뉴스영화)"와 "… 자"가 있는지, `captured_at` 이 연도이고 `date_precision` `approx` 인지. (3) 기간 밖 날짜(2002-06-22·2000-02-29·1995-03-15)와 `tragic` 날짜에 대한뉴스가 없다. (4) 엉뚱한 꼭지(준비·예행연습·카운트다운·폐회식·같은 회차 다른 꼭지)가 없다. (5) P1 한 번 더 — 결과가 같다(캐시). (6) 콘솔에서 그날 검색 사용량이 예상 안(새 날짜마다 사건 수 + N-f, 이 확인으로 15회 이하). (7) 다른 날짜로 찾아보지 않는다. (8) 캡션 문구는 이때 고칠 수 있다(10.1절 13번) |

```
# C-D1 — 색인 형식 (content-curator). ok 와 행 수
python3 - data/daehan-news/segments.csv <<'EOF'
import csv, sys, datetime, re
rows = list(csv.DictReader(open(sys.argv[1], encoding="utf-8")))
assert rows and list(rows[0].keys()) == ["issue_no","issue_date","segment_title","duration_seconds","source_url"]
lo, hi = datetime.date(1953,6,23), datetime.date(1994,12,31)
for r in rows:
    int(r["issue_no"]); d = datetime.date.fromisoformat(r["issue_date"]); assert lo <= d <= hi
    assert r["segment_title"].strip(); assert r["duration_seconds"] == "" or int(r["duration_seconds"]) > 0
    assert re.match(r"^https?://", r["source_url"])
print("ok", len(rows))
EOF

# C2 — 키 값이 코드·설정 파일에 없는지. 출력 없음
grep -rnE 'AIza[0-9A-Za-z_-]{20,}' server/src web/src .env.example

# S1 — 제품 코드·설정 줄 수 (M3a5a와 같은 명령)
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'

# P3 — (T4) 이전 youtube 캐시 줄 지우기. .env 의 POSTGRES_USER·POSTGRES_DB를 바꿨으면 그 값으로
docker compose exec db psql -U postgres -d the_world_that_day \
  -c "DELETE FROM event_media_search_cache WHERE source = 'youtube'"

# P1 — (T4) 서버 실행 중. 날짜마다 YouTube 영상(대한뉴스 포함)
for d in 1988-09-17 2002-06-22 1995-03-15 1950-06-25 1969-07-20 1986-09-20 2000-02-29 2014-04-16; do
  echo "== $d"
  curl -s "http://localhost:8080/api/story/world?date=$d" \
    | jq -c '{partial, events: [.events[] | {title, youtube: [.media[] | select(.provider=="youtube") | {id, attribution, description, captured_at, date_precision, selection}]}]}'
done
```

담당별: PM 2 (T0, T4), data-researcher 1 (T0b), content-curator 1 (T1), backend-dev 2 (T2 ★, T2b), test-engineer 1 (T3). 모두 7개.

## 리스크와 확인할 것

1. **막힘 — 색인 경로(N-a) "부분 확인"** — T0(PM)·T0b가 끝나야 시작한다. 1988 데모 영상이 여기에 달려 있다.
2. **KTV 아카이브에 모든 꼭지가 있는지 모른다**(위험 19) — 표본 3개 회차(736·1611·1715)는 있었다(3.5절). 색인에서 찾아도 영상이 없으면 비운다(정상).
3. **꼭지 제목이 CSV와 KTV에서 다를 수 있다** — 대조는 호수 + 꼭지 제목(공백·문장부호 무시)이다. 다르면 붙지 않을 뿐 엉뚱한 영상은 붙지 않는다(모르면 붙이지 않음). T0b (4)가 표본으로 먼저 보고, T4에서 PM이 본다.
4. **N-f 날의 왕복 4번** (결정 2 (a)) — 1953~1994 날짜에서 N-e가 실패하면 같은 안쪽 마감(`media.lookup-deadline` 3초) 안에서 검색 → `videos.list` → 검색 → `videos.list` 를 차례로 한다. 최악 시간은 마감이 막아 늘지 않지만, 느린 날에는 `FAILED`(`partial` true, 캐시 안 됨)가 늘 수 있고 다음 조회가 검색을 다시 쓴다. T2b (6)·T3이 스텁 시간을 잰다. 자주 보이면 결정 2 (b)(왕복 3번) 또는 M6b.
5. **N-f가 거부·실패하면 그 조회의 YouTube 영상이 모두 빠진다** — 대한뉴스 없는 답이 7일 캐시되지 않게 하려는 것이다(`QUOTA_SKIPPED`/`FAILED`). 검색 상한 바로 앞이나 실패일 때만 생기고, 다음 조회(또는 태평양 자정 뒤)에 다시 찾는다.
6. **사건마다 영상 하나** — 1986-09-20은 KTV 개막식 클립(60, M3a5a에서 붙던 것) 대신 대한뉴스 제1611호(90)가 나온다. 회차 날짜가 사건 날과 같은 정부 뉴스영화가 더 맞는 자료로 본다. 클립을 계속 보이고 싶으면 영상 슬롯 정책(M6b) 문제다.
7. **방영본 구분**(위험 20) — T0의 N1·N-B1로 정한다. 정하지 못하면 T1이 막힘으로 보고한다.
8. **회차 날짜의 뜻**(위험 21) — 제작일인지 상영일인지 모른다. `approx`·연도·"… 자" 표기로 촬영일처럼 보이지 않게 한다.
9. **정부 뉴스영화의 관점**(위험 22) — 캡션에 "대한뉴스(정부 제작 뉴스영화)"를 밝힌다(10.1절 13번). 문구는 PM이 T4에서 고칠 수 있다.
10. **색인을 저장소에 두는 것**(10.1절 11번 (a)) — "이용허락범위 제한 없음"의 해석 문제다(법률 자문 아님). 필요한 열만 두고 출처를 적는다. 파일 크기는 약 1~2 MB로 본다.
11. **검색 버킷 공유** — N-f가 1953~1994 날짜에서 사건당 최대 1회 더 쓴다. 최악이면 새 날짜 처리량이 하루 약 13개로 준다(3절 하루 처리량). PM 확인 명령을 돌린 날은 검색을 먼저 쓴다.
12. **이미 쌓인 캐시** — M3a5a로 캐시된 날짜는 최대 7일 동안 대한뉴스 날짜 없이 나온다. 색인을 고쳐도 같다. T4 전 P3으로 지운다. 공개 배포 때 색인 버전을 캐시 키에 넣을지 다시 본다.
13. **호수가 연도로 읽힘** — 대한뉴스 1900~2040호(1992~1994년)의 호수가 `VideoAccuracy` 의 다른 해 −40에 걸릴 수 있다. 대한뉴스 후보의 `title` 을 꼭지 제목으로 두어 막는다(T2 (5), T3 (1)). 표본 날짜에 그 범위의 회차는 없어 단위 시험으로만 본다.
14. **e영상역사관 저작권 문구**(위험 24) — 꼭지 제목(짧은 사실 문구)만 쓰고 내용 요약 문단은 옮기지 않는다. e영상역사관 주소는 응답에 나가지 않는다.
15. **시험이 저장소 색인을 읽을 위험** — 시험은 `app.data-dir` 로 저장소 `data/` 를 읽는다. 시험 설정이 색인을 조용히 끄지 않으면 T1 커밋 뒤 기존 시험(1988-09-17 대한뉴스 안 붙음)이 깨진다. T2 (1)이 시험 설정 한 줄로 막고, 빈 값은 WARN 없이 끄게 해 M3a5 T1b 같은 순서 의존 실패를 피한다.
16. **KTV 이용 조건**(위험 16, 10.1절 8번) — 결정 (a)(비상업 공식 임베드 + 차단 목록) 그대로, 공개 배포 전 KTV 서면 확인(로드맵 후속).

## 이전 결정 — 유지·대체

**유지 (다시 묻지 않음)**: 10.1절 8번 (a) KTV 이용 조건, 10번 (a) 하한 1953-06-23, 11번 (a) PM이 CSV를 받고 content-curator가 색인, 12번 (a) d ~ d+7 + 꼭지 사건어, 13번 (a) "대한뉴스(정부 제작 뉴스영화)"(문구는 PM), 로드맵 D1 (a) M3a5에서 나눔(2026-10-12), M3-P14 허용 채널 KTV 아카이브만, 10.1절 5·6번 (a), R-N5 컬러 복원은 쓰지 않음, M3a5 결정 3 (a) `reserve` n < 1 예외 유지(N-f에도 적용), 쿼터 계수기는 메모리, M3a5a T4 결정 2 (a) `MediaNormalizer` DEBUG 줄 예외, M3a5a T5 결정 (a) 캐시 쓰기 한 번 실패하면 `down`, PM 피드백 2026-10-16(다른 날짜로 찾지 않음), M3-P13 시험 날짜.

**대체 (이 개정에서)**:

| 2026-10-12 계획 | 바뀐 것 | 이유 |
|---|---|---|
| "N-f로 얻은 ID는 같은 날짜의 묶음에 더해 `videos.list` 1회" | 새 ID만 5단계 `videos.list`(결정 2 (a)) | 머지된 검색은 ID만 돌려주고, 날짜의 `videos.list` 는 N-e 전에 나간다 |
| "고른 영상은 M3a5의 `videos.list` 확인·버림 규칙" | `YoutubeVideoMapper`(M3a5a T4) | 같은 규칙이 M3a5a에서 그 이름으로 머지됨 |
| "색인 없으면 이 어댑터를 끈다" | 대조 부품만 끄고 YouTube 출처는 그대로. 빈 설정은 로그 없이 끔 | 결정 1 (a) — 따로 어댑터가 없다. 시험 설정(리스크 15) |
| "`YOUTUBE_API_KEY` 가 없으면 이 어댑터도 끈다" | `@Conditional(KeySet)` 로 출처째 없음 | 머지 코드가 이미 함 |
| "같은 영상이 M3a5 결과로도 나오면 레코드 하나(`daehan_news` 우선)" | 같은 출처 안에서 후보를 바꿈 | 결정 1 (a) — 겹칠 일이 없다(`VideoSearch` 안전장치는 남음) |
| "검색 버킷이 없으면 건너뛴다(캐시하지 않음)" | 출처 전체 `QUOTA_SKIPPED`(그 조회의 YouTube 영상 없음) | "보여 주되 캐시하지 않음" 상태가 없다(리스크 5) |
| 정확도에 YouTube 제목 | 대한뉴스 후보 `title` = 꼭지 제목 | 호수가 연도로 읽히는 문제(리스크 13) |
| 크기 약 150, 보정 약 240, 작업 T2 하나 | 약 515, T2·T2b, 확인점 두 개 | 실측 비율(M3a5a ×2.76, M3a5 ×2.5, M3a4c ×1.8)과 머지 코드 대조 |
| T3 `./gradlew check` 3회 | 2회, 해당 범위 3회 | M3a5a 반복 규칙과 맞춤 |

## PM 결정 필요

**PM 결정 (2026-10-27)**: 아래 다섯 가지 모두 추천대로 — 1 **(a)**, 2 **(a)**, 3 **(a)**, 4 **(a)** 한 PR + 확인점 T2 뒤 누적 S1 450(넘으면 멈추고 T2b 전에 나눌지 묻는다)·T2b 뒤 650(알림), 5 **(b)** backend-dev. 구현은 T0(PM)·T0b(N-a "확인됨")가 끝날 때까지 시작하지 않는다.

이미 정해져 다시 묻지 않는 것은 바로 위 "유지" 목록이다. 이 개정본은 PM 재승인을 받아야 하고, 승인되더라도 **T0(PM)·T0b가 끝날 때까지 구현은 막혀 있다.**

| # | 물을 것 | 선택지 | 추천 |
|---|---|---|---|
| 1 | 대한뉴스를 어디에 붙일지 | (a) YouTube 출처 안의 단계 + 따로 떼어 둔 대조 부품(N-e 추가 요청 0, 캐시·30일·차단·키는 기존 그대로) / (b) 따로 출처 `daehan_news`(병렬이라 N-e 불가 → 꼭지마다 늘 검색 1회 더, TTL·30일·지우기 확장) | **(a)** — "붙는 자리" 절 |
| 2 | N-e를 어느 제목으로 대조할지 | (a) `videos.list` 제목(클라이언트 그대로, N-f 날 왕복 4번) / (b) `search.list` 제목(클라이언트 결과 모양과 기존 시험 단언이 바뀜, 왕복 3번) | **(a)** — 머지된 계약을 바꾸지 않고, 늘어난 왕복은 N-e가 실패한 1953~1994 날짜에만. 최악 시간은 마감이 막음 |
| 3 | M3a5a 리뷰 권장 1(UNITS가 소진된 날에도 검색을 예약하고 보냄)을 언제 고칠지 | (a) 이 기능 T2b에 넣음(약 +10 파일별, `YoutubeQuota` 읽기 메서드 하나) / (b) 지금 따로 작은 `fix/` / (c) 하지 않음(M6b에서 다시) | **(a)** — N-f가 검색을 하나 더 써서 같은 줄을 고친다. 지금 그 낭비는 Google 403 `quotaExceeded` 로 UNITS가 소진된 날에만 생긴다(우리 상한 8,000단위는 날짜당 1~2단위라 보통 닿지 않음) — 기다려도 된다 |
| 4 | 크기 기대값 약 515(370~570)가 400을 넘는다 — 나눌지 | (a) 한 PR + 확인점 T2 뒤 450(멈춤)·T2b 뒤 650(알림) / (b) 두 PR(색인·부품 / 연결), M3 PR 24개 / (c) N-f를 미룸(1986-09-20 제1611호가 빠짐) | **(a)** — "크기 결정 4" 설명 |
| 5 | ★ T2 색인 읽기와 대조 부품(CSV 읽기·검증, 정규식, 비율·창 계산, 레코드 다시 만들기 — 외부 호출·DB 없음, 순수 함수)을 PM이 직접 구현할지 | (a) PM이 구현(backend-dev는 그동안 할 일이 없다 — T2b가 T2를 기다림) / (b) backend-dev | **(b)** — M3에서 ★ 작업을 매번 backend-dev로 정했고, T2가 T2b의 선행이라 PM 일정에 묶인다. 직접 해 보고 싶다면 이 기능에서 가장 떼어 맡기 좋은 작업이다(시험이 쉽고 외부 호출이 없다) |
