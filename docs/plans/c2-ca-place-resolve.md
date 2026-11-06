# 캐나다 장소 해석: 사전과 계약 (C2 1부)

> 브랜치: `feat/ca-place-data` → `feat/ca-place-matcher` → `feat/ca-place-resolve` (모두 `main` 에서, 차례로) · 작성: planner (2026-11-04) · 상태: **초안 (PM 위임 — "추천"이 진행할 안)**
> 기준: `docs/research/canada-place-search.md`(T1) 5·7·8·10절, `docs/research/canada-summary.md` 6.2·7절, PM 위임 결정(2026-11-04, `docs/progress.md` 377행: T1 D1~D5 모두 (a)), `CLAUDE.md` 7절, 머지 코드(`ApiProblem`, `AppProperties.dataPath()`, `EventCuration` 의 시작 시 읽기·검사 방식, `WorldStoryResponse` 의 snake_case), 멈춘 브랜치 `feat/place-resolve`(M2a: `PlaceQuery`, `RegionDictionary`, `RegionMatcher`, `PlaceResolveController`·`Service`·`Response`, `PlaceSearch`·`DisabledPlaceSearch`).
> 원래 이름은 PR 하나(`feat/ca-place-resolve`)였으나 크기 때문에 셋으로 나눈다(C2-P1). 마지막 PR이 그 이름을 쓴다.

## 목표

1. 캐나다 장소 입력 `q` 를 서버 안의 사전만으로 해석한다. 결과는 유형(`REGION`/`NEIGHBORHOOD`), 좌표, 주(province), CSD다. 외부 호출은 없다.
   - `REGION`: 주·준주 13개와 SGC 2021 CSD 5,161개. 좌표는 CGNDB 대표점(한 출처로 고정).
   - `NEIGHBORHOOD`: SGC 요소 CSV의 CSD 아래 "Place name(s)" 중 CGNDB 지명과 맞는 것. CSD 소속은 SGC가, 좌표는 CGNDB가 준다.
2. 사전 데이터를 공식 내려받기 파일에서 한 번 만들어 `data/places/` 에 커밋한다(파생 파일만, 원본 파일은 커밋하지 않음).
3. `GET /api/places/resolve` 를 캐나다 계약으로 낸다(`province`·`csd`, `OUTSIDE_CANADA` 자리, id 접두어 `province-`·`csd-`·`cgndb-`·`osm-`).
4. 데모 4곳과 같은 이름 확인용 "London", "Victoria", "Saint John"이 아래 기대값대로 나온다.

## 범위 밖

- `SPOT`(병원) 검색과 Nominatim → `feat/ca-place-search`. Nominatim은 종합 `부분 확인` 이라 등급이 오르고 T1 D2가 정해진 뒤에 계획한다(`CLAUDE.md` 7절 2항). 이 계획에서 `OUTSIDE_CANADA`, `SPOT`, `osm-` 은 계약에만 있고 응답에 나오지 않는다.
- CGNDB Geoname API·Geolocator 실행 중 호출(T1 D1 (a)). ODHF 병원 사전(D5 (b)), GeoNames 시간대, 경계 파일 점-다각형, 도시 열린 데이터 동네 경계(D3 (a): 경계는 필요한 작업이 정함).
- 웹 장소 칸 → 후보 고르기 → `feat/place-picker`(아래 "다음 PR"). 400줄 규칙과 S2(`feat/input-and-opening`, 입력 폼) 선행 때문에 이 계획에 넣지 않는다.
- Spring 기본 404·405·500 응답의 `code`(M2a2, 멈춤). 프랑스어 이름 화면 표기.

## 사용할 외부 출처

