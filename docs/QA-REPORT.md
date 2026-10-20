# ShifaDaily 1.0.0 — QA report

Test dates: 19–20 October 2026. Tester: Claude Code (automated + scripted emulator QA) for the project owner.

## Environments

| ID | Environment | Details |
|---|---|---|
| JVM | Local JVM unit tests | Temurin JDK 21.0.12, Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21 |
| LINT | Android Lint (release variant) | AGP 9.4.1 lint, `lintRelease` |
| E36 | Android emulator, **Android 16 (API 36)**, x86_64 "default" image, Pixel 6 profile, WHPX acceleration, swiftshader GPU | Release-signed APK (`com.shifadaily.app` 1.0.0), English system locale |
| E26 | Android emulator, **Android 8.0 (API 26)**, x86_64 "default" image | See §4 — minimum-SDK smoke test |
| WEB | Astro static build + local `astro preview`, in-app Chromium, desktop and 375×812 viewport | |
| PHY | **Physical device** | **Not tested** — no physical Android device was available. |

Evidence screenshots are in [`qa-evidence/`](qa-evidence/). Emulator results are not proof of behaviour on
manufacturer builds with aggressive battery management (Samsung, Xiaomi, Huawei, Oppo…).

## 1. Automated tests (JVM)

`./gradlew :app:testDebugUnitTest` — **49 tests, 0 failures, 0 skipped** (final run 20 Oct 2026).

| Suite | Tests | Covers |
|---|---|---|
| `ScheduleCalculatorTest` | 12 | daily / weekdays / every-N-days, start & end bounds, multiple times, half-open ranges, next occurrence with skips, **DST gap (Paris 2026-03-29)**, **DST overlap (Paris 2026-10-25) → exactly one occurrence**, wall-clock stability across DST, **time-zone change** (Casablanca vs Dubai), contradiction detection |
| `ReconcilePlannerTest` | 10 | due → notify once, **duplicate prevention** via existing keys, idempotent re-run, late-alarm grace (≤30 min), **phone off → "missed, not delivered" instead of an alarm burst**, 24 h look-back cap, no catch-up on first run, `effectiveFrom` (no false "missed" after adding/editing), earliest alarm across meds, taken-early skip |
| `RulesAndValidationTest` | 5 | validation errors (missing fields, no weekdays, interval range, end<start, duplicate times, impossible schedule), **snooze interval/limit/clamping**, Room converters round-trip **with an Arabic default locale** |
| `RecipeEngineTest` | 17 | catalog integrity, every ingredient referenced in steps, **removed ingredient never remains in instructions (all recipes, EN+AR)**, **replacement renames ingredient everywhere (EN+AR)**, core ingredients non-removable, **serving scaling**, override scaling, invalid substitutes ignored, added-ingredient steps, display rounding, customization encode/decode + garbage rejection, **allergy hiding / optional auto-removal**, custom-name allergy, vegetarian filter, pantry ranking for "courgettes, eggs, chickpeas, tomatoes, and olive oil", EN/FR/AR pantry parsing, filters |
| `LocalizationCompletenessTest` | 5 | identical EN/AR key sets, matching placeholders, no empty strings, Arabic file really Arabic, bilingual curated data |

Bugs found by tests and fixed: a seasoning list referencing an ingredient absent from recipes; the Arabic dish name
"الحمص" (hummus) equal to the ingredient name "الحمص" (chickpeas) left a stale mention after substitution.

## 2. Static analysis

`./gradlew :app:lintRelease` — **0 errors, 1 warning** (final).

The remaining warning is that `targetSdk 36` is not the newest API level (37 exists). This is deliberate: 36 is the
level Google Play requires from 31 Aug 2026, and 37 behaviour changes have not been tested.

Fixed during QA: 8 configuration-unsafe resource reads in Compose (`LocalContext` → `LocalResources`);
App Bundle language splits disabled (the app switches language at runtime); Arabic plural for the
adherence sentence; Trojan-source lint (`BidiSpoofing`) on literal bidi characters → escaped code points.

## 3. Emulator tests — Android 16 (E36), release-signed APK

