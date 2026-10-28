import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { agree } from '../test/consent'
import { openStory } from '../test/storyInput'

// Plan M3b3b T1 (6): one footer outside <main> on both screens, holding only
// the privacy policy link.
function expectFooter() {
  const footer = screen.getByRole('contentinfo')
  expect(screen.getByRole('main')).not.toContainElement(footer)
  const links = within(footer).getAllByRole('link')
  expect(links).toHaveLength(1)
  const [link] = links
  expect(link).toHaveAccessibleName(/^개인정보처리방침\s*\(새 창에서 열림\)$/)
  expect(link).toHaveAttribute('href', '/privacy.html')
  expect(link).toHaveAttribute('target', '_blank')
  expect(link).toHaveAttribute('rel', 'noopener noreferrer')
}

describe('SiteFooter', () => {
  it('links the privacy policy from the input and the result screen', async () => {
    const user = userEvent.setup()
    render(<App />)
    expectFooter()

    await agree(user)
    await openStory(user, { date: '1988-09-17', place: '서울' })
    await screen.findByRole('list', { name: '사건 목록' })
    expectFooter()
  })
})
