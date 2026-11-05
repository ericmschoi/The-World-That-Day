# 입력 폼·오프닝 캐나다판 — M2b 고쳐 쓰기 (C1 · S2)

> 브랜치: `feat/input-and-opening` (worktree `The-World-That-Day-m2b`, 기존 M2b 브랜치를 이어 씀) · 작성: planner (2026-10-29) · 상태: **PM 승인 (2026-10-29)**
> 기준: `docs/plans/canada-research.md` "M2b 브랜치: 고쳐 쓰기 추천 (결정 8-1)"과 그 PM 결정(2026-10-29: 1 (b), 2 (a), 3 (a), 5 (b), 8-1 (a), 9 (a), 10 (b)), 이 브랜치의 `docs/plans/m2b-input-and-opening.md`(승인 2026-10-27, T2~T5와 후속 B·C 완료, 누적 S1 +406/−70), `docs/plans/c1-english-ui.md`(S1), `docs/research/canada-privacy.md`(T8) 10절.
> 표기: "결정 N"은 리서치 계획의 PM 결정, "D N"·"S-N"은 `canada-privacy.md` 10절의 결정 후보·방침 변경 번호, "S1-PN"은 `c1-english-ui.md` 의 PM 결정, "S2-PN"은 이 문서 끝의 PM 결정이다.
> 이 문서는 M2b 계획의 **개정판**이다. M2b 계획의 목표·범위 밖·C1~C5 규칙·포커스 규칙·제출 규칙은 그대로이고, 아래에 적은 것만 바뀐다. 이 PR도 **장소를 서버로 보내지 않는다**(장소는 오프닝 글자뿐, C2에서 해석).

## 목표

1. S1이 머지된 `main` 을 이 브랜치에 merge 커밋으로 넣는다(rebase 없음).
2. M2b가 만든 폼·날짜 규칙·오프닝을 캐나다 영어(en-CA)로 바꾼다.
3. "오늘"을 `America/St_Johns` 로(결정 2 (a)), 시작 날짜는 1950-01-01 임시(결정 3 (a)) 그대로.
4. 데모 짝을 결정 1 (b)의 캐나다 입력 4개로 바꾼다.
5. 방침과 입력 화면에서 입력에 걸린 부분을 바꾼다: S-1(받는 정보, M2b T5 문장을 대신함), S-9(동의 상자 위 두 줄 요약), M2b T5의 YouTube 스크립트 문장.

## 범위 밖

- M2b 계획의 범위 밖 전부(장소 해석·후보 선택 → C2, 공유 링크, 뒤로 가기 기록, `REGION` 이면 동네 섹션 숨김).
- 방침의 S1 몫(S-0·S-2~S-6·S-8·S-10·S-12) → S1 T3b(S1-P4 (a)). 출처 목록(S-11) → S3a·S3b·S4·C2. 프랑스어 방침(D5), 공개 배포 때 다시 볼 것(D6).
- 장소를 지도 제공자에게 보낸다는 S-1의 뒷부분 → C2(장소가 실제로 서버로 갈 때). 이 PR에서는 장소가 어디에도 가지 않으므로 그 문장을 쓰지 않는다.
- 세계 섹션의 내용(한국어 사건 fixture) → S3a. 캐나다 데모 날짜에 맞는 세계 fixture는 S3a가 영어 위키백과 녹화로 만든다.
- 서버 `StoryDate` → S1-P3이 (b)면 S1 T5에서 끝난다. (a)면 아래 T1이 이 PR에 남는다.

## 사용할 외부 출처

없음. 시험은 MSW와 Playwright 가로채기(`e2e/network.ts`)만 쓴다. 방침·요약 문장의 근거는 `canada-privacy.md` 10.1(법률 자문 아님, 초안)이다.

## API / 데이터 변경

API는 M2b 계획 그대로다(웹은 `GET /api/story/world?date=` 만 부른다). 서버 하한·시간대는 `c1-english-ui.md` "서버 `GET /api/story/world` 의 `date` 검사" 표를 따른다.

### 웹 상수 (`web/src/input/dateRules.ts`)

| 이름 | 값 | 바뀐 점 |
|---|---|---|
| `TODAY_ZONE` | `'America/St_Johns'` | 새 상수. `seoulToday` → `serviceToday` 로 이름을 바꾼다 |
| `START_DATE` / `START_YEAR` | `'1950-01-01'` / `1950` | 그대로(결정 3 (a)) |

