import { useState } from 'react'
import styles from './App.module.css'
import { InputScreen } from './screens/InputScreen'
import { StoryScreen } from './screens/StoryScreen'

// Temporary screen switch; M2b replaces it with the real input flow.
function App() {
  const [screen, setScreen] = useState<'input' | 'story'>('input')
  return (
    <main className={styles.main}>
      {screen === 'input' ? (
        <InputScreen onShowStory={() => setScreen('story')} />
      ) : (
        <StoryScreen onRestart={() => setScreen('input')} />
      )}
    </main>
  )
}

export default App
