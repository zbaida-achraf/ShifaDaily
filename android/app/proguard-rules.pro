# Room, Compose, DataStore and AndroidX ship their own consumer rules.
# Receivers are referenced from the manifest and kept automatically.

# Keep enum names stable: they are persisted in Room as strings.
-keepclassmembers enum com.shifadaily.app.** { *; }
