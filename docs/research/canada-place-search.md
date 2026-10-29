# 캐나다 장소 해석·좌표 — 출처 검증

> 작업: Canada T1 · 계획서: docs/plans/canada-research.md (PM 승인 2026-10-29) · 조사 일자: 2026-10-29 · 작성: data-researcher
> 이 문서의 약관 해석은 법률 자문이 아니다. 확신도 기준은 `docs/plans/m0-source-research.md` 의 "확신도 기준"을 따른다.
> 공개 엔드포인트는 직접 호출했다(기록은 11절 "직접 호출 기록" R1~R18). 셸 요청의 User-Agent는 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)` 하나만 썼고, 이메일은 어디에도 넣지 않았다. WebFetch는 User-Agent를 바꿀 수 없어 기본값이다. 키가 필요한 Google은 문서만 읽었고 6절에 PM 명령을 남겼다. 가입, 로그인, 약관 동의, 키 사용은 하지 않았다. 내려받은 데이터 파일은 저장소 밖 임시 폴더에만 두었고 저장소에는 이 문서 하나만 넣는다.
> 한국판 `docs/research/place-search.md` 는 고치지 않는다. 그 문서의 Google 약관 판단은 오늘 다시 읽어 캐나다 기준으로 갱신했다.

## 1. 요약

- **추천 구성은 "사전 + 검색"이다.** 사전은 SGC 2021(주·준주 13개, 자치단체 단위 CSD 5,161개 [8][R10])과 CGNDB 내려받기 파일(좌표, 동네 이름 [1][R11])로 만들고, 둘 다 저장·재배포·상업 이용을 허용하는 개방 라이선스다 [6][13]. 검색은 병원 같은 지점(`SPOT`) 용으로 OpenStreetMap Nominatim 공개 서버를 1순위로 두되, 종합 확신도는 `부분 확인` 이다.
- 한국과 달리 **저장 금지가 걸림돌이 아니다.** 사전 출처와 Wikidata, GeoNames 내려받기는 저장이 허용되고, Nominatim은 오히려 "결과를 캐시하라"고 요구한다 [28]. 걸리는 쪽은 Google이다(캐시 금지, Google 지도 필수) [19][20].
- **캐나다에는 전국 공통 "동네" 목록이 없다.** CGNDB에서 동네는 "비법인 지명(UNP)" 안에 섞여 있고 [2], 토론토 158개, 밴쿠버 22개, 몽레알 참조 동네 91개, 캘거리 313개처럼 도시마다 기준이 다르다 [R14][R16][R15][R17]. "Hochelaga-Maisonneuve" 는 몽레알 공식 참조 동네 목록에서 Hochelaga, Maisonneuve, Préfontaine 으로 나뉘어 있다 [R15].
- 병원은 CGNDB에 없고 [R5], GeoNames에도 12개뿐이다 [R12]. 병원 사전 후보인 ODHF(1,140곳, 밴쿠버 종합병원 포함)는 2019~2020년 자료라 이후 변화를 모른다 [17][R13].
- 제품 정의서와 다른 점: "동 코드 + 외부 장소 검색" 구도(한국판)가 "SGC·CGNDB 사전 + Nominatim" 으로 바뀐다. M2a 계약에서 바꿀 필드 목록은 7절에 있다.

## 2. 확인할 질문과 답

질문 번호는 계획서 "T1 작업별 후보와 질문"의 번호다. M0 제품 정의서 7절 항목(병원 이름 검색 정확도, 장소 카테고리, 쿼터, 약관)은 아래 질문에 함께 들어 있다(질문 1, 3, 7). 출처 이름은 3절 카드 제목을 줄여 썼다. 확인 일자는 모두 2026-10-29다.

| # | 질문 | 출처 | 답 | 확신도 | 근거 |
|---|---|---|---|---|---|
| 1 | `REGION`/`NEIGHBORHOOD`/`SPOT` 은 캐나다에서 무엇에 대응하나 | SGC 2021 | `REGION` = 주·준주(13)와 CSD(5,161). 별도로 인구조사 구획(CD 293)이 있으나 이름이 "Division No. 1" 같은 번호형이 많아 사용자 입력과 맞지 않는다. 동네는 CSD 아래 "Place name(s)" 요소로 이름만 있다(34,285행, 좌표 없음) | 확인됨 | [8][R10] |
| 1 | 같은 질문 | CGNDB | `Concise Code` 로 나눈다. `PROV`/`TERR`/`CITY`/`TOWN`/`VILG`/`HAM`/`UTM`/`LTM`/`STM`/`MUN1`/`MUN2` = `REGION` 후보. `UNP`(비법인 지명) = 동네 후보이나 시골 마을과 도로 이름("Victoria Road", "Victoria Line")도 `UNP` 라서 동네만 가려낼 수 없다. NRCan은 동네가 비법인 지명 범주에 들어간다고 적는다. 병원 분류 코드는 50개 목록에 없다 | 확인됨 | [2][R1][R7] |
| 1 | 같은 질문 | Nominatim | 응답의 `category`/`type`/`addresstype`/`place_rank` 로 나눈다. 데모에서 본 값: Calgary = `boundary/administrative`, `addresstype=city`. Hochelaga-Maisonneuve = `boundary/neighbourhood`, `addresstype=neighbourhood`. Vancouver General Hospital = `amenity/hospital`. 다만 공식 문서는 이 값이 일관되지 않을 수 있다고 적는다 | 샘플: 확인됨 / 일반 규칙: 부분 확인 | [29][R9] |
| 1 | 같은 질문 | Google | 주소 구성요소 `locality`, `administrative_area_level_1` 이 `REGION`, `neighborhood`/`sublocality` 가 동네, `hospital` 이 `SPOT` 이다. `locality`, `administrative_area_level_1`, `hospital` 은 Place types의 Table A(필터로 쓸 수 있음), `neighborhood`, `sublocality` 는 Table B(응답에만 나옴)다. 캐나다 실제 값은 6절 G1~G9로 확인한다 | 필드: 확인됨 / 실제 결과: 부분 확인 (PM 호출 대기) | [24][25] |
| 1 | 같은 질문 | 도시 열린 데이터 | 토론토 "Neighbourhoods"(158개), 밴쿠버 "Local area boundary"(22개), 몽레알 "Quartiers de référence en habitation"(91개), 캘거리 "Community District Boundaries"(313개)가 경계 다각형이다. 전국 공통 기준이 없고 도시마다 이름·개수·정의가 다르다 | 확인됨 | [R14][R16][R15][R17] |
| 2 | 프랑스어·영어 이름과 악센트 | CGNDB API | "Montreal" 과 "Montréal" 로 같은 질의를 보내면 23건씩 같은 결과가 왔다(악센트 무시). "Quebec" 과 "Québec" 도 같은 5건이었다. 주 `PROV` "Quebec" 과 시 `CITY` "Québec" 이 한 질의에 함께 온다. 약어도 맞춘다: "Saint John" 질의에 "Fort St. John" 이 걸렸다. 응답의 `language.code` 는 관찰한 항목에서 `und`(미정)나 `mis`(여러 언어)뿐이었고, 한 도시의 영어·프랑스어 이름을 쌍으로 주는지는 확인하지 못했다. 프랑스어 경로 `/fr/geonames` 가 따로 응답한다 | 확인됨 (관찰) | [R4][R3][R2] |
| 2 | 같은 질문 | CGNDB 내려받기 | 영어판과 프랑스어판 CSV가 따로 있다. 영어 파일은 UTF-8(BOM 포함)이고 악센트가 유지된다("Québec"). 주 이름 약어는 `term` 에 있다(`QC`, `NL` 등) | 확인됨 | [1][7][R7][R11] |
| 2 | 같은 질문 | SGC 2021 | 영어 이름 CSV는 ISO-8859-1(Latin-1) 인코딩이고 악센트가 있다("Montréal"). 프랑스어 파일도 있다. 구현은 인코딩을 명시해 읽어야 한다 | 확인됨 | [10][R10] |
| 2 | 같은 질문 | Nominatim | `accept-language` 로 이름 언어를 고른다. "Hochelaga-Maisonneuve, Montréal" 질의가 1건을 돌려줬다. 악센트 없는 철자의 동작은 시험하지 않았다 | 부분 확인 | [29][R9] |
| 3 | 같은 이름 후보를 목록으로 받나. 질문 장소: London, Victoria, Saint John | SGC 2021 | 이름이 정확히 같은 CSD 수: Victoria 5개(뉴펀들랜드 1, PEI 2, 매니토바 1, BC 1), London 1개(온타리오), Saint John 1개(뉴브런즈윅), St. John's 1개. 구획(CD) 이름도 Victoria 2개(NS, NB)가 있다 | 확인됨 | [R10] |
| 3 | 같은 질문 | CGNDB API | `num` 기본 100, 최대 1,000, `skip` 으로 쪽을 넘긴다. 도시급만 걸러 쓰는 `concise` 필터가 된다. 기본 응답에는 예전 공식명(`category=H`)이 섞이므로 `category=O` 로 걸러야 한다. "London" + 시·읍 급 `concise` 필터 → 현재 공식 `CITY`(온타리오) 1건 + 예전 공식명 1건. "Victoria" 는 필터 없이 100건 중 `CITY`(BC) 1, `TOWN`(뉴펀들랜드) 1 외에 `UNP` 가 많다. 후보를 줄이려면 `concise` 필터가 필요하다. 응답에 `relevance` 숫자가 있으나 의미는 문서에 없다 | 확인됨 | [3][R1][R2] |
| 3 | 같은 질문 | Nominatim | `limit` 최대 40, `countrycodes` 는 강제 필터다. `countrycodes=ca`: London 2건(온타리오 시, 그리고 `place/county` 로 표시된 런던 항목 1건), Victoria 4건(BC 시, NB의 Victoria County, PEI의 Rural Municipality of Victoria, 뉴펀들랜드 마을), Saint John 2건(시, 카운티). 필터 없이 "London" 은 3건(Greater London, City of London, 온타리오) | 확인됨 | [29][R9] |
| 3 | 같은 질문 | Google | Text Search는 쪽당 최대 20, 합쳐서 최대 60건이다. Geocoding은 최대 건수 설명이 없다. 캐나다 실제 후보 수는 G5, G6로 확인한다 | 한도: 확인됨 / 실제: 부분 확인 (PM 호출 대기) | [23][24] |
| 4 | 문을 닫았거나 이름이 바뀐 병원 | CGNDB | 병원은 분류에 없다. "Vancouver General Hospital" 질의 0건, "hospital" 질의 35건은 모두 만·호수·곶 같은 지형이었다. 과거 이름은 `category=H`(예전 공식명)로 걸러 볼 수 있으나 지명 한정이다 | 확인됨 | [3][R5] |
| 4 | 같은 질문 | ODHF | 2019~2020년 수집 자료의 고정판(v1.1)이다. 개업·폐업 상태 필드가 없고 "빠짐없는 자료가 아니다"라고 적는다. 그 이후 생기거나 없어진 병원은 모른다 | 확인됨 | [17][R13] |
| 4 | 같은 질문 | Nominatim | "Calgary General Hospital" 질의가 "Peter Lougheed Centre"(`amenity/hospital`) 1건을 돌려줬다. 이전 이름이 현재 시설로 연결되는 경우가 있을 수 있으나, 이 결과가 질의한 옛 병원과 같은 곳인지는 판단하지 않았다 | 샘플: 확인됨 / 의미: 미확인 | [R9] |
| 4 | 같은 질문 | Wikidata | "Calgary General Hospital" 이 `former hospital in Canada` 로 항목(Q5019676)이 있고 좌표(P625), 국가(P17=Q16), 유형(P31=Q16917)이 있다. 폐업 연도(P576)나 설립 연도는 이 항목에 없었다. 병원 개수·폐업 정보의 일반적 커버리지는 모른다 | 샘플: 확인됨 / 커버리지: 부분 확인 | [49][R18] |
| 4 | 같은 질문 | Google Places | `businessStatus`(`OPERATIONAL`, `CLOSED_TEMPORARILY`, `CLOSED_PERMANENTLY`, `FUTURE_OPENING`)와 이전 시 `movedPlaceId` 를 준다. 폐업한 캐나다 병원이 검색에 남는지는 G9로 확인한다 | 필드: 확인됨 / 동작: 부분 확인 (PM 호출 대기) | [26] |
| 5 | 캐나다 밖 장소 판정 | CGNDB / SGC | 둘 다 캐나다 자료뿐이라 외국 이름은 "없음" 으로만 나온다. 이름 없음과 캐나다 밖을 구별하지 못한다. Geolocator에 "Paris France" 를 넣으면 캐나다의 "Paris Lake" 같은 지명만 왔다 | 확인됨 | [1][R8] |
| 5 | 같은 질문 | Nominatim | 응답 `address.country_code`(`ca`/`gb`)가 있다. 필터 없이 "London" 을 보내면 `gb` 와 `ca` 가 섞여 온다. 따라서 "사전에 없으면 필터 없는 검색을 한 번 해서 결과 국가를 본다"는 방식이 가능하다 | 확인됨 (관찰) | [29][R9] |
| 5 | 같은 질문 | Google | 주소 구성요소 `country` 가 있고, Geocoding `components=country:CA` 는 강제 필터다. Places Text Search의 `regionCode` 는 서식과 편향용이다 | 확인됨 | [23][24] |
| 6 | 장소에서 시간대를 알아낼 수 있나 | GeoNames 내려받기 | 장소마다 IANA 시간대 id 가 있다. 캐나다 파일에서 Calgary = `America/Edmonton`, St. John's = `America/St_Johns`, Montréal = `America/Toronto`, London(온타리오) = `America/Toronto`, Victoria(BC) = `America/Vancouver`, Saint John(NB) = `America/Moncton` | 확인됨 | [47][48][R12] |
| 6 | 같은 질문 | Google Time Zone API | 좌표와 시각으로 시간대 id 와 UTC 오프셋을 준다. 키가 필요하고 월 10,000건 무료다. 과거 시각이 되는지는 문서에 없다 | 부분 확인 | [52][21] |
| 6 | 같은 질문 | CGNDB / SGC / Nominatim / Wikidata | 열어 본 응답과 파일에 시간대 필드가 없었다. Wikidata 두 항목(Victoria, 밴쿠버 종합병원)에도 시간대 속성(P421)이 없었다 | 확인됨 (없음) | [R1][R9][R10][R18] |
| 6 | 같은 질문 | IANA tz | 시간대 데이터베이스 최신판은 2026f(2026-10-27)이고 "지역 시간의 역사"를 담는다고 적는다. 날짜별 오프셋은 이 데이터로 계산하는 것이 일반적이나, 캐나다 지역의 어느 해부터 정확한지는 문서에 없다 | 부분 확인 | [51] |
| 6 | 같은 질문 | (결론) | PM 결정 2 (a)(상한을 `America/St_Johns` 의 오늘로)가 확정이라 장소별 시간대는 지금 필요하지 않다. 나중에 (b)로 바꾸면 사전 항목에 `tz` 한 열을 더하는 길이 있다(GeoNames 값 또는 Google) | — | 계획서 결정 2 |
| 7 | 좌표계 | CGNDB | 위도·경도 십진 각도이고 GeoJSON 점(`position.coordinates` = [경도, 위도])도 준다. 측지계 이름은 읽은 문서에 없다 | 부분 확인 | [3][R1] |
| 7 | 같은 질문 | Nominatim / Google | `lat`/`lon` 과 `latitude`/`longitude`(도 단위)다. 측지계 이름은 읽은 문서에 없다 | 부분 확인 | [29][26] |
| 7 | 같은 질문 | StatCan 경계 파일 | 람베르트 정각원추도법, NAD83, GRS 1980이다. EPSG 번호는 쪽에 없다. 웹 지도와 쓰려면 변환이 필요하다 | 확인됨 | [16] |
| 7 | 같은 질문 | 토론토·캘거리 | 토론토 자원 이름에 `4326` 과 `2952` 가 붙은 파일이 따로 있다. 캘거리 카탈로그 메타데이터는 `EPSG:4326 WGS84` 라고 적는다. 밴쿠버·몽레알은 좌표계 표기를 찾지 못했다 | 토론토·캘거리: 확인됨 / 밴쿠버·몽레알: 미확인 | [R14][R17] |
| 7 | 키 | 출처별 | 키 필요: Google. 키 없음: CGNDB, SGC, StatCan 파일, Nominatim(User-Agent 필수), 토론토·몽레알·밴쿠버·캘거리 API, GeoNames 내려받기, Wikidata, ODHF. GeoNames 웹 서비스는 무료 계정(`username`)이 필요해서 쓰지 않는다 | 확인됨 | [R1][R9][R14]~[R17][46] |
| 7 | 쿼터 | CGNDB API | 숫자 한도 없음("제한 미기재"). 지오로케이션 서비스는 캐시 응답을 쓴다고 적는다. 대량 조회는 NRCan에 문의하라고 한다 | 미기재 = 미확인 | [3][4] |
| 7 | 같은 질문 | Nominatim | 초당 1회가 절대 상한이다. 하루 넘게 반복하는 일괄 작업은 분당 4회로 제한한다. 자동완성은 금지다. 용량이 작고 접근이 철회될 수 있다 | 확인됨 | [28] |
| 7 | 같은 질문 | Google | Geocoding 월 10,000건 무료(이후 1,000건당 5.00달러), Text Search Pro 월 5,000건 무료(32.00달러), Enterprise 월 1,000건(35.00달러), IDs만 받는 Essentials 무제한 무료 | 확인됨 | [21] |
| 7 | 같은 질문 | 도시 API | 밴쿠버 응답 헤더 `X-RateLimit-Limit: 15000`(단위는 문서에 없음). 토론토·몽레알은 한도 표기를 찾지 못했다. 로컬 데모 규모는 내려받기로 처리하는 편이 안전하다 | 부분 확인 | [R16][R14][R15] |
| 7 | 약관 | 출처별 | 3절 카드의 "라이선스·약관 핵심". 요약: 사전 출처는 모두 저장·변경·상업 이용 허용 + 출처 표시, Nominatim은 캐시 의무 + ODbL 표시, Google은 캐시 금지(`place_id` 예외) + 지도에 올리면 Google 지도 | 카드별 | 3절 |
| 8 | 테스트 fixture를 실제 응답 녹화로 만들 수 있나 | 출처별 | 저장이 허용되는 출처는 날짜·원본 URL·라이선스를 붙인 녹화 fixture가 가능하다(`CLAUDE.md` 7절 5항): CGNDB, SGC, ODHF(OGL-Canada), 토론토(OGL-Toronto), 몽레알(CC BY 4.0), GeoNames(CC BY 4.0), Wikidata(CC0). Nominatim은 OSM(ODbL) 자료라 소량 발췌는 "공정 이용" 범위라고 정책이 말하지만 [28], 파생 데이터베이스 해석은 10절 D2로 올렸다. Google은 합성 fixture만. 밴쿠버·캘거리는 라이선스 본문을 못 읽어 합성 fixture만 | 부분 확인 | [6][13][34][38][47][49][28][19] |

### 커버리지 시작 표

| 항목 | 가장 이른 기간 | 지역 단위 | 정밀도 | 갱신 주기 | 근거 |
|---|---|---|---|---|---|
| SGC 2021 목록 | 해당 없음 — 2021년 기준 분류(현재 지명) | 주·준주 13, CD 293, CSD 5,161 | 해당 없음 — 날짜가 아닌 분류 | 필요할 때(레코드 수정 2023-04-21) | [8][10] |
| 인구조사 경계 파일 | 2021 인구조사 기준 | CSD, CMA/CA, CT, DA | 해당 없음 — 날짜가 아닌 경계 | 필요할 때(레코드 수정 2024-02-15) | [15][16] |
| CGNDB 내려받기 | 현재 승인 이름 약 350,000개. 결정일은 1897년부터 | 전국 지명(주·준주별 파일) | 해당 없음 — 현재 이름과 좌표. 결정일(`Decision Date`)이 일 단위로 있음 | 매주 | [1][2][R11] |
| NRCan Geoname·Geolocator API | 같음(예전 공식명 약 37,000개 포함) | 전국 지명 | 해당 없음 | 서비스 문서 수정 2026-03-19 | [3][4] |
| Google Geocoding·Places | 해당 없음 — 현재 데이터 | 전국 | 해당 없음 | 미기재 | [22] |
| Nominatim(OSM) | 해당 없음 — 현재 지도 데이터 | 전국(세계) | 해당 없음 | 미확인 | [28] |
| 토론토 동네 | 현재 158개 구획. "예전 140개" 판도 있음 | 토론토 | 해당 없음 | 비정기(마지막 갱신 2026-02-20) | [R14] |
| 몽레알 참조 동네 | 현재 91개 | 몽레알 | 해당 없음 | 레코드 수정 2026-10-28 | [R15] |
| 밴쿠버 지역 구획 | "경계는 변하지 않는다"고 적음 | 밴쿠버 22개 | 해당 없음 | 레코드 수정 2023-06-24 | [R16] |
| 캘거리 커뮤니티 구획 | 해당 없음 — 현재 313개 | 캘거리 | 해당 없음 | 비정기(카탈로그 갱신 2026-10-01) | [R17] |
| GeoNames 캐나다 | 현재 317,133행 | 전국 | 해당 없음 | 매일 | [45][R12] |
| Wikidata | 해당 없음 — 항목별 | 전세계 | 해당 없음 | 상시 | [49] |
| ODHF 의료시설 | 2019-11~2020-06 수집 | 전국(7,033곳, 병원 1,140곳) | 해당 없음 | 고정판 v1.1(갱신 주기 미기재) | [17][R13] |

## 3. 출처 카드

### 출처 — Statistics Canada 표준지리분류(SGC) 2021 목록
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: Statistics Canada / https://www.statcan.gc.ca/en/subjects/standard/sgc/2021/introduction [8], 내려받기 쪽 [9], 데이터셋 기록 [10]
- 무엇을 얻나: 분류 구조 CSV(`Level, Hierarchical structure, Code, Class title`)와 요소 CSV(`Level, Code, Class title, Element Type Label English, Element Description English`). 구조 CSV는 지리 지역 6, 주·준주 13, 구획(CD) 293, 자치단체 구획(CSD) 5,161행이다. 요소 CSV의 "Place name(s)" 행(34,285행)은 CSD 아래 지명을 준다. 예: Montréal(2466023) 아래 88개(Hochelaga-Maisonneuve, Hochelaga, Maisonneuve, Rosemont 등), Calgary(4806016) 아래 180개, Vancouver(5915022) 아래 27개(Fairview, Kitsilano 등) [R10]. 좌표는 없다.
- 접근 방법: 파일 다운로드(CSV, 영어·프랑스어)
- 인증·키: 필요 없음(내려받기 성공)
- 요금·쿼터: 무료
- 커버리지: 기준 연도 2021. 코드는 7자리(주 2 + 구획 2 + CSD 3) [8]. 예: Calgary 4806016, Montréal 2466023, Vancouver 5915022, St. John's 1001519, London 3539036, Saint John 1301006 [R10]. 2026 판은 찾지 못했다(공식 쪽은 2021이 현행) [8].
- 날짜 정밀도: 해당 없음 — 날짜가 아닌 분류
- 응답 메타데이터: 좌표 없음. 라이선스 필드는 파일에 없고 데이터셋 기록에 있다 [10]. 인코딩 ISO-8859-1 [R10].
- 라이선스·약관 핵심:
  - 화면 표시, 캐싱·저장, 변경(가공), 재배포: Statistics Canada Open Licence가 이용·복제·게시·배포·판매와 가공해 새 제품을 만드는 것을 허용한다 [13].
  - 상업 이용: 허용(같은 라이선스) [13][14].
  - 출처 표시: 표준 문구 "Source: Statistics Canada, 제품 이름, 기준일. Reproduced and distributed on an "as is" basis with the permission of Statistics Canada." 가공 제품은 "Adapted from Statistics Canada, 제품 이름, 기준일. This does not constitute an endorsement by Statistics Canada of this product." [13]
  - 제외: 제3자 권리, 개인·기업을 식별하려고 다른 데이터베이스와 연결하는 것 [13].
- 이 서비스에 맞는가: **적합** (이름·코드 사전의 뼈대) — 비상업일 때와 상업일 때 모두 같다. 좌표와 병원은 못 준다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [9][R10] 2026-10-29 |
  | 인증·키 | 확인됨 (없음) | [R10] 2026-10-29 |
  | 요금·쿼터 | 확인됨 (무료, 제한 표기 없음) | [9] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [8][R10] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 — 날짜가 아닌 분류 | — |
  | 캐싱·저장 (필수) | 확인됨 | [10][13] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [13] 2026-10-29 |
  | 상업 이용 (필수) | 확인됨 | [13][14] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님. 데이터 가공은 허용 [13] | [13] 2026-10-29 |
- 확신도: 확인됨
- 샘플 응답 확인: [R10] 구조 CSV와 요소 CSV를 2026-10-29에 받았다.
- 근거 링크: [8][9][10][13][14][R10]

### 출처 — Statistics Canada 인구조사 경계 파일 (2021)
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: Statistics Canada / https://www150.statcan.gc.ca/n1/pub/92-160-g/92-160-g2021001-eng.htm [16], 데이터셋 기록 [15]
- 무엇을 얻나: CSD, CMA/CA, 인구조사 구역(CT), 배포 구역(DA)의 경계 다각형. CSD 파일은 식별자, DGUID, 이름, 유형, 면적, 주 식별자(PRUID)를 담는다. 해안선 단순화판(cartographic)과 전체 영역판(digital)이 있다 [16].
- 접근 방법: 파일 다운로드(Shapefile, GML, File Geodatabase)와 Esri REST·WMS 서비스 [15]
- 인증·키: 필요 없음(문서상). 내려받기 쪽은 이 도구로는 403이 나와 실제 내려받기는 해 보지 못했다.
- 요금·쿼터: 무료. 쿼터 미기재
- 커버리지: 2021 인구조사 기준 전국. 이 용도에서는 좌표로 CSD를 찾는 "점이 어느 구획에 있나" 계산에 쓸 수 있다(작업 2에서 확인할 일).
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: 좌표계 람베르트 정각원추도법 NAD83(GRS 1980), EPSG 번호는 쪽에 없음 [16]. 확대하면 정확하지 않다는 경고가 있다 [16].
- 라이선스·약관 핵심: 데이터셋 기록은 Open Government Licence – Canada [15], 안내서는 Statistics Canada Open Licence [16]. 두 라이선스 모두 복제·가공·상업 이용을 허용한다 [6][13]. 출처 표시는 위 SGC 카드와 같다.
- 이 서비스에 맞는가: **조건부** — 사전에 "좌표 → CSD" 역조회가 필요할 때만 쓴다. 파일 크기와 변환 비용은 확인하지 못했다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 부분 확인 — 형식은 문서로 확인, 실제 내려받기는 도구가 403이라 못 해 봄 | [15][16] 2026-10-29 |
  | 인증·키 | 부분 확인 | [15] 2026-10-29 |
  | 요금·쿼터 | 확인됨 (무료) | [15] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [16] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 확인됨 | [6][13][15] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [6][13] 2026-10-29 |
  | 상업 이용 (필수) | 확인됨 | [6][13] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 부분 확인
- 샘플 응답 확인: 해당 없음 — 파일을 받지 못했다. 6절 B6 참조
- 근거 링크: [6][13][15][16]

### 출처 — CGNDB(캐나다 지명 데이터베이스) 내려받기 파일
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: Natural Resources Canada / https://open.canada.ca/data/en/dataset/e27c6eba-3c5d-4051-9db2-082dc6411c2c [1], 소개 [2], 파일 목록 [7]
- 무엇을 얻나: 현재 승인된 지명 약 350,000개(그밖에 예전 공식명 약 37,000, 해저 지명 약 3,500) [2]. CSV 열: `CGNDB ID, Geographical Name, ISO Language Code, Language, Syllabic Form, Generic Term, Generic Category, Concise Code, Toponymic Feature ID, Latitude, Longitude, Location, Province - Territory, Relevance at Scale, Decision Date, Source` [R11]. 동네는 `UNP` 범주에 있다 [2].
- 접근 방법: 파일 다운로드(주·준주별 CSV/KML/SHP, 전국 합본, 영어·프랑스어), Esri REST·WMS [1][7]
- 인증·키: 필요 없음(뉴펀들랜드 파일 내려받기 성공) [R11]
- 요금·쿼터: 무료
- 커버리지: 전국, 매주 갱신, 시간 범위 1897년~현재 [1]. 뉴펀들랜드 파일 33,150행 중 `TOWN` 270, `CITY` 3, `UNP` 1,358 [R11]. 병원은 분류에 없다 [R7][R5].
- 날짜 정밀도: 해당 없음 — 현재 이름. 결정일(`Decision Date`)만 일 단위로 있음
- 응답 메타데이터: 위도·경도(십진 각도, 측지계 이름은 못 읽음), 라이선스 필드 없음(데이터셋 기록에 있음)
- 라이선스·약관 핵심: 데이터셋은 Open Government Licence – Canada [1]. 이 라이선스는 복제·가공·배포·상업 이용을 "어떤 합법 목적으로든" 허용하고, 출처는 제공자가 정한 문구를 쓰고 없으면 "Contains information licensed under the Open Government Licence – Canada." 를 쓴다 [6]. 공식 심볼·이름·로고와 개인정보는 제외하고, 공식 보증을 암시할 수 없다 [6]. 데이터셋 쪽에는 별도 표시 문구가 없다 [1]. 사회적으로 부적절한 옛 용어가 들어 있다는 경고가 있다 [1][2].
- 이 서비스에 맞는가: **적합** (REGION·동네 사전의 좌표 원천) — 비상업일 때와 상업일 때 모두 같다. 단 `UNP` 안에서 동네만 가려내려면 사전 큐레이션이 필요하고, SGC 코드와의 연결 열이 없다. 리서처가 한 간이 계산(공식 문서 아님, 이름의 악센트·"St."/"Saint"·하이픈만 접어서 비교): 전국 합본 CSV(366,235행)와 SGC CSD 5,161개를 (주, 이름) 으로 맞추면 3,329개(약 65%)가 시·읍 급 지명 1건과 유일하게 맞았고, 170개는 같은 이름의 `TOWN` + `MUN1` 처럼 2건 이상이 걸렸고, 1,662개는 맞는 지명이 없었다(앞쪽 예가 "Division No. 1, Subd. V" 같은 번호형 이름). 따라서 CSD 코드와 좌표를 잇는 일에는 수동 확인이 필요하다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [1][7][R11] 2026-10-29 |
  | 인증·키 | 확인됨 (없음) | [R11] 2026-10-29 |
  | 요금·쿼터 | 확인됨 (무료) | [1] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [1][2][R11] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 — 현재 이름 | — |
  | 캐싱·저장 (필수) | 확인됨 | [1][6] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [6] 2026-10-29 |
  | 상업 이용 (필수) | 확인됨 | [6] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님. 가공은 허용 [6] | [6] 2026-10-29 |
- 확신도: 확인됨
- 샘플 응답 확인: [R11] 뉴펀들랜드 CSV를 2026-10-29에 받아 열 이름과 행을 확인했다.
- 근거 링크: [1][2][6][7][R11]

### 출처 — NRCan Geoname Service와 Geolocator API
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: Natural Resources Canada / https://natural-resources.canada.ca/maps-tools-publications/maps/geographical-names-canada/geoname-service-api [3], https://natural-resources.canada.ca/maps-tools-publications/satellite-elevation-air-photos/geolocation-service [4]
- 무엇을 얻나:
  - Geoname Service(`https://geogratis.gc.ca/services/geoname/en/geonames`): CGNDB 이름 검색. 매개변수 `q`(`*` 와일드카드), `bbox`, `lat`·`lon`·`radius`(1~100 km), `province`, `category`(`O` 공식, `H` 예전 공식), `concise`, `theme`, `sort-field`, `num`(1~1,000, 기본 100), `skip` [3]. 응답 JSON 항목: `id`, `name`, `language`, `category`, `status`, `concise`, `generic`, `location`, `province`, `latitude`, `longitude`, `bbox`, `relevance`, `accuracy`, `position`(GeoJSON 점), `decision`, `map` [R1]. `codes/concise`(50개), `codes/province`(15개: 주·준주 13 + 해저 지형 72, 국제 해역 73) 목록도 준다. 주 코드는 SGC 주 코드와 같다(10, 11, 12, 13, 24, 35, 46, 47, 48, 59, 60, 61, 62) [R7][8].
  - Geolocator(`https://geolocator.api.geo.ca/`): `q`, `lang`, `keys`(`geonames`, `nominatim`, `locate`, `fsa`, `nts`)를 받고 평평한 JSON 배열을 준다. 항목마다 `key`, `name`, `province`, `category`, `lat`, `lng`, `bbox`, `tag` 가 있다 [4][R8]. 옛 Geolocation API(`geogratis.gc.ca/services/geolocation/en/locate`)는 2027-03-31에 끝난다 [4].
