import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { sameDayPhoto, undatedPhoto } from '../mocks/fixtures/media/photos'
import { MediaCaption } from './MediaCaption'
import type { Media } from './types'

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
    expect(screen.getByRole('link', { name: '원문 보기' })).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: '라이선스' }),
    ).not.toBeInTheDocument()
  })

  it.each([
    'javascript:alert(1)',
    'mailto:someone@example.org',
    '/licenses/by-sa/4.0/',
    '',
  ])('has no license link when license_url is %j', (license_url) => {
    renderCaption({ ...sameDayPhoto, license_url })
    expect(screen.getByRole('link', { name: '원문 보기' })).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: '라이선스' }),
    ).not.toBeInTheDocument()
    expect(screen.getByText('CC BY-SA 4.0')).toBeInTheDocument()
  })

  it('shows no badge for an unknown capture time', () => {
    const { container } = renderCaption(undatedPhoto)
    const first = container.querySelector('figcaption > p')
    expect(first).toHaveTextContent(/^촬영 시점 미상$/)
    expect(first?.querySelector('span')).toBeNull()
  })
})
