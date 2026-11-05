# 그때 vs 지금: 공용 수치 형식과 첫 두 항목 (C3)

> 브랜치(차례로, 모두 `main` 에서): 서버 `feat/then-now-format` → `feat/statcan-wds-client` → `feat/cpi-then-now` → `feat/min-wage-source` → `feat/min-wage-minutes`, 웹 `feat/then-now-ui` (서버와 병렬) · 작성: planner (2026-11-05) · 상태: **초안 (PM 위임 — "추천"이 진행할 안)**
> 기준: `docs/research/canada-money.md`(T5) 2~4·7·8·10절, `canada-local-figures.md`(T6) 4·7절, `canada-culture.md`(T7) 4절, `canada-summary.md` 1.2·6.3·7절, 리서치 계획 결정 6 (a)(수치의 정직한 표기), PM 위임 결정(2026-11-04: A1 (a) 1차 항목 1·2·3·6·11, 시작 날짜 1950-01-01), `CLAUDE.md` 7절, 머지 코드(`StoryDate`, `ApiProblem`, `WorldStoryResponse` 의 snake_case·`partial`, 미디어 형식의 R1~R9 검사 방식(`MediaRecord`·`MediaNormalizer`), `AppProperties.dataPath()`, 웹 `api/client.ts`·`story/sections.ts`·`world/SourceLine.tsx`·`components/NewTabLink.tsx`).
> 장소 쪽 계약은 `docs/plans/c2-ca-place-resolve.md` 다(`province.code` = SGC 두 자리).

## 목표

1. **공용 수치 형식**을 정한다. 값마다 기간, 정밀도(`day`/`week`/`month`/`year`/`census`), 지역, 출처·라이선스·표시 문구를 가진다. 연·월 값은 화면에 "That year"·"That month"로 쓰고 **그날 값처럼 보이지 않게** 한다. 출처 칸이 빈 값은 보이지 않는다(미디어와 같은 규칙).
2. **항목 2 — $100 그때 → 지금**: StatCan CPI 벡터 41690973(WDS, 확인됨), BoC 계산기 식(새 = 100 × 최신 달 CPI ÷ 그달 CPI). 날짜만 필요하다.
3. **항목 3 — 최저임금으로 일한 분**: ESDC 역대 최저임금 CSV(확인됨), 주 단위. 그날 시행 중인 요율과 오늘 요율, 그리고 빵 675 g·우유 1 L 값(StatCan, 캐나다 평균, 1995년부터)을 사려면 몇 분 일해야 하는지.
4. 이야기 화면에 "Then and now" 섹션과 공용 수치 카드를 둔다.

## 범위 밖

