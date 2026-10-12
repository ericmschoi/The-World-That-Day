# 공용 미디어 카드 (M3b1b)

> 브랜치: `feat/media-card` · 작성: planner · 상태: **PM 승인 (2026-10-11)** (2026-10-11 PM 결정 "M3b1 분할"로 생긴 기능. 내용은 승인된 M3b1 T3을 옮긴 것이고, M3b1 T2 메모(주소 형식)와 M3b1 리뷰 권장 3·4(대비 테스트, 링크 `:focus-visible`)를 더했다)
> 기준: `docs/plans/m3b1-design-foundation.md`(디자인 방향, 카드 모양, 날짜 관계 문구), `docs/product-brief.md` 6절, `CLAUDE.md` 7절 1항(**촬영 시점을 모르면 "촬영 시점 미상"으로 표시하고 그날 것처럼 보이게 하지 않는다. 출처가 비면 보여 주지 않는다** — PM 결정 2026-10-11), 안전장치 1~6, `docs/plans/m3a3-event-media.md`(공용 미디어 형식, R1~R9)
> 선행: M3b1 `feat/design-foundation` 머지(토큰, `types.ts`, `dateLabel`, `licenseLabel`, `isDisplayable`). `web/` 만 바꾸므로 서버 기능과 **병렬**. 뒤따르는 기능: M3b2 `feat/world-events-ui`, M3b3 `feat/media-video`(같은 `MediaItem` 에 영상 분기를 더한다).

## 목표

1. 공용 미디어 카드 입구 `MediaItem` 과 사진 카드 `MediaFigure`, 캡션 `MediaCaption`. 캡션(날짜 관계, 촬영 시점, 출처, 라이선스 상태)을 늘 함께 그린다(`CLAUDE.md` 7절 1항, 제품 정의서 6절, 안전장치 2).
2. 촬영 시점을 모르는 사진도 보이되 "촬영 시점 미상"으로만 표시하고, "같은 날" 배지나 요청 날짜를 떠올리게 하는 문구를 붙이지 않는다.
3. 주소 필드가 `http(s)` 가 아니면 그리지 않는다(서버 M3a3 R9와 같은 규칙, 이중 확인).
4. 링크 포커스가 보이고(공용 `:focus-visible`), 포커스 색이 카드·비극 블록 바탕에서도 대비 기준을 넘는다.

## 범위 밖

- 영상·iframe 임베드 그리기, 화면에 보일 때 재생 → M3b3
- 그날의 세계 섹션, `day_media` 묶기(M3-P17 (a) 결정) → M3b2
- 지도 SDK 미디어(`sdk`) → M4
- 원 출처 이미지가 깨질 때 처리 → M6b
- 사진 확대 보기

## 사용할 외부 출처

없음. 미디어 주소는 API 데이터로만 들어온다. 이미지는 브라우저가 원 출처(`thumbnail_url`)에서 바로 받는다(안전장치 1, `referrerPolicy="no-referrer"`).

## API / 데이터 변경

서버 변경 없음. M3b1의 `types.ts`·`dateLabel`·`licenseLabel`·`isDisplayable` 을 쓴다. 카드 모양과 날짜 관계 문구는 `docs/plans/m3b1-design-foundation.md` 의 "미디어 카드 모양"·"날짜 관계 문구" 표가 원천이다(`approximate` 는 "N년 전 무렵" / "N년 뒤 무렵" / "같은 해 무렵", 557192f).

