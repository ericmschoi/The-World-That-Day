# M3 후속 정리 (chore)

> 브랜치: `chore/m3-followups` (`main` 7bbdc96에서 갈라짐) · 작성: planner · 상태: **PM 승인 (2026-10-27): D1 (a) T1~T7, D2 (a) `source=<접두어> rule=<규칙>`, D3 (b) T8 진단만, D4 (c) PM 직접 구현 없음 — T2도 backend-dev, D5 (a) backend-dev가 `server-ci.yml` 수정**
> 기준: PM 지시(2026-10-27, `server-ci` 경로를 먼저, 나머지는 추천), `docs/plans/chore-m1-followups.md`(형식 선례), `docs/plans/m3a5-official-channel-video.md` "후속 작업 후보" 표, `docs/plans/m3a5a-youtube-video-source.md` T4 결정 2, `docs/plans/m3a5b-daehan-news-video.md`, `CLAUDE.md` 4절·5절·7절
> 위치: M3 PR 수에 넣지 않는 작은 `chore` PR (`chore/m1-followups` 와 같은 방식). M3a5b 계획의 PM T0(대한뉴스 CSV·N1·N2)을 기다리는 동안 한다. M3a5b와 바꾸는 파일이 겹치지 않는다. 단, **T1은 M3a5b T1(`data/daehan-news/` 추가)보다 먼저 머지**한다.

## 목표

1. `data/**` 나 `web/src/media/types.ts` 만 바뀐 PR에서도 `server-ci` 가 돈다. (서버 시험이 두 곳을 읽는다)
2. `MediaNormalizer` 의 DEBUG 줄이 영상 ID를 남기지 않는다. 공개 배포 전에 닫아야 하는 항목이고, M3a5a T4 결정 2 (a)의 예외가 사라진다.
3. 시험·이름·mock의 작은 어긋남 4건을 바로잡는다: `SearchCachePurgeStartupTest` 의 `toString()` 비교, `privacy.test.ts` 의 낡은 이름, `WorldSection.test` 의 낡은 describe 이름, 웹 mock `selection.score` 소수.
4. 위키백과 사건 어댑터 두 개가 Action API `error` 를 M3a3d T0의 공용 검사로 읽는다(로그 한 줄을 원격 값이 모양 짓지 못하게).
5. 흔들리는 `TwoPageKoWikiEventSourceTest` 의 원인을 찾아 보고한다(PM이 고르면, 결정 D3).

## 범위 밖

- YouTube 쿼터 소진(UNITS) 확인: M3a5b 결정 3 (a)에 이미 있다.
- 후보 5·7·11(아래 "후보별 판단"): 이번에 하지 않는다. 이유는 표에 있다.
- `web/src/world/types.ts` 등 다른 파일을 `server-ci` 경로에 넣는 일: 서버 시험이 읽는 저장소 밖 파일은 아래 표 1번의 목록뿐이다.
- 새 환경변수, 새 외부 호출, 도메인 로직 변경, `application.yml` 값 변경.
- "서버 시험이 읽는 저장소 파일이 경로 목록에 있는지" 자동으로 검사하는 시험: 다음 단계 제안으로만 둔다.

## 사용할 외부 출처

없음. (`tests never call the external network`; 이번 작업의 시험은 기존 fixture·WireMock만 쓴다.)

## 후보별 판단

