import type { Media } from '../../../media/types'

// Synthetic `display_mode: image` records in the M3a3 shared media format,
// built around the demo inputs (product brief 12). Hosts are example.org on
// purpose: these are not real Commons files and tests never load them.

// 1988-09-17 + "서울": a photo taken that day.
export const sameDayPhoto: Media = {
  id: 'example-commons-1988-0917-a',
  type: 'photo',
  display_mode: 'image',
  url: 'https://media.example.org/commons/Example_1988-09-17_a.jpg',
  thumbnail_url:
    'https://media.example.org/commons/thumb/640px-Example_1988-09-17_a.jpg',
  thumbnail_width: 640,
  thumbnail_height: 427,
  video_sources: null,
  duration_seconds: null,
  embed_url: null,
  provider: null,
  render: null,
  link_only_reason: null,
  source_name: 'Wikimedia Commons',
  source_url: 'https://commons.example.org/wiki/File:Example_1988-09-17_a.jpg',
  license: 'CC BY-SA 4.0',
  license_status: 'free',
  license_url: 'https://creativecommons.org/licenses/by-sa/4.0/',
  attribution: 'Example Author · CC BY-SA 4.0 · Wikimedia Commons',
  description: '1988년 서울 올림픽 개막식',
  captured_at: '1988-09-17',
  date_precision: 'day',
  lat: null,
  lng: null,
  place_ref: 'country:KR',
  retrieved_at: '2026-10-12T03:00:00Z',
  date_relation: { kind: 'same_day', years: 0 },
}

// 1988-09-17: a category file whose capture time has two values, so the
// server could not read it (captured_at and precision null).
export const undatedPhoto: Media = {
  ...sameDayPhoto,
  id: 'example-commons-1988-0917-b',
  url: 'https://media.example.org/commons/Example_1988-09-17_b.jpg',
  thumbnail_url:
    'https://media.example.org/commons/thumb/640px-Example_1988-09-17_b.jpg',
  source_url: 'https://commons.example.org/wiki/File:Example_1988-09-17_b.jpg',
  license: 'Public domain',
  license_url: null,
  attribution: 'Example Author · Public domain · Wikimedia Commons',
  description: '서울 올림픽 경기장 전경',
  captured_at: null,
  date_precision: null,
  date_relation: { kind: 'unknown', years: 0 },
}

// 2002-06-22 + "광주 서구 풍암동": a KOGL Type 1 photo.
export const koglPhoto: Media = {
  ...sameDayPhoto,
  id: 'example-commons-2002-0622',
  url: 'https://media.example.org/commons/Example_2002-06-22.jpg',
  thumbnail_url:
    'https://media.example.org/commons/thumb/640px-Example_2002-06-22.jpg',
  source_url: 'https://commons.example.org/wiki/File:Example_2002-06-22.jpg',
  license: 'KOGL Type 1',
  license_url: 'https://www.kogl.or.kr/info/licenseType1.do',
  attribution: 'Example Agency · KOGL Type 1 · Wikimedia Commons',
  description: '2002년 월드컵 거리 응원',
  captured_at: '2002-06-22',
  lat: 35.13,
  lng: 126.86,
  place_ref: null,
}

// Synthetic: no license and no author on the source page. Still shown.
export const unlicensedPhoto: Media = {
  ...sameDayPhoto,
  id: 'example-commons-1988-0917-c',
  url: 'https://media.example.org/commons/Example_1988-09-17_c.jpg',
  thumbnail_url:
    'https://media.example.org/commons/thumb/640px-Example_1988-09-17_c.jpg',
  source_url: 'https://commons.example.org/wiki/File:Example_1988-09-17_c.jpg',
  license: null,
  license_status: 'unknown',
  license_url: null,
  attribution: '작가 정보 없음 · 라이선스 확인 안 됨 · Wikimedia Commons',
  description: null,
}

// 1988-09-17: a photo whose year is an estimate ("circa 1987"), one year
// before the requested date.
export const approximatePhoto: Media = {
  ...sameDayPhoto,
  id: 'example-commons-1987-approx',
  url: 'https://media.example.org/commons/Example_1987_approx.jpg',
  thumbnail_url:
    'https://media.example.org/commons/thumb/640px-Example_1987_approx.jpg',
  source_url: 'https://commons.example.org/wiki/File:Example_1987_approx.jpg',
  description: '올림픽 경기장 건설 현장',
  captured_at: '1987',
  date_precision: 'approx',
  date_relation: { kind: 'approximate', years: -1 },
}
