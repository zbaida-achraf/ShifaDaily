#!/usr/bin/env node
// Post-build checks on dist/: broken internal links/assets, lang/dir attributes,
// bilingual coverage, no localhost URLs in production builds, release link sanity.
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const dist = fileURLToPath(new URL('../dist/', import.meta.url));
const base = (process.env.BASE_PATH || '/').replace(/\/?$/, '/');
const production = !!process.env.SITE_URL && !/localhost/.test(process.env.SITE_URL);
const errors = [];

const files = [];
(function walk(d) { for (const f of readdirSync(d)) { const p = join(d, f); statSync(p).isDirectory() ? walk(p) : files.push(p); } })(dist);
const html = files.filter((f) => f.endsWith('.html'));

function resolve(href) {
  let p = href.split('#')[0].split('?')[0];
  if (!p.startsWith(base)) return null; // external or other
  p = decodeURIComponent(p.slice(base.length));
  const cand = [join(dist, p), join(dist, p, 'index.html')];
  return cand.find((c) => existsSync(c) && statSync(c).isFile()) ?? false;
}

for (const f of html) {
  const s = readFileSync(f, 'utf8');
  const rel = relative(dist, f).split(sep).join('/');
  const m = /<html[^>]*\blang="(\w+)"[^>]*\bdir="(ltr|rtl)"/.exec(s);
  if (!m) errors.push(`${rel}: missing <html lang/dir>`);
  else if (rel.startsWith('ar/') && (m[1] !== 'ar' || m[2] !== 'rtl')) errors.push(`${rel}: Arabic page must be lang="ar" dir="rtl"`);
  else if (rel.startsWith('en/') && (m[1] !== 'en' || m[2] !== 'ltr')) errors.push(`${rel}: English page must be lang="en" dir="ltr"`);
  for (const [, attr, href] of s.matchAll(/\b(href|src)="([^"]+)"/g)) {
    if (/^(https?:|mailto:|data:)/.test(href)) {
      if (production && /localhost|127\.0\.0\.1/.test(href)) errors.push(`${rel}: localhost URL ${href}`);
      continue;
    }
    if (href.startsWith('#')) continue;
    const r = resolve(href);
    if (r === false) errors.push(`${rel}: broken ${attr} -> ${href}`);
  }
  if (production && /localhost:4321/.test(s)) errors.push(`${rel}: contains localhost`);
}

for (const f of html.filter((f) => relative(dist, f).startsWith('en' + sep))) {
  const twin = f.replace(`${sep}en${sep}`, `${sep}ar${sep}`);
  if (!existsSync(twin)) errors.push(`missing Arabic version of ${relative(dist, f)}`);
}

const release = JSON.parse(readFileSync(new URL('../src/data/release.json', import.meta.url), 'utf8'));
if (release.available && !/^https:\/\//.test(release.apkUrl || '')) errors.push('release.apkUrl must be an https URL');

console.log(`check-site: ${html.length} HTML files, ${files.length} files total, release.available=${release.available}`);
if (errors.length) { console.error(errors.join('\n')); process.exit(1); }
console.log('check-site: OK');
