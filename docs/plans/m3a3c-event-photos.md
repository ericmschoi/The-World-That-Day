# 사건 사진: PageImages와 Commons 파일 (M3a3c)

> 브랜치: `feat/event-photos` · 작성: planner · 상태: **PM 승인 (2026-10-14, M3a3b에서 분할)** — M3a3b 개정판의 2부(T5~T8)를 옮기고 번호를 T1~T4로 바꿨다(T5→T1, T6→T2, T7→T3, T8→T4). 완료 기준의 내용은 그대로이고, M3a3b가 만든 부품을 "이어받는 계약"으로 적은 것만 다르다.
> 기준: PM 미디어 결정 2026-10-11, 안전장치 1~6, `CLAUDE.md` 7절 1·2·3·5·6항, `docs/product-brief.md` 6.1절·6.2절·6.3절·8절·12절, `docs/research/day-events.md` 3·4절, `docs/plans/m3a3b-commons-photos.md`(Commons 파일 매퍼·사진 규칙·시간 상한), `docs/plans/m3a3-event-media.md`(형식·R1~R9·`date_relation`), M3-P7, M3-P16 변경(2026-10-12), #22 (c), #15 초안(±5년)
> 선행: **M3a3b `feat/commons-photos` 머지**. 이 기능은 그 `main` 에서 갈라진다.
> 뒤따르는 기능: **M3b2 T6**(PM 실제 사건 사진 확인, 이 PR 머지 뒤 `main` 에서). M3a4는 같은 연결 자리를 고친다(리스크 5).

## 이 계획서가 생긴 이유 (2026-10-14)

| 항목 | 내용 |
|---|---|
| 분할 | M3a3b를 머지된 실측(M3a3 505, M3a3a 456)과 파일별 셈으로 다시 재니 약 715줄이었다. PM 결정 1 **(a)**: 그날 사진(M3a3b, 약 485)과 사건 사진(이 기능, 약 230)의 두 PR. M3 PR 19 → 20 |
| 나눈 기준 | 두 PR 모두 화면에 보이는 결과와 PM 확인이 있다 — M3a3b 뒤 M3b2b T6(그날 사진 묶음), 이 기능 뒤 M3b2 T6(사건 사진) |
| ★ | PM 결정 2 **(b)**(M3a3b T2는 backend-dev)와 같은 흐름으로 이 기능도 backend-dev가 구현한다. ★ 표시는 학습 가치가 있는 T2에 남긴다 — PM이 직접 하고 싶으면 T2 시작 전에 오케스트레이터에게 말한다 |

## 목표

1. `GET /api/story/world` 의 `events[].media` 를 Commons **사진**으로 채운다(사건마다 최대 1장). 라이선스가 확인되지 않아도 출처·라이선스 상태·촬영 시점과 함께 직접 보인다. 촬영 시점을 모르는 사진은 "촬영 시점 미상"으로 붙는다.
2. `tragic` 사건에는 사진을 붙이지 않고 찾지도 않는다. 큐레이션 `hide` 로 내린 사건도 찾지 않는다.
3. 사건과 촬영 연도가 ±5년(`media.max-years-apart`)을 넘는 사진은 붙이지 않는다.
4. 사건 사진과 같은 파일은 `day_media` 에서 뺀다. 같은 파일은 앞 사건에만 붙는다.
5. 사건 사진 조회가 실패하거나 늦어도 사건과 `day_media` 는 그대로 200으로 나가고 `partial: true` 만 켜진다.

## 범위 밖

