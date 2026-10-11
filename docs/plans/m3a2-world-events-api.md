# 그날의 세계 API: 엔드포인트, 날짜 검증, 에러 형식, tone (M3a2)

> 브랜치: `feat/world-events-api` · 작성: planner · 상태: PM 승인 (2026-10-11)
> 기준: `CLAUDE.md` 7절, `docs/product-brief.md` 3절·5절 4번·6.3절·8절·12절, `docs/roadmap.md` M3, `docs/research/day-events.md` 3·4·5·8·10절, `docs/research/summary.md` 6절 #20~#23
> 선행: M3a1 `feat/wiki-events-source` 머지 (`KoWikiEventSource`, Wikimedia 외부 호출 공통, 녹화 fixture). 이 기능은 M3a1이 머지된 `main` 에서 갈라진다.

## 목표

첫 도메인 API `GET /api/story/world?date=YYYY-MM-DD` 를 만든다. 끝나면 다음이 된다.

1. 데모 날짜를 넣으면 같은 연·월·일의 사건을 한국어 원문 문장, 출처 문서 링크, 판 번호, CC BY-SA 4.0 표기와 함께 돌려준다.
2. 사건마다 `tone`(`normal`/`tragic`)과 판정 근거 `tone_basis` 가 있다(#22 (c)).
3. 사건이 없는 날은 `notice.code = NO_EVENTS_FOUND` 로 알린다(#21 (a)).
4. 잘못된 날짜는 400, 출처를 쓸 수 없으면 503을 **프로젝트 공통 에러 형식**(ProblemDetail + `code`)으로 돌려준다. M2·M4·M5의 API가 이 규약을 따른다.
5. 미디어 자리(`media`, `day_media`)는 응답 형태에 있지만 이 기능에서는 늘 빈 배열이다(M3a3이 채운다).

## 범위 밖

- 미디어(Commons), 공용 미디어 형식 → M3a3 (이 기능은 빈 배열만)
- 지원 시작 연도(#3) 검증 → M2b. 이 기능은 형식·존재하는 날짜·미래 날짜만 본다
- Wikidata 날짜 질의로 세계 사건을 더 찾는 것, Wikidata 날짜 교차 확인 → #20 후속(로드맵 "끝나면 PM이 정할 것")
- On this day (#20 (b)), 국내 사건 보강(#24, M3c)
- 응답 캐시 헤더(`Cache-Control`), API 버전 접두사
- 프론트엔드 (M3b2)

## 사용할 외부 출처

| 출처 | 확신도 | 이 기능에서 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| 한국어 위키백과 (M3a1 모듈) | **확인됨** | 사건 문장, 출처 문서, 판 번호. 추가로 대표 링크 문서의 QID 조회(`action=query&prop=pageprops&ppprop=wikibase_item`) | [day-events.md 3절](../research/day-events.md#3-출처-카드). 응답에 문서 링크·라이선스 고지에 필요한 값을 모두 담는다 |
| Wikidata Query Service (SPARQL) | **확인됨** | 사건 QID의 분류(P31/P279*), 사망자 수(P1120), 국가(P17) — `tone` 과 `scope` 신호 | CC0, 출처 표시 의무 없음. User-Agent 필수, 질의 60초 제한, 429 처리. 엔드포인트는 설정값(2026~2027 새 백엔드 이전 대비). 사건 날짜(P585)를 사진 촬영일로 쓰지 않는다 — [day-events.md 3절](../research/day-events.md#3-출처-카드), [8절 2항](../research/day-events.md#8-위험) |

키 없음. M3a1의 Wikimedia 공통 설정(User-Agent, 타임아웃, 재시도, 429)을 그대로 쓴다.

## 전제 (PM 결정과 가정)

| 결정 | 이 계획서의 전제 | PM이 다르게 정하면 바뀌는 곳 |
|---|---|---|
| #20 사건 1순위 조합 | (a) | (b): On this day 선택 어댑터를 별도 작은 기능으로 추가(M3a3 뒤). (c): 계획 다시 씀 |
| #21 빈 날짜 | (a) `notice` 로 안내, 같은 날 사진(M3a3의 `day_media`)만 | (b): 서비스가 같은 달 사건으로 넓히고 사건에 `date_relation` 을 붙인다(T2 +약 40줄, 제품 정의서 5절 4번 수정 필요). (c): API는 같고 M3b2가 섹션을 숨긴다 |
| #22 `tone` 판정 | (c) 큐레이션 → 키워드 → Wikidata 구조화 → 판정 불가면 `tragic` | (a): 큐레이션·키워드 제거(T1 없어짐, T3 −약 60줄). (b): 큐레이션만, 나머지는 모두 `tragic`/`default`(T3의 Wikidata 제거) |
| #23 위키백과 문장 | (a) 원문 그대로, `summary_modified` 는 늘 `false` | (b): 필드만 쓰이게 되고 이 기능 코드는 그대로 |
| M3-P1 분할 | 로드맵 M3 절 (a) | |

## API 계약 (프로젝트 공통 규약 + 그날의 세계)

### 공통 규약 (이후 모든 `/api` 도메인 엔드포인트)

| 항목 | 규약 |
|---|---|
| 경로 | 결과 화면 섹션마다 `GET /api/story/<section>`. 섹션 이름은 M1b `SectionId` 와 같다(`world`, 이후 `neighborhood`, `city`). 장소 해석은 `GET /api/places/resolve`(M2a) |
| 요청 | 조회는 GET + 쿼리 파라미터 |
| JSON 이름 | **snake_case** (제품 정의서 6.2절 필드 이름 그대로). 구현 방법(전역 설정 또는 DTO 주석)은 backend-dev가 Spring Boot 4.1·Jackson 3 문서로 확인해 고른다 |
| 날짜·시각 | 날짜 `YYYY-MM-DD`, 시각 ISO-8601 UTC(`2026-10-12T03:00:00Z`) |
| 에러 | `application/problem+json` (RFC 9457 ProblemDetail) + 확장 필드 `code`(대문자 스네이크). `instance` 는 경로만(쿼리 문자열 없음). `detail` 은 영어 설명이고 **입력값(날짜·장소)을 넣지 않는다**. 화면 문구는 프론트엔드가 `code` 로 정한다 |
| 일부 실패 | 섹션 안의 보조 출처가 실패해도 200으로 답하고 `partial: true`. 섹션의 필수 출처를 전혀 쓸 수 없을 때만 503 |
| 로그 | 요청 파라미터 값을 로그에 남기지 않는다. 400은 로그 없음, 503은 WARN(출처·상태만) |

### `GET /api/story/world`

요청: `GET /api/story/world?date=1988-09-17`

| 상태 | `code` | 조건 |
|---|---|---|
| 400 | `DATE_REQUIRED` | `date` 가 없거나 빈 값 |
| 400 | `DATE_FORMAT` | `^\d{4}-\d{2}-\d{2}$` 가 아님 (예 `1988-9-17`) |
| 400 | `DATE_NOT_EXIST` | 형식은 맞지만 없는 날짜 (예 `2001-02-29`, `1988-02-30`) |
| 400 | `DATE_IN_FUTURE` | `Asia/Seoul` 기준 오늘보다 뒤 (오늘은 허용) |
| 503 | `UPSTREAM_UNAVAILABLE` | 날짜 문서와 연도 문서를 모두 쓸 수 없음(캐시도 없음, M3a1 `allUnavailable()`) |
| 200 | - | 그 밖 (사건 0개 포함) |

400 예:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"about:blank","title":"Bad Request","status":400,
 "detail":"date must be an existing calendar date in YYYY-MM-DD format",
 "instance":"/api/story/world","code":"DATE_NOT_EXIST"}
```

200 예 (1988-09-17 — 값은 예시이고 실제 값은 fixture를 따른다):

```json
{
  "date": "1988-09-17",
  "events": [
    {
      "id": "kowiki-1988-09-17-1a2b3c4d",
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
    }
  ],
  "day_media": [],
  "notice": null,
  "partial": false
}
```

200 예 (1995-03-15):

```json
{"date":"1995-03-15","events":[],"day_media":[],"notice":{"code":"NO_EVENTS_FOUND"},"partial":false}
```

### 응답 필드

| 필드 | 형 | 설명 |
|---|---|---|
| `date` | string | 요청 날짜 |
| `events[]` | array | 아래 사건. 순서: `tone=normal` 먼저(원래 순서 유지), 그다음 `tragic`. 같은 `tone` 안에서는 M3a1 순서 |
| `day_media[]` | array | 같은 날 찍힌 사진(공용 미디어 형식, M3a3). 이 기능에서는 늘 `[]` |
| `notice` | object \| null | `events` 가 비면 `{"code":"NO_EVENTS_FOUND"}`, 아니면 `null` |
| `partial` | boolean | 이번 요청에서 보조 출처 실패나 오래된 캐시 사용이 있었으면 `true` (M3a1 `degraded()` 또는 Wikidata 실패, M3a3 이후 Commons 실패) |

사건(`events[]`) 필드 — 리서치의 "사건 레코드 필드 제안"([day-events.md 4절](../research/day-events.md#4-미디어-레코드-필드-매핑))을 확정한 것:

| 필드 | 형 | 채우는 법 |
|---|---|---|
| `id` | string | M3a1 사건 ID |
| `date` | string | 요청 날짜와 같다 (같은 날짜만 노출) |
| `scope` | `domestic` \| `world` \| `unknown` | 큐레이션 값 → QID의 P17에 대한민국(Q884)이 있으면 `domestic`, P17이 있고 Q884가 없으면 `world` → 그 밖 `unknown` |
| `title` | string \| null | 대표 링크 문서 제목. 없으면 `null` |
| `summary` | string | M3a1 평문 그대로 |
| `summary_lang` | `ko` | 고정 |
| `summary_modified` | boolean | `false` (#23 (a)) |
| `text_license`, `text_license_url` | string | `CC BY-SA 4.0`, CC BY-SA 4.0 한국어 증서 주소 |
| `source_name` | string | `한국어 위키백과` |
| `source_title` | string | `9월 17일` 또는 `1988년` |
| `source_url` | string | 출처 문서 주소(`https://ko.wikipedia.org/wiki/` + 제목, 공백은 `_`, UTF-8 퍼센트 인코딩). 판 고정 주소가 아니라 문서 주소(사용자에게 보여 줄 링크) |
| `source_section` | `사건` \| `문화` | |
| `source_revision` | number | 읽은 판 번호(`revid`) |
| `wikidata_qid` | string \| null | 대표 링크 문서의 `wikibase_item`. 없거나 조회 실패면 `null` |
| `tone` | `normal` \| `tragic` | 아래 판정 |
| `tone_basis` | `curated` \| `rule:keyword` \| `rule:class` \| `rule:deaths` \| `rule:no-signal` \| `default` | 판정 근거. `rule:no-signal` 은 리서치 제안에 없던 값으로, 구조화 규칙을 실제로 평가했고 신호가 없어 `normal` 이 된 경우다 |
| `related_links[]` | `{title, url}` | M3a1 `linkTitles` 의 문서 주소 |
| `media[]` | array | 공용 미디어 형식(M3a3). 이 기능에서는 `[]` |
| `retrieved_at` | string | 출처 문서를 받은 시각(캐시 행의 `fetched_at`) |

### `tone` 판정 (#22 (c))

순서대로 처음 맞는 것을 쓴다.

| 순서 | 조건 | `tone` | `tone_basis` |
|---|---|---|---|
| 1 | 큐레이션 항목과 맞음 (`data/curation/event-tone.json`) | 항목 값 | `curated` |
| 2 | `summary` 에 비극 키워드가 있음 | `tragic` | `rule:keyword` |
| 3 | QID가 있고 Wikidata 평가 성공, 분류가 비극 분류 목록의 하위 | `tragic` | `rule:class` |
| 4 | QID가 있고 Wikidata 평가 성공, 사망자 수 > 0 | `tragic` | `rule:deaths` |
| 5 | QID가 있고 Wikidata 평가 성공, 신호 없음 | `normal` | `rule:no-signal` |
| 6 | 그 밖 (QID 없음, Wikidata 실패) | `tragic` | `default` |

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
      "reason": "올림픽 개막. 비극 요소 없음",
      "evidence_url": "https://ko.wikipedia.org/w/index.php?title=9%EC%9B%94_17%EC%9D%BC&oldid=42604413",
      "reviewed_at": "2026-10-12"
    }
  ]
}
```

- 맞추는 법: `date` 가 같고, `primary_link` 가 사건의 대표 링크와 같거나(밑줄·공백 차이 무시) `summary_contains` 가 `summary` 에 들어 있으면 맞음. 둘 중 하나는 반드시 있다.
- 스키마: `data/schema/event-curation.schema.json` (JSON Schema 2020-12, content-curator).
- 서버는 `app.data-dir`(기본 `../data`, `server/` 에서 실행 기준) 아래 파일을 시작할 때 읽는다. 파일이 없으면 WARN 후 큐레이션 없이 동작, 형식이 틀리면 시작 실패.

### QID와 Wikidata 조회

1. 사건들의 대표 링크를 모아 위키백과에 한 번 묻는다: `action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&titles=<제목1>|<제목2>…&format=json&formatversion=2` (최대 50개, 넘으면 나눠서). 리다이렉트·정규화 결과를 따라 제목 → QID를 만든다.
2. QID들을 Wikidata에 한 번 묻는다(`wikimedia.wikidata-sparql-url`, 기본 `https://query.wikidata.org/sparql`, `format=json`). 질의 모양:

```sparql
SELECT ?item ?tragicClass ?deaths ?country WHERE {
  VALUES ?item { wd:Q8470 wd:Q6064606 }
  OPTIONAL { ?item wdt:P31/wdt:P279* ?tragicClass .
             VALUES ?tragicClass { wd:Q3839081 wd:Q171558 wd:Q2223653 wd:Q350604 } }
  OPTIONAL { ?item wdt:P1120 ?deaths }
  OPTIONAL { ?item wdt:P17 ?country }
}
```

3. Wikidata 읽기 타임아웃은 `wikimedia.wikidata-read-timeout: 5s`. 실패하면 모든 사건이 순서 6(`default`)로 가고 `partial: true`.

## 테스트 방침

M3a1 계획서 "테스트 방침"을 따른다. 이 기능에서 추가하는 것:

- 녹화 fixture: 데모 사건 대표 링크의 pageprops 응답(`fixtures/wikimedia/kowiki/`), 그 QID들의 SPARQL 응답(`fixtures/wikimedia/wikidata/`, `README.md` 에 원 URL·녹화 일자·라이선스 CC0). WireMock은 SPARQL 요청을 경로 + `query` 에 든 QID로 맞춘다.
- 합성 fixture: `tone`·`scope` 판정 분기마다 가짜 QID(`Q1`~`Q9`)와 가짜 SPARQL 응답. 실제 비극 사건 이름을 쓰지 않는다.
- 날짜 검증 테스트는 고정 시계(`2026-10-10`, `Asia/Seoul`)를 쓴다.
- 컨트롤러 형식 테스트(400 등)는 `KoWikiEventSource` 를 목으로 바꿔도 되고, 데모 날짜 테스트는 M3a1 녹화 fixture + WireMock으로 끝까지 돌린다.

## 데모 입력 3개로 확인할 것

| 데모 입력 | 확인 (녹화 fixture 기준) |
|---|---|
| 1988-09-17 + "서울" | 200. `summary` 에 "서울 하계 올림픽"이 든 사건 1개, `wikidata_qid` = `Q8470`, `tone` = `normal`, `tone_basis` = `curated`, `source_url` = 9월 17일 문서 주소, `source_revision` = fixture `revid`, `text_license` = `CC BY-SA 4.0`, `media` = `[]`. `notice` = `null` |
| 2002-06-22 + "광주 서구 풍암동" | 200. `source_section` = `문화` 이고 "스페인"이 든 사건, `tone` = `normal` (큐레이션) |
| 1995-03-15 + "서울대학교병원" | 200. `events` = `[]`, `notice.code` = `NO_EVENTS_FOUND`, `partial` = `false` |

데모 날짜에 나오는 모든 사건의 `tone`·`tone_basis`·`scope` 를 T4가 표로 보고하고 PM이 T5에서 검수한다(로드맵 "끝나면 PM이 정할 것").

## 크기 (400줄 규칙)

예상: 컨트롤러·날짜 검증·에러 처리 약 100, DTO 약 50, 서비스 약 60, `tone`·`scope`(큐레이션 로더·키워드·조합) 약 90, pageprops 조회 약 25, Wikidata 클라이언트 약 70 → **약 395줄**. 데이터 파일(`data/**`)은 따로 보고한다.
- **T2가 끝났을 때 누적 230줄을 넘으면** 오케스트레이터가 PM에게 T3의 Wikidata 부분(pageprops·SPARQL·`rule:class`/`rule:deaths`/`rule:no-signal`/`scope`)을 별도 기능으로 뺄지 묻는다. 빼면 그동안 큐레이션·키워드에 걸리지 않는 사건은 모두 `tragic`/`default` 가 된다.

확인 명령은 아래 S1·S2·S3.

## 작업 목록

순서: T1과 T2는 병렬 가능(서로 다른 폴더) → T3 → T4 → T5.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 사건 큐레이션 스키마와 데모 날짜 `tone` 큐레이션 | content-curator | `data/schema/event-curation.schema.json`, `data/curation/event-tone.json` | - (M3a1 머지) | (1) 스키마가 위 "큐레이션 파일 형식"의 필드·규칙(`primary_link`·`summary_contains` 중 하나 필수, `tone` 은 `normal`/`tragic`, `scope` 는 세 값 중 하나, `date` 형식, `evidence_url` 필수)을 담은 JSON Schema 2020-12. (2) M3a1 T4 표의 데모 날짜 사건(1988-09-17, 2002-06-22) **전부**에 항목이 있고, 항목마다 `evidence_url` 이 fixture `revid` 를 쓴 판 고정 주소이며 `reason` 이 사실만 적는다. 1995-03-15 항목은 없다(사건 없음). (3) C1이 두 파일 모두 `ok` 를 출력한다. (4) 보고서에 항목 표(날짜, 대표 링크, `tone`, `scope`, 근거)를 적는다 |
| T2 | `GET /api/story/world` 골격: 날짜 검증, 공통 에러 형식, 응답 DTO, `notice`, 503, `partial` | backend-dev | 컨트롤러, 날짜 파라미터 검증, 전역 예외 처리(ProblemDetail + `code`), 응답 DTO(snake_case), 서비스(`KoWikiEventSource` 사용, `tone` 은 임시로 모두 `tragic`/`default` 를 내는 판정기 인터페이스), 테스트 | - (M3a1 머지) | (1) `cd server && ./gradlew check` 성공. (2) `DATE_REQUIRED`(파라미터 없음, 빈 값), `DATE_FORMAT`(`1988-9-17`), `DATE_NOT_EXIST`(`2001-02-29`, `1988-02-30`) → 400, `Content-Type` `application/problem+json`, 본문 `code`·`status`·`instance`=`/api/story/world`, 본문 어디에도 입력 문자열이 없다. (3) 고정 시계 2026-10-10: `2026-10-11` → 400 `DATE_IN_FUTURE`, `2026-10-10` → 200. 경계값 `2000-02-29` → 200(합성 fixture). (4) 1988-09-17 → 200, 응답 JSON 키가 snake_case(`source_revision`, `text_license`, `day_media` 등 위 표의 키 전부 존재), `media` = `[]`, `day_media` = `[]`, `summary` 에 "서울 하계 올림픽"이 든 사건 1개, `source_url`·`source_revision`·`text_license` 가 위 "데모" 표와 같다. (5) 1995-03-15 → `events` = `[]`, `notice.code` = `NO_EVENTS_FOUND`. (6) 캐시 없음 + 두 문서 503 → 503 `UPSTREAM_UNAVAILABLE`(problem+json). 한 문서만 실패 → 200, `partial` = `true`. (7) `GET /api/health` 는 여전히 `{"status":"UP"}`, `GET /api` 는 404 |
| T3 | `tone`·`scope`: 큐레이션 로더, 키워드 규칙, QID 조회, Wikidata 클라이언트, 판정 조합 | backend-dev | 큐레이션 로더(`app.data-dir`), 키워드 리소스, pageprops 조회, Wikidata SPARQL 클라이언트(`wikimedia.wikidata-sparql-url`, `wikidata-read-timeout`), 판정기, fixture, 테스트 | T1, T2 | (1) 기본 설정에서 `../data/curation/event-tone.json` 을 읽는다. 형식이 틀린 파일(테스트용 사본) → 컨텍스트 시작 실패, 파일 없음 → WARN 후 동작. (2) 판정 분기 테스트(합성 QID): 큐레이션이 키워드보다 우선, 키워드 → `rule:keyword`, 비극 분류 → `rule:class`, 사망자 > 0 → `rule:deaths`, QID 있고 신호 없음 → `normal`/`rule:no-signal`, QID 없음 → `tragic`/`default`, Wikidata 503 → `tragic`/`default` + `partial` = `true`. (3) `scope` 분기: 큐레이션 값 우선, P17에 Q884 → `domestic`, 다른 나라만 → `world`, P17 없음·QID 없음 → `unknown`. (4) WireMock: API 호출 한 번에 pageprops 요청 1회(제목을 `\|` 로 묶음), SPARQL 요청 1회(모든 QID가 `VALUES` 에), 두 요청 모두 `User-Agent` 헤더. (5) 데모: 1988-09-17 올림픽 사건 `wikidata_qid` = `Q8470`, `tone` = `normal`/`curated`. 2002-06-22 "스페인" 사건 `tone` = `normal`/`curated`. (6) `fixtures/wikimedia/wikidata/README.md` 에 원 URL·녹화 일자·CC0 |
| T4 | 완료 기준 검증, 스키마 검증 테스트, 전체 테스트 | test-engineer | 빠진 기준의 테스트, `data/curation/event-tone.json` 을 `data/schema/event-curation.schema.json` 으로 검증하는 테스트(JSON Schema 검증 라이브러리를 테스트 의존성으로 추가 — M3a3도 쓴다) | T3 | (1) T2·T3 기준을 표로 확인하고 빠진 테스트를 추가. (2) 스키마 검증 테스트가 통과하고, 일부러 틀린 사본(필수 필드 없음)은 실패한다. (3) `cd server && ./gradlew check` 통과(개수 보고), 3회 반복 안정. (4) C2 출력 없음. (5) S1·S2·S3 결과 보고. (6) 데모 날짜 사건의 `tone`·`tone_basis`·`scope`·`wikidata_qid` 표를 보고 |
| T5 | 실제 호출 확인과 `tone` 검수 (PR 검수 때) | PM | - | T4 | (1) 루트 `.env` 에 `WIKIMEDIA_USER_AGENT` 값을 넣고 `docker compose config --quiet` 가 종료 코드 0. (2) `docker compose up -d db`, `cd server && ./gradlew bootRun` 후 P1~P4 명령 결과가 "데모" 표와 맞는다. (3) T4의 `tone` 표를 보고 틀린 판정이 있으면 PR 코멘트로 알린다(content-curator가 큐레이션을 고친다) |

완료 기준에 쓰는 명령 (저장소 루트):

```
# C1 — 큐레이션 파일이 JSON으로 읽히는지 (content-curator). 파일마다 ok
for f in data/schema/event-curation.schema.json data/curation/event-tone.json; do
  python3 -c 'import json,sys; json.load(open(sys.argv[1])); print("ok", sys.argv[1])' "$f"; done

# C2 — 컨트롤러·예외 처리에서 요청 값을 로그로 남기지 않는지. 출력을 사람이 확인 (code-reviewer도 본다)
grep -rnE 'log\.(info|warn|error|debug)\(' server/src/main/java | grep -iE 'date|query|param'

# S1 — 제품 코드·설정 줄 수
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
# S3 — 데이터 파일 줄 수
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P1~P4 — PM 실제 호출 확인 (서버 실행 중)
curl -s 'http://localhost:8080/api/story/world?date=1988-09-17' | jq '.events[] | {summary, tone, tone_basis, scope, source_revision}'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' | jq '.events[] | {summary, source_section, tone}'
curl -s 'http://localhost:8080/api/story/world?date=1995-03-15' | jq '{events, notice}'
curl -s -i 'http://localhost:8080/api/story/world?date=2001-02-29' | head -n 20
```

담당별: content-curator 1 (T1), backend-dev 2 (T2, T3), test-engineer 1 (T4), PM 1 (T5).

## 리스크와 확인할 것

1. **세계 사건이 데모 날짜에 거의 없다** — #20 (a) 조합으로는 데모 3개 날짜 모두 국내 사건만 나오거나(1988, 2002) 비어 있다(1995). "그날의 세계"라는 제목과 어긋날 수 있다. → M3a3 뒤에 실제 결과를 보고 #20 (b)(On this day — 2002 이란 지진 1건 추가) 또는 Wikidata 날짜 질의 세계 사건을 PM이 다시 정한다(로드맵 "끝나면 PM이 정할 것").
2. **`default` 판정이 많을 수 있다** — 대표 링크가 없는 줄이나 Wikidata 실패 때 `tragic` 으로 간다. 데모 날짜는 큐레이션으로 덮는다. 데모 밖 날짜에서 차분한 표시가 많아지는 것은 의도된 보수적 동작이다.
3. **P279* 질의가 느릴 수 있다** — 상위 분류를 따라가는 질의는 60초 제한 안에서도 느릴 수 있다. 읽기 타임아웃 5초를 넘으면 `default` 로 간다. T3 보고서에 녹화 때 걸린 시간을 적는다.
4. **Spring Boot 4.1·Jackson 3** — ProblemDetail 설정 키, snake_case 설정 방법, 예외 처리 클래스 이름이 Boot 3과 다를 수 있다. 보고서에 고른 방법을 적는다.
5. **`.env` 의 User-Agent 값** — 공백·괄호가 든 값을 Compose가 문제없이 읽는지 T5 (1)에서 본다. 문제가 있으면 M1a 리스크 5처럼 `.env.example` 주석을 고치는 작은 후속 작업으로 처리한다.

## PM이 정할 것

이 기능에 걸리는 결정만 적었다. 전체 표와 추천 근거는 로드맵 M3 절 "M3 PM 결정".

| # | 결정 | 추천 (전제) |
|---|---|---|
| #20 | 사건 1순위 조합 | (a). M3a3 뒤 실제 결과로 다시 본다 |
| #21 | 빈 날짜 처리 | (a) `NO_EVENTS_FOUND` 안내 + 같은 날 사진만 |
| #22 | `tone` 판정 | (c) 위 표. 추가 규칙(M3a3): `tragic` 사건에는 이미지를 붙이지 않고, `tragic` 사건이 있는 날은 `day_media` 를 비운다 |
| #23 | 위키백과 문장 | (a) |
