# Changelog

## 1.0.1 (versionCode 2) — 2026-10-22

- Accessibility: the "Add medication" button is now announced by screen readers (it had no accessible label).
- Prepared for Google Play (store listing kit in `play-store/`).

## 1.0.0 (versionCode 1) — 2026-10-20

First public release.

- Medication management: add, edit, pause/resume, archive (keeps history) and delete (with history), with
  schedule validation (daily, several times a day, chosen weekdays, every N days, start/end dates).
- Reminders with native Android exact alarms (inexact fallback when "Alarms & reminders" is off), Taken / Snooze /
  Skip actions, configurable snooze length and limit, recovery after reboot, app update, clock and time-zone changes.
- Dose history with corrections and an audit trail; "missed" detection; 7-day summary based on real records.
- Meal assistant: free-text pantry (English, French, Arabic), allergies vs preferences, 19 curated recipes,
  substitutions, added ingredients, quantity and serving adjustments, saved recipes.
- Food guide from the leaflet "For a balanced diet" (Sothema), with attribution.
- Arabic (RTL) and English, light/dark theme, data export (JSON) and full erase.
- Reminder check screen and onboarding with in-context permission requests and a test reminder.
