# Wikidata Query Service fixture (M3a2 T3)

## 녹화 fixture (이 폴더의 `sparql-*.json`)

- 출처: Wikidata Query Service (`https://query.wikidata.org/sparql`) SPARQL 응답(`format=json`)을 그대로 저장했다(바이트 수정 없음).
- 녹화 일자: 2026-10-13 (18:50 UTC 무렵), backend-dev가 한 번씩 받았다. User-Agent는 공개값 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)`(저장소 URL만, 개인 연락처 없음)를 썼다.
- 라이선스: Wikidata 구조화 데이터, [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) ([Wikidata:Licensing](https://www.wikidata.org/wiki/Wikidata:Licensing)). 출처 표시 의무는 없다.
- 걸린 시간(계획서 리스크 3, `P279*` 질의): `sparql-1988-09-17.json` 0.455초, `sparql-2002-06-22.json` 0.156초(같은 질의를 처음 보낸 시도는 0.370초). 읽기 타임아웃 `wikimedia.wikidata-read-timeout` 5초보다 훨씬 짧다.
- 질의는 서버 코드(`SparqlWikidataSignalSource.query`)가 만드는 문자열과 글자 하나까지 같다(`SparqlWikidataSignalSourceTest.readmeQueryAndUrlAreWhatTheCodeSends` 가 확인). 결과: 세 1988 항목과 Q47735 모두 비극 분류·사망자 수 없음, 국가(P17)에 대한민국(Q884). Q47735는 일본(Q17)도 있다.

| 파일 | 데모 날짜 | 질의한 항목 | 원 URL |
|---|---|---|---|
| `sparql-1988-09-17.json` | 1988-09-17 | Q8470 Q777278 Q624509 | `https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3FtragicClass%20%3Fdeaths%20%3Fcountry%20WHERE%20%7B%20VALUES%20%3Fitem%20%7B%20wd%3AQ8470%20wd%3AQ777278%20wd%3AQ624509%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP31%2Fwdt%3AP279%2A%20%3FtragicClass%20.%20VALUES%20%3FtragicClass%20%7B%20wd%3AQ3839081%20wd%3AQ171558%20wd%3AQ2223653%20wd%3AQ350604%20%7D%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP1120%20%3Fdeaths%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP17%20%3Fcountry%20%7D%20%7D` |
| `sparql-2002-06-22.json` | 2002-06-22 | Q47735 | `https://query.wikidata.org/sparql?format=json&query=SELECT%20%3Fitem%20%3FtragicClass%20%3Fdeaths%20%3Fcountry%20WHERE%20%7B%20VALUES%20%3Fitem%20%7B%20wd%3AQ47735%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP31%2Fwdt%3AP279%2A%20%3FtragicClass%20.%20VALUES%20%3FtragicClass%20%7B%20wd%3AQ3839081%20wd%3AQ171558%20wd%3AQ2223653%20wd%3AQ350604%20%7D%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP1120%20%3Fdeaths%20%7D%20OPTIONAL%20%7B%20%3Fitem%20wdt%3AP17%20%3Fcountry%20%7D%20%7D` |

질의 원문 (1988-09-17):

```sparql
SELECT ?item ?tragicClass ?deaths ?country WHERE { VALUES ?item { wd:Q8470 wd:Q777278 wd:Q624509 } OPTIONAL { ?item wdt:P31/wdt:P279* ?tragicClass . VALUES ?tragicClass { wd:Q3839081 wd:Q171558 wd:Q2223653 wd:Q350604 } } OPTIONAL { ?item wdt:P1120 ?deaths } OPTIONAL { ?item wdt:P17 ?country } }
```

## 합성 fixture (`synthetic/`)

손으로 쓴 응답이다. 항목은 가짜 QID(`Q1`~`Q7`, `Q99`)뿐이고 실제 사건을 담지 않는다. 필드 구조는 위 녹화 응답(SPARQL 1.1 Query Results JSON)과 같다.

| 파일 | 확인하는 것 |
|---|---|
| `synthetic/synthetic-signals.json` | Q1 비극 분류, Q2 사망자 3, Q3 사망자 0(신호 아님), Q4 국가 Q884와 Q17, Q5 국가 Q17만, Q6 신호 없음, Q7 읽을 수 없는 사망자 수(신호 아님), Q99 묻지 않은 항목(무시) |

## 녹화 fixture — 영상 연결 질의 (`video-links-*.json`, M3a4b T2)

- 출처: Wikidata Query Service SPARQL 응답(`format=json`). Commons 영상 어댑터의 연결 질의(`SparqlWikidataVideoLinks.query`)를 날짜마다 1회 보냈고, 받은 바이트를 그대로 저장했다(수정 없음).
- 녹화 일자: 2026-10-24 (21:14 UTC 무렵), backend-dev가 `curl` 로 한 번씩 받았다. User-Agent는 `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)`(저장소 URL만, 개인 연락처 없음).
- 라이선스: Wikidata 구조화 데이터, [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). 출처 표시 의무는 없다.
- 질의는 서버 코드가 만드는 문자열과 글자 하나까지 같다(`SparqlWikidataVideoLinksTest` 가 아래 원 URL로 확인). 항목은 데모 날짜의 대표 링크 QID(기존 `../kowiki/pageprops-*.json` 녹화 값)와 표본 날짜의 공개 사건 QID다.