- 접근 방법: REST API (JSON, CSV, KML, GML, GeoJSON)
- 인증·키: 필요 없음(문서에 언급 없음, 키 없이 호출 성공) [R1][R8]. CORS 지원 [4].
- 요금·쿼터: 무료. 숫자 한도 미기재, 캐시 응답을 쓴다고 적음. 대량 조회는 문의하라고 안내 [3][4]
- 커버리지: 전국. 예전 공식명 포함 [3]. 병원은 CGNDB에 없다. Geolocator는 병원 질의를 OSM(Nominatim)으로 넘긴다: "Vancouver General Hospital" 질의에서 `nominatim` 항목이 맨 앞에 왔다 [R8].
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: 좌표, `decision` 날짜. 라이선스 필드 없음
- 라이선스·약관 핵심:
  - 두 서비스 문서에 라이선스 문구가 없다. 링크된 NRCan 일반 약관은 정부 저작물을 상업적으로 재배포하려면 사전 서면 허가가 필요하다고 하면서 "달리 명시된 경우는 예외"라고 적는다 [5]. 일반 약관 쪽은 지리 데이터·웹 서비스의 라이선스를 정하지 않는다 [5].
  - 같은 데이터의 내려받기 파일은 OGL-Canada [1]. API 응답에도 같은 라이선스가 적용되는지는 문서에서 확인하지 못했다(10절 D1).
  - Geolocator는 OSM 자료(`nominatim` 키)를 섞어 준다 [4]. 그 부분은 OSM(ODbL) 조건이 따라붙는다.
