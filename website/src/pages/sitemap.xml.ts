import type { APIRoute } from 'astro';
import { LANGS, langPath } from '../site';
import { PAGES } from '../i18n';

export const GET: APIRoute = ({ site }) => {
  const urls = LANGS.flatMap((lang) => PAGES.map((p) => {
    const loc = new URL(langPath(lang, p), site).href;
    const alts = LANGS.map((l) => `<xhtml:link rel="alternate" hreflang="${l}" href="${new URL(langPath(l, p), site).href}"/>`).join('');
    return `<url><loc>${loc}</loc>${alts}</url>`;
  }));
  const body = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">${urls.join('')}</urlset>\n`;
  return new Response(body, { headers: { 'Content-Type': 'application/xml' } });
};
