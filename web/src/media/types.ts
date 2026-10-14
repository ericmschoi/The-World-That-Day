// Shared media format from docs/plans/m3a3-event-media.md ("공용 미디어 형식").
// Names stay snake_case so API JSON maps one to one. Do not rename fields.

export type MediaType =
  'photo' | 'video' | 'aerial' | 'satellite' | 'streetview'

// How the media is shown, not whether it is shown.
export type DisplayMode =
  'image' | 'image_embed' | 'video_file' | 'video_embed' | 'sdk' | 'link_only'

export type LicenseStatus = 'free' | 'other' | 'unknown'

// `date_precision: null` means the capture time is unknown.
export type DatePrecision = 'day' | 'month' | 'year' | 'approx' | 'current'

export type DateRelationKind =
  | 'same_day'
  | 'same_month'
  | 'same_year'
  | 'years_apart'
  | 'approximate'
  | 'current'
  | 'unknown'

// API only: the server compares `captured_at` with the requested date.
// `years` = capture year - requested year.
export type DateRelation = { kind: DateRelationKind; years: number }

// `height` and `render.zoom` may be null (server boxed types, schema
// `nullablePositiveInteger` and `null | number`).
export type VideoSource = { url: string; mime: string; height: number | null }

export type SdkRender = {
  provider: string
  kind: 'roadview' | 'skyview'
  lat: number
  lng: number
  zoom: number | null
}

// API only, added by M3a4 for quality review (revised M3a4 contract, no
// `provider_embed`). Not shown on screen; the server does not send it yet.
export type MediaSelection = {
  basis:
    | 'commons_p10'
    | 'commons_category'
    | 'commons_date_category'
    | 'commons_search'
    | 'youtube_official'
    | 'daehan_news'
  score: number
}

export type Media = {
  id: string
  type: MediaType
  display_mode: DisplayMode
  url: string | null
  thumbnail_url: string | null
  thumbnail_width: number | null
  thumbnail_height: number | null
  video_sources: VideoSource[] | null
  duration_seconds: number | null
  embed_url: string | null
  provider: string | null
  render: SdkRender | null
  link_only_reason: 'hotlink_blocked' | 'embed_disabled' | null
  source_name: string
  source_url: string
  license: string | null
  license_status: LicenseStatus
  license_url: string | null
  attribution: string
  description: string | null
  captured_at: string | null
  date_precision: DatePrecision | null
  lat: number | null
  lng: number | null
  place_ref: string | null
  retrieved_at: string
  date_relation: DateRelation
  selection?: MediaSelection | null
}
