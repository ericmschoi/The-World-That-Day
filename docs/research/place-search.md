# 장소 검색·좌표 — 출처 검증

> 작업: T1 · 계획서: docs/plans/m0-source-research.md · 조사 일자: 2026-10-08 ~ 2026-10-09 · 작성: data-researcher
> 이 문서의 약관 해석은 법률 자문이 아니다. 확신도 기준은 계획서의 "확신도 기준"을 따른다.
> 2026-10-27 갱신(M2a T0a): 법정동코드 카드를 결정 #10 (a)에 따라 `확인됨` 으로 올리고 데이터셋 페이지를 다시 확인했다. 다른 카드는 바뀌지 않았다.
> 이 영역의 후보는 모두 키가 필요하다. 그래서 실제 응답은 하나도 보지 못했고, 응답 형태는 공식 문서로만 판단했다. 실제 정확도는 6절 명령을 PM이 실행해야 확인된다.

## 1. 요약

- 1순위는 **카카오 로컬 API**(주소 검색 + 키워드 검색 + 좌표→행정구역)다. 종합 확신도는 `부분 확인 (PM 호출 대기)`. 장소(POI) 검색, 주소 계층, 행정동·법정동 코드, 병원 카테고리 코드(`HP8`)를 한 제공자에서 받고, 무료 한도가 API마다 하루 100,000건이다 [1][3].
- 가장 중요한 제약은 **결과를 저장하거나 캐시하기 어렵다**는 점이다. 네이버 Maps 약관은 결과 데이터를 "받은 즉시 1회" 쓰는 것만 허용한다 [10]. 카카오 운영정책은 사용자 경험을 높이는 목적의 캐시만 허용하고 최신 상태를 유지하라고 한다 [5]. 다만 카카오가 저장을 어디까지 허용하는지는 공식 문서마다 달라 PM 결정으로 올렸다(10절 D1).
- 네이버 NCP Geocoding은 **주소 검색 전용**이다. "서울대학교병원" 같은 장소 이름은 문서상 지원 근거가 없다 [6]. 네이버로 장소를 검색하려면 별도의 네이버 검색 API(지역)가 필요한데, 그 문서는 이 도구로 열 수 없어 `미확인`이다.
- Google Places(New) Text Search는 폐업 상태(`CLOSED_PERMANENTLY`)와 이전한 장소 ID를 준다 [16]. 하지만 Places 결과를 지도에 표시할 때는 Google 지도를 써야 하고 [19], 한국 행정동·법정동 코드는 주지 않는다 [14].
- 제품 정의서와 다른 점: 8절의 "외부 응답 캐시"와 로드맵 M2a의 "장소 해석 결과 캐시"는 카카오·네이버 약관에서는 사실상 하기 어렵다. 또 7절은 네이버 Geocoding을 장소 검색 후보로 올렸지만, 이 API는 장소 이름 검색을 지원하지 않는다(9절).

## 2. 확인할 질문과 답

질문 번호는 계획서 "T1 작업별 기준"의 번호다. 제품 정의서 7절의 "M0에서 확인할 것" 4개 항목(병원·동 이름 검색 정확도, 장소 카테고리, 쿼터, 약관)은 질문 열에 `(7절)` 로 표시했다.

| # | 질문 (제품 정의서 7절 + 계획서 작업별 질문) | 출처 | 답 | 확신도 | 근거 |
|---|---|---|---|---|---|
| 1 | 병원 이름 검색 정확도 (7절: 병원 이름 검색 정확도): "서울대학교병원"의 첫 후보 이름·주소·카테고리를 얻는가 | 카카오 로컬 | 키워드 검색(`/v2/local/search/keyword`)이 후보마다 `place_name`, `address_name`, `road_address_name`, `category_name`, `category_group_code` 를 준다. 실제 첫 후보는 명령 K3으로 확인해야 한다 | 필드: 확인됨 / 실제 결과: 부분 확인 (PM 호출 대기) | [1] |
| 1 | 같은 질문 | 네이버 NCP Geocoding | 문서는 이 API를 "입력한 주소와 연관된 주소 정보를 검색"하는 것으로만 설명하고, 장소 이름 검색은 언급하지 않는다. 응답 주소 요소에 `BUILDING_NAME` 이 있어 건물명으로 걸릴 수는 있지만, 카테고리 필드는 없다 | 부분 확인 (PM 호출 대기) | [6] |
| 1 | 같은 질문 | Google Places(New) Text Search | `places[].displayName`, `formattedAddress`, `primaryType`, `types` 를 준다. `hospital` 은 Table A 타입이라 `includedType` 필터로 쓸 수 있다. 실제 첫 후보는 명령 G3으로 확인해야 한다 | 필드: 확인됨 / 실제 결과: 부분 확인 (PM 호출 대기) | [15][17] |
| 1 | 같은 질문 | 네이버 검색 API(지역) | 문서를 열지 못했다 | 미확인 | [28] |
| 2 | 동 이름 검색: "광주 서구 풍암동"이 동 단위로 해석되고, 시·도 / 시·군·구 / 읍·면·동 계층과 코드(법정동·행정동 구분 포함)를 받는가 | 카카오 로컬 | 주소 검색의 `address` 객체가 `region_1depth_name`(시도), `region_2depth_name`(구), `region_3depth_name`(동), `region_3depth_h_name`(행정동 명칭), `h_code`(행정 코드), `b_code`(법정 코드)를 준다. 좌표→행정구역 변환은 `region_type` `H`(행정동)와 `B`(법정동)를 `code` 와 함께 준다. 실제 결과는 명령 K2, K6으로 확인해야 한다 | 필드: 확인됨 / 실제 결과: 부분 확인 (PM 호출 대기) | [1] |
| 2 | 같은 질문 | 네이버 NCP Geocoding | `addressElements[]` 가 `SIDO`, `SIGUGUN`, `DONGMYUN`, `RI` 계층을 `longName`, `shortName`, `code` 와 함께 준다. 이 `code` 가 법정동 코드인지 행정동 코드인지는 문서에 없다. 요청의 `filter` 는 `HCODE`(행정동)와 `BCODE`(법정동)를 받는다. Reverse Geocoding은 `legalcode`(법정동)와 `admcode`(행정동)를 따로 준다 | Geocoding 코드 구분: 부분 확인 / Reverse Geocoding: 확인됨 | [6][13] |
| 2 | 같은 질문 | Google Geocoding / Places | `address_components[].types` 에 `administrative_area_level_1~7`, `locality`, `sublocality_level_1~5` 가 있다. 문서의 필드 목록에는 한국 행정동·법정동 코드가 없다. 한국 주소가 어떤 타입으로 매핑되는지는 문서에 없다 | 계층 필드: 확인됨 / 코드 없음: 확인됨 / 한국 매핑: 부분 확인 (PM 호출 대기) | [14][16] |
| 2 | 같은 질문 | 법정동코드 API (추가 후보) | `region_cd`, `sido_cd`, `sgg_cd`, `umd_cd`, `ri_cd`, `locatadd_nm`, `locallow_nm` 를 준다. 법정동만 다루고 행정동은 다루지 않는다 | 확인됨 | [24] |
| 2 | 같은 질문 | 도로명주소 검색 API (추가 후보) | 공식 가이드 페이지를 열지 못했다 | 미확인 | [26][27] |
| 3 | 광역 지역: "서울"이 가게나 건물이 아니라 지역으로 해석되는가 | 카카오 로컬 | 주소 검색의 `address_type` 에 `REGION`("지명") 값이 있다. 키워드 검색은 `meta.same_name.region`("질의어에서 인식된 지역의 리스트")을 준다. "서울"이 실제로 `REGION` 으로 오는지는 명령 K1, K4로 확인해야 한다 | 필드: 확인됨 / 실제 결과: 부분 확인 (PM 호출 대기) | [1] |
| 3 | 같은 질문 | 네이버 NCP Geocoding | 주소 검색이라 가게로 해석될 일은 없다. 다만 "서울"만 넣었을 때 `SIDO` 요소만 채워진 결과가 오는지는 문서에 없다 | 부분 확인 (PM 호출 대기) | [6] |
| 3 | 같은 질문 | Google Geocoding / Places | Geocoding 결과의 `types` 로 지역 타입을 구별할 수 있다. Text Search 문서는 이 API가 모호한 질의용이 아니라고 적고 있어, 지역은 Geocoding으로 보는 편이 맞다. 실제 결과는 명령 G1으로 확인해야 한다 | 부분 확인 (PM 호출 대기) | [14][15] |
| 3 | 같은 질문 | 법정동코드 API (추가 후보) | `locatadd_nm` 으로 시·도와 시·군·구 이름을 코드와 함께 조회할 수 있다. 외부 장소 검색 없이 지역 사전을 만드는 원천이 될 수 있다 | 확인됨 (필드) | [24] |
| 4 | 유형 분류 근거 (7절: 장소 카테고리): 어떤 필드로 `REGION` / `NEIGHBORHOOD` / `SPOT` 을 나누는가 | 카카오 로컬 | 후보 규칙: (a) 주소 검색 `address_type` = `REGION` 이고 `region_3depth_name` 이 비면 `REGION`, (b) `region_3depth_name` 이 있고 번지(`main_address_no`)가 비면 `NEIGHBORHOOD`, (c) 키워드 검색 결과로 `category_group_code` 가 있으면 `SPOT`. `REGION` 결과에 `address` 객체가 오는지는 문서에 없다 | 필드 존재: 확인됨 / 규칙 성립: 부분 확인 (PM 호출 대기) | [1] |
| 4 | 같은 질문 | 네이버 NCP Geocoding | 후보 규칙: `addressElements` 에 `SIDO`/`SIGUGUN` 만 채워지면 `REGION`, `DONGMYUN` 까지만 채워지면 `NEIGHBORHOOD`, `LAND_NUMBER`/`BUILDING_NUMBER`/`BUILDING_NAME` 이 채워지면 `SPOT`. 카테고리 필드가 없어 병원인지는 알 수 없다 | 부분 확인 (PM 호출 대기) | [6] |
| 4 | 같은 질문 | Google Geocoding / Places | 후보 규칙: `types`/`primaryType` 이 `administrative_area_level_1`·`locality` 등이면 `REGION`, `sublocality`·`neighborhood` 계열이면 `NEIGHBORHOOD`, `hospital` 같은 업종 타입이면 `SPOT`. 한국 동이 어느 타입으로 오는지 몰라 경계가 불확실하다 | 필드 존재: 확인됨 / 규칙 성립: 부분 확인 (PM 호출 대기) | [14][16][17] |
| 5 | 동명 후보: "중앙병원"처럼 후보가 여럿이면 목록으로 받는가, 몇 개까지 받는가 | 카카오 로컬 | 목록으로 받는다. 키워드 검색은 `size` 최대 15, `page` 최대 45이고, `pageable_count` 는 "최대: 45"다. 즉 한 질의어로 최대 45개까지 볼 수 있다. `is_end` 로 마지막 페이지를 안다 | 확인됨 | [1] |
| 5 | 같은 질문 | 네이버 NCP Geocoding | 주소 후보를 `count` 최대 100개까지 받는다. 장소 이름의 동명 후보는 이 API의 대상이 아니다 | 확인됨 (주소 후보 수) | [6] |
| 5 | 같은 질문 | Google Places(New) Text Search | `pageSize` 최대 20, `pageToken` 으로 다음 페이지를 받고, 모든 페이지를 합쳐 최대 60개다 | 확인됨 | [15] |
| 5 | 같은 질문 | 네이버 검색 API(지역) | 문서를 열지 못했다 | 미확인 | [28] |
| 6 | 쿼터 (7절: 쿼터): 무료 한도와 초과 시 동작 | 카카오 로컬 | 주소 검색·좌표 변환·키워드 검색·카테고리 검색이 API마다 하루 100,000건, 월 전체 API 합계 3,000,000건이다. 무료 쿼터는 "첫 번째로 활성화한 앱에만" 준다. 무료 쿼터를 다 쓰면 HTTP 429가 오고, 비즈월렛을 연결해 유료로 전환하면 주소·좌표 변환은 건당 0.5원, 키워드·카테고리 검색은 건당 2원이다 | 확인됨 | [2][3][4] |
| 6 | 같은 질문 | 네이버 NCP Geocoding | Geocoding의 무료 이용량은 대표 계정(Maps를 처음 쓴 계정)에만 있고, 대표 계정이 아니면 호출할 때마다 과금된다. 무료 건수와 단가는 요금 페이지가 자바스크립트로만 그려져 확인하지 못했다. 한도 초과는 HTTP 429(코드 400 Quota Exceeded)다 | 부분 확인 | [7][8][29] |
| 6 | 같은 질문 | Google Geocoding / Places | 월 무료 한도는 SKU마다 따로 있다. Geocoding은 월 10,000건 무료, 그 뒤 1,000건당 5.00달러. Text Search Pro(이름·주소·좌표·타입 필드)는 월 5,000건 무료, 그 뒤 1,000건당 32.00달러. Text Search Essentials(ID만)는 무제한 무료다. 한도를 넘으면 과금되고, 쿼터 초과 응답은 `OVER_QUERY_LIMIT` 이다 | 확인됨 | [14][20] |
| 7 | 약관 (7절: 약관): 저장·캐싱 허용 여부와 기간, 자사 지도 위 표시 의무, 출처 표시, 상업 이용 | 카카오 로컬 | 저장·캐싱: 운영정책 제5조 20호는 사용자 환경 개선 목적 밖의 캐시와 최신 데이터로 유지하지 않는 캐시를 금지하고, 30호는 사전 승낙 없는 복사·복제와 타인 제공을 금지한다. 허용 기간은 정해져 있지 않다. 자사 지도 표시 의무: 공식 문서에서 찾지 못했다. 출처 표시: 로컬 API에 대한 표시 의무 조항은 찾지 못했고, 카카오 상표를 쓸 때는 제18조를 따라야 한다. 상업 이용: 제5조 11호는 "허가되지 않은 상업적 목적"의 판매·재판매를 금지하고, 유료 요금제가 있다 | 부분 확인 (10절 D1) | [2][3][5] |
| 7 | 같은 질문 | 네이버 NCP Geocoding | 저장·캐싱: Maps 서비스 이용약관 제7조 ⑪항은 결과 데이터를 "받은 즉시 1회" 쓰는 것만 허용하고, 저장·DB화·재사용을 금지한다. 같은 조 ⑨항은 사전 동의 없는 복제·저장·가공·배포와 제3자 제공을 금지한다. 자사 지도 표시 의무: 조항이 없다. 출처 표시: 제7조 ⑩항에 따라 회사가 로고 게재를 요청할 수 있다. 상업 이용: 제7조 ⑫항은 API·SDK 재판매만 금지한다. 다만 PDF가 v0.4(2025-03-20 시행)이고, 이것이 현행판인지는 약관 페이지가 자바스크립트로만 그려져 대조하지 못했다 | 부분 확인 | [10][11] |
| 7 | 같은 질문 | Google Geocoding / Places | 저장·캐싱: Places 콘텐츠는 허용된 예외를 넘어 미리 받거나 캐시하거나 저장하면 안 된다. `place_id` 는 예외라 무기한 저장할 수 있다. 위도·경도의 임시 캐시 기간은 Service Specific Terms에 있는데, 이 페이지 본문을 열지 못했다. 자사 지도 표시 의무: Geocoding·Places 결과를 지도에 표시하면 "must be shown on a Google Map" 이다. 출처 표시: 지도 없이 표시할 때는 Google 로고(공간이 좁으면 "Google Maps" 문구)를 붙여야 한다. 상업 이용: 유료 SKU 체계는 확인했지만, 약관 본문을 열지 못했다 | 부분 확인 (10절 D3) | [18][19][20][22][23] |
| 8 | 좌표계: 응답 좌표계와 변환 필요성 | 카카오 로컬 | `input_coord`·`output_coord` 기본값은 `WGS84` 이고, `x` 는 경도, `y` 는 위도다. 좌표계 변환 API는 `WGS84`, `WCONGNAMUL`, `CONGNAMUL`, `WTM`, `TM`, `KTM`, `UTM`, `BESSEL`, `WKTM`, `WUTM` 을 지원한다. 목록에 EPSG:5179(UTM-K)라는 이름은 없다 | 확인됨 | [1] |
| 8 | 같은 질문 | 네이버 NCP Geocoding | Geocoding 문서는 `x`=경도, `y`=위도라고만 적고 측지계를 밝히지 않는다. Reverse Geocoding은 기본값이 `EPSG:4326`(WGS84)이고 `EPSG:3857`, `NHN:2048`(UTM-K)도 받는다 | Geocoding: 부분 확인 / Reverse: 확인됨 | [6][13] |
| 8 | 같은 질문 | Google Geocoding / Places | `geometry.location.lat/lng`(Geocoding), `location.latitude/longitude`(Places), 단위는 도(degree)다. 측지계 이름은 열어 본 페이지들에 없다 | 부분 확인 | [14][16] |
| 9 | 대한민국 밖 판정: 국외 장소를 구별할 수 있는가 | 카카오 로컬 | 응답에 국가 필드가 없고, 문서에 커버리지 국가도 적혀 있지 않다. 국외 지명을 넣으면 어떻게 되는지는 문서로 알 수 없다 | 미확인 (10절 D5) | [1][2] |
| 9 | 같은 질문 | 네이버 NCP Geocoding | Geocoding 응답에는 국가 필드가 없다. Reverse Geocoding은 `region.area0` 에 국가 코드(`kr`)를 준다. 바다 좌표는 결과가 없다 | Geocoding: 미확인 / Reverse: 확인됨 | [6][13] |
| 9 | 같은 질문 | Google Geocoding / Places | 주소 요소 타입 `country` 가 있다. Geocoding은 `components=country:KR` 로 결과를 한국으로 제한할 수 있다 | 확인됨 (필드·필터) | [14] |
| 10 | 폐업·개명 장소: 문서에서 확인되는 처리 방식 | 카카오 로컬 | 키워드 검색 응답 필드 목록에 영업 상태 필드가 없다. 폐업한 장소가 결과에서 빠지는지는 문서에 없다 | 필드 없음: 확인됨 / 동작: 미확인 | [1] |
| 10 | 같은 질문 | 네이버 NCP Geocoding | 주소 검색이라 해당이 없다. 개명된 건물명이 어떻게 처리되는지는 문서에 없다 | 미확인 | [6] |
| 10 | 같은 질문 | Google Places(New) | `businessStatus` 가 `OPERATIONAL`, `CLOSED_TEMPORARILY`, `CLOSED_PERMANENTLY`, `FUTURE_OPENING` 값을 갖는다. 폐업 후 이전한 장소는 `movedPlaceId` 를 준다. 폐업한 병원이 실제로 검색에 남아 있는지는 문서에 없다 | 필드: 확인됨 / 동작: 미확인 | [16] |
| 10 | 같은 질문 | 도로명주소 검색 API (추가 후보) | 공식 가이드를 열지 못해 변동 이력 옵션을 확인하지 못했다 | 미확인 | [26] |