| 출처 | 확신도 | 쓰는 방식 | 지킬 조건 |
|---|---|---|---|
| SGC 2021 구조 CSV·요소 CSV ([T1 3절](../research/canada-place-search.md#3-출처-카드), 근거 [11][12]) | 확인됨 | 한 번 내려받아 파생 파일을 만든다 | Statistics Canada Open Licence. 가공했으므로 "Adapted from Statistics Canada, Standard Geographical Classification (SGC) 2021, <기록 수정일>. This does not constitute an endorsement by Statistics Canada of this product." CSV는 Windows-1252(Latin-1 상위 집합, 2026-11-05 리뷰에서 정정) |
| CGNDB 내려받기 CSV, 전국 합본 `cgn_canada_csv_eng.zip` ([T1 3절](../research/canada-place-search.md#3-출처-카드), 근거 [1][7], R11) | 확인됨 | 같음 | OGL-Canada v2.0. "Contains information licensed under the Open Government Licence – Canada." + 라이선스 링크. UTF-8 BOM. 공식 보증 암시 금지. 옛 용어 경고가 있으므로 이름을 고치지 않고 그대로 둔다 |

`확인됨` 이 아닌 출처는 쓰지 않는다. 내려받기와 녹화의 User-Agent는 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)` 하나뿐이고, 이메일은 어디에도 넣지 않는다.

## API / 데이터 변경

### `data/places/` (C2a)

| 파일 | 만드는 쪽 | 열 (UTF-8, BOM 없음, 첫 줄 머리글) |
|---|---|---|
| `provinces.csv` | content-curator (손으로, 13행) | `code,name,abbr,aliases,lat,lng,cgndb_id`. `aliases` 는 `\|` 로 나눔(예 "Newfoundland\|Nfld."). 좌표는 CGNDB `PROV`/`TERR` 행. 약어·별칭의 출처는 StatCan 공식 쪽(README에 URL) |
| `regions.csv` | 빌더 | `csd_code,name,province_code,cd_name,lat,lng,cgndb_id,coord_rule`. `coord_rule` = `unique`/`override`/`none`. `none` 이면 `lat`·`lng`·`cgndb_id` 비움 |
| `neighbourhoods.csv` | 빌더 | `cgndb_id,name,csd_code,province_code,lat,lng` |
| `overrides.csv` | content-curator | `csd_code,cgndb_id,reason,evidence_url,reviewed_at`. 여러 건이 맞거나 안 맞는 CSD 중 데모·같은 이름 확인 대상만 |
| `README.md` | content-curator | 출처 URL, 내려받은 날, 라이선스·표시 문구, 빌드 명령, 열 설명, 행 수, 파일 크기, 맞추기 규칙과 결과 수(유일/복수/없음) |

빌드 규칙 (빌더, T1):
- B1 `REGION` 좌표: (주, `fold(name)`)이 같은 CGNDB 현재 공식명 중 `Concise Code` ∈ {`CITY`,`TOWN`,`VILG`,`HAM`,`UTM`,`LTM`,`STM`,`MUN1`,`MUN2`} 가 정확히 1건이면 `unique`. 아니면 `overrides.csv` 에 있으면 `override`, 없으면 `none`(좌표 없음, 추측 금지).
- B2 `NEIGHBORHOOD`: SGC 요소 CSV "Place name(s)" 행(CSD 코드 + 이름)마다, 같은 주의 CGNDB 현재 공식명 중 `fold(name)` 이 같고 `Concise Code` ∈ {`UNP`,`HAM`,`VILG`} 이며 그 CSD 대표점에서 25 km 안인 것이 정확히 1건이면 한 행. 0건·2건 이상·CSD 좌표 없음이면 뺀다. 그래서 "Victoria Road" 같은 `UNP` 도로 이름은 SGC 요소에 없으면 들어오지 않는다.
- B3 출력은 `csd_code`·`cgndb_id` 오름차순이고, 같은 입력이면 바이트가 같다. 원본 파일 경로는 인자로 받고 저장소에 두지 않는다.
- `fold`: NFD 뒤 결합 문자 제거, 소문자(`Locale.ROOT`), `’`·`‘` → `'`, 하이픈·엔대시·엠대시 → 공백, 낱말 `st`/`st.`/`saint` → `saint`, `ste`/`ste.`/`sainte` → `sainte`, 나머지 마침표 제거, 공백 하나로. 빌더와 매처가 같은 코드를 쓴다.

### `GET /api/places/resolve?q=<1~50 code points>` (C2c)

M2a 뼈대를 그대로 옮긴다: snake_case, `Cache-Control: no-store`, `ApiProblem` 오류 형식, 요청값을 로그·`detail` 에 넣지 않음. `q` 외의 매개변수는 읽지 않는다(날짜는 받지 않는다).

```
GET /api/places/resolve?q=Calgary
200
{
  "status": "FOUND",
  "candidates": [
    {
      "id": "csd-4806016",
      "type": "REGION",
      "name": "Calgary",
      "full_name": "Calgary, Alberta",
      "province": { "code": "48", "name": "Alberta", "abbr": "AB" },
      "csd": { "code": "4806016", "name": "Calgary" },
      "lat": 51.0458,
      "lng": -114.0575
    }
  ],
  "more": false,
  "sources": [
    { "name": "Statistics Canada, SGC 2021", "url": "https://www.statcan.gc.ca/en/subjects/standard/sgc/2021/index",
      "license": "Statistics Canada Open Licence", "license_url": "https://www.statcan.gc.ca/en/terms-conditions/open-licence",
      "attribution": "Adapted from Statistics Canada, Standard Geographical Classification (SGC) 2021, <date>. This does not constitute an endorsement by Statistics Canada of this product." },
    { "name": "Natural Resources Canada, Canadian Geographical Names Database", "url": "https://open.canada.ca/data/en/dataset/e27c6eba-3c5d-4051-9db2-082dc6411c2c",
      "license": "Open Government Licence – Canada", "license_url": "https://open.canada.ca/en/open-government-licence-canada",
      "attribution": "Contains information licensed under the Open Government Licence – Canada." }
  ]
}
```

| 필드 | 값 |
|---|---|
| `status` | `FOUND` (입력 전체가 맞음) · `PARTIAL` (입력 일부가 가리키는 지역만 맞음, 아래 R5. 화면은 "동네나 도시로 다시 입력" 안내) · `NOT_FOUND` · `OUTSIDE_CANADA` (계약 자리만. `feat/ca-place-search` 가 Nominatim `country_code` 로 낸다) |
| `candidates[].id` | `province-<2자리>` · `csd-<7자리>` · `cgndb-<CGNDB ID>` · (`feat/ca-place-search`) `osm-<N\|W\|R><osm_id>` |
| `candidates[].type` | `REGION` · `NEIGHBORHOOD` · (`feat/ca-place-search`) `SPOT` |
| `name` | 공식 철자, 악센트 유지("Montréal") |
| `full_name` | CSD: "Name, Province". 주: "Alberta". `NEIGHBORHOOD`: "Name, CSD name, Province". 한 응답 안에서 겹치면 CSD 이름 뒤에 ` (<cd_name>)` 를 붙이고, 그래도 겹치면 ` (<csd_code>)` |
| `province` | 항상 있음 |
| `csd` | 주 후보는 `null` |
| `lat`·`lng` | 숫자 또는 둘 다 `null` (`coord_rule=none`) |
| `more` | 후보가 20개를 넘어 잘렸으면 `true` |
| `sources` | 위 두 개, 고정 순서 |

| 상태 | `code` | 조건 |
|---|---|---|
| 400 | `PLACE_REQUIRED` | `q` 가 없거나 정규화 뒤 비었음 |
| 400 | `PLACE_TOO_LONG` | 정규화 뒤 50 code point 초과 |

시작할 때 사전 파일이 없거나 규칙(열, id 형식, 중복, CSD 앞 두 자리 = `province_code`)을 어기면 서버가 뜨지 않는다(메시지에는 파일 경로와 줄 번호만). 그래서 503은 없다.

### 매칭 규칙 (C2b, `PlaceMatcher`)

비교는 `fold` 값이 **완전히 같을 때만** 한다(앞부분·부분 문자열 일치 없음, M2a와 같음).
- R1 입력 전체 = 주 이름·약어·별칭 → 그 주(`province-`).
- R2 입력 전체 = CSD 이름 → 그 이름의 CSD 모두.
- R3 입력 전체 = 동네 이름 → 그 이름의 동네 모두.
- R4 쉼표 형식: "A, P"(P = 주) → P 안의 CSD A와 동네 A. "A, C"(C = CSD 이름) → 그 이름의 CSD들 안의 동네 A만. "A, C, P" → P 안의 CSD C 안의 동네 A. 쉼표 3개 이상이나 빈 조각은 맞는 것 없음.
- R5 R1~R4가 0건이면: 첫 쉼표 앞 조각의 앞 1~3낱말 중 가장 긴 것, 또는 마지막 쉼표 조각이 R1·R2로 맞으면 그 지역들을 `PARTIAL` 로. 그것도 없으면 `NOT_FOUND`.
- R6 순서: 주 → CSD(SGC 코드 오름차순) → 동네(CSD 코드, `cgndb_id` 오름차순). 최대 20개. 같은 입력은 늘 같은 답. 외국의 같은 이름은 다루지 않는다(D4 (a)).

### 데모 기대값 (C2b·C2c 시험의 기준, 실제 `data/places/` 로 확인)

| 입력 | 기대 |
|---|---|
| `Calgary` | `FOUND`, 1건 `csd-4806016`, 주 48 AB, 좌표 51.0458 / −114.0575 (±0.001) |
| `Hochelaga-Maisonneuve, Montréal` · `hochelaga maisonneuve, montreal` | `FOUND`, `NEIGHBORHOOD` 1건, `csd.code` 2466023, 좌표 45.5647 / −73.5389 (±0.001), `full_name` "Hochelaga-Maisonneuve, Montréal, Quebec" |
| `Vancouver General Hospital` | `PARTIAL`, 1건 `csd-5915022` (D5 (a): 지점은 도시로 돌려주고 다시 입력 안내) |
| `St. John's` · `St John's, NL` | `FOUND`, `csd-1001519`, 주 10, 좌표 47.4822 / −52.7971 (±0.001) |
| `London` | `FOUND`, 1건 `csd-3539036`, `full_name` "London, Ontario" (영국 런던 없음, D4 (a)) |
| `Victoria` | `FOUND`, CSD 5건이 SGC 코드 오름차순(NL → PEI 2 → MB → BC), `full_name` 이 모두 다름 |
| `Victoria, BC` | `FOUND`, BC의 Victoria CSD 1건 |
| `Saint John` · `St. John` | `FOUND`, `csd-1301006` 만(Fort St. John·St. John's 없음) |

주 이름은 SGC 영어판 철자("Quebec")를 쓴다. 표의 좌표는 리서치가 읽은 CGNDB 값이며, T2 결과가 다르면 T2 보고에 적고 README 값을 기준으로 시험을 맞춘다.

## 작업 목록

### C2a `feat/ca-place-data` — 사전 빌더와 파생 데이터

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | `NameFold` + `PlaceDictionaryBuilder` + Gradle 작업 `buildPlaceDictionary` (원본 경로 3개·출력 폴더를 인자로) | backend-dev | - | (1) 녹화 발췌 fixture(`server/src/test/resources/places/`, 데모·같은 이름 행만, README에 내려받은 날·원본 URL·라이선스)로 단위 시험 통과: B1~B3, Latin-1 "Montréal"·BOM 처리, `fold("Montreal")==fold("Montréal")`, `fold("Mercier–Hochelaga-Maisonneuve")==fold("Mercier-Hochelaga-Maisonneuve")`, `fold("St. John's")!=fold("Saint John")`, `fold("St. John")==fold("Saint John")`, 같은 입력 두 번 → 출력 바이트 같음. (2) 빌더는 네트워크를 쓰지 않는다(파일만 읽음). (3) `./gradlew check` 통과 |
| T2 | 원본 3개를 저장소 밖에 한 번 내려받아 빌드하고 `data/places/` 5개 파일 커밋 | content-curator | T1 | (1) "데모 기대값" 표의 코드·좌표가 파일 행에 있다(Hochelaga-Maisonneuve는 `neighbourhoods.csv`, 나머지는 `regions.csv`, 필요하면 `overrides.csv` 에 근거 URL). (2) `data/places/` 합계 4 MB 이하(넘으면 멈추고 보고). (3) 원본 zip·CSV가 커밋에 없다(`git show --stat`). (4) README에 출처 URL, 내려받은 날, 두 표시 문구, 빌드 명령, 행 수, 맞추기 결과 수가 있다. (5) 커밋은 `data/places/` 만 바꾼다 |
| T3 | 데이터 파일 검사 시험 + 전체 시험 | test-engineer | T2 | `PlaceDataFilesTest`: 머리글이 정확히 위 열, id 유일, `csd_code` 7자리이고 앞 두 자리 = `province_code`, `province_code` 는 13개 안, 좌표는 둘 다 있거나 둘 다 비고 위도 41~84·경도 −142~−52, BOM 없음, 합계 4 MB 이하. `./gradlew check` 2회 통과 |

### C2b `feat/ca-place-matcher` — 사전 읽기와 매칭 (HTTP 없음)

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T4 | ★ `PlaceDictionary`(시작 때 `<app.data-dir>/places/` 읽기·검사) + `PlaceMatcher`(R1~R6, `full_name` 규칙) | backend-dev | C2a 머지 | (1) 작은 합성 사전 시험: R1~R6 각각, 쉼표 3개 → 0건, 빈 조각 → 0건, 21건 → 20건 + `more`. (2) 실제 `data/places/` 로 "데모 기대값" 표 전부(`PARTIAL` 행은 상태와 후보만). (3) 파일 없음·열 틀림·id 중복·CSD/주 불일치 → 시작 실패, 메시지에 경로와 줄 번호만. (4) DEBUG 로그에도 입력·후보 이름·코드가 없다(`CapturedOutput`). (5) `./gradlew check` 통과 |
| T5 | 매처 검증 시험 + 전체 시험 | test-engineer | T4 | 기준 (1)~(4)를 시험으로 고정한다(입력은 데모·같은 이름과 그 악센트·대소문자·"St."/"Saint" 변형만). 같은 입력 두 번 → 같은 답. `./gradlew check` 2회 통과 |

### C2c `feat/ca-place-resolve` — API 계약

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T6 | `PlaceQuery` 옮김(`git show feat/place-resolve:<경로>` 로 읽어 새 커밋) + `PlaceResolveController`·`Service`·`Response` (캐나다 계약) | backend-dev | C2b 머지 | (1) `PlaceQuery`: NFC, 제어 문자 제거, 공백 정리, code point 50 통과·51 실패, 문자열 표현과 오류 `detail` 에 입력 없음(M2a 시험도 옮김). (2) 위 계약대로 응답하고 키 집합이 정확히 표의 필드다. (3) `Cache-Control: no-store`. (4) 400 두 코드. (5) `q` 외 매개변수(예 `date`)를 읽지 않고 로그에도 없다. (6) M2a의 한국 전용 부분(`sido`·`sigungu`, `OUTSIDE_KOREA`, 법정동코드, 시·군 꼬리 규칙, `regions.json`)은 옮기지 않는다. `PlaceSearch` 자리는 `feat/ca-place-search` 가 만든다 |
| T7 | MockMvc 계약 시험 + 전체 시험 | test-engineer | T6 | "데모 기대값" 표 전부를 HTTP로, 응답 키 집합, 오류 2개와 공백만·제어 문자만 입력, 헤더, DEBUG 로그에 `q` 값 없음. 외부 네트워크 호출 0(시험 설정에서 막음). `./gradlew check` 2회 통과 |
| T8 | (선택) 실제 서버 확인 | PM | T7 | `cd server && ./gradlew bootRun` 뒤 `curl -s 'http://localhost:8080/api/places/resolve?q=Calgary'`, `q=Victoria`, `q=Hochelaga-Maisonneuve%2C%20Montr%C3%A9al` 결과가 표와 같다 |

## 크기 (400줄 규칙)

S1 = 제품 코드 줄(시험·`data/`·README 제외). 파일별 셈에 최근 서버 실측 비율 ×2.1~×2.9를 곱했다.

| PR | 파일별 셈 | 기대 (범위) | 확인점 |
|---|---|---|---|
| C2a | `NameFold` 30 + 빌더 110 + Gradle 5 = 약 145 | 약 360 (305~420) | T1 뒤 누적 S1 400을 넘으면 멈추고 PM에게 묻는다(빌더의 `NEIGHBORHOOD` 부분을 C2b로 옮길지) |
| C2b | `PlaceDictionary` 70 + `PlaceMatcher` 80 + 설정 5 = 약 155 | 약 390 (325~450) | T4 중간(사전 읽기 끝) 200을 넘으면 알림, T4 뒤 450을 넘으면 멈춤 |
| C2c | `PlaceQuery` 35 + API 3파일 70 = 약 105 | 약 260 (220~305) | T6 뒤 350을 넘으면 알림 |

CSV 읽기는 Jackson CSV 모듈(Spring Boot가 판을 관리하는 `tools.jackson.dataformat:jackson-dataformat-csv`)을 추천한다. 직접 파서를 쓰면 C2a가 40줄쯤 는다.

## 시험과 개인정보

- 외부 네트워크 없음: 빌더는 파일만, 서버는 사전만 읽는다. fixture는 SGC·CGNDB 발췌 녹화(저장·재배포가 허용된 출처, 날짜·URL·라이선스 표시)와 합성 사전이다.
- 장소 해석은 날짜를 받지 않는다. 로그에는 입력, 후보 이름, 코드를 어떤 수준에서도 남기지 않는다(M2a `PlaceResolveLogTest` 방식).
- 시험 입력은 데모 4곳과 "London", "Victoria", "Saint John" 및 그 표기 변형만 쓴다.

## 다음 PR

- `feat/place-picker` (web, frontend-dev): S2 입력 폼의 장소 칸 → 제출 때 `GET /api/places/resolve` → 후보가 1개면 바로 쓰고, 여럿이면 `full_name` 목록에서 고르고, `PARTIAL`·`NOT_FOUND` 는 "Enter a neighbourhood or city" 안내. 고른 후보의 `id`·`province.code`·`csd.code` 만 이야기 화면에 넘긴다(이름·생년월일은 주소에 넣지 않음, S2 규칙). 선행: S2 머지와 C2c 머지(그 전에는 위 계약으로 MSW). 크기는 웹 약 250~400으로 보고 계획 때 다시 센다.
- `feat/ca-place-search` (server): `SPOT`·`OUTSIDE_CANADA`·`osm-`. 선행: Nominatim 등급 상승과 T1 D2.

## 리스크와 확인할 것

1. **좌표 없는 CSD**: 리서처 간이 계산으로 5,161개 중 약 1,832개(복수 170 + 없음 1,662)가 B1로 좌표를 못 얻는다. 해석은 되지만 좌표가 `null` 이라 날씨 같은 뒤 항목이 빈다. T2 README의 실제 수를 보고, 많이 쓰이는 도시가 빠졌으면 `overrides.csv` 로 채우는 후속을 제안한다.
2. **동네 25 km 규칙**: 같은 이름이 가까이 둘 있으면 빠진다. 25는 공식 근거가 없는 값이라 README에 적고 상수로 둔다. 데모 동네 1개는 T2에서 확인한다.
3. **`PARTIAL` 의 오해**: "Victoria General Hospital"(위니펙·핼리팩스에 있음)은 Victoria CSD 5곳을 돌려준다. 화면 안내("We couldn't find that exact place")와 후보 고르기로 줄인다(place-picker).
4. **SGC 2026 판**: 나오면 C2a 빌더를 다시 돌린다. README에 판과 내려받은 날을 적는다.
5. **M2a 코드 옮기기**: `feat/place-resolve` 를 머지하지 않는다. `git show` 로 읽어 새 커밋으로 쓰므로 한국 계획서·커밋이 섞이지 않는다.

## PM 결정 필요 (PM 위임 — "추천"으로 진행)

- **C2-P1 PR 나누기** — 셈 합계 약 405 × 2.1~2.9 = 850~1,175.
  - (a) 세 PR: 데이터 → 매처 → API (위 표).
  - (b) 한 PR + 확인점.
  - (c) 두 PR: 데이터 + 매처 / API. 첫 PR이 약 750이다.
  - **추천: (a)**.
- **C2-P2 사전 데이터를 들여오는 방법**
  - (a) 서버 Gradle 작업으로 빌더를 두고, content-curator가 한 번 돌려 파생 CSV만 커밋한다(다시 돌릴 수 있음).
  - (b) content-curator가 코드 없이 손으로 뽑는다(다시 만들 수 없음).
  - (c) 원본을 커밋하고 서버가 시작할 때 만든다(CGNDB 압축 해제 약 77 MB).
  - **추천: (a)**.
- **C2-P3 동네의 기준** (T1 D3 (a)를 구체화)
  - (a) SGC "Place name(s)"(CSD 소속) ∩ CGNDB(`UNP`·`HAM`·`VILG`, 같은 주, CSD 대표점 25 km 안, 1건).
  - (b) CGNDB `UNP` 전부(26,869개. 도로·시골 마을이 섞이고 CSD를 모름).
  - (c) 데모 도시 4곳만 손으로 고른 목록.
  - **추천: (a)**.
- **C2-P4 좌표를 못 정한 CSD**
  - (a) 해석은 하고 좌표는 `null`. 데모·같은 이름 대상만 `overrides.csv` 로 채운다.
  - (b) 그런 CSD를 사전에서 뺀다.
  - (c) 맞는 것 중 첫 번째를 쓴다(추측).
  - **추천: (a)**.
- **C2-P5 사전에 없는 지점 입력** (D5 (a)를 구체화)
  - (a) 입력 앞 1~3낱말이나 마지막 쉼표 조각이 지역이면 `PARTIAL` 로 그 지역을 후보로 주고 다시 입력을 안내한다.
  - (b) 늘 `NOT_FOUND` + 안내.
  - **추천: (a)**. "도시로 돌려주고 다시 입력 안내"와 같다.
- **C2-P6 ★ PM 직접 구현**: C2b T4 `PlaceDictionary`·`PlaceMatcher`(시작 때 검사, 접기 규칙, 쉼표 해석, 정해진 순서). 정규화와 결정적 매칭을 배울 수 있다.
  - (a) PM이 한다.
  - (b) backend-dev가 한다.
  - **추천: (b)**. PM이 2026-11-04 자율 진행을 위임했고, 리서치 결정 10도 (b)였다.

### PM 위임 결정 (2026-11-05, orchestrator) — C2a T1 보고 뒤

- **빌더 규칙 3가지 (계획 문구에 없던 것)**: (a) 그대로 둔다. (가) 같은 주에 접은 이름이 같은 CSD가 둘 이상이면 B1 `unique` 를 주지 않는다. (나) CSD 자기 대표점과 같은 CGNDB 항목은 동네로 넣지 않는다. (다) 두 CSD에 함께 걸리는 동네는 뺀다. 같은 좌표를 여러 CSD에 추측해 붙이지 않기 위해서다.
- **CSD 이름과 같은 동네 (예: Caledon의 `Victoria`, Montréal 안의 `Montréal`)**: (a) C2b T4 매처에 규칙을 더한다 — 입력 전체가 CSD 이름과 맞으면 같은 이름의 동네 후보는 뺀다. 데모 기대값 "Victoria → CSD 5건"은 그대로 둔다.

### PM 위임 결정 (2026-11-05, orchestrator) — C2a T2 보고 뒤

- **T2 위임 판단 3개 승인**: PEI Victoria(Queens) 1102032 → BACOS 대체 유지, 주 별칭은 StatCan Table B 약어만, N.L.·NL처럼 코드와 같은 키로 접히는 별칭은 C2b 매처가 허용한다(같은 주 하나로 본다).
- **SGC 표시 문구 날짜**: StatCan 오픈 라이선스 문구가 요구하는 "reference date"로 바꾼다(SGC 2021 → `2021`). 기록 수정일 2023-04-21은 README의 내려받은 판 정보로 남긴다.
- **T2b 추가 (리스크 1 후속)**: content-curator가 2021년 인구조사 인구 10만 이상 CSD 가운데 좌표가 없는 곳(보고: Québec, Hamilton, Mississauga, Laval, Gatineau, Longueuil, Sherbrooke, Trois-Rivières, Saguenay, Lévis, Moncton 등)을 근거 URL과 함께 `overrides.csv` 로 채운다. 인구 근거는 StatCan 공식 표 주소를 README에 적는다. 다시 빌드한 결과만 `data/places/` 에 커밋한다.

### PM 위임 결정 (2026-11-05, orchestrator) — C2a 리뷰(수정 요청 1회) 뒤

- **필수 1 (SGC 인코딩)**: SGC 두 파일을 Windows-1252로 읽는다. backend-dev가 빌더·녹화 행 시험을 고치고, content-curator가 다시 빌드하고 README 인코딩 설명·수를 고치며, test-engineer가 이름 열의 제어 문자(`\p{Cc}`) 검사를 더한다.
- **권장 1 (대표점 겸 동네 18건)**: (a) 규칙 (나)를 "어떤 CSD의 대표점이든 동네에서 뺀다"로 넓힌다. 규칙 (다)와 같은 목적(같은 좌표를 두 CSD에 붙이지 않음)이다.
- **권장 2 (실제 파일의 B3 정렬 시험)**: test-engineer가 더한다.
- **권장 3 (리서치 문서 인코딩 정정)**: data-researcher가 `canada-place-search.md` 의 "ISO-8859-1"을 "Windows-1252(Latin-1 상위 집합)"로 고친다.
- **권장 4·5 (README 근거 인용, 시험 README 표시 날짜)**: content-curator가 고친다.
- **권장 6**: 그대로 둔다.

### PM 위임 결정 (2026-11-06, orchestrator) — C2a 재리뷰 승인 뒤

- 재리뷰 권장 1: 위 "사용할 외부 출처" 표의 SGC 인코딩을 Windows-1252로 고쳤다.
- 재리뷰 권장 2 → **C2b T4에 더한다**: `NameFold` 가 합자 `œ`→`oe`, `æ`→`ae`(대문자 포함)로 접는다. "Coteau-des-Soeurs" 입력이 `Le Coteau-des-Sœurs` 와 맞아야 한다. 시험을 둔다. C2a 제품 코드가 400줄이라 C2a에는 더하지 않는다.
