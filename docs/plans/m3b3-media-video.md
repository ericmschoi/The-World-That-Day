# 영상과 공식 임베드 재생 (M3b3)

> 브랜치: `feat/media-video` · 작성: planner · 상태: PM 승인 (2026-10-11) (2026-10-11 미디어 결정, M3-P9 (b) 반영) · **2026-10-11 갱신: 촬영 시점 미상 표시 결정, `MediaItem` 이 M3b1b로 옮겨짐, 리서치 T1 결과(국내 날짜 Commons 영상 없음)**
> 기준: PM 미디어 결정 2026-10-11(바로 보이고 바로 재생, 음소거 자동 재생, 누르면 소리, 화면에 보일 때 재생·벗어나면 멈춤), 안전장치 1~6(PM 확인), M3-P8(움직임 줄이기·데이터 절약이면 자동 재생 안 함, 결정), **M3-P9 (b)(아무 날짜에서나 자동 검색, 영상이 없으면 사진만)**, `CLAUDE.md` 7절 1항(**촬영 시점을 모르면 "촬영 시점 미상"으로 표시하고 그날 것처럼 보이게 하지 않는다** — PM 결정 2026-10-11), `docs/plans/m3a3-event-media.md`(형식), `docs/plans/m3a4-event-video.md`~`m3a6`, `docs/plans/m3b1-design-foundation.md`(문구), `docs/plans/m3b1b-media-card.md`(카드·캡션), `docs/plans/m3-video-sources-research.md`
> 선행: M3b2 `feat/world-events-ui` 머지(M3b1b 포함)와 리서치 머지(제공자별 임베드 형식·표시 조건). PM 화면 확인(T4)에는 M3a4 머지가 필요하고, YouTube·게티 임베드 확인은 M3a5·M3a6 머지 뒤에 한다(머지 전이면 그 부분은 그 기능의 PR에서 본다). 리서치 T1에 따르면 국내 데모 날짜에는 Commons 영상이 없을 가능성이 높아, **데모 날짜에서 실제 영상을 보는 것은 M3a5 머지 뒤**가 될 수 있다.

## 목표

1. M3b1b `MediaItem` 에 세 가지 표시 방식을 붙인다: `video_file`(`<video>`, 원 출처 파일), `video_embed`(원 제공자 공식 플레이어 iframe), `image_embed`(제공사 공식 이미지 임베드 iframe).
2. 영상은 화면에 보이면 **음소거로 자동 재생**, 벗어나면 **멈춤**, 누르면 소리.
3. 모든 영상·임베드 아래에 `MediaCaption`(날짜 관계, 촬영 시점, 출처, 라이선스 상태)을 붙인다. 촬영 시점이 명시되지 않은 영상은 "촬영 시점 미상"이고 "같은 날" 배지를 붙이지 않는다.
4. 제공사 임베드 위에 아무것도 겹치거나 로고를 가리지 않는다.
5. **영상이 없는 사건·날짜는 사진만 보인다**. 빈 영상 자리, "영상 없음" 문구, 오류 표시를 만들지 않는다(M3-P9 (b)).

## 범위 밖

- 서버 변경 (M3a4~M3a6)
- 영상 파일 복사·서버 경유 재생 (안전장치 1)
- 자체 플레이어 UI(재생 바·자막)
- 지도 SDK (M4)

## 사용할 외부 출처

프론트엔드가 직접 부르는 API는 없다. 브라우저가 원 출처 파일(Commons)과 제공자 플레이어(`youtube-nocookie.com`, 리서치로 확인된 제공사)를 **직접 불러온다**(제품 정의서 8절 문구 제안, 로드맵). 허용 제공자는 서버가 정하고, 프론트엔드는 받은 `embed_url` 만 쓴다(주소를 만들지 않는다). `embed_url`·`video_sources[].url` 도 M3b1b `isDisplayable` 의 `http(s)` 확인을 거친다.

## 재생 규칙

