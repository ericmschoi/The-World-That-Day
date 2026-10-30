import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  imageEmbed,
  linkOnly,
  sdkRoadview,
  videoEmbed,
  videoFile,
} from '../mocks/fixtures/media/modes'
import { sameDayPhoto, undatedPhoto } from '../mocks/fixtures/media/photos'
import { canRender, isDisplayable } from './isDisplayable'
import { MediaItem } from './MediaItem'
import type { Media } from './types'

const renderItem = (media: Media) =>
  render(<MediaItem media={media} fallbackAlt="서울 사진" />)

const patch = (media: Media, fields: Record<string, unknown>) =>
  ({ ...media, ...fields }) as Media

describe('MediaItem', () => {
  it.each([sameDayPhoto, undatedPhoto])(
    'renders an image record as a photo card ($id)',
    (media) => {
      renderItem(media)
      expect(screen.getByRole('figure')).toContainElement(
        screen.getByRole('img'),
      )
    },
  )

  it.each([
    ['source_url', ''],
    ['source_name', ''],
    ['attribution', ''],
    ['source_url', null],
  ])('renders nothing when R1 field %s is %j', (field, value) => {
    const { container } = renderItem(patch(sameDayPhoto, { [field]: value }))
    expect(container).toBeEmptyDOMElement()
  })

  it.each([
    ['thumbnail_url', 'javascript:alert(1)'],
    ['thumbnail_url', 'data:image/png;base64,AAAA'],
    ['source_url', '/wiki/File:Example.jpg'],
  ])('renders nothing when %s is %j', (field, value) => {
    const { container } = renderItem(patch(sameDayPhoto, { [field]: value }))
    expect(container).toBeEmptyDOMElement()
  })

  it('still renders the card when only license_url is malformed', () => {
    renderItem(patch(sameDayPhoto, { license_url: 'javascript:alert(1)' }))
    expect(screen.getByRole('img')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /^View original.*opens in a new tab/ }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Licence/ })).toBeNull()
  })

  // YouTube comes in M3b3c, map SDK media in M4; image_embed is left out.
  it.each([
    ['video_embed', videoEmbed],
    ['image_embed', imageEmbed],
    ['sdk', sdkRoadview],
    ['link_only', linkOnly],
  ])('renders nothing for %s yet', (_mode, media) => {
    const { container } = renderItem(media)
    expect(container).toBeEmptyDOMElement()
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })

  // M3b3 T2 (11): was in the table above, expecting nothing.
  it('renders a video card for video_file', () => {
    renderItem(videoFile)
    const figure = screen.getByRole('figure')
    expect(figure.querySelector('video')).toHaveAccessibleName('서울 사진')
    expect(screen.queryByRole('img')).toBeNull()
  })
})

describe('canRender (M3b3 T2 (10))', () => {
  it.each([
    ['image', sameDayPhoto, true],
    ['video_file', videoFile, true],
    ['video_embed', videoEmbed, false],
    ['image_embed', imageEmbed, false],
    ['sdk', sdkRoadview, false],
    ['link_only', linkOnly, false],
    ['image, not displayable', patch(sameDayPhoto, { source_url: '' }), false],
    [
      'video_file, not displayable',
      patch(videoFile, { attribution: '' }),
      false,
    ],
  ])('%s → %s', (_case, media, expected) => {
    expect(canRender(media)).toBe(expected)
  })

  it('agrees with what MediaItem draws', () => {
    for (const media of [videoEmbed, imageEmbed, sdkRoadview]) {
      expect(isDisplayable(media)).toBe(true)
      const { container, unmount } = renderItem(media)
      expect(container).toBeEmptyDOMElement()
      unmount()
    }
  })
})
