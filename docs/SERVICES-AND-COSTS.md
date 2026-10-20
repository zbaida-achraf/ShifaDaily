# External services, dependencies and costs

Checked 19–20 Oct 2026 against official documentation. "Free" below means *free within the stated limits
today*; none of these services is guaranteed to stay free.

| Service | Used for | Cost | Limits / conditions |
|---|---|---|---|
| GitHub (public repository) | Source code, Issues (support) | Free for public repos | GitHub Terms of Service |
| GitHub Pages | Static website hosting, HTTPS | Free for public repositories | Site ≤ 1 GB; soft bandwidth 100 GB/month; soft 10 builds/hour (not for Actions-based deploys); deployments time out after 10 min; **not intended for commercial/online-business hosting** |
| GitHub Releases | Hosting the APK | Free | Each asset < 2 GiB; up to 1000 assets per release; GitHub states no total-size or bandwidth limit |
| Custom domain (optional) | e.g. shifadaily.app | Paid, registrar-dependent (not purchased) | DNS check on 19 Oct 2026: no records for shifadaily.com/.app/.org (does **not** prove they are available to register) |
| Google Play Console (optional, not used) | Store distribution | One-time registration fee (check current amount in Play Console) | Identity verification, policy review, Data safety form |
| Apple Developer Program (iOS, not used) | iOS distribution | USD 99 / year | See docs/IOS.md |

## Build-time dependencies (no runtime services)

Android: AndroidX (Core, AppCompat, Activity, Lifecycle, Navigation, Room, DataStore, Compose BOM 2026.09.00),
Kotlin coroutines — Apache 2.0, free. Website: Astro 7.3.5 (MIT), free. Tooling: Temurin JDK (GPLv2+CE),
Android SDK (Android SDK License, accepted by the owner on 19 Oct 2026), Gradle (Apache 2.0).

**Recurring cost of the current setup: USD 0**, as long as the repository is public and usage stays within the
limits above. Astro's anonymous CLI telemetry was disabled on the build machine.

## Name / identity checks performed (19 Oct 2026)

| Check | Result | Meaning |
|---|---|---|
| `play.google.com/store/apps/details?id=com.shifadaily.app` | HTTP 404 | No public Play listing with this ID (not proof of global uniqueness) |
| `github.com/shifadaily` | HTTP 404 | Name unused on GitHub at that time |
| DNS shifadaily.com / .app / .org | No records | Not proof of availability |
| Trademark search | **Not performed** | Search WIPO Global Brand Database and OMPIC (Morocco) before publicising the name |
