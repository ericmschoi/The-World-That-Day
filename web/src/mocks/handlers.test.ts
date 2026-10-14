// Plan M3b2 T1b (8): the default world handler answers the three demo dates
// with their fixtures and every other date with a bodiless 500.
import { describe, expect, it } from 'vitest'
import {
  world1988WithMedia,
  world1995,
  world2002WithMedia,
} from './fixtures/world/responses'

const worldUrl = (query: string) =>
  new URL(`/api/story/world${query}`, window.location.origin)

describe('default world handler', () => {
  it.each([
    ['1988-09-17', world1988WithMedia],
    ['2002-06-22', world2002WithMedia],
    ['1995-03-15', world1995],
  ])('answers %s with its fixture', async (date, fixture) => {
    const response = await fetch(worldUrl(`?date=${date}`))

    expect(response.status).toBe(200)
    expect(await response.json()).toEqual(JSON.parse(JSON.stringify(fixture)))
  })

  it.each([['?date=2001-02-29'], ['?date=1988-9-17'], ['?date='], ['']])(
    'answers %s with a 500 without a body',
    async (query) => {
      const response = await fetch(worldUrl(query))

      expect(response.status).toBe(500)
      expect(await response.text()).toBe('')
    },
  )
})
