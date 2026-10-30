import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { NewTabLink } from './NewTabLink'

// Plan M3b3b T1 (7): our privacy page is the one non-http(s) value that
// becomes a link. The http(s) behaviour is covered by the caption tests.
describe('NewTabLink', () => {
  it('links /privacy.html in a new tab, with the hidden notice', () => {
    render(<NewTabLink href="/privacy.html">개인정보처리방침</NewTabLink>)
    const link = screen.getByRole('link', {
      name: /^개인정보처리방침\s*\(opens in a new tab\)$/,
    })
    expect(link).toHaveAttribute('href', '/privacy.html')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it.each([
    '/other.html',
    '/api/story/world',
    '//evil.example/x',
    'javascript:alert(1)',
    '/privacy.html?x=1',
  ])('leaves %s as plain text', (href) => {
    const { container } = render(<NewTabLink href={href}>글자</NewTabLink>)
    expect(screen.queryByRole('link')).toBeNull()
    expect(container).toHaveTextContent('글자')
  })
})
