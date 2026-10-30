export type SectionId =
  'opening' | 'neighborhood' | 'city' | 'world' | 'closing'

export type SectionDef = { id: SectionId; title: string }

// Story order from the product brief, section 5. The `id` values are a
// contract with later section APIs and tests, so do not rename them.
export const SECTIONS: readonly SectionDef[] = [
  { id: 'opening', title: 'Opening' },
  { id: 'neighborhood', title: 'The neighbourhood that day' },
  { id: 'city', title: 'Scenes of the city' },
  { id: 'world', title: 'The world that day' },
  { id: 'closing', title: 'Closing' },
]
