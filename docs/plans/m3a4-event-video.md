# 영상 자동 검색 틀과 정확도 규칙 (M3a4)

> 브랜치: `feat/event-video` (`main` d566bda — PR #22 M3a3d 머지 뒤 — 에서 갈라짐) · 작성: planner · 상태: **PM 승인 (2026-10-18)** — 결정 1 **(b)** 한 PR(T0~T8), 확인점 T2 뒤 400·T6 뒤 750(둘 다 알림), 결정 2 **(b)** ★ T2는 backend-dev, 결정 3 **(b)** (2026-10-23) 어포스트로피는 낱말을 가르지 않음 — T5에서 (처음 승인 2026-10-11, 다시 승인 2026-10-12). 2026-10-12판의 정확도 규칙(R-T1~R-T4, 숫자 핵심어, 다른 단계 낱말, 10분 상한, 늦은 촬영일은 "촬영 시점 미상")은 그대로다. 2026-10-18 개정은 머지된 코드(M3a3a~M3a3d)에 맞춘 계약, PR #22 리뷰 권장 2개(T0), 다시 셈이다 — 아래 "이번 개정에서 바뀐 것".
> 기준: PM 미디어 결정 2026-10-11, 안전장치 1~6, M3-P9 (b), M3-P11(기준선 60), M3-P12(사건 3개까지), M3-P13, `CLAUDE.md` 7절 1·2·3·5·6항, `docs/product-brief.md` 6.1절·6.3절·12절, `docs/plans/m3a3-event-media.md`(형식, R1~R9), `docs/plans/m3a3a-media-rules.md`(파서·차단·제외), `docs/plans/m3a3d-event-photo-wiring.md`(같은 끝 시각, 사건별 미디어 자리), `docs/research/video-sources.md` 3.5절, 7절 "정확도 규칙 제안 — 종합"·"점수 확인", R-T1~R-T4, R-V1~R-V7, R-Y1·R-Y2·R-Y4, R-N3, 10.1절 1~4·21번, PM 피드백 2026-10-16(다른 날짜를 찾지 않음)
> 선행: **M3a3d 머지됨 (PR #22, `main` d566bda)** — `MediaLookup.lookup`·`EventTarget`·`WorldMedia`, `CommonsMediaLookup` 의 두 경로·`Deadline` try-with-resources, `Deadline` 의 `Interrupted`, `MediaLookupProperties.maxYearsApart`, `WorldStoryResponse.Event.of(KoWikiEvent, Verdict, List<MediaItem>)`. 리서치 `docs/m3-video-sources` 머지됨(PR #9).
> 뒤따르는 기능: M3a4b `feat/commons-video`(Commons 동영상 + 결과 캐시, 키 없음), M3a5 `feat/official-channel-video`(YouTube, PM 키), M3a5b `feat/daehan-news-video`(대한뉴스, PM 파일). 모두 이 검색 틀에 어댑터 하나씩을 붙인다. M3a6(게티)은 M3에서 뺐다(영상 15번 (a)). M3 PR 수는 21개 그대로다.

## 이번 개정에서 바뀐 것 (2026-10-18, `main` d566bda 코드 대조)

| 항목 | 전 (2026-10-12) | 지금 | 이유 |
|---|---|---|---|
| 선행 | "M3a3 `feat/event-media` 머지" | M3a3d 머지(PR #22, d566bda) | M3a3a(파서·차단·제외)·M3a3d(사건별 미디어 자리·같은 끝 시각)를 그대로 쓴다 |
| PR #22 리뷰 권장 | — | **새 T0**: (1) `tragic` 사건에는 `WorldMedia` 가 무엇을 주든 `List.of()`, (2) `SparqlWikidataSignalSource.within` 에 한 줄 주석 | 영상이 같은 자리에 들어오기 전에 6.3절 두 번째 방어선을 둔다 |
| 어댑터 모양 | 사건마다 `find(EventContext)` | **날짜마다 한 번** `find(date, events, dayPathsOff)` → 사건별 후보 | M3a4b가 Wikidata 질의·`P10` 파일 정보·날짜 범주를 날짜당 1회로 묶는다(M3a4b "경로와 요청") |
| 전체 시간 상한 | 새 설정 `media.video.total-timeout` 3s | **없앤다.** 출처마다 작업 하나를 사진 경로와 **같은 `Deadline`**(`media.lookup-deadline`, 3s)에 올린다 | M3a3d가 정한 "미디어 창 하나". 세계 섹션 최악 시간이 늘지 않는다 |
| 후보 형식 | 레코드 + 재료(길이·`retrospective` 포함) | 어댑터가 `MediaFilter`·`MediaNormalizer` 를 거쳐 만든 `MediaItem` + 점수 재료. 길이·라이선스는 레코드에서 읽고, `retrospective` 는 뺀다 | `MediaItem` 은 `MediaNormalizer` 만 만든다(M3a3). 회고 편집물의 `captured_at` 은 M3a5 어댑터가 직접 비운다 — 점수에는 영향이 없다 |
| 촬영 시점 | 틀이 정함 | 출처별로 어댑터가 정한다(표는 그대로). 틀은 공용 도우미 `notAfter` 와 **늦은 촬영 시점 거르기**(어댑터가 실수해도 사건 날짜보다 늦은 `captured_at` 은 붙지 않음)만 둔다 | 출처마다 읽는 곳이 다르다(아래 "촬영 시점 규칙") |
| 글 속 날짜 | "M3a3 파서 보강" | **새 부품** `TextDates`(글 안에서 날짜를 찾음). `CaptureTimeParser` 는 문자열 **전체**가 한 모양일 때만 읽기 때문이다. 영어 3형식 중 2개("July 20, 1969", "20 July 1969")는 이미 있고 서수("17th September 1988")만 더한다. 한국어 날짜는 `TextDates` 만 읽는다 | M3a3a 범위 밖 항목(서수·한국어 → M3a4) |
| 사건어 | 조사를 뺌 | 조사를 떼지 않는다(제목은 명사구, "의"를 떼면 "회의"가 깨짐). 문장 부호는 띄어쓰기로 본다. 핵심어가 0개면 불일치. **숫자 핵심어는 앞뒤가 다른 숫자가 아닐 때만** 맞음("1호선" ↔ "11호선" 불일치) | 안전한 쪽으로 틀린다 |
| 낱말 목록 대조 | 목록마다 달리 적힘 | 한 규칙: 한글이 든 낱말은 양쪽 띄어쓰기를 없앤 부분 문자열, 그 밖은 낱말 경계·대소문자 무시(`MediaFilter` 와 같은 경계). 감점은 **종류마다 한 번** | 시험할 수 있게 |
| `selection` | 모든 미디어에? | **영상 레코드에만** 키가 있다. 사진에는 키가 없다(웹 타입은 `selection?:` 선택 필드라 둘 다 받음). 점수는 정수 | 사진 응답 키(26 + `date_relation`)를 지키는 기존 시험이 그대로 통과 |
| 같은 영상 | 사건 안에서만 | + 같은 `id` 가 두 사건에 나오면 앞 사건에만(사진 규칙과 같음) | M3a3d와 같게 |
| 사건 `media` 순서 | — | `[사진 0~1, 영상 0~1]` | 웹은 목록 순서대로 그린다(`WorldEventItem`) |
| 크기 | 약 230 → 보정 약 370 | 파일별 다시 셈 약 450 → **×1.5 약 675 (600~765)** → PM 결정 1 **(b)** 한 PR + 확인점 2개(알림) | 아래 "크기" |

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| M3-P9 (b) | 아무 날짜에서나 자동으로 찾는다. 맞는 영상이 없으면 사진만(오류 아님) | 결정 2026-10-11 |
| M3-P11, M3-P12 | 기준선 60, 사건 3개까지 | 결정 2026-10-11 |
| 영상 1~4번, 21번 | `deepcat:` 안 씀 / 늦은 촬영일은 범주 직속·`P10` 이면 "촬영 시점 미상"으로 붙음 / 10분 상한 / 언론사 자유 라이선스 영상 허용 + 차단 목록 / 정확도 보정 | 결정 2026-10-11 |
| #22 (c) · 6.3절 | `tragic` 사건에는 미디어 없음, 그날은 날짜 단위 경로 없음 | 결정 |
| M3a3c 결정 6 · PM 피드백 2026-10-16 | 그날에 맞는 미디어가 없으면 없이 낸다. **기능을 보이거나 확인하려고 다른 날짜를 찾거나 제안하지 않는다** | 결정 2026-10-16 |
| M3a3d 결정 2 (b) 연장 | 위키백과 문서 하나가 `UNAVAILABLE` 인 날은 `tragicDay` 와 같다 → 영상의 날짜 단위 경로도 부르지 않는다(사건 단위 경로는 부름) | 이 계획(M3a3d 규칙을 그대로 따름) |
| **M3a4 결정 1** | 크기 약 675 → **(b)** 한 PR(`feat/event-video`, T0~T8). 확인점 1: T2 뒤 누적 S1 400 넘으면 **알림**(멈추지 않음). 확인점 2: T6 뒤 750 넘으면 **알림**(멈추지 않음). M3 PR 21개 그대로 | **결정 2026-10-18** |
| **M3a4 결정 2** | ★ T2(정확도 판정)는 **(b)** backend-dev | **결정 2026-10-18** |

## 목표

1. 출처마다 어댑터 하나로 "날짜의 사건들 → 사건별 후보 영상"을 찾는 **영상 검색 틀**: 후보 형식, 정확도 판정, 우선순위(안전장치 4), 개수, 같은 영상, `tragic`, 출처별 실패 격리, 사진과 같은 끝 시각, `selection` 필드.
2. 리서치 7절 종합안의 **정확도 규칙**(R-T1~R-T4, R-V1~R-V7, R-Y1·R-Y2·R-Y4, R-N3).
3. 실제 외부 출처는 없다. 리서치 7절 "점수 확인" 표의 후보를 **합성 후보**로 만들어 점수가 같게 나오는지 본다.
4. 연결 전에 PR #22 리뷰 권장 2개를 닫는다(T0).

## 범위 밖

- Commons 동영상 어댑터, Wikidata 조회(`P10`·`P373`·`P910`·영어 라벨), 결과 캐시 → M3a4b
- YouTube → M3a5, 대한뉴스 → M3a5b, 게티 → M3에서 제외
- 대표 링크가 사건이 아닐 때의 보정(R-V8) → 후속 후보
- 파일 복사·서버 경유 재생(안전장치 1), 웹 변경(M3b3·M3b3c가 재생을 이미 한다, 웹 `Media.selection` 타입도 있다)
- 다른 날짜로 영상을 찾아 보이게 하는 것 — 하지 않는다(PM 피드백 2026-10-16)
- 위키백과 사건 어댑터의 `error` 공용 검사(M3a3d 다음 단계 제안) — 그대로 후속

## 사용할 외부 출처

**이 기능은 외부를 부르지 않는다**(새 HTTP 코드 없음, 새 녹화 없음, 키 없음). 뒤따르는 기능의 출처와 확신도(`docs/research/video-sources.md` 1절 기능 표, 3.5절):

| 출처·경로 | 확신도 | 쓰는 기능 | 시작 조건 |
|---|---|---|---|
| Commons 동영상 (a-3) `P10` → (a-1) 범주 직속 → (c-1) `Videos taken on` 직속, 변환본 | **확인됨** (키 없음) | M3a4b | 이 기능 머지 |
| YouTube Data API (Y-a 채널 한정 검색, Y-d `videos.list`) | **확인됨** (PM 실행 Y1·Y2·Y4, 3.5절, 6bf0267) | M3a5 | M3a4b 머지. Y5는 M3a5 T4 전 |
| 대한뉴스 색인 (N-a) | **부분 확인** — N1·N2·N-B1 대기. **"확인됨"이 될 때까지 구현하지 않는다**(`CLAUDE.md` 7절 2항) | M3a5b | M3a5 + PM CSV·명령 |
| KTV 아카이브 제목 대조 (N-e, N-f) | 확인됨 (N3) | M3a5b | 같음 |
| 게티 (G-a, G-b) | 부분 확인 — 계약 대기 | (제외) | — |

## 이어받는 계약 (`main` d566bda에서 확인)

| 부품 | 실제 모양 | 이 기능에서 |
|---|---|---|
| 미디어 조회 | `media.MediaLookup`: 추상 `DayMedia dayMedia(LocalDate, boolean tragicDay)` 하나 + `default WorldMedia lookup(LocalDate date, List<EventTarget> targets, boolean tragicDay)`. `record EventTarget(String eventId, String title, String placeRef)`, `record WorldMedia(Map<String, List<MediaItem>> eventMedia, List<MediaItem> dayMedia, boolean failed)` | `EventTarget` 에 `summary`·`wikidataQid` 를 더하고 3인자 생성자를 남긴다(T6) |
| Commons 조회 | `media.commons.CommonsMediaLookup(CommonsClient, CommonsFileMapper, KoWikiPageImages, MediaLookupProperties)`. `lookup` 이 `try (Deadline deadline = Deadline.after(lookupDeadline))` 안에서 날짜 범주(`"commons"`)와 사건 사진(`"media"`) 작업을 올리고 둘 다 `await`, `outcome(...)` 이 `Done`/`TimedOut`/`Interrupted`/`Failed` 를 나눈다. `dayMedia(d, t)` 는 `lookup(d, List.of(), t)` | 출처마다 작업 하나를 **같은 마감**에 올린다(T6). 4인자 생성자는 남기고 영상 없음 |
| 마감 | `wikimedia.Deadline implements AutoCloseable`: `after`, `start(name, Callable)`, `Task.await()` → `Done(value)` / `TimedOut(stopped, late)` / `Failed(cause)`(작업이 던짐) / `Interrupted()`(기다리던 스레드). `CANCEL_GRACE` 1s, `close()` 는 끝나지 않은 작업을 인터럽트 | 그대로 쓴다 |
| 세계 서비스 | `WorldStoryService.find`: 대상 = `normal` 이고 `linkTitles` 가 있는 사건(응답 순서), `noDayMedia` = `tragic` 사건이 있음 **또는** 문서 하나가 `UNAVAILABLE` → `media.lookup(date, targets, noDayMedia)`. **93행**: 모든 사건에 `worldMedia.eventMedia().getOrDefault(id, List.of())` — `tragic` 사건도 | T0이 `tone != NORMAL` 이면 `List.of()` |
| 응답 | `WorldStoryResponse.Event.of(KoWikiEvent, Verdict, List<MediaItem>)` | 그대로 |
| 미디어 항목 | `media.MediaItem(@JsonUnwrapped MediaRecord record, DateRelation dateRelation)` — `null` 키도 씀. Javadoc에 "M3a4 `selection` 은 여기에" | `selection` 을 더한다(T6, 값 없으면 키 없음) |
| 레코드 | `MediaRecord` 26필드(`durationSeconds` `Double`, `licenseStatus`, `capturedAt`·`datePrecision`, `description` …). `DisplayMode.VIDEO_FILE`·`VIDEO_EMBED` | 길이·라이선스는 여기서 읽는다 |
| 거르기·정규화 | `MediaFilter.check(MediaRecord, MediaOrigin)` → `Optional<Reason>`, `MediaNormalizer.normalize(MediaRecord, LocalDate requested)` → `Optional<MediaItem>`. `video_embed` 는 `media.allowed-video-embed-providers`(지금 `[]`, M3a5가 `youtube`)에 있어야 통과 | 어댑터가 부른다(틀은 부르지 않음) |
| 촬영 시점 파서 | `CaptureTimeParser.parse(String)` → `CaptureTime(capturedAt, precision)`, 문자열 **전체**가 한 모양일 때만(`matches()`). 이미 `17 September 1988`·`September 17, 1988`·`September 1988` 을 읽는다. 서수·한국어는 못 읽는다 | 서수만 더한다(T5). 글 속 날짜는 새 `TextDates` |
| 날짜 관계 | `DateRelation.of(capturedAt, precision, requested)` | 그대로 |
| 설정 | `MediaProperties`·`MediaLookupProperties` 가 둘 다 `@ConfigurationProperties("media")`. `media.video.*` 는 새 레코드가 받는다(두 레코드는 모르는 키를 무시) | 새 `VideoSearchProperties` (T1) |
| Wikidata `within` | `SparqlWikidataSignalSource.within(String label, Callable<WikimediaResponse>)` 155행, 패키지 공개, 설명 없음. `SparqlWikidataSignalSourceTest` 두 시험(`callThrowingItsOwnInterruptedExceptionIsNoAnswerWithOneWarn`, `interruptedCallerIsNoAnswerWithoutAWarn`)이 직접 부른다 | T0 주석 |
| 웹 계약 | `web/src/media/types.ts`: `MediaSelection { basis: 'commons_p10' \| 'commons_category' \| 'commons_date_category' \| 'commons_search' \| 'youtube_official' \| 'daehan_news'; score: number }`, `Media.selection?: MediaSelection \| null` | 서버 값이 이 6개와 같음을 시험(T1) |

## 검색 틀

### 부품 (이름은 예시 — 머지 코드 이름은 보고서에 적는다)

```
media.Selection(Basis basis, int score)                       — API 전용, JSON {"basis": "...", "score": 정수}
  Basis: COMMONS_P10, COMMONS_CATEGORY, COMMONS_DATE_CATEGORY, COMMONS_SEARCH, YOUTUBE_OFFICIAL, DAEHAN_NEWS
         (JSON 소문자 snake — 웹 6개와 같음)

media.video.EventContext(eventId, date, title, englishLabel, summary, wikidataQid, placeRef)
  title = 대표 링크 제목(한국어), englishLabel = 사건 QID 영어 라벨(없으면 null — M3a4b가 채움), summary = 사건 원문

media.video.Candidate(MediaItem item, String title, String description, CaptureTime originalCapture,
                      List<LocalDate> explicitDates, EventLink eventLink, boolean official,
                      boolean dateTextExcluded, Selection.Basis basis)
  item            = 어댑터가 MediaFilter → MediaNormalizer 를 거쳐 만든 것(차단·통신사 제외·R1~R9는 어댑터 책임)
  originalCapture = 원본 촬영일 필드(Commons DateTimeOriginal, YouTube recordingDate)를 파서로 읽은 값, 없으면 UNKNOWN
  explicitDates   = 출처가 준 명시 날짜(대한뉴스 회차 날짜)
  EventLink       = P10 | CATEGORY_DIRECT | NONE
  official        = 허용 채널을 채널 ID로 대조한 결과(M3a5)
  dateTextExcluded= 제목·설명의 날짜를 날짜 재료로 쓰지 않음(M3a5가 대한뉴스 제목에)

media.video.VideoAccuracy.judge(EventContext, Candidate) -> Judgement(int score, Reject reject)   — 순수 함수, 로그 없음
  Reject: NO_MATCH(필수 1) | BASIS_RULE(필수 2) | DURATION(필수 3) | LOW_SCORE(필수 4) | LATE_CAPTURE

media.video.EventVideoSource (출처마다, M3a4b부터)                                    — T6
  String key()                                   — "commons", "youtube", "daehan_news" (로그·차단 목록과 같은 소문자 키)
  SourceResult find(LocalDate date, List<EventContext> events, boolean dayPathsOff)
  SourceResult(Map<String, List<Candidate>> byEvent, Map<String, String> englishLabels, Status status)
  Status: OK | FAILED(어댑터가 이미 WARN) | QUOTA_SKIPPED(M3a5 — 실패 아님)

media.video.VideoSearch                                                               — T6
  CommonsMediaLookup.lookup 의 마감에서 출처마다 작업 하나를 시작하고, 끝나면 판정·고르기 → 사건 ID → 영상 0~1, failed
```

### 정확도 규칙 (리서치 7절 종합안 — 2026-10-12판과 같음)

후보마다 점수를 매기고 **필수 조건을 모두 채우고 기준선(`media.video.min-score`, 60) 이상**인 것만 붙인다(M3-P11). 원칙: **확실하지 않으면 붙이지 않는다**.

| 재료 | 점수 | 계산 | 근거 | 작업 |
|---|---|---|---|---|
| 사건 연결 | +50 | `eventLink` 가 `P10` 또는 `CATEGORY_DIRECT`. 하위 범주는 점수 없음(어댑터가 `NONE`) | R-V1, R-V2, 영상 1번 (a) | T2 |
| 날짜 일치 | 같은 날 +40 / 같은 달 +20 / 같은 해 +10 | 재료 중 사건 날짜에 **가장 가까운 하나**: (1) 제목·설명에 적힌 날짜(`TextDates`, `dateTextExcluded` 면 쓰지 않음 — **T5**), (2) `originalCapture` — **사건 날짜보다 늦으면 뺀다**, (3) `explicitDates`. 업로드·게시일(Commons `DateTime`, YouTube `publishedAt`)과 `insource:` 일치는 재료가 아니다(어댑터가 넣지 않음) | R-V3, R-V4, R-Y1, R-N3 | T2 (2)(3), T5 (1) |
| 사건어 일치 | +30 | 아래 "사건어 대조" 중 하나라도 맞으면(둘 다 맞아도 +30) | R-T2, R-T3 | T2 |
| 공식 출처 | +20 | `official` | R-Y4, 안전장치 4 | T2 |
| 자유 라이선스 | +10 | `item.record().licenseStatus() == FREE` | 안전장치 4 | T2 |
| 다른 연도 | −40 | **제목**에 사건 연도와 다른 4자리 연도(1900~2099, 앞뒤가 숫자가 아님). 자료 번호 안 연도도 걸린다(의도). "제 1715호"는 1900 미만이라 안 걸림 | R-V6 | T2 |
| 기념어 | −30 | 제목·설명에 `media.video.commemoration-words` 의 낱말 | R-V5 | T2 |
| 다른 단계 | −40 | 제목·설명에 `media.video.other-stage-words` 의 낱말. **그 낱말이 사건 제목·요약에 있으면 감점하지 않는다**(발사가 사건인 날) | R-T1(6bf0267), 3.5절 Y2 | T2 |

낱말 목록 대조(기념어·다른 단계 공통): 한글이 든 낱말은 양쪽 띄어쓰기를 모두 없앤 부분 문자열("카운트 다운"도 "카운트다운"에 걸림), 그 밖은 앞뒤가 글자·숫자가 아닌 낱말로 대소문자 무시. 감점은 **종류마다 한 번**(기념어 둘이 있어도 −30).

사건어 대조:
- 핵심어: 대표 링크 제목을 문장 부호(괄호·쉼표·마침표·하이픈)를 띄어쓰기로 바꿔 나눈 낱말에서 연도("1988년", "1988")와 `media.video.stopwords` 를 뺀 것. 조사는 떼지 않는다. 영어 라벨도 같은 방식(소문자). 핵심어가 0개면 불일치.
- 한국어: 후보 글(제목 + 설명)과 핵심어의 띄어쓰기를 없애고, 핵심어마다 부분 문자열로 있는지 본다. **절반 이상**(맞은 수 × 2 ≥ 전체)이면 일치. 예: "1986년 아시안 게임" → {아시안, 게임} ↔ "1986년 서울아시안게임 개막식".
- 영어: 영어 라벨 핵심어의 절반 이상이 후보 글에 낱말로 있으면 일치(대소문자 무시). 예: "Apollo 11" ↔ "Historic Apollo 11 Moonwalk Footage".
- **숫자가 든 핵심어는 반드시 있어야 하고, 앞뒤가 다른 숫자가 아니어야 한다.** 예: "서울 지하철 7호선" ↔ "서울 지하철 2호선 개통" → 불일치. "아폴로 11호" ↔ "아폴로 8호" → 불일치. "1호선" ↔ "11호선 개통" → 불일치.
- `englishLabel` 이 `null` 이면 한국어만 본다. 어댑터가 `SourceResult.englishLabels` 로 라벨을 주면 그 출처의 후보에 그것을 쓴다(T6).

필수 조건(점수와 별개):
1. **날짜 일치(같은 해 이상) 또는 사건어 일치 중 하나**. 사건 연결과 자유 라이선스만으로는 붙지 않는다(R-T4). → `NO_MATCH`
2. `COMMONS_DATE_CATEGORY` 는 사건어 일치가 있어야 한다. `COMMONS_SEARCH` 는 **같은 날** 날짜 일치와 사건어 일치가 둘 다 있어야 한다. → `BASIS_RULE`
3. **길이**: `item.record().durationSeconds()` 가 `media.video.max-duration-seconds`(600)보다 길거나 `null` 이면 버린다. → `DURATION`
4. 점수가 기준선 이상. → `LOW_SCORE`
5. (안전 거르기) 후보 `item` 의 `captured_at` 이 사건 날짜보다 늦으면(`day`·`month`·`year` 정밀도로 비교, `approx` 는 비교 안 함 — 대한뉴스 회차는 d ~ d+7) 버린다. 어댑터가 `notAfter` 를 쓰면 생기지 않는다. → `LATE_CAPTURE`

### 촬영 시점 규칙 (출처별 — 어댑터가 정함, 2026-10-12판과 같음)

`captured_at`·`date_precision` 은 **명시된 촬영·방송 날짜**로만 채운다(6.1절). 사건 날짜를 `captured_at` 으로 옮기는 경로는 없다.

| 출처 | `captured_at` 을 읽는 곳 | 비우는 경우 |
|---|---|---|
| Commons (M3a4b) | `DateTimeOriginal` (`CaptureTimeParser`) | 사건 날짜보다 늦음 — `notAfter` 로(R-V3). 설명문 날짜는 점수 재료로만(영상 2번이 (c)를 고르지 않음) |
| YouTube (M3a5) | 제목·설명에 적힌 날짜(`TextDates`), 사건 날짜 이하인 `recordingDate` | 회고 편집물 — 날짜 점수는 주되 비운다(R-Y3, M3a5 어댑터). 대한뉴스 제목(`dateTextExcluded`). `publishedAt` 은 쓰지 않는다 |
| 대한뉴스 (M3a5b) | 색인의 회차 날짜, `approx`(R8로 `captured_at` 은 연도) | 길이 대조 실패(R-N6), 설명 "제작일"과 색인 날짜가 다름(R-N8) |

- 공용 도우미 `notAfter(CaptureTime, LocalDate eventDate)`: 사건 날짜보다 늦으면 `UNKNOWN`, 아니면 그대로(같은 날은 그대로). M3a4b·M3a5가 쓴다.
- 비우면 `date_relation.kind = unknown`, 화면 "촬영 시점 미상". 범주 직속·`P10` 이고 사건어가 맞아 붙은 영상은 촬영 시점이 늦거나 없어도 **"촬영 시점 미상"으로 붙는다**(영상 2번 (b)).

### 고르기·개수·`tragic`·실패 (T6)

| 규칙 | 내용 |
|---|---|
| 대상 | `EventTarget`(= 응답의 `normal` 사건 중 대표 링크가 있는 것, 응답 순서)의 앞 **`media.video.max-events`(3)개**(M3-P12). 4번째부터는 어느 출처의 `events` 에도 없다. `0` 이면 영상 출처를 부르지 않는다 |
| `tragic` | `tragic` 사건(`default` 포함)은 대상이 아니다(M3a3d). `tragicDay`(`tragic` 사건이 있거나 문서 하나가 `UNAVAILABLE`)이면 모든 출처에 `dayPathsOff = true`(날짜 범주 등 날짜 단위 경로 없음). **그리고** T0: `tragic` 사건의 `media` 는 늘 `[]` |
| 판정 | 후보마다 `VideoAccuracy.judge`. `Reject` 가 있으면 버린다 |
| 같은 영상 (사건 안) | 같은 `id` 가 두 출처·경로에서 오면 점수가 높은 쪽. 같으면 `basis` 순서: `daehan_news` > `youtube_official` > `commons_p10` > `commons_category` > `commons_date_category` > `commons_search` |
| 순서 | 안전장치 4: 자유 라이선스 → 공식(`official`) → 그 밖. 같은 단계는 점수 높은 순, 같으면 위 `basis` 순서, 같으면 `id` 순 |
| 개수 | 사건마다 영상 최대 1 |
| 같은 영상 (사건 사이) | 같은 `id` 는 앞 사건에만(사진 규칙과 같음). 앞 사건에서 고른 영상이 뒤 사건의 1순위면 뒤 사건은 다음 후보 |
| 사건 `media` | `[사진(0~1), 영상(0~1)]`. 영상 항목에만 `selection` = `{basis, score}` |
| 영상 없음 | 정상. 사진만 남고 `partial` 은 바뀌지 않는다. 다른 날짜를 찾지 않는다 |
| 실패 | 출처 하나가 `FAILED`·예외·시간 초과·`Interrupted` 면 그 출처만 비고 `partial: true`. `QUOTA_SKIPPED` 는 실패가 아니라 `partial` 을 바꾸지 않는다(M3a5). 사진 경로가 실패해도 영상은 남고, 반대도 같다 |
| 시간 | 출처 작업은 사진 두 경로와 **같은 `Deadline`** 에 올라간다. 끝 시각에 안 끝난 출처만 비운다(그때까지 끝난 출처의 결과는 남는다). 어댑터의 읽기 타임아웃은 `media.lookup-deadline` 이하(M3a4b·M3a5가 지킨다) |
| 로그 | 출처의 시간 초과·예외는 WARN 한 줄 `Video search failed: source=<key> events=<개수> cause=<deadline 또는 예외 클래스 이름>`. `FAILED`(어댑터가 이미 WARN)와 `Interrupted` 는 더 쓰지 않는다. 버린 이유는 DEBUG에 이유별 개수만. 날짜·문서 제목·영상 ID·영상 제목·전체 사건 ID를 쓰지 않는다 |

## API / 데이터 변경

- `GET /api/story/world`: 사건 `media` 안 **영상** 레코드에 API 전용 `selection` 이 붙는다(저장하지 않음 — 스키마 검사 전에 `date_relation` 과 함께 뺀다). 사진 항목은 키가 그대로(26 + `date_relation`, `selection` 키 없음). 그 밖 형식·에러 코드는 그대로. **이 기능에는 어댑터가 없어 실제 응답은 바뀌지 않는다** — 영상은 M3a4b부터 나온다.
- 설정 `media.video.*`(새 `VideoSearchProperties`): `min-score: 60`, `max-events: 3`, `max-duration-seconds: 600`, `commemoration-words: [재현, 기념, 기념식, 주년, 다시 보는, 추모, anniversary, reenactment, montage, highlights, comparison, memorial, commemoration, tribute]`("restored"는 넣지 않음), `other-stage-words: [발사, launch, 리허설, rehearsal, 예행연습, 카운트다운, countdown, 훈련, training, 준비, preparation]`, `stopwords: [the, a, an, of, in, on, at, and, for, to, 및]`(처음 목록 — T4에서 PM이 고침). `min-score`·`max-events` 음수, `max-duration-seconds` 0 이하, 빈 낱말은 시작 실패(오류 메시지에 설정 이름). `total-timeout` 은 없다(`media.lookup-deadline` 을 나눠 씀).
- DB 변경 없음(캐시는 M3a4b). 데이터 파일 없음.

응답 예시 — 영상 항목 하나 (합성: 지어낸 파일, 경계값 날짜 2000-02-29. 데모 날짜에는 이런 항목이 없다):

```json
{
  "id": "commons-900002",
  "type": "video",
  "display_mode": "video_file",
  "url": "https://upload.wikimedia.org/wikipedia/commons/0/00/Example_test_video.webm",
  "thumbnail_url": "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/Example_test_video.webm/330px--Example_test_video.webm.jpg",
  "video_sources": [{ "url": "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/00/Example_test_video.webm/Example_test_video.webm.240p.vp9.webm", "mime": "video/webm", "height": 240 }],
  "duration_seconds": 95.0,
  "source_name": "Wikimedia Commons",
  "license": "CC BY-SA 4.0",
  "license_status": "free",
  "captured_at": null,
  "date_precision": null,
  "place_ref": "country:KR",
  "date_relation": { "kind": "unknown", "years": 0 },
  "selection": { "basis": "commons_category", "score": 90 }
}
```

(나머지 키는 M3a3 형식 그대로 — 값 없는 키는 `null` 로 있다. 사진 항목에는 `selection` 키가 없다.)

## 테스트 방침

- **외부 네트워크를 부르지 않는다.** 이 기능은 외부 호출 코드가 없다. 테스트 안의 가짜 출처(`EventVideoSource` 람다·작은 클래스)가 합성 후보를 돌려준다. WireMock은 기존 세계 API 시험에만 쓰고 새 스텁은 없다.
- 합성 후보는 리서치 7절 "점수 확인" 표에서 만든다. **YouTube·KTV 행(6~9, 1711호·카운트 다운 사례)은 리서치 문서에 이미 적힌 제목 문자열만** 쓰고, 영상 ID·채널 ID·응답 JSON은 넣지 않는다(`CLAUDE.md` 7절 5항 — Google 응답은 합성). 합성 YouTube 항목의 ID·주소는 `youtube-AAAAAAAAAAA`·`https://www.youtube-nocookie.com/embed/AAAAAAAAAAA` 처럼 지어낸 값, 정규화는 `youtube` 를 허용한 테스트용 `MediaProperties` 로.
- 날짜는 공개된 역사 사건 날짜(1969-07-20, 1986-09-20, 1988-09-17)와 경계값(2000-02-29)뿐이다 — 실제 사람의 생년월일이 아니다(M3-P13). 실제 비극 사건 이름은 쓰지 않는다(시험용 `tragic` 사건은 지어낸 문장).
- 데모 날짜에 영상이 없는 것은 정상이다(어댑터가 없고, M3a3c 결정 6). 붙는 경로는 합성으로만 시험하고, **보이려고 다른 날짜를 고르지 않는다.**
- 받아 보지 않은 설명문은 날짜·사건어가 없다고 본 리서치 계산을 그대로 따른다.
- 시간 시험은 짧은 상한과 응답하지 않는 가짜 출처. 상한은 설정값과 `Deadline.CANCEL_GRACE` 에서 계산한다(M3a3d T2 (6)과 같은 식).
- 로그 시험은 `OutputCaptureExtension` 으로 날짜·제목·영상 ID·전체 사건 ID가 없음을 본다.

### 점수 확인 (리서치 7절, 기준선 60) — 합성 후보

| # | 후보 (리서치 근거) | 사건(대표 링크 / 영어 라벨) | 재료 | 기대 점수 | 기대 결과 | 작업 |
|---|---|---|---|---|---|---|
| 1 | `AP11 FINAL APPROACH.ogv` [80] | 아폴로 11호 / Apollo 11 (1969-07-20) | `P10`, 원본 촬영일 1969-07-20, 설명에 "Apollo 11", 자유 | 50+40+30+10 = **130** | 붙음 | T2 |
| 2 | Historic Apollo 11 Moonwalk Footage [82] | 같음 | `CATEGORY_DIRECT`, 원본 촬영일 2019-07-21(늦음 → 재료 아님), 제목에 "Apollo 11", 자유, 후보 `captured_at` null | 50+30+10 = **90** | 붙음, `unknown` | T2 |
| 3 | Jack King's Apollo 11 Launch Commentary [27][82] | 같음 | `CATEGORY_DIRECT`, 원본 촬영일 2015-06-12, 제목에 "Apollo 11"·"Launch", 자유 | 50+30+10−40 = **50** | 버림 (`LOW_SCORE`) | T2 |
| 4 | Nixon speaking with astronauts on the Moon [79] | 같음 | `COMMONS_DATE_CATEGORY`, 원본 촬영일 1969-07-20, 설명에 "Apollo 11", 자유 | 40+30+10 = **80** | 붙음 | T2 |
| 5 | Mónica Mihánovich experiencia [27] | 같음 | `CATEGORY_DIRECT`, 날짜·사건어 없음, 자유 | (60) | 버림 (`NO_MATCH`) | T2 |
| 6 | 「대한뉴스 제 1715호-서울 올림픽 개막」 [120] | 1988년 하계 올림픽 (1988-09-17) | 공식, `explicitDates` 1988-09-17, 사건어 {하계, 올림픽} 중 "올림픽" | 30+20+40 = **90** | 붙음 | T2 |
| 7 | 「대한뉴스 제 736호-사람이 달에 내리다-아폴로 11호 제1신-」 [121] | 아폴로 11호 (1969-07-20) | 공식, `explicitDates` 1969-07-26, 사건어 {아폴로, 11호} | 30+20+20 = **70** | 붙음 | T2 |
| 8 | KTV 아카이브 1986년 서울아시안게임 개막식 [122] | 1986년 아시안 게임 (1986-09-20) | 공식, 제목에 "1986년", 붙여 쓴 "서울아시안게임" | 10+30+20 = **60** | 붙음(경계값) | **T5** |
| 9 | 「1986년 9월 20일 오늘 #제10회 #아시안게임 #개막」 [123] | 같음 | 공식, 제목 날짜 1986-09-20, 사건어 | 40+30+20 = **90** | 붙음 | **T5** |

추가 경계 사례(합성):
- **T2**: 「대한뉴스 제 1711호-개.폐회식 (종합)예행연습」 → 다른 단계 −40 / 「… 카운트 다운」(띄어 씀) → −40 / "서울 지하철 7호선" 사건 ↔ 「서울 지하철 2호선 개통」 → 사건어 불일치 / "1호선" 핵심어 ↔ "11호선" → 불일치 / 지어낸 사건 제목에 "발사" → 제목에 "발사"가 있는 후보도 감점 없음 / 길이 601초 → `DURATION`, 600초 → 통과, `null` → `DURATION` / 제목에 다른 연도 → −40 / 영어 라벨 `null` → 한국어만 / 기준선 60인 7행을 `min-score: 71` 로 → `LOW_SCORE` / 후보 `captured_at` 이 사건 다음 날(`day`) → `LATE_CAPTURE`.
- **T5**: 6행에서 `explicitDates` 를 비우고 설명에 "제작일: 1988-09-17" — `dateTextExcluded` 면 30+20 = **50 버림**, 아니면 40+30+20 = 90 붙음 / 영어 서수 "17th September 1988"이 든 설명 → 같은 날.

## 크기 (400줄 규칙)

제품 코드·설정(S1, `added`) 기준.

### 실측 비율

| 기능 | 파일별 셈 | 실제 S1 | 비율 |
|---|---|---|---|
| M3a3c (PR #20) | 약 255 | 433 / −109 | 약 ×1.7 |
| M3a3d T0 (PR #22) | 약 64 | 84 / −28 | 약 ×1.31 |
| M3a3d T1 (PR #22) | 약 170 | 228 / −44 | 약 ×1.34 |
| M3a3d 합계 | 약 234 (×1.7 보정 약 400) | **312 / −72** | **약 ×1.33** |

이번에는 파일별 셈에 **×1.5**(두 실측의 가운데, 범위 ×1.33~×1.7)를 곱한다.

### 다시 셈

| 작업 | 파일별 셈 | 셈 | ×1.5 | 범위 |
|---|---|---|---|---|
| T0 | `WorldStoryService` 가드(약 4) · `SparqlWikidataSignalSource` 주석(1) | 약 5 | 약 8 | 7~9 |
| T1 | `Selection`·`Basis`(약 20) · `EventContext`(약 10) · `Candidate`·`EventLink`(약 25) · `VideoSearchProperties` 와 검사(약 35) · `application.yml` 블록·주석(약 22) | 약 112 | 약 170 | 150~190 |
| T2 ★ | `VideoAccuracy`: 핵심어·한국어·영어·숫자 경계(약 30), 날짜 재료(약 15), 감점 3종과 낱말 대조(약 25), 필수 조건·기준선·늦은 촬영 시점(약 20) · `Judgement`·`Reject`(약 12) · `notAfter`(약 10) | 약 112 | 약 170 | 150~190 |
| **T0~T2 소계** | | 약 229 | **약 345** | **305~390** |
| T5 | `TextDates`(모양 7개, 겹침, 없는 날짜, 약 45) · `CaptureTimeParser` 서수(약 4) · 판정에 재료 (1) 연결(약 6) | 약 55 | 약 85 | 75~95 |
| T6 | `EventVideoSource`·`SourceResult`·`Status`(약 30) · `VideoSearch`: 출처 작업 시작·결과·실패 격리·쿼터·대상 자르기·판정·같은 영상·순서·사건 사이 중복(약 90) · `MediaItem` `selection`·2인자 생성자·`withSelection`(약 12) · `CommonsMediaLookup` 생성자·작업·합치기(약 25) · `EventTarget` 필드·3인자 생성자(약 6) · `WorldStoryService` 대상 필드(약 3) | 약 166 | 약 250 | 220~280 |
| **T5~T6 소계** | | 약 221 | **약 330** | **295~375** |
| **합계** | | 약 450 | **약 675** | **600~765** |

- 약 675로 400 규칙을 넘지만 **PM 결정 1 (b)로 한 PR**로 간다(2026-10-18). M3 PR 수는 21개 그대로다.
- **확인점 1 (T2 뒤, 알림)**: 누적 S1이 **400** 을 넘으면 오케스트레이터가 PM에게 알리고 **멈추지 않고** T3으로 계속한다(예상 약 345).
- **확인점 2 (T6 뒤, 알림)**: 누적 S1이 **750** 을 넘으면 알리고 **멈추지 않고** T7로 계속한다(예상 약 675. T7·T8은 테스트·확인만이라 S1이 늘지 않는다).
- 두 확인점 모두 멈추는 점이 아니다. 각 backend-dev 보고서(T0·T1·T2·T5·T6)는 누적 S1을 적는다.
- 테스트(S2)는 따로 보고한다.

## 작업 목록

순서: T0 → T1 → T2 → **확인점 1 (400 알림)** → T3 → T5 → T6 → **확인점 2 (750 알림)** → T7 → 리뷰 → PR → T4·T8(PR 검수 때) → 머지 → M3a4b.

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0 | 연결 전 정리: PR #22 리뷰 권장 2개 (`fix(api)`·`chore(wikidata)` 커밋) | backend-dev | M3a3d 머지 (완료) | (1) **`tragic` 두 번째 방어선**: `WorldStoryService.find` 가 `verdict.tone() != Tone.NORMAL` 인 사건에는 `WorldMedia.eventMedia()` 에 무엇이 있든 `Event.of(event, verdict, List.of())` 를 부른다. `WorldStoryServiceEventPhotoTest` 에 새 시험: `Recording.answer` 의 `eventMedia` 가 `tragic` 사건 ID(큐레이션 `tragic` 하나, `default` 하나)와 `normal` 사건 ID 모두에 항목을 줄 때 → `tragic` 두 사건의 `media` 는 `[]`, `normal` 사건은 그 항목 그대로, `day_media`·`partial` 은 `answer` 그대로. 날짜는 2000-02-29, 사건 문장은 지어낸 것. (2) **`within`**: `SparqlWikidataSignalSource.within` 은 패키지 공개 그대로이고 바로 위에 한 줄 주석이 있다 — 왜 공개 범위가 넓은지(테스트가 HTTP 스텁으로 만들 수 없는 `Failed`(작업이 스스로 던진 `InterruptedException`)와 `Interrupted`(기다리던 스레드) 두 경우를 직접 시험). 그 파일의 다른 줄은 바뀌지 않는다(`git diff` 로 확인 — 주석 줄만 더해짐). 공개 API로 시험하는 안은 고르지 않는다(두 경우를 WireMock으로 만들 수 없다). (3) 기존 테스트의 단언은 고치지 않는다. (4) `cd server && ./gradlew check` 성공. 보고서에 누적 S1 |
| T1 | 영상 후보 형식·`selection` 값·설정 (`feat(media)` 커밋) | backend-dev | T0 | (1) "부품" 절의 `Selection`(+`Basis`), `EventContext`, `Candidate`(+`EventLink`)가 있다. `Candidate` 의 `item`·`eventLink`·`basis` 가 `null` 이면 생성 때 예외, `explicitDates` 가 `null` 이면 빈 목록, `originalCapture` 가 `null` 이면 `CaptureTime.UNKNOWN`. (2) `Basis` 의 JSON 값 6개가 `web/src/media/types.ts` 의 `MediaSelection.basis` 6개 문자열과 같다 — 저장소 파일을 읽어 대조하는 시험(기존 `RepositoryFiles.read` 방식). `Selection` 을 JSON으로 쓰면 `{"basis":"commons_p10","score":130}` 처럼 `score` 가 정수다. (3) `VideoSearchProperties`(`@ConfigurationProperties("media.video")`)가 "API / 데이터 변경"의 6개 설정을 받고, `application.yml` 에 주석과 함께 그 값·목록 그대로 있다. 컨텍스트 시험: 기본값 `min-score` 60·`max-events` 3·`max-duration-seconds` 600, 목록 세 개가 계획 표와 같다. (4) 시작 실패(오류 메시지에 설정 이름): `media.video.min-score: -1`, `media.video.max-events: -1`, `media.video.max-duration-seconds: 0`, 빈 낱말(`""`, `" "`)이 든 목록 각각. `max-events: 0` 은 시작한다. (5) `MediaProperties`·`MediaLookupProperties` 의 기존 시험이 그대로 통과(`media.video.*` 가 그 둘을 깨지 않음). (6) 응답은 바뀌지 않는다: 기존 `WorldStory*Test` 단언 그대로 통과. (7) `./gradlew check` 성공. 보고서에 누적 S1 |
| T2 | ★ 정확도 판정: 점수·필수 조건·길이·늦은 촬영 시점, `notAfter` (`feat(media)` 커밋) | backend-dev (★ PM 결정 2 (b), 2026-10-18) | T1 | (1) `VideoAccuracy.judge(EventContext, Candidate)` 가 순수 함수다: 같은 입력에 같은 `Judgement`, 후보 `item` 을 바꾸지 않고(판정 뒤 `equals` 같음), 로그를 쓰지 않는다. (2) "점수 확인" 표 **1~7행**과 "추가 경계 사례"의 **T2** 항목이 매개변수 테스트로 기대 점수·`Reject` 와 같다. (3) 재료마다 단위 테스트: `originalCapture` 같은 날 +40·같은 달 +20·같은 해 +10·다른 해 0, `originalCapture` 가 사건 다음 날(`day`)·다음 달(`month`)·다음 해(`year`)면 날짜 점수 0, `explicitDates` 가 여럿이면 가장 가까운 것, 재료가 둘 이상이면 가장 높은 점수 하나만(합치지 않음), 재료가 없으면 0. (4) 사건어: 띄어쓰기가 다른 한국어 일치, 영어 라벨 일치(대소문자 무시), 핵심어 절반 미만이면 불일치·정확히 절반이면 일치, 숫자 핵심어가 없으면 불일치, "1호선" ↔ "11호선" 불일치, 제목이 연도·`stopwords` 뿐이면(핵심어 0개) 불일치, 괄호가 든 제목의 괄호는 띄어쓰기로, `englishLabel` `null` 이면 영어 대조 없음. (5) 감점: 다른 연도는 제목에서만(설명의 다른 연도는 감점 없음), "제 1715호"·"7호선"은 감점 없음, 2099·1900 경계, 기념어 둘이 있어도 −30 한 번, 다른 단계 −40 한 번, 한국어 낱말은 띄어 쓴 형태("카운트 다운")도, 라틴 낱말은 낱말 경계("relaunch" 는 `launch` 에 안 걸림), 그 낱말이 사건 제목 또는 요약에 있으면 다른 단계 감점 없음(기념어에는 이 예외가 없다). (6) 필수 조건 1~5가 각각 하나만 미달인 후보로 시험되고 해당 `Reject` 를 낸다. 기준선과 같은 점수(60)는 붙고 59는 `LOW_SCORE`. 설정값(`min-score`, `max-duration-seconds`, 낱말 목록)을 바꾸면 결과가 따라 바뀐다. (7) `notAfter`: 사건 날짜보다 늦은 `day`·`month`·`year` 는 `UNKNOWN`, 같은 날·같은 달·같은 해와 이른 값은 그대로, `approx`·`UNKNOWN` 은 그대로. (8) 판정 코드 어디에서도 사건 날짜를 `captured_at` 으로 쓰지 않는다(판정은 `MediaItem` 을 만들지 않음 — 리뷰 확인). (9) `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 1 (400 넘으면 알림, 멈추지 않음)** |
| T3 | T0~T2 중간 검증 | test-engineer | T2 | (1) T0~T2 기준 표(기준마다 테스트 이름). 빠진 기준은 테스트를 더한다(제품 코드는 고치지 않음). (2) `cd server && ./gradlew check` 통과, 3회 반복 안정. (3) 그 시점의 S1·S2·S3(0이 정상) 보고. (4) "점수 확인" 표 1~7행과 T2 경계 사례의 실제 결과 표(후보, 재료, 점수, 붙음/버림, `Reject`). (5) 데모 3날짜(1988-09-17, 2002-06-22, 1995-03-15) 응답이 M3a3d와 같다 — 기존 `WorldStoryDemoTest` 등이 단언을 바꾸지 않고 통과했음을 표로. (6) 새 테스트가 WireMock 밖으로 요청하지 않는다(새 테스트는 HTTP를 쓰지 않음을 확인) |
| T4 | 규칙 검토 (PR 검수 때) | PM | T7 | T3·T7의 결과 표와 `media.video` 낱말 목록 세 개(기념어·다른 단계·일반어)를 보고, 빼거나 더할 낱말을 PR 코멘트로 남긴다(있으면 backend-dev가 새 커밋). 실제 호출 없음 — 실제 품질 확인은 M3a4b T6·M3a5 T4·M3a5b T4에서 |
| T5 | 글 속 날짜: `TextDates`, 파서 서수, 재료 (1) 연결 (`feat(media)` 커밋) | backend-dev | T3 | (1) `TextDates.find(String text)` 가 글 안의 날짜를 나온 순서대로 `CaptureTime` 목록으로 준다. 모양: ISO `YYYY-MM-DD` → `day` / `YYYY년 M월 D일`(띄어쓰기 0~1) → `day` / `YYYY년 M월` → `month` / `YYYY년` → `year` / `Month D, YYYY`·`Month Dth, YYYY` → `day` / `D Month YYYY`·`Dth Month YYYY` → `day` / `Month YYYY` → `month`(영어는 대소문자 무시, 연도 1000~2999). 한 날짜의 긴 모양이 이긴다("1986년 9월 20일"은 `day` 하나, `year` 를 따로 내지 않음). (2) 날짜가 아님: 맨 연도 "1969", "AP11", "제 1715호", "7호선", 연도 없는 "9월 20일", 없는 날("2000-02-30", "1999년 2월 29일" — 건너뜀), 숫자에 붙은 ISO("x19690720"). "2000년 2월 29일"은 `day`. (3) `CaptureTimeParser.parse`: "17th September 1988"·"September 17th, 1988"·"1st July 1969" → `day`. 기존 `CaptureTimeParserTest`·`CaptureTimeParityTest` 단언 그대로 통과. (4) `VideoAccuracy` 의 날짜 재료 (1): 제목·설명의 `TextDates` 결과, `dateTextExcluded` 면 쓰지 않음. "점수 확인" 표 **8·9행**과 "추가 경계 사례"의 **T5** 항목이 매개변수 테스트로 같다. T2의 1~7행 시험이 그대로 통과(8·9행이 더해져도 1~7행 점수 불변). (5) `TextDates` 는 로그를 쓰지 않는다. (6) `./gradlew check` 성공. 보고서에 누적 S1. (7) **PM 결정 3 (b)**: `VideoAccuracy` 의 낱말 나누기에서 어포스트로피(`'`, U+2019)는 띄어쓰기로 보지 않는다. 시험: 영어 라벨 "Queen's Day" ↔ 후보 "Mary's day" 는 불일치. `'` 와 U+2019를 같은 글자로 볼지는 보고서에 적는다. T2의 1~7행·경계 사례 점수는 그대로 |
| T6 | 검색 틀 연결: `EventVideoSource`·`VideoSearch`·`selection`·같은 마감 (`feat(media)`·`feat(api)` 커밋) | backend-dev | T5 | (1) `EventVideoSource`·`SourceResult`·`Status` 가 "부품" 절 모양이다. 이 PR에는 출처 빈이 없고, 그래도 컨텍스트가 뜨며 데모 3날짜 응답이 바뀌지 않는다(기존 `WorldStoryDemoTest` 단언 그대로, 응답 어디에도 `selection` 키 없음). (2) `MediaItem` 에 `selection` 이 있다: 값이 있으면 JSON `selection: {basis, score}`, 없으면 **키가 없다**. 2인자 생성자가 남아 `MediaNormalizer` 와 기존 시험이 고치지 않고 동작. 사진 항목 키 시험(`MediaItemJsonTest`, M3a3d 사건 사진 키 시험 — `MediaRecords.STORED_KEYS` 26 + `date_relation`)이 그대로 통과. 영상 항목은 26 + `date_relation` + `selection` 이고, 둘을 뺀 뒤 `media.schema.json` 통과. (3) **같은 마감**: `CommonsMediaLookup.lookup` 이 출처마다 작업 하나를 사진 두 경로와 **같은 `Deadline`**(기존 try-with-resources 안)에서 시작하고 모두 `await` 한다(리뷰 확인 항목). 4인자 생성자가 남아(영상 없음) `CommonsMediaLookupTest`·`CommonsMediaLookupEventPhotosTest` 가 고치지 않고 통과. (4) 대상: 가짜 출처가 받은 `events` 가 `EventTarget` 앞 3개(응답 순서)이고 각 `EventContext` 의 `title`·`summary`·`wikidataQid`·`placeRef`·`date` 가 사건 값이다. `normal` 사건 4개 → 4번째 ID는 어느 출처에도 없음. `max-events: 0` → 출처 호출 0. 대상이 없으면 출처 호출 0. (5) `tragic`: `tragic` 사건 ID는 어느 출처의 `events` 에도 없다. `tragic` 사건이 있는 날·문서 하나가 `UNAVAILABLE` 인 날은 모든 출처가 `dayPathsOff = true` 를 받는다. 그렇지 않은 날은 `false`. 가짜 출처가 `tragic` 사건 ID로 후보를 돌려줘도 응답의 `tragic` 사건 `media` 는 `[]`(T0). (6) 고르기: 가짜 출처 둘로 — 자유 → 공식 → 그 밖 순서(자유 70점이 공식 130점을 이김), 같은 단계는 점수 순, 같은 점수는 `basis` 순서, 사건마다 영상 1. 같은 `id` 가 두 출처에서 오면 하나(점수 높은 쪽, 같으면 `basis` 순서). 같은 `id` 가 두 사건의 1순위면 앞 사건에만, 뒤 사건은 다음 후보. `Reject` 가 있는 후보는 붙지 않는다. `SourceResult.englishLabels` 의 라벨이 그 출처 후보의 영어 사건어 대조에 쓰인다(라벨 있음 → 붙음 / 없음 → 버림 한 쌍). 응답 영상 항목의 `selection.basis`·`score` 가 고른 후보의 값. (7) 사건 `media` 는 `[사진, 영상]` 순서. 사진 경로가 실패해도 영상은 남고, 영상 출처가 실패해도 사진·`day_media` 는 남는다. (8) 실패 격리: 출처 둘 중 하나가 — `FAILED` → 다른 출처 결과는 남고 `partial: true`, 추가 WARN 없음 / 예외를 던짐 → WARN 정확히 한 줄 `Video search failed: source=<key> events=<n> cause=<예외 클래스>`, `partial: true` / 응답하지 않음 → `lookup-deadline` + `CANCEL_GRACE` + 0.5초 안에 끝나고 WARN 한 줄 `cause=deadline`, `partial: true` / `QUOTA_SKIPPED` → WARN 없음, 다른 실패가 없으면 `partial: false`. 후보 0 → 사진만, `partial: false`. (9) **창 하나**: 날짜 범주·PageImages·영상 출처가 모두 응답하지 않아도 미디어 구간이 `lookup-deadline` + `CANCEL_GRACE` + 0.5초 이하(M3a3d T2 (6)의 미디어 구간 시험과 같은 식), 세계 섹션 최악 시간 시험(`WorldStoryEventPhotoPartsRegressionTest` 의 설정값 상한)이 단언을 바꾸지 않고 통과. (10) `EventTarget` 에 `summary`·`wikidataQid` 가 있고, 3인자 생성자가 남아 기존 시험이 고치지 않고 컴파일된다. `WorldStoryService` 가 두 값을 채운다. (11) 로그(DEBUG 포함)에 날짜·문서 제목·영상 ID·영상 제목·전체 사건 ID가 없다. 버린 후보는 DEBUG에 `Reject` 이름별 개수만. (12) 응답 어디에도 `display_mode: link_only` 가 없다. (13) 기존 테스트 단언은 고치지 않는다. 준비 코드(생성자 인자 등)를 바꿨으면 바꾼 줄을 보고서에 모두 적는다. (14) `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 2 (750 넘으면 알림, 멈추지 않음)** |
| T7 | 완료 기준 검증과 전체 테스트 | test-engineer | T6 | (1) T0~T6 기준 표(기준마다 테스트 이름 — T3 표에 T5·T6을 더함). 빠진 기준은 테스트를 더한다. (2) `./gradlew check` 통과, 3회 반복 안정. (3) S1·S2·S3 보고. (4) "점수 확인" 표 9행 전부의 실제 결과 표(후보, 점수, 붙음/버림, `Reject`, 붙었으면 `captured_at`·`date_relation.kind`·`selection`) — 합성 후보를 가짜 출처로 세계 API에 흘린 결과(MockMvc). (5) 잰 미디어 구간 시간과 세계 섹션 최악 시간을 M3a3d T2 측정과 나란히. (6) 최종 서버 미디어 DTO 필드 목록이 웹 `web/src/media/types.ts` 의 `Media`(선택 필드 `selection` 포함)와 같은지. (7) 컨텍스트 테스트에서 WireMock이 받지 않은 바깥 요청 0(`findAllUnmatchedRequests` 빈 목록) |
| T8 | 실제 서버 회귀 확인 (PR 검수 때) | PM | T7 | (1) `docker compose up -d db`, `cd server && ./gradlew bootRun` 이 뜬다(루트 `.env` 의 `WIKIMEDIA_USER_AGENT` 는 저장소 주소만, ASCII. 키 없음). (2) 아래 P1~P3 결과가 M3a3d 머지 때와 같다: 사건·`day_media`·`partial` 그대로, 응답 어디에도 `selection` 키와 `video` 항목이 없다(어댑터가 없으므로 정상 — 영상은 M3a4b부터). (3) 다른 날짜로 영상을 찾아보지 않는다(PM 피드백 2026-10-16). (4) 서버 로그에 `Video search failed` 줄이 없다 |

완료 기준에 쓰는 명령 (저장소 루트):

```
# S1 — 제품 코드·설정 줄 수
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
# S3 — 데이터 파일 줄 수 (0이 정상)
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P1~P3 — PM 실제 호출 (T8, 서버 실행 중, 키 없음)
curl -s 'http://localhost:8080/api/story/world?date=1988-09-17' \
  | jq '{partial, events: [.events[] | {title, tone, media: [.media[] | {id, type, selection}]}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' \
  | jq '{partial, events: [.events[] | {title, media: [.media[] | {id, type, selection}]}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=1995-03-15' | jq '{events, day_media, notice, partial}'
# selection 키가 하나라도 있으면 출력이 생긴다 (없어야 정상)
curl -s 'http://localhost:8080/api/story/world?date=1988-09-17' | jq '.. | objects | select(has("selection"))'
```

담당별: backend-dev 5 (T0, T1, T2, T5, T6), test-engineer 2 (T3, T7), PM 2 (T4, T8). 모두 9개, 한 PR.

## 데모 입력 3개로 확인할 것

이 기능에는 출처 어댑터가 없으므로 **데모 응답은 M3a3d와 같다**. 영상이 없는 것은 정상이다(M3-P9 (b), M3a3c 결정 6). 기대 영상(1988 대한뉴스 제1715호 등)은 M3a4b·M3a5·M3a5b의 품질 확인에서 본다.

| 데모 입력 | 사건 (M3a2) | 확인 |
|---|---|---|
| 1988-09-17 + "서울" | 3개, 모두 `normal` | 응답이 M3a3d와 같다(T3 (5), T8). 영상·`selection` 없음 |
| 2002-06-22 + "광주 서구 풍암동" | 1개, `normal` | 같음 |
| 1995-03-15 + "서울대학교병원" | 0개, `NO_EVENTS_FOUND` | 같음. 대상이 없어 영상 출처 호출 0(T6 (4)) |
| (합성) 2000-02-29, `tragic` 섞인 날 | 지어낸 `tragic` 사건 하나 + `normal` 하나 | `tragic` 사건 `media` = `[]`(T0), 영상 출처에 `tragic` 사건 없음·`dayPathsOff = true`(T6 (5)) |
| (합성) 1969-07-20·1986-09-20·1988-09-17 점수 확인 | 리서치 7절 후보 | 점수·결과가 표와 같다(T2, T5, T7 (4)) |

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 메모 |
|---|---|---|
| M3a4b Commons 동영상 | `EventVideoSource`, `Candidate`, `EventLink`, `notAfter`, `SourceResult.englishLabels`(Wikidata 영어 라벨), `Basis.COMMONS_*`, `media.video.max-duration-seconds` | `CommonsFileMapper.map` 은 `DateTimeOriginal` 을 안에서 읽는다 → M3a4b가 영상용으로 `notAfter` 를 적용할 자리를 정한다(계획 개정 때). 공용 `Semaphore(3)` 를 사진 경로와 나눠 쓴다(리스크 7) |
| M3a5 YouTube | `official`, `dateTextExcluded`, `Status.QUOTA_SKIPPED`, `TextDates`(제목·설명 날짜 → `captured_at`), `Basis.YOUTUBE_OFFICIAL` | 회고 편집물의 `captured_at` 비우기(R-Y3)는 M3a5 어댑터가 한다(이 기능은 `retrospective` 재료를 두지 않음) |
| M3a5b 대한뉴스 | `explicitDates`, `dateTextExcluded`, `Basis.DAEHAN_NEWS`, 같은 영상 규칙(`daehan_news` 우선) | 색인(N-a)이 "확인됨"이 되어야 시작 |

## 리스크와 확인할 것

1. **엉뚱한 영상** — 이름만 같은 다른 사건, 기념·재현·발사일·예행연습·카운트다운 영상, 노선·회차 번호만 다른 사건. 대응: 직속 범주·`P10` 만, 사건어(한국어·영어·숫자 핵심어와 숫자 경계), 필수 조건, 감점, 기준선, PM 품질 확인, 차단 목록.
2. **경계값 60** — 8행(KTV 1986 개막식)이 딱 60이다. 기준선은 60 그대로 두고(영상 21번) M3a5 T4 품질 확인 뒤 낱말 목록을 보정한다.
3. **핵심어가 하나뿐인 짧은 제목** — "절반 이상"이 1/1로 쉽게 맞는다. 필수 조건과 기준선이 같이 거른다. 품질 확인에서 엉뚱하면 일반어 목록을 늘린다.
4. **조사를 떼지 않음** — "X의 Y" 같은 제목은 "X의"가 후보 글에 없어 사건어가 덜 맞는다. 사진만 나오는 쪽으로 틀린다(안전). 실제로 자주 보이면 후속으로 조사 목록을 정한다.
5. **영어 라벨이 없는 사건** — 한국어만 대조하므로 영어 메타데이터 파일은 사건어 점수를 못 받는다(안전한 쪽).
6. **표본이 작다** — 9개 후보로 맞춘 값이다. M3a4b·M3a5·M3a5b의 PM 품질 확인으로 보정한다(M3-P11).
7. **같은 끝 시각의 자리 다툼** — 미디어 경로가 사진 2 + 영상 출처 N개로 늘어난다. Wikimedia 요청은 공용 `Semaphore(3)` 를 나눠 쓰므로 M3a4b의 Commons 영상 요청이 사진 요청 뒤에 줄 설 수 있다(그러면 끝 시각에 영상만 빔, `partial: true`). M3a4b 계획 개정 때 요청 수와 함께 다시 본다. 세계 섹션 최악 시간은 늘지 않는다(T6 (9)).
8. **`tragic` 오판** — 비극을 `normal` 로 잘못 보면 비극 영상이 붙을 수 있다. M3a2의 보수적 기본값(판정 불가면 `tragic`)과 T0의 두 번째 방어선이 같이 막는다(T0은 `tragic` 으로 판정된 사건만 막는다 — 오판 자체는 못 막음).
9. **대표 링크가 사건이 아닌 줄**(R-V8) — 다른 항목의 범주·라벨로 검색하게 된다. 사건어 필수 조건이 일부 막는다. 후속 후보.
10. **글 속 날짜의 모양** — 표에 없는 모양("1986.09.20", "'86")은 읽지 않는다(날짜 점수 없음 — 안전한 쪽). 품질 확인에서 자주 보이면 모양을 더한다.
11. **어댑터 책임** — 차단·통신사 제외·R1~R9와 출처별 촬영 시점 규칙은 어댑터가 지킨다(`MediaItem` 은 `MediaNormalizer` 만 만든다). 틀은 늦은 촬영 시점만 한 번 더 거른다. 각 어댑터 계획(M3a4b·M3a5·M3a5b)에 그 기준을 둔다.
12. **웹 mock 값** — 웹 `web/src/mocks/fixtures/world/youtube.ts` 의 `selection.score` 가 `0.9`(소수)다. 서버는 정수 합계(예: 90)를 보낸다. 웹 타입은 `number` 라 깨지지 않고 화면에 보이지 않는 값이다 → 작은 웹 `fix/` 후보(다음 단계 제안, 이 PR 밖).
13. **크기** — 약 675(600~765)로 400 규칙을 넘는 한 PR이다(PM 결정 1 (b)). 리뷰할 양이 많다 — 확인점 두 개(알림)로 진행 중 크기를 PM에게 알리고, 커밋을 작업마다 나눠 리뷰하기 쉽게 한다. M3a3d는 파일별 셈의 ×1.33, M3a3c는 ×1.7이었다.

## PM 결정 (2026-10-18, 기록)

| # | 물은 것 | 결정 | 이 계획서에 반영 |
|---|---|---|---|
| 1 | 크기 약 675(600~765) — 400 규칙을 넘는다 | **(b)** 한 PR(`feat/event-video`, T0~T8). 확인점 1: T2 뒤 누적 S1 400 넘으면 알림, 확인점 2: T6 뒤 750 넘으면 알림 — 둘 다 멈추지 않는다. M3 PR 21개 그대로. 고르지 않은 것: (a) 두 PR(정확도 판정 T0~T4 약 345 + 글 속 날짜·검색 틀 연결 T5~T8 약 330, planner 추천이었음), (c) 한 PR에서 글 속 날짜(T5)를 M3a4b로 미룸(약 590) | 머리말 상태, 전제 표, 크기 절, 작업 순서, T2·T6 완료 기준, 리스크 13 |
| 2 | ★ T2 정확도 판정을 PM이 직접 구현할지 | **(b)** backend-dev. 고르지 않은 것: (a) PM 직접 | 전제 표, T2 담당 |
| 3 | (2026-10-23, T3 보고) T2가 `\p{P}` 전부를 띄어쓰기로 봐서 어포스트로피도 낱말을 가른다 — "Queen's Day" ↔ "Mary's day" 가 영어 사건어로 일치(+30) | **(b)** backend-dev가 T5와 함께 어포스트로피(`'`, U+2019)를 띄어쓰기에서 빼고 시험 1개를 더한다. 고르지 않은 것: (a) M3a4b·M3a5 품질 확인에서 봄 | 머리말 상태, T5 완료 기준 (7) |

배우고 싶으면 PR 리뷰 때 `VideoAccuracy` 와 점수 확인 매개변수 테스트를 함께 읽기를 권한다.

## 다음 단계 제안 (이 PR 밖)

- 웹 mock `selection.score: 0.9` 를 정수(예: 90)로 — 작은 웹 `fix/`(리스크 12).
- M3a4b 계획 개정 때: `CommonsFileMapper` 의 영상용 `notAfter` 자리, `Semaphore(3)` 자리 다툼과 요청 수, 이 기능의 머지 이름(`EventVideoSource` 등)으로 다시 대조.
