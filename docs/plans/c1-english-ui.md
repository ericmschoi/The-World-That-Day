# 영어 화면 전환 (C1 · S1)

> 브랜치: `feat/english-ui` (`main` 에서) · 작성: planner (2026-10-29) · 상태: **PM 승인 (2026-10-29)**
> 기준: `docs/plans/canada-research.md` "지금 시작할 것" S1과 그 PM 결정(2026-10-29: 1 (b), 2 (a), 3 (a), 4 (b), 5 (b), 8 (a), 9 (a) en-CA, 10 (b)), `docs/research/canada-privacy.md`(T8) 10절, 머지 코드(`main` 의 `web/`, `server/.../CommonsFileMapper.java`, `StoryDate.java`), `CLAUDE.md` 7절, 제품 정의서 6절.
> 표기: "결정 N"은 리서치 계획의 PM 결정, "D N"은 `canada-privacy.md` 10.2의 결정 후보, "S-N"은 그 10.1의 방침 변경 번호, "S1-PN"은 이 문서 끝의 PM 결정이다.
> 다음 PR: `docs/plans/c1-input-and-opening-ca.md` (S2, M2b 고쳐 쓰기). 두 PR은 `InputScreen`·동의·`privacy.html`·시험 도우미를 함께 고치므로 차례로 한다(S1 머지 → S2).

## 목표

1. 웹 화면에서 **우리가 쓰는 글자**를 모두 캐나다 영어(en-CA)로 바꾼다. 화면 구조, 토큰, 미디어 카드, 재생 규칙, 동의 구성 요소는 그대로 둔다.
2. `index.html`·`privacy.html` 의 `lang` 을 `en-CA` 로 바꾼다. 서버가 준 한국어 사건 문장에는 `lang` 을 달아 화면 읽기 프로그램이 제대로 읽게 한다.
3. 개인정보처리방침을 영어로 옮기고, S1-P4 (a)(= D3 (a))이면 T8이 S1 몫으로 정한 보강 문장(S-2~S-6, S-8, S-10, S-12)을 더한다. 번역만 하면 목적, 보관, 국외 처리, 접근·이의·책임자 문장이 빠진다(`canada-privacy.md` 9절).
4. 서버가 만드는 표시 문구 2개(Commons 작가·라이선스 없음)를 영어로 바꾼다.
5. (S1-P3 추천안) 서버 `StoryDate` 의 하한 1950-01-01(`DATE_TOO_EARLY`)과 "오늘" 시간대 `America/St_Johns`(결정 2 (a))를 이 PR에서 한다.

## 범위 밖

