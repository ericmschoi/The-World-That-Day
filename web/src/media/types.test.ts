// T2 (1): `Media` has the same names and types as the M3a3 shared media format
// table plus the API-only `date_relation` (docs/plans/m3a3-event-media.md,
// "공용 미디어 형식"). `Expected` is written from the plan, not from types.ts.
// The type assertions run under `npm run typecheck` (tsc -b); the runtime
// checks run under Vitest.
import { describe, expect, it } from 'vitest'
import * as modes from '../mocks/fixtures/media/modes'
import * as photos from '../mocks/fixtures/media/photos'
import type { Media } from './types'

type Equal<A, B> =
  (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2
    ? true
    : false

type Expected = {
  id: string
  type: 'photo' | 'video' | 'aerial' | 'satellite' | 'streetview'
  display_mode:
    'image' | 'image_embed' | 'video_file' | 'video_embed' | 'sdk' | 'link_only'
  url: string | null
  thumbnail_url: string | null
  thumbnail_width: number | null
  thumbnail_height: number | null
  video_sources: { url: string; mime: string; height: number }[] | null
  duration_seconds: number | null
  embed_url: string | null
  provider: string | null
  render: {
    provider: string
    kind: 'roadview' | 'skyview'
    lat: number
    lng: number
    zoom: number
  } | null
  link_only_reason: 'hotlink_blocked' | 'embed_disabled' | null
  source_name: string
  source_url: string
  license: string | null
  license_status: 'free' | 'other' | 'unknown'
  license_url: string | null
  attribution: string
  description: string | null
  captured_at: string | null
  date_precision: 'day' | 'month' | 'year' | 'approx' | 'current' | null
  lat: number | null
  lng: number | null
  place_ref: string | null
  retrieved_at: string
  date_relation: {
    kind:
      | 'same_day'
      | 'same_month'
      | 'same_year'
      | 'years_apart'
      | 'approximate'
      | 'current'
      | 'unknown'
    years: number
  }
  // API-only field from docs/plans/m3a4-event-video.md (not in the M3a3
  // table); optional so M3a3 responses without it still type-check.
  selection?: {
    basis:
      | 'commons_category'
      | 'commons_search'
      | 'youtube_official'
      | 'daehan_news'
      | 'provider_embed'
    score: number
  } | null
}

// Fails `tsc -b` if a name, a type, null-ability or optionality differs.
const sameShape: Equal<Media, Expected> = true

const M3A3_FIELDS = [
  'id',
  'type',
  'display_mode',
  'url',
  'thumbnail_url',
  'thumbnail_width',
  'thumbnail_height',
  'video_sources',
  'duration_seconds',
  'embed_url',
  'provider',
  'render',
  'link_only_reason',
  'source_name',
  'source_url',
  'license',
  'license_status',
  'license_url',
  'attribution',
  'description',
  'captured_at',
  'date_precision',
  'lat',
  'lng',
  'place_ref',
  'retrieved_at',
  'date_relation',
].sort()

describe('Media type (T2 (1))', () => {
  it('matches the M3a3 field table plus date_relation', () => {
    expect(sameShape).toBe(true)
  })

  // Fixtures are typed `Media`; each must carry every field, with null for
  // "no value" rather than a missing key, as API JSON does.
  it.each(Object.entries({ ...photos, ...modes }))(
    'fixture %s has exactly the M3a3 fields',
    (_name, media) => {
      expect(Object.keys(media).sort()).toEqual(M3A3_FIELDS)
    },
  )
})
