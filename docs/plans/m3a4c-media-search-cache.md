# 영상 검색 결과 캐시 (M3a4c)

> 브랜치: `feat/media-search-cache` (`main` 9203562 — PR #24 M3a4b 머지 뒤 — 에서 갈라짐) · 작성: planner · 상태: **PM 승인 (2026-10-24): 크기 결정 1 (a) 한 PR, 확인점 T1 뒤 300·T3 뒤 700(둘 다 알림) — M3a4b 결정 1 (a)로 분리**. M3a4b 계획서(`docs/plans/m3a4b-commons-video.md`)의 "B — 결과 캐시"(그 계획서의 T6~T9)를 떼어 내 머지된 코드에 맞게 다시 썼다. M3a4b 계획서는 기록으로 그대로 둔다.
> 이미 정해진 것(다시 묻지 않음, 2026-10-24): 분할 (a) — 결과 캐시는 이 PR, 확인점 T1 뒤 300(알림), ★ 저장소 작업은 backend-dev(결정 3 (b)), M3 PR 22개.
> 기준: `docs/research/video-sources.md` 공통-4(캐싱·저장), 종합 경로 표의 "캐시 허용" 열, 7절 R-V1~R-V7, 8절 위험 2·4·5·6, `docs/plans/m3a4b-commons-video.md`("결과 캐시 (Flyway V3)", 리스크 8·9), `docs/plans/m3a4-event-video.md`(검색 틀), `docs/plans/m3a1b-wiki-section-cache.md`(캐시 방식), `CLAUDE.md` 7절 1·2·3·5·6항, 제품 정의서 6.1·6.3·12절
> 선행: **M3a4b 머지됨 (PR #24, `main` 9203562)** — `CommonsVideoSource`(key `commons`)가 `VideoSearch` 에 붙어 있고, 새 날짜의 첫 호출은 Wikidata 1 + Commons 영상 최대 5개 요청이다. **키가 필요 없다.**
> 뒤따르는 기능: M3a5(YouTube)가 이 캐시를 쓴다(`media.search-cache.ttl.youtube` 를 더하고 30일 상한을 검사).

## 머지된 코드와 맞춘 것 (M3a4b 계획서 B절과 달라진 점)

| 항목 | M3a4b 계획서 B절 | 머지 코드(`main` 9203562) 확인 결과 → 이 계획 |
|---|---|---|
| 저장하는 후보 | "판정 전 후보 + 필터 입력(`MediaOrigin`)" | `Candidate.item` 은 어댑터가 이미 `MediaFilter`·`MediaNormalizer` 를 거쳐 만든 `MediaItem` 이고, **`MediaOrigin`(파일 이름·작가·크레디트)은 `Candidate` 에 없다** — 매퍼 안에서 쓰고 버려진다. 읽을 때 차단 목록을 다시 적용하려면 원본 입력이 필요하다 → `Candidate` 에 `MediaOrigin origin`(널 허용) 열 번째 요소를 더하고 **9개 인자 생성자를 남긴다**(기존 `Candidate(` 사용 35곳이 고치지 않고 컴파일). `CommonsFileMapper.VideoResult` 가 `origin` 을 함께 돌려주고 `CommonsVideoSource.add` 가 넘긴다. origin 이 없는 후보는 저장하지 않는다(안전한 쪽) — 작업 T2 |
| 저장 단위 | `MediaItem` 또는 레코드 | `MediaItem` 은 `@JsonUnwrapped record` + `dateRelation` + `selection`(API 전용 값). 저장은 **`MediaRecord`**(정규화를 거친 것)와 판정 재료만. `dateRelation` 은 줄의 `event_date` 로 읽을 때 다시 계산, `selection` 은 판정 뒤 붙으므로 저장 안 함. `retrieved_at` 은 레코드 안에 있어 처음 조회 시각이 그대로 남는다 |
| 판정 자리 | `VideoSearch` 가 판정 전 후보를 받음 | 맞다: 판정은 `Running.await()` 안 `judged(key, result)`(`accuracy.forEvent(label 있으면 event.withEnglishLabel(label))` → `EventJudge.judge(Candidate)`), 요청 스레드에서 돈다. 읽은 후보를 같은 `judged` 에 넣으면 `min-score`·낱말 목록 변경이 캐시 줄 그대로 따라간다 |
| 읽는 자리 | "`VideoSearch` 가 출처마다 캐시를 본다" | `VideoSearch.start(Deadline, date, targets, dayPathsOff)` 안, `deadline.start("video-" + key, …)` **앞**, 요청 스레드. 마감 작업 안에서 읽으면 마감 인터럽트가 JDBC를 끊는다(M3a4b 리스크 9). 사진 두 경로는 `CommonsMediaLookup.lookup` 이 이미 시작한 뒤라 읽기가 사진을 늦추지 않는다 |
| 쓰는 자리 | `Running.await()` 뒤 | `Running.await()` 안, 출처 결과를 판정한 직후, 요청 스레드. `Task.await()` 결과 `Done(SourceResult)` 의 `Status.OK` 만 쓴다 — `TimedOut(stopped, late)` 의 `late` 가 `OK` 여도 쓰지 않는다 |
| 요청 슬롯 | (없던 내용) | 943244e의 `WikimediaHttpClient.Lane`(`DEFAULT`/`PHOTO`/`VIDEO`)·`openPhotoPath()`: 영상 요청은 열려 있는 사진 경로마다 슬롯 하나를 남긴다. 캐시가 맞으면 영상 요청이 0이라 슬롯을 쓰지 않는다. **정책은 바꾸지 않는다**(리스크 2) |
| 한 창 | 같음 | `CommonsMediaLookup.lookup` 의 `try (Deadline deadline = Deadline.after(properties.lookupDeadline()))` 하나. 캐시 읽기는 이 창 안에서 `start` 가 부르고, 영상 작업은 같은 끝 시각에 끝난다 — 세계 섹션 최악 시간은 늘지 않는다. DB가 죽었을 때 읽기 한 번(Hikari `connection-timeout` 1s)은 창 안에서 쓰인다(리스크 3) |
| Flyway·검증 | V3, `ddl-auto=validate` | `db/migration` 에 `V1__baseline.sql`, `V2__wiki_section_cache.sql` 만 → **V3 확인**. `application.yml` `spring.jpa.hibernate.ddl-auto: validate` 확인 |
| 시험 기반 | "캐시 비우기 준비 코드" | `TRUNCATE wiki_section_cache` 가 17개 시험 클래스에 있고 그중 12개가 `story/world` 다. 새 테이블을 모두에 넣는 대신 **공용 기반 `WikimediaWireMockTest` 의 `@DynamicPropertySource` 에서 `media.search-cache.ttl.commons` 를 끄고**(0), 캐시 시험만 따로 켠다 — 기존 시험의 준비·단언을 건드리지 않는다 |

## 목표

1. 출처 공통 **결과 캐시**(Flyway V3 `event_media_search_cache`)를 `VideoSearch` 에 붙인다: 출처별 TTL(`commons: 7d`), 0건도 캐시, 실패·시간 초과·쿼터 건너뜀은 캐시 안 함, 읽을 때 다시 판정하고 다시 거른다.
2. 같은 날짜의 두 번째 요청에서 Wikidata·Commons 영상 요청이 0이 되고 응답이 같다.
3. PR #24 리뷰 권장 중 시험·주석 수준의 것(M3a4b 첫 리뷰 권장 1·3·4, 고침 리뷰 권장 1·4)을 닫는다(T4).

## 범위 밖

- 새 영상 출처(YouTube·대한뉴스 → M3a5·M3a5b). 이 캐시가 쓰일 `youtube` TTL 줄과 30일 상한 검사는 M3a5
- 영상 슬롯 정책 바꾸기(영상은 사진 두 경로가 열려 있는 동안 슬롯 1개만 쓴다 — M3a4b 첫 리뷰 권장 2, 고침 리뷰 권장 2). 새 정책을 설계하지 않는다 — 리스크 2
- 서버 전체 시간 초과 계수기(고침 리뷰 권장 3) → M6b
- 사진 메타데이터 캐시(로드맵 후속 후보 — 이 캐시는 영상 출처만)
- 오래된 캐시 줄 정리 작업(만료 줄은 다음 쓰기에서 덮어쓴다). 파일·포스터 저장(안전장치 1)
- 다른 날짜로 영상을 찾아 보이게 하는 것(PM 피드백 2026-10-16)
- 보조 경로 (b-2)·(b-3), `deepcat:` — M3a4b 결정 4 (a) 그대로 후속 후보

## 사용할 외부 출처

새로 쓰는 외부 호출은 없다. 이미 쓰는 Commons Action API·Wikidata Query Service의 응답을 **저장하는 규칙**만 더한다.

| 출처 | 확신도 | 쓰는 것 | 지킬 조건 |
|---|---|---|---|
| Wikimedia Commons Action API ([video-sources.md](../research/video-sources.md) 공통-4, 종합 경로 표 "캐시 허용") | **확인됨** | 영상 검색 결과(후보 메타데이터)의 DB 저장 | 파일은 복사하지 않는다(안전장치 1) — 파일 바이트·포스터는 저장하지 않고 주소만. API 예절 문서가 응답 캐시를 권장한다(day-events.md [19]). 파일 사용 조건은 파일 라이선스(레코드의 `license`·`attribution` 그대로 저장). 리서치 문구는 "검색 결과(파일 이름·점수)"이고 이 계획은 화면에 쓰는 메타데이터 레코드(주소·라이선스·표기·설명 200자)까지 저장한다 — M3a4b 계획서가 승인한 범위이며 리스크 7에 적어 리뷰가 다시 본다 |
| Wikidata Query Service (같은 표 "Commons (a-3) 식별자·점수 캐시 가능") | **확인됨** | `P10`·`P373`·`P910`·영어 라벨 질의의 결과 중 영어 라벨만 사건별로 저장(`SourceResult.englishLabels`) | CC0. 파일 이름과 범주 이름은 후보 안에 이미 들어 있다 |

YouTube의 "최대 30일"(V2-9, III.E.4.d)은 이 기능과 무관하다. M3a5가 `ttl.youtube` 를 더할 때 검사한다.

## 이어받는 계약 (`main` 9203562에서 확인한 이름)

| 부품 | 실제 이름·모양 | 이 기능에서 |
|---|---|---|
| 출처 | `media.video.EventVideoSource`: `key()`, `SourceResult find(LocalDate, List<EventContext>, boolean dayPathsOff)`. `record SourceResult(byEvent, englishLabels, status)` — 안쪽 목록까지 복사, `null` 원소 거부. `enum Status { OK, FAILED, QUOTA_SKIPPED }` | 캐시가 `Status.OK` 만 저장. 없는 사건만 `find` 에 넘긴다 |
| 검색 | `media.video.VideoSearch`: `Running start(Deadline, LocalDate, List<EventTarget>, boolean dayPathsOff)`(앞 `media.video.max-events` 개를 `EventContext.of(target, date)`, 출처마다 `deadline.start("video-" + key, …)`), `Running.await()` → `Videos(Map<String, MediaItem> byEvent, boolean failed)`. 생성자 둘(`ObjectProvider<EventVideoSource>` `@Autowired` / `List<EventVideoSource>`) | 읽기는 `start`, 쓰기는 `await`. 시험이 쓰는 `List` 생성자는 캐시 없이 그대로 동작 |
| 후보 | `media.video.Candidate(item, title, description, originalCapture, explicitDates, eventLink, official, dateTextExcluded, basis)` + 새 `origin`(T2). 생성 때 `originalCapture` 모양(R8)·`null` 날짜 검사 | 되살릴 때 이 검사가 읽을 수 없는 줄을 걸러 준다 |
| 매퍼 | `media.commons.CommonsFileMapper.mapVideo(JsonNode, placeRef, date)` → `VideoResult(item, drop, title, description, originalCapture)`. 안에서 `MediaFilter.check(record, new MediaOrigin("commons", fileName, artist, credit, null))` → `MediaNormalizer.normalize(record, date)` | `VideoResult` 에 `origin` 을 더한다 |
| 규칙 | `media.MediaFilter.check(MediaRecord, MediaOrigin)` → `Optional<Reason>`, `media.MediaNormalizer.normalize(MediaRecord, LocalDate)` → `Optional<MediaItem>`(R1~R9, `date_relation`) | 읽을 때 다시 부른다 |
| 마감 | `wikimedia.Deadline`(`start`, `Task.await()` → `Done`/`TimedOut`/`Failed`/`Interrupted`), `CANCEL_GRACE` 1초 | 캐시 읽기·쓰기는 이 작업 밖 |
| 슬롯 | `wikimedia.WikimediaHttpClient`: `Lane { DEFAULT, PHOTO, VIDEO }`, `openPhotoPath()` → `PhotoPath`, 공정 `Semaphore(wikimedia.max-concurrent-requests = 3)` | 건드리지 않는다(T4는 주석 한 줄) |
| 캐시 선례 | `V2__wiki_section_cache.sql`, `WikiSectionCache`, `WikiSectionCacheRepository.upsert`(`ON CONFLICT … DO UPDATE`), `CachingKoWikiClient`(신선 = `fetched_at + ttl > now`, 읽기 실패 뒤 같은 호출의 쓰기 건너뜀, 로그에 제목만) | 같은 방식. `Clock` 빈 사용 |
| 설정 | `media.*` = `MediaProperties`·`MediaLookupProperties`(`@ConfigurationProperties("media")`, 알 수 없는 하위 키는 무시), `media.video.*` = `VideoSearchProperties`, `hikari.connection-timeout: 1000` | 새 `media.search-cache.ttl.<출처>` |
| 시험 기반 | `WikimediaWireMockTest`(`@SpringBootTest`, `@DynamicPropertySource`로 URL·읽기 타임아웃 설정), `MutableClock`, `FakeVideoSource`, `TRUNCATE wiki_section_cache` 17개 클래스 | T3에서 기반 클래스에 캐시 끔 |

## 결과 캐시 (Flyway V3)

```sql
-- 예시 (이름·타입은 T1에서 정하고 보고)
CREATE TABLE event_media_search_cache (
    id            BIGSERIAL    PRIMARY KEY,
    source        VARCHAR(32)  NOT NULL,   -- 'commons' (뒤에 'youtube', 'daehan_news')
    event_date    DATE         NOT NULL,
    event_id      VARCHAR(64)  NOT NULL,   -- 'kowiki-<날짜>-<8자리>' (M3a1c)
    day_paths_off BOOLEAN      NOT NULL,   -- 날짜 단위 경로를 껐는지 (같은 날도 결과가 다름)
    result        TEXT         NOT NULL,   -- 판정 전 후보 + 필터 입력 + 영어 라벨 (JSON 글). 메타데이터만, 파일 아님
    fetched_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_event_media_search_cache UNIQUE (source, event_date, event_id, day_paths_off)
);
```

- **`result` 는 `TEXT` 를 추천**한다: JSON 내용으로 조회할 일이 없고, `JSONB` 는 Hibernate 매핑(`@JdbcTypeCode`)을 따로 맞춰야 `ddl-auto=validate` 를 통과한다. 고르는 것과 이유는 T1 보고서에 적는다.
- 줄 하나 = (출처, 날짜, 사건 ID, `dayPathsOff`). `result` 는 `{버전, englishLabel, candidates:[{record, origin, title, description, originalCapture, explicitDates, eventLink, official, dateTextExcluded, basis}]}`. `MediaItem`·`dateRelation`·`selection` 은 저장하지 않는다(읽을 때 다시 만든다). 날짜 범주 후보는 그날의 모든 대상 사건에 붙으므로 줄마다 되풀이 저장된다(줄 최대 3개).
- 신선함: 읽을 때 `fetched_at + media.search-cache.ttl.<출처> > clock.instant()`. 정확히 `fetched_at + ttl` 이면 오래된 것. 설정 `media.search-cache.ttl.commons: 7d`. 음수는 시작 실패(오류에 `media.search-cache.ttl.<출처>`), `0`·없는 출처는 "캐시 안 함"(읽기·쓰기·쿼리 모두 없음).
- **0건도 캐시**한다. **`FAILED`·`QUOTA_SKIPPED`·예외·시간 초과(`late` 가 `OK` 여도)·`Interrupted`·판정 예외는 캐시하지 않는다.**
- 일부 사건만 신선하면 없는 사건만 `find` 에 넘긴다. 모두 신선하면 그 출처 요청 0(작업도 시작하지 않는다).
- 읽을 때: `Candidate` 되살림(R8 검사 포함), `MediaFilter.check`(차단 목록·통신사 제외), `MediaNormalizer.normalize`(R1~R9, `date_relation`), 그다음 평소의 `judged`. 읽을 수 없는 줄(버전 모름, JSON 깨짐, 필드 빠짐, 검사 예외)은 **없는 것**으로 보고 다시 찾아 덮어쓴다.
- 쓰기는 마감 작업 밖(요청 스레드)에서 — 마감 인터럽트가 JDBC를 끊지 않는다.
- DB 실패는 캐시만 잃는다: **한 번의 조회에서 읽기가 한 번 실패하면 같은 조회의 나머지 읽기와 모든 쓰기를 건너뛴다**(`CachingKoWikiClient` 의 "읽기 실패 뒤 쓰기 안 함"과 같은 이유 — DB가 죽었을 때 연결 대기를 한 번만 치른다). 영상 검색 결과와 `partial` 은 그대로, WARN 한 줄.
- 요청한 사람 정보는 저장하지 않는다(출처·날짜·사건·결과·시각뿐, `CLAUDE.md` 7절 6항). 로그에는 날짜·사건 ID·파일 이름·QID가 없다.

## API / 데이터 변경

- `GET /api/story/world`: **요청·응답 모양 변화 없음.** 캐시에서 되살린 응답은 첫 응답과 같다(`retrieved_at` 도 처음 조회 시각 그대로). 데모 3날짜는 Commons 영상이 없어(0건) 응답이 지금과 같다.
- 설정: `media.search-cache.ttl.commons: 7d` (`application.yml`, 주석 포함 — T1). `.env.example` 변경 없음.
- DB: V3 `event_media_search_cache` (T1).
- 내부 형식: `Candidate` 에 `MediaOrigin origin`(널 허용) 열 번째 요소 + 9개 인자 생성자 유지, `CommonsFileMapper.VideoResult` 에 `origin` (T2). 외부로 나가는 JSON은 바뀌지 않는다.
- `data/` 변경 없음.

## 작업 목록

순서: T1 ★ → (확인점 1) → T2 → T3 → (확인점 2) → T4 → 리뷰 → PR → T5 → T6(PR 검수 때) → 머지. T4는 T1~T3과 의존이 없지만 같은 담당이므로 T3 뒤에 한다(T3이 `WikimediaWireMockTest` 를 고치고 T4가 `WorldStoryEventPhotoPartsRegressionTest` 를 고쳐 겹침을 피함).

M3a4b 계획서 B절과의 대응: 옛 T6 → **T1**, 옛 T7 → **T2 + T3**(저장 형식·origin / 흐름으로 나눔), 옛 T8 → **T5**, 옛 T9 → **T6**. T4는 PR #24 리뷰 이월(새로).

| ID | 작업 | 담당 | 의존 | 완료 기준 |
|----|------|------|------|-----------|
| T1 | ★ 캐시 저장소: Flyway V3·엔티티·저장소·TTL 설정 (`feat(db)` 커밋) (옛 T6) | backend-dev (PM 결정 3 (b)) | - | (1) `V3__event_media_search_cache.sql` 이 적용되고 `ddl-auto=validate` 로 컨텍스트가 뜬다(Testcontainers). (2) 열은 "결과 캐시" 절의 것뿐 — `information_schema.columns` 로 열 이름 목록이 `id, source, event_date, event_id, day_paths_off, result, fetched_at` 와 같음을 단언(요청자 정보·IP·장소 없음). `result` 타입(`TEXT` 추천, `JSONB` 면 매핑)과 이유는 보고서에. (3) 유일 키 (출처, 날짜, 사건 ID, `day_paths_off`). `upsert` 는 같은 키를 덮어써 `result`·`fetched_at` 을 바꾸고, 두 스레드가 같은 키를 동시에 써도 예외 없이 줄 하나만 남는다(V2 `upsert` 시험과 같은 방식). 조회 메서드는 (출처, 날짜, `day_paths_off`, 사건 ID 목록)으로 줄을 돌려준다. (4) `media.search-cache.ttl` 지도를 읽는 설정 클래스(예 `SearchCacheProperties`, `@ConfigurationProperties("media.search-cache")`): `application.yml` 에 `commons: 7d` + 주석. 음수는 시작 실패하고 오류 글에 `media.search-cache.ttl.commons`, `0` 과 지도에 없는 출처는 "캐시 안 함"(`ttl("x")` 가 빈 값). `media.search-cache` 키가 `MediaProperties`·`MediaLookupProperties` 바인딩을 깨지 않음(기존 `media.*` 설정 시험 그대로 통과). (5) 아직 흐름에 연결하지 않는다 → `VideoSearch` 와 응답·요청 수 그대로. (6) `cd server && ./gradlew check` 성공. 보고서에 누적 S1 — **확인점 1 (300 넘으면 알림)** |
| T2 | 저장 내용: `Candidate.origin`·후보 저장 형식·되살림 (`feat(media)` 커밋) (옛 T7 앞부분) | backend-dev | T1 | (1) `Candidate` 가 `MediaOrigin origin`(널 허용) 열 번째 요소를 갖고 9개 인자 생성자가 남는다(origin `null`): `CandidateTest`·`VideoAccuracy*Test`·`VideoSearch*Test`·`WorldStoryScoreTableTest` 등 기존 `Candidate(` 사용이 고치지 않고 통과. `CommonsFileMapper.VideoResult` 에 `origin` 이 더해지고 `CommonsVideoSource` 가 후보에 넘긴다 — 녹화 1969-07-20 후보의 origin 은 출처 `commons`, 항목 ID 는 `File:` 뗀 파일 이름. `CommonsFileMapperTest`·`CommonsVideoSourceTest`·`CommonsRecordedDaysTest` 의 기존 단언 그대로 통과(`VideoResult` 생성자 호출이 있으면 그 준비 줄만 바꾸고 보고). (2) 후보 저장 형식(버전 번호 + 영어 라벨 + 후보 목록, 후보마다 `MediaRecord`·`origin`·`title`·`description`·`originalCapture`·`explicitDates`·`eventLink`·`official`·`dateTextExcluded`·`basis`; 파일 바이트·`MediaItem`·`selection` 없음)을 글로 쓰고 읽는다. 왕복: 녹화 아폴로 11호 후보(`P10`·범주·날짜 범주 세 경로)와 합성 후보(설명 없음·`explicitDates` 비어 있음·`UNKNOWN` 촬영 시점)가 `equals` 로 같다. (3) 되살릴 때 `MediaFilter.check(record, origin)` 와 `MediaNormalizer.normalize(record, 줄의 날짜)` 를 다시 거친다: 저장 뒤 `media.blocklist` 에 그 파일(`commons` + 파일 이름)을 넣으면 되살린 후보에서 빠지고, 작가 `Associated Press` 인 후보도 빠지고, R1~R9 중 하나를 깨는 합성 레코드도 빠진다. 통과한 후보의 `retrieved_at` 은 저장된 값, `date_relation` 은 줄의 날짜 기준 값. (4) 읽을 수 없는 입력 — 모르는 버전, 깨진 JSON, 필드 빠짐, R8 깨진 촬영 시점(`Candidate` 생성자 예외) — 은 예외를 밖으로 던지지 않고 "읽을 수 없음"으로 돌려준다(그 줄은 없는 것). 로그에 내용 없음. (5) origin 이 `null` 인 후보가 하나라도 있는 사건은 쓰지 않는다고 형식 부품이 알려 준다(쓰기 거부 결과). (6) 아직 흐름에 연결하지 않는다 → 응답·요청 수 그대로. (7) `./gradlew check` 성공. 보고서에 누적 S1 |
| T3 | 캐시 흐름: `VideoSearch` 읽기·합치기·쓰기와 실패 처리 (`feat(media)` 커밋) (옛 T7 뒷부분) | backend-dev | T2 | (1) `@Autowired` 생성자에 캐시 부품이 들어가고 `List` 생성자(시험용)는 캐시 없이 동작한다. 캐시를 쓰는 출처는 T1 설정에서 TTL이 0보다 큰 것만. (2) **읽기** — `start(...)` 안, `deadline.start` 앞, 호출한 스레드에서 출처마다 한 번: 신선한 사건의 후보를 되살리고, 없는 사건만 `find(date, 없는 사건만, dayPathsOff)` 에 넘긴다. 모두 신선하면 그 출처 작업을 시작하지 않고 `find` 를 부르지 않는다(가짜 출처 호출 수 0). `fetched_at + ttl` 정확히면 다시 부르고 1초 전이면 안 부른다(`MutableClock`). (3) **합치기** — `await()` 에서 출처의 후보 = 되살린 것 + 새로 찾은 것, 영어 라벨도 합쳐 같은 `judged(key, 합친 결과)` 로 판정한다: 설정 `min-score` 를 바꾸면 같은 캐시 줄로 결과가 달라진다(시험), 출처 간 중복·사건 간 중복·`max-events` 규칙은 그대로. 새로 찾는 부분이 `FAILED`·시간 초과·예외·`Interrupted` 면 되살린 후보는 판정에 쓰고 `Videos.failed` 는 참, 아무것도 쓰지 않는다. (4) **쓰기** — `await()` 에서, 새로 찾은 `Done` + `Status.OK` 결과가 판정 예외 없이 끝난 뒤에만, 물은 사건마다 한 줄(후보 0건 포함)을 `upsert`. `FAILED`·`QUOTA_SKIPPED`·예외·`TimedOut`(`late` 가 `OK` 여도)·`Interrupted`·판정 예외는 쓰지 않는다(가짜 출처로 각각 단언). T2 (5)가 거부한 사건은 쓰지 않는다. (5) **DB 실패** — 읽기가 던지면(가짜 저장소) 그 조회의 나머지 읽기와 모든 쓰기를 건너뛰고 출처에 모든 사건을 묻는다, WARN 정확히 한 줄 `Media search cache read failed: source=<key> cause=<클래스>`. 읽기는 되고 쓰기가 던지면 결과는 그대로 쓰고 WARN `Media search cache write failed: source=<key> cause=<클래스>` (사건 수만큼 반복하지 않고 그 조회에서 출처당 한 줄). 어느 쪽도 `partial`·영상 결과를 바꾸지 않는다. 로그에 날짜·사건 ID·파일 이름·QID 없음(로그 캡처 시험). (6) **창 하나** — 읽기가 `lookup-deadline` 의 절반 동안 막혔다가 던지는 가짜 저장소에서도 미디어 구간이 `lookup-deadline` + `CANCEL_GRACE` + 0.5초 이하이고 사진 두 경로는 늦어지지 않는다. `WorldStoryEventPhotoPartsRegressionTest` 와 M3a4b의 시험이 단언을 바꾸지 않고 통과. (7) **세계 API 끝까지(캐시 켠 전용 시험 클래스)** — 합성 2000-02-29(영상 하나 붙는 날): 같은 날짜 두 번째 호출은 영상 요청(`isVideoSearch` 로 가르는 Wikidata·Commons 영상) 0이고 응답 JSON 이 같다(`retrieved_at` 포함) / 녹화 1988-09-17(0건)의 두 번째 호출도 영상 요청 0(0건 캐시) / Commons 503 또는 Wikidata 시간 초과 뒤에는 줄이 없고 다음 호출이 다시 부른다 / `dayPathsOff` 가 다른 호출(합성 `tragic` 사건이 있는 날과 없는 날)은 다른 줄 / TTL `0` 설정은 쿼리도 쓰기도 없다. (8) **기존 시험 보호** — `WikimediaWireMockTest` 의 `@DynamicPropertySource` 가 `media.search-cache.ttl.commons` 를 꺼서(0) 기존 `story/world` 시험은 캐시를 만나지 않고, 캐시 시험 클래스는 켜고 `@BeforeEach` 에서 `TRUNCATE event_media_search_cache`. 기존 시험의 단언은 바꾸지 않는다. 바꾼 준비 줄을 보고서에 모두 적는다. (9) 응답 어디에도 `link_only` 없음. (10) `./gradlew check` 성공. 보고서에 누적 S1 — **확인점 2 (700 넘으면 알림)** |
| T4 | PR #24 리뷰 이월: 시험 4개와 주석 1개 (`test(media)`·`docs(media)` 커밋) | backend-dev | T3 | (1) **첫 리뷰 권장 1** — `WorldStoryEventPhotoPartsRegressionTest` 의 `failures(...)`(현재 151줄 근처)가 영상 경로 WARN 줄을 `filter` 로 버리기만 하지 않고 따로 모아 단언한다: 줄마다 `Wikimedia call failed: source=… title=<고정 이름표> cause=…` 모양이고 `title` 이 `wikidata video links of <수>`·`commons video date category`·`commons video category search`·`commons video files of <수>` 넷 중 하나와 정확히 같고(`SparqlWikidataVideoLinks.LABEL`, `ActionApiCommonsVideoClient.DATE_CATEGORY`·`CATEGORY`·`FILES`), 줄 수가 그 시험에서 WireMock 이 받은 영상 요청 수(`isVideoSearch`) 이하. 사진 두 경로의 "경로당 WARN 한 줄" 단언은 그대로. (2) **첫 리뷰 권장 3** — `CommonsVideoSourceTest.withoutP10TheBestCategoryVideoWinsAndTheLaunchVideoNeverAttaches` 를 증명하는 바를 말하는 이름으로 바꾼다(예 `launchVideoPassesAt70ButLosesToTheBestCategoryVideoOnRank`). 단언은 바꾸지 않는다(발사 영상 점수 70 ≥ `min-score` 60, 최고 점수 130 에 순위에서 짐). (3) **첫 리뷰 권장 4** — `WorldStoryNoVideoSourceTest` 를 "출처 빈이 하나(key `commons`) + 1988-09-17 녹화 0건 응답" 이라는 지금 내용을 말하는 이름으로 바꾼다(`WorldStoryDemoNoVideoTest` 가 이미 있으니 겹치지 않게). 클래스 설명과 이 시험을 가리키는 `README.md`·주석이 있으면 함께 고친다. 단언은 바꾸지 않는다. (4) **고침 리뷰 권장 1** — `WikimediaHttpClientLanesTest` 의 `Thread.onSpinWait()` 반복 둘(현재 115줄·147줄 근처, 리뷰 때 107·141)이 끝나지 않으면 시험이 매달리지 않는다. `@Timeout` 은 인터럽트를 확인하지 않는 반복을 멈추지 못하므로 **끝 시각이 있는 반복**(예 5초 안에 `videoWaiting()` 이 안 되면 `fail`)으로 바꾸고, 이 클래스의 모든 `@Test` 에 `@Timeout` 도 단다. 단언은 바꾸지 않는다. (5) **고침 리뷰 권장 4** — `WikimediaHttpClient` 에 "열린 사진 경로마다 `PHOTO` 요청이 동시에 하나만 있다고 가정한다(`kept = photoPaths - photoInFlight`, 사진 경로 안 요청은 차례로 간다 — 날짜 범주 한 번 / PageImages 다음 파일 묶음)" 주석을 `photoInFlight` 또는 `acquirePermit` 의 `kept` 줄에 단다. 주석만, 코드 변화 없음. (6) 제품 코드 동작 변화 없음: S1 의 증가는 주석 줄뿐(보고서에 줄 수). `./gradlew check` 성공 |
| T5 | 완료 기준 검증과 전체 테스트 (옛 T8) | test-engineer | T4 | (1) T1~T4 기준 표(기준마다 테스트 이름). 빠진 기준은 테스트를 더한다(제품 코드는 고치지 않음). (2) `cd server && ./gradlew check` **2회** 반복 안정, 마감·슬롯·창 하나 시험이 든 클래스(`WikimediaHttpClientLanesTest`, `WorldStoryEventPhotoPartsRegressionTest`, 캐시 흐름·DB 실패 시험)는 **3회**. (3) S1·S2 보고(S3 없음). (4) 캐시 결과 표(데모 1988-09-17 / 합성 2000-02-29 — 첫 호출·두 번째 호출의 출처별 요청 수, TTL 경계, 실패 뒤 다시 부름, `dayPathsOff` 별 줄 수). (5) 새 시험 모두 WireMock 밖으로 요청하지 않음(`findAllUnmatchedRequests` 빈 목록). (6) `event_media_search_cache` 열에 요청자 정보가 없고, 로그(DEBUG 포함) 캡처에 날짜·사건 ID·파일 이름·QID 없음. (7) 기존 시험의 단언이 하나도 바뀌지 않았는지 `git diff main...HEAD -- server/src/test` 로 확인해 목록으로 보고(이름 바꾼 시험 둘과 T4의 의도된 변경만 허용) |
| T6 | PM 캐시 확인 (PR 검수 때) (옛 T9) | PM | T5 | (1) `docker compose up -d db`, `cd server && ./gradlew bootRun`(루트 `.env` 의 `WIKIMEDIA_USER_AGENT` 는 저장소 주소만). (2) 아래 P1로 1969-07-20(M3a4b T5에서 영상이 붙은 표본)을 **두 번** 부른다: 두 응답의 영상 항목이 같다(`retrieved_at` 포함). 두 번째 호출 때 서버 로그에 Wikimedia 영상 요청 줄이 늘지 않는다. (3) P2로 캐시 줄이 출처 `commons` 로 있고 열에 요청자 정보가 없다. (4) 다른 날짜로 찾아보지 않는다 |

```
# S1 — 제품 코드·설정 줄 수 (M3a4b와 같은 명령)
git diff --numstat main...HEAD -- server ':(exclude,glob)server/src/test/**' \
  | awk '{a+=$1; d+=$2} END {print "added", a, "deleted", d}'
# S2 — 테스트 줄 수
git diff --numstat main...HEAD -- server/src/test | awk '{a+=$1} END {print "test added", a}'

# P1 — (T6) 같은 날짜를 두 번 (서버 실행 중, 키 없음). 두 줄 출력이 같아야 한다
for i in 1 2; do
  curl -s "http://localhost:8080/api/story/world?date=1969-07-20" \
    | jq -c '{partial, events: [.events[] | {title, videos: [.media[] | select(.type=="video") | {id, source_url, selection, captured_at, retrieved_at}]}]}'
done

# P2 — (T6) 캐시 줄 확인. .env 의 POSTGRES_USER·POSTGRES_DB를 바꿨으면 그 값으로. 열 이름은 T1 보고서 값
docker compose exec db psql -U postgres -d the_world_that_day \
  -c "SELECT source, event_date, day_paths_off, fetched_at FROM event_media_search_cache ORDER BY fetched_at"
```

담당별: backend-dev 4 (T1~T4 — T1 ★은 PM 결정 3 (b)), test-engineer 1 (T5), PM 1 (T6). 모두 6개.

## 테스트 방침

- **외부 네트워크를 부르지 않는다.** 캐시 시험은 가짜 출처(`FakeVideoSource`)·가짜 저장소·WireMock 으로 한다. 새 녹화는 하지 않는다 — 기존 M3a4b 녹화(`fixtures/wikimedia/commons/video/`, `wikidata/`)와 합성 fixture(`synthetic-` 접두)를 쓴다.
- 날짜는 데모 3개(1988-09-17, 2002-06-22, 1995-03-15)와 표본(1969-07-20, 2000-02-29 등 M3-P13의 공개 역사 사건 날짜·경계값)뿐이다. 실제 사람의 생년월일이 아니다. 시험용 `tragic` 사건은 지어낸 문장.
- 저장소·V3·`upsert` 동시성은 Testcontainers(Docker 필요). 시간 경계는 `MutableClock`. 시간·창 시험은 설정값과 `Deadline.CANCEL_GRACE` 에서 상한을 계산한다.
- 반복: 검증 작업(T5)은 `./gradlew check` **2회**, 마감·슬롯·창·동시성 시험이 든 범위는 **3회**.
- 로그 시험은 `OutputCaptureExtension` 으로 날짜·사건 ID·파일 이름·QID가 없음을 본다.

## 데모·표본 날짜로 확인할 것

| 날짜 | 자동 테스트 | PM 확인 (T6) |
|---|---|---|
| 1988-09-17 | 세계 API(녹화): 첫 호출 뒤 줄이 사건마다 있고(후보 0건), 두 번째 호출은 영상 요청 0, 응답 같음, `selection` 키 없음 | - |
| 2002-06-22, 1995-03-15 | 1995-03-15는 대상 없음 → 읽기·쓰기·요청 모두 0 | - |
| 2000-02-29 | 합성 세계 API: 영상 하나 붙음 → 두 번째 호출 영상 요청 0·응답 JSON 같음 | - |
| `tragic` 사건이 있는 날(합성) | `dayPathsOff` 가 참인 줄과 거짓인 줄이 따로, 서로 읽지 않음 | - |
| 1969-07-20 | 어댑터·`VideoSearch` 수준(녹화): 되살린 후보가 점수 130 `selection {commons_p10, 130}` 그대로 | P1 두 번 응답 같음, P2 줄 확인 |

## 크기 (400줄 규칙)

S1 = 제품 코드·설정(`server`, 테스트 제외, M3a4b와 같은 명령). 이 기능에는 `data/` 변경이 없다.

### 실측 비율 (M3a4b까지)

| 기능 | 파일별 셈 | 실제 S1 | 비율 |
|---|---|---|---|
| M3a3d (PR #22) | 약 234 | 312 / −72 | ×1.33 |
| M3a4 (PR #23) | 약 450 | 953 | ×2.1 |
| M3a4b (PR #24) T0~T3 | 약 405 | 954 / −222 (확인점 2 때) | ×2.36 |
| M3a4b (PR #24) 최종 | 약 405 | **1,155 / −236** (943244e CI 수정 포함 — 영상 슬롯 `Lane`·`PhotoPath` 등 계획에 없던 것) | ×2.85 |

### 다시 셈

M3a4b 계획서 B절의 파일별 셈(약 243)을 같은 기준으로 가져오고, 머지 코드 대조로 생긴 것(`origin`, 되살릴 때 다시 거르기, 합치기)을 더해 약 323으로 다시 셌다.

| 작업 | 파일별 셈 | 셈 | ×1.7 | ×2.0 (기대) | ×2.36 |
|---|---|---|---|---|---|
| T1 ★ | V3(약 15) · 엔티티(약 45) · 저장소·upsert·조회(약 35) · `media.search-cache` 설정과 검사(약 25) · `application.yml`(약 8) | 약 128 | 약 218 | 약 256 | 약 302 |
| T2 | `Candidate.origin`·`VideoResult.origin`·`CommonsVideoSource` 한 줄(약 19) · 후보 저장 형식 읽고 쓰기(약 35) · 되살림 + 다시 거르기(약 30) | 약 84 | 약 143 | 약 168 | 약 198 |
| T3 | 캐시 흐름(읽기·없는 사건만·합치기·쓰기·DB 실패, 약 85) · `VideoSearch` 연결(약 20) | 약 105 | 약 179 | 약 210 | 약 248 |
| T4 | `WikimediaHttpClient` 주석(약 6). 나머지는 시험 | 약 6 | 약 10 | 약 12 | 약 14 |
| **합계** | | **약 323** | **약 550** | **약 650** | **약 760** |

- 옛 계획서 B절은 약 243 × 1.7 = 약 415 였다. 이 표가 더 큰 이유: (a) 파일별 셈에 머지 코드 대조로 알게 된 `origin`(약 19)·다시 거르기(약 30)·`await` 합치기를 더했다, (b) M3a4b 실측이 ×2.36(최종 ×2.85)으로 ×1.7보다 컸다.
- 곱하는 비율은 ×1.7(M3a4b 계획의 비율)과 ×2.36(M3a4b T0~T3 실측) 사이 가운데인 **×2.0, 기대값 약 650 (550~760)** 으로 잡았다. ×2.85(최종)는 쓰지 않았다: M3a4b 최종에는 계획에 없던 영상 슬롯 수정(943244e, `Lane`·`PhotoPath`)과 `ActionApiCommonsClient` → `CommonsActionApi` 이동(약 147줄)이 들어 있었고, 이 기능은 새 외부 호출이 없다.
- 확인점: T1 뒤 300(PM 결정, 알림 — T1 단독 기대값이 약 256~300이라 **넘을 수 있다**. 넘어도 멈추지 않고 알린다), T3 뒤 700(알림, 이 계획이 더함 — PM 결정 1에 포함).

### PM 결정 1 (크기가 400을 넘는다)

기대값 약 650(550~760)은 400 규칙을 넘는다. 이미 승인된 분할(M3a4b 결정 1 (a))이 약 415를 전제로 했으므로 새 사실로 다시 묻는다.

| 안 | 내용 | 크기 (기대, 범위) | M3 PR 수 |
|---|---|---|---|
| **(a) 한 PR 그대로 (추천)** | T1~T6 모두 `feat/media-search-cache`. 확인점 T1 뒤 300(알림), T3 뒤 700(알림) | 약 650 (550~760) | 22 |
| (b) 한 번 더 나눔 | M3a4c = T1(저장소, ★) + T4(이월) → `feat/media-search-cache`. 새 `feat/media-search-flow` = T2·T3 (검증 T5·PM 확인 T6은 각 PR에서). 순서 M3a4c → M3a4d → M3a5 | 약 270 (230~315) / 약 380 (320~445) | 23 |
| (c) 흐름을 단순화 | 사건별 "일부만 신선" 처리를 없애고 출처 단위 전부-또는-없음(하나라도 오래되면 모든 사건을 다시 묻고 모두 덮어씀). T3 약 −25~40(기대 −50~80). 요청 절약이 줄어듦 | 약 585 (490~690) | 22 |

추천 이유 (a): M3a4(953)·M3a4b(1,155)도 확인점으로 한 PR에서 받았고, 저장소만 낸 PR((b)의 앞 PR)은 눈에 보이는 동작이 없어 흐름 PR과 같이 봐야 의미가 있다. (c)는 줄이 적게 줄고 부분 신선 사건의 요청 절약을 잃는다. 확인점 T3 뒤 700을 크게 넘으면(예 850+) 그때 (b)로 바꿀지 다시 묻는다.

각 backend-dev 보고서(T1·T2·T3)는 누적 S1을 적는다. 테스트(S2)는 따로 보고한다.

## 뒤따르는 기능

| 기능 | 이 기능에서 받는 것 | 메모 |
|---|---|---|
| M3a5 YouTube | 출처 공통 캐시 흐름, `media.search-cache.ttl.youtube` 를 더하고 30일 상한 검사, `QUOTA_SKIPPED` 는 캐시 안 됨(T3 (4)), `Candidate.origin`(채널 ID를 `MediaOrigin.channel` 로 — origin 이 없으면 저장되지 않음) | 30일이 지난 YouTube 줄을 지우는 일(III.E.4.d)은 M3a5가 정한다 — 이 기능은 만료 줄을 덮어쓰기만 한다 |
| M3a5b 대한뉴스 | 같음 | - |
| M3b3 T6 (웹) | 이미 `main` 에서 가능(M3a4b 머지됨) — 이 기능을 기다리지 않는다 | - |

## 리스크와 확인할 것

1. **크기** — 기대값 약 650이 400을 넘는다. PM 결정 1과 확인점으로 다룬다.
2. **영상 슬롯 정책과 3초 창 (지켜보는 위험, 정책 변경 없음)** — 사진 두 경로가 열려 있는 동안 영상 요청은 슬롯 하나만 쓴다(`Lane.VIDEO`, `openPhotoPath()`). 실제 네트워크에서 영상이 3초 창을 못 맞출 수 있다는 우려(M3a4b 첫 리뷰 권장 2, 고침 리뷰 권장 2)가 있었다. **PM T5(M3a4b, 2026-10-24): 병합된 코드에서 1969-07-20 Commons 영상이 화면에 도착했다 — 창 안에 들어왔다.** 그래서 이 기능은 슬롯 정책을 바꾸지 않고 설계도 하지 않는다. 이 캐시는 같은 날짜·사건의 두 번째 호출부터 영상 요청을 0으로 줄여 줄 뿐, 새 날짜의 첫 호출은 그대로다. 다른 날짜나 느린 네트워크에서 서버 로그에 `Video search failed … cause=deadline` 이 자주 보이면 그때 PM이 다시 본다(시간 초과는 캐시하지 않으므로 그런 날짜는 계속 다시 부른다).
3. **DB가 죽었을 때** — 읽기 한 번이 `connection-timeout` 1초까지 걸리고 이는 미디어 창 안에서 쓰인다(영상 작업이 그만큼 늦게 시작하고 끝 시각은 같다). 그래서 한 조회에서 읽기 실패 뒤 나머지 읽기·쓰기를 건너뛴다(T3 (5), (6)). 읽기는 되고 쓰기만 느리게 실패하는 경우는 창 밖에서 최대 1초가 더 걸릴 수 있다 — 드물어 허용하고 WARN 한 줄로 남긴다.
4. **오래된 결과** — TTL 7일 동안 Commons에 새로 올라온 영상은 보이지 않고, 0건도 7일 캐시된다. TTL은 설정이다. 날짜 단위 고정 데이터(과거 사건)라 허용.
5. **캐시와 차단 목록** — 읽을 때 `MediaFilter`·`MediaNormalizer` 를 다시 거친다(T2 (3)). 차단 목록은 지금처럼 다시 띄워야 적용된다(M3a3a 리스크 5).
6. **형식이 바뀌면** — 저장 형식에 버전 번호가 있고 읽을 수 없는 줄은 "없는 것"으로 보고 다시 찾아 덮어쓴다(T2 (4)). `Candidate` 가 바뀌어도 캐시를 비울 필요가 없다.
7. **저장 범위** — 리서치 공통-4 문구는 "검색 결과(파일 이름·점수)만 캐시"이고 이 계획은 화면에 쓰는 메타데이터 레코드(주소·썸네일 주소·라이선스·표기·설명 200자·변환본 주소)까지 저장한다. 파일 바이트·포스터는 저장하지 않는다. M3a4b 계획서가 승인한 범위 그대로이며, 코드 리뷰가 "주소와 텍스트 메타데이터뿐"인지 한 번 더 본다.
8. **줄 크기** — 날짜 범주 후보(최대 20)와 범주 후보(최대 10)가 사건마다 되풀이 저장되어 줄 하나가 수십 KB일 수 있고 줄은 날짜당 최대 3개다. `TEXT` 로 충분하다. 정리 작업은 범위 밖.
9. **origin 누락** — 새 출처가 `Candidate.origin` 을 안 넘기면 그 사건은 저장되지 않는다(안전한 기본값). M3a5·M3a5b 계획이 origin 을 넘기는 것을 완료 기준에 넣어야 한다.
10. **시험 사이에 캐시가 새는 것** — 기반 클래스가 캐시를 끄고, 캐시 시험만 켜고 `TRUNCATE` 한다(T3 (8)). 컨텍스트가 하나 늘어 시험이 약간 느려진다.
11. **사생활** — 줄에는 날짜·사건 ID·후보 메타데이터·시각뿐이고 요청한 사람을 가리키는 값이 없다(`CLAUDE.md` 7절 6항). 날짜는 사용자의 생년월일일 수 있지만 장소·사람과 함께 저장·기록하지 않고 로그에 쓰지 않는다. V2 위키 캐시와 같은 수준이다.
12. **서버 전체 시간 초과 계수기**(고침 리뷰 권장 3) — 이 기능에서 하지 않는다. M6b에서 다룬다.

## PM 결정 필요

**PM 결정 (2026-10-24)**: 크기 결정 1 **(a)** 한 PR(`feat/media-search-cache`, T1~T6). 확인점 1: T1 뒤 누적 S1 300 넘으면 알림, 확인점 2: T3 뒤 700 넘으면 알림 — 둘 다 멈추지 않는다(T3 뒤 850 이상이면 (b)로 바꿀지 다시 묻는다). M3 PR 22개 그대로. planner 추천과 같다. 고르지 않은 것: (b) 한 번 더 나눔, (c) 출처 단위 전부-또는-없음.

이미 정해져 다시 묻지 않는 것: 분할 (a)·확인점 T1 뒤 300(알림)·★ T1은 backend-dev(M3a4b 결정 1 (a)·3 (b), 2026-10-24).

| # | 물을 것 | 선택지 | 추천 |
|---|---|---|---|
| 1 | 크기 기대값 약 650(550~760)이 400을 넘는다 — 나눌지 | (a) 한 PR + 확인점 T1 뒤 300·T3 뒤 700 (알림) / (b) 저장소 PR과 흐름 PR로 한 번 더 나눔(M3 PR 23개) / (c) 부분 신선 처리를 빼서 줄임 | **(a)** — 위 "PM 결정 1" 설명 |

## 다음 단계 제안 (이 계획 밖, PM이 일정을 정함)

- 영상 슬롯 정책은 바꾸지 않는다(PM T5: 1969-07-20 영상이 창 안에 도착). 서버 로그에 `cause=deadline` 이 자주 보이면 그때 다시
- 서버 전체 시간 초과 계수기 → M6b
- 웹 mock `selection.score` 0.9 → 정수(예 90) — 작은 웹 `fix/`
- M3b3c 리뷰 권장 1 — 플레이어 `onReady` 전에 사용자가 누른 재생을 조정자가 모름 — 작은 웹 `fix/`
- 시험 공용 `noUnmatchedRequest` `@AfterEach`(매치 안 된 WireMock 요청 0을 시험마다 따로 단언하는 것을 하나로)
- 오래된 캐시 줄 정리(YouTube 30일 삭제 의무와 함께 M3a5가 정함)
- 사진 메타데이터 캐시(로드맵 후속 후보 — 이 캐시 형식을 재사용할 수 있는지 M6b 측정 뒤)
