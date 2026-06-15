package ru.gentslava.homeassistant.companion.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Home Assistant brand palette (dark). Primary is HA blue (#03A9F4).
val HaBlue = Color(0xFF03A9F4)
val HaBlueDeep = Color(0xFF0288D1)
val HaBackground = Color(0xFF111111)
val HaSurface = Color(0xFF1C1C1C)
val HaCard = Color(0xFF282828)
val HaOnSurface = Color(0xFFE1E1E1)
val HaSecondaryText = Color(0xFF9E9E9E)

// Entity state accents
val HaAmber = Color(0xFFFFC107)   // light on
val HaGreen = Color(0xFF4CAF50)   // switch on
val HaRed = Color(0xFFF44336)     // unlocked / alert
val HaInactive = Color(0xFF4A4A4A) // off / unavailable

val HaColorScheme = darkColorScheme(
    primary = HaBlue,
    onPrimary = Color.White,
    secondary = HaBlueDeep,
    background = HaBackground,
    onBackground = HaOnSurface,
    surface = HaSurface,
    onSurface = HaOnSurface,
    surfaceVariant = HaCard,
    onSurfaceVariant = HaSecondaryText,
    outline = Color(0xFF3A3A3A),
)

/** Accent color for an entity given its domain + state (matches the watch palette). */
fun entityAccent(domain: String, state: String): Color = when (domain) {
    "light" -> if (state == "on") HaAmber else HaInactive
    "switch" -> if (state == "on") HaGreen else HaInactive
    "lock" -> if (state == "locked") HaBlue else HaRed
    else -> HaInactive
}

/** A short glyph for an entity domain. */
fun entityGlyph(domain: String): String = when (domain) {
    "light" -> "💡"   // 💡
    "switch" -> "🔌"  // 🔌
    "lock" -> "🔒"    // 🔒
    else -> "?"
}