| 규칙 | 내용 |
|---|---|
| 자동 재생 | 미디어가 화면에 50% 이상 보이면 재생, 50% 미만이면 일시정지(`IntersectionObserver`). 처음 재생은 **늘 음소거**. 소리는 사용자가 플레이어 컨트롤로 켠다 |
| 자동 재생 안 함 (M3-P8) | `prefers-reduced-motion: reduce` 이거나 데이터 절약(`navigator.connection.saveData`)이면 자동 재생하지 않고 포스터·플레이어만(누르면 재생) |
| 동시에 하나 | 새 영상이 재생되면 다른 영상은 멈춘다 |
| `video_file` | `<video muted playsinline preload="metadata" controls poster={thumbnail_url}>`, `<source>` 는 `video_sources` 순서(작은 해상도 먼저) |
| `video_embed` | `<iframe src={embed_url} title=… allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen loading="lazy" referrerpolicy="strict-origin-when-cross-origin">`, 비율은 `thumbnail_width`/`thumbnail_height` 또는 16:9. `provider: youtube` 는 화면 밖에서 `postMessage` 로 일시정지·재생. 그 밖 제공자는 리서치가 확인한 제어 방법이 없으면 화면에서 멀리 벗어날 때 iframe을 내렸다가 돌아오면 다시 띄운다 |
| `image_embed` | 제공사 iframe을 그대로(`loading="lazy"`), 위에 겹치는 요소·자르기·필터 없음 |
| 캡션 | `MediaCaption` 그대로(M3b1b). `date_relation.kind = unknown` 이면 배지 없이 "촬영 시점 미상". iframe `title`·대체 텍스트에 요청 날짜를 넣지 않는다 |
| 실패 | 재생 오류는 그 카드만 "영상을 불러오지 못했습니다."로 바꾸고 캡션은 남긴다 |
| 영상 없음 | 서버가 영상을 보내지 않으면 아무것도 더 그리지 않는다(사진 카드만) |

## 크기 (400줄 규칙)

예상: 화면 보임 훅 약 35, 자동 재생 정책 약 40, `VideoFilePlayer` 약 55, `VideoEmbedFrame` 약 60, `ImageEmbedFrame` 약 25, `MediaItem` 분기 약 10, CSS 약 40 → **약 265줄**. M3b1 T2가 예상의 약 1.75배였으므로 **T1 뒤 누적 200줄을 넘으면** T2의 `ImageEmbedFrame`(M3a6 조건부)을 뺄지 PM에게 묻는다.

## 테스트 방침

M1b "테스트 방침"을 따른다. jsdom용 `IntersectionObserver`·`HTMLMediaElement.play` 가짜를 `web/src/test/` 에 둔다. iframe `postMessage` 는 `contentWindow.postMessage` 감시로 확인한다. 실제 YouTube·제공사를 부르지 않는다. 목 데이터는 M3a3 형식이고, 날짜는 데모·표본 날짜(공개 역사 사건 날짜·경계값)만 쓴다.

## 데모·표본 날짜로 확인할 것

