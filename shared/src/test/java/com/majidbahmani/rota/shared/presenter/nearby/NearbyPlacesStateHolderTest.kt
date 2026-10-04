package com.majidbahmani.rota.shared.presenter.nearby

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.core.domain.model.PoiDetails
import com.majidbahmani.rota.core.domain.usecase.GetNearbyPoisUseCase
import com.majidbahmani.rota.core.domain.usecase.ObservePoiDataSourceUseCase
import com.majidbahmani.rota.shared.fake.FakePoiRepository
import com.majidbahmani.rota.shared.fake.FakeSettingsRepository
import com.majidbahmani.rota.shared.presenter.nearby.NearbyPlacesUiState.ErrorReason
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// runCurrent() is still experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyPlacesStateHolderTest {

    private val repository = FakePoiRepository()
    private val center = GeoPoint(38.7223, -9.1393)

    private fun TestScope.stateHolder(scope: CoroutineScope = backgroundScope) = NearbyPlacesStateHolder(
        getNearbyPois = GetNearbyPoisUseCase(
            repository,
            ObservePoiDataSourceUseCase(FakeSettingsRepository(saved = PoiDataSource.OVERPASS)),
        ),
        scope = scope,
        center = center,
        category = PoiCategory.FUEL,
        radiusMeters = 5_000,
    )

    /** The state is lazy: like the Screen, a test must collect it to start loading. */
    private fun TestScope.collected(holder: NearbyPlacesStateHolder): NearbyPlacesStateHolder {
        backgroundScope.launch { holder.uiState.collect {} }
        runCurrent()
        return holder
    }

    private fun fuel(id: String, latitude: Double) = Poi(
        id = id,
        name = null,
        location = GeoPoint(latitude, center.longitude),
        address = null,
        operator = null,
        openingHours = null,
        hasFee = null,
        details = PoiDetails.Fuel(emptySet()),
    )

    private fun NearbyPlacesStateHolder.placeIds() =
        (uiState.value as NearbyPlacesUiState.Success).places.map { it.poi.id }

    @Test
    fun `starts in loading without a request until collected`() = runTest {
        val holder = stateHolder()
        runCurrent()

        assertEquals(NearbyPlacesUiState.Loading, holder.uiState.value)
        assertEquals(emptyList<Any>(), repository.requests)
    }

    @Test
    fun `collecting shows loading, then places nearest first`() = runTest {
        val holder = collected(stateHolder())
        assertEquals(NearbyPlacesUiState.Loading, holder.uiState.value)

        repository.answer(0, Result.success(listOf(fuel("far", 38.75), fuel("near", 38.73))))
        runCurrent()

        assertEquals(listOf("near", "far"), holder.placeIds())
    }

    @Test
    fun `searches the category and radius around the center once`() = runTest {
        val holder = collected(stateHolder())
        // A second collector shares the same search.
        backgroundScope.launch { holder.uiState.collect {} }
        runCurrent()

        assertEquals(
            listOf(FakePoiRepository.Request(center, 5_000, setOf(PoiCategory.FUEL))),
            repository.requests,
        )
    }

    @Test
    fun `empty result is success with no places`() = runTest {
        val holder = collected(stateHolder())

        repository.answer(0, Result.success(emptyList()))
        runCurrent()

        assertEquals(NearbyPlacesUiState.Success(emptyList()), holder.uiState.value)
    }

    @Test
    fun `network errors and timeouts are no connection`() = runTest {
        val holder = collected(stateHolder())

        listOf(IOException("offline"), SocketTimeoutException("timeout")).forEachIndexed { index, error ->
            if (index > 0) holder.retry()
            runCurrent()
            repository.answer(index, Result.failure(error))
            runCurrent()

            assertEquals(NearbyPlacesUiState.Error(ErrorReason.NO_CONNECTION), holder.uiState.value)
        }
    }

    @Test
    fun `other errors are a service problem`() = runTest {
        val holder = collected(stateHolder())

        repository.answer(0, Result.failure(IllegalStateException("runtime error: Query timed out")))
        runCurrent()

        assertEquals(NearbyPlacesUiState.Error(ErrorReason.SERVICE), holder.uiState.value)
    }

    @Test
    fun `retry after an error loads again`() = runTest {
        val holder = collected(stateHolder())
        repository.answer(0, Result.failure(IOException("offline")))
        runCurrent()

        holder.retry()
        runCurrent()
        assertEquals(NearbyPlacesUiState.Loading, holder.uiState.value)

        repository.answer(1, Result.success(listOf(fuel("near", 38.73))))
        runCurrent()
        assertEquals(listOf("near"), holder.placeIds())
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `retry cancels the running search, so an old answer can't overwrite it`() = runTest {
        val holder = collected(stateHolder())

        holder.retry()
        runCurrent()
        repository.answer(1, Result.success(listOf(fuel("new", 38.73))))
        repository.answer(0, Result.success(listOf(fuel("old", 38.73))))
        runCurrent()

        assertEquals(listOf("new"), holder.placeIds())
    }

    @Test
    fun `nothing updates after the screen's scope is cancelled`() = runTest {
        val screenScope = CoroutineScope(coroutineContext + Job())
        val holder = collected(stateHolder(screenScope))

        screenScope.cancel()
        repository.answer(0, Result.success(listOf(fuel("late", 38.73))))
        runCurrent()

        assertEquals(NearbyPlacesUiState.Loading, holder.uiState.value)
    }
}
