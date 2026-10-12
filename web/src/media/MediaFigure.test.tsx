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

const FALLBACK = '서울의 그날 모습을 담은 사진'

const renderFigure = (media: Media) =>
  render(<MediaFigure media={media} fallbackAlt={FALLBACK} />)

describe('MediaFigure, demo 1988-09-17', () => {
  it('shows the same-day photo inline from the original source', () => {
    renderFigure(sameDayPhoto)
    const img = screen.getByRole('img', { name: '1988년 서울 올림픽 개막식' })
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
    expect(within(figure).getByText('같은 날')).toBeInTheDocument()
    expect(figure).toHaveTextContent('같은 날 1988년 9월 17일 촬영')
    expect(figure).toHaveTextContent(sameDayPhoto.attribution)
    expect(figure).toHaveTextContent('CC BY-SA 4.0')
    expect(
      within(figure).getByRole('link', { name: '원문 보기' }),
    ).toHaveAttribute('href', sameDayPhoto.source_url)
    expect(
      within(figure).getByRole('link', { name: '라이선스' }),
    ).toHaveAttribute('href', sameDayPhoto.license_url)
  })

  it('shows an undated photo, labeled only 촬영 시점 미상', () => {
    renderFigure(undatedPhoto)
    expect(
      screen.getByRole('img', { name: '서울 올림픽 경기장 전경' }),
    ).toHaveAttribute('src', undatedPhoto.thumbnail_url)
    const figure = screen.getByRole('figure')
    expect(figure).toHaveTextContent('촬영 시점 미상')
    expect(figure.textContent).not.toContain('같은 날')
    expect(figure.textContent).not.toContain('그날')
    expect(figure.textContent).not.toContain('1988년 9월 17일')
  })

  it('shows the 무렵 badge for an approximate year', () => {
    renderFigure(approximatePhoto)
    const figure = screen.getByRole('figure')
    expect(within(figure).getByText('1년 전 무렵')).toBeInTheDocument()
    expect(figure).toHaveTextContent('1987년 무렵 촬영(추정)')
  })
})

describe('MediaFigure, demo 2002-06-22', () => {
  it('names the KOGL Type 1 license in the caption', () => {
    renderFigure(koglPhoto)
    expect(screen.getByRole('img')).toBeInTheDocument()
    expect(screen.getByText('KOGL Type 1')).toBeInTheDocument()
  })
})

describe('MediaFigure, missing values', () => {
  it('shows an unlicensed photo with 라이선스 확인 안 됨 and the fallback alt', () => {
    renderFigure(unlicensedPhoto)
    expect(screen.getByRole('img', { name: FALLBACK })).toHaveAttribute(
      'src',
      unlicensedPhoto.thumbnail_url,
    )
    expect(screen.getByText('라이선스 확인 안 됨')).toBeInTheDocument()
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
