# Place dictionary fixtures (plan C2 T1)

Recorded excerpts of the official SGC 2021 and CGNDB download files, used by `PlaceDictionaryBuilderTest` (package `io.github.ericmschoi.theworldthatday.place`). Both sources allow copying and redistribution, including excerpts (`docs/research/canada-place-search.md`, source cards; `CLAUDE.md` 7.5).

- Downloaded: 2026-11-05, by backend-dev, with the User-Agent `TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)`. The full files stayed outside the repository; only the rows below were copied, byte for byte (encoding, byte order mark and `\r\n` line ends kept). `.gitattributes` marks the CSV files `-text` so Git keeps those bytes.
- Rows kept: only the demo and same-name places of the plan and their parts. CSDs 1001365, 1001519, 1102032, 1103068, 1301006, 2466023, 3521024, 3539036, 4608042, 4806016, 5915022, 5917034, 5955034 with their province (all 13) and division rows; their SGC "Place name(s)" rows whose folded name is one of Calgary, Montréal, Hochelaga-Maisonneuve, Hochelaga, Maisonneuve, Mercier–Hochelaga-Maisonneuve, Vancouver, St. John's, London, Victoria, Saint John, St. John, Fort St. John, Caledon; and every CGNDB row with one of those folded names in the 8 provincial files listed below. Caledon is in because its SGC place names list "Victoria".

| File | Source | Encoding | Rows |
|---|---|---|---|
| `sgc-structure-excerpt.csv` | SGC 2021 classification structure, https://www.statcan.gc.ca/en/statistical-programs/document/sgc-cgt-2021-structure-eng.csv | ISO-8859-1 ("Montréal" is the byte `E9`), `\r\n` | header + 39 |
| `sgc-elements-excerpt.csv` | SGC 2021 elements, https://www.statcan.gc.ca/en/statistical-programs/document/sgc-cgt-2021-element-eng.csv | ISO-8859-1, `\r\n` | header + 18 |
| `cgn-excerpt.csv` | CGNDB English CSV, provincial files `cgn_{nl,pe,nb,qc,on,mb,ab,bc}_csv_eng.zip` of https://ftp.maps.canada.ca/pub/nrcan_rncan/vector/geobase_cgn_toponyme/prov_csv_eng/ (server date 2026-11-04), joined under one header in that file-name order (ab, bc, mb, nb, nl, on, pe, qc) | UTF-8 with byte order mark, `\r\n` | header + 30 |

Not recorded:

- `overrides.csv`: written by hand for the tests in the format of plan C2 (`data/places/overrides.csv`). The real file is the content-curator's (T2).
- `expected-regions.csv`, `expected-neighbourhoods.csv`: the builder's output for the three excerpts plus `overrides.csv`, checked by hand against the plan's demo table (Calgary 4806016, St. John's 1001519, London 3539036, Saint John 1301006, Victoria in 5 CSDs, Hochelaga-Maisonneuve EQVRD in 2466023). The test compares bytes.
- Synthetic rows that tests add to temporary copies use CGNDB IDs starting with `ZZ` and the source text `Synthetic test row`.

## Licences and attribution

- SGC 2021: Statistics Canada Open Licence, https://www.statcan.gc.ca/en/terms-conditions/open-licence. "Source: Statistics Canada, Standard Geographical Classification (SGC) 2021. Reproduced and distributed on an "as is" basis with the permission of Statistics Canada." Dataset record: https://open.canada.ca/data/en/dataset/1d815243-28ce-45e6-ad6b-58643327a253
- CGNDB: Open Government Licence – Canada, https://open.canada.ca/en/open-government-licence-canada. "Contains information licensed under the Open Government Licence – Canada." Dataset record: https://open.canada.ca/data/en/dataset/e27c6eba-3c5d-4051-9db2-082dc6411c2c. Names are kept exactly as published.