- 이 서비스에 맞는가: **조건부** — 비상업일 때: 실행 중에 부르지 않고 내려받기 파일로 사전을 만들면 쿼터 문제도 약관 문제도 피한다. API는 예전 이름(`category=H`)을 찾거나 좌표 주변 동네를 찾을 때만 쓴다. 상업일 때: API 응답의 라이선스를 NRCan에 확인해야 한다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [3][4][R1][R8] 2026-10-29 |
  | 인증·키 | 확인됨 (없음) | [R1][R8] 2026-10-29 |
  | 요금·쿼터 | 부분 확인 — 무료이나 한도 미기재 | [3][4] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [3][R1][R5] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 부분 확인 — API 응답의 라이선스가 문서에 없음, 10절 D1 | [3][4][5] 2026-10-29 |
  | 출처 표시 (필수) | 부분 확인 — OGL-Canada 기본 문구가 적용되는지 미확인 | [6] 2026-10-29 |
  | 상업 이용 (필수) | 부분 확인 — 일반 약관과 OGL의 관계가 애매함, 10절 D1 | [5][6] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 부분 확인
- 샘플 응답 확인: R1~R8 (요청 URL과 확인한 필드는 11절)
- 근거 링크: [3][4][5][6][R1][R2][R3][R4][R5][R6][R7][R8]

### 출처 — Google Geocoding API와 Places API (New) Text Search
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: Google / https://developers.google.com/maps/documentation/geocoding/requests-geocoding [24], https://developers.google.com/maps/documentation/places/web-service/text-search [23], 정책 [19][20], 요금 [21], 커버리지 [22]
- 무엇을 얻나: Geocoding은 `address_components`(`locality`, `sublocality`, `administrative_area_level_1~7`, `country`), `types`(`neighborhood` 포함), `geometry`, `place_id`, `partial_match` 를 준다 [24]. Text Search는 `id`, `displayName`, `formattedAddress`, `addressComponents`, `location`, `types`, `primaryType`, `businessStatus`, `movedPlaceId` 를 준다 [23][26].
- 접근 방법: REST API
- 인증·키: 필요. Google Cloud API 키와 결제 계정(`$GOOGLE_MAPS_SERVER_KEY`). 서버 키
- 요금·쿼터: Geocoding 월 10,000건 무료(이후 1,000건당 5.00달러), Text Search Pro 월 5,000건(32.00달러), Enterprise 월 1,000건(35.00달러), Essentials(ID만) 무제한 무료, Time Zone 월 10,000건 [21]
- 커버리지: 공식 표에서 캐나다는 Geocoding "좋은 데이터 품질"이다. 이 표에 Places 열은 없다 [22]. 캐나다 실제 후보·결과는 6절 명령으로 확인한다.
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: 좌표(도 단위, 측지계 이름 미확인), `place_id`. 라이선스 필드 없음. 폐업 상태 필드 있음 [26]
- 라이선스·약관 핵심: (2026-10-28 갱신된 정책 쪽을 오늘 다시 읽었다)
  - 화면 표시: 결과를 지도에 올리면 Google 지도 위에 보여야 한다. 지도 없이 쓸 때는 Google 로고(공간이 좁으면 "Google Maps" 문구)를 붙인다 [19][20].
  - 캐싱·저장: 허용된 예외를 넘어 미리 받거나 캐시하거나 저장하면 안 된다. `place_id` 는 예외로 무기한 저장할 수 있다. 위도·경도 캐시 기간은 서비스별 약관에 있다는데 그 본문을 읽지 못했다 [19][20][27].
  - 상업 이용: 유료 SKU 체계는 확인했으나 서비스 약관 본문은 읽지 못했다.
- 이 서비스에 맞는가: **조건부** — 비상업일 때: 캐시 금지와 "Google 지도 필수" 때문에 사전 구성과 맞지 않는다. 사전에서 못 찾은 입력의 보조 검색으로만 의미가 있다. 상업일 때: 같다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [23][24] 2026-10-29 |
  | 인증·키 | 확인됨 (키 필요) | [23][24] 2026-10-29 |
  | 요금·쿼터 | 확인됨 | [21] 2026-10-29 |
  | 커버리지 (필수) | 부분 확인 (PM 호출 대기) — Geocoding 캐나다 품질은 확인, Places 품질과 후보는 PM 명령 대기 | [22] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 부분 확인 — 일반 금지와 `place_id` 예외는 확인, 좌표 캐시 기간 조항은 못 읽음 | [19][20][27] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [19][20] 2026-10-29 |
  | 상업 이용 (필수) | 부분 확인 — 약관 본문을 못 읽음 | [21][27] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 부분 확인 (PM 호출 대기)
- 샘플 응답 확인: 6절 명령 G1~G9 참조
- 근거 링크: [19][20][21][22][23][24][25][26][27][52]

### 출처 — OpenStreetMap Nominatim 공개 서버
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: OpenStreetMap Foundation·Nominatim / https://nominatim.org/release-docs/latest/api/Search/ [29], 이용 정책 https://operations.osmfoundation.org/policies/nominatim/ [28], 저작권 [30], 표시 지침 [31]
- 무엇을 얻나: `GET https://nominatim.openstreetmap.org/search` 가 `place_id`, `osm_type`, `osm_id`, `lat`, `lon`, `category`, `type`, `addresstype`, `place_rank`, `importance`, `name`, `display_name`, `boundingbox`, `address`(`addressdetails=1` 일 때, `state`, `ISO3166-2-lvl4`, `country_code` 포함), `licence` 를 준다 [29][R9].
- 접근 방법: REST API (`format=jsonv2` 등)
- 인증·키: 필요 없음. 식별 가능한 User-Agent 또는 Referer 가 필수 [28]
- 요금·쿼터: 무료. 초당 1회가 절대 상한이다. 하루 넘게 이어지는 일괄 작업은 분당 4회로 제한된다. 자동완성 금지. "용량이 매우 작은 기부 서버"이고 접근이 철회될 수 있다. `limit` 최대 40 [28][29]
- 커버리지: 세계. 캐나다 데모 4곳과 동명 3곳은 모두 응답했다(11절 R9). 병원 전반의 커버리지는 시험하지 못했다.
- 날짜 정밀도: 해당 없음 — 현재 지도 데이터
- 응답 메타데이터: 좌표 `lat`/`lon`(측지계 이름은 읽은 문서에 없음), 응답 `licence` 필드: "Data © OpenStreetMap contributors, ODbL 1.0." [R9]
- 라이선스·약관 핵심:
  - 캐싱·저장: 정책이 "결과를 직접 캐시해야 한다"고 적는다. 같은 질의를 반복하면 차단될 수 있다 [28]. 한편 ODbL은 데이터를 바꾸거나 이를 토대로 만든 결과를 같은 라이선스로만 배포하게 한다 [30]. 표시 지침은 "파생 데이터베이스(Derivative Database)를 이루지 않는 한 검색 결과 묶음에 결과마다 표시를 붙일 필요는 없다"고 한다 [31]. 우리 캐시나 사전이 파생 데이터베이스인지는 해석이 필요하다(10절 D2).
  - 출처 표시: 응용에 "OpenStreetMap" 을 밝히고 ODbL 조건을 알리며 저작권 쪽으로 링크한다. "© OpenStreetMap contributors" 형태도 허용된다 [31].
  - 상업 이용: ODbL은 허용한다 [30]. 정책은 유료 고객에게 서비스하는 상업 앱은 정책 변경과 접근 철회를 감안하라고 적는다 [28].
  - 변경(가공): 소량 발췌는 "공정 이용" 범위로 볼 수 있다고 정책이 적는다. 큰 가공은 같은 라이선스 조건이다 [28][30].
- 이 서비스에 맞는가: **조건부** — 비상업일 때: 로컬 데모 규모(요청 1초 1회, 캐시)에서는 잘 맞는다. 병원(`SPOT`)을 키 없이 찾는 유일한 후보다. 공개 배포나 상업화하면 자체 서버나 유료 제공자를 다시 봐야 한다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [29][R9] 2026-10-29 |
  | 인증·키 | 확인됨 (없음, User-Agent 필수) | [28] 2026-10-29 |
  | 요금·쿼터 | 확인됨 | [28][29] 2026-10-29 |
  | 커버리지 (필수) | 부분 확인 — 데모 7곳은 확인, 병원 전반은 미확인 | [R9] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 부분 확인 — 캐시 의무는 확인, 파생 데이터베이스 해석은 애매함, 10절 D2 | [28][30][31] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [31] 2026-10-29 |
  | 상업 이용 (필수) | 부분 확인 — ODbL은 허용이나 공개 서버 정책에 단서가 있음 | [28][30] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 부분 확인
- 샘플 응답 확인: R9 (요청 URL 형태와 확인한 필드는 11절)
- 근거 링크: [28][29][30][31][R9]

### 출처 — 토론토 열린 데이터 "Neighbourhoods"
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: City of Toronto / https://open.toronto.ca/dataset/neighbourhoods/ , 데이터 API (CKAN) `https://ckan0.cf.opendata.inter.prod-toronto.ca/api/3/action/package_show?id=neighbourhoods` [R14], 라이선스 https://open.toronto.ca/open-data-licence/ [34]
- 무엇을 얻나: 시 지정 사회계획 동네 158개의 경계(GeoJSON, CSV, SHP, GPKG; 좌표계 4326과 2952 판). "예전 140개" 판도 있다. 설명은 이 동네들이 역사적 동네를 모두 포괄하지는 않는다고 적는다 [R14]
- 접근 방법: REST API(CKAN)와 파일 다운로드
- 인증·키: 필요 없음
- 요금·쿼터: 무료, 한도 표기 없음
- 커버리지: 토론토 한 도시. 마지막 갱신 2026-02-20
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: 라이선스 필드(`open-government-licence-toronto`) 있음 [R14]
- 라이선스·약관 핵심: OGL-Toronto v1.0은 복제·가공·게시·배포를 상업 이용 포함 "어떤 합법 목적으로든" 허용한다. 출처 문구가 없으면 "Contains information licensed under the Open Government Licence – Toronto." 를 쓰고 가능하면 라이선스를 링크한다. 개인정보, 시의 이름·로고·상징은 제외하며 시가 보증한다는 암시는 금지다 [34].
- 이 서비스에 맞는가: **적합** (데모 도시 4곳 밖이라 지금은 필요 없음. 토론토 사용자를 위한 동네 사전 후보) — 비상업일 때와 상업일 때 같다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [R14] 2026-10-29 |
  | 인증·키 | 확인됨 (없음) | [R14] 2026-10-29 |
  | 요금·쿼터 | 부분 확인 — 무료, 한도 미기재 | [R14] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [R14] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 확인됨 | [34] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [34] 2026-10-29 |
  | 상업 이용 (필수) | 확인됨 | [34] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님. 가공은 허용 [34] | [34] 2026-10-29 |
