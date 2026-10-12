# 그날 찍힌 사진·비극 블록과 카드 접근성 (M3b2b)

> 브랜치: `feat/world-photos-a11y` · 작성: planner · 상태: **PM 승인 (2026-10-12)** (M3b2 분할로 새로 생김, 로드맵 D1 (a). Playwright CI는 D3 (a))
> 기준: M3-P17 (a)(결정 2026-10-11), #22 (c), 제품 정의서 6.1·6.3절, `CLAUDE.md` 7절 1·5·6항, `docs/plans/m3b2-world-events-ui.md`, `docs/plans/m3b1b-media-card.md`, `docs/plans/m3b1-design-foundation.md`(토큰·문구), **M3b1b 리뷰 후속(PM 결정 2026-10-11)**: 라이선스 줄 중복은 (b), 새 창 안내, 설명 두 번 읽기 방지, `fallbackAlt` 확인, Playwright로 포커스 링·토큰 색 확인
> 선행: M3b2 `feat/world-events-ui` 머지(M3b1b 포함).

## 목표

1. `day_media` 를 M3-P17 (a)대로 묶는다: "그날 찍힌 사진" 제목 아래에는 `date_relation.kind = same_day` 만, 나머지는 "촬영 시점을 확인하지 못한 사진" 소제목 아래.
2. `tragic` 사건을 정상 사건 뒤의 차분한 별도 블록("같은 날 있었던 아픈 일")으로, 미디어 없이 보여 준다(6.3절, #22 (c)).
3. M3b1b 리뷰 후속(PM 결정)을 `MediaCaption`·`MediaFigure` 에 반영한다:
   - `attribution` 이 이미 라이선스 이름을 담고 있으면 캡션의 라이선스 줄을 숨긴다(선택지 (b)).
   - 캡션의 새 창 링크에 화면 읽기 프로그램용 "새 창에서 열림" 안내를 단다.
   - 같은 설명이 `alt` 와 캡션에서 두 번 읽히지 않게 한다.
   - 화면이 넘기는 `fallbackAlt` 에 "그날"을 떠올리게 하는 말이 없는지 테스트한다.
4. **Playwright**를 웹에 들여와(기술 스택의 E2E 도구) 실제 브라우저에서 확인한다: 키보드로는 포커스 링이 보이고 마우스 클릭으로는 보이지 않는다, 디자인 토큰 색이 실제로 칠해진다. `web-ci` 에 Chromium 하나로 돌리는 단계를 더한다(D3 (a)).

## 범위 밖

- 영상·임베드 재생 → M3b3·M3b3c
- 동의 단계 → M3b3b
- 데모·표본 날짜 전체 E2E → M6c (이 기능의 Playwright는 포커스·토큰 확인 두 가지와, 뒤 기능이 쓸 설정만)
- 원 출처 이미지가 깨질 때 처리 → M6b
- 서버 변경

## 사용할 외부 출처

없음. Playwright 테스트는 우리 화면만 띄우고, `/api/story/world` 는 `page.route` 로 MSW fixture JSON을 돌려주며, 그 밖의 외부 요청(원 출처 이미지 등)은 모두 막는다(`CLAUDE.md` 7절 5항 — 테스트는 외부 네트워크를 부르지 않는다).

## 화면 구조와 문구 (M3b2 화면에 더함)

```
그날의 세계
 [사건 …]                                  (M3b2)
 그날 찍힌 사진                            (h3, same_day 사진이 있을 때)
   [MediaItem × N]
 촬영 시점을 확인하지 못한 사진              (h3, 그 밖의 day_media 가 있을 때 — M3-P17 (a))
   [MediaItem × M]                        ← 캡션 "촬영 시점 미상", 배지 없음
 같은 날 있었던 아픈 일                     (h3, tragic 사건이 있을 때 — 차분한 회색 블록)
   · 문장 원문 + 출처 줄 (미디어 없음)
```

| 자리 | 문구 (PM이 T5에서 검토) |
|---|---|
| 그날 사진 소제목 | 그날 찍힌 사진 |
| 촬영 시점 미상 묶음 소제목 | 촬영 시점을 확인하지 못한 사진 |
| 비극 블록 소제목 | 같은 날 있었던 아픈 일 |
| 새 창 안내 (화면에는 안 보임) | (새 창에서 열림) |

- 비극 블록에는 강조색·축하 요소·미디어를 쓰지 않는다. `tragic` 사건에 `media` 가 와도 그리지 않는다. `normal` 사건이 없고 `tragic` 만 있으면 비극 블록만 보인다(빈 날짜 안내 없음).
- `day_media` 의 `fallbackAlt` 는 "기록 사진"처럼 날짜를 말하지 않는 문구를 쓴다.

## 캡션 접근성 규칙 (M3b1b 리뷰 후속)

| 규칙 | 내용 |
|---|---|
| 라이선스 줄 중복 (b) | `license_status` 가 `unknown` 이 아니고 `license` 값이 `attribution` 에 들어 있으면(대소문자·공백 무시) 라이선스 줄을 그리지 않는다. "라이선스" 링크(`license_url`)는 그대로 둔다. `license_status: unknown` 의 "라이선스 확인 안 됨"은 **늘** 보인다 |
| 새 창 안내 | `target="_blank"` 인 캡션 링크마다 시각적으로 숨긴 "(새 창에서 열림)" 글자를 링크 안에 넣는다(공용 `visually-hidden` 클래스). 접근 이름에 이 말이 들어간다 |
| 설명 두 번 읽기 | 캡션이 `description` 을 보여 주면 `img` 의 `alt` 는 `fallbackAlt` 를 쓴다(설명 전체가 `alt` 와 `figcaption` 에 둘 다 들어가지 않게). 캡션에 설명이 없으면 지금처럼 `alt` = `description` 또는 `fallbackAlt` |
| `fallbackAlt` | 화면(`WorldSection` 등)이 넘기는 값에 "그날", "같은 날", "당일", 요청 날짜 문자열(`YYYY-MM-DD`, "YYYY년 M월 D일", "M월 D일")이 없다 |

## Playwright 설정

- `web/` 에 `@playwright/test` 개발 의존성, `web/playwright.config.ts`, 테스트는 `web/e2e/*.spec.ts`, `npm run e2e` 스크립트.
- 브라우저는 Chromium 하나. `webServer` 는 `npm run dev`(5173). `serviceWorkers: 'block'`(MSW 서비스 워커가 있어도 `page.route` 가 요청을 받게).
- `/api/story/world` → `page.route` 로 `web/src/mocks/fixtures/world/` JSON. `localhost` 밖 요청은 모두 `abort`.
- 화면 폭 360px과 1280px 두 가지 프로젝트(뒤 기능의 YouTube 플레이어 크기 확인에도 쓴다, M3b3c).
- CI: `web-ci` 에 Chromium 하나로 `npm run e2e` 단계를 더한다(**D3 (a), PM 결정 2026-10-12**).
- S1(줄 수)에서 `web/e2e/**` 를 테스트로 보고 뺀다.

## 테스트 방침

M1b "테스트 방침"을 따른다. 컴포넌트 테스트는 Vitest·Testing Library, 실제 렌더링(포커스 링·계산된 색)은 Playwright. 비극 시험 응답의 문장은 누가 봐도 가짜로 쓴다("시험용 비극 사건 문장"). 날짜는 데모·표본 날짜만.

## 데모·표본 날짜로 확인할 것

| 날짜 | 자동 테스트 | PM 화면 확인 (T5) |
|---|---|---|
| 1988-09-17 | "그날 찍힌 사진" 아래에는 `same_day` 사진만 있고 그 안에 "촬영 시점 미상" 글자가 없다. `unknown` 사진은 "촬영 시점을 확인하지 못한 사진" 아래 `img` 로 보이고 "촬영 시점 미상". 비극 블록 없음. Commons 사진 캡션(`attribution` 에 "CC BY-SA 4.0")에 라이선스 줄이 따로 없고 "라이선스" 링크는 있다 | 실제 사진으로 같은 내용. 키보드 Tab으로 "원문 보기" 링크에 포커스 링이 보이고, 마우스로 누르면 보이지 않는다 |
| 2002-06-22 | `KOGL Type 1` 사진: `attribution` 에 그 이름이 없으면 라이선스 줄이 보인다 | 같은 내용 |
| 1995-03-15 | 안내 문장, 사진 묶음 제목 없음 | 같은 내용 |
| (합성) `tragic` 만 있는 응답 | 비극 블록만, 그 안에 `img`·`iframe`·`video` 없음(`media` 를 넣어 보내도) | (표본 1950-06-25·2014-04-16은 버튼이 없어 Playwright 결과로 본다) |
| (합성) 정상 + `tragic` | 정상 사건이 문서 순서상 앞, 비극 블록이 뒤 | - |

## 크기 (400줄 규칙)

예상: 사진 묶음 약 30, 비극 블록 약 30, 캡션 규칙(라이선스 줄·새 창 안내·`alt`) 약 30, `visually-hidden`·블록 CSS 약 25, Playwright 설정·스크립트·CI 약 35 → **약 150줄, 1.6~1.75배 보정 약 240~260줄**. `web/e2e/**` 테스트는 S1에서 뺀다.

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | `day_media` 묶음(M3-P17 (a))과 비극 블록 | frontend-dev | `WorldSection` 하위 컴포넌트, CSS, 컴포넌트 테스트 | M3b2 머지 | (1) 위 표 1988·1995·합성 행 통과. (2) `same_day` 가 없으면 "그날 찍힌 사진" 제목이 없고, 미상 사진이 없으면 그 소제목이 없다. (3) 비극 블록에 강조색 토큰을 쓰지 않는다(`--color-tragic-*` 만, CSS 확인). (4) `day_media`·사건 미디어에 넘기는 `fallbackAlt` 에 날짜 표현이 없다(목표 3의 테스트). (5) 전체 검사 통과 |
| T2 | 캡션 접근성: 라이선스 줄 (b), 새 창 안내, 설명 두 번 읽기 방지 | frontend-dev | `MediaCaption.tsx`, `MediaFigure.tsx`, `global.css`(`visually-hidden`), 테스트 | T1 | (1) `attribution` 이 `license` 를 담으면 라이선스 줄이 없고 "라이선스" 링크는 있다. 담지 않으면 줄이 있다. `license_status: unknown` 이면 "라이선스 확인 안 됨"이 늘 있다. (2) 캡션의 `target="_blank"` 링크마다 접근 이름에 "새 창에서 열림"이 있다(`getByRole('link', { name: /새 창에서 열림/ })`). (3) 설명이 있는 사진: `img` 의 접근 이름이 `fallbackAlt` 이고 `figcaption` 에 설명이 있다 — 설명 전체가 둘 다에 있지 않다. 설명이 없으면 `alt` = `fallbackAlt`. (4) M3b1b 기존 테스트가 새 규칙에 맞게 고쳐지고 통과(지운 테스트 없음 — 바꾼 기대값은 보고에 표로). (5) 전체 검사 통과 |
| T3 | Playwright 설정과 포커스·토큰 확인, `web-ci` e2e 단계 | frontend-dev | `web/package.json`, `web/playwright.config.ts`, `web/e2e/focus-and-tokens.spec.ts`, `.github/workflows/web-ci.yml` | T2 | (1) `cd web && npm run e2e` 통과(로컬, Chromium). (2) 키보드: 1988 화면에서 Tab으로 "원문 보기" 링크에 가면 계산된 `outline-style` 이 `none` 이 아니고 `outline-color` 가 `--color-focus` 값과 같다. (3) 마우스: 같은 링크를 마우스로 누르면(새 창은 닫음) `outline-style` 이 `none` 이거나 두께 0. (4) 토큰 색: `body` 배경이 `--color-bg`, 링크 글자가 `--color-accent`, 비극 블록 배경이 `--color-tragic-bg` 의 계산된 값과 같다(합성 비극 응답). (5) 테스트 중 `localhost` 밖 요청이 0건(가로챈 요청 기록으로 확인). (6) 360px 폭에서 가로 스크롤이 없다(`scrollWidth <= clientWidth`). (7) `web-ci` 에서 Chromium 하나로 e2e 단계가 돌고 통과한다 |
| T4 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T3 | (1) T1~T3 기준 표. (2) `npm test` 3회, `npm run e2e` 3회 반복 안정. (3) M3b2 C1·C2·C4 출력 없음, C3 `0`. (4) S1(아래)·S2 보고 |
| T5 | 실제 서버로 화면 확인 (PR 검수 때) | PM | - | T4 | 데모 버튼 3개로 "PM 화면 확인" 열, 문구 검토, 키보드 Tab·마우스 클릭으로 포커스 링 차이, 화면 읽기 프로그램(선택, macOS VoiceOver)으로 링크를 읽을 때 "새 창에서 열림"이 들리고 사진 설명이 두 번 읽히지 않는지 |

```
# S1 — 제품 코드 줄 수 (M3b2 S1 + e2e 제외, .github 포함)
git diff --numstat main...HEAD -- web .github \
  ':(exclude)web/package-lock.json' \
  ':(exclude,glob)web/src/**/*.test.ts' ':(exclude,glob)web/src/**/*.test.tsx' \
  ':(exclude,glob)web/src/mocks/fixtures/**' ':(exclude,glob)web/e2e/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# E1 — e2e (로컬, 처음 한 번 브라우저 설치: npx playwright install chromium)
cd web && npm run e2e
```

담당별: frontend-dev 3, test-engineer 1, PM 1.

## 리스크와 확인할 것

1. **Playwright 설치·CI 시간** — Chromium 내려받기로 `web-ci` 가 1~2분 는다(D3 (a)에서 받아들임).
2. **`:focus-visible` 판정은 브라우저 휴리스틱** — 마우스 클릭 뒤 포커스 링이 보이는지는 브라우저마다 다를 수 있다. Chromium 기준으로만 확인하고, 다르면 보고한다.
3. **라이선스 이름 포함 판단** — `attribution` 형식이 출처마다 다르다. 숨기는 것은 라이선스 이름이 `attribution` 에 그대로 있을 때만이라 정보가 빠지지 않는다.
4. **M3b1b 테스트 기대값 변경** — `alt` 규칙이 바뀐다. 테스트를 지우지 않고 기대값만 바꾸며, 바꾼 것을 보고에 표로 남긴다.

## PM이 정할 것

없음. 결정됨: M3-P17 (a), M3b1b 리뷰 후속 4건과 라이선스 줄 (b)(2026-10-11), 분할(로드맵 D1 (a))과 Playwright CI(D3 (a))(2026-10-12).
