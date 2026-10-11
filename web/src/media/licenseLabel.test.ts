import { describe, expect, it } from 'vitest'
import {
  koglPhoto,
  sameDayPhoto,
  unlicensedPhoto,
} from '../mocks/fixtures/media/photos'
import { imageEmbed } from '../mocks/fixtures/media/modes'
import { licenseLabel } from './licenseLabel'

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
