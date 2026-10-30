# 사건 출처를 영어 위키백과로 (C1 나라 전환 — S3)

> 브랜치: S3a `feat/enwiki-events` → S3b `feat/enwiki-canada-events` → S3c `feat/enwiki-media-scope` (모두 그 시점의 `main` 에서) · 작성: planner (2026-10-29) · 상태: **PM 승인 (2026-10-29)**
> 근거: `docs/plans/canada-research.md` "지금 시작할 것" S3와 PM 결정(2026-10-29, 특히 4 (b): 한국 전용 코드는 캐나다 PR 안에서 그 자리에서 바꾸거나 지운다), [canada-day-events.md](../research/canada-day-events.md) 7절(파서 변경 목록)·10절(D1~D5), 머지 코드(`main` 49248e9의 `server/`).
> 연구 계획은 S3a(문서·파서)·S3b(범위·tone·미디어) 두 PR을 제안했다. 크기를 다시 세니 **세 PR이 맞다**(아래 "크기", 결정 D6). 세 PR을 이 파일 하나에 둔다.

## 목표

1. 그날의 사건을 한국어 위키백과 대신 **영어 위키백과 날짜 문서 + 연도 문서 + "YYYY in Canada"** 에서 읽는다(D1 (b)). 응답 모양(`GET /api/story/world`)은 그대로 두고 값과 필드 두 개만 바뀐다.
2. 줄 형식의 차이를 처리한다: `Events`(+ Canada 문서는 `Sport`) 절, `prop=sections` → `prop=tocdata`, 링크 없는 연도·날짜 키, 범위 줄, 월만 있는 줄, Events/Sport 이중 기재.
3. 정직한 날짜(제품 정의서 6.1절)를 지킨다. 그날이 아닌 줄(월만, 날짜 없음, 기간 중간)은 넣지 않는다. **범위 줄은 "그날 시작"** 으로만 보인다(D2 (a), T3 발견).
4. 범위(`scope`)는 캐나다(Q16) 기준, `tone` 은 영어 규칙으로 바꾼다. 미디어(Commons 날짜 범주, 장소, YouTube 지역·채널)를 캐나다로 옮기고, 한국 전용 코드·자료를 지운다(결정 4 (b)).

## 범위 밖

- 화면 글자 영어화(S1 `feat/english-ui`, 다른 계획). 이 계획의 웹 작업은 S1 머지 뒤 "Started that day" 표시·타입·방침의 출처 문장뿐이다. `CommonsFileMapper` 의 "작가 정보 없음"·"라이선스 확인 안 됨"도 S1 몫이다.
- 캐나다 YouTube 채널 넣기(S4, T4 리서치 뒤). 이 계획은 KTV 채널을 빼고 "허용 채널이 비면 YouTube 꺼짐"까지만 한다.
- Canada 문서의 `Arts and literature` 절, On this day(보조로도 쓰지 않음), 국내/세계 사건을 화면에서 나눠 보이기, 프랑스어.
- 위키텍스트 템플릿(`{{...}}`)이 든 줄을 살리기. 지금처럼 건너뛰고 개수만 보고한다(D5 (a): 원문을 고치지 않음).
- `data/schema/examples/media/` 의 한국 예시 파일(규칙 시험용 예시라 출처와 무관). 다음 정리 때.
- 사건 순서 규칙 변경. 지금 그대로 `normal` 먼저, 그 안은 문서 순서(날짜 → 연도 → Canada).

## 사용할 외부 출처

