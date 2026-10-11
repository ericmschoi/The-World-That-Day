# 위키백과 사건 출처: 외부 호출 공통, 어댑터, 파서, 캐시 (M3a1)

> 브랜치: `feat/wiki-events-source` · 작성: planner · 상태: PM 승인 (2026-10-11)
> 기준: `CLAUDE.md` 7절, `docs/product-brief.md` 5절 4번·6절·8절·12절, `docs/roadmap.md` M3, `docs/research/day-events.md` (T5), `docs/research/summary.md` 2.1절·6절·8절 5~7항
> M3 묶음: `chore/m1-followups` → **M3a1** → M3a2 `feat/world-events-api` → M3a3 `feat/event-media`, 웹은 M3b1 `feat/design-foundation`(M3a와 병렬) → M3b2 `feat/world-events-ui`. 나눈 이유와 순서는 로드맵 M3 절.

## 목표

날짜 하나를 주면 한국어 위키백과 날짜 문서("9월 17일")와 연도 문서("1988년")의 "사건"·"문화" 절에서 **연·월·일이 같은 사건 줄**을 골라 평문으로 돌려주는 서버 모듈을 만든다. 이 기능은 엔드포인트를 만들지 않는다. M3a2가 이 모듈을 써서 첫 도메인 API를 만든다.

끝나면 다음이 된다.

1. 모든 Wikimedia 호출이 공통 설정을 거친다: User-Agent 헤더, 연결·읽기 타임아웃, 조회(GET)만 1회 재시도, 429와 `Retry-After` 존중, 엔드포인트 주소는 설정값. 이후 Wikidata(M3a2)·Commons(M3a3)가 같은 설정을 쓴다.
2. `KoWikiEventSource.find(LocalDate)` 가 그날의 사건 목록, 문서별 판 번호(`revid`), 문서 상태(새로 받음·캐시·오래된 캐시·없음·실패)를 돌려준다.
3. 읽은 절(사건·문화)의 위키텍스트를 DB에 캐시하고(Flyway V2), 갱신에 실패하면 오래된 캐시를 쓴다.
4. 테스트는 녹화한 Wikimedia fixture와 WireMock만 쓰고 외부 네트워크를 부르지 않는다.

## 범위 밖

