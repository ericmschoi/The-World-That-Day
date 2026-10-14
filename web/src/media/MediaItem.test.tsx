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
      screen.getByRole('link', { name: /^원문 보기.*새 창에서 열림/ }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /라이선스/ })).toBeNull()
  })

  // Video and iframe cards come in M3b3, map SDK media in M4.
  it.each([
    ['video_embed', videoEmbed],
    ['image_embed', imageEmbed],
    ['video_file', videoFile],
    ['sdk', sdkRoadview],
    ['link_only', linkOnly],
  ])('renders nothing for %s yet', (_mode, media) => {
    const { container } = renderItem(media)
    expect(container).toBeEmptyDOMElement()
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })
})
