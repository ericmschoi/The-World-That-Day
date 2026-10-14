# 사건 사진과 그날 사진: Commons 연결 (M3a3b)

> 브랜치: `feat/commons-photos` · 작성: planner · 상태: **PM 승인 (2026-10-13)** — M3a3 분할 결정 A (a)로 생김. 내용은 `docs/plans/m3a3-event-media.md` 2026-10-13 개정판의 2부(T5~T8)를 옮긴 것이고 작업 번호만 T1~T4로 바꿨다(T5→T1, T6→T2, T7→T3, T8→T4). **2026-10-13 선행 바꿈**: M3a3 확인점 결과(S1 505, PM (a))로 파서·라이선스 상태·차단과 제외가 M3a3a `feat/media-rules` 로 옮겨져, 이 기능은 **M3a3a 머지 뒤**에 시작한다.
> 기준: PM 미디어 결정 2026-10-11, 안전장치 1~6, `CLAUDE.md` 7절 1·2·3·5·6항, `docs/product-brief.md` 6.1절·6.2절·6.3절·8절·12절, `docs/research/day-events.md` 3·4·8절, `docs/plans/m3a3-event-media.md`(형식·R1~R9·`date_relation`), `docs/plans/m3a3a-media-rules.md`(파서·라이선스 상태·차단과 제외), M3-P16 변경(2026-10-12), #21 (a), #22 (c), M3-P17 (a)
> 선행: **M3a3a `feat/media-rules` 머지**(공용 규칙 — 그 앞의 M3a3 `feat/event-media` 형식 포함). 이 기능은 그 `main` 에서 갈라진다. M3a2 `feat/world-events-api`(PR #11)도 머지 완료.
> 뒤따르는 기능: M3b2(PM 화면 확인에 실제 사진이 필요해 이 기능 뒤), M3a4b(이 기능의 Commons 파일 매퍼를 재사용).

## 이 계획서가 생긴 이유 (2026-10-13)

| 항목 | 내용 |
|---|---|
| 분할 | M3a3을 다시 재니 약 1,290줄(최근 실측 ×2.8)이라 PM이 두 PR로 나눴다(결정 A (a)). 외부 호출이 없는 형식·규칙은 M3a3, Commons 연결은 이 기능 |
| **선행 바꿈 (확인점 결과)** | M3a3 T2 뒤 누적 S1 505 > 450 → PM (a) M3a3 T3을 M3a3a로 뺐다. 이 기능이 쓰는 파서·라이선스 상태·차단과 제외가 M3a3a에 있으므로 선행이 **M3a3 → M3a3a** 로 바뀌었다. 이 계획서에서 바뀐 곳: 머리말, "범위 밖", "이어받는 계약"(M3a3a 절 추가), "Commons 사진 규칙" 표의 출처 표기, T1 의존·(2)에 차단·제외 행 명시, "크기" 절 메모와 리스크 10(M3a3 T2 실측 비율 ×3.9로 보면 약 780) |
| 이 기능으로 옮긴 설정 | `media.max-years-apart`, `media.day-media-limit`, `media.lookup-deadline`, `wikimedia.commons-api-url` — 쓰는 곳이 이 기능뿐이다 |
| 바뀌지 않은 것 | 규칙·데모 기대값·크기 추정값·확인점(T1 뒤 400)·완료 기준은 M3a3 개정판 2부 그대로다(T1 (2)는 규칙 표에 이미 있던 차단·제외 행을 테스트 목록에 적어 넣었을 뿐이다) |

## 목표

1. `GET /api/story/world` 의 `events[].media` 와 `day_media` 를 Commons **사진**으로 채운다. 라이선스가 확인되지 않아도 출처·라이선스 상태·촬영 시점과 함께 직접 보인다. 촬영 시점을 모르는 사진도 "촬영 시점 미상"으로 보이고 그날 사진으로 분류되지 않는다.
2. `tragic` 사건과 그날에는 사진을 붙이지 않는다. 큐레이션 `hide` 로 내린 사건은 사진도 찾지 않는다.
3. Commons 파일 메타데이터 → `MediaItem` 매핑을 M3a4b(Commons 동영상)가 재사용할 수 있게 만든다.
4. 미디어 조회가 실패하거나 늦어도 사건은 그대로 200으로 나가고 `partial: true` 만 켜진다.

## 범위 밖

- 형식·정규화·`date_relation` → M3a3, 파서·라이선스 상태·차단과 제외 → M3a3a (이 기능은 쓰기만 한다)
- 영상 → M3a4~M3a5b
- 사진 메타데이터 캐시 → 후속(필요하면 M3a4b 결과 캐시에 출처를 더한다). 이 기능은 요청마다 부른다(리스크 7)
- Wikidata 대표 이미지(P18) — 쓰지 않는다
- 미디어 파일 복사·프록시 (안전장치 1)
- 프론트엔드 → M3b2·M3b2b

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| Wikimedia Commons (Action API) | **확인됨** | 파일 메타데이터(`prop=imageinfo`, `iiprop=url\|mime\|size\|extmetadata`, `iiurlwidth=640`), 날짜 범주 `Category:South Korea photographs taken on YYYY-MM-DD`(`generator=categorymembers&gcmtype=file`) | User-Agent·429·동시 3(M3a1·M3a1b 공통, `WikimediaHttpClient` 로만). 작가·라이선스·원문 페이지 표시. REST 요약·OTD의 `thumbnail` 을 쓰지 않는다. 썸네일은 API가 준 `thumburl` 만. 핫링크는 "가능하지만 권장하지 않음" — 안전장치 1에 따라 핫링크로만 쓰고 공개 배포 때 다시 본다 — [day-events.md 3절](../research/day-events.md#3-출처-카드) |
| 한국어 위키백과 PageImages | **확인됨** | 사건 대표 링크 문서의 대표 이미지 파일 이름(`prop=pageimages&piprop=name&pilicense=free`, 제목 묶음 1회) | 비자유 이미지를 1차로 거른다(M3-P7). `wikimedia.kowiki-api-url` 재사용 |

키 없음. 새 HTTP 클라이언트를 만들지 않는다.

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| 미디어 표시·촬영 시점 미상·안전장치 1~6 | M3a3 계획서와 같음 | 결정 2026-10-11 |
| #13-C | (a) Commons 주소 그대로(핫링크, `no-referrer`) | 결정 |
| #21 | (a) 사건이 없는 날도 같은 날 사진(`day_media`)은 찾는다 | 결정 |
| #22 | (c) + `tragic` 사건엔 미디어 없음, `tragic` 이 있는 날은 `day_media` 없음 | 결정 |
| M3-P7 | 비자유 위키 로컬 파일 제외 | 결정 |
| M3-P16 | **변경 2026-10-12**: 원문을 거르지 않음 → 응답에 남은 사건은 모두 사진 후보. `hide` 는 바로 내리는 수단, 같은 사건의 다른 항목보다 `hide` 가 이김 | 결정 |
| M3-P17 | (a) "그날 찍힌 사진"은 `same_day` 만 → `day_media` 순서 | 결정 |
| 근접 연도 | 사건 사진은 ±5년 안만 (`media.max-years-apart: 5`) | #15 확정 전 초안 |
| M3a3 분할 | A (a) — 이 기능이 2부 | **결정 2026-10-13** |
| M3a3 확인점 | (a) T3을 M3a3a로 — 이 기능의 선행이 M3a3a | **결정 2026-10-13** |

## 이어받는 계약

### M3a2 (머지된 코드)

| 자리 | 지금 | 이 기능에서 |
|---|---|---|
| 서비스 | `WorldStoryService.find`: `KoWikiEventSource.find` → `allUnavailable()` 이면 `ApiProblem(503, UPSTREAM_UNAVAILABLE)` → `EventToneJudge.judge` → `hidden` 빼고 `normal` 먼저 → `partial = day.degraded() \|\| judgement.partial()` | 판정 뒤 미디어 조회를 더한다. `partial = day.degraded() \|\| judgement.partial() \|\| 미디어 실패`. `Event.of` 가 미디어 목록을 받는다 |
| 판정 | `EventToneJudge.Verdict(Tone tone, Basis basis, Scope scope, boolean hidden)`, `Judgement(List<Verdict> verdicts, boolean partial)` | 사진 대상: `hidden == false` 이고 `tone == NORMAL`. 날 사진 차단: `hidden == false` 이고 `tone == TRAGIC` 인 사건이 하나라도 있음. `place_ref` 는 `scope` 에서 |
| 에러 | `ApiProblem` → ProblemDetail + `code` | 새 에러 코드 없음. 미디어 실패는 늘 200 + `partial: true`. 503은 위키백과 두 문서가 모두 `UNAVAILABLE` 일 때만(그때는 미디어를 찾지 않음) |
| HTTP | `WikimediaHttpClient.get(source, title, uri[, readTimeout])` — 호출별 읽기 타임아웃, 모든 Wikimedia 호스트가 나눠 쓰는 공정 `Semaphore(3)`(빈자리 대기는 `read-timeout` 4초까지, 인터럽트되면 `INTERRUPTED`), 실패 시 WARN 1줄(`source`·`title`·원인) | Commons·PageImages도 이것으로만. 호출별 읽기 타임아웃 = `media.lookup-deadline`. `title` 에는 날짜·파일 이름 없는 이름표만 |
| 시간 상한 | `SparqlWikidataSignalSource.within`: 5초까지 기다리고, 넘으면 `cancel(true)` 후 최대 1초(`CANCEL_GRACE`) 종료를 기다려 WARN 1줄 | 미디어 조회 전체에 같은 방식의 상한(3초). 이 도우미를 `wikimedia` 패키지 공용으로 빼도 된다(기존 Wikidata 테스트를 고치지 않고 통과할 때만) |
| 설정 묶음 | `WikimediaProperties`, `WikidataProperties`(주소 검증) | `CommonsProperties`(`wikimedia.commons-api-url`, 절대 `http(s)` 아니면 시작 실패), `MediaProperties` 에 이 기능 설정 추가 |

### M3a3 (형식)

`MediaRecord`(저장 형식, `LicenseStatus`·`DatePrecision` 열거형 포함), `MediaItem`(+ `VideoSource`·`Render`·`DateRelation`), `MediaNormalizer`(R1~R9, `date_relation` 계산), `MediaProperties`, 스키마 `data/schema/media.schema.json` 과 테스트 도우미 `DataSchemas`. 이름은 M3a3 머지 코드를 따른다(M3a3 T2 보고, 4d8929c).

### M3a3a (공용 규칙 — 선행 기능)

촬영 시점 파서, 라이선스 상태 판정, 차단·제외 필터와 그 입력인 "원문 정보" 형(`source`, `itemId`, `author`, `credit`, `channel` — 이름은 예시), `MediaProperties` 의 `media.blocklist`·`media.excluded-credits`. 이름은 M3a3a 머지 코드를 따른다(M3a3a T3 (8) 공개 부품 목록). 이 기능의 Commons 파일 매퍼는 원문 정보를 `source: commons`, `itemId` = 파일 이름(`File:` 없이), `author` = `Artist` 평문, `credit` = `Credit` 평문으로 채워 **정규화 앞에서** 필터를 부른다.

## Commons 사진 규칙

| 검사 | 결과 |
|---|---|
| Commons에 없음(Commons API 응답의 `missing` — 위키 로컬 파일) / `NonFree` | 제외 |
| 차단 목록·통신사·언론사 표기 (M3a3a 규칙) | 제외 |
| MIME이 `image/jpeg` 가 아님 | 제외 (로고·지도·국기 거르기. PNG 사진도 빠진다 — 리스크 5) |
| `LicenseShortName` | M3a3a "라이선스 상태"로 `free`/`other`, 없으면 `license: null`·`unknown` |
| `Restrictions` 있음 | 표시(거르지 않음) |
| `DateTimeOriginal` | M3a3a 파서로 읽음. 못 읽으면 `captured_at`·정밀도 null, `unknown` (그래도 표시, 그날 사진으로 분류하지 않음) |

사건 날짜(P585, 위키백과 줄 날짜)와 Commons 날짜 범주를 촬영 시점으로 옮기지 않는다.

필드: `type: photo`, `display_mode: image`, `id` = `commons-` + `pageid`, `url` = `imageinfo.url`, `thumbnail_url` = `thumburl`(`iiurlwidth=640`), `thumbnail_width`·`thumbnail_height` = `thumbwidth`·`thumbheight`, `source_name` = `Wikimedia Commons`, `source_url` = `descriptionurl`, `license_url` = `LicenseUrl`, `attribution` = `Artist`(평문, 없으면 `Credit`, 둘 다 없으면 "작가 정보 없음") + ` · ` + (`license` 또는 "라이선스 확인 안 됨") + ` · Wikimedia Commons`, `description` = `ImageDescription` 평문(한국어가 있으면 한국어, 없으면 받은 값, 200자에서 자름), 위치 = GPS(`GPSLatitude`·`GPSLongitude`)가 있으면 좌표, 없으면 `place_ref`(날짜 범주 사진은 `country:KR`, 사건 사진은 사건 `scope`: `domestic` → `country:KR`, `world` → `world`, `unknown` → `unknown`), `retrieved_at` = 응답을 받은 시각.

**Commons 파일 매퍼**는 MIME 거르기를 부르는 쪽이 정한다(이 기능은 `image/jpeg`, M3a4b는 동영상 MIME). 그 밖의 검사(`missing`, `NonFree`, 차단·제외 필터 호출, 작가·라이선스·설명·촬영 시점·위치)는 매퍼 안에 둔다.

### 고르는 법

| 자리 | 후보 | 개수·규칙 |
|---|---|---|
| `events[].media` | 응답에 남은 사건 중 `tone == NORMAL` 이고 `title`(대표 링크)이 있는 사건 → 한국어 위키백과 PageImages(`pilicense=free`) 파일 이름 → Commons 메타데이터 | 사건마다 사진 최대 1(M3a4부터 영상이 더해진다). `tragic` 사건(`default` 포함) 0, PageImages 요청에도 넣지 않음. `hidden` 사건은 요청에 넣지 않음. years의 절댓값 > `media.max-years-apart`(5)면 붙이지 않음(`unknown` 은 붙이고 "촬영 시점 미상"). 같은 파일이 두 사건에 나오면 앞 사건에만 |
| `day_media` | `Category:South Korea photographs taken on <날짜>` 의 파일(범주 순서) | 최대 `media.day-media-limit`(6). 사건 사진과 같은 파일 제외. 응답에 남은 사건 중 `tragic` 이 하나라도 있으면 0이고 날짜 범주 요청도 0. 사건이 0개인 날도 찾는다(#21 (a)). 순서: `same_day` 먼저(원래 순서), 그다음 그 밖의 `kind`, `unknown` 맨 뒤(M3-P17 (a)) |

요청 수(API 호출 1회): 위키백과 PageImages 최대 1(대상 사건이 없으면 0), Commons 최대 2(사건 파일 `imageinfo` 묶음 0~1, 날짜 범주 0~1). 날짜 범주 요청과 PageImages → Commons 요청은 동시에 보내도 된다(공용 `Semaphore(3)` 안).

### 실패와 시간

| 규칙 | 내용 |
|---|---|
| 시간 상한 | 미디어 조회 전체를 `media.lookup-deadline`(3초) 안에서 끝낸다. M3a2 `within` 과 같은 방식: 넘으면 `cancel(true)`, 최대 1초 취소 유예 뒤에도 안 끝났을 때만 WARN 1줄 |
| 호출별 읽기 타임아웃 | `WikimediaHttpClient.get(…, readTimeout)` 에 `media.lookup-deadline` 을 넘긴다(타임아웃 뒤 재시도는 사실상 없음) |
| 일부 실패 | Commons 실패·시간 초과 → 사건은 그대로, 그 부분 미디어만 비움, `partial: true`, 200. PageImages 실패 → `events[].media` 만 비고 `day_media` 는 그대로, `partial: true`. 차단·제외·규칙 위반으로 버린 미디어, `tragic` 으로 비운 미디어는 `partial` 이 아니다 |
| 503 | 바뀌지 않음 |
| 세계 섹션 최악 시간 | 위키백과 5초 + Wikidata 5초(+ 드물게 1초 유예) + 미디어 3초(+ 드물게 1초) ≈ 13초(최대 15초). 제품 정의서 8절(10초)을 넘을 수 있다 — M6b 측정, 리스크 3 |

### 로그

- `WikimediaHttpClient` 에 넘기는 `title` 은 이름표만: `commons day category`, `commons files of <개수>`, `kowiki pageimages of <개수>`. 날짜·범주 이름·파일 이름·문서 제목을 넣지 않는다(파일·범주 이름에 날짜가 들어간다).
- 미디어를 버린 이유는 DEBUG에 이유별 **개수만**. 사건을 가리킬 때는 M3a2처럼 ID 해시 8자리만.

## API / 데이터 변경

- `GET /api/story/world`: `events[].media` 와 `day_media` 에 M3a3 형식의 사진 레코드가 들어간다. `partial` 에 미디어 실패가 더해진다. 새 에러 코드 없음.
- 설정 `media.*` 추가: `max-years-apart: 5`, `day-media-limit: 6`, `lookup-deadline: 3s`(잘못된 값은 시작 실패). `wikimedia.commons-api-url: https://commons.wikimedia.org/w/api.php`.
- DB 변경 없음. 데이터 파일 없음(fixture는 테스트 리소스).

1988-09-17 응답 모양 (값은 녹화 fixture로 정해진다 — 아래는 모양만이고 JSON 그대로가 아니다):

```
{
  "date": "1988-09-17",
  "events": [
    {"id": "kowiki-1988-09-17-875c69fa", "title": "1988년 하계 올림픽", "tone": "normal", "tone_basis": "curated", "scope": "domestic",
     "media": [ 0~1장, M3a3 형식 ], … M3a2 필드 그대로},
    {"title": "KBS 1TV", "media": [ 0~1장 ], …},
    {"title": "KBS 2TV", "media": [ 0~1장 ], …}
  ],
  "day_media": [ 날짜 범주 사진, same_day 먼저 · unknown 맨 뒤 ],
  "notice": null,
  "partial": false
}
```

## 테스트 방침

- M3a1 "테스트 방침"을 따른다. 외부 네트워크를 부르지 않는다.
- 녹화 fixture: `fixtures/wikimedia/commons/`(데모 3날짜 날짜 범주 응답, 사건 파일 `imageinfo` 응답)와 `fixtures/wikimedia/kowiki/` 의 PageImages 응답(데모 사건 4개 제목). `README.md` 에 원 URL·녹화 일자·라이선스. **이미지 파일 자체는 내려받지 않는다**(메타데이터 JSON만).
- 합성 fixture로 규칙 표의 행마다 시험. 실제 비극 사건 이름을 쓰지 않는다. 표본 날짜는 공개 역사 사건 날짜·경계값(`CLAUDE.md` 7절 6항).
- 응답 미디어 전부를 `date_relation` 을 뺀 뒤 M3a3 스키마로 검증(`DataSchemas`).
- 날짜 기준 테스트는 M3a2와 같은 고정 시계(`2026-10-10`, `Asia/Seoul`).

## 데모 입력 3개로 확인할 것

사건·`tone` 은 M3a2 결과 그대로다. 사건 사진 값은 **T1 녹화 결과로 정하고 T3이 그 값으로 고정 단언**한다(PageImages 결과를 미리 알 수 없다. 리서치 기준 영어 위키백과 올림픽·월드컵 문서는 자유 대표 이미지가 없었다).

| 데모 입력 | 사건 (M3a2) | 확인 |
|---|---|---|
| 1988-09-17 + "서울" | 3개, 모두 `normal`/`curated`/`domestic`: ① `1988년 하계 올림픽`(`Q8470`) ② `KBS 1TV`(`Q777278`) ③ `KBS 2TV`(`Q624509`, 문장 원문 그대로) | PageImages 요청 **1회**에 세 제목이 모두 들어간다. 각 사건 `media` 는 0~1장이고 모두 `display_mode: image`, 스키마 통과, years 절댓값 5 이하 또는 `unknown`. 로고(SVG·PNG)·비자유 로컬 파일은 붙지 않는다(KBS 두 사건은 대표 이미지가 로고뿐이면 `[]` — 정상). `day_media` 에 날짜 범주 사진(리서치 기준 5장)이 **모두** `display_mode: image`. 한 날짜로 읽히는 파일은 `same_day`, 값이 둘인 파일도 빠지지 않고 `unknown`·정밀도 null·`captured_at` null이며 `same_day` 파일들 **뒤에** 온다. 사건 사진과 같은 파일은 `day_media` 에 없다. PD·CC BY-SA → `license_status: free`. `partial: false` |
| 2002-06-22 + "광주 서구 풍암동" | 1개: `2002년 FIFA 월드컵`(`Q47735`), `normal`/`curated` | PageImages 요청 1회(제목 1개). 사건 `media` 0~1장(T1 녹화로 확정). `day_media` 1장, `license` `KOGL Type 1`, `free`, `same_day`, `attribution` 이 `Artist` 평문(리서치 기준 "기증자")으로 시작하고 ` · KOGL Type 1 · Wikimedia Commons` 로 끝난다. `partial: false` |
| 1995-03-15 + "서울대학교병원" | 0개, `notice.code` = `NO_EVENTS_FOUND` | PageImages 요청 0, SPARQL 요청 0(M3a2 그대로), Commons 날짜 범주 요청 1 → `day_media` = `[]`(녹화). `partial: false` |
| (합성) `tragic` 날 | `tragic`(`default` 포함) 사건이 하나 있음 | 그 사건 `media` = `[]`, 다른 `normal` 사건은 사진 후보, `day_media` = `[]`, 날짜 범주 요청 0 |

## 크기 (400줄 규칙)

제품 코드(S1) 기준. 셈법은 M3a3 계획서와 같다(×2.8, 괄호는 ×2.4~×3.7). 메모(2026-10-13): M3a3 T2는 이전 방식의 약 3.9배(130 → 505)였다 — 같은 비율이면 이 기능은 약 780이다. 추정값은 그대로 두고 확인점에서 실측으로 본다.

| 작업 | 내용 | 이전 방식 | 다시 추정 |
|---|---|---|---|
| T1 | `CommonsProperties`, Commons 클라이언트(두 질의), Commons 파일 매퍼, PageImages | 약 120 | 약 335 (290~445) |
| T2 | 미디어 조회(사건·날 사진, `tragic`·`hidden`·±5년·중복·개수·순서), 시간 상한, 서비스 연결, `partial`, 옮겨 온 설정 3개 | 약 80 | 약 225 (190~295) |
| **합계** | | **약 200** | **약 560 (480~740)** |

- 400을 넘지만 PM 결정 A (a)로 한 PR이다.
- **중간 확인점: T1 뒤 누적 S1이 400을 넘으면** 오케스트레이터가 PM에게 알리고 계속할지 묻는다.
- 테스트(S2)는 따로 보고한다.

## 작업 목록

순서: T1 → **확인점** → T2 → T3 → T4. (이전 M3a3 번호: T5 → T1, T6 → T2, T7 → T3, T8 → T4)

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | Commons 클라이언트·파일 매퍼·PageImages, 녹화 fixture | backend-dev | `CommonsProperties`(`wikimedia.commons-api-url`), Commons 클라이언트(파일 제목 묶음 `imageinfo`, 날짜 범주 `generator=categorymembers`), Commons 파일 매퍼(메타데이터 → `MediaItem` 후보, MIME 거르기는 부르는 쪽이 정함, M3a3a 파서·라이선스 판정·차단·제외 필터 호출), 위키백과 PageImages 조회(`wikimedia.kowiki-api-url`), 녹화 fixture + `README.md`, 테스트 | **M3a3a 머지** (M3a3 포함) | (1) `./gradlew check` 성공. (2) "Commons 사진 규칙" 표의 행마다 합성 테스트: `NonFree`·Commons에 없음(`missing`)·비JPEG → 제외, **차단 목록에 넣은 파일 이름(합성) → 제외, `Credit` 이 `AP Photo/…` → 제외(M3a3a 필터를 거침)**, 허용 라이선스 → `free`, 다른 이름 → `other`, 라이선스 없음 → 표시 + `unknown`, `Restrictions` 있음 → 표시, `Artist` 없음 → `Credit`, 둘 다 없음 → "작가 정보 없음", `Artist` HTML → 평문, 설명 200자 자름, GPS 있음 → 좌표·`place_ref` null, 없음 → `place_ref`. (3) 모든 요청이 `WikimediaHttpClient` 로 나가고(새 HTTP 클라이언트 없음) `User-Agent` 헤더가 있으며, 호출별 읽기 타임아웃으로 `media.lookup-deadline` 을 넘긴다. (4) PageImages 요청은 제목 묶음 1회(`pilicense=free`, 최대 50개). (5) WireMock 503 → WARN 줄의 `title` 이 이름표(`commons day category`, `commons files of N`, `kowiki pageimages of N`)이고, 로그 어디에도 `YYYY-MM-DD` 모양·`File:`·`Category:`·문서 제목이 없다. (6) 녹화 fixture: 데모 3날짜 날짜 범주 응답, 데모 사건 4개 제목의 PageImages 응답, 돌려받은 파일의 `imageinfo` 응답. README에 원 URL·녹화 일자·라이선스. 이미지 파일은 받지 않는다. (7) 보고서에 데모 사건 4개의 PageImages 결과 표(제목, 파일 이름 또는 없음, MIME, 라이선스, 붙을지 여부)와 누적 S1 — **확인점 400**. 네트워크를 쓸 수 없으면 "막힘"으로 보고하고 PM이 실행할 `curl` 명령(User-Agent는 `$WIKIMEDIA_USER_AGENT`)을 적는다 |
| T2 | 세계 API 연결: 사건 사진·날 사진, `tragic`·`hidden`, 시간 상한, `partial` | backend-dev | 미디어 조회(어댑터 인터페이스 뒤 — 예외를 던지지 않음), `WorldStoryService`·`Event.of` 연결, 시간 상한(`within` 방식, 공용 도우미로 빼도 됨), `MediaProperties` 에 `media.max-years-apart`·`media.day-media-limit`·`media.lookup-deadline`, 테스트 | T1 (확인점 통과) | (1) "데모" 표 4행 통과(사건 사진 값은 T1 녹화 결과로 고정). (2) 데모 3날짜 응답의 모든 미디어가 `date_relation` 을 뺀 뒤 스키마 통과. (3) 합성: ±6년 사건 사진 → 안 붙음, ±5년 → 붙음, 촬영 시점 미상 사건 사진 → 붙고 `unknown`, **촬영 시점을 못 읽은 날짜 범주 파일 → `day_media` 에 남고 `captured_at` null·`kind` `unknown`(날짜 범주의 날짜가 들어가지 않음)·`same_day` 파일 뒤**. (4) `tragic`: `Verdict.tone() == TRAGIC` 인 사건(`tone_basis` `default` 포함) → `media` = `[]` 이고 PageImages 요청 제목에 없음. 응답에 남은 사건 중 `tragic` 이 있으면 `day_media` = `[]`, 날짜 범주 요청 0. (5) `hidden`: `hide` 로 내린 사건의 제목은 PageImages 요청에 없고, 그 사건은 `tragic` 날 판단에 들어가지 않는다. (6) 범주 파일 8개 → `day_media` 6개, 중복 없음, 사건 사진과 같은 파일은 `day_media` 에 없음, 같은 파일이 두 사건에 나오면 앞 사건에만. (7) Commons 503 → 200, 사건 그대로, `partial: true`. PageImages 503 → `events[].media` 비고 `day_media` 는 채워짐, `partial: true`. 두 위키백과 문서 `UNAVAILABLE` → 여전히 503 `UPSTREAM_UNAVAILABLE`(problem+json, 미디어 요청 0). (8) 요청 수: PageImages ≤ 1, Commons ≤ 2(API 호출 1회당). 1995-03-15는 PageImages 0·Commons 1. (9) 시간 상한: 응답하지 않는 Commons 스텁 → `media.lookup-deadline` + 1초 + 여유 안에 200, `partial: true`, WARN 1줄. (10) 응답 어디에도 `display_mode: link_only` 가 없다. (11) 로그에 날짜·파일 이름·전체 사건 ID가 없다(ID 해시 8자리만). (12) 설정 잘못된 값(`max-years-apart` 음수, `day-media-limit` 음수, `lookup-deadline` 0 이하) → 시작 실패. (13) 보고서에 누적 S1 |
| T3 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T2 | (1) T1·T2 기준 표. (2) `./gradlew check` 통과, 3회 반복 안정. (3) S1·S2·S3 보고. (4) 데모 3날짜 미디어 표(자리, 사건, 순서, `display_mode`, `license`, `license_status`, `captured_at`, `date_relation`). (5) **최종 서버 미디어 DTO 필드 목록(이름·형·null 가능)** — M3b2 T1 대조용(M3a3 T4 목록과 달라졌으면 차이를 적는다). (6) 스텁으로 잰 세계 섹션 최악 시간(위키백과·Wikidata·Commons 모두 상한까지 늦을 때) |
| T4 | 실제 호출 확인 (PR 검수 때) | PM | - | T3 | (1) `docker compose config --quiet` 종료 코드 0, `docker compose up -d db`, `cd server && ./gradlew bootRun` 이 뜬다. (2) P1~P3 결과가 데모 표와 맞는다. (3) `thumbnail_url` 하나를 브라우저로 열어 이미지가 보이고 `source_url` 페이지의 라이선스가 `license` 와 같다. (4) `date_relation.kind` 가 `unknown` 인 항목의 `captured_at` 이 `null` 이다. (5) 사건 사진이 사건과 너무 엉뚱하면 PR 코멘트로 알린다(리스크 4) |

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
  | jq '{partial, events: [.events[] | {title, tone, media: [.media[] | {display_mode, license, license_status, captured_at, date_relation, thumbnail_url}]}], day_media: [.day_media[] | {display_mode, license, license_status, captured_at, date_relation, thumbnail_url}]}'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' \
  | jq '{events: [.events[] | {title, media}], day_media: [.day_media[] | {display_mode, license, attribution, date_relation}]}'
curl -s 'http://localhost:8080/api/story/world?date=1995-03-15' | jq '{events, day_media, notice, partial}'
```

담당별: backend-dev 2 (T1, T2), test-engineer 1 (T3), PM 1 (T4). 모두 4개.

## 리스크와 확인할 것

1. **저작권 (PM이 알고 정함, 2026-10-11)** — 라이선스가 확인되지 않은 사진을 직접 보여 준다. 비상업·로컬 데모, 복사 없이 핫링크만, 출처·라이선스 상태 표시, 통신사·언론사 직접 표시 금지, 차단 목록으로 위험을 줄인다. 공개 배포 전에 다시 본다(로드맵 리스크 4).
2. **핫링크** — Commons는 권장하지 않는다. 로컬 데모라 요청이 적다. 브라우저는 `referrerpolicy="no-referrer"`(M3b1b).
3. **세계 섹션 시간** — 위키백과 5초 + Wikidata 5초(+1초 유예) + 미디어 3초(+1초 유예)로 최악 약 13~15초. 제품 정의서 8절(10초)을 넘을 수 있다. 공용 `Semaphore(3)` 도 줄을 세운다. M6b에서 재고, 넘으면 섹션 전체 상한(로드맵 후속 후보)을 제안한다. T3 (6)에 스텁 측정값을 남긴다.
4. **사건 대표 이미지가 사건과 어긋날 수 있다** — 특히 1988의 KBS 두 줄은 원문 그대로 나가므로(M3-P16) 그 문서의 대표 이미지(로고가 아니면 방송국 건물 등)가 붙을 수 있다. `date_relation` 으로 정직하게 보이고 ±5년을 넘으면 붙이지 않는다. 촬영 시점 미상 이미지는 연도 거르기가 안 되므로 "촬영 시점 미상"으로만 보인다. 너무 엉뚱하면 사건 미디어에서 `unknown` 을 빼는 규칙을 후속으로 정할 수 있다.
5. **JPEG만** — PNG 사진도 빠진다.
6. **보수적 `tragic` 이 사진을 지운다** — Wikidata가 실패하거나 QID가 없는 날(큐레이션 밖 사건이 `default` → `tragic`)에는 사건 사진도 날 사진도 없어진다. 의도된 방향(6.3절)이다. `hide` 로 내린 사건은 `tragic` 날 판단에 넣지 않는다(Wikidata를 묻지 않아 판정이 늘 `default` 라서). 실제 비극 줄을 권리자 요청으로 내린 드문 경우에는 그날 사진이 보일 수 있다 — 그때는 PM이 날짜 범주 사진을 차단 목록에 함께 넣는다.
7. **요청마다 Commons를 부른다** — 사진 메타데이터 캐시가 없다. 로컬 데모라 분당 200 제한에 닿지 않는다. 필요하면 M3a4b 결과 캐시에 사진 출처를 더한다(후속).
8. **M3a4와 같은 연결 자리** — M3a4가 이 기능과 병렬로 진행되면 둘 다 `WorldStoryService` 의 미디어 연결을 고친다. worktree를 나누고 오케스트레이터가 합친다. M3a4b는 이 기능의 Commons 파일 매퍼를 쓰므로 이 기능 머지 뒤다.
9. **로그로 날짜를 짐작할 수 있음** — 이 기능은 이름표만 넘기지만, M3a1 위키백과 호출의 WARN 줄은 지금도 문서 제목(`9월 17일`, `1988년`)을 찍는다. 장소가 요청에 들어오는 M2에서 다시 본다(로드맵 후속 후보).
10. **크기 추정이 낮을 수 있음** — M3a3 T2가 이전 방식의 약 3.9배였다. 같은 비율이면 약 780이다. T1 뒤 확인점(400)에서 실측으로 판단한다.

## PM 결정

| # | 결정 | 상태 |
|---|---|---|
| M3a3 분할 A | (a) 두 PR — 이 기능이 2부 | **결정 2026-10-13** |
| B | (a) 스키마 검증 라이브러리는 테스트 범위 유지(M4a·M5b 때 다시) — 이 기능은 테스트에서만 `DataSchemas` 를 쓴다 | **결정 2026-10-13** |
| C | (c) `chore/env-guard` 하지 않음 — T4 (1)에서 PM이 `.env` 를 직접 확인한다 | **결정 2026-10-13** |
| M3a3 확인점 | (a) M3a3 T3을 M3a3a로 — 이 기능의 선행이 M3a3a로 바뀜 | **결정 2026-10-13** |

새로 묻는 것 없음(확인점에서만 다시 묻는다).