## 3. 출처 카드

### 출처 — 카카오 로컬 API
- 구분: 제품 정의서 후보
- 제공 기관 / 공식 문서: 카카오 / https://developers.kakao.com/docs/latest/ko/local/dev-guide [1], 이용 정책 [2], 쿼터 [3], 운영 정책 [5]
- 무엇을 얻나: 주소 검색(`/v2/local/search/address`), 좌표→행정구역(`/v2/local/geo/coord2regioncode`), 좌표→주소(`/v2/local/geo/coord2address`), 좌표계 변환(`/v2/local/geo/transcoord`), 키워드 장소 검색(`/v2/local/search/keyword`), 카테고리 장소 검색(`/v2/local/search/category`). 장소 이름, 지번·도로명 주소, 시도/구/동 계층, 행정동·법정동 이름과 코드, 카테고리 그룹 코드(병원 `HP8` 등), 장소 ID, 카카오맵 장소 상세 URL, 좌표.
- 접근 방법: REST API (모두 `GET`, 기본 주소 `https://dapi.kakao.com/v2/local/`, JSON 또는 XML)
- 인증·키: 필요. REST API 키를 `Authorization: KakaoAK ${REST_API_KEY}` 헤더로 보낸다. 서버 키로 쓰며 브라우저에 둘 필요가 없다. 2026-07-21부터 앱 관리 페이지에서 "카카오맵 API"를 활성화해야 쓸 수 있고, 관리자 심사는 없어졌다 [4]. 개발자 계정에서 카카오맵을 **처음 활성화한 앱만** 무료 쿼터를 받는다. 두 번째 앱부터는 비즈월렛 연결과 유료 설정이 필요하다 [2][4]. 그래서 PM 계정에 이미 카카오맵을 활성화한 앱이 있다면 이 서비스용 새 앱은 무료가 아니다.
- 요금·쿼터: API마다 하루 100,000건, 월 전체 API 합계 3,000,000건. 무료 쿼터를 넘으면 HTTP 429. 유료 전환 시 주소·좌표 변환 건당 0.5원, 키워드·카테고리 검색 건당 2원. 문서는 쿼터와 요금이 "추후 변경될 수 있습니다"라고 적는다 [2][3]. 운영정책 제6조의 Hard 한도는 하루 전체 호출 1,000만 건, 요청 30초 이내다 [5].
- 커버리지: 연도 범위는 해당 없음(현재 시점의 장소·주소). 지역은 지번·도로명 주소 체계를 지원한다고 적혀 있다 [2]. 국외 지원 여부는 문서에 없다. 한국어 데이터가 기본이다. 데모 장소 3개의 실제 결과는 PM 호출 전까지 알 수 없다.
- 날짜 정밀도: 해당 없음 — 장소 검색은 촬영일이 있는 미디어를 주지 않는다
- 응답 메타데이터: 좌표 `x`(경도), `y`(위도), 기본 WGS84 [1]. 촬영일과 라이선스 필드는 없다(미디어가 아님).
- 라이선스·약관 핵심:
  - 화면 표시: 검색 결과를 화면에 보여주는 것을 막는 조항은 찾지 못했다.
  - 캐싱·저장: 운영정책 제5조 20호는 "사용자 환경을 개선하기 위한 목적 외 다른 목적으로" 받은 데이터를 캐시하는 것과, 캐시한 뒤 최신 데이터로 유지하지 않는 것을 금지한다. 제5조 30호는 사전 승낙 없이 데이터를 복사·복제하거나 타인에게 제공하는 것을 금지한다 [5]. 허용 기간은 없다. 공식 문서의 범위(조건부 캐시 허용)와 DevTalk 답변의 범위("실시간 호출로만", 아래 단서 [S1]~[S3])가 다르다. 그래서 해석을 확정하지 않고 10절 D1로 올렸다.
  - 상업 이용: 운영정책 제5조 11호는 "허가되지 않은 상업적 목적"의 판매·거래·재판매를 금지한다 [5]. 유료 요금제는 있다 [3]. 상업 서비스에 써도 된다는 명시 문장은 공식 문서에서 찾지 못했다.
  - 출처 표시: 로컬 API 결과에 대한 출처 표시 의무 조항은 찾지 못했다. 카카오 상표를 쓰면 ® 표기, 변형 금지 등 운영정책 제18조를 따라야 한다 [5]. 카카오맵 로고와 디자인 가이드는 권장 사항으로 제공된다 [2].
  - 변경 허용: 해당 없음 — 미디어가 아니다. 데이터 "변경"은 제5조 30호의 금지 대상에 들어 있다 [5].
- 이 서비스에 맞는가: **조건부**.
  - 비상업일 때: 장소 이름·동·광역을 한 제공자로 처리하고 무료 한도도 넉넉하다. 단, 결과를 저장하지 않고 매 요청마다 실시간으로 호출하는 설계가 전제다.
  - 상업일 때: 위 조건은 같다. 공식 문서에는 재판매 금지만 있지만, 상업 이용을 명시적으로 허용하는 문장은 확인하지 못했다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [1] 2026-10-08 |
  | 인증·키 | 확인됨 | [1][2][4] 2026-10-08 |
  | 요금·쿼터 | 확인됨 | [2][3][4] 2026-10-08 |
  | 커버리지 (필수) | 부분 확인 (PM 호출 대기) — 데모 장소 결과와 국외 동작은 문서에 없음 | [1][2] 2026-10-08 |
  | 날짜 정밀도 | 해당 없음 — 미디어 아님 | — |
  | 캐싱·저장 (필수) | 부분 확인 — 공식 조항이 조건부 허용으로 읽히고 허용 범위가 애매함, 10절 D1 | [5] 제5조 20호·30호, 2026-10-08 |
  | 출처 표시 (필수) | 부분 확인 — 의무 조항을 찾지 못함(없다는 확인은 아님) | [2][5] 제18조, 2026-10-08 |
  | 상업 이용 (필수) | 부분 확인 — 재판매 금지 조항과 유료 요금제만 확인함 | [3][5] 제5조 11호, 2026-10-08 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 부분 확인 (PM 호출 대기)
- 샘플 응답 확인: 6절 명령 K1~K6 참조 (키가 필요해 직접 호출하지 않음)
- 근거 링크: [1][2][3][4][5] (단서: [S1][S2][S3])

