# iOS position

The 1.0 release is Android-only. **No iOS app has been built or signed.**

## What an iOS version would require (Apple docs checked 20 Oct 2026)

- **A Mac with Xcode** and a Swift/SwiftUI implementation (Kotlin/Compose code is not directly reusable; the
  domain logic in `domain/` is pure Kotlin and could move to Kotlin Multiplatform later).
- **Free Apple Account (Personal Team):** on-device testing only. Up to 3 devices, and provisioning profiles
  **expire after 7 days**, so the app must be reinstalled. That is unsuitable for real users.
- **Apple Developer Program, USD 99/year:** required for distribution certificates, App Store Connect,
  TestFlight beta testing and App Store publication (with App Review).
- **No sideloading equivalent** to an Android APK download for the general public (outside specific EU
  arrangements, which have their own requirements).

## Reminder behaviour differences

iOS has no `AlarmManager` equivalent for third-party apps. Reminders would be **local notifications**
(`UNUserNotificationCenter`) scheduled in advance. iOS limits each app to 64 pending local notifications, so a
rolling window must be re-filled whenever the app runs. Time-sensitive notifications need an entitlement and user
consent. Critical alerts (which bypass silent mode) need a special Apple entitlement that is rarely granted for this
category. As on Android, delivery depends on Focus/Do Not Disturb and user settings.
