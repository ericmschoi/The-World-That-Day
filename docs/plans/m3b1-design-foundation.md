# 디자인 기초와 공용 미디어 카드 (M3b1)

> 브랜치: `feat/design-foundation` · 작성: planner · 상태: PM 승인 (2026-10-11, 미디어 결정 반영 개정)
> 기준: `docs/product-brief.md` 2절·5절·6절·8절, `docs/roadmap.md` M3, PM 결정 2026-10-10 디자인 방향 (b), **PM 미디어 결정 2026-10-11**(사진·영상은 링크로 처리하지 않고 바로 표시·재생, 라이선스 미확인도 직접 표시), 안전장치 1~6(PM 확인), `docs/plans/m1b-bootstrap-web.md`, `docs/plans/m3a3-event-media.md`("공용 미디어 형식")
> 진행: `web/` 만 바꾸므로 서버 기능과 **병렬**(worktree). 계획 승인 직후 `main` 에서 갈라질 수 있다.

## 2026-10-11 개정 요약

- 링크 카드(`LinkOnlyCard`)와 "확인 안 되면 링크로 내리기"를 **뺐다**. 카드는 미디어를 늘 바로 보여 준다.
- 라이선스를 모르면 캡션에 "라이선스 확인 안 됨", 촬영 시점을 모르면 "촬영 시점 미상"을 그대로 쓴다(안전장치 2).
- 이 기능의 카드는 `display_mode: image`(사진)를 그린다. 영상(`video_file`, `video_embed`)과 제공사 공식 이미지 임베드(`image_embed`)는 같은 `MediaItem` 입구에 M3b3 `feat/media-video` 가 붙인다. 그때까지 이 모드들은 그리지 않는다.

## 이 기능을 따로 두는 이유 (결정 M3-P4, 승인됨)

방향은 이 계획서의 "디자인 방향" 절로 계획 승인 때 확정되고(카드 코드보다 먼저), 구현은 서버와 병렬이라 일정 손해가 없다. M4·M5가 기대는 것은 카드라서 의존이 깔끔하다(M4a·M5c → M3b1). 최종 다듬기는 M6a.

## 목표

1. 디자인 방향(아래)을 따른 토큰 `web/src/styles/tokens.css` 와 기존 CSS의 토큰 이전. 글자 색 대비는 테스트로 지킨다(WCAG AA).
2. 결과 화면 섹션이 "장(챕터)"처럼 보이는 공용 레이아웃(`StorySection`).
3. 공용 미디어 카드 입구 `MediaItem` 과 사진 카드 `MediaFigure`. 캡션(날짜 관계, 촬영 시점, 출처, 라이선스 상태)을 늘 함께 그린다(`CLAUDE.md` 7절 1항, 제품 정의서 6절, 안전장치 2).

## 범위 밖

- 영상·iframe 임베드 그리기, 화면에 보일 때 재생 → M3b3
- 그날의 세계 섹션, API 호출, 데모 날짜 버튼 → M3b2
- 지도 SDK 미디어(`sdk`) → M4
- 장 전환 효과, 진행 표시, 다크 모드, 웹 폰트 → M6a
- 화면 전환 포커스 이동 → M2b
- 사진 확대 보기

## 사용할 외부 출처

없음. 미디어 주소는 API 데이터로만 들어온다. 이미지는 브라우저가 원 출처(`thumbnail_url`)에서 바로 받는다(안전장치 1, `referrerPolicy="no-referrer"`).

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

### 미디어 카드 모양 (사진 — `MediaFigure`)

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

M3b3의 영상·iframe 카드도 같은 틀(위: 미디어, 아래: 같은 캡션)을 쓴다. 비극 사건 블록(M3b2)은 `--color-tragic-*`, 강조색 없음, 미디어 없음.

### 날짜 관계 문구 (M3a3 `date_relation` → 화면)

