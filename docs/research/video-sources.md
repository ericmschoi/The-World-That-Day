# 영상·공식 임베드 출처 — 출처 검증

> 작업: T1 (V1, V7 일부) · 계획서: docs/plans/m3-video-sources-research.md · 조사 일자: 2026-10-10 ~ 2026-10-11 · 작성: data-researcher
> 이 문서의 약관 해석은 법률 자문이 아니다. 확신도 기준은 `docs/plans/m0-source-research.md` 의 "확신도 기준"을 따른다.
> 이 문서는 T1~T5가 나눠 채운다. 지금은 **T1(V1 Commons 동영상 자동 검색, V7 일부)** 부분만 있다. V2~V6 자리와 1·7·9·10절의 최종본은 T2~T5가 채운다.
> 실제 응답은 모두 키 없이 WebFetch로 받았다. WebFetch는 User-Agent를 바꿀 수 없어 도구 기본 User-Agent로 요청했다(서비스 구현은 M3a1 공통 설정의 User-Agent를 쓴다). 미디어 파일은 내려받지 않았고, 응답 원문은 저장소에 넣지 않았다. 2026-10-10 요청은 세션 한도로 중단되었다가 2026-10-11에 이어서 했다(11절 확인 일자 참조).
> "맞음" 판정은 사람(data-researcher)이 **파일 제목과, 받아 본 경우 설명·촬영일 필드**로 했다. 영상을 재생해 보지 않았다. "애매"는 그날 장면이 들어 있을 수 있지만 편집물·복원 비교·다큐멘터리라 제목만으로 판정할 수 없는 것이다.

## 1. 요약

(최종 요약은 T5가 쓴다. 아래는 V1 중간 요약이다.)

- **Commons 동영상은 키 없이 Action API 한 번(`generator=search` + `prop=imageinfo`)으로 후보·원본·포스터·길이·라이선스·촬영일을 함께 받을 수 있다(`확인됨`)** [2][4][82]. `prop=videoinfo&viprop=derivatives` 로 작은 해상도 변환본(240p VP9 WebM 등)의 주소도 받는다 [3][79][80].
- **표본 8개 날짜 중 사건에 맞는 영상이 나온 날은 1969-07-20(아폴로 11호) 하나뿐이다.** 국내 사건 날짜(1988-09-17, 2002-06-22, 1986-09-20, 2000-02-29)는 모든 경로에서 맞는 영상이 0건이다(5절). 국내 사건 영상은 Commons 자동 검색으로는 거의 기대할 수 없고, M3a5(공식 채널·대한뉴스)가 주 경로가 된다.
- **경로별 정확도 차이가 크다.** 사건 QID의 Wikidata `P10`(동영상) 1/1, 날짜 범주 `Videos taken on YYYY-MM-DD` 직속, 사건 범주 직속(`incategory:`)은 정확하다. 하위 범주까지 넓히는 `deepcat:` 은 선수·인물 범주를 타고 들어가 엉뚱한 결과가 대부분이다(예: "Korean War" 182건에 아폴로 11호 영상 포함) [28][29][33].
- **촬영일 필드(`DateTimeOriginal`)가 기록 영상에서는 업로드·복원 날짜인 경우가 있다**(예: 1969년 달 착륙 영상인데 2019-07-21) [82]. 그대로 쓰면 6.1절 정직한 날짜 표기를 어긴다 → 정확도 규칙 보정 제안(7절).
- Commons 핫링크는 정책상 "가능하지만 권장하지 않음"이다 [7]. 브라우저 `<video>` 재생을 막는 근거는 찾지 못했지만 실제 재생은 확인하지 않았다(6절 PM 확인 명령).

## 2. 확인할 질문과 답

질문은 계획서 V1·V7(일부)과 공통 질문이다. V2~V6 행은 T2~T4가 더한다.

| # | 질문 | 출처 | 답 | 확신도 | 근거 |
|---|---|---|---|---|---|
| V1-1 | 경로 (a): 사건 QID → Commons 범주 | Wikidata + Commons | 사건 QID의 `P373`(Commons 범주 이름)을 읽고, 없으면 `P910`(주 범주 항목)의 `commonswiki` 사이트링크를 쓴다. 표본 9개 QID 중 8개에 `P373` 이 있고, 1986년 아시안 게임(Q495611)은 `P373` 없이 `P910` → `Category:1986 Asian Games` 만 있다. 사건 항목 자체에는 `commonswiki` 사이트링크가 없다(범주 항목에 붙는다). 그 범주 안 동영상은 `filetype:video incategory:"<범주>"` 검색으로 찾는다 | 확인됨 | [1][23][24][27] |
| V1-2 | 경로 (a) 보조: QID의 동영상 속성 | Wikidata | 사건 QID의 `P10`(video)에 Commons 파일 이름이 들어 있을 수 있다. 표본에서는 아폴로 11호(Q43653) → `AP11 FINAL APPROACH.ogv` 1건뿐이고 맞는 영상이다 | 확인됨 | [23][80] |
| V1-3 | 경로 (b): Action API 검색 | Commons | `list=search`(또는 `generator=search`)에 `srsearch=filetype:video <검색어>`, `srnamespace=6`. `srlimit` 최대 500, 생성기로 쓰면 `gsr` 접두사. `filetype:` 값은 `video` 등, `insource:` 는 위키텍스트 검색. 한국어 검색어는 사건명이 영어 제목인 파일과 거의 맞지 않지만, "아폴로 11호"는 83건이 나왔다(구조화 데이터 다국어 라벨로 맞는 것으로 보이나 원리는 미확인) | 확인됨 (원리 일부 미확인) | [1][2][35][41] |
| V1-4 | 경로 (c): 날짜 범주 | Commons | 동영상 날짜 범주 이름은 `Category:Videos taken on YYYY-MM-DD` (상위 `Category:Videos by day`, 하위 범주 5,242개). 표본 8개 날짜 모두 범주 **문서가 없다**. 그래도 1969-07-20(1개)·2014-04-16(2개)은 파일이 그 범주로 분류돼 있어 `categorymembers` 로 나온다. 일반 날짜 범주 `Category:YYYY-MM-DD` 는 8개 모두 있지만 동영상이 거의 없다 | 확인됨 | [56][57][59][60][61] |
| V1-5 | 경로별 표본 결과 수·맞는 수 | Commons | 5절 표본 날짜 결과 표 | 확인됨 (판정은 제목 기준) | 5절 |
| V1-6 | 원본·변환본 주소 | Commons | 원본 `videoinfo.url`(`upload.wikimedia.org/wikipedia/commons/…`), 변환본 `derivatives[].src`(`…/transcoded/<해시>/<파일>/<파일>.240p.vp9.webm` 등). 표본 파일 변환본: 240p·480p VP9 WebM, 360p `video/quicktime; codecs="mp4v"`(.mov). 각 항목에 `type`(MIME+코덱), `width`, `height`, `bandwidth`, `transcodekey` | 확인됨 | [3][5][79][80] |
| V1-7 | 길이 | Commons | `duration`(초, 소수). `videoinfo`·`imageinfo` 모두 `iiprop=size`(`viprop=size`) 요청에 함께 나온다. 공식 도움말 설명에는 길이가 적혀 있지 않다(관찰값) | 확인됨 (필드 존재는 실제 응답, 문서화는 안 됨) | [4][60][79][82] |
| V1-8 | 포스터 | Commons | `iiurlwidth`(`viurlwidth`)를 주면 `thumburl` 에 정지 화면 JPEG 주소가 나온다(파일 중간 지점 프레임이 기본 [5]). 주소는 표준 썸네일 폭(20·40·60·120·250·330·500·960·1280·1920·3840) 중 하나로 맞춰진다(320 요청 → `250px`, 330 요청 → `330px`) [6][80][82]. 응답의 `thumbwidth` 는 요청 폭을 돌려준다(브라우저가 줄여서 보여 준다). 호스트가 `thumb.wikimedia.org` 로 나왔는데, 이 도메인에 대한 공식 공지는 찾지 못했다 | 확인됨 (도메인 공지는 미확인) | [5][6][79][80][82] |
| V1-9 | 라이선스 필드 | Commons | `extmetadata.LicenseShortName`, `LicenseUrl`, `UsageTerms`, `AttributionRequired`, `Copyrighted`, `Restrictions`, `Artist`, `Credit`. 사진과 같은 필드다 | 확인됨 | [79][80][82], day-events.md [47] |
| V1-10 | `<video>` 핫링크 가능 여부 | Commons | 정책: "가능하지만 권장하지 않음", 라이선스 조건(출처 표시)은 지켜야 한다 [7]. 속도 제한 문서는 Action API·REST API만 다루고 `upload.wikimedia.org` 의 브라우저 요청은 다루지 않는다 [8]. 봇 정책은 미디어 내려받기에 동시 2개·원본 또는 표준 썸네일 크기만을 요구한다(우리 서버는 내려받지 않으므로 직접 해당은 없다) [9]. 브라우저 재생을 막는다는 근거는 찾지 못했다. 실제 재생은 확인하지 않았다 | 확인됨 (정책) / 미확인 (실제 재생 — 6절 명령) | [7][8][9] |
| V7-1 | 사건과 맞춰 볼 메타데이터 | Commons | 파일 제목, `ImageDescription`(예: "On July 20, 1969, …"), `DateTimeOriginal`, 범주(`Categories`, 예: "Videos of 1969 from the United States"), 사건 범주 직속 여부, QID `P10`. 사건 QID 연결은 `P10` 과 사건 범주 직속뿐이다 | 확인됨 | [79][80][82] |
| V7-2 | 엉뚱한 결과 예 | Commons | (1) `deepcat:` 이 인물 범주를 타고 들어감: "1988 Summer Olympics" → 미국 축구 선수 기자회견·위키 워크숍 등 28건 중 0건 맞음, "Korean War" → 참전 군인이었던 닐 암스트롱 범주를 거쳐 아폴로 11호 영상 [25][33]. (2) 같은 사건의 다른 날: 1988-10-24 레이건 축하 연설, 아폴로 11호 발사(7-16)·궤도 랑데부(7-21) [36][28]. (3) 기념 영상: "Apollo 11 20th Anniversary", "75th Anniversary of the Korean War" [28][50]. (4) 날짜 문자열만 같은 무관 영상: `insource:"2014-04-16"` 40건 모두 세월호와 무관(논문 보충 영상, 슬로베니아 시위 등) [78]. (5) 업로드일이 촬영일 자리에 들어간 기록 영상(V7-3) | 확인됨 | 각 근거 |
| V7-3 | 촬영일 필드의 함정 | Commons | 기록 영상의 `DateTimeOriginal` 이 업로드·복원·공개 날짜일 수 있다: "Historic Apollo 11 Moonwalk Footage" 2019-07-21, "Apollo 11 Rare NASA Films…" 2019-03-20, "Jack King's Apollo 11 Launch Commentary" 2015-06-12, "Unknown date" 문자열도 있다. 반대로 "Taken on 20 July 1969", "1969-07-20" 처럼 실제 촬영일이 든 파일도 있다 | 확인됨 | [79][80][82] |
| V7-4 | `tragic` 사건 영상의 잔혹한 장면 위험 | Commons | 1950-06-25(6·25 전쟁) 검색 결과에 전쟁 기록 영상("KOREAN GUN CAMERA", "U.S. Germ Warfare in Korea (1952)" 등)이 섞인다. 그날 영상은 없다. 2014-04-16은 맞는 영상이 없고 언론사(뉴스타파) 영상 1건이 나온다. M3a4의 "`tragic` 사건은 영상을 찾지 않는다" 규칙을 유지할 근거다 | 확인됨 (제목 기준) | [50][51][52] |
| V7-5 | 대표 링크가 사건이 아닐 때 | 한국어 위키백과 | 날짜 문서 줄의 첫 링크가 사건이 아닌 경우가 있다: 1950-06-25 줄의 첫 링크는 `조선인민군`(Q240670, 범주 "Military of North Korea"), 2014-04-16 줄은 `전라남도`(Q41161). 경로 (a)는 이때 엉뚱한 범주를 뒤진다. 두 날 모두 `tragic` 이라 영상 검색은 하지 않지만 `normal` 사건에서도 생길 수 있다 | 확인됨 | [13][17][22][23] |
| 공통-1 | 날짜 정밀도(촬영·방송·게시) | Commons | `DateTimeOriginal` 은 "촬영일" 필드지만 V7-3처럼 업로드일이 들어 있을 수 있다. 게시일(업로드 시각)은 `extmetadata.DateTime`(예: 2017-03-14 04:01:24)으로 따로 온다 — 6.1절에 따라 쓰지 않는다 | 확인됨 | [79][80] |
| 공통-2 | 출처 표시에 필요한 값 | Commons | 작가(`Artist`), 라이선스 이름·링크, 원문 페이지(`descriptionurl`). 저작권자가 지정한 표시 방법이 있으면 그것을 따른다 | 확인됨 | [7][79] |
| 공통-3 | 해상도별 대략 용량(모바일) | Commons | `bandwidth`(초당 비트, 관찰값)×길이로 계산: 3분 22초 파일 기준 240p VP9 약 7.8 MB, 480p VP9 약 18.5 MB, 원본 27.5 MB [79]. 4분 30초 파일 240p 약 7.3 MB [80]. 3시간 16분 원본 622 MB인 파일도 있다 [82]. `bandwidth` 의 정의는 공식 문서에서 찾지 못했다 | 부분 확인 (계산은 관찰값 기준) | [79][80][82] |
| 공통-4 | 캐싱·저장 | Commons | 파일은 복사하지 않는다(안전장치 1). 검색 결과(파일 이름·점수)만 캐시한다. 파일 사용 조건은 파일 라이선스를 따르고, API 예절 문서는 응답 캐시를 권장한다 | 확인됨 | day-events.md [19][20] |

