import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  approximatePhoto,
  koglPhoto,
  sameDayPhoto,
  undatedPhoto,
  unlicensedPhoto,
} from '../mocks/fixtures/media/photos'
import { videoEmbed } from '../mocks/fixtures/media/modes'
import { MediaFigure } from './MediaFigure'
import type { Media } from './types'

const FALLBACK = '서울 사진'

// Caption links carry the hidden "(opens in a new tab)" (M3b2b T2).
const SOURCE_LINK = /^View original\s*\(opens in a new tab\)$/
const LICENSE_LINK = /^Licence\s*\(opens in a new tab\)$/

const renderFigure = (media: Media) =>
  render(<MediaFigure media={media} fallbackAlt={FALLBACK} />)

describe('MediaFigure, demo 1988-09-17', () => {
  it('shows the same-day photo inline from the original source', () => {
    renderFigure(sameDayPhoto)
    // The description is read in the caption only; alt is fallbackAlt.
    const img = screen.getByRole('img', { name: FALLBACK })
    expect(img).toHaveAttribute('src', sameDayPhoto.thumbnail_url)
    expect(img).toHaveAttribute('loading', 'lazy')
    expect(img).toHaveAttribute('decoding', 'async')
    expect(img).toHaveAttribute('referrerpolicy', 'no-referrer')
    expect(img).toHaveAttribute('width', '640')
    expect(img).toHaveAttribute('height', '427')
    expect(img.closest('figure')).not.toBeNull()
  })

  it('captions it with 같은 날, the capture date, source and both links', () => {
    renderFigure(sameDayPhoto)
    const figure = screen.getByRole('figure')
    expect(within(figure).getByText('Same day')).toBeInTheDocument()
    expect(figure).toHaveTextContent('Same day Taken September 17, 1988')
    expect(figure).toHaveTextContent(sameDayPhoto.attribution)
    expect(figure).toHaveTextContent('CC BY-SA 4.0')
    expect(
      within(figure).getByRole('link', { name: SOURCE_LINK }),
    ).toHaveAttribute('href', sameDayPhoto.source_url)
    expect(
      within(figure).getByRole('link', { name: LICENSE_LINK }),
    ).toHaveAttribute('href', sameDayPhoto.license_url)
  })

  it('shows an undated photo, labeled only 촬영 시점 미상', () => {
    renderFigure(undatedPhoto)
    expect(screen.getByRole('img', { name: FALLBACK })).toHaveAttribute(
      'src',
      undatedPhoto.thumbnail_url,
    )
    const figure = screen.getByRole('figure')
    expect(figure).toHaveTextContent('Capture time unknown')
    expect(figure.textContent).not.toContain('Same day')
    expect(figure.textContent).not.toContain('that day')
    expect(figure.textContent).not.toContain('September 17, 1988')
  })

  it('shows the 무렵 badge for an approximate year', () => {
    renderFigure(approximatePhoto)
    const figure = screen.getByRole('figure')
    expect(within(figure).getByText('About 1 year earlier')).toBeInTheDocument()
    expect(figure).toHaveTextContent('Taken around 1987 (estimated)')
  })
})

describe('MediaFigure, demo 2002-06-22', () => {
  it('names the KOGL Type 1 license in the caption', () => {
    renderFigure(koglPhoto)
    expect(screen.getByRole('img')).toBeInTheDocument()
    // Named in the attribution: no separate line, the link stays.
    expect(screen.getByRole('figure')).toHaveTextContent('KOGL Type 1')
    expect(screen.queryByText('KOGL Type 1')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: LICENSE_LINK })).toHaveAttribute(
      'href',
      koglPhoto.license_url,
    )
  })
})

describe('MediaFigure, missing values', () => {
  it('shows an unlicensed photo with 라이선스 확인 안 됨 and the fallback alt', () => {
    renderFigure(unlicensedPhoto)
    expect(screen.getByRole('img', { name: FALLBACK })).toHaveAttribute(
      'src',
      unlicensedPhoto.thumbnail_url,
    )
    expect(screen.getByText('Licence not verified')).toBeInTheDocument()
  })

  it('uses the fallback alt for a blank description', () => {
    renderFigure({ ...sameDayPhoto, description: '  ' })
    expect(screen.getByRole('img', { name: FALLBACK })).toBeInTheDocument()
  })

  it('leaves width and height off when the size is unknown', () => {
    renderFigure({
      ...sameDayPhoto,
      thumbnail_width: null,
      thumbnail_height: null,
    })
    const img = screen.getByRole('img')
    expect(img).not.toHaveAttribute('width')
    expect(img).not.toHaveAttribute('height')
  })

  it.each([
    ['a record without source_url', { ...sameDayPhoto, source_url: '' }],
    ['a non-image mode', videoEmbed],
  ])('renders nothing for %s', (_name, media) => {
    const { container } = renderFigure(media)
    expect(container).toBeEmptyDOMElement()
  })
})
