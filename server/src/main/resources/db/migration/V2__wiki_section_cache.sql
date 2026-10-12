-- Wikitext cache for Korean Wikipedia date and year pages (plan M3a1b, decision M3-P3 (a)).
-- Only the "사건" and "문화" sections are stored, as raw wikitext (CC BY-SA 4.0, revision kept).
-- Nothing about the person asking (IP, requested date, ...) is stored.
CREATE TABLE wiki_section_cache (
    id               BIGSERIAL    PRIMARY KEY,
    site             VARCHAR(64)  NOT NULL,   -- host of the API, e.g. ko.wikipedia.org
    page_title       VARCHAR(255) NOT NULL,   -- e.g. '9월 17일', '1988년'
    revid            BIGINT       NOT NULL,
    events_wikitext  TEXT,                    -- "사건" section; NULL when the page has none
    culture_wikitext TEXT,                    -- "문화" section; NULL when the page has none
    fetched_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_wiki_section_cache UNIQUE (site, page_title)
);