| # | 후보 | 판단 | 근거 |
|---|---|---|---|
| 1 | `server-ci` 경로 | **포함 (T1, 필수)** | 서버 시험이 읽는 저장소 파일을 코드에서 모두 찾았다: `docker-compose.yml`, `.env.example`, `.github/workflows/server-ci.yml`(이미 경로에 있음), **`data/` 아래 파일**(`DataSchemas.DATA = ../data`, `EventMediaExpectationsTest`, `EventCurationTest`), **`web/src/media/types.ts`**(`SelectionWebContractTest`, `MediaWebFieldsContractTest` 가 서버 DTO와 웹 타입의 필드 이름·모양을 대조). 이 둘만 바뀐 PR은 `web-ci` 만 돌거나 아무 CI도 돌지 않아, 서버-웹 계약이나 데이터 형식이 깨져도 PR에서 모른다. M3a5b PR은 `server/**` 도 바꾸므로 어차피 돌지만, 데이터만 바꾸는 PR(M5a 명소 데이터 등)은 못 잡는다 |
| 2 | `MediaNormalizer` DEBUG의 영상 ID | **포함 (T2)** | `MediaNormalizer.java:55` 가 `record.id()` 를 남긴다. YouTube 레코드 ID는 `youtube-<영상 ID>` 다. 공개 배포 전 필수(M3a5a T4 결정 2 (a)). 고칠 곳은 한 줄과 시험 두 파일 |
| 3 | `SearchCachePurgeStartupTest` 의 `toString()` | **포함 (T3)** | `:69` 가 Spring 작업의 `toString()` 문자열에 `SearchCachePurge.purge` 가 있는지로 찾는다. 스프링이 문자열 모양을 바꾸면 이유 없이 실패한다. 시험 6줄 안팎 |
| 4 | `privacy.test.ts` 낡은 이름 | **포함 (T6)** | `:58` 이름이 "…, and the one to come" 인데 본문은 YouTube 포함 현재형 문장 하나만 확인한다(M3a5a T6에서 바뀜). `:57` 주석도 M3a3b 기준. 이름·주석만 |
| 5 | Wikimedia 시작 WARN | **뺌** | `WikimediaHttpClient.java:86` 에 `WIKIMEDIA_USER_AGENT is empty…` WARN이 있고, 시험 설정의 `wikimedia.user-agent: ""` 때문에 기본 시험 컨텍스트에서 찍힌다. 그러나 WARN 줄 수를 세는 시험(`CommonsMediaLookupTest`, `WorldStoryCommonsVideoTest`, `SearchCachePurgeTest` 등)은 코드를 읽은 범위에서 모두 `WikimediaWireMockTest`(User-Agent를 넣어 Wikimedia를 켬)를 쓰거나 자기 컨텍스트를 만들어서 이 WARN이 나오지 않는다. 실제로 실패한 적이 없다. 이 WARN을 확인하는 시험도 없어 줄여도 잃는 것은 없지만, 고칠 문제가 아직 없다. 안 나는 문제를 막는 3줄 설정은 문제가 보일 때 한다. 참고: M3a5b T2도 "색인이 없으면 WARN 1줄"을 쓰는데, 시험 설정에서는 `data/daehan-news/segments.csv` 가 있어 WARN이 나지 않는다 |
| 6 | 웹 mock `selection.score` | **포함 (T5)** | `web/src/mocks/fixtures/world/youtube.ts:53` 의 `0.9`. 서버 `Selection` 은 `int score`(규칙표 합계)다. **범위는 0~100이 아니다**: `WorldStoryScoreTableTest` 에 130이 있고(`Expect.attached(130 …)`), 같은 시험의 대한뉴스 1988 approx 기대값이 90이다. 그래서 90으로 바꾸고, 정수만 확인한다(상한은 걸지 않는다) |
| 7 | 공용 `noUnmatchedRequest` | **뺌** | 23개 시험 파일에 약 35곳이 있고 각각 한 줄(`assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty()`)이다. 공용 `@AfterEach` 로 옮기면 줄이 줄지 않고(도우미 호출도 한 줄), 시험 메서드 20여 개가 사라지거나 위치가 바뀌어 `CLAUDE.md` 5절("시험을 지우지 않는다")과 리뷰 부담이 커진다. 얻는 것이 작다 → M6b 시험 정리 때 |
| 8 | 위키백과 어댑터 `ActionApiErrors` | **포함 (T4)** | `ActionApiKoWikiClient.java:53·121`, `ActionApiKoWikiLinkResolver.java:99` 가 원격 `error.code` 문자열을 걸러내지 않고 `"api error " + error` 로 로그에 쓴다. 미디어 어댑터 둘(`CommonsActionApi`, `ActionApiKoWikiPageImages`)은 이미 공용 검사를 쓴다. 한쪽만 안 쓰면 "원격 값이 로그 줄을 모양 짓지 못한다"는 규칙이 반쪽이다. 제품 코드 약 15줄 |
| 9 | `WorldSection.test` describe 이름 | **포함 (T6)** | `:179` "leaves tragic events to M3b2b" 는 M3b2b가 끝나(`TragicEvents` 블록) 낡았다. `:145` "today's server shape"(M3a2 시점, 미디어 없는 응답)도 같다. 이름만 |
| 10 | 흔들리는 `TwoPageKoWikiEventSourceTest` | **진단만 (T8, 결정 D3)** | 코드를 읽어도 원인이 확정되지 않는다. 확인한 것: `TwoPageKoWikiEventSource.find` 는 두 문서를 **차례로** 부르고(요청 순서 고정), 재시도에 잠자기가 없고(`HostRateLimiter` 는 기다리지 않음), `WireMockExtension` 은 시험마다 초기화되고, 시계는 `MutableClock` 이다. 남는 의심은 **실제 시간 예산**이다: 읽기 시간 초과 2초·하루 상한 5초(`KoWikiSourceFixture` 생성자 인자)가 느린 CI나 첫 요청 준비(클래스 로딩·큰 녹화 JSON 파싱)에서 깨질 수 있다(깨지면 "day deadline"으로 문서 하나가 `UNAVAILABLE` 이 되어 `params(...)`·`docs()` 단언이 어긋남). 이는 추정이다. 고침이 안전한지: 원인이 시간 예산이면 **예산만 넓히는 것은 단언을 약하게 하지 않는다**(이 클래스는 "시간 초과 시험이 아님"이라고 스스로 적음). 원인이 요청 순서나 상태 공유면 단언이 아니라 설정을 고쳐야 해서 다시 본다. 재현 없이 고치지 않는다 |
| 11 | M3b3c 리뷰 권장 1 | **뺌** | `YouTubePlayer.tsx` 의 상태 머신(`flags.calls`, `want`, `userPaused`)을 건드리는 동작 수정이다. `onReady` 전에 사용자가 튼 재생을 `register` 전이라 조정자가 모르는 문제로, 고치려면 등록 시점이나 `onReady` 처리를 바꿔야 하고 실제 YouTube 브라우저 확인(M3b3c T6, M3a5a 머지 뒤 PM)이 있어야 맞는지 안다. 이 PR은 "동작이 안 바뀌는 정리"로 두는 편이 안전하다. 로드맵이 이미 "M3b3c T6 전에 작은 `fix/`"로 적어 두었다 → M3b3c T6 결과를 보고 그 `fix/` 로 |

