# M2a2 카카오 로컬로 동네·장소 찾기 (M2)

> 브랜치: `feat/place-kakao` (M2a 머지 뒤 `main` 에서) · 작성: planner (2026-10-27) · 상태: **PM 승인 (2026-10-27, M2 공통 결정 추천대로) — 구현은 막힘(카카오 로컬 "부분 확인 (PM 호출 대기)")**
> 기준: `docs/plans/m2a-place-resolve.md`(API 계약, `PlaceSearch`, `RegionMatcher`), `docs/research/place-search.md` 2절 질문 1~10·6절 K1~K6, 결정 #2·#11·#12·#26, `YoutubeDataApiClient`·`YoutubeKeyCheck`(키 있는 외부 어댑터: 헤더 키, 1회 요청, 고정 원인 WARN), `WikimediaHttpClient`(동시 요청 제한 패턴)
> **`CLAUDE.md` 7.2**: 카카오 로컬이 "확인됨"이 되기 전에는 T1~T5를 시작하지 않는다. PM T0a와 data-researcher T0b가 끝나면 오케스트레이터가 planner를 불러 이 계획(규칙 K-R1~K-R9의 사실, 크기, ★ 결정)을 갱신하고 PM 승인을 다시 받는다.

## 목표

1. M2a의 `PlaceSearch` 에 카카오 로컬 구현을 넣는다. 사전에 없는 질의를 주소 검색 → (없으면) 키워드 검색으로 찾아 `NEIGHBORHOOD`·`SPOT` 후보를 낸다.
2. 국외 판정(#26)과 `NOT_FOUND`, 카카오 실패·키 없음은 그 부분만 503(M2a 계약 그대로).
3. 결과를 저장하지 않는다(#11 (a)). 질의·결과·키는 로그에 없다.
4. 개인정보처리방침에 장소 출처를 더한다(M3b3b 리스크 4·8).

## 범위 밖

- 지도 SDK·스카이뷰·로드뷰(M4), 지도에서 고르기(M2-P2).
- 좌표→행정구역(`coord2regioncode`) 호출: 후보마다 부르면 요청이 15배가 된다. 키워드 결과의 시·도·시·군·구는 주소 글자를 `RegionMatcher` 로 맞춘다(K-R5). 필요하면 M4·M5에서 고른 후보 하나에만 부른다.
- 쿼터 계수기: 하루 100,000건(API마다)이고 로컬 데모라 두지 않는다. 429는 실패로 처리한다.
- 2쪽 이상 받기: 1쪽(최대 15개)만. 더 있으면 `more: true` 로 좁혀 입력하게 한다.

## 사용할 외부 출처

| 출처 | 문서 | 지금 확신도 | 확인됨으로 올리는 방법 |
|---|---|---|---|
| 카카오 로컬 API (주소 검색·키워드 검색) | [place-search.md 3절](../research/place-search.md#3-출처-카드) | 부분 확인 (PM 호출 대기) — 막힌 필수 항목: 커버리지(데모 결과), 캐싱·저장, 출처 표시, 상업 이용 | PM T0a(K1~K6, #26 (b)면 K7) + 결정 #11 (a)(저장 안 함 — 캐싱 항목) + 비상업 #1 (a)(상업 항목 해당 없음) → data-researcher T0b |

## API / 데이터 변경

API 모양은 M2a 계약 그대로다. 바뀌는 것: 사전에 없는 질의가 503 대신 `FOUND`(동네·장소)·`NOT_FOUND`·`OUTSIDE_KOREA` 를 받는다. `sources` 에 `{ "name": "카카오 로컬", "url": <T0b가 정한 주소> }` 가 붙는다.

### 설정 (`application.yml`, `.env.example`)

```yaml
# Kakao Local API (plan M2a2): neighborhoods and spots not in the region dictionary. The key stays in
# the environment (KAKAO_REST_API_KEY, root .env) and is sent in the Authorization header only. An
# empty key turns place search off: no request is sent and a WARN is logged once at startup.
kakao:
  api-key: ${KAKAO_REST_API_KEY:}
  api-url: https://dapi.kakao.com/v2/local
  connect-timeout: 2s
  read-timeout: 3s
  max-concurrent-requests: 4
```

`.env.example` 에 `# KAKAO_REST_API_KEY=` (이름만, 주석). 시험 설정은 빈 키, `server/build.gradle.kts` `tasks.test` 에서 `environment.remove("KAKAO_REST_API_KEY")`(M3a5 리뷰 선례).

### 분류 규칙 (K1~K7 결과로 T0b가 확정 — 지금은 공식 문서 필드 기준)

| # | 규칙 |
|---|---|
| K-R1 | 주소 검색(`/search/address.json`, `size=10`)을 먼저 부른다 |
| K-R2 | 주소 문서: `address_type` = `REGION`(지명)이고 `address.region_3depth_name` 이 있으면 `NEIGHBORHOOD`(이름 `<시·도 짧은 이름> <시·군·구> <동>`, 예 `광주 서구 풍암동`). `REGION` 인데 3단계가 비면 `b_code` 로 사전의 지역을 찾아 `REGION` 후보로(보통 사전이 먼저 잡음). `REGION_ADDR`·`ROAD_ADDR`(번지가 있는 주소)는 `SPOT`(이름은 건물 이름이 있으면 그것, 없으면 주소) |
| K-R3 | 주소 문서가 0개면 키워드 검색(`/search/keyword.json`, `size=15`)을 부르고, 문서마다 `SPOT`(이름 `place_name`, `address` 는 도로명·없으면 지번, `category` 는 `category_name` 의 마지막 단계) |
| K-R4 | 좌표(`x`=경도, `y`=위도, 문자열)를 읽을 수 없거나 대한민국 범위(위도 33.0~38.9, 경도 124.5~132.0) 밖인 문서는 뺀다. 문서가 있었는데 모두 빠지면 `OUTSIDE_KOREA`, 문서가 처음부터 0개면 `NOT_FOUND` |
| K-R5 | `sido`·`sigungu`: 주소 문서는 `b_code` 앞 2·5자리(+0)로 사전에서, 키워드 문서는 주소 글자의 앞 낱말들을 `RegionMatcher` 로 맞춰서. 못 맞추면 `null` |
| K-R6 | 순서는 카카오가 준 그대로. 같은 `id` 는 하나만. 최대 15개. `more` = 주소는 `meta.total_count` > 낸 수, 키워드는 `meta.is_end == false` |
| K-R7 | 하나를 골라 주지 않는다. 후보가 1개면 그 1개로 `FOUND`(웹이 바로 진행) |
| K-R8 | 두 요청은 차례로, 각각 1회(재시도 없음), 읽기 3초 — 최악 약 6초(웹 상한 10초 안). 동시 요청은 4개까지, 자리가 날 때까지 읽기 시간만큼 기다리고 못 얻으면 실패 |
| K-R9 | 실패(연결·시간 초과, 401·403·429·5xx, 읽을 수 없는 본문) → 503 `UPSTREAM_UNAVAILABLE`. WARN 1줄 `Kakao call failed: call=<address|keyword> cause=<status NNN|예외 클래스>` — 질의·주소·키·본문 없음 |

### 시험 fixture (결정 #12 (a))

카카오 응답은 **공식 문서의 필드 구조로 손으로 만든 합성 JSON**만 쓴다(`server/src/test/resources/kakao/`, 같은 폴더 README에 "합성, 근거: 개발 가이드 주소, 만든 날"). 질의는 데모 장소 3개와 "중앙병원"(제품 정의서 4절 예), 값(좌표·ID·주소)은 둥근 수와 가짜 값. PM이 K 명령으로 본 실제 값은 넣지 않는다. 국외는 데모 질의에 범위 밖 좌표를 준 합성 응답으로 시험한다.

## 작업 목록

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T0a | 카카오 앱·키와 확인 명령 | PM | 결정 #2·#11·#26 | (1) 카카오 Developers 앱에서 카카오맵 API 사용 설정, 이 계정에서 카카오맵을 **처음** 켠 앱인지(무료 쿼터) 확인. (2) REST 키를 루트 `.env` 의 `KAKAO_REST_API_KEY` 에. (3) `place-search.md` 6절 K1~K6 실행(#26 (b)면 K7: K1과 같은 모양에 `query=도쿄`). (4) 응답 원문은 저장하지 않고 "응답에서 확인할 것"의 필드 사실만 오케스트레이터에게 |
| T0b | 카카오 로컬 다시 확인 → "확인됨" | data-researcher | T0a | (1) `place-search.md` 를 새 커밋으로: K 결과(필드 사실)와 K-R1~K-R9 각각이 맞는지 표, 커버리지 "확인됨" 여부. (2) 개발 가이드·이용 정책·쿼터·운영 정책을 다시 열어 2026-10-08 기록 뒤 바뀐 것(2026-07-21 이용 방식 변경 이후 공지 포함)을 표로. (3) 캐싱·저장: #11 (a) 저장 안 함 설계로 "확인됨", 출처 표시: 의무 조항 유무와 우리가 붙일 문구(`sources` 이름·URL), 상업 이용: 비상업 #1 (a)로 해당 없음. (4) 종합 확신도를 "확인됨"으로 올리거나, 못 올리면 막힘과 선택지(#2 (c) 등). (5) `summary.md` 2절 표 갱신. (6) User-Agent는 저장소 주소 형식만. 문서만 |
| (계획 갱신) | T0b 결과로 이 계획 갱신 | 오케스트레이터가 planner 호출 | T0b | 규칙·크기·★ 다시 정리, PM 재승인. 그 전에는 T1~ 시작 안 함 |
| T1 | 카카오 설정·키·시작 확인 | backend-dev | 계획 갱신 | (1) 위 설정, 빈 키면 시작 WARN 1줄 `KAKAO_REST_API_KEY is empty: place search is disabled` 이고 `PlaceSearch` 는 M2a의 꺼진 동작(503). (2) 값 검사: 시간·동시 수가 양수, URL `https`. (3) `.env.example` 이름 한 줄, `tasks.test` 환경 변수 제거, 시험 설정 빈 키 — 키를 내보낸 셸에서도 `check` 통과. (4) `./gradlew check` |
| T2 | `KakaoLocalClient` (주소·키워드) | backend-dev | T1 | (1) `Authorization: KakaoAK <키>` 헤더만, 키·질의가 로그·예외 메시지에 없음. (2) K-R8·K-R9 그대로: 1회, 연결 2초·읽기 3초, 동시 4개(WireMock 지연으로 5번째 요청이 기다렸다 실패하는 시험), 인터럽트면 플래그 유지. (3) 합성 fixture로 200·0건·401·429·500·읽을 수 없는 본문·시간 초과 시험. (4) 로그 시험: 모든 실패 경로에서 WARN 1줄, 질의·좌표·키 없음. (5) `./gradlew check`. **확인점 1**: T2 뒤 누적 S1 300을 넘으면 T3 전에 멈추고 T3·T4를 따로 낼지 묻는다 |
| T3 | ★ 카카오 문서 → 후보 분류 (순수 함수) | PM 또는 backend-dev (M2a2 시작 때 결정) | T2 | (1) K-R2~K-R7 그대로, 외부 호출·로그 없음. (2) 합성 fixture 표: `광주 서구 풍암동` → `NEIGHBORHOOD` 1개(`name` `광주 서구 풍암동`, `sido`·`sigungu` 사전 코드); `서울대학교병원` → 주소 0건 → 키워드 `SPOT` 여러 개(카카오 순서, `category` 마지막 단계); `중앙병원` → 15개 + `more: true`; 범위 밖 좌표만 → `OUTSIDE_KOREA`; 0건 → `NOT_FOUND`; 같은 `id` 둘 → 하나 |
| T4 | `KakaoPlaceSearch` 연결 | backend-dev | T3 | (1) 키가 있으면 `PlaceSearch` 가 카카오 구현. 사전에 맞으면 카카오를 부르지 않는다(WireMock 요청 0 확인 — `서울`·`서구`). (2) MockMvc + WireMock: 데모 3개·`중앙병원`·국외·0건·카카오 500 → 계약대로(`Cache-Control: no-store`, 503 `UPSTREAM_UNAVAILABLE`). (3) 저장 없음: 이 PR에 Flyway·JPA 변화 없음(C1). (4) 로그 시험(DEBUG까지, 3회 반복): 어떤 줄에도 질의·후보 이름·주소·좌표·카카오 ID·키가 없다. (5) `./gradlew check`. 확인점 2: T4 뒤 600을 넘으면 알림 |
| T5 | 개인정보처리방침 장소 출처 + 시행일 | frontend-dev | T4 | (1) `web/privacy.html` "정보를 쓰는 법과 공유"에 "지역 목록에서 찾지 못한 장소는 장소 이름만 카카오 로컬 API로 보냅니다. 날짜는 보내지 않고, 받은 결과는 저장하지 않습니다." (2) 시행일은 PM이 머지하는 날 한 줄 커밋. (3) `privacy.test.ts` 가 새 문장을 확인. (4) `npm run lint && npm run format:check && npm run typecheck && npm test` |
| T6 | 검증 | test-engineer | T1~T5 | (1) 기준 표. (2) `./gradlew check` 2회, 로그 시험 3회. (3) 외부 네트워크 호출 없음(모든 카카오 요청이 WireMock). (4) C1·C2 출력 없음. (5) S1·S2 |
| T7 | 실제 호출 확인 | PM | T6 | 앱에서(또는 `curl`) `광주 서구 풍암동`·`서울대학교병원`·`중앙병원`(#26 (b)면 `도쿄`)의 `status`·후보 수·첫 후보 이름을 표로. 기대와 다르면 data-researcher·planner로 돌아감. 서버 로그에 그 낱말이 없다 |

담당별: PM 2, data-researcher 1, backend-dev 4(★ T3 포함), frontend-dev 1, test-engineer 1. 합계 9개(계획 갱신 단계 제외).

```
# C1 — 장소 결과를 저장하지 않는지. 둘 다 출력이 없어야 한다
git diff --name-only main...HEAD -- server/src/main/resources/db/migration
git diff main...HEAD -- server/src/main/java | grep -E '^\+.*@(Entity|Table)'

# C2 — 시험 표시가 지워지지 않았는지. 출력이 없어야 한다
git diff -U0 main...HEAD -- server/src/test web/src | grep -E '^-[[:space:]]*@(Test|ParameterizedTest)|^-[[:space:]]*(it|test)\('
```

## 크기 (400줄 규칙)

| 작업 | 파일별 셈 | ×2.5 |
|---|---|---|
| T1 설정·키·시작 확인 | 약 40 | 약 100 |
| T2 클라이언트 | 약 70 | 약 175 |
| T3 ★ 분류 | 약 55 | 약 140 |
| T4 연결 | 약 35 | 약 90 |
| T5 방침 | 약 4 | 약 5 |
| **합계** | 약 205 | **약 500 (360~560)** |

400을 넘는다. 확인점 1(T2 뒤 300, 멈춤)·확인점 2(T4 뒤 600, 알림). T0b 뒤 계획 갱신 때 다시 센다 — 나누면 M2a2(설정·클라이언트, 흐름에 연결 안 함) → M2a3(분류·연결) 순서(M3a5 → M3a5a 방식).

## 리스크와 확인할 것

1. **"서울"이 지명이 아니라 장소로 올 수 있다** — 사전이 먼저 잡아 카카오를 부르지 않는다(M2a). 사전에 없는 지역 표현만 카카오로 간다.
2. **첫 후보가 본원이 아닐 수 있다(서울대학교병원)** — 고르지 않고 목록을 보인다(K-R7). 순서는 카카오 그대로.
3. **문을 닫았거나 이름이 바뀐 병원** — 카카오에 영업 상태가 없다. `NOT_FOUND` 안내가 주 경로(동 이름으로 다시, M2b2 문구).
4. **국외 판정이 거칠다** — 카카오에 국가 필드가 없다. K7로 실제 동작을 보고(#26 (b)), 범위 상자는 독도·마라도를 포함하게 잡는다.
5. **저장 해석(#11)** — 서버는 저장하지 않지만, 웹이 고른 후보의 좌표를 기억했다가 M4·M5 요청에 다시 보낼 수 있다. "재사용"인지는 카카오 문의(#11 (c)) 몫. M4 계획 때 다시 본다.
6. **무료 쿼터는 계정 상태에 달림** — 처음 카카오맵을 켠 앱이 아니면 유료다(T0a (1)). 다르면 PM이 정한다.
7. **약관·문서가 자주 바뀜** — T0b에서 다시 확인. 시험은 합성 fixture라 실제 모양이 바뀌어도 시험은 통과할 수 있다 → PM T7이 막는다.
8. **크기** — 위 확인점. 새 외부 어댑터는 ×2.36~×2.9였다.

## PM 결정 필요

- PM 결정 (2026-10-27) (D): M2a 확인점 2를 넘으면 M2a T5(Spring 기본 응답 `code`, 약 60줄)가 이 PR로 온다 — T0b 뒤 계획 갱신 때 작업 목록에 넣는다.

M2 공통 결정(`docs/plans/m2a-place-resolve.md` "M2 공통")의 #2·#11·#26이 이 PR의 전제다. ★ T3(카카오 문서 → 후보 분류: 규칙표를 순수 함수로, 합성 fixture 설계)의 담당은 T0b 뒤 계획 갱신 때 묻는다.
