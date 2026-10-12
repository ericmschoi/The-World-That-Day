# 공식 채널 영상 자동 검색: YouTube (M3a5)

> 브랜치: `feat/official-channel-video` · 작성: planner · 상태: **PM 승인 (2026-10-12)** (처음 승인 2026-10-11) · 2026-10-11 개정: 영상 리서치 T5와 PM 결정(`docs/research/video-sources.md` 10.1절 5~9·22~24번) 반영. 대한뉴스(이전 T2)는 M3a5b `feat/daehan-news-video` 로 나눔(로드맵 D1 (a), PM 결정 2026-10-12). 2026-10-12 갱신: PM이 키를 발급하고 Y1·Y2·N3·Y4를 실행했고(리서치 3.5절), data-researcher가 YouTube 경로(Y-a·Y-d)를 "확인됨"으로 갱신함(6bf0267) → T0b 완료, T0은 Y5만 남음
> 기준: M3-P9 (b), M3-P14(허용 공식 채널만), 안전장치 1~6, `CLAUDE.md` 7절 1·2·3·4·5·6항, `docs/plans/m3a4-event-video.md`(검색 틀·정확도 규칙), `docs/plans/m3a4b-commons-video.md`(결과 캐시), `docs/plans/m3a3-event-media.md`(형식, R5·R9), `docs/research/video-sources.md` 3.2절(V2·V3·V4 카드), 3.5절(PM 실행 결과, [177]), 3절 종합 경로 표·하루 처리량, 4절 YouTube 열, 6.2절, 7절 R-Y1~R-Y7·R-N5·R-N7, 8절 위험 9~17, 9절, 10.1절
> 선행: M3a4b 머지(검색 틀·결과 캐시). 출처 쪽 조건(YouTube 경로 "확인됨")은 6bf0267로 채워졌다. Y5(콘솔 사용량 확인)는 T1을 막지 않고 T4 전까지 하면 된다.
> **화면 노출**: API에는 이 기능 머지 뒤 YouTube 영상이 나온다. 화면에는 M3b3b(동의 단계·개인정보처리방침)와 M3b3c(YouTube 플레이어)가 머지된 뒤에만 나온다(10.1절 9·23번 — "의무가 갖춰지기 전에는 YouTube 영상을 보여 주지 않는다").

## 이번 개정에서 바뀐 것

| 항목 | 전 | 지금 | 근거 |
|---|---|---|---|
| 범위 | YouTube + 대한뉴스(조건부) | **YouTube만**. 대한뉴스는 M3a5b(이전 T2) | 보정 크기 약 575 > 400. 대한뉴스는 PM 작업(CSV·N1·N2)이 남아 막혀 있고 YouTube는 시작할 수 있다. D1 (a) |
| 허용 채널 | 공식 정부·공공기관 채널 목록 | **KTV 아카이브 한 채널**(`UC8_LPVE4Yuc6KF0opF6uS_w`, Y1 확인). KTV 국민방송은 Y2로 보아 더하지 않음, 국가기록원기록온은 후보에서 뺌 | 10.1절 5번 (a) 유지, 3.5절 |
| 검색 | 결과에서 허용 채널만 거름 | **채널 한정** `search.list`(`channelId`) — 사건당 1회 | 10.1절 6번 (a), 3.5절(8회 모두 요청 채널만) |
| 쿼터 | 10,000단위의 80% = 8,000 | **두 버킷**: 검색 하루 100회(상한 80회) / 일반 10,000단위(상한 8,000). 태평양 시간 자정 초기화. 다 쓰면 그날은 건너뜀 | 9절, 10.1절 6번 (a) |
| 임베드 주소 | `?autoplay=1&mute=1&playsinline=1&enablejsapi=1&rel=0` | `?playsinline=1&rel=0&enablejsapi=1&origin=<웹 origin>`. **`autoplay`·`mute` 없음**(재생은 M3b3c가 IFrame API로) | 3.2절 V4 카드, 위험 14 |
| 확인 | `status.embeddable` 만 | + 공개, KR 지역 차단 없음, 연령 제한 아님, 채널 ID, 10분 | R-Y5, R-Y4, 10.1절 3번. Y4 표본 5개는 모두 통과 |
| 촬영 시점 | 명시 날짜만 | + 회고 편집물("YYYY년 M월 D일 오늘")은 비움. 대한뉴스 제목의 "제작일"은 회차 날짜라 이 기능에서는 날짜로 쓰지 않음(M3a5b). `recordingDate` 는 표본 5개 모두 비어 있었다 | R-Y3, R-Y1, 3.5절 Y4 |
| 컬러 복원 재업로드 | 없음 | 「[📺컬러 복원] … [대한뉴스 698호] … 방영」 같은 영상은 **버린다** | R-N5(6bf0267) — "M3a5에서는 쓰지 않음" 추천 |
| 같은 꼭지 여러 ID | 없음 | 같은 꼭지가 두 ID로 올라온 경우 하나만(확인을 통과한 것 중 `publishedAt` 이 이른 것) | R-N7(6bf0267) |
| 출처 표시 | 리서치 값 | `attribution` = "<채널 이름> · YouTube", `license_url` = YouTube 이용약관 | III.F.2.a, III.A.1 |