| 컴포넌트·함수 | 받는 것 | 하는 일 |
|---|---|---|
| `isDisplayable(media)` 보강 | API 미디어 | 값이 있는 `source_url`, `url`, `thumbnail_url`, `embed_url`, `video_sources[].url` 중 하나라도 `http://`·`https://` 로 시작하지 않으면 `false` (M3b1 T2 메모, M3a3 R9) |
| `MediaItem` | `media`, `fallbackAlt` | `isDisplayable` 이 `false` 면 아무것도 그리지 않는다. `image` → `MediaFigure`. `image_embed`·`video_file`·`video_embed` → M3b3 전까지 그리지 않음(M3b3가 이 분기를 채운다). `sdk` → M4. `link_only` → 그리지 않음(M3에서는 서버가 만들지 않는다) |
| `MediaCaption` | API 미디어 | 배지(있을 때만), 촬영 시점 문구, 설명, 출처 문구, 라이선스 문구, "원문 보기"(`source_url`)·"라이선스"(`license_url` 이 있고 `http(s)` 일 때만) 링크. M3b3도 재사용 |
| `MediaFigure` | `image` 미디어, `alt` | `<figure>` + `<img>`(`src`=`thumbnail_url`, `alt`=`description` 또는 `fallbackAlt`, `loading="lazy"`, `decoding="async"`, `referrerPolicy="no-referrer"`, 크기가 있으면 `width`·`height`) + `MediaCaption` |
| `global.css` 링크 포커스 (리뷰 권장 4) | - | 모든 `a` 에 공용 `:focus-visible` 스타일(`--color-focus` 외곽선, 토큰 두께·간격). 마우스 클릭에는 보이지 않는다 |
| 대비 테스트 추가 (리뷰 권장 3) | `tokens.css` | focus 대 surface, focus 대 tragic-bg 가 3 이상(비텍스트), accent 대 surface 가 4.5 이상(링크 글자) |

외부 링크는 `target="_blank"` + `rel="noopener noreferrer"`.

## 촬영 시점 미상 표시 (PM 결정 2026-10-11)

| 상황 | 화면 |
|---|---|
| `date_relation.kind = unknown` (`date_precision` null) | 이미지는 그대로 보인다. 배지 없음, 첫 줄 "촬영 시점 미상". "같은 날"·"그날"·요청 날짜 문자열을 캡션에 넣지 않는다 |
| 출처 필드(`source_name`, `source_url`, `attribution`)가 빔 | 아무것도 그리지 않는다(`isDisplayable` false) |
| `license_status = unknown` | 이미지는 그대로, "라이선스 확인 안 됨" |

## 테스트 방침

M1b "테스트 방침"을 따른다. 목 데이터는 `web/src/mocks/fixtures/media/`(M3b1 T2가 만든 것 재사용, 필요한 것만 추가). 대비 테스트는 `tokens.css` 를 읽어 계산한다(M3b1 `tokens.test.ts` 에 더함).

## 데모 입력 3개로 확인할 것 (목 데이터)

| 데모 | 확인 |
|---|---|
| 1988-09-17 | `same_day` 사진 → 이미지와 "같은 날", "1988년 9월 17일 촬영", 출처, 두 링크. 촬영 시점을 읽지 못한 사진(값이 둘인 파일) → **이미지가 그대로 보이고** "촬영 시점 미상", "같은 날" 글자가 카드 어디에도 없다 |
| 2002-06-22 | `KOGL Type 1` 사진 → 라이선스 이름이 캡션에 |
| 1995-03-15 | 미디어 없음 — 확인할 것 없음 |

## 크기 (400줄 규칙)

