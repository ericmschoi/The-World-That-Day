// Plans M2b T3 (1) and C1 S2 T8b: the place rules. Places are the demo
// places (Canada research decision 1 (b)) only.
import { describe, expect, it } from 'vitest'
import { checkPlace } from './placeRules'

describe('checkPlace', () => {
  it.each([
    'Calgary',
    'Hochelaga-Maisonneuve, Montréal',
    'Vancouver General Hospital',
    "St. John's",
  ])('%s passes', (place) => {
    expect(checkPlace(place)).toEqual({ place })
  })

  it('trims the ends only', () => {
    expect(checkPlace('  Hochelaga-Maisonneuve, Montréal ')).toEqual({
      place: 'Hochelaga-Maisonneuve, Montréal',
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
