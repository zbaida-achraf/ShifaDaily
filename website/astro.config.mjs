// Static site. SITE_URL / BASE_PATH are set at build time for the hosting target
// (e.g. GitHub Pages project sites live under /<repo>/). See docs/DEPLOYMENT.md.
import { defineConfig } from 'astro/config';

export default defineConfig({
  site: process.env.SITE_URL || 'http://localhost:4321',
  base: process.env.BASE_PATH || '/',
  trailingSlash: 'always',
  output: 'static',
  build: { format: 'directory' },
});
