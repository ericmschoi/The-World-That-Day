// Plan M2b T3 (1): the place rules. Places are the demo places only.
import { describe, expect, it } from 'vitest'
import { checkPlace } from './placeRules'

describe('checkPlace', () => {
  it.each(['서울', '광주 서구 풍암동', '서울대학교병원'])(
    '%s passes',
    (place) => {
      expect(checkPlace(place)).toEqual({ place })
    },
  )

  it('trims the ends only', () => {
    expect(checkPlace('  광주 서구 풍암동 ')).toEqual({
      place: '광주 서구 풍암동',
    })
  })

  it.each(['', '   ', '\t\n'])('refuses %j as PLACE_REQUIRED', (text) => {
    expect(checkPlace(text)).toEqual({ code: 'PLACE_REQUIRED' })
  })

  it('passes 50 characters and refuses 51 as PLACE_TOO_LONG', () => {
    expect(checkPlace('가'.repeat(50))).toEqual({ place: '가'.repeat(50) })
    expect(checkPlace(` ${'가'.repeat(50)} `)).toEqual({
      place: '가'.repeat(50),
    })
    expect(checkPlace('가'.repeat(51))).toEqual({ code: 'PLACE_TOO_LONG' })
  })
})