### 출처 — 네이버 클라우드 플랫폼 Maps Geocoding
- 구분: 제품 정의서 후보
- 제공 기관 / 공식 문서: 네이버클라우드 / https://api.ncloud-docs.com/docs/application-maps-geocoding [6], Maps 개요 [7][8], Maps 서비스 이용약관 [10]
- 무엇을 얻나: 주소 문자열 → 도로명·지번·영문 주소, 주소 요소(`SIDO`, `SIGUGUN`, `DONGMYUN`, `RI`, `ROAD_NAME`, `BUILDING_NUMBER`, `BUILDING_NAME`, `LAND_NUMBER`, `POSTAL_CODE`)와 `code`, 좌표 [6]. 짝을 이루는 Reverse Geocoding은 좌표 → 법정동·행정동 코드, 국가 코드 `kr` 이 담긴 `area0`, 시도부터 리까지의 `area1~4` 를 준다 [13]. **장소(POI) 이름 검색은 문서에 없다.**
- 접근 방법: REST API (`GET https://maps.apigw.ntruss.com/map-geocode/v2/geocode`) [6][7]
- 인증·키: 필요. NCP 콘솔에서 Maps 이용을 신청하고 Application을 등록하면 Client ID와 Client Secret이 나온다. 이 값을 `x-ncp-apigw-api-key-id`, `x-ncp-apigw-api-key` 헤더로 보낸다 [7]. 이용 신청 때 서비스 약관 동의가 필요하다 [9]. 서버 키로 쓴다. 승인 소요 기간은 문서에 없다.
- 요금·쿼터: 무료 이용량은 대표 계정에만 있고, 대표 계정이 아니면 호출할 때마다 과금된다 [8]. 무료 건수와 단가는 요금 페이지가 자바스크립트로만 그려져 읽지 못했다(미확인) [29]. 2025년에 기존 AI·NAVER API 지도 상품의 무료 이용량을 끝내고 새 Maps 상품으로 옮긴다는 공지가 있다 [12]. 한도 초과는 HTTP 429다 [7].
- 커버리지: 연도 범위는 해당 없음. 국내 주소 체계(행정동·법정동·시도)를 쓰고 예시도 모두 한국 주소다. 국외 지원 여부는 문서에 없다 [6]. 주소만 다룬다.
- 날짜 정밀도: 해당 없음 — 미디어 아님
- 응답 메타데이터: `x`(경도), `y`(위도)는 문자열이고, Geocoding 문서에는 측지계가 없다 [6]. Reverse Geocoding 기본값은 EPSG:4326이다 [13]. 라이선스 필드는 없다.
- 라이선스·약관 핵심 (Maps 서비스 이용약관 v0.4, 2025-03-20 시행 PDF [10]):
  - 화면 표시: 결과를 자기 서비스에서 즉시 쓰는 것은 허용된다(제7조 ⑪).
  - 캐싱·저장: 제7조 ⑪항에 따르면 결과 데이터는 "값을 리턴 받는 즉시 1회 자신의 서비스에서 사용하는 것만 허용"된다. 별도 저장, DB화, 재사용은 금지다. 좌표를 모아 재사용하는 것은 "엄격히 금지"한다고 예까지 들어 적었다. 제7조 ⑨항은 사전 동의 없는 복제·저장·가공·배포와 제3자 제공을 금지한다.
  - 상업 이용: 제7조 ⑫항은 API·SDK를 그대로 또는 리패키징해서 재판매하는 것만 금지한다. 상업 이용 자체를 금지하는 조항은 없다.
  - 출처 표시: 제7조 ⑩항에 따라 회사가 결과 데이터나 애플리케이션에 로고나 지정 표시를 요청할 수 있고, 고객은 따라야 한다. 구체적 표시 방식은 문서에 없다.
  - 변경 허용: 해당 없음 — 미디어 아님. 결과 데이터 "가공"은 제7조 ⑨항의 금지 대상에 들어 있다.
  - 현행판 여부: 약관 페이지 [11]은 본문이 자바스크립트로 그려져 PDF와 같은 판인지 대조하지 못했다.
- 이 서비스에 맞는가: **부적합 (단독 사용 시)**.
  - 비상업일 때: "서울대학교병원" 같은 장소 이름 검색 근거가 없어 `SPOT` 을 처리할 수 없다. 장소 검색을 하려면 별도의 네이버 검색 API(지역, 미확인)가 필요하고, 저장 금지 조항도 가장 엄격하다. 동·광역 해석과 법정동·행정동 코드에는 쓸 수 있다.
  - 상업일 때: 비상업과 같다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [6][7] 2026-10-08 (문서 갱신 2026-09-17) |
  | 인증·키 | 확인됨 | [7] 2026-10-08, [9] 2026-10-09 |
  | 요금·쿼터 | 부분 확인 — 대표 계정 규칙만 확인, 건수와 단가는 미확인 | [8][12] 2026-10-09, [29] 2026-10-09 |
  | 커버리지 (필수) | 부분 확인 (PM 호출 대기) — 주소 전용, 장소 이름 검색 근거 없음 | [6] 2026-10-08 |
  | 날짜 정밀도 | 해당 없음 — 미디어 아님 | — |
  | 캐싱·저장 (필수) | 부분 확인 — 조항 자체는 분명함(금지). 단 PDF v0.4가 현행판인지 대조하지 못함 | [10] 제7조 ⑨⑪, 2026-10-09 / [11] 2026-10-09 |
  | 출처 표시 (필수) | 부분 확인 — 회사가 로고 게재를 요청할 수 있다는 조항만 있고, 구체적 방식은 없음 | [10] 제7조 ⑩, 2026-10-09 |
  | 상업 이용 (필수) | 부분 확인 — 재판매 금지 조항만 있고, 현행판 대조를 하지 못함 | [10] 제7조 ⑫, 2026-10-09 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 부분 확인 (PM 호출 대기)
- 샘플 응답 확인: 6절 명령 N1~N4 참조
- 근거 링크: [6][7][8][9][10][11][12][13][29]

### 출처 — Google Places API (New) Text Search / Geocoding API
- 구분: 제품 정의서 후보
- 제공 기관 / 공식 문서: Google / https://developers.google.com/maps/documentation/places/web-service/text-search [15], https://developers.google.com/maps/documentation/geocoding/requests-geocoding [14], 정책 [18][19], 요금 [20], 커버리지 [21]
- 무엇을 얻나:
  - Text Search: 장소 ID, 이름(`displayName`), 주소(`formattedAddress`, `addressComponents`), 좌표(`location`), 타입(`types`, `primaryType`), 영업 상태(`businessStatus`), 이전한 장소(`movedPlaceId`) [15][16].
  - Geocoding: 주소·지명 → `address_components`, `formatted_address`, `geometry.location`, `location_type`, `types`, `place_id`, `partial_match` [14].
- 접근 방법: REST API. Text Search는 `POST https://places.googleapis.com/v1/places:searchText` 이고, `X-Goog-Api-Key` 와 `X-Goog-FieldMask` 헤더가 필수다 [15]. Geocoding은 `GET https://maps.googleapis.com/maps/api/geocode/json` 이고 `key` 파라미터를 쓴다 [14].
- 인증·키: 필요. Google Cloud 프로젝트, API 키, 결제 계정이 필요하다. 서버 키로 쓴다. 키 제한 방법은 이 작업에서 보지 않았고 T3에서 확인한다.
- 요금·쿼터: SKU마다 월 무료 한도가 따로 있다. Geocoding 10,000건(이후 1,000건당 5.00달러), Text Search Pro 5,000건(이후 32.00달러), Text Search Enterprise 1,000건(이후 35.00달러), Text Search Essentials(ID만) 무제한 무료 [20]. 과금 등급은 FieldMask에 넣은 필드로 정해진다. 이름·주소·좌표를 받으려면 Pro 등급이다 [15].
- 커버리지: 공식 커버리지 표에서 한국(KR)은 Geocoding이 "good data quality" 표시(⬤)이고, 자동차·도보·자전거 길찾기와 교통 정보는 미제공(—)이다. 이 표에 Places 열은 없다 [21]. 한국어 장소 데이터의 양과 정확도는 문서로 알 수 없다.
- 날짜 정밀도: 해당 없음 — 미디어 아님 (Places 사진은 이 작업 범위 밖)
- 응답 메타데이터: 위도·경도는 도 단위이고 측지계 이름은 열어 본 문서에 없다 [14][16]. 라이선스 필드는 없지만 Text Search에는 `attributions` 필드가 있다 [15].
- 라이선스·약관 핵심:
  - 화면 표시: Geocoding·Places 결과를 지도에 표시하면 Google 지도 위에 보여야 한다. 지도 없이 표시하면 Google 로고를 붙여야 하고("you must include the Google logo"), 공간이 좁으면 "Google Maps" 문구로 대신할 수 있다 [18][19]. Google이 제3자 출처를 함께 주면 그 출처도 표시해야 한다 [18][19].
  - 캐싱·저장: Places 콘텐츠는 허용된 예외를 넘어 미리 받거나 캐시하거나 저장하면 안 된다. `place_id` 는 캐시 제한에서 빠져 무기한 저장할 수 있다 [18][19]. 위도·경도를 얼마나 캐시할 수 있는지는 Service Specific Terms에 있는데, 이 페이지 본문을 열지 못했다 [22]. 검색 결과 스니펫에서 본 "30일" 같은 기간은 근거로 쓰지 않았다.
  - 상업 이용: 유료 SKU 체계는 확인했다 [20]. 서비스 약관 본문 [23]은 열지 못했다.
  - 변경 허용: 해당 없음 — 미디어 아님.
- 이 서비스에 맞는가: **조건부**.
  - 비상업일 때: 동명 후보 목록, 폐업 상태, `place_id` 저장이 가능해 기능 면에서는 좋다. 하지만 (1) 결과를 지도에 올리면 Google 지도를 써야 한다. 이 서비스는 항공사진(T2)과 로드뷰(T3)를 다른 제공자에게서 받을 가능성이 크다. (2) 한국 행정동·법정동 코드가 없어 지역 사전과 직접 연결할 수 없다. (3) Pro 무료 한도가 월 5,000건으로 카카오보다 훨씬 작다.
  - 상업일 때: 비상업과 같다. 약관 본문을 확인해야 한다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [14][15] 2026-10-09 |
  | 인증·키 | 확인됨 (헤더·파라미터) | [14][15] 2026-10-09 |
  | 요금·쿼터 | 확인됨 | [20] 2026-10-09 (페이지 갱신 2026-10-07) |
  | 커버리지 (필수) | 부분 확인 (PM 호출 대기) — 한국 Geocoding 품질 표시는 확인, Places 품질은 문서에 없음 | [21] 2026-10-09 |
  | 날짜 정밀도 | 해당 없음 — 미디어 아님 | — |
  | 캐싱·저장 (필수) | 부분 확인 — `place_id` 예외와 일반 금지는 확인, 위도·경도 캐시 기간 조항은 열지 못함 | [18][19] 2026-10-09 / [22] 2026-10-09 |
  | 출처 표시 (필수) | 확인됨 | [18][19] 2026-10-09 (페이지 갱신 2026-10-07) |
  | 상업 이용 (필수) | 부분 확인 — 약관 본문을 열지 못함 | [20][23] 2026-10-09 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 부분 확인 (PM 호출 대기)
- 샘플 응답 확인: 6절 명령 G1~G4 참조
- 근거 링크: [14][15][16][17][18][19][20][21][22][23]

### 출처 — 행정안전부 도로명주소 검색 API
- 구분: 추가 후보
- 제공 기관 / 공식 문서: 행정안전부 주소정보 서비스 / https://business.juso.go.kr/jst/jstRoadNmAddrApiSearch [26] (본문을 읽지 못함), 신청 페이지 [27] (HTTP 404)
- 무엇을 얻나: 미확인. 검색 결과 스니펫에는 행정구역코드(`admCd`), 시도·시군구·읍면동명, 건물명이 응답에 있다고 나오지만, 공식 페이지를 직접 읽지 못해 근거로 쓰지 않았다.
- 접근 방법: 미확인 (REST API로 보이지만 명세를 읽지 못함)
- 인증·키: 미확인 (승인키가 필요하다는 단서만 있음)
- 요금·쿼터: 미확인
- 커버리지: 미확인
- 날짜 정밀도: 해당 없음 — 미디어 아님
- 응답 메타데이터: 미확인
- 라이선스·약관 핵심: 미확인
- 이 서비스에 맞는가: **미정**. 비상업일 때와 상업일 때 모두 약관을 읽지 못해 판단할 수 없다. 주소 검색만 다루고 장소 이름 검색은 없을 것으로 보여(단서 수준), 1순위 후보는 아니다. 지역 사전의 원천으로는 아래 법정동코드 API가 더 직접적이다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 미확인 — 공식 가이드가 본문 없이 열리거나 404 | [26][27] 2026-10-09 |
  | 인증·키 | 미확인 | [27] 2026-10-09 |
  | 요금·쿼터 | 미확인 | [26] 2026-10-09 |
  | 커버리지 (필수) | 미확인 | [26] 2026-10-09 |
  | 날짜 정밀도 | 해당 없음 — 미디어 아님 | — |
  | 캐싱·저장 (필수) | 미확인 | [26] 2026-10-09 |
  | 출처 표시 (필수) | 미확인 | [26] 2026-10-09 |
  | 상업 이용 (필수) | 미확인 | [26] 2026-10-09 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 미확인
- 샘플 응답 확인: 해당 없음 — 키가 필요하고, 공식 엔드포인트를 확인하지 못해 6절 명령도 쓰지 않았다. 6절 끝에 PM이 브라우저로 확인할 URL을 남겼다.
- 근거 링크: [26][27]

