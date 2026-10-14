# 미디어 공용 규칙: 촬영 시점·라이선스 상태·차단과 제외 (M3a3a)

> 브랜치: `feat/media-rules` · 작성: planner · 상태: **PM 승인 (2026-10-13)** — 이름 M3a3a 유지, D1 **(b)** 낱말 경계는 M3a3 계획서 문구 그대로
> 생긴 이유: M3a3 `feat/event-media` 의 중간 확인점에서 T2 뒤 누적 S1이 **505**로 450을 넘었고, PM이 **(a) T3을 별도 PR로 뺀다**를 골랐다(2026-10-13). 이 계획서는 `docs/plans/m3a3-event-media.md` 의 T3 내용(완료 기준 포함)을 그대로 옮긴 것이다. M3 PR은 18개에서 **19개**가 된다.
> 개정 (2026-10-13, PM 결정): M3a3 코드 리뷰(PR #12) 권장 2개(윤초 `retrieved_at`, 유니코드 공백만 있는 값)를 **T2** 에 더하고, 스키마 쪽은 content-curator 하위 작업 **T2b** 로 새로 넣었다. 기존 작업 ID(T1~T4)는 그대로다.
> 이름: M3a3 → **M3a3a** → M3a3b 순서로 진행한다. 이름도 그 순서로 정렬되게 `a` 를 붙였다(`m3a3c` 는 `m3a3b` 뒤에 정렬되어 진행 순서와 어긋난다).
> 기준: PM 미디어 결정 2026-10-11, 안전장치 1~6(3은 개정판), `CLAUDE.md` 7절 1·5·6항, `docs/product-brief.md` 6.1절·6.2절·6.3절·12절, `docs/plans/m3a3-event-media.md`("촬영 시점 파서", "라이선스 상태", "차단과 제외", R1~R9, "뒤 기능과의 형식 호환"), `docs/research/video-sources.md` 10.1절 4번
> 선행: **M3a3 `feat/event-media` 머지**(`MediaRecord`·`MediaItem`·`MediaNormalizer`·`MediaProperties` 가 `main` 에 있어야 한다). 이 기능은 그 `main` 에서 갈라진다.
> 뒤따르는 기능: **M3a3b `feat/commons-photos`**(파서·라이선스 상태·차단·제외를 쓴다 — 이 기능 머지 뒤), **M3a4 `feat/event-video`**(파서·차단 목록·제외 목록을 쓴다 — 이 기능 머지 뒤). 웹(M3b2)은 이 기능을 기다리지 않는다(아래 "웹과의 관계").

## 작업 번호 옮김

| M3a3 (이전) | 이 계획서 | 내용 |
|---|---|---|
| T3 완료 기준 (2)·(3) | **T1** | 촬영 시점 파서, 라이선스 상태 |
| T3 완료 기준 (4)~(7) | **T2** | 차단 목록·통신사 제외, M3a2 리뷰 2차 권장 1(Wikidata 주석) |
| T3 완료 기준 (1)·(8) | T1·T2 둘 다 | `./gradlew check`, 누적 S1 보고 |
| (없음) | **T3** test-engineer, **T4** PM | 이 기능만의 검증과 PR 검수 |
| (없음, PR #12 리뷰 권장) | **T2** (9)·(10), **T2b** content-curator (새 하위 작업 ID) | 윤초 거절, 유니코드 공백만 있는 값을 빈 값으로 — 서버(T2)와 스키마·예시(T2b) |

M3a3 T3 하나를 두 작업으로 나눈 이유: 서로 다른 부품(문자열 해석 / 설정 기반 필터)이고, 하나에 1~3 커밋이 넘을 크기다. 완료 기준의 내용은 바꾸지 않았다. 더한 것은 필터 입력 형태(아래 "M3a3에서 이어받는 계약")와, PM 결정(2026-10-13)으로 넣은 PR #12 리뷰 권장 2개(아래 "M3a3 리뷰 권장 (PR #12)")다.

## 목표

1. 출처와 무관한 **촬영 시점 파서**: 문자열 하나 → `captured_at`·`date_precision`. 못 읽으면 둘 다 `null`("촬영 시점 미상").
2. **라이선스 상태 판정**: `license` 이름 → `free`/`other`/`unknown`.
3. 출처별 **차단 목록**(`media.blocklist`, 안전장치 5)과 **통신사·언론사 직접 표시 금지**(`media.excluded-credits`, 안전장치 3).
4. M3a2 리뷰 2차 권장 1: Wikidata 시간 상한 주석에 취소 유예(`CANCEL_GRACE`)를 적는다(동작 그대로).
5. 응답은 바뀌지 않는다(`media: []`, `day_media: []`). 이 부품들을 실제로 부르는 것은 M3a3b·M3a4다.
6. M3a3 코드 리뷰(PR #12) 권장 2개: `retrieved_at` 의 윤초(초 = 60)를 서버도 거절하고, 유니코드 공백(`\p{Z}`, 예 U+00A0·U+2007)만 있는 값을 서버와 스키마 모두 빈 값으로 본다. "API가 내보내는 미디어는 늘 스키마를 통과한다"와 R1 "출처 칸이 비면 버림"을 지킨다.

## 범위 밖

- Commons 호출, 사진 고르기, `attribution` 문구 만들기(작가·라이선스 이름 조합) → M3a3b
- 영어 서수("17th September 1988")·한국어 날짜("1988년 9월 17일") 해석, "사건 날짜보다 늦으면 비움"(R-V3) → M3a4·M3a4b
- 비자유 위키 로컬 파일 제외(M3-P7) → M3a3b
- 형식·R1~R9·`date_relation` 바꾸기 → 바꾸지 않는다(M3a3에서 끝남). 예외는 PR #12 리뷰 권장 2개뿐이다 — 필드·규칙은 그대로 두고 "빈 값"과 "UTC 초 단위 시각"의 판정 구멍만 막는다
- 폭 없는 문자(U+200B 폭 없는 공백, U+FEFF 등 `\p{Cf}`)를 빈 값으로 보기 → 하지 않음(PM 결정은 `\p{Z}` 까지, 리스크 7)
- 스키마의 `retrieved_at` 정규식을 초 `[0-5][0-9]` 로 좁히기 → 하지 않음(서버가 더 엄격한 쪽이라 규칙이 지켜진다, 리스크 7)
- 차단 목록을 실행 중에 다시 읽기, 관리 화면 → 하지 않음(서버 재시작으로 적용)
- 한글이 바로 붙은 로마자 통신사 표기(`AP통신`, `EPA연합`) 잡기 → 하지 않음(D1 (b), 리스크 3)
- 프론트엔드

## 사용할 외부 출처

이 기능은 외부를 부르지 않는다. 새 데이터 파일은 T2b의 합성 invalid 예시 1개뿐이다(실제 출처 값 없음). 시험 값은 모두 합성 문자열이다(날짜는 데모·경계값만, `CLAUDE.md` 7절 6항. `retrieved_at` 의 `2026-10-13…` 은 가져온 시각이지 사람의 날짜가 아니며 기존 예시·테스트와 같은 값이다).

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| 미디어 표시·촬영 시점 미상·안전장치 1~6 | `docs/plans/m3a3-event-media.md` 와 같음 | 결정 2026-10-11 |
| M3a6 | M3에서 제외 — `image_embed` 허용 목록은 빈 채로, 통신사 사진은 M3에서 나오지 않음 | 결정 2026-10-11 |
| 영상 4번 (a) | Commons에 자유 라이선스로 올라온 언론사 영상은 허용, 차단 목록으로 대응 | 결정 2026-10-11 |
| M3a3 확인점 | S1 505 > 450 → **(a) T3을 별도 PR로** (M3 PR 19개) | **결정 2026-10-13** |
| D1 | **(b)** 로마자 낱말 경계는 M3a3 계획서 문구 그대로, 처음 제외 목록도 그대로 | **결정 2026-10-13** |
| PR #12 리뷰 권장 | 윤초 거절·유니코드 공백 빈 값 처리를 T2에 더함(스키마 쪽은 T2b) | **결정 2026-10-13** |

## M3a3에서 이어받는 계약 (M3a3 T2 코드, 4d8929c)

| 자리 | 지금 (M3a3) | 이 기능에서 |
|---|---|---|
| `media/MediaRecord` | 저장 형식 레코드(26필드). `LicenseStatus`(`FREE`/`OTHER`/`UNKNOWN`)·`DatePrecision`(`DAY`/`MONTH`/`YEAR`/`APPROX`/`CURRENT`) 열거형이 안에 있다 | 파서는 `DatePrecision`, 라이선스 판정은 `LicenseStatus` 를 그대로 돌려준다(새 열거형을 만들지 않음) |
| `media/MediaNormalizer` | `normalize(MediaRecord, LocalDate)` → `Optional<MediaItem>`. R1~R9, `date_relation` | 차단·제외 때문에는 **고치지 않는다.** 차단·제외는 별도 공개 부품이고, 부르는 쪽(M3a3b·M3a4)이 정규화 **앞에서** 부른다. 고치는 곳은 PR #12 리뷰 권장 두 메서드(`dateTime()`·`has()`)뿐이다(T2 (9)·(10)) |
| `media/MediaProperties` | `@ConfigurationProperties("media")` record, `allowedImageEmbedProviders`·`allowedVideoEmbedProviders`(소문자 제공자 ID 검증, 틀리면 시작 실패) | `blocklist`·`excludedCredits` 를 더한다. 같은 방식으로 검증 |
| `application.yml` `media:` | `allowed-image-embed-providers: []`, `allowed-video-embed-providers: []` | `blocklist: []`, `excluded-credits: [아래 처음 값]` |
| Wikidata 주석 | `SparqlWikidataSignalSource` 클래스 주석, `WikidataProperties` 주석, `application.yml` 의 `wikidata-*` 주석이 "읽기 타임아웃 안에 끝남"이라 적음. 실제는 5초 + 취소 뒤 최대 1초(`CANCEL_GRACE`) | 주석만 고친다 |
| `data/schema/media.schema.json` | `$defs.nonEmptyString` 의 `pattern` 이 `\S`(검증기의 자바 정규식에서 U+00A0 같은 유니코드 공백도 "공백 아님"으로 셈) | `pattern` 만 고친다(T2b). 다른 `$defs`·필드·규칙은 그대로 |

**필터 입력** — `MediaRecord` 에는 원문 작가(`Artist`)·`Credit`·채널 ID·출처 안의 항목 이름이 없다. 그래서 차단·제외 부품은 `MediaRecord` 와 함께 작은 "원문 정보" 형을 받는다(이름은 예시, 보고서에 실제 이름):

| 필드 (예시 이름) | 뜻 | 예 |
|---|---|---|
| `source` | 출처 키(소문자) | `commons`, `youtube` |
| `itemId` | 출처 안의 항목 이름 — Commons는 파일 이름(`File:` 없이), YouTube는 영상 ID | `Example photo.jpg`, `W0isyEBFRvI` |
| `author` | 작가 평문(Commons `Artist` 에서 HTML을 벗긴 값), 없으면 `null` | |
| `credit` | `Credit` 평문, 없으면 `null` | `AP Photo/…` |
| `channel` | 채널 ID, 없으면 `null` | |

부품은 "남김" 또는 "뺌 + 이유"(`blocked_id` / `blocked_author` / `blocked_channel` / `excluded_credit`)를 돌려준다. 예외를 던지지 않는다. 공개 형·메서드라 `media` 패키지 밖(M3a3b의 Commons 어댑터, M3a4의 영상 어댑터)에서 쓸 수 있다.

## 규칙 (M3a3 계획서에서 그대로 옮김)

### 촬영 시점 파서

입력은 문자열 하나(예: Commons `DateTimeOriginal`), 출력은 `captured_at`·`date_precision`. HTML 태그를 먼저 벗긴다(예: `<time datetime="1988-09-17">17 September 1988</time>`). 출처를 모른다(어느 필드에서 왔는지 받지 않는다).

| 원문 모양 | 결과 |
|---|---|
| `1988-09-17`, `1988-09-17 12:34:56`, `1988:09:17 12:34:56` | `1988-09-17`, `day` |
| `17 September 1988`, `September 17, 1988`, `Taken on 17 September 1988`, `… (Exif)` | `day` |
| `September 1988`, `1988-09` | `month` |
| `1988` | `year` |
| `c. 1988`, `circa 1988`, `ca. 1988`, `around 1988` | `approx` |
| 서로 다른 날짜가 둘 이상, 범위, `1980s`, 읽을 수 없음, 빈 값, 없는 날짜(`1988-02-30`) | `null` |

출력은 M3a3 R8을 늘 만족한다(`day` → `YYYY-MM-DD`, `month` → `YYYY-MM`, `year`·`approx` → `YYYY`, `null` → `captured_at` 도 `null`). 파서는 `current` 를 만들지 않는다(그것은 출처가 정한다 — M4b). 표에 없는 모양은 `null` 이다(안전한 쪽).

### 라이선스 상태

`license` 를 앞뒤 공백을 지우고 대소문자 무시로 대조한다. 판 번호·나라 표기가 붙어도 같은 종류면 같다.

| 이름 예 | `license_status` |
|---|---|
| `Public domain`, `CC0`, `CC0 1.0`, `CC BY 4.0`, `CC BY-SA 3.0`, `CC BY-SA 2.0 kr`, `KOGL Type 1` | `free` |
| `CC BY-NC 4.0`, `CC BY-ND 2.0`, `CC BY-NC-SA 3.0`, `KOGL Type 3`, `provider-terms`, 그 밖의 이름 | `other` |
| 없음·빈 값 | `unknown` |

### 차단과 제외 (안전장치 3·5)

| 규칙 | 내용 |
|---|---|
| 차단 목록 | 설정값 `media.blocklist`: 항목마다 `source`(예 `commons`, `youtube`, `getty`)와 `id`(Commons는 파일 이름 — `File:` 없이, 밑줄과 공백은 같게 / 영상 ID / 자산 ID) 또는 `author`(Commons `Artist` 평문, 대소문자 무시 완전 일치) 또는 `channel`(채널 ID). 맞으면 뺀다. 서버 재시작으로 적용. 권리자 요청을 받으면 PM이 설정에 추가한다. `source` 가 비었거나, `id`·`author`·`channel` 이 모두 없는 항목은 시작 실패 |
| 통신사·언론사 직접 표시 금지 | `display_mode` 가 `image`·`video_file` 인 레코드의 `source_name`·`attribution`·원문 `author`·`credit` 에 제외 목록(설정값 `media.excluded-credits`)이 들어 있으면 뺀다. **로마자 항목은 낱말 단위**(대소문자 무시, 앞뒤가 글자·숫자가 아님 — `AP Photo` 는 걸리고 `Japan` 은 안 걸림. D1 (b)). 한글 항목은 부분 문자열(`ⓒ연합뉴스` 걸림). **알려진 한계**: 한글도 글자이므로 로마자 항목 바로 뒤에 한글이 붙은 `AP통신`·`EPA연합` 은 걸리지 않는다(리스크 3). `image_embed`·`video_embed` 에는 적용하지 않는다(R-G7). **M3에서는 `image_embed` 경로가 없어 이 제공사들의 사진이 나오지 않는다.** Commons에 자유 라이선스로 올라온 언론사 영상은 허용(영상 4번 (a)) — 차단 목록으로 대응 |
| 처음 제외 목록 | 연합뉴스, Yonhap, 뉴시스, Newsis, AP, Associated Press, Reuters, AFP, Getty, EPA (M3a3 계획서 그대로) |

## M3a3 리뷰 권장 (PR #12, PM 결정 2026-10-13)

| # | 지금 | 문제 | 고침 |
|---|---|---|---|
| 1 윤초 | `MediaNormalizer.dateTime()` 이 정규식 + `Instant.parse` 로 본다. `Instant.parse` 는 초 60을 받아 주므로 `retrieved_at: "2026-10-13T23:59:60Z"` 가 통과한다 | 스키마(`format: date-time`)는 이 값을 거절한다 → "API가 내보내는 미디어는 늘 스키마를 통과한다"가 깨진다 | `dateTime()` 이 초 = 60을 거절한다. 서버가 스키마보다 엄격한 쪽이면 이 규칙은 지켜진다 |
| 2 유니코드 공백 | 서버 `has()` 는 `String.isBlank()`(자바 `Character.isWhitespace`), 스키마 `nonEmptyString` 은 `pattern: "\\S"`. 둘 다 U+00A0(줄 바꿈 없는 공백)·U+2007(숫자 폭 공백)·U+202F 를 공백으로 보지 않는다 | `attribution`·`id`·`source_name` 이 이런 공백만이어도 서버·스키마 모두 통과 → R1 "출처 칸이 비면 버림"의 구멍. Commons `Artist` 에서 HTML을 벗기면 `&nbsp;` 만 남을 수 있다(M3a3b) | 아래 "공백 문자 집합"의 문자만으로 된 값을 서버와 스키마 모두 빈 값으로 본다. 두 쪽 기준을 똑같이 맞춘다 |

**공백 문자 집합** (서버·스키마 공통): 자바 `Character.isWhitespace` 가 참인 문자(지금 `isBlank()` 의 기준 — U+0009~U+000D, U+001C~U+001F, 줄 바꿈 없는 것을 뺀 유니코드 공백) **또는** 유니코드 범주 `\p{Z}`(U+0020, U+00A0, U+2007, U+202F, U+2028, U+2029, U+3000 등). 값이 이 집합의 문자로만 되어 있으면(빈 문자열 포함) 빈 값이다.

- 서버(T2): `has()` 를 이 기준으로 바꾼다. `has()`·`nullOrHas()` 를 쓰는 모든 칸(`id`·`source_name`·`attribution`·`source_url`·`retrieved_at`·`provider`·`license`·`description`·`render.provider`·`video_sources[].url`)에 같이 적용된다.
- 스키마(T2b): `$defs.nonEmptyString` 의 `pattern` 이 이 집합 **밖의** 문자를 하나 이상 요구한다(예: `[^\s\p{Z}\x1C-\x1F]` — 검증기 `networknt` 는 자바 정규식을 쓰고, 거기서 `\s` 는 U+0009~U+000D·U+0020 이다). `nonEmptyString` 을 쓰는 칸 전부(`id`·`source_name`·`attribution`·`provider`·`license`·`description`·`render.provider`)에 같이 적용된다. 그래서 서버와 스키마가 칸마다 같은 판정을 한다.

## API / 데이터 변경

- `GET /api/story/world`: 바뀌지 않는다(`media`·`day_media` 는 `[]`, 사건 필드는 M3a2·M3a3과 같다).
- 설정 `media.*` 추가: `blocklist: []`(항목 `source` + `id`/`author`/`channel`), `excluded-credits: [위 처음 값]`. 잘못된 값은 시작 실패. `application.yml` 에 차단 목록 항목 예시를 **주석으로** 적는다(PM이 권리자 요청 때 그대로 따라 쓸 수 있게 — 예시 값은 합성, 실제 파일 이름·작가 이름을 쓰지 않음).
- 저장 형식(26필드)과 R1~R9는 그대로. 서버 판정 두 곳(`dateTime()`·`has()`)만 엄격해진다(PR #12 리뷰 권장).
- 데이터 변경(T2b): `data/schema/media.schema.json` 의 `$defs.nonEmptyString` `pattern` 한 줄, 새 invalid 예시 `data/schema/examples/media/invalid/r1-nbsp-only-attribution.json` 1개, `data/schema/examples/media/README.md` 의 표 행·설명.
- 새 환경 변수 없음. DB 변경 없음.

## 웹과의 관계 (M3b2)

M3b2(웹, PM 결정 A로 M3a3 머지 뒤 병렬 시작)는 **이 기능에서 받을 것이 없다.**

- 이 기능은 `MediaItem` 필드를 더하거나 바꾸지 않는다. `license_status`·`captured_at`·`date_precision` 필드와 값 목록은 M3a3에서 이미 정해졌고, 이 기능은 서버 안에서 그 값을 **계산하는 부품**만 만든다.
- M3b2 T1의 대조 기준인 "서버 미디어 DTO 필드 목록"은 M3a3 T4가 낸다(그대로).
- 차단·제외로 빠진 미디어는 응답에 아예 없으므로 웹이 따로 다룰 것이 없다.
- 간접 영향 하나: M3b2의 **PM 화면 확인**은 실제 사진이 필요해 M3a3b 머지를 기다린다(로드맵). M3a3b가 이 기능 뒤로 밀리므로 그 확인도 PR 하나만큼 늦어진다. M3b2의 구현·테스트(MSW)는 영향이 없다.

## 테스트 방침

- M3a1 "테스트 방침"을 따른다. 외부를 부르지 않는다.
- 규칙 표의 행마다 합성 문자열·합성 레코드로 단위 테스트. 날짜는 데모·경계값(1988-09-17, 2002-06-22, 2000-02-29)과 표에 적힌 값만 쓴다(`CLAUDE.md` 7절 6항).
- 차단 목록 시험 값은 합성(`Example photo.jpg` 같은 파일 이름, 지어낸 작가 이름). 실제 작가·채널 ID를 쓰지 않는다.
- 설정 검증은 `ApplicationContextRunner`(또는 같은 수준의 가벼운 컨텍스트)로 시작 실패를 본다.
- PR #12 리뷰 권장: 윤초 시험 값은 `2026-10-13T23:59:60Z`(실제 윤초가 없는 날)만 쓴다. 실제 윤초 날짜(예: 2016-12-31)는 스키마 검증기가 받을 수 있어 "서버가 버림 ⇒ 스키마도 거절" 대조 테스트에 맞지 않는다(리스크 7). 공백 시험 값은 자바 문자열·JSON에서 ` ` 처럼 이스케이프로 적어 눈에 보이게 한다.
- **순서 주의**: `MediaNormalizerTest.brokenRecords` 의 경우들은 `MediaSchemaParityTest.recordsTheServerDropsFailTheSchema` 가 스키마에도 넣어 본다. 그래서 유니코드 공백 경우는 스키마가 고쳐진 **T2b 뒤**에야 `brokenRecords`(또는 다른 대조 테스트)에 들어갈 수 있다. T2는 이 경우들을 `brokenRecords` 가 아닌 별도 테스트 메서드에 두고, 서버·스키마 대조 경우는 T3이 더한다. 윤초 경우는 스키마가 이미 거절하므로 T2에서 바로 `brokenRecords` 에 넣는다.

## 데모 입력 3개로 확인할 것

이 기능은 응답을 바꾸지 않는다. 데모로 보는 것은 "응답이 M3a3과 같다"이다.

| 데모 입력 | 확인 |
|---|---|
| 1988-09-17 + "서울" | 사건 3개(`1988년 하계 올림픽`·`KBS 1TV`·`KBS 2TV`, 모두 `normal`/`curated`), 각 `media` = `[]`, `day_media` = `[]`, `partial: false` |
| 2002-06-22 + "광주 서구 풍암동" | 사건 1개(`2002년 FIFA 월드컵`), `media`·`day_media` = `[]` |
| 1995-03-15 + "서울대학교병원" | `events` = `[]`, `day_media` = `[]`, `notice.code` = `NO_EVENTS_FOUND`, `partial: false` |

파서·라이선스 판정에는 데모 날짜를 문자열로 넣어 본다: `1988:09:17 12:34:56` → `1988-09-17`/`day`, `<time datetime="2002-06-22">22 June 2002</time>` → `2002-06-22`/`day`, `KOGL Type 1` → `free`.

## 크기 (400줄 규칙)

제품 코드(S1) 기준. 셈법은 M3a3 계획서와 같다(이전 방식 ×2.8, 괄호는 ×2.4~×3.7).

| 작업 | 내용 | 이전 방식 | 다시 추정 |
|---|---|---|---|
| T1 | 촬영 시점 파서(HTML 벗기기, 모양 표, 여러 날짜·범위 거르기), 라이선스 상태 판정 | 약 70 | 약 195 (170~260) |
| T2 | 원문 정보 형, 차단·제외 필터, `MediaProperties` 두 설정과 검증, `application.yml`, Wikidata 주석 3곳 | 약 60 | 약 170 (145~220) |
| T2 (9)·(10) | PR #12 리뷰 권장 2개 — `dateTime()` 윤초 거절, `has()` 공백 문자 집합(상수·주석 포함) | 약 10 | 약 30 (20~40) |
| **합계** | | **약 140** | **약 395 (335~520)** |
| T2b (S1 아님) | 스키마 `pattern` 1줄, invalid 예시 1개(약 28줄), README 2~4줄 | — | S3 약 35 |

- **M3a3 T2 실측으로 다시 보기**: M3a3 T2는 이전 방식 약 130 → 실제 505(약 3.9배)였다. 같은 비율이면 이 기능은 **약 545**다(리뷰 권장 2개를 더하기 전 약 505). M3a3 T2에는 26필드 레코드·복사 메서드 같은 형 선언이 많았고 이 기능은 로직이 많아 그보다 작을 것으로 보지만, 400을 넘을 수 있다.
- 더 나누지 않는다: 나누면 PR 하나가 200줄 안팎이 되고, M3a3b·M3a4가 두 PR을 모두 기다린다. PM 결정 (a)도 "T3 하나를 한 PR로"였다. 리뷰 권장 2개(약 20~40)도 같은 PR에 넣는다(PM 결정 2026-10-13).
- **중간 확인점: T1 뒤 누적 S1이 300을 넘으면** 오케스트레이터가 PM에게 **알리고 계속한다**(멈추지 않음). PM이 멈추라고 하면 멈춘다.
- 테스트(S2)와 데이터(S3, T2b만)는 따로 보고한다.

## 작업 목록

순서: T1 → **확인점(알림만)** → T2 → T2b → T3 → T4. T1과 T2는 같은 `media` 패키지에 새 파일을 더하므로 worktree로 나누지 않고 차례로 한다(T2만 `MediaProperties`·`application.yml` 을 고친다). T2b는 `data/` 만 고치지만, T2의 `has()` 변경이 있어야 새 invalid 예시를 서버도 버려 `./gradlew check` 가 통과하므로 T2 뒤에 차례로 한다.

**T2b를 따로 둔 이유 (담당 판단)**: `data/` 는 content-curator 담당이고, 스키마 예시 README의 형식(표 행, 오류 위치, 확인 날짜 문단)도 content-curator가 M3a3 T1에서 만들었다. 고칠 양은 작지만 담당을 섞지 않는다. 순서는 서버가 먼저 엄격해지고(T2 — 서버가 더 엄격한 동안에도 대조 테스트는 통과) 스키마가 뒤따라 같아지는(T2b) 쪽으로 해서, 어느 커밋에서도 `./gradlew check` 가 깨지지 않게 한다. 반대 순서(스키마 먼저)면 새 invalid 예시를 서버가 아직 받아 `MediaSchemaParityTest.invalidExamplesFailBoth` 가 깨진다.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 촬영 시점 파서, 라이선스 상태 판정 | backend-dev | `media` 패키지의 파서·라이선스 판정(공개 형·메서드, 이름은 예시 — 보고서에 실제 이름과 시그니처), 테스트 | M3a3 머지 | (1) `cd server && ./gradlew check` 성공. (2) "촬영 시점 파서" 표의 **행마다** 테스트: `1988-09-17`·`1988-09-17 12:34:56`·`1988:09:17 12:34:56` → `1988-09-17`/`day`, `17 September 1988`·`September 17, 1988`·`Taken on 17 September 1988`·`17 September 1988 (Exif)` → `day`, `September 1988`·`1988-09` → `month`, `1988` → `year`, `c. 1988`·`circa 1988`·`ca. 1988`·`around 1988` → `approx`, `<time datetime="1988-09-17">17 September 1988</time>` → `1988-09-17`/`day`, 서로 다른 날짜 둘(`1988-09-17 / 1988-09-18`)·범위(`1988-1990`)·`1980s`·읽을 수 없음·빈 값·`null`·없는 날짜 `1988-02-30` → 둘 다 `null`. 경계값 `2000-02-29` → `day`. (3) 파서는 출처와 무관한 문자열 하나만 받는다(출처·필드 이름 인자 없음). 파서가 돌려주는 모든 결과를 `captured_at`·`date_precision` 으로 넣은 레코드가 `MediaNormalizer` 의 R8을 통과한다(위 테스트 값 전부). (4) "라이선스 상태" 표의 이름 예 **전부**: `Public domain`·`CC0`·`CC0 1.0`·`CC BY 4.0`·`CC BY-SA 3.0`·`CC BY-SA 2.0 kr`·`KOGL Type 1` → `free`, `CC BY-NC 4.0`·`CC BY-ND 2.0`·`CC BY-NC-SA 3.0`·`KOGL Type 3`·`provider-terms`·지어낸 이름 → `other`, `null`·빈 값·공백만 → `unknown`. 앞뒤 공백·대소문자 차이(` cc by-sa 4.0 `)도 같은 결과. 돌려주는 형은 `MediaRecord.LicenseStatus`. (5) 보고서에 T1 S1과 누적 S1 — **확인점 300(알림만)** |
| T2 | 차단 목록·통신사 제외 필터, 설정, M3a2 리뷰 2차 권장 1, **M3a3 리뷰(PR #12) 권장 2개의 서버 쪽** | backend-dev | 원문 정보 형, 차단·제외 필터(공개, 예외를 던지지 않음), `MediaProperties` 에 `media.blocklist`·`media.excluded-credits`(검증 포함), `application.yml`(처음 값 + 차단 항목 주석 예시), Wikidata 주석 3곳 고침, `MediaNormalizer` 의 `dateTime()`·`has()`, 테스트 | T1 | (1) `./gradlew check` 성공. (2) **차단 목록**(합성 레코드): `source: commons` + `id: Example photo.jpg` 이면 원문 `itemId` `Example_photo.jpg` 도 빠진다(밑줄·공백 차이 무시). `source: commons` + `author: Example Author` 이면 `example author` 도 빠지고 `Example Author 2` 는 남는다(대소문자 무시 완전 일치). `source: youtube` + `id: <영상 ID>` / `channel: <채널 ID>` 로 빠진다(합성 ID). `source` 가 다르면 남는다. 이유가 `blocked_id`·`blocked_author`·`blocked_channel` 로 구분된다. (3) **설정 검증**: 차단 항목의 `id`·`author`·`channel` 이 모두 없음 → 컨텍스트 시작 실패, `source` 가 비었음 → 시작 실패, 제외 목록에 빈 문자열 항목 → 시작 실패. 기본 설정(`blocklist: []`)으로는 시작한다. (4) **통신사 제외**: `credit` 이 `AP Photo/…` 인 `image` → 빠짐(`excluded_credit`), `Japan …`·`Apple` → 남음, `ⓒ연합뉴스` → 빠짐, `source_name`·`attribution`·`author` 어느 자리에 있어도 걸린다, `video_file` 에도 적용, 같은 제공사 표기라도 `image_embed`·`video_embed` 레코드는 남는다(합성 — M3 기본 설정에서는 `image_embed` 가 생기지 않음). 처음 제외 목록이 `application.yml` 에 이 계획서 "처음 제외 목록" 그대로 있다. (5) 차단·제외 필터는 `MediaNormalizer` 를 부르거나 고치지 않는다. `MediaNormalizer` 의 변경은 (9)·(10)의 `dateTime()`·`has()` 두 메서드뿐이다. M3a3 테스트(`MediaNormalizerTest`·`MediaSchemaParityTest`·`MediaSchemaTest` 등)는 기존 경우를 고치거나 지우지 않고(더하기만) 통과한다. (6) 로그: 필터는 파일 이름·작가·`credit` 원문·채널 ID를 어떤 수준으로도 찍지 않는다. DEBUG에는 이유 이름(묶음으로 부르면 이유별 개수)만. (7) **M3a2 리뷰 2차 권장 1**: `SparqlWikidataSignalSource` 클래스 주석, `WikidataProperties` 주석, `application.yml` 의 `wikidata-*` 주석이 "`wikidata-read-timeout` 안에 끝남"이 아니라 "읽기 타임아웃 + 취소 뒤 최대 1초 유예(`CANCEL_GRACE`)"를 적는다 — **C4 출력 없음**, 동작·테스트는 그대로(Wikidata 테스트 파일 변경 없음). (8) 보고서에 누적 S1, 공개 형·메서드 목록(M3a3b·M3a4가 부를 이름과 시그니처), (9)·(10)만의 S1(계획 약 20~40). (9) **PR #12 권장 1 — 윤초**: `dateTime()` 이 초 = 60을 거절한다. `MediaNormalizerTest.brokenRecords` 에 `retrieved_at: "2026-10-13T23:59:60Z"` → 버림(R1) 경우를 더하고, 그 경우로 `MediaSchemaParityTest.recordsTheServerDropsFailTheSchema` 도 통과한다(스키마도 거절). `2026-10-13T23:59:59Z` 는 통과한다. `MediaSchemaParityTest` 클래스 주석의 "의도한 차이"에 "서버는 초 60을 모두 거절한다(스키마 검증기는 실제 윤초 날짜를 받을 수 있다 — 서버가 더 엄격하므로 서버 출력은 늘 스키마를 통과)"를 한 줄 더한다. (10) **PR #12 권장 2 — 유니코드 공백**: `has()` 가 "공백 문자 집합"(이 계획서 "M3a3 리뷰 권장" 절)의 문자로만 된 값을 빈 값으로 본다. `MediaNormalizerTest` 의 **별도 테스트 메서드**(`brokenRecords` 아님 — "테스트 방침"의 순서 주의)에서: `attribution`·`id`·`source_name` 각각이 `" "`·`" "`·`" "`·`"　"`·`"  "` 이면 버림(R1), 선택 칸 `license`·`description` 이 `" "` 이면 버림(빈 문자열과 같은 취급), `attribution` 이 `" 작가"`·`"Example Author"` 이면 남음. 기존 `"  "`·`""` 경우는 그대로 버림 |
| T2b | 스키마의 빈 값 기준을 서버와 맞춤 (PR #12 권장 2의 데이터 쪽, **새 하위 작업 ID**) | content-curator | `data/schema/media.schema.json` 의 `$defs.nonEmptyString` `pattern`, invalid 예시 1개, `data/schema/examples/media/README.md` | T2 | (1) `nonEmptyString` 의 `pattern` 이 "공백 문자 집합" **밖의** 문자를 하나 이상 요구한다(예 `[^\s\p{Z}\x1C-\x1F]` — 실제 값은 보고서에). `minLength: 1`·다른 `$defs`·필드·규칙은 바꾸지 않는다. (2) 새 invalid 예시 `invalid/r1-nbsp-only-attribution.json`: `r1-blank-attribution.json` 과 같고 `id`(`example-invalid-r1-nbsp-only-attribution`)와 `attribution: "  "`(JSON 이스케이프로 적어 눈에 보이게)만 다르다. 날짜·주소는 기존 합성 값 그대로. (3) README `invalid/` 표에 그 파일 행 — Breaks `R1 (empty source field → not shown)`, Change 는 no-break spaces only (U+00A0, U+2007), Error points at `` `/attribution` ``. README "Schema notes" 에 빈 값 기준(공백 문자 집합, 서버 `MediaNormalizer` 와 같음) 한 항목. 마지막 "Checked on …" 문단을 이 확인으로 갱신. (4) `cd server && ./gradlew check` 성공 — 특히 `MediaSchemaTest`(README가 모든 예시 파일을 적음, 새 파일의 오류 위치가 `/attribution` 과 그 상위뿐)와 `MediaSchemaParityTest`(새 파일을 스키마·서버 둘 다 거절, `valid/` 예시 전부와 `syntheticOutputPassesTheSchema` 가 여전히 통과). (5) `server/` 변경 없음. 보고서에 S3 |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트(테스트 파일만) | T2b | (1) T1·T2·T2b 완료 기준을 표로 확인(기준마다 테스트 이름)하고 빠진 테스트를 추가. (2) "데모" 표 3행: 세계 API 응답이 M3a3과 같다(`media`·`day_media` 가 `[]`, 사건 필드 그대로). (3) 파서 출력 전부가 R8을 통과하는지 표 단위로 한 번 더(파서와 정규화의 대조). (4) 로그 확인: 차단·제외 시험을 DEBUG로 돌린 출력에 시험에 쓴 파일 이름·작가 이름·`credit` 원문이 없다. (5) C4 출력 없음. (6) `cd server && ./gradlew check` 통과(개수 보고), 3회 반복 안정. (7) S1·S2·S3 보고(S3는 T2b만, 약 35가 정상). (8) **공개 부품 목록**(형·메서드 이름, 입력·출력, 예외 없음 여부) — M3a3b T1·M3a4 T2가 이 이름을 쓴다. (9) **PR #12 권장 대조 경우**(`MediaSchemaParityTest` 또는 `brokenRecords` 에 더함, 방법은 자유): U+0020·U+0009·U+000A·U+001F·U+00A0·U+2007·U+202F·U+2028·U+3000 각 문자만으로 된 `attribution`·`id`·`source_name` → 서버가 버리고 스키마도 거절, `" 작가"` 인 `attribution` → 서버가 남기고 스키마도 통과. 윤초 `2026-10-13T23:59:60Z` → 둘 다 거절 |
| T4 | 규칙 검토와 실행 확인 (PR 검수 때) | PM | - | T3 | (1) `application.yml` 의 `media.excluded-credits` 처음 값과 차단 목록 주석 예시를 읽고, 권리자 요청을 받았을 때 그 예시대로 항목을 더할 수 있다(어렵거나 빠진 제공사가 있으면 PR 코멘트). (2) `docker compose up -d db`, `cd server && ./gradlew bootRun` 이 기본 설정으로 뜨고 P1 결과가 "데모" 표와 같다. (3) P2: 잘못된 차단 항목으로 띄우면 시작이 실패하고 오류 메시지에 `media.blocklist` 가 보인다. (4) T3 (8) 공개 부품 목록이 이 계획서 "필터 입력"·규칙 표와 어긋나지 않는다 |

완료 기준에 쓰는 명령 (저장소 루트):

```
# C4 — Wikidata 주석이 취소 유예를 빼고 "읽기 타임아웃 안에 끝남"이라 적지 않는지 (T2). 출력 없음
grep -rnE "retries included, ends within|and the whole lookup of one request|each attempt and the whole lookup end within" server/src/main

# S1 — 제품 코드·설정 줄 수
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
# S3 — 데이터 파일 줄 수 (T2b만, 약 35가 정상)
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P1 — PM 실행 확인 (서버 실행 중). 응답이 M3a3과 같은지
for d in 1988-09-17 2002-06-22 1995-03-15; do
  curl -s "http://localhost:8080/api/story/world?date=$d" \
    | jq '{date, partial, notice, day_media, events: [.events[] | {title, tone, media}]}'; done

# P2 — PM 실행 확인 (서버를 끈 뒤). 시작이 실패하고 메시지에 media.blocklist 가 보여야 한다
cd server && ./gradlew bootRun --args='--media.blocklist[0].source=commons'
```

담당별: backend-dev 2 (T1, T2), content-curator 1 (T2b), test-engineer 1 (T3), PM 1 (T4). 모두 5개.

## 리스크와 확인할 것

1. **크기** — 계획값 약 395(리뷰 권장 2개 약 30 포함), M3a3 T2 실측 비율로는 약 545. 400을 넘어도 한 PR로 간다(PM 결정 (a)). T1 뒤 300에서 알린다.
2. **통신사 표기 판별은 문자열 비교다** — 다른 표기(예: 약어 없이 사진가 이름만)로 들어간 통신사 사진은 걸러지지 않을 수 있다. M3a3b 데모 표에서 사람이 본다. 발견되면 제외 목록이나 차단 목록에 더한다.
3. **알려진 한계: `AP통신`·`EPA연합` 은 걸리지 않는다 (D1 (b), PM 수용 2026-10-13)** — 로마자 낱말 경계가 "앞뒤가 글자·숫자가 아님"이고 한글도 글자이므로, 로마자 항목 바로 뒤에 한글이 붙은 표기(`AP통신`, `EPA연합`, `AFP통신`)는 제외되지 않는다. 한글 표기 `로이터`·`게티이미지` 도 처음 목록에 없다. 대응: M3에서는 Commons 사진이 주 경로라 `Credit` 이 대개 로마자이고, 발견되면 PM이 차단 목록(파일 이름·작가)으로 바로 내린다. 자주 보이면 경계 기준이나 처음 목록을 다시 정한다(PM 결정).
4. **파서 범위** — 한국어 날짜·영어 서수는 표에 없어 "촬영 시점 미상"이 된다(안전한 쪽). M3a4 T2가 보강한다. Commons `DateTimeOriginal` 의 다른 모양이 많으면 M3a3b 데모 표에서 `unknown` 이 많이 보인다 — 그때 표를 넓히는 것을 제안한다.
5. **차단 목록은 재시작해야 적용된다** — 권리자 요청 뒤 내리기까지 서버 재시작이 필요하다. 로컬 데모라 충분하다. 공개 배포(#5) 때 다시 본다(안전장치 6).
6. **M3a4가 이 기능을 기다린다** — M3a4는 파서(영어 서수 보강)·차단 목록·제외 목록을 쓰므로 선행이 M3a3에서 M3a3a로 바뀐다. 서버 줄이 PR 하나만큼 길어진다.
7. **PR #12 리뷰 권장 뒤에도 남는 차이 (의도한 것)** — (a) 윤초: 스키마 검증기는 실제 윤초 날짜(예 `2016-12-31T23:59:60Z`)를 받을 수 있고 서버는 초 60을 모두 버린다. 서버가 더 엄격한 쪽이라 "API 출력은 늘 스키마 통과"는 지켜진다. `retrieved_at` 은 우리가 가져온 시각이라 실제로 60초가 나올 일은 드물다. (b) 폭 없는 문자(U+200B, U+FEFF 등 `\p{Cf}`)만 있는 값은 서버·스키마 모두 여전히 "비지 않음"으로 본다(두 쪽은 같은 판정). Commons `Artist` 에서 이런 값이 보이면 M3a3b에서 다시 정한다. (c) 스키마 정규식은 JSON Schema가 권하는 ECMA-262가 아니라 우리 검증기(자바 정규식) 기준으로 확인한다 — 다른 검증기로 바꿀 일이 생기면 `nonEmptyString` 을 다시 본다.

★ PM 직접 구현: 없음 — 로드맵 "★ PM 직접 구현 후보"에 따라 M3에는 넣지 않는다.

## PM 결정 (기록)

| # | 결정 | 상태 |
|---|---|---|
| M3a3 확인점 | S1 505 > 450 → (a) T3을 별도 PR(이 기능)로, M3 PR 19개 | **결정 2026-10-13** |
| 계획 승인 | 이 계획서 승인, 이름 M3a3a(`feat/media-rules`) 유지 | **결정 2026-10-13** |
| D1 | 통신사 제외의 로마자 낱말 경계 — **(b)** M3a3 계획서 문구 그대로("앞뒤가 글자·숫자가 아님"), 처음 제외 목록도 그대로(`로이터`·`게티이미지` 를 더하지 않음). `AP통신`·`EPA연합` 이 걸리지 않는 것을 알고 수용(리스크 3). planner 추천은 (a)였다 | **결정 2026-10-13** |
| PR #12 리뷰 권장 | M3a3 코드 리뷰(PR #12) 권장 2개 — (1) `retrieved_at` 윤초(초 = 60) 서버 거절, (2) 유니코드 공백(`\p{Z}`)만 있는 값을 서버·스키마 모두 빈 값으로, 두 쪽 기준 일치 — 를 **T2** 에 더한다. 스키마·예시·README는 planner 판단으로 content-curator 하위 작업 **T2b**(새 ID, T2 뒤)로 둔다. 기존 ID T1~T4 유지. S1 약 +30(20~40), S3 약 35 | **결정 2026-10-13** |
