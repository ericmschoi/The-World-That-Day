# Commons 동영상 자동 검색과 결과 캐시 (M3a4b)

> 브랜치: `feat/commons-video` · 작성: planner · 상태: **PM 승인 (2026-10-12)** (M3a4 분할로 새로 생김, 로드맵 D1 (a))
> 기준: `docs/research/video-sources.md` 1절 기능 표, 3절 Commons 카드·3.1절·종합 경로 표·하루 처리량, 4절 Commons 열, 5절 V1 표, 6.1절, 7절 최종 추천·V1 추천·R-V1~R-V7, 8절 위험 2·4·5·6, 10.1절 1·2·3·4·21번, `docs/plans/m3a4-event-video.md`(검색 틀·정확도 규칙), `docs/plans/m3a3-event-media.md`(형식, R1~R9), `CLAUDE.md` 7절 1·2·3·5·6항
> 선행: M3a4 `feat/event-video` 머지. **키가 필요 없다**(PM 키 대기 없음). 리서치 6.0절 0번(Commons 핫링크 `curl` 2개)은 선택이고 이 기능을 막지 않는다.
> 뒤따르는 기능: M3a5(YouTube)가 이 기능의 결과 캐시를 쓴다.

## 목표

1. M3a4 검색 틀에 첫 어댑터 **Commons 동영상**(키 없음)을 붙인다. 경로 순서는 리서치 7절 최종 추천대로 **(a-3) 사건 QID `P10` → (a-1) 사건 범주 직속 → (c-1) `Videos taken on YYYY-MM-DD` 직속**이다. 재생은 `upload.wikimedia.org` 원본·변환본 핫링크(`video_file`)다.
2. 사건 QID의 `P10`·`P373`·`P910` 사이트링크·영어 라벨을 Wikidata에서 한 번에 받아 `EventContext.english_label` 을 채운다(정확도 규칙 R-T2).
3. 출처 공통 **결과 캐시**(Flyway V3)를 만든다. 출처별 TTL, 0건도 캐시.
4. 확인용 기준표를 만든다(M3a5·M3a5b 품질 확인에도 같은 표).
5. (크기가 되면) 보조 경로 (b-2) 영어 라벨 검색어와 (b-3) 날짜 문자열을 붙인다. 정확도 규칙을 모두 통과할 때만 붙는다.

## 범위 밖

- `deepcat:` (a-2)·(c-2) — 10.1절 1번 (a)
- 핫링크가 막혔을 때 `link_only` 로 두기 — 레코드를 만들지 않고 보고한다(4절 결론)
- YouTube·대한뉴스 → M3a5·M3a5b
- 파일 내려받기·서버 경유 재생 (안전장치 1)

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| Wikimedia Commons Action API | **확인됨** (video-sources.md 3절 Commons 카드, 1절 표) | `generator=search` + `prop=imageinfo`(원본 주소, `thumburl`, 크기, 길이, `extmetadata`), `prop=videoinfo&viprop=derivatives`(변환본) | 키 없음. User-Agent `WIKIMEDIA_USER_AGENT`(M3a1 공통). 분당 200·동시 3(M3a1b 제한기 재사용). 썸네일은 API가 준 `thumburl` 만(표준이 아닌 크기를 직접 만들면 429, 위험 4). 파일은 복사하지 않고 핫링크. 메타데이터·점수는 캐시 가능 |
| Wikidata Query Service | **확인됨** (day-events.md, M3a2 클라이언트) | 사건 QID → `P10`, `P373`, `P910` 항목의 commonswiki 사이트링크, 영어 라벨(`rdfs:label@en`) | 질의 60초, IP당 동시 5, 429 처리. 날짜 하나의 `normal` 사건 QID(최대 3개)를 질의 1회로 묶는다 |

메모: 리서치는 `P10` 을 M3a2 SPARQL에 속성으로 더하면 추가 요청이 0이라고 적었다. M3a2 계획(승인)을 바꾸지 않으려고 이 기능이 질의를 따로 1회 한다. M3a2 구현이 먼저 끝나 같은 질의에 더하는 것이 간단하면 backend-dev가 그렇게 하고 보고한다.

## 경로와 요청

