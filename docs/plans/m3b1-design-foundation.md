# 디자인 기초와 미디어 문구·타입 (M3b1)

> 브랜치: `feat/design-foundation` · 작성: planner · 상태: PM 승인 (2026-10-11, 미디어 결정 반영 개정) · **2026-10-11 PM 결정으로 분할**: 원래 T3(`MediaItem`, `MediaCaption`, `MediaFigure` + 카드 CSS)은 새 기능 M3b1b `feat/media-card`(`docs/plans/m3b1b-media-card.md`)로 옮겼다
> 기준: `docs/product-brief.md` 2절·5절·6절(6.1 ±N년 표기, 6.2 촬영 시점 미상 표시)·8절, `docs/roadmap.md` M3, PM 결정 2026-10-10 디자인 방향 (b), **PM 미디어 결정 2026-10-11**(사진·영상은 링크로 처리하지 않고 바로 표시·재생, 라이선스 미확인도 직접 표시), **`CLAUDE.md` 7절 1항(촬영 시점을 모르면 "촬영 시점 미상"으로 표시하고 그날 것처럼 보이게 하지 않는다, 출처가 비면 숨긴다 — PM 결정 2026-10-11)**, 안전장치 1~6(PM 확인), `docs/plans/m1b-bootstrap-web.md`, `docs/plans/m3a3-event-media.md`("공용 미디어 형식")
> 진행: `web/` 만 바꾸므로 서버 기능과 **병렬**(worktree).

## 2026-10-11 분할 (PM 결정)

| 항목 | 내용 |
|---|---|
| 이 기능에 남은 것 | T1 토큰·장 레이아웃(완료, b51a5fb 93b564d), T2 미디어 타입·`dateLabel`·`licenseLabel`·`isDisplayable`(완료, 6ee093a), 리뷰 후속(557192f: `approximate` 배지 문구), **T4** 완료 기준 검증, T5 PM 화면 확인 |
| 옮긴 것 | 원래 T3(`MediaItem`, `MediaCaption`, `MediaFigure` + 카드 CSS, 약 205줄) → M3b1b `feat/media-card`. M3b1 리뷰 권장 3·4(대비 테스트 추가, 링크 `:focus-visible`)도 M3b1b로 |
| 실제 제품 코드 줄 수 (S1) | T1 107 + T2 202 = **309줄**. T3까지 넣으면 약 515줄이라 "T2 뒤 250줄 넘으면 T3을 뺀다" 규칙대로 뺐다 |
| 작업 번호 | 검증 작업은 **T4 그대로**, PM 확인은 T5 그대로 둔다(M3a1과 같은 방식). "M3b1 T3"은 옮긴 카드 작업을 뜻한다 |
| T2에서 나온 메모 | (1) `isDisplayable` 에서 주소 필드가 `http(s)` 인지 확인 → M3b1b T1. 서버도 같은 규칙(M3a3 R9). (2) M3a3 서버 DTO와 `web/src/media/types.ts` 대조 → M3b2 T1(M3a3 T4가 서버 필드 목록을 보고) |
| 촬영 시점 미상 (T2의 PM 결정 필요 1) | PM 결정 2026-10-11: 보여 주되 "촬영 시점 미상"으로, 그날 것처럼 보이게 하지 않는다. T2 구현(배지 없음 + "촬영 시점 미상")이 이미 이 결정과 같다 |

## 목표

1. 디자인 방향(아래)을 따른 토큰 `web/src/styles/tokens.css` 와 기존 CSS의 토큰 이전. 글자 색 대비는 테스트로 지킨다(WCAG AA).
2. 결과 화면 섹션이 "장(챕터)"처럼 보이는 공용 레이아웃(`StorySection`).
3. 미디어 타입과 캡션 문구 함수(`dateLabel`, `licenseLabel`)와 표시 가능 판정(`isDisplayable`). 카드 컴포넌트는 M3b1b가 이것을 쓴다.

## 범위 밖

- `MediaItem`·`MediaCaption`·`MediaFigure`·카드 CSS, 링크 `:focus-visible`, 추가 대비 테스트 → **M3b1b**
- 영상·iframe 임베드 그리기, 화면에 보일 때 재생 → M3b3
- 그날의 세계 섹션, API 호출, 데모 날짜 버튼 → M3b2
- 지도 SDK 미디어(`sdk`) → M4
- 장 전환 효과, 진행 표시, 다크 모드, 웹 폰트 → M6a
- 화면 전환 포커스 이동 → M2b
- 사진 확대 보기