| 날짜 | 자동 테스트 (목) | PM 화면 확인 (T4) |
|---|---|---|
| 1988-09-17 | `video_embed` 목 → iframe(`src`=`embed_url`)과 캡션, `video_file` 목 → `muted` 비디오. **촬영 시점 미상 `video_embed` 목 → 캡션 "촬영 시점 미상", "같은 날" 글자 없음** | 올림픽 사건 영상이 보이면 소리 없이 재생, 벗어나면 멈춤, 누르면 소리(국내 날짜라 M3a5 머지 전에는 영상이 없을 수 있다 — 정상) |
| 2002-06-22 | `image_embed` 목 → iframe과 캡션, 겹치는 요소 없음 | 서버 결과대로 |
| 2000-02-29 | 영상 없는 응답 → `video`·`iframe` 0개, 사진 카드만, 오류 문구 없음 | 사진만 보이고 이상한 빈칸이 없다 |
| 1950-06-25, 2014-04-16 | `tragic` 사건 → 비극 블록에 `video`·`iframe` 0개 (M3b2 규칙) | 같음 |
| 1969-07-20 | - | 세계 사건이라 Commons 영상이 붙을 수 있는 날짜(리서치 T1 표본 결과에 따름). 엉뚱한 영상이 없는지 |
| 1995-03-15, 1986-09-20 | - | 서버 결과대로, 엉뚱한 영상이 없는지 |

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 화면 보임 훅, 자동 재생 정책, `VideoFilePlayer` | frontend-dev | `web/src/media/useInView.ts`, `autoplayPolicy.ts`, `VideoFilePlayer.tsx`, 테스트 지원 가짜, 테스트 | - (M3b2·리서치 머지) | (1) 50% 이상 보이면 `play()`, 미만이면 `pause()`. (2) `video` 에 `muted`, `playsinline`, `controls`, `preload="metadata"`, `poster`, `source` 순서. (3) 감소 모션·데이터 절약이면 `play()` 안 불림. (4) 두 영상이 차례로 보이면 먼저 것 `pause()`. (5) 재생 오류 → "영상을 불러오지 못했습니다."와 캡션 유지. (6) `captured_at` null 영상 목 → 캡션 "촬영 시점 미상", 배지 없음 |
| T2 | `VideoEmbedFrame`, `ImageEmbedFrame`, `MediaItem` 분기 | frontend-dev | `VideoEmbedFrame.tsx`, `ImageEmbedFrame.tsx`, `MediaItem.tsx` 수정, CSS, 테스트 | T1 | (1) `video_embed`: iframe `src`=`embed_url`, `allow` 에 `autoplay`, `title`, `loading="lazy"`, 캡션이 아래. (2) `youtube`: 화면 밖 → `postMessage` 일시정지, 다시 보이면 재생. (3) `image_embed`: iframe 위에 겹치는 형제 요소 없음, 캡션 있음. (4) `MediaItem` 이 네 표시 방식을 맞는 컴포넌트로, `sdk`·`link_only` 는 아무것도 없음, `embed_url` 이 `http(s)` 가 아니면 아무것도 없음. (5) 영상 없는 응답(표본 2000-02-29 목)으로 `WorldSection` 을 그리면 `video`·`iframe` 0개, 오류·빈 상자 없음. (6) 촬영 시점 미상 `video_embed` 목 → 캡션 "촬영 시점 미상", iframe `title` 에 날짜 문자열 없음. (7) C1 출력 없음. (8) 전체 검사 통과, S1 보고 |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T2 | (1) 기준 표. (2) 전체 검사 통과, 3회 반복. (3) C2 출력 없음, C3 출력 `0`. (4) S1·S2 보고(M3b1 명령) |
| T4 | 실제 화면 확인 (PR 검수 때) | PM | - | T3, M3a4 머지 | 서버·웹을 띄우고, 임시 데모 버튼(데모 3개)과 표본 날짜(브라우저 개발자 도구에서 `fetch('/api/story/world?date=…')` 결과 확인 또는 M3a4 P1 명령)로: 영상이 있는 날짜는 보이면 소리 없이 재생·벗어나면 멈춤·누르면 소리, 영상이 없는 날짜는 사진만 보이고 이상한 빈칸이 없음, 촬영 시점이 없는 영상은 "촬영 시점 미상", 제공사 임베드에 로고가 보임. 휴대폰(또는 모바일 모드 + 네트워크 제한)에서 첫 화면이 늦어지지 않는지 |

```
# C1 — 프론트엔드가 임베드 주소를 직접 만들지 않는지. 출력 없음
grep -rnE 'youtube(-nocookie)?\.com|gettyimages' web/src --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/mocks/'
# C2 — 제품 코드에 외부 절대 URL 없음 (테스트·목 제외). 출력 없음
grep -rnE 'https?://' web/src web/index.html --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/mocks/'
# C3 — act 경고 없음. 0
cd web && npm test 2>&1 | grep -c 'not wrapped in act'
```

담당별: frontend-dev 2, test-engineer 1, PM 1.

## 리스크와 확인할 것

1. **데이터 사용량** — 화면에 보일 때만 재생, `preload="metadata"`, 작은 해상도 먼저, 데이터 절약이면 자동 재생 안 함, 동시에 하나, 사건마다 영상 최대 1·사건 3개까지(M3a4). 아무 날짜에서나 영상을 찾으므로 영상이 붙는 날짜가 늘어난다.
2. **모바일 성능** — iframe은 무겁다. `loading="lazy"`. 3초·10초 목표는 M6b에서 측정.
3. **브라우저 자동 재생 정책** — 막히면 포스터와 재생 버튼이 정상 상태다.
4. **외부 플레이어의 추적·광고** — `youtube-nocookie.com`, 지연 로딩, 날짜를 URL에 넣지 않는다. 공개 배포 때 다시 본다.
5. **저작권** — 로드맵 리스크 4.
6. **데모 날짜에서 영상을 못 볼 수 있다** (리서치 T1) — 국내 날짜 Commons 영상이 없어 M3a5(YouTube 키)가 머지되기 전에는 데모 날짜 PM 확인에서 영상이 안 보일 수 있다. 재생 동작은 자동 테스트와 세계 사건 표본(1969-07-20)으로 먼저 본다.

## PM이 정할 것

없음 (M3-P8, 촬영 시점 미상 표시 결정됨).
