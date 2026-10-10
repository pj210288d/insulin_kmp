package com.dj.insulink.feature.settings.ui.viewmodel

import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.core.session.SettingsSession
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.shared.feature.settings.ui.viewmodel.SettingsViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private lateinit var preferences: SettingsPreferences
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        SettingsSession.setCurrentGlucoseUnit(GlucoseUnit.MG_DL)
        LocalizationSession.setCurrentLanguage(AppLanguage.SERBIAN)
        preferences = mockk(relaxed = true)
        every { preferences.getLanguage() } returns AppLanguage.ENGLISH
        viewModel = SettingsViewModel(preferences)
    }

    @After
    fun tearDown() {
        SettingsSession.setCurrentGlucoseUnit(GlucoseUnit.MG_DL)
        LocalizationSession.setCurrentLanguage(AppLanguage.SERBIAN)
    }

    @Test
    fun `initial state reflects stored language and session glucose unit`() {
        assertEquals(AppLanguage.ENGLISH, viewModel.language.value)
        assertEquals(GlucoseUnit.MG_DL, viewModel.glucoseUnit.value)
    }

    @Test
    fun `setLanguage persists and updates state and localization session`() {
        viewModel.setLanguage(AppLanguage.SERBIAN)

        verify { preferences.setLanguage(AppLanguage.SERBIAN) }
        assertEquals(AppLanguage.SERBIAN, viewModel.language.value)
        assertEquals(AppLanguage.SERBIAN, LocalizationSession.currentLanguage.value)
    }

    @Test
    fun `setGlucoseUnit persists and updates settings session`() {
        viewModel.setGlucoseUnit(GlucoseUnit.MMOL_L)

        verify { preferences.setGlucoseUnit(GlucoseUnit.MMOL_L) }
        assertEquals(GlucoseUnit.MMOL_L, viewModel.glucoseUnit.value)
        assertEquals(GlucoseUnit.MMOL_L, SettingsSession.currentGlucoseUnit.value)
    }
}
