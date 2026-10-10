export type SectionId =
  'opening' | 'neighborhood' | 'city' | 'world' | 'closing'

export type SectionDef = { id: SectionId; title: string }

// Story order from the product brief, section 5. The `id` values are a
// contract with later section APIs and tests, so do not rename them.
export const SECTIONS: readonly SectionDef[] = [
  { id: 'opening', title: '오프닝' },
  { id: 'neighborhood', title: '그날의 동네' },
  { id: 'city', title: '그 도시의 풍경' },
  { id: 'world', title: '그날의 세계' },
  { id: 'closing', title: '마무리' },
]
