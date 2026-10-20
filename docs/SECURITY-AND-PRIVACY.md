# Security, privacy and data lifecycle

## Data inventory

| Data | Where | Sensitive | Leaves the device? |
|---|---|---|---|
| Medications (name, dose, form, schedule, notes, instructions) | Room DB `shifadaily.db` (app-private storage) | Yes (health) | Only via a user-initiated export |
| Dose records + audit events | Room DB | Yes | Only via export |
| Pantry, food preferences, **allergies**, saved recipes | Room DB | Allergies: yes | Only via export |
| Settings (language, theme, snooze, meal filters, onboarding flags, last-reconcile time) | DataStore `settings.preferences_pb` | No | No |
| Scheduler log (event codes, timestamps, counts — no names/doses) | Room DB, max 200 rows | No | No |
| Notifications | Android notification system | Yes | Shown on the device; lock screen shows a generic public version |

There is **no network code and no INTERNET permission**, no analytics, no crash reporting, no ads, and no
third-party SDK that transmits data.

## Lifecycle

Created when the user enters it → used locally for reminders/suggestions → kept until the user edits,
archives (history kept) or deletes (with or without history) → **Settings › Export my data** writes a readable
JSON file to a location the user picks → **Settings › Erase all data** (two confirmations) clears the
database and settings, cancels alarms and notifications → uninstalling also deletes everything.

## Threat model and encryption decision

| Threat | Mitigation |
|---|---|
| Other apps reading data | Android app sandbox (private storage) |
| Lost/stolen locked phone | Android file-based encryption of app storage (default since Android 7/10 on supporting devices) + user screen lock |
| Shoulder surfing / lock screen | Notifications `VISIBILITY_PRIVATE` with a generic public version |
| Cloud backup leaks | `allowBackup=false`, backup rules and data-extraction rules exclude all domains (cloud backup and device-to-device) |
| Network interception | No network use |
| Malicious import files | Import is not implemented (no attack surface). Customization strings are decoded with a strict allow-list parser |
| Exported components | Only the launcher activity and a receiver for protected system broadcasts are exported |
| Trojan-source code | Lint `BidiSpoofing` enforced; escaped code points used |

**Decision:** no additional app-level encryption (e.g. SQLCipher) in 1.0. Without a user secret, the key
would sit in the Android Keystore on the same device and would mainly protect against an attacker who already
has a rooted/compromised or unlocked device. That case is **not** protected: anyone who can unlock the phone, or
malware with root, may read the data. This is disclosed in the privacy policy. Revisit if cloud sync or
multi-user features are ever added.

## Permissions

`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`. No location, contacts,
storage, camera, network, battery-optimisation-exemption or full-screen-intent permission.

## Legal assessment (not legal advice; outstanding items)

ShifaDaily's publisher does not receive any personal data. Processing happens only on the user's device.
That reduces, but may not eliminate, obligations:

- **Morocco — Law 09-08 / CNDP:** health data is "sensitive"; processing it generally needs CNDP authorization.
  It is **unverified** whether a publisher of a purely on-device app with no data access is a "data controller".
  → Obtain advice from a Moroccan data-protection lawyer or ask the CNDP before a wide public launch.
- **EU GDPR (if offered to EU residents):** health data is special-category data. With no server processing the
  publisher likely processes nothing, but the website host (GitHub) processes IP logs. → Confirm with counsel.
- **Medical-device regulation:** the app is designed as an organiser (no dosing, diagnosis or glucose
  interpretation), which is intended to keep it outside medical-device definitions. **No regulatory assessment
  has been performed.** Do not market it with medical claims.
- **Google Play (if used):** health-app declaration and Data safety form required.

ShifaDaily is **not** claimed to be "fully secure", certified, or compliant with any specific regulation.

## Dependencies

All from Google Maven / Maven Central (AndroidX, Kotlin, KSP) at latest stable versions on 19 Oct 2026.
Website: `astro@7.3.5` pinned exactly, `npm audit`: 0 vulnerabilities on 19 Oct 2026; no runtime JavaScript is
shipped except a 3-line language redirect on the root page.