주석: "서버 `StoryDate.TODAY_ZONE`·`START_DATE` 와 같은 값이다. 함께 바꾼다."

### 입력 화면 문구 (M2b "입력 화면" 표의 글자만 바꿈)

| 요소 | 영어 |
|---|---|
| 생년월일 `legend` | Date of birth (required) |
| 선택 상자 접근 이름 | Year / Month / Day. 빈 선택지 `Select` |
| 월 선택지 | January … December (값은 1~12 그대로) |
| 년 선택지 | St. John's 기준 올해부터 1950까지 내림차순 |
| 장소 라벨 / 안내 | Place of birth (required) / e.g. Calgary; Hochelaga-Maisonneuve, Montréal; Vancouver General Hospital |
| 시각 라벨 / 안내 2줄 | Time of birth (optional) / Used only in the opening line. It is never sent to the server. / Enter both the hour and the minutes for the time to show. |
| 동의 상자 위 요약 (S-9, 새 2줄) | Only your date of birth is sent to our server, to find events, photos and videos from that day. Photos and videos load straight from Wikimedia and YouTube, which receive your IP address and may process it outside Canada. |
| 제출 | See that day |

S-9 요약은 `ConsentNotice` 의 체크 상자 바로 앞에 둔다(무엇을, 누가 받는지, 왜, 위험의 4요소). 방침 링크와 YouTube 의무 문장은 그대로다.

| `code` | 영어 문구 |
|---|---|
| `DATE_REQUIRED` | Choose your date of birth. |
| `DATE_NOT_EXIST` | That date doesn't exist. Please check it. |
| `DATE_IN_FUTURE` | Dates after today can't be shown. |
| `DATE_TOO_EARLY` | The earliest date you can choose is January 1, 1950. |
| `PLACE_REQUIRED` | Enter your place of birth. |
| `PLACE_TOO_LONG` | The place can be up to 50 characters long. |
| 동의 안 함 | S1의 `CONSENT_HINT` "Agree to the privacy policy to see the story." |

### 오프닝 문장 (결정 1 (b), 결정 9 (a))

| 입력 | 문장 |
|---|---|
| 1988-02-13, `Calgary` | Saturday, February 13, 1988, Calgary |
| 1976-07-17, `Hochelaga-Maisonneuve, Montréal`, 시각 15:20 | Saturday, July 17, 1976 at 3:20 p.m., Hochelaga-Maisonneuve, Montréal |
| 2010-02-12, `Vancouver General Hospital`, 시각 00:05 | Friday, February 12, 2010 at 12:05 a.m., Vancouver General Hospital |
| 1980-04-12, `St. John's` | Saturday, April 12, 1980, St. John's |
| 시각 12:00 / 09:00 | 12:00 p.m. / 9:00 a.m. (분이 0이어도 쓴다, en-CA 형식) |

요일과 날짜는 날짜만으로 계산한다(시간대 영향 없음). 장소는 입력 글자에서 앞뒤 공백만 뺀 그대로다(악센트·아포스트로피 그대로). 달·요일 이름과 `a.m.`/`p.m.` 은 표로 만든다. 같은 값을 `Intl.DateTimeFormat('en-CA', …)` 로 만들어 공백을 보통 공백으로 고친 결과와 같은지 시험 하나가 확인한다(ICU 판마다 공백 문자가 다를 수 있다).

### 시험 날짜와 장소 (결정 1 (b), S2-P1)

- 데모 짝 4개(위 표)는 입력 흐름·오프닝 시험에 쓴다.
- 기존 세계·미디어 시험의 fixture 날짜 가운데 공개 역사 사건 날짜(1988-09-17, 2002-06-22, 1969-07-20, 1986-09-20, 1950-06-25)는 그대로 쓰고, 장소 칸에는 `Calgary` 를 넣는다(장소는 화면 글자뿐이다).
- 옛 한국 데모 날짜 1995-03-15(사건 없음 fixture `world1995`)는 S2-P1 (a)면 경계값 1950-01-01로 옮긴다.

### 방침 (S1 영어판 위에서 바꾸는 곳)

- What we receive 문단(S-1, 이 PR 시점의 사실로): "You enter a date of birth, a place of birth and, optionally, a time of birth. Only the date is sent to our server. The place and time stay in your browser and are only shown on this screen. We do not receive accounts, names or contact details."
- Content from elsewhere and ads 끝(M2b T5 (2)를 옮김): "The YouTube IFrame API script on the same page can read what is shown on the screen, including the date and place you entered."
- 나이 문장(S-7): S1 T3b에 들어가지 않았고 D4 (b)가 정해졌으면 여기서 넣는다.