| `kind` | 배지 | 촬영 시점 문구 |
|---|---|---|
| `same_day` | 같은 날 | `1988년 9월 17일 촬영` |
| `same_month` | 같은 달 | 정밀도에 맞춰 `1988년 9월 2일 촬영` 또는 `1988년 9월 촬영` |
| `same_year` | 같은 해 | `1988년 촬영`(정밀도에 맞춰 월·일까지) |
| `years_apart` 음수 / 양수 | `N년 전` / `N년 뒤` | `1987년 촬영` |
| `approximate` | 무렵 | `1988년 무렵 촬영(추정)` |
| `current` | 현재 모습 | `촬영 시점 미제공` (#17 (a)) |
| `unknown` | (배지 없음) | `촬영 시점 미상` |

라이선스 문구: `license_status` 가 `unknown` 이면 "라이선스 확인 안 됨", 그 밖은 `license` 값.

## API / 데이터 변경

서버 변경 없음. `web/src/media/types.ts` 는 M3a3 "공용 미디어 형식" 필드 표 + `date_relation` 과 이름·형이 같다(snake_case 그대로, 모든 `display_mode` 값 포함).

| 컴포넌트·함수 | 받는 것 | 하는 일 |
|---|---|---|
| `isDisplayable(media)` | API 미디어 | 서버 정규화를 한 번 더 확인: R1(캡션 필드)과 모드별 필수 필드(`image` 는 `url`·`thumbnail_url`)를 못 채우면 `false`. 링크로 바꾸지 않는다 |
| `dateLabel(media)` | API 미디어 | `{ badge: string \| null, text: string }` — 위 표 |
| `licenseLabel(media)` | API 미디어 | `license` 또는 "라이선스 확인 안 됨" |
| `MediaItem` | `media`, `fallbackAlt` | `isDisplayable` 이 `false` 면 아무것도 그리지 않는다. `image` → `MediaFigure`. `image_embed`·`video_file`·`video_embed` → M3b3 전까지 그리지 않음(M3b3가 이 분기를 채운다). `sdk` → M4. `link_only` → 그리지 않음(M3에서는 서버가 만들지 않는다) |
| `MediaCaption` | API 미디어 | 배지, 촬영 시점, 설명, 출처 문구, 라이선스 문구, "원문 보기"(`source_url`)·"라이선스"(`license_url` 있을 때) 링크. M3b3도 재사용 |
| `MediaFigure` | `image` 미디어, `alt` | `<figure>` + `<img>`(`src`=`thumbnail_url`, `alt`=`description` 또는 `fallbackAlt`, `loading="lazy"`, `decoding="async"`, `referrerPolicy="no-referrer"`, 크기가 있으면 `width`·`height`) + `MediaCaption` |

외부 링크는 `target="_blank"` + `rel="noopener noreferrer"`.

## 테스트 방침

M1b "테스트 방침"을 따른다. 목 데이터는 `web/src/mocks/fixtures/media/`. 비동기 동작을 시험하면 마지막 상태를 `findBy*`/`waitFor` 로 기다린다. 대비 테스트는 `tokens.css` 를 읽어 계산한다.

## 데모 입력 3개로 확인할 것 (목 데이터)

| 데모 | 확인 |
|---|---|
| 1988-09-17 | `same_day` 사진 → 이미지와 "같은 날", "1988년 9월 17일 촬영", 출처, 두 링크. 촬영 시점을 읽지 못한 사진(값이 둘인 파일) → **이미지가 그대로 보이고** "촬영 시점 미상" |
| 2002-06-22 | `KOGL Type 1` 사진 → 라이선스 이름이 캡션에 |
| 1995-03-15 | 미디어 없음 — 확인할 것 없음 |

## 크기 (400줄 규칙)

예상: `tokens.css` 약 60, 기존 CSS 바꿈 약 30, `StorySection` 장 레이아웃 약 20, 미디어 타입 약 50, `dateLabel`·`licenseLabel` 약 45, `isDisplayable` 약 20, `MediaCaption` 약 40, `MediaFigure` 약 30, `MediaItem` 약 15, 카드 CSS 약 60 → **약 370줄**. T2 뒤 누적 250줄을 넘으면 T3을 뺄지 PM에게 묻는다.

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 디자인 토큰, 기존 CSS 이전, 장 레이아웃, `SectionId` 타입 | frontend-dev | `web/src/styles/tokens.css`, `global.css`, 기존 `*.module.css`, `StorySection.tsx`·`.module.css`, `web/src/styles/tokens.test.ts` | - | (1) "디자인 방향"의 토큰 이름이 모두 있다. (2) C1, C2 출력 없음, C3 출력은 사람이 확인. (3) `tokens.test.ts`: text·muted·accent 대 bg, text·muted 대 surface, tragic-text·muted 대 tragic-bg 가 4.5 이상, focus 대 bg 가 3 이상. 계산 함수 단위 테스트(기준 아래 값은 실패). (4) `global.css` 에 `prefers-reduced-motion: reduce` 블록. (5) `StorySection` 의 `id` 형이 `SectionId`(C4), 장 구분선·여백 토큰 사용. (6) 기존 테스트 그대로 통과. (7) M1b 전체 검사 통과 |
| T2 | 미디어 타입, `dateLabel`, `licenseLabel`, `isDisplayable` | frontend-dev | `web/src/media/types.ts`, `dateLabel.ts`, `licenseLabel.ts`, `isDisplayable.ts`, 단위 테스트, `web/src/mocks/fixtures/media/*` | T1 | (1) 타입이 M3a3 필드 표 + `date_relation` 과 이름·형이 같다(보고서에 대조표). (2) `dateLabel`: 문구 표 행마다 테스트(경계값 `2000-02-29` 기준 `same_month` 포함). (3) `licenseLabel`: `unknown` → "라이선스 확인 안 됨", 그 밖 → `license`. (4) `isDisplayable`: R1 필드 하나가 비면 `false`, `image` 에서 `url`·`thumbnail_url` 이 비면 `false`, `license` null·`date_precision` null은 `true` |
| T3 | `MediaItem`, `MediaCaption`, `MediaFigure` + 카드 CSS | frontend-dev | `web/src/media/MediaItem.tsx`, `MediaCaption.tsx`, `MediaFigure.tsx`, `*.module.css`, 컴포넌트 테스트 | T2 | (1) `MediaFigure`: `img` 의 `alt` 가 `description`(없으면 `fallbackAlt`), `src`=`thumbnail_url`, `loading`=`lazy`, `referrerpolicy`=`no-referrer`. 캡션에 "같은 날", "1988년 9월 17일 촬영", 출처 문구, "원문 보기" `href`=`source_url`, "라이선스" `href`=`license_url`(없으면 링크 없음). (2) `license_status: unknown` 목 → 이미지가 보이고 "라이선스 확인 안 됨". `date_precision: null` 목 → 이미지가 보이고 "촬영 시점 미상". (3) `MediaItem`: R1을 못 채운 목 → 아무것도 없음, `video_embed`·`image_embed`·`sdk`·`link_only` 목 → 아무것도 없음(M3b3·M4 전). (4) 외부 링크 `rel` 에 `noopener`·`noreferrer`. (5) C5, C6, C8 출력 없음. (6) 전체 검사 통과, S1 결과 |
| T4 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T3 | (1) T1~T3 기준 표. (2) 전체 검사 통과, `npm test` 3회 반복 안정. (3) C7 출력 `0`. (4) S1·S2 보고 |
| T5 | 화면 확인 (PR 검수 때) | PM | - | T4 | `cd web && npm run dev` 후 360px 폭에서 입력 화면과 빈 결과 화면이 새 바탕색·글자·장 구분으로 보이고 가로 스크롤이 없다. 카드는 M3b2 PR에서 실제 데이터로 본다 |

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
# C5 — 사진에 필터·자르기 없음. 출력 없음
grep -rnE 'filter:|object-fit:[[:space:]]*cover|sepia|grayscale' web/src --include='*.css'
# C6 — 제품 코드에 외부 절대 URL 없음 (테스트·목 제외). 출력 없음
grep -rnE 'https?://' web/src web/index.html --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/mocks/'
# C7 — act 경고 없음. 0
cd web && npm test 2>&1 | grep -c 'not wrapped in act'
# C8 — 링크 카드가 남아 있지 않음. 출력 없음
grep -rn 'LinkOnlyCard' web/src

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

담당별: frontend-dev 3, test-engineer 1, PM 1.

## 리스크와 확인할 것

1. **방향을 글로 정했다** — 토큰에 모아 두어 M3b2 PR에서 값만 바꾸는 수정은 싸다.
2. **M3a3 형식과 어긋날 수 있다** — 병렬이다. M3a3 계획서 표를 원천으로 삼고, M3b2 시작 전에 한 번 더 맞춘다.
3. **원 출처 이미지가 깨질 수 있다** — 핫링크라 원 출처가 바뀌면 이미지가 안 뜬다. 이번에는 `img` 오류 시 처리를 넣지 않고, M6b(장애 격리)에서 본다.
4. **크기** — 한도 근처. T3을 나눌 기준을 두었다.

## PM이 정할 것

없음 (D1~D4, M3-P4 결정됨).
