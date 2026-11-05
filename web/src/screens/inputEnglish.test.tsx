// Plan C1 S2 T6 (2) and (3): the screen text of the input screen and of the
// opening is English (no Hangul, no US "license"), and the story request
// carries the date only: no place text and no time (privacy policy S-1 and
// the input-screen summary S-9). Dates and places are the demo pairs
// (product brief 12 / Canada research decision 1 (b)). Server-supplied Korean
// event text is out of scope until S3a, so the opening test uses the answer
// with no events.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { world1950, world1988 } from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import { agree } from '../test/consent'
import { openStory, submitStory } from '../test/storyInput'

const HANGUL = /[ᄀ-ᇿ㄰-㆏가-힯]/
const US_SPELLING = /\blicense\b/i

function everythingRead(): string {
  const attributes = [
    ...document.body.querySelectorAll('[alt], [aria-label], [title]'),
  ].flatMap((element) =>
    ['alt', 'aria-label', 'title']
      .map((name) => element.getAttribute(name))
      .filter((value): value is string => value !== null),
  )
  return [document.body.textContent ?? '', ...attributes].join('\n')
}

const DEMOS = [
  { date: '1988-02-13', place: 'Calgary' },
  {
    date: '1976-07-17',
    place: 'Hochelaga-Maisonneuve, Montréal',
    time: '15:20',
  },
  { date: '2010-02-12', place: 'Vancouver General Hospital', time: '00:05' },
  { date: '1980-04-12', place: "St. John's" },
]

describe('input screen and opening text is English (plan C1 S2 T6 (2))', () => {
  it('shows every error message with no Hangul and no "license"', async () => {
    const user = userEvent.setup()
    render(<App />)
    await submitStory(user) // empty: date, place and consent messages
    const text = everythingRead()
    expect(text).toContain('Agree to the privacy policy to see the story.')
    expect(
      document.body.querySelectorAll('[aria-invalid="true"]').length,
    ).toBeGreaterThan(0)
    expect(text).not.toMatch(HANGUL)
    expect(text).not.toMatch(US_SPELLING)
  })

  it('shows the opening on the result screen with no Hangul', async () => {
    server.use(http.get('/api/story/world', () => HttpResponse.json(world1950)))
    const user = userEvent.setup()
    render(<App />)
    await agree(user)
    await openStory(user, DEMOS[1]!)
    await screen.findByText('No major events were found for this date.')
    expect(
      screen.getByText(
        'Saturday, July 17, 1976 at 3:20 p.m., Hochelaga-Maisonneuve, Montréal',
      ),
    ).toBeVisible()
    const text = everythingRead()
    expect(text).not.toMatch(HANGUL)
    expect(text).not.toMatch(US_SPELLING)
  })
})

describe('only the date leaves the browser (plan C1 S2 T6 (3))', () => {
  it('sends /api/story/world?date=… alone for the four demo inputs', async () => {
    const requests: string[] = []
    server.events.on('request:start', ({ request }) => {
      requests.push(request.url)
    })
    server.use(http.get('/api/story/world', () => HttpResponse.json(world1988)))
    try {
      for (const demo of DEMOS) {
        const user = userEvent.setup()
        const { unmount } = render(<App />)
        await agree(user)
        await openStory(user, demo)
        await screen.findByRole('list', { name: 'Events' })
        unmount()
      }
    } finally {
      server.events.removeAllListeners('request:start')
    }

    expect(requests).toHaveLength(DEMOS.length)
    DEMOS.forEach((demo, index) => {
      const url = new URL(requests[index]!)
      expect(url.pathname).toBe('/api/story/world')
      expect([...url.searchParams.keys()]).toEqual(['date'])
      expect(url.searchParams.get('date')).toBe(demo.date)
    })
    const all = decodeURIComponent(requests.join('\n')).toLowerCase()
    for (const demo of DEMOS) {
      expect(all).not.toContain(demo.place.toLowerCase())
      if (demo.time) {
        expect(all).not.toContain(demo.time)
        expect(all).not.toContain(demo.time.replace(':', '%3a'))
      }
    }
    for (const word of ['montr', "john's", 'calgary', 'vancouver']) {
      expect(all).not.toContain(word)
    }
  })
})