## 3. 출처 카드

### 출처 — Wikimedia Commons 동영상 자동 검색
- 구분: 제품 정의서 후보 (사건 대표 이미지 카드 `day-events.md` 3절의 동영상 확장)
- 제공 기관 / 공식 문서: Wikimedia Foundation / CirrusSearch 검색 도움말 [1], API:Search [2], Commons API `prop=videoinfo` 도움말 [3], `prop=imageinfo` 도움말 [4], TimedMediaHandler [5], 표준 썸네일 크기 [6], 외부 재사용 안내 [7], 속도 제한 [8], 봇 정책 [9]
- 무엇을 얻나: 사건 범주·날짜 범주·검색어에 맞는 동영상 파일 목록과 파일마다 원본 주소, 변환본(해상도별) 주소, 포스터 JPEG 주소, 길이, 크기, MIME, 라이선스·작가·촬영일·설명(`extmetadata`)
- 접근 방법: REST API (`https://commons.wikimedia.org/w/api.php`). 검색 `list=search`/`generator=search` + `filetype:video`, 범주 `generator=categorymembers`, 파일 정보 `prop=imageinfo` 또는 `prop=videoinfo`(`viprop=derivatives`). 재생은 `upload.wikimedia.org` 파일 주소를 `<video>` 로 핫링크
- 인증·키: 필요 없음. User-Agent 필수(형식은 M3a1 공통 설정) — day-events.md [14]
- 요금·쿼터: 무료. Action API는 식별되지 않은 요청 분당 10, 규칙에 맞는 User-Agent 분당 200, 동시 3개 이하, 429는 `Retry-After` 존중 [8]. `upload.wikimedia.org` 에 대한 브라우저 재생 요청의 제한은 문서에 없다(미확인) [8]. 봇의 미디어 내려받기는 동시 2개, 원본 또는 표준 썸네일만 [9]. `extmetadata` 는 비싼 속성이라 한 번에 적은 파일만 요청하라고 적혀 있다 [3][4]
- 커버리지: 표본 8개 날짜 중 맞는 영상은 1969-07-20뿐(5절). 국내 사건 4개 날짜 0건. 1969년 아폴로 11호는 미 정부(NASA·국립문서보관소·닉슨 도서관) 퍼블릭 도메인 영상이 많다. 한국어 제목 동영상은 드물다(이 작업의 검색 결과에서 본 한국어 제목은 배구 경기·인터뷰 영상(2017~2018) [25][30], 뉴스타파 보도(2014) [52], 기록 영화 「정의의 진격」 1·2부(1951·1952) [50] 정도)
- 날짜 정밀도: 파일마다 다름. `DateTimeOriginal` 이 실제 촬영일(일 단위)인 파일과 업로드·복원일인 파일이 섞인다(2절 V7-3)
- 응답 메타데이터: `DateTimeOriginal`(촬영일, 자유 텍스트), `ImageDescription`, `Categories`, `LicenseShortName`·`LicenseUrl`·`UsageTerms`·`AttributionRequired`·`Copyrighted`·`Restrictions`, `Artist`·`Credit`, `duration`, `width`·`height`, `mime`, `mediatype`(`VIDEO`). 좌표(`GPSLatitude` 등)는 표본 동영상에서 보지 못했다(사진과 같은 필드라 있을 수는 있다, 미확인)
- 라이선스·약관 핵심:
  - 화면 표시: Commons는 자유 라이선스·퍼블릭 도메인 파일만 받는다(day-events.md [46]). 표본은 Public domain(NASA·닉슨 도서관), CC BY 3.0(미 국립문서보관소 업로드), CC BY 2.5 ar(아르헨티나 대통령실) [79][80][82]
  - 핫링크: "가능하지만 권장하지 않음" [7]. 포스터는 표준 썸네일 크기만 받는다 — 표준이 아닌 크기의 직접 요청은 거부된다 [6]. API가 준 `thumburl` 을 그대로 쓰면 표준 크기다 [6][82]
  - 캐싱·저장: 파일 라이선스를 따른다. 우리는 파일을 복사하지 않고 검색 결과(식별자·점수)만 캐시한다(day-events.md [19][20])
  - 상업 이용: 파일 라이선스를 따른다(CC BY-SA는 동일조건, 인물이 나오면 당사자 동의 문제 별도 [7])
  - 출처 표시: 작가·라이선스(필요하면 링크)·원문 페이지. 저작권자가 정한 방법이 있으면 그것을 따른다 [7]
  - 변경 허용: 파일 라이선스를 따른다. 우리는 영상을 편집하지 않고 Commons가 만든 변환본·포스터를 그대로 쓴다
