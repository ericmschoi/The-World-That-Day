# 위키백과 호출 보강과 위키텍스트 캐시 (M3a1b)

> 브랜치: `feat/wiki-section-cache` · 작성: planner · 상태: **PM 승인 (2026-10-11)** (2026-10-11 PM 결정 "M3a1 분할"로 생긴 기능. M3a1b·M3a1c 재분할도 같은 날 승인)
> 기준: `docs/plans/m3a1-wiki-events-source.md`(설계·테스트 방침), `CLAUDE.md` 7절, `docs/product-brief.md` 6절·8절·12절, `docs/research/day-events.md` 3절·8절(API 예절), 결정 M3-P3 (a), #23 (a)
> 선행: M3a1 `feat/wiki-events-source` 머지. 뒤따르는 기능: **M3a1c `feat/wiki-events-merge`**(`KoWikiEventSource`, 중복 제거 — `docs/plans/m3a1c-wiki-events-merge.md`), 그다음 M3a2.

## 왜 M3a1 T3을 다시 둘로 나눴나

PM이 옮기라고 한 원래 M3a1 T3(캐시 + 합치기 + `KoWikiEventSource`)에 M3a1 검증(T4, 8feaf87)과 M3a1 코드 리뷰(승인)에서 나온 일이 더해졌다: 중복 제거 강화·링크 정규화, 날짜당 시간 상한, `ratelimited` 처리, 동시 요청 제한, HTML 태그·엔티티 규칙. 예상은 약 365줄이지만 M3a1의 실제 줄 수가 예상의 약 1.6배였으므로 한 PR에 넣으면 400줄을 크게 넘는다. 그래서 성격대로 나눴다.

| 기능 | 브랜치 | 내용 | 예상 (보정) |
|---|---|---|---|
| **M3a1b (이 계획서)** | `feat/wiki-section-cache` | 위키백과 호출 보강(`ratelimited`, 동시 요청 3개, HTML 태그·엔티티) + 위키텍스트 캐시 V2(클라이언트를 감싸는 캐시) | 약 150 (약 240) |
| M3a1c | `feat/wiki-events-merge` | `KoWikiEventSource`, 불변 조건, 날짜당 시간 상한, 링크 정규화·QID, 중복 제거 | 약 210 (약 340) |

캐시를 `KoWikiClient` 를 감싸는 형태(`CachingKoWikiClient`)로 바꿔, 캐시가 `KoWikiEventSource` 없이도 완결되게 했다. M3a1c는 `KoWikiClient` 인터페이스만 쓰므로 캐시가 있는지 모른다.

## 목표

