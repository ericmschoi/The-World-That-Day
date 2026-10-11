import { describe, expect, it } from 'vitest'
import {
  koglPhoto,
  sameDayPhoto,
  undatedPhoto,
  unlicensedPhoto,
} from '../mocks/fixtures/media/photos'
import {
  imageEmbed,
  linkOnly,
  sdkRoadview,
  videoEmbed,
  videoFile,
} from '../mocks/fixtures/media/modes'
import { isDisplayable } from './isDisplayable'
import type { Media } from './types'

const R1_FIELDS = [
  'id',
  'type',
  'display_mode',
  'source_name',
  'source_url',
  'attribution',
  'license_status',
  'retrieved_at',
] as const

const without = (media: Media, patch: Record<string, unknown>) =>
  ({ ...media, ...patch }) as Media

describe('isDisplayable', () => {
  it.each([
    ['same-day photo', sameDayPhoto],
    ['KOGL photo with lat/lng only', koglPhoto],
    ['photo with license null (unknown)', unlicensedPhoto],
    ['current-precision media with no captured_at', sdkRoadview],
  ])('accepts a complete record: %s', (_name, media) => {
    expect(isDisplayable(media)).toBe(true)
  })

  it.each(
    R1_FIELDS.flatMap((field) => [
      [field, ''],
      [field, null],
    ]),
  )('rejects a record whose R1 field %s is %j', (field, value) => {
    expect(isDisplayable(without(sameDayPhoto, { [field]: value }))).toBe(false)
  })

  it('rejects a license_status outside free/other/unknown', () => {
    expect(
      isDisplayable(without(sameDayPhoto, { license_status: 'maybe' })),
    ).toBe(false)
  })

  it('needs a place: lat and lng, or place_ref', () => {
    const noPlace = { lat: null, lng: null, place_ref: null }
    expect(isDisplayable(without(sameDayPhoto, noPlace))).toBe(false)
    expect(
      isDisplayable(without(sameDayPhoto, { ...noPlace, lat: 37.5 })),
    ).toBe(false)
    expect(
      isDisplayable(without(sameDayPhoto, { ...noPlace, place_ref: ' ' })),
    ).toBe(false)
  })

  it.each(['url', 'thumbnail_url'])('rejects an image without %s', (field) => {
    expect(isDisplayable(without(sameDayPhoto, { [field]: null }))).toBe(false)
    expect(isDisplayable(without(sameDayPhoto, { [field]: '' }))).toBe(false)
  })

  // CLAUDE.md 7.1 and product brief 6.2: no capture time, not shown.
  // The M3b1 plan T2 (4) expects `true` here; open PM decision.
  it('rejects a photo whose capture time is unknown', () => {
    expect(isDisplayable(undatedPhoto)).toBe(false)
  })

  it('checks the required fields of the other display modes', () => {
    expect(isDisplayable(imageEmbed)).toBe(true)
    expect(isDisplayable(without(imageEmbed, { provider: null }))).toBe(false)
    expect(isDisplayable(videoEmbed)).toBe(true)
    expect(isDisplayable(without(videoEmbed, { embed_url: '' }))).toBe(false)
    expect(isDisplayable(videoFile)).toBe(true)
    expect(isDisplayable(without(videoFile, { video_sources: [] }))).toBe(false)
    expect(isDisplayable(sdkRoadview)).toBe(true)
    expect(isDisplayable(without(sdkRoadview, { render: null }))).toBe(false)
  })

  it('never shows link_only or unknown modes', () => {
    expect(isDisplayable(linkOnly)).toBe(false)
    expect(
      isDisplayable(without(sameDayPhoto, { display_mode: 'gallery' })),
    ).toBe(false)
  })
})