| # | Test | Steps | Expected | Actual | Result | Evidence |
|---|---|---|---|---|---|---|
| 1 | Install & launch | `adb install` release APK, start activity | Launches, onboarding step 1 | Launched in 474 ms; onboarding shown | PASS | 01 |
| 2 | Package identity | `aapt2 dump badging`, `apksigner verify` | `com.shifadaily.app`, 1.0.0/1, target 36, min 26, signed | As expected; v2 signature verified; cert SHA-256 `6d5bdc86…3278fd`; labels "ShifaDaily" / "شفاء يومي"; **no INTERNET permission** | PASS | — |
| 3 | Live language switch in onboarding | Tap "العربية" | Whole UI Arabic, RTL, stays on step | RTL layout, mirrored progress/back arrow, Western digits | PASS | 02 |
| 4 | Disclaimer gate | Tap Next without ticking "I understand" | Cannot continue | Stayed on step 2 | PASS | — |
| 5 | Default permissions on API 36 | Fresh install | Notifications off, exact alarms not granted | Both reported off; clear warnings | PASS | 05 |
| 6 | Notification permission in context | Tap "Allow notifications" → system dialog → Allow | Status updates | "Notifications are allowed" | PASS | 08 |
| 7 | Exact-alarm settings route | Tap "Open Alarms & reminders" | System "Alarms & reminders" page for the app | Correct page; toggled on; status updated on return | PASS | 07, 08 |
| 8 | Test reminder (AlarmManager path) | "Send a test reminder in 1 minute" | Exact alarm; notification in app language | `dumpsys alarm`: `window=0 exactAllowReason=permission`; Arabic notification posted 7 s after target | PASS | — |
| 9 | Form validation | Save empty form | Errors listed, nothing saved | Errors shown | PASS | 11 |
| 10 | Create medication | Metformin 500 mg 08:00 daily, created 22:35 | Today's 08:00 not "missed"; next alarm tomorrow 08:00 exact | "Before start" shown; alarm 08:00 next day `window=0` | PASS | — |
| 11 | Clock change recovery | Set clock to 07:58:30 (TIME_SET) | Alarm still correct | Alarm kept at 08:00 | PASS | — |
| 12 | **Delivery with screen off (Doze state "Dozing")** | Screen off, wait | Reminder at 08:00 with Taken/Snooze/Skip, PRIVATE on lock screen | Posted 08:00:01; 3 Arabic actions; `vis=PRIVATE` with public version | PASS | 13, 15 |
| 13 | Sound when another ShifaDaily notification is visible | Dose posted while test notification still shown | Dose alerts with sound | **FAIL (initial)**: Android auto-grouped the two and posted the dose **silently** (`mSoundNotificationKey` pointed at the old notification). Fixed with explicit group + `GROUP_ALERT_CHILDREN`; retested in #17 | FIXED | — |
| 14 | Snooze from notification | Tap "Snooze 10 min" | Notification cleared, exact alarm +10 min, re-alert | Cleared; alarm 08:11:20 exact; re-alerted with sound+vibration (`mSoundNotificationKey` = dose) | PASS | — |
| 15 | App upgrade | `adb install -r` new release over old | Data kept, alarm re-armed without opening app | Alarm re-armed by `MY_PACKAGE_REPLACED`. Android cleared the visible notification of the old version (platform behaviour) | PASS | — |
| 16 | **Bidi of user text in Arabic UI** | Arabic UI, Latin dose "500 mg" | "500 mg" | **FAIL (initial)**: rendered "mg 500" in list and notification. Fixed with Unicode isolates for names/doses and mixed-script strings | FIXED | 17 → 19 |
| 17 | Two medications due at the same minute + re-alert while another is visible | Metformin & Aspirin at 08:00; snooze Aspirin; Metformin still visible | Both shown; snoozed one alerts audibly | Both posted in explicit group; Aspirin re-alert **played sound+vibration** while Metformin visible | PASS | — |
| 18 | Mark taken from notification | Tap "Taken" | Notification removed; record TAKEN with action time | Removed; history "taken, recorded 08:12" (scheduled 08:00) | PASS | 22 |
| 19 | No-response → missed | Dose left unanswered > 6 h | Becomes "Missed" | 20 Oct 08:00 shown as Missed | PASS | 22 |
| 20 | Correction + audit trail | Correct missed → taken | Status updated; audit trail with sources | Trail: shown (auto) → snoozed (notification) → re-alert (auto) → missed (auto) → taken (correction) | PASS | 24 |
| 21 | Exact access revoked via system toggle | Turn off "Alarms & reminders" | App killed by OS; inexact fallback; warning | Process killed; on reopen alarm `window=+1h`; dashboard warning + "(may be delayed)" | PASS | 26 |
| 22 | Exact access re-granted with app in background | Turn toggle on | Exact alarm restored | `window=0` again | PASS | — |
| 23 | **Time-zone change** | Casablanca → Asia/Dubai | 08:00 stays 08:00 local | Alarm at 08:00 Dubai = 04:00 UTC | PASS | — |
| 24 | **Reboot recovery** | `adb reboot`, do not open app | Alarm restored | Exact alarm present after `BOOT_COMPLETED` | PASS | — |
| 25 | **Force-stop** | `am force-stop` | Alarms cancelled (documented platform limit); restored when opened | 0 pending alarms after force-stop; 1 after reopening | PASS (limitation confirmed) | — |
| 26 | Offline | Airplane mode | Fully functional; no network | No INTERNET permission declared; app usable | PASS | — |
| 27 | Pantry free text | "courgettes, eggs, chickpeas, tomatoes, and olive oil" | 5 ingredients; matching recipes first | 5 recognised; skillet first ("have 5 · need 3" = spices) | PASS | 29 |
| 28 | Recipe customisation | Remove chickpeas, servings 2→4 | Steps updated, quantities doubled, salt unscaled | As expected; chickpeas listed under "Removed" with restore | PASS | 30 |
| 29 | Allergy | Egg allergen | Egg recipes hidden with notice (Arabic dual plural) | "أُخفيت وصفتان…" | PASS | 32 |
| 30 | Language switch in Settings | Arabic → English | All UI English; data intact | Data intact | PASS | 50 |
| 31 | Pause / resume | Pause Metformin | Next alarm moves to next active med; resume restores | 21 Oct (Vitamin D) → back to 20 Oct 08:00 | PASS | — |
| 32 | Edit medication | Change dose/unit | Saved; schedule re-armed | Saved | PASS | — |
| 33 | **Erase all data** | Settings → Erase (2 confirmations) | All data, alarms, notifications removed; back to onboarding | **FAIL (initial)**: data erased but UI stayed on Settings (`recreate()` restored nav state). Fixed (fresh task); retested: onboarding step 1, 0 alarms | FIXED/PASS | — |
| 34 | Reminder check screen | Open | Real device state | Notifications/exact/sound/volume 5/7/DND OK; battery optimisation flagged | PASS | 55 |
| 35 | Accessibility labels | UI tree inspection | Controls labelled | Time picker exposes "20 hours" etc.; icon buttons have descriptions | PASS (partial) | — |

