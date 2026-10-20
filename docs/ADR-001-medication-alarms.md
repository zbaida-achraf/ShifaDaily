# ADR-001 — Medication reminder alarm strategy

- Status: Accepted
- Date: 2026-10-19
- Scope: ShifaDaily Android app (`com.shifadaily.app`)

## Context

Medication reminders are the core function of ShifaDaily. They must fire when the
app is not open, must not depend on a network connection, and must never be
silently duplicated or fabricated. Android restricts background work, and the
rules change between API levels, so the design was based on the official
documentation consulted on 2026-10-19:

| Source | Key facts used |
|---|---|
| developer.android.com — *Schedule alarms* | Inexact alarms may be deferred (≈ up to 1 h on Android 12+, longer in Doze). `setExactAndAllowWhileIdle()` fires "even if battery-saving measures are in effect". Apps targeting 12+ must declare `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`. `SCHEDULE_EXACT_ALARM` is user-granted, revocable and **not pre-granted** for new installs on Android 14+; it is denied again after a backup/restore onto Android 14. Call `canScheduleExactAlarms()` before scheduling. The system broadcasts `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` when access is granted. "All alarms are canceled when a device shuts down"; `RECEIVE_BOOT_COMPLETED` only works after the user has launched the app once. |
| developer.android.com — *Notification runtime permission* | On Android 13+ notifications are **off by default** for new installs; if the user denies `POST_NOTIFICATIONS`, the app cannot post (alarms are not exempt). |
| developer.android.com — *FGS Task Manager (Android 13)* | A **force stop** cancels the app's alarms and jobs. |
| developer.android.com — *Target API requirements* | From 31 Aug 2026 new apps/updates on Google Play must target API 36. |

`USE_EXACT_ALARM` is auto-granted but is meant for apps whose core function is an
alarm clock or calendar and is subject to Google Play policy review. A medication
reminder app is not clearly inside that scope, so we do **not** declare it.

## Decision

1. **Native AlarmManager, one rolling alarm.** The app keeps exactly **one** pending
   OS alarm: the earliest of (next scheduled dose of any active medication) and
   (earliest snooze expiry). When it fires, `ReminderEngine.reconcile()` posts every
   reminder that is due and schedules the next one. Because there is never more than
   one pending alarm, stale or duplicate OS alarms cannot accumulate.
2. **The database is the source of truth.** Medication schedules (start/end dates,
   schedule type, weekdays, interval, local times, paused flag) live in Room. Pending
   OS alarms are disposable and are recomputed from the database on: app start, any
   medication edit, alarm delivery, boot (`BOOT_COMPLETED`), app update
   (`MY_PACKAGE_REPLACED`), clock change (`TIME_SET`), time-zone change
   (`TIMEZONE_CHANGED`) and exact-alarm permission grant.
3. **Stable occurrence identity.** An occurrence is identified by
   `(medicationId, localDate, localTimeOfDay)`. `dose_records` has a UNIQUE index on
   that triple; a reminder is only posted when the insert succeeds, so the same dose
   can never be notified twice by concurrent triggers (a process-wide mutex also
   serialises reconcile runs). The notification id is the dose record id.
