package com.majidbahmani.rota.core.domain.usecase

import com.majidbahmani.rota.core.domain.fake.FakeSettingsRepository
import com.majidbahmani.rota.core.domain.model.PoiDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetPoiDataSourceUseCaseTest {

    @Test
    fun `available source is saved`() = runTest {
        val repository = FakeSettingsRepository()

        assertTrue(SetPoiDataSourceUseCase(repository)(PoiDataSource.GOOGLE_PLACES))
        assertEquals(PoiDataSource.GOOGLE_PLACES, repository.savedPoiDataSource.first())
    }

    @Test
    fun `unavailable source is ignored`() = runTest {
        val repository = FakeSettingsRepository(availablePoiDataSources = setOf(PoiDataSource.OVERPASS))

        assertFalse(SetPoiDataSourceUseCase(repository)(PoiDataSource.GOOGLE_PLACES))
        assertEquals(null, repository.savedPoiDataSource.first())
    }
}
