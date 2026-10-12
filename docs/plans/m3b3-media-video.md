# 영상 재생 기초와 Commons 동영상 (M3b3)

> 브랜치: `feat/media-video` · 작성: planner · 상태: **PM 승인 (2026-10-12)** (처음 승인 2026-10-11) · 2026-10-12 개정: 영상 리서치 T5와 PM 결정(`docs/research/video-sources.md` 10.1절 9·13·15·22·23번) 반영. 보정 약 680줄이라 셋으로 나눔(로드맵 D1 (a)) — 이 기능은 화면 보임·자동 재생 정책·"하나만 재생" 조정·`<video>`(Commons). 개인정보처리방침·동의 단계는 M3b3b `feat/privacy-consent`, YouTube 플레이어는 M3b3c `feat/youtube-embed`. 제공사 이미지 임베드(`image_embed`)는 M3a6 제외로 뺌
> 기준: PM 미디어 결정 2026-10-11(바로 보이고 바로 재생, 음소거 자동 재생, 누르면 소리, 화면에 보일 때 재생·벗어나면 멈춤), 안전장치 1~6, M3-P8(움직임 줄이기·데이터 절약이면 자동 재생 안 함), M3-P9 (b)(영상이 없으면 사진만), `CLAUDE.md` 7절 1항(촬영 시점 미상 표시)·5항, `docs/plans/m3a3-event-media.md`, `docs/plans/m3a4b-commons-video.md`, `docs/plans/m3b1b-media-card.md`, `docs/research/video-sources.md` 3절 Commons 카드·6.1절(WebM·iOS 메모), 3.2절 V4 카드(자동 재생 조건), 10.1절 22번
> 선행: M3b2 머지(M3b2b와는 독립 — 병렬 가능). PM 화면 확인(T4)에는 M3a4b 머지가 필요하다.
> 뒤따르는 기능: M3b3c가 이 기능의 화면 보임 훅·자동 재생 정책·"하나만 재생" 조정을 YouTube 플레이어에 쓴다.

## 이번 개정에서 바뀐 것

| 항목 | 전 | 지금 | 근거 |
|---|---|---|---|
| 범위 | `video_file` + `video_embed` + `image_embed` (예상 265, 보정 약 450) + 의무 표시 | **`video_file` 과 재생 기초만**. YouTube·의무·동의는 M3b3b·M3b3c, `image_embed` 는 뺌 | 크기(D1 (a)), 10.1절 15번 (a) |
| 자동 재생 조건 | 50% **이상** 보이면 | 50%를 **넘게** 보이면(YouTube 정책과 같은 기준을 모든 영상에) | 3.2절 V4 카드 [100] |
| 재생 실패 | "영상을 불러오지 못했습니다." 문구, 캡션 남김 | **그 항목을 캡션까지 숨긴다** | 10.1절 22번 (a) |
| PM 확인 날짜 | 데모 3개 | + 임시 **표본 날짜 버튼** 2개(1969-07-20, 1986-09-20) — 데모 날짜에는 Commons 영상이 없어서. M2b가 다른 임시 버튼과 함께 지운다 | 리서치 위험 A, `CLAUDE.md` 7절 6항(공개 역사 사건 날짜) |

## 목표

1. 화면 보임 훅, 자동 재생 정책, **한 번에 하나만 재생**하는 조정을 만든다(`video_file`·YouTube 공통, M3b3c가 이어 씀).
2. `MediaItem` 에 `video_file`(`<video>`, Commons 원 출처 핫링크)을 붙인다: 화면에 절반 넘게 보이면 **음소거로 자동 재생**, 벗어나면 **멈춤**, 소리는 사용자가 컨트롤로 켠다.
3. 영상 아래에 `MediaCaption`(날짜 관계, 촬영 시점, 출처, 라이선스 상태)을 붙인다. 촬영 시점이 없으면 "촬영 시점 미상", "같은 날" 배지 없음.
4. 재생할 수 없는 영상은 **카드 전체(캡션 포함)를 숨긴다**. 영상이 없는 사건·날짜는 사진만 보인다(빈 영상 자리·"영상 없음" 문구·오류 표시를 만들지 않음, M3-P9 (b)).
5. PM이 Commons·YouTube 영상을 실제 서버로 볼 수 있게 임시 표본 날짜 버튼 2개를 더한다.

## 범위 밖

- YouTube 플레이어(`video_embed`) → M3b3c. 그전까지 `MediaItem` 은 `video_embed` 를 그리지 않는다(M3b1b 그대로)
- 개인정보처리방침·동의 단계·YouTube 약관 링크 → M3b3b
- 제공사 이미지 임베드(`image_embed`) — M3a6 제외. `MediaItem` 은 계속 그리지 않는다
- 서버 변경, 영상 파일 복사·서버 경유 재생(안전장치 1), 자체 플레이어 UI(재생 바·자막), 지도 SDK(M4)

