package com.majidbahmani.rota.core.domain.usecase

import com.majidbahmani.rota.core.domain.fake.FakeSettingsRepository
import com.majidbahmani.rota.core.domain.model.PoiDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// runCurrent() is still experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class ObservePoiDataSourceUseCaseTest {

    private val all = PoiDataSource.entries.toSet()
    private val overpassOnly = setOf(PoiDataSource.OVERPASS)

    private suspend fun current(available: Set<PoiDataSource>, saved: PoiDataSource?) =
        ObservePoiDataSourceUseCase(FakeSettingsRepository(available, saved))().first()

    @Test
    fun `nothing saved, google available - google`() = runTest {
        assertEquals(PoiDataSource.GOOGLE_PLACES, current(all, saved = null))
    }

    @Test
    fun `nothing saved, google not available - overpass`() = runTest {
        assertEquals(PoiDataSource.OVERPASS, current(overpassOnly, saved = null))
    }

    @Test
    fun `saved choice is used when available`() = runTest {
        assertEquals(PoiDataSource.OVERPASS, current(all, saved = PoiDataSource.OVERPASS))
    }

    @Test
    fun `saved google falls back to overpass when it's no longer available`() = runTest {
        assertEquals(PoiDataSource.OVERPASS, current(overpassOnly, saved = PoiDataSource.GOOGLE_PLACES))
    }

    @Test
    fun `follows changes and skips repeated values`() = runTest {
        val repository = FakeSettingsRepository(all, saved = PoiDataSource.OVERPASS)
        val values = mutableListOf<PoiDataSource>()
        val job = launch { ObservePoiDataSourceUseCase(repository)().take(2).toList(values) }
        runCurrent() // let the collector receive the current value first

        repository.setPoiDataSource(PoiDataSource.OVERPASS) // same value: not emitted again
        runCurrent()
        repository.setPoiDataSource(PoiDataSource.GOOGLE_PLACES)
        job.join()

        assertEquals(listOf(PoiDataSource.OVERPASS, PoiDataSource.GOOGLE_PLACES), values)
    }
}