## API / 데이터 변경

없음. 응답 모양·DB·`data/` 파일 형식은 그대로다. 바뀌는 것:

- `.github/workflows/server-ci.yml` 경로 목록 (T1)
- DEBUG 로그 한 줄의 모양 (T2): `Media dropped: id=youtube-Abc123xyz_5 rule=R1` → `Media dropped: source=youtube rule=R1`
- 위키백과 어댑터 실패 로그의 `cause=` 문구는 정상적인 코드(`[a-z0-9-]{1,64}`)에서는 그대로(`api error ratelimited`). 코드가 이상하거나 없으면 `api error` 만 나온다 (T4)

### T1: `server-ci` 경로 (두 트리거 모두 6개, 순서 고정)

```yaml
paths:
  - "server/**"
  - "data/**"
  - "web/src/media/types.ts"
  - "docker-compose.yml"
  - ".env.example"
  - ".github/workflows/server-ci.yml"
```

`web/src/media/types.ts` 는 파일 하나로 둔다(웹 전체 `web/**` 를 넣으면 웹 PR마다 서버 20분 작업이 돈다).

### T2: DEBUG 줄 내용

`Media dropped: source=<출처 접두어> rule=<규칙>`. 출처 접두어는 레코드 ID에서 **첫 `-` 앞**이 `[a-z0-9]{1,16}` 에 맞을 때만 쓰고(`youtube-…` → `youtube`, `commons-123` → `commons`, `example-image` → `example`), 아니거나 ID가 비었으면 `unknown`. 이 규칙으로 영상 ID, 파일 번호, 날짜, 장소, 제목이 줄에 들어갈 수 없고 원격 값이 줄 모양을 바꾸지도 못한다. 어느 출처의 어느 규칙이 버려지는지는 계속 보인다(디버깅 가치 유지). 잃는 것: 버려진 항목을 ID로 바로 짚는 일(Commons는 시험 fixture로 재현한다). 결정 D2.