## 사용할 외부 출처

없음. 미디어 주소는 API 데이터로만 들어온다.

## 디자인 방향 (확정 — D1 A, D2 (a), D3 (a), D4 답 없음)

### 분위기

- **차분한 다큐멘터리, 오래된 앨범의 온기**. 뭉클함은 여백·글·정직한 캡션에서 나오게 한다.
- 생일 축하 장식(컨페티, 이모지, 밝은 강조색)을 쓰지 않는다(같은 화면에 비극 사건이 놓일 수 있다).
- 사진은 원본 그대로: **필터·색 보정·세피아·자르기를 하지 않는다**(6.1절).

팔레트 A "종이와 잉크" (대비는 WCAG 상대 휘도 계산값):

| 토큰 | 값 | 쓰임 | 대비 |
|---|---|---|---|
| `--color-bg` | `#FAF7F2` | 화면 바탕 | - |
| `--color-surface` | `#FFFFFF` | 카드 바탕 | - |
| `--color-text` | `#1F1D1A` | 본문 | bg 대비 약 16 |
| `--color-muted` | `#5E5850` | 캡션·출처 | bg 대비 약 6.6 |
| `--color-border` | `#E4DDD2` | 카드 테두리(장식) | - |
| `--color-accent` | `#2F4A6D` | 링크·제목 장식 | bg 대비 약 8.5 |
| `--color-focus` | `#0B63CE` | 포커스 링 | bg 대비 약 5.3 (비텍스트 3 이상) |
| `--color-tragic-bg` | `#EEF0F2` | 비극 블록 바탕 | - |
| `--color-tragic-text` | `#2B2F33` | 비극 블록 글 | tragic-bg 대비 약 11.8 |

### 글꼴·크기·간격·움직임

- 시스템 한글 글꼴만(웹 폰트 없음). 크기(rem): `--font-size-caption` 0.875, `-body` 1, `-lead` 1.125, `-h2` 1.375, `-h1` 1.75, `-display` 2(M2b 오프닝용 예약). 줄 간격 본문 1.7, 제목 1.3.
- 간격 4px 단위 `--space-1`~`--space-8`(4, 8, 12, 16, 24, 32, 48, 64px). `--radius-card` 4px, 그림자 없음, 테두리 1px.
- 움직임 `--duration-short` 150ms, `--duration-base` 300ms, `--easing-standard`. `prefers-reduced-motion: reduce` 면 전역으로 전환·애니메이션을 끈다(M3b3의 자동 재생 예외 규칙도 이 설정을 본다).

### 넘기는 방식

세로 스크롤 + 장 구분: 섹션마다 위쪽 얇은 구분선과 장 제목, 장 사이 `--space-8`. M6a에서 등장 효과·진행 표시를 더할 수 있다.

### 미디어 카드 모양 (구현은 M3b1b)

```
┌────────────────────────────────┐
│        사진 (원본 비율,          │  ← 자르지 않음(object-fit: contain), 최대 높이 70vh, 필터 없음
│        원 출처에서 바로)          │
├────────────────────────────────┤
│ [같은 날] 1988년 9월 17일 촬영     │  ← 날짜 관계 배지 + 촬영 시점 (가장 먼저)
│ 1988년 서울 올림픽 개막식          │  ← 설명 (있을 때)
│ U.S. Air Force · Public domain │  ← 출처 문구
│ 라이선스 확인 안 됨 (해당 시)       │  ← license_status unknown 일 때
│ 원문 보기 · 라이선스               │  ← 링크 (라이선스 링크는 license_url 있을 때)
└────────────────────────────────┘
```

촬영 시점을 모르는 사진은 배지 칸이 비고 첫 줄이 "촬영 시점 미상"이다. "같은 날"처럼 요청 날짜를 떠올리게 하는 배지·문구를 붙이지 않는다(PM 결정 2026-10-11). M3b3의 영상·iframe 카드도 같은 틀(위: 미디어, 아래: 같은 캡션)을 쓴다. 비극 사건 블록(M3b2)은 `--color-tragic-*`, 강조색 없음, 미디어 없음.

### 날짜 관계 문구 (M3a3 `date_relation` → 화면)

