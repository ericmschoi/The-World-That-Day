# 그날의 세계 섹션 (M3b2)

> 브랜치: `feat/world-events-ui` · 작성: planner · 상태: **PM 승인 (2026-10-13, 개정)** — 결정 1 **(a)** 한 PR + T1b 뒤 확인점(누적 S1 170), 결정 2 **(a)** T5(실제 서버, 사진 없이)로 머지하고 실제 사진 확인은 M3a3b 머지 뒤 `main` 에서 T6. (처음 승인 2026-10-11, 2026-10-12 개정 승인 — 분할로 섹션 뼈대만 남김(로드맵 D1 (a)). "그날 찍힌 사진" 묶음·비극 블록·M3b1b 리뷰 후속·Playwright는 M3b2b `feat/world-photos-a11y`)
> 기준: `docs/product-brief.md` 5절 4번·6절·8절·12절, `CLAUDE.md` 7절 1항(**촬영 시점을 모르면 "촬영 시점 미상"으로 표시하고 그날 것처럼 보이게 하지 않는다**)·5항·6항, `docs/roadmap.md` M3(속도 개선 A), `docs/plans/m3a2-world-events-api.md`("API 계약" — 머지된 코드 `server/src/main/java/io/github/ericmschoi/theworldthatday/story/world/` 기준), `docs/plans/m3a3-event-media.md`("공용 미디어 형식", "뒤 기능과의 형식 호환"), `data/schema/media.schema.json`·`data/schema/examples/media/valid/`, `docs/plans/m3b1-design-foundation.md`(토큰·문구), `docs/plans/m3b1b-media-card.md`(카드)
> 선행: **M3a2 (PR #11)·M3a3 (PR #12)·M3b1b (PR #7) 머지 — 모두 `main` f99a9e7 에 있다.** 이 기능은 그 `main` 에서 갈라졌다. **서버 M3a3a `feat/media-rules` 와 병렬**로 진행한다(PM 결정 A, 2026-10-13 — M3a3a는 응답 형식을 바꾸지 않으므로 기다릴 것이 없다). 구현과 자동 테스트는 MSW fixture만 쓴다. **실제 사진으로 보는 PM 화면 확인은 M3a3b `feat/commons-photos` 머지 뒤**다(T6, 결정 2 (a) — PR 머지를 막지 않는다).
> 뒤따르는 기능: M3b2b(사진 묶음·비극 블록·접근성·Playwright), M3b3(Commons 영상 재생), M3b3b(동의 단계), M3b3c(YouTube 재생). 이 섹션의 `MediaItem` 호출은 바꿀 필요가 없다.

## 2026-10-13 바뀐 점

| 바뀐 것 | 이유 |
|---|---|
| **선행과 일정**: "M3a3 머지를 기다린 뒤 시작, PM 화면 확인도 M3a3 머지 뒤" → **M3a3 머지 직후 시작, M3a3a와 병렬. 실제 사진 확인은 M3a3b 머지 뒤(T6)** | PM 결정 A (2026-10-13). M3a3는 머지됐지만 `media`·`day_media` 는 M3a3b 전까지 늘 `[]` 다 |
| **계약을 머지된 서버에 맞춤** (절 "API / 데이터 계약") | 에러 `code` 5개(`DATE_REQUIRED`, `DATE_FORMAT`, `DATE_NOT_EXIST`, `DATE_IN_FUTURE`, `UPSTREAM_UNAVAILABLE`), snake_case 키 목록(서버 데모 테스트의 키 집합), `normal` 먼저 → `tragic` 순서, `notice`·`partial`, `tone`·`tone_basis` 값 목록, 원문 그대로(M3-P16) |
| **데모 1988-09-17 사건 1개 → 3개** | M3a2 결과: 올림픽 개막 + 1988년 문서 문화 절의 KBS 1TV·KBS 2TV 줄. 원문 그대로 보인다(M3-P16, 2026-10-12) |
| **사건 문장은 글자(text)로만** 그린다. `dangerouslySetInnerHTML` 금지(C6) | M3-P16: 원문을 거르지도 고치지도 않는다. 다듬기는 최종 표시 단계에서 따로 계획(로드맵 후속 후보) |
| **fixture를 두 층으로**: (1) 지금 서버와 똑같은 모양(`media: []`), (2) M3a3 `valid/` 예시로 만든 미디어를 넣은 모양 | 서버가 실제로 내는 응답과 M3a3b 뒤의 응답을 둘 다 시험한다 |
| **T1을 T1·T1b로 나눔** (T2~T5 번호는 그대로, **T6 새로**) | T1의 완료 기준이 한 작업(1~3 커밋)을 넘었다. T1 = API 클라이언트, T1b = 타입·웹 타입 고침·fixture·핸들러. T6 = M3a3b 뒤 실제 사진 확인(PM) |
| **웹 타입 고침을 T1b에 명시** | M3a3 T4 대조에서 나온 차이 3개: `render.zoom`·`video_sources[].height` 는 서버에서 `null` 가능, `MediaSelection.basis` 가 M3a4 개정 전 목록, `selection` 은 서버가 아직 보내지 않음(M3a4가 더함) |
| **`fallbackAlt` 규칙 단순화**: `title` 이 있으면 「{title}」 관련 자료, 없으면 "사건 관련 자료" (문장 앞 20자 쓰지 않음) | 원문 문장에는 날짜 표현이 들어 있을 수 있다. 대체 텍스트에 날짜를 넣지 않는 규칙(M3b2b)을 지키기 쉽다 |
| **출처 줄 링크에 "(새 창에서 열림)" 숨은 글자**, 공용 `visually-hidden` 클래스를 이 기능에서 만든다 | 출처 링크는 새 창으로 연다(결과 화면 상태를 주소에 담지 않으므로 같은 창 이동은 결과를 잃는다). M3b1b 리뷰 후속(새 창 안내)과 같은 규칙. **M3b2b T2는 이 클래스를 다시 쓰고** 캡션 링크에 같은 안내를 단다 |
| **`ApiError` 에 `code`**, 오류 메시지에 쿼리(날짜) 없음 | 서버 공통 에러 형식(M3a2)을 읽어 두면 M2b가 문구를 `code` 로 고를 수 있다. 화면에는 지금 고정 문구 하나만 |
| **Commons `thumb.wikimedia.org` 호스트** (M3a3 리뷰 권장 6) | 웹에는 이미지 호스트 허용 목록·CSP가 **없다**(주소는 `http(s)` 만 확인). 이 기능도 만들지 않는다. fixture는 실제 예시대로 `thumb.wikimedia.org`(960px 단계) 주소를 써서, 이 호스트가 걸러지지 않음을 시험으로 남긴다. 허용 목록·CSP를 만들 때(M6b·공개 배포) `thumb.wikimedia.org` 와 `upload.wikimedia.org` 를 넣는다(리스크 7) |
| **크기 다시 셈**: 약 240(보정 385) → **약 365 (범위 365~510)**, T1b 뒤 확인점 | 아래 "크기". PM 결정 1 (a): 한 PR, T1b 뒤 누적 S1 170에서 확인 |

M3b1b 리뷰 후속 4건(새 창 안내, 설명 두 번 읽기, `fallbackAlt` 의 날짜 표현 시험, Playwright 포커스 링)은 **그대로 M3b2b**다. 이 기능은 그중 새 창 안내의 공용 클래스와 출처 줄 링크만 먼저 하고, `WorldSection` 이 넘기는 `fallbackAlt` 시험(T2 (10))을 둔다.

## 목표

1. 결과 화면의 `world` 섹션이 `GET /api/story/world` 를 불러 그날의 `normal` 사건을 서버가 준 순서대로 보여 준다: 원문 문장(글자 그대로), 사건 미디어(바로 표시), 문장 출처(위키백과 문서 링크 + CC BY-SA 4.0, #23 (a)).
2. 사건이 없는 날(`notice.code = NO_EVENTS_FOUND`)은 "이 날짜에 기록된 큰 사건을 찾지 못했습니다."를 보여 주고 섹션을 숨기지 않는다(#21 (a)).
3. 요청이 실패하거나(400, 503, 네트워크) 10초 안에 끝나지 않으면 이 섹션만 오류 상태가 되고 다른 섹션은 그대로다(제품 정의서 8절).
4. 입력 화면이 생기기 전(M2b)까지 PM이 실제 서버로 데모 3개 날짜를 볼 수 있게 **임시 데모 날짜 버튼**을 둔다(M3-P5 (a)).
5. 중간 상태: `tragic` 사건과 `day_media` 는 **이 기능에서는 그리지 않는다**. M3b2b가 비극 블록과 사진 묶음으로 그린다(데모 3개 날짜에는 `tragic` 사건이 없다).

## 범위 밖

- "그날 찍힌 사진"·"촬영 시점을 확인하지 못한 사진" 묶음(M3-P17 (a)), 비극 블록, 캡션 접근성(라이선스 줄 (b)·캡션 링크 새 창 안내·설명 두 번 읽기), Playwright 확인 → M3b2b
- 영상·제공사 임베드 재생 → M3b3·M3b3c (이 섹션은 `MediaItem` 에 미디어를 넘기기만 한다)
- 동의 단계·개인정보처리방침 → M3b3b(임시 입력 화면), M2b(실제 입력 폼)
- 실제 입력 폼, 날짜 검증 문구(`code` 별 문구), 오프닝, 결과 화면 URL, 화면 전환 포커스 이동 → M2b (M2b가 임시 데모 버튼을 지운다)
- 사건 문장 다듬기·해석(M3-P16, 로드맵 후속 후보), `partial` 표시, 장 전환 효과, 빈 섹션 문구 전체 다듬기, 출처 전체 목록 → M6a·M6b
- 이미지 호스트 허용 목록·CSP → M6b·공개 배포(#5) 때
- `scope` 로 국내·세계를 나눠 보여 주는 것, 데이터 패칭 라이브러리, 개발 브라우저용 MSW, 서버 변경

## 사용할 외부 출처

없음. 프론트엔드는 우리 API(`/api/story/world`)만 부른다. 사진은 데이터의 `thumbnail_url`(원 출처)에서 브라우저가 바로 받는다(안전장치 1, M3b1b `MediaFigure`). 테스트는 외부 네트워크를 부르지 않는다(MSW. jsdom은 `img` 를 내려받지 않는다).

## 전제 (PM 결정)

| 결정 | 내용 |
|---|---|
| #4 (개정 2026-10-11) | 사진·영상은 링크로 처리하지 않고 바로 표시·재생 |
| 미디어 표시 | 라이선스 미확인도 직접 표시하고 캡션에 "라이선스 확인 안 됨"(M3b1·M3b1b) |
| 촬영 시점 미상 | 표시하되 "촬영 시점 미상", 그날 것처럼 보이게 하지 않는다 |
| #21 | (a) 빈 날짜 안내 |
| #22 | (c), `tragic` 은 별도 블록·미디어 없음 → 블록은 M3b2b. 그전에는 그리지 않는다 |
| #23 | (a) 원문 그대로 + 문서 링크 + CC BY-SA 4.0 |
| M3-P16 (2026-10-12 변경) | 위키백과 원문을 거르지 않고 글자 그대로. 다듬기는 최종 표시 단계에서 따로 계획 |
| M3-P5 | (a) 임시 데모 날짜 버튼 |
| M3-P17 | (a) → M3b2b |
| 속도 개선 A (2026-10-13) | 웹·서버 병렬. 웹 화면 확인에 실제 데이터가 필요한 곳만 서버 머지를 기다린다 |
| **M3b2 결정 1 (2026-10-13)** | **(a)** 한 PR. T1b 뒤 확인점: T1·T1b 누적 S1이 170을 넘으면 오케스트레이터가 멈추고 PM에게 다시 묻는다 |
| **M3b2 결정 2 (2026-10-13)** | **(a)** T5(실제 서버, 사진 없이) 뒤 머지. 실제 사진 확인은 T6 — M3a3b 머지 뒤 `main` 에서 |

## API / 데이터 계약 (서버 변경 없음)

원천: `docs/plans/m3a2-world-events-api.md` "API 계약"과 머지된 `WorldStoryResponse`·`StoryDate`·`ApiExceptionHandler`, 미디어는 `docs/plans/m3a3-event-media.md` "공용 미디어 형식"과 `MediaRecord`·`MediaItem`·`DateRelation`·`Render`·`VideoSource`, 저장 필드의 null 가능 여부는 `data/schema/media.schema.json`. 계획서와 코드가 다르면 **코드를 따르고 보고**한다.

### 요청

`GET /api/story/world?date=YYYY-MM-DD` — 웹은 데모 3개 날짜만 보낸다.

### 응답 200 (`web/src/world/types.ts`, 이름·형은 서버 JSON 그대로 snake_case)

```ts
type WorldResponse = {
  date: string
  events: WorldEvent[]            // tone=normal 먼저(원래 순서), 그다음 tragic
  day_media: Media[]              // M3a3b 전까지 늘 []
  notice: { code: 'NO_EVENTS_FOUND' } | null   // events 가 비면 객체, 아니면 null
  partial: boolean                // 화면 표시 없음
}

type WorldEvent = {
  id: string                      // kowiki-YYYY-MM-DD-<8자리 16진수>
  date: string
  scope: 'domestic' | 'world' | 'unknown'
  title: string | null            // 대표 링크 제목
  summary: string                 // 원문 그대로 (M3-P16) — 글자로만 그린다
  summary_lang: 'ko'
  summary_modified: boolean       // 늘 false (#23 (a))
  text_license: string            // "CC BY-SA 4.0"
  text_license_url: string
  source_name: string             // "한국어 위키백과"
  source_title: string            // "9월 17일" 또는 "1988년"
  source_url: string
  source_section: '사건' | '문화'
  source_revision: number
  wikidata_qid: string | null
  tone: 'normal' | 'tragic'
  tone_basis: 'curated' | 'rule:keyword' | 'rule:class' | 'rule:deaths' | 'rule:no-signal' | 'default'
  related_links: { title: string; url: string }[]
  media: Media[]                  // M3a3b 전까지 늘 []
  retrieved_at: string
}
```

키 집합(서버 데모 테스트와 같음): 최상위 `date, events, day_media, notice, partial`. 사건 20개 — 위 `WorldEvent` 의 키 전부. 미디어 27개 — M3a3 저장 필드 26개 + `date_relation`(서버는 `null` 인 키도 늘 보낸다. `selection` 은 아직 없음).

### 미디어 타입 고침 (`web/src/media/types.ts`, T1b)

| 필드 | 지금 웹 | 서버·스키마 | 고친 뒤 |
|---|---|---|---|
| `video_sources[].height` | `number` | `Integer`, 스키마 `nullablePositiveInteger` | `number \| null` |
| `render.zoom` | `number` | `Double`, 스키마 `null \| number` | `number \| null` |
| `selection.basis` | `commons_category`·`commons_search`·`youtube_official`·`daehan_news`·`provider_embed` | M3a4 개정 계약: `commons_p10`·`commons_category`·`commons_date_category`·`commons_search`·`youtube_official`·`daehan_news` (`provider_embed` 없음 — M3a6 제외) | 오른쪽 6개 |
| `selection` | `selection?: MediaSelection \| null` | 서버가 아직 보내지 않음(M3a4가 더함) | 그대로 선택 필드. 화면에 쓰지 않음 |
| `render.provider`·`kind`·`lat`·`lng` | null 아님 | Java는 박싱형이지만 스키마 필수·null 불가 | 그대로 |
| 그 밖 26개 | — | — | T1b 보고서의 대조표(이름·형·null 가능)로 확인. 다르면 웹을 서버에 맞추고 보고 |

`web/src/media/types.test.ts` 의 기대 형(`Expected`)도 같이 바꾼다. 테스트를 지우지 않고 기대값만 바꾸며, 바꾼 줄은 보고서에 표로 남긴다.

### 에러 (ProblemDetail + `code`, 본문에 입력값 없음)

| 상태 | `code` | 화면 |
|---|---|---|
| 400 | `DATE_REQUIRED`, `DATE_FORMAT`, `DATE_NOT_EXIST`, `DATE_IN_FUTURE`(Asia/Seoul 기준) | `StorySection` `error` — "이 섹션을 불러오지 못했습니다." (`code` 별 문구는 M2b) |
| 503 | `UPSTREAM_UNAVAILABLE` (두 출처 문서를 모두 쓸 수 없을 때만) | 같음 |
| 네트워크 오류, 10초 시간 초과, 본문이 JSON이 아님 | (없음) | 같음 |

- 화면은 `detail`·`code`·요청 날짜를 보여 주지 않는다.
- 웹 타입 `ApiProblemCode` = 위 5개. `ApiError.code` 는 `string | null`(본문이 problem JSON이 아니면 `null`).

### 응답과 화면

| 응답 | 화면 |
|---|---|
| 요청 중 | `StorySection` `loading` (`aria-busy="true"`, "불러오는 중…") |
| 200, `normal` 사건 있음 | `ready` — `normal` 사건 목록, 서버 순서 그대로. `tragic` 은 M3b2b 전까지 그리지 않음 |
| 200, `notice.code` = `NO_EVENTS_FOUND` | `ready` — 안내 문장 |
| 200, `normal` 없이 `tragic` 만 (`notice` = `null`) | `ready` — 안내 문장 없이 빈 목록(M3b2b가 비극 블록으로 채움) |
| 200, `partial` = `true` | 위와 같음 (화면 표시 없음) |
| 200, 사건의 `media` = `[]` (M3a3b 전 실제 서버) | 사건 문장과 출처 줄만. 빈 미디어 자리·빈 목록 요소를 만들지 않는다 |
| 400, 503, 네트워크 오류, 10초 시간 초과 | `error` |

### API 클라이언트 보강 (M1 리뷰에서 미뤘던 것, T1)

- `apiGet<T>(path, options?: { signal?: AbortSignal; timeoutMs?: number })`. `timeoutMs` 기본 10000(제품 정의서 8절).
- 호출자의 `signal` 과 시간 초과 신호를 합쳐(`AbortSignal.any` + `AbortSignal.timeout`) `fetch` 에 넘긴다.
- 시간 초과 → `ApiTimeoutError`. 호출자가 취소 → 원래 `AbortError` 그대로(컴포넌트는 무시).
- `Accept: application/json, application/problem+json`. 실패 응답이면 본문이 JSON이고 `code` 가 문자열일 때만 `ApiError.code` 에 담는다.
- `ApiError`·`ApiTimeoutError` 의 `message` 에는 경로만 넣고 **쿼리 문자열(날짜)을 넣지 않는다**.
- `getWorld(date: string, signal?: AbortSignal)` → `/api/story/world?date=` + `encodeURIComponent(date)`. 기존 `/api/` 경로 검사는 쿼리가 붙어도 통과해야 한다.
- 기존 `getHealth()` 동작과 테스트는 그대로.

### MSW fixture (T1b, `web/src/mocks/fixtures/world/`)

TS 모듈로 둔다(`satisfies WorldResponse` 로 문자 값 유니언까지 타입 검사. M3b1b 미디어 fixture와 같은 방식). M3b2b의 Playwright `page.route` 는 이 객체를 JSON으로 직렬화해 쓴다.

| 이름 | 내용 |
|---|---|
| `world1988` | **지금 서버와 같은 모양.** 사건 3개, 이 순서: ① `kowiki-1988-09-17-875c69fa`, `title` `1988년 하계 올림픽`, `summary` "제24회 서울 하계 올림픽 개막.", `9월 17일`/`사건`, 42604413, `Q8470` ② `title` `KBS 1TV`, "KBS 1TV의 채널에서 광고 24개를 때려갔다.", `1988년`/`문화`, 42640701, `Q777278` ③ `title` `KBS 2TV`, "KBS 2TV의 채널에서 광고 37개를 때려갔다.", `1988년`/`문화`, 42640701, `Q624509`. 모두 `normal`/`curated`/`domestic`, `media: []`. `day_media: []`, `notice: null`, `partial: false` |
| `world2002` | 지금 서버와 같은 모양. 사건 1개: `kowiki-2002-06-22-04eaf6a6`, `title` `2002년 FIFA 월드컵`, `6월 22일`/`문화`, 42614972, `Q47735`, `normal`/`curated`, `summary` 에 "스페인", `media: []` |
| `world1995` | 서버 예시 그대로: `events: []`, `day_media: []`, `notice: {"code":"NO_EVENTS_FOUND"}`, `partial: false` |
| `world1988WithMedia` | `world1988` + 사건 ①의 `media` = [**PD 실제 예시**(`commons-1988-09-17-seoul-olympics-opening-pd.json`, `thumb.wikimedia.org` 주소 그대로) + `date_relation {same_day, 0}`, **촬영 시점 미상 예시**(`example-photo-capture-time-unknown.json`) + `{unknown, 0}`]. `day_media` = [라이선스 미확인 예시(`example-photo-license-unknown.json`, 1988-09-17 `day`) + `{same_day, 0}`, 촬영 시점 미상 예시의 사본(`id` 끝에 `-day`) + `{unknown, 0}`] — `day_media` 는 M3b2b가 쓴다 |
| `world2002WithMedia` | `world2002` + 사건의 `media` = [**KOGL 제1유형 실제 예시**(`commons-2002-06-22-gwangju-world-cup-kogl1.json`) + `{same_day, 0}`] |

- 값의 출처: 사건은 M3a2 계획서의 응답 예시·데모 표(위키백과 문장, CC BY-SA 4.0). 1988 KBS 두 줄의 `id` 해시, `related_links`, `retrieved_at`, 2002 문장의 정확한 글자는 **합성 또는 녹화 위키텍스트에서 손으로 옮긴 값**일 수 있다 — 서버 응답과 글자가 다를 수 있으며 웹은 그 값에 기대지 않는다. 미디어는 M3a3 `valid/` 예시의 값을 그대로 옮기고 `date_relation` 만 더한다(실제 Commons 예시 2개는 원 URL·확인 일자 2026-10-13·라이선스가 `data/schema/examples/media/README.md` 에 있다). 미디어 파일은 내려받거나 복사하지 않는다(메타데이터만).
- `web/src/mocks/fixtures/world/README.md` 에 위 출처와 "합성 값" 목록을 적는다.
- 기본 핸들러(`web/src/mocks/handlers.ts`)는 `date` 쿼리로 고른다: `1988-09-17` → `world1988WithMedia`, `2002-06-22` → `world2002WithMedia`, `1995-03-15` → `world1995`, 그 밖 → 500(본문 없음). 지금 서버 모양(`world1988`·`world2002`)과 에러는 테스트에서 `server.use(...)` 로 바꿔 끼운다.

### 화면 구조와 문구

```
그날의 세계                               (h2, 장 제목 — M3b1)
 ┌ 사건 ───────────────────────────────┐   (사건 목록 ul, 사건마다 li)
 │ 제24회 서울 하계 올림픽 개막.            │  ← summary 원문 그대로 (글자)
 │ [MediaItem × 사건 media 개수]          │  ← 바로 표시. media 가 [] 면 이 자리 없음
 │ 출처: 한국어 위키백과 「9월 17일」 ·       │  ← source_name + 「source_title」(source_url 링크)
 │       CC BY-SA 4.0                    │     + text_license(text_license_url 링크)
 └─────────────────────────────────────┘
 ┌ KBS 1TV의 채널에서 광고 24개를 때려갔다. ┐  ← 원문 그대로 (M3-P16)
 │ 출처: 한국어 위키백과 「1988년」 · CC BY-SA 4.0 │
 └─────────────────────────────────────┘
 (M3b2b: 그날 찍힌 사진 / 촬영 시점을 확인하지 못한 사진 / 같은 날 있었던 아픈 일)
```

| 자리 | 문구 (PM이 T5에서 검토) |
|---|---|
| 빈 날짜 안내 | 이 날짜에 기록된 큰 사건을 찾지 못했습니다. |
| 사건 목록 접근 이름 | 사건 목록 |
| 문장 출처 줄 | 출처: {source_name} 「{source_title}」 · {text_license} |
| 출처 줄 링크의 숨은 글자 | (새 창에서 열림) |
| 사건 미디어 대체 텍스트(`fallbackAlt`) | `title` 이 있으면 「{title}」 관련 자료, 없으면 사건 관련 자료 |

- 출처 줄의 두 링크는 `target="_blank" rel="noopener noreferrer"` 이고, 링크 안에 공용 `visually-hidden` 클래스(이 기능이 `global.css` 에 더함)의 "(새 창에서 열림)"이 있다.
- 사건 카드 안의 미디어가 `unknown` 이면 카드 캡션에 "촬영 시점 미상"(M3b1b)이 그대로 보인다. 사건 문장이 그날 일이라는 이유로 사진을 그날 사진처럼 설명하지 않는다(`fallbackAlt` 에도 날짜 표현 없음).
- 사건 문장은 React 글자 노드로만 넣는다. 문장 안에 `<`·`>` 같은 글자가 있어도 태그가 되지 않는다.

### 임시 데모 날짜 버튼 (M2b가 지움)

- 입력 화면의 "빈 결과 화면 보기" 버튼을 데모 날짜 버튼 3개로 바꾼다: "1988년 9월 17일로 보기", "2002년 6월 22일로 보기", "1995년 3월 15일로 보기". 제품 정의서 12절 데모 날짜만 쓴다(표본 날짜 버튼은 M3b3에서 더한다).
- `App` 상태는 `{ screen: 'input' } | { screen: 'story', date: string }`. `StoryScreen` 이 `date` 를 받아 `world` 섹션에만 넘기고 나머지 4개 섹션은 `empty`.
- "처음으로" 로 돌아오면 진행 중이던 요청은 취소된다.
- **날짜를 URL·`localStorage` 에 넣지 않는다**(C4). 결과 화면 주소에 날짜·장소가 들어가면 같은 페이지의 외부 스크립트(YouTube IFrame API, M3b3c)가 읽을 수 있다(리서치 위험 26). 결과 URL 설계는 M2b.

## 테스트 방침

M1b "테스트 방침"을 따른다. 추가:
- MSW 응답은 위 fixture 모듈. 지금 서버 모양(`media: []`)과 미디어가 든 모양을 **둘 다** 시험한다.
- **계약 시험**: fixture의 키 집합이 서버 키 집합(최상위 5, 사건 20, 미디어 27)과 같다. 서버 키 목록은 계획서 표에서 옮겨 적고 `types.ts` 에서 만들지 않는다(M3b1b `types.test.ts` 방식).
- `tragic` 사건 시험용 응답은 테스트 파일 안에서 만들고 문장은 "시험용 비극 사건 문장"처럼 누가 봐도 가짜로 쓴다. 태그가 든 문장 시험도 "시험용 <b>문장</b>"처럼 가짜로 쓴다. 기본 핸들러에는 넣지 않는다.
- 400 시험은 `DATE_NOT_EXIST` 본문과 경계값 날짜 `2001-02-29`(없는 날짜, 누구의 생일도 아님)만 쓴다. 그 밖 날짜는 데모 3개.
- **effect 요청 테스트는 끝을 기다린다**: 마지막 화면 상태를 `findBy*`/`waitFor` 로 확인한 뒤 끝낸다(C3).
- 시간 초과는 작은 `timeoutMs`(예: 20ms)와 MSW `delay()` 로 시험한다. 가짜 타이머는 MSW·`fetch` 와 섞으면 멈출 수 있어 쓰지 않는다(써야 하면 보고).

## 데모 입력 3개로 확인할 것

| 데모 입력 | 자동 테스트 (MSW) | PM 화면 확인 — T5 (실제 서버, PR 검수 때, 사진 없음) | PM 화면 확인 — T6 (M3a3b 머지 뒤, 실제 사진) |
|---|---|---|---|
| 1988-09-17 + "서울" | 버튼 → 사건 3개가 이 순서로: "서울 하계 올림픽"이 든 문장(이 문구는 1번만), "KBS 1TV의 채널에서 광고 24개를 때려갔다.", "KBS 2TV의 채널에서 광고 37개를 때려갔다."(글자 그대로). 출처 줄 3개(링크 `href` 가 `source_url`·`text_license_url`). 미디어가 든 fixture: 사건 ①에 `img` 2개 — 하나는 `src` 호스트가 `thumb.wikimedia.org` 이고 캡션 "같은 날", 다른 하나는 "촬영 시점 미상"이고 "같은 날" 배지 없음. `day_media` 사진은 그려지지 않음. 지금 서버 모양 fixture: `img` 0개 | 사건 3개, 출처 줄, 사진 없음(서버가 아직 `[]`) | 사건 사진이 실제 사진으로. 사진마다 캡션(날짜 관계, 촬영 시점, 출처, 라이선스 상태). M3b1b 카드를 실제 사진으로 처음 보는 자리 |
| 2002-06-22 + "광주 서구 풍암동" | "스페인"이 든 문장(1번만), 출처 줄 `「6월 22일」`. 미디어가 든 fixture: KOGL 제1유형 사진 1장, 캡션에 "KOGL Type 1" | 문장과 출처 줄 | 사건 사진(있으면) |
| 1995-03-15 + "서울대학교병원" | "이 날짜에 기록된 큰 사건을 찾지 못했습니다.", `img` 0개, 섹션 제목 "그날의 세계" 보임 | 같은 내용 | 같은 내용 |

## 크기 (400줄 규칙)

### 실측 비율

| 기능 | 예상 → 실제 (S1) | 비율 |
|---|---|---|
| M3b1 (웹) | 225 → 309 | 약 1.4배 |
| M3b1b (웹) | 165 → +158 | 약 1.0배 |
| M3a1b·M3a2·M3a1c·M3a3 T2 (서버) | 150 → 366, 390 → 1,094, 210 → 787, 130 → 505 | 약 2.4·2.8·3.7·3.9배 |

웹은 지금까지 예상과 가까웠고(1.0~1.4배) 서버만 크게 넘었다(2.4~3.9배). 서버 비율은 외부 호출 오류 처리·설정 검증·캐시처럼 예상에서 빠지기 쉬운 코드가 많아서다. 이 기능은 웹이지만 처음으로 비동기 요청·취소·시간 초과를 다루므로, 이전 셈법(약 240) 대신 **파일별로 다시 세고**(Prettier 80칸 기준) 웹 비율 1.0~1.4배를 범위로 둔다.

### 다시 센 값

| 작업 | 내용 | 다시 셈 | 범위 (×1.0~×1.4) |
|---|---|---|---|
| T1 | `client.ts` 옵션·신호 합치기·`ApiTimeoutError`·problem `code` 읽기 약 45, `getWorld` 약 10 | 약 55 | 55~75 |
| T1b | `world/types.ts` 약 50, `media/types.ts` 고침 약 6, `handlers.ts` 약 20 (fixture 모듈은 S1에서 뺌) | 약 75 | 75~105 |
| T2 | `WorldSection`(요청·상태·빈 날짜) 약 70, 사건 항목·출처 줄·`fallbackAlt` 약 55, CSS 약 45, `visually-hidden` 약 12 | 약 180 | 180~250 |
| T3 | `App` 약 10, `StoryScreen` 약 15, `InputScreen` 버튼 3개 약 20, CSS 약 8 | 약 55 | 55~80 |
| **합계** | | **약 365** | **365~510** (가운데 약 440) |

- 참고: 이전 셈법 240에 서버 비율(×2.8~×3.9)을 곱하면 670~940이다. 웹 실측과 맞지 않아 쓰지 않았다.
- **한 PR (PM 결정 1 (a), 2026-10-13)**. **T1b 뒤 확인점**: T1·T1b 누적 S1이 **170**(다시 셈 130 × 1.3)을 넘으면 오케스트레이터가 멈추고, T1·T1b만 이 PR로 내고 T2·T3을 새 기능(M3b2a)으로 옮길지 PM에게 다시 묻는다. 넘지 않으면 그대로 T2로 간다.
- 테스트(`*.test.ts(x)`)와 fixture(`web/src/mocks/fixtures/**`)는 S1에서 뺀다.

## 작업 목록

순서: T1 → T1b → **확인점 (S1 170)** → T2 → T3 → T4 → T5(PR 검수 때) → 머지 → T6(M3a3b 머지 뒤, `main` 에서 — 결정 2 (a)). 서버 M3a3a와 폴더가 달라 병렬이다. **번호**: 이전 T1이 T1·T1b로 갈라졌고 T2~T5는 그대로, T6이 새로 생겼다.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | API 클라이언트 시간 초과·취소·problem `code`, `getWorld` | frontend-dev | `web/src/api/client.ts`, `web/src/world/api.ts`(또는 `client.ts` 안), 테스트 | - (M3a2·M3a3·M3b1b 머지됨) | (1) 기존 `client.test.ts`·`network.test.ts` 를 고치지 않고 통과. (2) 새 테스트: `timeoutMs` 보다 늦으면 `ApiTimeoutError`. 호출자 `abort()` 면 `AbortError`(`ApiTimeoutError` 아님). `getWorld('1988-09-17')` 의 요청 경로가 `/api/story/world`, 쿼리 `date=1988-09-17`. (3) 400 problem+json 본문(`code` `DATE_NOT_EXIST`, 날짜 `2001-02-29` 요청) → `ApiError` `status` 400, `code` `DATE_NOT_EXIST`. 503 본문(`UPSTREAM_UNAVAILABLE`) → `status` 503, `code` `UPSTREAM_UNAVAILABLE`. 본문이 JSON이 아닌 500 → `code` `null`. (4) 위 오류마다 `message` 에 요청 날짜 문자열(`1988-09-17`, `2001-02-29`)이 없다. (5) 요청의 `Accept` 헤더에 `application/json` 과 `application/problem+json` 이 있다. (6) `cd web && npm run lint && npm run format:check && npm run typecheck && npm test` 통과, 보고서에 T1 S1 |
| T1b | 세계 응답 타입, 미디어 타입 고침, fixture, 기본 핸들러 | frontend-dev | `web/src/world/types.ts`, `web/src/media/types.ts`, `web/src/media/types.test.ts`(기대 형), `web/src/mocks/fixtures/world/*.ts`·`README.md`, `web/src/mocks/handlers.ts`, 테스트 | T1 | (1) `WorldResponse`·`WorldEvent`·`ApiProblemCode` 가 위 "응답 200"·"에러"의 이름·형과 같다 — 계획서 표에서 쓴 기대 형과 `Equal` 타입 시험(`tsc -b`). (2) **미디어 타입 고침 표** 3행(`height`, `zoom`, `basis`)이 반영되고 `selection` 은 선택 필드 그대로. `types.test.ts` 의 기대 형을 같은 표대로 바꾸고 통과(지운 시험 없음, 바꾼 줄은 보고에 표). (3) 보고서에 **서버 미디어 DTO(M3a3 T4 목록)와 `web/src/media/types.ts` 의 대조표**(필드 27개 + `video_sources[]`·`render`·`date_relation` 안쪽, 이름·형·null 가능). 위 표 밖의 차이가 있으면 웹을 고치고 보고(서버는 바꾸지 않음). (4) fixture 5개가 `satisfies WorldResponse` 로 타입 검사 통과. (5) 계약 시험: fixture마다 최상위 키 = 5개, 사건 키 = 20개, 미디어 키 = 27개(`selection` 없음, `null` 값도 키가 있음). (6) `world1988` 의 사건 3개가 위 fixture 표의 `id`(①)·`title`·`summary`(②·③ 글자 그대로)·`source_title`·`source_section`·`source_revision`·`wikidata_qid` 와 같고 `media` 가 모두 `[]`. `world1995` 는 `notice.code` `NO_EVENTS_FOUND`. (7) `world1988WithMedia` 사건 ①의 첫 미디어 `thumbnail_url` 이 `https://thumb.wikimedia.org/` 로 시작하고, 미디어 값이 `valid/` 예시와 `date_relation` 을 뺀 모든 키에서 같다(손으로 옮긴 값을 시험으로 대조). (8) 기본 핸들러: `1988-09-17`·`2002-06-22`·`1995-03-15` 가 표대로, 그 밖은 500. (9) README에 값 출처와 합성 값 목록. (10) 웹 전체 검사 통과, 보고서에 T1b S1과 **T1·T1b 누적 S1 — 확인점 170** |
| T2 | `WorldSection`: 요청·상태, `normal` 사건 항목, 출처 줄, 빈 날짜 안내 | frontend-dev | `web/src/world/WorldSection.tsx`, 하위 컴포넌트, `*.module.css`, `web/src/styles/global.css`(`visually-hidden`), 컴포넌트 테스트 | T1b (확인점 통과) | (1) 요청 중 `aria-busy="true"` 와 "불러오는 중…", 끝나면 "응답과 화면" 표대로. (2) `world1988WithMedia`: "사건 목록" 안 `listitem` 3개가 fixture 순서대로, 각 문장이 `summary` 와 글자 하나까지 같다(KBS 두 줄 포함). 출처 줄 3개 — 링크 `href` 가 각 사건의 `source_url`·`text_license_url`, 링크 접근 이름에 "새 창에서 열림". 사건 ①에 `img` 2개: 하나는 `src` 가 `thumb.wikimedia.org` 주소이고 캡션의 날짜 줄이 "같은 날" 배지를 담고, 다른 하나는 날짜 줄이 "촬영 시점 미상"이고 "같은 날" 배지가 없다. (3) `world1988`(지금 서버 모양): `listitem` 3개, `img`·`figure` 0개. (4) `world1995`: 안내 문장, `region` "그날의 세계" 남음, `img` 0개. (5) 합성 응답 [`normal` A, `tragic` "시험용 비극 사건 문장"(`media` 에 사진 1장 넣음)]: `tragic` 문장이 화면에 없고 `img`·`iframe`·`video` 가 그 사건 몫으로 생기지 않는다. `normal` 없이 `tragic` 만 + `notice: null` → 안내 문장도 목록 항목도 없다. (6) `day_media` 사진(`world1988WithMedia` 의 `-day` 사진 포함)이 그려지지 않는다. (7) 합성 `summary` "시험용 <b>문장</b>" → 화면 글자에 `<b>` 가 그대로 보이고 `b` 요소가 생기지 않는다. (8) 503(`UPSTREAM_UNAVAILABLE`, `detail` 있음)·400(`DATE_NOT_EXIST`)·시간 초과(작은 `timeoutMs`)·네트워크 오류 → "이 섹션을 불러오지 못했습니다.", 화면에 `detail`·`code`·날짜 문자열 없음. (9) 언마운트하면 요청 취소, 경고 없음. 날짜 prop이 바뀌면 이전 요청 취소 후 다시 부름(늦게 온 이전 응답이 화면을 덮지 않음). (10) `MediaItem` 에 넘기는 `fallbackAlt` 가 "「1988년 하계 올림픽」 관련 자료"이고, 모든 `fallbackAlt` 에 "그날"·"같은 날"·"당일"·요청 날짜 문자열("1988-09-17", "1988년 9월 17일", "9월 17일")이 없다. `title` 이 `null` 인 합성 사건 → "사건 관련 자료". (11) 모든 요청 테스트가 마지막 상태를 기다린다(C3). (12) C6 출력 없음, 웹 전체 검사 통과, 보고서에 누적 S1 |
| T3 | 임시 데모 날짜 버튼과 화면 연결 | frontend-dev | `InputScreen.tsx`, `App.tsx`, `StoryScreen.tsx`, `Screen.module.css`, 기존 테스트 수정 | T2 | (1) 버튼 3개("1988년 9월 17일로 보기", "2002년 6월 22일로 보기", "1995년 3월 15일로 보기"), "빈 결과 화면 보기" 없음. (2) `App` 테스트: "1988년 9월 17일로 보기" → region 5개, `world` 에 1988 사건 3개(MSW), 다른 4개는 "아직 준비 중인 섹션입니다." → "처음으로" → 입력 화면, 진행 중 요청이 취소된다(응답을 늦춘 핸들러로). "1995년 3월 15일로 보기" → 빈 날짜 안내. (3) C4 출력 없음. (4) 기존 `App.test.tsx`·`StoryScreen.test.tsx` 는 바뀐 화면에 맞게 기대값만 고친다(지운 시험 없음, 바꾼 것은 보고에 표). (5) 웹 전체 검사 통과, S1 보고 |
| T4 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 | T3 | (1) T1·T1b·T2·T3 기준 표, 빠진 시험 추가. (2) 웹 전체 검사 통과, `npm test` 3회 반복 안정. (3) C1·C2·C4·C5·C6 출력 없음, C3 출력 `0`. (4) S1·S2 보고. (5) "데모" 표의 자동 테스트 열 3행이 시험으로 덮였는지 표로 |
| T5 | 실제 서버로 화면 확인과 문구 검토 (PR 검수 때) | PM | - | T4 | (1) 루트 `.env` 에 `WIKIMEDIA_USER_AGENT`, DB·서버·웹 실행. (2) 데모 버튼 3개로 "데모" 표 T5 열과 맞는지(1988은 사건 3개, KBS 두 줄은 원문 그대로 나오는 것이 정상, 사진은 아직 없음), 360px 폭 가로 스크롤 없음. (3) 서버를 끄고 누르면 `world` 섹션만 오류 문구. (4) 문구 검토(빈 날짜 안내, 출처 줄, "사건 목록"). 카드 모양·"그날 찍힌 사진"·비극 블록은 T6·M3b2b에서 본다. T5가 끝나면 머지할 수 있다(결정 2 (a)) |
| T6 | 실제 사진으로 화면 확인 (머지 뒤, M3a3b 머지 뒤 `main` 에서) | PM | - | 이 PR 머지, **M3a3b 머지** | (1) `main` 을 받아 DB·서버·웹 실행. (2) 데모 버튼으로 "데모" 표 T6 열: 사건 사진이 원 출처에서 바로 보이고, 캡션마다 날짜 관계·촬영 시점·출처·라이선스 상태. 촬영 시점 미상 사진은 "촬영 시점 미상"이고 "같은 날" 배지가 없다. (3) 문제가 있으면 오케스트레이터에게 알린다 — M3b2b 범위면 M3b2b에서, 아니면 `fix/` 브랜치로 |

완료 기준에 쓰는 명령 (저장소 루트):

```
# C1 — 제품 코드에 외부 절대 URL 없음 (테스트·목 제외). 출력 없음
grep -rnE 'https?://' web/src web/index.html --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/mocks/'
# C2 — 환경변수 안 씀 (M4 전까지). 출력 없음
grep -rn 'import.meta.env' web/src
# C3 — act 경고 없음. 0
cd web && npm test 2>&1 | grep -c 'not wrapped in act'
# C4 — 날짜를 URL·브라우저 저장소에 넣지 않음. 출력 없음
grep -rnE 'localStorage|sessionStorage|history\.(push|replace)State|location\.(search|hash)' web/src --exclude='*.test.ts' --exclude='*.test.tsx'
# C5 — 제품 코드에서 콘솔에 남기지 않음 (요청 날짜가 로그에 남지 않게). 출력 없음
grep -rnE 'console\.(log|info|warn|error|debug)' web/src --exclude='*.test.ts' --exclude='*.test.tsx' | grep -v '^web/src/test/'
# C6 — 원문을 HTML로 넣지 않음. 출력 없음
grep -rn 'dangerouslySetInnerHTML' web/src
# S1 — 제품 코드 줄 수 (M3b1 계획서 S1 + 목 fixture 제외)
git diff --numstat main...HEAD -- web \
  ':(exclude)web/package-lock.json' \
  ':(exclude,glob)web/src/**/*.test.ts' ':(exclude,glob)web/src/**/*.test.tsx' \
  ':(exclude,glob)web/src/mocks/fixtures/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수 (최상위 App.test.tsx 포함)
git diff --numstat main...HEAD -- ':(glob)web/src/**/*.test.ts' ':(glob)web/src/**/*.test.tsx' \
  | awk '{a+=$1} END {print "test added", a}'
```

담당별: frontend-dev 4 (T1, T1b, T2, T3), test-engineer 1 (T4), PM 2 (T5, T6).

## 리스크와 확인할 것

1. **"그날의 세계"에 세계 사건이 없다** — #20 (a)로는 데모 날짜에 국내 사건만 나오거나 비어 있다. PM이 T5에서 본 뒤 #20 후속을 정한다.
2. **어색한 원문이 그대로 보인다 (의도된 동작)** — 1988-09-17의 KBS 두 줄. M3-P16(2026-10-12)대로 거르지도 고치지도 않는다. 다듬기를 할지는 로드맵 후속 후보("화면 표시 때의 위키백과 문장 해석·다듬기")에서 #23 (a)·`summary_modified` 와 함께 따로 정한다.
3. **중간 상태** — 이 기능만 머지되면 `tragic` 사건과 같은 날 사진이 화면에 없다. M3b2b를 이어서 한다. 데모 3개 날짜에는 `tragic` 사건이 없다.
4. **실제 사진 확인이 늦다** — 서버가 M3a3a → M3a3b 두 PR 뒤에야 사진을 보낸다. 그전까지 카드는 MSW 시험으로만 확인된다. fixture를 실제 Commons 예시 값으로 만들어 차이를 줄였다. T6에서 발견되는 것은 M3b2b나 `fix/` 로 고친다(결정 2 (a)).
5. **`AbortSignal.any` 와 jsdom** — Node 24·jsdom 30에는 둘 다 있지만, jsdom의 `AbortSignal` 을 Node `fetch` 에 넘기면 "Expected signal to be an instance of AbortSignal" 오류가 날 수 있다. 문제가 나면 컨트롤러 두 개를 잇는 방식으로 바꾸고 보고한다.
6. **세계 섹션 응답이 느릴 수 있다** — 서버 최악 약 10초(M3a2 리스크 4), M3a3b 뒤 약 13~15초(로드맵 후속 후보 "섹션 전체 시간 상한"). 웹 시간 초과 10초(제품 정의서 8절)에 걸리면 섹션 오류가 된다. M3a3b 뒤 T6에서 보이면 알리고, M6b에서 측정해 서버 섹션 상한 또는 웹 시간 초과를 다시 정한다.
7. **이미지 호스트 허용 목록·CSP가 없다** — 지금은 `http(s)` 확인만 한다(M3b1b `isDisplayable`). 나중에 허용 목록이나 `img-src` CSP를 두면 Commons 썸네일 호스트 `thumb.wikimedia.org`(960px 단계, M3a3 리뷰 권장 6)와 원본 `upload.wikimedia.org` 를 넣어야 한다. T1b (7)·T2 (2)가 `thumb.wikimedia.org` 주소가 그려지는 것을 시험으로 남긴다.
8. **개발 모드의 두 번 요청** — `main.tsx` 의 `StrictMode` 는 개발 중 effect를 두 번 돌린다. 첫 요청은 취소되므로 화면은 같지만 서버 로그에 요청이 두 번 보일 수 있다(테스트에는 `StrictMode` 가 없다).
9. **임시 버튼이 남는다** — M2b 범위에 "데모 버튼 제거"가 있다(로드맵).
10. **M3a4가 `selection` 을 더하면** — 웹 타입은 이미 선택 필드다. 그때 계약 시험의 미디어 키 집합(27)을 M3a4 계약대로 고친다(M3b3).
11. **줄 수가 범위 위쪽으로 갈 수 있다** — 가운데 약 440으로 400을 넘을 수 있다. PM이 한 PR((a))로 정했고, T1b 뒤 확인점(170)에서 넘으면 다시 묻는다.

## PM 결정 (2026-10-13, 기록)

| # | 물은 것 | 결정 | 이 계획서에 반영 |
|---|---|---|---|
| 1 | 크기(다시 셈 약 365, 범위 365~510) — 한 PR로 갈지 | **(a) 한 PR + T1b 뒤 확인점(누적 S1 170)**. planner 추천과 같음. 고르지 않은 것: (b) 지금 둘로(M3b2 API 계층 / M3b2a 화면), (c) 확인점 없이 한 PR | "크기", 작업 순서의 확인점, T1b (10) |
| 2 | PR 머지와 실제 사진 확인 시점 | **(a) T5(실제 서버, 사진 없이)로 머지, 실제 사진 확인은 T6 — M3a3b 머지 뒤 `main` 에서**. planner 추천과 같음. 고르지 않은 것: (b) M3a3b 머지까지 PR을 열어 둠 | 머리말 선행, T5·T6, 리스크 4 |

이전 결정(M3-P5·M3-P17, 분할 D1 (a) 2026-10-12, 속도 개선 A 2026-10-13)은 위 "전제" 표. 남은 질문은 없다.

★ PM 직접 구현: 없음 — 이 기능에는 백엔드 작업이 없다.