## 작업 목록

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | `server-ci` 경로에 `data/**`·`web/src/media/types.ts` 더하기, 경로 시험 갱신 | backend-dev | - | (1) C1 출력 `2`, C2 출력 `2`. (2) `ServerCiWorkflowTest` 의 `PATHS` 가 위 6개(순서 포함)이고 `runsOnPullRequestsForServerPaths`·`runsOnPushToMainForServerPaths` 가 통과한다. (3) 두 트리거의 목록이 서로 같다(기존 시험이 같은 `PATHS` 로 확인). (4) 워크플로의 다른 줄(권한·시간 제한·단계)은 그대로: `usesNoSecretsAndNoDeployOrUploadSteps` 등 나머지 시험 통과. (5) 커밋은 `ci(server): …` 하나, `Task: T1` |
| T2 | ★ `MediaNormalizer` DEBUG 줄에서 ID 빼고 출처 접두어로 | backend-dev | T1 | (1) C3 출력 없음. (2) 위 모양의 줄이 DEBUG에서 나온다: `MediaNormalizerLogTest` 가 `Media dropped: source=example rule=R1` 을 확인하고 `example-image` 는 포함하지 않는다. (3) 접두어 규칙 단위 시험: `youtube-Abc123xyz_5`→`youtube`, `commons-123`→`commons`, `example-image`→`example`, ID가 `null`/빈 문자열/공백→`unknown`, `-` 가 없는 ID→`unknown`, 대문자·공백·줄바꿈이 든 접두어→`unknown`, 17자 이상 접두어→`unknown`. (4) `WorldStoryYoutubeLogTest`: 예외가 사라진다. 모든 로그 줄에 `Abc123xyz` 가 없고(`isNotEmpty` 대신 `noneMatch`), `Media dropped: source=youtube rule=R1` 줄이 DEBUG로 있다. (5) 기존 `doesNotContain` 목록(`2002-06-22`·`1988-09-17`·`country:KR`, 키·제목·채널·날짜 등)은 그대로. (6) 로그 줄에 날짜·장소·제목·ID가 없음을 `CLAUDE.md` 7.6으로 확인. (7) `cd server && ./gradlew check` 성공 |
| T3 | `SearchCachePurgeStartupTest` 가 스프링 작업을 `ScheduledMethodRunnable` 로 찾기 | backend-dev | T1 | (1) C4 출력 없음. (2) `itRunsAgainEveryHour` 가 `task.getRunnable()` 이 `ScheduledMethodRunnable` 이고 `getTarget()` 이 `SearchCachePurge`, `getMethod().getName()` 이 `purge` 인 작업을 고른다. (3) `singleElement()` 와 간격·첫 지연 1시간 단언은 그대로(0개나 2개면 실패). (4) 시험 두 개 모두 통과, `@Test` 줄은 하나도 지워지지 않는다 |
| T4 | 위키백과 어댑터 둘이 `ActionApiErrors` 를 쓰기 | backend-dev | T1 | (1) C5 출력 없음(`ActionApiKoWikiClient`·`ActionApiKoWikiLinkResolver` 에 `path("error")` 직접 읽기가 없다). (2) `ratelimited` 는 `ActionApiErrors.isRateLimited`, 실패 원인은 `ActionApiErrors.cause`. `missingtitle`(문서 없음)은 계속 `absent` 로 처리되고 실패 로그를 남기지 않는다(필요하면 `ActionApiErrors` 에 코드 비교 도우미를 공개 메서드로 하나 더함). (3) 기존 `ActionApiKoWikiClientTest`(`title=9월 17일 cause=api error ratelimited` 포함)·`ActionApiKoWikiLinkResolverTest`·`CachingKoWikiClientTest`·`TwoPageKoWikiEventSourceTest` 를 **고치지 않고** 통과. (4) 새 시험: 코드에 줄바꿈·공백·대문자·65자 이상이 든 `error` → 로그에 `cause=api error` 만 나오고 원격 문자열은 없다(두 어댑터 모두); 코드 없는 `error` 객체 → 실패로 처리; `ratelimited` → 호스트 차단(`http.blockHost`)이 그대로. 시험 입력은 기존 `ActionApiErrorsTest.errorWithCode` 를 재사용하거나 같은 모양의 합성 JSON. (5) `cd server && ./gradlew check` 성공 |
| T5 | 웹 mock `selection.score` 를 정수로 | frontend-dev | - | (1) C6 출력 없음. (2) `exampleDaehanNews.selection.score` 가 `90`. (3) 새 시험(`web/src/world/worldFixtures.test.ts`): `youtube.ts` 가 내보내는 모든 미디어의 `selection.score` 가 `Number.isInteger` 이다. 에이전트가 고치기 전(`0.9`)에 이 시험이 **실패함**을 보고한다. (4) 상한(100)은 단언하지 않는다(서버 규칙표 합계는 130까지). (5) `cd web && npm run lint && npm run format:check && npm run typecheck && npm test` 성공. 기존 단언 변경 없음 |
| T6 | 시험 이름 정리: `privacy.test.ts`, `WorldSection.test.tsx` | frontend-dev | - | (1) C7 출력 없음. (2) `privacy.test.ts` 의 `it` 이름이 서버가 지금 부르는 출처만 말하고(예: `lists the outside sources the server calls now`), `T1 (9)` 주석이 M3a5a 기준(`application.yml`, YouTube Data API 포함)으로 바뀐다. 기대 문장(`지금은 한국어 위키백과, Wikidata, Wikimedia Commons(사진·영상), YouTube Data API(KTV 아카이브 채널 영상 검색)입니다.`)은 한 글자도 안 바뀐다. (3) `WorldSection.test.tsx` 의 `:179` describe 이름이 `TragicEvents` 블록이 있는 현재 동작과 맞고(예: `WorldSection keeps tragic events out of the event list (T2 (5))`), `:145` 가 미디어 없는 응답을 말한다(예: `WorldSection with an answer without media (T2 (3))`). (4) 두 파일의 `it`/`describe` 개수와 본문(단언)은 그대로, 이 PR 전후 `npm test` 의 두 파일 시험 수가 같다. (5) `npm run lint && npm run format:check && npm run typecheck && npm test` 성공 |
| T7 | 완료 기준 검증과 전체 시험 | test-engineer | T1~T6 | (1) T1~T6 기준을 표로 확인. (2) `cd server && ./gradlew check` 통과 2회, `MediaNormalizerLogTest`·`WorldStoryYoutubeLogTest`(사생활 시험) 는 3회 반복, 통과·실패 개수 보고. (3) `cd web && npm run lint && npm run format:check && npm run typecheck && npm test` 통과, 개수 보고. (4) 시험이 하나도 지워지지 않았다: C8 출력 없음. (5) S1·S2 보고. (6) PR을 연 뒤 오케스트레이터가 `gh pr checks` 로 `server`·`web` 잡이 이 PR에서 돌고 통과했는지 본다. 이 PR은 워크플로 파일 자체를 바꿔 `server-ci` 가 돈다. **새 `data/**`·`types.ts` 경로가 실제로 도는지는 이 PR로 증명되지 않는다**(YAML 내용만 시험). 첫 데이터 전용 PR이나 `types.ts` 전용 PR에서 확인한다 |
| T8 | (결정 D3 (b)일 때만) `TwoPageKoWikiEventSourceTest` 흔들림 진단 | test-engineer | - (T7과 별개) | (1) 이 클래스만 `./gradlew test --tests '*TwoPageKoWikiEventSourceTest' --rerun-tasks` 로 30회 반복하고, 한 번은 CPU가 바쁜 상태(다른 Gradle 작업 동시 실행)에서 반복한다. 반복 횟수·실패 횟수·실패한 단언과 메시지를 보고한다. (2) 원인 가설 둘(실제 시간 예산: 읽기 2초·하루 5초, 첫 요청 준비 시간 / 요청 순서·상태 공유)에 대해 어느 쪽인지 근거와 함께 쓴다. (3) **코드를 고치지 않는다.** 원인이 시간 예산이면 "예산만 넓히는 시험 설정 변경 제안"(단언은 그대로)을, 아니면 다음 단계를 보고에 쓴다. 30회 모두 통과하면 그렇게 보고하고 원인 미상으로 닫는다 |