예상: `MediaCaption` 약 40, `MediaFigure` 약 30, `MediaItem` 약 15, 카드 CSS 약 60, `isDisplayable` 주소 확인 약 10, `global.css` 포커스 약 8 → **약 165줄**(M3b1 T3 예상 205에서 이미 만든 부분을 뺀 값. M3b1 T2가 예상의 약 1.75배였으므로 보정하면 약 290). 400줄 안이다.

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | `MediaItem`, `MediaCaption`, `MediaFigure` + 카드 CSS, `isDisplayable` 주소 확인 | frontend-dev | `web/src/media/MediaItem.tsx`, `MediaCaption.tsx`, `MediaFigure.tsx`, `*.module.css`, `isDisplayable.ts` 수정, 컴포넌트·단위 테스트 | - (M3b1 머지) | (1) `MediaFigure`: `img` 의 `alt` 가 `description`(없으면 `fallbackAlt`), `src`=`thumbnail_url`, `loading`=`lazy`, `referrerpolicy`=`no-referrer`. 캡션에 "같은 날", "1988년 9월 17일 촬영", 출처 문구, "원문 보기" `href`=`source_url`, "라이선스" `href`=`license_url`(없으면 링크 없음). (2) `license_status: unknown` 목 → 이미지가 보이고 "라이선스 확인 안 됨". `date_precision: null` 목 → 이미지가 보이고 "촬영 시점 미상", 카드 텍스트에 "같은 날"과 요청 날짜 문자열("1988년 9월 17일")이 없다. `approximate` 목 → "N년 전 무렵" 배지(M3b1 문구 표). (3) `MediaItem`: R1을 못 채운 목(`source_url` 빈 값 포함) → 아무것도 없음, `video_embed`·`image_embed`·`sdk`·`link_only` 목 → 아무것도 없음(M3b3·M4 전). (4) `isDisplayable`: `thumbnail_url` 이 `javascript:` 또는 `data:` 로 시작 → `false`, `source_url` 이 상대 경로 → `false`, `license_url` 만 이상하면 카드는 그리되 "라이선스" 링크 없음. (5) 외부 링크 `rel` 에 `noopener`·`noreferrer`. (6) C5, C6, C8 출력 없음. (7) 전체 검사 통과, S1 결과 |
| T2 | 링크 `:focus-visible` 과 대비 테스트 추가 (M3b1 리뷰 권장 3·4) | frontend-dev | `web/src/styles/global.css`, `web/src/styles/tokens.test.ts` | T1 | (1) `global.css` 에 `a:focus-visible` 규칙이 있고 색은 `var(--color-focus)`, 길이는 토큰(C1·C2·C3 규칙 지킴). (2) 컴포넌트 테스트: 키보드(Tab)로 "원문 보기" 링크에 포커스가 가면 `:focus-visible` 규칙이 적용되는 요소다(`toHaveFocus` + 계산된 스타일 또는 클래스 확인 — jsdom 한계가 있으면 CSS 규칙 존재 테스트로 대신하고 보고). (3) `tokens.test.ts`: focus 대 surface ≥ 3, focus 대 tragic-bg ≥ 3, accent 대 surface ≥ 4.5. 기준을 못 넘는 토큰이 있으면 값을 바꾸지 말고 보고(PM이 정함). (4) 전체 검사 통과 |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T2 | (1) T1·T2 기준 표. (2) 전체 검사 통과, `npm test` 3회 반복 안정. (3) C7 출력 `0`. (4) S1·S2 보고 |
| T4 | 카드 화면 확인 (PR 검수 때) | PM | - | T3 | `cd web && npm run dev` 는 아직 카드를 보여 줄 화면이 없으므로 T3 표와 컴포넌트 테스트 이름으로 확인하고, 실제 카드 모양·문구는 M3b2 PR(T5)에서 실제 사진으로 본다. 키보드 Tab으로 링크 포커스가 보이는지는 M3b2에서 함께 본다 |

명령 (저장소 루트):

```
# C1~C3, C7, S1, S2 — M3b1 계획서와 같음
# C5 — 사진에 필터·자르기 없음. 출력 없음
grep -rnE 'filter:|object-fit:[[:space:]]*cover|sepia|grayscale' web/src --include='*.css'
# C6 — 제품 코드에 외부 절대 URL 없음 (테스트·목 제외). 출력 없음
grep -rnE 'https?://' web/src web/index.html --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/mocks/'
# C8 — 링크 카드가 남아 있지 않음. 출력 없음
grep -rn 'LinkOnlyCard' web/src
```

(C6: `isDisplayable` 의 `http(s)` 검사는 정규식 `^https?:\/\/` 형태로 쓰면 C6에 걸리지 않는다. 걸리면 보고하고 C6 예외를 PM에게 묻는다.)

담당별: frontend-dev 2, test-engineer 1, PM 1.

## 리스크와 확인할 것

1. **M3a3 형식과 어긋날 수 있다** — 병렬이다. M3a3 계획서 표를 원천으로 삼고, M3b2 T1에서 서버 DTO와 다시 맞춘다.
2. **원 출처 이미지가 깨질 수 있다** — 핫링크라 원 출처가 바뀌면 이미지가 안 뜬다. `img` 오류 처리는 M6b.
3. **PM이 카드를 실제로 보는 시점이 늦다** — 이 기능에는 카드를 띄울 화면이 없다. M3b2 PR에서 실제 사진으로 본다. 토큰 값만 바꾸는 수정은 싸다.
4. **포커스 대비** — focus 색(`#0B63CE`)은 bg 대비 약 5.3이라 surface·tragic-bg에서도 3을 넘을 것으로 보지만, 넘지 못하면 T2가 보고하고 PM이 토큰을 정한다.

## PM이 정할 것

없음 (분할과 촬영 시점 미상 표시는 2026-10-11 결정).
