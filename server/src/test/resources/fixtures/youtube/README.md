# YouTube Data API v3 fixtures (M3a5 T2)

Synthetic values built from the official field structure; no real responses or real video IDs.

- Nothing here was recorded from Google (CLAUDE.md 7.5, decision #12 (a)). The field names and nesting follow the official reference pages below; every value (video IDs such as `Abc123xyz_0`, the channel ID `UCsyntheticChannelId0001`, titles, descriptions, dates, etags) is made up.
- Written: 2026-10-25, by backend-dev, by hand. No API key was used and no request was sent.
- Used by `YoutubeDataApiClientTest` (WireMock), and from M3a5a T4 (2026-10-26, also by hand) by `YoutubeVideoMapperTest` and `YoutubeCandidateScoreTest` through `YoutubeItems`. Later tasks (T5) may add more synthetic files here the same way.

| File | Shape | Reference |
|---|---|---|
| `search-two-videos.json` | `search.list` answer with two `youtube#searchResult` items (`id.videoId`, `snippet`), in an order that is not sorted | [85] `search.list`, [88] `search` resource |
| `search-empty.json` | `search.list` answer with `items: []` (no result in the channel) | [85], [88] |
| `videos-two-items.json` | `videos.list` answer with `part=snippet,status,contentDetails,recordingDetails`: one public embeddable 117-second item, and one item that breaks the later rules (`embeddable` false, `unlisted`, `blocked: [KR]`, `ytAgeRestricted`, 601 seconds, a `recordingDate`) | [86] `videos.list`, [87] `videos` resource |
| `error-quota-exceeded.json` | 403 body with `error.errors[].reason` = `quotaExceeded` | [91] errors |
| `error-forbidden.json` | 403 body with reason `forbidden` | [91] |
| `error-rate-limit-exceeded.json` | 403 body with reason `rateLimitExceeded` | [91] |
| `videos-1986-09-20.json` | `videos.list` answer for the 1986-09-20 sample date (M3a5a T4): the contract example item `Abc123xyz_0` (title `1986년 …`, channel title `KTV 아카이브`, 117 seconds), two newsreel titles (`대한뉴스 제 1610호-…`) and a retrospective title (`… 오늘 …`) in a second synthetic channel `UCsyntheticChannelId0002`. Tests change single fields of copies for the drop rules | [86], [87] |
| `contract-video-embed-1986-09-20.json` | Not an API answer: the `video_embed` item the server returns for `Abc123xyz_0`, copied from the contract example of `docs/plans/m3a5a-youtube-video-source.md` (keys of the web fixture `exampleYoutubeEmbed`) | [102] embed parameters |

## The error body is an assumption

[91] lists `quotaExceeded` (and `forbidden`) as 403 errors but shows **no JSON body example** (research T0c, 2026-10-25). The `{"error": {"code", "message", "errors": [{"message", "domain", "reason"}]}}` shape used here is the common Google API error format, assumed by the implementation and not documented on that page. The client therefore treats a 403 whose body cannot be read, or that names no `quotaExceeded` reason, as an ordinary failure, not as a spent quota. In [91], `rateLimitExceeded` appears only as a 400 under `videos.reportAbuse`; it is used here as a 403 to show that any other reason is a failure.

## References

- [85] https://developers.google.com/youtube/v3/docs/search/list
- [86] https://developers.google.com/youtube/v3/docs/videos/list
- [87] https://developers.google.com/youtube/v3/docs/videos
- [88] https://developers.google.com/youtube/v3/docs/search
- [91] https://developers.google.com/youtube/v3/docs/errors
- [102] https://developers.google.com/youtube/player_parameters

Numbers are those of `docs/research/video-sources.md`.
