# YouTube 공식 채널 영상 재생 (M3b3c)

> 브랜치: `feat/youtube-embed` · 작성: planner · 상태: **PM 승인 (2026-10-12)** (M3b3 분할로 새로 생김, 로드맵 D1 (a))
> 기준: `docs/research/video-sources.md` 3.2절 V4 카드(크기·자동 재생·겹치기·`Referer`·제안 주소), 3.2절 V2 카드 출처 표시(III.F.2.a), 7절 최종 추천 "재생 (M3b3)", 8절 위험 12~15·34, 9절(M3b3 행), 10.1절 9·13·22·23번, `docs/plans/m3a5-official-channel-video.md`(계약 예시), `docs/plans/m3a5b-daehan-news-video.md`(대한뉴스 `attribution`), `docs/plans/m3b3-media-video.md`(화면 보임·자동 재생 정책·하나만 재생), `docs/plans/m3b3b-privacy-consent.md`(동의 상태), `CLAUDE.md` 7절 1·5·6항
> 선행: M3b3·M3b3b 머지. PM 화면 확인(T4)에는 M3a5 머지가 필요하고, 데모 1988-09-17의 대한뉴스 영상은 M3a5b 머지 뒤에 보인다(머지 전이면 그 부분은 그 기능의 PR에서 본다).

## 목표

1. `MediaItem` 에 `video_embed`(`provider: youtube`)를 붙인다: 서버가 준 `embed_url` 그대로 iframe을 만들고 **IFrame Player API(`YT.Player`)** 로 제어한다.
2. 화면에 **절반 넘게 보이면** `mute()` → `playVideo()`, 안 보이면 `pauseVideo()`. 한 번에 하나만(M3b3 조정). 움직임 줄이기·데이터 절약이면 자동 재생 안 함(M3-P8).
3. 플레이어는 **200×200 이상**, 위에 아무것도 겹치지 않는다. 캡션은 아래.
4. 재생 오류(`onError` 101·150·100·5·153 등)면 **그 항목을 캡션까지 숨긴다**(10.1절 22번).
5. YouTube 출처 표시와 이용약관 링크: 캡션에 "… · YouTube"(서버 `attribution`)와 "이용약관" 링크(`license_url`). 대한뉴스 영상은 서버가 준 "대한뉴스 제N호 (… 자) · 대한뉴스(정부 제작 뉴스영화) · …" 캡션을 그대로 보인다(10.1절 13번).
6. **동의(M3b3b)가 없으면 YouTube 영상을 그리지 않는다**(10.1절 23번, 방어용 확인).

## 범위 밖

- 임베드 주소 만들기(서버가 준다 — 프론트엔드는 `embed_url` 만 쓴다, C5)
- 원시 `postMessage` 제어(형식이 문서에 없음 — `YT.Player` 만, 9절)
- 제공사 이미지 임베드(`image_embed`) — M3a6 제외
- 자체 플레이어 UI, 서버 변경

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| YouTube 임베드 플레이어(`youtube-nocookie.com`) | **확인됨** (V4). nocookie + IFrame API 조합만 미확인 → T4 브라우저 확인 | 서버 `embed_url` 의 iframe | 200×200 이상, 겹치기·가리기 금지, 절반 넘게 보일 때만 자동 재생, 한 화면에 하나, `Referer` 를 보낸다(없으면 오류 153), 플레이어 수정 금지 |
| YouTube IFrame Player API 스크립트 `https://www.youtube.com/iframe_api` | **확인됨** (V4) | `YT.Player`, `mute`, `playVideo`, `pauseVideo`, `onReady`, `onError` | 처음 YouTube 카드가 화면 가까이 올 때 한 번만 불러온다. 주소는 `web/src/external/urls.ts`(M3b3b) |

## 재생 규칙

