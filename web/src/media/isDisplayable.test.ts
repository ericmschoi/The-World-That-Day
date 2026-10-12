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

  // PM decision 2026-10-11: shown, labeled "촬영 시점 미상" (see dateLabel).
  it('accepts a photo whose capture time is unknown', () => {
    expect(undatedPhoto.date_precision).toBeNull()
    expect(isDisplayable(undatedPhoto)).toBe(true)
    expect(
      isDisplayable(
        without(sameDayPhoto, { captured_at: null, date_precision: null }),
      ),
    ).toBe(true)
  })

  it.each([
    ['source_url', 'javascript:alert(1)'],
    ['source_url', 'data:text/html,hi'],
    ['source_url', '//commons.example.org/wiki/File:Example.jpg'],
    ['source_url', '/wiki/File:Example.jpg'],
    ['url', 'ftp://media.example.org/Example.jpg'],
    ['thumbnail_url', 'javascript:alert(1)'],
    ['thumbnail_url', ' javascript:alert(1)'],
    ['thumbnail_url', 'data:image/png;base64,AAAA'],
    ['embed_url', 'vbscript:msgbox'],
  ])('rejects a non-http(s) %s: %j', (field, value) => {
    expect(isDisplayable(without(sameDayPhoto, { [field]: value }))).toBe(false)
  })

  // M3b1b T1 (4), M3a3 R9: only the license link is dropped (see MediaCaption).
  it.each(['javascript:alert(1)', 'mailto:someone@example.org', '/license'])(
    'keeps a record whose only bad URL is license_url %j',
    (license_url) => {
      expect(isDisplayable(without(sameDayPhoto, { license_url }))).toBe(true)
    },
  )

  it('rejects a video source with a non-http(s) url', () => {
    const bad = { url: 'javascript:alert(1)', mime: 'video/webm', height: 480 }
    expect(isDisplayable(without(videoFile, { video_sources: [bad] }))).toBe(
      false,
    )
  })

  // Malformed API data must hide the record, never throw during render.
  it.each([
    ['a string', 'https://media.example.org/Example.webm'],
    ['an object', { url: 'https://media.example.org/Example.webm' }],
    ['a number', 42],
    ['an array with null', [null]],
    ['an array with undefined', [undefined]],
    ['an array with a number', [42]],
    ['an array with a string', ['https://media.example.org/Example.webm']],
    ['an array with an entry missing url', [{ mime: 'video/webm' }]],
  ])('rejects video_sources that is %s, without throwing', (_name, value) => {
    for (const media of [videoFile, sameDayPhoto]) {
      const malformed = without(media, { video_sources: value })
      expect(() => isDisplayable(malformed)).not.toThrow()
      expect(isDisplayable(malformed)).toBe(false)
    }
  })

  it('rejects a valid video source list mixed with a null entry', () => {
    const good = videoFile.video_sources![0]
    expect(
      isDisplayable(without(videoFile, { video_sources: [good, null] })),
    ).toBe(false)
  })

  it.each([null, undefined, 'media', 42])(
    'returns false for a non-object record %j',
    (value) => {
      expect(() => isDisplayable(value as unknown as Media)).not.toThrow()
      expect(isDisplayable(value as unknown as Media)).toBe(false)
    },
  )

  it('accepts http and https URLs in any letter case', () => {
    expect(
      isDisplayable(
        without(sameDayPhoto, {
          source_url: 'HTTPS://commons.example.org/wiki/File:Example.jpg',
          license_url: 'http://creativecommons.org/licenses/by-sa/4.0/',
        }),
      ),
    ).toBe(true)
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
