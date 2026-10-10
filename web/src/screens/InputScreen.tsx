import styles from './Screen.module.css'

type Props = { onShowStory: () => void }

// Placeholder until the real input form arrives in M2b.
export function InputScreen({ onShowStory }: Props) {
  return (
    <>
      <h1 className={styles.title}>The World That Day</h1>
      <p className={styles.lead}>
        생년월일과 태어난 장소를 입력하는 화면이 곧 이곳에 생깁니다.
      </p>
      <button type="button" className={styles.button} onClick={onShowStory}>
        빈 결과 화면 보기
      </button>
    </>
  )
}
