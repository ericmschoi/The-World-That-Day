# 서버 부트스트랩 (M1a)

> 브랜치: `chore/bootstrap-server` · 작성: planner · 상태: PM 승인 (2026-10-10)
> 기준: `CLAUDE.md` 0절·7절, `docs/product-brief.md` 8절·12절, `docs/roadmap.md` M1, `docs/research/summary.md` 3절·6절·8절
> 짝 계획서: `docs/plans/m1b-bootstrap-web.md` (`chore/bootstrap-web`). 두 계획서의 "M1a·M1b 경계" 절은 같은 내용이다.

## 목표

`server/` 에 이후 모든 백엔드 기능이 붙을 골격을 만든다. 끝나면 다음이 된다.

1. `cd server && ./gradlew check` 한 번으로 테스트(Testcontainers PostgreSQL 포함)와 포맷 검사가 돈다.
2. `docker compose up -d db` 로 로컬 PostgreSQL을 띄우고 `./gradlew bootRun` 으로 서버를 띄우면 `GET /api/health` 가 200이다.
3. Flyway가 기준 마이그레이션(V1)을 적용하고, JPA는 스키마를 만들지 않고 검증만 한다.
4. PR마다 GitHub Actions `server-ci` 가 테스트와 포맷 검사를 돌린다. 배포 잡은 없다(#5).
5. `.env.example` 에 M1에 필요한 환경변수 이름만 있다.

## 범위 밖

- 도메인 기능, 도메인 테이블, 엔티티 (첫 도메인 마이그레이션은 V2부터, M3a 또는 M2a)
- 외부 호출 클라이언트와 공통 설정(타임아웃, User-Agent, 재시도), WireMock 의존성 → 첫 외부 어댑터인 M3a에서 만든다 (아래 "summary 8절 처리")
- 캐시 테이블, 큐레이션 미디어 사본 제공(`/media/**`) → M3a 또는 M4c·M5b
- 에러 응답 형식(ProblemDetail), API 버전 규칙 → 첫 도메인 API(M3a 또는 M2a) 계획서에서 정한다
- CORS 설정 (개발 중에는 M1b의 Vite 프록시를 쓰므로 필요 없다)
- 정적 분석 도구(Checkstyle, Error Prone, SpotBugs), 테스트 커버리지 도구 → 필요하면 "다음 단계 제안"
- 배포 설정, Dockerfile(서버 이미지), 배포 잡 (#5 (a))
- 카카오·공공데이터포털·Google·네이버 키 이름 (`.env.example` 에는 처음 쓰는 마일스톤에서 추가한다 — 아래 "환경변수")
- `web/`, 루트 `.gitignore`, `README.md`, `CLAUDE.md`, `.claude/` 수정

## 사용할 외부 출처

없음. 이 기능은 외부 API를 호출하지 않는다.
- 개발 도구로 내려받는 것(Gradle 배포판, Maven Central 의존성, Docker Hub `postgres` 이미지)은 외부 데이터 출처가 아니므로 `docs/research/` 확인 대상이 아니다.
- `.env.example` 에 넣는 `WIKIMEDIA_USER_AGENT` 는 이름만 넣고 M1a 코드는 읽지 않는다. 사용처는 M3a(한국어 위키백과·Wikidata·Commons, 모두 `확인됨` — [day-events.md 3절](../research/day-events.md#3-출처-카드)).

## 전제 (PM 결정과 가정)

| 항목 | 내용 | 상태 |
|---|---|---|
| 기술 스택 | `CLAUDE.md` 0절 초안 확정 (#6 (b)): Java 21, **Spring Boot 4 (4.1.x)**, Spring Data JPA, PostgreSQL, Flyway, Gradle, JUnit 5, MockMvc, Testcontainers | 결정 2026-10-10 (Spring Boot 버전은 P1) |
| 배포 | 로컬 데모만 (#5 (a)). CI는 테스트·린트만 | 결정 |
| 테스트 fixture | #12 (a). M1a에는 외부 응답이 없어 해당 없음. 방침만 아래 "테스트 방침"에 고정한다 | 결정 |
| User-Agent 연락처 | #25 (a) 저장소 URL. 이름만 `.env.example` 에 넣는다 | 결정 |
| 기록 사진 사본 | #13 (a) Git 밖 사본. M1a는 저장 위치 약속만 적고 구현하지 않는다 (아래 "사본 저장 위치 약속") | 결정 |
| 지도·장소 제공자 | **가정**: 카카오 통일(#2 추천 (a)). M1a에는 이 가정에 기대는 코드가 없다. 카카오 키 이름은 M2a 계획 때 #2를 확정한 뒤 넣는다 | 미정 (M2 계획 전에 확정) |
| 지원 시작 연도 | M1a와 무관 (M2b) | 미정 (M2 계획 전에 확정) |

## 기술 선택

| 항목 | 정한 것 | 이유 |
|---|---|---|
| JDK | Java 21, Gradle toolchain (`JavaLanguageVersion.of(21)`) | `CLAUDE.md` 0절 |
| 빌드 | Gradle Kotlin DSL(`build.gradle.kts`, `settings.gradle.kts`), Gradle Wrapper 커밋. Gradle 버전은 Spring Boot 4.1이 지원하는 버전(구현 때 공식 문서로 확인) | 타입 검사와 IDE 자동완성. Spring Initializr 기본 선택지 중 하나 |
| 프레임워크 | **Spring Boot 4.1.x 최신 패치** (2026-10-10 기준 4.1.1) — P1 결정 | spring.io 공식 지원 데이터(2026-10-10 조회): 3.5.x OSS 지원 2026-06-30 종료, 4.0.x 2026-12-31 종료, 4.1.x 2027-07-31까지 |
| 이름 | `rootProject.name = "the-world-that-day"`, `group = "io.github.ericmschoi"`, 메인 클래스 `io.github.ericmschoi.theworldthatday.TheWorldThatDayApplication` | `CLAUDE.md` 0절 이름 규칙 |
| 의존성 (main) | 웹(MVC), actuator, data-jpa, Flyway(PostgreSQL 지원 포함), `postgresql`(runtimeOnly) | 헬스 체크와 DB 골격에 필요한 최소 |
| 의존성 (test) | `spring-boot-starter-test`, Spring Boot Testcontainers 연동, Testcontainers PostgreSQL·JUnit Jupiter 모듈 | 실제 PostgreSQL로 마이그레이션 검증 |
| 넣지 않는 것 | Lombok(대신 Java `record`), WireMock(M3a), Spring Security, devtools | 필요해질 때 그 계획서에서 추가 |
| DB | PostgreSQL 17, 이미지 `postgres:17-alpine` 을 Compose와 Testcontainers에 **같은 태그**로 | 로컬과 테스트의 DB 버전 일치 |
| 헬스 체크 | Spring Boot Actuator health를 `/api/health` 로 노출 (직접 컨트롤러를 만들지 않음) | DB 상태가 자동으로 포함되고 코드가 없다 |
| 포맷·린트 | Spotless + google-java-format, `check` 태스크에 `spotlessCheck` 연결 | 포맷 논쟁 제거, CI에서 강제 |
| 테스트 이름 | 모든 테스트 `*Test`, `./gradlew test` 하나로 실행 (통합·단위 태스크 분리 안 함) | 프로젝트 초반에는 단순하게 |

**Spring Boot 4 / Spring Framework 7 / Jackson 3 영향 — 구현 때 공식 문서로 확인**: 이 계획서의 의존성·클래스·설정 키 이름은 Spring Boot 3 기준 자료를 바탕으로 적었다. Spring Boot 4.1에서는 다음이 바뀌었을 수 있으므로 backend-dev가 구현할 때 Spring Boot 4.1 공식 문서(레퍼런스, 마이그레이션 가이드)로 확인하고, 계획서와 다른 이름을 쓰면 보고서에 적는다.
- 스타터·모듈 이름(웹 MVC, Flyway, actuator, 테스트 스타터의 모듈 분리)
- Testcontainers 연동(`@ServiceConnection` 의 패키지, Testcontainers 아티팩트 이름과 메이저 버전)
- 테스트 슬라이스·테스트 클라이언트(`@SpringBootTest` 의 HTTP 테스트 클라이언트, `TestRestTemplate`·MockMvc 관련 패키지 이동 가능성)
- Jackson 3(패키지 이름 변경) — M1a는 JSON 직렬화 코드를 직접 쓰지 않지만, 테스트에서 응답 본문을 파싱할 때 해당
- actuator 설정 키(아래 "설정" 표의 `management.*`)
- Spotless·google-java-format이 Gradle·Java 버전과 맞는지

### 설정 (`server/src/main/resources/application.yml`)

| 키 | 값 | 이유 |
|---|---|---|
| `spring.application.name` | `the-world-that-day` | |
| `spring.config.import` | `optional:file:../.env[.properties]` | 루트 `.env` 하나를 Compose와 Spring이 함께 쓴다. `bootRun` 의 작업 폴더는 `server/` 라서 `../.env` 가 루트다. 파일이 없으면 무시(CI) |
| `spring.datasource.url` | `jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/${POSTGRES_DB:the_world_that_day}` | |
| `spring.datasource.username` / `password` | `${POSTGRES_USER}` / `${POSTGRES_PASSWORD}` — **비밀번호 기본값 없음** | 비밀값을 커밋하지 않는다 |
| `spring.jpa.hibernate.ddl-auto` | `validate` | 스키마는 Flyway로만 바꾼다 (backend-dev 규칙) |
| `spring.jpa.open-in-view` | `false` | 지연 로딩이 컨트롤러까지 새는 것을 막는다 |
| `server.port` | `8080` (명시) | M1b 프록시 계약 |
| `management.endpoints.web.base-path` | `/api` | 헬스 체크를 `/api/health` 로 |
| `management.endpoints.web.exposure.include` | `health` | 다른 actuator 엔드포인트 노출 안 함 |
| `management.endpoint.health.show-details` | `never` | DB 정보 노출 안 함 |
| `management.endpoints.web.discovery.enabled` | `false` | `GET /api` 가 actuator 링크 목록을 내지 않게 |

T1은 DB 관련 키(`spring.config.import`, `spring.datasource.*`, `spring.jpa.*`) 없이 시작하고, T2가 추가한다.

## API / 데이터 변경

### 헬스 체크 (M1b와의 계약)

```
GET /api/health
Accept: application/json

200 OK
Content-Type: application/json
{"status":"UP"}
```

- DB에 연결할 수 없으면 `503` 과 `{"status":"DOWN"}` (Actuator 기본 동작). 세부 항목(`components`)은 응답에 없다.
- `GET /api` 는 404 (actuator discovery 끔).

### DB

- `server/src/main/resources/db/migration/V1__baseline.sql`: 테이블을 만들지 않는 기준 마이그레이션(주석만). Flyway가 빈 스크립트를 거부하면 무해한 문장 하나(`SELECT 1;`)를 둔다. 첫 도메인 테이블은 V2부터.
- 엔티티 없음. `ddl-auto=validate` 는 엔티티가 없으므로 그대로 통과한다.

### 환경변수 (루트 `.env.example`)

값은 비운다. 주석에는 형식 설명만 쓰고 실제 값은 쓰지 않는다. `VITE_` 로 시작하는 이름은 없다(서버 전용 파일).

| 이름 | 쓰는 곳 | 비고 |
|---|---|---|
| `POSTGRES_DB` | `docker-compose.yml`, `application.yml` | |
| `POSTGRES_USER` | 같음 | |
| `POSTGRES_PASSWORD` | 같음 | Compose는 `${POSTGRES_PASSWORD:?...}` 로 비어 있으면 실패 |
| `POSTGRES_PORT` | 같음 | 호스트 포트. 비우면 5432 |
| `WIKIMEDIA_USER_AGENT` | M3a (M1a 코드는 읽지 않음) | #25. 주석: `형식: TheWorldThatDay/<버전> (<저장소 URL>)` |

summary 3절의 나머지 이름은 처음 쓰는 마일스톤의 계획서에서 추가한다.

| 이름 | 추가할 때 | 조건 |
|---|---|---|
| `KAKAO_REST_API_KEY` | M2a | #2 확정 후 |
| `DATA_GO_KR_SERVICE_KEY` | M2a (지역 사전) 또는 M5 | |
| `VITE_KAKAO_JS_KEY` | M4a·M4b, **`web/.env.example`** 에 | #8 (a) 브라우저 키 하나만. 루트 `.env` 에 두지 않는다 |
| `GOOGLE_MAPS_*`, `NCP_MAPS_*`, `VWORLD_API_KEY` | 해당 제공자를 고를 때만 | #2, #14 |

`.env` 형식 주의: 루트 `.env` 는 Compose와 Spring(`[.properties]`)이 함께 읽는다. 값에 따옴표·`export`·공백을 쓰지 않는다(두 도구의 해석이 다르다). `.env.example` 주석에 이 규칙을 적는다.

### 로컬 DB (`docker-compose.yml`, 저장소 루트)

- 서비스 이름 `db`, 이미지 `postgres:17-alpine`, 포트 `${POSTGRES_PORT:-5432}:5432`, 이름 있는 볼륨 `db-data`, `pg_isready` 헬스체크.
- 환경변수는 루트 `.env` 에서 읽는다. `.env` 를 만드는 것은 PM이다.

### 사본 저장 위치 약속 (#13, 구현은 나중)

- 큐레이션 미디어 사본은 저장소 밖 로컬 폴더에 둔다. 경로는 환경변수 `MEDIA_STORAGE_DIR`(이름 예약)로 받고, 서버가 `/media/**` 로 제공한다.
- 처음 사본을 쓰는 마일스톤(M3a 또는 M4c·M5b) 계획서에서 이 약속을 확정하고 `.env.example` 에 이름을 추가한다. #5 (a)(로컬 데모)라서 오브젝트 스토리지는 쓰지 않는다. 제3·4유형 자료는 #9 답 전까지 변환(썸네일 생성)하지 않는다.

## 확정 명령 (M1 끝나면 PM이 `CLAUDE.md` 0절에 반영)

| 목적 | 명령 | 조건 |
|---|---|---|
| 로컬 DB 시작 | `docker compose up -d db` (저장소 루트) | 루트 `.env` 필요 |
| 로컬 DB 중지 | `docker compose stop db` | 데이터 유지 |
| 서버 실행 | `cd server && ./gradlew bootRun` | DB 실행 중 |
| 서버 테스트 | `cd server && ./gradlew test` | **Docker 실행 중**(Testcontainers). `.env` 불필요 |
| 서버 린트(포맷 검사) | `cd server && ./gradlew spotlessCheck` | |
| 서버 포맷 자동 수정 | `cd server && ./gradlew spotlessApply` | |
| 서버 전체 검사 (CI와 같음) | `cd server && ./gradlew check` | test + spotlessCheck |

웹 명령은 `docs/plans/m1b-bootstrap-web.md` "확정 명령"에 있다. `CLAUDE.md` 0절에 넣을 문구 제안은 아래 "끝나면 PM이 할 것"에 있다.

## 테스트 방침 (#12 반영, M1 이후 모든 서버 작업에 적용)

1. 테스트는 외부 네트워크를 호출하지 않는다. 예외는 테스트 인프라(Testcontainers가 Docker로 띄우는 PostgreSQL)뿐이다.
2. 외부 응답은 WireMock과 `server/src/test/resources/fixtures/<source>/` 의 fixture로 대신한다.
   - 카카오·네이버·Google 응답: **공식 문서의 필드 구조로 만든 합성 fixture**. 실제 응답을 녹화해 커밋하지 않는다. fixture 파일 첫머리(또는 옆 `README`)에 근거한 공식 문서 URL을 적는다.
   - 복제를 허용하는 Wikimedia 응답(위키백과 CC BY-SA 4.0, Wikidata CC0, Commons 메타데이터): 녹화 fixture 가능. 녹화 일자와 원 URL, 라이선스를 함께 적는다.
3. 테스트 입력은 제품 정의서 12절 데모 입력과 경계값만 쓴다.
4. DB가 필요한 테스트는 공용 `TestcontainersConfiguration`(`@ServiceConnection` PostgreSQL)을 가져다 쓴다. H2 같은 대체 DB를 쓰지 않는다.
5. 로그에 생년월일과 장소를 함께 남기지 않는다. 요청 본문·쿼리 문자열을 로그로 남기는 필터(`CommonsRequestLoggingFilter` 등)를 켜지 않는다.

`CLAUDE.md` 7절 5항은 P3 결정으로 이 방침과 같은 문구로 바뀐다(오케스트레이터 반영). `.claude/agents/backend-dev.md` 의 "녹화 응답" 문구는 이번에 고치지 않으며, 충돌하면 `CLAUDE.md` 가 우선한다.

## summary 8절 처리 — M1에서 할 것과 미룰 것

| # | 발견 | 처리 | 어디서 |
|---|---|---|---|
| 1 | 지도 SDK 로딩(React에서 한 번만 불러오는 로더) | 미룸 | M4a (frontend) |
| 2 | 브라우저 키와 서버 키 분리 | **M1에서 규칙 고정**: 루트 `.env.example` 은 서버 전용(`VITE_` 없음), 브라우저 키는 M4에서 `web/.env.example` 에. M1b는 환경변수를 쓰지 않는다 | M1a T3, M1b |
| 3 | 테스트 도구(SDK 스텁, 합성 fixture) | **M1에서 방침 고정**(위 "테스트 방침", M1b의 MSW `onUnhandledRequest: 'error'`). SDK 스텁 구현은 M4a, WireMock 추가는 M3a | M1a·M1b, M3a, M4a |
| 4 | 좌표 변환 라이브러리 | 넣지 않음. 1순위 조합(카카오, WGS84)에서 필요 없다. 브이월드 WMTS(#14 조합 B)를 고를 때만 M4a에서 검토 | M4a (조건부) |
| 5 | 위키텍스트 파서 | 미룸 | M3a |
| 6 | DB와 캐시 (장소 캐시 대신 지역 사전, 위키백과 문서 캐시) | **M1은 DB 연결과 Flyway까지만.** 캐시 테이블은 M3a, 지역 사전은 `data/regions.json`(M2a) | M1a T2, M2a, M3a |
| 7 | 외부 호출 클라이언트(타임아웃, User-Agent, 429·`Retry-After`, 엔드포인트 설정값) | 미룸. 쓰는 곳 없이 만들면 검증할 수 없다. 첫 외부 어댑터(Wikimedia)와 함께 WireMock 테스트로 만든다. M1a는 `WIKIMEDIA_USER_AGENT` 이름만 예약 | M3a |
| 8 | 큐레이션 미디어 사본 저장 | **M1은 위치 약속만**(`MEDIA_STORAGE_DIR`, `/media/**`). 구현은 처음 쓰는 마일스톤 | M3a 또는 M4c·M5b |
| 9 | 조건부(Google 서명, 대한뉴스 CSV 색인) | 미룸 | #2 (c)일 때 M4, #24 (b)일 때 M3c |

## M1a·M1b 경계 (병렬 진행)

**판단: 병렬 가능 (P4 (a) 결정).** 두 브랜치는 서로 다른 파일만 만든다. 각자 `main` 에서 갈라지고 각자 PR을 연다. 머지 순서는 상관없다. 오케스트레이터는 에이전트마다 worktree를 따로 둔다.

| 항목 | 값 | 정하는 쪽 | 쓰는 쪽 |
|---|---|---|---|
| 서버 포트 | `8080` | M1a (`server.port`) | M1b Vite 프록시 대상 `http://localhost:8080` |
| API 접두사 | `/api` | M1a | M1b 프록시 규칙 `/api` |
| 헬스 체크 | `GET /api/health` → `200 {"status":"UP"}` | M1a | M1b MSW 핸들러·API 클라이언트 테스트, PM의 프록시 확인 |
| 웹 개발 서버 | `http://localhost:5173` (`strictPort`) | M1b | M4 카카오 JavaScript SDK 도메인 등록 |
| CI 워크플로 | **파일 두 개**: `.github/workflows/server-ci.yml`(M1a 소유), `.github/workflows/web-ci.yml`(M1b 소유) | 각자 | 한 파일이면 두 브랜치가 같은 파일을 만들어 충돌하므로 나눈다 |

파일 소유 (에이전트 정의의 "자기 폴더 밖 수정 금지"에 대한 예외를 파일 단위로 정한다 — P4 (a)로 승인됨):

| 파일·폴더 | M1a (backend-dev) | M1b (frontend-dev) |
|---|---|---|
| `server/**` | 만든다 | 건드리지 않음 |
| `web/**` | 건드리지 않음 | 만든다 |
| `docker-compose.yml` (루트) | 만든다 (예외) | 건드리지 않음 |
| `.env.example` (루트) | 만든다 (예외) | 건드리지 않음 |
| `.github/workflows/server-ci.yml` | 만든다 (예외) | 건드리지 않음 |
| `.github/workflows/web-ci.yml` | 건드리지 않음 | 만든다 (예외) |
| 루트 `.gitignore`, `README.md`, `CLAUDE.md`, `.claude/`, `docs/` | 건드리지 않음 | 건드리지 않음 |

- 빌드 산출물 무시 규칙은 루트 `.gitignore` 에 이미 있다(`server/build/`, `server/.gradle/`, `web/node_modules/`, `web/dist/`). 더 필요하면 각자 `server/.gitignore`, `web/.gitignore` 에 둔다.
- CI 경로 필터: `server-ci` 는 `server/**`, `docker-compose.yml`, `.github/workflows/server-ci.yml` 이 바뀔 때만, `web-ci` 는 `web/**`, `.github/workflows/web-ci.yml` 이 바뀔 때만 돈다. 문서만 바뀐 PR에서는 둘 다 돌지 않는다(리스크 4).
- 계획서 커밋 위치 제안: 이 계획서와 `docs/roadmap.md` 는 `chore/bootstrap-server` 에, `docs/plans/m1b-bootstrap-web.md` 는 `chore/bootstrap-web` 에 커밋한다(파일이 겹치지 않는다).

## 크기 (400줄 규칙)

**세는 법** (`CLAUDE.md` 4절 "코드 기준 약 400줄"을 이 프로젝트에서 이렇게 센다. M1b와 같은 기준):
- 센다: 직접 쓴 제품 코드 + 직접 쓴 설정(빌드 스크립트, `application.yml`, 마이그레이션, Compose, CI 워크플로, `.env.example`).
- 따로 세어 보고만 한다(한도에 넣지 않음): 테스트 코드와 테스트 리소스.
- 뺀다: 생성 파일과 lockfile(Gradle Wrapper `gradlew`, `gradlew.bat`, `gradle/wrapper/**`), 문서(`docs/**`).

확인 명령 (test-engineer가 T5에서 실행해 보고):

```
git diff --numstat main...HEAD -- server docker-compose.yml .env.example .github/workflows/server-ci.yml \
  ':(exclude)server/gradlew' ':(exclude)server/gradlew.bat' ':(exclude)server/gradle/wrapper/**' \
  ':(exclude)server/src/test/**'
```

**예상**: 제품 코드·설정 약 180~230줄(빌드 스크립트 약 60, `application.yml` 약 30, 메인 클래스 약 10, V1 약 5, Compose 약 25, `.env.example` 약 12, CI 약 40), 테스트 약 80~120줄. 한도 안이라 더 나누지 않는다.

## 작업 목록

순서: T1 → (T2, T3, T4 는 T1 이후 서로 독립) → T5 → T6.

| ID | 작업 | 담당 | 산출물 | 의존 | 완료 기준 |
|----|------|------|--------|------|-----------|
| T1 | Gradle 프로젝트 골격, Actuator 헬스 체크, Spotless | backend-dev | `server/settings.gradle.kts`, `server/build.gradle.kts`, Gradle Wrapper, `server/src/main/java/io/github/ericmschoi/theworldthatday/TheWorldThatDayApplication.java`, `server/src/main/resources/application.yml`(DB 키 제외), 헬스 체크 테스트 | - | (1) `cd server && ./gradlew check` 성공. (2) `grep 'rootProject.name = "the-world-that-day"' server/settings.gradle.kts` 와 `grep 'JavaLanguageVersion.of(21)' server/build.gradle.kts` 가 각각 1줄. (3) `@SpringBootTest(webEnvironment = RANDOM_PORT)` 테스트가 `GET /api/health` → 200, 본문 `status` 가 `"UP"`, 본문에 `components` 키 없음을 확인. (4) 같은 테스트 클래스가 `GET /api` → 404 확인. (5) `./gradlew check --dry-run` 출력에 `:spotlessCheck` 가 있다. (6) 고른 Spring Boot 4.1.x 패치·Gradle 버전을 보고서에 적는다 |
| T2 | DB 연결 기반: PostgreSQL 데이터소스, Flyway V1, JPA 설정, Testcontainers 스모크 테스트 | backend-dev | 의존성 추가, `application.yml` DB·JPA 키(위 "설정" 표), `server/src/main/resources/db/migration/V1__baseline.sql`, `server/src/test/java/io/github/ericmschoi/theworldthatday/TestcontainersConfiguration.java`(`postgres:17-alpine`, `@ServiceConnection`), 스모크 테스트 | T1 | (1) Docker 실행 중 `cd server && ./gradlew check` 성공. (2) 테스트가 `flyway_schema_history` 에 `version = '1'`, `success = true` 행이 있음을 확인. (3) 테스트가 `HealthEndpoint` 의 `db` 구성요소 상태 `UP` 을 확인하고, `GET /api/health` 응답은 여전히 `{"status":"UP"}` 만 담는다(세부 없음). (4) `application.yml` 에 `ddl-auto: validate`, `open-in-view: false` 가 있고 `POSTGRES_PASSWORD` 기본값이 없다(`grep -n 'POSTGRES_PASSWORD:' server/src/main/resources/application.yml` 결과 없음). (5) T1의 헬스 체크 테스트가 같은 `TestcontainersConfiguration` 을 쓰도록 바뀌어 통과한다 |
| T3 | 로컬 DB Compose와 `.env.example` | backend-dev | 루트 `docker-compose.yml`, 루트 `.env.example` | T1 | (1) `POSTGRES_DB=x POSTGRES_USER=x POSTGRES_PASSWORD=x docker compose config --quiet` 종료 코드 0 (가짜 값, 커밋하지 않음). (2) 환경변수 없이 `docker compose config --quiet` 는 `POSTGRES_PASSWORD` 누락 메시지로 실패. (3) `grep -c 'postgres:17-alpine' docker-compose.yml` = 1, 서비스 이름 `db`, `pg_isready` 헬스체크 있음. (4) `.env.example` 에 이름 5개(`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`, `WIKIMEDIA_USER_AGENT`)가 있고 아래 검사 명령 C1의 출력이 없다(값 없음). (5) `grep -c '^VITE_' .env.example` = 0 |
| T4 | 서버 CI 워크플로 | backend-dev | `.github/workflows/server-ci.yml` | T1 | (1) 트리거: `pull_request` 와 `main` 대상 `push`, 경로 필터 `server/**`, `docker-compose.yml`, `.github/workflows/server-ci.yml`. (2) `permissions: contents: read`. (3) Temurin Java 21 설정, Gradle 캐시(setup-gradle 액션), `working-directory: server` 에서 `./gradlew check`. (4) `timeout-minutes` 설정. (5) `grep -c 'secrets\.' .github/workflows/server-ci.yml` = 0, 배포·업로드 단계 없음. (6) PR을 연 뒤 오케스트레이터가 `gh pr checks` 로 `server` 잡 통과를 확인한다 |
| T5 | 완료 기준 검증과 전체 테스트 | test-engineer | 빠진 기준의 테스트 추가(테스트 파일만) | T2, T3, T4 | (1) T1~T4 완료 기준을 표로 확인하고 빠진 테스트를 추가. (2) `cd server && ./gradlew check` 통과(통과·실패 개수 보고). (3) `postgres:17-alpine` 이 `docker-compose.yml` 과 `TestcontainersConfiguration` 에 같은 태그로 있다(`grep -rn 'postgres:17-alpine' docker-compose.yml server/src/test` 2곳 이상). (4) 아래 검사 명령 C2의 출력이 없다(서버 코드에 외부 호출 클라이언트 없음). (5) "크기" 절의 `git diff --numstat` 결과(제품·설정 줄 수, 테스트 줄 수)를 보고 |
| T6 | 로컬 실행 확인 (PR 검수 때) | PM | - | T5 | (1) `.env.example` 을 복사해 루트 `.env` 를 만든다(값은 PM이 정함). (2) `docker compose up -d db` 후 `docker compose ps` 에서 `db` 가 `healthy`. (3) `cd server && ./gradlew bootRun` 후 다른 터미널에서 `curl -s http://localhost:8080/api/health` → `{"status":"UP"}`. (4) `docker compose stop db` 후 같은 `curl` 이 503을 받는 것을 본다 |

완료 기준에 쓰는 검사 명령 (저장소 루트에서 실행):

```
# C1 — .env.example 에 주석·빈 줄·"이름=" 말고 다른 줄(값이 든 줄)이 없는지. 출력이 없어야 한다
grep -vE '^(#.*)?$' .env.example | grep -vE '^[A-Z0-9_]+=$'

# C2 — 서버 제품 코드에 외부 호출 클라이언트가 없는지 (M1a 범위). 출력이 없어야 한다
grep -rnE 'RestClient|WebClient|RestTemplate|HttpClient' server/src/main
```

담당별: backend-dev 4 (T1~T4), test-engineer 1 (T5), PM 1 (T6).

## 리스크와 확인할 것

1. **Spring Boot 4로 바뀐 부분** — 참고 자료와 예제 대부분이 Spring Boot 3 기준이다. 스타터·모듈 이름, Testcontainers 연동, 테스트 슬라이스·테스트 클라이언트 패키지, Jackson 3, actuator 설정 키가 이 계획서의 표기와 다를 수 있다. → 구현 때 Spring Boot 4.1 공식 문서로 확인하고, 다르면 보고서에 적는다(위 "기술 선택" 아래 목록). 4.1.x OSS 지원은 2027-07-31까지라 MVP 기간을 덮는다.
2. **Docker가 필수다** — `./gradlew test` 가 Testcontainers를 쓰므로 Docker 데몬이 없으면 테스트가 실패한다. 에이전트 환경에서 `docker` 명령은 허용 목록에 없어 권한 확인 창이 뜰 수 있다(`./gradlew` 는 허용). GitHub 호스티드 Ubuntu 러너에는 Docker가 있다.
3. **첫 빌드는 네트워크가 필요하다** — Gradle 배포판, 의존성, Docker 이미지 내려받기. 이것은 "테스트가 외부 네트워크를 호출하지 않는다" 규칙의 대상이 아니다(테스트 코드가 외부 API를 부르는 것이 아님).
4. **CI 경로 필터와 필수 상태 검사** — PM이 GitHub 브랜치 보호에서 `server`·`web` 잡을 필수로 지정하면, 경로 필터 때문에 돌지 않은 잡이 PR을 막는다. 필수 검사를 켤 때는 경로 필터를 빼거나 항상 도는 요약 잡을 추가한다(그때 별도 `chore` 기능으로).
5. **루트 `.env` 를 두 도구가 함께 읽는다** — Compose와 Spring `[.properties]` 가 따옴표·공백을 다르게 해석한다. `.env.example` 주석에 "따옴표 없이 `KEY=value`"를 적는다. `spring.config.import` 는 테스트 실행 때도 루트 `.env` 를 읽지만 `@ServiceConnection` 이 데이터소스를 덮어쓰므로 테스트 결과에 영향이 없다.
6. **빈 V1 마이그레이션** — Flyway 버전에 따라 내용 없는 스크립트를 다르게 처리할 수 있다. T2에서 확인하고 필요하면 `SELECT 1;` 을 둔다.
7. **Actuator를 `/api` 아래에 둔 것** — 이후 도메인 컨트롤러가 `/api/health` 경로를 쓰면 안 된다. `/api` 하위의 다른 경로와는 충돌하지 않는다.

## PM이 정할 것

| # | 질문 | 결정 (2026-10-10) |
|---|---|---|
| P1 | Spring Boot 메이저 버전 | 결정: **Spring Boot 4.1.x** (spring.io 공식 지원 데이터 기준 3.5.x OSS 지원 2026-06-30 종료, 4.1.x는 2027-07-31까지). `CLAUDE.md` 0절은 오케스트레이터가 "Spring Boot 4 (4.1.x)"로 고친다 |
| P2 | T2(DB 연결 기반)를 PM이 직접 구현할지 | 결정: (b) **backend-dev가 한다**. ★ 표시를 지웠다 |
| P3 | `CLAUDE.md` 7절 5항 문구 (#12와 맞추기) | 결정: 반영함. 오케스트레이터가 제안 문구대로 고친다. `.claude/agents/backend-dev.md` 는 이번에 고치지 않는다(`CLAUDE.md` 가 우선) |
| P4 | 병렬 진행과 파일 소유 예외 | 결정: (a) M1a·M1b 병렬(worktree), "파일 소유" 표의 파일 단위 예외 승인 |

## 끝나면 PM이 할 것

M1a·M1b가 모두 머지되면 `CLAUDE.md` 0절을 아래처럼 바꾸는 것을 제안한다.

```
- 실행 명령: `docker compose up -d db` (루트 `.env` 필요), `cd server && ./gradlew bootRun` (http://localhost:8080), `cd web && npm run dev` (http://localhost:5173, `/api` 는 8080으로 프록시)
- 테스트 명령: `cd server && ./gradlew test` (Docker 실행 중), `cd web && npm test`
- 린트·포맷 명령: `cd server && ./gradlew spotlessCheck` (수정: `spotlessApply`), `cd web && npm run lint && npm run format:check && npm run typecheck` (수정: `npm run format`)
- 전체 검사 (CI와 같음): `cd server && ./gradlew check`, `cd web && npm run lint && npm run format:check && npm run typecheck && npm test && npm run build`
```