| `kind` | 배지 | 촬영 시점 문구 |
|---|---|---|
| `same_day` | 같은 날 | `1988년 9월 17일 촬영` |
| `same_month` | 같은 달 | 정밀도에 맞춰 `1988년 9월 2일 촬영` 또는 `1988년 9월 촬영` |
| `same_year` | 같은 해 | `1988년 촬영`(정밀도에 맞춰 월·일까지) |
| `years_apart` 음수 / 양수 | `N년 전` / `N년 뒤` | `1987년 촬영` |
| `approximate` 음수 / 양수 / 0 | `N년 전 무렵` / `N년 뒤 무렵` / `같은 해 무렵` (557192f — 6.1절 ±N년, D1 "N년 전 / N년 뒤"와 맞춤) | `1988년 무렵 촬영(추정)` |
| `current` | 현재 모습 | `촬영 시점 미제공` (#17 (a)) |
| `unknown` | (배지 없음) | `촬영 시점 미상` |

라이선스 문구: `license_status` 가 `unknown` 이면 "라이선스 확인 안 됨", 그 밖은 `license` 값.

## API / 데이터 변경

서버 변경 없음. `web/src/media/types.ts` 는 M3a3 "공용 미디어 형식" 필드 표 + `date_relation` 과 이름·형이 같다(snake_case 그대로, 모든 `display_mode` 값 포함). M3a3이 머지되면 M3b2 T1이 서버 DTO와 다시 대조한다.

| 함수 | 받는 것 | 하는 일 |
|---|---|---|
| `isDisplayable(media)` | API 미디어 | 서버 정규화를 한 번 더 확인: R1(출처·캡션 필드)과 모드별 필수 필드(`image` 는 `url`·`thumbnail_url`)를 못 채우면 `false`. `captured_at`·`date_precision`·`license` 가 비어도 `true`. 링크로 바꾸지 않는다. (주소 형식 확인은 M3b1b T1에서 더한다) |
| `dateLabel(media)` | API 미디어 | `{ badge: string \| null, text: string }` — 위 표 |
| `licenseLabel(media)` | API 미디어 | `license` 또는 "라이선스 확인 안 됨" |

## 테스트 방침

M1b "테스트 방침"을 따른다. 목 데이터는 `web/src/mocks/fixtures/media/`. 대비 테스트는 `tokens.css` 를 읽어 계산한다.

## 데모 입력 3개로 확인할 것 (목 데이터, 함수 수준 — 화면은 M3b1b·M3b2)

| 데모 | 확인 |
|---|---|
| 1988-09-17 | `same_day` 목 → `dateLabel` 이 배지 "같은 날", 문구 "1988년 9월 17일 촬영". 촬영 시점을 읽지 못한 목(값이 둘인 파일) → `isDisplayable` `true`, 배지 `null`, 문구 "촬영 시점 미상" |
| 2002-06-22 | `KOGL Type 1` 목 → `licenseLabel` 이 라이선스 이름 |
| 1995-03-15 | 미디어 없음 — 확인할 것 없음 |

## 크기 (400줄 규칙)

| 작업 | 예상 | 실제 (S1) |
|---|---|---|
| T1 토큰·CSS 이전·장 레이아웃 | 약 110 | 107 |
| T2 타입·문구·판정 | 약 115 | 202 |
| 합계 | 약 225 (T3 포함 약 370) | **309** (+ 557192f 리뷰 후속분) |

## 작업 목록

순서: T1 → T2 → T4 → T5. (T3은 M3b1b로 옮김)

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 디자인 토큰, 기존 CSS 이전, 장 레이아웃, `SectionId` 타입 **(완료, b51a5fb 93b564d)** | frontend-dev | `web/src/styles/tokens.css`, `global.css`, 기존 `*.module.css`, `StorySection.tsx`·`.module.css`, `web/src/styles/tokens.test.ts` | - | (1) "디자인 방향"의 토큰 이름이 모두 있다. (2) C1, C2 출력 없음, C3 출력은 사람이 확인. (3) `tokens.test.ts`: text·muted·accent 대 bg, text·muted 대 surface, tragic-text·muted 대 tragic-bg 가 4.5 이상, focus 대 bg 가 3 이상. 계산 함수 단위 테스트(기준 아래 값은 실패). (4) `global.css` 에 `prefers-reduced-motion: reduce` 블록. (5) `StorySection` 의 `id` 형이 `SectionId`(C4), 장 구분선·여백 토큰 사용. (6) 기존 테스트 그대로 통과. (7) M1b 전체 검사 통과 |
| T2 | 미디어 타입, `dateLabel`, `licenseLabel`, `isDisplayable` **(완료, 6ee093a; 리뷰 후속 557192f)** | frontend-dev | `web/src/media/types.ts`, `dateLabel.ts`, `licenseLabel.ts`, `isDisplayable.ts`, 단위 테스트, `web/src/mocks/fixtures/media/*` | T1 | (1) 타입이 M3a3 필드 표 + `date_relation` 과 이름·형이 같다(보고서에 대조표). (2) `dateLabel`: 문구 표 행마다 테스트(경계값 `2000-02-29` 기준 `same_month` 포함, `approximate` 는 음수·양수·0 세 경우 — "N년 전 무렵"·"N년 뒤 무렵"·"같은 해 무렵"). (3) `licenseLabel`: `unknown` → "라이선스 확인 안 됨", 그 밖 → `license`. (4) `isDisplayable`: R1 필드 하나가 비면 `false`(출처 필드 포함), `image` 에서 `url`·`thumbnail_url` 이 비면 `false`, `license` null·`date_precision` null은 `true`. (5) `unknown` 의 배지는 `null` 이고 문구에 날짜가 없다 |
| ~~T3~~ | → **M3b1b `feat/media-card` 로 옮김** (`MediaItem`, `MediaCaption`, `MediaFigure` + 카드 CSS) | - | - | - | - |
| T4 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T2 | (1) T1·T2 기준 표. (2) 전체 검사 통과, `npm test` 3회 반복 안정. (3) C7 출력 `0`. (4) S1·S2 보고(S1은 309줄 + 557192f분 — 테스트만 더하므로 바뀌면 안 된다) |
| T5 | 화면 확인 (PR 검수 때) | PM | - | T4 | `cd web && npm run dev` 후 360px 폭에서 입력 화면과 빈 결과 화면이 새 바탕색·글자·장 구분으로 보이고 가로 스크롤이 없다. 카드는 M3b1b·M3b2 PR에서 본다 |

명령 (저장소 루트):

```
# C1 — 색 값은 tokens.css 에만. 출력 없음
grep -rnE '#[0-9a-fA-F]{3,8}\b' web/src --include='*.css' | grep -v 'web/src/styles/tokens.css'
# C2 — 글자 크기는 토큰으로만. 출력 없음
grep -rn 'font-size' web/src --include='*.css' | grep -v 'var(--' | grep -v 'web/src/styles/tokens.css'
# C3 — 간격 px 직접 사용 (테두리 1px 제외). 사람이 확인
grep -rnE '(margin|padding|gap)[^;]*[0-9]+px' web/src --include='*.css' | grep -v 'web/src/styles/tokens.css'
# C4 — StorySection id 형
grep -n 'SectionId' web/src/story/StorySection.tsx
# C7 — act 경고 없음. 0
cd web && npm test 2>&1 | grep -c 'not wrapped in act'

# S1 — 제품 코드·설정 줄 수 (glob 보정판)
git diff --numstat main...HEAD -- web \
  ':(exclude)web/package-lock.json' \
  ':(exclude,glob)web/src/**/*.test.ts' ':(exclude,glob)web/src/**/*.test.tsx' \
  ':(exclude,glob)web/src/test/**' ':(exclude,glob)web/src/mocks/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트·목 줄 수
git diff --numstat main...HEAD -- \
  ':(glob)web/src/**/*.test.ts' ':(glob)web/src/**/*.test.tsx' ':(glob)web/src/test/**' ':(glob)web/src/mocks/**' \
  | awk '{a+=$1} END {print "test added", a}'
```

(C5·C6·C8은 카드 컴포넌트용이라 M3b1b 계획서로 옮겼다.)

담당별: frontend-dev 2 (T1, T2 — 완료), test-engineer 1 (T4), PM 1 (T5).

## 리스크와 확인할 것

1. **방향을 글로 정했다** — 토큰에 모아 두어 M3b2 PR에서 값만 바꾸는 수정은 싸다.
2. **M3a3 형식과 어긋날 수 있다** — 병렬이다. M3a3 계획서 표를 원천으로 삼고, M3b2 T1에서 서버 DTO와 한 번 더 맞춘다.
3. **화면으로 볼 것이 적다** — 카드가 M3b1b로 옮겨져 이 PR의 PM 화면 확인은 토큰·장 구분뿐이다.

## PM이 정할 것

없음 (D1~D4, M3-P4 결정됨. 촬영 시점 미상 표시와 분할은 2026-10-11 결정).
