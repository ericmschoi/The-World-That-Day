# 위키백과 사건 합치기·중복 제거와 `KoWikiEventSource` (M3a1c)

> 브랜치: `feat/wiki-events-merge` · 작성: planner · 상태: **PM 승인 (2026-10-11)** (2026-10-11 "M3a1 분할"로 옮긴 원래 M3a1 T3 중 합치기 부분. M3a1 검증(T4, 8feaf87)과 M3a1 코드 리뷰(승인) 입력으로 중복 제거·시간 상한을 강화했다. 나눈 이유는 `docs/plans/m3a1b-wiki-section-cache.md` 첫 절)
> 기준: `docs/plans/m3a1-wiki-events-source.md`(설계·테스트 방침), `CLAUDE.md` 7절, `docs/product-brief.md` 5절 4번·6절·8절(첫 화면 3초, 느린 섹션은 나중에 채움, 전체 10초)·12절, `docs/research/day-events.md` 3절·8절, #23 (a)
> 선행: M3a1 머지(`KoWikiClient`, `KoWikiPageKind`, `WikitextEventParser`, 외부 호출 공통). M3a1b `feat/wiki-section-cache` 와는 `KoWikiClient` 인터페이스로만 이어진다 — **순서는 M3a1b 다음**을 기본으로 하되, 오케스트레이터가 worktree로 병렬 진행해도 된다(겹치는 파일: `application.yml`, `DocState`). 뒤따르는 기능: M3a2 `feat/world-events-api`.

## M3a1 검증·리뷰에서 받은 입력

| # | 발견 | 대응 |
|---|---|---|
| 검증 1 | 같은 사건이 3~4번 나온다. 1988-09-17 올림픽 개막이 날짜 문서 "사건"(대표 링크 `1988년 하계 올림픽`), 날짜 문서 "문화"(같은 링크), 연도 문서 "사건"(같은 링크), 연도 문서 "문화" 첫 하위 줄(대표 링크 `대한민국`)에 있다. 원래 규칙("각 문서에 한 번씩만, 문서 사이에서만")으로는 합쳐지지 않는다 | 절·문서를 가리지 않는 **새 중복 제거 규칙** D1~D6. 기준: 1988-09-17 개막 **정확히 1번** |
| 검증 2 | 2002-06-22 8강전이 두 번 나온다. 링크가 `2002 FIFA 월드컵` / `2002년 FIFA 월드컵` 으로 다르다 | **링크 정규화**: 넘겨주기 해석 + Wikidata QID + 글자 정규화(`2002년 X` ≡ `2002 X`). 기준: 2002-06-22 경기 **정확히 1번** |
| 검증 3 | 파서는 문서와 날짜가 맞는지 보지 않는다. 호출하는 쪽(`KoWikiPageKind.title(date)`)이 맞는 문서를 골라야 한다 | **불변 조건**과 테스트(T1 (3)) |
| 검증 4 | 국기 템플릿으로 빠진 줄은 녹화 12개 절에서 1줄 | 후속 작업 후보 `chore/kowiki-safe-templates`(이 기능 밖) |
| 리뷰 2 | 연결 2초·읽기 4초·재시도 1회면 멈춘 호스트 하나에 문서당 약 8초, 직렬 두 문서면 날짜당 약 16초 | **날짜당 전체 시간 상한** + 날짜 문서가 시간 초과면 연도 문서를 건너뜀 |

## 목표

1. `KoWikiEventSource.find(LocalDate)` 가 날짜 문서·연도 문서의 그날 사건을 합치고 **같은 사건은 한 번만** 남겨, 사건 목록, 문서별 판 번호, 문서 상태를 돌려준다. M3a2가 이 계약을 그대로 쓴다.
2. 사건의 링크를 정규 제목(넘겨주기 해석)과 Wikidata QID로 바꿔 둔다. 중복 제거에 쓰고, M3a2가 QID를 다시 묻지 않게 넘긴다(원래 M3a2의 pageprops 조회를 이 기능으로 옮김).
3. 날짜 하나를 처리하는 시간이 상한(`wikimedia.kowiki-day-deadline`)을 넘지 않는다.

## 범위 밖

- 캐시, `ratelimited`, 동시 요청 제한, HTML 태그·엔티티 → M3a1b
- HTTP 엔드포인트, 날짜 검증, 에러 형식, `tone`, `scope`, Wikidata SPARQL, 화면에 내보낼 사건 고르기(품질 필터, M3-P16 (b) 결정) → M3a2
- 문장 뜻을 비교하는 중복 판단(형태소 분석, 임베딩 등)
- 링크 해석 결과의 DB 캐시 (필요하면 M6b "다음 단계 제안")
- 탄생·사망·기념일 절

