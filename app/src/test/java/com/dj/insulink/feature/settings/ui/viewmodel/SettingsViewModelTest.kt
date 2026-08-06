package com.dj.insulink.feature.settings.ui.viewmodel

import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private lateinit var preferences: SettingsPreferences
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        preferences = mockk(relaxed = true)
        every { preferences.getLanguage() } returns AppLanguage.ENGLISH
        every { preferences.getGlucoseUnit() } returns GlucoseUnit.MG_DL
        viewModel = SettingsViewModel(preferences)
    }

    @Test
    fun `initial state reflects stored preferences`() {
        assertEquals(AppLanguage.ENGLISH, viewModel.selectedLanguage.value)
        assertEquals(GlucoseUnit.MG_DL, viewModel.selectedGlucoseUnit.value)
    }

    @Test
    fun `setLanguage persists and updates state`() {
        viewModel.setLanguage(AppLanguage.SERBIAN)

        verify { preferences.setLanguage(AppLanguage.SERBIAN) }
        assertEquals(AppLanguage.SERBIAN, viewModel.selectedLanguage.value)
    }

    @Test
    fun `setGlucoseUnit persists and updates state`() {
        viewModel.setGlucoseUnit(GlucoseUnit.MMOL_L)

        verify { preferences.setGlucoseUnit(GlucoseUnit.MMOL_L) }
        assertEquals(GlucoseUnit.MMOL_L, viewModel.selectedGlucoseUnit.value)
    }
}
