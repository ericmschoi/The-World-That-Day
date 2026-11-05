# 로드맵

> 작성: planner (2026-11-04, 2026-11-05 고침) · 기준: `docs/plans/canada-research.md` 의 로드맵 개요 C0~C8과 PM 결정(2026-10-29), `docs/research/canada-summary.md`(T9 판정표·결정·PM 명령), PM 위임(2026-11-04, `docs/progress.md` 375~376행), 승인된 기능 계획 `c1-english-ui.md`·`c1-input-and-opening-ca.md`·`c2-english-events.md`, 초안 `c2-ca-place-resolve.md`, `docs/product-brief.md`(캐나다 문구는 아직 반영 전 — 제안은 `docs/plans/brief-changes-canada.md`)
> 상태: **캐나다 전환으로 다시 씀, PM 확인 대기.** 한국판 로드맵(M0~M6, 처음 승인 2026-10-08 ~ 마지막 갱신 2026-10-27)은 git 기록에 있다(`main` f07e420의 이 파일). 한국판의 결정 번호(#1~#27, M3-Pn 등)는 그 판과 `docs/research/summary.md` 를 따른다.
> 표기: "결정 N" = `canada-research.md` 의 PM 결정(2026-10-29). "A1", "T1 D1", "T5 D2", "X4" = `canada-summary.md` 6절 결정 ID. "명령 #N" = 같은 문서 4절 명령 번호. "S1-Pn", "S2-Pn", "S3 Dn", "C2-Pn" = 각 기능 계획의 PM 결정. "#5" 같은 한국판 번호는 그대로 쓴다(공개 배포 #5).
> 기능 하나 = 브랜치 하나 = PR 하나. 브랜치 이름에 "(임시)"가 붙은 것은 기능 계획에서 확정한다.

## 현재 위치 (2026-11-05)

- **완료**: C0 캐나다 리서치 (PR #30, `main` bea33b1), C1 S1 영어 화면 (PR #31, `main` cc9af18). 한국판: M0 (PR #1), M1 (PR #2·#3), M3 22개 (PR #5~#28, M3a5a까지), 후속 정리 2개 (`chore/m1-followups`, `chore/m3-followups` PR #29).
- **진행**: S2 `feat/input-and-opening` (worktree `The-World-That-Day-m2b`, M2b 고쳐 쓰기), S3a `feat/enwiki-events` (worktree `The-World-That-Day-c2`). C2 계획 `docs/plans/c2-ca-place-resolve.md`(초안)와 C3 계획 `docs/plans/c3-then-vs-now.md`(작성 중)는 다른 planner가 쓴다.
- **막힘**: S4 캐나다 YouTube 채널 (PM 명령 #5~#8), `feat/ca-place-search` 지점 검색 (Nominatim `부분 확인`).
- **운영 방식 (PM 2026-11-04)**:
  - 배포 직전까지 PR마다 멈추지 않는다.
  - 앞 PR이 머지되지 않았으면 다음 브랜치를 그 위에 쌓는다(아래 "진행 순서").
  - 유료 API 호출과 배포는 하지 않는다.
  - 키나 PM 명령이 필요한 일은 마지막 보고의 "배포 전 확인 목록"(아래)에 모은다.
  - 막히면 가장 타당한 안(리서치·계획 문서의 추천안)으로 진행하고 기록한다. 다만 "PM만 정할 수 있는 결정"(아래 목록)은 위임으로 정하지 않는다.

| 마일스톤 | 목표 | 기능 (브랜치 단위) | 선행 조건 | 끝나면 PM이 정할 것 |
|---|---|---|---|---|
| C0 캐나다 리서치 **(완료)** | "그때 vs 지금" 11개 항목과 영역 8개 출처 검증, 판정표, 결정 목록 | `docs/canada-research` (PR #30) | 결정 1·7 | (반영됨: A1·A2·T1 D1~D5·T8 D4·T6 D4, 2026-11-04 위임) |
| C1 나라 전환 | 화면은 en-CA, 입력·오프닝은 캐나다판, 사건은 영어 위키백과 문서 3개, 미디어와 범위는 캐나다, 한국 전용 코드 정리 | S1 `feat/english-ui` **(완료, PR #31)** → S2 `feat/input-and-opening` **(진행)** ∥ S3a `feat/enwiki-events` **(진행)** → S3b `feat/enwiki-canada-events` → S3c `feat/enwiki-media-scope` → S4 `feat/ca-youtube-channels` (임시, 막힘) | 정의서 1차 수정(S2 T9). S4는 PM 명령 #5~#8 + T4 D4~D7 | S4 허용 채널, ★ S3b T2 담당 |
| C2 장소 해석 | 캐나다 장소 → 유형·좌표·주·CSD, 같은 이름 후보 고르기, 다시 입력 안내, 캐나다 밖 안내 | `feat/ca-place-data` → `feat/ca-place-matcher` → `feat/ca-place-resolve` (서버, C2-P1로 셋으로 나눔) → `feat/place-picker` (웹, M2b2 고쳐 씀) · `feat/ca-place-search` (`SPOT`·국외 판정, 막힘) | T1 D1~D5 (a). 웹은 S2(쌓기 가능). 검색은 Nominatim `확인됨` | Nominatim 예외 허용, 지도에서 위치 고르기, 동네 목록 큐레이션(T1 D3 (c)) |
| C3 그때 vs 지금 공용 | 수치 레코드 형식·검사 규칙, "그때와 지금" 섹션 뼈대와 수치 카드, 정직한 수치 표기(결정 6 (a)) | 서버 `feat/then-now-format` · 웹 `feat/then-now-section` (둘 다 임시, `c3-then-vs-now.md` 에서 확정) | 결정 6 (a), T5 D6·D7, 정의서 2차 6.4절 | 섹션 위치(정의서 5절), "그해 자료 없음" 문구 |
| C4 날짜만 필요한 항목 | 1차: 항목 2의 $100 환산. 2차: 7 금리·환율, 9 스탠리컵·주노상·영화, 5 같은 생일, 4 노래 | 1차 `feat/then-now-cpi` → 2차 항목마다 1 PR (임시) | C3. 2차는 항목별 결정·명령(아래 "MVP 부분집합") | 2차 결정 (T5 D1, T7 D3·D7 등) |
| C5 주·도시가 필요한 항목 | 1차: 1 날씨, 2 휘발유·빵·우유, 3 최저임금 시간, 6 아기 이름, 11 도시 인구. 2차: 10 집값 | `feat/ca-region-codes` (`data/`) → 항목마다 1 PR (임시) | C2 사전(`province`·`csd`), C3 | 1차 결과 검수, T6 D2 |
| C6 동네 그때/지금 | 지원 도시의 항공사진 그때/지금, 현재 위성 | `feat/ca-aerial-then` → `feat/ca-aerial-now` → `feat/montreal-aerial-curation` (임시) | C2, T2 D1~D6, X1·X2·X4, PM 브라우저 명령 #26~#28 | MVP에 넣을지 |
| C7 도시의 대표 풍경 | 데모 도시 4곳의 대표 명소와 기록 미디어 | `feat/ca-landmark-data` → `feat/ca-landmark-media` → `feat/ca-city-section` (임시) | C2, T4 D1~D3, X1 | MVP에 넣을지 |
| C8 이야기 화면 완성 | 흐름, 성능, 전체 출처 목록, 장애 격리, 데모 4개 E2E | `feat/story-flow` → `feat/sources-and-resilience` → `chore/demo-e2e` | C1~C5 (C6·C7은 머지된 만큼) | 공개 배포(#5), MVP 완료 선언 |

PR 수는 약 32개로 본다(C1 6, C2 5, C3 2, C4 5, C5 7, C6 3, C7 3, C8 3). 기능 계획에서 400줄 규칙으로 늘거나 준다.

## 지금 적용되는 PM 결정 (캐나다)

| 언제 | 결정 | 내용 |
|---|---|---|
| 2026-10-29 | 방향 전환 | 주 대상 캐나다, 화면은 영어만, 세계 사건은 영어 위키백과로, "그때 vs 지금" 11개 항목, 연·월 값은 "그해" 문구 |
| 2026-10-29 | 결정 1 (b) | 데모 4개(아래 "데모 입력과 표본 날짜"), 같은 이름 확인용 장소 이름 "London"·"Victoria"·"Saint John" 예외 |
| 2026-10-29 | 결정 2 (a) | "오늘"은 `America/St_Johns`, 서버·웹이 같은 상수 |
| 2026-10-29 / 2026-11-04 | 결정 3 → **A2** | 지원 시작 **1950-01-01 확정** |
| 2026-10-29 | 결정 4 (b) | 한국 전용 코드는 캐나다 PR 안에서 그 자리에서 바꾸거나 지운다 |
| 2026-10-29 | 결정 5 (b) | 정의서·`CLAUDE.md` 수정은 두 번에 나눈다 → `brief-changes-canada.md` |
| 2026-10-29 | 결정 6 (a) | 수치의 정직한 표기(기간·정밀도, "지금" 기간, approx., 인구조사 연도 차이) |
| 2026-10-29 | 결정 7 (a) | 같은 생일: 실행 때 Wikidata, 저장소에는 합성 인물만 |
| 2026-10-29 | 결정 8 | M2b 고쳐 씀(S2), M2a는 쓸 만한 부분만 새 브랜치로(C2) |
| 2026-10-29 | 결정 9 (a) | en-CA (neighbourhood, licence, "3:20 p.m.") |
| 2026-10-29 | 결정 10 (b) | `StoryDate` 는 backend-dev (S1 T5에서 끝남) |
| 2026-10-29 | S1-P1~P4, S2-P1~P2, S3 D1~D9 | 각 계획의 추천안. ★ S3b T2는 (a) PM 직접으로 기록하고 S3b 전에 다시 확인 |
| 2026-11-04 | **A1 (a)** | MVP 1차 항목 1·2·3·6·11. 4·5·7·9·10은 결정이나 PM 명령 뒤 2차. 8은 뺀다 |
| 2026-11-04 | **T1 D1~D5 (a)** | CGNDB는 내려받기 파일만. Nominatim은 저장소 밖 임시 캐시만. 동네 이름은 SGC·CGNDB·OSM, 경계는 나중. 같은 이름 외국 도시는 캐나다 후보만. 병원은 최선의 검색 + 다시 입력 안내. Nominatim이 `부분 확인` 이라 `SPOT` 검색은 확인 전까지 만들지 않음 |
| 2026-11-04 | **T8 D4 (b)** | 나이 문장 "This service is for a general audience. If you are under 13 (14 in Quebec), please ask a parent or guardian first." 을 S2 T8b에 |
| 2026-11-04 | **T6 D4 (a)** | ECCC 일별 기후 라이선스를 서비스 단위로 읽는다(해석 확정 아님) |
| 2026-11-04 | 운영 방식 | 위 "현재 위치" |

## MVP 부분집합 (A1 (a))

MVP = C1 + C2(`REGION`·`NEIGHBORHOOD`, `SPOT` 은 Nominatim 확인 뒤) + C3 + 아래 1차 항목 5개 + C8. C6·C7은 막히지 않은 만큼만 들어가고, 빠진 섹션은 정직한 빈 안내로 둔다(넣을지는 C5 끝에 PM이 정함).

| 항목 | 묶음 | 단계 | 1순위 출처 (확신도) | 표기 | 가장 이른 기간 | 시작에 필요한 것 |
|---|---|---|---|---|---|---|
| 1 생일 날씨 | 1차 | C5 | ECCC MSC GeoMet `climate-daily`·`climate-stations` (확인됨, T6 D4 (a)) | That day (기후일, 관측소 이름·거리) | 1950 | C2 도시 대표 좌표(CGNDB), 관측소 사전, T6 D3 "지금" 정의 |
| 2 $100 그때→지금 + 장바구니 | 1차 | C4 (CPI 환산) + C5 (휘발유, 빵·우유) | StatCan CPI 41690973, 휘발유 18-10-0001-01, 빵·우유 18-10-0002-01·18-10-0245-01 (확인됨) | That month, 환산은 approx. | CPI 1914-01, 휘발유 1979-01, 빵·우유 1995-01 | 휘발유는 도시 대응표, 빵·우유 이어 붙임은 T5 D2. 영화표·우표는 뺀다(T5 D3 (a)) |
| 3 최저임금으로 일한 시간 | 1차 | C5 | ESDC 최저임금 CSV + 항목 2 가격 (확인됨) | 그날 시행 요율 + That month, approx. | 임금 1965-01-01, 시간 계산은 가격 계열 시작부터 | 주(C2), T5 D7 |
| 6 주별 인기 아기 이름 | 1차 | C5 | BC·온타리오·퀘벡 주 데이터 (확인됨), 앨버타 (부분 확인) | That year (주별 정의 주석, 주 사이 비교 없음) | BC 1926, 온타리오 1913/1917, 퀘벡 1980 | 주(C2), T7 D5. 앨버타는 명령 #48 뒤 |
| 11 도시 인구 | 1차 | C5 | StatCan 17-10-0155-01 (CSD, 2001~) + CMA 표 17-10-0033-01·17-10-0037-01 (확인됨) | That year (7월 1일 추계, 경계 판 병기) | CSD 2001, CMA 1976 | CSD 코드(C2), T6 D1, T6 D6 |
| 7 주택담보대출 금리 + CAD/USD | 2차 | C4 | BoC Valet V80691335 (확인됨), FXUSDCAD·StatCan 보관 표 (부분 확인) | 금리 That week (수요일), 환율 Same day (영업일) | 금리 1975, 환율 1950-10-02 | 금리는 T5 D5만. **환율은 T5 D1 (PM만)** |
| 9 스탠리컵 + 주노상 + 영화 | 2차 | C4 | 스탠리컵 Wikidata (확인됨), 주노상·영화 (부분 확인) | That year | 1950 / 1970 / 1949 | 스탠리컵은 결정 없이 가능. 주노상 T7 D8 + 명령 #49. 영화 T7 D7 |
| 5 같은 생일 | 2차 | C4 | Wikidata P569, 366일 사전 계산 (확인됨, 조건) | Same day (월·일) | 제한 없음 | **T7 D3 생존 인물 노출 (PM만)**, T7 D4 |
| 4 그 주 1위 노래 + 재생 | 2차 | C4 | 영어 위키백과 1위 목록 큐레이션 + YouTube 공식 임베드 (부분 확인) | Chart dated <발행일> | 1957 | T7 D1·D2·D10, X3, 명령 #9 |
| 10 집값 | 2차 | C5 | StatCan NHPI 18-10-0205-01 (확인됨, 지수) | That month, "new-house price index" | 1981-01 | T6 D2. "소득 몇 년"은 뺀다 |
| 8 발명·기기 | **뺌** | — | 자동 출처 없음 | — | — | T7 D6 (a) |

- 2차의 순서는 결정이 필요 없는 부분부터다: 금리 → 스탠리컵 → (결정 뒤) 같은 생일·노래·환율·주노상·영화 → 집값.
- 비어 있는 해는 항목마다 "그해 자료 없음"을 보이고, 다른 날짜·해·지역으로 채우지 않는다(메모 "No substitute dates"). 규칙으로 정한 예외(가장 가까운 인구조사)만 연도를 밝혀 쓴다.

## 마일스톤 상세

### C0 — 완료 (PR #30)
`docs/research/canada-*.md` 8개와 `canada-summary.md`, 판정표, 결정 53개, PM 명령 50개. `docs/plans/canada-research.md`.

### C1 나라 전환 — PR 6개

| 기능 | 브랜치 | 담당 | 상태 | 계획서 | 메모 |
|---|---|---|---|---|---|
| S1 영어 화면 | `feat/english-ui` | frontend-dev, backend-dev, test-engineer, PM | **완료 (PR #31, `main` cc9af18)** | `docs/plans/c1-english-ui.md` | `StoryDate` 하한 1950-01-01·St. John's "오늘"(S1-P3 (b)), 방침 보강 S-0·S-2~S-6·S-8·S-10·S-12, 방침 시행일 November 4, 2026 |
| S2 입력·오프닝 캐나다판 (M2b 고쳐 쓰기) | `feat/input-and-opening` (worktree `The-World-That-Day-m2b`) | frontend-dev, test-engineer, PM(T7·T9) | **진행** (T0 `main` merge → T8a → T8b → T6) | `docs/plans/c1-input-and-opening-ca.md` (그 브랜치) | 장소는 서버로 보내지 않는다. T8b에 나이 문장 S-7(T8 D4 (b)). 크기 약 435~470(끝난 M2b 406 포함, 리서치 계획에서 받아들임). PM T9 = 정의서 1차(`brief-changes-canada.md` 1차) |
| S3a 날짜·연도 문서를 영어로 | `feat/enwiki-events` (worktree `The-World-That-Day-c2`) | backend-dev, content-curator, frontend-dev, test-engineer, PM(T8) | **진행** (T2~T4 한 작업, 병합 규칙 D3m) | `docs/plans/c2-english-events.md` (그 브랜치) | 응답 필드 `date_kind`·`range_end`, 화면 "Started that day". 그날 사진·영상은 S3c까지 끔(D7 (a)) |
| S3b "YYYY in Canada" 더하기 | `feat/enwiki-canada-events` | backend-dev, content-curator, test-engineer, PM | 계획됨 (S3a 위에 쌓기) | 같음 | ★ T2 캐시 V4·엔티티 — PM 직접 (a)로 기록, 시작 전에 다시 확인 |
| S3c 미디어 캐나다로, 한국 코드 정리 | `feat/enwiki-media-scope` | backend-dev, content-curator, frontend-dev, test-engineer, PM | 계획됨 | 같음 | Commons `Canada photographs taken on`, `country:CA`, YouTube 지역 CA, KTV 채널 뺌 → 허용 채널이 비면 YouTube 꺼짐, `excluded-credits` 에 The Canadian Press, `PD US no notice` 뺌(S3 D3 (a)) |
| S4 캐나다 YouTube 허용 채널 | `feat/ca-youtube-channels` (임시) | data-researcher(확신도 갱신), backend-dev | **막힘** — PM 명령 #5~#8(T4 C1~C4), 브라우저 명령 #36(T4 B5) | 미작성 | 추천 구성 Radio-Canada Archives + Olympic Games(올림픽 사건일만, T4 D4 (a)). 캐나다 밖 재생 제한 영상은 그 항목만 숨김(T4 D7 (a)). 그 전까지 사건 영상은 없다(정상) |

### C2 장소 해석 — PR 5개

계획: `docs/plans/c2-ca-place-resolve.md` (다른 planner, 초안). 아래는 로드맵 수준의 요약이다. 세부는 그 계획을 따른다.
- 서버 셋(C2-P1, 크기 때문에 나눔): `feat/ca-place-data`(사전 빌더와 파생 데이터, SGC 2021 + CGNDB 내려받기 파일만 — T1 D1 (a), 출처 문구) → `feat/ca-place-matcher`(사전 읽기와 매칭, HTTP 없음) → `feat/ca-place-resolve`(API 계약 `GET /api/places/resolve`).
- 계약: M2a 뼈대를 `feat/place-resolve` 에서 읽어 새 커밋으로 쓴다(머지하지 않음, 결정 8-2 (a)). `province`·`csd`, `status` `FOUND`·`PARTIAL`·`NOT_FOUND`·`OUTSIDE_CANADA`(자리만), `id` 접두어 `province-`·`csd-`·`cgndb-`. 같은 이름 외국 도시는 캐나다 후보만, 주 이름을 붙임("London, Ontario", T1 D4 (a)). 사전에 없거나 일부만 맞으면 다시 입력 안내(T1 D5 (a)).
- `feat/place-picker` (frontend-dev): M2b2 계획(`m2b2-place-picker.md`, `feat/input-and-opening` 에 있음)을 캐나다 계약으로 고쳐 쓴다. 후보 고르기, 다시 입력 안내. 장소가 처음 서버로 가므로 방침 S-1·입력 화면 요약 S-9를 이 PR에서 고친다(S2 계획 "개인정보"). 선행: S2와 `feat/ca-place-resolve`(쌓기 가능, 그 전에는 계약대로 MSW).
- `feat/ca-place-search` (backend-dev): `SPOT` 과 캐나다 밖 판정(Nominatim `address.country_code`), `osm-` 접두어. 초당 1회 제한기, 저장소 밖 임시 캐시와 OpenStreetMap 표시(T1 D2 (a)). **Nominatim이 `확인됨` 이 되기 전에는 시작하지 않는다**(`CLAUDE.md` 7절 2항). 그 전에는 데모 "Vancouver General Hospital"이 다시 입력 안내로 끝나고, 캐나다 밖 입력은 "찾지 못함"과 구별되지 않는다.
- 장소 해석 요청에는 날짜를 넣지 않는다. 날짜는 `GET /api/story/world` 에만 간다. 생년월일과 장소가 한 로그 줄에 남지 않음을 시험으로 확인한다(`CLAUDE.md` 7절 6항).

### C3 그때 vs 지금 공용 — PR 2개

계획: `docs/plans/c3-then-vs-now.md` (다른 planner, 작성 중). 로드맵이 기대하는 범위:
- 서버: 수치 레코드 형식(`value`, `unit`, `period`, `period_precision`, `geography`, `boundary_vintage`, `series_id`, `source_name`, `source_url`, `license`, `attribution`, `latest_period`, `is_preliminary`, `retrieved_at` — T5·T6·T7의 4절 매핑), `data/schema/` 의 스키마와 검사 규칙, 예시(M3a3 공용 미디어 형식과 같은 방식). 과거 값은 `data/` 에 고정하고 "지금" 값만 호출·캐시(T5 D6 (a)). 계산 값은 approx.와 계산식 링크(T5 D7 (a)).
- 웹: "그때와 지금" 섹션 뼈대와 공용 수치 카드(기간 배지 "That day / That week / That month / That year / 1986 census (nearest to 1988)", "2025, latest", 출처·라이선스 줄, 비어 있음 상태). 항목 PR이 카드를 새로 만들지 않게 한다.
- 이 단계에서 섹션 API의 모양(우리 API가 날짜와 장소 코드를 함께 받는 첫 엔드포인트)과 로그 규칙 시험을 정한다. 외부 출처에는 필요한 것만 보낸다: 날씨는 관측소 ID와 날짜, 연도만 필요한 항목(인구, 아기 이름)은 연도만(T9 7절).
- 정의서 2차의 6.4절(결정 6 (a))을 PM이 넣기 전이면 계획이 결정 6 (a)를 근거로 쓴다.

### C4 날짜만 필요한 항목 — PR 약 5개
위 "MVP 부분집합" 표. 1차는 CPI 환산 하나(장소 없이 됨). 같은 생일은 366일 사전 계산 작업이 앞선다(요청 경로에 Wikidata를 두지 않음). 노래 영상은 사건 영상과 YouTube 검색 버킷(하루 100회)을 나눠 쓴다(X3).

### C5 주·도시가 필요한 항목 — PR 약 7개
- 먼저 `feat/ca-region-codes` (content-curator): 장소 사전의 `csd`·`province` → 휘발유 도시 목록, CMA, 인구 벡터, 최저임금 관할, 아기 이름 주의 대응표(T9 7절 C5 — 영역 문서에 이 코드들을 자동으로 잇는 근거가 없다).
- 이어서 날씨(관측소 사전 포함) → 최저임금 시간 + 휘발유·빵·우유 → 아기 이름 → 인구 → (2차) 집값.
- 데모마다 빈 칸은 계열 시작 연도 그대로다. 1976 데모는 휘발유(1979~)·빵·우유(1995~)·집값(1981~)이 비고, 최저임금 시간은 임금만 나온다. 1980 데모는 빵·우유·집값이 빈다. 이것이 정상이다.

### C6 동네 그때/지금 — PR 3개 (MVP 포함 여부는 PM)
- 확인된 "그때" 출처는 NRCan NAPL 시계열(밴쿠버권 등 8곳)과 토론토 타일, 몽레알 기록보관소(큐레이션)다. "지금" 쪽 Esri는 `미확인`(T2 B4, 명령 #28)이라 `feat/ca-aerial-now` 는 그 확인 뒤다. 지도 타일을 그리는 웹 도구는 C6 계획에서 정한다(T9 8절 6항).
- 데모로 보이는 것이 거의 없다: Calgary·St. John's는 `REGION` 이라 동네 섹션이 없고, Hochelaga-Maisonneuve는 몽레알 큐레이션 사본(결정 #13, PM이 받음)과 X2가 있어야 하며, Vancouver General Hospital은 NAPL 1982가 ±5년 밖이라 T2 D3 (a)면 빈다. 그래서 C5 뒤로 두고, 시작 전에 PM이 MVP 포함을 정한다.

### C7 도시의 대표 풍경 — PR 3개 (MVP 포함 여부는 PM)
- content-curator가 데모 도시 4곳의 대표 명소와 존재 기간(근거 링크)을 만든다. 미디어는 Commons 파일별 라이선스, 공식 채널 영상(S4 뒤).
- 허락을 요구하는 기관 사진(PAA, LAC)과 "All rights reserved" 자료를 어떻게 다룰지는 X1(PM 미디어 결정의 적용 범위 해석)이라 PM만 정한다. 명소 데이터(미디어 없음)는 그 전에 시작할 수 있다.

### C8 이야기 화면 완성 — PR 3개
- `feat/story-flow`: 흐름, 지연 로딩, 빈 섹션 문구, "다시 해보기".
- `feat/sources-and-resilience`: 전체 출처 목록(CC BY-SA 4.0 위키백과, OGL-Canada·StatCan·ECCC 문구, Commons 작가·라이선스, OSM/ODbL, YouTube), 방침 출처 목록 갱신(T8 S-11), 출처 하나를 끊었을 때 섹션만 비는지, YouTube 쿼터 소진, 3초·10초 측정, 한국판 M3a1c 리뷰 권장 2·5(인터럽트와 캐시 쓰기, 긴 GET 414).
- `chore/demo-e2e`: 데모 4개 + 표본 1969-07-20 E2E(목 서버, 플레이어 스텁, 기존 Playwright·`web-ci`). 배포는 하지 않는다.

## 진행 순서

- **서버 줄**: S3a → S3b → S3c → `feat/ca-place-data` → `feat/ca-place-matcher` → `feat/ca-place-resolve` → `feat/then-now-format` → `feat/then-now-cpi` → `feat/ca-region-codes` → C5 항목들 → C4 2차 → (S4·`feat/ca-place-search` 는 풀리는 대로 끼움)
- **웹 줄**: S2 → `feat/place-picker` → `feat/then-now-section` → 항목 PR의 웹 몫 → C8
- 두 줄은 서로 다른 worktree에서 병렬로 간다(PM이 고른 서버·웹 병렬 방식). 같은 줄 안에서는 차례로 한다. `feat/ca-place-data` 는 `data/`·빌더만 바꾸므로 S3 줄과 겹치지 않으면 따로 worktree에서 먼저 할 수 있다.
- **쌓기 규칙** (앞 PR이 머지되지 않았을 때):
  1. 다음 브랜치는 앞 브랜치의 끝에서 만든다. 기능 계획 머리에 "쌓은 위치: `<앞 브랜치>` `<해시>`"를 적는다.
  2. PR 대상은 `main` 으로 두고, 설명 첫 줄에 "Stacked on #N — merge #N first; this PR's own commits start at `<해시>`"를 쓴다. 머지는 쌓인 순서대로 PM이 한다.
  3. 앞 PR에 고침 커밋이 생기면 오케스트레이터가 앞 브랜치를 뒤 브랜치에 merge 커밋으로 넣는다. rebase·amend는 하지 않는다(`CLAUDE.md` 4·5절).
  4. 400줄 규칙과 확인점은 `<앞 브랜치>...HEAD` 로 잰다.
  5. 쌓기는 줄마다 3단 정도에서 멈추고 오케스트레이터가 마지막 보고 때 머지를 요청한다(충돌과 리뷰 범위가 커진다).

## 멈춘 한국 작업

| 기능 | 브랜치 | 상태 | 처리 |
|---|---|---|---|
| M2a 장소 해석 (법정동 사전, `REGION`) | `feat/place-resolve` (T2~T4 커밋 있음) | 멈춤 (2026-10-29) | C2 서버 셋이 쓸 만한 코드(`PlaceQuery`, 계약 뼈대 등)를 `git show` 로 읽어 새 커밋으로 쓴다(결정 8-2 (a), 머지하지 않음). 브랜치는 그대로 둔다 |
| M2a2 카카오 로컬 | `feat/place-kakao` (브랜치 없음, 계획서만) | 멈춤 | 다시 열지 않는다. 계획서 `m2a2-place-kakao.md` 는 `feat/input-and-opening` 에 있다 |
| M2b 입력·오프닝 | `feat/input-and-opening` | **S2로 고쳐 씀 (진행)** | 결정 8-1 (a) |
| M2b2 후보 선택 화면 | `feat/place-picker` (브랜치 없음, 계획서만) | C2 웹으로 고쳐 씀 | 같은 브랜치 이름으로 C2에서 연다 |
| M3a5b 대한뉴스 영상 | `feat/daehan-news-video` | **닫음 제안** | PR을 열지 않는다. 브랜치는 지우지 않는다(`CLAUDE.md` 5절). 진행 표에 "닫음" 한 줄. PM의 대한뉴스 작업(공공데이터포털 신청, N1·N2, CSV)은 더 하지 않는다 |

- 머지된 한국 전용 코드(kowiki 어댑터 규칙, 한국어 키워드·목록, KTV 채널, 한국 데모 큐레이션)는 S3a~S3c가 결정 4 (b)대로 바꾸거나 지운다.
- 한국판의 남은 PM 확인(M3b2 T6, M3b3c T6, Y3 등)은 더 하지 않는다.
- M3는 계획 23개 중 22개를 머지하고 M3a5b를 닫아 끝난 것으로 본다.

## 데모 입력과 표본 날짜

결정 1 (b). 실제 장소와 공개된 캐나다 역사 사건의 날짜다(실제 사람의 생일이 아님, M3-P13). 정의서 12절 반영은 `brief-changes-canada.md` 1차.

| 입력 | 확인할 것 | 정직하게 비는 곳 (리서치 기준) |
|---|---|---|
| 1988-02-13 + "Calgary" | `REGION`, 산악 시간대, 캘거리 동계 올림픽 개막, 1차 항목 5개 모두 | 동네 섹션 없음(`REGION`) |
| 1976-07-17 + "Hochelaga-Maisonneuve, Montréal" | `NEIGHBORHOOD`, 악센트, 몽레알 하계 올림픽 개막 | 휘발유·빵·우유·집값(계열 시작 전), 동네 "그때"는 몽레알 큐레이션 전까지 |
| 2010-02-12 + "Vancouver General Hospital" | `SPOT`, 태평양 시간대, 밴쿠버 동계 올림픽 개막 | `feat/ca-place-search` 전에는 다시 입력 안내, 동네 "그때"(±5년 밖) |
| 1980-04-12 + "St. John's" | `REGION`, 뉴펀들랜드 시간대, 테리 폭스 "희망의 마라톤" 출발 | 빵·우유·집값(계열 시작 전), 동네 섹션 없음(`REGION`) |
| 장소 이름만: "London", "Victoria", "Saint John" | 같은 이름 후보 목록, "London, Ontario" 표기 | 날짜와 짝짓지 않는다 |
| 표본 1969-07-20 | 영상 표본(아폴로 11호), S3 D9 | 캐나다 관련 사건이 없어도 정상 |
| 경계 1950-01-01, 1949-12-31, 2000-02-29 | 하한·윤일 | — |

## 배포 전 확인 목록

마지막 보고(배포 직전)에 이 목록을 그대로 쓴다. 진행하면서 오케스트레이터가 줄을 지우거나 더한다.

### 1. 공개 배포 결정 (#5 — 지금은 로컬 데모만, 비상업)
배포를 정할 때 함께 다시 볼 것:
- 미디어 결정과 안전장치(2026-10-11), 결정 #13-C, 출처별 차단 목록 운영.
- T8 D6 목록: 접속 로그·IP, HTTPS, 수익, 분석 도구, 서버 저장·계정, 퀘벡 Law 25 적용 판단과 변호사 검토, 책임자 연락처, 프랑스어 방침(T8 D5), Bill C-36, 퀘벡 밖 전달 평가.
- 출처 조건: Nominatim 공개 서버 사용과 ODbL 파생 데이터베이스(T1 D2), BoC 환율 재배포(T5 D1), 위키백과 표를 `data/` 에 둔 동일조건(T7 D9), 몽레알 CC BY-NC-SA(비상업), 캘거리 "All rights reserved"(T2 D1), YouTube 쿼터 증설·감사.
- 방침 문장은 법률 검토 전 초안이다(`canada-privacy.md`, 법률 자문 아님).

### 2. PM이 발급하거나 확인할 키 (`.env`)
- 1차 MVP에는 새 키가 없다(`canada-summary.md` 3절). `WIKIMEDIA_USER_AGENT` 값과 이미 발급한 `YOUTUBE_API_KEY` 를 `.env` 에서 확인한다.
- 선택(정하면): `GOOGLE_MAPS_SERVER_KEY`(X4 (b)·(c)일 때만, 결제 계정 필요 — 유료 위험), `MAPILLARY_ACCESS_TOKEN`(명령 #4), `ARCGIS_API_KEY` 또는 `MAPBOX_ACCESS_TOKEN`(C6 "지금", T2 D4 — 브라우저 키인지 미확인, 그러면 한국판 #8 예외).
- 한국판 키(카카오, `DATA_GO_KR_SERVICE_KEY`)는 더 쓰지 않는다. `.env` 에서 정리는 PM이 한다.

### 3. PM 명령 (`canada-summary.md` 4절, 번호 그대로)

| 풀리는 것 | 명령 | 종류 |
|---|---|---|
| S4 사건 영상 캐나다 채널 | #5~#8 (T4 C1~C4, 검색 9회), 브라우저 #36 (T4 B5), #32 (T4 B1) | 키 `YOUTUBE_API_KEY` |
| 항목 4 노래 | #9 (T7 Y1, 검색 4회 — #5~#8과 같은 날 가능), 브라우저 #46 (T7 B1), #47 (T7 B2) | 키 |
| `SPOT` 검색 | Nominatim 등급 올리기: 병원 커버리지와 ODbL 칸. data-researcher가 공개 엔드포인트로 다시 보고, PM이 T1 D2 (a)를 그 근거로 받아들이는 확인. 또는 PM이 `CLAUDE.md` 7절 2항 예외로 허용 | PM 확인 |
| 항목 6 앨버타 | 브라우저 #48 (T7 B3) | 브라우저 |
| 항목 9 주노상 | 브라우저 #49 (T7 B4) | 브라우저 |
| 항목 1·3 (권장, 이미 진행 가능) | 브라우저 #40 (T6 B1 — T6 D4 (a)의 근거 확인), 선택 명령 #15 (T5 W5, 최저임금 원문) | 브라우저 / 키 없음 |
| 항목 10·11 (선택) | 브라우저 #41~#45 (T6 B2~B6) | 브라우저 |
| C2 장소 (선택) | 브라우저 #23 (T1 B5), #24 (T1 B6), 경계 #20~#22 (T1 B2~B4) | 브라우저 |
| C6 동네 | 브라우저 #25~#31 (T2 B1~B8), Google·Mapillary를 쓸 때만 #1~#4 | 브라우저 / 키 |
| C7 도시 | 브라우저 #33~#39 (T4 B2~B8) | 브라우저 |
| 개인정보 | 브라우저 #50 (T8 B1, 퀘벡 조문) | 브라우저 |
| 값 원문 재확인 (선택) | #10~#18 | 키 없음 |

필수 명령은 없다. 1차 MVP 5개 항목의 출처는 명령 없이 `확인됨` 이다(4.5절).

### 4. PM만 고칠 문서
- `docs/plans/brief-changes-canada.md` **1차**(정의서 1·2·3·4·5·6.1·6.2 글자·8·9·12·13절, `CLAUDE.md` 0절·7절 1·5·6항, `.claude/` 정의 1차) — S2 T9의 완료 기준. S2·S3a 머지 전.
- 같은 문서 **2차**(그때 vs 지금 섹션, 6.4절, 라이선스 예, 출처 표, 수치 원칙, `.claude/` 2차) — C3 계획 전이 좋고 늦어도 배포 전.

### 5. 개인정보 (T8)
- **D2 운영자 소재지와 법인 여부**: PM이 알려 준다(개인/단체, 한국/캐나다 어느 주). 방침 S-6 책임자 문장과 주 법 적용 판단이 달라진다.
- **D6 공개 배포 때 다시 볼 목록**: 위 1번에 넣었다.
- 장소가 처음 서버로 가는 PR(`feat/place-picker`)과, 장소에서 고른 관측소 ID가 날짜와 함께 ECCC로 가는 PR(C5 날씨)은 그 PR 안에서 방침 S-1·S-9를 고친다. 방침이 바뀐 PR은 머지하는 날 시행일 한 줄을 고친다(S-12).

### 6. PM만 정할 수 있는 결정 (위임으로 정하지 않음)

| 결정 | 이유 | 막는 것 |
|---|---|---|
| T7 D3 생존 인물 노출 범위 | 실제 인물의 개인정보, 결정 7 (c)를 다시 묻기로 함 | 항목 5 |
| T5 D1 BoC 환율 | 출처가 `부분 확인` 이라 `CLAUDE.md` 7절 2항 예외가 필요 | 항목 7의 환율 |
| X1 허락을 요구하는 자료 | PM 미디어 결정(2026-10-11)의 적용 범위 해석 | C6 캘거리, C7 기관 사진 |
| X4 Google 사용 | 결제 계정·유료 위험 | C6 거리 사진, 대안 장소 검색 |
| Nominatim 예외 허용 (확인 전 구현) | `CLAUDE.md` 7절 2항 예외 | `feat/ca-place-search` |
| T4 D1 NFB 임베드 | 약관 "개인적 사용만" 해석 | S4, C7 |
| T8 D2 운영자 정보 | PM만 아는 사실 | 방침 S-6 |
| C6·C7 MVP 포함 | 범위 결정 | C8 완료 조건 |

그 밖의 결정(T5 D2~D7, T6 D1·D2·D3·D5·D6·D7, T7 D1·D2·D4·D5·D7·D8·D10, T2 D3·D5·D6, T4 D4~D7, X2, X3, X5)은 문서 추천안으로 기능 계획에 적고 진행 기록에 남긴다. 약관 해석이 걸린 것(T6 D6, T7 D1)은 T6 D4와 같이 "해석 확정 아님, 배포 때 다시"로 적는다.

### 7. 미뤄 둔 PM 확인 (운영 방식 때문에 PR마다 하지 않은 것)
- S2 T7 화면 확인, S2 T9 정의서 1차, S3a T8, S3b·S3c의 PM 확인, 방침이 바뀐 PR의 시행일 커밋(머지하는 날), ★ S3b T2 담당.

## 순서를 이렇게 정한 이유

1. **C1을 먼저 끝낸다.** 화면과 사건 출처가 캐나다가 되어야 이후 기능을 모두 영어 데모로 확인할 수 있다. S1은 끝났고, S2와 S3a는 폴더가 겹치지 않아 병렬로 간다.
2. **C2 사전을 C3보다 앞에 둔다.** 1차 항목 다섯 가운데 1·3·6·11은 주나 CSD 코드를 받고, 2도 휘발유 부분은 도시를 받는다. 장소 없이 되는 것은 CPI 환산뿐이다(T9 1.2.1). 사전 PR은 지금 `확인됨` 출처만으로 되고, 웹 후보 화면이 같은 계약으로 바로 따라온다.
3. **C3 공용 형식을 항목보다 먼저 둔다.** 수치 레코드와 카드를 한 번 정하면 항목 PR이 작아지고 정직한 표기(결정 6 (a))가 한 곳에서 지켜진다(M3a3 공용 미디어 형식과 같은 이유).
4. **1차 항목 → 2차 항목.** 1차는 새 키·명령 없이 `확인됨` 이고(A1 (a)), 2차는 결정이나 명령을 기다린다. 2차 안에서도 결정이 필요 없는 금리·스탠리컵을 먼저 한다.
5. **C6·C7은 뒤로.** 출처 위험이 가장 크고(PM 브라우저 확인, X1·X4), 데모 4개에서 보일 것이 적다. C8은 C6·C7을 기다리지 않고 머지된 만큼으로 완성한다.

## 가장 큰 리스크 3가지와 대응

1. **막힌 출처와 결정이 MVP의 모습을 정한다.** S4(캐나다 사건 영상), `SPOT` 검색, 노래, 환율, C6·C7이 PM 명령이나 PM만의 결정을 기다린다. 자율 진행만으로 끝나면 데모는 "사건 + 도시 숫자" 중심이 되고, Vancouver General Hospital은 다시 입력 안내로 끝난다. 대응: 1차 5개는 명령 없이 `확인됨` 이다. 막힌 것은 "배포 전 확인 목록"에 풀리는 것과 함께 모은다. YouTube 명령 #5~#9는 하루에 몰아 검색 13회로 끝난다. 빈 섹션은 정직한 안내로 두고 다른 날짜로 채우지 않는다.
2. **쌓인 브랜치와 큰 PR.** 머지 없이 쌓이면 충돌이 커지고 리뷰 범위가 흐려진다. 서버 PR은 최근 실측이 파일별 셈의 ×2.0~×2.9였다(S2는 이미 약 435~470, C2는 크기 때문에 서버 PR 셋으로 나눔). 대응: 위 "쌓기 규칙"(merge 커밋만, `<앞 브랜치>...HEAD` 로 재기, 3단 정도에서 멈춤), 기능 계획마다 확인점, 목록 파일 커밋을 따로 두어 리뷰를 쉽게 한다(S3a처럼).
3. **수치와 개인정보의 정직성.** 같은 이름 다른 경계(몽레알 2002 통합, CSD 2021 경계), 가장 가까운 인구조사, 퀘벡·앨버타 CSD 추계의 제3자 제공(T6 D6), 월·연 값을 그날 값처럼 보일 위험. 그리고 장소가 처음 서버로 가고(C2), 우리 API가 날짜와 장소 코드를 함께 받는 첫 엔드포인트(C3)가 생긴다. 대응: C3 형식에 `period_precision`·`boundary_vintage`·`latest_period`·approx.를 필수로 두고 검사 규칙으로 막는다. 외부에는 관측소 ID + 날짜, 연도만 필요한 항목은 연도만 보낸다. 로그 규칙 시험은 3회 돌린다. 방침 S-1·S-9를 장소가 서버로 가는 PR에서 고친다.

## ★ PM 직접 구현 후보

- **S3b T2 캐시 V4와 엔티티** (Flyway 열 이름 바꾸기와 행 지우기, JPA `upsert`, "V3 → V4" Testcontainers 시험): 기록은 (a) PM 직접. 운영 방식(PR마다 멈추지 않음)으로는 PM이 맡으면 S3b가 기다린다. S3b 시작 전에 다시 확인한다.
- **C2 사전 매칭과 접기 규칙** (`feat/ca-place-matcher`, 순수 함수, 외부 호출 없음): C2 계획에서 묻는다.
- **C3 "지금" 값 캐시 전략** (과거 값 고정 + 최신 값 호출·캐시, `retrieved_at`, 출처별 TTL): C3 계획에서 묻는다.
- 한국판 후반과 캐나다 S2에서 PM은 ★를 backend-dev에게 맡겼다(속도 방침). PM이 따로 정하지 않으면 자율 진행 중에는 backend-dev가 맡는 것으로 기능 계획에 적고 확인 목록에 남긴다.