- 확신도: 확인됨 (필수 항목이 모두 확인됨. 쿼터 행은 필수가 아니라서 종합에 넣지 않았다.)
- 샘플 응답 확인: R14
- 근거 링크: [34][R14]

### 출처 — 몽레알 열린 데이터 "Quartiers de référence en habitation" 외
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: Ville de Montréal(Données Québec 포털에 올린 기록) / 기록 `https://www.donneesquebec.ca/recherche/api/3/action/package_show?id=vmtl-quartiers` [R15], 라이선스 CC BY 4.0 [38]
- 무엇을 얻나: 몽레알을 주거 분석용 역사·분석 단위로 나눈 91개 참조 동네(GeoJSON, SHP, CSV, 번호 `no_qr`, 이름 `nom_qr`, 구 번호 `no_arr`, 구 이름 `nom_arr`). 구(arrondissement) 22번 Mercier–Hochelaga-Maisonneuve 아래 동네는 Beaurivage, Tétreaultville, Dupéré, Guybourg, Longue-Pointe, Louis-Riel, Maisonneuve, Hochelaga, Préfontaine 9개다. 합쳐서 하나인 "Hochelaga-Maisonneuve" 라는 동네는 이 자료에 없다 [R15]. 행정 경계(구·도시)는 별도 자료 "Limites administratives de l'agglomération de Montréal" 이 있다 [R15].
- 접근 방법: REST API(CKAN datastore)와 파일 다운로드
- 인증·키: 필요 없음
- 요금·쿼터: 무료, 한도 표기를 찾지 못함
- 커버리지: 몽레알 한 도시. 레코드 수정 2026-10-28
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: `license_title` "Attribution (CC-BY 4.0)" [R15]. 좌표계 표기를 찾지 못함
- 라이선스·약관 핵심: CC BY 4.0은 복제·가공·상업 이용을 허용하고, 적절한 출처 표시, 라이선스 링크, 변경 표시를 요구한다 [38].
- 이 서비스에 맞는가: **적합** (데모 입력 "Hochelaga-Maisonneuve" 의 경계 기준을 정할 때 선택이 필요함, 10절 D3) — 비상업일 때와 상업일 때 같다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [R15] 2026-10-29 |
  | 인증·키 | 확인됨 (없음) | [R15] 2026-10-29 |
  | 요금·쿼터 | 미확인 — 한도 표기를 찾지 못함 | [R15] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [R15] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 확인됨 | [R15][38] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [38] 2026-10-29 |
  | 상업 이용 (필수) | 확인됨 | [38] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님. 가공은 허용 [38] | [38] 2026-10-29 |
- 확신도: 확인됨
- 샘플 응답 확인: R15
- 근거 링크: [38][R15]

### 출처 — 밴쿠버 열린 데이터 "Local area boundary"
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: City of Vancouver / `https://opendata.vancouver.ca/api/explore/v2.1/catalog/datasets/local-area-boundary` [R16], 라이선스 쪽 https://opendata.vancouver.ca/pages/licence/ [41]
- 무엇을 얻나: 시의 22개 "지역 계획 구역"(local area) 경계. 열: `name`, `geom`, `geo_point_2d`. 설명은 경계가 도로 중심선을 따르고 변하지 않는다고 적는다. 점-다각형 질의가 된다: 밴쿠버 종합병원 좌표를 넣으면 `Fairview` 1건이 돌아왔다 [R16].
- 접근 방법: REST API(Opendatasoft v2.1)와 내보내기
- 인증·키: 필요 없음
- 요금·쿼터: 응답 헤더 `X-RateLimit-Limit: 15000`(단위 설명 없음) [R16]
- 커버리지: 밴쿠버 한 도시. 레코드 수정 2023-06-24
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: `license` "Open Government Licence - Vancouver", `license_url` 있음 [R16]
- 라이선스·약관 핵심: 라이선스 본문 쪽이 자바스크립트로만 그려져 도구로 읽지 못했다 [41]. 라이선스 이름과 링크는 메타데이터로 확인했으나 이용·표시·상업 조건은 못 읽었다. 오픈스트리트맵 도움말 글 등 2차 출처에는 BC 라이선스 2.0 기반이라는 언급이 있으나 근거로 쓰지 않는다.
- 이 서비스에 맞는가: **미정** — 라이선스 본문을 확인하기 전까지 판단하지 않는다. 구조는 이 용도에 맞는다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [R16] 2026-10-29 |
  | 인증·키 | 확인됨 (없음) | [R16] 2026-10-29 |
  | 요금·쿼터 | 부분 확인 | [R16] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [R16] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 미확인 — 라이선스 본문을 못 읽음 | [41] 2026-10-29 |
  | 출처 표시 (필수) | 미확인 | [41] 2026-10-29 |
  | 상업 이용 (필수) | 미확인 | [41] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 미확인
- 샘플 응답 확인: R16
- 근거 링크: [41][R16]

### 출처 — 캘거리 열린 데이터 "Community District Boundaries"
- 구분: 계획서 후보
- 제공 기관 / 공식 문서: The City of Calgary / `https://data.calgary.ca/resource/surr-xmvs.json` , 카탈로그 `https://data.calgary.ca/api/catalog/v1` [R17], 약관 쪽 https://data.calgary.ca/d/Open-Data-Terms/u45n-7awa [44]
- 무엇을 얻나: 313개 커뮤니티 구획(열 `class`, `class_code`, `comm_code`, `name`, `sector`, `multipolygon`). 이름은 대문자로 온다(예: "MACEWAN GLEN"). `class` 로 주거·산업을 구분한다 [R17]. 카탈로그 메타데이터는 좌표계를 `EPSG:4326 WGS84` 로 적는다.
- 접근 방법: REST API(Socrata)와 내보내기
- 인증·키: 필요 없음(이 호출은 앱 토큰 없이 응답함)
- 요금·쿼터: 미확인
- 커버리지: 캘거리 한 도시. 갱신 비정기
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: `license` 가 "See Terms of Use" 라고만 적혀 있다 [R17]
- 라이선스·약관 핵심: 약관 쪽이 자바스크립트로만 그려져 본문을 읽지 못했다 [44]. 이용·표시·상업 조건을 확인하지 못했다.
- 이 서비스에 맞는가: **미정** — 약관 본문을 확인하기 전까지 판단하지 않는다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [R17] 2026-10-29 |
  | 인증·키 | 부분 확인 — 앱 토큰 없이 응답했으나 정책 문서를 못 읽음 | [R17] 2026-10-29 |
  | 요금·쿼터 | 미확인 | [R17] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [R17] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 미확인 — 약관 본문을 못 읽음 | [44] 2026-10-29 |
  | 출처 표시 (필수) | 미확인 | [44] 2026-10-29 |
  | 상업 이용 (필수) | 미확인 | [44] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 미확인
- 샘플 응답 확인: R17
- 근거 링크: [44][R17]

### 출처 — GeoNames 내려받기 (캐나다)
- 구분: 추가 후보
- 제공 기관 / 공식 문서: GeoNames / https://www.geonames.org/export/ [45], 내려받기 설명 https://download.geonames.org/export/dump/readme.txt [47], 웹 서비스 https://www.geonames.org/export/web-services.html [46]
- 무엇을 얻나: 캐나다 파일(`CA.zip`)에 317,133행. 열: 이름, 좌표, 지형 구분(`feature class`/`feature code`), 주 코드(`admin1`), 인구, IANA 시간대 id, 수정일 [47][R12]. 시간대가 있는 것이 이 후보의 이유다. 병원 코드(`HSP`)는 캐나다 12행뿐이다 [R12]. 주 코드가 SGC와 다른 번호 체계다(예: Alberta `01`, Quebec `10`, Ontario `08`).
- 접근 방법: 파일 다운로드. 웹 서비스는 무료 계정의 `username` 이 필요해서 쓰지 않는다 [46].
- 인증·키: 내려받기는 필요 없음
- 요금·쿼터: 무료. 웹 서비스는 앱당 하루 10,000 크레딧, 시간당 1,000 크레딧 [45]
- 커버리지: 전국, 매일 갱신 [45]
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: 좌표(측지계 이름은 못 읽음), 라이선스는 readme에 있다
- 라이선스·약관 핵심: Creative Commons Attribution 4.0 [47]. 데이터나 웹 서비스를 쓰면 GeoNames를 링크나 문구로 밝히라고 안내한다 [45]. CC BY 4.0은 복제·가공·상업 이용 허용 + 출처·링크·변경 표시 [38].
- 이 서비스에 맞는가: **조건부** — 시간대가 꼭 필요해지면(결정 2를 (b)로 바꿀 때) 쓸 수 있다. 지금은 필요 없다. 이름 철자는 CGNDB가 더 공식적이다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [47][R12] 2026-10-29 |
  | 인증·키 | 확인됨 (내려받기는 없음) | [R12] 2026-10-29 |
  | 요금·쿼터 | 확인됨 | [45] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [R12] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 확인됨 | [47][38] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [45][38] 2026-10-29 |
  | 상업 이용 (필수) | 확인됨 | [38][47] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 확인됨
- 샘플 응답 확인: R12
- 근거 링크: [38][45][46][47][R12]

### 출처 — Wikidata 장소 항목
- 구분: 추가 후보
- 제공 기관 / 공식 문서: Wikimedia / https://www.wikidata.org/wiki/Wikidata:Licensing [49], API `https://www.wikidata.org/w/api.php` [R18]. 호출 규칙은 `docs/research/day-events.md` 의 Wikidata 카드와 같다.
- 무엇을 얻나: `wbsearchentities` 로 이름 검색, `wbgetentities` 로 속성. 샘플에서 Victoria(Q2132)와 밴쿠버 종합병원(Q7914187)에 좌표(P625), 국가(P17=Q16), 소속 행정구역(P131), 유형(P31)이 있었다. 이름 검색은 전세계라 사람·배·식물이 섞인다("Victoria" 12건 중 캐나다 도시 1건). 시간대(P421)는 두 항목 모두에 없었다 [R18].
- 접근 방법: REST API
- 인증·키: 필요 없음, 식별 가능한 User-Agent 필수
- 요금·쿼터: 무료. 규칙에 맞는 User-Agent는 분당 200회, 식별 안 되는 요청은 분당 10회, 초과 시 HTTP 429 [53]. Wikidata Query Service 쪽 제한은 이 호출에서 쓰지 않았다(`day-events.md` 참조)
- 커버리지: 항목마다 다르다. 폐업 병원 1건(Calgary General Hospital, Q5019676)이 "former hospital in Canada" 로 있었으나 폐업 연도 속성은 없었다 [R18]. 일반적인 커버리지는 시험하지 못했다.
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: 좌표, 유형. 라이선스 필드는 없음(전체가 CC0)
- 라이선스·약관 핵심: 구조화 데이터는 CC0이다 [49]. 표시 의무 없음, 저장·가공·상업 이용 제한 없음.
- 이 서비스에 맞는가: **조건부** — 폐업한 병원이나 이름이 바뀐 병원의 보조 확인용이다. 이름 검색 잡음 때문에 1순위 검색으로는 쓰지 않는다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [R18] 2026-10-29 |
  | 인증·키 | 확인됨 (없음) | [R18] 2026-10-29 |
  | 요금·쿼터 | 확인됨 | [53] 2026-10-29 |
  | 커버리지 (필수) | 부분 확인 — 샘플 3건만 | [R18] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 확인됨 | [49] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [49] 2026-10-29 |
  | 상업 이용 (필수) | 확인됨 | [49] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 부분 확인
- 샘플 응답 확인: R18
- 근거 링크: [49][50][53][R18]

### 출처 — Statistics Canada 의료시설 공개 데이터베이스 (ODHF) v1.1
- 구분: 추가 후보
- 제공 기관 / 공식 문서: Statistics Canada / https://www.statcan.gc.ca/en/lode/databases/odhf [17]
- 무엇을 얻나: 의료시설 약 7,000곳(실측 7,033행: 병원 1,140, 외래 의료 2,227, 요양·돌봄 3,666). 열: `index, facility_name, source_facility_type, odhf_facility_type, provider, unit, street_no, street_name, postal_code, city, province, source_format_str_address, CSDname, CSDuid, Pruid, latitude, longitude` [R13]. 병원 1,140곳 중 1,047곳에 좌표가 있다. 밴쿠버 종합병원은 `CSDuid` 5915022(SGC와 같은 코드)와 좌표가 있고, 같은 건물군의 두 항목("- Gordon and Leslie Diamond Health Care Centre", "- Health Centre")도 따로 있다 [R13].
- 접근 방법: 파일 다운로드(`ODHF_v1.1.zip`, CSV와 메타데이터 PDF) [17][R13]
- 인증·키: 필요 없음
- 요금·쿼터: 무료
- 커버리지: 전국. 2019-11~2020-03 수집 + 2020-05~06 보강. "빠짐없는 자료가 아니고" 분류·좌표 오류가 있을 수 있다고 적는다 [17]. "General Hospital" 이 이름에 들어간 병원이 90곳이라 이름만으로는 겹친다 [R13].
- 날짜 정밀도: 해당 없음
- 응답 메타데이터: 좌표, CSD 코드. 라이선스 필드는 파일에 없고 쪽에 있다
- 라이선스·약관 핵심: 쪽이 Open Government Licence – Canada 를 적는다 [17]. 조건은 [6]과 같다.
- 이 서비스에 맞는가: **조건부** — 병원 이름 사전의 고정 근거로 쓸 수 있으나 2020년 이후 변화를 모른다. 이 서비스는 출생일 기준 병원을 찾으려는데 ODHF는 과거 병원 목록이 아니다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [17][R13] 2026-10-29 |
  | 인증·키 | 확인됨 (없음) | [R13] 2026-10-29 |
  | 요금·쿼터 | 확인됨 | [17] 2026-10-29 |
  | 커버리지 (필수) | 확인됨 | [17][R13] 2026-10-29 |
  | 날짜 정밀도 | 해당 없음 | — |
  | 캐싱·저장 (필수) | 확인됨 | [6][17] 2026-10-29 |
  | 출처 표시 (필수) | 확인됨 | [6] 2026-10-29 |
  | 상업 이용 (필수) | 확인됨 | [6] 2026-10-29 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 확인됨
- 샘플 응답 확인: R13
- 근거 링크: [6][17][R13]

## 4. 미디어 레코드 필드 매핑

장소 검색은 미디어를 주지 않는다. 그래서 제품 정의서 6.2절 필드 중 `lat`·`lng`·`place_ref` 행만 채우고 나머지는 `해당 없음` 으로 둔다(M0 T1 기준과 같음). 열은 라이선스 성격이 같은 출처끼리 묶었다.

