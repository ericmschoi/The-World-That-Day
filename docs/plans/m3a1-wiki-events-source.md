# 위키백과 사건 출처: 외부 호출 공통, 클라이언트, 파서 (M3a1)

> 브랜치: `feat/wiki-events-source` · 작성: planner · 상태: PM 승인 (2026-10-11) · **2026-10-11 PM 결정으로 분할**: 원래 T3(캐시·합치기·`KoWikiEventSource`)은 M3a1b `feat/wiki-section-cache`(호출 보강 + 캐시, `docs/plans/m3a1b-wiki-section-cache.md`)와 M3a1c `feat/wiki-events-merge`(합치기·중복 제거·`KoWikiEventSource`, `docs/plans/m3a1c-wiki-events-merge.md`)로 옮겼다
> 기준: `CLAUDE.md` 7절, `docs/product-brief.md` 5절 4번·6절·8절·12절, `docs/roadmap.md` M3, `docs/research/day-events.md` (T5), `docs/research/summary.md` 2.1절·6절·8절 5~7항
> M3 묶음: `chore/m1-followups` → **M3a1** → M3a1b `feat/wiki-section-cache` → M3a1c `feat/wiki-events-merge` → M3a2 `feat/world-events-api` → M3a3 `feat/event-media`, 웹은 M3b1 `feat/design-foundation` → M3b1b `feat/media-card`(M3a와 병렬) → M3b2 `feat/world-events-ui`. 나눈 이유와 순서는 로드맵 M3 절.

## 2026-10-11 분할 (PM 결정)

| 항목 | 내용 |
|---|---|
| 이 기능에 남은 것 | T1 외부 호출 공통(완료, 5b93b14), T2 클라이언트·파서·녹화 fixture(완료, e31a542 b440cb1), **T4** 완료 기준 검증(완료, 8feaf87), 리뷰 후속 수정(오케스트레이터, 4bc3e3d: 503 + `Retry-After` 호스트 차단, 절대 URL 검증) |
| 옮긴 것 | 원래 T3 → M3a1b(위키텍스트 캐시 V2 + 리뷰에서 나온 호출 보강) / M3a1c(`KoWikiEventSource`, 합치기·중복 제거, 링크 정규화, 날짜당 시간 상한). PM은 "T3을 새 기능 하나로"라고 했지만 검증·리뷰 입력이 더해져 한 PR에 넣으면 400줄을 크게 넘어 둘로 나눴다(M3a1b 계획서 첫 절). 이 재분할은 PM 승인(2026-10-11) |
| 실제 제품 코드 줄 수 (S1) | T1 **244** + T2 **333** = **577줄**(4bc3e3d 수정분은 별도). 400줄 기준을 넘지만, 이 계획서의 "T2 뒤 300줄을 넘으면 마지막 작업을 뺀다" 규칙대로 T3을 빼는 것으로 PM이 받아들였다(2026-10-11) |
| 작업 번호 | 검증 작업은 **T4 그대로** 둔다(T3으로 당기지 않음). 이미 진행된 test-engineer 작업·커밋 트레일러·진행 로그와 번호가 어긋나지 않게 하고, "M3a1 T3" 은 계속 "옮긴 작업"을 뜻하게 한다 |
| 예상과 차이 | 예상은 T1 약 105, T2 약 210이었다. 재시도·429 차단기와 파서 경계 처리가 예상보다 컸다(약 1.6배). 이후 M3 계획서들의 분할 기준에 이 차이를 반영했다 |
| T4 검증에서 나온 것 (8feaf87) | (1) 같은 사건이 여러 절·문서에 나와 3~4번 보일 수 있다(1988-09-17 개막). (2) 같은 문서를 가리키는 링크 표기가 다르다(`2002 FIFA 월드컵` / `2002년 FIFA 월드컵`). (3) 파서는 문서가 날짜와 맞는지 보지 않는다. (4) 국기 템플릿으로 빠진 줄은 녹화 12개 절에서 1줄(2002-05-31 개막). → (1)~(3)은 M3a1c, (4)는 후속 작업 후보 |
| 코드 리뷰(승인)에서 나온 것 | `ratelimited` 오류 코드, 동시 요청 3개 제한, HTML 태그·엔티티 → M3a1b. 날짜당 시간 상한 → M3a1c. 낮은 품질 문장 거르기 → M3a2(M3-P16 (b), 결정 2026-10-11) |

## 목표

