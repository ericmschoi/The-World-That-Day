// M3b3 decision 2 (b): a caption `description` or `license` that is not a
// string counts as missing instead of throwing, for photos and videos alike.
import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { videoFile } from '../mocks/fixtures/media/modes'
import { koglPhoto, sameDayPhoto } from '../mocks/fixtures/media/photos'
import { MediaItem } from './MediaItem'
import type { Media } from './types'

const patch = (media: Media, fields: Record<string, unknown>) =>
  ({ ...media, ...fields }) as Media

// A neighbour card that must survive the broken one.
const neighbour = patch(koglPhoto, { id: 'example-neighbour' })

const renderWithNeighbour = (media: Media) =>
  render(
    <>
      <MediaItem media={media} fallbackAlt="기록" />
      <MediaItem media={neighbour} fallbackAlt="이웃" />
    </>,
  )

const captionLines = (figure: HTMLElement) =>
  [...figure.querySelectorAll('figcaption > p')].map((p) => p.textContent)

afterEach(() => vi.restoreAllMocks())

describe.each([
  ['photo', sameDayPhoto],
  ['video', videoFile],
])('caption defense for a %s', (_kind, base) => {
  it.each([
    ['a number', 42],
    ['an object', {}],
  ])('shows no description line when description is %s', (_c, value) => {
    const error = vi.spyOn(console, 'error')
    renderWithNeighbour(patch(base, { description: value }))
    const [card, other] = screen.getAllByRole('figure')
    // The attribution names the license, so: date, attribution, links.
    expect(captionLines(card)).toEqual([
      expect.stringContaining('Same day'),
      base.attribution,
      expect.stringContaining('View original'),
    ])
    expect(other).toHaveTextContent(neighbour.attribution)
    expect(error).not.toHaveBeenCalled()
  })

  it.each([
    ['a number', 42],
    ['an object', {}],
  ])('says Licence not verified when license is %s', (_c, value) => {
    const error = vi.spyOn(console, 'error')
    renderWithNeighbour(
      patch(base, {
        license: value,
        attribution:
          'Example Author · Licence not verified · Wikimedia Commons',
      }),
    )
    const [card, other] = screen.getAllByRole('figure')
    // Never hidden, even though the attribution says the same words.
    expect(captionLines(card)).toContain('Licence not verified')
    expect(other).toHaveTextContent(neighbour.attribution)
    expect(error).not.toHaveBeenCalled()
  })
})
