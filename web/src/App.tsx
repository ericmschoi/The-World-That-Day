import { useState } from 'react'
import styles from './App.module.css'
import { SiteFooter } from './components/SiteFooter'
import { ConsentProvider } from './consent/ConsentProvider'
import { InputScreen, type StoryInput } from './screens/InputScreen'
import { StoryScreen } from './screens/StoryScreen'

// The input stays in memory only: never in the URL or browser storage. Back
// on the input screen, the last input fills the form again.
type Screen =
  | { screen: 'input'; last?: StoryInput }
  | { screen: 'story'; input: StoryInput }

function App() {
  const [state, setState] = useState<Screen>({ screen: 'input' })
  return (
    <ConsentProvider>
      <main className={styles.main}>
        {state.screen === 'input' ? (
          <InputScreen
            initial={state.last}
            onSubmit={(input) => setState({ screen: 'story', input })}
          />
        ) : (
          <StoryScreen
            {...state.input}
            onRestart={() => setState({ screen: 'input', last: state.input })}
          />
        )}
      </main>
      <SiteFooter className={styles.footer} />
    </ConsentProvider>
  )
}

export default App
