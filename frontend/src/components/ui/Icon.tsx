import type { SVGProps } from 'react';

const paths = {
  modal: 'M3 7h12v14H3V7Zm6-4h12v12M9 11h8V3',
  scales: 'M12 3v18m-7 0h14M4 7h16M6 7l-4 8h8L6 7Zm12 0-4 8h8l-4-8ZM2 15a4 4 0 0 0 8 0m4 0a4 4 0 0 0 8 0',
  user: 'M20 21v-2a6 6 0 0 0-6-6h-4a6 6 0 0 0-6 6v2M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  mail: 'M3 5h18v14H3V5Zm0 1 9 7 9-7',
  lock: 'M5 10h14v11H5V10Zm3 0V6a4 4 0 0 1 8 0v4m-4 5v2',
  eye: 'M2 12s3-7 10-7 10 7 10 7-3 7-10 7S2 12 2 12Zm13 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
  eyeOff: 'm3 3 18 18M10.5 5.1 12 5c7 0 10 7 10 7a18 18 0 0 1-3 4M6.5 6.5A22 22 0 0 0 2 12s3 7 10 7a13 13 0 0 0 5.5-1.5M10 10a3 3 0 0 0 4 4',
  badge: 'M3 4h18v16H3V4Zm10 4h5m-5 4h5m-5 4h4M9 9a2 2 0 1 1-4 0 2 2 0 0 1 4 0Zm-4 7v-1a2 2 0 0 1 4 0v1',
  pin: 'M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Zm-5 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
  grid: 'M3 3h7v7H3V3Zm11 0h7v7h-7V3ZM3 14h7v7H3v-7Zm11 0h7v7h-7v-7Z',
  folder: 'M3 7V4h6l3 3h9v13H3V7Zm0 3h18',
  plus: 'M12 5v14M5 12h14',
  search: 'M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0Zm-2 5 6 6',
  play: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0ZM10 8l6 4-6 4V8Z',
  minus: 'M5 12h14',
  edit: 'm16 3 5 5M3 21l5-1L21 7a2 2 0 0 0-5-4L3 16v5Z',
  trash: 'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7',
  refresh: 'M20 8a8 8 0 0 0-14-3L3 8m0-5v5h5m-4 8a8 8 0 0 0 14 3l3-3m0 5v-5h-5',
  gavel: 'm14 3 7 7-4 4-7-7 4-4ZM3 21l9-9M3 21h8M8 3l13 13',
  check: 'm5 12 4 4L19 6',
  clock: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0ZM12 6v6l4 3',
  chart: 'M4 20V10h3v10H4Zm7 0V4h3v16h-3Zm7 0v-8h3v8h-3Z',
  chevron: 'm9 5 7 7-7 7',
  down: 'm6 9 6 6 6-6',
  menu: 'M3 5h18M3 12h18M3 19h18',
  close: 'm6 6 12 12M6 18 18 6',
  logout: 'M9 4H3v16h6m6-13 5 5-5 5m-7-5h12',
  calendar: 'M4 5h16v16H4V5ZM4 9h16M8 3v4m8-4v4M8 13h1m3 0h1m3 0h1m-9 4h1m3 0h1',
  bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-9 12a3 3 0 0 0 6 0',
  info: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0ZM12 11v6m0-10h.01',
  file: 'M14 2H5v20h14V7l-5-5Zm0 0v6h5M8 12h8m-8 4h6',
  sparkles: 'm12 3 3 6 6 3-6 3-3 6-3-6-6-3 6-3 3-6Zm8-1v4m-2-2h4',
  alert: 'm12 3 10 18H2L12 3Zm0 6v5m0 3h.01',
  arrowUp: 'M12 20V4m-6 6 6-6 6 6',
  arrowLeft: 'M20 12H4m6-6-6 6 6 6',
  arrowRight: 'M4 12h16m-6-6 6 6-6 6',
} satisfies Record<string, string>;

export type IconName = keyof typeof paths;
export function Icon({ name, ...props }: SVGProps<SVGSVGElement> & { name: IconName }) {
  return <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"
    strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
    <path d={paths[name]} />
  </svg>;
}