| 순서 | 경로 | 요청 | `event_link` / `basis` | 규칙 |
|---|---|---|---|---|
| 1 | (a-3) 사건 QID `P10` | Wikidata 질의(날짜당 1, 사건끼리 묶음) + 파일 정보 `prop=imageinfo&titles=…`(묶음 1) | `p10` / `commons_p10` | 사건 연결 +50 |
| 2 | (a-1) 범주 직속 | `generator=search`, `gsrsearch=incategory:"<범주>" filetype:video`, `gsrnamespace=6` + `imageinfo` (사건당 1). 범주는 `P373`, 없으면 `P910` 항목의 commonswiki 사이트링크(R-V2) | `category_direct` / `commons_category` | 직속만. 사건 연결 +50 |
| 3 | (c-1) 날짜 범주 직속 | `incategory:"Videos taken on YYYY-MM-DD" filetype:video` (날짜당 1, 사건끼리 공유) | `none` / `commons_date_category` | 사건어 일치 필수(M3a4 필수 조건 2) |
| 4 (T4) | (b-2) 영어 라벨 검색어 | `"<영어 라벨>" filetype:video` (사건당 1) | `none` / `commons_search` | 같은 날 날짜 일치 + 사건어 둘 다 필수 |
| 5 (T4) | (b-3) 날짜 문자열 | `insource:"YYYY-MM-DD" filetype:video` (날짜당 1) | `none` / `commons_search` | 같음. `insource:` 일치는 날짜 일치가 아님(R-V4) |
| 끝 | 변환본 | 사건마다 고른 파일(최대 1)의 `prop=videoinfo&viprop=derivatives` (묶음 1) | - | `video_sources` 를 만든다 |

새 날짜 하나당 요청: Wikidata 1 + Commons 최대 1(P10 파일 정보) + 3(범주, 사건 3개) + 1(날짜 범주) + 1(변환본) = 최대 7. 보조 경로를 더하면 최대 11. 하루 한도는 없고 분당 200이라 제약이 아니다(3절 하루 처리량). `tragic` 사건은 요청 0, `tragic` 사건이 있는 날은 날짜 단위 경로 요청 0.

## 미디어 레코드 (리서치 4절 Commons 열)

| 필드 | 값 |
|---|---|
| `id` | `commons-` + `pageid` |
| `type` / `display_mode` | `video` / `video_file` |
| `url` | `imageinfo.url`(원본) |
| `thumbnail_url`, `thumbnail_width`·`thumbnail_height` | `thumburl`(`iiurlwidth` 는 표준 폭, 예 330), `thumbwidth`·`thumbheight` |
| `video_sources` | `derivatives[]` → `{url, mime, height}`. `height` 작은 것부터, `video/webm` 을 앞에, `video/quicktime` 을 뒤에, 원본은 마지막 |
| `duration_seconds` | `duration` (M3a4 길이 상한 600초) |
| `embed_url`·`provider`·`render`·`link_only_reason` | `null` |
| `source_name` / `source_url` | `Wikimedia Commons` / `descriptionurl` |
| `license`·`license_url`·`license_status` | `extmetadata.LicenseShortName`·`LicenseUrl`, 상태는 M3a3 허용 목록 대조 |
| `attribution` | M3a3과 같음(작가 HTML 제거, 없으면 `Credit`, 둘 다 없으면 "작가 정보 없음" + 라이선스 + `Wikimedia Commons`) |
| `description` | `extmetadata.ImageDescription` (HTML 제거) |
| `captured_at`·`date_precision` | `DateTimeOriginal` 을 M3a3 파서로. **사건 날짜보다 늦으면 `null`**(R-V3). `extmetadata.DateTime`(업로드 시각)은 쓰지 않는다 |
| 위치 | `place_ref`(사건 `scope` 기준). 좌표가 있으면 사용 |
| `retrieved_at` | 조회 시각 |

- 자유 라이선스로 올라온 언론사 영상(예: 뉴스타파)은 허용, 권리자 요청 시 차단 목록(10.1절 4번 (a)).
- 핫링크 재생이 막힌 것이 확인되면(6.1절 명령이 403·429) `link_only` 를 만들지 않고 PM에게 보고한다.

## 결과 캐시 (Flyway V3)

- 테이블 `event_media_search_cache`: 날짜, 사건 ID, 출처(`commons`, 뒤에 `youtube`, `daehan_news`), 결과(정규화한 미디어 레코드 JSON + 점수 + `basis` — **메타데이터만, 파일 아님**), 조회 시각, 만료 시각. Wikidata 조회 결과(범주·라벨)도 같은 줄에 둔다.
- TTL은 출처별 설정 `media.search-cache.ttl.<출처>`: `commons` 7일. YouTube는 M3a5가 30일 이하로 검증한다(III.E.4.d).
- **0건도 캐시**한다. **실패·시간 초과·쿼터 건너뜀은 캐시하지 않는다**(다음 요청에서 다시 찾는다).
- 요청한 사람 정보는 저장하지 않는다(날짜·사건·출처·결과·시각뿐, `CLAUDE.md` 7절 6항).
- Flyway 번호는 V3(V2는 M3a1b `wiki_section_cache`). 그사이 다른 기능이 V3를 쓰면 다음 빈 번호를 쓰고 보고한다.

## 확인용 기준표 (`data/verification/event-media-expectations.json`, content-curator)

