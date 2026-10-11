# 사건 사진과 공용 미디어 형식 (M3a3)

> 브랜치: `feat/event-media` · 작성: planner · 상태: PM 승인 (2026-10-11, 미디어 결정 반영 개정)
> 기준: **PM 미디어 결정 2026-10-11**(사진·영상은 링크로 처리하지 않고 화면에서 바로 보이고 재생, 라이선스 미확인 자료도 직접 표시, 비상업 — PM 선택지 (c)), 안전장치 1~6(**PM 확인**, 3은 개정판), `CLAUDE.md` 7절, `docs/product-brief.md` 6.1절·6.3절·12절, `docs/roadmap.md` M3, `docs/research/day-events.md` 3·4·8절, `docs/research/summary.md` 6절 #17·#18
> 선행: M3a2 `feat/world-events-api` 머지. 영상과 제공사 공식 임베드는 이 기능 뒤 M3a4 `feat/event-video` 가 붙인다.

## 2026-10-11 개정 요약

| 이전 | 지금 |
|---|---|
| `display_mode` = `embed` / `link_only` (보여 줄지 말지) | `display_mode` = **어떻게 보여 주나**: `image` / `image_embed` / `video_file` / `video_embed` / `sdk` / `link_only`(기술적으로 막힌 경우만) |
| 라이선스가 허용 목록이 아니면 `link_only` | **라이선스와 관계없이 직접 표시**. 라이선스 상태는 `license_status`(`free`/`other`/`unknown`)로 정직하게 표시 |
| 촬영 시점을 모르면 `link_only` | 직접 표시하고 "촬영 시점 미상" |
| `Restrictions`, 작가 누락 → `link_only` | 직접 표시. 작가가 없으면 "작가 정보 없음" |
| (없음) | 출처별 **차단 목록**(안전장치 5), 통신사·언론사 자료는 **직접 표시·핫링크 금지, 제공사 공식 임베드(`image_embed`)로만**(안전장치 3 개정), 파일 복사 금지(안전장치 1) |

바뀌지 않은 것: 비자유(공정 이용) 위키 로컬 파일 제외(M3-P7), ±5년 규칙, `tragic` 사건에 미디어 없음, 6.1절 정직한 날짜 표기, 6.3절 잔혹한 이미지 금지.

## 안전장치 (PM 확인, 2026-10-11)