## 목표

1. M3a4 검색 틀에 **YouTube 공식 채널 어댑터**를 붙인다: 사건마다 KTV 아카이브 한정 `search.list` → 날짜 하나의 후보를 묶어 `videos.list` 1회로 확인 → 맞는 것만 `video_embed`(`provider: youtube`).
2. 쿼터 두 버킷을 지킨다. 넘으면 그날은 YouTube만 건너뛴다(사진·Commons 영상은 그대로, 오류 아님).
3. M3a5b(대한뉴스)가 이 어댑터의 검색 결과를 다시 쓸 수 있게 날짜·사건별 검색 결과를 넘겨줄 자리를 둔다(대조는 M3a5b).

## 범위 밖

- 허용 채널 밖 YouTube(M3-P14), 방송사 채널, KTV 국민방송(Y2로 보아 지금은 넣지 않음 — 넣을 때는 설정값만 더함)
- 채널 한정 없는 검색(Y-b, 10.1절 6번 — Y3 결과가 좋아도 지금 결정은 (a)), 업로드 목록 색인(Y-c, 10.1절 7번 — KTV 아카이브만 1회 약 361단위), 서버 oEmbed 자동 판정
- 컬러 복원 재업로드를 대한뉴스 회차로 쓰기(보조 정규식 `\[대한뉴스\s*(?:제\s*)?(\d+)\s*호\]` 와 "컬러 복원" 캡션) — 후속 작업 후보
- 쿼터 증설 신청(공개 배포 #5 때, 10.1절 6번 (d))
- 대한뉴스 회차 대조·색인 → M3a5b
- 동의 단계·개인정보처리방침·플레이어 → M3b3b·M3b3c
- 영상 내려받기·서버 경유 재생

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| YouTube Data API v3 `search.list` (Y-a) | **확인됨** (3.5절, 6bf0267) | `channelId` + `q` + `type=video` + `videoEmbeddable=true` | 키 `YOUTUBE_API_KEY` 서버 전용, **`x-goog-api-key` 헤더로**(URL에 넣지 않음). 검색 별도 버킷 하루 100회. 서비스 하나에 프로젝트 하나(III.D.1.c) |
| YouTube Data API v3 `videos.list` (Y-d) | **확인됨** (3.5절: `embeddable`·`privacyStatus`·`regionRestriction`·`ytRating`·`duration`, `recordingDate` 표본 모두 비어 있음) | `part=snippet,status,contentDetails,recordingDetails` | 일반 버킷 하루 10,000단위. 메타데이터 캐시 최대 30일(III.E.4.d), 영상 저장 금지(III.E.1.a) |
| YouTube 임베드 플레이어 (V4) | **확인됨** | 임베드 주소 형식 | 200×200 이상, 겹치기 금지, 절반 넘게 보일 때만 자동 재생, 하나만 — M3b3c |

## 규칙

| 규칙 | 내용 |
|---|---|
| 허용 채널 | 설정값 `youtube.allowed-channels` = [`UC8_LPVE4Yuc6KF0opF6uS_w` (KTV 아카이브)]. **채널 ID로** 대조한다(R-Y4). 이름은 보지 않는다 |
| 검색 | 사건마다 채널마다 `search.list` 1회: `part=snippet`, `type=video`, `videoEmbeddable=true`, `channelId`, `q`=대표 링크 제목(한국어), `maxResults=25`, `regionCode=KR`, `relevanceLanguage=ko` |
| 확인 | 날짜 하나의 후보 ID를 묶어 `videos.list` 1회(최대 50개) |
| 버림 (레코드를 만들지 않음, `link_only` 도 아님) | `embeddable` false, `privacyStatus` ≠ `public`, `regionRestriction.blocked` 에 KR이 있거나 `allowed` 가 있는데 KR이 없음, `contentRating.ytRating` = `ytAgeRestricted`, 길이 > 600초 또는 모름(10.1절 3번), 채널 ID가 허용 목록에 없음, `videos.list` 응답에 없는 ID, **컬러 복원 재업로드**(제목에 "컬러 복원"이 있거나 `\[대한뉴스\s*(?:제\s*)?\d+\s*호\]` 가 있음, R-N5) |
| 같은 꼭지 여러 ID (R-N7) | 같은 채널에 같은 꼭지가 다른 ID로 올라온 경우가 있다(3.5절 N3: 736·750·535·601호). 제목(NFC, 공백 정리 뒤)이 같으면 **확인을 통과한 것 중 `publishedAt` 이 가장 이른 하나**만 남긴다(`publishedAt` 은 고르는 데만 쓰고 날짜로 쓰지 않는다). 대한뉴스 제목은 M3a5b가 (호수, 꼭지 제목)으로 같은 규칙을 쓴다 |
| 정확도 | M3a4 규칙(공식 출처 +20, 다른 단계 낱말에 예행연습·카운트다운 포함). 사건어는 제목·설명 |
| 촬영 시점 | 제목·설명에 적힌 날짜(M3a3 파서). `recordingDate` 는 사건 날짜 이하일 때만(표본에서는 늘 비어 있음). `publishedAt` 은 쓰지 않는다. **회고 편집물** — 제목이 설정 패턴 `youtube.retrospective-title-patterns`(처음 `\d{4}년\s*\d{1,2}월\s*\d{1,2}일\s*오늘`)에 맞으면 날짜 점수는 주되 `captured_at` 은 비운다(R-Y3). **대한뉴스 제목**(정규식 `^\s*대한뉴스\s*제\s*\d+\s*호` 에 맞는 제목) — 제목·설명의 날짜("제작일: YYYY-MM-DD" 등)는 회차 날짜라 이 기능에서는 날짜 재료로 쓰지 않는다(`date_text_excluded`). 회차 날짜는 M3a5b가 색인과 대조해 붙인다. 명시가 없으면 `null` → "촬영 시점 미상" |
| 임베드 주소 | `https://www.youtube-nocookie.com/embed/<ID>?playsinline=1&rel=0&enablejsapi=1&origin=<app.web-origin URL 인코딩>`. `autoplay`·`mute` 를 넣지 않는다. ID가 `^[A-Za-z0-9_-]{11}$` 가 아니면 버린다. `app.web-origin` 은 기본 `http://localhost:5173`, 시작할 때 경로 없는 `http(s)` origin인지 확인(아니면 시작 실패) |
| 레코드 값 (4절) | `id` `youtube-<ID>`, `display_mode` `video_embed`, `provider` `youtube`, `url` null, `thumbnail_url` `snippet.thumbnails.medium.url`(크기 함께), `duration_seconds` ISO 8601 → 초, `source_name` `snippet.channelTitle`, `source_url` `https://www.youtube.com/watch?v=<ID>`, `license` `provider-terms`, `license_status` `other`, `license_url` `https://www.youtube.com/t/terms`, `attribution` "<채널 이름> · YouTube", `description` `snippet.description` 첫 줄(최대 200자), `selection.basis` `youtube_official` |
| 허용 제공자 | M3a3 설정 `media.allowed-video-embed-providers` 에 `youtube` |
| 쿼터 — 검색 버킷 | `youtube.daily-search-budget` 80(하루 100회의 80%). 호출 **전에** 남은 횟수를 보고 없으면 부르지 않는다. 오류 응답도 1회로 센다 |
| 쿼터 — 일반 버킷 | `youtube.daily-unit-budget` 8,000(10,000단위의 80%). `videos.list` 1회 = 1단위 |
| 쿼터 — 초기화 | `America/Los_Angeles` 자정(서머타임 반영, 한국 시간 16:00 또는 17:00). 시계는 주입해서 시험한다 |
| 쿼터 — 403 `quotaExceeded` | 그 버킷을 다음 초기화까지 다 쓴 것으로 표시하고 다시 부르지 않는다 |
| 쿼터 — 건너뜀 | 그 사건의 YouTube 결과를 **캐시하지 않는다**(0건으로 저장하지 않음 — 다음 날 다시 찾게). `partial` 은 바꾸지 않는다(오류 아님). 카운터는 메모리에 둔다(서버를 다시 켜면 0부터 — 403 처리가 뒤를 받친다) |
| 캐시 | M3a4b 결과 캐시. `media.search-cache.ttl.youtube` 기본 7일, **30일을 넘게 설정하면 시작 실패**(III.E.4.d) |
| 키 없음 | `YOUTUBE_API_KEY` 가 비면 어댑터를 끈다(요청 0, 시작 WARN 1줄, 값은 출력 안 함) |
| 차단 목록 | M3a3 `media.blocklist` 의 `source: youtube`(영상 ID 또는 채널 ID) |
| `tragic` | 요청 0 |

## API / 데이터 변경

- 응답 형식은 M3a3 그대로이고 `video_embed` 레코드가 새로 나온다. 웹(M3b3c)과의 계약 예시(합성 값):

```json
{
  "id": "youtube-Abc123xyz_0",
  "type": "video",
  "display_mode": "video_embed",
  "url": null,
  "thumbnail_url": "https://i.ytimg.com/vi/Abc123xyz_0/mqdefault.jpg",
  "thumbnail_width": 320,
  "thumbnail_height": 180,
  "video_sources": null,
  "duration_seconds": 117,
  "embed_url": "https://www.youtube-nocookie.com/embed/Abc123xyz_0?playsinline=1&rel=0&enablejsapi=1&origin=http%3A%2F%2Flocalhost%3A5173",
  "provider": "youtube",
  "render": null,
  "link_only_reason": null,
  "source_name": "KTV 아카이브",
  "source_url": "https://www.youtube.com/watch?v=Abc123xyz_0",
  "license": "provider-terms",
  "license_status": "other",
  "license_url": "https://www.youtube.com/t/terms",
  "attribution": "KTV 아카이브 · YouTube",
  "description": "시험용 개막식 영상 설명",
  "captured_at": "1986",
  "date_precision": "year",
  "date_relation": { "kind": "same_year" },
  "selection": { "basis": "youtube_official", "score": 60 }
}
```

- 설정·환경변수: `YOUTUBE_API_KEY`(루트 `.env.example` 에 이름만, 값 없음, `VITE_` 금지), `youtube.*`, `app.web-origin`, `media.search-cache.ttl.youtube`.
- DB 변경 없음(M3a4b 캐시 사용).

## 테스트 방침

- YouTube 응답은 **공식 문서 필드 구조로 만든 합성 fixture**다(#12 (a) — Google 응답은 녹화하지 않는다. 3.5절 값은 fixture 모양의 참고로만 쓰고 원문·실제 ID를 넣지 않는다). fixture 옆 `README.md` 에 근거 문서 URL.
- 테스트는 키를 쓰지 않는다(가짜 키 문자열 + WireMock 주소).
- 쿼터 초기화는 주입한 시계로 시험한다(서머타임 시작·끝 날 포함).
- 실제 결과 품질은 PM 품질 확인(T4).

## 데모·표본 날짜로 확인할 것

| 날짜·경우 | 자동 테스트 (합성 fixture) | PM 품질 확인 (T4) |
|---|---|---|
| 1986-09-20 | 허용 채널·임베드 가능·제목에 "1986년"·붙여 쓴 "서울아시안게임" → `video_embed`, 점수 60, `captured_at` "1986" `year`. 같은 검색의 「대한뉴스 제 1610호-아시안 게임 준비」·「… 카운트 다운」 → 안 붙음 | 결과 표. 3.5절 Y2 1위 개막식 클립 같은 영상이 붙는지 |
| 1988-09-17 | 「대한뉴스 제 1715호-서울 올림픽 개막」(설명 "제작일: 1988-09-17") → 이 기능만으로는 날짜 재료가 없어 50점, **안 붙음**(M3a5b에서 붙음). 「…(종합)예행연습」 → 안 붙음. 허용 채널 밖(같은 이름 다른 채널 ID) → 안 붙음. `embeddable` false / `private` / KR 차단 / 연령 제한 / 601초 → 안 붙음 | 1988 영상은 M3a5b 뒤에 본다 |
| 1969-07-20 | 「아폴로 11호 발사 [1969]」 → 다른 단계 −40으로 안 붙음. 제목 "50주년" → 안 붙음 | 결과 표 |
| 컬러 복원 | 「[📺컬러 복원] 해외 소식 \| … [대한뉴스 698호] 📽️1968년 11월 2일 방영」 → 후보에서 빠짐(점수 계산 전) | - |
| 회고 편집물 | (설정에 시험용 채널을 더해) 「1986년 9월 20일 오늘 #제10회 #아시안게임 #개막」 → 붙고 `captured_at` null | - |
| 같은 꼭지 두 ID | 같은 제목 두 ID, `publishedAt` 다름 → 이른 것 하나만. 이른 것이 `embeddable` false면 다른 하나 | - |
| 2000-02-29 | 「서울 지하철 2호선 개통」 등 → 사건어 불일치(숫자 핵심어)로 0건 → 사진만 | 기준표와 대조 (3.5절: 0건) |
| 2002-06-22 | 허용 채널 결과 0건 → 사진만 | 기준표와 대조 (3.5절: 0건, 설계대로) |
| 1950-06-25, 2014-04-16 | `tragic` → 요청 0 | 같음 |
| 1995-03-15 | 사건 없음 → 요청 0 | 같음 |
| 쿼터 | 검색 79회 쓴 상태 → 1회 호출, 80회 → 호출 0·응답 200·`partial` false·캐시 안 됨. 403 `quotaExceeded` → 그 뒤 초기화까지 호출 0. 태평양 시간 자정이 지나면 다시 호출 | 콘솔 사용량이 예상 안 |
| 키 | 비면 요청 0, WARN 1줄. 요청에 `x-goog-api-key` 헤더가 있고 쿼리에 `key=` 가 없다. 로그에 키 값이 없다 | - |
| 임베드 주소 | `autoplay`·`mute` 없음, `origin` 이 설정값, 잘못된 ID는 버림 | - |

## 크기 (400줄 규칙)

예상: YouTube 어댑터(검색·확인·버림 규칙·레코드·촬영 시점·같은 꼭지·컬러 복원) 약 135, 쿼터(두 버킷·초기화·403) 약 50, 임베드 주소·origin 약 15, 설정·키·`.env.example` 약 20 → **약 220줄, 보정 약 350줄**. T1 뒤 누적(S1)이 220줄을 넘으면 오케스트레이터가 PM에게 알린다.

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T0 | YouTube 키 발급과 확인 명령 | PM | `.env` 의 `YOUTUBE_API_KEY`, 명령 출력을 data-researcher에게 | 리서치 머지 | **대부분 완료(2026-10-11)**: 키 발급, 6.0절 1~3번(Y1·Y2), 8번(N3), 9번(Y4) — 리서치 3.5절. **남음**: (1) 10번 Y5 — 콘솔 할당량 페이지에서 검색 버킷 100회와 일반 10,000단위가 보이고 사용량이 검색 11회·일반 5단위 근처인지(T4 전까지). (2) 4번 Y3 — 고친 6.2절 함수로 다시 실행(선택. Y-b는 쓰지 않는 대안이라 이 기능을 막지 않는다) |
| T0b | 리서치 확신도 갱신 | data-researcher | `docs/research/video-sources.md` | T0 | **완료 (6bf0267)**: Y-a·Y-d "확인됨", R-N5 컬러 복원·R-N7·R-N8·R-T1 낱말 추가. Y5 값은 PM이 확인하면 한 줄 더한다(이 브랜치 커밋) |
| T1 | YouTube 어댑터: 검색·확인·버림 규칙(컬러 복원 포함)·같은 꼭지·레코드·촬영 시점·임베드 주소, 설정, `.env.example` | backend-dev | 어댑터, 설정(`youtube.*`, `app.web-origin`), `.env.example`(루트 파일 예외, M3-P2와 같은 방식), 합성 fixture, 테스트 | M3a4b 머지 | (1) `./gradlew check` 성공. (2) 위 표의 자동 테스트 행 통과(쿼터 행 제외). (3) 키가 비면 요청 0, WARN 1줄. (4) 키는 헤더로만, 로그 캡처 테스트에 키 값 없음. (5) `.env.example` 에 값 없음(chore 계획서 C1), `VITE_` 없음. (6) 응답 `video_embed` 레코드가 R5·R9를 통과하고 위 계약 예시와 필드가 같다. (7) M3a5b가 쓸 "날짜·사건별 검색 결과" 조회 지점이 있다(테스트로 확인) |
| T2 | 쿼터 두 버킷과 YouTube 캐시 TTL 검증 | backend-dev | 쿼터 카운터, 설정, 테스트 | T1 | (1) `./gradlew check` 성공. (2) 위 표 쿼터 행 통과(서머타임 날 포함). (3) 쿼터로 건너뛴 사건은 캐시에 없다. (4) `media.search-cache.ttl.youtube` 가 31일이면 시작 실패 |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T2 | (1) 기준 표. (2) `./gradlew check` 통과, 3회 반복. (3) C2 출력 없음. (4) S1 보고 |
| T4 | PM 품질 확인 (PR 검수 때) | PM | - | T3, T0 Y5 | M3a4b P1 명령으로 데모 3 + 표본 5 날짜를 뽑아 기준표와 대조: 허용 채널 밖 영상 없음, 엉뚱한 영상 없음(예행연습·카운트다운·발사·다른 노선·컬러 복원), 촬영 시점이 명시되지 않은 영상은 `captured_at` null, `embed_url` 에 `autoplay`·`mute` 없음. 콘솔에서 그날 검색 사용량이 예상(새 날짜 하나당 사건 수만큼) 안인지 |

```
# C2 — 키 값이 코드·설정 파일에 없는지. 출력 없음 (값은 .env 에만)
grep -rnE 'AIza[0-9A-Za-z_-]{20,}' server/src .env.example
# S1 — M3a4와 같은 명령
```

담당별: PM 2(T0, T4), data-researcher 1(완료), backend-dev 2, test-engineer 1.

## 리스크와 확인할 것

1. **검색 하루 100회**(위험 B·9) — KTV 아카이브 한 채널·날짜당 사건 3개·80% 상한이면 캐시에 없는 새 날짜 **하루 약 26개**다. 넘으면 그날 YouTube만 빠진다(정상). PM이 확인 명령을 실행한 날은 검색 11~12회를 먼저 쓴다. 공개 배포(#5) 때 증설(준수 감사)을 다시 본다.
2. **국내 데모 영상은 M3a5b에 달려 있다** — 1988-09-17의 KTV 아카이브 영상은 대한뉴스 꼭지(제1715호)라, 회차 날짜를 붙이는 M3a5b가 있어야 기준선을 넘는다. 이 기능만으로 붙는 데모·표본 영상은 1986-09-20 개막식 클립 정도다. M3a5b는 PM 작업(공공데이터포털 활용신청·N1·N2·CSV)이 끝나야 시작한다.
3. **임베드 허용이어도 재생이 막힐 수 있다**(위험 12) — M3b3c가 재생 오류 때 그 항목을 숨긴다(10.1절 22번).
4. **nocookie + IFrame API 조합**(위험 15) — 문서에 없다. M3b3c 브라우저 확인에서 안 되면 이 기능의 임베드 도메인을 `www.youtube.com` 으로 바꿔야 할 수 있다. 그때 PM에게 묻는다(개인정보처리방침 문구도 바뀐다).
5. **기관 이용 조건**(위험 16, 10.1절 8번) — KTV 아카이브 채널 설명의 "부분발췌, 상업적 이용 등 무단사용을 금합니다"가 확인됐다(3.5절). 결정 (a)(비상업 공식 임베드 + 차단 목록) 그대로, 공개 배포 전 KTV 서면 확인의 필요가 커졌다(로드맵 후속).
6. **약관 개정**(위험 17) — 2026-09-14 갱신. 구현 직전(T1 시작 때) backend-dev가 리서치 3.2절 조항 링크가 바뀌지 않았는지 보고에 적는다.
7. **엉뚱한 영상** — 같은 주제 다른 날 회차(1711·1704호 예행연습, 1609호 카운트다운, 1717호 폐회식), 발사일 클립, 다른 노선 개통(3.5절 Y2). M3a4 규칙(다른 단계·숫자 핵심어)과 PM 품질 확인.
8. **데이터 합치기 해석**(10.1절 7번 (a)) — 출처를 구분해 보여 준다. 업로드 목록 전체 색인은 하지 않는다.

## PM이 정할 것

없음. 결정됨: M3-P14, 10.1절 5번 (a) KTV 아카이브만(Y2 뒤에도 유지), 6번 (a) 채널 한정 검색 + 캐시, 넘으면 건너뜀, 7번 (a), 8번 (a) + 차단 목록, 9번 (a) 의무는 M3b3(→ M3b3b·M3b3c), 22번 (a), 23번 (b), 24번 (a), 대한뉴스 분리(로드맵 D1 (a), 2026-10-12).