담당별: backend-dev 4(T1~T4), frontend-dev 2(T5·T6), test-engineer 1(T7) + 선택 1(T8). 합계 7개(선택 포함 8개). ★ 는 T2.

완료 기준에 쓰는 검사 명령 (저장소 루트):

```
# C1 — server-ci 두 트리거에 data/** 경로. 2
grep -c '"data/\*\*"' .github/workflows/server-ci.yml

# C2 — server-ci 두 트리거에 types.ts 경로. 2
grep -c '"web/src/media/types.ts"' .github/workflows/server-ci.yml

# C3 — MediaNormalizer 의 Media dropped 줄이 record.id() 를 쓰지 않는지. 출력이 없어야 한다
grep -n 'Media dropped' server/src/main/java/io/github/ericmschoi/theworldthatday/media/MediaNormalizer.java | grep 'record.id()'

# C4 — SearchCachePurgeStartupTest 가 toString() 으로 찾지 않는지. 출력이 없어야 한다
grep -n 'toString()' server/src/test/java/io/github/ericmschoi/theworldthatday/media/video/SearchCachePurgeStartupTest.java

# C5 — 위키백과 어댑터가 error 를 직접 읽지 않는지. 출력이 없어야 한다
grep -n 'path("error")' server/src/main/java/io/github/ericmschoi/theworldthatday/event/kowiki/*.java

# C6 — 웹 mock 에 소수 점수가 없는지. 출력이 없어야 한다
grep -rnE 'score: *[0-9]+\.[0-9]' web/src

# C7 — 낡은 이름이 남아 있지 않은지. 출력이 없어야 한다
grep -nE 'and the one to come|leaves tragic events to M3b2b|today’s server shape' web/src/privacy/privacy.test.ts web/src/world/WorldSection.test.tsx

# C8 — 서버 시험 메서드 표시가 지워지지 않았는지. 출력이 없어야 한다
git diff -U0 main...HEAD -- server/src/test | grep -E '^-[[:space:]]*@(Test|ParameterizedTest)'

# S1 — 제품 코드·설정 줄 수 (테스트·mock 제외)
git diff --numstat main...HEAD -- server .github/workflows/server-ci.yml web/src \
  ':(exclude,glob)server/src/test/**' ':(exclude,glob)web/src/**/*.test.ts' \
  ':(exclude,glob)web/src/**/*.test.tsx' ':(exclude,glob)web/src/test/**' \
  ':(exclude,glob)web/src/mocks/**' | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'

# S2 — 테스트 줄 수 (따로 보고)
git diff --numstat main...HEAD -- server/src/test web/src | grep -E '\.test\.tsx?|src/test/|src/mocks/|server/src/test' | awk '{a+=$1; d+=$2} END {print "test added", a, "deleted", d}'
```

