package com.dj.insulink.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val insulinkBlue = Color(0xFF4A7BF6)
val insulinkPurple = Color(0xFF8A5CF5)
val outlineGray = Color(0xFFB2B2B2)
val backgroundLight = Color(0xFFEDEDED)

val averageDropTitleRed = Color(0xFFA20000)
val averageDropLabelRed = Color(0xFFFF1B1B)
val averageDropBackgroundRed = Color(0xFFFFC2C2)

val lastDropTitleOrange = Color(0xFFBD5F00)
val lastDropLabelOrange = Color(0xFFF1861B)
val lastDropBackgroundOrange = Color(0xFFFEDFC3)

val glucoseHighYellow = Color(0xFFFFEE58)
val glucoseNormalGreen = Color(0xFF66BB6A)
val glucoseLowRed = Color(0xFFEF5350)

val insulinkBlueDark = Color(0xFF6B9BFF)
val insulinkPurpleDark = Color(0xFFA47CFF)
val outlineGrayDark = Color(0xFF6B6B6B)
val backgroundDark = Color(0xFF1A1A1A)

val averageDropTitleRedDark = Color(0xFFFF6B6B)
val averageDropLabelRedDark = Color(0xFFFF4444)
val averageDropBackgroundRedDark = Color(0xFF4D1A1A)

val lastDropTitleOrangeDark = Color(0xFFFFB366)
val lastDropLabelOrangeDark = Color(0xFFFF9933)
val lastDropBackgroundOrangeDark = Color(0xFF4D2D1A)

val glucoseHighYellowDark = Color(0xFFFFF176)
val glucoseNormalGreenDark = Color(0xFF81C784)
val glucoseLowRedDark = Color(0xFFFF7043)

data class InsulinkColors(
    val insulinkBlue: Color,
    val insulinkPurple: Color,
    val outlineGray: Color,
    val backgroundSecondary: Color,

    val averageDropTitle: Color,
    val averageDropLabel: Color,
    val averageDropBackground: Color,

    val lastDropTitle: Color,
    val lastDropLabel: Color,
    val lastDropBackground: Color,

    val glucoseHigh: Color,
    val glucoseNormal: Color,
    val glucoseLow: Color,
)

val lightInsulinkColors = InsulinkColors(
    insulinkBlue = insulinkBlue,
    insulinkPurple = insulinkPurple,
    outlineGray = outlineGray,
    backgroundSecondary = backgroundLight,

    averageDropTitle = averageDropTitleRed,
    averageDropLabel = averageDropLabelRed,
    averageDropBackground = averageDropBackgroundRed,

    lastDropTitle = lastDropTitleOrange,
    lastDropLabel = lastDropLabelOrange,
    lastDropBackground = lastDropBackgroundOrange,

    glucoseHigh = glucoseHighYellow,
    glucoseNormal = glucoseNormalGreen,
    glucoseLow = glucoseLowRed,
)

val darkInsulinkColors = InsulinkColors(
    insulinkBlue = insulinkBlueDark,
    insulinkPurple = insulinkPurpleDark,
    outlineGray = outlineGrayDark,
    backgroundSecondary = backgroundDark,

    averageDropTitle = averageDropTitleRedDark,
    averageDropLabel = averageDropLabelRedDark,
    averageDropBackground = averageDropBackgroundRedDark,

    lastDropTitle = lastDropTitleOrangeDark,
    lastDropLabel = lastDropLabelOrangeDark,
    lastDropBackground = lastDropBackgroundOrangeDark,

    glucoseHigh = glucoseHighYellowDark,
    glucoseNormal = glucoseNormalGreenDark,
    glucoseLow = glucoseLowRedDark,
)

val LocalInsulinkColors = staticCompositionLocalOf { lightInsulinkColors }

val LightColorScheme = lightColorScheme(
    primary = insulinkBlue,
    onPrimary = Color.White,
    secondary = insulinkPurple,
    onSecondary = Color.White,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    outline = outlineGray,
)

val DarkColorScheme = darkColorScheme(
    primary = insulinkBlueDark,
    onPrimary = Color.Black,
    secondary = insulinkPurpleDark,
    onSecondary = Color.Black,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF2B2B2B),
    onSurface = Color(0xFFE6E1E5),
    outline = outlineGrayDark,
)