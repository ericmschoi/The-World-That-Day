# 공용 미디어 형식과 규칙 (M3a3)

> 브랜치: `feat/event-media` · 작성: planner · 상태: **PM 승인 (2026-10-13)** — 결정 A (a) 두 PR로 나눔, B (a) 스키마 검증은 테스트 범위 유지, C (c) `chore/env-guard` 하지 않음. **확인점 결과 (2026-10-13): T2 뒤 누적 S1 505 > 450 → PM (a) T3을 별도 PR로 뺌 → M3a3a `feat/media-rules`(`docs/plans/m3a3a-media-rules.md`)**. (처음 승인 2026-10-11 "사건 사진과 공용 미디어 형식", 2026-10-13 M3a2 결과 반영 개정과 분할)
> 기준: **PM 미디어 결정 2026-10-11**(사진·영상은 링크로 처리하지 않고 화면에서 바로 보이고 재생, 라이선스 미확인 자료도 직접 표시, 비상업 — PM 선택지 (c)), 안전장치 1~6(**PM 확인**, 3은 개정판), `CLAUDE.md` 7절 1항(**촬영 시점을 모르면 "촬영 시점 미상"으로 표시하고 그날 것처럼 보이게 하지 않는다. 출처가 비면 보여 주지 않는다** — PM 결정 2026-10-11)·5항·6항, `docs/product-brief.md` 6.1절·6.2절·6.3절·12절, `docs/roadmap.md` M3, `docs/research/day-events.md` 3·4절, `docs/research/summary.md` 6절 #17·#18, `docs/research/video-sources.md` 4절·10.1절
> 선행: **M3a2 `feat/world-events-api` — 머지 완료 (PR #11, `main` e6abf8b)**. 형·메서드·설정 이름은 머지된 코드(`server/src/main/java/io/github/ericmschoi/theworldthatday/`)의 이름이다.
> 뒤따르는 기능: **M3a3a `feat/media-rules`**(촬영 시점 파서·라이선스 상태·차단과 제외 — 이 기능의 이전 T3, 이 기능 머지 뒤) → **M3a3b `feat/commons-photos`**(Commons 사진 연결 — `docs/plans/m3a3b-commons-photos.md`, M3a3a 머지 뒤), M3a4 `feat/event-video`(M3a3a 머지 뒤 시작 가능). 웹 M3b2는 이 기능 머지 뒤 병렬로 시작한다(PM 결정 A). 제공사 공식 이미지 임베드(M3a6)는 **M3에서 뺐다**(영상 15번 (a)).

## 2026-10-13 확인점 결과

| 항목 | 내용 |
|---|---|
| 측정 | T2(4d8929c) 뒤 누적 S1 **505** — 확인점 450을 넘음. T2 다시 추정은 약 365(310~480)였다 |
| PM 결정 | **(a) T3을 별도 PR로 뺀다** (2026-10-13). M3 PR 18 → **19** |
| 옮긴 것 | T3 전체 — 촬영 시점 파서, 라이선스 상태, 차단 목록·통신사 제외(`media.blocklist`·`media.excluded-credits`), M3a2 리뷰 2차 권장 1(Wikidata 주석의 취소 유예) → `docs/plans/m3a3a-media-rules.md` 의 T1·T2. 완료 기준은 그대로 옮겼다 |
| 이 계획서에 남은 작업 | T1(완료, d70cb1c·133eede), T2(완료, 4d8929c·414863c — 대조 테스트 (6) 포함), T4(완료, a56f1f5), T5(PM, PR 검수 때). **작업 번호는 바꾸지 않았다**: T3 행은 "옮김" 표시로 남기고, T4·T5는 T3에 기대지 않게 고쳤다 |
| 규칙 절 | "촬영 시점 파서", "라이선스 상태", "차단과 제외" 절은 M3a3a 계획서가 원천이다. 이 계획서에는 이름과 안내만 남겼다(두 곳에서 따로 고쳐지지 않게) |

## 2026-10-13 바뀐 점

| 바뀐 것 | 이유 |
|---|---|
| **PM 승인과 분할 (결정 A (a))** — 이 계획서는 1부 "공용 미디어 형식과 규칙"(외부 호출 없음)만 남고, 2부 "Commons 사진 연결"(이전 T5~T8)은 `docs/plans/m3a3b-commons-photos.md` 로 옮겼다(그쪽 T1~T4). 기능 이름도 "공용 미디어 형식과 규칙"으로 바꿨다 | 다시 잰 크기 약 1,290줄(최근 실측 ×2.8). PM 결정 2026-10-13 |
| 이 기능만의 PM 확인 단계(T5)·데모 기대값·크기·확인점(T2 뒤 450)을 따로 둠 | 한 PR로 스스로 끝나야 한다 |
| 미디어 조회에만 쓰는 설정(`media.max-years-apart`, `media.day-media-limit`, `media.lookup-deadline`)은 M3a3b로 옮김 | 쓰지 않는 설정을 먼저 만들지 않는다 |
| **M3a2 실제 계약으로 이름을 맞춤** (절 "M3a2에서 이어받는 계약") | `WorldStoryResponse(date, events, dayMedia, notice, partial)` 의 `dayMedia`·`Event.media` 는 `List<Object>` 빈 배열이다 → 이 기능이 `List<MediaItem>` 으로 바꾼다(응답 JSON은 그대로 `[]`). DTO마다 `@JsonNaming(SnakeCaseStrategy.class)` |
| **M3a6 제외 반영** | `image_embed` 형식(R3)과 `media.allowed-image-embed-providers`(빈 목록)는 M4·M5·다시 열 때를 위해 남긴다. M3에서는 통신사 사진이 나오지 않는다 |
| **영상 기능과의 형식 호환** (절 "뒤 기능과의 형식 호환") | M3a4·M3a4b·M3a5·M3a5b 계획(PM 승인 2026-10-12)과 리서치 10.1절이 이 형식을 전제로 한다 |
| `place_ref` 값 정리 | `country:KR` / `world` / `unknown` (+ 뒤 기능용 `<종류>:<값>`). M3a4b·M3a3b가 사건 `scope` 로 채운다 |
| 주소 필드 스키마는 `pattern` 으로만 | M3a2 `DataSchemas` 는 `format` 단언을 켠다. 한글이 든 주소가 `format: uri` 에 걸리지 않게 `^https?://\S+$` 만 쓴다 |
| 통신사 제외는 로마자 낱말 단위 | `AP` 가 `Japan` 에 걸리지 않게 (M3a3a로 옮김) |
| M3a2 리뷰 후속 | 2차 권장 1(Wikidata 주석의 취소 유예) → ~~T3~~ **M3a3a T2**(확인점 결과로 옮김). 2차 권장 2·3(테스트 스텁, `partial` 고정값) → **T4**. 1차 권장 4(`EventCuration` 손 검증 vs 스키마 라이브러리) → **결정 B (a)**: 지금은 테스트 범위 유지, M4a·M5b 계획 때 다시 |
| `.env` 교훈 | **결정 C (c)**: `chore/env-guard` 를 하지 않는다 |
| M3 PR 수 | 17 → **18** (M3a3b가 더해짐) → 확인점 결과로 **19** (M3a3a가 더해짐) |

## 2026-10-11 개정 요약 (기록)

| 이전 | 지금 |
|---|---|
| `display_mode` = `embed` / `link_only` (보여 줄지 말지) | `display_mode` = **어떻게 보여 주나**: `image` / `image_embed` / `video_file` / `video_embed` / `sdk` / `link_only`(기술적으로 막힌 경우만) |
| 라이선스가 허용 목록이 아니면 `link_only` | **라이선스와 관계없이 직접 표시**. 라이선스 상태는 `license_status`(`free`/`other`/`unknown`)로 정직하게 표시 |
| 촬영 시점을 모르면 `link_only` | 직접 표시하고 "촬영 시점 미상". **그날 찍은 것처럼 보이게 하지 않는다**(아래 "촬영 시점 미상 규칙", PM 결정 2026-10-11, `CLAUDE.md` 7절 1항) |
| `Restrictions`, 작가 누락 → `link_only` | 직접 표시. 작가가 없으면 "작가 정보 없음" |
| (없음) | 출처별 **차단 목록**(안전장치 5), 통신사·언론사 자료는 **직접 표시·핫링크 금지, 제공사 공식 임베드(`image_embed`)로만**(안전장치 3 개정), 파일 복사 금지(안전장치 1) |
| (없음) | **R9 주소 형식**: 주소 필드는 `http://`·`https://` 로 시작해야 한다(M3b1 T2 메모, 서버·웹 이중 확인) |

바뀌지 않은 것: 비자유(공정 이용) 위키 로컬 파일 제외(M3-P7), ±5년 규칙, `tragic` 사건에 미디어 없음, 6.1절 정직한 날짜 표기, 6.3절 잔혹한 이미지 금지, 출처 필드가 비면 표시하지 않음(R1).

## 안전장치 (PM 확인, 2026-10-11)

1. 미디어 파일은 우리 서버·저장소에 복사·재배포하지 않는다. 원 출처 주소(핫링크)나 원 제공자의 공식 플레이어·공식 임베드(iframe)로만 보여 준다.
2. 모든 미디어에 출처, 라이선스(모르면 "라이선스 확인 안 됨"), 촬영 시점·정밀도(모르면 "촬영 시점 미상")를 표시한다. 6.1절 정직한 날짜 표기, 6.3절 잔혹한 이미지 금지·`tragic` 규칙은 그대로다.
3. 통신사·언론사 사진(연합뉴스, AP, 게티이미지 등)을 직접 가져다 쓰는 것(핫링크 포함)은 금지한다. 제공사가 **공식 퍼가기(임베드)** 를 운영하면 그 방식으로는 허용한다. **M3에서는 그 경로(M3a6)를 뺐으므로 통신사·언론사 사진이 나오지 않는다**(2026-10-13 확인).
4. 재생·표시 경로 우선순위: 자유 라이선스(Commons, 공공누리 제1유형) → 임베드를 허용한 공식 채널 영상·제공사 공식 임베드 → 그 밖의 출처 직접 표시.
5. 권리자가 요청하면 바로 내릴 수 있게 출처별 차단 목록(설정값)을 둔다.
6. 공개 배포(#5)를 정할 때 이 결정을 다시 본다.

## 목표

1. **공용 미디어 형식**을 확정한다: `data/schema/media.schema.json` 과 서버 DTO `MediaItem`. 6.2절 필드를 모두 담고, `display_mode` 를 표시·재생 방식으로 정의하고, 영상(`video_file`, `video_embed`), 제공사 공식 이미지 임베드(`image_embed`, M3에서는 쓰지 않음), 지도 SDK(`sdk`)까지 형식에 넣는다. M3a3b·M3a4~M3a5b·M4·M5가 그대로 쓴다.
2. 서버 정규화(R1~R9)와 `date_relation`(같은 날 / 같은 달 / 같은 해 / N년 차이 / 무렵 / 현재 모습 / 미상) 계산을 만든다.
3. ~~촬영 시점 파서, 라이선스 상태, 차단 목록·통신사 제외 규칙을 출처와 무관한 공용 부품으로 만든다~~ → **M3a3a로 옮김**(확인점 결과, 2026-10-13).
4. 세계 API의 미디어 자리를 `List<MediaItem>` 형으로 바꾼다. 응답은 그대로 `media: []`, `day_media: []`(사진은 M3a3b가 채운다).

## 범위 밖

- **촬영 시점 파서, 라이선스 상태 판정, 차단 목록·통신사 제외, Wikidata 주석 고침(M3a2 리뷰 2차 권장 1)** → M3a3a `docs/plans/m3a3a-media-rules.md` (확인점 결과)
- **Commons·PageImages 호출, 사건 사진·날 사진 고르기, 미디어 조회 시간 상한, `partial` 의 미디어 실패** → M3a3b
- 영상 고르기·만들기 → M3a4~M3a5b (형식은 여기서 정한다)
- 제공사 공식 이미지 임베드 만들기 → **M3에서 제외**(영상 15번 (a)). 형식(R3)만 남긴다
- 지도 SDK 미디어 만들기 → M4 (`render` 형식만 정한다)
- 미디어 파일 복사·프록시·재배포 (안전장치 1)
- 런타임에서 `data/` 미디어 파일 읽기, 스키마 검증 라이브러리를 런타임에 쓰기 → M4a·M5b 계획 때 다시(결정 B (a))
- `.env` 값 점검 → 하지 않음(결정 C (c))
- 프론트엔드 → M3b2·M3b2b·M3b3·M3b3c

## 사용할 외부 출처

이 기능은 외부를 부르지 않는다. T1의 실제 예시 2개는 content-curator가 Commons 파일 설명 페이지(**확인됨** 출처, [day-events.md 3절](../research/day-events.md#3-출처-카드))를 열어 값을 옮긴 것이고 파일은 내려받지 않는다.

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| #1 | 비상업 | 결정 |
| #4 (개정) | 사진·영상 모두 링크로 처리하지 않고 바로 표시·재생 | 결정 2026-10-11 |
| 미디어 표시 | 라이선스 미확인도 직접 표시 (PM 선택지 (c)) | 결정 2026-10-11 |
| 촬영 시점 미상 | 표시하되 "촬영 시점 미상"으로, 그날 것처럼 보이게 하지 않는다. 출처가 비면 숨긴다 | 결정 2026-10-11 |
| 안전장치 1~6 | 위 절 | PM 확인 2026-10-11 |
| #17 | (a) `current` 유지, "현재 모습(촬영 시점 미제공)" | 결정 |
| #18 (개정) | 아래 "필수 필드 규칙" | 2026-10-11 결정에 맞춰 바뀜 |
| M3a6 | M3에서 제외. `image_embed` 허용 목록은 빈 채로 | 결정 2026-10-11 |
| 영상 1~4·21·22번 | 늦은 촬영일은 "촬영 시점 미상", 10분 상한, 언론사 자유 라이선스 영상 허용 + 차단 목록, 재생 실패는 숨김 — 이 형식이 받쳐야 한다 | 결정 2026-10-11 |
| **A·B·C** | (a) 두 PR / (a) 스키마 검증 테스트 범위 유지 / (c) `chore/env-guard` 안 함 | **결정 2026-10-13** |
| **확인점** | S1 505 > 450 → (a) T3을 별도 PR(M3a3a)로 | **결정 2026-10-13** |

## M3a2에서 이어받는 계약 (머지된 코드 기준)

| 자리 | 지금 (M3a2) | 이 기능에서 |
|---|---|---|
| 응답 DTO | `story/world/WorldStoryResponse(String date, List<Event> events, List<Object> dayMedia, Notice notice, boolean partial)`, `Event(…, List<Object> media, String retrievedAt)`, `Event.of(KoWikiEvent, Verdict)`. DTO마다 `@JsonNaming(SnakeCaseStrategy.class)`(전역 설정 없음) | `dayMedia`·`media` 를 `List<MediaItem>` 으로(값은 여전히 빈 목록). `MediaItem` 과 안의 작은 형(`VideoSource`, `Render`, `DateRelation`)에도 **각각** `@JsonNaming(SnakeCaseStrategy.class)` |
| 에러 | `api/ApiProblem` → ProblemDetail + `code` | 바뀌지 않음. 새 에러 코드 없음 |
| 데이터 폴더 | `config/AppProperties(@DefaultValue("../data") String dataDir)`, `dataPath()` | 런타임에 `data/` 를 읽지 않는다(결정 B (a)) |
| 스키마 검증 | 테스트 전용 `server/src/test/java/…/data/DataSchemas`(2020-12, 원격 읽기 끔, `format` 단언 켬, `../data` 기준), `testImplementation("com.networknt:json-schema-validator:3.0.6")` | `media.schema.json` 검증에 그대로 재사용. 범위는 테스트 그대로(결정 B (a)) |
| 설정 묶음 | `WikimediaProperties`·`WikidataProperties`(접두사 `wikimedia`, 값 검증 포함) | 같은 방식의 `MediaProperties`(접두사 `media`) |
| Wikidata 주석 | `SparqlWikidataSignalSource`(5초 상한 + 최대 1초 취소 유예 `CANCEL_GRACE`)의 주석이 "읽기 타임아웃 안에 끝남"이라 적음 | ~~T3~~ → **M3a3a T2**에서 주석만 고친다 |

## 공용 미디어 형식

### 필드 (저장 형식 = 스키마, API는 `date_relation` 만 더한다)

★ = 6.2절에 없던 필드.

| 필드 | 형 | 설명 |
|---|---|---|
| `id` | string | 예 `commons-12345678`(`pageid`), `youtube-<영상 ID>`, `getty-<자산 ID>` |
| `type` | `photo` \| `video` \| `aerial` \| `satellite` \| `streetview` | 무엇인가 |
| `display_mode` | `image` \| `image_embed` \| `video_file` \| `video_embed` \| `sdk` \| `link_only` | **어떻게 보여 주나**. `image`: `<img>`(원 출처 핫링크). `image_embed`: 제공사 공식 이미지 임베드 iframe(통신사·언론사는 이것만, 안전장치 3 — **M3에서는 만들지 않음**). `video_file`: `<video>` 로 원 출처 파일 재생(핫링크). `video_embed`: 원 제공자 공식 플레이어 iframe. `sdk`: 지도 SDK(M4). `link_only`: 원 출처가 핫링크·임베드를 **기술적으로** 막은 경우만(R7) |
| `url` | string \| null | 원본 파일 주소 (`image`, `video_file`) |
| `thumbnail_url` | string \| null | 화면용 이미지 주소. `image` 는 필수, 영상은 포스터(선택) |
| ★ `thumbnail_width`, `thumbnail_height` | integer \| null | 크기(레이아웃 흔들림 방지). `image_embed`·`video_embed` 는 iframe 비율에 쓴다 |
| ★ `video_sources` | array \| null | `video_file` 재생 후보 `[{"url":…, "mime":"video/webm", "height":480}]` (작은 것 먼저 — 순서는 M3a4b) |
| ★ `duration_seconds` | number \| null | 영상 길이 |
| ★ `embed_url` | string \| null | `image_embed`·`video_embed` 의 iframe 주소(서버가 허용 제공자 형식으로 만든다) |
| ★ `provider` | string \| null | `image_embed`·`video_embed`·`sdk` 의 제공자. 예 `youtube`, `getty`, `kakao` |
| ★ `render` | object \| null | `sdk` 용 `{"provider":"kakao","kind":"roadview"\|"skyview","lat":…,"lng":…,"zoom":…}` |
| ★ `link_only_reason` | `hotlink_blocked` \| `embed_disabled` \| null | `link_only` 일 때만 |
| `source_name` | string | 제공 기관·채널 |
| `source_url` | string | 원문 페이지 |
| `license` | string \| null | 원 출처에 적힌 라이선스 이름 그대로. 모르면 `null`. 제공사 임베드는 `provider-terms` |
| ★ `license_status` | `free` \| `other` \| `unknown` | "라이선스 상태"(판정 규칙은 M3a3a). `unknown` → 화면 "라이선스 확인 안 됨" |
| ★ `license_url` | string \| null | |
| `attribution` | string | 화면 출처 문구. 작가를 모르면 "작가 정보 없음" |
| ★ `description` | string \| null | 무엇이 찍혔는지(대체 텍스트·iframe `title`) |
| `captured_at` | string \| null | `YYYY-MM-DD` / `YYYY-MM` / `YYYY`. 모르면 `null` |
| `date_precision` | `day` \| `month` \| `year` \| `approx` \| `current` \| null | `null` = 촬영 시점 미상 |
| `lat`, `lng` | number \| null | 좌표(둘 다 있거나 둘 다 `null`) |
| `place_ref` | string \| null | `country:KR`(국내), `world`(국외), `unknown`(모름), 뒤 기능은 `<종류>:<값>`(예 `region:…`, `landmark:…` — M2·M4·M5가 정함). 스키마 패턴 `^(world\|unknown\|[a-z]+:\S+)$` |
| `retrieved_at` | string | 가져온 시각(ISO-8601 UTC 초 단위, M3a2 공통 규약) |
| `date_relation` (**API만**) | object | 아래. 늘 `kind` 와 `years` 둘 다 있다 |

API 전용 필드는 저장 형식(스키마)에 없다. 스키마 검증은 API 전용 필드를 뺀 뒤에 한다. 지금은 `date_relation` 하나이고, M3a4가 `selection` 을 더한다. 값이 `null` 인 필드도 키는 늘 응답에 있다.

### 필수 필드 규칙 (#18 개정 — 스키마와 서버 정규화가 같은 규칙)

| 규칙 | 내용 |
|---|---|
| R1 언제나 | `id`, `type`, `display_mode`, `source_name`, `source_url`, `attribution`, `license_status`, `retrieved_at` 이 있다. 위치는 (`lat`·`lng`) 또는 `place_ref`. **`license` 는 비어도 된다. `captured_at`·`date_precision` 도 비어도 된다**("촬영 시점 미상"으로 표시). 출처 필드(`source_name`, `source_url`, `attribution`)가 비면 버린다(`CLAUDE.md` 7절 1항) |
| R2 `image` | `url`, `thumbnail_url` 이 있다. `source_name`·`attribution`(그리고 원문 `Artist`·`Credit`)이 통신사·언론사 제외 목록에 걸리면 안 된다(안전장치 3 — **이 제외 검사는 M3a3a의 차단·제외 필터가 한다.** 정규화는 필드 존재만 본다) |
| R3 `image_embed` | `embed_url`, `provider` 가 있고 `provider` 가 허용 이미지 임베드 제공자 목록(설정값 `media.allowed-image-embed-providers`, **빈 목록 — M3a6 제외로 M3 동안 비어 있음**)에 있다. `url`·`thumbnail_url` 은 `null` |
| R4 `video_file` | `url` 또는 `video_sources`(1개 이상) |
| R5 `video_embed` | `embed_url`, `provider` 가 있고 `provider` 가 허용 영상 임베드 제공자 목록(설정값 `media.allowed-video-embed-providers`, 처음은 빈 목록 — M3a5가 `youtube` 를 더함)에 있다. `url` 은 `null` |
| R6 `sdk` | `render`, `provider` 가 있고 `url`·`thumbnail_url` 은 `null` |
| R7 `link_only` | 원 출처가 핫링크·임베드를 기술적으로 막았다고 확인된 경우만: `link_only_reason` 이 있고 `url`·`thumbnail_url`·`embed_url` 은 `null`. M3의 Commons 사진·영상에서는 만들지 않는다(막히면 보고 — M3a4b) |
| R8 정밀도와 형식 | `day` → `YYYY-MM-DD`, `month` → `YYYY-MM`, `year`·`approx` → `YYYY`, `current`·`null` → `captured_at` 은 `null` |
| R9 주소 형식 | 값이 있는 `source_url`, `url`, `thumbnail_url`, `embed_url`, `video_sources[].url` 은 `http://` 또는 `https://` 로 시작한다(스키마 `pattern` `^https?://\S+$`, `format: uri` 는 쓰지 않음). 아니면 버린다. `license_url` 만은 형식이 틀리면 레코드를 버리지 않고 `null` 로 바꾼다(라이선스 링크만 빠진다) |

정규화: R2~R7, R9를 못 채운 미디어는 **버린다**(다른 모드로 바꾸지 않는다). R1을 못 채워도 버린다. API에 나가는 미디어는 늘 스키마를 통과한다. 허용 제공자 목록·차단 목록·제외 목록은 설정값이라 서버만 검사한다(스키마는 `provider` 를 문자열로만 본다). 차단 목록·제외 목록 검사는 M3a3a 필터가 하고, 부르는 쪽(M3a3b·M3a4)이 정규화 앞에서 부른다.

### 촬영 시점 미상 규칙 (PM 결정 2026-10-11)

| 규칙 | 내용 |
|---|---|
| 표시 | `date_precision` 이 `null` 이면 `date_relation.kind = unknown`. 숨기지 않는다. 화면은 배지 없이 "촬영 시점 미상"(M3b1 문구 표) |
| 날짜를 옮기지 않음 | 사건 날짜(P585), 위키백과 줄 날짜, **Commons 날짜 범주**(`…photographs taken on <날짜>`), 업로드·게시일을 `captured_at` 으로 옮기지 않는다. 날짜 범주에서 온 파일이라도 파일 자체의 촬영 시점을 읽지 못하면 `unknown` 이다 |
| `day_media` 순서 | `same_day` 먼저(원래 순서), 그다음 그 밖의 `kind`, `unknown` 은 맨 뒤(M3a3b가 적용). 웹(M3b2b)은 `same_day` 만 "그날 찍힌 사진" 아래에 둔다(M3-P17 (a)) |
| ±5년 규칙 | `unknown` 은 연도 차이를 계산할 수 없다. 사건 미디어로 붙일 수 있지만 "촬영 시점 미상"으로만 보인다(M3a3b) |
| 출처 없음 | R1에 따라 버린다 |

### 촬영 시점 파서 → M3a3a

**M3a3a로 옮김**(확인점 결과, 2026-10-13). 원문 모양 표와 규칙은 `docs/plans/m3a3a-media-rules.md` "촬영 시점 파서"가 원천이다. 내용은 바뀌지 않았다(입력은 출처와 무관한 문자열 하나, 출력은 `captured_at`·`date_precision`, 못 읽으면 둘 다 `null`).

### 라이선스 상태 → M3a3a

**M3a3a로 옮김**. 이름 예 표는 `docs/plans/m3a3a-media-rules.md` "라이선스 상태"가 원천이다(`free`/`other`/`unknown`, 내용은 바뀌지 않음).

### 차단과 제외 (안전장치 3·5) → M3a3a

**M3a3a로 옮김**. 차단 목록(`media.blocklist`)·통신사·언론사 직접 표시 금지(`media.excluded-credits`, 로마자 낱말 단위)는 `docs/plans/m3a3a-media-rules.md` "차단과 제외"가 원천이다. 비자유 위키 로컬 파일 제외(M3-P7)는 그대로 M3a3b가 적용한다.

### `date_relation` (API만)

| `kind` | 조건 (요청 D, 촬영 C, 정밀도 P) | `years` |
|---|---|---|
| `same_day` | P=`day`, C=D | 0 |
| `same_month` | P가 `day`/`month`, 연·월 같음(same_day 제외) | 0 |
| `same_year` | P가 `day`/`month`/`year`, 연도 같음(위 제외) | 0 |
| `years_apart` | P가 `day`/`month`/`year`, 연도 다름 | C 연도 − D 연도 |
| `approximate` | P=`approx` | C 연도 − D 연도 |
| `current` | P=`current` | 0 |
| `unknown` | P=`null` | 0 |

### 예 (값은 예시)

```json
{
  "id": "commons-12345678", "type": "photo", "display_mode": "image",
  "url": "https://upload.wikimedia.org/wikipedia/commons/…/Example.jpg",
  "thumbnail_url": "https://upload.wikimedia.org/wikipedia/commons/thumb/…/640px-Example.jpg",
  "thumbnail_width": 640, "thumbnail_height": 427,
  "video_sources": null, "duration_seconds": null, "embed_url": null, "provider": null,
  "render": null, "link_only_reason": null,
  "source_name": "Wikimedia Commons", "source_url": "https://commons.wikimedia.org/wiki/File:Example.jpg",
  "license": "Public domain", "license_status": "free", "license_url": null,
  "attribution": "U.S. Air Force · Public domain · Wikimedia Commons",
  "description": "1988년 서울 올림픽 개막식",
  "captured_at": "1988-09-17", "date_precision": "day",
  "lat": null, "lng": null, "place_ref": "country:KR",
  "retrieved_at": "2026-10-13T03:00:00Z",
  "date_relation": {"kind": "same_day", "years": 0}
}
```

라이선스 미확인 사진: `"license": null, "license_status": "unknown", "attribution": "작가 정보 없음 · 라이선스 확인 안 됨 · Wikimedia Commons"` — 그래도 `display_mode: image` 로 직접 표시.

촬영 시점 미상 사진: `"captured_at": null, "date_precision": null, "date_relation": {"kind": "unknown", "years": 0}` — 직접 표시, 화면 "촬영 시점 미상", "같은 날" 배지 없음.

### 뒤 기능과의 형식 호환 (2026-10-13)

| 기능 | 이 형식에서 쓰는 것 | 이 기능이 지킬 것 |
|---|---|---|
| **M3a3a 공용 규칙** (확인점 결과로 생김) | `MediaRecord`(`LicenseStatus`·`DatePrecision` 열거형), `MediaNormalizer`(R8 대조), `MediaProperties`(설정 추가) | 공개 형으로 둔다. M3a3a는 정규화를 고치지 않고 별도 부품을 더한다 |
| **M3a3b Commons 사진** | `MediaItem`, 정규화, `date_relation` + M3a3a의 파서, 라이선스 상태, 차단·제외 | 모두 공개 형·메서드로(패키지 밖에서 쓸 수 있게). 정규화는 요청 날짜를 받아 `date_relation` 까지 채운다 |
| M3a4 검색 틀 | API 전용 `selection`(`basis`, `score`)을 `date_relation` 처럼 더함. M3a3a 파서(영어 서수는 M3a4 T2가 보강), 차단 목록, 제외 목록(`image`·`video_file` 에만), R9 | 파서는 출처와 무관한 문자열 입력(M3a3a). 정규화가 API 전용 필드를 몰라도 되게 DTO를 둔다 |
| M3a4b Commons 동영상 | `video_sources`, `place_ref` 는 사건 `scope` 기준, Commons 파일 매퍼(M3a3b) | 촬영일이 사건 날짜보다 늦으면 비우는 규칙(R-V3)은 M3a4b가 더한다 |
| M3a5 YouTube | `video_embed`, `provider: youtube`(허용 목록에 더함), `license: provider-terms` → `other`, `license_url` | `date_relation` 은 늘 `years` 를 함께 낸다. **M3a5 계획서의 계약 예시는 `{"kind": "same_year"}` 로 `years` 를 빠뜨렸다** — 실제 응답에는 `years: 0` 이 있다(M3a5 계획 다음 개정 때 예시를 맞춘다) |
| M3a5b 대한뉴스 | `approx`(R8로 `captured_at` 은 연도), `attribution` 에 "대한뉴스(정부 제작 뉴스영화)"와 "… 자" | `attribution` 은 자유 문자열(길이 제한 없음) |
| M3b2·M3b2b 웹 | `web/src/media/types.ts`(M3b1)와 대조 | T4 보고의 DTO 필드 목록. 웹 타입의 `MediaSelection.basis` 목록이 M3a4 개정보다 오래됐다 — M3b2 T1에서 고친다. **M3a3a는 DTO 필드를 바꾸지 않으므로 M3b2는 M3a3a를 기다리지 않는다** |
| M4·M5 | `sdk`·`render`, `aerial`·`satellite`·`streetview`, 큐레이션 미디어 자료(`data/`)를 이 스키마로 | `additionalProperties: false` + 저장 필드만. 런타임 스키마 검증은 그때 다시 정한다(결정 B (a)) |

## API / 데이터 변경

- `GET /api/story/world`: JSON은 바뀌지 않는다(`media`·`day_media` 는 `[]`). 서버 형만 `List<Object>` → `List<MediaItem>`.
- 설정 `media.*`(`MediaProperties`): `allowed-image-embed-providers: []`, `allowed-video-embed-providers: []`(T2). 잘못된 값은 시작 실패. (`blocklist`, `excluded-credits` 는 M3a3a, `max-years-apart`, `day-media-limit`, `lookup-deadline` 은 M3a3b)
- 데이터: `data/schema/media.schema.json`, `data/schema/examples/media/{valid,invalid}/*.json`, `data/schema/examples/media/README.md`(T1). 런타임은 읽지 않는다.
- DB 변경 없음.

## 테스트 방침

- M3a1 "테스트 방침"을 따른다. 이 기능은 외부를 부르지 않는다.
- 규칙 표의 행마다 합성 레코드로 단위 테스트. 날짜는 데모·경계값(1988-09-17, 2002-06-22, 2000-02-29)만 쓴다(`CLAUDE.md` 7절 6항).
- 스키마 검증은 M3a2의 `DataSchemas`(테스트 전용) + `json-schema-validator` 3.0.6으로 `valid/`·`invalid/` 예시 전부. 같은 예시를 Java 정규화에도 넣어 두 구현이 같은 결론인지 본다(T2 (6) 대조 테스트 — 결정 B (a)의 조건).

## 데모 입력 3개로 확인할 것

이 기능은 사진을 붙이지 않는다. 데모로 보는 것은 "응답이 바뀌지 않았다"와 "실제 예시 자료가 형식을 통과한다"이다.

| 데모 입력 | 확인 |
|---|---|
| 1988-09-17 + "서울" | 응답은 M3a2와 같다: 사건 3개(`1988년 하계 올림픽`·`KBS 1TV`·`KBS 2TV`, 모두 `normal`/`curated`), 각 `media` = `[]`, `day_media` = `[]`, `partial: false`. 실제 예시 `valid/` 의 1988-09-17 PD 사진이 정규화·스키마를 통과하고, 요청 날짜 1988-09-17 기준 `date_relation` 이 `{"kind":"same_day","years":0}`, `license_status: free`(예시 파일에 적힌 값 그대로 — 판정 부품은 M3a3a) |
| 2002-06-22 + "광주 서구 풍암동" | 응답은 M3a2와 같다: 사건 1개(`2002년 FIFA 월드컵`), `media`·`day_media` = `[]`. 실제 예시 `valid/` 의 2002-06-22 KOGL 제1유형 사진이 통과하고 `license_status: free`, 2002-06-22 기준 `same_day` |
| 1995-03-15 + "서울대학교병원" | 응답은 M3a2와 같다: `events` = `[]`, `day_media` = `[]`, `notice.code` = `NO_EVENTS_FOUND`, `partial: false` |

## 크기 (400줄 규칙)

제품 코드(S1) 기준. "이전 방식"은 2026-10-11 셈법, "다시 추정"은 최근 실측 비율의 가운데 값 ×2.8(M3a1b 약 2.4배, M3a2 약 2.8배, M3a1c 약 3.7배), 괄호는 ×2.4~×3.7.

| 작업 | 내용 | 이전 방식 | 다시 추정 | 실제 |
|---|---|---|---|---|
| T2 | `MediaItem` DTO(작은 형 3개)·정규화 R1~R9·`date_relation`·`MediaProperties`(허용 제공자, 검증)·응답 형 바꾸기 | 약 130 | 약 365 (310~480) | **505** (4d8929c, 이전 방식의 약 3.9배) |
| ~~T3~~ | ~~촬영 시점 파서·라이선스 상태·차단 목록·제외 목록(낱말 단위)·주석 고침~~ → **M3a3a** | ~~약 130~~ | ~~약 365~~ | - |
| **합계 (이 PR)** | | **약 130** | **약 365** | **505 + T4 뒤 남은 변경(테스트만이라 S1은 거의 그대로)** |

- **중간 확인점 결과: T2 뒤 누적 S1 505 > 450** → PM이 (a) T3 분리를 골랐다(2026-10-13). 이 PR은 400을 넘지만 PM 결정 A (a)와 확인점 결정으로 한 PR이다.
- 테스트(S2)는 따로 보고한다.

## 작업 목록

순서: T1과 T2는 병렬(완료, T2의 대조 테스트 (6)은 T1 뒤 — 완료 414863c) → **확인점(결과: 505, (a) T3 분리)** → T4(완료 a56f1f5) → T5. **작업 번호는 그대로**이고 T3 행은 옮긴 표시만 남긴다.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 공용 미디어 스키마와 예시 | content-curator | `data/schema/media.schema.json`, `data/schema/examples/media/{valid,invalid}/*.json`, `data/schema/examples/media/README.md` | - (M3a2 머지 완료) | (1) JSON Schema 2020-12(`$schema` 가 `https://json-schema.org/draft/2020-12/schema`), 필드 표 전부(`date_relation` 등 API 전용 필드 제외)와 R1~R9, `additionalProperties: false`. 주소 필드는 `pattern` `^https?://\S+$` 만(`format: uri` 쓰지 않음), `retrieved_at` 은 `format: date-time`, `place_ref` 는 필드 표의 패턴. 허용 제공자 목록은 서버 설정이라 스키마는 `provider` 를 문자열로만 검사. (2) `valid/` 최소 8개: Commons 실제 사진 2개(1988-09-17 PD 1, 2002-06-22 KOGL 제1유형 1 — 파일 설명 페이지를 열어 확인), `license: null`·`unknown` 사진 1개(합성), **`captured_at`·`date_precision` 이 null인 사진 1개(합성)**, `image_embed`·`video_file`·`video_embed`·`sdk` 각 1개(합성, `id` 가 `example-` 로 시작). `video_embed` 예시는 M3a5 계약 예시와 같은 모양(`selection`·`date_relation` 은 빼고). (3) `invalid/` 최소 10개, 각각 규칙 하나만 어김(R1~R9, R1은 `source_url` 빈 값 1개 포함, R9는 `thumbnail_url` 이 `javascript:` 로 시작하는 것 1개). (4) README에 파일마다 어기는 규칙 또는 실제 출처 URL·확인 일자. (5) C1이 모든 파일에 `ok`. 미디어 파일은 내려받지 않는다 — **완료 (d70cb1c, valid 10 / invalid 17)** |
| T2 | 미디어 형식 DTO·정규화·`date_relation`·허용 제공자 설정, 응답 형 바꾸기 | backend-dev | `media` 패키지(`MediaRecord`·`MediaItem` + `VideoSource`·`Render`·`DateRelation`, `MediaNormalizer`), `MediaProperties`(`media.allowed-image-embed-providers`·`media.allowed-video-embed-providers` 빈 목록), `WorldStoryResponse`·`Event` 의 미디어 형을 `List<MediaItem>` 으로, `application.yml`, 테스트 | - (대조 테스트 (6)은 T1 뒤) | (1) `cd server && ./gradlew check` 성공. (2) `MediaItem` 을 직렬화한 JSON 키가 필드 표의 저장 필드 전부 + `date_relation` 이고 모두 snake_case(안쪽 `video_sources[].mime`, `render.kind`, `date_relation.years` 포함), 값이 `null` 인 키도 빠지지 않는다. `MediaItem` 과 작은 형 3개 각각에 `@JsonNaming(SnakeCaseStrategy.class)`. (3) `date_relation`: D=1988-09-17 기준 7가지 `kind`, 경계값 D=2000-02-29(`2000-02-01` → `same_month`, `1999-02-28` → `years_apart` −1). (4) 정규화: R2~R7·R9를 못 채우면 버림(모드를 바꾸지 않음), `license` null은 통과, **`captured_at`·`date_precision` null은 통과하고 `kind` = `unknown`**, `source_name`·`source_url`·`attribution` 이 비면 버림, `license_url` 이 `http(s)` 가 아니면 레코드는 남고 `license_url` 만 `null`, R8(정밀도와 `captured_at` 모양이 어긋나면 버림). (5) 허용 제공자: 기본 설정(빈 목록)에서 `image_embed`·`video_embed` 는 모두 버려지고, 시험 설정에 `example` 을 넣으면 남는다. (6) **스키마 대조**: `valid/` 예시 전부가 정규화를 그대로 통과하고, 다시 쓴 JSON(`date_relation` 제외)이 `DataSchemas` 로 스키마를 통과한다. `invalid/` 예시 전부가 정규화에서 버려지거나 읽기에서 거부된다(모르는 필드). (7) 기존 세계 API 테스트를 고치지 않고 통과(`media`·`day_media` 는 여전히 `[]`). (8) 허용 제공자 목록에 빈 문자열 항목 → 컨텍스트 시작 실패. (9) 보고서에 T2 S1과 누적 S1 — **확인점 450 (결과: 505 → PM (a) T3 분리)**. 상태: (1)~(5)·(7)~(9) 완료(4d8929c, 610 tests), (6) 진행 중 |
| T3 | ~~촬영 시점 파서, 라이선스 상태, 차단 목록·통신사 제외, M3a2 리뷰 2차 권장 1~~ | - | - | - | **M3a3a로 옮김** (확인점 결과, PM 결정 (a) 2026-10-13). 내용과 완료 기준은 `docs/plans/m3a3a-media-rules.md` T1(파서·라이선스 상태)·T2(차단·제외·Wikidata 주석). 이 PR에서는 하지 않는다 |
| T4 | 완료 기준 검증, M3a2 리뷰 2차 권장 2·3, 전체 테스트 | test-engineer | 빠진 기준의 테스트, `media.schema.json` 스키마 테스트, `WorldStoryCuratedHideTest`·`CuratedEventToneJudgeTest` 수정(테스트 파일만) | **T1, T2** (T2 (6) 대조 테스트 포함) | (1) **T1·T2** 기준을 표로 확인하고 빠진 테스트를 추가. (2) 스키마 테스트: `media.schema.json` 이 2020-12 메타 스키마를 통과(`DataSchemas.metaSchemaErrors` 비어 있음), `valid/` 전부 통과, `invalid/` 전부 실패하고 각 파일의 오류가 README에 적힌 규칙의 필드를 가리킨다. (3) **2차 권장 2**: `WorldStoryCuratedHideTest` 의 Wikidata 스텁이 빈 맵 대신 **물어본 QID마다** `Signals.NONE` 을 돌려준다(`CuratedEventToneJudgeTest` 의 스텁과 같은 방식). 기존 단언은 그대로 통과. (4) **2차 권장 3**: `CuratedEventToneJudgeTest.takedownWinsOverAToneEntryForTheSameEvent` 가 `partial` 을 `day.degraded()` 대신 상태별 고정값으로 단언한다 — 연도 문서 `FRESH` → `false`, `STALE` → `true`. (5) "데모" 표 3행(응답 그대로 + 실제 예시 2개의 `date_relation`·`license_status`). (6) `cd server && ./gradlew check` 통과(개수 보고), 3회 반복 안정. (7) S1·S2·S3 보고. (8) **서버 미디어 DTO 필드 목록(이름·형·null 가능)** — M3b2 T1이 `web/src/media/types.ts` 와 대조한다. (이전 (9) C4 확인은 M3a3a로 옮겼다 — Wikidata 주석은 이 PR에서 고치지 않는다) |
| T5 | 형식 검토와 실행 확인 (PR 검수 때) | PM | - | T4 | (1) T4 (8)의 DTO 필드 목록이 이 계획서의 필드 표와 같다(다르면 PR 코멘트). (2) `valid/` 의 실제 예시 2개의 `source_url` 을 브라우저로 열어 라이선스가 `license` 와 같다. (3) `docker compose up -d db`, `cd server && ./gradlew bootRun` 후 P1 결과가 "데모" 표와 같다(`media`·`day_media` 가 `[]`, 사건 필드는 M3a2와 같음) |

완료 기준에 쓰는 명령 (저장소 루트):

```
# C1 — 스키마·예시가 JSON으로 읽히는지 (content-curator). 파일마다 ok
for f in data/schema/media.schema.json data/schema/examples/media/*/*.json; do
  python3 -c 'import json,sys; json.load(open(sys.argv[1])); print("ok", sys.argv[1])' "$f"; done

# (C4 — Wikidata 주석 확인은 M3a3a 계획서로 옮겼다)

# S1 — 제품 코드·설정 줄 수
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
# S3 — 데이터 파일 줄 수
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P1 — PM 실행 확인 (서버 실행 중). 미디어 자리가 빈 배열이고 사건이 M3a2와 같은지
for d in 1988-09-17 2002-06-22 1995-03-15; do
  curl -s "http://localhost:8080/api/story/world?date=$d" \
    | jq '{date, partial, notice, day_media, events: [.events[] | {title, tone, media}]}'; done
```

담당별: content-curator 1 (T1), backend-dev 1 (T2), test-engineer 1 (T4), PM 1 (T5). 모두 4개(T3은 M3a3a로 옮김).

## 리스크와 확인할 것

1. **크기** — T2 실제 505로 확인점 450을 넘었고 T3을 M3a3a로 뺐다. 이 PR의 S1은 약 505에서 거의 늘지 않는다(T4는 테스트만).
2. **스키마와 Java 정규화의 중복** — 결정 B (a)로 두 구현이 함께 산다. T2 (6) 대조 테스트가 어긋남을 잡는다. M4a·M5b 계획 때 런타임 스키마 검증을 다시 정한다.
3. **통신사 표기 판별·`DateTimeOriginal` 형식** — M3a3a 리스크로 옮겼다.
4. **이 PR만으로는 공용 규칙이 반쪽이다** — 파서·라이선스 판정·차단·제외가 없어 M3a3b·M3a4가 아직 시작할 수 없다. M3a3a가 바로 뒤에 온다. 응답이 `[]` 라 사용자에게 보이는 영향은 없다.
5. **웹 타입과 어긋남** — `web/src/media/types.ts` 와 DTO가 다를 수 있다(지금 보이는 차이: `MediaSelection.basis` 목록). T4 (8)로 M3b2 T1에서 맞춘다. M3a3a는 DTO를 바꾸지 않는다.
6. **M3a5 예시의 `date_relation`** — `years` 가 빠져 있다. 서버는 늘 내므로 웹 fixture를 그 예시로 만들면 어긋난다. M3a5 계획 다음 개정 때 맞춘다.
7. **M3a3b와 M3a4가 같은 연결 자리를 고친다** — 둘 다 M3a3a 뒤에 시작할 수 있지만 `WorldStoryService` 의 미디어 연결을 함께 고친다. 병렬이면 worktree를 나누고 오케스트레이터가 합친다(로드맵 진행 순서).

## PM 결정 (2026-10-13, 기록)

| # | 물은 것 | 결정 | 이 계획서에 반영 |
|---|---|---|---|
| A | M3a3(다시 추정 약 1,290줄)을 지금 나눌지 | **(a) 두 PR** — 이 기능(1부 형식·규칙)과 M3a3b `feat/commons-photos`(2부 Commons 사진). planner 추천과 같음 | 2부를 `docs/plans/m3a3b-commons-photos.md` 로 옮김. 이 기능에 PM 확인(T5)·데모·크기·확인점(T2 뒤 450) 추가. M3 PR 18개 |
| B | 런타임 데이터 검증에 `json-schema-validator` 를 쓸지 (M3a2 리뷰 1차 권장 4) | **(a) 지금은 테스트 범위 유지**, 첫 런타임 큐레이션 미디어 자료(M4a·M5b) 계획 때 다시 | 정규화는 Java, T2 (6) 대조 테스트. `EventCuration` 은 그대로 |
| C | `.env` 값 점검 chore(`chore/env-guard`) 시점 | **(c) 하지 않음** — 점검 코드를 만들지 않는다 | 이 계획서 범위 밖. 로드맵 후속 후보에 "PM: 하지 않음 (2026-10-13)" |
| 확인점 | T2 뒤 누적 S1 **505** > 450 — T3을 별도 PR로 뺄지 | **(a) 뺀다** — M3a3a `feat/media-rules` | T3을 `docs/plans/m3a3a-media-rules.md` 로 옮김. T4·T5는 T3에 기대지 않음. M3 PR 19개 |

이전 결정(M3-P7, M3-P16 변경, M3-P17 (a), M3a6 제외, 영상 1~4·21·22번)은 위 "전제" 표와 로드맵 M3 절. 남은 질문은 없다.
