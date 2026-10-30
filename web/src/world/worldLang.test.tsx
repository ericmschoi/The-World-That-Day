// Plan C1 S1 T2 (3): the page is en-CA, but the server's event text is
// Korean (`summary_lang: 'ko'`) until S3a. The text and the source page title
// carry that language so screen readers read them right; the hidden new-tab
// hint stays in the page language. Demo date 1988-09-17 (product brief 12).
import { render, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { NewTabLink } from '../components/NewTabLink'
import { world1988, worldWithTragic } from '../mocks/fixtures/world/responses'
import { server } from '../mocks/server'
import { WorldSection } from './WorldSection'

const answer = (body: object) =>
  server.use(http.get('/api/story/world', () => HttpResponse.json(body)))

describe('event text language (summary_lang ko)', () => {
  it('marks every event sentence and source title link as ko', async () => {
    answer(world1988)
    render(<WorldSection date="1988-09-17" />)
    const list = await screen.findByRole('list', { name: 'Events' })
    const items = within(list).getAllByRole('listitem')
    expect(items).toHaveLength(world1988.events.length)
    world1988.events.forEach((event, index) => {
      const item = items[index]
      expect(within(item).getByText(event.summary)).toHaveAttribute(
        'lang',
        'ko',
      )
      const title = within(item).getByRole('link', {
        name: new RegExp(`^“${event.source_title}”`),
      })
      expect(title).toHaveAttribute('lang', 'ko')
      expect(title.querySelector('.visually-hidden')).toHaveAttribute(
        'lang',
        'en-CA',
      )
      // The rest of the source line stays in the page language.
      expect(item.lastElementChild).not.toHaveAttribute('lang')
      expect(
        within(item).getByRole('link', { name: /^CC BY-SA 4\.0/ }),
      ).not.toHaveAttribute('lang')
    })
  })

  it('marks the tragic block the same way', async () => {
    answer(worldWithTragic)
    render(<WorldSection date="1988-09-17" />)
    const heading = await screen.findByRole('heading', {
      name: 'Difficult events on the same day',
    })
    const block = heading.parentElement as HTMLElement
    const [tragic] = worldWithTragic.events.filter(
      (event) => event.tone === 'tragic',
    )
    expect(within(block).getByText(tragic.summary)).toHaveAttribute(
      'lang',
      'ko',
    )
    expect(
      within(block).getByRole('link', {
        name: new RegExp(`^“${tragic.source_title}”`),
      }),
    ).toHaveAttribute('lang', 'ko')
  })
})

describe('NewTabLink lang', () => {
  it('has no lang anywhere when none is given', () => {
    const { container } = render(
      <NewTabLink href="https://example.org/">Example</NewTabLink>,
    )
    expect(container.querySelector('[lang]')).toBeNull()
  })

  it('keeps the lang on plain text when the href is not http(s)', () => {
    render(
      <NewTabLink href="javascript:alert(1)" lang="ko">
        “9월 17일”
      </NewTabLink>,
    )
    expect(screen.queryByRole('link')).toBeNull()
    expect(screen.getByText('“9월 17일”')).toHaveAttribute('lang', 'ko')
  })
})
