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
  // Our calls the player has not answered yet, in order (plan "YouTube 상태
  // 처리", revised). play: a start, answered by PLAYING (or dropped by a
  // cancel); pause: a pause of a running player, answered by PAUSED; cancel:
  // a pause before the start was reported, answered by PAUSED or by nothing
  // (the start goes on). Merged or missing answers are skipped, never
  // waited for. want: our last call was play (or the user played it).
  const flags = useRef({
    state: -1,
    want: false,
    calls: [] as ('play' | 'pause' | 'cancel')[],
    offScreen: false,
    userPaused: false,
    played: false,
    failed: false,
  })

  const player = useMemo<Player>(
    () => ({
      play() {
        const f = flags.current
        if (!api.current) return
        f.want = true
        // Already playing (the user started it), nothing of ours on the way.
        if (f.state === PLAYING && f.calls.length === 0) return
        // A play during a start that was not cancelled starts nothing new.
        if (f.calls.at(-1) !== 'play') f.calls.push('play')
        // Muted on the first play; after that a sound on is the user's.
        if (!f.played) api.current.mute()
        f.played = true
        api.current.playVideo()
      },
      pause() {
        const f = flags.current
        if (!api.current) return
        f.want = false
        if (f.state === PLAYING || f.state === BUFFERING) f.calls.push('pause')
        else if (f.calls.includes('play')) f.calls.push('cancel')
        else return // nothing running or starting
        api.current.pauseVideo()
      },
    }),
    [],
  )

  const onState = useEffectEvent((state: number) => {
    const f = flags.current
    f.state = state
    if (state === PLAYING) {
      const at = f.calls.indexOf('play')
      if (at < 0) {
        // Nothing of ours on the way: the user's own play.
        f.calls = []
        f.want = true
      } else {
        // That start's answer. The cancels right after it did not stop it,
        // and a play after such a cancel started nothing new.
        let end = at + 1
        while (f.calls[end] === 'cancel' || f.calls[end] === 'play') end += 1
        f.calls = f.calls.slice(end)
      }
      if (!f.want || f.offScreen) {
        // A start we no longer want, or anything off screen: stop it,
        // quietly, so it never takes over from the player in view.
        player.pause()
      } else {
        f.userPaused = false
        f.played = true
        coordinator.notifyPlaying(player)
      }
    } else if (state === PAUSED) {
      const at = f.calls.findIndex((call) => call !== 'play')
      if (at >= 0)
        f.calls = f.calls.slice(at + 1) // ours
      else if (f.want) {
        // The user, or the browser or YouTube (M3b3 review 5).
        f.userPaused = true
        f.want = false
      }
    } else if (state === ENDED) {
      Object.assign(f, { calls: [], want: false, userPaused: true })
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
  const [observe, inView] = useInView<HTMLIFrameElement>(
    (visible) => {
      if (!visible) coordinator.release(player)
    },
    (onScreen) => {
      flags.current.offScreen = !onScreen
    },
  )
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
              flags.current.calls = []
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