| 규칙 | 내용 |
|---|---|
| iframe | `<iframe src={embed_url} title={"영상: " + (설명 첫 20자 또는 출처 이름)} allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen loading="lazy" referrerpolicy="strict-origin-when-cross-origin">`. `title` 에 요청 날짜를 넣지 않는다. `referrerpolicy` 는 origin만 보내므로 YouTube 조건에 맞고 결과 주소의 경로·쿼리는 가지 않는다 |
| 플레이어 연결 | 스크립트가 준비되면 기존 iframe에 `new YT.Player(iframe, { events })` 로 붙는다(`enablejsapi=1` 은 서버 주소에 있음) |
| 자동 재생 | `onReady` 뒤, 보이는 비율 > 0.5 이고 자동 재생 허용(M3b3 정책)이고 조정이 허락하면 `mute()` → `playVideo()`. 0.5 이하가 되면 `pauseVideo()`. `autoplay` 매개변수에 기대지 않는다 |
| 하나만 | M3b3 `playbackCoordinator` 에 등록. `<video>` 와 YouTube가 섞여도 하나만 재생 |
| 크기 | 너비 100%, `aspect-ratio: 16/9`, `min-width: 200px`, `min-height: 200px`. 360px 화면에서도 보이는 상자가 200×200 이상(좁으면 높이가 200으로 유지되고 플레이어가 레터박스를 그림) |
| 겹치기 | 플레이어 상자 안·위에 형제 요소·덮개·`position: absolute` 요소를 두지 않는다. 캡션·출처·날짜는 아래 |
| 실패 | `onError` 가 오면(코드와 관계없이) 그 `MediaItem` 을 캡션까지 그리지 않고 조정에서 뺀다. 오류 문구를 남기지 않는다 |
| 동의 | `useConsent()` 가 `false` 면 `video_embed` 를 그리지 않는다(입력 화면을 거치지 않는 경로 대비) |
| 캡션 | `MediaCaption` 그대로. `license: provider-terms` 의 라이선스 문구는 "제공사 이용약관에 따름", 링크 글자는 "이용약관"(`license_url`). `attribution` 은 서버 값 그대로(자르지 않음). 대한뉴스 `approx` 는 배지 "같은 해 무렵", "1988년 무렵 촬영(추정)"(M3b1 문구 표) |

## 크기 (400줄 규칙)

예상: IFrame API 불러오기(한 번만, 화면 가까이) 약 30, `VideoEmbedFrame`(연결·자동 재생·실패 숨김·동의) 약 70, `licenseLabel` 의 `provider-terms` 문구 약 5, `MediaItem` 분기 약 5, CSS 약 20 → **약 130줄, 보정 약 210~230줄**.

## 테스트 방침

M1b "테스트 방침". `window.YT` 가짜(`Player` 생성자·메서드 감시·이벤트 호출)를 `web/src/test/` 에 둔다. 스크립트를 실제로 불러오지 않는다. 목 데이터는 M3a5 계약 예시 모양(합성 ID). 크기는 M3b2b Playwright 설정으로 360px에서 잰다(외부 요청을 막으므로 iframe은 빈 상자 — 크기만 본다). `web-ci` 의 e2e 단계(D3 (a))에서 함께 돈다.

## 데모·표본 날짜로 확인할 것