## 작업 목록

M2b의 ID를 이어 쓴다. T2~T5는 끝났고(한국어판), T6·T7은 아래 기준으로 다시 한다.

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | `main` merge와 충돌 해결 | frontend-dev (merge 시작과 merge 커밋은 오케스트레이터, `CLAUDE.md` 4절) | S1 머지 | (1) 오케스트레이터가 worktree에서 `git merge --no-ff main` 을 한다. (2) 충돌 해결 규칙: M2b의 구조(폼, 도우미 `openStory`, 버튼 제거)와 S1의 영어 글자를 함께 남긴다. M2b가 새로 만든 글자는 이 단계에서 한국어로 둔다(T8a에서 바꿈). `privacy.html`·`privacy.test.ts` 는 S1 쪽을 통째로 고른다(M2b 문장은 T8b에서 영어로 다시 넣음). (3) 충돌이 글자 수준을 넘으면(로직·구조가 서로 다름) 고치지 말고 막힘으로 보고한다(리서치 계획 리스크 7, 결정 8-1 (b)로 바꿀지 PM이 정함). (4) `git log -1 --format=%P` 가 해시 2개, `grep -rnE '^(<<<<<<<\|>>>>>>>)' web server` 출력 없음. (5) 웹 검사 4종과 `npm run e2e` 통과. (6) 보고: 충돌 파일 목록과 파일마다 고른 쪽, merge 뒤 S1 수치(기준선) |
| T1 | `StoryDate` 하한·시간대 (S1-P3이 (a)일 때만) | backend-dev | T0 | `c1-english-ui.md` T5 기준 그대로. S1-P3이 (b)면 이 작업은 없다 |
| T8a | 날짜 규칙·폼·오프닝 영어와 St. John's | frontend-dev | T0 | (1) 위 "웹 상수", "입력 화면 문구"(S-9 요약 제외), "오프닝 문장" 표와 같다. (2) 단위 시험: 요일 1988-02-13 토, 1976-07-17 토, 2010-02-12 금, 1980-04-12 토, 2000-02-29 화(윤일 통과), 1950-01-01 일(통과); 2001-02-29·1988-02-30 `DATE_NOT_EXIST`; 1949-12-31 `DATE_TOO_EARLY`; 가짜 시계로 St. John's 오늘 경계 — 표준시 `2026-01-15T03:29:59Z` 에 2026-01-15 `DATE_IN_FUTURE`, `03:30:00Z` 에 통과, 서머타임 `2026-07-15T02:29:59Z`/`02:30:00Z` 같은 방식; `yearOptions` 의 첫 해가 St. John's 기준 올해(`2027-01-01T03:29:59Z` → 2026, `03:30:00Z` → 2027); 시각 표의 네 경우; `Intl` en-CA 대조 1건. (3) `BirthDateField`: 월 선택지 글자 12개, 접근 이름 Year/Month/Day, 오류 연결(`aria-describedby`)은 그대로. (4) 기존 M2b 시험은 기대 글자·데모 값만 바뀌고 단언 개수가 같다. (5) 웹 검사 4종. **확인점 2**: 누적 S1이 460을 넘으면 알린다 |
| T8b | 시험 도우미·e2e·방침과 입력 화면 요약 | frontend-dev | T8a | (1) `web/src/test/storyInput.ts`·`web/e2e/network.ts` 의 라벨이 영어이고, 주석의 "데모 짝"이 결정 1 (b)를 가리킨다. (2) 위 "시험 날짜와 장소" 규칙을 지킨다. `grep -rnE '서울\|광주\|풍암동\|서울대학교병원\|1995-03-15' web/src web/e2e` 의 출력은 서버 자료를 옮긴 fixture 줄(`mocks/fixtures/`)과 그 대조 시험뿐이다(S2-P1 (a)면 1995-03-15는 0건). (3) e2e `input-flow`: 데모 4개로 오프닝 문장이 표와 같고 세계 섹션이 보이며 `page.url()` 이 처음과 같다. 키보드만으로 제출·포커스 이동. (4) 방침이 위 "방침" 절과 같고, S-9 요약이 동의 상자 앞에 있다. `privacy.test.ts` 가 S-1 문장의 "Only the date is sent to our server"와 "stay in your browser", YouTube 스크립트 문장을 확인하고, 동의 시험이 요약 두 줄과 그 위치(체크 상자보다 DOM 앞)를 확인한다. 시행일은 PM이 머지하는 날 한 줄 커밋. (5) `c1-english-ui.md` 검사 E1의 출력이 `world/types.ts` 의 `source_section` 줄뿐이다(임시 버튼이 없어졌으므로). (6) M2b 검사 C1·C3 출력 없음. (7) 웹 검사 4종과 `npm run e2e` |
| T9 | 제품 정의서 1차 수정 (결정 5 (b)) | PM | - | 정의서 1·2·3·8·9·12절과 `CLAUDE.md` 0절·7절 1·5항이 리서치 계획 결정 5 (b) 목록대로 바뀌어 `main` 에 있다. 12절에 데모 4개가 있다. S2 PR을 열기 전에 끝나야 한다 |
| T6 | 검증과 전체 시험 (다시) | test-engineer | T0, T8a, T8b (T1) | (1) T0·T8a·T8b(·T1) 기준 표와 M2b T6 (1)~(6)을 이 개정판 기준으로. (2) S1의 "화면 글자는 영어" 시험을 입력 화면까지 넓힌다: 빈 제출로 오류 문구가 모두 보이는 입력 화면과 오프닝이 있는 결과 화면에 한글이 없고, `/\blicense\b/i` 0건. (3) C3 확인: MSW가 받은 요청이 `/api/story/world?date=…` 하나뿐이고, 데모 4개의 장소 글자(`Montréal`, `St. John's` 포함)와 시각이 어떤 요청 주소에도 없다(방침 S-1과 요약 S-9가 사실임을 확인). 제품 코드에 `console.` 호출이 없다(`grep -rn 'console\.' web/src \| grep -vE '\.test\.tsx?:\|/test/'` 출력 없음). (4) 웹 검사 4종 2회, `npm run e2e` 2회, `npm run build`. 시간대 경계 시험 3회. T1이 있으면 `./gradlew check` 2회. (5) M2b T6 (5)의 대조표에 "merge 뒤 글자만 바뀜" 열을 더한다. (6) 검사 C1~C3, E1, S1 수치 |
| T7 | 화면 확인 (다시) | PM | T6, T9 | 휴대폰 크기에서 데모 4개 → 오프닝 문장이 표와 같고 세계 섹션이 나옴(S3a 전까지 사건은 한국어 위키백과에서 온다); 1988-02-30과 내일 날짜 거부 문구; 동의 없이 제출하면 동의 상자로 이동; 결과 화면 주소가 처음과 같음; 동의 상자 위 요약 두 줄 읽어 보기; 문구 검토 |

