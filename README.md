# ShifaDaily — شفاء يومي

*A healthier routine, one day at a time. روتين صحي أفضل، يومًا بعد يوم*

ShifaDaily is a bilingual (Arabic / English) Android app that reminds people to take the medications they already
have, records doses, and suggests diabetes-conscious meal ideas from what is in the kitchen. It works offline,
needs no account, and sends no data anywhere. A static website is the official download portal.

> ShifaDaily is an organiser, not a medical device. It never calculates or recommends doses, does not interpret
> blood glucose, and does not replace a doctor, pharmacist or dietitian.

## Repository

| Path | What |
|---|---|
| `android/` | Kotlin + Jetpack Compose (Material 3) app — Room, DataStore, AlarmManager |
| `android/app/src/main/java/.../domain/` | Pure-Kotlin schedule, reconcile, dose and recipe logic (unit-tested) |
| `website/` | Astro 7 static site, EN/AR, generated release metadata |
| `docs/ADR-001-medication-alarms.md` | Alarm strategy, guarantees and limits |
| `docs/QA-REPORT.md` | Tests, emulator evidence, untested scenarios |
| `docs/ANDROID-BUILD-AND-RELEASE.md` | Build, signing, verification, release process |
| `docs/DEPLOYMENT.md` | Website + APK publication, rollback |
| `docs/SECURITY-AND-PRIVACY.md` | Data lifecycle, threat model, legal open points |
| `docs/FOOD-GUIDE-SOURCE.md` | Leaflet transcription, recipe sourcing, nutrition policy |
| `docs/SERVICES-AND-COSTS.md` | Services, limits, costs, name checks |
| `docs/IOS.md` | iOS requirements (not built) |

## Quick start

```bash
cd android && ./gradlew testDebugUnitTest assembleDebug     # needs JDK 17+ and Android SDK (compileSdk 37)
cd website && npm ci && npm run build && npm run preview    # needs Node 22.22+
```

## Status (20 Oct 2026)

- Android app: built, signed release APK + AAB, 49 unit tests passing, lint clean, tested on Android 16 and
  Android 8.0 emulators (see QA report). **Not tested on physical devices.**
- Website: https://zbaida-achraf.github.io/ShifaDaily/ (GitHub Pages, deployed by `.github/workflows/pages.yml`).
- Download: [GitHub Release v1.0.0](https://github.com/zbaida-achraf/ShifaDaily/releases/tag/v1.0.0) — `ShifaDaily-1.0.0.apk`,
  SHA-256 `341dff20e6785eb5ac431a936e5e35fbb9144faebbefd852273a915c2ed2ab23`.
- Support: [GitHub Issues](https://github.com/zbaida-achraf/ShifaDaily/issues) (never post health information).
- Not done: iOS, Google Play listing, trademark search, legal review (Law 09-08 / GDPR), physical-device
  and OEM battery-manager testing.

## Licence

Copyright © 2026 the ShifaDaily owner. All rights reserved unless a licence file is added.
The food-guide content summarises a third-party leaflet (Sothema) with attribution; it is not covered by any
licence granted here.
