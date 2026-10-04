package com.majidbahmani.rota.viewmodel

import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.core.domain.usecase.ObservePoiDataSourceUseCase
import com.majidbahmani.rota.core.domain.usecase.SetPoiDataSourceUseCase
import com.majidbahmani.rota.fake.FakeSettingsRepository
import com.majidbahmani.rota.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// runCurrent() is still experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun TestScope.collectedViewModel(settings: FakeSettingsRepository): SettingsViewModel {
        val viewModel = SettingsViewModel(ObservePoiDataSourceUseCase(settings), SetPoiDataSourceUseCase(settings))
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun `shows the current and available sources`() = runTest {
        val viewModel = collectedViewModel(FakeSettingsRepository(availablePoiDataSources = setOf(PoiDataSource.OVERPASS)))

        assertEquals(
            SettingsUiState(selected = PoiDataSource.OVERPASS, available = setOf(PoiDataSource.OVERPASS)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `selecting a source saves it`() = runTest {
        val viewModel = collectedViewModel(FakeSettingsRepository(saved = PoiDataSource.OVERPASS))

        viewModel.onDataSourceSelected(PoiDataSource.GOOGLE_PLACES)
        runCurrent()

        assertEquals(PoiDataSource.GOOGLE_PLACES, viewModel.uiState.value.selected)
    }

    @Test
    fun `an unavailable source can't be selected`() = runTest {
        val viewModel = collectedViewModel(FakeSettingsRepository(availablePoiDataSources = setOf(PoiDataSource.OVERPASS)))

        viewModel.onDataSourceSelected(PoiDataSource.GOOGLE_PLACES)
        runCurrent()

        assertEquals(PoiDataSource.OVERPASS, viewModel.uiState.value.selected)
    }
}
