package com.majidbahmani.rota.viewmodel

import com.majidbahmani.rota.core.domain.config.SearchConfig
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDetails
import com.majidbahmani.rota.core.domain.usecase.GetNearbyPoisUseCase
import com.majidbahmani.rota.fake.FakePoiRepository
import com.majidbahmani.rota.util.MainDispatcherRule
import com.majidbahmani.rota.viewmodel.NearbyMapUiState.ErrorReason
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

// runCurrent() is still experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyMapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePoiRepository()
    private val center = SearchConfig.DEFAULT_CENTER

    /** The state is lazy: like the screen, a test must collect it to start loading. */
    private fun TestScope.collectedViewModel(): NearbyMapViewModel {
        val viewModel = NearbyMapViewModel(GetNearbyPoisUseCase(repository))
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private fun place(id: String, latitude: Double) = Poi(
        id = id,
        name = null,
        location = GeoPoint(latitude, center.longitude),
        address = null,
        operator = null,
        openingHours = null,
        hasFee = null,
        details = PoiDetails.Charging(emptyList(), network = null),
    )

    @Test
    fun `starts with EV chargers loading`() = runTest {
        val viewModel = collectedViewModel()

        assertEquals(NearbyMapUiState(PoiCategory.EV_CHARGER, center, isLoading = true), viewModel.uiState.value)
        assertEquals(
            listOf(FakePoiRepository.Request(center, SearchConfig.radiusMeters(PoiCategory.EV_CHARGER), setOf(PoiCategory.EV_CHARGER))),
            repository.requests,
        )
    }

    @Test
    fun `success shows places nearest first`() = runTest {
        val viewModel = collectedViewModel()

        repository.answer(0, Result.success(listOf(place("far", 38.75), place("near", 38.73))))
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(listOf("near", "far"), state.places.map { it.poi.id })
    }

    @Test
    fun `selecting a category searches it with its radius and clears old places`() = runTest {
        val viewModel = collectedViewModel()
        repository.answer(0, Result.success(listOf(place("charger", 38.73))))
        runCurrent()

        viewModel.onCategorySelected(PoiCategory.FUEL)
        runCurrent()

        assertEquals(NearbyMapUiState(PoiCategory.FUEL, center, isLoading = true), viewModel.uiState.value)
        assertEquals(
            FakePoiRepository.Request(center, SearchConfig.radiusMeters(PoiCategory.FUEL), setOf(PoiCategory.FUEL)),
            repository.requests.last(),
        )
    }

    @Test
    fun `switching category cancels the running search`() = runTest {
        val viewModel = collectedViewModel()

        viewModel.onCategorySelected(PoiCategory.PARKING)
        runCurrent()
        repository.answer(1, Result.success(listOf(place("parking", 38.73))))
        repository.answer(0, Result.success(listOf(place("late-charger", 38.73))))
        runCurrent()

        assertEquals(PoiCategory.PARKING, viewModel.uiState.value.category)
        assertEquals(listOf("parking"), viewModel.uiState.value.places.map { it.poi.id })
    }

    @Test
    fun `selecting the same category again doesn't search again`() = runTest {
        val viewModel = collectedViewModel()

        viewModel.onCategorySelected(PoiCategory.EV_CHARGER)
        runCurrent()

        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `errors map to a reason, and retry searches again`() = runTest {
        val viewModel = collectedViewModel()
        repository.answer(0, Result.failure(IOException("offline")))
        runCurrent()
        assertEquals(ErrorReason.NO_CONNECTION, viewModel.uiState.value.error)

        viewModel.retry()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)
        repository.answer(1, Result.failure(IllegalStateException("504")))
        runCurrent()

        assertEquals(ErrorReason.SERVICE, viewModel.uiState.value.error)
        assertEquals(2, repository.requests.size)
    }
}