- 그날 사진(`day_media`), Commons 파일 매퍼, 사진 규칙, 시간 상한, 이월 리뷰 권장 → M3a3b (이 기능은 쓰기만 한다)
- 영상 → M3a4~M3a5b
- 사진 메타데이터 캐시 → 후속(리스크 4)
- Wikidata 대표 이미지(P18) — 쓰지 않는다
- 미디어 파일 복사·프록시 (안전장치 1)
- 프론트엔드 → 변경 없음(M3b2가 이미 사건 `media` 를 그린다)

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| 한국어 위키백과 PageImages | **확인됨** — [day-events.md 4절](../research/day-events.md) [48][63] | 사건 대표 링크 문서의 대표 이미지 파일 이름(`prop=pageimages&piprop=name&pilicense=free`, 제목 묶음 1회) | 비자유 이미지를 1차로 거른다(M3-P7). `wikimedia.kowiki-api-url` 재사용 |
| Wikimedia Commons (Action API) | **확인됨** — [day-events.md 3절 Commons 카드](../research/day-events.md#3-출처-카드) | 파일 제목 묶음(`titles=File:A\|File:B&prop=imageinfo&iiprop=url\|mime\|size\|extmetadata&iiurlwidth=640&iiextmetadatalanguage=ko`) | M3a3b와 같음: `WikimediaHttpClient` 로만, User-Agent·429·동시 3, 작가·라이선스·원문 페이지 표시, 썸네일은 `thumburl` 만, 핫링크만 |

키 없음. 새 HTTP 클라이언트를 만들지 않는다. User-Agent는 연락처로 **저장소 주소만**(`https://github.com/ericmschoi/The-World-That-Day`) — 이메일을 넣지 않는다.

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| 미디어 표시·촬영 시점 미상·안전장치 1~6 | M3a3 계획서와 같음 | 결정 2026-10-11 |
| #13-C | (a) Commons 주소 그대로(핫링크, `no-referrer`) | 결정 |
| #22 | (c) + `tragic` 사건엔 미디어 없음 | 결정 |
| M3-P7 | 비자유 위키 로컬 파일 제외 | 결정 |
| M3-P16 | **변경 2026-10-12**: 원문을 거르지 않음 → 응답에 남은 사건은 모두 사진 후보. `hide` 는 바로 내리는 수단 | 결정 |
| 근접 연도 | 사건 사진은 ±5년 안만 (`media.max-years-apart: 5`) | #15 확정 전 초안 |
| M3a3b 결정 1·2 | 1 (a) 두 PR — 이 기능이 사건 사진, 2 (b) M3a3b T2는 backend-dev | **결정 2026-10-14** |

## 이어받는 계약

### M3a3b (선행 기능 — 이름은 M3a3b 머지 코드를 따른다, M3a3b T1 (12) 보고)

| 부품 | M3a3b에서 | 이 기능에서 |
|---|---|---|
| Commons 클라이언트 | `CommonsProperties`(`wikimedia.commons-api-url`), 날짜 범주 질의, `query.pages` 읽기, `error`·`ratelimited` 처리 | 파일 제목 묶음 질의를 더한다(같은 페이지 읽기) |
| Commons 파일 매퍼 | 페이지 하나 + MIME 조건 + 위치 대체값 + 요청 날짜 → `Optional<MediaItem>`. 레코드 → `MediaFilter.check` → `MediaNormalizer.normalize` | MIME `image/jpeg`, 위치 대체값은 사건 `scope`: `domestic` → `country:KR`, `world` → `world`, `unknown` → `unknown` |
| 설정 | `MediaLookupProperties`(`media.day-media-limit`, `media.lookup-deadline`) | `media.max-years-apart` 를 더한다 |
| 시간 상한 | 미디어 조회 전체를 `media.lookup-deadline`(3s) 안에(`within` 방식, 1s 취소 유예) | 사건 사진 경로(PageImages → Commons)도 같은 상한 안에 둔다 |
| 미디어 조회 | `dayMedia(LocalDate, boolean tragicDay)` → `(List<MediaItem>, boolean failed)`, `WorldStoryService` 가 판정 뒤 부름 | 사건 목록(대표 링크·`Verdict`)을 받아 사건별 미디어까지 돌려준다. `Event.of(KoWikiEvent, Verdict, List<MediaItem>)` |
| 테스트 기반 | `WikimediaWireMockTest` 에 Commons 경로(위키백과와 다름) | PageImages는 위키백과와 같은 `/w/api.php` 라 쿼리(`prop=pageimages`)로 구분한다 |

### M3a2·M3a1 (머지된 코드)

- `EventToneJudge.Verdict(Tone tone, Basis basis, Scope scope, boolean hidden)`. 사진 대상: `hidden == false` 이고 `tone == NORMAL` 이고 `title`(= `KoWikiEvent.linkTitles().getFirst()`)이 있음.
- `ActionApiKoWikiLinkResolver` 모양: `UriComponentsBuilder` + `titles={titles}` 묶음 50개, `formatversion=2`, `normalized`·`redirects` 따라감, `error.code == "ratelimited"` 이면 `blockHost`. PageImages가 같은 모양을 따른다.
- `DateRelation(Kind kind, int years)` — ±5년은 `years` 의 절댓값으로 본다. `UNKNOWN` 은 연도를 모르므로 붙인다.

## 고르는 법

| 자리 | 후보 | 개수·규칙 |
|---|---|---|
| `events[].media` | 응답에 남은 사건 중 `tone == NORMAL` 이고 `title`(대표 링크)이 있는 사건 → 한국어 위키백과 PageImages(`pilicense=free`) 파일 이름 → Commons 파일 제목 묶음 → M3a3b 매퍼 | 사건마다 사진 최대 1(M3a4부터 영상이 더해진다). `tragic` 사건(`default` 포함) 0, PageImages 요청에도 넣지 않음. `hidden` 사건은 요청에 넣지 않음. `years` 의 절댓값 > `media.max-years-apart`(5)면 붙이지 않음(`unknown` 은 붙이고 "촬영 시점 미상"). 같은 파일이 두 사건에 나오면 앞 사건에만 |
| `day_media` 와의 관계 | — | 사건 사진과 같은 파일(`id`)은 `day_media` 에서 뺀 뒤 `media.day-media-limit`(6)개를 자른다 |

요청 수(API 호출 1회): 위키백과 PageImages 최대 1(대상 사건이 없으면 0), Commons 최대 2(날짜 범주 0~1 — M3a3b, 파일 제목 묶음 0~1). 날짜 범주 요청과 PageImages → Commons 요청은 동시에 보내도 된다(공용 `Semaphore(3)` 안).

### 실패와 로그

| 규칙 | 내용 |
|---|---|
| 일부 실패 | PageImages 실패 또는 파일 제목 묶음 실패 → `events[].media` 만 모두 `[]`, `day_media` 는 그대로, `partial: true`, 200. `tragic` 으로 비운 미디어, 규칙으로 버린 미디어, 대표 이미지가 없는 문서는 `partial` 이 아니다 |
| 시간 상한 | M3a3b와 같은 상한 안. 사건 경로가 늦어도 `day_media` 가 상한 안에 끝났으면 남긴다 |
| 503 | 바뀌지 않음 |
| 로그 | `title` 이름표는 `kowiki pageimages of <개수>`, `commons files of <개수>` 만. 날짜·파일 이름·문서 제목을 넣지 않는다. 사건은 ID 해시 8자리만 |

## API / 데이터 변경

- `GET /api/story/world`: `events[].media` 에 M3a3 형식의 사진 레코드(0~1장)가 들어간다. `day_media` 에서 사건 사진과 같은 파일이 빠진다. `partial` 에 사건 사진 실패가 더해진다. 새 에러 코드 없음. 응답 키·형은 그대로다(웹 계약 변화 없음).
- 설정 추가: `media.max-years-apart: 5`(0 이상, 음수면 시작 실패, `MediaLookupProperties`).
- DB 변경 없음. 데이터 파일 없음(fixture는 테스트 리소스).

## 테스트 방침

- M3a3b "테스트 방침"을 따른다. **외부 네트워크를 부르지 않는다** — WireMock + 녹화 fixture.
- 녹화 fixture: `fixtures/wikimedia/kowiki/` 에 PageImages 응답(데모 사건 4개 제목), `fixtures/wikimedia/commons/` 에 돌려받은 파일의 `imageinfo` 응답. `README.md` 에 원 URL·녹화 일자·User-Agent(저장소 주소)·라이선스. 바이트를 고치지 않는다. 이미지 파일은 받지 않는다.
- 합성 fixture로 규칙 표의 행마다 시험. 실제 비극 사건 이름을 쓰지 않는다. 표본 날짜는 데모 3날짜와 공개 역사 사건 날짜·경계값. 실제 사람의 생년월일을 쓰지 않는다.
- 응답 미디어 전부를 `date_relation` 을 뺀 뒤 M3a3 스키마로 검증(`DataSchemas`).

## 데모 입력 3개로 확인할 것

사건·`tone` 은 M3a2 결과, `day_media` 는 M3a3b 결과 그대로다. 사건 사진 값은 **T1 녹화** 결과로 정하고 T2가 그 값으로 고정 단언한다(리서치 기준 영어 위키백과 올림픽·월드컵 문서는 자유 대표 이미지가 없었다).

| 데모 입력 | 사건 (M3a2) | 확인 (T2) |
|---|---|---|
| 1988-09-17 + "서울" | 3개, 모두 `normal`/`curated`/`domestic`: ① `1988년 하계 올림픽`(`Q8470`) ② `KBS 1TV`(`Q777278`) ③ `KBS 2TV`(`Q624509`) | PageImages 요청 **1회**에 세 제목이 모두 들어간다. 각 사건 `media` 0~1장, 모두 `display_mode: image`, 스키마 통과, `years` 절댓값 5 이하 또는 `unknown`. 로고(SVG·PNG)·비자유 로컬 파일은 붙지 않는다(KBS 두 사건은 대표 이미지가 로고뿐이면 `[]` — 정상). 사건 사진과 같은 파일은 `day_media` 에 없다. `partial: false` |
| 2002-06-22 + "광주 서구 풍암동" | 1개: `2002년 FIFA 월드컵`(`Q47735`), `normal`/`curated` | PageImages 요청 1회(제목 1개). 사건 `media` 0~1장(T1 녹화로 확정). `day_media` 는 M3a3b 결과(1장) 그대로(같은 파일이 아니면). `partial: false` |
| 1995-03-15 + "서울대학교병원" | 0개, `notice.code` = `NO_EVENTS_FOUND` | PageImages 요청 0, Commons 요청 1(날짜 범주만). `partial: false` |
| (합성) `tragic` 섞인 날 | `tragic`(`default` 포함) 사건 하나 + `normal` 사건 하나 | `tragic` 사건 `media` = `[]`, PageImages 요청에 그 제목 없음, `normal` 사건은 사진 후보(`day_media` 는 M3a3b 규칙대로 `[]`) |

## 크기 (400줄 규칙)

제품 코드·설정(S1) 기준. M3a3b 계획서 "크기"의 파일별 셈(실측 M3a3 ×3.9·M3a3a ×3.3으로 확인).

| 작업 | 내용 | 다시 셈 | 범위 |
|---|---|---|---|
| T1 | PageImages 조회(약 90), Commons 파일 제목 묶음 질의(약 15) | 약 105 | 90~135 |
| T2 | 사건 사진 고르기(±5년·중복·앞 사건, 약 80), `Event.of`·서비스·`partial`(약 25), `max-years-apart`(약 15), 설정 4 | 약 125 | 100~155 |
| **합계** | | **약 230** | **190~290** |

- 400 아래다. **알림만: T2 뒤 누적 S1이 350을 넘으면** 오케스트레이터가 PM에게 알린다(범위 위쪽보다 크면 셈이 틀린 것이라 이유를 함께 적는다).
- 테스트(S2)·fixture는 따로 보고한다.

## 작업 목록

순서: T1 → T2 → T3 → T4(PR 검수 때) → 머지 → M3b2 T6(`main` 에서). (이전 M3a3b 번호: T5 → T1, T6 → T2, T7 → T3, T8 → T4)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | PageImages 조회, Commons 파일 제목 묶음 질의, 녹화 fixture | backend-dev | **M3a3b 머지** | (1) PageImages: `wikimedia.kowiki-api-url` 에 `action=query&prop=pageimages&piprop=name&pilicense=free&redirects=1&titles=…&format=json&formatversion=2`, 제목 묶음 1회(최대 50개), `normalized`·`redirects` 를 따라 요청 제목 → 파일 이름. 대표 이미지가 없는 문서는 "없음"(실패 아님). `ratelimited` → `blockHost` + 실패. (2) Commons 파일 제목 묶음: `titles=File:A\|File:B&prop=imageinfo&…`(M3a3b와 같은 `iiprop`·`iiurlwidth`·`iiextmetadatalanguage`), 최대 50개, 응답을 M3a3b 매퍼로 읽는다(`missing` → 제외). (3) 두 요청 모두 `WikimediaHttpClient`, User-Agent 저장소 주소(이메일 없음), 호출별 읽기 타임아웃 `media.lookup-deadline`. (4) WireMock 503 → WARN 줄 `title` 이 `kowiki pageimages of N`·`commons files of N` 이고 로그에 `YYYY-MM-DD` 모양·`File:`·`Category:`·문서 제목이 없다. (5) PageImages 스텁은 `prop=pageimages` 쿼리로 구분하고, 기존 위키백과 스텁(같은 `/w/api.php`)이 PageImages 요청에 답하지 않는다(시험으로 확인). (6) 녹화 fixture: 데모 사건 4개 제목의 PageImages 응답(`fixtures/wikimedia/kowiki/`), 돌려받은 파일의 `imageinfo` 응답(`fixtures/wikimedia/commons/`), README 행 추가. 이미지 파일은 받지 않는다. 네트워크를 쓸 수 없으면 "막힘" + PM `curl` 명령(`-H "User-Agent: $WIKIMEDIA_USER_AGENT"`). (7) 보고서에 데모 사건 4개 결과 표(제목, 파일 이름 또는 없음, MIME, 라이선스, `DateTimeOriginal`, 붙을지 여부)와 `url`·`thumburl` 호스트(M3a3b 기록과 다른 호스트가 있으면 따로). (8) `./gradlew check` 성공 |
| T2 | ★ 세계 API 연결: 사건 사진, ±5년, 중복, `partial` | backend-dev | T1 | (1) "데모" 표 4행 통과(값은 T1 녹화로 고정). (2) 데모 3날짜 응답의 모든 미디어가 스키마 통과(`date_relation` 뺀 뒤). (3) 합성: ±6년 사건 사진 → 안 붙음, ±5년 → 붙음, 촬영 시점 미상 사건 사진 → 붙고 `unknown`. (4) `tragic`: `TRAGIC` 사건(`default` 포함) → `media` = `[]` 이고 PageImages 요청 제목에 없음. 다른 `normal` 사건은 후보. (5) `hidden`: `hide` 로 내린 사건의 제목은 PageImages 요청에 없다. (6) 같은 파일이 두 사건에 나오면 앞 사건에만. 사건 사진과 같은 `id` 는 `day_media` 에 없고, 그 뒤에도 `day_media` 는 최대 6개. (7) PageImages 503 → `events[].media` 모두 `[]`, `day_media` 는 채워짐, `partial: true`. 파일 제목 묶음 503 → 같은 결과. (8) 요청 수(API 호출 1회당): PageImages ≤ 1, Commons ≤ 2. 1995-03-15는 PageImages 0·Commons 1. 대상 사건이 없으면 PageImages 0. (9) 시간 상한이 사건 사진 경로(PageImages → Commons)까지 덮는다: 응답하지 않는 PageImages 스텁 → `lookup-deadline` + 1초 + 여유(1초) 안에 200, `partial: true`, `day_media` 는 상한 안에 끝났으면 남는다. (10) `media.max-years-apart`(음수면 시작 실패, 오류 메시지에 설정 이름, `MediaLookupProperties`). (11) 기존 `WorldStory*Test`·M3a3b 테스트의 단언을 고치지 않는다(준비 코드·요청 수 대상 경로만 바꿀 수 있고, 바꾼 줄을 보고). (12) `Event.of` 가 미디어 목록을 받고, 응답 키·형은 그대로(`MediaItem` 키 27개 — M3a3b T2 (9)의 Spring 매퍼·MockMvc null 키 시험이 사건 `media` 항목에도 적용). (13) `./gradlew check` 성공, 보고서에 누적 S1 |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | T2 | (1) T1·T2 기준 표(기준마다 테스트 이름). (2) `cd server && ./gradlew check` 통과, 3회 반복 안정. (3) S1·S2·S3(0이 정상) 보고. (4) 데모 3날짜 미디어 표(자리, 사건, 순서, `display_mode`, `license`, `license_status`, `captured_at`, `date_relation`). (5) 최종 서버 미디어 DTO 필드 목록이 M3a3b T3 (5)와 같은지. (6) 스텁으로 잰 세계 섹션 최악 시간(위키백과·Wikidata·Commons·PageImages 모두 상한까지 늦을 때) |
| T4 | 실제 호출 확인 (PR 검수 때) | PM | T3 | (1) `docker compose up -d db`, `cd server && ./gradlew bootRun` 이 뜬다. (2) P1~P3 결과가 "데모" 표와 맞는다. (3) 사건 사진의 `thumbnail_url` 하나를 브라우저로 열어 보이고 `source_url` 페이지의 라이선스가 `license` 와 같다. (4) 사건 사진이 사건과 너무 엉뚱하면 PR 코멘트로 알린다(리스크 1). (5) 머지 뒤 **M3b2 T6**(사건 사진 화면 확인)을 `main` 에서 한다 |

완료 기준에 쓰는 명령 (저장소 루트):

```
# S1 — 제품 코드·설정 줄 수
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
# S3 — 데이터 파일 줄 수 (0이 정상)
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P1~P3 — PM 실제 호출 (서버 실행 중)
curl -s 'http://localhost:8080/api/story/world?date=1988-09-17' \
  | jq '{partial, events: [.events[] | {title, tone, media: [.media[] | {display_mode, license, license_status, captured_at, date_relation, thumbnail_url}]}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' \
  | jq '{partial, events: [.events[] | {title, media}], day_media: [.day_media[] | .id]}'
curl -s 'http://localhost:8080/api/story/world?date=1995-03-15' | jq '{events, day_media, notice, partial}'
```

담당별: backend-dev 2 (T1, T2), test-engineer 1 (T3), PM 1 (T4). 모두 4개.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 시점 |
|---|---|---|
| **M3b2 T6** (PM, 웹 사건 사진 실제 확인) | 실제 `events[].media` | 이 PR 머지 뒤 `main` 에서. M3b2b T6(그날 사진)을 아직 안 했으면 한자리에서 함께 해도 된다 |
| M3a4 영상 검색 틀 | — (같은 `WorldStoryService` 연결 자리, 사건별 미디어 목록에 영상이 더해짐) | 병렬이면 worktree를 나누고 오케스트레이터가 합친다(리스크 5) |

## 리스크와 확인할 것

1. **사건 대표 이미지가 사건과 어긋날 수 있다** — 특히 1988의 KBS 두 줄은 원문 그대로 나가므로(M3-P16) 그 문서의 대표 이미지(로고가 아니면 방송국 건물 등)가 붙을 수 있다. `date_relation` 으로 정직하게 보이고 ±5년을 넘으면 붙이지 않는다. 촬영 시점 미상 이미지는 연도 거르기가 안 되므로 "촬영 시점 미상"으로만 보인다. 너무 엉뚱하면 사건 미디어에서 `unknown` 을 빼는 규칙을 후속으로 정할 수 있다.
2. **보수적 `tragic` 이 사진을 지운다** — Wikidata가 실패하거나 QID가 없는 날(큐레이션 밖 사건이 `default` → `tragic`)에는 사건 사진이 없어진다. 의도된 방향(6.3절)이다.
3. **JPEG만** — PNG 사진도 빠진다(M3a3b 리스크 5와 같음).
4. **요청마다 PageImages·Commons를 부른다** — 캐시가 없다. 로컬 데모라 분당 200 제한에 닿지 않는다. 필요하면 M3a4b 결과 캐시에 사진 출처를 더한다(후속).
5. **같은 연결 자리** — M3a4가 병렬로 진행되면 둘 다 `WorldStoryService` 와 `Event.of` 의 미디어 목록을 고친다. worktree를 나누고 오케스트레이터가 합친다.
6. **세계 섹션 시간** — 미디어 상한(3초 + 1초 유예)은 M3a3b와 같다. 이 기능은 상한 안에서 요청이 하나 더 줄을 선다(PageImages → Commons). T3 (6)에 측정값.
7. **위키백과 스텁과의 충돌** — PageImages는 위키백과 날짜·연도 문서와 같은 `/w/api.php` 다. 경로만으로 맞추는 기존 스텁이 PageImages 요청에 엉뚱한 본문을 줄 수 있다. 대응: T1 (5).
8. **알려진 한계** — 제외 목록의 `EPA`·`Getty` 안전 쪽 오탐은 M3a3b 리스크 9와 같다. 사건 사진에서 보이면 T3 표에 적는다.

## PM 결정

| # | 결정 | 상태 |
|---|---|---|
| M3a3b 결정 1 | (a) 두 PR — 이 기능이 사건 사진(약 230) | 결정 2026-10-14 |
| M3a3b 결정 2 | (b) M3a3b T2는 backend-dev — 이 기능의 ★ T2도 backend-dev가 기본(PM이 원하면 T2 전에 말함) | 결정 2026-10-14 |

새로 묻는 것 없음.
