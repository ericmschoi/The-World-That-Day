import { isDisplayable } from './isDisplayable'
import { MediaFigure } from './MediaFigure'
import type { Media } from './types'

type Props = { media: Media; fallbackAlt: string }

// Single entry point for every media card. Media that fails the checks is
// hidden, never turned into a link card.
export function MediaItem({ media, fallbackAlt }: Props) {
  if (!isDisplayable(media)) return null
  switch (media.display_mode) {
    case 'image':
      return <MediaFigure media={media} fallbackAlt={fallbackAlt} />
    // image_embed, video_file, video_embed: M3b3. sdk: M4.
    // link_only: not produced in M3 and never shown.
    default:
      return null
  }
}