## 바뀌는 기존 단언 (약해지는 것은 없다)

`CLAUDE.md` 5절에 따라 시험을 지우거나 건너뛰지 않는다. 아래 외의 기존 단언은 고치지 않는다.

| 시험 | 바뀌는 것 | 왜 약해지지 않는가 |
|---|---|---|
| `ServerCiWorkflowTest.PATHS` | 4개 → 6개 | 같은 목록 동일성 단언(`isEqualTo`)이 더 엄격하게 6개를 요구한다 |
| `MediaNormalizerLogTest` (`:40`) | `.contains("Media dropped", "example-image", "R1")` → `.contains("Media dropped: source=example rule=R1")` 와 `.doesNotContain("example-image")` | 요구 자체가 "ID를 남기지 않는다"여서 ID가 있어야 한다는 단언은 반대로 뒤집힌다. 날짜·장소 `doesNotContain` 은 그대로 |
| `WorldStoryYoutubeLogTest` (`ACCEPTED_ID_LINE`, `idLines` 블록, 이름, javadoc) | "영상 ID는 허용된 DEBUG 줄 하나에만" → "영상 ID가 어떤 줄에도 없음" | 허용 예외가 사라져 더 엄격해진다. `doesNotContain` 목록과 실패 흐름 단언은 그대로 |
| `SearchCachePurgeStartupTest.itRunsAgainEveryHour` | 작업 고르는 조건만 `toString()` → `ScheduledMethodRunnable` | `singleElement()`·간격·첫 지연 단언은 그대로 |
| `privacy.test.ts`, `WorldSection.test.tsx` | `it`/`describe` 이름·주석 | 본문과 개수는 그대로 |
| `youtube.ts` 의 mock 값 | `0.9` → `90` | 시험 입력이 서버 계약에 맞아진다. 이 값을 읽는 기존 시험(`YouTubePlayer.test`, `worldYoutube.test`)은 `selection` 을 단언하지 않아 영향 없음 |

