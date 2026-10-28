# Synthetic region dictionary (tests only)

`regions.json` is a small hand-written dictionary in the format of `data/places/regions.json`
(plan M2a). It is not the real file and not a copy of the legal-dong code list: it holds only the
regions the server tests need (the demo queries `서울`, `광주`, `서구`, `광주 서구`, plus a city
district, two `고성군` and two `중구`). The first entry is deliberately out of code order and before
its parent, so tests see that the loader sorts by code and resolves parents in any order.
Tests copy it into a temporary `app.data-dir`.
