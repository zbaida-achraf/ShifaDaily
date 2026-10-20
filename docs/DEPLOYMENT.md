# Website deployment and APK publication

Hosting choice: **GitHub Pages** (website) + **GitHub Releases** (APK). Both are free for a public repository within
the limits in `docs/SERVICES-AND-COSTS.md`. Pages alone is not ideal for large binaries and is not meant for
commercial hosting, so the APK lives in Releases (assets < 2 GiB, HTTPS, no stated bandwidth cap).

## 1. Prerequisites

- Node.js ≥ 22.22 (24 used), npm.
- GitHub account and the GitHub CLI (`gh`), authenticated with `gh auth login` (done by the owner).
- Android SDK build-tools (for `aapt2`/`apksigner`) to generate release metadata.

## 2. Environment configuration (build time, never secret)

| Variable | Example | Purpose |
|---|---|---|
| `SITE_URL` | `https://<user>.github.io` | absolute URLs (canonical, sitemap, Open Graph) |
| `BASE_PATH` | `/ShifaDaily` | project-site sub-path |
| `REPO_URL` | `https://github.com/<user>/ShifaDaily` | footer + support link to Issues |

The CI workflow sets these automatically. **No credentials are used by the website build**, and none can end up
in the public bundle.

## 3. Build and 4. local test

```bash
cd website
npm ci
npm run build      # astro build + scripts/check-site.mjs (links, lang/dir, bilingual pages, https APK link)
npm run preview    # http://localhost:4321
```

## 5. Hosting setup (once)

```bash
gh repo create ShifaDaily --public --source . --push
gh api -X POST repos/<user>/ShifaDaily/pages -f build_type=workflow
```

Pushing changes under `website/` to `main` runs `.github/workflows/pages.yml` (build → deploy).

## 6. Domain and HTTPS

Default: `https://<user>.github.io/ShifaDaily/` with HTTPS enforced by GitHub. Custom domain (optional, paid):
buy the domain, add a `CNAME` record to `<user>.github.io`, set it in *Settings → Pages*, enable *Enforce HTTPS*,
and change `SITE_URL`/`BASE_PATH` (`/`) in the workflow.

## 7. APK publication

```bash
cp android/app/build/outputs/apk/release/app-release.apk release/ShifaDaily-1.0.0.apk
gh release create v1.0.0 release/ShifaDaily-1.0.0.apk release/SHA256SUMS.txt --title "ShifaDaily 1.0.0" --notes-file release/NOTES.md
```

Asset URL: `https://github.com/<user>/ShifaDaily/releases/download/v1.0.0/ShifaDaily-1.0.0.apk`.

## 8. Release metadata

```bash
node website/scripts/release-meta.mjs --apk release/ShifaDaily-1.0.0.apk \
  --url https://github.com/<user>/ShifaDaily/releases/download/v1.0.0/ShifaDaily-1.0.0.apk --date 2026-10-20
git commit -am "Website: release 1.0.0 metadata" && git push   # triggers deploy
```

Then verify from a separate session: HTTP 200 on the APK URL, downloaded SHA-256 equals the website value,
`aapt2`/`apksigner` show the expected package/version/certificate.

## 9. Rollback

- Website: `git revert <commit>` and push (redeploys), or re-run an older successful *Deploy website* workflow run.
- APK: mark a bad release as pre-release or delete its asset, regenerate `release.json` for the previous
  version (or `--none` to hide the button), push. Never re-use a `versionCode`; ship a fixed higher version instead.

## 10. Troubleshooting

| Symptom | Fix |
|---|---|
| CSS/links broken on Pages | `BASE_PATH` wrong; it must equal the repo path |
| Workflow fails at deploy | Settings → Pages → Source = "GitHub Actions"; environment `github-pages` allowed for `main` |
| `check-site` fails | Read the listed file/link; it blocks deploys with broken links or http APK links |
| Download button missing | `release.json` has `"available": false`; regenerate with step 8 |
| Android blocks install | User must allow "Install unknown apps" for their browser (see /install/) |
