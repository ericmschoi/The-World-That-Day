# 그날의 세계 API: 엔드포인트, 날짜 검증, 에러 형식, tone (M3a2)

> 브랜치: `feat/world-events-api` · 작성: planner · 상태: PM 승인 (2026-10-11) · 2026-10-11 갱신: M3a1 분할로 선행이 M3a1c로 바뀜, 대표 링크 QID 조회(pageprops)를 M3a1c로 옮김 · **2026-10-12 개정 PM 승인 (2026-10-12): M3-P16 변경 — 원문을 거르지 않음(PM 결정 2026-10-12), M3a1c 실제 계약·데모 결과 반영, M3a1c 리뷰 후속(T2b, T4), 크기 다시 추정, 분할 없이 한 PR((b), 중간 확인점 T2·T2b 뒤 누적 S1 360)**
> 기준: `CLAUDE.md` 7절, `docs/product-brief.md` 3절·5절 4번·6.3절·8절·12절, `docs/roadmap.md` M3, `docs/research/day-events.md` 3·4·5·8·10절, `docs/research/summary.md` 6절 #20~#23
> 선행: M3a1 `feat/wiki-events-source`, M3a1b `feat/wiki-section-cache`(호출 보강·캐시), **M3a1c `feat/wiki-events-merge`(`KoWikiEventSource`: 중복 제거, 링크 정규화와 대표 링크 QID, 날짜당 시간 상한) — 머지 완료 (PR #10, `main` c2f1a5f)**. 이 기능은 그 `main` 에서 갈라졌다. 이 계획서의 형·메서드 이름은 머지된 코드(`server/src/main/java/io/github/ericmschoi/theworldthatday/event/kowiki/`)의 이름이다.

## 2026-10-12 바뀐 점

| 바뀐 것 | 이유 |
|---|---|
| **M3-P16 변경: 원문을 거르지 않는다.** 2026-10-11 (b)의 구조 필터(연도 문서 "문화" 절 단독 줄 빼기)를 모든 곳(목표, 응답 필드 설명, 품질 필터 절, T1·T2·T4·T5, 데모 표, 리스크, 크기)에서 지웠다. M3a1c가 돌려준 사건은 읽기 어색해도 글자 그대로 나간다 | **PM 결정 (2026-10-12)**, M3-P16 (b)를 뒤집음. 예: 1988년 문서 문화 절 하위 줄 "KBS 1TV의 채널에서 광고 24개를 때려갔다."는 판 42640701의 편집자 원문이다. 해석·다듬기는 최종 화면 표시 때에만 하고, 뒤의 웹·화면 기능에서 따로 계획한다(이 기능 밖) |
| 큐레이션 `hide` 는 남기되 용도를 좁힘 | 권리자 요청이나 명백히 틀린 줄을 **바로 내리는 수단**으로만 쓴다. 지금 데모 항목에는 `hide` 가 없고, T1은 어색한 문장을 `hide` 후보로 올리지 않는다 |
| 데모 1988-09-17 사건 **1개 → 3개** | 개막 1개 + 1988년 문서 문화 절의 KBS 1TV·KBS 2TV 줄. M3a1c T3·T5 결과 표 그대로 |
| M3a1c **실제 계약**으로 이름·동작을 맞춤 | `KoWikiEventSource.find(LocalDate)` → `KoWikiDayResult(date, events, docs, skippedLines, mergedCount, linkResolutionFailed)`, `allUnavailable()`·`degraded()`. 문서 상태는 `KoWikiPage.State`(`FRESH`/`CACHED`/`STALE`/`ABSENT`/`UNAVAILABLE`, 계획 초안의 `DocState` 가 아님). `KoWikiDocStatus(title, revision, retrievedAt, state)`. 날짜당 상한 `wikimedia.kowiki-day-deadline: 5s`, Hikari `connection-timeout: 1000`. pageprops는 M3a1c에서 끝났으므로 이 기능은 pageprops 요청을 하나도 더하지 않는다 |
| 로그에 사건 ID 대신 **ID 해시 8자리** | 전체 ID(`kowiki-yyyy-MM-dd-…`)에 날짜가 들어 있다. M3a1c D6 편차와 같은 규칙 |
| M3a1c 코드 리뷰 후속 | 권장 1(`CachingKoWikiClient` 의 `find` 가 `null` 로 DB 오류를 알림) → **T2b** (backend-dev). 권장 3(`KoWikiEventSourceDatabaseExhaustedTest` 의 `multipliedBy(4)`)·권장 4(D4 단위 테스트가 `rule=D4` 경로를 직접 단언) → **T4** (test-engineer). 권장 2·5 → 로드맵 M6b 측정 목록 |
| test-engineer 관찰 항목 | D3 일반 링크 충돌 (iii)이 나라 링크와 도시 링크를 다른 장소로 본다 → **T5 (4) 확인 항목**과 리스크 9. 이 기능에서 고치지 않는다 |
| 크기 다시 추정 | 품질 필터 −10, T2b +15 → 예상 약 390, 1.6배 보정 **약 625**. 중간 확인점을 "T2 뒤 230"에서 **"T2·T2b 뒤 누적 S1 360(보정값)"** 으로 옮김. 보정값이 400을 넘어 분할 여부를 물었고, **PM이 (b) 나누지 않고 한 PR로 결정**(2026-10-12) |

## 2026-10-11 바뀐 점 (기록)

| 바뀐 것 | 이유 |
|---|---|
| 선행이 M3a1 → **M3a1c** | PM 결정으로 M3a1이 나뉘었다(M3a1b 호출 보강·캐시, M3a1c 합치기) |
| 대표 링크 QID 조회(pageprops)를 **M3a1c로 옮김**. 이 기능은 `KoWikiEvent.wikidataQid` 를 그대로 쓴다 | M3a1 검증(8feaf87)에서 같은 사건이 링크 표기만 달라 두 번 나오는 문제가 나와, M3a1c가 중복 제거에 넘겨주기 해석·QID를 쓴다. 같은 요청을 두 번 하지 않는다 |
| `partial` 에 링크 해석 실패·시간 상한이 들어간다 | M3a1c `degraded()` 정의 |
| ~~품질 필터(M3-P16 (b))를 T2·T3에 더함~~ | **2026-10-12 PM 결정으로 취소** (위 표) |
| 국기 템플릿 줄 처리는 넣지 않는다 | M3a1 검증 결과 녹화 12개 절에서 1줄뿐 → 후속 작업 후보 `chore/kowiki-safe-templates` |

## 목표

첫 도메인 API `GET /api/story/world?date=YYYY-MM-DD` 를 만든다. 끝나면 다음이 된다.

1. 데모 날짜를 넣으면 같은 연·월·일의 사건을 한국어 원문 문장, 출처 문서 링크, 판 번호, CC BY-SA 4.0 표기와 함께 돌려준다. 같은 사건은 한 번만 나온다(M3a1c). **M3a1c가 돌려준 사건은 모두 나간다** — 문장을 고치지도, 읽기 어색하다는 이유로 거르지도 않는다(M3-P16, 2026-10-12). 빠지는 것은 큐레이션 `hide` 로 바로 내린 사건뿐이다.
2. 사건마다 `tone`(`normal`/`tragic`)과 판정 근거 `tone_basis` 가 있다(#22 (c)).
3. 사건이 없는 날은 `notice.code = NO_EVENTS_FOUND` 로 알린다(#21 (a)).
4. 잘못된 날짜는 400, 출처를 쓸 수 없으면 503을 **프로젝트 공통 에러 형식**(ProblemDetail + `code`)으로 돌려준다. M2·M4·M5의 API가 이 규약을 따른다.
5. 미디어 자리(`media`, `day_media`)는 응답 형태에 있지만 이 기능에서는 늘 빈 배열이다(M3a3이 채운다).

## 범위 밖

- 미디어(Commons), 공용 미디어 형식 → M3a3 (이 기능은 빈 배열만)
- 지원 시작 연도(#3) 검증 → M2b. 이 기능은 형식·존재하는 날짜·미래 날짜만 본다
- 사건 중복 제거, 링크 정규화, 대표 링크 QID 조회(pageprops), 날짜당 시간 상한 → M3a1c (완료). 이 기능은 위키백과에 따로 요청하지 않는다
- 문장을 고쳐 쓰거나 요약하는 것(#23 (a)), **읽기 어색하거나 사소해 보인다는 이유로 줄을 거르는 것**(M3-P16, 2026-10-12)
- 최종 화면 표시 때의 해석·다듬기 → 뒤의 웹·화면 기능에서 따로 계획(리스크 7)
- D3 일반 링크 충돌에서 나라·도시 링크를 같은 장소로 보는 보정 → 실제 사례를 본 뒤 후속(리스크 9)
- M3a1c 리뷰 권장 2(`cancel(true)` 와 JDBC 쓰기)·5(긴 주소의 pageprops 414) → M6b
- Wikidata 날짜 질의로 세계 사건을 더 찾는 것, Wikidata 날짜 교차 확인 → #20 후속(로드맵 "끝나면 PM이 정할 것")
- On this day (#20 (b)), 국내 사건 보강(#24, M3c)
- 응답 캐시 헤더(`Cache-Control`), API 버전 접두사
- 위키텍스트 템플릿 처리 → 후속 작업 후보
- 프론트엔드 (M3b2)

## 사용할 외부 출처

| 출처 | 확신도 | 이 기능에서 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| 한국어 위키백과 (M3a1·M3a1b·M3a1c 모듈) | **확인됨** | `KoWikiEventSource.find` 가 돌려준 사건 문장, 출처 문서, 판 번호, 정규화된 링크, 대표 링크 QID. pageprops 조회(`action=query&prop=pageprops&ppprop=wikibase_item&redirects=1`)는 M3a1c 안에서 끝나고, 이 기능은 위키백과 요청을 더하지 않는다 | [day-events.md 3절](../research/day-events.md#3-출처-카드). 응답에 문서 링크·라이선스 고지에 필요한 값을 모두 담는다 |
| Wikidata Query Service (SPARQL) | **확인됨** | 사건 QID의 분류(P31/P279*), 사망자 수(P1120), 국가(P17) — `tone` 과 `scope` 신호 | CC0, 출처 표시 의무 없음. User-Agent 필수, 질의 60초 제한, 429 처리. 엔드포인트는 설정값(2026~2027 새 백엔드 이전 대비). 사건 날짜(P585)를 사진 촬영일로 쓰지 않는다 — [day-events.md 3절](../research/day-events.md#3-출처-카드), [8절 2항](../research/day-events.md#8-위험) |

키 없음. M3a1의 Wikimedia 공통 설정(User-Agent, 타임아웃, 재시도, 429)과 M3a1b의 동시 요청 제한(3개)을 그대로 쓴다. 이 기능은 DB를 새로 쓰지 않는다(큐레이션은 시작할 때 파일에서 읽는다).

## 전제 (PM 결정과 가정)

| 결정 | 이 계획서의 전제 | PM이 다르게 정하면 바뀌는 곳 |
|---|---|---|
| #20 사건 1순위 조합 | (a) | (b): On this day 선택 어댑터를 별도 작은 기능으로 추가(M3a3 뒤). (c): 계획 다시 씀 |
| #21 빈 날짜 | (a) `notice` 로 안내, 같은 날 사진(M3a3의 `day_media`)만 | (b): 서비스가 같은 달 사건으로 넓히고 사건에 `date_relation` 을 붙인다(T2 +약 40줄, 제품 정의서 5절 4번 수정 필요). (c): API는 같고 M3b2가 섹션을 숨긴다 |
| #22 `tone` 판정 | (c) 큐레이션 → 키워드 → Wikidata 구조화 → 판정 불가면 `tragic` | (a): 큐레이션·키워드 제거(T1 없어짐, T3 −약 60줄). (b): 큐레이션만, 나머지는 모두 `tragic`/`default`(T3의 Wikidata 제거) |
| #23 위키백과 문장 | (a) 원문 그대로, `summary_modified` 는 늘 `false` | (b): 필드만 쓰이게 되고 이 기능 코드는 그대로 |
| M3-P1 분할 | 로드맵 M3 절 (a) | |
| **M3-P16 낮은 품질 원문** | **변경 (PM 결정 2026-10-12)**: 거르지 않는다. 큐레이션 `hide` 는 권리자 요청·명백히 틀린 줄을 바로 내리는 수단으로만. (2026-10-11 (b) 구조 필터는 취소) | 다시 (b)로: T2에 구조 필터 약 10줄, T2·T4·T5 기준과 데모 표(1988-09-17 사건 1개)가 되돌아간다 |
| **M3a2 분할** | **(b) 결정 (2026-10-12)**: 나누지 않고 한 PR. 중간 확인점 T2·T2b 뒤 누적 S1 360 | 확인점에서 넘으면 오케스트레이터가 다시 묻는다 |

## API 계약 (프로젝트 공통 규약 + 그날의 세계)

### 공통 규약 (이후 모든 `/api` 도메인 엔드포인트)

| 항목 | 규약 |
|---|---|
| 경로 | 결과 화면 섹션마다 `GET /api/story/<section>`. 섹션 이름은 M1b `SectionId` 와 같다(`world`, 이후 `neighborhood`, `city`). 장소 해석은 `GET /api/places/resolve`(M2a) |
| 요청 | 조회는 GET + 쿼리 파라미터 |
| JSON 이름 | **snake_case** (제품 정의서 6.2절 필드 이름 그대로). 구현 방법(전역 설정 또는 DTO 주석)은 backend-dev가 Spring Boot 4.1·Jackson 3 문서로 확인해 고른다 |
| 날짜·시각 | 날짜 `YYYY-MM-DD`, 시각 ISO-8601 UTC 초 단위(`2026-10-12T03:00:00Z`, 소수 초는 자른다) |
| 에러 | `application/problem+json` (RFC 9457 ProblemDetail) + 확장 필드 `code`(대문자 스네이크). `instance` 는 경로만(쿼리 문자열 없음). `detail` 은 영어 설명이고 **입력값(날짜·장소)을 넣지 않는다**. 화면 문구는 프론트엔드가 `code` 로 정한다 |
| 일부 실패 | 섹션 안의 보조 출처가 실패해도 200으로 답하고 `partial: true`. 섹션의 필수 출처를 전혀 쓸 수 없을 때만 503 |
| 로그 | 요청 파라미터 값을 로그에 남기지 않는다. 400은 로그 없음, 503은 WARN(출처·상태만). 사건을 가리킬 때는 ID 해시 8자리만(전체 ID에는 날짜가 있다) |

### `GET /api/story/world`

요청: `GET /api/story/world?date=1988-09-17`

| 상태 | `code` | 조건 |
|---|---|---|
| 400 | `DATE_REQUIRED` | `date` 가 없거나 빈 값 |
| 400 | `DATE_FORMAT` | `^\d{4}-\d{2}-\d{2}$` 가 아님 (예 `1988-9-17`) |
| 400 | `DATE_NOT_EXIST` | 형식은 맞지만 없는 날짜 (예 `2001-02-29`, `1988-02-30`) |
| 400 | `DATE_IN_FUTURE` | `Asia/Seoul` 기준 오늘보다 뒤 (오늘은 허용) |
| 503 | `UPSTREAM_UNAVAILABLE` | `KoWikiDayResult.allUnavailable()` — 날짜 문서와 연도 문서가 **둘 다** `KoWikiPage.State.UNAVAILABLE`(캐시도 없음). 하나라도 `STALE`(오래된 캐시)·`FRESH`·`CACHED` 면 200, 둘 다 `ABSENT`(문서 없음)면 실패가 아니라 200 + `NO_EVENTS_FOUND` |
| 200 | - | 그 밖 (사건 0개 포함) |

`KoWikiEventSource.find` 는 예외를 던지지 않는다(실패는 문서 상태로 온다). 서비스는 결과를 위 표대로 옮기기만 한다.

400 예:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"about:blank","title":"Bad Request","status":400,
 "detail":"date must be an existing calendar date in YYYY-MM-DD format",
 "instance":"/api/story/world","code":"DATE_NOT_EXIST"}
```

200 예 (1988-09-17, T3 뒤 — 사건 3개 중 앞의 2개. `id`·판 번호·QID는 M3a1c 녹화 fixture 값이고, KBS 줄의 ID 해시와 `related_links`·`retrieved_at` 은 fixture를 따른다):

```json
{
  "date": "1988-09-17",
  "events": [
    {
      "id": "kowiki-1988-09-17-875c69fa",
      "date": "1988-09-17",
      "scope": "domestic",
      "title": "1988년 하계 올림픽",
      "summary": "제24회 서울 하계 올림픽 개막.",
      "summary_lang": "ko",
      "summary_modified": false,
      "text_license": "CC BY-SA 4.0",
      "text_license_url": "https://creativecommons.org/licenses/by-sa/4.0/deed.ko",
      "source_name": "한국어 위키백과",
      "source_title": "9월 17일",
      "source_url": "https://ko.wikipedia.org/wiki/9%EC%9B%94_17%EC%9D%BC",
      "source_section": "사건",
      "source_revision": 42604413,
      "wikidata_qid": "Q8470",
      "tone": "normal",
      "tone_basis": "curated",
      "related_links": [
        {"title": "1988년 하계 올림픽", "url": "https://ko.wikipedia.org/wiki/1988%EB%85%84_%ED%95%98%EA%B3%84_%EC%98%AC%EB%A6%BC%ED%94%BD"}
      ],
      "media": [],
      "retrieved_at": "2026-10-12T03:00:00Z"
    },
    {
      "id": "kowiki-1988-09-17-(fixture 값)",
      "date": "1988-09-17",
      "scope": "domestic",
      "title": "KBS 1TV",
      "summary": "KBS 1TV의 채널에서 광고 24개를 때려갔다.",
      "summary_lang": "ko",
      "summary_modified": false,
      "text_license": "CC BY-SA 4.0",
      "text_license_url": "https://creativecommons.org/licenses/by-sa/4.0/deed.ko",
      "source_name": "한국어 위키백과",
      "source_title": "1988년",
      "source_url": "https://ko.wikipedia.org/wiki/1988%EB%85%84",
      "source_section": "문화",
      "source_revision": 42640701,
      "wikidata_qid": "Q777278",
      "tone": "normal",
      "tone_basis": "curated",
      "related_links": [
        {"title": "KBS 1TV", "url": "https://ko.wikipedia.org/wiki/KBS_1TV"}
      ],
      "media": [],
      "retrieved_at": "2026-10-12T03:00:00Z"
    }
  ],
  "day_media": [],
  "notice": null,
  "partial": false
}
```

(셋째 사건은 KBS 2TV 줄 — "KBS 2TV의 채널에서 광고 37개를 때려갔다.", `1988년`/`문화`, `Q624509`. 문장은 원문 그대로다.)

200 예 (1995-03-15):

```json
{"date":"1995-03-15","events":[],"day_media":[],"notice":{"code":"NO_EVENTS_FOUND"},"partial":false}
```

### 응답 필드

| 필드 | 형 | 설명 |
|---|---|---|
| `date` | string | 요청 날짜 |
| `events[]` | array | 아래 사건. 순서: `tone=normal` 먼저(원래 순서 유지), 그다음 `tragic`. 같은 `tone` 안에서는 `KoWikiDayResult.events` 순서(날짜 문서 사건 → 문화 → 연도 문서 사건 → 문화, 중복 제거 후). `hide` 로 내린 사건만 빠진다 |
| `day_media[]` | array | 같은 날 사진 후보(공용 미디어 형식, M3a3). 이 기능에서는 늘 `[]` |
| `notice` | object \| null | `events` 가 비면(`hide` 로 모두 빠진 경우 포함) `{"code":"NO_EVENTS_FOUND"}`, 아니면 `null` |
| `partial` | boolean | `KoWikiDayResult.degraded()`(두 문서 중 하나라도 `UNAVAILABLE`·`STALE`, 또는 `linkResolutionFailed` — pageprops 실패·날짜 상한 도달) **또는** Wikidata 실패(M3a3 이후 Commons 실패도)면 `true`. `hide` 로 내린 것은 `partial` 이 아니다 |

사건(`events[]`) 필드 — 리서치의 "사건 레코드 필드 제안"([day-events.md 4절](../research/day-events.md#4-미디어-레코드-필드-매핑))을 확정한 것. 왼쪽은 응답, 오른쪽은 M3a1c `KoWikiEvent` 필드:

| 필드 | 형 | 채우는 법 |
|---|---|---|
| `id` | string | `KoWikiEvent.id` (`kowiki-{yyyy-MM-dd}-{8자리 16진수}`) |
| `date` | string | `KoWikiEvent.date` (요청 날짜와 같다 — M3a1c 불변 조건) |
| `scope` | `domestic` \| `world` \| `unknown` | 큐레이션 값 → QID의 P17에 대한민국(Q884)이 있으면 `domestic`, P17이 있고 Q884가 없으면 `world` → 그 밖 `unknown` |
| `title` | string \| null | `linkTitles` 첫 번째(대표 링크, M3a1c 정규 제목). 빈 목록이면 `null` |
| `summary` | string | `KoWikiEvent.text` 글자 그대로 (어색해도 그대로, M3-P16 2026-10-12) |
| `summary_lang` | `ko` | 고정 |
| `summary_modified` | boolean | `false` (#23 (a)) |
| `text_license`, `text_license_url` | string | `CC BY-SA 4.0`, CC BY-SA 4.0 한국어 증서 주소 |
| `source_name` | string | `한국어 위키백과` |
| `source_title` | string | `KoWikiEvent.sourceTitle` (`9월 17일` 또는 `1988년`) |
| `source_url` | string | `https://ko.wikipedia.org/wiki/` + `sourceTitle`(공백은 `_`, UTF-8 퍼센트 인코딩). 판 고정 주소가 아니라 문서 주소(사용자에게 보여 줄 링크) |
| `source_section` | `사건` \| `문화` | `KoWikiEvent.sourceSection` |
| `source_revision` | number | `KoWikiEvent.revision` |
| `wikidata_qid` | string \| null | `KoWikiEvent.wikidataQid`. 대표 링크가 없거나 해석 실패(`linkResolutionFailed`)면 `null` |
| `tone` | `normal` \| `tragic` | 아래 판정 |
| `tone_basis` | `curated` \| `rule:keyword` \| `rule:class` \| `rule:deaths` \| `rule:no-signal` \| `default` | 판정 근거. `rule:no-signal` 은 구조화 규칙을 실제로 평가했고 신호가 없어 `normal` 이 된 경우다 |
| `related_links[]` | `{title, url}` | `linkTitles`(정규 제목) 각각의 문서 주소 (`source_url` 과 같은 인코딩) |
| `media[]` | array | 공용 미디어 형식(M3a3). 이 기능에서는 `[]` |
| `retrieved_at` | string | `KoWikiEvent.retrievedAt`(캐시 행의 `fetched_at` 또는 받은 시각)을 초 단위 ISO-8601 UTC로 |

### 원문 그대로 내보내기와 큐레이션 숨김 (M3-P16 — 2026-10-12 변경)

- `KoWikiDayResult.events` 를 **모두** 응답에 옮긴다. 출처 문서·절(연도 문서 "문화" 절 포함)이나 문장의 읽기 좋고 나쁨으로 거르지 않는다. 예: 1988-09-17의 KBS 1TV·KBS 2TV 줄은 판 42640701 원문 그대로 나간다.
- 해석·다듬기는 최종 화면 표시 때에만 하고, 뒤의 웹·화면 기능에서 따로 계획한다. 이 기능의 응답은 늘 원문이고 `summary_modified` 는 `false` 다.
- **큐레이션 숨김**: `data/curation/event-tone.json` 항목에 `"hide": true` 를 두면 그 사건을 뺀다(맞추는 법은 `tone` 항목과 같음). `hide` 항목은 `tone` 이 없어도 된다.
  - 쓰는 때: 권리자 요청을 받았을 때, 또는 사실과 명백히 다른 줄을 바로 내려야 할 때(제품 정의서 6.3절, `CLAUDE.md` 7절 1항 차단 목록과 같은 취지).
  - 쓰지 않는 때: 문장이 어색하거나, 사소해 보이거나, 사건 기록 같지 않을 때.
  - `reason` 은 사실만 적는다("권리자 요청 2026-10-20" 등, 문장 품질 평가는 적지 않는다).
  - 지금 데모 항목에는 `hide` 가 없다.
- `hide` 로 뺀 사건은 DEBUG 로그에 수와 **사건 ID 해시 8자리**만 남긴다(문장·날짜·전체 ID 없음).

### `tone` 판정 (#22 (c))

순서대로 처음 맞는 것을 쓴다.

| 순서 | 조건 | `tone` | `tone_basis` |
|---|---|---|---|
| 1 | 큐레이션 항목과 맞음 (`data/curation/event-tone.json`) | 항목 값 | `curated` |
| 2 | `summary` 에 비극 키워드가 있음 | `tragic` | `rule:keyword` |
| 3 | QID가 있고 Wikidata 평가 성공, 분류가 비극 분류 목록의 하위 | `tragic` | `rule:class` |
| 4 | QID가 있고 Wikidata 평가 성공, 사망자 수 > 0 | `tragic` | `rule:deaths` |
| 5 | QID가 있고 Wikidata 평가 성공, 신호 없음 | `normal` | `rule:no-signal` |
| 6 | 그 밖 (QID 없음 — 대표 링크 없음, M3a1c `linkResolutionFailed` 포함 — 또는 Wikidata 실패) | `tragic` | `default` |

- 비극 분류 목록(설정값, 리서치 시험 기준): 재난 Q3839081, 사고 Q171558, 테러 공격 Q2223653, 무력 충돌 Q350604.
- 비극 키워드(서버 리소스 파일, 한국어만): 사망, 숨지, 희생, 참사, 사고, 추락, 침몰, 충돌, 탈선, 붕괴, 폭발, 화재, 지진, 해일, 쓰나미, 태풍, 홍수, 테러, 총격, 학살, 암살, 피살, 살해, 전쟁, 공습, 폭격, 침공, 내전, 분쟁, 쿠데타, 처형. 오탐("전쟁 종결" 등)은 보수적 기본값과 같은 방향이라 허용한다.
- 판정은 사건을 해석하지 않는다(제품 정의서 6.3절). 화면 분리를 위한 신호일 뿐이다.

### 큐레이션 파일 형식 (`data/curation/event-tone.json`, content-curator)

```json
{
  "version": 1,
  "entries": [
    {
      "date": "1988-09-17",
      "primary_link": "1988년 하계 올림픽",
      "summary_contains": null,
      "tone": "normal",
      "scope": "domestic",
      "hide": false,
      "reason": "올림픽 개막. 비극 요소 없음",
      "evidence_url": "https://ko.wikipedia.org/w/index.php?title=9%EC%9B%94_17%EC%9D%BC&oldid=42604413",
      "reviewed_at": "2026-10-12"
    },
    {
      "date": "1988-09-17",
      "primary_link": "KBS 1TV",
      "summary_contains": null,
      "tone": "normal",
      "scope": "domestic",
      "hide": false,
      "reason": "방송 채널의 광고 관련 줄. 비극 요소 없음",
      "evidence_url": "https://ko.wikipedia.org/w/index.php?title=1988%EB%85%84&oldid=42640701",
      "reviewed_at": "2026-10-12"
    }
  ]
}
```

- 맞추는 법: `date` 가 같고, `primary_link` 가 사건의 대표 링크(`linkTitles` 첫 번째, M3a1c 정규 제목)와 같거나(밑줄·공백 차이 무시) `summary_contains` 가 `summary` 에 들어 있으면 맞음. 둘 중 하나는 반드시 있다.
- `hide`(선택, 기본 `false`): `true` 면 사건을 뺀다(위 "큐레이션 숨김"의 용도로만). 이때 `tone` 은 없어도 된다. `hide` 가 아니면 `tone` 필수.
- 스키마: `data/schema/event-curation.schema.json` (JSON Schema 2020-12, content-curator).
- 서버는 `app.data-dir`(기본 `../data`, `server/` 에서 실행 기준) 아래 파일을 시작할 때 읽는다. 파일이 없으면 WARN 후 큐레이션 없이 동작, 형식이 틀리면 시작 실패.

### Wikidata 조회

1. 사건들의 `wikidataQid`(M3a1c가 채움, `null` 은 뺀다)를 중복 없이 모은다. 이 기능은 위키백과에 QID를 다시 묻지 않는다.
2. QID들을 Wikidata에 한 번 묻는다(`wikimedia.wikidata-sparql-url`, 기본 `https://query.wikidata.org/sparql`, `format=json`). QID가 하나도 없으면 묻지 않는다. 질의 모양(1988-09-17 데모의 QID 3개):

```sparql
SELECT ?item ?tragicClass ?deaths ?country WHERE {
  VALUES ?item { wd:Q8470 wd:Q777278 wd:Q624509 }
  OPTIONAL { ?item wdt:P31/wdt:P279* ?tragicClass .
             VALUES ?tragicClass { wd:Q3839081 wd:Q171558 wd:Q2223653 wd:Q350604 } }
  OPTIONAL { ?item wdt:P1120 ?deaths }
  OPTIONAL { ?item wdt:P17 ?country }
}
```

3. Wikidata 읽기 타임아웃은 `wikimedia.wikidata-read-timeout: 5s`. 실패하면 큐레이션·키워드에 걸리지 않은 사건이 순서 6(`default`)로 가고 `partial: true`.
4. 호출 순서: `KoWikiEventSource.find`(날짜당 상한 5초, DB 대기 포함) → Wikidata(최대 5초). 세계 섹션 최악 약 10초(리스크 4).

## 테스트 방침

M3a1 계획서 "테스트 방침"을 따른다. 이 기능에서 추가하는 것:

- 녹화 fixture: 데모 사건 QID들의 SPARQL 응답(`fixtures/wikimedia/wikidata/` — 1988-09-17 `Q8470`·`Q777278`·`Q624509`, 2002-06-22 `Q47735`, `README.md` 에 원 URL·녹화 일자·라이선스 CC0). 위키백과 문서·pageprops 응답은 M3a1·M3a1c가 녹화한 것(`fixtures/wikimedia/kowiki/`, `pageprops-*.json`)을 쓴다. WireMock은 SPARQL 요청을 경로 + `query` 에 든 QID로 맞춘다.
- 합성 fixture: `tone`·`scope` 판정 분기마다 가짜 QID(`Q1`~`Q9`)와 가짜 SPARQL 응답, `hide` 분기, "거르지 않음" 분기(연도 문서 문화 단독 줄, 연도 문서 사건 단독 줄, 날짜 문서 문화 줄이 모두 나감). 실제 비극 사건 이름을 쓰지 않는다.
- 날짜 검증 테스트는 고정 시계(`2026-10-10`, `Asia/Seoul`)를 쓴다.
- 컨트롤러 형식 테스트(400, 상태 옮기기)는 `KoWikiEventSource` 를 목으로 바꿔 `KoWikiDayResult` 를 직접 만들어도 되고, 데모 날짜 테스트는 M3a1·M3a1c 녹화 fixture + WireMock으로 끝까지 돌린다.

## 데모 입력 3개로 확인할 것

M3a1c T3·T5 결과 표(녹화 fixture 기준)를 그대로 쓴다. `tone`·`scope` 는 T3 뒤 값이다(T2 단계에서는 모두 `tragic`/`default`, `scope` = `unknown`).

| 데모 입력 | 확인 |
|---|---|
| 1988-09-17 + "서울" | 200. `events` **정확히 3개**, 이 순서: ① `id` = `kowiki-1988-09-17-875c69fa`, `source_title` = `9월 17일`, `source_section` = `사건`, `title` = `1988년 하계 올림픽`, `wikidata_qid` = `Q8470`, `source_revision` = 42604413, `summary` 에 "서울 하계 올림픽"(이 문구가 든 사건은 이것 하나 — M3a1c 중복 제거로 4줄 중 3줄이 빠짐). ② `title` = `KBS 1TV`, `source_title` = `1988년`, `source_section` = `문화`, `wikidata_qid` = `Q777278`, `source_revision` = 42640701, `summary` = "KBS 1TV의 채널에서 광고 24개를 때려갔다."(글자 그대로). ③ `title` = `KBS 2TV`, `1988년`/`문화`, `Q624509`, 42640701, `summary` = "KBS 2TV의 채널에서 광고 37개를 때려갔다.". 셋 다 `tone` = `normal`, `tone_basis` = `curated`, `scope` = `domestic`, `text_license` = `CC BY-SA 4.0`, `media` = `[]`. `notice` = `null`, `partial` = `false` |
| 2002-06-22 + "광주 서구 풍암동" | 200. `events` **정확히 1개**: `id` = `kowiki-2002-06-22-04eaf6a6`, `source_title` = `6월 22일`, `source_section` = `문화`, `title` = `2002년 FIFA 월드컵`, `wikidata_qid` = `Q47735`, `source_revision` = 42614972, `summary` 에 "스페인", `tone` = `normal`/`curated` |
| 1995-03-15 + "서울대학교병원" | 200. `events` = `[]`, `notice.code` = `NO_EVENTS_FOUND`, `partial` = `false`. pageprops 요청 0, SPARQL 요청 0 |

데모 날짜에 나오는 모든 사건(4개)의 `tone`·`tone_basis`·`scope` 를 T4가 표로 보고하고 PM이 T5에서 검수한다(로드맵 "끝나면 PM이 정할 것").

## 크기 (400줄 규칙)

예상(제품 코드 S1, 데이터 파일 `data/**` 는 따로 보고):

| 작업 | 내용 | 예상 | 보정 (M3a1 실측 1.6배) |
|---|---|---|---|
| T2 | 컨트롤러·날짜 검증·에러 처리 약 100, DTO 약 50, 서비스(결과 옮기기·`notice`·`partial`·503) 약 60. 품질 필터 약 10은 **지움** | 약 210 | 약 335 |
| T2b | `CachingKoWikiClient` 읽기 결과 형 (추가 줄 기준, 지우는 줄 약 10) | 약 15 | 약 25 |
| T3 | `tone`·`scope`(큐레이션 로더·`hide`·키워드·조합) 약 95, Wikidata 클라이언트 약 70 | 약 165 | 약 265 |
| 합계 | (pageprops 약 25줄은 M3a1c로 이미 옮김) | **약 390** | **약 625** |

- **한 PR (PM 결정 (b), 2026-10-12)**: 보정 합계가 400을 넘지만 나누지 않는다.
- **중간 확인점**: 처음 계획은 "T2 뒤 누적 230줄"이었다. 230은 보정 전 값이라 옮겼다. **T2·T2b 뒤 누적 S1이 360(보정 예상값)을 넘으면** 오케스트레이터가 멈추고 PM에게 T3(또는 그 Wikidata 부분)을 별도 PR로 뺄지 묻는다.
- 참고: 최근 실측 비율은 M3a1b 약 2.4배(150 → 366), M3a1c 약 3.5배 이상(210 → 743)이라 1.6배도 낙관적일 수 있다.

확인 명령은 아래 S1·S2·S3.

## 작업 목록

순서: T1과 (T2 → T2b)는 병렬 가능(서로 다른 폴더) → **중간 확인점** → T3 → T4 → T5.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 사건 큐레이션 스키마와 데모 날짜 `tone` 큐레이션 | content-curator | `data/schema/event-curation.schema.json`, `data/curation/event-tone.json` | - (M3a1c 머지 완료) | (1) 스키마가 위 "큐레이션 파일 형식"의 필드·규칙(`primary_link`·`summary_contains` 중 하나 필수, `hide` 가 `true` 가 아니면 `tone` 필수이고 `normal`/`tragic`, `scope` 는 세 값 중 하나, `date` 형식, `evidence_url` 필수)을 담은 JSON Schema 2020-12. (2) **M3a1c T5 결과 표**(중복 제거 후)의 데모 사건 **4개 전부**에 항목이 있다: 1988-09-17 `1988년 하계 올림픽`·`KBS 1TV`·`KBS 2TV`, 2002-06-22 `2002년 FIFA 월드컵`(`primary_link` 는 M3a1c 정규 제목). 항목마다 `evidence_url` 이 fixture `revid` 를 쓴 판 고정 주소(9월 17일 `oldid=42604413`, 1988년 `oldid=42640701`, 6월 22일 `oldid=42614972`)다. 1995-03-15 항목은 없다(사건 없음). (3) `reason` 은 사실만 적고 문장 품질 평가("어색함", "사소함", "사건 아님" 등)를 쓰지 않는다. (4) **`hide: true` 항목 0개.** 어색하거나 사소해 보이는 줄을 `hide` 후보로 보고하지 않는다(`hide` 는 권리자 요청·명백히 틀린 줄용, PM 결정 2026-10-12). (5) C1이 두 파일 모두 `ok` 를 출력한다. (6) 보고서에 항목 표(날짜, 대표 링크, `tone`, `scope`, `hide`, 근거) |
| T2 | `GET /api/story/world` 골격: 날짜 검증, 공통 에러 형식, 응답 DTO, `KoWikiDayResult` 옮기기, `notice`, 503, `partial` | backend-dev | 컨트롤러, 날짜 파라미터 검증, 전역 예외 처리(ProblemDetail + `code`), 응답 DTO(snake_case), 서비스(`KoWikiEventSource.find` 사용, 거르지 않음, `tone` 은 임시로 모두 `tragic`/`default`·`scope` 는 `unknown` 을 내는 판정기 인터페이스), 테스트 | - (M3a1c 머지 완료) | (1) `cd server && ./gradlew check` 성공. (2) `DATE_REQUIRED`(파라미터 없음, 빈 값), `DATE_FORMAT`(`1988-9-17`), `DATE_NOT_EXIST`(`2001-02-29`, `1988-02-30`) → 400, `Content-Type` `application/problem+json`, 본문 `code`·`status`·`instance`=`/api/story/world`, 본문 어디에도 입력 문자열이 없다. (3) 고정 시계 2026-10-10: `2026-10-11` → 400 `DATE_IN_FUTURE`, `2026-10-10` → 200. 경계값 `2000-02-29` → 200(합성 fixture). (4) 1988-09-17(녹화 fixture + WireMock) → 200, 응답 JSON 키가 snake_case(위 표의 키 전부 존재), `media` = `[]`, `day_media` = `[]`, `events` **정확히 3개**이고 `id`·`source_title`·`source_section`·`source_revision`·`title`·`wikidata_qid` 가 위 "데모" 표 1988-09-17 행과 같다. 각 사건 `summary` 가 `KoWikiEvent.text` 와 글자 하나까지 같다(KBS 두 줄 포함). `source_url` 이 위 규칙의 문서 주소, `retrieved_at` 이 초 단위 ISO-8601 UTC. (5) 1995-03-15 → `events` = `[]`, `notice.code` = `NO_EVENTS_FOUND`, `partial` = `false`. (6) 상태 옮기기(`KoWikiDayResult` 를 직접 만든 목 또는 WireMock): 두 문서 `UNAVAILABLE`(`allUnavailable()`) → 503 `UPSTREAM_UNAVAILABLE`(problem+json), WARN 1줄에 날짜 없음. 한 문서 `UNAVAILABLE` + 다른 문서 `FRESH` → 200, `partial` = `true`. 한 문서 `STALE` + 다른 문서 `UNAVAILABLE` → 200(503 아님), `partial` = `true`. 두 문서 `ABSENT` → 200, `NO_EVENTS_FOUND`, `partial` = `false`. `linkResolutionFailed` = true(WireMock pageprops 503) → 200, `partial` = `true`, `wikidata_qid` 모두 `null`. (7) **거르지 않음**(합성): 연도 문서 문화 절 단독 사건, 연도 문서 사건 절 단독 사건, 날짜 문서 문화 절 사건이 모두 응답에 있고, `events` 수가 `KoWikiDayResult.events` 수와 같다. (8) `GET /api/health` 는 여전히 `{"status":"UP"}`, `GET /api` 는 404 |
| T2b | M3a1c 리뷰 권장 1: `CachingKoWikiClient` 의 캐시 읽기 결과를 `null` 대신 작은 결과 형으로 | backend-dev | `CachingKoWikiClient`(예: 행 있음·행 없음·읽기 실패를 나타내는 private record 또는 sealed 형) | - (T2와 같은 담당, T2 다음) | (1) private `find` 가 DB 오류를 `null` 로 알리지 않는다 — C3 출력 없음. (2) 동작은 그대로: 기존 `CachingKoWikiClient*Test`·`KoWikiEventSource*Test`(쓰기 건너뛰기, DB 고장·고갈, 캐시만 읽기 포함)를 **고치지 않고** 통과. (3) `cd server && ./gradlew check` 성공. (4) 보고서에 T2b S1 증가분(30 이하가 목표)과 T2·T2b 누적 S1 — **중간 확인점**(360) 판단에 쓴다 |
| T3 | `tone`·`scope`·큐레이션 숨김: 큐레이션 로더, 키워드 규칙, Wikidata 클라이언트, 판정 조합 | backend-dev | 큐레이션 로더(`app.data-dir`, `hide`), 키워드 리소스, Wikidata SPARQL 클라이언트(`wikimedia.wikidata-sparql-url`, `wikidata-read-timeout`), 판정기, SPARQL 녹화 fixture(+ `README.md`), 테스트 | T1, T2, T2b (중간 확인점 통과) | (1) 기본 설정에서 `../data/curation/event-tone.json` 을 읽는다. 형식이 틀린 파일(테스트용 사본) → 컨텍스트 시작 실패, 파일 없음 → WARN 후 동작. (2) 판정 분기 테스트(합성 QID): 큐레이션이 키워드보다 우선, 키워드 → `rule:keyword`, 비극 분류 → `rule:class`, 사망자 > 0 → `rule:deaths`, QID 있고 신호 없음 → `normal`/`rule:no-signal`, QID 없음 → `tragic`/`default`, Wikidata 503 → `tragic`/`default` + `partial` = `true`. (3) `scope` 분기: 큐레이션 값 우선, P17에 Q884 → `domestic`, 다른 나라만 → `world`, P17 없음·QID 없음 → `unknown`. (4) 큐레이션 `hide: true` 항목과 맞는 사건(합성) → 응답에 없음, `partial` 은 그대로, 모두 숨겨지면 `NO_EVENTS_FOUND`. DEBUG 로그에 ID 해시 8자리만 있고 문장·날짜·`kowiki-` 가 없다. (5) WireMock: API 호출 한 번에 SPARQL 요청 1회(그날의 `null` 아닌 QID가 모두 `VALUES` 에 — 1988-09-17은 `Q8470`·`Q777278`·`Q624509`), `User-Agent` 헤더. 위키백과 요청 수(pageprops 1회 포함)가 `KoWikiEventSource.find` 만 부를 때와 같다(이 기능이 더하지 않음). QID가 하나도 없으면(1995-03-15) SPARQL 요청 0. (6) 데모: 위 "데모" 표의 `tone`·`tone_basis`·`scope` — 1988-09-17 사건 3개 모두 `normal`/`curated`/`domestic`, 2002-06-22 사건 `normal`/`curated`. (7) `fixtures/wikimedia/wikidata/README.md` 에 원 URL·녹화 일자·CC0, 보고서에 녹화 때 걸린 시간(리스크 3). 네트워크를 쓸 수 없으면 "막힘"으로 보고하고 PM이 실행할 `curl` 명령(User-Agent는 `$WIKIMEDIA_USER_AGENT`)을 적는다 |
| T4 | 완료 기준 검증, 스키마 검증 테스트, M3a1c 리뷰 후속 테스트 2건, 전체 테스트 | test-engineer | 빠진 기준의 테스트, `data/curation/event-tone.json` 을 `data/schema/event-curation.schema.json` 으로 검증하는 테스트(JSON Schema 검증 라이브러리를 테스트 의존성으로 추가 — M3a3도 쓴다), `KoWikiEventSourceDatabaseExhaustedTest`·`KoWikiEventDeduplicatorTest` 수정(테스트 파일만) | T3 | (1) T2·T2b·T3 기준을 표로 확인하고 빠진 테스트를 추가. (2) 스키마 검증 테스트가 통과하고, 일부러 틀린 사본(필수 필드 없음, `hide` 아닌데 `tone` 없음)은 실패한다. (3) **리뷰 권장 3**: `KoWikiEventSourceDatabaseExhaustedTest` 의 `multipliedBy(4)` → `multipliedBy(2)`, 설명을 "one read per page (the write is skipped after a failed read), two pages, inside the day deadline" 처럼 실제 동작(`application.yml` Hikari 주석과 같은 뜻)으로. 다른 단언은 그대로. (4) **리뷰 권장 4**: `KoWikiEventDeduplicatorTest.d4LineMatchingTwoEarlierEventsIsNotMerged` 가 D4 경로를 직접 단언한다 — DEBUG 로그 캡처에 `rule=D4` 와 뒤 줄 ID 해시가 있다(D4 분기를 지우면 이 테스트가 실패). 기존 단언(`events`, `mergedCount` 0)은 지우지 않는다. (5) `cd server && ./gradlew check` 통과(개수 보고), 3회 반복 안정. (6) C2·C3 출력 없음. (7) S1·S2·S3 결과 보고. (8) 데모 날짜 사건 4개의 표(`id`, 출처 문서·절, 본문 앞 30자, `tone`, `tone_basis`, `scope`, `wikidata_qid`)와 `hide` 항목 수(0) |
| T5 | 실제 호출 확인과 `tone` 검수 (PR 검수 때) | PM | - | T4 | (1) 루트 `.env` 에 `WIKIMEDIA_USER_AGENT` 값을 넣고 `docker compose config --quiet` 가 종료 코드 0. (2) `docker compose up -d db`, `cd server && ./gradlew bootRun` 후 P1~P4 명령 결과가 "데모" 표와 맞는다(같은 사건이 두 번 나오지 않는다. 1988-09-17 KBS 두 줄은 원문 그대로 나오는 것이 정상). (3) T4의 `tone` 표를 보고 틀린 판정이 있으면 PR 코멘트로 알린다(content-curator가 큐레이션을 고친다). `hide` 는 권리자 요청·명백히 틀린 줄에만 쓴다. (4) **관찰 항목 (D3 나라·도시 링크)**: 같은 사건이 한 줄은 나라 링크(`대한민국`), 다른 줄은 도시 링크(`서울특별시`)만 일반 링크로 달고 있으면(예: `[대회, 대한민국]` / `[대회, 서울특별시]`) D3 (iii)이 다른 장소로 보아 두 번 나온다. 데모 3날짜에서는 M3a1c T5 표에 그런 줄이 없다. 실제 호출에서 보이면 PR 코멘트로 알린다 — 이 기능에서는 고치지 않고 M3a1c 규칙 보정 후속으로 넘긴다 |

완료 기준에 쓰는 명령 (저장소 루트):

```
# C1 — 큐레이션 파일이 JSON으로 읽히는지 (content-curator). 파일마다 ok
for f in data/schema/event-curation.schema.json data/curation/event-tone.json; do
  python3 -c 'import json,sys; json.load(open(sys.argv[1])); print("ok", sys.argv[1])' "$f"; done

# C2 — 컨트롤러·예외 처리에서 요청 값을 로그로 남기지 않는지. 출력을 사람이 확인 (code-reviewer도 본다)
grep -rnE 'log\.(info|warn|error|debug)\(' server/src/main/java | grep -iE 'date|query|param'

# C3 — CachingKoWikiClient가 null로 DB 오류를 알리지 않는지 (T2b). 출력 없음
grep -nE 'return null|== null|ofNullable' \
  server/src/main/java/io/github/ericmschoi/theworldthatday/event/kowiki/CachingKoWikiClient.java

# S1 — 제품 코드·설정 줄 수
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
# S3 — 데이터 파일 줄 수
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P1~P4 — PM 실제 호출 확인 (서버 실행 중)
curl -s 'http://localhost:8080/api/story/world?date=1988-09-17' | jq '.events[] | {id, summary, source_title, source_section, tone, tone_basis, scope, wikidata_qid, source_revision}'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' | jq '.events[] | {id, summary, source_section, tone, wikidata_qid}'
curl -s 'http://localhost:8080/api/story/world?date=1995-03-15' | jq '{events, notice, partial}'
curl -s -i 'http://localhost:8080/api/story/world?date=2001-02-29' | head -n 20
```

담당별: content-curator 1 (T1), backend-dev 3 (T2, T2b, T3), test-engineer 1 (T4), PM 1 (T5).

## 리스크와 확인할 것

1. **세계 사건이 데모 날짜에 거의 없다** — #20 (a) 조합으로는 데모 3개 날짜 모두 국내 사건만 나오거나(1988 — 개막과 KBS 줄, 2002) 비어 있다(1995). "그날의 세계"라는 제목과 어긋날 수 있다. → M3a3 뒤에 실제 결과를 보고 #20 (b)(On this day — 2002 이란 지진 1건 추가) 또는 Wikidata 날짜 질의 세계 사건을 PM이 다시 정한다(로드맵 "끝나면 PM이 정할 것").
2. **`default` 판정이 많을 수 있다** — 대표 링크가 없는 줄, 링크 해석 실패(`linkResolutionFailed`), Wikidata 실패 때 `tragic` 으로 간다. 데모 날짜는 큐레이션으로 덮는다. 데모 밖 날짜에서 차분한 표시가 많아지는 것은 의도된 보수적 동작이다.
3. **P279* 질의가 느릴 수 있다** — 상위 분류를 따라가는 질의는 60초 제한 안에서도 느릴 수 있다. 읽기 타임아웃 5초를 넘으면 `default` 로 간다. T3 보고서에 녹화 때 걸린 시간을 적는다.
4. **세계 섹션 전체 시간** — 위키백과(`wikimedia.kowiki-day-deadline` 5초. DB가 닿지 않을 때의 대기도 그 안 — Hikari `connection-timeout` 1000ms × 날짜당 최대 2번) 뒤에 Wikidata(최대 5초)가 오면 최악 약 10초다. 이 기능은 DB 호출을 더하지 않는다. M3a3에서 Commons가 더해진다. 동시 요청 3개 제한(M3a1b)도 줄을 세운다. 제품 정의서 8절(느린 섹션은 나중에 채움, 전체 10초)에 맞는지 M6b에서 측정하고, 넘으면 섹션 전체 상한을 후속으로 제안한다.
5. **Spring Boot 4.1·Jackson 3** — ProblemDetail 설정 키, snake_case 설정 방법, 예외 처리 클래스 이름이 Boot 3과 다를 수 있다. 보고서에 고른 방법을 적는다.
6. **`.env` 의 User-Agent 값** — 공백·괄호가 든 값을 Compose가 문제없이 읽는지 T5 (1)에서 본다. 문제가 있으면 M1a 리스크 5처럼 `.env.example` 주석을 고치는 작은 후속 작업으로 처리한다.
7. **어색한 원문이 그대로 나간다 (M3-P16 2026-10-12, 의도된 동작)** — 예: 1988-09-17의 KBS 두 줄. 화면 표시 때의 해석·다듬기를 뒤의 웹·화면 기능에서 계획할 때, #23 (a)(원문 그대로, 번역·요약하지 않음)와 CC BY-SA 4.0의 변경 표시(`summary_modified`, 리서치 #23 (b))를 함께 다시 정해야 한다. 그 전까지 응답은 원문이다.
8. **중복이 남을 수 있다** — M3a1c 규칙은 애매하면 합치지 않는다. T5에서 같은 사건이 두 번 보이면 PR 코멘트로 알리고 M3a1c 규칙(일반 링크 목록·겹침 기준)을 후속으로 보정한다.
9. **D3 나라·도시 링크 (test-engineer 관찰)** — D3 (iii) 일반 링크 충돌은 `대한민국` 과 `서울특별시` 를 서로 다른 일반 링크로 본다. 그래서 `[대회, 대한민국]` 과 `[대회, 서울특별시]` 두 줄은 같은 사건이어도 따로 남는다(두 번 보임 — 사건을 잃는 것보다 덜 나쁜 쪽). 데모 날짜에는 해당 줄이 없다(1988 개막의 두 문화 줄은 `대한민국` 을 공유해 합쳐짐). 이 기능에서는 고치지 않고 T5 (4)에서 본다. 보정안(도시를 그 나라에 포함해 겹침으로 보기 등)은 실제 사례가 모이면 후속으로 제안한다.
10. **줄 수가 예상보다 클 수 있다** — 보정 약 625로 400을 넘고, 최근 실측 비율(M3a1b 약 2.4배, M3a1c 약 3.5배 이상)로는 더 클 수 있다. PM이 한 PR((b), 2026-10-12)로 정했다. 대응: 중간 확인점(T2·T2b 뒤 누적 S1 360)에서 넘으면 T3 분리를 다시 묻는다.
11. **M3a1c 리뷰 권장 2·5는 M6b로** — 날짜 상한에 걸려 `future.cancel(true)` 가 캐시 쓰기(JDBC) 중인 가상 스레드를 인터럽트할 때의 영향, 링크가 많은 날 pageprops GET 주소가 길어져 414가 날 가능성. 데모 날짜에서는 생기지 않는다(링크 6개 이하). 로드맵 M6b 측정 목록에 적었다.

### 해결된 것

- **M3a1c 계약 이름** (2026-10-11 리스크 7) — M3a1c가 머지되어(PR #10) 이 계획서를 실제 이름(`KoWikiDayResult`, `KoWikiPage.State`, `KoWikiDocStatus`, `KoWikiEvent`)으로 맞췄다.
- **품질 필터가 좋은 사건을 뺄 수 있다** (2026-10-11 리스크 9) — 구조 필터를 지워(M3-P16 2026-10-12) 해당 없음.

## PM이 정할 것

이 기능에 걸리는 결정만 적었다. 전체 표와 추천 근거는 로드맵 M3 절 "M3 PM 결정". 남은 질문은 없다.

| # | 결정 | 추천 (전제) |
|---|---|---|
| #20 | 사건 1순위 조합 | (a). M3a3 뒤 실제 결과로 다시 본다 |
| #21 | 빈 날짜 처리 | (a) `NO_EVENTS_FOUND` 안내 + 같은 날 사진만 |
| #22 | `tone` 판정 | (c) 위 표. 추가 규칙(M3a3): `tragic` 사건에는 이미지를 붙이지 않고, `tragic` 사건이 있는 날은 `day_media` 를 비운다 |
| #23 | 위키백과 문장 | (a) |
| M3-P16 | 낮은 품질 원문 | **변경 (PM 결정 2026-10-12)**: 거르지 않고 원문 그대로 내보낸다. 큐레이션 `hide` 는 권리자 요청·명백히 틀린 줄을 바로 내리는 수단으로만. 해석·다듬기는 최종 화면 표시 때, 뒤의 웹·화면 기능에서 계획. (2026-10-11 (b) "연도 문서 문화 절 단독 줄 빼기"는 취소) |
| M3a2 분할 | 보정 약 625줄을 나눌지 | **(b) 결정 (PM 2026-10-12)**: 나누지 않고 한 PR. 중간 확인점 T2·T2b 뒤 누적 S1 360 |

### M3a2 분할 — PM 결정 (b) (2026-10-12, 기록)

보정 합계 약 625줄로 400줄 규칙(`CLAUDE.md` 4절)을 넘어 물었다. planner 추천은 (a)였고, PM은 (b)를 골랐다.

| 선택지 | 내용 | 결과 |
|---|---|---|
| (a) 지금 나눈다 | M3a2 = T2·T2b + 검증 + 실제 호출 확인(보정 약 360) / M3a2b `feat/world-events-tone` = T1·T3 + 검증 + `tone` 검수(보정 약 265) | 고르지 않음 |
| **(b) 한 PR, 중간 확인점만 옮긴다** | 이 계획서 그대로. T2·T2b 뒤 누적 S1이 360을 넘을 때만 다시 묻는다 | **결정** |
| (c) 한 PR, Wikidata 부분만 나중 기능으로 | 보정 약 515 | 고르지 않음 |