## 사용할 외부 출처

프론트엔드가 직접 부르는 API는 없다. 브라우저가 Commons 원본·변환본 파일(`upload.wikimedia.org`)과 포스터(`thumbnail_url`)를 **직접 불러온다**. 주소는 서버가 준 `video_sources[].url`·`thumbnail_url` 만 쓰고(만들지 않음), M3b1b `isDisplayable` 의 `http(s)` 확인을 거친다. 리서치 확신도: Commons 동영상 **확인됨**(3절). 브라우저의 WebM·VP9 재생(특히 iOS Safari)은 리서치 범위 밖이었고 이 기능에서 확인한다 — 서버가 QuickTime 변환본을 뒤에 넣어 준다(M3a4b).

## 재생 규칙

| 규칙 | 내용 |
|---|---|
| 화면 보임 | `IntersectionObserver`. 보이는 비율이 **0.5를 넘으면** "보임", 0.5 이하면 "안 보임" |
| 자동 재생 | "보임"이 되면 조정에 재생을 청한다. 처음 재생은 **늘 음소거**(`muted`). 소리는 사용자가 컨트롤로 켠다 |
| 자동 재생 안 함 (M3-P8) | `prefers-reduced-motion: reduce` 이거나 `navigator.connection.saveData` 면 자동 재생하지 않고 포스터·컨트롤만(누르면 재생) |
| 하나만 재생 | 조정(`playbackCoordinator`)이 한 번에 하나만 재생하게 한다. 새 영상이 재생되면 앞 영상은 멈춘다. "안 보임"이 되면 멈춘다. 사용자가 직접 재생한 영상도 조정에 알린다 |
| `video_file` | `<video muted playsinline preload="metadata" controls poster={thumbnail_url}>`, `<source>` 는 `video_sources` 순서(작은 해상도 먼저, 서버가 정함). 위에 겹치는 요소 없음, 캡션은 아래 |
| 캡션 | `MediaCaption` 그대로(M3b1b·M3b2b 규칙 포함). `date_relation.kind = unknown` 이면 배지 없이 "촬영 시점 미상". 대체 텍스트·`aria-label` 에 요청 날짜를 넣지 않는다 |
| 실패 (10.1절 22번) | 모든 `<source>` 가 실패하거나 `error` 이벤트가 나면 그 `MediaItem` 을 **캡션까지** 그리지 않는다. 조정에서도 빼고, 오류 문구를 남기지 않는다 |
| 영상 없음 | 서버가 영상을 보내지 않으면 아무것도 더 그리지 않는다(사진 카드만) |

## 임시 표본 날짜 버튼 (M2b가 지움)

- M3b2의 데모 버튼 3개 아래에 "표본: 1969년 7월 20일로 보기", "표본: 1986년 9월 20일로 보기"를 더한다. 1969는 Commons 영상(아폴로 11호, 리서치 5절)이, 1986은 YouTube 영상(M3a5 뒤)이 나올 수 있는 날짜다. 공개 역사 사건 날짜다(`CLAUDE.md` 7절 6항).
- 날짜를 URL·브라우저 저장소에 넣지 않는 규칙(M3b2 C4)은 그대로다.

## 크기 (400줄 규칙)

예상: 화면 보임 훅 약 35, 자동 재생 정책 약 25, 하나만 재생 조정 약 30, `VideoFilePlayer`(실패 숨김 포함) 약 55, `MediaItem` 분기 약 10, 표본 버튼 약 10, CSS 약 30 → **약 195줄, 1.6~1.75배 보정 약 310~340줄**.

## 테스트 방침

M1b "테스트 방침"을 따른다. jsdom용 `IntersectionObserver`·`HTMLMediaElement.play/pause` 가짜를 `web/src/test/` 에 둔다. 실제 Commons를 부르지 않는다. 목 데이터는 M3a3 형식이고, 날짜는 데모·표본 날짜만 쓴다. 실제 브라우저 재생은 PM 화면 확인(T4)과, 필요하면 M3b2b의 Playwright 설정으로 본다(외부 요청은 막으므로 재생 자체가 아니라 요소·속성만).

## 데모·표본 날짜로 확인할 것