데모 3개와 표본 5개 날짜(1950-06-25, 1969-07-20, 1986-09-20, 2000-02-29, 2014-04-16 — M3-P13)마다 사건별·출처별(Commons / 공식 YouTube / 대한뉴스) "있어야 할 것 / 없어야 할 것"과 근거 URL. 근거는 리서치 5절 종합 표·V1·V2·V5 표와 3.5절 Y2 표다. 예: "1988-09-17 올림픽: Commons 없음 / YouTube·대한뉴스 제1715호 `approx`", "1969-07-20: Commons 있음(`P10`), 발사일 영상·기념 영상은 없어야 함", "2000-02-29: 2·3·4호선 개통 회차는 없어야 함", "2014-04-16: `tragic`, 영상 없음", "1995-03-15: 사건 없음". 응답에는 쓰지 않는다.

## 테스트 방침

- M3a1 "테스트 방침". Wikimedia 응답은 **녹화 fixture**(복사 허용 출처, `CLAUDE.md` 7절 5항 — 녹화 일자·원 URL·라이선스를 `README.md` 에)와 합성 fixture(경계 사례). 데모 날짜에 0건이면 0건 응답을 녹화한다.
- 자동 테스트는 규칙과 경로만 본다. 실제 품질은 PM 품질 확인(T6).
- 표본 날짜는 공개 역사 사건 날짜·경계값이다(`CLAUDE.md` 7절 6항).

## 데모·표본 날짜로 확인할 것

| 날짜 | 자동 테스트 (fixture) | PM 품질 확인 (T6) |
|---|---|---|
| 1988-09-17 | 범주 `1988 Summer Olympics` 직속 동영상 0건, 날짜 범주 없음(녹화) → 올림픽 사건 `media` 에 사진만, `partial: false` | 기준표와 대조 (Commons 없음이 정상) |
| 1969-07-20 | `P10` 에 `AP11 FINAL APPROACH.ogv` → `video_file`, `basis` `commons_p10`, `captured_at` 1969-07-20, `video_sources` 작은 해상도 먼저. `P10` 을 뺀 합성 응답 → 범주 직속 후보 중 점수 최고, 발사일 영상은 안 붙음 | 결과 표 |
| 1986-09-20 | `P373` 없음 → `P910` 사이트링크 범주로 요청(요청 경로 확인), 0건 → 사진만 | 결과 표 |
| 2002-06-22, 2000-02-29 | 0건 → 사진만, 오류 아님 | 기준표와 대조 |
| 1995-03-15 | 사건 없음 → Wikidata·Commons 요청 0 | 같음 |
| 1950-06-25, 2014-04-16 | `tragic` → Wikidata·Commons 요청 0(날짜 범주 포함) | 같음 |
| 캐시 | 같은 날짜 두 번째 요청 → Wikidata·Commons 요청 0, TTL이 지나면 다시. 0건도 캐시. Commons 503·시간 초과 → 캐시 안 됨, 다음 요청에서 다시 부름 | - |

## 크기 (400줄 규칙)