4. **Exact when allowed, honest when not.** If `canScheduleExactAlarms()` is true
   (always true below API 31) we use `setExactAndAllowWhileIdle()`. Otherwise we
   fall back to `setAndAllowWhileIdle()` (inexact; may be delayed) and the dashboard
   and *Reminder check* screen show a warning with a button to the system
   "Alarms & reminders" screen (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`).
5. **Local wall-clock semantics.** Times are stored as local times of day. A dose at
   08:00 stays at 08:00 local time after travelling or a DST change. Instants are
   computed with `ZonedDateTime.of(date, time, systemZone)`:
   - DST gap (e.g. 02:30 does not exist): the occurrence moves forward by the gap
     length (03:30). It is not skipped.
   - DST overlap (01:30 happens twice): the earlier offset is used, once. The UNIQUE
     key prevents a second reminder.
6. **Missed reminders and catch-up.** Reconcile looks back from the last successful
   run (at most 24 h). For an occurrence that passed without a reminder:
   - ≤ 30 min late: one normal reminder is posted (e.g. phone just rebooted).
   - > 30 min late: the dose is recorded as **Missed — reminder not delivered** and
     **one** summary notification invites the user to review. No burst of
     catch-up alarms is produced.
   A reminder that was shown but never answered stays *Pending* and becomes
   *Missed (no response)* after 6 hours. Dismissing a notification records nothing.
   The user can correct any status; every change is written to `dose_events`
   (audit trail with source and timestamps; scheduled time and action time are
   stored separately).
7. **No endless ringing.** Each reminder plays the channel sound once. Snooze
   re-alerts after the user's chosen interval (5/10/15/30 min) up to a configurable
   maximum (default 3). After that only *Taken* / *Skipped* remain.
8. **Notification channels** (created once, then user-controllable in system
   settings): *Reminders with sound*, *Vibration only*, *Silent*. Each medication
   picks one. Lock-screen visibility is PRIVATE with a generic public version, so
   medication names are not shown on a locked screen.
9. **No full-screen intents, no foreground service, no WorkManager for alarms.**
   Full-screen intents are restricted on Android 14+ and are not needed. WorkManager
   is not used as a substitute for exact alarms.
10. **Troubleshooting log.** Scheduling outcomes (exact/inexact, failures, boot
    restores) are written to a local `scheduler_log` table (max 200 rows). It
    contains no medication names or doses, and nothing is transmitted.

## What can and cannot be guaranteed

| Situation | Behaviour |
|---|---|
| App closed / swiped from recents | Alarm still fires (OS-held). |
| Screen off, Doze | `setExactAndAllowWhileIdle` fires; Android may rate-limit such alarms in deep Doze (≈ once per 9 min per app), so a 5-min snooze can be late. |
| Exact-alarm access denied | Inexact alarm: may be delayed (minutes, up to an hour or more in Doze). User is warned. |
| Notifications denied | Alarm fires, but nothing can be shown. User is warned on the dashboard. |
| Device rebooted | Alarm restored after `BOOT_COMPLETED` once the user unlocks; past doses handled as in §6. Only works if the app was opened at least once. |
| App force-stopped | Android cancels the alarm and blocks the app's receivers. **No reminders** until the user opens the app again. Cannot be worked around. |
| Device powered off | Nothing can fire. Handled on next boot as in §6. |
| App updated | `MY_PACKAGE_REPLACED` → reconcile. |
| Clock / time-zone change | `TIME_SET` / `TIMEZONE_CHANGED` → reconcile. |
| Offline | Fully functional; no network is used for reminders. |
| Battery saver / OEM "app killers" | Can delay or suppress alarms on some manufacturers' builds. The *Reminder check* screen links to battery-optimisation settings; we do **not** request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. |
| DND / muted volume | The OS decides whether sound plays. The check screen reports ringer mode, notification volume and DND state where the API allows. |

**ShifaDaily cannot promise that a reminder will always sound.** This is said in
the onboarding text, the Reminder check screen and on the website.

## Alternatives considered

- **`setAlarmClock()`**: never deferred, but it shows an alarm-clock icon and
  appears as the user's "next alarm" (lock screen, quick settings), which is
  confusing for a medication helper. It also needs the same exact-alarm permission.
  Kept as a possible opt-in for a later release.
- **One OS alarm per occurrence**: more PendingIntents to keep in sync, so there is
  more risk of orphan or duplicate alarms. The rolling single alarm is simpler.
- **WorkManager / periodic work**: inexact and deferrable by design. Rejected for
  dose reminders.
- **PWA / web push**: rejected by the product requirements and unreliable.

## Consequences

- Correctness rests on `ScheduleCalculator` (pure Kotlin, unit-tested for DST,
  weekdays, intervals and boundaries) and on `ReminderEngine.reconcile()` being
  idempotent.
- Exact-alarm behaviour on real OEM devices must be validated on physical
  hardware. Emulator results are not proof (see `docs/QA-REPORT.md`).
