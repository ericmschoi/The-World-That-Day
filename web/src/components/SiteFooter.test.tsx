import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { agree } from '../test/consent'

// Plan M3b3b T1 (6): one footer outside <main> on both screens, holding only
// the privacy policy link.
function expectFooter() {
  const footer = screen.getByRole('contentinfo')
  expect(screen.getByRole('main')).not.toContainElement(footer)
  const links = within(footer).getAllByRole('link')
  expect(links).toHaveLength(1)
  const [link] = links
  expect(link).toHaveAccessibleName(
    /^개인정보처리방침\s*\(opens in a new tab\)$/,
  )
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
    await user.click(
      screen.getByRole('button', { name: '1988년 9월 17일로 보기' }),
    )
    await screen.findByRole('list', { name: '사건 목록' })
    expectFooter()
  })
})
