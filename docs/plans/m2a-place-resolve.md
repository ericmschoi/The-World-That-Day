# M2a 장소 해석 API와 지역 사전 (M2)

> 브랜치: `feat/place-resolve` (`main` 49248e9 이후) · 작성: planner (2026-10-27) · 상태: **PM 승인 (2026-10-27): M2 공통 결정 모두 추천대로 — #2 (a), #3 (a), #7 (a), #10 (a), #11 (a), #26 (b), M2-P1 (a) ~ M2-P8 (a), M2-P9 (b), M2-P10 (a)**
> 기준: 로드맵 "M2 장소 해석과 입력", 제품 정의서 3·4·12절, `docs/research/place-search.md`, `docs/research/summary.md` 결정 #2·#7·#10·#11·#26, M3a2 공통 API 규약(`ApiProblem`·`ApiExceptionHandler`), `EventCuration`(시작 때 한 번 읽는 큐레이션 파일), `StoryDate`
> M2 묶음 (결정 M2-P1): **M2a**(이 문서) → M2a2 `feat/place-kakao` · M2b `feat/input-and-opening` → M2b2 `feat/place-picker`. 나눈 이유, 진행 순서, M2 공통 PM 결정은 이 문서 끝 "M2 공통" 절.

## 목표

1. `data/places/regions.json`: 시·도 + 시·군·구(+ 시 아래 일반구) 지역 사전. 원천은 법정동코드(결정 #10), 짧은 이름과 별칭을 붙인다.
2. `GET /api/places/resolve?q=` — 사전으로 `REGION` 을 판정하고, 여러 개면 후보 목록을 준다(추측해서 하나를 고르지 않음, 제품 정의서 4절). 사전에 없으면 장소 검색(M2a2 카카오)으로 넘긴다. 이 PR에서는 장소 검색이 꺼져 있어 503 `UPSTREAM_UNAVAILABLE`.
3. 응답 계약을 이 계획에서 확정한다. `NEIGHBORHOOD`·`SPOT`·`NOT_FOUND`·`OUTSIDE_KOREA` 는 M2a2가 같은 계약으로 채우고, M2b2(웹)는 처음부터 이 계약으로 만든다.
4. Spring 기본 400·404·405·500 응답에도 `code` 를 붙인다(결정 M2-P5, 로드맵 후속 후보 "두 번째 API 전").
5. 장소 입력값과 해석 결과(이름·코드·좌표)는 어떤 로그 줄에도 남지 않는다 — 시험으로 지킨다(`CLAUDE.md` 7.6, 결정 M2-P4).

## 범위 밖

- 카카오 로컬(동네·장소·국외 판정) → M2a2. 입력 화면·후보 선택 화면 → M2b·M2b2.
- `REGION` 대표 좌표: `lat`·`lng` 는 `null`. 좌표를 쓰는 기능(M5 등)을 계획할 때 정한다. 동네·장소는 M2a2에서 늘 좌표가 있다.
- 읍·면·동 사전(동은 카카오가 맡는다), 통합된 옛 시·군 이름(마산 등 — 결정 M2-P8).
- 결과 저장·캐시(#11 (a)) — DB 테이블·Flyway 변화 없음. 지도에서 고르기(M2-P2), 공유 링크(M2-P3).
- 런타임 스키마 검증 라이브러리: M3a3 결정 B (a) 그대로 — 손 검증(`EventCuration` 방식), 스키마 파일은 시험에서만.
- 붙여 쓴 이름(`서울특별시종로구`)·오타 보정: 사전은 띄어 쓴 낱말이 정확히 맞을 때만. 나머지는 장소 검색 몫.

## 사용할 외부 출처

| 출처 | 문서 | 지금 확신도 | 이 PR에서 |
|---|---|---|---|
| 법정동코드 API (공공데이터포털) | [place-search.md 3절](../research/place-search.md#3-출처-카드), 10절 D4 | 부분 확인 (D4 대기) — 막힌 항목은 모두 라이선스 해석 | 결정 #10 (a)이면 **T0a**에서 data-researcher가 "확인됨 (결정 #10 (a), 출처 표시)"로 올린다. 서버는 실행 중 부르지 않는다: PM이 한 번 내려받은 파일(T0b)로 content-curator가 사전을 만든다(T1). 확인됨이 아니면 T1을 하지 않고 막힘 보고 — T2~T5는 합성 시험 사전으로 계속한다 |
| 카카오 로컬 | place-search.md 3절 | 부분 확인 (PM 호출 대기) | 쓰지 않는다 (M2a2) |

## API / 데이터 변경

### `GET /api/places/resolve?q=<장소>` (M2a·M2a2·M2b2 공통 계약)

요청 `q` 정규화(`PlaceQuery`): NFC → 제어 문자 제거 → 앞뒤 공백 제거 → 연속 공백(전각 공백 포함)을 하나로. 길이는 정규화 뒤 코드 포인트 1~50.

| 상태 | `code` | 조건 |
|---|---|---|
| 400 | `PLACE_QUERY_REQUIRED` | `q` 가 없거나 정규화 뒤 빈 값 |
| 400 | `PLACE_QUERY_TOO_LONG` | 정규화 뒤 50자 초과 |
| 503 | `UPSTREAM_UNAVAILABLE` | 사전에 없고 장소 검색을 쓸 수 없음 (M2a: 늘 — 꺼져 있음. M2a2: 키 없음·실패·시간 초과·429) |
| 200 | - | 그 밖 |

- 모든 응답(400·503 포함)에 `Cache-Control: no-store`. 에러는 M3a2 형식 그대로이고 `detail`·`instance` 에 입력값이 없다.
- 판정 순서: (1) 사전(`RegionMatcher`)에 맞는 지역이 하나 이상이면 그것만 낸다 — 장소 검색을 부르지 않는다("서울"이 서울역이 되지 않게, 쿼터 절약). (2) 없으면 `PlaceSearch`(M2a는 꺼진 기본 구현 → 503).

200 본문:

```json
{
  "status": "FOUND",
  "candidates": [
    {
      "id": "region-1100000000",
      "type": "REGION",
      "name": "서울",
      "full_name": "서울특별시",
      "address": null,
      "category": null,
      "lat": null,
      "lng": null,
      "sido": { "code": "1100000000", "name": "서울특별시" },
      "sigungu": null
    }
  ],
  "more": false,
  "sources": [
    { "name": "행정안전부 법정동코드 (공공데이터포털)", "url": "https://www.data.go.kr/data/15077871/openapi.do" }
  ]
}
```

| 필드 | 값 |
|---|---|
| `status` | `FOUND`(후보 1개 이상) / `NOT_FOUND`(검색했지만 없음, M2a2) / `OUTSIDE_KOREA`(결과가 모두 대한민국 범위 밖, M2a2). 뒤의 둘은 `candidates: []`. M2a는 `FOUND` 만 낸다 |
| `candidates[].id` | `region-<법정동코드 10자리>` / M2a2: `kakao-address-<b_code>`, `kakao-place-<장소 ID>`. 화면 안에서만 쓰고 어디에도 저장하지 않는다 |
| `type` | `REGION` / `NEIGHBORHOOD` / `SPOT` |
| `name` | 오프닝에 쓰는 짧은 이름. 시·도: 짧은 이름(`서울`). 시·군·구: `<시·도 짧은 이름> <이름>`(`광주 서구`, `경기 광주시`, `서울 종로구`). 일반구: `<시 이름> <구 이름>`(`수원시 장안구`). 동네·장소는 M2a2(`광주 서구 풍암동`, `서울대학교병원`) |
| `full_name` | 후보끼리 구별하는 공식 전체 이름(`서울특별시`, `광주광역시 서구`, `경기도 광주시`) |
| `address`, `category` | `SPOT` 만(M2a2). 나머지 `null` |
| `lat`, `lng` | `NEIGHBORHOOD`·`SPOT` 은 숫자(WGS84, M2a2), `REGION` 은 `null` |
| `sido`, `sigungu` | 사전 기준 `{code, name}`. 시·도 후보는 `sigungu: null`. 일반구는 `sigungu` 가 그 구 |
| `more` | 출처에 보여 준 것보다 결과가 더 있음(M2a2 카카오). 사전 후보는 늘 `false`(모두 낸다) |
| `sources` | 이 답에 쓴 출처(이름·URL). M6b 전체 출처 목록과 M2b2 후보 화면이 쓴다 |

- 사전 후보 순서는 법정동코드 오름차순(추측·인기 순 없음).
- 데모와 시험 질의 (제품 정의서 12절과 그 일부, 4절 예 "중앙병원"만): `서울` → 1개, `광주` → 2개(광주광역시, 경기도 광주시), `서구` → 5개(부산·대구·인천·광주·대전), `광주 서구` → 1개, `광주 서구 풍암동`·`서울대학교병원` → 사전에 없음(M2a 503, M2a2 카카오).

### `RegionMatcher` 규칙 (★ T3)

| # | 규칙 |
|---|---|
| M1 | 정규화한 질의를 공백으로 나눈 낱말 1~3개만 본다. 4개 이상이면 사전에 없음 |
| M2 | 지역마다 맞춰 볼 이름: 공식 이름, 파일의 `short_name`·`aliases`, 그리고 서버가 만드는 "끝의 `시`·`군` 을 뗀 이름"(남는 글자가 2자 이상일 때만 — `수원`, `고성`. `구` 는 떼지 않는다: `중구` → `중` 없음) |
| M3 | 마지막 낱말이 어떤 지역의 이름과 정확히 같고, 앞 낱말들(0~2개)이 차례로 그 지역의 조상(시·도, 시)과 같아야 한다. 모든 낱말이 쓰여야 한다(`광주 서구` 의 `광주` 는 광주광역시 → 서구 하나) |
| M4 | 비교는 정규화한 문자열의 완전 일치. 부분 일치·유사도·띄어쓰기 보정 없음 |
| M5 | 맞는 지역이 0개면 사전에 없음, 1개 이상이면 모두 후보(코드 순) |
| M6 | 외부 호출·DB·로그 없음. 같은 입력은 늘 같은 출력(순수 함수) |

### `data/places/regions.json` (content-curator, T1)

```json
{
  "version": 1,
  "source": {
    "name": "행정안전부 법정동코드 (공공데이터포털)",
    "url": "https://www.data.go.kr/data/15077871/openapi.do",
    "terms": "이용허락범위 제한 없음 (공공누리 유형 표시 없음) — 결정 #10 (a), 출처 표시",
    "retrieved_at": "YYYY-MM-DD"
  },
  "regions": [
    { "code": "1100000000", "level": "sido", "name": "서울특별시", "short_name": "서울", "parent": null, "aliases": ["서울시"] },
    { "code": "1111000000", "level": "sigungu", "name": "종로구", "parent": "1100000000", "aliases": [] }
  ]
}
```

- `level`: `sido` / `sigungu`(시·군·구) / `gu`(시 아래 일반구, 부모는 `sigungu` 인 시). `short_name` 은 `sido` 에 필수, 그 밖에는 없음.
- 별칭(근거 URL은 T1 보고에): 시·도 줄임말(`서울`·`부산`·…·`제주`), 이름이 바뀐 시·도의 옛 공식 이름(예: `강원도`, `전라북도`, `제주도`), 흔한 꼴(`서울시`). 통합된 옛 시·군 이름은 넣지 않는다(M2-P8 (a)).
- 서버 검사(어기면 시작 실패, 파일이 없으면 WARN 1줄과 빈 사전 — `EventCuration` 과 같음): `code` 10자리 숫자·중복 없음, `level` 값, 부모가 있고 단계가 맞음(`sido` 는 부모 없음), 이름·별칭이 비지 않음, 한 지역 안 별칭 중복 없음, `sido` 의 `short_name`. 스키마 `data/schema/regions.schema.json` 은 시험에서 같은 규칙을 본다.

### Spring 기본 응답 (T5, 결정 M2-P5)

| 상황 | 상태 | `code` | `detail` (고정 영어, 입력값 없음) |
|---|---|---|---|
| 없는 경로 | 404 | `NOT_FOUND` | `no such resource` |
| 허용되지 않은 메서드 (`Allow` 헤더 유지) | 405 | `METHOD_NOT_ALLOWED` | `method not allowed` |
| Spring이 400으로 보는 요청 오류(필수 파라미터 없음, 형식 변환 실패, 읽을 수 없는 본문) | 400 | `BAD_REQUEST` | `bad request` |
| 그 밖의 Spring MVC 표준 예외(406·415 등) | 그 상태 | 상태 이름 대문자 스네이크(`NOT_ACCEPTABLE`) | 상태 이름 |
| 처리되지 않은 예외 | 500 | `INTERNAL_ERROR` | `internal error` — ERROR 1줄 `Unhandled error: path=<경로> type=<예외 클래스>` + 스택(쿼리 문자열 없음) |

`ApiProblem` 처리와 `/api/health` 는 그대로. 구현 방법(기존 `ApiExceptionHandler` 확장 또는 `ResponseEntityExceptionHandler` 상속, `spring.mvc.problemdetails`)은 backend-dev가 Boot 4.1 문서로 고르고 보고에 적는다.

## 작업 목록

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0a | 법정동코드 확신도 갱신과 내려받기 명령 | data-researcher | 결정 #10 | (1) 결정이 #10 (a)이면 `place-search.md` 법정동코드 카드와 `summary.md` 2절 표를 "확인됨 (결정 #10 (a), 출처 표시: 행정안전부 법정동코드, 공공데이터포털)"로 새 커밋. (a)가 아니면 고치지 않고 막힘 보고. (2) 데이터셋 페이지 [24]를 다시 열어 수정일·필드·이용허락범위·쿼터가 2026-10-09 기록과 같은지 표로(바뀐 것은 보고). (3) PM 명령 **R1**: 전체 목록을 쪽마다 받아 시·도·시·군·구·일반구 행만 남긴 JSON 파일 하나를 저장소 밖에 만든다. `$DATA_GO_KR_SERVICE_KEY` 만, `curl`·`jq`, 쪽 수는 `totalCount` 로 계산, 요청 사이 1초. 행 고르는 조건(`umd_cd`·`ri_cd`)과 폐지된 코드를 거르는 방법을 문서 근거와 함께(근거가 없으면 "T1에서 파일로 확인"이라고 적음). (4) 키 없이 받을 수 있는 공식 파일이 있으면 대안으로 적되, 확인됨 근거가 없으면 쓰지 않는다. (5) 외부 요청 User-Agent는 저장소 주소 형식만. 문서만, 제품 코드 없음 |
| T0b | 법정동코드 활용신청과 R1 실행 | PM | T0a | 결과 파일이 저장소 밖에 있고 경로를 오케스트레이터에게 준다. 원문 파일은 커밋하지 않는다. (M3a5b T0의 공공데이터포털 계정·`DATA_GO_KR_SERVICE_KEY` 와 같다 — 데이터셋 활용신청만 따로) |
| T1 | 지역 사전 파일과 스키마 | content-curator | T0b | (1) `data/places/regions.json` 에 지금의 시·도 17개와 시·군·구·일반구가 모두 있고, 개수가 R1 파일의 해당 행 수와 같다(보고에 두 수와 폐지 행을 거른 방법). (2) 위 형식이고 `data/schema/regions.schema.json` 을 통과한다. 스키마 예제 `data/schema/examples/regions/` 에 통과 2개, 실패 3개 이상(부모 없음, 코드 중복, 시·도 `short_name` 없음). (3) 별칭 규칙대로, 옛 시·도 이름마다 근거 URL. (4) `source` 블록. (5) 손으로 센 데모 표: `서울` 1, `광주` 2, `서구` 5, `광주 서구` 1 — 파일의 어느 행이 맞는지. (6) 개인 정보 없음 |
| T2 | `RegionDictionary` 읽기·검사, `PlaceQuery` 정규화 | backend-dev | - (T1과 병렬, 합성 시험 사전) | (1) `<app.data-dir>/places/regions.json` 을 시작 때 한 번 읽는다. 없으면 WARN 1줄과 빈 사전, 규칙을 어기면 시작 실패(메시지에 파일 경로와 규칙). (2) 검사 규칙마다 실패 시험 하나. (3) `PlaceQuery` 단위 시험: 50자 통과, 51자 `PLACE_QUERY_TOO_LONG`, 공백·전각 공백·제어 문자만 `PLACE_QUERY_REQUIRED`, 연속 공백 하나로, NFD 입력이 NFC와 같게. (4) T1 파일이 브랜치에 들어온 뒤 실제 파일로 시작하는 시험과 스키마 시험(`DataSchemas`)이 통과. (5) `cd server && ./gradlew check` |
| T3 | ★ `RegionMatcher` (사전 기반 `REGION` 판정) | PM 또는 backend-dev (결정 M2-P9) | T2 | (1) M1~M6 그대로. (2) 합성 사전으로 규칙마다 시험(시·군 꼬리 떼기, `구` 는 안 뗌, 조상 순서, 낱말 4개, 부분 일치 없음). (3) 실제 파일로 데모 표: `서울`·`서울특별시`·`서울시` → 서울특별시 1개, `광주` → 2개(코드 순), `서구` → 5개, `광주 서구`·`광주광역시 서구` → 1개, `서울 종로구`·`종로구` → 1개, `서울  종로구`(공백 둘) → 1개, `광주 서구 풍암동`·`서울대학교병원` → 0개. (4) 외부 호출·DB·로그 없음 |
| T4 | `GET /api/places/resolve` 와 `PlaceSearch` 자리 | backend-dev | T3 | (1) 계약 표 그대로(snake_case, `Cache-Control: no-store`, 문제 형식). (2) MockMvc: `서울` 200 `FOUND` 1개 `REGION`·`lat`/`lng` `null`·`sido.code` `1100000000`·`sources` 1개; `서구` 5개 코드 순; `광주 서구 풍암동` 503 `UPSTREAM_UNAVAILABLE`; `q` 없음·공백 400 `PLACE_QUERY_REQUIRED`; 51자 400 `PLACE_QUERY_TOO_LONG`; 모든 에러의 `detail`·`instance` 에 입력값 없음. (3) `PlaceSearch` 인터페이스(결과: 후보·`more`·`NOT_FOUND`·`OUTSIDE_KOREA` / 사용 불가)와 꺼진 기본 구현 — M2a2가 카카오 구현을 넣는다. 꺼진 구현은 DEBUG 1줄 `Place search off` 만(값 없음). (4) 로그 시험(`CapturedOutput`, DEBUG까지): 데모 3개·400·503 경로에서 어떤 줄에도 질의 문자열·지역 이름·코드가 없다. (5) C1 출력 없음. (6) `./gradlew check` |
| T5 | Spring 기본 응답에 `code` | backend-dev | T4 (M2-P5 (a)) | (1) 위 표 그대로. (2) MockMvc: `GET /api/nope` 404 `NOT_FOUND` `application/problem+json`; `POST /api/places/resolve` 405 `METHOD_NOT_ALLOWED` + `Allow`; 시험 전용 컨트롤러의 예외 → 500 `INTERNAL_ERROR`, ERROR 줄에 쿼리 값 없음. (3) 기존 `/api/story/world`·`/api/health` 시험을 **고치지 않고** 통과. (4) `./gradlew check` |
| T6 | 검증과 전체 시험 | test-engineer | T1~T5 | (1) T1~T5 기준 표. (2) `./gradlew check` 2회, 로그 시험(T4 (4)) 3회 반복 — 개수 보고. (3) 데모 질의 표의 실제 응답(MockMvc, 실제 사전 파일). (4) C1·C2 출력 없음. (5) S1·S2 보고 |
| T7 | 실제 서버 확인 | PM | T6 | `cd server && ./gradlew bootRun` 뒤 `curl -sS -G localhost:8080/api/places/resolve --data-urlencode 'q=서울'`(그리고 `광주`, `서구`, `광주 서구 풍암동`)의 `status`·후보 수가 데모 표와 같다. 서버 로그에 그 낱말이 없다 |

담당별: data-researcher 1, content-curator 1, backend-dev 4(★ T3 포함, M2-P9), test-engineer 1, PM 2. 합계 9개.

검사 명령 (저장소 루트):

```
# C1 — place 패키지가 질의·이름·좌표를 로그에 넘기지 않는지. 출력이 없어야 한다
grep -rnE 'log\.(trace|debug|info|warn|error)\(.*\b(q|query|raw|name|fullName|lat|lng)\b' server/src/main/java/io/github/ericmschoi/theworldthatday/place

# C2 — 시험 메서드 표시가 지워지지 않았는지. 출력이 없어야 한다
git diff -U0 main...HEAD -- server/src/test | grep -E '^-[[:space:]]*@(Test|ParameterizedTest)'

# S1 — 제품 코드·설정 (시험 제외)
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
```

## 크기 (400줄 규칙)

파일별 셈에 최근 서버 실측 비율 ×2.5(M3a5 ×2.5, M3a5a ×2.76, M3a4c ×1.8 — 범위 ×1.8~×2.8)를 곱한다. `data/` 와 시험은 따로 보고한다.

| 작업 | 파일별 셈 | ×2.5 |
|---|---|---|
| T2 사전 읽기·검사 + 질의 정규화 | 약 60 | 약 150 |
| T3 ★ 매처 | 약 40 | 약 100 |
| T4 API·`PlaceSearch` 자리·DTO | 약 55 | 약 140 |
| T5 기본 응답 | 약 25 | 약 60 |
| **합계** | 약 180 | **약 450 (325~505)** |

400을 조금 넘는다. **확인점 1**: T3 뒤 누적 S1 300을 넘으면 알림(멈추지 않음). **확인점 2**: T4 뒤 누적 S1 450을 넘으면 T5 전에 멈추고 T5를 M2a2로 옮길지 묻는다. 결정 M2-P1.

## 병렬 실행

- T0a(문서) → T0b(PM) → T1(데이터)과 T2~T5(서버)는 파일이 겹치지 않아 동시에 한다. 서버는 `server/src/test/resources` 의 합성 사전으로 시험하고, T1이 들어온 뒤 T2 (4)·T3 (3)·T6을 실제 파일로 확인한다.
- 이 PR은 M2b(`feat/input-and-opening`)와 다른 worktree에서 동시에 한다. M2b의 서버 쪽은 `StoryDate` 하나라 이 PR의 파일과 겹치지 않는다.

## 리스크와 확인할 것

1. **R1 명령이 한 번에 맞지 않을 수 있다** — 응답 모양(쪽 나누기, 폐지 행)을 문서로만 안다. T0a·T0b를 되풀이할 수 있고, 그동안 서버 작업은 막히지 않는다.
2. **흔한 이름이 겹친다** — `서구` 5개, `광주` 2개(광주광역시·경기도 광주시). 제품 정의서 4절대로 후보를 모두 보여 주고 고르게 한다. 사용자가 번거로워하면 결과 검수(M2 끝) 때 다시 본다.
3. **옛 이름** — 1950~1990년대 출생자는 통합 전 이름(마산·진해·이리 등)을 넣을 수 있다. 사전은 지금 이름과 바뀐 시·도 이름만 안다. 나머지는 M2a2 카카오가 맡고, 못 찾으면 "동 이름으로 다시" 안내(M2-P8).
4. **M2a만 머지된 동안** 동네·장소는 503이다. M2b2 화면은 이것을 "지금은 찾을 수 없음"으로 보인다. M2a2가 머지되면 같은 계약으로 채워진다.
5. **법정동코드 "제한 없음"의 뜻** — 비상업 MVP 전제(#1 (a), #10 (a)). 상업화를 정하면 #10 (b) 문의를 한다. 출처는 `sources` 와 M6b 출처 목록에 늘 표시한다.
6. **사전 코드와 카카오 코드 연결** — M2a2는 카카오 `b_code` 앞자리로 사전의 시·도·시·군·구를 찾는다. 자릿수가 같은지는 K2·K6에서 확인(M2a2 T0b). 다르면 M2a2에서 이름 맞춤으로 바꾼다.
7. **크기** — 위 확인점. 서버 실측 비율이 ×2.9까지 나온 적이 있다(M3a5 T2).

## PM 결정 필요

M2 공통 결정(아래 절)을 이 계획 승인 때 함께 정한다. 이 PR에 직접 걸린 것: #10, #11, M2-P1, M2-P4, M2-P5, M2-P8, **M2-P9(★ T3)**.

---

## M2 공통

### PR 나누기 (결정 M2-P1)

로드맵은 M2를 2개(M2a, M2b)로 적었지만, 머지 코드와 최근 실측으로 세면 M2a 약 950, M2b 약 600이다. 또 카카오는 아직 "확인됨"이 아니라(`CLAUDE.md` 7.2) 카카오에 기대는 일과 기대지 않는 일이 갈린다. 그래서 4개로 나눈다.

| PR | 브랜치 | 담당 | S1 기대 | 선행 | 지금 시작 |
|---|---|---|---|---|---|
| **M2a** 지역 사전·API 계약·`REGION` | `feat/place-resolve` | data-researcher, content-curator, backend-dev, test-engineer, PM | 약 450 (325~505) | 결정 #10·#11, PM T0b(데이터만) | **예** (서버 T2~T5는 바로) |
| **M2a2** 카카오 로컬: 동네·장소·국외 | `feat/place-kakao` | PM, data-researcher, backend-dev, frontend-dev(방침 한 줄), test-engineer | 약 500 (360~560), T0b 뒤 다시 셈 | M2a 머지 + PM 카카오 키·K1~K6(+K7) + data-researcher "확인됨" | 아니오 (PM T0a 대기) |
| **M2b** 입력 폼·날짜 검증·동의 이동·오프닝·데모 버튼 제거 | `feat/input-and-opening` | backend-dev(하한), frontend-dev, test-engineer, PM | 약 400 (340~495, 서버 하한 약 20 포함) | 결정 #3, M2-P6, M2-P7 | **예** (M2a와 동시) |
| **M2b2** 장소 해석 연결·후보 선택 화면 | `feat/place-picker` | frontend-dev, test-engineer, PM | 약 200 (165~240) | M2b 머지(같은 파일). 계약은 이 문서 — M2a 머지 전에 MSW로 만들 수 있고, 실제 확인은 M2a 뒤(`REGION`)·M2a2 뒤(동네·장소) | M2b 뒤 |

```
서버: M2a ──────────────> M2a2 (PM: 카카오 키·K1~K7 → data-researcher 확인됨 → planner 갱신)
         └ PM T0b(법정동코드 파일) → content-curator T1
웹:   M2b (+ 서버 StoryDate 하한) ──> M2b2 (실제 확인: REGION은 M2a 뒤, 동네·장소는 M2a2 뒤)
```

- M2a ∥ M2b: 다른 브랜치·worktree, 파일이 겹치지 않는다(PM 병렬 선호).
- M2b2와 M2a2도 서로 기다리지 않는다(계약이 고정). M2a2가 머지되면 웹 코드 변경 없이 동네·장소 후보가 나온다.
- 로드맵 M2에서 이미 끝난 미룬 항목: API 클라이언트 타임아웃·`AbortSignal`(M3b2 — `apiGet` 10초), `SectionId` 타입(M3b1). 화면 전환 포커스 이동은 M2b T4.

### M2 공통 PM 결정

| # | 질문 | 선택지 | 추천 |
|---|---|---|---|
| #2 | 지도·로드뷰·장소 제공자 | (a) 카카오 통일 / (b) 카카오 + 네이버 거리뷰 / (c) Google 통일 | **(a)**. M2a2는 카카오 로컬. 거리뷰 촬영일이 필요하면 M4 계획 때 (b)를 다시 본다 |
| #3 | 지원 시작 날짜 | (a) 1950-01-01 / (b) 1957 / (c) 1966 | **(a)**. 시작을 올려도 서울 밖 섹션은 채워지지 않는다. 빈 섹션은 정직하게 안내한다 |
| #7 | Google 결과를 다른 회사 지도와 같은 화면에 | (a) Google을 장소 검색·동네 섹션에 쓰지 않음 / (b) 약관 원문을 본 뒤 / (c) 카드 분리 | **(a)** (#2 (a)와 맞음) |
| #10 | 법정동코드 "이용허락범위 제한 없음"의 해석 | (a) 비상업 MVP에서 출처를 표시해 쓴다 / (b) 행정안전부에 문의한 뒤 / (c) 다른 원천 | **(a)**. M2a T1의 전제. 상업화를 정하면 (b) |
| #11 | 장소·지도 응답 저장 | (a) 저장하지 않음(한 요청 안에서만, `REGION` 은 사전) / (b) 카카오 장소 ID만 저장 / (c) 카카오에 서면 문의 뒤 | **(a)**, (c)는 PM이 원하면 병행. 브라우저가 고른 후보를 기억해 뒤 섹션 요청에 다시 보내는 구조가 "재사용"인지도 그 문의에 넣는다 |
| #26 | 대한민국 밖 장소 판정 | (a) 결과 0건이면 "찾지 못함" 안내에 "대한민국 밖은 아직 지원하지 않음"을 함께, 결과가 모두 한국 좌표 범위 밖이면 국외 안내 / (b) (a) + PM 확인 명령 K7(질의 `도쿄` 1회)로 카카오의 실제 동작을 본다 | **(b)**. K1~K6을 어차피 실행하므로 비용이 작고, (a)의 규칙이 맞는지 안다 |
| M2-P1 | PR 나누기 | (a) 4개(위 표) / (b) 로드맵대로 2개(M2a 약 950, M2b 약 600) / (c) 3개(M2b와 M2b2를 합침, 약 600) | **(a)**. 카카오를 기다리지 않는 M2a·M2b를 지금 동시에 시작하고, 각 PR이 400 안팎이다 |
| M2-P2 | 지도에서 직접 위치를 고르는 기능 (제품 정의서 4절) | (a) M2에 넣지 않는다. 찾지 못하면 동 이름으로 다시 넣게 안내. 카카오 지도 SDK를 확인하는 M4 계획 때 다시 본다 / (b) M2에 넣는다(카카오 지도 SDK가 "미확인"이라 지금은 막힘) / (c) MVP에서 뺀다 | **(a)** |
| M2-P3 | 결과 공유 링크 | (a) 만들지 않는다(제품 정의서 9절 범위 밖, 결과 주소에 입력값 없음 유지) / (b) 입력값 없는 임시 토큰 링크(서버에 입력을 저장해야 해 #11·개인정보와 충돌) | **(a)** |
| M2-P4 | Wikimedia WARN 줄의 문서 제목(날짜를 짐작할 수 있음) | (a) 그대로 둔다. M2는 장소 값을 어떤 로그에도 남기지 않으므로(M2a T4 (4), M2a2 로그 시험) 한 줄에 날짜와 장소가 함께 남지 않는다. 날짜와 장소가 한 요청에 들어오는 첫 기능(M4) 계획 때 다시 본다 / (b) 지금 날짜 없는 이름표로 바꾼다(제품 코드 약 30줄 + 기존 로그 단언 여러 개 변경) | **(a)** |
| M2-P5 | Spring 기본 404·405·500에 `code` | (a) M2a T5 / (b) M6b | **(a)**. 두 번째 API 전에 하기로 한 항목이고 약 60줄 |
| M2-P6 | 생년월일 입력 모양 | (a) 년·월·일 선택 상자 3개 / (b) 브라우저 날짜 입력(`type="date"`) | **(a)**. 모바일·브라우저마다 같은 모양이고, "2월 30일"을 고를 수 있어 거부 문구를 정확히 보일 수 있다. (b)는 코드가 작지만 iOS·안드로이드 표시와 1950년대 연도 고르기가 제각각이다 — M2b 계획 |
| M2-P7 | 오프닝에 날짜·장소를 크게 보이는데, 같은 화면의 YouTube IFrame API 스크립트가 화면 내용을 읽을 수 있음(방침 (g), 로드맵 M2b) | (a) 제품 정의서 5절대로 보이고, 방침에 "같은 화면의 YouTube 스크립트가 화면에 보이는 내용(입력한 날짜·장소 포함)을 읽을 수 있다"를 더한다 / (b) 오프닝에 날짜만 보이고 장소는 뺀다 / (c) 영상 카드가 있는 동안 오프닝을 접는다 | **(a)**. 날짜는 사건 목록으로도 짐작할 수 있고, (b)·(c)는 이야기의 첫 문장을 해친다 |
| M2-P8 | 통합된 옛 시·군 이름(마산·진해·이리 등) | (a) 사전은 지금 이름 + 바뀐 시·도 공식 이름만. 나머지는 카카오에 맡기고 결과 검수(M2 끝) 때 다시 / (b) content-curator가 통합 시·군 옛 이름 목록을 근거 URL과 함께 만든다(M2a T1 범위가 커짐) | **(a)** |
| M2-P9 | ★ M2a T3 `RegionMatcher` 를 누가 | (a) PM 직접 / (b) backend-dev | **(b)**, 다만 배울 것이 가장 많은 작업이다: 한국어 낱말 정규화(NFC·공백), 규칙표를 순수 함수와 단위 시험으로 옮기기, 애매하면 고르지 않고 후보로 내는 설계. 외부 호출·DB가 없어 처음 해 보기 좋다. 서버 줄을 빨리 끝내려면 (b) |
| M2-P10 | ★ M2b T1 `StoryDate` 하한 | (a) PM 직접 / (b) backend-dev | **(a)**. 제품 코드 약 10줄 + 경계 시험(1949-12-31·1950-01-01)이라 작고, 공통 에러 규약(`ApiProblem`)과 MockMvc를 직접 다뤄 본다. 웹은 이 작업을 기다리지 않는다(웹이 따로 같은 하한을 검사) |

**PM 결정 (2026-10-27)**: 모두 추천대로 — #2 (a), #3 (a), #7 (a), #10 (a), #11 (a), #26 (b), M2-P1 (a) ~ M2-P8 (a), M2-P9 (b), M2-P10 (a). ★ M2a T3은 backend-dev, ★ M2b T1은 PM 직접.

M2a2의 ★ 후보(카카오 응답 → 후보 분류, 순수 함수)는 PM T0a·data-researcher T0b로 규칙을 확정한 뒤 M2a2를 시작할 때 묻는다.
