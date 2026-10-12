import { isDisplayable } from './isDisplayable'
import { MediaCaption } from './MediaCaption'
import type { Media } from './types'
import styles from './MediaCard.module.css'

type Props = { media: Media; fallbackAlt: string }

// A photo shown inline, straight from the original source (never rehosted),
// always with its caption. Records that fail the checks render nothing.
export function MediaFigure({ media, fallbackAlt }: Props) {
  if (media.display_mode !== 'image' || !isDisplayable(media)) return null
  return (
    <figure className={styles.card}>
      <img
        className={styles.image}
        src={media.thumbnail_url ?? undefined}
        alt={media.description?.trim() || fallbackAlt}
        width={media.thumbnail_width ?? undefined}
        height={media.thumbnail_height ?? undefined}
        loading="lazy"
        decoding="async"
        referrerPolicy="no-referrer"
      />
      <MediaCaption media={media} />
    </figure>
  )
}
