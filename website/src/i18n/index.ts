import { en } from './en';
import { ar } from './ar';
import type { Lang } from '../site';

export const dict = { en, ar } as const;
export const t = (lang: Lang) => dict[lang];
export const dirOf = (lang: Lang) => (lang === 'ar' ? 'rtl' : 'ltr');
export const otherLang = (lang: Lang): Lang => (lang === 'ar' ? 'en' : 'ar');

/** Pages that exist in both languages (slug '' = home). */
export const PAGES = ['', 'features', 'how-it-works', 'download', 'install', 'changelog', 'faq', 'privacy', 'disclaimer', 'support'] as const;
export type PageSlug = (typeof PAGES)[number];