- HTTP 엔드포인트, 날짜 파라미터 검증, 에러 응답 형식, `tone`, 사건 범위(`scope`), Wikidata → M3a2
- 미디어(Commons), 공용 미디어 형식 → M3a3
- On this day 어댑터 (#20 (b)일 때만, 별도 기능)
- 날짜 문서 366개·연도 문서 미리 받기(워밍업) → 필요하면 M6b "다음 단계 제안"
- 탄생·사망·기념일 절 — **요청하지도, 저장하지도 않는다**(실제 인물의 생년월일이 들어 있다)
- 위키텍스트 템플릿(`{{...}}`) 전개 — 템플릿이 든 줄은 건너뛰고 센다(리스크 2)

## 사용할 외부 출처

| 출처 | 확신도 | 근거 | 지킬 조건 (리서치 문서에 적힌 대로) |
|---|---|---|---|
| 한국어 위키백과 날짜 문서·연도 문서 (MediaWiki Action API `action=parse`) | **확인됨** | [day-events.md 3절 카드](../research/day-events.md#3-출처-카드), [summary 2.1절](../research/summary.md#21-확인됨--구현에-쓸-수-있다) | 텍스트 CC BY-SA 4.0 — 판 번호를 남기고(이 기능), 화면에서 문서 링크와 라이선스 고지(M3a2·M3b2). 연락처가 든 User-Agent 필수(없으면 분당 10회, 차단 위험). 규칙에 맞으면 분당 200회, 429와 `Retry-After` 처리, 직렬 요청 권장. "사건"·"문화" 절만 읽는다. 캐시 허용(라이선스가 복제 허용, API 예절이 캐시를 권장) |

키는 필요 없다. `WIKIMEDIA_USER_AGENT` 는 M1a에서 `.env.example` 에 이름을 넣었고 값은 PM이 정한다(#25 (a) 저장소 URL).

## 전제 (PM 결정과 가정)

| 항목 | 내용 | 상태 |
|---|---|---|
| 사건 출처 조합 | #20 (a) 한국어 위키백과 + Wikidata + Commons, On this day 없음 | **추천안 전제** (PM 미결정, 로드맵 M3 결정 표) |
| 위키백과 문장 | #23 (a) 원문 그대로, 링크 기호·굵은 글씨 기호·각주만 제거, 번역·요약 안 함 | **추천안 전제** |
| 테스트 fixture | #12 (a): Wikimedia 응답은 녹화 fixture 가능, 녹화 일자·원 URL·라이선스 기록 | 결정 |
| User-Agent 연락처 | #25 (a) 저장소 URL | 결정 |
| 캐시 저장 위치 | M3-P3 (a) DB 테이블(Flyway V2) | **추천안 전제** |

PM이 다르게 정하면 바뀌는 곳: #20 (c)(Wikidata만)이면 이 기능의 파서는 필요 없고 계획을 다시 쓴다. #23 (b)는 이 기능에 영향이 없다(고쳐 쓰는 곳이 없다). M3-P3 (b)(메모리)이면 T3의 V2 마이그레이션과 엔티티가 빠지고 메모리 맵 + TTL로 바뀐다(약 60줄 감소). (c)(캐시 없음)이면 T3의 캐시 부분이 빠진다.

## 설계

### 패키지 (제안 — backend-dev가 기존 관례에 맞춰 이름을 바꿔도 되지만 책임 경계는 지킨다)

| 패키지 | 들어가는 것 |
|---|---|
| `io.github.ericmschoi.theworldthatday.wikimedia` | `WikimediaProperties`(`@ConfigurationProperties("wikimedia")`), Wikimedia용 `RestClient` 만들기(User-Agent, 타임아웃, 재시도), 호스트별 429 차단기. Wikidata·Commons 어댑터도 여기서 클라이언트를 받는다 |
| `...event.kowiki` | `KoWikiClient`(인터페이스) ↔ `ActionApiKoWikiClient`(Action API 구현), `WikitextEventParser`, 캐시 엔티티·저장소, `KoWikiEventSource` 구현 |

### 외부 호출 공통 규칙

| 규칙 | 값 | 비고 |
|---|---|---|
| User-Agent | `wikimedia.user-agent` (= `${WIKIMEDIA_USER_AGENT:}`) 를 모든 요청 `User-Agent` 헤더에 | 비어 있으면 **Wikimedia 출처 전체를 끈다**: 요청을 보내지 않고 바로 "사용 불가"로 돌려준다. 시작할 때 WARN 한 번. 값 자체는 로그에 남기지 않는다 |
| 연결 타임아웃 | `wikimedia.connect-timeout: 2s` | |
| 읽기 타임아웃 | `wikimedia.read-timeout: 4s` | M3a2가 Wikidata용 값을 따로 둘 수 있다 |
| 재시도 | 조회(GET)만, `wikimedia.max-retries: 1` 회. 연결·읽기 타임아웃, 502·503·504일 때만 | 4xx와 429는 재시도하지 않는다 |
| 429 | 응답의 `Retry-After`(초) 동안 **그 호스트로 요청을 보내지 않는다**. 헤더가 없으면 30초, 최대 `wikimedia.rate-limit-max-wait: 120s` | 요청 안에서 잠들지 않는다(10초 목표, 제품 정의서 8절). 차단 중이면 바로 "사용 불가" |
| 엔드포인트 | `wikimedia.kowiki-api-url: https://ko.wikipedia.org/w/api.php` | Wikidata·Commons 주소는 M3a2·M3a3이 같은 접두사 아래에 추가 |
| 시계 | `Clock` 빈 (기본 `Asia/Seoul`) | 429 차단 만료·캐시 TTL 테스트용. M3a2가 날짜 검증에 같은 빈을 쓴다 |
| 로그 | 실패는 WARN 한 줄: 출처 이름, 문서 제목, 상태 코드 또는 예외 종류 | 요청 URL 전체·응답 본문을 남기지 않는다. 문서 제목("9월 17일")만으로는 사람과 연결되지 않지만, M2 이후 장소와 같은 줄에 남기지 않는다(`CLAUDE.md` 7절 6항) |

Spring Boot 4.1의 HTTP 클라이언트 설정 클래스·프로퍼티 이름(예: `RestClient` 빌더, 요청 팩토리 설정, `spring.http.client.*`)은 Boot 3 자료와 다를 수 있다. backend-dev가 4.1 공식 문서로 확인하고 고른 방식을 보고서에 적는다.

### 위키백과 호출 (문서 하나당)

| 순서 | 요청 | 얻는 것 |
|---|---|---|
| 1 | `GET {kowiki-api-url}?action=parse&page=<제목>&prop=sections\|revid&redirects=1&format=json&formatversion=2` | `revid`, 절 목록(`index`, `line`, `level`) |
| 2 | `GET {kowiki-api-url}?action=parse&oldid=<1의 revid>&prop=wikitext&section=<"사건" 절 index>&format=json&formatversion=2` | 사건 절 위키텍스트 |
| 3 | 같은 요청, `section=<"문화" 절 index>` | 문화 절 위키텍스트 |

- 제목: 날짜 문서 `"{M}월 {D}일"`(앞자리 0 없음, 예 `9월 17일`, `2월 29일`), 연도 문서 `"{YYYY}년"`(예 `1988년`).
- 절은 `level` 이 2이고 `line` 이 정확히 `사건` 또는 `문화` 인 것만 고른다. 없는 절은 요청하지 않는다(그 절은 "없음"). 다른 절 번호는 **요청하지 않는다**.
- 2·3은 1에서 받은 `revid` 로 고정(`oldid`)해 같은 판을 읽는다.
- 문서가 없으면(응답 `error.code` = `missingtitle`) 상태 `ABSENT` — 실패가 아니다. 재시도하지 않고 캐시하지 않는다.
- 요청은 직렬로 보낸다(리서치 권장). 캐시가 없을 때 날짜 하나에 최대 6번(문서 2개 × 3번).

### 파서 규칙 (`WikitextEventParser`)

입력: 절 위키텍스트, 문서 종류(날짜 문서·연도 문서), 찾는 날짜. 출력: 사건 줄 목록 + 건너뛴 줄 수.

| 문서 | 줄 형식 | 고르는 법 |
|---|---|---|
| 날짜 문서 | `* [[1988년]] - 본문`, 연도 링크에 별칭이 있어도 됨(`[[1988년\|1988]]`) | 맨 앞 연도 링크의 연도 = 찾는 연도 |
| 날짜 문서 | `* [[1988년]]` 다음 줄들 `** 본문` | 하위 줄은 위 연도를 물려받고 각각 사건 하나 |
| 연도 문서 | `* [[9월 17일]] - 본문` 또는 링크 없는 `* 9월 17일 - 본문` | 맨 앞 날짜 = 찾는 월·일 |
| 연도 문서 | `* [[9월 17일]]` 다음 줄들 `** 본문` | 하위 줄은 위 날짜를 물려받는다 |
| 둘 다 | 윗줄에 본문이 있고 하위 줄도 있으면 | 윗줄과 하위 줄을 각각 사건으로 |

- 구분자: ` - `(하이픈) 또는 ` – `(엔 대시). 다른 형식, 날짜 범위(`9월 17일 ~ 10월 2일`), 기원전 연도는 건너뛰고 센다.
- 평문 바꾸기 (#23 (a)의 "링크 기호 제거" 범위로 본다 — PM 확인): `[[대상|별칭]]` → 별칭, `[[대상]]` → 대상, `'''굵게'''`·`''기울임''` → 글자만, `<ref>…</ref>`·`<ref …/>`·`<!-- -->` 삭제, `&nbsp;` → 공백, 공백 정리.
- `[[파일:…]]`, `[[File:…]]`, `[[분류:…]]` 링크는 통째로 지운다.
- **템플릿(`{{`)이나 외부 링크(`[http`)가 든 줄은 건너뛰고 센다** (내용을 바꾸지 않기 위해). 건너뛴 줄 수를 결과에 담는다.
- 사건 링크: 구분자 뒤 본문에 나온 위키 링크의 대상 제목(밑줄 → 공백, `#절` 제거)을 순서대로. 맨 앞 연도·날짜 링크는 넣지 않는다. 첫 번째를 "대표 링크"라 부른다(M3a2의 QID·큐레이션 키).
- 결과 줄의 `summary_modified` 는 항상 `false` 다(위 바꾸기 외에는 손대지 않는다).

### 합치기와 중복 제거

1. 날짜 문서(사건 → 문화) 다음에 연도 문서(사건 → 문화) 순서.
2. **문서 사이에서만** 중복을 없앤다: 대표 링크가 같고, 그 대표 링크가 각 문서의 그날 사건 중 한 번씩만 나오면 같은 사건으로 보고 날짜 문서 쪽을 남긴다.
3. 같은 문서 안에서는 합치지 않는다(하위 줄이 같은 문서를 가리켜도 다른 사건일 수 있다).
4. 사건 ID: `kowiki-{yyyy-MM-dd}-{SHA-256(문서 제목 + "\n" + 평문) 앞 8자리 16진수}`. 같은 판이면 매번 같다.

### 캐시 (Flyway V2, M3-P3 (a) 전제)

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

- TTL `wikimedia.cache-ttl: 7d`. 신선하면 외부 호출 없이 쓴다(상태 `CACHED`).
- 오래됐으면 다시 받는다. 성공하면 갱신(`FRESH`), 실패하면 오래된 캐시를 쓴다(`STALE`). 캐시도 없으면 `UNAVAILABLE`.
- 저장은 같은 제목이면 갱신이고, 같은 제목을 두 번 저장해도 요청이 실패하지 않는다(유일 제약 충돌 처리).
- 테이블에는 요청한 사람에 관한 값(요청 시각 외의 메타데이터, IP 등)을 넣지 않는다. 탄생·사망 절은 받지 않으므로 저장될 수 없다.
- 엔티티는 API 응답으로 내보내지 않는다. `ddl-auto=validate` 가 통과해야 한다.

### M3a2에 넘기는 인터페이스 (계약)

```java
public interface KoWikiEventSource {
  KoWikiDayResult find(LocalDate date);
}

public record KoWikiDayResult(
    LocalDate date,
    List<KoWikiEvent> events,        // 위 순서, 중복 제거 후
    List<KoWikiDocStatus> docs,      // 날짜 문서, 연도 문서 순서로 2개
    int skippedLines) {
  public boolean allUnavailable() { ... } // 두 문서 모두 UNAVAILABLE
  public boolean degraded() { ... }       // 하나라도 UNAVAILABLE 또는 STALE
}

public record KoWikiEvent(
    String id,                // kowiki-1988-09-17-1a2b3c4d
    LocalDate date,
    String sourceTitle,       // "9월 17일" 또는 "1988년"
    String sourceSection,     // "사건" 또는 "문화"
    long revision,
    String text,              // 평문 (원문 그대로, 링크 기호 등만 제거)
    List<String> linkTitles,  // 첫 번째가 대표 링크. 없으면 빈 목록
    Instant retrievedAt) {}   // 캐시 행의 fetched_at

public record KoWikiDocStatus(String title, Long revision, Instant retrievedAt, DocState state) {}
public enum DocState { FRESH, CACHED, STALE, ABSENT, UNAVAILABLE }
```

이름은 바꿔도 되지만 필드의 뜻과 `allUnavailable`·`degraded` 의 정의는 지킨다. 바꾸면 보고서에 적는다.

### 설정 (`application.yml` 추가분)

```yaml
wikimedia:
  user-agent: ${WIKIMEDIA_USER_AGENT:}
  connect-timeout: 2s
  read-timeout: 4s
  max-retries: 1
  rate-limit-max-wait: 120s
  cache-ttl: 7d
  kowiki-api-url: https://ko.wikipedia.org/w/api.php
```

## 테스트 방침 (M3 서버 작업 공통 — M3a2·M3a3도 따른다)

1. 테스트는 외부 네트워크를 부르지 않는다. 테스트 설정은 모든 `wikimedia.*-url` 을 WireMock(`http://localhost:<port>`)으로 바꾼다. 테스트 하나가 테스트 컨텍스트의 `wikimedia.kowiki-api-url` 이 `http://localhost` 로 시작하는지 확인한다.
2. WireMock 의존성을 테스트에 추가한다(Spring Boot 4.1·Jetty와 충돌하지 않는 판 — 예: 셰이딩된 standalone 아티팩트). 고른 판을 보고서에 적는다.
3. **녹화 fixture** (`server/src/test/resources/fixtures/wikimedia/kowiki/`): 데모 날짜에 필요한 문서 6개(`9월 17일`, `6월 22일`, `3월 15일`, `1988년`, `2002년`, `1995년`)의 절 목록 응답과 사건·문화 절 위키텍스트 응답. 같은 폴더 `README.md` 에 파일마다 원 URL, 녹화 일자, `revid`, 라이선스(CC BY-SA 4.0, 위키백과 텍스트)를 적는다. User-Agent 값은 적지 않는다.
   - 녹화는 backend-dev가 `curl` 로 한다. User-Agent는 `.env` 를 읽지 않고 공개값 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)` 를 명령에 직접 쓴다(#25 (a), 비밀값 아님). 권한 문제로 네트워크를 쓸 수 없으면 "막힘"으로 보고하고 PM이 실행할 `curl` 명령을 적는다.
   - 탄생·사망 절은 요청하지 않으므로 fixture에 없다. C1(아래)로 확인한다.
4. **합성 fixture** (`fixtures/wikimedia/kowiki/synthetic/`, 파일 이름 `synthetic-` 로 시작): 파서 경계 사례, 오류 응답(`missingtitle`, 503, 429), 윤일 경계값 `2000-02-29`(`2월 29일`, `2000년` 문서). 내용은 "시험 사건 가" 같은 중립 문장만 쓴다. 실제 사건·인물을 지어내지 않는다.
5. 시간은 `Clock` 을 바꿔서 시험한다(`Thread.sleep` 금지).
6. 테스트 날짜는 데모 입력 3개와 경계값(윤일, 오늘·내일)만 쓴다.

## 데모 입력 3개로 확인할 것

| 데모 입력 | 이 기능에서 확인 (녹화 fixture 기준) |
|---|---|
| 1988-09-17 + "서울" | 결과에 본문이 "서울 하계 올림픽"을 포함하는 사건이 **정확히 1개**(날짜 문서·연도 문서 중복 제거, 남은 쪽 `sourceTitle` = `9월 17일`), 그 사건의 `linkTitles` 에 `1988년 하계 올림픽`. 모든 사건의 `date` 가 1988-09-17. 두 문서의 `revision` 이 fixture `README.md` 의 값과 같다 |
| 2002-06-22 + "광주 서구 풍암동" | `sourceSection` = `문화` 이고 본문에 "스페인"이 든 사건이 1개 이상 |
| 1995-03-15 + "서울대학교병원" | 사건 0개, 두 문서 상태가 `FRESH`(실패가 아니라 비어 있음) — M3a2가 #21 안내로 바꾼다 |

## 크기 (400줄 규칙)

세는 법: 제품 코드 + 설정(`build.gradle.kts`, `application.yml`, 마이그레이션)은 센다. 테스트 코드와 fixture(`server/src/test/**`)는 따로 보고한다.

예상: 외부 호출 공통 약 90, 위키백과 클라이언트 약 80, 파서 약 130, 캐시(마이그레이션·엔티티·저장소·흐름) 약 90, 합치기·인터페이스 약 40, 설정 약 15 → **약 400~450줄**. 한도에 걸친다.
- **T2가 끝났을 때 누적 300줄을 넘으면** backend-dev가 보고서에 적고, 오케스트레이터는 T3(캐시)를 M3a2 앞의 별도 기능(`feat/wiki-section-cache`)으로 뺄지 PM에게 묻는다. 그때 T3 없이도 `KoWikiEventSource` 는 매번 외부를 부르는 형태로 동작해야 한다.

확인 명령은 아래 S1·S2.

## 작업 목록

순서: T1 → T2 → T3 → T4.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | Wikimedia 외부 호출 공통 설정 + WireMock 테스트 기반 | backend-dev | `wikimedia` 패키지(프로퍼티, `RestClient` 구성, 재시도, 429 차단기), `application.yml` 의 `wikimedia.*`(위 설정 중 `cache-ttl` 은 T3), `Clock` 빈, WireMock 테스트 의존성, 테스트 | - | (1) `cd server && ./gradlew check` 성공. (2) WireMock 테스트: 모든 요청의 `User-Agent` 헤더가 설정값과 같다. (3) `wikimedia.user-agent` 가 빈 값이면 WireMock 요청 수 0이고 호출 결과가 "사용 불가"이며, 시작 로그에 WARN 1줄(값은 출력하지 않음). (4) 읽기 타임아웃(테스트 값 300ms)보다 늦는 응답 → 2초 안에 "사용 불가", 요청 수 2(첫 요청 + 재시도 1). (5) 503 다음 200 → 성공, 요청 수 2. 404 → 재시도 없음, 요청 수 1. (6) 429 + `Retry-After: 60` → 시계를 59초 옮긴 뒤 호출하면 요청 수가 늘지 않고 "사용 불가", 61초 옮기면 요청이 나간다. `Retry-After: 999` 는 120초로 자른다. 헤더가 없으면 30초. (7) 테스트 컨텍스트의 `wikimedia.kowiki-api-url` 이 `http://localhost` 로 시작한다는 테스트. (8) 보고서에 고른 WireMock 판과 Boot 4.1 HTTP 클라이언트 구성 방법(클래스·프로퍼티 이름)을 적는다 |
| T2 | 위키백과 클라이언트 + 위키텍스트 파서 + 녹화 fixture | backend-dev | `KoWikiClient`·`ActionApiKoWikiClient`, `WikitextEventParser`, `fixtures/wikimedia/kowiki/**`(+ `README.md`), 테스트 | T1 | (1) C1 출력 없음. `README.md` 에 녹화 fixture마다 원 URL·녹화 일자·`revid`·라이선스가 있다. (2) 파서 + 녹화 fixture: `9월 17일` 사건 절에서 1988년 → 본문에 "서울 하계 올림픽"이 든 사건 1개, 대표 링크 `1988년 하계 올림픽`. `3월 15일` 에서 1995년 → 0개. `6월 22일` 문화 절에서 2002년 → "스페인"이 든 사건 1개 이상. `1988년` 문서에서 9월 17일 → 1개 이상, `1995년` 문서에서 3월 15일 → 0개. (3) 파서 + 합성 fixture: 별칭 링크, 굵은 글씨, `<ref>` 제거, `[[파일:…]]` 제거, 하위 줄(`**`)이 연도·날짜를 물려받음, 엔 대시 구분자, 템플릿 줄과 외부 링크 줄은 건너뛰고 `skippedLines` 증가, 날짜 범위 줄 건너뜀 — 사례마다 테스트 1개 이상. (4) 클라이언트 + WireMock: 문서마다 요청이 정확히 "절 목록 1 + 있는 절(사건·문화) 수"이고, WireMock이 받은 `section` 값이 사건·문화 절 번호뿐이다(다른 번호 요청 0). 위키텍스트 요청의 `oldid` 가 절 목록 응답의 `revid` 와 같다. 요청 제목이 `9월 17일`, `1988년`(합성 경계값: `2월 29일`, `2000년`). (5) `missingtitle` 응답 → 상태 `ABSENT`, 재시도 없음(요청 수 1) |
| T3 | 위키텍스트 캐시(V2) + 합치기·중복 제거 + `KoWikiEventSource` | backend-dev | `V2__wiki_section_cache.sql`, 엔티티·저장소, `KoWikiEventSource` 구현, `wikimedia.cache-ttl`, 테스트(Testcontainers) | T2 | (1) `flyway_schema_history` 에 `version = '2'`, `success = true`, `ddl-auto=validate` 로 컨텍스트가 뜬다. (2) 1988-09-17 첫 호출은 WireMock에 요청이 가고, TTL 안의 두 번째 호출은 요청 수가 늘지 않는다(문서 상태 `CACHED`). (3) 시계를 TTL 너머로 옮기고 WireMock이 503 → 이전 사건을 그대로 돌려주고 상태 `STALE`, `degraded()` = true. (4) 캐시 없음 + 두 문서 모두 실패 → 사건 0개, `allUnavailable()` = true. 한 문서만 실패 → 다른 문서의 사건을 돌려주고 그 문서 상태 `UNAVAILABLE`. (5) 저장된 `events_wikitext`·`culture_wikitext` 가 fixture의 사건·문화 절 본문과 같고, 테이블에 다른 절 내용이 없다. (6) 데모: 1988-09-17 결과에 "서울 하계 올림픽"이 든 사건이 정확히 1개이고 `sourceTitle` = `9월 17일`. 합성 사례: 같은 문서 안에서 대표 링크가 같은 두 줄은 둘 다 남는다. (7) 사건 ID가 `^kowiki-\d{4}-\d{2}-\d{2}-[0-9a-f]{8}$` 이고 두 번 호출해도 같다. (8) 같은 제목을 두 번 저장해도 예외가 없고 행은 1개 |
| T4 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트(테스트 파일만) | T3 | (1) T1~T3 기준을 표로 확인하고 빠진 테스트를 추가. (2) `cd server && ./gradlew check` 통과(개수 보고), 같은 명령 3회 반복해 불안정한 테스트가 없음. (3) C1, C2 출력 없음. (4) S1·S2 결과 보고. (5) 데모 3개 날짜의 `KoWikiEventSource` 결과(사건 수, 본문 앞 30자, 출처 문서, 절, 건너뛴 줄 수)를 표로 보고 — M3a2의 큐레이션(content-curator)이 이 표를 쓴다 |

완료 기준에 쓰는 검사 명령 (저장소 루트):

```
# C1 — fixture에 탄생·사망 절이 없는지. 출력이 없어야 한다
grep -rlnE '==[[:space:]]*(탄생|사망)[[:space:]]*==' server/src/test/resources/fixtures

# C2 — 제품 코드에 Wikimedia 주소가 설정 밖에 박혀 있지 않은지. 출력이 없어야 한다
grep -rnE 'wikipedia\.org|wikidata\.org|wikimedia\.org' server/src/main/java

# S1 — 제품 코드·설정 줄 수 (테스트 제외)
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'

# S2 — 테스트 코드·fixture 줄 수 (따로 보고)
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
```

담당별: backend-dev 3 (T1~T3), test-engineer 1 (T4). PM 작업 없음(엔드포인트가 없어 직접 확인할 화면·명령이 없다. 결과는 T4 표로 본다).

## 리스크와 확인할 것

1. **크기** — 한도에 걸친다. "크기" 절의 기준(T2 후 300줄)으로 T3를 뺄지 정한다.
2. **템플릿이 든 줄을 건너뛴다** — 날짜 문서 줄 중 템플릿(국기 등)을 쓰는 줄은 화면에 나오지 않는다. 내용을 바꾸지 않기 위한 선택이다. T4 표의 "건너뛴 줄 수"로 규모를 보고, 많으면 M3a2 이후 "안전한 템플릿 목록" 처리를 다음 단계로 제안한다.
3. **위키텍스트 형식이 문서마다 다르다** — 사람이 편집하는 자유 형식이다(리서치 8절 9항). 형식이 맞지 않는 줄은 건너뛰고 세며, 판 번호를 남겨 어느 판을 읽었는지 추적한다.
4. **Spring Boot 4.1 HTTP 클라이언트 API** — Boot 3 예제와 클래스·프로퍼티 이름이 다를 수 있다. T1 보고서에 적는다.
5. **녹화에 네트워크 권한이 필요하다** — 에이전트 환경에서 `curl` 이 막히면 T2가 "막힘"이 된다. 그때 PM이 실행할 명령을 보고서에 적는다.
6. **`.env` 의 User-Agent 값에 공백·괄호가 있다** — 루트 `.env` 는 Compose와 Spring이 함께 읽는다(M1a 리스크 5). Spring `.properties` 형식은 `=` 뒤 공백을 값으로 읽는다. Compose가 이 줄을 문제없이 넘기는지 M3a2 PM 확인(T5)에서 `docker compose config --quiet` 로 함께 본다.

## PM이 정할 것

이 기능에 걸리는 결정만 적었다. 전체 표와 추천 근거는 로드맵 M3 절 "M3 PM 결정".

| # | 결정 | 추천 (이 계획서의 전제) | 다르게 정하면 |
|---|---|---|---|
| #20 | 사건 1순위 조합 | (a) 위키백과 + Wikidata + Commons | (c)면 파서가 필요 없어 계획을 다시 쓴다. (b)는 이 기능에 영향 없음 |
| #23 | 위키백과 문장 동일조건 범위 | (a) 원문 그대로. 굵은 글씨 기호·각주 제거도 "링크 기호 제거"와 같은 범위로 본다 | (b)는 이 기능에 영향 없음 |
| M3-P1 | M3 분할 | 로드맵 M3 절 (a) | 로드맵 참고 |
| M3-P3 | 캐시 저장 | (a) DB 테이블(Flyway V2) | (b) 메모리: T3 단순화. (c) 없음: T3 캐시 제거 |
