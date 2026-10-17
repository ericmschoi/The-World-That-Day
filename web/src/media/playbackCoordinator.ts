import { createContext, useContext, useState } from 'react'

// Anything that can play: a `<video>` or a YouTube player (M3b3c).
export type Player = { play(): void; pause(): void }

export type PlaybackCoordinator = {
  // Returns the function that unregisters the player; after it, the
  // coordinator never calls that player again.
  register(player: Player): () => void
  // The player came into view and may autoplay: pause the one playing, play it.
  requestPlay(player: Player): void
  // The user started the player with its own controls: pause the other one.
  notifyPlaying(player: Player): void
  // Out of view or stopped: pause it if it is the one playing.
  release(player: Player): void
}

// Keeps a single player playing at a time on one result screen.
export function createPlaybackCoordinator(): PlaybackCoordinator {
  const players = new Set<Player>()
  let playing: Player | null = null

  const takeOver = (player: Player) => {
    if (playing !== null && playing !== player) playing.pause()
    playing = player
  }

  return {
    register(player) {
      players.add(player)
      return () => {
        players.delete(player)
        if (playing === player) playing = null
      }
    },
    requestPlay(player) {
      if (!players.has(player)) return
      takeOver(player)
      player.play()
    },
    notifyPlaying(player) {
      if (players.has(player)) takeOver(player)
    },
    release(player) {
      if (playing !== player || !players.has(player)) return
      playing = null
      player.pause()
    },
  }
}

// The result screen's coordinator, set by `PlaybackProvider`.
export const PlaybackContext = createContext<PlaybackCoordinator | null>(null)

// The nearest provider's coordinator. Outside any provider (a card rendered
// on its own) the caller gets a coordinator of its own.
export function usePlaybackCoordinator(): PlaybackCoordinator {
  const shared = useContext(PlaybackContext)
  const [own] = useState(createPlaybackCoordinator)
  return shared ?? own
}
