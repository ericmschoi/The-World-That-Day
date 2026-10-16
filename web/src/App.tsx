import { useState } from 'react'
import styles from './App.module.css'
import { SiteFooter } from './components/SiteFooter'
import { InputScreen } from './screens/InputScreen'
import { StoryScreen } from './screens/StoryScreen'

// The date stays in memory only: never in the URL or browser storage.
type Screen = { screen: 'input' } | { screen: 'story'; date: string }

// Temporary screen switch; M2b replaces it with the real input flow.
function App() {
  const [state, setState] = useState<Screen>({ screen: 'input' })
  return (
    <>
      <main className={styles.main}>
        {state.screen === 'input' ? (
          <InputScreen
            onShowStory={(date) => setState({ screen: 'story', date })}
          />
        ) : (
          <StoryScreen
            date={state.date}
            onRestart={() => setState({ screen: 'input' })}
          />
        )}
      </main>
      <SiteFooter className={styles.footer} />
    </>
  )
}

export default App
