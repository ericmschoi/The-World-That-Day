import {
  useCallback,
  useEffect,
  useEffectEvent,
  useMemo,
  useRef,
  useState,
} from 'react'
import { canAutoplay } from './autoplayPolicy'
import { MediaCaption } from './MediaCaption'
import { usePlaybackCoordinator, type Player } from './playbackCoordinator'
import type { Media } from './types'
import { useInView } from './useInView'
import { loadYouTubeApi, type YTPlayer } from './youtubeApi'
import styles from './MediaCard.module.css'

type Props = {
  media: Media
  fallbackAlt: string
  onUnavailable?: (id: string) => void
}

// `YT.PlayerState` values from the IFrame API reference.
const ENDED = 0
const PLAYING = 1
const PAUSED = 2
const BUFFERING = 3

// A YouTube video in the official player, from the server's embed URL as is
// (privacy-enhanced, checked by `canRender`), with the same caption as every
// card. Nothing is laid over or inside the player. The IFrame API attaches
// to the iframe once; then it plays muted while more than half is visible,
// one player at a time, like `VideoFilePlayer`. If a video cannot play, the
// whole card goes, quietly. Without the API (blocked script) it stays a
// plain player the reader can start (decision 1 (a)).
export function YouTubePlayer({ media, fallbackAlt, onUnavailable }: Props) {
  const coordinator = usePlaybackCoordinator()
  const iframe = useRef<HTMLIFrameElement | null>(null)
  // The API player, set on `onReady`: its methods exist only from then on.
  const api = useRef<YTPlayer | null>(null)
  const [ready, setReady] = useState(false)
  const [failed, setFailed] = useState(false)
  // States arrive late, so ours are told apart by the last reported state
  // (plan "YouTube 상태 처리"). want: our last call was play. pending: our
  // playVideo() may still bring a PLAYING. cancels: our pauses before that
  // start was reported, each maybe answered by a PAUSED (quick scrolling).
  const flags = useRef({
    state: -1,
    ourPauses: 0,
    cancels: 0,
    userPaused: false,
    played: false,
    want: false,
    pending: false,
    failed: false,
  })

  const player = useMemo<Player>(
    () => ({
      play() {
        const f = flags.current
        if (!api.current) return
        f.want = true
        f.pending = true
        // Muted on the first play; after that a sound on is the user's.
        if (!f.played) api.current.mute()
        f.played = true
        api.current.playVideo()
      },
      pause() {
        const f = flags.current
        if (!api.current) return
        f.want = false
        if (f.state === PLAYING || f.state === BUFFERING) f.ourPauses += 1
        else if (f.pending) f.cancels += 1
        else return
        api.current.pauseVideo()
      },
    }),
    [],
  )

  const onState = useEffectEvent((state: number) => {
    const f = flags.current
    f.state = state
    if (state === PLAYING) {
      // Our start began. Cancels still unanswered did not stop it.
      const ours = f.pending
      f.pending = false
      f.cancels = 0
      if (ours && !f.want) {
        // A start we cancelled began late: stop it, quietly.
        f.ourPauses += 1
        api.current?.pauseVideo()
      } else if (ours || f.ourPauses === 0) {
        f.userPaused = false
        f.played = true
        coordinator.notifyPlaying(player)
      } // else: stale, from before our pause
    } else if (state === PAUSED) {
      if (f.ourPauses > 0) f.ourPauses -= 1
      else if (f.cancels > 0) {
        // The answer to a cancel. A play after it is a start still to come.
        f.cancels -= 1
        f.pending = f.want || f.cancels > 0
      }
      // The user, or the browser or YouTube (M3b3 review 5).
      else f.userPaused = true
    } else if (state === ENDED) {
      f.pending = false
      f.userPaused = true
    }
  })

  const fail = useEffectEvent(() => {
    if (flags.current.failed) return
    flags.current.failed = true
    api.current = null
    setFailed(true)
    onUnavailable?.(media.id)
  })

  // Every "out of view" report, like `VideoFilePlayer` (M3b3b T0).
  const [observe, inView] = useInView<HTMLIFrameElement>((visible) => {
    if (!visible) coordinator.release(player)
  })
  const ref = useCallback(
    (el: HTMLIFrameElement | null) => {
      iframe.current = el
      observe(el)
    },
    [observe],
  )

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
              if (!live) return
              api.current = target
              setReady(true)
            },
            onStateChange: ({ data }) => {
              if (live) onState(data)
            },
            // Any code (2, 5, 100, 101, 150, 153): hide the card.
            onError: () => {
              if (live) fail()
            },
            // YouTube's controls stay; the start we asked for is not coming.
            onAutoplayBlocked: () => {
              flags.current.pending = false
              flags.current.cancels = 0
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

  useEffect(
    () => (ready && !failed ? coordinator.register(player) : undefined),
    [coordinator, player, ready, failed],
  )

  // On coming into view, or on getting ready while in view.
  useEffect(() => {
    if (ready && inView && !flags.current.userPaused && canAutoplay()) {
      coordinator.requestPlay(player)
    }
  }, [ready, inView, coordinator, player])

  if (failed) return null

  return (
    <figure className={styles.card}>
      <iframe
        ref={ref}
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