담당별: frontend-dev 3(T0, T8a, T8b), test-engineer 1, PM 2(T7, T9), backend-dev 0(S1-P3 (a)면 1). 합계 6개(또는 7개). ★ 없음 — 결정 10 (b).

검사 명령: M2b 계획의 C1·C2·C3·S1과 `c1-english-ui.md` 의 E1을 그대로 쓴다. S1은 `main...HEAD`(S1이 머지된 `main`) 기준이다.

## 크기 (400줄 규칙)

| 부분 | 셈 | 보정 |
|---|---|---|
| M2b T2~T5 (끝남, 실측) | +406 | merge 뒤 기준선은 T0에서 잰다(약 400~420 예상) |
| T8a (글자는 이미 센 줄을 바꾸므로 0, 달·요일·`a.m.` 표와 시간대 함수만 새 줄) | 약 22 | ×1.0~1.7 → 22~37 |
| T8b (방침 S-1·YouTube 문장, S-9 요약 2줄) | 약 12 | 12~20 |
| T1 (S1-P3 (a)일 때만) | 약 14 | ×2.5 → 약 35 |
| **합계** | | **S1-P3 (b): 약 435~470 / (a): 약 470~505** |

400을 넘는다. 끝난 M2b 작업만으로 406이라 더 나누려면 끝난 커밋을 다시 짜야 해서 비싸다(리서치 계획에서 약 420으로 받아들여짐). 줄일 수 있는 것은 서버 T1을 S1로 옮기는 것(S1-P3)과 방침 보강을 S1에 두는 것(S1-P4)뿐이다. 확인점: T0 뒤 기준선 보고, T8a 뒤 460 알림, 끝에 500을 넘으면 멈추고 PM에게 묻는다. 시험 변경은 S1에 넣지 않는다(M2b 시험 약 300~450줄에 T8a·T8b의 기대 글자 변경이 더해진다).