1. 미디어 파일은 우리 서버·저장소에 복사·재배포하지 않는다. 원 출처 주소(핫링크)나 원 제공자의 공식 플레이어·공식 임베드(iframe)로만 보여 준다.
2. 모든 미디어에 출처, 라이선스(모르면 "라이선스 확인 안 됨"), 촬영 시점·정밀도를 표시한다. 6.1절 정직한 날짜 표기, 6.3절 잔혹한 이미지 금지·`tragic` 규칙은 그대로다.
3. 통신사·언론사 사진(연합뉴스, AP, 게티이미지 등)을 직접 가져다 쓰는 것(핫링크 포함)은 금지한다. 제공사가 **공식 퍼가기(임베드)** 를 운영하면 그 방식(제공사 서버에서 로고와 함께 나오는 iframe 등)으로는 허용한다.
4. 재생·표시 경로 우선순위: 자유 라이선스(Commons, 공공누리 제1유형) → 임베드를 허용한 공식 채널 영상·제공사 공식 임베드 → 그 밖의 출처 직접 표시.
5. 권리자가 요청하면 바로 내릴 수 있게 출처별 차단 목록(설정값)을 둔다.
6. 공개 배포(#5)를 정할 때 이 결정을 다시 본다.

## 목표

1. **공용 미디어 형식**을 확정한다: `data/schema/media.schema.json` 과 API DTO. 6.2절 필드를 모두 담고, `display_mode` 를 표시·재생 방식으로 정의하고, 영상(`video_file`, `video_embed`), 제공사 공식 이미지 임베드(`image_embed`), 지도 SDK(`sdk`)까지 형식에 넣는다. M3a4·M4·M5가 그대로 쓴다.
2. 서버가 `date_relation`(같은 날 / 같은 달 / 같은 해 / N년 차이 / 무렵 / 현재 모습 / 미상)을 계산한다.
3. `GET /api/story/world` 의 `events[].media` 와 `day_media` 를 Commons **사진**으로 채운다. 라이선스가 확인되지 않아도 출처·라이선스 상태·촬영 시점과 함께 직접 보인다.
4. 차단 목록과 통신사·언론사 제외 규칙을 둔다.

## 범위 밖

- 영상, 제공사 공식 임베드(게티이미지 등) 고르기·만들기 → M3a4 (형식은 여기서 정한다)
- 지도 SDK 미디어 만들기 → M4 (`render` 형식만 정한다)
- 미디어 파일 복사·프록시·재배포 (안전장치 1)
- 프론트엔드 카드 → M3b1(사진), M3b3(영상·iframe 임베드)

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| Wikimedia Commons (Action API) | **확인됨** | 파일 메타데이터(`imageinfo` + `extmetadata`), 날짜 범주 `Category:South Korea photographs taken on YYYY-MM-DD` | User-Agent, 429 처리(M3a1 공통). 작가·라이선스·원문 페이지 표시. REST 요약·OTD의 `thumbnail` 을 쓰지 않는다. 핫링크는 "가능하지만 권장하지 않음" — 안전장치 1에 따라 핫링크로만 쓰고 공개 배포 때 다시 본다 — [day-events.md 3절](../research/day-events.md#3-출처-카드) |
| 한국어 위키백과 PageImages | **확인됨** | 대표 링크 문서의 대표 이미지 파일 이름(`pilicense=free`) | 비자유 이미지를 1차로 거른다(M3-P7) |

## 전제 (PM 결정)

| 결정 | 내용 | 상태 |
|---|---|---|
| #1 | 비상업 | 결정 |
| #4 (개정) | 사진·영상 모두 링크로 처리하지 않고 바로 표시·재생 | **결정 2026-10-11** |
| 미디어 표시 | 라이선스 미확인도 직접 표시 (PM 선택지 (c)) | **결정 2026-10-11** |
| 안전장치 1~6 | 위 절 | **PM 확인 2026-10-11** |
| #13-C | (a) Commons 주소 그대로(핫링크, `no-referrer`) | 결정 |
| #17 | (a) `current` 유지, "현재 모습(촬영 시점 미제공)" | 결정 |
| #18 (개정) | 아래 "필수 필드 규칙" — 표시 방식별 필수 필드 | 2026-10-11 결정에 맞춰 바뀜 |
| #22 | (c) + `tragic` 사건엔 미디어 없음, `tragic` 이 있는 날은 `day_media` 없음 | 결정 |
| 근접 연도 | 사건 미디어는 ±5년 안만 (`media.max-years-apart: 5`) | #15 확정 전 초안 |

## 공용 미디어 형식

### 필드 (저장 형식 = 스키마, API는 `date_relation` 만 더한다)

★ = 6.2절에 없던 필드.

| 필드 | 형 | 설명 |
|---|---|---|
| `id` | string | 예 `commons-12345678`, `youtube-<영상 ID>`, `getty-<자산 ID>` |
| `type` | `photo` \| `video` \| `aerial` \| `satellite` \| `streetview` | 무엇인가 |
| `display_mode` | `image` \| `image_embed` \| `video_file` \| `video_embed` \| `sdk` \| `link_only` | **어떻게 보여 주나**. `image`: `<img>`(원 출처 핫링크). `image_embed`: 제공사 공식 이미지 임베드 iframe(통신사·언론사는 이것만, 안전장치 3). `video_file`: `<video>` 로 원 출처 파일 재생(핫링크). `video_embed`: 원 제공자 공식 플레이어 iframe. `sdk`: 지도 SDK(M4). `link_only`: 원 출처가 핫링크·임베드를 **기술적으로** 막은 경우만(R7) |
| `url` | string \| null | 원본 파일 주소 (`image`, `video_file`) |
| `thumbnail_url` | string \| null | 화면용 이미지 주소. `image` 는 필수, 영상은 포스터(선택) |
| ★ `thumbnail_width`, `thumbnail_height` | integer \| null | 크기(레이아웃 흔들림 방지). `image_embed`·`video_embed` 는 iframe 비율에 쓴다 |
| ★ `video_sources` | array \| null | `video_file` 재생 후보 `[{"url":…, "mime":"video/webm", "height":480}]` (작은 것 먼저) |
| ★ `duration_seconds` | number \| null | 영상 길이 |
| ★ `embed_url` | string \| null | `image_embed`·`video_embed` 의 iframe 주소(서버가 허용 제공자 형식으로 만든다) |
| ★ `provider` | string \| null | `image_embed`·`video_embed`·`sdk` 의 제공자. 예 `youtube`, `getty`, `kakao` |
| ★ `render` | object \| null | `sdk` 용 `{"provider":"kakao","kind":"roadview"\|"skyview","lat":…,"lng":…,"zoom":…}` |
| ★ `link_only_reason` | `hotlink_blocked` \| `embed_disabled` \| null | `link_only` 일 때만 |
| `source_name` | string | 제공 기관·채널 |
| `source_url` | string | 원문 페이지 |
| `license` | string \| null | 원 출처에 적힌 라이선스 이름 그대로. 모르면 `null`. 제공사 임베드는 `provider-terms` |
| ★ `license_status` | `free` \| `other` \| `unknown` | `free`: 자유 라이선스 허용 목록(퍼블릭 도메인, CC0, CC BY, CC BY-SA, 공공누리 제1유형). `other`: 이름은 있지만 허용 목록 밖(제공사 임베드 약관 포함). `unknown`: 확인 안 됨 → 화면 "라이선스 확인 안 됨" |
| ★ `license_url` | string \| null | |
| `attribution` | string | 화면 출처 문구. 작가를 모르면 "작가 정보 없음" |
| ★ `description` | string \| null | 무엇이 찍혔는지(대체 텍스트·iframe `title`) |
| `captured_at` | string \| null | `YYYY-MM-DD` / `YYYY-MM` / `YYYY`. 모르면 `null` |
| `date_precision` | `day` \| `month` \| `year` \| `approx` \| `current` \| null | `null` = 촬영 시점 미상 |
| `lat`, `lng` / `place_ref` | | 위치 |
| `retrieved_at` | string | 가져온 시각 |
| `date_relation` (**API만**) | object | 아래 |

### 필수 필드 규칙 (#18 개정 — 스키마와 서버 정규화가 같은 규칙)

| 규칙 | 내용 |
|---|---|
| R1 언제나 | `id`, `type`, `display_mode`, `source_name`, `source_url`, `attribution`, `license_status`, `retrieved_at` 이 있다. 위치는 (`lat`·`lng`) 또는 `place_ref`. **`license` 는 비어도 된다** |
| R2 `image` | `url`, `thumbnail_url` 이 있다. `source_name`·`attribution` 이 통신사·언론사 제외 목록에 걸리면 안 된다(안전장치 3) |
| R3 `image_embed` | `embed_url`, `provider` 가 있고 `provider` 가 허용 이미지 임베드 제공자 목록(설정값 `media.allowed-image-embed-providers`, 처음은 빈 목록 — 리서치 V6 결과로 `getty` 등 추가)에 있다. `url`·`thumbnail_url` 은 `null` |
| R4 `video_file` | `url` 또는 `video_sources`(1개 이상) |
| R5 `video_embed` | `embed_url`, `provider` 가 있고 `provider` 가 허용 영상 임베드 제공자 목록(설정값 `media.allowed-video-embed-providers`, 처음은 빈 목록 — 리서치 V2~V4 결과로 `youtube` 등 추가)에 있다. `url` 은 `null` |
| R6 `sdk` | `render`, `provider` 가 있고 `url`·`thumbnail_url` 은 `null` |
| R7 `link_only` | 원 출처가 핫링크·임베드를 기술적으로 막았다고 확인된 경우만: `link_only_reason` 이 있고 `url`·`thumbnail_url`·`embed_url` 은 `null`. M3a3의 Commons 사진에서는 생기지 않는다 |
| R8 정밀도와 형식 | `day` → `YYYY-MM-DD`, `month` → `YYYY-MM`, `year`·`approx` → `YYYY`, `current`·`null` → `captured_at` 은 `null` |

정규화: R2~R7을 못 채운 미디어는 **버린다**(다른 모드로 바꾸지 않는다). R1을 못 채워도 버린다. API에 나가는 미디어는 늘 스키마를 통과한다.

### 차단과 제외 (안전장치 3·5)

| 규칙 | 내용 |
|---|---|
| 차단 목록 | 설정값 `media.blocklist`: 항목마다 `source`(예 `commons`, `youtube`, `getty`), `id`(파일 이름·영상 ID·자산 ID) 또는 `author`/`channel`. 맞으면 응답에서 뺀다. 서버 재시작으로 적용. 권리자 요청을 받으면 PM이 설정에 추가한다 |
| 통신사·언론사 직접 표시 금지 | `display_mode` 가 `image`·`video_file` 인 레코드의 `Artist`·`Credit`·`source_name` 에 제외 목록(설정값 `media.excluded-credits`, 처음 값: 연합뉴스, Yonhap, 뉴시스, Newsis, AP, Associated Press, Reuters, AFP, Getty, EPA)이 들어 있으면 뺀다. 이 제공사들은 `image_embed`(공식 임베드, M3a4)로만 나올 수 있다 |
| 비자유 위키 로컬 파일 | Commons에 없는 파일(위키 로컬)·`NonFree` 표시 파일은 뺀다 (M3-P7) |

### `date_relation` (API만)

| `kind` | 조건 (요청 D, 촬영 C, 정밀도 P) | `years` |
|---|---|---|
| `same_day` | P=`day`, C=D | 0 |
| `same_month` | P가 `day`/`month`, 연·월 같음(same_day 제외) | 0 |
| `same_year` | P가 `day`/`month`/`year`, 연도 같음(위 제외) | 0 |
| `years_apart` | P가 `day`/`month`/`year`, 연도 다름 | C 연도 − D 연도 |
| `approximate` | P=`approx` | C 연도 − D 연도 |
| `current` | P=`current` | 0 |
| `unknown` | P=`null` | 0 |

### 예 (값은 예시)

```json
{
  "id": "commons-12345678", "type": "photo", "display_mode": "image",
  "url": "https://upload.wikimedia.org/wikipedia/commons/…/Example.jpg",
  "thumbnail_url": "https://upload.wikimedia.org/wikipedia/commons/thumb/…/640px-Example.jpg",
  "thumbnail_width": 640, "thumbnail_height": 427,
  "video_sources": null, "duration_seconds": null, "embed_url": null, "provider": null,
  "render": null, "link_only_reason": null,
  "source_name": "Wikimedia Commons", "source_url": "https://commons.wikimedia.org/wiki/File:Example.jpg",
  "license": "Public domain", "license_status": "free", "license_url": null,
  "attribution": "U.S. Air Force · Public domain · Wikimedia Commons",
  "description": "1988년 서울 올림픽 개막식",
  "captured_at": "1988-09-17", "date_precision": "day",
  "lat": null, "lng": null, "place_ref": "country:KR",
  "retrieved_at": "2026-10-12T03:00:00Z",
  "date_relation": {"kind": "same_day", "years": 0}
}
```

라이선스 미확인 사진: `"license": null, "license_status": "unknown", "attribution": "작가 정보 없음 · Wikimedia Commons"` — 그래도 `display_mode: image` 로 직접 표시.

## Commons 사진 규칙

| 검사 | 결과 |
|---|---|
| Commons에 없음(위키 로컬) / `NonFree` | 제외 |
| 차단 목록·통신사·언론사 표기 | 제외 |
| MIME이 `image/jpeg` 가 아님 | 제외 (로고·지도·국기 거르기. 동영상 MIME은 M3a4) |
| `LicenseShortName` 이 허용 목록 | `license_status: free` |
| 그 밖의 이름 | `license_status: other`, `license` = 받은 이름 |
| 라이선스 필드 없음 | `license: null`, `license_status: unknown` |
| `DateTimeOriginal` 을 읽음 / 못 읽음 | 아래 표 / `captured_at`·정밀도 null (그래도 표시) |

`captured_at` 읽기 (HTML 태그를 먼저 벗긴다):

| 원문 모양 | 결과 |
|---|---|
| `1988-09-17`, `1988-09-17 12:34:56`, `1988:09:17 12:34:56` | `1988-09-17`, `day` |
| `17 September 1988`, `September 17, 1988`, `Taken on 17 September 1988`, `… (Exif)` | `day` |
| `September 1988`, `1988-09` | `month` |
| `1988` | `year` |
| `c. 1988`, `circa 1988`, `ca. 1988`, `around 1988` | `approx` |
| 서로 다른 날짜가 둘 이상, 범위, `1980s`, 읽을 수 없음, 빈 값 | `null` |

사건 날짜(P585, 위키백과 줄 날짜)를 촬영 시점으로 옮기지 않는다.

그 밖의 필드: `display_mode: image`, `url` = `imageinfo.url`, `thumbnail_url` = `thumburl`(`iiurlwidth=640`), `source_url` = `descriptionurl`, `attribution` = `Artist`(평문, 없으면 `Credit`, 둘 다 없으면 "작가 정보 없음") + ` · ` + (`license` 또는 "라이선스 확인 안 됨") + ` · Wikimedia Commons`, `description` = `ImageDescription` 평문(한국어 → 영어, 200자), 위치 = GPS 또는 `place_ref`.

### 고르는 법

| 자리 | 후보 | 개수 |
|---|---|---|
| `events[].media` | 대표 링크 문서의 위키백과 대표 이미지 → Commons 메타데이터 | 사건마다 사진 최대 1(M3a4가 영상·제공사 임베드를 더한다). `tragic` 사건 0. years의 절댓값 > 5 면 붙이지 않음 |
| `day_media` | `Category:South Korea photographs taken on <날짜>` 의 파일 | 최대 6. 사건 미디어와 같은 파일 제외. 그날 `tragic` 사건이 있으면 0 |

Commons 실패 → 사건은 그대로, `media`·`day_media` 비움, `partial: true`, 200.

## 테스트 방침

M3a1 "테스트 방침"을 따른다. 녹화 fixture `fixtures/wikimedia/commons/`(README에 원 URL·녹화 일자·라이선스). 합성 fixture로 규칙 표의 행마다 시험. 스키마 검증은 M3a2 T4가 넣은 라이브러리로 `valid/`·`invalid/` 예시와 API 응답 미디어 전부.

## 데모 입력 3개로 확인할 것

| 데모 입력 | 확인 (녹화 fixture 기준) |
|---|---|
| 1988-09-17 + "서울" | `day_media` 에 날짜 범주 사진(리서치 기준 5장)이 **모두** `display_mode: image`. 한 날짜로 읽히는 파일은 `same_day`, 값이 둘인 파일도 빠지지 않고 `unknown`·정밀도 null. PD·CC BY-SA → `license_status: free` |
| 2002-06-22 + "광주 서구 풍암동" | `day_media` 1장, `license` `KOGL Type 1`, `free`, `same_day` |
| 1995-03-15 + "서울대학교병원" | `day_media` = `[]`, `notice.code` = `NO_EVENTS_FOUND` |

## 크기 (400줄 규칙)

예상: 미디어 도메인(형식·정규화·`date_relation`) 약 95, 촬영 시점 파서 약 60, 라이선스 상태·필드 만들기 약 55, 차단·제외 규칙 약 40, Commons 클라이언트 약 70, PageImages 약 25, 서비스 연결 약 45, 설정 약 10 → **약 400줄**. T2 뒤 누적 275줄을 넘으면 T3을 뺄지 PM에게 묻는다.

## 작업 목록

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | 공용 미디어 스키마와 예시 | content-curator | `data/schema/media.schema.json`, `data/schema/examples/media/{valid,invalid}/*.json`, `README.md` | - (M3a2 머지) | (1) JSON Schema 2020-12, 필드 표 전부(`date_relation` 제외)와 R1~R8, `additionalProperties: false`. (허용 제공자 목록은 서버 설정이라 스키마는 `provider` 를 문자열로만 검사) (2) `valid/` 최소 7개: Commons 실제 사진 2개(1988-09-17 PD 1, 2002-06-22 KOGL 제1유형 1 — 파일 설명 페이지를 열어 확인), `license: null`·`unknown` 사진 1개(합성), `image_embed`·`video_file`·`video_embed`·`sdk` 각 1개(합성, `id` 가 `example-` 로 시작). (3) `invalid/` 최소 8개, 각각 규칙 하나만 어김(R1~R8). (4) README에 파일마다 어기는 규칙 또는 실제 출처 URL·확인 일자. (5) C1이 모든 파일에 `ok`. 미디어 파일은 내려받지 않는다 |
| T2 | 미디어 도메인: 형식·정규화, `date_relation`, 촬영 시점 파서, 라이선스 상태, 차단·제외 규칙 | backend-dev | `media` 패키지, `media.blocklist`·`media.excluded-credits`·`media.allowed-image-embed-providers`·`media.allowed-video-embed-providers`(둘 다 빈 목록)·`media.max-years-apart` 설정, 테스트 | T1 | (1) `./gradlew check` 성공. (2) 촬영 시점 표 행마다 테스트. (3) `date_relation`: D=1988-09-17 기준 7가지 `kind`, 경계값 D=2000-02-29(`2000-02-01` → `same_month`, `1999-02-28` → `years_apart` −1). (4) 정규화: R2~R7을 못 채우면 버림(모드를 바꾸지 않음), `license` null은 통과, 허용 목록에 없는 `provider` 의 `image_embed`·`video_embed` 는 버림. (5) 라이선스 상태: 허용 목록 → `free`, `CC BY-NC 4.0` 같은 이름 → `other`, 없음 → `unknown`. (6) 차단 목록(파일 이름, 작가)과 통신사·언론사 `Credit` 이 든 `image` 레코드가 빠지고, 같은 제공사라도 허용 목록에 넣은 `image_embed` 는 남는다(합성). (7) `valid/` 예시를 읽고 다시 쓴 JSON이 스키마 통과, `invalid/` 는 실패 |
| T3 | Commons 클라이언트, PageImages, 세계 API 연결 | backend-dev | Commons 클라이언트(`wikimedia.commons-api-url`), PageImages 조회, 서비스 연결, fixture, 테스트 | T2 | (1) 데모 표 3행 통과. (2) 데모 3날짜 응답의 모든 미디어가 `date_relation` 을 뺀 뒤 스키마 통과. (3) 합성: `NonFree`·Commons에 없음·비JPEG → 제외, 라이선스 없음 → 표시 + `unknown`, `Restrictions` 있음 → 표시, 작가 없음 → "작가 정보 없음", ±6년 사건 이미지 → 붙지 않음. (4) `tragic` 사건 `media` = `[]`, `tragic` 이 있는 날 `day_media` = `[]`. (5) 범주 파일 8개 → `day_media` 6개, 중복 없음. (6) Commons 503 → 200, `partial: true`. (7) 요청마다 `User-Agent`, PageImages 요청 1회. (8) 응답 어디에도 `display_mode: link_only` 가 없다 |
| T4 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T3 | (1) T2·T3 기준 표. (2) `./gradlew check` 통과, 3회 반복 안정. (3) S1·S2·S3 보고. (4) 데모 3날짜 미디어 표(자리, `display_mode`, `license`, `license_status`, `captured_at`, `date_relation`) |
| T5 | 실제 호출 확인 (PR 검수 때) | PM | - | T4 | (1) P1·P2 결과가 데모 표와 맞는다. (2) `thumbnail_url` 하나를 브라우저로 열어 이미지가 보이고 `source_url` 페이지의 라이선스가 `license` 와 같다 |

명령 (저장소 루트):

```
# C1 — 스키마·예시가 JSON으로 읽히는지. 파일마다 ok
for f in data/schema/media.schema.json data/schema/examples/media/*/*.json; do
  python3 -c 'import json,sys; json.load(open(sys.argv[1])); print("ok", sys.argv[1])' "$f"; done

# S1·S2·S3 — 줄 수
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'
git diff --numstat main...HEAD -- data | awk '{a+=$1} END {print "data added", a}'

# P1·P2 — PM 실제 호출
curl -s 'http://localhost:8080/api/story/world?date=1988-09-17' \
  | jq '[.day_media[] | {display_mode, license, license_status, captured_at, date_relation, thumbnail_url}]'
curl -s 'http://localhost:8080/api/story/world?date=2002-06-22' | jq '.day_media[] | {display_mode, license, attribution, date_relation}'
```

담당별: content-curator 1, backend-dev 2, test-engineer 1, PM 1.

## 리스크와 확인할 것

1. **저작권 (PM이 알고 정함, 2026-10-11)** — 라이선스가 확인되지 않은 사진을 직접 보여 준다. 비상업·로컬 데모, 복사 없이 핫링크만, 출처·라이선스 상태 표시, 통신사·언론사 직접 표시 금지, 차단 목록으로 위험을 줄인다. 공개 배포 전에 다시 본다(로드맵 리스크 4).
2. **핫링크** — Commons는 권장하지 않는다. 로컬 데모라 요청이 적다. 브라우저는 `referrerpolicy="no-referrer"`(M3b1).
3. **통신사 표기 판별은 문자열 비교다** — `Credit` 에 다른 표기로 들어간 통신사 사진은 걸러지지 않을 수 있다. 데모 결과는 T4 표로 사람이 본다. 발견되면 제외 목록이나 차단 목록에 더한다.
4. **사건 대표 이미지가 사건과 어긋날 수 있다** — `date_relation` 으로 정직하게 보이고 ±5년을 넘으면 붙이지 않는다.
5. **JPEG만** — PNG 사진도 빠진다.
6. **`DateTimeOriginal` 형식** — 표에 없는 모양은 "촬영 시점 미상"으로 표시된다.

## PM이 정할 것

새 결정: M3-P7(비자유 위키 로컬 파일 제외 유지, 추천 예). 전체 표는 로드맵 M3 절.