| 필드 | 개방 라이선스 자료 (SGC [13], CGNDB [6], ODHF [6], 토론토 [34], 몽레알 [38], GeoNames [38], Wikidata [49]) | Nominatim (OSM) | Google |
|---|---|---|---|
| id | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| type | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| url | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| thumbnail_url | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| source_name | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| source_url | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| license | 해당 없음 — 미디어 아님. 라이선스는 데이터셋 기록에 있음(예: [1] OGL-Canada, [R14] `license_id`, [R15] `license_title`). 응답·파일에 필드 없음(CGNDB·SGC·ODHF) | 해당 없음 — 미디어 아님. 응답 `licence` 필드가 ODbL 1.0을 적음 [R9] | 해당 없음 — 미디어 아님. 응답에 라이선스 필드 없음 [23][24] |
| attribution | 해당 없음 — 미디어 아님. 장소 정보를 보여줄 때의 표시 문구는 3절 각 카드 | 해당 없음 — 미디어 아님. "OpenStreetMap" 표시 + ODbL 링크 [31] | 해당 없음 — 미디어 아님. 지도 없이 쓰면 Google 로고나 "Google Maps" [19][20] |
| display_mode | 해당 없음 — 미디어 아님. 표시 제한 조항 없음 [6][13] | 해당 없음 — 미디어 아님. 표시 제한 조항 못 찾음 [28][31] | 해당 없음 — 미디어 아님. 지도에 올리면 Google 지도 필수 [19][20] |
| captured_at | 해당 없음 — 촬영일 필드 없음 (CGNDB의 `Decision Date` 는 이름 결정일 [R11]) | 해당 없음 — 촬영일 필드 없음 [29] | 해당 없음 — 촬영일 필드 없음 [24][26] |
| date_precision | 해당 없음 — 촬영일이 없어 정밀도도 없음 | 해당 없음 — 촬영일이 없어 정밀도도 없음 | 해당 없음 — 촬영일이 없어 정밀도도 없음 |
| lat·lng 또는 place_ref | CGNDB: `lat` = `Latitude` / `latitude`, `lng` = `Longitude` / `longitude`. `place_ref` = `CGNDB ID`(5글자, 예 `JBOBQ`) [R1][R11]. SGC: 좌표 없음, `place_ref` = CSD 코드 7자리 [R10]. ODHF: `latitude`/`longitude`, `place_ref` = `index` + `CSDuid` [R13]. GeoNames: `lat`/`lon`, `place_ref` = geonameid [47]. Wikidata: P625, `place_ref` = QID [R18]. 도시 경계: 다각형(`geom` 등) [R16] | `lat` = `lat`, `lng` = `lon`, `place_ref` = `osm_type` + `osm_id` [29]. 저장 가능(캐시 의무 [28]) | `lat` = `location.latitude` 또는 `geometry.location.lat`, `place_ref` = `id`/`place_id` (무기한 저장 가능 [19][20]). 좌표 자체의 캐시 기간은 미확인 |
| retrieved_at | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |

결론: 장소 결과는 미디어가 아니므로 `display_mode` 의 `embed`/`link_only` 판단 대상이 아니다. 화면에 보여 줄 때의 조건은 다음과 같다.
- 비상업일 때: 개방 라이선스 자료는 표시 제한이 없고 출처 표시만 하면 된다. Nominatim은 "OpenStreetMap" 표시와 ODbL 링크, Google은 지도 없이 쓰면 로고(좁으면 "Google Maps" 문구), 지도에 올리면 Google 지도다.
- 상업일 때: 위와 같다. 상업 이용 추가 조건은 각 카드의 "상업 이용" 행에 있다. Google 약관 본문과 밴쿠버·캘거리 약관은 읽지 못했다.

### 장소 결과 매핑 (M2a 계약 입력, 확정 아님)

| 장소 결과 필드 | SGC + CGNDB 사전 | Nominatim | Google | 확신도 |
|---|---|---|---|---|
| `type` | 계산: SGC에 있는 CSD 이름 또는 CGNDB `Concise Code` ∈ {PROV, TERR, CITY, TOWN, VILG, HAM, UTM, LTM, STM, MUN1, MUN2} → `REGION`. 큐레이션 목록에 있는 `UNP` 또는 SGC "Place name(s)" 요소 → `NEIGHBORHOOD`. 둘 다 아니면 사전에 없음 | 계산: `addresstype`/`type` 이 city·town·village·administrative 계열이면 `REGION`, `neighbourhood`·`suburb` 면 `NEIGHBORHOOD`, `category=amenity` 이면 `SPOT`. 규칙은 데모 샘플에서만 확인 | 계산: `types` 의 `locality`·`administrative_area_level_1` → `REGION`, `neighborhood`·`sublocality` → `NEIGHBORHOOD`, `hospital` → `SPOT` | 사전: 확인됨 (샘플) / Nominatim: 부분 확인 / Google: 부분 확인 (PM 호출 대기) |
| `name` | CGNDB `Geographical Name` (악센트 유지) 또는 SGC `Class title` | `name` (`accept-language=en`) | `displayName.text` / `address_components[].long_name` | 확인됨 |
| `full_name` | 계산: `<이름>, <주 이름>`(예: "Victoria, British Columbia"). 주 이름은 SGC 또는 `codes/province` | 계산: `name` + `address.state` | 계산: `formattedAddress`(프로그램으로 분석하지 말라고 [24] 적음) | 확인됨 |
| `address` | 해당 없음 (`SPOT` 만) | `display_name` | `formattedAddress` | 확인됨 |
| `lat` / `lng` | CGNDB 좌표(CSD 대표점). 한국판의 `null` 이 사라진다 | `lat` / `lon` | `location.latitude` / `longitude` | 확인됨 |
| 주·준주 코드 | SGC 2자리(예 `48`, `59`), CGNDB `province` 코드와 같음 [R7][8] | 채울 수 없음 — 코드 없음. `address.ISO3166-2-lvl4`(예 `CA-AB`)로 변환 필요 | 채울 수 없음 — 코드 없음. `administrative_area_level_1` 의 `short_name` 으로 변환 | 확인됨 |
| CSD 코드 | SGC 7자리. CGNDB와의 연결은 (주, 이름) 맞추기 + 수동 확인 | 채울 수 없음. 좌표 → CSD는 StatCan 경계 파일의 점-다각형으로 계산하는 방법이 있음(미시험) | 채울 수 없음 | 사전: 확인됨 / 연결: 부분 확인 |
| 외부 장소 ID | CGNDB ID, SGC 코드 | `osm_type` + `osm_id` | `id` / `place_id` | 확인됨 |

## 5. 데모 입력 판단

날짜는 이 영역과 무관하다(장소 해석은 날짜를 쓰지 않는다). 날짜는 결정 1 (b)의 공개된 역사적 사건일이다(실제 사람의 생일이 아님). 모든 행의 실제 결과는 2026-10-29에 직접 호출한 값이고, Google 행만 `부분 확인 (PM 호출 대기)` 다.

| 데모 입력 (결정 1 (b)) | 출처 | 얻을 수 있는 것 | 얻을 수 없는 것 | 근거 | 확신도 |
|---|---|---|---|---|---|
| 1988-02-13 + Calgary | SGC + CGNDB 사전 | 유형 `REGION`. SGC CSD 4806016, 주 48(Alberta). CGNDB `CITY` "Calgary"(좌표 51.0458, -114.0575, 후보 1건, 도시급 필터 `concise`) | 날짜별 정보는 해당 없음 | [R10][R2] | 확인됨 |
| 1988-02-13 + Calgary | Nominatim | `boundary/administrative`, `addresstype=city`, 51.0456, -114.0575, 1건 | CSD 코드 | [R9] | 확인됨 |
| 1988-02-13 + Calgary | GeoNames | `PPL`, 인구 1,306,784, 좌표 51.0501, -114.0853, 시간대 `America/Edmonton` | CSD 코드. 좌표가 CGNDB와 달라 도시 대표점은 출처마다 다르다 | [R12] | 확인됨 |
| 1988-02-13 + Calgary | Google Geocoding | 유형, `place_id`, 좌표 | 실제 결과는 아직 모름 | [24] | 부분 확인 (PM 호출 대기), 명령 G1 |
| 1976-07-17 + Hochelaga-Maisonneuve, Montréal | SGC + CGNDB 사전 | 유형 `NEIGHBORHOOD`. SGC 요소 CSV에서 CSD 2466023(Montréal) 아래 "Hochelaga-Maisonneuve", "Hochelaga", "Maisonneuve", "Mercier--Hochelaga-Maisonneuve" 가 있음. CGNDB `UNP` "Hochelaga-Maisonneuve"(45.5647, -73.5389). `q=Hochelaga` 6건 중 구(`TOWN` "Mercier–Hochelaga-Maisonneuve"), 선거구(`MUN2`), 공원 등이 섞여 있어 이름만으로는 `UNP` 를 가려야 한다 | 쉼표 뒤 "Montréal" 로 도시를 좁히는 규칙은 직접 만들어야 함 | [R10][R5] | 확인됨 |
| 1976-07-17 + Hochelaga-Maisonneuve, Montréal | Nominatim | `boundary/neighbourhood`, 45.5515, -73.5400, 1건 (질의: 쉼표와 악센트 포함) | 경계가 몽레알 공식 참조 동네와 같은지 | [R9] | 확인됨 |
| 1976-07-17 + Hochelaga-Maisonneuve, Montréal | 몽레알 열린 데이터 | 참조 동네 91개 중 Hochelaga, Maisonneuve, Préfontaine 이 별개이고 Mercier–Hochelaga-Maisonneuve 구에 속함 | 한 이름으로 합친 단일 경계 | [R15] | 확인됨 |
| 1976-07-17 + Hochelaga-Maisonneuve, Montréal | GeoNames | `PPL` 좌표 45.5587, -73.5374, 시간대 `America/Toronto` | — | [R12] | 확인됨 |
| 1976-07-17 + Hochelaga-Maisonneuve, Montréal | Google Geocoding | 유형(`neighborhood` 여부), 좌표 | 실제 결과 | [24] | 부분 확인 (PM 호출 대기), 명령 G2 |
| 2010-02-12 + Vancouver General Hospital | CGNDB / SGC 사전 | 병원 항목은 없다(질의 0건). 좌표가 있으면 주변 동네는 찾는다: 반경 2 km 질의에서 `Fairview`, `South Cambie`, `Shaughnessy` | 병원 자체 | [R5][R6] | 확인됨 |
| 2010-02-12 + Vancouver General Hospital | Nominatim | `amenity/hospital`, 49.2618, -123.1227, 주 British Columbia, 1건 | 개업·폐업 상태 | [R9] | 확인됨 |
| 2010-02-12 + Vancouver General Hospital | ODHF | 이름 "Vancouver General Hospital"(병원 종류 `general`), CSD 5915022, 49.2616, -123.1239. 같은 이름의 부속 두 항목도 있음 | 2020년 이후 상태 | [R13] | 확인됨 |
| 2010-02-12 + Vancouver General Hospital | Wikidata | Q7914187, 유형 hospital, 49.2612, -123.123, 설립 1906 | 시간대 | [R18] | 확인됨 |
| 2010-02-12 + Vancouver General Hospital | 밴쿠버 열린 데이터 | 병원 좌표를 넣은 점-다각형 질의가 `Fairview` 를 돌려줌 | 라이선스 본문 | [R16] | 데이터: 확인됨 / 이용 조건: 미확인 |
| 2010-02-12 + Vancouver General Hospital | Google Places | 이름, 주소, 좌표, `businessStatus` | 실제 결과 | [23][26] | 부분 확인 (PM 호출 대기), 명령 G8 |
| 1980-04-12 + St. John's | SGC + CGNDB 사전 | 유형 `REGION`. CSD 1001519, 주 10(Newfoundland and Labrador). `q=St. John's` 18건 중 도시급 `CITY` 1건(47.4822, -52.7971). 나머지는 만·곶·군사시설 | 대표점이 Nominatim과 약 10 km 다름(Nominatim 47.5647, -52.7067) | [R10][R3][R9] | 확인됨 |
| 1980-04-12 + St. John's | Nominatim | `addresstype=city`, 1건 | — | [R9] | 확인됨 |
| 1980-04-12 + St. John's | GeoNames | 시간대 `America/St_Johns`(뉴펀들랜드 시간대) | — | [R12] | 확인됨 |
| 1980-04-12 + St. John's | Google Geocoding | 유형, 좌표 | 실제 결과 | [24] | 부분 확인 (PM 호출 대기), 명령 G7 |
| (장소 이름만) London | SGC / CGNDB / Nominatim | CSD 1개(온타리오 3539036). CGNDB 현재 공식 `CITY` 1건(+ 예전 공식명 1건). Nominatim `countrycodes=ca` 2건(그중 1건은 `place/county` 로 표시된 중복성 항목). 필터 없는 질의는 영국 2건 + 온타리오 1건 | 캐나다 밖 런던을 후보로 낼지는 제품 결정(10절 D4) | [R10][R2][R9] | 확인됨 |
| (장소 이름만) Victoria | SGC / CGNDB / Nominatim | CSD 5개(뉴펀들랜드, PEI 2개, 매니토바, BC). CGNDB `CITY`(BC) 1 + `TOWN`(뉴펀들랜드) 1 + `UNP` 다수. Nominatim 4건(BC 시, NB 카운티, PEI 지방자치체, 뉴펀들랜드 마을) | 사용자가 의도한 곳 — 후보 목록으로 고르게 해야 한다(제품 정의서 4절) | [R10][R1][R9] | 확인됨 |
| (장소 이름만) Saint John | SGC / CGNDB / Nominatim | CSD 1개(뉴브런즈윅 1301006). CGNDB `CITY` 2건(Saint John NB, Fort St. John BC — "Saint" 와 "St." 가 같은 글자로 맞춰짐). Nominatim 2건(시, 카운티). "St. John's" 는 별개로 온다 | — | [R10][R3][R9] | 확인됨 |

## 6. PM이 실행할 확인 명령

사전·검색 1순위 출처(SGC, CGNDB, Nominatim, 도시 열린 데이터, GeoNames, Wikidata, ODHF)는 공개 엔드포인트라 모두 직접 확인했다(11절). 키가 필요한 것은 Google뿐이다. 아래는 Google 명령과, 도구로 읽지 못한 쪽을 PM이 브라우저로 볼 목록이다.

공통 규칙:
- 키는 환경변수로만 쓴다: `GOOGLE_MAPS_SERVER_KEY` (서버 전용이라 `VITE_` 없음). 이전에 쓰던 `YOUTUBE_API_KEY` 는 쓰지 않는다.
- 비용: G1~G7은 Geocoding 호출 1건씩(월 10,000건 무료), G8~G9는 Text Search Pro 이상 호출 1건씩(월 5,000건 무료, `businessStatus` 의 정확한 등급은 문서에 없어 Enterprise일 수 있으며 월 1,000건 무료) [21]. 합계 9건.
- 응답 원문은 저장소에 커밋하지 않는다. "응답에서 확인할 것" 값만 data-researcher에게 넘긴다.
- 질의는 결정 1 (b)의 데모 장소와 동명 확인용 3개 이름만 쓴다.

### Google (G1~G9)

```bash
# G1 — 근거: https://developers.google.com/maps/documentation/geocoding/requests-geocoding
curl -sS -G "https://maps.googleapis.com/maps/api/geocode/json" \
  --data-urlencode "address=Calgary" \
  --data-urlencode "components=country:CA" \
  --data-urlencode "language=en" \
  --data-urlencode "key=$GOOGLE_MAPS_SERVER_KEY" | head -c 3000
