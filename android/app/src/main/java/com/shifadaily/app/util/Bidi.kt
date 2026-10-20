package com.shifadaily.app.util

/**
 * Wraps user-entered text (medication names, doses such as "500 mg") in a Unicode
 * first-strong isolate (FSI ... PDI). Without it, Latin text inside an Arabic RTL line is
 * reordered (e.g. "500 mg" shows as "mg 500"). Verified on the Android 16 emulator.
 * Escaped code points are used so the source itself contains no invisible characters.
 */
fun isolate(s: String): String = "\u2068$s\u2069"