1. 위키백과 호출이 API 예절을 더 지킨다: HTTP 200 + `error.code = ratelimited` 도 429처럼 그 호스트를 잠시 막고, Wikimedia 요청은 동시에 3개까지만 나간다.
2. 파서가 처리하지 않던 HTML 태그(`<br>`, `<small>`, `<sup>`, `<span>` 등)가 든 줄은 템플릿 줄처럼 건너뛰고 세며, HTML 엔티티(`&amp;`, `&ndash;` 등)는 같은 글자로 풀어 쓴다("내용을 바꾸지 않는다" 원칙, #23 (a)).
3. 읽은 절(사건·문화)의 위키텍스트를 DB에 캐시하고(Flyway V2), 갱신에 실패하면 오래된 캐시를 쓴다.

## 범위 밖

- `KoWikiEventSource`, 합치기·중복 제거, 링크 정규화, 날짜당 시간 상한 → M3a1c
- 503 + `Retry-After` 의 호스트 차단, 절대 URL 검증 → 오케스트레이터가 M3a1 안에서 고친다(이 계획에 넣지 않음)
- 위키텍스트 템플릿 처리("안전한 템플릿 목록") → 후속 작업 후보 `chore/kowiki-safe-templates`
- `prop=sections` → `prop=tocdata` 전환 → 후속 작업 후보 `chore/kowiki-tocdata`
- 날짜 문서 366개 미리 받기 → M6b
- 탄생·사망·기념일 절 — 요청하지도, 저장하지도 않는다

## 사용할 외부 출처

| 출처 | 확신도 | 근거 | 지킬 조건 |
|---|---|---|---|
| 한국어 위키백과 날짜·연도 문서 (M3a1 `KoWikiClient`) | **확인됨** | [day-events.md 3절](../research/day-events.md#3-출처-카드) | CC BY-SA 4.0 — 판 번호를 남긴다. 캐시 허용(복제 허용 라이선스, API 예절이 캐시를 권장). "사건"·"문화" 절만 저장. API 예절: 429·`ratelimited` 존중, 동시 요청을 적게(권장 최대 3) |

새 외부 호출은 없다. 키는 필요 없다.

## 설계

### 호출 보강 (M3a1 리뷰 입력 1·3)

| 항목 | 규칙 |
|---|---|
| `ratelimited` | Action API가 HTTP 200에 `error.code = "ratelimited"` 를 담아 보내면 429와 같이 다룬다: 재시도하지 않고, `KoWikiClient` 가 `WikimediaHttpClient` 에 그 호스트 차단을 요청한다(차단 시간은 429에서 `Retry-After` 가 없을 때와 같은 30초, 최대 `rate-limit-max-wait`). 그 문서 상태는 `UNAVAILABLE`. 이를 위해 `WikimediaHttpClient` 에 "호스트 차단" 입구를 하나 연다(429 차단기와 같은 저장소) |
| 동시 요청 제한 | `WikimediaHttpClient` 가 모든 Wikimedia 요청(위키백과·Wikidata·Commons)에 공유 허용 수 `wikimedia.max-concurrent-requests: 3` 을 건다(예: `Semaphore(3)`). 허용을 기다리는 시간은 읽기 타임아웃까지이고, 못 받으면 "사용 불가"(요청을 보내지 않음). 재시도도 허용을 다시 받는다 |

### HTML 태그·엔티티 (M3a1 리뷰 입력 4)

| 대상 | 처리 | 이유 |
|---|---|---|
| 엔티티 — 이름 목록(`&amp;` `&lt;` `&gt;` `&quot;` `&apos;` `&nbsp;` `&ndash;` `&mdash;` `&hellip;` `&middot;`)과 숫자 엔티티(`&#8211;`, `&#x2013;`) | 같은 글자로 푼다 | 화면에 보이는 글자가 같다. 내용을 바꾸지 않는다 |
| 목록에 없는 이름 엔티티 | 그 줄을 건너뛰고 센다 | 무슨 글자인지 확정할 수 없다 |
| HTML 태그 — `<ref…>…</ref>`, `<ref …/>`, `<!-- -->`(이미 지움) 밖의 모든 태그(`<br>`, `<small>`, `<sup>`, `<sub>`, `<span …>`, `<s>`, `<u>` 등) | 그 줄을 건너뛰고 센다(`skippedLines`) | 태그를 지우면 줄바꿈·위첨자·취소선 같은 뜻이 사라져 내용이 바뀔 수 있다. 템플릿 줄과 같은 원칙 |

데모 사건 줄(1988-09-17 올림픽 개막, 2002-06-22 스페인 경기)이 이 규칙으로 빠지면 특수 처리하지 말고 그 줄과 선택지를 "막힘"으로 보고한다.

### 캐시 (Flyway V2, M3-P3 (a)) — `KoWikiClient` 를 감싼다

```sql
-- server/src/main/resources/db/migration/V2__wiki_section_cache.sql (예시)
CREATE TABLE wiki_section_cache (
    id               BIGSERIAL PRIMARY KEY,
    site             VARCHAR(64)  NOT NULL,   -- 'ko.wikipedia.org'
    page_title       VARCHAR(255) NOT NULL,   -- '9월 17일', '1988년'
    revid            BIGINT       NOT NULL,
    events_wikitext  TEXT,                    -- "사건" 절. 절이 없으면 NULL
    culture_wikitext TEXT,                    -- "문화" 절. 절이 없으면 NULL
    fetched_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_wiki_section_cache UNIQUE (site, page_title)
);
```

- `CachingKoWikiClient implements KoWikiClient` 가 `ActionApiKoWikiClient` 를 감싼다. 빈 등록은 감싼 쪽이 기본(`@Primary` 등 — backend-dev가 고르고 보고).
- 문서 상태(M3a1 `DocState` 에 `CACHED`, `STALE` 을 더한다): TTL `wikimedia.cache-ttl: 7d` 안이면 외부 호출 없이 `CACHED`. 오래됐으면 다시 받아 성공하면 갱신(`FRESH`), 실패(`UNAVAILABLE`·차단 포함)하면 오래된 캐시를 쓴다(`STALE`). 캐시도 없으면 `UNAVAILABLE`.
- `ABSENT`(문서 없음)는 캐시하지 않는다.
- 저장은 같은 제목이면 갱신이고, 같은 제목을 두 번 저장해도 요청이 실패하지 않는다(유일 제약 충돌 처리).
- 캐시하는 것은 위키텍스트 원문이다. 파싱·중복 제거는 읽을 때마다 한다(규칙이 바뀌어도 캐시를 지울 필요가 없다).
- 문서 결과에 `fetchedAt`(캐시 행의 `fetched_at`)을 담는다. M3a1c가 사건 `retrievedAt` 으로 쓴다.
- 테이블에는 요청한 사람에 관한 값(IP, 요청 날짜 기록 등)을 넣지 않는다. 탄생·사망 절은 받지 않으므로 저장될 수 없다.
- 엔티티는 API 응답으로 내보내지 않는다. `ddl-auto=validate` 가 통과해야 한다.

### 설정 추가분

```yaml
wikimedia:
  max-concurrent-requests: 3
  cache-ttl: 7d
```

## 테스트 방침

M3a1 계획서 "테스트 방침"을 그대로 따른다(녹화 fixture 재사용, 합성 fixture는 `synthetic-` 접두사·중립 문장, `Clock` 으로 시간 이동, 외부 네트워크 없음, 데모·공개 역사 사건 날짜·경계값만). DB 테스트는 Testcontainers(M1a 방식). 동시성 테스트는 `Thread.sleep` 대신 WireMock 응답 지연과 래치를 쓴다.

## 데모 입력 3개로 확인할 것 (녹화 fixture 기준, 문서 단위)

| 데모 입력 | 확인 |
|---|---|
| 1988-09-17 + "서울" | `9월 17일`·`1988년` 첫 조회 `FRESH`, 두 번째 조회 `CACHED`(요청 수 그대로), 캐시를 거친 파서 결과에 올림픽 개막 줄이 그대로 있다 |
| 2002-06-22 + "광주 서구 풍암동" | `6월 22일` 문화 절 파서 결과에 "스페인"이 든 줄이 그대로 있다(HTML 규칙 뒤에도) |
| 1995-03-15 + "서울대학교병원" | `3월 15일`·`1995년` 이 캐시에 저장되고 그날 사건 0개 |

## 크기 (400줄 규칙)

예상: `ratelimited`·호스트 차단 입구 약 15, 동시 요청 제한 약 20, HTML 태그·엔티티 약 25, 캐시(마이그레이션·엔티티·저장소·감싼 클라이언트·TTL·STALE) 약 90 → **약 150줄**(보정 약 240).

## 작업 목록

순서: T1 → T2 → T3. (T1과 T2는 서로 다른 파일이 대부분이라 순서를 바꿔도 된다)

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 호출 보강: `ratelimited`, 동시 요청 제한, HTML 태그·엔티티 규칙 | backend-dev | `WikimediaHttpClient` 호스트 차단 입구·허용 수, `ActionApiKoWikiClient` `ratelimited` 처리, `WikitextEventParser` 태그·엔티티 규칙, 합성 fixture, 테스트 | - (M3a1 머지) | (1) `cd server && ./gradlew check` 성공. (2) HTTP 200 + `{"error":{"code":"ratelimited",…}}` → 그 문서 `UNAVAILABLE`, 재시도 없음(요청 1), 시계를 29초 옮겨 다시 조회하면 요청 수가 늘지 않고, 31초 옮기면 요청이 나간다. (3) 동시 요청: WireMock 응답 지연 200ms로 조회 6개를 동시에 시작하면 WireMock이 동시에 처리 중인 요청 수의 최댓값이 3 이하(수신 시각 기록으로 측정)이고 6개 모두 성공. 허용을 읽기 타임아웃 안에 못 받으면 "사용 불가"이고 그 요청은 WireMock에 도착하지 않는다. (4) 파서(합성): `<br>`·`<small>`·`<sup>`·`<span style=…>` 이 든 줄 → 건너뛰고 `skippedLines` 증가(태그마다 테스트). `&amp;`·`&ndash;`·`&#8211;`·`&#x2013;` → 같은 글자로 풀린 본문. 목록에 없는 이름 엔티티(`&foo;`) 줄 → 건너뜀. `<ref>`·주석 처리는 M3a1 그대로. (5) 녹화 fixture 6개를 다시 파싱해 M3a1 T4 표와 비교한 전·후 표(새로 건너뛴 줄 수, 태그 이름별)를 보고서에 적고, 데모 사건 줄은 그대로 나온다 |
| T2 | 위키텍스트 캐시(V2) — `CachingKoWikiClient` | backend-dev | `V2__wiki_section_cache.sql`, 엔티티·저장소, 감싼 클라이언트, `DocState` 에 `CACHED`·`STALE`, `wikimedia.cache-ttl`, 테스트(Testcontainers) | T1 | (1) `flyway_schema_history` 에 `version = '2'`, `success = true`, `ddl-auto=validate` 로 컨텍스트가 뜬다. (2) `9월 17일` 첫 조회는 WireMock에 요청이 가고(`FRESH`), TTL 안의 두 번째 조회는 요청 수가 늘지 않는다(`CACHED`). (3) 시계를 TTL 너머로 옮기고 WireMock이 503 → 이전 위키텍스트·`revid` 를 그대로 돌려주고 상태 `STALE`. `ratelimited` 응답이어도 `STALE`. TTL 너머 + 200(다른 `revid` 합성) → `FRESH`, 행의 `revid`·`fetched_at` 갱신. (4) 캐시 없음 + 실패 → `UNAVAILABLE`. (5) 저장된 `events_wikitext`·`culture_wikitext` 가 fixture의 사건·문화 절 본문과 같고, 테이블에 다른 절 내용이 없다. (6) 같은 제목을 두 번 저장해도 예외가 없고 행은 1개. (7) `missingtitle` 문서는 행이 생기지 않고 다음 조회에서 다시 요청한다. (8) 문서 결과의 `fetchedAt` 이 캐시 행의 `fetched_at` 과 같다. (9) "데모" 표 3행 통과 |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트(테스트 파일만) | T2 | (1) T1·T2 기준을 표로 확인하고 빠진 테스트를 추가. (2) `cd server && ./gradlew check` 통과(개수 보고), 3회 반복 안정(동시성 테스트 포함). (3) C1, C2 출력 없음. (4) S1·S2 결과 보고 |

완료 기준에 쓰는 검사 명령 (저장소 루트, M3a1과 같음):

```
# C1 — fixture에 탄생·사망 절이 없는지. 출력 없음
grep -rlnE '==[[:space:]]*(탄생|사망)[[:space:]]*==' server/src/test/resources/fixtures
# C2 — 제품 코드에 Wikimedia 주소가 설정 밖에 박혀 있지 않은지. 출력 없음
grep -rnE 'wikipedia\.org|wikidata\.org|wikimedia\.org' server/src/main/java
# S1 — 제품 코드·설정 줄 수 (테스트 제외)
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 코드·fixture 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
```

담당별: backend-dev 2 (T1, T2), test-engineer 1 (T3). PM 작업 없음(엔드포인트가 없다).

## 리스크와 확인할 것

1. **HTML 태그 줄이 생각보다 많을 수 있다** — 건너뛰는 줄이 늘면 사건이 줄어든다. T1 (5) 전·후 표로 규모를 본다. 많으면 `<br>` 처럼 뜻이 분명한 태그만 공백으로 바꾸는 규칙을 후속으로 제안한다(내용 변경 여부는 PM이 정함).
2. **동시 요청 제한이 지연을 늘릴 수 있다** — M3a2·M3a3이 Wikidata·Commons를 함께 부르면 줄을 서게 된다. 날짜당 시간 상한(M3a1c)과 M6b 측정으로 본다.
3. **캐시가 오래된 판을 보여 준다** — TTL 7일 동안은 위키백과 수정이 늦게 반영된다. 판 번호를 응답에 남기므로(M3a2 `source_revision`) 추적할 수 있다.
4. **M3a1 오케스트레이터 수정과 겹침** — 503 + `Retry-After` 차단이 M3a1에 들어간다. T1의 호스트 차단 입구는 그 코드를 재사용한다.
5. **Testcontainers와 Ryuk** — M1a T2의 결정(PM)을 그대로 따른다.

## PM이 정할 것

없음. (M3a1 T3을 M3a1b·M3a1c 둘로 나눈 것은 PM이 2026-10-11에 승인했다.)