# 응답에서 확인할 것: status(OK), results 개수, results[0].types, address_components[].types 와 short_name(AB 등), geometry.location, place_id
```

```bash
# G2 — 근거: 같은 문서
curl -sS -G "https://maps.googleapis.com/maps/api/geocode/json" \
  --data-urlencode "address=Hochelaga-Maisonneuve, Montréal" \
  --data-urlencode "components=country:CA" \
  --data-urlencode "language=en" \
  --data-urlencode "key=$GOOGLE_MAPS_SERVER_KEY" | head -c 3000
# 응답에서 확인할 것: results[0].types 에 neighborhood 가 있는지, address_components 에서 동네·구·도시·주가 어느 타입인지, partial_match 유무
```

```bash
# G3 — 근거: 같은 문서 (components 없이: 국가 판정 확인)
curl -sS -G "https://maps.googleapis.com/maps/api/geocode/json" \
  --data-urlencode "address=London" \
  --data-urlencode "language=en" \
  --data-urlencode "key=$GOOGLE_MAPS_SERVER_KEY" | head -c 3000
# 응답에서 확인할 것: results 개수, 첫 결과의 country 구성요소(short_name), 캐나다(CA) 결과가 포함되는지
```

```bash
# G4 — 근거: 같은 문서
curl -sS -G "https://maps.googleapis.com/maps/api/geocode/json" \
  --data-urlencode "address=London" \
  --data-urlencode "components=country:CA" \
  --data-urlencode "language=en" \
  --data-urlencode "key=$GOOGLE_MAPS_SERVER_KEY" | head -c 3000
# 응답에서 확인할 것: results 개수(1 기대), administrative_area_level_1 의 short_name(ON 기대)
```

```bash
# G5 — 근거: 같은 문서 (동명 후보)
curl -sS -G "https://maps.googleapis.com/maps/api/geocode/json" \
  --data-urlencode "address=Victoria" \
  --data-urlencode "components=country:CA" \
  --data-urlencode "language=en" \
  --data-urlencode "key=$GOOGLE_MAPS_SERVER_KEY" | head -c 3000
# 응답에서 확인할 것: results 개수(SGC 기준 CSD 5개, 4개는 시골·소규모), 결과마다 주(short_name), 결과 사이의 순서
```

```bash
# G6 — 근거: 같은 문서 (동명 후보)
curl -sS -G "https://maps.googleapis.com/maps/api/geocode/json" \
  --data-urlencode "address=Saint John" \
  --data-urlencode "components=country:CA" \
  --data-urlencode "language=en" \
  --data-urlencode "key=$GOOGLE_MAPS_SERVER_KEY" | head -c 3000
# 응답에서 확인할 것: results 개수, 첫 결과 주(NB 기대), "St. John's"(NL)가 섞여 오는지
```

```bash
# G7 — 근거: 같은 문서
curl -sS -G "https://maps.googleapis.com/maps/api/geocode/json" \
  --data-urlencode "address=St. John's" \
  --data-urlencode "components=country:CA" \
  --data-urlencode "language=en" \
  --data-urlencode "key=$GOOGLE_MAPS_SERVER_KEY" | head -c 3000
# 응답에서 확인할 것: 첫 결과의 주(NL 기대), geometry.location, results 개수
```

```bash
# G8 — 근거: https://developers.google.com/maps/documentation/places/web-service/text-search (includedType, regionCode, FieldMask)
curl -sS -X POST "https://places.googleapis.com/v1/places:searchText" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: $GOOGLE_MAPS_SERVER_KEY" \
  -H "X-Goog-FieldMask: places.id,places.displayName,places.formattedAddress,places.addressComponents,places.location,places.types,places.primaryType,places.businessStatus" \
  -d '{"textQuery":"Vancouver General Hospital","languageCode":"en","regionCode":"CA","includedType":"hospital"}' | head -c 3000
# 응답에서 확인할 것: places[0].displayName.text, primaryType, formattedAddress, location(49.26, -123.12 근처 기대), businessStatus, addressComponents 의 province short_name(BC)
```

```bash
# G9 — 근거: 같은 문서 (폐업·개명 병원)
curl -sS -X POST "https://places.googleapis.com/v1/places:searchText" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: $GOOGLE_MAPS_SERVER_KEY" \
  -H "X-Goog-FieldMask: places.id,places.displayName,places.formattedAddress,places.businessStatus,places.movedPlaceId" \
  -d '{"textQuery":"Calgary General Hospital","languageCode":"en","regionCode":"CA"}' | head -c 3000
# 응답에서 확인할 것: places 개수, businessStatus(CLOSED_PERMANENTLY 가 오는지), movedPlaceId 유무, 첫 결과 이름이 옛 병원인지 현재 병원인지
```

### 키 없이 PM이 브라우저로 볼 것 (도구로 읽지 못한 공식 페이지)

| # | URL | 확인할 것 |
|---|---|---|
| B1 | https://cloud.google.com/maps-platform/terms/maps-service-terms | Geocoding API·Places API 절의 위도·경도 캐시 허용 기간과 조건 |
| B2 | https://opendata.vancouver.ca/pages/licence/ | "Open Government Licence – Vancouver" 본문: 복제·가공·상업 이용 허용 여부, 출처 표시 문구 |
| B3 | https://data.calgary.ca/d/Open-Data-Terms/u45n-7awa | 캘거리 열린 데이터 약관 본문: 복제·가공·상업 이용 허용 여부, 출처 표시 문구 |
| B4 | https://www.donneesquebec.ca/licence/ | 몽레알 자료의 CC BY 4.0 표시 문구 안내(쪽 본문이 비어 읽히지 않음) |
| B5 | https://natural-resources.canada.ca/maps-tools-publications/maps/geographical-names-canada/geoname-service-api 의 "Terms and conditions" 링크 | Geoname API·Geolocator 응답에 OGL-Canada가 적용된다는 문장이 있는지, 호출 한도 |
| B6 | https://www12.statcan.gc.ca/census-recensement/2021/geo/sip-pis/boundary-limites/index2021-eng.cfm?year=21 | CSD 경계 파일의 내려받기 링크, 파일 크기, 좌표계(EPSG) |

## 7. 이 영역 추천안

### 추천 구성: "사전 + 검색"

```
입력(q) → 정규화(NFC, 악센트·대소문자·"St."/"Saint"·하이픈/엔대시 접기)
   ├─ 1) 사전 일치(서버 안, 외부 호출 없음)
   │     REGION:       SGC 2021 주·준주 + CSD (이름·코드) + CGNDB 좌표
   │     NEIGHBORHOOD: SGC "Place name(s)" 요소 + CGNDB UNP 중 큐레이션된 이름
   │     → 일치하면 후보 목록(여러 개면 모두, 추측 금지)
   └─ 2) 사전에 없으면 검색 1회(키 없는 Nominatim, 캐시 + 초당 1회)
         SPOT(병원 등) 후보. 필터 없는 질의로 결과 국가가 캐나다가 아니면 OUTSIDE_CANADA
