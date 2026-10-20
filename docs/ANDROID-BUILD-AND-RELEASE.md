# Android — build, sign and release

## Toolchain (verified 19 Oct 2026)

| Component | Version | Notes |
|---|---|---|
| JDK | Temurin 21 (AGP needs ≥ 17) | |
| Gradle | 9.8.2 (wrapper, SHA-256 pinned) | |
| Android Gradle Plugin | 9.4.1 | built-in Kotlin (no `kotlin-android` plugin) |
| Kotlin / Compose compiler | 2.4.21 | |
| KSP | 2.3.12 | Room annotation processing |
| Compose BOM | 2026.09.00 (Material 3 1.4.0) | |
| Room 2.8.5 · DataStore 1.2.1 · Navigation 2.10.2 · AppCompat 1.8.0 | | |
| SDK | `compileSdk 37`, `targetSdk 36`, `minSdk 26` | Play requires target 36 from 31 Aug 2026; current AndroidX releases require compileSdk 37 |

**Why minSdk 26 (Android 8.0):** notification channels and `java.time` are native (no desugaring),
adaptive icons, and it still covers the large majority of active devices. Older versions no longer
receive security updates.

## One-time setup

1. Install JDK 17+ and the Android SDK (command-line tools are enough), then accept the SDK licences.
2. Create `android/local.properties`: `sdk.dir=C\:/path/to/android-sdk`.
3. For release signing, create `android/keystore.properties` (git-ignored):

```properties
storeFile=C:/Users/<you>/ShifaDaily-signing/shifadaily-release.jks
storePassword=...
keyAlias=shifadaily
keyPassword=...
```

or set `SHIFADAILY_STORE_FILE`, `SHIFADAILY_STORE_PASSWORD`, `SHIFADAILY_KEY_ALIAS`, `SHIFADAILY_KEY_PASSWORD`.
Without these, `assembleRelease` produces an **unsigned** APK.

The production keystore for this project was generated locally on 19 Oct 2026 in
`C:\Users\Dell Precision\ShifaDaily-signing\` (RSA 4096, alias `shifadaily`, PKCS12, valid 10 000 days).
Certificate SHA-256: `6d5bdc86a2f8911258309db756c5ecb772ea2b723ba712ff65870127da3278fd`.
**Back that folder up in two safe places.** Losing it means existing users can never receive updates.

## Commands (run in `android/`)

```bash
./gradlew testDebugUnitTest          # 49 JVM unit tests
./gradlew lintRelease                # static analysis (fails on errors)
./gradlew assembleDebug              # app/build/outputs/apk/debug/app-debug.apk (id: com.shifadaily.app.debug)
./gradlew assembleRelease            # app/build/outputs/apk/release/app-release.apk (signed if keystore configured)
./gradlew bundleRelease              # app/build/outputs/bundle/release/app-release.aab (for Google Play)
```

On this Windows machine the JVM needed `-Djdk.net.unixdomain.tmpdir=<short path>` in `JAVA_TOOL_OPTIONS`
because of a loopback-socket error in the sandboxed environment. Normal machines do not need it.

## Verify a release artifact

```bash
aapt2 dump badging app-release.apk | grep -E "package|sdkVersion|targetSdk"
apksigner verify --verbose --print-certs app-release.apk
sha256sum app-release.apk
```

Expected: `package: name='com.shifadaily.app'`, the version you set, `minSdkVersion:'26'`,
`targetSdkVersion:'36'`, "Verifies", and the certificate digest above.

## Release process (repeatable)

1. Update `versionCode` (+1) and `versionName` in `android/app/build.gradle.kts`.
2. Add the entry to `CHANGELOG.md` and to both changelog dictionaries (`website/src/i18n/en.ts`, `ar.ts`).
3. `./gradlew clean testDebugUnitTest lintRelease assembleRelease bundleRelease`.
4. Copy the APK to `release/ShifaDaily-<version>.apk`; verify it (above); write `release/SHA256SUMS.txt`.
5. Install it over the previous version on a device; check upgrade, reminders and both languages.
6. Publish a GitHub Release `v<version>` with the APK (and optionally the AAB) as assets.
7. Regenerate website metadata from the real file:
   `node website/scripts/release-meta.mjs --apk release/ShifaDaily-<version>.apk --url <asset URL>`.
8. Build and deploy the website (docs/DEPLOYMENT.md), then download the APK from the live site and compare its SHA-256.

## Google Play (not done)

The AAB is ready (`bundleRelease`) with language splits disabled. Publishing on Play needs a developer
account (one-time fee, identity verification), a store listing, a Data safety form (declare: no data collected
or shared), a health-app declaration, and Play App Signing enrolment. `SCHEDULE_EXACT_ALARM` is declared;
`USE_EXACT_ALARM` is deliberately **not** declared (see ADR-001).
