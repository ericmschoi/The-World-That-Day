# 영상 재생 기초와 Commons 동영상 (M3b3)

> 브랜치: `feat/media-video` · 작성: planner · 상태: **PM 승인 (2026-10-14, 개정)** — 결정 1 **(a)** PR 검수 T5는 실제 서버·영상 없이 + Playwright UI, 실제 Commons 영상·iOS 확인은 머지 뒤 M3a4b 머지 뒤 T6, 결정 2 **(b)** 캡션의 `description`·`license` 방어를 T2에, 결정 3 **(a)** `day_media` 소제목은 그대로·영상 대체 텍스트 "기록 영상". 처음 승인 2026-10-11, 2026-10-12 개정 승인. 2026-10-12 개정: 영상 리서치 T5와 PM 결정(`docs/research/video-sources.md` 10.1절 9·13·15·22·23번) 반영. 보정 약 680줄이라 셋으로 나눔(로드맵 D1 (a)) — 이 기능은 화면 보임·자동 재생 정책·"하나만 재생" 조정·`<video>`(Commons). 개인정보처리방침·동의 단계는 M3b3b `feat/privacy-consent`, YouTube 플레이어는 M3b3c `feat/youtube-embed`. 제공사 이미지 임베드(`image_embed`)는 M3a6 제외로 뺌
> 기준: PM 미디어 결정 2026-10-11(바로 보이고 바로 재생, 음소거 자동 재생, 누르면 소리, 화면에 보일 때 재생·벗어나면 멈춤), 안전장치 1~6, M3-P8(움직임 줄이기·데이터 절약이면 자동 재생 안 함), M3-P9 (b)(영상이 없으면 사진만), `CLAUDE.md` 7절 1·5·6항, `docs/plans/m3a3-event-media.md`, `docs/plans/m3a4b-commons-video.md`, `docs/plans/m3b1b-media-card.md`, **`docs/plans/m3b2b-world-photos-a11y.md`(머지된 코드 기준)**, `docs/research/video-sources.md` 3절 Commons 카드·6.1절(WebM·iOS 메모, 핫링크 확인 명령), 3.2절 V4 카드(자동 재생 조건), 10.1절 22번
> 선행: **M3b2 (PR #13)·M3b2b (PR #15) 머지 — 이 브랜치는 `main` 9642d08 에서 갈라졌다.** 구현과 자동 테스트는 fixture만 쓴다. 서버는 **M3a4b 머지 전까지 영상을 보내지 않는다**(M3a4b는 M3a3b·M3a4 뒤). 그래서 M3b2·M3b2b와 같은 방식으로(결정 1 (a)) PR 검수 T5는 실제 서버로 영상 없이 보고 영상 카드는 Playwright UI로 본 뒤 머지하고, 실제 Commons 영상·iOS 확인은 **M3a4b 머지 뒤 `main` 에서 T6**으로 한다 — PR 머지는 M3a4b를 기다리지 않는다.
> 뒤따르는 기능: M3b3c가 이 기능의 화면 보임 훅·자동 재생 정책·"하나만 재생" 조정을 YouTube 플레이어에 쓴다. M3b3b는 같은 `InputScreen` 을 고치므로 기본은 이 기능 다음이다.

## 2026-10-14 바뀐 점

승인(2026-10-12)은 M3b2·M3b2b 머지 전이었다. 머지된 `web/src` 를 읽고 맞췄다. 계획서와 코드가 다른 곳은 **코드를 따랐다**.

| 바뀐 것 | 이유 |
|---|---|
| **영상 카드가 들어갈 자리를 실제 코드 이름으로** — `MediaItem`(모든 미디어의 단일 입구, `switch (display_mode)`)에 `video_file` 분기, 카드는 `MediaFigure` 와 같은 `<figure className={styles.card}>` + `MediaCaption` 모양 | `WorldEventItem` 은 이미 `event.media` 를 모두 `MediaItem` 으로 넘긴다 → **고치지 않는다**. `TragicEvents` 는 미디어를 아예 그리지 않는다 → 영상도 자동으로 빠진다(고치지 않음) |
| **`day_media` 묶음 필터를 "그릴 수 있는 미디어"로** (M3b2b T1 메모) | 지금 `DayMediaGroups` 는 `isDisplayable` 로 거른다. `video_embed`·`sdk` 처럼 `MediaItem` 이 `null` 을 돌려주는 항목만 있는 묶음은 **소제목만 남는다**. `MediaItem` 이 실제로 그리는 모드인지까지 보는 `canRender` 를 한 곳에 두고 둘이 같이 쓴다. 영상이 **재생 실패로 숨으면** 같은 빈 소제목이 생기므로 그것도 막는다(T3) |
| **캡션 `.trim()` 방어를 T2에** (M3b2b 리뷰 권장 2, **결정 2 (b)**) | `MediaCaption` 의 `media.description?.trim()` 과 `licenseLabel` 의 `media.license?.trim()` 은 값이 문자열이 아니면(숫자·객체) 던진다. ErrorBoundary가 없어(M6b) 결과 화면 전체가 깨진다. 영상 카드도 같은 캡션을 지나므로 T2에서 두 곳을 막는다: 문자열이 아니면 없는 값으로 본다(설명 줄 없음, "라이선스 확인 안 됨"). 사진에도 적용된다 |
| **Playwright 재사용** (M3b2b T3) | 실제 브라우저가 필요한 확인(진짜 `IntersectionObserver`, 움직임 줄이기 에뮬레이션, `<source>` 실패 이벤트, 360px 가로 스크롤)은 `web/e2e/` + `web-ci` 의 e2e 단계로 한다(T4). 외부 요청은 지금처럼 막는다 — 재생 자체가 아니라 재생 **시도**와 요소·속성을 본다 |
| **`web-ci` 는 고치지 않는다** | 경로에 `data/schema/examples/media/valid/**` 가 이미 있다(M3b2b 결정 2 (a)). 이 기능의 영상 fixture는 그 폴더의 `example-video-file.json` 을 값 그대로 옮겨 `worldFixtures.test.ts` 가 대조한다 |
| **데모 표를 실제 fixture로** | 이전 표의 2000-02-29·1950-06-25·2014-04-16 행은 fixture가 없고 기본 핸들러가 500을 준다. 있는 fixture(`world2002WithMedia`, `world1995`, `worldWithTragic`)와 새 합성 `world1988WithVideo` 로 바꿨다. 표본 날짜(1969·1986)는 버튼이 부르는 날짜만 자동 테스트로 본다 |
| **재생 규칙 보강** | 실제 브라우저에서 생기는 일: `play()` 가 거부되면(자동 재생 막힘, 멈춤과 겹침) 잡아서 포스터·컨트롤 상태로 둔다. 사용자가 직접 멈춘 영상은 다시 자동 재생하지 않는다. 사용자가 켠 소리는 그대로 둔다. `<source>` 의 실패는 `<video>` 가 아니라 `<source>` 에 온다. React의 `muted` 는 HTML 속성을 쓰지 않는다. `IntersectionObserver` 가 없으면 자동 재생하지 않는다 |
| **`day_media` 영상** (**결정 3 (a)**) | 소제목("…사진")은 그대로 두고 영상도 그 아래 그린다. 영상의 대체 텍스트만 "기록 영상" |
| **작업 번호** (**결정 1 (a)**) | T2를 둘로 나눔: T2 = 영상 카드(`VideoFilePlayer`·`MediaItem`·캡션 방어), **T3 = 세계 화면 연결**(`day_media` 필터·실패 시 소제목·fixture·표본 버튼). 이전 T3(test-engineer)는 **T4**, 이전 T4(PM)는 **T5(PR 검수, 영상 없이)와 T6(M3a4b 머지 뒤, 실제 영상)** |
| **크기 다시 셈** 약 195(보정 310~340) → **약 300 (범위 300~360, M3b2 비율 ×1.19면 약 355)** | 아래 "크기". 400 아래라 한 PR. T2 뒤 확인점(누적 S1 360) |

## 목표

1. 화면 보임 훅, 자동 재생 정책, **한 번에 하나만 재생**하는 조정을 만든다(`video_file`·YouTube 공통, M3b3c가 이어 씀).
2. `MediaItem` 에 `video_file`(`<video>`, Commons 원 출처 핫링크)을 붙인다: 화면에 절반 넘게 보이면 **음소거로 자동 재생**, 벗어나면 **멈춤**, 소리는 사용자가 컨트롤로 켠다.
3. 영상 아래에 `MediaCaption`(날짜 관계, 촬영 시점, 출처, 라이선스 상태, 원문·라이선스 링크)을 그대로 붙인다. 촬영 시점이 없으면 "촬영 시점 미상", "같은 날" 배지 없음. 캡션은 `description`·`license` 가 문자열이 아니어도 깨지지 않는다(결정 2 (b), 사진 포함).
4. 재생할 수 없는 영상은 **카드 전체(캡션 포함)를 숨긴다**. 그 때문에 `day_media` 묶음이 비면 소제목도 숨긴다. 영상이 없는 사건·날짜는 사진만 보인다(빈 영상 자리·"영상 없음" 문구·오류 표시를 만들지 않음, M3-P9 (b)).
5. `day_media` 묶음이 `MediaItem` 이 실제로 그리는 항목만 센다(M3b2b T1 메모).
6. PM이 실제 서버로 표본 날짜를 볼 수 있게 임시 표본 날짜 버튼 2개를 더한다(Commons 영상은 M3a4b 뒤, YouTube는 M3b3c 뒤에 나온다).

## 범위 밖

- YouTube 플레이어(`video_embed`) → M3b3c. 그전까지 `MediaItem` 은 `video_embed` 를 그리지 않는다
- 개인정보처리방침·동의 단계·YouTube 약관 링크 → M3b3b
- 제공사 이미지 임베드(`image_embed`) — M3a6 제외. `MediaItem` 은 계속 그리지 않는다
- 서버 변경, 영상 파일 복사·서버 경유 재생(안전장치 1), 자체 플레이어 UI(재생 바·자막), 지도 SDK(M4)
- 응답 모양 검사(`worldShape.ts`) 변경 — 영상 카드가 새로 읽는 필드 중 위험한 것(`video_sources`, `url`, `thumbnail_url`)은 이미 `isDisplayable` 이 본다(M3b2b 리스크 4에 대한 답). `description`·`license` 는 응답 검사가 아니라 캡션에서 막는다(결정 2 (b), T2)
- 섹션 ErrorBoundary, 그 밖의 그리는 코드 버그 격리, 탭이 가려졌을 때의 처리, 영상 데이터 사용량 측정 → M6b
- `day_media` 소제목 문구 변경(결정 3 (a)로 그대로)

## 사용할 외부 출처

프론트엔드가 직접 부르는 API는 없다. 브라우저가 Commons 원본·변환본 파일(`upload.wikimedia.org`)과 포스터(`thumbnail_url`)를 **직접 불러온다**. 주소는 서버가 준 `video_sources[].url`·`url`·`thumbnail_url` 만 쓰고(만들지 않음), `isDisplayable` 의 `http(s)` 확인을 거친다. 리서치 확신도: Commons 동영상 **확인됨**(3절, 핫링크는 정책상 허용·비권장). 실제 브라우저 재생(V1-10)과 WebM·VP9의 iOS Safari 재생은 리서치에서 확인하지 않았고 이 기능의 T6에서 본다 — 서버가 QuickTime 변환본을 뒤에 넣어 준다(M3a4b). `<video>` 요청에는 브라우저 기본 정책대로 우리 origin만 `Referer` 로 가고, 날짜는 주소에 없다(M3b2 C4).

## API / 데이터 변경

서버 API는 바뀌지 않는다(계약은 M3a3 공용 미디어 형식과 머지된 `web/src/media/types.ts`). 웹 안에서 바뀌는 것만 적는다.

### 부품과 연결 (머지된 코드 기준)

| 파일 | 지금 | 이 기능 뒤 |
|---|---|---|
| `web/src/media/MediaItem.tsx` | `image` → `MediaFigure`, 나머지 `null` | `video_file` → `VideoFilePlayer`. 선택 속성 `onUnavailable?: (id: string) => void` 를 받아 넘긴다. **`canRender(media)`** 를 내보낸다: `isDisplayable(media)` 이고 `display_mode` 가 `image`·`video_file` 중 하나. `MediaItem` 도 이 함수로 판단한다(한 곳) |
| `web/src/media/VideoFilePlayer.tsx` (새로) | - | `<figure className={styles.card}>` 안에 `<video>` 와 `MediaCaption`. 아래 "재생 규칙" |
| `web/src/media/MediaCaption.tsx` | `media.description?.trim()` | 문자열일 때만 `trim`, 아니면 설명 없음(결정 2 (b)) |
| `web/src/media/licenseLabel.ts` | `media.license?.trim()` | 문자열일 때만 `trim`, 아니면 "라이선스 확인 안 됨"(결정 2 (b)). `licenseInAttribution` 은 `licenseLabel` 을 거치므로 같이 안전해진다 |
| `web/src/media/useInView.ts`, `autoplayPolicy.ts`, `playbackCoordinator.ts` (새로) | - | T1. 조정은 `createPlaybackCoordinator()` 로 만들고 React 문맥으로 내려 준다 |
| `web/src/screens/StoryScreen.tsx` | 섹션 5개 | 결과 화면마다 조정 하나를 둔다(공급자로 감쌈). M3b3c·M5c의 영상도 같은 조정에 붙는다 |
| `web/src/world/WorldEventItem.tsx` | `event.media.map(MediaItem)` | **바뀌지 않음** — 영상은 서버 순서대로 문장과 출처 줄 사이에 나온다. 영상이 숨어도 문장·출처 줄은 남는다 |
| `web/src/world/DayMediaGroups.tsx` | `media.filter(isDisplayable)` | `media.filter(canRender)` + 재생에 실패해 숨은 항목의 `id` 를 상태로 기억해 묶음 계산에서 뺀다 → 묶음이 비면 소제목도 없다. 대체 텍스트는 사진 "기록 사진", **영상 "기록 영상"**(결정 3 (a)) |
| `web/src/world/TragicEvents.tsx` | 미디어를 그리지 않음 | **바뀌지 않음** |
| `web/src/screens/InputScreen.tsx` | 데모 버튼 3개 | + 표본 버튼 2개 |
| `web/src/media/MediaCard.module.css` | `.card`, `.image`, 캡션 | + `.video`(`.image` 와 같은 크기 규칙, 색은 토큰만, `filter`·`object-fit: cover` 없음 — `cssRules.test.ts` C1·C5) |

### 조정(`playbackCoordinator`) 인터페이스 — M3b3c가 그대로 쓴다

| 동작 | 뜻 |
|---|---|
| `register(player)` → 해제 함수 | `player` 는 `{ play(): void; pause(): void }`. 해제하면 그 뒤로 부르지 않는다 |
| `requestPlay(player)` | "보임"이 되어 자동 재생을 청함. 지금 재생 중인 다른 재생자를 `pause()` 하고 `player.play()` |
| `notifyPlaying(player)` | 사용자가 컨트롤로 직접 재생함. 다른 재생자를 `pause()` |
| `release(player)` | "안 보임"이거나 멈춤. 재생 중이면 `pause()` 하고 "지금 재생 중" 표시를 지운다 |

### fixture 추가 (T3, S1에서 뺌)

| 이름 | 파일 | 내용 |
|---|---|---|
| `exampleVideoFile` | `web/src/mocks/fixtures/world/media.ts` | `data/schema/examples/media/valid/example-video-file.json` 을 **값 그대로**(합성 예시, `captured_at` `1988-09`, `month`, `CC BY-SA 4.0`, 변환본 2개 360p→480p). `worldFixtures.test.ts` 가 대조 |
| `world1988WithVideo` | `web/src/mocks/fixtures/world/responses.ts` | 합성. `world1988WithMedia` 바탕. 사건 ① `media` 끝에 `exampleVideoFile`(`date_relation` `same_month`, 0). `day_media` 끝에 촬영 시점 미상 영상 사본(`id` `example-video-file-undated`, `captured_at`·`date_precision` `null`, `unknown`). 화면에 영상 2개 — 조정·e2e용. 기본 핸들러에는 넣지 않는다 |

- README의 표와 "합성 값" 표에 두 줄을 더한다. `worldFixtures.test.ts` 의 fixture 이름 목록과 `SYNTHETIC` 목록에 `world1988WithVideo` 를 더한다(기대값만 바꿈).
- 단위 테스트의 "같은 날" 영상은 이미 있는 `web/src/mocks/fixtures/media/modes.ts` 의 `videoFile`(1988-09-17, `same_day`, 변환본 1개)을 쓴다.

## 재생 규칙

| 규칙 | 내용 |
|---|---|
| 화면 보임 | `IntersectionObserver`. 보이는 비율이 **0.5를 넘으면** "보임", 0.5 이하면 "안 보임". 관찰 기준값(`threshold`)에 0.5보다 조금 큰 값을 넣어 넘는 순간 콜백이 오게 한다. `IntersectionObserver` 가 없는 환경이면 늘 "안 보임"(자동 재생 없음, 오류 없음) |
| 자동 재생 | "보임"이 되고 정책이 허용하면 조정에 `requestPlay`. 사용자가 소리를 켠 적이 없는 영상은 `play()` 전에 `video.muted = true` 를 **속성 값(property)으로** 다시 넣는다 — 처음 재생은 늘 음소거. 사용자가 컨트롤로 소리를 켠 영상은 그 선택을 둔다 |
| 자동 재생 안 함 (M3-P8) | `matchMedia('(prefers-reduced-motion: reduce)').matches` 이거나 `navigator.connection?.saveData === true` 면 자동 재생하지 않고 포스터·컨트롤만(누르면 재생). 정책은 자동 재생을 시도할 때마다 새로 읽는다(한 번 읽고 기억하지 않음). `navigator.connection` 이 없는 브라우저(Safari·Firefox)는 데이터 절약 아님으로 본다 |
| `play()` 거부 | `play()` 가 돌려준 약속이 거부되면(`NotAllowedError`, 멈춤과 겹친 `AbortError`) 잡아서 조용히 포스터·컨트롤 상태로 둔다. 콘솔 출력·오류 문구 없음(M3b2 C5) |
| 하나만 재생 | 조정이 한 번에 하나만 재생하게 한다. 새 영상이 재생되면 앞 영상은 멈춘다. "안 보임"이 되면 멈춘다. 사용자가 직접 재생한 영상도 조정에 알린다(`notifyPlaying`) |
| 사용자가 멈춤 | 사용자가 컨트롤로 멈춘 영상(우리가 부르지 않은 `pause`)은 사용자가 다시 재생할 때까지 자동 재생하지 않는다(화면을 벗어났다 돌아와도). 끝난 영상도 다시 자동 재생하지 않는다(`loop` 없음) |
| `video_file` 요소 | `<video controls playsInline preload="metadata" poster={thumbnail_url} width={thumbnail_width} height={thumbnail_height} aria-label={fallbackAlt}>`. `muted`·`defaultMuted` 는 렌더 직후 속성 값으로 `true`(React의 `muted` 는 HTML 속성을 쓰지 않는다). `autoplay`·`loop`·`crossorigin` 없음. `thumbnail_url` 이 없으면 `poster` 없음 |
| `<source>` | `video_sources` 순서 그대로(작은 해상도·WebM 먼저, QuickTime 뒤 — 서버가 정함). `type` 은 `mime` 이 빈 문자열이 아닌 문자열일 때만 넣는다(브라우저가 못 트는 형식을 받지 않고 건너뛰게). `video_sources` 가 비었거나 `null` 이고 `url` 이 있으면 `<source src={url}>` 하나(`type` 없음) |
| 겹침·캡션 | 영상 위에 겹치는 요소 없음. 캡션은 아래, `MediaCaption` 그대로(M3b1b·M3b2b 규칙 + 결정 2 (b) 방어). `date_relation.kind = unknown` 이거나 맞지 않으면 배지 없이 "촬영 시점 미상". `aria-label`(대체 텍스트 몫)에 요청 날짜·"그날"·"같은 날"·"당일"을 넣지 않는다(M3b2b 규칙) |
| 실패 (10.1절 22번) | **마지막 `<source>` 의 `error`** (모든 후보가 실패했다는 뜻 — `<source>` 의 실패는 `<video>` 에 오지 않는다) 또는 `<video>` 의 `error` 가 오면 그 카드를 **캡션까지** 그리지 않는다. 조정에서 해제하고, `onUnavailable(id)` 를 한 번 부르고, 오류 문구를 남기지 않는다 |
| 영상 없음 | 서버가 영상을 보내지 않으면 아무것도 더 그리지 않는다(사진 카드만) |

## 화면 구조 (M3b2b 화면에 더함)

```
그날의 세계                               (h2)
 [사건 목록 — normal 만]
   · 문장 원문
     [MediaItem × N]                      ← 사진 카드, 영상 카드가 서버 순서대로 (WorldEventItem 그대로)
     출처 줄
 그날 찍힌 사진 / 다른 때 찍힌 사진 / 촬영 시점을 확인하지 못한 사진   (h3 — 그릴 수 있는 항목이 남아 있을 때만)
   [MediaItem × M]                        ← day_media 영상의 대체 텍스트 "기록 영상"
 같은 날 있었던 아픈 일                     (미디어 없음 — 영상도 없음)
```

- `day_media` 에 영상이 오면 지금 소제목("…사진") 아래에 그대로 그린다(결정 3 (a)). M3a4b는 영상을 **사건에만** 붙이므로(사건마다 최대 1개) M3 서버에서는 `day_media` 영상이 나오지 않는다. 서버가 `day_media` 영상을 보내기 시작하는 기능이 생기면 그 계획에서 문구를 다시 본다.

## 임시 표본 날짜 버튼 (M2b가 지움)

- M3b2의 데모 버튼 3개 아래에 "표본: 1969년 7월 20일로 보기", "표본: 1986년 9월 20일로 보기"를 더한다. 1969는 Commons 영상(아폴로 11호, 리서치 5절, M3a4b 뒤)이, 1986은 YouTube 영상(M3a5·M3b3c 뒤)이 나올 수 있는 날짜다. 공개 역사 사건 날짜다(`CLAUDE.md` 7절 6항). 같은 `styles.button` 을 쓴다.
- 날짜를 URL·브라우저 저장소에 넣지 않는 규칙(M3b2 C4)은 그대로다.
- 기본 MSW 핸들러는 바꾸지 않는다(표본 날짜는 500). 버튼 테스트는 요청 날짜만 확인하고, 화면 내용은 `server.use(...)` 로 바꿔 끼운 응답으로 본다.

## 테스트 방침

M1b·M3b2·M3b2b "테스트 방침"을 따른다.

- **Vitest (jsdom)**: `IntersectionObserver` 가짜(비율을 테스트가 정함)와 `HTMLMediaElement.prototype.play/pause` 가짜(`play` 는 약속을 돌려주고, 거부하게도 할 수 있음)를 `web/src/test/mediaFakes.ts` 에 두고 영상 테스트 파일에서 설치한다. jsdom의 "Not implemented: HTMLMediaElement…" 메시지가 나오면 안 된다. 실제 Commons를 부르지 않는다(MSW `onUnhandledRequest: 'error'` 그대로 — `<video>` 의 파일 요청은 jsdom이 보내지 않는다). 날짜는 데모 3개, 표본 2개, `world1988WithVideo` 의 합성 값만.
- **Playwright (실제 Chromium)**: `interceptNetwork` 에 선택지를 더해, fixture의 영상 파일 주소 요청은 **응답하지 않고 붙잡아 둔다**(오류도 응답도 없음 → 카드가 남아 재생 시도를 볼 수 있음). 실패 시험에서는 같은 요청을 `abort` 한다. 그 밖의 `localhost` 밖 요청은 지금처럼 `abort`, 응답을 받은 밖 요청은 0건이어야 한다. `play`/`pause` 는 `addInitScript` 로 원래 함수를 감싸 호출과 그때의 `muted` 를 기록한다(실제 디코딩은 하지 않으므로 재생 **시도**를 본다 — 자동 재생 정책 플래그와 관계없이 같은 결과). 움직임 줄이기는 `page.emulateMedia({ reducedMotion: 'reduce' })`, 데이터 절약은 `addInitScript` 로 `navigator.connection` 을 정의한다. 콘솔의 "Failed to load resource"(막은 요청)는 정상이라 세지 않고, `pageerror`(잡히지 않은 예외)를 0건으로 본다.

## 데모·표본 날짜로 확인할 것

| 날짜 / 응답 | 자동 테스트 (fixture) | PM 확인 — T5 (PR 검수, 실제 서버·영상 없이 + Playwright UI) | PM 확인 — T6 (머지 뒤, M3a4b 머지 뒤 `main`) |
|---|---|---|---|
| 1988-09-17 `world1988WithVideo` (합성) | 사건 ① 안에 `img` 2개 뒤 `video` 1개, 문장·출처 줄 사이. 영상 캡션 배지 "같은 달", "1988년 9월 촬영", "같은 날" 없음, 출처 `attribution`, 라이선스 줄은 `attribution` 에 "CC BY-SA 4.0"이 있어 숨고 "라이선스" 링크는 있음, "원문 보기". `source` 2개가 360p→480p 순서, `type` `video/webm`. `day_media` 의 미상 영상은 "촬영 시점을 확인하지 못한 사진" 아래, "촬영 시점 미상", 접근 이름 "기록 영상". e2e: 영상이 화면 가운데 오면 `muted` 인 채로 `play` 기록, 맨 위로 돌아가면 `pause`, 두 영상 사이를 오가도 동시에 재생 중인 것은 하나 이하, 360px 가로 스크롤 없음 | Playwright UI(`npx playwright test --ui`)로 fixture 화면의 영상 카드 모양·캡션 문구·"기록 영상"(영상 자리는 포스터 없이 빈 칸 — 외부 요청을 막으므로) | - |
| 1988-09-17 (실제 데모) | `world1988WithMedia` 그대로 → `video`·`iframe` 0개(지금 시험 유지) | 사건 3개, 사진은 M3a3b 뒤, 영상 없음(정상) | Commons 국내 영상은 없다 — 사진만(정상) |
| 2002-06-22, 1995-03-15 | 영상 없는 응답 → `video`·`iframe` 0개, 사진 카드·안내 문장만, 오류 문구·빈 영상 자리 없음 | 지금과 같음, 이상한 빈칸 없음 | 같음 |
| (합성) `worldWithTragic` + 비극 사건에 영상 | 비극 블록 안 `img`·`iframe`·`video` 0개(M3b2b 규칙) | - | - |
| (합성) `day_media` 가 `video_embed`·`sdk` 만 | 사진 묶음 소제목 없음(M3b2b T1 메모) | - | - |
| (합성) 실패 | 모든 `source` 실패 → 그 카드와 캡션이 사라지고 다른 카드는 그대로, 오류 문구 없음. `day_media` 묶음의 유일한 항목이 실패하면 그 소제목도 사라진다. e2e: 영상 요청을 `abort` 하면 실제 Chromium에서 같은 결과 | - | - |
| (합성) 움직임 줄이기·데이터 절약 | 보여도 `play()` 0회, 컨트롤 있음. e2e: `reducedMotion: 'reduce'` 와 `saveData` 각각 `play` 기록 0 | - | macOS "동작 줄이기"를 켜면 자동 재생하지 않는지(선택) |
| (합성) 캡션 값이 문자열이 아님 (결정 2 (b)) | `description`·`license` 가 숫자·객체인 사진·영상 → 카드가 그려지고 설명 줄 없음, "라이선스 확인 안 됨", 결과 화면이 깨지지 않음 | - | - |
| 표본 1969-07-20 | 버튼을 누르면 `/api/story/world?date=1969-07-20` 요청 | 버튼이 보이고 결과 화면이 열린다(위키백과 사건. 영상 없음 — 정상) | 아폴로 11호 영상이 화면에 절반 넘게 보이면 소리 없이 재생, 벗어나면 멈춤, 컨트롤로 소리 켜기, 캡션(같은 날, 1969년 7월 20일 촬영, Wikimedia Commons, 라이선스). 휴대폰(iOS Safari 가능하면)에서 재생되는지 |
| 표본 1986-09-20 | 버튼을 누르면 `date=1986-09-20` 요청 | 결과 화면이 열린다 | 영상 없음(YouTube는 M3b3c 뒤) — 정상 |

## 크기 (400줄 규칙)

### 웹 실측 비율

| 기능 | 예상 → 실제 (S1) | 비율 |
|---|---|---|
| M3b1 | 225 → 309 | 약 1.4배 |
| M3b1b | 165 → +158 | 약 1.0배 |
| M3b2 | 365 → 434 | 약 1.19배 |
| **M3b2b** | **약 315 → 319** | **약 1.01배** |

이전 셈(약 195에 서버식 1.6~1.75배)은 웹 실측과 맞지 않는다. M3b2b처럼 **파일별로 다시 세고**(Prettier 80칸, 지금 코드의 주석 밀도) 웹 비율 ×1.0~×1.2를 범위로 둔다.

### 다시 센 값

| 작업 | 내용 | 다시 셈 | 범위 (×1.0~×1.2) |
|---|---|---|---|
| T1 | `useInView` 약 35, `autoplayPolicy` 약 20, 조정 약 45 + 문맥·공급자 약 15, `StoryScreen` 약 4, 테스트 지원 가짜 `web/src/test/mediaFakes.ts` 약 30(S1 명령이 `web/src/test/` 를 빼지 않으므로 넣음) | 약 150 | 150~180 |
| T2 | `VideoFilePlayer` 약 85(보임·정책·조정·거부 처리·사용자 멈춤·소리·`source`·실패 숨김), `MediaItem`·`canRender` 약 15, CSS 약 12, 캡션 방어 약 4(결정 2 (b)) | 약 116 | 116~140 |
| T3 | `DayMediaGroups` 약 18(`canRender`, 실패 기억, "기록 영상"), `InputScreen` 약 14. fixture는 S1에서 뺌 | 약 32 | 32~40 |
| **합계** | | **약 300** | **300~360** — M3b2 비율(×1.19)이면 **약 355** |

- 400 아래라 **한 PR**로 한다. 리뷰 수정(M3b2는 리뷰 필수 1건으로 +30)까지 넣어도 400 안팎이다.
- **확인점 (안전장치)**: T2 뒤 누적 S1이 **360**을 넘으면 오케스트레이터가 T3 전에 멈추고, T3(세계 화면 연결·표본 버튼)을 따로 뺄지 PM에게 묻는다. 넘지 않으면 그대로 T3으로 간다.
- 테스트(`*.test.ts(x)`), fixture(`web/src/mocks/fixtures/**`), e2e(`web/e2e/**`)는 S1에서 뺀다(M3b2b S1 그대로).

## 작업 목록

순서: T1 → T2 → **확인점 (누적 S1 360)** → T3 → T4 → T5(PR 검수 때) → 머지 → T6(M3a4b 머지 뒤, `main` 에서). 서버 M3a3b·M3a4·M3a4b와 폴더가 달라 병렬이다. M3b3b와는 `InputScreen` 이 겹쳐 기본은 이 기능 다음.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 화면 보임 훅, 자동 재생 정책, 하나만 재생 조정, 테스트 지원 가짜 | frontend-dev | `web/src/media/useInView.ts`, `autoplayPolicy.ts`, `playbackCoordinator.ts`(만드는 함수·문맥·공급자), `web/src/screens/StoryScreen.tsx`(공급자), `web/src/test/mediaFakes.ts`, 테스트 | M3b2b 머지(완료) | (1) `useInView`: 가짜 관찰자로 비율 0.5 → `false`, 0.51 → `true`, 다시 0.5 → `false`. 관찰 기준값에 0.5보다 큰 값이 있다. 언마운트하면 `disconnect` 가 불린다. `IntersectionObserver` 가 없으면 늘 `false` 이고 오류·콘솔 출력이 없다. (2) `autoplayPolicy`: 움직임 줄이기 `matches: true` → `false`, `navigator.connection.saveData: true` → `false`, `navigator.connection` 이 없고 움직임 줄이기도 아니면 `true`. 같은 테스트 안에서 `matchMedia` 결과를 바꾸면 다음 호출 결과가 바뀐다(기억하지 않음). (3) 조정: A `requestPlay` → A `play` 1회. 이어서 B `requestPlay` → A `pause`, B `play`. C `notifyPlaying` → B `pause`. C `release` → `pause`. 해제한 재생자는 더 불리지 않는다. 재생자 0개에서 `release` 해도 오류 없음. (4) `StoryScreen` 이 결과 화면마다 `createPlaybackCoordinator()` 로 만든 조정 하나를 문맥으로 둔다. 조정을 쓰는 테스트는 테스트마다 새 조정을 쓴다(테스트 사이 상태 공유 없음). (5) `mediaFakes.ts`: 비율을 정해 콜백을 부르는 관찰자 가짜, `play`(약속 반환, 거부 선택 가능)·`pause` 가짜. 설치·해제 함수가 있다. 기존 테스트에는 설치하지 않는다(기존 시험 변경 없음). (6) M3b2 C1·C5 출력 없음, C3 `0`, 웹 전체 검사(`lint`·`format:check`·`typecheck`·`test`) 통과, 보고서에 T1 S1 |
| T2 | `VideoFilePlayer`, `MediaItem` 분기와 `canRender`, 실패 숨김, CSS, 캡션 `description`·`license` 방어(결정 2 (b)) | frontend-dev | `web/src/media/VideoFilePlayer.tsx`, `MediaItem.tsx`, `MediaCaption.tsx`, `licenseLabel.ts`, `MediaCard.module.css`, 테스트 | T1 | (1) `modes.ts` `videoFile`: `video` 에 `controls`, `playsinline`, `preload="metadata"`, `poster`(= `thumbnail_url`), `width`·`height`(= 썸네일 크기), 접근 이름 = `fallbackAlt`. `video.muted`·`video.defaultMuted` 가 `true`(속성 값으로 확인). `autoplay`·`loop`·`crossorigin` 속성 없음. `figure` 의 자식은 `video` 와 `figcaption` 둘뿐. (2) `source` 가 `video_sources` 순서이고 `type` = `mime`. `mime` 이 빈 문자열이거나 문자열이 아니면 그 `source` 에 `type` 이 없다. `video_sources: []` + `url` 이 있는 합성 → `source` 1개(`src` = `url`, `type` 없음). `thumbnail_url: null` → `poster` 속성 없음. (3) 보이는 비율 0.51 → `play()` 1회, 그 직전 `muted` 가 `true`. 0.5 → `pause()`. 움직임 줄이기 또는 데이터 절약이면 0.51이어도 `play()` 0회. (4) 두 영상이 차례로 보이면 먼저 것의 `pause()` 가 불린다. 사용자가 두 번째 영상의 `play` 이벤트를 일으키면(직접 재생) 첫 영상이 멈춘다. (5) `play()` 가 `NotAllowedError` 로 거부되면 카드·캡션이 그대로, 오류 문구 없음, `console.error`·`console.warn` 0회(스파이), 처리되지 않은 거부 없음. (6) 사용자 멈춤(우리가 부르지 않은 `pause` 이벤트) 뒤에는 비율이 0.5 이하→0.51로 다시 바뀌어도 `play()` 가 불리지 않는다. 사용자가 다시 재생하면 규칙이 되살아난다. (7) 사용자가 소리를 켠 뒤(`volumechange`, `muted: false`) 안 보임→보임이면 `play()` 때 `muted` 가 `false` 그대로. (8) 실패: 마지막 `source` 에 `error` 를 일으키면 카드와 캡션이 사라지고(설명·"원문 보기" 없음) 오류 문구가 없으며, `onUnavailable` 이 그 `id` 로 1회, 조정 해제. `video` 의 `error` 도 같다. 같은 화면의 다른 사진 카드는 그대로. 첫 `source` 만 실패하면 카드가 남는다. (9) 캡션: `captured_at: null` 합성 → "촬영 시점 미상", "같은 날" 글자 없음. 출처(`attribution`), 라이선스(줄 또는 `attribution` 안), "원문 보기"가 있다. `fallbackAlt` 에 날짜 말이 있는지는 M3b2b 규칙 시험이 그대로 덮는다. (10) `canRender`: 표시 가능한 `image`·`video_file` → `true`, 표시 가능한 `video_embed`·`image_embed`·`sdk` → `false`, 표시 불가 → `false`. `MediaItem` 은 `canRender` 가 `false` 면 `null`. `video_embed`·`image_embed`·`sdk`·`link_only` 는 여전히 아무것도 그리지 않는다. (11) 기존 `MediaItem.test.tsx` 의 "renders nothing for video_file yet" 은 기대값만 바꾼다(영상 카드가 나옴) — 지운 시험 없음, 바꾼 기대값은 보고에 표로. (12) CSS: `.video` 에 색 값·`filter`·`object-fit: cover` 없음, 쓰는 `var(--…)` 는 모두 `tokens.css` 에 있다(`cssRules.test.ts` 통과). (13) 캡션 방어(결정 2 (b)): `description` 이 숫자(`42`)·객체(`{}`)인 **사진과 영상** 합성 각각 → 카드가 그려지고 설명 줄이 없다. `license` 가 숫자·객체 → 라이선스 줄이 "라이선스 확인 안 됨"이고 `attribution` 이 같은 말을 담아도 숨지 않는다. 어느 경우에도 예외가 없고(`console.error` 0회) 같은 화면의 다른 카드가 그대로. 문자열 `description`·`license` 의 기존 동작(공백만이면 없음, M3b2b 라이선스 줄 규칙)은 기존 시험이 고치지 않고 통과. (14) M3b2 C1·C5 출력 없음, C3 `0`, 웹 전체 검사 통과, 보고서에 **T1~T2 누적 S1 — 확인점 360** (넘으면 오케스트레이터가 T3 전에 멈추고 PM에게 묻는다) |
| T3 | 세계 화면 연결: `day_media` 를 "그릴 수 있는 미디어"로, 실패 시 소제목 숨김, 영상 대체 텍스트(결정 3 (a)), fixture, 임시 표본 버튼 | frontend-dev | `web/src/world/DayMediaGroups.tsx`, `web/src/screens/InputScreen.tsx`, `web/src/mocks/fixtures/world/media.ts`·`responses.ts`·`README.md`, 테스트 | T2 (확인점 통과) | (1) `day_media` 가 `video_embed`·`sdk` 항목만인 합성 → 사진 묶음 소제목이 하나도 없다. 그런 항목과 사진이 섞이면 사진이 있는 묶음의 소제목만 있다(M3b2b T1 메모). (2) `day_media` 가 영상 하나뿐인 합성에서 그 영상의 마지막 `source` 에 `error` 를 일으키면 그 묶음의 `h3` 가 사라진다. `world1988WithVideo` 에서 미상 영상이 실패하면 미상 사진 사본이 남아 "촬영 시점을 확인하지 못한 사진" 소제목은 그대로다. 사건 ①의 영상이 실패해도 사건 문장·출처 줄·사진 2장은 그대로. (3) 소제목 세 개의 글자는 바뀌지 않는다(M3b2b 결정 4 (a)). `day_media` 영상은 그 소제목 아래 그려지고 접근 이름은 "기록 영상", 사진은 지금처럼 "기록 사진". (4) `exampleVideoFile` 이 `example-video-file.json` 과 값이 같다(`worldFixtures.test.ts` 에 대조 시험 추가). `world1988WithVideo` 는 `satisfies WorldResponse` 로 타입 검사 통과, 키 집합 시험 통과, 기본 핸들러는 그대로, README 두 표에 줄 추가. `worldFixtures.test.ts` 의 fixture 이름·`SYNTHETIC` 목록은 기대값만 바꾼다. (5) `world1988WithVideo` 로 위 "데모·표본" 표 첫 행의 자동 테스트 열 중 jsdom으로 볼 수 있는 것(순서·캡션·`source`·묶음 위치·접근 이름) 통과. 영상 없는 응답(`world2002WithMedia`·`world1995`)에서 `video`·`iframe` 0개. 비극 사건에 영상을 넣은 합성에서 비극 블록 안 `video` 0개. (6) 표본 버튼: 데모 버튼 3개 뒤에 "표본: 1969년 7월 20일로 보기", "표본: 1986년 9월 20일로 보기" 순서. 누르면 각각 `date=1969-07-20`, `date=1986-09-20` 으로 `world` 를 부른다(MSW 요청 기록으로 확인). `App.test.tsx` 의 버튼 목록 기대값은 5개로 바꾼다(기대값만, 보고에 표로). (7) M3b2 C1·C4·C5 출력 없음, C3 `0`, 웹 전체 검사 통과, 보고서에 누적 S1 |
| T4 | 완료 기준 검증, 실제 브라우저 재생 시도 확인(e2e), 전체 테스트 | test-engineer | 빠진 기준의 테스트, `web/e2e/video-playback.spec.ts`, `web/e2e/network.ts`(영상 요청 붙잡기·끊기 선택지) | T3 | (1) T1~T3 기준 표, 빠진 시험 추가. (2) e2e(`world1988WithVideo`, 360·1280 두 프로젝트, 재시도 0): (a) 사건 ① 영상을 화면 가운데로 옮기면 `play` 가 기록되고 그때 `muted` 가 `true`, 맨 위로 돌아가 영상이 화면 밖이면 `pause` 가 기록된다. (b) 두 영상 사이를 오가는 동안 `paused === false` 인 `video` 가 늘 1개 이하. (c) `reducedMotion: 'reduce'` 로 같은 동작 → `play` 기록 0, `video` 에 컨트롤. (d) `navigator.connection.saveData = true` 로 같은 동작 → `play` 기록 0. (e) 영상 파일 요청을 `abort` → 실제 Chromium에서 두 영상 카드와 그 캡션이 사라지고, "촬영 시점을 확인하지 못한 사진" 소제목은 남은 미상 사진 때문에 그대로, 사진 카드·사건 문장은 그대로, 오류 문구 없음. (f) `pageerror` 0건, `localhost` 밖 요청 중 응답을 받은 것 0건. (g) 360px에서 가로 스크롤 없음(`scrollWidth <= clientWidth`). (3) `npm test` 3회, `npm run e2e` 3회 반복 안정(기존 e2e 14개 포함). (4) M3b2 계획서의 C1·C2·C4·C5·C6 출력 없음, C3 `0`. (5) "데모·표본" 표 자동 테스트 열의 행마다 덮은 시험을 표로. (6) S1·S2 보고 |
| T5 | 실제 서버로 화면 확인 (PR 검수 때, 영상 없이 + Playwright UI — 결정 1 (a)) | PM | - | T4 | (1) 루트 `.env` 에 `WIKIMEDIA_USER_AGENT`, DB·서버·웹 실행. (2) 데모 버튼 3개: 지금과 같은 화면, 이상한 빈칸·오류 문구 없음. (3) 표본 버튼 2개가 데모 버튼 아래에 있고 누르면 결과 화면이 열린다(영상이 없는 것이 정상 — 서버는 M3a4b 뒤에 영상을 보낸다). (4) `cd web && npx playwright test --ui` 로 `video-playback` 화면을 열어 영상 카드 모양, 캡션(배지·촬영 시점·출처·라이선스·원문 보기), `day_media` 영상이 "촬영 시점을 확인하지 못한 사진" 아래 있고 대체 텍스트가 "기록 영상"인지 본다(외부 요청을 막아 영상 자리는 빈 칸). (5) 문구 검토: 표본 버튼 두 줄, "기록 영상". (6) 실제 영상 재생·iOS는 이 단계에서 보지 않는다(T6). T5가 끝나면 머지할 수 있다 — M3a4b 머지를 기다리지 않는다 |
| T6 | 실제 Commons 영상으로 화면 확인 (머지 뒤, **M3a4b 머지 뒤** `main` 에서 — 결정 1 (a)) | PM | - | 이 PR 머지, M3a4b 머지 | (1) `main` 을 받아 DB·서버·웹 실행(M3a4b T6 품질 확인과 같은 자리에서 해도 된다). (2) 표본 "1969년 7월 20일": 영상이 화면에 절반 넘게 보이면 소리 없이 재생, 벗어나면 멈춤, 컨트롤로 소리 켜기, 다른 영상이 재생되면 앞 영상이 멈춤, 캡션(배지·촬영 시점·출처·라이선스·원문 보기). (3) 휴대폰(또는 개발자 도구 모바일 모드 + 네트워크 제한): 첫 화면이 늦어지지 않는지, **iOS Safari에서 재생되는지**(안 되면 카드가 조용히 사라지는지 — 어느 쪽이든 보고). (4) (선택) macOS "동작 줄이기"를 켜면 자동 재생하지 않는지. (5) (선택) 리서치 6.1절 `curl` 2개(`$WIKIMEDIA_USER_AGENT`)로 핫링크 응답 206/200. (6) 문제가 있으면 오케스트레이터에게 알린다 — `fix/` 브랜치로 |

완료 기준에 쓰는 명령 (저장소 루트). C1~C6은 `docs/plans/m3b2-world-events-ui.md` 의 같은 이름 명령을 그대로 쓴다. S1·S2·E1은 `docs/plans/m3b2b-world-photos-a11y.md` 의 같은 이름 명령을 그대로 쓴다(S1: 테스트·fixture·e2e 제외, `.github` 포함).

```
# C1 — 제품 코드에 외부 절대 URL 없음 (테스트·목 제외). 출력 없음
grep -rnE 'https?://' web/src web/index.html --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/mocks/'
# C3 — act 경고 없음. 0
cd web && npm test 2>&1 | grep -c 'not wrapped in act'
# C4 — 날짜를 URL·브라우저 저장소에 넣지 않음. 출력 없음
grep -rnE 'localStorage|sessionStorage|history\.(push|replace)State|location\.(search|hash)' web/src --exclude='*.test.ts' --exclude='*.test.tsx'
# C5 — 제품 코드에서 콘솔에 남기지 않음. 출력 없음
grep -rnE 'console\.(log|info|warn|error|debug)' web/src --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/test/'
# E1 — e2e (로컬, 처음 한 번 브라우저 설치: npx playwright install chromium)
cd web && npm run e2e
```

담당별: frontend-dev 3 (T1, T2, T3), test-engineer 1 (T4), PM 2 (T5, T6).

## 리스크와 확인할 것

1. **실제 영상 확인이 늦다** — 서버는 M3a3b → M3a4 → M3a4b가 머지되어야 영상을 보낸다. 그전에는 fixture와 Playwright(재생 시도·요소·실패 이벤트)로만 확인하고, 실제 디코딩·iOS 재생은 머지 뒤 T6이다. T6에서 나온 문제는 `fix/` 로 고친다(결정 1 (a)). 그동안 M3b3c가 이 기능의 조정을 쓰므로, 조정 인터페이스는 이 PR에서 고정한다.
2. **iOS의 WebM 재생** — `type` 이 있어 iOS는 WebM `source` 를 건너뛰고 서버가 뒤에 넣는 QuickTime 변환본으로 간다. 그래도 안 되면 실패 숨김으로 카드가 사라진다(오류 표시 없음). T6에서 확인한다.
3. **브라우저 자동 재생 정책** — 음소거 자동 재생은 대부분 허용되지만 막히면 `play()` 거부를 잡아 포스터·컨트롤 상태로 둔다. e2e는 Playwright Chromium의 정책 플래그에 기대지 않도록 재생 **시도**(`play` 호출과 `muted`)를 기록한다.
4. **React `muted`** — React는 `muted` 를 HTML 속성으로 쓰지 않아(속성 값만 바꿈) iOS가 음소거로 보지 않을 수 있다. 렌더 직후와 매 자동 재생 직전에 `muted` 를 속성 값으로 넣고, 시험은 속성 값(`video.muted`)을 본다.
5. **`<source>` 실패 이벤트** — 모든 후보가 실패해도 `<video>` 에는 `error` 가 오지 않고 각 `<source>` 에 온다. 마지막 `<source>` 의 `error` 를 기준으로 하고, 실제 Chromium에서 e2e T4 (2)(e)로 확인한다.
6. **0.5 경계** — 관찰자는 기준값을 지날 때만 콜백을 준다. 0.5만 두면 정확히 0.5에서 멈춘 콜백 뒤로 소식이 없을 수 있어 0.5보다 큰 기준값을 같이 둔다.
7. **e2e의 "붙잡아 둔 요청"** — 응답하지 않은 요청이 시험 끝에 정리되며 경고를 낼 수 있다. 불안정하면 test-engineer가 재현 방법과 함께 보고하고, 대안(영상 요청만 `addInitScript` 로 막고 `play` 기록만 보기)을 고른다.
8. **데이터 사용량** — 절반 넘게 보일 때만, `preload="metadata"`, 작은 해상도 먼저, 데이터 절약이면 자동 재생 안 함, 하나만, 10분 상한(서버). 실제 측정은 M6b.
9. **데모 날짜에서 영상을 못 본다** — 국내 데모 날짜에는 Commons 영상이 없다. 그래서 표본 버튼을 둔다. 국내 영상은 M3b3c(YouTube) 뒤에 본다.
10. **캡션 방어는 두 필드만** — 결정 2 (b)는 `description`·`license` 의 `.trim()` 충돌만 막는다. 그 밖의 그리는 코드 버그가 결과 화면 전체를 깨는 경로는 M6b의 섹션 ErrorBoundary까지 남는다.
11. **`day_media` 영상의 소제목** — 결정 3 (a)로 `day_media` 영상이 오면 "…사진" 소제목 아래 놓인다. M3 서버에서는 나오지 않는다(M3a4b는 사건에만 붙임). 서버가 `day_media` 영상을 보내기 시작하는 기능이 생기면 그 계획에서 문구를 다시 본다.
12. **M3b3b와 겹침** — 둘 다 `InputScreen` 을 고치고, M3b3b의 동의 단계가 생기면 e2e `openDemo` 도 체크 상자를 먼저 눌러야 한다. M3b3b가 이 기능 다음에 오면 그쪽에서 고친다.
13. **소리·멈춤 규칙** — "사용자가 켠 소리는 그대로", "사용자가 멈춘 영상은 다시 자동 재생하지 않음"은 PM 결정(누르면 소리)을 화면 규칙으로 옮긴 것이다. T5·T6에서 다르게 느껴지면 말하면 된다(규칙 한두 줄 고침).
14. **크기** — 다시 셈 약 300, M3b2 비율이면 약 355. T2 뒤 확인점(누적 S1 360)을 둔다.
15. **저작권** — 로드맵 리스크 4.

## PM 결정 (2026-10-14, 기록)

| # | 물은 것 | 결정 | 이 계획서에 반영 |
|---|---|---|---|
| 1 | PM 화면 확인과 PR 머지 시점(승인된 계획대로면 이 PR이 M3a3b·M3a4·M3a4b를 기다림) | **(a)** M3b2·M3b2b와 같은 방식: T5(PR 검수, 실제 서버·영상 없이 + Playwright UI로 영상 카드)로 머지하고, 실제 Commons 영상·iOS 확인은 머지 뒤 M3a4b 머지 뒤 `main` 에서 T6. planner 추천과 같음. 고르지 않은 것: (b) M3a4b 머지까지 PR 대기, (c) Playwright로만 확인 | 머리말 "선행", 바뀐 점 "작업 번호", 데모·표본 표의 T5·T6 열, 작업 순서, T5·T6, 리스크 1 |
| 2 | 캡션 `.trim()` 충돌(M3b2b 리뷰 권장 2) | **(b)** T2에서 `description`·`license` 가 문자열이 아니면 없는 값으로 본다(설명 줄 없음, "라이선스 확인 안 됨"). 약 4줄 + 시험, 사진에도 적용. planner 추천과 같음. 고르지 않은 것: (a) M6b로 미룸 | 바뀐 점, 목표 3, 범위 밖, 부품 표(`MediaCaption.tsx`·`licenseLabel.ts`), 데모·표본 표 "캡션 값이 문자열이 아님" 행, 크기 T2, T2 산출물·(13), 리스크 10 |
| 3 | `day_media` 에 영상이 올 때 소제목 | **(a)** 소제목("…사진")은 그대로, 영상도 그 아래 그리고 대체 텍스트만 "기록 영상". planner 추천과 같음. 고르지 않은 것: (b) `day_media` 에서는 사진만, (c) 소제목을 "…사진·영상"으로 | 바뀐 점, 범위 밖, 부품 표(`DayMediaGroups.tsx`), 화면 구조, T3 (3), T5 (4)·(5), 리스크 11 |

이전 결정: M3-P8, M3-P9 (b), 10.1절 22번 (a), 분할(로드맵 D1 (a), 2026-10-12), Playwright CI(D3 (a)), M3b2 결정 2 (a)의 "실제 데이터 확인은 머지 뒤" 방식(2026-10-13), M3b2b 결정 2 (a)(`web-ci` 경로)·4 (a)(세 번째 소제목). 남은 질문은 없다.

★ PM 직접 구현: 없음 — 이 기능에는 백엔드 작업이 없다.