| 출처 | 확신도 | 근거 | 이 계획에서 |
|---|---|---|---|
| 영어 위키백과 날짜·연도 문서 (Action API `parse`, `tocdata`) | 확인됨 | [3절 카드 1](../research/canada-day-events.md#3-출처-카드) | S3a. CC BY-SA 4.0, 문서 링크 + 라이선스 표시, 캐시 허용(지금 `cache-ttl` 7일 그대로), 분당 200회·동시 3개(지금 공용 클라이언트 그대로) |
| 영어 위키백과 "YYYY in Canada" | 확인됨 | 같은 3절 카드 2 | S3b. 조건 같음 |
| Wikidata (주 링크 QID의 P17·분류·사망자, `VALUES` 질의) | 확인됨 | 같은 3절 카드 4 | S3a. 지금 질의 그대로, 나라만 Q884 → Q16. 날짜 SPARQL(5~44초)은 쓰지 않는다 |
| 영어 위키백과 PageImages `pilicense=free` + Commons 파일 | 확인됨 | 같은 3절 카드 3 | S3a에서 주소만 따라 바뀐다(코드 같음). 영어판 로컬 비자유 파일은 Commons 조회에서 빠진다(`MISSING`) |
| Commons `Canada photographs taken on <날짜>` | 확인됨 (2005년 이전은 거의 빔) | 같은 3절 카드 3 | S3c. 통신사·`PD US no notice` 처리 D3 |
| YouTube Data API | 확인됨 (`video-sources.md`) | — | S3c. 허용 채널 없음 → 꺼짐. 캐나다 채널은 T4 리서치·S4 |

T3 2절 5행(세 문서 겹침 판정)만 "부분 확인 — 구현 때 재측정"이다. 새 출처가 아니라 우리 중복 규칙의 정확도 문제라 리서치 작업을 넣지 않고, S3a T4·S3b T4의 **멈춤 기준**으로 다룬다. 나머지는 모두 "확인됨"이라 data-researcher 작업이 없다.

## 머지 코드: 다시 쓰는 것·바꾸는 것·지우는 것

`server/src/main/java/io/github/ericmschoi/theworldthatday/` 기준. "이름"은 D8 (a)로 S3c T4에서 한 번에 바꾼다(그 전에는 `KoWiki*` 이름에 영어 동작이 든다).

| 부품 | S3a | S3b | S3c |
|---|---|---|---|
| `wikimedia/` `WikimediaHttpClient`·`HostRateLimiter`·`Deadline`·`ActionApiErrors`·`WikimediaResponse`·`CommonsProperties`·`WikidataProperties` | 그대로 | 그대로 | 그대로 |
| `WikimediaProperties` (`kowiki-api-url`, `kowiki-day-deadline`) | 값만 `https://en.wikipedia.org/w/api.php` | 마감 주석(문서 3개) | 이름 → `enwiki-api-url`, `wiki-day-deadline` |
| `event/kowiki/KoWikiPageKind` | 바꿈: `February 13`, `1988` | `CANADA` (`1988 in Canada`) 추가 | 이름 |
| `ActionApiKoWikiClient` | 바꿈: `prop=tocdata\|revid`, `hLevel` 2 `Events` 한 절, "사건"·"문화" 지움, 로그 `source=enwiki` | Canada 문서는 `Events`+`Sport` | 이름 |
| `KoWikiPage`·`CachingKoWikiClient`·`WikiSectionCache`·`WikiSectionCacheRepository`, V2 표 | 그대로(둘째 칸은 늘 `null`) | ★ V4: `culture_wikitext` → `sport_wikitext`, 한국 행 삭제 | 이름 |
| `WikitextEventParser` | 바꿈: 날짜·연도 문서 키, 범위 줄, `&ndash;` | Canada 줄(링크 없는 날짜, 범위, 월만) | 그대로 |
| `TwoPageKoWikiEventSource` | 바꿈: 한 절, `date_kind`·`range_end`, ID `enwiki-` | 문서 3개, 멈춤 규칙 일반화 | 이름 → `DayPagesWikiEventSource` |
| `KoWikiEvent`·`KoWikiDayResult`·`KoWikiDocStatus`·`KoWikiEventSource`·`KoWikiClient`·`KoWikiDedupProperties` | `KoWikiEvent` 에 `dateKind`·`rangeEnd` | 주석 | 이름 |
| `KoWikiEventDeduplicator`, `kowiki-generic-links.txt` | 바꿈: 문서 링크 `1988`·`February 13`, `titleKey` 의 `년` 규칙 지움, 목록 영어로 다시 만듦 | 목록에 Canada 문서 링크 추가, D4와 목록 공유 | 이름(파일 `wiki-generic-links.txt`) |
| `KoWikiLinkResolver`·`ActionApiKoWikiLinkResolver` | 그대로(주소만 따라감) | 그대로 | 이름 |
| `event/tone/CuratedEventToneJudge`, `tone-keywords.txt` | 바꿈: 영어 낱말 일치(대소문자 무시, 낱말 단위), Q884 → Q16 | Canada 문서 줄 → `domestic`, D4 숨김 | 그대로 |
| `EventCuration`·`EventToneJudge`·`event/wikidata/*` | 그대로 | 그대로 | 그대로 |
| `story/world/WorldStoryResponse`·`WorldStoryService` | 바꿈: 상수(아래 계약), `country:KR` → `country:CA` | D4 숨김 개수 DEBUG | 그대로 |
| `media/pageimages/ActionApiKoWikiPageImages` | 코드 그대로, 주소가 영어판이 됨 | 그대로 | 이름 → `ActionApiWikiPageImages` |
| `media/commons/ActionApiCommonsClient`·`CommonsMediaLookup` | 그대로(그날 사진은 D7로 꺼 둠) | 그대로 | 범주 `Canada photographs taken on`, `PLACE` `country:CA` |
| `CommonsFileMapper` | 그대로 | 그대로 | D3 `PD US no notice` 빼기(S1의 문자열 변경 뒤) |
| `MediaFilter`·`MediaNormalizer`·`CaptureTimeParser`·`TextDates`·`DateRelation`·Commons 영상·`VideoSearch`·`VideoAccuracy`·결과 캐시 | 그대로 | 그대로 | 그대로(`excluded-credits` 값만) |
| `media/youtube/*` | 그대로(영상은 D7로 꺼 둠) | 그대로 | `regionCode` CA·`relevanceLanguage` en, 지역 검사 KR → CA, 대한뉴스·컬러 복원 패턴 지움, 빈 허용 채널 = 꺼짐, KTV 기본값 지움 |
| `data/curation/event-tone.json` | 한국 항목 4개 지우고 캐나다 데모 항목 | Canada 문서 항목 추가 | 그대로 |
| `data/verification/event-media-expectations.json` | 그대로 | 그대로 | 캐나다 날짜로 다시 씀 |
| 시험 fixture `fixtures/wikimedia/kowiki/`·`commons/` 한국 녹화본 | 대체한 시험이 안 쓰는 것 지움 | 같음 | 같음 |

## API / 데이터 변경

### `GET /api/story/world?date=YYYY-MM-DD` (모양 그대로, 필드 2개 추가)

값이 바뀌는 필드: `summary_lang` `"en"`, `source_name` `"English Wikipedia"`, `source_url`·`related_links[].url` `https://en.wikipedia.org/wiki/<제목>`, `text_license_url` `https://creativecommons.org/licenses/by-sa/4.0/deed.en`, `source_section` `"Events"` | `"Sport"`, `id` `enwiki-<date>-<hash 8자>`. `summary` 는 원문 줄 그대로(링크 기호·굵게·각주만 뺌, `summary_modified` false — D5 (a), 지금 파서 그대로).

새 필드(S3a):
- `date_kind`: `"day"`(그날 줄) | `"range_start"`(그날 시작하는 기간 줄)
- `range_end`: `range_start` 일 때 끝 날짜 `YYYY-MM-DD`, 그 밖에는 `null`

```json
{
  "id": "enwiki-1988-02-13-1a2b3c4d",
  "date": "1988-02-13",
  "date_kind": "range_start",
  "range_end": "1988-02-28",
  "scope": "domestic",
  "title": "1988 Winter Olympics",
  "summary": "<연도 문서 원문 줄, 날짜 키 뒤 문장 그대로>",
  "summary_lang": "en",
  "summary_modified": false,
  "text_license": "CC BY-SA 4.0",
  "text_license_url": "https://creativecommons.org/licenses/by-sa/4.0/deed.en",
  "source_name": "English Wikipedia",
  "source_title": "1988",
  "source_url": "https://en.wikipedia.org/wiki/1988",
  "source_section": "Events",
  "source_revision": 0,
  "wikidata_qid": "Q9658",
  "tone": "normal",
  "tone_basis": "rule:no-signal",
  "related_links": [{"title": "1988 Winter Olympics", "url": "https://en.wikipedia.org/wiki/1988_Winter_Olympics"}],
  "media": [],
  "retrieved_at": "2026-10-30T00:00:00Z"
}
```
(`source_revision` 은 녹화 판 번호, `tone_basis` 는 Wikidata 신호에 따라 다름 — 모양 예시다.)

화면(S3a T6): `date_kind` 가 `range_start` 면 사건 문장 옆에 **"Started that day — ran until February 28"**(en-CA `Intl`, 월 이름 + 일, 결정 9 (a))를 붙인다. `day`·없음·모르는 값이면 아무것도 붙이지 않는다. 문장 자체는 바꾸지 않는다.

### 파서 규칙 (그날 줄만, 나머지는 건너뜀 개수에)

| 문서 | 줄 | 결과 |
|---|---|---|
| 날짜 `February 13` (`Events` 절, 하위 절 포함) | `*[[1913]] &ndash; 문장`, 같은 해 둘째 줄 `*  1945 &ndash; 문장`(링크 없는 연도) | 연도 = 요청 연도면 `day` |
| 연도 `1988` (`Events` 절, 달 하위 절) | `* [[February 12]] – 문장`, `* [[July 17]]` + `** 문장` 하위 줄 | 날짜 = 요청 날짜면 `day` |
| 연도 | `* [[February 13]]–[[February 28\|28]] – 문장` (달을 넘는 `[[July 30]] – [[August 2]]` 포함) | **시작일 = 요청 날짜일 때만** `range_start` + `range_end`. 기간 중간 날짜는 넣지 않는다. 끝이 시작보다 앞이면 다음 해. 끝을 못 읽으면 건너뜀. 범위 머리줄의 하위 줄도 `range_start` |
| Canada `1988 in Canada` (`Events`·`Sport`) — S3b | `*January 28 – 문장`(링크 없음), `*February 12` + `**` 하위 줄, `*February 13–28 – 문장`, `*February 28 – March 3 – 문장` | 위와 같은 규칙 |
| 모든 문서 | 월만 있는 줄, 날짜 없는 줄("Full date unknown" 절 포함), 템플릿·외부 링크·HTML 태그가 든 줄 | 넣지 않는다(정직한 날짜, D5) |

구분 기호는 `-`·`–`·`—`·`&ndash;`·`&mdash;` 를 모두 받는다.

### DB (S3b, ★)

`V4__wiki_section_sport.sql`: `culture_wikitext` → `sport_wikitext` 로 이름 바꾸고, `site = 'ko.wikipedia.org'` 행을 지운다(아무도 읽지 않는 한국 본문). 열 주석을 영어 문서 기준으로 고친다. V2 파일은 그대로 둔다.

### 설정·자료

- S3a `application.yml`: `kowiki-api-url` 값, **D7 (a)** `media.day-media-limit: 0`, `media.video.max-events: 0`(주석: S3c에서 캐나다 출처로 다시 켬). 시험은 자기 설정으로 켠다.
- S3c `application.yml`: 두 값 되돌림(6, 3), `youtube.allowed-channels: []`, 영상 낱말 목록의 한국어 항목과 `retrospective-title-patterns` 한국어 기본값 지움, `excluded-credits` 의 한국 통신사(연합뉴스, Yonhap, 뉴시스, Newsis) → `The Canadian Press`, `Canadian Press` (AP·Reuters·AFP·Getty·EPA 그대로).
- `tone-keywords.txt`(S3a): T3 2절 7행 제안 목록 + `coup`, 낱말 형태를 모두 적음(`kill`, `kills`, `killed`, `killing` …). 오탐("World War II")은 지금 규칙처럼 보수적 쪽(`tragic`)으로 받아들인다.
- `kowiki-generic-links.txt`(S3a, S3b 추가): 녹화한 절에 나온 나라·주·도시 링크 전부(M3a1c와 같은 방법).

## 작업 목록

크기는 S1 = `server` 제품 코드·설정 추가 줄(시험 제외, M3a5a와 같은 명령). 각 backend-dev 보고서는 누적 S1을 적는다.

```
# S1
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# P1 — PM 확인 (서버 실행 중, .env 에 저장소 주소 WIKIMEDIA_USER_AGENT)
for d in 1988-02-13 1976-07-17 2010-02-12 1980-04-12 1969-07-20; do echo "== $d"; curl -s "http://localhost:8080/api/story/world?date=$d" | jq -c '{partial, events: [.events[] | {source_title, source_section, date_kind, range_end, tone, scope, summary}], day_media: [.day_media[] | {source_url, license, captured_at, date_precision}]}'; done
```

**시험 날짜**: 결정 1 (b) 데모 4개(1988-02-13, 1976-07-17, 2010-02-12, 1980-04-12)와 영상 표본 1969-07-20, 경계값 2000-02-29(합성 fixture만)뿐(D9). 합성 문장은 "Test event A" 같은 중립 문장. 다른 날짜를 찾아 기능을 보이지 않는다. 기존 시험 중 **한국어 동작을 시험하는 것**만 영어 동작 시험으로 바꾸고, 한국 날짜를 단순한 날짜로 쓰는 시험(공개 역사 사건 날짜, M3-P13)은 그대로 둔다. 시험을 지우면 반드시 대체 시험을 짝지어 보고한다(`CLAUDE.md` 5절).

### S3a `feat/enwiki-events` — 날짜·연도 문서를 영어로

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | 영어 날짜·연도 문서 녹화 fixture (`test(fixtures)`) | backend-dev | - | (1) `fixtures/wikimedia/enwiki/` 에 날짜 문서 `February_13`·`July_17`·`February_12`·`April_12`·`July_20`, 연도 문서 `1988`·`1976`·`2010`·`1980`·`1969` 의 `prop=tocdata\|revid` 응답과 `Events` 절 `prop=wikitext&section=<번호>&oldid=<revid>` 응답, 날짜마다 링크 해석(`pageprops`)·대표 이미지(`pageimages`) 응답과 그 이미지 파일의 Commons `imageinfo` 응답(지금 `files` 요청), 주 링크 QID의 Wikidata 신호 응답(지금 `SparqlWikidataSignalSource` 질의). 바이트 수정 없음, 미디어 파일 자체는 받지 않음. (2) README에 파일마다 녹화 일시, 원 URL, 판 번호, 라이선스(텍스트 CC BY-SA 4.0, Wikidata CC0, 파일 라이선스는 응답 안), User-Agent(저장소 주소만). (3) 무결성 시험(`KoWikiFixtureIntegrityTest` 방식)이 README 표와 파일을 대조해 통과. (4) `grep -rnE '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}' server/src/test/resources/fixtures/wikimedia/enwiki` 출력 없음. S1 0 |
| T2 | 문서 읽기: 제목·`tocdata`·`Events` 한 절 (`feat(events)`) | backend-dev | T1 | (1) `KoWikiPageKind` 제목: 2000-02-29 → `February 29`, 1988-02-13 → `February 13`·`1988`(앞자리 0 없음). (2) WireMock(T1 fixture): 문서마다 요청 2개 — `prop=tocdata%7Crevid`(+`redirects=1`), 그 뒤 `oldid=<revid>&prop=wikitext&section=<Events 번호>`. 어떤 요청에도 `prop=sections` 가 없다. (3) `hLevel` 2인 `Events` 만 고른다: 합성 응답에서 `hLevel` 3 `Events` 나 `Pronunciation` 이 앞에 있어도(2010 형) 맞는 번호. `Events` 가 없으면 요청 1개에 본문 `null`. (4) `missingtitle` → `ABSENT`, `ratelimited` → 호스트 막힘(기존 시험을 영어 fixture로 옮겨 같은 단언). (5) 캐시 행의 `site` 가 `en.wikipedia.org`, 둘째 칸 `null`. (6) 실패 로그는 `source=enwiki`, 날짜·본문 없음. (7) `./gradlew check` 성공 |
| T3 | 파서: 날짜·연도 문서 줄과 범위 줄 (`feat(events)`) | backend-dev | T2 | (1) T1 녹화본에서 그해 줄 수: 날짜 문서 1988-02-13 0, 1976-07-17 2, 2010-02-12 0, 1980-04-12 3, 1969-07-20 2 / 연도 문서 1, 2, 1, 2, 2 (T3 2절 표). (2) 1988-02-13·2010-02-12 연도 문서 올림픽 줄은 `range_start`, `range_end` 1988-02-28·2010-02-28. 나머지는 `day`. (3) 합성: 1988-02-14로 같은 범위 줄을 읽으면 0개(기간 중간). 달 넘는 범위, 12월→1월 범위(다음 해), 끝을 못 읽는 범위(건너뜀 +1), 범위 머리줄 + 하위 줄(하위 줄도 `range_start`), 링크 없는 둘째 연도 줄, `&ndash;`·`–`·`-` 구분 기호, 월만 있는 줄(0개). (4) 녹화 줄 하나의 `text` 가 원문에서 링크 기호만 뺀 문장과 글자 그대로 같다(D5 (a)). (5) 보고서에 문서별 건너뜀 줄 수(템플릿 등). (6) `./gradlew check` 성공. **확인점 1**: 누적 S1 280 넘으면 T4 전에 멈추고 PM에게 묻는다 |
| T4 | 출처·중복·`tone`·응답을 영어로 (`feat(events)`) | backend-dev | T3 | (1) 응답 필드가 위 계약과 같다(1988-02-13 올림픽 사건으로 키·값 단언, `date_kind`·`range_end` 포함). (2) 문서 링크 규칙: `1988`·`February 13` 링크는 중복 판정에서 빠진다. 일반 링크 목록이 영어로 다시 만들어졌다(만든 방법과 개수를 보고서에). (3) **1976-07-17 올림픽 개막이 응답에 1번, 1969-07-20 달 착륙이 1번**(날짜 문서 줄 + 연도 문서 줄). 지금 규칙(D2·D3, 겹침 0.6)으로 합쳐지지 않으면 기준값을 바꾸지 말고 멈추고 두 줄의 링크 목록을 "PM 결정 필요"에 적는다. (4) `tone`: 낱말 단위·대소문자 무시(`Killed` 맞음, `skilled` 안 맞음). 1980-04-12 항공기 추락 줄 `tragic`(`rule:keyword`). (5) `scope`: P17에 Q16 → `domestic`, 다른 나라만 → `world`. `domestic` 사건 사진의 `place_ref` `country:CA`. (6) 문서 둘 다 `UNAVAILABLE` → 503 `UPSTREAM_UNAVAILABLE`, 하나만 → 200 `partial` true. (7) D7: 기본 설정으로 세계 API를 부르면 Commons 날짜 범주 요청 0, 영상 요청 0. 사건 사진(PageImages → Commons 파일)은 그대로 나오고 6절 필드(`source_name`·`source_url`·`license` 또는 확인 안 됨·`attribution`·`captured_at`·`date_precision`)를 갖는다. 촬영 시점을 모르면 `date_precision` null("촬영 시점 미상"). 대표 사진의 촬영일이 그날이 아니면 그 날짜대로 표시하고 그날로 보이지 않는다(예: 테리 폭스 문서 대표 사진은 1980-07-12 촬영 — 붙는다면 그 날짜). 파일 복사 없음, `link_only` 없음. (8) 로그에 날짜와 사건 본문이 함께 없다(기존 개인정보 시험 통과). (9) `./gradlew check` 성공. **확인점 2**: 누적 S1 520 넘으면 알림 |
| T5 | 캐나다 데모 큐레이션 (`data(curation)`) | content-curator | T4 | (1) `data/curation/event-tone.json` 의 한국 항목 4개를 지운다. (2) 데모 4개 + 1969-07-20 의 응답 사건마다 규칙 결과(T4 보고서)를 Wikidata P17·위키백과 판으로 확인하고, 틀린 것만 항목을 둔다(예상: 테리 폭스 줄의 `scope` — 주 링크가 사람 항목이라 P17 없음). (3) 항목마다 `evidence_url` 은 T1 판 번호의 `oldid` 주소, `reviewed_at`, `reason`. (4) `data/schema/event-curation.schema.json` 검사(`EventCurationSchemaTest`) 통과. (5) 바꾼 항목 표(날짜, 줄, 규칙 결과 → 큐레이션 값, 근거)를 보고서에 |
| T6 | 화면: "Started that day"와 타입 (`feat(web)`) | frontend-dev | **S1 머지**(오케스트레이터가 `main` 을 이 브랜치에 merge 커밋으로), T4 | (1) `web/src/world/types.ts`: `summary_lang: 'en'`, `source_section: 'Events' \| 'Sport'`, `date_kind`, `range_end`. (2) `range_start` 사건은 일반 목록과 `tragic` 목록 모두에서 문장 밖에 "Started that day — ran until February 28" 이 보인다(Vitest + Testing Library). `day`·없음·모르는 값 → 표시 없음. 문장 `<p>` 의 글자는 그대로. (3) 목 fixture의 사건에 `date_kind` 를 더한다(합성 영어). (4) `web/privacy.html` 의 출처 문장에서 한국어 위키백과 → 영어 위키백과, 시행일은 머지하는 날(M3a5a T6 방식). (5) `cd web && npm run lint && npm run format:check && npm run typecheck && npm test` 성공 |
| T7 | 완료 기준 검증과 전체 시험 | test-engineer | T5, T6 | (1) T1~T6 기준마다 시험 이름 표. 빠진 기준은 시험을 더한다(제품 코드는 고치지 않음). (2) 데모 기대표(아래) S3a 열이 응답과 같다. (3) **지우거나 바꾼 기존 시험 대응표**(옛 시험 → 새 시험, 또는 "한국어 전용 동작, 결정 4 (b)"). 단언을 약하게 바꾼 것이 없다. (4) `./gradlew check` 2회, 마감·DB 실패 시험 클래스 3회. (5) 새 시험 모두 WireMock `findAllUnmatchedRequests` 빈 목록, 실제 위키 주소에 연결하지 않음. (6) S1·S2 파일별 보고 |
| T8 | PM 확인 (PR 검수 때) | PM | T7 | P1을 돌려 데모 기대표 S3a 열과 대조: 한국어 문장 없음, 범위 줄은 화면에 "Started that day", 그날이 아닌 줄 없음, 그날 사진·영상 없음(D7). 다른 날짜로 찾아보지 않는다 |

### S3b `feat/enwiki-canada-events` — "YYYY in Canada" 더하기

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | Canada 문서 녹화 fixture | backend-dev | S3a 머지 | `1988_in_Canada`·`1976_in_Canada`·`2010_in_Canada`·`1980_in_Canada`·`1969_in_Canada` 의 `tocdata` + `Events`·`Sport` 절, 새 링크의 `pageprops`, 새 QID의 Wikidata 신호. 기준은 S3a T1 (1)~(4)와 같다 |
| T2 | ★ 캐시 V4와 엔티티 (`feat(db)`) | **PM 또는 backend-dev (★ 결정)** | S3a 머지 | (1) Testcontainers에서 V1~V4가 차례로 적용되고 `ddl-auto: validate` 로 시작한다. (2) V3까지 적용한 DB에 `ko.wikipedia.org` 행 1개·`en.wikipedia.org` 행 1개를 넣고 V4 → 한국 행만 없고 영어 행의 둘째 본문이 `sport_wikitext` 에 있다. (3) `KoWikiPage.sportWikitext`, 엔티티·`upsert` 가 새 열을 쓴다. 캐시 시험(`CachingKoWikiClient*`)은 이름만 바뀌고 단언이 같다. (4) 마이그레이션 개수를 세는 시험은 V3 때처럼 +1. (5) `./gradlew check` 성공 |
| T3 | Canada 문서 읽기·줄 파서 (`feat(events)`) | backend-dev | T1 (파서), T2 (클라이언트·캐시 연결) | (1) 제목 `1988 in Canada`. 요청 3개(`tocdata`, `Events`, `Sport`), 같은 `oldid`. `Sport` 가 없으면 요청 2개, 본문 `null`. 한 절이라도 실패하면 문서 전체 `UNAVAILABLE`(지금 규칙). (2) T1 녹화본 그날 줄: 1988-02-13 `Sport` 1(`range_start`, 끝 02-28), 1976-07-17 `Events` 1 + `Sport` 1, 2010-02-12 `Events` 2(하위 줄) + `Sport` 1(`range_start`), 1980-04-12 1, 1969-07-20 0. (3) 합성: 링크 없는 `Month D –`, `Month D–D`·`Month D – Month D` 범위, 기간 중간 날짜 0개, 월만 있는 줄 0개, "Full date unknown" 절 줄 0개, 날짜 머리줄 + 하위 줄. (4) `./gradlew check` 성공. **확인점 1**: 누적 S1 250 넘으면 T4 전에 멈춤 |
| T4 | 세 문서 출처·국내 범위·D4 (`feat(events)`) | backend-dev | T3 | (1) 문서 순서 날짜 → 연도 → Canada, 중복 판정 절 순서 0 날짜 Events, 1 연도 Events, 2 Canada Events, 3 Canada Sport(같은 절끼리는 합치지 않는 D1 그대로). (2) **1976-07-17 올림픽 개막 4줄 → 1건, 1988-02-13·2010-02-12 올림픽 범위 줄(연도 + Canada Sport) → 1건, 1980-04-12 테리 폭스 3줄 → 1건.** 안 되면 S3a T4 (3)처럼 멈추고 보고. (3) 날짜 문서가 시간 초과로 실패하면 연도·Canada 문서는 캐시만, 연도 문서에서 실패하면 Canada 문서는 캐시만(멈춤 규칙 일반화). 전체가 `kowiki-day-deadline` + 취소 여유 안에 끝난다(DB가 안 닿을 때 기다림 3번 포함 — 시험으로 측정해 보고, 넘으면 값을 바꾸지 말고 보고). 세 문서 모두 `UNAVAILABLE` → 503. (4) Canada 문서 줄은 큐레이션이 없으면 `scope` `domestic`. (5) **D4 (b)**: `tragic` 이고 링크가 없거나 링크가 모두 일반 링크(나라·주·도시)·날짜·연도 문서뿐이면 응답에서 뺀다. `partial` 은 바꾸지 않고 DEBUG에 개수만. 합성: `[[Toronto]]` 만 단 `tragic` 줄 → 빠짐, 구체 링크가 있는 `tragic` 줄 → 보임, 링크 없는 `normal` 줄 → 보임. 2010-02-12 훈련 중 사망 줄의 결과(보임/빠짐)와 그에 따른 그날 사진 판단(보이는 `tragic` 이 있으면 S3c에서 그날 사진 없음)을 보고서에. (6) 일반 링크 목록에 Canada 녹화 절의 나라·주·도시를 더한다. (7) `./gradlew check` 성공. **확인점 2**: 누적 S1 480 넘으면 알림 |
| T5 | Canada 문서 큐레이션 | content-curator | T4 | S3a T5 (2)~(5)와 같은 방법으로 Canada 문서 줄을 확인한다 |
| T6 | 완료 기준 검증 | test-engineer | T5 | S3a T7 (1)~(6)과 같고, 데모 기대표 S3b 열 |
| T7 | PM 확인 | PM | T6 | P1, 데모 기대표 S3b 열. 구체 링크 없이 비극만 적은 줄(사인 이름만 있는 사건)이 보이지 않는다(D4) |

### S3c `feat/enwiki-media-scope` — 미디어 캐나다로, 한국 코드 정리

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | Commons 캐나다 녹화 fixture | backend-dev | S3b 머지, S1 머지 | `Category:Canada_photographs_taken_on_1976-07-17`(3장)·`…_2010-02-12`(4장) 범주 응답(`iiprop=url\|mime\|size\|extmetadata`, 지금 요청과 같음), 1988-02-13·1980-04-12 범주 없음 응답. README 기준은 S3a T1과 같다(파일별 라이선스는 응답 안). 파일 자체는 받지 않는다 |
| T2 | 그날 사진 캐나다·D3 (`feat(media)`) | backend-dev | T1 | (1) 범주 `Canada photographs taken on <날짜>`, GPS 없는 사진 `place_ref` `country:CA`, `media.day-media-limit` 6으로 되돌림. (2) **D3 (a)**: `extmetadata.Categories` 에 `PD US no notice` 가 있으면 사진·영상 모두 응답에 없다(`link_only` 로도 남기지 않음). 작가·크레딧에 통신사(`excluded-credits`, `The Canadian Press` 포함)가 있으면 지금처럼 없다. 1976-07-17: AP 2장 없음, CC BY-SA 4.0 1장 있음(작가·라이선스·`captured_at` 1976-07-17 `day`). 2010-02-12: 보이는 `tragic` 이 없으면 4장(라이선스 4종). 1988-02-13·1980-04-12: `day_media` 빈 목록, `partial` false. (3) 모든 사진이 6절 필드를 갖고, 출처가 비면 없다. 촬영 시점을 모르면 "촬영 시점 미상" 값(null), 그날 것처럼 표시하지 않음. 파일 복사 없음. DEBUG에는 버린 이유 개수만. (4) `./gradlew check` 성공 |
| T3 | 영상: KTV 빼기·빈 허용 채널 = 꺼짐·지역 CA (`feat(media)`) | backend-dev | T2 | (1) `youtube.allowed-channels: []` 로 시작한다. 그때 키가 있어도 YouTube 요청 0·캐시 조회 0, 시작 WARN 한 줄(키가 빌 때와 같은 방식). KTV 채널 ID가 코드·설정 기본값에 없다(`grep -rn UC8_LPVE4Yuc6KF0opF6uS_w server/src/main` 출력 없음). (2) 시험 설정의 합성 채널로: 검색 요청에 `regionCode=CA`, `relevanceLanguage=en`. `blocked: [CA]`·`allowed: [US]` → 버림, `allowed: [CA]` → 남음. (3) 대한뉴스·컬러 복원 패턴과 한국어 회고 기본값을 지운다. 그 기존 시험은 대응표에 "한국어 전용 동작, 결정 4 (b)". (4) `media.video.max-events` 3으로 되돌림. 1969-07-20 Commons 영상(P10, 기존 녹화본)이 그대로 붙는다. `tragic` 사건 영상 요청 0. (5) `./gradlew check` 성공. **확인점**: 누적 S1(T4 제외) 280 넘으면 알림 |
| T4 | 이름 정리 (`refactor(events)`, 기계적 커밋 하나) | backend-dev | T3 | (1) `event/kowiki` → `event/wiki`, `KoWiki*` → `Wiki*`, `TwoPageKoWikiEventSource` → `DayPagesWikiEventSource`, `KoWikiPageImages` → `WikiPageImages`, 설정 `wikimedia.enwiki-api-url`·`wikimedia.wiki-day-deadline`, `wiki-generic-links.txt`. (2) `grep -rniE 'kowiki\|KoWiki' server/src` 결과가 V2 마이그레이션 주석과 과거 계획 언급뿐. (3) 시험 수가 그대로이고 단언 변경이 이름뿐(`git diff -M --stat`). (4) `./gradlew check` 성공. S1을 따로 보고 |
| T5 | 검증 기대값 캐나다로 (`data(verification)`) | content-curator | T3 | `data/verification/event-media-expectations.json` 을 데모 4개 + 1969-07-20 으로 다시 쓴다. Commons 칸은 T1 녹화본·T3 리서치 근거 번호, YouTube 칸은 `no_request`(이유: 허용 채널 없음, S4). 개인정보 문구 유지. `EventMediaExpectationsTest` 구조 시험 통과(날짜 수 단언은 test-engineer가 T7에서 새 범위로) |
| T6 | 방침 문장 (`docs(privacy)`, 머지하는 날) | frontend-dev | T3 | `web/privacy.html` 의 YouTube 문장을 "지금은 채널 검색을 하지 않음"에 맞게 고치고 시행일을 머지하는 날로. 그 단언만 바꾸고 웹 검사 4개 성공 |
| T7 | 완료 기준 검증 | test-engineer | T4, T5, T6 | S3a T7 (1)~(6)과 같고, 데모 기대표 S3c 열 |
| T8 | PM 확인 | PM | T7 | P1, 데모 기대표 S3c 열. 화면 사진마다 출처·라이선스·촬영 시점 표시. 통신사 사진 없음 |

담당별(세 PR 합계 23개): backend-dev 12 (★ S3b T2 포함 — PM이 맡으면 11), content-curator 3, frontend-dev 2, test-engineer 3, PM 3 (★를 맡으면 4).

### 데모 기대표 (T3 리서치 2절·5절 근거, 녹화본으로 다시 확인)

| 날짜 | S3a (날짜 + 연도) | S3b (+ Canada) | S3c 그날 사진 |
|---|---|---|---|
| 1988-02-13 | 1건: 올림픽 `range_start`(~02-28), `domestic` | 1건 그대로(Canada Sport 줄 합쳐짐) | 범주 없음 → 0 |
| 1976-07-17 | 2건: 올림픽 개막(`domestic`), 동티모르 병합 | 2건 그대로(Canada 2줄 합쳐짐) | 1장(CC BY-SA 4.0). AP 2장 없음 |
| 2010-02-12 | 1건: 올림픽 `range_start`(~02-28) | 3건: + 성화 봉송 시위, + 훈련 중 사망(`tragic`, D4 결과 보고) | 보이는 `tragic` 이 있으면 0, 없으면 4장 |
| 1980-04-12 | 3건: 쿠데타 `tragic`, 항공기 추락 `tragic`, 테리 폭스 `normal` | 3건 그대로 | 범주 없음 → 0 |
| 1969-07-20 (표본) | 3건: 달 착륙(1건으로 합쳐짐), 온두라스–엘살바도르 휴전, 투르 드 프랑스 | 3건 그대로(Canada 줄 없음) | 범주 없음 → 0 |

`tone`·`scope` 칸에 적지 않은 것은 규칙 결과 + 큐레이션(T5)으로 정하고 T7이 고정한다.

## 크기 (400줄 규칙)

실측 비율 ×2.1~2.9(M3a4b ×2.36, M3a5 ×2.5, T2 ×2.9)를 코드에 곱하고, 낱말·링크 목록(자료 줄)과 기계적 이름 바꾸기는 ×1로 센다.

| PR | 파일별 셈 (코드) | 자료·기계적 | 기대 S1 (범위) |
|---|---|---|---|
| S3a | 제목 8 · `tocdata` 20 · 파서 50 · 출처 12 · 사건 필드 4 · 중복 5 · `tone`·범위 11 · 응답 10 · 설정 12 = 약 132 | 낱말 약 45 · 일반 링크 약 65 | **약 440 (390~490)** |
| S3b | 종류 8 · 클라이언트 10 · ★ V4·엔티티 23 · 파서 30 · 세 문서 25 · 국내·D4 31 · 설정 8 = 약 135 | 링크 추가 약 30 | **약 370 (315~420)** |
| S3c | 범주·장소 3 · D3 15 · 통신사 6 · YouTube 35 · 설정 12 = 약 71 | 이름 바꾸기 약 130 | **약 310 (280~340)**, 이름 제외 약 180 |

S3a는 400을 조금 넘을 수 있다. 넘는 몫은 대부분 낱말·링크 목록이라 리뷰 부담이 작다. 확인점(T3 뒤 280 멈춤, T4 뒤 520 알림)으로 다룬다.

## 리스크와 확인할 것

1. **중복 규칙이 영어 줄에서 덜 맞을 수 있다**(T3 2절 5행 "부분 확인"). 한 사건이 두 번 보일 수 있다. S3a T4·S3b T4에서 데모 사건마다 1건인지 시험하고, 안 되면 기준값을 몰래 바꾸지 않고 멈춘다.
2. **템플릿이 든 영어 줄이 많을 수 있다.** 지금 규칙은 그 줄을 건너뛴다(원문을 고치지 않기 위해). 사건이 빠질 수 있으니 문서별 건너뜀 수를 보고하고, 많으면 다음 단계로 PM에게 올린다.
3. **문서 3개 = 요청 최대 8개**(목차·절 7 + 링크 1)가 5초 마감 안에 들어가야 한다. 캐시(7일) 뒤에는 거의 0개. DB가 안 닿을 때 기다림이 2번 → 3번(각 1초). S3b T4가 측정해 보고한다.
4. **빈 날이 대부분이다.** 1950~2025년의 약 52%는 세 문서 모두 그날 줄이 없다(T3 커버리지 표). 빈 날은 비운 채 둔다(PM 방침). 데모 기대표 밖 날짜로 기능을 보이지 않는다.
5. **S3a~S3c 사이 `main` 의 미디어**: D7 (a)면 그날 사진·영상이 잠시 꺼진다. (b)면 한국 그날 사진과 KTV 검색(영어 제목, 쿼터 소모)이 섞인다.
6. **S1과 같은 파일**: `web/src/world/*`, `privacy.html`(S3a T6, S3c T6), `CommonsFileMapper`(S3c T2). 그래서 웹 작업과 S3c는 S1 머지 뒤에 한다. 서버 S3a·S3b 작업은 S1과 겹치지 않는다.
7. **D3과 연구 문서 문구**: T3 D3 (a)는 "표시하지 않는다(링크만)"이다. `CLAUDE.md` 7.1절은 사진을 링크로 보이지 않고, 지금 코드도 통신사 자료를 응답에서 뺀다. 이 계획은 "응답에서 뺌"으로 읽었다(결정 D3에서 확인).
8. **제품 정의서 12절이 아직 한국 데모다.** 시험 날짜의 근거가 결정 1 (b)뿐이다(D9).

## PM 결정 필요

> **PM 결정 (2026-10-29, "모두 추천대로 승인")**: D1 (b) · D2 (a) · D3 (a) 응답에서 뺌 · D4 (b) · D5 (a) · D6 (a) 세 PR · D7 (a) · D8 (a) · D9 (a). ★ S3b T2는 추천 (a) PM 직접 구현으로 기록하되, S3b 시작 전에 PM에게 다시 확인한다(같은 날 S2 ★는 (b)였음).
>
> **PM 결정 (2026-10-29, S3a T2 막힘 뒤)**: S3a T2·T3·T4를 한 작업으로 묶는다(선택지 1). 문서 읽기를 영어로 바꾸면 한국어 전체 흐름 시험 약 140개가 T3 파서·T4 tone 없이는 고칠 수 없기 때문이다. `./gradlew check` 가 통과할 때만 커밋하고(시험 도우미 정리 커밋이 앞에 하나 붙을 수 있음), 보고에 기존 시험 → 새 시험 대응표를 넣는다. 확인점 1(T3 코드 뒤 누적 S1 280)은 커밋 없이 재서, 넘으면 T4 전에 멈추고 보고한다. 확인점 2(520)는 그대로.
>
> **PM 결정 (2026-10-29, T4 (3) 멈춤 뒤)**: 병합 규칙 선택지 1, 주 링크 규칙 D3m을 둔다. 날짜 문서 줄과 연도 문서 줄의 짝에서 두 줄의 주 링크가 같은 구체 링크(같은 QID 또는 같은 제목, 리다이렉트 해석 뒤)이면 같은 사건으로 본다. 기준값 0.6은 그대로이고, 같은 문서 안의 두 줄에는 쓰지 않는다. 그 대가로 M3a1c `synthetic-one-link` 시험을 영어로 옮길 때 문서 사이 짝은 기대값을 "합쳐짐"으로 바꾸는 것을 승인한다(같은 문서 안 짝은 그대로 "따로"). 확인점 2 초과(일반 링크·낱말 목록 약 500줄)는 받아들이고, 목록 파일은 따로 커밋해 리뷰를 쉽게 한다.

**D1. 사건 문서 2개인가 3개인가** — (a) 날짜·연도만(S3b가 없어짐, 국내 사건 대부분을 잃음) / (b) + YYYY in Canada. **추천 (b)** (T3 7절: Canada 문서 항목의 60~98%가 다른 두 문서에 없음, 데모 4개 날짜 모두 항목 있음).

**D2. 범위 줄** — (a) 시작일만 그날로 넣고 원문 그대로 + "Started that day — ran until <끝 날짜>" 표시(`date_kind`·`range_end`) / (b) 범위 줄을 뺀다(캘거리·밴쿠버 올림픽 개막이 사라짐) / (c) 제품 정의서 6.1절에 "시작" 값을 새로 둔다. **추천 (a)**. 끝 날짜를 함께 보이는 것은 이 계획이 더한 것이다(문장이 날짜 키 뒤만 남아 기간이 안 보이기 때문). 6.1절은 미디어 표기라 고치지 않아도 되지만, 1차 정의서 수정 때 한 줄 넣을지 PM이 정해도 된다.

**D3. Commons 통신사 사진·미국법만 근거인 PD** — (a) 작가·크레딧에 통신사가 있거나 `Categories` 에 `PD US no notice` 가 있으면 **응답에서 뺀다**(T3 문구 "링크만" 대신 7.1절과 지금 코드에 맞춤) / (b) Commons 표시를 믿고 보인다 / (c) 차단 목록으로 사례마다. **추천 (a)**.

**D4. 사인의 사건·사고** — (a) `tragic` 으로 차분히 보인다 / (b) `tragic` 이면서 구체 링크(나라·주·도시·날짜·연도 문서가 아닌 링크)가 없으면 뺀다, 세 문서 모두에 / (c) Canada 문서의 비극 낱말 줄은 모두 뺀다. **추천 (b)**. 이 규칙은 공식 근거가 없는 제안이고, 빠진 줄은 `partial` 로 치지 않는다.

**D5. 위키백과 문장** — (a) 원문 줄 그대로(링크 기호만 뺌) + 문서 링크·CC BY-SA 4.0, 요약하지 않음 / (b) 요약 허용 + "수정됨". **추천 (a)** (지금 파서 동작 그대로라 일이 없다).

**D6. PR 나누기 (새)** — (a) 세 PR: S3a 날짜·연도(약 440), S3b Canada 문서(약 370), S3c 미디어·정리(약 310) / (b) 연구 계획대로 두 PR: 문서·파서 전부(약 570~750) + 범위·`tone`·미디어·이름(약 410~500) / (c) 한 PR(약 1,100). **추천 (a)**. (b)는 두 PR 모두 400을 넘고, `tone`·범위가 뒤 PR로 가면 그 사이 `main` 에 Canada 문서의 비극 줄이 D4 없이 나온다. (a)는 PR마다 `main` 이 일관된다.

**D7. S3a~S3c 사이 그날 사진·영상 (새)** — (a) S3a에서 `day-media-limit: 0`·`video.max-events: 0` 으로 끄고 S3c에서 캐나다 출처로 켠다(사건 사진은 영어판 대표 이미지로 계속 나옴) / (b) 그대로 둔다(한국 그날 사진, KTV 검색이 섞임). **추천 (a)**.

**D8. 한국어 이름(`KoWiki*`, `kowiki-*` 설정) 정리 시점 (새)** — (a) S3c 마지막에 기계적 커밋 하나(S1 따로 보고) / (b) S3a 처음에(S3a가 약 570으로 커짐) / (c) 하지 않는다. **추천 (a)**. 그 전까지 이름은 한국어판이지만 동작은 영어다(이 계획 표에 적어 둠).

**D9. 시험 날짜의 근거 (새)** — (a) 결정 1 (b) 승인을 근거로 바로 캐나다 데모 4개 + 1969-07-20 을 쓰고, 제품 정의서 12절은 PM이 S3a 머지 전까지 고친다(결정 5 (b) 1차) / (b) 12절을 먼저 고친 뒤 시작. **추천 (a)**. 작업을 막지 않는다.

**★ PM 직접 구현: S3b T2 캐시 V4와 엔티티** — Flyway로 열 이름을 바꾸고 데이터를 지우는 마이그레이션, JPA 엔티티·`@Query` upsert, `ddl-auto: validate`, Testcontainers로 "V3 → V4" 시험을 배울 수 있다. 작고 S3a를 막지 않는다(S3b 안에서도 파서 작업은 먼저 할 수 있음). (a) PM이 한다 / (b) backend-dev가 한다. **추천 (a)**. 같은 날 S2 ★를 (b)로 고른 속도 방침을 따르려면 (b)도 괜찮다.

결정 4 (b)의 "KTV 채널은 S3b에서 빼고 허용 채널이 비면 YouTube 끔"은 D6 (a)면 S3c T3으로 옮긴다(내용 같음).

## 다음 단계 제안 (이 계획 밖)

- Canada 문서 `Arts and literature` 절(방송 개시·문학상 등)을 읽을지 S3b 뒤 PM이 정한다.
- 템플릿 줄 건너뜀 수가 크면, 원문을 고치지 않고 살릴 수 있는 템플릿 목록을 따로 계획한다.
