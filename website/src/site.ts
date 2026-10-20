import release from './data/release.json';

export type Lang = 'en' | 'ar';
export const LANGS: Lang[] = ['en', 'ar'];

/** Public source repository; set at build time once the repo exists (no invented URL). */
export const REPO_URL: string | undefined = process.env.REPO_URL || undefined;
export const ISSUES_URL: string | undefined = REPO_URL ? `${REPO_URL}/issues` : undefined;

export interface Release {
  available: boolean;
  version?: string;
  versionCode?: number;
  packageName?: string;
  minSdk?: number;
  targetSdk?: number;
  sizeBytes?: number;
  sha256?: string;
  certSha256?: string;
  releaseDate?: string;
  apkUrl?: string;
  apkFileName?: string;
}

export const RELEASE = release as Release;

/** Path inside the site, honouring the configured base (GitHub Pages project sites). */
export function url(path: string): string {
  const base = import.meta.env.BASE_URL.replace(/\/$/, '');
  const p = path.startsWith('/') ? path : `/${path}`;
  return `${base}${p}`;
}

export function langPath(lang: Lang, page = ''): string {
  return url(`/${lang}/${page ? `${page}/` : ''}`);
}

export function formatSize(bytes: number, lang: Lang): string {
  const mb = bytes / (1024 * 1024);
  return new Intl.NumberFormat(lang === 'ar' ? 'ar-MA' : 'en', { maximumFractionDigits: 1 }).format(mb) + (lang === 'ar' ? ' ميغابايت' : ' MB');
}

export function formatDate(iso: string, lang: Lang): string {
  return new Intl.DateTimeFormat(lang === 'ar' ? 'ar-MA' : 'en-GB', { year: 'numeric', month: 'long', day: 'numeric', timeZone: 'UTC' })
    .format(new Date(iso + 'T00:00:00Z'));
}

/** Android version names for the SDK levels we mention. */
export const ANDROID_NAME: Record<number, string> = { 26: '8.0', 27: '8.1', 28: '9', 29: '10', 30: '11', 31: '12', 32: '12L', 33: '13', 34: '14', 35: '15', 36: '16' };
