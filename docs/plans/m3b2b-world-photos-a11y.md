# 그날 찍힌 사진·비극 블록과 카드 접근성 (M3b2b)

> 브랜치: `feat/world-photos-a11y` · 작성: planner · 상태: **PM 승인 (2026-10-13, 개정)** — 결정 1 **(a)** T0은 응답 모양 검사만(섹션 ErrorBoundary는 M6b), 결정 2 **(a)** web-ci 경로를 T3에, 결정 3 **(a)** 한 PR + T2 뒤 확인점(누적 S1 300), 결정 4 **(a)** 세 번째 소제목 "다른 때 찍힌 사진". 처음 승인 2026-10-12(M3b2 분할로 새로 생김, 로드맵 D1 (a). Playwright CI는 D3 (a))
> 기준: M3-P17 (a)(결정 2026-10-11), #21 (a), #22 (c), 제품 정의서 6.1·6.3·8·12절, `CLAUDE.md` 7절 1·3·5·6항, `docs/plans/m3b2-world-events-ui.md`(머지된 코드 기준), `docs/plans/m3b1b-media-card.md`, `docs/plans/m3b1-design-foundation.md`(토큰·문구), `docs/plans/m3a3-event-media.md`(`day_media` 순서), `docs/plans/m3a3b-commons-photos.md`(`day_media` 내용), **M3b1b 리뷰 후속(PM 결정 2026-10-11)**: 라이선스 줄 중복은 (b), 새 창 안내, 설명 두 번 읽기 방지, `fallbackAlt` 확인, Playwright로 포커스 링·토큰 색 확인, **M3b2 리뷰 권장(2026-10-13)**: 사건 모양 검사, `clientFallback` 시간 초과 신호 리스너 시험
> 선행: **M3b2 (PR #13) 머지 — `main` e254ad1 에 있다.** 이 브랜치는 그 `main` 에서 갈라졌다. 구현과 자동 테스트는 fixture만 쓴다. 서버는 **M3a3b 머지 전까지 `media`·`day_media` 를 늘 `[]` 로** 보내므로, 실제 사진으로 보는 PM 확인은 머지 뒤 T6이다(M3b2 T6과 같은 방식 — PR 머지를 막지 않는다).

## 2026-10-13 바뀐 점

M3b2에서 실제로 머지된 코드를 읽고 맞췄다. 계획서와 코드가 다른 곳은 **코드를 따랐다**.

| 바뀐 것 | 이유 |
|---|---|
| **`visually-hidden` 클래스는 이미 있다** — T2 산출물에서 뺌 | M3b2가 `global.css` 에 만들었다(시험도 `cssRules.test.ts` 에 있음) |
| **출처 줄 링크의 "(새 창에서 열림)"은 이미 있다.** 남은 링크는 캡션의 "원문 보기"·"라이선스" 두 개 | M3b2 `WorldEventItem` 의 `NewTabLink`. T1이 이 부품을 공용으로 빼고, T2가 캡션에 쓴다 |
| **새 T0: 응답 모양 검사** | M3b2 리뷰 권장 1. `isWorldResponse` 는 `events` 가 배열인지만 본다. 사건 하나가 `null` 이거나 `media` 가 없으면 결과 화면 전체가 깨진다(앱에 ErrorBoundary 없음). **결정 1 (a)**: 모양 검사만 하고 섹션 ErrorBoundary는 M6b |
| **"다른 때 찍힌 사진" 묶음** (세 번째 소제목) | M3a3 계약상 `day_media` 순서는 `same_day` → 그 밖의 `kind` → `unknown` 이다. 이전 계획은 `same_day` 아닌 것을 모두 "촬영 시점을 확인하지 못한 사진" 아래에 두었는데, `same_month`·`years_apart` 사진은 촬영 시점을 **아는** 사진이라 그 제목이 틀린 말이 된다. **결정 4 (a)** |
| **데모 표를 실제 fixture 값으로** | 1988 사건 사진은 CC BY-SA가 아니라 **Public domain**(`license_url` 없음 → "라이선스" 링크 없음). 2002 KOGL 사진의 `attribution` 에는 "KOGL Type 1"이 **들어 있다** → 라이선스 줄이 숨는다. 줄이 보이는 경우는 합성 시험으로 본다 |
| **비극 블록 안 링크 색** | 전역 `a` 색이 `--color-accent` 라서 그대로 두면 비극 블록에 강조색이 들어간다. 블록 안 링크는 `--color-tragic-text` |
| **비극 시험 응답을 fixture 모듈에 더함** | M3b2는 비극 사건을 테스트 파일 안에서만 만들었다. Playwright도 같은 값을 써야 하므로 `responses.ts` 에 합성 응답 2개를 둔다(기본 핸들러에는 넣지 않음) |
| **Playwright 설정 세부** | Vitest 기본 수집 규칙이 `e2e/*.spec.ts` 도 잡는다 → 제외. e2e가 `src` 의 fixture 모듈을 가져오므로 e2e 타입 검사는 `bundler` 해석을 쓰는 별도 tsconfig. 새 창 요청까지 막도록 `context.route`. 개발 브라우저용 MSW는 없다(M3b2 범위 밖) |
| **T4에 M3b2 2차 리뷰 권장 1** | `clientFallback.test.ts` 에서 시간 초과 신호의 `removeEventListener` 도 감시(시험만) |
| **T5 = 실제 서버, 사진 없이 / 새 T6 = M3a3b 머지 뒤 실제 사진** | M3a3b가 아직 서버에서 진행 전이다. M3b2 결정 2 (a)와 같은 방식 |
| **web-ci 경로** | 웹 시험이 `data/schema/examples/media/valid/*.json` 을 읽는데 `web-ci` 는 `web/**` 에만 걸린다. **결정 2 (a)**: T3에서 경로를 더한다 |
| **크기 다시 셈**: 약 150(보정 240~260) → **약 315 (범위 315~445, M3b2 실측 비율이면 약 375)** | 아래 "크기". **결정 3 (a)**: 한 PR + T2 뒤 확인점(누적 S1 300) |

**작업 번호**: T1~T5는 그대로다. **T0(응답 모양 검사)과 T6(실제 사진 확인, PM)이 새로** 생겼다. T5의 내용은 "실제 서버, 사진 없이"로 바뀌었다.

## 목표

0. 서버 응답의 사건·미디어 하나가 모양이 틀려도 결과 화면이 깨지지 않는다. 틀린 사건만 빠지고 나머지는 보인다(M3b2 리뷰 권장 1, 제품 정의서 8절, 결정 1 (a)).
1. `day_media` 를 M3-P17 (a)대로 묶는다: "그날 찍힌 사진" 제목 아래에는 `date_relation.kind = same_day` 만. `unknown` 은 "촬영 시점을 확인하지 못한 사진" 아래, 그 밖의 `kind` 는 "다른 때 찍힌 사진" 아래(결정 4 (a)).
2. `tragic` 사건을 정상 사건 뒤의 차분한 별도 블록("같은 날 있었던 아픈 일")으로, 미디어 없이 보여 준다(6.3절, #22 (c)).
3. M3b1b 리뷰 후속(PM 결정)을 `MediaCaption`·`MediaFigure` 에 반영한다:
   - `attribution` 이 이미 라이선스 이름을 담고 있으면 캡션의 라이선스 줄을 숨긴다(선택지 (b)).
   - 캡션의 새 창 링크("원문 보기", "라이선스")에 화면 읽기 프로그램용 "(새 창에서 열림)" 안내를 단다(출처 줄 링크는 M3b2에서 끝남).
   - 같은 설명이 `alt` 와 캡션에서 두 번 읽히지 않게 한다.
   - 화면이 넘기는 `fallbackAlt` 에 "그날"을 떠올리게 하는 말이 없는지 테스트한다(사건 미디어는 M3b2 T2 (10)에서 끝남 — 이 기능은 `day_media` 몫을 더한다).
4. **Playwright**를 웹에 들여와(기술 스택의 E2E 도구) 실제 브라우저에서 확인한다: 키보드로는 포커스 링이 보이고 마우스 클릭으로는 보이지 않는다, 디자인 토큰 색이 실제로 칠해진다. `web-ci` 에 Chromium 하나로 돌리는 단계를 더하고(D3 (a)), 웹 시험이 읽는 데이터 경로에도 `web-ci` 가 걸리게 한다(결정 2 (a)).

## 범위 밖

- 영상·임베드 재생 → M3b3·M3b3c
- 동의 단계 → M3b3b
- 표본 날짜 버튼(1950-06-25·2014-04-16 등) → M3b3. 데모·표본 날짜 전체 E2E → M6c (이 기능의 Playwright는 포커스·토큰·가로 스크롤 확인과, 뒤 기능이 쓸 설정만)
- 섹션 ErrorBoundary, 원 출처 이미지가 깨질 때 처리, 앱 전체 장애 격리 다듬기 → M6b (결정 1 (a))
- 개발 브라우저용 MSW, 서버 변경
- `tragic` 이 있는 날 `day_media` 를 웹에서 따로 숨기는 규칙 — 서버가 이미 `[]` 로 보낸다(M3a3b #22 (c)). 웹은 받은 대로 그린다

## 사용할 외부 출처

없음. 프론트엔드는 우리 API(`/api/story/world`)만 부른다. Playwright 테스트는 우리 화면만 띄우고, `/api/story/world` 는 `context.route` 로 `web/src/mocks/fixtures/world/` 의 fixture 객체를 JSON으로 돌려주며, 그 밖의 `localhost` 밖 요청(원 출처 이미지, 새 창으로 연 원문 페이지 등)은 모두 `abort` 한다(`CLAUDE.md` 7절 5항 — 테스트는 외부 네트워크를 부르지 않는다). 그래서 e2e 화면의 사진 자리는 비어 보인다(크기는 `width`·`height` 속성으로 잡힌다).

## API / 데이터 변경

서버 API는 바뀌지 않는다(계약은 M3b2 계획서 "API / 데이터 계약"과 머지된 `web/src/world/types.ts` 그대로). 웹 쪽에서 바뀌는 것만 적는다.

### 응답 모양 검사 (T0, `web/src/world/` 안)

`isWorldResponse`(지금: `events` 가 배열인지만)를 "받은 값을 화면이 쓸 수 있는 `WorldResponse` 로 고르는 함수"로 바꾼다. 화면이 실제로 읽는 필드만 본다.

| 대상 | 규칙 |
|---|---|
| 최상위 | 객체이고 `events` 가 배열이 아니면 → 지금처럼 섹션 `error` |
| 사건 | 객체이고 `id`·`summary`·`source_name`·`source_title`·`source_url`·`text_license`·`text_license_url` 이 문자열, `tone` 이 `normal`·`tragic` 중 하나 → 남김. 아니면 그 사건만 버림 |
| 사건 `title` | 문자열이 아니면 `null` 로(→ `fallbackAlt` "사건 관련 자료") |
| 사건 `media` | 배열이 아니면 `[]`. 배열 안에서는 객체이고 `id` 가 빈 문자열이 아닌 것만 남김(나머지 검사는 지금처럼 `isDisplayable`) |
| `day_media` | 배열이 아니면 `[]`. 안쪽은 사건 `media` 와 같은 규칙 |
| `notice` | `{ code: 'NO_EVENTS_FOUND' }` 가 아니면 `null` |
| 모두 버려짐 | 받은 `events` 가 비어 있지 않았는데 남은 사건이 0개이고, 남은 `day_media` 도 0개이고, `notice` 가 `null` → 섹션 `error` |

- 버린 사건·미디어는 로그로 남기지 않는다(M3b2 C5, 요청 날짜가 로그에 남지 않게).
- 섹션 ErrorBoundary는 이 기능에서 두지 않는다(결정 1 (a)). 그리는 코드의 버그까지 막는 장치는 M6b "장애 격리"에서 섹션 전체에 한 번에 둔다.

### fixture 추가 (T1, `web/src/mocks/fixtures/world/responses.ts`·`README.md`, S1에서 뺌)

| 이름 | 내용 |
|---|---|
| `worldWithTragic` | 합성. `world1988` 바탕, 사건 = [`normal` "시험용 사건 문장 A", `tragic`(`tone_basis` `rule:keyword`) "시험용 비극 사건 문장" + `media` 에 사진 1장(그려지면 안 됨)]. `day_media: []` |
| `worldTragicOnly` | 합성. 위 `tragic` 사건 하나, `notice: null`, `day_media: []` |

- 기본 핸들러(`handlers.ts`)는 바꾸지 않는다. 테스트는 `server.use(...)`, Playwright는 `context.route` 로 바꿔 끼운다.
- 그 밖의 `kind`(`same_month`·`years_apart` 등) `day_media` 는 테스트 파일 안에서 합성한다.
- README의 "합성 값" 표에 두 응답을 더한다.

## 화면 구조와 문구 (M3b2 화면에 더함)

```
그날의 세계                               (h2 — M3b1)
 [빈 날짜 안내]                            (M3b2, notice 가 있을 때)
 [사건 목록 — normal 만]                   (M3b2)
 그날 찍힌 사진                            (h3, same_day 사진이 있을 때)
   [MediaItem × N]
 다른 때 찍힌 사진                          (h3, same_day·unknown 밖의 day_media 가 있을 때 — 결정 4 (a))
   [MediaItem × K]                        ← 캡션 배지는 카드 규칙 그대로(같은 달, ±N년 등)
 촬영 시점을 확인하지 못한 사진              (h3, unknown day_media 가 있을 때 — M3-P17 (a))
   [MediaItem × M]                        ← 캡션 "촬영 시점 미상", 배지 없음
 같은 날 있었던 아픈 일                     (h3, tragic 사건이 있을 때 — 차분한 회색 블록)
   · 문장 원문 + 출처 줄 (미디어 없음)
```

| 자리 | 문구 (PM이 T5에서 검토) |
|---|---|
| 그날 사진 소제목 | 그날 찍힌 사진 |
| 다른 때 사진 소제목 (결정 4 (a)) | 다른 때 찍힌 사진 |
| 촬영 시점 미상 묶음 소제목 | 촬영 시점을 확인하지 못한 사진 |
| 비극 블록 소제목 | 같은 날 있었던 아픈 일 |
| `day_media` 대체 텍스트(`fallbackAlt`) | 기록 사진 |
| 새 창 안내 (화면에는 안 보임) | (새 창에서 열림) — M3b2와 같은 공용 부품 |

- 묶음 안의 순서는 서버 순서 그대로다. 묶음이 비면 그 소제목도 없다.
- 사건 카드 안의 사진은 묶지 않는다(M3b2 그대로: 사건 ①의 촬영 시점 미상 사진은 사건 안에서 "촬영 시점 미상"으로 보인다). M3-P17 (a)의 묶음은 `day_media` 에만 적용한다.
- 비극 블록에는 강조색·축하 요소·미디어를 쓰지 않는다. 블록 안 링크 글자도 `--color-tragic-text`(밑줄로 링크임을 보임). 키보드 포커스 링은 접근성 표시라 `--color-focus` 그대로. `tragic` 사건에 `media` 가 와도 그리지 않는다. `normal` 사건이 없고 `tragic` 만 있으면 비극 블록만 보인다(빈 날짜 안내 없음).
- 비극 블록의 출처 줄은 사건 목록과 같은 부품(출처: {source_name} 「{source_title}」 · {text_license}, 두 링크에 새 창 안내)을 쓴다.

## 캡션 접근성 규칙 (M3b1b 리뷰 후속)

| 규칙 | 내용 |
|---|---|
| 라이선스 줄 중복 (b) | `license_status` 가 `unknown` 이 아니고, `license` 가 비어 있지 않고, 그 값이 `attribution` 에 들어 있으면(대소문자 무시, 공백은 한 칸으로 보고 비교) 라이선스 줄을 그리지 않는다. "라이선스" 링크(`license_url`)는 그대로 둔다. `license_status: unknown` 이거나 `license` 가 비면 "라이선스 확인 안 됨" 줄이 **늘** 보인다(`attribution` 에 같은 말이 있어도 — 정직한 표시가 먼저) |
| 새 창 안내 | 캡션의 `target="_blank"` 링크("원문 보기", "라이선스")마다 공용 `visually-hidden` 클래스의 "(새 창에서 열림)"을 링크 안에 넣는다. M3b2 `NewTabLink` 를 공용으로 옮겨 다시 쓴다 |
| 설명 두 번 읽기 | 캡션이 `description` 을 보여 주면 `img` 의 `alt` 는 `fallbackAlt` 를 쓴다(설명 전체가 `alt` 와 `figcaption` 에 둘 다 들어가지 않게). 캡션에 설명이 없으면 지금처럼 `alt` = `fallbackAlt` |
| `fallbackAlt` | 화면이 넘기는 값(사건: 「제목」 관련 자료 / 사건 관련 자료 — M3b2, `day_media`: 기록 사진)에 "그날", "같은 날", "당일", 요청 날짜 문자열(`YYYY-MM-DD`, "YYYY년 M월 D일", "M월 D일")이 없다 |

참고 (M3b2 PM 검수 메모): 사건 사진의 대체 텍스트 「제목」 관련 자료에는 제목 속 연도가 그대로 들어간다(예: 「1988년 하계 올림픽」 관련 자료). 제목은 사건 이름이고 "관련 자료"는 촬영 시점을 말하지 않으므로 이 계획은 그대로 둔다. 다만 위 "설명 두 번 읽기" 규칙으로 **설명이 있는 사진도 대체 텍스트가 이 문구가 되어** 더 자주 읽힌다. 바꾸고 싶으면 T5에서 말하면 된다(예: 제목 앞 "YYYY년 " 떼기 — 한 줄 고침).

## Playwright 설정

- `web/` 에 `@playwright/test` 개발 의존성, `web/playwright.config.ts`, 테스트는 `web/e2e/*.spec.ts`, `npm run e2e` 스크립트. 재시도 0(불안정을 숨기지 않음).
- 브라우저는 Chromium 하나. `webServer` 는 `npm run dev`(5173, `strictPort`). 8080 서버는 띄우지 않는다 — `/api` 요청은 모두 가로챈다. `serviceWorkers: 'block'`(지금은 서비스 워커가 없지만 뒤에 생겨도 가로채기가 먼저 받게).
- 가로채기는 `context.route`(새 창 포함): `/api/story/world` → fixture 객체, `localhost` 밖 → `abort`.
- 화면 폭 360px과 1280px 두 프로젝트(뒤 기능의 YouTube 플레이어 크기 확인에도 쓴다, M3b3c).
- Vitest가 `e2e/**` 를 수집하지 않게 `vite.config.ts` 의 `test` 설정에서 뺀다. e2e와 설정 파일의 타입 검사는 `tsc -b` 에 들어가되, `src` fixture 모듈을 가져오므로 `moduleResolution: bundler` 인 별도 tsconfig로 한다. `playwright-report/`·`test-results/` 는 Git·Prettier에서 뺀다.
- CI: `web-ci` 의 `npm run build` 뒤에 `npx playwright install --with-deps chromium` 과 `npm run e2e` 단계를 더한다(**D3 (a), PM 결정 2026-10-12**). 같은 파일의 `pull_request`·`push` 경로에 `data/schema/examples/media/valid/**` 를 더한다(**결정 2 (a), 2026-10-13**).
- S1(줄 수)에서 `web/e2e/**` 를 테스트로 보고 뺀다.

## 테스트 방침

M1b·M3b2 "테스트 방침"을 따른다. 컴포넌트 테스트는 Vitest·Testing Library·MSW, 실제 렌더링(포커스 링·계산된 색·가로 스크롤)은 Playwright. 비극 시험 응답의 문장은 누가 봐도 가짜로 쓴다("시험용 비극 사건 문장"). 날짜는 데모 3개(제품 정의서 12절)만, 400 시험은 M3b2처럼 경계값 `2001-02-29` 만. effect 요청 테스트는 마지막 상태를 기다린다(M3b2 C3).

## 데모 날짜로 확인할 것

| 날짜 | 자동 테스트 (fixture) | PM 확인 — T5 (실제 서버, 사진 없음) | PM 확인 — T6 (M3a3b 머지 뒤, 실제 사진) |
|---|---|---|---|
| 1988-09-17 | `world1988WithMedia`: "그날 찍힌 사진" 아래 `img` 1개(라이선스 미확인 예시 — 캡션 "같은 날", "라이선스 확인 안 됨"), 그 묶음 안에 "촬영 시점 미상" 글자 없음. "촬영 시점을 확인하지 못한 사진" 아래 `img` 1개(`-day` 사본, "촬영 시점 미상", "같은 날" 없음). "다른 때 찍힌 사진" 없음. 사건 ①의 사진 2장은 사건 안에 그대로, 화면의 `img` 는 모두 4개. 비극 블록 없음. 사건 ①의 PD 사진 캡션: `attribution` 에 "Public domain"이 있어 라이선스 줄이 따로 없고, `license_url` 이 `null` 이라 "라이선스" 링크도 없다. "원문 보기" 링크 접근 이름에 "새 창에서 열림". 설명이 있는 사진의 `alt` 는 「1988년 하계 올림픽」 관련 자료(사건) / 기록 사진(`day_media`) | 사건 3개, 사진 묶음 제목 없음(서버가 아직 `day_media: []`), 비극 블록 없음. 키보드 Tab으로 출처 줄 링크에 포커스 링이 보이고, 마우스로 누르면 보이지 않는다 | 사진 묶음이 실제 사진으로. 묶음 제목 아래 사진의 캡션이 제목과 맞는지(그날 찍힌 사진 = "같은 날"만). 캡션의 라이선스 줄 숨김과 "라이선스" 링크. 키보드 Tab으로 "원문 보기"에 포커스 링 |
| 2002-06-22 | `world2002WithMedia`: KOGL 사진 캡션 — `attribution` 에 "KOGL Type 1"이 있어 라이선스 줄이 없고 "라이선스" 링크(`license_url`)는 있다. 사진 묶음 제목 없음(`day_media: []`) | 문장과 출처 줄 | 사건·그날 사진(M3a3b 데모 표: `day_media` 1장, KOGL Type 1) |
| 1995-03-15 | 안내 문장, 사진 묶음 제목 없음, 비극 블록 없음 | 같은 내용 | 같은 내용(서버 녹화로 `day_media: []`) |
| (합성) 라이선스 줄이 보이는 경우 | `attribution` 에 `license` 값이 없는 사진 → 라이선스 줄이 보인다 | - | - |
| (합성) 다른 때 찍힌 `day_media` | `same_month`·`years_apart` 사진 → "다른 때 찍힌 사진" 아래, 캡션 배지가 실제 관계(같은 달, ±N년) | - | - |
| (합성) `worldTragicOnly` | 비극 블록만, 그 안에 `img`·`iframe`·`video` 없음(`media` 를 넣어 보내도), 빈 날짜 안내·사건 목록 없음 | 데모 날짜에는 `tragic` 이 없어 Playwright 결과로 본다(실제 화면은 표본 버튼이 생기는 M3b3 뒤) | - |
| (합성) `worldWithTragic` | 정상 사건이 문서 순서상 앞, 비극 블록이 뒤. 비극 블록 링크 접근 이름에 "새 창에서 열림" | - | - |
| (합성) 모양이 틀린 응답 | 아래 T0 완료 기준 | - | - |

## 크기 (400줄 규칙)

### 웹 실측 비율

| 기능 | 예상 → 실제 (S1) | 비율 |
|---|---|---|
| M3b1 | 225 → 309 | 약 1.4배 |
| M3b1b | 165 → +158 | 약 1.0배 |
| **M3b2** | **365 → 434** | **약 1.19배** |

이전 셈(약 150, 보정 240~260)은 너무 작았다. 머지된 코드를 보면 출처 줄·새 창 링크를 비극 블록과 캡션이 같이 쓰도록 공용 부품으로 빼야 하고, 응답 모양 검사(T0)와 세 번째 묶음, Playwright의 tsconfig·Vitest 제외 설정이 빠져 있었다. M3b2처럼 **파일별로 다시 세고**(Prettier 80칸) 웹 비율 1.0~1.4배를 범위로 둔다.

### 다시 센 값

| 작업 | 내용 | 다시 셈 | 범위 (×1.0~×1.4) |
|---|---|---|---|
| T0 | 응답 모양 검사 약 40, `WorldSection` 고침 약 5 | 약 45 | 45~65 |
| T1 | 사진 묶음 컴포넌트 약 45, 비극 블록 약 30, `NewTabLink`·출처 줄을 공용 부품으로 옮김 약 40(지우는 줄 약 28은 S1 "added"에 안 들어감), `WorldSection` 약 15, CSS 약 40 | 약 170 | 170~240 |
| T2 | 라이선스 줄 판단 약 12, `MediaCaption` 링크·줄 약 15, `MediaFigure` `alt` 약 3 | 약 30 | 30~40 |
| T3 | `playwright.config.ts` 약 35, e2e tsconfig 약 15, `vite.config.ts`·`tsconfig.json`·`package.json`·ignore 파일 약 10, `web-ci.yml` e2e 단계 약 6 + 경로 2 | 약 70 | 70~100 |
| **합계** | | **약 315** | **315~445** — M3b2 실측 비율(×1.19)이면 **약 375** |

- **한 PR + T2 뒤 확인점 (결정 3 (a), 2026-10-13)**: T0~T2 누적 S1이 **300**(다시 셈 245 × 약 1.2)을 넘으면 오케스트레이터가 멈추고, T3(Playwright·`web-ci`)을 별도 PR(M3b2c)로 뺄지 PM에게 다시 묻는다. 넘지 않으면 그대로 T3으로 간다.
- 테스트(`*.test.ts(x)`), fixture(`web/src/mocks/fixtures/**`), e2e(`web/e2e/**`)는 S1에서 뺀다. `.github` 는 넣는다.

## 작업 목록

순서: T0 → T1 → T2 → **확인점 (누적 S1 300)** → T3 → T4 → T5(PR 검수 때) → 머지 → T6(M3a3b 머지 뒤, `main` 에서). 서버 M3a3a·M3a3b와 폴더가 달라 병렬이다.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T0 | 응답 모양 검사 (새로, M3b2 리뷰 권장 1, 결정 1 (a) — ErrorBoundary 없음) | frontend-dev | `web/src/world/` 의 검사 함수, `WorldSection.tsx`, 테스트 | M3b2 머지(완료) | (1) 합성 응답 `events` = [정상 A, `null`, A와 같되 `media` 없는 B, `summary` 가 숫자인 C, `media` = [`null`, 사진 1장]인 D] → 섹션 `ready`, A·B·D 문장이 보이고 C는 없다. B는 사진 없이 문장과 출처 줄, D는 `img` 1개. (2) `day_media` 가 없거나 배열이 아니면 사진 묶음 없이 나머지가 보인다. `day_media` 안의 `null` 은 무시된다. (3) `events` 가 비어 있지 않은데 모두 버려지고 `day_media` 도 비고 `notice` 가 `null` → "이 섹션을 불러오지 못했습니다." (4) `StoryScreen` 시험: 위 (1) 응답에서 다른 4개 섹션과 "처음으로" 버튼이 그대로 있다. (5) (1)~(3)에서 `console.error`·`console.warn` 호출 0회(React key 경고 포함, 스파이로 확인). (6) 기존 `WorldSection.test.tsx` 의 "200 whose body is not the world shape"·"not JSON" 시험이 고치지 않고 통과. (7) ErrorBoundary 부품을 만들지 않는다(M6b). (8) M3b2 C5 출력 없음, 웹 전체 검사 통과, 보고서에 T0 S1 |
| T1 | `day_media` 묶음(M3-P17 (a), 결정 4 (a))과 비극 블록, 공용 링크·출처 줄 부품 | frontend-dev | `WorldSection` 하위 컴포넌트(사진 묶음, 비극 블록, 출처 줄), 공용 `NewTabLink`, CSS, fixture 2개·README, 컴포넌트 테스트 | T0 | (1) 위 "데모" 표 자동 테스트 열의 1988·2002·1995·다른 때 찍힌 `day_media`·`worldTragicOnly`·`worldWithTragic` 행 중 묶음·비극 블록 부분 통과. (2) 소제목 세 개("그날 찍힌 사진", "다른 때 찍힌 사진", "촬영 시점을 확인하지 못한 사진")가 h3이고 이 순서다. 각 묶음 안은 서버 순서. 합성 `same_month`·`years_apart` `day_media` 는 "다른 때 찍힌 사진" 아래에만 있고 "그날 찍힌 사진"·"촬영 시점을 확인하지 못한 사진" 아래에는 없다. `unknown` 은 "촬영 시점을 확인하지 못한 사진" 아래에만 있다. 묶음이 비면 그 소제목이 없다. (3) 비극 블록: `media` 를 넣어 보내도 그 안에 `img`·`iframe`·`video` 가 없고, 출처 줄 두 링크의 접근 이름에 "새 창에서 열림". (4) CSS 시험: 비극 블록 규칙에 `--color-accent` 가 없고 배경·글자·링크 색은 `--color-tragic-*` 만. (5) `day_media` 에 넘기는 `fallbackAlt` 가 "기록 사진"이고, 설명이 없는 `day_media`·사건 사진의 `alt` 에 "그날"·"같은 날"·"당일"·"1988-09-17"·"1988년 9월 17일"·"9월 17일"이 없다. (6) `NewTabLink` 는 공용 위치 한 곳에만 있고 사건 목록·비극 블록이 같은 출처 줄 부품을 쓴다. (7) `worldWithTragic`·`worldTragicOnly` 가 `satisfies WorldResponse` 로 타입 검사 통과, 기본 핸들러는 그대로, README 합성 값 표에 두 줄. (8) M3b2 시험 중 "tragic·`day_media` 를 그리지 않는다"를 확인하던 기대값(`WorldSection.test.tsx` 의 T2 (5)·"no day_media" 부분 등)은 새 동작에 맞게 기대값만 바꾼다 — 지운 시험 없음, 바꾼 것은 보고에 표로. (9) 웹 전체 검사 통과, 보고서에 누적 S1 |
| T2 | 캡션 접근성: 라이선스 줄 (b), 캡션 링크 새 창 안내, 설명 두 번 읽기 방지 | frontend-dev | `MediaCaption.tsx`, `MediaFigure.tsx`, `licenseLabel.ts`(판단 함수), 테스트 | T1 | (1) `attribution` 이 `license` 를 담으면(대소문자·공백 차이 무시) 라이선스 줄이 없고 `license_url` 이 http(s)면 "라이선스" 링크는 있다. 담지 않으면 줄이 있다. `license_status: unknown` 이거나 `license` 가 비면 "라이선스 확인 안 됨"이 늘 있다(`attribution` 에 같은 말이 있어도). (2) 캡션의 `target="_blank"` 링크마다 접근 이름에 "새 창에서 열림"이 있다(`getByRole('link', { name: /원문 보기.*새 창에서 열림/ })`, "라이선스"도 같음). 캡션 밖에도 안내 없는 `target="_blank"` 링크가 없다(제품 코드 검색 `grep -rn '_blank' web/src --exclude='*.test.*'` 결과가 공용 `NewTabLink` 한 곳). (3) 설명이 있는 사진: `img` 의 접근 이름이 `fallbackAlt` 이고 `figcaption` 에 설명이 있다 — 설명 전체가 둘 다에 있지 않다. 설명이 없거나 공백뿐이면 `alt` = `fallbackAlt`. (4) M3b1b·M3b2 기존 테스트가 새 규칙에 맞게 기대값만 고쳐지고 통과 — 예상 대상: `MediaFigure.test.tsx`(설명으로 찾던 `img` 이름, "KOGL Type 1" 줄), `MediaCaption.test.tsx`·`MediaItem.test.tsx`·`MediaCardContract.test.tsx`(링크 이름·글자). 지운 테스트 없음, 바꾼 기대값은 보고에 표로. (5) 웹 전체 검사 통과, 보고서에 **T0~T2 누적 S1 — 확인점 300** (넘으면 오케스트레이터가 T3 전에 멈추고 M3b2c 분리를 PM에게 묻는다) |
| T3 | Playwright 설정과 포커스·토큰 확인, `web-ci` e2e 단계와 데이터 경로(결정 2 (a)) | frontend-dev | `web/package.json`, `web/playwright.config.ts`, e2e tsconfig, `web/vite.config.ts`(Vitest 제외), ignore 파일, `web/e2e/focus-and-tokens.spec.ts`, `.github/workflows/web-ci.yml` | T2 (확인점 통과) | (1) `cd web && npm run e2e` 통과(로컬, Chromium, 360·1280 두 프로젝트, 재시도 0). (2) 키보드: `world1988WithMedia` 화면에서 Tab으로 "원문 보기" 링크에 가면 계산된 `outline-style` 이 `none` 이 아니고 `outline-color` 가 `--color-focus` 의 계산된 값과 같다. (3) 마우스: 같은 링크를 마우스로 누르면(열린 새 창은 닫음) `outline-style` 이 `none` 이거나 두께 0. (4) 토큰 색: `body` 배경이 `--color-bg`, 사건 출처 줄 링크 글자가 `--color-accent`, `worldWithTragic` 의 비극 블록 배경이 `--color-tragic-bg`, 그 안 링크 글자가 `--color-tragic-text` 의 계산된 값과 같다. (5) `localhost` 밖 요청은 모두 가로채져 `abort` 되고, 응답을 받은(`requestfinished`) `localhost` 밖 요청이 0건이다. 8080 서버 없이 통과한다. (6) 360px 폭에서 1988·`worldWithTragic` 화면 모두 가로 스크롤이 없다(`scrollWidth <= clientWidth`). (7) `npm test` 가 `web/e2e/**` 를 수집하지 않고, `npm run typecheck` 가 `playwright.config.ts`·`web/e2e/**` 를 검사하며, `npm run lint`·`format:check` 통과. `playwright-report/`·`test-results/` 가 `git status` 에 나오지 않는다. (8) `web-ci` 에서 `npm run build` 뒤 Chromium 설치와 `npm run e2e` 단계가 돌고 이 PR에서 통과한다. (9) `web-ci.yml` 의 `pull_request`·`push` 경로 목록 두 곳 모두에 `data/schema/examples/media/valid/**` 가 있다. (10) 보고서에 누적 S1 |
| T4 | 완료 기준 검증과 전체 테스트, M3b2 2차 리뷰 권장 1 | test-engineer | 빠진 기준의 테스트, `web/src/api/clientFallback.test.ts` 보강 | T3 | (1) T0~T3 기준 표, 빠진 시험 추가. (2) `npm test` 3회, `npm run e2e` 3회 반복 안정. (3) M3b2 계획서의 C1·C2·C4·C5·C6 출력 없음, C3 `0`. (4) `clientFallback.test.ts`(`AbortSignal.any` 없는 경로): `AbortSignal.timeout` 이 돌려준 시간 초과 신호의 `removeEventListener` 도 감시해, 정상 응답 뒤에는 `'abort'` 가 정확히 한 번, 호출자 취소 뒤와 시간 초과 뒤에는 `'abort'` 가 한 번 이상 지워진다. 제품 코드는 바꾸지 않는다(실패하면 재현 방법과 함께 frontend-dev로). (5) "데모" 표 자동 테스트 열의 행마다 덮은 시험을 표로. (6) S1·S2 보고 |
| T5 | 실제 서버로 화면 확인 (PR 검수 때, 사진 없이) | PM | - | T4 | (1) 루트 `.env` 에 `WIKIMEDIA_USER_AGENT`, DB·서버·웹 실행. (2) 데모 버튼 3개로 "데모" 표 T5 열: 1988은 사건 3개, 사진 묶음·비극 블록 없음(서버가 아직 사진을 보내지 않는 것이 정상), 360px 폭 가로 스크롤 없음. (3) 키보드 Tab·마우스 클릭으로 출처 줄 링크의 포커스 링 차이. (4) 화면 읽기 프로그램(선택, macOS VoiceOver)으로 출처 줄 링크에서 "새 창에서 열림"이 들리는지. (5) 문구 검토: 새 소제목 4개와 "기록 사진" — fixture 화면은 `cd web && npx playwright test --ui` 로 볼 수 있다(외부 요청을 막으므로 사진 자리는 빈 칸). (6) 대체 텍스트의 연도(위 "참고")를 그대로 둘지. T5가 끝나면 머지할 수 있다 |
| T6 | 실제 사진으로 화면 확인 (새로, 머지 뒤 M3a3b 머지 뒤 `main` 에서) | PM | - | 이 PR 머지, **M3a3b 머지** | (1) `main` 을 받아 DB·서버·웹 실행(M3b2 T6과 같은 자리에서 함께 해도 된다). (2) 데모 버튼으로 "데모" 표 T6 열: 사진이 원 출처에서 바로 보이고, "그날 찍힌 사진" 아래에는 캡션이 "같은 날"인 사진만, 촬영 시점 미상 사진은 그 묶음 밖에서 "촬영 시점 미상". (3) 캡션마다 출처·라이선스(줄 또는 `attribution` 안)·촬영 시점, `attribution` 에 라이선스 이름이 있으면 줄이 따로 없고 "라이선스" 링크가 있다. (4) 키보드 Tab으로 "원문 보기"에 포커스 링, 화면 읽기 프로그램(선택)으로 "새 창에서 열림"이 들리고 사진 설명이 두 번 읽히지 않는다. (5) 문제가 있으면 오케스트레이터에게 알린다 — `fix/` 브랜치로 |

완료 기준에 쓰는 명령 (저장소 루트). C1~C6은 `docs/plans/m3b2-world-events-ui.md` 의 같은 이름 명령을 그대로 쓴다.

```
# S1 — 제품 코드 줄 수 (M3b2 S1 + e2e 제외, .github 포함)
git diff --numstat main...HEAD -- web .github \
  ':(exclude)web/package-lock.json' \
  ':(exclude,glob)web/src/**/*.test.ts' ':(exclude,glob)web/src/**/*.test.tsx' \
  ':(exclude,glob)web/src/mocks/fixtures/**' ':(exclude,glob)web/e2e/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수 (e2e 포함)
git diff --numstat main...HEAD -- ':(glob)web/src/**/*.test.ts' ':(glob)web/src/**/*.test.tsx' ':(glob)web/e2e/**' \
  | awk '{a+=$1} END {print "test added", a}'
# E1 — e2e (로컬, 처음 한 번 브라우저 설치: npx playwright install chromium)
cd web && npm run e2e
```

담당별: frontend-dev 4 (T0, T1, T2, T3), test-engineer 1 (T4), PM 2 (T5, T6).

## 리스크와 확인할 것

1. **크기가 400에 가깝다** — 다시 셈 약 315, M3b2 비율이면 약 375, 범위 위쪽 약 445. T2 뒤 확인점(누적 S1 300)에서 넘으면 T3을 M3b2c로 뺄지 다시 묻는다(결정 3 (a)).
2. **실제 사진 확인이 늦다** — 서버는 M3a3b 머지 전까지 사진을 보내지 않는다. 묶음·캡션 규칙은 실제 Commons 예시 값으로 만든 fixture로만 확인되고, 실제 화면은 T6이다. T6에서 발견되는 것은 `fix/` 로 고친다.
3. **비극 블록은 실제 화면으로 보기 어렵다** — 데모 3개 날짜에는 `tragic` 사건이 없다. Playwright(합성 응답)로 보고, 실제 화면은 표본 버튼이 생기는 M3b3 뒤에 본다.
4. **응답 모양 검사가 서버 계약을 한 번 더 적는다** — 화면이 읽는 필드만 보므로 M3a4가 `selection` 을 더해도 영향이 없다. 화면이 새 필드를 읽게 되는 기능(M3b3 영상 등)이 검사 목록도 고친다.
5. **그리는 코드의 버그는 아직 화면 전체를 깰 수 있다** — 결정 1 (a)로 ErrorBoundary는 M6b다. 이 기능의 모양 검사는 서버 응답 모양만 막는다.
6. **`:focus-visible` 판정은 브라우저 휴리스틱** — 마우스 클릭 뒤 포커스 링이 보이는지는 브라우저마다 다를 수 있다. Chromium 기준으로만 확인하고, 다르면 보고한다.
7. **라이선스 이름 포함 판단** — M3a3b의 Commons `attribution` 은 " · {라이선스} · Wikimedia Commons" 형식이라 실제 화면에서는 라이선스 줄이 대부분 숨는다. 이름이 `attribution` 에 그대로 있을 때만 숨기므로 정보가 빠지지 않는다. "라이선스 확인 안 됨"은 `attribution` 에 같은 말이 있어도 늘 보여 두 번 읽힐 수 있다(정직한 표시를 먼저 둔 의도된 동작).
8. **기존 테스트 기대값 변경** — `alt`·링크 이름·라이선스 줄·비극 미표시 기대값이 바뀐다. 테스트를 지우지 않고 기대값만 바꾸며, 바꾼 것을 보고에 표로 남긴다.
9. **e2e 타입 검사와 모듈 해석** — e2e가 `src` 의 fixture 모듈(확장자 없는 상대 import)을 가져오므로 `nodenext` 설정(`tsconfig.node.json`)에 넣으면 오류가 난다. `bundler` 해석의 별도 tsconfig로 한다.
10. **Playwright 설치·CI 시간** — Chromium 내려받기로 `web-ci` 가 1~2분 는다(D3 (a)에서 받아들임). `web-ci` 시간 상한 15분 안이다.
11. **web-ci 경로 공백은 이 PR 머지까지 남는다** — 그전에 `data/schema/examples/media/valid/` 만 바뀌는 PR(서버 기능)이 머지되면 웹 시험이 돌지 않는다. 깨지면 이 PR의 `web-ci`(전체 웹 시험)에서 드러난다(결정 2 (a)에서 받아들임).

## PM 결정 (2026-10-13, 기록)

| # | 물은 것 | 결정 | 이 계획서에 반영 |
|---|---|---|---|
| 1 | 모양이 틀린 사건이 화면 전체를 깨는 문제(M3b2 리뷰 권장 1) | **(a)** T0에서 응답 모양 검사만. 섹션 ErrorBoundary는 M6b. planner 추천과 같음. 고르지 않은 것: (b) 검사 + ErrorBoundary 지금, (c) ErrorBoundary만 | 목표 0, 범위 밖, "응답 모양 검사", T0, 리스크 5 |
| 2 | `web-ci` 경로에 `data/schema/examples/media/valid/**` | **(a)** 이 기능 T3에 넣는다. planner 추천과 같음. 고르지 않은 것: (b) 별도 `chore/` PR | 목표 4, "Playwright 설정", T3 (9), 리스크 11 |
| 3 | 크기(다시 셈 약 315, 범위 315~445, M3b2 비율이면 약 375) | **(a)** 한 PR + T2 뒤 확인점(누적 S1 300 — 넘으면 T3을 M3b2c로 뺄지 다시 물음). planner 추천과 같음. 고르지 않은 것: (b) 지금 둘로(M3 PR 20개), (c) 확인점 없이 한 PR | "크기", 작업 순서의 확인점, T2 (5), 리스크 1 |
| 4 | `same_day`·`unknown` 이 아닌 `day_media`(같은 달, ±N년) 위치 | **(a)** 세 번째 소제목 "다른 때 찍힌 사진". planner 추천과 같음. 고르지 않은 것: (b) "촬영 시점을 확인하지 못한 사진" 아래, (c) 그리지 않음 | 목표 1, 화면 구조·문구, 데모 표, T1 (1)·(2) |

이전 결정: M3-P17 (a), M3b1b 리뷰 후속 4건과 라이선스 줄 (b)(2026-10-11), 분할(로드맵 D1 (a))과 Playwright CI(D3 (a))(2026-10-12), M3b2 결정 2 (a)의 "실제 사진 확인은 머지 뒤" 방식(2026-10-13). 남은 질문은 없다.

★ PM 직접 구현: 없음 — 이 기능에는 백엔드 작업이 없다.
