-- Result cache of event video searches (plan M3a4c). One row per source, date, event and whether
-- the day-wide paths were off (the same day gives other results then). The result is a JSON text of
-- the candidates' metadata (addresses, licence, attribution, short text) and English labels: no
-- file bytes and no posters (safeguard 1). Nothing about the person asking (IP, place, ...) is
-- stored.
CREATE TABLE event_media_search_cache (
    id            BIGSERIAL    PRIMARY KEY,
    source        VARCHAR(32)  NOT NULL,   -- source key, e.g. 'commons'
    event_date    DATE         NOT NULL,
    event_id      VARCHAR(64)  NOT NULL,   -- e.g. 'kowiki-1988-09-17-0123abcd'
    day_paths_off BOOLEAN      NOT NULL,
    result        TEXT         NOT NULL,
    fetched_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_event_media_search_cache UNIQUE (source, event_date, event_id, day_paths_off)
);
