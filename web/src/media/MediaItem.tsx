import { useConsent } from '../consent/consent'
import { canRender } from './isDisplayable'
import { MediaFigure } from './MediaFigure'
import type { Media } from './types'
import { VideoFilePlayer } from './VideoFilePlayer'
import { YouTubePlayer } from './YouTubePlayer'

type Props = {
  media: Media
  fallbackAlt: string
  // Called once with the id when a video cannot play and its card is hidden.
  onUnavailable?: (id: string) => void
}

// Single entry point for every media card. Media that fails the checks is
// hidden, never turned into a link card.
export function MediaItem({ media, fallbackAlt, onUnavailable }: Props) {
  const consented = useConsent()
  if (!canRender(media, consented)) return null
  switch (media.display_mode) {
    case 'image':
      return <MediaFigure media={media} fallbackAlt={fallbackAlt} />
    case 'video_file':
      return (
        <VideoFilePlayer
          media={media}
          fallbackAlt={fallbackAlt}
          onUnavailable={onUnavailable}
        />
      )
    case 'video_embed':
      return (
        <YouTubePlayer
          media={media}
          fallbackAlt={fallbackAlt}
          onUnavailable={onUnavailable}
        />
      )
    // image_embed: left out (M3a6). sdk: M4.
    // link_only: not produced in M3 and never shown.
    default:
      return null
  }
}