| 날짜 | 자동 테스트 (목) | PM 화면 확인 (T4) |
|---|---|---|
| 1969-07-20 (표본 버튼) | `video_file` 목 → `muted` 비디오, `source` 순서, 캡션 "1969년 7월 20일 촬영"·출처·라이선스. 보이는 비율 0.5 → 재생 안 함, 0.51 → `play()` | 아폴로 11호 영상이 보이면 소리 없이 재생, 벗어나면 멈춤, 소리를 켤 수 있음. 휴대폰(iOS Safari 가능하면)에서 재생되는지 |
| 1988-09-17 | 촬영 시점 미상 `video_file` 목 → 캡션 "촬영 시점 미상", "같은 날" 글자 없음 | Commons 영상 없음 — 사진만(정상) |
| 2000-02-29 | 영상 없는 응답 → `video`·`iframe` 0개, 사진 카드만, 오류 문구 없음 | 사진만 보이고 이상한 빈칸이 없다 |
| 1950-06-25, 2014-04-16 | `tragic` 사건 → 비극 블록에 `video`·`iframe` 0개(M3b2b 규칙) | - |
| 실패 | 모든 `source` 오류 목 → 그 카드와 캡션이 사라지고 다른 카드는 그대로, 오류 문구 없음 | - |

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 화면 보임 훅, 자동 재생 정책, 하나만 재생 조정 | frontend-dev | `web/src/media/useInView.ts`, `autoplayPolicy.ts`, `playbackCoordinator.ts`, 테스트 지원 가짜, 테스트 | M3b2 머지 | (1) 보이는 비율 0.5 → "안 보임", 0.51 → "보임". (2) 감소 모션·데이터 절약이면 자동 재생 허용이 `false`. (3) 조정: 재생자 둘을 차례로 시작하면 앞의 것 `pause` 가 불린다. "안 보임"이면 `pause`. 등록 해제하면 더 부르지 않는다. (4) 전체 검사 통과 |
| T2 | `VideoFilePlayer`, `MediaItem` 분기, 실패 숨김, 임시 표본 버튼 | frontend-dev | `VideoFilePlayer.tsx`, `MediaItem.tsx` 수정, `InputScreen.tsx`, CSS, 테스트 | T1 | (1) `video` 에 `muted`, `playsinline`, `controls`, `preload="metadata"`, `poster`, `source` 순서. (2) 보이면 `play()`, 벗어나면 `pause()`, 감소 모션이면 `play()` 안 불림. (3) 두 영상이 차례로 보이면 먼저 것 `pause()`. (4) 실패 목 → 카드·캡션 없음, 오류 문구 없음. (5) `captured_at` null 목 → "촬영 시점 미상", 배지 없음. (6) `video_embed`·`image_embed`·`sdk`·`link_only` 목 → 아무것도 없음. (7) 영상 없는 응답으로 `WorldSection` 을 그리면 `video`·`iframe` 0개. (8) 표본 버튼 2개가 있고 누르면 그 날짜로 `world` 를 부른다. (9) C1 출력 없음, M3b2 C4 출력 없음. (10) 전체 검사 통과, S1 보고 |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T2 | (1) 기준 표. (2) 전체 검사 통과, 3회 반복. (3) C1 출력 없음, C3 출력 `0`. (4) S1·S2 보고 |
| T4 | 실제 화면 확인 (PR 검수 때) | PM | - | T3, M3a4b 머지 | 서버·웹을 띄우고 표본 버튼 "1969년 7월 20일"로: 영상이 보이면 소리 없이 재생·벗어나면 멈춤·소리 켜기, 캡션. 데모 버튼으로: 영상이 없는 날짜는 사진만, 이상한 빈칸 없음. 휴대폰(또는 모바일 모드 + 네트워크 제한)에서 첫 화면이 늦어지지 않는지, iOS에서 재생되는지(안 되면 보고) |

```
# C1 — 제품 코드에 외부 절대 URL 없음 (테스트·목 제외). 출력 없음
grep -rnE 'https?://' web/src web/index.html --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/mocks/'
# C3 — act 경고 없음. 0
cd web && npm test 2>&1 | grep -c 'not wrapped in act'
# S1 — M3b2b 계획서 S1, S2 — M3b1 계획서
```

담당별: frontend-dev 2, test-engineer 1, PM 1.

## 리스크와 확인할 것

1. **데이터 사용량** — 절반 넘게 보일 때만, `preload="metadata"`, 작은 해상도 먼저, 데이터 절약이면 자동 재생 안 함, 하나만, 10분 상한(서버).
2. **iOS의 WebM 재생** — 안 되면 서버가 넣는 QuickTime 변환본으로 넘어간다. 그래도 안 되면 실패 숨김으로 카드가 사라진다(오류 표시 없음). PM이 T4에서 보고 다르면 보고한다.
3. **브라우저 자동 재생 정책** — 막히면 포스터와 재생 버튼이 정상 상태다.
4. **데모 날짜에서 영상을 못 본다** — 국내 데모 날짜에는 Commons 영상이 없다. 그래서 표본 버튼을 둔다. 국내 영상은 M3b3c(YouTube) 뒤에 본다.
5. **저작권** — 로드맵 리스크 4.

## PM이 정할 것

없음. 결정됨: M3-P8, 10.1절 22번 (a), 분할(로드맵 D1 (a), 2026-10-12).