```

- 1순위 (사전): **SGC 2021(`확인됨`) + CGNDB 내려받기 파일(`확인됨`)**. 두 자료 모두 저장·가공·재배포·상업 이용을 허용하는 개방 라이선스(Statistics Canada Open Licence, OGL-Canada)이고 출처 표시만 요구한다 [6][13]. `data/` 에 두고 고정할 수 있어 한국판의 "저장 금지" 문제가 사라진다. 표시 문구 예: "Adapted from Statistics Canada, SGC 2021, 2021." / "Contains information licensed under the Open Government Licence – Canada."
- 1순위 (검색, `SPOT` 용): **Nominatim 공개 서버(`부분 확인`)**. 키가 없고 캐시를 하라는 정책이며, 데모의 밴쿠버 종합병원을 `amenity/hospital` 로 찾았다 [R9]. 로컬 데모의 사용량(초당 1회 이하, 캐시)에서 맞는다. 공개 배포 때는 다시 판단한다.
- 대안:
  - **Google Geocoding·Places(`부분 확인 (PM 호출 대기)`)**: 폐업 상태와 이전 정보(`businessStatus`, `movedPlaceId`)가 장점이다. 키·결제가 필요하고, 캐시 금지, 지도에 올리면 Google 지도 필수라는 제약이 있어 T2 이미지 출처 선택에 따라 충돌할 수 있다. 사전에서 찾지 못한 입력의 보조 검색으로만 의미가 있다. 6절 G1~G9 결과로 재평가한다.
  - **ODHF(`확인됨`)**: 2020년 고정판 병원 사전. 병원 이름 입력이 사전에서 바로 맞는 경우 외부 호출을 줄이지만 최신성이 없다. 원하면 `SPOT` 후보에 보조로 얹는다.
  - **Wikidata(`부분 확인`)**: 폐업·개명 병원 확인의 보조.
  - **GeoNames 내려받기(`확인됨`)**: 장소별 시간대가 필요해질 때(결정 2 (b)).
  - 도시 경계(토론토 `확인됨`, 몽레알 `확인됨`, 밴쿠버·캘거리 `미확인`): 동네 경계가 필요할 때(작업 2 이미지, 작업 6 지표)만 데모 도시 4곳에 한해 쓴다.
- 이유:
  1. 데모 4곳의 유형(`REGION` 2, `NEIGHBORHOOD` 1, `SPOT` 1)을 모두 처리하는 근거가 확인된 출처가 있다(5절).
  2. 사전 출처는 키·쿼터·약관 위험이 없고 출처 표시 문구도 공식 문서에 있다 [6][13].
  3. 병원은 어느 개방 자료에도 최신·전수 목록이 없다. 검색에 의존하게 되므로 실패 때 "동네나 도시로 다시 입력" 안내가 주 경로가 된다(제품 정의서 4절 "찾지 못하는 장소"와 같다).
  4. 한국판과 달리 "사전에 없으면 검색" 순서가 쿼터를 아끼는 목적이 아니라 `REGION` 이 가게나 건물로 해석되지 않게 하는 목적이다.

### M2a 계약(`GET /api/places/resolve`)에서 바꿀 것

| 한국판 필드·값 | 캐나다판 | 이유 |
|---|---|---|
| `sido` `{code, name}` | `province` `{code, name, abbr}`: SGC 2자리 코드(`48`), 이름 "Alberta", 약어 "AB"(`codes/province` 의 `term`) | 법정동코드 → SGC 주 코드 [8][R7] |
| `sigungu` `{code, name}` | `csd` `{code, name}`: SGC 7자리(`4806016`), 이름 "Calgary" | SGC CSD가 시·군·구 위치다. 인구·날씨 등 이후 지표의 지역 키로 쓴다 [8] |
| `status: OUTSIDE_KOREA` | `OUTSIDE_CANADA` | 국가 판정은 Nominatim `address.country_code` 로 한다(5절 질문 5). CGNDB·SGC만으로는 "없음"과 구별이 안 된다 |
| `id` 접두어 `region-<법정동코드 10자리>`, `kakao-address-<b_code>`, `kakao-place-<ID>` | `csd-<SGC 7자리>` / `province-<2자리>`, `cgndb-<ID 5글자>`, `osm-<N/W/R><osm_id>`, (선택) `google-place-<id>` | 출처마다 ID 체계가 다름 [R1][29] |
| `lat`·`lng` (`REGION` 은 `null`) | `REGION` 도 숫자(CGNDB 대표점) | CGNDB가 좌표를 준다 [R1]. 대표점은 출처마다 다르므로(Calgary: CGNDB와 GeoNames 차이) 한 출처로 고정한다 |
| `name`: "시·도 짧은 이름 + 시·군·구" | `name`: 공식 철자(악센트 유지), `full_name`: "Victoria, British Columbia" 형식 | 동명 후보(Victoria 5곳)를 구별해야 한다 [R10] |
| `category` (`SPOT` 만) | `SPOT` 은 Nominatim `type`(예 `hospital`) | [R9] |
| `sources[].name/url` | CGNDB·SGC·OpenStreetMap 표시 문구와 라이선스 링크 | 출처 표시 의무 [6][13][31] |
| `PlaceQuery` 정규화 (NFC, 제어 문자, 공백, 1~50자) | 유지. 비교용 접기 규칙 추가: 악센트 제거, 대소문자, "St."/"Saint", 하이픈·엔대시, 쉼표로 도시 좁히기 | 프랑스어 이름 [R4] |
| `RegionMatcher` M1~M6 (낱말 1~3개, "시·군" 꼬리 떼기, 조상 일치) | "시·군" 꼬리 규칙 삭제. 쉼표 뒤 낱말은 주(약어 포함) 또는 도시로 해석 | 캐나다 입력 형식 "City, Province" |
| 후보 순서 = 법정동코드 오름차순 | SGC 코드 오름차순(동명 후보가 지역 순서로 나옴) | 추측·인기 순 없음 유지 |
| `more` | 사전 후보는 `false`. Nominatim `limit` 에 도달하면 `true` | 같은 의미 유지 |
| 응답 로그 규칙, `Cache-Control: no-store`, 400·503 코드, `UPSTREAM_UNAVAILABLE` | 그대로 | 나라와 무관 |

## 8. 위험

1. **"동네" 정의가 도시마다 다르다.** 데모 입력 "Hochelaga-Maisonneuve" 는 몽레알 공식 참조 동네에서 셋으로 나뉘고 [R15], OSM과 CGNDB에서는 하나의 이름이다 [R9][R5]. 어느 기준을 쓰느냐에 따라 같은 입력의 좌표와 경계가 달라진다(10절 D3).
2. **병원 최신 목록이 없다.** CGNDB에 없고 [R5], GeoNames는 12곳뿐이며 [R12], ODHF는 2020년 고정판이다 [17]. 출생 당시(예: 1990년대)에 있던 병원이 이미 폐업했거나 이름이 바뀌었을 가능성이 높다. Nominatim의 "Calgary General Hospital" 질의가 현재 시설 1건을 돌려준 것은 [R9] 같은 곳이라는 보장이 아니다.
3. **CGNDB API의 약관이 문서에 없다.** 내려받기 파일은 OGL-Canada이지만 API 쪽은 일반 약관만 링크돼 있다 [3][5]. 사전 만들기에 내려받기 파일만 쓰면 피한다(10절 D1).
4. **Nominatim 공개 서버는 보장이 없다.** 용량이 작고 접근이 철회될 수 있으며 [28], ODbL 파생 데이터베이스 해석이 남아 있다(10절 D2). 공개 배포 전에 재검토해야 한다.
5. **SGC와 CGNDB를 잇는 열이 없다.** (주, 이름)으로 맞추면 CSD 5,161개 중 3,329개가 유일하게 맞았지만(리서처 간이 계산, 공식 문서 아님), 170개는 여러 건이 걸렸고 1,662개는 안 맞았다(번호형 이름 등). 대표 도시만 먼저 연결하고 나머지는 큐레이션이나 점-다각형(경계 파일)으로 해결해야 한다.
6. **`UNP` 안에 동네와 시골 마을이 섞여 있다.** 동네 사전을 만들 때 큐레이션이 필요하다. 전부 `NEIGHBORHOOD` 로 취급하면 "Victoria Road" 같은 도로 이름이 동네로 나온다 [R1].
7. **인코딩이 서로 다르다.** SGC CSV는 ISO-8859-1 [R10], CGNDB CSV는 UTF-8(BOM 포함) [R11]. 읽을 때 인코딩을 명시하지 않으면 "Montréal" 이 깨진다.
8. **대표 좌표가 출처마다 다르다.** St. John's: CGNDB 47.4822, -52.7971 / Nominatim 47.5647, -52.7067 / GeoNames 47.5649, -52.7093 [R3][R9][R12]. 지도를 쓰지 않는 장소 해석에는 문제가 없으나 T2의 항공사진이나 날씨 관측소 선택의 입력이 되면 거리가 중요해진다.
9. **밴쿠버·캘거리 약관을 읽지 못했다.** 두 도시의 경계 자료는 이용 조건이 `미확인` 이라 구현에 쓰지 않는다. 필요하면 B2, B3를 PM이 본다.
10. **Google 서비스 약관 본문을 읽지 못했다.** 위도·경도 캐시 기간과 상업 이용 조항이 미확인이다(B1).
11. **옛 Geolocation API는 2027-03-31에 끝난다.** 새 Geolocator도 OSM 자료를 섞어 준다 [4]. 둘 다 사전 구성의 필수 부품으로 쓰지 않는다.
12. **SGC 2026 판 가능성.** 2026 인구조사가 있었으나 SGC 2026 발표는 찾지 못했다. 공식 쪽은 2021을 현행으로 적는다 [8]. 새 판이 나오면 사전을 다시 만들어야 한다.

## 9. 제품 정의서 가정과 다른 점

| 절 | 기존 가정 | 리서치 결과 |
|---|---|---|
| 4절 (장소 해석) | "동" 단위가 한국 행정동처럼 전국 공통 코드와 계층을 가진다 | 캐나다에는 전국 공통 동네 목록이 없다. CGNDB에서는 비법인 지명 안에 섞여 있고 [2], 도시마다 공식 구획의 개수와 이름이 다르다(토론토 158, 밴쿠버 22, 몽레알 91, 캘거리 313) [R14][R16][R15][R17]. 같은 입력("Hochelaga-Maisonneuve")이 출처에 따라 하나 또는 셋이다 [R15][R9] |
| 8절 (외부 응답 캐시) | 약관이 허용하는 범위에서 외부 응답을 캐시해 쿼터를 아낀다 | 캐나다 사전 출처는 개방 라이선스라 저장이 허용된다 [6][13]. Nominatim은 오히려 캐시를 요구한다 [28]. 한국판에서 막혔던 "장소 해석 결과 캐시"가 풀린다. Google만 캐시 금지가 남는다 [19][20] |
| 7절 / 로드맵 M2a | 지역 사전 원천은 법정동코드, `REGION` 의 `lat`/`lng` 는 `null` | 원천은 SGC 2021 + CGNDB이고, `REGION` 좌표를 CGNDB에서 받을 수 있다. 계약 변경 목록은 7절 |
| 13절 (지도·로드뷰 제공자) | 장소 검색 제공자와 지도 제공자가 묶일 수 있다 | 사전 + Nominatim 구성은 지도 제공자와 묶이지 않는다. Google을 쓰면 지도에 올릴 때 Google 지도가 필요하다 [19][20] |
| 12절 (데모 입력) | 데모 장소 3개 | 결정 1 (b)의 4개와 동명 확인용 3개 이름(London, Victoria, Saint John)을 5절에서 모두 판단했다 |

## 10. PM 결정 필요

아래 결정은 다른 리서치 작업(T2~T9)을 막지 않는다. T9 요약과 후속 계획(`feat/ca-place-resolve`, `feat/ca-place-search`)에서 한 번에 정한다. 이 판단은 법률 자문이 아니다.

**D1. CGNDB Geoname API·Geolocator 응답의 이용 조건** (관련: 계획서 C2 `feat/ca-place-search`)
- 근거: 내려받기 데이터셋은 OGL-Canada [1]. API 문서에는 라이선스 문구가 없고 [3][4], 링크된 NRCan 일반 약관은 정부 저작물의 상업적 재배포에 사전 서면 허가를 요구하면서 "달리 명시된 경우 예외"라고 적는다 [5]. 조항 위치: NRCan Terms and conditions의 "Reproduction", "Commercial reproduction" 단락.
- 선택지:
  - (a) 실행 중 API를 부르지 않고 내려받기 파일로만 사전을 만든다.
  - (b) API를 실행 중에 쓴다(한도 미기재).
  - (c) NRCan에 API 응답 라이선스를 서면으로 문의한 뒤 정한다(PM 몫).
- 추천안: (a). 이것만으로 `REGION`·`NEIGHBORHOOD` 사전이 완성된다. (c)는 API가 꼭 필요해질 때만.

**D2. Nominatim 캐시와 ODbL 파생 데이터베이스** (관련: `feat/ca-place-search`, 공개 배포 시기)
- 근거: 이용 정책은 "결과를 캐시하라"고 하고 [28], ODbL은 변경하거나 이를 토대로 만든 결과를 같은 라이선스로만 배포하게 하며 [30], 표시 지침은 "파생 데이터베이스를 이루지 않는 한" 결과마다 표시를 붙일 필요가 없다고 한다 [31]. 캐시나 `data/` 에 모은 검색 결과가 파생 데이터베이스인지는 문서로 판단할 수 없다.
- 선택지:
  - (a) 로컬 데모 동안만 서버 안 임시 캐시(저장소 밖, 커밋 안 함)를 쓰고 "OpenStreetMap" 표시를 붙인다. 검색 결과를 `data/` 에 모으지 않는다.
  - (b) 검색 결과를 큐레이션해 `data/` 에 쌓고 ODbL로 공개한다.
  - (c) OSM을 쓰지 않고 Google을 쓴다(캐시 금지).
- 추천안: (a). 공개 배포를 결정할 때 다시 본다.

**D3. 동네의 기준 (데모 입력 "Hochelaga-Maisonneuve, Montréal")**
- 근거: SGC 요소 CSV [R10]와 CGNDB [R5]와 OSM [R9]은 하나의 이름으로 두고, 몽레알 공식 참조 동네는 Hochelaga·Maisonneuve·Préfontaine 으로 나눈다 [R15].
- 선택지:
  - (a) 이름 해석(사전)은 SGC·CGNDB·OSM의 하나의 이름을 쓰고, 경계가 필요한 작업(T2, T6)은 그때 정한다.
  - (b) 몽레알 공식 참조 동네를 기준으로 하고 입력 "Hochelaga-Maisonneuve" 를 두세 동네의 묶음으로 처리한다.
  - (c) 데모 도시 4곳만 큐레이션한 동네 목록(이름, 대표점)을 직접 만든다.
- 추천안: (a)로 시작하고, T2 결과를 본 뒤 (c)를 검토한다.

**D4. 캐나다 밖과 같은 이름의 외국 도시**
- 근거: "London" 을 입력하면 영국 런던이 가장 유명하지만 캐나다 사전은 온타리오만 맞춘다. 제품 정의서 4절은 추측해 하나를 고르지 말라고 하고, 9절은 캐나다 밖을 범위 밖으로 둔다.
- 선택지:
  - (a) 사전에 일치가 있으면 캐나다 후보만 보여 준다(외국 런던은 다루지 않음). 후보 문구에 주 이름을 붙여 "London, Ontario" 로 보여 준다.
  - (b) 사전에 일치가 있어도 필터 없는 검색을 한 번 더 해서 외국 후보가 있으면 "캐나다 밖은 지원하지 않습니다" 안내를 함께 보인다.
- 추천안: (a). 가장 단순하고 사전 중심 구성과 맞다.

**D5. 병원 입력의 기대치** (관련: 제품 정의서 4절 "찾지 못하는 장소" 안내)
- 근거: 최신·전수 병원 목록이 없고 [R5][R12][17], 출생 당시 병원은 현재 지도에 없을 수 있다.
- 선택지:
  - (a) `SPOT` 은 "최선을 다한 검색"으로 두고 못 찾으면 동네나 도시로 다시 입력하게 안내한다.
  - (b) ODHF를 병원 사전으로 얹어 일치하면 외부 호출을 줄인다.
  - (c) `SPOT` 을 MVP에서 뺀다.
- 추천안: (a). (b)는 T9 이후 필요하면 추가.

## 11. 근거 링크

공식 문서·정책·데이터셋 기록은 아래 표다. 공개 엔드포인트를 직접 호출한 기록은 그 아래 표(R)다. 모든 확인 일자는 2026-10-29다.

| # | URL | 문서 제목 | 확인 일자 |
|---|---|---|---|
| 1 | https://open.canada.ca/data/en/dataset/e27c6eba-3c5d-4051-9db2-082dc6411c2c | Open Government Portal — Canadian Geographical Names (CGN) 데이터셋: OGL-Canada, 매주 갱신, 기록 수정 2025-12-22, 시간 범위 1897~현재, CSV·KML·SHP | 2026-10-29 |
| 2 | https://natural-resources.canada.ca/maps-tools-publications/maps/geographical-names-board-canada/about-canadian-geographical-names-database | NRCan — About the CGNDB: 현재 약 350,000, 예전 약 37,000, 해저 약 3,500, 동네는 비법인 지명 범주, 쪽 수정 2025-02-26 | 2026-10-29 |
| 3 | https://natural-resources.canada.ca/maps-tools-publications/maps/geographical-names-canada/geoname-service-api | NRCan — Geoname Service (API): 엔드포인트, 매개변수, `num` 1~1,000, 키 언급 없음, 쪽 수정 2025-06-12 | 2026-10-29 |
| 4 | https://natural-resources.canada.ca/maps-tools-publications/satellite-elevation-air-photos/geolocation-service | NRCan — Geolocator API(새)와 옛 Geolocation API(2027-03-31 종료), 쪽 수정 2026-03-19 | 2026-10-29 |
| 5 | https://natural-resources.canada.ca/terms-conditions | NRCan — Terms and conditions: 재복제·상업 재배포, 쪽 수정 2025-10-03 | 2026-10-29 |
| 6 | https://open.canada.ca/en/open-government-licence-canada | Open Government Licence – Canada v2.0 (2022-12-02 수정) | 2026-10-29 |
| 7 | https://ftp.maps.canada.ca/pub/nrcan_rncan/vector/geobase_cgn_toponyme/prov_csv_eng/ | CGNDB 영어 CSV 파일 목록(주·준주별 zip, 전국 합본) | 2026-10-29 |
| 8 | https://www.statcan.gc.ca/en/subjects/standard/sgc/2021/introduction | SGC 2021 소개: 4단계, 7자리 코드, 주 코드, 수치, 쪽 수정 2024-03-12 | 2026-10-29 |
| 9 | https://www.statcan.gc.ca/en/subjects/standard/sgc/2021/index | SGC 2021 — CSV 내려받기 링크(구조·요소), 쪽 수정 2023-04-17 | 2026-10-29 |
| 10 | https://open.canada.ca/data/en/dataset/1d815243-28ce-45e6-ad6b-58643327a253 | Open Government Portal — SGC 2021 Volume I 데이터셋: OGL-Canada·Statistics Canada Open Licence, 영어·프랑스어 CSV, 기록 수정 2023-04-21 | 2026-10-29 |
| 11 | https://www.statcan.gc.ca/en/statistical-programs/document/sgc-cgt-2021-structure-eng.csv | SGC 2021 분류 구조 CSV(영어, 직접 내려받아 확인, [R10]) | 2026-10-29 |
| 12 | https://www.statcan.gc.ca/en/statistical-programs/document/sgc-cgt-2021-element-eng.csv | SGC 2021 요소 CSV(영어, 직접 내려받아 확인, [R10]) | 2026-10-29 |
| 13 | https://www.statcan.gc.ca/en/terms-conditions/open-licence | Statistics Canada Open Licence: 이용·복제·게시·배포·판매·가공 허용, 표시 문구, 제외 항목 (쪽 수정 2025-07-10) | 2026-10-29 |
| 14 | https://www.statcan.gc.ca/en/terms-conditions | Statistics Canada 약관: 라이선스가 "대부분의 데이터 제품"에 적용, 상업 목적 포함 (쪽 수정 2025-07-10) | 2026-10-29 |
| 15 | https://open.canada.ca/data/en/dataset/ef70dc3b-1069-4037-9bce-61f47e628a1d | Open Government Portal — 2021 Census Boundary Files: OGL-Canada, GML·SHP·GDB, REST·WMS, 기록 수정 2024-02-15 | 2026-10-29 |
| 16 | https://www150.statcan.gc.ca/n1/pub/92-160-g/92-160-g2021001-eng.htm | Boundary Files, Reference Guide, Census year 2021: 단계(CSD·CMA/CA·CT·DA), 람베르트 NAD83, Statistics Canada Open Licence, 수정 2021-11-17 | 2026-10-29 |
| 17 | https://www.statcan.gc.ca/en/lode/databases/odhf | Open Database of Healthcare Facilities v1.1: 약 7,000곳, 2019~2020 수집, OGL-Canada, 쪽 수정 2025-12-22 | 2026-10-29 |
| 18 | https://www150.statcan.gc.ca/n1/pub/13-26-0001/2020001/ODHF_v1.1.zip | ODHF_v1.1.zip(내려받아 열 이름·행 확인) | 2026-10-29 |
| 19 | https://developers.google.com/maps/documentation/places/web-service/policies | Google — Places API 정책(Google 지도 필수, 캐시 금지, `place_id` 예외, 로고, 2026-10-28 갱신) | 2026-10-29 |
| 20 | https://developers.google.com/maps/documentation/geocoding/policies | Google — Geocoding API 정책(2026-10-28 갱신) | 2026-10-29 |
| 21 | https://developers.google.com/maps/billing-and-pricing/pricing | Google Maps Platform — 가격(SKU별 무료 한도, 2026-10-28 갱신) | 2026-10-29 |
| 22 | https://developers.google.com/maps/coverage | Google — 커버리지 상세(캐나다 행, Geocoding 품질 표시, 2026-10-28 갱신) | 2026-10-29 |
| 23 | https://developers.google.com/maps/documentation/places/web-service/text-search | Google — Text Search (New): `pageSize` 20, 총 60건, `regionCode`, `includedType` | 2026-10-29 |
| 24 | https://developers.google.com/maps/documentation/geocoding/requests-geocoding | Google — Geocoding 요청·응답: `components`, 주소 구성요소 타입, `partial_match`, `OVER_QUERY_LIMIT` | 2026-10-29 |
| 25 | https://developers.google.com/maps/documentation/places/web-service/place-types | Google — Place types(Table A: `hospital`·`locality`·`administrative_area_level_1`, Table B: `neighborhood`·`sublocality`, 2026-10-28 갱신) | 2026-10-29 |
| 26 | https://developers.google.com/maps/documentation/places/web-service/reference/rest/v1/places | Google — Places REST: `Place`, `businessStatus` 값, `movedPlaceId` | 2026-10-29 |
| 27 | https://cloud.google.com/maps-platform/terms/maps-service-terms | Google Maps Platform 서비스별 약관 (제목만 읽혀 본문은 못 읽음) | 2026-10-29 |
| 28 | https://operations.osmfoundation.org/policies/nominatim/ | OSMF — Nominatim 이용 정책(초당 1회, User-Agent, 캐시 의무, 자동완성 금지) | 2026-10-29 |
| 29 | https://nominatim.org/release-docs/latest/api/Search/ | Nominatim — Search API(`countrycodes`, `limit` 40, 응답 필드, `addresstype`) | 2026-10-29 |
| 30 | https://www.openstreetmap.org/copyright | OpenStreetMap — 저작권과 ODbL 요약(표시, 같은 조건 공유) | 2026-10-29 |
| 31 | https://osmfoundation.org/wiki/Licence/Attribution_Guidelines | OSMF — 표시 지침(표시 문구, 지오코딩 결과, 파생 데이터베이스 예외) | 2026-10-29 |
| 32 | https://ckan0.cf.opendata.inter.prod-toronto.ca/api/3/action/package_show?id=neighbourhoods | 토론토 열린 데이터 API(CKAN) — "Neighbourhoods" 레코드: 라이선스 `open-government-licence-toronto`, 자원 목록, 설명 | 2026-10-29 |
| 33 | https://open.toronto.ca/dataset/neighbourhoods/ | 토론토 열린 데이터 — "Neighbourhoods" 쪽(내용이 비어 보이고 "Retired" 표시가 나옴. 위 32번 API 레코드를 근거로 쓴다) | 2026-10-29 |
| 34 | https://open.toronto.ca/open-data-licence/ | Open Government Licence – Toronto v1.0 | 2026-10-29 |
| 35 | https://www.donneesquebec.ca/recherche/api/3/action/package_show?id=vmtl-quartiers | Données Québec — 몽레알 "Quartiers de référence en habitation" 레코드: CC-BY 4.0, 수정 2026-10-28 | 2026-10-29 |
| 36 | https://donnees.montreal.ca/api/3/action/datastore_search?resource_id=1c142277-53f7-4801-9d19-c54acce747d8&limit=100 | 몽레알 열린 데이터 — 참조 동네 91행(`no_qr`, `nom_qr`, `no_arr`, `nom_arr`) | 2026-10-29 |
| 37 | https://www.donneesquebec.ca/licence/ | Données Québec — 라이선스 쪽(본문이 비어 읽히지 않음) | 2026-10-29 |
| 38 | https://creativecommons.org/licenses/by/4.0/ | CC BY 4.0 deed | 2026-10-29 |
| 39 | https://opendata.vancouver.ca/api/explore/v2.1/catalog/datasets/local-area-boundary | 밴쿠버 열린 데이터 — "Local area boundary" 메타데이터(22행, 라이선스 이름·링크, 수정 2023-06-24) | 2026-10-29 |
| 40 | https://opendata.vancouver.ca/api/explore/v2.1/catalog/datasets/local-area-boundary/records | 밴쿠버 열린 데이터 — 점-다각형 질의 | 2026-10-29 |
| 41 | https://opendata.vancouver.ca/pages/licence/ | 밴쿠버 열린 데이터 라이선스 쪽(본문이 자바스크립트로만 그려져 읽지 못함) | 2026-10-29 |
| 42 | https://data.calgary.ca/api/catalog/v1 | 캘거리 열린 데이터 카탈로그 API(Community Boundaries, 좌표계 메타데이터) | 2026-10-29 |
| 43 | https://data.calgary.ca/resource/surr-xmvs.json | 캘거리 열린 데이터 — Community District Boundaries(313행) | 2026-10-29 |
| 44 | https://data.calgary.ca/d/Open-Data-Terms/u45n-7awa | 캘거리 열린 데이터 약관 쪽(본문을 읽지 못함) | 2026-10-29 |
| 45 | https://www.geonames.org/export/ | GeoNames — 내려받기와 웹 서비스: CC BY, 출처 표시, 웹 서비스 크레딧 | 2026-10-29 |
| 46 | https://www.geonames.org/export/web-services.html | GeoNames — 웹 서비스: 무료 계정 `username` 필요, `timezoneJSON` | 2026-10-29 |
| 47 | https://download.geonames.org/export/dump/readme.txt | GeoNames — 내려받기 설명: CC BY 4.0, 열(시간대 IANA id) | 2026-10-29 |
| 48 | https://download.geonames.org/export/dump/CA.zip | GeoNames — 캐나다 파일(직접 내려받아 확인, [R12]) | 2026-10-29 |
| 49 | https://www.wikidata.org/wiki/Wikidata:Licensing | Wikidata — 라이선스(구조화 데이터 CC0) | 2026-10-29 |
| 50 | https://www.wikidata.org/w/api.php | Wikidata — Action API(직접 호출, [R18]) | 2026-10-29 |
| 51 | https://www.iana.org/time-zones | IANA — Time Zone Database(최신 2026f, 2026-10-27) | 2026-10-29 |
| 52 | https://developers.google.com/maps/documentation/timezone/overview | Google — Time Zone API 개요 | 2026-10-29 |
| 53 | https://www.mediawiki.org/wiki/Wikimedia_APIs/Rate_limits | Wikimedia APIs — 속도 제한(식별 안 됨 분당 10, 규칙에 맞는 User-Agent 분당 200, 429·Retry-After, 수정 2026-10-15) | 2026-10-29 |

### 직접 호출 기록 (공개 엔드포인트, 2026-10-29)

모든 요청의 User-Agent는 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)` 이고, 요청 간격은 1.2초 이상이다. 응답 원문은 저장소에 넣지 않았다.

