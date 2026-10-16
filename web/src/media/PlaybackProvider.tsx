import { useState, type ReactNode } from 'react'
import {
  createPlaybackCoordinator,
  PlaybackContext,
} from './playbackCoordinator'

// One coordinator for every player inside, kept for the provider's lifetime:
// the story screen wraps each result in one, so a new result gets a new one.
export function PlaybackProvider({ children }: { children: ReactNode }) {
  const [coordinator] = useState(createPlaybackCoordinator)
  return <PlaybackContext value={coordinator}>{children}</PlaybackContext>
}