- 이 서비스에 맞는가: **조건부** — 키 없이 자동으로 쓸 수 있고 라이선스 필드로 자동 판정이 되지만, 국내 사건 영상은 표본에서 0건이다.
  - 비상업일 때: 세계 사건(특히 미 정부 기록) 영상의 1순위 자동 경로. 정확한 경로(`P10`, 범주 직속, 날짜 범주 직속)만 쓰고 정확도 규칙(7절)을 거쳐야 한다.
  - 상업일 때: 같은 경로. 파일 라이선스가 자유 라이선스라 그대로 성립하지만 CC BY-SA 동일조건, 인물 동의 주의.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 (실제 응답) | [1][2][3][4] 2026-10-10, 실제 응답 [25]~[82] 2026-10-10~11 |
  | 인증·키 | 확인됨 (키 없음, User-Agent 필수) | day-events.md [14] 2026-10-09, 실제 응답 [25]~[82] |
  | 요금·쿼터 | 부분 확인 (API 제한은 확인, `upload.wikimedia.org` 브라우저 요청 제한은 문서 없음) | [8] 2026-10-11, [9] 2026-10-11 |
  | 커버리지 (필수) | 확인됨 (표본 8개 날짜) | 5절, [25]~[78] 2026-10-10 |
  | 날짜 정밀도 | 확인됨 (파일별, 업로드일 섞임) | [79][80][82] 2026-10-10~11 |
  | 캐싱·저장 (필수) | 확인됨 (파일 라이선스로 판정, 파일은 복사 안 함) | day-events.md [19][20] 2026-10-09, [7] 2026-10-10 |
  | 출처 표시 (필수) | 확인됨 | [7] 2026-10-10, [79] 2026-10-10 |
  | 상업 이용 (필수) | 확인됨 (파일 라이선스로 판정) | [7] 2026-10-10, day-events.md [20][46] 2026-10-09 |
  | 변경 허용 (미디어 출처면 필수) | 확인됨 (파일 라이선스로 판정, 편집하지 않음) | [7] 2026-10-10 |
  | `<video>` 핫링크 (계획서 V1 질문) | 확인됨 (정책상 허용·비권장) / 실제 브라우저 재생은 미확인 | [7] 2026-10-10, 6절 명령 |
- 확신도: 확인됨 (검색·메타데이터 경로. 실제 브라우저 재생은 6절 명령으로 PM이 한 번 확인 권장 — 사진 핫링크와 같은 정책 근거다)
- 샘플 응답 확인: 3.1절과 5절. 파일 단위 필드는 [79](`President Nixon speaking with astronauts Armstrong and Aldrin on the Moon.webm`), [80](`AP11 FINAL APPROACH.ogv`), [82](검색 생성기 + `imageinfo` 5개)
- 근거 링크: [1]~[9], [22]~[82], day-events.md [14][19][20][46][47]

#### 3.1 V1 경로별 요청 형식과 실제 응답

모든 요청은 `https://commons.wikimedia.org/w/api.php`(Commons), `https://query.wikidata.org/sparql`(Wikidata), `https://ko.wikipedia.org/w/api.php`(위키백과)에 GET, `format=json`. 자리표시는 `<…>`.

