import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import globalCss from '../styles/global.css?raw'
import { sameDayPhoto, undatedPhoto } from '../mocks/fixtures/media/photos'
import { MediaCaption } from './MediaCaption'
import type { Media } from './types'

// Caption links carry the hidden "(새 창에서 열림)" (M3b2b T2).
const SOURCE_LINK = /^원문 보기\s*\(새 창에서 열림\)$/
const LICENSE_LINK = /^라이선스\s*\(새 창에서 열림\)$/

const renderCaption = (media: Media) =>
  render(
    <figure>
      <MediaCaption media={media} />
    </figure>,
  )

describe('MediaCaption', () => {
  it('puts the date line first', () => {
    const { container } = renderCaption(sameDayPhoto)
    const first = container.querySelector('figcaption > p')
    expect(first).toHaveTextContent('같은 날 1988년 9월 17일 촬영')
  })

  it('opens external links in a new tab without opener or referrer', () => {
    renderCaption(sameDayPhoto)
    for (const link of screen.getAllByRole('link')) {
      expect(link).toHaveAttribute('target', '_blank')
      expect(link.getAttribute('rel')?.split(' ')).toEqual(
        expect.arrayContaining(['noopener', 'noreferrer']),
      )
    }
    expect(screen.getAllByRole('link')).toHaveLength(2)
  })

  it('has no license link when license_url is null', () => {
    renderCaption(undatedPhoto)
    expect(screen.getByRole('link', { name: SOURCE_LINK })).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: LICENSE_LINK }),
    ).not.toBeInTheDocument()
  })

  it.each([
    'javascript:alert(1)',
    'mailto:someone@example.org',
    '/licenses/by-sa/4.0/',
    '',
  ])('has no license link when license_url is %j', (license_url) => {
    renderCaption({ ...sameDayPhoto, license_url })
    expect(screen.getByRole('link', { name: SOURCE_LINK })).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: LICENSE_LINK }),
    ).not.toBeInTheDocument()
    // Named in the attribution, so no separate license line.
    expect(screen.getByRole('figure')).toHaveTextContent('CC BY-SA 4.0')
  })

  describe('provider-terms (M3b3c T1 (5))', () => {
    const TERMS_LINK = /^이용약관\s*\(새 창에서 열림\)$/
    const youtube: Media = {
      ...sameDayPhoto,
      license: 'provider-terms',
      license_status: 'other',
      license_url: 'https://www.youtube.com/t/terms',
      attribution: 'KTV 아카이브 · YouTube',
    }

    it('says 제공사 이용약관에 따름 and links 이용약관 in a new tab', () => {
      renderCaption(youtube)
      const figure = screen.getByRole('figure')
      expect(figure).toHaveTextContent('제공사 이용약관에 따름')
      expect(figure).not.toHaveTextContent('provider-terms')
      const link = screen.getByRole('link', { name: TERMS_LINK })
      expect(link).toHaveAttribute('href', 'https://www.youtube.com/t/terms')
      expect(link).toHaveAttribute('target', '_blank')
      expect(link.getAttribute('rel')?.split(' ')).toEqual(
        expect.arrayContaining(['noopener', 'noreferrer']),
      )
      expect(link.querySelector('.visually-hidden')).toHaveTextContent(
        '(새 창에서 열림)',
      )
      expect(
        screen.queryByRole('link', { name: LICENSE_LINK }),
      ).not.toBeInTheDocument()
    })

    it.each([null, 'javascript:alert(1)', '/t/terms'])(
      'has no terms link when license_url is %j',
      (license_url) => {
        renderCaption({ ...youtube, license_url })
        expect(screen.getAllByRole('link')).toHaveLength(1)
        expect(screen.getByRole('link', { name: SOURCE_LINK })).toBeVisible()
      },
    )

    it('keeps 라이선스 for any other license', () => {
      renderCaption({
        ...youtube,
        license: 'CC BY 4.0',
        license_status: 'free',
      })
      expect(screen.getByRole('link', { name: LICENSE_LINK })).toBeVisible()
      expect(
        screen.queryByRole('link', { name: TERMS_LINK }),
      ).not.toBeInTheDocument()
    })
  })

  it('shows no badge for an unknown capture time', () => {
    const { container } = renderCaption(undatedPhoto)
    const first = container.querySelector('figcaption > p')
    expect(first).toHaveTextContent(/^촬영 시점 미상$/)
    expect(first?.querySelector('span')).toBeNull()
  })

  describe('keyboard focus (M3b1b T2)', () => {
    // Vitest blanks CSS imports, so load the real global.css text. jsdom does
    // not resolve var(), so the declared value is checked, not the colour.
    const style = document.createElement('style')
    afterEach(() => style.remove())

    it('Tab reaches 원문 보기 and the global focus ring applies', async () => {
      style.textContent = globalCss.replace(/@import[^;]*;/, '')
      document.head.append(style)
      renderCaption(sameDayPhoto)
      await userEvent.tab()
      const link = screen.getByRole('link', { name: SOURCE_LINK })
      expect(link).toHaveFocus()
      expect(link.matches('a:focus-visible')).toBe(true)
      expect(getComputedStyle(link).outline).toBe(
        'var(--focus-ring-width) solid var(--color-focus)',
      )
      await userEvent.tab()
      expect(screen.getByRole('link', { name: LICENSE_LINK })).toHaveFocus()
    })
  })
})
