import { describe, expect, it } from 'vitest'
import {
  koglPhoto,
  sameDayPhoto,
  unlicensedPhoto,
} from '../mocks/fixtures/media/photos'
import { imageEmbed } from '../mocks/fixtures/media/modes'
import { licenseInAttribution, licenseLabel } from './licenseLabel'

describe('licenseLabel', () => {
  it('says the license is unverified when license_status is unknown', () => {
    expect(licenseLabel(unlicensedPhoto)).toBe('라이선스 확인 안 됨')
  })

  it('ignores a license name when license_status is unknown', () => {
    expect(licenseLabel({ ...unlicensedPhoto, license: 'CC BY 4.0' })).toBe(
      '라이선스 확인 안 됨',
    )
  })

  it.each([
    ['free', sameDayPhoto, 'CC BY-SA 4.0'],
    ['free, KOGL Type 1 (2002-06-22)', koglPhoto, 'KOGL Type 1'],
    ['other', imageEmbed, 'provider-terms'],
  ])('shows the license name as given (%s)', (_name, media, expected) => {
    expect(licenseLabel(media)).toBe(expected)
  })

  it('says unverified when a known status comes without a name', () => {
    expect(licenseLabel({ ...sameDayPhoto, license: null })).toBe(
      '라이선스 확인 안 됨',
    )
    expect(licenseLabel({ ...sameDayPhoto, license: '  ' })).toBe(
      '라이선스 확인 안 됨',
    )
  })
})

describe('licenseInAttribution (M3b2b T2 (1), decision (b))', () => {
  const withNames = (license: string | null, attribution: string) => ({
    ...sameDayPhoto,
    license,
    attribution,
  })

  it.each([
    ['as written', 'CC BY-SA 4.0', 'Author · CC BY-SA 4.0 · Wikimedia Commons'],
    ['other case', 'cc by-sa 4.0', 'Author · CC BY-SA 4.0 · Wikimedia Commons'],
    ['other spaces', 'CC  BY-SA\t4.0', 'Author · CC BY-SA   4.0'],
    ['in brackets', 'KOGL Type 1', '공공누리 제1유형 (KOGL Type 1) · Commons'],
    ['the whole text', 'Public domain', 'public domain'],
  ])('is true when the attribution names the license (%s)', (_c, l, a) => {
    expect(licenseInAttribution(withNames(l, a))).toBe(true)
  })

  it.each([
    ['not named', 'CC BY-SA 4.0', 'Example Author · Wikimedia Commons'],
    ['only a longer license', 'CC BY', 'Author · CC BY-SA 4.0'],
    ['only a longer version', 'CC BY 4.0', 'Author · CC BY 4.01'],
  ])('is false when %s', (_c, l, a) => {
    expect(licenseInAttribution(withNames(l, a))).toBe(false)
  })

  it('is always false for "라이선스 확인 안 됨", even if the attribution says it', () => {
    const attribution = 'Author · 라이선스 확인 안 됨 · CC BY 4.0'
    expect(
      licenseInAttribution({
        ...unlicensedPhoto,
        license: 'CC BY 4.0',
        attribution,
      }),
    ).toBe(false)
    expect(licenseInAttribution(withNames(null, attribution))).toBe(false)
    expect(licenseInAttribution(withNames('  ', attribution))).toBe(false)
  })
})