| # | 요청 | 확인한 것 |
|---|---|---|
| R1 | `GET https://geogratis.gc.ca/services/geoname/en/geonames.json?q=Victoria` | HTTP 200, 100건. 항목 필드 `id`, `name`, `language`, `category`, `status`, `concise`, `generic`, `location`, `province`, `latitude`, `longitude`, `map`, `relevance`, `bbox`, `accuracy`, `position`, `decision`. `CITY`(BC) 1, `TOWN`(NL) 1, `UNP` 30 외 지형. 예전 이름 항목(`category=H`) 포함 |
| R2 | `…/geonames.json?q=London&concise=CITY,TOWN,VILG,HAM,MUN1,MUN2,UTM,LTM,STM`, `q=Calgary&concise=CITY,UNP,MUN1,MUN2,STM`, `/fr/geonames.json?q=Montréal&concise=CITY` | London 2건: 현재 공식 `CITY`(온타리오, 42.9675, -81.2325) 1건 + 예전 공식명(`category=H`) `MUN2` 1건. 기본 응답에는 `category=H` 항목이 섞이므로 `category=O` 로 걸러야 한다. Calgary `CITY` 1건(51.0458, -114.0575). 프랑스어 `/fr/` 경로도 HTTP 200으로 응답(Montréal 질의 `CITY` 4건, 그중 예전 공식명 1건) |
| R3 | `…/geonames.json?q=Saint John`, `q=St. John's` | Saint John: 51건 중 `CITY` 2(Saint John NB, Fort St. John BC). St. John's: 18건 중 `CITY` 1(47.4822, -52.7971) |
| R4 | `…/geonames.json?q=Montreal` 과 `q=Montréal`(+ `concise` 필터), `q=Quebec` 과 `q=Québec` | 각 쌍의 결과가 같다(23건, 5건). `PROV` "Quebec" 과 `CITY` "Québec" 이 함께 온다 |
| R5 | `…/geonames.json?q=Hochelaga`, `q=Vancouver General Hospital`, `q=hospital` | Hochelaga 6건(`UNP` "Hochelaga-Maisonneuve" 45.5647, -73.5389 포함). VGH 0건. hospital 35건은 모두 지형 |
| R6 | `…/geonames.json?lat=49.2618&lon=-123.1227&radius=2&concise=UNP,CITY,TOWN,HAM&sort-field=distance` | 4건: Vancouver(`CITY`), Fairview, South Cambie, Shaughnessy(`UNP`) |
| R7 | `…/geonames/en/codes/concise.json`, `…/codes/province.json` | concise 50개 코드(병원 코드 없음). province 15개: 10 NL, 11 PE, 12 NS, 13 NB, 24 QC, 35 ON, 46 MB, 47 SK, 48 AB, 59 BC, 60 YT, 61 NT, 62 NU, 72 UF, 73 IW |
| R8 | `GET https://geogratis.gc.ca/services/geolocation/en/locate?q=Calgary`(옛), `GET https://geolocator.api.geo.ca/?q=London&lang=en`, `…?q=Vancouver General Hospital`, `…?q=Paris France` | 옛: 응답 헤더 `Cache-Control: no-store`, 항목 `title`, `qualifier`, `type`, `bbox`, `geometry`. 새: 항목 `key`, `name`, `province`, `category`, `lat`, `lng`, `bbox`, `tag`. VGH 질의에서 `nominatim` 항목이 맨 앞. Paris France 질의는 캐나다 지명만 반환 |
| R9 | `GET https://nominatim.openstreetmap.org/search?q=<질의>&format=jsonv2&addressdetails=1&limit=10&accept-language=en&countrycodes=ca` (London은 `countrycodes` 있는 판과 없는 판) | London ca 2건 / 필터 없음 3건. Victoria 4건. Saint John 2건. "Vancouver General Hospital" 1건 `amenity/hospital` (49.2618, -123.1227). "Hochelaga-Maisonneuve, Montréal" 1건 `boundary/neighbourhood` (45.5515, -73.5400). "St. John's" 1건 city (47.5647, -52.7067). "Calgary" 1건 city (51.0456, -114.0575). "Calgary General Hospital" 1건 "Peter Lougheed Centre" `amenity/hospital`. 응답 필드: `place_id`, `osm_type`, `osm_id`, `lat`, `lon`, `category`, `type`, `place_rank`, `importance`, `addresstype`, `name`, `display_name`, `boundingbox`, `address`, `licence` |
| R10 | `GET https://www.statcan.gc.ca/en/statistical-programs/document/sgc-cgt-2021-structure-eng.csv`, `…/sgc-cgt-2021-element-eng.csv` | 구조 CSV: 레벨 1 6행, 레벨 2 13행, 레벨 3 293행, 레벨 4 5,161행. 요소 CSV: "Place name(s)" 34,285행(CSD 코드별: Montréal 88, Calgary 180, Vancouver 27, St. John's 35). 이름이 같은 CSD: Victoria 5, London 1, Saint John 1, St. John's 1. 인코딩 ISO-8859-1 |
| R11 | `GET https://ftp.maps.canada.ca/pub/nrcan_rncan/vector/geobase_cgn_toponyme/prov_csv_eng/cgn_nl_csv_eng.zip` | 33,150행, 열 16개(UTF-8 BOM). `TOWN` 270, `CITY` 3, `UNP` 1,358. 같은 폴더의 전국 합본 `cgn_canada_csv_eng.zip`(내려받기 약 16 MB, 압축 해제 약 77 MB)도 받아 366,235행을 확인: `CITY` 396, `TOWN` 859, `VILG` 1,502, `HAM` 261, `MUN1` 355, `MUN2` 1,643, `UNP` 26,869. SGC CSD 5,161개와 (주, 이름)으로 맞춘 결과 3,329 유일 / 170 복수 / 1,662 없음(리서처 간이 계산) |
| R12 | `GET https://download.geonames.org/export/dump/CA.zip` | 317,133행. 시간대 id 40종. `HSP`(병원) 12행. Calgary `America/Edmonton`, St. John's `America/St_Johns`, Montréal·London `America/Toronto`, Victoria BC `America/Vancouver`, Saint John `America/Moncton` |
| R13 | `GET https://www150.statcan.gc.ca/n1/pub/13-26-0001/2020001/ODHF_v1.1.zip` | 7,033행: `Hospitals` 1,140(좌표 있음 1,047), `Ambulatory health care services` 2,227, `Nursing and residential care facilities` 3,666. 이름에 "general hospital" 포함 90행. "Vancouver General Hospital" 의 `CSDuid` 5915022 |
| R14 | `GET https://ckan0.cf.opendata.inter.prod-toronto.ca/api/3/action/package_search?q=neighbourhoods`, `…/package_show?id=neighbourhoods` | "Neighbourhoods" 패키지: 라이선스 `open-government-licence-toronto`, GeoJSON·CSV·SHP·GPKG(4326·2952), 설명 "158 neighbourhoods", 마지막 갱신 2026-02-20 |
| R15 | `GET https://www.donneesquebec.ca/recherche/api/3/action/package_show?id=vmtl-quartiers`, `GET https://donnees.montreal.ca/api/3/action/datastore_search?resource_id=1c142277-53f7-4801-9d19-c54acce747d8&limit=100` | 라이선스 "Attribution (CC-BY 4.0)", 레코드 수정 2026-10-28. 91행. 구 22번 아래 9개 동네. 행정 경계 자료 `vmtl-limites-administratives-agglomeration` 존재 |
| R16 | `GET https://opendata.vancouver.ca/api/explore/v2.1/catalog/datasets/local-area-boundary`, `…/records?where=intersects(geom, geom'POINT(-123.1226691 49.2618427)')&select=name` | 22행, 라이선스 "Open Government Licence - Vancouver", 점 질의 결과 `Fairview`, 응답 헤더 `X-RateLimit-Limit: 15000` |
| R17 | `GET https://data.calgary.ca/api/catalog/v1?q=community boundaries`, `GET https://data.calgary.ca/resource/surr-xmvs.json?$select=count(*)` | 313행, 열 `class`, `class_code`, `comm_code`, `name`, `sector`, `srg`, `comm_structure`, `multipolygon`. 카탈로그 좌표계 `EPSG:4326 WGS84`. 라이선스 "See Terms of Use" |
| R18 | `GET https://www.wikidata.org/w/api.php?action=wbsearchentities&search=Victoria&language=en&limit=12&format=json`, `…wbgetentities&props=claims` 에 `ids` 로 Q2132 와 Q7914187 을 묶어 한 번, Q5019676 을 한 번 | Victoria 검색 12건(캐나다 도시 Q2132 1건). Q2132, Q7914187 에 P625·P17·P131·P31 있음, P421 없음. Q5019676 "former hospital in Canada" 에 P625·P17·P31(Q16917)·P131, 폐업 연도 속성 없음 |