## 사용할 외부 출처

| 출처 | 확신도 | 근거 | 지킬 조건 |
|---|---|---|---|
| 한국어 위키백과 날짜·연도 문서 (`KoWikiClient` 경유, M3a1b 캐시가 있으면 그 뒤) | **확인됨** | [day-events.md 3절](../research/day-events.md#3-출처-카드) | CC BY-SA 4.0 — 판 번호를 남긴다. M3a1 공통 설정 |
| 한국어 위키백과 Action API `action=query&prop=pageprops&ppprop=wikibase_item&redirects=1` | **확인됨** (같은 출처·같은 API. 원래 M3a2 계획에 있던 조회) | [day-events.md 3절](../research/day-events.md#3-출처-카드) | 제목을 `\|` 로 묶어 한 번에(최대 50개, 넘으면 나눠서). 직렬. 실패하면 링크 해석 없이 진행 |

키는 필요 없다.

## 설계

### 불변 조건 — 맞는 문서만 파서에 넣는다 (검증 3)

`WikitextEventParser` 는 날짜 문서에서는 연도만, 연도 문서에서는 월·일만 맞춰 본다. 그래서 **`KoWikiEventSource` 는 늘 `KoWikiPageKind.title(date)` 로 고른 두 문서만** 파서에 넘기고, 파서에 넘기는 "찾는 날짜"는 요청 날짜 그대로다. 다른 경로로 문서 제목을 만들지 않는다.

### 날짜당 시간 상한 (리뷰 2)

| 규칙 | 내용 |
|---|---|
| 상한 | `wikimedia.kowiki-day-deadline: 5s` — 문서 두 개와 pageprops 요청 전체. 남은 시간을 각 호출의 상한으로 넘긴다(읽기 타임아웃보다 남은 시간이 짧으면 남은 시간) |
| 날짜 문서가 시간 초과 | 같은 호스트가 멈춘 것으로 보고 **연도 문서를 요청하지 않는다**(캐시가 있으면 캐시는 쓴다). 날짜 문서 상태 `UNAVAILABLE`, 연도 문서는 캐시가 있으면 `STALE`, 없으면 `UNAVAILABLE` |
| 상한 도달 | 그때까지 받은 문서로 결과를 만든다. 남은 문서는 `UNAVAILABLE`(캐시가 있으면 `STALE`). pageprops를 못 하면 `linkResolutionFailed` |
| 근거 | 제품 정의서 8절: 느린 섹션은 나중에 채우고 전체 10초 목표. 세계 섹션의 나머지(Wikidata, Commons)가 쓸 시간을 남긴다. 값은 M6b 측정으로 보정 |

### 링크 정규화와 QID (검증 2)

1. 사건마다 링크 제목을 정규화한다: 밑줄 → 공백, `#절` 제거, 앞뒤·연속 공백 정리, 첫 글자가 라틴 문자면 대문자(MediaWiki 제목 규칙).
2. 그날 사건들의 모든 링크 제목을 모아 pageprops 요청으로 **넘겨주기·정규화 결과**와 **`wikibase_item`(QID)** 을 받는다. 사건의 `linkTitles` 는 해석된 정규 제목으로 바꾸고, 대표 링크의 QID를 `wikidataQid` 에 담는다. 사건이 없거나 링크가 없으면 요청하지 않는다.
3. 두 링크가 같은지: QID가 둘 다 있고 같거나, 해석된 제목이 같거나, 글자 열쇠(해석된 제목에서 맨 앞 `(\d{4})년 ` → `$1 `)가 같으면 같은 링크다.
4. 연도 문서·날짜 문서 링크(`^\d+년$`, `^\d+월 \d+일$`)는 비교 집합에서 뺀다.
5. pageprops 요청이 실패하면 1·3의 글자 정규화만으로 비교하고, 모든 `wikidataQid` 는 `null`, 결과 `linkResolutionFailed` = true(→ `degraded()` true → M3a2 `partial`).
6. 화면 문장(`text`)은 바꾸지 않는다(#23 (a)). 정규화는 비교와 링크 주소에만 쓴다.

### 중복 제거 (검증 1·2로 바꾼 규칙)

입력은 네 절(날짜 문서 사건 → 문화 → 연도 문서 사건 → 문화)의 그날 사건, 이 순서. "일반 링크"는 나라·도시처럼 여러 사건에 두루 붙는 링크로, 서버 리소스 파일 `kowiki-generic-links.txt`(처음 값: 녹화 fixture 12개 절에 나온 나라·도시 링크 — T2 보고서에 목록)와 4항의 연도·날짜 문서다.

| 규칙 | 내용 |
|---|---|
| D1 같은 절은 합치지 않는다 | 편집자가 한 절에 따로 적은 줄은 다른 사건으로 본다(같은 날 같은 대회의 여러 경기 등) |
| D2 글자 일치 | 공백·문장부호를 정리한 `text` 가 같으면 같은 사건 |
| D3 공유 대표 링크 + 겹침 | 두 사건 중 하나의 대표 링크 L이 다른 사건의 링크 집합에도 있고, L이 일반 링크가 아니며, 두 링크 집합의 겹침 계수(교집합 크기 ÷ 작은 집합 크기)가 `wikimedia.dedup.min-link-overlap`(처음 0.5) 이상이면 같은 사건. 예: 연도 문서 문화 하위 줄(대표 링크 `대한민국`)이 `1988년 하계 올림픽` 링크도 담고 있으면 날짜 문서 사건 줄(대표 링크 `1988년 하계 올림픽`)과 합쳐진다 |
| D4 일대일 | 뒤 절의 사건 하나가 앞 절의 서로 다른 사건 둘 이상과 맞으면 **합치지 않는다**(애매하면 사건을 잃지 않는 쪽) |
| D5 남는 쪽 | 묶음에서 입력 순서가 가장 앞인 것(날짜 문서 사건 > 문화 > 연도 문서 사건 > 문화)을 남긴다. 남은 사건의 문장·출처·판 번호는 바꾸지 않는다(문장을 섞지 않음) |
| D6 기록 | 뺀 사건 수를 `mergedCount` 에 담는다. DEBUG 로그에는 규칙 이름(D2/D3)과 사건 ID만(문장·날짜는 남기지 않음) |

- 사건 ID: `kowiki-{yyyy-MM-dd}-{SHA-256(문서 제목 + "\n" + 평문) 앞 8자리 16진수}`. 같은 판이면 매번 같다.
- `skippedLines` 는 두 문서의 건너뛴 줄 수의 합.
- 겹침 기준값은 설정값이다. T2에서 데모 기준과 "반례" 테스트가 함께 통과하는 값을 고르고 보고한다. 둘을 함께 만족하는 값이 없으면 "막힘"으로 보고한다(데모 날짜에 맞춘 특수 처리를 하지 않는다).

### M3a2에 넘기는 인터페이스 (계약)

```java
public interface KoWikiEventSource {
  KoWikiDayResult find(LocalDate date);
}

public record KoWikiDayResult(
    LocalDate date,
    List<KoWikiEvent> events,        // 입력 순서, 중복 제거 후
    List<KoWikiDocStatus> docs,      // 날짜 문서, 연도 문서 순서로 2개
    int skippedLines,
    int mergedCount,                 // 중복으로 뺀 사건 수
    boolean linkResolutionFailed) {  // pageprops 요청 실패·시간 상한
  public boolean allUnavailable() { ... } // 두 문서 모두 UNAVAILABLE
  public boolean degraded() { ... }       // 하나라도 UNAVAILABLE 또는 STALE, 또는 linkResolutionFailed
}

public record KoWikiEvent(
    String id,                // kowiki-1988-09-17-1a2b3c4d
    LocalDate date,
    String sourceTitle,       // "9월 17일" 또는 "1988년"
    String sourceSection,     // "사건" 또는 "문화"
    long revision,
    String text,              // 평문 (원문 그대로, 링크 기호 등만 제거)
    List<String> linkTitles,  // 해석된 정규 제목. 첫 번째가 대표 링크. 없으면 빈 목록
    String wikidataQid,       // 대표 링크의 QID. 없거나 해석 실패면 null
    Instant retrievedAt) {}   // 문서 결과의 fetchedAt (M3a1b 캐시 행의 fetched_at, 캐시 없으면 받은 시각)

public record KoWikiDocStatus(String title, Long revision, Instant retrievedAt, DocState state) {}
public enum DocState { FRESH, CACHED, STALE, ABSENT, UNAVAILABLE }  // CACHED·STALE은 M3a1b
```

이름은 바꿔도 되지만 필드의 뜻과 `allUnavailable`·`degraded` 의 정의는 지킨다. 바꾸면 보고서에 적는다. M3a1·M3a1b에서 만든 형은 그대로 쓴다.

## 테스트 방침

M3a1 계획서 "테스트 방침"을 따른다. 추가:

- **녹화 fixture**: 데모 날짜 사건 링크의 pageprops 응답(`fixtures/wikimedia/kowiki/pageprops-*.json`), `README.md` 에 원 URL·녹화 일자·라이선스. 녹화는 M3a1 T2와 같은 방식(공개 User-Agent, `curl`). 네트워크를 쓸 수 없으면 "막힘"으로 보고하고 PM이 실행할 `curl` 명령을 적는다.
- **반례 합성 fixture**: 중복이 아닌데 합쳐질 수 있는 경우를 중립 문장으로(예: "시험 대회 A조 경기 가"·"시험 대회 A조 경기 나"가 같은 대회 링크만 공유). 실제 사건·인물을 지어내지 않는다.
- 시간 상한은 작은 테스트 값(예: 상한 1초, 읽기 타임아웃 300ms)과 WireMock 지연으로 시험한다.

## 데모 입력 3개로 확인할 것 (녹화 fixture 기준)

| 데모 입력 | 확인 |
|---|---|
| 1988-09-17 + "서울" | 본문에 "서울 하계 올림픽"이 있거나 링크에 `1988년 하계 올림픽` 이 있는 사건이 **정확히 1개**, 남은 쪽 `sourceTitle` = `9월 17일`, `sourceSection` = `사건`, `linkTitles` 첫 번째가 `1988년 하계 올림픽`, `wikidataQid` 가 pageprops fixture 값과 같다. 모든 사건의 `date` 가 1988-09-17. 두 문서의 `revision` 이 fixture `README.md` 의 값과 같다 |
| 2002-06-22 + "광주 서구 풍암동" | 본문에 "스페인"이 든 사건이 **정확히 1개**, `sourceTitle` = `6월 22일`(남은 쪽 절은 fixture대로 보고). 같은 날 다른 경기 줄이 있으면 그 줄은 남는다 |
| 1995-03-15 + "서울대학교병원" | 사건 0개, 두 문서 상태 `FRESH`, `allUnavailable()` = false, `degraded()` = false, pageprops 요청 0 — M3a2가 #21 안내로 바꾼다 |

## 크기 (400줄 규칙)

예상: 결과 형·사건 ID 약 35, 흐름·불변 조건 약 30, 시간 상한 약 20, 링크 해석(pageprops 클라이언트·정규화) 약 45, 중복 제거(D1~D6, 일반 링크 리소스) 약 80 → **약 210줄**(M3a1 실제 비율 1.6배로 보정하면 약 340). **T1 뒤 누적 S1이 220줄을 넘으면** 오케스트레이터가 PM에게 T2(중복 제거)를 별도 기능으로 뺄지 묻는다.

## 작업 목록

순서: T1 → T2 → T3.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 결과 형, `KoWikiEventSource`, 불변 조건, 날짜당 시간 상한, 링크 정규화·pageprops 해석 | backend-dev | 결과 레코드, `KoWikiEventSource` 구현, 링크 해석 클라이언트, `wikimedia.kowiki-day-deadline`, pageprops 녹화 fixture(+ `README.md`), 테스트 | - (M3a1 머지, 가능하면 M3a1b 머지) | (1) `cd server && ./gradlew check` 성공. (2) 입력 순서: 날짜 문서 사건 → 문화 → 연도 문서 사건 → 문화(합성). (3) **불변 조건**: WireMock이 받은 문서 제목이 1988-09-17 → `9월 17일`·`1988년`, 2000-02-29 → `2월 29일`·`2000년`, 1995-03-15 → `3월 15일`·`1995년` 뿐이고, 결과의 모든 사건 `date` 가 요청 날짜와 같다. (4) **시간 상한**(테스트 값 상한 1초, 읽기 300ms): 모든 요청이 멈추면 `find` 가 1.5초 안에 돌아오고 두 문서 `UNAVAILABLE`, 날짜 문서가 시간 초과면 연도 문서 요청 0. 날짜 문서는 정상이고 연도 문서만 멈추면 날짜 문서 사건을 돌려주고 `degraded()` = true. (5) pageprops: 호출 한 번에 요청 1회(제목을 `\|` 로 묶음, 51개면 2회), `User-Agent` 헤더, 넘겨주기를 따라 `linkTitles` 가 정규 제목으로 바뀌고 대표 링크 QID가 `wikidataQid` 에 담긴다(녹화 fixture 값). 사건이 없으면 요청 0. (6) pageprops 503 → 사건은 그대로, `wikidataQid` 모두 `null`, `linkResolutionFailed` = true, `degraded()` = true. (7) 사건 ID가 `^kowiki-\d{4}-\d{2}-\d{2}-[0-9a-f]{8}$` 이고 두 번 호출해도 같다. (8) 한 문서만 실패(503 두 번) → 다른 문서의 사건, 그 문서 `UNAVAILABLE`, `degraded()` = true. 두 문서 모두 실패 → 사건 0개, `allUnavailable()` = true. 두 문서 모두 `missingtitle` → 사건 0개, 둘 다 `ABSENT`, `allUnavailable()` = false. (9) `skippedLines` 가 두 문서 합. (10) 사건 `text` 가 파서 출력과 글자 하나까지 같다 |
| T2 | 중복 제거 D1~D6 | backend-dev | 중복 제거 모듈, `kowiki-generic-links.txt`, `wikimedia.dedup.min-link-overlap`, 반례 합성 fixture, 테스트 | T1 | (1) "데모" 표 3행 통과 — **1988-09-17 개막 정확히 1개, 2002-06-22 "스페인" 경기 정확히 1개**. (2) 규칙마다 합성 테스트: D1 같은 절의 같은 대표 링크 두 줄 → 둘 다 남음. D2 같은 문장이 날짜·연도 문서에 → 1개. D3 대표 링크가 일반 링크(`대한민국`)인 줄이 구체 링크를 공유 → 합쳐짐, 일반 링크만 공유 → 둘 다 남음, 구체 링크 하나만 공유하고 겹침 계수가 기준 미만(반례 "시험 대회 A조 경기 가/나") → 둘 다 남음. D4 뒤 절 사건 하나가 앞 절 사건 둘과 맞음 → 합치지 않음. D5 남는 쪽이 입력 순서상 가장 앞이고 문장·출처·판 번호 그대로. (3) 링크 정규화: pageprops 실패 상태에서도 `2002 X` 와 `2002년 X` 링크를 가진 두 사건(합성)이 합쳐진다. 제목은 다르고 QID가 같은 두 링크(합성)도 합쳐진다. (4) `mergedCount` 가 뺀 수와 같다. (5) 로그 캡처: 중복 제거 로그에 사건 문장·날짜가 없다. (6) 보고서에 데모 3날짜의 묶음 표(남은 사건 ID·문서·절, 뺀 사건의 문서·절, 규칙), 고른 겹침 기준값, 일반 링크 목록 |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트(테스트 파일만) | T2 | (1) T1·T2 기준을 표로 확인하고 빠진 테스트를 추가. (2) `cd server && ./gradlew check` 통과(개수 보고), 3회 반복 안정. (3) C1, C2 출력 없음. (4) S1·S2 결과 보고. (5) 데모 3개 날짜의 `KoWikiEventSource` 결과 표(사건 수, 본문 앞 30자, 출처 문서, 절, 대표 링크, QID, 건너뛴 줄 수, 뺀 사건 수) — **M3a2 T1(content-curator 큐레이션)과 M3-P16 (b) 품질 필터 검수가 이 표를 쓴다**. (6) 같은 사건이 두 번 보일 만한 줄이 남았는지 사람이 표를 보고 적는다 |

완료 기준에 쓰는 검사 명령: M3a1 계획서의 C1·C2·S1·S2와 같다.

담당별: backend-dev 2 (T1, T2), test-engineer 1 (T3). PM 작업 없음(결과는 T3 표로 본다).

## 리스크와 확인할 것

1. **합치기가 너무 세거나 약할 수 있다** — 세면 같은 날 다른 사건이 사라지고, 약하면 같은 사건이 두 번 보인다. 대응: D1, 일반 링크 제외, 겹침 기준, D4, 반례 테스트, T3 사람 확인. 사건을 잃는 쪽보다 두 번 보이는 쪽을 덜 나쁜 실패로 본다.
2. **일반 링크 목록은 손으로 관리한다** — 데모 밖 날짜에서 새 나라 이름이 대표 링크면 D3이 그 링크로 합치지 못할 뿐(두 번 보임), 잘못 합치지는 않는다(겹침 기준이 함께 걸린다).
3. **pageprops 요청이 늘어난다** — 캐시가 있어도 호출마다 1회(링크가 없으면 0회). 분당 200회 한도 안이다. 지연이 문제면 M6b에서 해석 결과 캐시를 제안한다.
4. **시간 상한 값** — 5초는 첫 화면 3초·전체 10초 목표 사이의 출발값이다. 세계 섹션 전체 시간은 M3a2·M3a3에서 Wikidata·Commons가 더해지므로 M6b에서 측정해 보정한다.
5. **M3a1b와 병렬일 때 겹침** — `DocState`·`application.yml`. 병렬이면 오케스트레이터가 합칠 때 정리한다.

## PM이 정할 것

없음. (화면에 내보낼 사건을 고르는 품질 필터는 M3a2의 M3-P16 — 2026-10-11 (b)로 결정.)