| 날짜 | 자동 테스트 (목) | PM 화면 확인 (T4) |
|---|---|---|
| 1986-09-20 (표본 버튼) | `video_embed` 목 → iframe `src`=`embed_url`, `title` 에 날짜 없음, `referrerpolicy`, 캡션 "KTV 아카이브 · YouTube"·"이용약관" 링크(`https://www.youtube.com/t/terms` — 목 값). 비율 0.51 → `mute` 뒤 `playVideo`, 0.5 → `pauseVideo` | 개막식 영상이 보이면 소리 없이 재생, 벗어나면 멈춤, 누르면 소리. 플레이어 위에 아무것도 없음 |
| 1988-09-17 (데모 버튼) | 대한뉴스 목(`daehan_news`) → 캡션에 "대한뉴스 제1715호 (1988년 9월 17일 자)", "대한뉴스(정부 제작 뉴스영화)", "같은 해 무렵". 촬영 시점 미상 `video_embed` 목 → "촬영 시점 미상", "같은 날" 글자 없음 | M3a5b 뒤: 대한뉴스 제1715호가 보이고 캡션이 맞는다 |
| 1969-07-20 (표본 버튼) | Commons `video_file` 과 YouTube가 함께 있는 목 → 둘이 동시에 재생되지 않는다 | Commons·대한뉴스 영상이 하나씩만 재생 |
| 2000-02-29 | 영상 없는 응답 → `iframe` 0개, 오류 문구 없음 | 사진만 |
| 실패 | `onError(150)` → 카드·캡션 사라짐, 다른 카드 그대로 | (재현되면) 빈 자리가 남지 않음 |
| 동의 없음 | `useConsent()` false → `iframe` 0개 | - |

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | IFrame API 불러오기와 `VideoEmbedFrame`, `MediaItem` 분기, `provider-terms` 문구 | frontend-dev | `web/src/media/youtubeApi.ts`, `VideoEmbedFrame.tsx`, `MediaItem.tsx`·`licenseLabel.ts` 수정, `web/src/external/urls.ts`(IFrame API 주소 추가), CSS, 테스트 | M3b3·M3b3b 머지 | (1) 위 표 자동 테스트 행 전부 통과. (2) 스크립트는 YouTube 카드가 있을 때 한 번만 넣는다(카드 2개여도 `script` 1개, 카드가 없으면 0개). (3) iframe 형제·자식에 덮개 요소가 없다. (4) `provider` 가 `youtube` 가 아니거나 `embed_url` 이 `http(s)` 가 아니면 아무것도 그리지 않는다. (5) C1(M3b3b 판) 출력 없음, C5 출력 없음. (6) 전체 검사 통과, S1 보고 |
| T2 | 크기·겹치기 Playwright 확인 | frontend-dev | `web/e2e/youtube-player.spec.ts` | T1 | (1) 360px·1280px에서 YouTube 카드 iframe의 경계 상자가 200×200 이상. (2) iframe 위 같은 좌표의 맨 위 요소가 iframe 자신이다(`elementFromPoint` 로 가운데·네 모서리 안쪽). (3) `localhost` 밖 요청 0건(스크립트 요청도 막고, 테스트는 `window.YT` 가짜를 주입). (4) `npm run e2e` 통과(`web-ci` 포함) |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T2 | (1) 기준 표. (2) `npm test`·`npm run e2e` 3회 반복 안정. (3) C1·C5 출력 없음, C3 `0`. (4) S1 보고 |
| T4 | 실제 화면 확인 (PR 검수 때) | PM | - | T3, M3a5 머지 | 서버·웹(키 설정)을 띄우고 동의 → 표본 "1986년 9월 20일", 데모 "1988년 9월 17일"(M3a5b 뒤): 보이면 소리 없이 재생·벗어나면 멈춤·누르면 소리, 한 번에 하나, 캡션에 YouTube·이용약관, 대한뉴스 캡션 문구, 휴대폰 폭에서 플레이어가 작아지지 않음. **nocookie + IFrame API 조합이 동작하는지**(재생·일시정지가 스크롤에 따라 바뀌는지) — 안 되면 보고(로드맵 후속: 도메인 변경은 PM 결정) |

```
# C1 — M3b3b 계획서와 같음 (web/src/external/urls.ts 만 예외)
# C5 — 프론트엔드가 임베드 주소를 만들지 않는지. 출력 없음
grep -rnE 'youtube(-nocookie)?\.com/embed' web/src --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/mocks/'
# C3 — M3b2 계획서. S1 — M3b2b 계획서
```

담당별: frontend-dev 2, test-engineer 1, PM 1.

## 리스크와 확인할 것

1. **nocookie + IFrame API 조합**(위험 15) — 문서에 없다. 안 되면 서버(M3a5)의 임베드 도메인을 `www.youtube.com` 으로 바꾸거나 개인정보 보호 강화 모드를 포기해야 한다. PM 결정이 필요해지면 T4 보고에 선택지를 적는다.
2. **임베드 허용이어도 막힘**(위험 12) — 실패 숨김으로 빈 자리가 남지 않는다.
3. **IFrame API 스크립트가 우리 페이지에서 돈다** — 페이지 주소·DOM을 읽을 수 있다. 결과 주소에 생년월일·장소가 없고(M3b2 C4, M2b), 방침 (g)에 밝혔다(M3b3b).
4. **좁은 화면** — 16:9 플레이어는 가로 356px부터 높이 200을 넘는다. 그보다 좁으면 `min-height` 로 200을 지키고 플레이어가 레터박스를 그린다(T2로 확인).
5. **데이터 사용량** — 지연 로딩, 절반 넘게 보일 때만, 하나만, 데이터 절약이면 자동 재생 안 함.

## PM이 정할 것

없음. 결정됨: 10.1절 9번 (a), 13번 (a), 22번 (a), 23번 (b), M3-P8(2026-10-11), 분할(로드맵 D1 (a))·Playwright CI(D3 (a))(2026-10-12).
