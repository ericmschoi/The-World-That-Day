// Plan M3b2b T2: caption accessibility (M3b1b review follow-ups). The
// license line (b), new-tab notices on caption links, and the description
// read once. Synthetic photos and the demo fixtures only.
import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  koglPhoto,
  sameDayPhoto,
  unlicensedPhoto,
} from '../mocks/fixtures/media/photos'
import { olympicsOpeningPd } from '../mocks/fixtures/world/media'
import { MediaItem } from './MediaItem'
import type { Media } from './types'

const FALLBACK = '기록 사진'

const renderCard = (media: Media) => {
  render(<MediaItem media={media} fallbackAlt={FALLBACK} />)
  return screen.getByRole('figure')
}

// Caption lines as the eye reads them (hidden notices removed).
const lines = (figure: HTMLElement) =>
  [...figure.querySelectorAll('figcaption > p')].map((p) => {
    const copy = p.cloneNode(true) as HTMLElement
    copy.querySelectorAll('.visually-hidden').forEach((node) => node.remove())
    return copy.textContent?.trim()
  })

describe('license line (T2 (1))', () => {
  it('is left out when the attribution names the license; the link stays', () => {
    const figure = renderCard({
      ...koglPhoto,
      attribution: 'Example Agency · kogl  type 1 · Wikimedia Commons',
    })

    expect(lines(figure)).not.toContain('KOGL Type 1')
    expect(
      within(figure).getByRole('link', { name: /라이선스.*새 창에서 열림/ }),
    ).toHaveAttribute('href', koglPhoto.license_url)
  })

  it('is shown when the attribution does not name the license', () => {
    const figure = renderCard({
      ...sameDayPhoto,
      attribution: 'Example Author · Wikimedia Commons',
    })

    expect(lines(figure)).toContain('CC BY-SA 4.0')
  })

  it.each([
    ['license_status unknown', { license_status: 'unknown' }],
    ['license null', { license: null }],
    ['license blank', { license: '  ' }],
  ])(
    '%s: "라이선스 확인 안 됨" is always shown, even in the attribution',
    (_c, fields) => {
      const figure = renderCard({
        ...sameDayPhoto,
        attribution: 'Example Author · 라이선스 확인 안 됨 · CC BY-SA 4.0',
        ...fields,
      } as Media)

      expect(lines(figure)).toContain('라이선스 확인 안 됨')
    },
  )

  it('demo 1988-09-17 PD photo: no license line and no 라이선스 link', () => {
    const figure = renderCard({
      ...olympicsOpeningPd,
      date_relation: { kind: 'same_day', years: 0 },
    })

    expect(lines(figure)).not.toContain('Public domain')
    expect(figure).toHaveTextContent(olympicsOpeningPd.attribution)
    expect(within(figure).queryByRole('link', { name: /라이선스/ })).toBeNull()
  })
})

describe('caption links say they open a new tab (T2 (2))', () => {
  it('names both links with "새 창에서 열림"', () => {
    const figure = renderCard(sameDayPhoto)

    for (const name of [
      /원문 보기.*새 창에서 열림/,
      /라이선스.*새 창에서 열림/,
    ]) {
      const link = within(figure).getByRole('link', { name })
      expect(link).toHaveAttribute('target', '_blank')
      expect(link).toHaveAttribute('rel', 'noopener noreferrer')
    }
    expect(within(figure).getAllByRole('link')).toHaveLength(2)
  })

  it('keeps target="_blank" in the shared NewTabLink only', () => {
    const sources = import.meta.glob<string>('/src/**/*.{ts,tsx}', {
      query: '?raw',
      import: 'default',
      eager: true,
    })
    const withBlank = Object.entries(sources)
      .filter(([path]) => !path.includes('.test.'))
      .filter(([, text]) => text.includes('_blank'))
      .map(([path]) => path)

    expect(withBlank).toEqual(['/src/components/NewTabLink.tsx'])
  })
})

describe('the description is read once (T2 (3))', () => {
  it('puts the description in the caption and fallbackAlt in alt', () => {
    const figure = renderCard(sameDayPhoto)
    const description = sameDayPhoto.description!

    expect(within(figure).getByRole('img', { name: FALLBACK })).toBeVisible()
    expect(within(figure).getByRole('img')).not.toHaveAttribute(
      'alt',
      expect.stringContaining(description),
    )
    expect(figure.querySelector('figcaption')).toHaveTextContent(description)
  })

  it.each([
    ['null', null],
    ['blank', '   '],
  ])('uses fallbackAlt when the description is %s', (_c, description) => {
    const figure = renderCard({ ...unlicensedPhoto, description })

    expect(within(figure).getByRole('img')).toHaveAttribute('alt', FALLBACK)
  })
})