날짜 하나를 주면 한국어 위키백과 날짜 문서("9월 17일")와 연도 문서("1988년")의 "사건"·"문화" 절에서 **연·월·일이 같은 사건 줄**을 골라 평문으로 돌려줄 수 있는 부품을 만든다. 이 기능은 엔드포인트도, 두 문서를 합친 결과(`KoWikiEventSource`)도 만들지 않는다.

끝나면 다음이 된다.

1. 모든 Wikimedia 호출이 공통 설정을 거친다: User-Agent 헤더, 연결·읽기 타임아웃, 조회(GET)만 1회 재시도, 429와 `Retry-After` 존중, 엔드포인트 주소는 설정값. 이후 Wikidata(M3a2)·Commons(M3a3)가 같은 설정을 쓴다.
2. `KoWikiClient` 가 문서 하나의 판 번호(`revid`)와 "사건"·"문화" 절 위키텍스트를 가져오고, 문서가 없으면 `ABSENT` 를 돌려준다.
3. `WikitextEventParser` 가 절 위키텍스트에서 그날 사건 줄을 평문으로 골라낸다.
4. 테스트는 녹화한 Wikimedia fixture와 WireMock만 쓰고 외부 네트워크를 부르지 않는다.

## 범위 밖

- 위키텍스트 캐시(Flyway V2), `ratelimited`, 동시 요청 제한, HTML 태그·엔티티 → **M3a1b**
- 두 문서 합치기·중복 제거, 링크 정규화, 날짜당 시간 상한, `KoWikiEventSource` → **M3a1c**
- HTTP 엔드포인트, 날짜 파라미터 검증, 에러 응답 형식, `tone`, 사건 범위(`scope`), Wikidata → M3a2
- 미디어(Commons), 공용 미디어 형식 → M3a3
- On this day 어댑터 (#20 (b)일 때만, 별도 기능)
- 날짜 문서 366개·연도 문서 미리 받기(워밍업) → 필요하면 M6b "다음 단계 제안"
- 탄생·사망·기념일 절 — **요청하지도, 저장하지도 않는다**(실제 인물의 생년월일이 들어 있다)
- 위키텍스트 템플릿(`{{...}}`) 전개 — 템플릿이 든 줄은 건너뛰고 센다(리스크 2). "안전한 템플릿 목록"은 후속 작업 후보 `chore/kowiki-safe-templates`(로드맵)
- `prop=sections` → `prop=tocdata` 전환 — 이 기능의 계약을 바꾸지 않는다. 후속 작업 후보 `chore/kowiki-tocdata`(리스크 7)

## 사용할 외부 출처

| 출처 | 확신도 | 근거 | 지킬 조건 (리서치 문서에 적힌 대로) |
|---|---|---|---|
| 한국어 위키백과 날짜 문서·연도 문서 (MediaWiki Action API `action=parse`) | **확인됨** | [day-events.md 3절 카드](../research/day-events.md#3-출처-카드), [summary 2.1절](../research/summary.md#21-확인됨--구현에-쓸-수-있다) | 텍스트 CC BY-SA 4.0 — 판 번호를 남기고(이 기능), 화면에서 문서 링크와 라이선스 고지(M3a2·M3b2). 연락처가 든 User-Agent 필수(없으면 분당 10회, 차단 위험). 규칙에 맞으면 분당 200회, 429와 `Retry-After` 처리, 직렬 요청 권장. "사건"·"문화" 절만 읽는다. 캐시 허용(캐시는 M3a1b) |

키는 필요 없다. `WIKIMEDIA_USER_AGENT` 는 M1a에서 `.env.example` 에 이름을 넣었고 값은 PM이 정한다(#25 (a) 저장소 URL).

## 전제 (PM 결정과 가정)

| 항목 | 내용 | 상태 |
|---|---|---|
| 사건 출처 조합 | #20 (a) 한국어 위키백과 + Wikidata + Commons, On this day 없음 | 결정 2026-10-11 |
| 위키백과 문장 | #23 (a) 원문 그대로, 링크 기호·굵은 글씨 기호·각주만 제거, 번역·요약 안 함 | 결정 2026-10-11 |
| 테스트 fixture | #12 (a): Wikimedia 응답은 녹화 fixture 가능, 녹화 일자·원 URL·라이선스 기록 | 결정 |
| User-Agent 연락처 | #25 (a) 저장소 URL | 결정 |
| 캐시 저장 위치 | M3-P3 (a) DB 테이블(Flyway V2) — M3a1b에서 구현 | 결정 2026-10-11 |

## 설계

### 패키지 (T1·T2에서 만든 대로 — 이름이 다르면 T1·T2 보고서를 따른다)

| 패키지 | 들어가는 것 |
|---|---|
| `io.github.ericmschoi.theworldthatday.wikimedia` | `WikimediaProperties`(`@ConfigurationProperties("wikimedia")`), Wikimedia용 HTTP 클라이언트(`WikimediaHttpClient`: User-Agent, 타임아웃, 재시도), 호스트별 차단기(429, 503 + `Retry-After`). Wikidata·Commons 어댑터도 여기서 클라이언트를 받는다 |
| `...event.kowiki` | `KoWikiClient`(인터페이스) ↔ `ActionApiKoWikiClient`(Action API 구현), `KoWikiPageKind`(날짜 → 문서 제목), `WikitextEventParser`. (M3a1b가 캐시로 감싼 클라이언트를, M3a1c가 링크 해석·중복 제거·`KoWikiEventSource` 를 더한다) |

### 외부 호출 공통 규칙

| 규칙 | 값 | 비고 |
|---|---|---|
| User-Agent | `wikimedia.user-agent` (= `${WIKIMEDIA_USER_AGENT:}`) 를 모든 요청 `User-Agent` 헤더에 | 비어 있으면 **Wikimedia 출처 전체를 끈다**: 요청을 보내지 않고 바로 "사용 불가"로 돌려준다. 시작할 때 WARN 한 번. 값 자체는 로그에 남기지 않는다 |
| 연결 타임아웃 | `wikimedia.connect-timeout: 2s` | |
| 읽기 타임아웃 | `wikimedia.read-timeout: 4s` | M3a2가 Wikidata용 값을 따로 둘 수 있다. 날짜당 전체 상한은 M3a1c |
| 재시도 | 조회(GET)만, `wikimedia.max-retries: 1` 회. 연결·읽기 타임아웃, 502·503·504일 때만 | 4xx와 429는 재시도하지 않는다 |
| 429 (와 503 + `Retry-After`, 4bc3e3d) | 응답의 `Retry-After`(초) 동안 **그 호스트로 요청을 보내지 않는다**. 헤더가 없으면 30초, 최대 `wikimedia.rate-limit-max-wait: 120s` | 요청 안에서 잠들지 않는다(10초 목표, 제품 정의서 8절). 차단 중이면 바로 "사용 불가". HTTP 200 + `ratelimited` 는 M3a1b |
| 엔드포인트 | `wikimedia.kowiki-api-url: https://ko.wikipedia.org/w/api.php` (절대 URL 검증, 4bc3e3d) | Wikidata·Commons 주소는 M3a2·M3a3이 같은 접두사 아래에 추가 |
| 시계 | `Clock` 빈 (기본 `Asia/Seoul`) | 429 차단 만료·캐시 TTL(M3a1b) 테스트용. M3a2가 날짜 검증에 같은 빈을 쓴다 |
| 로그 | 실패는 WARN 한 줄: 출처 이름, 문서 제목, 상태 코드 또는 예외 종류 | 요청 URL 전체·응답 본문을 남기지 않는다. 문서 제목("9월 17일")만으로는 사람과 연결되지 않지만, M2 이후 장소와 같은 줄에 남기지 않는다(`CLAUDE.md` 7절 6항) |

Spring Boot 4.1 HTTP 클라이언트 구성 방법은 T1 보고서(5b93b14)에 적혀 있다.

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
- T2 메모: `prop=sections` 는 MediaWiki에서 사용 중단 예고(`prop=tocdata` 권장)가 붙어 있다. 지금은 동작하므로 이 계약은 바꾸지 않는다(리스크 7).

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
- 평문 바꾸기 (#23 (a)의 "링크 기호 제거" 범위): `[[대상|별칭]]` → 별칭, `[[대상]]` → 대상, `'''굵게'''`·`''기울임''` → 글자만, `<ref>…</ref>`·`<ref …/>`·`<!-- -->` 삭제, `&nbsp;` → 공백, 공백 정리. (그 밖의 HTML 태그·엔티티 규칙은 M3a1b T1)
- `[[파일:…]]`, `[[File:…]]`, `[[분류:…]]` 링크는 통째로 지운다.
- **템플릿(`{{`)이나 외부 링크(`[http`)가 든 줄은 건너뛰고 센다** (내용을 바꾸지 않기 위해). 건너뛴 줄 수를 결과에 담는다. 국기 템플릿 때문에 빠진 줄은 녹화 12개 절에서 1줄(2002-05-31 개막, T4)이라 후속 작업 후보로 둔다.
- 사건 링크: 구분자 뒤 본문에 나온 위키 링크의 대상 제목(밑줄 → 공백, `#절` 제거)을 순서대로. 맨 앞 연도·날짜 링크는 넣지 않는다. 첫 번째를 "대표 링크"라 부른다(M3a2의 QID·큐레이션 키). 넘겨주기 해석·표기 정규화는 M3a1c가 한다.
- **파서는 문서가 찾는 날짜와 맞는지 확인하지 않는다**(날짜 문서는 연도만, 연도 문서는 월·일만 본다). 맞는 문서를 고르는 것은 호출하는 쪽(`KoWikiPageKind.title(date)`)의 책임이고, M3a1c가 이 불변 조건을 테스트로 지킨다(T4 발견 (3)).
- 결과 줄의 `summary_modified` 는 항상 `false` 다(위 바꾸기 외에는 손대지 않는다).

### 뒤 기능에 넘기는 것

`KoWikiClient`(문서 하나 → `revid`, 사건·문화 절 위키텍스트, 문서 상태 `FRESH`/`ABSENT`/`UNAVAILABLE`), `KoWikiPageKind`, `WikitextEventParser`(절 하나 → 사건 줄 + 건너뛴 줄 수). M3a1b가 이 클라이언트를 캐시로 감싸고, M3a1c가 합치기·중복 제거와 M3a2에 넘기는 `KoWikiEventSource` 계약을 만든다.

### 설정 (`application.yml` 추가분, T1에서 넣음)

```yaml
wikimedia:
  user-agent: ${WIKIMEDIA_USER_AGENT:}
  connect-timeout: 2s
  read-timeout: 4s
  max-retries: 1
  rate-limit-max-wait: 120s
  kowiki-api-url: https://ko.wikipedia.org/w/api.php
  # max-concurrent-requests, cache-ttl 은 M3a1b / kowiki-day-deadline, dedup.* 는 M3a1c
```

## 테스트 방침 (M3 서버 작업 공통 — M3a1b·M3a1c·M3a2·M3a3도 따른다)

1. 테스트는 외부 네트워크를 부르지 않는다. 테스트 설정은 모든 `wikimedia.*-url` 을 WireMock(`http://localhost:<port>`)으로 바꾼다. 테스트 하나가 테스트 컨텍스트의 `wikimedia.kowiki-api-url` 이 `http://localhost` 로 시작하는지 확인한다.
2. WireMock 의존성은 T1에서 넣은 판을 쓴다(T1 보고서).
3. **녹화 fixture** (`server/src/test/resources/fixtures/wikimedia/kowiki/`): 데모 날짜에 필요한 문서 6개(`9월 17일`, `6월 22일`, `3월 15일`, `1988년`, `2002년`, `1995년`)의 절 목록 응답과 사건·문화 절 위키텍스트 응답. 같은 폴더 `README.md` 에 파일마다 원 URL, 녹화 일자, `revid`, 라이선스(CC BY-SA 4.0, 위키백과 텍스트)를 적는다. User-Agent 값은 적지 않는다.
   - 녹화는 backend-dev가 `curl` 로 한다. User-Agent는 `.env` 를 읽지 않고 공개값 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)` 를 명령에 직접 쓴다(#25 (a), 비밀값 아님).
   - 탄생·사망 절은 요청하지 않으므로 fixture에 없다. C1(아래)로 확인한다.
4. **합성 fixture** (`fixtures/wikimedia/kowiki/synthetic/`, 파일 이름 `synthetic-` 로 시작): 파서 경계 사례, 오류 응답(`missingtitle`, 503, 429), 윤일 경계값 `2000-02-29`(`2월 29일`, `2000년` 문서). 내용은 "시험 사건 가" 같은 중립 문장만 쓴다. 실제 사건·인물을 지어내지 않는다.
5. 시간은 `Clock` 을 바꿔서 시험한다(`Thread.sleep` 금지).
6. 테스트 날짜는 데모 입력 3개, 공개 역사 사건 날짜와 경계값(윤일, 오늘·내일)만 쓴다(`CLAUDE.md` 7절 6항, M3-P13).

## 데모 입력 3개로 확인할 것 (파서·클라이언트 수준, 녹화 fixture 기준)

두 문서를 합친 결과(중복 제거 후 "정확히 1개" 등)는 M3a1c에서 확인한다.

| 데모 입력 | 이 기능에서 확인 |
|---|---|
| 1988-09-17 + "서울" | `9월 17일` 사건 절에서 1988년 → "서울 하계 올림픽"이 든 사건 1개, 대표 링크 `1988년 하계 올림픽`. `1988년` 문서에서 9월 17일 → 1개 이상. 두 문서의 `revid` 가 fixture `README.md` 의 값과 같다 |
| 2002-06-22 + "광주 서구 풍암동" | `6월 22일` 문화 절에서 2002년 → "스페인"이 든 사건 1개 이상 |
| 1995-03-15 + "서울대학교병원" | `3월 15일` 에서 1995년 → 0개, `1995년` 에서 3월 15일 → 0개. 두 문서 상태가 `FRESH`(실패가 아니라 비어 있음) |

## 크기 (400줄 규칙)

세는 법: 제품 코드 + 설정(`build.gradle.kts`, `application.yml`, 마이그레이션)은 센다. 테스트 코드와 fixture(`server/src/test/**`)는 따로 보고한다.

| 작업 | 예상 | 실제 (S1) |
|---|---|---|
| T1 외부 호출 공통 | 약 105 | **244** |
| T2 클라이언트 + 파서 | 약 210 | **333** |
| 합계 | 약 315 (T3 포함 400~450) | **577** (+ 4bc3e3d 리뷰 수정분) — PM이 분할로 받아들임(2026-10-11) |

확인 명령은 아래 S1·S2.

## 작업 목록

순서: T1 → T2 → T4. (T3은 M3a1b·M3a1c로 옮김. 검증 작업 번호는 T4 그대로)

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | Wikimedia 외부 호출 공통 설정 + WireMock 테스트 기반 **(완료, 5b93b14)** | backend-dev | `wikimedia` 패키지(프로퍼티, HTTP 클라이언트 구성, 재시도, 429 차단기), `application.yml` 의 `wikimedia.*`, `Clock` 빈, WireMock 테스트 의존성, 테스트 | - | (1) `cd server && ./gradlew check` 성공. (2) WireMock 테스트: 모든 요청의 `User-Agent` 헤더가 설정값과 같다. (3) `wikimedia.user-agent` 가 빈 값이면 WireMock 요청 수 0이고 호출 결과가 "사용 불가"이며, 시작 로그에 WARN 1줄(값은 출력하지 않음). (4) 읽기 타임아웃(테스트 값 300ms)보다 늦는 응답 → 2초 안에 "사용 불가", 요청 수 2(첫 요청 + 재시도 1). (5) 503 다음 200 → 성공, 요청 수 2. 404 → 재시도 없음, 요청 수 1. (6) 429 + `Retry-After: 60` → 시계를 59초 옮긴 뒤 호출하면 요청 수가 늘지 않고 "사용 불가", 61초 옮기면 요청이 나간다. `Retry-After: 999` 는 120초로 자른다. 헤더가 없으면 30초. (7) 테스트 컨텍스트의 `wikimedia.kowiki-api-url` 이 `http://localhost` 로 시작한다는 테스트. (8) 보고서에 고른 WireMock 판과 Boot 4.1 HTTP 클라이언트 구성 방법(클래스·프로퍼티 이름)을 적는다 |
| T2 | 위키백과 클라이언트 + 위키텍스트 파서 + 녹화 fixture **(완료, e31a542 b440cb1)** | backend-dev | `KoWikiClient`·`ActionApiKoWikiClient`, `WikitextEventParser`, `fixtures/wikimedia/kowiki/**`(+ `README.md`), 테스트 | T1 | (1) C1 출력 없음. `README.md` 에 녹화 fixture마다 원 URL·녹화 일자·`revid`·라이선스가 있다. (2) 파서 + 녹화 fixture: `9월 17일` 사건 절에서 1988년 → 본문에 "서울 하계 올림픽"이 든 사건 1개, 대표 링크 `1988년 하계 올림픽`. `3월 15일` 에서 1995년 → 0개. `6월 22일` 문화 절에서 2002년 → "스페인"이 든 사건 1개 이상. `1988년` 문서에서 9월 17일 → 1개 이상, `1995년` 문서에서 3월 15일 → 0개. (3) 파서 + 합성 fixture: 별칭 링크, 굵은 글씨, `<ref>` 제거, `[[파일:…]]` 제거, 하위 줄(`**`)이 연도·날짜를 물려받음, 엔 대시 구분자, 템플릿 줄과 외부 링크 줄은 건너뛰고 `skippedLines` 증가, 날짜 범위 줄 건너뜀 — 사례마다 테스트 1개 이상. (4) 클라이언트 + WireMock: 문서마다 요청이 정확히 "절 목록 1 + 있는 절(사건·문화) 수"이고, WireMock이 받은 `section` 값이 사건·문화 절 번호뿐이다(다른 번호 요청 0). 위키텍스트 요청의 `oldid` 가 절 목록 응답의 `revid` 와 같다. 요청 제목이 `9월 17일`, `1988년`(합성 경계값: `2월 29일`, `2000년`). (5) `missingtitle` 응답 → 상태 `ABSENT`, 재시도 없음(요청 수 1) |
| ~~T3~~ | → **M3a1b `feat/wiki-section-cache`**(캐시 V2 + 호출 보강) / **M3a1c `feat/wiki-events-merge`**(합치기·중복 제거·`KoWikiEventSource`) 로 옮김 | - | - | - | - |
| T4 | 완료 기준 검증과 전체 테스트 **(완료, 8feaf87)** | test-engineer | 빠진 기준의 테스트(테스트 파일만) | T2 | (1) T1·T2 기준을 표로 확인하고 빠진 테스트를 추가. (2) `cd server && ./gradlew check` 통과(개수 보고), 같은 명령 3회 반복해 불안정한 테스트가 없음. (3) C1, C2 출력 없음. (4) S1·S2 결과 보고(S1은 577줄 — 테스트 파일만 더하므로 S1이 바뀌면 안 된다). (5) 데모 3개 날짜 × 문서 2개의 파서 결과 표(문서, 절, 그날 사건 수, 본문 앞 30자, 대표 링크, 건너뛴 줄 수). (6) 녹화 fixture에서 건너뛴 줄에 든 템플릿 이름별 개수 표 |

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

담당별: backend-dev 2 (T1, T2), test-engineer 1 (T4) — 모두 완료. PM 작업 없음(엔드포인트가 없어 직접 확인할 화면·명령이 없다. 결과는 T4 표로 본다).

## 리스크와 확인할 것

1. **크기** — 실제 577줄. PM이 분할로 받아들였다. 리뷰어가 볼 양이 크므로 PR 본문에 파일별 줄 수를 함께 적는다.
2. **템플릿이 든 줄을 건너뛴다** — 녹화 12개 절에서 1줄(2002-05-31 개막)뿐이었다(T4). 우선순위가 낮아 후속 작업 후보 `chore/kowiki-safe-templates` 로 둔다.
3. **위키텍스트 형식이 문서마다 다르다** — 사람이 편집하는 자유 형식이다(리서치 8절 9항). 형식이 맞지 않는 줄은 건너뛰고 세며, 판 번호를 남겨 어느 판을 읽었는지 추적한다.
4. **같은 사건이 여러 절·문서에 나온다** (T4) — M3a1c의 중복 제거·링크 정규화가 해결한다.
5. **파서가 문서와 날짜의 일치를 확인하지 않는다** (T4) — 호출하는 쪽의 불변 조건으로 두고 M3a1c가 테스트한다.
6. **`.env` 의 User-Agent 값에 공백·괄호가 있다** — 루트 `.env` 는 Compose와 Spring이 함께 읽는다(M1a 리스크 5). Compose가 이 줄을 문제없이 넘기는지 M3a2 PM 확인(T5)에서 `docker compose config --quiet` 로 함께 본다.
7. **`prop=sections` 사용 중단 예고** (T2 메모) — MediaWiki가 `prop=tocdata` 로 옮기라고 안내한다. 지금은 동작하고 녹화 fixture도 이 형식이라 M3a1 계약은 그대로 둔다. 후속 작업 후보 `chore/kowiki-tocdata`(클라이언트의 절 목록 요청만 `prop=tocdata` 로 바꾸고 fixture 재녹화, 약 20줄 + fixture)를 로드맵 "후속 작업 후보"에 올렸다. 응답에 경고가 실제로 붙거나 동작이 바뀌면 앞당긴다.

## PM이 정할 것

없음 (분할과 M3a1b·M3a1c 재분할은 2026-10-11 결정).