| 파일 | 날짜 | 질의한 항목 | 결과 (`P10` / `P373` / `P910` 사이트링크 / 영어 라벨) | 원 URL |
|---|---|---|---|---|
| `video-links-1969-07-20.json` | 1969-07-20 | Q43653 | `AP11 FINAL APPROACH.ogv` / `Apollo 11` / `Category:Apollo 11` / `Apollo 11` | `https://query.wikidata.org/sparql?format=json&query=SELECT+%3fitem+%3fvideo+%3fp373+%3fcs+%3flabel+WHERE+%7b+VALUES+%3fitem+%7b+wd%3aQ43653+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP10+%3fvideo+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP373+%3fp373+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP910+%3fcat+.+%3fpage+schema%3aabout+%3fcat+%3b+schema%3aisPartOf+%3chttps%3a%2f%2fcommons.wikimedia.org%2f%3e+%3b+schema%3aname+%3fcs+%7d+OPTIONAL+%7b+%3fitem+rdfs%3alabel+%3flabel+.+FILTER%28LANG%28%3flabel%29+%3d+%22en%22%29+%7d+%7d` |
| `video-links-1988-09-17.json` | 1988-09-17 | Q8470 Q777278 Q624509 | Q8470: 없음 / `1988 Summer Olympics` / `Category:1988 Summer Olympics` / `1988 Summer Olympics`. Q777278·Q624509: 라벨(`KBS 1TV`·`KBS 2TV`)만 | `https://query.wikidata.org/sparql?format=json&query=SELECT+%3fitem+%3fvideo+%3fp373+%3fcs+%3flabel+WHERE+%7b+VALUES+%3fitem+%7b+wd%3aQ8470+wd%3aQ777278+wd%3aQ624509+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP10+%3fvideo+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP373+%3fp373+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP910+%3fcat+.+%3fpage+schema%3aabout+%3fcat+%3b+schema%3aisPartOf+%3chttps%3a%2f%2fcommons.wikimedia.org%2f%3e+%3b+schema%3aname+%3fcs+%7d+OPTIONAL+%7b+%3fitem+rdfs%3alabel+%3flabel+.+FILTER%28LANG%28%3flabel%29+%3d+%22en%22%29+%7d+%7d` |
| `video-links-2002-06-22.json` | 2002-06-22 | Q47735 | 없음 / `2002 FIFA World Cup` / `Category:2002 FIFA World Cup` / `2002 FIFA World Cup` | `https://query.wikidata.org/sparql?format=json&query=SELECT+%3fitem+%3fvideo+%3fp373+%3fcs+%3flabel+WHERE+%7b+VALUES+%3fitem+%7b+wd%3aQ47735+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP10+%3fvideo+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP373+%3fp373+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP910+%3fcat+.+%3fpage+schema%3aabout+%3fcat+%3b+schema%3aisPartOf+%3chttps%3a%2f%2fcommons.wikimedia.org%2f%3e+%3b+schema%3aname+%3fcs+%7d+OPTIONAL+%7b+%3fitem+rdfs%3alabel+%3flabel+.+FILTER%28LANG%28%3flabel%29+%3d+%22en%22%29+%7d+%7d` |
| `video-links-1986-09-20.json` | 1986-09-20 | Q495611 | 없음 / **없음** / `Category:1986 Asian Games` / `1986 Asian Games` (`P373` 없이 `P910` 사이트링크만 — 녹화로 그 경우를 본다) | `https://query.wikidata.org/sparql?format=json&query=SELECT+%3fitem+%3fvideo+%3fp373+%3fcs+%3flabel+WHERE+%7b+VALUES+%3fitem+%7b+wd%3aQ495611+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP10+%3fvideo+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP373+%3fp373+%7d+OPTIONAL+%7b+%3fitem+wdt%3aP910+%3fcat+.+%3fpage+schema%3aabout+%3fcat+%3b+schema%3aisPartOf+%3chttps%3a%2f%2fcommons.wikimedia.org%2f%3e+%3b+schema%3aname+%3fcs+%7d+OPTIONAL+%7b+%3fitem+rdfs%3alabel+%3flabel+.+FILTER%28LANG%28%3flabel%29+%3d+%22en%22%29+%7d+%7d` |

걸린 시간: 0.57초, 0.46초, 0.39초, 0.30초. `P10` 값은 `http://commons.wikimedia.org/wiki/Special:FilePath/AP11%20FINAL%20APPROACH.ogv` 처럼 오고, 코드가 퍼센트 부호만 풀어 파일 이름으로 쓴다.