- 장바구니 휘발유(도시 필요 → C2의 `csd` → 휘발유 도시 대응표 뒤, C5), 영화표·우표(T5 D3 (a) 뺌), 금리·환율(항목 7, C4), 소득.
- 항목 1·6·11과 나머지 항목 → 아래 "다음 PR".
- 웹이 `province` 를 보내는 일 → `feat/place-picker` (C2 계획). 이 계획의 웹은 `province` 없이 부른다.
- JSON 스키마 파일: 수치 레코드는 실행 중 어댑터가 만든다. `data/` 에 수치 큐레이션 파일이 처음 생기는 PR(`feat/baby-names`)에서 `data/schema/figure.schema.json` 을 더한다.
- 제품 정의서 5절(이야기 순서)·6.4절 수정은 PM 몫(`docs/plans/brief-changes-canada.md`). 이 계획은 결정 6 (a)를 그대로 쓴다.

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 곳 | 지킬 조건 (리서치 그대로) |
|---|---|---|---|
| StatCan WDS `getDataFromVectorByReferencePeriodRange` (GET) ([T5 3절](../research/canada-money.md#3-출처-카드)) | 확인됨 | 항목 2·3 | 키 없음. IP당 25회/초. 영업일 00:00~08:30 ET에는 값이 없거나 409일 수 있음 → 그 항목만 비움. 문서상 이름이 엇갈리는 POST 메서드는 쓰지 않는다. 최신 값도 이 GET으로 기간을 넓혀 받는다 |
| CPI 41690973 (표 18-10-0004-01, 2002=100, 1914-01~) | 확인됨 | 항목 2 | 월 정밀도. 계절조정 안 한 CPI는 개정되지 않음. "approx."와 BoC 계산기 링크, CPI는 생활비 지수가 아님 |
| 빵 675 g·우유 1 L: 옛 표 18-10-0002-01 (v735175, v735169, 1995-01~2022) / 새 표 18-10-0245-01 (v1353834326, v1353834283, 2017-01~) | 확인됨 | 항목 3 | 표가 바뀌므로 값마다 표 이름을 쓰고 그때/지금 비율은 만들지 않는다(T5 D2 (a)) |
| ESDC 역대 최저임금 CSV ([T5 3절](../research/canada-money.md#3-출처-카드)) | 확인됨 | 항목 3 | OGL-Canada. 302 리다이렉트를 따라감. 같은 날 여러 행은 규칙 없이 고르지 않음. 미래 시행일은 "지금"에서 뺌. 리서치 값은 WebFetch 요약을 거쳤으므로 녹화 때 원문으로 다시 확인(T5 W5) |
| BoC 인플레이션 계산기 쪽 | 확인됨 (계산법) | 항목 2 | 계산법 링크로만 쓴다. 호출하지 않는다 |

표시 문구:
- StatCan(가공): `Adapted from Statistics Canada, <표 번호와 이름>, <releaseTime 날짜>. This does not constitute an endorsement by Statistics Canada of this product.` 가공하지 않은 값을 그대로 보이면 `Source: Statistics Canada, <표>, <날짜>. Reproduced and distributed on an "as is" basis with the permission of Statistics Canada.` 이 계획의 값은 모두 계산·선택을 거치므로 "Adapted from"을 쓴다.
- ESDC: `Contains information licensed under the Open Government Licence – Canada.` + 라이선스 링크.

캐싱(리서치 T5 D6 (a): 과거 값은 고정, "지금"만 다시 부름. 라이선스는 저장 기간 제한 없음): C3-P2 참고. 녹화·내려받기의 User-Agent는 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)` 하나뿐이다.

## API / 데이터 변경

### 수치 레코드 `Figure` (C3a)

| 필드 | 형식 | 규칙 |
|---|---|---|
| `role` | `then` / `now` | |
| `value` | 숫자 | 유한한 수(F3) |
| `unit` | 문자열 | 비지 않음. 예 `CAD`, `CAD per hour`, `minutes`, `Index (2002=100)` |
| `period` | 문자열 | 정밀도에 맞는 형식(F2): `day`·`week` `YYYY-MM-DD`(주는 관측일), `month` `YYYY-MM`, `year`·`census` `YYYY` |
| `period_precision` | `day`/`week`/`month`/`year`/`census` | 출처보다 세밀하게 쓰지 않는다(어댑터 책임, 시험으로 확인) |
| `geography` | 문자열 | 예 `Canada`, `Alberta` |
| `series_id` | 문자열 또는 `null` | 예 `41690973`. ESDC는 `null` |
| `approximate` | 불 | `true` 면 `method` 필수(F4) |
| `method` | 문자열 또는 `null` | 예 `100 × CPI (latest month) ÷ CPI (that month)` |
| `method_url` | https 또는 `null` | 예 BoC 계산기 쪽 |
| `note` | 문자열 또는 `null` | 짧은 영어. 예 `In effect since 1981-05-01`, `Table 18-10-0002-01 (archived)` |
| `boundary_vintage`, `kind`, `is_preliminary`, `quality_flag` | `null` 가능 | C3에서는 늘 `null`/`false`. 인구·날씨 PR이 채운다(T6 4절). 모양이 나중에 바뀌지 않게 지금 둔다 |
| `sources[]` | 1개 이상 | 각각 `name`, `title`, `url`(https), `license`, `license_url`(https), `attribution` 이 모두 비지 않음(F1). 하나라도 어기면 그 수치는 빠진다 |
| `retrieved_at` | ISO-8601 UTC, 초 단위 | |

규칙 F1~F6 (`FigureRules`, 미디어 R1~R9처럼 어긴 레코드를 버리고 이유를 DEBUG로 남김. 값·날짜·장소는 남기지 않음):
- F1 위 출처 칸. StatCan 출처의 `attribution` 은 `Source: Statistics Canada, ` 또는 `Adapted from Statistics Canada, ` 로 시작하고, "Adapted"면 보증 아님 문장으로 끝난다.
- F2 `period` 형식 = 정밀도.
- F3 `value` 유한, `unit` 비지 않음.
- F4 `approximate` → `method` 비지 않음.
- F5 `now` 의 `period` 는 `StoryDate.TODAY_ZONE` 의 오늘보다 뒤가 아니다(ESDC 미래 시행일 차단).
- F6 한 항목의 `then` 과 `now` 가 다른 표·경계 판에서 왔으면 항목의 `comparable` 은 `false` 이고 서버는 배수·비율을 만들지 않는다.

### `GET /api/story/then-and-now?date=YYYY-MM-DD[&province=NN]` (C3c, `province` 는 C3f)

- `date` 검사는 `StoryDate.parse` 를 그대로 쓴다(같은 400 코드 5개).
- `province`: 없어도 된다. 있으면 SGC 두 자리 13개 중 하나(`10,11,12,13,24,35,46,47,48,59,60,61,62`). 아니면 400 `PROVINCE_INVALID` (`detail` "province must be a two-digit SGC province or territory code"). 이름은 받지 않는다. 13개는 상수라 C2 코드에 의존하지 않는다.
- 응답 snake_case, `Cache-Control: no-store`, 항목 순서 고정(`cpi-100` → `min-wage` → `min-wage-minutes-milk` → `min-wage-minutes-bread`).

```
GET /api/story/then-and-now?date=1988-02-13&province=48
200
{
  "date": "1988-02-13",
  "items": [
    {
      "id": "cpi-100",
      "status": "OK",
      "comparable": true,
      "basis": [
        { "series_id": "41690973", "period": "1988-02", "value": 70.0, "unit": "Index (2002=100)" },
        { "series_id": "41690973", "period": "2026-09", "value": 170.0, "unit": "Index (2002=100)" }
      ],
      "figures": [
        { "role": "then", "value": 100.00, "unit": "CAD", "period": "1988-02", "period_precision": "month",
          "geography": "Canada", "series_id": "41690973", "approximate": false, "method": null, "method_url": null, "note": null,
          "boundary_vintage": null, "kind": null, "is_preliminary": false, "quality_flag": null,
          "sources": [ { "name": "Statistics Canada", "title": "Table 18-10-0004-01 Consumer Price Index, monthly, not seasonally adjusted",
                         "url": "https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1810000401",
                         "license": "Statistics Canada Open Licence", "license_url": "https://www.statcan.gc.ca/en/terms-conditions/open-licence",
                         "attribution": "Adapted from Statistics Canada, Table 18-10-0004-01 Consumer Price Index, monthly, not seasonally adjusted, 2026-10-19. This does not constitute an endorsement by Statistics Canada of this product." } ],
          "retrieved_at": "2026-11-05T12:00:00Z" },
        { "role": "now", "value": 242.86, "unit": "CAD", "period": "2026-09", "period_precision": "month",
          "approximate": true, "method": "100 × CPI (latest month) ÷ CPI (that month)",
          "method_url": "https://www.bankofcanada.ca/rates/related/inflation-calculator/", "...": "같은 sources" }
      ]
    },
    { "id": "min-wage", "status": "OK", "comparable": true, "basis": [], "figures": [
        { "role": "then", "value": 3.80, "unit": "CAD per hour", "period": "1988-02-13", "period_precision": "day",
          "geography": "Alberta", "note": "In effect since 1981-05-01", "...": "ESDC sources" },
        { "role": "now", "value": 15.00, "unit": "CAD per hour", "period": "2026-11-05", "period_precision": "day", "...": "" } ] },
    { "id": "min-wage-minutes-milk", "status": "NO_DATA", "comparable": false, "basis": [], "figures": [] },
    { "id": "min-wage-minutes-bread", "status": "NO_DATA", "comparable": false, "basis": [], "figures": [] }
  ],
  "partial": false
}
```
(값 15.00과 2026-10-19은 모양을 보이는 예다. 시험 기대값은 녹화 fixture에서 정한다.)

| 항목 `status` | 뜻 | 화면 |
|---|---|---|
| `OK` | 수치가 있다 | 카드 |
| `NO_DATA` | 그 기간에 출처 값이 없다(1995년 전 빵·우유, 아직 발표 안 된 달, 그날 요율을 규칙으로 고를 수 없음) | "No data for that month" 등 한 줄 (결정 6 (a)) |
| `NEEDS_PLACE` | `province` 가 없어 주 항목을 셀 수 없다 | 보이지 않음 |
| `UNAVAILABLE` | 출처 실패·시간 초과. 응답 `partial: true` | 보이지 않음 |

항목 규칙:
- **`cpi-100`**: 그달 = 생일의 연·월. 최신 달 = WDS에서 받은 마지막 `refPer`. 그달 값이 없으면(최신 달보다 뒤) `NO_DATA`. `now.value` = 100 × 최신 ÷ 그달, 소수 둘째 자리에서 반올림. 화면은 정수 달러로 보인다.
- **`min-wage`**: ESDC `jurisdiction` = 그 주의 영어 이름. 그때 = 시행일이 생일 이하인 행 중 가장 늦은 날짜. 지금 = 시행일이 오늘 이하인 행 중 가장 늦은 날짜. 그 날짜에 행이 여럿이면 `data/curation/min-wage-choices.json`(C3-P5) 에서 고르고, 그 파일에도 없으면 그 수치만 빼고 둘 다 빠지면 `NO_DATA`. `wage` 문자열("$3.80")을 숫자로 바꾸지 못하면 그 행은 없는 것으로 친다. 연방(`Federal`) 행은 쓰지 않는다.
- **`min-wage-minutes-*`**: 분 = 가격 ÷ 시급 × 60, 정수로 반올림, `approximate: true`, `method` "price ÷ hourly minimum wage × 60". 그때 가격 = 그달, 그달 < 2017-01이면 옛 표, 아니면 새 표. 지금 가격 = 새 표 최신 달, 지금 시급 = 오늘 요율. 그때·지금 표가 다르면 `comparable: false`. 가격 이전(1995-01 전)이나 그때 시급이 없으면 `NO_DATA`. 각 수치의 `sources` 는 StatCan 표 + ESDC 둘 다.

### 웹 (C3d)

- `story/sections.ts` 에 `{ id: 'then-and-now', title: 'Then and now' }` 를 `world` 바로 앞(도시 풍경 섹션 다음)에 더한다(기존 id는 그대로, C3-P6 위임 결정).
- 정밀도 → 배지: `then` 은 `day` "That day", `week` "That week", `month` "That month", `year` "That year", `census` "<YYYY> census (nearest to <생일 연도>)". `now` 는 `day` "Today", 그 밖은 "<기간>, latest"(예 "September 2026, latest"). 달 이름은 C1처럼 표로 만든다(`Intl` 안 씀).
- `approximate` → "approx." + `method` 글, `method_url` 이 있으면 "How this is calculated" 새 탭 링크(`NewTabLink`).
- 출처 줄: `sources` 마다 `attribution` 과 라이선스 링크. `sources` 가 비거나 칸이 빈 수치는 그리지 않는다(`isDisplayable` 과 같은 방식).
- `comparable: false` 면 그때·지금을 나란히만 보이고 "×N" 같은 문구를 만들지 않는다.
- 금액은 `Intl.NumberFormat('en-CA', { style: 'currency', currency: 'CAD', maximumFractionDigits: 0 })`. 시급은 소수 둘째 자리까지.
- 응답 모양 검사(`world/worldShape.ts` 방식). 모양이 틀리면 섹션을 비운다.

## 작업 목록

### C3a `feat/then-now-format` — 공용 수치 형식 (서버, 외부 호출 없음)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | `Figure`·`ThenNowItem`(record, snake_case) + `FigureRules`(F1~F6) + `Attributions`(StatCan "Source"/"Adapted" 문구, OGL-Canada 문구를 만드는 함수) | backend-dev | - | (1) F1~F6마다 어기는 합성 레코드 1개 이상이 버려지고 통과 레코드는 그대로 나온다. (2) `Attributions.statcanAdapted("Table 18-10-0004-01 …", 2026-10-19)` 가 위 문장과 글자 하나까지 같다. (3) 버릴 때의 DEBUG 로그에 규칙 번호만 있고 값·기간·지역이 없다. (4) JSON 키가 위 표의 이름과 정확히 같다. (5) `./gradlew check` 통과 |
| T2 | 형식 시험 고정 + 전체 시험 | test-engineer | T1 | 기준 (1)~(4)를 시험으로. 경계: `month` 에 `1988-02-13` → 버림, `year` 에 `1988-01` → 버림, `now` 가 내일 → 버림(고정 시계, `America/St_Johns`). `./gradlew check` 2회 통과 |

### C3b `feat/statcan-wds-client` — WDS 클라이언트와 캐시 (서버)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T3 | ★ `StatCanWdsClient` (GET 범위 메서드, 연결 2 s·읽기 4 s, 재시도 없음, User-Agent 설정값) + `WdsPointCache` (C3-P2 (a): 지난 기간 점은 재시작까지 메모리에 고정, "최신" 조회는 12시간 TTL, 실패하면 이전 최신 값을 쓰고 없으면 `UNAVAILABLE`) + 설정 `statcan.*` | backend-dev | C3a 머지 | (1) WireMock: 정상 응답의 `refPer`·`value`·`decimals`·`releaseTime` 이 읽힌다. (2) 409, 5xx, 시간 초과, `status` ≠ `SUCCESS`, 빈 `vectorDataPoint`, 숫자 아닌 `value` → 예외 없이 "값 없음"/"출처 실패"로 구별되어 나온다. (3) 같은 (벡터, 기간)을 두 번 물으면 두 번째는 요청이 없다. 최신 조회는 TTL 안에서 1회, TTL 뒤 실패면 이전 값. (4) 보낸 요청에는 벡터 번호와 기간 말고 아무것도 없다(WireMock 요청 기록으로 확인). (5) `./gradlew check` 통과 |
| T4 | WDS fixture 녹화 + 클라이언트 시험 + 전체 시험 | test-engineer | T3 | (1) 벡터 41690973의 1976-07, 1980-04, 1988-02, 2010-02와 최신 범위 응답을 한 번 녹화(`server/src/test/resources/statcan/`, README에 녹화일·원본 URL·StatCan Open Licence). 값이 리서치(31.3, 42.9, 70.0, 115.6)와 다르면 보고한다. (2) 409·시간 초과·잘못된 본문은 합성 fixture. (3) 시험 중 외부 네트워크 0. `./gradlew check` 2회 통과 |

### C3c `feat/cpi-then-now` — 항목 2와 첫 API (서버)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T5 | `CpiThenNow`(항목 규칙) + `ThenAndNowController`·`Service`·`Response` (`date` 만. `province` 는 받으면 무시하지 않고 C3f 전까지 400 없이 그냥 읽지 않음) | backend-dev | C3b 머지 | (1) 녹화 fixture(최신 2026-09 = 170.0)로: 1988-02-13 → `now.value` 242.86, 1976-07-17 → 543.13, 1980-04-12 → 396.27, 2010-02-12 → 147.06(fixture 값이 다르면 그 값으로 다시 계산한 수). (2) 최신 달보다 뒤인 달(고정 시계로 만든 날) → `NO_DATA`. 1950-01-01·2000-02-29도 정상. (3) WDS 실패 → 200, 항목 `UNAVAILABLE`, `partial: true`. (4) 날짜 400 코드 5개가 세계 API와 같다. (5) `Cache-Control: no-store`, 키 집합이 계약과 같다. (6) 어떤 로그 수준에도 요청 날짜가 없다(`CapturedOutput`, DEBUG). (7) `./gradlew check` 통과 |
| T6 | MockMvc 계약 시험 + 전체 시험 | test-engineer | T5 | 기준 (1)~(6)을 HTTP로. 화면이 쓰는 배지 근거(`period_precision` = `month`, `period` = `YYYY-MM`)를 데모 4날짜에서 단언한다. `./gradlew check` 2회 통과 |

### C3d `feat/then-now-ui` — 섹션과 수치 카드 (웹)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T7 | 타입·응답 모양 검사·`fetchThenAndNow`·`FigureCard`·`ThenNowSection`·섹션 등록, MSW 처리기(위 계약의 합성 응답) | frontend-dev | C3a 계약(이 계획). C3c와 병렬 | (1) `month` 수치는 "That month", `now` 는 "September 2026, latest". 어떤 `month`·`year` 수치도 "That day"를 보이지 않는다. (2) `approximate` → "approx."와 방법 링크(새 탭 안내 숨김 글자 포함). (3) `sources` 가 비거나 `attribution` 이 빈 수치는 그리지 않는다. (4) `NO_DATA` 는 한 줄, `NEEDS_PLACE`·`UNAVAILABLE` 은 그리지 않는다. 항목이 모두 보이지 않으면 섹션 본문은 "Nothing to compare for this date." (5) 요청 주소의 쿼리에는 `date` 만 있다. 오류 메시지·콘솔에 날짜 없음(`client.ts` 규칙). (6) `npm run lint && npm run format:check && npm run typecheck && npm test` 통과 |
| T8 | 컴포넌트 시험 + Playwright(MSW 가로채기) | test-engineer | T7 | 데모 4날짜의 합성 응답으로 기준 (1)~(5), 대비·포커스는 기존 검사 도우미로. `comparable: false` 일 때 "×" 문구 없음. `npm test` 2회, `npm run e2e` 2회 통과 |
| T9 | 실제 서버 확인 (C3c 머지 뒤 `main`) | PM | T8, C3c | `bootRun` + `npm run dev` 에서 데모 4날짜의 "Then and now" 섹션에 "$100 in <달>" 카드가 "That month" 배지와 Statistics Canada 출처 줄로 보인다 |

### C3e `feat/min-wage-source` — ESDC 출처와 고르기 규칙 (서버)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T10 | ESDC CSV 녹화 발췌 + 원문 재확인 + `data/curation/min-wage-choices.json` | content-curator | C3a 머지 | (1) CSV 전체를 한 번 받아 데모 주(AB, QC, BC, NL)와 13개 주 "지금" 행, 같은 날 여러 행인 묶음만 발췌해 `server/src/test/resources/esdc/` 에 둔다(README: 받은 날, 원본 URL, OGL-Canada). (2) 리서치 값(AB 1981-05-01 $3.80, QC 1976-07-01 $2.87, NL 1979-06-01 $2.80)을 원문과 대조해 보고에 적는다. (3) 고르기 파일: 13개 주의 "지금" 날짜와 데모 4날짜가 걸리는 묶음 중 여러 행인 것만, 항목마다 `jurisdiction`, `effective_date`, 고른 `wage`, `reason`, ESDC 데이터베이스 쪽 `evidence_url`, `reviewed_at`. 근거가 없으면 넣지 않는다(그 값은 빈다) |
| T11 | `EsdcMinWageClient`(리다이렉트는 https만 따라감, 연결 2 s·읽기 6 s, 파일 전체를 메모리에 24시간, 실패하면 이전 사본, 없으면 `UNAVAILABLE`) + `MinWageRules`(그때·지금 고르기, 미래 시행일 제외, `wage` 해석) + 고르기 파일 읽기(시작 때 검사, 어기면 시작 실패) | backend-dev | T10 | (1) T10 fixture로: AB 1988-02-13 → $3.80 · 1981-05-01, QC 1976-07-17 → $2.87(여러 행이면 고르기 파일이나 빈 값), NL 1980-04-12 → $2.80 또는 빈 값(T10 결과대로). (2) 시행일 바로 그날·바로 전날 경계. (3) 미래 시행일 행은 "지금"에 쓰이지 않는다(고정 시계). (4) 같은 날 여러 행 + 고르기 없음 → 빈 값. (5) 요청에 쿼리·주·날짜가 없다(WireMock). (6) `./gradlew check` 통과 |
| T12 | 규칙 시험 + 전체 시험 | test-engineer | T11 | 기준 (1)~(5)를 시험으로, 잘못된 `wage` 문자열과 BOM·따옴표 행은 합성 fixture. 외부 네트워크 0. `./gradlew check` 2회 통과 |

### C3f `feat/min-wage-minutes` — 항목 3을 API에 (서버)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T13 | `province` 매개변수(검사·`PROVINCE_INVALID`) + `MinWageThenNow`(`min-wage`, `min-wage-minutes-milk`, `min-wage-minutes-bread`) + 빵·우유 벡터 설정 | backend-dev | C3c, C3e 머지 | (1) `province` 없음 → 세 항목 `NEEDS_PLACE`, `cpi-100` 은 그대로. (2) `province=99`·`AB`·`48a` → 400 `PROVINCE_INVALID`, `detail` 에 입력 없음. (3) 1988-02-13 + 48 → `min-wage` 그때 $3.80(`day`, 노트 "In effect since 1981-05-01"), 분 항목은 `NO_DATA`(1995 전). (4) 2010-02-12 + 59 → 빵·우유 그때는 옛 표 2010-02, 지금은 새 표 최신, `comparable: false`, 분은 정수·`approximate`. (5) 로그 어느 수준에도 날짜와 주가 없다. WDS 요청에는 벡터·기간만, ESDC 요청에는 쿼리가 없다. (6) `./gradlew check` 통과 |
| T14 | 빵·우유 fixture 녹화(v735175·v735169 2010-02, v1353834326·v1353834283 최신) + MockMvc 시험 + 전체 시험 | test-engineer | T13 | 기준 (1)~(5)를 HTTP로, 데모 4날짜 × 해당 주(48, 24, 59, 10). 녹화 README에 날짜·URL·라이선스. `./gradlew check` 2회 통과 |

`min-wage` 계열은 C3d의 `FigureCard` 가 그대로 그린다. 웹이 `province` 를 보내는 것은 `feat/place-picker` 에서 한다(C3-P3). 그 전에는 화면에 항목 3이 나오지 않는 것이 정상이다.

## 크기 (400줄 규칙)

S1 = 제품 코드 줄(시험·fixture·`data/` 제외). 서버는 파일별 셈 × 2.1~2.9, 웹은 × 1.0~1.7.

| PR | 파일별 셈 | 기대 (범위) | 확인점 |
|---|---|---|---|
| C3a | `Figure`·`ThenNowItem` 40 + `FigureRules` 55 + `Attributions` 15 = 110 | 약 275 (230~320) | T1 뒤 350 넘으면 알림 |
| C3b | 클라이언트 75 + 캐시 35 + 설정 20 = 130 | 약 325 (275~375) | 클라이언트만 끝난 때 250 넘으면 알림, T3 뒤 420 넘으면 멈춤 |
| C3c | CPI 항목 45 + API 3파일 60 = 105 | 약 260 (220~305) | T5 뒤 350 넘으면 알림 |
| C3d (웹) | 타입·모양 45 + api 20 + `FigureCard` 60 + 섹션 50 + 배지 문구 40 + 등록 15 = 230 | 약 320 (230~390) | `FigureCard` 끝난 때 250 넘으면 알림, T7 뒤 420 넘으면 멈춤 |
| C3e | ESDC 클라이언트 55 + 규칙 45 + 고르기 파일 읽기 30 = 130 | 약 325 (275~375) | T11 뒤 420 넘으면 멈춤 |
| C3f | 항목 3 60 + `province` 검사·빵우유 설정 25 = 85 | 약 210 (180~245) | T13 뒤 300 넘으면 알림 |

순서: C3a → C3b → C3c → C3e → C3f (서버, 차례로). C3d는 C3a 뒤 웹 쪽에서 병렬. 서버 줄에서 C2(장소)와 섞는 순서는 로드맵이 정한다. C3f는 C2 코드에 의존하지 않는다.

## 시험과 개인정보

- 외부 네트워크 없음. StatCan·ESDC는 개방 라이선스라 녹화 fixture(녹화일·원본 URL·라이선스)를 쓰고, 실패·이상 응답은 합성이다. 웹은 MSW 합성 응답만.
- 우리 서버는 날짜와 주를 함께 받지만 **어떤 로그에도 남기지 않는다**(`CLAUDE.md` 7절 6항, 시험으로 확인). 바깥으로는 함께 나가지 않는다: WDS에는 벡터와 기간(달)만, ESDC에는 파일 전체 요청(쿼리 없음)만 간다. 장소 해석 API는 날짜를 받지 않는다(C2).
- 웹은 `province` 코드만 보내고 이름은 보내지 않는다. 날짜·장소는 주소창에 남기지 않는다(S2 규칙).
- 시험 날짜는 데모 4날짜, 1950-01-01, 2000-02-29, 고정 시계로 만든 "아직 발표 안 된 달"만 쓴다.

## 다음 PR (C5, 이름만)

| 브랜치 | 항목 | 출처 (확신도) | 선행 | 메모 |
|---|---|---|---|---|
| `feat/baby-names` | 6 주별 인기 아기 이름 | BC·온타리오·퀘벡(확인됨). 앨버타는 라이선스 원문(T7 B3) 뒤 | C3a·C3d, C3f의 `province` | content-curator가 상위 N개 요약을 `data/` 에 두고 이때 `figure.schema.json` 을 더한다. "Top baby names registered in <province>, <year>", 주별 정의 주석, 주 사이 비교 금지. 그 밖의 주는 "No data" |
| `feat/weather-stations` → `feat/birthday-weather` | 1 생일 날씨 | ECCC `climate-stations`·`climate-daily`(확인됨, T6 D4 (a) 위임됨) | C2c(좌표), C3a | 관측소 표를 `data/` 에 두고 외부로는 `STN_ID` + 날짜만. 가까운 순 5~10곳, 50 km 안, 최고·최저가 있는 첫 곳. "지금" = 지난 해 같은 날(T6 D3 (a), PR 계획 때 확정), 윤일은 "No record". 관측소 이름·거리 병기. 두 PR로 본다 |
| `feat/city-population` | 11 도시 인구 | StatCan 17-10-0155-01(CSD, 2001~)·17-10-0033-01/0037-01(CMA, 1976~2000)(확인됨) | C2c(`csd`), C3b | `csd` → 벡터·CMA 대응표를 content-curator가 `data/` 에. `boundary_vintage`·`kind`·`is_preliminary` 를 채우고 경계 판이 다르면 배수 없음(F6). T6 D1 (b)·D6은 PR 계획 때 정함 |

## 리스크와 확인할 것

1. **최신 달 바뀜**: CPI는 매달 바뀐다. 화면은 늘 "<달>, latest"로 쓰고 시험은 녹화 값에 묶는다. 녹화를 다시 하면 기대값도 같은 커밋에서 바뀐다.
2. **ESDC 같은 날 여러 행**: 1980년대 이전과 현재 BC(2026-06-01 두 행)에 있다. 고르기 파일에 근거가 없으면 비우므로 데모에서 최저임금이 빌 수 있다. 빈 것이 정답이고 다른 날짜로 바꾸지 않는다.
3. **WDS 점검 시간**(영업일 00:00~08:30 ET): 최신 조회가 실패하면 이전 값을 쓰고, 처음 켤 때 실패하면 그 항목만 빈다.
4. **빵·우유 표 이어 붙이기**: 옛 표와 새 표의 품목 정의가 다를 수 있다. 값마다 표 이름을 쓰고 비율을 만들지 않는다(T5 D2 (a)).
5. **섹션 위치**: 제품 정의서 5절 2차 수정(PM)이 다른 위치를 정하면 `sections.ts` 의 한 줄만 옮긴다.
6. **CPI 해석**: "approx."와 계산법 링크만으로는 "생활비"로 읽힐 수 있다. 문구는 "$100 in <달> ≈ $X in <최신 달>"로 두고 "worth"·"cost of living"을 쓰지 않는다.

## PM 결정 필요 (PM 위임 — "추천"으로 진행)

- **C3-P1 PR 나누기** — 서버 셈 합계 약 560 × 2.1~2.9 = 1,175~1,625, 웹 약 230~390.
  - (a) 서버 다섯 + 웹 하나(위 표, 각 400 아래).
  - (b) 서버 셋(형식+WDS / CPI+API / 최저임금) + 웹 하나. 각 약 450~650.
  - (c) 항목 3을 C5로 미루고 C3는 형식·CPI·웹만.
  - **추천: (a)**.
- **C3-P2 캐시**
  - (a) 메모리: 지난 기간 점은 재시작까지 고정, 최신은 12시간 TTL, ESDC 파일은 24시간. 실패하면 이전 사본.
  - (b) DB 표(Flyway V4)에 점을 쌓는다(재시작해도 남음, 약 100줄 더).
  - (c) CPI 계열을 `data/` 에 스냅샷으로 커밋하고 최신만 부른다.
  - **추천: (a)**. 라이선스는 셋 다 허용한다. 로컬 데모 규모(WDS 25회/초)에서 충분하다. 인구·NHPI처럼 계열이 늘면 C5에서 (b)를 다시 본다.
- **C3-P3 C2가 오기 전 주(province)를 어떻게 아나**
  - (a) API가 지금 `province` 코드(13개 상수)를 받는다. 웹은 `feat/place-picker` 에서 고른 후보의 `province.code` 를 보낸다. 그 전에는 항목 3이 `NEEDS_PLACE` 라 화면에 나오지 않고, 시험은 API를 직접 부른다.
  - (b) 항목 3 전체를 C2 머지 뒤로 미룬다.
  - (c) 웹에 임시 주 선택 상자를 둔다(나중에 지울 화면).
  - **추천: (a)**. 서버 작업이 C2를 기다리지 않고, 웹은 C2의 계약만 쓴다.
- **C3-P4 "몇 분" 의 기준 품목**
  - (a) 빵 675 g·우유 1 L(캐나다 평균, 1995~). 휘발유(도시, 1979~)는 C2의 도시 대응표 뒤 C5.
  - (b) 지금은 시급만 보이고 "몇 분"은 C5에서 휘발유와 함께.
  - (c) 휘발유만, C2 뒤.
  - **추천: (a)**. 1976·1980·1988 데모는 시급만 나오는 것이 정상이다(리서치 1.2 판정과 같음).
- **C3-P5 같은 날 여러 최저임금 행**
  - (a) 늘 비운다.
  - (b) content-curator가 ESDC 근거 URL을 단 고르기 파일로 13개 주 "지금"과 데모 날짜 묶음만 고르고, 나머지는 비운다.
  - (c) 가장 높은 요율을 쓴다(추측).
  - **추천: (b)**. "지금" 값이 가장 자주 보이는데 BC가 이미 두 행이다. 근거 없는 행은 여전히 비운다.
- **C3-P6 섹션 위치**
  - (a) `world` 와 `closing` 사이 새 섹션 `then-and-now`.
  - (b) `opening` 바로 뒤.
  - (c) 제품 정의서 5절 2차 수정까지 웹 섹션을 미룬다.
  - **추천: (a)**. 정의서가 다르게 정하면 한 줄을 옮긴다.
  - **PM 위임 결정 (2026-11-05, orchestrator)**: `brief-changes-canada.md` 2-1의 추천 (a)에 맞춰 `world` 바로 앞(도시 풍경 다음)으로 정한다. "그날 그곳" 값이 장소 섹션에 이어지고, 정의서 제안과 `frontend-dev.md` 순서 제안이 같은 위치를 쓴다.
- **C3-P7 ★ PM 직접 구현**: C3b T3 `StatCanWdsClient`·`WdsPointCache`(지난 값 고정 + 최신 TTL + 실패 때 이전 값, 시간 초과와 409 처리). 캐시 전략과 외부 어댑터 격리를 배울 수 있다.
  - (a) PM이 한다.
  - (b) backend-dev가 한다.
  - **추천: (b)**. PM이 2026-11-04 자율 진행을 위임했다.