### 출처 — 행정안전부 행정표준코드 법정동코드 API (공공데이터포털)
- 구분: 추가 후보
- 제공 기관 / 공식 문서: 행정안전부(디지털인프라혁신과) / https://www.data.go.kr/data/15077871/openapi.do [24]
- 무엇을 얻나: 법정동코드 목록. `region_cd`, `sido_cd`, `sgg_cd`, `umd_cd`, `ri_cd`, `locatjumin_cd`, `locatjijuk_cd`, `locatadd_nm`(지역 주소명), `locat_order`, `locat_rm`, `locathigh_cd`(상위 코드), `locallow_nm`(최하위 지역명), `adpt_de` [24]. 좌표는 없다. 행정동 코드도 없다.
- 접근 방법: REST API (`GET https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList`, JSON·XML) [24]
- 인증·키: 필요. 공공데이터포털 서비스 키(`ServiceKey`)를 쓴다. 개발 단계와 운영 단계 모두 "자동승인"이다 [24]. 서버 키다.
- 요금·쿼터: "무료". 개발 계정은 하루 10,000건이고, 운영 계정은 활용 사례를 등록하면 늘릴 수 있다 [24].
- 커버리지: 전국 법정동(시도~리). 업데이트 주기는 "실시간", 수정일은 2025-11-07이다 [24]. 지역 사전용이라 데이터 크기가 작고, 한 번 받아 두면 외부 호출 없이 쓸 수 있는 종류의 데이터다(저장 허용 여부는 아래 라이선스 참고).
- 날짜 정밀도: 해당 없음 — 미디어 아님
- 응답 메타데이터: 좌표 없음, 라이선스 필드 없음 [24]
- 라이선스·약관 핵심: 데이터셋 페이지의 이용허락범위는 "이용허락범위 제한 없음"이고, 공공누리 유형 표시는 없다 [24]. 공공데이터포털 이용정책 페이지는 공공누리 유형을 붙여 이용 범위를 표시하라고만 적고, 유형이 없는 경우는 정의하지 않는다 [25]. 다른 데이터셋들은 같은 "제한 없음" 문구 아래에 제2·4유형을 함께 표시하기도 하므로, 이 문구만으로 상업 이용과 변경이 자유롭다고 해석할 수 없다. 그래서 10절 D4로 올렸고, PM이 2026-10-27에 **결정 #10 (a)** 를 골랐다: 비상업 MVP에서 출처를 표시하고 쓴다. 이 카드의 `확인됨` 은 그 결정을 전제로 한다(법률 자문 아님).
- 이 서비스에 맞는가: **적합 (결정 #10 (a) 범위 안에서)**.
  - 비상업일 때(결정 #10 (a)): `data/regions.json` 의 시·도와 시·군·구 별칭·코드의 원천으로 적합하다. `REGION` 을 외부 장소 검색 없이 지역 사전으로 판정하면, 저장 금지 약관이 있는 카카오·네이버 호출을 줄일 수 있다. 출처 표시는 "행정안전부 법정동코드, 공공데이터포털" 로 붙인다(결정 #10 (a)).
  - 상업일 때: 공공누리 유형이 표시되지 않아 상업 이용 조건을 확정하지 못했다(D4 (b)). 결정 #10 (a)는 이 문제를 풀지 않는다. 상업화를 정하면 행정안전부에 문의한다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 확인됨 | [24] 2026-10-09 |
  | 인증·키 | 확인됨 | [24] 2026-10-09 |
  | 요금·쿼터 | 확인됨 | [24] 2026-10-09 |
  | 커버리지 (필수) | 확인됨 (필드와 범위) | [24] 2026-10-09 |
  | 날짜 정밀도 | 해당 없음 — 미디어 아님 | — |
  | 캐싱·저장 (필수) | 확인됨 (결정 #10 (a)) — 저장을 막는 표시가 없고, 비상업 범위에서 한 번 내려받아 사전으로 두는 것을 PM이 허용함 | [24][25] 2026-10-09, 2026-10-27 + 결정 #10 |
  | 출처 표시 (필수) | 확인됨 (결정 #10 (a)) — "행정안전부 법정동코드, 공공데이터포털" 표시 | 결정 #10 (2026-10-27) |
  | 상업 이용 (필수) | 비상업 한정으로 확인됨 (결정 #10 (a)). 상업은 미확인 — "제한 없음" 문구의 의미가 정의되어 있지 않음, 10절 D4 (b) | [24][25] 2026-10-27 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: **확인됨 (결정 #10 (a), 출처 표시: 행정안전부 법정동코드, 공공데이터포털)**
- 샘플 응답 확인: 실제 응답 모양은 보지 못했다(키 필요). 6절 명령 R1이 처음으로 전체 목록을 받는다. 응답 모양과 폐지 행 처리는 content-curator가 T1에서 받은 파일로 확인한다.
- 데이터셋 페이지 재확인 (2026-10-27, 아래 표): 2026-10-09 기록과 같다.

  | 항목 | 2026-10-09 기록 | 2026-10-27 재확인 | 변화 |
  |---|---|---|---|
  | 수정일 | 2025-11-07 | 2025-11-07 (등록일 2021-04-02) | 없음 |
  | 응답 필드 | `region_cd`, `sido_cd`, `sgg_cd`, `umd_cd`, `ri_cd`, `locatjumin_cd`, `locatjijuk_cd`, `locatadd_nm`, `locat_order`, `locat_rm`, `locathigh_cd`, `locallow_nm`, `adpt_de` (+ `resultCode`, `resultMsg`, `numOfRows`, `pageNo`, `totalCount`, `type`) | 같음 | 없음 |
  | 요청 변수 | `ServiceKey`, `pageNo`, `numOfRows`, `type`, `locatadd_nm`(선택, 최대 50자) | 같음. `type` 기본값 xml | 없음 |
  | 이용허락범위 | 이용허락범위 제한 없음 (공공누리 유형 표시 없음) | 같음. DCAT 메타데이터에도 별도 라이선스 필드는 없고 같은 문구만 있음 | 없음 |
  | 쿼터·승인 | 개발계정 10,000건/일, 운영계정은 활용사례 등록 시 증가 신청, 개발·운영 자동승인 | 같음 | 없음 |
  | 업데이트 주기 | 실시간 | 실시간 | 없음 |
  | 새로 보인 것 | — | (1) 활용신청 수 7,461 (2) 상세설명: "행정표준코드관리시스템(code.go.kr)에서 현재 제공 중인 법정동 코드"에 대한 API (3) 기술문서 docx는 링크가 보이지 않아 읽지 못함 (4) 페이지에 폐지 코드 안내는 없음 | 해석에 영향 없음 |
- 행 고르는 조건과 폐지 코드: 6절 R1의 설명 참조. 폐지 코드를 거르는 공식 근거는 문서에 없다(응답 필드에 폐지 여부·폐지일이 없고, 상세설명의 "현재 제공 중인" 한 문장이 현존 코드만 준다는 단서일 뿐이다). **T1에서 파일로 확인**한다.
- 키 없이 받을 수 있는 공식 파일(대안): 6절 R1 아래 "대안" 참조. 같은 원천(행정표준코드관리시스템)의 국토교통부 파일 두 개가 있다.
- 근거 링크: [24][25][30][31][32][33]

### 출처 — 네이버 검색 API (지역)
- 구분: 추가 후보
- 제공 기관 / 공식 문서: 네이버 / https://developers.naver.com/docs/serviceapi/search/local/local.md [28] (이 도구로는 이 도메인에 접근할 수 없음)
- 무엇을 얻나: 미확인. NCP Geocoding이 장소 이름 검색을 지원하지 않아서, 네이버 쪽에서 `SPOT` 을 처리하려면 이 API가 필요할 것으로 보고 후보에 넣었다.
- 접근 방법: 미확인
- 인증·키: 미확인
- 요금·쿼터: 미확인
- 커버리지: 미확인
- 날짜 정밀도: 해당 없음 — 미디어 아님
- 응답 메타데이터: 미확인 (좌표계 포함)
- 라이선스·약관 핵심: 미확인
- 이 서비스에 맞는가: **미정**. 비상업일 때와 상업일 때 모두 문서를 읽지 못해 판단할 수 없다. 네이버를 지도·로드뷰 제공자로 고를 경우(T3)에만 다시 볼 가치가 있다.
- 항목별 확신도:
  | 항목 | 확신도 | 근거 (URL, 확인 일자) |
  |---|---|---|
  | 접근 방법 (필수) | 미확인 — 도메인 접근 불가 | [28] 2026-10-09 |
  | 인증·키 | 미확인 | [28] 2026-10-09 |
  | 요금·쿼터 | 미확인 | [28] 2026-10-09 |
  | 커버리지 (필수) | 미확인 | [28] 2026-10-09 |
  | 날짜 정밀도 | 해당 없음 — 미디어 아님 | — |
  | 캐싱·저장 (필수) | 미확인 | [28] 2026-10-09 |
  | 출처 표시 (필수) | 미확인 | [28] 2026-10-09 |
  | 상업 이용 (필수) | 미확인 | [28] 2026-10-09 |
  | 변경 허용 (미디어 출처면 필수) | 해당 없음 — 미디어 아님 | — |
- 확신도: 미확인
- 샘플 응답 확인: 해당 없음 — 공식 엔드포인트를 확인하지 못했다. 6절 끝의 브라우저 확인 목록을 참고한다.
- 근거 링크: [28]

## 4. 미디어 레코드 필드 매핑

장소 검색은 미디어를 주지 않는다. 그래서 계획서 T1 기준에 따라 6.2절 필드 중 `lat`·`lng`·`place_ref` 행만 채우고, 나머지는 `해당 없음` 으로 둔다. 각 행의 판단 근거는 "장소 검색 응답에 미디어가 없음"이다 [1][6][14][15].

| 필드 | 카카오 로컬 | 네이버 NCP Geocoding | Google Places / Geocoding | 법정동코드 API |
|---|---|---|---|---|
| id | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| type | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| url | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| thumbnail_url | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| source_name | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| source_url | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |
| license | 해당 없음 — 미디어 아님. 응답에 라이선스 필드 없음 [1] | 해당 없음 — 미디어 아님. 응답에 라이선스 필드 없음 [6] | 해당 없음 — 미디어 아님. 응답에 라이선스 필드 없음 [14][15] | 해당 없음 — 미디어 아님. 응답에 라이선스 필드 없음 [24] |
| attribution | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님. 단, 장소 정보를 보여줄 때 Google 로고(공간이 좁으면 "Google Maps" 문구) 표시 의무가 있다 [19] | 해당 없음 — 미디어 아님 |
| display_mode | 해당 없음 — 미디어 아님. 약관상 표시 제한 조항 못 찾음 [5] | 해당 없음 — 미디어 아님. 결과는 받은 즉시 1회 사용 [10] 제7조 ⑪ | 해당 없음 — 미디어 아님. 지도에 올리면 Google 지도 필수 [18][19] | 해당 없음 — 미디어 아님 |
| captured_at | 해당 없음 — 촬영일 필드 없음 [1] | 해당 없음 — 촬영일 필드 없음 [6] | 해당 없음 — 촬영일 필드 없음 [14][16] | 해당 없음 — 촬영일 필드 없음 [24] |
| date_precision | 해당 없음 — 촬영일이 없어 정밀도도 없음 [1] | 해당 없음 — 촬영일이 없어 정밀도도 없음 [6] | 해당 없음 — 촬영일이 없어 정밀도도 없음 [14][16] | 해당 없음 — 촬영일이 없어 정밀도도 없음 [24] |
| lat·lng 또는 place_ref | `lat` = `y`, `lng` = `x` (WGS84 기본) [1]. `place_ref` 후보 = `id`(장소 ID). 단, ID를 저장해도 되는지는 D1 | `lat` = `y`, `lng` = `x` (문자열, 측지계 미기재) [6]. `place_ref` = 채울 수 없음 — 응답에 ID 필드가 없고 저장도 금지 [10] | `lat` = `location.latitude` 또는 `geometry.location.lat`, `lng` = `location.longitude` 또는 `geometry.location.lng` [14][16]. `place_ref` = `id`/`place_id` (무기한 저장 가능 [18][19]) | 좌표 없음. `place_ref` = `region_cd` [24] |
| retrieved_at | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 | 해당 없음 — 미디어 아님 |

결론: 장소 검색 결과는 미디어가 아니므로 `display_mode` 의 `embed`/`link_only` 판단 대상이 아니다. 대신 장소 결과를 화면에 보여주는 조건은 이렇다.
- 비상업일 때: 카카오는 표시 제한 조항을 찾지 못했다. 다만 결과를 저장하지 않고 실시간 호출로 받은 값을 보여주는 전제다(D1). 네이버는 받은 즉시 1회만 쓸 수 있다. Google은 지도 없이 후보 목록을 보여줄 때 Google 로고(공간이 좁으면 "Google Maps" 문구)를 붙이고, 지도에 올릴 때는 Google 지도를 써야 한다.
- 상업일 때: 위와 같다. 상업 이용에 대해 추가로 확인할 점은 각 카드의 상업 이용 항목에 있다.

### 장소 결과 매핑 (M2a 입력, 확정 아님)

| 장소 결과 필드 | 카카오 로컬 | 네이버 NCP Geocoding | Google Places / Geocoding | 확신도 |
|---|---|---|---|---|
| `type` (`REGION`/`NEIGHBORHOOD`/`SPOT`) | 계산: 주소 검색 `address_type` + `region_3depth_name` 유무 + 키워드 검색 `category_group_code` (2절 질문 4) | 계산: `addressElements[].types` 중 어디까지 채워졌는지 | 계산: `types`/`primaryType` | 부분 확인 (PM 호출 대기) |
| `name` | 키워드: `place_name` / 주소: `address_name` | 채울 수 없음 — 장소 이름 없음. 주소 요소 `BUILDING_NAME` 이 대신할 수 있음 | Places: `displayName` / Geocoding: `formatted_address` | 필드: 확인됨 |
| `address` | `road_address_name` 또는 `address_name` | `roadAddress` 또는 `jibunAddress` | `formattedAddress` / `formatted_address` (Google은 프로그램으로 분석하지 말라고 적음 [14]) | 확인됨 |
| `lat` | `y` | `y` | `location.latitude` / `geometry.location.lat` | 확인됨 |
| `lng` | `x` | `x` | `location.longitude` / `geometry.location.lng` | 확인됨 |
| 시·도 코드 | 주소 검색 `b_code`·`h_code` 앞부분, 또는 좌표→행정구역 `code`. 몇 자리를 잘라 쓸지는 문서에 없음 | `addressElements[SIDO].code` (법정/행정 구분 미기재) | 채울 수 없음 — 코드 없음 | 부분 확인 |
| 시·군·구 코드 | 같은 방식 | `addressElements[SIGUGUN].code` | 채울 수 없음 — 코드 없음 | 부분 확인 |
| 읍·면·동 코드 (법정동) | `address.b_code`, 좌표→행정구역 `region_type=B` 의 `code` | Reverse Geocoding `legalcode` 의 `code.id` [13] | 채울 수 없음 — 코드 없음 | 카카오·네이버 Reverse: 확인됨 |
| 읍·면·동 코드 (행정동) | `address.h_code`, 좌표→행정구역 `region_type=H` 의 `code` | Reverse Geocoding `admcode` 의 `code.id` [13] | 채울 수 없음 — 코드 없음 | 카카오·네이버 Reverse: 확인됨 |
| 외부 장소 ID | `id` | 없음 | `id` / `place_id` | 확인됨 |

## 5. 데모 입력 판단

날짜와 무관한 영역이라 장소만 판단한다. 모든 행은 문서 기반 예상이므로 실제 확인 전까지 `부분 확인 (PM 호출 대기)` 이다.

| 데모 입력 (12절) | 출처 | 얻을 수 있는 것 | 얻을 수 없는 것 | 근거 | 확신도 |
|---|---|---|---|---|---|
| 1988-09-17 + 서울 | 카카오 로컬 | 예상 유형 `REGION`. 주소 검색에서 `address_type`=`REGION`, `region_1depth_name`="서울"이 오면 그 대표 좌표 1개. 후보 수는 1개 안팎으로 예상. 키워드 검색은 서울역 같은 장소가 섞이므로 지역 판정에 쓰지 않는다 | `REGION` 결과에 `address` 객체가 오는지, 대표 좌표가 어디인지는 문서에 없음 | [1] | 부분 확인 (PM 호출 대기) |
| 1988-09-17 + 서울 | 네이버 NCP Geocoding | 예상 유형 `REGION`(`SIDO` 만 채워짐). 좌표 1개 | 결과가 0건일지 여러 건일지 문서로 알 수 없음 | [6] | 부분 확인 (PM 호출 대기) |
| 1988-09-17 + 서울 | Google Geocoding | 예상 유형 `REGION`(`administrative_area_level_1` 이나 `locality`). 좌표와 `place_id` 1개 | 한국 지역이 어느 타입으로 오는지 문서에 없음 | [14] | 부분 확인 (PM 호출 대기) |
| 1988-09-17 + 서울 | 법정동코드 API | "서울특별시" 시·도 코드. 외부 장소 검색 없이 `REGION` 판정 가능 | 좌표 (대표 좌표는 별도 큐레이션 필요) | [24] | 부분 확인 (PM 호출 대기) |
| 2002-06-22 + 광주 서구 풍암동 | 카카오 로컬 | 예상 유형 `NEIGHBORHOOD`. 주소 검색에서 `region_1depth_name` 광주, `region_2depth_name` 서구, `region_3depth_name` 풍암동, `region_3depth_h_name`, `h_code`, `b_code`, 동 대표 좌표 1개. 후보 수는 1개로 예상 | 대표 좌표가 동의 어느 지점인지 문서에 없음 | [1] | 부분 확인 (PM 호출 대기) |
| 2002-06-22 + 광주 서구 풍암동 | 네이버 NCP Geocoding | 예상 유형 `NEIGHBORHOOD`(`DONGMYUN` 까지 채워짐). 좌표 1개. Reverse Geocoding으로 법정동·행정동 코드 | Geocoding `code` 가 법정동인지 행정동인지 | [6][13] | 부분 확인 (PM 호출 대기) |
| 2002-06-22 + 광주 서구 풍암동 | Google Geocoding | 좌표와 `place_id`. 예상 유형은 `sublocality` 계열이면 `NEIGHBORHOOD` | 행정동·법정동 코드. 한국 동의 타입 매핑 | [14] | 부분 확인 (PM 호출 대기) |
| 2002-06-22 + 광주 서구 풍암동 | 법정동코드 API | 풍암동 법정동코드와 상위 코드 | 좌표, 행정동 코드 | [24] | 부분 확인 (PM 호출 대기) |
| 1995-03-15 + 서울대학교병원 | 카카오 로컬 | 예상 유형 `SPOT`. 키워드 검색 첫 후보의 `place_name`, `category_group_code`=`HP8`(병원), 도로명 주소, 좌표, 장소 ID. 같은 이름이 들어간 부속 시설이나 다른 지점이 함께 나올 수 있어 후보는 여러 개로 예상(최대 45) | 첫 후보가 본원일지는 문서로 알 수 없음 | [1] | 부분 확인 (PM 호출 대기) |
| 1995-03-15 + 서울대학교병원 | 네이버 NCP Geocoding | 건물명으로 걸리면 주소와 좌표 | 장소 이름 검색 근거가 없음. 0건일 가능성이 있고 카테고리는 없음 | [6] | 부분 확인 (PM 호출 대기) |
| 1995-03-15 + 서울대학교병원 | Google Places Text Search | 예상 유형 `SPOT`(`hospital`). 이름, 주소, 좌표, `businessStatus`, `place_id`. 후보는 여러 개로 예상(최대 60) | 행정동·법정동 코드 (좌표로 카카오나 네이버 Reverse를 따로 불러야 함) | [15][16][17] | 부분 확인 (PM 호출 대기) |
| 1995-03-15 + 서울대학교병원 | 법정동코드 API | 해당 없음 — 장소 이름 검색 기능이 없다 | — | [24] | 확인됨 |

## 6. PM이 실행할 확인 명령

공통 규칙:
- 키는 환경변수로만 넣는다. 제안 이름: `KAKAO_REST_API_KEY`, `NCP_MAPS_CLIENT_ID`, `NCP_MAPS_CLIENT_SECRET`, `GOOGLE_MAPS_SERVER_KEY`, `DATA_GO_KR_SERVICE_KEY` (모두 서버 전용이라 `VITE_` 를 붙이지 않는다).
- 응답 원문은 저장소에 커밋하지 않는다. 결과는 "응답에서 확인할 것"의 값만 data-researcher에게 넘겨 준다.
- 쿼리는 데모 장소 3개와, 동명 후보 확인용 4절 예시 "중앙병원"만 쓴다(PM 승인 2026-10-08).

### 카카오 로컬 (K1~K6)

```bash
# K1 — 근거: https://developers.kakao.com/docs/latest/ko/local/dev-guide (주소로 좌표 변환하기)
curl -sS -G "https://dapi.kakao.com/v2/local/search/address.json" \
  -H "Authorization: KakaoAK $KAKAO_REST_API_KEY" \
  --data-urlencode "query=서울" | head -c 3000
# 응답에서 확인할 것: meta.total_count, documents[0].address_type(REGION 기대), documents[0].address 가 있는지,
#   있으면 region_1depth_name="서울"이고 region_2depth_name·region_3depth_name 이 비었는지, x·y 값
```

```bash
# K2 — 근거: https://developers.kakao.com/docs/latest/ko/local/dev-guide (주소로 좌표 변환하기)
curl -sS -G "https://dapi.kakao.com/v2/local/search/address.json" \
  -H "Authorization: KakaoAK $KAKAO_REST_API_KEY" \
  --data-urlencode "query=광주 서구 풍암동" | head -c 3000
# 응답에서 확인할 것: meta.total_count(1 기대), documents[0].address_type, address.region_1depth_name/region_2depth_name/
#   region_3depth_name(풍암동 기대), region_3depth_h_name, h_code·b_code 값과 자릿수, main_address_no 가 비었는지, x·y
```

```bash
# K3 — 근거: https://developers.kakao.com/docs/latest/ko/local/dev-guide (키워드로 장소 검색하기)
curl -sS -G "https://dapi.kakao.com/v2/local/search/keyword.json" \
  -H "Authorization: KakaoAK $KAKAO_REST_API_KEY" \
  --data-urlencode "query=서울대학교병원" | head -c 3000
# 응답에서 확인할 것: documents[0].place_name·category_group_code(HP8 기대)·category_name·road_address_name·x·y·id,
#   meta.total_count, 첫 후보가 종로구의 본원인지
```

```bash
# K4 — 근거: https://developers.kakao.com/docs/latest/ko/local/dev-guide (키워드로 장소 검색하기)
curl -sS -G "https://dapi.kakao.com/v2/local/search/keyword.json" \
  -H "Authorization: KakaoAK $KAKAO_REST_API_KEY" \
  --data-urlencode "query=서울" | head -c 3000
# 응답에서 확인할 것: meta.same_name.region(서울이 지역으로 인식되는지), meta.same_name.keyword, documents 가 장소(역 등)인지
```

```bash
# K5 — 근거: https://developers.kakao.com/docs/latest/ko/local/dev-guide (키워드로 장소 검색하기, size·page·category_group_code)
curl -sS -G "https://dapi.kakao.com/v2/local/search/keyword.json" \
  -H "Authorization: KakaoAK $KAKAO_REST_API_KEY" \
  --data-urlencode "query=중앙병원" \
  --data-urlencode "category_group_code=HP8" \
  --data-urlencode "size=15" | head -c 3000
# 응답에서 확인할 것: meta.total_count, meta.pageable_count(45 이하), meta.is_end, documents 개수(15 기대),
#   후보마다 place_name·address_name 이 서로 다른 지역인지
```

```bash
# K6 — 근거: https://developers.kakao.com/docs/latest/ko/local/dev-guide (좌표로 행정구역정보 변환하기)
# <X>, <Y> 에는 K3 첫 후보의 x, y 값을 넣는다.
curl -sS -G "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json" \
  -H "Authorization: KakaoAK $KAKAO_REST_API_KEY" \
  --data-urlencode "x=<X>" \
  --data-urlencode "y=<Y>" | head -c 3000
# 응답에서 확인할 것: documents 에 region_type H 와 B 가 둘 다 오는지, 각 code 값과 자릿수, region_1depth_name~region_3depth_name
```

### 네이버 NCP Maps (N1~N4)

```bash
# N1 — 근거: https://api.ncloud-docs.com/docs/application-maps-geocoding , 헤더: https://api.ncloud-docs.com/docs/application-maps-overview
curl -sS -G "https://maps.apigw.ntruss.com/map-geocode/v2/geocode" \
  -H "x-ncp-apigw-api-key-id: $NCP_MAPS_CLIENT_ID" \
  -H "x-ncp-apigw-api-key: $NCP_MAPS_CLIENT_SECRET" \
  -H "Accept: application/json" \
  --data-urlencode "query=서울" | head -c 3000
# 응답에서 확인할 것: status, meta.totalCount, addresses[0].addressElements 중 채워진 types(SIDO 만인지), x·y
```

```bash
# N2 — 근거: https://api.ncloud-docs.com/docs/application-maps-geocoding
curl -sS -G "https://maps.apigw.ntruss.com/map-geocode/v2/geocode" \
  -H "x-ncp-apigw-api-key-id: $NCP_MAPS_CLIENT_ID" \
  -H "x-ncp-apigw-api-key: $NCP_MAPS_CLIENT_SECRET" \
  -H "Accept: application/json" \
  --data-urlencode "query=광주 서구 풍암동" | head -c 3000
# 응답에서 확인할 것: meta.totalCount, addressElements 의 SIDO/SIGUGUN/DONGMYUN longName 과 code 값, x·y
```

```bash
# N3 — 근거: https://api.ncloud-docs.com/docs/application-maps-geocoding
curl -sS -G "https://maps.apigw.ntruss.com/map-geocode/v2/geocode" \
  -H "x-ncp-apigw-api-key-id: $NCP_MAPS_CLIENT_ID" \
  -H "x-ncp-apigw-api-key: $NCP_MAPS_CLIENT_SECRET" \
  -H "Accept: application/json" \
  --data-urlencode "query=서울대학교병원" | head -c 3000
# 응답에서 확인할 것: meta.totalCount(0이면 장소 이름 검색 불가 확정), 결과가 있으면 BUILDING_NAME 요소와 주소
```

```bash
# N4 — 근거: https://api.ncloud-docs.com/docs/application-maps-reversegeocoding
# <X>,<Y> 에는 N2 결과의 x, y 값을 넣는다.
curl -sS -G "https://maps.apigw.ntruss.com/map-reversegeocode/v2/gc" \
  -H "x-ncp-apigw-api-key-id: $NCP_MAPS_CLIENT_ID" \
  -H "x-ncp-apigw-api-key: $NCP_MAPS_CLIENT_SECRET" \
  --data-urlencode "coords=<X>,<Y>" \
  --data-urlencode "orders=legalcode,admcode" \
  --data-urlencode "output=json" | head -c 3000
# 응답에서 확인할 것: status.code(0), results[].name(legalcode/admcode), code.id, code.type(L/A/S), region.area0.name(kr), area1~area3.name
```

### Google (G1~G4)

```bash
# G1 — 근거: https://developers.google.com/maps/documentation/geocoding/requests-geocoding
curl -sS -G "https://maps.googleapis.com/maps/api/geocode/json" \
  --data-urlencode "address=서울" \
  --data-urlencode "language=ko" \
  --data-urlencode "components=country:KR" \
  --data-urlencode "key=$GOOGLE_MAPS_SERVER_KEY" | head -c 3000
# 응답에서 확인할 것: status(OK), results 개수, results[0].types, address_components[].types(한국 시·도가 어느 타입인지),
#   geometry.location, place_id
```

```bash
# G2 — 근거: https://developers.google.com/maps/documentation/geocoding/requests-geocoding
curl -sS -G "https://maps.googleapis.com/maps/api/geocode/json" \
  --data-urlencode "address=광주 서구 풍암동" \
  --data-urlencode "language=ko" \
  --data-urlencode "components=country:KR" \
  --data-urlencode "key=$GOOGLE_MAPS_SERVER_KEY" | head -c 3000
# 응답에서 확인할 것: results[0].types, address_components 에서 서구·풍암동이 어느 타입(sublocality_level_n 등)인지,
#   partial_match 유무, geometry.location_type
```

```bash
# G3 — 근거: https://developers.google.com/maps/documentation/places/web-service/text-search
# FieldMask 의 displayName·formattedAddress·location 등은 Pro 등급 과금 대상(월 5,000건 무료).
curl -sS -X POST "https://places.googleapis.com/v1/places:searchText" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: $GOOGLE_MAPS_SERVER_KEY" \
  -H "X-Goog-FieldMask: places.id,places.displayName,places.formattedAddress,places.addressComponents,places.location,places.types,places.primaryType,places.businessStatus" \
  -d '{"textQuery":"서울대학교병원","languageCode":"ko","regionCode":"KR"}' | head -c 3000
# 응답에서 확인할 것: places[0].displayName.text, primaryType(hospital 계열 기대), formattedAddress, location, businessStatus,
#   addressComponents 의 types 와 country 의 shortText(KR)
```

```bash
# G4 — 근거: https://developers.google.com/maps/documentation/places/web-service/text-search (includedType, pageSize)
curl -sS -X POST "https://places.googleapis.com/v1/places:searchText" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: $GOOGLE_MAPS_SERVER_KEY" \
  -H "X-Goog-FieldMask: places.id,places.displayName,places.formattedAddress,places.businessStatus,nextPageToken" \
  -d '{"textQuery":"중앙병원","languageCode":"ko","regionCode":"KR","includedType":"hospital","pageSize":20}' | head -c 3000
# 응답에서 확인할 것: places 개수(20 기대), nextPageToken 유무, CLOSED_PERMANENTLY 인 후보가 섞여 있는지
```

### 공공데이터포털 법정동코드 (D1~D2)

> 2026-10-27: M2a 지역 사전용으로는 아래 R1(전체 목록)을 쓴다. D1~D2는 개별 이름 조회 확인용으로 남긴다.

```bash
# D1 — 근거: https://www.data.go.kr/data/15077871/openapi.do
# 공공데이터포털은 키를 인코딩/디코딩 두 형태로 준다. --data-urlencode 를 쓰므로 디코딩 키를 넣는다.
curl -sS -G "https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList" \
  --data-urlencode "ServiceKey=$DATA_GO_KR_SERVICE_KEY" \
  --data-urlencode "pageNo=1" \
  --data-urlencode "numOfRows=10" \
  --data-urlencode "type=json" \
  --data-urlencode "locatadd_nm=광주광역시 서구 풍암동" | head -c 3000
# 응답에서 확인할 것: region_cd(자릿수), sido_cd, sgg_cd, umd_cd, locatadd_nm, locathigh_cd, adpt_de
```

```bash
# D2 — 근거: https://www.data.go.kr/data/15077871/openapi.do
curl -sS -G "https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList" \
  --data-urlencode "ServiceKey=$DATA_GO_KR_SERVICE_KEY" \
  --data-urlencode "pageNo=1" \
  --data-urlencode "numOfRows=10" \
  --data-urlencode "type=json" \
  --data-urlencode "locatadd_nm=서울특별시" | head -c 3000
# 응답에서 확인할 것: totalCount(서울 하위 법정동 전체 수), 첫 행이 시·도 단위 행인지, umd_cd 가 000 인 행으로 시·도/구를 구별할 수 있는지
```

### 공공데이터포털 법정동코드 전체 목록 받기 (R1) — M2a T0a, PM 실행 (T0b)

목적: 시·도 / 시·군·구 / 일반구 행만 담은 JSON 파일 하나를 **저장소 밖**에 만든다. content-curator가 이 파일로 `data/regions.json` 을 만든다(T1). 이 명령은 D1~D2를 대체하고 응답 모양도 함께 확인한다.

실행 전에: 공공데이터포털에서 이 데이터셋의 활용신청(자동승인)을 마친 서비스 키가 `DATA_GO_KR_SERVICE_KEY` 에 **디코딩된 형태**로 들어 있어야 한다. 키를 화면에 찍거나 파일에 쓰지 않는다. 이 명령은 키를 어디에도 출력하지 않는다.

```bash
# R1 — 근거: https://www.data.go.kr/data/15077871/openapi.do (요청 변수 ServiceKey, pageNo, numOfRows, type; locatadd_nm 은 선택)
# 필요한 것: curl, jq. 요청 수는 약 21번(쪽 크기 1,000 가정, totalCount 로 계산). 개발계정 하루 10,000건 안이다.
set -euo pipefail

UA='TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)'
URL='https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList'
OUT_DIR="$HOME/twtd-private/region-codes"   # 저장소 밖. 저장소 안으로 바꾸지 말 것
ROWS=1000                                   # 요청할 쪽 크기(문서에 최대값이 없어, 서버가 줄이면 아래에서 맞춘다)
: "${DATA_GO_KR_SERVICE_KEY:?DATA_GO_KR_SERVICE_KEY 가 비어 있다}"
mkdir -p "$OUT_DIR/pages"

fetch_page() {   # $1=쪽 번호, $2=저장 파일
  curl -sS --fail --max-time 60 -A "$UA" -G "$URL" \
    --data-urlencode "ServiceKey=$DATA_GO_KR_SERVICE_KEY" \
    --data-urlencode "pageNo=$1" \
    --data-urlencode "numOfRows=$ROWS" \
    --data-urlencode "type=json" \
    -o "$2"
  jq -e . "$2" >/dev/null 2>&1 || { echo "쪽 $1 이 JSON 이 아니다. 앞 200자:" >&2; head -c 200 "$2" >&2; echo >&2; exit 1; }
}
row_count() { jq '[.. | objects | select(has("region_cd"))] | length' "$1"; }

# 1쪽으로 totalCount 를 읽는다 (응답 모양을 몰라도 되게 어느 깊이에 있든 찾는다)
fetch_page 1 "$OUT_DIR/pages/p0001.json"
TOTAL=$(jq -r '[.. | objects | .totalCount? // empty][0] | tonumber' "$OUT_DIR/pages/p0001.json")
GOT=$(row_count "$OUT_DIR/pages/p0001.json")
PER=$ROWS
if [ "$GOT" -lt "$ROWS" ] && [ "$GOT" -lt "$TOTAL" ]; then PER=$GOT; fi   # 서버가 쪽 크기를 줄인 경우
PAGES=$(( (TOTAL + PER - 1) / PER ))
echo "totalCount=$TOTAL, 쪽 크기=$PER, 쪽 수=$PAGES" >&2

# 쪽 크기를 줄여야 했다면 1쪽 이후를 그 크기로 받는다 (1쪽은 이미 PER 개)
ROWS=$PER
for P in $(seq 2 "$PAGES"); do
  sleep 1
  fetch_page "$P" "$(printf '%s/pages/p%04d.json' "$OUT_DIR" "$P")"
done

# 시·도 / 시·군·구 / 일반구 행만 남긴다: 법정동코드 10자리의 뒤 5자리(읍면동 3 + 리 2)가 00000
jq -s '
  [ .[] | .. | objects | select(has("region_cd")) ]            # 모든 쪽의 행
  | map(.region_cd |= tostring)
  | unique_by(.region_cd)                                        # 쪽 경계 중복 제거
' "$OUT_DIR"/pages/p*.json > "$OUT_DIR/all-rows.json"

jq '
  map(select(.region_cd | test("^[0-9]{10}$") and endswith("00000")))
  | map(. + {level: (if .region_cd[2:5] == "000" then "sido" else "sigungu" end)})
  | sort_by(.region_cd)
' "$OUT_DIR/all-rows.json" > "$OUT_DIR/regions-sido-sigungu.json"

# 점검 출력 (값은 문서에 근거가 없으니 사람이 눈으로 본다)
jq -r --argjson total "$TOTAL" '
  "받은 행 수=\(length) (totalCount=\($total))"' "$OUT_DIR/all-rows.json" >&2
jq -r '
  "남긴 행 수=\(length), sido=\(map(select(.level=="sido"))|length), sigungu=\(map(select(.level=="sigungu"))|length)",
  "umd_cd 가 000 이 아닌 행(0이어야 함)=\(map(select((.umd_cd|tostring) as $u | ($u!="000" and $u!="0" and $u!="")))|length)",
  "비고(locat_rm)가 있는 행=\(map(select((.locat_rm // "") != ""))|length)"' "$OUT_DIR/regions-sido-sigungu.json" >&2
echo "결과 파일: $OUT_DIR/regions-sido-sigungu.json" >&2
```

**행을 고르는 조건과 이유**

| 항목 | 내용 | 근거 |
|---|---|---|
| 코드 모양 | `region_cd` 는 10자리 = 시도 2 + 시군구 3 + 읍면동 3 + 리 2. 서울특별시는 `1100000000`, `sido_cd` 는 `11` | 데이터셋 페이지 샘플 응답 [24]. 10자리 고정은 공공데이터포털 행정기관 데이터 명세서에서 본 문장(검색 결과 요약, 원문 링크 미확인) — 확신도 부분 확인. T1에서 파일로 확인 |
| 시·도 | `region_cd` 뒤 5자리가 `00000` 이고 가운데 `sgg_cd`(3~5번째 자리)가 `000` | 위 코드 모양에서 도출. 필드 정의 [24] |
| 시·군·구, 일반구 | 뒤 5자리가 `00000` 이고 `sgg_cd` 가 `000` 이 아님. 일반구가 있는 시(수원시 등)는 시 행과 구 행이 모두 남는다. 시·군·구와 일반구는 `locathigh_cd` 로 구별한다 | 코드 모양에서 도출. `locathigh_cd` 가 상위 코드라는 정의 [24]. 실제로 일반구 행의 `locathigh_cd` 가 시 코드인지는 T1에서 파일로 확인 |
| `umd_cd`, `ri_cd` | 남긴 행은 `umd_cd` 가 `000`, `ri_cd` 가 `00` 이어야 한다. 스크립트는 `region_cd` 로 고르고, `umd_cd` 가 `000` 이 아닌 행 수를 점검 출력한다(0이어야 함). 필드가 0으로 채워진 문자열인지(`"000"`) 숫자인지는 문서에 없어 `region_cd` 를 기준으로 삼았다 | 필드 정의 [24]. 자릿수 표기는 T1에서 파일로 확인 |
| 폐지된 코드 | 응답 필드에 폐지 여부·폐지일이 **없다** [24]. 상세설명이 "현재 제공 중인 법정동 코드"의 API라고 적었으므로 현존 코드만 올 가능성이 있지만, 폐지 행이 오는지는 문서에 없다. 그래서 스크립트는 폐지를 거르지 않고, 비고(`locat_rm`)가 있는 행 수만 점검 출력한다 | 폐지 코드를 거르는 방법: **T1에서 파일로 확인**. 참고로 행정표준코드관리시스템 목록 화면에는 폐지구분(전체/현존/폐지) 검색 조건과 폐지일 열이 있어 [30] 폐지 코드가 시스템에는 존재한다. API가 그 구분을 노출하는지는 미확인 |
| 중복 | 쪽 경계에서 겹칠 수 있어 `region_cd` 로 중복을 제거한다 | 방어 |
| 쪽 수 | 1쪽을 받아 `totalCount` 를 읽고 `ceil(totalCount / 쪽 크기)` 로 계산. `numOfRows` 의 최대값은 문서에 없어 1,000을 요청하고, 서버가 더 적게 주면 그 크기로 다시 맞춘다 | `totalCount`, `numOfRows`, `pageNo` 응답 필드 [24] |
| 응답 모양 | 문서에는 응답이 JSON 일 때의 구조(래퍼 이름)가 없다. jq 가 래퍼에 의존하지 않도록 `region_cd` 를 가진 객체와 `totalCount` 를 깊이에 상관없이 찾는다 | 실제 모양은 R1 결과로 확인 (T1) |

**R1 점검 방법**: 실행 끝에 나오는 세 줄 중 `받은 행 수` 가 `totalCount` 와 같은지, `umd_cd 가 000 이 아닌 행` 이 0인지를 본다. 결과 파일 경로(`$HOME/twtd-private/region-codes/regions-sido-sigungu.json`)만 오케스트레이터에게 알려 주면 된다. 쪽별 원문(`pages/`)과 `all-rows.json` 도 같은 폴더에 남지만 **커밋하지 않는다**. 실패하면 화면에 나온 앞 200자와 메시지(키 제외)를 알려 주면 data-researcher가 명령을 고친다. 에러코드 의미는 [24] 하단 표: 20(키 없음/접근 거부), 22(일일 한도 초과), 23(초당 한도 초과), 30(등록되지 않은 키), 31(기한 만료).

**대안: 키 없이 받을 수 있는 공식 파일** (R1을 대신하거나 R1 결과를 맞춰 볼 때. 아직 쓰지 않는다)

| 파일 | 내용 | 확인된 것 (2026-10-27) | 막힌 것 |
|---|---|---|---|
| 국토교통부_전국 법정동_20260630 [32] | CSV, 20,561행. 열: 법정동코드, 시도명, 시군구명, 읍면동명, 리명, 순번, 생성일자. 현존 코드만 담고(삭제일자 열 없음), 원자료는 행정표준코드관리시스템이며 가공되어 원자료와 다를 수 있다고 적음. 연 1회 갱신 | 로그인 없이 다운로드, 이용허락범위 제한 없음, 공공누리 유형 표시 없음, 수정일 2026-07-29 | 페이지에 직접 다운로드 URL이 없어 이 도구로 받아 보지 못함(PM이 브라우저로 받아야 함). 제공 기관이 행정안전부가 아니라 국토교통부라서 결정 #10 (a)의 출처 문구(행정안전부 법정동코드)와 맞는지 PM 확인 필요 — 아래 PM 결정 필요 |
| 국토교통부_법정동코드_20260929 [31] | CSV, 53,387행. 열: 법정동코드, 법정동명, 폐지여부(현존여부) | 로그인 없이 다운로드, 이용허락범위 제한 없음, 공공누리 유형 표시 없음, 수정일 2026-09-29. 폐지 행을 포함하므로 거르는 열(`폐지여부`)이 있다. 이 열의 값 표기(예: "현존"/"폐지")는 페이지에 없음 | 위와 같음. 시도명·시군구명이 따로 없어 이름 분리는 법정동명으로 해야 함 |
| 행정표준코드관리시스템 법정동 전체자료 [30] | 시스템 화면의 "법정동 코드 전체자료" 버튼 | 목록 화면에 폐지구분 검색, 폐지일 열이 있고 총 20,560건으로 표시됨 | 다운로드가 JavaScript 호출이고 이용 조건 문구가 없으며, 로그인 필요 여부 미확인. 소관 부서 표기가 국토교통부로 데이터셋 페이지의 행정안전부와 다름. 쓰지 않는다 |
| 행정안전부_행정표준코드_전체코드_다운로드(전체) | TXT. 법정동 외 직종·직위 코드 등 전체 | 이용허락범위 제한 없음, 수정일 2025-08-05, 링크 `code.go.kr/stdcodesrch/codeAllDownloadL.do` | 열 구성·로그인·내용 범위 미확인. 쓰지 않는다 |

위 파일들은 R1과 같은 원천 데이터에서 나오지만 **어느 것도 확인됨 근거로 쓰지 않았다**. R1의 API가 막히거나(키 발급 지연 등) 폐지 구분이 필요할 때 PM이 브라우저로 받아 content-curator에게 줄 수 있다는 가능성만 적는다. 그때는 위 PM 결정이 먼저다.

### 키 없이 PM이 브라우저로 볼 것 (도구로 읽지 못한 공식 페이지)

| # | URL | 확인할 것 |
|---|---|---|
| B1 | https://www.ncloud.com/policy/terms/maps | 부칙의 시행일이 2025-03-20(v0.4)과 같은지, 제7조 ⑨~⑫ 문구가 같은지 |
| B2 | https://www.ncloud.com/product/applicationService/maps | Geocoding·Reverse Geocoding 월 무료 건수와 단가 |
| B3 | https://cloud.google.com/maps-platform/terms/maps-service-terms | Geocoding API·Places API 절의 위도·경도 캐시 허용 기간과 조건 |
| B4 | https://cloud.google.com/maps-platform/terms | 캐시 금지, 비Google 지도와 함께 쓰기 금지 조항 번호와 문구 |
| B5 | https://business.juso.go.kr/jst/jstRoadNmAddrApiSearch | 도로명주소 검색 API 엔드포인트, 응답 항목, 이용 조건 |
| B6 | https://developers.naver.com/docs/serviceapi/search/local/local.md | 네이버 검색 API(지역) 결과 수 한도, 좌표 필드와 좌표계, 일일 한도, 이용 약관 |

## 7. 이 영역 추천안

- 1순위: **카카오 로컬 API** (주소 검색 + 키워드 검색 + 좌표→행정구역). 종합 확신도: `부분 확인 (PM 호출 대기)`. 여기에 **법정동코드 API**(`확인됨 (결정 #10 (a), 출처 표시: 행정안전부 법정동코드, 공공데이터포털)`)로 만든 지역 사전(`data/regions.json`)을 함께 쓴다. `REGION` 은 사전으로 먼저 판정하고, `NEIGHBORHOOD` 와 `SPOT` 만 카카오를 부르는 구성이다.
- 대안: **Google Places(New) Text Search + Geocoding** (`부분 확인 (PM 호출 대기)`). 카카오 결과가 데모 장소에서 기대와 다르거나, 지도·로드뷰 제공자로 Google을 고를 때 쓴다. 이 경우에도 행정동·법정동 코드는 카카오나 네이버 Reverse Geocoding, 또는 지역 사전으로 채워야 한다.
- 이유:
  1. 데모 장소 3개 유형(광역·동·장소)을 한 제공자에서 처리할 수 있는 근거가 문서로 확인된 것은 카카오뿐이다. 카카오는 병원 카테고리 코드(`HP8`)와 행정동·법정동 코드를 함께 준다 [1].
  2. 무료 한도가 API마다 하루 100,000건으로 가장 크다 [3]. Google Pro는 월 5,000건이다 [20].
  3. 네이버 NCP Geocoding은 장소 이름 검색 근거가 없고 [6], 결과를 받은 즉시 1회만 쓸 수 있다 [10]. 그래서 단독 1순위가 될 수 없다.
  4. 지역 사전을 쓰면 `REGION` 입력은 외부 호출과 저장 금지 문제 없이 처리된다. 캐시가 어려운 약관 조건에서 쿼터와 지연을 줄이는 가장 확실한 방법이다.
  5. 카카오를 확인됨으로 올리려면 6절 K1~K6 실행과, 10절 D1(캐시 범위) 결정이 필요하다. 법정동코드는 결정 #10 (a)로 확인됨이 되었고(2026-10-27), 지역 사전을 만들려면 R1(또는 PM 결정에 따라 대안 파일)로 전체 목록을 한 번 받아야 한다.

**지도·로드뷰 제공자와 같은 회사로 묶을 때 (T3에서 이어받음)**: 카카오로 묶으면 콘솔·키·쿼터를 한곳에서 관리한다. 카카오맵 SDK는 플랫폼 키(브라우저)를 쓰고 REST 키와 섞어 쓰면 안 된다 [2]. 단점은 무료 쿼터가 "첫 번째로 활성화한 앱"에만 있어 [2][4] 계정 상태에 따라 비용이 생긴다는 점, 그리고 한 회사 약관 변경에 모든 섹션이 함께 영향을 받는다는 점이다. Google로 묶는 것은 사실상 약관이 강제한다. Places·Geocoding 결과를 지도에 올리면 Google 지도를 써야 하기 때문이다 [18][19]. 따라서 장소 검색을 Google로 고르면 항공사진(T2)이나 다른 회사 지도 위에 그 좌표를 표시하는 구성이 막힐 수 있다(10절 D3). 네이버로 묶으면 지도·좌표 변환·법정동/행정동 코드는 한 회사에서 받지만, 장소 이름 검색은 별도 서비스(네이버 검색 API, 미확인)가 필요하다. 지도·로드뷰 쪽 장단점(위성, 로드뷰, 브라우저 키, 비용)은 T3 `satellite-streetview.md` 의 제공자 비교표에서 이어서 판단한다.

## 8. 위험

1. **결과 저장 제한 때문에 캐시 설계가 막힐 수 있다.** 로드맵 M2a의 "약관이 허용하면 캐시"와 ★ PM 직접 구현 후보인 "장소 해석 결과 캐시 엔티티"는 카카오·네이버 약관에서 허용 범위가 좁거나 없다 [5][10]. 매 요청마다 실시간으로 호출해야 하면 지연과 쿼터 사용이 늘어난다. 지역 사전으로 `REGION` 을 먼저 처리해 줄일 수 있다.
2. **섹션별 API 사이에서 좌표를 넘기는 것이 "재사용"으로 해석될 수 있다.** 로드맵은 섹션별 엔드포인트(장소 해석 → 동네 → 세계)를 권장한다. 이때 장소 해석으로 받은 좌표를 브라우저가 들고 있다가 다음 섹션 요청에 다시 보내는 구조가 된다. 네이버 약관의 "받은 즉시 1회" [10] 기준과 맞는지는 해석이 필요하다(D1).
3. **테스트 fixture가 "저장·복제"에 해당할 수 있다.** `CLAUDE.md` 7절 5항은 녹화한 fixture로 테스트하라고 한다. 그런데 실제 응답을 녹화해 저장소에 커밋하는 것은 카카오 운영정책 제5조 30호(복사·복제) [5]나 네이버 약관 제7조 ⑨⑪항 [10]과 충돌할 수 있다(D2).
4. **카카오 무료 쿼터는 계정 상태에 달려 있다.** PM 계정에 이미 카카오맵을 활성화한 앱이 있으면 새 앱은 처음부터 유료다 [2][4]. 2026-07-21 이전에 활성화한 앱은 "추후 별도의 안내가 있기 전 까지" 기존 쿼터를 유지한다고 했으므로 [4], 앞으로 정책이 바뀔 수 있다.
5. **실제 정확도를 모른다.** 이 영역의 모든 후보는 키가 필요해, 데모 장소의 첫 후보가 맞는지 문서로는 알 수 없다. 특히 "서울"이 카카오 주소 검색에서 `REGION` 으로 오는지, 첫 후보가 서울대학교병원 본원인지가 분류 규칙의 전제다.
6. **폐업·개명 병원은 카카오에서 찾지 못할 가능성이 높다.** 카카오는 영업 상태 필드가 없다 [1]. 제품 정의서 4절의 "동 단위로 다시 입력" 안내가 주 경로가 된다. Google은 `CLOSED_PERMANENTLY` 를 주지만 [16], 폐업한 장소가 검색에 남아 있는지는 확인하지 못했다.
7. **공식 문서를 일부 읽지 못했다.** 네이버 Maps 약관 페이지와 요금 페이지, Google 서비스 약관 본문, 도로명주소 가이드, 네이버 검색 API 문서는 도구로 읽지 못했다(6절 B1~B6).
8. **문서가 자주 바뀐다.** 카카오 쿼터와 요금은 "추후 변경될 수 있습니다"라고 적혀 있고 [3], 네이버는 2025년에 상품을 개편했으며 [12], Google 정책·요금 페이지는 2026-10-07에 갱신됐다 [18][20]. 구현 직전에 다시 확인해야 한다.
9. **법정동코드 R1은 한 번에 맞지 않을 수 있다.** (2026-10-27) JSON 응답 구조, 쪽 크기 상한, 폐지 행 포함 여부, `region_cd` 자릿수 표기를 문서로 확인하지 못했다. 스크립트는 구조에 의존하지 않게 썼고 합성 응답 두 가지 모양으로 jq 동작만 확인했다(실제 서버 호출 없음). 맞지 않으면 T0a를 되풀이한다. 또 법정동은 행정구역 개편으로 바뀌므로(이름·코드 변경이 있는 시·도가 있는지는 미확인) 받은 날짜를 사전 파일에 기록한다.

## 9. 제품 정의서 가정과 다른 점

| 절 | 기존 가정 | 리서치 결과 |
|---|---|---|
| 7절 | 네이버 지도(NCP) Geocoding이 장소 검색·좌표 후보다 | NCP Geocoding은 주소 검색 전용이고, 장소 이름("서울대학교병원")을 검색한다는 근거가 문서에 없다 [6]. 네이버로 장소를 검색하려면 별도의 네이버 검색 API(지역)가 필요하다(미확인) |
| 8절 | 약관이 허용하는 범위에서 외부 응답을 캐시해 쿼터를 아낀다 | 장소 검색 영역에서는 그 "허용 범위"가 매우 좁다. 네이버는 저장·DB화·재사용을 금지한다 [10]. 카카오는 조건부 캐시만 허용하고 범위가 애매하다 [5]. Google은 `place_id` 만 무기한 저장할 수 있다 [18][19]. 그래서 캐시 대신 지역 사전(법정동코드)과 실시간 호출로 설계해야 할 가능성이 높다. 로드맵 M2a 범위와 ★ 후보("장소 해석 결과 캐시 엔티티")에도 영향이 있다 |
| 4절 | 문을 닫았거나 이름이 바뀐 병원은 찾지 못하므로 동 단위로 다시 입력하게 한다 | 가정 유지. 카카오는 영업 상태 필드가 없다 [1]. Google은 `CLOSED_PERMANENTLY` 와 `movedPlaceId` 를 주므로 [16], 대안 출처로 폐업 병원을 찾을 여지는 있다(실제 동작은 미확인) |

## 10. PM 결정 필요

아래 결정은 다음 M0 작업(T2~T6)을 막지 않는다. T6 `summary.md` 6절에 모아 한 번에 결정한다.

**D1. 장소 검색 결과의 캐시·저장 범위** (관련: 13절 "지도·로드뷰 제공자", 로드맵 M2a)
- 근거 조항: 카카오 운영정책 제5조 20호(사용자 환경 개선 목적의 캐시만, 최신 상태 유지)와 30호(사전 승낙 없는 복사·복제 금지) [5]. 네이버 Maps 서비스 이용약관 제7조 ⑨·⑪항 [10]. 참고로 카카오 DevTalk 답변들은 공식 조항보다 엄격하게 "실시간 호출만"을 안내하는데(단서 [S1]~[S3], 근거 아님), 같은 스레드에서 장소 ID만 기록하는 방향은 허용 가능하다고 답한 사례도 있다.
- 함께 볼 질문: 섹션별 API 사이에서 브라우저가 좌표를 들고 있다가 다시 보내는 구조가 "재사용"에 해당하는가.
- 선택지:
  - (a) 결과를 서버에 저장하지 않는다. 한 사용자 요청(이야기 1회) 안에서만 쓰고, 다음 섹션 요청 때는 필요하면 다시 호출한다. `REGION` 은 지역 사전으로 처리한다.
  - (b) (a)에 더해 카카오 장소 ID만 저장한다(이름·주소·좌표는 매번 실시간으로 다시 호출).
  - (c) 카카오에 앱 계정으로 공식 문의해 서면 답을 받은 뒤 정한다.
- 추천안: MVP는 (a)로 설계하고, (c)를 병행한다. (b)는 공식 답을 받은 뒤에만 쓴다.
- 이 판단은 법률 자문이 아니다.

**D2. 테스트 fixture에 실제 API 응답을 녹화해 커밋할 수 있는가** (관련: `CLAUDE.md` 7절 5항, M2a)
- 근거 조항: 카카오 운영정책 제5조 30호 [5], 네이버 Maps 서비스 이용약관 제7조 ⑨·⑪항 [10].
- 선택지:
  - (a) 공식 문서의 필드 구조를 따라 손으로 만든 합성 fixture를 쓴다. 실제 응답 값은 넣지 않는다.
  - (b) 실제 응답을 녹화해 커밋한다.
  - (c) 실제 응답을 녹화하되 저장소 밖(로컬, 커밋 금지)에만 두고, 커밋하는 fixture는 (a)로 만든다.
- 추천안: (a). 실제 응답의 필드 확인은 6절 명령으로 하고, 그 결과는 값이 아니라 필드 구조만 문서에 반영한다.
- 이 판단은 법률 자문이 아니다.

**D3. Google 장소 결과의 좌표를 다른 회사의 지도·항공사진 위에 표시해도 되는가** (관련: 13절 "지도·로드뷰 제공자", T3)
- 근거: Geocoding·Places 정책 페이지의 "results displayed on a map must be shown on a Google Map" 문장 [18][19]. 이 서비스는 그 좌표를 항공사진(T2)이나 다른 제공자의 지도에 마커로 올릴 수 있다. 서비스 약관 본문은 열지 못했다(B3, B4).
- 선택지:
  - (a) 장소 검색을 Google로 고르지 않는다(카카오 1순위 유지).
  - (b) Google을 고르면 지도 표시는 모두 Google 지도로 한다.
  - (c) Google 좌표는 다른 API 호출의 입력으로만 쓰고 지도에는 표시하지 않는다(해석 필요).
- 추천안: (a). Google은 대안으로만 두고, T3 제공자 비교표에서 다시 판단한다.
- 이 판단은 법률 자문이 아니다.

**D4. 법정동코드 API 데이터를 `data/regions.json` 의 원천으로 써도 되는가** (관련: 13절 "상업적 이용 여부", M2a)
- 근거: 데이터셋 페이지에 "이용허락범위 제한 없음"만 있고 공공누리 유형이 없다 [24]. 포털 이용정책은 유형이 없는 경우를 정의하지 않는다 [25].
- 선택지:
  - (a) 비상업 MVP에서는 출처 표시("행정안전부 법정동코드, 공공데이터포털")를 붙여 쓴다.
  - (b) 행정안전부에 이용 조건을 문의한 뒤 쓴다.
  - (c) 다른 원천을 찾는다(예: 도로명주소 계열 자료, 현재 미확인).
- 추천안: (a)로 진행하고, 상업화를 결정하면 (b)를 한다.
- **결정됨 (2026-10-27): (a)** — 결정 #10. 이 카드는 `확인됨 (결정 #10 (a), 출처 표시: 행정안전부 법정동코드, 공공데이터포털)` 로 올렸다. 남은 질문은 D6.
- 이 판단은 법률 자문이 아니다.

**D5. 대한민국 밖 장소 판정 방법** (관련: 제품 정의서 4절, M2a)
- 근거: 카카오 응답에는 국가 필드가 없고 커버리지 국가도 적혀 있지 않다 [1][2].
- 선택지:
  - (a) "결과 0건 또는 좌표가 한국 범위 밖이면 국외 안내"로 설계한다(추가 확인 없음).
  - (b) PM 확인 명령에 국외 지명 1개를 추가하도록 허용해, 카카오가 국외 지명에 어떻게 응답하는지 본다.
- 추천안: (a)로 M2a를 설계하고, M2a 계획 때 (b)가 필요한지 다시 본다.

**D6. (신규, M2a T0a) 키 없이 받는 국토교통부 법정동 파일을 R1 대신 또는 함께 써도 되는가** (관련: 결정 #10 (a))
- 근거: 결정 #10 (a)는 "행정안전부 법정동코드, 공공데이터포털" 데이터에 대한 것이다. 같은 원천(행정표준코드관리시스템)을 가공한 국토교통부 CSV 두 개가 공공데이터포털에 있고, 둘 다 로그인 없이 받을 수 있으며 이용허락범위가 "제한 없음"이다 [31][32]. 제공 기관이 달라 출처 문구도 달라진다("국토교통부 전국 법정동, 공공데이터포털"). 가공되어 원자료와 다를 수 있다는 주의 문구가 있다 [32]. 이 해석은 법률 자문이 아니다.
- 선택지:
  - (a) R1(행정안전부 API)만 쓴다. 키 발급이 지연되거나 R1이 막히면 그때 D6을 다시 묻는다.
  - (b) 결정 #10 (a)를 국토교통부 두 파일에도 적용한다. 출처 문구는 받은 파일의 기관 이름으로 쓴다.
  - (c) 국토교통부 파일로 R1 결과를 교차 점검하는 데에만 쓰고 사전에는 R1 결과만 쓴다.
- 추천안: (a). 키를 발급받는 것이 부담이면(b). (c)는 폐지 코드 처리를 맞춰 볼 때 유용하다.
- **결정됨 (PM, 2026-10-27): (a)** — R1(행정안전부 API)만 쓴다. 국토교통부 파일은 쓰지 않는다.

## 11. 근거 링크

| # | URL | 문서 제목 | 확인 일자 |
|---|---|---|---|
| 1 | https://developers.kakao.com/docs/latest/ko/local/dev-guide | 카카오 Developers — 로컬 REST API 개발 가이드 (`/docs/ko/local/dev-guide` 로 리다이렉트) | 2026-10-08 |
| 2 | https://developers.kakao.com/docs/latest/ko/local/common | 카카오 Developers — 로컬(카카오맵) 이해하기: 이용 정책, 무료 쿼터, 429 | 2026-10-08 |
| 3 | https://developers.kakao.com/docs/latest/ko/getting-started/quota | 카카오 Developers — 쿼터, 추가 쿼터 사용 요금 | 2026-10-08 |
| 4 | https://devtalk.kakao.com/t/api/150222 | 카카오 DevTalk 공지(kakao 계정, 공지 카테고리, 2026-06-16 게시) — 카카오맵 API 이용 방식 변경, 2026-07-21 시행 | 2026-10-08 |
| 5 | https://developers.kakao.com/terms/latest/ko/site-policies | 카카오 Developers 운영 정책 (2026-04-20 시행), 제5조·제6조·제18조 | 2026-10-08 |
| 6 | https://api.ncloud-docs.com/docs/application-maps-geocoding | 네이버클라우드 API 가이드 — Geocoding (2026-09-17 갱신) | 2026-10-08 |
| 7 | https://api.ncloud-docs.com/docs/application-maps-overview | 네이버클라우드 API 가이드 — Maps 개요: 공통 헤더, 오류 코드 | 2026-10-08 |
| 8 | https://guide.ncloud-docs.com/docs/maps-overview | 네이버클라우드 사용 가이드 — Maps 개요: 대표 계정 무료 이용량 | 2026-10-09 |
| 9 | https://guide.ncloud-docs.com/docs/application-maps-procedure | 네이버클라우드 사용 가이드 — Maps 시나리오: 이용 신청, 약관 확인 | 2026-10-09 |
| 10 | https://xv-ncloud.pstatic.net/images/provision/%5B%EB%AF%BC%EA%B0%84%5DMaps%EC%84%9C%EB%B9%84%EC%8A%A4%EC%9D%B4%EC%9A%A9%EC%95%BD%EA%B4%80_v0.4_(CLEAN)_1742433558704.pdf | 네이버 클라우드 플랫폼 Maps 서비스 이용약관 v0.4 PDF (부칙: 2025-03-20 적용), 네이버 정적 파일 서버 | 2026-10-09 |
| 11 | https://www.ncloud.com/policy/terms/maps | 네이버클라우드 — Maps 서비스 이용약관 페이지 (본문이 자바스크립트로 그려져 읽지 못함) | 2026-10-09 |
| 12 | https://www.ncloud.com/support/notice/all/1965 | 네이버클라우드 공지 — Maps 상품 신규 출시 및 AI NAVER API 지도 API 무료 이용량 제공 종료 안내 (2025-05-28 등록) | 2026-10-09 |
| 13 | https://api.ncloud-docs.com/docs/application-maps-reversegeocoding | 네이버클라우드 API 가이드 — Reverse Geocoding (2026-09-17 갱신) | 2026-10-09 |
| 14 | https://developers.google.com/maps/documentation/geocoding/requests-geocoding | Google — Geocoding request and response | 2026-10-09 |
| 15 | https://developers.google.com/maps/documentation/places/web-service/text-search | Google — Text Search (New) | 2026-10-09 |
| 16 | https://developers.google.com/maps/documentation/places/web-service/reference/rest/v1/places | Google — Places API REST reference: Place, BusinessStatus | 2026-10-09 |
| 17 | https://developers.google.com/maps/documentation/places/web-service/place-types | Google — Place types (Table A/B), 2026-10-07 갱신 | 2026-10-09 |
| 18 | https://developers.google.com/maps/documentation/geocoding/policies | Google — Policies and attributions for Geocoding API, 2026-10-07 갱신 | 2026-10-09 |
| 19 | https://developers.google.com/maps/documentation/places/web-service/policies | Google — Policies and attributions for Places API, 2026-10-07 갱신 | 2026-10-09 |
| 20 | https://developers.google.com/maps/billing-and-pricing/pricing | Google Maps Platform — Pricing, 2026-10-07 갱신 | 2026-10-09 |
| 21 | https://developers.google.com/maps/coverage | Google Maps Platform — Coverage details (South Korea 행), 2026-10-07 갱신 | 2026-10-09 |
| 22 | https://cloud.google.com/maps-platform/terms/maps-service-terms | Google Maps Platform Service Specific Terms (본문을 읽지 못함) | 2026-10-09 |
| 23 | https://cloud.google.com/maps-platform/terms | Google Maps Platform Terms of Service (본문을 읽지 못함) | 2026-10-09 |
| 24 | https://www.data.go.kr/data/15077871/openapi.do | 공공데이터포털 — 행정안전부_행정표준코드_법정동코드 (수정일 2025-11-07) | 2026-10-09 |
| 25 | https://www.data.go.kr/ugs/selectPortalPolicyView.do | 공공데이터포털 — 이용정책 (공공누리 유형별 조건) | 2026-10-09 |
| 26 | https://business.juso.go.kr/jst/jstRoadNmAddrApiSearch | 주소기반산업지원서비스 — 주소정보 API 연계 (제목만 읽힘) | 2026-10-09 |
| 27 | https://juso.go.kr/addrlink/openApi/apiReqst.do | 도로명주소 — API 신청하기 (HTTP 404) | 2026-10-09 |
| 28 | https://developers.naver.com/docs/serviceapi/search/local/local.md | 네이버 Developers — 검색 API 지역 (도구에서 접근 불가) | 2026-10-09 |
| 29 | https://www.ncloud.com/product/applicationService/maps | 네이버클라우드 — Maps 상품 페이지 (요금 표가 자바스크립트로 그려져 읽지 못함) | 2026-10-09 |
| 30 | https://www.code.go.kr/stdcode/regCodeL.do | 행정표준코드관리시스템 — 법정동코드목록조회 (전체자료 다운로드 버튼은 JavaScript 호출, 이용 조건 문구 없음, 소관 부서 표기: 국토교통부) | 2026-10-27 |
| 31 | https://www.data.go.kr/data/15123287/fileData.do | 공공데이터포털 — 국토교통부_법정동코드_20260929 (CSV, 53,387행, 폐지여부 열, 로그인 없이 다운로드, 이용허락범위 제한 없음) | 2026-10-27 |
| 32 | https://www.data.go.kr/data/15063424/fileData.do | 공공데이터포털 — 국토교통부_전국 법정동_20260630 (CSV, 20,561행, 현존 코드만, 로그인 없이 다운로드, 이용허락범위 제한 없음) | 2026-10-27 |
| 33 | https://www.data.go.kr/biz/dcat/metadata/15077871.do | 공공데이터포털 — [24]의 DCAT 메타데이터 (수정일 2025-11-07, 별도 라이선스 필드 없음, 권리 문구만 "이용허락범위 제한 없음") | 2026-10-27 |

단서 (2차 출처로 취급, 근거 열에 쓰지 않음). 카카오 도메인의 개발자 포럼 글이지만, 공지가 아니고 답변자가 공식 직원인지 페이지에서 확인되지 않는다.

| # | URL | 내용 | 확인 일자 |
|---|---|---|---|
| S1 | https://devtalk.kakao.com/t/faq-api/125610 | DevTalk "[FAQ] 카카오맵 API (지도/로컬) 문의 전 꼭 읽어 주세요" (작성자 C.L, 2022-10-18). 로컬 API 응답 저장 불허, 상업 서비스 사용은 제한하지 않음 | 2026-10-09 |
| S2 | https://devtalk.kakao.com/t/local-api-sdk/150397 | DevTalk 질의응답 (답변자 "Map", 2026-06-29). 실시간 호출이면 타사 지도 SDK와 함께 써도 약관 이슈 없음, 저장 목적 호출은 불가 | 2026-10-09 |
| S3 | https://devtalk.kakao.com/t/api-20/151271 | DevTalk 질의응답 (답변자 "Map", 2026-08-24~26). 20분 임시 저장도 불허, 장소 ID만 기록하고 나머지는 실시간 호출하는 방향은 허용 가능 | 2026-10-09 |