## 개인정보

- M2b의 C1~C5 그대로: 주소(경로·쿼리·해시)에 날짜·장소가 없고 `pushState`·`replaceState` 를 부르지 않는다. 브라우저 저장소·쿠키를 쓰지 않는다. 장소·시각은 어떤 요청에도 넣지 않고 날짜는 `GET /api/story/world` 에만 보낸다. `referrer` meta와 `autocomplete="off"` 그대로.
- 방침 S-1과 요약 S-9는 위 규칙을 사용자에게 그대로 알리는 문장이다. T6 (3)이 그 문장이 사실임을 시험으로 확인한다. C2에서 장소가 서버로 가게 되면 두 문장을 그 PR에서 고친다.
- 웹은 로그를 남기지 않는다(T6 (3) `console.` 없음). 서버는 장소를 받지 않으므로 생년월일과 장소를 함께 남기는 로그가 생길 수 없다.
- 시험 입력은 제품 정의서 12절 데모(T9 뒤 캐나다 4개), 공개 역사 사건 날짜, 경계값만 쓴다(`CLAUDE.md` 7절 6항, M3-P13).

## 리스크와 확인할 것

1. **merge 충돌의 크기**: S1-P1 (a)로 줄였지만 `privacy.html`·`privacy.test.ts`·`StoryScreen.tsx`·`ConsentNotice` 와 동의 시험·e2e 도우미는 충돌한다. 방침은 S1 쪽을 통째로 고르고 T8b에서 다시 쓴다. 글자 수준을 넘으면 T0에서 멈춘다.
2. **서머타임**: St. John's는 표준시 UTC−3:30, 서머타임 UTC−2:30이다. 웹 가짜 시계 시험이 두 경계를 모두 본다(T8a (2)). 서버(S1 T5)와 같은 순간을 쓴다.
3. **세계 섹션 내용과 데모 날짜가 어긋남**: e2e는 날짜와 상관없이 표본 fixture를 돌려준다. 캐나다 데모 날짜의 실제 사건은 S3a 뒤에야 확인할 수 있다. T7에서 PM이 알고 본다.
4. **T9 순서**: 정의서 12절이 바뀌기 전에는 캐나다 데모가 공식 데모가 아니다. 시험은 결정 1 (b)를 따라 미리 쓰되, PR은 T9 뒤에 연다.
5. **장소 글자 그대로 표시**: 아무 글자나 넣어도 결과가 나온다(M2b 리스크 6과 같음). 캐나다 밖 장소 안내는 C2.
6. **요약 문장의 출처 이름**: S-9는 "Wikimedia and YouTube"라고 쓴다. S3b·S4가 출처를 바꾸거나 늘리면 요약도 함께 고친다(S-11과 같은 규칙).

## PM 결정 필요

> **PM 결정 (2026-10-29, "모두 추천대로 승인")**: S2-P1 (a) 1995-03-15 → 1950-01-01 · S2-P2 (a) "… 1976 at 3:20 p.m., …". S1-P3 (b)이므로 `StoryDate` 는 S1 T5에서 한다.

- **S2-P1 옛 한국 데모 날짜 1995-03-15** (사건 없음 fixture와 시험 약 30곳에서 씀. T9 뒤에는 데모도, 공개 사건 날짜도 아니다)
  - (a) 경계값 1950-01-01로 옮긴다. 시험·mock만 바뀌고(약 30줄) `CLAUDE.md` 7절 6항에 그대로 맞다. 1988-09-17·2002-06-22 등 공개 사건 날짜는 그대로 둔다.
  - (b) S3a가 fixture를 새로 녹화할 때까지 예외로 허용한다(장소 이름 예외 "London" 등과 같은 방식).
  - (c) 모든 fixture 날짜를 캐나다 데모 날짜로 옮긴다. 서버 자료와 맞던 fixture가 어긋나고 시험 변경이 수백 줄 는다.
  - **추천: (a)**.
- **S2-P2 오프닝에서 시각 앞 낱말**
  - (a) "Saturday, July 17, 1976 at 3:20 p.m., Hochelaga-Maisonneuve, Montréal".
  - (b) 쉼표만: "Saturday, July 17, 1976, 3:20 p.m., Hochelaga-Maisonneuve, Montréal".
  - **추천: (a)**. 장소에도 쉼표가 있어 쉼표만 쓰면 어디까지가 시각인지 흐려진다.