## 병렬 실행

서버 쪽(T1~T4: `server/`, `.github/workflows/`)과 웹 쪽(T5·T6: `web/`)은 폴더가 겹치지 않아 **병렬 worktree가 가능**하다. 서버 T1→T2→T3→T4는 같은 담당이라 차례로 한다(T1이 먼저, PM 지시). 웹 두 작업은 합쳐 20줄 안팎이라 worktree를 만드는 비용이 더 클 수 있다. 추천: 웹 T5·T6을 별도 worktree에서 서버와 동시에 하고(PM의 병렬 선호, 충돌 가능성 0), 오케스트레이터가 합친 뒤 T7. 차례로 해도 결과는 같다.

## 크기 (400줄 규칙)

세는 법은 `chore-m1-followups` 와 같다(제품 코드·설정은 세고, 테스트와 `web/src/mocks` 는 따로 보고). 최근 실측 비율은 M3a5a ×2.76(파일별 셈 대비)이다.

| 작업 | 파일별 셈 (S1) | ×2.76 | 비고 |
|---|---|---|---|
| T1 | 설정 4 | 4 | 설정이라 곱하지 않음 |
| T2 | 약 9 / −1 | 약 25 | 접두어 도우미 + 로그 줄 |
| T3 | 0 | 0 | 시험만 |
| T4 | 약 14 / −8 (+ `ActionApiErrors` 도우미 약 4) | 약 50 | 두 어댑터 |
| T5·T6 | 0 | 0 | mock·시험 이름 |
| **합계** | 약 27 + 설정 4 | **약 80 (범위 50~110)** | 400의 약 20%, 확인점 불필요 |

시험(S2)은 따로 센다: T1 약 4, T2 약 25~30, T3 약 6, T4 약 35~40, T5 약 10, T6 약 6 → 파일별 셈 약 90, 시험은 비율이 더 커서 **약 150~200줄**을 예상한다. 합쳐도 PR 하나로 충분히 작다. 확인점은 두지 않는다. S1이 150을 넘으면 T7 전에 오케스트레이터가 멈추고 PM에게 알린다.

## 파일 소유 예외

backend-dev가 `server/` 밖의 `.github/workflows/server-ci.yml` 한 파일을 고친다. `chore/m1-followups`(결정 M3-P2)와 같은 파일 단위 예외를 이 파일에 한 번 더 적용하는 것을 PM이 승인한다(결정 D5). 웹은 frontend-dev가 `web/` 안에서만 고친다.

## 리스크와 확인할 것

