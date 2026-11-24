package com.example.seteasecloudmusic.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.seteasecloudmusic.core.settings.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {

    @Test
    fun resolveDarkTheme_respectsThemeModeAndSystemState() {
        assertFalse(resolveDarkTheme(AppThemeMode.SYSTEM, isSystemDark = false))
        assertTrue(resolveDarkTheme(AppThemeMode.SYSTEM, isSystemDark = true))

        assertFalse(resolveDarkTheme(AppThemeMode.LIGHT, isSystemDark = false))
        assertFalse(resolveDarkTheme(AppThemeMode.LIGHT, isSystemDark = true))

        assertTrue(resolveDarkTheme(AppThemeMode.DARK, isSystemDark = false))
        assertTrue(resolveDarkTheme(AppThemeMode.DARK, isSystemDark = true))
    }

    @Test
    fun colorPalettes_satisfyOledDarkAndHighContrastInvariants() {
        assertFalse(LightAppColors.isDark)
        assertEquals(Color.White, LightAppColors.background)
        assertTrue(LightAppColors.textPrimary.luminance() < 0.05f)
        assertTrue(LightAppColors.surface.luminance() > 0.9f)

        assertTrue(DarkAppColors.isDark)
        assertEquals(Color.Black, DarkAppColors.background)
        assertTrue(DarkAppColors.textPrimary.luminance() > 0.95f)
        assertTrue(DarkAppColors.surface.luminance() < 0.05f)
    }

    @Test
    fun tuneAmbientHslForTheme_outputsLightTintInLightModeAndDarkTintInDarkMode() {
        val samples = listOf(
            floatArrayOf(210f, 0.85f, 0.15f),
            floatArrayOf(15f, 0.45f, 0.50f),
            floatArrayOf(120f, 0.90f, 0.88f)
        )

        samples.forEach { sample ->
            val lightHsl = sample.copyOf()
            val darkHsl = sample.copyOf()

            tuneAmbientHslForTheme(lightHsl, isDark = false)
            tuneAmbientHslForTheme(darkHsl, isDark = true)

            assertEquals(sample[0], lightHsl[0], 0.001f)
            assertEquals(sample[0], darkHsl[0], 0.001f)
            assertTrue(lightHsl[1] in 0.32f..0.68f)
            assertTrue(lightHsl[2] in 0.75f..0.84f)
            assertTrue(darkHsl[2] in 0.13f..0.21f)
            assertTrue(lightHsl[2] - darkHsl[2] > 0.5f)
        }

        val neutralLight = floatArrayOf(0f, 0.02f, 0.5f)
        val neutralDark = floatArrayOf(0f, 0.02f, 0.5f)
        tuneAmbientHslForTheme(neutralLight, isDark = false)
        tuneAmbientHslForTheme(neutralDark, isDark = true)
        assertTrue(neutralLight[1] <= 0.08f)
        assertTrue(neutralDark[1] <= 0.08f)
    }
}
