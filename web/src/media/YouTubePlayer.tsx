import { useEffect, useEffectEvent, useRef, useState } from 'react'
import { MediaCaption } from './MediaCaption'
import type { Media } from './types'
import { loadYouTubeApi, type YTPlayer } from './youtubeApi'
import styles from './MediaCard.module.css'

type Props = {
  media: Media
  fallbackAlt: string
  onUnavailable?: (id: string) => void
}

// A YouTube video in the official player, from the server's embed URL as is
// (privacy-enhanced, checked by `canRender`), with the same caption as every
// card. Nothing is laid over or inside the player. The IFrame API attaches
// to the iframe once; if a video cannot play, the whole card goes, quietly.
// Without the API (blocked script) it stays a plain player the reader can
// start (decision 1 (a)).
export function YouTubePlayer({ media, fallbackAlt, onUnavailable }: Props) {
  const iframe = useRef<HTMLIFrameElement | null>(null)
  // The API player, set on `onReady`: its methods exist only from then on.
  const ready = useRef<YTPlayer | null>(null)
  const failedOnce = useRef(false)
  const [failed, setFailed] = useState(false)

  const fail = useEffectEvent(() => {
    if (failedOnce.current) return
    failedOnce.current = true
    ready.current = null
    setFailed(true)
    onUnavailable?.(media.id)
  })

  // Runs once per iframe. Under StrictMode the first run is cleaned up
  // before the API resolves, so only one `YT.Player` is made. No `destroy()`
  // (it removes the iframe React owns): after unmount events are ignored.
  useEffect(() => {
    let live = true
    loadYouTubeApi().then(
      (yt) => {
        const el = iframe.current
        if (!live || !el) return
        new yt.Player(el, {
          events: {
            onReady: ({ target }) => {
              if (live) ready.current = target
            },
            // Any code (2, 5, 100, 101, 150, 153): hide the card.
            onError: () => {
              if (live) fail()
            },
          },
        })
      },
      () => {},
    )
    return () => {
      live = false
    }
  }, [])

  if (failed) return null

  return (
    <figure className={styles.card}>
      <iframe
        ref={iframe}
        className={styles.embed}
        src={media.embed_url ?? undefined}
        title={fallbackAlt}
        allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
        allowFullScreen
        loading="lazy"
        // Origin only: YouTube needs a Referer, the result path stays here.
        referrerPolicy="strict-origin-when-cross-origin"
      />
      <MediaCaption media={media} />
    </figure>
  )
}