## 4. Minimum SDK (Android 8.0 / API 26)

| Test | Result |
|---|---|
| Install release APK on Android 8.0.0 emulator, launch | PASS (launch 1.6 s, onboarding shown) |
| Arabic via AppCompat per-app locale (pre-Android 13 path) | PASS: whole UI Arabic/RTL |
| Permissions on API 26: notifications granted by default, exact alarms allowed without special access | PASS: reported correctly |
| Test reminder through AlarmManager: exact (`window=0`), notification text in **Arabic** from a background receiver (localized-context path for API < 33) | PASS |
| Full medication flows on API 26 | Not repeated (covered on API 36) |

## 5. Website (WEB)

| Test | Result |
|---|---|
| `astro build` + `scripts/check-site.mjs`: 22 HTML pages, no broken internal links/assets, every `en/` page has an `ar/` twin, `lang`/`dir` correct (`ar`=`rtl`, `en`=`ltr`) | PASS |
| Arabic home at 375 px: RTL, no horizontal overflow (`scrollWidth ≤ innerWidth`) | PASS |
| English home desktop, dark scheme | PASS (visual) |
| No release published → no download button; "not available" notice | PASS |
| Production build rejects `localhost` URLs and non-HTTPS APK links | PASS (script) |
| Deployed site / live download / checksum match | See §7 |

## 6. Not tested / known limitations

- **No physical device testing.** Manufacturer battery managers (MIUI, One UI, EMUI, ColorOS) may still delay or kill alarms.
- Deep Doze with the device unplugged for hours was not simulated (`dumpsys deviceidle force-idle`), only screen-off "Dozing".
- Low-storage behaviour, TalkBack walkthrough, 200 % font scale and contrast measurement on device were not executed.
- Android 9–15 were not tested (only 16, and 8.0 smoke test).
- Clock moved backwards: records scheduled "in the future" stay visible in history; no data loss, cosmetic only.
- App update clears the currently visible reminder notification (Android platform behaviour); the dose remains "pending" in the app.
- When two reminders are posted in the same instant, Android plays one sound for both.
- No instrumented (on-device) automated tests are included; device tests were scripted manually via `adb`.