- **서버가 돌려주는 자료 값**: 사건 문장·제목, `source_name` "한국어 위키백과", `source_title`("9월 17일"), `source_section`("사건"/"문화"), YouTube 출처 이름 "KTV 아카이브". 이 값들은 출처가 바뀌는 S3a·S3b·S4에서 바뀐다. 그 전까지 영어 화면 안에 한국어 사건 문장이 보인다(T7에서 PM이 확인).
- 서버 자료를 그대로 옮긴 웹 fixture 값(사건, `data/schema/examples/` 에서 복사한 미디어 레코드)과 `data/` 의 예시·스키마 설명. S3a·S3b에서 녹화를 새로 하거나 content-curator 작업으로 바꾼다.
- 글꼴 목록(`--font-family-base` 의 한국어 글꼴)과 `word-break: keep-all`. 한국어 사건 문장이 남아 있는 동안 필요하다. S3a에서 다시 본다.
- 입력 폼, 날짜 검증(웹), 오프닝 → S2. 장소 해석 → C2(리서치 T1 후속 계획). 프랑스어 화면과 프랑스어 방침(D5).
- 임시 데모·표본 버튼과 안내 문장(`InputScreen.tsx`)의 글자 → S1-P1 (a)면 S2에서 버튼과 함께 지운다.
- 방침 가운데 입력·장소에 걸린 것: S-1(받는 정보)과 S-9(입력 화면 요약) → S2. S-11(출처 목록 갱신) → S3a·S3b·S4·C2가 각자. 책임자의 비공개 연락처, 로그 보관 기간 문장, 그 밖의 D6 목록 → 공개 배포 결정(#5) 때.

## 사용할 외부 출처

없음. 새 외부 호출이 없고, 시험은 MSW·Playwright 가로채기·기존 fixture만 쓴다.

방침 문장의 근거는 `docs/research/canada-privacy.md` 10.1이다. 그 문서는 법률 자문이 아니고 문장은 초안이다. 이 PR은 그 초안을 그대로 쓰고, 표현을 고치면 뜻을 바꾸지 않는다.

## API / 데이터 변경

### 서버 응답 값 (모양은 그대로)

| 자리 | 지금 | 바뀐 뒤 |
|---|---|---|
| `CommonsFileMapper.NO_AUTHOR` (`attribution` 안) | 작가 정보 없음 | No author information |
| `CommonsFileMapper.NO_LICENSE` (`attribution` 안) | 라이선스 확인 안 됨 | Licence not verified |

웹 `LICENSE_UNKNOWN` 과 서버 `NO_LICENSE` 는 같은 글자 `Licence not verified` 다.

### 서버 `GET /api/story/world` 의 `date` 검사 (T5, S1-P3 (b)일 때)

검사 순서: `DATE_REQUIRED` → `DATE_FORMAT` → `DATE_NOT_EXIST` → `DATE_IN_FUTURE` → `DATE_TOO_EARLY`.

| 상태 | `code` | 조건 | `detail` (고정 영어) |
|---|---|---|---|
| 400 | `DATE_IN_FUTURE` | `America/St_Johns` 의 오늘보다 뒤 | `date must not be later than today in America/St_Johns` |
| 400 | `DATE_TOO_EARLY` (새) | 존재하는 날짜이지만 1950-01-01 전 (1950-01-01은 허용) | `date must not be earlier than 1950-01-01` |

```
GET /api/story/world?date=1949-12-31
→ 400 { ..., "code": "DATE_TOO_EARLY", "detail": "date must not be earlier than 1950-01-01" }
```

나머지 필드는 기존 400 응답과 같다. 시간대와 시작 날짜는 `StoryDate` 의 상수 `TODAY_ZONE`·`START_DATE` 다(설정값 아님). `ClockConfig.SERVICE_ZONE` 은 다른 부품의 시계가 쓰므로 이 PR에서 바꾸지 않는다(S3a에서 다시 봄). 웹 `dateRules` 도 같은 두 값을 갖는다(S2). 한쪽을 바꾸면 다른 쪽도 바꾼다고 두 파일 주석에 적는다.

### 웹 문구표 (en-CA, 결정 9 (a))

**미디어 캡션 (T1)**

| 자리 | 지금 | 영어 |
|---|---|---|
| 배지 | 같은 날 / 같은 달 / 같은 해 | Same day / Same month / Same year |
| 배지 (`years_apart`) | N년 전 / N년 뒤 | N years earlier / N years later (1이면 `1 year earlier` / `1 year later`) |
| 배지 (`approximate`) | 같은 해 무렵 / N년 전 무렵 | Around the same year / About N years earlier (뒤면 later, 1이면 year) |
| 배지·글 (`current`) | 현재 모습 · 촬영 시점 미제공 | Present day · Capture time not provided |
| 글 (모름, 배지 없음) | 촬영 시점 미상 | Capture time unknown |
| 촬영 시점 글 | 1988년 9월 17일 촬영 / 1988년 9월 촬영 / 1988년 촬영 | Taken September 17, 1988 / Taken September 1988 / Taken 1988 |
| 촬영 시점 글 (`approx`) | 1988년 무렵 촬영(추정) | Taken around 1988 (estimated) |
| 라이선스 | 라이선스 확인 안 됨 / 제공사 이용약관에 따름 | Licence not verified / Provider's terms apply |
| 링크 | 원문 보기 · 라이선스 / 이용약관 | View original · Licence / Terms |
| 새 탭 링크 (숨김 글자) | (새 창에서 열림) | (opens in a new tab) |

달 이름은 `Intl` 을 쓰지 않고 표(January~December)로 만든다. ICU 판마다 공백 문자가 다를 수 있어서다. 결과는 en-CA 긴 날짜 형식과 같다.

**화면 틀·세계·동의·바닥글 (T2)**

| 자리 | 지금 | 영어 |
|---|---|---|
| 섹션 제목 (`sections.ts`) | 오프닝 / 그날의 동네 / 그 도시의 풍경 / 그날의 세계 / 마무리 | Opening / The neighbourhood that day / Scenes of the city / The world that day / Closing |
| 섹션 상태 | 불러오는 중… / 아직 준비 중인 섹션입니다. / 이 섹션을 불러오지 못했습니다. | Loading… / This section is coming soon. / This section couldn't be loaded. |
| 결과 화면 버튼 | 처음으로 | Start over |
| 사건 없음 | 이 날짜에 기록된 큰 사건을 찾지 못했습니다. | No major events were found for this date. |
| 사건 목록 접근 이름 | 사건 목록 | Events |
| 비극 묶음 제목 | 같은 날 있었던 아픈 일 | Difficult events on the same day |
| 그날 미디어 소제목 | 그날 찍힌 사진 / 다른 때 찍힌 사진 / 촬영 시점을 확인하지 못한 사진 | Photos taken that day / Photos taken at other times / Photos with an unknown capture time |
| 대체 글 | 기록 사진 / 기록 영상 / 「제목」 관련 자료 / 사건 관련 자료 | Archive photo / Archive video / Material about “title” / Material about this event |
| 출처 줄 | 출처: 이름 「제목」 · 라이선스 | Source: name, “title” · licence |
| 바닥글 | 개인정보처리방침 | Privacy policy |
| 동의 상자 | 개인정보처리방침에 동의합니다 (필수) | I agree to the privacy policy (required) |
| 동의 링크 | 개인정보처리방침 보기 | Read the privacy policy |
| 동의 안내 (S-10, YouTube 의무 문장) | 이 서비스는 YouTube API 서비스를 … 동의하게 됩니다. | This service uses YouTube API Services. By using it, you agree to be bound by the YouTube Terms of Service. (링크 글자: YouTube Terms of Service) |
| 동의 힌트 | 개인정보처리방침에 동의해야 볼 수 있습니다. | Agree to the privacy policy to see the story. |

**`lang` (T2)**: `index.html`·`privacy.html` 은 `<html lang="en-CA">`. 사건 문장 `<p>` 와 출처 줄의 문서 제목 링크에는 `lang={event.summary_lang}` 을 단다(지금 서버 값 `ko`).

**주석**: UI 글자를 인용한 주석(예: `"(새 창에서 열림)"`, `"같은 날" → 0`)은 새 영어 글자로 고친다. 계획서 절 이름을 가리키는 한국어 주석은 그대로 둔다.

**fixture 글자 규칙 (T1·T2)**: (1) 누가 봐도 가짜인 시험 글("시험용 사건 문장 A" 등)은 영어 가짜 글("Test event sentence A")로 바꾼다. (2) 서버의 `NO_AUTHOR`·`NO_LICENSE` 출력을 흉내 낸 값(`mocks/fixtures/media/photos.ts` 의 attribution)은 새 영어 글자를 따른다. (3) 서버 응답이나 `data/` 에서 그대로 옮긴 값은 바꾸지 않는다(`worldFixtures.test.ts` 대조 유지). fixture README에 이 규칙을 한 줄 더한다.

### 개인정보처리방침 (T3, T3b)

절 제목 7개(순서 그대로): What we receive / How we use and share it / Content from elsewhere and ads / YouTube API Services / Cookies and device storage / Contact / Effective date and changes. 제목 `Privacy policy - The World That Day`(S-0).

| 자리 | T3 (번역) | T3b (S1-P4 (a)일 때 바꾸거나 더함, 문장은 `canada-privacy.md` 10.1 그대로) |
|---|---|---|
| What we receive | "This service receives the date of birth (a date) and place of birth that you enter. It does not receive accounts, names or contact details. Right now only the date is sent to the server; the place will be received once place input is added." | 그대로(S-1은 S2) |
| How we use and share it | 지금 항목을 하나씩 옮김. 결과 주소 항목은 "The address of the result page never contains your date of birth or place." | S-2 목적 문장을 첫 항목으로. "외부 출처에는 누구의 생년월일인지 보내지 않습니다" 항목을 S-4 문장("Outside sources receive the date as a search term. They are not told whose date it is.")으로 바꿈. S-3 보관 문장(로그 보관 기간 문장은 빼고) 더함 |
| Content from elsewhere and ads | 지금 문단을 옮김 | S-5 국외 처리 문단을 더함("…may be processed in other countries…", Google 처리 위치는 쓰지 않음) |
| YouTube API Services | 옮김(S-10) | 그대로 |
| Cookies and device storage | 옮김(S-8) | 그대로 |
| Contact | "Questions about this policy or your personal information: GitHub Issues." | S-6 문장(접근·정정할 것이 없음, 공개 이슈에 개인정보를 쓰지 말 것, "Responsible person: the operator")으로 바꿈. 직함·비공개 연락 경로는 D2와 공개 배포 때 |
| 나이 | 없음 | D4 (b)가 T3b 시작 전에 정해졌으면 S-7 한 문장("This service is for a general audience. If you are under 13 (14 in Quebec), please ask a parent or guardian first.")을 더한다. 아니면 넣지 않고 S2 T8b로 넘긴다. 연령 확인 게이트는 만들지 않는다 |
| Effective date and changes | "Effective date: <Month D, YYYY>" + 변경 때 페이지를 고치고 날짜를 새로 씀(S-12) | 그대로 |

## 작업 목록

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | 미디어 캡션 공용 문구 | frontend-dev | 결정 9 | (1) `dateLabel.ts`·`licenseLabel.ts`·`MediaCaption.tsx`·`NewTabLink.tsx`·`DayMediaGroups.tsx` 가 "미디어 캡션" 표와 글자가 같다. (2) 단위 시험: 배지 종류마다 하나씩, `years_apart` 1·3·−1·−3, `approximate` 0·2·−1, 정밀도 `day`/`month`/`year`/`approx` 글, 없는 날짜(1988-02-30, 2001-02-29)와 모양이 틀린 값 → `Capture time unknown` 에 배지 없음, `current` → `Present day`. (3) `DayMediaGroups` 의 "같은 날" 묶음 판정이 글자를 따로 적지 않고 `dateLabel.ts` 에서 내보낸 상수를 쓴다. 기존 묶음 시험은 기대 글자만 바뀌고 통과한다. (4) 주석 규칙. (5) `cd web && npm run lint && npm run format:check && npm run typecheck && npm test` 와 `npm run e2e` 통과 |
| T2 | 화면 틀·세계·동의·바닥글 문구와 `lang` | frontend-dev | T1 | (1) "화면 틀·세계·동의·바닥글" 표와 글자가 같다. (2) `index.html`·`privacy.html` 이 `lang="en-CA"` 이고 `bootstrap.test.ts` 가 이를 확인한다. (3) 컴포넌트 시험: `summary_lang: 'ko'` 사건의 문장 요소와 출처 제목 링크에 `lang="ko"` 가 있다. (4) fixture 글자 규칙 (1)~(3)을 지키고, 바꾼 fixture 값 목록을 보고에 적는다. (5) 아래 검사 E1의 출력이 허용 목록뿐이다. (6) 웹 검사 4종과 `npm run e2e`. **확인점 1**: T2 뒤 누적 S1이 220을 넘으면 알린다(멈추지 않음) |
| T3 | 개인정보처리방침 영어 번역 | frontend-dev | T2 | (1) 위 "개인정보처리방침" 표의 T3 열 그대로. 문단과 목록 항목이 한국어판과 같은 순서로 하나씩 대응하고, 사실 주장을 더하거나 빼지 않는다(출처 이름 Korean Wikipedia, Wikidata, Wikimedia Commons, YouTube Data API (KTV Archive channel)도 지금 그대로, S-11). (2) `privacy.test.ts` 가 절 제목 7개, T3 열에 적은 두 문장, `lang="en-CA"`, 본문에 한글이 없음을 확인한다. (3) 시행일 줄은 PM이 머지하는 날 그날 날짜로 한 줄만 고치는 커밋을 따로 만든다(M3a5a T6 방식) |
| T3b | 방침 보강 (S1-P4 (a)일 때) | frontend-dev | T3 | (1) 표의 T3b 열 그대로. 문장은 `canada-privacy.md` 10.1 초안을 쓰고, 고친 표현이 있으면 보고에 원문과 나란히 적는다. (2) `privacy.test.ts` 가 S-2·S-3·S-4·S-5·S-6 문장(그리고 넣었다면 S-7)의 핵심 구절을 하나씩 확인한다: "advertising or profiling", "not by person", "not told whose date it is", "other countries", "nothing to access or correct". (3) 방침에 이메일 주소·전화번호가 없다(`grep -nE '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}' web/privacy.html` 출력 없음). (4) 지금 설계와 어긋나는 주장이 없다: 서버로 가는 것은 날짜뿐, 저장소·쿠키 없음, 계정 없음. (5) 웹 검사 4종 |
| T4 | 서버 표시 문구 2개 | backend-dev | 결정 9 | (1) `NO_AUTHOR`·`NO_LICENSE` 가 위 "서버 응답 값" 표와 같다. (2) 이 두 값을 단언하던 시험(`CommonsFileMapperTest`, `WorldStoryDayMediaTest` 등)은 기대 글자만 바뀐다. 녹화 fixture는 바꾸지 않는다. (3) `grep -nE '"[^"]*[가-힣][^"]*"' server/src/main/java/io/github/ericmschoi/theworldthatday/media/commons/CommonsFileMapper.java` 출력 없음. (4) `cd server && ./gradlew check` |
| T5 | `StoryDate` 하한과 St. John's "오늘" (S1-P3 (b)일 때) | backend-dev | 결정 2·3, T4 (같은 worktree에서 차례로) | (1) 위 검사 순서·표·`detail` 그대로, 상수 `TODAY_ZONE = America/St_Johns`, `START_DATE = 1950-01-01`. `ClockConfig` 는 바꾸지 않는다. (2) 단위 시험: `1950-01-01` 통과, `1949-12-31` → `DATE_TOO_EARLY`, `1900-02-29` → 여전히 `DATE_NOT_EXIST`(순서). 경계: 표준시 `2026-01-15T03:29:59Z` 에 `2026-01-15` → `DATE_IN_FUTURE`, `03:30:00Z` 에 통과. 서머타임 `2026-07-15T02:29:59Z` → `DATE_IN_FUTURE`, `02:30:00Z` 통과. (3) 기존 서울 경계 시험(`todayIsTheSeoulCalendarDay`) 하나만 St. John's 경계로 바꾸고(결정 2에 따른 행동 변경), 그 밖의 `StoryDate`·세계 API 시험은 고치지 않고 통과한다(표본 1950-06-25 포함). (4) MockMvc: `GET /api/story/world?date=1949-12-31` 이 400 `DATE_TOO_EARLY` 이고 `detail`·`instance` 에 요청 날짜가 없다. (5) 새 로그 없음(400은 로그를 남기지 않는 규약). (6) `./gradlew check`. 시간 경계 시험은 3회 |
| T6 | 검증과 전체 시험 | test-engineer | T1~T5 | (1) T1~T5 기준 표(기준마다 확인 방법과 결과). (2) 새 시험 "화면 글자는 영어": 합성 영어 세계 응답(사건 `normal`·`tragic`, 배지 종류 전부, 촬영 시점 모름, 라이선스 모름, `provider-terms`, 그날 미디어 세 묶음)과 섹션 상태(로딩·준비 중·실패)를 그리고 `document.body.textContent` 에 한글(`/[ᄀ-ᇿ㄰-㆏가-힯]/`)이 없고, `/\blicense\b/i` 가 0건이며 `Licence` 가 있다. 입력 화면은 S1-P1 (a)면 이 시험에서 뺀다. (3) 웹 검사 4종 2회, `npm run e2e` 2회, `npm run build`. 서버 `./gradlew check` 2회(T5 경계 시험은 3회). (4) 검사 E1·C2 결과. (5) 바뀐 기존 시험 목록과 "기대 글자만 바뀜" 대조표(입력 단계·단언 개수가 같음). (6) S1 수치 |
| T7 | 화면 확인 | PM | T6 | `npm run dev` 에서 표본 버튼으로 결과 화면 → 섹션 제목·캡션 배지·라이선스·출처 줄·비극 묶음·바닥글이 영어, 사건 문장은 한국어(S3a 전까지 정상), 동의 안내와 방침 페이지가 영어. 문구 검토(S1-P2의 두 곳, 방침 보강 문장) |

담당별: frontend-dev 4, backend-dev 2, test-engineer 1, PM 1. 합계 8개(S1-P4가 (a)가 아니면 T3b가 빠져 7개). ★ 없음 — 결정 10 (b)(2026-10-29)로 `StoryDate` 는 backend-dev가 한다. 이 PR의 다른 서버 일은 글자 2개라 배울 거리가 작다.

### 진행 방식

- T1→T2→T3→T3b(웹)와 T4→T5(서버)는 폴더가 겹치지 않는다. 각자 worktree에서 병렬로 하고 오케스트레이터가 브랜치로 모은다. T6은 둘 다 끝난 뒤.
- S1-P3을 (a)로 고르면 T5는 S2 계획(`c1-input-and-opening-ca.md`)의 T1로 간다.

### 검사 명령 (저장소 루트)

```
# E1 — 제품 코드에 남은 한글(시험·mock·test 도우미·주석 줄 제외).
# 출력은 허용 목록만: InputScreen.tsx 임시 버튼·안내(S1-P1 (a)), world/types.ts 의 source_section 값 '사건' | '문화'(서버 계약, S3a)
grep -rnE '[가-힣]' web/src web/index.html web/privacy.html \
  | grep -vE '\.test\.tsx?:|/test/|/mocks/' \
  | grep -vE '^[^:]+:[0-9]+:[[:space:]]*(//|/\*|\*|\{/\*)'

# C2 — 시험 표시가 지워지지 않았는지. 출력이 없어야 한다
git diff -U0 main...HEAD -- web/src web/e2e server/src/test | grep -E '^-[[:space:]]*(it|test)\(|^-[[:space:]]*@(Test|ParameterizedTest)'

# S1 — 제품 코드 (시험·mock 제외, CSS와 HTML 포함)
git diff --numstat main...HEAD -- server web/src web/index.html web/privacy.html \
  ':(exclude,glob)server/src/test/**' ':(exclude,glob)web/src/**/*.test.ts' \
  ':(exclude,glob)web/src/**/*.test.tsx' ':(exclude,glob)web/src/test/**' \
  ':(exclude,glob)web/src/mocks/**' | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
```

## 크기 (400줄 규칙)

웹은 최근 실측 ×1.0~×1.7, 서버는 ×2.5. 대부분 한 줄을 다른 한 줄로 바꾸는 일이라 추가 줄 수가 곧 바뀐 줄 수다.

| 작업 | 파일별 셈 | 보정 |
|---|---|---|
| T1 캡션 (dateLabel 달 이름 표·복수형 포함) | 약 55 | 55~95 |
| T2 틀·세계·동의·바닥글·`lang`·CSS 주석 | 약 40 | 40~70 |
| T3 방침 번역 (영어 줄이 길어 줄 바꿈이 는다) | 약 75 | 75~110 |
| T3b 방침 보강 (문단 4~5개) | 약 25 | 25~40 |
| T4 서버 글자 | 약 3 | ×2.5 → 약 8 |
| T5 `StoryDate` | 약 14 | ×2.5 → 약 35 |
| **합계** | 약 212 | **약 240~360** |

400 아래다. 확인점 1(T2 뒤 220 알림), 확인점 2(T3b 뒤 웹 누적 330 알림), 확인점 3(모두 모은 뒤 400을 넘으면 멈추고 PM에게). 시험 변경은 S1에 넣지 않지만 클 것이다(기대 글자를 단언하는 시험 파일 약 50개, 약 400~700줄). 리뷰는 T6의 대조표로 한다.

## 개인정보

- 이 PR은 입력값을 새로 받거나 보내지 않는다. 결과 주소에는 지금처럼 날짜도 장소도 없다.
- T5: 400 응답의 `detail`·`instance` 에 요청 날짜를 넣지 않고, 로그를 남기지 않는다. 서버는 장소를 받지 않으므로 생년월일과 장소를 함께 남기는 로그가 생길 수 없다.
- 방침은 지금 설계를 사실대로 적는다(T3b (4)). 기준은 D1 (a)(PIPEDA 수준을 모범 사례로)를 전제로 한다. D1이 (b)로 정해지면 T3b를 빼고 번역만 남긴다.
- 시험 날짜는 제품 정의서 12절 데모, 공개된 역사적 사건의 날짜(1988-09-17 등 기존 fixture), 경계값(1949-12-31, 1950-01-01, 시간대 경계의 "오늘")만 쓴다(`CLAUDE.md` 7절 6항).

## 리스크와 확인할 것

1. **S2 merge 충돌**: S1과 M2b가 같은 시험 파일을 고친다. S1-P1 (a)로 임시 버튼 글자를 건드리지 않으면, M2b가 버튼 클릭을 도우미로 바꾼 줄과 S1이 기대 글자를 바꾼 줄이 대부분 다른 줄이 된다. 남는 충돌은 `privacy.html`·`privacy.test.ts`·`StoryScreen.tsx`·동의 시험 쪽이다(S2 T0). T3b로 방침이 더 바뀌므로 S2 T0에서는 방침을 S1 쪽으로 통째로 고른다.
2. **한 화면에 두 언어**: S3a 전까지 사건 문장·출처 제목은 한국어다. `lang` 으로 읽기는 맞추지만 보기에는 섞인다. T7에서 PM이 알고 확인한다.
3. **같은 글자가 두 곳**: `Licence not verified` 는 서버 `NO_LICENSE` 와 웹 `LICENSE_UNKNOWN` 에 따로 있다. 서버·웹 교차 시험은 두지 않는다(M2b의 시작 날짜와 같은 판단). 두 파일 주석에 서로를 적는다.
4. **철자 기준**: 화면은 licence·neighbourhood(en-CA). 코드 식별자(`license`, `neighborhood` 섹션 id, API 필드)는 바꾸지 않는다. T6 (2)가 화면 글자만 검사한다.
5. **시간대 상수가 서버에만 먼저 들어간다**(T5): S2 머지 전까지 웹은 "오늘"을 검사하지 않으므로(임시 버튼뿐) 어긋날 일이 없다.
6. **방침 문장은 법률 검토 전 초안이다**: D2(운영자 소재지)가 정해지면 S-6 책임자 문장이, 공개 배포 때는 연락처·로그 보관 문장이 바뀐다. 그때마다 시행일을 새로 쓴다(S-12).

## PM 결정 필요

> **PM 결정 (2026-10-29, "모두 추천대로 승인")**: S1-P1 (a) · S1-P2 (a) "Difficult events on the same day", (a) "Scenes of the city" · S1-P3 (b) `StoryDate` 는 이 PR의 T5 · S1-P4 (a) T3b에 S-0·S-2~S-6·S-8·S-10·S-12. 나이 문장(T8 D4)은 T9 요약 결정을 기다린다(T3b 시작 전에 확인).

- **S1-P1 임시 데모·표본 버튼의 글자**
  - (a) S1에서는 그대로 둔다(한국어). S2가 버튼을 지울 때 함께 사라진다. M2b merge 충돌이 가장 적다.
  - (b) S1에서 영어로 바꾼다. S1 머지 직후 화면이 모두 영어지만, 버튼 이름을 단언하는 시험 약 10개 파일이 M2b와 같은 줄에서 충돌한다.
  - **추천: (a)**. S1과 S2 사이의 짧은 기간에만 입력 화면에 한국어 버튼이 남는다.
- **S1-P2 문구 두 곳** (문구표 나머지는 이 계획 승인으로 함께 승인)
  - 비극 묶음 제목: (a) "Difficult events on the same day" / (b) "Also on this day, in sorrow" / (c) "Sad events on the same day". **추천: (a)** (차분하고 해석을 붙이지 않음, 제품 정의서 6.3).
  - 도시 섹션 제목: (a) "Scenes of the city" / (b) "The city then" / (c) "The city that day". **추천: (a)** (C7 내용이 "그때"만이 아닐 수 있음).
- **S1-P3 `StoryDate` 작업을 어디에 둘까** (승인된 계획은 S2)
  - (a) 계획대로 S2에 둔다. S2가 `main` 대비 약 470~500줄이 된다.
  - (b) 이 PR의 T5로 옮긴다. 서버 일이 웹과 병렬로 지금 시작되고, S2는 약 435~470줄로 준다. S1은 약 240~360줄.
  - (c) 따로 작은 PR(`feat/story-date-bounds`)로 낸다. PR이 하나 는다.
  - **추천: (b)**. 400줄 규칙과 서버·웹 병렬 방식에 맞고 새 브랜치가 필요 없다.
- **S1-P4 방침 보강을 언제 넣나** (= `canada-privacy.md` D3, 이 계획에서 함께 정함)
  - (a) S1에 S-0·S-2~S-6·S-8·S-10·S-12를 넣는다(T3b). 입력·장소에 걸린 S-1·S-9는 S2.
  - (b) S1은 번역만 하고 보강은 S2에 넣는다. S2가 약 30줄 더 커진다(이미 400 넘음).
  - (c) 공개 배포 결정(#5) 때까지 미룬다. 그동안 방침에 목적·국외 처리·책임자 문장이 없다.
  - **추천: (a)**. 문장 추가는 작고, S1이 어차피 파일 전체를 영어로 바꾼다. 나이 문장(D4)은 T9 요약에서 PM이 정한 대로 T3b 또는 S2 T8b에 넣는다.
