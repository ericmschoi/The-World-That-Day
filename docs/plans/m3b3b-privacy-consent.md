# 개인정보처리방침과 동의 단계 (M3b3b)

> 브랜치: `feat/privacy-consent` · 작성: planner · 상태: **PM 승인 (2026-10-15, 개정)** — 결정 1 **(a)** M3b3 리뷰 권장 1·2를 이 PR의 T0에서(`fix(media)` 커밋 따로), 결정 2 운영자 "The World That Day 운영자" · 문의 **(a)** 이 저장소의 GitHub Issues · 시행일 **(a)** 머지하는 날. 처음 승인 2026-10-12(M3b3 분할로 새로 생김, 로드맵 D1 (a)·D2 (a))
> 기준: `docs/research/video-sources.md` 3.2절 V2 카드 "서비스 전체 의무"(YouTube API 개발자 정책 III.A.1, III.A.2 (a)~(g), III.F.2.a), 2절 V2-10, 8절 위험 E·26, 10.1절 9번 (a)(의무는 M3b3에서), **23번 (b)(입력 화면 안에서 제출 전에 필수 동의 + 개인정보처리방침 링크, PM 2026-10-11)**, `CLAUDE.md` 7절 6항(생년월일과 장소를 함께 남기지 않음), 제품 정의서 8절, **머지된 `web/` 코드(`main` 72fd4ae)**
> 선행: **M3b2 (PR #13)·M3b2b (PR #15)·M3b3 (PR #16) 머지 — 이 브랜치는 `main` 72fd4ae 에서 갈라졌다.** 서버 변경 없음. 실제 서버 없이도 입력 화면·방침 페이지를 볼 수 있다.
> 뒤따르는 기능: M3b3c(YouTube 플레이어)는 이 기능의 `useConsent()` 가 `true` 일 때만 YouTube 영상을 그린다. M2b가 동의 구성 요소를 실제 입력 폼 제출 버튼 앞으로 옮긴다.

## 2026-10-15 바뀐 점

승인(2026-10-12)은 M3b2·M3b2b·M3b3 머지 전이었다. 머지된 `web/src`·`web/e2e` 를 읽고 맞췄다. 계획서와 코드가 다른 곳은 **코드를 따랐다**.

| 바뀐 것 | 이유 |
|---|---|
| **방침 페이지를 `web/public/privacy.html` → `web/privacy.html`(Vite의 두 번째 HTML 입구)로** | 디자인 토큰(M3b1)을 그대로 쓰려면 `web/src/styles/global.css` 를 불러와야 한다. `public/` 의 정적 파일은 Vite가 처리하지 않아 토큰을 못 쓴다. 두 번째 입구는 스크립트 없이 CSS만 불러오고, `vite.config.ts` 에 입구 한 줄만 더한다. 안 되면 되돌릴 길은 리스크 1 |
| **바닥글 링크는 `NewTabLink` 로** | 앱의 새 창 링크는 `web/src/components/NewTabLink.tsx` 하나뿐이다(주석: "The only `target="_blank"` link"). 그런데 지금은 `isHttpUrl` 이 아니면 **글자만** 낸다 → `/privacy.html` 은 링크가 안 된다. 서버 값(`source_url` 등)에 상대 경로가 와도 링크가 되지 않게, **`/privacy.html` 한 값만** 더 허용한다(일반 상대 경로 허용 아님) |
| **동의 상태 파일을 지금 코드 모양으로** | `playbackCoordinator.ts`(문맥·훅) + `PlaybackProvider.tsx`(공급자)와 같게 `consent/consent.ts` + `ConsentProvider.tsx`. 공급자는 `App` 에 둔다 — "처음으로"로 돌아와도 동의가 남고, 결과 화면(M3b3c)에서도 읽힌다. 구성 요소 파일에서 상수·훅을 같이 내보내면 `react-refresh` 규칙에 걸리므로 나눈다 |
| **기존 시험의 화면 이동에 동의 한 단계** | 데모·표본 버튼으로 결과 화면에 가는 시험이 5곳이다: `App.test.tsx`, `world/worldDemo.test.tsx`, `world/worldPhotosDemo.test.tsx`, `world/worldVideo.test.tsx`(표본 버튼), `e2e/network.ts` 의 `openDemo`(e2e 4개 파일이 모두 씀). 동의가 생기면 모두 막히므로 T2가 함께 고친다. 공용 도우미는 `web/src/test/consent.ts`(S1 제외 — PM 2026-10-14). 기대값은 바꾸지 않는다. 단 "works with the keyboard alone" 은 Tab 순서가 바뀌어 기대값이 바뀐다(T2 (8)) |
| **버튼 5개** | 지금 입력 화면은 데모 3개 + 표본 2개(M3b3). 다섯 개 모두 동의 전에는 결과로 가지 않는다 |
| **III.A.1 문장을 리서치 문구대로** | 이전 "영상을 재생하면 YouTube 이용약관에 동의하게 됩니다" → **"이 서비스를 이용하면 YouTube 이용약관에 동의하게 됩니다."** 리서치(V2-10, 3.2절)는 "**이용하면** YouTube 약관에 동의"다. "재생하면"은 범위를 좁힌 말이라 리서치 문구로 되돌렸다 |
| **방침 (e)를 지금 서버대로** | 머지된 서버가 실제로 부르는 곳은 한국어 위키백과 API·Wikidata SPARQL(`application.yml`)이고, Commons(M3a3b)·YouTube Data API(M3a5)는 M3 안에서 더해진다. 서버 접속 기록(access log) 설정은 없다. 서버 로그에 날짜를 짐작할 수 있는 위키백과 문서 제목이 남는다(로드맵 리스크 7). 방침은 이것을 그대로 적는다 |
| **"쿠키·저장소를 쓰지 않는다"를 명령으로 확인** | 방침 (g) 문장의 근거로 C8(`document.cookie`·`indexedDB`·`caches` 없음)을 더했다. C4(URL·`localStorage`·`sessionStorage`)는 그대로 |
| **방침의 운영자·문의·시행일 (결정 2)** | PM이 정했다: 운영자 "The World That Day 운영자", 문의 이 저장소의 GitHub Issues, 시행일 머지하는 날. T1에 글자 그대로 넣는다. 시행일 처리는 아래 "시행일 넣는 법" |
| **M3b3 리뷰 이월 권장 1·2 → T0 (결정 1 (a))** | 이 기능 범위 밖이지만 M3b3c가 같은 패턴을 YouTube 플레이어에 쓰기 전에 고친다. 커밋은 `fix(media): …` 로 따로. 권장 5(브라우저가 멈춘 것도 사용자 멈춤으로 봄)는 M6b — 기록만 |
| **크기 다시 셈** 약 165(보정 약 280) → **약 316, T0 포함 (웹 비율 ×1.14면 약 360)** | 아래 "크기". 400 아래라 한 PR, T1 뒤 확인점 |

## 왜 M2b가 아니라 지금인가 (로드맵 D2 (a), PM 결정 2026-10-12)

23번 결정은 "입력 화면 안에서 제출 전 필수 동의"다. 그런데 진행 순서는 M3 → M2라서 실제 입력 폼(M2b)보다 YouTube 영상(M3b3c)이 먼저 화면에 나온다. 10.1절 9번은 "의무가 갖춰지기 전에는 YouTube 영상을 보여 주지 않는다"고 정했다. 그래서 PM 결정(D2 (a))대로 동의 구성 요소와 방침을 지금 만들어 **임시 입력 화면(데모·표본 버튼) 안**에 두고, M2b가 같은 구성 요소를 실제 폼으로 옮긴다.

## 목표

1. **개인정보처리방침** 페이지를 만든다(III.A.2 (a)~(g) 내용, 한국어, 디자인 토큰). 입력 화면과 결과 화면 바닥글에서 늘 열 수 있다.
2. 입력 화면 안에 **필수 동의**를 둔다: 체크 상자 "개인정보처리방침에 동의합니다 (필수)" + 방침 링크 + YouTube 고지 문장 + YouTube 이용약관 링크(III.A.1). 체크하기 전에는 데모·표본 버튼 5개(뒤에는 M2b의 제출 버튼)가 결과 화면으로 가지 않는다.
3. 동의 상태를 앱 안에서만 들고 있는 `ConsentProvider`(메모리, 브라우저 저장소·URL 없음)를 만들고, M3b3c가 YouTube 영상을 그릴지 정하는 `useConsent(): boolean` 을 내보낸다.
4. M3b3 리뷰 권장 1·2를 T0으로 고친다(결정 1 (a)).

## 범위 밖

- YouTube 플레이어·캡션의 YouTube 출처 표시(III.F.2.a)·캡션 안 약관 링크 → M3b3c
- 실제 입력 폼·장소 입력·결과 URL → M2b. 장소 출처(카카오 로컬 등)의 방침 (e) 추가 → M2a
- 회원 가입·동의 기록 저장(서버에 아무것도 저장하지 않는다), 서버 변경
- 법률 자문 — 방침 문구는 frontend-dev가 아래 점검표로 초안을 쓰고 PM이 확정한다(T4). 공개 배포(#5) 전에 다시 본다
- M3b3 리뷰 권장 5(브라우저가 일으킨 멈춤, 예: 탭 숨김·전화가 사용자 멈춤으로 셈) → M6b. 기록만

## 사용할 외부 출처

없음(우리 코드가 부르는 외부 API·스크립트 없음). 방침과 동의 문장에 들어가는 외부 링크 3개(사용자가 누를 때만 브라우저가 연다):

| 링크 | 주소 | 근거 (확신도) |
|---|---|---|
| YouTube 이용약관 (III.A.1) | `https://www.youtube.com/t/terms` | `video-sources.md` V2-10, 3.2절 "서비스 전체 의무" — **확인됨 (문서, 2026-10-11 원문 다시 확인)** |
| Google 개인정보처리방침 (III.A.2 (c)) | `https://policies.google.com/privacy` | 같음 — **확인됨** |
| 문의 (결정 2 (a)) | `https://github.com/ericmschoi/The-World-That-Day/issues` | 우리 저장소(`CLAUDE.md` 0절 원격 주소). 외부 데이터 출처가 아니다 |

- 제품 코드의 외부 주소는 `web/src/external/urls.ts` 한 파일에만 둔다(C1 예외). M3b3c가 YouTube IFrame API 주소를 같은 파일에 더한다. `web/privacy.html` 은 HTML이라 이 파일을 못 읽으므로, 시험이 페이지 안 절대 주소 집합 = `urls.ts` 의 값 집합인지 대조한다(C7).

## 방침 내용 점검표 (III.A.2, 리서치 3.2절·V2-10)

사실대로 쓴다 — 구현과 다르면 구현이 아니라 문구를 고치고 보고한다.

| 항목 | 방침에 쓸 것 |
|---|---|
| (a) 늘 쉽게 볼 수 있음 | 입력·결과 화면 바닥글에 "개인정보처리방침" 링크, 동의 문장 안 링크 |
| (b) YouTube API 서비스 사용 고지 | "이 서비스는 YouTube API 서비스를 사용합니다." |
| (c) Google 개인정보처리방침 링크 | `https://policies.google.com/privacy` |
| (d) 받는 정보 | 입력한 생년월일(날짜)과 태어난 장소. 계정·이름·연락처는 받지 않는다. 지금 임시 화면은 날짜만 서버로 보낸다(장소는 M2b부터) — 문구는 둘 다 적고 M2b 계획 때 다시 확인 |
| (e) 쓰는 법·처리·공유 | 그날 자료를 찾는 데만 쓴다. 우리 서버는 날짜·사건 이름으로 외부 출처에 요청한다 — 지금 한국어 위키백과·Wikidata, M3 안에서 Wikimedia Commons·YouTube Data API가 더해진다. 누구의 생년월일인지는 보내지 않는다. 날짜별 결과(위키백과 문단 등)는 캐시하지만 요청한 사람 정보(IP 주소 등)는 저장하지 않는다. 서버 작업 기록(로그)에 날짜와 관련된 문서 제목이 남을 수 있지만 생년월일과 장소를 함께 남기지 않는다(`CLAUDE.md` 7절 6항). 장소 해석(M2a)이 생기면 그 출처를 더한다 |
| (f) 제3자 콘텐츠·광고 | 결과 화면의 사진·영상은 원 출처 서버(Wikimedia Commons 등)에서 브라우저가 직접 받는다 — 그 서버는 접속 정보(IP 주소 등)를 받는다. YouTube 플레이어(개인정보 보호 강화 모드)를 페이지에 넣는다. YouTube가 플레이어 안에 광고를 낼 수 있다 |
| (g) 기기 저장·쿠키 | 우리는 쿠키·브라우저 저장소를 쓰지 않는다(C4·C8). YouTube 플레이어와 IFrame API 스크립트는 기기에 정보를 저장하거나 읽을 수 있다(개인정보 보호 강화 모드도 "쿠키를 쓰지 않는다"고 하지 않는다 — 리서치 [104]) |
| III.A.1 | "이 서비스를 이용하면 YouTube 이용약관에 동의하게 됩니다." + 약관 링크 (동의 문장과 방침 안) |
| 결과 화면 주소 | 결과 화면 주소에 생년월일·장소를 넣지 않는다(리서치 위험 26, M3b2 C4) |
| 운영자 (결정 2) | **"The World That Day 운영자"** — 글자 그대로. 실명·다른 이름을 더하지 않는다 |
| 문의 (결정 2 (a)) | **"이 방침이나 개인정보에 관한 문의는 GitHub Issues(`https://github.com/ericmschoi/The-World-That-Day/issues`)로 받습니다."** — 링크 글자는 "GitHub Issues", 주소는 위 그대로. 이메일 주소를 넣지 않는다 |
| 시행일·변경 (결정 2 (a)) | 시행일은 **머지하는 날**. 넣는 법은 아래 "시행일 넣는 법". 방침이 바뀌면 시행일을 고친다는 문장도 넣는다 |

(h)(i)는 OAuth(Authorized Data)를 쓸 때만이라 해당 없음(리서치 V2-10).

### 시행일 넣는 법 (결정 2 (a) "머지하는 날")

머지하는 날은 PM이 머지할 때에야 정해진다. 그래서 가장 단순하고 거짓이 없는 방법으로 한다.

1. T1에서 frontend-dev는 시행일 줄을 **`시행일: 머지하는 날 넣음`** 으로 쓴다(날짜를 미리 지어 넣지 않는다). 시험은 "시행일" 낱말만 보고 날짜 모양은 보지 않는다.
2. T4에서 PM이 문구를 승인하고 **"오늘 머지한다"** 고 오케스트레이터에게 알리면, 오케스트레이터가 frontend-dev에게 한 줄 커밋을 맡긴다: `시행일: YYYY년 M월 D일`(그날 날짜, 예 `2026년 10월 16일`). 제품 파일이라 일을 한 에이전트가 커밋한다(`CLAUDE.md` 4절). 커밋 예 `docs(privacy): set the policy effective date`.
3. 오케스트레이터가 푸시하고, PM은 **같은 날** 머지한다. 머지가 다음 날로 밀리면 같은 한 줄 커밋을 한 번 더 한다.
4. 머지 전 확인: `grep -n '머지하는 날 넣음' web/privacy.html` 출력 없음.

## API / 데이터 변경

서버 API·`data/` 는 바뀌지 않는다. 웹 안에서 바뀌는 것만 적는다.

### 부품과 연결 (머지된 코드 기준)

| 파일 | 지금 | 이 기능 뒤 |
|---|---|---|
| `web/privacy.html` (새로) | - | 정적 방침 페이지. `<html lang="ko">`, `<h1>개인정보처리방침</h1>`, 점검표 항목. `<link rel="stylesheet" href="/src/privacy/privacy.css">` 하나. **`<script>` 없음**, 외부 글꼴·이미지 없음. 외부 링크는 같은 탭(이 페이지에서 잃을 것이 없음) |
| `web/src/privacy/privacy.css` (새로) | - | `@import '../styles/global.css'` + 본문 폭(`--content-max-width`)·여백. 색·글자 크기는 토큰만(`cssRules.test.ts` 가 `/src/**/*.css` 로 자동 검사) |
| `web/vite.config.ts` | 입구 `index.html` 하나 | 빌드 입구에 `privacy.html` 을 더한다(설치된 Vite 8 문서의 여러 페이지 설정대로). 개발 서버는 설정 없이 `/privacy.html` 을 준다 |
| `web/src/external/urls.ts` (새로) | - | `YOUTUBE_TERMS_URL`, `GOOGLE_PRIVACY_URL`, `CONTACT_ISSUES_URL` |
| `web/src/consent/consent.ts` (새로) | - | `PRIVACY_PAGE = '/privacy.html'`, 동의 문맥(`{ agreed, setAgreed }`), **`useConsent(): boolean`** — 공급자 밖이면 `false`(M3b3c의 "입력 화면을 거치지 않는 경로" 방어) |
| `web/src/consent/ConsentProvider.tsx` (새로) | - | `useState(false)` 하나. 저장소·URL에 쓰지 않음 |
| `web/src/consent/ConsentNotice.tsx` (새로) + `Consent.module.css` | - | 체크 상자·방침 링크·YouTube 고지·약관 링크·안내 문구. 체크 상자 `ref` 를 받는다(React 19의 `ref` 속성) — M2b가 그대로 옮긴다 |
| `web/src/components/NewTabLink.tsx` | `isHttpUrl` 일 때만 링크 | + `href === PRIVACY_PAGE` 일 때도 링크. 그 밖의 상대 경로·`//…`·`javascript:` 는 지금처럼 글자만 |
| `web/src/components/SiteFooter.tsx` (새로) | - | `<footer>` 안에 `NewTabLink` "개인정보처리방침" 하나 |
| `web/src/App.tsx`, `App.module.css` | `<main>` 하나 | `ConsentProvider` 로 감싸고 `<main>` 다음에 `SiteFooter`(두 화면 공통). `.footer` 는 `.main` 과 같은 폭 |
| `web/src/screens/InputScreen.tsx`, `Screen.module.css` | 버튼 5개, 누르면 `onShowStory` | 버튼 위에 `ConsentNotice`. 동의 전: 버튼 5개 `aria-disabled="true"`(`disabled` 아님 — 포커스·클릭이 되어 안내를 들을 수 있음), 누르면 `onShowStory` 를 부르지 않고 안내 문구 + 체크 상자로 포커스. `.button[aria-disabled='true']` 모양은 토큰만(바탕 `--color-surface`, 글자 `--color-muted`, 테두리 `--color-border` — `opacity` 로 흐리게 하지 않음, 대비는 `tokens.test.ts` 의 기존 쌍) |
| `web/src/screens/StoryScreen.tsx` | - | **바뀌지 않음**(바닥글은 `App` 에서) |
| `web/src/test/consent.ts` (새로, S1 제외) | - | 시험 도우미: 체크 상자를 눌러 동의 |
| `web/e2e/network.ts` | `openDemo` = 주소 열기 → 버튼 | 주소 열기 → **체크 상자 체크** → 버튼 |

### T0 (결정 1 (a)) — 재생 규칙 보강

| 파일 | 지금 | 뒤 |
|---|---|---|
| `web/src/media/useInView.ts` | 보고마다 `setSeen` 하지만 돌려주는 값은 불리언 → "안 보임"이 두 번 연속이면 쓰는 쪽 효과가 다시 돌지 않는다 | "안 보임" **보고마다** 쓰는 쪽이 알 수 있게 한다(보고 콜백 인자 또는 보고마다 바뀌는 값 — 방식은 frontend-dev가 고르고 보고). `[ref, inView]` 의 지금 뜻은 그대로(M3b3c가 씀) |
| `web/src/media/VideoFilePlayer.tsx` | `inView` 가 바뀔 때만 `release` | **"안 보임" 보고마다 `coordinator.release(player)`**. "보임"은 지금처럼 바뀔 때만 `requestPlay` |
| 같은 파일 (권장 2) | `ourPause: boolean` | 우리가 부른 `pause()` 수를 세는 **카운터**. `pause` 이벤트가 오면 0보다 클 때 1 줄이고(우리 것), 0이면 사용자 멈춤 |

- 권장 1 증상(M3b3 리뷰): 절반 이하로 보일 때 사용자가 직접 튼 영상은 이미 `inView === false` 라, 화면 밖으로 완전히 나가도 효과가 다시 돌지 않아 계속 재생된다(켰다면 소리까지). 다른 영상이 재생될 때까지.
- 권장 2 증상: 우리 `pause()` → `play()` → `pause()` 가 이벤트보다 먼저 일어나면(두 영상이 한 프레임에 바뀔 때) 두 번째 `pause` 이벤트가 사용자 멈춤으로 읽혀 그 영상이 다시 자동 재생되지 않는다.
- 조정(`playbackCoordinator`) 인터페이스는 바뀌지 않는다. `release` 는 그 재생자가 "지금 재생 중"일 때만 멈추므로, 다른 영상이 재생 중일 때의 "안 보임" 보고는 아무것도 하지 않는다.

## 화면

```
[입력 화면 — 임시]
  The World That Day                                   (h1)
  생년월일과 태어난 장소를 입력하는 화면이 곧 이곳에 생깁니다.
  ┌──────────────────────────────────────────────┐
  │ ☐ 개인정보처리방침에 동의합니다 (필수)           │  ← <input type="checkbox" required>
  │   개인정보처리방침 보기 (새 창에서 열림)          │
  │   이 서비스는 YouTube API 서비스를 사용합니다.   │
  │   이 서비스를 이용하면 YouTube 이용약관           │
  │   (새 창에서 열림)에 동의하게 됩니다.             │
  │   [안내] 개인정보처리방침에 동의해야 볼 수 있습니다. │  ← 동의 없이 버튼을 눌렀을 때만
  └──────────────────────────────────────────────┘
  [1988년 9월 17일로 보기] … [표본: 1986년 9월 20일로 보기]   ← 동의 전 aria-disabled
바닥글: 개인정보처리방침 (새 창에서 열림)               ← 입력·결과 화면 모두, <main> 밖
```

- Tab 순서: 체크 상자 → 방침 링크 → 약관 링크 → 버튼 5개 → 바닥글 링크.
- 안내 문구는 체크 상자의 접근 설명(`aria-describedby`)으로 이어서, 포커스가 옮겨질 때 화면 읽기 프로그램이 읽는다(`role="alert"` 로 두 번 읽히지 않게). 체크하면 사라진다.
- 체크 상자의 접근 이름은 "개인정보처리방침에 동의합니다 (필수)"이고 `required` 속성이 있다(M2b의 `<form>` 에 들어가면 브라우저 검사도 같이 된다). 체크 상자에 `:focus-visible` 포커스 링(`--color-focus`).
- 방침 페이지는 새 창에서 열려 화면 상태가 사라지지 않는다. 결과 화면은 주소에 없으므로(M3b2 C4) 같은 탭으로 가면 결과를 잃는다 — 그래서 새 창이다.
- 동의 상태는 메모리에만 둔다. "처음으로"로 돌아오면 체크가 남아 있고, 새로 고치면 다시 동의한다(브라우저 저장소 없음 — M3b2 C4).

## 데모·표본 날짜로 확인할 것

| 날짜 / 경우 | 자동 테스트 (fixture) | PM 확인 — T4 (PR 검수, 실제 서버 없어도 됨) |
|---|---|---|
| 1988-09-17, 동의 전 | 버튼을 눌러도 결과 화면 없음(`region` 0개), `/api/story/world` 요청 0건, 안내 문구, 체크 상자에 포커스. Enter 키도 같음 | 같은 동작, 안내 문구가 읽기 쉬운지 |
| 1988-09-17, 동의 뒤 | 지금과 같은 결과(사건 3개 — 기존 시험 그대로 통과). 결과 화면 바닥글에 방침 링크 | 바닥글 링크로 방침이 새 창에서 열림 |
| 2002-06-22, 1995-03-15 | 동의 뒤 지금과 같은 결과(기존 시험 그대로) | - |
| 표본 1969-07-20, 1986-09-20 | 동의 전 막힘, 동의 뒤 각각 그 날짜로 요청(기존 시험 그대로) | - |
| "처음으로" → 다시 입력 | 체크가 남아 있고 바로 결과로 갈 수 있음 | - |
| 새로 고침 (e2e) | 체크가 풀려 있음, `localStorage`·`sessionStorage` 0개, `document.cookie` 빈 값 | - |
| 방침 페이지 (e2e) | 360px 가로 스크롤 없음, 응답을 받은 밖 요청 0건, 페이지 안 스크립트는 우리 개발 서버 것만 | 문구 전체(T4) |
| (T0) `world1988WithVideo` 사건 ① 영상 | jsdom 가짜 관찰자 + e2e: 절반 이하로 보일 때 사용자가 틀고 화면 밖으로 완전히 나가면 멈춤(소리를 켰어도) | - |

## 크기 (400줄 규칙)

### 웹 실측 비율

| 기능 | 예상 → 실제 (S1) | 비율 |
|---|---|---|
| M3b2 | 365 → 434 | 약 1.19배 |
| M3b2b | 약 315 → 319 | 약 1.01배 |
| M3b3 | 약 300 → 343 (`web/src/test/` 제외 규칙) | 약 1.14배 |

파일별로 다시 세고(Prettier 80칸, 지금 코드의 주석 밀도 — 한국어 HTML 문단은 줄바꿈이 많다) ×1.0~×1.2를 범위로, 가운데 ×1.14를 쓴다.

### 다시 센 값

| 작업 | 내용 | 다시 셈 | ×1.14 (범위 ×1.0~×1.2) |
|---|---|---|---|
| T0 | `useInView` 약 6, `VideoFilePlayer` 약 10(보고마다 해제, 카운터) | 약 16 | 약 18 (16~19) |
| T1 | `privacy.html` 약 90, `privacy.css` 약 20, `vite.config.ts` 약 6, `urls.ts` 약 7, `NewTabLink` 약 5, `SiteFooter` 약 14, `App.tsx`·`App.module.css` 약 16 | 약 158 | 약 180 (158~190) |
| T2 | `consent.ts` 약 22, `ConsentProvider` 약 14, `ConsentNotice` 약 45, `Consent.module.css` 약 25, `InputScreen` 약 25, `Screen.module.css` 약 8, `App.tsx` 약 4 | 약 143 | 약 163 (143~172) |
| **합계** | | **약 317** | **약 361** (317~380) |

- 400 아래라 **한 PR**로 한다. 리뷰 수정(M3b2 +30)까지 넣으면 400 안팎이다.
- **확인점 (안전장치)**: T0~T1 누적 S1이 **210**을 넘으면(다시 셈 174 × 1.2) 오케스트레이터가 T2 전에 멈추고, T2(동의 단계)를 별도 PR로 뺄지 PM에게 묻는다. 넘지 않으면 그대로 T2로 간다. 방침 HTML의 줄 수가 가장 불확실하다.
- S1에서 빼는 것: 테스트(`*.test.ts(x)`), fixture(`web/src/mocks/fixtures/**`), **시험 지원 코드 `web/src/test/**`(PM 결정 2026-10-14)**, e2e(`web/e2e/**`). `.github` 는 넣는다.

## 테스트 방침

M1b·M3b2·M3b2b·M3b3 "테스트 방침"을 따른다.

- **Vitest (jsdom)**: 방침 페이지는 `web/privacy.html` 을 글자로 읽어(`?raw` 가져오기 또는 `node:fs`) 필수 낱말·절대 주소 집합·`<script` 없음을 본다. 외부 링크는 누르지 않고 `href` 만 본다. MSW `onUnhandledRequest: 'error'` 그대로 — 동의 전 버튼은 요청을 보내지 않아야 하므로 요청 기록(`server.events` 또는 `server.use` 핸들러 안 기록)으로 0건을 본다. 날짜는 데모 3개·표본 2개·`world1988WithVideo` 의 합성 값만.
- **Playwright (실제 Chromium)**: `interceptNetwork` 그대로(밖 요청 `abort`, 응답받은 밖 요청 0건). 바닥글 링크로 열린 새 탭도 문맥에 걸려 막힌다. 방침 페이지는 `localhost` 라 `route.continue()` 로 개발 서버가 준다. 개발 서버는 HTML에 자기 스크립트(`/@vite/client`)를 넣으므로 "스크립트 없음"은 원본 파일(Vitest)과 빌드 결과(B1)로 보고, e2e는 "밖 스크립트 없음"을 본다.
- 실제 사람의 생년월일 없음. 어떤 서비스에도 가입하거나 약관에 동의하지 않는다(에이전트). 동의 체크는 우리 화면의 체크 상자일 뿐이다. GitHub Issues 주소는 `href` 만 보고 열지 않는다.

## 작업 목록

순서: T0 → T1 → **확인점 (누적 S1 210)** → T2 → T3 → T4(PR 검수 때) → 시행일 한 줄 커밋 → 머지. T0은 `web/src/media/` 만, T1·T2는 그 밖이라 겹치지 않는다(T0을 먼저 끝내고 같은 브랜치에서 이어 간다 — 병렬 worktree 불필요).

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T0 | M3b3 리뷰 권장 1 — "안 보임" 보고마다 해제, 권장 2 — `ourPause` 카운터 (결정 1 (a), 커밋 `fix(media): …` 따로) | frontend-dev | `web/src/media/useInView.ts`, `VideoFilePlayer.tsx`, 테스트 | M3b3 머지(완료) | **시험을 먼저 쓰고 지금 코드에서 실패하는 것을 보고에 적는다.** (1) `mediaFakes.ts` 가짜 관찰자로: 비율 0.3 보고 → 사용자 `play` 이벤트(직접 재생, `notifyPlaying`) → 비율 0 보고 → 그 영상의 `pause()` 1회, `paused === true`. (2) (1)에서 사용자가 소리를 켠 뒤(`volumechange`, `muted: false`)에도 같다. (3) 그렇게 멈춘 영상은 사용자 멈춤이 아니다: 다시 0.51 → `play()` 1회(소리를 켰다면 `muted` 가 `false` 그대로 — M3b3 규칙). (4) 다른 영상 B가 재생 중일 때 A의 "안 보임" 보고가 연달아 와도 B의 `pause()` 0회. (5) "보임" 보고가 연달아 두 번 와도 `play()` 1회(보임은 바뀔 때만). (6) (권장 2) 이벤트를 보내기 전에 우리 `pause()` → `play()` → `pause()` 를 부르고 `pause` 이벤트 2개를 보낸 뒤 0.5 → 0.51 이면 `play()` 가 불린다. 그 뒤 진짜 사용자 멈춤(우리가 부르지 않은 `pause` 이벤트)은 지금처럼 자동 재생을 막는다. (7) 기존 `useInView`·`VideoFilePlayer`·`PlaybackProvider`·`StoryScreenPlayback`·조정 시험은 고치지 않고 통과. `useInView` 의 반환 모양을 바꾸면 쓰는 곳 전부를 함께 바꾸고 보고에 적는다. (8) C1·C5 출력 없음, C3 `0`, 웹 전체 검사(`lint`·`format:check`·`typecheck`·`test`) 통과, `npm run e2e` 통과(기존 28개), 보고에 T0 S1 |
| T1 | 방침 페이지 초안, 외부 주소 파일, 바닥글, `NewTabLink` 의 방침 경로 | frontend-dev | `web/privacy.html`, `web/src/privacy/privacy.css`, `web/vite.config.ts`, `web/src/external/urls.ts`, `web/src/consent/consent.ts`(이 작업에서는 `PRIVACY_PAGE` 만), `web/src/components/NewTabLink.tsx`, `web/src/components/SiteFooter.tsx`, `web/src/App.tsx`·`App.module.css`, 테스트 | T0 | (1) 방침이 위 점검표의 모든 행을 담는다 — 점검 시험: "YouTube API 서비스", `GOOGLE_PRIVACY_URL`, "생년월일", "장소", "쿠키", "광고", "개인정보 보호 강화 모드", "YouTube 이용약관"과 `YOUTUBE_TERMS_URL`, "이 서비스를 이용하면 YouTube 이용약관에 동의하게 됩니다", "결과 화면 주소", "로그", "위키백과", "Wikidata", "Wikimedia Commons", "YouTube Data API", "시행일", "문의". `<html lang="ko">`, `h1` "개인정보처리방침". (2) **PM이 정한 사실을 글자 그대로**(결정 2): 운영자 **"The World That Day 운영자"**, 문의 문장 **"이 방침이나 개인정보에 관한 문의는 GitHub Issues로 받습니다."** — "GitHub Issues" 가 `https://github.com/ericmschoi/The-World-That-Day/issues` 링크, 시행일 줄 **"시행일: 머지하는 날 넣음"**(위 "시행일 넣는 법" — 날짜를 지어 넣지 않음). 시험이 세 문장이 있는지 본다. 이메일 주소·실명·다른 연락처 없음. (3) **C7**: 페이지 안 절대 주소 집합이 `urls.ts` 가 내보내는 값 집합(3개)과 같다(시험). (4) 원본에 `<script` 없음, 스타일시트는 `/src/privacy/privacy.css` 하나, 외부 글꼴·이미지 없음. **B1**: `npm run build` 뒤 `web/dist/privacy.html` 이 있고 `<script` 가 없으며 CSS가 연결돼 있다. (5) `privacy.css` 는 `cssRules.test.ts` 통과(토큰만, 절대 주소 없음, 쓰는 `var(--…)` 는 모두 정의됨). (6) 입력 화면과 결과 화면 모두 `contentinfo` 랜드마크가 `main` 밖에 하나 있고, 그 안의 링크는 "개인정보처리방침" 하나: `href="/privacy.html"`, `target="_blank"`, `rel="noopener noreferrer"`, 숨은 "(새 창에서 열림)". (7) `NewTabLink`: `/privacy.html` → 링크. `/other.html`, `/api/story/world`, `//evil.example/x`, `javascript:alert(1)` → 글자만(링크 없음). 기존 `http(s)` 동작 시험은 고치지 않고 통과. (8) `urls.ts` 는 `YOUTUBE_TERMS_URL = 'https://www.youtube.com/t/terms'`, `GOOGLE_PRIVACY_URL = 'https://policies.google.com/privacy'`, `CONTACT_ISSUES_URL = 'https://github.com/ericmschoi/The-World-That-Day/issues'`. (9) 방침 문장이 실제 구현과 다르면(예: 서버가 부르는 곳) 문구를 고치고 보고. (10) C1(새 명령)·C5·C6 출력 없음, C3 `0`, 웹 전체 검사 통과, `npm run e2e` 통과, 보고에 **T0~T1 누적 S1 — 확인점 210** (넘으면 오케스트레이터가 T2 전에 멈추고 PM에게 묻는다) |
| T2 | 동의 구성 요소, `ConsentProvider`, 입력 화면 연결, 기존 시험의 화면 이동 | frontend-dev | `web/src/consent/consent.ts`, `ConsentProvider.tsx`, `ConsentNotice.tsx`, `Consent.module.css`, `web/src/screens/InputScreen.tsx`·`Screen.module.css`, `web/src/App.tsx`, `web/src/test/consent.ts`, `web/e2e/network.ts`(`openDemo`), 기존 시험 4개 파일의 동의 단계, 테스트 | T1 (확인점 통과) | (1) 첫 화면: 체크 상자 체크 안 됨, 접근 이름 "개인정보처리방침에 동의합니다 (필수)", `required`. 버튼 5개 모두 `aria-disabled="true"` 이고 `disabled` 속성은 없다. (2) 동의 전 버튼 5개 각각: 눌러도 `region` 0개, `/api/story/world` 요청 0건, "개인정보처리방침에 동의해야 볼 수 있습니다."가 보이고 체크 상자에 포커스, 그 문구가 체크 상자의 접근 설명에 들어 있다. 버튼에 포커스를 두고 Enter도 같다. (3) 체크 뒤: 안내 문구가 사라지고 버튼의 `aria-disabled` 가 없거나 `"false"`, 누르면 지금처럼 결과 화면과 그 날짜의 요청. (4) 체크를 풀면 (1)의 상태로 돌아간다. (5) 동의 문장: "개인정보처리방침 보기" 링크(`href="/privacy.html"`, 새 창, 숨은 안내), "이 서비스는 YouTube API 서비스를 사용합니다.", "이 서비스를 이용하면 YouTube 이용약관에 동의하게 됩니다." — "YouTube 이용약관" 링크 `href` = `YOUTUBE_TERMS_URL`, 새 창, 숨은 안내. 동의 문장에는 운영자·문의·시행일을 넣지 않는다(방침 페이지에만 — 결정 2의 세 사실은 T1 문구 그대로). (6) `useConsent()`: 공급자 밖 `false`, 공급자 안 처음 `false`, 체크 뒤 `true`, 풀면 `false`(확인용 구성 요소로 시험). "처음으로"로 돌아오면 체크가 남아 있고 바로 결과로 갈 수 있다. (7) 동의 뒤 결과 화면까지 간 뒤 `localStorage.length`·`sessionStorage.length` 가 0, `document.cookie` 가 빈 값, 주소(`location.search`·`hash`)가 비어 있다. C4·C8 출력 없음. (8) 기존 시험 `App.test.tsx`·`worldDemo.test.tsx`·`worldPhotosDemo.test.tsx`·`worldVideo.test.tsx` 는 버튼을 누르기 전에 `web/src/test/consent.ts` 로 동의하는 단계만 더한다 — **기대값은 바꾸지 않는다.** 예외: "works with the keyboard alone" 은 Tab 순서(체크 상자 → 방침 링크 → 약관 링크 → 1988 버튼)와 Space로 체크하는 단계로 바뀐다. 지운 시험 없음. 바꾼 시험은 보고에 표로(파일·시험 이름·바뀐 줄). "offers the three demo dates…" 의 버튼 목록 기대값(5개)은 그대로 통과(체크 상자·링크는 버튼이 아님). (9) `e2e/network.ts` 의 `openDemo` 가 주소를 연 뒤 체크 상자를 체크하고 버튼을 누른다. 기존 e2e 28개가 고치지 않고 통과(`npm run e2e`). (10) CSS: 체크 상자 `:focus-visible` 링이 `--color-focus`·`--focus-ring-width`, `aria-disabled` 버튼 모양은 토큰만이고 `opacity` 없음, `cssRules.test.ts` 통과. (11) C1·C5·C6 출력 없음, C3 `0`, 웹 전체 검사 통과, 보고에 누적 S1 |
| T3 | 완료 기준 검증, 실제 브라우저 확인(e2e), 전체 테스트 | test-engineer | 빠진 기준의 테스트, `web/e2e/consent.spec.ts`, `web/e2e/video-playback.spec.ts` 에 T0 시험 추가 | T2 | (1) T0~T2 기준 표, 빠진 시험 추가. (2) e2e `consent.spec.ts`(360·1280 두 프로젝트, 재시도 0): (a) 첫 화면에서 1988 버튼을 누르면 결과 화면이 열리지 않고 안내 문구가 보이며 체크 상자에 포커스, `/api/story/world` 요청 0건. (b) 체크 뒤 누르면 결과 화면. (c) 입력 화면과 결과 화면 바닥글 링크를 누르면 새 탭이 `/privacy.html` 로 열리고 `h1` "개인정보처리방침", "The World That Day 운영자"와 GitHub Issues 링크(`href` 만 확인, 누르지 않음)가 보이며, 그 탭에서 응답받은 밖 요청 0건, `script[src]` 는 모두 `localhost`, 360px에서 가로 스크롤 없음. (d) "처음으로" 뒤 체크가 남아 있고, 새로 고치면 풀려 있다. (e) 동의·결과 화면 뒤 `localStorage`·`sessionStorage` 0개, `document.cookie` 빈 값. (f) 모든 시험에서 `pageerror` 0건. (3) T0: `world1988WithVideo` 영상 요청 `hold`: 사건 ① 영상을 보이는 비율 0.05~0.5 사이에 두고 페이지에서 그 영상을 재생(사용자 재생 흉내) → 영상이 화면 밖(비율 0)으로 나가게 스크롤하면 `pause` 가 기록되고 `paused === true`. `muted = false` 로 소리를 켠 경우도 같다. (4) **B1** 빌드 확인. (5) `npm test` 3회, `npm run e2e` 3회 반복 안정(기존 28개 포함). (6) C1·C2·C4·C5·C6·C7·C8 출력 없음(C7은 시험 통과), C3 `0`. (7) "데모·표본" 표 자동 테스트 열의 행마다 덮은 시험을 표로. (8) S1·S2 보고 |
| T4 | 방침·동의 문구 확정과 시행일 (PR 검수 때) | PM | - | T3 | (1) `cd web && npm run dev` 로 입력 화면(서버 없어도 됨): 체크 전 버튼·안내 문구·포커스, 체크 뒤 결과 화면(서버를 띄웠으면 실제 사건, 아니면 "이 섹션을 불러오지 못했습니다" — 둘 다 정상), 바닥글 링크로 방침이 새 창에서 열림. (2) `web/privacy.html` 과 동의 문장을 읽고 고칠 곳을 PR 코멘트로. 운영자·문의 문장은 결정 2대로 들어가 있는지 본다. (3) 문구가 확정되고 머지할 날 PM이 "오늘 머지한다"고 알리면, 오케스트레이터가 frontend-dev에게 시행일 한 줄 커밋(`시행일: YYYY년 M월 D일`, 그날)을 맡기고 푸시한다. PM은 같은 날 머지한다(밀리면 한 줄 커밋을 다시). (4) 머지 전 `grep -n '머지하는 날 넣음' web/privacy.html` 출력 없음. (5) 이 문구는 법률 자문을 거친 것이 아니다 — 공개 배포(#5) 전에 다시 본다 |

완료 기준에 쓰는 명령 (저장소 루트). C2·C3·C4·C5·C6은 `docs/plans/m3b2-world-events-ui.md` 의 같은 이름 명령을 그대로 쓴다. E1은 `docs/plans/m3b2b-world-photos-a11y.md` 의 E1 그대로. C1·S1·S2는 아래로 바꿔 쓴다.

```
# C1 — 제품 코드에 외부 절대 URL 없음 (테스트·목·외부 주소 파일 제외). 출력 없음
grep -rnE 'https?://' web/src web/index.html --exclude='*.test.ts' --exclude='*.test.tsx' \
  | grep -v '^web/src/mocks/' | grep -v '^web/src/external/urls.ts:'
# C7 — 방침 페이지의 절대 주소 (시험이 urls.ts 값과 대조. 사람이 볼 때 — 3개)
grep -oE 'https?://[^"<> ]+' web/privacy.html | sort -u
# C8 — 쿠키·그 밖의 브라우저 저장소 안 씀 (방침 (g)의 근거). 출력 없음
grep -rnE 'document\.cookie|indexedDB|caches\.' web/src --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/test/'
# B1 — 빌드한 방침 페이지에 스크립트 없음. "ok"
cd web && npm run build && test -f dist/privacy.html && ! grep -q '<script' dist/privacy.html && grep -q 'stylesheet' dist/privacy.html && echo ok
# D1 — 머지 전 시행일이 채워졌는지. 출력 없음
grep -n '머지하는 날 넣음' web/privacy.html
# S1 — 제품 코드 줄 수 (테스트·fixture·시험 지원 코드·e2e 제외, .github 포함)
git diff --numstat main...HEAD -- web .github \
  ':(exclude)web/package-lock.json' \
  ':(exclude,glob)web/src/**/*.test.ts' ':(exclude,glob)web/src/**/*.test.tsx' \
  ':(exclude,glob)web/src/mocks/fixtures/**' ':(exclude,glob)web/src/test/**' \
  ':(exclude,glob)web/e2e/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수 (시험 지원 코드·e2e 포함)
git diff --numstat main...HEAD -- ':(glob)web/src/**/*.test.ts' ':(glob)web/src/**/*.test.tsx' \
  ':(glob)web/src/test/**' ':(glob)web/e2e/**' \
  | awk '{a+=$1} END {print "test added", a}'
```

담당별: frontend-dev 3 (T0, T1, T2 — 그리고 T4 뒤 시행일 한 줄 커밋), test-engineer 1 (T3), PM 1 (T4).

## 리스크와 확인할 것

1. **Vite 두 번째 입구** — 빌드가 `privacy.html` 에 스크립트를 넣거나 CSS를 연결하지 못하면(B1 실패) frontend-dev가 멈추고 보고한다. 대안: `web/public/privacy.html` 정적 파일(토큰 대신 색 없는 기본 모양, `cssRules` 밖) — 처음 승인안. 이 경우 PM에게 모양 차이를 알린다.
2. **임시 화면에 붙는 동의** — 데모 버튼 화면에 붙였다가 M2b가 옮긴다. `ConsentNotice`(체크 상자 `ref` 를 받음)와 `useConsent` 로 나눠 옮기기만 하면 되게 한다(M2b 범위). 버튼을 막는 판단은 `InputScreen` 에 있어 M2b가 제출 처리로 옮긴다.
3. **"기능을 쓰기 전 동의"의 해석**(23번 †) — 입력 제출 전 명시적 동의로 읽는다. 법률 자문이 아니다.
4. **방침이 아직 머지되지 않은 출처를 적는다** — Commons(M3a3b)·YouTube Data API(M3a5)는 이 PR 머지 시점에 서버에 없을 수 있다. 방침은 "M3 안에서 더해진다"로 사실대로 적고, 공개 배포 전이라 로컬에서만 보인다. M2a(장소 출처)·M3b3c(nocookie가 안 되면 `www.youtube.com` — 로드맵 후속)·M4(지도 SDK)·M6b 계획 때 방침을 고칠지 확인 항목으로 넣는다. 방침을 고치면 시행일도 같은 방법(머지하는 날 한 줄 커밋)으로 고친다.
5. **IFrame API 스크립트가 우리 페이지에서 돈다** — 같은 페이지의 DOM·주소를 읽을 수 있다. 결과 주소에 생년월일·장소를 넣지 않고(위험 26, C4), 방침 (g)에 밝힌다. 화면에 보이는 입력값은 M2b 오프닝 설계 때 다시 본다.
6. **기존 시험 수정** — 동의 단계 때문에 시험 4개 파일과 `openDemo` 를 고친다. 기대값은 그대로 두고 단계만 더하며(키보드 시험만 Tab 순서), 바꾼 줄을 표로 보고해 리뷰어가 확인한다.
7. **T0이 이 PR에 들어가 범위가 섞인다** — 커밋을 `fix(media): …` 로 따로 두고 PR 설명에 따로 적는다. M3b3c가 같은 `useInView`·조정을 YouTube 플레이어에 쓰므로, M3b3c 계획을 시작할 때 "안 보임 보고마다 해제"·"우리 `pause()` 카운터" 규칙을 그 플레이어 기준에 넣는다.
8. **방침 문구가 구현과 어긋날 수 있다** — 서버 저장·로그 규칙이 바뀌면 방침도 고친다(M2a·M4·M6b 계획 때 확인 항목). 서버 로그의 문서 제목은 장소가 요청에 들어오는 M2에서 다시 본다(로드맵 리스크 7).
9. **시행일 커밋을 잊고 머지** — D1 명령을 T4 머지 전 확인에 넣었다. 오케스트레이터가 PR 설명의 확인 목록에도 적는다.
10. **크기** — 다시 셈 약 317(T0 포함), ×1.14면 약 361. 방침 HTML 줄 수가 가장 불확실해 T1 뒤 확인점(누적 S1 210)을 둔다.

## PM 결정 (2026-10-15, 기록)

| # | 물은 것 | 결정 | 이 계획서에 반영 |
|---|---|---|---|
| 1 | M3b3 리뷰 권장 1(절반 이하로 보일 때 사용자가 튼 영상이 화면 밖에서도 계속 재생)·권장 2(`ourPause` 카운터)를 어디서 고치나 | **(a)** 이 PR의 T0에서 둘 다, `fix(media)` 커밋 따로. planner 추천과 같음. 고르지 않은 것: (b) 권장 1만, (c) 별도 `fix/` PR, (d) M6b로 미룸 | 바뀐 점, 목표 4, "T0 — 재생 규칙 보강", 데모 표 T0 행, 크기 T0, 작업 순서, T0, T3 (3), 리스크 7 |
| 2 | 방침에 넣을 사실 3가지 | 운영자 **"The World That Day 운영자"** · 문의 **(a)** 이 저장소의 GitHub Issues(`https://github.com/ericmschoi/The-World-That-Day/issues`) · 시행일 **(a)** 머지하는 날. planner 추천과 같음. 고르지 않은 것: 문의 (b) 이메일 (c) 둘 다, 시행일 (b) 따로 정한 날 | 바뀐 점, 사용할 외부 출처(링크 3개), 점검표 운영자·문의·시행일 행, "시행일 넣는 법", 부품 표 `urls.ts`, 크기 T1, T1 (2)·(3)·(8), T2 (5), T3 (2)(c), T4, D1 명령, 리스크 9 |

이전 결정: 10.1절 9번 (a), 23번 (b)(2026-10-11), 동의 단계를 지금 임시 입력 화면에 만들고 M2b가 옮김(로드맵 D2 (a)), 분할(D1 (a))(2026-10-12), `web/src/test/**` S1 제외(2026-10-14). 남은 질문은 없다.

★ PM 직접 구현: 없음 — 이 기능에는 백엔드 작업이 없다.
