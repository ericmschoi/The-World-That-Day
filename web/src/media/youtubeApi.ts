import { YOUTUBE_IFRAME_API_URL } from '../external/urls'

// Only the part of the YouTube IFrame Player API we use (plan M3b3c, V4-5),
// typed by hand: no npm package (N1). Methods exist only after `onReady`.
export type YTPlayer = {
  mute(): void
  isMuted(): boolean
  playVideo(): void
  pauseVideo(): void
  getPlayerState(): number
}

export type YTPlayerEvents = {
  onReady?(event: { target: YTPlayer }): void
  onStateChange?(event: { target: YTPlayer; data: number }): void
  onError?(event: { target: YTPlayer; data: number }): void
  onAutoplayBlocked?(event: { target: YTPlayer }): void
}

export type YT = {
  // Attaches to an existing <iframe> whose src has `enablejsapi=1`.
  Player: new (
    element: HTMLIFrameElement,
    options: { events: YTPlayerEvents },
  ) => YTPlayer
  PlayerState: Record<
    'UNSTARTED' | 'ENDED' | 'PLAYING' | 'PAUSED' | 'BUFFERING' | 'CUED',
    number
  >
}

declare global {
  interface Window {
    YT?: YT
    onYouTubeIframeAPIReady?: () => void
  }
}

let loading: Promise<YT> | null = null

// The API, loading its script at most once per page. Already there: no
// script. A failed script is never inserted again; the promise rejects and
// the players stay plain official players (decision 1 (a)).
export function loadYouTubeApi(): Promise<YT> {
  if (window.YT?.Player) return Promise.resolve(window.YT)
  loading ??= new Promise<YT>((resolve, reject) => {
    const previous = window.onYouTubeIframeAPIReady
    window.onYouTubeIframeAPIReady = () => {
      previous?.()
      if (window.YT?.Player) resolve(window.YT)
      else reject(new Error('YouTube IFrame API not ready'))
    }
    const script = document.createElement('script')
    script.async = true
    script.src = YOUTUBE_IFRAME_API_URL
    script.addEventListener('error', () =>
      reject(new Error('YouTube IFrame API script failed')),
    )
    document.head.append(script)
  })
  return loading
}