예상: Wikidata 조회 약 35, Commons 어댑터(a-3·a-1·c-1, 변환본, 레코드) 약 110, 결과 캐시(V3·엔티티·흐름·출처별 TTL) 약 65, 설정 약 10 → **약 220줄, 보정 약 350줄**. T4(보조 경로) 약 25 → 합계 약 245, 보정 약 390. **T3 뒤 누적(S1)이 230줄을 넘으면** 오케스트레이터가 PM에게 T4를 후속으로 뺄지 묻는다.

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 확인용 기준표 | content-curator | `data/verification/event-media-expectations.json` | 리서치 머지 | (1) 데모 3 + 표본 5 날짜마다 사건별·출처별 "있어야 할 것/없어야 할 것"과 근거 URL. (2) `tragic` 날짜는 "영상 없음". (3) Commons 열은 리서치 5절 V1 표, YouTube 열은 3.5절 Y2 표, 대한뉴스 열은 5절 V5 표와 맞는다. (4) C1 `ok` |
| T2 | Wikidata 조회 + Commons 어댑터 (a-3·a-1·c-1, 변환본, 레코드) | backend-dev | 어댑터, Wikidata 질의, 녹화·합성 fixture, 테스트 | M3a4 머지 | (1) `./gradlew check` 성공. (2) 위 표의 자동 테스트 행(캐시 행 제외) 통과. (3) Wikidata 질의는 날짜당 1회, `tragic`·사건 없음이면 0회. (4) `english_label` 이 `EventContext` 에 들어가 M3a4 사건어 대조에 쓰인다(영어 제목 후보가 사건어 +30을 받는 테스트). (5) `video_sources` 순서(작은 해상도, webm 먼저, 원본 끝), 통신사 표기 파일 제외. (6) `captured_at` 이 사건 날짜보다 늦은 `DateTimeOriginal` 이면 `null`, 업로드 시각은 쓰이지 않는다. (7) 응답 미디어 전부 스키마 통과(`date_relation`·`selection` 제외). (8) 요청에 `WIKIMEDIA_USER_AGENT` 와 동시 3 제한이 적용된다 |
| T3 | 결과 캐시 (V3) | backend-dev | V3 마이그레이션·엔티티, 캐시 흐름, 출처별 TTL 설정, 테스트 | T2 | (1) 위 표 캐시 행 통과. (2) V3 적용·`ddl-auto=validate` 통과. (3) 저장 열에 요청자 정보가 없다(스키마 확인). (4) 실패·시간 초과는 캐시하지 않는다. (5) 어댑터가 "쿼터 건너뜀"을 알리면 캐시하지 않는다(가짜 어댑터로 — M3a5 대비) |
| T4 | 보조 경로 (b-2)·(b-3) (크기 조건부) | backend-dev | 어댑터 보강, fixture, 테스트 | T3 | 오케스트레이터가 크기 확인 뒤 진행. (1) (b-2)·(b-3) 후보는 같은 날 날짜 일치와 사건어 일치가 둘 다 있을 때만 붙는다. (2) `insource:` 일치만 있는 후보는 날짜 점수 0. (3) 1969-07-20 녹화 fixture에서 발사 장면 영상(`Flight of Apollo 11 … since launch`)은 붙지 않는다. (4) 새 날짜 요청 수 최대 11 |
| T5 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T4 (또는 T3) | (1) 기준 표. (2) `./gradlew check` 통과, 3회 반복 안정. (3) S1·S3 보고. (4) fixture 기준 데모·표본 날짜 결과 표(사건, 영상 유무, `selection`, `captured_at`, `date_relation.kind`) |
| T6 | PM 품질 확인 (PR 검수 때) | PM | - | T5 | 서버 실행 중 P1으로 데모 3 + 표본 5 날짜를 뽑아 기준표와 대조: 엉뚱한 영상, 있어야 하는데 없는 영상, `tragic` 날짜에 영상 없음, `unknown` 영상에 사건 날짜가 촬영 시점으로 들어가지 않음. 국내 날짜에 Commons 영상이 없는 것은 정상. (선택) P0으로 핫링크 재생 확인 — 403·429면 보고 |

```
# C1 — 기준표 JSON (content-curator). ok
python3 -c 'import json,sys; json.load(open(sys.argv[1])); print("ok")' data/verification/event-media-expectations.json

# S1·S3 — M3a4와 같은 명령

# P0 — (선택, PM) Commons 핫링크 재생 확인. 리서치 6.1절 명령 2개를 그대로 실행한다
#      (206 또는 200 + video/webm, 200 + image/jpeg 이면 정상)

# P1 — PM 품질 확인 (서버 실행 중)
for d in 1988-09-17 2002-06-22 1995-03-15 1950-06-25 1969-07-20 1986-09-20 2000-02-29 2014-04-16; do
  echo "== $d"
  curl -s "http://localhost:8080/api/story/world?date=$d" \
    | jq -c '.events[] | {summary: .summary[0:30], tone, videos: [.media[] | select(.display_mode=="video_file" or .display_mode=="video_embed") | {source_url, attribution, selection, captured_at, date_relation}]}'
done
```

담당별: content-curator 1, backend-dev 3, test-engineer 1, PM 1.

## 리스크와 확인할 것

1. **국내 날짜에 Commons 영상이 거의 없다**(리서치 T1, 위험 A) — 데모 날짜(모두 국내 사건)에는 이 기능만으로 영상이 붙지 않는다. 정상(사진만)이다. 국내 영상은 M3a5·M3a5b가 주 경로다.
2. **촬영일 필드 오류**(위험 2) — 업로드·복원일이 들어 있다. R-V3로 비우고 "촬영 시점 미상"으로 붙인다.
3. **핫링크** — 정책상 "가능하지만 권장하지 않음"이다. 로컬 데모 규모에서는 문제를 찾지 못했다. 공개 배포(#5) 때 다시 본다. `thumburl` 만 쓴다(위험 4·5).
4. **모바일 용량** — 원본이 수백 MB일 수 있다. 작은 변환본부터, 10분 상한(위험 6).
5. **Wikidata 질의 한 번 더** — 날짜당 1회, 캐시로 되풀이하지 않는다. M3a2와 합칠 수 있으면 합친다.
6. **보조 경로의 엉뚱한 결과** — T4는 날짜·사건어 둘 다 맞을 때만 붙인다. 품질 확인에서 나쁘면 설정으로 끈다(`media.video.commons.auxiliary-enabled`).

## PM이 정할 것

없음. 결정됨: 10.1절 1·2·3·4·21번(2026-10-11), M3a4에서 나누는 것(로드맵 D1 (a), 2026-10-12).
