import { afterEach, describe, expect, it, vi } from 'vitest'
import type { YTPlayerEvents } from '../media/youtubeApi'
import {
  type FakePlayer,
  installYouTubeFake,
  PlayerState,
  youtubeScripts,
  type YouTubeFake,
} from './youtubeFake'

let fake: YouTubeFake | null = null

afterEach(() => {
  fake?.uninstall()
  fake = null
})

const { ENDED, PLAYING, PAUSED, BUFFERING } = PlayerState

// A player on a new iframe, with every event recorded.
function create(yt: YouTubeFake) {
  const iframe = document.createElement('iframe')
  const log: string[] = []
  const events: YTPlayerEvents = {
    onReady: () => log.push('ready'),
    onStateChange: ({ data }) => log.push(`state ${data}`),
    onError: ({ data }) => log.push(`error ${data}`),
    onAutoplayBlocked: () => log.push('autoplay blocked'),
  }
  const player = new yt.YT.Player(iframe, { events }) as FakePlayer
  return { iframe, log, player }
}

describe('installYouTubeFake', () => {
  it('records each player with its element', () => {
    fake = installYouTubeFake()
    expect(window.YT).toBe(fake.YT)
    expect(fake.YT.PlayerState.PLAYING).toBe(1)
    const a = create(fake)
    const b = create(fake)
    expect(fake.players).toEqual([a.player, b.player])
    expect(fake.playersFor(a.iframe)).toEqual([a.player])
    expect(fake.playersFor(document.createElement('iframe'))).toEqual([])
  })

  it('has no methods before onReady, like the real player', () => {
    fake = installYouTubeFake()
    const { player, log } = create(fake)
    expect(() => player.playVideo()).toThrow(TypeError)
    fake.ready(player)
    expect(log).toEqual(['ready'])
    expect(() => player.playVideo()).not.toThrow()
  })

  it('plays through buffering, pauses, and records calls in order', () => {
    fake = installYouTubeFake()
    const { player, log } = create(fake)
    fake.ready(player)
    player.mute()
    expect(player.isMuted()).toBe(true)
    player.playVideo()
    player.playVideo()
    expect(player.getPlayerState()).toBe(PLAYING)
    player.pauseVideo()
    player.pauseVideo()
    expect(log).toEqual([
      'ready',
      `state ${BUFFERING}`,
      `state ${PLAYING}`,
      `state ${PAUSED}`,
    ])
    expect(fake.callsOf(player)).toEqual([
      'mute',
      'isMuted',
      'playVideo',
      'playVideo',
      'getPlayerState',
      'pauseVideo',
      'pauseVideo',
    ])
  })

  it('sends nothing for a pause before any start', () => {
    fake = installYouTubeFake()
    const { player, log } = create(fake)
    fake.ready(player)
    player.pauseVideo()
    expect(log).toEqual(['ready'])
  })

  it('sends states, errors and blocked autoplay on request', () => {
    fake = installYouTubeFake()
    const { player, log } = create(fake)
    fake.state(player, ENDED)
    fake.error(player, 150)
    fake.autoplayBlocked(player)
    expect(log).toEqual([`state ${ENDED}`, 'error 150', 'autoplay blocked'])
    expect(player.state).toBe(ENDED)
  })

  it('defers events and sends them, with those they cause, in order', () => {
    fake = installYouTubeFake()
    const { player, log } = create(fake)
    fake.ready(player)
    fake.deferEvents()
    player.playVideo()
    // Not started yet as far as the page knows: the default sends nothing.
    player.pauseVideo()
    fake.state(player, PAUSED)
    expect(log).toEqual(['ready'])
    expect(player.getPlayerState()).toBe(PlayerState.UNSTARTED)
    player.events.onStateChange = ({ data }) => {
      log.push(`state ${data}`)
      if (data === PLAYING) player.pauseVideo()
    }
    fake.flushEvents()
    expect(log).toEqual([
      'ready',
      `state ${BUFFERING}`,
      `state ${PLAYING}`,
      `state ${PAUSED}`,
      `state ${PAUSED}`,
    ])
  })

  it('can cancel the start with PAUSED for a pause before the report', () => {
    fake = installYouTubeFake()
    const { player, log } = create(fake)
    fake.ready(player)
    fake.setPauseBeforeStart('paused')
    fake.deferEvents()
    player.playVideo()
    player.pauseVideo()
    fake.flushEvents()
    expect(log).toEqual(['ready', `state ${BUFFERING}`, `state ${PAUSED}`])
    expect(player.getPlayerState()).toBe(PAUSED)
  })

  it('drops queued events on request', () => {
    fake = installYouTubeFake()
    const { player, log } = create(fake)
    fake.ready(player)
    fake.deferEvents()
    player.playVideo()
    fake.discardEvents()
    fake.flushEvents()
    expect(log).toEqual(['ready'])
  })

  it('records destroy so tests can see it is never called', () => {
    fake = installYouTubeFake()
    const { player } = create(fake)
    player.destroy()
    expect(fake.callsOf(player)).toEqual(['destroy'])
  })

  it('calls the ready callback on announce', () => {
    fake = installYouTubeFake()
    const ready = vi.fn()
    window.onYouTubeIframeAPIReady = ready
    fake.announce()
    expect(ready).toHaveBeenCalledTimes(1)
  })

  it('puts window back as it was on uninstall, scripts removed', () => {
    fake = installYouTubeFake()
    window.onYouTubeIframeAPIReady = () => {}
    const script = document.createElement('script')
    script.src = 'https://www.youtube.com/iframe_api'
    document.head.append(script)
    fake.uninstall()
    fake = null
    expect('YT' in window).toBe(false)
    expect('onYouTubeIframeAPIReady' in window).toBe(false)
    expect(youtubeScripts()).toHaveLength(0)
  })
})