| 경로 | 요청 형식 | 실제 응답 (확인 일자) | 확인한 필드 |
|---|---|---|---|
| (사전) 대표 링크 → QID | 위키백과 `action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&titles=<제목1>\|<제목2>…&formatversion=2` (M3a2와 같음) | 9개 제목 → QID 9개, 리다이렉트 1(`2002 FIFA 월드컵` → `2002년 FIFA 월드컵`) [22] (2026-10-10) | `pages[].wikibase_item`, `redirects` |
| (a-0) QID → 범주 이름·동영상 | SPARQL `SELECT ?item ?p373 ?video ?cs WHERE { VALUES ?item {wd:<Q…>} OPTIONAL{?item wdt:P373 ?p373} OPTIONAL{?item wdt:P10 ?video} OPTIONAL{?item wdt:P910 ?cat. ?s schema:about ?cat; schema:isPartOf <https://commons.wikimedia.org/>; schema:name ?cs} }` — M3a2의 SPARQL 한 번에 붙일 수 있다 | P373 8/9, P10 1/9(Q43653), P910 범주 사이트링크 7/7 [23][24] (2026-10-10) | `p373`(이름만, `Category:` 없음), `video`(`Special:FilePath/<파일>` 주소), `cs`(`Category:…`) |
| (a-1) 사건 범주 직속 동영상 | `action=query&list=search&srnamespace=6&srlimit=50&srsearch=filetype:video incategory:"<P373>"` | 1988 Summer Olympics 0 [26], Apollo 11 5 [27], 2002 FIFA World Cup 0 [31] (2026-10-10) | `searchinfo.totalhits`, `search[].title` |
| (a-2) 사건 범주 + 하위 범주 | `srsearch=filetype:video deepcat:"<P373>"` (하위 5단계·범주 256개까지 [1]) | 1988 Summer Olympics 28, Apollo 11 62, 2002 FIFA World Cup 19, 1986 Asian Games 2, Seoul Subway Line 7 0, Korean War 182, Sinking of the MV Sewol 0 [25][28][29][30][32][33][34] (2026-10-10). 256개를 넘는 큰 범주에서도 경고가 없었다(잘렸는지 알 수 없음) | 같음 |
| (a-3) QID `P10` | (a-0) 응답의 `video` | Q43653 → `AP11 FINAL APPROACH.ogv` [23] | `video` |
| (b-1) 한국어 검색어 | `srsearch=filetype:video <대표 링크 제목>` | 5절 표. 대부분 0, "아폴로 11호" 83 [41], "6.25 전쟁" 64 [50] (2026-10-10). 0건일 때 `searchinfo.suggestion` 에 엉뚱한 교정어가 온다(예: "1908 년 하계 올림픽") [35] | `totalhits`, `suggestion` |
| (b-2) 영어 검색어 | `srsearch=filetype:video "<영어 라벨>"` (QID 영어 라벨, enwiki 제목 등) | 5절 표 [36]~[55] (2026-10-10) | 같음 |
| (b-3) 날짜 문자열 | `srsearch=filetype:video insource:"YYYY-MM-DD"` (파일 설명 위키텍스트의 `date=` 등) | 1969-07-20 7 [72], 2014-04-16 40 [78], 나머지 6개 날짜 0 [71][73]~[77] (2026-10-10). 정규식 `insource:/…/` 은 0건(시간 초과 위험이 있다고 문서에 적힘) [1][70] | 같음 |
| (b-3') 날짜 문자열 + 사건 범주 | `srsearch=filetype:video deepcat:"<P373>" insource:"YYYY-MM-DD"` | Apollo 11 + 1969-07-20 → 7 (b-3과 같은 7건) [81] (2026-10-11) | 같음 |
| (c-1) 동영상 날짜 범주 | `action=query&generator=categorymembers&gcmtitle=Category:Videos taken on YYYY-MM-DD&gcmtype=file&gcmlimit=50&prop=imageinfo&iiprop=mime\|mediatype\|size\|url` | 범주 문서는 8개 날짜 모두 없음. 파일은 1969-07-20 1개, 2014-04-16 2개 [59][60][61] (2026-10-10) | `title`, `mime`, `mediatype`, `size`, `width`, `height`, `duration`, `url` |
| (c-2) 일반 날짜 범주 + 하위 | `srsearch=filetype:video deepcat:"YYYY-MM-DD"` | 1969-07-20 56(아폴로 11호 범주 트리를 탐), 2014-04-16 5(슬로베니아 학생 시위), 나머지 0 [62]~[69] (2026-10-10) | `totalhits`, `title` |
| 파일 정보 (한 번에) | `action=query&generator=search&gsrnamespace=6&gsrlimit=10&gsrsearch=<위 검색식>&prop=imageinfo&iiprop=url\|size\|mime\|extmetadata&iiurlwidth=330&iiextmetadatafilter=DateTimeOriginal\|LicenseShortName\|Artist\|…&formatversion=2` | 5개 파일의 원본·포스터·길이·라이선스·촬영일 [82] (2026-10-11) | `url`, `thumburl`, `thumbwidth`, `size`, `width`, `height`, `duration`, `mime`, `extmetadata.*` |
| 변환본 | `action=query&titles=File:<파일>&prop=videoinfo&viprop=url\|size\|mime\|mediatype\|derivatives\|extmetadata&viurlwidth=<표준 폭>&viextmetadatafilter=…&formatversion=2` (여러 파일을 `titles` 로 묶을 수 있지만 크기를 지정한 썸네일 주소는 한 요청에 50개까지 [3]) | [79][80] (2026-10-10) | `derivatives[].src`·`type`·`width`·`height`·`bandwidth`·`transcodekey`, `thumburl`, `duration` |

파일 단위 실제 값 (V1 완료 기준 3):

| 필드 | `President Nixon speaking with astronauts Armstrong and Aldrin on the Moon.webm` [79] | `AP11 FINAL APPROACH.ogv` [80] |
|---|---|---|
| 원본 `url` | `https://upload.wikimedia.org/wikipedia/commons/1/15/President_Nixon_speaking_with_astronauts_Armstrong_and_Aldrin_on_the_Moon.webm` (+ `?utm_source=…` 추적 파라미터) | `https://upload.wikimedia.org/wikipedia/commons/1/19/AP11_FINAL_APPROACH.ogv` (+ `?utm_…`) |
| 원본 형식 | `video/webm`, VP8·Vorbis, 640×480, 27,518,858 B | `application/ogg`, Theora·Vorbis, 320×240, 22,673,915 B |
| 변환본 | 240p VP9 WebM(320×240, 306,888 bps), 360p `video/quicktime; codecs="mp4v"`(480×360), 480p VP9 WebM(640×480, 730,296 bps) — 주소 `…/transcoded/1/15/<파일>/<파일>.240p.vp9.webm` 형식 | 240p VP9 WebM(216,464 bps), 360p mp4v .mov(320×240 — 원본보다 크게 만들지 않음) |
| 길이 `duration` | 202.272초 | 270초 |
| 포스터 `thumburl` | `https://thumb.wikimedia.org/wikipedia/commons/thumb/1/15/<파일>/500px--<파일>.jpg` (640 요청) | `…/thumb/1/19/AP11_FINAL_APPROACH.ogv/250px--AP11_FINAL_APPROACH.ogv.jpg` (320 요청) |
| 라이선스 | `LicenseShortName` Public domain, `AttributionRequired` false, `Copyrighted` False, `Restrictions` 빈 값 | 같음 |
| 작가 `Artist` | RichardNixonLibrary (YouTube 채널 링크) | René and Jonathan Cantin / Apollo Lunar Surface Journal |
| 촬영일 `DateTimeOriginal` | "Taken on 20 July 1969" | "1969-07-20" |
| 설명 | "On July 20, 1969, the Apollo 11 mission brought the first humans to the Moon. …" | "Apollo 11 - Landing on the Sea of Tranquility - July 20, 1969, 16mm landing film." |
| 업로드 시각 `DateTime` | 2017-03-14 04:01:24 (쓰지 않음) | 2009-07-23 01:00:57 (쓰지 않음) |

#### 3.2 V2·V3·V4 — YouTube 자동 검색·공식 채널·임베드 약관

(T2에서 작성)

#### 3.3 V5 — e영상역사관·대한뉴스 날짜 검색

(T3에서 작성)

#### 3.4 V6 — 제공사 공식 이미지 임베드와 검색

(T4에서 작성)

### 자동 검색 경로 표

(V1 행만. T2~T5가 다른 경로 행을 더하고 T5가 완성한다. 요청 수는 사건 하나 기준이다.)

| 경로 | 키 (환경변수, 서버 전용) | 쿼터·속도 제한 | 요청 한 번당 비용 | 캐시 허용 | 확신도 | V1 판단 |
|---|---|---|---|---|---|---|
| Commons (a-3) QID `P10` | 없음 (User-Agent `WIKIMEDIA_USER_AGENT` 등 M3a1 설정) | WDQS: 질의 60초, IP당 동시 5개, 429 (day-events.md [24]) | 0 추가 (M3a2 SPARQL에 `wdt:P10` 추가) + 파일 정보 1 | 결과 식별자 캐시 가능 | 확인됨 | 정확도 최상(1/1), 수는 적음 |
| Commons (a-1) `incategory:"<P373>"` | 없음 | Action API 분당 200(User-Agent 준수), 동시 3 [8] | 1 (`generator=search` + `imageinfo` 한 번) | 같음 | 확인됨 | 사용 (P373 없으면 P910 사이트링크) |
| Commons (a-2) `deepcat:` | 없음 | 같음 + 하위 5단계·256범주 [1] | 1 | 같음 | 확인됨 | 쓰지 않음 (엉뚱한 결과 대부분) |
| Commons (b-1/b-2) 검색어 | 없음 | 같음 | 1 (검색어마다) | 같음 | 확인됨 | 보조. 사건어·날짜 규칙 필수 |
| Commons (b-3) `insource:"YYYY-MM-DD"` | 없음 | 같음 | 1 (날짜마다, 사건끼리 공유) | 같음 | 확인됨 | 후보 생성용. 결과 단독으로 붙이지 않음 |
| Commons (c-1) `Videos taken on YYYY-MM-DD` | 없음 | 같음 | 1 (날짜마다) | 같음 | 확인됨 | 사용 (날짜 확실, 사건 일치는 따로 확인) |
| Commons (c-2) `deepcat:"YYYY-MM-DD"` | 없음 | 같음 | 1 | 같음 | 확인됨 | 쓰지 않음 |

## 4. 미디어 레코드 필드 매핑

`docs/plans/m3a3-event-media.md` 의 공용 미디어 형식 기준. V2~V6 열은 T2~T4가 더한다.

| 필드 | Commons 동영상 (`video_file`) |
|---|---|
| id | 계산: `commons-` + `pageid` (검색 응답 `pages[].pageid`) |
| type | 고정값: `video` |
| display_mode | 고정값: `video_file` |
| url | 응답 필드: `imageinfo.url` / `videoinfo.url` (원본). `utm_*` 쿼리는 그대로 두거나 떼도 같은 파일이다(뗄지 여부는 구현 선택, 미확인 영향 없음) |
| thumbnail_url | 응답 필드: `thumburl` (`iiurlwidth` 를 표준 폭으로 요청 — 예 330) [6] |
| thumbnail_width / thumbnail_height | 응답 필드: `thumbwidth` / `thumbheight` (요청 폭 기준 값. 실제 JPEG는 표준 폭) |
| video_sources | 응답 필드: `videoinfo.derivatives[]` → `{url: src, mime: type, height}`, `height` 작은 것부터. `video/webm` 변환본을 앞에, `video/quicktime` 변환본을 뒤에. 원본은 마지막 |
| duration_seconds | 응답 필드: `duration` |
| embed_url / provider / render / link_only_reason | 고정값: `null` |
| source_name | 고정값: `Wikimedia Commons` |
| source_url | 응답 필드: `descriptionurl` |
| license | 응답 필드: `extmetadata.LicenseShortName` (없으면 `null`) |
| license_status | 계산: M3a3 허용 목록 대조 (`free`/`other`/`unknown`) |
| license_url | 응답 필드: `extmetadata.LicenseUrl` |
| attribution | 계산: `Artist`(HTML 제거, 없으면 `Credit`, 둘 다 없으면 "작가 정보 없음") + 라이선스 + `Wikimedia Commons` (M3a3과 같음) |
| description | 응답 필드: `extmetadata.ImageDescription` (HTML 제거) |
| captured_at / date_precision | 계산: `DateTimeOriginal` 을 M3a3 파서로. **단, 7절 제안 R-V3: 사건 날짜보다 늦은 값은 업로드·복원일로 보고 `null`**. `extmetadata.DateTime`(업로드 시각)은 쓰지 않는다 |
| lat·lng 또는 place_ref | 표본 동영상에는 좌표가 없었다 → `place_ref`(사건의 `scope` 기준). `GPSLatitude`·`GPSLongitude` 가 있으면 사용(미확인 — 표본에 없음) |
| retrieved_at | 계산: 조회 시각 |

결론: Commons 동영상은 라이선스와 관계없이 `display_mode: video_file`(원 출처 핫링크)이다. `link_only` 는 실제 재생이 기술적으로 막힌 것이 확인될 때만이다(6절 명령 결과가 200/206이 아니면 PM에게 보고). 비상업·상업 모두 같고, 상업이면 `license_status: free` 만 쓰는 선택지가 생긴다(M3a3 결정 범위).

## 5. 데모·표본 날짜 판단

### 표본 날짜 결과 표 (V1)

사건은 한국어 위키백과 날짜 문서(사건·문화 절)와, 확인한 경우 연도 문서에서 뽑았다 [11]~[21]. 연도 문서는 1969·1986·2000년만 봤고 추가 사건이 없었다(1988·2002·1995·1950·2014년 연도 문서는 이 작업에서 보지 않음 — 미확인). 칸은 `찾은 수 / 맞음` (괄호는 애매). "상위 50"은 50건까지만 받아 판정했다는 뜻이다.

| 날짜 (tone 예상) | 사건 (대표 링크 → QID → Commons 범주) | (a-1) 범주 직속 | (a-2) deepcat | (a-3) P10 | (b-1) 한국어 | (b-2) 영어 | (b-3) 날짜 문자열 | (c-1) Videos taken on | (c-2) 날짜 deepcat |
|---|---|---|---|---|---|---|---|---|---|
| 1988-09-17 (normal) | 서울 올림픽 개막 (`1988년 하계 올림픽` → Q8470 → 1988 Summer Olympics) | 0 / 0 [26] | 28 / 0 [25] | 0 | "1988년 하계 올림픽" 0 [35]; "서울 올림픽" 9 / 0 [39] | "1988 Summer Olympics" 1 / 0 [36]; "Seoul Olympics" 4 / 0 [38]; "Seoul Olympics 1988 opening ceremony" 0 [37]; "1988 Seoul" 1 / 0 [54] | 0 [71] | 0 (범주 없음) [59] | 0 [64] |
| 2002-06-22 (normal) | 한국-스페인 8강 (`2002 FIFA 월드컵` → Q47735 → 2002 FIFA World Cup) | 0 / 0 [31] | 19 / 0 [29] | 0 | "2002년 FIFA 월드컵" 0 [43] | "2002 FIFA World Cup" 0 [42]; "Spain South Korea 2002" 0 [45]; "2002 World Cup Korea" 0 [53] | 0 [73] | 0 [59] | 0 [65] |
| 1995-03-15 | 사건 없음 [18] | 해당 없음 | 해당 없음 | 해당 없음 | 해당 없음 | 해당 없음 | 0 [75] | 0 [59] | 0 [68] |
| 1950-06-25 (tragic) | 6·25 전쟁 발발. 대표 링크가 `조선인민군`(Q240670 → Military of North Korea)으로 잡힌다(V7-5). 실제 사건 QID Q8663 → Korean War로도 확인 | 미실행 (tragic) | Korean War 182 / 0 [33] | 0 | "6.25 전쟁" 64 / 0 [50] | "Korean War" 63 / 0 [51] | 0 [76] | 0 [59] | 0 [66] |
| 1969-07-20 (normal) | 아폴로 11호 달 착륙 (`아폴로 11호` → Q43653 → Apollo 11) | 5 / 2 (1) [27] | 62 / 9 (16) [28] | 1 / 1 [23] | "아폴로 11호" 83, 상위 50 중 8 [41] | "Apollo 11" 67, 상위 50 중 7 [40] | 7 / 5 (1) [72] | 1 / 1 [60] | 56, 상위 50 중 8 [62] |
| 1986-09-20 (normal) | 서울 아시안 게임 개막 (`1986년 아시안 게임` → Q495611 → P373 없음, P910 → 1986 Asian Games) | 미실행 (deepcat 2건이 상한) / 0 | 2 / 0 [30] | 0 | "1986년 아시안 게임" 0 [48] | "1986 Asian Games" 0 [44]; 따옴표 없이 0 [55]; "Asian Games Seoul" 0 [49] | 0 [74] | 0 [59] | 0 [67] |
| 2000-02-29 (normal) | 서울 지하철 7호선 온수~신풍 개통 (`서울 지하철 7호선` → Q22910 → Seoul Subway Line 7) | 0 (deepcat 0) | 0 / 0 [32] | 0 | "서울 지하철 7호선" 0 [46] | "Seoul Subway Line 7" 0 [47] | 0 [77] | 0 [59] | 0 [69] |
| 2014-04-16 (tragic) | 세월호 참사. 대표 링크가 `전라남도`(Q41161)로 잡힌다(V7-5). 실제 사건 QID Q16338015 → Sinking of the MV Sewol로도 확인 | 미실행 (tragic) | Sewol 0 [34] | 0 | "세월호" 1 / 0 (뉴스타파 2014-08-12 보도) [52] | 미실행 | 40 / 0 [78] | 2 / 0 (태양 관측 SDO 영상) [61] | 5 / 0 (슬로베니아 시위) [63] |

1969-07-20 판정 내역 (제목 기준):
- (a-1) 맞음: TV clip of Buzz descending the ladder, Historic Apollo 11 Moonwalk Footage. 애매: Rare NASA Films from the National Archives. 아님: Jack King's Launch Commentary(발사일), Mónica Mihánovich experiencia(회고) [27].
- (a-2) 맞음 9: Restored Moonwalk, TV clip of Buzz, AP11 FINAL APPROACH, Landing - first steps, Nixon telephones the crew on the Moon, MSC TV Transmission Moon Walk Part 1, Historic Moonwalk Footage, Nixon speaking with astronauts on the Moon, Partially Restored Apollo 11 Video. 애매 16(복원 비교 3, 1969년 다큐 "The Eagle Has Landed" 4, 몽타주·소개 3, 임무 개관 등). 아님 37(발사일 7편, 기념 영상 7편, 훈련 4편, 같은 이름의 조사선 "R/V Neil Armstrong" 6편, 2010년대 착륙지 영상 3편 등) [28].
- (b-3) 맞음 5: TV clip of Buzz, AP11 FINAL APPROACH, Landing - first steps, MSC TV Transmission, Nixon speaking. 애매 1: Buzz Descends Comparison(복원 비교). 아님 1: Flight of Apollo 11 0:50~3:14 since launch(발사) [72].
- (c-1) 맞음 1: Nixon speaking with astronauts on the Moon [60].

### 데모 입력 판단

| 데모 입력 (12절) | 출처 | 얻을 수 있는 것 | 얻을 수 없는 것 | 근거 | 확신도 |
|---|---|---|---|---|---|
| 1988-09-17 + 서울 | Commons 동영상 | 없음 | 올림픽 개막식 영상 (모든 경로 0건, 개막식 범주는 사진 5장뿐) | [25][26][35]~[39][54][64][71], day-events.md [50] | 확인됨 |
| 2002-06-22 + 광주 서구 풍암동 | Commons 동영상 | 없음 | 한국-스페인전 영상 | [29][31][42][43][45][53][65][73] | 확인됨 |
| 1995-03-15 + 서울대학교병원 | Commons 동영상 | 없음 (사건 없음) | — | [18][68][75] | 확인됨 |

## 6. PM이 실행할 확인 명령

키가 필요한 명령은 없다. 아래 두 명령은 **브라우저 핫링크 재생이 기술적으로 막히지 않는지**(V1-10의 미확인 부분)를 서버 밖에서 한 번 확인하는 것이다. 파일 앞 1 KB만 받는다. 결과 파일은 저장소에 두지 않는다(`-o /dev/null`).

```bash
# 근거: [7] Commons 외부 재사용 안내(핫링크 가능·비권장), [80] 변환본 주소
# User-Agent는 M3a1 공통 설정 값(연락처 포함)을 환경변수로
curl -sS -o /dev/null -r 0-1023 \
  -A "$WIKIMEDIA_USER_AGENT" -H "Referer: http://localhost:5173/" \
  -w "%{http_code} %{content_type}\n" \
  "https://upload.wikimedia.org/wikipedia/commons/transcoded/1/19/AP11_FINAL_APPROACH.ogv/AP11_FINAL_APPROACH.ogv.240p.vp9.webm"
# 응답에서 확인할 것: 206(또는 200)과 video/webm. 403·429면 핫링크 재생이 막힌 것 → 보고

# 근거: [6] 표준 썸네일 크기, [80] 포스터 주소
curl -sS -o /dev/null -A "$WIKIMEDIA_USER_AGENT" -H "Referer: http://localhost:5173/" \
  -w "%{http_code} %{content_type}\n" \
  "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/19/AP11_FINAL_APPROACH.ogv/250px--AP11_FINAL_APPROACH.ogv.jpg"
# 응답에서 확인할 것: 200과 image/jpeg
```

브라우저의 WebM·VP9 재생 지원(특히 iOS Safari)은 이 작업 범위 밖이다. 변환본에 360p QuickTime(mp4v) 항목이 있다는 것만 확인했다 [79][80]. TimedMediaHandler 문서는 이 형식을 iOS 17.4 이전 기기용 대체본이라고 설명한다 [5]. 실제 재생은 M3b3에서 확인한다.

## 7. 이 영역 추천안

(최종 추천은 T5가 쓴다. 아래는 V1 추천안이다.)

- 1순위 (Commons 안에서): **(a-3) QID `P10` → (a-1) 사건 범주 직속(`incategory:`, P373 없으면 P910 사이트링크) → (c-1) `Videos taken on YYYY-MM-DD` 직속** 순서로 후보를 모은다. 사건 하나당 Commons 요청 1~2회(범주 검색 1 + 날짜 범주 1, 날짜 범주는 사건끼리 공유), P10·P373은 M3a2 SPARQL에 속성만 더해 추가 요청 0.
- 대안: (b-2) 영어 라벨 검색과 (b-3) 날짜 문자열 검색은 후보를 넓히는 데만 쓰고, 정확도 규칙의 사건어·날짜 조건을 모두 통과할 때만 붙인다.
- 쓰지 않음: (a-2)·(c-2) `deepcat:`.
- 이유: 표본에서 정확한 경로는 P10(1/1), 범주 직속(2/5, 애매 1), 날짜 범주 직속(1969 1/1, 2014년 2건은 날짜만 같고 사건과 무관 → 사건어 조건으로 걸러야 함)이다. `deepcat:` 은 인물·선수 범주를 타고 들어가 1988 Summer Olympics 28/0, 2002 FIFA World Cup 19/0, Korean War 182/0이 나왔다.

### 정확도 규칙 제안 (M3a4 계획서 "정확도 규칙"과 대조, V1 근거만)

| # | M3a4 현재 | 제안 | 근거 |
|---|---|---|---|
| R-V1 | 사건 QID의 Commons 범주 안 +50 | **직속(`incategory:`)만** +50, QID `P10` 에 든 파일도 +50. 하위 범주(deepcat)는 점수 없음·쓰지 않음 | [25][28][29][33] |
| R-V2 | (범주 출처: P373) | P373이 없으면 P910 항목의 `commonswiki` 사이트링크를 쓴다 | [23][24] (1986 Asian Games) |
| R-V3 | `captured_at` 은 명시된 촬영 날짜로만 | `DateTimeOriginal` 이 **사건 날짜보다 늦으면** 업로드·복원일로 보고 `captured_at`·정밀도를 `null`(촬영 시점 미상)로 둔다. 이 경우 날짜 일치 점수도 주지 않는다. 사건 범주 직속·P10이면 필수 조건(범주 안)으로 붙을 수 있다 | [82] (1969년 영상인데 2019-07-21 등) |
| R-V4 | 날짜 일치: 제목·설명·촬영일 | 유지. 설명문의 영어 날짜("On July 20, 1969", "July 20, 1969")도 읽도록 M3a3 파서 표 사용을 명시. `insource:` 일치만으로는 날짜 일치로 보지 않는다(업로드·논문 날짜일 수 있다) | [78][79][80] |
| R-V5 | 기념어 −30 | 낱말 추가 후보: "montage", "highlights", "comparison", "memorial", "commemoration", "tribute", "추모", "기념식". ("restored"는 그날 장면의 복원본이라 넣지 않음) | [28][50] |
| R-V6 | 다른 연도가 제목에 −40 | 유지. "Apollo 11 20th Anniversary (KSC-19900707-…)" 처럼 제목 속 자료 번호의 연도도 걸린다 — 의도대로다 | [28] |
| R-V7 | (없음) | 길이 상한 설정값 제안(예: `media.video.max-duration-seconds`). 3시간 16분·622 MB 파일이 후보로 나온다. 값은 PM 결정 | [82] |
| R-V8 | (없음) | 대표 링크가 사건이 아닐 수 있다(V7-5). 줄 안의 링크 중 QID의 P585/P580이 사건 날짜와 같은 것을 우선하는 방안을 planner가 검토 | [13][17][22][23] |
| R-V9 | `tragic` 사건은 영상 찾지 않음 | 유지. 6·25 검색 결과에 전쟁 기록 영상이 섞인다 | [50][51] |

## 8. 위험

1. **국내 사건 영상이 거의 없다** — 표본의 국내 사건 4개 날짜 모두 0건. Commons 동영상만으로는 "그날의 사건" 영상이 대부분 비어 보인다(정상 결과이지만 사용자 기대와 다를 수 있음). M3a5 경로가 필요하다.
2. **촬영일 필드 오류** — 업로드·복원일이 촬영일 자리에 있다(V7-3). 규칙 R-V3 없이 쓰면 6.1절을 어긴다.
3. **`deepcat:` 의 조용한 잘림** — 256개 범주 제한을 넘는 큰 범주에서도 경고가 없었다 [1][25][33]. 쓰지 않는 것으로 피한다.
4. **핫링크** — 정책상 비권장이고 `upload.wikimedia.org` 브라우저 요청 제한은 문서화되지 않았다 [7][8]. 로컬 데모 규모에서는 문제를 찾지 못했지만 공개 배포(#5) 때 다시 본다. 표준이 아닌 썸네일 크기를 직접 만들면 거부(429)된다 → API의 `thumburl` 만 쓴다 [6].
5. **`thumb.wikimedia.org` 호스트** — 응답에 나온 포스터 호스트에 대한 공식 공지를 찾지 못했다. 주소를 직접 조립하지 말고 API 값을 그대로 쓰면 영향이 없다.
6. **모바일 용량** — 원본이 수백 MB일 수 있다. `video_sources` 를 작은 변환본부터 두고 길이 상한(R-V7)을 둔다.
7. **"맞음" 판정 한계** — 제목·설명으로만 판정했고 영상을 보지 않았다. 잔혹한 장면 여부도 제목으로만 봤다.
8. **약관 해석은 법률 자문이 아니다.**

## 9. 제품 정의서 가정과 다른 점

(최종본은 T5. V1 결과만.)

- 1절 한 줄 설명·5절의 "그날 … 짧은 영상": Commons 자동 검색으로는 표본의 국내 사건 날짜에서 영상을 하나도 얻지 못했다. 세계 사건(1969-07-20)만 얻었다. 또 Commons 영상은 "짧은" 영상이 아닐 수 있다(3시간 16분 파일).

## 10. PM 결정 필요

(T5가 전체 목록으로 정리한다. V1에서 나온 질문.)

1. **V1-D1 Commons 하위 범주 검색(`deepcat:`) 사용 여부** — (a) 쓰지 않음, (b) 다른 경로가 0건일 때만 씀, (c) 씀. **추천 (a)**: 표본에서 맞는 결과를 늘리지 못하고 엉뚱한 결과만 늘었다(5절).
2. **V1-D2 촬영일이 사건보다 늦은 기록 영상 처리** — (a) 버림, (b) 사건 범주 직속·P10이면 "촬영 시점 미상"으로 표시하고 붙임, (c) 설명문 날짜를 촬영일로 씀. **추천 (b)**: 6.1절을 지키면서 맞는 영상(예: 달 착륙 복원 영상)을 살린다. (c)는 설명문 해석이 틀릴 위험이 있다.
3. **V1-D3 영상 길이 상한** — (a) 10분, (b) 30분, (c) 상한 없음(작은 변환본만 재생). **추천 (a)**: "짧은 영상"이라는 제품 정의와 모바일 용량.
4. **V1-D4 Commons에 자유 라이선스로 올라온 언론사 영상(예: 뉴스타파)** — 안전장치 3(통신사·언론사 자료는 공식 임베드로만)이 이런 파일에도 적용되는지 해석이 애매하다(근거: `docs/plans/m3a3-event-media.md` "안전장치" 3항, `CLAUDE.md` 7절 1항 네 번째 줄). (a) 자유 라이선스이므로 허용, (b) 언론사 이름을 `media.excluded-credits` 에 더해 제외. **추천 (a)**, 단 권리자 요청 시 차단 목록으로 대응. 이 해석은 법률 자문이 아니다. 표본에서 이런 파일은 `tragic` 날짜에만 나와 지금 화면에 영향은 없다.

## 11. 근거 링크

| # | URL | 문서 제목 | 확인 일자 |
|---|---|---|---|
| 1 | https://www.mediawiki.org/wiki/Help:CirrusSearch | MediaWiki — CirrusSearch 도움말 (`incategory:`, `deepcat:` 5단계·256범주, `intitle:`, `insource:` 정규식 시간 초과, `filetype:` 값 VIDEO 등, `filemime:`, 2026-09-11 수정) | 2026-10-10 |
| 2 | https://www.mediawiki.org/wiki/API:Search | MediaWiki — API:Search (`srsearch`, `srnamespace`, `srlimit` 최대 500, 생성기 `gsr`, 2026-08-11 수정) | 2026-10-10 |
| 3 | https://commons.wikimedia.org/w/api.php?action=help&modules=query%2Bvideoinfo | Commons API 도움말 — `prop=videoinfo` (`viprop=derivatives`·`timedtext`, `viurlwidth`, 썸네일 50개 제한, `extmetadata` 비용 경고) | 2026-10-10 |
| 4 | https://commons.wikimedia.org/w/api.php?action=help&modules=query%2Bimageinfo | Commons API 도움말 — `prop=imageinfo` (`iiprop=size` 설명에 길이 언급 없음, `iiurlwidth`, `iiextmetadatafilter`) | 2026-10-10 |
| 5 | https://www.mediawiki.org/wiki/Extension:TimedMediaHandler | MediaWiki — TimedMediaHandler (VP9 변환, 160p~1080p, MP4 업로드 기본 꺼짐, iOS 17.4 이전용 대체본, 중간 지점 정지 화면·`thumbtime`, 2026-06-12 수정) | 2026-10-10 |
| 6 | https://www.mediawiki.org/wiki/Common_thumbnail_sizes | MediaWiki — 표준 썸네일 크기 (표준 폭 목록, API는 올림, 직접 요청은 표준 크기만, 429 안내, 2026-07-28 수정) | 2026-10-10 |
| 7 | https://commons.wikimedia.org/wiki/Commons:Reusing_content_outside_Wikimedia | Commons — 외부 재사용 안내 (핫링크 가능·비권장, 원작자·라이선스 표시, 동일조건, 2026-08-11 수정) | 2026-10-10 |
| 8 | https://www.mediawiki.org/wiki/Wikimedia_APIs/Rate_limits | Wikimedia APIs — 속도 제한 (식별 안 됨 분당 10, User-Agent 준수 분당 200, 동시 3, 429·Retry-After, Action·REST API만 대상, 2026-06-03 수정) | 2026-10-11 |
| 9 | https://wikitech.wikimedia.org/wiki/Robot_policy | Wikitech — 봇 정책 (미디어 내려받기 동시 2·25 Mbps, 원본 또는 표준 썸네일만, 2026-03-16 수정) | 2026-10-11 |
| 10 | (번호 비움) | — | — |
| 11 | https://ko.wikipedia.org/w/index.php?title=9%EC%9B%94_17%EC%9D%BC&action=raw | 한국어 위키백과 `9월 17일` 위키텍스트 (1988년 줄 2개, 대표 링크 `1988년 하계 올림픽`) | 2026-10-10 |
| 12 | https://ko.wikipedia.org/w/index.php?title=6%EC%9B%94_22%EC%9D%BC&action=raw | `6월 22일` 위키텍스트 (2002년 사건 줄, 대표 링크 `2002 FIFA 월드컵`) | 2026-10-10 |
| 13 | https://ko.wikipedia.org/w/index.php?title=6%EC%9B%94_25%EC%9D%BC&action=raw | `6월 25일` 위키텍스트 (1950년 줄 첫 링크 `조선인민군`) | 2026-10-10 |
| 14 | https://ko.wikipedia.org/w/index.php?title=7%EC%9B%94_20%EC%9D%BC&action=raw | `7월 20일` 위키텍스트 (1969년 `아폴로 11호`) | 2026-10-10 |
| 15 | https://ko.wikipedia.org/w/index.php?title=9%EC%9B%94_20%EC%9D%BC&action=raw | `9월 20일` 위키텍스트 (1986년 문화 절 `1986년 아시안 게임`) | 2026-10-10 |
| 16 | https://ko.wikipedia.org/w/index.php?title=2%EC%9B%94_29%EC%9D%BC&action=raw | `2월 29일` 위키텍스트 (2000년 문화 절 `서울 지하철 7호선`) | 2026-10-10 |
| 17 | https://ko.wikipedia.org/w/index.php?title=4%EC%9B%94_16%EC%9D%BC&action=raw | `4월 16일` 위키텍스트 (2014년 줄 첫 링크 `전라남도`) | 2026-10-10 |
| 18 | https://ko.wikipedia.org/w/index.php?title=3%EC%9B%94_15%EC%9D%BC&action=raw | `3월 15일` 위키텍스트 (1995년 사건 없음, 탄생 절 줄만) | 2026-10-10 |
| 19 | https://ko.wikipedia.org/w/index.php?title=2000%EB%85%84&action=raw | `2000년` 위키텍스트 (2월 29일 문화 절 7호선 개통 1개) | 2026-10-10 |
| 20 | https://ko.wikipedia.org/w/index.php?title=1969%EB%85%84&action=raw | `1969년` 위키텍스트 (7월 20일 아폴로 11호 1개) | 2026-10-10 |
| 21 | https://ko.wikipedia.org/w/index.php?title=1986%EB%85%84&action=raw | `1986년` 위키텍스트 (9월 20일 사건 없음, 탄생만) | 2026-10-10 |
| 22 | https://ko.wikipedia.org/w/api.php?action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&titles=1988%EB%85%84%20%ED%95%98%EA%B3%84%20%EC%98%AC%EB%A6%BC%ED%94%BD%7C2002%20FIFA%20%EC%9B%94%EB%93%9C%EC%BB%B5%7C%EC%A1%B0%EC%84%A0%EC%9D%B8%EB%AF%BC%EA%B5%B0%7C6.25%20%EC%A0%84%EC%9F%81%7C%EC%95%84%ED%8F%B4%EB%A1%9C%2011%ED%98%B8%7C1986%EB%85%84%20%EC%95%84%EC%8B%9C%EC%95%88%20%EA%B2%8C%EC%9E%84%7C%EC%84%9C%EC%9A%B8%20%EC%A7%80%ED%95%98%EC%B2%A0%207%ED%98%B8%EC%84%A0%7C%EC%A0%84%EB%9D%BC%EB%82%A8%EB%8F%84%7C%EC%84%B8%EC%9B%94%ED%98%B8%20%EC%B9%A8%EB%AA%B0%20%EC%82%AC%EA%B3%A0&format=json&formatversion=2 | 위키백과 pageprops 실제 응답 — 대표 링크 9개의 QID | 2026-10-10 |
| 23 | https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3Fp373%20%3Fcs%20%3Fvideo%20%3Fp910%20WHERE%20%7B%20VALUES%20%3Fitem%20%7Bwd%3AQ8470%20wd%3AQ47735%20wd%3AQ240670%20wd%3AQ8663%20wd%3AQ43653%20wd%3AQ495611%20wd%3AQ22910%20wd%3AQ41161%20wd%3AQ16338015%7D%20OPTIONAL%7B%3Fitem%20wdt%3AP373%20%3Fp373%7D%20OPTIONAL%7B%3Fs%20schema%3Aabout%20%3Fitem%3B%20schema%3AisPartOf%20%3Chttps%3A%2F%2Fcommons.wikimedia.org%2F%3E%3B%20schema%3Aname%20%3Fcs%7D%20OPTIONAL%7B%3Fitem%20wdt%3AP10%20%3Fvideo%7D%20OPTIONAL%7B%3Fitem%20wdt%3AP910%20%3Fp910%7D%20%7D | Wikidata SPARQL 실제 응답 — P373·P10·P910·사건 항목 Commons 사이트링크 (P373 8/9, P10 Q43653만, 사건 항목 사이트링크 1/9) | 2026-10-10 |
| 24 | https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3Fcat%20%3Fcs%20WHERE%20%7B%20VALUES%20%3Fitem%20%7Bwd%3AQ8470%20wd%3AQ47735%20wd%3AQ8663%20wd%3AQ43653%20wd%3AQ495611%20wd%3AQ22910%20wd%3AQ16338015%7D%20%3Fitem%20wdt%3AP910%20%3Fcat.%20OPTIONAL%7B%3Fs%20schema%3Aabout%20%3Fcat%3B%20schema%3AisPartOf%20%3Chttps%3A%2F%2Fcommons.wikimedia.org%2F%3E%3B%20schema%3Aname%20%3Fcs%7D%20%7D | Wikidata SPARQL 실제 응답 — P910 범주 항목의 Commons 사이트링크 7/7 (Q495611 → Category:1986 Asian Games) | 2026-10-10 |
| 25 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%221988%20Summer%20Olympics%22&srnamespace=6&srlimit=50&srprop=size%7Ctimestamp&format=json | Commons 검색 — deepcat 1988 Summer Olympics (28건, 맞음 0) | 2026-10-10 |
| 26 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20incategory%3A%221988%20Summer%20Olympics%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — incategory 1988 Summer Olympics (0건) | 2026-10-10 |
| 27 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20incategory%3A%22Apollo%2011%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — incategory Apollo 11 (5건) | 2026-10-10 |
| 28 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%22Apollo%2011%22&srnamespace=6&srlimit=100&format=json | Commons 검색 — deepcat Apollo 11 (62건) | 2026-10-10 |
| 29 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%222002%20FIFA%20World%20Cup%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 2002 FIFA World Cup (19건, 맞음 0) | 2026-10-10 |
| 30 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%221986%20Asian%20Games%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 1986 Asian Games (2건, 배구 영상) | 2026-10-10 |
| 31 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20incategory%3A%222002%20FIFA%20World%20Cup%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — incategory 2002 FIFA World Cup (0건) | 2026-10-10 |
| 32 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%22Seoul%20Subway%20Line%207%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat Seoul Subway Line 7 (0건) | 2026-10-10 |
| 33 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%22Korean%20War%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat Korean War (182건, 아폴로 11호 영상 포함) | 2026-10-10 |
| 34 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%22Sinking%20of%20the%20MV%20Sewol%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat Sinking of the MV Sewol (0건) | 2026-10-10 |
| 35 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%201988%EB%85%84%20%ED%95%98%EA%B3%84%20%EC%98%AC%EB%A6%BC%ED%94%BD&srnamespace=6&srlimit=50&format=json | Commons 검색 — "1988년 하계 올림픽" (0건, 교정어 "1908 년 하계 올림픽") | 2026-10-10 |
| 36 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%221988%20Summer%20Olympics%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — "1988 Summer Olympics" (1건, 1988-10-24 레이건 연설) | 2026-10-10 |
| 37 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20Seoul%20Olympics%201988%20opening%20ceremony&srnamespace=6&srlimit=50&format=json | Commons 검색 — Seoul Olympics 1988 opening ceremony (0건) | 2026-10-10 |
| 38 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20Seoul%20Olympics&srnamespace=6&srlimit=50&format=json | Commons 검색 — Seoul Olympics (4건, 맞음 0) | 2026-10-10 |
| 39 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%EC%84%9C%EC%9A%B8%20%EC%98%AC%EB%A6%BC%ED%94%BD&srnamespace=6&srlimit=50&format=json | Commons 검색 — "서울 올림픽" (9건, 다른 나라 올림픽 공원 등) | 2026-10-10 |
| 40 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%22Apollo%2011%22&srnamespace=6&srlimit=50&srinfo=totalhits&format=json | Commons 검색 — "Apollo 11" (67건) | 2026-10-10 |
| 41 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%EC%95%84%ED%8F%B4%EB%A1%9C%2011%ED%98%B8&srnamespace=6&srlimit=50&format=json | Commons 검색 — "아폴로 11호" (83건) | 2026-10-10 |
| 42 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%222002%20FIFA%20World%20Cup%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — "2002 FIFA World Cup" (0건) | 2026-10-10 |
| 43 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%202002%EB%85%84%20FIFA%20%EC%9B%94%EB%93%9C%EC%BB%B5&srnamespace=6&srlimit=50&format=json | Commons 검색 — "2002년 FIFA 월드컵" (0건) | 2026-10-10 |
| 44 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%221986%20Asian%20Games%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — "1986 Asian Games" (0건) | 2026-10-10 |
| 45 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20Spain%20South%20Korea%202002&srnamespace=6&srlimit=50&format=json | Commons 검색 — Spain South Korea 2002 (0건) | 2026-10-10 |
| 46 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%EC%84%9C%EC%9A%B8%20%EC%A7%80%ED%95%98%EC%B2%A0%207%ED%98%B8%EC%84%A0&srnamespace=6&srlimit=50&format=json | Commons 검색 — "서울 지하철 7호선" (0건) | 2026-10-10 |
| 47 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%22Seoul%20Subway%20Line%207%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — "Seoul Subway Line 7" (0건) | 2026-10-10 |
| 48 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%201986%EB%85%84%20%EC%95%84%EC%8B%9C%EC%95%88%20%EA%B2%8C%EC%9E%84&srnamespace=6&srlimit=50&format=json | Commons 검색 — "1986년 아시안 게임" (0건) | 2026-10-10 |
| 49 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20Asian%20Games%20Seoul&srnamespace=6&srlimit=50&format=json | Commons 검색 — Asian Games Seoul (0건) | 2026-10-10 |
| 50 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%206.25%20%EC%A0%84%EC%9F%81&srnamespace=6&srlimit=50&format=json | Commons 검색 — "6.25 전쟁" (64건, 기념식·다른 날 전쟁 기록) | 2026-10-10 |
| 51 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%22Korean%20War%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — "Korean War" (63건) | 2026-10-10 |
| 52 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20%EC%84%B8%EC%9B%94%ED%98%B8&srnamespace=6&srlimit=50&format=json | Commons 검색 — "세월호" (1건, 뉴스타파 2014-08-12) | 2026-10-10 |
| 53 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%202002%20World%20Cup%20Korea&srnamespace=6&srlimit=50&format=json | Commons 검색 — 2002 World Cup Korea (0건) | 2026-10-10 |
| 54 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%201988%20Seoul&srnamespace=6&srlimit=50&format=json | Commons 검색 — 1988 Seoul (1건, 레이건 연설) | 2026-10-10 |
| 55 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%201986%20Asian%20Games&srnamespace=6&srlimit=50&format=json | Commons 검색 — 1986 Asian Games 따옴표 없이 (0건) | 2026-10-10 |
| 56 | https://commons.wikimedia.org/w/api.php?action=query&titles=Category:Videos%20by%20date%7CCategory:Videos%20by%20year%7CCategory:Videos%20of%201969%7CCategory:1969%20videos%7CCategory:Videos%20taken%20on%201969-07-20%7CCategory:1969-07-20%7CCategory:July%201969%20videos%7CCategory:Videos%20of%20July%201969%7CCategory:Videos%20by%20day%7CCategory:Films%20of%201969&prop=categoryinfo%7Cinfo&format=json&formatversion=2 | Commons — 동영상 날짜 범주 이름 후보 확인 (Videos by day 하위 5,242개) | 2026-10-10 |
| 57 | https://commons.wikimedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:Videos%20by%20day&cmtype=subcat&cmlimit=30&cmstartsortkeyprefix=1969&format=json | Commons — Videos by day 하위 범주 이름 형식 ("Videos taken on YYYY-MM-DD") | 2026-10-10 |
| 58 | https://commons.wikimedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:Videos%20by%20date&cmlimit=30&format=json | Commons — Videos by date 구성 (century·day·decade·month·year) | 2026-10-10 |
| 59 | https://commons.wikimedia.org/w/api.php?action=query&titles=Category:Videos%20taken%20on%201988-09-17%7CCategory:Videos%20taken%20on%202002-06-22%7CCategory:Videos%20taken%20on%201995-03-15%7CCategory:Videos%20taken%20on%201950-06-25%7CCategory:Videos%20taken%20on%201969-07-20%7CCategory:Videos%20taken%20on%201986-09-20%7CCategory:Videos%20taken%20on%202000-02-29%7CCategory:Videos%20taken%20on%202014-04-16%7CCategory:1988-09-17%7CCategory:2002-06-22%7CCategory:1995-03-15%7CCategory:1950-06-25%7CCategory:1969-07-20%7CCategory:1986-09-20%7CCategory:2000-02-29%7CCategory:2014-04-16&prop=categoryinfo&format=json&formatversion=2 | Commons — 표본 8개 날짜의 동영상·일반 날짜 범주 크기 | 2026-10-10 |
| 60 | https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category:Videos%20taken%20on%201969-07-20&gcmtype=file&gcmlimit=50&prop=imageinfo&iiprop=mime%7Cmediatype%7Csize%7Curl&format=json&formatversion=2 | Commons — Videos taken on 1969-07-20 파일 1개 (`duration` 202.272) | 2026-10-10 |
| 61 | https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category:Videos%20taken%20on%202014-04-16&gcmtype=file&gcmlimit=50&prop=imageinfo&iiprop=mime%7Cmediatype&format=json&formatversion=2 | Commons — Videos taken on 2014-04-16 파일 2개 (SDO 태양 관측) | 2026-10-10 |
| 62 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%221969-07-20%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 1969-07-20 (56건) | 2026-10-10 |
| 63 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%222014-04-16%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 2014-04-16 (5건, 슬로베니아 시위) | 2026-10-10 |
| 64 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%221988-09-17%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 1988-09-17 (0건) | 2026-10-10 |
| 65 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%222002-06-22%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 2002-06-22 (0건) | 2026-10-10 |
| 66 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%221950-06-25%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 1950-06-25 (0건) | 2026-10-10 |
| 67 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%221986-09-20%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 1986-09-20 (0건) | 2026-10-10 |
| 68 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%221995-03-15%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 1995-03-15 (0건) | 2026-10-10 |
| 69 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%222000-02-29%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat 2000-02-29 (0건) | 2026-10-10 |
| 70 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20insource%3A%2F%5C%7Cdate%20%2A%3D%20%2A1988-09-17%2F&srnamespace=6&srlimit=50&format=json | Commons 검색 — 정규식 insource 1988-09-17 (0건) | 2026-10-10 |
| 71 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20insource%3A%221988-09-17%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — insource "1988-09-17" (0건) | 2026-10-10 |
| 72 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20insource%3A%221969-07-20%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — insource "1969-07-20" (7건) | 2026-10-10 |
| 73 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20insource%3A%222002-06-22%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — insource "2002-06-22" (0건) | 2026-10-10 |
| 74 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20insource%3A%221986-09-20%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — insource "1986-09-20" (0건) | 2026-10-10 |
| 75 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20insource%3A%221995-03-15%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — insource "1995-03-15" (0건) | 2026-10-10 |
| 76 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20insource%3A%221950-06-25%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — insource "1950-06-25" (0건) | 2026-10-10 |
| 77 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20insource%3A%222000-02-29%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — insource "2000-02-29" (0건) | 2026-10-10 |
| 78 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20insource%3A%222014-04-16%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — insource "2014-04-16" (40건, 모두 무관) | 2026-10-10 |
| 79 | https://commons.wikimedia.org/w/api.php?action=query&titles=File:President_Nixon_speaking_with_astronauts_Armstrong_and_Aldrin_on_the_Moon.webm&prop=videoinfo&viprop=url%7Csize%7Cmime%7Cmediatype%7Cderivatives%7Cextmetadata&viurlwidth=640&viextmetadatafilter=DateTimeOriginal%7CLicenseShortName%7CLicenseUrl%7CArtist%7CCredit%7CImageDescription%7CCopyrighted%7CAttributionRequired%7CUsageTerms%7CRestrictions%7CNonFree%7CObjectName&format=json&formatversion=2 | Commons videoinfo 실제 응답 — 닉슨 통화 영상 (원본·변환본 4개·포스터·길이·PD) | 2026-10-10 |
| 80 | https://commons.wikimedia.org/w/api.php?action=query&titles=File:AP11_FINAL_APPROACH.ogv&prop=videoinfo&viprop=url%7Csize%7Cmime%7Cmediatype%7Cderivatives%7Cextmetadata&viurlwidth=320&viextmetadatafilter=DateTimeOriginal%7CLicenseShortName%7CLicenseUrl%7CArtist%7CCredit%7CImageDescription%7CAttributionRequired%7CUsageTerms%7CRestrictions%7CNonFree&format=json&formatversion=2 | Commons videoinfo 실제 응답 — AP11 FINAL APPROACH (변환본 3개, 320 요청 → 250px 포스터, 촬영일 1969-07-20) | 2026-10-10 |
| 81 | https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=filetype%3Avideo%20deepcat%3A%22Apollo%2011%22%20insource%3A%221969-07-20%22&srnamespace=6&srlimit=50&format=json | Commons 검색 — deepcat Apollo 11 + insource "1969-07-20" (7건) | 2026-10-11 |
| 82 | https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=filetype%3Avideo%20incategory%3A%22Apollo%2011%22&gsrnamespace=6&gsrlimit=10&prop=imageinfo&iiprop=url%7Csize%7Cmime%7Cextmetadata&iiurlwidth=330&iiextmetadatafilter=DateTimeOriginal%7CLicenseShortName%7CArtist&format=json&formatversion=2 | Commons 검색 생성기 + imageinfo 실제 응답 — 한 번에 5개 파일의 원본·포스터(330px)·길이·라이선스·촬영일 (업로드일이 촬영일 자리에 든 예 3개, 3시간 16분·622 MB 파일) | 2026-10-11 |

이전 문서 근거(이 문서에서 "day-events.md [n]"으로 참조): [14] Wikimedia APIs Access policy(User-Agent), [19] API:Etiquette(캐시 권장), [20] Wikimedia 재단 이용 약관, [24] WDQS 사용자 설명서, [46] Commons 라이선스 정책, [47] CommonsMetadata 확장, [50] 서울 올림픽 개막식 범주 — 모두 2026-10-09 확인, `docs/research/day-events.md` 11절.