1. **경로 필터는 PR에서 증명되지 않는다** — 워크플로 시험은 YAML 내용만 본다. 이 PR은 워크플로를 바꿔 `server-ci` 가 돌지만 새 경로의 효과는 보이지 않는다. 첫 데이터 전용·`types.ts` 전용 PR에서 `gh pr checks` 로 `server` 잡이 도는지 본다(T7 (6)).
2. **데이터 전용 PR에서 서버 잡이 최대 20분 돈다** — 허용한다(계약 깨짐을 못 잡는 것보다 낫다). `data/**` 전체라 `data/` 아래 문서 수정에도 돈다. 불편하면 나중에 확장자로 좁힌다(지금은 서버 시험이 `data/` 아래 무엇을 읽는지 하나씩 따라가기보다 안전한 쪽을 택한다).
3. **T2의 디버깅 정보 손실** — 버려진 항목의 ID로 바로 찾을 수 없다. 출처 접두어와 규칙은 남고, 버림 원인은 시험 fixture로 재현한다. 공개 배포 전에는 이 쪽이 필요하다(M3a5a T4 결정 2 (a)).
4. **T4는 실패 로그 문구가 조금 달라질 수 있다** — 정상 코드는 그대로이고, 코드 없는 `error` 객체는 예전에는 "no revision"(문서)·"no query"(링크 조회) 로 실패했다가 이제 `api error` 로 실패한다. 위키백과 사건 어댑터 시험을 찾아본 범위에서는 `no revision` 문구를 단언하는 곳이 없다(`api error` 단언은 `ActionApiKoWikiClientTest:232` 의 `ratelimited` 한 곳이고 그대로 통과해야 한다). 다른 실패 경로는 같다.
5. **T8은 원인을 못 찾을 수 있다** — 30회 모두 통과하면 "재현 안 됨"으로 보고하고, 고치지 않고 닫는다. 흔들림을 숨기려고 재시도·건너뛰기를 넣지 않는다.
6. **`ScheduledMethodRunnable` 이 다른 래퍼에 싸일 수 있다** — Spring 구현이 바뀌어 `getRunnable()` 이 다른 래퍼를 줄 수 있다. T3은 `singleElement()` 를 유지하므로 못 찾으면 실패로 드러난다(조용히 통과하지 않음). 그때는 래퍼를 풀어 비교하게 고친다.

## PM 결정 필요

**PM 결정 (2026-10-27)**: D1 **(a)**, D2 **(a)**, D3 **(b)**, D4 **(c)** (★ T2는 backend-dev), D5 **(a)**.

| # | 질문 | 선택지 | 추천 |
|---|---|---|---|
| D1 | 이 정리의 범위 | (a) T1~T7: 경로·DEBUG ID·`toString`·위키백과 `ActionApiErrors`·이름·mock / (b) T4(위키백과 `ActionApiErrors`)만 빼고 나머지 / (c) T1·T2만 | (a). 근거: S1 약 80으로 400의 20%, 모두 동작이 안 바뀌거나 안전 쪽 변경이고, T4는 미디어 어댑터와 로그 규칙이 같아지는 가치가 있다. 뺀 것은 5·7·11(표 참고) |
| D2 | T2 로그 줄에 무엇을 남기나 | (a) `source=<접두어> rule=<규칙>` / (b) `rule=<규칙>` 만 / (c) 지금처럼 ID를 두고 공개 배포 때로 미룸 | (a). 근거: 영상 ID는 빠지고 어느 출처가 어느 규칙으로 버려지는지는 보인다. (c)는 이미 있는 예외를 계속 시험 주석에 끌고 가고 공개 배포 직전에 잊을 수 있다 |
| D3 | 흔들리는 `TwoPageKoWikiEventSourceTest` | (a) 이번에는 안 함 / (b) T8로 진단만(코드 고침 없음, 보고) | (b). 근거: 클래스 하나를 30회 돌리는 값싼 일이고 M3a5b가 PM T0를 기다리는 동안 한다. 원인을 모르는 채 고치지 않는다. 시간 예산이 원인이면 별도 작은 시험 설정 변경을 이 PR에 더할지 그때 다시 묻는다 |
| D4 | ★ 이 PR에서 PM이 직접 구현할 작업 | (a) T2 / (b) T4 / (c) 안 함 | (a) T2. 근거: 범위가 작고(제품 코드 약 10줄 + 시험), "로그에 무엇을 남기고 무엇을 남기지 않나"라는 사생활 설계 판단과 `CapturedOutput` 로그 시험 작성을 함께 배운다. (b)는 JSON 오류 처리 리팩터링이라 배울 것은 있지만 두 어댑터에 걸쳐 더 크다 |
| D5 | `.github/workflows/server-ci.yml` 파일 단위 소유 예외 | (a) 승인(backend-dev가 이 한 파일 수정) / (b) 오케스트레이터가 직접 고침 | (a). `chore/m1-followups` 와 같은 방식 |

## 끝나면 PM이 할 것

- 없음. (다음 데이터 전용 PR에서 `server` 잡이 도는지 눈으로 한 번 본다.)
