import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { canAutoplay } from './autoplayPolicy'
import { MediaCaption } from './MediaCaption'
import { usePlaybackCoordinator, type Player } from './playbackCoordinator'
import type { Media } from './types'
import { useInView } from './useInView'
import styles from './MediaCard.module.css'

type Props = {
  media: Media
  fallbackAlt: string
  onUnavailable?: (id: string) => void
}

// `type` only when `mime` is a non-empty string, so a browser skips formats it
// cannot play. No `video_sources` but a `url`: that one file, no type.
function sourcesOf(media: Media) {
  if (media.video_sources?.length) {
    return media.video_sources.map(({ url, mime }) => ({
      url,
      type: typeof mime === 'string' && mime.trim() !== '' ? mime : undefined,
    }))
  }
  return media.url ? [{ url: media.url, type: undefined }] : []
}

// A video file played inline from its original URL (never rehosted), with the
// same caption as a photo. Muted autoplay while more than half is visible,
// unless the user asked for reduced motion or data saving; paused when it
// leaves the screen; one video at a time per result screen. Sound only when
// the user turns it on. If no source can play, the whole card goes, quietly.
export function VideoFilePlayer({ media, fallbackAlt, onUnavailable }: Props) {
  const coordinator = usePlaybackCoordinator()
  const [observe, inView] = useInView<HTMLVideoElement>()
  const video = useRef<HTMLVideoElement | null>(null)
  // ourPause: the next `pause` event is ours. userPaused: the user stopped it
  // (or it ended), so no autoplay until they play it again.
  const flags = useRef({
    ourPause: false,
    userPaused: false,
    unmuted: false,
    failed: false,
  })
  const [failed, setFailed] = useState(false)

  const player = useMemo<Player>(
    () => ({
      play() {
        const el = video.current
        if (!el) return
        // React's `muted` prop never sets the attribute: set the property.
        if (!flags.current.unmuted) el.muted = true
        // Blocked autoplay or a pause in between: stay on poster and controls.
        el.play().catch(() => {})
      },
      pause() {
        const el = video.current
        if (!el || el.paused) return
        flags.current.ourPause = true
        el.pause()
      },
    }),
    [],
  )

  const ref = useCallback(
    (el: HTMLVideoElement | null) => {
      video.current = el
      observe(el)
      if (el) {
        el.muted = true
        el.defaultMuted = true
      }
    },
    [observe],
  )

  useEffect(
    () => (failed ? undefined : coordinator.register(player)),
    [coordinator, player, failed],
  )

  useEffect(() => {
    if (!inView) coordinator.release(player)
    else if (!flags.current.userPaused && canAutoplay()) {
      coordinator.requestPlay(player)
    }
  }, [inView, coordinator, player])

  if (failed) return null

  const fail = () => {
    if (flags.current.failed) return
    flags.current.failed = true
    setFailed(true)
    onUnavailable?.(media.id)
  }
  const sources = sourcesOf(media)

  return (
    <figure className={styles.card}>
      <video
        ref={ref}
        className={styles.video}
        controls
        playsInline
        preload="metadata"
        poster={media.thumbnail_url ?? undefined}
        width={media.thumbnail_width ?? undefined}
        height={media.thumbnail_height ?? undefined}
        aria-label={fallbackAlt}
        onPlay={(event) => {
          // Browsers fire `play` later. If another video took over in the
          // meantime this one is paused again: a stale event, not the user.
          if (event.currentTarget.paused) return
          flags.current.userPaused = false
          coordinator.notifyPlaying(player)
        }}
        onPause={() => {
          if (flags.current.ourPause) flags.current.ourPause = false
          else flags.current.userPaused = true
        }}
        onVolumeChange={(event) => {
          flags.current.unmuted = !event.currentTarget.muted
        }}
        // React bubbles `<source>` errors up here too: only the video's own.
        onError={(event) => {
          if (event.target === event.currentTarget) fail()
        }}
      >
        {sources.map((source, index) => (
          <source
            key={`${index}-${source.url}`}
            src={source.url}
            type={source.type}
            // Only the last failure means no candidate could play.
            onError={index === sources.length - 1 ? fail : undefined}
          />
        ))}
      </video>
      <MediaCaption media={media} />
    </figure>
  )
}
