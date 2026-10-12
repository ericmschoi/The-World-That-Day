// T4 extra checks for T2 (docs/plans/m3b1-design-foundation.md) and the PM
// media decision of 2026-10-11: media with an unknown capture time is shown
// as "촬영 시점 미상", media without a source stays hidden, and only http(s)
// URLs reach href/src. Inputs are the demo dates (product brief 12) and the
// leap-day boundary 2000-02-29.
import { describe, expect, it } from 'vitest'
import { videoFile } from '../mocks/fixtures/media/modes'
import {
  koglPhoto,
  sameDayPhoto,
  undatedPhoto,
  unlicensedPhoto,
} from '../mocks/fixtures/media/photos'
import { dateLabel } from './dateLabel'
import { isDisplayable } from './isDisplayable'
import { licenseLabel } from './licenseLabel'
import type { Media } from './types'

const patch = (media: Media, fields: Record<string, unknown>) =>
  ({ ...media, ...fields }) as Media

describe('demo 1988-09-17 (fixtures)', () => {
  it('same-day photo: shown, 같은 날, full capture date', () => {
    expect(isDisplayable(sameDayPhoto)).toBe(true)
    expect(dateLabel(sameDayPhoto)).toEqual({
      badge: '같은 날',
      text: '1988년 9월 17일 촬영',
    })
  })

  it('photo whose capture time could not be read: shown, 촬영 시점 미상', () => {
    expect(isDisplayable(undatedPhoto)).toBe(true)
    expect(dateLabel(undatedPhoto)).toEqual({
      badge: null,
      text: '촬영 시점 미상',
    })
  })
})

describe('demo 2002-06-22 (fixtures)', () => {
  it('KOGL Type 1 photo: shown with the license name', () => {
    expect(isDisplayable(koglPhoto)).toBe(true)
    expect(licenseLabel(koglPhoto)).toBe('KOGL Type 1')
  })
})

describe('unknown capture time and unknown license together', () => {
  const both = patch(unlicensedPhoto, {
    captured_at: null,
    date_precision: null,
    date_relation: { kind: 'unknown', years: 0 },
  })

  it('is still shown, with both honest labels', () => {
    expect(isDisplayable(both)).toBe(true)
    expect(dateLabel(both)).toEqual({ badge: null, text: '촬영 시점 미상' })
    expect(licenseLabel(both)).toBe('라이선스 확인 안 됨')
  })

  it('is hidden once the source is missing', () => {
    expect(isDisplayable(patch(both, { source_url: null }))).toBe(false)
    expect(isDisplayable(patch(both, { source_name: null }))).toBe(false)
  })
})

describe('media without a source stays hidden', () => {
  it.each([
    ['source_name', '   '],
    ['source_url', '   '],
    ['attribution', '\t'],
    ['id', ' '],
    ['retrieved_at', ' '],
  ])('rejects a whitespace-only %s', (field, value) => {
    expect(isDisplayable(patch(sameDayPhoto, { [field]: value }))).toBe(false)
  })

  it.each([undatedPhoto, unlicensedPhoto, koglPhoto])(
    'hides $id when source_url is gone',
    (media) => {
      expect(isDisplayable(patch(media, { source_url: undefined }))).toBe(false)
    },
  )
})

describe('only http(s) URLs pass', () => {
  it.each([
    ['source_url', 'JAVASCRIPT:alert(1)'],
    ['source_url', 'https:commons.example.org/wiki/File:Example.jpg'],
    ['source_url', 'http:/commons.example.org/wiki/File:Example.jpg'],
    ['source_url', 'https://'],
    ['source_url', ' https://commons.example.org/wiki/File:Example.jpg'],
    ['source_url', 'file:///etc/passwd'],
    ['license_url', 'mailto:someone@example.org'],
    ['url', 'blob:https://media.example.org/0001'],
    ['thumbnail_url', 'data:image/png;base64,AAAA'],
    ['thumbnail_url', '/relative/thumb.jpg'],
  ])('rejects %s = %j', (field, value) => {
    expect(isDisplayable(patch(sameDayPhoto, { [field]: value }))).toBe(false)
  })

  it('allows a null license_url (no license link)', () => {
    expect(undatedPhoto.license_url).toBeNull()
    expect(isDisplayable(undatedPhoto)).toBe(true)
  })

  it('rejects a video file when any one source is not http(s)', () => {
    const good = videoFile.video_sources![0]
    const bad = { ...good, url: 'data:video/webm;base64,AAAA' }
    expect(
      isDisplayable(patch(videoFile, { video_sources: [good, bad] })),
    ).toBe(false)
    expect(
      isDisplayable(patch(videoFile, { url: 'javascript:alert(1)' })),
    ).toBe(false)
  })
})

describe('dateLabel boundaries', () => {
  const photo = (
    captured_at: string,
    date_precision: Media['date_precision'],
    date_relation: Media['date_relation'],
  ) => patch(sameDayPhoto, { captured_at, date_precision, date_relation })

  it.each([
    [
      'same day on the leap day (D = 2000-02-29)',
      photo('2000-02-29', 'day', { kind: 'same_day', years: 0 }),
      '같은 날',
      '2000년 2월 29일 촬영',
    ],
    [
      'same year, different month, day precision (D = 1988-09-17)',
      photo('1988-01-01', 'day', { kind: 'same_year', years: 0 }),
      '같은 해',
      '1988년 1월 1일 촬영',
    ],
    [
      'different year, month precision (D = 2002-06-22)',
      photo('2001-12', 'month', { kind: 'years_apart', years: -1 }),
      '1년 전',
      '2001년 12월 촬영',
    ],
    [
      'approximate a year off (D = 1988-09-17)',
      photo('1987', 'approx', { kind: 'approximate', years: -1 }),
      '무렵',
      '1987년 무렵 촬영(추정)',
    ],
  ])('%s', (_name, media, badge, text) => {
    expect(dateLabel(media)).toEqual({ badge, text })
  })

  it('never shows 현재 모습 for current precision with a captured_at', () => {
    // R8: current precision has captured_at null; if a date leaks in, the
    // label still says the capture time was not provided.
    const media = photo('1988-09-17', 'current', { kind: 'current', years: 0 })
    expect(dateLabel(media).text).not.toMatch(/1988/)
  })
})
