# M2b 입력 폼·날짜 검증·오프닝 (M2)

> 브랜치: `feat/input-and-opening` (`main` 49248e9 이후) · 작성: planner (2026-10-27) · 상태: **PM 승인 (2026-10-27): M2 공통 결정 모두 추천대로 — ★ T1은 PM 직접(M2-P10 (a))**
> 기준: 로드맵 "M2 장소 해석과 입력" M2b 항목, 제품 정의서 3·5·8·12절, M2 공통 결정(`docs/plans/m2a-place-resolve.md` "M2 공통" — #3, M2-P1, M2-P3, M2-P6, M2-P7, M2-P10), 머지 코드: `web/src/App.tsx`, `screens/InputScreen.tsx`(임시 데모·표본 버튼), `consent/ConsentNotice.tsx`·`consent.ts`(M3b3b), `screens/StoryScreen.tsx`, `story/sections.ts`, `api/client.ts`(10초·`AbortSignal` — M3b2), `e2e/network.ts`(`openDemo`), 서버 `story/StoryDate.java`
> 이 PR은 **장소를 서버로 보내지 않는다**. 장소 해석 호출과 후보 선택은 M2b2(`feat/place-picker`). 그래서 M2a·카카오를 기다리지 않고 M2a와 동시에 한다.

## 목표

1. 실제 입력 폼: 생년월일(년·월·일 선택 상자, M2-P6 (a)), 태어난 장소(자유 입력), 출생 시각(선택), 제출 버튼 앞의 필수 동의(영상 23번 (b), D2 (a) — M3b3b `ConsentNotice` 를 옮김).
2. 날짜 검증: 웹(필수·없는 날짜·미래·1950-01-01 전)과 서버 `GET /api/story/world` 하한(#3 (a)).
3. 오프닝 섹션: "1988년 9월 17일 토요일, 서울" — 요일 계산, 출생 시각이 있으면 함께. 이 PR에서 장소는 입력한 글자 그대로(M2b2가 고른 후보의 `name` 으로 바꿈).
4. **임시 데모·표본 버튼 제거**. 그 버튼을 누르던 기존 시험은 폼에 같은 날짜를 넣는 도우미로 바꾼다(단언은 그대로).
5. 화면 전환 포커스 이동(M1 리뷰에서 미룸).
6. 결과 화면 주소·저장소 규칙, `Referrer-Policy`, 자동 완성 끄기.
7. 개인정보처리방침: 출생 시각과 장소 처리, 같은 화면의 YouTube 스크립트(M2-P7 (a)).

## 범위 밖

- 장소 해석 호출·후보 선택·찾지 못함·국외 안내 → M2b2. 지도에서 고르기(M2-P2), 공유 링크(M2-P3 (a)).
- 뒤로 가기 기록(`history`) — 이 PR은 주소를 바꾸지 않아 뒤로 가기는 페이지를 떠난다. 결과 화면의 "처음으로"로 돌아간다. 흐름 다듬기 M6a에서 다시 본다.
- 세계 섹션의 400 `code` 별 문구: 폼이 먼저 걸러 실제로는 나오지 않는다. 섹션 문구는 그대로("이 섹션을 불러오지 못했습니다").
- 이미 끝난 미룬 항목: API 클라이언트 타임아웃·`AbortSignal`(M3b2), `SectionId`(M3b1).
- `REGION` 이면 동네 섹션 숨김 → M2b2(장소 유형을 알게 되는 곳).

## 사용할 외부 출처

없음. (서버 하한은 계산만. 시험은 MSW·Playwright 가로채기만 쓴다.)

## API / 데이터 변경

### 서버: `GET /api/story/world` 하한 (T1, #3 (a))

| 상태 | `code` | 조건 |
|---|---|---|
| 400 | `DATE_TOO_EARLY` | 존재하는 날짜이지만 `1950-01-01` 보다 앞 (1950-01-01은 허용) |

검사 순서: `DATE_REQUIRED` → `DATE_FORMAT` → `DATE_NOT_EXIST` → `DATE_IN_FUTURE` → `DATE_TOO_EARLY`. `detail` 은 고정 영어 `date must not be earlier than 1950-01-01`. 시작 날짜는 `StoryDate` 상수 하나(설정값 아님). 나머지 응답은 그대로.

### 웹: 화면과 상태 (API 호출은 기존 `GET /api/story/world` 만)

입력 화면(`InputScreen`):

| 요소 | 규칙 |
|---|---|
| 제목 | `h1` "The World That Day", `tabIndex={-1}`(돌아올 때 포커스) |
| 폼 | `<form noValidate autoComplete="off">` — 브라우저 말풍선 대신 우리 문구, 브라우저 자동 완성 기록을 남기지 않음 |
| 생년월일 (필수) | `fieldset` + `legend` "생년월일 (필수)". 선택 상자 3개: 년(올해 → 1950, 내림차순), 월(1~12), 일(1~31, 달에 따라 줄이지 않음 — 없는 날짜는 문구로 거부). 각 상자에 접근 이름("년", "월", "일") |
| 태어난 장소 (필수) | 글자 입력, 접근 이름 "태어난 장소 (필수)", 안내 "예: 서울, 광주 서구 풍암동, 서울대학교병원" |
| 출생 시각 (선택) | `<input type="time">`, 안내 "오프닝 문구에만 씁니다. 서버로 보내지 않습니다." |
| 동의 | `ConsentNotice` 를 제출 버튼 바로 앞으로(구성 요소는 그대로, `ref`·`hint` 사용) |
| 제출 | `<button type="submit">` "그날 보기". 막지 않는다(`disabled` 없음) |

제출할 때 모든 칸을 검사하고, 틀린 칸마다 문구를 그 칸 아래에 두고(`aria-invalid`, `aria-describedby`), DOM 순서로 처음 틀린 칸(년 상자 → 장소 → 시각 → 동의 상자)에 포커스. 모두 맞고 동의했을 때만 결과 화면으로.

| `code` (웹) | 조건 | 문구 |
|---|---|---|
| `DATE_REQUIRED` | 년·월·일 중 하나라도 비었음 | 생년월일을 골라 주세요. |
| `DATE_NOT_EXIST` | 없는 날짜(2월 30일, 평년 2월 29일, 4월 31일) | 없는 날짜입니다. 날짜를 다시 확인해 주세요. |
| `DATE_IN_FUTURE` | Asia/Seoul 오늘보다 뒤 | 오늘 이후의 날짜는 볼 수 없습니다. |
| `DATE_TOO_EARLY` | 1950-01-01 전(선택 상자로는 못 고르지만 함수는 검사) | 1950년 1월 1일부터 볼 수 있습니다. |
| `PLACE_REQUIRED` | 앞뒤 공백을 뺀 뒤 빈 값 | 태어난 장소를 입력해 주세요. |
| `PLACE_TOO_LONG` | 앞뒤 공백을 뺀 뒤 50자 초과(M2a 서버와 같은 한도) | 장소는 50자까지 입력할 수 있습니다. |
| 동의 안 함 | — | 기존 `CONSENT_HINT` "개인정보처리방침에 동의해야 볼 수 있습니다." |

결과 화면(`StoryScreen`): 오프닝 섹션이 처음 섹션. 포커스는 오프닝 문장(`tabIndex={-1}`)으로 옮긴다. "처음으로"를 누르면 입력 화면으로 가고 포커스는 입력 화면 `h1`, 입력한 값과 동의는 메모리에 남는다(새로 고침하면 사라짐).

오프닝 문장 (`--font-size-display`, M3b1에서 예약):

| 입력 | 문장 |
|---|---|
| 1988-09-17, `서울` | 1988년 9월 17일 토요일, 서울 |
| 2002-06-22, `광주 서구 풍암동`, 시각 15:20 | 2002년 6월 22일 토요일 오후 3시 20분, 광주 서구 풍암동 |
| 1995-03-15, `서울대학교병원`, 시각 00:05 | 1995년 3월 15일 수요일 오전 0시 5분, 서울대학교병원 |
| 시각 12:00 / 09:00 | 오후 12시 / 오전 9시 (분이 0이면 생략) |

요일은 날짜만으로 계산한다(시간대 영향 없음). 장소는 입력 글자를 앞뒤 공백만 빼서 그대로(M2b2 전까지).

### 결과 주소·저장 규칙 (C-규칙, 시험으로 지킴)

| # | 규칙 |
|---|---|
| C1 | 입력·결과·처음으로 오가는 동안 페이지 주소(경로·쿼리·해시)가 바뀌지 않는다. `history.pushState`·`replaceState` 를 부르지 않는다 |
| C2 | `localStorage`·`sessionStorage`·쿠키·IndexedDB에 아무것도 쓰지 않는다 |
| C3 | 출생 시각과 장소는 어떤 요청에도 들어가지 않는다(이 PR). 날짜는 `GET /api/story/world` 에만 |
| C4 | `index.html` 에 `<meta name="referrer" content="strict-origin-when-cross-origin">`. 페이지 전체에 `no-referrer` 를 걸지 않는다(YouTube 오류 153). 사진 `img` 의 요소별 `no-referrer` 는 그대로 |
| C5 | 폼과 입력 칸이 `autocomplete="off"` |

## 작업 목록

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | ★ `StoryDate` 하한 `DATE_TOO_EARLY` | PM 또는 backend-dev (M2-P10) | 결정 #3 | (1) 위 표·검사 순서 그대로. (2) MockMvc: `1950-01-01` 200(서비스 대역), `1949-12-31` 400 `DATE_TOO_EARLY`, `detail`·`instance` 에 날짜 없음. (3) 기존 `StoryDate`·세계 API 시험을 고치지 않고 통과(표본 1950-06-25 포함). (4) 로그 없음(400은 로그를 남기지 않는 규약). (5) `cd server && ./gradlew check` |
| T2 | 날짜 규칙과 생년월일 칸 | frontend-dev | 결정 #3, M2-P6 | (1) `web/src/input/dateRules.ts`: 검사(위 `code` 표), 요일, 오프닝 날짜·시각 문구, Asia/Seoul 오늘. (2) 단위 시험: 1988-09-17 토요일, 2002-06-22 토요일, 1995-03-15 수요일, 2000-02-29 화요일(윤일 통과), 2001-02-29·1988-02-30 `DATE_NOT_EXIST`, 1950-01-01 통과·일요일, 1949-12-31 `DATE_TOO_EARLY`, 가짜 시계로 Asia/Seoul 오늘 통과·내일 `DATE_IN_FUTURE`(UTC 14:59와 15:00 경계), 시각 표의 네 경우. (3) `BirthDateField`: 년 상자에 올해부터 1950까지 빠짐없이, 월 12개, 일 31개, 접근 이름, 오류 문구 연결. (4) `npm run lint && npm run format:check && npm run typecheck && npm test` |
| T3 | 입력 폼, 동의 이동, 데모·표본 버튼 제거 | frontend-dev | T2 | (1) 위 입력 화면 표·제출 규칙·문구 그대로. (2) 컴포넌트 시험: 빈 제출 → 년 상자에 포커스와 문구 셋; 2월 30일 → `DATE_NOT_EXIST`; 장소 51자 → `PLACE_TOO_LONG`; 동의 안 함 → 힌트와 동의 상자 포커스(기존 `consent.test` 단언 유지); 모두 맞음 → `onSubmit({date, place, time})`. (3) C1 출력 없음(`로 보기` 버튼·`DEMO_DATES`·`openDemo` 가 없다). (4) 데모 버튼을 누르던 시험(`App.test`, `consent.test`, `SiteFooter.test`, `world*.test`, e2e `consent`·`focus-and-tokens`·`page-background`·`video-playback`·`youtube-player`)은 새 도우미 `web/src/test/storyInput.ts`(`openStory(user, {date, place})`)와 e2e `openStory(page, {date, place})`(`openDemo` 를 바꿈)로 같은 날짜를 넣는다. 장소는 데모 짝(1988-09-17 `서울`, 2002-06-22 `광주 서구 풍암동`, 1995-03-15 `서울대학교병원`), 표본 날짜(1969-07-20·1986-09-20 등)는 `서울`. 그 밖의 단언은 바꾸지 않는다(C2). (5) 웹 검사 4종과 `npm run e2e` 통과. **확인점**: T3 뒤 누적 S1 330을 넘으면 알림(멈추지 않음) |
| T4 | 오프닝 섹션, 흐름, 포커스, 주소·저장 규칙 | frontend-dev | T3 | (1) 오프닝 문장 표 그대로, 섹션 순서 `SECTIONS` 그대로. (2) 포커스: 제출 뒤 `document.activeElement` 가 오프닝 문장, "처음으로" 뒤 입력 화면 `h1`, 값과 동의가 남아 있음. (3) C1~C5를 시험으로: jsdom에서 `location.href` 가 그대로이고 `pushState`·`replaceState` 호출 0, 저장소·쿠키 비어 있음, MSW가 받은 요청은 `/api/story/world?date=…` 하나뿐이고 장소·시각 글자가 어떤 요청 주소에도 없음; `index.html` 의 `referrer` meta; 입력 칸 `autocomplete`. (4) Playwright: 데모 3개 입력으로 오프닝 문장과 세계 섹션이 보이고 `page.url()` 이 처음과 같음, 키보드만으로 제출·포커스 이동. (5) 웹 검사 4종과 `npm run e2e` |
| T5 | 개인정보처리방침 문구 + 시행일 | frontend-dev | T4 | (1) "받는 정보"를: "이 서비스는 입력한 생년월일(날짜), 태어난 장소, 출생 시각(선택)을 받습니다. 계정, 이름, 연락처는 받지 않습니다. 날짜만 서버로 보내고, 장소와 출생 시각은 지금 화면에 보여 주는 데만 씁니다." (2) "다른 곳의 콘텐츠와 광고"에 M2-P7 (a) 한 문장: "같은 화면의 YouTube IFrame API 스크립트는 화면에 보이는 내용(입력한 날짜와 장소 포함)을 읽을 수 있습니다." (3) `privacy.test.ts` 가 두 문장을 확인. (4) 시행일은 PM이 머지하는 날 한 줄 커밋(M3a5a T6 방식) |
| T6 | 검증과 전체 시험 | test-engineer | T1~T5 | (1) T1~T5 기준 표. (2) `cd server && ./gradlew check` 2회. (3) 웹 검사 4종 2회, `npm run e2e` 2회, `npm run build`. (4) C1~C3 출력 없음. (5) 바뀐 기존 시험 목록과 "입력 단계만 바뀌고 단언은 같음" 대조표. (6) S1·S2 |
| T7 | 화면 확인 | PM | T6 | 휴대폰(또는 개발자 도구 모바일 크기)에서 데모 입력 3개 → 오프닝 문장이 표와 같고 세계 섹션이 나옴; 2월 30일·내일 날짜 거부 문구; 동의 없이 제출하면 동의 상자로 이동; 결과 화면 주소가 처음과 같음. 문구 검토 |

담당별: backend-dev 1(★, M2-P10), frontend-dev 4, test-engineer 1, PM 1. 합계 7개.

검사 명령 (저장소 루트):

```
# C1 — 임시 데모·표본 버튼이 남지 않았는지. 출력이 없어야 한다
grep -rnE '로 보기|DEMO_DATES|openDemo' web/src web/e2e

# C2 — 시험 표시가 지워지지 않았는지. 출력이 없어야 한다
git diff -U0 main...HEAD -- web/src web/e2e server/src/test | grep -E '^-[[:space:]]*(it|test)\(|^-[[:space:]]*@(Test|ParameterizedTest)'

# C3 — 웹이 브라우저 저장소·기록을 쓰지 않는지(시험 파일 제외). 출력이 없어야 한다
grep -rnE 'localStorage|sessionStorage|document\.cookie|indexedDB|pushState|replaceState' web/src --include='*.ts' --include='*.tsx' | grep -vE '\.test\.tsx?:|/test/'

# S1 — 제품 코드 (시험·mock 제외)
git diff --numstat main...HEAD -- server web/src web/index.html web/privacy.html \
  ':(exclude,glob)server/src/test/**' ':(exclude,glob)web/src/**/*.test.ts' \
  ':(exclude,glob)web/src/**/*.test.tsx' ':(exclude,glob)web/src/test/**' \
  ':(exclude,glob)web/src/mocks/**' | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
```

## 크기 (400줄 규칙)

웹은 최근 실측 ×1.0~×1.46(가운데 ×1.2), 서버는 ×2.5.

| 작업 | 파일별 셈 | 보정 |
|---|---|---|
| T1 서버 하한 | 약 8 | ×2.5 → 약 20 |
| T2 날짜 규칙·생년월일 칸 | 약 100 | ×1.2 → 약 120 |
| T3 폼·동의 이동·버튼 제거(−25 포함) | 약 115 | ×1.2 → 약 140 |
| T4 오프닝·흐름·포커스·meta | 약 95 | ×1.2 → 약 115 |
| T5 방침 | 약 6 | 약 6 |
| **합계** | 약 325 | **약 400 (340~495)** |

400 안팎이다. 확인점(T3 뒤 330, 알림). 시험 변경(S2)은 기존 시험 10여 개 파일의 입력 단계가 바뀌어 클 것이다(약 300~450줄) — S1에 넣지 않는다.

## 병렬 실행

- M2a(`feat/place-resolve`)와 다른 worktree에서 동시에 한다. 이 PR의 서버 파일은 `StoryDate` 와 그 시험뿐이다.
- 이 PR 안: T1(서버)과 T2~T5(웹)는 폴더가 달라 병렬 가능. T1이 PM 몫(M2-P10 (a))이면 웹은 T1을 기다리지 않는다 — T6만 T1 뒤.

## 리스크와 확인할 것

1. **기존 시험 변경이 많다** — 데모 버튼으로 결과 화면에 가던 시험이 10여 개 파일이다. 입력 단계만 도우미로 바꾸고 단언은 그대로 둔다(T6 (5) 대조표). 시험을 지우지 않는다.
2. **PM의 남은 M3 화면 확인** — 표본 버튼이 사라진다. M3b3 T6(1969-07-20)·M3b3c T6(실제 YouTube)은 이 PR 머지 전에 `main` 에서 하거나, 머지 뒤 폼에 같은 표본 날짜와 `서울` 을 넣어 한다(공개 역사 사건 날짜, `CLAUDE.md` 7.6).
3. **시간대** — "오늘"은 Asia/Seoul(서버와 같음). 다른 시간대의 기기에서 자정 근처에 웹과 서버 판단이 같아야 한다 — 가짜 시계 경계 시험(T2).
4. **시작 날짜가 두 곳에 있다** — 서버 `StoryDate` 와 웹 `dateRules` 의 1950-01-01. 바꿀 때 둘 다(#3 결정 문서에 적음). 서버가 웹 파일을 읽는 계약 시험은 두지 않는다(크기 대비 이득이 작다).
5. **오프닝 입력값이 같은 화면의 외부 스크립트에 보인다** — M2-P7 (a)로 방침에 밝힌다.
6. **장소를 서버로 보내지 않는 동안** — 이 PR만 머지된 상태에서는 아무 글자나 넣어도 결과가 나온다(장소는 오프닝에만). M2b2가 해석·후보·찾지 못함을 붙인다.

## PM 결정 필요

- PM 결정 (2026-10-27, 구현 중 질문 — 추천대로): (B) 단언은 그대로 두고 이름이 낡은 시험 3개(`App.test`, `consent.test`, e2e `consent.spec`)의 이름만 바꾼다. (C) 출생 시각을 덜 채우면 지금처럼 "시각 없음"으로 두고, 칸 아래에 시와 분을 모두 넣어야 반영된다는 안내 한 줄을 더한다(오류로 막지 않음). A·D·E는 `docs/plans/m2a-place-resolve.md` (M2a 브랜치).

M2 공통 결정(`docs/plans/m2a-place-resolve.md` "M2 공통") 가운데 이 PR에 걸린 것: **#3**(시작 날짜), **M2-P1**(나누기), **M2-P3**(공유 링크 없음 — 주소 규칙 C1), **M2-P6**(생년월일 입력 모양), **M2-P7**(오프닝과 방침), **M2-P10**(★ T1 `StoryDate` 하한: PM 직접 추천).
