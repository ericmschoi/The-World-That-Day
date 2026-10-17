# YouTube 공식 채널 영상 재생 (M3b3c)

> 브랜치: `feat/youtube-embed` · 작성: planner · 상태: **PM 승인 (2026-10-16, 개정)** — 결정 1 **(a)** IFrame API 스크립트를 못 불러오거나 플레이어가 준비되지 않으면 공식 플레이어를 그대로 둔다(자동 재생·하나만 재생·오류 숨김 없음, 눌러서 봄, 추가 코드 없음), 결정 2 **(a)** T5(Playwright 검수) 뒤 머지, 실제 YouTube 확인 T6은 M3a5 머지 뒤 `main` 에서(1988은 M3a5b 뒤). 처음 승인 2026-10-12 (M3b3 분할로 생김, 로드맵 D1 (a))
> 기준: `docs/research/video-sources.md` 3.2절 V4 카드(V4-1 크기·`Referer`, V4-2 자동 재생·`onAutoplayBlocked`, V4-4 nocookie, V4-5 IFrame API), V2 카드 출처 표시(III.F.2.a), 7절 최종 추천 "재생", 8절 위험 12~15, 10.1절 9·13·22·23번, 제품 정의서 6절, `CLAUDE.md` 7절 1·3·5·6항, `docs/plans/m3a5-official-channel-video.md`(계약 예시), `docs/plans/m3a5b-daehan-news-video.md`(대한뉴스 `attribution`), `docs/plans/m3b3-media-video.md`(결정 1 (a) 방식), `docs/plans/m3b3b-privacy-consent.md`(T0 재생 규칙, 동의, C1·C7·C8·B1·S1·S2), **머지된 `web/` 코드(`main` 728b532)**
> 선행: **M3b3 (PR #16)·M3b3b (PR #18)·방침 시행일 후속 (PR #19) 머지 — 이 브랜치는 `main` 728b532 에서 갈라졌다.** 서버 변경 없음. 서버는 **M3a5 머지 전까지 YouTube 항목을 보내지 않는다**(M3a5는 M3a3c·M3a4·M3a4b 뒤). 그래서 M3b3 결정 1 (a)와 같은 방식으로(결정 2 (a)) PR 검수 T5는 Playwright(가짜 `YT`)로 보고 머지하고, 실제 YouTube 확인은 **M3a5 머지 뒤 `main` 에서 T6**으로 한다(데모 1988-09-17의 대한뉴스는 M3a5b 뒤). PR 머지는 M3a5를 기다리지 않는다.

## 2026-10-16 바뀐 점

승인(2026-10-12)은 M3b3·M3b3b 코드가 생기기 전이었다. 머지된 `web/src`·`web/e2e`·`web/privacy.html` 을 읽고 맞췄다. 계획서와 코드가 다른 곳은 **코드를 따랐다**.

| 바뀐 것 | 이유 |
|---|---|
| **부품 이름과 자리를 코드대로** — `VideoEmbedFrame` → **`web/src/media/YouTubePlayer.tsx`**, `VideoFilePlayer` 와 같은 모양(`<figure className={styles.card}>` + `MediaCaption`, `onUnavailable`) | `MediaItem` 이 모든 미디어의 단일 입구(`switch (display_mode)`)이고 `video_file` 이 `VideoFilePlayer` 로 간다. YouTube도 같은 자리, 같은 계약 |
| **`canRender` 에 동의와 YouTube 주소 검사** | `DayMediaGroups` 는 `canRender` 로 센다. 동의가 없을 때 `MediaItem` 만 `null` 을 내면 소제목만 남는다(M3b3 T3와 같은 문제). 그래서 `canRender(media, consented)` 하나로 둘이 같이 판단한다. 공급자 밖에서 그리는 기존 시험(`worldVideo.test.tsx`, `MediaItem.test.tsx`)은 동의가 `false` 라 기대값이 그대로다 |
| **재생 규칙에 M3b3b T0 규칙** (로드맵 메모) — "안 보임" 보고**마다** `coordinator.release`, 우리가 부른 멈춤을 **세는 카운터** | M3b3 리뷰 권장 1·2. YouTube는 `<video>` 와 달리 상태를 메시지로 늦게 알려 주므로(`paused` 같은 즉시 값이 없다) "언제 세는가"를 상태 기준으로 정했다(아래 "YouTube 상태 처리") |
| **브라우저·YouTube가 멈춘 것도 사용자 멈춤** (M3b3 리뷰 권장 5) | 우리가 부르지 않은 `PAUSED` 는 모두 사용자 멈춤으로 본다 → 다시 들어와도 자동 재생하지 않는다. 탭 숨김 처리는 M6b. T6에서 PM이 이 동작을 "버그 아님"으로 보도록 적었다 |
| **IFrame API 스크립트를 부르는 때**: "처음 YouTube 카드가 화면 가까이 올 때" → **첫 YouTube 카드가 그려질 때 한 번** | 지금 `useInView` 는 "절반 넘게 보임"만 알린다. "가까이"에는 관찰자가 하나 더 필요하다. 붙이기가 빠를수록 `onError` 숨김이 빨라 화면이 덜 흔들린다. 결과 화면은 동의 뒤에만 열리고 방침이 스크립트를 이미 밝힌다. iframe 자체는 `loading="lazy"` 그대로 |
| **스크립트를 못 불러오거나 준비되지 않을 때** (결정 1 (a)) | 승인안에 없던 경우(광고 차단기 등). 공식 플레이어는 그대로 두고 자동 재생·조정·오류 숨김만 하지 않는다. 추가 코드 없음 |
| **iframe `title` = `fallbackAlt`** (이전: "영상: " + 설명 첫 20자) | `VideoFilePlayer` 가 `aria-label={fallbackAlt}` 를 쓴다(사건: "「…」 관련 자료", `day_media`: "기록 영상"). 같은 규칙. `DayMediaGroups` 는 `video_embed` 도 "기록 영상" |
| **캡션의 `provider-terms`** | 지금 `licenseLabel` 은 `license` 문자열을 그대로 내서 화면에 **"provider-terms"** 가 보이고, 링크 글자는 늘 "라이선스"다. → 라이선스 줄 "제공사 이용약관에 따름", 링크 글자 "이용약관" |
| **임베드 주소 검사를 웹에서도** — origin이 `https://www.youtube-nocookie.com` 이고 경로가 `/embed/`, `enablejsapi=1` 있음, `autoplay` 없음 | 방침의 "개인정보 보호 강화 모드" 문장과 "절반 넘게 보일 때만 자동 재생" 규칙을 코드가 지킨다. 서버가 다른 주소를 보내면 그리지 않는다. `www.youtube.com` 으로 바꾸는 것은 PM 결정 + 방침 수정(로드맵 후속 "nocookie 대체") |
| **외부 주소 2개를 `urls.ts` 에** + **C7 시험 수정** | C1 규칙(외부 주소는 `web/src/external/urls.ts` 하나). 그런데 지금 C7 시험은 "방침 페이지의 절대 주소 집합 = `urls.ts` 의 **모든** 값"이라 값이 늘면 깨진다 → "방침 링크 3개 = 페이지 주소 집합"이고 "`urls.ts` 의 값은 방침 링크 또는 플레이어 주소 둘 중 하나"로 바꾼다(새 값을 분류 없이 더하면 실패) |
| **명령 이름** — 이전 "C5(임베드 주소를 만들지 않음)"는 M3b2의 C5(콘솔)와 이름이 겹쳤다 | **C9** 로 바꾸고 C10·C11·P1·N1을 더했다(아래 명령) |
| **fixture** — 2000-02-29 행 | 그 날짜 fixture가 없고 기본 핸들러는 500이다(M3b3과 같은 이유). 영상 없는 날은 `world2002WithMedia`·`world1995`. YouTube 레코드는 새 모듈 `web/src/mocks/fixtures/world/youtube.ts`(합성) — `media.ts` 는 `data/schema/examples` 를 값 그대로 옮긴 파일이라 섞지 않는다(S3 0) |
| **M3a5 계약 예시의 빈 필드** | 예시에 `lat`·`lng`·`place_ref`·`retrieved_at`·`date_relation.years` 가 없다. `isDisplayable` 은 이 값이 없으면 숨긴다. fixture는 채워 넣고(`place_ref` "country:KR" 등), M3a5 계획 개정 때 예시를 고치도록 로드맵에 적는다 |
| **개인정보처리방침** — 고칠 문장 없음 | 아래 "개인정보처리방침 점검". 시행일 그대로(P1 명령으로 확인) |
| **작업 나눔** — T1(스크립트·캡션) / T2(카드·숨김·동의) / **확인점** / T3(재생 규칙) / T4 test-engineer(e2e 포함 — 이전엔 frontend-dev) / T5 PM PR 검수 / T6 PM 실제 확인(M3a5 뒤, `main`, 결정 2 (a)) | M3b3·M3b3b와 같은 단위. 재생 규칙(T3)이 가장 불확실해서 따로 둔다 |
| **크기 다시 셈** 약 130(보정 약 220) → **약 257 (범위 257~308, 웹 실측 ×1.14면 약 293)** | 아래 "크기". 400 아래라 한 PR, T2 뒤 확인점 220 |

## 목표

1. `MediaItem` 에 `video_embed`(`provider: youtube`)를 붙인다: 서버가 준 `embed_url` 그대로 iframe을 만들고 **IFrame Player API(`YT.Player`)** 로 제어한다(원시 `postMessage` 없음, V4-5).
2. 화면에 **절반 넘게 보이면** `mute()` → `playVideo()`, 안 보이면 멈춤. `<video>` 와 섞여도 **한 번에 하나만**(M3b3 조정). 움직임 줄이기·데이터 절약이면 자동 재생 안 함(M3-P8). M3b3b T0 규칙(보고마다 해제, 우리 멈춤 카운터)을 그대로 지킨다.
3. 플레이어는 **200×200 이상**, 위·안에 아무것도 겹치지 않는다. 캡션은 아래.
4. 재생 오류(`onError`, 코드와 관계없이)면 **그 항목을 캡션까지 숨긴다**(10.1절 22번).
5. YouTube 출처 표시: 캡션에 서버 `attribution`("… · YouTube", III.F.2.a), 라이선스 줄 "제공사 이용약관에 따름", "이용약관" 링크(`license_url`). 대한뉴스는 서버 `attribution` 그대로(10.1절 13번).
6. **동의(`useConsent()`)가 `true` 일 때만** YouTube iframe·스크립트가 생긴다. 동의 전에는 YouTube로 가는 요청이 0건이다(10.1절 23번, 방어용).

## 범위 밖

- 임베드 주소 만들기·고치기(서버 M3a5가 준다 — C9), 서버 변경
- 원시 `postMessage` 제어, `YT.Player.destroy()`(iframe을 지워 React와 충돌 — C11)
- 자체 플레이어 UI, 썸네일 먼저 보여 주기, 제공사 이미지 임베드(`image_embed`, M3a6 제외)
- 스크립트를 못 불러올 때 카드를 숨기거나 시간 제한으로 판단하기(결정 1 (a) — 공식 플레이어 그대로)
- `www.youtube.com` 임베드로 바꾸기(nocookie 조합이 T6에서 안 되면 PM 결정 + 방침 수정)
- 탭 숨김·화면 꺼짐 처리, 섹션 ErrorBoundary → M6b
- 개인정보처리방침 문구 변경(이 PR에서는 고칠 문장이 없다 — 아래 점검), 동의를 실제 폼으로 옮기기(M2b)

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| YouTube 임베드 플레이어(`www.youtube-nocookie.com/embed/…`) | **확인됨** (V4-1·V4-2·V4-4). **nocookie + IFrame API 조합만 미확인** → T6 브라우저 확인(리스크 1) | 서버 `embed_url` 의 iframe | 200×200 이상, 겹치기·가리기 금지, 절반 넘게 보일 때만 자동 재생, 자동 재생 플레이어는 한 화면에 하나, `Referer` 를 보낸다(없으면 오류 153 — `referrerpolicy="no-referrer"` 금지), 플레이어 수정·중첩 iframe 금지(III.I.21) |
| YouTube IFrame Player API 스크립트 `https://www.youtube.com/iframe_api` | **확인됨** (V4-5) | `YT.Player`(기존 iframe에 붙이기), `mute`·`isMuted`·`playVideo`·`pauseVideo`·`getPlayerState`, `onReady`·`onStateChange`·`onError`·`onAutoplayBlocked` | 첫 YouTube 카드가 그려질 때 한 페이지에 한 번. 주소는 `web/src/external/urls.ts` |

이 계획을 쓰며 새로 연 외부 주소는 없다(근거는 리서치 문서). 구현·시험 중 공식 문서를 열어 볼 때는 User-Agent "The-World-That-Day (https://github.com/ericmschoi/The-World-That-Day)" 만 쓴다(이메일 넣지 않음).

## 개인정보처리방침 점검 (이 PR이 사실로 만드는 것)

| 방침 문장 (`web/privacy.html`, 시행일 2026년 10월 16일) | 이 PR 뒤 | 고칠 것 |
|---|---|---|
| "YouTube 영상은 YouTube 플레이어(개인정보 보호 강화 모드)를 페이지에 넣어 보여 주며, YouTube가 플레이어 안에 광고를 낼 수 있습니다." | 웹이 실제로 그렇게 한다(서버가 항목을 보내는 것은 M3a5부터). 웹이 nocookie 주소만 그린다(T2 (5))는 것이 이 문장을 지킨다 | 없음 |
| "YouTube 플레이어와 YouTube IFrame API 스크립트는 기기에 정보를 저장하거나 읽을 수 있으며…" | 스크립트가 처음으로 실제로 불린다 | 없음. T6에서 `localhost` 저장소·쿠키를 보고 무엇이 생기는지 기록 |
| "이 서비스는 쿠키나 브라우저 저장소를 쓰지 않습니다." | 우리 코드는 여전히 안 쓴다(C4·C8) | 없음 |
| "결과 화면의 사진과 영상은 원 출처 서버(Wikimedia Commons 등)에서 브라우저가 직접 받아 옵니다. 이때 그 서버는 접속 정보(IP 주소 등)를 받습니다." | YouTube 플레이어·스크립트 요청도 여기에 든다("등") | 없음 |
| "지금은 한국어 위키백과, Wikidata, Wikimedia Commons(…)이고, **YouTube Data API가 더해질 예정입니다.**" | 서버 쪽 — 이 PR과 무관. M3a5가 사실로 만든다 | 이 PR에서는 없음. **M3a5 머지 때 현재형으로 고치고 시행일을 새로 적는다**(웹 작업 — 로드맵 고칠 곳) |

결론: 이 PR에서 방침과 시행일은 그대로다(P1 출력 없음). 구현이 방침과 어긋나게 되면(예: 주소 검사를 풀어야 함) frontend-dev는 멈추고 보고한다. 그때 방침을 고치면 M3b3b "시행일 넣는 법"(머지하는 날 한 줄 커밋)을 다시 쓴다.

## API / 데이터 변경

서버 API·`data/` 는 바뀌지 않는다(S3 0). 계약은 M3a3 공용 미디어 형식과 M3a5·M3a5b 계약 예시. 웹 안에서 바뀌는 것만 적는다.

### 부품과 연결 (머지된 코드 기준)

| 파일 | 지금 | 이 기능 뒤 |
|---|---|---|
| `web/src/external/urls.ts` | 방침 링크 3개 | + `YOUTUBE_IFRAME_API_URL = 'https://www.youtube.com/iframe_api'`, `YOUTUBE_EMBED_ORIGIN = 'https://www.youtube-nocookie.com'` |
| `web/src/privacy/privacy.test.ts` (C7) | 페이지 주소 집합 = `urls.ts` 모든 값 | 페이지 주소 집합 = 방침 링크 3개, `urls.ts` 의 모든 값은 방침 링크 3개 ∪ 플레이어 주소 2개 안 |
| `web/src/media/youtubeApi.ts` (새로) | - | 필요한 만큼의 `YT` 타입(새 npm 패키지 없음 — N1) + `loadYouTubeApi(): Promise<YT>`: `window.YT?.Player` 가 있으면 그것, 없으면 `<script async src=YOUTUBE_IFRAME_API_URL>` 를 **한 번만** 넣고 `onYouTubeIframeAPIReady` 로 끝남(먼저 있던 콜백도 부른다). 스크립트 `error` 면 실패로 끝나고 같은 페이지에서 다시 넣지 않는다 |
| `web/src/media/YouTubePlayer.tsx` (새로) | - | 아래 "재생 규칙"·"YouTube 상태 처리". T2가 카드·붙이기·숨김, T3이 재생 |
| `web/src/media/isDisplayable.ts` | `canRender(media)`: `image`·`video_file` | `canRender(media, consented)`: + `video_embed` 는 `consented` 이고 `isYouTubeEmbed(media)`(아래 "주소 검사")일 때만. `image`·`video_file` 은 동의와 무관 |
| `web/src/media/MediaItem.tsx` | `video_embed` → `null` | `useConsent()` 를 읽어 `canRender(media, consented)`, `video_embed` → `YouTubePlayer`(`onUnavailable` 전달) |
| `web/src/world/DayMediaGroups.tsx` | `canRender(item)`, 대체 텍스트는 `video_file` 만 "기록 영상" | `canRender(item, useConsent())`, `video_embed` 도 "기록 영상" |
| `web/src/media/licenseLabel.ts` | 이름 그대로 | `license` 가 `provider-terms`(앞뒤 공백 무시)이고 상태가 `unknown` 이 아니면 **"제공사 이용약관에 따름"** |
| `web/src/media/MediaCaption.tsx` | 링크 글자 늘 "라이선스" | `license` 가 `provider-terms` 면 **"이용약관"**, 아니면 지금처럼 "라이선스" |
| `web/src/media/MediaCard.module.css` | `.video` 등 | `.embed`: `display: block; width: 100%; aspect-ratio: 16 / 9; min-width: 200px; min-height: 200px; border: 0`(200은 YouTube 정책 값 — 주석). iframe이 카드의 둥근 모서리(`overflow: hidden`)에 잘리지 않게 한다(방법은 frontend-dev — 예: 카드 안 여백). `position: absolute|fixed` 없음 |
| `web/src/media/playbackCoordinator.ts` | 주석 "the YouTube player in M3b3c" | 주석만(인터페이스 그대로) |
| `web/src/consent/consent.ts` | `useConsent()` | 바뀌지 않음 |
| `web/src/test/youtubeFake.ts` (새로, S1 제외) | - | 가짜 `window.YT`: IFrame API 문서 [103]의 구조대로 만든 합성 스텁(`CLAUDE.md` 7절 5항). `Player(el, { events })`, 메서드 감시, `PlayerState`, 시험 도우미(준비·상태·오류·자동 재생 막힘 보내기, 이벤트 미루기·내보내기, 요소별 생성 기록), `install`/`uninstall`(`mediaFakes.ts` 와 같은 방식). 기본 동작: `playVideo` → 버퍼링(3) → 재생(1), 재생·버퍼링 중 `pauseVideo` → 멈춤(2), 시작 전 `pauseVideo` → 이벤트 없음(문서에 없는 경우의 나쁜 쪽) |
| `web/src/mocks/fixtures/world/youtube.ts` (새로, fixture) | - | 합성 레코드 3개 (아래) + README "Synthetic" 표에 행 추가 |
| `web/src/mocks/fixtures/world/responses.ts` | `world1988WithVideo` | + `world1988WithYoutube`: `world1988WithVideo` 에서 사건 ① 영상 뒤에 `exampleDaehanNews`, 사건 ②(KBS 1TV)에 `exampleYoutubeUndated`. Commons `<video>` 2개 + YouTube 2개 한 화면. 기본 핸들러는 바꾸지 않음 |

### 주소 검사 (`isYouTubeEmbed`)

`provider === 'youtube'` 이고 `embed_url` 을 `new URL` 로 읽었을 때 `origin === YOUTUBE_EMBED_ORIGIN`, `pathname` 이 `/embed/` 로 시작, `searchParams.get('enablejsapi') === '1'`, `autoplay` 매개변수 없음. 하나라도 아니면(읽기 실패 포함) 그리지 않는다. 주소를 만들거나 고치지 않는다(C9).

### fixture (합성, `youtube.ts`)

| 이름 | 값 (M3a5 계약 예시 + 빈 필드 채움) |
|---|---|
| `exampleYoutubeEmbed` | `id` `youtube-Abc123xyz_0`, `embed_url` `https://www.youtube-nocookie.com/embed/Abc123xyz_0?playsinline=1&rel=0&enablejsapi=1&origin=http%3A%2F%2Flocalhost%3A5173`, `source_name` "KTV 아카이브", `source_url` `https://www.youtube.com/watch?v=Abc123xyz_0`, `license` `provider-terms`, `license_status` `other`, `license_url` `https://www.youtube.com/t/terms`, `attribution` "KTV 아카이브 · YouTube", `description` "시험용 개막식 영상 설명", `captured_at` "1986" `year`, `date_relation` `{same_year, 0}`(1986-09-20 기준), `place_ref` "country:KR", `retrieved_at` 합성 |
| `exampleDaehanNews` | 위와 같고 `id` `youtube-Dhn1715xyz0`(합성), `attribution` "대한뉴스 제1715호 (1988년 9월 17일 자) · 대한뉴스(정부 제작 뉴스영화) · KTV 아카이브 · YouTube", `description` "서울 올림픽 개막", `captured_at` "1988" `approx`, `date_relation` `{approximate, 0}`, `selection.basis` `daehan_news` |
| `exampleYoutubeUndated` | `exampleYoutubeEmbed` 에서 `id` 만 바꾸고 `captured_at`·`date_precision` `null`, `date_relation` `{unknown, 0}` |

ID는 모두 합성이다(실제 영상 ID·응답을 넣지 않음 — `CLAUDE.md` 7절 5항).

### 재생 규칙

| 규칙 | 내용 |
|---|---|
| iframe | `<iframe src={embed_url} title={fallbackAlt} allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowFullScreen loading="lazy" referrerPolicy="strict-origin-when-cross-origin" className={styles.embed}>`. `sandbox` 없음. `strict-origin-when-cross-origin` 은 origin만 보내 YouTube 조건(`Referer`)에 맞고 결과 화면 경로·쿼리는 가지 않는다 |
| 카드 | `<figure className={styles.card}>` 첫 자식이 iframe, 그다음 `MediaCaption`. 다른 형제·덮개 없음 |
| 붙이기 | `loadYouTubeApi()` 가 끝나면 그 iframe 요소에 `new YT.Player(iframe, { events })` — **같은 iframe에는 한 번만**(StrictMode에서 효과가 두 번 돌아도). 언마운트에서 `destroy()` 를 부르지 않고, 그 뒤 오는 이벤트는 무시한다 |
| 준비 | `onReady` 뒤에만 조정에 등록하고 메서드를 부른다. 준비 전에 이미 절반 넘게 보이고 있었으면 준비되자마자 자동 재생 조건을 본다 |
| 자동 재생 | 절반 넘게 보이게 **바뀌었을 때**(또는 그 상태로 준비됐을 때) `!userPaused && canAutoplay()` 면 `coordinator.requestPlay(player)`. `autoplay` 매개변수에 기대지 않는다 |
| 해제 | `useInView` 의 **"안 보임" 보고마다** `coordinator.release(player)`(M3b3b T0과 같음 — 이미 안 보임인 채 사용자가 튼 영상도 화면 밖으로 나가면 멈춤) |
| 하나만 | M3b3 `playbackCoordinator` 그대로. `<video>` 와 YouTube가 섞여도 하나 |
| 소리 | 우리 자동 재생은 `mute()` 뒤 `playVideo()`. 우리가 한 번 `mute()` 한 뒤 `isMuted()` 가 `false` 면 사용자가 켠 것 → 다음 자동 재생에서 `mute()` 하지 않는다(M3b3 규칙 "사용자가 켠 소리는 그대로") |
| 실패 | `onError`(코드와 관계없이) → 그 카드를 캡션까지 그리지 않고, 조정에서 빼고, `onUnavailable(id)` 한 번. 오류 문구 없음 |
| 자동 재생 막힘 | `onAutoplayBlocked` → 아무것도 하지 않는다(YouTube 컨트롤로 볼 수 있음, 오류 문구 없음) |
| 스크립트 실패·준비 안 됨 (결정 1 (a)) | iframe(공식 플레이어)은 그대로. 자동 재생·조정 등록·`onError` 숨김 없음, 오류 문구 없음. 사용자는 눌러서 본다. 시간 제한·숨김 코드를 두지 않는다 |
| 동의 | `useConsent()` 가 `false` 면 `canRender` 가 거짓 → iframe·스크립트·`YT.Player` 0 |
| 크기 | 너비 100%, 16:9, 최소 200×200. 360px 화면에서도 상자가 200×200 이상(좁으면 높이 200, 플레이어가 레터박스) |

### YouTube 상태 처리 (T3 — 권장안, 다르게 하면 이유를 보고)

`<video>` 는 `paused` 를 바로 읽을 수 있지만 YouTube는 상태를 메시지로 늦게 알려 준다. 그래서 "우리 멈춤"을 셀 때를 **마지막으로 보고된 상태**로 정한다.

| 일 | 처리 |
|---|---|
| 우리 `play()` (조정이 부름) | 준비 전이면 아무것도 안 함. `cancelStart` 를 지우고, 사용자가 소리를 켜지 않았으면 `mute()`, 그리고 `playVideo()` |
| 우리 `pause()` | 마지막 상태가 재생(1)·버퍼링(3)이면 `ourPauses += 1` 하고 `pauseVideo()`. `playVideo()` 를 부른 뒤 아직 시작 보고가 없으면 `cancelStart = true` 하고 `pauseVideo()`(세지 않음 — 이 경우 이벤트가 오는지 문서에 없다). 그 밖(멈춤·끝남)이면 아무것도 안 함 |
| `PLAYING`(1) | `cancelStart` 면 → 지우고 `ourPauses += 1`, `pauseVideo()`(늦게 시작한 우리 재생을 멈춤, 알리지 않음). 아니고 `ourPauses > 0` 이면 → 오래된 이벤트, 무시. 그 밖 → `userPaused = false`, `coordinator.notifyPlaying(player)` |
| `PAUSED`(2) | `cancelStart` 면 → 지우고 우리 것. 아니고 `ourPauses > 0` 이면 → 1 줄임(우리 것). 그 밖 → `userPaused = true`(사용자, 또는 브라우저·YouTube가 멈춤 — M3b3 리뷰 권장 5) |
| `ENDED`(0) | `userPaused = true`(`<video>` 의 끝남과 같음 — 다시 들어와도 자동 재생 안 함) |
| 그 밖(−1·3·5) | 마지막 상태로만 기억 |

## 화면

```
[사건 ① 「1988년 하계 올림픽」]
  제24회 서울 하계 올림픽 개막.            ← 원문
  [사진] [사진] [Commons 영상 <video>]
  ┌──────────────────────────────┐
  │                              │  ← iframe (YouTube 공식 플레이어), 너비 100%, 16:9, 최소 200×200
  │   (위·안에 아무것도 없음)     │     title = 「1988년 하계 올림픽」 관련 자료
  └──────────────────────────────┘
  같은 해 무렵  1988년 무렵 촬영(추정)        ← figcaption (MediaCaption 그대로)
  서울 올림픽 개막
  대한뉴스 제1715호 (1988년 9월 17일 자) · 대한뉴스(정부 제작 뉴스영화) · KTV 아카이브 · YouTube
  제공사 이용약관에 따름
  원문 보기 (새 창에서 열림) · 이용약관 (새 창에서 열림)
  출처: 한국어 위키백과 …                 ← 사건 출처 줄
```

## 데모·표본 날짜로 확인할 것

| 날짜 / 경우 | 자동 테스트 (fixture, 가짜 `YT`) | PM — T5 (PR 검수, Playwright UI) | PM — T6 (머지 뒤, M3a5 머지 뒤 `main`) |
|---|---|---|---|
| 1986-09-20 (표본 버튼) | `exampleYoutubeEmbed`: iframe `src` = `embed_url` 그대로, `title` = `fallbackAlt`, `referrerpolicy`, 캡션 "같은 해" "1986년 촬영", "KTV 아카이브 · YouTube", "제공사 이용약관에 따름", "원문 보기"·"이용약관" 링크. 0.51 → `mute` 다음 `playVideo`, 0.5 → `pauseVideo` | 카드 자리·크기·캡션 글자 | 개막식 영상이 절반 넘게 보이면 소리 없이 재생, 벗어나면 멈춤, 누르면 소리, 위에 아무것도 없음. **nocookie + IFrame API 조합이 동작하는지** |
| 1988-09-17 (데모 버튼) | `world1988WithYoutube`: 대한뉴스 캡션에 "대한뉴스 제1715호 (1988년 9월 17일 자)", "대한뉴스(정부 제작 뉴스영화)", 배지 "같은 해 무렵", "1988년 무렵 촬영(추정)". 사건 ②의 `exampleYoutubeUndated` → "촬영 시점 미상", "같은 날" 글자 없음. Commons 2개 + YouTube 2개가 번갈아도 동시에 재생 0~1개 | 네 영상 카드의 자리 | M3a5b 뒤: 제1715호가 보이고 캡션이 맞다 |
| 1969-07-20 (표본 버튼) | (1988 fixture로 대신 — 섞인 재생) | - | M3a4b·M3a5 뒤: Commons 영상과 YouTube가 하나씩만 재생 |
| 2002-06-22, 1995-03-15 | `world2002WithMedia`·`world1995` → `iframe` 0, YouTube 스크립트 0, 오류 문구 없음 | - | 사진만(또는 사건 없음 문구) |
| 재생 오류 | `onError` 2·5·100·101·150·153 각각 → 카드·캡션 사라짐, 다른 카드 그대로, `day_media` 마지막이면 소제목도 | 가짜 오류로 사라지는 것 | (재현되면) 빈 자리 없음 |
| 동의 없음 | 공급자 밖·체크 안 함 → `iframe` 0, `script` 0, `YT.Player` 0 | - | 새로 고친 뒤 입력 화면에서 YouTube 요청 0 (DevTools 네트워크) |
| 스크립트 실패 (결정 1 (a)) | iframe 그대로, 재생 호출 0, 오류 문구 없음 | - | 광고 차단기를 켜 보면(선택) 플레이어는 남고 자동 재생만 없음 |

## 크기 (400줄 규칙)

### 웹 실측 비율

| 기능 | 예상 → 실제 (S1) | 비율 |
|---|---|---|
| M3b2b | 약 315 → 319 | 약 1.01배 |
| M3b3 | 약 300 → 343 | 약 1.14배 |
| M3b3b | 약 317 → 361 | 약 1.14배 |

최근 두 기능이 ×1.14로 같아 가운데 ×1.14, 범위 ×1.0~×1.2를 쓴다. 파일별로 지금 코드의 주석 밀도(`VideoFilePlayer.tsx` 151줄)를 기준으로 셌다.

### 다시 센 값 (S1 — 테스트·fixture·`web/src/test/**`·e2e 제외)

| 작업 | 내용 | 다시 셈 | ×1.14 (범위 ×1.0~×1.2) |
|---|---|---|---|
| T1 | `urls.ts` 약 4, `youtubeApi.ts` 약 50(타입 20 + 불러오기 25 + 주석), `licenseLabel.ts` 약 5, `MediaCaption.tsx` 약 3 | 약 62 | 약 71 (62~74) |
| T2 | `YouTubePlayer.tsx` 카드·붙이기·준비·숨김·동의 약 80, `isDisplayable.ts`(`canRender`·주소 검사) 약 14, `MediaItem.tsx` 약 8, `DayMediaGroups.tsx` 약 4, `MediaCard.module.css` 약 14 | 약 120 | 약 137 (120~144) |
| T3 | `YouTubePlayer.tsx` 재생(플레이어 객체·상태 처리·보임 효과·등록) 약 74, `playbackCoordinator.ts` 주석 약 1 | 약 75 | 약 86 (75~90) |
| **합계** | | **약 257** | **약 293** (257~308) |

- 400 아래라 **한 PR**로 한다. 리뷰 수정(지난 웹 기능 +20~30)까지 넣어도 약 325다. 결정 1 (a)로 스크립트 실패 처리에 더하는 코드는 없다.
- **확인점 (안전장치)**: T1~T2 누적 S1이 **220**(다시 셈 182 × 1.2)을 넘으면 오케스트레이터가 T3 전에 멈추고, T3(재생 규칙)을 별도 PR로 뺄지 PM에게 묻는다. 빼면 이 PR은 "YouTube 카드 — 자동 재생 없이 눌러서 재생"으로 머지된다(YouTube 규칙 위반 없음). 넘지 않으면 그대로 T3로 간다.
- 시험(S2)은 약 600~800으로 본다(가짜 `YT`, 단위 시험, e2e 새 파일). S1에 넣지 않는다(PM 규칙 2026-10-14).

## 테스트 방침

M1b·M3b2·M3b2b·M3b3·M3b3b "테스트 방침"을 따른다.

- **외부 네트워크 0** (`CLAUDE.md` 7절 5항): YouTube 스크립트를 실제로 불러오지 않는다. Vitest(jsdom)는 iframe·`script` 를 불러오지 않는다(jsdom `resources` 설정을 바꾸지 않는다) — `script` 요소 수와 가짜 `YT` 호출만 본다. MSW `onUnhandledRequest: 'error'` 그대로.
- **가짜 `YT`** (`web/src/test/youtubeFake.ts`): IFrame API 문서 [103]의 필드 구조로 만든 합성 스텁. 실제 응답·실제 영상 ID를 넣지 않는다. 시험 파일에서 설치하고 시험마다 걷는다(`setup.ts` 에 넣지 않음). 상태 이벤트를 미뤘다 순서대로 내보낼 수 있어 경쟁 상황(T3 (6)·(7))을 재현한다.
- **Playwright**: `interceptNetwork` 그대로(밖 요청은 `abort`, 응답받은 밖 요청 0건). iframe 탐색 요청(`www.youtube-nocookie.com`)도 막혀 iframe 안은 빈 오류 화면이다 — **크기·자리·재생 호출만** 본다. 가짜 `YT` 는 `page.addInitScript` 로 앱보다 먼저 넣는다(그러면 앱은 스크립트를 넣지 않는다). 가짜 없이 도는 시험 하나로 "스크립트 요청 1회(막힘)·iframe 남음"(결정 1 (a))을 본다. 개발 서버는 StrictMode라 "같은 iframe에 `YT.Player` 한 번"을 실제로 확인한다.
- 날짜는 데모 3개·표본 2개·합성 fixture 값만. 실제 사람의 생년월일 없음. 어떤 서비스에도 가입하거나 약관에 동의하지 않는다(동의 체크는 우리 화면의 체크 상자일 뿐).
- 기존 시험의 기대값을 바꾸면(C7, `MediaItem.test.tsx` 의 "renders nothing for video_embed yet" 이름 등) 보고에 표로(파일·시험 이름·바뀐 줄·이유). 지운 시험 없음.

## 작업 목록

순서: T1 → T2 → **확인점 (누적 S1 220)** → T3 → T4 → T5(PR 검수 때) → 머지(결정 2 (a)) → T6(M3a5 머지 뒤, `main` 에서). 모두 `web/` 안이고 서버 브랜치(`feat/event-photos` 등)와 폴더가 달라 병렬이다. T1~T3은 같은 파일(`YouTubePlayer.tsx`)을 이어 쓰므로 한 worktree에서 차례로.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | IFrame API 불러오기, 외부 주소, 캡션의 `provider-terms`, 가짜 `YT` | frontend-dev | `web/src/external/urls.ts`, `web/src/media/youtubeApi.ts`, `licenseLabel.ts`·`MediaCaption.tsx` 수정, `web/src/privacy/privacy.test.ts`(C7), `web/src/test/youtubeFake.ts`, 테스트 | M3b3·M3b3b 머지(완료) | (1) `urls.ts` 에 `YOUTUBE_IFRAME_API_URL = 'https://www.youtube.com/iframe_api'`, `YOUTUBE_EMBED_ORIGIN = 'https://www.youtube-nocookie.com'`. 기존 3개 값 그대로. (2) **C7 시험**: 방침 페이지 절대 주소 집합 = {`YOUTUBE_TERMS_URL`, `GOOGLE_PRIVACY_URL`, `CONTACT_ISSUES_URL`}, 그리고 `urls.ts` 가 내보내는 모든 값이 그 3개 ∪ 플레이어 주소 2개 안에 있다(분류 없이 새 값을 더하면 실패하는 것을 시험으로 보임). 바뀐 줄을 보고. (3) `loadYouTubeApi()`: `window.YT?.Player` 가 있으면 그것으로 끝나고 `script` 0개. 없으면 여러 번 불러도 `script[src="https://www.youtube.com/iframe_api"]` 정확히 1개, 가짜 `YT` 를 설치하고 `onYouTubeIframeAPIReady()` 를 부르면 모든 호출이 같은 `YT` 로 끝남, 먼저 있던 `onYouTubeIframeAPIReady` 도 1회 불림. 스크립트 `error` → 실패로 끝나고 다시 불러도 `script` 가 늘지 않음, 던지지 않음. 시험끼리 상태가 새지 않는다(순서를 바꿔도 통과). (4) `licenseLabel`: `license` `"provider-terms"`(앞뒤 공백 포함)·상태 `other` → "제공사 이용약관에 따름". 상태 `unknown` → "라이선스 확인 안 됨". 다른 이름·기존 시험은 고치지 않고 통과. `licenseInAttribution` 은 "제공사 이용약관에 따름"이 `attribution` 에 없으면 줄을 보인다. (5) `MediaCaption`: `provider-terms` 이고 `license_url` 이 `http(s)` 면 링크 글자 "이용약관"(새 창, `rel="noopener noreferrer"`, 숨은 "(새 창에서 열림)"), 아니면 "라이선스" 그대로. (6) `youtubeFake.ts` 와 그 시험(`mediaFakes.test.ts` 처럼): 생성 기록(요소별), 메서드 감시, 상태·오류·자동 재생 막힘 보내기, 이벤트 미루기·내보내기, 기본 상태 전이(위 부품 표), `uninstall` 뒤 `window.YT` 원래대로. (7) C1·C5·C9·C10·C11 출력 없음, C3 `0`, N1·P1 출력 없음, 웹 전체 검사(`lint`·`format:check`·`typecheck`·`test`) 통과, `npm run e2e` 통과(기존 48개), 보고에 T1 S1 |
| T2 | `YouTubePlayer` 카드: iframe, 붙이기, 준비, 오류 숨김, 동의, `canRender`·`MediaItem`·`DayMediaGroups`, CSS, fixture | frontend-dev | `web/src/media/YouTubePlayer.tsx`, `isDisplayable.ts`·`MediaItem.tsx`·`MediaCard.module.css`·`web/src/world/DayMediaGroups.tsx` 수정, `web/src/mocks/fixtures/world/youtube.ts`·`responses.ts`·`README.md`, 테스트 | T1 | (1) 동의(공급자 + 동의 `true`) 뒤 `exampleYoutubeEmbed` → `figure` 하나에 첫 자식 `iframe`, 다음 `figcaption`. iframe: `src` === `embed_url`(글자 그대로), `title` === `fallbackAlt`, `allow` 에 `autoplay`·`encrypted-media`·`picture-in-picture`·`fullscreen`, `allowfullscreen`, `loading="lazy"`, `referrerpolicy="strict-origin-when-cross-origin"`, `sandbox` 없음. (2) 덮개 없음: iframe 자식 0, `figure` 의 자식은 iframe과 `figcaption` 둘뿐, `.embed`·`.card` 규칙에 `position: absolute|fixed` 없음. (3) `YT.Player` 는 그 iframe 요소로 1회 생성 — `<StrictMode>` 로 감싸 그려도 1회. 언마운트 뒤 오는 이벤트는 오류·조정 호출 없음, `destroy` 0회. (4) `onError` 2·5·100·101·150·153 각각 → 그 카드와 캡션이 사라지고 `onUnavailable(id)` 1회, 다른 카드 그대로, 오류 문구(/오류\|실패\|재생할 수 없/) 없음. `day_media` 에서 마지막 항목이면 소제목도 사라진다. (5) `canRender(media, consented)`: `video_embed` 는 `consented` 이고 주소 검사 통과일 때만 참. 거짓이어야 하는 경우 각각 시험: 동의 없음, `provider` `"example"`, origin `https://www.youtube.com`, `http://www.youtube-nocookie.com`, 경로가 `/embed/` 아님, `enablejsapi` 없음·`0`, `autoplay=1` 있음, 읽을 수 없는 주소. 거짓이면 `MediaItem` 이 아무것도 그리지 않는다. `image`·`video_file` 은 동의와 무관하게 지금과 같다. (6) 동의 없음(공급자 밖, 또는 공급자 안 체크 전): `iframe` 0, `script[src*="iframe_api"]` 0, `YT.Player` 생성 0. (7) `DayMediaGroups`: 동의 없으면 `video_embed` 만 있는 묶음은 소제목 없음, 동의 있으면 그 아래 YouTube 카드, iframe `title` "기록 영상". (8) 스크립트 실패(결정 1 (a)): iframe 그대로, `YT.Player` 0, 오류 문구 없음, 던지지 않음, 숨김·시간 제한 코드 없음. (9) 캡션: 1986 행·1988 행·촬영 시점 미상 행(위 "데모·표본" 표)의 글자. `exampleDaehanNews` 의 `attribution` 은 자르지 않고 그대로. (10) CSS: `.embed` 가 위 부품 표대로, `cssRules.test.ts` 통과(색·글자 크기 토큰, 쓰는 `var(--…)` 정의됨). (11) 기존 `MediaItem.test.tsx`·`worldVideo.test.tsx`·`MediaFigure.test.tsx`·`isDisplayable.test.ts` 는 기대값을 바꾸지 않고 통과(이름만 바꾸면 표로). (12) fixture: `youtube.ts` 세 레코드가 `isDisplayable` 통과, README "Synthetic" 표에 행, 기본 핸들러 그대로, `worldFixtures.test.ts` 고치지 않고 통과. (13) C1·C5·C6·C9·C10·C11 출력 없음, C3 `0`, N1·P1 출력 없음, 웹 전체 검사·`npm run e2e` 통과, 보고에 **T1~T2 누적 S1 — 확인점 220** (넘으면 오케스트레이터가 T3 전에 멈추고 PM에게 묻는다) |
| T3 | 재생 규칙: 조정 등록, 보임·안 보임, 우리 멈춤 카운터, 소리, 상태 처리 | frontend-dev | `YouTubePlayer.tsx`, `playbackCoordinator.ts`(주석), 테스트 | T2 (확인점 통과) | **시험을 먼저 쓰고 T2 코드에서 실패하는 것을 보고에 적는다.** 가짜 관찰자(`mediaFakes.ts`)와 가짜 `YT` 로: (1) 준비 뒤 0.51 → `mute()` 다음 `playVideo()` 각 1회(순서 확인). 준비 전에 0.51이 먼저 왔어도 준비되면 1회. (2) 재생 보고 뒤 0.5 → `pauseVideo()` 1회. 다른 플레이어가 재생 중일 때 이 카드의 "안 보임" 보고가 연달아 와도 그 플레이어 멈춤 0회. "보임" 보고가 연달아 두 번 와도 `playVideo()` 1회. (3) (M3b3b T0 (1)·(2)) 0.3에서 우리가 부르지 않은 `PLAYING`(사용자 재생) → 재생 중이던 `<video>` 가 멈추고(`notifyPlaying`), 그 뒤 0 보고 → `pauseVideo()` 1회. 사용자가 소리를 켠 경우(`isMuted()` `false`)도 같다. (4) (T0 (3)) 그렇게 우리가 멈춘 카드는 사용자 멈춤이 아니다: 다시 0.51 → `playVideo()` 1회, 소리를 켰다면 `mute()` 0회. (5) 우리가 부르지 않은 `PAUSED` 뒤 0.5 → 0.51 → `playVideo()` 0회. 사용자가 다시 재생하면(`PLAYING`) 규칙이 다시 돈다. (6) (T0 (6), 카운터) 이벤트를 미뤄 두고 우리 `pause` → `play` → `pause` 를 부른 뒤 이벤트를 순서대로 내보내도 사용자 멈춤으로 읽지 않는다: 0.5 → 0.51이면 `playVideo()` 가 불린다. 그 사이의 오래된 `PLAYING` 은 `notifyPlaying` 을 부르지 않는다(다른 플레이어 멈춤 0회). (7) 시작 보고 전 취소: `playVideo()` 뒤 상태 보고 전에 조정이 멈추면 `pauseVideo()` 가 불리고, 뒤늦은 `PLAYING` 이 오면 `pauseVideo()` 를 한 번 더 부르고 `notifyPlaying` 은 하지 않는다. 뒤늦은 `PAUSED` 는 사용자 멈춤이 아니다. 가짜의 "시작 전 멈춤 → 이벤트 없음"과 "→ `PAUSED`" 두 모드 모두 통과. (8) YouTube 2개, YouTube + `<video>` 가 같은 때 절반 넘게 보이면 끝에 재생 중은 정확히 하나. (9) `ENDED` 뒤 다시 0.51이어도 `playVideo()` 0회. (10) `canAutoplay()` 거짓(움직임 줄이기·데이터 절약) → `mute()`·`playVideo()` 0회, 사용자 재생은 지금처럼 조정에 알림. (11) `onAutoplayBlocked` → 카드 그대로, 오류 문구 없음. (12) 기존 `useInView`·`VideoFilePlayer`·`PlaybackProvider`·`StoryScreenPlayback`·`playbackCoordinator` 시험은 고치지 않고 통과, 조정 인터페이스 그대로. (13) C1·C5·C9·C10·C11 출력 없음, C3 `0`, N1·P1 출력 없음, 웹 전체 검사·`npm run e2e` 통과, 보고에 누적 S1 |
| T4 | 완료 기준 검증, 실제 브라우저 확인(e2e), 전체 테스트 | test-engineer | 빠진 기준의 테스트, `web/e2e/youtube-player.spec.ts`(+ 필요하면 `web/e2e/youtubeFake.ts`) | T3 | (1) T1~T3 기준 표, 빠진 시험 추가. (2) e2e(360·1280, 재시도 0), `world1988WithYoutube`, 가짜 `YT` 를 `addInitScript` 로: (a) YouTube iframe마다 경계 상자 200×200 이상. (b) 가운데로 스크롤한 뒤 iframe 가운데와 네 모서리 2px 안쪽에서 `document.elementFromPoint` 가 iframe 자신. (c) 가로 스크롤 없음. (d) 대한뉴스 카드를 가운데로 → `mute` 다음 `playVideo` 기록, 보이는 비율 0.5 이하로 → `pauseVideo` 기록. (e) Commons `<video>` 와 YouTube를 3번씩 번갈아 가운데로 → 동시에 재생 중(가짜 상태 1 + `video.paused === false`)은 언제나 1개 이하. (f) 움직임 줄이기 → `playVideo` 0. (g) 가짜로 `onError(150)` → 그 카드·캡션 사라짐, 사건 원문·사진·출처 줄 그대로, 오류 문구 없음. (h) 가짜 있음: `iframe_api` 요청 0. 가짜 없음(결정 1 (a)): `iframe_api` 요청 정확히 1(막힘), iframe 남음, `playVideo` 기록 0. (i) 개발 서버(StrictMode)에서 iframe마다 `YT.Player` 생성 1. (j) (T0 규칙, 실제 브라우저) 대한뉴스 카드를 보이는 비율 0.05~0.5에 두고 가짜로 사용자 재생(`PLAYING`) → 화면 밖(비율 0)으로 → `pauseVideo` 기록. (k) 모든 시험에서 `pageerror` 0, 응답받은 밖 요청 0, 막힌 밖 요청은 fixture의 `embed_url` 들·(h)의 `iframe_api`·M3b3 영상 파일뿐. (3) 기존 `consent.spec.ts` 의 동의 전 "밖 요청 0"이 그대로 통과(YouTube 포함). (4) `npm test` 3회, `npm run e2e` 3회 반복 안정(기존 48개 포함). (5) B1 `ok`, C1~C11 기준대로(C7은 시험 통과), N1·P1 출력 없음. (6) "데모·표본" 표 자동 테스트 열의 행마다 덮은 시험을 표로. (7) S1·S2·S3 보고 |
| T5 | PR 검수 (실제 YouTube 없이) | PM | - | T4 | (1) `cd web && npx playwright test e2e/youtube-player.spec.ts --ui` 로 카드 자리·크기·캡션 글자를 본다(iframe 안은 막혀서 비어 있음 — 정상). (2) 캡션 문구 "제공사 이용약관에 따름"·"이용약관"이 괜찮은지 PR 코멘트로. (3) 머지(결정 2 (a)). 방침은 바뀌지 않았다(P1 출력 없음 — 시행일 커밋 필요 없음) |
| T6 | 실제 YouTube 확인 (머지 뒤, M3a5 머지 뒤 `main` 에서) | PM | - | 이 PR 머지, M3a5 머지 (1988은 M3a5b) | 서버(키 설정)·웹을 띄우고 동의 → 표본 "1986년 9월 20일": 절반 넘게 보이면 소리 없이 재생, 벗어나면 멈춤, 누르면 소리, 한 번에 하나(1969 표본에서 Commons 영상과 함께), 캡션에 YouTube·"이용약관", 휴대폰 폭(360px)에서 플레이어가 200×200보다 작아지지 않음. **nocookie + IFrame API 조합**: 스크롤에 따라 재생·멈춤이 바뀌면 동작. 안 바뀌면 보고 → PM 결정(서버 임베드 도메인 `www.youtube.com` + 웹 `YOUTUBE_EMBED_ORIGIN` + 방침 문구·시행일, `fix/` PR). DevTools: 재생 뒤 `localhost` 의 쿠키·저장소에 생긴 것이 있으면 보고(방침 문장 점검). 알아 둘 것: 탭을 바꾸는 등 브라우저·YouTube가 멈춘 영상은 돌아와도 자동 재생하지 않는다(사용자 멈춤으로 봄, M3b3 리뷰 권장 5 — 탭 숨김 처리는 M6b). M3a5b 뒤 "1988년 9월 17일": 제1715호 캡션 문구 |

완료 기준에 쓰는 명령 (저장소 루트). C2·C3·C4·C5·C6은 `docs/plans/m3b2-world-events-ui.md`, C1·C7·C8·B1은 `docs/plans/m3b3b-privacy-consent.md` 의 같은 이름 명령을 그대로 쓴다. E1은 `docs/plans/m3b2b-world-photos-a11y.md` 의 E1 그대로.

```
# C9 — 프론트엔드가 임베드 주소를 만들지 않음 (테스트·목·시험 지원 코드 제외). 출력 없음
grep -rnE 'youtube(-nocookie)?\.com/embed|/embed/\$\{|embed/" *\+' web/src \
  --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/mocks/' | grep -v '^web/src/test/'
# C10 — IFrame API 주소는 urls.ts 와 불러오기 파일에서만 씀. 두 줄(그 두 파일)만
grep -rlE 'YOUTUBE_IFRAME_API_URL|iframe_api' web/src --exclude='*.test.ts' --exclude='*.test.tsx' \
  | grep -v '^web/src/test/' | sort
#   기대: web/src/external/urls.ts, web/src/media/youtubeApi.ts
# C11 — 원시 postMessage·destroy 안 씀 (V4-5, 범위 밖). 출력 없음
grep -rnE 'postMessage|\.destroy\(' web/src --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/test/'
# P1 — 개인정보처리방침이 바뀌지 않음 (바뀌면 시행일 절차). 출력 없음
git diff --stat main...HEAD -- web/privacy.html
# N1 — 새 npm 패키지 없음 (YT 타입은 직접). 출력 없음
git diff --stat main...HEAD -- web/package.json web/package-lock.json
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
# S3 — 데이터 파일 줄 수 (0이 정상)
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a+0}'
```

담당별: frontend-dev 3 (T1, T2, T3), test-engineer 1 (T4), PM 2 (T5, T6).

## 리스크와 확인할 것

1. **nocookie + IFrame API 조합**(위험 15, V4-4 "미확인") — 문서에 없다. 각 출처는 "확인됨"이고 조합만 T6에서 본다. 서버가 YouTube 항목을 보내기 전(M3a5)에는 화면에 영향이 없다. 안 되면 T6 보고에 선택지를 적는다: (a) 임베드 도메인을 `www.youtube.com` 으로(서버 M3a5 설정 + 웹 `YOUTUBE_EMBED_ORIGIN` 한 줄 + 방침 "개인정보 보호 강화 모드" 문장·시행일), (b) 자동 재생을 포기하고 nocookie 유지(공식 플레이어만 — 결정 1 (a)의 "준비 안 됨" 모양과 같아 웹 코드는 그대로). 웹 쪽 변경은 작다.
2. **시작 전 `pauseVideo()` 의 동작이 문서에 없다** — 이벤트가 오는지 모른다. 그래서 그 경우를 세지 않고 `cancelStart` 로 처리하며, 가짜의 두 모드(이벤트 없음·있음)로 시험한다(T3 (7)). 실제 동작은 T6에서 "두 영상을 빠르게 번갈아 스크롤"로 본다. 다르면 규칙 한두 줄을 고친다(`fix/`).
3. **StrictMode에서 붙이기 두 번** — 개발 서버는 효과를 두 번 돌린다. 같은 iframe에 `YT.Player` 를 두 번 만들면 실제 API가 어떻게 할지 모른다 → 한 번만 만든다(T2 (3), T4 (2)(i)). `destroy()` 는 iframe을 지워 React와 충돌하므로 부르지 않는다(C11).
4. **임베드 허용이어도 막힘**(위험 12) — `onError` 숨김으로 빈 자리가 남지 않는다. 결정 1 (a)에 따라 스크립트를 못 불러온 경우에는 숨김도 없다(YouTube 오류 화면이 남을 수 있음 — PM이 받아들임).
5. **IFrame API 스크립트가 우리 페이지에서 돈다** — 페이지 주소·DOM을 읽을 수 있다. 결과 주소에 생년월일·장소가 없고(M3b2 C4, M2b), 방침이 스크립트를 밝힌다. 화면에 보이는 입력값은 M2b 오프닝 설계 때 다시 본다(M3b3b 리스크 5).
6. **좁은 화면** — 16:9 플레이어는 가로 356px부터 높이 200을 넘는다. 그보다 좁으면 `min-height` 로 200을 지키고 플레이어가 레터박스를 그린다(T4 (2)(a), 360px). 카드의 둥근 모서리가 iframe 모서리를 자르면 "겹치기"가 되므로 T4 (2)(b)가 모서리 2px 안쪽까지 본다.
7. **데이터 사용량** — iframe 지연 로딩, 절반 넘게 보일 때만, 하나만, 데이터 절약이면 자동 재생 안 함. 스크립트는 YouTube 카드가 있을 때만 한 번.
8. **M3a5 계약 예시의 빈 필드** — 웹은 `place_ref`(또는 좌표)·`retrieved_at` 이 없으면 숨긴다(M3a3 R1). 서버는 이미 R1을 지키지만 예시가 비어 있어 M3a5 계획 개정 때 예시를 채운다(로드맵 고칠 곳).
9. **방침과 구현의 어긋남** — 이 PR은 방침 문장을 사실로 만들 뿐 바꾸지 않는다(P1). M3a5가 "YouTube Data API가 더해질 예정"을 현재형으로 바꿔야 하고, T6에서 도메인이 바뀌면 "개인정보 보호 강화 모드" 문장도 바뀐다 — 둘 다 시행일 절차를 쓴다.
10. **크기** — 다시 셈 약 257, ×1.14면 약 293. 재생 규칙(T3)이 가장 불확실해 T2 뒤 확인점(누적 S1 220)을 둔다.
11. **실제 확인이 머지 뒤다** (결정 2 (a)) — T6에서 나온 문제는 `fix/` 로 고친다. M3a5 전에는 서버가 YouTube 항목을 보내지 않아 사용자 영향이 없다.

## 로드맵 고칠 곳 (오케스트레이터가 반영 — 서버 브랜치도 로드맵을 고치므로 planner는 쓰지 않음)

| 자리 | 지금 | 고칠 내용 |
|---|---|---|
| 머리말 상태 줄 끝 | … M3b3 개정 PM 승인 (2026-10-14) | + "**M3b3 완료 (PR #16, 실제 S1 343)** · M3b3b 개정 PM 승인 (2026-10-15) · **M3b3b 완료 (PR #18, 실제 S1 361 + 리뷰 수정, 방침 시행일 후속 PR #19 — 시행일 2026년 10월 16일)** · **M3b3c 개정 PM 승인 (2026-10-16): 결정 1 (a) 스크립트 실패·준비 안 됨이면 공식 플레이어 그대로, 결정 2 (a) T5 뒤 머지·실제 YouTube 확인 T6은 M3a5 머지 뒤 `main`**" |
| "현재 위치" 웹 줄 | 진행 M3b3 … | "웹: **M3b3** 완료 (PR #16), **M3b3b** 완료 (PR #18·#19). 진행 **M3b3c** `feat/youtube-embed`(개정 PM 승인 2026-10-16 — 결정 1 (a)·2 (a), 약 257, ×1.14 약 293, 확인점 T2 뒤 220). 실제 YouTube 확인(M3b3c T6)은 M3a5 머지 뒤 `main` 에서(1988은 M3a5b 뒤). M3b3c가 M3의 마지막 웹 기능". 날짜 제목을 갱신(서버 줄은 `feat/event-photos` 쪽 수정과 합침) |
| M3 완료·진행·남은 수 줄 | 완료 11, 진행 2, 남은 7 | M3b3·M3b3b를 완료로 옮김(서버 쪽 M3a3b 등은 서버 브랜치 수정과 합쳐 다시 셈). 진행에 M3b3c |
| 기능 표 M3b3 행 | 상태 **진행** | 줄 수 "… / **실제 S1 343** (약 1.14배)", 상태 **완료 (PR #16, `main` 72fd4ae)**. 실제 영상 확인 T6은 M3a4b 머지 뒤 `main` 그대로 |
| 기능 표 M3b3b 행 | 개정 PM 승인 2026-10-15 | 줄 수 "… / **실제 S1 361** (약 1.14배)", 상태 **완료 (PR #18 + 방침 시행일 후속 PR #19)** |
| 기능 표 M3b3c 행 | 130 → 보정 약 220, PM 승인 2026-10-12 | 이름 "(개정 2026-10-16)", 줄 수 "130 → 파일별 다시 셈 약 257 (범위 257~308, 웹 실측 ×1.14면 약 293). 한 PR, 확인점 T2 뒤 누적 S1 220", 선행 "**M3b3 (PR #16)·M3b3b (PR #18·#19) 완료**. PR 머지는 T5 뒤(결정 2 (a)), 실제 YouTube 확인 T6은 M3a5 머지 뒤 `main`(1988은 M3a5b 뒤)", 상태 "**진행** — 개정 PM 승인 2026-10-16 (결정 1 (a)·2 (a))" |
| 기능 표 위 비율 문단 | 그 뒤 웹 실측은 M3b2 ×1.19, M3b2b 약 1.01 — M3b3은 ×1.0~×1.2 | + "M3b3 ×1.14(343), M3b3b ×1.14(361) — M3b3c는 ×1.14(범위 ×1.0~×1.2)" |
| 진행 순서 그림 | `M3b3 ─┐`, `M3b3b ─┴─> M3b3c (PM 화면 확인은 M3a5 뒤)` | `M3b3 ✔`, `M3b3b ✔`, "M3b3c (실제 YouTube 확인 T6은 M3a5 뒤)" |
| 진행 순서 아래 병렬 문단 | M3b3c ← M3a5·M3a5b | "M3b3c ← M3a5·M3a5b (PR 머지는 기다리지 않음, 결정 2 (a))" |
| "M3b3c 계획을 시작할 때" 메모 | 해야 할 일 | "**반영됨** (M3b3c 개정 2026-10-16 — 재생 규칙·YouTube 상태 처리 표, 권장 5는 T6 안내)" |
| M3a5 행 또는 M3a5 메모 | - | + "M3a5 계획 개정 때: (1) 계약 예시에 `lat`·`lng`·`place_ref`·`retrieved_at`·`date_relation.years` 채우기, (2) **웹 작업 하나** — 방침의 'YouTube Data API가 더해질 예정입니다'를 현재형으로 + 시행일(머지하는 날 한 줄 커밋, frontend-dev), (3) 머지 뒤 PM이 M3b3c T6" |
| 리스크 7 | nocookie 조합은 M3b3c T4에서 확인 | "M3b3c T6(M3a5 머지 뒤)에서 확인" |
| 후속 표 "nocookie 대체" | M3b3c T4 결과 | "M3b3c T6 결과 (서버 도메인 + 웹 `YOUTUBE_EMBED_ORIGIN` + 방침·시행일)" |
| 결정 기록 표 | - | + "**M3b3c 개정 결정 1·2** | 1 **(a)** 스크립트 실패·준비 안 됨이면 공식 플레이어 그대로(추가 코드 없음) · 2 **(a)** T5 뒤 머지, 실제 YouTube 확인 T6은 M3a5 머지 뒤 `main`. 둘 다 planner 추천과 같음 | **결정 2026-10-16**" |
| PM 할 일 표 | - | + "M3b3c 개정 계획 검토와 결정 1·2 — **완료 2026-10-16**", "M3b3c T5 (PR 검수 때)", "M3b3c T6 (M3a5 머지 뒤, 1988은 M3a5b 뒤)" |

## PM 결정 (2026-10-16, 기록)

| # | 물은 것 | 결정 | 이 계획서에 반영 |
|---|---|---|---|
| 1 | IFrame API 스크립트를 못 불러오거나(광고 차단기·네트워크) 플레이어가 준비되지 않을 때 YouTube 카드를 어떻게 하나 | **(a)** 공식 플레이어(iframe)를 그대로 둔다 — 자동 재생·하나만 재생·오류 숨김 없이 눌러서 본다. 추가 코드 없음. planner 추천과 같음. 고르지 않은 것: (b) 카드를 숨김(스크립트 오류는 바로, 준비 안 됨은 시간 제한 뒤) | 머리말, 바뀐 점, 범위 밖, 재생 규칙 "스크립트 실패·준비 안 됨", 데모·표본 표 "스크립트 실패" 행, 크기 메모, 테스트 방침, T2 (8), T4 (2)(h), 리스크 1·4 |
| 2 | PR 머지 시점 (처음 승인안대로면 PM 실제 확인이 PR 검수 안에 있어 M3a5 머지까지 PR이 열려 있게 됨) | **(a)** M3b3 결정 1 (a)와 같은 방식: T5(Playwright, 가짜 `YT`)로 머지하고, 실제 YouTube 확인은 머지 뒤 M3a5 머지 뒤 `main` 에서 T6(1988은 M3a5b 뒤). 문제는 `fix/` 로. planner 추천과 같음. 고르지 않은 것: (b) M3a5 머지까지 PR 대기 | 머리말 "선행", 바뀐 점 "작업 나눔", 데모·표본 표의 T5·T6 열, 작업 순서, T5 (3), T6, 리스크 11, 로드맵 고칠 곳 |

이전 결정: 10.1절 9번 (a), 13번 (a), 22번 (a), 23번 (b)(2026-10-11), M3-P8(2026-10-11), 분할(D1 (a))·Playwright CI(D3 (a))(2026-10-12), `web/src/test/**` S1 제외(2026-10-14), M3b3b 결정 1 (a)(T0 재생 규칙 — 이 계획의 T3 기준). 남은 질문은 없다.

★ PM 직접 구현: 없음 — 이 기능에는 백엔드 작업이 없다.

## 다음 단계 제안 (이 PR 밖)

- `web/src/world/WorldSection.test.tsx` 의 오래된 describe 이름 "WorldSection leaves tragic events to M3b2b (T2 (5))" 바꾸기 — 이 PR이 고치는 파일이 아니라 넣지 않았다. 다음 웹 기능(M2b)에서 그 파일을 고칠 때, 또는 작은 `chore/` 로.
- M3a5 계획 개정 때 계약 예시의 빈 필드 채우기와 방침 "YouTube Data API" 문장 현재형(위 로드맵 고칠 곳).
